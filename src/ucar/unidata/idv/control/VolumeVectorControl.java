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


import ucar.unidata.data.*;
import ucar.unidata.data.gis.MapMaker;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.idv.control.drawing.GlyphCreatorCommand;
import ucar.unidata.idv.control.drawing.SymbolGlyph;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.MapProjectionDisplay;

import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;
import ucar.visad.display.ZSelector;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * Created by yuanho on 4/5/15.
 */
public class VolumeVectorControl extends GridDisplayControl implements FlowDisplayControl {

    /**
     * the display for the volume renderer
     */
    FlowDisplayable myDisplay;

    /** data choice for the data */
    protected DataChoice datachoice;

    /** a component to change the barb size */
    ValueSliderWidget barbSizeWidget;

    /** vector/barb size component */
    JComponent sizeComponent;

    /** a component to change the traj size */
    ValueSliderWidget trajLengthWidget;

    /** vector/traj length component */
    JComponent trajLengthComponent;

    /** _more_ */
    JComponent trajSkipComponent;

    /** _more_ */
    JComponent trajSkipComponentE;

    /** _more_ */
    JComponent streamLSkipComponent;

    /** _more_ */
    JComponent streamLSkipComponentE;

    /** a component to change the traj size */
    ValueSliderWidget streamLLengthWidget;

    /** vector/traj length component */
    JComponent streamLLengthComponent;

    /** _more_ */
    JComponent smoothComponent;

    /** _more_ */
    ValueSliderWidget smoothWidget;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** _more_ */
    ValueSliderWidget skipFactorWidgetZ;

    /** vector/barb button */
    private JRadioButton vectorBtn;

    /** trajectory button */
    private JRadioButton trajectoryBtn;

    /** trajectory button */
    private JRadioButton streamlineBtn;

    /** _more_ */
    boolean isStreamLine = false;

    /** _more_ */
    boolean isVectors = true;

    /** _more_ */
    boolean isTrajectories = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = true;

    /** autoscale */
    boolean autoSize = false;

    /** arrow */
    boolean arrowHead = false;

    /** arrow */
    boolean arrowHeadL = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 4.0f;

    /** a traj offset value */
    float arrowHeadSizeValue = 1.0f;

    /** a traj offset value */
    float trajOffsetValue = 4.0f;

    /** a traj offset value */
    float streamLOffsetValue = 4.0f;

    /** slider components */
    private JComponent[] widthSliderComps;

    /** Range for flow scale */
    private Range flowRange;

    /** the range dialog */
    RangeDialog rangeDialog;

    /** _more_ */
    private boolean useSpeedForColor = false;

    /** _more_ */
    private boolean coloredByAnother = false;

    /** _more_ */
    private int colorIndex = -1;

    /** labels for trajectory form */
    private final static String[] trajFormLabels = new String[] { "Line",
                                                                  "Ribbon",
                                                                  "Cylinder",
                                                                  "Deform Ribbon",
                                                                  "Point" };

    /** types of smoothing functions */
    private final static int[] trajForm = new int[] { 0, 1, 2, 3, 4 };

    /** labels for trajectory form */
    private final static String[] streamLFormLabels = new String[] { "Line",
                                                                     "Ribbon",
                                                                     "Cylinder" };

    /** types of smoothing functions */
    private final static int[] streamLForm = new int[] { 0, 1, 2 };

    /** vector/traj length component */
    JComponent trajFormComponent;

    /** default type */
    private Integer trajFormType = Integer.valueOf(0);

    /** start level */
    private int trajStartLevel = 0;

    /** _more_ */
    private int trajEndLevel = 0;

    /** start level */
    private int trajSkipLevels = 0;

    /** vector/traj length component */
    JComponent streamLFormComponent;

    /** default type */
    private Integer streamLFormType = Integer.valueOf(0);

    /** start level */
    private int streamLStartLevel = 0;

    /** end level */
    private int streamLEndLevel = 0;

    /** _more_ */
    private Range flowColorRange;

    /** _more_ */
    private int skipValueZ = 0;

    /** _more_ */
    JCheckBox arrowCbx ;

    /** _more_ */
    JCheckBox arrowCbxL;

    /** _more_ */
    int smoothFactor = 10;

    /** _more_ */
    boolean fromBundle = false;

    /** a component to change the vector arrow head size */
    ValueSliderWidget vectorAHSizeWidget;

    /** _more_ */
    boolean isLine = true;

