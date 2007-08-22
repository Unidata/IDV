/*
 * $Id: WindStaff.java,v 1.28 2006/05/02 14:54:27 dmurray Exp $
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

import java.util.*;

import java.beans.*;

import ucar.visad.display.*;

import visad.*;


/**
 * Provides support for the display of a wind profile as a collection of wind
 * arrows.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindStaff.java,v 1.28 2006/05/02 14:54:27 dmurray Exp $
 */
public class WindStaff extends WindProfile {

    /** composite of wind arrows */
    private CompositeDisplayable arrows;

    /** listener for the arrows */
    private ArrowWindListener arrowWindListener;

    /** listener for the arrows map changes */
    private ArrowMapSetListener arrowMapSetListener;

    /** DataReference for the profile */
    private DataReferenceImpl profileRef;

    /** action for changes to the profile */
    private ActionImpl profileAction;

    /** Type for the arrows */
    private TupleType tupleType;

    /** missing wind field */
    private FlatField missingWindField;

    /** original field */
    private Field originalData;

    /**
     * Constructs from a VisAD display.
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindStaff(LocalDisplay display)
            throws VisADException, RemoteException {
        this(display, WindArrow.getDefaultTupleType());
    }

    /**
     * Constructs from a VisAD display.
     * @param display           The VisAD display.
     * @param tupleType         TupleType for arrows
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindStaff(LocalDisplay display, TupleType tupleType)
            throws VisADException, RemoteException {

        this.tupleType = tupleType;
        missingWindField = new FlatField(
            new FunctionType(
                tupleType.getComponent(0), tupleType.getComponent(
                    1)), new SingletonSet(
                        new RealTuple(
                            new Real[]{ new Real(
                                (RealType) tupleType.getComponent(0), 0) })));
        arrows = new CompositeDisplayable(display);
        addDisplayable(arrows);
        arrowWindListener = new ArrowWindListener();

        // arrowMapSetListener = new ArrowMapSetListener();
        setRefAndAction(missingWindField);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindStaff(WindStaff that)
            throws RemoteException, VisADException {

        super(that);

        missingWindField  = that.missingWindField;
        arrows = (CompositeDisplayable) that.arrows.cloneForDisplay();
        arrowWindListener = new ArrowWindListener();

        // arrowMapSetListener = new ArrowMapSetListener();
        setRefAndAction(that.getProfile());  // same data
    }

    /**
     * Returns an instance of a wind field with no values.
     *
     * @return                 A wind field with no values.
     */
    protected FlatField getMissingWindField() {
        return missingWindField;
    }

    /**
     *
     * @param profile
     * @throws VisADException  if a necessary VisAD object couldn't be created.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    private void setRefAndAction(Field profile)
            throws VisADException, RemoteException {

        profileRef = new DataReferenceImpl("WindStaffProfileFieldRef");

        //profileRef.setData(profile);
        setProfile(profile);

        profileAction = new ActionImpl("WindStaffProfileFieldListener") {

            public void doAction() throws RemoteException, VisADException {
                setWindArrows();
            }
        };

        profileAction.addReference(profileRef);
    }

    /**
     * Sets the wind profile.
     * @param profile           The wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfile(Field profile)
            throws VisADException, RemoteException {
        profileRef.setData((Field) profile.dataClone());
        originalData = profile;
    }

    /**
     * Resets the vertical profile of the horizontal wind to the profile of
     * the last setProfile().
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfile() throws VisADException, RemoteException {
        setProfile(originalData);
    }

    /**
     * Sets the visibility of this instance.
     * @param visible           Whether or not this instance is to be visible.
     *
     * @param display
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * public void setVisible(boolean visible)
     *       throws RemoteException, VisADException {
     *   arrows.setVisible(visible);
     * }
     */

    /**
     * Associates this instance with a given VisAD display.  If this instance
     * was previously associated with a VisAD display, then it is first
     * removed from that display.  This method fires a PropertyChangeEvent
     * for DISPLAY with the old and new values.  This method will not
     * cause this instance to be rendered in the display.  This method may
     * be overridden in subclasses; the overriding method should invoke
     * <code>super.setDisplay(display)</code>.
     * @param display           The VisAD display.  May not be
     *                          <code>null</code>.
     * @throws NullPointerException The display is <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #removeDataReferences
     */
    public void setDisplay(LocalDisplay display)
            throws RemoteException, VisADException {

        if (display == null) {
            throw new NullPointerException();
        }

        arrows.setDisplay(display);
        super.setDisplay(display);
    }

    /**
     * Returns the wind profile.
     * @return                  The wind profile.
     */
    protected Field getProfile() {
        return (Field) profileRef.getData();
    }

    /**
     * Returns the type of the geopotential altitude quantity.
     * @return                  The type of the geopotential altitude quantity.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getGeopotentialAltitudeRealType()
            throws VisADException {
        return WindArrow.getGeopotentialAltitudeDefaultRealType();
    }

    /**
     * Returns the type of the vertical component of the profile quantity.
     * @return                  The type of the vertical quantity.
     * @throws VisADException   VisAD failure.
     */
    public RealType getVerticalComponentRealType() throws VisADException {
        return (RealType) tupleType.getComponent(0);
    }

