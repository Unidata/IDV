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

import ucar.httpservices.*;
import ucar.unidata.util.*;

import junit.framework.TestCase;

/**
 * Test HttpFormEntry
 *
 * @author IDV development team
 */


public class TestForms extends TestCase
{

    //////////////////////////////////////////////////
    // Constants

    // Requires obtaining this from putsreq.com
    static protected final String TESTURL = "";

    // Field values to use

    static final String DESCRIPTIONENTRY = "";
    static final String NAMEENTRY = "";
    static final String EMAILENTRY = "";
    static final String ORGENTRY = "";

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestForms()
    {
        super("TestForms");
    }

    @Test
    public void
    testForms()
	throws Exception
    {
	List<HttpFormEntry> form = buildForm();
        String[] results = HttpFormEntry.doPost(form,TESTURL);
	if(result.length >= 3) {
	    int code = 0;
	    try {
	        code = Integer.parseInt(results[2]);
	    } catch (NumberFormatException nfe) {
		code = 0;
	    }
	    System.err.println("code="+code);
	    assertTrue("Bad status code" + results[2], code == 200);
	}                       
        if(results[0] != null) {
	    System.err.println("fail; message="+results[0]);
	} else if(results[0] == null) {
	    System.err.println("ok; message="+results[1]);
        }
	
    }

    /**
     * Compute list of HttpFormEntry's to test.
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */
    protected List<HttpFormEntry> buildForm()
    {
        List<HttpFormEntry> entries  = new ArrayList<>();
        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));

        HttpFormEntry descriptionEntry;
        HttpFormEntry nameEntry;
        HttpFormEntry emailEntry;
        HttpFormEntry orgEntry;

        entries.add(nameEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "fullName", "Name:",NAMEENTRY));
        entries.add(emailEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "emailAddress", "Your Email:", EMAILENTRY));
        entries.add(orgEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "organization", "Organization:", ORGENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_INPUT, "subject",
                                      "Subject:",SUBJECTENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_LABEL, "", NAGENTRY));
        entries.add(descriptionEntry =
            new HttpFormEntry(HttpFormEntry.TYPE_AREA, "description",
                              "Description:", DESCRIPTIONENTRY, 5, 30, true));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                                      "attachmentThree", "Attachment:", "",
                                      false));

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

        entries.add(new HttpFormEntry("attachmentOne",
                        "extra.html", EXTRATEXT.toByteArray()));
	entries.add(new HttpFormEntry(
                        "attachmentTwo", "bundle.xidv",
			BUNDLETEXT.toByteArray()));
	return entries;
    }

}

