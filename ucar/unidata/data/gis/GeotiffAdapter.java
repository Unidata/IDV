/*
 * $Id: GeotiffAdapter.java,v 1.10 2006/12/01 20:42:31 jeffmc Exp $
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



package ucar.unidata.data.gis;


import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.epsg.CoordinateOperationMethod;
import ucar.unidata.gis.epsg.CoordinateOperationParameter;
import ucar.unidata.gis.epsg.Pcs;


import ucar.unidata.gis.geotiff.*;
import ucar.unidata.util.Misc;

import visad.*;

import visad.data.*;
import visad.data.jai.*;
import visad.data.tiff.*;

import visad.georef.*;

import visad.jmet.GRIBCoordinateSystem;

import com.sun.media.jai.codec.*;

import java.io.*;


import java.util.Hashtable;
import java.util.List;


/**
 * An adapter for GeoTIFF data files.
 *
 * @author IDV development team
 * @version $Revision: 1.10 $
 */
public class GeotiffAdapter {

    /** no value flag */
    private static final int NOVALUE = -1;

    /** flag for a transverse mercator projection */
    private static final String PROJ_TRANSVERSEMERCATOR =
        "transverse mercator";

    /** user defined flag */
    private static final int USER_DEFINED = 32767;

    /** TIFF parameter */
    private static final CoordinateOperationParameter COM = null;

    /** angular units */
    private int angularUnits = GeoKeys.EpsgUnit.Degree;



    /** the filename */
    private String filename;

    /** the TIFF directory */
    private TIFFDirectory dir;

    /** the TIFF field */
    private TIFFField dirField;

    /** the tie point field */
    private TIFFField tiePointField;

    /** the scale field */
    private TIFFField scaleField;


    /** flag for geotiff (otherwise a regular TIFF) */
    private boolean isGeotiff = false;



    /** number of colums (elements) in the image */
    private int cols;

    /** number of rows (lines) in the image */
    private int rows;

    /**
     *  Holds a mapping of paramCode ->valueXuom
     */
    private Hashtable paramCodeMap = new Hashtable();

    /**
     * Create a new GeoTiffAdapter
     *
     * @param filename         filename or URL
     *
     * @throws IOException  problem opening the file
     *
     */
    public GeotiffAdapter(String filename) throws IOException {
        this.filename = filename;
        this.dir      = getDirectory();
        init();
    }

    /**
     * Get the data for this source.
     *
     * @return  associated data
     *
     * @throws IOException              problem opening file
     * @throws VisADException           VisAD problem
     */
    public FlatField getData() throws VisADException, IOException {
        return getDataAsRgb();
    }

    /**
     * Get the data for this source.
     *
     * @return  associated data
     *
     * @throws IOException              problem opening file
     * @throws VisADException           VisAD problem
     */
    public FlatField getDataAsRgb() throws VisADException, IOException {
        return createData(true);
    }


    /**
     * Get the data for this source.
     *
     * @return  associated data
     *
     * @throws IOException              problem opening file
     * @throws VisADException           VisAD problem
     */
    public FlatField getDataAsGrid() throws VisADException, IOException {
        return createData(false);
    }

    /**
     * See if this is a GeoTIFF file or not
     *
     * @return   true if GeoTIFF
     */
    public boolean getIsGeotiff() {
        return isGeotiff;
    }

    /**
     * See if this has a projection
     *
     * @return  true if data has a projection
     *
     * @throws IOException              problem opening file
     * @throws VisADException           VisAD problem
     */
    public boolean getHasProjection() throws VisADException, IOException {
        return (getMapProjection() != null);
    }


