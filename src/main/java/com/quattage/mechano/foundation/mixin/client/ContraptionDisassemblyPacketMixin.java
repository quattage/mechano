package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.LinkDataStorage;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.landmark.GridCatenary;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionDisassemblyPacket;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.client.player.LocalPlayer;

@Mixin(ContraptionDisassemblyPacket.class)
public abstract class ContraptionDisassemblyPacketMixin {

    @Shadow private StructureTransform transform;
    @Shadow private int entityId;
    
    @Inject(
        method = "handle", 
        at = @At(value = "TAIL"), 
        cancellable = false, remap = false
    )
    private void mechano$respondToChangesAsClient(LocalPlayer player, CallbackInfo info) {
        if(!(player.level().getEntity(entityId) instanceof AbstractContraptionEntity ace))
            return;
        ClientGrid grid = SidedGridDispatcher.client(player);        
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(ace, false);
        GridCatenary.replaceEndsOnDisassemble(grid, storage, transform, ace);
    }
}
