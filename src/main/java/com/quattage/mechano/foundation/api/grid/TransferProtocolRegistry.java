package com.quattage.mechano.foundation.api.grid;

import com.quattage.mechano.foundation.SimpleTransferProtocol;

import net.minecraft.nbt.CompoundTag;

public class TransferProtocolRegistry {
    
    private SimpleTransferProtocol[] protocols;
    private boolean loaded = false;

    private static final EmptyProtocol EMPTY_PROTOCOL = new EmptyProtocol();

    protected void markLoaded() {
        this.loaded = true;
    }

    public void registerProtocol(SimpleTransferProtocol protocol) {
        if(loaded) throw new IllegalStateException("Couldn't register TransferProtocol '" + protocol.getName() + " - The registry has already finished loading!");
        SimpleTransferProtocol[] copy = new SimpleTransferProtocol[protocols.length + 1];
        System.arraycopy(protocols, 0, copy, 0, protocols.length);
        copy[protocols.length] = protocol;
        protocol.index = protocols.length;
        this.protocols = copy;
    }

    /**
     * Gets a {@link SimpleTransferProtocol} from a supplied integer index or string name.
     * @param index
     * @return TransferProtocol at the given index, or {@link SimpleTransferProtocol#EMPTY} if none could be found.
     */
    public SimpleTransferProtocol get(int index) {
        if(protocols.length == 0) 
            throw new IllegalStateException("Couldn't retrieve transfer protocol at index " + index + " - the registry hasn't finished loading!");
        if(index < 0) return TransferProtocolRegistry.EMPTY_PROTOCOL;
        if(index >= protocols.length) 
            throw new IndexOutOfBoundsException("Couldn't retrieve transfer protocol at index " + index + " - There are only " + protocols.length + " protocols in the registry!");
        return protocols[index];
    }

    /**
     * Gets a {@link SimpleTransferProtocol} from a supplied integer index or string name.
     * @param name The value returned by {@link SimpleTransferProtocol#getName}
     * @return TransferProtocol with the given name, or {@link SimpleTransferProtocol#EMPTY} if none could be found.
     */
    protected SimpleTransferProtocol get(String name) {
        if(!loaded)
            throw new IllegalStateException("Couldn't retrieve transfer protocol '" + name + "'' - the registry hasn't finished loading!");
        if(protocols.length == 0) 
            throw new IllegalStateException("Couldn't retrieve transfer protocol '" + name + "'- the registry is empty! (something serious has gone wrong)");
        if(name == null)
            throw new NullPointerException("Couldn't retrieve transfer protocol - The supplied name is null!");
        if(name.isEmpty())
            throw new IllegalArgumentException("Couldn't retrieve transfer protocol - The supplied name is null!");
        for(int x = 0; x < protocols.length; x++) {
            if(protocols[x] != null && protocols[x].getName().equals(name))
                return protocols[x];
        }
        return TransferProtocolRegistry.EMPTY_PROTOCOL;
    }

    private static class EmptyProtocol extends SimpleTransferProtocol {

        @Override
        public int getCost() {
            return 127;
        }

        @Override
        public CompoundTag writeTo(CompoundTag in) {
            return in;
        }

        @Override
        public void readFrom(CompoundTag in) {
            return;
        }

        @Override
        public String getName() {
            return "EMPTY";
        }
        
    }
}
