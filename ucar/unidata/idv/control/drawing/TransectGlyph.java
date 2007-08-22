/*
 * $Id: TransectGlyph.java,v 1.23 2007/04/16 20:53:48 jeffmc Exp $
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

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.control.DrawingControl;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;

//import ucar.visad.FrontDrawer;
import ucar.visad.display.*;



import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import java.awt.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.ChangeEvent;




/**
 * Class TransectGlyph. Displays a shape.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.23 $
 */
public class TransectGlyph extends ShapeGlyph {


    /** The distance threshold. */
    private Real maxDataDistance;

    /** Shows the distance threshold */
    LineDrawing maxDistanceBox;

    /**
     * For glyphs that show the displayed transect for a ViewManager this is
     * the id of the view
     */
    private ViewDescriptor viewDescriptor;

    /** So we show the end point text */
    private boolean showText = true;

    /** Text displayable */
    private TextDisplayable startTextDisplayable;

    /** Text displayable */
    private TextDisplayable endTextDisplayable;

    /** The type used in the display */
    private TextType startTextType;

    /** text type */
    private TextType endTextType;

    /** Start text */
    private String startText = "B";

    /** End text */
    private String endText = "E";


    /** property widget */
    private JTextField startTextFld;

    /** property widget */
    private JTextField endTextFld;

    /** property widget */
    private JTextField maxDistanceFld;

    /** My view manager wen I am showing the display transect */
    private TransectViewManager tvm;


    /**
     * Ctor
     */
    public TransectGlyph() {
        setShapeType(SHAPE_LINE);
    }


    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param editable Is this editable
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public TransectGlyph(DrawingControl control, DisplayEvent event,
                         boolean editable)
            throws VisADException, RemoteException {
        this(control, event, editable, true);
    }

    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param editable Is this glyph editable
     * @param showText Should we show the end point text
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public TransectGlyph(DrawingControl control, DisplayEvent event,
                         boolean editable, boolean showText)
            throws VisADException, RemoteException {
        super(control, event);
        this.showText = showText;
        setShapeType(SHAPE_LINE);
        this.editable = editable;
    }


    /**
     * How many points do we interpolate along the line
     *
     * @return 10
     */
    protected int getNumInterpolationPoints() {
        return 10;
    }

