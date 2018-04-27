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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregationRangeProcessor extends RangeProcessor {

	List<RangeProcessor> rangeProcessors = new ArrayList<>();

	int rngIdx = 0;

	public AggregationRangeProcessor(GranuleAggregation aggrReader,
			Map<String, Object> metadata) throws Exception {
		super();

		List<NetCDFFile> readers = aggrReader.getReaders();

		int num = 0;

		for (int rdrIdx = 0; rdrIdx < readers.size(); rdrIdx++) {
			RangeProcessor rngProcessor = RangeProcessor.createRangeProcessor(
					readers.get(rdrIdx), metadata);

			if (rngProcessor.hasMultiDimensionScale()) {
				num++;
			}

			rangeProcessors.add(rngProcessor);
		}

		if (num > 0 && num != readers.size()) {
			throw new Exception(
					"AggregationRangeProcessor: all or none can define a multiDimensionScale");
		} else if (num == readers.size()) {
			setHasMultiDimensionScale(true);
		}

		aggrReader.addRangeProcessor(
				(String) metadata.get(SwathAdapter.array_name), this);
	}

	public synchronized void setWhichRangeProcessor(int index) {
		rngIdx = index;
	}

	public synchronized void setMultiScaleIndex(int idx) {
		rangeProcessors.get(rngIdx).setMultiScaleIndex(idx);
	}

	public synchronized float[] processRange(byte[] values, Map<String, double[]> subset) {
		return rangeProcessors.get(rngIdx).processRange(values, subset);
	}

	public synchronized float[] processRange(short[] values, Map<String, double[]> subset) {
		return rangeProcessors.get(rngIdx).processRange(values, subset);
	}

	public synchronized float[] processRange(float[] values, Map<String, double[]> subset) {
		return rangeProcessors.get(rngIdx).processRange(values, subset);
	}

	public synchronized double[] processRange(double[] values, Map<String, double[]> subset) {
		return rangeProcessors.get(rngIdx).processRange(values, subset);
	}
}
