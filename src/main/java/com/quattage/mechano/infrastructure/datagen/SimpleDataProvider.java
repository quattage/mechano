package com.quattage.mechano.infrastructure.datagen;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public abstract class SimpleDataProvider implements DataProvider {

    private final Path outputDir;
    private final ExistingFileHelper fileHelper;

    public SimpleDataProvider(PackOutput output, ExistingFileHelper fileHelper) {
        this.outputDir = configureOutputDirectory(output);
        this.fileHelper = fileHelper;
    }

    protected abstract Path configureOutputDirectory(PackOutput output);

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.runAsync(() -> prepare(output));
    }

    public final void prepare(CachedOutput output) {
        createDirectory(outputDir);
        generate();
    }

    protected ExistingFileHelper getFileHelper() {
        return fileHelper;
    }

    protected Path getOutputDir() {
        return outputDir;
    }

    /**
     * The implementing 
     * @param output
     */
    public abstract void generate();

    protected final void createDirectory(Path dir) {
        try { Files.createDirectories(dir); } catch(IOException e) {
            Mechano.LOGGER.error("Failure while creating hitbox directory!");
            e.printStackTrace();
        }
    }

    public PrintWriter createWriter() throws IOException {
        return new PrintWriter(Files.newBufferedWriter(getOutputDir()));
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
