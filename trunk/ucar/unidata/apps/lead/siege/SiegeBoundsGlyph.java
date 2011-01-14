/*
 * $Id: SiegeBoundsGlyph.java,v 1.3 2006/05/16 16:15:18 jeffmc Exp $
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




package ucar.unidata.apps.lead.siege;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.drawing.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 *
 * @author MetApps development team
 * @version $Revision: 1.3 $
 */

public class SiegeBoundsGlyph extends ShapeGlyph {

    /** The displayable */
    LineDrawing lineDisplayable;

    /**
     * Need this constructor for un-persistence
     */
    public SiegeBoundsGlyph() {}

    /**
     * Init at the end
     *
     * @return Success
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner() throws VisADException,
            RemoteException {
        if ( !super.initFinalInner()) {
            return false;
        }
        lineDisplayable = new LineDrawing("SiegeBoundsGlyph" + (uniqueCnt++));
        setLineWidth(2);
        lineDisplayable.setLineWidth(2);
        addDisplayable(lineDisplayable);
        return true;
    }


    /**
     * Create a new Drawing Control; set attributes.
     *
     * @param control The drawing control I am part of
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public SiegeBoundsGlyph(
            SiegeDrawingControl control) throws VisADException,
                RemoteException {
        super(control, null, SHAPE_RECTANGLE);
        setCoordType(DrawingGlyph.COORD_LATLON);
        List            pointStrings = new ArrayList();
        LatLonPointImpl center       = new LatLonPointImpl(40, -90);
        pointStrings.add(center.getLatitude() + "");
        pointStrings.add(center.getLongitude() + "");
        processPointStrings(pointStrings);
    }

    /**
     * round the lat/lon value
     *
     * @param f _more_
     *
     * @return _more_
     */
    private float round(float f) {
        return ((int) (f * 100)) / 100.0f;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected LatLonPointImpl getCenter() throws VisADException,
            RemoteException {
        float[][] lineVals  = getPointValues();
        double    centerLat = lineVals[IDX_LAT][0];
        double    centerLon = lineVals[IDX_LON][0];
        return new LatLonPointImpl(centerLat, centerLon);

    }


    /**
     * The glyph has moved
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() == 0) {
            return;
        }
        float[][]       lineVals  = getPointValues();
        LatLonPointImpl centerLLP = getCenter();
        double          fixedAlt  = getFixedAltitude();
        float[][]       pts       = new float[3][5];

        //Need to figure out the real radius
        double radiusKM = 600;
        for (int i = 0; i < 5; i++) {
            pts[2][i] = (float) fixedAlt;
            LatLonPointImpl llp = Bearing.findPoint(centerLLP, 45 + 90 * i,
                                      radiusKM, null);
            //Need to round
            pts[0][i] = round((float) llp.getLatitude());
            pts[1][i] = round((float) llp.getLongitude());
        }
        setActualPoints(pts);
        Data theData =
            new Gridded3DSet(RealTupleType.LatitudeLongitudeAltitude, pts, 5);
        lineDisplayable.setData(theData);
        //Update the selection points;
        //super.updateLocation();
    }

    /**
     * noop
     *
     * @param selected is selected
     */
    public void setSelected(boolean selected) {
        //NOOP
    }

    /**
     * Is this glyph valid. 
     *
     * @return By default return true
     */
    public boolean isValid() {
        return true;
    }

}

