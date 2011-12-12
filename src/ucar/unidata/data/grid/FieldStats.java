/*
 * $Id: GridUtil.java,v 1.112 2007/08/09 22:06:44 dmurray Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/oar modify it
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



package ucar.unidata.data.grid;


/**
 * @version $Revision: 1.112 $
 */

public class FieldStats {
    
    private float min;
    private float max;
    private float average;
    private int cnt;

    public FieldStats() {
    }


    public FieldStats(float[]mmac) {
	min = mmac[0];
	max = mmac[1];
	average = mmac[2];
	cnt = (int) mmac[3];
    }

    public String toString() {
	return "min:" + min +" max:" + max +" average:" + average +" count:" + cnt;
    }


    /**
       Set the Min property.

       @param value The new value for Min
    **/
    public void setMin (float value) {
	this.min = value;
    }

    /**
       Get the Min property.

       @return The Min
    **/
    public float getMin () {
	return this.min;
    }

    /**
       Set the Max property.

       @param value The new value for Max
    **/
    public void setMax (float value) {
	this.max = value;
    }

    /**
       Get the Max property.

       @return The Max
    **/
    public float getMax () {
	return this.max;
    }

    /**
       Set the Average property.

       @param value The new value for Average
    **/
    public void setAverage (float value) {
	this.average = value;
    }

    /**
       Get the Average property.

       @return The Average
    **/
    public float  getAverage () {
	return this.average;
    }

    /**
       Set the Cnt property.

       @param value The new value for Cnt
    **/
    public void setCnt (int value) {
	this.cnt = value;
    }

    /**
       Get the Cnt property.

       @return The Cnt
    **/
    public int getCnt () {
	return this.cnt;
    }



}

