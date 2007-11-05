/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Read a Gempak grid file
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakGridReader extends GempakFileReader {

    /** Grid identifier */
    public static final String GRID = "GRID";

    /** grid headers */
    private List<GridHeader> gridList;

    // Grid packing types

    /** no packing */
    private static final int MDGNON = 0;

    /** GRIB1 packing */
    private static final int MDGGRB = 1;

    /** NMC packing */
    private static final int MDGNMC = 2;

    /** DIF packing */
    private static final int MDGDIF = 3;

    /** decimal packing? */
    private static final int MDGDEC = 4;

    /** GRIB2 packing */
    private static final int MDGRB2 = 5;

    /** Navigation Block */
    private GridNavBlock navBlock;

    /** Navigation Block */
    private GridAnalysisBlock analBlock;

    /** column headers */
    private static final String[] kcolnm = {
        "GDT1", "GTM1", "GDT2", "GTM2", "GLV1", "GLV2", "GVCD", "GPM1",
        "GPM2", "GPM3"
    };

    /** grid header len */
    private int khdrln = 0;

    /**
     * Bean ctor
     */
    public GempakGridReader() {}

    /**
     * Create a Gempak Grid Reader from the file
     *
     * @param filename  filename
     *
     * @throws IOException problem reading file
     */
    public GempakGridReader(String filename) throws IOException {
        super(filename);
    }

    /**
     * Create a Gempak Grid Reader from the file
     *
     * @param raf  RandomAccessFile
     *
     * @throws IOException problem reading file
     */
    public GempakGridReader(RandomAccessFile raf) throws IOException {
        super(raf);
    }

    /**
     * Initialize this reader.  Get the Grid specific info
     *
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init() throws IOException {

        if ( !super.init()) {
            return false;
        }

        // Modeled after GD_OFIL
        if (dmLabel.kftype != MFGD) {
            logError("not a grid file ");
            return false;
        }
        // find the part for GRID
        DMPart part = getPart("GRID");

        if (part == null) {
            logError("No part named GRID found");
            return false;
        }
        int lenhdr = part.klnhdr;
        if (lenhdr > LLGDHD) {
            logError("Grid part header too long");
            return false;
        }
        khdrln = lenhdr - 2;

        // check that the column names are correct
        for (int i = 0; i < keys.kkcol.size(); i++) {
            String colkey = keys.kkcol.get(i);
            if ( !colkey.equals(kcolnm[i])) {
                logError("Column name " + colkey + " doesn't match "
                         + kcolnm[i]);
                return false;
            }
        }

        // Make the NAV and ANAL blocks
        float[] headerArray = getFileHeader("NAVB");
        if (headerArray == null) {
            return false;
        }
        navBlock    = new GridNavBlock(headerArray);
        headerArray = getFileHeader("ANLB");
        if (headerArray == null) {
            return false;
        }
        analBlock = new GridAnalysisBlock(headerArray);

        // Make the grid headers
        // TODO: move this up into GempakFileReader using DM_RHDA
        // and account for the flipping there.
        int iword = dmLabel.kpcolh;
        gridList = new ArrayList<GridHeader>();
        int[] header = new int[dmLabel.kckeys];
        for (int i = 0; i < dmLabel.kcol; i++) {
            int valid = DM_RINT(iword++);
            DM_RINT(iword, header);
            if (valid != IMISSD) {
                GridHeader gh = new GridHeader(i + 1, header);
                gridList.add(gh);
            }
            iword+=header.length;
        }

        // find the packing types for these grids
        if ( !gridList.isEmpty()) {
            for (int i = 0; i < gridList.size(); i++) {
                GridHeader gh = (GridHeader) gridList.get(i);
                gh.packingType = getGridPackingType(gh.gridNumber);
            }
        }

        return true;

    }

    /**
     * Run the program
     *
     * @param args  filename
     *
     * @throws IOException problem reading the file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("need to supply a GEMPAK grid file name");
            System.exit(1);
        }

        GempakGridReader ggr = new GempakGridReader(args[0]);
        ggr.listGrids();
        String var = "PMSL";
        if (args.length > 1) {
            var = args[1];
        }
        GridHeader gh = ggr.findGrid(var);
        if (gh != null) {
            System.out.println(gh);
            float[] data = ggr.readGrid(gh.gridNumber);
            System.out.println("# of points = " + data.length);
            int cnt = 0;
            int it = 10;
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < data.length; i++) {
                if (cnt == it) cnt = 0;
                //if (cnt == 0) System.out.print("\ndata["+i+"-"+(i+it)+"] = ");
                //if (cnt == 0) System.out.print("\n");
                //System.out.print(Misc.format(data[i])+", ");
                cnt++;
                if (data[i] < min) {
                    min = data[i];
                }
                if (data[i] > max) {
                    max = data[i];
                }
            }
            System.out.println("max/min = " + max + "/"+min);
        }
        /*
        */
    }

    /**
     * Get the grid packing type
     *
     * @param gridNumber   grid number
     *
     * @return packing type or error number
     *
     * @throws IOException problem reading file
     */
    public int getGridPackingType(int gridNumber) throws IOException {
        // See DM_RDTR
        int irow = 1;  // Always 1 for grids
        int icol = gridNumber;
        if ((icol < 1) || (icol > dmLabel.kcol)) {
            System.out.println("bad grid number " + icol);
            return -9;
        }
        int iprt = getPartNumber("GRID");
        if (iprt == 0) {
            System.out.println("couldn't find part");
            return -10;
        }
        // gotta subtract 1 because parts are 1 but List is 0 based
        DMPart part = (DMPart) parts.get(iprt - 1);
        // check for valid data type
        if (part.ktyprt != MDGRID) {
            System.out.println("Not a valid type");
            return -21;
        }
        int ilenhd = part.klnhdr;
        int ipoint = dmLabel.kpdata
                     + (irow - 1) * dmLabel.kcol * dmLabel.kprt
                     + (icol - 1) * dmLabel.kprt + (iprt - 1);
        // From DM_RPKG
        int istart = DM_RINT(ipoint);
        if (istart == 0) {
            return -15;
        }
        int length = DM_RINT(istart);
        int isword = istart + 1;
        if (length <= ilenhd) {
            System.out.println("length (" + length
                               + ") is less than header length (" + ilenhd
                               + ")");
            return -15;
        } else if (Math.abs(length) > 10000000) {
            System.out.println("length is huge");
            return -34;
        }
        int[] header = new int[ilenhd];
        DM_RINT(isword, header);
        int nword = length - ilenhd;
        isword+=ilenhd;
        // read the data packing type
        int ipktyp = DM_RINT(isword);
        return ipktyp;
    }

    /**
     * Find the first grid with this name
     *
     * @param parm  name of grid
     *
     * @return  the grid header or null
     */
    public GridHeader findGrid(String parm) {
        if (gridList == null) {
            return null;
        }
        for (int i = 0; i < gridList.size(); i++) {
            GridHeader gh = (GridHeader) gridList.get(i);
            if (gh.param.trim().equals(parm)) {
                return gh;
            }
        }
        return null;
    }

    /**
     * Get a name for the grid packing type
     *
     * @param pktyp   packing type
     *
     * @return  String version of packing type
     */
    public static String getGridPackingName(int pktyp) {
        String packingType = "UNKNOWN";
        switch (pktyp) {

          case MDGNON :
              packingType = "MDGNON";
              break;

          case MDGGRB :
              packingType = "MDGGRB";
              break;

          case MDGNMC :
              packingType = "MDGNMC";
              break;

          case MDGDIF :
              packingType = "MDGDIF";
              break;

          case MDGDEC :
              packingType = "MDGDEC";
              break;

          case MDGRB2 :
              packingType = "MDGRB2";
              break;

          default :
              break;
        }
        return packingType;
    }

    /**
     * Get the grid packing type
     *
     * @param gridNumber   grid number
     *
     * @return packing type or error number
     *
     * @throws IOException problem reading file
     */
    public float[] readGrid(int gridNumber) throws IOException {
        float[] data = null;
        // See DM_RDTR
        int irow = 1;  // Always 1 for grids
        int icol = gridNumber;
        if ((icol < 1) || (icol > dmLabel.kcol)) {
            System.out.println("bad grid number " + icol);
            return null;
        }
        int iprt = getPartNumber("GRID");
        if (iprt == 0) {
            System.out.println("couldn't find part");
            return null;
        }
        // gotta subtract 1 because parts are 1 but List is 0 based
        DMPart part = (DMPart) parts.get(iprt - 1);
        // check for valid data type
        if (part.ktyprt != MDGRID) {
            System.out.println("Not a valid type");
            return null;
        }
        int ilenhd = part.klnhdr;
        int ipoint = dmLabel.kpdata
                     + (irow - 1) * dmLabel.kcol * dmLabel.kprt
                     + (icol - 1) * dmLabel.kprt + (iprt - 1);
        // From DM_RDTR
        int istart = DM_RINT(ipoint);
        if (istart == 0) {
            return null;
        }
        int length = DM_RINT(istart);
        int isword = istart+1;
        if (length <= ilenhd) {
            System.out.println("length (" + length
                               + ") is less than header length (" + ilenhd
                               + ")");
            return null;
        } else if (Math.abs(length) > 10000000) {
            System.out.println("length is huge");
            return null;
        }
        int[] header = new int[ilenhd];
        DM_RINT(isword, header);
        int nword = length - ilenhd;
        isword+=header.length;
        
        // from DM_RPKG
        // read the data packing type
        int ipktyp = DM_RINT(isword);
        int iiword = isword+1;
        int lendat = nword - 1;
        if (ipktyp == MDGNON) {  // no packing
            data = new float[lendat];
            DM_RFLT(iiword, data);
            return data;
        }
        int iiw;
        int irw;
        if (ipktyp == MDGDIF) {
            iiw = 4;
            irw = 3;
        } else if (ipktyp == MDGRB2) {
            iiw = 4;
            irw = 1;
        } else {
            iiw = 3;
            irw = 2;
        }
        int[]   iarray = new int[iiw];
        float[] rarray = new float[irw];
        DM_RINT(iiword, iarray);
        iiword = iiword + iiw;
        lendat = lendat - iiw;
        DM_RFLT(iiword, rarray);
        iiword = iiword + irw;
        lendat = lendat - irw;

        if (ipktyp == MDGRB2) {
            data = unpackGrib2Data(iiword, lendat, iarray, rarray);
            return data;
        }
        int     nbits  = iarray[0];
        int     misflg = iarray[1];
        boolean miss   = misflg != 0;
        int     kxky   = iarray[2];
        int     mword  = kxky;
        int     kx     = 0;
        if (iiw == 4) {
            kx = iarray[3];
        }
        float ref    = rarray[0];
        float scale  = rarray[1];
        float difmin = 0;
        if (irw == 3) {
            difmin = rarray[2];
        }
        data = unpackData(iiword, lendat, ipktyp, kxky, nbits, ref, scale,
                          miss, difmin, kx);
        return data;
    }

    /**
     * Read packed data
     *
     * @param iiword         Starting word  (FORTRAN 1 based)
     * @param nword          Number of words
     * @param ipktyp         Packing type
     * @param kxky           Number of grid points
     * @param nbits          Number of bits
     * @param ref            Reference minimum value of grid
     * @param scale          Scaling factor
     * @param miss           Missing data flag
     * @param difmin         Minimum value of differences
     * @param kx             Number of points in x direction
     *
     * @return   unpacked data
     *
     * @throws IOException problem reading file
     */
    private float[] unpackData(int iiword, int nword, int ipktyp, int kxky,
                               int nbits, float ref, float scale,
                               boolean miss, float difmin, int kx)
            throws IOException {
        if (ipktyp == MDGGRB) {
            float[] values = new float[kxky];
            int     imax   = (int) Math.pow(2, nbits) - 1;
            int     iword  = 0;
            int     ibit   = 1;
            bitPos = 0;
            bitBuf = 0;
            rf.seek(getOffset(iiword));
            //rf.order(RandomAccessFile.LITTLE_ENDIAN);
            //int[] iwords = new int[nword];
            //DM_RINT(iiword,iwords);
            //int jshft, idat,idat2;
            int idataw;
            for (int i = 0; i < values.length; i++) {
                int idat = bits2UInt(nbits, rf);
                if (i < 25) {
                    System.out.println("idat[" + i + "] = " + idat);
                }
                //values[i] = ref + scale * bits2UInt(nbits, rf);
                values[i] = ref + scale * idat;
                /*
//C
//C*          Get the integer from the buffer.
//C
                jshft = nbits + ibit - 33;
                idat  = 0;
                //idat  = ISHFT ( idata [iword], jshft );
                //idat  = IAND  ( idat, imax );
                idataw = iwords[iword];
                idat = iwords[iword] >> jshft;
                idat = idat & imax;
//C
//C*          Check to see if packed integer overflows into next word.
//C
                if  ( jshft  >   0 )  {
                    jshft = jshft - 32;
                    idat2 = 0;
                    //idat2 = ISHFT ( idata [iword+1], jshft );
                    //idat  = IOR ( idat, idat2 );
                    idataw = iwords[iword+1];
                    idat2 = iwords[iword+1] >> jshft;
                    idat = idat | imax;
                }
//C
//C*          Compute value of word.
//C
                if  ( ( idat  ==  imax )  &&  miss )  {
                    values [i] = RMISSD;
                } else {
                    values [i] = ref + idat * scale;
                }
//C
//C*          Set location for next word.
//C
                ibit = ibit + nbits;
                if  ( ibit  >   32 ) {
                    ibit  = ibit - 32;
                    iword = iword + 1;
                }
                if (i < 25) System.out.println("idat["+i+"] = " + idat);
                */
            }
            return values;
        } else if (ipktyp == MDGNMC) {
            return null;
        } else if (ipktyp == MDGDIF) {
            return null;
        }
        return null;
    }

    /**
     * Read packed Grib2 data
     *
     * @param iiword  Starting word  (FORTRAN 1 based)
     * @param lendat  Number of words
     * @param iarray  integer packing info
     * @param rarray  float packing info
     * @return   unpacked data
     *
     * @throws IOException problem reading file
     */
    private float[] unpackGrib2Data(int iiword, int lendat, int[] iarray,
                                    float[] rarray)
            throws IOException {
        return null;
    }

    /**
     * Print out the navibation block so it looks something like this:
     * <pre>
     *   GRID NAVIGATION:
     *        PROJECTION:          LCC
     *        ANGLES:                25.0   -95.0    25.0
     *        GRID SIZE:           93  65
     *        LL CORNER:              12.19   -133.46
     *        UR CORNER:              57.29    -49.38
     * </pre>
     */
    public void printNavBlock() {
        StringBuffer buf = new StringBuffer("GRID NAVIGATION:");
        if (navBlock != null) {
            buf.append(navBlock.toString());
        } else {
            buf.append("\n\tUNKNOWN GRID NAVIGATION");
        }
        System.out.println(buf.toString());
    }

    /**
     * Print out the analysis block so it looks something like this:
     */
    public void printAnalBlock() {
        StringBuffer buf = new StringBuffer("GRID ANALYSIS BLOCK:");
        if (analBlock != null) {
            buf.append(analBlock.toString());
        } else {
            buf.append("\n\tUNKNOWN ANALYSIS TYPE");
        }
        System.out.println(buf.toString());
    }

    /**
     * Get list of grids
     * @return list of grids
     */
    public List<GridHeader> getGridList() {
        return gridList;
    }

    /**
     * Print out the grids.
     */
    public void printGrids() {
        if (gridList == null) {
            return;
        }
        System.out.println(
            "  NUM       TIME1              TIME2           LEVL1 LEVL2  VCORD PARM");
        for (Iterator iter = gridList.iterator(); iter.hasNext(); ) {
            System.out.println(iter.next());
        }
    }

    /**
     * A class to hold grid header information
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class GridHeader {

        /** Time 1 */
        public String time1;

        /** Time 2 */
        public String time2;

        /** Level 1 */
        public int level1 = IMISSD;

        /** Level 2 */
        public int level2 = IMISSD;

        /** coordinate type */
        public int ivcord;

        /** parameter */
        public String param;

        /** grid number */
        public int gridNumber;  // column

        /** packing type */
        public int packingType;

        /**
         * Create a grid header from the integer bits
         * @param number  grid number
         * @param header integer bits
         */
        public GridHeader(int number, int[] header) {
            gridNumber = number;
            int[] times1 = GempakUtil.TG_FTOI(header, 0);
            time1 = GempakUtil.TG_ITOC(times1);
            int[] times2 = GempakUtil.TG_FTOI(header, 2);
            time2  = GempakUtil.TG_ITOC(times2);
            level1 = header[4];
            level2 = header[5];
            ivcord = header[6];
            if (ivcord > 6) ivcord = GempakUtil.swp4(ivcord);
            if (needToSwap) GempakUtil.swp4(header, 7, 9);
            param = McIDASUtil.intBitsToString(new int[] { header[7],
                    header[8], header[9] });

        }

        /**
         * Get a String representation of this object
         * @return a String representation of this object
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(StringUtil.padLeft(String.valueOf(gridNumber), 5));
            buf.append(StringUtil.padLeft(time1, 20));
            buf.append(" ");
            buf.append(StringUtil.padLeft(time2, 20));
            buf.append(" ");
            buf.append(StringUtil.padLeft(String.valueOf(level1), 5));
            if (level2 != -1) {
                buf.append(StringUtil.padLeft(String.valueOf(level2), 5));
            } else {
                buf.append("     ");
            }
            buf.append("  ");
            buf.append(StringUtil.padLeft(GempakUtil.LV_CCRD(ivcord), 6));
            buf.append(" ");
            buf.append(param.trim());
            buf.append(" ");
            buf.append(getGridPackingName(packingType));
            return buf.toString();
        }

    }

    /**
     * List out the grids (aka GDINFO)
     */
    public void listGrids() {
        System.out.println("/nGRID FILE: " + getFilename() + "\n");
        printNavBlock();
        System.out.println("");
        printAnalBlock();
        System.out.println("\nNumber of grids in file:  " + gridList.size());
        System.out.println("\nMaximum number of grids in file:  "
                           + dmLabel.kcol);
        System.out.println("");
        printGrids();
    }

    /** bit position */
    int bitPos = 0;

    /** bit buffer size */
    int bitBuf = 0;

    /**
     * Convert bits (nb) to Unsigned Int .
     *
     * @param nb
     * @param raf
     * @throws IOException
     * @return int of BinaryDataSection section
     */
    private int bits2UInt(int nb, RandomAccessFile raf) throws IOException {
        int bitsLeft = nb;
        int result   = 0;

        if (bitPos == 0) {
            bitBuf = raf.read();
            bitPos = 8;
        }

        while (true) {
            int shift = bitsLeft - bitPos;
            if (shift > 0) {
                // Consume the entire buffer
                result   |= bitBuf << shift;
                bitsLeft -= bitPos;

                // Get the next byte from the RandomAccessFile
                bitBuf = raf.read();
                bitPos = 8;
            } else {
                // Consume a portion of the buffer
                result |= bitBuf >> -shift;
                bitPos -= bitsLeft;
                bitBuf &= 0xff >> (8 - bitPos);  // mask off consumed bits

                return result;
            }
        }                                        // end while
    }                                            // end bits2Int
}

