package com.quattage.mechano.content.block.RollingWheel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.recipe.RollingRecipe.RollingRecipe;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.processing.ProcessingInventory;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;

@SuppressWarnings("deprecation")
public class RollingWheelControllerBlockEntity extends KineticTileEntity implements SidedStorageBlockEntity {

    public ProcessingInventory inventory;
    private Entity processingEntity;
    private UUID entityUUID;

    private int processTick = 0;
    public float rollingSpeed;

    public RollingWheelControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        inventory = new ProcessingInventory(this::doItemInserted) {
			@Override
			public boolean isItemValid(int slot, ItemVariant stack, long amount) {
				return super.isItemValid(slot, stack, amount) && processingEntity == null;
			}
        };
    }

    private ProcessingInventory getInventory() {
        return inventory;
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput));
    }

    private boolean supportsDirectBeltInput(Direction beltSide) {
		return true;
	}

    @Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        Mechano.log("ROLLING WHEEL DIRECTION: " + getDirectionFromRotation(getCachedState()));
        world.setBlockState(pos, getCachedState()
                .with(RollingWheelControllerBlock.FACING, getDirectionFromRotation(getCachedState())));
    }

    // resets the stored entity and uuid
    public void clear() {
        processingEntity = null;
        entityUUID = null;
    }

    // if the object has a processingEntity
    public boolean hasEntity() {
        return processingEntity != null;
    }

    // if the object has an entity or any inventory space taken up
    public boolean isOccupied() {
		return hasEntity() || !inventory.isEmpty();
	}

    // retrieves recipe from recipe object and returns an optional
    public Optional<RollingRecipe> findRecipe() {
        Optional<RollingRecipe> recipe = world.getRecipeManager().getFirstMatch(RollingRecipe.RECIPE_TYPE, inventory, world);
        return recipe;
    }

    // resets the inventory, adds the ItemEntity to the inventory, and despawns the ItemEntity
    private void doItemIntake(ItemEntity itemEntity) {
        inventory.clear();
        inventory.setStackInSlot(0, itemEntity.getStack().copy());
        doItemInserted(inventory.getStackInSlot(0));
        itemEntity.discard();
        startProcessing(itemEntity);
        world.updateListeners(pos, getCachedState(), getCachedState(),  2 | 16);
    }

    // finds a recipe that matches the given itemstack and tells ProcessingInventory
    // also called directly when an item is inserted with a belt
    private void doItemInserted(ItemStack stack) {
        ItemEntity newForStorage = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
		Optional<RollingRecipe> recipe = findRecipe();
		inventory.remainingTime = recipe.isPresent() ? recipe.get().getProcessTime() : 10;
		inventory.appliedRecipe = false;
	}

    // stores the entity and uuid in this object
    public void startProcessing(Entity entity) {
		processingEntity = entity;
		entityUUID = entity.getUuid();
	}
    
    // ensures that both the top and bottom wheel are rotating in the same direction and at the same speed
    // returns a speed of 0 if that is not the case
        public float getOverallSpeed() {
        BlockPos upOffset = pos.offset(Direction.UP);
        Optional<RollingWheelBlockEntity> aboveWheelBlockEntity = Optional.ofNullable((RollingWheelBlockEntity)world.getBlockEntity(upOffset));

        if(aboveWheelBlockEntity.isEmpty())
            return 0;
        
        RollingWheelBlock aboveBlock = (RollingWheelBlock)aboveWheelBlockEntity.get().getCachedState().getBlock();
        RollingWheelControllerBlock thisBlock = (RollingWheelControllerBlock)this.getCachedState().getBlock();

        if(aboveBlock.getRotationAxis(aboveWheelBlockEntity.get().getCachedState()) == thisBlock.getRotationAxis(this.getCachedState())) {
            if(aboveWheelBlockEntity.get().getSpeed() == this.getSpeed() * -1)
                return Math.abs(this.getSpeed());
        }
        return 0;
    }

    // gets a cardinal direction based on the rotation and axis of the wheel
    // since these wheels can't face down, Direction.DOWN is considered the "failure" case
    public Direction getDirectionFromRotation(BlockState state) {
        if(getSpeed() == 0)
            return Direction.DOWN;

        Axis rollerAxis = ((RollingWheelControllerBlock)getCachedState().getBlock()).getRotationAxis(state);
        if(rollerAxis == Axis.Y)
            return Direction.DOWN;

        speed = getSpeed();
        if(rollerAxis == Axis.X) {
            // north (-) or south (+)
            if(speed < 0)
                return Direction.NORTH;
            return Direction.SOUTH;
        } else {
            // east (-) or west (+)
            if(speed < 0)
                return Direction.EAST;
            return Direction.WEST;
        }
    }

    ////////////////////////// WHERE THE MAGIC HAPPENS /////////////////////////////
    public void tick() {
        super.tick();
        float processSpeed = getOverallSpeed();
        BlockState wheelState = getCachedState();
    
        if(isOccupied() && processSpeed > 0) {
            Optional<RollingRecipe> recipe = findRecipe();
            ItemStack processingStack = inventory.getStackInSlot(0);
            Mechano.log("INVENTORY INSTANCE DURING TICK:" + inventory);
            if(recipe.isPresent()) {
                
            } else {        
                if(processTick < inventory.remainingTime) {
                    this.notifyUpdate();
                    spawnParticles(processingStack);
                    processTick += 1;
                } else {
                    processTick = 0;
                    inventory.clear();
                    Direction dir = wheelState.get(RollingWheelControllerBlock.FACING);
                    Vec3d offset = centerFromDirection(pos.offset(dir), dir);
                    double power = 0.0037d;
                    power = (dir == Direction.NORTH || dir == Direction.EAST) ? power * -1 : power;
                    ItemEntity itemEntity = new ItemEntity(world, offset.getX(), offset.getY(), offset.getZ(), processingStack);
                    itemEntity.setVelocity((dir.getOffsetX() * speed) * power, 0.02, (dir.getOffsetZ() * speed) * power);
                    world.spawnEntity(itemEntity);
                }
            }
        }

        if(world.isClient) {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.tickAudio());
        }
    }
    ////////////////////////// WHERE THE MAGIC HAPPENS /////////////////////////////

    @Environment(EnvType.CLIENT)
    @Override
    public void tickAudio() {
        Mechano.log("INVENTORY INSTANCE DURING AUDIO:" + inventory);
        if (inventory.getStackInSlot(0).isEmpty())
            return;
        float pitch = MathHelper.clamp((speed / 320f) + 0.07f, 0.10f, 0.9f);
        Mechano.log("SOUND PLAYED");
        SoundScapes.play(AmbienceGroup.CRUSHING, pos, pitch);
    }

    public void spawnParticles(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ParticleEffect particle = null;
        if (stack.getItem() instanceof BlockItem)
            particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock().getDefaultState());
        else
            particle = new ItemStackParticleEffect(ParticleTypes.ITEM, stack);
        Random r = world.random;
        for (int i = 0; i < 4; i++) {
            Vec3d rv = new Vec3d(pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat());
            if(world instanceof ServerWorld)
                ((ServerWorld)world).spawnParticles(particle, rv.getX(), rv.getY(), rv.getZ(), 4, 0, 0, 0, 0.08);
        }
    }
    public Vec3d centerFromDirection(BlockPos pos, Direction dir) {
        Vec3d offset = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        float yOffset = 0.8f;
        switch(dir) {
            case UP:
                break;
            case DOWN:
                break;
            case NORTH:
                offset = new Vec3d(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.8);
                break;
            case SOUTH:
                offset = new Vec3d(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.2);
                break;
            case WEST:
                offset = new Vec3d(pos.getX() + 0.8, pos.getY() + yOffset, pos.getZ() + 0.5);
                break;
            case EAST:
                offset = new Vec3d(pos.getX() + 0.2, pos.getY() + yOffset, pos.getZ() + 0.5);
                break;
        }
        return offset;
    }

    @Override
	public void write(NbtCompound compound, boolean clientPacket) {
		if (hasEntity())
			compound.put("Entity", NbtHelper.fromUuid(entityUUID));
		compound.put("Inventory", NBTSerializer.serializeNBT(inventory));
		compound.putFloat("Speed", rollingSpeed);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (compound.contains("Entity") && !isOccupied()) {
			entityUUID = NbtHelper.toUuid(NBTHelper.getINBT(compound, "Entity"));
		}
		rollingSpeed = compound.getFloat("Speed");
		inventory.deserializeNBT(compound.getCompound("Inventory"));
	}

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction arg0) {
        return inventory;
    }

    
}
