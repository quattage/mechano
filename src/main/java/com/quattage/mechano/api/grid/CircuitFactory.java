package com.quattage.mechano.api.grid;

import java.util.Objects;
import java.util.Set;

import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.Node.GroundedJoint;
import com.quattage.mechano.api.grid.topology.Node.Joint;
import com.quattage.mechano.api.grid.topology.Terminal;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.grid.topology.ancillary.BlockJack;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;
import com.quattage.mechano.foundation.numeric.EsoMath;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.Direction;

public class CircuitFactory {

    private ObjectArrayList<FunctionalComponent> components = new ObjectArrayList<>();
    private Set<Terminal> terminals = new ObjectOpenHashSet<>();
    private GroundedJoint ground = new GroundedJoint(null);
    private Set<Node> preload = new ObjectOpenHashSet<>();

    public CircuitFactory() {}

    

    public WireJackBuilder wireJack(String id) {
        assertNotConsumed();
        return new WireJackBuilder(this, id);
    }

    public BlockJackBuilder blockJack(String id) {
        assertNotConsumed();
        return new BlockJackBuilder(this, id);
    }

    public CircuitFactory solder(Terminal a, Terminal b) {
        assertNotConsumed();
        if(!terminals.contains(a))
            throw new IllegalArgumentException("Failed while soldering terminals in factory - This factory doesn't contain " + a);
        if(!terminals.contains(b))
            throw new IllegalArgumentException("Failed while soldering terminals in factory - This factory doesn't contain " + b);
        
        return this;
    }

    /**
     * Supply a new CircuitComponent to this builder. This component may be soldered later in the builder chain.
     * <pre>Resistor r1 = builder.supply(new Resistor())</pre>
     * The resistor may be soldered later. Components that receive no soldering will be omitted from the final circuit when {@link #make() constructed}
     * @param component The {@link FunctionalComponent} instance that will be added. The instance should created uniquely for this method call.
     * @return The component that was added, so that a reference can be temporarily stored for later.
     * @throws NullPointerException if the provided <code>component</code> is null
     * @throws IllegalArgumentException if the provided <code>component</code> has already been added in a previous call
     */
    public <T extends FunctionalComponent> T supply(T component) {
        assertNotConsumed();
        if(component == null) throw new NullPointerException("Failed while adding new component to factory - The supplied component was null!");
        if(terminals.contains(component.getTerminals().toArray()[0]))
            throw new IllegalArgumentException("Failed while adding new component to factory - The supplied component '" + component + "' has already been added to this factory!");
        this.components.add(component);
        return component;
    }

    public CircuitFactory supply(Node node) {
        assertNotConsumed();
        if(node == null) return this;
        if(node.isGrounded()) 
            throw new IllegalArgumentException("Failed while including node in factory - Included nodes can't be grounded! (Try using the built-in grounded node instead.)");
        if(!preload.add(node)) 
            throw new IllegalArgumentException("Failed while including node in factory - This node is already in this factory!");
        return this;
    }

    public CircuitFactory ground(Terminal a) {
        assertNotConsumed();
        if(!terminals.contains(a))
            throw new IllegalArgumentException("Failed while grounding terminal in factory - This factory doesn't contain the terminal " + a);
        this.ground.attach(a);
        return this;
    }

    public CircuitFactory ground(AncillaryNode j) {
        assertNotConsumed();
        this.ground.attach(null, j);
        return this;
    }

    public Node newJoint() {
        assertNotConsumed();
        Node j = new Joint(null);
        supply(j);
        return j;
    }

    public Node ground() {
        assertNotConsumed();
        return ground();
    }

    /**
     * Consumes this CircuitFactory, turning it into a new CircuitComponent instance.
     * Places the CircuitFactory in a state where it cannot be reused.
     * @return A new CircuitComponent instance conforming to the attributes in this builder
     */
    public CircuitComponent make(Griddable<?>source) {
        assertNotConsumed();
        if(components.size() <= 0 && preload.size() <= 0 && !ground.isSignificant()) 
            throw new IllegalStateException("Attempted to create a CircuitComponent from a factory with no components or nodes!");
        Circuit c = new Circuit(source, ground, components, preload);
        components = null;
        terminals = null;
        preload = null;
        ground = null;
        return c;
    }

    private void assertNotConsumed() {
        if(components == null || terminals == null)
            throw new IllegalStateException("Attempted to use a CircuitFactory that has already been consumed!");
    }

    public static class WireJackBuilder {

        private final CircuitFactory prev;
        private final String id;
        private short x = Short.MIN_VALUE, y = Short.MIN_VALUE, z = Short.MIN_VALUE, s = (Short.MIN_VALUE + 37);
        private boolean isVisible = true;
        private Node attachmentTarget = null;

        public WireJackBuilder(CircuitFactory prev, String id) {
            this.prev = prev;
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

        public WireJack make() {
            WireJack newJack = new WireJack(id, isVisible, EsoMath.quadShort2Long(x, y, z, s));
            if(attachmentTarget == null) attachmentTarget = prev.ground;
            attachmentTarget.attach(null, newJack);
            return newJack;
        }

        private short toShort(float x) {
            float mapped = -32767 + (x + 16f) * (65535f / 48f);
            return (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mapped));
        }
    }


    public static class BlockJackBuilder {

        private final CircuitFactory prev;
        private final String id;
        private Relative rel = Relative.BOTTOM;
        private boolean isVisible = false;
        private Node attachmentTarget = null;

        private BlockJackBuilder(CircuitFactory prev, String id) {
            this.prev = prev;
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
            if(attachmentTarget == null) attachmentTarget = prev.ground;
            attachmentTarget.attach(null, newJack);
            return newJack;
        }
    }
}
