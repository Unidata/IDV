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

package ucar.unidata.repository;



import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PluginClassLoader;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;





/**
 * This class provides a variety of mapping services, e.g., map display and map form selector
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public class MapManager extends RepositoryManager {





    /** _more_ */
    private static final String MAP_JS_MICROSOFT =
        "http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.1";

    /** _more_ */
    private static final String MAP_ID_MICROSOFT = "microsoft";

    /** _more_ */
    private static final String MAP_JS_YAHOO =
        "http://api.maps.yahoo.com/ajaxymap?v=3.8&appid=idvunidata";

    /** _more_ */
    private static final String MAP_ID_YAHOO = "yahoo";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public MapManager(Repository repository) {
        super(repository);
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param mapVarName _more_
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param normalControls _more_
     *
     * @return _more_
     */
    public void initMap(Request request, String mapVarName,
                          StringBuffer sb, int width, int height,
                          boolean normalControls) {
        String userAgent = request.getHeaderArg("User-Agent");
        String host      = request.getHeaderArg("Host");
        if (host == null) {
            host = "localhost";
        }
        host = (String) StringUtil.split(host, ":", true, true).get(0);
        String googleMapsKey = null;

        if (userAgent == null) {
            userAgent = "Mozilla";
        }
        String mapProvider = MAP_ID_MICROSOFT;
        String mapJS       = MAP_JS_MICROSOFT;
        String googleKeys  = getProperty(PROP_GOOGLEAPIKEYS, "");
        googleMapsKey = null;
        for (String line :
                (List<String>) StringUtil.split(googleKeys, "\n", true,
                true)) {
            if (line.length() == 0) {
                continue;
            }
            String[] toks = StringUtil.split(line, ":", 2);
            if (toks == null) {
                continue;
            }
            if (toks.length != 2) {
                continue;
            }
            if (toks[0].equals(host)) {
                googleMapsKey = toks[1];
                break;
            }
        }


        if (userAgent.indexOf("MSIE") >= 0) {
            mapProvider = MAP_ID_YAHOO;
            mapJS       = MAP_JS_YAHOO;
        }

        if (googleMapsKey != null) {
            mapJS = "http://maps.google.com/maps?file=api&v=2&key="
                    + googleMapsKey;
            mapJS = "http://maps.google.com/maps?file=api&v=3";
            mapProvider = "google";
        }


        if (request.getExtraProperty("initmap") == null) {
            sb.append(HtmlUtil.importJS(mapJS));
            sb.append(
                HtmlUtil.importJS(fileUrl("/mapstraction/mapstraction.js")));
            sb.append(HtmlUtil.importJS(fileUrl("/mapstraction/mymap.js")));
            request.putExtraProperty("initmap", "");
        }


        sb.append(HtmlUtil.div("",
                               HtmlUtil.style("width:" + width
                                   + "px; height:" + height + "px") + " "
                                       + HtmlUtil.id(mapVarName)));
        sb.append(HtmlUtil.script(mapVarName + "="
                                  + HtmlUtil.call("MapInitialize",
                                                  normalControls + ","
                                      + HtmlUtil.squote(mapProvider) + ","
                                      + HtmlUtil.squote(mapVarName)) + ";"));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getMapUrl() {
        return getRepository().getUrlBase() + "/images/maps/caida.jpg";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param popup _more_
     * @param extraLeft _more_
     * @param extraTop _more_
     *
     * @return _more_
     */
    public String makeMapSelector(Request request, String arg, boolean popup,
                                  String extraLeft, String extraTop) {

        return makeMapSelector(request, arg, popup, extraLeft, extraTop, null);
    }

    public String makeMapSelector(Request request, String arg, boolean popup,
                                  String extraLeft, String extraTop, double[][]marker) {
        return makeMapSelector(arg, popup, extraLeft, extraTop,
                               new String[]{
                                   request.getString(arg + "_south", ""),
                                   request.getString(arg + "_north", ""),
                                   request.getString(arg + "_east", ""),
                                   request.getString(arg + "_west", "")},marker);
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param popup _more_
     *
     * @return _more_
     */
    public String makeMapSelector(String arg, boolean popup, 
                                  String[] snew) {
        return makeMapSelector(arg, popup, "", "", snew);
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param popup _more_
     * @param extraLeft _more_
     * @param extraTop _more_
     *
     * @return _more_
     */
    public String makeMapSelector(String arg, boolean popup,
                                  String extraLeft, String extraTop,
                                  String[]snew) {

        return makeMapSelector(arg,popup, extraLeft, extraTop, snew, null);
    }


    public String makeMapSelector(String arg, boolean popup,
                                  String extraLeft, String extraTop,
                                  String[]snew,
                                  double[][]markerLatLons) {
        StringBuffer sb = new StringBuffer();
        String msg = HtmlUtil.italics(msg("Shift-click to select point"));
        sb.append(msg);
        sb.append(HtmlUtil.br());
        String       widget;
        if (snew==null) {
            widget = HtmlUtil.makeLatLonBox(arg, "","","","");
        } else if (snew.length == 4) {
            widget = HtmlUtil.makeLatLonBox(arg, snew[0], snew[1], snew[2],
                                            snew[3]);
        } else {
            widget = " Lat: "
                     + HtmlUtil.input(arg + "_lat", snew[0],
                                      HtmlUtil.SIZE_5 + " "
                                      + HtmlUtil.id(arg + "_lat")) + " Lon: "
                                          + HtmlUtil.input(arg + "_lon",
                                              snew[1],
                                                  HtmlUtil.SIZE_5 + " "
                                                      + HtmlUtil.id(arg
                                                          + "_lon"));
        }
        if ((extraLeft != null) && (extraLeft.length() > 0)) {
            widget = widget + HtmlUtil.br() + extraLeft;
        }


        String var         = "mapselector" + HtmlUtil.blockCnt++;
        String onClickCall = HtmlUtil.onMouseClick(var + ".click(event);");
        String rightSide = null;
        String clearLink = HtmlUtil.mouseClickHref(var + ".clear();",
                               msg("Clear"));
        String initParams = HtmlUtil.squote(arg) + "," + (popup
                ? "1"
                : "0");

        String mapVarName = "selectormap";

        try {
            initMap(getRepository().getTmpRequest(), mapVarName, sb, 500,300,true);
        } catch(Exception exc) {}

        if (popup) {
            rightSide = getRepository().makeStickyPopup(msg("Select"),
                                                        sb.toString(),
                                                        var + ".init();") + HtmlUtil.space(2) + clearLink
                                      + HtmlUtil.space(2)
                                      + HtmlUtil.space(2) + extraTop;
        } else {
            rightSide = clearLink + HtmlUtil.space(2) 
                + HtmlUtil.br() + sb.toString();
        }

        StringBuffer script = new StringBuffer();
        script.append("var " + var + " =  new MapSelector(" + initParams+ ");\n");
        if(markerLatLons!=null) {
            script.append("var markerLine = new Polyline([");
            for(int i=0;i<markerLatLons[0].length;i++) {
                if(i>0)
                    script.append(",");
                script.append("new LatLonPoint(" + markerLatLons[0][i]+"," +
                              markerLatLons[1][i]+")");
            }
            script.append("]);\n");
            script.append("markerLine.setColor(\"#00FF00\");\n");
            script.append("markerLine.setWidth(3);\n");
            script.append(mapVarName +".addPolyline(markerLine);\n");
            script.append(mapVarName +".autoCenterAndZoom();\n");
        }

        return HtmlUtil.table(new Object[] { widget, rightSide }) + "\n"
            + HtmlUtil.script(script.toString());

    }


}
