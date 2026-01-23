package com.quattage.mechano.api.grid.component;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.GridComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentUUID.UUIDComposite;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * An object that participates in the {@link GridHierarchy}.
 * Subclasses must provide a {@link ComponentUUID} of a specific type and 
 * lookup paradigm. {@link Griddable} so that they can be located by 
 * the {@link Grid} between logical sides. Most 
 * {@link CircuitComponent} implementations will implement this
 * interface.
 * @see DiscreteComponent
 */
public interface GridConstruct {

    /**
     * Throws exceptions if component <code>child</code> cannot be parented to 
     * <code>parent</code> This method is useful for gametests or after 
     * serialization/deserialization to prevent cascading bugs
     * @param child object - Must be a valid {@link GridConstruct} and must be ownable by <code>parent</code>
     * @param parent object - Must be a valid {@link GridConstruct}
     * @see #assertHierarchyIs
     */
    static void assertValidOwnership(Object child, Object parent) {
        if(child == null) throw new NullPointerException("child component is null!");
        if(parent == null) throw new NullPointerException("parent component is null!");
        if(child == parent) throw new ComponentHierarchyInvalidException(child);
        if(!(child instanceof GridConstruct cup)) throw new IllegalArgumentException("Child object (" + child.getClass().getSimpleName() + ") is not a parentable object!");
        if(!(parent instanceof GridConstruct pup)) throw new IllegalArgumentException("Parent object (" + parent.getClass().getSimpleName() + ") is not a parentable object!");
        if(!cup.getHierarchyType().canBeOwnedBy(pup.getHierarchyType())) throw new ComponentHierarchyInvalidException(cup, pup);
    }

    /**
     * Throws exceptions if <code>obj</code>'s hierarchy type doesn't match
     * <code>type</code>. This method is useful for gametests or after
     * serialization/deserialization to prevent cascading bugs
     * @param obj
     * @param expected
     * @see #assertValidOwnership
     */
    static void assertHierarchyIs(Object obj, ComponentHierarchy expected) {
        switch (obj) {
            case null -> throw new NullPointerException("object is null!");
            case ComponentHierarchy actual -> {
                if(actual == expected) return;
                throw new ComponentHierarchyInvalidException(actual, expected);
            }
            case ComponentUUID<?> id -> {
                if(id.getTargetType() == expected) return;
                throw new ComponentHierarchyInvalidException(id.getTargetType(), expected);
            }
            case GridConstruct gc -> {
                if(gc.getHierarchyType() == expected) return;
                throw new ComponentHierarchyInvalidException(obj, expected);
            }
            default -> {
                throw new IllegalArgumentException("Object (" + obj.getClass().getSimpleName() + ") is not a parentable object!");
            }
        }
    }

