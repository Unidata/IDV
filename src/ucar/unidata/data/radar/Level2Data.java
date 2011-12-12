/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.radar;


import ucar.netcdf.HTTPRandomAccessFile;


import ucar.netcdf.RandomAccessFile;

import ucar.unidata.data.DataContext;
import ucar.unidata.io.bzip2.BZip2ReadException;

import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;

import java.io.File;
import java.io.IOException;

import java.net.URL;


/**
 * This class reads a NEXRAD level II data  file and keeps track
 * of cut start and other info. Will probably change in the future.<p>
 *
 * Adapted with permission from the Java Iras software developed
 * by David Priegnitz at NSSL.<p>
 *
 * Documentation on Archive Level II data format can be found at:
 * <a href="http://www.ncdc.noaa.gov/oa/radar/leveliidoc.html">
 * http://www.ncdc.noaa.gov/oa/radar/leveliidoc.html</a>
 *
 * @author MetApps Development Team
 * @version $Revision: 1.27 $ $Date: 2007/05/04 16:00:18 $
 */
public class Level2Data {

    /** number of bytes to skip in file header */
    private int skip_file_header = 24;

    /** number of bytes to skip in record header */
    private int skip_record_header = 12;

    /** Radar data message size */
    private int RADAR_DATA_MSG_SIZE = 2432;

    /** File header size */
    private static final int FILE_HEADER_SIZE = 24;

    /** Volume coverage pattern */
    private int vcp = 0;

    /** last cut */
    private int old_cut = -1;

    /** end of volume flag */
    private int end_of_volume = 0;

    /** array of azimuths */
    private float[] azimuth = new float[10000];

    /** array of elevation angles */
    private float[] elevation = new float[10000];

    /** array of true elevation angles */
    private float[] trueElevation = new float[100];

    /** cut start locations */
    private int[] cut_start = new int[100];

    /** number of cuts */
    private int numberOfCuts = 0;

    /** number of true cuts */
    private int numberOfTrueCuts = 0;

    /** cut elevation */
    private float cutElevation[] = new float[100];

    /** cut index */
    private int cutIndex[] = new int[100];

    /** old elevation */
    private float oldElevation = 0.f;

    /** delta elevation */
    private float deltaElevation = 0.f;

    /** number of records */
    private int number_of_records = 0;

    /** current record */
    private int record = 0;

    /** time since the epoch */
    private int timeSinceEpoch;

    /** milliseconds since midnight */
    private int millis;

    /** flag for reading all the data */
    private boolean readAll = false;

    /** Data file */
    private RandomAccessFile din;

    /** filename */
    private String filename = null;

    /** DataContext */
    private DataContext context = null;

    // TODO: find optimal value of default buffer size

    /** Default buffer size for a remote read */
    private static final int DEFAULT_FILE_BUFFER = 204800;

    /** Default buffer size for a remote read */
    private static final int DEFAULT_HTTP_BUFFER = 204800;

    /** Identifier for ARCHIVE2 format */
    public static final String ARCHIVE2 = "ARCHIVE2";

    /** Identifier for AR2V0001 format */
    public static final String AR2V0001 = "AR2V0001";

    /** data type (format) */
    private String dataType = null;

    /** volume number */
    private int volumeNumber = 0;

    /** Identifier for Station ID */
    private String stationId = null;

    /** file header */
    private byte[] fileHeader = new byte[FILE_HEADER_SIZE];

    /** log category */
    private static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(Level2Data.class.getName());

    /**
     * Create a new Level II data wrapper for the file
     * @param file file to read
     * @param context  DataContext for finding temporary directory
     *
     * @throws IOException problem reading or writing file
     */
    public Level2Data(File file, DataContext context) throws IOException {
        this(file.getAbsolutePath(), context);
    }

    /**
     * Create a new Level II data wrapper for the file
     * @param filename name of the file to read
     * @param context  DataContext for finding temporary directory
     *
     * @throws IOException problem reading or writing file
     */
    public Level2Data(String filename, DataContext context)
            throws IOException {
        this.filename = filename;
        this.context  = context;
        makeInputStream();
    }

    /**
     * Create the input stream for this data
     *
     * @throws IOException problem reading or creating input stream
     */

