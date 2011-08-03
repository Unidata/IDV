/*
 * $Id: DbaseData.java,v 1.11 2005/05/13 18:29:47 jeffmc Exp $
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

/**
 *  Class to contain a single field of data from a dbase file
 * @author Russ Rew
 * @version $Id: DbaseData.java,v 1.11 2005/05/13 18:29:47 jeffmc Exp $
 */
package ucar.unidata.gis.shapefile;



import java.io.DataInputStream;

import ucar.unidata.io.Swap;

import java.text.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;


/**
 * Class DbaseData
 *
 *
 * @author Russ Rew
 */
public class DbaseData {

    /** _more_ */
    DbaseFieldDesc desc;

    /** _more_ */
    int nrec;  /* number of records */

    /**
     *  Character type data (String[]).
     */
    public final static int TYPE_CHAR = 0;

    /**
     *  Data is an array of doubles (double[]).
     */
    public final static int TYPE_NUMERIC = 1;

    /**
     *  Data is an array of booleans (boolean[]).
     */
    public final static int TYPE_BOOLEAN = 2;

    /* the various possible types */

    /** _more_ */
    String[] character;

    /** _more_ */
    double[] numeric;

    /** _more_ */
    boolean[] logical;

    /** _more_ */
    byte[] field;

    /** _more_ */
    int type;


    /**
     * _more_
     *
     * @param desc
     * @param nrec
     *
     */
    DbaseData(DbaseFieldDesc desc, int nrec) {
        this.desc = desc;
        this.nrec = nrec;
        field     = new byte[desc.FieldLength];
        char switchValue = (char) desc.Type;
        switch (switchValue) {

          case 'C' :
          case 'D' :
              character = new String[nrec];
              type      = TYPE_CHAR;
              break;

          case 'N' :
          case 'F' :
              numeric = new double[nrec];
              type    = TYPE_NUMERIC;
              break;

          case 'L' :
              logical = new boolean[nrec];
              type    = TYPE_BOOLEAN;
              break;
        }


    }

    /**
     *  Method to return the type of data for the field
     *  @return One of TYPE_CHAR, TYPE_BOOLEAN, or TYPE_NUMERIC
     */
    public int getType() {
        return type;
    }

    /** _more_          */
    static int cnt = 0;

    /**
     *  Method to read an entry from the data stream.  The stream is assumed to be
     *  in the right spot for reading.  This method should be called from something
     *  controlling the reading of the entire file.
     *  @see DbaseFile
     *
     * @param ds
     * @param n
     * @return _more_
     */
    int readRowN(DataInputStream ds, int n) {
        if (n > nrec) {
            return -1;
        }
        /* the assumption here is that the DataInputStream (ds)
         * is already pointing at the right spot!
         */
        try {
            ds.readFully(field, 0, desc.FieldLength);
        } catch (java.io.IOException e) {
            return -1;
        }
        char switchValue = (char) desc.Type;
        switch (switchValue) {

          case 'C' :
          case 'D' :
              character[n] = new String(field);
              break;

          case 'N' :
              String sv = new String(field);
              numeric[n] = 0.0;
              if (sv.trim().length() != 0 && !sv.startsWith("*")) {
                  try {
                      numeric[n] = Double.valueOf(sv).doubleValue();
                  } catch (NumberFormatException nfe) {
                      System.err.println("Bad number format:" + sv);
                  }
              }

              break;

          case 'F' :  /* binary floating point */
              if (desc.FieldLength == 4) {
                  numeric[n] = (double) Swap.swapFloat(field, 0);
              } else {
                  numeric[n] = Swap.swapDouble(field, 0);
              }
              break;

          case 'L' :
              switch ((char) field[0]) {

                case 't' :
                case 'T' :
                case 'Y' :
                case 'y' :
                    logical[n] = true;
                    break;

                default :
                    logical[n] = false;
                    break;
              }
          default :
              return -1;
        }

        return 0;
    }

    /**
     *  Method to retrieve the double array for this field
     *  @return An array of doubles with the data
     */
    public double[] getDoubles() {
        return numeric;
    }

    /**
     *  Method to retrieve a double for this field
     *  @param i index of desired double, assumes 0 < i < getNumRec()
     *  @return A double with the data
     */
    public double getDouble(int i) {
        return numeric[i];
    }

    /**
     *  Method to retrieve a booleans array for this field
     *  @return An array of boolean values
     */
    public boolean[] getBooleans() {
        return logical;
    }

    /**
     *  Method to retrieve a boolean for this field
     *  @param i index of desired boolean, assumes 0 < i < getNumRec()
     *  @return A boolean with the data
     */
    public boolean getBoolean(int i) {
        return logical[i];
    }

    /**
     *  Method to retrieve an array of Strings for this field
     *  @return An array of Strings
     */
    public String[] getStrings() {
        return character;
    }

    /**
     *  Method to retrieve a String for this field
     *  @param i index of desired String, assumes 0 < i < getNumRec()
     *  @return A String with the data
     */
    public String getString(int i) {
        return character[i];
    }

    /** _more_          */
    List listData;

    /**
     * _more_
     *
     * @return _more_
     */
    public List asList() {
        if (listData != null) {
            return listData;
        }
        switch (type) {

          case TYPE_CHAR :
              return listData = new ArrayList(Arrays.asList(character));

          case TYPE_NUMERIC :
              listData = new ArrayList();
              for (int i = 0; i < numeric.length; i++) {
                  listData.add(new Double(numeric[i]));
              }
              return listData;
        }

        listData = new ArrayList();
        for (int i = 0; i < logical.length; i++) {
            listData.add(new Boolean(logical[i]));
        }
        return listData;
    }



    /**
     *  Method to retrieve data for this field
     *  @param i index of desired String, assumes 0 < i < getNumRec()
     *  @return either a Double, Boolean, or String with the data
     */
    public Object getData(int i) {
        switch (type) {

          case TYPE_CHAR :
              return character[i];

          case TYPE_NUMERIC :
              return new Double(numeric[i]);

          case TYPE_BOOLEAN :
              return new Boolean(logical[i]);
        }
        return null;
    }

    /**
     *  @return The number of records in the field.
     */
    public int getNumRec() {
        return nrec;
    }
}

/* Change History:
   $Log: DbaseData.java,v $
   Revision 1.11  2005/05/13 18:29:47  jeffmc
   Clean up the odd copyright symbols

   Revision 1.10  2005/03/10 18:38:52  jeffmc
   jindent and javadoc

   Revision 1.9  2005/01/20 20:46:24  jeffmc
   More changes to the shape file control. Put in
   some code to degrade a bit more gracefully when reading bad numeric data in the dbf files.,
   Javadoc, jindent, etc.

   Revision 1.8  2005/01/20 17:15:50  jeffmc
   First (and fairly extensive) pass at reading
   and handing dbf attribute databases in shape files.
   The shapefilecontrol now can filter on the attributes and
   display them in a tabular form. It doesn't effect
   the display yet.

   Revision 1.7  2004/02/27 21:22:42  jeffmc
   Lots of javadoc warning fixes

   Revision 1.6  2004/01/29 17:36:05  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.5  2002/01/03 17:42:20  jeffmc

   Fixes for jikes.


   CV i: ----------------------------------------------------------------------

   Revision 1.4  2000/08/18 04:15:26  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:55  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:22  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.2  1998/12/14  17:11:03  russ
# Add comment for accumulating change histories.
#
*/







