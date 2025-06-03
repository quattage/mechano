package com.quattage.mechano.foundation.api;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.ListTag;

public final class GlobalClientGrid extends SidedGridDispatcher {

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
     * Request that a link is made. This method does some simple
     * client-sided sanity checks and sents a packet to the 
     * {@link GlobalServerGrid server-sided} version of this 
     * instance. 
     * 
     * @param startBE the BlockEntity hosting the starting node
     * @param startAnchor the address of the starting node
     * @param endBE the BlockEntiity hosting the ending node
     * @param endAnchor The address of the ending node
     * @param type The type of transmitter that the resulting link will host
     * @return {@link Response}
     */
    public Response<?> requestLink(AnchorPoint startAnchor, AnchorPoint endAnchor, TransmitterType<?> type) {

        if(!startAnchor.existsInWorld(getLevelReader())) {
            Mechano.LOGGER.error("Failed to create link from " + startAnchor.strip() + " and " + endAnchor.strip() + " - No valid PGBE could be found at the starting address");
            return Response.Link.FAIL_SYNC_OUTDATED.andBailout();
        }

        if(!endAnchor.existsInWorld(getLevelReader())) {
            Mechano.LOGGER.error("Failed to create link from " + startAnchor.strip() + " and " + endAnchor.strip() + " - No valid PGBE could be found at the ending address");
            return Response.Link.FAIL_SYNC_OUTDATED.andBailout();
        }

        if(!type.ignoresLimits()) {
            if(!endAnchor.hasRoom()) return Response.Link.FAIL_DESTINATION_FULL;
            if(!endAnchor.isCompatableWith(type)) return Response.Link.FAIL_DESTINATION_UNSUPPORTED;
        }

        if(type.supportsSameBlockConnections()) {
            if(endAnchor.equals(startAnchor)) return Response.Link.FAIL_DUPLICATE;    
        } else if(startAnchor.isLocatedAt(endAnchor.getPos())) return Response.Link.FAIL_DUPLICATE;

        float linkDistance = startAnchor.distanceTo(endAnchor);
        if(linkDistance < type.getMinDistance()) return Response.Link.FAIL_TOO_CLOSE;
        if(linkDistance > type.getMaxDistance()) return Response.Link.FAIL_TOO_FAR;

        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.strip(), endAnchor.strip(), type, Response.Task.CREATE));
        return Response.SUCCESS;
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
