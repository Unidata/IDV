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


import ucar.unidata.data.point.PointObTuple;

import ucar.unidata.idv.JythonManager;
import ucar.unidata.metdata.NamedStationImpl;

import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.TextSymbol;
import ucar.unidata.ui.symbol.WeatherSymbol;

import ucar.unidata.util.Counter;
import ucar.unidata.util.Trace;

import ucar.visad.quantities.CommonUnits;



import visad.*;

import visad.georef.*;

import visad.util.DataUtility;

import java.awt.*;

import java.rmi.RemoteException;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Provides support for display of station locations.  Each location can
 * have a station identifier (e.g., id, name, lat, lon) and/or a location
 * marker (e.g., plus, circle, star).  What gets displayed is determined
 * by the parameters set through the
 * {@link #setDisplayState(int, boolean, int, boolean)} and associated
 * methods.
 *
 * @author IDV development team.
 * @version $Revision: 1.22 $
 */
public class StationLocationDisplayable extends StationModelDisplayable {

    /** counter */
    static Counter counter = new Counter();

    /** The identifier for a station identifier */
    public static final int ID_ID = 1;

    /** The identifier for a station name */
    public static final int ID_NAME = 2;

    /** The identifier for a station latitude */
    public static final int ID_LAT = 3;

    /** The identifier for a station longitude */
    public static final int ID_LON = 4;

    /** The identifier for a station latitude/longitude */
    public static final int ID_LATLON = 5;

    /** The identifier for a station elevation (units of meters) */
    public static final int ID_ELEVATION = 6;

    /** The identifier for a station elevation (units of feet) */
    public static final int ID_ELEVATION_FT = 7;

    /** Array of station information identifiers */
    public static final int[] IDS = {
        ID_ID, ID_NAME, ID_LAT, ID_LON, ID_LATLON, ID_ELEVATION,
        ID_ELEVATION_FT
    };

    /** Array of names of station information identifiers */
    public static final String[] ID_NAMES = {
        "ID", "Name", "Latitude", "Longitude", "Lat/Lon", "Elevation (m)",
        "Elevation (ft)"
    };


    /** The identifier for no station location marker */
    public static final int SYMBOL_NONE = -1;

    /** The identifier for a square station location marker */
    public static final int SYMBOL_SQUARE = 0;

    /** The identifier for a filled square station location marker */
    public static final int SYMBOL_FSQUARE = 1;

    /** The identifier for a circle station location marker */
    public static final int SYMBOL_CIRCLE = 2;

    /** The identifier for a filled circle station location marker */
    public static final int SYMBOL_FCIRCLE = 3;

    /** The identifier for a triangle station location marker */
    public static final int SYMBOL_TRIANGLE = 4;

    /** The identifier for a filled triangle station location marker */
    public static final int SYMBOL_FTRIANGLE = 5;

    /** The identifier for a diamond station location marker */
    public static final int SYMBOL_DIAMOND = 6;

    /** The identifier for a filled diamond station location marker */
    public static final int SYMBOL_FDIAMOND = 7;

    /** The identifier for a star station location marker */
    public static final int SYMBOL_STAR = 8;

    /** The identifier for a filled start station location marker */
    public static final int SYMBOL_FSTAR = 9;

    /** The identifier for a plus station location marker */
    public static final int SYMBOL_PLUS = 14;


    /** Array of station location markers */
    public static final int[] SYMBOLS = {
        SYMBOL_SQUARE, SYMBOL_FSQUARE, SYMBOL_CIRCLE, SYMBOL_FCIRCLE,
        SYMBOL_TRIANGLE, SYMBOL_FTRIANGLE, SYMBOL_DIAMOND, SYMBOL_FDIAMOND,
        SYMBOL_STAR, SYMBOL_FSTAR, SYMBOL_PLUS
    };

    /** Array of names of station markers */
    public static final String[] SYMBOL_NAMES = {
        "Square", "Filled square", "Circle", "Filled circle", "Triangle",
        "Filled Triangle", "Diamond", "Filled diamond", "Star", "Filled star",
        "Plus"
    };


    /** Color for the station model */
    private Color myColor = null;




    /** default symbol type */
    private int symbolType = SYMBOL_FSTAR;

    /** default id type */
    private int idType = ID_ID;

    /** List of stations */
    private List stations;

    /** boolean for whether the ID should be visible */
    private boolean showId;

    /** boolean for whether the symbol should be visible */
    private boolean showSymbol;

    /**
     * Constructs an instance with the supplied reference name.  Uses
     * default station marker and station identifier.
     *
     * @param  name  reference name
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public StationLocationDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, SYMBOL_CIRCLE, true, ID_ID, true);
    }


    /**
     * Constructs an instance with the supplied parameters.
     *
     * @param  name  reference name
     * @param  symbolType  type of symbol (e.g., SYMBOL_CIRCLE)
     * @param  showSymbol  true if symbol should be shown on plot
     * @param  idType  type of station identifier (e.g., ID_ID)
     * @param  showId  true if ID should be shown on plot
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public StationLocationDisplayable(String name, int symbolType,
                                      boolean showSymbol, int idType,
                                      boolean showId)
            throws VisADException, RemoteException {
        this(name, symbolType, showSymbol, idType, showId, null);
    }

    /**
     * Constructs an instance with the supplied parameters.
     *
     * @param  name  reference name
     * @param  symbolType  type of symbol (e.g., SYMBOL_CIRCLE)
     * @param  showSymbol  true if symbol should be shown on plot
     * @param  idType  type of station identifier (e.g., ID_ID)
     * @param  showId  true if ID should be shown on plot
     * @param jythonManager Used for evaluating embedded expressions
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public StationLocationDisplayable(String name, int symbolType,
                                      boolean showSymbol, int idType,
                                      boolean showId,
                                      JythonManager jythonManager)
            throws VisADException, RemoteException {
        super(name, jythonManager);
        this.symbolType = symbolType;
        this.showSymbol = showSymbol;
        this.showId     = showId;
        this.idType     = idType;
        setStationModel(makeStationModel());
        //        counter.incr();
        //System.err.println ("DisplayMaster.ctor:"  + counter);
        //        System.err.println ("StationLocationDisplable.ctor: " + counter);
    }


    /**
     * Constructs an instance with the supplied parameters.
     *
     * @param name The name
     * @param jythonManager Used for evaluating embedded expressions
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public StationLocationDisplayable(String name,
                                      JythonManager jythonManager)
            throws VisADException, RemoteException {
        super(name, jythonManager);

        //        counter.incr();
        //System.err.println ("DisplayMaster.ctor:"  + counter);
        //        System.err.println ("StationLocationDisplable.ctor 2: " + counter);
    }





    /**
     * Constructs an instance with the supplied reference name and
     * station model view.
     *
     * @param  name  reference name
     * @param  view  station model view
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public StationLocationDisplayable(String name, StationModel view)
            throws VisADException, RemoteException {
        super(name, view);
        //        counter.incr();
        //System.err.println ("DisplayMaster.ctor:"  + counter);
        //        System.err.println ("StationLocationDisplable.ctor 3: " + counter);
    }


    /**
     * Destroy this instance
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    protected void destroy() throws RemoteException, VisADException {
        if (getDestroyed()) {
            return;
        }
        super.destroy();
        //        counter.decr();
        //        System.err.println ("StationLocationDisplable.destroy: " + counter);
    }


    /**
     * Sets the parameters used for generating a station model.
     *
     * @param  symbolType  type of symbol (e.g., SYMBOL_CIRCLE)
     * @param  showSymbol  true if symbol should be shown on plot
     * @param  idType  type of station identifier (e.g., ID_ID)
     * @param  showId  true if ID should be shown on plot
     *
     * @throws VisADException  couldn't create the necessary VisAD object
     * @throws RemoteException couldn't create the remote object
     */
    public void setDisplayState(int symbolType, boolean showSymbol,
                                int idType, boolean showId)
            throws VisADException, RemoteException {
        this.symbolType = symbolType;
        this.showSymbol = showSymbol;
        this.showId     = showId;
        this.idType     = idType;
        setStationModel(makeStationModel());
        updateDisplayable();
    }



    /**
     * Set the color of the model.
     * @param c  color to use for all components in the model
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setColor(Color c) throws VisADException, RemoteException {
        myColor = c;
        setStationModel(makeStationModel());
    }



    /**
     * Update yourself
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateDisplayable() throws VisADException, RemoteException {
        setStationData(makeStationField(stations));
    }

    /**
     * Set the symbol type (The index into WeatherSymbols.getMisc) that
     * is used for the station display.
     *
     * @param  value  set the value for the symbol type (e.g., SYMBOL_SQUARE)
     *
     * @throws VisADException  couldn't create the necessary VisAD object
     * @throws RemoteException couldn't create the remote object
     */
    public void setSymbolType(int value)
            throws VisADException, RemoteException {
        setDisplayState(value, showSymbol, idType, showId);
    }

    /**
     * Get the symbol type.
     *
     * @return the symbol type being used (e.g., SYMBOL_FSQUARE);
     */
    public int getSymbolType() {
        return symbolType;
    }

    /**
     * Set the id type  that is used for the station display.
     *
     * @param  value  set the value for the id type (e.g., ID_NAME)
     *
     * @throws VisADException  couldn't create the necessary VisAD object
     * @throws RemoteException couldn't create the remote object
     */
    public void setIdType(int value) throws VisADException, RemoteException {
        setDisplayState(symbolType, showId, value, showId);
    }

    /**
     * Get the id type.
     *
     * @return the id type being used (e.g., ID_LAT);
     */
    public int getIdType() {
        return idType;
    }


    /**
     * Set whether the station id should be shown in the station model view.
     *
     * @param value true to show the id
     *
     * @throws VisADException  couldn't create the necessary VisAD object
     * @throws RemoteException couldn't create the remote object
     * @see  #setIdType(int)
     */
    public void setShowId(boolean value)
            throws VisADException, RemoteException {
        setDisplayState(symbolType, showSymbol, idType, value);
    }

    /**
     * Get whether or not the id should be displayed.
     *
     * @return true if it should be displayed.
     */
    public boolean getShowId() {
        return showId;
    }

    /**
     * Set whether the station marker symbol should be shown in the
     * station model view.
     *
     * @param value true to show the symbol
     *
     * @throws VisADException  couldn't create the necessary VisAD object
     * @throws RemoteException couldn't create the remote object
     * @see  #setSymbolType(int)
     */
    public void setShowSymbol(boolean value)
            throws VisADException, RemoteException {
        setDisplayState(symbolType, value, idType, showId);
    }

    /**
     * Get whether or not the station marker symbol should be displayed.
     *
     * @return true if it should be displayed.
     */
    public boolean getShowSymbol() {
        return showSymbol;
    }

    /**
     * Set the list stations to be displayed.
     *
     * @param stations  List of NamedStationImpls
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public void setStations(List stations)
            throws VisADException, RemoteException {

        this.stations = stations;
        setStationData(makeStationField(stations));
    }


    /**
     * Set the stations to be displayed.
     *
     * @param stations  array of stations.
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public void setStations(NamedLocation[] stations)
            throws VisADException, RemoteException {

        setStations(makeStationList(stations));
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return this;
    }

    /** decimal format */
    private static DecimalFormat format = new DecimalFormat("####.0");

    /**
     * Format a real using the default decimal format and units.
     * @param r  Real to use for value
     * @return string representing the formatted value.
     */
    private String fmt(Real r) {
        return format.format(r.getValue());
    }

    /**
     * Format a real using the default decimal format and supplied units.
     * @param r  Real to use for value
     * @param u  Unit of value
     * @return string representing the formatted value in the supplied units.
     *         if there is an error converting to the unit, "" is returned.
     */
    private String fmt(Real r, Unit u) {
        String retString = "";
        try {
            retString = format.format(r.getValue(u));
        } catch (Exception e) {}
        return retString;
    }

    /**
     * Create a VisAD Field of point obs from the list of stations.
     * Since this is a subclass of StationModelDisplayable which
     * handles point obs, we fake the DateTime field when creating
     * the PointObTuple by setting it to NaN.
     *
     * @param stations
     * @return
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private FieldImpl makeStationField(List stations)
            throws RemoteException, VisADException {

        if ((stations == null) || stations.isEmpty()) {
            return null;
        }
        RealType index  = RealType.getRealType("index");
        Real symbolData = new Real(RealType.getRealType("SYMBOL"),
                                   symbolType);
        Integer1DSet  domain       = new Integer1DSet(index, stations.size());
        DateTime      bogusTime    = new DateTime(Double.NaN);
        FieldImpl     stationField = null;
        Data[]        theData;
        PointObTuple  pot;
        NamedLocation nlt       = null;

        List          attrNames = new ArrayList();
        List          attrTypes = new ArrayList();
        if (stations.size() > 0) {
            NamedStationImpl station    = (NamedStationImpl) stations.get(0);
            Hashtable        properties = station.getProperties();
            Enumeration      keys       = properties.keys();
            while (keys.hasMoreElements()) {
                String attrName = (String) keys.nextElement();
                attrNames.add(attrName);
                attrName = ucar.visad.Util.cleanName(attrName);
                int      cnt  = 0;
                TextType type = TextType.getTextType(attrName);
                while (type == null) {
                    cnt++;
                    type = TextType.getTextType(attrName + "_" + cnt);
                    if (cnt > 1000) {
                        break;
                    }
                }
                attrTypes.add(type);
            }
        }


        Data[] firstData = null;
        Trace.call1("make field", " stations:" + stations.size());
        for (int i = 0; i < stations.size(); i++) {
            Text   idData   = null;
            String idString = " ";
            //Put this here because we need to use it later
            NamedStationImpl station = (NamedStationImpl) stations.get(i);
            nlt = station.getNamedLocation();
            switch (idType) {

              case ID_ID :
                  idData = nlt.getIdentifier();
                  break;

              case ID_NAME :
                  idString = station.getName();
                  break;

              case ID_LAT :
                  idString = fmt(nlt.getLatitude());
                  break;

              case ID_LON :
                  idString = fmt(nlt.getLongitude());
                  break;

              case ID_LATLON :
                  idString = fmt(nlt.getLatitude()) + "/"
                             + fmt(nlt.getLongitude());
                  break;

              case ID_ELEVATION :
                  idString = fmt(nlt.getAltitude(), CommonUnit.meter);
                  break;

              case ID_ELEVATION_FT :
                  idString = fmt(nlt.getAltitude(), CommonUnits.FOOT);
                  break;

            }
            if (idData == null) {
                idData = new Text(NamedLocationTuple.IDENTIFIER_TYPE,
                                  idString);
            }

            theData = new Data[2 + attrNames.size()];
            if (firstData == null) {
                firstData = theData;
            }
            theData[0] = idData;
            theData[1] = symbolData;
            Hashtable properties = station.getProperties();
            for (int attrIdx = 0; attrIdx < attrNames.size(); attrIdx++) {
                int    arrayIndex = 2 + attrIdx;
                Object attrValue  = properties.get(attrNames.get(attrIdx));
                if ((attrValue == null)
                        && (firstData[arrayIndex] instanceof Real)) {
                    Real firstReal = (Real) firstData[arrayIndex];
                    theData[arrayIndex] =
                        new Real((RealType) firstReal.getType(), Double.NaN,
                                 firstReal.getUnit());
                    continue;
                }
                if (attrValue == null) {
                    attrValue = " ";
                }
                if (attrValue instanceof Real) {
                    theData[arrayIndex] = (Real) attrValue;
                } else {
                    TextType textType = (TextType) attrTypes.get(attrIdx);
                    try {
                        theData[arrayIndex] = new Text(textType,
                                attrValue.toString());
                    } catch (TypeException te) {
                        throw te;
                    }
                }
            }
            pot = new PointObTuple(nlt, bogusTime, new Tuple(theData, false));
            if (i == 0) {
                stationField = new FieldImpl(new FunctionType(index,
                        pot.getType()), domain);
            }
            stationField.setSample(i, pot, false, false);
        }

        Trace.call2("make field");
        return stationField;
    }


    /**
     * Creates a station model from the supplied parameters.
     * @return
     */
    private StationModel makeStationModel() {
        StationModel obView = new StationModel("StationLocation");
        TextSymbol idSymbol =
            new TextSymbol(NamedLocationTuple.IDENTIFIER_TYPE.getName(),
                           "Station ID");

        WeatherSymbol symbolSymbol = new WeatherSymbol(0, 0, "SYMBOL",
                                         "SYMBOL");

        if (myColor != null) {
            symbolSymbol.setForeground(myColor);
            idSymbol.setForeground(myColor);
        }
        symbolSymbol.setSymbolType(WeatherSymbol.SYMBOL_MISC);

        symbolSymbol.bounds = new java.awt.Rectangle(-12, -12, 24, 24);
        symbolSymbol.setRectPoint(Glyph.PT_MM);

        if (showId) {
            obView.addSymbol(idSymbol);
        }
        if (showSymbol) {
            obView.addSymbol(symbolSymbol);
        }
        if (showSymbol && showId) {
            idSymbol.bounds = new java.awt.Rectangle(-11, -31, 72, 24);
            idSymbol.setRectPoint(Glyph.PT_LM);
        } else if (showId) {
            idSymbol.bounds = new java.awt.Rectangle(-11, -8, 72, 24);
            idSymbol.setRectPoint(Glyph.PT_MM);
        } else if (showSymbol) {
            //Same position as above
        }
        return obView;
    }

    /**
     * Create a List of ucar.unidata.metdata.NamedStationImpl-s from an array
     * of visad.georef.NamedLocation-s.  In this implementation, the
     * station name is set to the ID since there is no name in a
     * NamedLocation.
     * @param stations array of stations as NamedLocations
     * @return List of NamedStations.
     */
    private List makeStationList(NamedLocation[] stations) {
        List list = new ArrayList(stations.length);
        for (int i = 0; i < stations.length; i++) {
            list.add(
                new NamedStationImpl(
                    stations[i].getIdentifier().getValue(),
                    (NamedLocationTuple) stations[i]));
        }
        return list;
    }




}
