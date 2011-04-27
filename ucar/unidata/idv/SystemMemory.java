package ucar.unidata.idv;

//~--- JDK imports ------------------------------------------------------------

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * Class to determine system memory characteristics. Thanks to the McIDAS-V team and John Beavers
 * for hints on how to do this.
 */
public final class SystemMemory {

    /** A sensible default for the IDV, in case all else fails. */
    public final static int DEFAULT_MEMORY = 512;

    /** The INSTANCE. */
    private static final SystemMemory INSTANCE = new SystemMemory();

    /** Max heap for a 32 bit OS */
    private static final long OS_32_MAX = 1536 * 1024 * 1024;

    /** The total available system memory in bytes. */
    private final long memory;

    /**
     * Private constructor.
     * See this discussion: http://stackoverflow.com/questions/1190837/java-xmx-max-memory-on-system
     * Post condition: memory will either be -1, OS_32_MAX, or the result of getTotalPhysicalMemorySize
     */
    private SystemMemory() {
        long mem = -1;

        try {
            final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            final Method                m      = osBean.getClass().getMethod("getTotalPhysicalMemorySize");

            m.setAccessible(true);
            mem = (Long) m.invoke(osBean);
        } catch (Exception ignore) {}

        // Are we running on a 64 bit OS?
        final boolean is64 = System.getProperty("os.arch").indexOf("64") >= 0;

        this.memory = (!is64 && (mem > OS_32_MAX))
                      ? OS_32_MAX
                      : mem;
    }

    /**
     * Gets total available system memory in bytes. If none could be found
     * because the JVM does not implement this functionality, this method will
     * return -1. Probably should call {@link #isMemoryAvailable} first.
     *
     * @return the memory in bytes
     */
    public static long getMemory() {
        return INSTANCE.memory;
    }

    /**
     * Checks if is memory is available for this JVM.
     *
     * @return true, if memory is available
     */
    public static boolean isMemoryAvailable() {
        return INSTANCE.memory > 0;
    }

    /**
     * Same as {@link #getMemory()}, but in megabytes for convenience.
     *
     * @return the memory in megabytes
     */
    public static long getMemoryInMegabytes() {
        return isMemoryAvailable()
               ? INSTANCE.memory / 1024 / 1024
               : INSTANCE.memory;
    }

    /**
     * Another convenience method, returns 80% of {@link #getMemoryInMegabytes()}.
     */
    public static long getMaxMemoryInMegabytes() {
        return isMemoryAvailable()
               ? Math.round(getMemoryInMegabytes() * (80 / 100f))
               : INSTANCE.memory;
    }
}
