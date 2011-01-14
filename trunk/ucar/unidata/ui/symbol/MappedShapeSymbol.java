/**
 * $Id: MappedShapeSymbol.java,v 1.8 2007/05/22 20:00:21 jeffmc Exp $
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
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.ui.symbol;


import org.w3c.dom.Element;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.ui.drawing.DisplayCanvas;


import ucar.unidata.ui.drawing.Glyph;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;


import visad.*;

import java.awt.*;

import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * This is a symbol that holds a list of ShapeMap-s.
 * It allows us to map a string pattern to a particular shape type.
 * For example, if a point obs has a filed: aircraft_type which can contain
 * the values:<pre>
 * military
 * glider
 * civilian
 * ...
 * </pre>
 * This symbol could hold the shape maps:
 * <pre>
 * military -> triangle
 * glider -> circle
 * civilian->square
 * ...
 * </pre>
 * @author Metapps development team
 * @version $Revision: 1.8 $
 */
public class MappedShapeSymbol extends ShapeSymbol {

    /** List of ShapeMap-s */
    private List shapeMappings = new ArrayList();

    /** List of ShapeMap-s */
    private List mappingFields;

    /** For the gui */
    private JPanel mappingHolder;

    /**
     * Default constructor.
     */
    public MappedShapeSymbol() {}


    /**
     * Construct a ShapeSymbol without a canvas at the
     * position specified.  Use the parameter names and long names specified.
     * @param x              x position
     * @param y              y position
     */
    public MappedShapeSymbol(int x, int y) {
        this(null, x, y);
    }


    /**
     * Construct a ShapeSymbol to use on the canvas specified at the
     * position specified.  Use the parameter names and long names specified.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public MappedShapeSymbol(DisplayCanvas canvas, int x, int y) {
        super(canvas, x, y);
        setParamIds(new String[] { "param" });
        setParamDescs(new String[] { "Parameter" });
        shapeMappings = new ArrayList();
    }


    /**
     * Get the label
     *
     * @return Label for gui
     */
    public String getLabel() {
        return "Mapped Shape Symbol ";
    }



    /**
     * Add any components to the list of widgets for the main property dialog
     * tab.
     *
     * @param comps List of components.
     */
    protected void initPropertyComponents(List comps) {
        super.initPropertyComponents(comps);
        mappingHolder = new JPanel(new BorderLayout());
        updateShapeMappings();
        JScrollPane sp = new JScrollPane(mappingHolder);
        sp.setPreferredSize(new Dimension(300, 200));
        JViewport vp = sp.getViewport();
        vp.setViewSize(new Dimension(300, 200));
        comps.add(GuiUtils.top(GuiUtils.rLabel("Shape Mappings:")));
        comps.add(sp);
    }


    /**
     * Update the gui
     */
    private void updateShapeMappings() {
        if (mappingHolder == null) {
            return;
        }
        mappingFields = new ArrayList(shapeMappings);
        mappingFields.add(new ShapeMap());
        mappingFields.add(new ShapeMap());
        List comps = new ArrayList();
        comps.add(GuiUtils.cLabel("Value"));
        comps.add(GuiUtils.cLabel("Shape"));
        for (int i = 0; i < mappingFields.size(); i++) {
            ShapeMap   shapeMap     = (ShapeMap) mappingFields.get(i);
            JComponent patternField = shapeMap.getPatternWidget();
            patternField.setToolTipText(
                "<html>Enter a string pattern to match or, for numeric values<br>enter a range of the form:<br><i>min,max</i></html>");
            comps.add(patternField);
            comps.add(shapeMap.getShapeWidget());
        }
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel mappingPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_YN,
                                  GuiUtils.WT_N);
        mappingHolder.removeAll();
        mappingHolder.add(BorderLayout.CENTER, GuiUtils.top(mappingPanel));
        mappingHolder.validate();
    }


    /**
     * Apply properties from the properties dialog.
     */
    protected void applyProperties() {
        super.applyProperties();
        shapeMappings = ColorMap.applyProperties(mappingFields);
        updateShapeMappings();
    }



    /**
     * Should this symbol do all of the observations. Used by the
     * StationModelDisplayable to determine  if makeShapes should be called.
     *
     * @return Do all obs - true.
     */
    public boolean doAllObs() {
        return false;
    }


    /**
     * If this symbol is not one of the 'known' symbols then this method is
     * called to create the shapes.
     *
     * @param dataArray The array of data objects. May have 0 or more data depending
     * on the paramids.
     * @param ob The observation
     *
     * @return The shapes
     *
     * @throws Exception On badness
     */
    public VisADGeometryArray[] makeShapes(Data[] dataArray, PointOb ob)
            throws Exception {
        if ((dataArray.length == 0) || (dataArray[0] == null)) {
            return null;
        }
        ShapeMap shapeMap;
        if ((shapeMappings != null) && (shapeMappings.size() > 0)) {
            shapeMap = (ShapeMap) shapeMappings.get(0);
            if (shapeMap.isNumericRange()) {
                Unit   unit  = getDisplayUnit();
                Real   r     = (Real) dataArray[0];
                double value = ((unit != null)
                                ? r.getValue(unit)
                                : r.getValue());
                //                System.err.println ("unit:" + unit+" value=  " + r + "[" + r.getUnit() + "]  value=" + value);
                for (int i = 0; i < shapeMappings.size(); i++) {
                    shapeMap = (ShapeMap) shapeMappings.get(i);
                    Real[] range = shapeMap.getNumericRange();
                    if ((value >= range[0].getValue())
                            && (value <= range[1].getValue())) {
                        return ShapeUtility.createShape(shapeMap.getShape());
                    }
                }
                return null;
            } else {
                for (int i = 0; i < shapeMappings.size(); i++) {
                    shapeMap = (ShapeMap) shapeMappings.get(i);
                    if (shapeMap.match(dataArray[0])) {
                        return ShapeUtility.createShape(shapeMap.getShape());
                    }
                }
            }
        }
        return super.makeShapes(ob);
    }



    /**
     * Should we show the display unit widget in the properties dialog
     *
     * @return true
     */
    protected boolean showDisplayUnitInProperties() {
        return true;
    }

    /**
     * Set the ShapeMappings property.
     *
     * @param value The new value for ShapeMappings
     */
    public void setShapeMappings(List value) {
        shapeMappings = value;
    }

    /**
     * Get the ShapeMappings property.
     *
     * @return The ShapeMappings
     */
    public List getShapeMappings() {
        return shapeMappings;
    }



}

