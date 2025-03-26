package com.quattage.mechano;

import com.quattage.mechano.foundation.block.hitbox.HitboxProvider;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGridDispatcher;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.GridClientCacheProvider;
import com.quattage.mechano.foundation.electricity.watt.WattStorable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerLifecycleEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

@EventBusSubscriber
public class MechanoCapabilities {

    public final Capability<GlobalTransferGrid> SERVER_GRID_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public final Capability<GridClientCache> CLIENT_CACHE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public final Capability<WattStorable> WATT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public static final ResourceLocation CLIENT_CACHE_CAPABILITY_ID = Mechano.asResource("transfer_grid_client_cache");
    public static final ResourceLocation SERVER_GRID_CAPABILITY_ID = Mechano.asResource("transfer_grid_server_manager");

    // capability-adjacent
    public final HitboxProvider HITBOX_PROVIDER = new HitboxProvider();

    public void registerTo(IEventBus bus) {
        bus.addGenericListener(Level.class, this::addWorldCapabilities);
        Mechano.logReg("capabilities");
    }

    @SuppressWarnings({"resource"})
    public void addWorldCapabilities(AttachCapabilitiesEvent<Level> event) {
        if(event.getObject() instanceof ClientLevel) {
            if (event.getCapabilities().containsKey(CLIENT_CACHE_CAPABILITY_ID)){
                Mechano.LOGGER.info(event.getObject().dimension().location() + " has ClientGrid capability already attached");
                return;
            }
            Mechano.LOGGER.info("Attaching ClientCache capability to " + event.getObject().dimension().location());
            event.addCapability(CLIENT_CACHE_CAPABILITY_ID, new GridClientCacheProvider((ClientLevel)event.getObject()));
        } else if (event.getObject() instanceof ServerLevel) {
            if (event.getCapabilities().containsKey(SERVER_GRID_CAPABILITY_ID)){
                Mechano.LOGGER.info(event.getObject().dimension().location() + " has ServerGrid capability already attached");
                return;
            }
            Mechano.LOGGER.info("Attaching ServerGrid capability to " + event.getObject().dimension().location());
            event.addCapability(SERVER_GRID_CAPABILITY_ID, new GlobalTransferGridDispatcher(event.getObject()));
        }
    }

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        Mechano.logReg("Watt Capability");
        event.register(WattStorable.class);
    }

    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(HITBOX_PROVIDER);
    }
}
