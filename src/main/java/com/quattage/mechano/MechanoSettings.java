package com.quattage.mechano;

import net.neoforged.bus.api.IEventBus;

public class MechanoSettings {
    
    // TODO json
    public static byte ALTERNATOR_MAX_LENGTH = 16;
    public static byte ALTERNATOR_MINIMUM_PERCENT = 55;
    public static short WIRE_ANIM_LENGTH = 300;
    public static byte POLE_STACK_SIZE = 16;

    // 1 Watt = (FE2W_RATE) * FE
    public static short FE2W_RATE = 64; 
    public static short FE2W_VOLTAGE = 128;

    // Modify the total SU2W rate by an arbitrary value
    public static int SU2W_DIVIDEND = 2;
    
    // RPM to voltage exponent
    public static float RPM_VOLTAGE = 1.0247183464f;

    // size of AnchorPoint hitboxes`
    public static byte ANCHOR_SELECT_SIZE = 40;
    public static byte ANCHOR_NORMAL_SIZE = 20;

    // resolution of raymarch to select AnchorPoints - lower numbers = higher resolution
    public static float ANCHOR_SELECT_RAYMARCH_RESOLUTION = 0.01f;

    // distance that's considered "close enough" to select the closest AnchorPoint
    public static float ANCHOR_BAILOUT_DISTANCE = 0.4f;

    public static byte GRID_WORKER_THREADS = 6;


    protected static void init(IEventBus modBus) {
        
    }
}