    private void makeInputStream() throws IOException {

        din = new RandomAccessFile(filename, "r", DEFAULT_FILE_BUFFER);
        log_.debug("reading from " + din);

        log_.debug("-----" + filename + "-----");
        log_.debug("\nLoading Data starting with record " + number_of_records
                   + " Be patient!");


        din.seek(0);
        din.readFully(fileHeader);
        readHeaderInfo();

        byte[] testArray = new byte[6];
        din.readFully(testArray);
        String bz = new String(testArray, 4, 2);
        if ((bz).equals("BZ")) {
            File   tmpfile;
            String fileTail = IOUtil.getFileTail(filename) + "_tmp";
            if (context == null) {
                String directory = "level2";
                IOUtil.makeDir(directory);
                tmpfile = new File(IOUtil.joinDir(directory, fileTail));
            } else {
                tmpfile =
                    new File(context.getObjectStore().getTmpFile(fileTail));
            }
            RandomAccessFile dout = null;

            log_.debug("tmpfile = " + tmpfile);

            if ( !tmpfile.exists()) {
                byte[]            ubuff  = new byte[40000];
                byte[]            obuff  = new byte[40000];
                CBZip2InputStream cbzip2 = new CBZip2InputStream();
                dout = new RandomAccessFile(tmpfile.toString(), "rw",
                                            DEFAULT_FILE_BUFFER);
                log_.debug("writing to " + tmpfile + " " + dout);
                long start = System.currentTimeMillis();
                dout.write(fileHeader);
                din.seek(FILE_HEADER_SIZE);
                int numCompBytes = 0;

                try {
                    while ((numCompBytes = din.readInt()) != -1) {
                        log_.debug("reading compressed bytes "
                                   + numCompBytes);
                        /*
                         * For some stupid reason, the last block seems to
                         * have the number of bytes negated.  So, we just
                         * assume that any negative number (other than -1)
                         * is the last block and go on our merry little way.
                         */
                        if (numCompBytes < 0) {
                            numCompBytes = -numCompBytes;
                        }
                        byte[] buf = new byte[numCompBytes];

                        din.readFully(buf);
                        ByteArrayInputStream bais =
                            new ByteArrayInputStream(buf, 2,
                                numCompBytes - 2);
                        cbzip2.setStream(bais);

                        int nread = 0;
                        int total = 0;
                        try {
                            while ((nread = cbzip2.read(ubuff)) != -1) {
                                if (total + nread > obuff.length) {
                                    byte[] temp = obuff;
                                    obuff = new byte[temp.length * 2];
                                    System.arraycopy(temp, 0, obuff, 0,
                                            temp.length);
                                }
                                System.arraycopy(ubuff, 0, obuff, total,
                                        nread);
                                total += nread;
                            }
                            dout.write(obuff, 0, total);
                        } catch (BZip2ReadException bz2e) {
                            log_.debug("Exception reading data "
                                       + bz2e.getMessage());
                        }
                        log_.debug("reading num bytes at "
                                   + dout.getFilePointer());
                    }
                } catch (EOFException eof) {}  // ignore
            } else {
                dout = new RandomAccessFile(tmpfile.toString(), "r",
                                            DEFAULT_FILE_BUFFER);
            }
            filename = tmpfile.toString();
            din.close();
            din = dout;
        }
        readHeaderInfo();
        vcp               = 0;
        number_of_records = 0;
    }

    /**
     * Read the header info
     *
     * @throws IOException problem reading header info
     */
    private void readHeaderInfo() throws IOException {

        try {
            dataType = new String(fileHeader, 0, 8);
            volumeNumber = (int) Misc.parseDouble(new String(fileHeader, 9,
                    3));
            timeSinceEpoch =
                new DataInputStream(new ByteArrayInputStream(fileHeader, 12,
                    4)).readInt();
            millis = new DataInputStream(new ByteArrayInputStream(fileHeader,
                    16, 4)).readInt();

            stationId = new String(fileHeader, 20, 4).trim();

            if (stationId.equals("")) {
                stationId = null;
            }

            log_.debug("dataType = " + dataType);
            log_.debug("volume number = " + volumeNumber);
            log_.debug("stationId = >" + stationId + "<");
            log_.debug("timeSinceEpoch = " + timeSinceEpoch);
            log_.debug("millis = " + millis);
        } catch (Exception e) {
            throw new IOException("unable to read header information");
        }
    }

