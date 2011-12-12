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


import ucar.visad.Util;


import visad.*;

import visad.bom.*;

import visad.java3d.*;

import visad.util.*;

import java.awt.Color;

import java.awt.event.*;

import java.rmi.*;

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


/**
 * FrontDrawer is the VisAD class for manipulation of fronts. Is was originally
 * taken from the class visad.bom.FrontDrawer
 */
public class FrontDrawer extends DisplayableData {

    /** static instance counter */
    private static int count = 0;

    /** identifier for cold front */
    public static final String TYPE_COLD_FRONT = "COLD_FRONT";

    /** identifier for warm front */
    public static final String TYPE_WARM_FRONT = "WARM_FRONT";

    /** identifier for occluded front */
    public static final String TYPE_OCCLUDED_FRONT = "OCCLUDED_FRONT";

    /** identifier for stationary front */
    public static final String TYPE_STATIONARY_FRONT = "STATIONARY_FRONT";

    /** identifier for convergence */
    public static final String TYPE_CONVERGENCE = "CONVERGENCE";

    /** identifier for frontogenesis */
    public static final String TYPE_FRONTOGENESIS = "FRONTOGENESIS";

    /** identifier for frontolysis */
    public static final String TYPE_FRONTOLYSIS = "FRONTOLYSIS";

    /** identifier for upper cold front */
    public static final String TYPE_UPPER_COLD_FRONT = "UPPER_COLD_FRONT";

    /** identifier for upper warm front */
    public static final String TYPE_UPPER_WARM_FRONT = "UPPER_WARM_FRONT";

    /** identifier for trough */
    public static final String TYPE_TROUGH = "TROUGH";

    /** identifier for ridge */
    public static final String TYPE_RIDGE = "RIDGE";

    /** identifier for moisture */
    public static final String TYPE_MOISTURE = "MOISTURE";

    /** identifier for low level jet */
    public static final String TYPE_LOW_LEVEL_JET = "LOW_LEVEL_JET";

    /** identifier for upper level jet */
    public static final String TYPE_UPPER_LEVEL_JET = "UPPER_LEVEL_JET";

    /** identifier for dry line */
    public static final String TYPE_DRY_LINE = "DRY_LINE";

    /** identifier for total-totals */
    public static final String TYPE_TOTAL_TOTALS = "TOTAL_TOTALS";

    /** identifier for lifted index */
    public static final String TYPE_LIFTED_INDEX = "LIFTED_INDEX";

    /** identifier for isotherms */
    public static final String TYPE_ISOTHERMS = "ISOTHERMS";

    /** identifier for thickness ridge */
    public static final String TYPE_THICKNESS_RIDGE = "THICKNESS_RIDGE";

    /** identifier for lower level thermal trough */
    public static final String TYPE_LOWER_THERMAL_TROUGH =
        "LOWER_THERMAL_TROUGH";

    /** identifier for upper  level thermal trough */
    public static final String TYPE_UPPER_THERMAL_TROUGH =
        "UPPER_THERMAL_TROUGH";

    /** identifier for uneven low level jet */
    public static final String TYPE_UNEVEN_LOW_LEVEL_JET =
        "UNEVEN_LOW_LEVEL_JET";


    /** base type to use */
    public static final String[] BASETYPES = { TYPE_COLD_FRONT,
            TYPE_WARM_FRONT, TYPE_OCCLUDED_FRONT, TYPE_STATIONARY_FRONT,
            TYPE_TROUGH };


    /** all types */
    public static final String[] TYPES = {
        TYPE_COLD_FRONT, TYPE_WARM_FRONT, TYPE_OCCLUDED_FRONT,
        TYPE_STATIONARY_FRONT, TYPE_CONVERGENCE, TYPE_FRONTOGENESIS,
        TYPE_FRONTOLYSIS, TYPE_UPPER_COLD_FRONT, TYPE_UPPER_WARM_FRONT,
        TYPE_TROUGH, TYPE_RIDGE, TYPE_MOISTURE, TYPE_LOW_LEVEL_JET,
        TYPE_UPPER_LEVEL_JET, TYPE_DRY_LINE, TYPE_TOTAL_TOTALS,
        TYPE_LIFTED_INDEX, TYPE_ISOTHERMS, TYPE_THICKNESS_RIDGE,
        TYPE_LOWER_THERMAL_TROUGH, TYPE_UPPER_THERMAL_TROUGH,
        TYPE_UNEVEN_LOW_LEVEL_JET
    };


    /** labels for types */
    public static final String[] LABELS = {
        "Cold front", "Warm front", "Occluded front", "Stationary front",
        "Convergence", "Frontogenesis", "Frontolysis", "Upper cold front",
        "Upper warm front", "Trough", "Ridge", "Moisture", "Low level jet",
        "Upper level jet", "Dry line", "Total totals", "Lifted index",
        "Isotherms", "Thickness ridge", "Lower thermal trough",
        "Upper thermal trough", "Uneven low level jet"
    };



