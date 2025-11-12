package com.quattage.mechano.api.grid.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.VoltageDecay;
import com.quattage.mechano.api.grid.solver.NodalSnapshot;
import com.quattage.mechano.api.grid.solver.NodalSnapshot.Stamper;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.Terminal;

public abstract class VoltageSource extends FunctionalComponent implements Stamper {

    private final Terminal[] terminals;
    private final VoltageDecay volts;
    private int cIndex = -1;

    public VoltageSource(String name, VoltageDecay decayFunction) {
        super(name);
        this.terminals = Terminal.polarPair(this);
        this.volts = decayFunction;
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

    public abstract double getStateOfCharge();

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }

    @Override
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        int pI = terminals[0].getJoint().getIndex();
        int nI = terminals[1].getJoint().getIndex();
        cIndex = snapshot.allocateSource();
        if(pI >= 0) {
            snapshot.stampMatrix(pI, cIndex, 1);
            snapshot.stampMatrix(cIndex, pI, 1);
        }
        if(nI >= 0) {
            snapshot.stampMatrix(nI, cIndex, -1);
            snapshot.stampMatrix(cIndex, nI, -1);
        }
        snapshot.stampRHS(cIndex, volts.apply(getStateOfCharge()));
    }
}
