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
import ucar.unidata.repository.metadata.Metadata;
import ucar.unidata.repository.metadata.JpegMetadataHandler;
import ucar.unidata.repository.type.*;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;



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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MapOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_MAP =
        new OutputType("Map", "map.map",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_MAP);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public MapOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_MAP);
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
        boolean ok = false;
        for (Entry entry : state.getAllEntries()) {
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                ok = true;
                break;
            }
        }
        if (ok) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_MAP));

        }
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
        List<Entry> entriesToUse = new ArrayList<Entry>();
        entriesToUse.add(entry);
        StringBuffer sb = new StringBuffer();
        getMap(request, entriesToUse, sb, 700, 500, true,new boolean[]{false});
        return makeLinksResult(request, msg("Map"), sb, new State(entry));
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
        List<Entry> entriesToUse = new ArrayList<Entry>(subGroups);
        entriesToUse.addAll(entries);
        StringBuffer sb = new StringBuffer();
        if (entriesToUse.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");
            return makeLinksResult(request, msg("Map"), sb,
                                   new State(group, subGroups, entries));
        }

        sb.append(
            "<table border=\"0\" width=\"100%\"><tr valign=\"top\"><td width=700>");
        boolean [] haveBearingLines = {false};
        String mapVarName = getMap(request, entriesToUse, sb, 700, 500, true,haveBearingLines);
        sb.append("</td><td>");
        if(haveBearingLines[0]) {
            sb.append(HtmlUtil.checkbox("dummy", "true", false, HtmlUtil.onMouseClick("toggleBearingLines(" + mapVarName+");")));
            sb.append("  ");
            sb.append(msg("Show bearing lines"));
            sb.append(HtmlUtil.br());
        }

        for (Entry entry : entriesToUse) {
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                sb.append(HtmlUtil.img(getEntryManager().getIconUrl(request,
                        entry)));
                sb.append(HtmlUtil.space(1));
                sb.append("<a href=\"javascript:hiliteEntry(" + mapVarName
                          + "," + sqt(entry.getId()) + ");\">"
                          + entry.getName() + "</a><br>");
            }
        }
        sb.append("</td></tr></table>");
        return makeLinksResult(request, msg("Map"), sb,
                               new State(group, subGroups, entries));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entriesToUse _more_
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param normalControls _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getMap(Request request, List<Entry> entriesToUse,
                         StringBuffer sb, int width, int height,
                         boolean normalControls, boolean []haveBearingLines)
            throws Exception {
        StringBuffer js         = new StringBuffer();
        String       mapVarName = "mapstraction" + HtmlUtil.blockCnt++;
        getRepository().getMapManager().initMap(request, mapVarName, sb,
                width, height, normalControls);
        js.append(mapVarName + ".resizeTo(" + width + "," + height + ");\n");
        js.append("var marker;\n");
        js.append("var line;\n");

        int cnt = 0;
        for (Entry entry : entriesToUse) {
            if (entry.hasAreaDefined()) {
                cnt++;
            }
        }

        boolean makeRectangles = cnt <= 20;
        for (Entry entry : entriesToUse) {
            String idBase = entry.getId();
            if (entry.hasAreaDefined()) {
                js.append("line = new Polyline([");
                js.append(llp(entry.getNorth(), entry.getWest()));
                js.append(",");
                js.append(llp(entry.getNorth(), entry.getEast()));
                js.append(",");
                js.append(llp(entry.getSouth(), entry.getEast()));
                js.append(",");
                js.append(llp(entry.getSouth(), entry.getWest()));
                js.append(",");
                js.append(llp(entry.getNorth(), entry.getWest()));
                js.append("]);\n");
                js.append("initLine(line," + qt(entry.getId()) + ","
                          + (makeRectangles
                             ? "1"
                             : "0") + "," + mapVarName + ");\n");

            }

            if(makeRectangles) {
                entry.getTypeHandler().addToMap(request, entry, mapVarName, js);
            }
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                String info =
                    "<table>"
                    + entry.getTypeHandler().getInnerEntryContent(entry,
                        request, OutputHandler.OUTPUT_HTML, true, false,
                        false) + "</table>";


                double[]location;
                if (makeRectangles || !entry.hasAreaDefined()) {
                    location = entry.getLocation();
                } else {
                    location = entry.getCenter();
                }

                if (entry.getResource().isImage()) {
                    String thumbUrl = getRepository().absoluteUrl(HtmlUtil.url(
                                      request.url(repository.URL_ENTRY_GET)
                                      + "/"
                                      + getStorageManager().getFileTail(
                                          entry), ARG_ENTRYID, entry.getId(),
                                      ARG_IMAGEWIDTH, "300"));
                    info = info+HtmlUtil.img(thumbUrl,"","");

                    List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
                    for(Metadata metadata: metadataList) {
                        if(metadata.getType().equals(JpegMetadataHandler.TYPE_CAMERA_DIRECTION)) {
                            double dir = Double.parseDouble(metadata.getAttr1());
                            LatLonPointImpl fromPt = new LatLonPointImpl(location[0],location[1]);
                            LatLonPointImpl pt = Bearing.findPoint(fromPt,dir,0.25,null);
                            js.append("var bearingLine = new Polyline([");
                            js.append(llp(location[0],location[1]));
                            js.append(",");
                            js.append(llp(pt.getLatitude(), pt.getLongitude()));
                            js.append("]);");
                            js.append("bearingLine.setColor(bearingLineColor);\n");
                            js.append("bearingLine.setWidth(1);\n");
                            js.append("bearingLines[bearingLines.length] = bearingLine;\n");
                            if(entriesToUse.size()==1) {
                                js.append(mapVarName +".addPolyline(bearingLine);\n");
                            }
                            haveBearingLines[0] = true;
                            break;
                        } 
                    }
                }

                info = info.replace("\r", " ");
                info = info.replace("\n", " ");
                info = info.replace("\"", "\\\"");
                String icon = getEntryManager().getIconUrl(request, entry);
                js.append("marker = new Marker("
                          + llp(location[0], location[1])
                              + ");\n");

                
                js.append("marker.setIcon(" + qt(icon) + ");\n");
                js.append("marker.setInfoBubble(\"" + info + "\");\n");
                js.append("initMarker(marker," + qt(entry.getId()) + ","
                          + mapVarName + ");\n");
            }
        }
        js.append(mapVarName + ".autoCenterAndZoom();\n");
        sb.append(HtmlUtil.script(js.toString()));
        return mapVarName;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String qt(String s) {
        return "\"" + s + "\"";
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String sqt(String s) {
        return "'" + s + "'";
    }


    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public static String llp(double lat, double lon) {
        if (lat < -90) {
            lat = -90;
        }
        if (lat > 90) {
            lat = 90;
        }
        if (lon < -180) {
            lon = -180;
        }
        if (lon > 180) {
            lon = 180;
        }
        return "new LatLonPoint(" + lat + "," + lon + ")";

    }



}
