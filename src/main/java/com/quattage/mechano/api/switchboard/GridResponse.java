package com.quattage.mechano.api.switchboard;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.theme.Color;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public enum GridResponse implements StringRepresentable {

    TASK_CREATE_LINK                     (true, false),
    TASK_REASSERT_LINK                   (true, false),
    TASK_DESTROY_LINK                    (true, false),
    TASK_DESTROY_LINK_LAZY               (true, false),
    TASK_FREE_LINK                       (true, false),
    TASK_SYNC_SINGLE                     (true, false),
    TASK_SYNC_ANCHORS                    (true, false),
    TASK_FORGET_ANCHORS                  (true, false),
    TASK_SELECT_SUCCESS                  (true, false, HighlightMode.SHOW_SUCCESS),
    TASK_SWAP_START                      (true, false),
    TASK_SWAP_END                        (true, false),
    TASK_COMPLETED                       (true, false, HighlightMode.SHOW_SUCCESS),
    FAIL_INTERACTION_CANCELLED           (false, true),
    FAIL_DESTINATION_UNSUPPORTED         (false, true),
    FAIL_HELD_INCOMPATIBLE               (false, false),
    FAIL_DESTINATION_FULL                (false, false),
    FAIL_DUPLICATE                       (false, true),
    FAIL_TOO_CLOSE                       (false, true),
    FAIL_TOO_FAR                         (false, false),
    FAIL_DIM_MISMATCH                    (false, true),
    FAIL_START_MISSING                   (false, true),
    FAIL_END_MISSING                     (false, true),
    FAIL_BOTH_ENDS_MISSING               (false, true),
    FAIL_CATENARY_NOT_FOUND              (false, true),
    FAIL_GENERIC                         (false, true, HighlightMode.SHOW_FAILURE),
    NONE                                 (false, false, HighlightMode.SHOW_PASSIVE);

    public static final StreamCodec<ByteBuf, GridResponse> STREAM_CODEC = new StreamCodec<>() {
        @Override public GridResponse decode(ByteBuf buffer) { return GridResponse.values()[buffer.readByte()]; }
        @Override public void encode(ByteBuf buffer, GridResponse value) { buffer.writeByte(value.ordinal()); }
    };

    public static void logUnhandled(@Nullable GridResponse response, @Nullable Object o) {
        Mechano.LOGGER.error(("Response type '" + response + "' is not handled ") 
            + o == null ? "!" : (" by handler in '" + o.getClass().getSimpleName() + "'!"));
    }

    private final boolean isTask;
    private final boolean shouldFailHard;
    private final HighlightMode mode;
    

    GridResponse(boolean isTask, boolean shouldFailHard, HighlightMode mode) { 
        this.isTask = isTask; 
        this.shouldFailHard = shouldFailHard;
        this.mode = mode;
    }

    GridResponse(boolean isTask, boolean shouldFailHard) {
        this.isTask = isTask; 
        this.shouldFailHard = shouldFailHard;
        this.mode = HighlightMode.HIDE;
    }

    /**
     * Indicates whether or not this response represents the completion
     * of a task.
     * @return If <code>false</code>, this task represents a failure.
     */
    public boolean indicatesCompletion() {
        return this.isTask;
    }

    /**
     * Indicates whether this response represents a hard or soft failure mode.
     * @return If <code>true</code>, implementations should reset connection progress.
     */
    public boolean shouldFailHard() {
        return this.shouldFailHard;
    }

    /**
     * @return the {@link HighlightMode} associated with this response
     */
    public HighlightMode getVisibility() {
        return this.mode; 
    }

    @Override
    public String getSerializedName() {
        return "response_" + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.getSerializedName();
    }

    public ResourceLocation getKey() {
        return Mechano.asResource(this.getSerializedName());
    }

    public static enum HighlightMode {
        /**
         * Shows the vanilla-style black outline around the targeted AnchorPoint
         */
        SHOW_PASSIVE(null),
        /**
         * Shows a green AABB drawn by Create's outliner
         */
        SHOW_SUCCESS(new Color(0, 255, 0)),
        /**
         * Shows a red AABB drawn by Create's outliner
         */
        SHOW_FAILURE(new Color(255, 0, 0)),
        HIDE(null);

        private final Color color;
        HighlightMode(Color color) { this.color = color; }
        public boolean isVisible() { return this != HIDE; }
        public boolean isHighlighted() { return this.color != null; }
        public @Nullable Color getColor() { return color; }
    }
}
