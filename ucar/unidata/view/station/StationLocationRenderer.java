/*
 * $Id: StationLocationRenderer.java,v 1.30 2006/12/27 17:43:08 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.station;


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.SpatialGrid;
//import ucar.unidata.metdata.NamedStation;
//import ucar.unidata.metdata.NamedStationImpl;
//import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.metdata.Station;

import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;



import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;


/**
 * Implements the Renderer interface for collections of <code>Station</code>-s.
 * Plots a station location as a name and circle for a marker:<p>
 *  <pre>
 *        Boulder
 *           o
 *  </pre>
 *
 * @author Don Murray
 */
public class StationLocationRenderer implements ucar.unidata.view.Renderer {



    /** list of stations */
    private ArrayList stations = new ArrayList(0);

    /** default color */
    private Color color = Color.black;

    /** the station grid */
    private SpatialGrid stationGrid;  // for progressive disclosure

    /** the station model view */
    private StationModelView smv;

    /** the projection */
    private ProjectionImpl project = null;

    /** position was calculated flag */
    private boolean posWasCalc = false;

    /** declutter flag */
    private boolean declutter = true;

    /** id template */
    private String template = StationLocationMap.TEMPLATE_ID;



    /** popup display */
    private popupDisplay popup = null;

    /** transform world -> normal */
    private AffineTransform world2Normal;

    // working objects to minimize excessive gc

    /** working string buffer */
    private StringBuffer sbuff = new StringBuffer(200);

    /** working point */
    private Point2D.Double ptN = new Point2D.Double();

    /** marker size */
    private Rectangle markerSize;

    /** debug flag */
    private boolean debug        = false,
                    debugInput   = false,
                    debugClosest = false;

    /** select multiple */
    private boolean multipleSelect = false;

    /** cloud cover symbol (circle) */
    private CloudCoverageSymbol cloudSymbol;

    /** station symbol */
    private TextSymbol stationSymbol;

    /**
     * Default constructor.  Uses default label type and allows only
     * single station selection.
     */
    public StationLocationRenderer() {
        this(StationLocationMap.TEMPLATE_ID);
    }

    /**
     * Create a <code>StationLocationRenderer</code> using single station
     * selection and the labeling type defined.
     * @param template The String template to use for displaying station labels.
     */
    public StationLocationRenderer(String template) {
        this(false, template);
    }

    /**
     * Create a <code>StationLocationRenderer</code> using the
     * selection type defined and the default labeling type.
     * @param multipleSelect   allow selection of multiple stations
     */
    public StationLocationRenderer(boolean multipleSelect) {
        this(multipleSelect, StationLocationMap.TEMPLATE_ID);
    }

    /**
     * Create a <code>StationLocationRenderer</code> using the
     * selection type and the labeling type defined.
     * @param template The String template to use for displaying station labels.
     * @param multipleSelect   allow selection of multiple stations
     */
    public StationLocationRenderer(boolean multipleSelect, String template) {
        this.multipleSelect = multipleSelect;
        stationGrid         = new SpatialGrid(30, 30);
        this.template       = template;

        // station model is a station identifier and a clear cloud symbol
        smv         = new StationModelView("Station");

        cloudSymbol = new CloudCoverageSymbol(0, 0, "cc", "cloud coverage");
        cloudSymbol.setSize(6, 6);
        cloudSymbol.setActive(true);
        cloudSymbol.setCoverage(0.0);
        markerSize = cloudSymbol.getBounds();
        smv.addSymbol(cloudSymbol);

        stationSymbol = new TextSymbol(0, -12, "name", "Station Name");
        stationSymbol.setSize(0, 9);  // default only
        stationSymbol.setActive(true);
        smv.addSymbol(stationSymbol);
    }

    /**
     * Set the color for rendering unselected stations.
     * @param color  color for rendering.
     */
    public void setColor(java.awt.Color color) {
        this.color = color;
    }

    /**
     * Get the color used for rendering stations.
     * @return color being used.
     */
    public java.awt.Color getColor() {
        return color;
    }


    /**
     * Get the preferred area to be displayed.
     *
     * @return null
     */
    public LatLonRect getPreferredArea() {
        return null;
    }

    /**
     * Set the stations to be displayed by this renderer.
     * @param stns    <code>List</code> of <code>Station</code> objects
     * @param declut  true to declutter stations.
     */
    public void setStations(List stns, boolean declut) {
        setStations(stns, null, declut);
    }


    /**
     * Set the stations to be displayed by this renderer.  Set the
     * stations in the selected list to be selected.
     * @param stns             <code>List</code> of <code>Station</code> objects
     * @param selectedStations <code>List</code> of selected stations
     * @param declut           true to declutter stations.
     */
    public void setStations(List stns, List selectedStations,
                            boolean declut) {

        declutter = declut;
        stations.clear();
        for (int i = 0; i < stns.size(); i++) {
            Station   station   = (Station) stns.get(i);
            SLStation slStation = new SLStation(station);
            if ((selectedStations != null)
                    && selectedStations.contains(station)) {
                slStation.setSelected(true);
            } else {
                slStation.setSelected(false);
            }
            stations.add(slStation);
        }
    }

