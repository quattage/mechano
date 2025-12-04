package com.quattage.mechano.api.switchboard.action;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.GridActionC2SPacket;
import com.quattage.mechano.api.switchboard.GridActionS2CPacket;
import com.quattage.mechano.api.switchboard.task.GridTaskExecuteEvent;
import com.quattage.mechano.api.switchboard.task.LinkJointsTask;
import com.quattage.mechano.api.switchboard.task.RequestActionTask;
import com.quattage.mechano.foundation.tracking.TrackedObject;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

    TASK_PASSTHROUGH                      ( GridActionType.RESPONSE_SUCCESS, null),

    RESPONSE_SUCCESS                      ( GridActionType.RESPONSE_SUCCESS, null),
    RESPONSE_DEFERED                      ( GridActionType.RESPONSE_SUCCESS, null),
    RESPONSE_FAIL_DUPLICATE_ELEMENT       ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_CLOSE               ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_TOO_FAR                 ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_INCOMPATIBLE            ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_FAIL_CANCELLED               ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_DIM_MISMATCH            ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_REFERRENT_MISSING       ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_REFERRENT_MISSING_START ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_REFERRENT_MISSING_END   ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_REFERRENT_MISSING_BOTH  ( GridActionType.RESPONSE_FAIL_HARD, null),
    RESPONSE_FAIL_GENERIC                 ( GridActionType.RESPONSE_FAIL_SOFT, null),
    RESPONSE_TASK_CANCELLED               ( GridActionType.RESPONSE_FAIL_HARD, null),

    NONE                                  ( GridActionType.NONE, null );

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

    public @Nullable GridActionTask getTask() {
        return task;
    }

    public GridActionType getActionType() {
        return type;
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

    public ResourceLocation getKey() {
        return Mechano.asResource(this.getSerializedName());
    }

    public static class ActionRunner implements Supplier<GridAction> {

        private final Grid grid;
        private GridAction action;
        private Object[] args = new Object[0];
        private TrackedObject[] senders = new TrackedObject[0];

        public ActionRunner(Grid grid, GridAction action) {
            if(action == null) 
                throw new NullPointerException("Error creating TaskRunner from " + grid + " - The provided task is null!");
            if(!action.isTask()) 
                throw new IllegalArgumentException("Error creating TaskRunner from " + grid + " - The provided action is not a task type!");
            this.grid = grid;
            this.action = action;
        }

        /**
         * Define any number {@link TrackedObject} instances responsible for sending packets. This is used by
         * the action runner to target relevent clients. If you skip this method call, the runner
         * will send packets to all clients when executing. If objects are supplied here,
         * the runner will only send packets to clients that are tracking them.
         * @param senders varargs array of {@link TrackedObject TrackedObjects}
         * @return This ActionRunner for chaining
         */
        public ActionRunner from(TrackedObject... senders) {
            Objects.requireNonNull(senders);
            this.senders = senders;
            return this;
        }

        /**
         * Supply arguments that match the {@link GridActionTask#getArgumentTemplate argument template}
         * of the supplied {@link GridAction action} task.
         * @param args varargs array
         * @return This ActionRunner for chaining
         */
        public ActionRunner args(Object... args) {
            Objects.requireNonNull(args);
            this.args = args;
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
         * Hands off the initial execution of this task to the server. This method
         * is callable by clients to manually schedule some server-dependent logic.
         * <p> This method wraps this TaskRunner's {@link #action internal task}
         * into a {@link GridAction#TASK_REQUEST requester}. This task will be
         * sent to the server immediately and executed by Minecraft's server-sided
         * packet handler. A packet may or may not be sent back to the client as a 
         * response, depending on the task's particular implementation.
         * @return This ActionRunner for chaining.
         */
        @OnlyIn(Dist.CLIENT)
        public GridAction requestRun() {
            Object[] internalArgs = args;
            this.args = new Object[] {
                this.action,
                this.senders,
                internalArgs
            };
            this.action = GridAction.TASK_REQUEST;
            return get();
        }

        @Override
        public GridAction get() {
            Objects.requireNonNull(grid);
            GridActionTask task = action.getTask();
            GridActionTask.validateArguments(action, task, args);
            if(grid instanceof ClientGrid client) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Client(client, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                GridAction response = task.executeAsClient(client, args);
                if(!response.isTask() || response.getActionType().indicatesFailure())
                    return response;
                CatnipServices.NETWORK.sendToServer(new GridActionC2SPacket(response, args));
                return response;
            }
            if(grid instanceof ServerGrid server) {
                GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Server(server, action));
                if(event.isCanceled()) return GridAction.RESPONSE_FAIL_CANCELLED;
                GridAction response = task.executeAsServer(server, args);
                if(!response.isTask() || response.getActionType().indicatesFailure())
                    return response;
                if(senders == null || senders.length <= 0)
                    CatnipServices.NETWORK.sendToAllClients(new GridActionS2CPacket(response, args));
                else {
                    for(TrackedObject obj : senders) 
                        obj.sendToClientsTracking((ServerLevel)grid.getWorld(), new GridActionS2CPacket(response, args));
                }
                return response;
            }
            return GridAction.RESPONSE_FAIL_GENERIC;
        }
    }

    public static class GridActionTaskArgumentParseException extends RuntimeException {
        public GridActionTaskArgumentParseException(GridAction action, GridActionTask task, String message) {
            super("Error while parsing arguments for task '" + task.getClass().getSimpleName() + "' from action '" + action + "' - " + message);
        }
    }
}

