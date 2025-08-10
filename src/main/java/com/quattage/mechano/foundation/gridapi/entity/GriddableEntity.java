package com.quattage.mechano.foundation.gridapi.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.MechanoEntities;
import com.quattage.mechano.foundation.gridapi.Griddable;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorArray;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.anchor.SurrogateNode;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.EntityUUID;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * This class is designed specifically to function as a singular, moveable, invisible
 * point in the world. This point does all of the things outlined by the {@link Griddable} 
 * interface, and very little more. If you're looking to implement a more traditional 
 * LivingEntity that participates in the grid, take a look at the 
 * {@link GriddableEntityAttachment data attachment} instead of this class.<p>
 */
public final class GriddableEntity extends Entity implements Griddable<GriddableEntity> {

    private AnchorArray anchor;
    private final SurrogateNode surrogate = new SurrogateNode(this);

    public static GriddableEntity of(Level world, Vec3 pos) {
        GriddableEntity out = new GriddableEntity(MechanoEntities.ANCHOR.get(), world);
        out.setPos(pos);
        world.addFreshEntity(out);
        out.refreshDimensions();
        return out;
    }

    public GriddableEntity(EntityType<?> type, Level world) {
        super(type, world);
        setInvisible(true);
        setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    public void constructAnchors(AnchorArray.Builder anchors) {
        anchor = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(getUUID(), 0), 0, 0, 0, 2f, true, 2)));
    }
    @Override public void onAddedToLevel() { constructAnchors(null); }

    // this class is just a dummy so all of the normal entity features are replaced with simplified versions
    @Override protected double getDefaultGravity() { return 0; }
    @Override public void load(CompoundTag compound) {}
    @Override public CompoundTag saveWithoutId(CompoundTag compound) { return compound; }
    @Override public boolean save(CompoundTag compound) { return false; }
    @Override public boolean saveAsPassenger(CompoundTag compound) { return false; }
    @Override public boolean shouldRender(double x, double y, double z) { return false; }
    @Override public boolean isInvisibleTo(Player player) { return true; }
    @Override public boolean isInvisible() { return true; }
    @Override public boolean isInvulnerable() { return true; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState blockState, float explosionPower) { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean isPushedByFluid(FluidType type) { return false; }
    @Override public Component getDisplayName() { return Component.empty(); }
    @Override public boolean hasCustomName() { return false; }
    @Override public void setCustomName(Component name) { return; }
    @Override public boolean isCustomNameVisible() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public void igniteForTicks(int ticks) { return; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return false; }
    @Override protected void defineSynchedData(Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag compound) {}
    @Override protected void addAdditionalSaveData(CompoundTag compound) {}
    @Override protected boolean canRide(Entity vehicle) { return false; }
    @Override public boolean canCollideWith(Entity entity) { return false; }
    @Override public void kill() { this.remove(Entity.RemovalReason.KILLED); }
    @Override public ProjectileDeflection deflection(Projectile projectile) { return ProjectileDeflection.NONE; }
    @Override public PushReaction getPistonPushReaction() { return PushReaction.IGNORE; }
    @Override public AABB getBoundingBoxForCulling() { return getAnchor().makeHitbox(level(), true); }
    @Override public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) { return false; }

    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        return getPosition(partialTicks);
    }

    @Override public boolean isMovable() {
        return true;
    }

    @Override
    public boolean isInteractable() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return false;
    }
    
    @Override
    public void tick() {
        // VectorHelper.drawDebugBox(getPosition(1));
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {        
        AABB box = getAnchor().makeHitbox(level(), true);
        return EntityDimensions.fixed((float)box.maxX, (float)box.maxY);
    }

    @Override
    public AnchorArray getAnchors() {
        return anchor;
    }

    @Override
    public Level getWorld() {
        return level();
    }

    @Override
    public SurrogateNode getSurrogate() {
        return surrogate;
    }

    @Override
    public GridUUID createSupplementaryAddress() {
        if(anchor == null) return new EntityUUID(getUUID(), 0);
        return anchor.getByIndex(0).getAddress();
    }

    @Override
    public String describeState() {
        return "SingleAnchor[" + getPosition(1) + ", (" + getUUID() + ")]";
    }

    @Override
    public GriddableEntity getSource() {
        return this;
    }

    public static class SingleAnchorEntityRenderer extends EntityRenderer<GriddableEntity> {

        public SingleAnchorEntityRenderer(Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(GriddableEntity livingEntity, Frustum camera, double camX, double camY, double camZ) { return false; }

        @Override
        public void render(GriddableEntity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {}

        @Override
        public ResourceLocation getTextureLocation(GriddableEntity entity) {
            return null;
        }

    }
}
