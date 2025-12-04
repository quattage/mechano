package com.quattage.mechano;

import com.quattage.mechano.api.catenary.model.CatenaryModel;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.foundation.LeftClickCapturable;
import com.quattage.mechano.foundation.item.MechanoItemProperties;
import com.quattage.mechano.foundation.item.MechanoItemProperties.SpoolFullnessProperty;
import com.quattage.mechano.foundation.mixin.client.accessor.RenderBuffersAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    // private static final CatenaryModelProvider CATENARY_RESOURCES = new CatenaryModelProvider();

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
        // LivingEntity e = evt.getEntity();
        // Griddable<?>holder = GriddableEntityAttachment.of(e, false);
        // if(holder == null) return;
        // float pTicks = evt.getPartialTick();
        // ((CatenaryAccess)e).forEachCatenary(cat -> {
        //     Vec3 offsetOverride = e.getRopeHoldPosition(pTicks).subtract(e.getPosition(pTicks));
        //     cat.render(e, offsetOverride, evt.getMultiBufferSource(), evt.getPoseStack(), pTicks);
        // });
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
        JackSelector.getInstance().tick(player, evt.getPartialTick());
        if(player == null) return;

        // if(!instance.options.getCameraType().isFirstPerson()) return;
        // ((CatenaryAccess)player).forEachCatenary(cat -> {
        //     if(!cat.getPrimaryConstruct(player.level()).equals(GriddableEntityAttachment.of(player, false).createAddress()))
        //         return;
        //     cat.render(player, new Vec3(0, player.getBbHeight() * 0.9f, 0), 
        //         Minecraft.getInstance().renderBuffers().bufferSource(), new PoseStack(), evt.getPartialTick().getGameTimeDeltaPartialTick(false));
        // });
    }


    /**
     * Draws nearby anchors submitted to the {@link AnchorSelector}
     * at the BE phase of the current frame
     */
    @SubscribeEvent
    public static void onRenderStageComplete(RenderLevelStageEvent evt) {
        if(evt.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        JackSelector.getInstance()
            .draw(evt.getCamera(), evt.getPoseStack(), 
                ((RenderBuffersAccessor)evt.getLevelRenderer())
                .mechano$getRenderBuffers().bufferSource().getBuffer(RenderType.lines()),
                evt.getPartialTick()
            );
    }

    /**
     * Cancel the vanilla selection highlight if the player is currently
     * targeting an {@link AnchorPoint}
     */
    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block evt) {
        evt.setCanceled(JackSelector.getInstance().hasSelection());
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
     * This is the hook where {@link CatenaryModel} instances get rendered
     * to chunks if both ends of said model are attached to immovable,
     * voxel-adjacent elements. This event collects links attached to
     * the section via the registered {@link LinkDataStorage data attachment}.
     */
    // @SubscribeEvent
    // public static void onSectionMeshed(AddSectionGeometryEvent evt) {
    //     SectionPos pos = SectionPos.of(evt.getSectionOrigin());
    //     ClientLevel world = (ClientLevel)evt.getLevel(); 
    //     LinkDataStorage.ClientSectionable storage = LinkDataStorage.getAsClient(world.getChunk(pos.getX(), pos.getZ()), false);
    //     if(storage == null) return;
    //     LinkDataStorage.Client section = storage.getStorageInSection(pos.getY());
    //     if(section == null) return;
    //     evt.addRenderer(ctx -> GridCatenary.renderToSection(world, pos, evt.getSectionOrigin(), section.getAll(), ctx));
    // }

    public static void onRegisterLayers(RegisterGuiLayersEvent evt) {
        evt.registerAbove(VanillaGuiLayers.HOTBAR, Mechano.asResource("anchor_selection"), (graphics, deltas) -> { JackSelector.getInstance().renderOverlay(Minecraft.getInstance(), graphics, deltas); });
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent evt) {
        // evt.registerReloadListener(MechanoClientEvents.CATENARY_RESOURCES);
        ItemProperties.register(MechanoItems.SPOOL_HOOKUP.get(), MechanoItemProperties.FULLNESS, new SpoolFullnessProperty());
    }
}
