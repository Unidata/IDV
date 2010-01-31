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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;

import ucar.unidata.data.DataManager;
import ucar.unidata.geoloc.*;



import ucar.unidata.idv.*;
import ucar.unidata.ui.ChooserPanel;




import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.*;


import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.view.geoloc.NavigatedMapPanel;

import ucar.unidata.view.station.StationLocationMap;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Allows the user to select a url as a data source
 *
 * @author IDV development team
 * @version $Revision: 1.40 $Date: 2007/07/27 13:53:08 $
 */


public class MesoWestChooser extends IdvChooser implements ActionListener {


    /**
     *   TODO:
     *   Bill F noticed a time discrpency between GMT and local time?????
     */

    public static final String BASEURL =
        "http://mesowest.utah.edu/cgi-bin/droman/obs_lsa_export.cgi";

    /** _more_          */
    public static final String ARG_CLAT = "clat";

    /** _more_          */
    public static final String ARG_CLON = "clon";

    /** _more_          */
    public static final String ARG_BOXRAD = "boxrad";

    /** _more_          */
    public static final String ARG_HOUR1 = "hour1";

    /** _more_          */
    public static final String ARG_DAY1 = "day1";

    /** _more_          */
    public static final String ARG_MONTH1 = "month1";

    /** _more_          */
    public static final String ARG_YEAR1 = "year1";

    /** _more_          */
    public static final String ARG_RAWSFLAG = "rawsflag";

    /** _more_          */
    public static final String ARG_PAST = "past";

    /** _more_          */
    public static final String ARG_HOURS = "hours";

    /** _more_          */
    public static final String ARG_MINUTES = "minutes";

    /** _more_          */
    public static final int RAWS_NWS = 3;

    /** _more_          */
    public static final int RAWS_NWSANDRAWS = 1;

    /** _more_          */
    public static final int RAWS_ALL = 290;

    /** _more_          */
    private static String projectionString =
        "<object class=\"ucar.unidata.geoloc.projection.LatLonProjection\"><property name=\"CenterLon\"><double>-109</double></property><property name=\"Name\"><string><![CDATA[US>States>West>Colorado]]></string></property><property name=\"DefaultMapArea\"><object class=\"ucar.unidata.geoloc.ProjectionRect\"><constructor><double>-124</double><double>31</double><double>-100</double><double>47</double></constructor></object></property></object>";




    /** _more_          */
    private JTextField clatFld;

    /** _more_          */
    private JTextField clonFld;

    /** _more_          */
    private JComboBox radiiCbx;

    /** _more_          */
    private DateTimePicker dateTimePicker;

    /** _more_          */
    private NavigatedMapPanel map;

    /** _more_          */
    private JLabel statusLbl;

    /** _more_          */
    private JComboBox rawsBox;

    /** _more_          */
    private JComboBox minutesBox;

    /** _more_          */
    private JComboBox rangeBox;

    /** _more_          */
    private JRadioButton mostRecentBtn;

    /** _more_          */
    private JRadioButton dateBtn;

    /**
     * Create the <code>UrlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public MesoWestChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoUpdate() {
        return false;
    }


    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the MesoWest Data";
    }



    /**
     * _more_
     *
     * @param g _more_
     */
    private void doAnnotateMap(Graphics2D g) {
        NavigatedPanel    np      = map.getNavigatedPanel();
        ProjectionImpl    project = np.getProjectionImpl();
        List<LatLonPoint> points  = new ArrayList<LatLonPoint>();
        LatLonRect        llr     = np.getSelectedEarthRegion();
        if (llr == null) {
            return;
        }
        double  width    = llr.getWidth();
        double  radii    = width / 2;
        boolean maxedOut = false;
        if (radii > 5) {
            radii    = 5;
            maxedOut = true;
        }

        if (maxedOut) {
            statusLbl.setText("Bounds radius > 5 degrees");
        } else {
            statusLbl.setText("");
        }
        statusLbl.repaint();
        double clat = (llr.getLatMin()
                       + (llr.getLatMax() - llr.getLatMin()) / 2);
        double clon = (llr.getLonMin()
                       + (llr.getLonMax() - llr.getLonMin()) / 2);
        points.add(new LatLonPointImpl(clat + radii, clon - radii));
        points.add(new LatLonPointImpl(clat + radii, clon + radii));
        points.add(new LatLonPointImpl(clat - radii, clon + radii));
        points.add(new LatLonPointImpl(clat - radii, clon - radii));

        g.setStroke(new BasicStroke(0.1f));  // default stroke size is one pixel
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                           points.size());

