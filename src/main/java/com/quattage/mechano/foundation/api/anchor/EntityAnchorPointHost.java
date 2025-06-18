package com.quattage.mechano.foundation.api.anchor;

import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;
import com.quattage.mechano.foundation.api.landmark.classifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityAnchorPointHost implements AnchorPointable {

    private Entity entity;
    private AnchorArray anchor;
    private final DispatchedAnchorNode surrogate = new DispatchedAnchorNode(this);

    public EntityAnchorPointHost(Entity entity) {
        this.entity = entity;
        surrogate.nodeCount = 1;
    }

    @Override
    public void constructAnchors(Builder anchors) {
        anchor = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(entity.getUUID(), 0), 0, 0, 0, 1.7f, true, 0)));
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
        return entity.getName().toString();
    }
}