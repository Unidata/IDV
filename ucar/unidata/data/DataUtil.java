/*
 * $Id: DataUtil.java,v 1.8 2006/12/01 20:41:22 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;


import ucar.ma2.Array;
import ucar.ma2.Index;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.quantities.AirPressure;


import visad.*;

import visad.data.vis5d.Vis5DVerticalSystem;

import visad.georef.*;

import java.lang.reflect.*;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * A class to hold general utility functions for manipulating data
 *
 * @author IDV development team
 * @version $Revision: 1.8 $
 */
public class DataUtil {

    /** flag for standard atmosphere coordinate system class */
    public static final String STD_ATMOSPHERE =
        "ucar.visad.quantities.AirPressure$StandardAtmosphereCoordinateSystem";

    /** flag for Vis5D pressure to height coordinate system class */
    public static final String VIS5D_VERTICALCS =
        "visad.data.vis5d.Vis5DVerticalSystem$Vis5DVerticalCoordinateSystem";

    /**
     * Get the 1D values for an array as floats.
     *
     * @param arr   Array of values
     * @return  float representation
     */
    public static float[] toFloatArray(Array arr) {
        Object dst       = arr.get1DJavaArray(float.class);
        Class  fromClass = dst.getClass().getComponentType();
        if (fromClass.equals(float.class)) {
            //It should always be a float
            return (float[]) dst;
        } else {
            float[] values = new float[(int) arr.getSize()];
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray = (double[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (float) fromArray[i];
                }
            }
            return values;
        }

    }



    /**
     * Get the 1D values for an array as doubles.
     *
     * @param arr   Array of values
     * @return  double representation
     */
    public static double[] toDoubleArray(Array arr) {
        Object dst = arr.get1DJavaArray(double.class);
        //        Object dst = arr.copyTo1DJavaArray();
        Class fromClass = dst.getClass().getComponentType();

        if (fromClass.equals(double.class)) {
            //It should always be a double
            return (double[]) dst;
        } else {
            double[] values = new double[(int) arr.getSize()];
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray = (double[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (double) fromArray[i];
                }
            }
            return values;
        }

    }


    /**
     * Get the 1D values for an array as Strings.
     *
     * @param arr   Array of values
     * @return  String representation
     */
    public static String[] toStringArray(Array arr) {
        return (String[]) arr.get1DJavaArray(String.class);
    }

    /**
     * Get the 1D values for an array as chars.
     *
     * @param arr   Array of values
     * @return  chars representation
     */
    public static char[] toCharArray(Array arr) {
        Object dst = arr.get1DJavaArray(char.class);
        //        Object dst = arr.copyTo1DJavaArray();
        Class fromClass = dst.getClass().getComponentType();

        if (fromClass.equals(char.class)) {
            //It should always be a char
            return (char[]) dst;
        } else {
            char[] values = new char[(int) arr.getSize()];
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray = (double[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            }
            return values;
        }

    }

    /**
     * Tries to parse the string. If an exception occurs just return null.
     *
     * @param unitString The unit string
     *
     * @return The parsed unit or null if an error occurs
     */
    public static Unit parseUnit(String unitString) {
        try {
            return Util.parseUnit(unitString);
        } catch (Exception exc) {
            return null;
        }
    }


    /**
     * Try to create a RealType from the name and unit.
     * @param name name of type
     * @param unit can be null
     * @return RealType or null
     */
    public static RealType makeRealType(String name, Unit unit) {
        /* old way
        RealType type = null;
        if (unit == null) {
            type = RealType.getRealType(cleanName(name));
        } else {
            type = RealType.getRealType(cleanName(name), unit);
        }
        return type;
        */
        try {
            return Util.makeRealType(name, unit);
        } catch (Exception exc) {
            System.err.println(exc.toString());
            return null;
        }
    }

    /**
     * Make a valid VisAD RealType name from the string.  Remove
     * spaces, "." and parens.
     * @param name name to clean
     * @return cleaned up name
     */
    public static String cleanName(String name) {
        return Util.cleanName(name);
    }

    /**
     * Get the appropriate vertical coordinate system
     * @param type  type of pressure to height cs
     *
     * @return the corresponding CS
     *
     * @throws VisADException couldn't create the CS
     */
    public static CoordinateSystem getPressureToHeightCS(String type)
            throws VisADException {
        CoordinateSystem cs = null;
        try {
            Class c = Class.forName(type);
            cs = (CoordinateSystem) c.newInstance();
        } catch (Exception excp) {
            throw new VisADException("Unable to create vertical transform "
                                     + type);
        }
        return cs;
    }

}

