package com.quattage.mechano.foundation.electricity.system.edge;

import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork;
import com.quattage.mechano.foundation.electricity.system.SVID;

public class ElectricSystemEdge extends SystemEdge {


    private final int transferRate;
    private final SVID sideA;
    private final SVID sideB;

    public ElectricSystemEdge(GlobalTransferNetwork parent, SVID sideA, SVID sideB) {
        super(parent, sideA.combine(sideB));
        this.transferRate = Integer.MAX_VALUE;
        this.sideA = sideA;
        this.sideB = sideB;
    }

    public ElectricSystemEdge(GlobalTransferNetwork parent, SVID sideA, SVID sideB, int transferRate) {
        super(parent, sideA.combine(sideB));
        this.transferRate = transferRate;
        this.sideA = sideA;
        this.sideB = sideB;
    }

    public ElectricSystemEdge(GlobalTransferNetwork parent, WireSpool type, SVID sideA,  SVID sideB) {
        super(parent, sideA.combine(sideB));
        this.sideA = sideA;
        this.sideB = sideB;
        this.transferRate = type.getRate();
    }

    public int getTransferRate() {
        return transferRate;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public boolean rendersWire() {
        return true;
    }

    @Override
    public SVID getSideA() {
        return sideA;
    }

    @Override
    public SVID getSideB() {
        return sideB;
    }
}
