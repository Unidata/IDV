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

package ucar.unidata.idv.control;


import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;

import ucar.unidata.idv.ViewManager;


import ucar.unidata.idv.control.drawing.*;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;


/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.145 $
 */

public class DrawingControl extends DisplayControlImpl {


    /** xgrf xml attribute */
    public static final String ATTR_USETIMESINANIMATION =
        "usetimesinanimation";

    /** xgrf attribute */
    public static final String ATTR_FRONTDISPLAY = "frontdisplay";

    /** Xml tag for the editor settings in the import/export format */
    public static final String TAG_EDITOR = "editor";

    /** Xml attribute for the editable property */
    public static final String ATTR_EDITABLE = "editable";

    /** Xml attribute for the title property */
    public static final String ATTR_TITLE = "title";

    /** File filter used for the xgrf files */
    public static final PatternFileFilter FILTER_XGRF =
        new PatternFileFilter(".+\\.xgrf", "IDV Drawing files (*.xgrf)");

    /** File suffix used for the xgrfg files */
    public static final String SUFFIX_XGRF = ".xgrf";

    /** The cursor used for when in delete mode */
    static Cursor deleteCursor;

    /** property for sharing lines */
    public static final String SHARE_LINES = "DrawingControl.SHARE_LINES";

    /** Filled capable flag for the command object */
    public static final int FLAG_FILLED = 1 << 1;

    /** Full lat/lon capable flag for the command object */
    public static final int FLAG_FULLLATLON = 1 << 2;

    /** flag for straight */
    public static final int FLAG_STRAIGHT = 1 << 3;

    /** Select command */
    public static final DrawingCommand CMD_SELECT =
        new DrawingCommand(
            "Select",
            "Click to select; CTRL+x to cut; CTRL+P to set properties",
            "/ucar/unidata/ui/drawing/images/pointer.gif");

    /** command */
    public static final DrawingCommand CMD_MOVE =
        new DrawingCommand(
            "Move or stretch graphic",
            "Drag: move graphic; Control-Drag: stretch graphic",
            "/auxdata/ui/icons/Move16.gif");

    /** command */
    public static final DrawingCommand CMD_STRETCH =
        new DrawingCommand("Stretch graphic", "Drag: reshape graphic",
                           "/auxdata/ui/icons/Reshape16.gif");






    /** Is this control enabled */
    private boolean enabled = true;

    /** Are we a front display */
    private boolean frontDisplay = false;

    /** Controls the disabled state */
    protected JCheckBox enabledCbx;

    /** Is this control editable */
    private boolean editable = true;

    /** The title we get from the xml */
    private String editorTitle = null;


    /** The current coordinate type */
    private int coordType = DrawingGlyph.COORD_LATLON;


    /** The current line width */
    private int lineWidth = 1;

    /** The current font size */
    private int fontSize = 12;

    /** Holds the list of fonts */
    private JComboBox fontBox;

    /** scale field */
    private JTextField scaleFld;

    /** The z slider */
    private JPanel zPositionPanel;


    /** Holds the hor justification */
    private JComboBox justificationBox;

    /** Holds the vert justification */
    private JComboBox vertJustificationBox;

    /** Holds the font sizes */
    private JComboBox fontSizeBox;

    /** Filled mode */
    private JCheckBox filledCbx;

    /** Is filled turned on */
    private boolean filled = false;

    /** Filled mode */
    protected JCheckBox straightCbx;

    /** Is filled turned on */
    private boolean straight = false;

    /** Is in full lat lon mode */
    private JCheckBox fullLatLonCbx;

    /** Is in full lat lon mode */
    private boolean fullLatLon = false;

    /** Draw in the current time_ */
    private JCheckBox useTimeCbx;

    /** Draw in the current time_ */
    private boolean useTime = false;


    /** Ignore time. Show all glyphs */
    private JCheckBox ignoreTimeCbx;


    /** Ignore time. Show all glyphs */
    private boolean ignoreTime = false;

    /** clipboard for XML */
    private String clipboardXml;

    /** Message label */
    JLabel msgLabel;


    /** Lists the glyphs */
    GlyphTable glyphTable;

    /** Lists the glyphs */
    GlyphTableModel glyphTableModel;


    /** List of all glyphs */
    protected List glyphs = new ArrayList();

    /** List of selected glyphs */
    protected List selectedGlyphs = new ArrayList();

    /** Top level displayable */
    CompositeDisplayable displayHolder;


    /** Currently manipulated glyph */
    DrawingGlyph currentGlyph;



    /** Current command */
    protected DrawingCommand currentCmd =
        GlyphCreatorCommand.CMD_SMOOTHPOLYGON;



    /** Keep track of the last projection in the display */
    private Object lastProjection;

    /** If we get our glyphs from a datachoice this is set to true */
    private boolean displayOnly = false;

    /** We can evaluate jython on a glyph */
    private String glyphJython;

    /** Show the front buttons */
    private boolean showFronts = false;

    /** Keep track of how many columns for the shape buttons in the gui */
    private int shapeColumns = 10;


    /** Do we skip the next time the mouse is released. */
    private boolean skipNextMouseReleased = false;

    /** For exporting file */
    private JCheckBox loadAsMapData;


    /** front scale */
    private double frontScale = 1.0;

    /** the autoscroll counter */
    private int autoScrollCnt = 0;


    /**
     * Create a new Drawing Control; set attributes.
     */
    public DrawingControl() {
        setAttributeFlags(FLAG_DISPLAYUNIT);
    }


    /**
     * How long do we sleep until we act on the control changed event
     *
     * @return sleep time
     */
    protected long getControlChangeSleepTime() {
        return 250;
    }


