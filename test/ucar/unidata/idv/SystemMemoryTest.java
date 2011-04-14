package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import java.util.logging.Logger;

import org.junit.Test;

/**
 * The SystemMemoryTest. Does not do much at this point.
 */
public class SystemMemoryTest {
	
	/**
	 * Test get memory.
	 */
	@Test
	public void test() {
		log(SystemMemory.DEFAULT_MEMORY + "");
		log(SystemMemory.isMemoryAvailable() + "");
		log(SystemMemory.getMemory() + "");
		log(SystemMemory.getMemoryInMegabytes() + "");
		log(SystemMemory.getMaxMemoryInMegabytes() + "");
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
