/*
 * $Id: AerologicalReadoutTable.java,v 1.19 2005/05/13 18:33:23 jeffmc Exp $
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



import java.awt.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.text.NumberFormat;

import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.*;

import ucar.visad.quantities.*;

import visad.*;
import visad.data.units.*;


/**
 * Provides support for a readout-table of Skew-T values.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.19 $ $Date: 2005/05/13 18:33:23 $
 */
public final class AerologicalReadoutTable extends JTable {

    /**
     * The pressure readout.
     */
    private PressureReadout pressureReadout;

    /**
     * The altitude readout.
     */
    private AltitudeReadout altitudeReadout;

    /**
     * The background temperature readout.
     */
    private TemperatureReadout bgTemperatureReadout;

    /**
     * The background potential temperature readout.
     */
    private PotentialTemperatureReadout bgPotentialTemperatureReadout;

    /**
     * The background saturation equivalent potential temperature readout.
     */
    private SatEquivalentPotTempReadout bgSatEquivalentPotTempReadout;

    /**
     * The background mixing-ratio readout.
     */
    private MixingRatioReadout bgSatMixingRatioReadout;

    /**
     * The profile temperature readout.
     */
    private TemperatureReadout profileTemperatureReadout;

    /**
     * The profile dew-point readout.
     */
    private TemperatureReadout profileDewPointReadout;

    /**
     * The profile wind speed readout.
     */
    private WindSpeedReadout profileWindSpeedReadout;

    /**
     * The profile wind direction readout.
     */
    private WindDirectionReadout profileWindDirectionReadout;

    /**
     * The profile mixing-ratio readout.
     */
    private MyReadout profileMixingRatioReadout;

    /**
     * The LCL pressure readout.
     */
    private LCLPressureReadout lclPressureReadout;

    /**
     * The LCL temperature readout.
     */
    private LCLTemperatureReadout lclTemperatureReadout;

    /**
     * The LCL altitude readout.
     */
    private LCLAltitudeReadout lclAltitudeReadout;

    /**
     * The LFC readout.
     */
    private PressureReadout lfcReadout;

    /** LFC temperature readout */
    private TemperatureReadout lfcTempReadout;

    /**
     * The LNB readout.
     */
    private PressureReadout lnbReadout;

    /** LNB temperature readout */
    private final TemperatureReadout lnbTempReadout;

    /**
     * The CAPE readout.
     */
    private CapeReadout capeReadout;

    /**
     * The CIN readout.
     */
    private CinReadout cinReadout;

    /**
     * Constructs from nothing.
     *
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public AerologicalReadoutTable() throws VisADException, RemoteException {

        setShowVerticalLines(false);

        MyTableModel tableModel = new MyTableModel();

        try {
            pressureReadout               = new PressureReadout();
            altitudeReadout               = new AltitudeReadout();
            bgTemperatureReadout          = new TemperatureReadout();
            bgPotentialTemperatureReadout = new PotentialTemperatureReadout();
            bgSatEquivalentPotTempReadout = new SatEquivalentPotTempReadout();
            bgSatMixingRatioReadout =
                new MixingRatioReadout("Saturation Mixing-Ratio");
            profileTemperatureReadout   = new TemperatureReadout();
            profileDewPointReadout      = new TemperatureReadout("Dew-Point");
            profileWindSpeedReadout     = new WindSpeedReadout();
            profileWindDirectionReadout = new WindDirectionReadout();
            profileMixingRatioReadout   = new MixingRatioReadout();
            lclPressureReadout          = new LCLPressureReadout();
            lclTemperatureReadout       = new LCLTemperatureReadout();
            lclAltitudeReadout          = new LCLAltitudeReadout();
            capeReadout                 = new CapeReadout();
            cinReadout                  = new CinReadout();

            // Background:
            tableModel.addRowEntry(new SeparatorRowEntry("Background:"));
            tableModel.addRowEntry(new RealRowEntry(pressureReadout, "    "));
            tableModel.addRowEntry(new RealRowEntry(altitudeReadout, "    "));
            tableModel.addRowEntry(new RealRowEntry(bgTemperatureReadout,
                                                    "    "));
            tableModel.addRowEntry(
                new RealRowEntry(bgPotentialTemperatureReadout, "    "));
            tableModel.addRowEntry(
                new RealRowEntry(bgSatEquivalentPotTempReadout, "    "));
            tableModel.addRowEntry(new RealRowEntry(bgSatMixingRatioReadout,
                                                    "    "));

            // Profile at Background Pressure:
            tableModel.addRowEntry(
                new SeparatorRowEntry("Profile at Background Pressure:"));
            tableModel.addRowEntry(new RealRowEntry(profileTemperatureReadout,
                                                    "    "));
            tableModel.addRowEntry(new RealRowEntry(profileDewPointReadout,
                                                    "    "));
            tableModel.addRowEntry(new RealRowEntry(profileWindSpeedReadout,
                                                    "    "));
            tableModel.addRowEntry(
                new RealRowEntry(profileWindDirectionReadout, "    "));

            /*
            tableModel.addRowEntry(new RealRowEntry(profileMixingRatioReadout,
                                                    "    "));
             */

