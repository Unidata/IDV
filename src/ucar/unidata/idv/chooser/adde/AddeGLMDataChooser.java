/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.McIDASUtil;

import edu.wisc.ssec.mcidas.adde.DataSetInfo;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;


import ucar.unidata.data.DataSelection;
import ucar.unidata.data.point.AddePointDataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.UtcDate;

import visad.DateTime;
import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Selection widget for ADDE point data
 *
 * @author MetApps Development Team
 * @version $Revision: 1.2 $ $Date: 2007/07/06 20:40:19 $
 */
public class AddeGLMDataChooser extends AddePointDataChooser {

    /** _more_ */
    private JComponent descriptorLabel;

    /** _more_ */
    protected JComboBox descriptorComboBox;

    /** _more_ */
    private boolean ignoreDescriptorChange = false;

    /** _more_ */
    protected String[] descriptorNames;

    /** _more_ */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /** _more_ */
    private PreferenceList descList;

    /** _more_ */
    protected Hashtable descriptorTable;


    /** box for the relative time */
   // private JComboBox relTimeIncBox;

    /** box for the relative time */
    private JComponent relTimeIncComp;

    /** box for the relative time */
   // public static String DATA_TYPE = "ADDE.GLM";

    /**
     * Create a new <code>AddeLightningDataChooser</code> with the preferred
     * list of ADDE servers.
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeGLMDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        setRelativeTimeIncrement(5.0f);
    }


    /**
     * Get the default station model for this chooser.
     * @return name of default station model
     */
    public String getDefaultStationModel() {
        return "location";
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "GLM Lightning Data";
    }

    /**
     * Get the request string for times particular to this chooser
     *
     * @return request string
     * protected String getTimesRequest() {
     *   StringBuffer buf = getGroupUrl(REQ_POINTDATA, getGroup());
     *   appendKeyValue(buf, PROP_DESCR, getDescriptor());
     *   // this is hokey, but take a smattering of stations.
     *   //appendKeyValue(buf, PROP_SELECT, "'CO US'");
     *   appendKeyValue(buf, PROP_POS, "0");
     *   appendKeyValue(buf, PROP_NUM, "ALL");
     *   appendKeyValue(buf, PROP_PARAM, "DAY TIME");
     *   return buf.toString();
     * }
     */

