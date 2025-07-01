package com.quattage.mechano.infrastructure.datagen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.processing.Generated;

import org.apache.commons.lang3.text.WordUtils;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.HitboxRepresentable;
import com.quattage.mechano.foundation.block.hitbox.HitboxStateTree;
import com.quattage.mechano.foundation.block.hitbox.LazyRotatableHitbox;
import com.quattage.mechano.foundation.block.hitbox.VoxelShapeBuilder;
import com.quattage.mechano.foundation.block.hitbox.VoxelShapeBuilder.ShapeAccumulator;
import com.quattage.mechano.foundation.block.hitbox.VoxelShapeBuilder.TemporaryShape;
import com.tterrag.registrate.providers.RegistrateGenericProvider;
import com.tterrag.registrate.providers.RegistrateGenericProvider.GeneratorData;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;


public class HitboxDataProvider {

    public static void generate(RegistrateGenericProvider provider) {
        provider.add(Provider::new);
    }

    public static class Provider extends SimpleDataProvider {
        
        private final Path inputDir;
        private final String generatedName;

        @SuppressWarnings("deprecation")
        protected Provider(GeneratorData data) {
            super(data);
            this.inputDir = data.output()
                .getOutputFolder(PackOutput.Target.DATA_PACK)
                .getParent().getParent().getParent()
                .resolve("main/resources/data/" + Mechano.ID + "/hitboxes");

            this.generatedName = WordUtils.capitalize(Mechano.ID) + "Hitboxes";
        }

        @Override
        public Path getOutputDirectory(GeneratorData data) {
            return data.output()
                .getOutputFolder(PackOutput.Target.DATA_PACK)
                .getParent().getParent().getParent()
                .resolve("main/java/com/quattage/" + Mechano.ID + "/foundation/block/hitbox/" + generatedName + ".java");
        }

        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return CompletableFuture.runAsync(() -> generate(output));
        }

        @Override
        public String getName() {
            return "hitbox";
        }

