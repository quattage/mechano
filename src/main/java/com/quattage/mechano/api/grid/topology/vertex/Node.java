package com.quattage.mechano.api.grid.topology.vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridReferent;
import com.quattage.mechano.api.grid.GridReferent.SourceIdentifier;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.netlist.NodalCluster;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;

public interface Node extends CircuitComponent, SourceIdentifier, Disposable {

    /**
     * Gets the node that has the lower merge priority
     */
    static Node choosePrimary(Node a, Node b) {
        if(a == null && b == null) return null;
        if(a != null && b == null) return a;
        if(b != null && a == null) return b;
        int amp = a.isGrounded() ? -1 : a.getMergePriority();
        int bmp = b.isGrounded() ? -1 : b.getMergePriority();
        if(amp < bmp) return a;
        if(bmp < amp) return b;
        return a;
    }

    /**
     * Transfers the contents of <code>nodeB</code> onto
     * <code>nodeA</code> and disposes of <code>nodeB</code>
     * <p>
     * Subsequent access to a node that has been disposed of
     * will throw errors.
     * @param nodeA
     * @param nodeB
     * @return
     */
    static Node collapse(Node nodeA, Node nodeB) {
        if(nodeA == nodeB) return nodeA;
        for(Terminal term : nodeB.getTerminals()) {
            term.setConnectedTo(nodeA);
            if(term.getNode() == nodeB) continue;
            nodeA.getTerminals().add(term);
        }
        nodeB.dispose();
        return nodeA;
    }

    boolean localAttach(Terminal pin);
    boolean localAttach(@Nullable Griddable<?> source, AncillaryNode jack);
    boolean localDetach(Terminal pin);
    boolean localDetach(@Nullable Griddable<?> source, AncillaryNode jack);

    int getCircuitIndex();

    /**
     * @return The index that this node belongs to in its associated
     * {@link NodeUnionSet}. A return value <code><0</code> indicates
     * that this node is grounded.
     */
    int getNodalIndex();

    /**
     * To be called only by API elements, particularly the {@link NodeUnionSet}
     * when unioning this node for solving.
     * @param nodalIndex The index in the {@link NodeUnionSet} that this node belongs
     */
    @ApiStatus.Internal
    void setNodalIndex(int nodalIndex);

    List<AncillaryNode> getAncillaries();
    @Override List<Terminal> getTerminals();

    @Override
    void dispose();

    @Override
    default String getComponentID() {
        return "Node";
    }

    @Override 
    default boolean isSignificant() { 
        return hasTerminals() || hasAncillaries(); 
    }

    @Override default int size() { 
        Collection<Terminal> terminals = getTerminals();
        return terminals == null ? 0 : terminals.size();
    }

    default boolean hasTerminals() { 
        return size() > 0; 
    }

    default boolean hasAncillaries() {
        return getAncillaries() != null && getAncillaries().size() > 0;
    }

    default boolean has(Terminal terminal) {
        return indexOf(terminal) >= 0;
    }

    default boolean has(AncillaryNode ancillary) {
        return indexOf(ancillary) >= 0;
    }

    default int indexOf(AncillaryNode ancillary) {
        List<AncillaryNode> ancillaries = getAncillaries();
        if(ancillaries == null || ancillaries.isEmpty())
            return -1;
        return ancillaries.indexOf(ancillary);
    }

    default int indexOf(Terminal terminal) {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return -1;
        return terminals.indexOf(terminal);
    }

    @Override
    default void saturate() {
        forEachTerminal(Terminal::saturate);
    }

    @Override
    default void reset() {
        forEachTerminal(Terminal::reset);
    }

    default void forEachTerminal(Consumer<Terminal> cons) {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return;
        for(Terminal t : terminals)
            cons.accept(t);
    }

    @Override
    default boolean isGrounded() {
        return false;
    }

    @Override
    default void forEachNode(Consumer<Node> cons) {
        cons.accept(this);
    }

    @Override
    default GridUUID bindUUID(GridUUID id) {
        return id.withBinding(getReferentType(), getCircuitIndex());
    }
    
    @Override
    default GridReferent getReferentType() {
        return GridReferent.EMITTER_NODE;
    }

    // dear god
    default String toFullString(@Nullable ServerGrid grid) {
        String out = "    ▸ " + getComponentID() + " " + getNodalIndex() + ", " + (isGrounded() ? "Grounded" : "Ungrounded");
        if(hasAncillaries()) {
            Griddable<?> src = SourceIdentifier.getSourceFor(this);
            BlockPos bp = src.getBlockPos();
            out += "\n\t  Owned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
            out += "\n\t  " + getAncillaries().size() + " Ancillaries: [";
            for(AncillaryNode anc : getAncillaries())
                out += anc.getComponentID() + ", ";
            out = out.substring(0, out.length() - 2) + "]";
        } else out += "\n\t  0 Ancillaries: [Empty]";
        if(hasTerminals()) {
            out += "\n\t  " + getTerminals().size() + " Terminals: [";
            for(Terminal term : getTerminals())
                out += "\n\t\t" + term.toString();
            out += "]";
        } else out += "\n\t  0 Terminals: [Empty]";
        if(grid == null) return out;
        Node[] children = NodalCluster.getConstituents(grid.getNetlist(), this);
        if(children == null) return out = "\n\t  Non-root, no children.";
        out += "\n\t  " + children.length + " child" + (children.length == 1 ? ":" : "ren:");
        for(Node node : children) {
            Griddable<?> source = SourceIdentifier.getSourceFor(node);
            BlockPos bp = source == null ? null : source.getBlockPos();
            out += "\n\t\t  ↪ " + node.getComponentID() + " " + node.getNodalIndex() + " (" + (source == null ? "no source" : source.getClass().getSimpleName() + " [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]") + ")";
        }
        final Set<AncillaryPair> collectedLinks = collectAttachedLinks(grid);
        if(collectedLinks.isEmpty())
            return out + "\n\t  0 Links [Empty]";
        out += "\n\t  " + collectedLinks.size() + " links:";
        for(AncillaryPair link : collectedLinks) {
            Griddable<?> linkOwner = SourceIdentifier.getSourceFor(link.getStartNode());
            BlockPos bp = linkOwner.getBlockPos();
            String linkName = link instanceof ComponentLink cl ? cl.getComponentID() : "anonymous";
            out += "\n\t\t  ☍ " + linkName + " ┳ from " + linkOwner.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
            linkOwner =  SourceIdentifier.getSourceFor(link.getEndNode());
            bp = linkOwner.getBlockPos();
            out += "\n\t\t    " + whitespace(linkName) + " ┗  to  "  + linkOwner.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
        }
        return out;
    }

