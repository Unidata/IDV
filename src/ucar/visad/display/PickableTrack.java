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

import visad.bom.PickManipulationRendererJ3D;

import visad.util.DataUtility;

import java.awt.event.InputEvent;

import java.rmi.RemoteException;


/**
 * Provides support for a color coded display of a track trace.
 * @author Don Murray
 * @version $Revision: 1.5 $
 */
public class PickableTrack extends TrackDisplayable {

    /** pick renderer */
    private PickManipulationRendererJ3D picker = null;

    /** the data */
    private FlatField track = null;

    /** the color ScalarMap */
    private RealType rgbRealType = null;

    /**
     * Constructs an instance with the supplied reference name.
     *
     * @param  name  reference name
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public PickableTrack(String name) throws VisADException, RemoteException {
        super(name);
    }

    /**
     * Returns the closest data object near the picked point.
     * @return  Closest data point in the form of (lat,lon,alt) -> (value)
     */
    public FlatField getClosestData() {

        FlatField data = null;

        if ((track != null) && (picker != null)) {
            try {
                int index = picker.getCloseIndex();
                RealTuple domain =
                    DataUtility.getSample(track.getDomainSet(), index);

                data = new FlatField((FunctionType) track.getType(),
                                     new SingletonSet(domain));

                Data range = track.getSample(index);

                data.setSample(0, range);
            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                ;
            }
        }

        return data;
    }

    /**
     * Used to create a specialized renderer.  In this case, the
     * data renderer is a PickManipulationRendererJ3D
     * @return  renderer used in this instance
     */
    protected DataRenderer getDataRenderer() {

        int mask = InputEvent.SHIFT_MASK;

        picker = new PickManipulationRendererJ3D(mask, mask);

        return picker;
    }
}