    default void updateOwnership(GridConstruct parent) { updateOwnership(null, parent); }
    default void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent) {
        Mechano.LOGGER.warn("Cannot update ownership of construct '" + this.getClass().getSimpleName() + "'");
    }

    default int getHierarchyIndex() {
        GridConstruct parent = getParentConstruct();
        return parent == null ? -1 : parent.indexOfChild(this);
    }

    default int indexOfChild(GridConstruct child) {
        return -1;
    }

    ComponentHierarchy getHierarchyType();

    default int getMergePriority() {
        return getHierarchyType().getMergePriority();
    }


    /**
     * Modify the {@link ComponentUUID} bindings of <code>id</code>
     * to point towards this SourceIdentiifer.
     * @param id GridUUID to bind
     * @return The provided {@link ComponentUUID}, modified as a result of this call.
     */
    default <T extends ComponentUUID<T>> T bindUUID(T id) { 
        return id.withBinding(getHierarchyIndex(), getHierarchyType());
    }

    /**
     * Search this GridConstruct's internal data to find a
     * CircuitComponent using the provided {@link ComponentUUID}'s bindings
     * @param binding {@link UUIDComposite} used to search this component for a sub-component. Defaults to {@link UUIDComposite#EMPTY}
     * @return a {@link CircuitComponent}, or <code>null</code>
     */
    @Nullable CircuitComponent getComponent(UUIDComposite binding);

    /**
     * Search this GridConstruct's internal data to find a
     * CircuitComponent using the provided {@link ComponentUUID}'s bindings
     * @param binding {@link UUIDComposite} used to search this component for a sub-component. Defaults to {@link UUIDComposite#EMPTY}
     * @return a {@link CircuitComponent}, or <code>null</code>
     */
    @Nullable default CircuitComponent getComponent() { return getComponent(UUIDComposite.EMPTY); }

    /**
     * Used to enforce a parent/child relationship for components and the 
     * circuits they belong to. CircuitComponent implementations which
     * require {@link Terminal terminals} may derive their parent
     * component from the {@link Node} at <code>terminals[0]</code>
     * @return The CircuitComponent instance that currently owns this one, 
     * or <code>null</code> if this component has no parent.
     */
    @Nullable GridConstruct getParentConstruct();

    /**
     * Gets the parent of this CircuitComponent, traversing
     * the parent/child hierarchy upwards until it finds the highest
     * level parent.
     * @return The superparent, or <code>null</code> if this construct was found 
     * to have a hierarchy deeper than 255 objects.
     */
    default @Nullable GridConstruct getSuperparent() {
        GridConstruct parent = getParentConstruct();
        for(int x = 0; x < 255; x++) {
            if(!(parent instanceof GridConstruct hp)) return parent;
            GridConstruct candidate = hp.getParentConstruct();
            if(candidate == null || candidate == parent) return parent;
            parent = candidate;
        }
        Mechano.LOGGER.warn("Component hierarchy traversal for " + this + " failed to identify a superparent.");
        return null;
    }

    default void forEachConstructInHierarchy(Consumer<GridConstruct> cons) {
        cons.accept(this);
        GridConstruct parent = getParentConstruct();
        for(int x = 0; x < 255; x++) {
            if(!(parent instanceof GridConstruct hp)) return;
            cons.accept(parent);
            GridConstruct candidate = hp.getParentConstruct();
            if(candidate == null || candidate == parent) return;
            parent = candidate;
        }
    }

    /**
     * An object that refers in some way to one or more {@link GridConstruct} objects.
     * (e.g. a BlockEntity with a {@link Circuit})
     */
    public interface GridReferent<T extends ComponentUUID<T>> {

        static GridReferent<?> choosePrimary(GridReferent<?> a, GridReferent<?> b) {
            if(a.canMoveDynamically() && !b.canMoveDynamically()) return a;
            if(b.canMoveDynamically() && !a.canMoveDynamically()) return b;
            return System.identityHashCode(a) > System.identityHashCode(b) ? a : b;
        }

        /**
         * Provides a (new or pre-existing) {@link ComponentUUID} instance 
         * that points towards this object. Can be used by the {@link Grid}
         * to look this object up. <p>
         * For API users: Use {@link #getUUIDSafe() the checked version} 
         * of this method instead.
         * @return The UUID associated with this identifiable object.
         * @see #getUUIDSafe()
         */
        T getUUID();

        /**
         * Provides a (new or pre-existing) {@link ComponentUUID} instance 
         * that points towards this object. Can be used by the {@link Grid}
         * to look this object up. <p>
         * This method will throw exceptions for null or invalid returns.
         * @return The UUID associated with this identifiable object. Will never be <code>null</code>
         */
        @ApiStatus.NonExtendable
        default T getUUIDSafe() {
            T uuid = getUUID();
            if(uuid == null) 
                throw new NullPointerException("GridIdentifiable '" + this.getClass().getSimpleName() + " failed to provide a vlaid UUID! (got " + uuid + ")");
            return uuid;
        }

        GridComponentTracker getTrackerScope();

        /**
         * Provides access to the instantiator/composer source object
         * that created this GridAPI component. Allows grid-scoped
         * code to access the {@link Griddable} (and, by extension, the world)
         * at arbitrary moments in time. <p>
         * Implementations may <code>null</code> to indicate that their source couldn't be found.
         * @param world world to operate within. Use this world instance for performing BlockEntity, LevelChunk, or Entity lookups.
         * @return The {@link Griddable} that this object belongs to
         */
        @Nullable Griddable<?> getProviderSource(LevelReader world);

        boolean isBeingTrackedBy(ServerPlayer sp);

        default void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
            MinecraftServer server = world.getServer();
            if(server == null)
                server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
            }
        }

        default BlockPos getBlockPos() {
            return getBlockPos(null);
        }

        BlockPos getBlockPos(@Nullable LevelReader world);

        default boolean canMoveDynamically() {
            return canReceiveVelocity();
        }

        default boolean canReceiveVelocity() {
            return false;
        }

        default int getApproximateMass() {
            return Integer.MAX_VALUE;
        }
    }

    public interface TerminalProvider {
        
        Terminal[] getTerminals();

        default int indexOfTerminal(Terminal terminal) {
            Terminal[] terminals = getTerminals();
            if(terminals == null || terminals.length <= 0)
                return -1;
            for(int x = 0; x < terminals.length; x++)
                if(terminals[x] == terminal) return x;
            return -1;
        }

        default boolean hasTerminals() {
            Terminal[] terminals = getTerminals();
            return terminals != null && terminals.length > 0;
        }

        default boolean has(Terminal terminal) {
            return indexOfTerminal(terminal) > -1;
        }

        default void forEachTerminal(Consumer<Terminal> cons) {
            Terminal[] terminals = getTerminals();
            if(terminals == null || terminals.length <= 0)
                return;
            for(int x = 0; x < terminals.length; x++)
                cons.accept(terminals[x]);
        }

        default void forEachAttachedNode(Consumer<Node> cons) {
            Terminal[] terminals = getTerminals();
            if(terminals == null || terminals.length <= 0)
                return;
            for(int x = 0; x < terminals.length; x++) {
                Node attached = terminals[x].getAttachedNode();
                if(attached != null)
                    cons.accept(attached);
            }
        }

        default boolean hasGroundedTerminal() {
            Terminal[] terminals = getTerminals();
                if(terminals == null || terminals.length <= 0)
                    return false;
            for(int x = 0; x < terminals.length; x++) {
                Terminal terminal = terminals[x];
                if(terminal != null && terminal.isGrounded() && terminal.isAttached()) 
                    return true;
            }
            return false;
        }

        default void assertHasTerminals() {
            Terminal[] terminals = getTerminals();
            if(terminals == null) {
                throw new NullPointerException("Error processing StampingComponent '" 
                    + getClass().getSimpleName() + " - This component's terminal array is null!");
            }
            if(terminals.length <= 0) {
                throw new NullPointerException("Error processing StampingComponent '" 
                    + getClass().getSimpleName() + " - This component's terminal array is empty!");
            }
            for(int x = 0; x < terminals.length; x++) {
                if(terminals[x] == null) {
                    throw new NullPointerException("Error processing StampingComponent '" 
                        + getClass().getSimpleName() + "' - Terminal at index " + x + " is null!");
                }
            }
        }
    }

    public static class ComponentHierarchyInvalidException extends RuntimeException {
        public ComponentHierarchyInvalidException(Object root) {
            super("Component of type '" + root.getClass().getSimpleName() + "' cannot be owned by itself.");
        }
        public ComponentHierarchyInvalidException(Object root, ComponentHierarchy expected) {
            super("Component of type '" + root.getClass().getSimpleName() + "' doesn't conform to the expected hierarchy type " + expected + "!");
        }
        public ComponentHierarchyInvalidException(ComponentHierarchy actual, ComponentHierarchy expected) {
            super("An operation got '" + actual + ",' but required '" + expected + "'!");
        }
        public ComponentHierarchyInvalidException(GridConstruct child, GridConstruct parent) {
            super("Component of type '" + child.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "'");
        }
        public ComponentHierarchyInvalidException(GridConstruct child, GridConstruct parent, String message) {
            super("Component of type '" + child.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "' - (" + message + ")");
        }
    }
}