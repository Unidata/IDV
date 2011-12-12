/*
 * $Id: MeanWindTrace.java,v 1.15 2005/05/13 18:33:33 jeffmc Exp $
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
 * Provides support for displaying the mean wind as a trace (ex: a point).
 *
 * The VisAD MathType of a mean wind is the TupleType (GeopotentialAltitude,
 * (WesterlyWind, SoutherlyWind)).
 *
 * @author Steven R. Emmerson
 * @version $Id: MeanWindTrace.java,v 1.15 2005/05/13 18:33:33 jeffmc Exp $
 */
public class MeanWindTrace extends LineDrawing {

    /**
     * Constructs from the display types.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public MeanWindTrace() throws VisADException, RemoteException {

        super("MeanWindTrace");

        setManipulable(false);
        setPointSize(4);
        setData(MeanWindCell.getMissing());

        /*

        ScalarMap       map =
            new ScalarMap(WesterlyWind.getRealType(), displayUType);
        map.setRangeByUnits();
        addScalarMap(map);

        map = new ScalarMap(SoutherlyWind.getRealType(), displayVType);
        map.setRangeByUnits();
        addScalarMap(map);
        */
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected MeanWindTrace(MeanWindTrace that)
            throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Sets the mean wind.
     * @param meanWind          The mean wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWind(Tuple meanWind)
            throws VisADException, RemoteException {
        setData(meanWind);
    }

    /**
     * Sets the mean wind.
     * @param meanWindRef       The data reference for the mean wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWind(DataReference meanWindRef)
            throws VisADException, RemoteException {
        setDataReference(meanWindRef);
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
        return new MeanWindTrace(this);
    }
}







