/*
 * $Id: MixingRatioTrajectory.java,v 1.13 2005/05/13 18:33:34 jeffmc Exp $
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

import ucar.visad.display.Displayable;

import visad.VisADException;

import visad.LocalDisplay;


/**
 * Provides support for displaying a constant mixing-ratio curve from a
 * saturation point to lower pressure.
 *
 * @author Steven R. Emmerson
 * @version $Id: MixingRatioTrajectory.java,v 1.13 2005/05/13 18:33:34 jeffmc Exp $
 */
public class MixingRatioTrajectory extends Trajectory {

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public MixingRatioTrajectory() throws RemoteException, VisADException {
        super("MixingRatioTrajectory",
              MixingRatioTemperatureCalculatorFactory.instance(), true);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected MixingRatioTrajectory(MixingRatioTrajectory that)
            throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new MixingRatioTrajectory(this);
    }
}







