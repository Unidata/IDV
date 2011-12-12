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


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSource;




import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;



import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.ui.FineLineBorder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import ucar.visad.display.PointProbe;



import visad.*;


import java.applet.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.*;

import java.rmi.RemoteException;

import java.text.*;

import java.util.ArrayList;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.*;

import javax.swing.*;

import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.*;



/**
 * DisplayControl for displaying straight text or HTML.
 *
 * @author IDV development team
 * @version $Revision: 1.18 $
 */
public class ChatLogControl extends DisplayControlImpl {


    /** table for output */
    private JTable table;

    /** table model */
    private AbstractTableModel tableModel;


    /** filename for text */
    String filename;


    /** The text */
    String textContents;

    /** The unprocessed lines */
    List rawLines;

    /** The processed lines */
    List lines;

    /** The dates */
    List dates;


    /** The year */
    private String year;

    /** The timezone */
    private String timezone;

    /**
     * Default constructor; does nothing.  Heavy work done in init().
     */
    public ChatLogControl() {}



    /**
     * Initialize this class with the supplied {@link DataChoice}.
     *
     * @param dataChoice   choice to describe the data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !setData(dataChoice)) {
            return false;
        }
        try {
            this.textContents = IOUtil.readContents(dataChoice.getStringId());
        } catch (Exception exc) {
            logException("Reading chat file:" + dataChoice.getStringId(),
                         exc);
            return false;
        }
        filename = dataChoice.getStringId();
        if ( !parseText()) {
            return false;
        }
        PointProbe probe = new PointProbe(0.0, 0.0, 0.0);
        probe.setVisible(false);
        addDisplayable(probe);
        return true;
    }

    /**
     * Parse the text
     *
     * @return success
     */
    private boolean parseText() {
        rawLines = StringUtil.split(textContents, "\n", true, true);
        lines    = new ArrayList();
        dates    = new ArrayList();
        //Dec 17 21:25:33 <MikeDaniels-ops>     Where was the bad food from?n


        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("MMM d H:m:s yyyy-Z");
        Pattern pattern = Pattern.compile("([^<]+)<([^>]+)>(.*)");
        try {
            for (int i = 0; i < rawLines.size(); i++) {
                String line = (String) rawLines.get(i);
                if (line.startsWith("***")) {
                    Pattern pattern2 =
                        Pattern.compile("BEGIN.*(\\d\\d\\d\\d)$");
                    //**** BEGIN LOGGING AT Fri Dec 17 21:16:20 2004
                    Matcher m = pattern2.matcher(line);
                    if (m.find()) {
                        year = m.group(1);
                    }
                    continue;
                }
                Matcher matcher = pattern.matcher(line);
                if ( !matcher.find()) {
                    continue;
                }

                if (year == null) {
                    JTextField yearFld = new JTextField("2005", 8);
                    JTextField tzFld   = new JTextField("GMT", 8);
                    GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
                    String exampleLine =
                        StringUtil.replace(StringUtil.replace(line, "<",
                            "&lt;"), ">", "&gt;");
                    JPanel panel = GuiUtils.doLayout(new Component[] {
                                       GuiUtils.rLabel("Year:"),
                                       yearFld, GuiUtils.rLabel("Timezone:"),
                                       tzFld }, 2, GuiUtils.WT_NN,
                                           GuiUtils.WT_N);
                    panel = GuiUtils
                        .vbox(GuiUtils
                            .inset(GuiUtils
                                .lLabel(
                                    "Please enter the year and  timezone of the chat log"), 4), GuiUtils
                                        .inset(GuiUtils
                                            .lLabel(
                                                "<html>First line: <i>"
                                                    + exampleLine
                                                        + "</i>"), 4), panel);
                    if ( !GuiUtils.showOkCancelDialog(null,
                            "Chat Log Information", panel, null)) {
                        return false;
                    }
                    year     = yearFld.getText().trim();
                    timezone = tzFld.getText().trim();
                }




                Date v = sdf.parse(matcher.group(1).trim() + " " + year + "-"
                                   + timezone);
                DateTime dttm = new DateTime(v);
                dates.add(dttm);
                lines.add(Misc.newList(dttm.toString(), matcher.group(2),
                                       matcher.group(3)));
            }
            setAnimationSet(dates);
        } catch (Exception exc) {
            logException("Error processing log", exc);
        }
        return true;
    }




    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            int theIndex = getInternalAnimation(null).getCurrent();
            if (theIndex >= 0) {
                table.setRowSelectionInterval(theIndex, theIndex);
                GuiUtils.makeRowVisible(table, theIndex);
            }
        } catch (Exception exc) {
            logException("Error handling time change", exc);
        }
    }


    /**
     *  Overwrite the base class method to return the filename or url.
     *
     *  @return The filename or url as the title.
     */
    protected String getTitle() {
        if (filename != null) {
            return filename;
        }
        return super.getTitle();
    }



    /** Table column names */
    private static String[] colNames = { "Date", "User", "Message" };

    /**
     * Make the contents for this control
     * @return  UI for the control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        tableModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                return lines.size();
            }

            public int getColumnCount() {
                return 3;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {
                if (row < lines.size()) {
                    List rowData = (List) lines.get(row);
                    if (column < rowData.size()) {
                        return rowData.get(column);
                    }
                }
                return "";
            }

            public String getColumnName(int column) {
                return colNames[column];
            }
        };



        table = new JTable(tableModel);
        int         width    = 300;
        int         height   = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(table, width, height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        return GuiUtils.topCenter(
            GuiUtils.left(getAnimationWidget().getContents()), scroller);
    }


    /**
     * Set the Year property.
     *
     * @param value The new value for Year
     */
    public void setYear(String value) {
        year = value;
    }

    /**
     * Get the Year property.
     *
     * @return The Year
     */
    public String getYear() {
        return year;
    }

    /**
     * Set the Timezone property.
     *
     * @param value The new value for Timezone
     */
    public void setTimezone(String value) {
        timezone = value;
    }

    /**
     * Get the Timezone property.
     *
     * @return The Timezone
     */
    public String getTimezone() {
        return timezone;
    }

}
