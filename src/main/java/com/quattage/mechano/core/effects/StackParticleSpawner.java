package com.quattage.mechano.core.effects;

import org.apache.commons.lang3.NotImplementedException;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.util.BlockMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class StackParticleSpawner {
    // assignment
    protected Vec3 pos = null;
    protected ItemStack stack = null;
    protected Level world = null;

    // particle data
    protected  ParticleOptions particle = null;
    protected int particleDensity = 3;
    protected float particleSpeed = 0.3f;
    protected float randomStrength = 0f;

    protected StackParticleSpawner(StackParticleSpawner original) {
        this.world = original.world;
        this.pos = original.pos;
        this.stack = original.stack;
        this.particleDensity = original.particleDensity;
        this.randomStrength = original.randomStrength;
        //Mechano.log("New particle spawner bound at " + pos + " with a density of " + particleDensity);
        instantiateParticle();
    }

    public StackParticleSpawner(Level world, BlockPos pos, ItemStack stack) {
        this.world = world;
        this.pos = BlockMath.getVecFromPos(pos);
        this.stack = stack;
        instantiateParticle();
    }

    public StackParticleSpawner(Level world, Vec3 pos, ItemStack stack) {
        this.world = world;
        this.pos = pos;
        this.stack = stack;
        instantiateParticle();
    }

    public StackParticleSpawner(Level world, ItemStack stack, int x, int y, int z) {
        this.world = world;
        this.pos = new Vec3(x, y, z);
        this.stack = stack;
        instantiateParticle();
    }

    /***
     * Modifies the stored particleDensity value. Higher numbers mean more particles will spawn
     * @param particleDensity Density number. A ParticleSpawner's default value is 3
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner withDensity(int particleDensity) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.particleDensity = particleDensity;
        return out;
    }

    /***
     * Modified the stored particleSpeed value. Higher numbers will make particles fly further from the origin point.
     * Default value is 0.3
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner withSpeed(float particleSpeed) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.particleSpeed = particleSpeed;
        return out;
    }

    /***
     * Modifies the stored randomStrength value. Higher numbers will make the particles
     * spawn with more randomness, further away from the position value. 
     * Pair with {@link #withDensity() withDensity} to increase the overall size and intensity
     * of particles.
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner withRandom(float particleStrength) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.randomStrength = particleStrength;
        return out;
    }

    /***
     * Changes the particle ItemStack to the specified ItemStack.
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner withCustom(ItemStack stack) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.stack = stack;
        return out;
    }

    /***
     * Changes the particle ItemStack to the specified ItemStack.
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner withCustom(Block block) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.stack = new ItemStack(block.asItem());
        return out;
    }

    /***
     * Sets the ParticleSpawner's position to the nearest block's center.
     * @return a new ParticleSpawner with the modified value
     */
    public StackParticleSpawner toNearestCenter() {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.pos = BlockMath.getCenter(out.pos);
        return out;
    }

    /***
     * Sets the stored position of this ParticleSpawner to an absolute position.
     * @return a new ParticleSpawner with the modified value. 
     */
    public StackParticleSpawner toAbsolute(BlockPos pos) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.pos = BlockMath.getVecFromPos(pos);
        return out;
    }

    /***
     * Adds an offset to the ParticleSpawner's position. If you intend to use this with {@link #toNearestCenter() toNearestCenter},
     * do so with caution, as {@link #toNearestCenter() toNearestCenter} will likely override your offset value.
     * This can be avoided by ensuring that you always use {@link #toNearestCenter() toNearestcenter}
     * <strong>before</strong> you use {@link #toOffset() toOffset}
     * @return a new ParticleSpawner with the modified value. 
     */
    public StackParticleSpawner toOffset(double offsetX, double offsetY, double offsetZ) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.pos = new Vec3(pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
        return out;
    }

    /***
     * Does the same thing as {@link #toOffset() toOffset}, but uses a Direction instead of a concrete value.
     * @return a new ParticleSpawner with the modified value.
     */
    public StackParticleSpawner toDirectionalOffset(Direction direction) {
        StackParticleSpawner out = new StackParticleSpawner(this);
        out.pos = out.pos.relative(direction, 1);
        return out;
    }

    public void instantiateParticle() {
        if (stack.getItem() instanceof BlockItem)
            particle = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock().defaultBlockState());
        else
            particle = new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    public void setPos(BlockPos pos) {
        this.pos = BlockMath.getVecFromPos(pos);
    }

    public void setPos(double x, double y, double z) {
        this.pos = new Vec3(x, y, z);
    }

    /***
     * Adds the particle to the world. Syncs between clients.
     */
    public void spawn() {
        if(this.pos == null)     
            throw new NullPointerException(this + " Failed to spawn particles, BlockPos is null. (did you mean to use spawnAt()?)");
        if(this.world == null)
            throw new NullPointerException(this + " Failed to spawn particles, World is null");
        spawnParticles(this.stack, this.pos, this.world);
    }   

    /***
     * Adds mutliple particles to the world starting at the stored position, and ending at the specified destination.
     * This can be used to fill an entire 3d space with particles. Probably best to be used sparingly.
     * Increments in whole blocks only.
     */
    public void spawnWithin(Vec3 destination) {
        
    }

    public void spawnParticles(ItemStack stack, Vec3 pos, Level world) {
        if (stack == null || stack.isEmpty()) 
            return;
        for (int i = 0; i < particleDensity; i++) {
            Vec3 rv = pos;
            if(randomStrength > 0.1)
                BlockMath.addRandomness(pos, randomStrength);
            if(world instanceof ServerLevel)
                ((ServerLevel)world).sendParticles(particle, rv.x, rv.y, rv.z, (int)(particleDensity * 1.7), 0, 0, 0, particleSpeed);
        }
    }

    public String toString() {
        return "ParticleSpawner{stack=" + stack + ", x=" + pos.x + ", y=" + pos.y + ", z=" + pos.z + "}";
    }
}
