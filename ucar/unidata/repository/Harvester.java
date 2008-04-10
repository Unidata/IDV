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


import ucar.unidata.sql.SqlUtil;
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

import java.io.ByteArrayInputStream;

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
public class Harvester extends RepositoryManager {

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
    private String name = "";


    /** _more_ */
    private Element element;

    /** _more_ */
    private boolean monitor = false;

    /** _more_ */
    private boolean active = false;

    /** _more_ */
    private boolean activeOnStart = false;

    /** _more_ */
    private double sleepMinutes = 5;


    /** _more_ */
    private String id;

    /** _more_ */
    private boolean isEditable = false;

    /** _more_ */
    protected TypeHandler typeHandler;

    /** _more_ */
    private String error;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public Harvester(Repository repository) {
        super(repository);
        this.id = repository.getGUID();
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public Harvester(Repository repository, String id) throws Exception {
        super(repository);
        this.id          = id;
        this.isEditable  = true;
        this.typeHandler = repository.getTypeHandler(TypeHandler.TYPE_FILE);

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
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        this.typeHandler =
            repository.getTypeHandler(XmlUtil.getAttribute(element,
                ATTR_TYPE, TypeHandler.TYPE_ANY));

        this.name = XmlUtil.getAttribute(element, ATTR_NAME, "");
        this.monitor = XmlUtil.getAttribute(element, ATTR_MONITOR, false);
        this.activeOnStart = this.active = XmlUtil.getAttribute(element,
                ATTR_ACTIVE, false);
        this.sleepMinutes = XmlUtil.getAttribute(element, ATTR_SLEEP,
                sleepMinutes);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        name = request.getString(ARG_NAME, name);
        typeHandler = repository.getTypeHandler(request.getString(ATTR_TYPE,
                ""));
        activeOnStart = request.get(ATTR_ACTIVE, false);
        monitor       = request.get(ATTR_MONITOR, false);
        sleepMinutes  = request.get(ATTR_SLEEP, sleepMinutes);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtil.formEntry(msgLabel("Harvester name"),
                                     HtmlUtil.input(ARG_NAME, name,
                                         HtmlUtil.SIZE_40)));
        sb.append(HtmlUtil.formEntry(msgLabel("Create entries of type"),
                                     repository.makeTypeSelect(request,
                                         false, typeHandler.getType())));
        sb.append(
            HtmlUtil.formEntry(
                msgLabel("Run"),
                HtmlUtil.checkbox(ATTR_ACTIVE, "true", activeOnStart)
                + HtmlUtil.space(1) + msg("Active on startup")
                + HtmlUtil.space(3)
                + HtmlUtil.checkbox(ATTR_MONITOR, "true", monitor)
                + HtmlUtil.space(1) + msg("Monitor") + HtmlUtil.space(3)
                + msgLabel("Sleep") + HtmlUtil.space(1)
                + HtmlUtil.input(
                    ATTR_SLEEP, "" + sleepMinutes,
                    HtmlUtil.SIZE_5) + HtmlUtil.space(1)
                                      + msg("(minutes)")));

    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !getClass().equals(o.getClass())) {
            return false;
        }
        return this.id.equals(((Harvester) o).id);
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        element.setAttribute(ATTR_CLASS, getClass().getName());
        element.setAttribute(ATTR_NAME, name);
        element.setAttribute(ATTR_ACTIVE, activeOnStart + "");
        element.setAttribute(ATTR_MONITOR, monitor + "");
        element.setAttribute(ATTR_TYPE, typeHandler.getType());
        element.setAttribute(ATTR_SLEEP, sleepMinutes + "");
        if (rootDir != null) {
            element.setAttribute(ATTR_ROOTDIR, rootDir.toString());
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getContent() throws Exception {
        Document doc  = XmlUtil.makeDocument();
        Element  root = doc.createElement(TAG_HARVESTER);
        applyState(root);
        return XmlUtil.toString(root);
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @throws Exception _more_
     */
    public void initFromContent(String content) throws Exception {
        if ((content == null) || (content.trim().length() == 0)) {
            return;
        }
        Element root =
            XmlUtil.getRoot(new ByteArrayInputStream(content.getBytes()));
        init(root);
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param filepath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry processFile(TypeHandler type, String filepath)
            throws Exception {
        return null;
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
            Harvester harvester = (Harvester) ctor.newInstance(new Object[] {
                                      repository,
                                      node });
            harvesters.add(harvester);
            harvester.init(node);
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
            error = null;
            setActive(true);
            runInner();
        } catch (Exception exc) {
            getRepository().log("In harvester", exc);
            error = "Error: " + exc + "<br>" + LogUtil.getStackTrace(exc);
        }
        setActive(false);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getError() {
        return error;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        if (error != null) {
            return "<pre>" + error + "</pre>";
        }
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

    /**
     * Set the IsEditable property.
     *
     * @param value The new value for IsEditable
     */
    public void setIsEditable(boolean value) {
        isEditable = value;
    }

    /**
     * Get the IsEditable property.
     *
     * @return The IsEditable
     */
    public boolean getIsEditable() {
        return isEditable;
    }



}

