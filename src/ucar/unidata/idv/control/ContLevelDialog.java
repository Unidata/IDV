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


import ucar.unidata.idv.ui.ContourInfoDialog;

import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.visad.display.*;



import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.rmi.RemoteException;




import java.util.*;

import javax.swing.*;

import javax.swing.event.*;



/**
 * A JFrame widget to get contouring info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.35 $
 */
public class ContLevelDialog extends ContourInfoDialog implements ActionListener {


    /** The display */
    private DisplayControlImpl displayControl;


    /**
     * Construct the widget.
     * with interval, min, max entry boxes
     * and ok and cancel buttons.
     *
     * @param displayControl The display
     * @param title  title for frame
     * @param unit The unit to display
     */
    public ContLevelDialog(DisplayControlImpl displayControl, String title,
                           Unit unit) {
        super(title, true, unit);
        this.displayControl = displayControl;
    }




    /**
     * Apply the state to the display
     *
     * @return Was this successful
     */
    public boolean doApply() {
        if ( !super.doApply()) {
            return false;
        }
        try {
            displayControl.setContourInfo(new ContourInfo(getInfo()));
            return true;
        } catch (Exception exc) {
            displayControl.logException("Setting contours", exc);
            return false;
        }
    }


}
