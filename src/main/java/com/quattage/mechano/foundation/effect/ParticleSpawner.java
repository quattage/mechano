package com.quattage.mechano.foundation.effect;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class ParticleSpawner {

    private final ParticleOptions particle;
    private int tick = 0;

    private Vec3 pos;
    private Vec3 velocity = new Vec3(0, 0, 0);

    private int density;
    private int cooldown;

    private float size;
    private float randomness;
    
    public ParticleSpawner(ParticleOptions particle, Vec3 pos, int density, int cooldown, float size, float randomness) {
        this.particle = particle;
        this.pos = pos;
        this.density = density;
        this.cooldown = cooldown;
        this.tick = cooldown;
        this.size = size;
        this.randomness = randomness;
    }

    /***
     * Sets the position of this ParticleSpawner to the given Vec3
     * @param pos Vec3 to set
     * @return This ParticleSpawner, modified for chaining
     */
    public ParticleSpawner setPos(Vec3 pos) {
        this.pos = pos;
        return this;
    }

    /***
     * Sets the position of this ParticleSpawner to the given BlockPos's center
     * @param pos BlockPos to set
     * @return This ParticleSpawner, modified for chaining
     */
    public ParticleSpawner setPos(BlockPos pos) {
        this.pos = pos.getCenter();
        return this;
    }

    /***
     * Sets the velocity of this ParticleSpawner
     * @param vec Vector (X, Y, Z) velocity
     * @return This ParticleSpawner, modified for chaining
     */
    public ParticleSpawner setVelocity(Vec3 vec) {
        this.velocity = vec;
        return this;
    }

    /***
     * Replaces the Velocity value of this ParticleSpawner with a random value ranging from -dispersion to +dispersion
     * @param dispersion Strength of randomness
     * @return This ParticleSpawner, modified for chaining
     */
    public ParticleSpawner randomizeVelocity(float dispersion) {
        this.velocity = VectorHelper.addRandomness(new Vec3(0, 0, 0), dispersion);
        return this;
    }

    public ParticleSpawner addVelocity(Axis ax, float amount) {
        switch(ax) {
            case X:
                velocity.add(amount, 0, 0);
                return this;
            case Y:
                velocity.add(0, amount, 0);
                return this;
            case Z:
                velocity.add(0, 0, amount);
                return this;
            default:
                return this;
        }
    }

    public void spawnAsClient(ClientLevel world) {

        if(!shouldSpawn()) return;

        if(size <= 1) {
            for(int x = 0; x < density; x++) {

                Vec3 randPos = this.pos;
                Vec3 randVelo = this.velocity;
                if(randomness > 0) {
                    randPos = VectorHelper.addRandomness(randPos, randomness / 3f);
                    randVelo = VectorHelper.addRandomness(randVelo, randomness);
                }

                world.addParticle(particle, randPos.x, randPos.y, randPos.z, randVelo.x, randVelo.y, randVelo.z);
            }
            return;
        }
        
        float step = size / density;

        for(int x = 0; x < size; x += step) {
            for(int y = 0; x < size; x += step) {
                for(int z = 0; x < size; x += step) {

                    Vec3 aPos = new Vec3(
                        pos.x + (x - size / 2),
                        pos.y + (y - size / 2),
                        pos.z + (z - size / 2)
                    );

                    Vec3 randPos = aPos;
                    Vec3 randVelo = this.velocity;
                    if(randomness > 0) {
                        randPos = VectorHelper.addRandomness(randPos, randomness / 3f);
                        randVelo = VectorHelper.addRandomness(randVelo, randomness);
                    }

                    world.addParticle(particle, randPos.x, randPos.y, randPos.z, randVelo.x, randVelo.y, randVelo.z);
                }
            }
        }
    }

    public void spawnAsServer(ServerLevel world, ServerPlayer player) {

        if(!shouldSpawn()) return;

        if(size <= 1) {

            Vec3 randPos = this.pos;
            Vec3 randVelo = this.velocity;
            if(randomness > 0) {
                randPos = VectorHelper.addRandomness(randPos, randomness / 3f);
                randVelo = VectorHelper.addRandomness(randVelo, randomness);
            }
            randVelo = randVelo.normalize();
            double speed = VectorHelper.getMagnitude(randVelo);

            world.sendParticles(player, particle, false, randPos.x, randPos.y, randPos.z, density, randVelo.x, randVelo.y, randVelo.z, speed);
            return;
        }
        
        float step = size / density;

        for(int x = 0; x < size; x += step) {
            for(int y = 0; x < size; x += step) {
                for(int z = 0; x < size; x += step) {

                    Vec3 aPos = new Vec3(
                        pos.x + (x - size / 2),
                        pos.y + (y - size / 2),
                        pos.z + (z - size / 2)
                    );

                    Vec3 randPos = aPos;
                    Vec3 randVelo = this.velocity;
                    if(randomness > 0) {
                        randPos = VectorHelper.addRandomness(randPos, randomness / 3f);
                        randVelo = VectorHelper.addRandomness(randVelo, randomness);
                    }
                    randVelo = randVelo.normalize();
                    double speed = VectorHelper.getMagnitude(randVelo);

                    world.sendParticles(player, particle, size > 3, randPos.x, randPos.y, randPos.z, 1, randVelo.x, randVelo.y, randVelo.z, speed);
                }
            }
        }
    }

    public void spawnAsServer(ServerLevel world) {

        if(!shouldSpawn()) return;

        if(size <= 1) {

            Vec3 randPos = this.pos;
            Vec3 randVelo = this.velocity;
            if(randomness > 0) {
                randPos = VectorHelper.addRandomness(randPos, randomness / 3f);
                randVelo = VectorHelper.addRandomness(randVelo, randomness);
            }
            double speed = VectorHelper.getMagnitude(randVelo);
            randVelo = randVelo.normalize();
            Mechano.log("speed: " + speed);

            world.sendParticles(particle, randPos.x, randPos.y, randPos.z, density, randVelo.x, randVelo.y, randVelo.z, speed);
            return;
        }
        
        float step = size / density;

        for(int x = 0; x < size; x += step) {
            for(int y = 0; x < size; x += step) {
                for(int z = 0; x < size; x += step) {

                    Vec3 aPos = new Vec3(
                        pos.x + (x - size / 2),
                        pos.y + (y - size / 2),
                        pos.z + (z - size / 2)
                    );

                    Vec3 randPos = aPos;
                    Vec3 randVelo = this.velocity;
                    if(randomness > 0) {
                        randPos = VectorHelper.addRandomness(randPos, randomness / 2);
                        randVelo = VectorHelper.addRandomness(randVelo, randomness);
                    }
                    randVelo = randVelo.normalize();
                    double speed = VectorHelper.getMagnitude(randVelo);

                    world.sendParticles(particle, randPos.x, randPos.y, randPos.z, 1, randVelo.x, randVelo.y, randVelo.z, speed);
                }
            }
        }
    }

    private boolean shouldSpawn() {

        if(cooldown <= 0) return true;

        tick++;
        if(tick >= cooldown) {
            tick = 0;
            return true;
        }
        return false;
    }
}
