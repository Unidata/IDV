/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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



package ucar.unidata.repository;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Class FileInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class FileInfo {

    /** _more_ */
    File file;

    /** _more_ */
    long time;

    /** _more_ */
    long size=0;

    /** _more_ */
    boolean hasInitialized = false;

    boolean isDir;

    /**
     * _more_
     *
     * @param f _more_
     */
    public FileInfo(File f) {
        this(f, f.isDirectory());
    }

    public FileInfo(File f, boolean isDir) {
        this.isDir = isDir;
        file = f;
        time           = file.lastModified();
        if(!isDir)
            size           = file.length();
        hasInitialized = true;
    }

    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof FileInfo)) {
            return false;
        }
        FileInfo that = (FileInfo) obj;
        return Misc.equals(this.file.toString(), that.file.toString());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasChanged() {
        if ( !hasInitialized) {
            if (file.exists()) {
                time           = file.lastModified();
                if(!isDir)
                    size           = file.length();
                hasInitialized = true;
                return true;
            } else {
                return false;
            }
        }
        long    newTime = file.lastModified();
        long    newSize = (isDir?0:file.length());
        boolean changed = (newTime != time) || (newSize != size);
        time = newTime;
        size = newSize;
        return changed;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getFile() {
        return file;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return file.toString();
    }

    public static List<FileInfo> collectDirs(File rootDir) throws  Exception {
        final List<FileInfo> dirs    = new ArrayList();
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                if (f.isDirectory()) {
                    if(f.getName().startsWith(".")) return DO_DONTRECURSE;
                    dirs.add(new FileInfo(f,true));
                }
                return DO_CONTINUE;
            }
        };
        IOUtil.walkDirectory(rootDir, fileViewer);
        return dirs;
    }


    public static void main(String[]args) throws Exception {
        File rootDir =
            new File(
                "c:/cygwin/home/jeffmc/unidata");
        if(!rootDir.exists()) {
            rootDir =
                new File(
                         "/data/ldm/gempak/nexrad/NIDS");            
        }
        if(args.length>0) 
            rootDir = new File(args[0]);
        final List<FileInfo> dirs    = new ArrayList();
        final int[]cnt = {0};
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                cnt[0]++;
                if(cnt[0]%1000==0) System.err.print(".");
                if (f.isDirectory()) {
                    dirs.add(new FileInfo(f,true));
                    //    if(dirs.size()%1000==0) System.err.print(".");
                }
                return DO_CONTINUE;
            }
        };

        long tt1= System.currentTimeMillis();
        IOUtil.walkDirectory(rootDir, fileViewer);
        long tt2= System.currentTimeMillis();
        System.err.println("found:" + dirs.size() + " in:" + (tt2-tt1) + " looked at:" + cnt[0] );

        while(true) {
            long t1 = System.currentTimeMillis();
            for(FileInfo fileInfo: dirs) {
                long oldTime = fileInfo.time;
                if(fileInfo.hasChanged()) {
                    System.err.println ("Changed:" + fileInfo);
                    File[]files = fileInfo.file.listFiles();
                    for(int i=0;i<files.length;i++) {
                        if(files[i].lastModified()> oldTime) {
                            System.err.println ("    " + files[i].getName());
                        }

                    }
                    

                }
            }
            long t2= System.currentTimeMillis();
            //            System.err.println ("Time:" + (t2-t1));
            Misc.sleep(5000);
        }
    }

}
