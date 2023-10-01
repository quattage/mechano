package com.quattage.mechano.foundation.electricity.system;

import com.quattage.mechano.Mechano;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class NetworkSavedData extends SavedData {

    public static final int SAVE_VERSION = 0;
    public static final String MECHANO_SAVE_KEY = Mechano.MOD_ID + "-AuxilarySaveData";
    public static final String MECHANO_NETWORK_KEY = Mechano.MOD_ID + ":network";

    public static NetworkSavedData INSTANCE;

    public NetworkSavedData() { super(); }

    public NetworkSavedData(CompoundTag in, ServerLevel initialWorld) {
        this();
        NETWORK.readFrom(in, initialWorld);
    }

    @Override
    public CompoundTag save(CompoundTag in) {
        NETWORK.writeTo(in);
        Mechano.LOGGER.info("Serialized network stack to NBT:\n" + in);
        return in;
    }

    public static void markInstanceDirty() {
        if(INSTANCE != null) {
            Mechano.LOGGER.info("Network stack has been marked dirty");
            INSTANCE.setDirty();
        }
    }

    public static void markInstanceDirty(SVID id) {
        if(INSTANCE != null) {
            Mechano.LOGGER.info("Network stack has been marked dirty from " + id);
            INSTANCE.setDirty();
        }
    }

    public static void setInstance(NetworkSavedData data) {
        INSTANCE = data;
    }
}
