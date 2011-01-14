/*
 * $Id: TrackInfo.java,v 1.4 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.data;


import ucar.ma2.Range;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataUtil;

import ucar.unidata.data.point.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.quantities.*;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.*;

import visad.util.DataUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class Variable Holds info about track variables
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class VarInfo {

    /** name */
    private String name;

    /** desc */
    private String description;

    /** The category */
    private String category;

    /** unit */
    private Unit unit;

    /** is this numeric */
    private boolean isNumeric = true;

    /** The real type to use */
    private RealType realType;

    /** the missing value */
    private double missingValue;


    /**
     * ctor
     *
     * @param name name
     * @param desc desc
     * @param unit unit
     */
    public VarInfo(String name, String desc, Unit unit) {
        this(name, desc, null, unit);
    }


    /**
     * ctor
     *
     * @param name name
     * @param desc desc
     * @param category the category of the var
     * @param unit unit
     */
    public VarInfo(String name, String desc, String category, Unit unit) {
        this(name, desc, category, unit, Double.NaN);
    }


    /**
     * ctor
     *
     * @param name name
     * @param desc desc
     * @param category the category of the var
     * @param unit unit
     * @param missingValue missing value
     */

    public VarInfo(String name, String desc, String category, Unit unit,
                   double missingValue) {
        this.name        = name;
        this.description = desc;
        this.category    = category;
        if ((this.description == null)
                || (this.description.trim().length() == 0)) {
            this.description = name;
        }
        this.unit         = unit;
        this.missingValue = missingValue;
        realType          = DataUtil.makeRealType(getShortName(), unit);
        if (realType == null) {
            System.out.println("can't create realtype for " + getShortName()
                               + " with unit " + unit);
        }
    }


    /**
     * ctor
     *
     * @param name name
     * @param unit unit
     */
    public VarInfo(String name, Unit unit) {
        this(name, name, unit);
    }

    /**
     * ctor
     *
     * @param name name
     * @param units unit string
     */
    public VarInfo(String name, String units) {
        this(name, DataUtil.parseUnit(units));
    }

    /**
     * ctor
     *
     * @param name name
     * @param desc description
     * @param units unit string
     */
    public VarInfo(String name, String desc, String units) {
        this(name, desc, DataUtil.parseUnit(units));
    }

    /**
     * Utility to find the variable with the given name
     *
     * @param variableName The name
     * @param variables List of variables to look into
     *
     * @return The variable.
     */

    public static VarInfo getVarInfo(String variableName,
                                     List<VarInfo> variables) {
        //Jump through some hoops for legacy bundles
        String[] vars = { variableName, variableName.toLowerCase() };
        for (int dummyIdx = 0; dummyIdx < vars.length; dummyIdx++) {
            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                VarInfo theVar = (VarInfo) variables.get(varIdx);
                if (vars[dummyIdx].equals(theVar.getName())) {
                    return theVar;
                }
            }

            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                VarInfo theVar = (VarInfo) variables.get(varIdx);
                if (vars[dummyIdx].equals(theVar.getDescription())) {
                    return theVar;
                }
            }
        }

        throw new IllegalArgumentException("Unknown variable: "
                                           + variableName);
    }



    /**
     * get the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }


    /**
     * get the name
     *
     * @return name
     */
    public String getShortName() {
        return name;
    }

    /**
     * get desc
     *
     * @return desc
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Category property.
     *
     * @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     * Get the Category property.
     *
     * @return The Category
     */
    public String getCategory() {
        return category;
    }



    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return name;
    }


    /**
     * Set the IsNumeric property.
     *
     * @param value The new value for IsNumeric
     */
    public void setIsNumeric(boolean value) {
        isNumeric = value;
    }

    /**
     * Get the IsNumeric property.
     *
     * @return The IsNumeric
     */
    public boolean getIsNumeric() {
        return isNumeric;
    }


    /**
     * Set the RealType property.
     *
     * @param value The new value for RealType
     */
    public void setRealType(RealType value) {
        realType = value;
    }

    /**
     * Get the RealType property.
     *
     * @return The RealType
     */
    public RealType getRealType() {
        return realType;
    }


    /**
     * Set the Unit property.
     *
     * @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        unit = value;
    }

    /**
     * Get the Unit property.
     *
     * @return The Unit
     */
    public Unit getUnit() {
        return unit;
    }


    /**
     * Set the MissingValue property.
     *
     * @param value The new value for MissingValue
     */
    public void setMissingValue(double value) {
        missingValue = value;
    }

    /**
     * Get the MissingValue property.
     *
     * @return The MissingValue
     */
    public double getMissingValue() {
        return missingValue;
    }


}

