/*
 * $Id: RealReadout.java,v 1.16 2005/05/13 18:33:36 jeffmc Exp $
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

import java.text.NumberFormat;

import javax.swing.*;

import ucar.unidata.beans.NonVetoableProperty;

import visad.*;
import visad.data.units.*;


/**
 * Provides support for readouts of real values.
 *
 * @author Steven R. Emmerson
 * @version $Id: RealReadout.java,v 1.16 2005/05/13 18:33:36 jeffmc Exp $
 */
public class RealReadout implements PropertyChangeListener {

    /**
     * The indicator for using the name of the RealType.
     */
    public final static int REALTYPE_NAME = 0;

    /**
     * The indicator for using the client-supplied name.
     */
    public final static int SPECIFIED_NAME = 1;

    /**
     * The indicator for using the unit of the Real.
     */
    public final static int REAL_UNIT = 0;

    /**
     * The indicator for using the unit of the RealType.
     */
    public final static int REALTYPE_UNIT = 1;

    /**
     * The indicator for using the unit supplied by the client.
     */
    public final static int SPECIFIED_UNIT = 2;

    /**
     * The name of the name property.
     */
    public final static String NAME = "name";

    /**
     * The name of the format property.
     */
    public final static String FORMAT = "format";

    /**
     * The name of the numeric-string property.
     */
    public final static String NUMERIC_STRING = "numericString";

    /**
     * The name of the unit-string property.
     */
    public final static String UNIT_STRING = "unitString";

    /**
     * The value of the readout quantity.
     */
    private Real real;

    /**
     * The name property.
     */
    private NameProperty nameProperty;

    /**
     * The format property.
     */
    private FormatProperty formatProperty;

    /**
     * The numeric value property.
     */
    private NumericValueProperty numericValueProperty;

    /**
     * The unit property.
     */
    private UnitProperty unitProperty;

    /**
     * Constructs from nothing.
     */
    public RealReadout() {

        nameProperty         = new NameProperty();
        formatProperty       = new FormatProperty();
        numericValueProperty = new NumericValueProperty();
        unitProperty         = new UnitProperty();
    }

    /**
     * Sets the client-supplied name for the readout.
     * @param name              The client-supplied name for the readout.
     */
    public void setSpecifiedName(String name) {
        nameProperty.setSpecifiedName(name);
    }

    /**
     * Sets the type of name-use.
     * @param which             The type of name-use.  One of REALTYPE_NAME or
     *                          SPECIFIED_NAME.
     */
    public void setNameUse(int which) {
        nameProperty.setNameUse(which);
    }

    /**
     * Sets the format property.
     * @param format            The new value.
     */
    public void setFormat(NumberFormat format) {
        formatProperty.setFormat(format);
    }

    /**
     * Sets the client-supplied unit-specification.
     *
     * @param unitString           The client-supplied unit-specification.
     * @throws NoSuchUnitException if the specification can't be decoded.
     * @throws ParseException      if a parse-error occurs while decoding.
     */
    public void setSpecifiedUnit(String unitString)
            throws NoSuchUnitException, ParseException {
        unitProperty.setSpecifiedUnit(unitString);
    }

    /**
     * Sets the type of unit-use.
     * @param which             The type of unit-use.  One of REAL_UNIT,
     *                          REALTYPE_UNIT, or SPECIFIED_UNIT.
     */
    public void setUnitUse(int which) {
        unitProperty.setUnitUse(which);
    }

    /**
     * Returns the name of the readout.
     * @return                  The name of the readout.
     */
    public String getName() {
        return nameProperty.getQuantityName();
    }

    /**
     * Returns the numeric string of the readout.
     * @return                  The numeric string of the readout.
     */
    public String getNumericString() {
        return numericValueProperty.getNumericString();
    }

