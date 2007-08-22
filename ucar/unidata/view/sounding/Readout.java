/*
 * $Id: Readout.java,v 1.12 2005/05/13 18:33:36 jeffmc Exp $
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



import java.awt.Component;

import java.beans.*;

import java.text.NumberFormat;

import javax.swing.*;

import visad.*;
import visad.data.units.*;


/**
 * Provides support for value readouts.
 *
 * @author Steven R. Emmerson
 * @version $Id: Readout.java,v 1.12 2005/05/13 18:33:36 jeffmc Exp $
 */
public class Readout implements PropertyChangeListener {

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
     * The name-label for the readout.
     */
    private JLabel nameLabel;

    /**
     * The numeric-value label for the readout.
     */
    private JLabel numericValueLabel;

    /**
     * The unit label for the readout.
     */
    private JLabel unitLabel;

    /**
     * The type of name-use.
     */
    private int nameUse;

    /**
     * The type of unit-use.
     */
    private int unitUse;

    /**
     * The client-supplied name.
     */
    private String specifiedName;

    /**
     * The client-supplied unit-string.
     */
    private String specifiedUnitString;

    /**
     * The client-supplied unit.
     */
    private Unit specifiedUnit;

    /**
     * The name of the readout.
     */
    private String name;

    /**
     * The numeric-string of the readout.
     */
    private String numericString;

    /**
     * The unit-string of the readout.
     */
    private String unitString;

    /**
     * The number-format of the readout.
     */
    private NumberFormat numberFormat;

    /**
     * Constructs from nothing.
     */
    public Readout() {

        // super();
        // setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        // add(nameLabel = new JLabel("Name"));
        // add(numericValueLabel = new JLabel("NaN", SwingConstants.RIGHT));
        // add(unitLabel = new JLabel("Unit"));
        nameLabel         = new JLabel("Name");
        numericValueLabel = new JLabel("NaN", SwingConstants.RIGHT);
        unitLabel         = new JLabel("Unit");
        nameUse           = REALTYPE_NAME;
        unitUse           = REAL_UNIT;
        numberFormat      = NumberFormat.getInstance();
    }

    /**
     * Sets the client-supplied name for the readout.
     * @param name              The client-supplied name for the readout.
     */
    public void setSpecifiedName(String name) {

        specifiedName = name;

        if (SPECIFIED_NAME == nameUse) {
            nameLabel.setText(specifiedName);
        }
    }

    /**
     * Returns the client-supplied name for the readout.
     * @return                  The client-supplied name for the readout.
     */
    public String getSpecifiedName() {
        return specifiedName;
    }

    /**
     * Sets the type of name-use.
     * @param which             The type of name-use.  One of REALTYPE_NAME or
     *                          SPECIFIED_NAME.
     */
    public void setNameUse(int which) {

        nameUse = which;

        if (SPECIFIED_NAME == nameUse) {
            nameLabel.setText(specifiedName);
        }
    }

    /**
     * Returns the type of name-use.
     * @return                  The type of name-use.  One of REALTYPE_NAME or
     *                          SPECIFIED_NAME.
     */
    public int getNameUse() {
        return nameUse;
    }

    /**
     * Sets the client-supplied unit-specification.
     * @param unitString        The client-supplied unit-specification.
     * @throws NoSuchUnitException if the specification couldn't be decoded.
     * @throws ParseException      if the specification couldn't be parsed.
     */
    public void setSpecifiedUnit(String unitString)
            throws NoSuchUnitException, ParseException {

        specifiedUnitString = unitString;
        specifiedUnit       = Parser.parse(specifiedUnitString);

        unitLabel.setText(unitString);
    }

    /**
     * Returns the client-supplied unit-specification.
     * @return                  The client-supplied unit-specification.
     */
    public String getSpecifiedUnit() {
        return specifiedUnitString;
    }

    /**
     * Sets the type of unit-use.
     * @param which             The type of unit-use.  One of REAL_UNIT,
     *                          REALTYPE_UNIT, or SPECIFIED_UNIT.
     */
    public void setUnitUse(int which) {

        unitUse = which;

        numericValueLabel.setText(getNumericString(real));
        unitLabel.setText(getUnitString(real));
    }

