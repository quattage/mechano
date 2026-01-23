package com.quattage.mechano.infrastructure.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

@EventBusSubscriber
public class MechanoGameTests {

    @SubscribeEvent
    public static void appendGametests(RegisterGameTestsEvent evt) {
        try { evt.register(MechanoGameTests.class.getMethod("collectTests")); }
        catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @GameTestGenerator
    public static Collection<TestFunction> collectTests() {
        Stream<Class<?>> gameTestClasses = MechanoGameTests.getTestClasses(MechanoGameTests.class.getPackageName());
        return gameTestClasses.map(Class::getDeclaredMethods)
            .flatMap(Stream::of)
            .map(MechanoGameTests::makeTest)
            .filter(Objects::nonNull)
            .toList();
    }

    private static Stream<Class<?>> getTestClasses(String dirName) {
        Optional<? extends ModContainer> mechano = ModList.get().getModContainerById(Mechano.ID);
        String path = dirName.replace(".", "/");
        if(!mechano.isPresent()) return Stream.empty();
        IModInfo info = mechano.get().getModInfo();
        ModFileScanData scan = info.getOwningFile().getFile().getScanResult();
        return scan.getClasses().stream()
            .map(cd -> cd.clazz().getInternalName())
            .filter(className -> className.startsWith(path))
            .map(className -> className.replace("/", "."))
            .<Class<?>>map(className -> {
                try { return Class.forName(className); } 
                catch (ClassNotFoundException e) { 
                    e.printStackTrace(); 
                    Mechano.LOGGER.error("Couldn't find gametest class '" + className + "'"); 
                    return null; 
                }
            })
            .filter(Objects::nonNull)
            .filter(clazz -> clazz.isAnnotationPresent(MechanoTestHolder.class));
    }

    private static @Nullable TestFunction makeTest(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        MechanoTestHolder gth = clazz.getAnnotation(MechanoTestHolder.class);
        if(gth == null) return null;
        GameTest gt = method.getAnnotation(GameTest.class);
        if(gt == null) return null;

        String name = method.getName().toLowerCase();
        if(!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException("GameTest method '" + name + "' must be static!");
		if(method.getReturnType() != void.class)
			throw new IllegalArgumentException("GameTest method '" + name + "' must return void! (got " + method.getReturnType() + ")");
		if(method.getParameterCount() != 1 || method.getParameterTypes()[0] != MechanoGameTestHelper.class)
			throw new IllegalArgumentException("GameTest Method '" + name + "' may only take one MechanoGameTestHelper parameter!");

        Repeat repeat = method.getAnnotation(Repeat.class);
        int repeats = repeat == null ? 1 : Math.max(0, repeat.iterations());

        String templateName = gt.template();
        if(templateName == null || templateName.isBlank()) templateName = "empty";
        String structureName = gth.namespace() + ":gametest/" + templateName;
        String batchName = gt.batch();
        if(batchName == null || batchName.isBlank() || "defaultBatch".equals(batchName)) 
            batchName = clazz.getSimpleName().toLowerCase();
        Rotation rotation = StructureUtils.getRotationForRotationSteps(gt.rotationSteps());
        TestFunction result = new TestFunction(batchName, name, structureName, rotation,
            gth.useTimeout() ? (gt.timeoutTicks() * repeats) : Integer.MAX_VALUE, 
            gt.setupTicks(),
            gt.required(), gt.manualOnly(),
            gt.requiredSuccesses(), gt.attempts(),
            gt.skyAccess(), 
            (Consumer<GameTestHelper>)MechanoGameTests.encapsulate(method, repeats)
        );
        // Mechano.LOGGER.debug("Registered test " + result.testName() + " to batch '" + result.batchName() + "' using template '" + result.structureName() + "' (" + repeats + " repeats)");
        return result;
    }

    private static Consumer<GameTestHelper> encapsulate(Method method, int repeats) {
        return gth -> {
            MechanoGameTestHelper mgth = gth instanceof MechanoGameTestHelper mgthh ? mgthh : MechanoGameTestHelper.of(gth);
            for(int x = 0; x < repeats; x++) {
                try { 
                    mgth.getGrid().load();
                    method.invoke(null, mgth); 
                    mgth.getGrid().unload();
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                } 
            }
        };
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MechanoTestHolder {
        String namespace() default Mechano.ID;
        boolean useTimeout() default true;
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Repeat {
        int iterations() default 1;
    }
}