    private String whitespace(String original) {
        String out = "";
        for(int x = 0; x < original.length(); x++) out += " ";
        return out;
    }

    default Set<AncillaryPair> collectAttachedLinks(ServerGrid grid) {
        final Set<AncillaryPair> output = new HashSet<>();
        Node[] children = NodalCluster.getConstituents(grid.getNetlist(), this);
        this.addAncillaryLinks(grid, output);
        for(Node node : children)
            node.addAncillaryLinks(grid, output);
        return output;
    }

    private void addAncillaryLinks(ServerGrid grid, Set<AncillaryPair> toModify) {
        for(AncillaryNode ancillary : getAncillaries()) {
            List<AncillaryPair> linksAt = grid.getLinksBelongingTo(grid.getAddressFor(ancillary.getSource(), ancillary));
            if(linksAt == null || linksAt.isEmpty()) continue;
            toModify.addAll(linksAt);
        }
    }

    default double getVoltage(ServerGrid grid) {
        int idx = getNodalIndex();
        if(idx < 0) return 0;
        DMatrixRMaj solution = grid.getSolution();
        if(solution == null || solution.numRows <= 0) return 0;
        if(idx >= solution.numRows) return 0;
        return solution.get(idx, 0);
    }

    default int getMergePriority() {
        CircuitComponent component = getParentComponent();
        if(component == null) return 2;
        return component.getReferentType().getMergePriority();
    }

    public static class JointNode implements Node {

        private CircuitComponent parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode> ancillaries = null;
        private int circuitIndex, nodalIndex;

        public JointNode(CircuitComponent parent) {
            this.parent = parent;
            this.circuitIndex = -1;
            this.nodalIndex = -1;
        }

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this, 0);
            return true;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode jack) {
			boolean removed = ancillaries.remove(jack);
            if(ancillaries.isEmpty()) ancillaries = null;
            if(removed) jack.updateOwnership(null, null, -1);
            return removed;
		}

        @Override
        public Griddable<?> getSource() {
            if(!hasAncillaries()) return null;
            for(AncillaryNode node : getAncillaries()) {
                if(node == null) continue;
                Griddable<?> source = node.getSource();
                if(source == null) continue;
                return source;
            }
            return null;
        }

        @Override
        public int getCircuitIndex() {
            return circuitIndex;
        }

        @Override
        public int getNodalIndex() {
            if(nodalIndex < 0 && !isGrounded()) {
                Mechano.LOGGER.warn("nodal index for " + this + " returned invalid value (" 
                    + nodalIndex + ") - Perhaps the matrix hasn't been initialized?");
            }
            return nodalIndex;
        }

        @Override
        public void setNodalIndex(int nodalIndex) {
            this.nodalIndex = nodalIndex;
        }

        @Override
        public List<AncillaryNode> getAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return parent;
        }

        @Override
        public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) {
            CircuitComponent.assertValidOwnership(this, parent);
            this.parent = parent;
            this.circuitIndex = index;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryNode jack : ancillaries)
                jack.updateOwnership(source, this, 0);
        }

        @Override
        public void dispose() {
            this.parent = null;
            this.circuitIndex = -1;
            this.terminals = null;
        }

        @Override
        public String toString() {
            return getComponentID();
        }
    }

    public static class GroundNode implements Node {

        protected @Nullable ObjectArrayList<Terminal> terminals;

        public GroundNode() {}

        @Override
        public String getComponentID() {
            return "GroundNode";
        }

        @Override
        public GridUUID bindUUID(GridUUID id) {
            return id.withBinding(getReferentType(), -1);
        }

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode jack) {
			return false;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode jack) {
			return false;
		}

        @Override
        public @Nullable Griddable<?> getSource() {
            return null;
        }

        @Override
        public int getCircuitIndex() {
            return -1;
        }
        
        @Override
        public int getNodalIndex() {
            return -1;
        }

        @Override
        public void setNodalIndex(int nodalIndex) {
            return;
        }

        @Override
        public List<AncillaryNode> getAncillaries() {
            return Collections.emptyList();
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return null;
        }

        @Override
        public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) {
            return;
        }

        @Override
        public void dispose() {}

        @Override
        public String toString() {
            return getComponentID();
        }
    }
}


