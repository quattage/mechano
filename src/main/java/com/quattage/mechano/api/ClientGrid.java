package com.quattage.mechano.api;

import java.util.OptionalDouble;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quattage.mechano.api.grid.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.api.grid.topology.NetlistLookup.ClientNetlistLookup;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public final class ClientGrid extends Grid {

    protected final ClientNetlistLookup lookup = new ClientNetlistLookup();
    private static LinkDebugRenderer debugger;

    public static LinkDebugRenderer getDebugger() {
        if(ClientGrid.debugger == null) ClientGrid.debugger = new LinkDebugRenderer(Minecraft.getInstance());
        return ClientGrid.debugger;
    }

    protected ClientGrid(Level world) {
        super(world);
    }

    @Override
    protected void read(LevelReader world, CompoundTag contents, Provider provider) {
        
    }

    @Override
    protected void write(LevelReader world, CompoundTag contents, Provider provider) {
        
    }

    @Override
    protected void load() {

    }

    @Override
    protected void unload() {
        lookup.reset();
    }

    @Override
    public ClientNetlistLookup lookup() {
        return lookup;
    }

    @Override
    public void tick() {
        // warn("LINKS: \n " + linksAsString());
    }

    public static class LinkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

        private final Minecraft minecraft;
        private boolean enabled = false;

        public static final RenderType DEBUG_LINES_NO_DEPTH = RenderType.create(
            "debug_lines_no_depth",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                .setCullState(RenderStateShard.NO_CULL)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false)
        );

        public LinkDebugRenderer(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        public void toggle() {
        
            enabled = !enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void render(PoseStack matrixStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
            if(!enabled) return;
            ClientGrid grid = Grid.client(minecraft.player);
            grid.lookup.forEachLink(link -> 
                drawSingle(matrixStack, bufferSource, link.getStartAncillary(), link.getEndAncillary(), camX, camY, camZ)
            );
        }

        private void drawSingle(PoseStack matrixStack, MultiBufferSource bufferSource, @Nullable AncillaryNode<?> start, @Nullable AncillaryNode<?> end, double cx, double cy, double cz) {
            if(start == null || end == null) return;
            if(GridReferent.choosePrimary(start, end) != start) return;
            Vector3d startPos = start.getRealPosition();
            Vector3d endPos = end.getRealPosition();
            float x1 = (float)(startPos.x - cx), y1 = (float)(startPos.y - cy), z1 = (float)(startPos.z - cz);
            float x2 = (float)(endPos.x - cx), y2 = (float)(endPos.y - cy), z2 = (float)(endPos.z - cz);
            drawLine(matrixStack, bufferSource.getBuffer(LinkDebugRenderer.DEBUG_LINES_NO_DEPTH), x1, y1, z1, x2, y2, z2, 0.26f);
            drawLine(matrixStack, bufferSource.getBuffer(RenderType.lines()), x1, y1, z1, x2, y2, z2, 1f);
        }

        private void drawLine(PoseStack matrixStack, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float alpha) {
            matrixStack.pushPose();
            LevelRenderer.renderLineBox(matrixStack, consumer, VectorOperations.toAABB(x1, y1, z1, 0.015f), 1, 1, .6f , alpha);
            LevelRenderer.renderLineBox(matrixStack, consumer, VectorOperations.toAABB(x2, y2, z2, 0.015f), 1, 1, .6f , alpha);
            Matrix4f matrix = matrixStack.last().pose();
            float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
            float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (length != 0) { nx /= length; ny /= length; nz /= length; }
            consumer.addVertex(matrix, x1, y1, z1)
                    .setNormal(0, 1, 0)
                    .setColor(1, 1, 0, alpha);
            consumer.addVertex(matrix, x2, y2, z2)
                    .setNormal(0, 1, 0)
                    .setColor(1, 1, 0, alpha);
            matrixStack.popPose();
        }
    }
}
