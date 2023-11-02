package com.quattage.mechano.foundation.electricity.system.edge;

import com.quattage.mechano.foundation.electricity.system.SVID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/***
 * A SystemEdge is a logical representation of a connection between two verticies in a TransferSystem.
 * Where verticies represent connectors themselves, edges can be thought of as the wires between connectors.
 */
public interface ISystemEdge {
    
    /***
     * Whether or not this SystemEdge should have a wire rendered for it
     * @return
     */
    boolean rendersWire();

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
    boolean isReal();

    /***
     * Whether or not this SystemEdge can transfer any FE/t
     * @return True if this Edge's transfer rate is > 0
     */
    default boolean canTransfer() {
        return getTransferRate() > 0;
    }

    /***
     * Gets the integer transfer rate in FE per Tick of the is edge
     * @return Positive integer from 0 to int MAX_VALUE
     */
    default int getTransferRate() {
        return 0;
    }

    /***
     * Whether or not this SystemEdge interacts with the SystemNode at the given SVID
     * @param target SVID to check
     * @return True if one of this SystemEdge's ends are at this SVID
     */
    default boolean connectsTo(SVID target) {
        return target == getSideA() || target == getSideB(); 
    }

    SVID getSideA();
    SVID getSideB();

    /***
     * @return BlockPos position of this edge's A side
     */
    default BlockPos getPosA() {
        return getSideA().getPos();
    }

    /***
     * @return BlockPos position of this edge's B side
     */
    default BlockPos getPosB() {
        return getSideB().getPos();
    }

    /***
     * @return Vec3 absolute position of this edge's A side
     */
    default Vec3 getVecA() {
        return getPosA().getCenter();
    }

    /***
     * @return Vec3 absolute position of this edge's B side
     */
    default Vec3 getVecB() {
        return getPosB().getCenter();
    }
}
