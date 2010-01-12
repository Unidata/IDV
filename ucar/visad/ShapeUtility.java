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

package ucar.visad;


import ucar.unidata.util.TwoFacedObject;


import visad.*;

import visad.meteorology.WeatherSymbols;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.*;

import javax.media.j3d.Transform3D;

import javax.vecmath.*;



/**
 * A utility class for manipulating VisAD Shapes (VisADGeometryArrays).
 * Adapted from RAP's ShapeFactory class.
 *
 * @author IDV Development Team
 */
public class ShapeUtility {

    /** use in text calls */
    public static final double[] TEXT_START = { 0.0, 0.0, 0.0 };

    /** use in text calls */
    public static final double[] TEXT_BASE = { 1.0, 0.0, 0.0 };

    /** use in text calls */
    public static final double[] TEXT_UP = { 0.0, 1.0, 0.0 };


    /** Id for a plus shape */
    public static final String PLUS = "PLUS";

    /** Id for a plus shape */
    public static final String MINUS = "MINUS";

    /** Id for a plus shape */
    public static final String DOWNLINE = "DOWNLINE";

    /** Id for a plus shape */
    public static final String RIGHTARROW = "RIGHTARROW";

    /** Id for a plus shape */
    public static final String LEFTARROW = "LEFTARROW";

    /** Id for a plus shape */
    public static final String UPARROW = "UPARROW";

    /** Id for a plus shape */
    public static final String DOWNARROW = "DOWNARROW";


    /** Id for a horizontal line shape */
    public static final String HORLINE = "HORLINE";

    /** Id for a vertical line shape */
    public static final String VERTLINE = "VERTLINE";


    /** Id for a cross shape */
    public static final String CROSS = "CROSS";

    /** Id for a square (unfilled) shape */
    public static final String SQUARE = "SQUARE";

    /** Id for a null shape */
    public static final String NONE = "NONE";

    /** Id for a filled square shape */
    public static final String FILLED_SQUARE = "FILLED_SQUARE";

    /** Id for a cube shape */
    public static final String CUBE = "CUBE";

    /** Id for a pyramid of 4 faces (triangular base) */
    public static final String PYRAMID_4FACE = "PYRAMID_4FACE";

    /** Id for a pyramid of 5 faces (square base) */
    public static final String PYRAMID_5FACE = "PYRAMID_5FACE";

    /** Id for a triangle (unfilled) shape */
    public static final String TRIANGLE = "TRIANGLE";

    /** Id for a filled triangle shape */
    public static final String FILLED_TRIANGLE = "FILLED_TRIANGLE";

    /** Id for a filled triangle shape */
    public static final String FILLED_SPHERE = "FILLED_SPHERE";

    /** Id for a 2-D airplane shape */
    public static final String AIRPLANE = "AIRPLANE";

    /** Id for a 3-D airplane shape */
    public static final String AIRPLANE3D = "AIRPLANE3D";

    /** shape id */
    public static final String PIN = "PIN";

    /** Hurricane symbol */
    public static final String HURRICANE = "HURRICANE";

    /** tropical storm symbol */
    public static final String TROPICALSTORM = "TROPICALSTORM";

    /** The identifier for a square station location marker */
    public static final int MISC_ID_SQUARE = 0;

    /** The identifier for a filled square station location marker */
    public static final int MISC_ID_FILLED_SQUARE = 1;

    /** The identifier for a circle station location marker */
    public static final int MISC_ID_CIRCLE = 2;

    /** The identifier for a filled circle station location marker */
    public static final int MISC_ID_FILLED_CIRCLE = 3;

    /** The identifier for a triangle station location marker */
    public static final int MISC_ID_TRIANGLE = 4;

    /** The identifier for a filled triangle station location marker */
    public static final int MISC_ID_FILLED_TRIANGLE = 5;

    /** The identifier for a diamond station location marker */
    public static final int MISC_ID_DIAMOND = 6;

    /** The identifier for a filled diamond station location marker */
    public static final int MISC_ID_FILLED_DIAMOND = 7;

    /** The identifier for a star station location marker */
    public static final int MISC_ID_STAR = 8;

    /** The identifier for a filled start station location marker */
    public static final int MISC_ID_FILLED_STAR = 9;

    /** The identifier for a plus station location marker */
    public static final int MISC_ID_PLUS = 14;

    /** The identifier for a pin station location marker */
    public static final int MISC_ID_PIN = 15;

    /** The identifier for a minus station location marker */
    public static final int MISC_ID_MINUS = 16;

    /** Id for tropical storm */
    public static final int MISC_ID_TROPICALSTORM = 17;

    /** The identifier for a minus station location marker */
    public static final int MISC_ID_HURRICANE = 18;

    /** shape name from WeatherSymbols */
    public static final String CIRCLE = "CIRCLE";

    /** shape name from WeatherSymbols */
    public static final String FILLED_CIRCLE = "FILLED_CIRCLE";

    /** shape name from WeatherSymbols */
    public static final String DIAMOND = "DIAMOND";

    /** shape name from WeatherSymbols */
    public static final String FILLED_DIAMOND = "FILLED_DIAMOND";

    /** shape name from WeatherSymbols */
    public static final String STAR = "STAR";

    /** shape name from WeatherSymbols */
    public static final String FILLED_STAR = "FILLED_STAR";


    /** shape names from WeatherSymbols */
    public static final String[] MISC_NAMES = {
        CIRCLE, FILLED_CIRCLE, DIAMOND, FILLED_DIAMOND, STAR, FILLED_STAR,
        TROPICALSTORM, HURRICANE,
    };

    /** shape ids from WeatherSymbols */
    public static final int[] MISC_IDS = {
        MISC_ID_CIRCLE, MISC_ID_FILLED_CIRCLE, MISC_ID_DIAMOND,
        MISC_ID_FILLED_DIAMOND, MISC_ID_STAR, MISC_ID_FILLED_STAR,
        MISC_ID_TROPICALSTORM, MISC_ID_HURRICANE
    };



    /** Array of all of the shapes */
    public static final TwoFacedObject[] SHAPES = {
        new TwoFacedObject("Pin", PIN), new TwoFacedObject("Plus", PLUS),
        new TwoFacedObject("Minus", MINUS),
        new TwoFacedObject("Cross", CROSS),
        new TwoFacedObject("Square", SQUARE),
        new TwoFacedObject("Filled Square", FILLED_SQUARE),
        new TwoFacedObject("Triangle", TRIANGLE),
        new TwoFacedObject("Filled Triangle", FILLED_TRIANGLE),
        new TwoFacedObject("Circle", CIRCLE),
        new TwoFacedObject("Filled Circle", FILLED_CIRCLE),
        new TwoFacedObject("Diamond", DIAMOND),
        new TwoFacedObject("Filled Diamond", FILLED_DIAMOND),
        new TwoFacedObject("Star", STAR),
        new TwoFacedObject("Filled Star", FILLED_STAR),
        new TwoFacedObject("Hor. Line", HORLINE),
        new TwoFacedObject("Vert. Line", VERTLINE),
        new TwoFacedObject("Left Arrow", LEFTARROW),
        new TwoFacedObject("Right Arrow", RIGHTARROW),
        new TwoFacedObject("Up Arrow", UPARROW),
        new TwoFacedObject("Down Arrow", DOWNARROW),
        new TwoFacedObject("Cube", CUBE),
        new TwoFacedObject("Down Line", DOWNLINE),
        new TwoFacedObject("4 Face Pyramid", PYRAMID_4FACE),
        new TwoFacedObject("5 Face Pyramid", PYRAMID_5FACE),
        new TwoFacedObject("Sphere", FILLED_SPHERE),
        new TwoFacedObject("2-D Airplane", AIRPLANE),
        new TwoFacedObject("3-D Airplane", AIRPLANE3D),
        new TwoFacedObject("Hurricane", HURRICANE),
        new TwoFacedObject("Tropical Storm", TROPICALSTORM),
        new TwoFacedObject("None", NONE)
    };