    /**
     * Make the data
     *
     *
     * @param asRGB _more_
     *
     * @return _more_
     * @throws IOException              problem opening file
     * @throws VisADException           VisAD problem
     */
    private FlatField createData(boolean asRGB)
            throws VisADException, IOException {
        Form form;
        if (asRGB) {
            form = new LegacyTiffForm();
        } else {
            form = new TiffForm();
        }
        FlatField   field  = (FlatField) form.open(filename);
        Linear2DSet domain = (Linear2DSet) field.getDomainSet();
        cols = domain.getX().getLength();
        rows = domain.getY().getLength();
        MapProjection projection = getMapProjection();
        if (projection != null) {
            //            System.err.println ("got projection:" + projection.getClass().getName() + " " +projection);
            //            SampledSet  newDomain = new Linear2DSet(domain.getType(), domain.getX(), domain.getY(),width,height, projection);
            //RealTupleType.SpatialEarth2DTuple
            RealTupleType rtt =
                new RealTupleType(((SetType) domain.getType()).getDomain()
                    .getRealComponents(), projection, null);
            SampledSet newDomain = new Linear2DSet(rtt,
                                       new Linear1DSet[] { domain.getX(),
                    domain.getY() }, null, null, null, false);
            field =
                (FlatField) ucar.unidata.data.grid.GridUtil.setSpatialDomain(
                    field, newDomain);

        }
        return field;
    }

    /**
     * Get the directory for the TIFF image
     * @return   the directory
     *
     * @throws IOException   problem opening the file
     */
    private TIFFDirectory getDirectory() throws IOException {
        if (dir == null) {
            ImageDecoder imageDecoder = ImageCodec.createImageDecoder("tiff",
                                            new File(filename), null);
            SeekableStream stream = imageDecoder.getInputStream();
            dir = new TIFFDirectory(stream, 0);
        }
        return dir;
    }


    /**
     * Convert a value to degrees
     *
     * @param v    value to convert
     * @return  converted value.
     */
    private double toDegrees(double v) {
        switch (angularUnits) {

          case GeoKeys.EpsgUnit.Degree :
              return v;

          /**
           * //TODO finish these
           * case GeoKeys.EpsgUnit.Radian:
           * return v;
           * case GeoKeys.EpsgUnit.:
           * return v;
           * case GeoKeys.EpsgUnit.:
           * return v;
           * case GeoKeys.EpsgUnit.:
           * return v;
           * case GeoKeys.EpsgUnit.:
           * return v;
           * case GeoKeys.EpsgUnit.:
           * return v;
           */

        }
        return v;
    }

    /**
     * Initialize this Adapter
     *
     * @throws IOException   problem opening file
     */
    private void init() throws IOException {
        tiePointField = dir.getField(GeoKeys.Tiff.GEO_TIEPOINTS);
        scaleField    = dir.getField(GeoKeys.Tiff.GEO_PIXEL_SCALE);

        dirField      = dir.getField(GeoKeys.Tiff.GEO_KEY_DIRECTORY);
        if (dirField == null) {
            isGeotiff = false;
            return;
        }
        isGeotiff = true;

        for (int tagIdx = 4; tagIdx < dirField.getCount(); tagIdx += 4) {
            int      geoKeyId = (int) dirField.getAsLong(tagIdx);
            long     location = dirField.getAsLong(tagIdx + 1);
            int      count    = (int) dirField.getAsLong(tagIdx + 2);
            int      offset   = (int) dirField.getAsLong(tagIdx + 3);
            String   keyName  = GeoKeys.Geokey.getName((int) geoKeyId);
            double[] value;
            if (location == 0) {
                value = new double[] { offset };
            } else {
                TIFFField subField = dir.getField((int) location);
                if (subField == null) {
                    continue;
                }
                if (subField.getType() == TIFFField.TIFF_ASCII) {
                    continue;
                }
                //TODO - actually use the offset here...
                //                print ("subfield:", subField);

                if (subField.getType() == TIFFField.TIFF_LONG) {
                    value = toDouble(subField.getAsLongs());
                } else if (subField.getType() == TIFFField.TIFF_DOUBLE) {
                    value = subField.getAsDoubles();
                } else if (subField.getType() == TIFFField.TIFF_FLOAT) {
                    value = toDouble(subField.getAsFloats());
                } else {
                    continue;
                }
                double[] subset = new double[count];
                for (int i = 0; i < subset.length; i++) {
                    subset[i] = value[i + offset];
                }
                value = subset;
            }
            //            System.err.println ("GeoKey:" + GeoKeys.Geokey.getName (geoKeyId) +  " " + toString (value));
            processKey(geoKeyId, value);
        }

    }