    /**
     * Returns the unit-specification of the readout.
     * @return                  The unit-specification of the readout.
     */
    public String getUnitString() {
        return unitProperty.getUnitString();
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param propertyName      The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {

        if (NAME.equals(propertyName)) {
            nameProperty.addPropertyChangeListener(listener);
        } else if (NUMERIC_STRING.equals(propertyName)) {
            numericValueProperty.addPropertyChangeListener(listener);
        } else if (UNIT_STRING.equals(propertyName)) {
            unitProperty.addPropertyChangeListener(listener);
        }
    }

    /**
     * Sets the value of the readout quantity.
     * @param real              The new value for the readout quantity.
     */
    public void setReal(Real real) {

        this.real = real;

        nameProperty.setValue(real);
        unitProperty.setValue(real);
        numericValueProperty.setValue(real);
    }

    /**
     * Handles a change to a property.
     * @param event             The property change event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        setReal((Real) event.getNewValue());
    }

    /**
     * Provides support for name properties.
     */
    protected class NameProperty extends NonVetoableProperty {

        /**
         * The client-supplied name.
         */
        private String specifiedName;

        /**
         * The type of name-use.
         */
        private int nameUse = REALTYPE_NAME;

        /**
         * Constructs from nothing.
         */
        public NameProperty() {
            super(RealReadout.this, NAME);
        }

        /**
         * Sets the type of name-use.
         * @param which         The type of name-use.  One of REALTYPE_NAME or
         *                      SPECIFIED_NAME.
         */
        public void setNameUse(int which) {

            nameUse = which;

            if (nameUse == SPECIFIED_NAME) {
                setValue(real);
            }
        }

        /**
         * Sets the value of the quantity.
         * @param real          The value for the quantity.
         */
        public void setValue(Real real) {

            super.setValueAndNotifyListeners((nameUse == SPECIFIED_NAME)
                                             ? specifiedName
                                             : (real == null)
                                               ? "<null>"
                                               : ((RealType) real.getType())
                                                   .getOriginalName());
        }

        /**
         * Returns the name of the quantity.
         * @return              The name of the quantity.
         */
        public String getQuantityName() {
            return (String) getValue();
        }

        /**
         * Sets the client-supplied name.
         * @param specifiedName The client-supplied name.
         */
        public void setSpecifiedName(String specifiedName) {

            this.specifiedName = specifiedName;

            if (nameUse == SPECIFIED_NAME) {
                setValue(real);
            }
        }
    }

    /**
     * Provides support for format properties.
     */
    protected class FormatProperty extends NonVetoableProperty {

        /**
         * Constructs from nothing.
         */
        public FormatProperty() {

            super(RealReadout.this, FORMAT);

            NumberFormat format = NumberFormat.getNumberInstance();

            format.setMinimumFractionDigits(3);
            super.setValue(format);
        }

        /**
         * Sets the format.
         * @param format        The format.
         */
        public void setFormat(NumberFormat format) {
            super.setValueAndNotifyListeners(format.clone());
            numericValueProperty.setValue(real);
        }

        /**
         * Returns the format.
         * @return              The format.
         */
        public NumberFormat getFormat() {
            return (NumberFormat) getValue();
        }
    }

    /**
     * Provides support for numeric value properties.
     */
    protected class NumericValueProperty extends NonVetoableProperty {

        /**
         * Constructs from nothing.
         */
        public NumericValueProperty() {

            super(RealReadout.this, NUMERIC_STRING);

            setValue("                ");
        }

        /**
         * Sets the value.
         * @param real          The new value.
         */
        public void setValue(Real real) {

            if (real != null) {
                String numericString;

                try {
                    double value = real.getValue(unitProperty.getUnit());
                    numericString = (value != value)  // NaN
                                    ? ""
                                    : formatProperty.getFormat().format(
                                        value);
                } catch (Exception e) {
                    numericString = "<error>";
                }

                super.setValueAndNotifyListeners(numericString);
            }
        }

        /**
         * Gets the numeric value as a string.
         * @return              The numeric value as a string.
         */
        public String getNumericString() {
            return (String) getValue();
        }
    }

    /**
     * Provides support for unit properties.
     */
    protected class UnitProperty extends NonVetoableProperty {

        /**
         * The client-supplied unit-specification.
         */
        private String specifiedUnitString;

        /**
         * the client-supplied unit.
         */
        private Unit specifiedUnit;

        /**
         * The type of unit-use.
         */
        private int unitUse = REAL_UNIT;

        /**
         * The unit.
         */
        private Unit unit;

        /**
         * Constructs from nothing.
         */
        public UnitProperty() {
            super(RealReadout.this, UNIT_STRING);
        }

        /**
         * Sets the client-supplied unit-specification.
         * @param specifiedUnitString   The client-supplied unit-specification.
         * @throws NoSuchUnitException  No such unit.
         * @throws ParseException       Couldn't decode specification.
         */
        public void setSpecifiedUnit(String specifiedUnitString)
                throws NoSuchUnitException, ParseException {

            this.specifiedUnitString = specifiedUnitString;
            specifiedUnit            = Parser.parse(specifiedUnitString);

            if (unitUse == SPECIFIED_UNIT) {
                setValue(real);
                numericValueProperty.setValue(real);
            }
        }

        /**
         * Sets the type of unit-use.
         * @param which         The type of unit-use.  One of REAL_UNIT,
         *                      REALTYPE_UNIT, or SPECIFIED_UNIT.
         */
        public void setUnitUse(int which) {

            this.unitUse = which;

            setValue(real);
            numericValueProperty.setValue(real);
        }

        /**
         * Sets the value from a Real.
         * @param real          The Real.
         */
        public void setValue(Real real) {

            String unitString;

            if (unitUse == SPECIFIED_UNIT) {
                unitString = specifiedUnitString;
                unit       = specifiedUnit;
            } else if (unitUse == REALTYPE_UNIT) {
                unit       = (real == null)
                             ? null
                             : ((RealType) real.getType()).getDefaultUnit();
                unitString = (unit == null)
                             ? "<null>"
                             : unit.toString();
            } else {
                unit       = (real == null)
                             ? null
                             : real.getUnit();
                unitString = (unit == null)
                             ? "<null>"
                             : unit.toString();
            }

            super.setValueAndNotifyListeners(unitString);
        }

        /**
         * Returns the unit-specification.
         * @return              The unit-specification.
         */
        public String getUnitString() {
            return (String) getValue();
        }

        /**
         * Returns the unit.
         * @return              The unit.
         */
        public Unit getUnit() {
            return unit;
        }
    }
}







