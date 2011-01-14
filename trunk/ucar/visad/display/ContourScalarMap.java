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



import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;


/**
 * Provides support for adapting ScalarMap-s for 2-D contour lines.
 *
 * <p>Instances of this class have the following, bound, JavaBean
 * properties:<br>
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
 * <td>contourLevels</td>
 * <td>{@link ContourLevels}</td>
 * <td>set/get</td>
 * <td>empty-set of levels</td>
 * <td align=left>The contour levels of this instance</td>
 * </tr>
 *
 * <tr align=center>
 * <td>labeling</td>
 * <td><code>boolean</code></td>
 * <td>set/get</td>
 * <td><code>false</code></td>
 * <td align=left>Whether or not contour-line labeling is enabled</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $
 */
public class ContourScalarMap extends IsoContourScalarMap {

    /**
     * The name of the "contour levels" property.
     */
    public static final String CONTOUR_LEVELS = "contourLevels";

    /**
     * The name of the "labeling" property.
     */
    public static final String LABELING = "labeling";

    /** initial contour levels */
    private static ContourLevels initialContourLevels =
        new IrregularContourLevels(new float[0]);

    /** contour levels */
    private ContourLevels contourLevels = initialContourLevels;

    /** minimum of contour range */
    private float rangeMinimum = Float.NaN;

    /** minimum of contour range */
    private float rangeMaximum = Float.NaN;

    /** flag for labeling (true to label) */
    private boolean labeling = false;

    /**
     * Constructs.
     *
     * @param realType          The type of data that is to be contoured.
     * @param display           The adapted VisAD display on which to contour
     *                          the data.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ContourScalarMap(RealType realType, DisplayAdapter display)
            throws VisADException, RemoteException {
        super(realType, display);
    }

    /**
     * Sets the labeling of contour lines.  This method fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #LABELING} with this
     * instance as the source and the old and new values appropriately set.
     * The event is fired <em>synchronously</em> -- so watch out for deadlock.
     *
     * @param on                Whether or not the contour lines should be
     *                          labeled.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized final void setLabeling(boolean on)
            throws VisADException, RemoteException {

        Boolean oldValue = new Boolean(labeling);

        this.labeling = on;

        autoSetLabeling();
        firePropertyChange(LABELING, oldValue, new Boolean(labeling));
    }

    /**
     * Indicates whether or not contour-line labeling is enabled.
     *
     * @return                  <code>true</code> if and only if the contour
     *                          lines are labeled.
     */
    public synchronized final boolean isLabeling() {
        return labeling;
    }

    /**
     * Sets the range of data to be contoured.
     *
     * @param min               The minimum, possible data value to be
     *                          considered
     * @param max               The maximum, possible data value to be
     *                          considered
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setRange(float min, float max)
            throws RemoteException, VisADException {

        rangeMinimum = min;
        rangeMaximum = max;

        autoSetContourLevels();
    }

    /**
     * Sets the contour values.  This method fires a
     * {@link java.beans.PropertyChangeEvent} for {@link #CONTOUR_LEVELS}
     * with this instance as the source and
     * the old and new values appropriately set.  The firing is done
     * <em>synchronously</em> -- so watch out for deadlock.
     *
     * @param contourLevels     The contour values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #CONTOUR_LEVELS
     */
    public synchronized final void setContourLevels(
            ContourLevels contourLevels)
            throws RemoteException, VisADException {

        ContourLevels oldValue = this.contourLevels;

        this.contourLevels = contourLevels;

        autoSetContourLevels();
        firePropertyChange(CONTOUR_LEVELS, oldValue, this.contourLevels);
    }

    /**
     * Returns the contour levels.
     *
     * @return          The Contour levels.  The initial value is an empty
     *                  set of levels.
     */
    public synchronized final ContourLevels getContourLevels() {
        return contourLevels;
    }

    /**
     * Sets the control of the underlying {@link visad.ScalarMap}.  This is a
     * template method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void setControl()
            throws VisADException, RemoteException {
        autoSetContourLevels();
        autoSetLabeling();
    }

    /**
     * Autoset the contour levels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void autoSetContourLevels()
            throws VisADException, RemoteException {

        if ((contourLevels != initialContourLevels)
                && (rangeMinimum == rangeMinimum)
                && (rangeMaximum == rangeMaximum)) {
            ContourControl control = getContourControl();

            if (control != null) {
                contourLevels.setControl(control, rangeMinimum, rangeMaximum);
            }
        }
    }

    /**
     * Autoset the labeling.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void autoSetLabeling() throws VisADException, RemoteException {

        ContourControl control = getContourControl();

        if (control != null) {
            control.enableLabels(labeling);
        }
    }
}
