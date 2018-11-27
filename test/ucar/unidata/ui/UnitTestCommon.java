/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;


import junit.framework.TestCase;

import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.nio.charset.Charset;

import java.util.EnumSet;
import java.util.Set;


/**
 * Class description
 *
 */
public class UnitTestCommon extends TestCase {

    //////////////////////////////////////////////////
    // Static Constants

    /** _more_ */
    static final boolean DEBUG = false;

    /** _more_ */
    static protected final Charset UTF8 = Charset.forName("UTF-8");

    // Look for these to verify we have found the thredds root

    /** _more_ */
    static final String[] DEFAULTSUBDIRS = new String[] { "httpservices",
            "cdm", "tds", "opendap", "dap4" };

    /** _more_ */
    static public org.slf4j.Logger log;

    // NetcdfDataset enhancement to use: need only coord systems

    /** _more_ */
    static final Set<NetcdfDataset.Enhance> ENHANCEMENT =
        EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Static methods

    // Walk around the directory structure to locate
    // the path to the thredds root (which may not
    // be names "thredds").
    // Same as code in UnitTestCommon, but for
    // some reason, Intellij will not let me import it.

    /**
     * _more_
     *
     * @return _more_
     */
    static String locateThreddsRoot() {
        // Walk up the user.dir path looking for a node that has
        // all the directories in SUBROOTS.

        String path = System.getProperty("user.dir");

        // clean up the path
        path = path.replace('\\', '/');  // only use forward slash
        assert (path != null);
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        File prefix = new File(path);
        for (; prefix != null; prefix = prefix.getParentFile()) {  //walk up the tree
            int      found   = 0;
            String[] subdirs = prefix.list();
            for (String dirname : subdirs) {
                for (String want : DEFAULTSUBDIRS) {
                    if (dirname.equals(want)) {
                        found++;
                        break;
                    }
                }
            }
            if (found == DEFAULTSUBDIRS.length) {
                try {  // Assume this is it
                    String root = prefix.getCanonicalPath();
                    // clean up the root path
                    root = root.replace('\\', '/');  // only use forward slash
                    return root;
                } catch (IOException ioe) {}
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param pieces _more_
     * @param last _more_
     *
     * @return _more_
     */
    static protected String rebuildpath(String[] pieces, int last) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i <= last; i++) {
            buf.append("/");
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param dir _more_
     * @param clearsubdirs _more_
     */
    static public void clearDir(File dir, boolean clearsubdirs) {
        // wipe out the dir contents
        if ( !dir.exists()) {
            return;
        }
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (clearsubdirs) {
                    clearDir(f, true);  // clear subdirs
                    f.delete();
                }
            } else {
                f.delete();
            }
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // System properties

    /** _more_ */
    protected boolean prop_ascii = true;

    /** _more_ */
    protected boolean prop_diff = true;

    /** _more_ */
    protected boolean prop_baseline = false;

    /** _more_ */
    protected boolean prop_visual = false;

    /** _more_ */
    protected boolean prop_debug = DEBUG;

    /** _more_ */
    protected boolean prop_generate = true;

    /** _more_ */
    protected String prop_controls = null;

    /** _more_ */
    protected String title = "Testing";

    /** _more_ */
    protected String name = "testcommon";

    /** _more_ */
    protected String threddsroot = null;

    /** _more_ */
    protected String dtsServer = null;

    /** _more_ */
    protected String threddsServer = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Construct a UnitTestCommon 
     */
    public UnitTestCommon() {
        this("UnitTest");
    }

    /**
     * Construct a UnitTestCommon 
     *
     * @param name _more_
     */
    public UnitTestCommon(String name) {
        super(name);
        this.name = name;
        setSystemProperties();
        initPaths();
    }

    /**
     * _more_
     */
    protected void initPaths() {
        // Compute the root path
        this.threddsroot = locateThreddsRoot();
    }

    /**
     * Try to get the system properties
     */
    protected void setSystemProperties() {
        if (System.getProperty("nodiff") != null) {
            prop_diff = false;
        }
        if (System.getProperty("baseline") != null) {
            prop_baseline = true;
        }
        if (System.getProperty("nogenerate") != null) {
            prop_generate = false;
        }
        if (System.getProperty("debug") != null) {
            prop_debug = true;
        }
        if (System.getProperty("visual") != null) {
            prop_visual = true;
        }
        if (System.getProperty("ascii") != null) {
            prop_ascii = true;
        }
        if (System.getProperty("utf8") != null) {
            prop_ascii = false;
        }
        if (prop_baseline && prop_diff) {
            prop_diff = false;
        }
        prop_controls = System.getProperty("controls", "");
    }

    //////////////////////////////////////////////////
    // Accessor

    /**
     * _more_
     *
     * @param title _more_
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getThreddsroot() {
        return this.threddsroot;
    }

    //////////////////////////////////////////////////
    // Instance Utilities

    /**
     * _more_
     *
     * @param header _more_
     * @param captured _more_
     */
    public void visual(String header, String captured) {
        visual(header, captured, '-');
    }

    /**
     * _more_
     *
     * @param header _more_
     * @param captured _more_
     * @param marker _more_
     */
    public void visual(String header, String captured, char marker) {
        if ( !captured.endsWith("\n")) {
            captured = captured + "\n";
        }
        // Dump the output for visual comparison
        System.out.println("Testing " + getName() + ": " + header + ":");
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sep.append(marker);
        }
        System.out.println(sep.toString());
        System.out.print(captured);
        System.out.println(sep.toString());
    }

    /**
     * _more_
     *
     * @param tag _more_
     * @param baseline _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String compare(String tag, String baseline, String s) {
        try {
            // Diff the two print results
            Diff         diff = new Diff(tag);
            StringWriter sw   = new StringWriter();
            boolean      pass = !diff.doDiff(baseline, s, sw);
            return (pass
                    ? null
                    : sw.toString());
        } catch (Exception e) {
            System.err.println("UnitTest: Diff failure: " + e);
            return null;
        }
    }

    /**
     * _more_
     *
     * @param servlet _more_
     * @param svcname _more_
     * @param schema _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String findServer(String servlet, String svcname, String schema)
            throws Exception {
        if (servlet.startsWith("/")) {
            servlet = servlet.substring(1);
        }
        String svc = "http://" + svcname + "/" + servlet;
        if ( !checkServer(svc)) {
            throw new Exception("Server not reachable:" + svc);
        }
        // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
        return schema + "://" + svcname + "/" + servlet;
    }

    /**
     * _more_
     *
     * @param candidate _more_
     *
     * @return _more_
     */
    protected boolean checkServer(String candidate) {
        if (candidate == null) {
            return false;
        }
        /* requires httpclient4
                int savecount = HTTPSession.getRetryCount();
                HTTPSession.setRetryCount(1);
        */
        // See if the sourceurl is available by trying to get the DSR
        System.err.print("Checking for sourceurl: " + candidate);
        try {
            try (HTTPMethod method = HTTPFactory.Get(candidate)) {
                method.execute();
                String s = method.getResponseAsString();
                System.err.println(" ; found");
                return true;
            }
        } catch (IOException ie) {
            System.err.println(" ; fail");
            return false;
        } finally {
            // requires httpclient4            HTTPSession.setRetryCount(savecount);
        }
    }

    //////////////////////////////////////////////////
    // Static utilities

    // Copy result into the a specified dir

    /**
     * _more_
     *
     * @param path _more_
     * @param content _more_
     *
     * @throws IOException _more_
     */
    static public void writefile(String path, String content)
            throws IOException {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        FileWriter out = new FileWriter(f);
        out.write(content);
        out.close();
    }

    // Copy result into the a specified dir

    /**
     * _more_
     *
     * @param path _more_
     * @param content _more_
     *
     * @throws IOException _more_
     */
    static public void writefile(String path, byte[] content)
            throws IOException {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream out = new FileOutputStream(f);
        out.write(content);
        out.close();
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    static public String readfile(String filename) throws IOException {
        StringBuilder buf = new StringBuilder();
        File          xx  = new File(filename);
        if ( !xx.canRead()) {
            int x = 0;
        }
        FileReader     file = new FileReader(filename);
        BufferedReader rdr  = new BufferedReader(file);
        String         line;
        while ((line = rdr.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    static public byte[] readbinaryfile(String filename) throws IOException {
        FileInputStream stream = new FileInputStream(filename);
        byte[]          result = readbinaryfile(stream);
        stream.close();
        return result;
    }

    /**
     * _more_
     *
     * @param stream _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    static public byte[] readbinaryfile(InputStream stream)
            throws IOException {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[]                tmp   = new byte[1 << 16];
        for (;;) {
            int cnt;
            cnt = stream.read(tmp);
            if (cnt <= 0) {
                break;
            }
            bytes.write(tmp, 0, cnt);
        }
        return bytes.toByteArray();
    }

    // Properly access a dataset

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    static public NetcdfDataset openDataset(String url) throws IOException {
        return NetcdfDataset.acquireDataset(null, url, ENHANCEMENT, -1, null,
                                            null);
    }

    // Fix up a filename reference in a string

    /**
     * _more_
     *
     * @param text _more_
     * @param filename _more_
     *
     * @return _more_
     */
    static public String shortenFileName(String text, String filename) {
        // In order to achieve diff consistentcy, we need to
        // modify the output to change "netcdf .../file.nc {...}"
        // to "netcdf file.nc {...}"
        String fixed     = filename.replace('\\', '/');
        String shortname = filename;
        if (fixed.lastIndexOf('/') >= 0) {
            shortname = filename.substring(fixed.lastIndexOf('/') + 1,
                                           filename.length());
        }
        text = text.replaceAll(filename, shortname);
        return text;
    }

    /**
     * _more_
     *
     * @param t _more_
     */
    static public void tag(String t) {
        System.err.println(t);
        System.err.flush();
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    static protected String ncdumpmetadata(NetcdfFile ncfile)
            throws Exception {
        StringWriter sw = new StringWriter();
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if ( !ucar.nc2.NCdumpW.print(ncfile, "-unsigned", sw, null)) {
                throw new Exception("NcdumpW failed");
            }
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    static protected String ncdumpdata(NetcdfFile ncfile) throws Exception {
        StringWriter sw = new StringWriter();
        // Dump the databuffer
        sw = new StringWriter();
        try {
            if ( !ucar.nc2.NCdumpW.print(ncfile, "-vall -unsigned", sw,
                                         null)) {
                throw new Exception("NCdumpW failed");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

}
