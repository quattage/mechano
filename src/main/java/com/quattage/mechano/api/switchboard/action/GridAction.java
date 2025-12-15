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
import com.quattage.mechano.api.switchboard.GridActionC2SPacket;
import com.quattage.mechano.api.switchboard.GridActionS2CPacket;
import com.quattage.mechano.api.switchboard.task.LinkJointsTask;
import com.quattage.mechano.api.switchboard.task.RequestActionTask;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.TrackedObject;

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

    TASK_LINK_JOINTS                      ( GridActionType.TASK_GENERIC, LinkJointsTask.class ),
    TASK_REQUEST                          ( GridActionType.TASK_GENERIC, RequestActionTask.class ),

    RESPONSE_SUCCESS                      ( GridActionType.RESPONSE_SUCCESS, null),
    RESPONSE_FAIL_DUPLICATE_ELEMENT       ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_ELEMENT_FULL            ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_CLOSE               ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_FAR                 ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_INCOMPATIBLE            ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_CANCELLED               ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_DIM_MISMATCH            ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_REFERRENT_MISSING       ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_GENERIC                 ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_TASK_CANCELLED               ( GridActionType.RESPONSE_FAIL_HARD, null),

    NONE                                  ( GridActionType.NONE, null );

    public static final boolean VERBOSE_LOGS = true;

    public static void logUnhandled(@Nullable GridAction action, @Nullable Object o) {
        Mechano.LOGGER.error(("Response type '" + action + "' is not ") 
            + o == null ? "handled!" : (" included in handler contained within class '" + o.getClass().getSimpleName() + "'!"));
    }

    private final GridActionType type;
    private final @Nullable GridActionTask task;

    <T extends GridActionTask> GridAction(GridActionType type, @Nullable Class<T> taskClass) {
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
            if(this.task == null) throw new NullPointerException("constructor " + ctor + " returned null somehow idk");
            Mechano.LOGGER.debug("Loaded task [" + task.getClass().getSimpleName() + "] for '" + this.name().toLowerCase(Locale.ROOT) + "'");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unspecified error encoutered configuring GridActionTask for member '" + this + "'");
        }
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link GridActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link GridActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link GridActionTask task}
     * @see #broadcastBelligerent(Grid, Collection, Object[])
     */
    public GridAction broadcast(Grid grid, @Nullable Collection<ServerPlayer> trackers, @Nullable Object[] args) {
        if(args == null) args = new Object[0];
        if(task == null) return this;
        return broadcastBelligerent(grid, trackers, task.validateArguments(args));
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link GridActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link GridActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link GridActionTask task}
     * @see #broadcastBelligerent(Grid, Object[])
     */
    public GridAction broadcast(Grid grid, @Nullable Object[] args) {
        if(args == null) args = new Object[0];
        if(task == null) return this;
        args = task.validateArguments(args);
        return broadcastBelligerent(grid, task.validateArguments(args));
    }

    /**
     * Sends a packet to schedule this GridAction's associated {@link GridActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * This method contains no validity checks to ensure that the arguments are passed correctly
     * or that the packet being sent is not redundant.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link GridActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link GridActionTask task}
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
     * Sends a packet to schedule this GridAction's associated {@link GridActionTask task},
     * should one exist. The packet is always sent to the opposite side that this method was called from.
     * This method contains no validity checks to ensure that the arguments are passed correctly
     * or that the packet being sent is not redundant.
     * @param grid The grid to use when getting the level, which is used to determine the side
     * @param trackers A collection of ServerPlayer instances, used for targeting clients on the server. Can be <code>null</code> 
     * to send packets to all clients. Ignore this if you're calling this method on the client.
     * @param args The arguments conforming to the {@link GridActionTask#getArgumentTemplate() argument template} of this 
     * GridAction's {@link GridActionTask task}
     */
    public GridAction broadcastBelligerent(Grid grid, @Nullable Object[] args) {
        if(grid.getWorld().isClientSide) CatnipServices.NETWORK.sendToServer(new GridActionC2SPacket(this, args));
        else CatnipServices.NETWORK.sendToAllClients(new GridActionS2CPacket(this, args));
        return this;
    }

    public GridActionTask getTask() {
        return this.task;
    }

    public GridActionType getActionType() {
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
        private TrackedObject[] trackers = new TrackedObject[0];

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
        public ActionRunner from(TrackedObject... trackers) {
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
        public ActionRunner from(Collection<TrackedObject> trackers) {
            Objects.requireNonNull(trackers);
            this.trackers = trackers.toArray(new TrackedObject[trackers.size()]);
            return this;
        }

        /**
         * Supply arguments that match the {@link GridActionTask#getArgumentTemplate argument template}
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
         * Supply arguments that match the {@link GridActionTask#getArgumentTemplate argument template}
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
            List<GridUUID> trackerIDs = new ArrayList<GridUUID>(trackers.length);
            for(int x = 0; x < trackers.length; x++) {
                TrackedObject obj = trackers[x];
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
            GridActionTask task = action.getTask();
            if(grid instanceof ClientGrid client) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Client(client, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                return task.executeAsClient(client, args).broadcast(client, args);
            }
            if(grid instanceof ServerGrid server) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Server(server, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                return task.executeAsServer(server, args).broadcast(server, TrackedObject.collectTrackers((ServerLevel)grid.getWorld(), trackers), args);
            }
            return GridAction.RESPONSE_FAIL_GENERIC;
        }
    }

    public static class GridActionTaskArgumentParseException extends RuntimeException {
        public GridActionTaskArgumentParseException(GridActionTask task, String message) {
            super("Error while parsing arguments for '" + task.getClass().getSimpleName() + " - " + message);
        }
    }
}

