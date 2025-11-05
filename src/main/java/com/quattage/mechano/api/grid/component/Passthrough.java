package com.quattage.mechano.api.grid.component;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.BasicComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.Terminal;

/**
 * A component that does literally nothing
 */
public class Passthrough extends BasicComponent {
    public Passthrough() { super("Empty"); }
    @Override public Collection<Terminal> getTerminals() { return Collections.singleton(null); }
    @Override public void forEachJoint(Consumer<Node> cons) { return; }
    @Override public @Nullable CircuitComponent getParentComponent() { return null; }
    @Override public boolean isSignificant() { return false; }
}
