package com.quattage.mechano.foundation.api;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.LinkDataStorable.Client;
import com.quattage.mechano.foundation.api.LinkDataStorable.ClientSectionable;
import com.quattage.mechano.foundation.api.LinkDataStorable.Server;
import com.quattage.mechano.foundation.api.LinkDataStorable.ServerSectionable;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.GridConnection.InsertionPolicy;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * A data store containing {@link GridConnection} instances registered as a data attachment
 * ensuring that grid data is only one hash lookup away in most contexts, expecially for 
 * rendering on the client. LevelChunk, Entity, and BlockEntity attachment holders are 
 * explicitly supported. Attempts to push link data to any other IAttachmentHolder type
 * will result in thrown exceptions.
 */
public sealed interface LinkDataStorable<T extends GridConnection> permits Client, Server, ClientSectionable, ServerSectionable {

    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    public static <T extends GridConnection> LinkDataStorable<T> make(IAttachmentHolder holder) {
        if(holder instanceof LevelChunk chunk) {
            if(chunk.getLevel().isClientSide())
                return (LinkDataStorable<T>) new ClientSectionable();
            return (LinkDataStorable<T>) new ServerSectionable();
        }
        if(holder instanceof Entity entity) {
            if(entity.level().isClientSide())
                return (LinkDataStorable<T>) new Client();
            return (LinkDataStorable<T>) new Server();
        }
        if(holder instanceof BlockEntity be) {
            if(be.getLevel().isClientSide())
                return (LinkDataStorable<T>) new Client();
            return (LinkDataStorable<T>) new Server();
        }
        throwBadHolderType(holder);
        return null;
    }

    public static @Nullable LinkDataStorable<?> getUnsided(IAttachmentHolder holder, boolean force) {
        Objects.requireNonNull(holder);
        LinkDataStorable<?> data = holder.getData(MechanoData.LINK_ATTACHMENT);
        if(data == null) return null;
        if(force) return data;
        if(wipeIfEmpty(data, holder)) return null;
        return data;
    }

