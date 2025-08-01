/*
 * $Id: SkewTCoordinateSystem.java,v 1.14 2005/05/13 18:33:37 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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

import java.util.Arrays;

import ucar.visad.quantities.*;

import visad.*;


/**
 * <p>Provides support for converting between the (x,y) coordinates on a skew T
 * - log P diagram and (pressure,temperature) coordinates.</p>
 *
 * <p>Instances of this class are immutable.</p>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.14 $ $Date: 2005/05/13 18:33:37 $
 */
public final class SkewTCoordinateSystem extends AerologicalCoordinateSystem {

    /**
     * The default SkewTCoordinateSystem.
     */
    private static SkewTCoordinateSystem defaultCS;

    /**
     * The minimum X coordinate.
     */
    private final Real minimumX;

    /**
     * The maximum X coordinate.
     */
    private final Real maximumX;

    /**
     * The minimum Y coordinate.
     */
    private final Real minimumY;

    /**
     * The maximum Y coordinate.
     */
    private final Real maximumY;

    /**
     * The minimum display pressure.
     * @serial
     */
    private final Real minimumPressure;

    /**
     * The maximum display pressure.
     * @serial
     */
    private final Real maximumPressure;

    /**
     * The minimum display temperature.
     * @serial
     */
    private final Real minimumTemperature;

    /**
     * The maximum display temperature.
     * @serial
     */
    private final Real maximumTemperature;

    /**
     * The tangent of the isotherms.
     * @serial
     */
    private final Real isothermTangent;

    /**
     * The change in temperature per change in X coordinate.  Increasing this
     * value squeezes the isotherms together.
     * @serial
     */
    private final Real temperaturePerLogPressure;

    /**
     * The minimum X coordinate.
     * @serial
     */
    private double minX;

    /**
     * The maximum X coordinate.
     * @serial
     */
    private double maxX;

    /**
     * The minimum Y coordinate.
     * @serial
     */
    private double minY;

    /**
     * The maximum Y coordinate.
     * @serial
     */
    private double maxY;

    /**
     * The minimum pressure coordinate.
     * @serial
     */
    private final double minP;

    /**
     * The maximum pressure coordinate.
     * @serial
     */
    private final double maxP;

    /**
     * The minimum temperature coordinate.
     * @serial
     */
    private final double minT;

    /**
     * The maximum temperature coordinate.
     * @serial
     */
    private final double maxT;

    /**
     * The tangent of the isotherms.
     * @serial
     */
    private final double tangent;

    /**
     * The change in Y per change in natural logarithm of pressure.
     * @serial
     */
    private final double yPerLogP;

    /**
     * The natural logarithm of the minimum pressure.
     * @serial
     */
    private final double logMinP;

    /**
     * The change in X per change in temperature.
     * @serial
     */
    private final double xPerT;

    /**
     * The default minimum pressure.
     */
    public static final Real DEFAULT_MINIMUM_PRESSURE;

    /**
     * The default maximum pressure.
     */
    public static final Real DEFAULT_MAXIMUM_PRESSURE;

    /**
     * The default minimum temperature.
     */
    public static final Real DEFAULT_MINIMUM_TEMPERATURE;

    /**
     * The default maximum temperature.
     */
    public static final Real DEFAULT_MAXIMUM_TEMPERATURE;

    /**
     * The default isotherm tangent.
     */
    public static final Real DEFAULT_ISOTHERM_TANGENT;

    /**
     * The default change in temperature per natural logarithm of pressure in
     * the vertical.
     */
    public static final Real DEFAULT_TEMPERATURE_PER_LOG_PRESSURE;

    /**
     * The quantity one.
     */
    private static final Real ONE;

    /**
     * The quantity negative one.
     */
    private static final Real NEGATIVE_ONE;

