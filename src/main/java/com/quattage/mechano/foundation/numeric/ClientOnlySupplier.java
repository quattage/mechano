package com.quattage.mechano.foundation.numeric;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A LazySupplier which can only be accessed on the client
 */
public class ClientOnlySupplier<T> implements Supplier<T> {
	
    @NotNull private final Supplier<T> supplier;
	@Nullable private T cachedResult;

	public ClientOnlySupplier(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public T get() {
		if(cachedResult == null)
            cachedResult = supplier.get();
		return cachedResult;
	}

	public void invalidate() {
		this.cachedResult = null;
	}
}
