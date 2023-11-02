package com.quattage.mechano.foundation.electricity.core.connection;

import com.quattage.mechano.foundation.electricity.NodeBank;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/***
 * A "Fake" connection constitutes a connection that doesn't yet have a proper destination. Instead,
 * it holds an Entity as its destination point. This allows BlockEntities to draw wires
 * from their ElectricNodes to any other entity, including players.
 */
public class FakeNodeConnection extends NodeConnection {

    /***
     * The Entity to attach this FakeNodeConnection to
     */
    private Entity attachedEntity;

    /***
     * The ID of this FakeNodeConnection's source ElectricNode.
     * This is used when realizing this connection.
     */
    private int sourceID;

    public FakeNodeConnection(WireSpool spoolType, int sourceID, Vec3 sourcePos, Entity placer, BlockPos parentPos) {
        super(parentPos);
        this.sourcePos = sourcePos;
        this.attachedEntity = placer;
        this.sourceID = sourceID;
        super.setAge(-1);
        destPos = placer.position().add(0.0D, (double)placer.getEyeHeight() * 0.7d, 0.0d);
        this.spoolType = spoolType;
    }

    /***
     * Updates the destination position of this FakeNodeConnection.
     */
    @Override
    public void updatePosition(float pTicks) {
        destPos = attachedEntity.getRopeHoldPosition(pTicks);
    }

    public Entity getAttachedEntity() {
        return attachedEntity;
    }
    
    public int getSourceID() {
        return sourceID;
    }

    @Override
    public boolean shouldIgnoreFrustrum() {
        return attachedEntity instanceof Player;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && attachedEntity != null;
    }

	@Override
	public CompoundTag writeTo(CompoundTag in) {
		return in;
	}

    @Override
    protected boolean setNeedsLerped() {
        return true;
    }

    @Override
    protected boolean setTransferPower() {
        return false;
    }
}
