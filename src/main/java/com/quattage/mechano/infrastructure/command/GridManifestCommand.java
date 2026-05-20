package com.quattage.mechano.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.grid.Grid;
import com.quattage.mechano.grid.ServerGrid;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GridManifestCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> make() {
        return Commands.literal("dump")
            .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if(!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Couldn't dump from non-player source"));
                        return 1;
                    }
                    ServerGrid grid = Grid.server(sp.level());
                    grid.makeManifestAsync(sp);
                    return 1;
                });
    }
}
