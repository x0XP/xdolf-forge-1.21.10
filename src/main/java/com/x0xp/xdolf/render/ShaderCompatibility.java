package com.x0xp.xdolf.render;

import java.lang.reflect.Method;

/** Optional OptiFine bridge kept reflection-only so Xdolf has no runtime dependency on OptiFine. */
public final class ShaderCompatibility {
    private static boolean resolved;
    private static Method optifineIsShaders;

    private ShaderCompatibility() {}

    public static boolean shadersActive() {
        if (!resolved) resolve();
        if (optifineIsShaders == null) return false;
        try {
            return Boolean.TRUE.equals(optifineIsShaders.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        try {
            optifineIsShaders = Class.forName("net.optifine.Config", false, ShaderCompatibility.class.getClassLoader())
                .getMethod("isShaders");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            optifineIsShaders = null;
        }
        resolved = true;
    }
}

