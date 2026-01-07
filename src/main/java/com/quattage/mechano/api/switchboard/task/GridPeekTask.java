package com.quattage.mechano.api.switchboard.task;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.foundation.numeric.VectorOperations;
import com.quattage.mechano.foundation.numeric.VectorOperations.Ray;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GridPeekTask implements ActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return null;
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        return;
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        return null;
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        return GridAction.TASK_GRID_PEEK;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        final HitResult result = Minecraft.getInstance().hitResult;
        if(result instanceof BlockHitResult bhr && result.getType() == HitResult.Type.BLOCK) {
            BlockEntity be = grid.getWorld().getBlockEntity(bhr.getBlockPos());
            if(be instanceof GriddableBlockEntity gbe) return printSourceDetails(gbe);
        }
        if(result instanceof EntityHitResult ehr && result.getType() == HitResult.Type.ENTITY) {
            Entity target = ehr.getEntity();
            if(target instanceof Griddable ge) return printSourceDetails(ge);
            // TODO attachable griddable
            // if(target != null && target.hasData(null))
        }
        LocalPlayer lp = self();
        Ray ray = VectorOperations.getLookingRay(lp);
        List<Entity> nearbyEntities = grid.getWorld().getEntitiesOfClass(Entity.class, AABB.ofSize(lp.position(), 10d, 10d, 10d));
        for(Entity e : nearbyEntities) {
            Optional<Vec3> clip = e.getBoundingBox().clip(ray.start, ray.end);
            if(!clip.isPresent()) continue;
            if(e instanceof Griddable ge) return printSourceDetails(ge);
            // TODO attachable griddable
            // if(target != null && target.hasData(null))
        }
        self().sendSystemMessage(Component.literal("No target").withStyle(ChatFormatting.RED));
        return GridAction.RESPONSE_SUCCESS;
    }
    
    private GridAction printSourceDetails(Griddable<?> source) {
        self().sendSystemMessage(Component.literal(source.toString()));
        return GridAction.RESPONSE_SUCCESS;
    }
}