    /** coordinates */
    public static final float L_X = -1.0f;

    /** coordinates */
    public static final float ML_X = -0.5f;

    /** coordinates */
    public static final float C_X = 0.0f;

    /** coordinates */
    public static final float MR_X = 0.5f;

    /** coordinates */
    public static final float R_X = 1.0f;

    /** coordinates */
    public static final float T_Y = -1.0f;

    /** coordinates */
    public static final float MT_Y = -0.5f;

    /** coordinates */
    public static final float C_Y = 0.0f;

    /** coordinates */
    public static final float MB_Y = 0.5f;

    /** coordinates */
    public static final float B_Y = 1.0f;

    /** coordinates */
    public static final float D_Z = -1.0f;

    /** coordinates */
    public static final float C_Z = 0.0f;

    /** coordinates */
    public static final float U_Z = 1.0f;


    /** Default constructor */
    public ShapeUtility() {}


    /**
     * Create a predefined shape.  Shapes are drawn on a 1x1(x1) box.
     * Use <code>setSize()</code> methods to rescale.
     *
     * @param  s  shape to create
     *
     * @deprecated Use create shape
     * @return corresponding shape
     */
    public static VisADGeometryArray makeShape(String s) {
        VisADGeometryArray[] result = createShape(s);
        if (s == null) {
            return null;
        }
        return result[0];
    }

