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
 * A wire that connects two
 * {@link WireJack wire jacks}
 */
public class Transmitter implements CircuitComponent {
    
    private GridUUID startID, endID;
    private WireJack startJack, endJack;
    private @Nullable CircuitComponent element; // null if this transmitter is a perfect conductor
    // client catenary data

    public Transmitter(LevelReader world, WireJack sideA, WireJack sideB, float resistance) {
        // iunno
    }

    @Override
    public Collection<Terminal> getTerminals() {
        Collection<Terminal> tA = (startJack == null || !startJack.isSignificant()) ? Collections.emptyList() : startJack.getParentComponent().getTerminals();
        Collection<Terminal> tB = (endJack == null || !endJack.isSignificant()) ? Collections.emptyList() : endJack.getParentComponent().getTerminals();
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
    public void forEachNode(Consumer<Node> cons) {
        cons.accept(startJack);
        cons.accept(endJack);
    }

    public GridUUID getStartID() {
        return startID;
    }

    public WireJack getStart() {
        return startJack;
    }

    public GridUUID getEndID() {
        return endID;
    }

    public WireJack getEnd() {
        return endJack;
    }

    @Override
    public String getComponentID() {
        return "Transmitter";
    }

    @Override
    public String describeState() {
        return startID + ", " + startJack + " -> " + endID + ", " + endJack;
    }

    @Override
    public ResourceLocation asResource() {
        // TODO unfuck
        return Mechano.asResource("trns");
    }

    @Override
    public boolean isSignificant() {
        return startID != null && endID != null;
    }

    @Override
    public boolean isGrounded() {
        return (startJack != null && startJack.isGrounded()) || (endJack != null && endJack.isGrounded()) || (element != null && element.isGrounded());
    }

    public boolean isPerfectConductor() {
        return element == null;
    }

    public @Nullable CircuitComponent getFunctionalElement() {
        return element;
    }

    @Override
    public void saturate() {
        if(element != null) element.saturate();
    }

    @Override
    public void reset() {
        if(element != null) element.reset();
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
        return startJack == null ? null : startJack.getParentComponent();
    }
}
