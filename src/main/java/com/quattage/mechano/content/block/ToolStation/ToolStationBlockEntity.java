package com.quattage.mechano.content.block.ToolStation;

import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.ToolStation.ToolStationBlock.WideBlockModelType;
import com.quattage.mechano.core.effects.BoundParticleSpawner;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class ToolStationBlockEntity extends SmartTileEntity implements MenuProvider {

    // storage data
    public final ToolStationInventory INVENTORY;
    private BoundParticleSpawner particle;

    // progress data
    private int heat = 0;
    private int maxHeat = 25;

    // gui information

    public ToolStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        INVENTORY = new ToolStationInventory(this);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        particle = new BoundParticleSpawner((BlockEntity)this)
            .withDensity(5)
            .withRandom(0.3f)
            .withDensity(6)
            .toNearestCenter()
            .toOffset(0, 0.39, 0);
    }

    public void sendToContainer(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(getBlockPos());
		buffer.writeNbt(getUpdateTag());
	}

    @Override
    public Component getDisplayName() {
        return Mechano.asKey("toolStationBlockEntity");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ToolStationContainer.create(containerId, playerInventory, this);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        heat = nbt.getInt("tool_station_operations");
        super.read(nbt, clientPacket);
        
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {
        nbt.putInt("tool_station_operations", heat);
        super.write(nbt, clientPacket);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ToolStationBlockEntity entity) {
        if(level.isClientSide) {
            return;
        }

        /*
        if(hasRecipe(entity)) {
            
        } 
        */
        
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        // no implemtation
    }

    public void playUpSound(BlockState state, WideBlockModelType blockType) {
        BoundParticleSpawner customParticle = particle;
        Mechano.log("pee " + state);
        if(level.isClientSide)
            return;
        switch (blockType) {
            case BASE:
                particle.spawn();
                particle.toDirectionalOffset(state.getValue(ToolStationBlock.FACING).getClockWise())
                    .spawn();
                break;
            case FORGED:
                customParticle = particle.withCustom(Blocks.NETHERITE_BLOCK);
                customParticle.spawn();
                customParticle.toDirectionalOffset(state.getValue(ToolStationBlock.FACING).getClockWise())
                    .spawn();
                break;
            case HEATED:
                customParticle = particle.withCustom(Blocks.NETHERITE_BLOCK);  
                customParticle.spawn();
                customParticle.toDirectionalOffset(state.getValue(ToolStationBlock.FACING).getClockWise())
                    .spawn();
                break;
            case MAXIMIZED:
                customParticle = particle.withCustom(Blocks.NETHERITE_BLOCK);  
                customParticle.spawn();
                customParticle.toDirectionalOffset(state.getValue(ToolStationBlock.FACING).getClockWise())
                    .spawn();
                break;
            default:
                return;
        }
    }
}