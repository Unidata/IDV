/**
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.idv.flythrough;


import java.util.List;

import visad.DateTime;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * Class FlythroughTableModel _more_
 *
 *
 * @author IDV Development Team
 */
public class FlythroughTableModel extends AbstractTableModel {

    /** _more_          */
    Flythrough flythrough;

    /** table column  index */
    public static final int COL_LAT = 0;

    /** table column  index */
    public static final int COL_LON = 1;

    /** table column  index */
    public static final int COL_ALT = 2;

    /** table column  index */
    public static final int COL_DATE = 3;


    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public FlythroughTableModel(Flythrough flythrough) {
        this.flythrough = flythrough;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRowCount() {
        return flythrough.getPointsToUse().size();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getColumnCount() {
        return 4;
    }

    /**
     * _more_
     *
     * @param aValue _more_
     * @param rowIndex _more_
     * @param columnIndex _more_
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        List<FlythroughPoint> thePoints = flythrough.getPointsToUse();
        FlythroughPoint       pt        = thePoints.get(rowIndex);
        if (aValue == null) {
            pt.setDateTime(null);
        } else if (aValue instanceof DateTime) {
            pt.setDateTime((DateTime) aValue);
        } else {
            //??
        }
    }

    /**
     * _more_
     *
     * @param rowIndex _more_
     * @param columnIndex _more_
     *
     * @return _more_
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COL_DATE;
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param column _more_
     *
     * @return _more_
     */
    public Object getValueAt(int row, int column) {
        List<FlythroughPoint> thePoints = flythrough.getPointsToUse();
        if (row >= thePoints.size()) {
            return "n/a";
        }
        FlythroughPoint pt = thePoints.get(row);
        if (column == COL_LAT) {
            if (pt.getMatrix() != null) {
                return "matrix";
            }
            return pt.getEarthLocation().getLatitude();
        }
        if (column == COL_LON) {
            if (pt.getMatrix() != null) {
                return "";
            }
            return pt.getEarthLocation().getLongitude();
        }
        if (column == COL_ALT) {
            if (pt.getMatrix() != null) {
                return "";
            }
            return pt.getEarthLocation().getAltitude();
        }
        if (column == COL_DATE) {
            return pt.getDateTime();
        }
        return "";
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public String getColumnName(int column) {
        switch (column) {

          case COL_LAT :
              return "Latitude";

          case COL_LON :
              return "Longitude";

          case COL_ALT :
              return "Altitude";

          case COL_DATE :
              return "Date/Time";
        }
        return "";
    }
}

