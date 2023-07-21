package com.quattage.mechano.core.electricity.node.connection;

import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.electricity.node.NodeBank;

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
    private String sourceID;

    public FakeNodeConnection(WireSpool spoolType, String sourceID, Vec3 sourcePos, Entity placer) {
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

    /***
     * Builds a new ElectricNodeConnection from the data in this LiveConnection
     * @return
     */
    public ElectricNodeConnection realize(NodeBank fromBank, NodeBank destinationBank, String destinationId) {
        return new ElectricNodeConnection(spoolType, fromBank, sourcePos, destinationBank, destinationId);
    }

    public Entity getAttachedEntity() {
        return attachedEntity;
    }
    
    public String getSourceID() {
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
