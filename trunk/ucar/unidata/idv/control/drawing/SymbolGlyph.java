/*
 * $Id: SymbolGlyph.java,v 1.3 2007/08/17 10:49:38 jeffmc Exp $
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

package ucar.unidata.idv.control.drawing;


import org.w3c.dom.Element;

import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.ShapeUtility;
import ucar.visad.WxSymbolGroup;

//import ucar.visad.FrontDrawer;
import ucar.visad.display.*;

import ucar.visad.display.ShapeDisplayable;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import java.awt.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;



/**
 * Class SymbolGlyph. Displays a shape.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SymbolGlyph extends DrawingGlyph {

    /** the symbol */
    private static String defaultSymbol = ShapeUtility.CROSS;

    /** the scale */
    private static float defaultScale = 1.0f;

    /** xml attr */
    public static final String ATTR_SCALE = "scale";

    /** xml attr */
    public static final String ATTR_SYMBOL = "symbol";

    /** the displayable */
    private ShapeDisplayable shapeDisplayable;

    /** the symbol */
    private String symbol = defaultSymbol;

    /** the scale */
    private float scale = defaultScale;


    /**
     * Ctor
     */
    public SymbolGlyph() {}


    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public SymbolGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        super(control, event);
    }



    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param symbol The symbol
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public SymbolGlyph(DrawingControl control, DisplayEvent event,
                       String symbol)
            throws VisADException, RemoteException {
        this(control, event);
        this.symbol = symbol;
    }



    /**
     * Do we have a valid number of points
     *
     * @return Is this glyph valid
     */
    public boolean isValid() {
        return (points != null) && (points.size() >= 1);
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseDragged(DisplayEvent event)
            throws VisADException, RemoteException {
        if (points.size() == 0) {
            points.add(getPoint(event));
        } else {
            points.set(0, getPoint(event));
        }
        updateLocation();
        return this;
    }

    /**
     * create the glyph
     *
     * @param event event
     *
     * @return the glyph
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {
        handleMouseDragged(event);
        return super.handleCreation(event);
    }



    /**
     * handle event
     *
     * @param event event
     *
     * @return this
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        System.err.println("mouse pressed");
        return handleMouseDragged(event);
    }


    /**
     * apply the properties from the dialog
     *
     * @param compMap components
     *
     * @return ok
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if ( !super.applyProperties(compMap)) {
            return false;
        }
        if (selectedSymbol != null) {
            defaultSymbol = (String) selectedSymbol.getId();
        }

        String newSymbol = ((selectedSymbol != null)
                            ? (String) selectedSymbol.getId()
                            : symbol);
        float  newScale  = new Float(scaleFld.getText().trim()).floatValue();
        defaultScale = newScale;
        if ((newScale != scale) || !Misc.equals(newSymbol, symbol)) {
            symbol = newSymbol;
            scale  = newScale;
            shapeDisplayable.setMarker(
                ShapeUtility.setSize(
                    ShapeUtility.makeShape(symbol),
                    scale * 0.04f * (float) control.getDisplayScale()));
        }
        return true;
    }


    /** for gui */
    private TwoFacedObject selectedSymbol;

    /** for gui */
    private JLabel symbolLabel;

    /** for gui */
    private JComboBox symbolBox;

    /** for gui */
    private JTextField scaleFld;

    /** for gui */
    private JButton symbolBtn;

    /**
     * popup list of symbols
     */
    public void showSymbolPopup() {
        List items = new ArrayList();
        List subItems;

        subItems = new ArrayList();
        for (int i = 0; i < ShapeUtility.SHAPES.length; i++) {
            subItems.add(
                GuiUtils.makeMenuItem(
                    ShapeUtility.SHAPES[i].toString(), this,
                    "setPropertySymbol", ShapeUtility.SHAPES[i]));
        }
        items.add(GuiUtils.makeMenu("General", subItems));

        List symbolGroups = WxSymbolGroup.getSymbolGroups();
        for (int i = 0; i < symbolGroups.size(); i++) {
            WxSymbolGroup s = (WxSymbolGroup) symbolGroups.get(i);
            subItems = new ArrayList();
            List symbols = s.getSymbols();
            for (int symbolIdx = 0; symbolIdx < symbols.size(); symbolIdx++) {
                TwoFacedObject tfo = (TwoFacedObject) symbols.get(symbolIdx);
                subItems.add(GuiUtils.makeMenuItem(tfo.toString(), this,
                        "setPropertySymbol", tfo));
            }


            JMenu symbolMenu = GuiUtils.makeMenu(s.getName(), subItems);
            GuiUtils.limitMenuSize(symbolMenu, "Group", 20);
            items.add(symbolMenu);
        }

        GuiUtils.showPopupMenu(items, symbolBtn);

    }



    /**
     * set the symbol
     *
     * @param tfo tfo
     */
    public void setPropertySymbol(TwoFacedObject tfo) {
        selectedSymbol = tfo;
        String name = tfo.toString();
        if (name.length() > 30) {
            name = name.substring(0, 29) + "...";
        }

        symbolLabel.setText(name);
    }

    /**
     * Make the properties widgetsF
     *
     * @param comps List of components
     * @param compMap Map to hold name to widget
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        super.getPropertiesComponents(comps, compMap);
        selectedSymbol = null;
        scaleFld       = new JTextField("" + scale, 8);
        symbolLabel    = new JLabel(" ");
        String name = WxSymbolGroup.getName(symbol);
        if (name == null) {
            name = symbol;
        }
        if (name.length() > 30) {
            name = name.substring(0, 29) + "...";
        }
        symbolLabel.setText(name);

        symbolBtn = GuiUtils.makeButton("Symbol:", this, "showSymbolPopup");



        comps.add(GuiUtils.right(symbolBtn));
        comps.add(GuiUtils.left(symbolLabel));
        comps.add(GuiUtils.rLabel("Scale:"));
        comps.add(GuiUtils.left(scaleFld));


    }


    /**
     * Init at the end
     *
     * @return Success
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {
        if ( !super.initFinalInner()) {
            return false;
        }


        RealTupleType mathType = null;
        if (isInXYSpace()) {
            mathType = RealTupleType.SpatialCartesian3DTuple;
        } else if (isInLatLonSpace()) {
            mathType = RealTupleType.LatitudeLongitudeAltitude;
        } else {
            //??
        }

        shapeDisplayable = new ShapeDisplayable("symbolglyph", mathType);
        shapeDisplayable.setMarker(
            ShapeUtility.setSize(
                ShapeUtility.makeShape(symbol),
                scale * 0.04f * (float) control.getDisplayScale()));
        shapeDisplayable.setAutoSize(true);
        addDisplayable(shapeDisplayable);
        return true;
    }



    /**
     * Init from xml
     *
     *
     * @param control The control I'm in.
     * @param node The xml node
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);
        scale  = (float) XmlUtil.getAttribute(node, ATTR_SCALE, scale);
        symbol = XmlUtil.getAttribute(node, ATTR_SYMBOL, defaultSymbol);
    }



    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_SCALE, "" + scale);
        e.setAttribute(ATTR_SYMBOL, "" + symbol);
    }



    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_SYMBOL;
    }


    /**
     * By default we don't interpolate
     *
     * @return 0
     */
    protected int getNumInterpolationPoints() {
        return 0;
    }



    /**
     * The glyph moved. Update location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() < 1) {
            return;
        }
        RealTupleType mathType = null;
        if (isInXYSpace()) {
            mathType = RealTupleType.SpatialCartesian3DTuple;
        } else if (isInLatLonSpace()) {
            mathType = RealTupleType.LatitudeLongitudeAltitude;
        } else {
            //??
        }


        float[][] lineVals = getPointValues();
        RealTuple loc = new RealTuple(mathType, new double[] { lineVals[0][0],
                lineVals[1][0], lineVals[2][0] });

        shapeDisplayable.setPoint(loc);

        super.updateLocation();


    }





    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Symbol";
    }

    /**
     * Get the description of this glyph
     *
     * @return The description
     */
    public String getDescription() {
        String name = WxSymbolGroup.getName(symbol);
        if (name == null) {
            name = symbol;
        }
        if (name.length() > 20) {
            name = name.substring(0, 19) + "...";
        }
        return getTypeName() + " - " + name;
    }



    /**
     * Set the Symbol property.
     *
     * @param value The new value for Symbol
     */
    public void setSymbol(String value) {
        symbol = value;
    }

    /**
     * Get the Symbol property.
     *
     * @return The Symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Set the Scale property.
     *
     * @param value The new value for Scale
     */
    public void setScale(float value) {
        scale = value;
    }

    /**
     * Get the Scale property.
     *
     * @return The Scale
     */
    public float getScale() {
        return scale;
    }


}


