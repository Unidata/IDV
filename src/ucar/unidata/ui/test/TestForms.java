/*
 * $Id: TestForms.java,v 1.31 2006/06/28 16:52:33 jeffmc Exp $
 *
 * Copyright  1997-2015 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui.test;

import junit.framework.TestCase;
import org.junit.Test;
import ucar.httpservices.HTTPException;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.Diff;
import ucar.unidata.util.Json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test HttpFormEntry
 *
 * @author IDV development team
 */


public class TestForms extends TestCase
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    // Pick a URL to test

    // Will send to esupport
    //static protected final String TESTURL = "http://www.unidata.ucar.edu/support/requestSupport.jsp";

    static protected final String TESTURL = "http://echo.httpkit.com";

    static protected final String FAKEBOUNDARY = "XXXXXXXXXXXXXXXXXXX";

    // Field values to use

    static final String DESCRIPTIONENTRY = "hello world";
    static final String NAMEENTRY = "Jim Jones";
    static final String EMAILENTRY = "jones@gmail.com";
    static final String ORGENTRY = "UCAR";
    static final String SUBJECTENTRY = "test httpformentry";
    static final String NAGENTRY = "I have no idea";
    static final String VERSIONENTRY = "1.0";
    static final String HARDWAREENTRY = "1.0";
    static final String SOFTWAREPACKAGEENTRY = "IDV";
    static final String BUNDLETEXT = "bundle";
    static final String EXTRATEXT = "whatever";

    //////////////////////////////////////////////////
    // Instance fields

    protected boolean pass = true;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestForms()
    {
        super("TestForms");
    }

    //////////////////////////////////////////////////

    @Test
    public void
    testSimple()
            throws Exception
    {
        pass = true;
        List<HttpFormEntry> form = buildForm(false);
        String[] results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if(results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if(results[0] == null) {
            pass |= process(results[1], false);
        }
        assertTrue("TestForms.simple: failed", pass);
    }

    @Test
    public void
    testMultipartForm()
            throws Exception
    {
        pass = false;
        List<HttpFormEntry> form = buildForm(true);
        String[] results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if(results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if(results[0] == null) {
            pass |= process(results[1], true);
        }
        assertTrue("TestForms.multipart: failed", pass);
    }

    /**
     * Compute list of HttpFormEntry's to test.
     */
    protected List<HttpFormEntry> buildForm(boolean multipart)
    {
        List<HttpFormEntry> entries = new ArrayList<>();
        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));

        HttpFormEntry descriptionEntry;
        HttpFormEntry nameEntry;
        HttpFormEntry emailEntry;
        HttpFormEntry orgEntry;

        entries.add(nameEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "fullName", "Name:", NAMEENTRY));
        entries.add(emailEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "emailAddress", "Your Email:", EMAILENTRY));
        entries.add(orgEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "organization", "Organization:", ORGENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_INPUT, "subject",
                "Subject:", SUBJECTENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_LABEL, "", NAGENTRY));
        entries.add(descriptionEntry =
                new HttpFormEntry(HttpFormEntry.TYPE_AREA, "description",
                        "Description:", DESCRIPTIONENTRY, 5, 30, true));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "submit",
                "", "Send Email"));

        entries.add(
                new HttpFormEntry(
                        HttpFormEntry.TYPE_HIDDEN, "softwarePackage", "",
                        SOFTWAREPACKAGEENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                "packageVersion", "",
                VERSIONENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "os", "",
                System.getProperty("os.name")));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "hardware",
                "", HARDWAREENTRY));

        if(multipart) {
            try {
                entries.add(new HttpFormEntry("attachmentOne",
                        "extra.html", EXTRATEXT.getBytes("UTF-8")));
                entries.add(new HttpFormEntry(
                        "attachmentTwo", "bundle.xidv",
                        BUNDLETEXT.getBytes("UTF-8")));
                entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                        "attachmentThree", "Attachment:", "",
                        false));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return entries;
    }

    protected boolean process(String body, boolean multipart)
            throws IOException
    {
        Object json = Json.parse(body);
        cleanup(json, multipart);
        String text = Json.toString(json);
        body = text;
        if(DEBUG) {
            System.err.println("********************");
            System.err.println(body);
            System.err.println("********************");
        }
        String diffs = compare("TestMultipart", expectedMultipart, text);
        if(diffs != null) {
            System.err.println(diffs);
            return false;
        }
        return true;
    }

    protected void cleanup(Object o, boolean multipart)
            throws HTTPException
    {
        Map<String, Object> map = (Map<String, Object>) o;
        Object oh = map.get("headers");
        String boundary = null;
        if(oh != null) {
            Map<String, Object> headers = (Map<String, Object>) oh;
            String formdata = (String) headers.get("content-type");
            if(oh != null) {
                String[] pieces = formdata.split("[ \t]*[;][ \t]*");
                for(String p : pieces) {
                    if(p.startsWith("boundary=")) {
                        boundary = p.substring("boundary=".length(), p.length());
                        break;
                    }
                }
            }
            // Remove headers
            map.remove("headers");
        }
        if(multipart && boundary != null) {
            // Now parse and change the body
            String body = (String) map.get("body");
            if(body != null) {
                String[] lines = body.split("\\r\\n");
                for(int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if(line.startsWith("--" + boundary))
                        lines[i] = "--" + FAKEBOUNDARY;
                }
                map.put("body", join(lines, "\n"));
            }
        }
    }

    static protected String join(String[] pieces, String sep)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < pieces.length; i++) {
            if(i > 0) buf.append(sep);
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    protected String compare(String tag, String baseline, String s)
    {
        try {
            // Diff the two print results
            Diff diff = new Diff(tag);
            StringWriter sw = new StringWriter();
            boolean pass = !diff.doDiff(baseline, s, sw);
            return (pass ? null : sw.toString());
        } catch (Exception e) {
            System.err.println("UnitTest: Diff failure: " + e);
            return null;
        }
    }

    //////////////////////////////////////////////////

    static final String expectedSimple =
            "{\n"
            +"  \"method\" : \"POST\",\n"
            +"  \"uri\" : \"/\",\n"
            +"  \"path\" : {\n"
            +"    \"name\" : \"/\",\n"
            +"    \"query\" : \"\",\n"
            +"    \"params\" : {}\n"
            +"  },\n"
            +"  \"body\" : \"os=Windows+7&organization=UCAR&hardware=1.0&packageVersion=1.0&softwarePackage=IDV&submit=Send+Email&description=hello+world&subject=test+httpformentry&emailAddress=jones%40gmail.com&fullName=Jim+Jones\",\n"
            +"  \"ip\" : \"127.0.0.1\",\n"
            +"  \"powered-by\" : \"http://httpkit.com\",\n"
            +"  \"docs\" : \"http://httpkit.com/echo\"\n"
            +"}\n";

    static final String expectedMultipart =
            "{\n"
            +"  \"method\" : \"POST\",\n"
            +"  \"uri\" : \"/\",\n"
            +"  \"path\" : {\n"
            +"    \"name\" : \"/\",\n"
            +"    \"query\" : \"\",\n"
            +"    \"params\" : {}\n"
            +"  },\n"
            +"  \"body\" : \"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"os\"\n"
            +"\n"
            +"Windows 7\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"organization\"\n"
            +"\n"
            +"UCAR\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"attachmentOne\"; filename=\"extra.html\"\n"
            +"Content-Type: application/octet-stream\n"
            +"\n"
            +"whatever\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"hardware\"\n"
            +"\n"
            +"1.0\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"packageVersion\"\n"
            +"\n"
            +"1.0\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"softwarePackage\"\n"
            +"\n"
            +"IDV\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"submit\"\n"
            +"\n"
            +"Send Email\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"attachmentTwo\"; filename=\"bundle.xidv\"\n"
            +"Content-Type: application/octet-stream\n"
            +"\n"
            +"bundle\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"description\"\n"
            +"\n"
            +"hello world\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"subject\"\n"
            +"\n"
            +"test httpformentry\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"emailAddress\"\n"
            +"\n"
            +"jones@gmail.com\n"
            +"--XXXXXXXXXXXXXXXXXXX\n"
            +"Content-Disposition: form-data; name=\"fullName\"\n"
            +"\n"
            +"Jim Jones\n"
            +"--XXXXXXXXXXXXXXXXXXX\",\n"
            +"  \"ip\" : \"127.0.0.1\",\n"
            +"  \"powered-by\" : \"http://httpkit.com\",\n"
            +"  \"docs\" : \"http://httpkit.com/echo\"\n"
            +"}\n";

}