    /**
     *  Return the (cloned) list of  Stations held by this renderer.
     *
     *  @return The list of Stations.
     */
    public List getStations() {
        return new ArrayList(stations);
    }


    /**
     * Set selected stations.
     * @param selectedStations <code>List</code> of selected stations
     */
    public void setSelectedStations(List selectedStations) {
        for (int i = 0; i < stations.size(); i++) {
            SLStation slStation = (SLStation) stations.get(i);
            slStation.setSelected(
                selectedStations.contains(slStation.getStation()));
        }
    }


    /**
     * Set whether station should be decluttered or not.
     * @param declut  true to declutter
     */
    public void setDeclutter(boolean declut) {
        declutter = declut;
    }

    /**
     * Get whether station should be decluttered or not.
     * @return true if decluttering
     */
    public boolean getDeclutter() {
        return declutter;
    }

    /**
     * render the stations.
     * @param g  Graphics to draw to
     * @param normal2Device  transform for data to device coordinates
     */
    public void draw(java.awt.Graphics2D g, AffineTransform normal2Device) {

        if ((smv == null) || (project == null) || !posWasCalc) {
            return;
        }

        // use world coordinates for position, but draw in screen coordinates
        // so that the symbols stay the same size
        AffineTransform world2Device = g.getTransform();
        g.setTransform(normal2Device);  //  identity transform

        // transform World to Normal coords:
        //    world2Normal = pixelAT-1 * world2Device
        // cache for pick closest
        try {
            world2Normal = normal2Device.createInverse();
            world2Normal.concatenate(world2Device);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            System.out.println(
                " RendSurfObs: NoninvertibleTransformException on "
                + normal2Device);
            return;
        }


        g.setColor(color);

        // we want aliasing; but save previous state to restore at end
        Object saveHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new java.awt.BasicStroke(1.0f));

        // this is the bounding box of the Station Model View object
        // that we will draw
        Rectangle typicalBounds = null;

        // clipping area in normal coords
        Rectangle2D bbox = (Rectangle2D) g.getClip();

        // clear the grid = "no stations are drawn"
        stationGrid.clear();

        for (int i = 0; i < stations.size(); i++) {
            SLStation s = (SLStation) stations.get(i);
            world2Normal.transform(s.worldPos, s.screenPos);  // work in normalized coordinate space

            s.resetBounds();
            //Skip over the stations for which the current projection gives us infinity
            if ((s.screenPos.getX() == Double.POSITIVE_INFINITY)
                    || (s.screenPos.getX() == Double.NEGATIVE_INFINITY)
                    || (s.screenPos.getY() == Double.POSITIVE_INFINITY)
                    || (s.screenPos.getY() == Double.NEGATIVE_INFINITY)) {
                continue;
            }
            if (typicalBounds == null) {
                if (declutter) {
                    typicalBounds = smv.getBounds();
                } else {
                    typicalBounds = markerSize;
                }
                // set the grid size based  on typical bounding box
                stationGrid.setGrid(bbox, typicalBounds);
            }

            boolean passDeclutter = stationGrid.markIfClear(s.getBounds(), s);
            if (s.getSelected() || !declutter || passDeclutter) {
                s.setVisible(true);
                s.draw(g);
            } else {
                s.setVisible(false);
            }
        }

