package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ClientGrid.LinkDebugRenderer;

import net.minecraft.client.KeyboardHandler;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    
    @Inject(method = "handleDebugKeys(I)Z", at = {@At(value = "HEAD")}, cancellable = true)
    public void mechano$handleCustomF3Key(int key, CallbackInfoReturnable<Boolean> cir) {
        if(key == 48) { // key 0
            LinkDebugRenderer dbr = ClientGrid.getDebugger();
            dbr.toggle();
            ((KeyboardHandler)(Object)this).debugFeedback("GridAPI union lines: " + (dbr.isEnabled() ? "shown" : "hidden"));
            cir.setReturnValue(true);
        }
    }
}
