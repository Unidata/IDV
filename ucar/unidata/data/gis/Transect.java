/*
 * $Id: Transect.java,v 1.14 2006/12/01 20:42:32 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Found2ation; either version 2.1 of the License, or (at
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

package ucar.unidata.data.gis;


import org.w3c.dom.*;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import visad.*;

import java.awt.Color;

import java.util.ArrayList;
import java.util.List;




/**
 * Holds a list of lat/lon points
 */

public class Transect {

    /** Is this transect editable */
    private boolean editable = true;

    /** xml tag */
    private static final String TAG_TRANSECTS = "transects";

    /** xml tag */
    private static final String TAG_TRANSECT = "transect";

    /** xml attribute */
    private static final String ATTR_COLOR = "color";

    /** xml attribute */
    private static final String ATTR_NAME = "name";

    /** xml attribute */
    private static final String ATTR_POINTS = "points";

    /** xml attribute */
    private static final String ATTR_STARTTEXT = "starttext";

    /** xml attribute */
    private static final String ATTR_ENDTEXT = "endtext";

    /** The name */
    private String name = "";

    /** Start label */
    private String startText = "";

    /** End label */
    private String endText = "";

    /** Color */
    private Color color;

    /** List of LatLonPoints */
    private List points = new ArrayList();

    /**
     * Default ctor
     */
    public Transect() {}



    /**
     * Copy ctor
     *
     * @param that The transect to copy
     */
    public Transect(Transect that) {
        this.name   = that.name;
        this.points = new ArrayList(that.points);
    }


    /**
     * ctor
     *
     * @param name name
     * @param points list of LatLonPoints
     */
    public Transect(String name, List points) {
        this.name   = name;
        this.points = points;
    }

    public Transect(String name, LatLonPoint p1, LatLonPoint p2) {
        this.name = name;
        points= Misc.newList(p1,p2);
    }

    /**
     * Process the xml
     *
     * @param root doc root
     *
     * @return List of Transect objects
     */
    public static List parseXml(Element root) {
        List transects = new ArrayList();
        List nodes     = XmlUtil.findChildren(root, TAG_TRANSECT);
        for (int i = 0; i < nodes.size(); i++) {
            Element node = (Element) nodes.get(i);
            double[] latLons = Misc.parseLatLons(XmlUtil.getAttribute(node,
                                   ATTR_POINTS));
            List points = new ArrayList();
            for (int ptIdx = 0; ptIdx < latLons.length; ptIdx += 2) {
                points.add(new LatLonPointImpl(latLons[ptIdx],
                        latLons[ptIdx + 1]));
            }
            Transect transect = new Transect(XmlUtil.getAttribute(node,
                                    ATTR_NAME, ""), points);

            transect.setColor(XmlUtil.getAttribute(node, ATTR_COLOR,
                    (Color) null));
            transect.setStartText(XmlUtil.getAttribute(node, ATTR_STARTTEXT,
                    ""));
            transect.setEndText(XmlUtil.getAttribute(node, ATTR_ENDTEXT, ""));
            transects.add(transect);

        }
        return transects;
    }


    /**
     * Convert to xml
     *
     * @param transects transects
     *
     * @return Xml
     */
    public static String toXml(List transects) {
        Document doc   = XmlUtil.makeDocument();
        Element  root  = doc.createElement(TAG_TRANSECTS);

        List     nodes = XmlUtil.findChildren(root, TAG_TRANSECT);
        for (int i = 0; i < transects.size(); i++) {
            Transect transect = (Transect) transects.get(i);
            Element  node     = doc.createElement(TAG_TRANSECT);
            root.appendChild(node);
            String name = transect.getName();
            if ((transect.getStartText() != null)
                    && (transect.getStartText().length() > 0)) {
                node.setAttribute(ATTR_STARTTEXT, transect.getStartText());
            }
            if ((transect.getEndText() != null)
                    && (transect.getEndText().length() > 0)) {
                node.setAttribute(ATTR_ENDTEXT, transect.getEndText());
            }
            if (transect.getColor() != null) {
                XmlUtil.setAttribute(node, ATTR_COLOR, transect.getColor());
            }
            node.setAttribute(ATTR_NAME, ((name == null)
                                          ? ""
                                          : name));

            List points = new ArrayList();
            for (int ptIdx = 0; ptIdx < transect.points.size(); ptIdx++) {
                Object      obj = transect.points.get(ptIdx);
                LatLonPoint llp = (LatLonPoint) transect.points.get(ptIdx);
                points.add("" + llp.getLatitude());
                points.add("" + llp.getLongitude());
            }
            node.setAttribute(ATTR_POINTS, StringUtil.join(",", points));
        }
        return XmlUtil.toString(root);
    }

    public void shiftPercent(double latPercent, double lonPercent, boolean doPoint1, boolean doPoint2 ) {
        System.err.println("before:" + this);
        List newPoints = new ArrayList();
        LatLonPoint llp1 = (LatLonPoint) points.get(0);
        LatLonPoint llp2 = (LatLonPoint) points.get(1);
        double latDelta = latPercent*Math.abs(llp1.getLatitude()-llp2.getLatitude());
        double lonDelta = lonPercent*Math.abs(llp1.getLongitude()-llp2.getLongitude());



        newPoints.add(doPoint1?new LatLonPointImpl(llp1.getLatitude()+latDelta,
                                                   llp1.getLongitude()+lonDelta):llp1);
        newPoints.add(doPoint2?new LatLonPointImpl(llp2.getLatitude()+latDelta,
                                                   llp2.getLongitude()+lonDelta):llp2);
        points = newPoints;
        System.err.println("after:" + this);
        
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Points property.
     *
     * @param value The new value for Points
     */
    public void setPoints(List value) {
        points = value;
    }

    /**
     * Get the Points property.
     *
     * @return The Points
     */
    public List getPoints() {
        return points;
    }

    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        String result = name;
        if (points.size() >= 1) {
            result = result + ": " + points.get(0);
        }
        if (points.size() >= 2) {
            result = result + " - " + points.get(1);
        }
        return result;
    }

    /**
     * equals
     *
     * @param o that
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof Transect)) {
            return false;
        }
        Transect that = (Transect) o;
        return Misc.equals(this.name, that.name)
               && Misc.equals(this.points, that.points);
    }

    /**
     * Set the Editable property.
     *
     * @param value The new value for Editable
     */
    public void setEditable(boolean value) {
        editable = value;
    }

    /**
     * Get the Editable property.
     *
     * @return The Editable
     */
    public boolean getEditable() {
        return editable;
    }



    /**
     * Set the StartText property.
     *
     * @param value The new value for StartText
     */
    public void setStartText(String value) {
        startText = value;
    }

    /**
     * Get the StartText property.
     *
     * @return The StartText
     */
    public String getStartText() {
        return startText;
    }

    /**
     * Set the EndText property.
     *
     * @param value The new value for EndText
     */
    public void setEndText(String value) {
        endText = value;
    }

    /**
     * Get the EndText property.
     *
     * @return The EndText
     */
    public String getEndText() {
        return endText;
    }

    /**
     * Set the Color property.
     *
     * @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
    }

    /**
     * Get the Color property.
     *
     * @return The Color
     */
    public Color getColor() {
        return color;
    }



}

