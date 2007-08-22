/*
 * $Id: DataModelImpl.java,v 1.12 2005/05/13 18:33:27 jeffmc Exp $
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

import javax.swing.*;
import javax.swing.event.*;

import ucar.unidata.data.sounding.*;

import ucar.visad.quantities.CartesianHorizontalWind;

import visad.*;


/**
 * Provides support for adapting a JList of sounding observations to the API-s
 * of the sounding and wind data models.  The conceptual model that this class
 * presents is that of a set of soundings -- a subset of which are the SELECTED
 * SOUNDINGS -- and of which at most one sounding is the SELECTED SOUNDING.  The
 * soundings that constitute the set may change -- as may the selected soundings
 * and the selected sounding.  Modifications to the soundings by users of this
 * class will appear to actually modify the soundings in the set.
 *
 * @author Steven R. Emmerson
 * @version $Id: DataModelImpl.java,v 1.12 2005/05/13 18:33:27 jeffmc Exp $
 */
public class DataModelImpl implements SoundingDataModel, WindDataModel {

    /** list */
    private JList jList;

    /** listeners */
    private volatile PropertyChangeSupport propertyListeners;

    /** selected index */
    private int selectedIndex = -1;

    /*
     * The following contain the data that are returned to users of this
     * instance.  The RAOB objects are used in a read-only mode as the source
     * of original data.
     */

    /** temperature profiles */
    private List temperatureProfiles;

    /** dewpoint profiles */
    private List dewPointProfiles;

    /** wind profiles */
    private List windProfiles;

    /** means wind cells */
    private List meanWindCells;

    /**
     * Constructs from a JList.  The JList's data model and selection model
     * must be in their final form.  The SELECTED_INDEX property of this
     * instance will be bound to the "selectedIndex" property of the JList and
     * PropertyChangeEvent-s will be propagated.
     * @param jList             The underlying JList whose mutable data model
     *                          contains the sounding data and whose list
     *                          selection model determines the selected
     *                          soundings and selected sounding.
     */
    public DataModelImpl(JList jList) {

        temperatureProfiles = new ArrayList();
        dewPointProfiles    = new ArrayList();
        windProfiles        = new ArrayList();
        meanWindCells       = new ArrayList();
        this.jList          = jList;

        jList.addListSelectionListener(new ListSelectionListener() {

            private int selectedIndex = -1;

            public void valueChanged(ListSelectionEvent event) {

                if ( !event.getValueIsAdjusting()) {
                    int oldSelectedIndex = selectedIndex;

                    setSelectedIndex(
                        DataModelImpl.this.jList.getMinSelectionIndex());
                }
            }
        });
        addListDataListener(new ListDataListener() {

            /*
             * Keeps data copies consistent with the original data.
             */
            public void contentsChanged(ListDataEvent event) {

                try {
                    synchronized (DataModelImpl.this) {
                        temperatureProfiles.clear();
                        dewPointProfiles.clear();
                        windProfiles.clear();
                        meanWindCells.clear();
                        setSelectedIndex(
                            DataModelImpl.this.jList.getMinSelectionIndex());
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".contentsChanged(): "
                        + "Couldn't handle change to sounding database: "
                        + e);
                }
            }

            public void intervalAdded(ListDataEvent event) {
                setSelectedIndex(
                    DataModelImpl.this.jList.getMinSelectionIndex());
            }

            public void intervalRemoved(ListDataEvent event) {

                synchronized (DataModelImpl.this) {
                    int index1 = event.getIndex1();

                    for (int i = event.getIndex0(); i <= index1; ++i) {
                        temperatureProfiles.add(i, null);
                        dewPointProfiles.add(i, null);
                        windProfiles.add(i, null);
                        meanWindCells.add(i, null);
                    }

                    setSelectedIndex(
                        DataModelImpl.this.jList.getMinSelectionIndex());
                }
            }
        });
    }

    /**
     * Returns the number of soundings.
     * @return                  The number of soundings.
     */
    public int getSize() {
        return jList.getModel().getSize();
    }

    /**
     * Indicates if the given sounding is a member of the selected soundings.
     * @param index             The index of the sounding.
     * @return                  <code>true</code> if and only if the given
     *                          sounding is a member of the selected soundings.
     */
    public boolean isSelectedIndex(int index) {
        return jList.isSelectedIndex(index);
    }

