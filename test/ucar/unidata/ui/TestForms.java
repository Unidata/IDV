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

/**
 * In order to test the changes to HttpFormEntry, I created
 * a unit test for it. Testing http connections is always a bit
 * tricky, so what I did was to use an echo service: http://echo.httpkit.com.
 * The echo services accepts requests from some client and returns
 * what it received in text form, but encoded in Json format.
 * So, This test creates a form and sends it to the echo service.
 * It collects the returned Json, processes it and compares it
 * to expected value (see end of this file). Processing involves
 * removing irrelevant info and converting changeable info
 * to a fixed form or to removing it altogether.
 * (see the procedures process and cleanup).
 * If, eventually, the echo service I am using goes away,
 * then this test will need to be rewritten for some other
 * echo service.
 */


package ucar.unidata.ui;


import org.junit.Test;

import ucar.httpservices.HTTPException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Test HttpFormEntry
 *
 * @author IDV development team
 */


public class TestForms extends UnitTestCommon {

    //////////////////////////////////////////////////
    // Constants

    /** _more_ */
    static final boolean DEBUG = true;

    // Pick a URL to test

    // Will send to esupport
    //static protected final String TESTURL = "http://www.unidata.ucar.edu/support/requestSupport.jsp";

    /** _more_ */
    static protected final String TESTURL = "http://echo.httpkit.com";

    // Field values to use

    /** _more_ */
    static final String DESCRIPTIONENTRY = "hello world";

    /** _more_ */
    static final String NAMEENTRY = "Jim Jones";

    /** _more_ */
    static final String EMAILENTRY = "jones@gmail.com";

    /** _more_ */
    static final String ORGENTRY = "UCAR";

    /** _more_ */
    static final String SUBJECTENTRY = "test httpformentry";

    /** _more_ */
    static final String NAGENTRY = "I have no idea";

    /** _more_ */
    static final String VERSIONENTRY = "1.0";

    /** _more_ */
    static final String HARDWAREENTRY = "x86";

    /** _more_ */
    static final String SOFTWAREPACKAGEENTRY = "IDV";

    /** _more_ */
    static final String BUNDLETEXT = "bundle";

    /** _more_ */
    static final String EXTRATEXT = "whatever";

    /** _more_ */
    static final String OSTEXT = System.getProperty("os.name");

    /** _more_ */
    static final char QUOTE = '"';

    //////////////////////////////////////////////////
    // Instance fields

    /** _more_ */
    protected boolean pass = true;

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Construct a TestForms 
     */
    public TestForms() {
        super("TestForms");
    }