    /** spacing between repeating segments */
    private static final float[] rsegmentarray = {
        0.2f,   // COLD FRONT = 0
        0.2f,   // WARM FRONT = 1
        0.15f,  // OCCLUDED FRONT = 2
        0.2f,   // STATIONARY FRONT = 3
        0.2f,   // CONVERGENCE = 4
        0.2f,   // FRONTOGENESIS = 5
        0.2f,   // FRONTOLYSIS = 6
        0.2f,   // UPPER COLD FRONT = 7
        0.2f,   // UPPER WARM FRONT = 8
        0.05f,  // TROUGH = 9
        0.1f,   // RIDGE = 10
        0.05f,  // MOISTURE = 11
        0.2f,   // LOW_LEVEL_JET = 12
        0.2f,   // UPPER_LEVEL_JET = 13
        0.1f,   // DRY_LINE = 14
        0.05f,  // TOTAL_TOTALS = 15
        0.1f,   // LIFTED_INDEX = 16
        0.15f,  // ISOTHERMS = 17
        0.1f,   // THICKNESS_RIDGE = 18
        0.05f,  // LOWER_THERMAL_TROUGH = 19
        0.1f,   // UPPER_THERMAL_TROUGH = 20
        0.1f    // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** lengths of first segment in graphics coordinates */
    private static final float[] fsegmentarray = {
        0.2f,   // COLD FRONT = 0
        0.2f,   // WARM FRONT = 1
        0.2f,   // OCCLUDED FRONT = 2
        0.2f,   // STATIONARY FRONT = 3
        0.2f,   // CONVERGENCE = 4
        0.2f,   // FRONTOGENESIS = 5
        0.2f,   // FRONTOLYSIS = 6
        0.2f,   // UPPER COLD FRONT = 7
        0.2f,   // UPPER WARM FRONT = 8
        0.05f,  // TROUGH = 9
        0.1f,   // RIDGE = 10
        0.05f,  // MOISTURE = 11
        0.2f,   // LOW_LEVEL_JET = 12
        0.2f,   // UPPER_LEVEL_JET = 13
        0.1f,   // DRY_LINE = 14
        0.05f,  // TOTAL_TOTALS = 15
        0.1f,   // LIFTED_INDEX = 16
        0.15f,  // ISOTHERMS = 17
        0.1f,   // THICKNESS_RIDGE = 18
        0.05f,  // LOWER_THERMAL_TROUGH = 19
        0.1f,   // UPPER_THERMAL_TROUGH = 20
        0.2f    // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** shape coordinates */
    private static final float[][][][] rshapesarray = {

        // COLD_FRONT =0
        {
            {
                {
                    0.0f, 0.025f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f,
                    0.05f, 0.025f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f,
                    0.01f, 0.04f, 0.01f
                }
            }
        },
        // WARM_FRONT = 1
        {
            {
                {
                    0.0f, 0.035f, 0.07f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f,
                    0.07f, 0.0525f, 0.035f, 0.0175f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f,
                    0.01f, 0.03f, 0.037f, 0.03f, 0.01f
                }
            }
        },
        // OCCLUDED_FRONT = 2
        {
            {
                {
                    0.0f, 0.025f, 0.05f, 0.07f, 0.105f, 0.14f, 0.17f, 0.2f,
                    0.2f, 0.17f, 0.14f, 0.1225f, 0.105f, 0.0875f, 0.07f,
                    0.05f, 0.025f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f,
                    0.01f, 0.01f, 0.03f, 0.037f, 0.03f, 0.01f, 0.01f, 0.04f,
                    0.01f
                }
            }
        },
        // STATIONARY_FRONT = 3
        {
            {
                {
                    0.09f, 0.11f, 0.1275f, 0.145f, 0.1625f, 0.18f, 0.2f, 0.2f,
                    0.1775f, 0.155f, 0.1175f, 0.09f
                }, {
                    0.0f, 0.0f, -0.02f, -0.027f, -0.02f, 0.0f, 0.0f, 0.01f,
                    0.01f, 0.01f, 0.01f, 0.01f
                }
            }, {
                {
                    0.0f, 0.02f, 0.045f, 0.07f, 0.09f, 0.09f, 0.07f, 0.045f,
                    0.02f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.04f, 0.01f,
                    0.01f
                }
            }
        },
        // CONVERGENCE = 4
        {
            {
                {
                    0.0f, 0.03f, 0.035f, 0.01f, 0.05f, 0.1f, 0.15f, 0.2f,
                    0.2f, 0.15f, 0.11f, 0.135f, 0.13f, 0.1f, 0.05f, 0.0f
                }, {
                    0.01f, 0.04f, 0.035f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
                    0.0f, 0.0f, 0.0f, -0.025f, -0.03f, 0.0f, 0.0f, 0.0f
                }
            }
        },
        // FRONTOGENESIS = 5
        {
            {
                {
                    0.0f, 0.035f, 0.07f, 0.1f, 0.15f, 0.15f, 0.1f, 0.0875f,
                    0.075f, 0.0625f, 0.05f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.025f,
                    0.035f, 0.025f, 0.01f, 0.01f
                }
            }, {
                { 0.16f, 0.19f, 0.19f, 0.16f },
                { -0.005f, -0.005f, 0.015f, 0.015f }
            }
        },
        // FRONTOLYSIS = 6
        {
            {
                {
                    0.0f, 0.035f, 0.07f, 0.1f, 0.15f, 0.15f, 0.1f, 0.0875f,
                    0.075f, 0.0625f, 0.05f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.025f,
                    0.035f, 0.025f, 0.01f, 0.01f
                }
            }, {
                {
                    0.16f, 0.17f, 0.17f, 0.18f, 0.18f, 0.19f, 0.19f, 0.18f,
                    0.18f, 0.17f, 0.17f, 0.16f
                }, {
                    0.0f, 0.0f, -0.01f, -0.01f, 0.0f, 0.0f, 0.01f, 0.01f,
                    0.02f, 0.02f, 0.01f, 0.01f
                }
            }
        },
        // UPPER_COLD_FRONT = 7
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f, 0.05f,
                    0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f, 0.01f,
                    0.01f
                }
            }, {
                {
                    0.0f, 0.03f, 0.06f, 0.05f, 0.03f, 0.01f
                }, {
                    0.01f, 0.04f, 0.01f, 0.01f, 0.03f, 0.01f
                }
            }
        },
        // UPPER_WARM_FRONT = 8
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f, 0.05f,
                    0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f, 0.01f,
                    0.01f
                }
            }, {
                {
                    0.0f, 0.015f, 0.03f, 0.045f, 0.06f, 0.05f, 0.04f, 0.03f,
                    0.02f, 0.01f
                }, {
                    0.01f, 0.03f, 0.037f, 0.03f, 0.01f, 0.01f, 0.023f, 0.027f,
                    0.023f, 0.01f
                }
            }
        },
        // TROUGH = 9
        {
            {
                { 0.0f, 0.035f, 0.035f, 0.0f }, { 0.0f, 0.0f, 0.01f, 0.01f }
            }
        },
        // RIDGE = 10
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.1f, 0.05f, 0.0f
                }, {
                    0.04f, -0.06f, 0.04f, 0.06f, -0.04f, 0.06f
                }
            }
        },
        // MOISTURE = 11
        {
            {
                {
                    0.0f, 0.0f, 0.01f, 0.01f, 0.05f, 0.05f, 0.0f
                }, {
                    0.01f, 0.05f, 0.05f, 0.01f, 0.01f, 0.0f, 0.0f
                }
            }
        },
        // LOW_LEVEL_JET = 12
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f, 0.05f,
                    0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f, 0.01f,
                    0.01f
                }
            }
        },
        // UPPER_LEVEL_JET = 13
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.15f, 0.1f, 0.05f,
                    0.0f
                }, {
                    -0.01f, -0.01f, -0.01f, -0.01f, -0.01f, 0.02f, 0.02f,
                    0.02f, 0.02f, 0.02f
                }
            }
        },
        // DRY_LINE = 14
        {
            {
                { 0.0f, 0.05f, 0.05f, 0.0f }, { 0.0f, 0.0f, 0.01f, 0.01f }
            }, {
                { 0.06f, 0.09f, 0.09f, 0.06f },
                { -0.005f, -0.005f, 0.015f, 0.015f }
            }
        },
        // TOTAL_TOTALS = 15
        {
            {
                { 0.0f, 0.035f, 0.035f, 0.0f }, { 0.0f, 0.0f, 0.01f, 0.01f }
            }
        },
        // LIFTED_INDEX = 16
        {
            {
                { 0.0f, 0.05f, 0.05f, 0.0f }, { 0.0f, 0.0f, 0.01f, 0.01f }
            }, {
                { 0.06f, 0.09f, 0.09f, 0.06f },
                { -0.005f, -0.005f, 0.015f, 0.015f }
            }
        },
        // ISOTHERMS = 17
        {
            {
                {
                    0.0f, 0.0f, 0.04f, 0.08f, 0.08f, 0.04f, 0.0f, 0.0f, 0.02f,
                    0.02f, 0.06f, 0.06f, 0.02f, 0.02f
                }, {
                    0.0f, -0.02f, -0.02f, -0.02f, 0.02f, 0.02f, 0.02f, 0.0f,
                    0.0f, 0.01f, 0.01f, -0.01f, -0.01f, 0.0f
                }
            }
        },
        // THICKNESS_RIDGE = 18
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.1f, 0.05f, 0.0f
                }, {
                    0.01f, -0.06f, 0.01f, 0.06f, -0.01f, 0.06f
                }
            }
        },
        // LOWER_THERMAL_TROUGH = 19
        {
            {
                { 0.0f, 0.045f, 0.045f, 0.0f },
                { -0.01f, -0.01f, 0.02f, 0.02f }
            }
        },
        // UPPER_THERMAL_TROUGH = 20
        {
            {
                { 0.0f, 0.04f, 0.02f }, { 0.0f, 0.0f, 0.04f }
            }
        },
        // UNEVEN_LOW_LEVEL_JET = 21
        {
            {
                {
                    0.0f, 0.05f, 0.1f, 0.1f, 0.05f, 0.0f
                }, {
                    0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f
                }
            }
        },
    };

    /** red values for repeating shapes */
    private static final float[][] rredarray = {
        { 0.0f },        // COLD FRONT = 0
        { 1.0f },        // WARM FRONT = 1
        { 1.0f },        // OCCLUDED FRONT = 2
        { 1.0f, 0.0f },  // STATIONARY FRONT = 3
        { 1.0f },        // CONVERGENCE = 4
        { 1.0f, 1.0f },  // FRONTOGENESIS = 5
        { 1.0f, 1.0f },  // FRONTOLYSIS = 6
        { 1.0f, 1.0f },  // UPPER COLD FRONT = 7
        { 1.0f, 1.0f },  // UPPER WARM FRONT = 8
        { 0.5f },        // TROUGH = 9
        { 0.5f },        // RIDGE = 10
        { 1.0f },        // MOISTURE = 11
        { 0.5f },        // LOW_LEVEL_JET = 12
        { 0.5f },        // UPPER_LEVEL_JET = 13
        { 0.5f, 0.5f },  // DRY_LINE = 14
        { 1.0f },        // TOTAL_TOTALS = 15
        { 1.0f, 1.0f },  // LIFTED_INDEX = 16
        { 1.0f },        // ISOTHERMS = 17
        { 1.0f },        // THICKNESS_RIDGE = 18
        { 1.0f },        // LOWER_THERMAL_TROUGH = 19
        { 1.0f },        // UPPER_THERMAL_TROUGH = 20
        { 0.5f }         // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** green values for repeating shapes */
    private static final float[][] rgreenarray = {
        { 0.0f },        // COLD FRONT = 0
        { 0.0f },        // WARM FRONT = 1
        { 0.0f },        // OCCLUDED FRONT = 2
        { 0.0f, 0.0f },  // STATIONARY FRONT = 3
        { 1.0f },        // CONVERGENCE = 4
        { 1.0f, 1.0f },  // FRONTOGENESIS = 5
        { 1.0f, 1.0f },  // FRONTOLYSIS = 6
        { 1.0f, 1.0f },  // UPPER COLD FRONT = 7
        { 1.0f, 1.0f },  // UPPER WARM FRONT = 8
        { 0.3f },        // TROUGH = 9
        { 0.3f },        // RIDGE = 10
        { 1.0f },        // MOISTURE = 11
        { 0.5f },        // LOW_LEVEL_JET = 12
        { 0.5f },        // UPPER_LEVEL_JET = 13
        { 0.3f, 0.3f },  // DRY_LINE = 14
        { 1.0f },        // TOTAL_TOTALS = 15
        { 1.0f, 1.0f },  // LIFTED_INDEX = 16
        { 1.0f },        // ISOTHERMS = 17
        { 1.0f },        // THICKNESS_RIDGE = 18
        { 1.0f },        // LOWER_THERMAL_TROUGH = 19
        { 1.0f },        // UPPER_THERMAL_TROUGH = 20
        { 0.5f }         // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** blue values for shapes */
    private static final float[][] rbluearray = {
        { 1.0f },        // COLD FRONT = 0
        { 0.0f },        // WARM FRONT = 1
        { 1.0f },        // OCCLUDED FRONT = 2
        { 0.0f, 1.0f },  // STATIONARY FRONT = 3
        { 1.0f },        // CONVERGENCE = 4
        { 1.0f, 1.0f },  // FRONTOGENESIS = 5
        { 1.0f, 1.0f },  // FRONTOLYSIS = 6
        { 1.0f, 1.0f },  // UPPER COLD FRONT = 7
        { 1.0f, 1.0f },  // UPPER WARM FRONT = 8
        { 0.0f },        // TROUGH = 9
        { 0.0f },        // RIDGE = 10
        { 1.0f },        // MOISTURE = 11
        { 1.0f },        // LOW_LEVEL_JET = 12
        { 1.0f },        // UPPER_LEVEL_JET = 13
        { 0.0f, 0.0f },  // DRY_LINE = 14
        { 1.0f },        // TOTAL_TOTALS = 15
        { 1.0f, 1.0f },  // LIFTED_INDEX = 16
        { 1.0f },        // ISOTHERMS = 17
        { 1.0f },        // THICKNESS_RIDGE = 18
        { 1.0f },        // LOWER_THERMAL_TROUGH = 19
        { 1.0f },        // UPPER_THERMAL_TROUGH = 20
        { 1.0f }         // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** first shape array */
    private static final float[][][][] fshapesarray = {
        null,  // COLD FRONT = 0
        null,  // WARM FRONT = 1
        null,  // OCCLUDED FRONT = 2
        null,  // STATIONARY FRONT = 3
        null,  // CONVERGENCE = 4
        null,  // FRONTOGENESIS = 5
        null,  // FRONTOLYSIS = 6
        null,  // UPPER COLD FRONT = 7
        null,  // UPPER WARM FRONT = 8
        null,  // TROUGH = 9
        null,  // RIDGE = 10
        null,  // MOISTURE = 11
        // LOW_LEVEL_JET = 12
        {
            {
                {
                    0.0f, 0.07f, 0.075f, 0.01f, 0.05f, 0.1f, 0.15f, 0.2f,
                    0.2f, 0.15f, 0.1f, 0.05f, 0.01f, 0.075f, 0.07f, 0.0f
                }, {
                    0.0f, -0.07f, -0.065f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.075f, 0.08f, 0.01f
                }
            }
        },
        // UPPER_LEVEL_JET = 13
        {
            {
                {
                    0.0f, 0.06f, 0.077f, 0.04f, 0.05f, 0.1f, 0.15f, 0.2f,
                    0.2f, 0.15f, 0.1f, 0.05f, 0.04f, 0.077f, 0.06f, 0.0f
                }, {
                    -0.001f, -0.06f, -0.04f, -0.01f, -0.01f, -0.01f, -0.01f,
                    -0.01f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.05f, 0.07f,
                    0.02f
                }
            }
        }, null,  //
           null,  //
           null,  //
           null,  //
           null,  //
           null,  //
           null,  //
        // UNEVEN_LOW_LEVEL_JET = 21
        {
            {
                {
                    0.0f, 0.07f, 0.075f, 0.01f, 0.05f, 0.1f, 0.15f, 0.2f,
                    0.2f, 0.15f, 0.1f, 0.05f, 0.01f, 0.075f, 0.07f, 0.0f
                }, {
                    0.0f, -0.07f, -0.065f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.075f, 0.08f, 0.01f
                }
            }
        }
    };

    /** first segment reds */
    private static final float[][] fredarray = {
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 0.5f },  // LOW_LEVEL_JET = 12
        { 0.5f },  // UPPER_LEVEL_JET = 13
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 0.5f }   // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** first segment greens */
    private static final float[][] fgreenarray = {
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 0.5f },  // LOW_LEVEL_JET = 12
        { 0.5f },  // UPPER_LEVEL_JET = 13
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 0.5f }   // UNEVEN_LOW_LEVEL_JET = 21
    };

    /** first segment blues */
    private static final float[][] fbluearray = {
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 1.0f },  // LOW_LEVEL_JET = 12
        { 1.0f },  // UPPER_LEVEL_JET = 13
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        null,      //
        { 1.0f }   // UNEVEN_LOW_LEVEL_JET = 21
    };



    /** latitude index */
    private static final int LAT_INDEX = 0;

    /** longitude index */
    private static final int LON_INDEX = 1;

    /** the curve */
    private float[][] curve;

    /** times for curves */
    private List curveTimes;

    /** clip delta */
    private static final float CLIP_DELTA = 0.001f;

    /** debug flag */
    private boolean debug = true;

    /** zoom factor */
    private float zoom = 1.0f;

    /** last zoom factor */
    private float last_zoom = 1.0f;

    /** latitude map */
    private ScalarMap latMap = null;

    /** longitude map */
    private ScalarMap lonMap = null;

    /** curve type */
    private SetType curve_type = null;  // Set(Latitude, Longitude)

    /** fronts type */
    private FunctionType fronts_type = null;

    /** single front type */
    private FunctionType front_type = null;

    /** inner front type */
    private FunctionType front_inner = null;

    /** front index */
    private RealType front_index = null;

    /** front red type */
    private RealType front_red = null;

    /** front green type */
    private RealType front_green = null;

    /** front blue type */
    private RealType front_blue = null;

    /** number of first shapes */
    private int nfshapes = -1;

    /** shapes for first segment of front */
    private float[][][] first_shapes = null;

    /** first shape triangles */
    private int[][][] first_tris = null;

    /** first reds */
    private float[] first_red = null;

    /** first shape greens */
    private float[] first_green = null;

    /** first shape blues */
    private float[] first_blue = null;

    /** number of repeating shapes */
    private int nrshapes = -1;

    /** shapes for repeating segments of front, after first */
    private float[][][] repeat_shapes = null;

    /** repeat shapes  triangles */
    private int[][][] repeat_tris = null;

    /** repeat shapes reds */
    private float[] repeat_red = null;

    /** repeat shapes greens */
    private float[] repeat_green = null;

    /** repeat shapes blues */
    private float[] repeat_blue = null;

    /** length of first segment in graphics coordinates */
    private float fsegment_length;

    /** length of each repeating segment in graphics coordinates */
    private float rsegment_length;

    /** number of intervals in curve for first segment */
    private int fprofile_length = -1;

    /** number of intervals in curve for each repeating segment */
    private int rprofile_length = -1;

    /** size of filter window for smoothing curve */
    private int filter_window = 1;

    /** true to use colors */
    private boolean doColors = true;

    /** true to flip the flip (default is Southern Hemisphere orientation */
    private boolean flipTheFlip = true;

    /** default base scale */
    public double baseScale = 15.0f;

    /**
     * Create a front drawer with the front type
     *
     * @param fw  window id
     * @param frontType  front type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FrontDrawer(int fw, String frontType)
            throws VisADException, RemoteException {
        this(fw, frontType, true);
    }


    /**
     * Create a front drawer with the front type index
     *
     * @param fw  window
     * @param frontType front type index
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FrontDrawer(int fw, int frontType)
            throws VisADException, RemoteException {
        this(fw, frontType, true);
    }

    /**
     * Create a front drawer with the front type
     *
     * @param fw  window id
     * @param frontType  front type
     * @param doColors  true to use colors
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FrontDrawer(int fw, String frontType, boolean doColors)
            throws VisADException, RemoteException {
        this(fw, getIndex(frontType), doColors);
    }



    /**
     * Create a front drawer with the front type index
     *
     * @param fw  window id
     * @param frontType  front type
     * @param doColors  true to use colors
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FrontDrawer(int fw, int frontType, boolean doColors)
            throws VisADException, RemoteException {
        super("FrontDrawer");
        this.doColors = doColors;
        filter_window = fw;
        setFrontType(frontType);
        initColorMaps();
    }

    /**
     * Create a front drawer from another instance
     *
     * @param that  the other instance
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FrontDrawer(FrontDrawer that)
            throws RemoteException, VisADException {
        super("FrontDrawer");
        this.doColors = that.doColors;
    }


    /**
     * Sets the color of the lines for this Displayable.
     *
     * @param   color     color for the line.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color color) throws VisADException, RemoteException {
        synchronized (this) {
            addConstantMaps(new ConstantMap[] {
                new ConstantMap(color.getRed() / 255., Display.Red),
                new ConstantMap(color.getGreen() / 255., Display.Green),
                new ConstantMap(color.getBlue() / 255., Display.Blue),
                new ConstantMap(color.getAlpha() / 255., Display.Alpha) });
        }
    }


    /**
     * Set the line width
     *
     * @param lineWidth  the line width
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setLineWidth(float lineWidth)
            throws VisADException, RemoteException {
        synchronized (this) {
            addConstantMap(new ConstantMap(lineWidth, Display.LineWidth));
        }
    }

    /**
     * Clone for display
     *
     * @return  return a clone
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new FrontDrawer(this);
    }

    /**
     * Set the scale
     *
     * @param baseScale  the base scale
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setScale(double baseScale)
            throws VisADException, RemoteException {
        this.baseScale = baseScale;
        if (getDisplayMaster() == null) {
            return;
        }
        ProjectionControl pcontrol =
            getDisplayMaster().getDisplay().getProjectionControl();
        if (pcontrol == null) {
            return;
        }
        double[] matrix = pcontrol.getMatrix();
        double[] rot    = new double[3];
        double[] scale  = new double[1];
        double[] trans  = new double[3];
        MouseBehaviorJ3D.unmake_matrix(rot, scale, trans, matrix);
        // System.err.println("scale:" + scale[0]);
        //It seems like dividing by 15 gives us good results
        zoom = (float) scale[0] / (float) baseScale;
    }

    /**
     * Get the index for the front name
     *
     * @param name the front name
     *
     * @return  the index or -1 if not found
     */
    private static int getIndex(String name) {
        if (name == null) {
            return -1;
        }
        name = name.toUpperCase();
        for (int i = 0; i < TYPES.length; i++) {
            if (name.equals(TYPES[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the label for the type
     *
     * @param type  the type
     *
     * @return  the label
     */
    public static String getLabel(String type) {
        int index = getIndex(type);
        if (index >= 0) {
            return LABELS[index];
        }
        return null;
    }



    /**
     * Set the front type
     *
     * @param type the type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setFrontType(String type)
            throws VisADException, RemoteException {
        setFrontType(getIndex(type));
    }

    /**
     * Set the front type
     *
     * @param type front type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setFrontType(int type)
            throws VisADException, RemoteException {
        setFrontState(fsegmentarray[type], rsegmentarray[type],
                      fshapesarray[type], fredarray[type], fgreenarray[type],
                      fbluearray[type], rshapesarray[type], rredarray[type],
                      rgreenarray[type], rbluearray[type]);

    }


    /**
     * Set the front state
     *
     * @param fsegment length of first segment in graphics coord
     * @param rsegment length of repeating segment in graphics coord
     * @param fshapes  shapes of the front
     * @param fred     first reds
     * @param fgreen   first greens
     * @param fblue    first blues
     * @param rshapes  repeating shapes
     * @param rred     repeating reds
     * @param rgreen   repeating greens
     * @param rblue    repeating blues
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setFrontState(float fsegment, float rsegment,
                               float[][][] fshapes, float[] fred,
                               float[] fgreen, float[] fblue,
                               float[][][] rshapes, float[] rred,
                               float[] rgreen, float[] rblue)
            throws VisADException, RemoteException {
        fsegment_length = fsegment;
        rsegment_length = rsegment;
        nrshapes        = rshapes.length;
        for (int i = 0; i < nrshapes; i++) {
            if ((rshapes[i] == null) || (rshapes[i].length != 2)
                    || (rshapes[i][0] == null) || (rshapes[i][1] == null)
                    || (rshapes[i][0].length != rshapes[i][1].length)) {
                throw new VisADException("bad rshapes[" + i + "]");
            }
        }
        if ((rred == null) || (rred.length != nrshapes) || (rgreen == null)
                || (rgreen.length != nrshapes) || (rblue == null)
                || (rblue.length != nrshapes)) {
            throw new VisADException("bad rcolors");
        }
        repeat_tris = new int[nrshapes][][];
        for (int i = 0; i < nrshapes; i++) {
            repeat_tris[i] = DelaunayCustom.fill(rshapes[i]);
        }
        repeat_shapes = new float[nrshapes][2][];
        int rlen = 0;
        for (int i = 0; i < nrshapes; i++) {
            int n = rshapes[i][0].length;
            rlen                += n;
            repeat_shapes[i][0] = new float[n];
            repeat_shapes[i][1] = new float[n];
            System.arraycopy(rshapes[i][0], 0, repeat_shapes[i][0], 0, n);
            System.arraycopy(rshapes[i][1], 0, repeat_shapes[i][1], 0, n);
        }
        rprofile_length = rlen;
        repeat_red      = new float[nrshapes];
        repeat_green    = new float[nrshapes];
        repeat_blue     = new float[nrshapes];
        System.arraycopy(rred, 0, repeat_red, 0, nrshapes);
        System.arraycopy(rgreen, 0, repeat_green, 0, nrshapes);
        System.arraycopy(rblue, 0, repeat_blue, 0, nrshapes);

        if (fshapes == null) {
            // if no different first shapes, just use repeat shapes
            nfshapes     = nrshapes;
            first_tris   = repeat_tris;
            first_shapes = repeat_shapes;
            first_red    = repeat_red;
            first_green  = repeat_green;
            first_blue   = repeat_blue;
        } else {
            nfshapes = fshapes.length;
            for (int i = 0; i < nfshapes; i++) {
                if ((fshapes[i] == null) || (fshapes[i].length != 2)
                        || (fshapes[i][0] == null) || (fshapes[i][1] == null)
                        || (fshapes[i][0].length != fshapes[i][1].length)) {
                    throw new VisADException("bad fshapes[" + i + "]");
                }
            }
            if ((fred == null) || (fred.length != nfshapes)
                    || (fgreen == null) || (fgreen.length != nfshapes)
                    || (fblue == null) || (fblue.length != nfshapes)) {
                throw new VisADException("bad fcolors");
            }
            first_tris = new int[nfshapes][][];
            for (int i = 0; i < nfshapes; i++) {
                first_tris[i] = DelaunayCustom.fill(fshapes[i]);
            }
            first_shapes = new float[nfshapes][2][];
            int flen = 0;
            for (int i = 0; i < nfshapes; i++) {
                int n = fshapes[i][0].length;
                flen               += n;
                first_shapes[i][0] = new float[n];
                first_shapes[i][1] = new float[n];
                System.arraycopy(fshapes[i][0], 0, first_shapes[i][0], 0, n);
                System.arraycopy(fshapes[i][1], 0, first_shapes[i][1], 0, n);
            }
            fprofile_length = flen;
            first_red       = new float[nfshapes];
            first_green     = new float[nfshapes];
            first_blue      = new float[nfshapes];
            System.arraycopy(fred, 0, first_red, 0, nfshapes);
            System.arraycopy(fgreen, 0, first_green, 0, nfshapes);
            System.arraycopy(fblue, 0, first_blue, 0, nfshapes);
        }
        if (rprofile_length < 5) {
            rprofile_length = 5;
        }
        if (fprofile_length < 5) {
            fprofile_length = 5;
        }
    }




    /**
     * Get the latitude scalar map
     *
     * @return the latitude scalar map
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private ScalarMap getLatMap() throws VisADException, RemoteException {
        if (latMap == null) {
            initLatLonMap();
        }
        return latMap;
    }

    /**
     * Get the longitude scalar map
     *
     * @return the longitude scalar map
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private ScalarMap getLonMap() throws VisADException, RemoteException {
        if (lonMap == null) {
            initLatLonMap();
        }
        return lonMap;
    }

    /**
     * Set the display master
     *
     * @param master  the display master
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setDisplayMaster(DisplayMaster master)
            throws VisADException, RemoteException {
        super.setDisplayMaster(master);
        if ((latMap == null) && (curve != null)) {
            setCurve(curve, curveTimes);
        }
    }


    /**
     * Set the curve
     *
     * @param curve  the  curve coordinates
     * @param curveTimes   the curve times
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setCurve(float[][] curve, List curveTimes)
            throws VisADException, RemoteException {
        this.curve      = curve;
        this.curveTimes = curveTimes;
        FieldImpl front = makeFront(curve);
        if (front != null) {
            setData(Util.makeTimeRangeField(front, curveTimes));
        }
    }



    /**
     * Initialize the lat/lon scalar maps
     *
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initLatLonMap() throws VisADException, RemoteException {
        if (latMap != null) {
            return;
        }
        if (getDisplayMaster() == null) {
            return;
        }
        boolean     callScale = (latMap == null);
        DisplayImpl display   = (DisplayImpl) getDisplayMaster().getDisplay();
        // find spatial maps for Latitude and Longitude
        latMap = null;
        lonMap = null;
        Vector      scalar_map_vector = display.getMapVector();
        Enumeration en                = scalar_map_vector.elements();
        while (en.hasMoreElements()) {
            ScalarMap        map   = (ScalarMap) en.nextElement();
            DisplayRealType  real  = map.getDisplayScalar();
            DisplayTupleType tuple = real.getTuple();
            if ((tuple != null)
                    && (tuple.equals(Display.DisplaySpatialCartesianTuple)
                        || ((tuple.getCoordinateSystem() != null)
                            && tuple.getCoordinateSystem().getReference()
                                .equals(Display
                                    .DisplaySpatialCartesianTuple)))) {  // Spatial
                if (RealType.Latitude.equals(map.getScalar())) {
                    latMap = map;
                } else if (RealType.Longitude.equals(map.getScalar())) {
                    lonMap = map;
                }
            }
        }
        if (callScale) {
            setScale(baseScale);
        }
    }


    /**
     * Initialize the color maps
     *
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void initColorMaps() throws VisADException, RemoteException {
        setupTypes();
        if (doColors) {
            ScalarMap rmap = new ScalarMap(front_red, Display.Red);
            rmap.setRange(0.0, 1.0);
            ScalarMap gmap = new ScalarMap(front_green, Display.Green);
            gmap.setRange(0.0, 1.0);
            ScalarMap bmap = new ScalarMap(front_blue, Display.Blue);
            bmap.setRange(0.0, 1.0);


            ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
            maps.add(rmap);
            maps.add(bmap);
            maps.add(gmap);
            setScalarMapSet(maps);
        }

    }

    /**
     * set up the types
     *
     * @throws VisADException On badness
     */
    private void setupTypes() throws VisADException {
        if (curve_type == null) {
            RealTupleType latlon = RealTupleType.LatitudeLongitudeTuple;
            curve_type = new SetType(latlon);
            // (front_index -> 
            //    ((Latitude, Longitude) -> (front_red, front_green, front_blue)))
            count++;
            front_index = RealType.getRealType("front_index" + count);
            front_red   = RealType.getRealType("front_red" + count);
            front_green = RealType.getRealType("front_green" + count);
            front_blue  = RealType.getRealType("front_blue" + count);
            RealTupleType rgb = new RealTupleType(front_red, front_green,
                                    front_blue);
            front_inner = new FunctionType(latlon, rgb);
            front_type  = new FunctionType(front_index, front_inner);
            fronts_type = new FunctionType(RealType.Time, front_type);
        }
    }


    /**
     * Make the front
     *
     * @param curve_samples   the front samples (coords,rgb)
     *
     * @return  a FieldImpl of the front
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FieldImpl makeFront(float[][] curve_samples)
            throws VisADException, RemoteException {
        boolean flip = true;
        initLatLonMap();
        if (latMap == null) {
            return null;
        }
        double[] lat_range = getLatMap().getRange();
        double[] lon_range = getLonMap().getRange();
        if (lat_range[1] < lat_range[0]) {
            flip = !flip;
        }
        if (lon_range[1] < lon_range[0]) {
            flip = !flip;
        }
        if (curve_samples[LAT_INDEX][0] < 0.0) {
            flip = !flip;
        }


        // transform curve to graphics coordinates
        // in order to "draw" front in graphics coordinates, then
        // transform back to (lat, lon)
        float[][] curve = new float[2][];
        curve[0] = getLatMap().scaleValues(curve_samples[LAT_INDEX]);
        curve[1] = getLonMap().scaleValues(curve_samples[LON_INDEX]);
        // inverseScaleValues
        // if (debug) System.out.println("curve length = " + curve[0].length);

        return robustCurveToFront(curve, flip);
    }






    /**
     * Robustly make a curve to a front
     *
     * @param curve  the curve
     * @param flip   true to flip the pips
     *
     * @return the resulting front as a FieldImpl
     *
     * @throws RemoteException On badness
     */
    public FieldImpl robustCurveToFront(float[][] curve, boolean flip)
            throws RemoteException {

        // resample curve uniformly along length
        float     increment = rsegment_length / (rprofile_length * zoom);
        float[][] oldCurve  = null;
        try {
            oldCurve = resample_curve(curve, increment);
        } catch (VisADError ve) {  // bad curve
            return null;
        }

        int fw = filter_window;
        fw = 1;
        FieldImpl front         = null;

        float[][] originalCurve = curve;


        debug = true;
        for (int tries = 0; tries < 12; tries++) {
            // lowpass filter curve
            curve = smooth_curve(oldCurve, fw);
            // resample smoothed curve
            curve = resample_curve(curve, increment);
            try {
                front = curveToFront(curve, flip);
                break;
            } catch (VisADException e) {
                oldCurve = curve;
                if (tries > 4) {
                    int n = oldCurve[0].length;
                    if (n > 2) {
                        float[][] no = new float[2][n - 2];
                        System.arraycopy(oldCurve[0], 1, no[0], 0, n - 2);
                        System.arraycopy(oldCurve[1], 1, no[1], 0, n - 2);
                        oldCurve = no;
                    }
                }
                if (tries > 8) {
                    fw = 2 * fw;
                }
                // if (debug) System.out.println("retry filter window = " + fw + " " + e);
                if (tries == 9) {
                    //                    System.out.println("cannot smooth curve");
                    front = null;
                }
            }
        }
        return front;
    }



    /**
     * Create a front from the curve
     *
     * @param curve   the curve coordinates
     * @param flip    true to flip the pips
     *
     * @return The front as a FieldImpl
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FieldImpl curveToFront(float[][] curve, boolean flip)
            throws VisADException, RemoteException {

        if (flipTheFlip) {
            flip = !flip;
        }

        // compute various scaling factors
        int len = curve[0].length;
        if (len < 2) {
            return null;
        }
        float[] seg_length   = new float[len - 1];
        float   curve_length = curveLength(curve, seg_length);
        float   delta        = curve_length / (len - 1);
        // curve[findex] where
        // float findex = ibase + mul * repeat_shapes[shape][0][j]
        float mul = rprofile_length * zoom / rsegment_length;
        // curve_perp[][findex] * ratio * repeat_shapes[shape][1][j]
        float ratio = delta * mul;


        // compute unit perpendiculars to curve
        float[][] curve_perp = new float[2][len];
        for (int i = 0; i < len; i++) {
            int im = i - 1;
            int ip = i + 1;
            if (im < 0) {
                im = 0;
            }
            if (ip > len - 1) {
                ip = len - 1;
            }
            float yp = curve[0][ip] - curve[0][im];
            float xp = curve[1][ip] - curve[1][im];
            xp = -xp;
            float d = (float) Math.sqrt(xp * xp + yp * yp);
            if (flip) {
                d = -d;
            }
            xp               = xp / d;
            yp               = yp / d;
            curve_perp[0][i] = xp;
            curve_perp[1][i] = yp;
        }

        // build Vector of FlatFields for each shape of each segment
        Vector inner_field_vector = new Vector();
        for (int segment = 0; true; segment++) {

            // curve[findex] where
            // float findex = ibase + mul * repeat_shapes[shape][0][j]
            float segment_length = (segment == 0)
                                   ? fsegment_length
                                   : rsegment_length;
            int   profile_length = (segment == 0)
                                   ? fprofile_length
                                   : rprofile_length;
            mul = profile_length * zoom / segment_length;
            // curve_perp[][findex] * ratio * repeat_shapes[shape][1][j]
            // float ratio = delta * mul;


            // figure out if clipping is needed for this segment
            // only happens for last segment
            boolean clip  = false;
            float   xclip = 0.0f;
            // int ibase = segment * profile_length;
            int ibase = (segment == 0)
                        ? 0
                        : fprofile_length + (segment - 1) * rprofile_length;
            int iend  = ibase + profile_length;
            if (ibase > len - 1) {
                break;
            }
            if (iend > len - 1) {
                clip  = true;
                iend  = len - 1;
                xclip = (iend - ibase) / mul;
            }

            // set up shapes for first or repeating segment
            int         nshapes = nrshapes;
            float[][][] shapes  = repeat_shapes;
            int[][][]   tris    = repeat_tris;
            float[]     red     = repeat_red;
            float[]     green   = repeat_green;
            float[]     blue    = repeat_blue;
            if (segment == 0) {
                nshapes = nfshapes;
                shapes  = first_shapes;
                tris    = first_tris;
                red     = first_red;
                green   = first_green;
                blue    = first_blue;
            }

            // iterate over shapes for segment
            for (int shape = 0; shape < nshapes; shape++) {
                float[][] samples = shapes[shape];
                int[][]   ts      = tris[shape];
                /*
                // if needed, clip shape
                if (clip) {
                float[][][] outs = new float[1][][];
                int[][][] outt = new int[1][][];
                DelaunayCustom.clip(samples, ts, 1.0f, 0.0f, xclip, outs, outt);
                samples = outs[0];
                ts = outt[0];
                }
                */
                if ((samples == null) || (samples[0].length < 1)) {
                    break;
                }

                float[][] ss = mapShape(samples, len, ibase, mul, ratio,
                                        curve, curve_perp);

                // **** get rid of previous calls to fill() ****
                ts = DelaunayCustom.fill(ss);

                //jeffmc: For now don't clip. This seems to fix the problem of too short a front
                boolean DOCLIP = false;
                if (clip && DOCLIP) {
                    float[][] clip_samples = {
                        { xclip, xclip, xclip - CLIP_DELTA },
                        { CLIP_DELTA, -CLIP_DELTA, 0.0f }
                    };
                    float[][] clip_ss = mapShape(clip_samples, len, ibase,
                                            mul, ratio, curve, curve_perp);
                    // now solve for:
                    //   xc * clip_samples[0][0] + yc * clip_samples[1][0] = 1
                    //   xc * clip_samples[0][1] + yc * clip_samples[1][1] = 1
                    //   xc * clip_samples[0][2] + yc * clip_samples[1][2] < 1
                    float det = (clip_samples[0][1] * clip_samples[1][0]
                                 - clip_samples[0][0] * clip_samples[1][1]);
                    float xc = (clip_samples[1][0] - clip_samples[1][1])
                               / det;
                    float yc = (clip_samples[0][1] - clip_samples[0][0])
                               / det;
                    float v = 1.0f;
                    if (xc * clip_samples[0][2] + yc * clip_samples[1][2]
                            > v) {
                        xc = -xc;
                        yc = -yc;
                        v  = -v;
                    }

                    float[][][] outs = new float[1][][];
                    int[][][]   outt = new int[1][][];
                    DelaunayCustom.clip(ss, ts, xc, yc, v, outs, outt);
                    ss = outs[0];
                    ts = outt[0];
                }

                if (ss == null) {
                    break;
                }
                int n = ss[0].length;

                // create color values for field
                float[][] values = new float[3][n];
                float     r      = red[shape];
                float     g      = green[shape];
                float     b      = blue[shape];
                for (int i = 0; i < n; i++) {
                    values[0][i] = r;
                    values[1][i] = g;
                    values[2][i] = b;
                }

                // construct set and field
                DelaunayCustom delaunay = new DelaunayCustom(ss, ts);
                Irregular2DSet set = new Irregular2DSet(curve_type, ss, null,
                                         null, null, delaunay);
                FlatField field = new FlatField(front_inner, set);
                field.setSamples(values, false);
                inner_field_vector.addElement(field);
                // some crazy bug - see Gridded3DSet.makeNormals()
            }  // end for (int shape=0; shape<nshapes; shape++)
        }      // end for (int segment=0; true; segment++)

        int          nfields = inner_field_vector.size();
        Integer1DSet iset    = new Integer1DSet(front_index, nfields);
        FieldImpl    front   = new FieldImpl(front_type, iset);
        FlatField[]  fields  = new FlatField[nfields];
        for (int i = 0; i < nfields; i++) {
            fields[i] = (FlatField) inner_field_vector.elementAt(i);
        }
        front.setSamples(fields, false);
        return front;
    }



    /**
     * Map the shapes
     *
     * @param samples _more_
     * @param len _more_
     * @param ibase _more_
     * @param mul _more_
     * @param ratio _more_
     * @param curve _more_
     * @param curve_perp _more_
     *
     * @return _more_
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private float[][] mapShape(float[][] samples, int len, int ibase,
                               float mul, float ratio, float[][] curve,
                               float[][] curve_perp)
            throws VisADException, RemoteException {
        // map shape into "coordinate system" defined by curve segment
        int       n  = samples[0].length;
        float[][] ss = new float[2][n];
        for (int i = 0; i < n; i++) {
            float findex = ibase + mul * samples[0][i] / zoom;
            int   il     = (int) findex;
            int   ih     = il + 1;

            if (il < 0) {
                il = 0;
                ih = il + 1;
            }
            if (ih > len - 1) {
                ih = len - 1;
                il = ih - 1;
            }
            // if (il < 0) il = 0;
            // if (il > len - 1) il = len - 1;
            // if (ih < 0) ih = 0;
            // if (ih > len - 1) ih = len - 1;

            float a = findex - il;

            if (a < -1.0f) {
                a = -1.0f;
            }
            if (a > 2.0f) {
                a = 2.0f;
            }
            // if (a < 0.0f) a = 0.0f;
            // if (a > 1.0f) a = 1.0f;

            float b = 1.0f - a;
            float xl = curve[0][il]
                       + ratio * samples[1][i] * curve_perp[0][il] / zoom;
            float yl = curve[1][il]
                       + ratio * samples[1][i] * curve_perp[1][il] / zoom;
            float xh = curve[0][ih]
                       + ratio * samples[1][i] * curve_perp[0][ih] / zoom;
            float yh = curve[1][ih]
                       + ratio * samples[1][i] * curve_perp[1][ih] / zoom;
            ss[0][i] = b * xl + a * xh;
            ss[1][i] = b * yl + a * yh;
        }
        // map shape back into (lat, lon) coordinates
        ss[LAT_INDEX] = getLatMap().inverseScaleValues(ss[0]);
        ss[LON_INDEX] = getLonMap().inverseScaleValues(ss[1]);
        return ss;
    }

    /**
     * Smooth the curve
     *
     * @param curve   the curve
     * @param window  the window id
     *
     * @return  a smoothed curve
     */
    public static float[][] smooth_curve(float[][] curve, int window) {
        int       len      = curve[0].length;
        float[][] newcurve = new float[2][len];
        for (int i = 0; i < len; i++) {
            int win = window;
            if (i < win) {
                win = i;
            }
            int ii = (len - 1) - i;
            if (ii < win) {
                win = ii;
            }
            float runx = 0.0f;
            float runy = 0.0f;
            for (int j = i - win; j <= i + win; j++) {
                runx += curve[0][j];
                runy += curve[1][j];
            }
            newcurve[0][i] = runx / (2 * win + 1);
            newcurve[1][i] = runy / (2 * win + 1);
        }
        return newcurve;
    }

    /**
     * Resample curve into segments approximately increment in length
     *
     * @param curve  the curve
     * @param increment the increment
     *
     * @return  the resampled curve
     */
    public static float[][] resample_curve(float[][] curve, float increment) {
        int       len          = curve[0].length;
        float[]   seg_length   = new float[len - 1];
        float     curve_length = curveLength(curve, seg_length);
        int       npoints      = 1 + (int) (curve_length / increment);
        float     delta        = curve_length / (npoints - 1);
        float[][] newcurve     = new float[2][npoints];
        newcurve[0][0] = curve[0][0];
        newcurve[1][0] = curve[1][0];
        if (npoints < 2) {
            return newcurve;
        }
        int   k       = 0;
        float old_seg = seg_length[k];
        for (int i = 1; i < npoints - 1; i++) {
            float new_seg = delta;
            while (true) {
                if (old_seg < new_seg) {
                    new_seg -= old_seg;
                    k++;
                    if (k > len - 2) {
                        throw new VisADError("k = " + k + " i = " + i);
                    }
                    old_seg = seg_length[k];
                } else {
                    old_seg -= new_seg;
                    float a = old_seg / seg_length[k];
                    newcurve[0][i] = a * curve[0][k]
                                     + (1.0f - a) * curve[0][k + 1];
                    newcurve[1][i] = a * curve[1][k]
                                     + (1.0f - a) * curve[1][k + 1];
                    break;
                }
            }
        }
        newcurve[0][npoints - 1] = curve[0][len - 1];
        newcurve[1][npoints - 1] = curve[1][len - 1];
        return newcurve;
    }

    /**
     * Get the curve length;
     * assumes curve is float[2][len] and seg_length is float[len-1]
     *
     * @param curve the curve
     * @param seg_length  the segment lengths
     *
     * @return the length of the curve
     */
    public static float curveLength(float[][] curve, float[] seg_length) {
        int   len          = curve[0].length;
        float curve_length = 0.0f;
        for (int i = 0; i < len - 1; i++) {
            seg_length[i] = (float) Math.sqrt(((curve[0][i + 1]
                    - curve[0][i]) * (curve[0][i + 1]
                                      - curve[0][i])) + ((curve[1][i + 1]
                                          - curve[1][i]) * (curve[1][i + 1]
                                              - curve[1][i])));
            curve_length += seg_length[i];
        }
        return curve_length;
    }







    /**
     * Set the FlipTheFlip property. If true then this flips the orientation of
     * the front
     *
     * @param value The new value for FlipTheFlip
     */
    public void setFlipTheFlip(boolean value) {
        flipTheFlip = value;
    }

    /**
     * Get the FlipTheFlip property.
     *
     * @return The FlipTheFlip
     */
    public boolean getFlipTheFlip() {
        return flipTheFlip;
    }



}
