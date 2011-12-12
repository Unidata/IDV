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



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import visad.*;

import visad.java3d.*;

import java.awt.event.InputEvent;

import java.awt.event.KeyEvent;



import java.util.ArrayList;
import java.util.List;



/**
 * A class to hold  event mappings for keys and mouse movements
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
abstract public class EventMap {


    /** Mouse function values */
    public static final int[] MOUSE_FUNCTION_VALUES = {
        MouseHelper.NONE, MouseHelper.ROTATE, MouseHelper.ZOOM,
        MouseHelper.TRANSLATE, MouseHelper.DIRECT,
        MouseHelper.CURSOR_TRANSLATE, MouseHelper.CURSOR_ZOOM,
        MouseHelper.CURSOR_ROTATE
    };

    /** Mouse function names */
    public static final String[] MOUSE_FUNCTION_NAMES = {
        "None", "Rotate", "Zoom", "Translate", "Selection",
        "Cursor/Data Readout", "Cursor Zoom", "Cursor Rotate",
    };


    /** Key function values */
    public static final int[] KEY_FUNCTION_VALUES = {
        KeyboardBehaviorJ3D.RESET, KeyboardBehaviorJ3D.TRANSLATE_UP,
        KeyboardBehaviorJ3D.TRANSLATE_DOWN,
        KeyboardBehaviorJ3D.TRANSLATE_LEFT,
        KeyboardBehaviorJ3D.TRANSLATE_RIGHT, KeyboardBehaviorJ3D.ZOOM_IN,
        KeyboardBehaviorJ3D.ZOOM_OUT, KeyboardBehaviorJ3D.ROTATE_Z_POS,
        KeyboardBehaviorJ3D.ROTATE_Z_NEG, KeyboardBehaviorJ3D.ROTATE_X_POS,
        KeyboardBehaviorJ3D.ROTATE_X_NEG, KeyboardBehaviorJ3D.ROTATE_Y_POS,
        KeyboardBehaviorJ3D.ROTATE_Y_NEG
    };

    /** Key function names */
    public static final String[] KEY_FUNCTION_NAMES = {
        "Reset", "Translate Up", "Translate Down", "Translate Left",
        "Translate Right", "Zoom In", "Zoom Out", "Rotate Z Positive",
        "Rotate Z Negative", "Rotate X Positive", "Rotate X Negative",
        "Rotate Y Positive", "Rotate Y Negative"
    };

    /** Identifier for no mask */
    public static int NO_MASK = KeyboardBehaviorJ3D.NO_MASK;

    /** Identifier for ctrl mask */
    public static int CTRL_MASK = InputEvent.CTRL_MASK;

    /** Identifier for shift mask */
    public static int SHIFT_MASK = InputEvent.SHIFT_MASK;


    //J-
    /** IDV keyboard functions */
    public static final int[][] IDV_KEYBOARD_FUNCTIONS = {
        { KeyboardBehaviorJ3D.RESET,           KeyEvent.VK_R,     CTRL_MASK },

        { KeyboardBehaviorJ3D.TRANSLATE_UP,   KeyEvent.VK_UP,    CTRL_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_DOWN, KeyEvent.VK_DOWN,  CTRL_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_LEFT, KeyEvent.VK_LEFT,  CTRL_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_RIGHT,KeyEvent.VK_RIGHT, CTRL_MASK },
        { KeyboardBehaviorJ3D.ZOOM_IN,        KeyEvent.VK_UP,    SHIFT_MASK },
        { KeyboardBehaviorJ3D.ZOOM_OUT,       KeyEvent.VK_DOWN,  SHIFT_MASK },

        { KeyboardBehaviorJ3D.ROTATE_X_POS,   KeyEvent.VK_DOWN,  NO_MASK },
        { KeyboardBehaviorJ3D.ROTATE_X_NEG,   KeyEvent.VK_UP,    NO_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Y_POS,   KeyEvent.VK_LEFT,  NO_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Y_NEG,   KeyEvent.VK_RIGHT, NO_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Z_POS,   KeyEvent.VK_LEFT,  SHIFT_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Z_NEG,   KeyEvent.VK_RIGHT, SHIFT_MASK }
    };


    /** Google Earth keyboard functions */
    public static final int[][] GEARTH_KEYBOARD_FUNCTIONS = {
        { KeyboardBehaviorJ3D.RESET,           KeyEvent.VK_U,     NO_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_UP,    KeyEvent.VK_UP,    NO_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_DOWN,  KeyEvent.VK_DOWN,  NO_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_LEFT,  KeyEvent.VK_LEFT,  NO_MASK },
        { KeyboardBehaviorJ3D.TRANSLATE_RIGHT, KeyEvent.VK_RIGHT, NO_MASK },
        { KeyboardBehaviorJ3D.ZOOM_IN,         KeyEvent.VK_UP,    CTRL_MASK },
        { KeyboardBehaviorJ3D.ZOOM_OUT,        KeyEvent.VK_DOWN,  CTRL_MASK },
        { KeyboardBehaviorJ3D.ROTATE_X_POS,    KeyEvent.VK_DOWN,  SHIFT_MASK },
        { KeyboardBehaviorJ3D.ROTATE_X_NEG,    KeyEvent.VK_UP,    SHIFT_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Y_POS,    KeyEvent.VK_LEFT,  CTRL_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Y_NEG,    KeyEvent.VK_RIGHT, CTRL_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Z_POS,    KeyEvent.VK_LEFT,  SHIFT_MASK },
        { KeyboardBehaviorJ3D.ROTATE_Z_NEG,    KeyEvent.VK_RIGHT, SHIFT_MASK }
    };
