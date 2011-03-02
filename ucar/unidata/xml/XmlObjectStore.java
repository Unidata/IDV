/*
 * $Id: XmlObjectStore.java,v 1.51 2007/04/20 21:51:21 jeffmc Exp $
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




package ucar.unidata.xml;


import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;

import ucar.unidata.util.PersistentStore;

import java.awt.Color;
import java.awt.Font;



import java.io.*;

import java.util.*;



/**
 * This allows us to use the {@link XmlEncoder} to read/write a Hashtable that holds a collection
 * of persistent objects.
 * @author Metapps development team
 * @version $Revision: 1.51 $
 */

public class XmlObjectStore implements PersistentStore {

    /** The encoder we use to encode the store */
    private XmlEncoder encoder;

    /**
     * <code>ResourceCollection</code> that points to
     * the encoded xml files that holds this store
     */
    private XmlResourceCollection resources;

    /** The directory we write things to */
    private File userDirectory;

    /** The <code>Hashtable</code> that holds the store in memory */
    private Hashtable table = new Hashtable();


    /** We keep track if anything wasput into the store and set this flag to true */
    private boolean inNeedOfSaving = false;


    /** Keep track of the tmp files that have been created */
    protected List tmpFiles = new ArrayList();


    /** _more_ */
    private String tmpDirOverride;


    /**
     *  Create a new store.
     *
     * @param encoder
     */
    public XmlObjectStore(XmlEncoder encoder) {
        this.encoder = encoder;
    }


