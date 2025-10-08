
package com.quattage.mechano.foundation.api.entity;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.DynamicAnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.ContraptionUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public final class GriddableContraptionAttachment extends GriddableEntityAttachment {

    /**
     * This map describes a composite made of multiple AnchorPoint sources
     * acquired during the contraption assembly process. These sources can be stored
     * as BlockPos structure offsets, where the GridUUID and surrogate can be inferred
     * later.
     */
    private Object2IntOpenHashMap<BlockPos> composite = new Object2IntOpenHashMap<>();

    public GriddableContraptionAttachment(IAttachmentHolder holder) {
        super(holder);
        if(!(holder instanceof AbstractContraptionEntity))
            throw new IllegalArgumentException("GriddableContraptionAttachments can only be attached to ContraptionEntities, got " + holder + "!");
        anchors = null;
    }

    @Override // anchor construction is automatic for contraption attachments
    public void constructAnchors(Builder anchors) {}

    @Override
    @OnlyIn(Dist.CLIENT)
    public AnchorArray getAnchors() {
        if(this.anchors != null) return this.anchors;
        DynamicAnchorArray newAnchors = new DynamicAnchorArray();
        Contraption c = getSource() instanceof AbstractContraptionEntity ace ? ace.getContraption() : null;
        if(c == null || c.presentBlockEntities == null || c.presentBlockEntities.isEmpty()) return newAnchors;
        // dummy blockentities freshly instantiated by the contraption will need their AnchorPoints 
        // and surrogate overwritten with the correct, contraption-compatible one
        for(BlockEntity be : c.presentBlockEntities.values()) {
            if(!(be instanceof GriddableBlockEntity gbe)) continue;
            gbe.applyContraptionOverride(this, be.getBlockPos());
            newAnchors.combineWith(gbe.getAnchors());
        }
        this.anchors = newAnchors;
        return newAnchors;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AnchorPoint getAnchor() {
        AnchorArray anchors = getAnchors();
        return anchors.size() <= 0 ? null : anchors.getByIndex(0);
    }

    @Override
    public AnchorPoint getAnchor(int index) {
        AnchorArray anchors = getAnchors();
        return anchors.size() <= 0 ? null : anchors.getByIndex(index);
    }

    public Contraption getContraption() {
        return entity == null ? null : ((AbstractContraptionEntity)entity).getContraption();
    }

    /**
     * Adds the given SurrogateNode to this contraption attachment, instructing this object
     * that the given surrogate node is part of this attachment's associated
     * contraption. This makes it possible for the SurrogateNode to be inferred later
     * when the contraption is disassembled.
     * @param node
     * @param structurePos
     */
    public void markParticipatingSubsurrogate(SurrogateNode node, BlockPos structurePos) {
        this.anchors = null;
        int index = node.getOwnerMatrix() == null ? -1 : node.getOwnerMatrix().getIndex();
        if(index < 0) return;
        composite.put(structurePos, index);
    }

    /**
     * This method is used to iterate over each destined AnchorPoint-containing BlockEntity
     * when a contraption transitions from an assembled to a dissassembled state. The
     * @param action The action to perform on the {@link GridNode} after its data has been corrected
     * @param transform The transform reflecting the current position of the contraption. This 
     * is used to get the real-world position of the block after the contraption has moved.
     */
    public void forEachAssociated(StructureTransform transform, Consumer<GridNode> action) {
        if(entity == null || entity.level().isClientSide) return;
        ServerGrid grid = SidedGridDispatcher.server(entity.level());
        boolean found = false;
        for(Object2IntMap.Entry<BlockPos> subsurrogate : composite.object2IntEntrySet()) {
            ServerMatrix matrix = grid.getMatrixByIndex(subsurrogate.getIntValue());
            if(matrix == null || matrix.nodes == null) continue;
            GridUUID walkingAddress = new ContraptionUUID(entity.getUUID(), subsurrogate.getKey(), 0);
            for(int x = 0; x < GridUUID.MAX_SHARED_OCCUPANCY; x++) {
                walkingAddress = walkingAddress.indexedCopy(x);
                GridNode node = matrix.nodes.get(walkingAddress);
                if(node == null) continue;
                action.accept(node);
                found = true;
            }
        }
        if(!found) Mechano.LOGGER.warn("Iteration attempt on GridNode assocations for " + entity + " produced no results.");
    }

    public void forEachAssociatedClient(StructureTransform transform, Consumer<AnchorPoint> action) {
        
    }

    public Object2IntOpenHashMap<BlockPos> getCompositeUUIDs() {
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
        for(Object2IntMap.Entry<BlockPos> subsurrogate : composite.object2IntEntrySet()) {
            CompoundTag addr = new CompoundTag();
            addr.putInt("ctr", subsurrogate.getIntValue());
            addr.putInt("x", subsurrogate.getKey().getX());
            addr.putInt("y", subsurrogate.getKey().getY());
            addr.putInt("z", subsurrogate.getKey().getZ());
            list.add(addr);
        }
        in.put("GridComposite", list);
        return in;
    }

    public void readCompositeFrom(CompoundTag in) {
        if(in == null) return;
        ListTag list = in.getList("GridComposite", Tag.TAG_COMPOUND);
        if(list == null) return;
        composite.ensureCapacity(list.size());
        for(int x = 0; x < list.size(); x++) {
            CompoundTag addr = list.getCompound(x);
            composite.put(new BlockPos(addr.getInt("x"), addr.getInt("y"), addr.getInt("z")), addr.getInt("ctr"));
        }
    }

    public long[] packComposite() {
        if(composite.isEmpty()) return new long[0];
        long[] compressed = new long[composite.size() * 2];
        int index = 0;
        for(Object2IntMap.Entry<BlockPos> subsurrogate : composite.object2IntEntrySet()) {
            compressed[index] = subsurrogate.getIntValue();
            index++;
            compressed[index] = subsurrogate.getKey().asLong();
            index++;
        }
        return compressed;
    }

    public void unpackComposite(long[] compressed) {
        if(compressed.length <= 0) return;
        composite.clear();
        composite.ensureCapacity(compressed.length / 2);
        for(int x = 0; x < compressed.length; x += 2)
            composite.put(BlockPos.of(compressed[x + 1]), (int)compressed[x]);
    }

    // public static StreamCodec<RegistryFriendlyByteBuf, 
}
