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


import ucar.netcdf.RandomAccessFile;

import java.io.EOFException;
import java.io.IOException;


/**
 * This class reads a specified record a NEXRAD level II file
 * and provides methods to retrieve selected data from it.
 * Will probably change in the future.
 *
 * Adapted with permission from the Java Iras software developed
 * by David Priegnitz at NSSL.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.13 $ $Date: 2006/12/01 20:42:38 $
 */
public class Level2Record {

    /** Size of the record header */
    public static final int RECORD_HEADER_SIZE = 12;

    /** Size of the file header */
    public static final int FILE_HEADER_SIZE = 24;

    /** Size of the radar data */
    public static final int RADAR_DATA_SIZE = 2432;

    /** Reflectivity moment identifier */
    public static final int REFLECTIVITY = 0;

    /** Radial Velocity moment identifier */
    public static final int VELOCITY = 1;

    /** Sprectrum Width moment identifier */
    public static final int SPECTRUM_WIDTH = 2;

    /** Value for signal below threshold */
    public static final int SIGNAL_BELOW_THRESHOLD = 999;

    /** Value for overlaid signal */
    public static final int SIGNAL_OVERLAID = 998;

    /** Value for data not fount */
    public static final int DATA_NOT_FOUND = 997;

    /** Low doppler resolution code */
    public static final int DOPPLER_RESOLUTION_LOW_CODE = 4;

    /** High doppler resolution code */
    public static final int DOPPLER_RESOLUTION_HIGH_CODE = 2;

    /** Low resolution value */
    public static final float DOPPLER_RESOLUTION_LOW = (float) 1.0;

    /** High resolution value */
    public static final float DOPPLER_RESOLUTION_HIGH = (float) 0.5;

    /** Maximum radials in a cut */
    public static final int MAX_RADIALS_IN_CUT = 500;

    /** Horizontal beam width */
    public static final float HORIZONTAL_BEAM_WIDTH = (float) 1.5;

    /** Kilometer to nautical mile conversion */
    public static final float KM_PER_NM = (float) 1.94;

    /** Initialization flag for lookup tables */
    public static int data_lut_init_flag = 0;

    /** Reflectivity look up table */
    public static float[] Reflectivity_LUT = new float[256];

    /** 1 km Velocity look up table */
    public static float[] Velocity_1km_LUT = new float[256];

    /** 1/2 km Velocity look up table */
    public static float[] Velocity_hkm_LUT = new float[256];

    static {

        int i;

        Reflectivity_LUT[0] = Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
        Reflectivity_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;
        Velocity_1km_LUT[0] = Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
        Velocity_1km_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;
        Velocity_hkm_LUT[0] = Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
        Velocity_hkm_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;

        for (i = 2; i < 256; i++) {

            Reflectivity_LUT[i] = (float) (i / 2.0 - 33.0);
            Velocity_1km_LUT[i] = (float) (i - 129.0);
            Velocity_hkm_LUT[i] = (float) (i / 2.0 - 64.5);

        }
    }


    /** end of file flag */
    int eof_flag = 0;

    /** message size */
    short message_size = 0;

    /** channel */
    byte channel = 0;

    /** message type */
    byte message_type = 0;

    /** sequence id */
    short id_sequence = 0;

    /** julian date */
    short julian_date = 0;

    /** milliseconds since midnight */
    int milliseconds = 0;

    /** number of segments */
    short number_segs = 0;

    /** segment number */
    short seg_number = 0;

    /** time */
    int time = 0;

    /** julian day */
    short julian = 0;

    /** unambiguous range */
    short unamb_range = 0;

    /** azimuth angel */
    int azimuth_ang = 0;

    /** azimuth number */
    short azimuth_num = 0;

    /** radial status */
    short radial_status = 0;

    /** elevation angle */
    short elevation_ang = 0;

    /** elevation number */
    short elevation_num = 0;

    /** first bin */
    short first_bin = 0;

    /** doppler range */
    short doppler_range = 0;

    /** survey size */
    short surv_size = 0;

    /** doppler size */
    short dopl_size = 0;

    /** survey bins */
    short surv_bins = 0;

    /** doppler bins */
    short dopl_bins = 0;

    /** cut number */
    short cut = 0;

    /** calibration value */
    float calibration = 0;

    /** survey pointer */
    short surv_pointer = 0;

    /** velocity pointer */
    short vel_pointer = 0;

    /** spectrum width pointer */
    short spw_pointer = 0;

    /** resolution */
    short resolution = 0;

    /** volume coverage pattern */
    short vcp = 0;

    /** bin values */
    byte[][] bins = new byte[MAX_RADIALS_IN_CUT][2400];

    /** Default constructor; does nothing */
    public Level2Record() {}