    /**
     * Returns the index of the selected sounding.
     * @return                  The index of the selected thing or -1 if nothing
     *                          is selected.
     */
    public int getSelectedIndex() {
        return jList.getSelectedIndex();
    }

    /**
     * Restores the selected soundings to their original values.  Doesn't affect
     * unselected soundings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void restoreSelectedSoundings()
            throws VisADException, RemoteException {

        int[] indices = jList.getSelectedIndices();

        for (int i = 0; i < indices.length; ++i) {
            int  index = indices[i];
            RAOB raob  = getRAOB(index);

            ((Field) temperatureProfiles.get(index)).setSamples(
                raob.getTemperatureProfile().getFloats());
            ((Field) dewPointProfiles.get(index)).setSamples(
                raob.getDewPointProfile().getFloats());
            ((Field) windProfiles.get(index)).setSamples(
                ensureCartesianWindProfile(
                    raob.getWindProfile()).getFloats());
        }
    }

    /**
     * Returns the aerological sounding data at the given index.
     * @param index             The index of the sounding.
     * @return                  The aerological sounding data at the given
     *                          index.  The returned array has 2 elements; the
     *                          first element is the temperature profile and the
     *                          second element is the dew-point profile.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    public synchronized Field[] getSounding(int index)
            throws IndexOutOfBoundsException {
        return new Field[]{ getTemperatureProfile(index),
                            getDewPointProfile(index) };
    }

    /**
     * Returns the temperature profile at the given index.
     * @param index             The index of the temperature profile.
     * @return                  The temperature profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    public synchronized Field getTemperatureProfile(int index)
            throws IndexOutOfBoundsException {

        Field temperatureProfile = (index >= temperatureProfiles.size())
                                   ? (Field) null
                                   : (Field) temperatureProfiles.get(index);

        if (temperatureProfile == null) {
            temperatureProfile =
                (Field) getRAOB(index).getTemperatureProfile().dataClone();

            temperatureProfiles.add(index, temperatureProfile);
        }

        return temperatureProfile;
    }

    /**
     * Returns the dew-point profile at the given index.
     * @param index             The index of the dew-point profile.
     * @return                  The dew-point profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    public synchronized Field getDewPointProfile(int index)
            throws IndexOutOfBoundsException {

        Field dewPointProfile = (index >= dewPointProfiles.size())
                                ? (Field) null
                                : (Field) dewPointProfiles.get(index);

        if (dewPointProfile == null) {
            dewPointProfile =
                (Field) getRAOB(index).getDewPointProfile().dataClone();

            dewPointProfiles.add(index, dewPointProfile);
        }

        return dewPointProfile;
    }

    /**
     * Returns the wind profile and mean-wind at the given index.
     * @param index             The index of the sounding.
     * @return                  The wind data at the given index.  The returned
     *                          array has 2 elements; the first element is the
     *                          wind profile and the second element is the mean
     *                          wind.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Object[] getWindData(int index)
            throws IndexOutOfBoundsException, VisADException,
                   RemoteException {
        return new Object[]{ getWindProfile(index), getMeanWind(index) };
    }

    /**
     * Returns the wind profile at the given index.
     * @param index             The index of the wind profile.
     * @return                  The wind profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Field getWindProfile(int index)
            throws IndexOutOfBoundsException, VisADException,
                   RemoteException {

        Field windProfile = (index >= windProfiles.size())
                            ? (Field) null
                            : (Field) windProfiles.get(index);

        if (windProfile == null) {
            windProfile = ensureCartesianWindProfile(
                (FlatField) getRAOB(index).getWindProfile().dataClone());

            windProfiles.add(index, windProfile);
        }

        return windProfile;
    }

    /**
     * Returns the mean-wind at the given index.
     * @param index             The index of the mean-wind.
     * @return                  The mean-wind at the given index.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Tuple getMeanWind(int index)
            throws VisADException, RemoteException {
        return getMeanWindCell(index).getWind();
    }

    /**
     * Returns the data reference for the mean-wind at the given index.
     * @param index             The index of the mean-wind.
     * @return                  The data reference for the mean-wind at the
     *                          given index.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized DataReference getMeanWindRef(int index)
            throws VisADException, RemoteException {
        return getMeanWindCell(index).getMeanWindRef();
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {

        if (propertyListeners == null) {
            synchronized (this) {
                if (propertyListeners == null) {
                    propertyListeners = new PropertyChangeSupport(this);
                }
            }
        }

        propertyListeners.addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public synchronized void removePropertyChangeListener(String name,
            PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Adds a listener for changes to the selected things.
     * @param listener          The listener for changes to the selected
     *                          things.
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        jList.addListSelectionListener(listener);
    }

    /**
     * Removes a listener for changes to the selected things.
     * @param listener          The listener for changes to the selected
     *                          things.
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        jList.removeListSelectionListener(listener);
    }

    /**
     * Adds a listener for changes to the underlying list of things.
     * @param listener          The listener for changes to the underlying list
     *                          of things.
     */
    public void addListDataListener(ListDataListener listener) {
        jList.getModel().addListDataListener(listener);
    }

