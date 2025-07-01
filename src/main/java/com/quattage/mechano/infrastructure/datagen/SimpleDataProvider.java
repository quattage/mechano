package com.quattage.mechano.infrastructure.datagen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.tterrag.registrate.providers.RegistrateGenericProvider.GeneratorData;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

public abstract class SimpleDataProvider implements DataProvider {

    protected Path outputDir;

    public SimpleDataProvider(GeneratorData data) {
        this.outputDir = getOutputDirectory(data);
    }

    public abstract Path getOutputDirectory(GeneratorData data);

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.runAsync(() -> prepare(output));
    }

    public final void prepare(CachedOutput output) {
        createDirectory(outputDir);
        generate(output);
    }

    public abstract void generate(CachedOutput output);

    protected final void createDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch(IOException e) {
            Mechano.LOGGER.error("Failure while creating hitbox directory!");
            e.printStackTrace();
        }
    }


protected final @Nullable List<Path> listFiles(Path dir) {
    List<Path> output = null;
    try(Stream<Path> fileStream = Files.walk(dir, 8)) {
        output = fileStream
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".json"))
            .collect(Collectors.toList());
    } catch(IOException e) {
        Mechano.LOGGER.error("Failure while traversing hitbox directory!");
        e.printStackTrace();
    }
    return output;
}
}
