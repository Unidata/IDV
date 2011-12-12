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


import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.JythonManager;

import visad.*;

import java.rmi.RemoteException;


/**
 * Class for displaying grid point values as text
 *
 * @author IDV Development Team
 * @version $Revision: 1.15 $
 */
public class GridValueDisplayable extends StationModelDisplayable implements GridDisplayable {

    /**
     * Default constructor
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public GridValueDisplayable() throws VisADException, RemoteException {
        this("Grid_Value");
    }

    /**
     * Construct with the given name.
     * @param name  name must contain no spaces.
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public GridValueDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, null);
    }

    /**
     * Construct with the given name.
     * @param name  name must contain no spaces.
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public GridValueDisplayable(String name, JythonManager jythonManager)
            throws VisADException, RemoteException {
        super(name, jythonManager);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected GridValueDisplayable(GridValueDisplayable that)
            throws RemoteException, VisADException {

        super(that);

    }

    /**
     * Clone this {@link Displayable} so it can go into a different
     * display.
     *
     * @return clone of this object
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new GridValueDisplayable(this);
    }

    /**
     * Set the data into the Displayable
     *
     * @param field a VisAD FlatField with a 2D nature
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {


        FieldImpl stationData;
        if ( !isPointObs(field)) {
            stationData = GridUtil.getGridAsPointObs(field);
        } else {
            stationData = field;
        }
        setStationData(stationData);
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.  This implementation is a no-op.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {}

    /**
     * _more_
     *
     * @param field _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private boolean isPointObs(FieldImpl field)
            throws VisADException, RemoteException {
        FieldImpl test = null;
        if (GridUtil.isTimeSequence(field)) {
            test = (FieldImpl) field.getSample(0);
        } else {
            test = field;
        }
        return RealType.getRealType("index").equalsExceptNameButUnits(
            ((SetType) test.getDomainSet().getType()).getDomain());
    }
}
