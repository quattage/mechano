package com.quattage.mechano.core.blockEntity;

import java.util.List;

import com.quattage.mechano.core.nbt.ITagHelper;
import com.quattage.mechano.core.nbt.TagManager;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.GrindstoneEvent.OnplaceItem;

public abstract class SyncableBlockEntity extends SmartBlockEntity {

    public static final TagManager data = new TagManager();
    private boolean wasContraption = false;

    public SyncableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    private class TagProperty implements ITagHelper {
        
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(connection, packet);
        this.load(packet.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /***
     * When extending from SyncableBLockEntity, using
     * {@link #setData() setData()} instead of write is reccomended.
     * It will deal with server/client syncing for you
     */
    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {
        setData();
        data.writeAll(nbt);
        super.write(nbt, clientPacket);
    }

    /***
     * When extending from SyncableBLockEntity, using
     * {@link #getData() getData()} instead of read is reccomended.
     * It will deal with server/client syncing for you
     */
    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        data.readFrom(nbt);
        getData(data);
    }

    /*** 
     * This is where you store values in the TagManager. If they don't exist, they'll be added
     * automatically.
     * Called before write(). Use this where you'd normally use write().
     */
    protected abstract void setData();

    /*** 
     * This is where you pull values from the TagManager (and, by extension, NBT itself). 
     * Called after read(). Use this where you'd normally use read().
     * @param data TagManager to get values from
     */
    protected abstract void getData(TagManager data);

    @Override
    public CompoundTag getUpdateTag() {
        if(data.needsSyncing()) 
            return data.writeSyncable(super.getUpdateTag());
        return super.getUpdateTag();
    }
}
