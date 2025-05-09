package edu.wisc.ssec.mcidasv.data.hydra;

import ucar.unidata.data.grid.GridUtil;
import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;

import visad.*;

import java.rmi.RemoteException;

import visad.georef.MapProjection;

public class SubsetRubberBandBox extends LineDrawing {

    /**
     * x type for the box
     */
    private RealType xType;

    /**
     * y type for the box
     */
    private RealType yType;

    /**
     * renderer
     */
    private MyRubberBandBoxRendererJ3D rubberBandBox;

    /**
     * bounds defined by the rubber band box
     */
    private Gridded2DSet bounds;

    /**
     * mouse event mask
     */
    private int mask;

    private FlatField data;
    private boolean isLL;
    private boolean lastBoxOn;

    private CoordinateSystem dataCS;

    private CoordinateSystem displayCS;

    private static int count = 0;

    ScalarMap elemMap;
    ScalarMap lineMap;

    private boolean flipY = false;
    private boolean flipX = false;

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException Remote error
     */
    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS)
            throws VisADException, RemoteException {
        this(data, displayCS, 0);
    }

    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(data, displayCS, mask, true);
    }

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param mask  key mask to use for rubberbanding
     * @throws VisADException  VisAD error
     * @throws RemoteException Remote error
     */
    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask, boolean lastBoxOn)
            throws VisADException, RemoteException {
        super("Subset Rubber Band Box");

        this.data = data;
        GriddedSet domainSet0   = (GriddedSet) data.getDomainSet();
        this.displayCS = displayCS;
        this.isLL = GridUtil.isLatLonOrder(domainSet0);
        this.lastBoxOn = lastBoxOn;

        boolean inverseCS = false;

        RealTupleType rtype = ((FunctionType) data.getType()).getDomain();
        dataCS = rtype.getCoordinateSystem();
        if (dataCS == null) {
            dataCS = new GridCoordinateSystem((GriddedSet) data.getDomainSet());
        } else if (dataCS instanceof MapProjection) {
            flipX = ((MapProjection) dataCS).getFlipX();
            flipY = ((MapProjection) dataCS).getFlipY();
        }

        if (displayCS instanceof InverseLinearScaledCS) {
            inverseCS = ((InverseLinearScaledCS) displayCS).getInvertedCoordinateSystem().equals(dataCS);
        }

        IdentityCoordinateSystem iCS =
                new IdentityCoordinateSystem(
                        new RealTupleType(new RealType[]{RealType.getRealType("ZZtop")}));

        CoordinateSystem cs =
                new CartesianProductCoordinateSystem(new CoordinateSystem[]{dataCS, iCS});


        DisplayRealType displayLineType;
        DisplayRealType displayElemType;

        if (!inverseCS) {
            displayLineType = new DisplayRealType("displayLine_" + count, true, 0.0, 10000.0, 0.0, null);
            displayElemType = new DisplayRealType("displayElem_" + count, true, 0.0, 10000.0, 0.0, null);
        } else {
            displayLineType = new DisplayRealType("displayLine_" + count, true, -1.0, 1.0, 0.0, null);
            displayElemType = new DisplayRealType("displayElem_" + count, true, -1.0, 1.0, 0.0, null);
        }
        DisplayRealType displayAltType = new DisplayRealType("displayAlt_" + count, true, -1.0, 1.0, 0.0, null);

        RealType elemType = RealType.getRealType("elem_" + count);
        RealType lineType = RealType.getRealType("line_" + count);
        this.xType = elemType;
        this.yType = lineType;
        this.mask = mask;
        bounds = new Gridded2DSet(new RealTupleType(xType, yType), null, 1);

        elemMap = new ScalarMap(elemType, displayElemType);
        lineMap = new ScalarMap(lineType, displayLineType);
        GriddedSet domainSet = (GriddedSet) data.getDomainSet();
        float[] low = domainSet.getLow();
        float[] hi = domainSet.getHi();

        elemMap.setRange(low[0], hi[0]);
        if (!flipY) {
            lineMap.setRange(low[1], hi[1]);
        } else {
            lineMap.setRange(hi[1], low[1]);
        }

        CoordinateSystem new_cs;

        if (!inverseCS) {
            new_cs = new DataToDisplayCoordinateSystem(isLL, cs, displayCS);
        } else {
            new_cs = new DataToDisplayCoordinateSystemInv(lineMap, elemMap, flipY, flipX);
        }

        DisplayTupleType dtt = new DisplayTupleType(new DisplayRealType[]{displayElemType, displayLineType, displayAltType}, new_cs);

        addScalarMap(elemMap);
        addScalarMap(lineMap);

        setData(bounds);
        count += 1;
    }

    /**
     * Constructor for creating a RubberBandBox from another instance
     *
     * @param that other instance
     * @throws VisADException  VisAD error
     * @throws RemoteException Remote error
     */
    protected SubsetRubberBandBox(SubsetRubberBandBox that)
            throws VisADException, RemoteException {

        super(that);

        this.xType = that.xType;
        this.yType = that.yType;
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
    }

    /**
     * Return the bounds of the RubberBandBox.  The Gridded2DSet that
     * is returned contains the opposite (starting and ending) corners
     * of the box.
     *
     * @return set containing the opposite corners of the box.
     */
    public Gridded2DSet getBounds() {
        return bounds;
    }

    /**
     * Get the DataRenderer used for this displayable.
     *
     * @return RubberBandBoxRendererJ3D associated with this displayable
     */
    protected DataRenderer getDataRenderer() {
        rubberBandBox = new MyRubberBandBoxRendererJ3D(xType, yType, mask,
                mask);
        rubberBandBox.setKeepLastBoxOn(lastBoxOn);

        return rubberBandBox;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return A semi-deep clone of this instance.
     * @throws VisADException  VisAD failure.
     * @throws RemoteException Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new SubsetRubberBandBox(this);
    }

    public void setBox(SubsetRubberBandBox rbb) {
        rubberBandBox.setLastBox((MyRubberBandBoxRendererJ3D) rbb.getDataRenderer());
    }

    public Gridded3DSet getLastBox() {
        return rubberBandBox.last_box;
    }
}


