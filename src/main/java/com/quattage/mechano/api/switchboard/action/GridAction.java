package com.quattage.mechano.api.switchboard.action;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.GridConstruct.GridReferent;
import com.quattage.mechano.api.switchboard.GridActionC2SPacket;
import com.quattage.mechano.api.switchboard.GridActionS2CPacket;
import com.quattage.mechano.api.switchboard.task.GridDumpTask;
import com.quattage.mechano.api.switchboard.task.GridPeekTask;
import com.quattage.mechano.api.switchboard.task.NodeUnionTask;
import com.quattage.mechano.api.switchboard.task.NodeUnunionTask;
import com.quattage.mechano.api.switchboard.task.RequestActionTask;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

public enum GridAction implements StringRepresentable {

    TASK_UNION_NODES                      ( ActionType.TASK_GENERIC, NodeUnionTask.class ),
    TASK_UNUNION_NODES                    ( ActionType.TASK_GENERIC, NodeUnunionTask.class ),
    TASK_GRID_DUMP                        ( ActionType.TASK_GENERIC, GridDumpTask.class ),
    TASK_GRID_PEEK                        ( ActionType.TASK_GENERIC, GridPeekTask.class),
    TASK_REQUEST                          ( ActionType.TASK_GENERIC, RequestActionTask.class ),

