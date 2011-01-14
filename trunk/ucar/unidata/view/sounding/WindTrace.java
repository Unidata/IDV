/*
 * $Id: WindTrace.java,v 1.24 2005/08/11 22:12:15 dmurray Exp $
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

import java.beans.*;

import java.util.*;

import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;
import visad.java3d.*;


/**
 * Provides support for displaying a wind profile as a trace.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindTrace.java,v 1.24 2005/08/11 22:12:15 dmurray Exp $
 */
public class WindTrace extends WindProfile {

    /** displayable for the data */
    private DisplayableWindTrace windTrace;

    /** original field */
    private Field originalData;

    /**
     * Constructs from the display types for the axes and a VisAD display.
     *
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindTrace(LocalDisplay display)
            throws VisADException, RemoteException {

        /*super(zType);*/
        windTrace = new DisplayableWindTrace( /*xType, yType, */display);

        windTrace.setManipulable(true);
        windTrace.setLineWidth(2);
        windTrace.setActive(true);
        addDisplayable(windTrace);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindTrace(WindTrace that)
            throws VisADException, RemoteException {

        super(that);

        windTrace = (DisplayableWindTrace) that.windTrace.cloneForDisplay();
    }

    /**
     * Sets the wind profile.
     * @param profile           The wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfile(Field profile)
            throws VisADException, RemoteException {
        windTrace.setProfile((Field) profile.dataClone());
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
     * Returns the wind profile.
     * @return                  The wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Field getProfile() throws VisADException, RemoteException {
        return windTrace.getProfile();
    }

    /**
     * Set the levels of the wind profile.
     * @param levels new levels
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindLevels(Gridded1DSet levels)
            throws VisADException, RemoteException {}

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindTrace)) {
            equals = false;
        } else {
            WindTrace that = (WindTrace) obj;

            equals = (that == this)
                     || (windTrace.equals(that.windTrace)
                         && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return windTrace.hashCode() ^ super.hashCode();
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
        return new WindTrace(this);
    }

    /**
     * Provides support for displaying a wind profile as an (x,y,z) trace.
     */
    protected class DisplayableWindTrace extends LineDrawing {

        /*
        private DisplayRealType         xType;
        private DisplayRealType         yType;
        private ScalarMap               xMap;
        private ScalarMap               yMap;
        private ScalarMap               zMap;
        */

        /**
         * Constructs from a VisAD display and the types of the axes.
         * @param display               The VisAD display.
         * @throws VisADException if a core VisAD failure occurs.
         * @throws RemoteException if a Java RMI failure occurs.
         */
        public DisplayableWindTrace(LocalDisplay display)
                throws VisADException, RemoteException {

            super("DisplayableWindTrace");

            setDisplay(display);
            setActive(false);
            setProfile(getMissingWindField());

            /*
            this.xType = xType;
            this.yType = yType;
            */
        }

        /**
         * Constructs from another instance.
         * @param that                  The other instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected DisplayableWindTrace(DisplayableWindTrace that)
                throws RemoteException, VisADException {
            super(that);
        }

        /**
         * Sets the wind profile.
         * @param profile               The wind profile.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setProfile(Field profile)
                throws VisADException, RemoteException {

            setData(profile);

            /*
            FunctionType        funcType = (FunctionType)profile.getType();
            ScalarMap           oldMap;

            oldMap = xMap;
            xMap = new ScalarMap(
                (ScalarType)((RealTupleType)funcType.getRange())
                    .getComponent(0),
                xType);
            WindTrace.this.replaceScalarMap(oldMap, xMap);

            oldMap = yMap;
            yMap = new ScalarMap(
                (ScalarType)((RealTupleType)funcType.getRange())
                    .getComponent(1),
                yType);
            WindTrace.this.replaceScalarMap(oldMap, yMap);
            */
        }

        /**
         * Returns the wind profile.
         * @return                      The wind profile.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Field getProfile() throws VisADException, RemoteException {
            return (Field) getData();
        }

        /**
         * Returns a clone of this instance suitable for another VisAD display.
         * Underlying data objects are not cloned.
         * display.
         * @return                      A clone of this instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Displayable cloneForDisplay()
                throws RemoteException, VisADException {
            return new DisplayableWindTrace(this);
        }

        /**
         * Handles a change to the Data object of this displayable's
         * DataReference.
         */
        protected void dataChange() {

            try {
                profileChange(null);
            } catch (Exception e) {
                System.err.println(this.getClass().getName()
                                   + ".dataChange(): "
                                   + "Couldn't handle change to data: " + e);
            }
        }
    }
}





