/*
 * $Id: WorldWindReader.java,v 1.7 2005/03/10 18:38:29 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.gis;


import ucar.unidata.util.Misc;
import ucar.unidata.util.IOUtil;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.io.BeLeDataInputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Class WorldWindReader _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.7 $
 */
public class WorldWindReader {

    /** _more_          */
    byte[] stringBuff = new byte[10000];



    /**
     * _more_
     *
     * @param wwps _more_
     * @param rect _more_
     *
     * @return _more_
     */
    public List findWwps(List wwps, Rectangle2D.Double rect) {
        List   names  = new ArrayList();
        double height = Math.min(180.0, rect.getHeight());
        for (int i = 0; i < wwps.size(); i++) {
            Object[] a     = (Object[]) wwps.get(i);
            double[] range = (double[]) a[1];
            //      System.err.println (a[0] + " -- " + range[0] + " " + range[1]);
            if ((height > range[1]) || (height < range[0])) {
                continue;
            }
            Rectangle2D.Double wwpRect = (Rectangle2D.Double) a[2];
            if (wwpRect.intersects(rect) || rect.contains(wwpRect)) {
                names.add(a[0]);
            }
        }
        return names;
    }


    /**
     * _more_
     *
     * @param bdis _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public String readString(BeLeDataInputStream bdis) throws Exception {
        int size = (int) bdis.readByte();
        bdis.readFully(stringBuff, 0, size);
        return new String(stringBuff, 0, size);
    }

    /**
     * _more_
     *
     * @param bdis _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public byte[] readStringAsBytes(BeLeDataInputStream bdis)
            throws Exception {
        int    size = (int) bdis.readByte();
        byte[] buff = new byte[size];
        bdis.readFully(buff, 0, size);
        return buff;
    }

    /**
     * _more_
     *
     * @param fileOrUrl _more_
     * @param minRange _more_
     * @param maxRange _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public List readWPL(String fileOrUrl, double minRange, double maxRange)
            throws Exception {
        String dirPath = IOUtil.getFileRoot(fileOrUrl);
        List   entries = new ArrayList();
        byte[] bytes   = IOUtil.readBytesAndCache(fileOrUrl, "WorldWind");
        BeLeDataInputStream bdis =
            new BeLeDataInputStream(new ByteArrayInputStream(bytes));
        int numEntries = bdis.readLEInt();
        for (int i = 0; i < numEntries; i++) {
            String name = readString(bdis);
            float  lon1 = bdis.readLEFloat();
            float  lat1 = bdis.readLEFloat();
            float  lon2 = bdis.readLEFloat();
            float  lat2 = bdis.readLEFloat();
            name = dirPath + "/" + name;
            //            System.err.println ("name=" + name);
            entries.add(new Object[]{ name,
                                      new double[]{ minRange, maxRange },
                                      new Rectangle2D.Double(lon1, lat1,
                                      lon2 - lon1, lat2 - lat1) });
        }
        return entries;
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public FeatureList readWWP(String file) throws Exception {
        return readWWP(file, false);
    }


    /** _more_          */
    DataOutputStream dos = null;

    /**
     * _more_
     *
     * @param file _more_
     * @param convert _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public FeatureList readWWP(String file, boolean convert)
            throws Exception {
        dos = null;
        boolean isBlf = false;
        if (file.startsWith("http:")) {
            file  = IOUtil.stripExtension(file) + ".zip";
            isBlf = true;
        }
        byte[] fileBuffer = IOUtil.readBytesAndCache(file, "WorldWind", true);
        //        System.err.println (fileBuffer.length +" file:" + file);
        InputStream is = new ByteArrayInputStream(fileBuffer, 0,
                                                  fileBuffer.length);
        return readWWP(is, isBlf);
    }


    /**
     * _more_
     *
     * @param inputStream _more_
     * @param isBlf _more_
     *
     * @return _more_
     *
     * @throws Exception On badness 
     */
    public FeatureList readWWP(InputStream inputStream, boolean isBlf)
            throws Exception {
        BeLeDataInputStream bdis = new BeLeDataInputStream(inputStream);
        int                 numFeatures = (isBlf
                                           ? bdis.readInt()
                                           : bdis.readLEInt());
        if (isBlf) {
            //Has altitude
            bdis.readByte();
            //Has attributes
            bdis.readByte();
        }
        byte[][] names    = new byte[numFeatures][];
        double[] lats     = new double[numFeatures];
        double[] lons     = new double[numFeatures];
        boolean  skipMeta = true;
        if (dos != null) {
            dos.writeInt(numFeatures);
            //Contains altitude
            dos.writeByte(0);
            //contains attributes
            dos.writeByte(0);
        }
        for (int i = 0; i < numFeatures; i++) {
            float alt = 0.0f;
            names[i] = readStringAsBytes(bdis);
            if (isBlf) {
                lats[i] = bdis.readFloat();
                lons[i] = bdis.readFloat();
            } else {
                lats[i] = bdis.readLEFloat();
                lons[i] = bdis.readLEFloat();
            }
            int numEntries = bdis.readLEInt();
            for (int entryIdx = 0; entryIdx < numEntries; entryIdx++) {
                if (skipMeta) {
                    bdis.skip((int) bdis.readByte());
                    bdis.skip((int) bdis.readByte());
                } else {
                    String name  = readString(bdis);
                    String value = readString(bdis);
                }
            }
            if (dos != null) {
                dos.writeByte(names[i].length);
                dos.write(names[i], 0, names[i].length);
                dos.writeFloat((float) lats[i]);
                dos.writeFloat((float) lons[i]);
                dos.writeFloat((float) alt);
            }
        }
        if (dos != null) {
            dos.close();
        }
        bdis.close();
        return new FeatureList(names, lats, lons);
    }


    /**
     * Class FeatureList _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    public class FeatureList {

        /** _more_          */
        public double[] lats;

        /** _more_          */
        public double[] lons;

        /** _more_          */
        public byte[][] names;

        /**
         * _more_
         *
         * @param names _more_
         * @param lats _more_
         * @param lons _more_
         */
        public FeatureList(byte[][] names, double[] lats, double[] lons) {
            this.lats  = lats;
            this.lons  = lons;
            this.names = names;
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception On badness 
     */
    public static void main(String[] args) throws Exception {
        long            t1     = System.currentTimeMillis();
        WorldWindReader reader = new WorldWindReader();
        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".wpl")) {
                List wwps = reader.readWPL(args[i], 0, 5);
                List names = reader.findWwps(wwps,
                                             new Rectangle2D.Double(-110, 35,
                                                 5, 5));

                for (int nameIdx = 0; nameIdx < names.size(); nameIdx++) {
                    System.err.println("WWP:" + names.get(nameIdx));
                    reader.readWWP(names.get(nameIdx).toString());
                }
            } else {
                reader.readWWP(args[i], false);
            }
        }
        long t2 = System.currentTimeMillis();
        System.err.println(" time:" + (t2 - t1));
    }

}

