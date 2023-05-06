package com.quattage.mechano.content.block.RollingWheel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.processing.ProcessingInventory;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class RollingWheelControllerBlockEntity extends KineticTileEntity {

    private UUID storedEntityUUID;
    private boolean holdingEntity = false;
    public ProcessingInventory inventory;
    public float rollingSpeed;

    public RollingWheelControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
		inventory = new ProcessingInventory(this::onItemInsertion) {

			@Override
			public boolean isItemValid(int slot, ItemVariant stack) {
				return super.isItemValid(slot, stack) && processingEntity == null;
			}

		};
    }
    
    private void syncControllers() {
        for (Direction dir : Iterate.directions) {
            ((RollingWheelControllerBlock) getCachedState().getBlock()).updateControllers(getCachedState(), getWorld(), getPos(), dir);
        }
    }

    @Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		syncControllers();
	}

    @Override
	public void lazyTick() {
		super.lazyTick();
		syncControllers();
	}

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}

    @Override
    public void tick() {
        super.tick();

    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(holdingEntity)
            nbt.put("Entity", NbtHelper.fromUuid(storedEntityUUID));
        nbt.put("Inventory", NBTSerializer.serializeNBT(inventory));

    }

    @Override
    protected void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
    }

    
    public Optional<ProcessingRecipe<Inventory>> parseRollingRecipe() {
		Optional<ProcessingRecipe<Inventory>> rollingRecipe = MechanoRecipeTypes.ROLLING.find(inventory, world);
		return rollingRecipe;
	}

    private void onItemInsertion(ItemStack stack) {
		Optional<ProcessingRecipe<Inventory>> recipe = parseRollingRecipe();
		inventory.remainingTime = recipe.isPresent() ? recipe.get().getProcessingDuration() : 125;
		inventory.appliedRecipe = false;
	}
}
