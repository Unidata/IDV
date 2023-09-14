/*
 * Copyright 1997-2023 Unidata Program Center/University Corporation for
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

import java.awt.*;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.DisplayMaster;

import ucar.visad.display.ImageRGBDisplayable;
import visad.BaseColorControl;
import visad.CoordinateSystem;
import visad.Data;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.VisADException;
import visad.georef.MapProjection;




public class RGBCompositeControl extends DisplayControlImpl {

    public final static String FORMULA_IN_PROGRESS_FLAG = "Formula_Active";

    /** Displayable for the data */
    private ImageRGBDisplayable imageDisplay;

    private DisplayMaster displayMaster;

    private ScalarMap redMap = null;
    private ScalarMap grnMap = null;
    private ScalarMap bluMap = null;

    float[][] redTable = null;
    float[][] grnTable = null;
    float[][] bluTable = null;

    final private double[] redRange = { Double.NaN, Double.NaN };
    final private double[] grnRange = { Double.NaN, Double.NaN };
    final private double[] bluRange = { Double.NaN, Double.NaN };

    final double[] initRedRange = { Double.NaN, Double.NaN };
    final double[] initGrnRange = { Double.NaN, Double.NaN };
    final double[] initBluRange = { Double.NaN, Double.NaN };

    private FieldImpl imageField = null;
    private MapProjection mapProjection = null;

    private double gamma = 1.0;

    private double redGamma = 1.0;
    private double grnGamma = 1.0;
    private double bluGamma = 1.0;

    private final JTextField gammaTxtFld =
            new JTextField(Float.toString(1.0f), 4);
    private final JTextField redGammaTxtFld =
            new JTextField(Float.toString(1.0f), 4);
    private final JTextField grnGammaTxtFld =
            new JTextField(Float.toString(1.0f), 4);
    private final JTextField bluGammaTxtFld =
            new JTextField(Float.toString(1.0f), 4);

    private final JTextField redLowTxtFld =
            new JTextField(Float.toString(1.0f), 10);
    private final JTextField redHighTxtFld =
            new JTextField(Float.toString(1.0f), 10);
    private final JTextField grnLowTxtFld =
            new JTextField(Float.toString(1.0f), 10);
    private final JTextField grnHighTxtFld =
            new JTextField(Float.toString(1.0f), 10);
    private final JTextField bluLowTxtFld =
            new JTextField(Float.toString(1.0f), 10);
    private final JTextField bluHighTxtFld =
            new JTextField(Float.toString(1.0f), 10);

    public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {

        displayMaster = getViewManager().getMaster();
        DataSelection dataSelection = getDataSelection();

        // TJJ Jul 2014
        // by sharing a property via the active View Manager, we can signal all three
        // preview windows they are part of an in-progress RGB Composite. If so, it
        // appears we need to use a shared HydraContext so our geographic coverage
        // subset applies across channels.

        Hashtable ht = getIdv().getViewManager().getProperties();
        ht.put(FORMULA_IN_PROGRESS_FLAG, true);

        imageField = (FieldImpl) dataChoice.getData(dataSelection);

        imageDisplay = new ImageRGBDisplayable("rgb composite", null, false, imageField);

        ht.put(FORMULA_IN_PROGRESS_FLAG, false);

        Iterator iter = imageDisplay.getScalarMapSet().iterator();
        while (iter.hasNext()) {
            ScalarMap map = (ScalarMap) iter.next();
            if (map.getScalarName().startsWith("redimage")) {
                redMap = map;
            }
            if (map.getScalarName().startsWith("greenimage")) {
                grnMap = map;
            }
            if (map.getScalarName().startsWith("blueimage")) {
                bluMap = map;
            }
        }

        if (checkRange()) { //- from unpersistence if true, initialize gui, ScalarMaps
            double[] redRange = getRedRange();
            double[] grnRange = getGrnRange();
            double[] bluRange = getBluRange();

            initRedRange[0] = redRange[0];
            initRedRange[1] = redRange[1];
            initGrnRange[0] = grnRange[0];
            initGrnRange[1] = grnRange[1];
            initBluRange[0] = bluRange[0];
            initBluRange[1] = bluRange[1];

            redLowTxtFld.setText(Float.toString((float)redRange[0]));
            redHighTxtFld.setText(Float.toString((float)redRange[1]));
            grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
            grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
            bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
            bluHighTxtFld.setText(Float.toString((float)bluRange[1]));

            gammaTxtFld.setText(Float.toString((float)gamma));
            redGammaTxtFld.setText(Float.toString((float)redGamma));
            grnGammaTxtFld.setText(Float.toString((float)grnGamma));
            bluGammaTxtFld.setText(Float.toString((float)bluGamma));

            redMap.setRange(redRange[0], redRange[1]);
            grnMap.setRange(grnRange[0], grnRange[1]);
            bluMap.setRange(bluRange[0], bluRange[1]);
        } else {
            redMap.resetAutoScale();
            grnMap.resetAutoScale();
            bluMap.resetAutoScale();

            redMap.addScalarMapListener(new ColorMapListener(redMap, initRedRange, redRange, redLowTxtFld, redHighTxtFld));
            grnMap.addScalarMapListener(new ColorMapListener(grnMap, initGrnRange, grnRange, grnLowTxtFld, grnHighTxtFld));
            bluMap.addScalarMapListener(new ColorMapListener(bluMap, initBluRange, bluRange, bluLowTxtFld, bluHighTxtFld));
        }

        setShowInDisplayList(true);

        addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_ZPOSITION);

        return true;
    }

    public void initDone() {
        while (true) {
            if (null != redMap.getControl()) {
                redTable = ((BaseColorControl) redMap.getControl()).getTable();
                break;
            }
        }
        while (true) {
            if (null != grnMap.getControl()) {
                grnTable = ((BaseColorControl) grnMap.getControl()).getTable();
                break;
            }
        }

        while (true) {
            if (null != bluMap.getControl()) {
                bluTable = ((BaseColorControl) bluMap.getControl()).getTable();
                break;
            }
        }

        float[][] newRedTbl = getZeroOutArray(redTable);
        float[][] newGrnTbl = getZeroOutArray(grnTable);
        float[][] newBluTbl = getZeroOutArray(bluTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], redGamma);
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], grnGamma);
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], bluGamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            imageDisplay.loadData(imageField);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    public MapProjection getDataProjection() {
        CoordinateSystem cs = null;
        try {
            if (imageField instanceof FlatField) {
                cs = ((FunctionType)imageField.getType()).getDomain().getCoordinateSystem();
            }
            else if (imageField instanceof FieldImpl) {
                Data dat = imageField.getSample(0, false);
                if (dat instanceof FlatField) {
                    FlatField img = (FlatField) dat;
                    cs = ((FunctionType)img.getType()).getDomain().getCoordinateSystem();
                }
            }
        }
        catch (Exception ex) {
            LogUtil.logException("problem accessing data", ex);
        }

        if (cs instanceof MapProjection) {
            mapProjection = (MapProjection)cs;
        }

        return mapProjection;
    }

    boolean checkRange() {
        return !(Double.isNaN(redRange[0]) || Double.isNaN(grnRange[0]) || Double.isNaN(bluRange[0]));
    }

    private void updateRedRange(double lo, double hi) {
        redRange[0] = lo;
        redRange[1] = hi;
        redHighTxtFld.setText(Float.toString((float)hi));
        redLowTxtFld.setText(Float.toString((float)lo));
        try {
            redMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("redMap.setRange", ex);
        }
    }

    public void setRedRange(double[] range) {
        redRange[0] = range[0];
        redRange[1] = range[1];
    }

    public double[] getRedRange() {
        return new double[] {redRange[0], redRange[1]};
    }

    private void updateGrnRange(double lo, double hi) {
        grnRange[0] = lo;
        grnRange[1] = hi;
        grnHighTxtFld.setText(Float.toString((float)hi));
        grnLowTxtFld.setText(Float.toString((float)lo));
        try {
            grnMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("grnMap.setRange", ex);
        }
    }

    public void setGrnRange(double[] range) {
        grnRange[0] = range[0];
        grnRange[1] = range[1];
    }

    public double[] getGrnRange() {
        return new double[] {grnRange[0], grnRange[1]};
    }

    private void updateBluRange(double lo, double hi) {
        bluRange[0] = lo;
        bluRange[1] = hi;
        bluHighTxtFld.setText(Float.toString((float)hi));
        bluLowTxtFld.setText(Float.toString((float)lo));
        try {
            bluMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("bluMap.setRange", ex);
        }
    }

    public void setBluRange(double[] range) {
        bluRange[0] = range[0];
        bluRange[1] = range[1];
    }

    public double[] getBluRange() {
        return new double[] {bluRange[0], bluRange[1]};
    }

    public void setRedGamma(double gamma) {
        redGamma = gamma;
    }

    public double getRedGamma() {
        return redGamma;
    }

    public void setGrnGamma(double gamma) {
        grnGamma = gamma;
    }

    public double getGrnGamma() {
        return grnGamma;
    }

    public void setBluGamma(double gamma) {
        bluGamma = gamma;
    }

    public double getBluGamma() {
        return bluGamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getGamma() {
        return gamma;
    }

    private void updateGamma(double gamma) {
        setGamma(gamma);
        setRedGamma(gamma);
        setGrnGamma(gamma);
        setBluGamma(gamma);
        redGammaTxtFld.setText(Float.toString((float)gamma));
        grnGammaTxtFld.setText(Float.toString((float)gamma));
        bluGammaTxtFld.setText(Float.toString((float)gamma));

        float[][] newRedTbl = getZeroOutArray(redTable);
        float[][] newGrnTbl = getZeroOutArray(grnTable);
        float[][] newBluTbl = getZeroOutArray(bluTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
        }
        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateRedGamma(double gamma) {
        setRedGamma(gamma);

        float[][] newRedTbl = getZeroOutArray(redTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateGrnGamma(double gamma) {
        setGrnGamma(gamma);

        float[][] newGrnTbl = getZeroOutArray(grnTable);
        for (int k=0; k<grnTable[0].length; k++) {
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateBluGamma(double gamma) {
        setBluGamma(gamma);

        float[][] newBluTbl = getZeroOutArray(bluTable);
        for (int k=0; k<bluTable[0].length; k++) {
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    public float[][] getZeroOutArray(float[][] array) {
        float[][] newArray = new float[array.length][array[0].length];
        for (int i=0; i<newArray.length; i++) {
            for (int j=0; j<newArray[0].length; j++) {
                newArray[i][j] = 0.0f;
            }
        }
        return newArray;
    }

    protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable("image");
    }

    public Container doMakeContents() {

        JButton allGammaButton = new JButton("Apply to All Gamma Fields");
        allGammaButton.addActionListener(e -> {
            String tmp = gammaTxtFld.getText().trim();
            updateGamma(Double.valueOf(tmp));
        });

        gammaTxtFld.addActionListener(e -> {
            String tmp = gammaTxtFld.getText().trim();
            updateGamma(Double.valueOf(tmp));
        });

        redLowTxtFld.addActionListener(e -> {
            String tmp = redLowTxtFld.getText().trim();
            updateRedRange(Double.valueOf(tmp), redRange[1]);
        });

        redHighTxtFld.addActionListener(e -> {
            String tmp = redHighTxtFld.getText().trim();
            updateRedRange(redRange[0], Double.valueOf(tmp));
        });

        redGammaTxtFld.addActionListener(e -> {
            String tmp = redGammaTxtFld.getText().trim();
            updateRedGamma(Double.valueOf(tmp));
        });

        JButton redReset = new JButton("Reset");
        redReset.addActionListener(e -> {
            updateRedRange(initRedRange[0], initRedRange[1]);
            redRange[0] = initRedRange[0];
            redRange[1] = initRedRange[1];
            redLowTxtFld.setText(Float.toString((float)redRange[0]));
            redHighTxtFld.setText(Float.toString((float)redRange[1]));
            updateRedGamma(1.0);
            redGammaTxtFld.setText("1.0");
        });

        grnLowTxtFld.addActionListener(e -> {
            String tmp = grnLowTxtFld.getText().trim();
            updateGrnRange(Double.valueOf(tmp), grnRange[1]);
        });

        grnHighTxtFld.addActionListener(e -> {
            String tmp = grnHighTxtFld.getText().trim();
            updateGrnRange(grnRange[0], Double.valueOf(tmp));
        });

        grnGammaTxtFld.addActionListener(e -> {
            String tmp = grnGammaTxtFld.getText().trim();
            updateGrnGamma(Double.valueOf(tmp));
        });

        JButton grnReset = new JButton("Reset");
        grnReset.addActionListener(e -> {
            updateGrnRange(initGrnRange[0], initGrnRange[1]);
            grnRange[0] = initGrnRange[0];
            grnRange[1] = initGrnRange[1];
            grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
            grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
            updateGrnGamma(1.0);
            grnGammaTxtFld.setText("1.0");
        });

        bluLowTxtFld.addActionListener(e -> {
            String tmp = bluLowTxtFld.getText().trim();
            updateBluRange(Double.valueOf(tmp), bluRange[1]);
        });

        bluHighTxtFld.addActionListener(e -> {
            String tmp = bluHighTxtFld.getText().trim();
            updateBluRange(bluRange[0], Double.valueOf(tmp));
        });

        bluGammaTxtFld.addActionListener(e -> {
            String tmp = bluGammaTxtFld.getText().trim();
            updateBluGamma(Double.valueOf(tmp));
        });

        JButton bluReset = new JButton("Reset");
        bluReset.addActionListener(e -> {
            updateBluRange(initBluRange[0], initBluRange[1]);
            bluRange[0] = initBluRange[0];
            bluRange[1] = initBluRange[1];
            bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
            bluHighTxtFld.setText(Float.toString((float)bluRange[1]));
            updateBluGamma(1.0);
            bluGammaTxtFld.setText("1.0");
        });

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            String redLow = redLowTxtFld.getText().trim();
            String redHigh = redHighTxtFld.getText().trim();
            updateRedRange(Double.valueOf(redLow), Double.valueOf(redHigh));
            String grnLow = grnLowTxtFld.getText().trim();
            String grnHigh = grnHighTxtFld.getText().trim();
            updateGrnRange(Double.valueOf(grnLow), Double.valueOf(grnHigh));
            String bluLow = bluLowTxtFld.getText().trim();
            String bluHigh = bluHighTxtFld.getText().trim();
            updateBluRange(Double.valueOf(bluLow), Double.valueOf(bluHigh));

            String tmp = redGammaTxtFld.getText().trim();
            updateRedGamma(Double.valueOf(tmp));
            tmp = grnGammaTxtFld.getText().trim();
            updateGrnGamma(Double.valueOf(tmp));
            tmp = bluGammaTxtFld.getText().trim();
            updateBluGamma(Double.valueOf(tmp));
        });

        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel topPanel =  GuiUtils.doLayout(new Component[] {
                GuiUtils.rLabel("Red range: "), redLowTxtFld, redHighTxtFld, GuiUtils.cLabel("Red Gamma: "),
                redGammaTxtFld, redReset,
                GuiUtils.rLabel("Green range: "), grnLowTxtFld, grnHighTxtFld, GuiUtils.cLabel("Green Gamma: "),
                grnGammaTxtFld, grnReset,
                GuiUtils.rLabel("Blue range: "), bluLowTxtFld, bluHighTxtFld, GuiUtils.cLabel("Blue Gamma: "),
                bluGammaTxtFld,bluReset,
                GuiUtils.filler(),GuiUtils.filler(),GuiUtils.filler(),GuiUtils.filler(),GuiUtils.filler(), applyButton
                }, 6, GuiUtils.WT_N, GuiUtils.WT_N);


       // topPanel.add(Box.createHorizontalStrut(1), "span 5");
        //topPanel.add(applyButton, "wrap");

        JPanel bottomPanel = GuiUtils.doLayout(new Component[] {
                GuiUtils.rLabel("Common Gamma: "), gammaTxtFld, allGammaButton,
                GuiUtils.rLabel("Vertical Position: "), doMakeZPositionSlider(), GuiUtils.filler()
        }, 3, GuiUtils.WT_N, GuiUtils.WT_N);



        JPanel mainPanel = GuiUtils.vbox(topPanel, bottomPanel);
        mainPanel = GuiUtils.topLeft(mainPanel);

        return mainPanel;
    }

    private class ColorMapListener implements ScalarMapListener {
        ScalarMap clrMap;

        double[] range = null;
        double[] initRange = null;

        JTextField lowTxtFld;
        JTextField highTxtFld;

        ColorMapListener(ScalarMap clrMap, double[] initRange, double[] range, JTextField lowTxtFld, JTextField highTxtFld) {
            this.clrMap = clrMap;
            this.lowTxtFld = lowTxtFld;
            this.highTxtFld = highTxtFld;
            this.range = range;
            this.initRange = initRange;
        }

        public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
        }

        @Override
        public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {
            if (event.getId() == ScalarMapEvent.AUTO_SCALE) {
                double[] rng = clrMap.getRange();
                boolean shouldRemove = false;
                //Ghansham: decide whether it is first time. The cleaner way
                if (!Double.isNaN(rng[0]) && !Double.isNaN(rng[1]) && Double.isNaN(initRange[0]) && Double.isNaN(initRange[1])) {
                    shouldRemove = true;
                }
                range[0] = rng[0];
                range[1] = rng[1];
                initRange[0] = rng[0];
                initRange[1] = rng[1];
                lowTxtFld.setText(Float.toString((float)rng[0]));
                highTxtFld.setText(Float.toString((float)rng[1]));
                //Ghansham:If its first time remove the scalarmaplistener and setRange manually to disable autscaling of the scalarmap
                if (shouldRemove) {
                    clrMap.removeScalarMapListener(this);
                    //-Lock out auto-scaling
                    clrMap.disableAutoScale();
                }
            } else if (event.getId() == ScalarMapEvent.MANUAL) {
                double[] rng = clrMap.getRange();
                range[0] = rng[0];
                range[1] = rng[1];
            }
        }
    }
}