    /**
     * Print out a double array
     *
     * @param a   array to print
     * @return  string of values
     */
    private String toString(double[] a) {
        if (a.length == 1) {
            return "" + a[0];
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            buf.append("[");
            buf.append(i);
            buf.append("]: ");
            buf.append(a[i]);
            buf.append(" ");
        }
        return buf.toString();
    }

    /**
     * Covert a long array to a double array
     *
     * @param from    array of longs
     * @return  array of doubles
     */
    private double[] toDouble(long[] from) {
        double[] to = new double[from.length];
        for (int i = 0; i < from.length; i++) {
            to[i] = (double) from[i];
        }
        return to;
    }

    /**
     * Covert a float array to a double array
     *
     * @param from    array of floats
     * @return  array of doubles
     */
    private double[] toDouble(float[] from) {
        double[] to = new double[from.length];
        for (int i = 0; i < from.length; i++) {
            to[i] = (double) from[i];
        }
        return to;
    }

    /** hashtable of keys */
    private Hashtable keys = new Hashtable();

    /**
     * Get a geokey from the table as an int
     *
     * @param key   key to look up
     * @param dflt  default value
     * @return  key value or dflt
     */
    private int getGeoKey(int key, int dflt) {
        double[] v = (double[]) keys.get(new Integer(key));
        if (v == null) {
            return dflt;
        }
        return (int) v[0];
    }

    /**
     * Get a geokey from the table as a double array
     *
     * @param key   key to look up
     * @param dflt  default value
     * @return  key value or dflt
     */
    private double[] getGeoKey(int key, double[] dflt) {
        double[] v = (double[]) keys.get(new Integer(key));
        if (v == null) {
            return dflt;
        }
        return v;
    }

    /**
     * Get the model type
     * @return  model type
     */
    private int getModelType() {
        return getGeoKey(GeoKeys.Geokey.GTModelTypeGeoKey,
                         GeoKeys.ModelType.Projected);
    }

    /**
     * Get the projected CoordinateSystem type
     * @return  the projected CoordinateSystem type
     */
    private int getProjectedCSType() {
        return getGeoKey(GeoKeys.Geokey.ProjectedCSTypeGeoKey, NOVALUE);
    }


    /**
     * Process a key and put it in the keys table
     *
     * @param geoKeyId    key id
     * @param value       value
     */
    private void processKey(int geoKeyId, double[] value) {
        keys.put(new Integer(geoKeyId), value);
    }



    /**
     * Get the MapProjection for this GeoTIFF
     * @return  MapProjection
     *
     * @throws VisADException   couldn't create VisAD object
     * @throws IOException      couldn't read data from file
     */
    private MapProjection getMapProjection()
            throws VisADException, IOException {
        MapProjection projection = null;
        if ( !isGeotiff) {
            return null;
        }

        //        printKeys();

        switch (getModelType()) {

          case GeoKeys.ModelType.Projected : {
              //Lets not do this now
              //              projection = getProjectedMapProjection();
              break;
          }

          case GeoKeys.ModelType.Geographic : {
              projection = getGeographicMapProjection();
              break;
          }

          case GeoKeys.ModelType.UserDefined : {
              break;
          }
        }
        return projection;
    }


