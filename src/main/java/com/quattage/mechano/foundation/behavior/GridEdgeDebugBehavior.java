package com.quattage.mechano.foundation.behavior;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.content.item.DebugButter;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GIDPair;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.quattage.mechano.foundation.network.GridPathViewMaskS2CPacket;

/**
 * Represents GridClientEdges received by the GridClientCache as in-world lines and boxes for debugging puroses
 */
public class GridEdgeDebugBehavior extends ClientBehavior {

    private static Set<GID> mask = null;

    public GridEdgeDebugBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {
        return mainHand.getItem() instanceof DebugButter;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {

        if(mask != null) {
            for(GID id : mask)
                spawnParticles(world, ParticleTypes.GLOW, id.getBlockPos(), 1);
        }

        for(Map.Entry<GIDPair, GID[]> ids : GridClientCache.ofInstance().getAllPaths().entrySet()) {

            if(mask != null && (!ids.getKey().isIn(mask))) continue;

            GID[] steps = ids.getValue();

            for(int x = 0; x < steps.length; x++) {

                Pair<AnchorPoint, WireAnchorBlockEntity> pointA = AnchorPoint.getAnchorAt(world, steps[x]);
                if(pointA == null || pointA.getFirst() == null) continue;

                if(x == 0) VectorHelper.drawDebugBox(pointA.getFirst().getPos());
                else if (x == steps.length - 1) {
                    VectorHelper.drawDebugBox(pointA.getFirst().getPos());
                    continue;
                }

                Pair<AnchorPoint, WireAnchorBlockEntity> pointB = AnchorPoint.getAnchorAt(world, steps[x + 1]);
                if(pointB == null || pointB.getFirst() == null) continue;

                Vec3 from = pointA.getFirst().getPos();
                Vec3 to = pointB.getFirst().getPos();

                Outliner
                    .getInstance()
                    .showLine("edge-" + from + "-" + to, from, to)
                    .lineWidth(1/16f)
                    .disableCull()
                    .disableLineNormals();
            }
        }
    }

    private void spawnParticles(Level world, SimpleParticleType type, BlockPos pos, int count) {
        Random r = new Random();
        for(int x = 0; x < count; x++) {
            Supplier<Double> rS = () -> (r.nextDouble() - 0.5d) * .1f;
		    Supplier<Double> rO = () -> ((r.nextDouble() - 0.5d) * .2f) + 0.5f;
            world.addParticle(type, pos.getX() + rO.get(), pos.getY() + rO.get(), pos.getZ() + rO.get(), rS.get(), rS.get(), rS.get());
        }
    }

    @SuppressWarnings("resource")
    public static void setMask(Level world, Set<GID> ids, ServerPlayer player) {

        if(!world.isClientSide()) {
            MechanoPackets.sendToClient(new GridPathViewMaskS2CPacket(ids), player);
            return;
        }

        mask = ids;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleAsClient(ids));
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("resource")
    private static void handleAsClient(Set<GID> ids) {
        LocalPlayer lp = Minecraft.getInstance().player;
        Level world = Minecraft.getInstance().level;
        if(ids == null) lp.displayClientMessage(Component.literal("§7Cleared edge mask"), true);
        else {
            lp.displayClientMessage(Component.literal("§7Edge mask: Only displaying edges involving §b" + ids), true);
            world.playSound(lp, ((GID)ids.toArray()[0]).getBlockPos(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3f, 1);
        }
    }
    
    @Override
    public double setTickRate() {
        return 200;
    }

    @Override
    public double setTickIncrement() {
        return 1;
    }
}