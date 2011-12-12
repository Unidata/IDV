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

import visad.java2d.*;

import visad.java3d.*;

import java.awt.event.InputEvent;

import java.rmi.RemoteException;


/**
 * Provides support for a Displayable that comprises a set of
 * drawn curves.  The curves can be drawn in in spherical and
 * other non-Cartesian coordinate systems by selecting the
 * appropriate RealTypes or RealTupleType.<P>
 * Sample usage:<P>
 * <PRE>
 *  CurveDrawer curveDraw =
 *      new CurveDrawer(RealType.Latitude, RealType.Longitude);
 *  curveDraw.addAction(new ActionImpl() {
 *      public void doAction()
 *          throws VisADException, RemoteException
 *      {
 *          UnionSet curves = curveDraw.getData();
 *          (do something useful with the curves)
 *      }
 *  });
 * </PRE>
 * @author  Don Murray
 * @version $Revision: 1.7 $
 */
public class CurveDrawer extends LineDrawing {

    /** The type for the drawing space */
    private RealTupleType type;

    /** the set of drawn curves */
    private UnionSet curves;

    /** mask for mouse events */
    private int mask;

    /** data renderer */
    private DataRenderer curver;

    /**
     * Construct a CurveDrawer using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     * @param  xType   RealType of the X coordinate of the box
     * @param  yType   RealType of the Y coordinate of the box
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public CurveDrawer(RealType xType, RealType yType)
            throws VisADException, RemoteException {
        this(new RealTupleType(xType, yType), 0);
    }

    /**
     * Construct a CurveDrawer using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     * @param  xType   RealType of the X coordinate of the box
     * @param  yType   RealType of the Y coordinate of the box
     * @param  mask    key mask to use for mouse button
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public CurveDrawer(RealType xType, RealType yType, int mask)
            throws VisADException, RemoteException {
        this(new RealTupleType(xType, yType), mask);
    }

    /**
     * Construct a CurveDrawer using the RealTupleType
     * @param  type    RealTupleType of the drawing space
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public CurveDrawer(RealTupleType type)
            throws VisADException, RemoteException {
        this(type, 0);
    }

    /**
     * Construct a CurveDrawer using the RealTupleType of the drawing
     * space and a mask for the mouse
     * @param  type    RealTupleType of the drawing space
     * @param  mask    key mask to use for mouse button
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public CurveDrawer(RealTupleType type, int mask)
            throws VisADException, RemoteException {
        this(new UnionSet(new Gridded2DSet[] {
            new Gridded2DSet(type, new float[][] {
            { 0.0f }, { 0.0f }
        }, 1) }));
    }


    /**
     * Construct a CurveDrawer with a predefined set of curves.
     * @param curves  UnionSet of curves
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public CurveDrawer(UnionSet curves)
            throws VisADException, RemoteException {
        this(curves, 0);
    }

    /**
     * Construct a CurveDrawer with a predefined set of curves.
     * @param curves   UnionSet of curves
     * @param mask     key mask to use for mouse button
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public CurveDrawer(UnionSet curves, int mask)
            throws VisADException, RemoteException {

        super("Curve Drawer");

        this.type = ((SetType) curves.getType()).getDomain();
        this.mask = mask;
        setManipulable(true);
        setData(curves);
    }

    /**
     * Constructor for creating a CurveDrawer from another instance
     * @param that  other instance
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    protected CurveDrawer(CurveDrawer that)
            throws VisADException, RemoteException {

        super(that);

        this.type   = that.type;
        this.curves = that.curves;
        this.mask   = that.mask;
    }

    /**
     * Invoked when box mouse is released. Subclasses should invoke
     * super.dataChange() to ensure the the curves are set.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected void dataChange() throws VisADException, RemoteException {

        curves = (UnionSet) getData();
        super.dataChange();

    }

    /**
     * Return the curves of the CurveDrawer.  The UnionSet that
     * is returned contains the lines.
     * @return  set containing sets of curves
     */
    public UnionSet getCurves() {
        return curves;
    }

    /**
     * Set the curves of the CurveDrawer.  The input must have the
     * same MathType as this instance.
     * @param  curves  set of curves to display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setCurves(UnionSet curves)
            throws VisADException, RemoteException {

        if ( !((SetType) curves.getType()).getDomain().equals(type)) {
            throw new IllegalArgumentException("MathType of curve must be "
                    + type);
        }
        setData(curves);
    }


    /**
     * Set whether the curves are manipulable or not.
     *
     * @param b  true to enable
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setDrawingEnabled(boolean b)
            throws VisADException, RemoteException {
        setManipulable(b);
    }

    /**
     * Set whether the curves are manipulable or not.
     * @return true if drawing is enabled
     */
    public boolean getDrawingEnabled() {
        return isManipulable();
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new CurveDrawer(this);
    }

    /**
     * Returns the DataRenderer for this displayable.  This method does not
     * verify that the VisAD display has been set.
     * @return                  The DataRenderer associated with this
     *                          displayable.
     */
    protected DataRenderer getDataRenderer() {

        LocalDisplay display = getDisplay();

        return isManipulable()
               ? (display.getDisplayRenderer() instanceof DisplayRendererJ2D)
                 ? (DataRenderer) new CurveManipulationRendererJ2D()
                 : (DataRenderer) new CurveManipulationRendererJ3D()
               : (display.getDisplayRenderer() instanceof DisplayRendererJ2D)
                 ? (DataRenderer) new DefaultRendererJ2D()
                 : (DataRenderer) new DefaultRendererJ3D();
    }
}
