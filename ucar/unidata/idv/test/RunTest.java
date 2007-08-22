/*
 * $Id: RunTest.java,v 1.10 2005/05/13 18:31:09 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.test;



import java.io.*;

import java.util.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.PatternFileFilter;


/**
 * Runs through a set of test bundles, generating test image archives, etc.
 *
 *
 * @author IDV development team
 */
public class RunTest {

    /**
     * Dummy ctor for doclint
     */
    public RunTest() {}

    /**
     *  Find out where this test directory is
     *
     * @return The test dir
     */
    public static File getTestDirectory() {
        String cp = System.getProperty("java.class.path");
        if (cp == null) {
            System.err.println("No classpath found");
            return null;
        }
        StringTokenizer st = new StringTokenizer(cp, ":");
        while (st.hasMoreElements()) {
            String path = (String) st.nextElement();
            File   dir  = new File(path + "/ucar/unidata/idv/test");
            if ( !dir.exists()) {
                continue;
            }
            return dir;
        }
        return null;
    }

    /**
     * The main
     *
     * @param args Command line args
     */
    public static void main(String[] args) {
        File testDirectory = null;
        if (args.length > 0) {
            testDirectory = new File(args[0]);
        } else {
            testDirectory = getTestDirectory();
        }

        if (testDirectory == null) {
            System.err.println("No  testDirectory given");
            return;
        }

        File[] files = testDirectory.listFiles(
                           (FileFilter) new PatternFileFilter(
                               ".*\\.xidv$", false));
        if (files.length == 0) {
            System.err.println("No .xidv files found in: " + testDirectory);
            return;
        }
        boolean inError = false;

        for (int i = 0; i < files.length; i++) {
            try {
                File   f         = files[i];
                String imageName = f.getName();
                imageName =
                    imageName.substring(0, imageName.lastIndexOf("."));
                String   newImageName = "new_" + imageName;
                String[] cmd          = new String[] {
                    "java", "ucar.unidata.idv.DefaultIdv", "-test",
                    f.getPath(), "-testimage", newImageName
                };
                System.err.println(f.getPath());
                Process p = Runtime.getRuntime().exec(cmd, null,
                                                      testDirectory);
                p.waitFor();

                System.err.println(getOutput(p));

                if (p.exitValue() == 0) {
                    if ( !checkImage(testDirectory, imageName + ".jpg",
                                     newImageName)) {
                        inError = true;
                    }
                } else {
                    inError = true;
                    System.err.println("Error evaluating " + f.getPath());
                    System.err.println(getOutput(p));
                }
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                System.exit(1);
            }
        }
        if (inError) {
            System.exit(1);
        }
        System.err.println("No errors");
    }



    /**
     * Read the output from the given process
     *
     * @param p The process
     * @return its output
     *
     * @throws Exception
     */
    public static String getOutput(Process p) throws Exception {
        InputStream stderr = p.getErrorStream();
        String      errstr = IOUtil.readContents(stderr);
        stderr.close();

        InputStream stdout = p.getInputStream();
        String      outstr = IOUtil.readContents(stdout);
        stdout.close();

        return errstr + outstr;
    }

    /**
     * Compare the  given image
     *
     * @param testDirectory The dir
     * @param imageName The old image
     * @param newImageName The new image
     * @return are images same
     *
     * @throws Exception
     */
    public static boolean checkImage(File testDirectory, String imageName, String newImageName)
            throws Exception {
        File image = new File(testDirectory.getPath() + File.separator
                              + imageName);
        File newImage = new File(testDirectory.getPath() + File.separator
                                 + newImageName);
        if ( !image.exists()) {
            return true;
        }
        if ( !newImage.exists()) {
            return true;
        }

        String[] cmd = { "diff", image.getPath(), newImage.getPath() };
        Process  p   = Runtime.getRuntime().exec(cmd, null);
        p.waitFor();
        if (p.exitValue() == 0) {
            return true;
        }
        System.err.println("images differ: " + image.getPath() + "\n"
                           + "               " + newImage.getPath());
        return false;
    }


}