    /**
     * Get the default datasets for the chooser.  The objects are
     * a descriptive name and the ADDE group/descriptor
     *
     * @return  default datasets.
     */
    protected TwoFacedObject[] getDefaultDatasets() {
        return new TwoFacedObject[] {
            new TwoFacedObject("RTGOESR", "RTGOESR"),
            new TwoFacedObject("RTGOESS", "RTGOESS") };
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement1() {
        return (5f / 60f);
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE URL.
     *
     * @return the select day and time strings
     */
    protected String getDayTimeSelectString() {
        StringBuffer buf = new StringBuffer();
        if (getDoAbsoluteTimes()) {
            buf.append("time ");
            List     times = getSelectedAbsoluteTimes();
            DateTime dt    = (DateTime) times.get(0);
            buf.append(UtcDate.getHMS(dt));
            buf.append(" ");
            dt = (DateTime) times.get(times.size() - 1);
            buf.append(UtcDate.getHMS(dt));
        } else {
            buf.append(getRelativeTimeId());
        }
        return buf.toString();
    }

    /**
     * Get the identifier for relative time.  Subclasses can override.
     * @return the identifier
     */
    protected String getRelativeTimeId() {
        return AddeUtil.RELATIVE_TIME;
    }

    /**
     * Get the selection mode for the absolute times panel. Subclasses
     * can override.
     *
     * @return the list selection mode
     */
    protected int getAbsoluteTimeSelectMode() {
        return ListSelectionModel.SINGLE_INTERVAL_SELECTION;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getGroup() {
        String dataset =
            TwoFacedObject.getIdString(dataTypes.getSelectedItem());
        int index = dataset.indexOf('/');
        /*    if (index == -1) {
                throw new IllegalArgumentException("Bad dataset: \"" + dataset
                        + "\"");
            } */
        //  return dataset.substring(0, index);
        return dataset;
    }

    /**
     * Return the currently selected descriptor form the combobox
     *
     * @return the currently selected descriptor
     */
    protected String getDescriptor1() {
        String dataset =
            TwoFacedObject.getIdString(descriptorComboBox.getSelectedItem());
        int index = dataset.indexOf('/');
        /*  if (index == -1) {
              throw new IllegalArgumentException("Bad dataset: \"" + dataset
                      + "\"");
          }
          return dataset.substring(index + 1); */
        return dataset;
    }

    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        return getDescriptorFromSelection(getSelectedDescriptor());
    }

    /**
     * Get the descriptor relating to the selection.
     *
     * @param selection   String name from the widget
     *
     * @return  the descriptor
     */
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        return (String) descriptorTable.get(selection);
    }

    /**
     * Get the selected descriptor.
     *
     * @return the selected descriptor
     */
    public String getSelectedDescriptor() {
        String selection = (String) descriptorComboBox.getSelectedItem();
        if (selection == null) {
            return null;
        }
        if (selection.equals(LABEL_SELECT)) {
            return null;
        }
        return selection;
    }

    /**
     * Set the list of available times.
     */
    public void readTimes() {

        clearTimesList();
        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());

        setState(STATE_CONNECTING);
        try {
            GregorianCalendar utcCalendar =
                    new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            Date   now         = new Date();
            utcCalendar.setTime(now);
            int curHour = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int curMin  = utcCalendar.get(utcCalendar.MINUTE);
            float min = getRelativeTimeIncrement();
            // only show 24 hours
            int      numTimes   = (int) ((24f * 60f) / min);

            int minB = 2; //back 2 minutes from current time
            utcCalendar.add(utcCalendar.MINUTE, -minB);
            int    sec   = utcCalendar.get(utcCalendar.SECOND);
            utcCalendar.add(utcCalendar.SECOND, -sec);
            try {
                DateTime dt = new DateTime(utcCalendar.getTime());
                uniqueTimes.add(dt);
            } catch (Exception e) {}
            for (int i = 1; i < numTimes; i++) {
                utcCalendar.add(utcCalendar.MINUTE, -((int)min));
               // int hour = McIDASUtil.mcDoubleToPackedInteger(i * min/60.f);
                try {
                    DateTime dt = new DateTime(utcCalendar.getTime());
                    uniqueTimes.add(dt);
                } catch (Exception e) {}
            }
            setState(STATE_CONNECTED);
            //System.out.println(
            //       "found " + uniqueTimes.size() + " unique times");
        } catch (Exception excp) {
            handleConnectionError(excp);
            return;
        }
        if (getDoAbsoluteTimes()) {
            if ( !uniqueTimes.isEmpty()) {
                setAbsoluteTimes(new ArrayList(uniqueTimes));
            }
            int selectedIndex = getAbsoluteTimes().size() - 1;
            setSelectedAbsoluteTime(selectedIndex);
        }
    }