    /**
     * Removes a listener for changes to the underlying list of things.
     * @param listener          The listener for changes to the underlying list
     *                          of things.
     */
    public void removeListDataListener(ListDataListener listener) {
        jList.getModel().removeListDataListener(listener);
    }

    /**
     * Returns the RAOB at the given index.
     * @param index             The index of the RAOB.
     * @return                  The RAOB at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    protected synchronized RAOB getRAOB(int index)
            throws IndexOutOfBoundsException {
        return ((SoundingOb) jList.getModel().getElementAt(index)).getRAOB();
    }

    /**
     * Ensures that a wind profile is in cartesian coordinates.
     * @param input             Wind profile in cartesian or polar coordinates.
     * @return                  Wind profile in cartesian coordinates.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected static FlatField ensureCartesianWindProfile(FlatField input)
            throws VisADException, RemoteException {

        FlatField output;

        if (Unit.canConvert(input.getDefaultRangeUnits()[0], CommonUnit
                .meterPerSecond) && Unit
                    .canConvert(input.getDefaultRangeUnits()[1], CommonUnit
                        .meterPerSecond)) {
            output = input;
        } else {
            RealTupleType cartesianType =
                CartesianHorizontalWind.getRealTupleType();

            output =
                new FlatField(new FunctionType(((FunctionType) input
                    .getType()).getDomain(), CartesianHorizontalWind
                        .getRealTupleType()), input.getDomainSet());

            RealTupleType inputType =
                (RealTupleType) ((FunctionType) input.getType()).getRange();
            ErrorEstimate[] inputErrors = input.getRangeErrors();
            ErrorEstimate[] outputErrors =
                new ErrorEstimate[inputErrors.length];

            output.setSamples(
                CoordinateSystem.transformCoordinates(
                    cartesianType, cartesianType.getCoordinateSystem(),
                    cartesianType.getDefaultUnits(), outputErrors, inputType,
                    ucar.visad.Util.getRangeCoordinateSystem(input),
                    ucar.visad.Util.getRangeUnits(input), inputErrors,
                    input.getValues()));
        }

        return output;
    }

    /**
     * Returns the mean-wind cell at the given index.
     * @param index             The index of the mean-wind cell.
     * @return                  The mean-wind cell at the given index.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized MeanWindCell getMeanWindCell(int index)
            throws VisADException, RemoteException {

        MeanWindCell meanWindCell = (index >= meanWindCells.size())
                                    ? (MeanWindCell) null
                                    : (MeanWindCell) meanWindCells.get(index);

        if (meanWindCell == null) {
            DensityProfile densityProfile =
                new DensityProfile(getTemperatureProfile(index),
                                   getDewPointProfile(index));
            final MeanWindCell mwc =
                new MeanWindCell(getWindProfile(index),
                                 densityProfile.getDensityProfile());

            densityProfile.addPropertyChangeListener(
                densityProfile.DENSITY_PROFILE, new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {

                    try {
                        mwc.setDensityProfile((Field) event.getNewValue());
                    } catch (Exception e) {
                        System.err.println(
                            getClass().getName() + ".propertyChange(): "
                            + "Couldn't set mean-wind's density profile: "
                            + e);
                    }
                }
            });

            meanWindCell = mwc;

            meanWindCells.add(index, meanWindCell);
        }

        return meanWindCell;
    }

    /**
     * Set the selected index in the list and fire a property change
     *
     * @param index  index to set
     */
    private synchronized void setSelectedIndex(int index) {

        int oldValue = selectedIndex;

        selectedIndex = index;

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(SELECTED_INDEX, oldValue,
                                                 selectedIndex);
        }
    }
}







