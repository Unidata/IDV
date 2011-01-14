/*
 * $Id: GlyphCreatorCommand.java,v 1.21 2007/08/16 22:30:14 jeffmc Exp $
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

package ucar.unidata.idv.control.drawing;


import ucar.unidata.idv.control.DrawingControl;

import ucar.visad.display.FrontDrawer;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class GlyphCreatorCommand represents when a glyph should be created
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.21 $
 */
public abstract class GlyphCreatorCommand extends DrawingCommand {


    /** command */
    public static final DrawingCommand CMD_SMOOTHPOLYGON =
        new GlyphCreatorCommand("Create a polygon",
                                "Click and drag: create a polygon",
                                "/auxdata/ui/icons/SmoothPoly16.gif",
                                DrawingControl.FLAG_FILLED
                                | DrawingControl.FLAG_STRAIGHT) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new PolyGlyph(control, event, !control.getStraight());
        }
    };

    /** command to create a closed polygon          */
    public static final DrawingCommand CMD_CLOSEDPOLYGON =
        new GlyphCreatorCommand("Create a closed polygon",
                                "Click and drag: create a closed polygon",
                                "/auxdata/ui/icons/ClosedPoly16.gif",
                                DrawingControl.FLAG_STRAIGHT) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            PolyGlyph glyph = new PolyGlyph(control, event,
                                            !control.getStraight());
            glyph.setClosed(true);
            return glyph;
        }
    };

    /** command */
    public static final DrawingCommand CMD_POLYGON =
        new GlyphCreatorCommand(
            "Create a polygon",
            "Click and drag: create a polygon. Space key to add points",
            "/auxdata/ui/icons/Poly16.gif",
            DrawingControl.FLAG_FILLED | DrawingControl.FLAG_STRAIGHT) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new PolyGlyph(control, event, !control.getStraight());
        }
    };

    /** command */
    public static final DrawingCommand CMD_LINE =
        new GlyphCreatorCommand("Create a line",
                                "Click and drag: create a line segment",
                                "/auxdata/ui/icons/Line16.gif") {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_LINE);
        }
    };

    /** command */
    public static final DrawingCommand CMD_SYMBOL =
        new GlyphCreatorCommand("Create a symbol", "Click to create symbol",
                                "/auxdata/ui/icons/Symbol16.gif") {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new SymbolGlyph(control, event);
        }
    };

    /** command */
    public static final DrawingCommand CMD_TRANSECT =
        new GlyphCreatorCommand("Create a transect",
                                "Click and drag: create a transect",
                                "/auxdata/ui/icons/Transect16.gif") {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new TransectGlyph(control, event, true);
        }
    };


    /** command */
    public static final DrawingCommand CMD_RECTANGLE =
        new GlyphCreatorCommand("Create a rectangle",
                                "Click and drag: create a rectangle",
                                "/auxdata/ui/icons/Rectangle16.gif",
                                DrawingControl.FLAG_FILLED) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_RECTANGLE);
        }
    };

    /** command */
    public static final DrawingCommand CMD_ARROW =
        new GlyphCreatorCommand("Create an arrow",
                                "Click and drag: create an arrow",
                                "/auxdata/ui/icons/Arrow16.gif",
                                DrawingControl.FLAG_STRAIGHT) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ArrowGlyph(control, event, !control.getStraight());
        }
    };


    /** command */
    public static final DrawingCommand CMD_HARROW =
        new GlyphCreatorCommand("Create a horizontal arrow",
                                "Click and drag: create an horizontal arrow",
                                "/auxdata/ui/icons/HorArrow16.gif",
                                DrawingControl.FLAG_FILLED) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_HARROW);
        }
    };


    /** command */
    public static final DrawingCommand CMD_VARROW =
        new GlyphCreatorCommand("Create a vertical arrow",
                                "Click and drag: create a vertical arrow",
                                "/auxdata/ui/icons/VertArrow16.gif",
                                DrawingControl.FLAG_FILLED) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_VARROW);
        }
    };


    /** command */
    public static final DrawingCommand CMD_DIAMOND =
        new GlyphCreatorCommand("Create a diamond",
                                "Click and drag: create a diamond",
                                "/auxdata/ui/icons/Diamond16.gif",
                                DrawingControl.FLAG_FILLED) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_DIAMOND);
        }
    };


    /** command */
    public static final DrawingCommand CMD_OVAL =
        new GlyphCreatorCommand("Create an oval",
                                "Click and drag: create an oval",
                                "/auxdata/ui/icons/Circle16.gif",
                                DrawingControl.FLAG_FILLED) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ShapeGlyph(control, event, ShapeGlyph.SHAPE_OVAL);
        }
    };


    /** command */
    public static final DrawingCommand CMD_TEXT =
        new GlyphCreatorCommand("Create text", "Click to create text",
                                "/auxdata/ui/icons/Text16.gif", false) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new TextGlyph(control, event, "");
        }
    };


    /** command */
    public static final DrawingCommand CMD_WAYPOINT =
        new GlyphCreatorCommand("Create waypoint",
                                "Click to create waypoint",
                                "/auxdata/ui/icons/Placemark16.gif", false) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            TextGlyph glyph = new TextGlyph(control, event, "");
            ((TextGlyph) glyph).setShowMarker(true);
            return glyph;
        }
    };

    /** command */
    public static final DrawingCommand CMD_IMAGE =
        new GlyphCreatorCommand("Create image", "Click to add an image",
                                "/auxdata/ui/icons/Image16.gif",
                                DrawingControl.FLAG_FULLLATLON, false) {

        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new ImageGlyph(control, event, null);
        }
    };


    /** command */
    public static final DrawingCommand CMD_MOVIE =
        new GlyphCreatorCommand("Create quicktime movie",
                                "Click to add a movie",
                                "/auxdata/ui/icons/Movie.gif", false) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new MovieGlyph(control, event);
        }
    };


    /** command */
    public static final DrawingCommand CMD_HIGH =
        new GlyphCreatorCommand("Create high pressure symbol",
                                "Click to create a high pressure symbol",
                                "/auxdata/ui/icons/High16.gif", false) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new HighLowGlyph(control, event, true);
        }
    };


    /** command */
    public static final DrawingCommand CMD_LOW =
        new GlyphCreatorCommand("Create low pressure symbol",
                                "Click to create a low pressure symbol",
                                "/auxdata/ui/icons/Low16.gif", false) {
        public DrawingGlyph createGlyph(DrawingControl control,
                                        DisplayEvent event)
                throws VisADException, RemoteException {
            return new HighLowGlyph(control, event, false);
        }
    };


    /** Does this glyph creation need a mouse press */
    private boolean needsMouse = true;


    /**
     * Ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath  The icon to display
     */
    public GlyphCreatorCommand(String label, String message,
                               String iconPath) {
        this(label, message, iconPath, 0);
    }



    /**
     * Ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath  The icon to display
     * @param needsMouse  Does this glyph creation need a mouse press
     */
    public GlyphCreatorCommand(String label, String message, String iconPath,
                               boolean needsMouse) {
        this(label, message, iconPath, 0, needsMouse);
    }


    /**
     * Ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath  The icon to display
     * @param flags Command flags
     */
    public GlyphCreatorCommand(String label, String message, String iconPath,
                               int flags) {
        super(label, message, iconPath, flags);
    }



    /**
     * Ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath  The icon to display
     * @param flags Command flags
     * @param needsMouse  Does this glyph creation need a mouse press
     */
    public GlyphCreatorCommand(String label, String message, String iconPath,
                               int flags, boolean needsMouse) {
        super(label, message, iconPath, flags);
        this.needsMouse = needsMouse;
    }


    /**
     * Get the NeedsMouse property.
     *
     * @return The NeedsMouse
     */
    public boolean getNeedsMouse() {
        return needsMouse;
    }



    /**
     * Create the glyph
     *
     * @param control DrawingControl we are in
     * @param event The event
     *
     * @return The glyph
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public abstract DrawingGlyph createGlyph(DrawingControl control,
                                             DisplayEvent event)
     throws VisADException, RemoteException;



}


