package com.quattage.mechano.infrastructure;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;

import org.openjdk.jol.info.ClassLayout;

public class ReflectionWizard {

    private static int MAX_RECURSE = 3;

    public static String estimateFootprint(Object instance) {
        String out = "";
        long total = 0L;
        for(Field field : instance.getClass().getDeclaredFields()) {
            if(!ReflectionWizard.isAnalyzable(field)) continue;
            field.setAccessible(true);
            String member = "\n  │ " + field.getName() + ": ";
            Object subInstance = null;
            try { subInstance = field.get(instance); }
            catch (Exception e) { e.printStackTrace(); total += 32L; out += (member + "null, 32 bytes"); continue; }
            if(subInstance == null) { total += 8L; out += (member + "null, 8 bytes"); continue; }
            long size = ReflectionWizard.deepSize(subInstance);
            total += size;
            out += (member + "(" + subInstance.getClass().getSimpleName() + "), " + size + " bytes");
        }
        long rtt = Runtime.getRuntime().totalMemory();
        double percent = ((double)total / (double)rtt);
        return out += "\n  ┕ Total: " + total + " bytes (" + percent + " % of heap space)";
    }

    public static long deepSize(Object root) { return ReflectionWizard.deepSize(root, 0, new IdentityHashMap<>()); }
    private static long deepSize(Object obj, int depth, Map<Object, Boolean> visited) {
        if(obj == null || depth > ReflectionWizard.MAX_RECURSE || visited.put(obj, Boolean.TRUE) != null) return 0;
        Class<?> clazz = obj.getClass();
        if(!ReflectionWizard.isAnalyzable(clazz)) return 0;
        long size = ClassLayout.parseInstance(obj).instanceSize();
        if(clazz.isArray()) {
            if(!clazz.getComponentType().isPrimitive()) {
                for(int x = 0; x < ((Object[])obj).length; x++)
                    size += ReflectionWizard.deepSize(((Object[])obj)[x], depth + 1, visited);
            }
            return size;
        }
        while(clazz != null) {
            for(Field field : clazz.getDeclaredFields()) {
                if(!ReflectionWizard.isAnalyzable(field)) continue;
                try { Object child = field.get(obj); size += ReflectionWizard.deepSize(child, depth + 1, visited); } 
                catch (IllegalAccessException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        return size;
    }

    public static boolean isAnalyzable(Field field) {
        return field != null && !field.getType().isPrimitive() 
            && !Modifier.isStatic(field.getModifiers()) 
            && field.trySetAccessible()
            && field.getAnnotation(DoNotAnalyze.class) == null
            && field.getDeclaringClass().getAnnotation(DoNotAnalyze.class) == null;
    }

    public static boolean isAnalyzable(Object obj) {
        return obj != null && ReflectionWizard.isAnalyzable(obj.getClass());
    }

    public static boolean isAnalyzable(Class<?> clazz) {
        return clazz != null && !Proxy.isProxyClass(clazz) 
            && !clazz.isHidden()
            && !clazz.getPackageName().contains("net.minecraft")
            && !clazz.getName().contains("$$Lambda") 
            && clazz.getAnnotation(DoNotAnalyze.class) == null;
    }


    private static Class<?> tryGetClass(String name) {
        Class<?> clazz = null;
        try { clazz = Class.forName(name); } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return clazz;
    }

    private static File tryGetFile(URL url) {
        File out = null;
        try { out = new File(url.toURI()); } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Couldn't get file url for " + url);
        }
        return out;
    }

    @Target( { ElementType.TYPE, ElementType.FIELD } )
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DoNotAnalyze {
        
    }

    // we're not gonna need the stackwalker where we're going
    public static void whatCalledMe() {
        try{ throw new StackTraceInvokerException(); }
        catch (StackTraceInvokerException e) { e.printStackTrace(); }
    }
    public static class StackTraceInvokerException extends RuntimeException {}
}
