/*
 * $Id: ExampleViewManager.java,v 1.3 2005/05/13 18:28:19 jeffmc Exp $
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


package ucar.unidata.apps.example;


import ucar.unidata.collab.Sharable;

import ucar.unidata.idv.*;
import ucar.unidata.idv.ui.*;

import ucar.visad.display.*;


import ucar.unidata.view.geoloc.*;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.idv.ui.*;


import visad.*;
import visad.georef.EarthLocationTuple;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


import org.w3c.dom.Element;



/**
 * Class ExampleViewManager derives from the IDV's MapViewManager
 * to do some special example things. This gets created automagically
 * form the example skin.xml file that has a class=example.ExampleViewManager
 * attribute.
 */
public class ExampleViewManager extends MapViewManager {

    /** The identifier we send when the probe position changed */
    private final static String SHARE_PROBE = "probeposition";


    /** the probe */
    private PointProbe probe;


    /**
     * A flag so we don't try to share the probe position when we just rcvd
     * the probe position change event.
     */
    private boolean okToShareProbe = true;


    /** A list of JLabel-s, one for each display in the legend */
    List legendReadoutLabels = new ArrayList();

    /** This is the panel that holds the bottom legend */
    private JPanel legendPanel;


    /**
     * Create this view manager.
     *
     * @param idv The idv
     * @param descriptor The descriptor
     * @param properties Semi-colon delimited list of name=value properties
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public ExampleViewManager(IntegratedDataViewer idv, ViewDescriptor descriptor, String properties)
            throws VisADException, RemoteException {
        //Just pass thru the args to the base class ctor
        super(idv, descriptor, properties);
    }


    /**
     * Gets called by the base class to do initialization
     *
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void init() throws VisADException, RemoteException {
        super.init();

        //Create our own point probe and listen for events.
        probe = new PointProbe(0.0, 0.0, 0.0);
        probe.setAutoSize(true);
        probe.setVisible(true);
        probe.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(
                        SelectorDisplayable.PROPERTY_POSITION)) {
                    probePositionChanged();
                    if (okToShareProbe) {
                        //Tell others that we changed
                        doShare(SHARE_PROBE, ExampleViewManager.this);
                    }
                }
            }
        });

        //Now, add in the displayable
        getMaster().addDisplayable(probe);
    }


    /**
     * Respond to some event from the other view manager (i.e., the left
     * one or the right one).
     *
     * @param from Where this came from
     * @param dataId What changed
     * @param data The thing that changed
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        try {
            if (dataId.equals(SHARE_PROBE)) {
                ExampleViewManager that = (ExampleViewManager) data[0];
                okToShareProbe = false;
                if ( !Misc.equals(probe.getPosition(),
                                  that.probe.getPosition())) {
                    probe.setPosition(that.probe.getPosition());
                    probePositionChanged();
                }
                okToShareProbe = true;
                return;
            }
            //If it wasn't the probe then let the base class handle it
            super.receiveShareData(from, dataId, data);
        } catch (Exception exc) {
            logException("receiveShareData.position", exc);
        }
    }



    /**
     * Update the legend readout based on the probe position.
     */
    private void probePositionChanged() {

        try {
            List displayInfos = getDisplayInfos();
            if ((displayInfos == null) || (legendReadoutLabels == null)) {
                return;
            }
            RealTuple        position       = probe.getPosition();
            double[]         positionValues = position.getValues();
            NavigatedDisplay display        = (NavigatedDisplay) getMaster();
            //The probe position is in xyz space. We need to turn it into
            //lat/lon/alt space
            EarthLocationTuple elt =
                (EarthLocationTuple) display.getEarthLocation(new double[]{
                    positionValues[0],
                    positionValues[1],
                    positionValues[2] });
            LatLonPoint llp = elt.getLatLonPoint();


            //We normally would just call getControls, which gives us a list
            //of the DisplayControls. However, we actually want to look at
            //the DisplayInfo-s, which hold both the DisplayControl and
            //the Displayable because we want to look at the visad.Data 
            //that the Displayable has. A particular DisplayControl may show up
            //more than once in the list.

            Animation      anime              = getAnimation();
            int            step               = (anime != null)
                                                ? anime.getCurrent()
                                                : 0;
            Real           aniValue           = ((anime != null)
                                                 ? anime.getAniValue()
                                                 : null);


            Hashtable      seen               = new Hashtable();
            Hashtable      controlToReadout   = new Hashtable();
            DisplayControl lastDisplayControl = null;
            for (int i = 0; i < displayInfos.size(); i++) {
                FieldImpl field  = null;
                FieldImpl sample = null;
                Data      rt     = null;

                try {
                    DisplayInfo displayInfo =
                        (DisplayInfo) displayInfos.get(i);
                    Displayable displayable = displayInfo.getDisplayable();
                    DisplayControl displayControl =
                        displayInfo.getDisplayControl();
                    //Only do one from each control
                    if (seen.get(displayControl) != null) {
                        continue;
                    }

                    Data data = displayable.getData();

                    //Skip this if it is null.
                    if (data == null) {
                        continue;
                    }

                    //Only do FieldImpl-s
                    if ( !(data instanceof FieldImpl)) {
                        continue;
                    }

                    field = (FieldImpl) data;

                    //This sampling is broken  for now:
                    try {
                        sample = GridUtil.sample(field, llp);
                    } catch (Exception exc) {
                        //                    sample = GridUtil.sampleAtPoint(field, position,GridUtil.DEFAULT_SAMPLING_MODE);
                    }
                    if (sample == null) {
                        continue;
                    }


                    seen.put(displayControl, displayControl);
                    if (aniValue != null) {
                        rt = sample.evaluate(aniValue);
                    } else {
                        rt = sample.getSample(step);
                    }
                    if (rt.isMissing()) {
                        controlToReadout.put(displayControl, "missing");
                    } else {
                        Real real = (rt instanceof RealTuple)
                                    ? (Real) ((RealTuple) rt).getComponent(0)
                                    : (Real) rt;
                        controlToReadout.put(displayControl, "" + real);
                    }
                    field  = null;
                    sample = null;
                    rt     = null;

                } catch (Exception exc) {
                    System.err.println("Error sampling data:" + exc);
                    if (field != null) {
                        System.err.println("Field:" + field.getType());
                    }

                    if (sample != null) {
                        System.err.println("Sample:" + sample.getType());
                    }

                    if (rt != null) {
                        System.err.println("Data:" + rt.getType());
                    }
                    exc.printStackTrace();
                }

            }


            List allControls = getControls();
            if (allControls.size() != legendReadoutLabels.size()) {
                System.err.println(
                    "Humm, controls  size != legend readout size");
                return;
            }

            for (int i = legendReadoutLabels.size() - 1; i >= 0; i--) {
                DisplayControl displayControl =
                    (DisplayControl) allControls.get(i);
                String value = (String) controlToReadout.get(displayControl);
                if (value == null) {
                    value = "";
                }
                JLabel readout = (JLabel) legendReadoutLabels.get(i);
                readout.setText(value);
            }

        } catch (Exception exc) {
            logException("Setting readouts", exc);
        }


    }


