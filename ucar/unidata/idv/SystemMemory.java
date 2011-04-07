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

    /** The INSTANCE. */
    private static final SystemMemory INSTANCE = new SystemMemory();

    /** The total available system memory in bytes. */
    private final long memory;

    /**
     * Private constructor.
     */
    private SystemMemory() {
        memory            = getMemoryInternal();
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
     * Actually determining memory.
     *
     * @return the total available system memory
     */
    private long getMemoryInternal() {
        long mem = -1;

        try {
            final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            final Method                m      = osBean.getClass().getMethod("getTotalPhysicalMemorySize");

            m.setAccessible(true);
            mem = (Long) m.invoke(osBean);
        } catch (Exception ignore) {}

        return mem;
    }
}
