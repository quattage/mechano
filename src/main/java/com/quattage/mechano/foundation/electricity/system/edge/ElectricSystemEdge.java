package com.quattage.mechano.foundation.electricity.system.edge;

import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.electricity.system.SVID;

public class ElectricSystemEdge implements ISystemEdge {


    private final int transferRate;
    private final SVID sideA;
    private final SVID sideB;

    public ElectricSystemEdge(SVID sideA, SVID sideB) {
        this.transferRate = Integer.MAX_VALUE;
        this.sideA = sideA;
        this.sideB = sideB;
    }

    public ElectricSystemEdge(SVID sideA, SVID sideB, int transferRate) {
        this.transferRate = transferRate;
        this.sideA = sideA;
        this.sideB = sideB;
    }

    public ElectricSystemEdge(WireSpool type, SVID sideA,  SVID sideB) {
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
