/*
 * $Id: GeoSelectionPanel.java,v 1.17 2006/12/27 20:16:49 jeffmc Exp $
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


package ucar.unidata.data;


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;



import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HashCodeUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.*;
import ucar.unidata.view.geoloc.NavigatedMapPanel;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Holds geo-location information  - lat/lon bounding box, image size, etc.
 * This is used to pass information from a chooser into a datasource.
 */
public class GeoSelectionPanel extends JPanel {


    /**
     * This is the extra component at the top of the panel. It can be null. It is used
     * for, e.g., to show the grid size
     */
    private JComponent extraComponent;

    /** Shows the stride widgets */
    private JComponent strideComponent;

    /** Shows the area widgets */
    private JComponent areaComponent;

    /** For the properties */
    private static final int[] STRIDE_VALUES = {
        GeoSelection.STRIDE_NONE, GeoSelection.STRIDE_BASE, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 15, 20, 50, 100
    };

    /** For the properties */
    private static final String[] PT_STRIDE_LABELS = {
        "Undefined", "All points", "Every other point", "Every third point",
        "Every fourth point", "Every fifth point", "Every sixth point",
        "Every seventh point", "Every eighth point", "Every ninth point",
        "Every tenth point", "Every fifteenth point", "Every twentieth point",
        "Every fiftieth point", "Every hundredth "
    };

    /** For the properties */
    private static final String[] PT_STRIDE_LABELS_SHORT = {
        "Undefined", "All", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th",
        "9th", "10th", "15th", "20th", "50th", "100th"
    };




    /** For the properties */
    private static final String[] LEVEL_STRIDE_LABELS = {
        "Undefined", "All levels", "Every other level", "Every third level",
        "Every fourth level", "Every fifth level", "Every sixth level",
        "Every seventh level", "Every eighth level", "Every ninth level",
        "Every tenth level", "Every fifteenth level", "Every twentieth level",
        "Every fiftieth level", "Every hundredth "
    };



    /** The geo selection */
    private GeoSelection geoSelection;


    /** for properties */
    private JComboBox xStrideBox;

    /** for properties */
    private JComboBox yStrideBox;

    /** for properties */
    private JComboBox zStrideBox;

    /** for properties */
    private NavigatedMapPanel mapPanel;

    /** for properties */
    private LatLonWidget ulLatLon;

    /** for properties */
    private LatLonWidget lrLatLon;

    /** for gui */
    private JCheckBox indexCbx;

    /** for properties */
    private JComponent latLonPanel;

    /** for properties */
    private boolean ignoreBoxChanges = false;

    /** Are we enabled */
    private boolean enabled = false;

    /** for properties */
    private JToggleButton lockBtn;

    /** for properties */
    private JCheckBox enabledCbx;


