

package com.quattage.mechano.foundation.api.anchor;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.SidedGridDispatcher.LinkData;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.classifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

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
public class GriddableEntityAttachment implements AnchorPointable<Entity> {

    private Entity entity;
    private AnchorArray anchor;
    private final DispatchedAnchorNode surrogate = new DispatchedAnchorNode(this);

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @Nullable AnchorPointable<T> of(T e, boolean force) {
        if(e == null) {
            Mechano.LOGGER.warn("Tried (and failed) to get AnchorPointable for null entity!");
            return null;
        }
        if(e instanceof AnchorPointable<?> ap) return (AnchorPointable<T>)ap;
        if(!force && !e.hasData(MechanoData.ANCHOR_ATTACHMENT)) return null;
        return (AnchorPointable<T>)e.getData(MechanoData.ANCHOR_ATTACHMENT);
    }

    /**
     * This constructor is only used by the {@link MechanoData data attachment registry}.
     * Use {@link GriddableEntityAttachment#of} instead.
     * @param holder
     */
    @ApiStatus.Internal
    public GriddableEntityAttachment(IAttachmentHolder holder) {
        if(!(holder instanceof Entity entity))
            throw new IllegalArgumentException("EntityAnchorPointHosts can only be attached to entities, got " + holder + "!");
        this.entity = entity;
        constructAnchors(null);
        surrogate.nodeCount = anchor.size();
    }

    @Override
    public void constructAnchors(Builder anchors) {
        anchor = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(entity.getUUID(), 0), 0, 0, 0, 1.7f, true, 2)));
    }

    @SuppressWarnings("unchecked")
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!entity.level().isClientSide) return null;
        if(!entity.hasData(MechanoData.LINK_ATTACHMENT)) return null;
        LinkData data = entity.getData(MechanoData.LINK_ATTACHMENT);
        if(data.isEmpty()) {
            entity.removeData(MechanoData.LINK_ATTACHMENT);
            return null;
        }
        return (ObjectSet<GridCatenary>)(Object)data.get();
    }

    @Override
    public AnchorArray getAnchors() {
        return anchor;
    }

    @Override
    public AnchorPoint getAnchor(int index) {
        return anchor.getByIndex(0);
    }

    @Override
    public Level getWorld() {
        return entity.level();
    }

    @Override
    public DispatchedAnchorNode getSurrogate() {
        return surrogate;
    }

    @Override
    public GridUUID createAddress() {
        return anchor.getByIndex(0).getAddress();
    }

    @Override
    public String describeState() {
        return "Entity '" + entity.getName().getString() + "'";
    }

    @Override
    public Entity getSource() {
        return entity;
    }
}