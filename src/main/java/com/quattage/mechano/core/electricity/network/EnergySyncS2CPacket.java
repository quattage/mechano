package com.quattage.mechano.core.electricity.network;

import java.util.function.Supplier;

import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.network.Packetable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class EnergySyncS2CPacket implements Packetable {
    private final BlockPos target;
    private final int energy;

    public EnergySyncS2CPacket(int energy, BlockPos target) {
        this.target = target;
        this.energy = energy;
    }

    public EnergySyncS2CPacket(FriendlyByteBuf buf) {
        this.energy = buf.readInt();
        this.target = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(energy);
        buf.writeBlockPos(target);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(target) instanceof ElectricBlockEntity ebe) {
                ebe.nodes.setEnergyStored(energy);
            }
        });
        return true;
    }
}