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

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceResults;



import ucar.unidata.idv.*;


import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PollingInfo;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.filechooser.FileFilter;




/**
 * A class for choosing files that can be polled.
 *
 * @author IDV development team
 */
public class PollingFileChooser extends FileChooser {

    /** Any initial file system path to start with */
    public static final String ATTR_DIRECTORY = "directory";

    /** Polling interval */
    public static final String ATTR_INTERVAL = "interval";

    /** Pre-defined data source  id to use */
    public static final String ATTR_DATASOURCEID = "datasourceid";

    /** The title attribute */
    public static final String ATTR_TITLE = "title";

    /** The load command name */
    public static final String ATTR_LOADLABEL = "loadlabel";


    /**
     *  The chooser xml can specify a datasourceid attribute.
     *  If set this file chooser uses that (instead of relying on the
     *  file name pattern matching).
     */
    private String dfltDataSourceId;

    /** polling info */
    private PollingInfo pollingInfo;


    /**
     * Create the PollingFileChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     *
     */
    public PollingFileChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Get the tooltip for the update button
     *
     * @return The tooltip for the update button
     */
    protected String getUpdateToolTip() {
        return super.getUpdateToolTip();
    }


    /**
     * Make the UI for this chooser
     *
     * @return the UI component
     */
    protected JComponent doMakeContents() {
        pollingInfo = (PollingInfo) idv.getPreference(PREF_POLLINGINFO + "."
                + getId());
        if (pollingInfo == null) {
            pollingInfo = new PollingInfo();
            pollingInfo.setMode(PollingInfo.MODE_COUNT);
            pollingInfo.setName(getAttribute(ATTR_TITLE, ""));
            pollingInfo.setFilePattern(getAttribute(ATTR_FILEPATTERN, ""));
            pollingInfo.setFilePath(getAttribute(ATTR_DIRECTORY, ""));
            pollingInfo.setIsActive(XmlUtil.getAttribute(chooserNode,
                    ATTR_POLLON, true));

            pollingInfo.setInterval((long) (XmlUtil.getAttribute(chooserNode,
                    ATTR_INTERVAL, 5.0) * 60 * 1000));
            int    fileCount = 1;
            String s = XmlUtil.getAttribute(chooserNode, ATTR_FILECOUNT, "1");
            s = s.trim();
            if (s.equals("all")) {
                fileCount = Integer.MAX_VALUE;
            } else {
                fileCount = new Integer(s).intValue();
            }
            pollingInfo.setFileCount(fileCount);
        }

        List comps = new ArrayList();

        if ( !XmlUtil.hasAttribute(chooserNode, ATTR_DATASOURCEID)) {
            JComponent dsComp = getDataSourcesComponent();
            comps.add(GuiUtils.rLabel("Data Source Type:"));
            comps.add(GuiUtils.left(dsComp));
        }



        pollingInfo.getPropertyComponents(comps, true,
                                          XmlUtil.hasAttribute(chooserNode,
                                              ATTR_FILECOUNT));
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                            GuiUtils.WT_N);
        contents = GuiUtils.left(contents);
        setHaveData(true);
        return GuiUtils.top(GuiUtils.vbox(contents, getDefaultButtons()));
    }



    /**
     * Override the base class method to catch the do load
     */
    public void doLoadInThread() {
        Hashtable properties = new Hashtable();
        if ( !pollingInfo.applyProperties()) {
            return;
        }
        //pollingInfo.setMode(PollingInfo.MODE_COUNT);
        if (pollingInfo.hasName()) {
            properties.put(DataSource.PROP_TITLE, pollingInfo.getName());
        }
        properties.put(DataSource.PROP_POLLINFO, pollingInfo.cloneMe());

        String dataSourceId;
        if (XmlUtil.hasAttribute(chooserNode, ATTR_DATASOURCEID)) {
            dataSourceId = XmlUtil.getAttribute(chooserNode,
                    ATTR_DATASOURCEID);
        } else {
            dataSourceId = getDataSourceId();
        }
        makeDataSource(pollingInfo.getFiles(), dataSourceId, properties);
        idv.getStateManager().writePreference(PREF_POLLINGINFO + "."
                + getId(), pollingInfo);
    }


    /**
     * Override the base class method to catch the do update
     */
    public void doUpdate() {}


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoUpdate() {
        return false;
    }


    /**
     * This allows for derived classes to define their own name for the
     * "Add source" button.
     *
     * @return custom name
     */
    protected String getLoadCommandName() {
        String loadLabel = getAttribute(ATTR_LOADLABEL, (String) null);
        if (loadLabel != null) {
            return loadLabel;
        }
        return super.getLoadCommandName();
    }





}
