/*
 * $Id: AerologicalCellNetwork.java,v 1.12 2005/05/13 18:33:21 jeffmc Exp $
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

import ucar.visad.functiontypes.AtmosphericProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.AirTemperature;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.MassicVolume;
import ucar.visad.quantities.PolarHorizontalWind;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.quantities.WaterVaporMixingRatio;

import visad.CommonUnit;

import visad.DataReference;

import visad.DataReferenceImpl;

import visad.Field;

import visad.Real;

import visad.RealTuple;

import visad.RealType;

import visad.Set;

import visad.TypeException;

import visad.VisADException;


/**
 * A network that computes derived, output, aerological parameters
 * from input, aerological parameters.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.12 $ $Date: 2005/05/13 18:33:21 $
 */
public final class AerologicalCellNetwork {

    /** dewpoint extractor cell */
    private final DewPointExtractorCell dewExtractorCell;

    /** profile temperature at pressure */
    private final RealEvaluatorCell proTempAtPresCell;

    /** profile temperature at pressure */
    private final RealEvaluatorCell proWindAtPresCell;

    /** profile mixing ratio at pressure */
    private final MixingRatioCell proMixingRatioCell;

    /** LCL temperature */
    private final LclTemperatureCell lclTempCell;

    /** LCL pressure */
    private final LclPressureCell lclPresCell;

    /** dry adiabatic trajectory */
    private final DryTrajectoryCell dryTrajectoryCell;

    /** moist adiabatic trajectory */
    private final WetTrajectoryCell wetTrajectoryCell;

    /** virtual temperature (dry) */
    private final VirtualTemperatureProfileCell dryVirtTempProCell;

    /** virtual temperature (wed) */
    private final VirtualTemperatureProfileCell wetVirtTempProCell;

    /** parcel virtual temperature profile */
    private final ProfileCombinerCell parVirtTempProCell;

    /** environmental virtual temperature profile */
    private final VirtualTemperatureProfileCell envVirtTempProCell;

    /** parcel density profile */
    private final AirDensityProfileCell parDenProCell;

    /** environmental density profile */
    private final AirDensityProfileCell envDenProCell;

    /** bouyancy profile */
    private final BuoyancyProfileCell buoyProfileCell;

    /** clean bouyancy */
    private final ProfileCleanerCell cleanBuoyCell;

    /** CAPE */
    private final CapeCell capeCell;

    /** Level of Free Convention (LFC) */
    private final LfcCell lfcCell;

    /** Level of Free Convention (LFC)  temperature */
    private final RealEvaluatorCell lfcTempCell;

    /** Level of negative bouyancy */
    private final LnbCell lnbCell;

    /** Level of negative bouyancy temperature */
    private final RealEvaluatorCell lnbTempCell;

    /** Convective instability */
    private final CinCell cinCell;

    /** parcel index reference */
    private final DataReference parIndexRef;

    /** zero */
    private static Real zero;

    /** one */
    private static Real one;

    /** two */
    private static Real two;

    /** three */
    private static Real three;

