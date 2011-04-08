package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import org.junit.Test;

/**
 * The SystemMemoryTest. Does not do much at this point.
 */
public class SystemMemoryTest {
	
	/**
	 * Test get memory.
	 */
	@Test
	public void testGetMemory() {
		SystemMemory.getMemory();
	}

	/**
	 * Test is memory available.
	 */
	@Test
	public void testIsMemoryAvailable() {
		SystemMemory.isMemoryAvailable();
	}
}