    /**
     * Utility
     *
     * @param distance The distance
     *
     * @return Formatted string
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public String formatDistance(Real distance)
            throws VisADException, RemoteException {
        return getDisplayConventions().formatDistance(
            distance.getValue(getDistanceUnit())) + " " + getDistanceUnit();
    }

    /**
     * Get the distance unit
     *
     * @return the distance unit
     */
    public Unit getDistanceUnit() {
        return getDisplayUnit();
    }

    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     * @return true if everything is okay
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        setColor(Color.red);
        if (deleteCursor == null) {
            deleteCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                GuiUtils.getImage("/auxdata/ui/icons/Cut16.gif"),
                new Point(0, 0), "Custom Delete");
        }

        initDisplayUnit();
        displayHolder = new CompositeDisplayable();
        displayHolder.setUseTimesInAnimation(getUseTimesInAnimation());
        addDisplayable(displayHolder);

        List oldGlyphs = glyphs;
        glyphs = new ArrayList();
        setDisplayInactive();
        for (int i = 0; i < oldGlyphs.size(); i++) {
            DrawingGlyph glyph = (DrawingGlyph) oldGlyphs.get(i);
            glyph.initFromBundle(this);
            addGlyph(glyph);
        }
        setDisplayActive();

        getViewAnimation();
        checkGlyphTimes();
        if (dataChoice != null) {
            Data data = dataChoice.getData(null);
            if (data != null) {
                editable    = false;
                displayOnly = true;
                processData(data);
            }
        }
        return true;
    }


    /**
     * Initialize the display unit
     */
    protected void initDisplayUnit() {
        if (getDisplayUnit() == null) {
            setDisplayUnit(getDefaultDistanceUnit());
        }
    }

    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */

    protected boolean shouldAddDisplayListener() {
        return true;
    }



    /**
     * Handle the window closing.
     */
    protected void handleWindowClosing() {
        try {
            clearCursor();
            clearSelection();
        } catch (Exception exc) {
            logException("Clearing the selection on window closing", exc);
        }
        super.handleWindowClosing();
    }

    /**
     * Clear the selection when we minisze the window
     */
    public void close() {
        try {
            clearSelection();
        } catch (Exception exc) {
            logException("Clearing the selection on window closing", exc);
        }
        super.close();
    }

    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            checkGlyphTimes();
        } catch (Exception exc) {
            logException("Property change", exc);
        }
        super.timeChanged(time);
    }






    /**
     * Process the visad data object. For now  this is a text object
     * that holds the glyph xml
     *
     * @param data The data object
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void processData(Data data)
            throws VisADException, RemoteException {
        if (data instanceof visad.Text) {
            String  contents = ((visad.Text) data).getValue();
            Element root     = null;
            try {
                root = XmlUtil.getRoot(contents);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            if (root != null) {
                processXml(root, true);
                checkGlyphTimes();
            }
        }
    }


    /**
     * respond to the reload data call
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void resetData() throws VisADException, RemoteException {
        DataChoice dataChoice = getDataChoice();
        if (dataChoice != null) {
            removeAllGlyphs();
            Data data = dataChoice.getData(null);
            if (data != null) {
                processData(data);
            }
        }
    }

    /**
     * Process the glyph xml.
     *
     * @param root Root of the xml dom
     * @param initialXml Did this come from the data choice or from an import
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void processXml(Element root, boolean initialXml)
            throws VisADException, RemoteException {
        List times = new ArrayList();
        setDisplayInactive();
        NodeList elements = XmlUtil.getElements(root);
        //Only set the usetimesinanimation when we are newly created
        if ( !getWasUnPersisted()) {
            setUseTimesInAnimation(XmlUtil.getAttribute(root,
                    ATTR_USETIMESINANIMATION, getUseTimesInAnimation()));
        }

        frontDisplay = XmlUtil.getAttribute(root, ATTR_FRONTDISPLAY,
                                            frontDisplay);
        if (displayHolder != null) {
            displayHolder.setUseTimesInAnimation(getUseTimesInAnimation());
        }


        for (int i = 0; i < elements.getLength(); i++) {
            Element      child = (Element) elements.item(i);
            DrawingGlyph glyph = null;
            if (child.getTagName().equals(DrawingGlyph.TAG_POLYGON)) {
                glyph = new PolyGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_FRONT)) {
                glyph = new FrontGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_SYMBOL)) {
                glyph = new SymbolGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_HIGH)) {
                glyph = new HighLowGlyph(true);
            } else if (child.getTagName().equals(DrawingGlyph.TAG_LOW)) {
                glyph = new HighLowGlyph(false);
            } else if (child.getTagName().equals(DrawingGlyph.TAG_TEXT)) {
                glyph = new TextGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_IMAGE)) {
                glyph = new ImageGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_MOVIE)) {
                glyph = new MovieGlyph();
            } else if (child.getTagName().equals(DrawingGlyph.TAG_SHAPE)) {
                glyph = new ShapeGlyph();
            } else if (child.getTagName().equals(TAG_EDITOR)) {
                if (initialXml) {

                    editable = XmlUtil.getAttribute(child, ATTR_EDITABLE,
                            editable);
                    editorTitle = XmlUtil.getAttribute(child, ATTR_TITLE,
                            (String) null);
                }
                continue;
            } else {
                System.err.println("Unknown shape tag:" + child.getTagName());
            }

            if (glyph != null) {
                glyph.initFromXml(this, child);
                addGlyph(glyph);
                List glyphTimes = glyph.getTimeValues();
                if (glyphTimes != null) {
                    for (int timeIdx = 0; timeIdx < glyphTimes.size();
                            timeIdx++) {
                        Object dttm = glyphTimes.get(timeIdx);
                        if ( !times.contains(dttm)) {
                            times.add(dttm);
                        }
                    }
                }
            }
        }
        if (initialXml && (times.size() > 0)) {
            setAnimationSet(times);
        }
        setDisplayActive();
    }


    /**
     * Have the glyphs check their visibility vis-a-vis animation time.
     */
    private void checkGlyphTimes() {
        try {
            if ( !getDisplayVisibility()) {
                return;
            }
            if ((ignoreTimeCbx == null) || !ignoreTimeCbx.isSelected()) {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).checkTimeVisibility();
                }
            } else {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).setVisible(true);
                }

            }
        } catch (Exception e) {
            logException("Setting glyph visiblity", e);
        }
    }


    /**
     * Toggle the visibility for vector graphics rendering
     *
     * @param rasterMode  the toggle mode
     *
     * @throws Exception  problem toggling
     */
    public void toggleVisibilityForVectorGraphicsRendering(int rasterMode)
            throws Exception {
        if (rasterMode == RASTERMODE_SHOWRASTER) {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                glyph.oldVisibility = glyph.isVisible();
                if (glyph.oldVisibility) {
                    glyph.setVisible(glyph.getIsRaster());
                }
            }
        } else if (rasterMode == RASTERMODE_SHOWNONRASTER) {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                if (glyph.oldVisibility) {
                    glyph.setVisible( !glyph.getIsRaster());
                }
            }

        } else {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                glyph.setVisible(glyph.oldVisibility);
            }
        }
    }


    /**
     * Should this glyph be visible
     *
     * @param tg glyph
     *
     * @return should be visible
     */
    public boolean shouldBeVisible(DrawingGlyph tg) {
        return getDisplayVisibility();
    }


    /**
     *  TUrn on the visibility of this display
     *
     * @param on Visible?
     */
    public void setDisplayVisibility(boolean on) {
        super.setDisplayVisibility(on);
        for (int i = 0; i < glyphs.size(); i++) {
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
            glyph.checkVisibility();
        }

        if (getHaveInitialized()) {
            checkGlyphTimes();
        }
    }


    /**
     * Add the glyph into the list of glyphs
     *
     * @param glyph The glyph to add
     *
     * @return Was this successful
     */
    protected boolean addGlyph(DrawingGlyph glyph) {
        try {
            //Add the glyphs displayable first so the initFinal can access the
            //DisplayMaster if needed
            Displayable displayable = glyph.getDisplayable();
            displayable.setUseTimesInAnimation(getUseTimesInAnimation());
            displayHolder.addDisplayable(displayable);
            if ( !glyph.initFinal()) {
                return false;
            }
            glyphs.add(glyph);
            if (glyphTable != null) {
                glyphTable.glyphsChanged();
            }
        } catch (Exception e) {
            logException("Adding glyph", e);
            return false;
        }
        return true;
    }

    /**
     * The given glyph has changed somehow. Update the JTable
     *
     * @param glyph The glyph that changed
     */
    public void glyphChanged(DrawingGlyph glyph) {
        if (glyphTable != null) {
            glyphTable.repaint();
        }
    }



    /**
     * Remove this DisplayControl from the system.  Nulls out any
     * objects for garbage collection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doRemove() throws VisADException, RemoteException {
        clearCursor();
        if (glyphs != null) {
            for (int i = 0; i < glyphs.size(); i++) {
                ((DrawingGlyph) glyphs.get(i)).setBeenRemoved(true);
            }
        }
        glyphs         = null;
        selectedGlyphs = null;
        displayHolder  = null;
        super.doRemove();
    }


    /**
     * Overwrite the legend labels method to use the editor title if there is one.
     *
     * @param labels List of labels
     * @param legendType Side or bottom
     */
    protected void getLegendLabels(List labels, int legendType) {
        if ((editorTitle != null) && (editorTitle.length() > 0)) {
            labels.add(editorTitle);
        } else {
            super.getLegendLabels(labels, legendType);
        }
    }


    /**
     * Create a glyph
     *
     * @param event The event
     * @param mousePress Is mouse pressed or released
     *
     * @return The glyph or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected DrawingGlyph createGlyph(DisplayEvent event, boolean mousePress)
            throws VisADException, RemoteException {

        DrawingGlyph glyph = null;

        if (currentCmd instanceof GlyphCreatorCommand) {
            GlyphCreatorCommand gcc = (GlyphCreatorCommand) currentCmd;
            if (gcc.getNeedsMouse() && !mousePress) {
                return null;
            }
            if ( !gcc.getNeedsMouse() && mousePress) {
                return null;
            }
            glyph = gcc.createGlyph(this, event);
        }

        if (glyph == null) {
            return null;
        }

        String name = glyph.getName();
        if ((name == null) || (name.length() == 0)) {
            name = getGlyphNameDefault(glyph) + " " + (glyphs.size() + 1);
            glyph.setName(name);
        }

        if ( !glyph.initFromUser(this, event)) {
            return null;
        }
        if ( !addGlyph(glyph)) {
            return null;
        }

        setCurrentGlyph(glyph, glyph.handleCreation(event));
        if (currentGlyph != null) {
            if (currentGlyph instanceof TextGlyph) {
                setCursor(Cursor.TEXT_CURSOR);
            } else {
                setCursor(Cursor.HAND_CURSOR);
            }
        }
        return glyph;
    }



    /**
     * Set the current glyph
     *
     *
     * @param glyphWeHad This is the one we had
     * @param glyph The glyph
     */
    private void setCurrentGlyph(DrawingGlyph glyphWeHad,
                                 DrawingGlyph glyph) {

        try {
            if ((glyphWeHad != null) && (glyphWeHad != glyph)
                    && glyphWeHad.getBeingCreated()) {
                try {
                    setSelection(glyphWeHad);
                } catch (Exception exc) {}
            }


            currentGlyph = glyph;
            if (currentGlyph == null) {
                setCursor();
            } else {
                if (Misc.equals(currentCmd, CMD_STRETCH)
                        && (currentGlyph instanceof ImageGlyph)) {
                    msgLabel.setText(
                        "Drag: reshape; Drag/Ctrl: unconstrained");

                }
            }
        } catch (Exception e) {
            logException("Setting current drawing glyph", e);
        }
    }



    /**
     * Respond to control changed events from the view manager
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        //        boolean wasActive = navDisplay.ensureInactive();
        try {
            long t1 = System.currentTimeMillis();
            if (glyphs != null) {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).viewpointChanged();
                }
            }
            //            navDisplay.setActive(wasActive);
            long t2 = System.currentTimeMillis();
            //            System.err.println ("time:" + (t2-t1)+ " #:" + glyphs.size());
        } catch (Exception e) {
            logException("Handling projection change event", e);
        }
    }

    /**
     * Respond to the projection changing event
     */
    public void projectionChanged() {
        super.projectionChanged();
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        //        boolean wasActive = navDisplay.ensureInactive();
        try {
            if (glyphs != null) {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).projectionChanged();
                }
            }
            //                navDisplay.setActive(wasActive);
        } catch (Exception e) {
            logException("Handling projection change event", e);
        }
    }



    /**
     * Should we handle display events
     *
     * @return Ok to handle events
     */
    protected boolean canHandleEvents() {
        if (displayOnly || !getEnabled() || !editable
                || !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }

    /**
     * Do the flythrough
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD problem
     */
    public void doFlythrough() throws VisADException, RemoteException {
        for (int i = 0; i < selectedGlyphs.size(); i++) {
            if (selectedGlyphs.get(i) instanceof PolyGlyph) {
                ((PolyGlyph) selectedGlyphs.get(i)).doFlythrough();
                break;
            }
        }

    }

    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {

        int id = event.getId();
        if ((id == DisplayEvent.MOUSE_MOVED) && (currentGlyph == null)) {
            return;
        }
        InputEvent inputEvent = event.getInputEvent();
        if ( !canHandleEvents()) {
            return;
        }

        try {
            //            System.err.println ("event:" + displayEventName(id));
            if (id == DisplayEvent.KEY_PRESSED) {
                if ((inputEvent instanceof KeyEvent)) {
                    KeyEvent keyEvent = (KeyEvent) inputEvent;
                    if ((keyEvent.getKeyCode() == KeyEvent.VK_A)
                            && keyEvent.isControlDown()) {
                        setSelection(glyphs, false);
                        return;
                    }
                    if ((keyEvent.getKeyCode() == KeyEvent.VK_X)
                            && keyEvent.isControlDown()) {
                        doCut();
                        return;
                    }
                    if ((keyEvent.getKeyCode() == KeyEvent.VK_F)
                            && keyEvent.isControlDown()) {
                        doFlythrough();
                        return;
                    }
                    if ((keyEvent.getKeyCode() == KeyEvent.VK_C)
                            && keyEvent.isControlDown()) {
                        clipboardXml = toXml(selectedGlyphs);
                        return;
                    }
                    if ((keyEvent.getKeyCode() == KeyEvent.VK_V)
                            && keyEvent.isControlDown()) {
                        if (clipboardXml != null) {
                            doImportXml(clipboardXml);
                        }
                        return;
                    }

                    if (GuiUtils.isDeleteEvent(keyEvent)) {
                        if ((currentGlyph != null)
                                && currentCmd.equals(CMD_STRETCH)) {
                            setSelection(currentGlyph);
                            currentGlyph.doDeletePoint(event);
                        }
                    }


                    if ((keyEvent.getKeyCode() == KeyEvent.VK_P)
                            && keyEvent.isControlDown()) {
                        doProperties(selectedGlyphs);
                        return;
                    }
                }

                if (currentGlyph != null) {
                    setCurrentGlyph(currentGlyph,
                                    currentGlyph.handleKeyPressed(event));
                }
            }



            //Don't respond to the event if the shift or control key
            //is pressed. These are used for zooming/panning/etc
            //            if ((event.getModifiers() & event.SHIFT_MASK) != 0
            //                    || (event.getModifiers() & event.CTRL_MASK) != 0) {
            if ((event.getModifiers() & event.SHIFT_MASK) != 0) {
                return;
            }

            if (id == DisplayEvent.MOUSE_MOVED) {
                if (currentGlyph != null) {
                    setCurrentGlyph(currentGlyph,
                                    currentGlyph.handleMouseMoved(event));
                }
                return;
            }


            setCursor();
            if (id == DisplayEvent.MOUSE_PRESSED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }

                DrawingGlyph closestGlyph = closestGlyph(event);
                if (closestGlyph != null) {
                    if (closestGlyph.mousePressed(event)) {
                        skipNextMouseReleased = true;
                        return;
                    }
                }


                if (currentCmd.equals(CMD_SELECT)) {
                    if (closestGlyph != null) {
                        closestGlyph.mousePressed(event);
                        setCurrentGlyph(currentGlyph, null);
                        setSelection(Misc.newList(closestGlyph),
                                     inputEvent.isControlDown());
                    } else {
                        clearSelection();
                    }
                    return;
                }


                if (currentCmd.equals(CMD_MOVE)) {
                    closestGlyph = closestGlyph(event, true);
                    if (closestGlyph != null) {
                        setCurrentGlyph(currentGlyph, closestGlyph);
                        closestGlyph.initMove(event);
                    }
                    return;
                }


                if (currentCmd.equals(CMD_STRETCH)) {
                    closestGlyph = closestGlyph(event, true);
                    if (closestGlyph != null) {
                        setCurrentGlyph(currentGlyph, closestGlyph);
                        closestGlyph.initStretch(event);
                    }
                    return;
                }
                if (currentGlyph == null) {
                    DrawingGlyph glyph = createGlyph(event, true);
                } else {
                    setCurrentGlyph(currentGlyph,
                                    currentGlyph.handleMousePressed(event));
                }
            } else if (id == DisplayEvent.MOUSE_DRAGGED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }

                int       x      = (int) event.getX();
                int       y      = (int) event.getY();
                Rectangle bounds = getScreenBounds();
                int       pad    = 5;
                if ((x > bounds.width + pad) || (y > bounds.height + pad)
                        || (x < -pad) || (y < -pad)) {
                    if (autoScrollCnt++ > 10) {
                        EarthLocation el = screenToEarth(x, y);
                        getNavigatedDisplay().center(el, false);
                        autoScrollCnt = 0;
                    }
                }

                if (currentGlyph == null) {
                    return;
                }
                Real distance = currentGlyph.getDistance();
                if (distance != null) {
                    Unit distanceUnit = getDistanceUnit();
                    msgLabel.setText(" Distance: "
                                     + formatDistance(distance));

                }

                if (currentCmd.equals(CMD_MOVE)) {
                    setSelection(currentGlyph);
                    currentGlyph.doMove(event);
                    //Update the table listing
                    if (glyphTable != null) {
                        glyphTable.repaint();
                    }
                    return;
                }
                if (currentCmd.equals(CMD_STRETCH)) {
                    setSelection(currentGlyph);
                    currentGlyph.doStretch(event);
                    //Update the table listing
                    if (glyphTable != null) {
                        glyphTable.repaint();
                    }
                    return;
                }
                setCurrentGlyph(currentGlyph,
                                currentGlyph.handleMouseDragged(event));
            } else if (id == DisplayEvent.MOUSE_RELEASED) {
                if (skipNextMouseReleased) {
                    skipNextMouseReleased = false;
                    return;
                }
                if ( !isLeftButtonDown(event)) {
                    return;
                }
                //DrawingGlyph closestGlyph = closestGlyph(event);
                if (currentGlyph == null) {
                    createGlyph(event, false);
                } else {
                    if ( !currentGlyph.isValid()) {
                        removeGlyph(currentGlyph);
                        setCurrentGlyph(currentGlyph, null);
                    } else {
                        DrawingGlyph glyphNow = currentGlyph;
                        setSelection(currentGlyph);
                        DrawingGlyph nextGlyph =
                            currentGlyph.handleMouseReleased(event);
                        setCurrentGlyph(currentGlyph, nextGlyph);
                        if (nextGlyph == null) {
                            //&& (currentCmd.equals(CMD_MOVE)
                            //|| currentCmd.equals(CMD_STRETCH))) {
                            doneMovingGlyph(glyphNow);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }


    /**
     * Done moving glyph
     *
     * @param glyph the glyph
     */
    protected void doneMovingGlyph(DrawingGlyph glyph) {}

    /**
     * Find the glyph that is closest to the x/y point of the given event
     *
     * @param event The event
     *
     * @return The closest glyph (or null if none close).
     */
    public DrawingGlyph closestGlyph(DisplayEvent event) {
        return closestGlyph(event, false);
    }

    /**
     * Find the glyph that is closest to the x/y point of the given event
     *
     * @param event The event
     * @param forMove Are we looking for a glyph to move
     *
     * @return The closest glyph (or null if none close).
     */

    public DrawingGlyph closestGlyph(DisplayEvent event, boolean forMove) {
        try {
            DrawingGlyph closestGlyph = null;
            VisADRay ray = getNavigatedDisplay().getRay(event.getX(),
                               event.getY());
            double[] box1     = { 0, 0 };
            double[] box2     = { 100, 0 };

            double[] location = toBox(event);
            double[] vector =
                getNavigatedDisplay().getRayDirection(event.getX(),
                    event.getY());
            Component comp =
                displayHolder.getDisplayMaster().getDisplayComponent();
            Rectangle bounds = comp.getBounds();
            double[]  ul     = screenToBox(0, 0);
            double[] lr = screenToBox((int) bounds.getWidth(),
                                      (int) bounds.getHeight());
            double diagonal    = DrawingGlyph.distanceBetween(ul, lr);
            double minDistance = diagonal * 0.05;

            //            System.err.println("Loc:" + location[0] + " " + location[1] + " " + location[2]);
            //            System.err.println("Vec:" + vector[0] + " " + vector[1] + " " + vector[2]);
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                if ( !glyph.isVisible() || !glyph.isSelectable()) {
                    continue;
                }
                if (forMove && !glyph.getEditable()) {
                    continue;
                }
                double tmpDistance = glyph.distance(location, vector);
                if (tmpDistance < minDistance) {
                    minDistance  = tmpDistance;
                    closestGlyph = glyph;
                }
            }
            return closestGlyph;
        } catch (Exception exc) {
            logException("Closest glyph", exc);
            return null;
        }
    }


    /**
     * Remove the glyph from the drawing
     *
     * @param glyph The glyph to remove
     */
    public void removeGlyph(DrawingGlyph glyph) {
        glyph.setBeenRemoved(true);
        glyphs.remove(glyph);
        selectedGlyphs.remove(glyph);
        if (glyphTable != null) {
            glyphTable.glyphsChanged();
        }
        try {
            displayHolder.removeDisplayable(glyph.getDisplayable());
        } catch (Exception exc) {
            logException("Removing glyph", exc);
        }
    }


    /**
     * Clear the current selection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void clearSelection() throws VisADException, RemoteException {
        setSelection(new ArrayList(), false);
    }

    /**
     * Clear the selection except for glyphs in the given list
     *
     * @param newSelection List of glyphs that should not be removed from the selection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void clearSelection(List newSelection)
            throws VisADException, RemoteException {
        List tmp = new ArrayList(selectedGlyphs);
        for (int i = 0; i < tmp.size(); i++) {
            DrawingGlyph g = (DrawingGlyph) tmp.get(i);
            if ((newSelection != null) && newSelection.contains(g)) {
                continue;
            }
            g.setSelected(false);
            selectedGlyphs.remove(g);
        }
        selectionChanged();
    }


    /**
     * The selection set changed. Update the JTable
     */
    protected void selectionChanged() {
        if (glyphTable != null) {
            glyphTable.selectionChanged();
        }
    }


    /**
     * Clear the selection and add the given glyph to the selection
     *
     * @param glyph Glyph to add to the selection set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setSelection(DrawingGlyph glyph)
            throws VisADException, RemoteException {
        setSelection(Misc.newList(glyph), false);
    }



    /**
     * Set the selection. Clear the old selection set if addTo is false.
     *
     * @param newSelection New set of glyphs
     * @param addTo If true then don't clear the selection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setSelection(List newSelection, boolean addTo)
            throws VisADException, RemoteException {
        if (newSelection.equals(selectedGlyphs)) {
            return;
        }
        List oldSelection = new ArrayList(selectedGlyphs);
        setDisplayInactive();
        if ( !addTo) {
            clearSelection(newSelection);
        }
        for (int i = 0; i < newSelection.size(); i++) {
            DrawingGlyph g = (DrawingGlyph) newSelection.get(i);
            if ( !g.isVisible() || selectedGlyphs.contains(g)) {
                continue;
            }
            selectedGlyphs.add(g);
            g.setSelected(true);
        }
        if ( !oldSelection.equals(selectedGlyphs)) {
            selectionChanged();
        }
        setDisplayActive();
    }


    /**
     * Is the given glyph selected
     *
     * @param glyph The glyph
     *
     * @return Is selected
     */
    public boolean isSelected(DrawingGlyph glyph) {
        return selectedGlyphs.contains(glyph);
    }

    /**
     * Remove the glyphs in the selection set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void doCut() throws VisADException, RemoteException {
        setDisplayInactive();
        List tmp       = new ArrayList(selectedGlyphs);
        List cutGlyphs = new ArrayList();

        for (int i = 0; i < tmp.size(); i++) {
            DrawingGlyph g = (DrawingGlyph) tmp.get(i);
            if ( !g.isVisible()) {
                continue;
            }
            removeGlyph(g);
            cutGlyphs.add(g);
        }
        clipboardXml = toXml(cutGlyphs);
        selectionChanged();
        setDisplayActive();
    }




    /**
     * Show the properties dialog for the glyphs in the list
     *
     * @param glyphs Glyphs to set properties on
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doProperties(final List glyphs)
            throws VisADException, RemoteException {
        if ((glyphs.size() > 0) && (glyphs.size() < 4)) {
            Misc.runInABit(1, new Runnable() {
                public void run() {
                    doPropertiesInner(glyphs);
                }
            });
        }
    }

    /**
     * Really set the properties on the glyphs
     *
     * @param glyphs The glyphs
     */
    private void doPropertiesInner(List glyphs) {
        try {
            for (int i = 0; i < glyphs.size(); i++) {
                ((DrawingGlyph) glyphs.get(i)).setProperties();
            }
        } catch (Exception exc) {
            logException("Do properties", exc);
        }
    }


    /**
     * Method called by other classes that share the the state.
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Set the current active command
     *
     * @param command The command
     */
    protected void setCurrentCommand(DrawingCommand command) {
        if (filledCbx != null) {
            filledCbx.setEnabled(command.isCapable(FLAG_FILLED));
            fullLatLonCbx.setEnabled(command.isCapable(FLAG_FULLLATLON));
        }

        if (straightCbx != null) {
            straightCbx.setEnabled(command.isCapable(FLAG_STRAIGHT));
        }

        setCurrentGlyph(currentGlyph, null);
        currentCmd = command;
        setCursor();
        msgLabel.setText(command.getMessage());
    }

    /**
     * Set the cursor depending on the current command.
     */
    private void setCursor() {
        if ((enabledCbx == null) || (currentCmd == null)) {
            return;
        }
        if ( !enabledCbx.isSelected() || !canHandleEvents()) {
            clearCursor();
            return;
        }
        if (currentCmd.equals(CMD_MOVE)) {
            //            setCursor(Cursor.MOVE_CURSOR);
            setCursor(Cursor.HAND_CURSOR);
        } else if (currentCmd.equals(CMD_STRETCH)) {
            setCursor(Cursor.MOVE_CURSOR);
        } else {
            clearCursor();
        }

    }



    /**
     * Utility to make a panel holding the icon buttons.
     *
     * @param commands Command to make buttons for.
     * @param bg Button group to add buttons to
     *
     * @return The button panel
     */
    protected JPanel makeButtonPanel(List commands, ButtonGroup bg) {
        List buttons = new ArrayList();
        for (int i = 0; i < commands.size(); i++) {
            final DrawingCommand cmd = (DrawingCommand) commands.get(i);
            JToggleButton btn = GuiUtils.getToggleButton(cmd.getIconPath(),
                                    4, 4);
            btn.setToolTipText(cmd.getLabel());
            bg.add(btn);
            if (Misc.equals(currentCmd, cmd)) {
                btn.setSelected(true);
                msgLabel.setText(cmd.getMessage());
                setCurrentCommand(cmd);
            }

            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setCurrentCommand(cmd);
                }
            });
            buttons.add(btn);
        }
        if (buttons.size() > shapeColumns) {
            return GuiUtils.colGrid(GuiUtils.getComponentArray(buttons),
                                    shapeColumns);
        } else {
            return GuiUtils.hflow(buttons, 4, 0);
        }
    }




    /**
     * Get the lsit of shape commands the user can draw with.
     * Derived classes can override this to control what
     * can be drawn
     *
     * @return Shape commands
     */
    protected List getShapeCommands() {
        List commands = new ArrayList();
        commands.add(GlyphCreatorCommand.CMD_SMOOTHPOLYGON);
        //        commands.add(GlyphCreatorCommand.CMD_POLYGON);
        //        commands.add(GlyphCreatorCommand.CMD_LINE);
        commands.add(GlyphCreatorCommand.CMD_RECTANGLE);
        commands.add(GlyphCreatorCommand.CMD_DIAMOND);
        commands.add(GlyphCreatorCommand.CMD_ARROW);
        commands.add(GlyphCreatorCommand.CMD_HARROW);
        commands.add(GlyphCreatorCommand.CMD_VARROW);
        //GlyphCreatorCommand.CMD_OVAL);
        commands.add(GlyphCreatorCommand.CMD_TEXT);
        commands.add(GlyphCreatorCommand.CMD_WAYPOINT);
        commands.add(GlyphCreatorCommand.CMD_IMAGE);
        commands.add(GlyphCreatorCommand.CMD_MOVIE);
        commands.add(GlyphCreatorCommand.CMD_SYMBOL);
        shapeColumns = commands.size();


        if (showFronts) {
            commands.add(GlyphCreatorCommand.CMD_HIGH);
            commands.add(GlyphCreatorCommand.CMD_LOW);
            for (int i = 0; i < FrontDrawer.BASETYPES.length; i++) {
                final String type  = FrontDrawer.BASETYPES[i];
                String       label = FrontDrawer.getLabel(type).toLowerCase();
                String       icon  = "/auxdata/ui/icons/ColdFront16.gif";
                if (type.equals(FrontDrawer.TYPE_WARM_FRONT)) {
                    icon = "/auxdata/ui/icons/WarmFront16.gif";
                } else if (type.equals(FrontDrawer.TYPE_OCCLUDED_FRONT)) {
                    icon = "/auxdata/ui/icons/OccludedFront16.gif";
                } else if (type.equals(FrontDrawer.TYPE_STATIONARY_FRONT)) {
                    icon = "/auxdata/ui/icons/StationaryFront16.gif";
                } else if (type.equals(FrontDrawer.TYPE_TROUGH)) {
                    icon = "/auxdata/ui/icons/Trough16.gif";
                }

                commands.add(
                    new GlyphCreatorCommand(
                        "Create " + StringUtil.getAnOrA(label) + " " + label,
                        "Click and drag: create "
                        + StringUtil.getAnOrA(label) + " " + label, icon,
                            DrawingControl.FLAG_STRAIGHT) {
                    public DrawingGlyph createGlyph(DrawingControl control,
                            DisplayEvent event)
                            throws VisADException, RemoteException {
                        return new FrontGlyph(control, event, type,
                                !getStraight());
                    }
                });
            }
        }
        return commands;
    }


    /**
     * Make the shapes panel
     *
     * @return the shapes panel
     */
    protected JComponent doMakeShapesPanel() {
        JComponent contents = GuiUtils.inset(doMakeTablePanel(), 4);
        if (displayOnly) {
            zPositionPanel = GuiUtils.hgrid(doMakeZPositionSlider(),
                                            GuiUtils.filler());
            contents = GuiUtils.centerBottom(contents,
                                             GuiUtils.label("Z Position: ",
                                                 zPositionPanel));
            return GuiUtils.centerBottom(contents, msgLabel);
        }

        return contents;

    }


    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JTabbedPane tabbedPane = new JTabbedPane();
        if (frontDisplay) {
            tabbedPane.add("Fronts", doMakeShapesPanel());
        } else if ( !editable) {
            return GuiUtils.topCenter(doMakeControlsPanel(),
                                      doMakeShapesPanel());
        } else {
            tabbedPane.add("Controls", GuiUtils.top(doMakeControlsPanel()));
            if (editable) {
                tabbedPane.add("Style", GuiUtils.top(doMakeStylePanel()));
            }
            tabbedPane.add("Shapes", doMakeShapesPanel());
        }
        return GuiUtils.centerBottom(tabbedPane, msgLabel);
    }


    /**
     * Handle the z position slider changed
     *
     * @param value the new z slider value
     */
    public void zSliderChanged(double value) {
        super.zSliderChanged(value);
        if (displayOnly) {
            try {
                applyZPosition(glyphs);
            } catch (Exception exc) {
                logException("Setting z position", exc);
            }
        }
    }

    /**
     * Make the main tabbed pane
     *
     * @return Controls panel
     */
    protected JComponent doMakeControlsPanel() {
        JComponent titleLabel = null;
        if (editorTitle != null) {
            titleLabel = new JLabel(editorTitle);
        } else {
            titleLabel = new JPanel();
        }
        if ( !editable) {
            return titleLabel;
        }
        List widgets = new ArrayList();
        addControlWidgets(widgets);
        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        JPanel comps = GuiUtils.doLayout(widgets, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);

        return GuiUtils.top(GuiUtils.topCenter(titleLabel, comps));
    }

    /**
     * Add the widgets into the controls panel
     *
     * @param widgets List to add to. Add in pairs (label, widget)
     */
    protected void addControlWidgets(List widgets) {

        msgLabel = new JLabel("");
        msgLabel.setBorder(new FineLineBorder(BevelBorder.LOWERED));


        int[] coords = { DrawingGlyph.COORD_XYZ, DrawingGlyph.COORD_XY,
                         DrawingGlyph.COORD_LATLONALT,
                         DrawingGlyph.COORD_LATLON };
        String[] coordLabels = { "X/Y/Z", "X/Y", "Lat/Lon/Alt", "Lat/Lon" };
        List           coordItems = new ArrayList();
        TwoFacedObject coordTfo   = null;
        for (int i = 0; i < coords.length; i++) {
            TwoFacedObject tfo = new TwoFacedObject(coordLabels[i],
                                     coords[i]);
            if (coords[i] == coordType) {
                coordTfo = tfo;
            }
            coordItems.add(tfo);
        }

        zPositionPanel = GuiUtils.hgrid(doMakeZPositionSlider(),
                                        GuiUtils.filler());

        final JComboBox coordBox = new JComboBox();
        GuiUtils.setListData(coordBox, coordItems);
        if (coordTfo != null) {
            coordBox.setSelectedItem(coordTfo);
        }
        coordBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                TwoFacedObject tfo =
                    (TwoFacedObject) coordBox.getSelectedItem();
                coordType = ((Integer) tfo.getId()).intValue();
                checkZSliderEnabled();
            }
        });


        filledCbx     = new JCheckBox("Filled", filled);
        straightCbx   = new JCheckBox("Straight", straight);
        fullLatLonCbx = new JCheckBox("Full Lat/Lon", fullLatLon);
        useTimeCbx    = new JCheckBox("Draw In Current Time", useTime);
        ignoreTimeCbx = new JCheckBox("Show All", ignoreTime);
        ignoreTimeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkGlyphTimes();
            }
        });

        enabledCbx = new JCheckBox("Enabled", enabled);
        enabledCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setCursor();
            }
        });



        makeModePanel(widgets);

        //        widgets.add(new JLabel(" "));
        //        widgets.add(GuiUtils.inset(new JLabel(" "), 5));
        if (showLocationWidgets()) {
            widgets.add(GuiUtils.rLabel("Coordinates:"));
            widgets.add(GuiUtils.left(GuiUtils.flow(new Component[] {
                coordBox,
                fullLatLonCbx })));
        }
        widgets.add(GuiUtils.rLabel("Z Position:"));
        widgets.add(zPositionPanel);
        checkZSliderEnabled();

        if (showTimeWidgets()) {
            widgets.add(GuiUtils.rLabel("Time:"));
            widgets.add(GuiUtils.left(GuiUtils.flow(new Component[] {
                useTimeCbx,
                ignoreTimeCbx })));
        }

    }

    /**
     * Get default z position to use
     *
     * @return Default z position
     */
    protected double getInitialZPosition() {
        return super.getInitialZPosition() + ZFUDGE;
    }


    /**
     * Makes the model panel. Can be overwritten.
     *
     * @param widgets List ot add label/widget pairs
     */
    protected void makeModePanel(List widgets) {

        List        commands = Misc.newList(CMD_SELECT, CMD_MOVE,
                                            CMD_STRETCH);
        List        shapes   = getShapeCommands();
        ButtonGroup bg       = new ButtonGroup();
        widgets.add(GuiUtils.rLabel("Mode:"));
        widgets.add(GuiUtils.leftCenter(makeButtonPanel(commands, bg),
                                        GuiUtils.left(enabledCbx)));
        widgets.add(GuiUtils.rLabel("Shapes:"));
        if (showFilledCbx()) {
            widgets.add(
                GuiUtils.leftCenter(
                    makeButtonPanel(shapes, bg),
                    GuiUtils.left(GuiUtils.hbox(filledCbx, straightCbx))));
        } else {
            widgets.add(GuiUtils.left(makeButtonPanel(shapes, bg)));
        }

    }

    /**
     * Make the jtable panel
     *
     * @return jtable panel
     */
    protected JComponent doMakeTablePanel() {
        glyphTableModel = new GlyphTableModel();
        glyphTable      = new GlyphTable(glyphTableModel);
        JScrollPane sp = GuiUtils.makeScrollPane(glyphTable, 200, 100);
        sp.setPreferredSize(new Dimension(200, 100));
        JComponent tablePanel = GuiUtils.center(sp);
        glyphTable.selectionChanged();
        return tablePanel;
    }


    /**
     * Make the style panel
     *
     * @return Style panel
     */
    protected JComponent doMakeStylePanel() {
        List  styleWidgets = new ArrayList();


        Color c            = getColor();
        //        Component colorCbx     = doMakeColorControl(((c == null)
        //                ? Color.red
        //                : c));
        if (c == null) {
            c = Color.red;
        }
        GuiUtils.ColorSwatch colorSwatch = new GuiUtils.ColorSwatch(c,
                                               "Set color", true) {
            public void setBackground(Color newColor) {
                super.setBackground(newColor);
                try {
                    setColor(newColor);
                } catch (Exception exc) {
                    logException("Setting color", exc);
                }
            }
        };
        colorSwatch.setMinimumSize(new Dimension(20, 20));
        colorSwatch.setPreferredSize(new Dimension(20, 20));
        Component colorCbx  = colorSwatch;

        JComboBox widthComp = doMakeLineWidthBox(lineWidth);
        widthComp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox theBox = (JComboBox) e.getSource();
                setLineWidth(((Integer) theBox.getSelectedItem()).intValue());
            }
        });



        fontBox = GuiUtils.doMakeFontBox(new Font("Dialog", Font.PLAIN, 12));
        fontSizeBox = GuiUtils.doMakeFontSizeBox(12);
        justificationBox =
            new JComboBox(new Vector(Misc.newList(TextGlyph.JUST_LEFT,
                TextGlyph.JUST_CENTER, TextGlyph.JUST_RIGHT)));
        vertJustificationBox =
            new JComboBox(new Vector(Misc.newList(TextGlyph.JUST_BOTTOM,
                TextGlyph.JUST_CENTER, TextGlyph.JUST_TOP)));
        JPanel fontPanel = GuiUtils.flow(new Component[] { fontBox,
                new JLabel("Size: "), fontSizeBox });
        JPanel justPanel = GuiUtils.flow(new Component[] { justificationBox,
                vertJustificationBox });

        styleWidgets.add(GuiUtils.rLabel("Color:"));
        styleWidgets.add(GuiUtils.left(GuiUtils.hflow(Misc.newList(colorCbx,
                GuiUtils.filler(10, 5), GuiUtils.rLabel("Line Width:  "),
                GuiUtils.left(widthComp)), 4, 0)));
        styleWidgets.add(GuiUtils.rLabel("Font:"));
        styleWidgets.add(GuiUtils.left(fontPanel));
        styleWidgets.add(GuiUtils.rLabel("Justification:"));
        styleWidgets.add(GuiUtils.left(justPanel));



        if (getShowFronts()) {
            scaleFld = new JTextField("" + frontScale, 5);
            styleWidgets.add(GuiUtils.rLabel("Front Scale:"));
            styleWidgets.add(GuiUtils.left(scaleFld));
        }


        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        return GuiUtils.doLayout(styleWidgets, 2, GuiUtils.WT_NY,
                                 GuiUtils.WT_N);

    }


    /**
     *  Enable/disable the z slider
     */
    protected void checkZSliderEnabled() {
        boolean posEnabled = ((coordType == DrawingGlyph.COORD_XY)
                              || (coordType == DrawingGlyph.COORD_LATLON));
        //Leave this enabled
        //        GuiUtils.enableTree(zPositionPanel, posEnabled);
    }




    /**
     * Should we show the filled cbx
     *
     * @return Show the filled cbx
     */
    protected boolean showFilledCbx() {
        return true;
    }

    /**
     * Should we show the time widgets
     *
     * @return  show the tiem widgets
     */
    protected boolean showTimeWidgets() {
        return true;
    }

    /**
     * Should we show the locatio  widgets
     *
     * @return  show the locatio  widgets
     */
    protected boolean showLocationWidgets() {
        return true;
    }


    /**
     * Get the color table to use for the image glyphs
     *
     * @return The rgb color table
     */
    public ColorTable getRGBColorTable() {
        return getDisplayConventions().getParamColorTable("image");
    }




    /**
     * Make a component to change the width of the lines.
     *
     * @param lineWidth The selected width
     * @return the component.
     */
    public static JComboBox doMakeLineWidthBox(int lineWidth) {
        int[]  widths = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };
        Vector values = new Vector();
        for (int i = 0; i < widths.length; i++) {
            values.add(new Integer(widths[i]));
        }
        JComboBox box = new JComboBox(values);
        box.setSelectedItem(new Integer(lineWidth));
        box.setToolTipText("Change width of lines");
        return box;
    }



    /**
     * Get the coord type (e.g., LATLON, XYZ)
     *
     * @return The coord type
     */
    public int getCoordType() {
        return coordType;
    }


    /**
     * Get the coord type (e.g., LATLON, XYZ)
     *
     *
     * @param c The coord type to use
     */
    protected void setCoordType(int c) {
        coordType = c;
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if ( !editable || displayOnly) {
            super.getEditMenuItems(items, forMenuBar);
            return;
        }

        items.add(GuiUtils.makeMenuItem("Remove All", this,
                                        "removeAllGlyphs"));
        items.add(GuiUtils.makeMenuItem("Apply Color to Selected", this,
                                        "applyColorToAll"));
        items.add(GuiUtils.makeMenuItem("Apply Z Position to Selected", this,
                                        "applyZPosition", selectedGlyphs));

        super.getEditMenuItems(items, forMenuBar);

    }

    /**
     * Add to the file menu.
     *
     * @param items Holds the menu items
     * @param forMenuBar Is it for the main window
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        super.getFileMenuItems(items, forMenuBar);
        addFileMenuItems(items, forMenuBar);
    }

    /**
     * Add to the file menu
     *
     * @param items List to add to
     * @param forMenuBar for the menu bar
     */
    protected void addFileMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.MENU_SEPARATOR);
        JMenuItem mi;
        items.add(mi = new JMenuItem("Export Drawing..."));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                doExport();
            }
        });
        if ( !displayOnly) {
            items.add(mi = new JMenuItem("Import Drawing..."));
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    doImport();
                }
            });
        }

    }

    /**
     * Import a grf file
     */
    private void doImport() {
        try {
            String filename = FileManager.getReadFile(FILTER_XGRF);
            if (filename == null) {
                return;
            }
            Element root = XmlUtil.getRoot(filename, getClass());
            processXml(root, false);
        } catch (Exception exc) {
            logException("Importing drawing", exc);
        }
    }

    /**
     * Import kml
     *
     * @param xml the KML
     */
    private void doImportXml(String xml) {
        try {
            Element root = XmlUtil.getRoot(xml);
            processXml(root, false);
        } catch (Exception exc) {
            logException("Importing drawing", exc);
        }
    }


    /**
     * Export a grf file
     */
    private void doExport() {
        try {
            if (loadAsMapData == null) {
                loadAsMapData = new JCheckBox("Load as map data", false);
                loadAsMapData.setToolTipText(
                    "Load this xgrf file back in as map data");
            }
            String filename = FileManager.getWriteFile(FILTER_XGRF,
                                  SUFFIX_XGRF, GuiUtils.top(loadAsMapData));
            if (filename == null) {
                return;
            }
            String xml = toXml(glyphs);
            if (xml == null) {
                return;
            }
            IOUtil.writeFile(filename, xml);
            if (loadAsMapData.isSelected()) {
                getIdv().makeOneDataSource(filename, "FILE.MAPFILE", null);
            }
        } catch (Exception exc) {
            logException("Exporting drawing", exc);
        }

    }

    /**
     * Convert glyphs to XML
     *
     * @param glyphs list of glyphs
     *
     * @return  the KML
     */
    private String toXml(List glyphs) {
        try {

            Document doc  = XmlUtil.getDocument("<shapes></shapes>");
            Element  root = doc.getDocumentElement();
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph g = (DrawingGlyph) glyphs.get(i);
                root.appendChild(g.getElement(doc));
            }
            return XmlUtil.toString(root);

        } catch (Exception exc) {
            logException("Exporting drawing", exc);
        }
        return null;
    }

    /**
     * Apply the current color to all glyphs
     */
    public void applyColorToAll() {
        for (int i = 0; i < selectedGlyphs.size(); i++) {
            ((DrawingGlyph) selectedGlyphs.get(i)).setColor(getColor());
        }
    }


    /**
     * Apply the current z position to all glyphs
     *
     *
     * @param glyphs  the list of glyphs
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void applyZPosition(List glyphs)
            throws VisADException, RemoteException {
        for (int i = 0; i < glyphs.size(); i++) {
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
            if ( !glyph.isInFlatSpace()) {
                continue;
            }
            glyph.setZPosition((float) getZPosition());
            glyph.updateLocation();
        }
    }


    /**
     * Remove em all.
     */
    public void removeAllGlyphs() {
        try {
            while (glyphs.size() > 0) {
                removeGlyph((DrawingGlyph) glyphs.get(0));
            }
        } catch (Exception exc) {
            logException("Removing drawings", exc);
        }
        selectionChanged();
    }





    /**
     * Set the width of the lines.
     * @param width width of lines.
     */
    public void setLineWidth(int width) {
        lineWidth = width;
    }

    /**
     * Get the line width
     *
     * @return The line width
     */
    public int getLineWidth() {
        return lineWidth;
    }




    /**
     * Get an array of all the current times in the display
     *
     * @return All the animation times
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DateTime[] getAllTimes() throws VisADException, RemoteException {
        DateTime[] times     = null;
        Animation  animation = getViewAnimation();
        if (animation == null) {
            return times;
        }
        Set timeSet = animation.getSet();
        if (timeSet == null) {
            return times;
        }
        if (timeSet instanceof Gridded1DSet) {
            return DateTime.timeSetToArray((Gridded1DSet) timeSet);
        }
        return new DateTime[] {
            new DateTime(
                (Real) ((SingletonSet) timeSet).getData().getComponent(0)) };
    }


    /**
     * Get the current time in the animation
     *
     * @return The current time
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public Real getCurrentTime() throws VisADException, RemoteException {
        Animation animation = getViewAnimation();
        if (animation == null) {
            return null;
        }
        if (animation.getNumSteps() == 0) {
            return null;
        }
        Real r = animation.getAniValue();
        if (r.isMissing()) {
            return null;
        }
        return r;
    }


    /**
     * Get the animation set
     *
     * @return animation set
     *
     * @throws RemoteException  On badness
     * @throws VisADException On badness
     */
    public Set getAnimationSet() throws VisADException, RemoteException {
        Animation animation = getViewAnimation();
        if (animation == null) {
            return null;
        }
        return animation.getSet();
    }


    /**
     * If we are not drawing in the current time then return null. Else return the current time.
     *
     * @return The time to use for a glyph
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public Real getTimeForGlyph() throws VisADException, RemoteException {
        if (useTimeCbx == null) {
            return null;
        }

        if ( !useTimeCbx.isSelected()) {
            return null;
        }
        Animation animation = getViewAnimation();
        if (animation == null) {
            return null;
        }
        if (animation.getNumSteps() == 0) {
            return null;
        }
        Real r = animation.getAniValue();
        if (r.isMissing()) {
            return null;
        }
        return r;
    }


    /**
     * Get the text justification value
     *
     * @return h justification
     */
    public String getJustification() {
        if (justificationBox == null) {
            return TextGlyph.JUST_LEFT;
        }
        return ((String) justificationBox.getSelectedItem()).toLowerCase();
    }

    /**
     * Get the vert text justification value
     *
     * @return The vert justification_
     */
    public String getVerticalJustification() {
        if (vertJustificationBox == null) {
            return TextGlyph.JUST_BOTTOM;
        }
        return ((String) vertJustificationBox.getSelectedItem())
            .toLowerCase();
    }



    /**
     * Get the font from the ui widget
     *
     * @return The font to use for new text glyphs
     */
    public Font getFont() {
        if (fontBox == null) {
            return null;
        }
        Font font =
            (Font) ((TwoFacedObject) fontBox.getSelectedItem()).getId();
        int fontSize = ((Integer) fontSizeBox.getSelectedItem()).intValue();
        return font.deriveFont((float) fontSize);
    }


    /**
     * Set the line data.
     * @deprecated
     * @param lines  set of lines to use
     */
    public void setLines(UnionSet lines) {}


    /**
     * Enable/disable the drawing
     *
     * @deprecated
     * @param b   true to enable
     */
    public void setActive(boolean b) {}




    /**
     * Set the DrawingGlyphs property.
     *
     * @param value The new value for DrawingGlyphs
     */
    public void setGlyphs(List value) {
        glyphs = value;
    }

    /**
     * Get the Glyphs property.
     *
     * @return The Glyphs
     */
    public List getGlyphs() {
        //If were are display only then don't save the glyphs
        if (displayOnly) {
            return new ArrayList();
        }
        return glyphs;
    }


    /**
     * Set the IgnoreTime property.
     *
     * @param value The new value for IgnoreTime
     */
    public void setIgnoreTime(boolean value) {
        ignoreTime = value;
    }

    /**
     * Get the IgnoreTime property.
     *
     * @return The IgnoreTime
     */
    public boolean getIgnoreTime() {
        if (ignoreTimeCbx != null) {
            return ignoreTimeCbx.isSelected();
        }
        return ignoreTime;
    }


    /**
     * Set the UseTime property.
     *
     * @param value The new value for UseTime
     */
    public void setUseTime(boolean value) {
        useTime = value;
    }

    /**
     * Get the UseTime property.
     *
     * @return The UseTime
     */
    public boolean getUseTime() {
        if (useTimeCbx != null) {
            return useTimeCbx.isSelected();
        }
        return useTime;
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
     * Set the Enabled property.
     *
     * @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Get the Enabled property.
     *
     * @return The Enabled
     */
    public boolean getEnabled() {
        if (enabledCbx != null) {
            return enabledCbx.isSelected();
        }
        return enabled;
    }


    /**
     * Set the editorTitle property.
     *
     * @param value The new value for editorTitle
     */
    public void setEditorTitle(String value) {
        editorTitle = value;
    }

    /**
     * Get the Editor Title property.
     *
     * @return The Title
     */
    public String getEditorTitle() {
        return editorTitle;
    }




    /**
     * Clear the cursor in the main display
     */
    private void clearCursor() {
        setCursor(null);
    }

    /**
     * Set the cursor in the main display
     *
     * @param c  The cursor id
     */
    private void setCursor(int c) {
        setCursor(Cursor.getPredefinedCursor(c));
    }

    /**
     * Set the cursor in the main display
     *
     * @param c The cursor
     */
    private void setCursor(Cursor c) {
        getViewManager().setCursorInDisplay(c);
    }



    /**
     *  Set the Filled property.
     *
     *  @param value The new value for Filled
     */
    public void setFilled(boolean value) {
        filled = value;
    }

    /**
     *  Get the Filled property.
     *
     *  @return The Filled
     */
    public boolean getFilled() {
        if (filledCbx != null) {
            return filledCbx.isSelected();
        }
        return filled;
    }


    /**
     *  Set the Straight property.
     *
     *  @param value The new value for Straight
     */
    public void setStraight(boolean value) {
        straight = value;
    }

    /**
     *  Get the Straight property.
     *
     *  @return The Straight
     */
    public boolean getStraight() {
        if (straightCbx != null) {
            return straightCbx.isSelected();
        }
        return straight;
    }

    /**
     * Set the FullLatLon property.
     *
     * @param value The new value for FullLatLon
     */
    public void setFullLatLon(boolean value) {
        fullLatLon = value;
    }

    /**
     * Get the FullLatLon property.
     *
     * @return The FullLatLon
     */
    public boolean getFullLatLon() {
        if (fullLatLonCbx != null) {
            return fullLatLonCbx.isSelected();
        }
        return fullLatLon;
    }



    /**
     * Evalue the given jython for the glyph
     *
     * @param glyph The glyph
     * @param jython The jython
     */
    public void evaluateGlyphJython(DrawingGlyph glyph, String jython) {
        glyphJython = jython;
        PythonInterpreter interpreter =
            getControlContext().getJythonManager().createInterpreter();
        interpreter.set("glyph", glyph);
        interpreter.set("control", this);
        interpreter.exec(jython);
    }


    /**
     * Set the GlyphJython property.
     *
     * @param value The new value for GlyphJython
     */
    public void setGlyphJython(String value) {
        glyphJython = value;
    }

    /**
     * Get the GlyphJython property.
     *
     * @return The GlyphJython
     */
    public String getGlyphJython() {
        return glyphJython;
    }




    /**
     * Class GlyphTable
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.145 $
     */
    private class GlyphTable extends JTable {

        /** table model */
        GlyphTableModel myTableModel;

        /**
         * ctor
         *
         *
         * @param tableModel my model
         */
        public GlyphTable(GlyphTableModel tableModel) {
            super(tableModel);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            setToolTipText(
                "<html>Double Click to  show properties<br>Delete to delete</html>");
            this.myTableModel = tableModel;
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent ke) {
                    if ((ke.getKeyCode() == KeyEvent.VK_F)
                            && ke.isControlDown()) {
                        int[] rows = getSelectedRows();
                        int   row  = -1;
                        for (int i = 0; i < rows.length; i++) {
                            row = rows[i];
                            if ((row >= 0) && (row < glyphs.size())) {
                                DrawingGlyph glyph =
                                    (DrawingGlyph) glyphs.get(row);
                                if (glyph instanceof PolyGlyph) {
                                    try {
                                        ((PolyGlyph) glyph).doFlythrough();
                                    } catch (Exception exc) {
                                        logException("", exc);
                                    }
                                    return;
                                }
                            }
                        }
                    }



                    if (GuiUtils.isDeleteEvent(ke)) {
                        int[]   rows      = getSelectedRows();
                        List    tmpGlyphs = new ArrayList(glyphs);
                        boolean orig      = glyphTableModel.ignoreChanges;
                        glyphTableModel.ignoreChanges = true;
                        int row = -1;
                        for (int i = 0; i < rows.length; i++) {
                            row = rows[i];
                            if ((row >= 0) && (row < tmpGlyphs.size())) {
                                DrawingGlyph glyph =
                                    (DrawingGlyph) tmpGlyphs.get(row);
                                removeGlyph(glyph);
                            }
                        }
                        glyphTableModel.ignoreChanges = orig;
                        glyphTableModel.fireTableStructureChanged();
                        if (row >= 0) {
                            row--;
                            if (row < 0) {
                                row = 0;
                            }
                            if (row < glyphs.size()) {
                                setRowSelectionInterval(row, row);
                            }
                        }

                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    final int row = rowAtPoint(e.getPoint());
                    if ((row < 0) || (row >= glyphs.size())) {
                        return;
                    }
                    try {
                        DrawingGlyph glyph = (DrawingGlyph) glyphs.get(row);
                        if (e.getClickCount() > 1) {
                            doProperties(Misc.newList(glyph));
                        }
                    } catch (Exception exc) {
                        logException("", exc);
                    }
                }
            });
        }

        /**
         * handle event
         *
         * @param e event
         */
        public void valueChanged(ListSelectionEvent e) {
            super.valueChanged(e);
            if (myTableModel == null) {
                return;
            }
            if (myTableModel.ignoreChanges) {
                return;
            }
            boolean orig = glyphTableModel.ignoreChanges;
            glyphTableModel.ignoreChanges = true;
            int[] rows     = getSelectedRows();
            List  selected = new ArrayList();
            for (int i = 0; i < rows.length; i++) {
                int row = rows[i];
                if ((row >= 0) && (row < glyphs.size())) {
                    DrawingGlyph glyph = (DrawingGlyph) glyphs.get(row);
                    if (glyph.isSelectable()) {
                        selected.add(glyph);
                    }
                }
            }
            try {
                setSelection(selected, false);
            } catch (Exception exc) {
                logException("setting selection", exc);
            }
            glyphTableModel.ignoreChanges = orig;
        }

        /**
         * The glyphs changed. Update the table
         */
        public void glyphsChanged() {
            glyphTableModel.fireTableStructureChanged();
            selectionChanged();
        }

        /**
         * Selectio  changed. Update the table.
         */
        protected void selectionChanged() {
            if (glyphTableModel.ignoreChanges) {
                return;
            }
            boolean orig = glyphTableModel.ignoreChanges;
            glyphTableModel.ignoreChanges = true;
            clearSelection();
            for (int row = 0; row < glyphs.size(); row++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(row);
                if (isSelected(glyph)) {
                    getSelectionModel().addSelectionInterval(row, row);
                }
            }
            glyphTableModel.ignoreChanges = orig;
        }
    }


    /**
     * Class GlyphTableModel shows locations
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.145 $
     */
    private class GlyphTableModel extends AbstractTableModel {

        /** Ignore any events */
        boolean ignoreChanges = false;

        /**
         * ctor
         *
         */
        public GlyphTableModel() {}


        /**
         * Only fire event when ignoreChanges flag is ok
         */
        public void fireTableStructureChanged() {
            if ( !ignoreChanges) {
                super.fireTableStructureChanged();
            }
        }

        /**
         * is cell editable
         *
         * @param rowIndex rowindex
         * @param columnIndex colindex_
         *
         * @return is editable
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         * Num rows
         *
         * @return num rows
         */
        public int getRowCount() {
            if (glyphs == null) {
                return 0;
            }
            return glyphs.size();
        }

        /**
         * num cols
         *
         * @return num cols
         */
        public int getColumnCount() {
            if ( !editable) {
                return 3;
            } else {
                return 4;
            }
        }

        /**
         * get cell value
         *
         * @param row row
         * @param column col
         *
         * @return cell value
         */
        public Object getValueAt(int row, int column) {
            if (row >= glyphs.size()) {
                return "";
            }
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(row);
            if (column == 0) {
                String name = glyph.getName();
                if ( !glyph.getVisibleFlag()) {
                    name = name + " (hidden)";
                }
                return name;
            }
            if (column == 1) {
                return glyph.getDescription();
            }
            if ( !editable) {
                return glyph.getExtraDescription();
            }
            if (column == 2) {
                int type = glyph.getCoordType();
                for (int i = 0; i < DrawingGlyph.COORD_TYPES.length; i++) {
                    if (type == DrawingGlyph.COORD_TYPES[i]) {
                        return DrawingGlyph.COORD_LABELS[i];
                    }
                }
                return "";
            }
            return glyph.getExtraDescription();
        }


        /**
         * col name
         *
         * @param column column
         *
         * @return col name
         */
        public String getColumnName(int column) {
            if (column == 0) {
                return "Name";
            }
            if (column == 1) {
                return "Type";
            }
            if ( !editable) {
                return "";
            }
            if (column == 2) {
                return "Coordinates";
            }
            return "";
        }
    }


    /**
     * Get the default name to use
     *
     *
     * @param glyph the glyph
     * @return The name
     */
    protected String getGlyphNameDefault(DrawingGlyph glyph) {
        return "Glyph";
    }

    /**
     *  Set the ShowFronts property.
     *
     *  @param value The new value for ShowFronts
     */
    public void setShowFronts(boolean value) {
        showFronts = value;
    }

    /**
     *  Get the ShowFronts property.
     *
     *  @return The ShowFronts
     */
    public boolean getShowFronts() {
        return showFronts;
    }

    /**
     *  Set the FrontScale property.
     *
     *  @param value The new value for FrontScale
     */
    public void setFrontScale(double value) {
        frontScale = value;
    }

    /**
     *  Get the FrontScale property.
     *
     *  @return The FrontScale
     */
    public double getFrontScale() {
        if (scaleFld != null) {
            return Double.parseDouble(scaleFld.getText().trim());
        }
        return frontScale;
    }



    /**
     *  Set the FrontDisplay property.
     *
     *  @param value The new value for FrontDisplay
     */
    public void setFrontDisplay(boolean value) {
        frontDisplay = value;
    }

    /**
     *  Get the FrontDisplay property.
     *
     *  @return The FrontDisplay
     */
    public boolean getFrontDisplay() {
        return frontDisplay;
    }



}
