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

import java.rmi.RemoteException;


/**
 * A class for support of a select range scalar map.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class SelectRangeDisplayable extends Displayable {

    /** low range for select */
    private double lowSelectedRange = Double.NaN;  // low range for scalarmap

    /** high range for select */
    private double highSelectedRange = Double.NaN;  // high range for scalarmap

    /** RealType for the SelectRange ScalarMap */
    private ScalarMap selectMap = null;

    /** Control for select range */
    private RangeControl selectControl;

    /** RealType for the SelectRange ScalarMap */
    private RealType selectRealType = null;

    /** low range for select map */
    private double minSelect = Double.NaN;  // low range for scalarmap

    /** high range for select map */
    private double maxSelect = Double.NaN;  // high range for scalarmap

    /**
     * Default ctor.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SelectRangeDisplayable() throws VisADException, RemoteException {
        this((RealType) null);
    }

    /**
     * Create a select range displayable for the particular type.
     * @param rangeType  RealType for the select range
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SelectRangeDisplayable(RealType rangeType)
            throws VisADException, RemoteException {
        if (rangeType != null) {

            selectRealType = rangeType;
            setSelectRangeMap();
        }
    }

    /**
     * Constructs from a SelectRangeDisplayable.
     *
     * @param that a SelectRangeDisplayable.
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    protected SelectRangeDisplayable(SelectRangeDisplayable that)
            throws VisADException, RemoteException {

        super(that);

        this.lowSelectedRange  = that.lowSelectedRange;
        this.highSelectedRange = that.highSelectedRange;
        this.minSelect         = that.minSelect;
        this.maxSelect         = that.maxSelect;

        if (that.getSelectRealType() != null) {
            setSelectRealType(that.getSelectRealType());
        }
    }

    /**
     * Sets the RealType of the select parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSelectRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(selectRealType)) {
            RealType oldValue = selectRealType;
            selectRealType = realType;
            setSelectRangeMap();
        }
    }

    /**
     * Returns the RealType of the SelectRange parameter.
     * @return                  The RealType of the select range parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getSelectRealType() {
        return selectRealType;
    }

    /**
     * <p>Adds the {@link visad.DataReference}s associated with this instance
     * to the display.</p>
     *
     * <p>This implementation does nothing.</p>
     */
    public final void myAddDataReferences() {}

    /**
     * <p>Removes the {@link visad.DataReference}s associated with this
     * instance from the display.</p>
     *
     * <p>This implementation does nothing.</p>
     */
    public final void myRemoveDataReferences() {}

    /**
     * Returns whether this Displayable has a valid range
     * (i.e., lowSelectedRange and highSelectedRange are both not NaN's
     *
     * @return true if range has been set
     */
    public boolean hasSelectedRange() {
        return ( !Double.isNaN(lowSelectedRange)
                 && !Double.isNaN(highSelectedRange));
    }

    /**
     * Check to see if the range has been set for the select
     *
     * @return true if it has
     */
    private boolean hasSelectMinMax() {
        return ( !Double.isNaN(minSelect) && !Double.isNaN(maxSelect));
    }

    /**
     * Set selected range with the range for select
     *
     * @param low  low select value
     * @param hi   hi select value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setSelectedRange(double low, double hi)
            throws VisADException, RemoteException {


        lowSelectedRange  = low;
        highSelectedRange = hi;
        if ((selectControl != null) && hasSelectedRange()) {
            selectControl.setRange(new double[] { low, hi });
        }

    }


    /**
     * Set the units for the displayed range
     *
     * @param unit Unit for display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setDisplayUnit(Unit unit)
            throws VisADException, RemoteException {
        super.setDisplayUnit(unit);
        applyDisplayUnit(selectMap, selectRealType);
    }

    /**
     * creates the ScalarMap for SelectRange and control for this Displayable.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setSelectRangeMap() throws RemoteException, VisADException {

        selectMap = new ScalarMap(selectRealType, Display.SelectRange);

        applyDisplayUnit(selectMap, selectRealType);

        if (hasSelectMinMax()) {
            selectMap.setRange(minSelect, maxSelect);
        }

        selectMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    selectControl = (RangeControl) selectMap.getControl();
                    if (hasSelectedRange() && (selectControl != null)) {
                        selectControl.setRange(new double[] {
                            lowSelectedRange,
                            highSelectedRange });
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == event.AUTO_SCALE)
                        && hasSelectMinMax()) {
                    selectMap.setRange(minSelect, maxSelect);
                }
            }
        });
        ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
        maps.add(selectMap);
        setScalarMapSet(maps);
    }


    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * @param low    the minimun value
     * @param hi     the maximum value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeForSelect(double low, double hi)
            throws VisADException, RemoteException {

        minSelect = low;
        maxSelect = hi;
        if ((selectMap != null) && hasSelectMinMax()) {
            selectMap.setRange(low, hi);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     *
     * @exception VisADException        VisAD failure.
     * @exception RemoteException       Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new SelectRangeDisplayable(this);
    }


}