        for (int i = 0; i <= points.size(); i++) {
            LatLonPoint llp;
            if (i >= points.size()) {
                llp = points.get(0);
            } else {
                llp = points.get(i);
            }
            ProjectionPoint ppi = (ProjectionPoint) project.latLonToProj(llp,
                                      new ProjectionPointImpl());
            Point2D p = np.earthToScreen(llp);
            //            System.err.println ("\t" + p);
            if (i == 0) {
                path.moveTo((float) ppi.getX(), (float) ppi.getY());
                if (maxedOut) {
                    //                    g.drawString("-- max width 5 degrees --",(float)ppi.getX(),(float)ppi.getY());
                }
            } else {
                path.lineTo((float) ppi.getX(), (float) ppi.getY());
            }
        }
        g.setColor(Color.gray);
        g.draw(path);
    }


    /**
     * _more_
     */
    private void selectionChanged() {
        map.redraw();
    }

    /**
     * Create the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        Vector rawsList = new Vector();
        rawsList.add(new TwoFacedObject("NWS Only", new Integer(RAWS_NWS)));
        rawsList.add(new TwoFacedObject("NWS and RAWS",
                                        new Integer(RAWS_NWSANDRAWS)));
        rawsList.add(new TwoFacedObject("All Network",
                                        new Integer(RAWS_ALL)));
        rawsBox    = new JComboBox(rawsList);

        minutesBox = new JComboBox(new Vector());
        Vector rangeList = new Vector();
        rangeList.add(new TwoFacedObject("24 hours", new Integer(24)));
        rangeList.add(new TwoFacedObject("12 hours", new Integer(12)));
        rangeList.add(new TwoFacedObject("6 hours", new Integer(6)));
        rangeList.add(new TwoFacedObject("2 hours", new Integer(2)));
        rangeBox = new JComboBox(rangeList);
        rangeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkMinutes();
            }
        });

        checkMinutes();
        mostRecentBtn = new JRadioButton("Use most recent time", true);
        mostRecentBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkDateEnable();
            }
        });
        dateBtn = new JRadioButton("Select date:", true);
        dateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkDateEnable();
            }
        });

        GuiUtils.buttonGroup(mostRecentBtn, dateBtn);
        statusLbl = new JLabel("");
        map       = new NavigatedMapPanel(true, true) {
            protected void annotateMap(Graphics2D g) {
                super.annotateMap(g);
                doAnnotateMap(g);
            }
            protected NavigatedPanel doMakeMapPanel() {
                return new NavigatedPanel() {
                    protected void selectedRegionChanged() {
                        super.selectedRegionChanged();
                        selectionChanged();
                    }
                };
            }
        };
        NavigatedPanel np = map.getNavigatedPanel();
        np.setSelectRegionMode(true);
        np.setSelectedRegion(new LatLonRect(new LatLonPointImpl(39, -110),
                                            new LatLonPointImpl(43, -114)));

        map.repaint();
        try {
            ProjectionImpl proj =
                (ProjectionImpl) getIdv().decodeObject(projectionString);

            np.setProjectionImpl(proj);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        np.setPreferredSize(new Dimension(250, 300));
        //        StationLocationMap map = getStationMap();
        dateTimePicker = new DateTimePicker();
        JComponent dateComp = GuiUtils.vbox(mostRecentBtn,
                                            GuiUtils.hbox(dateBtn,
                                                dateTimePicker));
        checkDateEnable();
        List comps = new ArrayList();
        comps.add(GuiUtils.rLabel("Date/Time:"));
        comps.add(GuiUtils.left(dateComp));

        comps.add(GuiUtils.rLabel("Range:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(rangeBox,
                GuiUtils.filler(20, 5), GuiUtils.rLabel("Interval: "),
                GuiUtils.left(minutesBox))));

        comps.add(GuiUtils.rLabel("Observations:"));
        comps.add(GuiUtils.left(rawsBox));

        comps.add(GuiUtils.rLabel("Location:"));
        comps.add(GuiUtils.centerBottom(np,
                                        GuiUtils.left(np.getNavToolBar())));
        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(statusLabel));

        JComponent mainContents = GuiUtils.formLayout(comps);
        JComponent urlButtons   = getDefaultButtons();
        setHaveData(true);
        return GuiUtils.top(GuiUtils.vbox(mainContents, urlButtons));
    }



    /**
     * _more_
     */
    private void checkMinutes() {
        int    hours       = ((Integer) getId(rangeBox)).intValue();
        Object selected    = minutesBox.getSelectedItem();
        Vector minutesList = new Vector();
        minutesList.add(new TwoFacedObject("60 Minutes", new Integer(60)));
        if (hours < 24) {
            minutesList.add(new TwoFacedObject("30 Minutes",
                    new Integer(30)));
            if (hours < 12) {
                minutesList.add(new TwoFacedObject("15 Minutes",
                        new Integer(15)));
                if (hours < 6) {
                    minutesList.add(new TwoFacedObject("5 Minutes",
                            new Integer(5)));
                }
            }
        }
        GuiUtils.setListData(minutesBox, minutesList);
        if ((selected != null) && minutesList.contains(selected)) {
            minutesBox.setSelectedItem(selected);
        }
    }

    /**
     * _more_
     */
    private void checkDateEnable() {
        GuiUtils.enableTree(dateTimePicker, !mostRecentBtn.isSelected());
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param what _more_
     */
    public void setStatus(String msg, String what) {
        super.setStatus("Press \"" + CMD_LOAD + "\" to load the data",
                        "buttons");
    }


    /**
     * Handle the action event from the GUI
     */
    public void doLoadInThread() {
        Hashtable properties   = new Hashtable();
        String    dataSourceId = "FILE.POINTTEXT";
        properties.put(DataManager.DATATYPE_ID, dataSourceId);
        Date date = dateTimePicker.getDate();
        GregorianCalendar cal =
            new GregorianCalendar(DateTimePicker.getDefaultTimeZone());
        cal.setTime(date);

        String hour = "" + cal.get(Calendar.HOUR_OF_DAY);
        String day  = "" + cal.get(Calendar.DAY_OF_MONTH);
        String month = StringUtil.padLeft("" + (cal.get(Calendar.MONTH) + 1),
                                          2, "0");
        String         year = "" + cal.get(Calendar.YEAR);

        NavigatedPanel np   = map.getNavigatedPanel();
        LatLonRect     llr  = np.getSelectedEarthRegion();
        if (llr == null) {
            LogUtil.userErrorMessage("You need to select a region");
            return;
        }

        double width     = llr.getWidth();
        double centerLon = llr.getCenterLon();
        //        System.out.println("llr:" + llr);
        //        System.out.println("minmax" + llr.getLatMin() +  " " + llr.getLatMax() + " " +llr.getLonMin() + " " + llr.getLonMax());
        double  radii    = width / 2;
        boolean maxedOut = false;
        if (radii > 5) {
            radii    = 5;
            maxedOut = true;
        }
        List<String> args = new ArrayList<String>();
        if (mostRecentBtn.isSelected()) {
            args.add(ARG_PAST);
            args.add("0");
        } else {
            args.addAll(Misc.toList(new Object[] {
                ARG_PAST, "1", ARG_HOUR1, hour, ARG_DAY1, day, ARG_MONTH1,
                month, ARG_YEAR1, year
            }));
        }
        args.addAll(Misc.toList(new Object[] {
            ARG_RAWSFLAG, getId(rawsBox).toString(), ARG_CLAT,
            (llr.getLatMin() + (llr.getLatMax() - llr.getLatMin()) / 2) + "",
            ARG_CLON,
            (llr.getLonMin() + (llr.getLonMax() - llr.getLonMin()) / 2) + "",
            ARG_BOXRAD, radii + "", ARG_HOURS, getId(rangeBox).toString(),
            ARG_MINUTES, getId(minutesBox).toString()
        }));
        String url = HtmlUtil.url(BASEURL, Misc.listToStringArray(args));
        url = url + "&maxcount=%maxcount%";
        if (makeDataSource(url, dataSourceId, properties)) {
            closeChooser();
        }
    }

    /**
     * _more_
     *
     * @param box _more_
     *
     * @return _more_
     */
    private Object getId(JComboBox box) {
        return ((TwoFacedObject) box.getSelectedItem()).getId();
    }

}