    static {
        try {

            /*
             * Values for the following are based on Department of Defense form
             * DOD-WPC 9-16-1 ("USAF SKEW T, log p DIAGRAM"; current as of March
             * 1978).
             */
            DEFAULT_MINIMUM_PRESSURE = new Real(AirPressure.getRealType(),
                                                100, CommonUnits.HECTOPASCAL);
            DEFAULT_MAXIMUM_PRESSURE = new Real(AirPressure.getRealType(),
                                                1050,
                                                CommonUnits.HECTOPASCAL);
            DEFAULT_MINIMUM_TEMPERATURE =
                new Real(AirTemperature.getRealType(), -122.5,
                         CommonUnits.CELSIUS);
            DEFAULT_MAXIMUM_TEMPERATURE =
                new Real(AirTemperature.getRealType(), 52.0,
                         CommonUnits.CELSIUS);

            RealType itt = RealType.getRealType("SkewTIsothermTangent",
                                                CommonUnit.dimensionless,
                                                (Set) null);

            DEFAULT_ISOTHERM_TANGENT = new Real(itt, 1.09);

            RealType tplpt =
                RealType.getRealType("SkewTTemperaturePerLogPressure",
                                     SI.kelvin, (Set) null);

            DEFAULT_TEMPERATURE_PER_LOG_PRESSURE = new Real(tplpt, 32.534152);
            ONE = new Real(RealType.getRealType("SkewTCoordinateSystemOne"),
                           1);
            NEGATIVE_ONE = new Real(
                RealType.getRealType("SkewTCoordinateSystemNegativeOne"), -1);
            defaultCS =
                new SkewTCoordinateSystem(new RealTupleType(new RealType[]{
                    RealType.XAxis,
                    RealType.YAxis }), DEFAULT_MINIMUM_PRESSURE,
                                       DEFAULT_MAXIMUM_PRESSURE,
                                       DEFAULT_MINIMUM_TEMPERATURE,
                                       DEFAULT_MAXIMUM_TEMPERATURE,
                                       DEFAULT_ISOTHERM_TANGENT,
                                       DEFAULT_TEMPERATURE_PER_LOG_PRESSURE,
                                       AirPressure.getRealType().getDefaultUnit(),
                                       AirTemperature.getRealType().getDefaultUnit(),
                                       NEGATIVE_ONE, ONE, NEGATIVE_ONE, ONE);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't initialize class: " + e);
        }
    }