            // LNB:
            tableModel.addRowEntry(new SeparatorRowEntry("LNB:"));
            tableModel.addRowEntry(new RealRowEntry(lnbReadout =
                new PressureReadout(), "    "));
            tableModel.addRowEntry(new RealRowEntry(lnbTempReadout =
                new TemperatureReadout(), "    "));

            // LFC:
            tableModel.addRowEntry(new SeparatorRowEntry("LFC:"));
            tableModel.addRowEntry(new RealRowEntry(lfcReadout =
                new PressureReadout(), "    "));
            tableModel.addRowEntry(new RealRowEntry(lfcTempReadout =
                new TemperatureReadout(), "    "));

            // LCL:
            tableModel.addRowEntry(new SeparatorRowEntry("LCL:"));
            tableModel.addRowEntry(new RealRowEntry(lclPressureReadout,
                                                    "    "));
            tableModel.addRowEntry(new RealRowEntry(lclTemperatureReadout,
                                                    "    "));

            /*
            tableModel.addRowEntry(new RealRowEntry(lclAltitudeReadout,
                                                    "    "));
             */
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't set readouts: " + e);
        }

        tableModel.addRowEntry(new RealRowEntry(capeReadout, ""));
        tableModel.addRowEntry(new RealRowEntry(cinReadout, ""));
        setModel(tableModel);

        TableColumnModel columnModel = getColumnModel();
        DefaultTableCellRenderer numericRenderer =
            new DefaultTableCellRenderer();

        numericRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        columnModel.getColumn(1).setCellRenderer(numericRenderer);
        setPreferredSize();
    }

    /**
     * Sets the preferred size of this component.
     *
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setPreferredSize() throws VisADException, RemoteException {

        setColumnWidth(this, 0);
        setColumnWidth(this, 1);
        setColumnWidth(this, 2);
    }

    /**
     * Sets the pressure property.
     * @param pressure          The new pressure.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPressure(Real pressure)
            throws VisADException, RemoteException {

        pressureReadout.setPressure(pressure);
        altitudeReadout.setPressure(pressure);
        bgPotentialTemperatureReadout.setPressure(pressure);
        bgSatEquivalentPotTempReadout.setPressure(pressure);
        bgSatMixingRatioReadout.setPressure(pressure);

        // profileMixingRatio.setPressure(pressure);
        // lclPressureReadout.setPressure(pressure);
        // lclTemperatureReadout.setPressure(pressure);
        // lclAltitudeReadout.setPressure(pressure);
    }

    /**
     * Sets the background temperature property.
     * @param temperature       The new temperature.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setBackgroundTemperature(Real temperature)
            throws VisADException, RemoteException {

        bgTemperatureReadout.setTemperature(temperature);
        bgPotentialTemperatureReadout.setTemperature(temperature);
        bgSatEquivalentPotTempReadout.setTemperature(temperature);
        bgSatMixingRatioReadout.setTemperature(temperature);
    }

    /**
     * Sets the profile temperature property.
     * @param temperature       The new profile temperature.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileTemperature(Real temperature)
            throws VisADException, RemoteException {

        profileTemperatureReadout.setTemperature(temperature);

        // lclPressureReadout.setTemperature(temperature);
        // lclTemperatureReadout.setTemperature(temperature);
        // lclAltitudeReadout.setTemperature(temperature);
    }

    /**
     * Sets the profile dew-point property.
     * @param dewPoint  The new profile dew-point.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileDewPoint(Real dewPoint)
            throws VisADException, RemoteException {

        profileDewPointReadout.setTemperature(dewPoint);

        // profileMixingRatioReadout.setTemperature(dewPoint);
        // lclPressureReadout.setDewPoint(dewPoint);
        // lclTemperatureReadout.setDewPoint(dewPoint);
        // lclAltitudeReadout.setDewPoint(dewPoint);
    }

    /**
     * Sets the profile wind speed property.
     * @param windSpeed  The new profile wind speed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileWindSpeed(Real windSpeed)
            throws VisADException, RemoteException {

        profileWindSpeedReadout.setWindSpeed(windSpeed);

    }

    /**
     * Sets the profile wind direction property.
     * @param windDir  The new profile wind direction.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileWindDirection(Real windDir)
            throws VisADException, RemoteException {

        profileWindDirectionReadout.setWindDirection(windDir);

    }

    /**
     * Sets the profile mixing-ratio.
     *
     * @param ratio               The profile mixing ratio.
     */
    public void setProfileMixingRatio(Real ratio) {
        profileMixingRatioReadout.setReal(ratio);
    }

    /**
     * Sets the LCL pressure.
     *
     * @param pres                The LCL pressure.
     */
    public void setLclPressure(Real pres) {
        lclPressureReadout.setReal(pres);
    }

    /**
     * Sets the LCL temperature.
     *
     * @param temp                The LCL temperature.
     */
    public void setLclTemperature(Real temp) {
        lclTemperatureReadout.setReal(temp);
    }

    /**
     * Sets the profile LFC property.
     * @param lfc      The new profile LFC.
     */
    public void setLfc(Real lfc) {
        lfcReadout.setPressure(lfc);
    }

    /**
     * Sets the temperature at the LFC.
     *
     * @param temp     The temperature at the LFC
     */
    public void setLfcTemperature(Real temp) {
        lfcTempReadout.setTemperature(temp);
    }

    /**
     * Sets the profile Level of NeutralBuoyancy (LNB) property.
     * @param lnb      The new profile LNB.
     */
    public void setLnb(Real lnb) {
        lnbReadout.setPressure(lnb);
    }

    /**
     * Sets the temperature at the LNB.
     *
     * @param temp     The temperature at the LNB
     */
    public void setLnbTemperature(Real temp) {
        lnbTempReadout.setTemperature(temp);
    }

    /**
     * Sets the profile CAPE property.
     * @param cape      The new profile CAPE.
     */
    public void setCape(Real cape) {
        capeReadout.setCape(cape);
    }

    /**
     * Sets the profile CIN property.
     * @param cin       The new profile CIN.
     */
    public void setCin(Real cin) {
        cinReadout.setCin(cin);
    }

    /**
     * Sets the width of a column.
     * @param table             The table.
     * @param columnIndex       The column index.
     */
    protected static void setColumnWidth(JTable table, int columnIndex) {

        int         width  = getPreferredColumnWidth(table, columnIndex);
        TableColumn column = table.getColumnModel().getColumn(columnIndex);

        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * Provides support for the data model behind the table.
     */
    protected class MyTableModel extends AbstractTableModel {

        /**
         * The table rows.
         */
        private ArrayList rowEntries = new ArrayList();

        /**
         * Constructs from nothing.
         */
        public MyTableModel() {}

        /**
         * Returns the number of rows.
         * @return              The number of rows.
         */
        public int getRowCount() {
            return rowEntries.size();
        }

        /**
         * Returns the number of columns.
         * @return              The number of columns.
         */
        public int getColumnCount() {
            return 3;
        }

        /**
         * Returns the value of a cell.
         * @param row           The row index.
         * @param column        The column index.
         * @return              The value of the given cell.
         */
        public Object getValueAt(int row, int column) {
            return ((RowEntry) rowEntries.get(row)).getValueAt(column);
        }

        /**
         * Adds a row to the model.
         * @param entry         The row to be added.
         */
        public void addRowEntry(RowEntry entry) {
            entry.setRowIndex(rowEntries.size());
            rowEntries.add(entry);
        }
    }

    /**
     * Provides support for a row in the table model.
     */
    protected abstract class RowEntry {

        /**
         * The index of the row.
         */
        protected int rowIndex;

        /**
         * Constructs from nothing.
         */
        protected RowEntry() {}

        /**
         * Constructs from a row index.
         * @param rowIndex      The row index.
         */
        public void setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        /**
         * Returns the value of the cell at a given column.
         * @param column        The column index.
         * @return              The value of the given cell.
         */
        public abstract Object getValueAt(int column);
    }

    /**
     * Provides support for rows with Real values.
     */
    protected class RealRowEntry extends RowEntry {

        /**
         * The prefix for the ID.
         */
        private String prefix;

        /**
         * The readout.
         */
        private RealReadout readout;

        /**
         * Constructs from a readout.
         * @param readout       The readout.
         */
        public RealRowEntry(RealReadout readout) {
            this(readout, "");
        }

        /**
         * Constructs from a readout and an ID prefix.
         * @param readout       The readout.
         * @param prefix        The prefix for the ID.
         */
        public RealRowEntry(RealReadout readout, String prefix) {

            this.readout = readout;
            this.prefix  = prefix;

            readout.addPropertyChangeListener(RealReadout.NAME,
                                              new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    ((MyTableModel) getModel()).fireTableCellUpdated(rowIndex,
                            0);
                }
            });
            readout.addPropertyChangeListener(RealReadout.NUMERIC_STRING,
                                              new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    ((MyTableModel) getModel()).fireTableCellUpdated(rowIndex,
                            1);
                }
            });
            readout.addPropertyChangeListener(RealReadout.UNIT_STRING,
                                              new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    ((MyTableModel) getModel()).fireTableCellUpdated(rowIndex,
                            2);
                }
            });
        }

        /**
         * Returns the value at a given column.
         * @param column        The column index.
         * @return              The value at the given column.
         */
        public Object getValueAt(int column) {

            return (column == 0)
                   ? prefix + readout.getName()
                   : (column == 1)
                     ? readout.getNumericString()
                     : readout.getUnitString();
        }
    }

    /**
     * Provides support for separating rows in the table.
     */
    protected class SeparatorRowEntry extends RowEntry {

        /**
         * The name of the separator.
         */
        private String name;

        /**
         * Constructs from a name.
         * @param name          The name of the separator.
         */
        public SeparatorRowEntry(String name) {
            this.name = name;
        }

        /**
         * Returns the value of a column.
         * @param column        The column index.
         * @return              The value of the given column.
         */
        public Object getValueAt(int column) {

            return (column == 0)
                   ? name + " "
                   : null;
        }
    }

    /**
     * Returns the preferred width of a column in a given table.
     * @param table             The table.
     * @param columnIndex       The column index.
     * @return                  The preferred width of the given column.
     */
    protected static int getPreferredColumnWidth(JTable table,
            int columnIndex) {

        TableColumnModel  columnModel = table.getColumnModel();
        TableColumn       column      = columnModel.getColumn(columnIndex);
        TableCellRenderer renderer    = column.getHeaderRenderer();
        int               maxWidth    = 0;

        if (renderer != null) {
            maxWidth = renderer.getTableCellRendererComponent(table,
                    column.getHeaderValue(), false, false, 0,
                    0).getPreferredSize().width;
        }

        for (int rowIndex = 0, n = table.getRowCount(); rowIndex < n;
                ++rowIndex) {
            renderer = table.getCellRenderer(rowIndex, columnIndex);

            Object value = table.getValueAt(rowIndex, columnIndex);
            int width = renderer.getTableCellRendererComponent(table, value,
                            false, false, rowIndex,
                            columnIndex).getPreferredSize().width;

            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth + 1;  // HACK: workaround for JTable preferred-size bug
    }

    /**
     * Provides support for readouts in a table.
     */
    protected static class MyReadout extends RealReadout {

        /**
         * Constructs from a name, unit, and format.
         * @param name          The name of the readout.
         * @param unitString    The unit specification for the readout.
         * @param fractionDigitCount    The number of digits to have after the
         *                              decimal point.
         * @throws ParseException       Invalid unit specification argument.
         */
        public MyReadout(String name, String unitString, int fractionDigitCount)
                throws ParseException {

            setSpecifiedName(name);
            setNameUse(Readout.SPECIFIED_NAME);
            setSpecifiedUnit(unitString);
            setUnitUse(Readout.SPECIFIED_UNIT);

            NumberFormat format = NumberFormat.getNumberInstance();

            format.setMinimumFractionDigits(fractionDigitCount);
            format.setMaximumFractionDigits(fractionDigitCount);
            setFormat(format);
        }
    }

    /**
     * Provides support for pressure readouts.
     */
    protected static class PressureReadout extends MyReadout {

        /**
         * Constructs from nothing.
         *
         * @throws ParseException if the unit specification "hPa" couldn't be
         *                        parsed.
         */
        public PressureReadout() throws ParseException {
            super("Pressure", "hPa", 1);
        }

        /**
         * Sets the pressure property.
         * @param pressure      The new value.
         */
        public void setPressure(Real pressure) {
            setReal(pressure);
        }
    }

    /**
     * Provides support for wind speed readouts.
     */
    protected static class WindSpeedReadout extends MyReadout {

        /**
         * Constructs from nothing.
         *
         * @throws ParseException if the unit specification "kts" couldn't be
         *                        parsed.
         */
        public WindSpeedReadout() throws ParseException {
            super("Wind Speed", "m/s", 1);
        }

        /**
         * Sets the wind speed property.
         * @param windSpeed      The new value.
         */
        public void setWindSpeed(Real windSpeed) {
            setReal(windSpeed);
        }
    }

    /**
     * Provides support for wind direction readouts.
     */
    protected static class WindDirectionReadout extends MyReadout {

        /**
         * Constructs from nothing.
         *
         * @throws ParseException if the unit specification "hPa" couldn't be
         *                        parsed.
         */
        public WindDirectionReadout() throws ParseException {
            super("Wind Direction", "degree", 1);
        }

        /**
         * Sets the wind direction property.
         * @param windDir      The new value.
         */
        public void setWindDirection(Real windDir) {
            setReal(windDir);
        }
    }

    /**
     * Provides support for temperature readouts.
     */
    protected static class TemperatureReadout extends MyReadout {

        /**
         * Constructs from nothing.
         *
         * @throws ParseException if the unit specification "degC" couldn't be
         *                        parsed.
         */
        public TemperatureReadout() throws ParseException {
            this("Temperature");
        }

        /**
         * Constructs from a name.
         *
         * @param name          The name for the readout.
         * @throws ParseException if the unit specification "degC" couldn't be
         *                        parsed.
         */
        public TemperatureReadout(String name) throws ParseException {
            super(name, "degC", 2);
        }

        /**
         * Sets the temperature property.
         * @param temperature   The new value.
         */
        public void setTemperature(Real temperature) {
            setReal(temperature);
        }
    }

    /**
     * Provides support for altitude readouts.
     */
    protected static class AltitudeReadout extends MyReadout {

        /**
         * Constructs from nothing.
         *
         * @throws ParseException if the unit specification "gpm" couldn't be
         *                        parsed.
         */
        public AltitudeReadout() throws ParseException {
            super("Geopotential Altitude", "gpm", 0);
        }

        /**
         * Sets the pressure property.
         * @param pressure      The new value.
         */
        public void setPressure(Real pressure) {

            try {
                setReal(
                    (Real) GeopotentialAltitude.fromAltitude(
                        AirPressure.toAltitude(pressure)));
            } catch (Exception e) {
                System.err.println("Couldn't set altitude readout: " + e);
            }
        }
    }

    /**
     * Provides support for readouts derived from pressure and temperature.
     */
    protected static abstract class PTDerivedReadout extends MyReadout {

        /**
         * The temperature.
         */
        private Real temperature;

        /**
         * The pressure.
         */
        private Real pressure;

        /**
         * Constructs from a name, unit, and display accuracy.
         * @param name          The name for the readout.
         * @param unitString    The unit specification for the readout.
         * @param fractionDigitCount    The number of digits to have after the
         *                              decimal point.
         * @throws ParseException       Invalid unit specificatin.
         */
        protected PTDerivedReadout(String name, String unitString, int fractionDigitCount)
                throws ParseException {
            super(name, unitString, fractionDigitCount);
        }

        /**
         * Sets the pressure property.
         * @param pressure      The new value.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setPressure(Real pressure)
                throws VisADException, RemoteException {

            this.pressure = pressure;

            compute();
        }

        /**
         * Returns the pressure property.
         * @return              The pressure.
         */
        public Real getPressure() {
            return pressure;
        }

        /**
         * Sets the temperature property.
         * @param temperature   The new value.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setTemperature(Real temperature)
                throws VisADException, RemoteException {

            this.temperature = temperature;

            compute();
        }

        /**
         * Returns the temperature property.
         * @return              The value of the temperature property.
         */
        public Real getTemperature() {
            return temperature;
        }

        /**
         * Computes the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected void compute() throws VisADException, RemoteException {

            if ((temperature != null) && (pressure != null)) {
                try {
                    setReal(compute(pressure, temperature));
                } catch (Exception e) {
                    System.err.println("Couldn't compute " + getName() + ": "
                                       + e);
                }
            }
        }

        /**
         * Computes the derived quantity from the pressure and temperature.
         * @param pressure      The pressure.
         * @param temperature   The temperature.
         * @return              The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected abstract Real compute(Real pressure, Real temperature)
         throws VisADException, RemoteException;
    }

    /**
     * Provides support for potential temperature readouts.
     */
    protected static class PotentialTemperatureReadout
            extends PTDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Bad internal unit specification.
         */
        public PotentialTemperatureReadout() throws ParseException {
            super("Potential Temperature", "K", 2);
        }

        /**
         * Computes the derived quantity.
         * @param pressure      The pressure.
         * @param temperature   The temperature.
         * @return              The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature)
                throws RemoteException, VisADException {
            return (Real) PotentialTemperature.create(pressure, temperature);
        }
    }

    /**
     * Provides support for saturation equivalent potential temperature
     * readouts.
     */
    protected static class SatEquivalentPotTempReadout
            extends PTDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Bad internal unit specification.
         */
        public SatEquivalentPotTempReadout() throws ParseException {
            super("Sat' Equiv' Pot' Temp'", "K", 2);
        }

        /**
         * Computes the derived quantity.
         * @param pressure      The pressure.
         * @param temperature   The temperature.
         * @return              The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature)
                throws RemoteException, VisADException {
            return (Real) SaturationEquivalentPotentialTemperature.create(
                pressure, temperature);
        }
    }

    /**
     * Provides support for mixing ratio readouts.
     */
    protected static class MixingRatioReadout extends PTDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Bad internal unit specification.
         */
        public MixingRatioReadout() throws ParseException {
            this("Mixing-Ratio");
        }

        /**
         * Constructs from a name for the readout.
         * @param name          The name for the readout.
         * @throws ParseException       Bad internal unit specification.
         */
        public MixingRatioReadout(String name) throws ParseException {
            super(name, "g/kg", 3);
        }

        /**
         * Computes the derived quantity.
         * @param pressure      The pressure.
         * @param temperature   The temperature.
         * @return              The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature)
                throws RemoteException, VisADException {
            return (Real) WaterVaporMixingRatio.create(pressure, temperature);
        }
    }

    /**
     * Provides support for readouts derived from pressure, temperature, and
     * mixing-ratio.
     */
    protected abstract static class PTRDerivedReadout extends MyReadout {

        /**
         * The temperature.
         */
        private Real temperature;

        /**
         * The pressure.
         */
        private Real pressure;

        /**
         * The dew-point.
         */
        private Real dewPoint;

        /**
         * Constructs from a name, unit, and format.
         *
         * @param name          The name of the readout.
         * @param unitString    The unit specification for the readout.
         * @param fractionDigitCount    The number of digits to have after the
         *                              decimal point.
         * @throws ParseException if the unit specification couldn't be parsed.
         */
        protected PTRDerivedReadout(String name, String unitString, int fractionDigitCount)
                throws ParseException {
            super(name, unitString, fractionDigitCount);
        }

        /**
         * Sets the pressure property.
         * @param pressure      The new value.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setPressure(Real pressure)
                throws VisADException, RemoteException {

            this.pressure = pressure;

            compute();
        }

        /**
         * Gets the value of the pressure property.
         * @return              The value of the pressure property.
         */
        public Real getPressure() {
            return pressure;
        }

        /**
         * Sets the temperature property.
         * @param temperature   The new value.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setTemperature(Real temperature)
                throws VisADException, RemoteException {

            this.temperature = temperature;

            compute();
        }

        /**
         * Gets the value of the temperature property.
         * @return              The value of the temperature property.
         */
        public Real getTemperature() {
            return temperature;
        }

        /**
         * Sets the dew-point property.
         *
         * @param dewPoint      The new value.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setDewPoint(Real dewPoint)
                throws VisADException, RemoteException {

            this.dewPoint = dewPoint;

            compute();
        }

        /**
         * Gets the value of the dew-point property.
         * @return              The value of the dew-point property.
         */
        public Real getDewPoint() {
            return dewPoint;
        }

        /**
         * Computes the value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected void compute() throws VisADException, RemoteException {

            if ((temperature != null) && (pressure != null)
                    && (dewPoint != null)) {
                try {
                    setReal(compute(pressure, temperature, dewPoint));
                } catch (Exception e) {
                    System.err.println("Couldn't compute " + getName() + ": "
                                       + e);
                }
            }
        }

        /**
         * Computes the value of the derived quantity from pressure,
         * temperature, and dew-point.
         * @param pressure              The pressure.
         * @param temperature           The temperature.
         * @param dewPoint              The dew-point.
         * @return                      The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected abstract Real compute(Real pressure, Real temperature, Real dewPoint)
         throws VisADException, RemoteException;
    }

    /**
     * Provides support for LCL readouts.
     */
    protected class LCLPressureReadout extends PTRDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public LCLPressureReadout() throws ParseException {
            super("Pressure", "hPa", 1);
        }

        /**
         * Computes the value of the derived quantity from pressure,
         * temperature, and dew-point.
         * @param pressure              The pressure.
         * @param temperature           The temperature.
         * @param dewPoint              The dew-point.
         * @return                      The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature, Real dewPoint)
                throws VisADException, RemoteException {

            return (Real) SaturationPointPressure.create(
                pressure, temperature,
                SaturationPointTemperature.create(
                    pressure, temperature,
                    WaterVaporMixingRatio.create(pressure, dewPoint)));
        }
    }

    /**
     * Provides support for LCL temperature readouts.
     */
    protected class LCLTemperatureReadout extends PTRDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public LCLTemperatureReadout() throws ParseException {
            super("Temperature", "degC", 2);
        }

        /**
         * Computes the value of the derived quantity from pressure,
         * temperature, and dew-point.
         * @param pressure              The pressure.
         * @param temperature           The temperature.
         * @param dewPoint              The dew-point.
         * @return                      The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature, Real dewPoint)
                throws VisADException, RemoteException {

            return (Real) SaturationPointTemperature.create(pressure,
                    temperature,
                    WaterVaporMixingRatio.create(pressure, dewPoint));
        }
    }

    /**
     * Provides support for LCL altitude readouts.
     */
    protected class LCLAltitudeReadout extends PTRDerivedReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public LCLAltitudeReadout() throws ParseException {
            super("Geopotential Altitude", "gpm", 0);
        }

        /**
         * Computes the value of the derived quantity from pressure,
         * temperature, and dew-point.
         * @param pressure              The pressure.
         * @param temperature           The temperature.
         * @param dewPoint              The dew-point.
         * @return                      The value of the derived quantity.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected Real compute(Real pressure, Real temperature, Real dewPoint)
                throws VisADException, RemoteException {

            return (Real) GeopotentialAltitude.fromAltitude(
                AirPressure.toAltitude(
                    SaturationPointPressure.create(
                        pressure, temperature,
                        SaturationPointTemperature.create(
                            pressure, temperature,
                            WaterVaporMixingRatio.create(
                                pressure, dewPoint)))));
        }
    }

    /**
     * Provides support for LFC readouts.
     */
    protected class LfcReadout extends MyReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public LfcReadout() throws ParseException {
            super("LFC", "hPa", 1);
        }

        /**
         * Sets the value of the LFC property.
         * @param lfc          The new value.
         */
        public void setLfc(Real lfc) {

            try {
                setReal(lfc);
            } catch (Exception e) {
                System.err.println("Couldn't set LFC readout: " + e);
            }
        }
    }

    /**
     * Provides support for Level of Neutral Buoyancy (LNB) readouts.
     */
    protected class LnbReadout extends MyReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public LnbReadout() throws ParseException {
            super("LNB", "hPa", 1);
        }

        /**
         * Sets the value of the LNB property.
         * @param lnb          The new value.
         */
        public void setLnb(Real lnb) {

            try {
                setReal(lnb);
            } catch (Exception e) {
                System.err.println("Couldn't set LNB readout: " + e);
            }
        }
    }

    /**
     * Provides support for CAPE readouts.
     */
    protected class CapeReadout extends MyReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public CapeReadout() throws ParseException {
            super("CAPE", "J/kg", 0);
        }

        /**
         * Sets the value of the CAPE property.
         * @param cape          The new value.
         */
        public void setCape(Real cape) {

            try {
                setReal(cape);
            } catch (Exception e) {
                System.err.println("Couldn't set CAPE readout: " + e);
            }
        }
    }

    /**
     * Provides support for CIN readouts.
     */
    protected class CinReadout extends MyReadout {

        /**
         * Constructs from nothing.
         * @throws ParseException       Invalid internal unit specification.
         */
        public CinReadout() throws ParseException {
            super("CIN", "J/kg", 0);
        }

        /**
         * Sets the value of the CIN property.
         * @param cin           The new value.
         */
        public void setCin(Real cin) {

            try {
                setReal(cin);
            } catch (Exception e) {
                System.err.println("Couldn't set CIN readout: " + e);
            }
        }
    }
}







