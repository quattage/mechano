package com.quattage.mechano;

import com.quattage.mechano.foundation.api.anchor.SingleAnchorEntity;
import com.quattage.mechano.foundation.api.anchor.SingleAnchorEntity.SingleAnchorEntityRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;

import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;

public class MechanoEntities {

    public static final EntityEntry<SingleAnchorEntity> SINGLE_ANCHOR = Mechano.REGISTRATE
        .entity("single_anchor", SingleAnchorEntity::new, MobCategory.MISC)
        .properties(p -> p
            .fireImmune()
            .setShouldReceiveVelocityUpdates(true)
            .setTrackingRange(5)
            .setUpdateInterval(3)
        )
        .renderer(() -> SingleAnchorEntityRenderer::new)
        .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering block entities");
    }
}
