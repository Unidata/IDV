/*
 * Copyright 1997-2017 Unidata Program Center/University Corporation for
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


import edu.wisc.ssec.mcidasv.data.hydra.StatsTable;

import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import edu.wisc.ssec.mcidasv.data.hydra.HistogramField;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.LongitudeLatitudeCoordinateSystem;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.ColorTableWidget;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;

import ucar.visad.data.AreaImageFlatField;
import ucar.visad.data.MyAreaImageFlatField;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.MapLines;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.XYDisplay;

import visad.*;

import visad.data.mcidas.BaseMapAdapter;

import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

import visad.python.JPythonMethods;

/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.
 *
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */




import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Tue, Feb 6, '18
 * @author         Enter your name here...    
 */
public class ScatterDisplay extends DisplayControlImpl {

    /** _more_ */
    private static final Logger logger =
        LoggerFactory.getLogger(ScatterDisplay.class);

    /** _more_ */
    private Container container;

    /** _more_ */
    private FlatField X_field;

    /** _more_ */
    private FlatField Y_field;

    /** _more_ */
    private FlatField Area_field;

    /** _more_ */
    private double total_area;

    /** _more_ */
    private DisplayMaster scatterMaster = null;

    /** _more_ */
    private DisplayMaster dspMasterX;

    /** _more_ */
    private DisplayMaster dspMasterY;

    /** _more_ */
    private HistogramField histoField;

    /** _more_ */
    private FlatField mask_field;

    /** _more_ */
    private float[][] mask_range;

    /** _more_ */
    private float[][] scatterFieldRange;

    /** _more_ */
    private Data X_data;

    /** _more_ */
    private Data Y_data;

    /** _more_ */
    private String X_name;

    /** _more_ */
    private String Y_name;

    /** _more_ */
    private boolean cancel = false;

    /** _more_ */
    private ScatterDisplayable scatterMarkDsp;

    /** _more_ */
    private BoxCurveSwitch boxCurveSwitch;

    /** _more_ */
    public DataChoice dataChoiceX = null;

    /** _more_ */
    public DataChoice dataChoiceY = null;

    /** _more_ */
    public DataSelection dataSelectionX = null;

    /** _more_ */
    public DataSelection dataSelectionY = null;

    /** _more_ */
    JComponent ctwCompX;

    /** _more_ */
    JComponent ctwCompY;

    /** _more_ */
    ColorTableWidget ctw;

    /** _more_ */
    int n_selectors = 3;

    /** _more_ */
    List<ScatterBoxSelector> scatterBoxSelectors = new ArrayList<>();

    /** _more_ */
    List<ScatterCurveSelector> scatterCurveSelectors = new ArrayList<>();

    /** _more_ */
    List<ImageBoxSelector> imageXBoxSelectors = new ArrayList<>();

    /** _more_ */
    List<ImageBoxSelector> imageYBoxSelectors = new ArrayList<>();

    /** _more_ */
    List<ImageCurveSelector> imageXCurveSelectors = new ArrayList<>();

    /** _more_ */
    List<ImageCurveSelector> imageYCurveSelectors = new ArrayList<>();

    /** _more_ */
    JToggleButton[] selectorToggleButtons = new JToggleButton[n_selectors];

    /** _more_ */
    Color[] selectorColors = new Color[] { Color.magenta, Color.green,
                                           Color.blue };

    /** _more_ */
    float[][] maskColorPalette = new float[][] {
        { 0.8f, 0.0f, 0.0f }, { 0.0f, 0.8f, 0.0f }, { 0.8f, 0.0f, 0.8f }
    };

    /** _more_ */
    float[][] markPaletteBlackBackground = new float[][] {
        { 1.0f, 0.8f, 0.0f, 0.0f }, { 1.0f, 0.0f, 0.8f, 0.0f },
        { 1.0f, 0.8f, 0.0f, 0.8f }
    };

    /** _more_ */
    float[][] markPaletteWhiteBackground = new float[][] {
        { 0.0f, 0.8f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.8f, 0.0f },
        { 0.0f, 0.8f, 0.0f, 0.8f }
    };

    /** used for persistence */
    private boolean blackBackground = true;

    /** _more_ */
    JRadioButton bgColorBlack;

    /** _more_ */
    JRadioButton bgColorWhite;

    /** _more_ */
    ButtonGroup bgColorGroup;

    /** _more_ */
    JButton computeStatsButton;

    /** _more_ */
    StatsTable statsTable;

    /** _more_ */
    boolean selectByCurve = false;

    /**
     * Construct a ScatterDisplay 
     */
    public ScatterDisplay() {
        super();
        setHelpUrl("idv.controls.misc.scatteranalysiscontrol");
    }


