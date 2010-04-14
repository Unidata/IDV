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
import ucar.unidata.data.gis.Transect;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.TransectViewManager;


import ucar.unidata.idv.VMManager;

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
import ucar.unidata.util.TwoFacedObject;

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
 * @version $Revision: 1.41 $
 */

public class TransectDrawingControl extends DrawingControl {

    /** Do we show the normal transects */
    private boolean showRegularTransects = true;

    /** Do we show the display  transects */
    private boolean showDisplayTransects = true;


    /**
     * Do we automatically update the transect view manager and global list of transects
     *  when we change any of the transects and/or select on
     */
    private boolean autoUpdateTransect = true;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public TransectDrawingControl() {
        currentCmd = CMD_STRETCH;
        setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(4);
    }



    /**
     * Overwrite this even handler to apply the z position to all of
     * the transect glyphs
     *
     * @param value New z position
     */
    public void zSliderChanged(int value) {
        super.zSliderChanged(value);
        try {
            List glyphs = new ArrayList(getGlyphs());
            for (int glyphIdx = 0; glyphIdx < glyphs.size(); glyphIdx++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(glyphIdx);
                glyph.setZPosition((float) getZPosition());
                glyph.updateLocation();
            }
        } catch (Exception e) {
            logException("Setting Z position ", e);
        }
    }



    /**
     * Is this a normal transect. Is it not a display transect and does
     * it have the normal number of points.
     *
     * @param glyph The glyph
     *
     * @return Is normal
     */
    private boolean isThisANormalTransect(DrawingGlyph glyph) {
        if ( !(glyph instanceof TransectGlyph)) {
            return false;
        }
        if (glyph.getPoints().size() < 2) {
            return false;
        }
        if (((TransectGlyph) glyph).getForDisplay()) {
            return false;
        }
        return true;
    }

    /**
     * The selections set of glyphs has changed.
     * Update the global transects if needed
     */
    protected void selectionChanged() {
        super.selectionChanged();
        if ((selectedGlyphs.size() > 0) && autoUpdateTransect) {
            TransectGlyph glyph = (TransectGlyph) selectedGlyphs.get(0);
            if (isThisANormalTransect(glyph)) {
                Transect transect = makeTransect(glyph);
                getControlContext().getIdv().getVMManager().setTransect(
                    transect);
            }
        }
    }



    /**
     * Write out the transects when we've changed
     *
     * @param glyph Which glyph
     */
    protected void transectChanged(DrawingGlyph glyph) {
        if (autoUpdateTransect && isThisANormalTransect(glyph)) {
            getControlContext().getIdv().getVMManager().setTransect(
                makeTransect((TransectGlyph) glyph));
            writeTransects();
        }
    }


    /**
     * The user is done moving or resizing the given glyph
     *
     * @param glyph The glyph that was moved
     */
    protected void doneMovingGlyph(DrawingGlyph glyph) {
        super.doneMovingGlyph(glyph);
        transectChanged(glyph);
    }

    /**
     * Overwrite base class method to write out the transects on a delete
     *
     * @param glyph Which glyph
     */
    public void removeGlyph(DrawingGlyph glyph) {
        super.removeGlyph(glyph);
        transectChanged(glyph);
    }


