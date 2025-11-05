
package com.quattage.mechano.api.griddable;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * Griddable grants implementations the ability to utilize the {@link SidedGridDispatcher GridAPI}
 * to store and use {@link GridNode GridNodes,} {@link GridLink GridLinks,}
 * and {@link AnchorPoint AnchorPoints}. 
 * Implementations of this class should expect to handle both server and client sided logic
 * in the same object. Methods that can't be called on the server are marked with the cooresponding
 * <code>@OnlyIn</code> annotation.
 */
public interface Griddable<T extends IAttachmentHolder> {

    CircuitComponent getCircuit();
    LazyJointHolder getExposedAncillaries();

    /**
     * Some implementations represent their source directly, like BlockEntities that 
     * implement the Griddable interface. This may not alwyas be the case, though, like
     * for PlayerEntities - The Griddable is a DataAttachment, and the source is the entity
     * itself.
     * @return The actual object that this Griddable represents.
     */
    T getSource();
    Vector3d getSourcePos();
    Quaternionf getSourceRotation();
    float getSourceMass();
    Level getWorld();
    default boolean isMovable() { return false; };
    default void drawGUILabel(List<Component> tooltip, float posX, float posY, GuiGraphics graphics) {}

    default Vector3d getPositionOf(AncillaryJack joint) {
        if(joint == null) return new Vector3d();
        Vector3f local = joint.getRotatedOffset(getSourceRotation());
        return getSourcePos().add(local);
    }

    /**
     * @return a decorative string used for debugging
     */
    default String describeState() {
        return "No state descriptor";
    }
}
