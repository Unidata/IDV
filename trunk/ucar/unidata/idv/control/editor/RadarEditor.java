/*
 * $Id: TransectDrawingControl.java,v 1.41 2006/12/28 19:50:59 jeffmc Exp $
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

package ucar.unidata.idv.control.editor;


import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.gis.MapMaker;
import ucar.unidata.data.gis.Transect;
import ucar.unidata.data.grid.GridDataInstance;

import ucar.unidata.data.radar.RadarConstants;

import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.RadarSweepControl;



import ucar.unidata.idv.control.drawing.*;


import ucar.unidata.ui.CommandManager;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.data.MapSet;

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
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;


/**
 *
 * @author MetApps development team
 * @version $Revision: 1.41 $
 */

public class RadarEditor extends DrawingControl {



    /** _more_ */
    private MyRadarSweepControl radarSweepControl;

    /** _more_ */
    private PythonInterpreter interpreter;

    /** _more_ */
    private JTextField maxFld;

    /** _more_ */
    private JTextField minFld;

    /** _more_          */
    private JTextArea exprFld;

    /** _more_ */
    private JComboBox regionModeCbx;

    /** _more_ */
    private JComboBox insideCbx;

    /** _more_ */
    private JTextArea commandsTextArea;

    /** _more_ */
    private CommandManager commandManager = new CommandManager(10);

    /** _more_ */
    private List<Action> actions = new ArrayList<Action>();

    /** _more_ */
    private JList actionList;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public RadarEditor() {
        setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
    }


    /**
     * Class MyRadarSweepControl _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class MyRadarSweepControl extends RadarSweepControl {

        /**
         * _more_
         */
        protected void addToControlContext() {}

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getShowInLegend() {
            return false;
        }

