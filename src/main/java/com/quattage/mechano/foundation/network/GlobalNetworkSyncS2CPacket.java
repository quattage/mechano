package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GlobalNetworkSyncS2CPacket implements Packetable {
    private final BlockPos target;
    private final int energy;

    public GlobalNetworkSyncS2CPacket(int energy, BlockPos target) {
        this.target = target;
        this.energy = energy;
    }

    public GlobalNetworkSyncS2CPacket(FriendlyByteBuf buf) {
        this.energy = buf.readInt();
        this.target = buf.readBlockPos();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(energy);
        buf.writeBlockPos(target);
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(target) instanceof ElectricBlockEntity ebe) {
                ebe.batteryBank.setEnergyStored(energy);
            }
        });
        return true;
    }
}