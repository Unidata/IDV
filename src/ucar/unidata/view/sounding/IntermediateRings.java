/*
 * $Id: IntermediateRings.java,v 1.18 2005/05/13 18:33:31 jeffmc Exp $
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
 * Provides support for intermediate-level rings in a 3D wind hodograph.
 *
 * @author Steven R. Emmerson
 * @version $Id: IntermediateRings.java,v 1.18 2005/05/13 18:33:31 jeffmc Exp $
 */
public class IntermediateRings extends DisplayableData {

    /**
     * The object to be displayed.
     */
    private Field ringField;

    /**
     * The set of intermediate-level altitudes.
     */
    private Gridded1DSet altitudes;

    /**
     * The template for the rings.
     */
    private SampledSet ringSetTemplate;

    /**
     * The type of the vector space.
     */
    private RealTupleType polarType;

    /**
     * Constructs from a type for the vector space.
     * @param polarType         The type for the vector space.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public IntermediateRings(RealTupleType polarType)
            throws VisADException, RemoteException {

        super("IntermediateRings");

        this.polarType = polarType;
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected IntermediateRings(IntermediateRings that)
            throws RemoteException, VisADException {

        super(that);

        ringField       = (Field) that.getData();  // from superclass
        altitudes       = that.altitudes;          // immutable object
        ringSetTemplate = that.ringSetTemplate;    // immutable object
        polarType       = that.polarType;
    }

    /**
     * Sets the altitudes for the rings.
     * @param altitudes         The altitudes for the rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setAltitudes(Gridded1DSet altitudes)
            throws VisADException, RemoteException {

        if (ringSetTemplate != null) {
            ringField = computeRingField(altitudes, ringSetTemplate);

            setData(ringField);
        }

        this.altitudes = altitudes;
    }

    /**
     * Sets the speeds for the rings.
     * @param speeds            The speeds for the rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingSpeeds(Gridded1DSet speeds)
            throws VisADException, RemoteException {

        ringSetTemplate = RingSet.newUnionSet(polarType, speeds);

        if (altitudes != null) {
            ringField = computeRingField(altitudes, ringSetTemplate);

            setData(ringField);
        }
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
        return new IntermediateRings(this);
    }

    /**
     * Computes the data object to be displayed from a set of altitudes and
     * a ring template.
     * @param altitudes         The set of altitudes.
     * @param ringSetTemplate   The ring template.
     * @return                  The Field of rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected static Field computeRingField(Gridded1DSet altitudes, SampledSet ringSetTemplate)
            throws VisADException, RemoteException {

        Field ringField =
            new FieldImpl(new FunctionType(((SetType) altitudes.getType())
                .getDomain(), ringSetTemplate.getType()), altitudes);
        int altitudeCount = ringField.getLength();

        for (int i = 0; i < altitudeCount; ++i) {
            ringField.setSample(i, ringSetTemplate, false);
        }

        return ringField;
    }
}







