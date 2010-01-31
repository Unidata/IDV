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

import ucar.unidata.util.GuiUtils;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;






/**
 * Bare bones chooser. You don't have to overwrite any method.
 * By default, this object will be inserted into the gui.
 * You can overwrite the doMakeContents method to construct
 * some other UI.
 *
 * To get the Idv to use this chooser component look at the file:
 * /ucar/unidata/idv/resources/choosers.xml
 *
 * @author IDV development team
 * @version $Revision: 1.11 $Date: 2007/04/16 21:36:21 $
 */

public class SkeletonChooser extends IdvChooser {

    /**
     *  The constructor that is called.
     *
     *  @param mgr The <code>IdvChooserManager</code>
     *  @param root The xml Element that was used to create this chooser.
     *  This is taken from /ucar/unidata/idv/resources/choosers.xml
     */
    public SkeletonChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     *  Construct an example UI to put into the GUI.  Note: If your chooser can Cancel
     *  then you should call super.doClose. This will close the main chooser dialog.
     *
     * @return The GUI for this chooser
     */
    protected JComponent doMakeContents() {
        return new JLabel("Gui goes here");
    }





    /**
     *  An example method that could get called from the GUI to
     * load in the selected data.
     */
    private void loadData() {
        //The definingObject argument to makeDataSource can be any object.
        //For many data sources it is a String url or file name
        String definingObject = "url to the data we chose";

        //The dataSourceId is the defined id in the /ucar/unidata/idv/resources/datasource.xml file
        //It can be null. If it is null and the defininingObject is a String
        //then the  the DataManager tries to match up the correct  data source based on
        //the patterns attribute in datasource.xml
        String dataSourceId = "DODS.GRID";

        //Call the base class method. This returns false if something failed, true otherwise.
        //If it does fail then the error is shown to the user.
        if (makeDataSource(definingObject, dataSourceId, NULL_PROPERTIES)) {
            System.err.println("Success!");
        }
    }


}
