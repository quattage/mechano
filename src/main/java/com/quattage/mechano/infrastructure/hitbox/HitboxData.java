package com.quattage.mechano.infrastructure.hitbox;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.helper.VoxelShapeBuilder.ShapeAccumulator;
import com.quattage.mechano.foundation.helper.VoxelShapeBuilder.TemporaryShape;
import com.tterrag.registrate.providers.RegistrateGenericProvider;
import com.tterrag.registrate.providers.RegistrateGenericProvider.Generator;
import com.tterrag.registrate.providers.RegistrateGenericProvider.GeneratorData;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;


public class HitboxData {

    private static final Gson GSON = new GsonBuilder().setLenient().create();


    public static void generate(RegistrateGenericProvider provider) {
        provider.add(new Generator() {
            @Override
            public DataProvider generate(GeneratorData data) {
                return new Provider(data);
            }
        });
    }

    public static class Provider implements DataProvider {
        
        private final Path inputDir;
        private final String generatedName;
        private final Path outputDir;

        @SuppressWarnings("deprecation")
        protected Provider(GeneratorData data) {
            this.inputDir = data.output()
                .getOutputFolder(PackOutput.Target.DATA_PACK)
                .getParent().getParent().getParent()
                .resolve("main/resources/data/" + Mechano.ID + "/hitboxes");

            this.generatedName = WordUtils.capitalize(Mechano.ID) + "Hitboxes";

            this.outputDir = data.output()
                .getOutputFolder(PackOutput.Target.DATA_PACK)
                .getParent().getParent().getParent()
                .resolve("main/java/com/quattage/" + Mechano.ID + "/" + generatedName + ".java");
        }

        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return CompletableFuture.runAsync(() -> generate(output));
        }

        @Override
        public String getName() {
            return "hitbox";
        }

        @SuppressWarnings("unchecked")
        public void generate(CachedOutput output) {

            long startTime = System.currentTimeMillis();
            int count = 0;

            createDirectory(inputDir);
            List<Path> files = listFiles(inputDir);
            if(files == null) return;

            StringBuilder cls = generateClassFile(generatedName);

            for(Path path : files) {
                try(Reader reader = Files.newBufferedReader(path)) {
                    String name = path.getFileName().toString();
                    readAndGenerate(
                        cls, name.substring(0, name.lastIndexOf(".")), 
                        (List<Map<String, Map<String, Object>>>)(GSON.fromJson(reader, Map.class).get("elements"))
                    );
                } catch(IOException e) {
                    Mechano.LOGGER.error("Error reading file '" + path + "'");
                    e.printStackTrace();
                }
                count++;
            }

            try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputDir))) {
                pw.write(cls.toString());
            } catch(IOException e) {
                Mechano.LOGGER.error("Error creating class definition for '" + generatedName + ".java'");
                e.printStackTrace();
            }

            long elapsed = (System.currentTimeMillis() - startTime);
            Mechano.LOGGER.info("Generated " + count + " hitboxes in " + elapsed + "ms");
        }





        private void createDirectory(Path dir) {
            try {
                Files.createDirectories(dir);
            } catch(IOException e) {
                Mechano.LOGGER.error("Failure while creating hitbox directory!");
                e.printStackTrace();
            }
        }





        private @Nullable List<Path> listFiles(Path dir) {
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






        private void readAndGenerate(StringBuilder cls, String filename, List<Map<String, Map<String, Object>>> modelFile) {
            String[] tokens = filename.split("\\.");

            ShapeAccumulator accumulator = new ShapeAccumulator();
            TemporaryShape shape = new TemporaryShape();

            try{ 
                for(Map<String, Map<String, Object>> raw : modelFile) {
                    for(Map.Entry<String, Map<String, Object>> s : raw.entrySet()) {
                        shape.collect(s);
                        shape.flushInto(accumulator);
                    }
                }
            } catch(Exception e) {
                Mechano.LOGGER.error("An error occured while extracting usable data from JSON hitbox model '" + filename + "'");
                e.printStackTrace();
                return;
            }

            if(accumulator.isEmpty()) {
                Mechano.LOGGER.warn("Skipped generation of hitbox '" + filename + "' - The resulting shape was empty.");
                return;
            }


        }

        private StringBuilder generateClassFile(String generatedName) {
            StringBuilder cls = new StringBuilder();
            cls.append("package " + "com.quattage." + Mechano.ID + ";\n\n");
            cls.append("import javax.annotation.processing.Generated;\n\n");
            cls.append("import com.quattage.mechano.foundation.helper.VoxelShapeBuilder;\n");
            cls.append("import com.quattage.mechano.infrastructure.hitbox.HitboxRepresentable;\n");
            cls.append("import com.quattage.mechano.infrastructure.hitbox.LazyRotatableHitbox;\n\n");
            cls.append("import net.neoforged.bus.api.IEventBus;\n\n");
            cls.append("@SuppressWarnings(\"unused\")\n");
            cls.append("@Generated(\"" + this.getClass().getCanonicalName() + "\")\n");
            cls.append("public class " + generatedName + " {\n\n");
            cls.append("\tpublic void register(IEventBus modBus) {}\n\n");
            cls.append("}");

            return cls;
        }
    }
}
