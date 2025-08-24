package com.quattage.mechano.foundation.api.entity;

import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.ContraptionUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class GriddableContraptionAttachment extends GriddableEntityAttachment {

    private Int2ObjectOpenHashMap<GridUUID> composite = new Int2ObjectOpenHashMap<>();

    public GriddableContraptionAttachment(IAttachmentHolder holder) {
        super(holder);
        if(!(holder instanceof AbstractContraptionEntity))
            throw new IllegalArgumentException("GriddableContraptionAttachments can only be attached to ContraptionEntities, got " + holder + "!");
        anchors = null;
    }

    @ApiStatus.Internal
    public GriddableContraptionAttachment() {
        super(null);
        anchors = null;
    }

    public GriddableContraptionAttachment bindTo(AbstractContraptionEntity entity) {
        Objects.requireNonNull(entity);
        this.entity = entity;
        return this;
    }

    @Override // anchor construction is automatic for contraption attachments
    public void constructAnchors(Builder anchors) {}

    @Override
    public AnchorArray getAnchors() {
        throw new UnsupportedOperationException("GriddableContraptionAttachments cannot host anchors on their own! This functionality is deferred to local BlockEntities within the contraption. (Did you attempt to query a Contraption with an EntityUUID?)");
    }

    @Override
    public AnchorPoint getAnchor() {
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
        return "Entity '" + entity.getName().getString() + "'";
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

    public CompoundTag writeCompositeTo(CompoundTag in) {
        ListTag list = new ListTag(composite.size());
        for(Int2ObjectMap.Entry<GridUUID> subsurrogate : composite.int2ObjectEntrySet()) {
            CompoundTag addrTag = new CompoundTag();
            subsurrogate.getValue().writeTo(addrTag);
            list.add(addrTag);
        }
        in.put("GridComposite", list);
        return in;
    }

    public void readCompositeFrom(ListTag list) {
        composite.ensureCapacity(list.size());
        for(int x = 0; x < list.size(); x++) {
            CompoundTag member = list.getCompound(x);
            composite.put(x, UUIDDiscriminator.read(member));
        }
    }
}
