package com.quattage.mechano.api.grid.topology;

import com.quattage.mechano.api.JackSelector;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.api.switchboard.GridResponse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface CircuitComponentProvider {
    /**
     * Evalutaes the provided {@link Node} and returns a {@link GridResponse response}
     * indicating whether or not the targeted joint should be highlighted by the {@link JackSelector selector}
     * <p>
     * <h3>Remember to tag implementations with</h3> 
     * <pre>@OnlyIn(Dist.CLIENT)</pre>
     * @param world world to operate whithin
     * @param target the {@link AncillaryJack} currently targeted by the {@link Minecraft#player local player}
     * @return
     */
    @OnlyIn(Dist.CLIENT)
    GridResponse evaluateTarget(ClientLevel world, AncillaryJack target);

    CircuitComponent getComponent();
}
