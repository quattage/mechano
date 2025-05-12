package com.quattage.mechano.foundation.api;

import com.quattage.mechano.foundation.api.client.AnchorPoint;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.api.network.LinkRequestPacket;
import com.quattage.mechano.foundation.api.transmission.Transmitable.LinkResponse;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelReader;

public class GlobalClientGrid extends SidedGridDispatcher {

    public static GlobalClientGrid loadFrom(ListTag serializedGlobals, ClientLevel world) {
        return new GlobalClientGrid(world);
    }

    protected GlobalClientGrid(ClientLevel world) {
        super(world);
    }

    @Override
    public ClientLevel getWorld() {
        return (ClientLevel)super.getWorld();
    }

    /**
     * 
     * @param startBE the block entity hosting the starting node
     * @param startAnchor the address of the starting node
     * @param endBE the block entiity hosting the ending node
     * @param endAnchor The address of the ending node
     * @param type The type transmitter that the resulting link will host
     */
    public LinkResponse requestLink(AnchorPoint startAnchor, AnchorPoint endAnchor, TransmitterType<?> type) {

        if(!type.ignoresLimits()) {
            if(!endAnchor.hasRoom()) return LinkResponse.FAIL_DESTINATION_FULL;
            if(!endAnchor.isCompatableWith(type)) return LinkResponse.FAIL_DESTINATION_UNSUPPORTED;
        }

        if(type.supportsSameBlockConnections()) {
            if(endAnchor.equals(startAnchor)) return LinkResponse.FAIL_DUPLICATE;    
        } else if(startAnchor.isLocatedAt(endAnchor.getPos())) return LinkResponse.FAIL_DUPLICATE;

        float linkDistance = startAnchor.distanceTo(endAnchor);
        if(linkDistance < type.getMinDistance()) return LinkResponse.FAIL_TOO_CLOSE;
        if(linkDistance > type.getMaxDistance()) return LinkResponse.FAIL_TOO_FAR;

        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.strip(), endAnchor.strip(), type, LinkRequestPacket.Task.CREATE));
        return LinkResponse.SUCCESS.andBailout();
    }

    public void confirmLink(LevelReader world, NodeIdentifier<?> start, NodeIdentifier<?> end, TransmitterType<?> type) {

        PowerGridBlockEntity startBE = start.getHost(world);
        PowerGridBlockEntity endBE = end.getHost(world);
        if(startBE == null) throw new IllegalArgumentException("Couldn't confirm a link from " + start + " to " + end + " - No PowerGridBlockEntity could be found at the starting address!");
        if(endBE == null) throw new IllegalArgumentException("Couldn't confirm a link from " + start + " to " + end + " - No PowerGridBlockEntity could be found at the ending address!");

        if(!type.ignoresLimits()) {
            startBE.anchors.getByIndex(start.getIndex()).incrementCurrentConnections();
            endBE.anchors.getByIndex(end.getIndex()).incrementCurrentConnections();
        }
    }

    @Override
    protected ListTag writeAll() {
        return new ListTag();
    }

    @Override
    protected String getDistPrefix() {
        return "CLIENT";
    }

    @Override
    public String toString() {
        return "GlobalClientGrid(" + getDimensionName() + ", 0 members)";
    }
}