    RESPONSE_SUCCESS                      ( ActionType.RESPONSE_SUCCESS, null),
    RESPONSE_FAIL_DUPLICATE_ELEMENT       ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_ELEMENT_FULL            ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_CLOSE               ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_FAR                 ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_INCOMPATIBLE            ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_CANCELLED               ( ActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_DIM_MISMATCH            ( ActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_MISSING                 ( ActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_START_MISSING           ( ActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_END_MISSING             ( ActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_GENERIC                 ( ActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_TASK_CANCELLED               ( ActionType.RESPONSE_FAIL_HARD, null),

    NONE                                  ( ActionType.NONE, null );

    public static final boolean VERBOSE_LOGS = true;

    public static void logUnhandled(@Nullable GridAction action, @Nullable Object o) {
        Mechano.LOGGER.error(("Response type '" + action + "' is not ") 
            + o == null ? "handled!" : (" included in handler contained within class '" + o.getClass().getSimpleName() + "'!"));
    }

    public static GridAction ofNullcheck(Object a, Object b) {
        if(a == null && b == null) return RESPONSE_FAIL_MISSING;
        if(a == null) return RESPONSE_FAIL_START_MISSING;
        if(b == null) return RESPONSE_FAIL_END_MISSING;
        return RESPONSE_SUCCESS;
    }

    public static GridAction ofNullcheck(Object o) {
        return o == null ? RESPONSE_FAIL_END_MISSING : RESPONSE_SUCCESS;
    }

    public static GridAction ofResult(InteractionResult result) {
        if(result == null) return RESPONSE_FAIL_CANCELLED;
        if(result.consumesAction()) return GridAction.RESPONSE_SUCCESS;
        return GridAction.RESPONSE_FAIL_GENERIC;
    }

    private final ActionType type;
    private final @Nullable ActionTask task;

    <T extends ActionTask> GridAction(ActionType type, @Nullable Class<T> taskClass) {
        Objects.requireNonNull(type);
        this.type = type;
        if(taskClass == null) {
            if(type.isTask())
                throw new IllegalArgumentException("GridAction enum member '" + this + "' has no task, despite being a task type!");
            this.task = null;
            return;
        }
        try { 
            Constructor<T> ctor = taskClass.getDeclaredConstructor(new Class<?>[0]); 
            this.task = ctor.newInstance(new Object[0]);
            if(this.task == null) throw new NullPointerException("Constructor " + ctor + " returned null somehow idk");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unspecified error encoutered configuring GridActionTask for member '" + this + "'");
        }
        Mechano.LOGGER.debug("Loaded task [" + task.getClass().getSimpleName() + "] for '" + this.name().toLowerCase(Locale.ROOT) + "'");
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link ActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link ActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link ActionTask task}
     * @see #broadcastBelligerent(Grid, Collection, Object[])
     */
    public GridAction broadcast(Grid grid, @Nullable Collection<ServerPlayer> trackers, @Nullable Object[] args) {
        if(args == null) args = new Object[0];
        if(task == null) return this;
        return broadcastBelligerent(grid, trackers, task.validateArguments(args));
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link ActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link ActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link ActionTask task}
     * @see #broadcastBelligerent(Grid, Object[])
     */
    public GridAction broadcast(Grid grid, @Nullable Object[] args) {
        if(args == null) args = new Object[0];
        if(task == null) return this;
        args = task.validateArguments(args);
        return broadcastBelligerent(grid, task.validateArguments(args));
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link ActionTask task},
     * should one exist. The packet is always sent from the server to the client.
     * specifically targeting the provided player
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param sp ServerPlayer to send the packet to. This packet will be executed on that player's client.
     * @param args The arguments conforming to the {@link ActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link ActionTask task}
     * @see #broadcastBelligerent(Grid, Object[])
     */
    public GridAction broadcastTo(Grid grid, @Nullable Object[] args, @Nullable ServerPlayer sp) {
        if(args == null) args = new Object[0];
        if(task == null) return this;
        args = task.validateArguments(args);
        if(sp == null) return broadcastBelligerent(grid, args);
        CatnipServices.NETWORK.sendToClient(sp, new GridActionS2CPacket(this, args));
        return this;
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link ActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * This method contains no validity checks to ensure that the arguments are passed correctly
     * or that the packet being sent is not redundant.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link ActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link ActionTask task}
     */
    public GridAction broadcastBelligerent(Grid grid, Collection<ServerPlayer> trackers, @Nullable Object[] args) {
        if(grid.getWorld().isClientSide) CatnipServices.NETWORK.sendToServer(new GridActionC2SPacket(this, args));
        else {
            if(trackers == null || trackers.isEmpty()) CatnipServices.NETWORK.sendToAllClients(new GridActionS2CPacket(this, args));
            else CatnipServices.NETWORK.sendToClients(trackers, new GridActionS2CPacket(this, args));
        }
        return this;
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link ActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * This method contains no validity checks to ensure that the arguments are passed correctly
     * or that the packet being sent is not redundant.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link ActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link ActionTask task}
     */
    public GridAction broadcastBelligerent(Grid grid, @Nullable Object[] args) {
        if(grid.getWorld().isClientSide) CatnipServices.NETWORK.sendToServer(new GridActionC2SPacket(this, args));
        else CatnipServices.NETWORK.sendToAllClients(new GridActionS2CPacket(this, args));
        return this;
    }

    public ActionTask getTask() {
        return this.task;
    }

    public ActionType getActionType() {
        return this.type;
    }

    public boolean isTask() {
        return task != null && getActionType().isTask();
    }

    public InteractionResultHolder<ItemStack> getResultHolder(ItemStack stack) {
        if(!isTask() && this.getActionType().indicatesSuccess())
            return InteractionResultHolder.success(stack);
        return InteractionResultHolder.fail(stack);
    }

    public InteractionResult getResult() {
        if(!this.isTask()) return InteractionResult.PASS;
        if(this.getActionType().indicatesSuccess())
            return InteractionResult.SUCCESS;
        return InteractionResult.FAIL;
    }

    @Override
    public String getSerializedName() {
        return "action_" + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.getSerializedName() + "(" + type + ", " + 
            (task == null ? "no task)" : task.getClass().getSimpleName() + ")");
    }

    public ResourceLocation asResource() {
        return Mechano.asResource(this.getSerializedName());
    }

    public static class ActionRunner implements Supplier<GridAction> {

        private Grid grid;
        private GridAction action;
        private Object[] args = new Object[0];
        private GridReferent<?>[] trackers = new GridReferent<?>[0];

        public ActionRunner() {}

        public ActionRunner(Grid grid, GridAction action) {
            Objects.requireNonNull(grid);
            if(action == null) 
                throw new NullPointerException("Error creating TaskRunner from " + grid + " - The provided task is null!");
            if(!action.isTask()) 
                throw new IllegalArgumentException("Error creating TaskRunner from " + grid + " - The provided action is not a task type!");
            this.grid = grid;
            this.action = action;
        }

        public ActionRunner in(Grid grid) {
            Objects.requireNonNull(grid);
            this.grid = grid;
            return this;
        }

        public ActionRunner action(GridAction action) {
            if(action == null) 
                throw new NullPointerException("Error configuring TaskRunner from " + grid + " - The provided task is null!");
            if(!action.isTask()) 
                throw new IllegalArgumentException("Error configuring TaskRunner from " + grid + " - The provided action is not a task type!");
            this.action = action;
            return this;
        }

        /**
         * Define any number {@link TrackedObject} instances responsible for sending packets. This is used by
         * the action runner to target relevent clients. If you skip this method call, the runner
         * will send packets to all clients when executing. If objects are supplied here,
         * the runner will only send packets to clients that are tracking them.
         * @param trackers varargs array of {@link TrackedObject TrackedObjects}
         * @return This ActionRunner for chaining
         */
        public ActionRunner from(GridReferent<?>... trackers) {
            Objects.requireNonNull(trackers);
            this.trackers = trackers;
            return this;
        }

        /**
         * Define any number {@link TrackedObject} instances responsible for sending packets. This is used by
         * the action runner to target relevent clients. If you skip this method call, the runner
         * will send packets to all clients when executing. If objects are supplied here,
         * the runner will only send packets to clients that are tracking them.
         * @param trackers collection of {@link TrackedObject TrackedObjects}
         * @return This ActionRunner for chaining
         */
        public ActionRunner from(Collection<GridReferent<?>> trackers) {
            Objects.requireNonNull(trackers);
            this.trackers = trackers.toArray(new GridReferent<?>[trackers.size()]);
            return this;
        }

        /**
         * Supply arguments that match the {@link ActionTask#getArgumentTemplate argument template}
         * of the supplied {@link GridAction action} task.
         * @param args varargs array of objects
         * @return This ActionRunner for chaining
         */
        public ActionRunner withArguments(Object... args) {
            Objects.requireNonNull(args);
            this.args = args;
            return this;
        }

        /**
         * Supply arguments that match the {@link ActionTask#getArgumentTemplate argument template}
         * of the supplied {@link GridAction action} task.
         * @param args collection of objects
         * @return This ActionRunner for chaining
         */
        public ActionRunner withArguments(Collection<Object> args) {
            Objects.requireNonNull(args);
            this.args = args.toArray(new Object[args.size()]);
            return this;
        }

        /**
         * Runs this action on whatever side its currently on, and sends packets
         * to hit the runner on the other side.
         * @return A {@link GridAction} describing the results of this immediate execution.
         */
        public GridAction executeImmediately() {
            return get();
        }

        /**
         * Defers the initial execution of this task to the server. This method
         * is callable <strong>only</strong> by clients and is used to manually 
         * schedule {@link GridAction actions} that need access to server-sided
         * data. 
         * <p> This method wraps this TaskRunner's {@link #action internal task}
         * into a {@link GridAction#TASK_REQUEST requester}. This requester is 
         * sent to the server immediately as a result of this call and executed 
         * by Minecraft's server-sided packet handler. A packet may or may not 
         * be sent back to the client as a response, depending on the task's 
         * particular implementation.
         * @return This ActionRunner for chaining.
         */
        @OnlyIn(Dist.CLIENT)
        public GridAction requestRun() {
            Object[] internalArgs = args;
            List<ComponentUUID<?>> trackerIDs = new ArrayList<ComponentUUID<?>>(trackers.length);
            for(int x = 0; x < trackers.length; x++) {
                GridReferent<?> obj = trackers[x];
                if(!(obj instanceof Griddable<?> gobj))
                    continue;
                trackerIDs.add(gobj.getUUID());
            }
            this.args = new Object[] {
                this.action, trackerIDs,
                Arrays.asList(this.action.getTask().validateArguments(internalArgs))
            };
            this.action = GridAction.TASK_REQUEST;
            action.broadcastBelligerent((ClientGrid)grid, args);
            return this.action;
        }

        @Override
        public GridAction get() {
            Objects.requireNonNull(grid);
            ActionTask task = action.getTask();
            if(grid instanceof ClientGrid client) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Client(client, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                return task.executeAsClient(client, args).broadcast(client, args);
            }
            if(grid instanceof ServerGrid server) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Server(server, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                return task.executeAsServer(server, args).broadcast(server, ComponentTracker.collect((ServerLevel)grid.getWorld(), trackers), args);
            }
            return GridAction.RESPONSE_FAIL_GENERIC;
        }

        public GridAction executeAs(ServerPlayer sp) {
            Objects.requireNonNull(sp);
            Objects.requireNonNull(grid);
            ActionTask task = action.getTask();
            if(!(grid instanceof ServerGrid server)) throw new IllegalArgumentException("what");
            GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Server(server, action));
            if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
            return task.executeAsServer(server, args).broadcast(server, ComponentTracker.collect((ServerLevel)grid.getWorld(), trackers), args);
        }
    }

    public static class GridActionTaskArgumentParseException extends RuntimeException {
        public GridActionTaskArgumentParseException(ActionTask task, String message) {
            super("Error while parsing arguments for '" + task.getClass().getSimpleName() + " - " + message);
        }
    }
}