    @Override
    public boolean init(List choices) throws VisADException, RemoteException {

        bgColorBlack = new JRadioButton("Black");
        bgColorBlack.addActionListener(
            e -> {
                scatterMaster.setForeground(Color.white);
                scatterMaster.setBackground(Color.black);
                setBlackBackground(true);
                try {
                    scatterMarkDsp.setColorPalette(
                        markPaletteBlackBackground);
                } catch (Exception ex) {
                    logger.error("could not change color palette", ex);
                }
            });

        bgColorWhite = new JRadioButton("White");
        bgColorWhite.addActionListener(
            e -> {
                scatterMaster.setForeground(Color.black);
                scatterMaster.setBackground(Color.white);
                setBlackBackground(false);
                try {
                    scatterMarkDsp.setColorPalette(
                        markPaletteWhiteBackground);
                } catch (Exception ex) {
                    logger.error("could not change color palette", ex);
                }
            });

        bgColorGroup = new ButtonGroup();
        bgColorGroup.add(bgColorBlack);
        bgColorGroup.add(bgColorWhite);

        bgColorBlack.setSelected(getBlackBackground());
        bgColorWhite.setSelected( !getBlackBackground());

        if ((dataChoiceX != null) && (dataChoiceY != null)) {
            setupFromUnpersistence();
        } else {
            try {
                setup();
            } catch (VisADException vade) {
                return false;
            }
        }

        mask_field = new FlatField(
            new FunctionType(((FunctionType) X_field.getType()).getDomain(),
                             RealType.Generic), X_field.getDomainSet());

        int   len  = X_field.getDomainSet().getLength();
        int[] lens = ((Gridded2DSet) X_field.getDomainSet()).getLengths();
        mask_range = new float[1][len];
        for (int t = 0; t < len; t++) {
            mask_range[0][t] = Float.NaN;
        }
        mask_range[0][0] = 0;  //- field should not be all missing
        mask_field.setSamples(mask_range, false);

        try {
            int binSize = ((lens[0] * lens[1] / (256 * 256)) * 4) / 10;
            if (binSize < 2) {
                binSize = 2;
            }
            histoField = new HistogramField(X_field, Y_field, mask_field,
                                            256, binSize);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Range      rangeX    = getImageRange(X_field);
        Range      rangeY    = getImageRange(Y_field);
        ColorTable clrTableX = getColorTable(X_field);
        ColorTable clrTableY = getColorTable(Y_field);

        dspMasterX = makeImageDisplay(getDataProjection(X_field), X_field,
                                      mask_field, rangeX, clrTableX);

        dspMasterY = makeImageDisplay(getDataProjection(Y_field), Y_field,
                                      mask_field, rangeY, clrTableY);

        dspMasterX.addDisplayListener(
            e -> {
                double[] xProjection = dspMasterX.getProjectionMatrix();
                double[] yProjection = dspMasterY.getProjectionMatrix();
                if (xProjection.equals(yProjection)) {
                    return;
                }

                try {
                    dspMasterY.setProjectionMatrix(xProjection);
                } catch (Exception ex) {
                    LogUtil.logException("dspMasterX.displayChanged", ex);
                }
            });

        dspMasterY.addDisplayListener(
            e -> {
                double[] xProjection = dspMasterX.getProjectionMatrix();
                double[] yProjection = dspMasterY.getProjectionMatrix();
                if (yProjection.equals(xProjection)) {
                    return;
                }

                try {
                    dspMasterX.setProjectionMatrix(yProjection);
                } catch (Exception ex) {
                    LogUtil.logException("dspMasterX.displayChanged", ex);
                }
            });

        X_name =
            ((((FunctionType) X_field.getType()).getFlatRange()
            .getRealComponents())[0]).getName();
        Y_name =
            ((((FunctionType) Y_field.getType()).getFlatRange()
            .getRealComponents())[0]).getName();

        if (statsTable != null) {
            statsTable.setNames(X_name, Y_name);
        }

        Grid2DReadoutProbe probeX = new Grid2DReadoutProbe(X_field,
                                        dspMasterX);
        Grid2DReadoutProbe probeY = new Grid2DReadoutProbe(Y_field,
                                        dspMasterY);
        probeX.doMakeProbe(Color.red, dspMasterX);
        probeY.doMakeProbe(Color.red, dspMasterY);

        ImageControl dCntrl =
            new ImageControl(
                (HydraRGBDisplayable) dspMasterX.getDisplayables(0),
                getDisplayConventions());
        ctw = new ColorTableWidget(dCntrl, ColorTableManager.getManager(),
                                   clrTableX, rangeX);
        ctwCompX   = ctw.getLegendPanel(BOTTOM_LEGEND);
        dCntrl.ctw = ctw;

        dCntrl = new ImageControl(
            (HydraRGBDisplayable) dspMasterY.getDisplayables(0),
            getDisplayConventions());
        ctw = new ColorTableWidget(dCntrl, ColorTableManager.getManager(),
                                   clrTableY, rangeY);
        ctwCompY   = ctw.getLegendPanel(BOTTOM_LEGEND);
        dCntrl.ctw = ctw;

        return true;

    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setup() throws VisADException, RemoteException {
        dataSelectionX = getDataSelection();
        dataChoiceX    = getDataChoice();
        Hashtable tt = getRequestProperties();
        if(tt == null){
            tt = new Hashtable();
        }
        tt.put("id", dataChoiceX.getId());
        X_data         = dataChoiceX.getData(dataSelectionX, tt);
        int idx = 0;
        if(dataSelectionX != null && dataSelectionX.getTimes() != null ){
            idx = dataSelectionX.getTimes().size() - 1;
        } else if (getUsesTimeDriver() && dataChoiceX.getAllDateTimes() != null) {
            idx = 0;

        } else if (dataSelectionX.getTimes() ==  null &&
                dataChoiceX.getAllDateTimes() != null) {
            idx = dataChoiceX.getAllDateTimes().size() -1;
        }
        if (X_data instanceof FlatField) {
            X_field = (FlatField) X_data;
        } else if (X_data instanceof FieldImpl) {
            X_field = (FlatField) ((FieldImpl) X_data).getSample(idx);
        }
//if(X_field instanceof MyAreaImageFlatField)
  //  X_field = ((AreaImageFlatField)X_field);
        popupDataDialog("select Y Axis field", container, false, null);

        // if user canceled the popup, popupDataDialog will set the cancel flag
        if (cancel) {
            throw new VisADException("Scatter Display Canceled");
        }

        dataSelectionY = getDataSelection();
        dataChoiceY    = getDataChoice();
        Hashtable tt1 = getRequestProperties();
        if(tt1 == null){
            tt1 = new Hashtable();
        }
        tt1.put("id", dataChoiceY.getId());
        dataSelectionY.setGeoSelection(dataSelectionX.getGeoSelection());

        Y_data = dataChoiceY.getData(dataSelectionY, tt1);
        int idy = 0;
        if(dataSelectionY != null &&  dataSelectionY.getTimes() != null){
            idy = dataSelectionY.getTimes().size() - 1;
        } else if (getUsesTimeDriver() && dataChoiceY.getAllDateTimes() != null) {
            idy = 0;
        } else if (dataSelectionY.getTimes() == null &&
                dataChoiceY.getAllDateTimes() != null) {
            idy = dataChoiceY.getAllDateTimes().size() - 1;
        }
        if (Y_data instanceof FlatField) {
            Y_field = (FlatField) Y_data;
        } else if (Y_data instanceof FieldImpl) {
            Y_field = (FlatField) ((FieldImpl) Y_data).getSample(idy);
        }

        if ( !(X_field.getDomainSet().equals(Y_field.getDomainSet()))) {
            Y_field = resample(X_field, Y_field);
        }

        Area_field = JPythonMethods.createAreaField(X_field);
        statsTable = new StatsTable();
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setupFromUnpersistence()
            throws VisADException, RemoteException {
        X_data = dataChoiceX.getData(dataSelectionX);
        if (X_data instanceof FlatField) {
            X_field = (FlatField) X_data;
        } else if (X_data instanceof FieldImpl) {
            X_field = (FlatField) ((FieldImpl) X_data).getSample(0);
        }

        Y_data = dataChoiceY.getData(dataSelectionY);
        if (Y_data instanceof FlatField) {
            Y_field = (FlatField) Y_data;
        } else if (X_data instanceof FieldImpl) {
            Y_field = (FlatField) ((FieldImpl) Y_data).getSample(0);
        }
    }

    @Override
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {
        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);

        Color     fg;
        Color     bg;
        float[][] bgPalette;
        if (getBlackBackground()) {
            fg        = Color.white;
            bg        = Color.black;
            bgPalette = markPaletteBlackBackground;
        } else {
            fg        = Color.black;
            bg        = Color.white;
            bgPalette = markPaletteWhiteBackground;
        }
        scatterMaster.setForeground(fg);
        scatterMaster.setBackground(bg);
        try {
            scatterMarkDsp.setColorPalette(bgPalette);
        } catch (Exception ex) {
            logger.error("could not change color palette", ex);
        }
    }

    @Override
    protected void popupDataDialog(final String dialogMessage,
                                   Component from, boolean multiples,
                                   List categories) {

        List<DataChoice> choices = selectDataChoices(dialogMessage, from,
                                       multiples, categories);
        if ((choices == null) || (choices.size() == 0)) {
            logger.debug("popupDataDialog, no data choice, user canceled");
            cancel = true;
            return;
        }
        final List clonedList =
            DataChoice.cloneDataChoices((List) choices.get(0));
        dataSelection = ((DataChoice) clonedList.get(0)).getDataSelection();
        //- don't do this in a separate thread like the IDV does.
        //- We want the dataChoice list updated before return.
        try {
            addNewData(clonedList);
        } catch (Exception exc) {
            logException("Selecting new data", exc);
        }
    }


    @Override
    public void initDone() {

        try {
            DisplayMaster master = makeScatterDisplay();
            for (int k = 0; k < n_selectors; k++) {
                scatterBoxSelectors.add(new ScatterBoxSelector(master,
                        selectorColors[k],
                        (float) k));
                scatterCurveSelectors.add(new ScatterCurveSelector(master,
                        selectorColors[k],
                        (float) k));
            }
            master.draw();

            for (int k = 0; k < n_selectors; k++) {
                SubsetRubberBandBox X_subsetBox =
                    new SubsetRubberBandBox(
                        getIsLatLon(X_field), X_field,
                        ((MapProjectionDisplayJ3D) dspMasterX).getDisplayCoordinateSystem(),
                        1, false);
                X_subsetBox.setColor(selectorColors[k]);

                ImageBoxSelector markX = new ImageBoxSelector(X_subsetBox,
                                             X_field.getDomainSet(),
                                             dspMasterX, selectorColors[k],
                                             (float) k + 1, statsTable);

                SubsetRubberBandBox Y_subsetBox =
                    new SubsetRubberBandBox(
                        getIsLatLon(Y_field), Y_field,
                        ((MapProjectionDisplayJ3D) dspMasterY).getDisplayCoordinateSystem(),
                        1, false);
                Y_subsetBox.setColor(selectorColors[k]);
                ImageBoxSelector markY = new ImageBoxSelector(Y_subsetBox,
                                             Y_field.getDomainSet(),
                                             dspMasterY, selectorColors[k],
                                             (float) k + 1, statsTable);

                markX.setOther(markY);
                markY.setOther(markX);
                imageXBoxSelectors.add(markX);
                imageYBoxSelectors.add(markY);
            }

            for (int k = 0; k < n_selectors; k++) {
                CurveDrawer curveDraw = new CurveDrawer(RealType.Longitude,
                                            RealType.Latitude, 1);
                curveDraw.setColor(selectorColors[k]);
                curveDraw.setLineWidth(2);
                ImageCurveSelector curveX = new ImageCurveSelector(curveDraw,
                                                X_field, dspMasterX,
                                                selectorColors[k],
                                                (float) k + 1, statsTable);
                curveX.setActive(false);
                curveDraw.addAction(curveX);
                curveX.setVisible(false);
                dspMasterX.addDisplayable(curveDraw);

                curveDraw = new CurveDrawer(RealType.Longitude,
                                            RealType.Latitude, 1);
                curveDraw.setColor(selectorColors[k]);
                curveDraw.setLineWidth(2);
                ImageCurveSelector curveY = new ImageCurveSelector(curveDraw,
                                                Y_field, dspMasterY,
                                                selectorColors[k],
                                                (float) k + 1, statsTable);
                curveY.setActive(false);
                curveDraw.addAction(curveY);
                curveY.setVisible(false);
                dspMasterY.addDisplayable(curveDraw);

                curveX.setOther(curveY);
                curveY.setOther(curveX);
                imageXCurveSelectors.add(curveX);
                imageYCurveSelectors.add(curveY);
            }

            for (int k = 0; k < n_selectors; k++) {
                JToggleButton jtog = selectorToggleButtons[k];

                jtog.addActionListener(
                    e -> {
                        int idx = Integer.valueOf(e.getActionCommand());
                        try {
                            for (int i = 0; i < n_selectors; i++) {
                                ScatterBoxSelector boxSel =
                                    (ScatterBoxSelector) scatterBoxSelectors.get(
                                        i);
                                ImageBoxSelector imageXbox =
                                    (ImageBoxSelector) imageXBoxSelectors.get(
                                        i);
                                ImageBoxSelector imageYbox =
                                    (ImageBoxSelector) imageYBoxSelectors.get(
                                        i);
                                ScatterCurveSelector curveSel =
                                    (ScatterCurveSelector) scatterCurveSelectors.get(
                                        i);
                                ImageCurveSelector imageXcurve =
                                    (ImageCurveSelector) imageXCurveSelectors.get(
                                        i);
                                ImageCurveSelector imageYcurve =
                                    (ImageCurveSelector) imageYCurveSelectors.get(
                                        i);

                                if (i == idx) {
                                    if (
                                     !selectorToggleButtons[i].isSelected()) {

                                        if (statsTable != null) {
                                            statsTable.resetValues(i);
                                        }

                                        boxSel.reset();
                                        boxSel.setActive(false);
                                        boxSel.setVisible(false);

                                        imageXbox.reset();
                                        imageXbox.setActive(false);
                                        imageXbox.setVisible(false);

                                        imageYbox.reset();
                                        imageYbox.setActive(false);
                                        imageYbox.setVisible(false);

                                        curveSel.reset();
                                        curveSel.setActive(false);
                                        curveSel.setVisible(false);

                                        imageXcurve.reset();
                                        imageXcurve.setActive(false);
                                        imageXcurve.setVisible(false);
                                        imageYcurve.reset();
                                        imageYcurve.setActive(false);
                                        imageYcurve.setVisible(false);
                                        selectorToggleButtons[i].setSelected(
                                            true);
                                    }
                                    boxSel.setActive( !getSelectByCurve());
                                    boxSel.setVisible( !getSelectByCurve());
                                    imageXbox.setActive( !getSelectByCurve());
                                    imageXbox.setVisible(
                                         !getSelectByCurve());
                                    imageYbox.setActive( !getSelectByCurve());
                                    imageYbox.setVisible(
                                         !getSelectByCurve());

                                    curveSel.setActive(getSelectByCurve());
                                    curveSel.setVisible(getSelectByCurve());
                                    imageXcurve.setActive(getSelectByCurve());
                                    imageXcurve.setVisible(
                                        getSelectByCurve());
                                    imageYcurve.setActive(getSelectByCurve());
                                    imageYcurve.setVisible(
                                        getSelectByCurve());
                                } else {
                                    selectorToggleButtons[i].setSelected(
                                        false);
                                    boxSel.setActive(false);
                                    boxSel.setVisible(false);
                                    imageXbox.setActive(false);
                                    imageXbox.setVisible(false);
                                    imageYbox.setActive(false);
                                    imageYbox.setVisible(false);
                                    curveSel.setActive(false);
                                    curveSel.setVisible(false);
                                    imageXcurve.setActive(false);
                                    imageXcurve.setVisible(false);
                                    imageYcurve.setActive(false);
                                    imageYcurve.setVisible(false);
                                }
                            }
                        } catch (Exception exc) {
                            System.out.println(exc);
                        }
                    });

                ScatterBoxSelector boxSel =
                    (ScatterBoxSelector) scatterBoxSelectors.get(k);
                ImageBoxSelector imageXbox =
                    (ImageBoxSelector) imageXBoxSelectors.get(k);
                ImageBoxSelector imageYbox =
                    (ImageBoxSelector) imageYBoxSelectors.get(k);
                ScatterCurveSelector curveSel =
                    (ScatterCurveSelector) scatterCurveSelectors.get(k);
                ImageCurveSelector imageXcurve =
                    (ImageCurveSelector) imageXCurveSelectors.get(k);
                ImageCurveSelector imageYcurve =
                    (ImageCurveSelector) imageYCurveSelectors.get(k);

                if (k == 0) {
                    jtog.setSelected(true);
                    boxSel.setActive( !getSelectByCurve());
                    boxSel.setVisible( !getSelectByCurve());
                    imageXbox.setActive( !getSelectByCurve());
                    imageXbox.setVisible( !getSelectByCurve());
                    imageYbox.setActive( !getSelectByCurve());
                    imageYbox.setVisible( !getSelectByCurve());

                    curveSel.setActive(getSelectByCurve());
                    curveSel.setVisible(getSelectByCurve());
                    imageXcurve.setActive(getSelectByCurve());
                    imageXcurve.setVisible(getSelectByCurve());
                    imageYcurve.setActive(getSelectByCurve());
                    imageYcurve.setVisible(getSelectByCurve());
                } else {
                    boxSel.setActive(false);
                    boxSel.setVisible(false);
                    imageXbox.setActive(false);
                    imageXbox.setVisible(false);
                    imageYbox.setActive(false);
                    imageYbox.setVisible(false);
                    curveSel.setActive(false);
                    curveSel.setVisible(false);
                    imageXcurve.setActive(false);
                    imageXcurve.setVisible(false);
                    imageYcurve.setActive(false);
                    imageYcurve.setVisible(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    public DisplayMaster makeScatterDisplay()
            throws VisADException, RemoteException {

        ScatterDisplayable scatterDsp = new ScatterDisplayable("scatter",
                                            RealType.getRealType("mask"),
                                            markPaletteBlackBackground,
                                            false);
        float[]      valsX = X_field.getFloats(false)[0];
        float[]      valsY = Y_field.getFloats(false)[0];
        Integer1DSet set   = new Integer1DSet(valsX.length);
        FlatField scatter = new FlatField(new FunctionType(RealType.Generic,
                                                           new RealTupleType(
                                                               RealType.XAxis,
                                                                       RealType
                                                                           .YAxis,
                                                                       RealType.getRealType(
                                                                           "mask"))), set);
        float[] mask = new float[valsX.length];
        for (int k = 0; k < mask.length; k++) {
            mask[k] = 0;
        }
        scatterFieldRange = new float[][] {
            valsX, valsY, mask
        };
        scatter.setSamples(scatterFieldRange);
        scatterDsp.setPointSize(2f);
        scatterDsp.setRangeForColor(0, n_selectors);

        float[] xRange = minmax(valsX);
        float[] yRange = minmax(valsY);

        scatterDsp.setData(scatter);

        scatterMarkDsp = new ScatterDisplayable("scatter",
                RealType.getRealType("mask"), markPaletteBlackBackground,
                false);
        set = new Integer1DSet(2);
        scatter = new FlatField(new FunctionType(RealType.Generic,
                new RealTupleType(RealType.XAxis,
                                  RealType.YAxis,
                                  RealType.getRealType("mask"))), set);
        scatterMarkDsp.setData(scatter);
        scatterMarkDsp.setPointSize(2f);
        scatterMarkDsp.setRangeForColor(0, n_selectors);

        DisplayMaster master = scatterMaster;
        ((XYDisplay) master).showAxisScales(true);
        AxisScale scaleX = ((XYDisplay) master).getXAxisScale();
        scaleX.setTitle(X_name);
        AxisScale scaleY = ((XYDisplay) master).getYAxisScale();
        scaleY.setTitle(Y_name);

        ((XYDisplay) master).setXRange((double) xRange[0],
                                       (double) xRange[1]);
        ((XYDisplay) master).setYRange((double) yRange[0],
                                       (double) yRange[1]);
        master.addDisplayable(scatterDsp);
        master.addDisplayable(scatterMarkDsp);

        return master;
    }

    @Override
    public Container doMakeContents() {
        JPanel      pane  = new JPanel(new GridLayout(1, 3));

        Component[] comps = new Component[] { null, null, null };
        comps[0] = dspMasterX.getComponent();
        comps[1] = dspMasterY.getComponent();
        comps[2] = getScatterTabComponent();

        JPanel panelX = new JPanel(new BorderLayout());
        panelX.setBorder(new EmptyBorder(4, 4, 4, 4));
        panelX.add(comps[0], BorderLayout.CENTER);
        panelX.add(ctwCompX, BorderLayout.SOUTH);

        JPanel panelY = new JPanel(new BorderLayout());
        panelY.setBorder(new EmptyBorder(4, 4, 4, 4));
        panelY.add(comps[1], BorderLayout.CENTER);
        panelY.add(ctwCompY, BorderLayout.SOUTH);

        JPanel panelS = new JPanel(new BorderLayout());
        panelS.setBorder(new EmptyBorder(4, 4, 4, 4));
        panelS.add(comps[2], BorderLayout.CENTER);

        pane.add(panelX);
        pane.add(panelY);
        pane.add(panelS);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JRadioButton boxSelect = new JRadioButton("Box");
        boxSelect.setSelected(true);
        JRadioButton curveSelect = new JRadioButton("Curve");
        ButtonGroup  buttonGroup = new ButtonGroup();
        buttonGroup.add(boxSelect);
        buttonGroup.add(curveSelect);
        buttonPanel.add(boxSelect);
        buttonPanel.add(curveSelect);

        boxCurveSwitch = new BoxCurveSwitch();
        boxSelect.addActionListener(boxCurveSwitch);
        curveSelect.addActionListener(boxCurveSwitch);


        JPanel toggleButtonPanel = new JPanel(new FlowLayout());
        for (int k = 0; k < n_selectors; k++) {
            JToggleButton jtog =
                new JToggleButton( GuiUtils.getImageIcon(
                        "/ucar/unidata/idv/images/subset12.jpg"));

            jtog.setBorder(
                new CompoundBorder(new LineBorder(selectorColors[k],
                        2),
                                   new EmptyBorder(4, 4, 4, 4)));
            jtog.setActionCommand(String.valueOf(k));
            toggleButtonPanel.add(jtog);
            selectorToggleButtons[k] = jtog;
        }

        buttonPanel.add(toggleButtonPanel);

        JButton computeStatsButton = new JButton("compute statistics");

        computeStatsButton.addActionListener(
            e -> {
                if (statsTable == null) {
                    statsTable = new StatsTable();
                }

                statsTable.setIsShowing();
                statsTable.setFields(X_field, Y_field, 0);
            });

        buttonPanel.add(computeStatsButton);
        buttonPanel.add(new JLabel("Background Color:"));
        buttonPanel.add(bgColorBlack);
        buttonPanel.add(bgColorWhite);

        //-container = pane;
        JPanel new_pane = new JPanel(new BorderLayout());
        new_pane.add(pane, BorderLayout.CENTER);
        new_pane.add(buttonPanel, BorderLayout.SOUTH);
        container = new_pane;
        return container;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setBlackBackground(boolean value) {
        blackBackground = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getBlackBackground() {
        return blackBackground;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Component getScatterTabComponent() {
        try {
            scatterMaster = new XYDisplay("Scatter", RealType.XAxis,
                                          RealType.YAxis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scatterMaster.getComponent();
    }

    /**
     * _more_
     *
     * @param mapProj _more_
     * @param image _more_
     * @param mask_image _more_
     * @param imageRange _more_
     * @param colorTable _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public DisplayMaster makeImageDisplay(MapProjection mapProj,
                                          FlatField image,
                                          FlatField mask_image,
                                          Range imageRange,
                                          ColorTable colorTable)
            throws VisADException, RemoteException {
        MapProjectionDisplayJ3D mapProjDsp;
        DisplayMaster           dspMaster;

        mapProjDsp =
            new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(mapProj);

        RealType imageRangeType =
            (((FunctionType) image.getType()).getFlatRange()
            .getRealComponents())[0];

        boolean alphaflag = false;
        HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image",
                                           imageRangeType, null, alphaflag,
                                           null);

        imageDsp.setData(image);
        dspMaster.addDisplayable(imageDsp);
        addMapDisplayables(mapProjDsp);

        if (mask_image != null) {
            RGBDisplayable maskDsp = new ScatterDisplayable("mask",
                                         RealType.Generic, maskColorPalette,
                                         false);
            maskDsp.setData(mask_image);
            maskDsp.setRangeForColor(0, n_selectors - 1);
            dspMaster.addDisplayable(maskDsp);
        }

        dspMaster.draw();

        ScalarMap colorMap = imageDsp.getColorMap();
        colorMap.setRange(imageRange.getMin(), imageRange.getMax());
        BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
        float[][]        ct       = colorTable.getColorTable();

        if ( !(alphaflag) && (ct.length == 4)) {
            float[][] new_ct = new float[3][];
            new_ct[0] = ct[0];
            new_ct[1] = ct[1];
            new_ct[2] = ct[2];
            ct        = new_ct;
        }

        clrCntrl.setTable(ct);

        return dspMaster;
    }

    /**
     * _more_
     *
     * @param image _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public Range getImageRange(FlatField image)
            throws VisADException, RemoteException {
        DisplayConventions dc         = getDisplayConventions();
        Range[]            range      = GridUtil.fieldMinMax(image);
        Range              imageRange = range[0];
        RealType imageRangeType =
            (((FunctionType) image.getType()).getFlatRange()
            .getRealComponents())[0];
        String canonicalName =
            DataAlias.aliasToCanonical(imageRangeType.getName());
        Range dfltRange = dc.getParamRange(canonicalName, null);

        if (dfltRange == null) {
            imageRange = range[0];
        } else if ((imageRange.getMax() - imageRange.getMin())
                   < (dfltRange.getMax() - dfltRange.getMin())) {}
        else {
            imageRange = dfltRange;
        }
        return imageRange;
    }

    /**
     * _more_
     *
     * @param image _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public ColorTable getColorTable(FlatField image)
            throws VisADException, RemoteException {
        RealType imageRangeType =
            (((FunctionType) image.getType()).getFlatRange()
            .getRealComponents())[0];
        DisplayConventions dc = getDisplayConventions();
        return dc.getParamColorTable(imageRangeType.getName());
    }


    /**
     * _more_
     *
     * @param image _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public MapProjection getDataProjection(FlatField image)
            throws VisADException, RemoteException {
        MapProjection mp = null;
        //- get MapProjection from incoming image.  If none, use default method
        FunctionType     fnc_type  = (FunctionType) image.getType();
        RealTupleType    rtt       = fnc_type.getDomain();
        CoordinateSystem cs        = rtt.getCoordinateSystem();
        Set              domainSet = image.getDomainSet();

        if (cs instanceof visad.CachingCoordinateSystem) {
            cs = ((visad
                .CachingCoordinateSystem) cs).getCachedCoordinateSystem();
        }

        if (cs instanceof MapProjection) {
            return (MapProjection) cs;
        } else if (cs instanceof LongitudeLatitudeCoordinateSystem) {
            Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(image);
            try {
                mp = new LambertAEA(rect);
            } catch (Exception e) {
                System.out.println(" getDataProjection" + e);
            }
            return mp;
        }

        float minLon = Float.NaN;
        float minLat = Float.NaN;
        float delLon = Float.NaN;
        float delLat = Float.NaN;

        if (domainSet instanceof LinearLatLonSet) {
            MathType type0 =
                ((SetType) domainSet.getType()).getDomain().getComponent(0);
            int     latI = RealType.Latitude.equals(type0)
                           ? 0
                           : 1;
            int     lonI = (latI == 1)
                           ? 0
                           : 1;

            float[] min  = ((LinearLatLonSet) domainSet).getLow();
            float[] max  = ((LinearLatLonSet) domainSet).getHi();
            minLon = min[lonI];
            minLat = min[latI];
            delLon = max[lonI] - min[lonI];
            delLat = max[latI] - min[latI];

            try {
                mp = new TrivialMapProjection(
                    RealTupleType.SpatialEarth2DTuple,
                    new Rectangle2D.Float(minLon,
                                          minLat,
                                          delLon,
                                          delLat));
            } catch (Exception e) {
                logException("MultiSpectralControl.getDataProjection", e);
            }

            return mp;
        } else if (domainSet instanceof Gridded2DSet) {
            rtt = ((SetType) domainSet.getType()).getDomain();
            rtt = RealTupleType.SpatialEarth2DTuple;
            if ( !(rtt.equals(RealTupleType.SpatialEarth2DTuple)
                    || rtt.equals(RealTupleType.LatitudeLongitudeTuple))) {
                minLon = -180f;
                minLat = -90f;
                delLon = 360f;
                delLat = 180f;
            } else {
                int     latI = rtt.equals(RealTupleType.SpatialEarth2DTuple)
                               ? 1
                               : 0;
                int     lonI = (latI == 1)
                               ? 0
                               : 1;

                float[] min  = ((Gridded2DSet) domainSet).getLow();
                float[] max  = ((Gridded2DSet) domainSet).getHi();
                minLon = min[lonI];
                minLat = min[latI];
                delLon = max[lonI] - min[lonI];
                delLat = max[latI] - min[latI];
            }
        }

        try {
            mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                                          new Rectangle2D.Float(minLon,
                                                  minLat,
                                                  delLon,
                                                  delLat));
        } catch (Exception e) {
            logException("MultiSpectralControl.getDataProjection", e);
        }

        return mp;
    }

    /**
     * _more_
     *
     * @param mapProjDsp _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void addMapDisplayables(MapProjectionDisplayJ3D mapProjDsp)
            throws VisADException, RemoteException {
        MapLines mapLines = new MapLines("maplines");
        URL mapSource =
            mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        mapLines = new MapLines("maplines");
        mapSource =
            mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        mapLines = new MapLines("maplines");
        mapSource =
            mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getSelectByCurve() {
        return selectByCurve;
    }

    /**
     * _more_
     *
     * @param X_field _more_
     * @param Y_field _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private FlatField resample(FlatField X_field, FlatField Y_field)
            throws VisADException, RemoteException {

        RealTupleType X_domainRef = null;
        RealTupleType Y_domainRef = null;
        float[][]     coords      = null;
        int[]         indexes     = null;
        float[][]     Yvalues     = Y_field.getFloats(false);
        float[][] Xsamples =
            ((SampledSet) X_field.getDomainSet()).getSamples(false);

        CoordinateSystem X_cs = X_field.getDomainCoordinateSystem();
        if (X_cs == null) {
            RealTupleType X_domain =
                ((FunctionType) X_field.getType()).getDomain();
        } else {
            X_domainRef = X_cs.getReference();
        }

        CoordinateSystem Y_cs = Y_field.getDomainCoordinateSystem();
        if (Y_cs == null) {
            RealTupleType Y_domain =
                ((FunctionType) Y_field.getType()).getDomain();
        } else {
            Y_domainRef = Y_cs.getReference();
        }

        if ((X_domainRef != null) && (Y_domainRef != null)) {
            Xsamples = X_cs.toReference(Xsamples);
            coords   = Y_cs.fromReference(Xsamples);
            indexes =
                ((SampledSet) Y_field.getDomainSet()).valueToIndex(coords);
        } else if ((X_domainRef == null) && (Y_domainRef != null)) {
            Xsamples = Y_cs.fromReference(Xsamples);
            indexes =
                ((SampledSet) Y_field.getDomainSet()).valueToIndex(Xsamples);
        } else if ((X_domainRef != null) && (Y_domainRef == null)) {
            Xsamples = X_cs.toReference(Xsamples);
            Gridded2DSet domSet = (Gridded2DSet) Y_field.getDomainSet();

            // TODO this is a hack for the longitude range problem
            float[] hi = domSet.getHi();
            if (hi[0] <= 180f) {
                for (int t = 0; t < Xsamples[0].length; t++) {
                    if (Xsamples[0][t] > 180f) {
                        Xsamples[0][t] -= 360;
                    }
                }
            }

            indexes =
                ((SampledSet) Y_field.getDomainSet()).valueToIndex(Xsamples);
        } else if ((X_domainRef == null) && (Y_domainRef == null)) {
            Gridded2DSet domSet = (Gridded2DSet) Y_field.getDomainSet();
            indexes = domSet.valueToIndex(Xsamples);
        }

        float[][] new_values = new float[1][indexes.length];
        for (int k = 0; k < indexes.length; k++) {
            new_values[0][k] = Float.NaN;
            if (indexes[k] >= 0) {
                new_values[0][k] = Yvalues[0][indexes[k]];
            }
        }

        FunctionType ftype =
            new FunctionType(((FunctionType) X_field.getType()).getDomain(),
                             ((FunctionType) Y_field.getType()).getRange());
        Y_field = new FlatField(ftype, X_field.getDomainSet());
        Y_field.setSamples(new_values);

        return Y_field;
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ScatterDisplayable extends RGBDisplayable {

        /**
         * Construct a ScatterDisplayable 
         *
         * @param name _more_
         * @param rgbRealType _more_
         * @param colorPalette _more_
         * @param alphaflag _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        ScatterDisplayable(String name, RealType rgbRealType,
                           float[][] colorPalette, boolean alphaflag)
                throws VisADException, RemoteException {
            super(name, rgbRealType, colorPalette, alphaflag);
        }
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ImageControl extends DisplayControlImpl {

        /** _more_ */
        HydraRGBDisplayable rgbDisp;

        /** _more_ */
        DisplayConventions dc;

        /** _more_ */
        ColorTableWidget ctw;

        /**
         * Construct a ImageControl 
         *
         * @param rgbDisp _more_
         * @param dc _more_
         */
        ImageControl(HydraRGBDisplayable rgbDisp, DisplayConventions dc) {
            super();
            this.rgbDisp = rgbDisp;
            this.dc      = dc;
        }

        @Override
        public void setRange(Range r) throws VisADException, RemoteException {
            if (r != null) {
                rgbDisp.setRangeForColor(r.getMin(), r.getMax());
            }
        }

        @Override
        public DisplayConventions getDisplayConventions() {
            return dc;
        }

        @Override
        public void setColorTable(ColorTable ct) {
            try {
                ctw.setColorTable(ct);
                ScalarMap colorMap = rgbDisp.getColorMap();
                BaseColorControl clrCntrl =
                    (BaseColorControl) colorMap.getControl();

                // Force incoming color dimension to that of the colorMap
                //
                int       numComps = clrCntrl.getNumberOfComponents();
                float[][] clrTable = ct.getColorTable();
                float[][] newTable = null;
                if (numComps != clrTable.length) {
                    if (numComps < clrTable.length) {
                        newTable = new float[numComps][clrTable[0].length];
                        for (int k = 0; k < numComps; k++) {
                            System.arraycopy(clrTable[k], 0, newTable[k], 0,
                                             newTable[0].length);
                        }
                    } else if (numComps > clrTable.length) {
                        newTable = new float[numComps][clrTable[0].length];
                        for (int k = 0; k < clrTable.length; k++) {
                            System.arraycopy(clrTable[k], 0, newTable[k], 0,
                                             newTable[0].length);
                        }
                        newTable[3] = new float[clrTable[0].length];
                    }
                } else {
                    newTable = new float[numComps][clrTable[0].length];
                    for (int k = 0; k < clrTable.length; k++) {
                        System.arraycopy(clrTable[k], 0, newTable[k], 0,
                                         newTable[0].length);
                    }
                }
                clrCntrl.setTable(newTable);
            } catch (Exception e) {
                LogUtil.logException("Problem changing color table", e);
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ImageCurveSelector extends CellImpl implements DisplayListener {

        /** _more_ */
        boolean init = false;

        /** _more_ */
        CurveDrawer curveDraw;

        /** _more_ */
        DisplayMaster dspMaster;

        /** _more_ */
        Gridded2DSet domainSet;

        /** _more_ */
        CoordinateSystem cs;

        /** _more_ */
        int domainLen_0;

        /** _more_ */
        int domainLen_1;

        /** _more_ */
        ImageCurveSelector other;

        /** _more_ */
        UnionSet last_uSet = null;

        /** _more_ */
        boolean imageLatLon = false;

        /** _more_ */
        boolean active = true;

        /** _more_ */
        float maskVal;

        /** _more_ */
        LineDrawing lastCurve;

        /** _more_ */
        StatsTable myTable = null;

        /** _more_ */
        int myTableIndex = 0;

        /**
         * Construct a ImageCurveSelector 
         *
         * @param curveDraw _more_
         * @param image _more_
         * @param master _more_
         * @param color _more_
         * @param maskVal _more_
         * @param mst _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        ImageCurveSelector(CurveDrawer curveDraw, FlatField image,
                           DisplayMaster master, Color color, float maskVal,
                           StatsTable mst)
                throws VisADException, RemoteException {
            this.curveDraw = curveDraw;
            this.maskVal   = maskVal;
            this.myTable   = mst;
            myTableIndex   = 0;
            if (color == Color.magenta) {
                myTableIndex = 1;
            }
            if (color == Color.green) {
                myTableIndex = 2;
            }
            if (color == Color.blue) {
                myTableIndex = 3;
            }
            dspMaster = master;
            dspMaster.addDisplayListener(this);
            domainSet = (Gridded2DSet) image.getDomainSet();
            int[] lens = domainSet.getLengths();
            domainLen_0 = lens[0];
            domainLen_1 = lens[1];
            cs = ((FunctionType) image.getType()).getDomain()
            .getCoordinateSystem();
            RealTupleType reference = null;
            if (cs != null) {
                reference = cs.getReference();
            } else {
                reference = ((SetType) domainSet.getType()).getDomain();
            }
            RealType[] rtypes = reference.getRealComponents();
            if (rtypes[0].equals(RealType.Latitude)) {
                imageLatLon = true;
            }
            lastCurve = new LineDrawing("lastCurve");
            lastCurve.setColor(color);
            lastCurve.setLineWidth(2);
            master.addDisplayable(lastCurve);
        }

        @Override
        public void displayChanged(DisplayEvent de)
                throws VisADException, RemoteException {

            if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
                UnionSet uSet = curveDraw.getCurves();
                if (uSet == last_uSet) {
                    return;
                }
                SampledSet[] sets  = uSet.getSets();
                int          s_idx = sets.length - 1;
                float[][]    crv;

                if (cs != null) {
                    crv = sets[s_idx].getSamples();
                    if (imageLatLon) {
                        float[] tmp = crv[0];
                        crv[0] = crv[1];
                        crv[1] = tmp;
                    }
                    crv = cs.fromReference(crv);
                    crv = domainSet.valueToGrid(crv);
                } else {
                    crv = sets[s_idx].getSamples();
                    crv = domainSet.valueToGrid(crv);
                }

                float[][] onImage = new float[2][crv[0].length];
                int       cnt     = 0;
                for (int i = 0; i < crv[0].length; i++) {
                    if (((crv[0][i] >= 0) && (crv[0][i] <= domainLen_0))
                            && ((crv[1][i] >= 0)
                                && (crv[1][i] <= domainLen_1))) {
                        onImage[0][cnt] = crv[0][i];
                        onImage[1][cnt] = crv[1][i];
                        cnt++;
                    }
                }
                uSet      = new UnionSet(new SampledSet[] { sets[s_idx] });
                last_uSet = uSet;
                lastCurve.setData(last_uSet);
                curveDraw.setCurves(uSet);
                other.updateCurve(sets[s_idx]);

                if (cnt == 0) {
                    return;
                }

                float[][] tmp = new float[2][cnt];
                System.arraycopy(onImage[0], 0, tmp[0], 0, cnt);
                System.arraycopy(onImage[1], 0, tmp[1], 0, cnt);
                onImage = tmp;

                float[] minmaxvals = minmax(onImage[0]);
                int     low_0      = Math.round(minmaxvals[0]);
                int     hi_0       = Math.round(minmaxvals[1]);
                minmaxvals = minmax(onImage[1]);
                int low_1 = Math.round(minmaxvals[0]);
                int hi_1  = Math.round(minmaxvals[1]);

                int len_0 = (hi_0 - low_0) + 1;
                int len_1 = (hi_1 - low_1) + 1;
                int len   = len_0 * len_1;

                tmp = new float[3][len];
                int[] tmpsel     = new int[len];

                int   num_inside = 0;
                for (int j = 0; j < len_1; j++) {
                    for (int i = 0; i < len_0; i++) {
                        int   idx = (j + low_1) * domainLen_0 + (i + low_0);
                        float x   = (float) (i + low_0);
                        float y   = (float) (j + low_1);
                        if (DelaunayCustom.inside(crv, x, y)) {
                            tmp[0][num_inside] = scatterFieldRange[0][idx];
                            tmp[1][num_inside] = scatterFieldRange[1][idx];
                            tmp[2][num_inside] = maskVal;
                            tmpsel[num_inside] = idx;
                            num_inside++;
                        }
                    }
                }
                len = num_inside;
                float[][] markScatter = new float[3][len];
                System.arraycopy(tmp[0], 0, markScatter[0], 0, len);
                System.arraycopy(tmp[1], 0, markScatter[1], 0, len);
                System.arraycopy(tmp[2], 0, markScatter[2], 0, len);


                int last_len = 0;
                float[][] lastMark =
                    ((FlatField) scatterMarkDsp.getData()).getFloats(false);
                tmp = new float[3][lastMark[0].length];
                for (int k = 0; k < lastMark[0].length; k++) {
                    if (lastMark[2][k] != maskVal) {
                        tmp[0][last_len] = lastMark[0][k];
                        tmp[1][last_len] = lastMark[1][k];
                        tmp[2][last_len] = lastMark[2][k];
                        last_len++;
                    }
                }

                float[][] newMarkScatter = new float[3][len + last_len];
                System.arraycopy(tmp[0], 0, newMarkScatter[0], 0, last_len);
                System.arraycopy(tmp[1], 0, newMarkScatter[1], 0, last_len);
                System.arraycopy(tmp[2], 0, newMarkScatter[2], 0, last_len);
                System.arraycopy(markScatter[0], 0, newMarkScatter[0],
                                 last_len, len);
                System.arraycopy(markScatter[1], 0, newMarkScatter[1],
                                 last_len, len);
                System.arraycopy(markScatter[2], 0, newMarkScatter[2],
                                 last_len, len);

                Integer1DSet dset = new Integer1DSet(len + last_len);
                FlatField scatterFieldMark =
                    new FlatField(new FunctionType(RealType.Generic,
                                                   new RealTupleType(
                                                       RealType.XAxis,
                                                               RealType.YAxis,
                                                               RealType.getRealType(
                                                                   "mask"))), dset);

                scatterFieldMark.setSamples(newMarkScatter, false);
                scatterMarkDsp.setData(scatterFieldMark);

                if (myTable != null) {
                    int[] selected = new int[len];
                    System.arraycopy(tmpsel, 0, selected, 0, len);
                    total_area = JPythonMethods.computeSum(Area_field,
                            selected);
                    myTable.setPoints(markScatter, len, myTableIndex,
                                      total_area);
                }

            }

        }

        /**
         * _more_
         *
         * @param active _more_
         */
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void reset() throws VisADException, RemoteException {

            float[][] lastMark =
                ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            int       cnt = 0;
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][cnt] = lastMark[0][k];
                    tmp[1][cnt] = lastMark[1][k];
                    tmp[2][cnt] = lastMark[2][k];
                    cnt++;
                }
            }

            RealTupleType type =
                ((SetType) curveDraw.getCurves().getType()).getDomain();
            curveDraw.setCurves(new UnionSet(new Gridded2DSet[] {
                new Gridded2DSet(type,
                                 new float[][] {
                { 0.0f }, { 0.0f }
            }, 1) }));

            lastCurve.setData(new UnionSet(new Gridded2DSet[] {
                new Gridded2DSet(type,
                                 new float[][] {
                { 0.0f }, { 0.0f }
            }, 1) }));

            FlatField scatterFieldMark = null;
            if (cnt == 0) {
                Integer1DSet dset = new Integer1DSet(2);
                scatterFieldMark =
                    new FlatField(new FunctionType(RealType.Generic,
                            new RealTupleType(RealType.XAxis,
                                    RealType.YAxis,
                                    RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][2];
                for (int k = 0; k < 2; k++) {
                    markScatter[0][k] = scatterFieldRange[0][k];
                    markScatter[1][k] = scatterFieldRange[1][k];
                    markScatter[2][k] = 0;
                }
                scatterFieldMark.setSamples(markScatter, false);
            } else {
                Integer1DSet dset = new Integer1DSet(cnt);
                scatterFieldMark =
                    new FlatField(new FunctionType(RealType.Generic,
                            new RealTupleType(RealType.XAxis,
                                    RealType.YAxis,
                                    RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][cnt];
                for (int k = 0; k < cnt; k++) {
                    markScatter[0][k] = tmp[0][k];
                    markScatter[1][k] = tmp[1][k];
                    markScatter[2][k] = tmp[2][k];
                }
                scatterFieldMark.setSamples(markScatter, false);
            }

            scatterMarkDsp.setData(scatterFieldMark);
        }

        /**
         * _more_
         *
         * @param set _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void updateCurve(SampledSet set)
                throws VisADException, RemoteException {
            last_uSet = new UnionSet(new SampledSet[] { set });
            curveDraw.setCurves(last_uSet);
            lastCurve.setData(last_uSet);
        }

        /**
         * _more_
         *
         * @param other _more_
         */
        public void setOther(ImageCurveSelector other) {
            this.other = other;
        }

        @Override
        public void doAction() throws VisADException, RemoteException {
            if ( !init) {
                init = true;
                return;
            }
        }

        /**
         * _more_
         *
         * @param visible _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setVisible(boolean visible)
                throws VisADException, RemoteException {
            curveDraw.setVisible(visible);
        }

    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ImageBoxSelector extends CellImpl {

        /** _more_ */
        boolean init = false;

        /** _more_ */
        boolean active = true;

        /** _more_ */
        SubsetRubberBandBox subsetBox;

        /** _more_ */
        Set imageDomain;

        /** _more_ */
        int domainLen_0;

        /** _more_ */
        LineDrawing lastBox;

        /** _more_ */
        ImageBoxSelector other;

        /** _more_ */
        float maskVal;

        /** _more_ */
        boolean earthCoordDomain = false;

        /** _more_ */
        StatsTable myTable = null;

        /** _more_ */
        int myTableIndex = 0;

        /**
         * Construct a ImageBoxSelector 
         *
         * @param subsetBox _more_
         * @param imageDomain _more_
         * @param master _more_
         * @param color _more_
         * @param maskVal _more_
         * @param mst _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        ImageBoxSelector(SubsetRubberBandBox subsetBox, Set imageDomain,
                         DisplayMaster master, Color color, float maskVal,
                         StatsTable mst)
                throws VisADException, RemoteException {
            super();
            this.myTable = mst;
            myTableIndex = 0;
            if (color == Color.magenta) {
                myTableIndex = 1;
            }
            if (color == Color.green) {
                myTableIndex = 2;
            }
            if (color == Color.blue) {
                myTableIndex = 3;
            }
            this.subsetBox   = subsetBox;
            this.imageDomain = imageDomain;
            int[] lens = ((Gridded2DSet) imageDomain).getLengths();
            this.maskVal = maskVal;
            domainLen_0  = lens[0];
            lastBox      = new LineDrawing("last_box");
            lastBox.setColor(color);
            master.addDisplayable(lastBox);
            subsetBox.addAction(this);
            master.addDisplayable(subsetBox);
            RealTupleType rtt = ((SetType) imageDomain.getType()).getDomain();
            if (rtt.equals(RealTupleType.SpatialEarth2DTuple)
                    || rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
                earthCoordDomain = true;
            }
        }

        @Override
        public void doAction() throws VisADException, RemoteException {
            if ( !init) {
                init = true;
                return;
            }

            if ( !active) {
                return;
            }

            Gridded2DSet set     = subsetBox.getBounds();
            float[][]    corners = set.getSamples(false);
            float[][]    coords  = corners;
            if (corners == null) {
                return;
            }

            if ((imageDomain instanceof Linear2DSet) || !earthCoordDomain) {
                coords = ((Gridded2DSet) imageDomain).valueToGrid(corners);
            }

            float[]   coords_0    = coords[0];
            float[]   coords_1    = coords[1];

            int       low_0 = Math.round(Math.min(coords_0[0], coords_0[1]));
            int       low_1 = Math.round(Math.min(coords_1[0], coords_1[1]));
            int       hi_0 = Math.round(Math.max(coords_0[0], coords_0[1]));
            int       hi_1 = Math.round(Math.max(coords_1[0], coords_1[1]));

            int       len_0       = (hi_0 - low_0) + 1;
            int       len_1       = (hi_1 - low_1) + 1;
            int       len         = len_0 * len_1;

            float[][] markScatter = new float[3][len];
            int[]     selected    = new int[len];

            for (int j = 0; j < len_1; j++) {
                for (int i = 0; i < len_0; i++) {
                    int idx = (j + low_1) * domainLen_0 + (i + low_0);
                    int k   = j * len_0 + i;
                    markScatter[0][k] = scatterFieldRange[0][idx];
                    markScatter[1][k] = scatterFieldRange[1][idx];
                    markScatter[2][k] = maskVal;
                    selected[k]       = idx;
                }
            }

            int last_len = 0;
            float[][] lastMark =
                ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][last_len] = lastMark[0][k];
                    tmp[1][last_len] = lastMark[1][k];
                    tmp[2][last_len] = lastMark[2][k];
                    last_len++;
                }
            }

            float[][] newMarkScatter = new float[3][len + last_len];
            System.arraycopy(tmp[0], 0, newMarkScatter[0], 0, last_len);
            System.arraycopy(tmp[1], 0, newMarkScatter[1], 0, last_len);
            System.arraycopy(tmp[2], 0, newMarkScatter[2], 0, last_len);
            System.arraycopy(markScatter[0], 0, newMarkScatter[0], last_len,
                             len);
            System.arraycopy(markScatter[1], 0, newMarkScatter[1], last_len,
                             len);
            System.arraycopy(markScatter[2], 0, newMarkScatter[2], last_len,
                             len);

            Integer1DSet dset = new Integer1DSet(len + last_len);
            FlatField scatterFieldMark =
                new FlatField(new FunctionType(RealType.Generic,
                                               new RealTupleType(
                                                   RealType.XAxis,
                                                           RealType.YAxis,
                                                           RealType.getRealType(
                                                               "mask"))), dset);

            scatterFieldMark.setSamples(newMarkScatter, false);
            scatterMarkDsp.setData(scatterFieldMark);

            if (myTable != null) {
                total_area = JPythonMethods.computeSum(Area_field, selected);
                myTable.setPoints(markScatter, len, myTableIndex, total_area);
            }

            updateBox();
        }

        /**
         * _more_
         *
         * @param active _more_
         */
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * _more_
         *
         * @param visible _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setVisible(boolean visible)
                throws VisADException, RemoteException {
            subsetBox.setVisible(visible);
            if (visible) {
                lastBox.setVisible(visible);
            }
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void reset() throws VisADException, RemoteException {
            Gridded2DSet set2D =
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                { 0 }, { 0 }
            }, 1);
            lastBox.setVisible(false);
            lastBox.setData(set2D);

            float[][] lastMark =
                ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            int       cnt = 0;
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][cnt] = lastMark[0][k];
                    tmp[1][cnt] = lastMark[1][k];
                    tmp[2][cnt] = lastMark[2][k];
                    cnt++;
                }
            }

            FlatField scatterFieldMark;
            if (cnt == 2) {
                Integer1DSet dset = new Integer1DSet(2);
                scatterFieldMark =
                    new FlatField(new FunctionType(RealType.Generic,
                            new RealTupleType(RealType.XAxis,
                                    RealType.YAxis,
                                    RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][2];
                for (int k = 0; k < 2; k++) {
                    markScatter[0][k] = scatterFieldRange[0][k];
                    markScatter[1][k] = scatterFieldRange[1][k];
                    markScatter[2][k] = 0;
                }
                scatterFieldMark.setSamples(markScatter, false);
            } else {
                Integer1DSet dset = new Integer1DSet(cnt);
                scatterFieldMark =
                    new FlatField(new FunctionType(RealType.Generic,
                            new RealTupleType(RealType.XAxis,
                                    RealType.YAxis,
                                    RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][cnt];
                for (int k = 0; k < cnt; k++) {
                    markScatter[0][k] = tmp[0][k];
                    markScatter[1][k] = tmp[1][k];
                    markScatter[2][k] = tmp[2][k];
                }
                scatterFieldMark.setSamples(markScatter, false);
            }

            scatterMarkDsp.setData(scatterFieldMark);
        }

        /**
         * _more_
         *
         * @param other _more_
         */
        public void setOther(ImageBoxSelector other) {
            this.other = other;
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void updateBox() throws VisADException, RemoteException {
            Gridded3DSet set3D   = subsetBox.getLastBox();
            float[][]    samples = set3D.getSamples(false);
            Gridded2DSet set2D =
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                samples[0], samples[1]
            }, samples[0].length);
            lastBox.setData(set2D);
            other.updateBox(set2D);
        }

        /**
         * _more_
         *
         * @param set2D _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void updateBox(Gridded2DSet set2D)
                throws VisADException, RemoteException {
            lastBox.setData(set2D);
        }

    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ScatterBoxSelector extends CellImpl {

        /** _more_ */
        boolean init = false;

        /** _more_ */
        double[] x_coords = new double[2];

        /** _more_ */
        double[] y_coords = new double[2];

        /** _more_ */
        RubberBandBox rbb;

        /** _more_ */
        LineDrawing selectBox;

        /** _more_ */
        boolean active = true;

        /** _more_ */
        float maskVal = 0;

        /**
         * Construct a ScatterBoxSelector 
         *
         * @param master _more_
         * @param color _more_
         * @param maskVal _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        ScatterBoxSelector(DisplayMaster master, Color color, float maskVal)
                throws VisADException, RemoteException {
            selectBox = new LineDrawing("select");
            selectBox.setColor(color);

            rbb = new RubberBandBox(RealType.XAxis, RealType.YAxis, 1);
            rbb.setColor(color);
            rbb.addAction(this);

            master.addDisplayable(rbb);
            master.addDisplayable(selectBox);
            this.maskVal = maskVal;
        }


        @Override
        public void doAction() throws VisADException, RemoteException {
            if ( !init) {
                init = true;
                return;
            }

            if ( !active) {
                return;
            }

            Gridded2DSet set = rbb.getBounds();
            float[]      low = set.getLow();
            float[]      hi  = set.getHi();
            x_coords[0] = low[0];
            x_coords[1] = hi[0];
            y_coords[0] = low[1];
            y_coords[1] = hi[1];

            SampledSet[] sets = new SampledSet[4];
            sets[0] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                       new float[][] {
                { low[0], hi[0] }, { low[1], low[1] }
            }, 2);
            sets[1] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                       new float[][] {
                { hi[0], hi[0] }, { low[1], hi[1] }
            }, 2);
            sets[2] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                       new float[][] {
                { hi[0], low[0] }, { hi[1], hi[1] }
            }, 2);
            sets[3] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                       new float[][] {
                { low[0], low[0] }, { hi[1], low[1] }
            }, 2);
            UnionSet uset = new UnionSet(sets);
            selectBox.setData(uset);

            try {
                histoField.markMaskFieldByRange(x_coords, y_coords, maskVal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * _more_
         *
         * @param visible _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setVisible(boolean visible)
                throws VisADException, RemoteException {
            rbb.setVisible(visible);
            if (visible) {
                selectBox.setVisible(visible);
            }
        }

        /**
         * _more_
         *
         * @param active _more_
         */
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * _more_
         *
         * @throws Exception _more_
         */
        public void reset() throws Exception {
            if ( !active) {
                return;
            }
            selectBox.setVisible(false);
            selectBox.setData(
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                { 0f, 0f }, { 0f, 0f }
            },
                                 2));
            histoField.resetMaskField(maskVal);
        }

    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class ScatterCurveSelector extends CellImpl implements DisplayListener {

        /** _more_ */
        CurveDrawer curveDraw;

        /** _more_ */
        boolean init = false;

        /** _more_ */
        UnionSet last_uSet = null;

        /** _more_ */
        boolean active = true;

        /** _more_ */
        float maskVal = 0;

        /** _more_ */
        LineDrawing selectCurve;

        /**
         * Construct a ScatterCurveSelector 
         *
         * @param master _more_
         * @param color _more_
         * @param maskVal _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        ScatterCurveSelector(DisplayMaster master, Color color, float maskVal)
                throws VisADException, RemoteException {
            curveDraw = new CurveDrawer(RealType.XAxis, RealType.YAxis, 1);
            curveDraw.setColor(color);
            curveDraw.setLineWidth(2);
            curveDraw.setData(new UnionSet(new Gridded2DSet[] {
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                { scatterFieldRange[0][0] }, { scatterFieldRange[1][0] }
            }, 1) }));

            selectCurve = new LineDrawing("select");
            selectCurve.setColor(color);
            selectCurve.setLineWidth(2);
            master.addDisplayable(curveDraw);
            master.addDisplayable(selectCurve);
            this.maskVal = maskVal;

            curveDraw.addAction(this);
            master.addDisplayListener(this);
        }

        @Override
        public void displayChanged(DisplayEvent de)
                throws VisADException, RemoteException {
            if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
                UnionSet uSet = curveDraw.getCurves();
                if (uSet == last_uSet) {
                    return;
                }
                SampledSet[] sets  = uSet.getSets();
                int          s_idx = sets.length - 1;
                float[][]    crv;

                crv       = sets[s_idx].getSamples();
                last_uSet = new UnionSet(new SampledSet[] { sets[s_idx] });
                curveDraw.setCurves(last_uSet);
                selectCurve.setData(last_uSet);

                try {
                    histoField.clearMaskField(maskVal);
                    histoField.markMaskFieldByCurve(crv, maskVal);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void doAction() throws VisADException, RemoteException {
            if ( !init) {
                init = true;
                return;
            }
        }

        /**
         * _more_
         *
         * @param visible _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setVisible(boolean visible)
                throws VisADException, RemoteException {
            curveDraw.setVisible(visible);
        }

        /**
         * _more_
         *
         * @param active _more_
         */
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * _more_
         *
         * @throws Exception _more_
         */
        public void reset() throws Exception {
            if ( !active) {
                return;
            }
            curveDraw.setData(new UnionSet(new Gridded2DSet[] {
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                { scatterFieldRange[0][0] }, { scatterFieldRange[1][0] }
            }, 1) }));
            selectCurve.setData(new UnionSet(new Gridded2DSet[] {
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
                { scatterFieldRange[0][0] }, { scatterFieldRange[1][0] }
            }, 1) }));
            histoField.resetMaskField(maskVal);
        }

    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Feb 6, '18
     * @author         Enter your name here...    
     */
    private class BoxCurveSwitch implements ActionListener {

        /**
         * Construct a BoxCurveSwitch 
         */
        public BoxCurveSwitch() {}

        @Override
        public void actionPerformed(ActionEvent ae) {
            String cmd = ae.getActionCommand();
            try {
                if (cmd.equals("Box")) {
                    selectByCurve = false;
                } else if (cmd.equals("Curve")) {
                    selectByCurve = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public static float[] minmax(float[] values) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int k = 0; k < values.length; k++) {
            float val = values[k];
            if ((val == val)
                    && (val < Float.POSITIVE_INFINITY)
                    && (val > Float.NEGATIVE_INFINITY)) {
                if (val < min) {
                    min = val;
                }
                if (val > max) {
                    max = val;
                }
            }
        }
        return new float[] { min, max };
    }

    /**
     * _more_
     *
     * @param field _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean getIsLatLon(FlatField field)
            throws VisADException, RemoteException {
        boolean       isLL     = false;
        FunctionType  fnc_type = (FunctionType) field.getType();
        RealTupleType rtt      = fnc_type.getDomain();
        if (rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
            isLL = true;
        } else if ( !rtt.equals(RealTupleType.SpatialEarth2DTuple)) {
            rtt = fnc_type.getDomain().getCoordinateSystem().getReference();
            if (rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
                isLL = true;
            }
        }
        return isLL;
    }

}
