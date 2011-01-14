/*
 * $Id: AerologicalCoordinateSystem.java,v 1.11 2005/05/13 18:33:22 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import visad.*;


/**
 * Provides support for converting between the (x,y) coordinates on a
 * thermodynamic diagram and (pressure,temperature) coordinates.</p>
 *
 * <p>Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: AerologicalCoordinateSystem.java,v 1.11 2005/05/13 18:33:22 jeffmc Exp $
 */
public abstract class AerologicalCoordinateSystem extends CoordinateSystem {

    /**
     * Constructs from a reference vector space and units.
     * @param reference         The reference vector space.
     * @param units             The units of this vector space.
     * @throws VisADException   VisAD failure.
     */
    protected AerologicalCoordinateSystem(RealTupleType reference, Unit[] units)
            throws VisADException {
        super(reference, units);
    }

    /**
     * Gets the minimum X coordinate.
     *
     * @return                  The minimum X coordinate.
     */
    public abstract Real getMinimumX();

    /**
     * Gets the maximum X coordinate.
     *
     * @return                  The maximum X coordinate.
     */
    public abstract Real getMaximumX();

    /**
     * Gets the minimum Y coordinate.
     *
     * @return                  The minimum Y coordinate.
     */
    public abstract Real getMinimumY();

    /**
     * Gets the maximum Y coordinate.
     *
     * @return                  The maximum Y coordinate.
     */
    public abstract Real getMaximumY();

    /**
     * Gets the minimum pressure.
     * @return                  The minimum pressure.
     */
    public abstract Real getMinimumPressure();

    /**
     * Gets the maximum pressure.
     * @return                  The maximum pressure.
     */
    public abstract Real getMaximumPressure();

    /**
     * Gets the minimum temperature.
     * @return                  The minimum temperature.
     */
    public abstract Real getMinimumTemperature();

    /**
     * Gets the maximum temperature.
     * @return                  The maximum temperature.
     */
    public abstract Real getMaximumTemperature();

    /**
     * Create a 3D spatial coordinateSystem from the existing
     * one.
     * @param acs   existing coordinate system
     * @return 3D version of acs
     * @throws VisADException  illegal type or unable to create CS
     */
    public abstract AerologicalCoordinateSystem createDisplayCoordinateSystem(AerologicalCoordinateSystem acs)
     throws VisADException;
}
