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

import java.lang.Class;

public interface MultiDimensionReader {

  public float[] getFloatArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public double[] getDoubleArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public int[] getIntArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public short[] getShortArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public byte[] getByteArray(String name, int[] start, int[] count, int[] stride) throws Exception;

  public Object getArray(String name, int[] start, int[] count, int[] stride) throws Exception;

  public Class getArrayType(String name);

  public String[] getDimensionNames(String arrayName);

  public int[] getDimensionLengths(String arrayName);

  public HDFArray getGlobalAttribute(String attrName) throws Exception;

  public HDFArray getArrayAttribute(String arrayName, String attrName) throws Exception;

  public void close() throws Exception;
}
