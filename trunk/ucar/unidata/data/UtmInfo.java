/*
 * $Id: AddeUtil.java,v 1.5 2007/05/26 13:31:06 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;

import javax.swing.*;

import ucar.unidata.util.GuiUtils;
import java.util.List;

/**
 * A class for holding some Adde related constants and static methods
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.5 $
 */
public class UtmInfo {
    private boolean isUtm = false;
    private boolean isUtmMeters = true;
    private int utmZone = 30;
    private boolean isUtmNorth = true;

    JCheckBox isUtmCbx;
    JCheckBox isUtmMetersCbx;
    JCheckBox isUtmNorthCbx;
    JTextField utmZoneFld;


    public UtmInfo() {}

    public void getPropertiesComponents(List comps) {
        isUtmCbx = new JCheckBox("Is UTM Projection", isUtm);
        isUtmMetersCbx = new JCheckBox("Meters?", isUtmMeters);
        isUtmNorthCbx = new JCheckBox("UTM is north",isUtmNorth);
        utmZoneFld = new JTextField(""+utmZone,5);
        comps.add(GuiUtils.rLabel("UTM:"));
        comps.add(GuiUtils.vbox(isUtmCbx, GuiUtils.hbox(new JLabel("Zone: " ), GuiUtils.left(utmZoneFld),
                                                        /*isUtmMetersCbx,*/isUtmNorthCbx)));
    }


    public boolean applyProperties() {
        isUtm = isUtmCbx.isSelected();
        isUtmNorth = isUtmNorthCbx.isSelected();
        isUtmMeters = isUtmMetersCbx.isSelected();
        utmZone = new Integer(utmZoneFld.getText().trim()).intValue();
        return true;
    }

    /**
       Set the IsUtm property.

       @param value The new value for IsUtm
    **/
    public void setIsUtm (boolean value) {
	this.isUtm = value;
    }

    /**
       Get the IsUtm property.

       @return The IsUtm
    **/
    public boolean getIsUtm () {
	return this.isUtm;
    }

    /**
       Set the IsUtmNorth property.

       @param value The new value for IsUtmNorth
    **/
    public void setIsUtmNorth (boolean value) {
	this.isUtmNorth = value;
    }

    /**
       Get the IsUtmNorth property.

       @return The IsUtmNorth
    **/
    public boolean getIsUtmNorth () {
	return this.isUtmNorth;
    }

    /**
       Set the IsUtmMeters property.

       @param value The new value for IsUtmMeters
    **/
    public void setIsUtmMeters (boolean value) {
	this.isUtmMeters = value;
    }

    /**
       Get the IsUtmMeters property.

       @return The IsUtmMeters
    **/
    public boolean getIsUtmMeters () {
	return this.isUtmMeters;
    }

    /**
       Set the UtmZone property.

       @param value The new value for UtmZone
    **/
    public void setUtmZone (int value) {
	this.utmZone = value;
    }

    /**
       Get the UtmZone property.

       @return The UtmZone
    **/
    public int getUtmZone () {
	return this.utmZone;
    }



}

