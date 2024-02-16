package com.quattage.mechano.foundation.electricity.system.edge;

import java.util.ArrayList;
import java.util.List;

import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork;
import com.quattage.mechano.foundation.electricity.system.SVID;
import com.simibubi.create.foundation.utility.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A SystemEdge is a logical representation of a connection between two verticies in a TransferSystem.
 * Where verticies represent connectors themselves, edges can be thought of as the wires between connectors.
 */
public abstract class SystemEdge {


    private final SVIDPair target;

    public SystemEdge(GlobalTransferNetwork parent, SVIDPair edgeID) {
        if(parent == null) throw new NullPointerException("Error instantiating SystemEdge - Parent network is null!");
        if(edgeID == null) throw new NullPointerException("Error instantiating SystemEdge - Target is null!");
        this.target = edgeID;
        if(parent.isClient()) {
            Object2ObjectOpenHashMap<SectionPos, List<SystemEdge>> clientRenderQueue = parent.getClientRenderQueue();
            SectionPos sectionPosition = SectionPos.of(edgeID.getSideA().getPos());
            List<SystemEdge> section = clientRenderQueue.get(sectionPosition);
            if(section == null) section = new ArrayList<SystemEdge>();
            section.add(this);
            parent.getClientRenderQueue().put(SectionPos.of(edgeID.getSideA().getPos()), section);
        }
    }

    /***
     * Whether or not this SystemEdge should have a wire rendered for it
     * @return
     */
    abstract boolean rendersWire();

    /***
     * Whether or not this SystemEdge is "real."
     * "Real" wires are tied to a logical representation of some sort in the world - 
     * they can break if stretched too far, they'll drop wire when broken, they can be
     * placed by the player, etc. 
     * 
     * Non-real wires are usually added by the network itself, and are intangible. A good example
     * of a non-real wire would be a wireless or cross-dimensional form of energy transport.
     * @return
    */
    public boolean isReal() { return false; }

    /***
     * Whether or not this SystemEdge can transfer any FE/t
     * @return True if this Edge's transfer rate is > 0
     */
    public boolean canTransfer() {
        return getTransferRate() > 0;
    }

    /***
     * Gets the integer transfer rate in FE per Tick of the is edge
     * @return Positive integer from 0 to int MAX_VALUE
     */
    public int getTransferRate() {
        return 0;
    }

    /***
     * Whether or not this SystemEdge interacts with the SystemNode at the given SVID
     * @param target SVID to check
     * @return True if one of this SystemEdge's ends are at this SVID
     */
    public boolean connectsTo(SVID target) {
        return target == getSideA() || target == getSideB(); 
    }

    public SVID getSideA() {
        return target.getSideA();
    }

    public SVID getSideB() {
        return target.getSideB();
    }

    /***
     * @return BlockPos position of this edge's A side
     */
    public BlockPos getPosA() {
        return getSideA().getPos();
    }

    /***
     * @return BlockPos position of this edge's B side
     */
    public BlockPos getPosB() {
        return getSideB().getPos();
    }

    public Pair<Vec3, Vec3> getPositions(Level world) {
        Pair<AnchorPoint, WireAnchorBlockEntity> cA = AnchorPoint.getAnchorAt(world, getSideA());
        Pair<AnchorPoint, WireAnchorBlockEntity> cB = AnchorPoint.getAnchorAt(world, getSideA());

        if(cA == null || cA.getFirst() == null || cB == null || cB.getFirst() == null) return null;
        return Pair.of(cA.getFirst().getPos(), cB.getFirst().getPos());
    }
}
