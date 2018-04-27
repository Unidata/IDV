package edu.wisc.ssec.mcidasv.data.hydra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.AxisScale;
import visad.BaseColorControl;
import visad.CellImpl;
import visad.CoordinateSystem;
import visad.Data;
import visad.DelaunayCustom;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.Real;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Gridded3DSet;
import visad.Integer1DSet;
import visad.Linear2DSet;
import visad.LinearLatLonSet;
import visad.RealTupleType;
import visad.MathType;
import visad.RealType;
import visad.SampledSet;
import visad.ScalarMap;
import visad.Set;
import visad.SetType;
import visad.UnionSet;
import visad.VisADException;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;
import visad.python.JPythonMethods;

//import ucar.unidata.data.hydra.Statistics;

public class StatsTable extends AbstractTableModel {

    String [][] data;
    JTable table;
    JFrame statsWindow;
    double total_area = 0.0;
    int numCols;
    boolean isShowing = false;
    Color[] coltab = {new Color(0xf0f0f0), new Color(0xffd0ff),
            new Color(0xd0ffd0), new Color(0xc0d0ff)};

    final int maxCols = 9;
    String[] colNames = {"Stats Parameter","Whole Field X","Whole Field Y",
            "Magenta X","Magenta Y", "Green X","Green Y","Blue X","Blue Y"};

    final int maxRows = 13;
    final String[] rowNames = {"Maximum","Minimum",
            "Number of points","Mean","Median","Variance","Kurtosis",
            "Std Dev","Correlation","Difference Maximum",
            "Difference Minimum","Difference Mean","Area [km^2]"};

    boolean saveStats = true;


    public StatsTable() {
        this(true);
    }

