/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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



package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Harvester {

    /** _more_ */
    public static final String TAG_HARVESTER = "harvester";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_ROOTDIR = "rootdir";


    /** _more_ */
    public static final String ATTR_MONITOR = "monitor";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_ACTIVE = "active";

    /** _more_ */
    public static final String ATTR_SLEEP = "sleep";

    /** _more_ */
    protected Harvester parent;

    /** _more_ */
    protected List<Harvester> children;

    /** _more_ */
    protected File rootDir;


    /** _more_ */
    private String name;

    /** _more_ */
    protected Repository repository;

    /** _more_ */
    private Element element;

    /** _more_ */
    private boolean monitor = false;

    /** _more_ */
    private boolean active = false;

    /** _more_ */
    private double sleepMinutes = 5;


    /** _more_          */
    private String id;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public Harvester(Repository repository) {
        this.repository = repository;
        this.id         = repository.getGUID();
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public Harvester(Repository repository, Element element)
            throws Exception {
        this(repository);
        this.children = createHarvesters(repository, element);
        for (Harvester child : children) {
            child.parent = this;
        }
        this.name    = XmlUtil.getAttribute(element, ATTR_NAME, "");
        this.monitor = XmlUtil.getAttribute(element, ATTR_MONITOR, false);
        this.active  = XmlUtil.getAttribute(element, ATTR_ACTIVE, false);
        this.sleepMinutes = XmlUtil.getAttribute(element, ATTR_SLEEP,
                sleepMinutes);
        this.rootDir = new File(XmlUtil.getAttribute(element, ATTR_ROOTDIR));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }



    /**
     * _more_
     *
     * @param repository _more_
     * @param root _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Harvester> createHarvesters(Repository repository,
            Element root)
            throws Exception {
        List<Harvester> harvesters = new ArrayList<Harvester>();
        List            children   = XmlUtil.findChildren(root,
                                         TAG_HARVESTER);
        for (int i = 0; i < children.size(); i++) {
            Element node = (Element) children.get(i);
            Class c = Misc.findClass(XmlUtil.getAttribute(node, ATTR_CLASS));
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    Element.class });
            harvesters.add((Harvester) ctor.newInstance(new Object[] {
                repository,
                node }));
        }
        return harvesters;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public File getRootDir() {
        return rootDir;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public final void run() throws Exception {
        try {
            setActive(true);
            runInner();
            setActive(false);
        } catch (Exception exc) {
            repository.log("In harvester", exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        return "";
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runInner() throws Exception {}



    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return active;
    }



    /**
     * Set the Monitor property.
     *
     * @param value The new value for Monitor
     */
    public void setMonitor(boolean value) {
        monitor = value;
    }

    /**
     * Get the Monitor property.
     *
     * @return The Monitor
     */
    public boolean getMonitor() {
        return monitor;
    }

    /**
     * Set the SleepMinutes property.
     *
     * @param value The new value for SleepMinutes
     */
    public void setSleepMinutes(double value) {
        sleepMinutes = value;
    }

    /**
     * Get the SleepMinutes property.
     *
     * @return The SleepMinutes
     */
    public double getSleepMinutes() {
        return sleepMinutes;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }



}

