/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.GuiUtils;

import ucar.visad.display.AnimationInfo;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.XSDisplay;

import visad.VisADException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


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

    /** Gets set when the properties dialog is instantiated for first time */
    private boolean propsComponentInstantiated = false;

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public CrossSectionViewManager() {}

    /**
     *  Create a CrossSectionViewManager with the given context, descriptor, object store
     *  and properties string.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param desc The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public CrossSectionViewManager(ViewContext viewContext,
                                   ViewDescriptor desc, String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }



    /**
     *  Create a CrossSectionViewManager with the given context, descriptor, object store,
     *  properties string and animation state.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param desc The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @param animationInfo Initial animation properties
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
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
        Dimension dimension = getIdv().getStateManager().getViewSize();
        if (dimension == null) {
            if ((getFullScreenWidth() > 0) && (getFullScreenHeight() > 0)) {
                dimension = new Dimension(getFullScreenWidth(),
                                          getFullScreenHeight());
            } else if (displayBounds != null) {
                dimension = new Dimension(displayBounds.width,
                                          displayBounds.height);
            }
        }

        if ((dimension == null) || (dimension.width == 0)
                || (dimension.height == 0)) {
            dimension = null;
        }

        return new VerticalXSDisplay(
            getIdv().getArgsManager().getIsOffScreen(), dimension);
    }


    /**
     * Initialize this object. This creates and initializes
     * the {@link ucar.visad.display.XSDisplay}.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void init() throws VisADException, RemoteException {

        if (getHaveInitialized()) {
            return;
        }
        super.init();
        VerticalXSDisplay csDisplay = (VerticalXSDisplay) getMaster();


        // squash z axis height or aspect ratio to same as 3d display;
        // and zoom in by a factor of 3.0 from the small default size.
        //csDisplay.setDisplayAspect(new double[] { .65, .65, 1.0} );
        csDisplay.setAspect(1.0, .4);

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
     * This was added to avoid calling doApplyProperties on this view
     * if the Properties Dialog has not been instantiated.  This can
     * happen for example if the user does Edit -> Change Display Unit
     *
     * @return the propsComponentInstantiated
     */

    public boolean isPropsComponentInstantiated() {
        return propsComponentInstantiated;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getPropertiesComponent() {
        JComponent jc = super.getPropertiesComponent();
        propsComponentInstantiated = true;
        return jc;
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
     * Some user preferences have changed.
     */
    public void applyPreferences() {
        super.applyPreferences();
        ((VerticalXSDisplay) getXSDisplay()).setXDisplayUnit(
            getIdv().getPreferenceManager().getDefaultDistanceUnit());
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
     * Set the  clipping  flag
     *
     * @param value The value
     */
    public void setClipping(boolean value) {
        setBp(PREF_CLIP, value);
    }

    /**
     * Get  the 3d clipping  flag
     * @return The flag value
     */
    public boolean getClipping() {
        return getBp(PREF_CLIP);
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
        if (csBorderTitle != null) {
            csBorderTitle.setTitle(getXSDisplay().getName() + " "
                                   + titlePart);
            getContents().repaint();
        }
    }

    /**
     * Make this String the new title on the display
     *
     * @param newTitle The new title
     */
    public void setNewDisplayTitle(String newTitle) {
        if (csBorderTitle != null) {
            csBorderTitle.setTitle(newTitle);
            getContents().repaint();
        }
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
        return "Cross Section";
    }

    /**
     * The BooleanProperty identified byt he given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handeling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value)
            throws Exception {
        if (id.equals(PREF_CLIP)) {
            if (getXSDisplay() != null) {
                getXSDisplay().enableClipping(value);
            }
        } else if (id.equals(PREF_SHOWSCALES)) {
            if (getXSDisplay() != null) {
                getXSDisplay().showAxisScales(value);
            }
        } else {
            super.handleBooleanPropertyChange(id, value);
        }
    }


    /**
     * Get the intial BooleanProperty-s
     *
     * @param props list to add them to.
     */
    protected void getInitialBooleanProperties(List props) {
        super.getInitialBooleanProperties(props);
        props.add(new BooleanProperty(PREF_CLIP, "Clip View At Box", "",
                                      false));
        props.add(new BooleanProperty(PREF_SHOWSCALES, "Show Axis Scales",
                                      "", true));
    }


    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = super.makeShowMenu();
        createCBMI(showMenu, PREF_CLIP);
        createCBMI(showMenu, PREF_SHOWSCALES);
        return showMenu;
    }

}
