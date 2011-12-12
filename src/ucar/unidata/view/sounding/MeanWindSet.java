/*
 * $Id: MeanWindSet.java,v 1.12 2005/05/13 18:33:33 jeffmc Exp $
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



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import ucar.visad.display.*;

import visad.*;


/**
 * Provides support for a CompositeDisplayable of mean winds.
 *
 * @author Steven R. Emmerson
 * @version $Id: MeanWindSet.java,v 1.12 2005/05/13 18:33:33 jeffmc Exp $
 */
public class MeanWindSet extends CompositeDisplayable {

    /**
     * The name of the active mean-wind Displayable property.
     */
    public static String ACTIVE_MEAN_WIND = "activeMeanWind";

    /** active mean wind index */
    private int activeMeanWindIndex;

    /** active mean wind display */
    private Displayable activeMeanWind;

    /** missing mean wind display */
    private Displayable missingMeanWind;

    /**
     * Constructs from a prototype Displayable with a missing value and a
     * VisAD display.
     * @param missingMeanWind   A prototype Displayable with a missing value.
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public MeanWindSet(Displayable missingMeanWind, LocalDisplay display)
            throws VisADException, RemoteException {

        super(display);

        this.missingMeanWind = missingMeanWind;
        activeMeanWind       = missingMeanWind;
    }

    /**
     * Adds a mean-wind Displayable to this composite.
     * @param index             The index of the mean-wind Displayable.
     * @param meanWind          The mean-wind Displayable.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setMeanWind(int index, Displayable meanWind)
            throws RemoteException, VisADException {
        setDisplayable(index, meanWind);
    }

    /**
     * Removes a mean-wind Displayable from this composite.
     * @param index             The index of the mean-wind Displayable.
     * @return                  The mean-wind Displayable that was removed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws IndexOutOfBoundsException
     *                          The index was out of range.
     */
    public synchronized Displayable removeMeanWind(int index)
            throws RemoteException, VisADException,
                   IndexOutOfBoundsException {

        Displayable meanWind = removeDisplayable(index);

        if ((displayableCount() == 0) || (meanWind == activeMeanWind)) {
            setActiveMeanWind(missingMeanWind);
        }

        return meanWind;
    }

    /**
     * Sets the active mean-wind Displayable.
     * @param index             The index of the active mean-wind Displayable.
     *                          If the index is out of range, then the active
     *                          mean-wind is set the the missing mean-wind
     *                          Displayable.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setActiveMeanWind(int index)
            throws RemoteException, VisADException {

        setActiveMeanWind((index < 0)
                          ? missingMeanWind
                          : (Displayable) getDisplayable(index));
    }

    /**
     * Sets the active mean-wind property.
     * @param meanWind          The new value.
     */
    protected synchronized void setActiveMeanWind(Displayable meanWind) {

        Displayable old = activeMeanWind;

        activeMeanWind = meanWind;

        firePropertyChange(ACTIVE_MEAN_WIND, old, activeMeanWind);
    }

    /**
     * Returns the active mean-wind property.
     * @return                  The active mean-wind property.
     */
    public Displayable getActiveMeanWind() {
        return activeMeanWind;
    }

    /**
     * Clears the mean-wind Displayable-s from this composite.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void clear() throws VisADException, RemoteException {

        for (int i = 0; i < displayableCount(); ++i) {
            removeMeanWind(i);
        }
    }
}







