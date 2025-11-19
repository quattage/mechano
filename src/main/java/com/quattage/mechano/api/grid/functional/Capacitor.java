package com.quattage.mechano.api.grid.functional;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.solver.NodalSnapshot;
import com.quattage.mechano.api.grid.solver.NodalSnapshot.Stamper;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.Terminal;

public class Capacitor extends FunctionalComponent implements Stamper {

    private final Terminal[] terminals;
    private final float capacitance;
    private double prevVoltage;

    public Capacitor(float capacitance) {
        super("Capacitor");
        this.capacitance = capacitance;
        this.terminals = Terminal.polarPair(this);
    }

    @Override
    public Collection<Terminal> getTerminals() {
        return Arrays.asList(terminals);
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        terminals[0].forEachNode(cons);
        terminals[1].forEachNode(cons);
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }

    @Override
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        prevVoltage = terminals[0].getJoint().getVoltage() - terminals[1].getJoint().getVoltage();
        double g = (double)capacitance / Circuit.DELTA;
        int pI = terminals[0].getJoint().getIndex();
        int nI = terminals[1].getJoint().getIndex();
        double ieq = g * prevVoltage;
        if(pI >= 0) {
            snapshot.stampMatrix(pI, pI, g);
            snapshot.stampRHS(pI, ieq);
        }
        if(nI >= 0) {
            snapshot.stampMatrix(nI, nI, g);
            snapshot.stampRHS(nI, ieq);
        }
        if(pI >= 0 && nI >= 0) {
            snapshot.stampMatrix(pI, nI, -g);
            snapshot.stampMatrix(nI, pI, -g);
        }
    }
}
