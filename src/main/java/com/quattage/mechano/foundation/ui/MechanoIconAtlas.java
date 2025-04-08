package com.quattage.mechano.foundation.ui;

import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MechanoIconAtlas {

    private final ResourceLocation location;

    private final int size;
    private int activeX = 0;
    private int activeY = -1;

	public MechanoIconAtlas (ResourceLocation location, int size) {
		this.location = location;
		this.size = size;
    }


	public MechanoIcon shift() {
		return new MechanoIcon(activeX++, activeY);
	}

	public MechanoIcon newRow() {
		return new MechanoIcon(activeX = 0, activeY++);
	}

	public class MechanoIcon implements ScreenElement {

		private final int locationX;
		private final int locationY;

		public MechanoIcon(int locationX, int locationY) {
			this.locationX = locationX * 16;
			this.locationY = locationY * 16;
		}

		@OnlyIn(Dist.CLIENT)
		@Override
		public void render(GuiGraphics graphics, int x, int y) {
			graphics.blit(location, x, y, 0, locationX, locationY, 16, 16, 256, 256);
		}

		@OnlyIn(Dist.CLIENT)
		public void render(PoseStack ms, MultiBufferSource buffer, int color) {
			VertexConsumer builder = buffer.getBuffer(RenderType.text(location));
			Matrix4f matrix = ms.last().pose();
			Color rgb = new Color(color);
			int light = LightTexture.FULL_BRIGHT;

			Vec3 vec1 = new Vec3(0, 0, 0);
			Vec3 vec2 = new Vec3(0, 1, 0);
			Vec3 vec3 = new Vec3(1, 1, 0);
			Vec3 vec4 = new Vec3(1, 0, 0);

			float u1 = locationX * 1f / size;
			float u2 = (locationX + 16) * 1f / size;
			float v1 = locationY * 1f / size;
			float v2 = (locationY + 16) * 1f / size;

			newVert(builder, matrix, vec1, rgb, u1, v1, light);
			newVert(builder, matrix, vec2, rgb, u1, v2, light);
			newVert(builder, matrix, vec3, rgb, u2, v2, light);
			newVert(builder, matrix, vec4, rgb, u2, v1, light);
		}

		@OnlyIn(Dist.CLIENT)
		private void newVert(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
			builder.vertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z)
				.color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
				.uv(u, v)
				.uv2(light)
				.endVertex();
		}

		@OnlyIn(Dist.CLIENT)
		public DelegatedStencilElement asStencil() {
			return new DelegatedStencilElement().withStencilRenderer((ms, w, h, alpha) -> this.render(ms, 0, 0)).withBounds(16, 16);
		}

		@OnlyIn(Dist.CLIENT)
		public void bind() {
			RenderSystem.setShaderTexture(0, location);
		}
	}
}