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

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataManager;



import ucar.unidata.idv.*;

import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.util.Vector;

import javax.swing.*;




/**
 * A no-op class for now but this will provide common facilities  to
 * run data loads in threads, cancel the load, etc.
 *
 * @author IDV development team
 * @version $Revision: 1.9 $Date: 2007/04/16 21:36:20 $
 */

public abstract class ChooserRunnable implements Runnable {

    /** Reference to the chooser that is using me_ */
    protected IdvChooser chooser;

    /** Was the load canceled */
    private boolean canceled = false;

    /**
     * Create the object
     *
     * @param chooser The chooser
     *
     */
    public ChooserRunnable(IdvChooser chooser) {
        this.chooser = chooser;
    }

    /**
     * Close the chooser
     */
    public void doClose() {
        chooser.doClose();
    }

    /**
     * Was the choosing canceled
     * @return Was canceled
     */
    public boolean getCanceled() {
        return canceled;
    }

}
