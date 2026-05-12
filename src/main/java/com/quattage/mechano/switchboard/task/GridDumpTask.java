package com.quattage.mechano.switchboard.task;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.switchboard.action.ActionTask;
import com.quattage.mechano.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GridDumpTask implements ActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[] { UUID.class };
    }

    @Override   
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        buffer.writeLong(((UUID)args[0]).getMostSignificantBits());
        buffer.writeLong(((UUID)args[0]).getLeastSignificantBits());
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] { new UUID(buffer.readLong(), buffer.readLong()) };
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        // ServerPlayer sp = grid.getServer().getPlayerList().getPlayer((UUID)args[0]);
        return GridAction.TASK_GRID_DUMP;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        MutableComponent message = Component.literal("-- Client-sided dump:\n" + grid.lookup() + "\n--").withStyle(ChatFormatting.GRAY);
        self().sendSystemMessage(message);
        return GridAction.RESPONSE_SUCCESS;
    }
    
}