    /**
     * Read the angles from the file
     *
     * @param din       file to read from
     * @param record    record to read
     */
    public void readAngles(RandomAccessFile din, int record) {

        try {

            int  count  = 0;
            long offset = 0;
            int  i;

            offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
            din.seek(offset);

            din.skipBytes(RECORD_HEADER_SIZE);

            message_size = din.readShort();
            channel      = din.readByte();
            message_type = din.readByte();

            din.skipBytes(20);

            azimuth_ang = (int) din.readUnsignedShort();

            din.skipBytes(4);

            elevation_ang = din.readShort();
            elevation_num = din.readShort();
            /*
                    din.skipBytes (2382);
            */
        } catch (EOFException e) {

            eof_flag = 1;
            /*
                    System.out.println ("End-of-file exception caught");
            */
        } catch (IOException e) {

            System.err.println(e);

        }
    }

    /**
     * Method to read a selected header from a specified level II file.
     *
     * @param din        file to read from
     * @param record     record to read
     */
    public void readHeader(RandomAccessFile din, int record) {

        try {

            int  count  = 0;
            long offset = 0;
            int  i;

            offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
            din.seek(offset);

            din.skipBytes(RECORD_HEADER_SIZE);

            message_size  = din.readShort();
            channel       = din.readByte();
            message_type  = din.readByte();
            id_sequence   = din.readShort();
            julian_date   = din.readShort();
            milliseconds  = din.readInt();
            number_segs   = din.readShort();
            seg_number    = din.readShort();
            time          = din.readInt();
            julian        = din.readShort();
            unamb_range   = din.readShort();
            azimuth_ang   = (int) din.readUnsignedShort();
            azimuth_num   = din.readShort();
            radial_status = din.readShort();
            elevation_ang = din.readShort();
            elevation_num = din.readShort();
            first_bin     = din.readShort();
            doppler_range = din.readShort();
            surv_size     = din.readShort();
            dopl_size     = din.readShort();
            surv_bins     = din.readShort();
            dopl_bins     = din.readShort();
            cut           = din.readShort();
            calibration   = din.readFloat();
            surv_pointer  = din.readShort();
            vel_pointer   = din.readShort();
            spw_pointer   = din.readShort();
            resolution    = din.readShort();
            vcp           = din.readShort();

            din.skipBytes(14);

            short nyquist_vel   = din.readShort();
            short attenuation   = din.readShort();
            short tover         = din.readShort();
            short spot_blanking = din.readShort();

            din.skipBytes(2332);

        } catch (EOFException e) {

            eof_flag = 1;
            /*
                    System.out.println ("End-of-file exception caught");
            */
        } catch (IOException e) {

            System.err.println(e);

        }
    }


    /**
     * Method to read a selected record from a specified level II file.
     *
     * @param din            file to read from
     * @param record         record to read
     */
    public void readRecord(RandomAccessFile din, int record) {

        try {

            int  count  = 0;
            long offset = 0;
            int  i;

            offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
            din.seek(offset);

            din.skipBytes(RECORD_HEADER_SIZE);

            message_size  = din.readShort();
            channel       = din.readByte();
            message_type  = din.readByte();

            id_sequence   = din.readShort();
            julian_date   = din.readShort();
            milliseconds  = din.readInt();
            number_segs   = din.readShort();
            seg_number    = din.readShort();
            time          = din.readInt();
            julian        = din.readShort();
            unamb_range   = din.readShort();
            azimuth_ang   = (int) din.readUnsignedShort();
            azimuth_num   = din.readShort();
            radial_status = din.readShort();
            elevation_ang = din.readShort();
            elevation_num = din.readShort();
            first_bin     = din.readShort();
            doppler_range = din.readShort();
            surv_size     = din.readShort();
            dopl_size     = din.readShort();
            surv_bins     = din.readShort();
            dopl_bins     = din.readShort();
            cut           = din.readShort();
            calibration   = din.readFloat();
            surv_pointer  = din.readShort();
            vel_pointer   = din.readShort();
            spw_pointer   = din.readShort();
            resolution    = din.readShort();
            vcp           = din.readShort();

            din.skipBytes(14);

            short nyquist_vel   = din.readShort();
            short attenuation   = din.readShort();
            short tover         = din.readShort();
            short spot_blanking = din.readShort();

            din.skipBytes(32);

            din.readFully(bins[0], 0, 2304);

        } catch (EOFException e) {

            eof_flag = 1;
            /*
                    System.out.println ("End-of-file exception caught");
            */
        } catch (IOException e) {

            System.err.println(e);

        }
    }