    /**
     * Get the projected MapProjection
     *
     * @return  the projected MapProjection
     */
    private MapProjection getProjectedMapProjection() {
        if (tiePointField == null) {
            throw new IllegalStateException("No tie point field found");
        }
        if (scaleField == null) {
            throw new IllegalStateException("No scale field found");
        }


        int projectedCSType = getProjectedCSType();
        if (projectedCSType == NOVALUE) {
            System.err.println("No CS type given in keys");
            printKeys();
            return null;
        }
        printKeys();
        Pcs pcs = Pcs.findCoordRefSysCode(projectedCSType);
        if (pcs == null) {
            if (projectedCSType == USER_DEFINED) {
                return createUserDefined();
            } else {
                System.err.println("Unable to find projected CS type = "
                                   + projectedCSType);
                return null;
            }
        }
        initParamCodeMap(pcs);
        //        System.err.println ("pcs:" + pcs);

        CoordinateOperationMethod com = pcs.findCoordinateOperationMethod();
        if (com == null) {
            System.err.println(
                "Unable to find coordinate operation method = "
                + pcs.getCoordOpMethodCode());
            return null;
        }
        System.err.println("com:" + com);
        if (com.getCoordOpMethodName().toLowerCase().equals(
                PROJ_TRANSVERSEMERCATOR)) {
            return createFromTransverseMercator(pcs);
        }

        //        double lat = pcs.getParamValue1 ();
        //        double lon = pcs.getParamValue2 ();


        return null;

    }

    /**
     * Create user defined map projection
     *
     * @return  null
     */
    private MapProjection createUserDefined() {
        return null;

    }


    /**
     *  Create a MapProjection from the given bounding box. The points array is
     *  a 4x2 array containing the lat/lon points to the (respectively)
     *  northwest, northeast, southeast, and southwest points.
     *
     * @param points
     * @return   MapProjection from the box
     */

    private MapProjection createProjectionFromBox(double[][] points) {
        return null;
    }

    /**
     *  Retrieve the 3xN array of parameter code/values/uom
     *
     * @param pcs   parameter codes
     */
    private void initParamCodeMap(Pcs pcs) {
        int num;
        for (num = 1; true; num++) {
            try {
                int    code  = pcs.findIntByName("parameterCode" + num);
                int    uom   = pcs.findIntByName("parameterUom" + num);
                double value = pcs.findDoubleByName("parameterValue" + num);
                paramCodeMap.put(new Integer(code), new double[] { value,
                        (double) uom });
            } catch (Exception exc) {
                break;
            }

        }
    }

    /**
     * Check if we have the particular parameter code
     *
     * @param code   code to check
     * @return  true if we have it
     */
    private boolean hasParamCode(int code) {
        double[] valueAndUom = (double[]) paramCodeMap.get(new Integer(code));
        return (valueAndUom != null);
    }

    /**
     * Get the parameter code value
     *
     * @param code   parameter code
     * @param dflt   default value
     * @return  value for code or dflt
     */
    private double getParamCodeValue(int code, double dflt) {
        double[] valueAndUom = (double[]) paramCodeMap.get(new Integer(code));
        if (valueAndUom != null) {
            return valueAndUom[0];
        }
        return dflt;
    }


    /** static conversion factor */
    private static final double FT_TO_METERS = 0.3048;

    /**
     * Convert feet to meters
     *
     * @param v   value in feet
     * @return  value in meters
     */
    private double ftToM(double v) {
        return FT_TO_METERS * v;
    }

    /**
     * Convert feet to kilometers
     *
     * @param v  value in feet
     * @return  value in km
     */
    private double ftToKm(double v) {
        return ftToM(v) / 1000.0;
    }

