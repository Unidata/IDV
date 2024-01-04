/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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

package ucar.unidata.gis.shapefile;


import ucar.unidata.io.Swap;



import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Class to read a dbase file in its entirety.
 *
 * @author  Kirk Waters, NOAA Coastal Services Center, 1997.
 * @author  Russ Rew, modified to restrict access to read-only
 */
public class DbaseFile extends Object {

    /** _more_ */
    static public int DBASEIII = 0;

    /** _more_ */
    static public int DBASEIV = 1;

    /** _more_ */
    static public int DBASE5DOS = 2;

    /** _more_ */
    static public int DBASE5WIN = 3;

    /** _more_ */
    URL url;

    /** _more_ */
    byte filetype;

    /** _more_ */
    byte updateYear;

    /** _more_ */
    byte updateMonth;

    /** _more_ */
    byte updateDay;

    /** _more_ */
    int nrecords;

    /** _more_ */
    int nfields;

    /** _more_ */
    short nbytesheader;

    /** _more_ */
    short nbytesrecord;

    /** _more_ */
    DbaseFieldDesc[] FieldDesc;

    /** _more_ */
    DbaseData[] data;

    /** _more_ */
    byte[] Header;

    /** _more_ */
    private boolean headerLoaded = false;

    /** _more_ */
    private boolean dataLoaded = false;

    /** _more_ */
    InputStream stream = null;

    /** _more_ */
    DataInputStream ds = null;

    /**
     * Instantiates a new dbase file.
     *
     * @param url URL to the *.dbf file
     * @throws MalformedURLException the malformed url exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public DbaseFile(URL url) throws MalformedURLException, IOException {
        this.url = url;
        stream   = url.openStream();
    }

    /**
     * Instantiates a new dbase file.
     *
     * @param spec Location of the *.dbf file, as either a URL or filename
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public DbaseFile(String spec) throws IOException {
        try {
            url    = new URL(spec);
            stream = url.openStream();
        } catch (java.net.MalformedURLException e) {
            stream = new FileInputStream(spec);
            if (stream == null) {
                System.out.println("Got a null trying to open " + spec);
                throw new java.io.IOException("Failed to open stream");
            }
        }
    }

    /**
     * Instantiates a new dbase file.
     *
     * @param file A file object of the *.dbf file.
     */
    public DbaseFile(File file) {
        try {
            stream = new FileInputStream(file);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Failed to open file " + file);
            stream = null;
        }
    }

    /**
     * Instantiates a new dbase file.
     *
     * @param s the s
     */
    public DbaseFile(InputStream s) {
        stream = s;
    }

    /**
     *  Load the dbase file header.
     *  @return 0 for success, -1 for failure
     */
    public int loadHeader() {
        if (headerLoaded) {
            return 0;
        }
        InputStream s = stream;
        if (s == null) {
            return -1;
        }
        try {
            BufferedInputStream bs = new BufferedInputStream(s);
            ds = new DataInputStream(bs);
            /* read the header as one block of bytes*/
            Header = new byte[32];
            ds.read(Header);
            //System.out.println("dbase header is " + Header);
            if (Header[0] == '<') {  //looks like html coming back to us!
                close(ds);
                return -1;
            }
            filetype    = Header[0];


            updateYear  = Header[1];
            updateMonth = Header[2];
            updateDay   = Header[3];
            /* 4 bytes for number of records is in little endian */
            nrecords     = Swap.swapInt(Header, 4);
            nbytesheader = Swap.swapShort(Header, 8);
            nbytesrecord = Swap.swapShort(Header, 10);

            /* read in the Field Descriptors */
            /* have to figure how many there are from
             * the header size.  Should be nbytesheader/32 -1
             */
            nfields = (int) (nbytesheader / 32) - 1;
            if (nfields < 1) {
                System.out.println("nfields = " + nfields);
                System.out.println("nbytesheader = " + nbytesheader);
                return -1;
            }
            FieldDesc = new DbaseFieldDesc[nfields];
            data      = new DbaseData[nfields];
            for (int i = 0; i < nfields; i++) {
                FieldDesc[i] = new DbaseFieldDesc(ds, filetype);
                data[i]      = new DbaseData(FieldDesc[i], nrecords);
            }

            /* read the last byte of the header (0x0d) */
            ds.readByte();

            headerLoaded = true;
        } catch (java.io.IOException e) {
            close(s);
            return -1;
        }
        return 0;
    }

    /**
     *  Load the dbase file data.
     *  @return 0 for success, -1 for failure
     */

    public int loadData() {
        if ( !headerLoaded) {
            return -1;
        }
        if (dataLoaded) {
            return 0;
        }
        InputStream s = stream;
        if (s == null) {
            return -1;
        }
        try {
            /* read in the data */
            for (int i = 0; i < nrecords; i++) {
                /* read the data record indicator */
                byte recbyte = ds.readByte();
                if (recbyte == 0x20) {
                    for (int j = 0; j < nfields; j++) {
                        try {
                            data[j].readRowN(ds, i);
                        } catch (NumberFormatException nfe) {
                            System.err.println("nfe field=" + j + " row="
                                    + i);
                            System.err.println("data:" + data[j].getType());
                            throw nfe;
                        }
                    }
                } else {
                    /* a deleted record */
                    nrecords--;
                    i--;
                }
            }
            dataLoaded = true;
        } catch (java.io.IOException e) {
            close(s);
            return -1;
        }
        /*
          catch(java.net.UnknownServiceException e){
          return -1;
          }
        */
        finally {
            if (s != null) {
                close(s);
            }
        }
        return 0;
    }

