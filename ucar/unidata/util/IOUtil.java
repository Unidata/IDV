/*
 * $Id: IOUtil.java,v 1.52 2007/08/14 16:06:15 jeffmc Exp $
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


package ucar.unidata.util;


import java.io.*;

import java.net.*;

import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.zip.*;


/**
 * A set of io related utilities
 * @author IDV development group.
 *
 * @version $Revision: 1.52 $
 */
public class IOUtil {

    /** Holds the filename/urls that we have checked if they are html */
    private static Hashtable isHtmlCache = new Hashtable();

    /** debug flag */
    public static boolean debug = false;

    /** Default constructor; does nothing */
    public IOUtil() {}


    /**
     * Does the given file or url have the given suffix
     *
     * @param fileOrUrl The name of the file or url
     * @param suffix The suffix
     *
     * @return Does the fileOrUrl have the suffix
     */
    public static boolean hasSuffix(String fileOrUrl, String suffix) {
        fileOrUrl = fileOrUrl.toLowerCase();
        if (suffix.startsWith(".")) {
            suffix = suffix.substring(1);
        }
        if (fileOrUrl.endsWith("." + suffix)) {
            return true;
        }

        if (StringUtil.stringMatch(fileOrUrl, ".*\\." + suffix + "\\?")) {
            return true;
        }
        return false;

    }




    /**
     *  Find the youngest file in the given directory.
     *
     *  @param dir The directory to search in.
     *  @return The most recent file (or null if none found).
     */
    public static File getMostRecentFile(File dir) {
        return getMostRecentFile(dir, (java.io.FileFilter) null);
    }

    /**
     *  Find the youngest file in the given directory that matches the given {@link FileFilter}.
     *
     *  @param dir The directory to search in.
     *  @param filter The {@link FileFilter} to be used to limit what files we look at (may be null).
     *  @return The most recent file (or null if none found).
     */
    public static File getMostRecentFile(File dir,
                                         java.io.FileFilter filter) {
        if ( !dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory:" + dir);
        }

        File[] list       = ((filter == null)
                             ? dir.listFiles()
                             : dir.listFiles(filter));
        File   latestFile = null;
        long   latest     = Long.MIN_VALUE;
        for (int i = 0; i < list.length; i++) {
            long tmp = list[i].lastModified();
            if (tmp > latest) {
                latestFile = list[i];
                latest     = tmp;
            }
        }
        return latestFile;
    }



    /**
     * Sort the files contained by the given directory and that (if non-null)
     * match the given filter.
     *
     * @param directory The directory
     * @param filter The filter
     * @param youngestFirst Ascending or descending
     *
     * @return The sorted files
     */
    public static File[] sortFilesOnAge(File directory,
                                        java.io.FileFilter filter,
                                        boolean youngestFirst) {
        File[] files = ((filter == null)
                        ? directory.listFiles()
                        : directory.listFiles(filter));
        sortFilesOnAge(files, youngestFirst);
        return files;
    }


    /**
     * Wrapper for a file for doing comparisons
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.52 $
     */
    private static class FileWrapper implements Comparable {

        /** file to wrap */
        File f;

        /** last modified time */
        long modified;

        /** flag for youngest first sorting */
        boolean youngestFirst;

        /**
         * Create a FileWrapper for the file with the appropriate sorting
         *
         * @param f   File to wrap
         * @param youngestFirst flag for sorting
         */
        public FileWrapper(File f, boolean youngestFirst) {
            this.f             = f;
            this.youngestFirst = youngestFirst;
            modified           = f.lastModified();
        }

        /**
         * Compare the object
         *
         * @param o  object to compare
         *
         * @return comparison value
         */
        public int compareTo(Object o) {
            FileWrapper that = (FileWrapper) o;
            if (modified > that.modified) {
                return (youngestFirst
                        ? -1
                        : 1);
            }
            if (modified < that.modified) {
                return (youngestFirst
                        ? 1
                        : -1);
            }
            return 0;

        }
    }