    /**
     * Set the <code>XmlEncoder</code> that we use
     *
     * @param encoder The encoder we use to write out the store
     */
    protected void setEncoder(XmlEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Get the encoder we use
     *
     * @return The encoder we use to write out the store
     */
    protected XmlEncoder getEncoder() {
        if (encoder == null) {
            encoder = new XmlEncoder();
        }
        return encoder;
    }


    /**
     *  We will assume that the first file in the list of store files
     *  is held within the user's directory.
     *
     *  @return The user's directory.
     */
    public File getUserDirectory() {
        if (resources == null) {
            return null;
        }
        if (userDirectory == null) {
            userDirectory = new File(resources.getWritable()).getParentFile();
        }
        return userDirectory;
    }


    /**
     * Check if the userDirectory is okay to write to.
     *
     * @return true if the directory exists and is writable
     */
    public boolean userDirectoryOk() {
        File userDirectory = getUserDirectory();
        if (userDirectory.exists() && userDirectory.canWrite()) {
            return true;
        }
        return false;
    }



    /**
     * _more_
     *
     * @param dir _more_
     */
    public void setTmpDir(String dir) {
        tmpDirOverride = dir;
    }


    /**
     *  Create (if not there) a "tmp" directory under the user's directory.
     *
     *  @return The path to the new tmp directory.
     */
    public String getUserTmpDirectory() {
        String tmpDir = null;
        if (tmpDirOverride == null) {
            tmpDir = IOUtil.joinDir(getUserDirectory().toString(), "tmp");
        } else {
            tmpDir = tmpDirOverride;
        }
        IOUtil.makeDir(tmpDir);
        return tmpDir;
    }


    /**
     * Return  the full path to a temporary file with the given file tail.
     * The filename will be added to a list of tmpFiles that can then be removed
     * when an application exits.
     *
     * @param tail The file tail (e.g., temp.txt)
     * @return The full path (e.g., ~/.metapps/tmp/temp.txt)
     */
    public String getTmpFile(String tail) {
        String filename = IOUtil.joinDir(getUserTmpDirectory(), tail);
        if ( !tmpFiles.contains(filename)) {
            tmpFiles.add(filename);
        }
        return filename;
    }


    /**
     * This routine will delete all files in the user's tmp directory but
     * will leave the directories
     */
    public void cleanupTmpDirectory() {
        if ( !userDirectoryOk()) {
            return;
        }
        File dir = new File(getUserTmpDirectory());
        cleanupTmpDirectory(dir);
    }


    /**
     * This routine will delete all files in the given dir parameter.
     * This will recurse on any sub directories.
     *
     * @param dir the directory to clean up
     */
    private void cleanupTmpDirectory(File dir) {
        long ONEDAY = 1000 * 60 * 60 * 24;
        try {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                long hoursOld = (System.currentTimeMillis()
                                 - f.lastModified()) / 1000 / 60 / 60;
                //If its a dir then recurse
                if (f.isDirectory()) {
                    cleanupTmpDirectory(f);
                    //Check to see if it still holds files
                    //If the dir is empty and old then get rid of it
                    File[] subfiles = f.listFiles();
                    if ((subfiles.length == 0) && (hoursOld > 23)) {
                        //System.err.println ("Deleting empty tmp dir:" + f);
                        f.delete();
                    } else {
                        //System.err.println ("dir is ok: " + f + " subfiles:" + subfiles.length);
                    }
                } else if (hoursOld > 23) {
                    //System.err.println ("Deleting tmp file:" + f);
                    f.delete();
                }
            }
        } catch (Exception exc) {
            System.err.println("Error cleaning up files:" + exc);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public File getUniqueTmpDirectory() {
        String file   = null;
        File   tmpDir = new File(getUserTmpDirectory());
        int    cnt    = 0;
        while (true) {
            file = IOUtil.joinDir(tmpDir, "tmpdir" + cnt);
            if ( !(new File(file)).exists()) {
                break;
            }
            cnt++;
        }
        IOUtil.makeDir(file);
        return new File(file);
    }


    /**
     * _more_
     *
     * @param prefix _more_
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getUniqueTmpFile(String prefix, String suffix) {
        int    cnt  = 0;
        String file = null;
        while (true) {
            file = getTmpFile(prefix + cnt + suffix);
            if ( !(new File(file)).exists()) {
                break;
            }
            cnt++;
        }
        return file;
    }


    /**
     * This method will remove all temp files that have been created during the
     * current run of the program and clear out the list of tmp files.
     */
    public void cleanupTmpFiles() {
        for (int i = 0; i < tmpFiles.size(); i++) {
            File f = new File(tmpFiles.get(i).toString());
            if ( !f.exists()) {
                continue;
            }
            try {
                //System.err.println ("deleting tmp file:" + f);
                f.delete();
            } catch (Exception exc) {
                //              System.err.println("exc:" + exc);
            }
        }
        tmpFiles = new ArrayList();
    }



    /**
     *  Initialize the store.
     *
     *  @param rc The resource collection to read from
     *
     * @return _more_
     */
    public int init(XmlResourceCollection rc) {
        this.resources = rc;
        return append(rc, false);
    }


    /**
     * _more_
     *
     * @param rc _more_
     * @param onlyAddIfNotExists _more_
     *
     * @return _more_
     */
    public int append(XmlResourceCollection rc, boolean onlyAddIfNotExists) {
        int cnt = 0;
        for (int i = rc.size() - 1; i >= 0; i--) {
            try {
                Element root = rc.getRoot(i);
                if (root == null) {
                    continue;
                }
                Object object = getEncoder().toObject(root);
                if ( !(object instanceof Hashtable)) {
                    continue;
                }
                cnt++;
                Hashtable newTable = (Hashtable) object;
                newTable = processTable(newTable);
                if ( !onlyAddIfNotExists) {
                    table.putAll(newTable);
                } else {
                    for (Enumeration keys = newTable.keys();
                            keys.hasMoreElements(); ) {
                        Object key = keys.nextElement();
                        if ( !table.containsKey(key)) {
                            table.put(key, newTable.get(key));
                        }
                    }
                }
            } catch (Exception exc) {
                System.err.println("Error reading resources" + exc);
                exc.printStackTrace();
            }
        }
        return cnt;
    }



    /**
     * Process a HashTable
     *
     * @param newTable  table to process
     *
     * @return a processed table
     */
    protected Hashtable processTable(Hashtable newTable) {
        return newTable;
    }

    /**
     *  The given filename is a file which (should) hold the xml
     * encoded ({@link XmlEncoder}) version of some Object.
     *
     *  @param filename The file that contains the encoded object.
     *  @return The decoded object.
     */
    public Object getEncodedFile(String filename) {
        String contents = getFileContents(filename);
        if (contents == null) {
            return null;
        }
        try {
            getEncoder().clear();
            return getEncoder().toObject(contents);
        } catch (Exception exc) {
            System.err.println("Error reading file: " + filename + "\n"
                               + exc);
            return null;
        }
    }


    /**
     *  Encode the given object and write it to the given filename.
     *
     *  @param filename The filename to write to.
     *  @param o The object to encode.
     */
    public void putEncodedFile(String filename, Object o) {
        getEncoder().clear();
        String contents = getEncoder().toXml(o);
        if (contents == null) {
            System.err.println("Failed to write file:" + filename);
            return;
        }
        putFile(filename, contents);
    }



    /**
     *  Read the contents of the filename, which is  relative to the user's directory.
     *
     *  @param filename The file   to read.
     *  @return The contents of the file.
     */
    public String getFileContents(String filename) {
        if ( !userDirectoryOk()) {
            return null;
        }
        String filePath = IOUtil.joinDir(getUserDirectory(), filename);
        try {
            return IOUtil.readContents(filePath);
        } catch (Exception exc) {
            //      System.err.println ("Error reading file: " + filePath +"\n"+exc);
        }
        return null;
    }


    /**
     *  Write the contents to the filename, which is  relative to the user's directory.
     *
     *  @param filename The file   to read.
     *  @param contents  The contents of the file.
     */
    public void putFile(String filename, String contents) {
        if ( !userDirectoryOk()) {
            return;
        }
        String filePath = IOUtil.joinDir(getUserDirectory(), filename);
        try {
            IOUtil.writeFile(filePath, contents);
        } catch (Exception exc) {
            System.err.println("Error reading file: " + filePath + "\n"
                               + exc);
        }
    }

    /**
     *  Return the object held in the table identified by the given key.
     *
     *  @param key The object's key.
     *  @return The Object identified by the given key or null if not found.
     */
    public synchronized Object get(String key) {
        return table.get(key);
    }


    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, boolean value) {
        put(key, new Boolean(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, char value) {
        put(key, new Character(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, short value) {
        put(key, new Short(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, int value) {
        put(key, new Integer(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, float value) {
        put(key, new Float(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, long value) {
        put(key, new Long(value));
    }

    /**
     *  Put the given value into the tabl.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public void put(String key, double value) {
        put(key, new Double(value));
    }

    /**
     *  Put the given value.
     *  @param key Convert to toString to get the actual key.
     *  @param value The value to store.
     */
    public void put(Object key, Object value) {
        put(key.toString(), value);
    }


    /**
     *  Put the given value.
     *  @param key The object's key.
     *  @param value The value to store.
     */
    public synchronized void put(String key, Object value) {
        if (value == null) {
            table.remove(key);
        } else {
            table.put(key, value);
        }
        inNeedOfSaving = true;
    }




    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public String get(String key, String dflt) {
        Object value = get(key);
        if (value == null) {
            return dflt;
        }
        return value.toString();
    }



    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public Color get(String key, Color dflt) {
        Object value = get(key);
        if (value == null) {
            return dflt;
        }
        return (Color) value;
    }


    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public Font get(String key, Font dflt) {
        Object value = get(key);
        if (value == null) {
            return dflt;
        }
        return (Font) value;
    }



    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public boolean get(String key, boolean dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Boolean(value.toString()).booleanValue();
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public char get(String key, char dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return value.toString().charAt(0);
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public short get(String key, short dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Short(value.toString()).shortValue();
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public int get(String key, int dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Integer(value.toString()).intValue();
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public float get(String key, float dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Float(value.toString()).floatValue();
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public long get(String key, long dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Long(value.toString()).longValue();
    }

    /**
     *  Lookup the given key's value. If not found return the dflt.
     *  @param key The object's key.
     *  @param dflt The default value to return if not found.
     *  @return The value of the given key or the dflt if not found.
     */
    public double get(String key, double dflt) {
        Object value = get(key);
        if (value == null || value.toString().isEmpty()) {
            return dflt;
        }
        return new Double(value.toString()).doubleValue();
    }


    /**
     *  Lookup the given key's value. If not found return null.
     *  @param key The object's key.
     *  @return The value of the given key.
     */
    public Object get(Object key) {
        return get(key.toString());
    }




    /**
     *  Remove the given value from the table.
     *
     *  @param key The object's key.
     */
    public synchronized void remove(String key) {
        table.remove(key);
        inNeedOfSaving = true;
    }



    /**
     *   Save the store to disk if there has been a put since the last save
     */

    public void saveIfNeeded() {
        if (inNeedOfSaving) {
            save();
        }
    }



    /**
     *   Save the store to disk.
     */
    public void save() {
        try {
            if ( !userDirectoryOk()) {
                return;
            }
            String encodedString = getEncoder().toXml(table);
            File file = new File(resources.getWritable());
            if(encodedString==null || encodedString.trim().length()==0) {
                System.err.println("Error: trying to write 0 bytes to:" + file);
                inNeedOfSaving = false;
                return;
            }
            File bakFile = new File(resources.getWritable()+".bak");
            if(file.exists()) {
                IOUtil.copyFile(file,bakFile);
            }
            IOUtil.writeFile(resources.getWritable(),
                             encodedString);
            inNeedOfSaving = false;
        } catch (Exception exc) {
            System.out.println("XmlObjectStore save failed writing to:"
                               + resources.getWritable());
            exc.printStackTrace();
            return;
        }

    }





    /**
     *  @param args Command line args.
     */
    public static void main(String[] args) {}



}

