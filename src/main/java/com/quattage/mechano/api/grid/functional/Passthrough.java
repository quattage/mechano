package com.quattage.mechano.api.grid.functional;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

/**
 * A component that does literally nothing
 */
public class Passthrough extends FunctionalComponent {
    public Passthrough() { super("Empty"); }
    @Override public Collection<Terminal> getTerminals() { return Collections.emptyList(); }
    @Override public void forEachNode(Consumer<Node> cons) { return; }
    @Override public @Nullable CircuitComponent getParentComponent() { return null; }
    @Override public boolean isSignificant() { return false; }
}
