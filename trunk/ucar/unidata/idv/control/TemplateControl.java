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

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;

import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.visad.display.Grid3DDisplayable;



import visad.Real;
import visad.Unit;
import visad.VisADException;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/**
 * A class that serves as a template for creating new (Grid oriented) display controls
 * Cut and paste this. The methods that you need to overwrite are init and doMakeContents
 * You will also need to add an entry into the /ucar/unidata/idv/controls.properties
 * file so the IDV knows about this display control.
 *
 * @author Unidata Metapps group
 * @version $Revision: 1.27 $
 */



public class TemplateControl extends GridDisplayControl {

    /** displayable for data */
    private Grid3DDisplayable myDisplay;

    /**
     * Need to have a parameter-less constructor for the reflection based
     * object creation in the IDV to call
     */
    public TemplateControl() {}



    /**
     * After this object is created the init method is called, passing in
     * the DataChoice that was selected.
     *
     * @param dataChoice    selected data choice
     * @return   true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        //If you want the window be shown when this control is created then call:
        //setWindowVisible (true); 
        //Or in the controls.properties file define the property:
        //template_control.properties=windowVisible=true;


        //Now here you want to create any Displayable for this control, e.g.:
        //Remember: the getGridDataInstance() has not been created yet.
        myDisplay = new Grid3DDisplayable("3diso_" + paramName, true);

        //For displayables that you want to show up in a common ViewManager
        //call addDisplayable, passing the ViewDescriptor instance that
        //defines which ViewManager to put this displayable in.
        //      addDisplayable (myDisplay);
        //For now there is just one shared  ViewManager
        //Note: If something bad happens with the DataChoice below
        //and this method returns false then any Displayable that has been
        //added does not actually show up in the ViewManager.
        //Only if this method returns true.




        //Everything is fine, return true.
        return true;

    }



    /**
     *  The setData method is called when the user has selected a different
     *  DataChoice for this control. Calling  super.setData does the
     *  doMakeDataInstance call.
     *
     * @param choice    choice for data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }
        //The getGridDataInstance() has a variety of methods for accessing 
        //the data it contains:
        myDisplay.setGrid3D(getGridDataInstance().getGrid());


        return true;

    }


    /**
     * Make the UI contents for this control.
     * @return  UI container
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JPanel mainPanel = new JPanel();
        // add the toggle, color, etc icons:
        return mainPanel;
    }


    /**
     * Called by the base class when the user selects a level
     * (from the doMakeLevelControl)
     *
     * @param level   level to use
     */
    public void setLevel(Real level) {
        super.setLevel(level);
    }


}
