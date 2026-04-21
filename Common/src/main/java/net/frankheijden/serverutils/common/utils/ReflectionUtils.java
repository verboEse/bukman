package net.frankheijden.serverutils.common.utils;

import dev.frankheijden.minecraftreflection.Reflection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class ReflectionUtils {

    private static MethodHandle theUnsafeFieldMethodHandle;

    static {
        try {
            theUnsafeFieldMethodHandle = MethodHandles.lookup().unreflectGetter(Reflection.getAccessibleField(
                    Unsafe.class,
                    "theUnsafe"
            ));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private ReflectionUtils() {}

    /**
     * Performs an action while accessing {@link Unsafe}.
     */
    @SuppressWarnings("removal")
    public static void doWithUnsafe(Consumer<Unsafe> action) {
        try {
            action.accept((Unsafe) theUnsafeFieldMethodHandle.invoke());
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
