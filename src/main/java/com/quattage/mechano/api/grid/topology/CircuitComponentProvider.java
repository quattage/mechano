package com.quattage.mechano.api.grid.topology;

import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.api.switchboard.action.GridAction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface CircuitComponentProvider {
    /**
     * Evalutaes the provided {@link Node} and returns a {@link GridAction response}
     * indicating whether or not the targeted joint should be highlighted by the {@link JackSelector selector}
     * <p>
     * <h3>Remember to tag implementations with</h3> 
     * <pre>@OnlyIn(Dist.CLIENT)</pre>
     * @param world world to operate whithin
     * @param target the {@link AncillaryNode} currently targeted by the {@link Minecraft#player local player}
     * @return A {@link GridAction action} 
     */
    @OnlyIn(Dist.CLIENT)
    default GridAction evaluateTarget(ClientLevel world, AncillaryNode target) {
        return GridAction.RESPONSE_SUCCESS;
    }

    /**
     * Gets the CircuitComponent associated with this object, and throws errors if anything goes wrong.
     * @return A new CircuitComponent instance constructed by this object.
     */
    default CircuitComponent getComponentSafe() {
        CircuitComponent output = null;
        try { output = getComponent(); }
        catch(RuntimeException e) {
            e.printStackTrace();
            throw new CircuitComponentProviderException(this, "Construction failed! (see exception above)");
        }
        if(output == null)
            throw new CircuitComponentProviderException(this, "Construction failed (factory returned null)");
        return output;
    }

    CircuitComponent getComponent();

    public static class CircuitComponentProviderException extends RuntimeException {
        public CircuitComponentProviderException(Object source, String message) {
            super("Failed while getting CircuitComponent from '" + source.getClass().getSimpleName() 
                + ((message == null || message.isEmpty()) ? "" : ("' - " + message)));
        }
        public CircuitComponentProviderException(Object source) {
            super("Failed while getting CircuitComponent from '" + source.getClass().getSimpleName());
        }
    }
}
