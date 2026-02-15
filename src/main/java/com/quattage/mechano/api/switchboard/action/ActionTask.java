package com.quattage.mechano.api.switchboard.action;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.TopologyProcessQueue.RemovalCache;
import com.quattage.mechano.api.switchboard.action.GridAction.GridActionTaskArgumentParseException;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Represents a single, discrete operation that modifies/reacts to the 
 * {@link Grid Grid} in some way. Subclasses are registered
 * and stored in the {@link GridAction} enum statically, so these classes
 * can't have meaningful constructors.
 */
public interface ActionTask {

    /**
     * The series of arguments expected by this particular task. This method is used
     * to check the arguments provided by internal method calls and throw errors if
     * things aren't correct.
     * @return An array of class objects in the same order as is expected in 
     * {@link #encode}, {@link #executeAsServer}, and {@link #executeAsClient}
     */
    @Nullable Class<?>[] getArgumentTemplate();

    void dynamicEncode(Object[] args, ByteBuf buffer);
    @Nullable Object[] dynamicDecode(ByteBuf buffer);

    /**
     * Validates this GridActionTask against the series of arguments provided by its {@link #getArgumentTemplate template}.
     * Individual tasks may override this method to provide fault correction capabilities or additional throws.
     * @param args The array of arguments that this task will be run with
     * @return The arguments array. Most of the time, this will be the exact same instance, but some implementations may
     * modify this arguments array to replace references (e.g. correcting <code>nulls</code>).
     */
    default Object[] validateArguments(boolean allowNulls, @Nullable Object... args) {
        Class<?>[] template = getArgumentTemplate();
        if(template == null) template = new Class[0];
        if(args == null) args = new Object[0];
        if(template.length != args.length) {
            throw new GridActionTaskArgumentParseException(this, "Incorrect number of arguments supplied! (got " 
                + args.length + ", expected " + template.length + ")");
        }
        for(int x = 0; x < template.length; x++) {
            Class<?> expected = template[x];
            Object arg = args[x];
            if(allowNulls && arg == null) continue;
            if(expected.isInstance(arg)) continue;
            if(expected == UUID.class && arg == null) continue;
            throw new GridActionTaskArgumentParseException(this, "Bad argument type at position " + x + " - expected '" 
                + expected.getSimpleName() + "', got '" + (arg == null ? ("null'!") : (arg.getClass().getSimpleName() + "'")));
        }
        return args;
    }

    @ApiStatus.NonExtendable
    default GridAction executeAsServer(ServerGrid grid, Object... args) { return executeAsServer(0, grid, args); }

    /**
     * The logic contained within this method is run once per task and shouldn't access or store
     * any non-static variables within the scope of this class. <p>
     * @param attempt Attempt number. Can safely be ignored for most implementations. This number will
     * be <code>> 0</code> in cases where this task is scheduled in a buffer that failed a previous run.
     * @param grid The {@link ServerGrid} within the current world context. Contains access to the world and Grid API data so that they can be modified.
     * @param args Any number of wrapped arguments, conforming to this task's serialized format {@link #getArgumentTemplate() template}.
     * @return {@link GridAction A GridAction} to prompt a response as a result of this task's execution.
     */
    GridAction executeAsServer(int attempt, ServerGrid grid, Object... args);

    @OnlyIn(Dist.CLIENT)
    @ApiStatus.NonExtendable
    default GridAction executeAsClient(ClientGrid grid, Object... args) { return executeAsClient(0, grid, args); }

    /**
     * The logic contained within this method is run once per task and shouldn't access or store
     * any non-static variables within the scope of this class. <p>
     * <h3>Remember to annotate client-specific implementations with the appropriate <code>@OnlyIn</code></h3>
     * @param attempt Attempt number. Can safely be ignored for most implementations. This number will
     * be <code>> 0</code> in cases where this task is scheduled in a buffer that failed a previous run.
     * @param grid The {@link ClientGrid} within the current world context. Contains access to the world and Grid API data so that they can be modified.
     * @param args Any number of wrapped arguments, conforming to this task's serialized format {@link #getArgumentTemplate() template}.
     * @return A {@link GridAction GridAction} to prompt a response as a result of this task's execution.
     */
    @OnlyIn(Dist.CLIENT)
    GridAction executeAsClient(int attempt, ClientGrid grid, Object... args);


    /**
     * This method is guaranteed to be called at the right time 
     * to avoid concurrent modifications to the ServerGrid's 
     * topology. Any tasks that directly modify the indexer, 
     * netlist, vectors, or component states must defer their 
     * implementations to this method, rather than directly in
     * {@link #executeAsServer}
     * @param grid to operate within
     * @param removals A container to mark nodes and links for removal - The grid will handle removing them for you
     * @param args Any number of wrapped arguments
     * @return A {@link GridAction action} to indicate the success/failure of this execution
     */
    default GridAction executeTopological(ServerGrid grid, RemovalCache removals, Object[] args) {
        return GridAction.NONE;
    }

    /**
     * Log a message associated with this task's execution. Implementations 
     * or API users may call this method to print debug messages.
     * @param grid Grid to log for
     * @param attempt The attempt # of this execution (Optional, defaults to <code>-1</code>)
     * @param args The arguments that were used
     */
    default void logExecution(Grid grid, Object... args) {
        logExecution(grid, -1, args);
    }

    /**
     * Log a message associated with this task's execution. Implementations 
     * or API users may call this method to print debug messages.
     * @param grid Grid to log for
     * @param attempt The attempt # of this execution (Optional, defaults to <code>-1</code>)
     * @param args The arguments that were used
     */
    default void logExecution(Grid grid, int attempt, Object... args) {
        Objects.requireNonNull(grid);
        String summary = collectArgsAsString(args);
        grid.info("executing '" + this.getClass().getSimpleName() + "'" + (attempt > 0 ? ", attempt " 
            + attempt + ": " : ": ") + (summary.isEmpty() ? "no arguments" : "\nArguments: \n" + summary) + "\n\n");
    }

    default String collectArgsAsString(Object... args) {
        String summary = "Arguments:";
        if(args == null || args.length <= 0) return summary + "\n  none";
        for(Object obj : args) {
            if(obj == null) summary += "  - null\n";
            else summary += "\n  - " + obj.getClass().getSimpleName() + ":\n     " + obj.toString() + ", ";
        }
        return summary.substring(0, summary.length() - 1);
    }


    /**
     * @return The LocalPlayer of this client
     */
    @OnlyIn(Dist.CLIENT)
    default LocalPlayer self() {
        Minecraft mc = Minecraft.getInstance();
        if(mc == null) throw new NullPointerException("what");
        if(mc.player == null) throw new NullPointerException("LocalPlayer is not reachable in the current context.");
        return mc.player;
    }
}
