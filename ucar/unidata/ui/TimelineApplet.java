/**
 * $Id: Timeline.java,v 1.36 2007/08/16 14:09:56 jeffmc Exp $
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





package ucar.unidata.ui;



import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.DatedThing;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import java.io.*;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;



import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import java.net.URL;
import java.applet.*;


/**
 * Widget for selecting dates and times
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.36 $
 */
public class TimelineApplet extends Applet {
    Timeline timeline;
    URL baseUrl;
    JLabel label = new JLabel("  ");

    public TimelineApplet() {
    }

    public  void init () {
        super.init();
        String times  = getParameter ("times");
        String labels  = getParameter ("labels");
        List realTimes = new ArrayList();
        //        System.err.println("TIMES:" + times);
        //        System.err.println("labels:" + labels);
        try {
        if(times!=null&& labels!=null) {
            List timeStrings = StringUtil.split(times,",",true, true);
            List labelStrings = StringUtil.split(labels,",",true, true);
            for(int i=0;i<timeStrings.size();i++) {
                realTimes.add(new DatedObject(DateUtil.parse((String) timeStrings.get(i)),
                                                             labelStrings.get(i)));
            }
        }
        } catch(Exception exc) {
            exc.printStackTrace();
        }
        //        System.err.println("xxxx Real times:" + realTimes);
        timeline = new Timeline(realTimes,400) {
                public void setHighlightedDate(DatedThing d) {
                    if(d==null) label.setText("  ");
                    else label.setText(d.toString());
                    super.setHighlightedDate(d);
                }
            };
        //        baseUrl =getDocumentBase();
        setLayout (new BorderLayout());
        timeline.setUseDateSelection(false);
        JPanel container = LayoutUtil.centerBottom(timeline.getContents(false,false),LayoutUtil.inset(label,new Insets(0,5,0,0)));
        container.setBorder(BorderFactory.createLineBorder(Color.black));

        this.add(container);
    }
}

