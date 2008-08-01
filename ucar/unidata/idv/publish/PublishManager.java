/*
 * $Id: PublishManager.java,v 1.13 2005/05/13 18:31:06 jeffmc Exp $
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


import ucar.unidata.idv.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.IOUtil;



import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.List;


import org.w3c.dom.Element;
import org.w3c.dom.Node;


import ucar.unidata.idv.ui.*;


/**
 *  This manages  the nascent publishing facility within the IDV.
 * This whole framework needs to be thought out a bit more but for
 * now we have on instance of a publisher: InfoceteraBlogger
 * that knows how to post articles and files to an infocetera web log
 * server.
 *
 *
 * @author IDV development team
 */


public class PublishManager extends IdvManager {

    /** List of {@link ucar.unidata.idv.publish.IdvPublisher}s */
    private List publishers;


    /**
     * Create me with the IDV
     *
     * @param idv The IDV
     */
    public PublishManager(IntegratedDataViewer idv) {
        super(idv);
    }

    public void newPublisher() {

    }


    /**
     * Do we have any publishers
     *
     * @return Can we publish any content
     */
    public boolean isPublishingEnabled() {
        //For now just return false
        return true;
        //        return false;
        //return ((publishers != null) && (publishers.size() > 0));
    }

    /**
     * If publishing is not enabled then show a user message and return false
     *
     * @return Is publishing enabled
     */
    public boolean publishCheck() {
        if ( !isPublishingEnabled()) {
            LogUtil.userMessage("No publishing available");
            return false;
        }
        return true;
    }

    /**
     * Gets a publisher.
     *
     * @return THe first publisher in the list of publishers (for now)
     */
    public IdvPublisher getPublisher() {
        //For now jsut return the first one
        return (IdvPublisher) publishers.get(0);
    }



    /**
     * This xml encoders the given object as a xidv bundle file
     * and publishes it with the given description
     *
     * @param desc The description
     * @param object The object to encode and publish
     */
    public void publishObject(String desc, Object object) {
        try {
            publishXml(desc, getIdv().getEncoderForWrite().toElement(object),
                       ".xidv");
        } catch (Exception exc) {
            logException("Publishing " + desc, exc);
        }
    }

    /**
     * Publish the xml
     *
     * @param desc The description
     * @param root The xml root to publish
     */
    public void publishXml(String desc, Element root) {
        publishXml(desc, root, ".xml");
    }

    /**
     * Publish the xml
     *
     * @param desc The description
     * @param root The xml root to publish
     * @param fileExt The file extension
     */
    public void publishXml(String desc, Element root, String fileExt) {
        try {
            String xml  = XmlUtil.toString(root);
            String uid  = Misc.getUniqueId();
            String tail = uid + fileExt;
            String file = IOUtil.joinDir(getStore().getUserTmpDirectory(),
                                         tail);
            IOUtil.writeFile(file, xml);
            doPublish("Publish " + desc, file);
        } catch (Exception exc) {
            logException("Publishing " + desc, exc);
        }
    }


    /**
     * Publish a message
     */
    public void publishMessage() {
        publishMessage(null);
    }

    /**
     * Publish a message with the given properties(?)
     *
     * @param properties The properties
     */
    public void publishMessage(String properties) {
        doPublish("Publish message", null, properties);
    }


    /**
     * Publish the idv bundle
     */
    public void publishState() {
        publishState(null);
    }

    /**
     * Publish the idv bundle
     *
     * @param properties The properties
     */
    public void publishState(String properties) {
        String uid  = Misc.getUniqueId();
        String tail = uid + ".jnlp";
        String file = IOUtil.joinDir(getStore().getUserTmpDirectory(), tail);
        getPersistenceManager().doSave(file);
        doPublish("Publish bundle file", file, properties);
    }

    /**
     * Publish the file
     *
     * @param title Title to use
     * @param filePath The file
     */
    public void doPublish(String title, String filePath) {
        doPublish(title, filePath, null);
    }


    /**
     * Publish the file
     *
     * @param title Title to use
     * @param filePath The file
     * @param properties The properties
     */
    public void doPublish(String title, String filePath, String properties) {
        if ( !publishCheck()) {
            return;
        }
        getPublisher().doPublish(title, filePath, properties);
    }

    /**
     * Initialize me
     */
    public void initPublisher() {
        try {
            publishers = new ArrayList();
            //            if (true) return;
            Element root =
                XmlUtil.getRoot("/ucar/unidata/idv/resources/publishers.xml",
                                getClass());
            if (root != null) {
                publishers.addAll(IdvPublisher.getPublishers(getIdv(), root));
            }
        } catch (Exception exc) {
            logException("Initializing publishers", exc);
        }
    }


    /**
     * Get the list of Publishers
     *
     * @return The publishers
     */
    public List getPublishers() {
        return publishers;
    }





}