    /**
     * Method to read a selected cut from a specified level II file.
     *
     * @param din          file to read from
     * @param record       record to read
     *
     * @return  number of radials in the cut
     */
    public int readCut(RandomAccessFile din, int record) {

        int i = 0;
        int j = 0;
        int range;
        int max;

        try {

            int   count             = 0;
            long  offset            = 0;
            short old_elevation_num = 99;

            // Read the header of the first radial in the cut so we can update
            // the data pointers. 


            readRecord(din, record);

            /*
            offset = record*RADAR_DATA_SIZE + FILE_HEADER_SIZE;
            din.seek (offset);
            */

            old_elevation_num = 999;

            for (i = 0; i < MAX_RADIALS_IN_CUT; i++) {

                offset = (record + i) * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
                din.seek(offset);

                din.skipBytes(RECORD_HEADER_SIZE + 3);

                message_type = din.readByte();

                if (message_type == 1) {

                    din.skipBytes(28);
                    elevation_num = din.readShort();

                    if (elevation_num > old_elevation_num) {

                        //System.out.println (i+" radials read in cut "+old_elevation_num);
                        break;

                    } else {

                        old_elevation_num = elevation_num;
                        din.skipBytes(82);

                        din.readFully(bins[i], 0, 2304);

                    }

                } else {

                    //System.out.println ("Message type "+message_type+" found");

                }
            }

            return i;

        } catch (EOFException e) {

            eof_flag = 1;

            // System.out.println ("End-of-file exception caught");

            return i;

        } catch (IOException e) {

            System.err.println(e);
            return i;

        }
    }

    /**
     * Get the azimuth for the current read
     *
     * @return   azimuth value
     */
    public float getAzimuth() {

        if (message_type != 1) {

            return ((float) -1);

        } else {

            return (((float) 180.0) * ((float) azimuth_ang)
                    / ((float) 32768));

        }
    }

    /**
     * Get the elevation angle for the current read
     *
     * @return  elevation angle
     */
    public float getElevation() {

        return (((float) 180.0) * ((float) elevation_ang) / ((float) 32768));

    }

    /**
     * Get the elevation number for the current read
     *
     * @return  elevation number
     */
    public short getElevationNum() {

        return (elevation_num);

    }

    /**
     * Get the message type for the current read
     *
     * @return  message type
     */
    public short getMessageType() {

        return (message_type);

    }

    /**
     * Get the Volume Coverage Pattern (VCP) for this record
     *
     * @return   volume coverage pattern
     */
    public short getVCP() {

        return (vcp);

    }

    /**
     * Get the end-of-file flag
     *
     * @return  EOF flag
     */
    public int eof() {

        return (eof_flag);

    }

    /**
     * This method returns the radial bin size (meters) for
     * the specified moment in a read level II record.
     *
     * @param moment   moment id
     *
     * @return   radial bin size
     */
    public int getBinSize(int moment) {

        switch (moment) {

          case REFLECTIVITY :

              return ((int) surv_size);

          case VELOCITY :
          case SPECTRUM_WIDTH :

              return ((int) dopl_size);

        }

        return (0);
    }

    /**
     * This method returns the number of bins defined for
     * the specified moment in a read level II record.
     *
     * @param moment   moment id
     *
     * @return  number of bins for this moment
     */
    public int getBinNum(int moment) {

        switch (moment) {

          case REFLECTIVITY :

              return ((int) surv_bins);

          case VELOCITY :
          case SPECTRUM_WIDTH :

              return ((int) dopl_bins);

        }

        return (0);
    }

    /**
     * This method returns a bin data element from the specified read
     * level II record.
     *
     * @param moment    moment id
     * @param radial    radial index
     * @param bin       bin index
     * @return   bin data element
     */
    public int getBinData(int moment, int radial, int bin) {

        int value;

        value = DATA_NOT_FOUND;

        switch (moment) {

          case REFLECTIVITY :

              value = (bins[radial][(int) (surv_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (surv_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (surv_pointer - 100 + bin)];
              break;

          case VELOCITY :

              value = (bins[radial][(int) (vel_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (vel_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (vel_pointer - 100 + bin)];
              break;

          case SPECTRUM_WIDTH :

              value = (bins[radial][(int) (spw_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (spw_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (spw_pointer - 100 + bin)];
              break;

        }

        return (value);

    }

    /**
     * This method returns a bin data value from the specified
     * read level II record.
     *
     * @param moment   moment ID
     * @param radial   radial index
     * @param bin      bin index
     *
     * @return   bin data value
     */
    public float getBinValue(int moment, int radial, int bin) {

        int value;

        switch (moment) {

          case REFLECTIVITY :

              value = (bins[radial][(int) (surv_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (surv_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (surv_pointer - 100 + bin)];

              return (Reflectivity_LUT[value]);

          case VELOCITY :

              value = (bins[radial][(int) (vel_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (vel_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (vel_pointer - 100 + bin)];

              if (resolution == DOPPLER_RESOLUTION_LOW) {

                  return (Velocity_1km_LUT[value]);

              } else {

                  return (Velocity_hkm_LUT[value]);

              }

          case SPECTRUM_WIDTH :

              value = (bins[radial][(int) (spw_pointer - 100 + bin)] >= 0)
                      ? bins[radial][(int) (spw_pointer - 100 + bin)]
                      : 256 + bins[radial][(int) (spw_pointer - 100 + bin)];

              return (Velocity_hkm_LUT[value]);

        }

        return Float.NaN;
    }
}
