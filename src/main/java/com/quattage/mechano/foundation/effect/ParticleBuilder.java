package com.quattage.mechano.foundation.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleBuilder {

    private Vec3 pos = new Vec3(0, 0, 0);
    private ParticleOptions particle = ParticleTypes.POOF;

    private int density = 5;
    private int cooldown = 0;

    private float size = 1;
    private float randomness = 0;
    private Vec3 initialVelocity = null;

    public ParticleBuilder(ParticleOptions particle) {
        this.particle = particle;
    }

    public static ParticleBuilder ofType(ItemStack stack) {
        ParticleOptions particle = new ItemParticleOption(ParticleTypes.ITEM, stack);
        return new ParticleBuilder(particle);
    }

    public static ParticleBuilder ofType(Block block) {
        return ofType(block.defaultBlockState());
    }

    public static ParticleBuilder ofType(ParticleOptions particle) {
        return new ParticleBuilder(particle);
    }

    public static ParticleBuilder ofType(BlockEntity be) {
        
        Level world = be.getLevel();
        if(world == null) 
            throw new NullPointerException("Failed to build ParticleSpawner from a BlockEntity - World canoot be accessed in this context. (world is null during world load)");

        BlockState state = be.getLevel().getBlockState(be.getBlockPos());
        if(state == null) 
            throw new NullPointerException("Failed to build ParticleSpawner from a BlockEntity - Unknown error occured while fetching BlockState (getBlockState() returned null)");

        return ofType(be.getLevel().getBlockState(be.getBlockPos()));
    }

    public static ParticleBuilder ofType(BlockState state) {
        ParticleOptions particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        return new ParticleBuilder(particle);
    }

    public ParticleBuilder at(Vec3 pos) {
        this.pos = pos;
        return this;
    }

    public ParticleBuilder at(BlockPos pos) {
        this.pos = pos.getCenter();
        return this;
    }

    public ParticleBuilder density(int density) {
        this.density = Math.abs(density);
        return this;
    }

    public ParticleBuilder size(float size) {
        this.size = Math.abs(size);
        return this;
    }

    public ParticleBuilder randomness(float randomness) {
        this.randomness = Math.abs(randomness);
        return this;
    }

    public ParticleBuilder initialVelocity(Direction dir, float speed) {
        Vec3i bias = dir.getNormal();
        this.initialVelocity = new Vec3(bias.getX() * speed, bias.getY() * speed, bias.getZ() * speed);
        return this;
    }

    public ParticleBuilder cooldown(int cooldown) {
        this.cooldown = Math.abs(cooldown);
        return this;
    }

    public ParticleSpawner build() {
        if(initialVelocity != null) return new ParticleSpawner(particle, pos, density, cooldown, size, randomness).setVelocity(initialVelocity);
        return new ParticleSpawner(particle, pos, density, cooldown, size, randomness);
    }
}