package com.quattage.mechano.api.grid.component;

import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

import net.createmod.catnip.platform.CatnipServices;
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
     * @param type
     * @see #assertValidOwnership
     */
    static void assertHierarchyIs(Object obj, ComponentHierarchy type) {
        if(obj == null) throw new NullPointerException("object is null!");
        if(!(obj instanceof GridConstruct pobj)) throw new IllegalArgumentException("Object (" + obj.getClass().getSimpleName() + ") is not a parentable object!");
        if(pobj.getHierarchyType() != type) throw new ComponentHierarchyInvalidException(obj, type);
    }

    default void updateOwnership(GridConstruct parent, int index) { updateOwnership(null, parent, index); }
    default void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent, int index) {
        Mechano.LOGGER.warn("Cannot update ownership of construct '" + this.getClass().getSimpleName() + "'");
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
    default <T extends ComponentUUID<T>> T bindUUID(T id) { return id; }

    /**
     * Search this GridConstruct's internal data to find a
     * CircuitComponent using the provided {@link ComponentUUID}'s bindings
     * @param id ID to look for
     * @return a {@link CircuitComponent}, or <code>null</code>
     */
    @Nullable CircuitComponent findSubComponent(ComponentUUID<?> id);

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
        if(parent == null) return null;
        for(int x = 0; x < 255; x++) {
            if(!(parent instanceof GridConstruct hp)) return parent;
            GridConstruct candidate = hp.getParentConstruct();
            if(candidate == null) return parent;
            parent = candidate;
        }
        Mechano.LOGGER.warn("Component hierarchy traversal for " + this + " failed to identify a superparent.");
        return null;
    }

    /**
     * An object that refers in some way to one or more {@link GridConstruct} objects.
     * (e.g. a BlockEntity with a {@link Circuit}) would be a good candidate for this
     * interface)
     */
    public interface GridReferent<T extends ComponentUUID<T>> {


        static @Nullable Griddable<?> getProviderSourceFor(@Nullable LevelReader world, Object obj) {
            if(obj instanceof AncillaryNode<?> an) return an.getProviderSource();
            if(!(obj instanceof GridReferent<?> gr)) return null;
            return gr.getProviderSource(world);
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

        ComponentTracker getTrackerScope();

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
    }

    public static class ComponentHierarchyInvalidException extends RuntimeException {
        public ComponentHierarchyInvalidException(Object root) {
            super("Component of type '" + root.getClass().getSimpleName() + "' cannot be owned by itself.");
        }
        public ComponentHierarchyInvalidException(Object root, ComponentHierarchy expected) {
            super("Component of type '" + root.getClass().getSimpleName() + "' doesn't conform to the expected hierarchy type " + expected + "!");
        }
        public ComponentHierarchyInvalidException(GridConstruct child, GridConstruct parent) {
            super("Component of type '" + child.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "'");
        }
        public ComponentHierarchyInvalidException(GridConstruct child, GridConstruct parent, String message) {
            super("Component of type '" + child.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "' - (" + message + ")");
        }
    }
}