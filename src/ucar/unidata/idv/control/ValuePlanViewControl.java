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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.*;

import ucar.unidata.gis.SpatialGrid;
import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.*;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import ucar.visad.display.GridValueDisplayable;
import ucar.visad.display.LineDrawing;

import visad.*;
import visad.RealType;
import visad.VisADException;


import java.awt.*;

import java.awt.Color;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.JCheckBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.*;



// $Id: ValuePlanViewControl.java,v 1.25 2003/10/14 17:18:39 dmurray Exp $ 

/**
 * Class for controlling the display of plan view text plots of
 * gridded data.
 * @author Unidata Development Team
 * @version $Revision: 1.25 $
 */
public class ValuePlanViewControl extends PlanViewControl {

    /** local copy of the grid display */
    private GridValueDisplayable pointDisplay = null;

    /** The scale the user can enter */
    private float layoutScale = 1.0f;

    /** layout model */
    private StationModel layoutModel = null;

    /** grid for decluttering */
    private SpatialGrid stationGrid;

    /** flag for decluttering */
    private boolean declutter = true;

    /** decluttering filter factor */
    private float declutterFilter = 1.0f;

    /** The widget to show the layout model in the gui */
    protected LayoutModelWidget layoutModelWidget;

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ValuePlanViewControl() {
        setAttributeFlags(FLAG_SKIPFACTOR);
    }

    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     *
     * @return Contour2DDisplayable for this instance.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        pointDisplay = new GridValueDisplayable("plan_text_" + paramName,
                getControlContext().getJythonManager());
        pointDisplay.setStationModel(getLayoutModel());
        addAttributedDisplayable(pointDisplay);
        return pointDisplay;
    }


    /**
     * Init is done
     */
    public void initDone() {
        super.initDone();
        loadDataInThread();
    }

    /**
     * Property change method.
     *
     * @param evt   event to act on
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                StationModelManager.PROP_RESOURCECHANGE)) {

            StationModel changedModel = (StationModel) evt.getNewValue();
            handleChangedStationModel(changedModel);
        } else if (evt.getPropertyName().equals(
                StationModelManager.PROP_RESOURCEREMOVE)) {
            StationModel changedModel = (StationModel) evt.getOldValue();
            if (layoutModel.getName().equals(changedModel.getName())) {
                handleStationModelChange();
            }
        }
        super.propertyChange(evt);
    }


    /**
     * The station model changed
     *
     * @param changedModel The changed station model_
     */
    protected void handleChangedStationModel(StationModel changedModel) {
        if (layoutModel.getName().equals(changedModel.getName())) {
            setLayoutModel(changedModel);
        }

    }

    /**
     * Set the layout model
     *
     * @param lm layout model
     */
    public void setLayoutModelFromWidget(StationModel lm) {
        setLayoutModel(lm);
    }


    /**
     *  The station model changed
     */
    private void handleStationModelChange() {
        StationModel changedModel = null;
        if (layoutModel != null) {
            changedModel =
                getControlContext().getStationModelManager().getStationModel(
                    layoutModel.getName());
        }
        if (changedModel != null) {
            if (changedModel != layoutModel) {
                setLayoutModel(changedModel);
            }
            return;
        }
        setLayoutModel(getControlContext().getStationModelManager()
            .getDefaultStationModel());
    }


    /**
     * Listen for DisplayEvents
     *
     * @param evt The event
     */
    public void handleDisplayChanged(DisplayEvent evt) {
        try {
            int        id         = evt.getId();
            InputEvent inputEvent = evt.getInputEvent();
            if (id == DisplayEvent.MAPS_CLEARED) {
                setScaleOnLayout();
            } else {
                super.handleDisplayChanged(evt);
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }

    /**
     * Method to call if projection changes. This will reset the
     * viewScape, bounds and llBounds held by this object and then
     * load data
     */
    public void projectionChanged() {
        super.projectionChanged();
        loadDataInThread();
    }

    /**
     * Respond to changes in the control.
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        loadDataInThread();
    }

    /**
     * Add into the given the  widgets  for the different attributes
     *
     * @param controlWidgets List of {@link ControlWidget}s to add into
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        JCheckBox toggle = new JCheckBox("", declutter);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDeclutter(((JCheckBox) e.getSource()).isSelected());
                loadDataInThread();
            }
        });
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Declutter:"),
                                             toggle));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Layout Model:"),
                makeLayoutModelWidget()));
        super.getControlWidgets(controlWidgets);

    }

    /**
     * A utility method that sets the wait cursor and calls loadData in a separate thread .
     */
    protected void loadDataInThread() {
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                try {
                    if (getGridDisplayable() != null) {
                        if (currentSlice == null) {
                            Object newLevel = currentLevel;
                            currentLevel = null;
                            loadDataAtLevel(newLevel);
                        } else {
                            getGridDisplayable().loadData(
                                getSliceForDisplay(currentSlice));
                        }
                    }
                } catch (Exception exc) {
                    logException("Loading data", exc);
                }
                showNormalCursor();

            }
        });
    }

    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        FieldImpl subset      = super.getSliceForDisplay(slice);
        FieldImpl stationData = GridUtil.getGridAsPointObs(subset);
        if (declutter) {
            try {
                stationData = doDeclutter(stationData);
            } catch (RemoteException re) {
                logException("getSliceForDisplay: doDeclutter", re);
            }
        }
        return stationData;
    }


    /**
     * Set the current station model view.
     *
     * @param model  station model layout
     */
    public void setLayoutModel(StationModel model) {
        layoutModel = model;
        if (layoutModelWidget != null) {
            layoutModelWidget.setLayoutModel(model);
        }
        if (getHaveInitialized() && (pointDisplay != null)) {
            try {
                pointDisplay.setStationModel(layoutModel);
            } catch (Exception excp) {
                logException("setting layout", excp);
            }
        }
    }

    /**
     * Get the current layout model view.
     *
     * @return station model layout
     */
    public StationModel getLayoutModel() {
        if (layoutModel == null) {
            layoutModel = makeLayoutModel();
        }
        return layoutModel;
    }

    /**
     * Get the scale the user can enter
     *
     * @return The scale
     */
    public float getLayoutScale() {
        return layoutScale;
    }

    /**
     * Set the scale the user can enter
     *
     * @param f The scale
     */
    public void setLayoutScale(float f) {
        layoutScale = f;
        if (pointDisplay != null) {
            try {
                setScaleOnLayout();
            } catch (Exception exc) {
                logException("Setting scale ", exc);
            }
        }
    }

    /**
     *  A utility to set the scale on the layout model dislayable
     *
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void setScaleOnLayout() throws RemoteException, VisADException {
        setScaleOnLayout(getDisplayScale() * layoutScale);
    }

    /**
     *  A utility to set the scale on the dislayable
     *
     * @param f The new scale value
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void setScaleOnLayout(float f)
            throws RemoteException, VisADException {
        if (pointDisplay != null) {
            pointDisplay.setScale(f);
        }
    }

    /**
     * Set whether this DisplayControl should be decluttered or not.
     * Used by XML persistence.
     *
     * @param v true to declutter
     */
    public void setDeclutter(boolean v) {
        declutter = v;
    }

    /**
     * Get whether this DisplayControl should be decluttered or not.
     *
     * @return true if set to declutter
     */
    public boolean getDeclutter() {
        return declutter;
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
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }


    /**
     * Set whether the filtering for decluttering.
     * Used by XML persistence.
     *
     * @param filter value of 1 (default) for no overlap (default).
     *               0 &lt; filter &lt; 1 allows some data overlap.
     *               filter &gt; 1 causes data to be more widely spaced.
     */
    public void setDeclutterFilter(float filter) {
        declutterFilter = filter;
    }

    /**
     * Get whether this DisplayControl should be decluttered or not.
     *
     * @return weighting for decluttering.
     */
    public float getDeclutterFilter() {
        return declutterFilter;
    }


    /**
     * Popup the station model editor
     */
    public void editLayoutModel() {
        getControlContext().getStationModelManager().show(layoutModel);
    }

    /**
     *  A utility to get the scale from the dislayable
     *
     * @return The scale
     */
    protected float getScaleFromDisplayable() {
        if (pointDisplay != null) {
            return pointDisplay.getScale();
        }
        return 0.0f;
    }


    /**
     * Make the gui widget for setting the layout model
     *
     * @return the widget
     */
    protected JPanel makeLayoutModelWidget() {
        final JTextField scaleField =
            new JTextField(Misc.format(layoutScale), 5);
        ActionListener scaleListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    //System.err.println("display scale = "
                    //                   + getDisplayScale());
                    setLayoutScale(
                        (float) Misc.parseNumber(scaleField.getText()));
                } catch (Exception nfe) {
                    userErrorMessage("Bad scale format");
                }
            }
        };
        scaleField.addActionListener(scaleListener);
        JButton scaleBtn = new JButton("Apply");
        scaleBtn.addActionListener(scaleListener);

        JPanel stationModelPanel =
            GuiUtils.hbox(
                GuiUtils.left(
                    layoutModelWidget =
                        new LayoutModelWidget(
                            this, this, "setLayoutModelFromWidget",
                            getLayoutModel())), GuiUtils.rLabel("   Scale:"),
                                GuiUtils.hflow(
                                    Misc.newList(scaleField, scaleBtn), 4,
                                    0));

        return stationModelPanel;

    }

    /**
     * Creates a station model from the supplied parameters.
     * @return
     */
    private StationModel makeLayoutModel() {
        StationModel layout = null;

        String       name   = "Grids>Grid Value";
        layout = getControlContext().getStationModelManager().getStationModel(
            name);
        if (layout == null) {
            LogUtil.userErrorMessage("Unable to find layout model: " + name
                                     + ". Using default");
        }
        if (layout == null) {
            layout =
                getControlContext().getStationModelManager()
                    .getDefaultStationModel();
        }
        return layout;
    }

    /**
     * Declutters the observations.  This is just a wrapper around
     * the real decluttering in {@link #doTheActualDecluttering(FieldImpl)}
     * to handle the case where there is a time sequence of observations.
     *
     * @param  obs initial field of observations.
     *
     * @return a decluttered version of obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl doDeclutter(FieldImpl obs)
            throws VisADException, RemoteException {


        long      millis           = System.currentTimeMillis();
        boolean   isTimeSequence   = GridUtil.isTimeSequence(obs);
        FieldImpl declutteredField = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            declutteredField = new FieldImpl((FunctionType) obs.getType(),
                                             timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualDecluttering(oneTime);
                if (subTime != null) {
                    declutteredField.setSample(i, subTime, false);
                }
            }
        } else {
            declutteredField = doTheActualDecluttering(obs);
        }
        //System.out.println("Subsetting took : " +
        //                    (System.currentTimeMillis() - millis) + " ms");
        return declutteredField;
    }

    /**
     * a     * Declutters a single timestep of observations.
     *
     * @param pointObs  point observations for one timestep.
     *
     * @return a decluttered version of pointObs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl doTheActualDecluttering(FieldImpl pointObs)
            throws VisADException, RemoteException {
        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField    = null;
        Set       domainSet   = pointObs.getDomainSet();
        int       numObs      = domainSet.getLength();
        Vector    v           = new Vector();

        long      t1          = System.currentTimeMillis();
        Rectangle glyphBounds = getLayoutModel().getBounds();
        float myScale = getScaleFromDisplayable() * .0025f
                        * getDeclutterFilter();
        //System.out.println("\ndecluttering  myScale=" + myScale +
        //                           " filter=" +getDeclutterFilter());
        Rectangle2D scaledGlyphBounds =
            new Rectangle2D.Double(glyphBounds.getX() * myScale,
                                   glyphBounds.getY() * myScale,
                                   glyphBounds.getWidth() * myScale,
                                   glyphBounds.getHeight() * myScale);
        NavigatedDisplay   navDisplay = getNavigatedDisplay();

        Rectangle2D.Double obBounds   = new Rectangle2D.Double();
        obBounds.width  = scaledGlyphBounds.getWidth();
        obBounds.height = scaledGlyphBounds.getHeight();

        if (stationGrid == null) {
            stationGrid = new SpatialGrid(200, 200);
        }
        stationGrid.clear();
        stationGrid.setGrid(getBounds(), scaledGlyphBounds);
        //if (getDeclutterFilter() < 0.3f) {
        //      stationGrid.setOverlap((int)((1.0-getDeclutterFilter())*100));
        //      stationGrid.setOverlap(          (int)((.5f-getDeclutterFilter())*100));
        //} else {
        //      stationGrid.setOverlap(0);
        //}

        double[] xyz = new double[3];
        //TODO: The repeated getSpatialCoords is a bit expensive
        for (int i = 0; i < numObs; i++) {
            if (pointObs.getSample(i) instanceof PointOb) {  //Ghansham
                PointOb ob = (PointOb) pointObs.getSample(i);
                xyz = navDisplay.getSpatialCoordinates(ob.getEarthLocation(),
                        xyz);
                obBounds.x = xyz[0];
                obBounds.y = xyz[1];
                if (stationGrid.markIfClear(obBounds, "")) {
                    v.add(ob);  // is in the bounds
                }
            }
        }
        //      stationGrid.print();
        long t2 = System.currentTimeMillis();


        if (v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), 1));
            retField.setSample(0, pointObs.getSample(0), false);
        } else if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), v.size()));
            retField.setSamples((PointOb[]) v.toArray(new PointOb[v.size()]),
                                false, false);
        }

        long t3 = System.currentTimeMillis();
        //System.err.println("size:" + v.size() +" declutter:" + (t2-t1) + " " + (t3-t2));


        return retField;
    }

    /**
     * Get the bounds for the visible portion of the screen.
     *
     * @return bounds in VisAD screen coordinates.
     */
    protected Rectangle2D getBounds() {
        return calculateRectangle();
    }

    /**
     * Calculate the LatLonBounds based on the VisAD screen bound.  This
     * uses the projection for the navigated display and the screen bounds
     *
     * @param screenBounds  VisAD screen bounds.
     *
     * @return  LinearLatLonSet of screen bounds in lat/lon coordinates.
     */
    protected LinearLatLonSet calculateLatLonBounds(
            Rectangle2D screenBounds) {
        if ((screenBounds.getWidth() == 0)
                || (screenBounds.getHeight() == 0)) {
            return null;
        }

        LinearLatLonSet bounds = null;
        try {

            Rectangle2D.Double rect = getNavigatedDisplay().getLatLonBox();

            bounds =
                new LinearLatLonSet(RealTupleType.LatitudeLongitudeTuple,
                                    rect.y, rect.y + rect.height, 11, rect.x,
                                    rect.x + rect.width, 11);

        } catch (Exception e) {
            try {
                bounds =
                    new LinearLatLonSet(RealTupleType.LatitudeLongitudeTuple,
                                        -90, 90, 19, -180, 180, 37);
            } catch (Exception ne) {
                logException("calculating LLLSet ", ne);
            }
        }
        return bounds;
    }



}
