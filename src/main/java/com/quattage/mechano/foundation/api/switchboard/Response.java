package com.quattage.mechano.foundation.api.switchboard;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public abstract sealed class Response<T> permits com.quattage.mechano.foundation.api.switchboard.Response.Anchor, com.quattage.mechano.foundation.api.switchboard.Response.Link, com.quattage.mechano.foundation.api.switchboard.Response.Agnostic {
    
    protected static Response<?>[] responses = new Response<?>[0];

    public static final Response.Agnostic SUCCESS = new Agnostic("success", true);
    public static final Response.Agnostic FAIL_CANCEL = new Agnostic("cancel", false);
    public static final Response.Agnostic FAIL_GENERIC = new Agnostic("generic", false);

    public static final StreamCodec<ByteBuf, Response<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Response<?> decode(ByteBuf buffer) {
            return responses == null ? Response.FAIL_GENERIC : responses[buffer.readByte()];
        }
        @Override
        public void encode(ByteBuf buffer, Response<?> value) {
            buffer.writeByte(value.code);
        }
    };

    private final String name;
    private final boolean success;
    protected final byte code;
    private @Nullable Consumer<T> cons = null;

    @SuppressWarnings("unchecked")
    public void executeAdditional() {
        if(cons == null) return;
        cons.accept((T)this);
    }

    protected Response(String name) {
        Response<?>[] copy = new Response<?>[responses.length + 1];
        System.arraycopy(responses, 0, copy, 0, responses.length);
        this.code = (byte)responses.length;
        this.name = name;
        this.success = false;
        copy[responses.length] = this;
        responses = copy;
    }

    protected Response(String name, boolean success) {
        Response<?>[] copy = new Response<?>[responses.length + 1];
        System.arraycopy(responses, 0, copy, 0, responses.length);
        this.code = (byte)responses.length;
        this.success = success;
        this.name = name;
        copy[responses.length] = this;
        responses = copy;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Response<?> that)) return false;
        return this.code == that.code;
    }

    @Override
    public int hashCode() {
        return code;
    }

    @Override
    public String toString() {
        return "response_" + typeID() + "_" + (success ? "success_" : "fail_") + name;
    }

    public boolean indicatesSuccess() {
        return success;
    }

    protected abstract String typeID();

    public static boolean isHighlighted(@Nullable Response<?> response) {
        if(response == null) return false;
        if(response.equals(Response.SUCCESS)) return true;
        if(!(response instanceof Response.Anchor arep)) return false;
        return !arep.equals(Response.Anchor.NONE) && (!arep.hidesAnchor());
    }

    public static boolean hidesAnchor(@Nullable Response<?> response) {
        if(response == null) return false;
        if(!(response instanceof Response.Anchor arep)) return !response.indicatesSuccess();
        return arep.hidesAnchor();
    }

    public static boolean shouldBail(@Nullable Response<?> response) {
        if(response.indicatesSuccess()) return true;
        if(!(response instanceof Response.Link link)) return false;
        return link.shouldBail();
    }

    public static final class Anchor extends Response<Anchor> {

        public static final Response.Anchor NONE = new Anchor("none", true);
        public static final Response.Anchor INCOMPATABLE = new Anchor("incompatable", false);
        public static final Response.Anchor FULL = new Anchor("full", false);

        private boolean hidesAnchor;

        protected Anchor(String name) {
            super(name);
        }

        protected Anchor(String name, boolean success) {
            super(name, success);
        }

        public Anchor andHideAnchor() {
            this.hidesAnchor = true;
            return this;
        }

        public boolean hidesAnchor() {
            return this.hidesAnchor;
        }

        @Override
        protected String typeID() {
            return "anchor";
        }
    }

    public static final class Link extends Response<Link>{

        public static final Response.Link FAIL_DESTINATION_UNSUPPORTED = new Link("destination_unsupported");
        public static final Response.Link FAIL_DESTINATION_FULL = new Link("desination_full");
        public static final Response.Link FAIL_USER_CANCEL = new Link("user_cancel");
        public static final Response.Link FAIL_DUPLICATE = new Link("duplicate");
        public static final Response.Link FAIL_TOO_CLOSE = new Link("too_close");
        public static final Response.Link FAIL_TOO_FAR = new Link("too_far");
        public static final Response.Link FAIL_DIMENSION_MISMATCH = new Link("dimension_mismatch");
        public static final Response.Link FAIL_SYNC_OUTDATED = new Link("sync_outdated");

        private boolean shouldBail = false;

        protected Link(String name) {
            super(name);
        }

        protected Link(String name, boolean success) {
            super(name, success);
        }

        /**
         * If this method is called, this response will cause the 
         * connection that the player is currently making to immediately cancel
         * itself and return to its passive state. 
         * @return this ConnectionResponse
         */
        public Link andBailout() {
            this.shouldBail = true;
            return this;
        }

        public boolean shouldBail() {
            return this.shouldBail;
        }

        @Override
        protected String typeID() {
            return "link";
        }
    }

    public record LinkResponseHolder(Response<?> response, byte[] anchorData) {
        public static final StreamCodec<ByteBuf, LinkResponseHolder> STREAM_CODEC = StreamCodec.composite(
            Response.STREAM_CODEC, LinkResponseHolder:: response,
            ByteBufCodecs.BYTE_ARRAY, LinkResponseHolder::anchorData,
            LinkResponseHolder::new
        );
        public static LinkResponseHolder of(@Nullable GridLink link, Response<?> response) {
            if(link == null) return new LinkResponseHolder(response, new byte[] {Byte.MIN_VALUE, Byte.MIN_VALUE});
            return LinkResponseHolder.of(link.getStart(), link.getEnd(), response);
        }
        public static LinkResponseHolder of(@Nullable GridNode start, @Nullable GridNode end, Response<?> response) {
            return new LinkResponseHolder(response, new byte[] {
                start == null ? Byte.MIN_VALUE : start.links == null ? Byte.MIN_VALUE : (byte)(start.links.size() - 128),
                end == null ? Byte.MIN_VALUE : end.links == null ? Byte.MIN_VALUE : (byte)(end.links.size() - 128)
            });
        }
    }




    public static final class Agnostic extends Response<Agnostic> {
        protected Agnostic(String name) {
            super(name);
        }
        protected Agnostic(String name, boolean success) {
            super(name, success);
        }
        @Override
        protected String typeID() {
            return "agnostic";
        }
    }







    public static enum Task {
        CREATE,
        DESTROY,
        SYNC,
        UNSYNC,
        RESYNC,
        CHUNK_LOAD
        ;

        public static final StreamCodec<ByteBuf, Task> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Task decode(ByteBuf buffer) {
                return Task.values()[buffer.readByte()];
            }
            @Override
            public void encode(ByteBuf buffer, Task value) {
                buffer.writeByte(value.ordinal());
            }
        };
    }
}
