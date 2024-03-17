package com.quattage.mechano.content.block.simple.diagonalGirder;

import java.util.ArrayList;
import java.util.List;

import com.jozufozu.flywheel.core.PartialModel;
import com.quattage.mechano.MechanoPartials;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlock.DiagonalGirderModelType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DiagonalGirderBlockEntity extends SmartBlockEntity {
    protected boolean showDownFlat = false;
    protected boolean showDownVert = false;
    protected boolean showMiddle = false;
    protected boolean showUpFlat = false;
    protected boolean showUpVert = false;

    private BlockState state;
    private Block block;

    public DiagonalGirderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state;
        this.block = state.getBlock();
        setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    
    @Override
    protected void read(CompoundTag nbt, boolean arg1) {
        showDownFlat = nbt.getBoolean("downFlat");
        showDownVert = nbt.getBoolean("downVert");
        showMiddle = nbt.getBoolean("middle");
        showUpFlat = nbt.getBoolean("upFlat");
        showUpVert = nbt.getBoolean("upVert");
        super.read(nbt, arg1);
    }

    public List<PartialModel> getRelevantPartials() {
        List<PartialModel> out = new ArrayList<PartialModel>();

        DiagonalGirderModelType modelType = getBlockState().getValue(DiagonalGirderBlock.MODEL_TYPE);
        switch(modelType) {
            case LONG_DOUBLE:
                if(showDownFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_DOWN_FLAT);
                if(showDownVert) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_DOWN_VERT);
                if(showUpFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_UP_FLAT);
                if(showUpVert) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_UP_VERT);
                break;
            case LONG_END_DOWN:
                if(showDownFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_FLAT);
                if(showDownVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_VERT);
                if(showUpFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_UP_FLAT);
                if(showUpVert) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_UP_VERT);
                break;
            case LONG_END_UP:
                if(showDownFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_DOWN_FLAT);
                if(showDownVert) out.add(MechanoPartials.DIAGONAL_GIRDER_LONG_DOWN_VERT);
                if(showUpFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_FLAT);
                if(showUpVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_VERT);
                break;
            case MIDDLE:
                break;
            case SHORT_DOUBLE:
                if(showDownFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_FLAT);
                if(showDownVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_VERT);
                if(showUpFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_FLAT);
                if(showUpVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_VERT);
                break;
            case SHORT_END_DOWN:
                if(showDownFlat) showDownFlat = false;
                if(showDownVert) showDownVert = false;
                if(showUpFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_FLAT);
                if(showUpVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_UP_VERT);
                break;
            case SHORT_END_UP:
                if(showDownFlat) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_FLAT);
                if(showDownVert) out.add(MechanoPartials.DIAGONAL_GIRDER_SHORT_DOWN_VERT);
                if(showUpFlat) showUpFlat = false;
                if(showUpVert) showUpVert = false;
                break;
        }
        return out;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if(!(block instanceof DiagonalGirderBlock)) return;
        if(level.isClientSide()) return;

        ((DiagonalGirderBlock)block).doShapeUpdate(level, getBlockPos(), state);
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        nbt.putBoolean("downFlat", showDownFlat);
        nbt.putBoolean("downVert", showDownVert);
        nbt.putBoolean("middle", showMiddle);
        nbt.putBoolean("upFlat", showUpFlat);
        nbt.putBoolean("upVert", showUpVert);
        super.write(nbt, clientPacket);
    }
}
