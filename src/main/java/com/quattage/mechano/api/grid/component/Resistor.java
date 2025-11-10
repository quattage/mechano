package com.quattage.mechano.api.grid.component;

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

public class Resistor extends FunctionalComponent implements Stamper {

    private final Terminal[] terminals;
    private final float ohms;

    public Resistor(float ohms) {
        super("Resistor");
        this.terminals = Terminal.pair(this);
        this.ohms = ohms;
    }

    @Override
	public Collection<Terminal> getTerminals() {
		return Arrays.asList(terminals);
	}

    @Override
    public void forEachJoint(Consumer<Node> cons) {
        terminals[0].forEachJoint(cons);
        terminals[1].forEachJoint(cons);
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }

    @Override
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        double g = 1d / (double)ohms;
        int aI = terminals[0].getJoint().getIndex();
        int bI = terminals[0].getJoint().getIndex();
        if(aI >= 0) snapshot.stampMatrix(aI, aI, g);
        if(bI >- 0) snapshot.stampMatrix(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            snapshot.stampMatrix(aI, bI, -g);
            snapshot.stampMatrix(bI, aI, -g);
        }
    }

    
}
