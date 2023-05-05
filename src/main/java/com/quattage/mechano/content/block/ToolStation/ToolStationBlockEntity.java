package com.quattage.mechano.content.block.ToolStation;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.util.ImplementedInventory;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToolStationBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory{

    private final DefaultedList<ItemStack> INVENTORY = DefaultedList.ofSize(5, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate;
    private int operations = 0;
    private int maxOperations = 25;

    public ToolStationBlockEntity(BlockPos pos, BlockState state) {
        super(MechanoBlockEntities.TOOL_STATION, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int index) {
                switch(index) {
                    case 0: return ToolStationBlockEntity.this.operations;
                    case 1: return ToolStationBlockEntity.this.maxOperations;
                    default: return 0;
                }
            }

            public void set(int index, int value) {
                switch(index) {
                    case 0: ToolStationBlockEntity.this.operations = value; break;
                    case 1: ToolStationBlockEntity.this.maxOperations = value; break;
                }
            }

            public int size() {return 2;}
        };
    }

    @Override
    public Text getDisplayName() {
        return Mechano.newKey("toolStationBlockEntity");
    }

    @Override
    public ScreenHandler createMenu(int var1, PlayerInventory var2, PlayerEntity var3) {
        throw new UnsupportedOperationException("Unimplemented method 'createMenu'");
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.INVENTORY;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, INVENTORY);
        nbt.putInt("tool_station_operations", operations);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, INVENTORY);
        super.readNbt(nbt);
        operations = nbt.getInt("tool_station_operations");
        
    }

    public static void tick(World world, BlockPos pos, BlockState state, ToolStationBlockEntity entity) {
        if(world.isClient()) {
            return;
        }

        /*
        if(hasRecipe(entity)) {
            
        } 
        */
        
    }
}
