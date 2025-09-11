package com.quattage.mechano.infrastructure.command;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.quattage.mechano.foundation.helper.VectorHelper.Ray;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class LinkPeekCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> make() {
        return Commands.literal("peek")
            .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if(!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Couldn't peek from non-player source"));
                        return 1;
                    }
                    ServerGrid grid = SidedGridDispatcher.server(sp);
                    int size = grid.matrices == null ? 0 : grid.matrices.size();
                    CatnipServices.NETWORK.sendToClient(sp, new LinkPeekRequestPacket(size));
                    return 1;
                });
    }


    private static void printHolderDetails(LocalPlayer player, @Nullable IAttachmentHolder holder) {
        if(holder == null) {
            player.sendSystemMessage(Component.literal("The targeted object has no links.").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return;
        }
        LinkDataStorable<?> storage = LinkDataStorable.getAsClient(holder);
        if(storage == null) {
            player.sendSystemMessage(Component.literal("The targeted object has no links.").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return;
        }
        MutableComponent list = Component.literal("Link summary for " + holder.getClass().getSimpleName() + ":\n");
        storage.forEach(cat -> list.append(Component.literal("  - [" + cat.getStart() + " -> " + cat.getEnd() + "], [" + cat.getTransmitter() + "]" + "\n")));
        player.sendSystemMessage(list);
    }


    public static record LinkPeekRequestPacket(int size) implements ClientboundPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, LinkPeekRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, LinkPeekRequestPacket::size,
            LinkPeekRequestPacket::new
        );
        @Override public PacketTypeProvider getTypeProvider() { return MechanoPackets.LINK_PEEK_S2C; }
        @Override
        public void handle(LocalPlayer player) {

            final ClientLevel world = (ClientLevel)player.level();
            final HitResult result = Minecraft.getInstance().hitResult;

            IAttachmentHolder holder = null;
            if(result != null) {
                if(result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blr) {  
                    BlockEntity be = world.getBlockEntity(blr.getBlockPos());
                    if((be instanceof GriddableBlockEntity gbe)) 
                        holder = gbe;
                }
                if(result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult elr) {
                    Entity target = elr.getEntity();
                    if(target != null && target.hasData(MechanoData.LINK_ATTACHMENT)) 
                        holder = target;
                }
            }
            if(holder == null) {
                Ray ray = VectorHelper.getLookingRay(player, DeltaTracker.ONE.getGameTimeDeltaPartialTick(false), (float)player.blockInteractionRange());
                List<Entity> nearbyEntities = world.getEntitiesOfClass(Entity.class, AABB.ofSize(player.position(), 10d, 10d, 10d));
                for(Entity e : nearbyEntities) {
                    Optional<Vec3> clip = e.getBoundingBox().clip(ray.start, ray.end);
                    if(!clip.isPresent()) continue;
                    if(e.hasData(MechanoData.LINK_ATTACHMENT))
                        holder = e;
                }
            }
            printHolderDetails(player, holder);
        }
    }
}
