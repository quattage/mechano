package com.quattage.mechano.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.foundation.gridapi.SidedGridDispatcher;

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
                    if(SidedGridDispatcher.MANIFEST.isQueued()) {
                        source.sendFailure(Component.literal("A manifest is currently being processed!").withStyle(style -> style.withColor(ChatFormatting.RED)));
                        return 1;
                    }
                    if(SidedGridDispatcher.MANIFEST.enqueueRequestFrom(sp)) {
                        source.sendSuccess(() -> Component.literal("Enqueued request").withStyle(ChatFormatting.AQUA), false);
                        return 1;
                    }

                    source.sendFailure(Component.literal("No grid information has been saved to '" + sp.level().dimension().location().toString() + "'"));
                    return 1;
                });
    }
}

