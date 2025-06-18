package com.quattage.mechano.foundation.api;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.ListTag;

public final class ClientGrid extends SidedGridDispatcher {

    private final CatenaryMesher mesher;
    public final ObjectOpenHashSet<GridCatenary> catenaries;

    public static ClientGrid loadFrom(ListTag serializedGlobals, ClientLevel world) {
        return new ClientGrid(world);
    }

    public ClientGrid(ClientLevel world) {
        super(world);
        this.mesher = CatenaryMesher.asEmpty();
        this.catenaries = new ObjectOpenHashSet<>(4);
    }

    @Override
    public ClientLevel getWorld() {
        return (ClientLevel)super.getWorld();
    }

    /**
     * Request that a link is made. This method does some simple
     * client-sided sanity checks and sents a packet to the 
     * {@link ServerGrid server-sided} version of this 
     * instance. 
     * @param startBE the BlockEntity hosting the starting node
     * @param startAnchor the address of the starting node
     * @param endBE the BlockEntiity hosting the ending node
     * @param endAnchor The address of the ending node
     * @param type The type of transmitter that the resulting link will host
     * @return {@link Response}
     */
    public Response<?> requestLink(AnchorPoint startAnchor, AnchorPoint endAnchor, TransmitterType<?> type) {

        if(!startAnchor.existsIn(world)) {
            Mechano.LOGGER.error("Failed to create link from " + startAnchor.getAddress() + " and " + endAnchor.getAddress() + " - No valid PGBE could be found at the starting address");
            return Response.Link.FAIL_SYNC_OUTDATED.andBailout();
        }

        if(!endAnchor.existsIn(world)) {
            Mechano.LOGGER.error("Failed to create link from " + startAnchor + " and " + endAnchor + " - No valid PGBE could be found at the ending address");
            return Response.Link.FAIL_SYNC_OUTDATED.andBailout();
        }

        if(!type.ignoresLimits()) {
            if(!endAnchor.hasRoom()) return Response.Link.FAIL_DESTINATION_FULL;
            if(!endAnchor.isCompatableWith(type)) return Response.Link.FAIL_DESTINATION_UNSUPPORTED;
        }

        if(type.supportsSameBlockConnections()) {
            if(endAnchor.equals(startAnchor)) return Response.Link.FAIL_DUPLICATE;    
        } else if(startAnchor.getAddress().isApproximately(world, endAnchor.getAddress())) return Response.Link.FAIL_DUPLICATE;

        float linkDistance = startAnchor.distanceTo(world, endAnchor);
        if(linkDistance < type.getMinDistance()) return Response.Link.FAIL_TOO_CLOSE;
        if(linkDistance > type.getMaxDistance()) return Response.Link.FAIL_TOO_FAR;

        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.getAddress(), endAnchor.getAddress(), type, Response.Task.CREATE));
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
        return "ClientGrid(" + getDimensionName() + ", 0 members)";
    }

    public CatenaryMesher getMesher() {
        return mesher;
    }
}
