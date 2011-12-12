/*
 * $Id: ChartWrapper.java,v 1.46 2007/04/16 21:32:10 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control.chart;


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

import org.python.core.*;
import org.python.util.*;

import ucar.unidata.data.DataAlias;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;



import ucar.unidata.data.point.*;

import ucar.unidata.data.sounding.TrackDataSource;

import ucar.unidata.gis.SpatialGrid;



import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.multi.*;

import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.symbol.*;

import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.AnimationInfo;
import ucar.visad.display.AnimationWidget;



import visad.*;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.rmi.RemoteException;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


//import com.lavantech.gui.comp.*;



/**
 * Abstract class for chart implementations
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.46 $
 */
public abstract class ChartWrapper extends DisplayComponent implements KeyListener {


    /** Property change id */
    public static final String PROP_TIMERANGE = "prop.timerange";

    /** Property change id */
    public static final String PROP_SELECTEDTIME = "prop.selectedtime";

    /** The current min date to use */
    private double minDate = Double.NEGATIVE_INFINITY;

    /** The current max date to use */
    private double maxDate = Double.POSITIVE_INFINITY;

    /** The min date in the data */
    private double dataMinDate = Double.NEGATIVE_INFINITY;

    /** The max date in the data */
    private double dataMaxDate = Double.POSITIVE_INFINITY;

    /** The widget */
    protected AnimationWidget animationWidget;

    /** List of data choices */
    protected List dataChoiceWrappers;

    /** Resolution property widget */
    private JSlider resolutionSlider;


    /**
     *   The resolution. Used by derived classes to subsample the data.
     *   Is from 0.0-1.0 and represents a percentage.
     */
    private double resolution = 1.0;


    /** Used in properties to track if the user changed the order in the jlist */
    private boolean chartOrderChanged;

    /**
     *   This is the time series that we get the time segments from for subsetting
     */
    protected TimeSeriesChartWrapper timeFilterSource;

    /**
     *   This is the source for time selects
     */
    protected ChartWrapper timeSelectSource;


    /** show animation time in charts */
    private boolean showTime = false;

    /** map chart tim to animation time */
    private boolean driveTime = false;


    /** This holds the entries for each wrapper in the properties gui */
    private JComponent wrapperPanel;

    /** This holds the wrapper proeprties objects */
    private List fieldProperties;


    /** Jython for those charts that can have jython applied to them */
    private String jython = "";


    /** The interpreter to use for whatever */
    private PythonInterpreter interpreter;


    /** For properties */
    private JTextField jythonFld;

    /** What group are we sharing animation times as */
    private String animationShareGroup;

    /**
     * Default ctor
     */
    public ChartWrapper() {}



    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public ChartWrapper(String name, List dataChoices) {
        super(name);
        this.dataChoiceWrappers = wrapDataChoices(dataChoices);

    }

    /**
     * Get the data categories for selecting data choices
     *
     * @return List of data categories
     */
    public List getCategories() {
        List cats = DataCategory.parseCategories("trace", false);
        return Misc.newList(cats);
    }


    /**
     * Initialize
     *
     * @param displayControl The display control we're in.
     * @param dataChoices List of data choices
     */
    public void init(MultiDisplayHolder displayControl, List dataChoices) {
        this.dataChoiceWrappers = wrapDataChoices(dataChoices);
        this.displayControl     = displayControl;
        if (getName() == null) {
            setName(getTypeName());
        }
    }



    /**
     * Called after this chart has been created.
     */
    public void initDone() {
        super.initDone();
        if (canDoDriveTime()) {
            try {
                AnimationInfo animationInfo = new AnimationInfo();
                animationWidget = new AnimationWidget(animationInfo) {
                    protected void handleSharedTime(Real time) {
                        if ( !hasBeenInitialized) {
                            return;
                        }
                        super.handleSharedTime(time);
                        if ( !time.isMissing()) {
                            animationTimeChanged(time);
                        }
                    }

                    public String toString() {
                        return "chart anim";
                    }
                };
                if (animationShareGroup != null) {
                    animationWidget.setShareGroup(animationShareGroup);
                }
                //Just call this here to initialize the widget
                animationWidget.getContents();
                Animation animation = new Animation();
                animationWidget.setAnimation(animation);
            } catch (Exception exc) {
                LogUtil.logException("Creating animation widget", exc);
            }
        }




    }


