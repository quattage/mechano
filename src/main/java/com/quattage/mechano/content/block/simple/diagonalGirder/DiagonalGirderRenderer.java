package com.quattage.mechano.content.block.simple.diagonalGirder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

public class DiagonalGirderRenderer extends SafeBlockEntityRenderer<DiagonalGirderBlockEntity> {

    public DiagonalGirderRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(DiagonalGirderBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.solid());
        render(blockEntity, state, poseStack, (sbb) -> sbb.light(light).renderInto(poseStack, vertexBuffer));   
    }

    private static void render(DiagonalGirderBlockEntity blockEntity, BlockState state, PoseStack local, Consumer<SuperByteBuffer> drawCall) {
        List<PartialModel> relevantPartials = blockEntity.getRelevantPartials();

        
        for(PartialModel component : relevantPartials) {
            SuperByteBuffer girderModel = CachedBuffers.partial(component, state);

            Direction facing = state.getValue(DirectionalBlock.FACING);
            rotateToFacing(girderModel, facing);
    
            drawCall.accept(girderModel);
        }
    }



    private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
		buffer.center().rotateY(AngleHelper.horizontalAngle(facing)).uncenter();
	}
}