    /**
     * Constructs an instance.  The resulting coordinate
     * system will be centered in the given (X,Y) region and the given, extreme
     * pressure and temperature values will not extend beyond it.
     *
     * @param referenceTupleType
     *                          The type of the reference coordinate system.
     * @param minimumPressure   The minimum, displayed pressure.
     * @param maximumPressure   The maximum, displayed pressure.
     * @param minimumTemperature
     *                          The minimum, displayed temperature.
     * @param maximumTemperature
     *                          The maximum, displayed temperature.
     * @param isothermTangent   The tangent of the isotherms in the
     *                          background plot.
     * @param temperaturePerLogPressure
     *                          The rate of change in temperature per natural
     *                          logarithm of pressure in the vertical.
     *                          Increasing this value squeezes the isotherms
     *                          together on the plot.
     * @param pressureUnit      The unit of pressure for numeric values.
     * @param temperatureUnit   The unit of temperature for numeric
     *                          values.
     * @param minimumX          The minimum X coordinate.
     * @param maximumX          The maximum X coordinate.
     * @param minimumY          The minimum Y coordinate.
     * @param maximumY          The maximum Y coordinate.
     * @throws UnitException    Improper unit.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SkewTCoordinateSystem(RealTupleType referenceTupleType,
                                  Real minimumPressure, Real maximumPressure,
                                  Real minimumTemperature,
                                  Real maximumTemperature,
                                  Real isothermTangent, Real temperaturePerLogPressure, Unit pressureUnit, Unit temperatureUnit, Real minimumX, Real maximumX, Real minimumY, Real maximumY)
                                   throws UnitException, VisADException {

        super(referenceTupleType,
              makeCSUnits(referenceTupleType.getDimension(), pressureUnit,
                          temperatureUnit));

        this.minimumPressure           = minimumPressure;
        this.maximumPressure           = maximumPressure;
        this.minimumTemperature        = minimumTemperature;
        this.maximumTemperature        = maximumTemperature;
        this.isothermTangent           = isothermTangent;
        this.temperaturePerLogPressure = temperaturePerLogPressure;

        RealType xType = (RealType) referenceTupleType.getComponent(0);
        RealType yType = (RealType) referenceTupleType.getComponent(1);
        Unit     xUnit = xType.getDefaultUnit();
        Unit     yUnit = yType.getDefaultUnit();

        minX = minimumX.getValue(xUnit);
        maxX = maximumX.getValue(xUnit);
        minY = minimumY.getValue(yUnit);
        maxY = maximumY.getValue(yUnit);

        Real   verticalTemperatureChange = null;
        Real   lowerLeftTemperature      = null;
        double aspectRatio               = Double.NaN;

        try {
            verticalTemperatureChange = (Real) maximumPressure.divide(
                minimumPressure).log().multiply(temperaturePerLogPressure);
            lowerLeftTemperature =
                (Real) minimumTemperature.add(verticalTemperatureChange);
            aspectRatio = ((Real) verticalTemperatureChange.divide(
                maximumTemperature.subtract(lowerLeftTemperature)).multiply(
                isothermTangent)).getValue();
        } catch (RemoteException e) {}  // ignore because data is local

        if (aspectRatio <= (maxY - minY) / (maxX - minX)) {
            double radius = (maxX - minX) * aspectRatio / 2;

            minY = -radius;
            maxY = radius;
        } else {
            double radius = (maxY - minY) / (aspectRatio * 2);

            minX = -radius;
            maxX = radius;
        }

        this.minimumX = new Real(xType, minX);
        this.maximumX = new Real(xType, maxX);
        this.minimumY = new Real(yType, minY);
        this.maximumY = new Real(yType, maxY);
        minP          = minimumPressure.getValue(pressureUnit);
        maxP          = maximumPressure.getValue(pressureUnit);
        minT          = minimumTemperature.getValue(temperatureUnit);
        maxT          = maximumTemperature.getValue(temperatureUnit);
        tangent       = isothermTangent.getValue(CommonUnit.dimensionless);

        double minTAtMaxP = lowerLeftTemperature.getValue(temperatureUnit);

        logMinP  = (float) Math.log(minP);
        yPerLogP = (float) ((minY - maxY) / (Math.log(maxP) - logMinP));
        xPerT    = (maxX - minX) / (maxT - minTAtMaxP);
    }

    /**
     * Factory method for obtaining an instance.  The RealTupleType of the
     * reference coordinate system will be (RealType.XAxis,RealType.YAxis) and
     * the default coordinate transformation will be used.
     *
     * @return                  The default SkewTCoordinateSystem.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static SkewTCoordinateSystem instance() throws VisADException {
        return defaultCS;
    }

    /**
     * Factory method for obtaining an instance.  The default coordinate
     * transformation will be used.
     *
     * @param referenceTupleType
     *                          The type of the reference coordinate system.
     *                          It shall have 2-3 components.  The first
     *                          component shall be the X component; the second
     *                          component shall be the Y component; any third
     *                          component is ignored.
     * @return                  The SkewTCoordinateSystem with
     *                          <code>referenceTupleType</code> as its
     *                          reference RealTupleType.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static SkewTCoordinateSystem instance(RealTupleType referenceTupleType)
            throws VisADException {

        return instance(referenceTupleType, DEFAULT_MINIMUM_PRESSURE,
                        DEFAULT_MAXIMUM_PRESSURE,
                        DEFAULT_MINIMUM_TEMPERATURE,
                        DEFAULT_MAXIMUM_TEMPERATURE,
                        DEFAULT_ISOTHERM_TANGENT,
                        DEFAULT_TEMPERATURE_PER_LOG_PRESSURE,
                        AirPressure.getRealType().getDefaultUnit(),
                        AirTemperature.getRealType().getDefaultUnit(),
                        NEGATIVE_ONE, ONE, NEGATIVE_ONE, ONE);
    }

    /**
     * Factory method for obtaining an instance.  The resulting coordinate
     * system will be centered in the given (X,Y) region and the given, extreme
     * pressure and temperature values will not extend beyond it.
     *
     * @param referenceTupleType
     *                          The type of the reference coordinate system.
     * @param minimumPressure   The minimum, displayed pressure.
     * @param maximumPressure   The maximum, displayed pressure.
     * @param minimumTemperature
     *                          The minimum, displayed temperature.
     * @param maximumTemperature
     *                          The maximum, displayed temperature.
     * @param isothermTangent   The tangent of the isotherms in the
     *                          background plot.
     * @param temperaturePerLogPressure
     *                          The rate of change in temperature per natural
     *                          logarithm of pressure in the vertical.
     *                          Increasing this value squeezes the isotherms
     *                          together on the plot.
     * @param pressureUnit      The unit of pressure for numeric values.
     * @param temperatureUnit   The unit of temperature for numeric
     *                          values.
     * @param minimumX          The minimum X coordinate.
     * @param maximumX          The maximum X coordinate.
     * @param minimumY          The minimum Y coordinate.
     * @param maximumY          The maximum Y coordinate.
     * @return                  The SkewTCoordinateSystem corresponding to the
     *                          input parameters.
     * @throws UnitException    Improper unit.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static SkewTCoordinateSystem instance(RealTupleType referenceTupleType,
            Real minimumPressure, Real maximumPressure,
            Real minimumTemperature, Real maximumTemperature,
            Real isothermTangent, Real temperaturePerLogPressure, Unit pressureUnit, Unit temperatureUnit, Real minimumX, Real maximumX, Real minimumY, Real maximumY)
             throws UnitException, VisADException {

        return new SkewTCoordinateSystem(referenceTupleType, minimumPressure,
                                         maximumPressure, minimumTemperature,
                                         maximumTemperature, isothermTangent,
                                         temperaturePerLogPressure,
                                         pressureUnit, temperatureUnit,
                                         minimumX, maximumX, minimumY,
                                         maximumY);
    }

    /**
     * Gets the minimum display pressure.
     *
     * @return                  The minimum display pressure.
     */
    public Real getMinimumPressure() {
        return minimumPressure;
    }

