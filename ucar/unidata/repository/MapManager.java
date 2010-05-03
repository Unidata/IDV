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
public class MapManager  extends RepositoryManager {





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
    public String initMap(Request request, String mapVarName,
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
        String googleKeys = getProperty(PROP_GOOGLEAPIKEYS, "");
        googleMapsKey = null;
        for (String line : (List<String>) StringUtil.split(googleKeys, "\n",
                true, true)) {
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
            mapProvider = "google";
        }


        if (request.getExtraProperty("initmap") == null) {
            sb.append(HtmlUtil.importJS(mapJS));
            sb.append(HtmlUtil.importJS(fileUrl("/mapstraction/mapstraction.js")));
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
        return "";
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
        return makeMapSelector(arg, popup, extraLeft, extraTop,
                               request.getString(arg + "_south", ""),
                               request.getString(arg + "_north", ""),
                               request.getString(arg + "_east", ""),
                               request.getString(arg + "_west", ""));
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param popup _more_
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public String makeMapSelector(String arg, boolean popup, String south,
                                  String north, String east, String west) {
        return makeMapSelector(arg, popup, "", "", south, north, east, west);
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param popup _more_
     * @param extraLeft _more_
     * @param extraTop _more_
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public String makeMapSelector(String arg, boolean popup,
                                  String extraLeft, String extraTop,
                                  String south, String north, String east,
                                  String west) {
        return makeMapSelector(arg,popup, extraLeft, extraTop, new String[]{
                south, north, east,west});
    }


    public String makeMapSelector(String arg, boolean popup,
                                  String extraLeft, String extraTop,
                                  String[]pts) {
        StringBuffer sb = new StringBuffer();


        String widget;
        if(pts.length==4) {
            widget = HtmlUtil.makeLatLonBox(arg, pts[0], pts[1], pts[2], pts[3]);
        } else {
            widget = " Lat: " + HtmlUtil.input(arg+"_lat", pts[0], HtmlUtil.SIZE_5+" " +HtmlUtil.id(arg+"_lat")) +
                " Lon: " + HtmlUtil.input(arg+"_lon", pts[1], HtmlUtil.SIZE_5+" " +HtmlUtil.id(arg+"_lon"));
        }
        if ((extraLeft != null) && (extraLeft.length() > 0)) {
            widget = widget + HtmlUtil.br() + extraLeft;
        }

        String imageId = arg + "_bbox_image";


        String var = "mapselector" + HtmlUtil.blockCnt++;
        String onClickCall = HtmlUtil.onMouseClick(var+".click(event);");
        String bboxDiv = HtmlUtil.div("",
                                      HtmlUtil.cssClass("latlon_box")
                                      + onClickCall
                                      + HtmlUtil.id(arg + "_bbox_div"));

        StringBuffer imageHtml = new StringBuffer();
        String nextMapLink =
            HtmlUtil.mouseClickHref(var+".cycleMap()", HtmlUtil.img(iconUrl(ICON_MAP),
                                                                    " View another map", ""));
        imageHtml.append("\n");
        imageHtml.append(bboxDiv);
        imageHtml.append(HtmlUtil.table(new Object[] {
                    HtmlUtil.img(getMapUrl(), "",
                                 HtmlUtil.id(imageId) + onClickCall
                                 + HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, (popup
                                                                       ? "800"
                                                                       : "800"))), nextMapLink }));

        imageHtml.append("\n");
        String rightSide = null;
        String clearLink = HtmlUtil.mouseClickHref(var+".clear();", msg("Clear"));
        String updateLink = HtmlUtil.mouseClickHref(var+".update();", msg("Update Map"));

        String initParams = HtmlUtil.squote(imageId) + ","
                            + HtmlUtil.squote(arg) + "," + (popup
                ? "1"
                : "0");
        if (popup) {
            rightSide =
                getRepository().makeStickyPopup(msg("Select"), imageHtml.toString(),  
                                var +".init();") +
                                 HtmlUtil.space(2)
                                        + clearLink + HtmlUtil.space(2)
                                        + updateLink + HtmlUtil.space(2)
                                        + extraTop;
        } else {
            rightSide = clearLink + HtmlUtil.space(2) + updateLink
                        + HtmlUtil.br() + imageHtml;
        }

        
        String script = "var " + var + " =  new MapSelector(" +initParams +");\n";
        return HtmlUtil.table(new Object[] { widget, rightSide }) +
            "\n" +HtmlUtil.script(script);

    }


}

