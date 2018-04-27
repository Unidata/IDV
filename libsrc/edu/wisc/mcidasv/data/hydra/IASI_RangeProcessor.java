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

public class IASI_RangeProcessor extends RangeProcessor {

	public IASI_RangeProcessor() throws Exception {
		super();
	}

	public float[] processRange(short[] values, Map<String, double[]> subset) {
		int channelIndex = (int) (subset.get(SpectrumAdapter.channelIndex_name))[0];

		float[] new_values = IASI_L1C_Utility.getDecodedIASIImage(values, null, channelIndex);
		double[] track_coords = subset.get(SwathAdapter.track_name);
		int numLines = ((int) (track_coords[1] - track_coords[0]) + 1);
		new_values = IASI_L1C_Utility.psuedoScanReorder2(new_values, 60, numLines * 2);

		// - subset here, if necessary
		return new_values;
	}

}