    /**
     * Gets the maximum display pressure.
     *
     * @return                  The maximum display pressure.
     */
    public Real getMaximumPressure() {
        return maximumPressure;
    }

    /**
     * Gets the minimum display temperature.
     *
     * @return                  The minimum display temperature.
     */
    public Real getMinimumTemperature() {
        return minimumTemperature;
    }

    /**
     * Gets the maximum display temperature.
     *
     * @return                  The maximum display temperature.
     */
    public Real getMaximumTemperature() {
        return maximumTemperature;
    }

    /**
     * Gets the tangent of the isotherms.
     *
     * @return                  The tangent of the isotherms.
     */
    public Real getIsothermTangent() {
        return isothermTangent;
    }

    /**
     * Gets the change in temperature per natural logarithm of pressure in the
     * vertical.  The greater this value, the closer together the isotherms are
     * on the plot.
     *
     * @return                  The change in temperature per natural logarithm
     *                          of pressure in the vertical.
     */
    public Real getTemperaturePerLogPressure() {
        return temperaturePerLogPressure;
    }

    /**
     * Gets the minimum X coordinate.
     *
     * @return                  The minimum X coordinate.
     */
    public Real getMinimumX() {
        return minimumX;
    }

    /**
     * Gets the maximum X coordinate.
     *
     * @return                  The maximum X coordinate.
     */
    public Real getMaximumX() {
        return maximumX;
    }

    /**
     * Gets the minimum Y coordinate.
     *
     * @return                  The minimum Y coordinate.
     */
    public Real getMinimumY() {
        return minimumY;
    }

