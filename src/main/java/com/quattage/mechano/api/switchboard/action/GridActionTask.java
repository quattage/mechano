package com.quattage.mechano.api.switchboard.action;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.GridActions.GridActionTaskArgumentParseException;

import io.netty.buffer.ByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Represents a single, discrete operation that modifies/reacts to the 
 * {@link Grid Grid} in some way. Subclasses are registered
 * and stored in the {@link GridActions} enum statically, so these classes
 * can't have meaningful constructors.
 */
public interface GridActionTask {

    /**
     * The series of arguments expected by this particular task. This method is used
     * to check the arguments provided by internal method calls and throw errors if
     * things aren't correct.
     * @return An array of class objects in the same order as is expected in 
     * {@link #encode}, {@link #executeAsServer}, and {@link #executeAsClient}
     */
    @Nullable Class<?>[] getArgumentTemplate();

    void encode(Object[] args, ByteBuf buffer);
    @Nullable Object[] decode(ByteBuf buffer);

    static void validateArguments(GridActions action, GridActionTask task, @Nullable Object... args) {
        Class<?>[] template = task.getArgumentTemplate();
        if(template == null) template = new Class[0];
        if(args == null) args = new Object[0];
        if(template.length != args.length) {
            throw new GridActionTaskArgumentParseException(action, task, "Incorrect number of arguments supplied! (got " 
                + args.length + ", expected" + template.length);
        }
        for(int x = 0; x < template.length; x++) {
            Class<?> expected = template[x];
            Object arg = args[x];
            if(arg == null || expected.getClass().isInstance(arg)) continue;
            throw new GridActionTaskArgumentParseException(action, task, "Bad argument type at position " + x + " - expected '" 
                + expected.getSimpleName() + "', got '" + arg == null ? "null'!" : (arg.getClass().getSimpleName() + "'"));
        }
    }

    @ApiStatus.NonExtendable
    default GridActions executeAsServer(ServerGrid grid, Object... args) { return executeAsServer(0, grid, args); }

    /**
     * The logic contained within this method is run once per task and shouldn't access or store/modify
     * any non-static variables within the scope of this class. <p>
     * @param attempt Attempt number. Can safely be ignored for most implementations. This number will
     * be <code>> 0</code> in cases where this task is scheduled in a buffer that failed a previous run.
     * @param grid The {@link ServerGrid} within the current world context. Contains access to the world and Grid API data so that they can be modified.
     * @param args Any number of wrapped arguments, conforming to this task's serialized format {@link #getArgumentTemplate() template}.
     * @return {@link GridActions A GridAction} to prompt a response as a result of this task's execution.
     */
    GridActions executeAsServer(int attempt, ServerGrid grid, Object... args);

    @OnlyIn(Dist.CLIENT)
    @ApiStatus.NonExtendable
    default GridActions executeAsClient(ClientGrid grid, Object... args) { return executeAsClient(0, grid, args); }

    /**
     * The logic contained within this method is run once per task and shouldn't access or store/modify
     * any non-static variables within the scope of this class. <p>
     * <h3>Remember to annotate client-specific implementations with the appropriate <code>@OnlyIn</code></h3>
     * @param attempt Attempt number. Can safely be ignored for most implementations. This number will
     * be <code>> 0</code> in cases where this task is scheduled in a buffer that failed a previous run.
     * @param grid The {@link ClientGrid} within the current world context. Contains access to the world and Grid API data so that they can be modified.
     * @param args Any number of wrapped arguments, conforming to this task's serialized format {@link #getArgumentTemplate() template}.
     * @return A {@link GridActions GridAction} to prompt a response as a result of this task's execution.
     */
    @OnlyIn(Dist.CLIENT)
    GridActions executeAsClient(int attempt, ClientGrid grid, Object... args);
}
