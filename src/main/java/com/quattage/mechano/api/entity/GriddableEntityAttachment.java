

package com.quattage.mechano.api.entity;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.LinkDataStorage;
import com.quattage.mechano.api.anchor.AnchorCollection;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.catenary.CatenaryAccess;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.griddable.SurrogateNode;
import com.quattage.mechano.api.identifier.EntityUUID;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.landmark.GridCatenary;
import com.quattage.mechano.api.switchboard.GriddableUpdatePacket;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * A mirror implementation of {@link GriddableBlockEntity} built
 * for externally registered Entities, rather than around a bespoke
 * BlockEntity or Entity implementation. This object can be attached 
 * to any entity in Minecraft, vanilla or otherwise, using Neoforge's
 * Data Attachments system. This class is not currently designed to be 
 * extended and is only for being attached to Player entities while wires
 * are being created. If I ever get back to this in the future, some kind
 * of registry system will exist to allow the creation of custom AnchorPoint
 * and circuit configurations for vanilla entities
 */
public class GriddableEntityAttachment implements Griddable<Entity>, CatenaryAccess {

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @Nullable Griddable<T> of(T hostEntity, boolean force) {
        if(hostEntity == null) {
            Mechano.LOGGER.warn("Tried (and failed) to get Griddable for null entity!");
            return null;
        }
        if(hostEntity instanceof Griddable<?> sub) return (Griddable<T>)sub;
        Griddable<T> data = (Griddable<T>)hostEntity.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
        if(data != null) return data;
        if(hostEntity instanceof AbstractContraptionEntity ace) {
            if(force) {
                GriddableContraptionAttachment gca = new GriddableContraptionAttachment(ace);
                ace.setData(MechanoData.ANCHOR_ATTACHMENT, gca);
                return (Griddable<T>)gca;
            }
            return data;
        }
        return force ? (Griddable<T>)hostEntity.getData(MechanoData.ANCHOR_ATTACHMENT) : data;
    }

    public static final IAttachmentSerializer<CompoundTag, GriddableEntityAttachment> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public GriddableEntityAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            if(!(holder instanceof Entity e)) {
                throw new IllegalArgumentException("GriddableEntityAttachments cannot be de-serialized to any non-entity holder type, got '" 
                    + holder.getClass().getSimpleName() + "!'" );
            }
            GriddableEntityAttachment attachment = (GriddableEntityAttachment)GriddableEntityAttachment.of(e, true);
            attachment.readFrom(tag);
            return attachment;
        }
        @Override
        public @Nullable CompoundTag write(GriddableEntityAttachment attachment, HolderLookup.Provider provider) {
            return attachment.writeTo(new CompoundTag());
        }
    };

    protected Entity entity;
    protected AnchorCollection anchors;
    protected final SurrogateNode surrogate = new SurrogateNode(this);

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
        this.anchors = null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!entity.level().isClientSide) return null;
        if(!entity.hasData(MechanoData.LINK_ATTACHMENT)) return null;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(entity, false);
        return storage == null ? null : storage.getAll();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AnchorCollection getAnchors() {
        /**
         * TODO TEMPORARY!!!!!
         * the only place this is used is for drawing in-progress wires to/from the player while they're making a spool
         * if there's ever an additional use-case for this there needs to be some kind of registry-specific wrapped object
         * for creating circuits and AnchorPoints for vanilla entities
         */
        if(anchors == null) this.anchors = AnchorCollection.asSingle((new AnchorPoint(new EntityUUID(entity.getUUID(), 0), 0, 0, 0, 1.7f, true, 2)));
        return this.anchors;
    }

    /**
     * Syncs attachment-specific data from the server to all clients currently laoding this object.
     * @param world
     */
    public void broadcast(LevelReader world) {
        if(world.isClientSide()) return;
        CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, GriddableUpdatePacket.of(this));
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

    public CompoundTag writeTo(CompoundTag in) {
        return in;
    }

    public void readFrom(CompoundTag in) {
        return;
    }
}