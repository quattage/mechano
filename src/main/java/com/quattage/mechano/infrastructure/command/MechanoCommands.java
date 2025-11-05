package com.quattage.mechano.infrastructure.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.quattage.mechano.Mechano;

import net.createmod.catnip.command.CatnipCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class MechanoCommands {
    
    @SubscribeEvent
    public static void register(RegisterCommandsEvent evt) {
        LiteralCommandNode<CommandSourceStack> root = evt.getDispatcher().register(MechanoCommands.makeRoot());
        root.addChild(CatnipCommands.buildRedirect("grid", MechanoCommands.makeGrid()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeRoot() {
        return Commands.literal(Mechano.ID)
            .requires(stack -> stack.hasPermission(0));
    }

    private static LiteralCommandNode<CommandSourceStack> makeGrid() {
        return Commands.literal("grid")
            .build();
    }
}