        /**
         * _more_
         *
         * @param slice _more_
         *
         * @throws Exception _more_
         */
        protected void setCurrentSlice(FieldImpl slice) throws Exception {
            super.setCurrentSlice(slice);
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected FieldImpl getCurrentSlice() throws Exception {
            return super.getCurrentSlice();
        }


    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void doRemove() throws RemoteException, VisADException {
        if (radarSweepControl != null) {
            radarSweepControl.doRemove();
        }
        super.doRemove();
    }


    /**
     * _more_
     *
     * @param field _more_
     *
     * @throws Exception _more_
     */
    protected void setField(FieldImpl field) throws Exception {
        radarSweepControl.getGridDisplayable().loadData(field);
        radarSweepControl.setCurrentSlice(field);
    }


    /**
     * _more_
     *
     * @param choices _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean init(List choices) throws VisADException, RemoteException {
        super.init((DataChoice) null);
        radarSweepControl = new MyRadarSweepControl();
        radarSweepControl.setUse3D(false);
        radarSweepControl.init(getDisplayId(), getCategories(), choices,
                               getControlContext(), "", getDataSelection());
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private PythonInterpreter getInterpreter() {
        if (interpreter == null) {
            interpreter =
                getControlContext().getJythonManager().createInterpreter();
        }
        return interpreter;
    }

    /**
     * _more_
     *
     * @param text _more_
     */
    protected void appendCommand(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                commandsTextArea.append(text);
            }
        });

    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getRegionMode() {
        return (String) ((TwoFacedObject) regionModeCbx.getSelectedItem())
            .getId();
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
        actionList = new JList();
        actionList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        actionList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
		if (GuiUtils.isDeleteEvent(e)) {
                    int[]        indices = actionList.getSelectedIndices();
                    List<Action> tmp     = actions;
                    actions = new ArrayList<Action>(tmp);
                    for (int i = 0; i < indices.length; i++) {
                        actions.remove(tmp.get(indices[i]));
                    }
                    updateActionList();
                }
            }
        });
        updateActionList();
        actionList.setVisibleRowCount(5);
        commandsTextArea = new JTextArea("", 5, 40);
        regionModeCbx = new JComboBox(new Object[] {
            new TwoFacedObject("All Regions", Selector.TYPE_REGION_ALL),
            new TwoFacedObject("Selected Regions",
                               Selector.TYPE_REGION_SELECTED),
            new TwoFacedObject("Entire Field", Selector.TYPE_FIELD) });
        insideCbx = new JComboBox(new String[] { "Inside Region",
                "Outside Region" });
        JTabbedPane tabbedPane = new JTabbedPane();
        maxFld  = new JTextField("0", 5);
        minFld  = new JTextField("0", 5);
        exprFld = new JTextArea("", 3, 30);
        exprFld.setToolTipText("e.g, value = value*4;");
        JComponent regionComp = GuiUtils.hbox(regionModeCbx, insideCbx, 5);
        List       comps      = new ArrayList();
        comps.add(GuiUtils.rLabel("History:"));
        comps.add(GuiUtils.left(commandManager.getContents(false)));
        comps.add(GuiUtils.rLabel("Apply To:"));
        comps.add(GuiUtils.left(regionComp));


        List actionComps = new ArrayList();
        actionComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.makeButton("Average", this, "doAverage"),
                    GuiUtils.makeButton(
                        "Absolute value", this, "doAbsoluteValue"), 5)));
        actionComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.hbox(
                        GuiUtils.makeButton("Max", this, "doMax"), maxFld,
                        2), GuiUtils.left(
                            GuiUtils.hbox(
                                GuiUtils.makeButton("Min", this, "doMin"),
                                minFld, 2)), 5)));

        actionComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.makeButton("Expression", this, "doExpr"),
                    exprFld, 2)));


        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent actionComp = GuiUtils.doLayout(actionComps, 1,
                                    GuiUtils.WT_N, GuiUtils.WT_N);


        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        comps.add(GuiUtils.rLabel("Actions:"));
        comps.add(actionComp);
        JComponent topComp = GuiUtils.doLayout(comps, 2, GuiUtils.WT_N,
                                 GuiUtils.WT_N);


        JComponent execButtons =
            GuiUtils.hbox(GuiUtils.makeButton("Execute", this,
                "executeActions"), GuiUtils.makeButton("Clear", this,
                    "clearActions"), 5);
        JComponent actionsComp =
            GuiUtils.inset(GuiUtils.topCenter(GuiUtils.left(execButtons),
                new JScrollPane(actionList)), new Insets(5, 0, 0, 0));
        JComponent commandsPanel = GuiUtils.topCenter(topComp, actionsComp);
        //        JComponent commandsPanel  = GuiUtils.topCenter(buttons, new JLabel(""));


        tabbedPane.add("Commands", commandsPanel);
        tabbedPane.add("Regions",
                       GuiUtils.topCenter(doMakeControlsPanel(),
                                          doMakeShapesPanel()));
        tabbedPane.add("Radar Display", radarSweepControl.doMakeContents());
        return tabbedPane;
        //        return GuiUtils.centerBottom(tabbedPane, msgLabel);
    }


    /**
     * _more_
     *
     *
     *
     * @param selector _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private UnionSet getMapLines(Selector selector) throws Exception {
        if ( !selector.isRegion()) {
            return null;
        }

        List glyphsToUse =
            (selector.getType().equals(Selector.TYPE_REGION_ALL)
             ? glyphs
             : selectedGlyphs);
        if (glyphsToUse.size() == 0) {
            return null;
        }


        MapMaker mapMaker = new MapMaker();
        for (DrawingGlyph glyph : (List<DrawingGlyph>) glyphsToUse) {
            mapMaker.addMap(glyph.getLatLons());
        }
        return mapMaker.getMaps();
    }


    /**
     * _more_
     *
     * @param selector _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getMapLinesJython(Selector selector, StringBuffer sb)
            throws Exception {
        String var = "regions_" + (exprCnt++);
        if ( !selector.isRegion()) {
            sb.append(var + "=None;\n");
            return var;
        }

        List glyphsToUse =
            (selector.getType().equals(Selector.TYPE_REGION_ALL)
             ? glyphs
             : selectedGlyphs);
        if (glyphsToUse.size() == 0) {
            sb.append(var + "=None;\n");
            return var;
        }

        sb.append(var + "=MapMaker();\n");
        MapMaker mapMaker = new MapMaker();
        for (DrawingGlyph glyph : (List<DrawingGlyph>) glyphsToUse) {
            float[][] latLons = glyph.getLatLons();
            sb.append(var + ".addMap(array([");
            for (int i = 0; i < latLons[0].length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(latLons[0][i]);
                sb.append(",");
                sb.append(latLons[1][i]);
            }
            sb.append("],'f'));\n");
        }
        return var;
    }


    /**
     * _more_
     */
    private void updateActionList() {
        actionList.setListData(new Vector(actions));
    }

    /**
     * _more_
     *
     * @param action _more_
     */
    public void addAction(Action action) {
        actions.add(action);
        updateActionList();
    }


    /**
     * _more_
     */
    public void executeActions() {
        executeActions(false);
    }

    /**
     * _more_
     */
    public void executeAndClearActions() {
        executeActions(true);
    }

    /**
     * _more_
     */
    public void clearActions() {
        actions = new ArrayList();
        updateActionList();
    }

    /**
     * _more_
     *
     * @param andClear _more_
     */
    public void executeActions(boolean andClear) {
        try {
            FieldImpl oldSlice = radarSweepControl.getCurrentSlice();
            for (Action action : actions) {
                applyAction(action);
            }
            FieldImpl newSlice = radarSweepControl.getCurrentSlice();
            commandManager.add(new FieldCommand(this, oldSlice, newSlice),
                               true);
            radarSweepControl.getGridDisplayable().loadData(
                (FieldImpl) newSlice);
            if (andClear) {
                clearActions();
            }
        } catch (Exception exc) {
            logException("Error", exc);
        }
    }


    /**
     * _more_
     *
     * @param action _more_
     */
    public void applyAction(Action action) {
        try {
            UnionSet mapLines = getMapLines(action.getSelector());
            if ((mapLines == null) && action.getSelector().isRegion()) {
                userMessage("No regions defined");
                return;
            }

            long         t1       = System.currentTimeMillis();
            FieldImpl    oldSlice = radarSweepControl.getCurrentSlice();
            StringBuffer sb       = new StringBuffer();
            //            getMapLinesJython(action.getSelector(),  sb);
            //            getInterpreter().exec(sb.toString());


            getInterpreter().set("field", oldSlice);
            if (action.getJython() != null) {
                getInterpreter().exec(action.getJython());
            }

            if (action.getSelector().isRegion()) {
                getInterpreter().set("mapLines", mapLines);
                getInterpreter().exec("newField = mapsApplyToField('"
                                      + action.getFunction()
                                      + "',field,mapLines,"
                                      + (action.getSelector().getInside()
                                         ? "1"
                                         : "0") + ")");
            }
            long t2 = System.currentTimeMillis();
            System.err.println("Time:" + (t2 - t1));
            PyObject  obj      = interpreter.get("newField");
            FieldImpl newSlice = (FieldImpl) obj.__tojava__(Data.class);
            radarSweepControl.setCurrentSlice(newSlice);
        } catch (Exception exc) {
            logException("Error", exc);
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param func _more_
     */
    public void addAction(String name, String func) {
        addAction(new Action(name, func, getSelector()));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private Selector getSelector() {
        return new Selector(getRegionMode(),
                            insideCbx.getSelectedIndex() == 0);
    }






    /**
     * _more_
     */
    public void doAverage() {
        addAction("Average",
                  "mapsAverage(originalValues, newValues, indexArray)");
    }



    /** _more_          */
    private int exprCnt = 0;

    /**
     * _more_
     */
    public void doExpr() {
        String expr     = exprFld.getText();
        String funcName = "exprFunction" + (exprCnt++);
        Action action =
            new Action("Expression " + expr.replace("\n", ""),
                       funcName + "(originalValues,newValues,indexArray)",
                       getSelector());

        StringBuffer jython =
            new StringBuffer("def " + funcName
                             + "(originalValues,newValues,indexArray):\n");
        jython.append("\tfor i in xrange(len(indexArray)):\n");
        jython.append("\t\tindex=indexArray[i];\n");
        jython.append("\t\tvalue=originalValues[0][index];\n");
        for (String line : (List<String>) StringUtil.split(expr, "\n", false,
                false)) {
            jython.append("\t\t");
            jython.append(line);
            jython.append("\n");
        }
        jython.append("\t\tnewValues[0][index] = value;\n");
        //        System.err.println(jython);
        action.setJython(jython.toString());
        addAction(action);
    }

    /**
     * _more_
     */
    public void doAbsoluteValue() {
        addAction("Absolute value",
                  "mapsAbsoluteValue(originalValues, newValues, indexArray)");
    }

    /**
     * _more_
     */
    public void doMax() {
        String value = maxFld.getText();
        addAction("Max (" + value + ")",
                  "mapsMax(originalValues, newValues, indexArray," + value
                  + ")");
    }

    /**
     * _more_
     */
    public void doMin() {
        String value = minFld.getText();
        addAction("Min (" + value + ")",
                  "mapsMin(originalValues, newValues, indexArray," + value
                  + ")");
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
        List commands = Misc.newList(CMD_SELECT, CMD_MOVE, CMD_STRETCH);
        List shapes   = new ArrayList();
        currentCmd = GlyphCreatorCommand.CMD_CLOSEDPOLYGON;
        shapes.add(GlyphCreatorCommand.CMD_CLOSEDPOLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_POLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_LINE);
        //        shapes.add(GlyphCreatorCommand.CMD_RECTANGLE);
        ButtonGroup bg = new ButtonGroup();
        widgets.add(GuiUtils.rLabel("Mode:"));
        if (straightCbx == null) {
            straightCbx = new JCheckBox("Straight", getStraight());
        }

        widgets.add(GuiUtils.left(GuiUtils.hbox(makeButtonPanel(shapes, bg),
                makeButtonPanel(commands, bg), straightCbx, enabledCbx)));

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
     *  Set the Actions property.
     *
     *  @param value The new value for Actions
     */
    public void setActions(List<Action> value) {
        actions = value;
    }

    /**
     *  Get the Actions property.
     *
     *  @return The Actions
     */
    public List<Action> getActions() {
        return actions;

    }




    /**
     * Class EditCommand _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class FieldCommand extends ucar.unidata.ui.Command {

        /** _more_ */
        RadarEditor editor;

        /** _more_ */
        FieldImpl oldSlice;

        /** _more_ */
        FieldImpl newSlice;

        /**
         * _more_
         *
         * @param editor _more_
         * @param oldSlice _more_
         * @param newSlice _more_
         */
        public FieldCommand(RadarEditor editor, FieldImpl oldSlice,
                            FieldImpl newSlice) {
            this.editor   = editor;
            this.oldSlice = oldSlice;
            this.newSlice = newSlice;
        }

        /**
         * _more_
         */
        public void doCommand() {
            try {
                editor.radarSweepControl.getGridDisplayable().loadData(
                    (FieldImpl) newSlice);
                editor.radarSweepControl.setCurrentSlice(newSlice);
            } catch (Exception exc) {
                editor.logException("Error", exc);
            }
        }



        /**
         * _more_
         */
        public void undoCommand() {
            try {
                editor.radarSweepControl.getGridDisplayable().loadData(
                    (FieldImpl) oldSlice);
                editor.radarSweepControl.setCurrentSlice(oldSlice);
            } catch (Exception exc) {
                editor.logException("Error", exc);
            }
        }

    }





}