    /**
     * Gets the maximum Y coordinate.
     *
     * @return                  The maximum Y coordinate.
     */
    public Real getMaximumY() {
        return maximumY;
    }

    /**
     * Transforms (pressure,temperature) coordinates to (X,Y) coordinates.
     *
     * @param coords    Real coordinates: <code>coords[0][i]</code>
     *                  and <code>coords[1][i]</code> are the
     *                  pressure and temperature coordinates,
     *                  respectively, of the <code>i</code>th point.
     *                  On output, <code>coords[0][i]</code> and
     *                  <code>coords[1][i]</code> are the corresponding
     *                  X and Y display coordinates, respectively.
     * @return          Corresponding display coordinates (i.e.
     *                  <code>coords</code>).
     */
    public double[][] toReference(double[][] coords) {

        if ((coords == null) || (coords.length < 2)) {
            throw new IllegalArgumentException("Invalid real coordinates");
        }

        int      npts = coords[0].length;
        double[] x    = coords[0];
        double[] y    = coords[1];

        for (int i = 0; i < npts; ++i) {
            double pressure    = x[i];
            double temperature = y[i];
            double deltaY      = yPerLogP * ((Math.log(pressure)) - logMinP);

            x[i] = xPerT * (temperature - minT) + minX + deltaY / tangent;
            y[i] = maxY + deltaY;
        }

        return coords;
    }

    /**
     * Transforms (X,Y) coordinates to (pressure,temperature) coordinates.
     *
     * @param coords    Display coordinates: <code>coords[0][i]</code>
     *                  and <code>coords[1][i]</code> are the X
     *                  and Y display coordinates, respectively,
     *                  of the <code>i</code>th point.  On
     *                  output, <code>coords[0][i]</code> and
     *                  <code>coords[1][i]</code> are the corresponding
     *                  pressure and temperature coordinates,
     *                  respectively.
     * @return          Corresponding real coordinates (i.e.
     *                  <code>coords</code>).
     */
    public double[][] fromReference(double[][] coords) {

        if ((coords == null) || (coords.length < 2)) {
            throw new IllegalArgumentException("Invalid real coordinates");
        }

        int      npts         = coords[0].length;
        double[] pressures    = coords[0];
        double[] temperatures = coords[1];

        for (int i = 0; i < npts; ++i) {

            // System.out.print("SkewTCoordinateSystem.fromReference(): (" +
            // coords[0][i] + "," + coords[1][i] + ") -> ");
            double x      = pressures[i];
            double deltaY = temperatures[i] - maxY;

            pressures[i]    = Math.exp(deltaY / yPerLogP + logMinP);
            temperatures[i] = (x - deltaY / tangent - minX) / xPerT + minT;

            // System.out.println("(" + coords[0][i] + "," +
            // coords[1][i] + ")");
        }

        return coords;
    }

    /**
     * Indicate whether or not this coordinate system is the same as
     * another.
     *
     * @param obj               The object to be compared with this one.
     * @return          <code>true</code> if and only if
     *                  <code>obj</code> is semantically identical to
     *                  this object.
     */
    public boolean equals(Object obj) {

        if ( !(obj instanceof SkewTCoordinateSystem)) {
            return false;
        }

        SkewTCoordinateSystem that = (SkewTCoordinateSystem) obj;

        return (that.minX == minX) && (that.maxX == maxX)
               && (that.minY == minY) && (that.maxY == maxY)
               && (that.minP == minP) && (that.maxP == maxP)
               && (that.minT == minT) && (that.maxT == maxT)
               && (that.tangent == tangent) && (that.yPerLogP == yPerLogP)
               && (that.logMinP == logMinP) && (that.xPerT == xPerT)
               && that.minimumX.equals(minimumX)
               && that.maximumX.equals(maximumX)
               && that.minimumY.equals(minimumY)
               && that.maximumY.equals(maximumY)
               && getReference().equals(that.getReference())
               && Arrays.equals(getCoordinateSystemUnits(),
                                that.getCoordinateSystemUnits());
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        return Double.valueOf(minX).hashCode() ^ Double.valueOf(maxX).hashCode()
               ^ Double.valueOf(minY).hashCode() ^ Double.valueOf(maxY).hashCode()
               ^ Double.valueOf(minP).hashCode() ^ Double.valueOf(maxP).hashCode()
               ^ Double.valueOf(minT).hashCode() ^ Double.valueOf(maxT).hashCode()
               ^ Double.valueOf(tangent).hashCode()
               ^ Double.valueOf(yPerLogP).hashCode()
               ^ Double.valueOf(logMinP).hashCode()
               ^ Double.valueOf(xPerT).hashCode() ^ minimumX.hashCode()
               ^ maximumX.hashCode() ^ minimumY.hashCode()
               ^ maximumY.hashCode() ^ getReference().hashCode()
               ^ Arrays.asList(getCoordinateSystemUnits()).hashCode();
    }

