/*
 * $Id: FieldSelectorWrapper.java,v 1.6 2007/04/16 21:32:37 jeffmc Exp $
 *
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control.multi;


import ucar.unidata.data.DataChoice;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.idv.control.DisplayControlImpl;


import ucar.unidata.idv.ui.*;
import ucar.unidata.ui.TableSorter;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;

import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;


import java.beans.PropertyChangeListener;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Provides a table view
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.6 $
 */
public class FieldSelectorWrapper extends DisplayComponent {

    /** The selector */
    DataSelector dataSelector;


    /**
     * Default ctor
     */
    public FieldSelectorWrapper() {
        setName("Field Selector");
    }




    /**
     * Return the human readable name of this component
     *
     * @return component type name
     */
    public String getTypeName() {
        return "Field Selector";
    }



    /**
     * Cleanup the component
     */
    public void doRemove() {
        if (dataSelector != null) {
            dataSelector.doClose();
        }
        super.doRemove();
    }


    /**
     * make the gui
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        dataSelector =
            getDisplayControl().getIdv().getIdvUIManager().createDataSelector(
                false);
        return dataSelector.getContents();
    }


}