    public StatsTable(boolean saveStats) { super();
        this.saveStats = saveStats;

        data = new String[maxRows][maxCols];
        numCols = 1;

        for (int i=0; i<maxRows; i++) {
            data[i][0] = rowNames[i];
            for (int j=1; j<maxCols; j++) {
                data[i][j] = "  ";
            }
        }


        table = new JTable(this) {
            public Component prepareRenderer(
                    TableCellRenderer renderer, int row, int col) {
                Component comp = super.prepareRenderer(renderer, row, col);
                Color c = Color.white;
                if (col == 0) c = coltab[0];
                if (col == 3 || col == 4) c = coltab[1];
                if (col == 5 || col == 6) c = coltab[2];
                if (col == 7 || col == 8) c = coltab[3];
                comp.setBackground(c);
                return comp;
            }

        };
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(620,220));
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        JButton saveStatsButt = new JButton("Save As CSV");
        JScrollPane sp = new JScrollPane(table);
        statsWindow = new JFrame("Scatter Statistics");
        statsWindow.setLayout(new BorderLayout());
        statsWindow.getContentPane().add(sp,BorderLayout.NORTH);
        JPanel bpan = new JPanel(new FlowLayout());
        bpan.add(saveStatsButt);
        if (saveStats) {
            statsWindow.getContentPane().add(bpan,BorderLayout.SOUTH);
        }
        statsWindow.setSize(650,340);
        statsWindow.pack();
        statsWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                isShowing = false;
            }
        });

        saveStatsButt.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JFileChooser chzr = new JFileChooser();
                FileFilter filt = new FileNameExtensionFilter("csv","txt");
                chzr.addChoosableFileFilter(filt);
                int rv = chzr.showSaveDialog(statsWindow);
                if (rv == JFileChooser.APPROVE_OPTION) {
                    try {
                        File fpw = chzr.getSelectedFile();
                        statsWindow.setTitle("Scatter Statistics saved to "+fpw.toString());
                        PrintWriter pw = new PrintWriter(fpw);
                        String line = "";
                        for (int k=0; k<colNames.length; k++) {
                            if (k != 0) line = line + ",";
                            line = line + colNames[k];
                        }
                        pw.println(line);

                        for (int i=0; i<data.length; i++) {
                            line = "";
                            for (int j=0; j<data[i].length; j++) {
                                if (j != 0) line = line+",";
                                line = line+data[i][j];
                            }
                            pw.println(line);
                        }
                        pw.flush();
                        pw.close();
                    } catch (Exception epw) {
                        statsWindow.setTitle("Scatter Statistics: File not saved");
                    }

                }

            }
        });

        isShowing = false;
        statsWindow.setVisible(false);
    }

    public void setIsShowing() {
        isShowing = true;
    }

    public void resetValues(int col) {
        for (int i=0; i<maxRows; i++) {
            int c = 2*col + 3;
            data[i][c] = " ";
            data[i][c+1] = " ";
        }
        fireTableStructureChanged();
    }

    public void setNames(String xn, String yn) {
        colNames[1] = colNames[3] = colNames[5] = colNames[7] =xn;
        colNames[2] = colNames[4] = colNames[6] = colNames[8] =yn;
    }

    // fx, fy are Fields, col = 0,1,2,3 (all, red, green, blue)
    public void setFields(FlatField fx, FlatField fy, int col) {
        statsWindow.setTitle("Scatter Statistics");
        try {
            Statistics sx = new Statistics(fx);
            Statistics sy = new Statistics(fy);
            Statistics diff = new Statistics((FlatField)fx.subtract(fy));

            int c = 2*col + 1;
            data[0][c] = fmtMe(((Real)sx.max()).getValue());
            data[0][c+1] = fmtMe(((Real)sy.max()).getValue());

            data[1][c] = fmtMe(((Real)sx.min()).getValue());
            data[1][c+1] = fmtMe(((Real)sy.min()).getValue());

            data[2][c] = String.format("%d",sx.numPoints());
            data[2][c+1] = String.format("%d",sy.numPoints());

            data[3][c] = fmtMe(((Real)sx.mean()).getValue());
            data[3][c+1] = fmtMe(((Real)sy.mean()).getValue());

            data[4][c] = fmtMe(((Real)sx.median()).getValue());
            data[4][c+1] = fmtMe(((Real)sy.median()).getValue());

            data[5][c] = fmtMe(((Real)sx.variance()).getValue());
            data[5][c+1] = fmtMe(((Real)sy.variance()).getValue());

            data[6][c] = fmtMe(((Real)sx.kurtosis()).getValue());
            data[6][c+1] = fmtMe(((Real)sy.kurtosis()).getValue());

            data[7][c] = fmtMe(((Real)sx.standardDeviation()).getValue());
            data[7][c+1] = fmtMe(((Real)sy.standardDeviation()).getValue());

            data[8][c] = fmtMe(((Real)sx.correlation(fy)).getValue());
            data[8][c+1] = " ";

            data[9][c] = fmtMe(((Real)diff.max()).getValue());
            data[9][c+1] = " ";

            data[10][c] = fmtMe(((Real)diff.min()).getValue());
            data[10][c+1] = " ";

            data[11][c] = fmtMe(((Real)diff.mean()).getValue());
            data[11][c+1] = " ";

            if (c == 1) {
                data[12][c] = " ";
            } else {
                data[12][c] = fmtMe(total_area);
            }
            data[12][c+1] = " ";

            if (c+2 > numCols) numCols = c+2;
            fireTableStructureChanged();

        } catch (VisADException exc) {
            System.out.println(exc.getMessage());
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        if (isShowing) statsWindow.setVisible(true);
    }

    public static String fmtMe(double val) {

        if (Math.abs(val) == 0.0) {
            return "0.00";

        } else if (Math.abs(val) > 9999.9 || Math.abs(val) < .0010) {
            return String.format("%.6e", val);

        } else if (Math.abs(val) < 1.0) {
            return String.format("%.5f", val);

        } else if (Math.abs(val) < 10.0) {
            return String.format("%.3f", val);

        } else {
            return String.format("%.2f", val);
        }
    }

    public void setPoints(float[][] markScatter, int len, int indx, double area) {
        try {
            total_area = area;
            Integer1DSet sdset = new Integer1DSet(len);
            FlatField scattX = new FlatField(
                    new FunctionType(RealType.Generic, RealType.Generic), sdset);

            float[][] scattValsX = new float[1][len];
            System.arraycopy(markScatter[0],0,scattValsX[0],0,len);
            scattX.setSamples(scattValsX, false);

            FlatField scattY = new FlatField(
                    new FunctionType(RealType.Generic, RealType.Generic), sdset);
            float[][] scattValsY = new float[1][len];
            System.arraycopy(markScatter[1],0,scattValsY[0],0,len);
            scattY.setSamples(scattValsY, false);

            setFields(scattX, scattY, indx);

        } catch (Exception esd) {
            esd.printStackTrace();
        }
    }

    public int getRowCount() {
        return maxRows;
    }
    public int getColumnCount() {
        return numCols;
    }
    public String getValueAt(int row, int col) {
        return data[row][col];
    }
    public String getColumnName(int col) {
        return colNames[col];
    }
}