    public static @Nullable ServerSectionable getAsServer(LevelReader world, ChunkPos pos, boolean force) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(pos);
        return getAsServer(world.getChunk(pos.x, pos.z), force);
    }

    public static @Nullable ServerSectionable getAsServer(LevelChunk chunk, boolean force) {
        return getAsServer((ChunkAccess)chunk, force);
    }

    public static @Nullable Server getAsServer(BlockEntity be, boolean force) {
        Objects.requireNonNull(be);
        assertSided(be, false);
        LinkDataStorable<?> data = getUnsided(be, force);
        return (Server)data;
    }

    public static @Nullable ServerSectionable getAsServer(ChunkAccess chunk, boolean force) {
        Objects.requireNonNull(chunk);
        assertSided(chunk, false);
        LinkDataStorable<?> data = getUnsided(chunk, force);
        return (ServerSectionable)data;
    }

    public static @Nullable Server getAsServer(Entity e, boolean force) {
        Objects.requireNonNull(e);
        assertSided(e, false);
        LinkDataStorable<?> data = getUnsided(e, force);
        return (Server)data;
    }

    public static @Nullable GridLink getAsServer(LevelReader world, ConnectionKey key) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(key);
        assertSided(world, false);
        IAttachmentHolder holder = key.getStart().getDataStorageHolder(world);
        if(holder instanceof LevelChunk chunk) {
            ServerSectionable data = getAsServer(chunk, false);
            return data == null ? null : data.get(world, key);
        }
        if(holder instanceof Entity e) {
            Server data = getAsServer(e, false);
            return data == null ? null : data.get(world, key);
        }
        if(holder instanceof BlockEntity be) {
            Server data = getAsServer(be, false);
            return data == null ? null : data.get(world, key);
        }
        throwBadHolderType(holder);
        return null;
    }

    public static @Nullable GridLink popAsServer(LevelReader world, GridConnection key) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(key);
        assertSided(world, false);

        GridLink removed = null;
        IAttachmentHolder holder = key.getEnd().getDataStorageHolder(world);

        if(holder instanceof LevelChunk chunk) {
            ServerSectionable data = getAsServer(chunk, false);
            if(data != null) removed = data.pop(world, key);
        } else if(holder instanceof Entity e) {
            Server data = getAsServer(e, false);
            if(data != null) removed = data.pop(world, key);
        } else if (holder instanceof BlockEntity be) {
            Server data = getAsServer(be, false);
            if(data != null) removed = data.pop(world, key);
        } else if(holder != null) throwBadHolderType(holder);

        // we don't need to make an inverse copy of the key here since the equals and 
        // hashcode implementations for GridConnection are logically symmetrical
        holder = key.getStart().getDataStorageHolder(world);
        if(holder instanceof LevelChunk chunk) {
            ServerSectionable data = getAsServer(chunk, false);
            if(data != null) data.pop(world, key);
        } else if(holder instanceof Entity e) {
            Server data = getAsServer(e, false);
            if(data != null) data.pop(world, key);
        } else if(holder instanceof BlockEntity be) {
            Server data = getAsServer(be, false);
            if(data != null) data.pop(world, key);
        } else if(holder != null) throwBadHolderType(holder);
        return removed;
    }

    private static boolean pushAsServer(LevelReader world, GridLink link, InsertionPolicy mode) {
        switch(mode) {
            case SINGLE -> {
                IAttachmentHolder holder = link.getStart().getDataStorageHolder(world);
                if(holder instanceof LevelChunk chunk) {
                    ServerSectionable data = getAsServer(chunk, true);
                    return data.add(world, link);
                }
                if(holder instanceof Entity e) {
                    Server data = getAsServer(e, true);
                    return data.add(world, link);
                }
                if(holder instanceof BlockEntity be) {
                    Server data = getAsServer(be, true);
                    return data.add(world, link);
                } 
                throwBadHolderType(holder);
            }
            case SYMMETRIC -> {
                pushAsServer(world, link, InsertionPolicy.SINGLE);
                return pushAsServer(world, link.inverseCopy(), InsertionPolicy.SINGLE);
            }
        }
        return false;
    }

    public static @Nullable ClientSectionable getAsClient(LevelReader world, ChunkPos pos, boolean force) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(pos);
        return getAsClient(world.getChunk(pos.x, pos.z), force);
    }

    public static @Nullable ClientSectionable getAsClient(LevelChunk chunk, boolean force) {
        return getAsClient((ChunkAccess)chunk, force);
    }

    public static @Nullable ClientSectionable getAsClient(ChunkAccess chunk, boolean force) {
        Objects.requireNonNull(chunk);
        assertSided(chunk, true);
        LinkDataStorable<?> data = getUnsided(chunk, force);
        return (ClientSectionable)data;
    }

    public static @Nullable Client getAsClient(Entity e, boolean force) {
        Objects.requireNonNull(e);
        assertSided(e, true);
        LinkDataStorable<?> data = getUnsided(e, force);
        return (Client)data;
    }

    public static @Nullable Client getAsClient(BlockEntity e, boolean force) {
        Objects.requireNonNull(e);
        assertSided(e, true);
        LinkDataStorable<?> data = getUnsided(e, force);
        return (Client)data;
    }

    public static @Nullable GridCatenary getAsClient(LevelReader world, GridConnection key) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(key);
        assertSided(world, true);

        IAttachmentHolder holder = key.getStart().getDataStorageHolder(world);
        if(holder instanceof LevelChunk chunk) {
            ClientSectionable data = getAsClient(chunk, false);
            return data == null ? null : data.get(world, key);
        }
        if(holder instanceof Entity e) {
            Client data = getAsClient(e, false);
            return data == null ? null : data.get(world, key);
        }
        if(holder instanceof BlockEntity be) {
            Client data = getAsClient(be, false);
            return data == null ? null : data.get(world, key);
        }
        throwBadHolderType(holder);
        return null;
    }

    public static @Nullable GridCatenary popAsClient(LevelReader world, GridConnection key) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(key);
        assertSided(world, true);

        GridCatenary removed = null;
        IAttachmentHolder holder = key.getStart().getDataStorageHolder(world);

        if(holder instanceof LevelChunk chunk) {
            ClientSectionable data = getAsClient(chunk, false);
            if(data != null) removed = data.pop(world, key);
        } else if(holder instanceof Entity e) {
            Client data = getAsClient(e, false);
            if(data != null) removed = data.pop(world, key);
        } else if(holder instanceof BlockEntity be) {
            Client data = getAsClient(be, false);
            if(data != null) removed = data.pop(world, key);
        } else if(holder != null) throwBadHolderType(holder);

        // we don't need to make an inverse copy of the key here since the equals and 
        // hashcode implementations for GridConnection are logically symmetrical
        holder = key.getEnd().getDataStorageHolder(world);
        if(holder instanceof LevelChunk chunk) {
            ClientSectionable data = getAsClient(chunk, false);
            if(data != null) data.pop(world, key);
        } else if(holder instanceof Entity e) {
            Client data = getAsClient(e, false);
            if(data != null) data.pop(world, key);
        }else if(holder instanceof BlockEntity be) {
            Client data = getAsClient(be, false);
            if(data != null) data.pop(world, key);
        } else if(holder != null) throwBadHolderType(holder);
        return removed;
    }

    private static boolean pushAsClient(LevelReader world, GridCatenary cat, InsertionPolicy mode) {
        switch(mode) {
            case SINGLE -> {
                IAttachmentHolder holder = cat.getStart().getDataStorageHolder(world);
                if(holder instanceof LevelChunk chunk) {
                    ClientSectionable data = getAsClient(chunk, true);
                    return data.add(world, cat);
                }
                if(holder instanceof Entity e) {
                    Client data = getAsClient(e, true);
                    return data.add(world, cat);
                } 
                if(holder instanceof BlockEntity be) {
                    Client data = getAsClient(be, true);
                    return data.add(world, cat);
                } 
                throwBadHolderType(holder);
            }
            case SYMMETRIC -> {
                pushAsClient(world, cat, InsertionPolicy.SINGLE);
                return pushAsClient(world, cat.inverseCopy(), InsertionPolicy.SINGLE);
            }
        }
        return false;
    }

    public static boolean put(LevelReader world, GridConnection connection) {
        return put(world, connection, InsertionPolicy.SYMMETRIC);
    }

    public static boolean put(LevelReader world, GridConnection connection, InsertionPolicy mode) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(connection);
        if(connection instanceof ConnectionKey key) {
            throw new IllegalArgumentException("Failed while pushing " + key 
                + " to data storage in " + world + " - ConnectionKeys can't be pushed!");
        }
        if(world.isClientSide() && connection.isClientSide() && connection instanceof GridCatenary cat)
            return pushAsClient(world, cat, mode);
        if(!world.isClientSide() && !connection.isClientSide() && connection instanceof GridLink link)
            return pushAsServer(world, link, mode);
        throw new IllegalArgumentException("Failed while pushing " + connection + " due to a " 
            + (world.isClientSide() ? "client" : "server") + "-sided mismatch!");
    }

    public static boolean remove(LevelReader world, GridConnection key) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(key);
        return world.isClientSide() ? popAsClient(world, key) != null : popAsServer(world, key) != null;
    }


    /**
     * if a data attachment is empty when it is retrieved, treat it as if it doesn't exist
     * and immediately remove the one that was lazily created in the getters above.
     */
    private static boolean wipeIfEmpty(LinkDataStorable<?> data, IAttachmentHolder holder) {
        if(!data.isEmpty()) return false;
        holder.removeData(MechanoData.LINK_ATTACHMENT);
        return true;
    }

    private static void assertSided(ChunkAccess chunk, boolean clientSided) { assertSided(chunk.getLevel(), clientSided); }
    private static void assertSided(Entity e, boolean clientSided) { assertSided(e.level(), clientSided); }
    private static void assertSided(BlockEntity be, boolean clientSided) { assertSided(be.getLevel(), clientSided); }
    private static void assertSided(LevelReader world, boolean clientSided) {
        if(world.isClientSide() == clientSided) return;
        throw new IllegalStateException("Sided mismatch encountered while retrieving data store from '" + world + "' - The world passed " + 
            (world.isClientSide() ? " was client-sided" : " was server-sided") + ", but the " + (clientSided ? "client-sided" : "server-sided") + " version of the getter was used!");
    }

    private static void throwBadHolderType(IAttachmentHolder holder) {
        throw new IllegalArgumentException("Invalid AttachmentHolder type - Expected LevelChunk, BlockEntity, or Entity, got '" 
            + holder.getClass().getSimpleName() + "' instead!");
    }

    public abstract boolean add(LevelReader world, T connection);
    public abstract @Nullable T get(LevelReader world, GridConnection key);
    public abstract @Nullable T pop(LevelReader world, GridConnection key);
    public abstract boolean isEmpty();
    public abstract void forEach(Consumer<T> action);
    public abstract boolean isClientSide();
    public abstract ObjectSet<T> getAll();
    
    public default void assertSided(LevelReader world) {
        if(world.isClientSide() == isClientSide()) return;
        throw new IllegalStateException("Sided mismatch encountered for link data of type '" + this.getClass().getSimpleName() + "' - The world passed " + 
            (world.isClientSide() ? " was client-sided" : " was server-sided") + ", but this data store is only available on the " + (isClientSide() ? "client!" : "server!"));
    }

    public static final class Client implements LinkDataStorable<GridCatenary> {

        private final ObjectOpenHashSet<GridCatenary> contents = new ObjectOpenHashSet<>(2);

        @Override
        public boolean add(LevelReader world, GridCatenary connection) {
            assertSided(world);
            return contents.add(connection);
        }

        @Override
        public @Nullable GridCatenary get(LevelReader world, GridConnection key) {
            assertSided(world);
            return contents.get(key);
        }

        @Override
        public @Nullable GridCatenary pop(LevelReader world, GridConnection key) {
            assertSided(world);
            GridCatenary cat = contents.get(key);
            if(key == null) return null;
            contents.remove(key);
            return cat;
        }

        @Override
        public void forEach(Consumer<GridCatenary> action) {
            for(GridCatenary cat : contents) {
                if(cat == null) continue;
                action.accept(cat);
            }
        }

        @Override public boolean isEmpty() { return contents.isEmpty(); }
        @Override public boolean isClientSide() { return true; }
        @Override public ObjectSet<GridCatenary> getAll() { return contents; }
    }

    public static final class ClientSectionable implements LinkDataStorable<GridCatenary> {

        private final Int2ObjectOpenHashMap<Client> contents = new Int2ObjectOpenHashMap<>(2);

        @Override
        public boolean add(LevelReader world, GridCatenary connection) {
            assertSided(world);
            int sectionY = connection.getSectionY(world);
            Client sectionData = contents.get(sectionY);
            if(sectionData == null) {
                sectionData = new Client();
                sectionData.contents.add(connection);
                contents.put(sectionY, sectionData);
                return true;
            }
            return sectionData.contents.add(connection);
        }

        @Override
        public @Nullable GridCatenary get(LevelReader world, GridConnection key) {
            assertSided(world);
            int sectionY = key.getSectionY(world);
            Client sectionData = contents.get(sectionY);        
            if(sectionData == null) return null;
            return sectionData.get(world, key);
        }

        @Override
        public @Nullable GridCatenary pop(LevelReader world, GridConnection key) {
            assertSided(world);
            int sectionY = key.getSectionY(world);
            Client sectionData = contents.get(sectionY);
            if(sectionData == null) return null;
            GridCatenary catenary = sectionData.contents.get(key);
            if(catenary == null) return null;
            sectionData.contents.remove(key);
            if(sectionData.isEmpty())
                contents.remove(sectionY);
            return catenary;
        }

        public Client getStorageInSection(int sectionY) {
            Client output = contents.get(sectionY);
            if(output == null) return null;
            if(output.isEmpty()) {
                contents.remove(sectionY);
                return null;
            }
            return output;
        }

        @Override
        public void forEach(Consumer<GridCatenary> action) {
            for(Int2ObjectMap.Entry<Client> chunk : contents.int2ObjectEntrySet())
                chunk.getValue().forEach(action);
        }

        @Override public boolean isEmpty() { return contents.isEmpty(); }
        @Override public boolean isClientSide() { return true; }
        @Override public ObjectSet<GridCatenary> getAll() { throw new UnsupportedOperationException("lol"); }
    }


    public static final class Server implements LinkDataStorable<GridLink> {

        private final ObjectOpenHashSet<GridLink> contents = new ObjectOpenHashSet<>(2);

        @Override
        public boolean add(LevelReader world, GridLink connection) {
            assertSided(world);
            return contents.add(connection);
        }

        @Override
        public @Nullable GridLink get(LevelReader world, GridConnection key) {
            assertSided(world);
            return contents.get(key);
        }

        @Override
        public @Nullable GridLink pop(LevelReader world, GridConnection key) {
            assertSided(world);
            GridLink cat = contents.get(key);
            if(key == null) return null;
            contents.remove(key);
            return cat;
        }

        @Override
        public void forEach(Consumer<GridLink> action) {
            for(GridLink cat : contents) {
                if(cat == null) continue;
                action.accept(cat);
            }
        }
        
        @Override public boolean isEmpty() { return contents.isEmpty(); }
        @Override public boolean isClientSide() { return false; }
        @Override public ObjectSet<GridLink> getAll() { return contents; }
    }

    public static final class ServerSectionable implements LinkDataStorable<GridLink> {

        private final Int2ObjectOpenHashMap<Server> contents = new Int2ObjectOpenHashMap<>(2);

        @Override
        public boolean add(LevelReader world, GridLink connection) {
            assertSided(world);
            int sectionY = connection.getSectionY(world);
            Server sectionData = contents.get(sectionY);
            if(sectionData == null) {
                sectionData = new Server();
                sectionData.contents.add(connection);
                contents.put(sectionY, sectionData);
                return true;
            }
            return sectionData.contents.add(connection);
        }

        @Override
        public @Nullable GridLink get(LevelReader world, GridConnection key) {
            assertSided(world);
            int sectionY = key.getSectionY(world);
            Server sectionData = contents.get(sectionY);        
            if(sectionData == null) return null;
            return sectionData.get(world, key);
        }

        @Override
        public @Nullable GridLink pop(LevelReader world, GridConnection key) {
            assertSided(world);
            int sectionY = key.getSectionY(world);
            Server sectionData = contents.get(sectionY);
            if(sectionData == null) return null;
            GridLink catenary = sectionData.contents.get(key);
            if(catenary == null) return null;
            sectionData.contents.remove(key);
            if(sectionData.isEmpty())
                contents.remove(sectionY);
            return catenary;
        }

        @Override
        public void forEach(Consumer<GridLink> action) {
            for(Int2ObjectMap.Entry<Server> chunk : contents.int2ObjectEntrySet())
                chunk.getValue().forEach(action);
        }

        @Override public boolean isEmpty() { return contents.isEmpty(); }
        @Override public boolean isClientSide() { return false; }
        @Override public ObjectSet<GridLink> getAll() { throw new UnsupportedOperationException("lol"); }
    }

    /**
     * Allows implementing classes to assert what kind 
     * of LinkData they store, and, by extension, where 
     * internal systems should look for retrieval. 
     * This class is used particularly in the {@link TrackedStreamable}
     * interface as a wway to allow {@link GridConnections} to
     * reassert their link data to allow catenaries to smoothly
     * hand off control and rendering context to/from all of the
     * {@link IAttachmentHolder} subclasses supported by {@link LinkDataStorable}.
     */
    public static enum DataScope implements StringRepresentable {
        SERVER_UNKNOWN,
        STATIC_CHUNK,
        BLOCKENTITY,
        MOVING_ENTITY,
        ENTITY_VOXEL_DOMAIN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
        @Override
        public String toString() {
            return getSerializedName();
        }
    }
}
