package ucar.unidata.idv;

import org.junit.Test;


public class SystemMemoryTest {
	
    @Test
    public void testGetMemory() {
    	System.out.println(SystemMemory.getMemory());
    }
    
    @Test
    public void testIsMemoryAvailable() {
    	System.out.println(SystemMemory.isMemoryAvailable());
    }
    
    
}
