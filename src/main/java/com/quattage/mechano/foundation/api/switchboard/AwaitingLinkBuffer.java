package com.quattage.mechano.foundation.api.switchboard;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;

public class AwaitingLinkBuffer {

    public static final int LIFETIME_MS = 4000;
    private static final int BUFFER_MAX = 256;
    
    private final ObjectOpenHashSet<Awaiting> buffer = new ObjectOpenHashSet<>();
    private final ScheduledExecutorService worker;


    public AwaitingLinkBuffer() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    public void deferForLater(ClientGrid grid, AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, float span, GridResponse futureTask) {

        Objects.requireNonNull(grid);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(trns);
        if(futureTask == null) futureTask = GridResponse.values()[0];
        if(!futureTask.indicatesCompletion()) return;
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
            buffer.add(new Awaiting(start, end, trns, span, futureTask));
        }
    }

    public void deferForLater(ClientGrid grid, AnchorSynchronizer start, AnchorSynchronizer end) {

        Objects.requireNonNull(grid);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        synchronized(buffer) {
            if(buffer.size() > BUFFER_MAX) {
                grid.warn("Couldn't defer task '" + GridResponse.TASK_DESTROY_LINK + "' to this buffer - This buffer is already full! " 
                    + "(buffer currently contains " + buffer.size() + " members)");
                return;
            }
            Awaiting old = buffer.get(new ConnectionKey(start, end));
            if(old != null) {
                old.refresh(GridResponse.TASK_DESTROY_LINK);
                return;
            }
            buffer.add(new Awaiting(start, end));
        }
    }

    public void deferForLater(ClientGrid grid, AnchorSynchronizer start, AnchorSynchronizer end, GridUUID replacement, GridResponse futureTask) {

        Objects.requireNonNull(grid);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if(!(futureTask == GridResponse.TASK_SWAP_END || futureTask == GridResponse.TASK_SWAP_START))
            futureTask = GridResponse.TASK_SWAP_START;
        if(!futureTask.indicatesCompletion()) return;

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
            buffer.add(new Awaiting(start, end, replacement, futureTask));
        }
        return;
    }

    public void deferForLater(ClientGrid grid, GridCatenary cat) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(cat);
        synchronized(buffer) {
            if(buffer.size() > BUFFER_MAX) {
                grid.warn("Couldn't defer task '" + GridResponse.TASK_REASSERT_LINK + "' to this buffer - This buffer is already full! " 
                    + "(buffer currently contains " + buffer.size() + " members)");
                return;
            }
            Awaiting old = buffer.get(cat);
            if(old != null) {
                old.refresh(GridResponse.TASK_REASSERT_LINK);
                return;
            }
            buffer.add(new Awaiting(cat));
        }
        return;
    }

    public void deferForLater(ClientGrid grid, AnchorSynchronizer addr) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(addr);
        synchronized(buffer) {
            if(buffer.size() > BUFFER_MAX) {
                grid.warn("Couldn't defer task '" + GridResponse.TASK_SYNC_SINGLE + "' to this buffer - This buffer is already full! " 
                    + "(buffer currently contains " + buffer.size() + " members)");
                return;
            }
            Awaiting old = buffer.get(new ConnectionKey(addr, addr));
            if(old != null) {
                old.refresh(GridResponse.TASK_SYNC_SINGLE);
                return;
            }
            buffer.add(new Awaiting(addr));
        }
        return;
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
                if(Minecraft.getInstance().isPaused()) 
                    awaiting.refresh(awaiting.awaitingTask);
                else if(awaiting == null || awaiting.taskStatus.indicatesCompletion())
                    bufferIterator.remove();
                else if(awaiting.tryExpire()) {
                    Mechano.LOGGER.warn("Task " + awaiting + " expired before completion after " + awaiting.attempts + " attempts. (error " + awaiting.taskStatus.ordinal() + ": " + awaiting.taskStatus + ")");
                    bufferIterator.remove(); 
                    continue;
                }
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
        private final float span;
        private final @Nullable GridCatenary reassert;
        private final @Nullable GridUUID replacement;
        private final @Nullable TransmitterType<?> trns;
        private GridResponse awaitingTask;
        private GridResponse taskStatus = GridResponse.NONE;
        private long expiryTime;
        private short attempts;

        protected Awaiting(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, float span, GridResponse awaitingTask) {
            this.start = start;
            this.end = end;
            this.span = span;
            this.trns = trns;
            this.awaitingTask = awaitingTask;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
            this.replacement = null;
            this.reassert = null;
        }

        protected Awaiting(AnchorSynchronizer start, AnchorSynchronizer end,  GridUUID replacement, GridResponse awaitingTask) {
            this.start = start;
            this.end = end;
            this.span = 0;
            this.trns = null;
            this.awaitingTask = awaitingTask;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
            this.replacement = replacement;
            this.reassert = null;
        }

        protected Awaiting(AnchorSynchronizer start, AnchorSynchronizer end) {
            this.start = start;
            this.end = end;
            this.span = 0;
            this.trns = null;
            this.awaitingTask = GridResponse.TASK_DESTROY_LINK;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
            this.replacement = null;
            this.reassert = null;
        }

        protected Awaiting(GridCatenary cat) {
            this.start = null;
            this.end = null;
            this.span = cat.calculateSpan();
            this.trns = cat.getTransmitter() == null ? null : cat.getTransmitter().getType();
            this.awaitingTask = GridResponse.TASK_REASSERT_LINK;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
            this.replacement = null;
            this.reassert = cat;
        }

        protected Awaiting(AnchorSynchronizer anchor) {
            this.start = anchor;
            this.end = anchor;
            this.span = 0;
            this.trns = null;
            this.awaitingTask = GridResponse.TASK_SYNC_SINGLE;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
            this.replacement = null;
            this.reassert = null;
        }

        public boolean tryExpire() {
            if(attempts >= Short.MAX_VALUE) return true;
            return expiryTime < System.currentTimeMillis();
        }

        public void refresh(GridResponse awaitingTask) {
            this.awaitingTask = awaitingTask;
            this.expiryTime = System.currentTimeMillis() + (long)LIFETIME_MS;
        }

        public void tryDeferredAction(ClientGrid owner) {
            Objects.requireNonNull(owner);
            if(taskStatus.indicatesCompletion() || awaitingTask == GridResponse.NONE || tryExpire()) return;         
            attempts++;
            switch(awaitingTask) {
                case TASK_CREATE_LINK -> taskStatus = owner.handleCatenaryCreation(start, end, span, trns, ProcessMode.IMMEDIATE);
                case TASK_SYNC_SINGLE -> taskStatus = owner.syncSingleAnchor(start, ProcessMode.IMMEDIATE);
                case TASK_REASSERT_LINK -> taskStatus = owner.reassertCatenary(reassert, ProcessMode.IMMEDIATE);
                case TASK_DESTROY_LINK -> taskStatus = owner.handleCatenaryDestruction(start, end, ProcessMode.IMMEDIATE);
                case TASK_DESTROY_LINK_LAZY -> taskStatus = owner.ensureCatenaryDestroyed(start, end);
                case TASK_SYNC_ANCHORS -> taskStatus = owner.handleCatenarySync(start, end, span, trns, ProcessMode.IMMEDIATE);
                case TASK_SWAP_START -> taskStatus = owner.swapStartingPoint(start, end, replacement, ProcessMode.IMMEDIATE);
                case TASK_SWAP_END -> taskStatus = owner.swapEndingPoint(start, end, replacement, ProcessMode.IMMEDIATE);
                case null, default -> GridResponse.logUnhandled(awaitingTask, this);
            }
        }

        @Override
        public String toString() {
            return "Awaiting['" + awaitingTask + "', @" + SidedGridDispatcher.MANIFEST.getTime(expiryTime) + ", " + start.getAddress() + ", " + end.getAddress() + ", " + replacement + "]";
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
            if(obj instanceof ConnectionKey key) {
                if(reassert != null)
                    return GridUUID.areAsymmetricallyEqual(reassert.getStart(), reassert.getEnd(), key.getStart(), key.getEnd());
                return GridUUID.areAsymmetricallyEqual(this.start, this.end, key.getStart(), key.getEnd());
            }
            if(!(obj instanceof Awaiting that)) return false;
            if(reassert != null)
                return GridUUID.areAsymmetricallyEqual(reassert.getStart(), reassert.getEnd(), that.start, that.end);
            return GridUUID.areAsymmetricallyEqual(this.start, this.end, that.start, that.end);
        }

        @Override
        public int hashCode() {
            if(reassert != null) return reassert.hashCode();
            return start.getAddress().hashCode() + end.getAddress().hashCode();
        }
    }

    /**
     * Controls scheduling behaviour for implementing tasks
     */
    public enum ProcessMode {
        /**
         * executed immediately with no scheduling
         */
        IMMEDIATE,
        /**
         * scheduled immediately without an initial execution
         */
        SCHEDULE,
        /**
         * execute immediately, then scedule only if the initial execution fails
         */
        TRY_THEN_SCHEDULE,
    }
}
