package com.quattage.mechano.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.switchboard.action.GridAction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GridPeekCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> make() {
        return Commands.literal("peek")
            .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if(!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Couldn't peek from non-player source"));
                        return 1;
                    }
                    Grid.server(sp).initiateTask(GridAction.TASK_GRID_PEEK).executeAs(sp);
                    return 1;
                });
    }
}
