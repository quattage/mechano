package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.block.anchor.AnchorEntry;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.block.anchor.AnchorVertexData;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;
import com.quattage.mechano.foundation.helper.TickingTimeTracker;
import com.quattage.mechano.foundation.network.AnchorStatRequestC2SPacket;
import com.simibubi.create.AllSpecialTextures;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class WireAnchorSelectionManager {

    private final Minecraft instance;
    private final HashSet<AnchorEntry> nearbyAnchors;
    private final TickingTimeTracker packetClock = new TickingTimeTracker(500);

    // only for displaying temporary warning messages
    public final TickingTimeTracker tenaciousTerriblyTemporaryTickingTracker = new TickingTimeTracker(5000);

    public boolean hasOutgoingRequest = false;
    @Nullable private AnchorEntry selectedAnchor = null;
    @Nullable private AnchorVertexData selectedVertex = null;

    public WireAnchorSelectionManager(Minecraft instance) {
        this.instance = instance;
        this.nearbyAnchors = new HashSet<>(11);
    }

    public HashSet<AnchorEntry> allNearbyEntries() {
        return nearbyAnchors;
    }

    /**
     * Tick AnchorPoints belonging to the given WireAnchorBlockEntity.
     * Designed to be invoked by {@link WireAnchorBlockRenderer}
     * @param wbe
     */
    public void tickAnchorsBelongingTo(WireAnchorBlockEntity wbe) {
        if(!canTick()) return;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            double delta = wbe.getTimeSinceLastTick();
            for(AnchorPoint anchor : wbe.getAnchorBank().getAll()) {

                if(anchor == null) continue;
                AnchorEntry entry = new AnchorEntry(wbe, anchor, instance.player);
                if(!AnchorEntry.isValid(entry)) continue;

                if(entry.getDistance() < 11) {

                    if(!nearbyAnchors.add(entry)) {
                        nearbyAnchors.remove(entry);
                        nearbyAnchors.add(entry);
                    }

                    if(WireSpool.getHeldByPlayer(instance.player) != null) {
                        if(isAnchorSelected(anchor))
                            anchor.increaseToSize(MechanoSettings.ANCHOR_SELECT_SIZE, delta * 0.2f);
                        else {
                            if(anchor.getSize() > MechanoSettings.ANCHOR_NORMAL_SIZE)
                                anchor.decreaseToSize(MechanoSettings.ANCHOR_NORMAL_SIZE, delta * 0.2f);
                            else if(anchor.getSize() < MechanoSettings.ANCHOR_NORMAL_SIZE)
                                anchor.increaseToSize(MechanoSettings.ANCHOR_NORMAL_SIZE, delta * 0.2f);
                        }
                        
                    } else anchor.decreaseToSize(0, delta * 0.2f);
                } else {
                    nearbyAnchors.remove(entry);
                    anchor.decreaseToSize(0, delta * 0.2f);
                }
            }
        });
    }

    public boolean isAnchorSelected(AnchorEntry entry) {
        if(entry == null) return false;
        return isAnchorSelected(entry.get());
    }

    public boolean isAnchorSelected(AnchorPoint anchor) {
        if(anchor == null) return false;
        if(!AnchorEntry.isValid(selectedAnchor)) return false;
        return anchor.equals(selectedAnchor.get());
    }

    @Nullable
    public AnchorEntry getSelectedEntry() {
        return selectedAnchor;
    }

    @Nullable
    public AnchorPoint getSelectedAnchor(Level world) {
        if(world == null) return null;
        if(selectedAnchor == null) return null;
        if(selectedAnchor.get() == null) return null;
        if(AnchorPoint.getAnchorAt(world, selectedAnchor.get().getID()) != null)
            return selectedAnchor.get();
        return null;
    }
    

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void tick(ViewportEvent event) {

        Minecraft instance = Minecraft.getInstance();
        double closestDist = Double.MAX_VALUE;

        final List<AnchorEntry> outdated = new ArrayList<>();
        for(AnchorEntry near : MechanoClient.ANCHOR_SELECTOR.allNearbyEntries()) {
            if(near == null || near.get() == null) continue;
            if(AnchorPoint.getAnchorAt(instance.level, near.get().getID()) == null) {
                outdated.add(near);
                if(near.equals(MechanoClient.ANCHOR_SELECTOR.selectedAnchor))
                    MechanoClient.ANCHOR_SELECTOR.selectedAnchor = null;
                continue;
            }

            AABB anchorBox = near.get().getHitbox();
            if(anchorBox != null) {
                int size = (int)near.get().getSize();
                if(size > 0) {
                    Outliner.getInstance().showAABB(near.hashCode(), anchorBox)
                        .disableLineNormals()
                        .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                        .colored(near.get().getColor())
                        .lineWidth(size < 25 ? size * 0.0006f : size * 0.001f);
                }
            }

            if(near.refreshDistance(instance.player) < closestDist) {
                closestDist = near.getDistance();
                MechanoClient.ANCHOR_SELECTOR.selectedAnchor = near;
            }
        }

        boolean holdingSpool = WireSpool.getHeldByPlayer(instance.player) != null;
        MechanoClient.ANCHOR_SELECTOR.resetSelectedIfTooFarAway(holdingSpool);
        MechanoClient.ANCHOR_SELECTOR.drawSimpleSelectedOutline(holdingSpool);
        MechanoClient.ANCHOR_SELECTOR.removeAll(outdated);
        MechanoClient.ANCHOR_SELECTOR.requestVertexDataUpdate(true);
    }

    public void requestVertexDataUpdate(boolean skippable) {

        if(hasOutgoingRequest) return;

        if(selectedAnchor == null) {
            selectedVertex = null;
            return;
        }

        // if the has not looked at an AnchorPoint during 
        // the current session, it will be null
        if(selectedVertex == null)
            packetClock.skip();

        // if the player looks at a new AnchorPoint, send a fresh packet 
        // immediately rather than displaying the outdated data.
        else if(skippable && (!selectedAnchor.get().getID().equals(selectedVertex.getID())))
            packetClock.skip();

        if(packetClock.isDoneTicking()) {
            hasOutgoingRequest = true;
            MechanoPackets.sendToServer(new AnchorStatRequestC2SPacket(selectedAnchor));
        }
        else packetClock.tickTimer();

    }

    private void drawSimpleSelectedOutline(boolean holdingSpool) {
        if(holdingSpool) return;
        AnchorPoint anchor = MechanoClient.ANCHOR_SELECTOR.getSelectedAnchor(instance.level);
        if(anchor == null) return;
        Outliner.getInstance().showAABB(anchor.hashCode(), anchor.getStaticHitbox())
            .disableLineNormals()
            .colored(Color.TRANSPARENT_BLACK)
            .lineWidth(0.003f);
    }

    private void resetSelectedIfTooFarAway(boolean holdingSpool) {
        if(selectedAnchor == null) return;
        if(selectedAnchor.getDistance() > (holdingSpool ? MechanoSettings.ANCHOR_BAILOUT_DISTANCE : 0.25))
            selectedAnchor = null;
    }
    
    public void removeAll(Collection<AnchorEntry> entries) {
        for(AnchorEntry entry : entries)
            nearbyAnchors.remove(entry);
    }

    public void removeAll() {
        nearbyAnchors.clear();
    }

    public void setAnchorData(AnchorVertexData data) {
        if(data == null) throw new NullPointerException("Error setting AnchorData - data is null!");
        this.selectedVertex = data;
    }

    public AnchorVertexData getAnchorData() {
        return selectedVertex;
    }

    public boolean canTick() {
        if(instance.level != null)
            return instance.level.isClientSide();
        return false;
    }
}