    /**
     * ctor
     *
     * @param geoSelection The geo selection
     * @param fullVersion If true we show the decimation along with the area subset in one component
     * @param enabled Initially enabled
     * @param doStride Show decimation
     * @param doBoundingBox Show area subset
     * @param sampleProjection Sample projection for the area map. May be null.
     * @param extraComponent Extra stuff for gui. example: the grid size label. May be null.
     */
    public GeoSelectionPanel(GeoSelection geoSelection, boolean fullVersion,
                             boolean enabled, boolean doStride,
                             boolean doBoundingBox,
                             ProjectionImpl sampleProjection,
                             JComponent extraComponent) {
        this.geoSelection   = geoSelection;
        this.extraComponent = extraComponent;
        this.enabled        = enabled;
        setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER,
                 makePanel(fullVersion, doStride, doBoundingBox,
                           sampleProjection));
        if (enabledCbx != null) {
            GuiUtils.enableTree(this, enabled);
            GuiUtils.enableTree(enabledCbx, true);
        }
    }




    /**
     * Utility to make a stride combobox
     *
     * @param stride The stride value
     * @param labels The labels to use in the combobox
     *
     * @return The widget
     */
    private JComboBox makeBox(int stride, String[] labels) {
        final JComboBox box = GuiUtils.makeComboBox(STRIDE_VALUES, labels,
                                  stride);

        //FOr now do include the STRIDE_NONE in the combo box
        //        final JComboBox box = GuiUtils.makeComboBox(STRIDE_VALUES, labels,
        //                                  ((stride != GeoSelection.STRIDE_NONE)
        //                                   ? stride
        //                                   : GeoSelection.STRIDE_BASE));
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (ignoreBoxChanges || !lockBtn.isSelected()) {
                    return;
                }
                if (zStrideBox == box) {
                    return;
                }
                ignoreBoxChanges = true;
                Object obj = box.getSelectedItem();
                xStrideBox.setSelectedItem(obj);
                yStrideBox.setSelectedItem(obj);
                //                zStrideBox.setSelectedItem(obj);
                ignoreBoxChanges = false;
            }
        });
        return box;
    }


    /**
     * Initialize this object with the given object
     *
     * @param p object to init with
     */
    public void initWith(GeoSelectionPanel p) {
        if ((xStrideBox != null) && (p.xStrideBox != null)) {
            xStrideBox.setSelectedItem(p.xStrideBox.getSelectedItem());
        }
        if ((yStrideBox != null) && (p.yStrideBox != null)) {
            yStrideBox.setSelectedItem(p.yStrideBox.getSelectedItem());
        }

        if ((zStrideBox != null) && (p.zStrideBox != null)) {
            zStrideBox.setSelectedItem(p.zStrideBox.getSelectedItem());
        }
        if ((lockBtn != null) && (p.lockBtn != null)) {
            lockBtn.setSelected(p.lockBtn.isSelected());
        }
        if ((mapPanel != null) && (p.mapPanel != null)) {
            if (mapPanel.getProjectionImpl().equals(
                    p.mapPanel.getProjectionImpl())) {
                NavigatedPanel np = mapPanel.getNavigatedPanel();
                if (p.mapPanel.getNavigatedPanel().getSelectedRegion()
                        != null) {
                    np.setSelectedRegion(
                        p.mapPanel.getNavigatedPanel().getSelectedRegion());
                }
                mapPanel.repaint();
            }

        }
    }



    /**
     * Make the panel for the propeties dialog
     *
     *
     * @param fullVersion If true we show the decimation along with the area subset in one component
     * @param doStride Show stride widgets
     * @param doBoundingBox Show subset widget
     * @param sampleProjection Use in map panel. May be null.
     *
     * @return The properties panel
     */
    private JComponent makePanel(boolean fullVersion, boolean doStride,
                                 boolean doBoundingBox,
                                 ProjectionImpl sampleProjection) {

        List boxComps    = new ArrayList();
        List strideComps = new ArrayList();
        if (doStride) {
            lockBtn =
                GuiUtils.getToggleButton("/auxdata/ui/icons/link_break.png", 0,
                                         0);
            lockBtn.setContentAreaFilled(false);
            lockBtn.setSelectedIcon(
                GuiUtils.getImageIcon(
                    "/auxdata/ui/icons/link.png", getClass()));
            lockBtn.setSelected(geoSelection.getXStride()
                                == geoSelection.getYStride());
            lockBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            lockBtn.setToolTipText("Link stride changes between x & y");


            xStrideBox = makeBox(geoSelection.getXStride(), (fullVersion
                    ? PT_STRIDE_LABELS
                    : PT_STRIDE_LABELS));

            strideComps.add(GuiUtils.rLabel((fullVersion
                                             ? "X Stride:"
                                             : "X:")));
            strideComps.add(GuiUtils.left(GuiUtils.hbox(xStrideBox,
                    new JLabel("      "), lockBtn)));

            yStrideBox = makeBox(geoSelection.getYStride(), (fullVersion
                    ? PT_STRIDE_LABELS
                    : PT_STRIDE_LABELS));
            strideComps.add(GuiUtils.rLabel((fullVersion
                                             ? "Y Stride:"
                                             : "Y:")));
            strideComps.add(GuiUtils.left(yStrideBox));

            zStrideBox = makeBox(geoSelection.getZStride(), (fullVersion
                    ? LEVEL_STRIDE_LABELS
                    : PT_STRIDE_LABELS));
            strideComps.add(GuiUtils.rLabel((fullVersion
                                             ? "Level Stride:"
                                             : "Level:")));
            strideComps.add(GuiUtils.left(zStrideBox));
        }

        if (doBoundingBox) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setBoundsFromFields();
                }
            };

            indexCbx = new JCheckBox("Use indices");

            ulLatLon = new LatLonWidget("Lat:", "Lon:", actionListener);
            lrLatLon = new LatLonWidget("Lat:", "Lon:", actionListener);
            ulLatLon.setDoFormat(false);
            lrLatLon.setDoFormat(false);
            latLonPanel = GuiUtils.doLayout(new Component[] {
                GuiUtils.inset(GuiUtils.wrap(ulLatLon.getLatField()), 5),
                GuiUtils.hbox(ulLatLon.getLonField(), lrLatLon.getLonField(),
                              5),
                GuiUtils.inset(GuiUtils.wrap(lrLatLon.getLatField()),
                               5) }, 1, GuiUtils.WT_N, GuiUtils.WT_N);
            //            JPanel leftPanel = GuiUtils.vbox(latLonPanel, indexCbx);
            JComponent leftPanel = latLonPanel;
            //      latLonPanel = GuiUtils.inset(latLonPanel,5);


            mapPanel = new MyNavigatedMapPanel(this, sampleProjection,
                    fullVersion);
            if (fullVersion) {
                mapPanel.setPreferredSize(new Dimension(400, 300));
            } else {
                //                mapPanel.setPreferredSize(new Dimension(200, 200));
            }


            if (geoSelection.getBoundingBox() != null) {
                GeoLocationInfo bb = geoSelection.getBoundingBox();
                selectedRegionChanged(bb.getLatLonRect());
            } else {
                selectedRegionChanged(null);
            }
            /*
              mapPanel.setDrawBounds(boundingBox.getMinLon(),
                                   boundingBox.getMaxLat(),
                                   boundingBox.getMaxLon(),
                                   boundingBox.getMinLat());*/

            boxComps.add(
                GuiUtils.topCenter(
                    GuiUtils.inset(
                        GuiUtils.rLabel("Bounding Box:"),
                        new Insets(10, 0, 0, 0)), GuiUtils.inset(
                            leftPanel, new Insets(0, 0, 60, 0))));
            boxComps.add(mapPanel);
        }


        if (fullVersion) {
            List comps = new ArrayList(strideComps);
            comps.addAll(boxComps);
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            return GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                     GuiUtils.WT_NNNY);
        } else {
            JTabbedPane tab = GuiUtils.getNestedTabbedPane();
            enabledCbx = new JCheckBox("Enabled", enabled);
            enabledCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    GuiUtils.enableTree(GeoSelectionPanel.this,
                                        enabledCbx.isSelected());
                    GuiUtils.enableTree(enabledCbx, true);
                }
            });

            if (strideComps.size() > 0) {
                //                strideComps.add(0, enabledCbx);
                //                strideComps.add(0, new JLabel(" "));
                GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
                tab.addTab(
                    "Stride",
                    strideComponent = GuiUtils.topLeft(
                        GuiUtils.doLayout(
                            strideComps, 2, GuiUtils.WT_N, GuiUtils.WT_N)));
            }
            if (doBoundingBox) {
                if (strideComps.size() == 0) {
                    tab.add("Area",
                            GuiUtils.topCenter(GuiUtils.left(enabledCbx),
                                mapPanel));
                } else {
                    tab.add("Area", mapPanel);
                }
                //                areaComponent = mapPanel;
            }
            return tab;
        }
    }


    /**
     * Get the component that shows the stride. Used by the DataControlDialog
     * to show the stride in  a separate tab
     *
     * @return stride component
     */
    public JComponent getStrideComponent() {
        if ((strideComponent != null) && (extraComponent != null)) {
            return GuiUtils.vbox(extraComponent, strideComponent);
        }
        return strideComponent;
    }


    /**
     * Get the component that shows the area map. Used by the DataControlDialog
     * to show the map in  a separate tab
     *
     * @return area map component
     */
    public JComponent getAreaComponent() {
        if ((areaComponent == null) && (mapPanel != null)) {
            JLabel tmp  = new JLabel(" ");
            Font   font = tmp.getFont();
            font = font.deriveFont((float) (font.getSize() - 2.0f));
            mapPanel.setPreferredSize(new Dimension(100, 100));
            ulLatLon.getLatField().setColumns(4);
            ulLatLon.getLonField().setColumns(4);
            lrLatLon.getLonField().setColumns(4);
            lrLatLon.getLatField().setColumns(4);
            GuiUtils.setFontOnTree(latLonPanel, font);
            GuiUtils.tmpInsets = new Insets(3, 0, 0, 0);
            latLonPanel = GuiUtils.vbox(
                GuiUtils.wrap(ulLatLon.getLatField()),
                GuiUtils.hbox(
                    ulLatLon.getLonField(), lrLatLon.getLonField(),
                    3), GuiUtils.wrap(lrLatLon.getLatField()));
            //            areaComponent = GuiUtils.doLayout(new Component[]{latLonPanel, mapPanel},2, GuiUtils.WT_NY, GuiUtils.WT_Y);
            JPanel leftPanel = GuiUtils.vbox(latLonPanel, indexCbx);
            //            JSplitPane splitPane  = GuiUtils.hsplit(leftPanel, mapPanel,100,0.5);
            //            splitPane.setOneTouchExpandable(true); 
            //            areaComponent =  splitPane;
            areaComponent = mapPanel;
        }
        return areaComponent;
    }

    /**
     * Get the selected latlon area rect. May be null
     *
     * @return Area rectangle
     */
    private LatLonRect getLatLonRect() {
        if ((ulLatLon != null) && ulLatLon.isLatLonDefined()
                && lrLatLon.isLatLonDefined()) {
            //System.err.println("getlatlon-2");
            LatLonPoint ul = new LatLonPointImpl(ulLatLon.getLat(),
                                 ulLatLon.getLon());
            LatLonPoint lr = new LatLonPointImpl(lrLatLon.getLat(),
                                 lrLatLon.getLon());
            return new LatLonRect(ul, lr);
        }
        //System.err.println(
        //    "getlatlon-1: "
        //    + mapPanel.getNavigatedPanel().getSelectedEarthRegion());
        return mapPanel.getNavigatedPanel().getSelectedEarthRegion();
    }


    /**
     * Set the bounds from the field
     */
    private void setBoundsFromFields() {
        NavigatedPanel np = mapPanel.getNavigatedPanel();
        np.setSelectedRegion(getLatLonRect());
        mapPanel.repaint();
    }


    /**
     * Is enabled
     *
     * @return enabled
     */
    public boolean getEnabled() {
        if (enabledCbx == null) {
            return true;
        }
        return enabledCbx.isSelected();
    }




    /**
     * Apply the gui settings to the geo selection and return it
     *
     * @return current geo selection
     */
    public GeoSelection getGeoSelection() {
        applyProperties(geoSelection);
        return new GeoSelection(geoSelection);
    }



    /**
     * Apply the properties
     *
     *
     * @param geoSelection The geo selection to apply to
     * @return false if it failed
     */
    public boolean applyProperties(GeoSelection geoSelection) {
        if (xStrideBox != null) {
            geoSelection.setXStride(GuiUtils.getValueFromBox(xStrideBox));
            if (geoSelection.getXStride() == GeoSelection.STRIDE_BASE) {
                //                geoSelection.setXStride(GeoSelection.STRIDE_NONE);
            }
        }

        if (yStrideBox != null) {
            geoSelection.setYStride(GuiUtils.getValueFromBox(yStrideBox));
            if (geoSelection.getYStride() == GeoSelection.STRIDE_BASE) {
                //                geoSelection.setYStride(GeoSelection.STRIDE_NONE);
            }
        }

        if (zStrideBox != null) {
            geoSelection.setZStride(GuiUtils.getValueFromBox(zStrideBox));
            if (geoSelection.getZStride() == GeoSelection.STRIDE_BASE) {
                //                geoSelection.setZStride(GeoSelection.STRIDE_NONE);
            }
        }


        if (mapPanel != null) {
            //xxxx
            LatLonRect llr = getLatLonRect();
            if (llr == null) {
                geoSelection.setBoundingBox(null);
                if ((enabledCbx != null) && enabledCbx.isSelected()) {
                    geoSelection.setUseFullBounds(true);
                }
            } else {
                geoSelection.setBoundingBox(new GeoLocationInfo(llr));
            }
        }


        return true;
    }


    /**
     * Region changed. Update the latlon widgets
     *
     * @param llr new region
     */
    protected void selectedRegionChanged(LatLonRect llr) {
        if (ulLatLon == null) {
            //            System.err.println("region changed ullatlon is null" );
            return;
        }
        GuiUtils.enableTree(latLonPanel, llr != null);
        if (llr == null) {
            return;
        }
        LatLonPointImpl    ul = llr.getUpperLeftPoint();
        LatLonPointImpl    lr = llr.getLowerRightPoint();
        DisplayConventions dc = DisplayConventions.getDisplayConventions();
        /*        ulLatLon.setLatLon(dc.formatLatLon(ul.getLatitude()),
                           dc.formatLatLon(ul.getLongitude()));
        lrLatLon.setLatLon(dc.formatLatLon(lr.getLatitude()),
                           dc.formatLatLon(lr.getLongitude()));
        */
        ulLatLon.setLatLon(ul.getLatitude(),
                           ul.getLongitude());
        lrLatLon.setLatLon(lr.getLatitude(),
                           lr.getLongitude());
    }






    /**
     * Class MyNavigatedMapPanel shows the subset panel
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.17 $
     */
    public static class MyNavigatedMapPanel extends NavigatedMapPanel {

        /** The panel_ */
        GeoSelectionPanel geoPanel;

        /** Points to draw */
        private List points = new ArrayList();


        /**
         * ctor
         *
         *
         * @param geoPanel The panel
         * @param sampleProjection may be null
         * @param fullVersion full version or truncated
         */
        public MyNavigatedMapPanel(GeoSelectionPanel geoPanel,
                                   ProjectionImpl sampleProjection,
                                   boolean fullVersion) {
            super(true, fullVersion);
            this.geoPanel = geoPanel;
            NavigatedPanel np = getNavigatedPanel();

            if (sampleProjection != null) {
                setProjectionImpl(sampleProjection);
                ProjectionRect r = np.normalizeRectangle(
                                       sampleProjection.getDefaultMapArea());
                np.setSelectedRegionBounds(r);
                points.add(new ProjectionPointImpl(r.getX(), r.getY()));
                points.add(new ProjectionPointImpl(r.getX() + r.getWidth(),
                        r.getY()));
                points.add(new ProjectionPointImpl(r.getX() + r.getWidth(),
                        r.getY() + r.getHeight()));
                points.add(new ProjectionPointImpl(r.getX(),
                        r.getY() + r.getHeight()));
                //                System.err.println("rect:" + r);
                //                System.err.println ("Points:" + points);
            }
            np.setSelectRegionMode(true);

            if (geoPanel.geoSelection.getBoundingBox() != null) {
                np.setSelectedRegion(
                    geoPanel.geoSelection.getBoundingBox().getLatLonRect());
            }
            np.zoom(0.6);
        }



        /**
         * Update
         *
         * @param llr new  region
         */
        protected void _selectedRegionChanged(LatLonRect llr) {
            geoPanel.selectedRegionChanged(llr);
        }


        /**
         * Make panel
         *
         * @return map panel
         */
        protected NavigatedPanel doMakeMapPanel() {
            return new NavigatedPanel() {
                protected void selectedRegionChanged() {
                    super.selectedRegionChanged();
                    LatLonRect llr = getSelectedEarthRegion();
                    _selectedRegionChanged(llr);
                }
            };
        }

        /**
         * Draw extra stuff on the map
         *
         * @param gNP Graphics to draw into
         */
        protected void annotateMap(Graphics2D gNP) {
            super.annotateMap(gNP);
            NavigatedPanel np = getNavigatedPanel();
            if (points.size() == 0) {
                return;
            }
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                   points.size());
            for (int i = 0; i <= points.size(); i++) {
                ProjectionPoint ppi;
                LatLonPoint     llp;
                if (i >= points.size()) {
                    ppi = (ProjectionPoint) points.get(0);
                } else {
                    ppi = (ProjectionPoint) points.get(i);
                }
                //                System.err.println ("\t" + ppi);
                if (i == 0) {
                    path.moveTo((float) ppi.getX(), (float) ppi.getY());
                } else {
                    path.lineTo((float) ppi.getX(), (float) ppi.getY());
                }
            }
            //            gNP.setColor(Color.red);
            //            gNP.fillRect(10, 10, 50, 50);
            gNP.draw(path);
        }


    }




}

