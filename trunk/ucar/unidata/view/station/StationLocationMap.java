/*
 * Copyright  1997-2009 Unidata Program Center/University Corporation for
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


import ucar.unidata.beans.NonVetoableProperty;
import ucar.unidata.beans.Property;
import ucar.unidata.beans.PropertySet;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.metdata.Station;
import ucar.unidata.view.Renderer;
import ucar.unidata.view.geoloc.NavigatedPanel;
import ucar.unidata.view.geoloc.NewMapAreaEvent;
import ucar.unidata.view.geoloc.NewMapAreaListener;
import ucar.unidata.view.geoloc.PickEvent;
import ucar.unidata.view.geoloc.PickEventListener;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import java.util.ArrayList;

import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;


/**
 * A navigated panel with 2 renderers - a map renderer and a
 * station location renderer.
 * @author Don Murray
 */
public class StationLocationMap extends JPanel {

    /** Property for selecting a station */
    public static final String SELECTED_PROPERTY = "selectedStation";

    /** Property for unselecting a station */
    public static final String UNSELECTED_PROPERTY = "unselectedStation";

    /** Property for unselecting a station */
    public static final String ALL_UNSELECTED_PROPERTY = "unselectedAll";

    /** name of default map */
    public static final String DEFAULT_MAP = "/auxdata/maps/OUTLSUPW";


    /** ID label */
    public static final String LABEL_ID = "%ID%";

    /** Name label */
    public static final String LABEL_NAME = "%NAME%";

    /** latitude label */
    public static final String LABEL_LAT = "%LAT%";

    /** longitude label */
    public static final String LABEL_LON = "%LON%";


    /** Property to use the station ID for the station label */
    public static final String TEMPLATE_ID = LABEL_ID;

    /** template for name */
    public static final String TEMPLATE_NAME = LABEL_NAME;

    /** template for name and id */
    public static final String TEMPLATE_NAME_AND_ID = LABEL_ID + "("
                                                      + LABEL_NAME + ")";


    /** the navigated panel */
    private NavigatedPanel navigatedPanel;

    /** the map renderer */
    private Renderer mapRender = null;

    /** the station renderer */
    private StationLocationRenderer stnRender = null;

    /** the projection */
    private ProjectionImpl project;

    /** the transform */
    private AffineTransform atI = new AffineTransform();  // identity transform

    /** the selected station property */
    private Property selectedStationProperty;

    /** the unselected station property */
    private Property unselectedStationProperty;

    /** the unselected station property */
    private Property unselectAllProperty;

    /** the property set */
    private PropertySet propertySet;

    /** flag for multiple selection */
    private boolean multipleSelect = false;

    /** flag for a double click */
    private boolean wasDoubleClick = false;

    /** debugging */
    private boolean debugTime = false;

    /**
     * Default constructor.  Uses default label type and single station
     * selection.
     */
    public StationLocationMap() {
        this(false);
    }

    /**
     * Construct a new <code>StationLocationMap</code> using the specified
     * label type.  Use default station selection and map.
     * @param template The String template to use for displaying station labels.
     */
    public StationLocationMap(String template) {
        this(false, (String) null, template);
    }

    /**
     * Construct a new <code>StationLocationMap</code> using the specified
     * station selection type.  Use default labeling and map.
     * @param multipleSelect  true to allow multiple station selections
     */
    public StationLocationMap(boolean multipleSelect) {
        this(multipleSelect, (String) null, TEMPLATE_ID);
    }


    /**
     * Construct a new <code>StationLocationMap</code> using the specified
     * station selection type and map.  Use default labeling.
     * @param multipleSelect  true to allow multiple station selections
     * @param defaultMap      map to use
     */
    public StationLocationMap(boolean multipleSelect, String defaultMap) {
        this(multipleSelect, defaultMap, TEMPLATE_ID);
    }


    /**
     * Construct a new <code>StationLocationMap</code> using the specified
     * station selection type, map and template.
     * @param multipleSelect  true to allow multiple station selections
     * @param defaultMap      map to use
     * @param template The String template to use for displaying station labels.
     */
    public StationLocationMap(boolean multipleSelect, String defaultMap,
                              String template) {
        this(multipleSelect,
             new ucar.unidata.gis.mcidasmap.McidasMap((defaultMap != null)
                ? defaultMap
                : DEFAULT_MAP), template);

    }

    /**
     * Create a StationLocationMap
     *
     * @param multipleSelect  true if multiple selection is allowed
     * @param theMapRender the renderer for the map
     */
    public StationLocationMap(boolean multipleSelect, Renderer theMapRender) {
        this(multipleSelect, theMapRender, TEMPLATE_ID);
    }




