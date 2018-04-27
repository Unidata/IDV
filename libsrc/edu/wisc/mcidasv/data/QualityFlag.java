/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data;

import java.util.HashMap;

/**
 * @author tommyj
 * 
 * Holds info to extract a Suomi NPP Quality Flag from a packed byte.
 * Info is read from the appropriate XML Product Profile
 *
 */

public class QualityFlag {

	private int bitOffset = -1;
	private int numBits = -1;
	private String name = null;
	private String packedName = null;
	private HashMap<String, String> hm = null;
	
	/**
	 * @param bitOffset
	 * @param numBits
	 * @param name
	 */
	
	public QualityFlag(int bitOffset, int numBits, String name, HashMap<String, String> hm) {
		this.bitOffset = bitOffset;
		this.numBits = numBits;
		this.name = name;
		this.hm = hm;
	}

	/**
	 * @return the bitOffset
	 */
	public int getBitOffset() {
		return bitOffset;
	}

	/**
	 * @param bitOffset the bitOffset to set
	 */
	public void setBitOffset(int bitOffset) {
		this.bitOffset = bitOffset;
	}

	/**
	 * @return the numBits
	 */
	public int getNumBits() {
		return numBits;
	}

	/**
	 * @param numBits the numBits to set
	 */
	public void setNumBits(int numBits) {
		this.numBits = numBits;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the packedName
	 */
	public String getPackedName() {
		return packedName;
	}

	/**
	 * @param packedName the packedName to set
	 */
	public void setPackedName(String packedName) {
		this.packedName = packedName;
	}

	/**
	 * @return the hm
	 */
	public HashMap<String, String> getHm() {
		return hm;
	}

	/**
	 * @param hm the hm to set
	 */
	public void setHm(HashMap<String, String> hm) {
		this.hm = hm;
	}
	
	/**
	 * @return the name for a discreet value for this flag
	 */
	public String getNameForValue(String valueStr) {
		if (hm != null) {
			if (hm.containsKey(valueStr)) {
				return hm.get(valueStr);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QualityFlag [bitOffset=" + bitOffset + ", numBits=" + numBits
				+ ", name=" + name + ", packedName=" + packedName + "]";
	}
	
}