    /**
     * Method to read the file.
     *
     * @param startFlag  starting record number (unused)
     */
    public void read(int startFlag) {
        read(startFlag, false);
    }

    /**
     * Method to read the file.
     * If * <code>onlyVCP</code>, only read enough data to get the
     * levels.
     *
     * @param startFlag  starting record number (unused)
     * @param onlyVCP    if true, only read until we get the VCP info
     */
    public void read(int startFlag, boolean onlyVCP) {

        /*  Read the file until we get an I/O exception (i.e., EOF)
            or until we get the VCP if onlyVCP == true */

        float        azi;
        float        ele;
        int          i;

        byte[]       bins         = new byte[2332];

        Level2Record level2record = new Level2Record();

        // Read the entire file and return when we get an end-of-file
        // exception.
        for (record = number_of_records; record < 10000; record++) {

            if (onlyVCP && (vcp != 0)) {
                log_.debug("vcp = " + vcp);
                return;
            }
            level2record.readHeader(din, record);

            if (level2record.eof() != 0) {

                cut_start[old_cut + 1] = record;
                log_.debug(" did read all");
                readAll = true;
                return;

            }

            // Check to see if this is the last radial in the volume.  If 
            // so, set the end of volume flag to 1.
            if (level2record.radial_status != 1) {
                log_.debug("Radial status [" + level2record.radial_status
                           + "] for record " + record);
            }
            if (level2record.radial_status == 4) {
                end_of_volume = 1;
            } else {
                end_of_volume = 0;
            }

            // Only process digital radar data messages.  Ignore the 
            // rest for  now. 
            if (level2record.getMessageType() == 1) {

                // If the vcp has yet to be initialized set it.  Right now 
                // we assume that the VCP is one of the following: 
                // 11, 21, 31, 32.

                if (vcp == 0) {

                    vcp = level2record.vcp;
                }

                // Extract the azimuth and elevation angles of the radial.

                azimuth[record]   = level2record.getAzimuth();
                elevation[record] = level2record.getElevation();

                // If a new elevation cut has started, update the cut LUT

                if (old_cut != level2record.getElevationNum()) {

                    log_.debug("message_size [" + level2record.message_size
                               + "]");

                    if (level2record.getElevationNum() > 0) {

                        log_.debug("New cut --> "
                                   + level2record.getElevationNum()
                                   + " angle[" + level2record.getElevation()
                                   + "] at record " + record);
                        old_cut = level2record.getElevationNum();
                        cut_start[level2record.getElevationNum() - 1] =
                            record;
                        cutElevation[numberOfCuts] = elevation[record];
                        deltaElevation = Math.abs(elevation[record]
                                - oldElevation);
                        if ((double) deltaElevation > 0.10) {
                            trueElevation[numberOfTrueCuts] =
                                elevation[record];
                            numberOfTrueCuts++;
                        }
                        oldElevation           = elevation[record];
                        cutIndex[numberOfCuts] = numberOfTrueCuts;
                        numberOfCuts++;

                        log_.debug(
                            "dBZ bins ["
                            + level2record.getBinNum(
                                level2record.REFLECTIVITY) + "]");
                        log_.debug(
                            "Vel bins ["
                            + level2record.getBinNum(level2record.VELOCITY)
                            + "]");

                    } else {

                        return;

                    }
                }

                number_of_records++;

            } else {

                // Since this message doesn't contain radar data, set the
                // azimuth/elevation LUT table entries to -1 to indicate 
                // this record doesn't contain radar data.

                log_.debug("Message type " + level2record.getMessageType()
                           + " detected");
                azimuth[record]   = -1;
                elevation[record] = -1;

            }
        }
        readAll = true;  // end of file reached?


        /* System.out.println ("  hit eof"); */
        /* System.out.println (" vvv readall is "+readAll); */

    }

    /**
     * Get the true number of slices from the file.  May differ from
     * expected cuts.
     *
     * @return number of true cuts
     */
    public int getNumberOfTrueCuts() {
        return numberOfTrueCuts;
    }

    /**
     * Get the index of the particular cut
     *
     * @param i  cut number
     *
     * @return cut index
     */
    public int getCutIndex(int i) {
        if (i <= numberOfCuts) {
            return 0;
        } else {
            return cutIndex[i];
        }
    }

