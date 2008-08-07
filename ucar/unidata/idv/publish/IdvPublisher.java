/*
 * $Id: IdvPublisher.java,v 1.10 2005/05/18 20:32:33 jeffmc Exp $
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

package ucar.unidata.idv.publish;



import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ucar.unidata.idv.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.net.*;

import java.io.*;

import HTTPClient.*;

import java.lang.reflect.Constructor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the start to an abstract base class that represents
 * classes that can &quot;publish&quot; IDV content.
 * <b>Note:  This framework is very much in flux</b>
 * <p>
 * Right no we just have a single concrete derived
 * class, {@link InfoceteraBlogger}, that publishes
 * weblog content to an Infocetera web log.
 * <p>
 * The idea is that we might a variety of publishing
 * mechanisms: other weblog clients, email, wiki, etc.
 * <p>
 * Instances of publishers are defined by the
 * <code>resources/publishers.xml</code> file.
 *
 * @author IDV development team
 */
public abstract class IdvPublisher {

    /**
     * A helper attribute so we can call static routines in GuiUtils
     *   without typing the whole class name
     */
    public static final GuiUtils GU = null;

    /** Xml element &quot;publisher&quot; tag name */
    public static final String TAG_PUBLISHER = "publisher";

    /**
     * Xml element &quot;class&quot; attribute name.
     * This is the class name of  a concrete derived class
     * of this class.
     */
    public static final String ATTR_CLASS = "class";



    /** Reference to the IDV */
    IntegratedDataViewer idv;

    /** The id of this publisher */
    String id = "id";

    /** The xml element that defined this publisher */
    Element initElement;




    public IdvPublisher() {}


    /**
     * Construct the object with the reference to the idv
     *
     * @param idv Reference to the idv
     * @param element The xml element that defined this publisher
     *
     */
    protected IdvPublisher(IntegratedDataViewer idv, Element element) {
        this.idv         = idv;
        this.initElement = element;
    }


    /**
     * Process the given xml, instantiating a list
     * of <code>IdvPublisher</code>s
     *
     * @param idv The idv
     * @param root Root of the publishers.xml file
     * @return List of publishers
     */
    public static List getPublishers(IntegratedDataViewer idv, Element root) {
        List publishers = new ArrayList();
        List nodes      = XmlUtil.findChildren(root, TAG_PUBLISHER);
        for (int i = 0; i < nodes.size(); i++) {
            try {
                Element child = (Element) nodes.get(i);
                Class publisherClass =
                    Misc.findClass(XmlUtil.getAttribute(child, ATTR_CLASS));
                if (publisherClass == null) {
                    continue;
                }
                Constructor ctor =
                    Misc.findConstructor(publisherClass,
                                         new Class[]{
                                             IntegratedDataViewer.class,
                                             Element.class });
                if (ctor == null) {
                    continue;
                }
                IdvPublisher idvPublisher =
                    (IdvPublisher) ctor.newInstance(new Object[]{ idv,
                                                                  child });
                idvPublisher.init();
                publishers.add(idvPublisher);
            } catch (Exception exc) {
                LogUtil.logException("Creating publisher client", exc);
            }
        }

        return publishers;
    }


    /**
     * Used to prefix persistent properties.
     *
     * @return The property prefix
     */
    public String getPropertyPrefix() {
        return "publish." + id + ".";
    }

    /**
     * Initialize this publisher. A hook so derived classes
     * can get initialized
     */
    protected void init() {}

    /**
     * Write out the preferences held by this publisher.
     * A hook for derived classes.
     */
    protected void savePreferences() {}


    /**
     * Publish the message and the given file
     *
     * @param subject The subject
     * @param label The link label for the file
     * @param msg The message
     * @param file The file
     * @param props The properties
     */
    public void publishMessageAndFile(String subject, String label,
                                      String msg, String file,
                                      Properties props) {
        if ( !isConfigured()) {
            if ( !configure()) {
                return;
            }
        }
        if ((subject != null) && (msg != null)) {
            if ( !publishMessage(subject, label, msg, file, props)) {
                return;
            }
        }
        if (file != null) {
            publishFile(file, props);
        }
    }

    /**
     * Has this publisher been configured.
     *
     * @return Is configured
     */
    public boolean isConfigured() {
        return true;
    }

    /**
     * Get the name of this publisher
     *
     * @return The name
     */
    public abstract String getName();

    /**
     * Configure this publisher
     *
     * @return Was configuration successful
     */
    public abstract boolean configure();

    /**
     * Called by others to publish the given content
     *
     * @param title Title of content
     * @param filePath File name that was saved (e.g., an image)
     * @param properties Other properties
     */
    public abstract void doPublish(String title, final String filePath,
                                   String properties);

    /**
     * Called by others to publish the given file
     *
     * @param file File name that was saved (e.g., an image)
     * @param properties Other properties
     * @return Was this successful
     */
    public abstract boolean publishFile(String file, Properties properties);


    /**
     * Called by others to publish the given content
     *
     * @param filename File name that was saved (e.g., an image)
     * @param props Other properties
     * @param subject The subject line
     * @param label The Label
     * @param msg The message
     * @return Was this successful
     */
    public abstract boolean publishMessage(String subject, String label,
                                           String msg, String filename,
                                           Properties props);



}





