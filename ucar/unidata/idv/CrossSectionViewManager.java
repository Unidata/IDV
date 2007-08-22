/*
 * $Id: CrossSectionViewManager.java,v 1.67 2006/12/27 20:14:06 jeffmc Exp $
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

package ucar.unidata.idv;


import ucar.unidata.collab.*;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.xml.XmlObjectStore;



import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import java.awt.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;



/**
 * A wrapper around a Cross Section (XSDisplay) display master.
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class CrossSectionViewManager extends ViewManager {

    /** Prefix for preferences */
    public static final String PREF_PREFIX = ViewManager.PREF_PREFIX + "XS";

    /** Preference for clipping at 3d box_ */
    public static final String PREF_CLIP = PREF_PREFIX + ".clip";

    /** clip state */
    private boolean clipOn = true;


    /** A border used in the gui. Keep this around to change the title */
    private TitledBorder csBorderTitle;

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public CrossSectionViewManager() {}

    /**
     *  Create a CrossSectionViewManager with the given context, descriptor, object store
     *  and properties string.
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param desc The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public CrossSectionViewManager(ViewContext viewContext,
                                   ViewDescriptor desc, String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }



    /**
     *  Create a CrossSectionViewManager with the given context, descriptor, object store,
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
    public CrossSectionViewManager(ViewContext viewContext,
                                   ViewDescriptor desc, String properties,
                                   AnimationInfo animationInfo)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties, animationInfo);
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
        viewMenu.addSeparator();
        viewMenu.add(GuiUtils.makeMenuItem("Properties", this,
                                           "showPropertiesDialog"));
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
        return new VerticalXSDisplay();
    }


    /**
     * Initializr this object. This creates and initializes
     * the {@link ucar.visad.display.XSDisplay}.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void init() throws VisADException, RemoteException {

        VerticalXSDisplay csDisplay = (VerticalXSDisplay) getMaster();


        // squash z axis height or aspect ratio to same as 3d display;
        // and zoom in by a factor of 3.0 from the small default size.
        //csDisplay.setDisplayAspect(new double[] { .65, .65, 1.0} );
        csDisplay.setAspect(1.0, .4);
        super.init();


        /* TODO
        // To enable middle button sampling in the "csDisplay,"
        // to pick up changes in cursor position values:
        PropertyChangeListener labelListener =   new PropertyChangeListener () {
                public void propertyChange (PropertyChangeEvent event) {
                    setCSValueLabel ();
                }
            };

        csDisplay.addPropertyChangeListener (
            csDisplay.CURSOR_XVALUE, labelListener);
        csDisplay.addPropertyChangeListener (
            csDisplay.CURSOR_YVALUE,  labelListener);
        */
    }



    /**
     * Get the default foreground color
     *
     * @return the color
     */
    protected Color getDefaultForeground() {
        return Color.black;
    }


    /**
     * Get the default background color
     *
     * @return the color
     */
    protected Color getDefaultBackground() {
        return Color.white;
    }




    /**
     * Set the foreground and background colors.
     *
     * @param foreground  foreground color
     * @param background  background color
     */
    public void setColors(Color foreground, Color background) {
        super.setColors(foreground, background);
        getXSDisplay().setBackground(background);
        getXSDisplay().setForeground(foreground);
    }


    /**
     * Get the cross section display that this view manager uses.
     *
     * @return XSDisplay
     */
    public XSDisplay getXSDisplay() {
        return (XSDisplay) getMaster();
    }

    /**
     * Set the clipping  flag
     *
     * @param value The value
     */
    public void setClipping(boolean value) {
        clipOn = value;
        if (getXSDisplay() != null) {
            getXSDisplay().enableClipping(clipOn);
        }
    }

    /**
     * Get the clipping  flag
     * @return The flag value
     */
    public boolean getClipping() {
        return clipOn;
    }

    /**
     * Create the GUI. This is a titled border around the XSDisplay.
     *
     *
     * @return The GUI
     */
    protected Container doMakeContents() {
        JPanel contents = new JPanel(new BorderLayout());

        csBorderTitle =
            new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
                             getXSDisplay().getName(),
                             TitledBorder.ABOVE_TOP, TitledBorder.CENTER);

        //      contents.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        contents.setBorder(csBorderTitle);
        contents.setPreferredSize(new Dimension(450, 250));

        // get the associated java.awt.Component of the vert cross sec display
        JComponent csComponent = (JComponent) getXSDisplay().getComponent();
        csComponent.setPreferredSize(contents.getSize());
        contents.add("Center", csComponent);
        return contents;
    }


    /**
     * Set the title shown in the gui by appending the given
     * titlePart to the name of the XSDisplay.
     *
     * @param titlePart The suffix
     */
    public void setDisplayTitle(String titlePart) {
        csBorderTitle.setTitle(getXSDisplay().getName() + " " + titlePart);
        getContents().repaint();
    }

    /**
     * Make this String the new title on the display
     *
     * @param newTitle The new title
     */
    public void setNewDisplayTitle(String newTitle) {
        csBorderTitle.setTitle(newTitle);
        getContents().repaint();
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
     * TODO
     * Display value,
     * extracted from data in the current working
     * FlatField of the vertical cross section display.
     * Evaluate at location given by cursor.
     * private synchronized void setCSValueLabel () {
     *   DecimalFormat presFormat = new DecimalFormat ("###.0");
     *
     *   try {
     *       if ((getXSDisplay ().getCursorXValue ()   != null)
     *        && (getXSDisplay ().getCursorYValue ()   != null)) {
     *
     *           // find "location" of cursor in geographic units
     *           Real      value    = new Real (0.0);
     *           RealTuple location = new RealTuple (new Real[]{ value,
     *                                    value });
     *
     *
     *           // using "location" try to get the data value there
     *           //TODO                value = (Real) csFieldForSample.evaluate (location, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
     *
     *             // show "value" in the "valueLabel"
     *             if (value.isMissing ()) {
     *           valueLabel.setText ("  Outside data area.");
     *           } else {
     *           valueLabel.setText (" Value: "+  valueFormat.format (value.getValue (display2DUnit)));
     *           }
     *       }
     *   } catch (Exception e) {
     *       LogUtil.printException (log_, "setCSValueLabel", e);
     *   }
     * }    // end set cs value label
     *
     * @return foo
     */



    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Cross Section";
    }


}

