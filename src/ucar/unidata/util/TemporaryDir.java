/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for Atmospheric Research
 * Copyright 2010- Jeff McWhirter
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
 * 
 */

package ucar.unidata.util;


import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.zip.*;


/**
 *
 * @version $Revision: 1.52 $
 */
public class TemporaryDir {

    /** _more_          */
    private File dir;

    /** _more_          */
    private long currentDirTime = 0;

    /** _more_          */
    private int maxFiles = -1;

    /** _more_          */
    private long maxSize = -1;

    /** _more_          */
    private long maxAge = -1;

    /** _more_          */
    private boolean touched = false;

    /**
     * _more_
     *
     * @param dir _more_
     */
    public TemporaryDir(String dir) {
        this(new File(dir));
    }


    /**
     * _more_
     *
     * @param dir _more_
     */
    public TemporaryDir(File dir) {
        this.dir = dir;
    }

    /**
     * _more_
     *
     * @param dir _more_
     * @param maxFiles _more_
     * @param maxSize _more_
     * @param maxAge _more_
     */
    public TemporaryDir(File dir, int maxFiles, long maxSize, long maxAge) {
        this(dir);
        this.maxFiles = maxFiles;
        this.maxSize  = maxSize;
        this.maxAge   = maxAge;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveChanged() {
        return currentDirTime != dir.lastModified();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return dir.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<File> findFilesToScour() {
        List<File> results  = new ArrayList<File>();

        long       t1       = System.currentTimeMillis();
        List<File> allFiles = IOUtil.getFiles(dir, true);
        //        System.err.println ("all:" + allFiles);
        long t2 = System.currentTimeMillis();

        long t3 = System.currentTimeMillis();
        //Sort files oldest first
        IOUtil.FileWrapper[] files =
            IOUtil.sortFilesOnAge(IOUtil.FileWrapper.toArray(allFiles,
                false));
        long t4        = System.currentTimeMillis();

        long now       = new Date().getTime();

        long totalSize = 0;
        int  numFiles  = 0;
        for (int i = 0; i < files.length; i++) {
            //            if(files[i].isDirectory()) {
            //                continue;
            //            } 
            numFiles++;
        }

        if (maxSize > 0) {
            for (int i = 0; i < files.length; i++) {
                totalSize += files[i].length();
            }
        }


        if (files.length > 0) {
            //            System.err.println ("    found " + files.length +" in " + (t2-t1) +" size:" + totalSize);
        }

        //        System.err.println("max age:" + maxAge);
        for (int i = 0; i < files.length; i++) {
            //            System.err.println("\tfile:" + files[i]);
            //            if(files[i].isDirectory()) {
            //                continue;
            //            } 
            boolean shouldScour = false;
            if ((maxSize > 0) && (totalSize > maxSize)) {
                shouldScour = true;
            }
            if (maxAge > 0) {
                long lastModified = files[i].lastModified();
                long age          = now - lastModified;
                //                System.err.println("\tage:" + age);
                if (age > maxAge) {
                    shouldScour = true;
                }
            }
            if (maxFiles > 0) {
                if (numFiles > maxFiles) {
                    shouldScour = true;
                }
            }

            if ( !shouldScour) {
                break;
            }
            long fileSize = files[i].length();
            results.add(files[i].getFile());
            totalSize -= files[i].length();
            numFiles--;
        }

        if (results.size() > 0) {
            //            System.err.println ("    found " + results.size() + " files to delete");
        }
        currentDirTime = dir.lastModified();
        return results;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public File getDir() {
        return dir;
    }


    /**
     *  Set the MaxFiles property.
     *
     *  @param value The new value for MaxFiles
     */
    public void setMaxFiles(int value) {
        this.maxFiles = value;
    }

    /**
     *  Get the MaxFiles property.
     *
     *  @return The MaxFiles
     */
    public int getMaxFiles() {
        return this.maxFiles;
    }

    /**
     *  Set the MaxBytes property.
     *
     *  @param value The new value for MaxBytes
     */
    public void setMaxSize(long value) {
        this.maxSize = value;
    }

    /**
     *  Get the MaxBytes property.
     *
     *  @return The MaxBytes
     */
    public long getMaxSize() {
        return this.maxSize;
    }

    /**
     *  Set the MaxAge property.
     *
     *  @param value The new value for MaxAge
     */
    public void setMaxAge(long value) {
        this.maxAge = value;
    }

    /**
     *  Get the MaxAge property.
     *
     *  @return The MaxAge
     */
    public long getMaxAge() {
        return this.maxAge;
    }

    /**
     * Set the Touched property.
     *
     * @param value The new value for Touched
     */
    public void setTouched(boolean value) {
        touched = value;
    }

    /**
     * Get the Touched property.
     *
     * @return The Touched
     */
    public boolean getTouched() {
        return touched;
    }



}
