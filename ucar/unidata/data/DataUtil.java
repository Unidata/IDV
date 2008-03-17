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


import org.apache.poi.hssf.usermodel.*;


import ucar.ma2.Array;
import ucar.ma2.Index;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.quantities.AirPressure;


import visad.*;

import visad.data.vis5d.Vis5DVerticalSystem;

import visad.georef.*;

import java.io.*;

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
            } else {
                throw new IllegalArgumentException("Unknown array type:" + fromClass.getName());
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


    /**
     * Make a float array of the sizes indicated with the value
     *
     * @param rows  number of arrays
     * @param cols  number of values in each row
     * @param value fill value
     *
     * @return  the filled in array
     */
    public static float[][] makeFloatArray(int rows, int cols, float value) {
        float[][] values = new float[rows][cols];
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                values[i][j] = value;
            }
        }
        return values;
    }

    /**
     * This method find the flat field somewhere in the given data
     *
     * @param field field
     *
     * @return a flat field or null
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public static FlatField getFlatField(Data field)
            throws VisADException, RemoteException {
        if (field == null) {
            return null;
        }
        if (field instanceof FlatField) {
            return (FlatField) field;
        }
        if (field instanceof Tuple) {
            Tuple t = (Tuple) field;
            for (int i = 0; i < t.getLength(); i++) {
                FlatField f = getFlatField(t.getComponent(i));
                if (f != null) {
                    return f;
                }
            }
        }

        if ( !(field instanceof FieldImpl)) {
            return null;
        }
        return getFlatField(((FieldImpl) field).getSample(0, false));
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @param rows _more_
     */
    public static void writeCsv(String filename, List rows) {
        try {
            if (filename.toLowerCase().endsWith(".xls")) {
                writeXls(filename, rows);
                return;
            }
            StringBuffer sb = new StringBuffer();
            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                sb.append(StringUtil.join(",", (List) rows.get(rowIdx)));
                sb.append("\n");
            }
            IOUtil.writeFile(filename, sb.toString());
        } catch (Exception exc) {
            LogUtil.logException("Writing CSV", exc);
        }
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @param rows _more_
     *
     * @throws Exception _more_
     */
    public static void writeXls(String filename, List rows) throws Exception {
        HSSFWorkbook     wb      = new HSSFWorkbook();
        FileOutputStream fileOut = new FileOutputStream(filename);
        HSSFSheet        sheet   = wb.createSheet();
        for (int i = 0; i < rows.size(); i++) {
            HSSFRow row  = sheet.createRow((short) i);
            List    cols = (List) rows.get(i);
            for (int colIdx = 0; colIdx < cols.size(); colIdx++) {
                Object   o    = cols.get(colIdx);
                HSSFCell cell = row.createCell((short) colIdx);
                if (o instanceof Double) {
                    cell.setCellValue(((Double) o).doubleValue());
                } else if (o instanceof Real) {
                    cell.setCellValue(((Real) o).getValue());
                } else if (o instanceof Integer) {
                    cell.setCellValue(((Integer) o).intValue());
                } else {
                    cell.setCellValue(o.toString());
                }
            }
        }
        wb.write(fileOut);
        fileOut.close();
    }



    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String xlsToCsv(String filename) throws Exception {
        StringBuffer sb    = new StringBuffer();
        InputStream  myxls = IOUtil.getInputStream(filename, DataUtil.class);
        HSSFWorkbook wb    = new HSSFWorkbook(myxls);
        HSSFSheet    sheet = wb.getSheetAt(0);  // first sheet
        for (int rowIdx = sheet.getFirstRowNum();
                rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            HSSFRow row = sheet.getRow(rowIdx);
            for (short colIdx = row.getFirstCellNum();
                    colIdx < row.getPhysicalNumberOfCells(); colIdx++) {
                HSSFCell cell = row.getCell(colIdx);
                if (cell == null) {
                    continue;
                }
                if (colIdx > 0) {
                    sb.append(",");
                }
                sb.append(cell.toString());
                /*                if(false && comment!=null) {
                    String author = comment.getAuthor();
                    String str = comment.getString().getString();
                    str = StringUtil.replace(str, author+":","");
                    str = StringUtil.replace(str, "\n","");
                    sb.append("("+str+")");
                    }*/
            }
            sb.append("\n");
        }
        return sb.toString();
    }



}

