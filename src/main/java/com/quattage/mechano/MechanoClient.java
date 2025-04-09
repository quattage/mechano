package com.quattage.mechano;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.quattage.mechano.foundation.compat.embeddium.EmbeddiumWireCompat;
import com.quattage.mechano.foundation.electricity.grid.WireAnchorSelectionManager;
import com.quattage.mechano.foundation.electricity.rendering.WireTextureProvider;
import com.quattage.mechano.foundation.ui.MechanoIconAtlas;


public class MechanoClient {

    public static final WireTextureProvider WIRE_TEXTURE_PROVIDER = new WireTextureProvider();
    public static final MechanoIconAtlas ICONS = new MechanoIconAtlas(Mechano.asResource("textures/gui/icons.png"), 128);
    public static final WireAnchorSelectionManager ANCHOR_SELECTOR = new WireAnchorSelectionManager(Minecraft.getInstance());
    

    public static final PartialModel
        PART_DIAGIRDER_SDF = newPartial("diagonal_girder/partials/short_down_flat"),
        PART_DIAGIRDER_SDV = newPartial("diagonal_girder/partials/short_down_vert"),
        PART_DIAGIRDER_SUF = newPartial("diagonal_girder/partials/short_up_flat"),
        PART_DIAGIRDER_SUV = newPartial("diagonal_girder/partials/short_up_vert"),
        PART_DIAGIRDER_LDF = newPartial("diagonal_girder/partials/long_down_flat"),
        PART_DIAGIRDER_LDV = newPartial("diagonal_girder/partials/long_down_vert"),
        PART_DIAGIRDER_LUF = newPartial("diagonal_girder/partials/long_up_flat"),
        PART_DIAGIRDER_LUV = newPartial("diagonal_girder/partials/long_up_vert"),
        PART_CHEV_OVERLAY = newPartial("generic/chevron_cutout"),
        PART_CHEV_OVERLAY_INV = newPartial("generic/chevron_cutout_2");

    private static PartialModel newPartial(String path) {
        return PartialModel.of(Mechano.asResource("block/" + path));
	}

    protected static void init(IEventBus modBus, IEventBus forgeBus) {
        MechanoPartialModels.load();
        Mechano.logReg("client");
        if(ModList.get().isLoaded("embeddium") && FMLEnvironment.dist == Dist.CLIENT)
            EmbeddiumWireCompat.registerCompatModule(forgeBus);
    }
}