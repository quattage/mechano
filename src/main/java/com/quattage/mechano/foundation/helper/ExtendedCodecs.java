package com.quattage.mechano.foundation.helper;

import java.util.Base64;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

public class ExtendedCodecs {
    
    public static final Codec<byte[]> B64 = Codec.STRING.xmap(
        s -> Base64.getDecoder().decode(s),
        bytes -> Base64.getEncoder().encodeToString(bytes)
    );

    public static final Codec<Character> CHAR = new PrimitiveCodec<Character>() {
        @Override public <T> DataResult<Character> read(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).map(number -> (char)number.intValue());
        }
        @Override public <T> T write(DynamicOps<T> ops, Character value) {
            return ops.createString("" + value);
        }
        @Override public String toString() {
            return "Character";
        }
    };
}
