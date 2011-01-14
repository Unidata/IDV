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


import ucar.unidata.idv.ui.PlotModelComponent;


import ucar.unidata.ui.symbol.StationModel;


/**
 * Widget for selecting plot (station/layout) models
 *
 * @author IDV Development Team
 */

public class LayoutModelWidget extends PlotModelComponent {


    /**
     * Default constructor.
     *
     * @param control the control
     * @param layoutModelListener the listener
     * @param methodName the method to call on listener
     * @param layoutModel the layout model
     */
    public LayoutModelWidget(DisplayControlImpl control,
                             Object layoutModelListener, String methodName,
                             StationModel layoutModel) {
        this(control, layoutModelListener, methodName, layoutModel, false);
    }

    /**
     * ctor
     *
     * @param control the control
     * @param layoutModelListener the listener
     * @param methodName method on listener to call
     * @param layoutModel the layout mode
     * @param addNone should we add the 'none' entry to the widget
     */
    public LayoutModelWidget(DisplayControlImpl control,
                             Object layoutModelListener, String methodName,
                             StationModel layoutModel, boolean addNone) {
        super(control.getControlContext().getIdv(), layoutModelListener,
              methodName, layoutModel, addNone);
    }

    /**
     * edit the plot model
     */
    public void editLayoutModel() {
        super.editPlotModel();
    }

    /**
     * get the plot model
     *
     * @return the plot model
     */
    public StationModel getLayoutModel() {
        return super.getPlotModel();
    }


    /**
     * set the plot model
     *
     * @param sm the plot model
     */
    public void setLayoutModel(StationModel sm) {
        super.setPlotModel(sm);
    }
}