    //////////////////////////////////////////////////

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testSimple() throws Exception {
        pass = true;
        List<HttpFormEntry> form    = buildForm(false);
        String[]            results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if (results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if (results[0] == null) {
            pass |= process(results[1], false);
        }
        assertTrue("TestForms.simple: failed", pass);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testMultipartForm() throws Exception {
        pass = false;
        List<HttpFormEntry> form    = buildForm(true);
        String[]            results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if (results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if (results[0] == null) {
            pass |= process(results[1], true);
        }
        assertTrue("TestForms.multipart: failed", pass);
    }

    /**
     * Compute list of HttpFormEntry's to test.
     *
     * @param multipart _more_
     *
     * @return _more_
     */
    protected List<HttpFormEntry> buildForm(boolean multipart) {
        List<HttpFormEntry> entries  = new ArrayList<>();
        StringBuffer        javaInfo = new StringBuffer();
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
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_LABEL, "",
                                      NAGENTRY));
        entries.add(descriptionEntry =
            new HttpFormEntry(HttpFormEntry.TYPE_AREA, "description",
                              "Description:", DESCRIPTIONENTRY, 5, 30, true));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "submit",
                                      "", "Send Email"));

        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "softwarePackage", "",
                                      SOFTWAREPACKAGEENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "packageVersion", "", VERSIONENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "os", "",
                                      OSTEXT));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "hardware",
                                      "", HARDWAREENTRY));

        if (multipart) {
            try {
                entries.add(new HttpFormEntry("attachmentOne", "extra.html",
                        EXTRATEXT.getBytes("UTF-8")));
                entries.add(new HttpFormEntry("attachmentTwo", "bundle.xidv",
                        BUNDLETEXT.getBytes("UTF-8")));
                entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                        "attachmentThree", "Attachment:", "", false));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return entries;
    }

    /**
     * _more_
     *
     * @param body _more_
     * @param multipart _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    protected boolean process(String body, boolean multipart)
            throws IOException {
        Object json = Json.parse(body);
        if (DEBUG) {
            visual("Raw text", Json.toString(json));
        }
        json = cleanup(json, multipart);
        String text = Json.toString(json);
        text = localize(text, OSTEXT);
        if (DEBUG) {
            visual(multipart
                   ? "TestMultipart"
                   : "TestSimple", text);
        }
        String diffs = compare(multipart
                               ? "TestMultipart"
                               : "TestSimple", multipart
                ? expectedMultipart
                : expectedSimple, text);
        if (diffs != null) {
            System.err.println(diffs);
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param o _more_
     * @param multipart _more_
     *
     * @return _more_
     *
     * @throws HTTPException _more_
     */
    protected Map<String, Object> cleanup(Object o, boolean multipart)
            throws HTTPException {
        Map<String, Object> map = (Map<String, Object>) o;
        map = (Map<String, Object>) sort(map);
        Object oh       = map.get("headers");
        String boundary = null;
        if (oh != null) {
            Map<String, Object> headers = (Map<String, Object>) oh;
            String formdata             =
                (String) headers.get("content-type");
            if (oh == null) {
                formdata = (String) headers.get("Content-Type");
            }
            if (oh != null) {
                String[] pieces = formdata.split("[ \t]*[;][ \t]*");
                for (String p : pieces) {
                    if (p.startsWith("boundary=")) {
                        boundary = p.substring("boundary=".length(),
                                p.length());
                        break;
                    }
                }
            }
            // Remove headers
            map.remove("headers");
        }
        String body = (String) map.get("body");
        if (body != null) {
            if (multipart && (boundary != null)) {
                Map<String, String> bodymap = parsemultipartbody(body);
                map.put("body", mapjoin(bodymap, "\n", ": "));
            } else {
                Map<String, String> bodymap = parsesimplebody(body);
                map.put("body", mapjoin(bodymap, "&", "="));
            }
        }
        return map;
    }

    /**
     * _more_
     *
     * @param text _more_
     * @param os _more_
     *
     * @return _more_
     *
     * @throws HTTPException _more_
     */
    protected String localize(String text, String os) throws HTTPException {
        text = text.replace(os, "<OS_NAME>");
        if (os.indexOf(' ') >= 0) {
            os = os.replace(' ', '+');
        }
        text = text.replace(os, "<OS+NAME>");
        return text;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    protected Object sort(Object o) {
        if (o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            map = new TreeMap(map);  // Convert the map to sorted order
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                map.put(entry.getKey(), sort(entry.getValue()));
            }
            return map;
        } else if (o instanceof List) {
            List<Object> list = (List) o;
            List<Object> out  = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                out.add(sort(list.get(i)));
            }
            return out;
        } else {
            return o;
        }
    }

    /**
     * _more_
     *
     * @param body _more_
     *
     * @return _more_
     *
     * @throws HTTPException _more_
     */
    protected Map<String, String> parsesimplebody(String body)
            throws HTTPException {
        Map<String, String> map    = new TreeMap<>();
        String[]            pieces = body.split("[&]");
        for (String piece : pieces) {
            String[] pair = piece.split("[=]");
            if (pair.length == 1) {
                pair = new String[] { pair[0], "" };
            }
            if ((pair[0] == null) || (pair[0].length() == 0)) {
                throw new HTTPException("Illegal body : " + body);
            }
            map.put(pair[0], pair[1]);
        }
        return map;
    }


    /** _more_ */
    static final Pattern blockb = Pattern.compile("--[^\n]*+[\n]",
                                      Pattern.DOTALL);

    /** _more_ */
    static final Pattern blockc =
        Pattern.compile("Content-Disposition:\\s+form-data;\\s+name="
                        + "[\"]([^\"]*)[\"]", Pattern.DOTALL);

    /** _more_ */
    static final Pattern blockf =
        Pattern.compile(
            "[;]\\s+filename=" + "[\"]([^\"]*)[\"]"
            + "[\n][Cc]ontent-[Tt]ype[:]\\s*([^\n]*)", Pattern.DOTALL);

    /** _more_ */
    static final Pattern blockx = Pattern.compile("([^\n]*)[\n]",
                                      Pattern.DOTALL);


    /**
     * _more_
     *
     * @param body _more_
     *
     * @return _more_
     *
     * @throws HTTPException _more_
     */
    protected Map<String, String> parsemultipartbody(String body)
            throws HTTPException {
        Map<String, String> map = new TreeMap<>();
        body = body.replace("\r\n", "\n");
        while (body.length() > 0) {
            Matcher mb = blockb.matcher(body);
            if ( !mb.lookingAt()) {
                throw new HTTPException("Missing boundary marker : " + body);
            }
            int len = mb.group(0).length();
            body = body.substring(len);
            if (body.length() == 0) {
                break;  // trailing boundary marker
            }
            Matcher mc = blockc.matcher(body);
            if ( !mc.lookingAt()) {
                throw new HTTPException("Missing Content-type marker : "
                                        + body);
            }
            len = mc.group(0).length();
            String name = mc.group(1);
            body = body.substring(len);
            Matcher mf          = blockf.matcher(body);
            String  filename    = null;
            String  contenttype = null;
            if (mf.lookingAt()) {
                filename    = mf.group(1);
                contenttype = mf.group(2);
                len         = mf.group(0).length();
                body        = body.substring(len);
            }
            // Skip the two newlines
            if ( !body.startsWith("\n\n")) {
                throw new HTTPException("Missing newlines");
            }
            body = body.substring(2);
            // Extract the pieces
            String  value = null;
            Matcher mx    = blockx.matcher(body);
            if (mx.lookingAt()) {
                value = mx.group(1);
                len   = mx.group(0).length();
                body  = body.substring(len);
            } else {
                throw new HTTPException("Match failure at : " + body);
            }
            map.put(name, value);
        }
        return map;
    }

    /**
     * _more_
     *
     * @param pieces _more_
     * @param sep _more_
     *
     * @return _more_
     */
    static protected String join(String[] pieces, String sep) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0) {
                buf.append(sep);
            }
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param map _more_
     * @param sep1 _more_
     * @param sep2 _more_
     *
     * @return _more_
     */
    static protected String mapjoin(Map<String, String> map, String sep1,
                                    String sep2) {
        StringBuilder buf   = new StringBuilder();
        boolean       first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if ( !first) {
                buf.append(sep1);
            }
            first = false;
            buf.append(entry.getKey());
            buf.append(sep2);
            buf.append(entry.getValue());
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////

    /** _more_ */
    static final String expectedMultipart =
        "{\n" + "  \"body\" : \"attachmentOne: whatever\n"
        + "attachmentTwo: bundle\n" + "description: hello world\n"
        + "emailAddress: jones@gmail.com\n" + "fullName: Jim Jones\n"
        + "hardware: x86\n" + "organization: UCAR\n" + "os: <OS_NAME>\n"
        + "packageVersion: 1.0\n" + "softwarePackage: IDV\n"
        + "subject: test httpformentry\n" + "submit: Send Email\",\n"
        + "  \"docs\" : \"http://httpkit.com/echo\",\n"
        + "  \"ip\" : \"127.0.0.1\",\n" + "  \"method\" : \"POST\",\n"
        + "  \"path\" : {\n" + "    \"name\" : \"/\",\n"
        + "    \"params\" : {},\n" + "    \"query\" : \"\"\n" + "  },\n"
        + "  \"powered-by\" : \"http://httpkit.com\",\n"
        + "  \"uri\" : \"/\"\n" + "}\n";

    /** _more_ */
    static final String expectedSimple =
        "{\n"
        + "  \"body\" : \"description=hello+world&emailAddress=jones%40gmail.com&fullName=Jim+Jones&hardware=x86&organization=UCAR&os=<OS+NAME>&packageVersion=1.0&softwarePackage=IDV&subject=test+httpformentry&submit=Send+Email\",\n" + "  \"docs\" : \"http://httpkit.com/echo\",\n" + "  \"ip\" : \"127.0.0.1\",\n" + "  \"method\" : \"POST\",\n" + "  \"path\" : {\n" + "    \"name\" : \"/\",\n" + "    \"params\" : {},\n" + "    \"query\" : \"\"\n" + "  },\n" + "  \"powered-by\" : \"http://httpkit.com\",\n" + "  \"uri\" : \"/\"\n" + "}\n" + "\n";
}
