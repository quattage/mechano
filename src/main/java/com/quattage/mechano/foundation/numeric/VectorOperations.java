

package com.quattage.mechano.foundation.numeric;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.simibubi.create.AllSpecialTextures;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class VectorOperations {

    private static final float eps = 1e-7f;

    @OnlyIn(Dist.CLIENT)
    public static boolean isInWorld(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null && mc.level.getWorldBorder().isWithinBounds(pos);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isInWorld(Vector3d pos) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null && mc.level.getWorldBorder().isWithinBounds(pos.x, pos.z);
    }

    public static boolean isGreater(Vec3 a, Vec3 b) {
        return a.x > b.x && a.y > b.y && a.z > b.z();
    }

    public static float getGreatest(Vector3f a) {
        return Math.max(a.x, Math.max(a.y, a.z));
    }

    public static boolean approxEqual(Vec3 a, Vec3 b) {
        return Math.abs(a.x - b.x) < VectorOperations.eps && Math.abs(a.y - b.y) < VectorOperations.eps && Math.abs(a.z - b.z) < VectorOperations.eps;
    }

    /**
     * Draws the normal vector as a line in 3d space
     * @param basis Position of the starting point
     * @param normal Offset towards the ending point (the direction of the ray)
     * @param c Color of the line to be drawn
     */
    public static void drawDebugRay(Vec3 basis, Vec3 normal, Color c) {
        Outliner.getInstance().showLine("rdb_" + basis + normal, basis, basis.add(normal.x * 0.7f, normal.y * 0.7f, normal.z * 0.7f)).lineWidth(0.02f).disableCull().colored(c);
    }

    /**
     * Draws the normal vector as a line in 3d space
     * @param basis Position of the starting point
     * @param normal Offset towards the ending point (the direction of the ray)
     * @param c Color of the line to be drawn
     */
    public static void drawDebugRay(Vec3 basis, Vector3f normal, Color c) {
        Outliner.getInstance().showLine("rdb_" + basis + normal, basis, basis.add(normal.x * 0.3f, normal.y * 0.3f, normal.z * 0.3f)).lineWidth(0.02f).disableCull().colored(c);
    }

    /**
     * Draws the normal vector as a line in 3d space
     * @param basis Position of the starting point
     * @param normal Offset towards the ending point (the direction of the ray)
     * @param c Color of the line to be drawn
     */
    public static void drawDebugRay(Vec3 basis, Vector3f normal, Color c, String id) {
        Outliner.getInstance().showLine("rdb_" + id, basis, basis.add(normal.x * 0.3f, normal.y * 0.3f, normal.z * 0.3f)).lineWidth(0.02f).disableCull().colored(c);
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos) {
        VectorOperations.drawDebugBox(pos, VectorOperations.toColor(pos), "debug_" + pos.hashCode());
    }

    /***
     * Draws multiple debug boxes at the given Vec3 positions
     */
    public static void drawDebugBox(Vec3... positions) {
        for(Vec3 pos : positions)
            VectorOperations.drawDebugBox(pos, VectorOperations.toColor(pos), "debug_" + pos.hashCode());
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, String hash) {
        VectorOperations.drawDebugBox(pos, VectorOperations.toColor(pos), hash);
    }
    
    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, Color color) {
        VectorOperations.drawDebugBox(pos, color, "debug_" + pos.hashCode());
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, Color color, String hash) {
        Outliner.getInstance().showAABB(hash, VectorOperations.toAABB(pos, 0.1f))
            .disableLineNormals()
            .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(0.006f)
            .colored(color);
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, float size, Color color, String hash) {
        Outliner.getInstance().showAABB(hash, VectorOperations.toAABB(pos, size))
            .disableLineNormals()
            .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(0.006f)
            .colored(color);
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vector3d pos, float size, Color color, String hash) {
        Outliner.getInstance().showAABB(hash, VectorOperations.toAABB(pos, size))
            .disableLineNormals()
            .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(0.006f)
            .colored(color);
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vector3f pos, float size, Color color, String hash) {
        Outliner.getInstance().showAABB(hash, VectorOperations.toAABB(pos, size))
            .disableLineNormals()
            .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(0.006f)
            .colored(color);
    }

    /***
     * Creates a new AABB at the given Vec3.
     * @param pos Vec3 to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given Vec3
     */
    public static AABB toAABB(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }

    /***
     * Creates a new AABB at the given Vec3.
     * @param pos Vec3 to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given Vec3
     */
    public static AABB toAABB(Vector3d pos, float s) {
        return VectorOperations.toAABB(new Vec3(pos.x, pos.y, pos.z), s);
    }

    /***
     * Creates a new AABB at the given Vec3.
     * @param pos Vec3 to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given Vec3
     */
    public static AABB toAABB(Vector3f pos, float s) {
        return VectorOperations.toAABB(new Vec3(pos.x, pos.y, pos.z), s);
    }

    
    /***
     * Creates a new AABB at the given BlockPos.
     * @param pos BlockPos to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given BlockPos
     */
    public static AABB toAABB(BlockPos pos, float s) {
        return VectorOperations.toAABB(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) , s);
    }

    /***
     * Converts an arbitrary Vec3 (non-normalized) into a percentage based color.
     * @param vec Vector 
     * @param variation (Optional, default is 8) Lower numbers result in more noticable 
     * changes in color, requiring smaller changes to the input vector
     * @return A new Color derived from the input vector
     */
    public static Color toColor(Vec3 vec) {
        return VectorOperations.toColor(vec, 8);
    }

    /***
     * Converts an arbitrary Vec3 (non-normalized) into a percentage based color.
     * @param vec Vector 
     * @param variation (Optional, default is 8) Lower numbers result in more noticable 
     * changes in color, requiring smaller changes to the input vector
     * @return A new Color derived from the input vector
     */
    public static Color toColor(Vector3d vec) {
        return VectorOperations.toColor(new Vec3(vec.x, vec.y, vec.z), 8);
    }

    /***
     * Converts an arbitrary Vec3 (non-normalized) into a percentage based color.
     * @param vec Vector 
     * @param variation (Optional, default is 8) Lower numbers result in more noticable 
     * changes in color, requiring smaller changes to the input vector
     * @return A new Color derived from the input vector
     */
    public static Color toColor(Vec3 vec, int variation) {
        if(variation < 2) variation = 2;
        Vec3i norm = new Vec3i(
            (int)Math.abs(vec.x % variation),
            (int)Math.abs(vec.y % variation),
            (int)Math.abs(vec.z % variation)
        );
        Color out = new Color(
            (int)((norm.getX() / 8f) * 255),
            (int)((norm.getY() / 8f) * 255),
            (int)((norm.getZ() / 8f) * 255)
        );
        return out;
    }

    /***
     * Gets the HitResult for the given player.
     * @param player Player to use
     * @param dist How far the ray should go before terminating
     * @return HitResult describing the player's absolute look position.
     */
    public static VectorOperations.Ray getLookingRay(Player player) {
        return VectorOperations.getLookingRay(player, DeltaTracker.ONE.getGameTimeDeltaPartialTick(false), (float)player.blockInteractionRange());
    }


    /***
     * Gets the HitResult for the given player.
     * @param player Player to use
     * @param dist How far the ray should go before terminating
     * @return HitResult describing the player's absolute look position.
     */
    public static VectorOperations.Ray getLookingRay(Player player, float pTicks, float dist) {
        Vec3 viewDir = player.getViewVector(pTicks);
        Vec3 start = player.getEyePosition(pTicks);
        Vec3 end = start.add(viewDir.x * dist, viewDir.y * dist, viewDir.z * dist);
        return new Ray(start, player.getCommandSenderWorld().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)), viewDir);
    }

    public static double[] toArray(Vec3 vec) {
        return new double[] {vec.x, vec.y, vec.z};
    }

    public static class Ray {
        public final Vec3 start;
        public final Vec3 end;
        public final Vec3 normal;
        public Ray(Vec3 start, HitResult hit, Vec3 normal) { 
            this.start = start; 
            this.end = hit.getLocation();
            this.normal = normal;
        }
        @Override
        public String toString() {
            return "Ray[" + end + "]";
        }
    }
}