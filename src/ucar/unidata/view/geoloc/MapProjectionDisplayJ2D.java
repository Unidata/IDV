/*
 * $Id: MapProjectionDisplayJ2D.java,v 1.10 2006/08/22 20:18:47 jeffmc Exp $
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

package ucar.unidata.view.geoloc;



import java.awt.Color;
import java.awt.event.InputEvent;

import java.rmi.RemoteException;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.*;

import visad.*;
import visad.georef.MapProjection;

import visad.java2d.DisplayImplJ2D;
import visad.java2d.DisplayRendererJ2D;
import visad.java2d.KeyboardBehaviorJ2D;
import visad.java2d.ProjectionControlJ2D;

import visad.KeyboardBehavior;


/**
 * Provides a navigated VisAD DisplayImpl for displaying data.
 * The Projection or MapProjection provides the transformation from
 * lat/lon space to xy space.  There is one mode with this display - MODE_2D
 * Performance is better using MapProjectionDisplayJ3D.
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude.<P>
 *
 * @author Don Murray
 * @version $Revision: 1.10 $ $Date: 2006/08/22 20:18:47 $
 */
public class MapProjectionDisplayJ2D extends MapProjectionDisplay {

    /** instance lock */
    private static Object INSTANCE_MUTEX = new Object();


    /**
     * Constructs the default instance.  The default instance is based
     * on a LatLonProjection.
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ2D() throws VisADException, RemoteException {
        this((ProjectionImpl) null);
    }

    /**
     * Constructs an instance with the specified Projection and mode.
     *
     * @param  projection   map projection
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ2D(ProjectionImpl projection)
            throws VisADException, RemoteException {
        this((projection == null)
             ? (MapProjection) null
             : new ProjectionCoordinateSystem(projection));
    }

    /**
     * Constructs an instance with the specified MapProjection
     *
     * @param  projection   map projection CS
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ2D(MapProjection projection)
            throws VisADException, RemoteException {
        super((projection == null)
              ? makeDefaultMapProjection()
              : projection, new DisplayImplJ2D("Navigated Display"));
    }

    /**
     * Set up the display called by constructor
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void initializeClass() throws VisADException, RemoteException {
        super.initializeClass();
        setBoxVisible(false);
        DisplayRendererJ2D rend =
            (DisplayRendererJ2D) getDisplay().getDisplayRenderer();
        KeyboardBehaviorJ2D kb = new KeyboardBehaviorJ2D(rend);
        rend.addKeyboardBehavior(kb);
        setKeyboardBehavior(kb);


        // Create a RubberBandBox
        /*  Someday, we'll get this to work
        RubberBandBox rubberBandBox =
            new RubberBandBox(RealType.XAxis, RealType.YAxis,
                              InputEvent.SHIFT_MASK);
        rubberBandBox.addAction(new ActionImpl("RBB Action") {
            public void doAction()
                throws VisADException, RemoteException
            {
                if (rubberBandBox.getBounds() != null)
                    setMapRegion(rubberBandBox.getBounds());
            }
        });
        setRubberBandBox(rubberBandBox);
        enableRubberBanding(true);
        */

    }

    //    private List keyboardBehaviors;

    /**
     * Add a keyboard behavior.
     *
     * @param behavior  behavior to add
     */
    public void addKeyboardBehavior(KeyboardBehavior behavior) {
        DisplayRendererJ2D rend =
            (DisplayRendererJ2D) getDisplay().getDisplayRenderer();
        KeyboardBehaviorWrapper2D beh = new KeyboardBehaviorWrapper2D(rend,
                                            behavior);
        rend.addKeyboardBehavior(beh);
    }


    /**
     * Class KeyboardBehaviorWrapper2D.  Wrapper for adding
     * a KeyboardBehaviorJ2D
     *
     * @author Unidata development team
     */
    static class KeyboardBehaviorWrapper2D extends KeyboardBehaviorJ2D {

        /** the behavior */
        KeyboardBehavior behavior;

        /**
         * Create a new wrapper
         *
         * @param rend      display renderer
         * @param behavior  keyboard behavior
         *
         */
        public KeyboardBehaviorWrapper2D(DisplayRendererJ2D rend,
                                         KeyboardBehavior behavior) {
            super(rend);
            this.behavior = behavior;
        }

        /**
         * Map a key to a function.  Wrapper around KeyboardBehavior method.
         *
         * @param function   function to map
         * @param keycode    keycode for function
         * @param modifiers  key modifiers
         */
        public void mapKeyToFunction(int function, int keycode,
                                     int modifiers) {
            //This method does not work because it is called by the super class's ctor
            //before we have a chance to set the behavior
            if (behavior != null) {
                behavior.mapKeyToFunction(function, keycode, modifiers);
            }
        }

        /**
         * Wrapper around {@link KeyboardBehavior#processKeyEvent}
         *
         * @param event  event to process
         */
        public void processKeyEvent(java.awt.event.KeyEvent event) {
            behavior.processKeyEvent(event);
        }

        /**
         * Wrapper around {@link KeyboardBehavior#execFunction}
         *
         * @param function   function to execute
         */
        public void execFunction(int function) {
            behavior.execFunction(function);
        }
    }

    /**
     * Get the view to perspective or parallel.
     *
     * @return true if perpsective view
     */
    public boolean isPerspectiveView() {
        return false;
    }

    /**
     * Enable clipping of data at the box edges
     *
     * @param  clip  true to turn clipping on, otherwise off
     */
    public void enableClipping(boolean clip) {
        DisplayRendererJ2D dr2 =
            (DisplayRendererJ2D) getDisplay().getDisplayRenderer();
        if (clip) {
            dr2.setClip(-1.0f, 1.0f, -1.0f, 1.0f);
        }
        super.enableClipping(clip);
    }


    /**
     * Retrieve the RubberBandBox being used in this component.
     *
     * @return  RubberBandBox being used.  This instance returns null.
     */
    public RubberBandBox getRubberBandBox() {
        return null;
    }

}
