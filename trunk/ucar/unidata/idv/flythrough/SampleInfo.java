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


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CompassPlot;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ThermometerPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import org.jfree.ui.RectangleInsets;


import org.w3c.dom.*;

import org.w3c.dom.*;

import ucar.unidata.collab.*;


import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.*;

import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.idv.ui.CursorReadoutWindow;
import ucar.unidata.idv.ui.EarthNavPanel;

import ucar.unidata.idv.ui.PipPanel;
import ucar.unidata.ui.ImagePanel;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;

import ucar.visad.display.*;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.*;

import visad.java3d.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;




import java.io.File;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.vecmath.*;

import javax.vecmath.*;


/**
 *
 * @author IDV development team
 */

public class SampleInfo {

    /** _more_ */
    private Unit unit;

    /** _more_ */
    private XYSeries series;

    /** _more_ */
    private String name;

    /** _more_ */
    private Range range;

    /** _more_ */
    private List<Real> values = new ArrayList<Real>();

    /** _more_ */
    private List<EarthLocation> locations = new ArrayList<EarthLocation>();

    /**
     * _more_
     *
     * @param name _more_
     * @param unit _more_
     * @param range _more_
     */
    public SampleInfo(String name, Unit unit, Range range) {
        this.name  = name;
        this.unit  = unit;
        this.range = range;
        String unitSuffix = "";
        if (unit != null) {
            unitSuffix = " [" + unit + "]";
        }
        series = new XYSeries(name + unitSuffix);
    }

    /**
     * _more_
     *
     * @param r _more_
     * @param loc _more_
     *
     * @throws VisADException _more_
     */
    public void add(Real r, EarthLocation loc) throws VisADException {
        double v = ((unit == null)
                    ? r.getValue()
                    : r.getValue(unit));
        series.add(values.size(), v);
        values.add(r);
        locations.add(loc);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof SampleInfo)) {
            return false;
        }
        return Misc.equals(name, ((SampleInfo) o).name);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return name.hashCode();
    }


    /**
     *  Set the Unit property.
     *
     *  @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        this.unit = value;
    }

    /**
     *  Get the Unit property.
     *
     *  @return The Unit
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }

    /**
     *  Set the Values property.
     *
     *  @param value The new value for Values
     */
    public void setValues(List<Real> value) {
        this.values = value;
    }

    /**
     *  Get the Values property.
     *
     *  @return The Values
     */
    public List<Real> getValues() {
        return this.values;
    }

    /**
     *  Set the Locations property.
     *
     *  @param value The new value for Locations
     */
    public void setLocations(List<EarthLocation> value) {
        this.locations = value;
    }

    /**
     *  Get the Locations property.
     *
     *  @return The Locations
     */
    public List<EarthLocation> getLocations() {
        return this.locations;
    }

    /**
     *  Set the Range property.
     *
     *  @param value The new value for Range
     */
    public void setRange(Range value) {
        this.range = value;
    }

    /**
     *  Get the Range property.
     *
     *  @return The Range
     */
    public Range getRange() {
        return this.range;
    }

    /**
     *  Set the Series property.
     *
     *  @param value The new value for Series
     */
    public void setSeries(XYSeries value) {
        this.series = value;
    }

    /**
     *  Get the Series property.
     *
     *  @return The Series
     */
    public XYSeries getSeries() {
        return this.series;
    }



}

