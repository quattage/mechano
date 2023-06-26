package com.quattage.mechano.content.block.power.transfer.voltometer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrh0.createaddition.rendering.WireNodeRenderer;

import com.quattage.mechano.registry.MechanoPartials;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class VolotmeterRenderer extends WireNodeRenderer<VoltometerBlockEntity> {

    private static final double NEEDLE_OFFSET = 0.15625;

    private static float lerpDestination = 0;
    private static double slowPTick = 0;

    public VolotmeterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    

    @Override
    public void render(VoltometerBlockEntity blockEntity, float partialTicks, PoseStack local, 
            MultiBufferSource bufferSource, int light, int overlay) {
        
        if(slowPTick < 0.999)
            slowPTick += 0.000000001;
        else slowPTick = 0;
        
        super.render(blockEntity, partialTicks, local, bufferSource, light, overlay);
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        
        VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.solid());
        SuperByteBuffer needleModel = CachedBufferer.partial(MechanoPartials.VOLTOMETER_NEEDLE, state);
            
        rotateToFacing(needleModel, facing);
        rotateToTarget(blockEntity, needleModel, facing, partialTicks);

        CachedBufferer.partial(MechanoPartials.VOLTOMETER_NEEDLE, state).renderInto(local, vertexBuffer);
        
    }

    private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
		buffer.centre().rotateY(AngleHelper.horizontalAngle(facing.getOpposite())).unCentre();
	}

    private static void rotateToTarget(VoltometerBlockEntity blockEntity, SuperByteBuffer buffer, Direction facing, float partialTicks) {        
        CompoundTag nbt = blockEntity.getUpdateTag();
        float dialState = nbt.getFloat("DialState");
        float prevDialState = nbt.getFloat("OldState");

        lerpDestination = Mth.lerp((float)slowPTick, prevDialState, dialState);
        buffer.translateY(NEEDLE_OFFSET * -1).centre().rotateZ(lerpDestination).translateY(NEEDLE_OFFSET).unCentre();
    }
}
