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


import ucar.unidata.idv.*;
import ucar.unidata.ui.XmlTree;

import ucar.unidata.util.LogUtil;


import java.awt.*;

import javax.swing.*;




/**
 * This is  an abstract base class for defining different handlers
 * of xml documents for the {@link XmlChooser}.
 * The two primary subclasses are the {@link WmsHandler}
 * and the {@link ThreddsHandler}
 *
 * @author IDV development team
 * @version $Revision: 1.14 $Date: 2007/07/09 22:59:59 $
 */


public abstract class XmlHandler {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(XmlHandler.class.getName());


    /** The chooser we are handling xml docs for */
    protected XmlChooser chooser;

    /** The root of the xml document */
    protected Element root;

    /** This is the url or file path pointing to the xml */
    protected String path;

    /**
     * Most of the subclasses use the {@link ucar.unidata.ui.XmlTree}
     * to display the xml.
     */
    protected XmlTree tree;

    /** The GUI contents */
    JComponent contents;


    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public XmlHandler(XmlChooser chooser, Element root, String path) {
        this.chooser = chooser;
        this.root    = root;
        this.path    = path;
    }


    /**
     * _more_
     */
    protected void updateStatus() {
        chooser.setStatus("");
    }

    /**
     * Return the url or file path to the xml document
     *
     * @return The url or file path
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the GUI contents
     *
     *  @return The UI component
     */
    public final JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    /**
     * Overwritten by derived classes to actually create
     * the GUI
     *
     * @return The GUI
     */
    protected abstract JComponent doMakeContents();


    /**
     *  The user  has pressed the 'Load' button. Check if a  node is selected
     * Needs to ber overwritten by derived classes.
     */
    public abstract void doLoad();



}
