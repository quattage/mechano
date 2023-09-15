package com.quattage.mechano.foundation.electricity.system;

import com.quattage.mechano.Mechano;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class NetworkSavedData extends SavedData {

    public static final int SAVE_VERSION = 0;
    public static final String MECHANO_SAVE_KEY = Mechano.MOD_ID + "-AuxilarySaveData";
    public static final String MECHANO_NETWORK_KEY = Mechano.MOD_ID + ":network";

    public static NetworkSavedData INSTANCE;

    public NetworkSavedData() { super(); }

    public NetworkSavedData(CompoundTag in) {
        this();
        Mechano.log("!! READING NETWORK FROM DISK !! ");
        NETWORK.readFrom(in);
    }

    @Override
    public CompoundTag save(CompoundTag in) {
        Mechano.log("!! WRITING NETWORK TO DISK !!");
        NETWORK.writeTo(in);
        return in;
    }

    public static void markInstanceDirty() {
        Mechano.log("GlobalTransferNetwork marked as dirty");
        if(INSTANCE != null) INSTANCE.setDirty();
    }

    public static void setInstance(NetworkSavedData data) {
        Mechano.log("Registering SaveData instance");
        INSTANCE = data;
    }
}
