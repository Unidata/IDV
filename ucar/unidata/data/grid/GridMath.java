/*
 *
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.grid;


import ucar.unidata.util.Misc;

import visad.*;

import java.rmi.RemoteException;


/**
 * A class to handle grid math.  This handles math between grids on
 * different manifolds where possible.
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GridMath {


    /**
     * Add two grids together
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the sum of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl add(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return doMath(grid1, grid2, Data.ADD);
    }

    /**
     * Subtract two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the difference of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl subtract(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return doMath(grid1, grid2, Data.SUBTRACT);
    }

    /**
     * Multiply two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the product of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl multiply(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return doMath(grid1, grid2, Data.MULTIPLY);
    }

    /**
     * Divide two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the quotient of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl divide(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return doMath(grid1, grid2, Data.DIVIDE);
    }

    /**
     * Do the math.  This method handles making the data as compatible
     * as possible before actually going off and doing the math.
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param op  the VisAD math operand  (see visad.Data)
     *
     * @return the grid
     *
     * @throws VisADException  problem doing the math
     */
    private static FieldImpl doMath(FieldImpl grid1, FieldImpl grid2, int op)
            throws VisADException {
        FieldImpl a         = grid1;
        FieldImpl b         = grid2;
        boolean   is3D1     = GridUtil.is3D(grid1);
        boolean   is3D2     = GridUtil.is3D(grid2);
        boolean   isVolume1 = GridUtil.isVolume(grid1);
        boolean   isVolume2 = GridUtil.isVolume(grid2);
        boolean   isSlice1  = !isVolume1 && is3D1;
        boolean   isSlice2  = !isVolume2 && is3D2;
        if (isSlice1 && isSlice2) {
            if ( !Misc.equals(GridUtil.getSpatialDomain(grid1),
                              GridUtil.getSpatialDomain(grid2))) {
                a = GridUtil.make2DGridFromSlice(grid1, false);
                b = GridUtil.make2DGridFromSlice(grid2, false);
            }
        } else if (isSlice1 && !is3D2) {
            a = GridUtil.make2DGridFromSlice(grid1, false);
        } else if ( !is3D1 && isSlice2) {
            b = GridUtil.make2DGridFromSlice(grid2, false);
        }
        int mode = (Misc.equals(GridUtil.getSpatialDomain(a),
                                GridUtil.getSpatialDomain(b)))
                   ? Data.NEAREST_NEIGHBOR
                   : Data.WEIGHTED_AVERAGE;
        return (FieldImpl) binary(a, b, op, mode, Data.NO_ERRORS);
    }

    /**
     * Wrapper for visad.Data.binary
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param op  the VisAD math operand  (see visad.Data)
     * @param samplingMode  sampling mode
     * @param errorMode  error mode
     *
     * @return  the result  or null
     *
     * @throws VisADException  problem doing the math
     */
    private static FieldImpl binary(FieldImpl grid1, FieldImpl grid2, int op,
                                    int samplingMode, int errorMode)
            throws VisADException {
        try {
            return (FieldImpl) grid1.binary(grid2, op, samplingMode,
                                            errorMode);
        } catch (RemoteException re) {}
        return null;
    }
}
