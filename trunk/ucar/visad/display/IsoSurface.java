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


import ucar.unidata.beans.*;

import visad.*;



import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Provides support for a Displayable that comprises an iso-surface
 * Supports color tables as well.
 *
 * <p>
 * Instances of this class have the following bound properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>surfaceValue</td>
 * <td>float</td>
 * <td>set/get</td>
 * <td>0</td>
 * <td align=left>The displayed contour iso-surface value associated with this
 * instance.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>contourRealType</td>
 * <td>visad.RealType</td>
 * <td>set/get</td>
 * <td><code>null</code></td>
 * <td align=left>The VisAD type of the contoured quantity.</td>
 * </tr>
 *
 * </table>
 *
 * @author Don Murray
 * @version $Revision: 1.15 $
 */
public abstract class IsoSurface extends RGBDisplayable {

    /**
     * The name of the "surface value" property.
     */
    public static final String SURFACE_VALUE = "surfaceValue";

    /**
     * The name of the "contour real-type" property.
     */
    public static final String CONTOUR_REAL_TYPE = "contourRealType";

    /**
     * The contour levels.
     */
    private volatile float surfaceValue;

    /** contour ScalarMap */
    private volatile ScalarMap contourMap;

    /** control for ScalarMap */
    private volatile ContourControl contourControl;

    /** type for ScalarMap */
    private volatile RealType contourRealType;

    /**
     * Constructs from a name for the Displayable and the type of the
     * iso-surface parameter.
     *
     * @param name              The name for the displayable.
     * @param contourRealType   The type of the iso-surface parameter.  May be
     *                          <code>null</code>.
     * @param alphaflag         boolean flag whether to use transparency
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public IsoSurface(String name, RealType contourRealType,
                      boolean alphaflag)
            throws VisADException, RemoteException {

        super(name, contourRealType, alphaflag);

        this.contourRealType = contourRealType;

        if (contourRealType != null) {
            setContourMaps();
        }
    }

    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: surface value, the contour RealType.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected IsoSurface(IsoSurface that)
            throws VisADException, RemoteException {

        super(that);

        surfaceValue    = that.surfaceValue;
        contourRealType = that.contourRealType;  // immutable object

        if (contourRealType != null) {
            setContourMaps();
        }
    }

    /**
     * Sets the RealType of the contoured parameter.
     * @param realType          The RealType of the contoured parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setContourRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(contourRealType)) {
            RealType oldValue = contourRealType;

            contourRealType = realType;

            setContourMaps();
            firePropertyChange(CONTOUR_REAL_TYPE, oldValue, contourRealType);
        }
    }

    /**
     * Set the units for the displayed range
     * @param unit Unit for display
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplayUnit(Unit unit)
            throws VisADException, RemoteException {
        //Make sure this unit is ok
        //For now lets comment this out
        //      checkUnit (contourRealType, unit);
        super.setDisplayUnit(unit);
        //      applyDisplayUnit (contourMap, contourRealType);
    }






    /**
     * Returns the RealType of the contoured parameter.
     * @return                  The RealType of the contoured parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getContourRealType() {
        return contourRealType;
    }

    /**
     * Sets the "contour" value, the value of the isosurface.
     * This method fires a PropertyChangeEvent for
     * SURFACE_VALUE.
     * @param value     The value of the isosurface.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #SURFACE_VALUE
     */
    public final void setSurfaceValue(float value)
            throws RemoteException, VisADException {

        if (value != surfaceValue) {
            Float oldValue = new Float(surfaceValue);

            if (contourControl != null) {
                contourControl.setSurfaceValue(value, true /*setLevels*/);
            }

            surfaceValue = value;

            firePropertyChange(SURFACE_VALUE, oldValue,
                               new Float(surfaceValue));
        }
    }

    /**
     * Sets the set of ScalarMap-s of this instance.  The ScalarMap-s of
     * this instance will be added to the set before the SCALAR_MAP_SET
     * property is set.  This method fires a PropertyChangeEvent for
     * SCALAR_MAP_SET with <code>null</code> for the old value and the new
     * set of ScalarMap-s for the new Value.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * <code>super.setScalarMaps(ScalarMapSet)</code>.
     * @param maps              The set of ScalarMap-s to be added.
     * @throws BadMappingException      The RealType of the contour parameter
     *                          has not been set or its ScalarMap is alread in
     *                          the set.
     */
    protected void setScalarMaps(ScalarMapSet maps)
            throws BadMappingException {

        if (contourMap == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "Contour parameter not yet set");
        }

        maps.add(contourMap);
        super.setScalarMapSet(maps);
    }

    /**
     * Gets the value of the isosurface
     * @return  float value of the isosurface
     */
    public final float getSurfaceValue() {
        return surfaceValue;
    }


    /**
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setContourMaps() throws RemoteException, VisADException {

        ScalarMap oldContourMap = contourMap;

        contourMap = new ScalarMap(contourRealType, Display.IsoContour);

        contourMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    contourControl = (ContourControl) contourMap.getControl();

                    contourControl.setSurfaceValue(surfaceValue, true);
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });

        //For now comment this out
        //applyDisplayUnit (contourMap, contourRealType);

        replaceScalarMap(oldContourMap, contourMap);

        // this moved to Grid3dDisplayable; and super is superfluous
        //super.setRGBRealType(contourRealType);

        fireScalarMapSetChange();
    }
}