    /**
     * Returns the type of unit-use.
     * @return                  The type of unit-use.  One of REAL_UNIT,
     *                          REALTYPE_UNIT, or SPECIFIED_UNIT.
     */
    public int getUnitUse() {
        return unitUse;
    }

    /**
     * Returns the name of the readout.
     * @return                  The name of the readout.
     */
    public String getName() {
        return nameLabel.getText();
    }

    /**
     * Returns the numeric string of the readout.
     * @return                  The numeric string of the readout.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public String getNumericString() throws VisADException {
        return numericValueLabel.getText();
    }

    /**
     * Returns the unit-specification of the readout.
     * @return                  The unit-specification of the readout.
     */
    public String getUnitString() {
        return unitLabel.getText();
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param propertyName      The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {

        if (NAME.equals(propertyName)) {
            nameLabel.addPropertyChangeListener(listener);
        } else if (NUMERIC_STRING.equals(propertyName)) {
            numericValueLabel.addPropertyChangeListener(listener);
        } else if (UNIT_STRING.equals(propertyName)) {
            unitLabel.addPropertyChangeListener(listener);
        }
    }

    /**
     * Handles a change to a property.
     * @param event             The property change event.
     */
    public void propertyChange(PropertyChangeEvent event) {

        Real real = (Real) event.getNewValue();

        this.real = real;

        nameLabel.setText(getName(real));
        numericValueLabel.setText(getNumericString(real));
        unitLabel.setText(getUnitString(real));
    }

    /**
     * Returns the name component.
     * @return                  The name component.
     */
    public Component getNameComponent() {
        return nameLabel;
    }

    /**
     * Returns the numeric-value component.
     * @return                  The numeric-value component.
     */
    public Component getNumericValueComponent() {
        return numericValueLabel;
    }

    /**
     * Returns the unit-specification component.
     * @return                  The unit-specification component.
     */
    public Component getUnitComponent() {
        return unitLabel;
    }

    /**
     * Returns the name of the given Real, given the current name-use mode.
     * @param real              The Real to have a name returned.
     * @return                  The name of the Real given the current
     *                          name-use mode.
     */
    protected String getName(Real real) {

        return (SPECIFIED_NAME == nameUse)
               ? specifiedName
               : (real == null)
                 ? "<null>"
                 : ((RealType) real.getType()).getName();
    }

    /**
     * Returns the numeric-string of the given Real, given the current
     * unit-use mode.
     * @param real              The Real to have a numeric-string returned.
     * @return                  The numeric-string of the Real given the
     *                          current unit-use mode.
     */
    protected String getNumericString(Real real) {

        String numericString;

        try {
            double numericValue;

            switch (unitUse) {

              case SPECIFIED_UNIT :
                  numericValue = (real == null)
                                 ? Double.NaN
                                 : real.getValue(specifiedUnit);
                  break;

              case REALTYPE_UNIT :
                  numericValue = (real == null)
                                 ? Double.NaN
                                 : real.getValue(((RealType) real.getType())
                                     .getDefaultUnit());
                  break;

              case REAL_UNIT :
              default :
                  numericValue = (real == null)
                                 ? Double.NaN
                                 : real.getValue();
            }

            numericString = numberFormat.format(numericValue);
        } catch (Exception e) {
            numericString = e.toString();
        }

        return numericString;
    }

    /**
     * Returns the unit-specification of the given Real, given the current
     * unit-use mode.
     * @param real              The Real to have a unit-specification returned.
     * @return                  The unit-specification of the Real given the
     *                          current unit-use mode.
     */
    protected String getUnitString(Real real) {

        String unitString;

        switch (unitUse) {

          case SPECIFIED_UNIT :
              unitString = specifiedUnitString;
              break;

          case REALTYPE_UNIT :
              unitString = (real == null)
                           ? "<null>"
                           : ((RealType) real.getType()).getDefaultUnit()
                               .toString();
              break;

          case REAL_UNIT :
          default :
              unitString = (real == null)
                           ? "<null>"
                           : real.getUnit().toString();
        }

        return unitString;
    }
}







