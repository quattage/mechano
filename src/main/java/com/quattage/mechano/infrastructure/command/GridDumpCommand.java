package com.quattage.mechano.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.api.Grid;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GridDumpCommand {
    
    public static ArgumentBuilder<CommandSourceStack, ?> make() {
        return Commands.literal("dump")
            .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if(!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Couldn't dump from non-player source"));
                        return 1;
                    }
                    Grid grid = Grid.server(sp);
                    String output = grid.writeAllLinks();
                    source.sendSuccess(() -> Component.literal("Dump result:\n" + output).withStyle(ChatFormatting.GRAY), false);
                    return 1;
                });
    }

}