    /**
     * Returns the type of the westerly wind quantity.
     * @return                  The type of the westerly wind quantity.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getWesterlyWindRealType() throws VisADException {
        return WindArrow.getWesterlyWindDefaultRealType();
    }

    /**
     * Returns the type of the southerly wind quantity.
     * @return                  The type of the southerly wind quantity.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getSoutherlyWindRealType() throws VisADException {
        return WindArrow.getSoutherlyWindDefaultRealType();
    }

    /**
     * Set the levels of the wind profile to display.
     * @param levels  the set of levels (if null, display all);
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindLevels(Gridded1DSet levels)
            throws VisADException, RemoteException {}

    /**
     * Sets the wind arrows.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void setWindArrows()
            throws RemoteException, VisADException {

        Field field = getProfile();

        if (arrows.displayableCount() == field.getLength()) {
            arrowWindListener.setEnabled(false);

            Enumeration e = field.domainEnumeration();

            for (Iterator iter = arrows.iterator(); iter.hasNext(); ) {
                Real vertCoord =
                    (Real) ((RealTuple) e.nextElement()).getComponent(0);
                WindArrow windArrow = (WindArrow) iter.next();

                windArrow.setWind(new Tuple(tupleType, new Data[]{ vertCoord,
                                                                   field.evaluate(
                                                                   vertCoord) }, false));
            }

            arrowWindListener.setEnabled(true);
        } else {
            arrows.clearDisplayables();

            for (Enumeration e = field.domainEnumeration();
                    e.hasMoreElements(); ) {
                Real vertCoord =
                    (Real) ((RealTuple) e.nextElement()).getComponent(0);
                WindArrow windArrow = new WindArrow(tupleType);

                windArrow.setWind(new Tuple(tupleType, new Data[]{ vertCoord,
                                                                   field.evaluate(
                                                                   vertCoord) }, false));

                if (arrows.displayableCount() == 0) {
                    setScalarMapSet(windArrow.getScalarMapSet());
                    windArrow.addPropertyChangeListener(SCALAR_MAP_SET,
                                                        arrowMapSetListener);
                }

                windArrow.addPropertyChangeListener(WindArrow.WIND,
                                                    arrowWindListener);
                arrows.addDisplayable(windArrow);
            }
        }
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindStaff)) {
            equals = false;
        } else {
            WindStaff that = (WindStaff) obj;

            equals =
                (this == that)
                || (arrows.equals(that.arrows)
                    && getProfile().equals(that.getProfile())
                    && arrowWindListener.equals(that.arrowWindListener)
                    && arrowMapSetListener.equals(that.arrowMapSetListener)
                    && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        return arrows.hashCode() ^ getProfile().hashCode()
               ^ arrowWindListener.hashCode()
               ^ arrowMapSetListener.hashCode() ^ super.hashCode();
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
        return new WindStaff(this);
    }

    /**
     * Provides support for observing changes in the "wind" property of a
     * WindArrow.
     */
    protected class ArrowWindListener implements PropertyChangeListener {

        /** enable flag */
        private boolean enabled = true;

        /** change flag */
        private boolean dirty = false;

        /**
         * Constructs from nothing.
         */
        public ArrowWindListener() {}

        /**
         * Handles a change to a WindArrow's "wind" property.
         * @param event         The property-change event.
         */
        public void propertyChange(PropertyChangeEvent event) {

            dirty = true;

            if (enabled) {
                try {
                    Field profile      = getProfile();
                    Tuple newWindPoint = (Tuple) event.getNewValue();
                    RealTuple newWind =
                        (RealTuple) newWindPoint.getComponent(1);
                    Real newU      = (Real) newWind.getComponent(0);
                    Real newV      = (Real) newWind.getComponent(1);
                    Real vertCoord = (Real) newWindPoint.getComponent(0);
                    int index =
                        profile.getDomainSet().doubleToIndex(new double[][] {
                        { vertCoord.getValue(profile.getDomainUnits()[0]) }
                    })[0];
                    //System.out.println("wind index = " + index);
                    RealTuple oldWind = (RealTuple) profile.getSample(index);
                    Real      oldU    = (Real) oldWind.getComponent(0);
                    Real      oldV    = (Real) oldWind.getComponent(1);

                    if ( !visad.util.Util
                            .isApproximatelyEqual(newU.getValue(), oldU
                                .getValue(newU.getUnit())) || !visad.util.Util
                                    .isApproximatelyEqual(newV
                                        .getValue(), oldV
                                        .getValue(newV.getUnit()))) {
                        //System.out.println("got new wind " + newWind);
                        profile.setSample(index, newWind);
                        profileChange(null);
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to wind-arrow: " + e);
                }

                dirty = false;
            }
        }

        /**
         * Sets whether or not this instance responds to property-change events.
         * @param yes           Whether or not this instance responds to
         *                      property-change events.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setEnabled(boolean yes)
                throws VisADException, RemoteException {

            enabled = yes;

            if (dirty) {
                profileChange(null);

                dirty = false;
            }
        }
    }

    /**
     * Provides support for observing changes in the set of ScalarMap-s of a
     * WindArrow.
     */
    protected class ArrowMapSetListener implements PropertyChangeListener {

        /**
         * Constructs from nothing.
         */
        public ArrowMapSetListener() {}

        /**
         * Handles a change to a WindArrow's "scalarMapSet" property.
         * @param event         The property-change event.
         */
        public void propertyChange(PropertyChangeEvent event) {
            WindStaff.this.setScalarMapSet(
                (ScalarMapSet) event.getNewValue());
        }
    }
}







