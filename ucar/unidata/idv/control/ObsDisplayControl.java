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


import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.point.*;

import ucar.unidata.gis.SpatialGrid;

import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.TwoListPanel;
import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import ucar.visad.ShapeUtility;
import ucar.visad.Util;



import visad.*;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.*;



/**
 * A DisplayControl for station models
 *
 * @author MetApps Development Team
 * @version $Revision: 1.58 $
 */

public abstract class ObsDisplayControl extends DisplayControlImpl {


    /**
     * Have this around to force the compilation of
     */
    private static final ucar.unidata.ui.PropertyFilter dummyFilter = null;


    /** Represents when we use all fields */
    protected static final String FIELD_ALL = "*";

    /** decimal formatter */
    protected static DecimalFormat format = new DecimalFormat("####.0");

    /** column string */
    protected String colString = FIELD_ALL;

    /** Label for latitude fields */
    protected static String LABEL_LAT = "Latitude";

    /** Label for longitude fields */
    protected static String LABEL_LON = "Longitude";

    /** Label for altitude fields */
    protected static String LABEL_ALT = "Altitude";

    /** Label for Time fields */
    protected static String LABEL_TIME = "Date/Time";

    /** Holds the column names */
    protected List colNames = new ArrayList();

    /** Holds the col ids */
    private JTextField colField;

    /** The dialog window that shows the field selector panel */
    private JDialog selectorWindow;

    /** Used in the field selector */
    private TwoListPanel twoListPanel;

    /** Should data in tables be shown raw */
    private boolean showDataRaw = false;

    /** template for parameter readouts */
    private String dataTemplate = null;

    /** Used to declutter on time */
    private double timeDeclutterMinutes = 1.0;

    /** The two time declutter components */
    JComponent[] timeDeclutterComps;

    /** Holds the timeDeclutterMinutes */
    private JTextField timeDeclutterFld;

    /** Is time clutter enabled */
    private boolean timeDeclutterEnabled = false;

    /** checkbox */
    private JCheckBox timeDeclutterCbx;

    /** Hashtable of special names to labels */
    private Hashtable nameToLabel;

    /** ignore changes */
    private boolean ignoreTimeDeclutterEnabled = false;

    /**
     * Default ctor
     */
    public ObsDisplayControl() {
        initLabels();
    }