    /**
     * Create a StationLocationMap
     *
     * @param multipleSelect  true if multiple selection is allowed
     * @param theMapRender the renderer for the map
     * @param template the template for the display of the data
     */
    public StationLocationMap(boolean multipleSelect, Renderer theMapRender,
                              String template) {

        this.mapRender      = theMapRender;
        this.multipleSelect = multipleSelect;

        getPropertySet().addProperty(selectedStationProperty =
            new NonVetoableProperty(this, SELECTED_PROPERTY));
        getPropertySet().addProperty(unselectedStationProperty =
            new NonVetoableProperty(this, UNSELECTED_PROPERTY));
        getPropertySet().addProperty(unselectAllProperty =
            new NonVetoableProperty(this, ALL_UNSELECTED_PROPERTY));

        // here's where the map will be drawn:
        navigatedPanel = new NavigatedPanel();

        /**
         * if(multipleSelect) {
         *   navigatedPanel.setToolTipText("<html>Left-drag: change region<br>Left-click: select station<br>Left-Shift-drag: select stations in region<br>Left-Ctrl-drag: add stations to selection</html>");
         * } else {
         *   navigatedPanel.setToolTipText("<html>Left-drag: change region<br>Left-click: select station</html>");
         *   }
         */



        stnRender = new StationLocationRenderer(multipleSelect, template);
        project   = navigatedPanel.getProjectionImpl();
        mapRender.setProjection(project);
        stnRender.setProjection(project);


        // get NewMapAreaEvents from the navigate object
        navigatedPanel.addNewMapAreaListener(new NewMapAreaListener() {
            public void actionPerformed(NewMapAreaEvent e) {
                if (project.isLatLon()) {
                    ProjectionRect boundingBox = navigatedPanel.getMapArea();
                    double wx0 = boundingBox.getX()
                                 + boundingBox.getWidth() / 2;
                    LatLonProjection llproj = (LatLonProjection) project;
                    if (llproj.getCenterLon() != wx0) {
                        llproj.setCenterLon(wx0);  // shift cylinder seam
                        wx0 = llproj.getCenterLon();  // normalize wx0 to  [-180,180]
                        // tell navigation panel to shift
                        navigatedPanel.setWorldCenterX(wx0);
                        // force recalc of world positions
                        mapRender.setProjection(project);
                        stnRender.setProjection(project);
                    }
                }
                redraw();
            }
        });

        // get Pick events from the navigated panel
        navigatedPanel.addPickEventListener(new PickEventListener() {
            public void actionPerformed(PickEvent e) {
                if (e.isPointSelect()) {
                    doPickPoint(e);
                } else {
                    doPickRegion(e);
                }
            }
        });


        navigatedPanel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (getMultipleSelect() && (e.getKeyCode() == KeyEvent.VK_A)
                        && e.isControlDown()) {
                    selectAll();
                } else if ((e.getKeyCode() == KeyEvent.VK_R)
                           && e.isControlDown()) {
                    navigatedPanel.resetZoom();
                }
            }
        });

        navigatedPanel.setBorder(BorderFactory.createEtchedBorder());
        navigatedPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                //Don't do this:
                //                navigatedPanel.requestFocus();
            }

            public void mouseClicked(MouseEvent e) {
                navigatedPanel.requestFocus();
                if ( !getMultipleSelect()) {
                    return;
                }
                // pick event
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                JPopupMenu popup = new JPopupMenu();
                JMenuItem  mi    = new JMenuItem("Select All");
                mi.setAccelerator(KeyStroke.getKeyStroke(new Character('A'),
                        InputEvent.CTRL_MASK));
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectAll();
                    }
                });
                popup.add(mi);

                mi = new JMenuItem("Clear Selection");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unselectSelected();
                        redraw();
                    }
                });
                popup.add(mi);
                popup.show(navigatedPanel, e.getX(), e.getY());
            }
        });



        // set up the display
        setLayout(new BorderLayout());
        navigatedPanel.setPreferredSize(new Dimension(400, 300));
        JPanel    toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox declutCB  = new JCheckBox("Declutter", true);
        declutCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDeclutter(((JCheckBox) e.getSource()).isSelected());
            }
        });
        toolPanel.add(declutCB);
        toolPanel.add(navigatedPanel.getNavToolBar());
        toolPanel.add(navigatedPanel.getMoveToolBar());
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EtchedBorder());
        JLabel positionLabel = new JLabel("position");
        statusPanel.add(positionLabel, BorderLayout.CENTER);
        navigatedPanel.setPositionLabel(positionLabel);
        add(toolPanel, BorderLayout.SOUTH);
        add(navigatedPanel, BorderLayout.CENTER);
        //add(statusPanel, BorderLayout.SOUTH);
    }




    /**
     * Select the station
     *
     * @param s
     */
    private void select(StationLocationRenderer.SLStation s) {
        select(s, false);
    }



    /**
     * Select the station
     *
     * @param s the station
     * @param wasDoubleClick true if a double click
     */
    private void select(StationLocationRenderer.SLStation s,
                        boolean wasDoubleClick) {
        this.wasDoubleClick = wasDoubleClick;
        s.setSelected(true);
        try {
            selectedStationProperty.setValueAndNotifyListeners(
                s.getStation());
            selectedStationProperty.clearValue();
        } catch (java.beans.PropertyVetoException pve) {}
        this.wasDoubleClick = false;
    }

    /**
     * Unselect a station
     *
     * @param s  the station
     */
    private void unselect(StationLocationRenderer.SLStation s) {
        s.setSelected(false);
        try {
            unselectedStationProperty.setValueAndNotifyListeners(
                s.getStation());
            unselectedStationProperty.clearValue();
        } catch (java.beans.PropertyVetoException pve) {}
    }


    /**
     * Pick a region
     *
     * @param pickEvent  the event that specifies the region
     */
    private void doPickRegion(PickEvent pickEvent) {
        //If it is not a zoom then it is a group selection.
        if ( !isMultipleSelect()) {
            return;
        }
        //For now make all group picks additive picks to fix a Mac problem
        //with the control key
        //        boolean isAdditivePick = pickEvent.getMouseEvent().isControlDown();
        boolean isAdditivePick = true;
        if ( !isAdditivePick) {
            unselectSelected();
        }
        Rectangle2D                       pickBounds = pickEvent.getBounds();
        List                              stations   =
            stnRender.getStations();
        StationLocationRenderer.SLStation station;
        for (int i = 0; i < stations.size(); i++) {
            station = (StationLocationRenderer.SLStation) stations.get(i);
            if (station.getSelected() || !station.getVisible()) {
                continue;
            }
            Rectangle2D stationBounds = station.getBounds();
            if (stationBounds.intersects(pickBounds.getX(),
                                         pickBounds.getY(),
                                         pickBounds.getWidth(),
                                         pickBounds.getHeight())) {
                select(station);
            }
        }
        redraw();
    }



    /**
     * Process the <code>PickEvent</code>.
     * @param e <code>PickEvent</code> to process
     */
    protected void doPickPoint(PickEvent e) {
        StationLocationRenderer.SLStation closest =
            stnRender.find(e.getLocation());
        if (closest == null) {
            return;
        }
        MouseEvent mouseEvent = e.getMouseEvent();

        //For now make all group picks additive picks to fix a Mac problem
        //with the control key
        boolean isAdditivePick = true;
        //        boolean    isAdditivePick     = mouseEvent.isControlDown();
        boolean closestWasSelected = closest.getSelected();
        if ( !multipleSelect || !isAdditivePick) {
            unselectSelected();
        }
        if (multipleSelect) {
            if (closestWasSelected) {
                unselect(closest);
            } else {
                select(closest);
            }
        } else {
            select(closest, mouseEvent.getClickCount() > 1);
        }
        redraw();
    }


    /**
     *  Call unselect for all of the stations in the selected list.
     */
    private void unselectSelected() {
        setSelectedStations(new ArrayList());
        unselectAllProperty.notifyListeners();
    }




    /**
     * Get the isFocusTransverable property
     * @return true if we can transverse focus
     */
    public boolean isFocusable() {
        return true;
    }


    /**
     *  Select all visible stations.
     */
    protected void selectAll() {
        List                              stations = stnRender.getStations();
        StationLocationRenderer.SLStation station;
        for (int i = 0; i < stations.size(); i++) {
            station = (StationLocationRenderer.SLStation) stations.get(i);
            if ( !station.getSelected()) {
                select(station);
            }
        }

        redraw();
    }





    /**
     * See if this map allows multiple station selection
     * @return true if allows multiple selection.
     */
    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    /**
     * Get the list of selected stations.
     * @return <code>List</code> of stations that have been selected.
     */
    public List getSelectedStations() {
        List                              selectedStations = new ArrayList();
        List                              stations = stnRender.getStations();
        StationLocationRenderer.SLStation station;

        for (int i = 0; i < stations.size(); i++) {
            station = (StationLocationRenderer.SLStation) stations.get(i);
            if (station.getSelected()) {
                selectedStations.add(station.getStation());
            }
        }
        return selectedStations;
    }

    /**
     * Access to the navigated panel.
     *
     * @return  navigated panel object
     */
    public NavigatedPanel getNavigatedPanel() {
        return navigatedPanel;
    }

    /**
     * Set the map renderer for this object
     *
     * @param   r   map renderer
     */
    public void setMapRenderer(Renderer r) {
        mapRender = r;
        mapRender.setProjection(project);
        redraw();
    }


    /**
     * Get the map renderer for this object
     *
     * @return  Map renderer
     */
    public Renderer getMapRender() {
        return mapRender;
    }

    /**
     * Access to the station location renderer
     *
     * @return  station location renderer
     */
    public StationLocationRenderer getStationLocationRenderer() {
        return stnRender;
    }

    /**
     * Set the station location renderer.
     *
     * @param   r  station location renderer
     */
    public void setStationRenderer(Renderer r) {
        stnRender = (StationLocationRenderer) r;
        stnRender.setProjection(project);
        redraw();
    }


    /**
     * Set the list of selected stations.
     * @param  stns <code>List</code> of stations to set selected.
     */
    public void setSelectedStations(List stns) {
        stnRender.setSelectedStations(stns);
        redraw();
    }


    /**
     * Set the list of stations to be displayed.  Stations will be
     * decluttered when they are displayed.
     *
     * @param   stns        list of stations
     */
    public void setStations(List stns) {
        setStations(stns, true);
    }

    /**
     * Set the list of stations to be displayed.  Stations will be
     * decluttered or not based on the value of <code>declutter</code>
     *
     * @param   stns        list of stations
     * @param   declutter   display will be decluttered if true, not if false
     */
    public void setStations(List stns, boolean declutter) {
        setStations(stns, null, declutter);
    }

    /**
     * Set the list of stations to be displayed.  Stations will be
     * decluttered or not based on the value of <code>declutter</code>
     *
     * @param   stns        list of stations
     * @param   selectedStations already selected stations
     * @param   declutter   display will be decluttered if true, not if false
     */
    public void setStations(List stns, List selectedStations,
                            boolean declutter) {
        stnRender.setStations(stns, selectedStations, declutter);
        redraw();
    }

    /**
     * Get the list of stations
     *
     * @return the list of stations
     */
    public List getStations() {
        return stnRender.getStations();
    }


    /**
     *  Change the state of decluttering
     *
     * @param  declut   station display will be decluttered if true, not if
     *                  false
     */
    public void setDeclutter(boolean declut) {
        stnRender.setDeclutter(declut);
        redraw();
    }

    /**
     * Convenience method to clear out the data in the station renderer
     */
    public void clearStations() {
        setStations(new ArrayList(0));
    }

    /**
     * Determine whether decluttering is on for this map.
     * @return true if map is being decluttered.
     */
    public boolean getDeclutter() {
        return stnRender.getDeclutter();
    }

    /**
     * Set the projection to use for this map.
     * @param p  projection to use
     */
    public void setProjectionImpl(ProjectionImpl p) {
        mapRender.setProjection(p);
        stnRender.setProjection(p);
        navigatedPanel.setProjectionImpl(p);
        project = p;
        redraw();
    }

    /**
     * Adds a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertySet().addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        getPropertySet().removePropertyChangeListener(listener);
    }

    /**
     * Adds a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        getPropertySet().addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {
        getPropertySet().removePropertyChangeListener(name, listener);
    }

    /**
     *  Redraw the graphics on the screen.
     */
    public void redraw() {
        java.awt.Graphics2D gNP = navigatedPanel.getBufferedImageGraphics();
        if (gNP == null) {  // panel not drawn on screen yet
            return;
        }

        // clear it
        gNP.setBackground(navigatedPanel.getBackgroundColor());
        java.awt.Rectangle r = gNP.getClipBounds();
        gNP.clearRect(r.x, r.y, r.width, r.height);

        mapRender.draw(gNP, atI);
        stnRender.draw(gNP, atI);
        gNP.dispose();

        // copy buffer to the screen
        navigatedPanel.repaint();
    }

    /**
     * See if this supports multiple station selection.
     *
     * @return true if this allows multiple station selection
     */
    public boolean getMultipleSelect() {
        return multipleSelect;
    }


    /**
     * test with ucar.unidata.view.station.StationLocationMap
     *
     * @param args
     */
    public static void main(String[] args) {
        StationLocationMap slm   = new StationLocationMap();
        JFrame             frame = new JFrame("Station Location Map Test");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(slm);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * See if a double click happened.
     *
     * @return true if a double click happened
     */
    public boolean getWasDoubleClick() {
        return wasDoubleClick;
    }

    /**
     * Returns the PropertyChangeListener-s of this instance.
     * @return                  The PropertyChangeListener-s.
     */
    private PropertySet getPropertySet() {
        if (propertySet == null) {
            synchronized (this) {
                if (propertySet == null) {
                    propertySet = new PropertySet();
                }
            }
        }
        return propertySet;
    }

}

