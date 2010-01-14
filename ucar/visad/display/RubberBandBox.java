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



import visad.*;

import visad.bom.*;

import java.awt.event.InputEvent;

import java.rmi.RemoteException;


/**
 * Provides support for a Displayable that comprises a rubber band box.
 * The box can be drawn in spherical and other non-Cartesian coordinate
 * systems by selecting the appropriate X and Y RealTypes.<P>
 * Sample usage:<P>
 * <PRE>
 *  RubberBandBox rbBox =
 *      new RubberBandBox(RealType.Latitude, RealType.Longitude);
 *  rbBox.addAction(new ActionImpl() {
 *      public void doAction()
 *          throws VisADException, RemoteException
 *      {
 *          Gridded2DSet bounds = rbBox.getBounds();
 *          (do something useful with the box)
 *      }
 *  });
 * </PRE>
 * @author  Don Murray
 * @version $Revision: 1.10 $
 */
public class RubberBandBox extends LineDrawing {

    /** x type for the box */
    private RealType xType;

    /** y type for the box */
    private RealType yType;

    /** renderer */
    private RubberBandBoxRendererJ3D rubberBandBox;

    /** bounds defined by the rubber band box */
    private Gridded2DSet bounds;

    /** mouse event mask */
    private int mask;

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param  xType   RealType of the X coordinate of the box
     * @param  yType   RealType of the Y coordinate of the box
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public RubberBandBox(RealType xType, RealType yType)
            throws VisADException, RemoteException {
        this(xType, yType, 0);
    }

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param xType   RealType of the X coordinate of the box
     * @param yType   RealType of the Y coordinate of the box
     * @param mask    key mask to use for rubberbanding
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public RubberBandBox(RealType xType, RealType yType, int mask)
            throws VisADException, RemoteException {

        super("Rubber Band Box");

        this.xType = xType;
        this.yType = yType;
        this.mask  = mask;
        bounds = new Gridded2DSet(new RealTupleType(xType, yType), null, 1);

        setData(bounds);
    }

    /**
     * Constructor for creating a RubberBandBox from another instance
     *
     * @param that  other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    protected RubberBandBox(RubberBandBox that)
            throws VisADException, RemoteException {

        super(that);

        this.xType  = that.xType;
        this.yType  = that.yType;
        this.bounds = that.bounds;
    }

    /**
     * Invoked when box mouse is released. Subclasses should invoke
     * super.dataChange() to ensure the the bounds are set.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void dataChange() throws VisADException, RemoteException {

        bounds = (Gridded2DSet) getData();

        /*  debug
            float[] highs = bounds.getHi();
            float[] lows = bounds.getLow();
            if (highs != null && lows != null)
                System.out.println("box: X range = " + lows[0] + " to " +
                                   highs[0] +
                      "; Y range = " + lows[1] + " to " + highs[1] );
        */
        super.dataChange();
    }

    /**
     * Return the bounds of the RubberBandBox.  The Gridded2DSet that
     * is returned contains the opposite (starting and ending) corners
     * of the box.
     *
     * @return  set containing the opposite corners of the box.
     */
    public Gridded2DSet getBounds() {
        return bounds;
    }

    /**
     * Get the range of values for the X RealType as a Linear1DSet
     *
     * @return  array of min and max values
     * @throws VisADException   VisAD error
     * public Linear1DSet getXRange()
     *   throws VisADException
     * {
     *   float[] highs = bounds.getHi();
     *   float[] lows = bounds.getLow();
     *   return
     *       (highs == null || lows == null)
     *           ? (Linear1DSet) null
     *           : new Linear1DSet(xType, lows[0], highs[0], 2);
     * }
     */

    /**
     * Get the range of values for the Y RealType
     *
     * @return  array of min and max values
     * @throws VisADException   VisAD error
     * public Linear1DSet getYRange()
     *   throws VisADException
     * {
     *   float[] highs = bounds.getHi();
     *   float[] lows = bounds.getLow();
     *   return
     *       (highs == null || lows == null)
     *           ? (Linear1DSet) null
     *           : new Linear1DSet(yType, lows[1], highs[1], 2);
     * }
     */

    /**
     * Get the DataRenderer used for this displayable.
     *
     * @return  RubberBandBoxRendererJ3D associated with this displayable
     */
    protected DataRenderer getDataRenderer() {

        rubberBandBox = new RubberBandBoxRendererJ3D(xType, yType, mask,
                mask);

        return rubberBandBox;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new RubberBandBox(this);
    }
}
