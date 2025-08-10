package com.quattage.mechano.foundation.gridapi.entity;

import com.quattage.mechano.foundation.gridapi.anchor.AnchorArray;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorArray.DynamicAnchorArray;
import com.quattage.mechano.foundation.gridapi.anchor.SurrogateNode;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.ContraptionUUID;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class GriddableContraptionAttachment extends GriddableEntityAttachment {

    private Int2ObjectOpenHashMap<GridUUID> composite = new Int2ObjectOpenHashMap<>();

    public GriddableContraptionAttachment(IAttachmentHolder holder) {
        super(holder);
        if(!(holder instanceof AbstractContraptionEntity))
            throw new IllegalArgumentException("GriddableContraptionAttachments can only be attached to ContraptionEntities, got " + holder + "!");
        anchors = new DynamicAnchorArray();
    }

    @Override // anchor construction is automatic for contraption attachments
    public void constructAnchors(Builder anchors) {}

    @Override
    public AnchorArray getAnchors() {
        throw new UnsupportedOperationException("GriddableContraptionAttachments cannot host anchors on their own! This functionality is deferred to local BlockEntities within the contraption. (Did you attempt to query a Contraption with an EntityUUID?)");
    }

    public Contraption getContraption() {
        return entity == null ? null : ((AbstractContraptionEntity)entity).getContraption();
    }

    public void markParticipatingSubsurrogate(SurrogateNode node, GridUUID id) {
        int index = node.getOwnerMatrix() == null ? -1 : node.getOwnerMatrix().getIndex();
        if(index < 0) return;
        composite.put(index, id);
    }

    public Int2ObjectOpenHashMap<GridUUID> getCompositeUUIDs() {
        return composite;
    }

    @Override
    public GridUUID createSupplementaryAddress() {
        return new ContraptionUUID(entity.getUUID(), BlockPos.ZERO, 0);
    }

    @Override
    public String describeState() {
        return "Contraption '" + entity.getName().toString() + "' @" + entity.getUUID();
    }

    @Override
    public boolean isMovable() {
        return true;
    }

    // TODO for now contraption attachments' AnchorPoints are always hidden, but that doesn't always have to be the case.
    @Override
    public boolean isInteractable() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return false;
    }
}
