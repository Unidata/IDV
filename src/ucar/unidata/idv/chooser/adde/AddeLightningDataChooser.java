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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.McIDASUtil;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;


import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.UtcDate;

import visad.DateTime;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Selection widget for ADDE point data
 *
 * @author MetApps Development Team
 * @version $Revision: 1.2 $ $Date: 2007/07/06 20:40:19 $
 */
public class AddeLightningDataChooser extends AddePointDataChooser {


    /**
     * Create a new <code>AddeLightningDataChooser</code> with the preferred
     * list of ADDE servers.
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeLightningDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Get the default station model for this chooser.
     * @return name of default station model
     */
    public String getDefaultStationModel() {
        return "flash";
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "Lightning Data";
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
        return new TwoFacedObject[] { new TwoFacedObject("NLDN", "LGT/NLDN"),
                                      new TwoFacedObject("USPLN",
                                      "LGT/USPLN") };
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement() {
        return .5f;
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
        return AddeUtil.RELATIVE_TIME_RANGE;
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
     * Set the list of available times.
     */
    public void readTimes() {
        clearTimesList();
        ArrayList uniqueTimes = new ArrayList();

        setState(STATE_CONNECTING);
        try {
            float    hours      = getRelativeTimeIncrement();
            int      numTimes   = (int) (24f / hours);
            DateTime currentDay = new DateTime(new Date());
            int day = Integer.parseInt(UtcDate.formatUtcDate(currentDay,
                          "yyyyDDD"));
            for (int i = 0; i < numTimes; i++) {
                int hour = McIDASUtil.mcDoubleToPackedInteger(i * hours);
                try {
                    DateTime dt =
                        new DateTime(McIDASUtil.mcDayTimeToSecs(day, hour));
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


}
