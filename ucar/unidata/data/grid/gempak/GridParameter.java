/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.grid.gempak;


/**
 * Class which represents a grid parameter.
 * A parameter consists of a discipline( ie Meteorological_products),
 * a Category( ie Temperature ) and a number that refers to a name( ie Temperature),
 * Description( ie Temperature at 2 meters), and Units( ie K ).
 * see <a href="../../Parameters.txt">Parameters.txt</a>
 */

public final class GridParameter {

    /**
     * parameter number.
     */
    private int number;

    /**
     * name of parameter.
     */
    private String name;

    /**
     * description of parameter.
     */
    private String description;

    /**
     * unit of Parameter.
     */
    private String unit;

    /**
     * constructor.
     */
    public GridParameter() {
        number      = -1;
        name        = "undefined";
        description = "undefined";
        unit        = "undefined";
    }

    /**
     * constructor.
     * @param number
     * @param name
     * @param description
     * @param unit of parameter
     */
    public GridParameter(int number, String name, String description,
                         String unit) {
        this.number      = number;
        this.name        = name;
        this.description = description;
        this.unit        = unit;
    }

    /**
     * number of parameter.
     * @return number
     */
    public final int getNumber() {
        return number;
    }

    /**
     * name of parameter.
     * @return name
     */
    public final String getName() {
        return name;
    }

    /**
     * description of parameter.
     *
     * @return description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * unit of parameter.
     * @return unit
     */
    public final String getUnit() {
        return unit;
    }

    /**
     * sets number of parameter.
     * @param number of parameter
     */
    public final void setNumber(int number) {
        this.number = number;
    }

    /**
     * sets name of parameter.
     * @param name  of parameter
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * sets description of parameter.
     * @param description of parameter
     */
    public final void setDescription(String description) {
        this.description = description;
    }

    /**
     * sets unit of parameter.
     * @param unit of parameter
     */
    public final void setUnit(String unit) {
        this.unit = unit;
    }
}

