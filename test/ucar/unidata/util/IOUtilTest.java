package ucar.unidata.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class IOUtilTest {

	@Test
	public void testJoinDirStringArray() {
		final String FS = System.getProperty("file.separator");
		assertEquals("Junit error", "foo" + FS + "bar",IOUtil.joinDir("foo", "bar"));
		assertEquals("Junit error", "foo" + FS + "bar" + FS + "foobar",IOUtil.joinDirs("foo", "bar", "foobar"));
	}
}
