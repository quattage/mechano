

package com.quattage.mechano.foundation.entity;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.catenary.CatenaryAccessor;

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
public class GriddableEntityAttachment implements Griddable<Entity>, CatenaryAccessor {

    private Entity entity;
    private AnchorArray anchor;
    private final SurrogateNode surrogate = new SurrogateNode(this);

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @Nullable Griddable<T> of(T e, boolean force) {
        if(e == null) {
            Mechano.LOGGER.warn("Tried (and failed) to get Griddable for null entity!");
            return null;
        }

        if(e instanceof Griddable<?> ap) return (Griddable<T>)ap;
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
        if(!(holder instanceof Entity entity))
            throw new IllegalArgumentException("EntityAnchorPointHosts can only be attached to entities, got " + holder + "!");
        this.entity = entity;
        constructAnchors(null);
    }

    @Override
    public void constructAnchors(Builder anchors) {
        anchor = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(entity.getUUID(), 0), 0, 0, 0, 1.7f, true, 2)));
    }

    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!entity.level().isClientSide) return null;
        if(!entity.hasData(MechanoData.LINK_ATTACHMENT)) return null;
        LinkDataStorable.Client storage = LinkDataStorable.getAsClient(entity, false);
        return storage == null ? null : storage.getAll();
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
    public SurrogateNode getSurrogate() {
        return surrogate;
    }

    @Override
    public GridUUID getOrCreateAddress() {
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