    /**
     * Create a shape from a string name
     *
     * @param s name of the staring
     *
     * @return corresponding array
     */
    public static VisADGeometryArray[] createShape(String s) {

        int symbolIndex = WxSymbolGroup.getIndex(s);
        if (symbolIndex >= 0) {
            return setSize(new VisADGeometryArray[] {
                WeatherSymbols.getSymbol(symbolIndex) }, 6.0f);
        }


        float                scale  = 1.0f;
        VisADGeometryArray   shape  = null;
        VisADGeometryArray[] shapes = null;
        if (s.equals(NONE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {};
        } else if (s.equals(PIN)) {
            VisADGeometryArray circle =
                WeatherSymbols.getMiscSymbol(MISC_ID_FILLED_CIRCLE);
            circle = offset(circle, 1.0f, 1.0f, 0.0f);
            VisADGeometryArray dart = new VisADLineArray();
            dart.coordinates = new float[] {
                C_X, C_Y, C_Z, R_X, B_Y, C_Z
            };
            //            dart.coordinates = new float[] {C_X,C_Y,C_Z, 
            //                                            R_X-0.25f,B_Y,C_Z, 
            //                                            R_X,B_Y-0.25f,C_Z};
            //            shapes = new VisADGeometryArray[]{ circle, dart,
            //                                               makeShape(SQUARE) };
            shapes = new VisADGeometryArray[] { circle, dart };
        } else if (s.equals(PLUS)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                L_X, C_Y, C_Z, R_X, C_Y, C_Z, C_X, T_Y, C_Z, C_X, B_Y, C_Z
            };
        } else if (s.equals(MINUS)) {  // same as hor line
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                L_X, C_Y, C_Z, C_X, C_Y, C_Z
            };
        } else if (s.equals(VERTLINE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                C_X, B_Y, C_Z, C_X, C_Y, C_Z
            };
        } else if (s.equals(HORLINE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                L_X, C_Y, C_Z, C_X, C_Y, C_Z
            };
        } else if (s.equals(LEFTARROW)) {
            scale             = 1.2f;
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                R_X, C_Y, C_Z, C_X, C_Y, C_Z, C_X, C_Y, C_Z, MR_X / 2.0f,
                -0.25f, C_Z, C_X, C_Y, C_Z, MR_X / 2.0f, 0.25f, C_Z
            };

        } else if (s.equals(RIGHTARROW)) {
            scale             = 1.2f;
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                L_X, C_Y, C_Z, C_X, C_Y, C_Z, C_X, C_Y, C_Z, ML_X / 2.0f,
                -0.25f, C_Z, C_X, C_Y, C_Z, ML_X / 2.0f, 0.25f, C_Z
            };
        } else if (s.equals(DOWNARROW)) {
            scale             = 1.2f;
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                C_X, B_Y, C_Z, C_X, C_Y, C_Z, C_X, C_Y, C_Z, ML_X / 2.0f,
                MB_Y / 2.0f, C_Z, C_X, C_Y, C_Z, MR_X / 2.0f, MB_Y / 2.0f, C_Z
            };
        } else if (s.equals(UPARROW)) {
            scale             = 1.2f;
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                C_X, T_Y, C_Z, C_X, C_Y, C_Z, C_X, C_Y, C_Z, ML_X / 2.0f,
                MT_Y / 2.0f, C_Z, C_X, C_Y, C_Z, MR_X / 2.0f, MT_Y / 2.0f, C_Z
            };


        } else if (s.equals(CROSS)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                R_X, C_Y, C_Z, L_X, C_Y, C_Z, C_X, T_Y, C_Z, C_X, B_Y, C_Z,
                C_X, C_Y, U_Z, C_X, C_Y, D_Z
            };

        } else if (s.equals(DOWNLINE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                C_X, C_Y, C_Z, C_X, C_Y, -20.0f
            };
        } else if (s.equals(SQUARE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                R_X, B_Y, C_Z, R_X, T_Y, C_Z, R_X, T_Y, C_Z, L_X, T_Y, C_Z,
                L_X, T_Y, C_Z, L_X, B_Y, C_Z, L_X, B_Y, C_Z, R_X, B_Y, C_Z
            };
        } else if (s.equals(TRIANGLE)) {
            shape             = new VisADLineArray();
            shape.coordinates = new float[] {
                L_X, MT_Y, C_Z, R_X, MT_Y, C_Z, L_X, MT_Y, C_Z, C_X, B_Y, C_Z,
                R_X, MT_Y, C_Z, C_X, B_Y, C_Z
            };
        } else if (s.equals(FILLED_TRIANGLE)) {
            shape             = new VisADTriangleArray();
            shape.coordinates = new float[] {
                L_X, MT_Y, C_Z, R_X, MT_Y, C_Z, C_X, B_Y, C_Z
            };
        } else if (s.equals(FILLED_SQUARE)) {
            shape             = new VisADQuadArray();
            shape.coordinates = new float[] {
                R_X, B_Y, C_Z, R_X, T_Y, C_Z, L_X, T_Y, C_Z, L_X, B_Y, C_Z
            };

            shape.normals     = new float[12];
            for (int i = 0; i < 12; i += 3) {
                shape.normals[i]     = 0.0f;
                shape.normals[i + 1] = 0.0f;
                shape.normals[i + 2] = 1.0f;
            }

        } else if (s.equals(PYRAMID_4FACE)) {
            shape             = new VisADQuadArray();
            shape.coordinates = new float[] {
                // each line here is 2 sets of xyz values for 2 end points 
                // of one edge of one face, 3 edges of the triangular face 1.
                C_X, C_Y, U_Z, L_X, B_Y, -0.5f, L_X, B_Y, -0.5f, R_X, C_Y,
                -0.5f, R_X, C_Y, -0.5f, C_X, C_Y, U_Z,
                // face 2:
                C_X, C_Y, U_Z, R_X, C_Y, -0.5f, R_X, C_Y, -0.5f, L_X, T_Y,
                -0.5f, L_X, T_Y, -0.5f, C_X, C_Y, U_Z,
                // face 3
                C_X, C_Y, U_Z, L_X, B_Y, -0.5f, L_X, B_Y, -0.5f, L_X, T_Y,
                -0.5f, L_X, T_Y, -0.5f, C_X, C_Y, U_Z,
                // face 4, flat bottom
                R_X, C_Y, -0.5f, L_X, B_Y, -0.5f, L_X, B_Y, -0.5f, L_X, T_Y,
                -0.5f, L_X, T_Y, -0.5f, R_X, C_Y, -0.5f
            };

            shape.normals = new float[72];
            for (int i = 0; i < 12; i += 3) {
                shape.normals[i]      = 0.33f;
                shape.normals[i + 1]  = 0.66f;
                shape.normals[i + 2]  = 0.66f;

                shape.normals[i + 12] = 0.33f;
                shape.normals[i + 13] = -0.66f;
                shape.normals[i + 14] = 0.66f;

                shape.normals[i + 24] = -1.0f;
                shape.normals[i + 25] = 0.0f;
                shape.normals[i + 26] = 0.666f;

                shape.normals[i + 36] = 0.0f;
                shape.normals[i + 37] = 0.0f;
                shape.normals[i + 38] = -1.0f;
            }

        } else if (s.equals(PYRAMID_5FACE)) {
            shape             = new VisADQuadArray();
            shape.coordinates = new float[] {
                // each line here is 2 sets of xyz values for 2 end points of 
                // one edge of one face, 3 edges of the triangular face 1.
                0.0f, 0.0f, 1.41f, .0f, 1.0f, .0f, .0f, 1.0f, .0f, 1.0f, 0.0f,
                .0f, 1.0f, 0.0f, .0f, 0.0f, 0.0f, 1.41f,
                // face 2:
                0.0f, 0.0f, 1.41f, 1.0f, 0.0f, .0f, 1.0f, 0.0f, .0f, .0f,
                -1.0f, 0.0f, .0f, -1.0f, .0f, 0.0f, 0.0f, 1.41f,
                // face 3
                0.0f, 0.0f, 1.41f, -1.0f, .0f, .0f, -1.0f, .0f, .0f, .0f,
                -1.0f, .0f, .0f, -1.0f, .0f, 0.0f, 0.0f, 1.41f,
                // face 4
                0.0f, 0.0f, 1.41f, -1.0f, .0f, .0f, -1.0f, .0f, .0f, .0f,
                1.0f, .0f, .0f, 1.0f, .0f, 0.0f, 0.0f, 1.41f,
                // face 5, flat bottom, 4 edges
                1.0f, 0.0f, .0f, .0f, -1.0f, .0f, .0f, -1.0f, .0f, -1.0f, .0f,
                .0f, -1.0f, .0f, .0f, .0f, 1.0f, 0.0f, .0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f
            };

            shape.normals = new float[96];
            for (int i = 0; i < 18; i += 3) {
                // normals to edges of face one
                shape.normals[i]     = 0.50f;
                shape.normals[i + 1] = 0.50f;
                shape.normals[i + 2] = 0.33f;
                // normals to edges of face 2
                shape.normals[i + 18] = 0.50f;
                shape.normals[i + 19] = -0.50f;
                shape.normals[i + 20] = 0.33f;

                shape.normals[i + 36] = -0.50f;
                shape.normals[i + 37] = -0.50f;
                shape.normals[i + 38] = 0.33f;

                shape.normals[i + 54] = -0.50f;
                shape.normals[i + 55] = 0.50f;
                shape.normals[i + 56] = 0.33f;

                shape.normals[i + 72] = 0.0f;
                shape.normals[i + 73] = 0.0f;
                shape.normals[i + 74] = -1.0f;
            }


            shape.normals[90] = 0.0f;
            shape.normals[91] = 0.0f;
            shape.normals[92] = -1.0f;
            shape.normals[93] = 0.0f;
            shape.normals[94] = 0.0f;
            shape.normals[95] = -1.0f;
        } else if (s.equals(PIN)) {
            //TODO:
            //            shape = WeatherSymbols.getMiscSymbol(MISC_ID_FILLED_CIRCLE);
        } else if (s.equals(CUBE)) {
            shape             = new VisADQuadArray();
            shape.coordinates = new float[] {
                //face 1:
                R_X, B_Y, -1.0f, R_X, T_Y, -1.0f, R_X, T_Y, -1.0f, L_X, T_Y,
                -1.0f,
                //face 2:
                L_X, T_Y, -1.0f, L_X, B_Y, -1.0f, L_X, B_Y, -1.0f, R_X, B_Y,
                -1.0f,
                //face 3:
                R_X, B_Y, 1.0f, R_X, T_Y, 1.0f, R_X, T_Y, 1.0f, L_X, T_Y,
                1.0f,
                //face 4:
                L_X, T_Y, 1.0f, L_X, B_Y, 1.0f, L_X, B_Y, 1.0f, R_X, B_Y,
                1.0f,
                //face 5:
                R_X, B_Y, 1.0f, R_X, B_Y, -1.0f, R_X, B_Y, -1.0f, R_X, T_Y,
                -1.0f,
                //face 6:
                R_X, T_Y, -1.0f, R_X, T_Y, 1.0f, R_X, T_Y, 1.0f, R_X, B_Y,
                1.0f,
                //face 7:
                L_X, B_Y, 1.0f, L_X, B_Y, -1.0f, L_X, B_Y, -1.0f, L_X, T_Y,
                -1.0f,
                //face 8:
                L_X, T_Y, -1.0f, L_X, T_Y, 1.0f, L_X, T_Y, 1.0f, L_X, B_Y,
                1.0f,
                //face 9:
                R_X, B_Y, 1.0f, R_X, B_Y, -1.0f, R_X, B_Y, -1.0f, L_X, B_Y,
                -1.0f,
                //face 10:
                L_X, B_Y, -1.0f, L_X, B_Y, 1.0f, L_X, B_Y, 1.0f, R_X, B_Y,
                1.0f,
                //face 11:
                R_X, T_Y, 1.0f, R_X, T_Y, -1.0f, R_X, T_Y, -1.0f, L_X, T_Y,
                -1.0f,
                //face 12:
                L_X, T_Y, -1.0f, L_X, T_Y, 1.0f, L_X, T_Y, 1.0f, R_X, T_Y,
                1.0f
            };

            shape.normals = new float[144];
            for (int i = 0; i < 24; i += 3) {
                shape.normals[i]       = 0.0f;
                shape.normals[i + 1]   = 0.0f;
                shape.normals[i + 2]   = -1.0f;

                shape.normals[i + 24]  = 0.0f;
                shape.normals[i + 25]  = 0.0f;
                shape.normals[i + 26]  = 1.0f;

                shape.normals[i + 48]  = 1.0f;
                shape.normals[i + 49]  = 0.0f;
                shape.normals[i + 50]  = 0.0f;

                shape.normals[i + 72]  = -1.0f;
                shape.normals[i + 73]  = 0.0f;
                shape.normals[i + 74]  = 0.0f;

                shape.normals[i + 96]  = 0.0f;
                shape.normals[i + 97]  = 1.0f;
                shape.normals[i + 98]  = 0.0f;

                shape.normals[i + 120] = 0.0f;
                shape.normals[i + 121] = -1.0f;
                shape.normals[i + 122] = 0.0f;
            }
        } else if (s.equals(FILLED_SPHERE)) {
            shape             = new VisADTriangleArray();
            shape.coordinates = new float[] {
                1.000000f, 0.000000f, 0.000000f, 0.707107f, 0.000000f,
                0.707107f, 0.707107f, 0.707107f, 0.000000f, 0.707107f,
                0.000000f, 0.707107f, 0.000000f, 0.000000f, 1.000000f,
                0.000000f, 0.707107f, 0.707107f, 0.707107f, 0.707107f,
                0.000000f, 0.707107f, 0.000000f, 0.707107f, 0.000000f,
                0.707107f, 0.707107f, 0.707107f, 0.707107f, 0.000000f,
                0.000000f, 0.707107f, 0.707107f, 0.000000f, 1.000000f,
                0.000000f, 0.000000f, 1.000000f, 0.000000f, 0.000000f,
                0.707107f, 0.707107f, -0.707107f, 0.707107f, 0.000000f,
                0.000000f, 0.707107f, 0.707107f, 0.000000f, 0.000000f,
                1.000000f, -0.707107f, 0.000000f, 0.707107f, -0.707107f,
                0.707107f, 0.000000f, 0.000000f, 0.707107f, 0.707107f,
                -0.707107f, 0.000000f, 0.707107f, -0.707107f, 0.707107f,
                0.000000f, -0.707107f, 0.000000f, 0.707107f, -1.000000f,
                0.000000f, 0.000000f, -1.000000f, 0.000000f, 0.000000f,
                -0.707107f, 0.000000f, 0.707107f, -0.707107f, -0.707107f,
                0.000000f, -0.707107f, 0.000000f, 0.707107f, 0.000000f,
                0.000000f, 1.000000f, 0.000000f, -0.707107f, 0.707107f,
                -0.707107f, -0.707107f, 0.000000f, -0.707107f, 0.000000f,
                0.707107f, 0.000000f, -0.707107f, 0.707107f, -0.707107f,
                -0.707107f, 0.000000f, 0.000000f, -0.707107f, 0.707107f,
                0.000000f, -1.000000f, 0.000000f, 0.000000f, -1.000000f,
                0.000000f, 0.000000f, -0.707107f, 0.707107f, 0.707107f,
                -0.707107f, 0.000000f, 0.000000f, -0.707107f, 0.707107f,
                0.000000f, 0.000000f, 1.000000f, 0.707107f, 0.000000f,
                0.707107f, 0.707107f, -0.707107f, 0.000000f, 0.000000f,
                -0.707107f, 0.707107f, 0.707107f, 0.000000f, 0.707107f,
                0.707107f, -0.707107f, 0.000000f, 0.707107f, 0.000000f,
                0.707107f, 1.000000f, 0.000000f, 0.000000f, 1.000000f,
                0.000000f, 0.000000f, 0.707107f, 0.707107f, 0.000000f,
                0.707107f, 0.000000f, -0.707107f, 0.707107f, 0.707107f,
                0.000000f, 0.000000f, 1.000000f, 0.000000f, 0.000000f,
                0.707107f, -0.707107f, 0.707107f, 0.000000f, -0.707107f,
                0.707107f, 0.707107f, 0.000000f, 0.000000f, 0.707107f,
                -0.707107f, 0.707107f, 0.000000f, -0.707107f, 0.000000f,
                0.707107f, -0.707107f, 0.000000f, 0.000000f, -1.000000f,
                0.000000f, 1.000000f, 0.000000f, -0.707107f, 0.707107f,
                0.000000f, 0.000000f, 0.707107f, -0.707107f, -0.707107f,
                0.707107f, 0.000000f, -1.000000f, 0.000000f, 0.000000f,
                -0.707107f, 0.000000f, -0.707107f, 0.000000f, 0.707107f,
                -0.707107f, -0.707107f, 0.707107f, 0.000000f, -0.707107f,
                0.000000f, -0.707107f, 0.000000f, 0.707107f, -0.707107f,
                -0.707107f, 0.000000f, -0.707107f, 0.000000f, 0.000000f,
                -1.000000f, -1.000000f, 0.000000f, 0.000000f, -0.707107f,
                -0.707107f, 0.000000f, -0.707107f, 0.000000f, -0.707107f,
                -0.707107f, -0.707107f, 0.000000f, 0.000000f, -1.000000f,
                0.000000f, 0.000000f, -0.707107f, -0.707107f, -0.707107f,
                0.000000f, -0.707107f, -0.707107f, -0.707107f, 0.000000f,
                0.000000f, -0.707107f, -0.707107f, -0.707107f, 0.000000f,
                -0.707107f, 0.000000f, -0.707107f, -0.707107f, 0.000000f,
                0.000000f, -1.000000f, 0.000000f, -1.000000f, 0.000000f,
                0.707107f, -0.707107f, 0.000000f, 0.000000f, -0.707107f,
                -0.707107f, 0.707107f, -0.707107f, 0.000000f, 1.000000f,
                0.000000f, 0.000000f, 0.707107f, 0.000000f, -0.707107f,
                0.000000f, -0.707107f, -0.707107f, 0.707107f, -0.707107f,
                0.000000f, 0.707107f, 0.000000f, -0.707107f, 0.000000f,
                -0.707107f, -0.707107f, 0.707107f, 0.000000f, -0.707107f,
                0.000000f, 0.000000f, -1.000000f,
            };
        } else if (s.equals(AIRPLANE)) {
            scale             = 1.2f;
            shape             = new VisADTriangleArray();
            shape.coordinates = new float[] {
                0.0f, -1.0f, 0.0f, 0.1f, 0.8f, 0.0f, -0.1f, 0.8f, 0.0f, 0.0f,
                -0.9f, 0.0f, 0.1f, 0.8f, 0.0f, -0.1f, 0.8f, 0.0f, -0.025f,
                -0.9f, 0.0f, 0.025f, -0.9f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.35f, 0.0f, 0.0f, 0.05f, 0.0f, 0.8f, 0.0f, 0.0f, 0.0f, 0.35f,
                0.0f, 0.0f, .05f, 0.0f, -0.8f, 0.0f, 0.0f, 0.0f, -0.75f, 0.0f,
                0.0f, -0.85f, 0.0f, .25f, -0.85f, 0.0f, 0.0f, -0.75f, 0.0f,
                0.0f, -0.85f, 0.0f, -0.25f, -0.85f, 0.0f
            };

            // from Mike Masscotte, Embry-Riddle Aeronautical University
        } else if (s.equals(AIRPLANE3D)) {
            scale             = 1.2f;
            shape             = new VisADQuadArray();
            shape.coordinates = new float[] {
                //fuselage
                0.0f, 0.6f, 0.2f, 0.1f, 0.6f, 0.0f, 0.1f, -0.6f, 0.0f, 0.0f,
                -0.6f, 0.2f, 0.1f, 0.6f, 0.0f, 0.0f, 0.6f, -0.2f, 0.0f, -0.6f,
                -0.2f, -0.1f, -0.6f, 0.0f, 0.0f, 0.6f, -0.2f, -0.1f, 0.6f,
                0.0f, -0.1f, -0.6f, 0.0f, 0.0f, -0.6f, -0.2f, -0.1f, 0.6f,
                0.0f, 0.0f, 0.6f, 0.2f, 0.0f, -0.6f, 0.2f, -0.1f, -0.6f, 0.0f,
                //nose cone
                0.0f, 0.6f, 0.2f, 0.1f, 0.6f, 0.0f, 0.0125f, 0.7f, 0.0f, 0.0f,
                0.7f, 0.0125f, 0.1f, 0.6f, 0.0f, 0.0f, 0.6f, -0.2f, 0.0f,
                0.7f, -0.0125f, 0.0125f, 0.7f, 0.0f, 0.0f, 0.6f, -0.2f, -0.1f,
                0.6f, 0.0f, 0.0125f, 0.7f, 0.0f, 0.0f, 0.7f, -0.0125f, -0.1f,
                0.6f, 0.0f, 0.0f, 0.6f, 0.2f, 0.0f, 0.7f, 0.0125f, -0.0125f,
                0.7f, 0.0f, 0.0f, 0.7f, 0.0125f, 0.0125f, 0.7f, 0.0f, 0.0f,
                0.7f, -0.0125f, -0.0125f, 0.7f, 0.0f,
                //tail cone
                0.0f, -0.6f, 0.2f, 0.1f, -0.6f, 0.0f, 0.0125f, -0.7f, 0.0f,
                0.0f, -0.7f, 0.0125f, 0.1f, -0.6f, 0.0f, 0.0f, -0.6f, -0.2f,
                0.0f, -0.7f, -0.0125f, 0.0125f, -0.7f, 0.0f, 0.0f, -0.6f,
                -0.2f, -0.1f, -0.6f, 0.0f, 0.0125f, -0.7f, 0.0f, 0.0f, -0.7f,
                -0.0125f, -0.1f, -0.6f, 0.0f, 0.0f, -0.6f, 0.2f, 0.0f, -0.7f,
                0.0125f, -0.0125f, -0.7f, 0.0f, 0.0f, -0.7f, 0.0125f, 0.0125f,
                -0.7f, 0.0f, 0.0f, -0.7f, -0.0125f, -0.0125f, -0.7f, 0.0f,
                //vert stab

                //left
                0.0125f, -0.5f, 0.1f, 0.0125f, -0.7f, 0.0125f, 0.0125f, -0.8f,
                0.6f, 0.0125f, -0.7f, 0.6f,
                //right
                -0.0125f, -0.5f, 0.1f, -0.0125f, -0.7f, 0.0125f, -0.0125f,
                -0.8f, 0.6f, -0.0125f, -0.7f, 0.6f,
                //top
                0.0125f, -0.7f, 0.6f, 0.0125f, -0.8f, 0.6f, -0.0125f, -0.8f,
                0.6f, -0.0125f, -0.7f, 0.6f,
                //front
                0.0125f, -0.5f, 0.1f, -0.0125f, -0.5f, 0.1f, -0.0125f, -0.7f,
                0.6f, 0.0125f, -0.7f, 0.6f,
                //back
                0.0125f, -0.7f, 0.0125f, -0.0125f, -0.7f, 0.0125f, -0.0125f,
                -0.8f, 0.6f, 0.0125f, -0.8f, 0.6f,
                //horiz stab

                //top right
                0.0125f, -0.6f, 0.0125f, 0.4f, -0.7f, 0.0125f, 0.4f, -0.8f,
                0.0125f, 0.0f, -0.75f, 0.0125f,
                //bottom right
                0.0125f, -0.6f, -0.0125f, 0.4f, -0.7f, -0.0125f, 0.4f, -0.8f,
                -0.0125f, 0.0f, -0.75f, -0.0125f,
                //front right
                0.0125f, -0.6f, 0.0125f, 0.4f, -0.7f, 0.0125f, 0.4f, -0.7f,
                -0.0125f, 0.09f, -0.6f, -0.0125f,
                //back right
                0.0f, -0.75f, 0.0125f, 0.4f, -0.8f, 0.0125f, 0.4f, -0.8f,
                -0.0125f, 0.0f, -0.75f, -0.0125f,
                //top left
                -0.0125f, -0.6f, 0.0125f, -0.4f, -0.7f, 0.0125f, -0.4f, -0.8f,
                0.0125f, -0.0f, -0.75f, 0.0125f,
                //bottom left
                -0.0125f, -0.6f, -0.0125f, -0.4f, -0.7f, -0.0125f, -0.4f,
                -0.8f, -0.0125f, -0.0f, -0.75f, -0.0125f,
                //front left
                -0.0125f, -0.6f, 0.0125f, -0.4f, -0.7f, 0.0125f, -0.4f, -0.7f,
                -0.0125f, -0.09f, -0.6f, -0.0125f,
                //back left
                -0.0f, -0.75f, 0.0125f, -0.4f, -0.8f, 0.0125f, -0.4f, -0.8f,
                -0.0125f, -0.0f, -0.75f, -0.0125f,
                //wings
                //right top
                0.09f, 0.1f, 0.0125f, 0.8f, -0.2f, 0.0125f, 0.8f, -0.3f,
                0.0125f, 0.09f, -0.2f, 0.0125f,
                //right bottom
                0.09f, 0.1f, -0.0125f, 0.8f, -0.2f, -0.0125f, 0.8f, -0.3f,
                -0.0125f, 0.09f, -0.2f, -0.0125f,
                //right front
                0.09f, 0.1f, 0.0125f, 0.8f, -0.2f, 0.0125f, 0.8f, -0.2f,
                -0.0125f, 0.09f, 0.1f, -0.0125f,
                //right side
                0.8f, -0.2f, 0.0125f, 0.8f, -0.3f, 0.0125f, 0.8f, -0.3f,
                -0.0125f, 0.8f, -0.2f, -0.0125f,
                //right back
                0.09f, -0.2f, 0.0125f, 0.8f, -0.3f, 0.0125f, 0.8f, -0.3f,
                -0.0125f, 0.09f, -0.2f, -0.0125f,
                //left top
                -0.09f, 0.1f, 0.0125f, -0.8f, -0.2f, 0.0125f, -0.8f, -0.3f,
                0.0125f, -0.09f, -0.2f, 0.0125f,
                //left bottom
                -0.09f, 0.1f, -0.0125f, -0.8f, -0.2f, -0.0125f, -0.8f, -0.3f,
                -0.0125f, -0.09f, -0.2f, -0.0125f,
                //left front
                -0.09f, 0.1f, 0.0125f, -0.8f, -0.2f, 0.0125f, -0.8f, -0.2f,
                -0.0125f, -0.09f, 0.1f, -0.0125f,
                //left side
                -0.8f, -0.2f, 0.0125f, -0.8f, -0.3f, 0.0125f, -0.8f, -0.3f,
                -0.0125f, -0.8f, -0.2f, -0.0125f,
                //left back
                -0.09f, -0.2f, 0.0125f, -0.8f, -0.3f, 0.0125f, -0.8f, -0.3f,
                -0.0125f, -0.09f, -0.2f, -0.0125f
            };
        } else {
            for (int i = 0; i < MISC_NAMES.length; i++) {
                if (s.equals(MISC_NAMES[i])) {
                    shape = WeatherSymbols.getMiscSymbol(MISC_IDS[i]);
                    break;
                }
            }
            scale = 2.5f;
        }
        if ((shapes == null) && (shape == null)) {
            throw new IllegalArgumentException("unsupported shape " + s);
        }
        if (shapes == null) {
            shapes = new VisADGeometryArray[] { shape };
        }
        if (scale != 1.0f) {
            shapes = setSize(shapes, scale);
        }
        for (int i = 0; i < shapes.length; i++) {
            shapes[i].vertexCount = shapes[i].coordinates.length / 3;
        }
        return shapes;
    }


    /**
     * Wrapper for PlotText.  Uses default values for start, base, and
     * up to draw the text along the X axis.
     *
     * @param s string to turn into a shape.
     *
     * @return corresponding VisADGeometryArray
     */
    public static VisADGeometryArray shapeText(String s) {
        return shapeText(s, false);
    }



    /**
     * Wrapper for PlotText.  Uses default values for start, base, and
     * up to draw the text along the X axis.
     *
     * @param s string to turn into a shape.
     * @param center  center the text
     *
     * @return corresponding VisADGeometryArray
     */
    public static VisADGeometryArray shapeText(String s, boolean center) {
        return PlotText.render_label(s, TEXT_START, TEXT_BASE, TEXT_UP,
                                     center);
    }


    /**
     * Wrapper for PlotText.  Uses default values for start, base, and
     * up to draw the text along the X axis.
     *
     * @param s string to turn into a shape.
     * @param fontSize the font size
     * @param center  center the text
     *
     * @return corresponding VisADGeometryArray
     */
    public static VisADGeometryArray shapeText(String s, int fontSize,
            boolean center) {
        try {
            VisADGeometryArray result = PlotText.render_label(s, TEXT_START,
                                            TEXT_BASE, TEXT_UP, center);
            if (fontSize != 12) {
                result = setSize(result, (float) fontSize / 12.0f);
            }
            return result;
        } catch (Exception exc) {
            //Hum, maybe bad data
            VisADGeometryArray result = PlotText.render_label("BAD TEXT",
                                            TEXT_START, TEXT_BASE, TEXT_UP,
                                            center);
            if (fontSize != 12) {
                result = setSize(result, (float) fontSize / 12.0f);
            }
            return result;
        }
    }





    /**
     * Wrapper for PlotText using font.  Uses default values for start,
     * base, and up to draw the text along the X axis.
     *
     * @param s string to turn into a shape.
     * @param f font to use
     *
     * @return corresponding VisADGeometryArray
     */
    public static VisADGeometryArray shapeFont(String s, Font f) {
        return shapeFont(s, f, false);
    }

    /**
     * Wrapper for PlotText.  Uses default values for start, base, and
     * up to draw the text along the X axis.
     *
     * @param s string to turn into a shape.
     * @param f font to use
     * @param center  center the text
     *
     * @return corresponding VisADGeometryArray
     */
    public static VisADGeometryArray shapeFont(String s, Font f,
            boolean center) {
        return PlotText.render_font(s, f, TEXT_START, TEXT_BASE, TEXT_UP,
                                    center);
    }


    /*
    public static VisADGeometryArray[] makeWindBarb(float u, float v, float w)
        throws Exception
    {
        float[][]   flow_values     = {{u}, {v}, {w}};
        //float       flowscale       = 1.0f;
        float       flowscale       = 2.5f;
        float[][]   spatial_values  = {{0f},{0f},{0f}};
        byte[][]    color_values    = {{ShadowType.floatToByte(255.0f)},
                                       {ShadowType.floatToByte(255.0f)},
                                       {ShadowType.floatToByte(255.0f)}};
        boolean[][] range_select    = {{true},{true},{true}};
        boolean     south           = false;

        return Barb.staticMakeFlow(flow_values,flowscale,spatial_values,color_values,range_select,south);
    }
    */

    //-------------

    /**
     * Set the size of the shapes.  Scales the size by size.
     *
     * @param  shapes   shapes to resize
     * @param  size  scaling factor
     *
     * @return  resized shapes
     */
    public static VisADGeometryArray[] setSize(VisADGeometryArray[] shapes,
            float size) {
        for (int i = 0; i < shapes.length; i++) {
            setSize(shapes[i], size);
        }
        return shapes;
    }

    /**
     * Set the size of the shapes.  Scales the size by size.
     *
     * @param  shape   shapes to resize
     * @param  size  scaling factor
     *
     * @return  rescaled shape
     */
    public static VisADGeometryArray setSize(VisADGeometryArray shape,
                                             float size) {
        return setSize(shape, size, size, size);
    }




    /**
     * Set the size of the shapes.  Scales the size by size.
     *
     * @param  shape   shapes to resize
     * @param  x  scaling factor
     * @param  y  scaling factor
     * @param  z  scaling factor
     *
     * @return  rescaled shape
     */
    public static VisADGeometryArray setSize(VisADGeometryArray shape,
                                             float x, float y, float z) {
        if (shape.coordinates != null) {
            for (int i = 0; i < shape.coordinates.length; i += 3) {
                shape.coordinates[i + 0] *= x;
                shape.coordinates[i + 1] *= y;
                shape.coordinates[i + 2] *= z;
            }
        }
        return shape;
    }








    /**
     * Offset the shape by the amounts in off.
     *
     * @param shape  shape to offset
     * @param off    offset (x, y, z)
     * @return offset shape
     */
    public static VisADGeometryArray offset(VisADGeometryArray shape,
                                            float[] off) {
        return offset(shape, off[0], off[1], off[2]);
    }

    /**
     * Offset the shape by the amounts in off.
     *
     * @param shape  shape to offset
     * @param dx delta x
     * @param dy delta y
     * @param dz delta z
     * @return offset shape
     */
    public static VisADGeometryArray offset(VisADGeometryArray shape,
                                            float dx, float dy, float dz) {
        if ((dx == 0.0f) && (dy == 0.0f) && (dz == 0.0f)) {
            return shape;
        }
        //int ndim = shape.coordinates.length / shape.vertexCount; //should be 3?
        for (int i = 0; i < shape.coordinates.length; i += 3) {
            shape.coordinates[i]     += dx;
            shape.coordinates[i + 1] += dy;
            shape.coordinates[i + 2] += dz;
        }
        return shape;
    }




    /**
     *  Return the bounds of this shape
     *
     *  @param shape  shape to bound
     *
     *  @return Rectangle of shape coordinates
     */
    public static Rectangle2D bounds2d(VisADGeometryArray shape) {
        return bounds2d(shape, null);
    }



    /**
     *  Return the bounds of this shape
     *
     *  @param shape  shape to bound
     * @param rect The rectangle to set. If null then create a new one.
     *
     *  @return Rectangle of shape coordinates
     */
    public static Rectangle2D bounds2d(VisADGeometryArray shape,
                                       Rectangle2D rect) {
        if (rect == null) {
            rect = new Rectangle2D.Float(0.0f, 0.0f, 0.0f, 0.0f);
        } else {
            rect.setRect(0.0f, 0.0f, 0.0f, 0.0f);
        }

        if ((shape != null) && (shape.coordinates != null)) {
            for (int i = 0; i < shape.coordinates.length; i += 3) {
                if (i == 0) {
                    if (rect == null) {
                        rect = new Rectangle2D.Float();
                    }
                    rect.setRect(shape.coordinates[i],
                                 shape.coordinates[i + 1], 0.0f, 0.0f);
                } else {
                    rect.add(shape.coordinates[i], shape.coordinates[i + 1]);
                }
            }
        }
        return rect;
    }

    /**
     *  Return the bounds of array of shapes
     *
     *  @param shapes shape to bound
     *
     *  @return Rectangle of region bounded by shape coordinates
     */
    public static Rectangle2D bounds2d(VisADGeometryArray[] shapes) {
        Rectangle2D.Float rect = null;
        if (shapes != null) {
            for (int j = 0; j < shapes.length; j++) {
                VisADGeometryArray shape = shapes[j];
                if ((shape != null) && (shape.coordinates != null)) {
                    for (int i = 0; i < shape.coordinates.length; i += 3) {
                        if (rect == null) {
                            rect = new Rectangle2D.Float(
                                shape.coordinates[i],
                                shape.coordinates[i + 1], 0.0f, 0.0f);
                        } else {
                            rect.add(shape.coordinates[i],
                                     shape.coordinates[i + 1]);
                        }
                    }
                }
            }
        }
        if (rect == null) {
            rect = new Rectangle2D.Float(0.0f, 0.0f, 0.0f, 0.0f);
        }
        return rect;
    }


    //-------------------------------------------------------------------------//

    /**
     * Set the color for the shapes in the array.
     *
     * @param shapes array of shapes to color
     * @param color color to use.
     */
    public static void setColor(VisADGeometryArray[] shapes, Color color) {
        for (int i = 0; i < shapes.length; i++) {
            setColor(shapes[i], color);
        }
    }

    /**
     * Set the color for the shapes in the array.
     *
     * @param shapes array of shapes to color
     * @param RGB array of color components (R, G, B) to use.
     */
    public static void setColor(VisADGeometryArray[] shapes, float[] RGB) {
        for (int i = 0; i < shapes.length; i++) {
            setColor(shapes[i], RGB);
        }
    }

    /**
     * Set the color for the shape specified.
     *
     * @param shape shape to color
     * @param color color to use.
     */
    public static void setColor(VisADGeometryArray shape, Color color) {
        setColor(shape, (float) color.getRed() / 255.0f,
                 (float) color.getGreen() / 255.0f,
                 (float) color.getBlue() / 255.0f,
                 (float) color.getAlpha() / 255.0f);
    }

    /**
     * Set the color for the shape specified.
     *
     * @param shape shape to color
     * @param RGB array of color components (R, G, B) to use.
     */
    public static void setColor(VisADGeometryArray shape, float[] RGB) {
        if (RGB.length > 3) {
            setColor(shape, RGB[0], RGB[1], RGB[2], RGB[3]);
        } else {
            setColor(shape, RGB[0], RGB[1], RGB[2]);
        }
    }

    /**
     * Set the color for the shape specified.
     *
     * @param shape shape to color
     * @param R red color component
     * @param G green color component
     * @param B blue color component
     */
    public static void setColor(VisADGeometryArray shape, float R, float G,
                                float B) {
        setColor(shape, R, G, B, 1.f);
    }

    /**
     * Set the color for the shape specified.
     *
     * @param shape shape to color
     * @param R red color component
     * @param G green color component
     * @param B blue color component
     * @param A alpha component
     */
    public static void setColor(VisADGeometryArray shape, float R, float G,
                                float B, float A) {
        // TODO: should we always use 4 or should we stick to the previous
        // color?
        int colorLength = 4;
        int vertexCount = shape.coordinates.length / 3;
        /*
        if (shape.colors != null) {
          int c1 = shape.colors.length;
          int c2 = shape.coordinates.length;
          colorLength = (c1 == c2) ? 3 : 4;
        }
        */
        byte[] colors = new byte[vertexCount * colorLength];
        for (int ic = 0; ic < colors.length; ic += colorLength) {
            colors[ic]     = ShadowType.floatToByte(R);
            colors[ic + 1] = ShadowType.floatToByte(G);
            colors[ic + 2] = ShadowType.floatToByte(B);
            if (colorLength == 4) {
                colors[ic + 3] = ShadowType.floatToByte(A);
            }
        }
        shape.colors = colors;

    }

    // --- Color blending

    /**
     * Blend the color for the shape specified with color.
     *
     * @param shape shape to color
     * @param color color to use for blending.
     */
    public static void blendColor(VisADGeometryArray shape, Color color) {
        blendColor(shape, (float) color.getRed() / 255.0f,
                   (float) color.getGreen() / 255.0f,
                   (float) color.getBlue() / 255.0f);
    }


    /**
     * Blend the color for the shape specified with RGB components specified
     *
     * @param shape shape to color
     * @param R red color component
     * @param G green color component
     * @param B blue color component
     */
    public static void blendColor(VisADGeometryArray shape, float R, float G,
                                  float B) {
        if (shape.colors == null) {
            shape.colors = new byte[shape.coordinates.length];
        }
        for (int ic = 0; ic < shape.coordinates.length; ic += 3) {
            float f = ShadowType.byteToFloat(shape.colors[ic]);
            shape.colors[ic]     = ShadowType.floatToByte(R * f);
            f = ShadowType.byteToFloat(shape.colors[ic + 1]);
            shape.colors[ic + 1] = ShadowType.floatToByte(G * f);
            f = ShadowType.byteToFloat(shape.colors[ic + 2]);
            shape.colors[ic + 2] = ShadowType.floatToByte(B * f);
        }

    }


    //-------------------------------------------------------------------------//

    /**
     * Rescale the shapes
     *
     * @param shapes  shapes to scale
     * @param scale   scale factor
     */
    public static void reScale(VisADGeometryArray[] shapes, double scale) {

        for (int i = 0; i < shapes.length; i++) {
            reScale(shapes[i], scale);
        }
    }

    /**
     * Rescale the shape
     *
     * @param shape  shape to scale
     * @param scale  scale factor
     */
    public static void reScale(VisADGeometryArray shape, double scale) {
        if (shape.coordinates != null) {
            for (int i = 0; i < shape.coordinates.length; i++) {
                shape.coordinates[i] *= (float) scale;
            }
        }
    }




    /**
     * Rescale the shapes
     *
     * @param shapes  shapes to scale
     * @param scale  xyz  scale factor
     */
    public static void reScale(VisADGeometryArray[] shapes, double[] scale) {

        for (int i = 0; i < shapes.length; i++) {
            reScale(shapes[i], scale);
        }
    }

    /**
     * Rescale the shapes
     *
     * @param shapes  shapes to scale
     * @param scaleArray  xyz  scale factor
     * @param scale extra scale factor
     */
    public static void reScale(VisADGeometryArray[] shapes,
                               double[] scaleArray, double scale) {

        for (int i = 0; i < shapes.length; i++) {
            reScale(shapes[i], scaleArray, scale);
        }
    }




    /**
     * Do a deep clone of the given shapes array
     *
     * @param shapes  shape to clone
     *
     * @return The cloned array
     */
    public static VisADGeometryArray[] clone(VisADGeometryArray[] shapes) {
        VisADGeometryArray[] newShapes =
            new VisADGeometryArray[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            newShapes[i] = (VisADGeometryArray) shapes[i].clone();
        }
        return newShapes;
    }

    /**
     * Rescale the shape
     *
     * @param shape  shape to scale
     * @param scale  xyz scale factor
     */
    public static void reScale(VisADGeometryArray shape, double[] scale) {
        for (int i = 0; i < shape.coordinates.length; i += 3) {
            shape.coordinates[i]     *= (float) scale[0];
            shape.coordinates[i + 1] *= (float) scale[1];
            shape.coordinates[i + 2] *= (float) scale[2];
        }
    }

    /**
     * Rescale the shape
     *
     * @param shape  shape to scale
     * @param scaleArray  xyz scale factor
     * @param scale  scale factor
     */
    public static void reScale(VisADGeometryArray shape, double[] scaleArray,
                               double scale) {
        for (int i = 0; i < shape.coordinates.length; i += 3) {
            shape.coordinates[i]     *= (float) scaleArray[0] * scale;
            shape.coordinates[i + 1] *= (float) scaleArray[1] * scale;
            shape.coordinates[i + 2] *= (float) scaleArray[2] * scale;
        }
    }


    /** Local state for the rotate method */
    private static Matrix3f rotateZMatrix = new Matrix3f();

    /** Local state for the rotate method */
    private static Matrix3f rotateXMatrix = new Matrix3f();

    /** Local state for the rotate method */
    private static Matrix3f rotateYMatrix = new Matrix3f();

    /** Local state for the rotate method */
    private static float[] rotateArray = new float[3];

    /** Local state for the rotate method */
    private static Vector3f rotateVector = new Vector3f();


    /**
     * Rotate the given shape about the z axis the given angle (in radians).
     *
     * @param shape The shape to rotate
     * @param angle Radians
     */
    public static void rotate(VisADGeometryArray shape, float angle) {
        rotateZ(shape, angle);
    }


    /**
     * Rotate the given shape about the z axis the given angle (in radians).
     *
     * @param shape The shape to rotate
     * @param angle Radians
     */
    public static void rotateZ(VisADGeometryArray shape, float angle) {
        synchronized (rotateZMatrix) {
            rotateZMatrix.rotZ(angle);
            for (int i = 0; i < shape.coordinates.length; i += 3) {
                rotateVector.set(shape.coordinates[i],
                                 shape.coordinates[i + 1],
                                 shape.coordinates[i + 2]);
                rotateZMatrix.transform(rotateVector);
                rotateVector.get(rotateArray);
                shape.coordinates[i]     = rotateArray[0];
                shape.coordinates[i + 1] = rotateArray[1];
            }
        }

    }


    /**
     * Rotate a shape
     *
     * @param shape the shape
     * @param transform  the 3D transform
     */
    public static void rotate(VisADGeometryArray shape,
                              Transform3D transform) {
        rotate(shape, transform, 0f, 0f, 0f);
    }


    /**
     * Rotate a shape
     *
     * @param shape  the shape
     * @param transform  the transform
     * @param deltax delta x
     * @param deltay delta y
     * @param deltaz delta z
     */
    public static void rotate(VisADGeometryArray shape,
                              Transform3D transform, float deltax,
                              float deltay, float deltaz) {
        Point3f point3f = new Point3f();
        for (int i = 0; i < shape.coordinates.length; i += 3) {
            point3f.x = shape.coordinates[i + 0] + deltax;
            point3f.y = shape.coordinates[i + 1] + deltay;
            point3f.z = shape.coordinates[i + 2] + deltaz;
            transform.transform(point3f);
            shape.coordinates[i + 0] = point3f.x - deltax;
            shape.coordinates[i + 1] = point3f.y - deltay;
            shape.coordinates[i + 2] = point3f.z - deltaz;
        }
    }



    /**
     * Rotate the given shape about the x axis the given angle (in radians).
     *
     * @param shape The shape to rotate
     * @param angle Radians
     */
    public static void rotateX(VisADGeometryArray shape, float angle) {
        synchronized (rotateXMatrix) {
            rotateXMatrix.rotX(angle);
            for (int i = 0; i < shape.coordinates.length; i += 3) {
                rotateVector.set(shape.coordinates[i],
                                 shape.coordinates[i + 1],
                                 shape.coordinates[i + 2]);
                rotateXMatrix.transform(rotateVector);
                rotateVector.get(rotateArray);
                shape.coordinates[i + 1] = rotateArray[1];
                shape.coordinates[i + 2] = rotateArray[2];
            }
        }
    }


    /**
     * Rotate the given shape about the y axis the given angle (in radians).
     *
     * @param shape The shape to rotate
     * @param angle Radians
     */
    public static void rotateY(VisADGeometryArray shape, float angle) {
        synchronized (rotateYMatrix) {
            rotateYMatrix.rotY(angle);
            for (int i = 0; i < shape.coordinates.length; i += 3) {
                rotateVector.set(shape.coordinates[i],
                                 shape.coordinates[i + 1],
                                 shape.coordinates[i + 2]);
                rotateYMatrix.transform(rotateVector);
                rotateVector.get(rotateArray);
                shape.coordinates[i]     = rotateArray[0];
                shape.coordinates[i + 2] = rotateArray[2];
            }
        }

    }





    /*
  //-------------------------------------------------------------------------//


    public static VisADLineArray makeWindVector(float speed, float direction) {
        return makeWindVector(speed,direction,0.0f);
    }

  public static VisADLineArray makeWindVector(float speed,
                                              float direction,
                                              float zoff) {

    // angle is direction FROM which the wind is blowing
    // degrees clockwise from North

    VisADLineArray shape = new VisADLineArray();
    Vector3f v;
    float[] t = new float[3];
    float scale = 0.5f; // ADD get from config

    direction = (direction>270.0f ? direction-360.0f : direction);
    float angle = (float) ((270.0 - direction)*Math.PI/180.0);

    shape.coordinates = new float[] {1.0f, 0.0f, 0.0f,  0.0f, 0.0f, 0.0f,
                                     1.0f, 0.0f, 0.0f,  0.8f, 0.1f, 0.0f,
                                     1.0f, 0.0f, 0.0f,  0.8f,-0.1f, 0.0f};
    shape.vertexCount = shape.coordinates.length / 3;

    // Apply rotation and length
    Matrix3f matrix = new Matrix3f();
    matrix.rotZ(angle);
    matrix.mul(scale * (float) speed);

    for(int i=0; i<shape.coordinates.length; i+=3) {
      v = new Vector3f( shape.coordinates[i],
                        shape.coordinates[i+1],
                        shape.coordinates[i+2] );
      matrix.transform(v);
      v.get(t);

      shape.coordinates[i] = t[0];
      shape.coordinates[i+1] = t[1];
      shape.coordinates[i+2] = t[2] + zoff;

    }

    return shape;

  }

  //-------------------------------------------------------------------------//


    public static float[] windVector(float speed, float direction) {
        float[] wind_vector = new float[2];
        double deg2rad = Math.PI/180.0;
        wind_vector[0] = (float) (-1 * speed * Math.sin(direction * deg2rad));
        wind_vector[1] = (float) (-1 * speed * Math.cos(direction * deg2rad));
        return wind_vector;
    }
    */


}
