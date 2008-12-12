/*
 * $Id: GeoSelectionPanel.java,v 1.17 2006/12/27 20:16:49 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Found2ation; either version 2.1 of the License, or (at
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


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;



import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HashCodeUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.*;
import ucar.unidata.view.geoloc.NavigatedMapPanel;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Holds geo-location information  - lat/lon bounding box, image size, etc.
 * This is used to pass information from a chooser into a datasource.
 */
public abstract class DataSelectionComponent {

    /** the name */
    private String name;

    /** gui content */
    JComponent contents;

    /** We can be used by a display control and this is the control's data selection */
    protected DataSelection dataSelection;

    /**
     * ctor
     *
     * @param name name
     */
    public DataSelectionComponent(String name) {
        this.name = name;
    }


    /**
     * Make if needed and return the gui contents
     *
     * @return gui contents
     */
    public JComponent getContents() {
        return getContents(null);
    }

    /**
     * Make if needed and return the gui contents
     *
     * @param dataSelection If created by a display control this is the control's dataselection
     * @return gui contents
     */
    public JComponent getContents(DataSelection dataSelection) {
        this.dataSelection = dataSelection;
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }


    /**
     * Hook method to make the contents
     *
     * @return gui contents
     */
    protected abstract JComponent doMakeContents();

    /**
     * Apply settings to the data selection
     *
     * @param dataSelection data selection to apply to
     */
    public abstract void applyToDataSelection(DataSelection dataSelection);



    /**
     * get name
     *
     * @return name
     */
    public String getName() {
        return name;
    }




}