    /**
     * Utility to extract a FlatField from the data
     *
     * @param data The data
     *
     * @return The flat field
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected FlatField getFlatField(FieldImpl data)
            throws VisADException, RemoteException {
        FlatField ff = null;
        if (GridUtil.isSequence(data)) {
            ff = (FlatField) data.getSample(0);
        } else {
            ff = (FlatField) data;
        }
        return ff;
    }






    /**
     * Overwritten by derived classes to return the name of the type of this chart.
     * ex: Histogram, Time Series, etc.
     *
     * @return The type name
     */
    public abstract String getTypeName();




    /**
     * Noop
     *
     * @param e The event
     */
    public void keyPressed(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {}



    /**
     * A utility that takes a list of DataChoice-s and
     * wraps each one in the DataChoiceWrapper.
     * We use the DataChoiceWrapper so that sub classes
     * can have their own extra info for each data choice.
     * ex:  colors, etc.
     *
     * @param choices List of data choices
     *
     * @return List of data choice wrappers
     */
    protected List wrapDataChoices(List choices) {
        List result = new ArrayList();
        if (choices == null) {
            return result;
        }
        for (int i = 0; i < choices.size(); i++) {
            DataChoice dataChoice = (DataChoice) choices.get(i);
            result.add(createDataChoiceWrapper(dataChoice));
        }
        return result;
    }

    /**
     * A utility to create a data choice wrapper
     *
     * @param dataChoice The data choice
     *
     * @return The data choice wrapper
     */
    protected DataChoiceWrapper createDataChoiceWrapper(
            DataChoice dataChoice) {
        return new DataChoiceWrapper(dataChoice);
    }




    /**
     * Get get list of Ranges for time subsetting. If there are none
     * then return null.
     *
     * @return List of time ranges or null
     */
    protected List getTimeFilterRanges() {
        List ranges = null;
        if ((minDate != Double.NEGATIVE_INFINITY)
                || (maxDate != Double.POSITIVE_INFINITY)) {
            if ((minDate != dataMinDate) || (maxDate != dataMaxDate)) {
                ranges = Misc.newList(new ucar.unidata.util.Range(minDate,
                        maxDate));

            }
        }

        if (timeFilterSource != null) {
            List filterRanges = timeFilterSource.getTimeRanges();
            if (filterRanges != null) {
                if (ranges == null) {
                    ranges = filterRanges;
                } else {
                    ranges.addAll(filterRanges);
                }
            }
        }
        return ranges;
    }




    /**
     * Get the time ranges to use
     *
     * @return null
     */
    public List getTimeRanges() {
        return null;
    }


    /**
     * Add the default menu items
     *
     *
     * @param items List of menu items
     *
     * @return The items list
     */
    protected List getPopupMenuItems(List items) {

        if (canDoParameters()) {
            items.add(GuiUtils.makeMenuItem("Add Field...", this,
                                            "addField"));
        }

        if (canDoJython() && (jython != null)
                && (jython.trim().length() > 0)) {
            items.add(GuiUtils.makeMenuItem("Apply Jython", this,
                                            "applyJython", jython));

        }

        items.add(GuiUtils.makeMenuItem("Remove Chart", this,
                                        "removeDisplayComponent"));

        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Save Image...", this,
                                        "doSaveImage"));
        items.add(GuiUtils.makeMenuItem("Save Movie...", this,
                                        "doSaveMovie"));


