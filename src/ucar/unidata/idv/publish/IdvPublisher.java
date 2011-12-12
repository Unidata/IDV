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
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.net.*;

import java.io.*;

import HTTPClient.*;



import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the start to an abstract base class that represents
 * classes that can &quot;publish&quot; IDV content.
 * <b>Note:  This framework is very much in flux</b>
 * <p>
 * Right no we just have a single concrete derived
 * class, InfoceteraBlogger, that publishes
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
public abstract class IdvPublisher implements Cloneable {

    private boolean local = false;


    private String name="Publisher";

    /** Reference to the IDV */
    private IntegratedDataViewer idv;

    /** The id of this publisher */
    private     String id = "id";

    /** The xml element that defined this publisher */
    Element initElement;



    public IdvPublisher() {}


    public IdvPublisher cloneMe() throws CloneNotSupportedException {
        return (IdvPublisher) this.clone();
    }

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


    public IntegratedDataViewer getIdv() {
        return idv;
    }


    protected void setIdv(IntegratedDataViewer idv) {
        this.idv = idv;
    }

    public boolean doInitNew() {
        return true;
    }


    public void initMenu(JMenu menu) {
        menu.add(GuiUtils.makeMenuItem("Publish to " + getName(),this,"doPublish"));
    }


    public void publishIslImage(Element tag, Image image) {
    }


    public void doPublish() {
    }

    public void publishContent(String file,ViewManager fromViewmanager) {}


    /**
     * Initialize this publisher. A hook so derived classes
     * can get initialized
     */
    protected void init() {}



    /**
     * Get the name of this publisher
     *
     * @return The name
     */
    public abstract String getTypeName();

    public boolean configurexxx(){
        return true;
    }

    public void configure(){
    }



    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }


    public String toString() {
        return getName();
    }

    /**
       Set the Local property.

       @param value The new value for Local
    **/
    public void setLocal (boolean value) {
	local = value;
    }

    /**
       Get the Local property.

       @return The Local
    **/
    public boolean getLocal () {
	return local;
    }



}





