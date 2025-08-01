package com.quattage.mechano;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorGuiLayer;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.AnchorSurrogateDestroyPacket;
import com.quattage.mechano.foundation.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.catenary.CatenaryModelProvider;
import com.quattage.mechano.foundation.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.item.LeftClickCapturable;
import com.quattage.mechano.foundation.item.SpoolItem;
import com.quattage.mechano.foundation.mixin.client.accessor.RenderBuffersAccessor;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerChangeGameTypeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    private static final CatenaryModelProvider CATENARY_RESOURCES = new CatenaryModelProvider();

    public static boolean shouldRenderOverlay(Minecraft mc) {
        return !(mc == null || mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR);
    }

    /**
     * Renders catenaries belonging to the event's entity as part of that entity.
     * This allows catenaries to be visible and inherit the same culling behaviour
     * as the entity itself.
     */
    @SubscribeEvent
    public static <T extends LivingEntity, M extends EntityModel<T>> void onRenderLiving(RenderLivingEvent.Pre<T, M> evt) {
        LivingEntity e = evt.getEntity();
        Griddable<?> holder = GriddableEntityAttachment.of(e, false);
        if(holder == null) return;
        ((CatenaryAccessor)e).forEachCatenary(cat -> {
            if(cat == null || !cat.hasPoints() || !cat.canMoveDynamically(e.level())) return;
            cat.renderDynamic(e, evt.getMultiBufferSource(), evt.getPoseStack(), evt.getPartialTick());
        });
    }

    /**
     * Renders catenaries in the specific context not covered above - This hook
     * specifically renders catenaries that belong to the client's LocalPlayer
     * in first person.
     */
    @SubscribeEvent
    public static <T extends LivingEntity, M extends EntityModel<T>> void onViewport(ViewportEvent.ComputeCameraAngles evt) {
        Minecraft instance = Minecraft.getInstance();
        if(instance == null) return;
        if(!instance.options.getCameraType().isFirstPerson()) return;
        LocalPlayer player = instance.player;
        if(player == null) return;
        ((CatenaryAccessor)player).forEachCatenary(cat -> {
            cat.renderDynamicFirstPerson(player, Minecraft.getInstance().renderBuffers().bufferSource(), new PoseStack(), (float)evt.getPartialTick());
        });
    }

    /**
     * Tick the {@link AnchorSelector}
     * @param evt
     */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post evt) {
        AnchorSelector.INSTANCE.tick(Minecraft.getInstance().player, evt.getPartialTick());
    }

    /**
     * Draws nearby anchors submitted to the {@link AnchorSelector}
     * at the BE phase of the current frame
     */
    @SubscribeEvent
    public static void onRenderStageComplete(RenderLevelStageEvent evt) {
        if(evt.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        AnchorSelector.INSTANCE.drawTrackedAnchors(
            evt.getCamera(), evt.getPoseStack(), 
            ((RenderBuffersAccessor)evt.getLevelRenderer())
                .mechano$getRenderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines()),
            evt.getPartialTick()
        );
    }

    /**
     * Cancel the vanilla selection highlight if the player is currently
     * targeting an {@link AnchorPoint}
     */
    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block evt) {
        evt.setCanceled(AnchorSelector.INSTANCE.hasSelection());
    }

    /**
     * Suppresses the inclusion of "Durability: xx/xx" tooltips 
     * on spools that request to do so.
     */
    @SubscribeEvent
    public static void onTooltipGather(RenderTooltipEvent.GatherComponents evt) {
        if(!(evt.getItemStack().getItem() instanceof SpoolItem schpool)) return;
        if(!schpool.hidesDefaultTooltip()) return;
        evt.getTooltipElements().removeIf(line -> line.left().get().getString().startsWith("Durability"));
    }

    @SubscribeEvent
    public static void onLeftClick(InputEvent.MouseButton.Pre evt) {
        Minecraft instance = Minecraft.getInstance();
        if(instance == null || instance.screen != null || evt.getButton() != 0 || evt.getAction() != 1) return;
        LocalPlayer player = instance.player;
        if(player == null) return;
        ItemStack stack = player.getMainHandItem();
        if(stack.getItem() instanceof LeftClickCapturable lcc) {
            evt.setCanceled(lcc.onLeftClick(player, stack, InteractionHand.MAIN_HAND));
            return;
        }
        stack = player.getOffhandItem();
        if(stack.getItem() instanceof LeftClickCapturable lcc)
            evt.setCanceled(lcc.onLeftClick(player, stack, InteractionHand.OFF_HAND));
    }

    @SubscribeEvent
    public static void onChangeMode(ClientPlayerChangeGameTypeEvent evt) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        Griddable<?> points = GriddableEntityAttachment.of(player, false);
        if(points == null) return;
        GridUUID addr = points.getOrCreateAddress();
        if(addr == null) return;
        CatnipServices.NETWORK.sendToServer(new AnchorSurrogateDestroyPacket(addr));
    }

    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Bus {

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent evt) {
            evt.registerAbove(VanillaGuiLayers.HOTBAR, Mechano.asResource("ancor_selection"), AnchorGuiLayer::renderOverlay);
        }
        
        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent evt) {
            evt.registerReloadListener(CATENARY_RESOURCES);
        }
    }
}
