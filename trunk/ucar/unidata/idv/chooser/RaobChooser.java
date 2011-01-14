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



import ucar.unidata.data.sounding.RaobDataSet;

import ucar.unidata.idv.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;


import ucar.unidata.view.sounding.SoundingSelector;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A chooser class for selecting Raob data.
 * Mostly just a wrapper around a
 *  {@link ucar.unidata.view.sounding.SoundingSelector}
 * that does most of the work
 *
 * @author IDV development team
 * @version $Revision: 1.30 $Date: 2007/07/27 13:53:08 $
 */


public class RaobChooser extends IdvChooser {

    /** The data source id (from datasources.xml) that we create */
    public static final String DATASOURCE_RAOB = "RAOB";

    /**
     * An xml attribute to show or not show the server selector
     * in the gui
     */
    public static final String ATTR_SHOWSERVER = "showserver";


    /** Does most of the work */
    private SoundingSelector soundingChooser;


    /**
     * Construct a <code>RaobChooser</code> using the manager
     * and the root XML that defines this object.
     *
     * @param mgr  <code>IdvChooserManager</code> that controls this chooser.
     * @param root root element of the XML that defines this object
     */
    public RaobChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

    /**
     * Pass this on to the
     * {@link ucar.unidata.view.sounding.SoundingSelector}
     * to reload the current chooser state from the server
     */
    public void doUpdate() {
        soundingChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        boolean showServer = XmlUtil.getAttribute(chooserNode,
                                 ATTR_SHOWSERVER, true);
        soundingChooser = new SoundingSelector(this, chooserManager,
                chooserNode) {
            public void doLoad() {
                RaobChooser.this.doLoad();
            }

            public void doCancel() {
                //                closeChooser();
            }
        };
        initChooserPanel(soundingChooser);
        return soundingChooser.getContents();
    }



    /**
     * get default display to create
     *
     * @return default display
     */
    protected String getDefaultDisplayType() {
        return "raob_skewt";
    }


    /**
     * Load the data source in a thread
     */
    public void doLoadInThread() {
        List soundings = soundingChooser.getSelectedSoundings();
        if (soundings.size() == 0) {
            userMessage("Please select one or more soundings.");
            return;
        }
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);

        makeDataSource(new RaobDataSet(soundingChooser.getSoundingAdapter(),
                                       soundings), DATASOURCE_RAOB, ht);
        soundingChooser.getAddeChooser().saveServerState();
    }


}
