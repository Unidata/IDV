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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.AnimationWidget;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.quantities.Altitude;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;



import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenuItem;


/**
 * <p>Creates an aerological sounding diagram for soundings.  Adds a line probe
 * to the main display and uses its position to interpolate the data.</p>
 *
 * @author Unidata Development Team
 * @version $Revision: 1.14 $Date: 2007/03/09 11:55:55 $
 */
public class GriddedSoundingControl extends AerologicalSoundingControl {

    /** data node for this control */
    private SoundingDataNode dataNode;

    /** the probe */
    private LineProbe probe;

    /** displayable for grid locations */
    private DisplayableData gridLocs;

    /** displayable for grid locations */
    private DisplayableData timesHolder;

    /** widget */
    private Component widget;


    /**
     * Constructs from nothing.
     *
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public GriddedSoundingControl() throws VisADException, RemoteException {

        super(false);
        gridLocs = new LineDrawing("GriddedSoundingControl gridLocs");
        probe    = new LineProbe();
        probe.setVisible(true);
        setSounding(0);
    }

    /**
     * <p>Creates and returns the {@link ucar.unidata.data.DataInstance}
     * corresponding to a {@link ucar.unidata.data.DataChoice}. Returns
     * <code>null</code> if the {@link ucar.unidata.data.DataInstance} was
     * somehow invalid.</p>
     *
     * <p>This method is invoked by the overridable method {@link
     * #setData(DataChoice)}.</p>
     *
     * @param dataChoice       The {@link ucar.unidata.data.DataChoice} from
     *                         which to create a
     *                         {@link ucar.unidata.data.DataInstance}.
     * @return                 The created
     *                         {@link ucar.unidata.data.DataInstance} or
     *                         <code>null</code>.
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     * protected DataInstance doMakeDataInstance(DataChoice dataChoice)
     *       throws RemoteException, VisADException {
     *
     *   DataInstance inst = super.doMakeDataInstance(dataChoice);
     *
     *   if (inst != null) {
     *       dataNode = SoundingDataNode.getInstance(new Listener());
     *
     *       dataNode.setData(inst.getData(getDataSelection()));
     *   }
     *
     *   return inst;
     * }
     */

    /**
     * Constructs the vertical profile display and control buttons
     *
     * @param dataChoice       The data for this instance.
     * @return                 <code>true</code> if and only if this instance
     *                         was correctly initialized.
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException couldn't create a remote object needed
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        /*
         * Initialize the superclass.
         */
        if ( !super.init()) {
            return false;
        }
        setSpatialLoci(gridLocs);

        if ( !setData(dataChoice)) {
            return false;
        }


