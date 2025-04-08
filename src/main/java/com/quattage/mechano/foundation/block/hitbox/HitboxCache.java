package com.quattage.mechano.foundation.block.hitbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape.DefaultModelType;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class HitboxCache {

    private static final FileToIdConverter LISTER = FileToIdConverter.json("models");
    private Set<UnbuiltHitbox<? extends StringRepresentable>> unbuiltCache = new HashSet<>();
    private final Map<ResourceLocation, RotatableHitboxShape<?>> hitboxes = new HashMap<>();;

    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable>
        RotatableHitboxShape<R> get(EnumProperty<R> group, @Nullable T type, Block block) {

        RotatableHitboxShape<?> check = null;
        if(type != null) {
            check = hitboxes.getOrDefault(
                BuiltInRegistries.BLOCK.getKey(block).withSuffix("/" + type.getHitboxName()), 
                RotatableHitboxShape.ofMissingResource(group)
            );
        } else {
            check = hitboxes.getOrDefault(
                BuiltInRegistries.BLOCK.getKey(block).withSuffix(DefaultModelType.DEFAULT.getHitboxName()),
                RotatableHitboxShape.ofMissingResource(group)
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


    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(String sub, @Nullable EnumProperty<T> typeStates, EnumProperty<R> orientStates) {
        if(typeStates == null) {
            return b -> {
                UnbuiltHitbox<R> key = 
                    new UnbuiltHitbox<R>(
                        DefaultModelType.DEFAULT.getHitboxName(),
                        ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), 
                        LISTER.idToFile(ResourceLocation.fromNamespaceAndPath( 
                            b.getOwner().getModid(), b.getName()).withPrefix("block" + (sub != null ? sub + "/" : "/")).withSuffix("/hitbox/" + 
                                DefaultModelType.DEFAULT.getHitboxName())),
                        orientStates
                    );

                if(!unbuiltCache.contains(key)) {
                    unbuiltCache.add(key);
                }

                return b;
            };
        }

        return b -> {
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
                if(unbuiltCache.contains(key)) continue;
                unbuiltCache.add(key);
            }
            
            return b;
        };
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(EnumProperty<T> typeStates, EnumProperty<R> orientStates) {
        return flag(null, typeStates, orientStates);
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(String sub, EnumProperty<R> orientStates) {
        return flag(sub, null, orientStates);
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(EnumProperty<R> orientStates) {
        return flag(null, null, orientStates);
    }

    protected Set<UnbuiltHitbox<? extends StringRepresentable>> getAllUnbuilt() {
        return unbuiltCache;
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
