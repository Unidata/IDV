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

package ucar.unidata.idv.control;


import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.gis.Transect;

import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.idv.control.drawing.*;


import ucar.unidata.ui.CommandManager;

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
import ucar.visad.data.MapSet;


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
    public static final String REGION_FIELD = "field";
    public static final String REGION_ALL = "all";
    public static final String REGION_SELECTED = "selected";


    private RadarSweepControl radarSweepControl;

    private  PythonInterpreter interpreter;

    private     JTextField upperThresholdFld;
    private     JTextField lowerThresholdFld;

    private JComboBox regionModeCbx;
    private JComboBox insideCbx;
    private JTextArea commandsTextArea;

    private CommandManager commandManager = new CommandManager(10);

    private List<Action> actions = new ArrayList<Action>();

    private  JList actionList;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public RadarEditor() {
        setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
    }


    public static class MyRadarSweepControl extends RadarSweepControl {
        protected void addToControlContext() {
        }

        public boolean getShowInLegend() {
            return false;
        }

    }

    public void doRemove() throws RemoteException, VisADException {
        if(radarSweepControl!=null) {
            radarSweepControl.doRemove();
        }
        super.doRemove();
    }

    public boolean init(List choices) throws VisADException, RemoteException {
        super.init((DataChoice)null);
        radarSweepControl = new MyRadarSweepControl();
        radarSweepControl.setUse3D(false);
        radarSweepControl.init(getDisplayId(), getCategories(), choices,
                               getControlContext(), "", getDataSelection());
        return true;
    }

    private  PythonInterpreter  getInterpreter() {
        if(interpreter ==null)
            interpreter =   getControlContext().getJythonManager().createInterpreter();
        return interpreter;
    }

    protected void appendCommand(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    commandsTextArea.append(text);
                }
            });

    }




    public String getRegionMode() {
        return (String)((TwoFacedObject)regionModeCbx.getSelectedItem()).getId();
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
        updateActionList();
        actionList.setVisibleRowCount(5);
        commandsTextArea = new JTextArea("",5,40);
        regionModeCbx = new JComboBox(new Object[]{
            new TwoFacedObject("All Regions", REGION_ALL),
            new TwoFacedObject("Selected Regions", REGION_SELECTED),
            new TwoFacedObject("Entire Field", REGION_FIELD)});
        insideCbx =  new JComboBox(new String[]{"Inside Region","Outside Region"});
        JTabbedPane tabbedPane = new JTabbedPane();
        upperThresholdFld= new JTextField("0",5);
        lowerThresholdFld= new JTextField("0",5);

        JComponent regionComp  = GuiUtils.hbox(regionModeCbx, insideCbx,5);
        List commands = new ArrayList();
        commands.add(GuiUtils.rLabel("History:"));
        commands.add(GuiUtils.left(commandManager.getContents(false)));
        commands.add(GuiUtils.rLabel("Apply To:"));
        commands.add(GuiUtils.left(regionComp));
        commands.add(GuiUtils.filler());
        commands.add(GuiUtils.left(GuiUtils.makeButton("Average", this,"doAverage")));
        commands.add(GuiUtils.filler());
        commands.add(GuiUtils.left(GuiUtils.makeButton("Absolute value", this,"doAbsoluteValue")));
        commands.add(GuiUtils.filler());
        commands.add(GuiUtils.left(GuiUtils.hbox(GuiUtils.makeButton("Upper Threshold", this,"doUpperThreshold"),
                                                 upperThresholdFld,2)));
        commands.add(GuiUtils.filler());
        commands.add(GuiUtils.left(GuiUtils.hbox(GuiUtils.makeButton("Lower Threshold", this,"doLowerThreshold"),
                                                 lowerThresholdFld,2)));


        GuiUtils.tmpInsets  =GuiUtils.INSETS_5;
        JComponent buttons = GuiUtils.doLayout(commands,2,GuiUtils.WT_N,GuiUtils.WT_N);
        JComponent execButtons = GuiUtils.hbox(
                                               GuiUtils.makeButton("Execute", this,"executeActions"),
                                               GuiUtils.makeButton("Clear", this,"clearActions"),5);
        JComponent actionsComp = GuiUtils.topCenter(GuiUtils.left(execButtons), new JScrollPane(actionList));
        JComponent commandsPanel  = GuiUtils.topCenter(buttons, actionsComp);
        //        JComponent commandsPanel  = GuiUtils.topCenter(buttons, new JLabel(""));


        tabbedPane.add("Commands", commandsPanel);
        tabbedPane.add("Regions", GuiUtils.topCenter(doMakeControlsPanel(),doMakeShapesPanel()));
        tabbedPane.add("Radar Display", radarSweepControl.doMakeContents());
        return tabbedPane;
        //        return GuiUtils.centerBottom(tabbedPane, msgLabel);
    }


    private UnionSet getMapLines(String regionMode) throws Exception {
        if(regionMode.equals(REGION_FIELD)) return null;
        List glyphsToUse = (regionMode.equals(REGION_ALL)?glyphs:selectedGlyphs);
        if(glyphsToUse.size()==0) {
            return null;
        }

        Gridded2DSet[] latLonLines = new Gridded2DSet[glyphsToUse.size()];
        int cnt=0;
        for(DrawingGlyph glyph: (List<DrawingGlyph>) glyphsToUse) {
            latLonLines[cnt++] = glyph.makeMapSet();
        }
        RealTupleType coordMathType =
            new RealTupleType(RealType.Longitude,RealType.Latitude);
        UnionSet maplines = new UnionSet(coordMathType, latLonLines,
                                         (CoordinateSystem) null,
                                         (Unit[]) null,
                                         (ErrorEstimate[]) null, false);  

        return maplines;
    }


    private void updateActionList() {
        actionList.setListData(new Vector(actions));
    }

    public void addAction(Action action) {
        actions.add(action);
        updateActionList();
    }


    public void executeActions() {
        executeActions(false);
    }

    public void executeAndClearActions() {
        executeActions(true);
    }

    public void clearActions() {
        actions = new ArrayList();
        updateActionList();
    }

    public void executeActions(boolean andClear) {
        try {
            FieldImpl oldSlice = radarSweepControl.getCurrentSlice();
            for(Action action: actions) {
                applyAction(action);
            }
            FieldImpl newSlice =  radarSweepControl.getCurrentSlice();
            commandManager.add(new EditCommand(this, oldSlice,newSlice),true);
            radarSweepControl.getGridDisplayable().loadData((FieldImpl)newSlice);
            if(andClear) {
                clearActions();
            }
        } catch(Exception exc) {
            logException("Error", exc);
        }
    }


    public void applyAction (Action action){
        try {
            UnionSet mapLines = getMapLines(action.regionMode);
            if(mapLines==null && !action.regionMode.equals(REGION_FIELD)) {
                userMessage("No regions defined");
                return;
            }
            FieldImpl oldSlice = radarSweepControl.getCurrentSlice();
            getInterpreter().set("mapLines", mapLines);
            getInterpreter().set("slice", oldSlice);
            long t1 = System.currentTimeMillis();
            getInterpreter().exec("newSlice = mapsApplyToField('" +action.function+"',slice,mapLines," +
                                  (action.inside?"1":"0")+")");
            long t2 = System.currentTimeMillis();
            System.err.println("Time:" + (t2-t1));
            PyObject    obj     = interpreter.get("newSlice");
            FieldImpl newSlice = (FieldImpl) obj.__tojava__(Data.class);
            radarSweepControl.setCurrentSlice(newSlice);
        } catch(Exception exc) {
            logException("Error", exc);
        }
    }


    public void addAction(String name, String func) {
        addAction(new Action(name, func, getRegionMode(),insideCbx.getSelectedIndex()==0));
    }


    public void doAverage () {
        addAction("Average","mapsAverage(originalValues, newValues, indexArray)");
    }

    public void doAbsoluteValue () {
        addAction("Absolute value","mapsAbsoluteValue(originalValues, newValues, indexArray)");
    }

    public void doUpperThreshold () {
        String value = upperThresholdFld.getText();
        addAction("Upper Threshold ("+value+")", "mapsThresholdUpper(originalValues, newValues, indexArray," + value+")");
    }

    public void doLowerThreshold () {
        String value = lowerThresholdFld.getText();
        addAction("Lower Threshold ("+value+")", "mapsThresholdLower(originalValues, newValues, indexArray," + value+")");
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
        List shapes = new ArrayList();
        shapes.add(GlyphCreatorCommand.CMD_CLOSEDPOLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_POLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_LINE);
        shapes.add(GlyphCreatorCommand.CMD_RECTANGLE);
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
       Set the Actions property.

       @param value The new value for Actions
    **/
    public void setActions (List<Action> value) {
	actions = value;
    }

    /**
       Get the Actions property.

       @return The Actions
    **/
    public List<Action> getActions () {
	return actions;

    }




    public static class EditCommand extends ucar.unidata.ui.Command {
        RadarEditor editor;
        FieldImpl oldSlice;
        FieldImpl newSlice;

        public EditCommand(RadarEditor editor, FieldImpl oldSlice,
                            FieldImpl newSlice) {
            this.editor=editor;
            this.oldSlice=oldSlice;
            this.newSlice=newSlice;
        }

        /**
         * _more_
         */
        public void doCommand() {
            try {
            editor.radarSweepControl.getGridDisplayable().loadData((FieldImpl)newSlice);
            editor.radarSweepControl.setCurrentSlice(newSlice);
        } catch(Exception exc) {
            editor.logException("Error", exc);
        }
        }



        /**
         * _more_
         */
        public void undoCommand() {
            try {
            editor.radarSweepControl.getGridDisplayable().loadData((FieldImpl)oldSlice);
            editor.radarSweepControl.setCurrentSlice(oldSlice);
        } catch(Exception exc) {
            editor.logException("Error", exc);
        }
        }
        
    }



    public static class Action {
        String name;
        String function;
        String regionMode;
        boolean inside;
        public Action() {

        }

        public Action(String name, String function, String regionMode, boolean inside) {
            this.name = name;
            this.function = function;
            this.regionMode = regionMode;
            this.inside = inside;
        }

        public String toString() {
            String region = regionMode.equals(REGION_FIELD)?"entire field":(regionMode.equals(REGION_ALL)?"all regions":"selected regions");
            return name + " applied to " +region;
        }



        /**
           Set the Name property.

           @param value The new value for Name
        **/
        public void setName (String value) {
            name = value;
        }

        /**
           Get the Name property.

           @return The Name
        **/
        public String getName () {
            return name;
        }

        /**
           Set the Function property.

           @param value The new value for Function
        **/
        public void setFunction (String value) {
            function = value;
        }

        /**
           Get the Function property.

           @return The Function
        **/
        public String getFunction () {
            return function;
        }

        /**
           Set the RegionMode property.

           @param value The new value for RegionMode
        **/
        public void setRegionMode (String value) {
            regionMode = value;
        }

        /**
           Get the RegionMode property.

           @return The RegionMode
        **/
        public String getRegionMode () {
            return regionMode;
        }

        /**
           Set the Inside property.

           @param value The new value for Inside
        **/
        public void setInside (boolean value) {
            inside = value;
        }

        /**
           Get the Inside property.

           @return The Inside
        **/
        public boolean getInside () {
            return inside;
        }




    }


}

