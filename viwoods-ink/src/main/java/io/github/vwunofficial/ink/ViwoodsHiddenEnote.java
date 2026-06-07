package io.github.vwunofficial.ink;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ViwoodsHiddenEnote {
    interface NativeInputSink {
        void onNativeInput(int x, int y, int pressureValue, float tilt, int toolType,
                           int action, int actionButton, int buttonState, long callbackNanos);
    }

    private static final String[] SETTING_CLASSES = {
            "android.os.enote.ENoteSetting",
            "android.p000os.enote.ENoteSetting",
            "android.p001os.enote.ENoteSetting",
            "android.p002os.enote.ENoteSetting"
    };

    private final ViwoodsInkLogger logger;
    private Class<?> settingClass;
    private Class<?> listenerClass;
    private Object setting;
    private Object listenerProxy;

    ViwoodsHiddenEnote(ViwoodsInkLogger logger) {
        this.logger = logger == null ? ViwoodsInkLogger.NONE : logger;
    }

    ViwoodsInkAvailability availability() {
        if (ensureSetting()) {
            return new ViwoodsInkAvailability(true, ViwoodsInkAvailability.Status.AVAILABLE,
                    settingClass.getName());
        }
        return new ViwoodsInkAvailability(false, ViwoodsInkAvailability.Status.ENOTE_SETTING_NOT_FOUND,
                "ENoteSetting was not found");
    }

    boolean setInputSink(final NativeInputSink sink) {
        if (!ensureSetting() || listenerClass == null) {
            return false;
        }
        try {
            listenerProxy = sink == null ? null : Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("onInput".equals(method.getName()) && args != null && args.length == 8) {
                                sink.onNativeInput(
                                        asInt(args[0]), asInt(args[1]), asInt(args[2]), asFloat(args[3]),
                                        asInt(args[4]), asInt(args[5]), asInt(args[6]), asInt(args[7]),
                                        SystemClock.elapsedRealtimeNanos());
                            }
                            return null;
                        }
                    });
            invoke("setWritingInputlistener", listenerProxy);
            logger.log("setWritingInputlistener " + (sink == null ? "null" : "proxy"));
            return true;
        } catch (Throwable t) {
            logger.log("setWritingInputlistener failed: " + t);
            return false;
        }
    }

    boolean configureBitmap(Bitmap bitmap, int orientation, int left, int top,
                            int jumpPointCount, int renderDelayCount) {
        if (!ensureSetting() || bitmap == null) {
            return false;
        }
        boolean ok = true;
        ok &= invokeQuiet("setWritingJavaBitmap", bitmap, orientation, left, top);
        ok &= invokeQuiet("setWritingJavaBackgroundBitmap", bitmap, orientation, left, top);
        ok &= invokeQuiet("setWritingInputJumpPointCount", jumpPointCount);
        ok &= invokeQuiet("setRenderWritingDelayCount", renderDelayCount);
        return ok;
    }

    boolean configureWritingBitmap(Bitmap bitmap, int orientation, int left, int top) {
        if (!ensureSetting() || bitmap == null) {
            return false;
        }
        return invokeQuiet("setWritingJavaBitmap", bitmap, orientation, left, top);
    }

    boolean configureBackgroundBitmap(Bitmap bitmap, int orientation, int left, int top) {
        if (!ensureSetting() || bitmap == null) {
            return false;
        }
        return invokeQuiet("setWritingJavaBackgroundBitmap", bitmap, orientation, left, top);
    }

    boolean setWritingEnabled(boolean enabled) {
        return invokeQuiet("setWritingEnabled", enabled);
    }

    boolean setPictureMode(int mode) {
        return invokeQuiet("setPictureMode", mode);
    }

    boolean onWritingStart() {
        boolean ok = invokeQuiet("onWritingStart");
        ok &= invokeQuiet("setWritingEnabled", true);
        return ok;
    }

    boolean onWritingEnd() {
        return invokeQuiet("onWritingEnd");
    }

    void release() {
        setInputSink(null);
        invokeQuiet("setWritingEnabled", false);
        invokeQuiet("onWritingEnd");
        invokeQuiet("releaseWritingJavaBitmap");
        invokeQuiet("releaseWritingJavaBackgroundBitmap");
    }

    ViwoodsInkRenderResult render(Rect screenRect) {
        if (screenRect == null || screenRect.isEmpty()) {
            return ViwoodsInkRenderResult.skippedEmptyRect(screenRect);
        }
        if (!ensureSetting()) {
            return ViwoodsInkRenderResult.failed(screenRect, -1L, "ENoteSetting was not found");
        }
        long start = SystemClock.elapsedRealtimeNanos();
        try {
            invoke("renderWriting", screenRect);
            return ViwoodsInkRenderResult.rendered(screenRect,
                    SystemClock.elapsedRealtimeNanos() - start);
        } catch (Throwable t) {
            logger.log("renderWriting failed: " + t);
            return ViwoodsInkRenderResult.failed(screenRect,
                    SystemClock.elapsedRealtimeNanos() - start, String.valueOf(t));
        }
    }

    private boolean ensureSetting() {
        if (setting != null) {
            return true;
        }
        for (String name : SETTING_CLASSES) {
            try {
                Class<?> cls = Class.forName(name);
                Object instance = cls.getMethod("getInstance").invoke(null);
                Class<?> iface = Class.forName(name.substring(0, name.lastIndexOf('.') + 1)
                        + "ENoteWritingInputListener");
                settingClass = cls;
                listenerClass = iface;
                setting = instance;
                logger.log("bound " + cls.getName());
                return true;
            } catch (Throwable ignored) {
                // Try the next package spelling observed in Viwoods ROM apps.
            }
        }
        return false;
    }

    private Object invoke(String name, Object... args) throws Exception {
        Method method = findMethod(name, args);
        method.setAccessible(true);
        return method.invoke(setting, args);
    }

    private boolean invokeQuiet(String name, Object... args) {
        try {
            invoke(name, args);
            return true;
        } catch (Throwable t) {
            logger.log(name + " failed: " + t);
            return false;
        }
    }

    private Method findMethod(String name, Object[] args) throws NoSuchMethodException {
        for (Method method : settingClass.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < types.length; i++) {
                if (!matches(types[i], args[i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static boolean matches(Class<?> type, Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        if (!type.isPrimitive()) {
            return type.isInstance(value);
        }
        return (type == boolean.class && value instanceof Boolean)
                || (type == int.class && value instanceof Integer)
                || (type == long.class && value instanceof Long)
                || (type == float.class && value instanceof Float)
                || (type == double.class && value instanceof Double);
    }

    private static int asInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static float asFloat(Object value) {
        return value instanceof Number ? ((Number) value).floatValue() : 0f;
    }
}