    /**
     * Default constructor; does nothing.
     */
    public VolumeVectorControl() {
        //setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
        setAttributeFlags(FLAG_LINEWIDTH | FLAG_COLOR);
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return true if successful
     *
     * @throws java.rmi.RemoteException  Java RMI error
     * @throws visad.VisADException   VisAD Error
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        datachoice = dataChoice;
        if ( !isDisplay3D()) {
            LogUtil.userMessage(log_, "Can't render volume in 2D display");
            return false;
        }

        // checeking grid size matching between u and w
        if (dataChoice instanceof DerivedDataChoice) {
            DerivedDataChoice ddc      = (DerivedDataChoice) dataChoice;
            List              choices0 = ddc.getChoices();
            if (choices0.size() == 3) {
                DirectDataChoice udc = (DirectDataChoice) choices0.get(0);
                DirectDataChoice vdc = (DirectDataChoice) choices0.get(1);

                DataChoice       wdc           = (DataChoice) choices0.get(2);
                List             usTime        = udc.getAllDateTimes();
                List             wsTime        = wdc.getAllDateTimes();
                List             selectedTimes =
                    getDataSelection().getTimes();
                if (isStreamLine || isTrajectories) {
                    if ((selectedTimes != null)
                            && (selectedTimes.size() < 4)) {
                        userErrorMessage(
                            "Minumum selected times need to be 4 for trajectory calculation");
                        return false;
                    } else if ((usTime != null) && (usTime.size() < 4)) {
                        userErrorMessage(
                            "Minumum selected times need to be 4 for trajectory calculation");
                        return false;
                    }
                }
                /* if(selectedTimes != null){
                   int len = selectedTimes.size();
                   if(usTime.get((Integer) selectedTimes.get(0)) != wsTime.get((Integer)selectedTimes.get(0)) ||
                          usTime.get((Integer)selectedTimes.get(len-1)) != wsTime.get((Integer)selectedTimes.get(len-1)) )
                   {
                       userErrorMessage("w grid selected times are different from u grid " );
                       return false;
                   }
               } else */
                if (wdc.getSelectedDateTimes() != null) {
                    selectedTimes = wdc.getSelectedDateTimes();
                    int len = selectedTimes.size();
                    if ((usTime.get(0) != wsTime.get(0))
                            || (usTime.get(len - 1) != wsTime.get(len - 1))) {
                        userErrorMessage(
                            "w grid selected times are different from u grid ");
                        return false;
                    }
                }
            }
        }

        levelBox = doMakeLevelControl(null);
        levelBox.addActionListener(new ActionListener() {
                                       public void actionPerformed(
                                       ActionEvent e) {
                                           if (getOkToFireEvents()) {
                                               TwoFacedObject select =
                                                   (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                               int selectIdx =
                                                   ((JComboBox) e.getSource()).getSelectedIndex();

                                               if ((select != null)
                                                       && isTrajectories) {
                                                   int ct =
                                                       ((JComboBox) e.getSource()).getItemCount();
                                                   if (
                                                   select.toString().equals(
                                                       "All Levels")) {
                                                       setTrajStartLevel(
                                                       select, 0);
                                                   } else {
                                                       setTrajStartLevel(
                                                       select, selectIdx);
                                                   }
                                               }
                                           }
                                       }
                                   });

        levelBoxEnd = doMakeLevelControl(null);
        levelBoxEnd.addActionListener(new ActionListener() {
                                          public void actionPerformed(
                                          ActionEvent e) {
                                              if (getOkToFireEvents()) {
                                                  TwoFacedObject select =
                                                      (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                                  int selectIdx =
                                                      ((JComboBox) e.getSource()).getSelectedIndex();

                                                  if ((select != null)
                                                          && isTrajectories) {
                                                      setTrajEndLevel(select,
                                                      selectIdx);
                                                  }
                                              }
                                          }
                                      });

        slevelBox = doMakeLevelControl(null);
        slevelBox.addActionListener(new ActionListener() {
                                        public void actionPerformed(
                                        ActionEvent e) {
                                            if (getOkToFireEvents()) {
                                                TwoFacedObject select =
                                                    (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                                int selectIdx =
                                                    ((JComboBox) e.getSource()).getSelectedIndex();

                                                if ((select != null)
                                                        && isStreamLine) {
                                                    int ct =
                                                        ((JComboBox) e.getSource()).getItemCount();
                                                    if (
                                                    select.toString().equals(
                                                        "All Levels")) {
                                                        setStreamLStartLevel(
                                                        select, 0);
                                                    } else {
                                                        setStreamLStartLevel(
                                                        select, selectIdx);
                                                    }
                                                }
                                            }
                                        }
                                    });

        slevelBoxEnd = doMakeLevelControl(null);
        slevelBoxEnd.addActionListener(new ActionListener() {
                                           public void actionPerformed(
                                           ActionEvent e) {
                                               if (getOkToFireEvents()) {
                                                   TwoFacedObject select =
                                                       (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                                   int selectIdx =
                                                       ((JComboBox) e.getSource()).getSelectedIndex();

                                                   if ((select != null)
                                                           && isStreamLine) {
                                                       setStreamLEndLevel(
                                                       select, selectIdx);
                                                   }
                                               }
                                           }
                                       });

        myDisplay = (FlowDisplayable) createPlanDisplay();

        myDisplay.setPointSize(getPointSize());
        addDisplayable(myDisplay, getAttributeFlags());

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }


        //Now set up the flags and add the displayable
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        FlowDisplayable planDisplay;
        if (isWindBarbs) {
            planDisplay =
                new WindBarbDisplayable("FlowPlanViewControl_windbarbs_"
                                        + ((datachoice != null)
                                           ? datachoice.toString()
                                           : ""), null);
        } else {
            planDisplay = new FlowDisplayable("FlowPlanViewControl_vectors_"
                    + ((datachoice != null)
                       ? datachoice.toString()
                       : ""), null);

            planDisplay.set3DFlow(true);
        }
        planDisplay.setAutoScale(autoSize);
        planDisplay.setUseSpeedForColor(useSpeedForColor);
        if (isStreamLine) {
            //since streamline is part of trajectory
            planDisplay.setStreamline(isStreamLine);
            planDisplay.setIsTrajectories(true);
            planDisplay.setTrojectoriesEnabled(true, arrowHeadSizeValue,
                    false);
        } else {
            planDisplay.setStreamline(isStreamLine);
            planDisplay.setTrojectoriesEnabled(isTrajectories,
                    arrowHeadSizeValue, false);
        }

        planDisplay.setTrojectoriesEnabled(isTrajectories,
                                           arrowHeadSizeValue, false);

        if (useSpeedForColor || coloredByAnother) {
            addAttributedDisplayable(planDisplay, FLAG_COLORTABLE);
        } else {
            addAttributedDisplayable(planDisplay);
        }
        return planDisplay;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    FlowDisplayable getGridDisplay() {
        return myDisplay;
    }

    /**
     * Add in any special control widgets to the current list of widgets.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        //super.getControlWidgets(controlWidgets);

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());

        skipFactorWidgetZ = new ValueSliderWidget(this, 0, 10, "skipValueZ",
                getSkipWidgetLabel());


        addRemovable(skipFactorWidget);
        addRemovable(skipFactorWidgetZ);

        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                "Size");
        addRemovable(barbSizeWidget);

        JCheckBox autoSizeCbx = new JCheckBox("Autosize", autoSize);
        arrowCbx  = new JCheckBox("Arrow", arrowHead);
        arrowCbxL = new JCheckBox("Arrow", arrowHeadL);
        autoSizeCbx.addActionListener(new ActionListener() {
                                          public void actionPerformed(
                                          ActionEvent e) {
                                              autoSize =
                                              ((JCheckBox) e.getSource()).isSelected();
                                              getGridDisplay().setAutoScale(
                                              autoSize);
                                          }
                                      });

        arrowCbx.addActionListener(new ActionListener() {
                                       public void actionPerformed(
                                       ActionEvent e) {
                                           arrowHead =
                                           ((JCheckBox) e.getSource()).isSelected();
                                           if (arrowHead) {
                                               getGridDisplay().setArrowHead(
                                               arrowHead);
                                           } else {
                                               getGridDisplay().setArrowHead(
                                               arrowHead);
                                           }
                                           if ( !fromBundle) {
                                               getGridDisplay().resetTrojectories();
                                           }
                                       }
                                   });

        arrowCbxL.addActionListener(new ActionListener() {
                                        public void actionPerformed(
                                        ActionEvent e) {
                                            arrowHeadL =
                                            ((JCheckBox) e.getSource()).isSelected();
                                            if (arrowHeadL) {
                                                getGridDisplay().setArrowHead(
                                                arrowHeadL);
                                            } else {
                                                getGridDisplay().setArrowHead(
                                                arrowHeadL);
                                            }
                                            if ( !fromBundle) {
                                                getGridDisplay().resetTrojectories();
                                            }
                                        }
                                    });
        sizeComponent = GuiUtils.hbox(GuiUtils.rLabel("Size: "),
                                      barbSizeWidget.getContents(false),
                                      autoSizeCbx);
        if (getIsThreeComponents()) {

            vectorBtn = new JRadioButton((isWindBarbs
                                          ? "Wind Barbs:"
                                          : "Vectors:"), isVectors);
            trajLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "trajOffset", "LengthOffset");

            streamLLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "streamLOffset", "LengthOffset");

            smoothWidget = new ValueSliderWidget(this, 11, 31,
                    "smoothFactor", "smoothFactor");

            List<TwoFacedObject> trajFormList =
                TwoFacedObject.createList(trajForm, trajFormLabels);
            List<TwoFacedObject> streamLFormList =
                TwoFacedObject.createList(streamLForm, streamLFormLabels);
            JComboBox trajFormBox    = new JComboBox();
            JComboBox streamLFormBox = new JComboBox();
            GuiUtils.setListData(trajFormBox, trajFormList);
            GuiUtils.setListData(streamLFormBox, streamLFormList);
            trajFormBox.setSelectedItem(
                TwoFacedObject.findId(getTrajFormType(),
                                      trajFormList));
            trajFormBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            TwoFacedObject select =
                                (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                            setTrajFormType(select.getId().hashCode());
                            if (select.getLabel() == "Line") {
                                isLine = true;
                                arrowCbx.setSelected(arrowHead);
                            } else {
                                arrowCbx.setSelected(false);
                                isLine = false;
                            }
                            ;
                            enableArrowCompnoentBox();
                        }
                    });

            enableArrowCompnoentBox();
            streamLFormBox.setSelectedItem(
                TwoFacedObject.findId(getStreamLFormType(),
                                      streamLFormList));
            streamLFormBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            TwoFacedObject select =
                                (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                            setStreamLFormType(select.getId().hashCode());
                        }
                    });
            trajFormComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Trajectory Form: "),
                              GuiUtils.filler(), trajFormBox,
                              GuiUtils.filler());

            trajLengthComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Length Offset: "),
                              trajLengthWidget.getContents(false), arrowCbx);

            trajSkipComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Trajectory Start Level: "),
                              GuiUtils.filler(), levelBox);

            trajSkipComponentE =
                GuiUtils.hbox(GuiUtils.rLabel("Trajectory End Level: "),
                              GuiUtils.filler(), levelBoxEnd);

            trajectoryBtn = new JRadioButton("Trajectories:", isTrajectories);

            streamLFormComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Streamline Form: "),
                              GuiUtils.filler(), streamLFormBox,
                              GuiUtils.filler());

            streamLLengthComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Length Offset: "),
                              streamLLengthWidget.getContents(false),
                              arrowCbxL);

            smoothComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Smooth Factor: "),
                              smoothWidget.getContents(false));

            streamLSkipComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Streamline Start Level: "),
                              GuiUtils.filler(), slevelBox);
            streamLSkipComponentE =
                GuiUtils.hbox(GuiUtils.rLabel("Streamline End Level: "),
                              GuiUtils.filler(), slevelBoxEnd);

            streamlineBtn = new JRadioButton("Streamlines:", isStreamLine);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButton source = (JRadioButton) e.getSource();

                    if (source == trajectoryBtn) {
                        isTrajectories = true;
                        isVectors      = false;
                        isStreamLine   = false;
                        if ( !fromBundle) {
                            setStreamlines();
                            if(getTrajEndLevel() == 0) {
                                Object ob = levelBoxEnd.getSelectedItem();
                                levelBoxEnd.setSelectedItem(ob);
                            }
                            else
                                levelBoxEnd.setSelectedIndex(getTrajEndLevel());
                        }
                    } else if (source == streamlineBtn) {
                        isStreamLine   = true;
                        isTrajectories = false;
                        isVectors      = false;
                        if ( !fromBundle) {
                            setStreamlines();
                            if(getTrajEndLevel() == 0) {
                                Object ob = slevelBoxEnd.getSelectedItem();
                                slevelBoxEnd.setSelectedItem(ob);
                            }
                            else
                                slevelBoxEnd.setSelectedIndex(
                                    getStreamLEndLevel());
                        }
                    } else {
                        isVectors      = true;
                        isTrajectories = false;
                        isStreamLine   = false;
                        if ( !fromBundle) {
                            setStreamlines();
                        }
                        // levelBox.setSelectedIndex(levelBox.getItemCount()
                        //         - 1);
                    }
                }
            };




            vectorBtn.addActionListener(listener);
            trajectoryBtn.addActionListener(listener);
            streamlineBtn.addActionListener(listener);
            GuiUtils.buttonGroup(vectorBtn, trajectoryBtn, streamlineBtn);


            Insets spacer = new Insets(0, 30, 0, 0);
            JComponent rightComp =
                GuiUtils.vbox(GuiUtils.left(GuiUtils.vbox(vectorBtn,
                                                          GuiUtils.inset(
                                                              sizeComponent,
                                                                      spacer))), GuiUtils.left(
                                                                          GuiUtils.vbox(
                                                                              trajectoryBtn,
                                                                                      GuiUtils.vbox(
                                                                                          GuiUtils.inset(
                                                                                              trajFormComponent,
                                                                                                      spacer),
                                                                                                  GuiUtils.inset(
                                                                                                      trajLengthComponent,
                                                                                                              spacer),
                                                                                                  GuiUtils.inset(
                                                                                                      trajSkipComponent,
                                                                                                              spacer),
                                                                                                  GuiUtils.inset(
                                                                                                      trajSkipComponentE,
                                                                                                              spacer)))), GuiUtils.left(
                                                                                                                  GuiUtils.vbox(
                                                                                                                      streamlineBtn,
                                                                                                                              GuiUtils.vbox(
                                                                                                                                  GuiUtils.inset(
                                                                                                                                      streamLFormComponent,
                                                                                                                                              spacer),
                                                                                                                                          GuiUtils.inset(
                                                                                                                                              streamLLengthComponent,
                                                                                                                                                      spacer),
                                                                                                                                          GuiUtils.inset(
                                                                                                                                              smoothComponent,
                                                                                                                                                      spacer),
                                                                                                                                          GuiUtils.inset(
                                                                                                                                              streamLSkipComponent,
                                                                                                                                                      spacer),
                                                                                                                                          GuiUtils.inset(
                                                                                                                                              streamLSkipComponentE,
                                                                                                                                                      spacer)))));
            JLabel showLabel = GuiUtils.rLabel("Show:");
            showLabel.setVerticalTextPosition(JLabel.TOP);
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.top(GuiUtils.inset(showLabel,
                            new Insets(10, 0, 0, 0))),
                    GuiUtils.left(GuiUtils.top(rightComp))));


        }

        /*    controlWidgets.add(
                    new WrapperWidget(
                            this, GuiUtils.rLabel("Start Level:"),
                            GuiUtils.left(levelBox)));  */
        vectorAHSizeWidget = new ValueSliderWidget(this, 0, 40,
                "ArrowHeadSize", "Arrow Head Size", 10.0f);

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Arrow Scale: "),
                                             vectorAHSizeWidget.getContents(
                                             false)));

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("XY Skip:"),
                                             GuiUtils.left(
                                             skipFactorWidget.getContents(
                                                 false))));
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Z Skip:"),
                                             GuiUtils.left(
                                             skipFactorWidgetZ.getContents(
                                                 false))));

        enableTrajLengthBox();
        enableStreamLLengthBox();
        enableVectorBox();

        List timeL = getDataSelection().getTimes();
        if ((timeL == null) && getHadDataChoices()) {
            List dchoices = getMyDataChoices();
            timeL = ((DataChoice) dchoices.get(0)).getSelectedDateTimes();
            if ((timeL != null) && (timeL.size() == 0)) {
                timeL = ((DataChoice) dchoices.get(0)).getAllDateTimes();
            }
        }
        if ((timeL != null) && (timeL.size() < 4)) {
            GuiUtils.enableTree(trajectoryBtn, false);
        }
        if ((timeL != null) && (timeL.size() < 4)) {
            GuiUtils.enableTree(streamlineBtn, false);
        }

        super.getControlWidgets(controlWidgets);


    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSmoothFactor() {
        return smoothFactor;
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void setSmoothFactor(int f) {
        smoothFactor = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setSmoothFactor(f);
                getGridDisplay().setStreamline(true);
                getGridDisplay().resetTrojectories();
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (smoothWidget != null) {
            smoothWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsThreeComponents() {
        return isThreeComponents;
    }

    /**
     * _more_
     */
    public void setStreamlines() {
        //isStreamlines = v;
        if (getGridDisplay() != null) {
            int    ct = levelBox.getItemCount();
            String tt = levelBox.getSelectedItem().toString();
            if (tt.equals("All Levels")) {
                getGridDisplay().setZskip(0);
            } else {
                getGridDisplay().setZskip(ct - 1);
            }


            //getGridDisplay().setTrajStartLevel(trajStartLevel);
            getGridDisplay().setIsTrajectories(isTrajectories);
            getGridDisplay().setStreamline(isStreamLine);
            if (isStreamLine) {
                //since streamline is part of trajectory
                getGridDisplay().setTrajFormType(getStreamLFormType());
                getGridDisplay().setIsTrajectories(true);
                getGridDisplay().setTrajStartLevel(streamLStartLevel);
                getGridDisplay().setTrajOffset(getStreamLOffset());
                if (arrowHeadL) {
                    getGridDisplay().setArrowHead(true);
                    arrowCbxL.setSelected(arrowHeadL);
                }
                getGridDisplay().setTrojectoriesEnabled(true, arrowHeadL,
                        arrowHeadSizeValue, false);
            } else {
                getGridDisplay().setTrajFormType(getTrajFormType());
                getGridDisplay().setTrajStartLevel(trajStartLevel);
                getGridDisplay().setTrajOffset(getTrajOffset());
                if (arrowHead) {
                    getGridDisplay().setArrowHead(true);
                    arrowCbx.setSelected(arrowHead);
                }
                getGridDisplay().setTrojectoriesEnabled(isTrajectories,
                        arrowHead, arrowHeadSizeValue, false);
            }
            enableTrajLengthBox();
            enableStreamLLengthBox();
            enableVectorBox();
        }
        if (trajectoryBtn != null) {
            vectorBtn.setSelected(isVectors);
            trajectoryBtn.setSelected(isTrajectories);
        }

    }

    /**
     * _more_
     *
     * @param
     */
    private void enableArrowCompnoentBox() {
        //GuiUtils.enableTree(vectorAHSizeBox, isLine);
        GuiUtils.enableTree(arrowCbx, isLine);
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void setArrowHeadSize(float f) {
        arrowHeadSizeValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(getTrajOffset());
                getGridDisplay().setArrowHeadSize(arrowHeadSizeValue);
                if (isTrajectories) {
                    getGridDisplay().resetTrojectories();
                }
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (vectorAHSizeWidget != null) {
            vectorAHSizeWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     *
     * @return _more_
     */
    public float getArrowHeadSize() {
        return arrowHeadSizeValue;
    }

    /**
     * enable the barb   box
     */
    private void enableTrajLengthBox() {
        if (trajLengthComponent != null) {
            GuiUtils.enableTree(trajLengthComponent, isTrajectories);
            GuiUtils.enableTree(trajFormComponent, isTrajectories);
            GuiUtils.enableTree(trajSkipComponent, isTrajectories);
            GuiUtils.enableTree(trajSkipComponentE, isTrajectories);
            GuiUtils.enableTree(levelBox, isTrajectories);
            GuiUtils.enableTree(levelBoxEnd, isTrajectories);
        }
    }

    /**
     * enable the streamline   box
     */
    private void enableStreamLLengthBox() {
        if (streamLLengthComponent != null) {
            GuiUtils.enableTree(streamLLengthComponent, isStreamLine);
            GuiUtils.enableTree(streamLFormComponent, isStreamLine);
            GuiUtils.enableTree(streamLSkipComponent, isStreamLine);
            GuiUtils.enableTree(streamLSkipComponentE, isStreamLine);
            GuiUtils.enableTree(smoothComponent, isStreamLine);
            GuiUtils.enableTree(slevelBox, isStreamLine);
            GuiUtils.enableTree(slevelBoxEnd, isStreamLine);
        }
    }

    /**
     * enable the vector   box
     */
    private void enableVectorBox() {
        if (sizeComponent != null) {
            GuiUtils.enableTree(sizeComponent, !isTrajectories);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (myDisplay != null) {
            try {
                myDisplay.setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
        }
    }



    /**
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {
        try {
            showWaitCursor();
            if ( !fromBundle) {
                loadVolumeData();
            }
        } catch (Exception exc) {
            logException("loading volume data", exc);
        } finally {
            showNormalCursor();
        }

    }


    /**
     * Set the data in this control.
     *
     * @param choice  data description
     *
     * @return true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
            return false;
        }

        DataSelection tmpSelection = new DataSelection(getDataSelection());
        tmpSelection.setFromLevel(null);
        tmpSelection.setToLevel(null);
        List cchoices = ((DerivedDataChoice) choice).getChoices();
        List levelsList;
        if (cchoices.size() == 2) {  //colored by other
            DataChoice uvwchoice =
                ((DataChoice) ((DerivedDataChoice) choice).getChoices().get(
                    0));
            DataChoice wchoice =
                ((DataChoice) ((DerivedDataChoice) uvwchoice).getChoices()
                .get(2));
            levelsList = wchoice.getAllLevels(tmpSelection);
        } else {
            DataChoice wchoice =
                ((DataChoice) ((DerivedDataChoice) choice).getChoices().get(
                    2));
            levelsList = wchoice.getAllLevels(tmpSelection);
        }
        //List     levelsList = wchoice.getAllLevels(tmpSelection);
        Object[] levels = getGridDataInstance().getLevels();
        if ((levels == null)
                && (levelsList != null)
                && (levelsList.size() > 0)) {
            levels =
                (Object[]) levelsList.toArray(new Object[levelsList.size()]);
        }

        //if (levels == null) {
        //    levels = getGridDataInstance().getLevels();
        //}

        if(!reloadFromBounds)
            setLevels(levels);

        myDisplay.setActive(false);
        myDisplay.setUseSpeedForColor(useSpeedForColor);
        myDisplay.setColoredByAnother(coloredByAnother);
        if (useSpeedForColor) {
            colorIndex = myDisplay.getSpeedTypeIndex();
        }
        if (coloredByAnother) {
            colorIndex = 3;
        }

        loadVolumeData();

        if (useSpeedForColor || coloredByAnother) {
            setFlowColorRange();
        }

        myDisplay.setActive(true);
        return true;
    }


    /** level selection box */
    private JComboBox levelBox;

    /** level selection box */
    private JComboBox slevelBox;

    /** level selection box */
    private JComboBox slevelBoxEnd;

    /** level selection box */
    private JComboBox levelBoxEnd;

    /** current level */
    protected Object currentLevel;

    /**
     * _more_
     *
     * @param levels _more_
     */
    public void setLevels(Object[] levels) {

        if (levels == null) {
            levels = getGridDataInstance().getLevels();
        }

        setOkToFireEvents(false);

        if ((levelBox == null) || (slevelBox == null)) {
            return;
        }
        levelBox.setEnabled(false);
        levelBoxEnd.setEnabled(false);
        slevelBox.setEnabled(false);
        slevelBoxEnd.setEnabled(false);

        Object[] all = formatLevels(levels);

        GuiUtils.setListData(levelBox, formatLevels(levels));
        GuiUtils.setListData(levelBoxEnd, formatLevels(levels));
        GuiUtils.setListData(slevelBox, formatLevels(levels));
        GuiUtils.setListData(slevelBoxEnd, formatLevels(levels));

        int len = (levels.length - 1) / (skipValueZ + 1) + 2;

        if ( !fromBundle) {
            currentLevel = all[len - 1];
        }
        trajStartLevel    = getTrajStartLevel();
        streamLStartLevel = getStreamLStartLevel();

        if (currentLevel == null) {
            levelBox.setSelectedItem(getLabeledReal(all[levels.length]));
            slevelBox.setSelectedItem(getLabeledReal(all[levels.length]));
        } else {
            levelBox.setSelectedItem(getLabeledReal(currentLevel));
            slevelBox.setSelectedItem(getLabeledReal(currentLevel));
        }
        if ((currentLevel != null)
                && ((TwoFacedObject) currentLevel).getLabel().equals(
                    "All Levels")) {
            slevelBoxEnd.setSelectedItem(getLabeledReal(currentLevel));
        } else if (currentLevel == null) {
            slevelBoxEnd.setSelectedItem(getLabeledReal(all[levels.length]));
        } else {
            slevelBoxEnd.setSelectedItem(
                getLabeledReal(all[getStreamLEndLevel()]));
        }

        if ((currentLevel != null)
                && ((TwoFacedObject) currentLevel).getLabel().equals(
                    "All Levels")) {
            levelBoxEnd.setSelectedItem(getLabeledReal(currentLevel));
        } else if (currentLevel == null) {
            levelBoxEnd.setSelectedItem(getLabeledReal(all[levels.length]));
        } else {
            levelBoxEnd.setSelectedItem(
                getLabeledReal(all[getTrajEndLevel()]));
        }

        setOkToFireEvents(true);
        levelBox.setEnabled(true);
        levelBoxEnd.setEnabled(true);
        slevelBox.setEnabled(true);
        slevelBoxEnd.setEnabled(true);
    }

    /**
     * _more_
     *
     * @param levels _more_
     *
     * @return _more_
     */
    protected Object[] formatLevels(Object[] levels) {
        if (levels == null) {
            return null;
        }
        int      zskip   = skipValueZ;
        int      len     = (levels.length - 1) / (zskip + 1) + 1 + 1;
        Object[] tfoList = new Object[len];
        for (int i = 0; i < len - 1; i++) {
            int j = i * (zskip + 1);
            tfoList[i] = getLabeledReal(levels[j]);
        }
        tfoList[len - 1] = new TwoFacedObject("All Levels", new Real(0));
        return tfoList;
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void setFlowColorRange() throws RemoteException, VisADException {
        if ((getGridDisplay() != null)) {
            if (getFlowRange() == null) {
                Range[] ranges = null;
                Data    data   = getGridDisplay().getData();
                if (data != null) {
                    ranges = GridUtil.getMinMax((FieldImpl) data);
                    double  max         = Double.NEGATIVE_INFINITY;
                    double  min         = Double.POSITIVE_INFINITY;
                    int     startComp   = 0;
                    int     numComps    = getIsThreeComponents()
                                          ? 3
                                          : 2;
                    boolean isCartesian = getGridDisplay().isCartesianWind();
                    //System.out.println("control thinks cartesian is " + isCartesian);
                    if ( !isCartesian) {
                        int speedIndex = getGridDisplay().getSpeedTypeIndex();
                        if (speedIndex != -1) {
                            startComp = speedIndex;
                            numComps  = startComp + 1;
                        }
                    }
                    if ((useSpeedForColor || coloredByAnother)
                            && (ranges.length > numComps)) {
                        Range compRange = ranges[ranges.length - 1];
                        max = Math.max(compRange.getMax(), max);
                        min = Math.min(compRange.getMin(), min);
                    } else {
                        for (int i = startComp; i < numComps; i++) {
                            Range compRange = ranges[i];
                            max = Math.max(compRange.getMax(), max);
                            //min = Math.min(compRange.getMin(), min);
                            min = Math.min(compRange.getMin(), min);
                        }
                    }

                    if ( !useSpeedForColor
                            && !coloredByAnother
                            && !Double.isInfinite(max)
                            && !Double.isInfinite(min)) {
                        max = Math.max(max, -min);
                        min = isCartesian
                              ? -max
                              : 0;
                    }

                    flowColorRange = new Range(min, max);
                } else {  // gotta set it to something
                    flowColorRange = new Range(-40, 40);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param arrow _more_
     */
    public void setArrowHead(boolean arrow) {
        arrowHead = arrow;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getArrowHead() {
        return arrowHead;
    }

    /**
     * _more_
     *
     * @param arrow _more_
     */
    public void setArrowHeadL(boolean arrow) {
        arrowHeadL = arrow;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getArrowHeadL() {
        return arrowHeadL;
    }

    /**
     * _more_
     *
     * @param colorRange _more_
     */
    public void setFlowColorRange(Range colorRange) {
        flowColorRange = colorRange;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Range getFlowColorRange() {
        return flowColorRange;
    }

    /**
     * Get the flow range.
     * Used by XML persistence
     *
     * @return  the flow range for this control
     */
    public Range getFlowRange() {
        return flowRange;
    }




    /**
     * Set the flow range.
     * Used by XML persistence
     *
     * @param f   new flow range
     */
    public void setFlowRange(Range f) {
        flowRange = f;
        if ((getGridDisplay() != null) && (flowRange != null)) {
            try {
                getGridDisplay().setFlowRange(flowRange);
            } catch (Exception excp) {
                logException("setFlowRange: ", excp);
            }
        }

        if (getHaveInitialized()) {
            doShare(SHARE_FLOWRANGE, flowRange);
        }
    }

    /**
     * Make the gui. Align it left
     *
     * @return The gui
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        return GuiUtils.left(doMakeWidgetComponent());
    }



    /**
     * Load the volume data to the display
     *
     * @throws RemoteException   problem loading remote data
     * @throws VisADException    problem loading the data
     */
    private void loadVolumeData() throws VisADException, RemoteException {
        Trace.call1("VRC.loadVolumeData");
        FieldImpl grid    = getGridDataInstance().getGrid();
        FieldImpl newGrid = grid;

        //if(!isTrajectories) {
        if ((getSkipValue() > 0) && (getSkipValueZ() > 0)) {
            grid = GridUtil.subset(grid, getSkipValue() + 1,
                                   getSkipValue() + 1, getSkipValueZ() + 1);
            newGrid = grid;
        } else if (getSkipValue() > 0) {
            grid    = GridUtil.subset(grid, getSkipValue() + 1);
            newGrid = grid;
        } else if (getSkipValueZ() > 0) {
            grid    = GridUtil.subset(grid, 1, 1, getSkipValueZ() + 1);
            newGrid = grid;
        }

        Trace.call1("VRC.loadVolumeData.loadData");
        myDisplay.loadData(newGrid);
        Trace.call2("VRC.loadVolumeData.loadData");
        Trace.call2("loadVolumeData");
    }


    /**
     * _more_
     *
     * @param trajForm _more_
     * @param skip _more_
     * @param setLocs _more_
     * @param lenX _more_
     * @param lenY _more_
     * @param level _more_
     * @param flowValues _more_
     * @param ribbonWidthFac _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public float[][] setStartPointsFromDomain2D(int trajForm, int skip,
            float[][] setLocs, int lenX, int lenY, int level,
            float[][] flowValues, float ribbonWidthFac)
            throws VisADException {
        int       len2D  = lenX * lenY;


        float[][] locs2D = new float[3][len2D];

        System.arraycopy(setLocs[0], level * len2D, locs2D[0], 0, len2D);
        System.arraycopy(setLocs[1], level * len2D, locs2D[1], 0, len2D);
        System.arraycopy(setLocs[2], level * len2D, locs2D[2], 0, len2D);

        float[][] startPts = new float[3][];
        int[]     o_j      = new int[] { 0, 0, 1, 1 };
        int[]     o_i      = new int[] { 0, 1, 0, 1 };
        int       m        = 0;

        int       jA       = 1 + o_j[m] * (skip / 2);
        int       jB       = lenY - skip;
        int       iA       = 1 + o_i[m] * (skip / 2);
        int       iB       = lenX - skip;

        int       numJ     = 1 + ((jB - 1) - jA) / skip;
        int       numI     = 1 + ((iB - 1) - iA) / skip;
        int       num      = numJ * numI;

        if (trajForm == TrajectoryParams.DEFORM_RIBBON) {
            num *= 2;
        }

        startPts[0] = new float[num];
        startPts[1] = new float[num];
        startPts[2] = new float[num];

        float[] norm  = new float[] { 0f, 0f, 1f };
        float[] traj  = new float[3];
        float   width = ribbonWidthFac * 0.006f;

        num = 0;
        for (int j = 1 + o_j[m] * (skip / 2); j < lenY - skip; j += skip) {
            for (int i = 1 + o_i[m] * (skip / 2); i < lenX - skip;
                    i += skip) {

                int k = j * lenX + i;


                if (trajForm == TrajectoryParams.DEFORM_RIBBON) {
                    float u = flowValues[0][k];
                    float v = flowValues[1][k];

                    traj[0] = u;
                    traj[1] = v;
                    traj[2] = 0f;
                    float mag = (float) Math.sqrt(u * u + v * v);
                    traj[0] /= mag;
                    traj[1] /= mag;
                    float[] norm_x_traj = AxB(norm, traj);


                    startPts[0][num] = width * norm_x_traj[0] + locs2D[0][k];
                    startPts[1][num] = width * norm_x_traj[1] + locs2D[1][k];
                    startPts[2][num] = width * norm_x_traj[2] + locs2D[2][k];


                    num++;

                    startPts[0][num] = -width * norm_x_traj[0] + locs2D[0][k];
                    startPts[1][num] = -width * norm_x_traj[1] + locs2D[1][k];
                    startPts[2][num] = -width * norm_x_traj[2] + locs2D[2][k];


                    num++;

                } else {
                    startPts[0][num] = locs2D[0][k];
                    startPts[1][num] = locs2D[1][k];
                    startPts[2][num] = locs2D[2][k];

                    num++;

                }

            }
        }


        return startPts;
    }

    /**
     * _more_
     *
     * @param A _more_
     * @param B _more_
     *
     * @return _more_
     */
    public static float[] AxB(float[] A, float[] B) {
        float[] axb = new float[3];

        axb[0] = A[1] * B[2] - A[2] * B[1];
        axb[1] = -(A[0] * B[2] - A[2] * B[0]);
        axb[2] = A[0] * B[1] - A[1] * B[0];

        return axb;
    }

    /**
     * Make a grid with a Linear3DSet for the volume rendering
     *
     * @param domainSet grid to transform
     * @param cs   coordinate system to transform to XYZ
     *
     * @return transformed grid
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   problem creating grid
     */
    private GriddedSet makeLinearGrid(GriddedSet domainSet,
                                      CoordinateSystem cs)
            throws VisADException, RemoteException {

        Trace.call1("VRC.makeLinearGrid");

        SampledSet ss          = null;
        boolean    latLonOrder = GridUtil.isLatLonOrder(domainSet);
        //System.out.println("grid is latLonOrder " + latLonOrder);
        Trace.call1("VRC.convertDomain");
        if (latLonOrder) {
            ss = Util.convertDomain(domainSet,
                                    RealTupleType.LatitudeLongitudeAltitude,
                                    null);
        } else {
            ss = Util.convertDomain(domainSet,
                                    RealTupleType.SpatialEarth3DTuple, null);
        }
        Trace.call2("VRC.convertDomain");
        float[][] refVals = ss.getSamples(true);
        MapProjectionDisplay mpd =
            (MapProjectionDisplay) getNavigatedDisplay();
        MapProjection mp             = mpd.getMapProjection();
        boolean       mapLatLonOrder = mp.isLatLonOrder();
        //System.out.println("map is latLonOrder " + mapLatLonOrder);
        float[][] newVals = (latLonOrder)
                            ? refVals
                            : new float[][] {
            refVals[1], refVals[0], refVals[2]
        };
        Trace.call1("VRC.toRef");
        newVals = cs.toReference(newVals);
        Trace.call2("VRC.toRef");
        Trace.call1("VRC.scaleVerticalValues");
        newVals[2] = mpd.scaleVerticalValues(newVals[2]);
        Trace.call2("VRC.scaleVerticalValues");
        int[] lengths = domainSet.getLengths();
        //Misc.printArray("lengths",lengths);
        GriddedSet xyzSet =
            GriddedSet.create(RealTupleType.SpatialCartesian3DTuple, newVals,
                              domainSet.getLengths(),
                              (CoordinateSystem) null, (Unit[]) null,
                              (ErrorEstimate[]) null, false, true);

        return xyzSet;
    }

    /**
     * Method to call if projection changes.  Subclasses that
     * are worried about such events should implement this.
     */
    public void projectionChanged() {
        //System.out.println("projection changed");
        try {
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        }
        super.projectionChanged();
    }



    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return false;
    }


    /**
     * Get the flow scale.
     * Used by XML persistence
     *
     * @return  the flow scale for this control
     */
    public float getFlowScale() {
        return flowScaleValue;
    }

    /**
     * Set the flow scale.
     * Used by XML persistence
     *
     * @param f   new flow scale
     */
    public void setFlowScale(float f) {
        flowScaleValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setFlowScale(getDisplayScale() * scaleFactor
                        * flowScaleValue);
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getTrajOffset() {
        return trajOffsetValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public float getStreamLOffset() {
        return streamLOffsetValue;
    }

    /**
     * _more_
     *
     *
     * @param f _more_
     * @return _more_
     */

    public void setTrajOffset(float f) {
        trajOffsetValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(trajOffsetValue);
                getGridDisplay().setArrowHead(arrowHead);
                getGridDisplay().resetTrojectories();
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (trajLengthWidget != null) {
            trajLengthWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void setStreamLOffset(float f) {
        streamLOffsetValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(trajOffsetValue);
                getGridDisplay().setArrowHead(arrowHeadL);
                getGridDisplay().resetTrojectories();
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (streamLLengthWidget != null) {
            streamLLengthWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setTrajectories(boolean v) {
        isTrajectories = v;
        isVectors      = !v;
        isStreamLine   = !v;
        setStreamlines();
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setVectors(boolean v) {
        isTrajectories = !v;
        isVectors      = v;
        isStreamLine   = !v;
        setStreamlines();
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setStreamline(boolean v) {
        isTrajectories = !v;
        isVectors      = !v;
        isStreamLine   = v;
        setStreamlines();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Integer getTrajFormType() {
        return trajFormType;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Integer getStreamLFormType() {
        return streamLFormType;
    }

    /**
     * _more_
     *
     * @param trajForm _more_
     */
    public void setTrajFormType(Integer trajForm) {
        trajFormType = trajForm;

        if (isTrajectories) {
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setTrajFormType(trajForm.intValue());
                    //getGridDisplay().resetTrojectories();
                    getGridDisplay().setArrowHead(arrowHead);
                    //getGridDisplay().resetTrojectories();
                    setLineWidth(super.getLineWidth());
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }
        }
    }

    /**
     * _more_
     *
     * @param streamLForm _more_
     */
    public void setStreamLFormType(Integer streamLForm) {
        streamLFormType = streamLForm;

        if (isStreamLine) {
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setTrajFormType(streamLForm.intValue());
                    //getGridDisplay().resetTrojectories();
                    getGridDisplay().setArrowHead(arrowHeadL);
                    //getGridDisplay().resetTrojectories();
                    setLineWidth(super.getLineWidth());
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }
        }
    }

    /**
     * _more_
     *
     * @param startLevel _more_
     * @param idx _more_
     */
    public void setTrajStartLevel(Object startLevel, int idx) {
        trajStartLevel = idx;
        currentLevel   = startLevel;
        int    ct       = levelBox.getItemCount();
        Object endLevel = levelBoxEnd.getSelectedItem();
        int    jdx      = levelBoxEnd.getSelectedIndex();
        if (ct == 1) {
            ct = 2;
        }
        //if(skipFactorWidgetZ != null)
        //    skipFactorWidgetZ.setValue(ct-1);
        if ((getGridDisplay() != null) && !fromBundle) {
            try {
                getGridDisplay().setTrajStartLevel(idx);
                if (((TwoFacedObject) startLevel).getLabel().equals(
                        "All Levels")) {
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(0);
                    getGridDisplay().resetTrojectories();
                } else if (idx == jdx) {
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                } else if ( !((TwoFacedObject) endLevel).getLabel().equals(
                        "All Levels")) {
                    levelBoxEnd.setSelectedIndex(jdx);
                }
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }

    /**
     * _more_
     *
     * @param endLevel _more_
     * @param idx _more_
     */
    public void setTrajEndLevel(Object endLevel, int idx) {

        trajEndLevel = idx;
        //currentLevel   = endLevel;
        int bt = levelBoxEnd.getItemCount();
        int ct = levelBox.getItemCount();
        if (ct == 1) {
            ct = 2;
        }
        if (bt == 1) {
            bt = 2;
        }
        int jdx = levelBox.getSelectedIndex();
        //if(skipFactorWidgetZ != null)
        //    skipFactorWidgetZ.setValue(ct-1);
        if (getGridDisplay() != null) {
            try {
                if (((TwoFacedObject) endLevel).getLabel().equals(
                        "All Levels")
                        || ((TwoFacedObject) currentLevel).getLabel().equals(
                            "All Levels")) {
                    getGridDisplay().setTrajStartLevel(0);
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(0);
                    getGridDisplay().resetTrojectories();
                } else if (idx == jdx) {
                    getGridDisplay().setTrajStartLevel(idx);
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                } else {
                    GriddedSet domainSet =
                        (GriddedSet) getGridDataInstance().getSpatialDomain();

                    CoordinateSystem cs =
                        getNavigatedDisplay().getDisplayCoordinateSystem();
                    float[][] domainLatLonAlt =
                        GridUtil.getEarthLocationPoints(domainSet);
                    MapProjectionDisplay mpd =
                        (MapProjectionDisplay) getNavigatedDisplay();
                    boolean isLatLon = GridUtil.isLatLonOrder(domainSet);
                    int     latIndex = isLatLon
                                       ? 1
                                       : 0;
                    int     lonIndex = isLatLon
                                       ? 0
                                       : 1;

                    int     numX     = domainSet.getLengths()[lonIndex];
                    int     numY     = domainSet.getLengths()[latIndex];
                    int     numXY    = numX * numY;
                    int     startZ   = (idx > jdx)
                                       ? jdx
                                       : idx;
                    int     endZ     = (idx > jdx)
                                       ? idx
                                       : jdx;
                    int     numP;
                    int     numXY0 = numXY;
                    if(getSkipValue() > 0)
                        numXY0 = numXY / (getSkipValue() + 1) + 1;

                    numP = numXY0 * (endZ - startZ + 1);

                    float[][] geoVals = new float[3][numP];
                    int       skipFactor = getSkipValue();
                    int jj = 0;
                    for (int k = startZ; k <= endZ; k = k + 1 ) {
                        for (int j = 0; j < numXY; j = j + 1 + skipFactor) {
                            int ii = k * (numXY) * (getSkipValueZ() + 1)+ j;
                            geoVals[0][jj] = domainLatLonAlt[lonIndex][ii];
                            geoVals[1][jj] = domainLatLonAlt[latIndex][ii];
                            geoVals[2][jj] = domainLatLonAlt[2][ii];
                            jj = jj + 1;
                        }
                    }

                    float[][] setLocs = cs.toReference(geoVals);
                    setLocs[2] = mpd.scaleVerticalValues(setLocs[2]);
                    RealTupleType types = cs.getReference();
                    getGridDisplay().setStartPoints(types, setLocs);
                    //getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                }

            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }


    /**
     * _more_
     *
     * @param startLevel _more_
     * @param idx _more_
     */
    public void setStreamLStartLevel(Object startLevel, int idx) {
        streamLStartLevel = idx;
        currentLevel      = startLevel;
        int    ct       = slevelBox.getItemCount();
        Object endLevel = slevelBoxEnd.getSelectedItem();
        int    jdx      = slevelBoxEnd.getSelectedIndex();
        if (ct == 1) {
            ct = 2;
        }

        //if(skipFactorWidgetZ != null)
        //    skipFactorWidgetZ.setValue(ct-1);
        if ((getGridDisplay() != null) && !fromBundle) {
            try {
                getGridDisplay().setTrajStartLevel(idx);
                if (((TwoFacedObject) startLevel).getLabel().equals(
                        "All Levels")) {
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(0);
                    getGridDisplay().resetTrojectories();
                } else if (idx == jdx) {
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                } else if ( !((TwoFacedObject) endLevel).getLabel().equals(
                        "All Levels")) {
                    slevelBoxEnd.setSelectedIndex(jdx);
                }
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getStreamLStartLevel() {
        return streamLStartLevel;
    }

    /**
     * _more_
     *
     * @param startLevel _more_
     */
    public void setStreamLStartLevel(int startLevel) {
        this.streamLStartLevel = startLevel;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getStreamLEndLevel() {
        return streamLEndLevel;
    }

    /**
     * _more_
     *
     * @param endLevel _more_
     */
    public void setStreamLEndLevel(int endLevel) {
        this.streamLEndLevel = endLevel;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getCurrentLevel() {
        return currentLevel;
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void setCurrentLevel(Object object) {
        currentLevel = object;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getTrajStartLevel() {
        return trajStartLevel;
    }

    /**
     * _more_
     *
     * @param startLevel _more_
     */
    public void setTrajStartLevel(int startLevel) {
        this.trajStartLevel = startLevel;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getTrajEndLevel() {
        return trajEndLevel;
    }

    /**
     * _more_
     *
     * @param endLevel _more_
     */
    public void setTrajEndLevel(int endLevel) {
        this.trajEndLevel = endLevel;
    }

    /**
     * _more_
     *
     * @param endLevel _more_
     * @param idx _more_
     */
    public void setStreamLEndLevel(Object endLevel, int idx) {

        streamLEndLevel = idx;
        //currentLevel   = endLevel;
        int bt = slevelBoxEnd.getItemCount();
        int ct = slevelBox.getItemCount();
        if (ct == 1) {
            ct = 2;
        }
        if (bt == 1) {
            bt = 2;
        }
        int jdx = slevelBox.getSelectedIndex();
        //if(skipFactorWidgetZ != null)
        //    skipFactorWidgetZ.setValue(ct-1);
        if (getGridDisplay() != null) {
            try {
                if (((TwoFacedObject) endLevel).getLabel().equals(
                        "All Levels")
                        || ((TwoFacedObject) currentLevel).getLabel().equals(
                            "All Levels")) {
                    getGridDisplay().setTrajStartLevel(0);
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(0);
                    getGridDisplay().resetTrojectories();
                } else if (idx == jdx) {
                    getGridDisplay().setTrajStartLevel(idx);
                    getGridDisplay().setStartPoints(null, null);
                    getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                } else {
                    GriddedSet domainSet =
                        (GriddedSet) getGridDataInstance().getSpatialDomain();

                    CoordinateSystem cs =
                        getNavigatedDisplay().getDisplayCoordinateSystem();
                    float[][] domainLatLonAlt =
                        GridUtil.getEarthLocationPoints(domainSet);
                    MapProjectionDisplay mpd =
                        (MapProjectionDisplay) getNavigatedDisplay();
                    boolean isLatLon = GridUtil.isLatLonOrder(domainSet);
                    int     latIndex = isLatLon
                                       ? 1
                                       : 0;
                    int     lonIndex = isLatLon
                                       ? 0
                                       : 1;

                    int     numX     = domainSet.getLengths()[lonIndex];
                    int     numY     = domainSet.getLengths()[latIndex];
                    int     numXY    = numX * numY;
                    int     startZ   = (idx > jdx)
                                       ? jdx
                                       : idx;
                    int     endZ     = (idx > jdx)
                                       ? idx
                                       : jdx;

                    int     numP;
                    int     numXY0 = numXY;
                    if(getSkipValue() > 0)
                        numXY0 = numXY / (getSkipValue() + 1) + 1;

              /*      if(getSkipValueZ() > 0)
                        numZ = numZ / (getSkipValueZ() + 1) + 1; */

                    numP = numXY0 * (endZ - startZ + 1);

                    float[][] geoVals = new float[3][numP];
                    int       skipFactor = getSkipValue();
                    int jj = 0;
                    for (int k = startZ; k <= endZ; k = k + 1 ) {
                        for (int j = 0; j < numXY; j = j + 1 + skipFactor) {
                            int ii = k * (numXY) * (getSkipValueZ() + 1)+ j;
                            geoVals[0][jj] = domainLatLonAlt[lonIndex][ii];
                            geoVals[1][jj] = domainLatLonAlt[latIndex][ii];
                            geoVals[2][jj] = domainLatLonAlt[2][ii];
                            jj = jj + 1;
                        }
                    }

                    float[][] setLocs = cs.toReference(geoVals);
                    setLocs[2] = mpd.scaleVerticalValues(setLocs[2]);
                    RealTupleType types = cs.getReference();
                    getGridDisplay().setStartPoints(types, setLocs);

                  //  getGridDisplay().setZskip(ct - 1);
                    getGridDisplay().resetTrojectories();
                }

            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }

    /**
     * _more_
     *
     * @param yesno _more_
     */
    public void setColoredByAnother(boolean yesno) {
        coloredByAnother = yesno;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getColoredByAnother() {
        return coloredByAnother;
    }

    /**
     * _more_
     *
     * @param yesno _more_
     */
    public void setUseSpeedForColor(boolean yesno) {
        useSpeedForColor = yesno;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getUseSpeedForColor() {
        return useSpeedForColor;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected int getColorRangeIndex() {
        if (colorIndex >= 0) {
            return colorIndex;
        }

        return getIsThreeComponents()
               ? 3
               : 2;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public Range getRangeForColorTable()
            throws RemoteException, VisADException {
        if (getFlowColorRange() == null) {
            setFlowColorRange();
        }
        return getFlowColorRange();
    }

    /**
     * _more_
     *
     * @param r _more_
     *
     * @return _more_
     */
    private Range makeFlowRange(Range r) {
        if (haveMultipleFields()) {
            return r;
        }
        if (r == null) {
            return r;
        }
        double max = Math.max(Math.abs(r.getMax()), Math.abs(r.getMin()));
        return new Range(-max, max);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean haveMultipleFields() {
        if (getGridDataInstance() == null) {
            return false;
        }
        return ((getGridDataInstance().getNumRealTypes()
                 > ((getIsThreeComponents())
                    ? 3
                    : 2))) || useSpeedForColor || coloredByAnother;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showColorControlWidget() {
        return !useSpeedForColor && !coloredByAnother;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getColorWidgetLabel() {
        return "Color";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getColorParamName() {
        if (useSpeedForColor) {
            return "windSpeed";
        } else if (coloredByAnother) {
            return getGridDataInstance().getRealTypeName(colorIndex);
        } else {
            return super.getColorParamName();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        if (useSpeedForColor) {
            return flowRange;
        } else if (coloredByAnother) {
            return getGridDataInstance().getRanges()[colorIndex];
        } else {
            return super.getInitialRange();
        }

    }


    /**
     * Set the skip value, if it is vector display we reload the sampled
     * data, otherwise, it is trajectory, skip only apply to the display not data
     *
     * @param value the value
     */
    public void setSkipValueZ(int value) {
        skipValueZ = value;
        if (skipFactorWidgetZ != null) {
            skipFactorWidgetZ.setValue(value);
        }

        if (getControlContext() != null) {
            applySkipFactor();
            setLevels(null);
        }

        doShare(SHARE_SKIPVALUE + "Z", Integer.valueOf(skipValueZ));

        FlowDisplayable fd = getGridDisplay();
        if (fd != null) {
            int    ct;
            String tt;
            if (isStreamLine) {
                tt = slevelBox.getSelectedItem().toString();
                ct = slevelBox.getItemCount();
            } else {
                tt = levelBox.getSelectedItem().toString();
                ct = levelBox.getItemCount();
            }
            if (tt.equals("All Levels")) {
                getGridDisplay().setZskip(1);
            } else {
                getGridDisplay().setZskip(ct - 1);
            }
            if (isTrajectories) {
                getGridDisplay().resetTrojectories();
            }
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSkipValueZ() {
        return (int) ((skipFactorWidgetZ == null)
                      ? skipValueZ
                      : skipFactorWidgetZ.getValue());

    }


    /**
     * _more_
     *
     *
     * @param width _more_
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setLineWidth(int width)
            throws RemoteException, VisADException {
        if ((isTrajectories || isStreamLine) && (getGridDisplay() != null)) {
            if (trajFormType == 4) {
                setPointSize(width * 1.0f);
            } else if (trajFormType == 2) {
                getGridDisplay().setTrajWidth(width * 0.01f);
            } else if ((trajFormType == 1) || (trajFormType == 3)) {
                getGridDisplay().setRibbonWidth(width);
            }
            if ( !fromBundle) {
                getGridDisplay().resetTrojectories();
            }
        }

        super.setLineWidth(width);

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLineWidth() {
        return super.getLineWidth();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsTrajectories() {
        return isTrajectories;
    }

    /**
     * _more_
     *
     * @param isTrajectories _more_
     */
    public void setIsTrajectories(boolean isTrajectories) {
        this.isTrajectories = isTrajectories;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsStreamline() {
        return isStreamLine;
    }

    /**
     * _more_
     *
     * @param isStreamLine _more_
     */
    public void setIsStreamline(boolean isStreamLine) {
        this.isStreamLine = isStreamLine;
    }


    /**
     * _more_
     *
     * @param vc _more_
     * @param properties _more_
     */
    @Override
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties) {
        fromBundle = true;
        super.initAfterUnPersistence(vc, properties);
        if (isTrajectories) {
            if (trajectoryBtn == null) {
                doMakeWidgetComponent();
            }
            trajectoryBtn.doClick();
            //setTrajFormType(getTrajFormType());
            int width = super.getLineWidth();
            if (isTrajectories && (getGridDisplay() != null)) {
                getGridDisplay().setTrajFormType(trajFormType);
                if (trajFormType == 2) {
                    getGridDisplay().setTrajWidth(width * 0.01f);
                } else if ((trajFormType == 1) || (trajFormType == 3)) {
                    getGridDisplay().setRibbonWidth(width);
                }
                if (arrowHead) {
                    getGridDisplay().setArrowHead(true);
                    arrowCbx.setSelected(arrowHead);
                }
                levelBoxEnd.setSelectedIndex(getTrajEndLevel());
            }
            //getGridDisplay().resetTrojectories();
        } else if (isStreamLine) {
            if (streamlineBtn == null) {
                doMakeWidgetComponent();
            }
            streamlineBtn.doClick();
            //setStreamLFormType(getStreamLFormType());
            int width = super.getLineWidth();
            if (isStreamLine && (getGridDisplay() != null)) {
                getGridDisplay().setTrajFormType(streamLFormType);
                if (streamLFormType == 2) {
                    getGridDisplay().setTrajWidth(width * 0.01f);
                } else if ((streamLFormType == 1) || (streamLFormType == 3)) {
                    getGridDisplay().setRibbonWidth(width);
                }
                if (arrowHeadL) {
                    getGridDisplay().setArrowHead(true);
                    arrowCbxL.setSelected(arrowHeadL);
                }
                if ((currentLevel != null)
                        && ((TwoFacedObject) currentLevel).getLabel().equals(
                            "All Levels")) {
                    slevelBox.setSelectedItem(currentLevel);
                } else if (currentLevel == null) {
                    int len = slevelBoxEnd.getItemCount();
                    slevelBox.setSelectedIndex(len - 1);
                } else {
                    slevelBox.setSelectedIndex(getStreamLStartLevel());
                }
                slevelBoxEnd.setSelectedIndex(getStreamLEndLevel());
            }

            //getGridDisplay().resetTrojectories();
        } else {
            if (vectorBtn == null) {
                doMakeWidgetComponent();
            }
            vectorBtn.doClick();
            setFlowScale(flowScaleValue);
            setArrowHeadSize(arrowHeadSizeValue);
        }
        if (skipFactorWidgetZ != null) {
            skipFactorWidgetZ.setValue(getSkipValueZ());
        }

        fromBundle = false;
    }

}