    /**
     * Create a MapProjection from a parameter code
     *
     * @param pcs   parameter code
     * @return  corresponding MapPRojection
     */
    private MapProjection createFromTransverseMercator(Pcs pcs) {
        double lat0       = 40.0;
        double tangentLon = -80.0;

        double scale      = scaleField.getAsDouble(0);

        if ( !hasParamCode(COM.LATITUDE_OF_NATURAL_ORIGIN)) {
            throw new IllegalStateException(
                "No LATITUDE_OF_NATURAL_ORIGIN found in geotiff");
        }

        if ( !hasParamCode(COM.LONGITUDE_OF_NATURAL_ORIGIN)) {
            throw new IllegalStateException(
                "No LONGITUDE_OF_NATURAL_ORIGIN found in geotiff");
        }

        double natLat = getParamCodeValue(COM.LATITUDE_OF_NATURAL_ORIGIN,
                                          0.0);
        double natLon = getParamCodeValue(COM.LONGITUDE_OF_NATURAL_ORIGIN,
                                          0.0);
        double naturalOriginScale =
            getParamCodeValue(COM.SCALE_FACTOR_AT_NATURAL_ORIGIN, 1.0);
        double falseNorthing = getParamCodeValue(COM.FALSE_NORTHING, 0.0);
        double falseEasting  = getParamCodeValue(COM.FALSE_EASTING, 0.0);

        int    indexX        = (int) tiePointField.getAsDouble(0);
        int    indexY        = (int) tiePointField.getAsDouble(1);

        double scaleX        = scaleField.getAsDouble(0);
        double scaleY        = scaleField.getAsDouble(1);

        int    x             = (int) tiePointField.getAsDouble(0);
        int    y             = (int) tiePointField.getAsDouble(1);

        double p1x           = toDegrees(tiePointField.getAsDouble(3));
        double p1y           = toDegrees(tiePointField.getAsDouble(4));
        //TODO - make sure that when x/y are not 0 then this gets the correct lat1/lon1
        if (x != 0) {
            p1x = p1x + (-x) * scaleX;
        }
        if (y != 0) {
            p1y = p1y + (-y) * scaleY;
        }

        double p2x = p1x + (cols - 1) * scaleX;
        double p2y = p1y - (rows - 1) * scaleY;


        p1x    -= falseEasting;
        p2x    -= falseEasting;
        p1y    -= falseNorthing;
        p2y    -= falseNorthing;
        natLat = 36.666667;
        natLon = -88.333333;

        System.err.println("TransverseMercator:\nnatLat/lon: " + natLat + " "
                           + natLon + "\nnorthing/easting: " + falseNorthing
                           + "  " + falseEasting + "\nimage: (" + p1x + ","
                           + p1y + ") (" + p2x + "," + p2y + ")");


        System.err.println("pcs=" + pcs);
        TransverseMercator tm = new TransverseMercator(natLat, natLon,
                                    naturalOriginScale);
        double[][] from = {
            { ftToKm(p1x), ftToKm(p2x), ftToKm(p2x), ftToKm(p1x) },
            { ftToKm(p1y), ftToKm(p1y), ftToKm(p2y), ftToKm(p2y) }
        };


        double[][] to = tm.projToLatLon(from, new double[2][4]);


        System.err.println("from:");
        for (int i = 0; i < from.length; i++) {
            Misc.printArray("", from[i]);
        }


        System.err.println("to:");
        for (int i = 0; i < to.length; i++) {
            Misc.printArray("", to[i]);
        }


        return createProjectionFromBox(to);
    }

    /**
     * Get the geographic MapProjection
     *
     * @return  the MapProjection
     *
     * @throws IOException      unable to access file
     * @throws VisADException   unable to create VisAD object
     */
    private MapProjection getGeographicMapProjection()
            throws VisADException, IOException {
        if (tiePointField == null) {
            throw new IllegalStateException("No tie point field found");
        }
        if (scaleField == null) {
            throw new IllegalStateException("No scale field found");
        }

        double scaleX = scaleField.getAsDouble(0);
        double scaleY = scaleField.getAsDouble(1);
        int    x      = (int) tiePointField.getAsDouble(0);
        int    y      = (int) tiePointField.getAsDouble(1);

        double lat1   = toDegrees(tiePointField.getAsDouble(4));
        double lon1   = toDegrees(tiePointField.getAsDouble(3));
        //TODO - make sure that when x/y are not 0 then this gets the correct lat1/lon1
        if (y != 0) {
            lat1 = lat1 + (-y) * scaleY;
        }
        if (x != 0) {
            lat1 = lat1 + (-x) * scaleX;
        }
        double lat2 = lat1 - (rows - 1) * scaleY;
        double lon2 = lon1 + (cols - 1) * scaleX;


        //        System.err.println ("coord info:" + cols + "x" + rows +" lat: (" + lat1  + " - " + lat2 + ") lon: (" + lon1 +" - " + lon2 +")  Di:" + scaleX +" Dj:" + scaleY );
        return new GRIBCoordinateSystem(0, cols, rows, lat2, lon1, lat1,
                                        lon2, scaleX, scaleY);
    }