    /**
     * Remove this displayable
     *
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (selectorWindow != null) {
            selectorWindow.dispose();
        }
    }


    /**
     * export the point data as a  netcdf file
     */
    public void exportAsNetcdf() {
        try {
            JComboBox publishCbx =
                getIdv().getPublishManager().getSelector("nc.export");
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_NETCDF,
                                         FileManager.SUFFIX_NETCDF,
                                         ((publishCbx != null)
                                          ? GuiUtils.top(publishCbx)
                                          : null));
            if (filename == null) {
                return;
            }
            PointDataInstance pdi = (PointDataInstance) getDataInstance();
            PointObFactory.writeToNetcdf(new java.io.File(filename),
                                         pdi.getTimeSequence());
            getIdv().getPublishManager().publishContent(filename, null,
                    publishCbx);
        } catch (Exception exc) {
            logException("Exporting point data to netcdf", exc);
        }
    }


    /**
     * Get the column value  and add it to the html
     *
     * @param data   data to search
     * @param type   mathtype of the parameter for this column
     * @param useFormatPref use parameter readout preference for formatting
     * @return String representation of the data
     */
    protected Object getColValue(Data data, MathType type,
                                 boolean useFormatPref) {

        Unit displayUnit = null;
        if (data instanceof Real) {
            displayUnit =
                getDisplayConventions().getDisplayUnit(type.toString(),
                    ((Real) data).getUnit());
        }
        return getColValue(data, type, useFormatPref, displayUnit);
    }


    /**
     *  Get the column value  and add it to the html
     *
     *  @param data   data to search
     *  @param type   mathtype of the parameter for this column
     *  @param useFormatPref use parameter readout preference for formatting
     *  @param displayUnit The unit to use
     *  @return String representation of the data
     */

    protected Object getColValue(Data data, MathType type,
                                 boolean useFormatPref, Unit displayUnit) {
        if ( !(data instanceof Real)) {
            return data.toString();
        }
        try {
            Real r = (Real) data;
            if (getShowDataRaw()) {
                return new Double(r.getValue());
            }
            double value;
            if (displayUnit != null) {
                value = r.getValue(displayUnit);
            } else {
                value = r.getValue();
            }

            //Is this a nan
            if ( !(value == value)) {
                return new RealWrapper(getDisplayConventions().format(value),
                                       r);
            } else if (useFormatPref) {
                String valueStr = null;
                if ((displayUnit != null)
                        && !displayUnit.toString().equals("")) {
                    if (dataTemplate == null) {
                        dataTemplate = getObjectStore().get(PREF_PROBEFORMAT,
                                DEFAULT_PROBEFORMAT);
                        dataTemplate = dataTemplate.trim();
                        if (dataTemplate.equals("")) {
                            dataTemplate = "%value%";
                        }
                    }

                    valueStr = getDisplayConventions().format(value);
                    valueStr = StringUtil.replace(dataTemplate, "%value%",
                            valueStr);
                    valueStr = StringUtil.replace(valueStr, "%rawvalue%",
                            "" + r.getValue());
                    valueStr = StringUtil.replace(valueStr, "%unit%",
                            displayUnit.toString());
                    valueStr = StringUtil.replace(valueStr, "%rawunit%",
                            "" + r.getUnit());
                    valueStr = "<html>" + valueStr + "</html>";
                }
                if (valueStr == null) {
                    valueStr = getDisplayConventions().format(value);
                }
                return new RealWrapper(valueStr, r);
            } else {
                return new RealWrapper(getDisplayConventions().format(value),
                                       r);
            }
        } catch (Exception exc) {
            return "error:" + exc;
        }
    }



    /**
     * Get the unit string for the specified Scalar
     *
     * @param data  real/text value
     * @return String representation of the unit or null
     */
    protected String getDisplayUnitName(Scalar data) {
        Unit displayUnit = getDisplayUnit(data);
        if ((displayUnit == null) || displayUnit.toString().equals("")) {
            return null;
        }
        return displayUnit.toString();
    }


    /**
     * Get the display unit to use for the data
     *
     * @param data data
     *
     * @return display unit
     */
    protected Unit getDisplayUnit(Scalar data) {
        if ( !(data instanceof Real)) {
            return null;
        }
        Real r           = (Real) data;

        Unit displayUnit = (getShowDataRaw())
                           ? r.getUnit()
                           : getDisplayConventions().getDisplayUnit(
                               r.getType().toString(), r.getUnit());

        if ((displayUnit == null) || displayUnit.toString().equals("")) {
            return null;
        }
        return displayUnit;
    }

    /**
     * Apply the preferences.  Used to pick up the date format changes.
     */
    public void applyPreferences() {
        super.applyPreferences();
        dataTemplate = null;
        fieldSelectorChanged();
    }

    /**
     * Declutter in time.
     *
     * @param  obs initial field of observations.
     *
     * @return a 'time' decluttered version of obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected FieldImpl doDeclutterTime(FieldImpl obs)
            throws VisADException, RemoteException {
        boolean isTimeSequence = GridUtil.isTimeSequence(obs);
        if ( !isTimeSequence) {
            return obs;
        }

        Set       timeSet    = obs.getDomainSet();
        int       numTimes   = timeSet.getLength();
        List      timeFields = new ArrayList();
        List      timeValues = new ArrayList();
        Hashtable seenTime   = new Hashtable();
        int       seconds    = (int) (timeDeclutterMinutes * 60);
        if (seconds == 0) {
            seconds = 1;
        }
        //        System.out.println ("***********  numTimes=" + numTimes);
        for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
            FieldImpl oneTime = (FieldImpl) obs.getSample(timeIdx);
            PointOb   ob      = null;
            int       numObs  = oneTime.getDomainSet().getLength();
            for (int obIdx = 0; obIdx < numObs; obIdx++) {
                PointOb tmpOb = (PointOb) oneTime.getSample(obIdx);
                double  time  = tmpOb.getDateTime().getValue();
                //Humm, why do we have this check here?
                if ((time > 0) || (time < 0)) {
                    ob = tmpOb;
                    break;
                }
            }
            if (ob == null) {
                continue;
            }
            Integer timeKey = new Integer(((int) ob.getDateTime().getValue())
                                          / seconds);
            //Include the last time step
            if ((timeIdx < numTimes - 1) && (seenTime.get(timeKey) != null)) {
                //                System.out.println ("   skipping:" + ob.getDateTime() + "  " + timeKey);
                continue;
            }
            seenTime.put(timeKey, timeKey);
            timeFields.add(oneTime);
            timeValues.add(ob.getDateTime());
            //            System.out.println ("   Using:" + ob.getDateTime());
        }



        if (timeValues.size() == 0) {
            System.err.println("found:" + timeValues.size());
            return obs;
        }


        DateTime[] times =
            (DateTime[]) timeValues.toArray(new DateTime[timeValues.size()]);
        FieldImpl newField = new FieldImpl((FunctionType) obs.getType(),
                                           DateTime.makeTimeSet(times));
        for (int i = 0; i < timeFields.size(); i++) {
            newField.setSample(i, (FieldImpl) timeFields.get(i), false);
        }
        return newField;
    }



    /**
     * Utility to make the timekey used in time decluttering
     *
     * @param ob The ob
     *
     * @return The key
     */
    Object getTimeKey(PointOb ob) {
        int seconds = (int) (timeDeclutterMinutes * 60);
        return new Integer(((int) ob.getDateTime().getValue()) / seconds);
    }




    /**
     * Handle when the time declutering state has changed
     */
    protected void timeDeclutterChanged() {}


    /**
     * Make the gui panel for the time decluttering
     *
     * @return The time declutter panel
     */
    protected JComponent[] getTimeDeclutterComps() {
        if (timeDeclutterComps != null) {
            return timeDeclutterComps;
        }
        ActionListener timeDeclutterListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    if (ignoreTimeDeclutterEnabled) {
                        return;
                    }
                    timeDeclutterMinutes =
                        Misc.parseNumber(timeDeclutterFld.getText().trim());
                    //Only do this when there was a change in the enabled
                    //or (when the text field had a return pressed event)
                    //when the value changed and we are enabled
                    if (timeDeclutterEnabled
                            != timeDeclutterCbx.isSelected()) {
                        timeDeclutterEnabled = timeDeclutterCbx.isSelected();
                        timeDeclutterChanged();
                    } else if (timeDeclutterEnabled) {
                        timeDeclutterChanged();
                    }
                } catch (NumberFormatException nfe) {
                    userErrorMessage("Bad number format");
                }
            }
        };
        //timeDeclutterFld = new JTextField("" + getTimeDeclutterMinutes(), 5);
        timeDeclutterFld = new JTextField(
            getDisplayConventions().format(getTimeDeclutterMinutes()), 5);
        timeDeclutterFld.addActionListener(timeDeclutterListener);
        timeDeclutterCbx = new JCheckBox("", getTimeDeclutterEnabled());
        timeDeclutterCbx.addActionListener(timeDeclutterListener);
        return timeDeclutterComps = new JComponent[] { timeDeclutterCbx,
                timeDeclutterFld };
    }



    /**
     * A utility to determine if the given param name is the station id
     *
     * @param name The param name
     *
     * @return Is it a station id
     */
    protected boolean isIdParam(String name) {
        name = StringUtil.replace(name, "(Text)", "").trim();
        if (name.equalsIgnoreCase(PointOb.PARAM_ID)
                || name.equalsIgnoreCase(PointOb.PARAM_IDN)) {
            return true;
        }
        String canonical = DataAlias.aliasToCanonical(name);
        if (canonical != null) {
            return canonical.equals(PointOb.PARAM_ID)
                   || canonical.equals(PointOb.PARAM_IDN);
        }
        return false;
    }

    /**
     * Get the label to show to the user for the given param name.
     * This handles lat,lon,alt and time special.
     *
     * @param name The param name
     *
     * @return The label to use.
     */
    protected String getParamLabel(String name) {
        if (nameToLabel == null) {
            initLabels();
        }
        String s = (String) nameToLabel.get(name);
        return (s != null)
               ? s
               : name;
    }

    /**
     * Initialize the labels hash table
     */
    private void initLabels() {
        nameToLabel = new Hashtable();
        nameToLabel.put(PointOb.PARAM_LAT, LABEL_LAT);
        nameToLabel.put(PointOb.PARAM_LON, LABEL_LON);
        nameToLabel.put(PointOb.PARAM_ALT, LABEL_ALT);
        nameToLabel.put(PointOb.PARAM_TIME, LABEL_TIME);
    }

    /**
     * Return a list of the names of the fields to use
     *
     * @param tt The types we get the names from
     *
     * @return List of field names to use
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected List getFieldsToShow(TupleType tt)
            throws VisADException, RemoteException {
        if (colString.equals(FIELD_ALL)) {
            MathType[] comps = tt.getComponents();
            List       names = new ArrayList();
            names.add(PointOb.PARAM_TIME);
            names.add(PointOb.PARAM_LAT);
            names.add(PointOb.PARAM_LON);
            names.add(PointOb.PARAM_ALT);
            for (int i = 0; i < comps.length; i++) {
                String param = Util.cleanTypeName(comps[i].toString());
                names.add(param);
            }
            return names;
        }
        return StringUtil.split(colString, ",");
    }

    /**
     * Return an array of the indexes in the obs to use
     *
     * @param tt The type
     *
     * @return Indices to use
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected int[] getIndicesToShow(TupleType tt)
            throws VisADException, RemoteException {
        List       cols    = getFieldsToShow(tt);
        int[]      indices = new int[cols.size()];
        MathType[] comps   = tt.getComponents();
        for (int compIdx = 0; compIdx < comps.length; compIdx++) {}
        for (int colIdx = 0; colIdx < cols.size(); colIdx++) {
            String colName = cols.get(colIdx).toString();
            indices[colIdx] = getIndex(tt, colName);
            if (indices[colIdx] == PointOb.BAD_INDEX) {
                indices[colIdx] = getIndex(tt, colName.toLowerCase());
                if (indices[colIdx] == PointOb.BAD_INDEX) {
                    indices[colIdx] = getIndex(tt, colName.toUpperCase());
                }
            }
        }
        return indices;
    }



    /**
     * Get the index of the comma separated names of values in the tuple
     *
     * @param tType     tuple type to search
     * @param commaSeparatedNames   list of possible names (aliases)
     *
     * @return  index in the tuple or -1 if not found
     */
    protected int getIndex(TupleType tType, String commaSeparatedNames) {
        List l = StringUtil.split(commaSeparatedNames, ",", true, true);
        for (int i = 0; i < l.size(); i++) {
            String name = l.get(i).toString();
            //A hack to make sure we don't pick up a param labeled "time" instead of the pointob time
            if (name.equals(PointOb.PARAM_TIME)) {
                return PointOb.BAD_INDEX;
            }

            // first check to see if the name is good before aliases
            int index = Util.getIndex(tType, name);
            if (index != PointOb.BAD_INDEX) {
                return index;
            }
            List aliases = DataAlias.getAliasesOf(name);
            if ((aliases == null) || aliases.isEmpty()) {
                continue;
            }
            for (int aliasIdx = 0; aliasIdx < aliases.size(); aliasIdx++) {
                String alias = (String) aliases.get(aliasIdx);
                index = Util.getIndex(tType, alias);
                if (index != PointOb.BAD_INDEX) {
                    return index;
                }
            }
        }
        return PointOb.BAD_INDEX;
    }



    /**
     * Handle the event from the field selector window
     *
     * @param ae The event
     */
    private void handleFieldSelectorEvent(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_APPLY) || cmd.equals(GuiUtils.CMD_OK)) {
            colString = StringUtil.join(
                ",",
                TwoFacedObject.getIdStrings(
                    twoListPanel.getCurrentEntries()));

            fieldSelectorChanged();
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            selectorWindow.setVisible(false);
        }
    }

    /**
     * Show the field selector window
     */
    public void showFieldSelector() {
        if (selectorWindow == null) {
            twoListPanel = new TwoListPanel(new ArrayList(), "All Fields",
                                            new ArrayList(),
                                            "Current Fields", null);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    handleFieldSelectorEvent(ae);
                }
            };
            JPanel buttons = GuiUtils.makeButtons(listener,
                                 new String[] { GuiUtils.CMD_APPLY,
                    GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });

            JPanel contents = GuiUtils.centerBottom(twoListPanel, buttons);
            selectorWindow = GuiUtils.createDialog(null, "Field Selector",
                    false);
            selectorWindow.getContentPane().add(contents);
            selectorWindow.pack();
        }

        try {
            List      allFields = new ArrayList();
            TupleType tt        = getTupleType();
            if (tt == null) {
                return;
            }
            MathType[] comps = tt.getComponents();
            allFields.add(
                new TwoFacedObject(
                    getParamLabel(PointOb.PARAM_TIME), PointOb.PARAM_TIME));
            allFields.add(
                new TwoFacedObject(
                    getParamLabel(PointOb.PARAM_LAT), PointOb.PARAM_LAT));
            allFields.add(
                new TwoFacedObject(
                    getParamLabel(PointOb.PARAM_LON), PointOb.PARAM_LON));
            allFields.add(
                new TwoFacedObject(
                    getParamLabel(PointOb.PARAM_ALT), PointOb.PARAM_ALT));
            for (int compIdx = 0; compIdx < comps.length; compIdx++) {
                String name = Util.cleanTypeName(comps[compIdx]);
                allFields.add(new TwoFacedObject(name));
            }
            List selectedFields;
            if (colString.equals(FIELD_ALL)) {
                selectedFields = new ArrayList(allFields);
            } else {
                List tfos  = new ArrayList();
                List names = StringUtil.split(colString, ",", true, true);
                selectedFields = new ArrayList();
                for (int i = 0; i < names.size(); i++) {
                    String id = (String) names.get(i);
                    selectedFields.add(new TwoFacedObject(getParamLabel(id),
                            id));
                }
            }

            twoListPanel.reinitialize(allFields, selectedFields);


            selectorWindow.setVisible(true);
        } catch (Exception exc) {
            logException("Showing field selector", exc);
        }
    }

    /**
     * Make a selector for showing raw data
     *
     * @return  a container for this selector
     */
    protected JPanel doMakeShowRawSelector() {
        JCheckBox rawCbx = new JCheckBox("Show Raw Data", getShowDataRaw());
        rawCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JCheckBox cbox = (JCheckBox) ae.getSource();
                if (cbox.isSelected() != getShowDataRaw()) {
                    setShowDataRaw(cbox.isSelected());
                    fieldSelectorChanged();
                }
            }
        });
        return GuiUtils.right(rawCbx);
    }

    /**
     * Make the UI contents for this control.
     *
     * @return  a container for this selector
     */
    protected JPanel doMakeFieldSelector() {
        JButton showBtn = new JButton("Select Fields");
        showBtn.setToolTipText("Change the  fields that are being displayed");
        showBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showFieldSelector();
            }
        });
        return GuiUtils.left(showBtn);
    }

    /**
     * Used to notify derived classes of when the field selector has changed
     */
    protected void fieldSelectorChanged() {}


    /**
     * Override this in derived classes to get the TupleType of the obs
     *
     * @return The tuple type
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected TupleType getTupleType()
            throws RemoteException, VisADException {
        return null;
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
     *
     * @return                 The created
     *                         {@link ucar.unidata.data.DataInstance} or
     *                         <code>null</code>.
     *
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return new PointDataInstance(dataChoice, getDataSelection(),
                                     getRequestProperties());
    }


    /**
     * Set the ColString property.
     *
     * @param value The new value for ColString
     */
    public void setColString(String value) {
        colString = value;
    }

    /**
     * Get the ColString property.
     *
     * @return The ColString
     */
    public String getColString() {
        return colString;
    }

    /**
     * Set the ShowDataRaw property.
     *
     * @param value The new value for ShowDataRaw
     */
    public void setShowDataRaw(boolean value) {
        showDataRaw = value;
        fieldSelectorChanged();
    }

    /**
     * Get the ShowDataRaw property.
     *
     * @return The ShowDataRaw
     */
    public boolean getShowDataRaw() {
        return showDataRaw;
    }




    /**
     * Set the TimeDeclutterMinutes property.
     *
     * @param value The new value for TimeDeclutterMinutes
     */
    public void setTimeDeclutterMinutes(double value) {
        timeDeclutterMinutes = value;
    }

    /**
     * Get the TimeDeclutterMinutes property.
     *
     * @return The TimeDeclutterMinutes
     */
    public double getTimeDeclutterMinutes() {
        return timeDeclutterMinutes;
    }


    /**
     * Set the TimeDeclutterEnabled property.
     *
     * @param value The new value for TimeDeclutterEnabled
     */
    public void setTimeDeclutterEnabled(boolean value) {
        timeDeclutterEnabled = value;
        if ((timeDeclutterCbx != null)
                && (value != timeDeclutterCbx.isSelected())) {
            ignoreTimeDeclutterEnabled = true;
            timeDeclutterCbx.setSelected(value);
            ignoreTimeDeclutterEnabled = false;
        }
    }

    /**
     * Add properties to the display settings dialog
     *
     * @param dsd display settings dialog
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(new Boolean(getTimeDeclutterEnabled()),
                             "timeDeclutterEnabled", "Subset Times",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Float(getTimeDeclutterMinutes()),
                             "timeDeclutterMinutes", "Subset Interval (min)",
                             SETTINGS_GROUP_DISPLAY);
    }

    /**
     * Get the TimeDeclutterEnabled property.
     *
     * @return The TimeDeclutterEnabled
     */
    public boolean getTimeDeclutterEnabled() {
        return timeDeclutterEnabled;
    }

    /**
     * Class RealWrapper Used in the JTable to show a formatted string and support sorting on the Real value
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.58 $
     */
    protected static class RealWrapper implements Comparable {

        /** The label */
        String lbl;

        /** The value */
        Real value;

        /**
         * ctor
         *
         * @param lbl label
         * @param value value
         */
        public RealWrapper(String lbl, Real value) {
            this.lbl   = lbl;
            this.value = value;
        }

        /**
         * Compare
         *
         * @param that to that
         *
         * @return comparison
         */
        public int compareTo(Object that) {
            if (that instanceof RealWrapper) {
                return value.compareTo(((RealWrapper) that).value);
            }
            return this.toString().compareTo(that.toString());
        }

        /**
         * to string
         *
         * @return string
         */
        public String toString() {
            return lbl;
        }

    }




}
