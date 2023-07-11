package com.quattage.mechano.core.blockEntity.render;

import java.util.function.Consumer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.base.NodeConnection;
import com.quattage.mechano.registry.MechanoPartials;
import com.quattage.mechano.registry.MechanoRenderTypes;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricBlockRenderer<T extends ElectricBlockEntity> extends SafeBlockEntityRenderer<T> {

    public ElectricBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    private Vec3 fromPos;
    private Vec3 toPos;

    @Override
    protected void renderSafe(ElectricBlockEntity blockEntity, float partialTicks, PoseStack local, 
        MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();

        VertexConsumer modelBuilder = bufferSource.getBuffer(MechanoRenderTypes.WIRE_ALL);
        renderWires(blockEntity, local, modelBuilder, light, state);

        //Mechano.log("tick");
    }

    private void renderWires(ElectricBlockEntity ebe, PoseStack local, VertexConsumer modelBuilder, 
        int light, BlockState state) {

        ElectricNode[] nodes = ebe.nodes.values();

        for(int n = 0; n < nodes.length; n++) {
            ElectricNode thisNode = nodes[n];
            NodeConnection[] theseConnections = thisNode.getConnections();

            for(int c = 0; c < theseConnections.length; c++) {
                if(!thisNode.hasConnection(c)) continue;

                //////////
                if(Minecraft.getInstance().options.renderDebug == true) 
                    drawDebug(c, fromPos, toPos);
                //////////
                
                Vec3 fromPos = thisNode.getPosition();
                NodeBank other = NodeBank.retrieveFrom(ebe.getLevel(), ebe, theseConnections[c]
                        .getRelativePos());

                if(other == null) continue;

                Vec3 toPos = other.get(theseConnections[c].getDestinationId()).getPosition();
                draw(c, fromPos, toPos, local, modelBuilder);
            }
        }
    }


    private void draw(int iteration, Vec3 fromPos, Vec3 toPos, PoseStack local, 
        VertexConsumer modelBuilder) {
            
        Matrix4f modelMatrix = local.last().pose();
        
    }

    private void drawDebug(int iteration, Vec3 fromPos, Vec3 toPos) {

        CreateClient.OUTLINER.showAABB("wireVF" + iteration, boxFromPos(fromPos))
            .lineWidth(1/32f);
        CreateClient.OUTLINER.showAABB("wireVT" + iteration, boxFromPos(toPos))
            .lineWidth(1/32f);
    }

    private AABB boxFromPos(Vec3 pos) {
        Vec3 size = new Vec3(0.2, 0.2, 0.2);
        return new AABB(pos.subtract(size), pos.add(size));
    }
}
