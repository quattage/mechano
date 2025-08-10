package com.quattage.mechano.foundation.gridapi.switchboard;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.gridapi.ClientGrid;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable;
import com.quattage.mechano.foundation.gridapi.SidedGridDispatcher;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.gridapi.landmark.GridCatenary;
import com.quattage.mechano.foundation.gridapi.landmark.GridConnection;
import com.quattage.mechano.foundation.gridapi.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.gridapi.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class AwaitingLinkBuffer {

    public static final int LIFETIME_MS = 5000;
    private static final int BUFFER_MAX = 256;
    
    private final ObjectOpenHashSet<Awaiting> buffer = new ObjectOpenHashSet<>();
    private final ScheduledExecutorService worker;


    public AwaitingLinkBuffer() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    public void deferForLater(ClientGrid grid, AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, GridResponse futureTask) {

        Objects.requireNonNull(grid);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(trns);
        if(futureTask == null) futureTask = GridResponse.values()[0];

        synchronized(buffer) {
            if(buffer.size() > BUFFER_MAX) {
                grid.warn("Couldn't defer task '" + futureTask + "' to this buffer - This buffer is already full! " 
                    + "(buffer currently contains " + buffer.size() + " members)");
                return;
            }
            Awaiting old = buffer.get(new ConnectionKey(start, end));
            if(old != null) {
                old.refresh(futureTask);
                return;
            }
            buffer.add(new Awaiting(start, end, trns, futureTask));
        }
    }

    public void tryTickFrom(ClientGrid owner) {
        if(owner == null) return;
        if(worker.isShutdown()) return;
        synchronized(buffer) {
            for(Awaiting task : buffer)
                task.tryDeferredAction(owner);
        }
    }

    public void wakeUp() {
        Mechano.LOGGER.info("Scheduled cleanup task for awaiting buffer");
        worker.scheduleAtFixedRate(() -> {
            inspectBuffer();
        }, LIFETIME_MS, LIFETIME_MS / 2, TimeUnit.MILLISECONDS);
    }

    private void inspectBuffer() {
        synchronized(buffer) {
            if(buffer == null || buffer.isEmpty()) return;
            Iterator<Awaiting> bufferIterator = buffer.iterator();
            while(bufferIterator.hasNext()) {
                Awaiting awaiting = bufferIterator.next();
                if(awaiting == null || awaiting.tryExpire())
                    bufferIterator.remove(); 
            }
        }
    }

    public void shutdown() {
        worker.shutdown();
        try {
            if(!worker.awaitTermination(LIFETIME_MS / 2, TimeUnit.MILLISECONDS)) {
                Mechano.LOGGER.info("Closed buffer worker thread");
                worker.shutdownNow();
            }
        } catch(InterruptedException e) {
            Mechano.LOGGER.info("Timeout delay reached, forcibly closed buffer worker thread");
            worker.shutdownNow();
        }
        this.buffer.clear();
    }

    public int size() {
        return buffer == null ? 0 : buffer.size();
    }

    private static class Awaiting implements Comparable<Awaiting> {

        private final AnchorSynchronizer start;
        private final AnchorSynchronizer end;
        private final TransmitterType<?> trns;
        private GridResponse awaitingTask;
        private long expiryTime;
        private boolean wasTaskCompleted = false;

        protected Awaiting(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, GridResponse awaitingTask) {
            this.start = start;
            this.end = end;
            this.trns = trns;
            this.awaitingTask = awaitingTask;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
        }

        public boolean tryExpire() {
            if(expiryTime > System.currentTimeMillis())
                return false;
            if(!wasTaskCompleted) 
                Mechano.LOGGER.warn("Task " + this + " expired before completion.");
            this.awaitingTask = GridResponse.NONE;
            return true;
        }

        public void refresh(GridResponse awaitingTask) {
            this.awaitingTask = awaitingTask;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
        }

        public boolean tryDeferredAction(ClientGrid owner) {
            Objects.requireNonNull(owner);
            if(wasTaskCompleted) {
                tryExpire();
                return true;
            }
            if(awaitingTask == GridResponse.NONE) return true;
            switch(awaitingTask) {
                case TASK_CREATE_LINK: return recreate(owner);
                case TASK_SYNC_ANCHORS: return resync(owner);
                default:
                    Mechano.LOGGER.error("Couldn't perform deferred action " + this + " - This response type is unsupported!");
            }
            return false;
        }

        private boolean recreate(ClientGrid grid) {
            AnchorPoint startAnchor = start.applyAndGet(grid.getWorld());
            AnchorPoint endAnchor = end.applyAndGet(grid.getWorld());
            if(startAnchor == null || endAnchor == null) return tryExpire();
            TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(grid.getWorld(), startAnchor, endAnchor);
            GridCatenary cat = new GridCatenary(grid.getWorld(), (AnchorPoint)ordered[0], (AnchorPoint)ordered[1], trns);
            LinkDataStorable.put(grid.getWorld(), cat);
            cat.sendLevelUpdates(grid.getWorld());
            this.awaitingTask = GridResponse.NONE;
            wasTaskCompleted = true;
            return true;
        }

        private boolean resync(ClientGrid grid) {
            GridCatenary cat = LinkDataStorable.getAsClient(grid.getWorld(), new ConnectionKey(start, end));
            if(cat == null) return recreate(grid);
            cat.reinitializeModel(grid.getWorld(), CatenaryAttributes.Initializer.RESTING_SIMULATION);
            this.awaitingTask = GridResponse.NONE;
            wasTaskCompleted = true;
            return true;
        }

        @Override
        public String toString() {
            return "Awaiting['" + awaitingTask + "', @" + SidedGridDispatcher.MANIFEST.getTime(expiryTime) + "]";
        }

        @Override
        public int compareTo(Awaiting o) {
            if(this.expiryTime > o.expiryTime) return 1;
            if(this.expiryTime < o.expiryTime) return -1;
            return this.awaitingTask.compareTo(o.awaitingTask);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this) return true;
            if(obj instanceof ConnectionKey key) 
                return GridConnection.isAsymmetricallyEqual(this.start, this.end, key.getStart(), key.getEnd());
            if(!(obj instanceof Awaiting that)) return false;
            return GridConnection.isAsymmetricallyEqual(this.start, this.end, that.start, that.end);
        }

        @Override
        public int hashCode() {
            return start.getAddress().hashCode() + end.getAddress().hashCode();
        }
    }
}
