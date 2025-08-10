package com.quattage.mechano.infrastructure.command;

import java.util.UUID;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.foundation.gridapi.ServerGrid;
import com.quattage.mechano.foundation.gridapi.SidedGridDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GridWipeCommand {
    
    private static UUID requester = null;
    private static long requestTime = 0L;

    public static ArgumentBuilder<CommandSourceStack, ?> make() {
        return Commands.literal("wipe")
            .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if(!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Couldn't dump from non-player source"));
                        return 1;
                    }

                    ServerGrid grid = SidedGridDispatcher.server(sp);
                    if(grid.matrices.isEmpty()) {
                        requester = null;
                        requestTime = 0L;
                        source.sendFailure(Component.literal("No grid information has been saved to '" + sp.level().dimension().location().toString() + "'").withStyle(style -> style.withColor(ChatFormatting.RED)));
                        return 1;
                    }

                    if(requester == null) {
                        requester = sp.getUUID();
                        requestTime = System.currentTimeMillis();
                        source.sendFailure(Component.literal("Are you sure? Wiping all grid data will destroy all wires and compatable blocks! Enter the command again to confirm.").withStyle(style -> style.withColor(ChatFormatting.RED)));
                        return 1;
                    }

                    if(!requester.equals(sp.getUUID())) {
                        source.sendFailure(Component.literal("A pending wipe request is still being processed.").withStyle(style -> style.withColor(ChatFormatting.RED)));
                        return 1;
                    }

                    long now = System.currentTimeMillis();
                    if(now - requestTime > 2000) {
                        requester = null;
                        requestTime = 0L;
                        source.sendFailure(Component.literal("The previous wipe request timed out.").withStyle(style -> style.withColor(ChatFormatting.RED)));
                        return 1;
                    }

                    requester = null;
                    requestTime = 0L;
                    source.sendSuccess(() -> Component.literal("Wiping all grid data...").withStyle(style -> style.withColor(ChatFormatting.GRAY)), false);
                    return 1;
                });
        }
    }
