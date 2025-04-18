package com.quattage.mechano.foundation.block.hitbox;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape.DefaultModelType;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class HitboxCache {

    private FileToIdConverter LISTER = FileToIdConverter.json("models");
    private List<UnbuiltHitbox<? extends StringRepresentable>> unbuiltHitboxes = new ObjectArrayList<>();
    private final Map<ResourceLocation, RotatableHitboxShape<?>> hitboxes = new HashMap<>();;
    private boolean isLoaded = false;

    /**
     * Marks this HitboxCache as loaded, 
     */
    protected void finishLoading() {
        unbuiltHitboxes.clear();
        unbuiltHitboxes = null;
        isLoaded = true;
        LISTER = null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable>
        RotatableHitboxShape<R> get(Block block, @Nullable T subState, EnumProperty<R> orientation) {

        RotatableHitboxShape<?> check = null;
        if(subState != null) {
            check = hitboxes.getOrDefault(
                BuiltInRegistries.BLOCK.getKey(block).withSuffix("/" + subState.getHitboxName()), 
                RotatableHitboxShape.ofMissingResource(orientation)
            );
        } else {
            check = hitboxes.getOrDefault(
                BuiltInRegistries.BLOCK.getKey(block).withSuffix(DefaultModelType.DEFAULT.getHitboxName()),
                RotatableHitboxShape.ofMissingResource(orientation)
            );
        }

        return (RotatableHitboxShape<R>)check;
    }

    @SuppressWarnings("unchecked")
    public<T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        Hitbox<R> collectAllOfType(Block block) {
        final ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        final Map<String, RotatableHitboxShape<R>> collected = new HashMap<>();
        for(Map.Entry<ResourceLocation, RotatableHitboxShape<?>> entry : hitboxes.entrySet()) {
            if(!entry.getKey().getNamespace().equals(loc.getNamespace())) continue;
            if(entry.getKey().getPath().contains(loc.getPath())) {
                collected.put(entry.getValue().getTypeName(), (RotatableHitboxShape<R>)entry.getValue());
            }
        }

        return new Hitbox<R>(collected);
    }

    /***
     * Looks for JSON files containing model data to be parsed into
     * VoxelShapes at load-time. The name of the hitbox to use for each property is determined
     * by the string returned by HitboxNameable in your Block class.
     * @param typeStates (optional) The EnumProperty for this block that represents its type - This is just an auxilury state
     * which allows arbitrary hitbox variants to be loaded for blocks that change shape
     * @param orientStates The EnumProperty for this block that represents its orientation
     * @return The Operator to be called upon by Registrate while processing transforms
     */
    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> find(String sub, EnumProperty<R> orientStates) {
        return b -> {
            if(isLoaded) {
                Mechano.LOGGER.error("Can't register hitbox for '" + b.getName() + "' - Attempted to register outside of permissible loading context!");
                return b;
            }
            UnbuiltHitbox<R> key = 
                new UnbuiltHitbox<R>(
                    DefaultModelType.DEFAULT.getHitboxName(),
                    ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), 
                    LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( 
                        b.getOwner().getModid(), b.getName()).withPrefix("block" + (sub != null ? sub + "/" : "/")).withSuffix("/hitbox/" + 
                            DefaultModelType.DEFAULT.getHitboxName())),
                    orientStates
                );
            unbuiltHitboxes.add(key);
            return b;
        };
    }

    /***
     * Looks for JSON files containing model data to be parsed into
     * VoxelShapes at load-time. The name of the hitbox to use for each property is determined
     * by the string returned by HitboxNameable in your Block class.
     * @param typeStates (optional) The EnumProperty for this block that represents its type - This is just an auxilury state
     * which allows arbitrary hitbox variants to be loaded for blocks that change shape
     * @param orientStates The EnumProperty for this block that represents its orientation
     * @return The Operator to be called upon by Registrate while processing transforms
     */
    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> findType(String sub, EnumProperty<T> typeStates, EnumProperty<R> orientStates) {
        return b -> {
            if(isLoaded) {
                Mechano.LOGGER.error("Can't register hitbox for '" + b.getName() + "' - Attempted to register outside of permissible loading context!");
                return b;
            }
            for(T property : typeStates.getPossibleValues()) {
                UnbuiltHitbox<R> key = 
                    new UnbuiltHitbox<R>(
                        property.getHitboxName(),
                        ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()),
                        LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( // ex. block/stator/hitbox/hitbox.json
                            b.getOwner().getModid(), b.getName()).withPrefix("block/"  + (sub != null ? sub + "/" : "/")).withSuffix("/hitbox/" + 
                                property.getHitboxName())),
                        orientStates
                    );
                unbuiltHitboxes.add(key);
            }
            return b;
        };
    }
    

    /***
     * Looks for JSON files containing model data to be parsed into
     * VoxelShapes at load-time. The name of the hitbox to use for each property is determined
     * by the string returned by HitboxNameable in your Block class.
     * Assumes, by default, that the model data .JSON is named the same as the registry object
     * and is located durectly in models/block. A sub-directory can be provided to search deeper.
     * @param sub (Optional) A sub-directory to search within.
     * @param orientStates The EnumProperty for this block that represents its orientation
     * @return The Operator to be called upon by Registrate while processing transforms
     */
    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> findSelf(EnumProperty<R> orientStates) {
        return b -> {
            if(isLoaded) {
                Mechano.LOGGER.error("Can't register hitbox for '" + b.getName() + "' - Attempted to register outside of permissible loading context!");
                return b;
            }
            UnbuiltHitbox<R> key = 
                new UnbuiltHitbox<R>(
                    DefaultModelType.DEFAULT.getHitboxName(),
                    ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), 
                    LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( 
                        b.getOwner().getModid(), b.getName()).withPrefix("block/")),
                    orientStates
                );
            unbuiltHitboxes.add(key);
            return b;
        };
    }

    /***
     * Looks for JSON files containing model data to be parsed into
     * VoxelShapes at load-time.
     * Assumes, by default, that the model file is named the same as the registry object
     * and is located durectly in models/block. A sub-directory can be provided to search deeper.
     * @param sub (Optional) A sub-directory to search within.
     * @param orientStates The EnumProperty for this block that represents its orientation
     * @return The Operator to be called upon by Registrate while processing transforms
     */
    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> findSelf(String sub, EnumProperty<R> orientStates) {
        return b -> {
            if(isLoaded) {
                Mechano.LOGGER.error("Can't register hitbox for '" + b.getName() + "' - Attempted to register outside of permissible loading context!");
                return b;
            }
            UnbuiltHitbox<R> key = 
                new UnbuiltHitbox<R>(
                    DefaultModelType.DEFAULT.getHitboxName(),
                    ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), 
                    LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( 
                        b.getOwner().getModid(), b.getName()).withPrefix("block/" + sub + "/")),
                    orientStates
                );
            unbuiltHitboxes.add(key);
            return b;
        };
    }

    /***
     * Looks for JSON files containing model data to be parsed into
     * VoxelShapes at load-time. Provides no additional automation for
     * locating the resource - you provide the exact directory and file name.
     * @param dir The exact directory of the JSON model file.
     * @param orientStates The EnumProperty for this block that represents its orientation
     * @return The Operator to be called upon by Registrate while processing transforms
     */
    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> findExact(String dir, EnumProperty<R> orientStates) {
        return b -> {
            if(isLoaded) {
                Mechano.LOGGER.error("Can't register hitbox for '" + b.getName() + "' - Attempted to register outside of permissible loading context!");
                return b;
            }
            UnbuiltHitbox<R> key = 
                new UnbuiltHitbox<R>(
                    DefaultModelType.DEFAULT.getHitboxName(),
                    ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), 
                    LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( 
                        b.getOwner().getModid(), "block/" + dir)),
                    orientStates
                );
            unbuiltHitboxes.add(key);
            return b;
        };
    }

    /***
     * @return A set containing all hitboxes that haven't been built yet - 
     */
    protected List<UnbuiltHitbox<? extends StringRepresentable>> getAllUnbuilt() {
        return unbuiltHitboxes;
    }

    /**
     * Turns the given UnbuiltHitbox into an actual hitbox
     * @param <T> 
     * @param unbuilt
     * @param boxes
     */
    protected <T extends Enum<T> & StringRepresentable> void putNew(UnbuiltHitbox<T> unbuilt, List<List<Float>> boxes) {
        hitboxes.put(unbuilt.getSerializedLocation(), new RotatableHitboxShape<T>(unbuilt.getType(), unbuilt.getOrientStates(), boxes));
    }

    protected class UnbuiltHitbox<R extends Enum<R> & StringRepresentable> {

        final String hitboxType;
        final ResourceLocation loc;
        final ResourceLocation path;
        final EnumProperty<R> orientStates;
        
        protected UnbuiltHitbox(String hitboxType, ResourceLocation loc, ResourceLocation path, EnumProperty<R> orientStates) {
            this.hitboxType = hitboxType;
            this.loc = loc;
            this.path = path;
            this.orientStates = orientStates;
        }

        public EnumProperty<R> getOrientStates() {
            return orientStates;
        }

        protected String getType() {
            return hitboxType;
        }

        protected ResourceLocation getSerializedLocation() {
            return loc.withSuffix("/" + hitboxType);
        }

        protected ResourceLocation getResourceLocation() {
            return path;
        }

        protected String getRawPath() {
            return "assets/" + path.getNamespace() + "/" + path.getPath();
        }

        public int hashCode() {
            return path.hashCode();
        }

        public String toString() {
            return loc + " (" + path + ")";
        }

        public boolean equals(Object other) {
            if(!(other instanceof HitboxCache.UnbuiltHitbox key)) return false;
            return this.path.equals(key.path);
        }
    }
}
