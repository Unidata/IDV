/*
 * $Id: FilePoller.java,v 1.30 2006/05/05 19:19:34 jeffmc Exp $
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


package ucar.unidata.util;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import java.io.File;

import java.util.ArrayList;
import java.util.List;



import javax.swing.filechooser.FileFilter;


/**
 * Class for handling polling of files
 *
 * @author IDV development team
 *
 * @version $Revision: 1.8
 */
public class FilePoller extends Poller {

    /** Holds polling information */
    PollingInfo pollingInfo;

    /** Polling filter */
    PatternFileFilter fileFilter;


    /** _more_ */
    List fileInfos = new ArrayList();

    /** _more_ */
    File directory;

    /** _more_ */
    boolean haveInitialized = false;

    /**
     * Create a new file poller
     *
     * @param listener    the listener for the polling info
     * @param info        polling info
     */
    public FilePoller(ActionListener listener, PollingInfo info) {
        super(listener, info.getInterval());
        this.pollingInfo = (PollingInfo) info.cloneMe();
        init();
    }


    /**
     * Initialize the class
     */
    public void init() {
        if (pollingInfo.getFilePattern() != null) {
            this.fileFilter =
                new PatternFileFilter(pollingInfo.getFilePattern(), false,
                                      pollingInfo.getIsHiddenOk());
            Trace.msg("FilePoller.init filter=" + fileFilter);
        }
        List paths = pollingInfo.getFilePaths();
        for (int i = 0; i < paths.size(); i++) {
            File f = new File(paths.get(i).toString());
            fileInfos.add(new FileInfo(f));
        }
        if ((fileInfos.size() == 1)
                && ((FileInfo) fileInfos.get(0)).getFile().isDirectory()) {
            directory = ((FileInfo) fileInfos.get(0)).getFile();
            fileInfos = getFileInfos();
        }
        super.init();
    }




    /**
     * Poll
     */
    protected void doPoll() {
        if (directory != null) {
            Trace.msg("FilePoller: Polling directory:" + directory);
            doDirectory();
            return;
        }

        Trace.msg("FilePoller: Polling file:" + fileInfos);
        doFile();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private List getFileInfos() {
        File[] list  = directory.listFiles((java.io.FileFilter) fileFilter);
        List   files = new ArrayList();
        if (list == null) {
            return files;
        }
        for (int i = 0; i < list.length; i++) {
            files.add(new FileInfo(list[i]));
        }
        return files;
    }


    /**
     * Poll a directory
     */
    private void doDirectory() {
        Trace.call1("FilePoller.doDirectory");
        List newFileInfos = getFileInfos();
        List changedFiles = new ArrayList();
        for (int i = 0; i < newFileInfos.size(); i++) {
            FileInfo newFileInfo = (FileInfo) newFileInfos.get(i);
            int      index       = fileInfos.indexOf(newFileInfo);
            FileInfo oldFileInfo = null;
            if (index >= 0) {
                oldFileInfo = (FileInfo) fileInfos.get(index);
                if ((oldFileInfo.size != newFileInfo.size)
                        || (oldFileInfo.time != newFileInfo.time)) {
                    changedFiles.add(newFileInfo.getFile());
                }
            } else {
                changedFiles.add(newFileInfo.getFile());
            }
        }

        fileInfos = newFileInfos;
        Trace.call2("FilePoller.doDirectory",
                    " changed files=" + changedFiles);
        if (changedFiles.size() == 0) {
            return;
        }
        waitOnFiles(changedFiles);
        fireChange(changedFiles);
    }

    /**
     * _more_
     *
     * @param files _more_
     */
    private void fireChange(List files) {
        if (listener != null) {
            listener.actionPerformed(new ActionEvent(files, 1,
                    "FILECHANGED"));
        }
    }


    /**
     * Poll a file
     */
    private void doFile() {
        List files = new ArrayList();
        for (int i = 0; i < fileInfos.size(); i++) {
            FileInfo fileInfo = (FileInfo) fileInfos.get(i);
            if (fileInfo.hasChanged()) {
                files.add(fileInfo.getFile());
            }
        }
        if (files.size() == 0) {
            return;
        }
        waitOnFiles(files);
        if ( !running) {
            return;
        }
        fireChange(files);
    }

    /**
     *  This goes into a wait loop, sleeping interval/10
     *  for a maximum of interval time or until the length
     *  of the given files has stopped changing.
     *
     * @param files the files to wait on
     */
    private void waitOnFiles(List files) {
        long[] lengths   = new long[files.size()];
        File[] fileArray = new File[files.size()];
        for (int i = 0; i < files.size(); i++) {
            lengths[i]   = ((File) files.get(i)).length();
            fileArray[i] = (File) files.get(i);
        }
        int cnt = 5;
        //        long subInterval = getInterval() / cnt;
        long subInterval = 1000;
        while (cnt-- > 0) {
            Misc.sleep(subInterval);
            if ( !running) {
                return;
            }
            boolean allOk = true;
            for (int i = 0; i < fileArray.length; i++) {
                if (fileArray[i] == null) {
                    continue;
                }
                long newLength = fileArray[i].length();
                if (newLength == lengths[i]) {
                    fileArray[i] = null;
                } else {
                    allOk = false;
                }
            }
            if (allOk) {
                break;
            }
        }
    }


    /**
     * Class FileInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.30 $
     */
    private static class FileInfo {

        /** _more_ */
        File file;

        /** _more_ */
        long time;

        /** _more_ */
        long size;

        /** _more_ */
        boolean hasInitialized = false;

        /**
         * _more_
         *
         * @param f _more_
         */
        public FileInfo(File f) {
            file = f;
            if (file.exists()) {
                time           = file.lastModified();
                size           = file.length();
                hasInitialized = true;
            }
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
                    size           = file.length();
                    hasInitialized = true;
                    return true;
                } else {
                    return false;
                }
            }
            long    newTime = file.lastModified();
            long    newSize = file.length();
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

    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        FileInfo fileInfo = new FileInfo(new File("util"));
        System.err.println("hasChanged:" + fileInfo.hasChanged() + " size="
                           + fileInfo.size);
    }



}

