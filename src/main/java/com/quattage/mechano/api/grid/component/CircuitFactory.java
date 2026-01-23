package com.quattage.mechano.api.grid.component;

import java.util.Objects;
import java.util.Set;

import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.DiscreteComponent.NodeStub;
import com.quattage.mechano.api.grid.topology.landmark.BlockJack;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.Node.JointNode;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;
import com.quattage.mechano.api.grid.topology.landmark.WireJack;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;
import com.quattage.mechano.foundation.numeric.EsoMath;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.Direction;

public class CircuitFactory implements Disposable {

    private ObjectArrayList<DiscreteComponent> components = new ObjectArrayList<>();
    private Set<Node> nodes = new ObjectOpenHashSet<>();

    public CircuitFactory() {}

    public WireJackBuilder wireJack(String id) {
        assertNotConsumed();
        return new WireJackBuilder(id);
    }

    public BlockJackBuilder blockJack(String id) {
        assertNotConsumed();
        return new BlockJackBuilder(id);
    }

    public CircuitFactory solder(Node trace, Terminal pin) { return solder(pin, trace); }
    public CircuitFactory solder(Terminal pin, Node trace) {
        pin.updateOwnership(trace);
        trace.localAttach(pin);
        return this;
    }

    /**
     * Supply a new {@link DiscreteComponent} to this builder. This component may be soldered later in the 
     * builder chain. <pre>Resistor r1 = builder.supply(new Resistor())</pre>
     * The resistor must be {@link #solder soldered} at some point during the lifetime of this factory. 
     * If this CircuitFactory is {@link #make applied } before this component
     * receives any soldering, a {@link CircuitInstantiationException} will be thrown.
     * @param <T> Must extend {@link DiscreteComponent}.
     * @param component The {@link DiscreteComponent} instance that will be added. The instance should created uniquely for this method call.
     * @return The component that was added, so that a reference can be temporarily stored for later.
     * @throws NullPointerException if <code>component</code> is null
     * @throws IllegalArgumentException if <code>component</code> has already been added by a previous call
     */
    public <T extends DiscreteComponent> T supply(T component) {
        assertNotConsumed();
        if(component == null) throw new NullPointerException("Failed while adding new component to factory - The supplied component was null!");
        if(components.contains(component)) throw new IllegalArgumentException("Failed while adding new component to factory - This factory already contained the provided component!");
        this.components.add(component);
        return component;
    }

    /**
     * Supply a new {@link Node} to this builder. 
     * @param node The {@link Node} instance that will be added. The instance should created uniquely for this method call.
     * @return This CircuitFactory for chaining
     * @throws NullPointerException if <code>node</code> is null
     * @throws IllegalArgumentException if <code>node</code> has already been added by a previous call
     * @see #newNode
     */
    public CircuitFactory supply(Node node) {
        assertNotConsumed();
        if(node == null) throw new NullPointerException("Failed while adding node to factory - The supplied node was null!");
        if(!nodes.add(node)) 
            throw new IllegalArgumentException("Failed while including node in factory - This node is already in this factory!");
        return this;
    }

    /**
     * Creates a new {@link JointNode} and adds it to this CircuitFactory.
     * @return A new {@link Node} instance.
     * @see #supply(Node)
     */
    public Node newNode() {
        assertNotConsumed();
        Node j = new JointNode(null);
        supply(j);
        return j;
    }

    public Node newNode(String name) {
        assertNotConsumed();
        Node j = new JointNode(null, name);
        supply(j);
        return j;
    }

    /**
     * Consumes this CircuitFactory, turning it into a new Circuit instance.
     * Places the CircuitFactory in a state where it cannot be reused.
     * @return A new Circuit instance conforming to the attributes in this builder
     */
    public Circuit make(Griddable<?> source) {
        assertNotConsumed();
        if(components.size() <= 0 && nodes.size() <= 0) 
            throw new CircuitInstantiationException("This factory is empty!");
        Circuit circuit = new Circuit();
        consumeComponents(source, circuit);
        dispose();
        return circuit;
    }

    // flushes all components in this factory into the destination circuit
    private void consumeComponents(Griddable<?> source, Circuit circuit) {
        circuit.components = this.components;
        for(CircuitComponent component : circuit.components) {
            if(component == null) throw new CircuitInstantiationException("Encountered a null component!");
            if(component instanceof GridConstruct gc)
                gc.updateOwnership(source, circuit);
        }
        circuit.components.ensureCapacity(circuit.components.size() + this.nodes.size());
        for(Node node : nodes) {
            NodeStub ns = new NodeStub(node);
            circuit.components.add(ns);
            ns.updateOwnership(source, circuit);
        }
        circuit.owner = source;
        circuit.components.trim();
    }

