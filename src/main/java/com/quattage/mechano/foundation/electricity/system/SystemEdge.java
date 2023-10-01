package com.quattage.mechano.foundation.electricity.system;

import com.quattage.mechano.foundation.electricity.WireSpool;

public class SystemEdge {


    private final boolean isVirtual;
    private final int transferRate;

    public SystemEdge() {
        isVirtual = true;
        transferRate = 0;
    }

    public SystemEdge(WireSpool connectionType, boolean isVirtual) {
        if(connectionType == null) {
            transferRate = 0;
        } else {
            transferRate = connectionType.getRate();
        }

        this.isVirtual = isVirtual;
    }

    public int getTransferRate() {
        return transferRate;
    }
}
