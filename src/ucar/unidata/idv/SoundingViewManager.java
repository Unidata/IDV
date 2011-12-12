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

package ucar.unidata.idv;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.view.sounding.AerologicalDisplay;
import ucar.unidata.view.sounding.AerologicalDisplayConstants;

import ucar.visad.display.*;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;



/**
 * A wrapper around a sounding display (AerologicalDisplay) like a Skew-T
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class SoundingViewManager extends ViewManager implements AerologicalDisplayConstants {

    /** Prefix for preferences */
    public static final String PREF_PREFIX = ViewManager.PREF_PREFIX
                                             + "SOUNDING";

    /** the chart type */
    private String chartType = SKEWT_DISPLAY;

    /** saturation mixing ratio visibility */
    private boolean saturationMixingRatioVisibility = false;

    /** saturation adiabat visibility */
    private boolean saturationAdiabatVisibility = true;

    /** dry adiabat visibility */
    private boolean dryAdiabatVisibility = true;

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public SoundingViewManager() {}

    /**
     * Create a SoundingViewManager with the given context,
     * descriptor, object store and properties string.
     *
     * @param viewContext  Provides a context for the VM to be in.
     * @param desc         The ViewDescriptor that identifies this VM
     * @param properties   A set of ";" delimited name-value pairs.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public SoundingViewManager(ViewContext viewContext, ViewDescriptor desc,
                               String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }


    /**
     *  Create a SoundingViewManager with the given context, descriptor, object store,
     *  properties string and animation state
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param desc The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *  @param animationInfo Initial animation properties
     *
     * @throws RemoteException
     * @throws VisADException
     *
     */
    public SoundingViewManager(ViewContext viewContext, ViewDescriptor desc,
                               String properties, AnimationInfo animationInfo)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties, animationInfo);
    }


    /**
     *  Create a SoundingViewManager with the given context, display,
     *  descriptor, properties string
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param master  display master
     *  @param viewDescriptor The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *
     * @throws RemoteException
     * @throws VisADException
     *
     */
    public SoundingViewManager(ViewContext viewContext, DisplayMaster master,
                               ViewDescriptor viewDescriptor,
                               String properties)
            throws VisADException, RemoteException {
        this(viewContext, viewDescriptor, properties, null);
        setDisplayMaster(master);
    }

    /**
     * Initialize the view menu
     *
     * @param viewMenu the view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        showControlMenu = false;
        super.initializeViewMenu(viewMenu);
        viewMenu.add(makeColorMenu());
    }


    /**
     * Factory method for creating the display master
     *
     * @return The Display Master
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        AerologicalDisplay display =
            AerologicalDisplay.getInstance(chartType);
        setLineVisibility(display);
        return display;
    }

    /**
     * Set the sounding display
     *
     * @param ad  the sounding display
     */
    public void setSoundingDisplay(AerologicalDisplay ad) {
        try {
            setLineVisibility(ad);
        } catch (Exception e) {
            LogUtil.logException("setting line Visibility", e);
        }
        setDisplayMaster(ad);
    }


    /**
     * Don't show the side legend
     *
     * @return false
     */
    public boolean getShowSideLegend() {
        return false;
    }

    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Sounding View";
    }

    /**
     * Do we support animation?
     *
     * @return false
     */
    public boolean animationOk() {
        return false;
    }

    /**
     * Add a JTabbedPane to the properties component
     *
     * @param tabbedPane  the pane to add
     */
    protected void addPropertiesComponents(JTabbedPane tabbedPane) {
        AerologicalDisplay soundingDisplay = getAerologicalDisplay();

        List<JRadioButton> chartTypes      = new ArrayList<JRadioButton>();

        ButtonGroup        bg              = new ButtonGroup();

        JRadioButton       rb = makeChartTypeButton(SKEWT_DISPLAY);
        bg.add(rb);
        chartTypes.add(rb);

        rb = makeChartTypeButton(STUVE_DISPLAY);
        bg.add(rb);
        chartTypes.add(rb);

        rb = makeChartTypeButton(EMAGRAM_DISPLAY);
        bg.add(rb);
        chartTypes.add(rb);
        JPanel types = GuiUtils.left(GuiUtils.vbox(chartTypes));
        types.setBorder(new TitledBorder("Display Types"));

        List<JCheckBox> lineControls = new ArrayList<JCheckBox>();

        lineControls.add(GuiUtils.makeCheckbox("Dry Adiabats", this,
                "dryAdiabatVisibility"));
        lineControls.add(GuiUtils.makeCheckbox("Saturation Adiabats", this,
                "saturationAdiabatVisibility"));
        lineControls.add(GuiUtils.makeCheckbox("Mixing Ratio", this,
                "saturationMixingRatioVisibility"));
        JPanel lines = GuiUtils.left(GuiUtils.vbox(lineControls));
        lines.setBorder(new TitledBorder("Line Visibility"));

        JPanel comp = GuiUtils.topLeft(GuiUtils.hbox(GuiUtils.inset(types,
                          5), GuiUtils.inset(lines, 5)));

        tabbedPane.add("Chart", comp);

        super.addPropertiesComponents(tabbedPane);
    }

    /**
     * Make the chart type menu
     *
     * @param type chart type
     *
     * @return the JRadioButtonMenuItem menu
     */
    private JRadioButton makeChartTypeButton(String type) {

        JRadioButton rb = new JRadioButton(getTypeLabel(type),
                                           isChartType(type));
        rb.setActionCommand(type);
        rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton myRb = (JRadioButton) e.getSource();
                String       type = myRb.getActionCommand();
                setChartType(type);
            }
        });
        return rb;
    }

    /**
     * Apply the properties
     *
     * @return  true if successful
     */
    public boolean applyProperties() {

        try {

            getAerologicalDisplay().setCoordinateSystem(getChartType());
            setLineVisibility(getAerologicalDisplay());
        } catch (Exception excp) {
            return false;
        }
        return true;
    }

    /**
     * Set the background line visibility on the specified  display
     *
     * @param aeroDisplay   display to set
     *
     * @throws RemoteException  remote display problem
     * @throws VisADException   local display problem
     */
    private void setLineVisibility(AerologicalDisplay aeroDisplay)
            throws VisADException, RemoteException {
        if (aeroDisplay == null) {
            return;
        }
        aeroDisplay.setSaturationMixingRatioVisibility(
            saturationMixingRatioVisibility);
        aeroDisplay.setSaturationAdiabatVisibility(
            saturationAdiabatVisibility);
        aeroDisplay.setDryAdiabatVisibility(dryAdiabatVisibility);
    }

    /**
     * Get the label for the type of display
     *
     * @param chartType  type name
     *
     * @return the label
     */
    public static String getTypeLabel(String chartType) {
        if (chartType.equals(SKEWT_DISPLAY)) {
            return "Skew T";
        } else if (chartType.equals(STUVE_DISPLAY)) {
            return "Stuve";
        } else if (chartType.equals(EMAGRAM_DISPLAY)) {
            return "Emagram";
        }
        return chartType;
    }

    /**
     * Get the chart type.
     * @return chart type
     */
    public String getChartType() {
        return chartType;
    }

    /**
     * Set the chart type.
     * @param value   chart type
     */
    public void setChartType(String value) {
        chartType = value;
    }

    /**
     * See if the chart type in question is the same as this type.
     * @param type   chart type
     * @return true if chart types are the same
     */
    private boolean isChartType(String type) {
        return getChartType().equals(type);
    }

    /**
     * Get the saturated adiabat visibility
     * @return true if visiable
     */
    public boolean getSaturationAdiabatVisibility() {
        return saturationAdiabatVisibility;
    }

    /**
     * Set the saturated adiabat visibility
     * @param value  true if visiable
     */
    public void setSaturationAdiabatVisibility(boolean value) {
        saturationAdiabatVisibility = value;
        if (getHaveInitialized()) {
            try {
                setLineVisibility(getAerologicalDisplay());
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the dry adiabat visibility
     * @return true if visiable
     */
    public boolean getDryAdiabatVisibility() {
        return dryAdiabatVisibility;
    }

    /**
     * Set the dry adiabat visibility
     * @param value  true if visiable
     */
    public void setDryAdiabatVisibility(boolean value) {
        dryAdiabatVisibility = value;
        if (getHaveInitialized()) {
            try {
                setLineVisibility(getAerologicalDisplay());
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the saturation mixing ratio visibility
     * @return true if visiable
     */
    public boolean getSaturationMixingRatioVisibility() {
        return saturationMixingRatioVisibility;
    }

    /**
     * Set the saturation mixing ratio visibility
     * @param value  true if visiable
     */
    public void setSaturationMixingRatioVisibility(boolean value) {
        saturationMixingRatioVisibility = value;
        if (getHaveInitialized()) {
            try {
                setLineVisibility(getAerologicalDisplay());
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the display side coordinate system, subclasses should implement
     * if there is one.
     * @return  CoordinateSystem or null
     */
    public CoordinateSystem getDisplayCoordinateSystem() {
        return getAerologicalDisplay().getCoordinateSystem();
    }

    /**
     * Get the display, casting it to an AerologicalDisplay
     *
     * @return get the aerological display
     */
    private AerologicalDisplay getAerologicalDisplay() {
        return (AerologicalDisplay) getMaster();
    }
}