    /**
     * Get the number of cuts found in this file.
     *
     * @return the number of cuts
     */
    public int getNumberOfCuts() {
        return numberOfCuts;
    }

    /**
     * Get the elevation angle of a particular cut
     *
     * @param i   cut number
     *
     * @return elevation angle
     */
    public float getCutElevation(int i) {
        if (i >= numberOfCuts) {
            return -99.9F;
        } else {
            return cutElevation[i];
        }
    }

    /**
     * Get the true elevation for a particular cut
     *
     * @param i   cut number
     *
     * @return true elevation angle (may differ from published values)
     */
    public float getTrueElevation(int i) {
        return trueElevation[i];
    }

    /**
     * This method returns the record number where the specified
     * elevation cut begins.
     * @param  cut_num  cut number
     * @return starting record number for that cut.
     */
    public int getCutStart(int cut_num) {

        if ( !readAll) {
            read(0);
        }
        return (cut_start[cut_num]);

    }

    /**
     * This method returns the azimuth angle for the specified
     * record.
     * @param  record  record number
     * @return azimuth angle for the specified record.
     */
    public float getAzimuth(int record) {

        if ( !readAll) {
            read(0);
        }
        return (azimuth[record]);

    }

    /**
     * This method returns the elevation angle for the specified
     * record.
     * @param  record  record number
     * @return elevation angle for the specified record.
     */
    public float getElevation(int record) {

        if ( !readAll) {
            read(0);
        }
        return (elevation[record]);

    }

    /**
     * This method returns the number of records read in from a
     * file.
     * @return  number of data records read in this file.
     */
    public int numberOfRecords() {

        if ( !readAll) {
            read(0);
        }
        return (number_of_records);

    }

    /**
     * Get the Volume Coverage Pattern number for this data.
     * @return VCP with:
     * <pre>
     * Value of:    11 = 14 elev. scans/ 5 mins.
     *              12 = 14 elev. scans/ 4.1 mins.
     *              21 = 11 elev. scans/ 6 mins.
     *              31 = 8 elev. scans/ 10 mins.
     *              32 = 7 elev. scans/ 10 mins.
     *             121 = 9 elev. scans/ 5 mins.
     * </pre>
     */
    public int getVCP() {
        return vcp;
    }

    /**
     * Get the starting Julian date for this volume
     * @return Modified Julian date referenced from 1/1/70.
     */
    public int getJulianDate() {
        return timeSinceEpoch;
    }

    /**
     * Get the starting time in seconds since midnight.
     * @return Generation time of data in milliseconds of day past
     *         midnight (UTC).
     */
    public int getSecsSinceMidnight() {
        return millis;
    }


    /**
     * Get the DataInput for this this data
     *
     * @return the DataInput
     */
    public ucar.netcdf.RandomAccessFile getDataInput() {
        return din;
    }


    /**
     * Get the station ID for this data
     *
     * @return station ID (may be null)
     */
    public String getStationId() {
        return stationId;
    }

    /**
     * Get the data type (ARCHIVE2, AR2V0001) for this file.
     *
     * @return data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Get the filename that this is using for input
     *
     * @return filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Test the class.
     *
     * @param args file to read
     *
     * @throws IOException problem reading file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Must supply a filename");
            System.exit(1);
        }
        Level2Data data = new Level2Data(args[0], null);
        data.read(0, true);
        System.out.println("Reading from file " + data.getFilename());
        System.out.println("\tVCP = " + data.getVCP());
        System.out.println("\trecords = " + data.numberOfRecords());
        System.out.println("\tdate = " + data.getJulianDate());
        System.out.println("\ttime = " + data.getSecsSinceMidnight());
        System.out.println("\tnumber of cuts = " + data.getNumberOfCuts());
        System.out.println("\tnumber of true cuts = "
                           + data.getNumberOfTrueCuts());
        Level2Record level2Record = new Level2Record();
        int          k            = 0;
        for (int i = 0; i < data.getNumberOfCuts(); i++) {
            int l = data.getCutStart(i);
            level2Record.readHeader(data.getDataInput(), l);
            if (level2Record.surv_bins > 0) {
                k++;
                int j = (int) (data.getTrueElevation(k - 1) * 100F);
                //float f = data.getTrueElevation(k - 1);
                float f = (float) j / 100F;
                System.out.println(" angle for cut[" + i + "] = " + f);
            }
        }
    }
}