    /**
     * All initialization is done
     */
    public void initDone() {
        super.initDone();
        try {
            List transects =
                getControlContext().getResourceManager().getTransects();


            for (int i = 0; i < transects.size(); i++) {
                Transect transect     = (Transect) transects.get(i);
                List     pointStrings = new ArrayList();
                List     points       = transect.getPoints();

                for (int ptIdx = 0; ptIdx < points.size(); ptIdx++) {
                    LatLonPointImpl llp = (LatLonPointImpl) points.get(ptIdx);
                    pointStrings.add(llp.getLatitude() + "");
                    pointStrings.add(llp.getLongitude() + "");
                }
                TransectGlyph newGlyph = new TransectGlyph(this, null,
                                             transect.getEditable());
                newGlyph.setStartText(transect.getStartText());
                newGlyph.setEndText(transect.getEndText());
                newGlyph.setLineWidth(4);
                if (transect.getColor() != null) {
                    newGlyph.setColor(transect.getColor());
                } else {
                    if (transect.getEditable()) {
                        newGlyph.setColor(getColor());
                    } else {
                        newGlyph.setColor(Color.blue);
                    }
                }
                newGlyph.setZPosition(0.0f);
                newGlyph.setCoordType(DrawingGlyph.COORD_LATLON);
                newGlyph.processPointStrings(pointStrings);
                newGlyph.setName(transect.getName());
                boolean glyphOk = true;
                for (int glyphIdx = 0; glyphIdx < glyphs.size(); glyphIdx++) {
                    DrawingGlyph existingGlyph =
                        (DrawingGlyph) glyphs.get(glyphIdx);
                    if ( !(existingGlyph instanceof TransectGlyph)) {
                        continue;
                    }
                    String existingName = existingGlyph.getName();
                    if (existingName == null) {
                        existingName = "";
                    }
                    if ( !Misc.equals(existingName, newGlyph.getName())) {
                        continue;
                    }
                    TransectGlyph existingTransectGlyph =
                        (TransectGlyph) existingGlyph;
                    Transect existingTransect =
                        makeTransect(existingTransectGlyph);
                    if (existingTransect.equals(transect)) {
                        glyphOk = false;
                        break;
                    }
                }
                if (glyphOk) {
                    addGlyph(newGlyph);
                }
            }
            checkTransectVisiblity();
            List vms =
                getControlContext().getIdv().getVMManager().getViewManagers();
            for (int i = 0; i < vms.size(); i++) {
                ViewManager vm = (ViewManager) vms.get(i);
                if (vm instanceof TransectViewManager) {
                    setDisplayedTransect((TransectViewManager) vm);
                }
            }
        } catch (Exception e) {
            logException("Reading transects", e);
        }
    }


    /**
     * Provide the list of shape commands to the base class
     *
     * @return Shape creation commands
     */
    protected List getShapeCommands() {
        return Misc.newList(GlyphCreatorCommand.CMD_TRANSECT);
    }