        if (canDoDriveTime()) {
            items.add(GuiUtils.MENU_SEPARATOR);

            final JCheckBoxMenuItem mi1 =
                new JCheckBoxMenuItem("Show Animation Times", showTime);
            items.add(mi1);
            mi1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    showTime = mi1.isSelected();
                }
            });

            final JCheckBoxMenuItem mi2 =
                new JCheckBoxMenuItem("Drive Animation Times", driveTime);
            items.add(mi2);
            mi2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    driveTime = mi2.isSelected();
                }
            });

        }


        if (canDoTimeFilters() && (getDisplayGroup() != null)) {
            List comps =
                getDisplayGroup().getAncestorGroup().findDisplayComponents(
                    ChartWrapper.class);
            JMenu   filterMenu = null;
            boolean didSep     = false;
            if (timeFilterSource != null) {
                filterMenu = new JMenu("Time Subset");
                didSep     = true;
                items.add(GuiUtils.MENU_SEPARATOR);
                items.add(filterMenu);
                filterMenu.add(GuiUtils.makeMenuItem("Remove",
                        ChartWrapper.this, "removeTimeFilterSource"));
            }


            for (int i = 0; i < comps.size(); i++) {
                ChartWrapper chartWrapper = (ChartWrapper) comps.get(i);
                if (chartWrapper.getTimeRanges() == null) {
                    continue;
                }
                if (filterMenu == null) {
                    filterMenu = new JMenu("Time Subset");
                    if ( !didSep) {
                        items.add(GuiUtils.MENU_SEPARATOR);
                    }
                    didSep = true;
                    items.add(filterMenu);
                }
                filterMenu.add(GuiUtils.makeMenuItem("From: "
                        + chartWrapper.getName(), ChartWrapper.this,
                            "setTimeFilterSource", chartWrapper));
            }

        }


        if (canDoTimeSelect() && (getDisplayGroup() != null)) {
            List comps =
                getDisplayGroup().getAncestorGroup().findDisplayComponents(
                    ChartWrapper.class);
            JMenu   filterMenu = null;
            boolean didSep     = false;
            if (timeSelectSource != null) {
                filterMenu = new JMenu("Time Select");
                didSep     = true;
                items.add(GuiUtils.MENU_SEPARATOR);
                items.add(filterMenu);
                filterMenu.add(GuiUtils.makeMenuItem("Remove",
                        ChartWrapper.this, "removeTimeSelectSource"));
            }

            for (int i = 0; i < comps.size(); i++) {
                ChartWrapper chartWrapper = (ChartWrapper) comps.get(i);
                if ((chartWrapper == this)
                        || !chartWrapper
                            .canBeASourceForTimeSelectionEvents()) {
                    continue;
                }

                if (filterMenu == null) {
                    filterMenu = new JMenu("Time Select");
                    if ( !didSep) {
                        items.add(GuiUtils.MENU_SEPARATOR);
                    }
                    didSep = true;
                    items.add(filterMenu);
                }
                filterMenu.add(GuiUtils.makeMenuItem("From: "
                        + chartWrapper.getName(), ChartWrapper.this,
                            "setTimeSelectSource", chartWrapper));
            }

        }

        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Properties...", this,
                                        "showProperties"));

        return items;

    }




    /**
     * Create, if needed, and return the interpreter
     *
     * @return The interpreter
     */
    protected PythonInterpreter getInterpreter() {
        if (interpreter == null) {
            interpreter =
                getDisplayControl().getControlContext().getIdv()
                    .getJythonManager().createInterpreter();
        }
        return interpreter;
    }

    /**
     * Get the jython text from the text field and apply it
     */
    public void applyJython() {
        jython = jythonFld.getText();
        applyJython(jython);
    }


    /**
     * Evaluate the jython. This is used so end users can write some jython to extract
     * and muck with the data in a chart.
     *
     * @param jython The jython
     */
    protected void applyJython(String jython) {
        initializeJython(getInterpreter());
        try {
            interpreter.eval(jython);
        } catch (Exception exc) {
            LogUtil.logException("Error evaluating Jython", exc);
        }
    }


    /**
     * Add the state of this chart to the interpreter
     *
     * @param interpreter The interpreter to initialize
     */
    protected void initializeJython(PythonInterpreter interpreter) {
        interpreter.set("chart", this);
    }

    /**
     * Should the jython field be shown in the properties
     *
     * @return Can this chart have jython applied to it
     */
    protected boolean canDoJython() {
        return false;
    }

    /**
     * Can this component be a source for time selection events
     *
     * @return false
     */
    protected boolean canBeASourceForTimeSelectionEvents() {
        return false;
    }


    /**
     * Can this chart use time subset filters.
     * This is used to determine whether the checkbox should be shown
     * in the menus
     *
     * @return     Can this chart use time subset filters.
     */
    protected boolean canDoTimeFilters() {
        return true;
    }

    /**
     * Can this chart use time selects
     * This is used to determine whether the checkbox should be shown
     * in the menus
     *
     * @return     Can this chart use time select
     */
    protected boolean canDoTimeSelect() {
        return false;
    }




    /**
     * Can this chart drive the times in the main display.
     * This is used to determine whether the checkbox should be shown
     * in the menus
     *
     * @return  Can this chart drive the times in the main display
     */
    protected boolean canDoDriveTime() {
        return false;
    }

    /**
     * Can this chart subset the entire data set on time
     *
     * @return Can do min max data subsetting
     */
    protected boolean canDoMinMaxDate() {
        return true;
    }


    /**
     * Should show resolution widget
     *
     * @return Should show resolution widget
     */
    protected boolean canDoResolution() {
        return true;
    }

    /**
     * Can we add fields
     *
     * @return ok to add fields
     */
    public boolean canDoParameters() {
        return true;
    }

    /**
     * Can we remove fields
     *
     * @return ok to add fields
     */
    public boolean canDoRemoveParameters() {
        return true;
    }

    /**
     * ok to show the data choice list
     *
     * @return ok to show the data choice list
     */
    public boolean canDoDataChoiceList() {
        return true;
    }


    /**
     * Can the color swatch be shown in the properties for the data choice wrappers.
     *
     * @return Can do wrapper colors
     */
    public boolean canDoWrapperColor() {
        return false;
    }

    /**
     * Can the Side menu be shown in the  properties for the data choice wrappers.
     *
     * @return Can do sides in properties
     */
    public boolean canDoWrapperSide() {
        return false;
    }



    /**
     * Handle the event
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(PROP_TIMERANGE)) {
            try {
                loadData();
            } catch (Exception exc) {
                LogUtil.logException("Error creating data set", exc);
            }
            return;
        } else if (event.getPropertyName().equals(PROP_REMOVED)) {
            Object source = event.getSource();
            if (source == timeFilterSource) {
                setTimeFilterSource(null);
            }
            if (source == timeSelectSource) {
                setTimeSelectSource(null);
            }
        }
        super.propertyChange(event);
    }


    /**
     * Cleanup the chart
     */
    public void doRemove() {
        super.doRemove();
        if (interpreter != null) {
            getDisplayControl().getControlContext().getIdv()
                .getJythonManager().removeInterpreter(interpreter);
            interpreter = null;
        }
        if (timeFilterSource != null) {
            timeFilterSource.removePropertyChangeListener(this);
            timeFilterSource = null;
        }
    }



    /*
    private boolean minOk(double min) {
        return min != Double.NEGATIVE_INFINITY;
    }


    private boolean maxOk(double max) {
        return max != Double.NEGATIVE_INFINITY;
    }

    private GregorianCalendar getMinCalendar() {
        GregorianCalendar cal =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        if (minOk(minDate)) {
            cal.setTime(new Date((long) minDate));
        } else if (minOk(dataMinDate)) {
            cal.setTime(new Date((long) dataMinDate));
        } else {
            cal = null;
        }
        return cal;
    }



    private GregorianCalendar getMaxCalendar() {
        GregorianCalendar cal =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        if (minOk(minDate)) {
            cal.setTime(new Date((long) minDate));
        } else if (minOk(dataMaxDate)) {
            cal.setTime(new Date((long) dataMaxDate));
        } else {
            cal = null;
        }
        return cal;
    }



    private GregorianCalendar getCalendar(Date d) {
        GregorianCalendar cal =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(d);
        return cal;
    }



    private void dateTimeChanged() {
        //        maxPicker.setMinSelectableTime(getCalendar(minPicker.getDate()));
        //        minPicker.setMaxSelectableTime(getCalendar(maxPicker.getDate()));

    }

    //    private DateTimePicker minPicker;
    //    private DateTimePicker maxPicker;

    */




    /**
     * Create the properties contents
     *
     * @param comps  List of components
     * @param tabIdx Which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }


        if (canDoResolution()) {
            comps.add(GuiUtils.rLabel("Resolution: "));
            resolutionSlider = new JSlider(0, 100, (int) (100 * resolution));
            comps.add(
                GuiUtils.vbox(
                    resolutionSlider,
                    GuiUtils.leftRight(
                        GuiUtils.lLabel("Low"), GuiUtils.rLabel("High"))));
        }

        /*
        if(canDoMinMaxDate() && minOk(dataMinDate) && maxOk(dataMaxDate)) {
            LocaleSpecificResources.setHourFormat(LocaleSpecificResources.HOUR_FORMAT_24);

            GregorianCalendar minCal = getMinCalendar();
            GregorianCalendar maxCal = getMaxCalendar();
            ActionListener listener = new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        dateTimeChanged();
                    }
                };

            minPicker = new DateTimePicker(minCal, "dd-MMM-yyyy HH:mm:ss z");
            minPicker.setDisplayTodayButton(false);
            minPicker.addActionListener(listener);
            minPicker.setMaxSelectableTime(maxCal);
            minPicker.setMinSelectableTime(getCalendar(new Date((long)dataMinDate)));


            maxPicker = new DateTimePicker(maxCal, "dd-MMM-yyyy HH:mm:ss z");
            maxPicker.setDisplayTodayButton(false);
            maxPicker.addActionListener(listener);
            maxPicker.setMinSelectableTime(minCal);
            maxPicker.setMaxSelectableTime(getCalendar(new Date((long)dataMaxDate)));


            JPanel datePanel = GuiUtils.hbox(minPicker, new JLabel(" to "), maxPicker);
            comps.add(GuiUtils.rLabel("Date Range: "));
            comps.add(GuiUtils.left(datePanel));
            }*/

        if (canDoJython()) {
            comps.add(GuiUtils.rLabel("Jython: "));
            jythonFld = new JTextField(jython, 40);
            comps.add(GuiUtils.centerRight(jythonFld,
                                           GuiUtils.makeButton("Apply", this,
                                               "applyJython")));
        }





        if (canDoDataChoiceList()) {
            chartOrderChanged = false;

            fieldProperties   = new ArrayList();
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx++) {
                DataChoiceWrapper wrapper =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);
                fieldProperties.add(new FieldProperties(wrapper));
            }
            wrapperPanel = new JPanel();

            updateWrapperPanel();



            JScrollPane sp =
                new JScrollPane(
                    GuiUtils.left(wrapperPanel),
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            sp.getVerticalScrollBar().setUnitIncrement(10);

            JViewport vp = sp.getViewport();
            sp.setPreferredSize(new Dimension(600, 200));

            comps.add(GuiUtils.top(GuiUtils.rLabel("Fields:")));
            comps.add(sp);
        }



    }




    /**
     * Move the field in the properties list
     *
     * @param idx Which one
     * @param down up or down
     */
    private void moveFieldInProperties(int idx, boolean down) {
        if (down && (idx == fieldProperties.size() - 1)) {
            return;
        }
        if ( !down && (idx == 0)) {
            return;
        }
        Object o2     = fieldProperties.remove(idx);
        int    newIdx = (down
                         ? idx + 1
                         : idx - 1);
        fieldProperties.add(newIdx, o2);
        updateWrapperPanel();

    }

    /**
     * Class FieldProperties holds state for the data choice wrappers in the properties dialog
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.46 $
     */
    protected static class FieldProperties {

        /** state */
        DataChoiceWrapper wrapper;

        /** state */
        JCheckBox removeCbx;

        /** state */
        JComboBox sideCbx;

        /** state */
        JTextField nameFld;

        /** state */
        JComponent displayComp;

        /**
         * ctor
         *
         * @param wrapper The wrapper we represent
         */
        public FieldProperties(DataChoiceWrapper wrapper) {

            this.wrapper   = wrapper;
            nameFld        = new JTextField(wrapper.getDescription(), 15);
            displayComp    =
                this.wrapper.getLineState().getPropertyContents();
            this.removeCbx = new JCheckBox(" ", false);
            this.sideCbx = GuiUtils.makeComboBox(DataChoiceWrapper.SIDES,
                    DataChoiceWrapper.SIDELABELS, wrapper.getSide());
        }

        /**
         * apply the properties
         *
         * @return success
         */
        public boolean applyProperties() {
            if ( !wrapper.getLineState().applyProperties()) {
                return false;
            }
            wrapper.setMyDescription(nameFld.getText().trim());
            wrapper.setSide(GuiUtils.getValueFromBox(sideCbx));
            return true;
        }



    }



    /**
     * reload the wrapper panel in the properties gui
     */
    private void updateWrapperPanel() {
        List wrapperComps = new ArrayList();
        wrapperComps.add(new JLabel("  "));
        wrapperComps.add(new JLabel("Remove   "));
        wrapperComps.add(new JLabel("Field    "));
        int columns = 3;
        if (canDoWrapperColor()) {
            wrapperComps.add(GuiUtils.cLabel("Width/Color/Style"));
            columns++;
        }
        if (canDoWrapperSide()) {
            wrapperComps.add(GuiUtils.cLabel("Side"));
            columns++;
        }


        for (int i = 0; i < fieldProperties.size(); i++) {
            FieldProperties fieldProperty =
                (FieldProperties) fieldProperties.get(i);
            JButton upButton =
                GuiUtils.getImageButton("/auxdata/ui/icons/Up16.gif",
                                        getClass());
            JButton downButton =
                GuiUtils.getImageButton("/auxdata/ui/icons/Down16.gif",
                                        getClass());
            downButton.addActionListener(new ObjectListener(new Integer(i)) {
                public void actionPerformed(ActionEvent ae) {
                    moveFieldInProperties(((Integer) theObject).intValue(),
                                          true);
                }
            });
            upButton.addActionListener(new ObjectListener(new Integer(i)) {
                public void actionPerformed(ActionEvent ae) {
                    moveFieldInProperties(((Integer) theObject).intValue(),
                                          false);
                }
            });
            wrapperComps.add(GuiUtils.hbox(upButton, downButton));
            if (canDoRemoveParameters()) {
                wrapperComps.add(fieldProperty.removeCbx);
            }
            wrapperComps.add(GuiUtils.wrap(fieldProperty.nameFld));
            if (canDoWrapperColor()) {
                wrapperComps.add(doMakeWrapperDisplayComponent(i,
                        fieldProperty));
            }
            if (canDoWrapperSide()) {
                wrapperComps.add(GuiUtils.wrap(fieldProperty.sideCbx));
            }
        }
        wrapperPanel.removeAll();
        wrapperPanel.setLayout(new BorderLayout());
        GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);
        JPanel guts = GuiUtils.doLayout(wrapperComps, columns,
                                        GuiUtils.WT_NNN, GuiUtils.WT_N);
        wrapperPanel.add(BorderLayout.NORTH, guts);
        wrapperPanel.validate();
        wrapperPanel.repaint();
    }



    /**
     * Make the widget for the field
     *
     * @param idx which one
     * @param fieldProperty The wrapper wrapper
     *
     * @return The gui
     */
    protected JComponent doMakeWrapperDisplayComponent(int idx,
            FieldProperties fieldProperty) {
        return GuiUtils.inset(fieldProperty.displayComp, 4);
    }




    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if (resolutionSlider != null) {
            double newResolution = resolutionSlider.getValue() / 100.0;
            if (newResolution != resolution) {
                resolution = newResolution;
            }
        }


        /*
        if(maxPicker!=null) {
            minDate = minPicker.getDate().getTime();
            maxDate = maxPicker.getDate().getTime();
            }*/



        if (canDoDataChoiceList()) {
            dataChoiceWrappers = new ArrayList();
            List tmpFields = fieldProperties;
            fieldProperties = new ArrayList();
            for (int i = 0; i < tmpFields.size(); i++) {
                FieldProperties fieldProperty =
                    (FieldProperties) tmpFields.get(i);
                if ( !fieldProperty.applyProperties()) {
                    return false;
                }
            }

            for (int i = 0; i < tmpFields.size(); i++) {
                FieldProperties fieldProperty =
                    (FieldProperties) tmpFields.get(i);
                if (fieldProperty.removeCbx.isSelected()) {
                    continue;
                }
                dataChoiceWrappers.add(fieldProperty.wrapper);
                fieldProperties.add(fieldProperty);
            }
            updateWrapperPanel();
        }

        if (jythonFld != null) {
            jython = jythonFld.getText();
        }


        return true;
    }


    /**
     *  Apply the properties
     *
     *
     * @return success
     */
    protected boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }
        try {
            loadData();
        } catch (Exception exc) {
            LogUtil.logException("Error loading data", exc);
            return false;
        }
        return true;
    }



    /**
     * Add a field to thei chart
     */
    public void addField() {
        getDisplayControl().addFieldToChartWrapper(this);
    }

    /**
     * Returns the list of labels used for selecting data choices.
     * If a derived class needs more than one data choice they
     * can override this.
     *
     * @return List of field labels
     */
    public List getFieldSelectionLabels() {
        return Misc.newList("Choose Fields for Chart: ");
    }

    /**
     * When selecting data does the data tree support multiple selections
     *
     * @return Do multiples
     */
    public boolean doMultipleAddFields() {
        return true;
    }

    /**
     * utility to format the value
     *
     * @param v the value
     *
     * @return the value formatted
     */
    public String formatValue(double v) {
        return getDisplayControl().formatValue(v);
    }



    /**
     * Add the dta choice
     *
     * @param dataChoice the choice
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void addDataChoice(DataChoice dataChoice)
            throws VisADException, RemoteException {
        dataChoiceWrappers.add(createDataChoiceWrapper(dataChoice));
        loadData();
    }



    /**
     * Add the data choices
     *
     * @param newDataChoices the choices
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void addDataChoices(List newDataChoices)
            throws VisADException, RemoteException {
        dataChoiceWrappers.addAll(wrapDataChoices(newDataChoices));
        loadData();
    }


    /**
     * create shapes for an individual time step.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {}


    /**
     * Animation in main display changed. Some charts show this
     *
     * @param time  the animation time
     */
    public void setTimeFromAnimation(Real time) {}


    /**
     * Use the animation time to set the domain crosshairs
     *
     * @param time The time
     */
    public void animationTimeChanged(Real time) {
        super.animationTimeChanged(time);
        if (showTime) {
            setTimeFromAnimation(time);
        }
    }


    /**
     *  Set the DataChoiceWrappers property.
     *
     *  @param value The new value for DataChoiceWrappers
     */
    public void setDataChoiceWrappers(List value) {
        dataChoiceWrappers = value;
    }

    /**
     *  Get the DataChoiceWrappers property.
     *
     *  @return The DataChoiceWrappers
     */
    public List getDataChoiceWrappers() {
        return dataChoiceWrappers;
    }







    /**
     * Set the Resolution property.
     *
     * @param value The new value for Resolution
     */
    public void setResolution(double value) {
        resolution = value;
    }

    /**
     * Get the Resolution property.
     *
     * @return The Resolution
     */
    public double getResolution() {
        return resolution;
    }



    /**
     * Create and return a list of the data choices held by the data choice wrappers.
     *
     * @return List of data choices
     */
    public List getDataChoices() {
        List result = new ArrayList();
        for (int i = 0; i < dataChoiceWrappers.size(); i++) {
            result.add(
                ((DataChoiceWrapper) dataChoiceWrappers.get(
                    i)).getDataChoice());
        }
        return result;
    }


    /**
     * Remove the current time filter  source
     */
    public void removeTimeFilterSource() {
        setTimeFilterSource(null);
    }


    /**
     *  Set the FilterSource property.
     *
     *  @param value The new value for FilterSource
     */
    public void setTimeFilterSource(TimeSeriesChartWrapper value) {
        if (timeFilterSource != null) {
            timeFilterSource.removePropertyChangeListener(this);
        }
        timeFilterSource = value;
        if (timeFilterSource != null) {
            timeFilterSource.addPropertyChangeListener(this);
        }
        try {
            if (hasBeenInitialized) {
                loadData();
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }
    }





    /**
     *  Get the FilterSource property.
     *
     *  @return The FilterSource
     */
    public TimeSeriesChartWrapper getTimeFilterSource() {
        return timeFilterSource;
    }



    /**
     * Remove the current time select  source
     */
    public void removeTimeSelectSource() {
        setTimeSelectSource(null);
    }


    /**
     *  Set the SelectSource property.
     *
     *  @param value The new value for SelectSource
     */
    public void setTimeSelectSource(ChartWrapper value) {
        if (timeSelectSource != null) {
            timeSelectSource.removePropertyChangeListener(this);
        }
        timeSelectSource = value;
        if (timeSelectSource != null) {
            timeSelectSource.addPropertyChangeListener(this);
        }
        try {
            if (hasBeenInitialized) {
                loadData();
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }
    }





    /**
     *  Get the SelectSource property.
     *
     *  @return The SelectSource
     */
    public ChartWrapper getTimeSelectSource() {
        return timeSelectSource;
    }




    /**
     * Utility to pull subset the given samples based on the time filter ranges
     *
     * @param samples The data
     * @param timeValues The times
     *
     * @return The sampled data/time
     *
     * @throws RemoteException On badness_
     * @throws VisADException On badness_
     */
    protected double[][] filterData(double[] samples, double[] timeValues)
            throws VisADException, RemoteException {
        if (timeValues == null) {
            return new double[][] {
                samples
            };
        }

        List timeRanges    = getTimeFilterRanges();
        int  size          = samples.length;
        int  resolutionCnt = (int) (getResolution() * size);
        int  stride        = 1;
        while (size / stride > resolutionCnt) {
            stride++;
        }
        int      valueIdx      = 0;
        double[] values        = new double[size];
        double[] tmpTimeValues = new double[size];

        for (int i = 0; i < size; i += stride) {
            if (timeRanges != null) {
                boolean ok        = false;
                double  timeValue = timeValues[i];
                for (int rangeIdx = 0; !ok && (rangeIdx < timeRanges.size());
                        rangeIdx++) {
                    ucar.unidata.util.Range r =
                        (ucar.unidata.util.Range) timeRanges.get(rangeIdx);
                    if ((timeValue >= r.getMin())
                            && (timeValue <= r.getMax())) {
                        ok = true;
                    }
                }
                if ( !ok) {
                    continue;
                }
            }
            tmpTimeValues[valueIdx] = timeValues[i];
            values[valueIdx++]      = samples[i];

        }

        double[] actualValues     = new double[valueIdx];
        double[] actualTimeValues = new double[valueIdx];
        for (int i = 0; i < valueIdx; i++) {
            actualValues[i]     = values[i];
            actualTimeValues[i] = tmpTimeValues[i];
        }
        return new double[][] {
            actualValues, actualTimeValues
        };
    }


    /**
     * Convert the time from the data (in samples[1]) into an array
     * of milliseconds
     *
     * @param samples The data
     * @param data Where the data came from
     *
     * @return The times
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getTimeValues(double[][] samples, FlatField data)
            throws VisADException, RemoteException {
        if (samples.length < 2) {
            return null;
        }
        Unit[] units = ucar.visad.Util.getDefaultRangeUnits((FlatField) data);
        double[] timeValues =
            CommonUnit.secondsSinceTheEpoch.toThis(samples[1], units[1]);


        for (int i = 0; i < timeValues.length; i++) {
            timeValues[i] *= 1000;
            if ((i == 0) || (timeValues[i] < dataMinDate)) {
                dataMinDate = timeValues[i];
            }
            if ((i == 0) || (timeValues[i] > dataMaxDate)) {
                dataMaxDate = timeValues[i];
            }
        }

        return timeValues;
    }




    /**
     *  Set the ShowTime property.
     *
     *  @param value The new value for ShowTime
     */
    public void setShowTime(boolean value) {
        showTime = value;
    }

    /**
     *  Get the ShowTime property.
     *
     *  @return The ShowTime
     */
    public boolean getShowTime() {
        return showTime;
    }



    /**
     *  Set the DriveTime property.
     *
     *  @param value The new value for DriveTime
     */
    public void setDriveTime(boolean value) {
        driveTime = value;
    }

    /**
     *  Get the DriveTime property.
     *
     *  @return The DriveTime
     */
    public boolean getDriveTime() {
        return driveTime;
    }

    /**
     *  Set the MinDate property.
     *
     *  @param value The new value for MinDate
     */
    public void setMinDate(double value) {
        minDate = value;
    }

    /**
     *  Get the MinDate property.
     *
     *  @return The MinDate
     */
    public double getMinDate() {
        return minDate;
    }

    /**
     *  Set the MaxDate property.
     *
     *  @param value The new value for MaxDate
     */
    public void setMaxDate(double value) {
        maxDate = value;
    }

    /**
     *  Get the MaxDate property.
     *
     *  @return The MaxDate
     */
    public double getMaxDate() {
        return maxDate;
    }


    /**
     * Set the Jython property.
     *
     * @param value The new value for Jython
     */
    public void setJython(String value) {
        jython = value;
    }

    /**
     * Get the Jython property.
     *
     * @return The Jython
     */
    public String getJython() {
        return jython;
    }

    /**
     * Set the AnimationShareGroup property.
     *
     * @param value The new value for AnimationShareGroup
     */
    public void setAnimationShareGroup(String value) {
        animationShareGroup = value;
    }

    /**
     * Get the AnimationShareGroup property.
     *
     * @return The AnimationShareGroup
     */
    public String getAnimationShareGroup() {
        return animationShareGroup;
    }






}

