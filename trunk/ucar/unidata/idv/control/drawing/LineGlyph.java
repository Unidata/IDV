/*
 * $Id: LineGlyph.java,v 1.19 2007/04/16 20:53:47 jeffmc Exp $
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

//import ucar.visad.FrontDrawer;
import ucar.visad.display.*;


import visad.*;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


import javax.swing.event.ChangeEvent;



/**
 * Class LineGlyph draws lines.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.19 $
 */
public abstract class LineGlyph extends DrawingGlyph {


    /** Width */
    private float lineWidth = 1.0f;


    /** Displayable_ */
    LineDrawing lineDisplayable;



    /**
     * Ctor
     */
    public LineGlyph() {}


    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public LineGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        super(control, event);
        this.lineWidth = control.getLineWidth();
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
        createDisplayable();
        return true;
    }

    /**
     * Create the displayable to use
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void createDisplayable()
            throws VisADException, RemoteException {
        lineDisplayable = new LineDrawing("DrawingControl.ShapeGlyph"
                                          + (uniqueCnt++));
        lineDisplayable.setLineWidth(getLineWidth());
        addDisplayable(lineDisplayable);
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
        lineWidth = XmlUtil.getAttribute(node, ATTR_LINEWIDTH, 1.0f);
    }

    /**
     * Init from user
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     *
     * @return ok
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        lineWidth = control.getLineWidth();
        return super.initFromUser(control, event);
    }



    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_LINEWIDTH, "" + lineWidth);
    }









    /**
     * Set the LineWidth property.
     *
     * @param value The new value for LineWidth
     */
    public void setLineWidth(float value) {
        lineWidth = value;
        try {
            if (lineDisplayable != null) {
                lineDisplayable.setLineWidth(lineWidth);
            }
        } catch (Exception exc) {
            LogUtil.logException("Setting color", exc);
        }
    }

    /**
     * Get the LineWidth property.
     *
     * @return The LineWidth
     */
    public float getLineWidth() {
        return lineWidth;
    }




    /**
     * Apply props
     *
     * @param compMap Holds mapping of attribute name to widget
     *
     *
     * @return Success
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if ( !super.applyProperties(compMap)) {
            return false;
        }
        JComboBox box = (JComboBox) compMap.get(ATTR_LINEWIDTH);
        if (box == null) {
            return true;
        }
        Integer value = (Integer) box.getSelectedItem();
        setLineWidth((float) value.intValue());
        return true;
    }


    /**
     *  Get properties  widgets
     *
     * @param comps List of widgets
     * @param compMap Holds mapping of attribute name to widget
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        if (lineDisplayable != null) {
            JComboBox box = control.doMakeLineWidthBox((int) lineWidth);
            comps.add(GuiUtils.rLabel("Line Width:"));
            comps.add(GuiUtils.left(box));
            compMap.put(ATTR_LINEWIDTH, box);
        }
        super.getPropertiesComponents(comps, compMap);
    }




}