        // restore
        g.setTransform(world2Device);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, saveHint);

    }

    /**
     * Set the projection used by this renderer.
     *
     * @param project   projection to use
     */
    public void setProjection(ProjectionImpl project) {
        this.project = project;
        calcWorldPos();
    }

    //    public void setStationModelView(StationModelView sm) {
    //        this.smv = sm;
    //    }

    /**
     * Get the <code>StationModelView</code> used by this Renderer.
     * @return the station model view
     */
    public StationModelView getStationModelView() {
        return smv;
    }

    /**
     * set the projection position for the stations based on the projection.
     */
    private void calcWorldPos() {
        for (int i = 0; i < stations.size(); i++) {
            SLStation s = (SLStation) stations.get(i);
            s.worldPos.setLocation(project.latLonToProj(s.latlonPos));
        }
        posWasCalc = true;
    }

    ///////////////// pickable stuff

    /**
     * Find a station that is closest to the pickPt.
     * @param pickPt  point where user clicked.
     * @return closest station.
     */
    public SLStation find(Point2D pickPt) {
        if ((world2Normal == null) || (pickPt == null)
                || stations.isEmpty()) {
            return null;
        }

        world2Normal.transform(pickPt, ptN);  // work in normalized coordinate space

        double    distance = Double.MAX_VALUE;
        SLStation closest  = null;
        for (int i = 0; i < stations.size(); i++) {
            SLStation s = (SLStation) stations.get(i);
            if ( !s.getVisible()) {
                continue;
            }
            Rectangle2D bounds = s.getBounds();
            double tmpDistance = ptN.distance(bounds.getX()
                                     + bounds.getWidth() / 2.0, bounds.getY()
                                         + bounds.getHeight() / 2.0);
            if (tmpDistance < distance) {
                distance = tmpDistance;
                closest  = s;
            }
        }
        return closest;
    }

    /** map of station maps */
    private Properties stationNameMap;

    /** flag for whether we read the station name map */
    private boolean readStationNameMap = false;

    // inner class

    /**
     *  Specialized station class used by this Renderer.
     */
    public class SLStation {

        /** name of station */
        String name;

        /** lat/lon position */
        LatLonPointImpl latlonPos = new LatLonPointImpl();  // latlon pos

        /** world position */
        ProjectionPointImpl worldPos = new ProjectionPointImpl();  // world pos

        /** screen position */
        Point2D.Double screenPos = new Point2D.Double();  // normalized screen pos

        /** bounds */
        Rectangle bounds = new Rectangle();

        /** selected flag */
        private boolean selected = false;

        /** visible flag */
        private boolean visible = true;

        /** the station that this wraps */
        Station myStation;

        /**
         * Construct a new <code>SLStation</code> from a <code>Station</code>
         * object.
         * @param stn  <code>Station</code> to wrap.
         */
        SLStation(Station stn) {
            myStation = stn;

            String stationName = stn.toString();
            if ((stationNameMap == null) && !readStationNameMap) {
                readStationNameMap = true;
                try {
                    stationNameMap = Misc.readProperties(
                        "/ucar/unidata/view/station/station.properties",
                        null, getClass());
                } catch (Exception exc) {}
            }




            if (stationNameMap != null) {
                String displayName =
                    (String) stationNameMap.get(stn.toString());
                if (displayName != null) {
                    stationName = displayName;
                }
            }




            name = template;
            name = StringUtil.replace(name, StationLocationMap.LABEL_ID,
                                      stn.getIdentifier());
            name = StringUtil.replace(name, StationLocationMap.LABEL_NAME,
                                      stationName);
            name = StringUtil.replace(name, StationLocationMap.LABEL_LAT,
                                      "" + stn.getLatitude());
            name = StringUtil.replace(name, StationLocationMap.LABEL_LON,
                                      "" + stn.getLongitude());

            latlonPos.setLatitude(stn.getLatitude());
            latlonPos.setLongitude(stn.getLongitude());
            // current world position based on current projection
            worldPos.setLocation(project.latLonToProj(latlonPos));
        }





        /**
         * Get the station that this object is wrapping.
         * @return wrapped <code>Station</code>
         */
        public Station getStation() {
            return myStation;
        }

        /**
         * Get the location of this in projection coordinates.
         * @return projection location.
         */
        public ProjectionPointImpl getLocation() {
            return worldPos;
        }

        /**
         * Get whether this station is selected.
         * @return true if selected, otherwise false.
         */
        public boolean getSelected() {
            return selected;
        }

        /**
         * Set whether this station is selected.
         * @param selected  true to select.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }


        /**
         * Get whether this station is visible, i.e., if it has been drawn (not decluttered)
         * @return true if visible, otherwise false.
         */
        public boolean getVisible() {
            return visible;
        }

        /**
         * Set whether this station is visible.
         * @param visible  true to select.
         */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }



        /**
         * Get the bounds of this station.
         * @return  <code>Rectangle</code> that determines the bounds.
         */
        public Rectangle getBounds() {
            return bounds;
        }

        /**
         * Reset the bounds for this station
         */
        public void resetBounds() {
            stationSymbol.setValue(name);
            bounds = smv.getBounds(screenPos);
        }

        /**
         * Draw this station.
         * @param g  graphics object on which to draw
         */
        void draw(Graphics2D g) {
            smv.setColor((selected == true)
                         ? Color.red
                         : color);
            try {

                stationSymbol.setValue(name);
                cloudSymbol.setCoverage((selected == true)
                                        ? 100.0
                                        : 0.0);
                smv.draw(g, screenPos);
            } catch (Exception exc) {
                System.err.println("error:" + this);
            }
        }

        /**
         * String representation of this object
         * @return the string
         */
        public String toString() {
            return myStation.toString();
        }

    }  // end inner class SLStation




    /**
     * Popup a display that lists the station info.
     */
    class popupDisplay extends JDialog {

        /** information area for popup */
        private JTextArea info;

        /** position lat/lon */
        private LatLonPointImpl posLatLon = new LatLonPointImpl();

        /**
         * popup the display
         *
         */
        popupDisplay() {
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            getContentPane().add(mainPanel);
            setTitle("Selected Station Info");

            info = new JTextArea();
            info.setLineWrap(true);
            info.setColumns(50);
            info.setRows(10);
            mainPanel.add(info, BorderLayout.CENTER);

            //mainPanel.setPreferredSize(new Dimension(300, 200));
            pack();
            setLocation(200, 200);
        }

        /**
         * Show info for this station.
         * @param s  station to show
         */
        public void show(SLStation s) {
            if (debugClosest) {
                System.out.println("selected = " + s + " " + info);
            }

            posLatLon.setLatitude(s.latlonPos.getLatitude());
            posLatLon.setLongitude(s.latlonPos.getLongitude());

            info.setText("Station: " + s.name + "\n");
            info.append("  pos: " + posLatLon + "\n");

            super.show();
        }
    }

}

