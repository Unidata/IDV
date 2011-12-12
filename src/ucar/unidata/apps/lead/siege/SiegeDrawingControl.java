/*
 * $Id: SiegeDrawingControl.java,v 1.4 2006/06/06 11:32:30 jeffmc Exp $
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




package ucar.unidata.apps.lead.siege;


import org.w3c.dom.*;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.grid.GeoGridDataSource;


import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.drawing.*;

import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.util.FileManager;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Date;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 *
 * @author MetApps development team
 * @version $Revision: 1.4 $
 */

public class SiegeDrawingControl extends DrawingControl {

    /** xml tag */
    private static final String TAG_DATA = "data";

    /** xml tag */
    private static final String TAG_CENTER = "center";

    /** xml tag */
    private static final String TAG_LAT = "lat";

    /** xml tag */
    private static final String TAG_LON = "lon";

    /** xml tag */
    private static final String TAG_STARTTIME = "starttime";


    /** _more_          */
    private Date startDate;

    /** selected data/time */
    private DateTimePicker startTimePicker;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public SiegeDrawingControl() {}


    /**
     * _more_
     *
     * @param dataChoice _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean init(DataChoice dataChoice) throws VisADException,
            RemoteException {
        if ( !super.init((DataChoice) null)) {
            return false;
        }

        if (dataChoice != null) {
            List times = dataChoice.getSelectedDateTimes();
            if (times == null) {
                times = dataChoice.getAllDateTimes();
            }

            if ((times != null) && (times.size() > 0)) {
                Object date = times.get(0);
                if (date instanceof DateTime) {
                    startDate = new Date(
                        (long) (((DateTime) date).getValue(
                            CommonUnit.secondsSinceTheEpoch) * 1000.0));
                }
            }
            //Clear out the data choices
            setDataChoices(new ArrayList());
        }



        return true;
    }


    /**
     * All initialization is done
     */
    public void initDone() {
        super.initDone();
        currentCmd = CMD_MOVE;
        setCoordType(DrawingGlyph.COORD_LATLON);
        //Only create the one if we don't have any
        if (getGlyphs().size() == 0) {
            try {
                SiegeBoundsGlyph boundsGlyph = new SiegeBoundsGlyph(this);
                addGlyph(boundsGlyph);
            } catch (Exception exc) {
                logException("", exc);
            }
        }
    }



    /**
     * Overwrite this to make our own gui
     *
     * @return The gui contents
     *
     * @throws RemoteException     On badness
     * @throws VisADException      On badness
     */
    protected Container doMakeContents() throws VisADException,
            RemoteException {
        super.doMakeContents();
        startTimePicker = new DateTimePicker();
        if (startDate != null) {
            startTimePicker.setDate(startDate);
        }
        JButton writeBtn = GuiUtils.makeButton("Write Siege File...", this,
                               "writeSiegeFile");
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        return GuiUtils.topLeft(GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Time:"),
            GuiUtils.left(startTimePicker), new JLabel(""), writeBtn }, 2,
                GuiUtils.WT_N, GuiUtils.WT_N));
    }

    /**
     * Overwrite  this as a no-op
     *
     * @throws RemoteException      On badness
     * @throws VisADException      On badness
     */
    protected void doCut() throws VisADException, RemoteException {
        //Make this a no-op
    }


    /**
     * Write out the xml file
     *
     * This is  a public method as an implementation side effect
     */
    public void writeSiegeFile() {
        try {
            String filename = FileManager.getReadFile(FileManager.FILTER_XML);
            if (filename == null) {
                return;
            }
            Document doc = XmlUtil.makeDocument();
            /*
<data>
  <center>
    <lat>47.1</lat>
    <lon>-87.5</lon>
  </center>
  <start-time>200605150900</start-time>
</data>
            */

            //Assume we have one
            SiegeBoundsGlyph glyph = (SiegeBoundsGlyph) getGlyphs().get(0);
            LatLonPointImpl  center     = glyph.getCenter();
            Element          root       = doc.createElement(TAG_DATA);
            Element          centerNode = doc.createElement(TAG_CENTER);
            Element          latNode    = doc.createElement(TAG_LAT);
            Element          lonNode    = doc.createElement(TAG_LON);
            latNode.appendChild(doc.createTextNode(""
                    + center.getLatitude()));
            lonNode.appendChild(doc.createTextNode(""
                    + center.getLongitude()));
            Element startNode = doc.createElement(TAG_STARTTIME);
            startNode.appendChild(doc.createTextNode(""
                    + startTimePicker.getDate()));
            root.appendChild(centerNode);
            centerNode.appendChild(latNode);
            centerNode.appendChild(lonNode);
            root.appendChild(startNode);
            IOUtil.writeFile(new File(filename), XmlUtil.toString(root));
        } catch (Exception exc) {
            logException("Writing Siege file", exc);
        }
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void addFileMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Write Siege File...", this,
                                        "writeSiegeFile"));

    }


    /**
     * Set the StartDate property.
     *
     * @param value The new value for StartDate
     */
    public void setStartDate(Date value) {
        startDate = value;
    }

    /**
     * Get the StartDate property.
     *
     * @return The StartDate
     */
    public Date getStartDate() {
        if (startTimePicker != null) {
            return startTimePicker.getDate();
        }
        return startDate;
    }



}

