package com.quattage.mechano.foundation.api;

import java.util.HashSet;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection;
import com.quattage.mechano.foundation.helper.Worldly;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * our god is not a just one.
 */
public class LinkDataTracker {

    private @Nullable HashSet<TrackedLink> elements = null;
    private @Nullable Logger log = null;

    public boolean isEnabled() {
        return elements != null;
    }

    public LinkDataTracker enable() {
        this.elements = new HashSet<>();
        return this;
    }

    public LinkDataTracker disable() {
        this.elements = null;
        return this;
    }

    public LinkDataTracker setEnabled(boolean enabled) {
        if(enabled) enable();
        else disable();
        return this;
    }

    public LinkDataTracker withLogging(Logger log) {
        this.log = log;
        return this;
    }

    public void track(LevelReader world, IAttachmentHolder holder, GridConnection connection) {
        if(!isEnabled()) return;
        if(connection == null) {
            log("Skipped adding a null element");
            return;
        }
        if(world == null) {
            log("Skipped adding " + connection + " because the world passed was null.");
            return;
        }
        
        if(holder == null) {
            log("Skipped adding " + connection + " because the holder passed was null.");
            return;
        }
        TrackedLink toAdd = new TrackedLink(world, holder, connection);
        if(elements.add(toAdd))
            log("Successfully added " + toAdd.describeConnection() + " from '" + toAdd.getHolderName() + "' to tracker in [" + toAdd.getDimensionName() + "]");
    }

    public void forget(LevelReader world, IAttachmentHolder holder, GridConnection connection) {
        if(!isEnabled()) return;
        if(connection == null) log("Skipped removing a null element");
        TrackedLink toRemove = new TrackedLink(world, holder, connection);
        if(elements.remove(toRemove))
            log("Successfully removed " + toRemove.describeConnection() + " at '" + toRemove.getHolderName() + "' from tracker in [" + toRemove.getDimensionName() + "]"); 
        else log("Failed to remove " + toRemove.describeConnection() + " at '" + toRemove.getHolderName() + "' from tracker in [" + toRemove.getDimensionName() + "]"); 
    }

    public void log(String message) {
        if(log != null) log.info(message);
    }

    public String describeAll() {
        String out = "";
        if(elements == null || elements.isEmpty()) return out += "\n\tNo link data was tracked during this session.";
        for(TrackedLink link : elements) {
            out += "\n\t■ " + link.getTime() + " ⇒ " + link.describeConnection() + " ::";
            if(link.getWorld().isClientSide()) {
                GridConnection conn = LinkDataStorage.getAsClient(link.world, link.connection);
                if(conn instanceof GridCatenary cat)
                    out += "\n\t\t" + "⇄ Verified, " + conn.describeDataScope(link.world) + " :: (" + cat.describeCatenary() + ") @" + conn.hashCode() + "\n";
                else  out += "\n\t\t⚠ Leaked!\n";
            }
        }

        return out;
    }

    public static class TrackedLink implements Worldly {

        private final long time;
        private final LevelReader world;
        private final @Nullable IAttachmentHolder holder;
        private final GridConnection connection;

        public TrackedLink(LevelReader world, @Nullable IAttachmentHolder holder, GridConnection connection) {
            Objects.requireNonNull(world);
            Objects.requireNonNull(holder);
            Objects.requireNonNull(connection);
            this.time = System.currentTimeMillis();
            this.world = world;
            this.holder = holder;
            this.connection = connection;
        }

        @Override
        public @Nullable Level getWorld() {
            return world instanceof Level level ? level : null;
        }

        @Override
        public int hashCode() {
            return connection.hashCode();
        }

        public String describeConnection() {
            return "[" + connection.getStart() + " -> " + connection.getEnd() + "]";
        }

        public String getHolderName() {
            return holder == null ? "N/A" : holder.getClass().getSimpleName();
        }

        public String getTime() {
            return "( ⏲ " + SidedGridDispatcher.MANIFEST.getTime(time) + " )";
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this) return true;
            if(obj instanceof TrackedLink el)
                return this.connection.equals(el.connection);
            if(obj instanceof GridConnection connection)
                return this.connection.equals(connection);
            return false;
        }
    }
}
