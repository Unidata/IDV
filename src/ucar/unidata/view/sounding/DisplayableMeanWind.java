/*
 * $Id: DisplayableMeanWind.java,v 1.11 2005/05/13 18:33:28 jeffmc Exp $
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



import java.rmi.RemoteException;

import ucar.visad.display.*;

import visad.*;


/**
 * Supports the display of a mean wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: DisplayableMeanWind.java,v 1.11 2005/05/13 18:33:28 jeffmc Exp $
 */
public abstract class DisplayableMeanWind extends LineDrawing {

    /** data renderer */
    private final DataRenderer dataRenderer;

    /**
     * Constructs from nothing.  The DataRenderer will be the default one.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected DisplayableMeanWind() throws VisADException, RemoteException {
        this(null);
    }

    /**
     * Constructs with a given VisAD DataRenderer.
     * @param dataRenderer      The VisAD DataRenderer.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected DisplayableMeanWind(DataRenderer dataRenderer)
            throws VisADException, RemoteException {

        super("MeanWind");

        this.dataRenderer = dataRenderer;
    }

    /**
     * Sets the mean wind.
     * @param meanWind          The mean wind (geopotentialAltitude, (u, v)).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setMeanWind(Tuple meanWind)
            throws VisADException, RemoteException {
        setData(meanWind);
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof DisplayableMeanWind)) {
            equals = false;
        } else {
            DisplayableMeanWind that = (DisplayableMeanWind) obj;

            equals = (this == that) || ((dataRenderer == null)
                                        ? that.dataRenderer == null
                                        : dataRenderer
                                            .equals(that
                                                .dataRenderer)) && super
                                                    .equals(that);
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        return ((dataRenderer == null)
                ? 0
                : dataRenderer.hashCode()) ^ super.hashCode();
    }

    /**
     * Returns the VisAD DataRenderer associated with this displayable.
     * @return                  The DataRenderer associated with this
     *                          displayable.
     * @throws VisADException   VisAD failure.
     */
    protected DataRenderer getDataRenderer() throws VisADException {

        return (dataRenderer == null)
               ? super.getDataRenderer()
               : dataRenderer;
    }
}







