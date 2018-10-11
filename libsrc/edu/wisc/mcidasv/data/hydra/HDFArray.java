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

public class HDFArray {

   Class type;
   int length;

   public HDFArray(Class type) {
    this.type = type;
   }

   public Object getArray() {
     return null;
   }

   public Class getType() {
     return type;
   }

   public static HDFArray make(float[] array) {
     return new HDFFloatArray(array);
   }

   public static HDFArray make(double[] array) {
     return new HDFDoubleArray(array);
   }

   public static HDFArray make(int[] array) {
     return new HDFIntArray(array);
   }

   public static HDFArray make(short[] array) {
     return new HDFShortArray(array);
   }

   public static HDFArray make(String[] array) {
     return new HDFStringArray(array);
   }
}

class HDFFloatArray extends HDFArray {
  float[] float_array;
  public HDFFloatArray(float[] fa) {
    super(Float.TYPE);
    float_array = fa;
    length = fa.length;
  }
  public float[] getArray() {
    return float_array;
  }
}

class HDFDoubleArray extends HDFArray {
  double[] double_array;
  public HDFDoubleArray(double[] da) {
    super(Double.TYPE);
    double_array = da;
    length = da.length;
  }
  public double[] getArray() {
    return double_array;
  }
}

class HDFShortArray extends HDFArray {
  short[] short_array;
  public HDFShortArray(short[] sa) {
    super(Short.TYPE);
    short_array = sa;
    length = sa.length;
  }
  public short[] getArray() {
    return short_array;
  }
}

class HDFIntArray extends HDFArray {
  int[] int_array;
  public HDFIntArray(int[] ia) {
    super(Integer.TYPE);
    int_array = ia;
    length = ia.length;
  }
  public int[] getArray() {
    return int_array;
  }
}

class HDFByteArray extends HDFArray {
  byte[] byte_array;
  public HDFByteArray(byte[] ba) {
    super(Byte.TYPE);
    byte_array = ba;
    length = ba.length;
  }
  public byte[] getArray() {
    return byte_array;
  }
}


class HDFStringArray extends HDFArray {
  String[] string_array;
  public HDFStringArray(String[] sa) {
    super(String.class);
    string_array = sa;
    length = sa.length;
  }
  public String[] getArray() {
    return string_array;
  }
}