    /**
     * Add gui components to the list
     *
     * @param widgets List if gui components
     */
    protected void addControlWidgets(List widgets) {
        super.addControlWidgets(widgets);
        widgets.add(GuiUtils.filler());
        widgets.add(
            GuiUtils.left(
                GuiUtils.makeCheckbox(
                    "Automatically update transects", this,
                    "autoUpdateTransect")));
        widgets.add(GuiUtils.filler());
        widgets.add(GuiUtils.left(GuiUtils.makeCheckbox("Show transects",
                this, "showRegularTransects", null)));
        widgets.add(GuiUtils.filler());
        widgets.add(
            GuiUtils.left(
                GuiUtils.makeCheckbox(
                    "Show active transects", this, "showDisplayTransects",
                    null)));

    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void addFileMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Save Transects", this,
                                        "writeTransects"));
    }



    /**
     * Add and/or change the displayed transect for the given tvm.
     *
     * @param tvm The view manager
     *
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setDisplayedTransect(TransectViewManager tvm)
            throws VisADException, RemoteException {
        checkTransectVisiblity();
        TransectGlyph tg = findGlyph(tvm);
        if (tg == null) {
            //Should have it already created from the check call above
            tg = makeDisplayTransectGlyph(tvm);
        }
        Transect        transect     = tvm.getAxisTransect();
        LatLonPointImpl from = (LatLonPointImpl) transect.getPoints().get(0);
        LatLonPointImpl to = (LatLonPointImpl) transect.getPoints().get(1);
        List            pointStrings = new ArrayList();
        pointStrings.add(from.getLatitude() + "");
        pointStrings.add(from.getLongitude() + "");
        pointStrings.add(to.getLatitude() + "");
        pointStrings.add(to.getLongitude() + "");
        tg.processPointStrings(pointStrings);
        tg.setMaxDataDistance(tvm.getMaxDataDistance());
        tg.updateLocation();
    }

    /**
     * Write out the transect xml resource
     */
    public void writeTransects() {
        List glyphs    = getGlyphs();
        List transects = new ArrayList();
        for (int i = 0; i < glyphs.size(); i++) {
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
            if ( !isThisANormalTransect(glyph)) {
                continue;
            }
            transects.add(makeTransect((TransectGlyph) glyph));
        }
        getControlContext().getResourceManager().writeTransects(transects);
    }


    /**
     * Create a Transect from the given glyph
     *
     * @param glyph glyph
     *
     * @return Transect
     */
    private Transect makeTransect(TransectGlyph glyph) {
        List glyphPoints = glyph.getPoints();
        List points      = new ArrayList();
        for (int ptIdx = 0; ptIdx < glyphPoints.size(); ptIdx++) {
            EarthLocation elt = (EarthLocation) glyphPoints.get(ptIdx);
            LatLonPointImpl llp =
                new LatLonPointImpl(
                    elt.getLatLonPoint().getLatitude().getValue(),
                    elt.getLatLonPoint().getLongitude().getValue());
            points.add(llp);
        }
        Transect transect = new Transect(glyph.getName(), points);
        transect.setStartText(glyph.getStartText());
        transect.setEndText(glyph.getEndText());
        transect.setColor(glyph.getColor());
        return transect;
    }


    /**
     * Overwrite base class method to not show the filled cbx
     *
     * @return false
     */
    protected boolean showFilledCbx() {
        return false;
    }

    /**
     * Overwrite base class method to make the mode panel
     *
     * @param widgets List of panel widgets to add to
     */
    protected void makeModePanel(List widgets) {

        List        commands = Misc.newList(CMD_SELECT, CMD_MOVE,
                                            CMD_STRETCH);
        List        shapes   = getShapeCommands();
        ButtonGroup bg       = new ButtonGroup();
        widgets.add(GuiUtils.rLabel("Mode:"));
        widgets.add(GuiUtils.left(GuiUtils.hbox(makeButtonPanel(shapes, bg),
                makeButtonPanel(commands, bg), enabledCbx)));

    }



    /**
     * Don't show time widgets
     *
     * @return false
     */
    protected boolean showTimeWidgets() {
        return false;
    }

    /**
     * Don't show location widgets
     *
     * @return false
     */
    protected boolean showLocationWidgets() {
        return false;
    }



    /**
     * Use a different name
     *
     *
     * @param glyph The glyph
     * @return glyph name
     */
    protected String getGlyphNameDefault(DrawingGlyph glyph) {
        if ( !(glyph instanceof TransectGlyph)) {
            return super.getGlyphNameDefault(glyph);
        }
        if (((TransectGlyph) glyph).getForDisplay()) {
            return "Display Transect";
        }
        return "Transect";
    }


    /**
     * This is called by the VMManager to notify us that one of the
     * TransectViewManagers has changed its zoom.
     */
    public void transectViewsChanged() {
        checkTransectVisiblity();
    }

    /**
     * Set the AutoUpdateTransect property.
     *
     * @param value The new value for AutoUpdateTransect
     */
    public void setAutoUpdateTransect(boolean value) {
        autoUpdateTransect = value;
    }

    /**
     * Get the AutoUpdateTransect property.
     *
     * @return The AutoUpdateTransect
     */
    public boolean getAutoUpdateTransect() {
        return autoUpdateTransect;
    }


    /**
     * Set the ShowRegularTransects property.
     *
     * @param value The new value for ShowRegularTransects
     */
    public void setShowRegularTransects(boolean value) {
        showRegularTransects = value;
        if (getHaveInitialized()) {
            checkTransectVisiblity();
        }
    }


    /**
     * Get the ShowRegularTransects property.
     *
     * @return The ShowRegularTransects
     */
    public boolean getShowRegularTransects() {
        return showRegularTransects;
    }


    /**
     * Set the ShowDisplayTransects property.
     *
     * @param value The new value for ShowDisplayTransects
     */
    public void setShowDisplayTransects(boolean value) {
        showDisplayTransects = value;
        if (getHaveInitialized()) {
            checkTransectVisiblity();
        }
    }

    /**
     * Get the ShowDisplayTransects property.
     *
     * @return The ShowDisplayTransects
     */
    public boolean getShowDisplayTransects() {
        return showDisplayTransects;
    }


    /**
     * Extra check for the display visibility
     *
     * @param on visible
     */
    public void setDisplayVisibility(boolean on) {
        super.setDisplayVisibility(on);
        checkTransectVisiblity();
    }


    /**
     * Overwrite base class method to determine if we should show the glyph
     *
     * @param tg The glyph
     *
     * @return Should we show this
     */
    public boolean shouldBeVisible(DrawingGlyph tg) {
        if ( !getDisplayVisibility()) {
            return false;
        }
        if (isThisANormalTransect(tg)) {
            return showRegularTransects;
        }
        return showDisplayTransects;
    }

    /**
     * Ignore this call
     *
     * @param time The time
     */
    protected void timeChanged(Real time) {
        //noop
    }

    /**
     * This makes sure that we are showing all of the display transects
     * for the view managers
     *
     */
    private void checkTransectVisiblity() {
        try {
            boolean   meVisible = getDisplayVisibility();
            VMManager vmManager = getControlContext().getIdv().getVMManager();
            List vms = vmManager.getViewManagers(TransectViewManager.class);
            List      glyphs    = new ArrayList(getGlyphs());
            for (int glyphIdx = 0; glyphIdx < glyphs.size(); glyphIdx++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(glyphIdx);
                if ( !(glyph instanceof TransectGlyph)) {
                    continue;
                }
                TransectGlyph tg = (TransectGlyph) glyph;
                if (isThisANormalTransect(tg)) {
                    glyph.setVisible(meVisible && showRegularTransects);
                } else {
                    tg.setVisible(meVisible && showDisplayTransects);
                    if (tg.getViewDescriptor() != null) {
                        TransectViewManager tvm =
                            (TransectViewManager) VMManager
                                .findViewManagerInList(tg
                                    .getViewDescriptor(), vms);

                        if (tvm == null) {
                            removeGlyph(tg);
                        } else {
                            vms.remove(tvm);
                            checkDisplayTransectName(tg, tvm);
                        }
                    } else {}

                }
            }
            for (int i = 0; i < vms.size(); i++) {
                TransectViewManager tvm = (TransectViewManager) vms.get(i);
                TransectGlyph       tg  = findGlyph(tvm);
                if (tg == null) {
                    tg = makeDisplayTransectGlyph(tvm);
                }
            }
        } catch (Exception exc) {
            logException("Toggling transect visibility", exc);
        }
    }

    /**
     * Set the name of the glyph
     *
     * @param tg glyph
     * @param tvm the view manager
     */
    private void checkDisplayTransectName(TransectGlyph tg,
                                          TransectViewManager tvm) {
        String name = "Display";
        if ((tvm.getName() != null) && (tvm.getName().length() > 0)) {
            name = name + ": " + tvm.getName();
        }
        tg.setName(name);
    }


    /**
     * Make a TransectGlyph that shows the given ViewManager
     *
     * @param tvm view manager
     *
     * @return glyph
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private TransectGlyph makeDisplayTransectGlyph(TransectViewManager tvm)
            throws VisADException, RemoteException {
        TransectGlyph tg = new TransectGlyph(this, null, false, true);
        tg.setColor(Color.green);
        tg.setZPosition(0.0f);
        tg.setCoordType(DrawingGlyph.COORD_LATLON);
        tg.setLineWidth(2);
        tg.initFinal();
        tg.setViewDescriptor(tvm.getViewDescriptor());
        addGlyph(tg);
        checkDisplayTransectName(tg, tvm);
        return tg;
    }

    /**
     * Find the TransectGlyph that shows the given view manager
     *
     * @param tvm view  manager
     *
     * @return the glyph or null if none found
     */
    private TransectGlyph findGlyph(TransectViewManager tvm) {
        List glyphs = new ArrayList(getGlyphs());
        for (int glyphIdx = 0; glyphIdx < glyphs.size(); glyphIdx++) {
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(glyphIdx);
            if ( !(glyph instanceof TransectGlyph)) {
                continue;
            }
            TransectGlyph tg = (TransectGlyph) glyph;
            if (tg.getViewDescriptor() == null) {
                continue;
            }
            if (tvm.isDefinedBy(tg.getViewDescriptor())) {
                return tg;
            }
        }
        return null;
    }



}
