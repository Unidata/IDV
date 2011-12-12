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


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;




import ucar.visad.display.Displayable;
import ucar.visad.display.ProfileLine;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.XYDisplay;


import visad.ActionImpl;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Data;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.Unit;
import visad.VisADException;

import visad.data.units.Parser;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Given a VisAD Field, make a 2D plot of the range data against
 * one of the 3 domain coordinates.
 *
 * @author IDV Development Team
 * @version $Revision: 1.97 $Date: 2007/01/30 12:27:42 $
 */
public class ProfileControl extends LineProbeControl {

    /** profile sharing property */
    public static final String SHARE_PROFILE = "ProfileControl.SHARE_PROFILE";


    /** profile display */
    private XYDisplay profileDisplay;

    /** local copy of the data */
    private FieldImpl myFieldImpl;


    /** realtypes for data, old data and vertical coordinate */
    private RealType parm, oldParmRT;

    /** set of point for the profile */
    private ProfileLine points;

    /** line for the profile */
    private ProfileLine line;

    /** label for the value */
    private JLabel valueLabel;

    /** flag for a sequence */
    private boolean isSequence = false;

    /** parameter label */
    private JLabel paramLabel;

    /** sampling mode */
    private int samplingMode;

    /**
     * Default constructor; set attribute flags
     */
    public ProfileControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL | FLAG_DISPLAYUNIT);
    }


    /**
     * Construct the vertical profile display and control buttons
     *
     * @param dataChoice   data description
     *
     * @return  true if successful
     *
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException  couldn't create a remote object needed
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        parm = RealType.getRealType("no_parameter_set");

        // Displayable to hold the points
        points = new ProfileLine("points");
        points.setPointSize(3.50f);
        points.setColor(java.awt.Color.white);

        // Displayable to hold the line
        line = new ProfileLine("line");
        line.setLineWidth(1.50f);
        addAttributedDisplayable(line, FLAG_COLOR);

        // DisplayMaster
        profileDisplay = new XYDisplay("vertprof", parm, RealType.Altitude);
        profileDisplay.setAspect(.7, 1.0);
        addDisplayMaster(profileDisplay);
        profileDisplay.showAxisScales(true);

        paramLabel = new JLabel("                ");

        if ( !setData(dataChoice)) {
            return false;
        }

        profileDisplay.setDisplayables(new Displayable[] { points, line,
                getInternalAnimation() });
        profileDisplay.draw();
        profileDisplay.getYAxisScale().setSnapToBox(true);

        doMakeProbe();

        samplingMode = (getDefaultSamplingMode().equals(WEIGHTED_AVERAGE))
                       ? Data.WEIGHTED_AVERAGE
                       : Data.NEAREST_NEIGHBOR;

        return true;
    }

    /**
     * Called after init().  Load profile into display.
     */
    public void initDone() {
        super.initDone();
        try {
            setViewParameters();
            loadProfile(getPosition());
        } catch (Exception exc) {
            logException("initDone", exc);
        }
    }






    /**
     * Get the specialized control widgets for this control
     *
     * @param controlWidgets  list of widgets to add to and return
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Parameter:"),
                                             paramLabel));
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Animation:"),
                GuiUtils.left(getAnimationWidget().getContents(false))));
        super.getControlWidgets(controlWidgets);
    }

    /**
     * Make the UI contents for this control.
     *
     * @return  container for UI
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        valueLabel = new JLabel(" ", JLabel.LEFT);
        JComponent cmp = (JComponent) profileDisplay.getComponent();
        cmp.setPreferredSize(new Dimension(350, 400));
        return GuiUtils.topCenterBottom(valueLabel, cmp,
                                        doMakeWidgetComponent());
    }




    /**
     * Set the data for this control based on the data choice.
     *
     * @param dataChoice   choice describing the data to load.
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws RemoteException, VisADException {
        if ( !super.setData(dataChoice)) {
            return false;
        }
        if (paramLabel != null) {
            paramLabel.setText(dataChoice.toString());
        }

        myFieldImpl = getGridDataInstance().getGrid();
        isSequence  = GridUtil.isTimeSequence(myFieldImpl);
        parm        = getGridDataInstance().getRealType(0);
        setViewParameters();
        loadProfile(getPosition());
        return true;
    }


    /**
     * Make a 2D display of the range values against domain coordinate # NN.
     *
     * @param fi a VisAD FlatField or seqence of FlatFields with 3 or more
     *           domain coordinates, manifold dimension 1.
     * @param NN an integer, the index number of the coordinate to use
     *               as profile or y axis of plot (0,1,2,...)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void displayProfileForCoord(FieldImpl fi, int NN)
            throws VisADException, RemoteException {

        FlatField    ffld = (isSequence == true)
                            ? (FlatField) fi.getSample(0)
                            : (FlatField) fi;
        FunctionType ft   = (FunctionType) ffld.getType();

        parm = (RealType) ((RealTupleType) ft.getRange()).getComponent(0);

        RealType      height = (RealType) ft.getDomain().getComponent(NN);
        RealTupleType domainType    = new RealTupleType(RealType.Altitude);
        FunctionType  pType         = new FunctionType(domainType, parm);
        FunctionType  profileType   = null;
        FieldImpl     profile       = null;
        Gridded1DSet  profileDomain = null;
        int           numIters      = 1;
        if (isSequence) {
            SampledSet timeDomain = (SampledSet) fi.getDomainSet();
            numIters = timeDomain.getLength();
            MathType tMT = ((SetType) timeDomain.getType()).getDomain();
            profileType = new FunctionType(tMT, pType);
            profile     = new FieldImpl(profileType, timeDomain);
        } else {
            profileType = pType;
        }

        for (int i = 0; i < numIters; i++) {
            SampledSet ss = (SampledSet) GridUtil.getSpatialDomain(fi, i);
            if ((profileDomain == null)
                    || !GridUtil.isConstantSpatialDomain(fi)) {
                float[][] domainVals = ss.getSamples();
                if ( !height.equals(RealType.Altitude)) {
                    domainVals =
                        ss.getCoordinateSystem().toReference(domainVals,
                            ss.getSetUnits());
                }
                try {  // domain might have NaN's in it
                    profileDomain = new Gridded1DSet(domainType,
                            new float[][] {
                        domainVals[NN]
                    }, domainVals[0].length, (CoordinateSystem) null,
                       (Unit[]) null, (ErrorEstimate[]) null, false);
                } catch (Exception e) {
                    break;
                }
            }
            FlatField ff = new FlatField(pType, profileDomain);
            if (isSequence) {
                ff.setSamples(((FlatField) fi.getSample(i)).getFloats(false));
                profile.setSample(i, ff);
            } else {
                ff.setSamples(((FlatField) fi).getFloats(false));
                profile = ff;
            }
        }

        // Set the FlatFields in the ProfileLines. 
        points.setData(fi);
        if (profile != null) {
            line.setData(profile);
        }
        // rescale display so data fits inside the display
        profileDisplay.reScale();

    }  // end method displayProfileForCoord



    /**
     * Called if the display unit changes.  Set the scale on the
     * axis.
     *
     * @param oldUnit   old display unit
     * @param newUnit   new display unit
     */
    protected void displayUnitChanged(Unit oldUnit, Unit newUnit) {
        super.displayUnitChanged(oldUnit, newUnit);
        //      if (true) return;

        Range r = getGridDataInstance().getRange(0);
        oldUnit = getGridDataInstance().getRawUnit(0);
        if (newUnit != null) {
            try {
                r = new Range(newUnit.toThis(r.getMin(), oldUnit),
                              newUnit.toThis(r.getMax(), oldUnit));
            } catch (Exception exc) {
                logException("Setting units", exc);
                return;
            }
            profileDisplay.setXDisplayUnit(newUnit);
        }
        profileDisplay.setXRange(r.getMin(), r.getMax());
    }


    /**
     * This gets called by the base class LineProbeControl class
     * when the probe positon has changed (either through user interaction
     * or through the sharing framework.
     *
     * @param position   new position
     */
    protected void probePositionChanged(RealTuple position) {
        try {
            loadProfile(position);
        } catch (Exception exc) {
            logException("probePositionChanged", exc);
        }

    }

    /**
     * Given the location of the profile SelectorPoint,
     * and a FieldImpl for one or more times for animation,
     * create a data set for a profile at the profile's SP location.
     * Create a vertical line showing where profile is in the data.
     *
     * @param position   new position for profile
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadProfile(RealTuple position)
            throws VisADException, RemoteException {

        if ( !getHaveInitialized()) {
            return;
        }

        if ((myFieldImpl == null) || (position == null)) {
            return;
        }
        LatLonPoint   llp    = null;
        RealTupleType rttype = (RealTupleType) position.getType();
        if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)) {
            // get earth location of the x,y position in the VisAD display
            double[] values = position.getValues();
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] { values[0],
                    values[1], 1.0 });
            llp = elt.getLatLonPoint();
        } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[1], reals[0]);
        } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[0], reals[1]);
        } else {
            throw new VisADException(
                "Can't convert position to navigable point");
        }

        FieldImpl newFI = GridUtil.getProfileAtLatLonPoint(myFieldImpl, llp,
                              getDefaultSamplingModeValue());

        if (newFI != null) {
            displayProfileForCoord(newFI, 2);
        }

        // set location label, if available.
        if (valueLabel != null) {
            valueLabel.setText(
                getDisplayConventions().formatLatLonPoint(llp));
        }

    }

    /**
     * Set the parameters for the view.  Mostly deals with the
     * vertical scale.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setViewParameters() throws VisADException, RemoteException {
        if (parm != null) {
            profileDisplay.setXAxisType(parm);
            profileDisplay.getXAxisScale().setSnapToBox(true);
            if (paramLabel != null) {
                profileDisplay.getXAxisScale().setTitle(paramLabel.getText());
            }
            //TODO: use the right index
            Unit units = getDisplayUnit();
            displayUnitChanged(units, getDisplayUnit(units));
        }
    }



}