    /**
     * Toggle the  visibility
     *
     * @param visible is visible
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setVisible(boolean visible)
            throws VisADException, RemoteException {
        super.setVisible(visible);
        checkBoxVisibility();
    }


    /**
     * override this because we ignore time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void checkTimeVisibility()
            throws VisADException, RemoteException {}


    /**
     * Check if we need to show/hide the max data distance box
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void checkBoxVisibility() throws VisADException, RemoteException {
        if (maxDistanceBox == null) {
            return;
        }
        if ((maxDataDistance == null) || !super.isVisible()) {
            maxDistanceBox.setVisible(false);
        } else {
            double km = maxDataDistance.getValue(CommonUnit.meter) / 1000.0;
            if (km > 2000) {
                maxDistanceBox.setVisible(false);
            } else {
                if (control != null) {
                    maxDistanceBox.setVisible(control.shouldBeVisible(this));
                }
            }
        }

    }


    /**
     * Handle glyph moved
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        super.updateLocation();
        if (points.size() < 2) {
            return;
        }
        if (showText) {
            setText(startTextDisplayable, 0, startText, startTextType);
            setText(endTextDisplayable, 1, endText, endTextType);
        }


        checkBoxVisibility();
        if ((maxDataDistance == null) || (maxDistanceBox == null)) {
            return;
        }
        double km = maxDataDistance.getValue(CommonUnit.meter) / 1000.0;
        if (km > 2000) {
            return;
        }

        EarthLocation p1       = (EarthLocation) points.get(0);
        EarthLocation p2       = (EarthLocation) points.get(1);


        MathType      mathType = RealTupleType.LatitudeLongitudeAltitude;

        Bearing baseBearing =
            Bearing.calculateBearing(p1.getLatitude().getValue(),
                                     p1.getLongitude().getValue(),
                                     p2.getLatitude().getValue(),
                                     p2.getLongitude().getValue(), null);


        double baseAngle = baseBearing.getAngle();

        LatLonPointImpl[] llps =
            new LatLonPointImpl[] {
                Bearing.findPoint(p1.getLatitude().getValue(),
                                  p1.getLongitude().getValue(),
                                  baseAngle + 90.0, km, null),
                Bearing.findPoint(p2.getLatitude().getValue(),
                                  p2.getLongitude().getValue(),
                                  baseAngle + 90.0, km, null),
                Bearing.findPoint(p2.getLatitude().getValue(),
                                  p2.getLongitude().getValue(),
                                  baseAngle - 90, km, null),
                Bearing.findPoint(p1.getLatitude().getValue(),
                                  p1.getLongitude().getValue(),
                                  baseAngle - 90, km, null),
                Bearing.findPoint(p1.getLatitude().getValue(),
                                  p1.getLongitude().getValue(),
                                  baseAngle + 90.0, km, null) };


        float[][] lineVals = getPointValues();
        float     alt      = lineVals[2][0];
        lineVals = new float[3][llps.length];
        for (int i = 0; i < lineVals[0].length; i++) {
            lineVals[0][i] = (float) llps[i].getLatitude();
            lineVals[1][i] = (float) llps[i].getLongitude();
        }


        float[][] tmp = new float[3][];

        for (int i = 0; i < lineVals[0].length - 1; i++) {
            tmp[0] =
                Misc.merge(tmp[0],
                           Misc.interpolate(2 + getNumInterpolationPoints(),
                                            lineVals[0][i],
                                            lineVals[0][i + 1]));
            tmp[1] =
                Misc.merge(tmp[1],
                           Misc.interpolate(2 + getNumInterpolationPoints(),
                                            lineVals[1][i],
                                            lineVals[1][i + 1]));
        }

        tmp[2]   = new float[tmp[0].length];
        lineVals = tmp;

        for (int i = 0; i < lineVals[0].length; i++) {
            lineVals[2][i] = alt;
        }

        Data theData = new Gridded3DSet(mathType, lineVals,
                                        lineVals[0].length);
        maxDistanceBox.setData(theData);


    }



    /**
     * Set the LineWidth property.
     *
     * @param value The new value for LineWidth
     */
    public void setLineWidth(float value) {
        super.setLineWidth(value);
        try {
            if (maxDistanceBox != null) {
                maxDistanceBox.setLineWidth(value);
            }
        } catch (Exception exc) {
            LogUtil.logException("Setting color", exc);
        }
    }