    /**
     * Tests this class.
     *
     * @param args          Execution arguments.  Ignored.
     * @throws Exception    if something went wrong.
     */
    public static void main(String[] args) throws Exception {

        SkewTCoordinateSystem cs     = SkewTCoordinateSystem.instance();
        double[][]            coords = new double[][] {
            { 0, cs.minX, cs.maxX, cs.maxX, cs.minX },
            { 0, cs.minY, cs.minY, cs.maxY, cs.maxY }, { 0, 0, 0, 0, 0 }
        };
        int npts = coords[0].length;

        System.out.println("(X,Y) Coordinates: ");

        for (int i = 0; i < npts; ++i) {
            System.out.println("    (" + coords[0][i] + "," + coords[1][i]
                               + ")");
        }

        cs.fromReference(coords);
        System.out.println("(P,T) Coordinates: ");

        for (int i = 0; i < npts; ++i) {
            System.out.println("    (" + coords[0][i] + "," + coords[1][i]
                               + ")");
        }

        cs.toReference(coords);
        System.out.println("(X,Y) Coordinates: ");

        for (int i = 0; i < npts; ++i) {
            System.out.println("    (" + coords[0][i] + "," + coords[1][i]
                               + ")");
        }
    }

    /**
     * Create the units of the coordinate system transformation.
     *
     * @param rank
     * @param pressureUnit
     * @param temperatureUnit
     * @return                  The units of the coordinate system
     *                          transformation.
     */
    private static Unit[] makeCSUnits(int rank, Unit pressureUnit,
                                      Unit temperatureUnit) {

        Unit[] csUnits = new Unit[rank];

        csUnits[0] = pressureUnit;
        csUnits[1] = temperatureUnit;

        Arrays.fill(csUnits, 2, rank, null);

        return csUnits;
    }

    /**
     * Create a 3D display coordinate system from the (possibly) 2D input.
     *
     * @param acs   SkewTCoordinateSystem to get values from.
     *
     * @return 3D version of (possibly) 2D system.
     *
     * @throws VisADException problem creating new CoordinateSystem
     */
    public AerologicalCoordinateSystem createDisplayCoordinateSystem(AerologicalCoordinateSystem acs)
            throws VisADException {
        if ( !(acs instanceof SkewTCoordinateSystem)) {
            throw new IllegalArgumentException(
                "coordinate system must be of same type");
        }
        if (acs.getDimension() == 3) {
            return acs;
        }
        SkewTCoordinateSystem scs   = (SkewTCoordinateSystem) acs;
        SkewTCoordinateSystem newCS = null;
        return new SkewTCoordinateSystem(Display.DisplaySpatialCartesianTuple,
                                         scs.minimumPressure,
                                         scs.maximumPressure,
                                         scs.minimumTemperature,
                                         scs.maximumTemperature,
                                         scs.isothermTangent,
                                         scs.temperaturePerLogPressure,
                                         scs.getCoordinateSystemUnits()[0],
                                         scs.getCoordinateSystemUnits()[1],
                                         scs.minimumX, scs.maximumX,
                                         scs.minimumY, scs.maximumY);
    }
}
