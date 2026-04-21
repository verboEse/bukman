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
     * Performs a privileged action while accessing {@link Unsafe}.
     */
    @SuppressWarnings("removal")
    public static void doPrivilegedWithUnsafe(Consumer<Unsafe> privilegedAction) {
        try {
            privilegedAction.accept((Unsafe) theUnsafeFieldMethodHandle.invoke());
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
