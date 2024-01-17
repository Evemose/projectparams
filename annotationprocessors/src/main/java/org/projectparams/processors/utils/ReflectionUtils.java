package org.projectparams.processors.utils;

import sun.misc.Unsafe;

import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

// TODO: maybe migrate from Unsafe when project is working if possible
@SuppressWarnings("deprecation")
public class ReflectionUtils {
    /** hehe unsafe lol **/
    private static final Unsafe WHO_CARES_IF_ITS_UNSAFE = becomeUnsafe();

    /** silly little offset for doing silly bad things. Empty if we got caught **/
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<Long> SILLY_OFFSET = becomeSilly();

    /** get silly little offset for doing silly bad things **/
    private static Optional<Long> becomeSilly() {
        Long g;
        try {
            g = getSillyFieldOffset();
            return Optional.of(g);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    /** Silly override field will help us do silly bad things **/
    @SuppressWarnings("deprecation")
    private static long getSillyFieldOffset() throws Throwable {
        Field overrideField = null;
        Throwable saved = null;
        try {
            overrideField = AccessibleObject.class.getDeclaredField("override");
        } catch (Throwable t) {
            saved = t;
        }

        if (overrideField != null) {
            return WHO_CARES_IF_ITS_UNSAFE.objectFieldOffset(overrideField);
        }

        // I snatched it from lombok. They say it might brake in the future, but let`s hope for better
        try {
            return WHO_CARES_IF_ITS_UNSAFE.objectFieldOffset(SillyFake.class.getDeclaredField("override"));
        } catch (Throwable t) {
            throw saved;
        }
    }

    private ReflectionUtils() {
        throw new UnsupportedOperationException();
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = null;
        var originalClass = clazz;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                clazz = clazz.getSuperclass();
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldError("%s::%s".formatted(Objects.requireNonNull(originalClass).getName(),
                    fieldName));
        }
        setAccessible(field);
        return field;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = null;
        var originalClass = clazz;
        while (clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                clazz = clazz.getSuperclass();
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        if (method == null) {
            throw new NoSuchMethodError("%s::%s".formatted(Objects.requireNonNull(originalClass).getName(),
                    methodName));
        }
        setAccessible(method);
        return method;
    }

    public static Optional<Object> getStaticFieldValue(Class<?> clazz, String fieldName) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return Optional.ofNullable(field.get(null));
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return Optional.empty();
        }
    }

    public static <T extends AccessibleObject> void setAccessible(T object) {
        SILLY_OFFSET.ifPresentOrElse(
                offset -> WHO_CARES_IF_ITS_UNSAFE.putBoolean(object, offset, true),
                () -> object.setAccessible(true)
        );
    }

    /** Who cares if it`s unsafe? **/
    private static Unsafe becomeUnsafe() {
        var unsafe = getStaticFieldValue(Unsafe.class, "theUnsafe");
        return unsafe.map(Unsafe.class::cast).orElseThrow();
    }

    @SuppressWarnings("unused")
    static class SillyFake {
        boolean override;
        Object accessCheckCache;
    }

//    currently unused
//    private static long getFieldOffset(Class<?> clazz, String fieldName) {
//        return WHO_CARES_IF_ITS_UNSAFE.objectFieldOffset(getField(clazz, fieldName));
//    }

    private static long getFirstFieldOffset() {
        return WHO_CARES_IF_ITS_UNSAFE.objectFieldOffset(getField(Parent.class, "first"));
    }


//    currently unused
//    public static void setVolatile(Object object, String fieldName, boolean value) {
//        var offset = getFieldOffset(object.getClass(), fieldName);
//        WHO_CARES_IF_ITS_UNSAFE.putBooleanVolatile(object, offset, value);
//    }

    public static void setFirstFieldVolatile(Object object, boolean value) {
        WHO_CARES_IF_ITS_UNSAFE.putBooleanVolatile(object, getFirstFieldOffset(), value);
    }

    @SuppressWarnings("unused")
    private static class Parent {
        boolean first;
        static final Object staticObj = OutputStream.class;
        volatile Object second;
        private static volatile boolean staticSecond;
        private static volatile boolean staticThird;
    }
}

