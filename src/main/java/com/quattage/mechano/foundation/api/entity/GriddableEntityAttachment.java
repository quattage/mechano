

package com.quattage.mechano.foundation.api.entity;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorage;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccess;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * A mirror implementation of {@link GriddableBlockEntity} built
 * for externally registered Entities, rather than around a bespoke
 * BlockEntity implementation. This object can be attached to any 
 * entity in Minecraft, vanilla or otherwise, using Neoforge's
 * Data Attachments system. Nothing here is actually synced or
 * serialized, since that's all handled by the {@link SidedGridDispatcher}
 * at the world-level, so despite being a Data Attachment, there are
 * no persistence features built into this class directly.
 */
public class GriddableEntityAttachment implements Griddable<Entity>, CatenaryAccess {

    public static final IAttachmentSerializer<CompoundTag, GriddableEntityAttachment> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public GriddableEntityAttachment read(IAttachmentHolder holder, CompoundTag tag,
                net.minecraft.core.HolderLookup.Provider provider) {
            if(holder instanceof AbstractContraptionEntity ace) {
                GriddableContraptionAttachment gca = new GriddableContraptionAttachment(ace);
                gca.readCompositeFrom(tag);
                return gca;
            }
            return new GriddableEntityAttachment(holder);
        }
        @Override
        public @Nullable CompoundTag write(GriddableEntityAttachment attachment,
                net.minecraft.core.HolderLookup.Provider provider) {
            if(attachment instanceof GriddableContraptionAttachment gca && !gca.getCompositeUUIDs().isEmpty())
                return gca.writeCompositeTo(new CompoundTag());
            return null;
        }
    };

    protected Entity entity;
    protected AnchorArray anchors;
    protected final SurrogateNode surrogate = new SurrogateNode(this);

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @Nullable Griddable<T> of(T e, boolean force) {
        if(e instanceof Griddable<?> ap) return (Griddable<T>)ap;
        if(e instanceof AbstractContraptionEntity ace) {
            if(force) {
                GriddableContraptionAttachment gca = new GriddableContraptionAttachment(ace);
                ace.setData(MechanoData.ANCHOR_ATTACHMENT, gca);
                return (Griddable<T>)gca;
            }
            GriddableEntityAttachment data = ace.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
            if(data == null) return null;
            if(data instanceof GriddableContraptionAttachment) return (Griddable<T>) data;
            ace.removeData(MechanoData.ANCHOR_ATTACHMENT);
            return null;
        }
        if(e == null) {
            Mechano.LOGGER.warn("Tried (and failed) to get Griddable for null entity!");
            return null;
        }
        if(force) return (Griddable<T>)e.getData(MechanoData.ANCHOR_ATTACHMENT);
        return (Griddable<T>)e.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
    }

    /**
     * This constructor is only used by the {@link MechanoData data attachment registry}.
     * Use {@link GriddableEntityAttachment#of} instead.
     * @param holder
     */
    @ApiStatus.Internal
    public GriddableEntityAttachment(IAttachmentHolder holder) {
        if(holder == null) return;
        if(!(holder instanceof Entity entity))
            throw new IllegalArgumentException("GriddableEntityAttachments can only be attached to entities, got " + holder + "!");
        this.entity = entity;
        // TODO provisions for optional anchor construction to allow APIs to declare circuits for entities
        constructAnchors(null);
    }

    @Override
    public void constructAnchors(Builder anchors) {
        this.anchors = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(entity.getUUID(), 0), 0, 0, 0, 1.7f, true, 2)));
    }

    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!entity.level().isClientSide) return null;
        if(!entity.hasData(MechanoData.LINK_ATTACHMENT)) return null;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(entity, false);
        return storage == null ? null : storage.getAll();
    }

    @Override
    public AnchorArray getAnchors() {
        return anchors;
    }

    @Override
    public AnchorPoint getAnchor(int index) {
        return anchors.getByIndex(0);
    }

    @Override
    public Level getWorld() {
        return entity.level();
    }

    @Override
    public SurrogateNode getSurrogate() {
        return surrogate;
    }

    @Override
    public GridUUID createSupplementaryAddress() {
        return new EntityUUID(entity.getUUID(), 0);
    }

    @Override
    public String describeState() {
        return "Entity '" + entity.getName().getString() + "' @" + entity.getUUID();
    }

    @Override
    public Entity getSource() {
        return entity;
    }

    @Override
    public Vec3 getSourcePosition() {
        return entity == null ? Vec3.ZERO : entity.position();
    }
}