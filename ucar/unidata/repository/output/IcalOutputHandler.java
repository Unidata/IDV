/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.output;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.type.*;
import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringBufferCollection;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;



import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IcalOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_ICAL = new OutputType("ICAL",
                                                     "ical",
                                                     OutputType.TYPE_NONHTML,
                                                     "", ICON_CALENDAR);


    /** _more_          */
    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public IcalOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ICAL);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.getEntry() != null) {
            links.add(
                makeLink(
                    request, state.entry, OUTPUT_ICAL,
                    "/" + IOUtil.stripExtension(state.getEntry().getName())
                    + ".ics"));
        }
    }





    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_ICAL)) {
            return repository.getMimeTypeFromSuffix(".ics");
        } else {
            return super.getMimeType(output);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        entries.addAll(subGroups);
        return outputEntries(request, entries);
    }

    /**
     * _more_
     *
     * @param t _more_
     *
     * @return _more_
     */
    private String format(long t) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");
        }
        return sdf.format(new Date(t)) + "Z";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntries(Request request, List<Entry> entries)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("PRODID:-//Unidata/UCAR//RAMADDA Calendar//EN\n");
        sb.append("VERSION:2.0\n");
        sb.append("CALSCALE:GREGORIAN\n");
        sb.append("METHOD:PUBLISH\n");
        OutputType output = request.getOutput();
        request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);

        for (Entry entry : entries) {
            sb.append("BEGIN:VEVENT\n");
            sb.append("UID:" + entry.getId() + "\n");
            sb.append("CREATED:" + format(entry.getCreateDate()) + "\n");
            sb.append("DTSTAMP:" + format(entry.getCreateDate()) + "\n");
            sb.append("DTSTART:" + format(entry.getStartDate()) + "\n");
            sb.append("DTEND:" + format(entry.getEndDate()) + "\n");

            sb.append("SUMMARY:" + entry.getName() + "\n");
            String desc = entry.getDescription();
            desc = desc.replace("\n", "\\n");

            sb.append("DESCRIPTION:" + desc);
            sb.append("\n");
            double[] loc = null;
            if (entry.hasAreaDefined()) {
                loc = entry.getLocation();
            } else if (entry.hasLocationDefined()) {
                loc = entry.getCenter();
            }
            if (loc != null) {
                sb.append("GEO:" + loc[0] + ";" + loc[1] + "\n");
            }
            String url =
                repository.absoluteUrl(request.url(repository.URL_ENTRY_SHOW,
                    ARG_ENTRYID, entry.getId()));
            sb.append("ATTACH:" + url + "\n");
            sb.append("END:VEVENT\n");
        }

        request.put(ARG_OUTPUT, output);
        sb.append("END:VCALENDAR\n");


        Result result = new Result("Query Results", sb,
                                   getMimeType(OUTPUT_ICAL));

        return result;

    }


}