    /**
     * Return an array of the file Files. That is, the ones where File.isFile is  true
     *
     * @param files Array of files
     *
     * @return The files
     */
    public static File[] getNormalFiles(File[] files) {
        List normalFiles = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                normalFiles.add(files[i]);
            }
        }
        return toFiles(normalFiles);
    }

    /**
     * Sort the given files
     *
     * @param files The files
     * @param youngestFirst Ascending or descending
     * @return Just return the given array
     */
    public static File[] sortFilesOnAge(File[] files,
                                        final boolean youngestFirst) {
        FileWrapper[] fw = new FileWrapper[files.length];
        for (int i = 0; i < fw.length; i++) {
            fw[i] = new FileWrapper(files[i], youngestFirst);
        }
        Arrays.sort(fw);
        for (int i = 0; i < fw.length; i++) {
            files[i] = fw[i].f;
        }
        return files;
    }


    /**
     * Convert the toString value of the objects in the given files list
     * to an array of File-s
     *
     * @param files List of files
     *
     * @return array of files
     */
    public static File[] toFiles(List files) {
        File[] fileArray = new File[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileArray[i] = new File(files.get(i).toString());
        }
        return fileArray;
    }



    /**
     * Create a javaio FileFilter from the filechooser package file filter.
     *
     * @param filter The filechooser file filter
     *
     * @return The javaio FileFilter.
     */
    public static java.io.FileFilter wrapFilter(
            final javax.swing.filechooser.FileFilter filter) {
        return new java.io.FileFilter() {
            public boolean accept(File f) {
                return ((filter == null)
                        ? true
                        : filter.accept(f));
            }
        };

    }



    /**
     *  Find the youngest file in the given directory that matches the given {@link FileFilter}.
     *
     *  @param dir The directory to search in.
     *  @param filter The filter to be used to limit what files we look at (may be null).
     *  @return The most recent file (or null if none found).
     */
    public static File getMostRecentFile(
            File dir, final javax.swing.filechooser.FileFilter filter) {
        return getMostRecentFile(dir, wrapFilter(filter));
    }



    /**
     * Copy the input stream to the output stream
     *
     * @param from input stream
     * @param to output
     *
     *
     * @return How may bytes were written
     * @throws IOException On badness
     */
    public static int writeTo(InputStream from, OutputStream to)
            throws IOException {
        return writeTo(from, to, null, 0);
    }



    /**
     * Write to the file from the URL stream
     *
     * @param from   URL for input
     * @param file   file for output
     * @param loadId A JobManager loadId that, if set, can be used to 
     *               stop the load
     *
     * @return number of bytes written
     *
     * @throws IOException  problem writing to file.
     */
    public static int writeTo(URL from, File file, Object loadId)
            throws IOException {
        URLConnection    connection = from.openConnection();
        InputStream      is         = connection.getInputStream();
        int              length     = connection.getContentLength();
        int              numBytes   = -1;
        FileOutputStream fos        = new FileOutputStream(file);
        try {
            int result = writeTo(is, fos, loadId, length);
            numBytes = result;
        } finally {
            try {
                fos.close();
            } catch (Exception exc) {}
            try {
                is.close();
            } catch (Exception exc) {}
            if (numBytes <= 0) {
                try {
                    file.delete();
                } catch (Exception exc) {}

            }
        }
        return numBytes;
    }


    /**
     * Copy the input stream to the output stream
     *
     *
     * @param from input stream
     * @param to output
     * @param loadId A JobManager loadId that, if set, can be used to 
     *               stop the load
     * @param length  number of bytes to write
     *
     * @return How may bytes were written
     *
     * @throws IOException On badness
     */
    public static int writeTo(InputStream from, OutputStream to,
                              Object loadId, int length)
            throws IOException {
        from = new BufferedInputStream(from, 100000);
        byte[] content = new byte[100000];
        int    total   = 0;
        while (true) {
            int howMany = from.read(content);
            if (howMany <= 0) {
                break;
            }
            total += howMany;
            if ( !JobManager.getManager().canContinue(loadId)) {
                return -1;
            }
            String msg;
            if (length > 0) {
                msg = "Transferred " + (total / 1000) + "/" + (length / 1000)
                      + " Kbytes ";
            } else {
                msg = "Transferred " + ((total) / 1000) + " Kbytes";
            }

            JobManager.getManager().setDialogLabel2(loadId, msg);

            to.write(content, 0, howMany);
        }
        return total;
    }


    /**
     * Copy the files pointed to by the urls list to the directory/file pointed to by prefx.
     * We copy prefix0.suffix,prefix1.suffix, ..., prefixN.suffix
     *
     * @param urls List of urls to copy
     * @param prefix file directory prefix
     * @param suffix suffix
     *
     *
     * @return List of new files
     * @throws IOException On badness
     */
    public static List writeTo(List urls, String prefix, String suffix)
            throws IOException {
        return writeTo(urls, prefix, suffix, null);
    }

    /**
     * Copy the files pointed to by the urls list to the directory/file pointed to by prefx.
     * We copy prefix0.suffix,prefix1.suffix, ..., prefixN.suffix
     *
     * @param urls List of urls to copy
     * @param prefix file directory prefix
     * @param suffix suffix
     * @param loadId JobManager loadId
     *
     *
     * @return List of new files
     * @throws IOException On badness
     */
    public static List writeTo(List urls, String prefix, String suffix,
                               Object loadId)
            throws IOException {
        if ( !suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        List files  = new ArrayList();
        int  total  = 0;
        List exists = new ArrayList();
        if (urls.size() == 1) {
            String path = prefix + "" + suffix;
            if ((new File(path)).exists()) {
                exists.add(path);
            }
        } else {
            for (int i = 0; i < urls.size(); i++) {
                String path = prefix + i + "" + suffix;
                if ((new File(path)).exists()) {
                    exists.add(path);
                }
            }
        }
        if (exists.size() > 0) {
            String msg;
            if (exists.size() == 1) {
                msg = "File: " + exists.get(0) + " exists.\n";
            } else {
                msg = "The following files exists:\n"
                      + StringUtil.join("\n", exists) + "\n";
            }
            if (javax.swing.JOptionPane.showConfirmDialog(null,
                    msg + "Do you want to overwrite?", "File exists",
                    javax.swing.JOptionPane.YES_NO_OPTION) == 1) {
                return null;
            }
        }

        for (int i = 0; i < urls.size(); i++) {
            String      path = ((urls.size() == 1)
                                ? prefix + suffix
                                : prefix + i + "" + suffix);
            InputStream from;
            Object      obj = urls.get(i);
            JobManager.getManager().setDialogLabel1(loadId,
                    " Writing:  " + IOUtil.getFileTail(path));
            int length = 0;
            if (obj instanceof InputStream) {
                from = (InputStream) obj;
            } else {
                URL           url        = ((obj instanceof URL)
                                            ? (URL) obj
                                            : getURL(obj.toString(),
                                                IOUtil.class));
                URLConnection connection = url.openConnection();
                try {
                    from   = connection.getInputStream();
                    length = connection.getContentLength();
                } catch (Exception exc) {
                    String msg    = "There was an error reading the data";
                    Map    fields = connection.getHeaderFields();
                    if (connection instanceof HttpURLConnection) {
                        try {
                            msg = IOUtil
                                .readContents(((HttpURLConnection) connection)
                                    .getErrorStream());
                        } catch (Exception exc2) {
                            System.err.println(exc2);
                        }
                    }
                    for (int fieldIdx = 0; fieldIdx < fields.size();
                            fieldIdx++) {
                        System.err.println(
                            connection.getHeaderFieldKey(fieldIdx) + "="
                            + connection.getHeaderField(fieldIdx));
                    }
                    LogUtil.userErrorMessage(msg);
                    return null;
                }
            }

            files.add(path);
            OutputStream to    = new FileOutputStream(path);
            int          bytes = writeTo(from, to, loadId, length);
            if (bytes <= 0) {
                return null;
            }
            total += bytes;
            JobManager.getManager().setDialogLabel2(loadId,
                    "Transferred " + ((total) / 1000) + "K bytes");
            if ( !JobManager.getManager().canContinue(loadId)) {
                return null;
            }
        }
        return files;
    }


    /**
     *  Gets the file name, removing any leading directory paths.
     *
     *  @param f The file path.
     *  @return The file name.
     */
    public static String getFileTail(String f) {
        int idx = f.lastIndexOf("/");
        if (idx < 0) {
            idx = f.lastIndexOf(File.separator);
        }
        if (idx < 0) {
            return f;
        }
        return f.substring(idx + 1);
    }


    /**
     *  Excise the filename from the given path and return the root.
     *
     *  @param f The file path.
     *  @return The file name.
     */
    public static String getFileRoot(String f) {
        int idx = f.lastIndexOf("/");
        if (idx < 0) {
            idx = f.lastIndexOf(File.separator);
        }
        if (idx < 0) {
            return ".";
            //            return f;
        }
        return f.substring(0, idx);
    }


    /**
     *  Remove any file extension from the given file name.
     *
     *  @param f The file path.
     *  @return The file name without the extension.
     *
     */
    public static String stripExtension(String f) {
        int idx = f.lastIndexOf(".");
        if (idx < 0) {
            return f;
        }
        return f.substring(0, idx);
    }


    /**
     * Remove illegal characters in the given filename
     *
     *
     * @param name The filename to be cleaned up
     * @return The cleaned up filename
     */
    public static String cleanFileName(String name) {
        return StringUtil.replaceList(name, new String[] { ":", "/", "\\",
                "?", "&" }, new String[] { "_", "_", "_", "_", "_" });
    }


    /**
     *  Return the file extension from the given file (including the ".").
     *
     *  @param f The file path.
     *  @return The file  extension or an empty string if none found.
     */
    public static String getFileExtension(String f) {
        int idx = f.lastIndexOf(".");
        //Check for the case of http://www.foo.bar/
        int slashIdx = f.lastIndexOf("/");
        if (idx < slashIdx) {
            return "";
        }
        if (idx < 0) {
            return "";
        }
        String ext = f.substring(idx).toLowerCase();
        idx = ext.indexOf("?");
        if (idx >= 0) {
            ext = ext.substring(0, idx);
        }
        return ext;
    }



    /**
     * Write out a file to the filename specified.
     *
     * @param filename  filename to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     */
    public static void writeFile(String filename, String contents)
            throws FileNotFoundException, IOException {
        writeFile(new File(filename), contents);
    }


    /**
     * Write out a file to the {@link File} specified.
     *
     * @param filename  File to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     */
    public static void writeFile(File filename, String contents)
            throws FileNotFoundException, IOException {
        writeBytes(filename, contents.getBytes());
    }


    /**
     * Write out a file to the {@link File} specified.
     *
     * @param filename  File to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     */
    public static void writeBytes(File filename, byte[] contents)
            throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(filename);
        out.write(contents);
        out.flush();
        out.close();
    }



    /**
     * Move the from file to the to file
     *
     * @param from File to move
     * @param to The destination
     *
     *
     * @throws FileNotFoundException When we cannot find the file
     * @throws IOException When something untoward happens
     */
    public static void moveFile(File from, File to)
            throws FileNotFoundException, IOException {
        if (to.isDirectory()) {
            to = new File(joinDir(to, getFileTail(from.toString())));
        }
        copyFile(from, to);
        from.delete();
    }



    /**
     * Copy from file to to file
     *
     * @param from File to copy
     * @param to The destination
     *
     * @throws FileNotFoundException When we cannot find the file
     * @throws IOException When something untoward happens
     */
    public static void copyFile(File from, File to)
            throws FileNotFoundException, IOException {
        if (to.isDirectory()) {
            to = new File(joinDir(to, getFileTail(from.toString())));
        }

        String contents = readContents(from.toString(), IOUtil.class);
        writeFile(to, contents);
    }



    /**
     * Determine if the given filename is a text file.  i.e., it ends with
     * .txt or .text
     *
     * @param filename The filename to check.
     * @return Is the filename a text file.
     */
    public static boolean isTextFile(String filename) {
        filename = filename.toLowerCase();
        return (filename.endsWith(".txt") || filename.endsWith(".text"));
    }


    /**
     * Determine if the given filename is an image file (e.g., ends with
     * .gif, .jpg, .jpeg, .png)
     *
     *  @param filename The filename to check.
     *  @return Is the filename an image file.
     */
    public static boolean isImageFile(String filename) {
        filename = filename.toLowerCase();
        return (filename.endsWith(".gif") || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg") || filename.endsWith(".png"));
    }




    /**
     *  Determine if the given filename is an html file.
     *  i.e., it ends with .htm or .html or if it does not have
     *  a file extension and begins with http:
     *
     *  @param filenameOrUrl The filename to check.
     *  @return Is the filename an html file.
     */
    public static boolean isHtmlFile(String filenameOrUrl) {
        //We go through this caching business because the Inner method
        //can end up fetching the url for some cases and we
        //don't want to keep doing that every time this method is called.
        Boolean b = (Boolean) isHtmlCache.get(filenameOrUrl);
        if (b != null) {
            return b.booleanValue();
        }
        boolean result = isHtmlFileInner(filenameOrUrl);
        isHtmlCache.put(filenameOrUrl, new Boolean(result));
        return result;
    }

    /**
     *  Determine if the given filename is an html file.
     *  i.e., it ends with .htm or .html or if it does not have
     *  a file extension and begins with http:
     *
     *  @param filenameOrUrl The filename to check.
     *  @return Is the filename an html file.
     */
    private static boolean isHtmlFileInner(String filenameOrUrl) {
        String originalUrl = filenameOrUrl;

        filenameOrUrl = filenameOrUrl.toLowerCase();
        if (isHtmlSuffix(filenameOrUrl)) {
            return true;
        }

        int hashIndex = filenameOrUrl.indexOf("#");
        if (hashIndex > 0) {
            filenameOrUrl = filenameOrUrl.substring(0, hashIndex);
            if (isHtmlSuffix(filenameOrUrl)) {
                return true;
            }
        }

        //Look for no dots and no args
        //Not sure wha't going on here
        if (filenameOrUrl.indexOf("?") < 0) {
            if (filenameOrUrl.startsWith("http:")) {
                if ( !StringUtil.stringMatch(filenameOrUrl,
                                             "http://[^\\/].*\\.\\.*")) {
                    return true;
                }
            }
        }


        //TODO - this fails when you don't have an "/" at the end
        //e.g., http://www.yahoo.com fails
        String ext = getFileExtension(filenameOrUrl);
        int    idx = ext.indexOf("?");
        if (idx >= 0) {
            ext = ext.substring(0, idx);
        }

        if (ext.length() == 0) {
            return isHttpProtocol(filenameOrUrl);
        }


        if (ext.equals(".php") && isHttpProtocol(filenameOrUrl)) {
            try {
                URL           url        = new URL(originalUrl);
                URLConnection connection = url.openConnection();
                String        type       = connection.getContentType();
                return (type.indexOf("text/html") >= 0);
            } catch (Exception exc) {}

            return true;
        }


        if (ext.equals(".com") || ext.equals(".edu") || ext.equals(".org")
                || isHtmlSuffix(ext)) {
            return true;
        }
        return false;
    }


    /**
     * Does the given url end with an html suffix.
     *
     * @param url The url
     *
     * @return Ends with html, htm, or shtml
     */
    public static boolean isHtmlSuffix(String url) {
        return url.endsWith(".htm") || url.endsWith(".html")
               || url.endsWith(".shtml");
    }

    /**
     * Is the given url an http protocol.
     *
     * @param url The url
     *
     * @return Starts with http or https
     */
    public static boolean isHttpProtocol(String url) {
        return url.startsWith("http:") || url.startsWith("https:");
    }

    /**
     * Get an input stream for the filename
     *
     * @param filename   name of file
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     */
    public static InputStream getInputStream(String filename)
            throws FileNotFoundException, IOException {
        return IOUtil.getInputStream(filename, null);
    }


    /**
     * Get an input stream for the filename
     *
     * @param filename    name of file
     * @param origin      relative origin point for file location
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     */
    public static InputStream getInputStream(String filename, Class origin)
            throws FileNotFoundException, IOException {
        try {
            URL url = getURL(filename, origin);
            if (url != null) {
                return url.openConnection().getInputStream();
            }
        } catch (Exception exc) {
            throw new IOException("Could not load resource:" + filename
                                  + " error:" + exc);
        }
        throw new FileNotFoundException("Could not load resource:"
                                        + filename);
    }

    /**
     * Get an input stream for the filename
     *
     * @param filename    name of file
     * @param origin      relative origin point for file location
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     */
    public static InputStream getInputStreamOLDWAY(String filename,
            Class origin)
            throws FileNotFoundException, IOException {


        InputStream s = null;

        //Try the file system
        if (s == null) {
            File f = new File(filename);
            if (f.exists()) {
                try {
                    s = new FileInputStream(f);
                } catch (Exception e) {}
            }
        }

        //Try it as a url
        if (s == null) {
            try {
                String encodedUrl = StringUtil.replace(filename, " ", "%20");
                URL           dataUrl    = new URL(encodedUrl);
                URLConnection connection = dataUrl.openConnection();
                s = connection.getInputStream();
            } catch (Exception exc) {}
        }



        if (s == null) {
            List classLoaders = Misc.getClassLoaders();
            for (int i = 0; i < classLoaders.size(); i++) {
                ClassLoader cl = (ClassLoader) classLoaders.get(i);
                s = cl.getResourceAsStream(filename);
                if (s != null) {
                    break;
                }
            }
        }


        if (s == null) {
            while (origin != null) {
                s = origin.getResourceAsStream(filename);
                if (s != null) {
                    break;
                }
                origin = origin.getSuperclass();
            }
        }

        //Try an absolute resource path
        if (s == null) {
            s = IOUtil.class.getResourceAsStream(filename);
        }

        if (s == null) {
            throw new FileNotFoundException("Unable to open:" + filename);
        }
        return s;
    }


    /**
     * Get an input stream for the filename
     *
     * @param filename    name of file
     * @param origin      relative origin point for file location
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     */
    public static URL getURL(String filename, Class origin)
            throws FileNotFoundException, IOException {
        //Try the file system


        File f = new File(filename);
        if (f.exists()) {
            try {
                String name = f.getName();
                //Check if we are dealing with a file whose name came from a encoded url
                if (name.indexOf("%") >= 0) {
                    name = java.net.URLEncoder.encode(name, "UTF-8");
                    f    = new File(joinDir(f.getParent(), name));
                    //                    System.err.println ("new file:" + f);
                }
                return f.toURL();
            } catch (Exception e) {}
        }

        try {
            //            filename = String


            String encodedUrl = StringUtil.replace(filename, " ", "%20");
            return new URL(encodedUrl);
        } catch (Exception exc) {}


        List classLoaders = Misc.getClassLoaders();
        for (int i = 0; i < classLoaders.size(); i++) {
            try {
                ClassLoader cl  = (ClassLoader) classLoaders.get(i);
                URL         url = cl.getResource(filename);
                if (url != null) {
                    return url;
                }
            } catch (Exception exc) {}
        }


        while (origin != null) {
            try {
                URL url = origin.getResource(filename);
                if (url != null) {
                    return url;
                }
                origin = origin.getSuperclass();
            } catch (Exception exc) {}
        }


        try {
            URL url = IOUtil.class.getResource(filename);
            if (url != null) {
                return url;
            }
        } catch (Exception exc) {}

        throw new FileNotFoundException("Unable to open:" + filename);
    }




    /**
     * Read the contents of a {@link File}.  Used for reading text type
     * files (XML, HTML, etc)
     *
     * @param file    file to read.
     * @return  contents as a String
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     */
    public static String readContents(File file)
            throws FileNotFoundException, IOException {
        return readContents(file.getPath());
    }



    /**
     * Return the String contents of the specified contentName.
     * If the read fails (for whatever reason) then return the dflt parameter
     *
     * @param contentName   URL or filename
     * @param dflt          default to return if a problem
     * @return  contents or default value
     */
    public static String readContents(String contentName, String dflt) {
        return readContents(contentName, IOUtil.class, dflt);
    }


    /**
     * Return the String contents of the specified contentName.
     * If the read fails (for whatever reason) then return the dflt parameter
     *
     * @param contentName   URL or filename
     * @param origin        origin class
     * @param dflt          default to return if a problem
     * @return  contents or default value
     */
    public static String readContents(String contentName, Class origin,
                                      String dflt) {
        try {
            return readContents(contentName, origin);
        } catch (Exception exc) {}
        return dflt;
    }


    /**
     * Return the String contents of the specified contentName.
     *
     * @param contentName can either be a URL, a filename or a resource.
     *
     * @return   contents or <code>null</code> if there is a problem.
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem reading contents
     */
    public static String readContents(String contentName)
            throws FileNotFoundException, IOException {
        return readContents(contentName, IOUtil.class);
    }


    /**
     * Is the given path relative
     *
     * @param path file path
     *
     * @return is relative
     */
    public static boolean isRelativePath(String path) {
        if (path.startsWith("http:") || path.startsWith("ftp:")
                || path.startsWith("/") || path.startsWith(File.separator)) {
            return false;
        }
        //Check for windows drives
        if (path.substring(1, 2).equals(":")) {
            return false;
        }
        return true;

    }


    /**
     *  Return the String contents of the specified contentName.
     *  contentName  can either be a URL, a filename or a resource.
     *
     * @param contentName can either be a URL, a filename or a resource.
     * @param origin    relative origin for path to file
     *
     * @return   contents or <code>null</code> if there is a problem.
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem reading contents
     */
    public static String readContents(String contentName, Class origin)
            throws FileNotFoundException, IOException {
        //If a bad url then try it as a file

        InputStream s = IOUtil.getInputStream(contentName, origin);
        if (s == null) {
            return null;
        }
        String results = readContents(s);
        try {
            s.close();
        } catch (Exception exc) {}
        return results;
    }



    /**
     * See if the content is in the perma-cache. If it is then return it.
     * Else read it (e.g., from  a url) and cache it.
     *
     * @param contentName url or filename
     * @param cacheGroup Cache group
     *
     * @return Bytes read
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public static byte[] readBytesAndCache(String contentName,
                                           String cacheGroup)
            throws FileNotFoundException, IOException {
        return readBytesAndCache(contentName, cacheGroup, false);
    }


    /**
     * See if the content is in the perma-cache. If it is then return it.
     * Else read it (e.g., from  a url) and cache it.
     *
     * @param contentName url or filename
     * @param cacheGroup Cache group
     * @param unzipIfNeeded IF true and if the url is a zip file then unzip it
     *
     * @return Bytes read
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */

    public static byte[] readBytesAndCache(String contentName,
                                           String cacheGroup,
                                           boolean unzipIfNeeded)
            throws FileNotFoundException, IOException {
        //If a bad url then try it as a file
        //        System.err.println ("readBytesAndCache:" + contentName);
        byte[] bytes = null;
        if ( !(new File(contentName)).exists()) {
            bytes = CacheManager.getCachedFile(cacheGroup, contentName);
            if (bytes != null) {
                //                System.err.println("cached:" + contentName);
                return bytes;
            }
        }
        //        System.err.println("not cached:" + Misc.getFileTail(contentName));
        bytes = readBytes(IOUtil.getInputStream(contentName, IOUtil.class));
        if (bytes != null) {
            if (unzipIfNeeded && contentName.endsWith(".zip")) {
                //                System.err.println("Reading zip:" + bytes.length);
                ZipInputStream zin =
                    new ZipInputStream(new ByteArrayInputStream(bytes));
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String name = ze.getName().toLowerCase();
                    bytes = IOUtil.readBytes(zin);
                    //                    System.err.println ("Zipped file:" + name + "  " + bytes.length);
                    break;
                }
            }
            CacheManager.putCachedFile(cacheGroup, contentName, bytes);
        }
        return bytes;
    }



    /**
     * Read in the bytes from the given InputStream
     * and construct and return a String.
     * Closes the InputStream argument.
     *
     * @param is   InputStream to read from
     * @return  contents as a String
     *
     * @throws IOException  problem reading contents
     */
    public static String readContents(InputStream is) throws IOException {
        return new String(readBytes(is));
    }


    /**
     * Read in the bytes from the given InputStream
     * Closes the InputStream argument.
     *
     * @param is   InputStream to read from
     * @return  bytes read
     *
     * @throws IOException  problem reading contents
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        return readBytes(is, null);
    }

    /**
     * Read in the bytes from the given InputStream
     * Closes the InputStream argument. The globalTimestamp and myTimestamp
     * parameters, if non-null, allow calling routines to abort the read.
     * globalTimestamp, if non-null, is a array of size one that holds a virtual
     * timestamp. If at anytime during the read of the bytes the value in globalTimestamp
     * is different then the value given for myTimestamp then the read is aborted and
     * null is returned.
     *
     * @param is   InputStream to read from
     * @param loadId Job manager load id
     * @return  bytes read
     *
     * @throws IOException  problem reading contents
     */
    public static byte[] readBytes(InputStream is, Object loadId)
            throws IOException {
        return readBytes(is, loadId, true);
    }

    /**
     * Read the bytes in the given input stream.
     *
     * @param is The input stream
     * @param loadId If non-null check with the JobManager if we should continue
     * @param closeIt If true then close the input stream
     *
     * @return The bytes
     *
     * @throws IOException On badness
     */
    public static byte[] readBytes(InputStream is, Object loadId,
                                   boolean closeIt)
            throws IOException {
        int    totalRead = 0;
        byte[] content   = getByteBuffer();
        while (true) {
            int howMany = is.read(content, totalRead,
                                  content.length - totalRead);
            //      Trace.msg("IOUtil.readBytes:" + howMany + " buff.length=" + content.length);
            if ((loadId != null)
                    && !JobManager.getManager().canContinue(loadId)) {
                try {
                    if (closeIt) {
                        is.close();
                    }
                } catch (Exception exc) {}
                //                System.err.println ("Ditching");                
                return null;
            }
            if (howMany < 0) {
                break;
            }
            if (howMany == 0) {
                continue;
            }
            totalRead += howMany;
            if (totalRead >= content.length) {
                byte[] tmp       = content;
                int    newLength = ((content.length < 25000000)
                                    ? content.length * 2
                                    : content.length + 5000000);
                content = new byte[newLength];
                System.arraycopy(tmp, 0, content, 0, totalRead);
            }
        }
        if (closeIt) {
            is.close();
        }
        byte[] results = new byte[totalRead];
        System.arraycopy(content, 0, results, 0, totalRead);
        putByteBuffer(content);
        return results;
    }

    /** mutex for reading */
    private static Object MUTEX = new Object();

    /**
     * Save the read buffer
     *
     * @param bytes read buffer
     */
    private static void putByteBuffer(byte[] bytes) {
        synchronized (MUTEX) {
            //Don't cache a big chunk
            if (bytes.length < 5000000) {
                CacheManager.put(MUTEX, "bytes", bytes);
            }
        }
    }

    /**
     * Get a read buffer
     *
     * @return read buffer
     */
    private static byte[] getByteBuffer() {
        synchronized (MUTEX) {
            byte[] bytes = (byte[]) CacheManager.get(MUTEX, "bytes");
            if (bytes == null) {
                bytes = new byte[1000000];
            } else {
                CacheManager.remove(MUTEX, "bytes");
            }
            return bytes;
        }
    }






    /**
     * Return the String representation of the given filename joined to the
     * given directory.
     *
     * @param f1    directory path
     * @param f2    filename
     * @return  concatenated String with the appropriate file separator
     */
    public static String joinDir(String f1, String f2) {
        return f1 + File.separator + f2;
    }


    /**
     * Return the String representation of the given filename joined to the
     * given directory f1.
     *
     * @param f1          directory path
     * @param filename    filename
     * @return  concatenated String with the appropriate file separator
     */
    public static String joinDir(File f1, String filename) {
        return (f1.getPath() + File.separator + filename);
    }


    /**
     *  If the directory defined in the given argument path
     *  does not exist then make it.
     *
     * @param path   directory to make
     * @return  the directory path
     */
    public static String makeDir(String path) {
        return makeDir(new File(path));
    }


    /**
     *  If the directory defined in the given argument f
     *  does not exist then make it.
     *
     * @param f   directory as a file
     * @return   directory path
     */
    public static String makeDir(File f) {
        if ( !f.exists()) {
            if ( !f.mkdir()) {
                System.out.println("Failed to make directory " + f.getPath());
            }
        }
        return f.getPath();
    }




    /**
     * Recursively descend (if recurse is true)
     * through the given directory and return a
     * list of all files
     *
     * @param dir The directory to look at
     * @param recurse Do we recurse
     *
     * @return List of files
     */
    public static List getFiles(File dir, boolean recurse) {
        return getFiles(new ArrayList(), dir, recurse);

    }


    /**
     * Recursively descend (if recurse is true)
     * through the given directory and return a
     * list of all files
     *
     * @param files The list of files to add to
     * @param dir The directory to look at
     * @param recurse Do we recurse
     *
     * @return List of files
     */
    public static List getFiles(List files, File dir, boolean recurse) {
        return getFiles(files, dir, recurse, null);
    }


     /**
     * Recursively descend (if recurse is true)
     * through the given directory and return a
     * list of all files
     *
     * @param files The list of files to add to
     * @param dir The directory to look at
     * @param recurse Do we recurse
     * @param filter If non-null then use this to find files
     *
     * @return List of files
     */
    public static List getFiles(List files, File dir, boolean recurse,
                                PatternFileFilter filter) {
        if (files == null) {
            files = new ArrayList();
        }
        List dirs = getDirectories(dir, recurse);
        System.err.println ("dirs:" + dirs);
        for (int dirIdx = 0; dirIdx < dirs.size(); dirIdx++) {
            File   directory = (File) dirs.get(dirIdx);
            File[] allFiles  = ((filter == null)
                                ? directory.listFiles()
                                : directory.listFiles((FileFilter) filter));
            for (int fileIdx = 0; fileIdx < allFiles.length; fileIdx++) {
                if ( !allFiles[fileIdx].isDirectory()) {
                    files.add(allFiles[fileIdx]);
                }
            }
        }
        return files;
    }

    
    public static interface FileViewer {
        public boolean viewFile(File f) throws Exception ;
    }


    public static boolean walkDirectory(File dir, FileViewer fileViewer) throws Exception {
        File[] children  = dir.listFiles();
        if(children == null) return true;
        for (int i=0;i<children.length;i++) {
            if(!fileViewer.viewFile(children[i])) return false;
            if(!walkDirectory(children[i], fileViewer)) return false;
        }
        return true;
    }


    /**
     * Recursively descend (if recurse is true)
     * through the given directory and return a
     * list of all subdirectories.
     *
     * @param dir The directory to look at
     * @param recurse Do we recurse
     *
     * @return List of subdirs (File)
     */
    public static List getDirectories(File dir, boolean recurse) {
        return getDirectories(Misc.newList(dir), recurse);
    }


    /**
     * Recursively descend (if recurse is true)
     * through the given directories and return a
     * list of all subdirectories.
     *
     * @param dirs List of directories to look at
     * @param recurse Do we recurse
     *
     * @return List of subdirs (File)
     */
    public static List getDirectories(List dirs, boolean recurse) {
        List results = new ArrayList();
        File dir;
        for (int i = 0; i < dirs.size(); i++) {
            if (dirs.get(i) == null) {
                continue;
            }
            if ( !(dirs.get(i) instanceof File)) {
                dir = new File(dirs.get(i).toString());
            } else {
                dir = (File) dirs.get(i);
            }
            if ( !dir.exists()) {
                continue;
            }
            File[] subdirs = dir.listFiles();
            for (int subDirIdx = 0; subDirIdx < subdirs.length; subDirIdx++) {
                if ( !subdirs[subDirIdx].isDirectory()) {
                    continue;
                }
                results.add(subdirs[subDirIdx]);
                if (recurse) {
                    results.addAll(getDirectories(subdirs[subDirIdx],
                            recurse));
                }
            }
        }
        return results;
    }


    /**
     * This will recursively  prune empty subdirectories
     * of the given root directory. It will delete the given
     * root directory if it is empty.
     *
     * @param root Directory to prune
     */
    public static void pruneIfEmpty(File root) {
        if (root == null) {
            return;
        }
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                pruneIfEmpty(files[i]);
            }
        }
        if (root.listFiles().length == 0) {
            root.delete();
        }
    }



    /**
     * This will recursively  prune empty subdirectories
     * of the given root directory. It will NOT delete the given
     * root directory if it is empty.
     *
     * @param root Directory to prune
     */
    public static void pruneEmptyDirectories(File root) {
        File[] files = root.listFiles();
        for (int i = 0; i < files.length; i++) {
            pruneIfEmpty(files[i]);
        }
    }


    /**
     * This will recursively  delete all contents under the given directory.
     *
     * @param root Directory to delete
     */
    public static void deleteDirectory(File root) {
        if (root.exists() && root.isDirectory()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteDirectory(files[i]);
            }
        }
        root.delete();
    }





    /**
     * Wait until there are new files in the given directory.
     * If filePattern is non null then use that (regular expression)
     * to match the files.
     *
     * @param directory The directory
     * @param filePattern The pattern
     * @param sleepSeconds Sleep this number of seconds between each check
     */
    public static void wait(File directory, String filePattern,
                            double sleepSeconds) {
        java.io.FileFilter pattern = null;
        if (filePattern != null) {
            pattern = (java.io.FileFilter) new PatternFileFilter(filePattern);
        }
        File[] allFiles = ((pattern == null)
                           ? directory.listFiles()
                           : directory.listFiles(pattern));
        while (true) {
            Misc.sleep((long) (1000 * sleepSeconds));
            File[] newFiles = ((pattern == null)
                               ? directory.listFiles()
                               : directory.listFiles(pattern));
            if ( !Arrays.equals(allFiles, newFiles)) {
                break;
            }
        }
    }

    /**
     * Wait until one or more of the files in the files list (File)
     * has changed. If the files list is null or empty then just return.
     *
     * @param files List of File-s
     * @param sleepSeconds Seconds to sleep between checks.
     */
    public static void wait(List files, double sleepSeconds) {
        if ((files == null) || (files.size() == 0)) {
            return;
        }
        long[] times = new long[files.size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = ((File) files.get(i)).lastModified();
        }
        boolean oneChanged = false;
        while ( !oneChanged) {
            Misc.sleep((long) (1000 * sleepSeconds));
            for (int i = 0; i < times.length; i++) {
                File theFile = (File) files.get(i);
                if (times[i] != theFile.lastModified()) {
                    oneChanged = true;
                    break;
                }
            }
        }
    }


    

    static class FileInfo {
        File f;
        long dttm;
        public FileInfo(File f) {
            this.f = f;
            dttm =f.lastModified();
        }
        public String toString() {
            return f.toString();
        }
    }


    static List files = new ArrayList();
    private static int cnt = 0;
    private static Hashtable ht = new Hashtable();

    public static void walkTree(File f) {
        Object foo = ht.get(f);
        if(foo!=null) {
            return;
        }
        cnt++;
        long t = f.lastModified();
        //        System.err.println ("t:" + t);
        if(f.isDirectory()) {
            File[] files = f.listFiles();
            if(files == null) return;
            for(int i=0;i<files.length;i++)
                walkTree(files[i]);

        } else {
            Object o = ht.get(f);
            if(o==null) {
                files.add(new FileInfo(f));
                //crackFile(f);
                ht.put(f,f);
            }

        }
    }


    public static List findFiles(Date d1, Date d2) {
        long t1 = d1.getTime();
        long t2 = d2.getTime();
        List result = new ArrayList();
        for(int i=0;i<files.size();i++) {
            FileInfo fi = (FileInfo)  files.get(i);
            if(fi.dttm>=t1 && fi.dttm<=t2) 
                result.add(fi.f);
        }
        return result;
    }


    public static List findChangedFiles() {
        List result = new ArrayList();
        for(int i=0;i<files.size();i++) {
            FileInfo fi = (FileInfo)  files.get(i);
            if(fi.dttm!=fi.f.lastModified()) {
                result.add(fi);
            }
        }
        return result;
    }



    private static void testWrite(float[][]f, int buffSize) throws Exception {
        FileOutputStream   fos = new FileOutputStream("test2.data");
        BufferedOutputStream bos = new BufferedOutputStream(fos, buffSize);
        ObjectOutputStream p       = new ObjectOutputStream(bos);
        p.writeObject(f);
        p.flush();
        p.close();
        fos.close();
    }





    /**
     * test main
     *
     * @param args cmd line args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {

        float[][] f= new float[2][1000000];

        
        for(int bs = 500000;bs>1000;bs-=20000) {
            long total = 0;
            for(int i=0;i<10;i++) {
                long t1 = System.currentTimeMillis();
                //                readBytes(new FileInputStream("test2.data"));
                readBytes(new FileInputStream("test2.data"),100000);
                long t2= System.currentTimeMillis();
                total+= (t2-t1);
            }
            System.err.println ((total/10));
            if(true) break;
            System.err.println (bs +" " + (total/5));
        }









        if(true) return;

        /*

        //        File ff = new File(args[0]);
        //        System.err.println(ff.lastModified());
        //        if(true) return;
        for(int xx=0;xx<10;xx++) {
            cnt = 0;
            //        ht = new Hashtable();
            //        files  = new ArrayList();
            long tt1 = System.currentTimeMillis();
            walkTree(new File(args[0]));
            long tt2 = System.currentTimeMillis();
            List result = findFiles(new Date(System.currentTimeMillis()-1000*60*60*24*7),
                                    new Date(System.currentTimeMillis()));
            long tt3 = System.currentTimeMillis();
            List changed = findChangedFiles();
            long tt4 = System.currentTimeMillis();
            System.err.println (" files indexed:" + cnt + " time:" + (tt2-tt1) + " files found:" +  result.size() + " time to find:" + (tt3-tt2) + " time to find changed:" + (tt4-tt3));



            if(true) break;
        }



        //        System.err.println("isrel:" + isRelativePath("C:"));
        if (true) {
            return;
        }





        String file =
            "/home/jeffmc/.metapps/DefaultIdv/plugins/%2Fhome%2Fjeffmc%2Fcontrols.xml";
        File f = new File(file);
        try {
            System.err.println("f.exists: " + f.exists());
            URL url = getURL(file, IOUtil.class);
            System.err.println("the url:" + url);
            System.err.println("getFile:" + url.getFile());
            InputStream s = url.openConnection().getInputStream();
            System.err.println("url:" + url);
            //        InputStream s = IOUtil.getInputStream(file,IOUtil.class);
            System.err.println("s!=null " + (s != null));
            String c = IOUtil.readContents(file, IOUtil.class);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (true) {
            System.exit(0);
        }



        try {
            for (int j = 0; j < 5; j++) {

                for (int i = 0; i < args.length; i++) {
                    long   t1       = System.currentTimeMillis();
                    String contents = readContents(args[i]);
                    long   t2       = System.currentTimeMillis();
                    //                readContents(new ByteArrayInputStream(contents.getBytes()));
                    long t3 = System.currentTimeMillis();
                    System.err.println(args[i] + " Time:" + (t2 - t1) + " "
                                       + (t3 - t2));
                }
            }


            //            ftpPut("ftp.unidata.ucar.edu", "anonymous", "jeffmc@ucar.edu",
            //                   "/incoming/idv/test11.txt", "hello".getBytes());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        */
    }

    /**
     * Write out the list of files to the jar file specified by filename
     *
     * @param filename jar file name
     * @param files list of files
     *
     * @throws IOException On badness
     */
    public static void writeJarFile(String filename, List files)
            throws IOException {
        writeJarFile(filename, files, null);
    }


    /**
     * Write out the list of files to the jar file specified by filename
     *
     * @param filename jar file name
     * @param files list of files
     * @param pathPrefix If not null this is the prefx we add to the jar entry
     *
     * @throws IOException On badness
     */
    public static void writeJarFile(String filename, List files,
                                    String pathPrefix)
            throws IOException {
        ZipOutputStream zos =
            new ZipOutputStream(new FileOutputStream(filename));
        for (int i = 0; i < files.size(); i++) {
            String path;
            Object tmp = files.get(i);
            byte[] bytes;
            if (tmp instanceof String) {
                bytes = IOUtil.readBytes(IOUtil.getInputStream((String) tmp));
                path = IOUtil.getFileTail((String)tmp);
            } else if (tmp instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) tmp;
                if(tfo.getId() instanceof byte[]) {
                    bytes = (byte[]) tfo.getId();
                } else {
                    bytes = IOUtil.readBytes(IOUtil.getInputStream(tfo.getId().toString()));
                }
                path = tfo.getLabel().toString();
            } else {
                throw new IllegalArgumentException("Unknown file:" + tmp);
            }
            if (pathPrefix != null) {
                path = pathPrefix + "/" + path;
            }
            zos.putNextEntry(new ZipEntry(path));
            zos.write(bytes, 0, bytes.length);
        }
        zos.close();
    }




}