//J+


    /** VisAD mouse functions */
    public static final int[][][] VISAD_MOUSE_FUNCTIONS = {
        {
            { MouseHelper.ROTATE, MouseHelper.ZOOM },
            { MouseHelper.TRANSLATE, MouseHelper.NONE }
        }, {
            { MouseHelper.CURSOR_TRANSLATE, MouseHelper.CURSOR_ZOOM },
            { MouseHelper.CURSOR_ROTATE, MouseHelper.NONE }
        }, {
            { MouseHelper.DIRECT, MouseHelper.DIRECT },
            { MouseHelper.DIRECT, MouseHelper.DIRECT }
        }
    };


    /** Google Earth mouse functions */
    public static final int[][][] GEARTH_MOUSE_FUNCTIONS = {
        {
            { MouseHelper.DIRECT, MouseHelper.TRANSLATE },
            { MouseHelper.TRANSLATE, MouseHelper.TRANSLATE }
        }, {
            { MouseHelper.ROTATE, MouseHelper.ROTATE },
            { MouseHelper.ROTATE, MouseHelper.ROTATE }
        }, {
            { MouseHelper.ZOOM, MouseHelper.ZOOM },
            { MouseHelper.ZOOM, MouseHelper.ZOOM }
        }
    };




    /**
     * The default mouse behavior (IDV mouse functions)
     */
    public static final int[][][] IDV_MOUSE_FUNCTIONS = new int[][][] {
        {
            { MouseHelper.DIRECT, MouseHelper.DIRECT },
            { MouseHelper.DIRECT, MouseHelper.DIRECT }
        }, {
            { MouseHelper.CURSOR_TRANSLATE, MouseHelper.CURSOR_ZOOM },
            { MouseHelper.CURSOR_ROTATE, MouseHelper.NONE }
        }, {
            { MouseHelper.ROTATE, MouseHelper.ZOOM },
            { MouseHelper.TRANSLATE, MouseHelper.NONE }
        }
    };




    /** Identifier for the mouse wheel */
    public static int WHEEL_NONE = -1;

    /** Identifier for mouse wheel rotate X */
    public static int WHEEL_ROTATEX = 0;

    /** Identifier for mouse wheel rotate Y */
    public static int WHEEL_ROTATEY = 1;

    /** Identifier for mouse wheel rotate Z */
    public static int WHEEL_ROTATEZ = 2;

    /** Identifier for wheel zoom in */
    public static int WHEEL_ZOOMIN = 4;

    /** Identifier for wheel zoom out */
    public static int WHEEL_ZOOMOUT = 5;


    /** Mouse wheel function values */
    public static final int[] WHEEL_FUNCTION_VALUES = {
        WHEEL_ZOOMIN, WHEEL_ZOOMOUT, WHEEL_ROTATEX, WHEEL_ROTATEY,
        WHEEL_ROTATEZ, WHEEL_NONE
    };

    /** Mouse wheel function names */
    public static final String[] WHEEL_FUNCTION_NAMES = {
        "Down Zoom In", "Down Zoom Out", "Rotate X", "Rotate Y", "Rotate Z",
        "None"
    };

    /** IDV wheel functions */
    public static final int[][] IDV_WHEEL_FUNCTIONS = {
        { WHEEL_ZOOMIN, WHEEL_ROTATEX }, { WHEEL_ROTATEY, WHEEL_ROTATEZ }
    };

    /** IDV wheel functions */
    public static final int[][] GEARTH_WHEEL_FUNCTIONS = {
        { WHEEL_ZOOMOUT, WHEEL_ROTATEX }, { WHEEL_ROTATEY, WHEEL_ROTATEZ }
    };

}
