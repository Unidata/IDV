package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

public class SystemMemoryManagerTest {
    @Test
    public void testGetTotalMemory() {
        if (SystemMemoryManager.isMemoryAvailable()) {
            assertTrue("should always be > than low water mark when memory is available",
                       SystemMemoryManager.getTotalMemory() >= SystemMemoryManager.MINIMUM_MEMORY);
        } else {
            assertEquals("should always be -1 when memory is not available", -1, SystemMemoryManager.getTotalMemory());
        }

        log("SystemMemoryManager.getTotalMemory() " + SystemMemoryManager.getTotalMemory());
        log("SystemMemoryManager.getDefaultMemory() " + SystemMemoryManager.getDefaultMemory());
        log("SystemMemoryManager.isMemoryAvailable() " + SystemMemoryManager.isMemoryAvailable());
    }

    @Test
    public void testGetDefaultMemory() {
        assertTrue("should always be > than low water mark",
                   SystemMemoryManager.getDefaultMemory() >= SystemMemoryManager.MINIMUM_MEMORY);
    }

    @Test
    public void testConvertToNumber() {
        if (SystemMemoryManager.isMemoryAvailable()) {
            assertEquals("0% should always be 512", 512, SystemMemoryManager.convertToNumber(0));
            assertEquals("100% should always be total memory", SystemMemoryManager.getTotalMemory(),
                         SystemMemoryManager.convertToNumber(100));
        } else {
            assertEquals("Should always be -1", -1, SystemMemoryManager.convertToNumber(0));
            assertEquals("Should always be -1", -1, SystemMemoryManager.convertToNumber(100));
        }
    }

    @Test
    public void testConvertToPercent() {
        if (SystemMemoryManager.isMemoryAvailable()) {

            // Deal with special case
            if (SystemMemoryManager.getTotalMemory() == SystemMemoryManager.MINIMUM_MEMORY) {
                assertEquals("512 should always be 100%", 100, SystemMemoryManager.convertToPercent(512), 0.001);
                assertEquals("total memory should always be 100%",
                             SystemMemoryManager.convertToPercent(SystemMemoryManager.getTotalMemory()), 100, 0.001);
            } else {
                assertEquals("512 should always be 0%", 0, SystemMemoryManager.convertToPercent(512), 0.001);
                assertEquals("total memory should always be 100%",
                             SystemMemoryManager.convertToPercent(SystemMemoryManager.getTotalMemory()), 100, 0.001);
            }
        } else {
            assertEquals("Should always be -1", -1, SystemMemoryManager.convertToPercent(0), 0.001);
            assertEquals("Should always be -1", -1, SystemMemoryManager.convertToPercent(100), 0.001);
        }
    }

    @Test
    public void testCheckAndRepair() {
        if (SystemMemoryManager.isMemoryAvailable()) {
            assertEquals("Should always be 512", 512, SystemMemoryManager.checkAndRepair(Long.MIN_VALUE));
            assertEquals("Should always be total memory", SystemMemoryManager.getTotalMemory(),
                         SystemMemoryManager.checkAndRepair(Long.MAX_VALUE));
        } else {
            assertEquals("Should always be 512", 512, SystemMemoryManager.checkAndRepair(Long.MIN_VALUE));
            assertEquals("Should always be total memory", Long.MAX_VALUE,
                         SystemMemoryManager.checkAndRepair(Long.MAX_VALUE));
        }
    }

    /**
     * Just for logging convenience.
     *
     * @param o the o
     */
    private static void log(final Object o) {
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(o.toString());
    }
}
