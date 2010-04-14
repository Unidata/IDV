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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.visad.Util;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.LineDrawing;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.functiontypes.InSituAirTemperatureProfile;
import ucar.visad.functiontypes.PolarHorizontalWindOfPressure;
import ucar.visad.quantities.AirPressure;

import visad.CoordinateSystem;

import visad.DateTime;

import visad.ErrorEstimate;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.Gridded1DSet;

import visad.Gridded3DSet;

import visad.Real;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.SetType;

import visad.TupleType;

import visad.Unit;

import visad.VisADException;

import visad.georef.LatLonTuple;



import java.awt.Component;

import java.rmi.RemoteException;

import javax.swing.JLabel;


/**
 * <p>Creates an aerological Skew-T diagram for soundings from track (i.e.
 * aircraft) data.</p>
 *
 * @author Stuart Wier
 * @author Jeff McWhirter
 * @author Steven Emmerson
 * @version $Revision: 1.4 $Date: 2006/12/01 20:16:39 $
 */
public class TrackSoundingControl extends AerologicalSoundingControl {

    /** Displayable for the track */
    private final DisplayableData track;

    /** time at bottom of profile */
    private DateTime bottomTime;

    /**
     * Constructs from nothing.
     *
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public TrackSoundingControl() throws VisADException, RemoteException {

        super(true);

        track = new LineDrawing("TrackSoundingControl track");
        track.setPointSize(2f);
    }

    /**
     * Creates the display and control buttons from a {@link
     * ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice       The data for this instance.
     * @return                 <code>true</code> if and only if this instance
     *                         was correctly initialized.
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException couldn't create a remote object needed
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        /*
         * Initialize the superclass.
         */
        if ( !super.init()) {
            return false;
        }

        setSpatialLoci(track);

        if ( !setData(dataChoice)) {
            return false;
        }


        Field     field             = (Field) getDataInstance().getData();
        float[][] params            = field.getFloats();
        float[]   press             = params[0];
        float[]   temps             = params[1];
        float[]   dews              = params[2];
        float[]   spds              = params[3];
        float[]   dirs              = params[4];
        float[]   lats              = params[5];
        float[]   lons              = params[6];
        float[]   alts              = params[7];
        Set       timeDomain        = field.getDomainSet();
        int[]     indexes           = Util.strictlySortedIndexes(press,
                                          false);
        Unit[]    defaultRangeUnits = field.getDefaultRangeUnits();

        press = Util.take(press, indexes);
        temps = Util.take(temps, indexes);
        dews  = Util.take(dews, indexes);
        spds  = Util.take(spds, indexes);
        dirs  = Util.take(dirs, indexes);
        lats  = Util.take(lats, indexes);
        lons  = Util.take(lons, indexes);
        alts  = Util.take(alts, indexes);
        bottomTime =
            new DateTime(new Real((RealType) ((SetType) timeDomain.getType())
                .getDomain().getComponent(0), timeDomain
                .indexToDouble(new int[] { 0 })[0][0], timeDomain
                .getSetUnits()[0]));

        setLocation(
            new LatLonTuple(
                new Real(RealType.Latitude, lats[0], defaultRangeUnits[5]),
                new Real(RealType.Longitude, lons[0], defaultRangeUnits[6])));
        track.setData(
            new Gridded3DSet(
                RealTupleType.LatitudeLongitudeAltitude, new float[][] {
            lats, lons, alts
        }, lats.length));

        Set presDomain = new Gridded1DSet(AirPressure.getRealTupleType(),
                                          new float[][] {
            press
        }, press.length, (CoordinateSystem) null,
           new Unit[] { defaultRangeUnits[0] }, (ErrorEstimate[]) null);
        Field tempPro = new FlatField(InSituAirTemperatureProfile.instance(),
                                      presDomain);
        Field dewPro = new FlatField(DewPointProfile.instance(), presDomain);
        Field windPro =
            new FlatField(PolarHorizontalWindOfPressure.instance(),
                          presDomain);

        tempPro.setSamples(new float[][] {
            temps
        });
        dewPro.setSamples(new float[][] {
            dews
        });
        windPro.setSamples(new float[][] {
            spds, dirs
        });
        setSoundings(new Field[] { tempPro }, new Field[] { dewPro },
                     new Field[] { windPro });
        setSounding(0);
        addDisplayable(track, FLAG_COLOR);

        return true;
    }



    /**
     * Override the base class method to return the relevant name
     * @return  the text for the label
     */
    protected String getSpatialLociLabel() {
        return "Track";
    }

    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display.</p>
     *
     * @return                      The data-specific control-component.
     */
    Component getSpecificWidget() {
        return new JLabel(bottomTime.toString());
    }
}