    /**
     * Overwrite the doMakeContents to add in our own legend
     *
     *
     * @return The gui
     */
    protected Container doMakeContents() {
        Component parentContents = super.doMakeContents();
        legendPanel = new JPanel();
        legendPanel.setLayout(new BorderLayout());
        JScrollPane scroller = GuiUtils.makeScrollPane(legendPanel, 300, 50);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setPreferredSize(new Dimension(300, 50));
        return GuiUtils.centerBottom(parentContents, scroller);
    }



    /**
     * _more_
     */
    protected void fillLegends() {
        super.fillLegends();
        legendPanel.removeAll();

        String text        = "";
        List   controls    = getControls();
        List   legendComps = new ArrayList();
        legendReadoutLabels = new ArrayList();
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            Container controlLegend =
                control.getLegendComponent(control.BOTTOM_LEGEND);
            JLabel legendReadout = new JLabel("--");
            legendReadoutLabels.add(legendReadout);
            legendComps.add(GuiUtils.centerRight(controlLegend,
                                                 legendReadout));
        }
        JPanel innerLegend = GuiUtils.vbox(legendComps);
        legendPanel.add(innerLegend, BorderLayout.CENTER);

        probePositionChanged();
    }


    /**
     * Overwrite to not (maybe) not create a menu bar
     *
     * @return _more_
     */
    protected JMenuBar doMakeMenuBar() {
        return super.doMakeMenuBar();
        //        return null;
    }



    /**
     * Overwrite to not show default bottom legend
     *
     * @return _more_
     */
    public boolean getShowBottomLegend() {
        return false;
    }

    /**
     * Overwrite to not show default side legend
     *
     * @return _more_
     */
    public boolean getShowSideLegend() {
        return false;
    }




}
