package com.quattage.mechano.infrastructure;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

import org.openjdk.jol.info.ClassLayout;

public class MemoryAnalyzer {

    private static int MAX_RECURSE = 3;

    public static String estimateFootprint(Object instance) {
        String out = "";
        long total = 0L;
        for(Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if(field.getType().isPrimitive()) continue;
            String member = "\n  │ " + field.getName() + ": ";
            Object subInstance = null;
            try { subInstance = field.get(instance); }
            catch (Exception e) { e.printStackTrace(); total += 32L; out += (member + "null, 32 bytes"); continue; }
            if(subInstance == null) { total += 32L; out += (member + "null, 32 bytes"); continue; }
            long size = MemoryAnalyzer.deepSize(subInstance);
            total += size;
            out += (member + "(" + subInstance.getClass().getSimpleName() + "), " + size + " bytes");
        }
        return out += "\n  ┕ Total: " + total + " bytes";
    }

    public static long deepSize(Object root) {
        return MemoryAnalyzer.deepSize(root, 0, new IdentityHashMap<>());
    }

    private static long deepSize(Object obj, int depth, Map<Object, Boolean> visited) {
        if(obj == null || depth > MemoryAnalyzer.MAX_RECURSE)
            return 0;
        if(visited.put(obj, Boolean.TRUE) != null)
            return 0;
        long size = ClassLayout.parseInstance(obj).instanceSize();
        Class<?> cls = obj.getClass();
        if(cls.isArray()) {
            if(!cls.getComponentType().isPrimitive()) {
                int len = ((Object[])obj).length;
                for (int i = 0; i < len; i++)
                    size += MemoryAnalyzer.deepSize(((Object[])obj)[i], depth + 1, visited);
            }
            return size;
        }
        while(cls != null) {
            for(Field field : cls.getDeclaredFields()) {
                if(Modifier.isStatic(field.getModifiers())) continue;
                if(field.getType().isPrimitive()) continue;
                if(!field.trySetAccessible()) continue;
                try { Object child = field.get(obj); size += MemoryAnalyzer.deepSize(child, depth + 1, visited); } 
                catch (IllegalAccessException ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return size;
    }
}
