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
import java.util.Vector;



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
    JButton loadBtn;    
    JComboBox loadWhat;

    public TimelineApplet() {
    }

    public  void init () {
        super.init();
        String times  = getParameter ("times");
        String labels  = getParameter ("labels");
        String ids  = getParameter ("ids");
        List realTimes = new ArrayList();
        //        System.err.println("TIMES:" + times);
        //        System.err.println("labels:" + labels);
        //        System.err.println("ids:" + ids);
        List idList = null;
        JComponent loadComp = new JPanel();
        try {
            if(times!=null&& labels!=null) {
                List timeStrings = StringUtil.split(times,",",true, true);
                List labelStrings = StringUtil.split(labels,",",true, true);
                if(ids!=null) idList = StringUtil.split(ids,",",true, true);
                for(int i=0;i<timeStrings.size();i++) {
                    String id = (String)(idList!=null?idList.get(i):null);
                    Date date  = DateUtil.parse((String) timeStrings.get(i));
                    String label = (String)labelStrings.get(i);
                    MyDatedObject mdo = new MyDatedObject(date, label, id);
                    //                    System.err.println ("item:" +  label + " " + date +" id=" + id);
                    realTimes.add(mdo);
                }
            }
        } catch(Exception exc) {
            exc.printStackTrace();
        }

        final String  loadUrl  = getParameter("loadurl");
        if(loadUrl!=null && idList!=null) {
            String loadLabel  = getParameter("loadlabel");
            if(loadLabel == null)
                loadLabel = "Load Selected";
            loadBtn  = new JButton(loadLabel);
            loadBtn.setEnabled(false);
            loadBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        doLoad(loadUrl);
                    }
                });
            String  loadTypes  = getParameter("loadtypes");
            if(loadTypes!=null && loadUrl.indexOf("%loadtype%")>=0) {
                loadWhat = new JComboBox(new Vector(StringUtil.split(loadTypes,",",true,true)));
                loadComp = LayoutUtil.hbox(loadBtn, loadWhat);
            } else {
                loadComp = loadBtn;
            }
        }


        timeline = new Timeline(new ArrayList(),400) {
                public void setHighlightedDate(DatedThing d) {
                    if(d==null) label.setText("  ");
                    else label.setText(d.toString());
                    super.setHighlightedDate(d);
                }
                public void selectedDatesChanged() {
                    super.selectedDatesChanged();
                    if(loadBtn!=null)
                        loadBtn.setEnabled(getSelected().size()>0);
                }
            };


        //        baseUrl =getDocumentBase();
        setLayout (new BorderLayout());
        timeline.setDatedThings(realTimes, true);
        timeline.setUseDateSelection(false);
        JPanel container = LayoutUtil.centerBottom(timeline.getContents(false,false),LayoutUtil.inset(LayoutUtil.centerRight(
                                                                                                                             label,loadComp),new Insets(0,5,0,0)));
        container.setBorder(BorderFactory.createLineBorder(Color.black));

        this.add(container);
    }

    private void doLoad(String loadUrl) {
        try {
            StringBuffer  ids = new StringBuffer();
            List selected = timeline.getSelected();
            String extra = "&EXTRA=";
            for(int i=0;i<selected.size();i++) {
                if(i>0) ids.append(",");
                MyDatedObject mdo = (MyDatedObject) selected.get(i);
                extra = extra+mdo;
                ids.append(mdo.id.toString());
            }
            URL base  = getDocumentBase();
            loadUrl  = loadUrl.replace("%ids%", ids.toString());
            loadUrl  = loadUrl.replace("%25ids%25", ids.toString());
            if(loadWhat!=null) {
                loadUrl  = loadUrl.replace("%loadtype%", loadWhat.getSelectedItem().toString());
                loadUrl  = loadUrl.replace("%25loadtype%25", loadWhat.getSelectedItem().toString());
            }
            int port =base.getPort();
            String portString = "";
            if(port!=80 && port!=-1) {
                portString = ":" +port;
            }
            String s = "http://" + base.getHost()  + portString +loadUrl;
            URL doc = new URL(s);
            System.err.println ("URL: " + doc);
            getAppletContext().showDocument(doc);
        } catch(Exception exc) {
            exc.printStackTrace();
        }
    }

    private static class MyDatedObject extends DatedObject {
        private Object id;
        public MyDatedObject(Date date, String label, String id) {
            super(date, label);
            this.id = id;
        }
    }

}

