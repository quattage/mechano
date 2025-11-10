package com.quattage.mechano.api.grid.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;

/**
 * Represents a wire with a known resistance value that connects two
 * singular {@link Terminal}
 */
public class Transmitter implements CircuitComponent {
    
    private GridUUID idA, idB;
    private WireJack sideA, sideB;
    private CircuitComponent functional;
    // client catenary data

    public Transmitter(LevelReader world, WireJack sideA, WireJack sideB, float resistance) {
        // iunno
    }

    @Override
    public Collection<Terminal> getTerminals() {
        Collection<Terminal> tA = (sideA == null || !sideA.isSignificant()) ? Collections.emptyList() : sideA.getParentComponent().getTerminals();
        Collection<Terminal> tB = (sideB == null || !sideB.isSignificant()) ? Collections.emptyList() : sideB.getParentComponent().getTerminals();
        // i avoid using addAll() here because we cannot guarantee that the collections above are returned as
        // shallow-copies by API users (in fact, its inadvisable to do so) - instead, the collections 
        // are concatenated using primitive arrays
        int tal = tA.size();
        int tbl = tB.size();
        Terminal[] tm = new Terminal[tal + tbl];
        System.arraycopy(tA.toArray(), 0, tm, 0, tal);
        System.arraycopy(tB.toArray(), 0, tm, tal, tbl);
        return Arrays.asList(tm);
    }

    @Override
    public void forEachJoint(Consumer<Node> cons) {
        
    }

    public GridUUID getStartID() {
        return idA;
    }

    public GridUUID getEndID() {
        return idB;
    }

    @Override
    public String getComponentID() {
        return "Transmitter";
    }

    @Override
    public String describeState() {
        return idA + ", " + sideA + " -> " + idB + ", " + sideB;
    }

    @Override
    public ResourceLocation asResource() {
        // TODO unfuck
        return Mechano.asResource("trns");
    }

    @Override
    public boolean isSignificant() {
        return idA != null && idB != null;
    }

    @Override
    public boolean isGrounded() {
        return (sideA != null && sideA.isGrounded()) || (sideB != null && sideB.isGrounded());
    }

    @Override
    public void saturate() {
        
    }

    @Override
    public void reset() {
        this.sideA = null;
        this.sideB = null;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Type getType() {
        return CircuitComponent.Type.TRANSMITTER;
    }

    @Override
    public void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index) {}

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return sideA == null ? null : sideA.getParentComponent();
    }
}