    /**
     * Close.
     *
     * @param d the d
     */
    private void close(InputStream d) {
        //TODO: don't close the stream for now
        if (true) {
            return;
        }
        if (d == null) {
            return;
        }
        try {
            d.close();
        } catch (java.io.IOException e) {}
        catch (NullPointerException e) {}
    }

    /**
     *  Extract the data for a field by field index number.
     *  @param index Column number of the field to extract.
     *  @return A DbaseData object if the column is within bounds. Otherwise, null.
     */
    public DbaseData getField(int index) {
        if ((index < 0) || (index >= nfields)) {
            return null;
        }
        return data[index];
    }

    /**
     *  Extract the data for a given field by name.
     *  @param Name String with the name of the field to retrieve.
     *  @return A DbaseData object if the name was found or null if not found
     */
    public DbaseData getField(String Name) {
        for (int i = 0; i < nfields; i++) {
            if (FieldDesc[i].Name.equals(Name)) {
                return data[i];
            }
        }
        return null;
    }

    /**
     *  Extract the double array of data for a field by Name.
     *  @param Name String with the name of the field to retrieve
     *  @return A double[] if valid numeric field, otherwise null
     */
    public double[] getDoublesByName(String Name) {
        DbaseData d;
        if ((d = getField(Name)) == null) {
            return null;
        }
        if (d.getType() == DbaseData.TYPE_CHAR) {
            String[] s  = d.getStrings();
            double[] dd = new double[s.length];
            for (int i = 0; i < s.length; i++) {
                dd[i] = Double.valueOf(s[i]).doubleValue();
            }
            return dd;
        }
        if (d.getType() == DbaseData.TYPE_BOOLEAN) {
            boolean[] b  = d.getBooleans();
            double[]  dd = new double[b.length];
            for (int i = 0; i < b.length; i++) {
                if (b[i]) {
                    dd[i] = 1;
                } else {
                    dd[i] = 0;
                }
            }
            return dd;

        }
        return d.getDoubles();
    }

    /**
     *  Extract the string array of data for a field by Name.
     *  @param Name String with the name of the field to retrieve
     *  @return A String[] if valid character field, otherwise null
     */
    public String[] getStringsByName(String Name) {
        DbaseData d;
        if ((d = getField(Name)) == null) {
            return null;
        }
        if (d.getType() != DbaseData.TYPE_CHAR) {
            return null;
        }
        return d.getStrings();
    }

    /**
     *  Extract the boolean array of data for a field by Name.
     *  @param Name String with the name of the field to retrieve
     *  @return A boolean[] if valid character field, otherwise null
     */
    public boolean[] getBooleansByName(String Name) {
        DbaseData d;
        if ((d = getField(Name)) == null) {
            return null;
        }
        if (d.getType() != DbaseData.TYPE_BOOLEAN) {
            return null;
        }
        return d.getBooleans();
    }

    /**
     *  Get the name of a field by column number.
     *  @param i The column number of the field name.
     *  @return A String with the field name or null if out of bounds
     */
    public String getFieldName(int i) {
        if ((i >= nfields) || (i < 0)) {
            return null;
        }
        return (FieldDesc[i].Name);
    }

    /**
     *  Get a list of all the field names in the dbase file
     *  @return A String array of all the field names
     */
    public String[] getFieldNames() {
        String[] s = new String[nfields];
        for (int i = 0; i < nfields; i++) {
            s[i] = new String(getFieldName(i));
        }
        return (s);
    }

    /**
     *  Get the number of fields in the file.
     *  @return number of fields
     */
    public int getNumFields() {
        return (nfields);
    }

    /**
     *   @return number of records in the dbase file
     */
    public int getNumRecords() {
        return (nrecords);
    }

    /**
     *  @return Boolean true if the data has been loaded, otherwise false.
     */
    public boolean isLoaded() {
        return (dataLoaded);
    }

    /**
     * Test program, dumps a Dbase file to stdout.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("filename or URL required");
            System.exit(-1);
        }
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            System.out.println("*** Dump of Dbase " + s + ":");
            try {
                DbaseFile dbf = new DbaseFile(s);
                // load() method reads all data at once
                if (dbf.loadHeader() != 0) {
                    System.out.println("Error loading header" + s);
                    System.exit(-1);
                }

                // output schema as [type0 field0, type1 field1, ...]
                String[] fieldNames = dbf.getFieldNames();
                System.out.print("[");

                int         nf  = dbf.getNumFields();
                DbaseData[] dbd = new DbaseData[nf];
                for (int field = 0; field < nf; field++) {
                    dbd[field] = dbf.getField(field);
                    switch (dbd[field].getType()) {

                      case DbaseData.TYPE_BOOLEAN :
                          System.out.print("boolean ");
                          break;

                      case DbaseData.TYPE_CHAR :
                          System.out.print("String ");
                          break;

                      case DbaseData.TYPE_NUMERIC :
                          System.out.print("double ");
                          break;
                    }
                    System.out.print(fieldNames[field]);
                    if (field < nf - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");

                if (dbf.loadData() != 0) {
                    System.out.println("Error loading data" + s);
                    System.exit(-1);
                }

                // output data
                for (int rec = 0; rec < dbf.getNumRecords(); rec++) {
                    for (int field = 0; field < nf; field++) {
                        System.out.print(dbd[field].getData(rec));
                        if (field < nf - 1) {
                            System.out.print(", ");
                        } else {
                            System.out.println();
                        }
                    }
                }
            } catch (java.io.IOException e) {
                System.out.println(e);
                System.exit(-1);
            }
        }
    }
}