    /**
     * Print a field
     *
     * @param name   name of field
     * @param field  field to print
     * @param sb _more_
     */
    public static void print(String name, TIFFField field, StringBuffer sb) {
        if (field == null) {
            sb.append(name + ": null field\n");
            return;
        }
        //        System.err.print (name + ": " + GeoKeys.Tiff.getFieldType (field.getType ()) + "  values=");
        sb.append(name + "=");
        int type = field.getType();
        for (int i = 0; i < field.getCount(); i++) {
            switch (type) {

              case TIFFField.TIFF_DOUBLE :
                  double v = field.getAsDouble(i);
                  sb.append(((i == 0)
                             ? ""
                             : ",") + v);
                  break;

              case TIFFField.TIFF_ASCII :
                  //                System.err.println (field.getAsString (i));
                  break;

            }
        }
        sb.append("");

    }


    /**
     * Print the keys for this GeoTIFF
     */
    public void printKeys() {
        System.err.println(getKeyString());
    }



    /**
     * Print the keys for this GeoTIFF
     *
     * @return _more_
     */
    public String getKeyString() {
        if (dirField == null) {
            return "Not a geotiff";
        }

        StringBuffer sb = new StringBuffer();

        print("\ttie point", tiePointField, sb);
        sb.append("\n");
        print("\tscale", scaleField, sb);
        sb.append("\n");
        //        System.err.println ("");
        for (int tagIdx = 4; tagIdx < dirField.getCount(); tagIdx += 4) {
            long   geoKeyId = dirField.getAsLong(tagIdx);
            long   location = dirField.getAsLong(tagIdx + 1);
            long   count    = dirField.getAsLong(tagIdx + 2);
            long   offset   = dirField.getAsLong(tagIdx + 3);
            String keyName  = GeoKeys.Geokey.getName((int) geoKeyId);
            long   value    = 0;
            if (location == 0) {
                sb.append("\tgeokey: " + keyName + "(" + geoKeyId + ")  = "
                          + offset);
                value = offset;
            } else {
                TIFFField subField = dir.getField((int) location);
                if (subField == null) {
                    sb.append("\tgeokey: " + keyName + "(" + geoKeyId
                              + ")  could not be found\n");
                    continue;
                }
                String typeName =
                    GeoKeys.Tiff.getFieldType(subField.getType());
                //                    System.err.print ("\tgeokey: " + keyName +"("+geoKeyId + ")  type=" + typeName +" location  = " + location + " count=" + count);

                if (count == 1) {
                    if (subField.getType() == TIFFField.TIFF_ASCII) {}
                    else {
                        sb.append("\tgeokey: " + keyName + "(" + geoKeyId
                                  + ")  = "
                                  + subField.getAsDouble((int) offset));
                    }
                } else {
                    print("\tgeokey: " + keyName + "(" + geoKeyId
                          + ") offset: " + offset, subField, sb);
                }
                //                System.err.print ("\tgeokey: " + keyName +"("+geoKeyId + ")  count=" + count);
                //                print (keyName, subField);

            }
            if (geoKeyId == GeoKeys.Geokey.ProjectedCSTypeGeoKey) {
                sb.append("  projectedCS:"
                          + GeoKeys.EpsgPcs.getName((int) value) + "\n");
            } else if (geoKeyId == GeoKeys.Geokey.GeographicTypeGeoKey) {
                sb.append(" geographic type:"
                          + GeoKeys.EpsgGcs.getName((int) value) + "\n");
            } else {
                sb.append("\n");
            }
        }
        return sb.toString();
    }





    /**
     * Test routine
     *
     * @param args   space separated filenames
     */
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.err.print("\nFile: " + args[i] + "  ");
            try {
                new GeotiffAdapter(args[i]).printKeys();
                //new GeotiffAdapter(args[i]).getData();

            } catch (Exception exc) {
                System.err.println("oops:" + args[i]);
                exc.printStackTrace();
                //                return;
            }
        }
    }

}

