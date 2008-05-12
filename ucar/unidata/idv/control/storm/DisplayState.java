/**
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.idv.control.storm;
import ucar.unidata.util.LogUtil;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class DisplayState {

    private JCheckBox cbx;
    private boolean visible;
    private String name;

    public DisplayState() {
    }

    public DisplayState(String name, boolean visible) {
        this.name = name;
        this.visible = visible;
    }


    public JCheckBox getCheckBox(final StormDisplayState stormDisplayState) {
        if (cbx == null) {
            cbx = new JCheckBox("", getVisible());
            cbx.setToolTipText(name);
            cbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setVisible(cbx.isSelected());
                        stormDisplayState.updateDisplays();
                    } catch (Exception exc) {
                        LogUtil.logException("Toggling way visible", exc);
                    }
                }
            });
        }
        return cbx;
    }




/**
Set the Visible property.

@param value The new value for Visible
**/
public void setVisible (boolean value) {
	visible = value;
}

/**
Get the Visible property.

@return The Visible
**/
public boolean getVisible () {
	return visible;
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


}