class DataToDisplayCoordinateSystem extends CoordinateSystem {
    private CoordinateSystem dataCS;
    private CoordinateSystem displayCS;
    private boolean isLL;


    DataToDisplayCoordinateSystem(boolean isLL, CoordinateSystem dataCS, CoordinateSystem displayCS) throws VisADException {
        super(displayCS.getReference(), null);
        this.dataCS = dataCS;
        this.displayCS = displayCS;
        this.isLL = isLL;
    }

    public float[][] toReference(float[][] values) throws VisADException {
        float[][] new_values = dataCS.toReference(values);
        if (isLL) {
            new_values = new float[][]{new_values[1], new_values[0], new_values[2]};
        }
        new_values = displayCS.toReference(new float[][]{new_values[1], new_values[0], new_values[2]});
        return new_values;
    }

    public float[][] fromReference(float[][] values) throws VisADException {
        float[][] new_values = displayCS.fromReference(values);
        if (isLL) {
            new_values = new float[][]{new_values[1], new_values[0], new_values[2]};
        }
        new_values = dataCS.fromReference(new float[][]{new_values[1], new_values[0], new_values[2]});

        return new_values;
    }

    public double[][] toReference(double[][] values) throws VisADException {
        double[][] new_values = dataCS.toReference(values);
        if (isLL) {
            new_values = new double[][]{new_values[1], new_values[0], new_values[2]};
        }
        new_values = displayCS.toReference(new double[][]{new_values[1], new_values[0], new_values[2]});

        return new_values;
    }

    public double[][] fromReference(double[][] values) throws VisADException {
        double[][] new_values = displayCS.fromReference(values);
        if (isLL) {
            new_values = new double[][]{new_values[1], new_values[0], new_values[2]};
        }
        new_values = dataCS.fromReference(new double[][]{new_values[1], new_values[0], new_values[2]});
        return new_values;
    }

    public boolean equals(Object obj) {
        return true;
    }
}

class DataToDisplayCoordinateSystemInv extends CoordinateSystem {
    ScalarMap elemMap;
    ScalarMap lineMap;
    double[] elemRng;
    double[] lineRng;
    boolean flipX;
    boolean flipY;


    DataToDisplayCoordinateSystemInv(ScalarMap lineMap, ScalarMap elemMap, boolean flipY, boolean flipX) throws VisADException {
        super(Display.DisplaySpatialCartesianTuple, null);
        this.elemMap = elemMap;
        this.lineMap = lineMap;
        this.elemRng = elemMap.getRange();
        this.lineRng = lineMap.getRange();
        this.flipY = flipY;
        this.flipX = flipX;
    }

    public float[][] toReference(float[][] values) throws VisADException {
        float[] vals0 = elemMap.scaleValues(values[0], true);
        float[] vals1 = lineMap.scaleValues(values[1], true);
        return new float[][]{vals0, vals1, values[2]};
    }

    public float[][] fromReference(float[][] values) throws VisADException {
        float[] vals0 = elemMap.inverseScaleValues(values[0], true);
        float[] vals1 = lineMap.inverseScaleValues(values[1], true);

        // Check range to prevent drawing box (selecting coordinates) outside the data's domain
        for (int k = 0; k < vals0.length; k++) {
            float val = vals0[k];
            if (!flipX) {
                if (!(val >= elemRng[0] && val <= elemRng[1])) vals0[k] = Float.NaN;
            } else {
                if (!(val >= elemRng[1] && val <= elemRng[0])) vals0[k] = Float.NaN;
            }

            val = vals1[k];
            if (!flipY) {
                if (!(val >= lineRng[0] && val <= lineRng[1])) vals1[k] = Float.NaN;
            } else {
                if (!(val >= lineRng[1] && val <= lineRng[0])) vals1[k] = Float.NaN;
            }
        }

        return new float[][]{vals0, vals1, values[2]};
    }

    public double[][] toReference(double[][] values) throws VisADException {
        return values;
    }

    public double[][] fromReference(double[][] values) throws VisADException {
        return values;
    }

    public boolean equals(Object obj) {
        return true;
    }

}