    /**
     * Make the contents for this chooser
     *
     * @return  a panel with the UI
     */
    protected JComponent doMakeContents() {

        List allComps = new ArrayList();
        clearOnChange(dataTypes);
        addTopComponents(allComps, "Group:", dataTypes);

        descriptorLabel = addServerComp(GuiUtils.rLabel(getDescriptorLabel()
                + ":"));
        descriptorComboBox = new JComboBox();

        descriptorComboBox.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if ( !ignoreDescriptorChange
                            && (e.getStateChange() == e.SELECTED)) {
                            descriptorChanged();
                        }
                    }
                });
        allComps.add(descriptorLabel);
        allComps.add(addServerComp(descriptorComboBox));

        JPanel timesComp = makeTimesPanel();
        allComps.add(addServerComp(GuiUtils.valignLabel(LABEL_TIMES)));
        allComps.add(addServerComp(timesComp));

        //        allComps.add(addServerComp(GuiUtils.rLabel("Layout Model:")));
        //        allComps.add(addServerComp(GuiUtils.left(lastPanel)));
        JComponent top = GuiUtils.formLayout(allComps, GRID_INSETS);
        return GuiUtils.top(GuiUtils.centerBottom(top, getDefaultButtons()));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescriptorLabel() {
        return "GLM Type";
    }

    /**
     * _more_
     */
    protected void descriptorChanged() {
        readTimes();
        updateStatus();
    }
    /**
     * _more_
     */
    protected boolean getGoodToGo() {
        //  if(!super.getGoodToGo()) return false;
        if (getDoAbsoluteTimes()) {
            return getHaveAbsoluteTimesSelected();
        } else {
            return canReadTimes() ;
        }
    }
    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        //if (getState() != STATE_CONNECTED) {
        //  setDescriptors(null);
        // setPropertiesState(null);
        // return;
        // }
        enableWidgets();
    }

    /**
     * _more_
     */
    protected void enableWidgets() {
        boolean descriptorState = ( ( getState() == STATE_CONNECTED ||
                getState() == STATE_CONNECTING)
                                   && canReadTimes());
        if (drivercbx != null) {
            drivercbx.setSelected(false);
            enableTimeWidgets();
        }

        checkTimesLists();

        enableAbsoluteTimesList(getDoAbsoluteTimes() && descriptorState);

        getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
                                             && descriptorState);
        if(descriptorComboBox.getItemCount() > 0)
            GuiUtils.enableTree(descriptorComboBox, true);
        else
            GuiUtils.enableTree(descriptorComboBox, false);
        revalidate();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canReadTimes() {
        return haveDescriptorSelected();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean haveDescriptorSelected() {
        if ( !GuiUtils.anySelected(descriptorComboBox)) {
            return false;
        }
        return (getDescriptor() != null);
    }

    /**
     * _more_
     *
     * @param names _more_
     */
    protected void setDescriptors(String[] names) {
        synchronized (WIDGET_MUTEX) {
            ignoreDescriptorChange = true;
            descriptorComboBox.removeAllItems();
            descriptorNames = names;
            if ((names == null) || (names.length == 0)) {
                return;
            }
            descriptorComboBox.addItem(LABEL_SELECT);
            for (int j = 0; j < names.length; j++) {
                descriptorComboBox.addItem(names[j]);
            }
            ignoreDescriptorChange = false;
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void handleConnect() throws Exception {
        setState(STATE_CONNECTING);
        connectToServer();
        setState(STATE_CONNECTED);
        updateStatus();
    }

    /**
     * _more_
     */
    protected void connectToServer() {
        setDescriptors(null);

        // set to relative times
        setDoAbsoluteTimes(false);
        if ( !canAccessServer()) {
            return;
        }

        readDescriptors();
        //        readTimes();
        //Save the server/group state
        saveServerState();
        ignoreStateChangedEvents = true;
        if (descList != null) {
            descList.saveState(groupSelector);
        }
        ignoreStateChangedEvents = false;
    }

    /**
     * _more_
     */
    private void readDescriptors() {
        try {
            StringBuffer buff = getGroupUrl(REQ_DATASETINFO, getGroup());
            buff.append("&type=").append(getDataType());
            DataSetInfo dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            String[]    names       = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                names[i] = enumeration.nextElement().toString();
            }
            Arrays.sort(names);
            setDescriptors(names);
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getTimesRequest() {
        StringBuffer buf = getGroupUrl("POINT", getGroup());
        appendKeyValue(buf, PROP_DESCR, getDescriptor());
        // this is hokey, but take a smattering of stations.
        // appendKeyValue(buf, PROP_SELECT, "'LAT 39.5 40.5;LON 104.5 105.5'");
        // include buoys
        //appendKeyValue(buf, PROP_SELECT, "'LAT 38 42;LON 70 75'");
        appendKeyValue(buf, PROP_POS, "ALL");  // set to 0 for now
        if (getDoAbsoluteTimes()) {
            appendKeyValue(buf, PROP_NUM, "all");
            appendKeyValue(buf, PROP_SELECT,
                           "'DAY " + getJulianDay() + ";TIME "
                           + getTimeRange() + "'");
        }
        appendKeyValue(buf, PROP_PARAM, "DAY TIME");
        return buf.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private String getJulianDay() {
        String retString = "";
        try {
            DateTime dt = new DateTime();
            retString = UtcDate.formatUtcDate(dt, "yyyyDDD");
        } catch (VisADException ve) {
            handleConnectionError(ve);
        }

        int    bday     = Integer.parseInt(retString);

        String dayRange = String.valueOf(bday) + " " + retString;
        return dayRange;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getTimeRange() {
        String retString = "";
        try {
            DateTime dt = new DateTime();
            retString = UtcDate.formatUtcDate(dt, "HHmmss");
        } catch (VisADException ve) {
            handleConnectionError(ve);
        }

        int    btime     = Integer.parseInt(retString) - 1000;

        String timeRange = String.valueOf(btime) + " " + retString;
        return timeRange;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRequestUrl() {
        StringBuffer request = getGroupUrl("point", getGroup());
        appendKeyValue(request, PROP_DESCR, getDescriptor());
        appendRequestSelectClause(request);
        appendKeyValue(request, PROP_NUM, "all");
        //appendKeyValue(request, PROP_DEBUG, "true");
        appendKeyValue(request, PROP_POS, "1");

        return request.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getExtraRelativeTimeComponent() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JComboBox box = (JComboBox) ae.getSource();
                if (GuiUtils.anySelected(box)) {
                    setRelativeTimeIncrement(getRelBoxValue());
                    readTimes();
                }
            }
        };
        String[] nums = new String[] {
            "1", "10", "15", "20", "25", "30"
        };
        float[]  vals = new float[] {
            1f, 10f, 15f, 20f, 25f, 30f
        };
        List     l    = new ArrayList();
        for (int i = 0; i < nums.length; i++) {
            l.add(new TwoFacedObject(nums[i], new Float(vals[i])));
        }
        relTimeIncBox = GuiUtils.getEditableBox(l,
                new Float(getRelativeTimeIncrement()));
        relTimeIncBox.addActionListener(listener);
        relTimeIncBox.setToolTipText(
            "Set the increment between most recent times");
        //        Dimension prefSize = relTimeIncBox.getPreferredSize();
        //        if(prefSize!=null) {
        //            relTimeIncBox.setPreferredSize(new Dimension(20,prefSize.height));
        //        }
        relTimeIncComp = GuiUtils.hbox(new JLabel("Increment: "),
                                       relTimeIncBox,
                                       GuiUtils.lLabel(getRelTimeIncLabel()));
        return GuiUtils.left(relTimeIncComp);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private float getRelBoxValue() {
        float value = getRelativeTimeIncrement();
        if (relTimeIncBox != null) {
            Object o = relTimeIncBox.getSelectedItem();
            if (o != null) {
                String val = TwoFacedObject.getIdString(o);
                value = (float) Misc.parseNumber(val);
            }
        }
        return value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRelTimeIncLabel() {
        return " minutes";
    }


    public void doLoadInThread() {
        showWaitCursor();
        try {
            StationModel selectedStationModel = getSelectedStationModel();
            String       source               = getRequestUrl();
            // make properties Hashtable to hand the station name
            // to the AddeProfilerDataSource
            source = source.replaceAll("group", "GROUP");
            Hashtable ht = new Hashtable();
            getDataSourceProperties(ht);
            ht.put(DataSelection.PROP_CHOOSERTIMEMATCHING,
                    getDoTimeDrivers());
            ht.put(AddePointDataSource.PROP_STATIONMODELNAME,
                    selectedStationModel.getName());
            ht.put(DATASET_NAME_KEY, getDatasetName() + "/" + getDescriptor());
            //ht.put(DATASET_NAME_KEY, getDatasetName());
            ht.put(DATA_NAME_KEY, getDataName());
            ht.put(AddeUtil.RELATIVE_TIME_INCREMENT,
                    new Double(getRelativeTimeIncrement()/60.0));
            if (source.indexOf(AddeUtil.RELATIVE_TIME) >= 0) {
                ht.put(AddeUtil.NUM_RELATIVE_TIMES, getRelativeTimeIndices());
            }
            if (getDoAbsoluteTimes()) {
                ht.put(AddeUtil.ABSOLUTE_TIMES, getSelectedAbsoluteTimes());
            }
            if (source.indexOf(AddeUtil.LEVEL) >= 0) {
                ht.put(LEVELS, getLevels());
                ht.put(SELECTED_LEVEL, getSelectedLevel());
            }


            makeDataSource(source, DATA_TYPE, ht);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open ADDE point dataset", excp);
        }
        showNormalCursor();
        // uncheck the check box every time click the add source button
        drivercbx.setSelected(false);
        enableTimeWidgets();
        setDoTimeDrivers(false);
    }

    protected String getDefaultDisplayType() {
        return null;  //"imagedisplay";
    }

}
