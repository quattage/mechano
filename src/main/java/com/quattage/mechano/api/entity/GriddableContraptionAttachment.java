
package com.quattage.mechano.api.entity;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.anchor.AnchorCollection;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.anchor.AnchorCollection.AliasedAnchorMap;
import com.quattage.mechano.api.anchor.AnchorCollection.AnchorArray;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.ContraptionUUID;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.identifier.VoxelUUID;
import com.quattage.mechano.api.landmark.GridCatenary;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public final class GriddableContraptionAttachment extends GriddableEntityAttachment {

    public GriddableContraptionAttachment(IAttachmentHolder holder) {
        super(holder);
        if(!(holder instanceof AbstractContraptionEntity))
            throw new IllegalArgumentException("GriddableContraptionAttachments can only be attached to ContraptionEntities, got " + holder + "!");
        anchors = null;
    }

    public Contraption getContraption() {
        return entity == null ? null : ((AbstractContraptionEntity)entity).getContraption();
    }

    @Override
    public GridUUID createSupplementaryAddress() {
        return new ContraptionUUID(entity.getUUID(), BlockPos.ZERO, 0);
    }

    @Override
    public String describeState() {
        return "Entity '" + entity.getName().getString() + "'";
    }

    @Override
    public AnchorCollection getAnchors() {
        if(anchors == null) anchors = new AliasedAnchorMap(2);
        return anchors;
    }

    public void takeControlOver(GriddableBlockEntity host, BlockPos structurePos) {
        host.getSurrogate().forceHostHandoff(this);
        if(getWorld().isClientSide) {
            subsumeAnchorPoints(host, structurePos);
            return;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void subsumeAnchorPoints(Griddable<?> host, BlockPos structurePos) {
        AnchorCollection hostAnchors = host.getAnchors();
        if(!(hostAnchors instanceof AnchorArray contents)) 
            throw new IllegalArgumentException("Failed to add alias - AnchorCollection belonging to '" + host.getClass().getSimpleName() + "' is not an AnchorArray!");
        VoxelUUID newAddr = new VoxelUUID(structurePos, 0);
        for(int x = 0; x < hostAnchors.size(); x++) {
            AnchorPoint ap = hostAnchors.get(x);
            ap.replaceAddress(newAddr.indexedCopy(x));
        }
        ((AliasedAnchorMap)anchors).combineWith(newAddr, contents);
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

    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        return null;
    }


}