    /**
     * Set the text on the given displayable
     *
     * @param displayable The displayable
     * @param index Which one, 0 or 1
     * @param lbl The text
     * @param textType The text type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setText(TextDisplayable displayable, int index, String lbl,
                         TextType textType)
            throws VisADException, RemoteException {
        Text      t        = new Text(textType, lbl);
        Data      theData  = null;
        float[][] lineVals = getPointValues();
        if (lineVals.length == 3) {
            theData = new Tuple(new Data[] {
                new RealTuple(RealTupleType.LatitudeLongitudeAltitude,
                              new double[] { lineVals[0][index],
                                             lineVals[1][index],
                                             lineVals[2][index] }), t });
            displayable.setData(theData);
        } else {
            /*            theData = new Tuple(new Data[] {
                new RealTuple(RealTupleType.LatitudeLongitude,
                              new double[] { lineVals[0][index],
                                             lineVals[1][index]
                                             }), t });*/


        }

    }

    /**
     * Add on extra description. This shows up in the JTable
     *
     * @return Extra description
     */
    public String getExtraDescription() {
        if ( !editable) {
            return "System Transect";
        }
        return "";
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


        maxDistanceBox = new LineDrawing("TransectGlyph_" + (typeCnt++));
        maxDistanceBox.setLineWidth(getLineWidth());
        maxDistanceBox.setColor(getColor());
        addDisplayable(maxDistanceBox);


        if (showText) {
            startTextType = TextType.getTextType("TransectGlyphText_"
                    + (typeCnt++));
            startTextDisplayable = new TextDisplayable("start text_"
                    + (typeCnt++), startTextType);
            startTextDisplayable.setTextSize(control.getDisplayScale()
                                             * 2.0f);
            endTextType = TextType.getTextType("TransectGlyphText_"
                    + (typeCnt++));
            endTextDisplayable = new TextDisplayable("end text_"
                    + (typeCnt++), endTextType);
            endTextDisplayable.setTextSize(control.getDisplayScale() * 2.0f);
            addDisplayable(startTextDisplayable);
            addDisplayable(endTextDisplayable);
        }
        return true;

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
        if ( !editable) {
            return null;
        }
        return super.handleMouseDragged(event);
    }


    /**
     * Add to the properties list
     *
     * @param comps List of label/widgets
     * @param compMap Optional mapping to hold  components for later access
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        super.getPropertiesComponents(comps, compMap);
        startTextFld = new JTextField(startText, 5);
        endTextFld   = new JTextField(endText, 5);
        comps.add(GuiUtils.rLabel("Start Label:"));
        comps.add(GuiUtils.left(startTextFld));
        comps.add(GuiUtils.rLabel("End Label:"));
        comps.add(GuiUtils.left(endTextFld));
        maxDistanceFld = null;
        tvm            = null;
        if (viewDescriptor != null) {
            VMManager vmManager =
                control.getControlContext().getIdv().getVMManager();
            List vms = vmManager.getViewManagers(TransectViewManager.class);
            tvm = (TransectViewManager) VMManager.findViewManagerInList(
                viewDescriptor, vms);
            if ((tvm != null) && (maxDataDistance != null)) {
                maxDistanceFld = new JTextField(maxDataDistance.getValue()
                        + " [" + maxDataDistance.getUnit() + "]", 15);
                maxDistanceFld.setToolTipText(
                    "Maximum distance shown. e.g.: value[unit]");
                comps.add(GuiUtils.rLabel("Max distance:"));
                comps.add(GuiUtils.left(maxDistanceFld));
            }
        }

    }

    /**
     * Apply the properties
     *
     * @param compMap Optional map that holds components
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
        setStartText(startTextFld.getText().trim());
        setEndText(endTextFld.getText().trim());
        try {
            if (maxDistanceFld != null) {
                Real oldMaxDataDistance = maxDataDistance;
                maxDataDistance =
                    ucar.visad.Util.toReal(maxDistanceFld.getText().trim());
                tvm.setMaxDataDistance(maxDataDistance);
                updateLocation();
            }
        } catch (Exception exc) {
            LogUtil.userErrorMessage("Bad value:"
                                     + maxDistanceFld.getText().trim());
            return false;
        }

        return true;
    }


    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Transect";
    }

    /**
     * Set the StartText property.
     *
     * @param value The new value for StartText
     */
    public void setStartText(String value) {
        startText = value;
    }

    /**
     * Get the StartText property.
     *
     * @return The StartText
     */
    public String getStartText() {
        return startText;
    }

    /**
     * Set the EndText property.
     *
     * @param value The new value for EndText
     */
    public void setEndText(String value) {
        endText = value;
    }

    /**
     * Get the EndText property.
     *
     * @return The EndText
     */
    public String getEndText() {
        return endText;
    }


    /**
     * Get the glyph description
     *
     * @return The description
     */
    public String getDescription() {
        if (getForDisplay()) {
            return "Display";
        }
        return "Transect";

    }

    /**
     * Can the user select this glyph
     *
     * @return Is this glyph selectable
     */
    public boolean isSelectable() {
        return !getForDisplay();
    }

    /**
     * Get the ForDisplay property.
     *
     * @return The ForDisplay
     */
    public boolean getForDisplay() {
        return viewDescriptor != null;
    }

    /**
     * Set the ViewDescriptor property.
     *
     * @param value The new value for ViewDescriptor
     */
    public void setViewDescriptor(ViewDescriptor value) {
        viewDescriptor = value;
    }

    /**
     * Get the ViewDescriptor property.
     *
     * @return The ViewDescriptor
     */
    public ViewDescriptor getViewDescriptor() {
        return viewDescriptor;
    }


    /**
     * Set the MaxDataDistance property.
     *
     * @param value The new value for MaxDataDistance
     */
    public void setMaxDataDistance(Real value) {
        maxDataDistance = value;
    }

    /**
     * Get the MaxDataDistance property.
     *
     * @return The MaxDataDistance
     */
    public Real getMaxDataDistance() {
        return maxDataDistance;
    }
}