    @Override
    public void dispose() {
        this.components = null;
        this.nodes = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return this.components == null;
    }

    private void assertNotConsumed() {
        if(components == null)
            throw new IllegalStateException("Attempted to use a CircuitFactory that has already been consumed!");
    }

    public static class WireJackBuilder {

        private final String id;
        private short x = Short.MIN_VALUE, y = Short.MIN_VALUE, z = Short.MIN_VALUE, s = (Short.MIN_VALUE + 37);
        private boolean isVisible = true;
        private Node attachmentTarget = null;

        public WireJackBuilder(String id) {
            this.id = id;
        }

        /**
         * Attach this WireJack to a node, allowing it to
         * receive external electrical forces from said node.
         * @param node
         * @return this builder for chaining
         */
        public WireJackBuilder attachedTo(Node node) {
            Objects.requireNonNull(node);
            this.attachmentTarget = node;
            return this;
        }
        
        /**
         * X offset of this WireJack
         * All offsets are relative to the lower corner of the block, between -16 and 32. 
         * These offsets are automatically rotated at runtime to match the parent block's state.
         * @param x
         * @return 
         */
        public WireJackBuilder x(float x) {
            this.x = this.toShort(x);
            return this;
        }

        /**
         * Y offset of this WireJack
         * All offsets are relative to the lower corner of the block, between -16 and 32. 
         * These offsets are automatically rotated at runtime to match the parent block's state.
         * @param x
         * @return 
         */
        public WireJackBuilder y(float y) {
            this.y = this.toShort(y);
            return this;
        }
        
        /**
         * Z offset of this WireJack
         * All offsets are relative to the lower corner of the block, between -16 and 32. 
         * These offsets are automatically rotated at runtime to match the parent block's state.
         * @param x
         * @return 
         */
        public WireJackBuilder z(float z) {
            this.z = this.toShort(z);
            return this;
        }

        /**
         * The physical size of this WireJack's hitbox. Defaults to 2.
         * @param s
         * @return
         */
        public WireJackBuilder size(float s) {
            this.s = this.toShort(s);
            return this;
        }

        /**
         * Ensures that this WireJack is disabled when instantiated,
         * which makes it invisible and uninteractable.
         * It can always be re-enabled by BE-sided logic later.
         * @return
         */
        public WireJackBuilder disabledByDefault() {
            this.isVisible = false;
            return this;    
        }

        public WireJack<?> make() {
            WireJack<?> newJack = new WireJack<>(id, isVisible, EsoMath.quadShort2Long(x, y, z, s));
            attachmentTarget.localAttach(null, newJack);
            return newJack;
        }

        private short toShort(float x) {
            float mapped = -32767 + (x + 16f) * (65535f / 48f);
            return (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mapped));
        }
    }


    public static class BlockJackBuilder {

        private final String id;
        private Relative rel = Relative.BOTTOM;
        private boolean isVisible = false;
        private Node attachmentTarget = null;

        private BlockJackBuilder(String id) {
            this.id = id;
        }

        /**
         * Attach this BlockJack to a node, allowing it to
         * receive external electrical forces from said node.
         * @param node
         * @return this builder for chaining
         */
        public BlockJackBuilder attachedTo(Node node) {
            Objects.requireNonNull(node);
            this.attachmentTarget = node;
            return this;
        }

        /**
         * The face indicated by this BlockJack. This face is reltaive to the parent block's 
         * default blockstate, and is rotated automatically.
         * @param rel {@link Relative} or {@link Direction}
         * @return this builder for chaining
         */
        public BlockJackBuilder face(Relative rel) {
            Objects.requireNonNull(rel);
            this.rel = rel;
            return this;
        }

        /**
         * The face indicated by this BlockJack. This face is reltaive to the parent block's 
         * default blockstate, and is rotated automatically.
         * @param rel {@link Relative} or {@link Direction}
         * @return this builder for chaining
         */
        public BlockJackBuilder face(Direction dir) {
            Objects.requireNonNull(dir);
            this.rel = Relative.of(dir);
            return this;
        }

        /**
         * BlockJacks are usually hidden by default due to their implicit nature, but they can
         * be manually made visible. This method makes it so that the BlockJack instantiated
         * by this builder will be visible by default, but does not prevent it from being hidden
         * again by external logic.
         */
        public BlockJackBuilder visibleByDefault() {
            this.isVisible = true;
            return this;    
        }

        public BlockJack make() {
            BlockJack newJack = new BlockJack(id, isVisible, new RelativeDirection(rel));
            attachmentTarget.localAttach(null, newJack);
            return newJack;
        }
    }

    private static class CircuitInstantiationException extends RuntimeException {
        public CircuitInstantiationException(String message) {
            super("Failed while running CircuitFactory - " + message);
        }
    }
}
