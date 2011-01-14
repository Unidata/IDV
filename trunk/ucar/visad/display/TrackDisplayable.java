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

package ucar.visad.display;


import ucar.unidata.data.grid.GridUtil;



import visad.*;


import java.awt.event.InputEvent;

import java.rmi.RemoteException;


/**
 * Provides support for a color coded display of a track trace.
 * @author Don Murray
 * @version $Revision: 1.12 $
 */
public class TrackDisplayable extends RGBDisplayable {

    /**
     * The name of the line-width property.
     */
    public static String LINE_WIDTH = "lineWidth";

    /** local copy of the data */
    private FieldImpl track = null;

    /** line width (pixels) */
    private float myLineWidth = 1.0f;

    /**
     * Constructs an instance with the supplied reference name.
     *
     * @param  name  reference name
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public TrackDisplayable(String name)
            throws VisADException, RemoteException {
        super(name, null, true);
        setLineWidth(myLineWidth);
    }

    /**
     * Set the track to be displayed.
     *
     * @param track must have the form (lat,lon, alt) -> param
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public void setTrack(FieldImpl track)
            throws VisADException, RemoteException {

        // get the RealType of the range from the FlatField
        RealType[] types =
            ((TupleType) GridUtil.getParamType(track)).getRealComponents();
        RealType ffldType    = types[0];

        RealType rgbRealType = getRGBRealType();

        if ((rgbRealType == null) || !ffldType.equals(rgbRealType)) {

            super.setRGBRealType(ffldType);
        }

        if (types.length > 1) {
            RealType newSelectType  = (types.length > 1)
                                      ? types[1]
                                      : getRGBRealType();

            RealType selectRealType = getSelectRealType();

            if ((selectRealType == null)
                    || !newSelectType.equals(selectRealType)) {
                setSelectRealType(newSelectType);
            }
        }

        this.track = track;

        setData(this.track);
    }

    /**
     * Sets the width of lines in this Displayable.
     *
     * @param   lineWidth     Width of lines (1 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineWidth(float lineWidth)
            throws VisADException, RemoteException {

        float oldValue;

        synchronized (this) {
            oldValue = myLineWidth;

            addConstantMaps(new ConstantMap[] {
                new ConstantMap(lineWidth, Display.LineWidth),
                new ConstantMap(lineWidth, Display.PointSize) });

            myLineWidth = lineWidth;
        }

        firePropertyChange(LINE_WIDTH, new Float(oldValue),
                           new Float(myLineWidth));
    }

    /**
     * Gets the current line width associated with this LineDrawing
     *
     * @return  line width
     */
    public float getLineWidth() {
        return myLineWidth;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return this;
    }

    /**
     * Get whether the RGB type is used for the select range.
     * @return true if the RGB type for the
     */
    public boolean getUseRGBTypeForSelect() {
        return false;
    }

}
