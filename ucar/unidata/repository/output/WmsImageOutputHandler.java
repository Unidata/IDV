/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WmsUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.io.File;


import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WmsImageOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_WMS_CAPABILITIES =
        new OutputType("WMS Capabilities", "wms.capabilities",
                       OutputType.TYPE_NONHTML, "", ICON_IMAGE);


    /** _more_          */
    public static final OutputType OUTPUT_WMS_IMAGE =
        new OutputType("WMS Image", "wms.image", OutputType.TYPE_INTERNAL,
                       "", ICON_IMAGE);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public WmsImageOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_WMS_CAPABILITIES);
        addType(OUTPUT_WMS_IMAGE);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static boolean isLatLonImage(Entry entry) {
        return entry.getType().equals("latlonimage")
               && entry.getResource().isImage();
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
        List<Entry> entries = state.getAllEntries();
        if (entries.size() == 0) {
            return;
        }
        boolean ok = false;
        for (Entry entry : entries) {
            if (isLatLonImage(entry)) {
                ok = true;
                break;
            }
        }
        if ( !ok) {
            return;
        }
        links.add(makeLink(request, state.getEntry(),
                           OUTPUT_WMS_CAPABILITIES));
        links.add(makeLink(request, state.getEntry(), OUTPUT_WMS_IMAGE));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        String wmsRequest = request.getString("request", "");
        if (wmsRequest.equals("GetMap")) {
            return outputMap(request, entry);
        }

        if (outputType.equals(OUTPUT_WMS_CAPABILITIES)) {
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            return outputCapabilities(request, entry, entries);
        }
        StringBuffer sb = new StringBuffer();
        return makeLinksResult(request, msg("WMS"), sb, new State(entry));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Group group, List<Group> subGroups,
                              List<Entry> entries)
            throws Exception {

        String wmsRequest = request.getString("request", "");
        if (wmsRequest.equals("GetMap")) {
            return outputMap(request, group);
        }
        StringBuffer sb = new StringBuffer();
        return outputCapabilities(request, group, entries);
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
    public Result outputMap(Request request, Entry entry) throws Exception {
        String layers = request.getString("Layers", (String) null);
        if (layers == null) {
            layers = request.getString("layers", (String) null);
        }
        if (layers == null) {
            throw new IllegalArgumentException(
                "No Layers argument specified");
        }
        //Pull out the first layer id (which is the entry id)
        List toks = StringUtil.split(layers, ",", true, true);
        request.put(ARG_ENTRYID, (String) toks.get(0));
        return getEntryManager().processEntryGet(request);
    }


    /** _more_          */
    private String layerTemplate =
        "<Layer         noSubsets=\"1\"         opaque=\"0\"         queryable=\"0\">        <Name>${name}</Name>        <Title>${title}</Title>        <SRS>EPSG:4326</SRS>        <LatLonBoundingBox           maxx=\"%east%\"           maxy=\"%north%\"           minx=\"%west%\"           miny=\"%south%\"/>        <BoundingBox           SRS=\"EPSG:4326\"           maxx=\"%east%\"           maxy=\"%north%\"           minx=\"%west%\"           miny=\"%south%\"/>      </Layer>";

    /** _more_          */
    private String wmsTemplate;


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputCapabilities(Request request, Entry entry,
                                     List<Entry> entries)
            throws Exception {
        if (wmsTemplate == null) {
            wmsTemplate = getRepository().getResource(
                "/ucar/unidata/repository/output/wmstemplate.xml");
        }
        String url = getRepository().absoluteUrl(
                         request.entryUrl(
                             getRepository().URL_ENTRY_SHOW, entry,
                             ARG_OUTPUT, OUTPUT_WMS_CAPABILITIES));
        String wms = wmsTemplate;
        wms = wms.replace("${url}", XmlUtil.encodeString(url));
        wms = wms.replace("${title}", XmlUtil.encodeString(entry.getName()));
        wms = wms.replace("${abstract}",
                          XmlUtil.encodeString(entry.getDescription()));

        StringBuffer layers = new StringBuffer();
        for (Entry layerEntry : entries) {
            if ( !isLatLonImage(layerEntry)) {
                continue;
            }
            String layer = layerTemplate;
            layer = layer.replace("${name}", layerEntry.getId());
            layer = layer.replace("${title}",
                                  XmlUtil.encodeString(layerEntry.getName()));
            layer = layer.replaceAll("%north%", layerEntry.getNorth() + "");
            layer = layer.replaceAll("%south%", layerEntry.getSouth() + "");
            layer = layer.replaceAll("%east%", layerEntry.getEast() + "");
            layer = layer.replaceAll("%west%", layerEntry.getWest() + "");
            layers.append(layer);
            layers.append("\n");
        }
        wms = wms.replace("${layers}", layers.toString());
        return new Result("", new StringBuffer(wms), "text/xml");
    }


}