    static {
        try {
            zero = new Real(
                RealType.getRealType(
                    "dimensionless_constant", CommonUnit.dimensionless), 0);
            one = new Real(
                RealType.getRealType(
                    "dimensionless_constant", CommonUnit.dimensionless), 1);
            two = new Real(
                RealType.getRealType(
                    "dimensionless_constant", CommonUnit.dimensionless), 2);
            three = new Real(
                RealType.getRealType(
                    "dimensionless_constant", CommonUnit.dimensionless), 3);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Constructs from input data references.
     *
     * @param tempProfileRef       The temperature profile reference.
     * @param dewProfileRef        The dew-point profile reference.
     * @param presRef              The pressure reference.
     * @param tempRef              The temperature reference.
     * @param minPresRef           The minimum pressure reference.
     *
     * @throws NullPointerException if an argument is <code>null</code>.
     * @throws VisADException      if a VisAD failure occurs.
     * @throws RemoteException     if a Java RMI failure occurs.
     */
    public AerologicalCellNetwork(DataReference tempProfileRef, DataReference dewProfileRef, DataReference presRef, DataReference tempRef, DataReference minPresRef)
            throws VisADException, RemoteException {
        this(tempProfileRef, dewProfileRef, presRef, tempRef, minPresRef,
             new DataReferenceImpl("WindProfile"));
    }

    /**
     * Constructs from input data references.
     *
     * @param tempProfileRef       The temperature profile reference.
     * @param dewProfileRef        The dew-point profile reference.
     * @param presRef              The pressure reference.
     * @param tempRef              The temperature reference.
     * @param minPresRef           The minimum pressure reference.
     * @param windProfileRef       The wind profile reference
     *
     * @throws NullPointerException if an argument is <code>null</code>.
     * @throws VisADException      if a VisAD failure occurs.
     * @throws RemoteException     if a Java RMI failure occurs.
     */
    public AerologicalCellNetwork(DataReference tempProfileRef, DataReference dewProfileRef, DataReference presRef, DataReference tempRef, DataReference minPresRef, DataReference windProfileRef)
            throws VisADException, RemoteException {

        CellNetwork net    = new CellNetwork();
        Real        noPres = (Real) AirPressure.getRealType().missingData();
        Real noTemp = (Real) AirTemperature.getRealType().missingData();
        RealTuple noWind =
            (RealTuple) PolarHorizontalWind.getRealTupleType().missingData();
        Real noRatio =
            (Real) WaterVaporMixingRatio.getRealType().missingData();
        Field noRatioPro =
            (Field) new AtmosphericProfile(noRatio.getType()).missingData();
        Field noTempPro =
            (Field) new AtmosphericProfile(noTemp.getType()).missingData();
        ComputeCell maxProPresCell = new MaxPressureCell(tempProfileRef,
                                         noPres);
        Set noPresDomain = noTempPro.getDomainSet();

        net.add(maxProPresCell);

        ComputeCell potTempProCell =
            new PotentialTemperatureProfileCell(tempProfileRef, noTempPro);

        net.add(potTempProCell);

        LayerMeanCell meanPotTempCell =
            new LayerMeanCell(potTempProCell.getOutputRef(),
                              maxProPresCell.getOutputRef(), presRef, noTemp);

        net.add(meanPotTempCell);

        ComputeCell meanTempCell =
            new AirTemperatureCell(meanPotTempCell.getMeanPresRef(),
                                   meanPotTempCell.getOutputRef(), noTemp);

        net.add(meanTempCell);

        ComputeCell mixingRatioProCell =
            new MixingRatioProfileCell(dewProfileRef, noRatioPro);

        net.add(mixingRatioProCell);

        ComputeCell meanMixingRatioCell =
            new LayerMeanCell(mixingRatioProCell.getOutputRef(),
                              maxProPresCell.getOutputRef(), presRef,
                              noRatio);

        net.add(meanMixingRatioCell);

        parIndexRef = new DataReferenceImpl("ParcelIndexRef");

        setParcelMode(ParcelMode.BOTTOM);
        net.add(dewExtractorCell = new DewPointExtractorCell(dewProfileRef,
                presRef));
        net.add(proMixingRatioCell =
            new MixingRatioCell(dewExtractorCell.getOutputRef(), presRef));

        ComputeCell parPresCell = new SelectorCell(parIndexRef,
                                                   new DataReference[]{
                                                       presRef,
                                                       meanPotTempCell
                                                           .getMeanPresRef(),
                                                       maxProPresCell
                                                           .getOutputRef(),
                                                       presRef }, noPres);

        net.add(parPresCell);

        proTempAtPresCell = new RealEvaluatorCell(tempProfileRef, presRef,
                                                  noTemp);

        net.add(proTempAtPresCell);

        proWindAtPresCell = new RealEvaluatorCell(windProfileRef, presRef,
                                                  noWind);

        net.add(proWindAtPresCell);

        ComputeCell proTempAtMaxPresCell =
            new RealEvaluatorCell(tempProfileRef,
                                  maxProPresCell.getOutputRef(), noTemp);

        net.add(proTempAtMaxPresCell);

        ComputeCell parTempCell = new SelectorCell(parIndexRef,
                                                   new DataReference[]{
                                                       tempRef,
                                                       meanTempCell
                                                           .getOutputRef(),
                                                       proTempAtMaxPresCell
                                                           .getOutputRef(),
                                                       proTempAtPresCell
                                                           .getOutputRef() }, noTemp);

        net.add(parTempCell);

        ComputeCell proMixingRatioAtMaxPresCell =
            new RealEvaluatorCell(mixingRatioProCell.getOutputRef(),
                                  maxProPresCell.getOutputRef(), noRatio);

        net.add(proMixingRatioAtMaxPresCell);

        ComputeCell parMixingRatioCell =
            new SelectorCell(parIndexRef,
                             new DataReference[]{
                                 proMixingRatioCell.getOutputRef(),
                                 meanMixingRatioCell.getOutputRef(),
                                 proMixingRatioAtMaxPresCell.getOutputRef(),
                                 proMixingRatioCell
                                     .getOutputRef() }, noRatio);

        net.add(parMixingRatioCell);
        net.add(lclTempCell =
            new LclTemperatureCell(parMixingRatioCell.getOutputRef(),
                                   parPresCell.getOutputRef(),
                                   parTempCell.getOutputRef()));
        net.add(lclPresCell =
            new LclPressureCell(parPresCell.getOutputRef(),
                                parTempCell.getOutputRef(),
                                lclTempCell.getOutputRef()));
        net.add(dryTrajectoryCell =
            new DryTrajectoryCell(parPresCell.getOutputRef(),
                                  parTempCell.getOutputRef(),
                                  lclPresCell.getOutputRef()));
        net.add(wetTrajectoryCell =
            new WetTrajectoryCell(lclTempCell.getOutputRef(),
                                  lclPresCell.getOutputRef(), minPresRef));
        net.add(
            dryVirtTempProCell = new VirtualTemperatureProfileCell(
                dryTrajectoryCell.getOutputRef(),
                parMixingRatioCell.getOutputRef()));
        net.add(
            wetVirtTempProCell = new VirtualTemperatureProfileCell(
                wetTrajectoryCell.getOutputRef(),
                wetTrajectoryCell.getOutputRef()));
        net.add(parVirtTempProCell =
            new ProfileCombinerCell(dryVirtTempProCell.getOutputRef(),
                                    wetVirtTempProCell.getOutputRef(),
                                    VirtualTemperature.getRealType()));
        net.add(parDenProCell =
            new AirDensityProfileCell(parVirtTempProCell.getOutputRef()));
        net.add(envVirtTempProCell =
            new VirtualTemperatureProfileCell(tempProfileRef, dewProfileRef));
        ComputeCell parDomainCell =
            new DomainExtractorCell(parVirtTempProCell.getOutputRef(),
                                    noPresDomain);
        net.add(parDomainCell);
        ComputeCell envVirtTempProParDomainCell =
            new DomainEvaluatorCell(envVirtTempProCell.getOutputRef(),
                                    parDomainCell.getOutputRef(), noTempPro);
        net.add(envVirtTempProParDomainCell);
        net.add(envDenProCell = new AirDensityProfileCell(
        // envVirtTempProCell.getOutputRef()));
        envVirtTempProParDomainCell.getOutputRef()));
        net.add(buoyProfileCell =
            new BuoyancyProfileCell(envDenProCell.getOutputRef(),
                                    parDenProCell.getOutputRef()));
        net.add(cleanBuoyCell =
            new ProfileCleanerCell(buoyProfileCell.getOutputRef(),
                                   MassicVolume.getRealType()));
        net.add(lfcCell = new LfcCell(cleanBuoyCell.getOutputRef()));
        net.add(lfcTempCell = new RealEvaluatorCell(tempProfileRef,
                                                    lfcCell.getOutputRef(),
                                                    noTemp));
        net.add(lnbCell = new LnbCell(cleanBuoyCell.getOutputRef()));
        net.add(lnbTempCell = new RealEvaluatorCell(tempProfileRef,
                                                    lnbCell.getOutputRef(),
                                                    noTemp));

        ComputeCell energyProCell =
            new EnergyProfileCell(cleanBuoyCell.getOutputRef());

        net.add(energyProCell);
        net.add(capeCell = new CapeCell(energyProCell.getOutputRef(),
                                        lfcCell.getOutputRef(),
                                        lnbCell.getOutputRef()));
        net.add(cinCell = new CinCell(energyProCell.getOutputRef(),
                                      parPresCell.getOutputRef(),
                                      lfcCell.getOutputRef()));
        net.configure();
    }

    /**
     * Sets the mode for determining the parameters (pressure, temperature,
     * water-vapor mixing-ratio) of the lifting parcel.
     *
     * @param mode                  The mode.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setParcelMode(ParcelMode mode)
            throws VisADException, RemoteException {

        if (mode == ParcelMode.POINT) {
            parIndexRef.setData(zero);
        } else if (mode == ParcelMode.LAYER) {
            parIndexRef.setData(one);
        } else if (mode == ParcelMode.BOTTOM) {
            parIndexRef.setData(two);
        } else if (mode == ParcelMode.PRESSURE) {
            parIndexRef.setData(three);
        } else {
            throw new Error("Assertion failure");
        }
    }

    /**
     * Returns the {@link visad.DataReference} for the temperature of the
     * temperature-profile at the reference pressure.
     *
     * @return                      The {@link visad.DataReference}
     */
    public DataReference getProfileTemperatureRef() {
        return proTempAtPresCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the profile dew point.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getProfileDewPointRef() {
        return dewExtractorCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the profile's mixing-ratio.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getProfileMixingRatioRef() {
        return proMixingRatioCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the profile's wind.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getProfileWindRef() {
        return proWindAtPresCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the LCL temperature.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getLclTemperatureRef() {
        return lclTempCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the LCL pressure.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getLclPressureRef() {
        return lclPresCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the lifted-parcel's dry-adiabatic
     * trajectory.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getDryTrajectoryRef() {
        return dryTrajectoryCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the lifted-parcel's wet-adiabatic
     * trajectory.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getWetTrajectoryRef() {
        return wetTrajectoryCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the lifted-parcel's dry-adiabatic
     * virtual temperature profile.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getDryVirtualTemperatureProfileRef() {
        return dryVirtTempProCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the lifted-parcel's wet-adiabatic
     * virtual temperature profile.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getWetVirtualTemperatureProfileRef() {
        return wetVirtTempProCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the lifted-parcel's pseudo-adiabatic
     * virtual-temperature profile.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getParcelVirtualTemperatureProfileRef() {
        return parVirtTempProCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the environmental virtual-temperature
     * profile.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getEnvironmentVirtualTemperatureProfileRef() {
        return envVirtTempProCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the buoyancy profile.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getBuoyancyProfileRef() {
        return buoyProfileCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the CAPE.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getCapeRef() {
        return capeCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the LFC.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getLfcRef() {
        return lfcCell.getOutputRef();
    }

    /**
     * Returns the {@link visad.DataReference} for the temperature at the LFC.
     *
     * @return                      The LFC temperature reference.
     */
    public DataReference getLfcTemperatureRef() {
        return lfcTempCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the Level of Neutral Buoyancy (LNB).
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getLnbRef() {
        return lnbCell.getOutputRef();
    }

    /**
     * Returns teh {@link visad.DataReference} for the temperature at the LNB.
     *
     * @return                      The LNB temperature reference.
     */
    public DataReference getLnbTemperatureRef() {
        return lnbTempCell.getOutputRef();
    }

    /**
     * Returns the {@link DataCell} for the CIN.
     *
     * @return                      The {@link DataCell}
     */
    public DataReference getCinRef() {
        return cinCell.getOutputRef();
    }
}







