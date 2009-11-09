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
import ucar.unidata.repository.metadata.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;






/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_DIF_XML =
        new OutputType("Dif-XML", "dif.xml",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_DIF);


    /** _more_ */
    public static final OutputType OUTPUT_DIF_TEXT =
        new OutputType("Dif-Text", "dif.text",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_DIF);


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public DifOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_DIF_XML);
        addType(OUTPUT_DIF_TEXT);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.isDummyGroup()) {
            return;
        }
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_DIF_XML));
            links.add(makeLink(request, state.getEntry(), OUTPUT_DIF_TEXT));
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
        return outputEntry(request, group);
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element tag(String tag, Element parent, String text)
            throws Exception {
        return XmlUtil.create(parent.getOwnerDocument(), tag, parent, text,
                              null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, DifUtil.TAG_DIF, null,
                                      new String[] {
            "xmlns", "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/",
            "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation",
            "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/ http://gcmd.nasa.gov/Aboutus/xml/dif/dif_v9.7.1.xsd"
        });


        Element parent;


        tag(DifUtil.TAG_Entry_ID, root, entry.getId());
        tag(DifUtil.TAG_Entry_Title, root, entry.getName());
        tag(DifUtil.TAG_Summary, root, entry.getDescription());
        parent = tag(DifUtil.TAG_Temporal_Coverage, root, null);
        tag(DifUtil.TAG_Start_Date, parent,
            getRepository().formatYYYYMMDD(new Date(entry.getStartDate())));
        tag(DifUtil.TAG_Stop_Date, parent,
            getRepository().formatYYYYMMDD(new Date(entry.getEndDate())));
        if (entry.hasAreaDefined()) {
            parent = tag(DifUtil.TAG_Spatial_Coverage, root, null);
            tag(DifUtil.TAG_Northernmost_Latitude, parent,
                "" + entry.getNorth());
            tag(DifUtil.TAG_Southernmost_Latitude, parent,
                "" + entry.getSouth());
            tag(DifUtil.TAG_Westernmost_Longitude, parent,
                "" + entry.getWest());
            tag(DifUtil.TAG_Easternmost_Longitude, parent,
                "" + entry.getEast());
        }


        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if (metadataHandler.canHandle(metadata)) {
                    metadataHandler.addMetadataToXml(request,
                            MetadataTypeBase.TEMPLATETYPE_DIF, entry,
                            metadata, doc, root);
                    break;
                }
            }
        }


        StringBuffer sb = new StringBuffer();
        if (request.getOutput().equals(OUTPUT_DIF_TEXT)) {
            XmlUtil.toHtml(sb, root);
            return new Result("DIF-Text", sb);
        } else {
            sb.append(XmlUtil.XML_HEADER);
            sb.append(XmlUtil.toString(root));
            return new Result("dif", sb, "text/xml");
        }
    }

}

