package com.quattage.mechano.foundation.item;

import static com.quattage.mechano.Mechano.lang;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.identifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.mixin.client.ItemInHandRendererInvoker;
import com.quattage.mechano.foundation.mixin.client.ItemInHandRendererMixin;
import com.quattage.mechano.foundation.mixin.client.accessor.PlayerInfoAccessor;
import com.quattage.mechano.infrastructure.datagen.SpoolDataProvider;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class SpoolItem<T extends Transmitter<?>> extends Item implements Transmitable<T>, LeftClickCapturable {

    private int startingDamage = -1;

    public SpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) return InteractionResultHolder.pass(Transmitable.getHolding(player).stack());
        if(usedHand != InteractionHand.MAIN_HAND || !AnchorSelector.INSTANCE.hasSelection()) 
            return InteractionResultHolder.fail(AnchorSelector.INSTANCE.getHeldStack());
        ItemStack stack = AnchorSelector.INSTANCE.getHeldStack();
        if(!stack.has(UUIDDiscriminator.ATTACHMENT)) 
            return handleFirstRightClick((LocalPlayer)player, stack, AnchorSelector.INSTANCE.getSelected());
        return handleSecondRightClick((LocalPlayer)player, stack, AnchorSelector.INSTANCE.getSelected());
    }

    /**
     * Called when the player initially selects an anchor, which creates a temporary link and catenary
     * between the selected anchor and the player.
     */
    private InteractionResultHolder<ItemStack> handleFirstRightClick(LocalPlayer player, ItemStack stack, @Nullable AnchorPoint startAnchor) {
        if(startAnchor == null || !AnchorSelector.INSTANCE.isSelectedGood()) {
            cancelAwaitingConnection(startAnchor.getAddress(), null, stack);
            return InteractionResultHolder.fail(stack);
        }
        Griddable<?> playerPoints = GriddableEntityAttachment.of(player, true);
        if(playerPoints == null || !playerPoints.getAnchor().hasRoom()) {
            cancelAwaitingConnection(startAnchor.getAddress(), null, stack);
            return InteractionResultHolder.fail(stack);
        }
        GridResponse result = SidedGridDispatcher.client(player).requestLinkCreation(playerPoints.getAnchor(), startAnchor, getTransmitterType(), true);
        startingDamage = stack.getDamageValue();
        if(!result.indicatesCompletion()) {
            cancelAwaitingConnection(startAnchor.getAddress(), null, stack);
            Mechano.LOGGER.warn("Link request returned failure state '" + result + "' (Requested by '" + player.getName() + "', from " + startAnchor + " -> " + playerPoints.getAnchor() + ")");
            return InteractionResultHolder.fail(stack);
        }
        stack.set(UUIDDiscriminator.ATTACHMENT, startAnchor.getAddress());
        return InteractionResultHolder.success(stack);
    }

    /**
     * Called when the player selects a second anchor, which finalizes the creation of a valid link and catenary
     */
    private InteractionResultHolder<ItemStack> handleSecondRightClick(LocalPlayer player, ItemStack stack, @Nullable AnchorPoint endAnchor) {

        if(endAnchor == null) {
            cancelAwaitingConnection(null, null, stack);
            return InteractionResultHolder.fail(stack);
        }

        GridResponse initialResponse = AnchorSelector.INSTANCE.getSelectedResponse();
        if(!initialResponse.indicatesCompletion()) {
            if(initialResponse.shouldFailHard()) {
                onLeftClick((LocalPlayer)player, stack, InteractionHand.MAIN_HAND);
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        
        GridUUID startAddress = stack.get(UUIDDiscriminator.ATTACHMENT);
        AnchorPoint startAnchor = startAddress.getAnchor((ClientLevel)player.level());
        if(startAnchor == null) {
            cancelAwaitingConnection(null, endAnchor.getAddress(), stack);
            return InteractionResultHolder.fail(stack);
        }

        if(startAnchor.getAddress().equals(endAnchor.getAddress()))
            return InteractionResultHolder.pass(stack);

        ClientGrid grid = SidedGridDispatcher.client(player);
        GridResponse result = grid.requestLinkCreation(startAnchor, endAnchor, getTransmitterType(), true);
        if(!result.indicatesCompletion()) {
            if(result.shouldFailHard()) {
                onLeftClick((LocalPlayer)player, stack, InteractionHand.MAIN_HAND);
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        Griddable<?> playerPoints = GriddableEntityAttachment.of(player, true);
        result = grid.requestLinkDestruction(startAnchor, playerPoints.getAnchor(), true);
        applyDurability(player, stack, GridConnection.getEuclideanDistance(player.level(), startAddress, endAnchor.getAddress()));
        stack.remove(UUIDDiscriminator.ATTACHMENT);
        startingDamage = -1;
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {

        if(!world.isClientSide) return;
        GridUUID startAddress = stack.get(UUIDDiscriminator.ATTACHMENT);
        if(startAddress == null) return;

        EntityUUID playerAddress = new EntityUUID(entity.getUUID(), 0);
        AnchorPoint startAnchor = startAddress.getAnchor((ClientLevel)world);
        if(startAnchor == null || (startAnchor.getCurrentConnections() > startAnchor.getMaxConnections())) {
            Mechano.LOGGER.warn("Connection to " + startAnchor + " was cancelled prematurely.");
            cancelAwaitingConnection(startAnchor == null ? null : startAnchor.getAddress(), playerAddress, stack);
            return;
        }

        LinkDataStorable.Client storage = LinkDataStorable.getAsClient(entity, false);
        if(storage == null) return;
        GridCatenary cat = storage.get(world, new ConnectionKey(playerAddress, startAddress));
        if(cat == null) return;
        applyDurability(entity, stack, cat.getSpan());
        cat.adjustSpan(world, (stack.getMaxDamage() - stack.getDamageValue()) / 2f);
    }

    private void cancelAwaitingConnection(@Nullable GridUUID startAddress, @Nullable GridUUID endAddress, ItemStack stack) {
        stack.remove(UUIDDiscriminator.ATTACHMENT);
        if(startingDamage > -1) stack.setDamageValue(startingDamage);
        startingDamage = -1;
        if(startAddress == null || endAddress == null) return;
        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAddress, endAddress, MechanoTransmissionTypes.PERFECT_CONDUCTOR, GridResponse.TASK_DESTROY_LINK));
    }


    @Override
    public boolean onLeftClick(LocalPlayer player, ItemStack stack, @Nullable InteractionHand hand) {
        if(!stack.has(UUIDDiscriminator.ATTACHMENT)) return false;
        GridUUID addr = stack.get(UUIDDiscriminator.ATTACHMENT);
        if(addr == null) return false;

        AnchorPoint previous = addr.getAnchor((ClientLevel)player.level());
        if(previous == null) {
            GriddableEntityAttachment entityHost = player.getData(MechanoData.ANCHOR_ATTACHMENT);
            CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(entityHost.getOrCreateAddress(), addr, MechanoTransmissionTypes.PERFECT_CONDUCTOR, GridResponse.TASK_DESTROY_LINK));
            return true;
        }

        Vec3 disp = previous.getPos(player.level()).subtract(player.getPosition(1)).normalize();
        float faceDot = (float)player.getViewVector(1).dot(disp);
        if(faceDot < CatenaryAttributes.DETACH_THRESHOLD) return false;
        GriddableEntityAttachment entityHost = player.getData(MechanoData.ANCHOR_ATTACHMENT);
        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(entityHost.getOrCreateAddress(), addr, MechanoTransmissionTypes.PERFECT_CONDUCTOR, GridResponse.TASK_FREE_LINK));
        cancelAwaitingConnection(addr, null, stack);
        if(hand != null) {
            player.swing(hand);
            player.swingTime = 1;
        }
        return true;
    }


    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.has(UUIDDiscriminator.ATTACHMENT);
    }


    /**
     * A helper method for setting the durability of this spool
     * based on the proportional length of the currently awaiting catenary
     * @param stack
     * @param length
     * @param maxLength
     */
    private void applyDurability(Entity entity, ItemStack stack, float length) {
        if(startingDamage < 0) return;
        if(entity instanceof LocalPlayer lp) {
            GameType mode = ((PlayerInfoAccessor)lp).mechano$getPlayerInfo().getGameMode();
            if(mode == null || mode == GameType.CREATIVE || mode == GameType.SPECTATOR)
                return;
        }            
        stack.setDamageValue(Math.min(stack.getMaxDamage(), Math.max(1, startingDamage + (int)Math.ceil((length * 2f)))));
    }

    /**
     * Minecraft's default "Durability: xx/xx" tooltip is added
     * to all damageable items automatically. If this method
     * returns <code>true</code>, that behaviour is skipped
     * by the {@link MechanoClientEvents#onTooltipGather tooltip overwriter.}
     * This method is designed to be used in conjunction with
     * an override to {@link #appendHoverText} to replace the 
     * durability indicator with one that makes more sense
     * for spools (by clarifying the unit as a meter)
     */
    public boolean hidesDefaultTooltip() { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        float max = (float)stack.getMaxDamage() / 2f;
        float current = max - ((float)stack.getDamageValue() / 2f);
        lang().text(String.format("%.1f", current) + "/" + String.format("%.1f", max) + "m").style(ChatFormatting.GRAY).forGoggles(tooltipComponents);
    }

    /**
     * Overrides the vanilla {@link ItemInHandRenderer} behaviour as invoked by the
     * {@link ItemInHandRendererMixin mixin.} You may implement your own logic here 
     * for determining how the player should hold this spool in first person, or 
     * you can simply return <code>false</code> here to do nothing and use the 
     * default pose.
     * @return <code>true</code> if traditional hand rendering should
     * be cancelled in favor of a custom implementation defined within
     * the scope of this method.
     */
    public boolean renderInHands(ItemStack item, MultiBufferSource bufferSource, PoseStack matrixStack, AbstractClientPlayer player, ItemInHandRenderer renderer, float swingProgress, float equipProgress, float pitch, float pTicks, int packedLight) {
        
        // float f = Mth.sqrt(swingProgress);
        // float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        // float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        // matrixStack.translate(0.0F, -f1 / 2.0F, f2);
        float tilt = ((ItemInHandRendererInvoker)renderer).mechano$calculateMapTilt(pitch);
        matrixStack.translate(0f, 0.2f + equipProgress * -1.2f + tilt * -0.5f, -0.72f);
        matrixStack.mulPose(Axis.XP.rotationDegrees(tilt * -85f));

        if (!player.isInvisible()) {
            matrixStack.pushPose();
            matrixStack.mulPose(Axis.YP.rotationDegrees(90));
            ((ItemInHandRendererInvoker)renderer).mechano$renderMapHand(matrixStack, bufferSource, packedLight, HumanoidArm.RIGHT);
            ((ItemInHandRendererInvoker)renderer).mechano$renderMapHand(matrixStack, bufferSource, packedLight, HumanoidArm.LEFT);
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        matrixStack.mulPose(Axis.YP.rotationDegrees(90).mul(Axis.XN.rotationDegrees(290)));
        matrixStack.translate(0.2f, -0.3f, 0.17);
        renderer.renderItem(player, item, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, matrixStack, bufferSource, packedLight);
        matrixStack.popPose();

        return true;
    }











    // the rest of the code here pertains to registering and using the ItemProperty function
    // which allows the spool to change texture based on its durability percent
    public static final ResourceLocation FULLNESS = Mechano.asResource("full");
    public static ArrayList<ItemBuilder<SpoolItem<?>, CreateRegistrate>> spools = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static <T extends SpoolItem<?>> ItemBuilder<T, CreateRegistrate> make(ItemBuilder<T, CreateRegistrate> builder) {
        spools.add((ItemBuilder<SpoolItem<?>, CreateRegistrate>) builder);
        return builder
            .setData(ProviderType.ITEM_MODEL, SpoolDataProvider::generate)
            .properties(p -> p.stacksTo(1).setNoRepair().craftRemainder(MechanoItems.SPOOL_EMPTY.get().asItem())
        );
    }

    public static void registerSpoolProperties() {
        for(ItemBuilder<SpoolItem<?>, CreateRegistrate> b : spools)
            ItemProperties.register(b.getEntry(), SpoolItem.FULLNESS, new SpoolFullnessProperty());
        spools = null;
    }

    @SuppressWarnings("deprecation")
    public static class SpoolFullnessProperty implements ItemPropertyFunction {
        @Override
        public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            float out = 1f - ((float)stack.getDamageValue() / stack.getOrDefault(DataComponents.MAX_DAMAGE, 512));
            return out;
        }
    }
    
}
