package com.quattage.mechano.core.events;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/***
 * Basic behavoir template which exposes useful values to be accessed by subclasses. This is useful to render client-side
 * effects such as AABB highlights for wrenching or client-side particles.
 */
public abstract class ClientBehavior {
    protected Minecraft instance = null;
    public final String name;
    private Player player = null;
    private ClientLevel world = null;
    private Vec3 hit = null;
	private ItemStack mainHandStack = null;
    private ItemStack offHandStack = null;

    public static HashMap<String, ClientBehavior> behaviors = new HashMap<String, ClientBehavior>();

    public ClientBehavior(String name) {
        this.name = name;
        this.instance = Minecraft.getInstance();
        behaviors.put(name, this);
    }

    public void updateValues() {
        world = instance.level;
        hit = instance.hitResult.getLocation();
		player = instance.player;
		mainHandStack = player.getMainHandItem();
        offHandStack = player.getOffhandItem();
    }
    
    public boolean isShifting() {
        return player.isSteppingCarefully();
    }

    @OnlyIn(Dist.CLIENT)
	public void tick() {
        updateValues();
		if (player == null || world == null || !(instance.hitResult instanceof BlockHitResult result))
			return;
        BlockPos hitBlockPos = result.getBlockPos();

        if(!shouldTick(world, player, mainHandStack, offHandStack, hit, hitBlockPos)) return;
        tickSafe(world, player, mainHandStack, offHandStack, hit, hitBlockPos);
    }

    public String toString() {
        return name;
    }

    /***
     * A manually defined condiiton to determine whether this ClientBehavior should tick or not.
     * @param world World to tick.
     * @param player 
     * @param mainHand ItemStack in main hand
     * @param offHand ItemStack in off hand
     * @param lookingPosition Exact position the player is looking
     * @param lookingBlockPos The BlockPos the player is looking
     * @return Boolean value representing whether the tick() method should be run or not.
     */
    @OnlyIn(Dist.CLIENT)
    public abstract boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, 
        ItemStack offHand, Vec3 lookingPosition, BlockPos lookingBlockPos);

    /***
     * Runs every tick. Only runs safely, when sanity checks are passed.
     * @param world World to tick.
     * @param player 
     * @param mainHand ItemStack in main hand
     * @param offHand ItemStack in off hand
     * @param lookingPosition Exact position the player is looking
     * @param lookingBlockPos The BlockPos the player is looking
     */
    @OnlyIn(Dist.CLIENT)
    public abstract void tickSafe(ClientLevel world, Player player, ItemStack mainHand, 
        ItemStack offHand, Vec3 lookingPosition, BlockPos lookingBlockPos);
}