        /*
         * Add the listener here (as opposed to in the contructor) so we
         * don't get spurious events before we are fully initialized
         */
        probe.addPropertyChangeListener(
            SelectorDisplayable.PROPERTY_POSITION,
            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (getActive() && getHaveInitialized()) {
                    lineProbeWasMoved();
                }
            }
        });
        probe.setPointSize(getDisplayScale());
        probe.setAutoSize(true);
        addDisplayable(probe, FLAG_COLOR);
        addDisplayable(gridLocs, FLAG_COLOR);

        dataNode = SoundingDataNode.getInstance(new Listener());

        dataNode.setData(getData(getDataInstance()));

        return true;
    }




    /**
     * Called by the parent class after all initialization has been done.
     */
    public void initDone() {
        try {
            // communicate the probe's initial position
            //Run this in a thread to get around possible deadlock issues.
            Misc.runInABit(10, new Runnable() {
                public void run() {
                    lineProbeWasMoved();
                }
            });
        } catch (Exception ex) {
            logException(ex);
        }
        super.initDone();
    }

    /**
     *  Remove this control. Call the parent  class doRemove and clears
     *  references to gridLocs, etc.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
        gridLocs = null;
        dataNode = null;
        probe    = null;
        widget   = null;
    }


    /**
     * Reset the position of the probe to the center.
     */
    private void resetProbePosition() {
        try {
            if (probe != null) {
                probe.setPosition(0.0, 0.0);
            }
        } catch (Exception exc) {
            logException("Resetting probe position", exc);
        }
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if (probe != null) {
            JMenuItem mi = new JMenuItem("Reset Probe Position");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resetProbePosition();
                }
            });
            items.add(mi);
        }
        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            super.timeChanged(time);
            if (time != null) dataNode.setTime(new DateTime(time));
            if (getProfilesVisibility()) {
                AnimationWidget aniWidget = this.getAnimationWidget();
                int idx = 0;
                if(aniWidget.getTimes() != null)  {
                    int             n         = aniWidget.getTimes().length;
                    for (int i = 0; i < n; i++) {
                        if ((new DateTime(time)).equals(
                                aniWidget.getTimeAtIndex(i))) {
                            idx = i;
                            continue;
                        }

                    }
                }
                aeroDisplay.setProfilesVisibility(true, idx);
                // display list update
                Set s   = getDataTimeSet();
                if (s != null) {
                    double[][] samples = s.getDoubles();
                    DateTime   dt      = new DateTime(samples[0][idx+1],
                            s.getSetUnits()[0]);
                    DateTime dt1 = new DateTime(samples[0][idx],
                            s.getSetUnits()[0]);
                    listlabel = dt1.dateString() + ":" + dt1.timeString()
                            + " and " + dt.dateString() + ":"
                            + dt.timeString();
                }

                updateDisplayList();
                updateHeaderLabel();
            }
        } catch (Exception ex) {
            logException("timeValueChanged", ex);
        }

    }

    /**
     * Override the base class method to return the relevant name
     * @return  get the label text for the spatial location label.
     */
    protected String getSpatialLociLabel() {
        return "Grid points";
    }



    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display.</p>
     *
     * @return                      The data-specific control-component.
     */
    Component getSpecificWidget() {
        return widget;
    }

    /**
     * Invoked if and when the position of the line probe changes.
     */
    private void lineProbeWasMoved() {
        try {
            probeMoved(getPosition());  // handle internal effects
            doShare(SHARE_POSITION, getPosition());  // tell others
        } catch (Exception e) {
            logException("lineProbeWasMoved", e);
        }
    }

    /**
     * Sets the horizontal position of the probe.  This method is used by the
     * persistance mechanism.
     *
     * @param p                     The horizontal position of the probe.
     * @throws VisADException       if <code>p.getType()</code> isn't {@link
     *                              visad.RealTupleType#SpatialCartesian2DTuple}
     *                              or if another VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setPosition(RealTuple p)
            throws VisADException, RemoteException {

        /*
         * The following fires a PropertyChangeEvent for the probe
         * position.
         */
        probe.setPosition(p);
    }

    /**
     * Returns the horizontal position of the probe or <code>null</code> if the
     * probe's position is indeterminate.
     *
     * @return                  The horizontal probe position or
     *                          <code>null </code>.  The {@link visad.MathType}
     *                          of a non-<code>null</code> returned object is
     *                          {@link
     *                          visad.RealTupleType#SpatialCartesian2DTuple}.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     */
    public RealTuple getPosition() throws VisADException, RemoteException {

        return ((probe != null)
                ? probe.getPosition()
                : null);
    }

    /**
     * Receive shared-data updates.
     *
     * @param from              The {@link ucar.unidata.collab.Sharable}
     *                          object from which this event originates.
     * @param dataId            The shared data identifier.
     * @param data              The shared data.
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {

        if ( !(dataId.equals(SHARE_POSITION))) {
            super.receiveShareData(from, dataId, data);  // pass it up
        } else {
            if (probe == null) {
                return;
            }

            try {

                /*
                 * The following fires a PropertyChangeEvent for the
                 * probe position.
                 */
                probe.setPosition((RealTuple) data[0]);
            } catch (Exception ex) {
                logException("receiveShareData:" + dataId, ex);
            }
        }
    }

    /**
     * Handles internal consequences of a change to the probe position.
     *
     * @param pos                       The probe's new position.
     */
    public void probeMoved(RealTuple pos) {
        if ( !getHaveInitialized()) {
            return;
        }

        try {
            LatLonPoint   llp    = null;
            RealTupleType rttype = (RealTupleType) pos.getType();

            if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)
                    || rttype.equals(RealTupleType.SpatialCartesian3DTuple)) {
                llp = latLon(pos);
            } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)
                       || rttype.equals(RealTupleType.SpatialEarth3DTuple)) {
                Real[] reals = pos.getRealComponents();

                llp = new LatLonTuple(reals[1], reals[0]);
            } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)
                       || rttype.equals(
                           RealTupleType.LatitudeLongitudeAltitude)) {
                Real[] reals = pos.getRealComponents();

                llp = new LatLonTuple(reals[0], reals[1]);
            } else {
                throw new IllegalArgumentException(
                    "Can't convert position to navigable point");
            }

            if (llp != null) {
                dataNode.setLocation(llp);  // invokes super.setLocation(llp)
            }
        } catch (Exception ex) {
            logException("probeMoved", ex);
        }
    }


    /**
     * Provides support for receiving output from a {@link SoundingDataNode}.
     *
     * @author Steven R. Emmerson
     * @version $Revision: 1.14 $ $Date: 2007/03/09 11:55:55 $
     */
    private class Listener implements SoundingDataNode.Listener {

        /**
         * Listener for data
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         *
         */
        private Listener() throws VisADException, RemoteException {}

        /**
         * <p>Sets the time-index of the current profiles.</p>
         *
         * @param index              The time-index of the current profiles.
         * @param source
         * @throws VisADException    if a VisAD failure occurs.
         * @throws RemoteException   if a Java RMI failure occurs.
         */
        public void setTimeIndex(int index, SoundingDataNode source)
                throws VisADException, RemoteException {
            setSounding(index);
        }

        /**
         * Sets the set of times of all the profiles.  The set will contain one
         * or more times as double values, in order, from earliest to latest.
         *
         * @param times              The times of all the profiles.
         * @param source
         * @throws VisADException    if a VisAD failure occurs.
         * @throws RemoteException   if a Java RMI failure occurs.
         */
        public void setTimes(SampledSet times, SoundingDataNode source)
                throws VisADException, RemoteException {

            RealType timeType =
                (RealType) ((SetType) times.getType()).getDomain()
                    .getComponent(0);

            // use a LineDrawing because it's the simplest DisplayableData
            if (timesHolder == null) {
                timesHolder = new LineDrawing("times ref");
            }

            /*
             * Add a data object to the display that has the right
             * time-centers.
             */
            Field dummy = new FieldImpl(new FunctionType(timeType,
                              AirTemperatureProfile.instance()), times);

            for (int i = 0, n = times.getLength(); i < n; i++) {
                dummy.setSample(
                    i, AirTemperatureProfile.instance().missingData());
            }

            timesHolder.setData(dummy);
            if(widget==null) {
                if (times.getLength() == 1) {
                    DateTime time = new DateTime(new Real(timeType,
                                        times.indexToDouble(new int[] {
                                            0 })[0][0], times.getSetUnits()[0]));

                    widget = GuiUtils.wrap(new JLabel(time.toString()));
                    dataNode.setTime(time);
                    setSounding(0);
                } else {
                    //
                    // Set the animation.
                     //
                    Animation animation = getInternalAnimation(timeType);
                    getSoundingView().setExternalAnimation(animation,
                            getAnimationWidget());
                    aeroDisplay.addDisplayable(animation);
                    aeroDisplay.addDisplayable(timesHolder);

                    Container container = Box.createHorizontalBox();
                    //Wrap these components so they don't get stretched in the Y direction
                    container.add(
                        GuiUtils.wrap(getAnimationWidget().getContents(false)));
                    //container.add(GuiUtils.wrap (animationWidget.getIndicatorComponent()));

                    widget = container;

                }
            }
        }

        /**
         * Sets the location of the current profiles.
         *
         * @param loc                The current location.
         * @param source
         *
         * @throws RemoteException
         * @throws VisADException
         */
        public void setLocation(LatLonPoint loc, SoundingDataNode source)
                throws VisADException, RemoteException {
            GriddedSoundingControl.this.setLocation(loc);
        }

        /**
         * <p>Sets the set of locations of the profiles.  The set will contain
         * one or more {@link visad.georef.EarthLocationTuple}-s.</p>
         *
         * <p>This implementation currently does nothing.</p>
         *
         * @param locs               The locations of the profiles.
         * @param source
         *
         * @throws RemoteException
         * @throws VisADException
         */
        public void setLocations(SampledSet locs, SoundingDataNode source)
                throws VisADException, RemoteException {
            gridLocs.setData(locs);
        }

        /**
         * Sets the set of profiles.
         *
         * @param tempPros        The temperature profiles.
         * @param dewPros         The dew-point profiles.
         * @param windPros        The wind profiles.
         * @param source          The source of the profiles.
         * @throws VisADException           if a VisAD failure occurs.
         * @throws RemoteException          if a Java RMI failure occurs.
         */
        public void setProfiles(Field[] tempPros, Field[] dewPros,
                                Field[] windPros, SoundingDataNode source)
                throws VisADException, RemoteException {
            setSoundings(tempPros, dewPros, windPros);
        }
    }

    /**
     * Collect the time animation set from the displayables.
     * If none found then return null.
     *
     * @return Animation set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Set getDataTimeSet() throws RemoteException, VisADException {
        Set aniSet = null;

        if (dataNode != null) {
            FieldImpl data = (FieldImpl) getData(getDataInstance());
            if (data != null) {
                aniSet = GridUtil.getTimeSet(data);
            }
        }
        return aniSet;
    }



    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_POSITION));
        labels.addAll(Misc.newList("Probe Position"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);

        if (getLocation() != null) {
            patterns.add(MACRO_POSITION);
            values.add(
                getDisplayConventions().formatLatLonPoint(getLocation()));
        }
    }


    /**
     * Update the location label, subclasses can override.
     */
    protected void updateHeaderLabel() {
        Data d = getDisplayListData();
        if (d == null) {
            super.updateHeaderLabel();
        }
        Text text = null;
        if (d instanceof FieldImpl) {
            int index = getCurrentIndex();
            if (index >= 0) {
                try {
                    text = (Text) ((FieldImpl) d).getSample(index, false);
                } catch (Exception ve) {}
            }
        } else {
            text = (Text) d;
        }
        if (text != null) {
            headerLabel.setText(text.toString());
        } else {
            super.updateHeaderLabel();
        }
    }
    /**
     * _more_
     *
     * @return _more_
     */
    protected Data getDisplayListData() {
        Data     data = null;
        Data     d    = super.getDisplayListData();
        TextType tt   = TextType.getTextType(DISPLAY_LIST_NAME);

        if (listlabel != null) {
            try {
                String label = "Skew-T ";
                data = new Text(tt, label + listlabel);
            } catch (Exception e) {}

            return data;
        } else {
            return d;
        }

    }
    public Set subSetProfilesTimeSet(Set tset) throws Exception {

        List<DateTime> timeList      = (List<DateTime>) Util.toList(tset);
        int size = timeList.size();
        List<DateTime> subList = new ArrayList<DateTime>();
        
        for(int i = 0; i< size-1; i++){
            Real r = timeList.get(i);
            DateTime dt = new DateTime(r);
            subList.add(dt);
        }
        

        return Util.makeTimeSet(subList);
    }


    /**
     * _more_
     *
     * @param visible _more_
     *
     * @throws Exception _more_
     */
    public void setPairProfilesVisibility(boolean visible) throws Exception {
        profilesVisibility = visible;
        Set s   = getDataTimeSet();
        int len = s.getLength();
        if (len < 2) {  // no pair do nothing
            return;
        }
        AnimationWidget aniWidget = this.getAnimationWidget();
        // aniWidget.setBoxPanelVisible( !visible);
       //if (visible) {
        aniWidget.gotoIndex(0);
        aniWidget.setRunning(false);
        //}

        if (visible) {

            Set    timeset = subSetProfilesTimeSet(s);
            dataNode.setOutputTimes((SampledSet) timeset);
        } else {
            Set timeset = getDataTimeSet();
            dataNode.setOutputTimes((SampledSet) timeset);
        }
        // GuiUtils.enableTree(aniWidget.getContents(), !visible);
        // now update the display list label
        aeroDisplay.setProfilesVisibility(visible, 0);

        if (visible) {
            if (s != null) {
                double[][] samples = s.getDoubles();
                DateTime   dt      = new DateTime(samples[0][1],
                        s.getSetUnits()[0]);
                DateTime dt1 = new DateTime(samples[0][0],
                        s.getSetUnits()[0]);
                listlabel = dt1.dateString() + ":" + dt1.timeString()
                        + " and " + dt.dateString() + ":"
                        + dt.timeString();
            }

        } else {
            listlabel = null;
        }

        updateDisplayList();
        updateHeaderLabel();
    }

    /** _more_ */
    String listlabel = null;

    /** flag for whether to display all profiles */
    private boolean profilesVisibility = false;

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getProfilesVisibility() {
        return profilesVisibility;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public void relocateDisplay(LatLonRect originalBounds,
                                LatLonRect newBounds, boolean useDataProjection) {
        //super.relocateDisplay(originalBounds, newBounds);
        // get the ratio of original probe point, init value to the center
        double             latRatio = 0.5;
        double             lonRatio = 0.5;
        EarthLocationTuple el       = null;
        //doUpdateRegion = false;

        double deltaLat = newBounds.getLatMax() - newBounds.getLatMin();
        double deltaLon = newBounds.getLonMax() - newBounds.getLonMin();

        //TODO: move the end points by the delta
        //It isn't just a matter of shifting by the delta as the bbox may have been resized and not just translated
        LatLonPointImpl lowerLeft = newBounds.getLowerLeftPoint();
        double          nlat = lowerLeft.getLatitude() + deltaLat * latRatio;
        double          nlon = lowerLeft.getLongitude() + deltaLon * lonRatio;
        double          nalt      = 0.0;
        if(originalBounds == null ){
            GeoSelection gs = dataSelection.getGeoSelection();
            GeoLocationInfo ginfo = new GeoLocationInfo(newBounds);
            gs.setBoundingBox(ginfo);
            try {
                updateDataSelection(dataSelection);
                getDataInstance().setDataSelection(dataSelection);
                getDataInstance().reInitialize();
                dataNode.setData(getData(getDataInstance()));
            } catch (Exception eee) {}
        }
        try {
            if (el != null) {
                nalt = el.getAltitude().getValue();
            }
            if(nlon < 0)
                nlon = GeoUtils.normalizeLongitude360(nlon);
            EarthLocation newel = makeEarthLocation(nlat, nlon, nalt);;
            double[]      ets   = earthToBox(newel);
            setPosition(new RealTuple(new Real[] {
                                      new Real(RealType.XAxis, ets[0]),
                                      new Real(RealType.YAxis, ets[1]) }));
            probeMoved(getPosition());
        } catch (Exception e) {
            System.out.print(e);
        }

    }


    public void viewpointChanged() {
        //System.out.println("viewpointChanged");
        if (getMatchDisplayRegion()) {
            if (reloadFromBounds) {
                try {
                    NavigatedDisplay navDisplay = getMapDisplay();
                    LatLonRect baseLLR =
                            dataSelection.getGeoSelection().getLatLonRect();
                    //LatLonRect newLLR = overrideGeoSelection.getLatLonRect();
                    getViewManager().setProjectionFromData(false);
                    Rectangle2D bbox = navDisplay.getLatLonBox();
                    LatLonPointImpl p1 = new LatLonPointImpl(bbox.getY(), bbox.getX());
                    LatLonRect newLLR = new LatLonRect(p1, bbox.getHeight(), bbox.getWidth());
                    //LatLonRect newLLR = navDisplay.getLatLonRect();

                    relocateDisplay(baseLLR, newLLR, false);

                    reloadFromBounds = false;
                } catch (Exception e) {}
            }
        }
    }

    protected boolean shouldAddControlListener() {
        return true;
    }

    protected boolean canDoProgressiveResolution() {
        return true;
    }

    public boolean hasMapProjection() {

        return true;

    }
}
