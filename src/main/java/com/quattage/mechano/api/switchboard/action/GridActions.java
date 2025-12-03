package com.quattage.mechano.api.switchboard.action;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.task.GridTaskExecuteEvent;
import com.quattage.mechano.api.switchboard.task.LinkJointsTask;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.common.NeoForge;

public enum GridActions implements StringRepresentable {

    TASK_LINK_JOINTS                      ( GridActionType.TASK_GENERIC, LinkJointsTask.class ),

    RESPONSE_SUCCESS                      ( GridActionType.RESPONSE_SUCCESS, null),
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

    public static void logUnhandled(@Nullable GridActions action, @Nullable Object o) {
        Mechano.LOGGER.error(("Response type '" + action + "' is not ") 
            + o == null ? "handled!" : (" included in handler contained within class '" + o.getClass().getSimpleName() + "'!"));
    }

    private final GridActionType type;
    private final @Nullable GridActionTask task;

    <T extends GridActionTask> GridActions(GridActionType type, @Nullable Class<T> taskClass) {
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
     * Runs the task associated with <code>action</code>, provided the supplied 
     * <code>args</code> match what is expected by the task. If this is not the case,
     * this method will throw exceptions.
     * @param grid The {@link Grid power grid} instance passed to the task
     * @param action Any {@link GridActions action} with a valid task to run
     * @param args Any number of arguments expected by the given task. 
     * See {@link GridActionTask#getArgumentTemplate}
     * @return A new {@link GridActions action} instance representing a response to the action that was run.
     * This response indidcates success or failure.
     */
    public GridActions run(Grid grid, @Nullable Object... args) {
        if(!this.isTask()) throw new IllegalArgumentException("Attempted to run GridAction '" + this + "' but this action has no task!");
        if(grid == null) throw new NullPointerException("Attempted to run GridAction '" + this + "' on a null grid!");
        GridActionTask task = this.getTask();
        GridActionTask.validateArguments(this, task, args);
        if(grid instanceof ClientGrid client) {
            GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Client(client, this));
            if(event.isCanceled()) return GridActions.RESPONSE_FAIL_CANCELLED;
            return task.executeAsClient(client, args);
        }
        if(grid instanceof ServerGrid server) {
            GridTaskExecuteEvent<?> event = NeoForge.EVENT_BUS.post(new GridTaskExecuteEvent.Server(server, this));
            if(event.isCanceled()) return GridActions.RESPONSE_FAIL_CANCELLED;
            return task.executeAsServer(server, args);
        }
        return GridActions.RESPONSE_FAIL_GENERIC;
    }

    /**
     * Runs the task associated with <code>action</code>, provided the supplied 
     * <code>args</code> match what is expected by the task. If this is not the case,
     * this method will throw exceptions.
     * @param world World to get the {@link Grid power grid} instance from. This instance
     * will be automatically attached to the world and initialized if it doesn't already exist.
     * @param action Any {@link GridActions action} with a valid task to run
     * @param args Any number of arguments expected by the given task. 
     * See {@link GridActionTask#getArgumentTemplate}
     * @return An {@link GridActions action} representing a response to the task 
     * that was run. This response indidcates success or failure.
     */
    public GridActions run(LevelReader world, @Nullable Object... args) {
        if(world == null) throw new NullPointerException("Attempted to run GridAction '" + this + "' on a null world!");
        return run(Grid.getUnsided(world), this, args);
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

    public static class GridActionTaskArgumentParseException extends RuntimeException {
        public GridActionTaskArgumentParseException(GridActions action, GridActionTask task, String message) {
            super("Error while parsing arguments for task '" + task.getClass().getSimpleName() + "' from action '" + action + "' - " + message);
        }
    }
}
