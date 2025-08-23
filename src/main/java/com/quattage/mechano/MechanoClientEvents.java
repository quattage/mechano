package com.quattage.mechano;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.gridapi.ClientGrid;
import com.quattage.mechano.foundation.gridapi.Griddable;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorGuiLayer;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorSelector;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryModelProvider;
import com.quattage.mechano.foundation.gridapi.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.item.LeftClickCapturable;
import com.quattage.mechano.foundation.item.SpoolItem;
import com.quattage.mechano.foundation.mixin.client.accessor.RenderBuffersAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerChangeGameTypeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    private static final CatenaryModelProvider CATENARY_RESOURCES = new CatenaryModelProvider();

    private static long frameTime = System.nanoTime();
    private static float deltaSeconds = 0f;

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
            cat.renderDynamic(e, evt.getMultiBufferSource(), evt.getPoseStack(), evt.getPartialTick());
        });
    }

    /**
     * Tick the {@link AnchorSelector}
     * and render catenaries attached to the player in first person
     * @param evt
     */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post evt) {
        Minecraft instance = Minecraft.getInstance();
        if(instance == null) return;
        LocalPlayer player = instance.player;
        AnchorSelector.INSTANCE.tick(player, evt.getPartialTick());
        if(player == null) return;

        if(!instance.options.getCameraType().isFirstPerson()) return;
        ((CatenaryAccessor)player).forEachCatenary(cat -> {
            if(!cat.getPrimaryConstruct(player.level()).equals(ClientGrid.getCachedPoints(player).createSupplementaryAddress()))
                return;
            cat.renderDynamic(player, new Vec3(0, player.getBbHeight() * 0.9f, 0), 
                Minecraft.getInstance().renderBuffers().bufferSource(), new PoseStack(), evt.getPartialTick().getGameTimeDeltaPartialTick(false));
        });

        long now = System.nanoTime();
        deltaSeconds = (now - frameTime) / 1_000_000_000f;
        frameTime = now;
    }

    public static float getDeltaSeconds() {
        return deltaSeconds;
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

    /**
     * The methods here describe various edge cases which can occur while the player is in the process of creating 
     * a new wire between two points. While the player is holding a spool with a wire attached to it, they could log out,
     * die, change their game mode, etc - These cases need to be accounted for cuz they'll break shit, yknow?
     */
    @SubscribeEvent public static void onChangeMode(ClientPlayerChangeGameTypeEvent evt) { ClientGrid.destroyCachedPoints(); }
    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut evt) { ClientGrid.destroyCachedPoints(); }
    // the player dying is handled by the LivingEntityMixin since death-related events apply to all entities

    public static void onRegisterLayers(RegisterGuiLayersEvent evt) {
        evt.registerAbove(VanillaGuiLayers.HOTBAR, Mechano.asResource("ancor_selection"), AnchorGuiLayer::renderOverlay);
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent evt) {
        evt.registerReloadListener(CATENARY_RESOURCES);
    }
}
