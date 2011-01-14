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


import edu.wisc.ssec.mcidas.adde.GetAreaGUI;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.ImageDataset;



import ucar.unidata.idv.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;






/**
 * This is a wrapper around VisAD's ADDE image chooser
 *
 * @author IDV development team
 * @version $Revision: 1.20 $Date: 2007/04/16 21:36:21 $
 */


public class McidasImageChooser extends IdvChooser {

    /** widget */
    private GetAreaGUI gag;


    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McidasImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Just create the GetAreaGUI, adding this chooser as an action
     * listener to route the selection events.
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        gag = new GetAreaGUI(null, false, "Get Images", true, false, false) {
            public void doCancel() {
                closeChooser();
            }
        };
        gag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                McidasImageChooser.this.doLoad();
            }
        });
        return gag;
    }


    /**
     * Load the selected list of URLs as an ADDE.IMAGE data source
     *
     */
    public void doLoadInThread() {
        List source = gag.getImageList();
        makeDataSource(StringUtil.listToStringArray(source), "ADDE.IMAGE",
                       NULL_PROPERTIES);
    }

}
