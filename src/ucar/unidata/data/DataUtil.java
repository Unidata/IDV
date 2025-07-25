/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import ucar.ma2.Array;

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.CoordInterval;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.TimeOffsetAxis;
import ucar.nc2.time.CalendarDate;

import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.NamedAnything;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.ProbeControl;
import ucar.unidata.idv.control.ProbeRowInfo;
import ucar.unidata.util.*;

import ucar.visad.Util;
import ucar.visad.data.CalendarDateTime;

import ucar.visad.display.Animation;
import visad.*;


import java.io.FileOutputStream;
import java.io.InputStream;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


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
        Class fromClass = arr.getElementType();
        if (fromClass.equals(float.class)) {
            // It should always be a float
            return (float[]) arr.get1DJavaArray(float.class);
        } else {
            float[] values     = new float[(int) arr.getSize()];
            boolean isUnsigned = arr.isUnsigned();
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) arr.get1DJavaArray(byte.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (int) fromArray[i] & 0xFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) arr.get1DJavaArray(short.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (int) fromArray[i] & 0xFFFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) arr.get1DJavaArray(int.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (long) fromArray[i] & 0xFFFFFFFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray =
                    (double[]) arr.get1DJavaArray(double.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (float) fromArray[i];
                }
            } else {
                throw new IllegalArgumentException("Unknown array type:"
                        + fromClass.getName());
            }
            return values;
        }

    }

    /**
     * Get the 1D values for an array as floats.
     *
     * @param arr   Array of values
     * @return  float representation
     */
    public static float[] toFloatArray(Array[] arr){
        float [] values0 = toFloatArray(arr[0]);
        if(arr.length == 1)
            return values0;
        else {
            int size0 = values0.length;
            int size = size0 * arr.length;
            float [] values = new float[size];
            System.arraycopy(values0, 0, values, 0, size0);
            for(int i = 1; i < arr.length; i++) {
                float [] valuesi = toFloatArray(arr[i]);
                System.arraycopy(valuesi, 0, values, i *size0, size0 );
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
        Class fromClass = arr.getElementType();
        if (fromClass.equals(double.class)) {
            // It should always be a double
            return (double[]) arr.get1DJavaArray(double.class);
        } else {
            double[] values     = new double[(int) arr.getSize()];
            boolean  isUnsigned = arr.isUnsigned();
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) arr.get1DJavaArray(byte.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (int) fromArray[i] & 0xFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) arr.get1DJavaArray(short.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (int) fromArray[i] & 0xFFFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) arr.get1DJavaArray(int.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (long) fromArray[i] & 0xFFFFFFFF;
                    } else {
                        values[i] = fromArray[i];
                    }
                }
            } else if (fromClass.equals(float.class)) {
                float[] fromArray = (float[]) arr.get1DJavaArray(float.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
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
        Class fromClass = arr.getElementType();

        if (fromClass.equals(char.class)) {
            // It should always be a char
            return (char[]) arr.get1DJavaArray(char.class);
        } else {
            char[]  values     = new char[(int) arr.getSize()];
            boolean isUnsigned = arr.isUnsigned();
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) arr.get1DJavaArray(byte.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    if (isUnsigned) {
                        values[i] = (char) ((int) fromArray[i] & 0xFF);
                    } else {
                        values[i] = (char) fromArray[i];
                    }
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) arr.get1DJavaArray(short.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) arr.get1DJavaArray(int.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(float.class)) {
                float[] fromArray = (float[]) arr.get1DJavaArray(float.class);
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (char) fromArray[i];
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray =
                    (double[]) arr.get1DJavaArray(double.class);
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
     * Try to create a TextType from the name.
     * @param name name of type
     * @return TextType or null
     */
    public static TextType makeTextType(String name) {
        return TextType.getTextType(Util.cleanName(name));
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
            java.util.Arrays.fill(values[i], value);
        }
        return values;
    }

    /**
     * clone the array
     *
     * @param a incoming array
     *
     * @return cloned array
     */
    public static float[][] cloneArray(float[][] a) {
        float[][] values = new float[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, values[i], 0, a[0].length);
        }
        return values;
    }

    /**
     * copy the originalValues to the new values. Set all values to be positive
     *
     * @param originalValues original values
     * @param newValues new values
     * @param indexArray The indices to change
     */
    public static void absoluteValue(float[][] originalValues,
                                     float[][] newValues, int[] indexArray) {
        for (int j = 0; j < indexArray.length; j++) {
            if (originalValues[0][indexArray[j]] < 0) {
                newValues[0][indexArray[j]] =
                    -originalValues[0][indexArray[j]];
            }
        }
    }


    /**
     * Copy the values in originalValues[indexArray] that are greater than the given value to newValues
     *
     * @param originalValues original values
     * @param newValues new Values
     * @param indexArray indices
     * @param value the threshold value
     */
    public static void max(float[][] originalValues, float[][] newValues,
                           int[] indexArray, float value) {
        for (int j = 0; j < indexArray.length; j++) {
            if (originalValues[0][indexArray[j]] > value) {
                newValues[0][indexArray[j]] = value;
            }
        }
    }


    /**
     * set the values in the indices in newValues = the given value
     *
     * @param originalValues originalValues
     * @param newValues newValues
     * @param indexArray indices
     * @param value value
     */
    public static void setValue(float[][] originalValues,
                                float[][] newValues, int[] indexArray,
                                float value) {
        for (int j = 0; j < indexArray.length; j++) {
            newValues[0][indexArray[j]] = value;
        }
    }

    /**
     * If the originalValues < given value then set the newValues = value
     *
     * @param originalValues  originalValues
     * @param newValues  newValues
     * @param indexArray indices
     * @param value value
     */
    public static void min(float[][] originalValues, float[][] newValues,
                           int[] indexArray, float value) {
        for (int j = 0; j < indexArray.length; j++) {
            if (originalValues[0][indexArray[j]] < value) {
                newValues[0][indexArray[j]] = value;
            }
        }
    }


    /**
     * set newValues to be the average of the original values in the index array
     *
     * @param originalValues original values
     * @param newValues new value
     * @param indexArray indices
     */
    public static void average(float[][] originalValues, float[][] newValues,
                               int[] indexArray) {
        if (indexArray.length == 0) {
            return;
        }
        float total = 0;

        for (int j = 0; j < indexArray.length; j++) {
            total += originalValues[0][indexArray[j]];
        }
        for (int j = 0; j < indexArray.length; j++) {
            newValues[0][indexArray[j]] = total / indexArray.length;
        }
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
     * Write a csv file for a time series point data
     *
     * @param filename file to write to
     * @param field data
     */
    public static void exportCsvAllTimes(FieldImpl field, String filename) {
        try {
            if(field.getSample(0) instanceof FlatField){
                GridUtil.writeGridToCsv(field, filename);
            } else{
                List rows = fieldToRows(field);
                writeCsv(filename, rows);
            }
        } catch (Exception exc) {
            LogUtil.logException("Exporting to csv", exc);
        }
    }

    /**
     * output a list for a time series point data
     *
     * @param field data
     */
    public static List fieldToRows(FieldImpl field) {
        List rows = new ArrayList();
        try {
            Set timeSet = field.getDomainSet();
            int numTimes = timeSet.getLength();
            DateTime[] times   = Animation.getDateTimeArray(timeSet);
            //Real[]    times     = Animation.getDateTimeArray(aniSet);
            List cols;
            cols = Misc.newList("Time");
            MathType type = field.getType();
            String name = ((FunctionType) type).getRange().toString();
            if(name != null)
                cols.add(name);
            else
                cols.add("name");

            rows.add(cols);

            for (int timeIdx = 0; timeIdx < times.length; timeIdx++) {
                Real aniValue = times[timeIdx];
                cols = Misc.newList("" + aniValue);
                Data data = field.getSample(timeIdx);
                //rows.add(cols);
                Real real = null;
                if (data instanceof Real) {
                    real = (Real) data;
                } else if (data instanceof RealTuple) {
                    real = (Real) ((RealTuple) data).getComponent(0);
                } else {
                    real =new Real(0);
                }
                cols.add(real.getValue());
                rows.add(cols);
            }

        } catch (Exception exc) {
            LogUtil.logException("Exporting to csv", exc);
        }
        return rows;
    }

    /**
     * Write a csv file. The rows list contains lists. We take the toString value of each list element
     *
     * @param filename file to write to
     * @param rows data
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
     * Write to an excel spreadsheet
     *
     * @param filename file
     * @param rows data
     *
     * @throws Exception On badness
     */
    public static void writeXls(String filename, List rows) throws Exception {
        writeXls(filename, Misc.newList(rows), null);
    }

    /**
     * Write to an excel spreadsheet
     *
     * @param filename file
     * @param rowsList data
     * @param names sheet names
     *
     * @throws Exception On badness
     */
    public static void writeXls(String filename, List<List> rowsList,
                                List<String> names)
            throws Exception {
        HSSFWorkbook     wb      = new HSSFWorkbook();
        FileOutputStream fileOut = new FileOutputStream(filename);
        for (int sheetIdx = 0; sheetIdx < rowsList.size(); sheetIdx++) {
            String    sheetName = ((names != null)
                                   ? names.get(sheetIdx)
                                   : null);
            HSSFSheet sheet     = ((sheetName != null)
                                   ? wb.createSheet(sheetName)
                                   : wb.createSheet());
            List      rows      = rowsList.get(sheetIdx);
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
        }
        wb.write(fileOut);
        fileOut.close();
    }



    /**
     * Convert excel to csv
     *
     * @param filename excel file
     *
     * @return csv
     *
     * @throws Exception On badness
     */
    public static String xlsToCsv(String filename) throws Exception {
        return xlsToCsv(filename, false, null);
    }


    /**
     * Convert excel to csv
     *
     * @param filename excel file
     * @param skipToFirstNumeric if true, skip to first numeric
     * @param sdf If non null then use this to format any date cells
     *
     * @return csv
     *
     * @throws Exception On badness
     */
    public static String xlsToCsv(String filename,
                                  boolean skipToFirstNumeric,
                                  SimpleDateFormat sdf)
            throws Exception {
        StringBuffer sb         = new StringBuffer();
        InputStream  myxls = IOUtil.getInputStream(filename, DataUtil.class);
        HSSFWorkbook wb         = new HSSFWorkbook(myxls);
        HSSFSheet    sheet      = wb.getSheetAt(0);  // first sheet
        boolean      seenNumber = false;
        for (int rowIdx = sheet.getFirstRowNum();
                rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            HSSFRow row = sheet.getRow(rowIdx);
            if (row == null) {
                sb.append("\n");
                continue;
            }
            boolean rowOk = true;
            for (short colIdx = row.getFirstCellNum();
                    colIdx < row.getPhysicalNumberOfCells(); colIdx++) {
                HSSFCell cell = row.getCell(colIdx);
                if (cell == null) {
                    continue;
                }
                if (skipToFirstNumeric && !seenNumber) {
                    if (cell.getCellType() != HSSFCell.CELL_TYPE_NUMERIC) {
                        rowOk = false;
                        break;
                    }
                    seenNumber = true;
                }

                String cellValue = null;

                if ((sdf != null)
                        && (cell.getCellType()
                            == HSSFCell.CELL_TYPE_NUMERIC)) {
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        cellValue = sdf.format(date);
                    }
                }

                if (cellValue == null) {
                    cellValue = cell.toString();
                }

                if (colIdx > 0) {
                    sb.append(",");
                }
                sb.append(cellValue);
                /*                if(false && comment!=null) {
                    String author = comment.getAuthor();
                    String str = comment.getString().getString();
                    str = StringUtil.replace(str, author+":","");
                    str = StringUtil.replace(str, "\n","");
                    sb.append("("+str+")");
                    }*/
            }
            if (rowOk) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }



    /**
     * Determine if the given point is inside the given polygon
     *
     * Code originally taken from: http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     *
     *
     *     Copyright (c) 1970-2003, Wm. Randolph Franklin
     * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
     *     1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimers.
     *     2. Redistributions in binary form must reproduce the above copyright notice in the documentation and/or other materials provided with the distribution.
     *     3. The name of W. Randolph Franklin may not be used to endorse or promote products derived from this Software without specific prior written permission.
     *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
     *
     *
     * @param pts the xy outline points
     * @param x  x value
     * @param y  y value
     *
     * @return true if inside
     */
    public static boolean pointInside(float[][] pts, float x, float y) {
        int     i, j;
        boolean isPointInside = false;
        int     numPoints     = pts[0].length;
        float[] vertx         = pts[0];
        float[] verty         = pts[1];
        for (i = 0, j = numPoints - 1; i < numPoints; j = i++) {
            if (((verty[i] > y) != (verty[j] > y))
                    && (x < (vertx[j] - vertx[i]) * (y - verty[i])
                        / (verty[j] - verty[i]) + vertx[i])) {
                isPointInside = !isPointInside;
            }
        }
        return isPointInside;
    }

    /**
     * Select the dates from sourceTimes that most closely match
     * the selectionTimes;
     *
     * @param sourceTimes set of available times
     * @param driverTimes     times we want to match
     *
     * @return the matching times as Dates
     * @throws Exception  problem creating times
     */
    public static List<Date> selectDatesFromList(List sourceTimes,
            List<DateTime> driverTimes)
            throws Exception {
        List<DateTime> dts   = selectTimesFromList(sourceTimes, driverTimes);
        List<Date>     dates = new ArrayList<Date>();
        for (DateTime dt : dts) {
            dates.add(new Date((long) dt.getValue() * 1000));
        }
        return dates;
    }


    /**
     * Select the times from sourceTimes that most closely match
     * the selectionTimes;
     *
     * @param sourceTimes set of available times
     * @param selectionTimes  times we want to match
     *
     * @return the matching times
     *
     * @throws Exception  problem creating times
     */
    public static List<DateTime> selectTimesFromList(List sourceTimes,
            List<DateTime> selectionTimes)
            throws Exception {
        List<DateTime> results = new ArrayList<DateTime>();
        //First convert the source times to a list of Date objects
        List<Date> sourceDates = new ArrayList<Date>();
        for (int i = 0; i < sourceTimes.size(); i++) {
            Object object = sourceTimes.get(i);
            if (object instanceof DateTime) {
                sourceDates.add(ucar.visad.Util.makeDate((DateTime) object));
            } else if (object instanceof Date) {
                sourceDates.add((Date) object);
            } else if (object instanceof DatedObject) {
                sourceDates.add(((DatedObject) object).getDate());
            } else if (object instanceof TwoFacedObject) {  //relative time
                return null;
            } else {
                System.err.println("Unknown time type: "
                                   + object.getClass().getName());
                return null;
            }
        }
        //This keeps track of what times in the source list we have used so far
        HashSet seenTimes = new HashSet();

        //Now look at each selection time and find the closest source time
        //We need to have logic for when a selection time is outside the range of the source times
        for (DateTime dateTime : selectionTimes) {
            Date dttm        = ucar.visad.Util.makeDate(dateTime);
            long minTimeDiff = -1;
            Date minDate     = null;
            for (int i = 0; i < sourceDates.size(); i++) {
                Date sourceDate = sourceDates.get(i);
                long timeDiff = Math.abs(sourceDate.getTime()
                                         - dttm.getTime());
                if ((minTimeDiff < 0) || (timeDiff < minTimeDiff)) {
                    minTimeDiff = timeDiff;
                    minDate     = sourceDate;
                }
            }
            if ((minDate != null) && !seenTimes.contains(minDate)) {
                results.add(new DateTime(minDate));
                seenTimes.add(minDate);
            }
        }
        return results;
    }

    /**
     * Make a CalendarDateTime object corresponding the the calendar date
     *
     * @param cdate   the CalendarDate
     * @return the corresponding DateTime
     * @throws VisADException  problem parsing cdate
     */
    public static CalendarDateTime makeDateTime(CalendarDate cdate)
            throws VisADException {
        CalendarDateTime d = new CalendarDateTime(cdate);
        return d;
    }

    /**
     * Make a List of CalendarDateTime objects from the times in the timeAxis
     *
     * @param timeAxis  the time axis
     * @return List of DateTimes
     * @throws VisADException  problem parsing timeAxis calendar dates
     */
    public static List<CalendarDateTime> makeDateTimes(
            CoordinateAxis1DTime timeAxis)
            throws VisADException {
        List<CalendarDate> cdates = timeAxis.getCalendarDates();
        //java.util.Date[] dates = timeAxis.getTimeDates();
        List<CalendarDateTime> times =
            new ArrayList<CalendarDateTime>(cdates.size());
        for (CalendarDate cdate : cdates) {
            times.add(makeDateTime(cdate));
        }
        return times;
    }

    /**
     * Make a List of CalendarDateTime objects from the times in the timeAxis
     *
     * @param timeAxis  the time axis
     * @return List of DateTimes
     * @throws VisADException  problem parsing timeAxis calendar dates
     */
    public static List<CalendarDateTime> makeDateTimes(
            CoverageCoordAxis1D timeAxis)
            throws VisADException {
        List<ucar.nc2.util.NamedObject> ttt= timeAxis.getCoordValueNames();
        List<CalendarDateTime> times =
                new ArrayList<CalendarDateTime>(ttt.size());
        double [] t = timeAxis.getValues();
        int i = 0;
        for(Object oj: ttt) {
            NamedAnything anything = (NamedAnything)oj;
            if(anything.getValue() instanceof CoordInterval){
                double [] od = (double [])timeAxis.getCoordObject(i++);
                CalendarDate cdate = timeAxis.makeDate(od[1]);
                times.add(DataUtil.makeDateTime(cdate));
            } else {
                CalendarDate cdate = (CalendarDate) anything.getValue();
                times.add(DataUtil.makeDateTime(cdate));
            }
        }

        return times;
    }
    /**
     * Make a date object corresponding the the calendar date
     *
     * @param cdate   the CalendarDate
     * @return the corresponding Date
     * @throws VisADException  problem parsing cdate
     */
    public static Date makeDate(CalendarDate cdate) throws VisADException {
        CalendarDateTime dt = makeDateTime(cdate);
        return new Date((long) dt.getValue() * 1000);
    }

    /**
     * Make a label for the stride/sampling
     * @param xStride  the x stride
     * @param yStride  the y stride
     * @param pointType    the name for the point type
     * @return
     */
    public static String makeSamplingLabel(int xStride, int yStride, String pointType) {
    	StringBuilder buf = new StringBuilder();
    	if (xStride > 1 && yStride > 1) {
    		buf.append("Data Sampling: every ");
    			buf.append(getStrideLabel(xStride));
    		if (xStride != yStride) {
    			buf.append(" by ");
    			buf.append(getStrideLabel(yStride));
    		}
    		buf.append(" ");
    		buf.append(pointType);
    	}
    	return buf.toString();
    }
    
    /**
     * Get a label for a stride value
     * @param strideValue the value
     * @return
     */
    private static String getStrideLabel(int strideValue) {
    	int remainder = strideValue%10;
    	if (strideValue == 2) {
    		//return "other";
    		return "2nd";
    	//} else if (strideValue == 3) {
    	//	return strideValue+"rd";
    	} else if (remainder == 1 && strideValue != 11) {
    		return strideValue+"st";
    	} else if (remainder == 2 && strideValue != 12) {
    		return strideValue+"nd";
    	} else if (remainder == 3 && strideValue != 13) {
    		return strideValue+"rd";
    	} else {
    		return strideValue+"th";
    	}
    }
}
