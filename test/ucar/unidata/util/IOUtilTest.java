/*
 *
 * Copyright  1997-2019 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.util;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class IOUtilTest {

	@Test
	public void testJoinDirStringArray() {
		final String FS = System.getProperty("file.separator");
		assertEquals("Junit error", "foo" + FS + "bar",IOUtil.joinDir("foo", "bar"));
		assertEquals("Junit error", "foo" + FS + "bar" + FS + "foobar",IOUtil.joinDirs("foo", "bar", "foobar"));
		assertEquals("Junit error",  FS + "foobar",IOUtil.joinDir(new File(""), "foobar"));
	}
}