        @Override
        @SuppressWarnings("unchecked")
        public void generate(CachedOutput output) {

            long startTime = System.currentTimeMillis();
            int count = 0;
            
            this.createDirectory(inputDir);
            List<Path> files = this.listFiles(inputDir);
            if(files == null) return;
            StringBuilder cls = this.generateClassFile(generatedName);

            Object2ObjectOpenHashMap<String[], ShapeAccumulator> hitboxes = new Object2ObjectOpenHashMap<>();
            TemporaryShape shape = new TemporaryShape();

            for(Path path : files) {
                try(Reader reader = Files.newBufferedReader(path)) {
                    String name = path.getFileName().toString();
                    readModel(
                        cls, hitboxes, name.substring(0, name.lastIndexOf(".")), shape,
                        (List<Map<String, Map<String, Object>>>)(Mechano.GSON.fromJson(reader, Map.class).get("elements"))
                    );
                } catch(IOException e) {
                    Mechano.LOGGER.error("Error reading file '" + path + "'");
                    e.printStackTrace();
                }
                count++;
            }
            shape.dispose();

            this.defineFields(cls, hitboxes);

            cls.append("\n}");
            try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputDir))) {
                pw.write(cls.toString());
            } catch(IOException e) {
                Mechano.LOGGER.error("Error creating class definition for '" + generatedName + ".java'");
                e.printStackTrace();
            }
            long elapsed = (System.currentTimeMillis() - startTime);
            Mechano.LOGGER.info("Generated " + count + " hitboxes in " + elapsed + "ms");
        }

        private StringBuilder generateClassFile(String generatedName) {
            StringBuilder cls = new StringBuilder();
            cls.append("package " + "com.quattage." + Mechano.ID + ".foundation.block.hitbox;\n\n");
            writeImport(cls, Generated.class, true);
            writeImport(cls, VoxelShapeBuilder.class, false);
            writeImport(cls, HitboxRepresentable.class, false);
            writeImport(cls, LazyRotatableHitbox.class, false);
            writeImport(cls, HitboxStateTree.class, false);
            writeImport(cls, VoxelShape.class, false);
            writeImport(cls, IEventBus.class, true);
            cls.append("@SuppressWarnings(\"unused\")\n");
            cls.append("@Generated(\"" + this.getClass().getCanonicalName() + "\")\n");
            cls.append("public class " + generatedName + " {\n\n");
            cls.append("\tpublic void register(IEventBus modBus) {}\n\n");
            return cls;
        }

        private void writeImport(StringBuilder cls, Class<?> imp, boolean doubleBreak) {
            cls.append("import " + imp.getCanonicalName() + (doubleBreak ? ";\n\n" : ";\n"));
        }


        private void readModel(StringBuilder cls, Object2ObjectOpenHashMap<String[], ShapeAccumulator> hitboxes, String filename, TemporaryShape shape, List<Map<String, Map<String, Object>>> modelFile) {
            if(!filename.matches("[a-z1-9_.]+")) {
                Mechano.LOGGER.error("Error reading file '" + filename + "' - This file contains characters that are not allowed! (Non [a-z1-9_.])");
                return;
            }
            String[] tokens = filename.split("\\.");
            if(tokens.length > 2) {
                Mechano.LOGGER.error("Error reading file '" + filename + "' - This file has more than one seperating token (indicated by '.') - This is currently not supported, so this hitbox has been skipped.");
                return;
            }
            ShapeAccumulator accumulator = new ShapeAccumulator();
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
                shape.clear();
                return;
            }
            if(accumulator.isEmpty()) {
                Mechano.LOGGER.warn("Skipped generation of hitbox '" + filename + "' - The resulting shape was empty.");
                shape.clear();
                return;
            }
            hitboxes.put(tokens, accumulator);
            shape.clear();
        }



        private void defineFields(StringBuilder cls, Object2ObjectOpenHashMap<String[], ShapeAccumulator> hitboxes) {

            final Set<String[]> writtens = new HashSet<>();
            Map<String, ShapeAccumulator> aliases;

            for(Map.Entry<String[], ShapeAccumulator> hitbox : hitboxes.entrySet()) {

                String[] token = hitbox.getKey();
                if(writtens.contains(token)) continue;
                ShapeAccumulator shape = hitbox.getValue();
                if(shape.isEmpty()) continue;

                String fieldName = token[0].toUpperCase();

                if(token.length == 1) {
                    writtens.add(token);
                    writeLRH(cls, fieldName, shape, 1);
                    continue;
                }

                aliases = new HashMap<>();

                for(Map.Entry<String[], ShapeAccumulator> otherHitbox : hitboxes.entrySet()) {
                    String[] otherToken = otherHitbox.getKey();
                    if(!token[0].equals(otherToken[0])) continue;
                    if(token[1].equals(otherToken[1])) continue;
                    if(writtens.contains(otherToken)) continue;
                    aliases.put(token[1], shape);
                    aliases.put(otherToken[1], otherHitbox.getValue());
                    writtens.add(token);
                    writtens.add(otherToken);
                }

                if(aliases.size() > 1) {
                    cls.append("\tpublic static final HitboxStateTree " + fieldName + " = new HitboxStateTree(\n");
                    for(Map.Entry<String, ShapeAccumulator> alias : aliases.entrySet()) {
                        cls.append("\t\t\"" + alias.getKey() + "\", new LazyRotatableHitbox(\n\t\t\tVoxelShapeBuilder\n");
                        alias.getValue().forEachBox((box, first)-> { writeBox(cls, box, first, 3); });
                        cls.append("\t\t\t.optimize().make()\n\t\t),\n");
                    }
                    cls.setLength(cls.length() - 2);
                    cls.append("\n\t);\n\n");
                } else {
                    String lonelyName = String.join("_", token).toUpperCase();
                    Mechano.LOGGER.warn("Hitbox named '" + String.join(".", token) 
                        + ".json' had redundant tokens, so its resulting field name was combined into '" + lonelyName + "'");
                    writeLRH(cls, lonelyName, shape, 1);
                }
            }
        }

        

        private void writeLRH(StringBuilder cls, String fieldName, ShapeAccumulator shape, int indent) {
            String header = makeIndent(indent);
            cls.append(header + "public static final LazyRotatableHitbox " + fieldName + " = new LazyRotatableHitbox(\n" + header + "\tVoxelShapeBuilder\n");
            shape.forEachBox((box, first)-> { writeBox(cls, box, first, indent + 1); });
            cls.append(header + "\t.optimize().make()\n" + header + ");\n\n");
        }

        private void writeBox(StringBuilder cls, TemporaryShape shape, boolean first, int indent) {
            cls.append(makeIndent(indent) + (first ? ".start(" : ".addBox(") + shape.minX() + ", " + shape.minY() + ", " + shape.minZ() + ", " + shape.maxX() + ", " + shape.maxY() + ", " + shape.maxZ() + ")\n");
        }

        private String makeIndent(int indent) {
            String out = "";
            for(int x = 0; x < indent; x++)
                out += "\t";
            return out;
        }
    }
}
