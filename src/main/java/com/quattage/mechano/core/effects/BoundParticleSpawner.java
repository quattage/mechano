package com.quattage.mechano.core.effects;

import com.quattage.mechano.core.util.BlockMath;
import com.quattage.mechano.core.util.Stacky;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;


public class BoundParticleSpawner extends ParticleSpawner {
    BlockEntity target = null;

    private BoundParticleSpawner(BoundParticleSpawner original) {
        super(original);
        this.target = original.target;
    }

    /***
     * New ParticleSpawner bound to a BlockEntity. You'll have access to the Level through the BlockEntitity
     * <strong>as long as you instantiate a new ParticleSpawner after the BlockEntity is created</strong> If you
     * attempt to access the Level from the BlockEntity's constructor, you'll find that the Level instance that is passed
     * will be null, since it has not been set in the BlockEntity yet.
     * @param target The BlockEntity target to bind to
     */
    public BoundParticleSpawner(BlockEntity target) {
        super(target.getLevel(), target.getBlockPos(), Stacky.newStack(target));
        this.target = target;
    }

    /***
     * New ParticleSpawner bound to a BlockEntity with a manually stored Level.
     * For most cases, the {@link #BoundParticleSpawner(BlockEntity) standard constructor} is reccomended,
     * since you'll most often have access to the Level through the BlockEntity anyway.
     * @param target The BlockEntity target to bind to
     * @param world The Level to operate within
     */
    public BoundParticleSpawner(BlockEntity target, Level world) {        
        super(world, target.getBlockPos(), Stacky.newStack(target));
        this.target = target;
    }

    public void setPos(BlockEntity be) {
        this.pos = BlockMath.getVecFromPos(be.getBlockPos());
    }

    @Override
    public BoundParticleSpawner withDensity(int particleDensity) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.particleDensity = particleDensity;
        return out;
    }

    @Override
    public BoundParticleSpawner withRandom(float randomStrength) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.randomStrength = randomStrength;
        return out;
    }


    @Override
    public BoundParticleSpawner withCustom(ItemStack stack) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.stack = stack;
        return out;
    }

    @Override
    public BoundParticleSpawner withCustom(Block block) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.stack = new ItemStack(block.asItem());
        return out;
    }

    @Override
    public BoundParticleSpawner toNearestCenter() {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.pos = BlockMath.getCenter(out.pos);
        return out;
    }

    @Override
    public BoundParticleSpawner toOffset(double offsetX, double offsetY, double offsetZ) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.pos = new Vec3(pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
        return out;
    }

    @Override
    public BoundParticleSpawner toDirectionalOffset(Direction dir) {
        BoundParticleSpawner out = new BoundParticleSpawner(this);
        out.pos = out.pos.relative(dir, 1);
        return out;
    }
    

    @Override
    public String toString() {
        return "BoundParticleSpawner{target=" + target + " stack=" + stack + ", x=" + pos.x + ", y=" + pos.y + ", z=" + pos.z + "}";
    }
}