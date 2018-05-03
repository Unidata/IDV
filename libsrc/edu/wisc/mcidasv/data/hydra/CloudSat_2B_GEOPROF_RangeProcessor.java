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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Map;

public class CloudSat_2B_GEOPROF_RangeProcessor extends RangeProcessor {

	public CloudSat_2B_GEOPROF_RangeProcessor(MultiDimensionReader reader,
			Map<String, Object> metadata) throws Exception {
		super(reader, metadata);
		if (scale == null) { // use implicit default value since E05, E06 has
								// removed the scale/offset from the Radar Refl
								// variable
			scale = new float[] { 100f };
			offset = new float[] { 0f };
		}
	}

	public float[] processRange(short[] values, Map<String, double[]> subset) {
		float[] new_values = new float[values.length];
		for (int k = 0; k < values.length; k++) {
			float val = (float) values[k];
			if (val == missing[0]) {
				new_values[k] = Float.NaN;
			} else if ((val < valid_low) || (val > valid_high)) {
				new_values[k] = -40f;
			} else {
				new_values[k] = val / scale[0] + offset[0];
			}
		}
		return new_values;
	}

}
