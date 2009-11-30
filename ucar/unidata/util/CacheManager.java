/*
 * $Id: CacheManager.java,v 1.15 2007/04/12 18:55:39 jeffmc Exp $
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


import java.awt.event.*;

import java.io.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * A static class to manage a set of data caches.  Each object
 * could have a cache of data for specific
 * data objects associated with it.  This class is a central
 * manager for all the caches.  Effectively, it is a Hashtable of
 * Hashtables.  The key for the managed cache is the owner
 * of the cached data.  The value for the owner key is a Hashtable
 * of cached Data objects.
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class CacheManager {

    /** Where we cache data */
    private static File cacheDir;

    /** The tmp dir */
    private static File tmpDir;


    /** Do we actually do any caching */
    private static boolean doCache = true;

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            CacheManager.class.getName());

    /** mutex */
    private static Object MUTEX = new Object();

    /** cache of owned caches */
    private static Hashtable caches = new Hashtable();

    /** initialization flag */
    private static boolean haveInitialized = false;

    /** for unique file names */
    private static int fileCount = 0;

    /** The max file cache size */
    private static long maxCacheSize = Integer.MAX_VALUE;


    /** The current file cache size */
    private static long currentCacheSize = -1;


    /** List of action listeners to notify when we clear the cache */
    private static List cacheListeners = new ArrayList();

    /**
     * Default constructor; does nothing.
     */
    public CacheManager() {}


    /**
     * Set whether we do caching. If we aren't doing caching then clear the cache.
     *
     * @param cache Do we do caching
     */
    public static void setDoCache(boolean cache) {
        doCache = cache;
        if ( !doCache) {
            clearCache();
        }
    }

    /**
     * Get whether we are caching or not
     *
     * @return true if caching
     */
    public static boolean getDoCache() {
        return doCache;
    }

    /**
     * set the cache dir
     *
     * @param dir cache dir
     */
    public static void setCacheDir(File dir) {
        cacheDir = dir;
    }


    /**
     * Set the tmp dir
     *
     * @param dir tmp dir
     */
    public static void setTmpDir(File dir) {
        tmpDir = dir;
    }


    /**
     * get tmp dir
     *
     * @return the tmp dir
     */
    public static File getTmpDir() {
        return tmpDir;
    }




    /**
     * get a tmp file
     *
     * @param prefix file prefix
     *
     * @return tmp file path
     */
    public static File getTmpFile(String prefix) {
        return new File(IOUtil.joinDir(tmpDir,
                                       prefix + "_"
                                       + System.currentTimeMillis() + "_"
                                       + (fileCount++)));
    }


    /**
     * What dir is the cache group in
     *
     * @param group cache group
     *
     * @return dir
     */
    public static File getCacheGroupDir(String group) {
        if (cacheDir == null) {
            return null;
        }
        if ( !cacheDir.exists()) {
            return null;
        }
        File f       = cacheDir;
        List subdirs = StringUtil.split(group, "/");
        for (int i = 0; i < subdirs.size(); i++) {
            f = new File(IOUtil.joinDir(f, subdirs.get(i).toString()));
            IOUtil.makeDir(f);
        }
        return f;
    }

    /**
     * Clean up filename or url
     *
     * @param name filename or url to clean
     *
     * @return cleaned name
     */
    private static String prepareCacheFilename(String name) {
        return StringUtil.replaceList(name, new String[] {
            "http:", ":", "/", "\\", "?", "&", ",", "="
        }, new String[] {
            "", "_", "_", "_", "_", "_", "_", "_"
        });
    }

    /**
     * Get the cached file under the group id if it exists
     *
     * @param group group
     * @param id id
     *
     * @return file bytes or null
     */
    public static File getCachedFilePath(String group, String id) {
        File f = getCacheGroupDir(group);
        if (f == null) {
            return null;
        }
        id = prepareCacheFilename(id);
        f  = new File(IOUtil.joinDir(f, id));
        return f;
    }




    /**
     * Get the cached file under the group id if it exists
     *
     * @param group group
     * @param id id
     *
     * @return file bytes or null
     */
    public static byte[] getCachedFile(String group, String id) {
        try {
            File f = getCachedFilePath(group, id);
            if (f == null) {
                return null;
            }
            if ( !f.exists()) {
                return null;
            }
            f.setLastModified(System.currentTimeMillis());
            return IOUtil.readBytes(new FileInputStream(f));
        } catch (Exception exc) {
            throw new IllegalArgumentException("Error reading cache:" + exc);
        }
    }


    /**
     * Set the max limit on the file cache size
     *
     * @param maxSize Max cache size
     */
    public static void setMaxFileCacheSize(long maxSize) {
        maxCacheSize = maxSize;
        checkCacheSize();
    }


    /**
     * Write the cached file
     *
     * @param group group
     * @param id id
     * @param bytes bytes
     */
    public static void putCachedFile(String group, String id, byte[] bytes) {
        try {
            if ( !doCache) {
                return;
            }
            File f = getCacheGroupDir(group);
            if (f == null) {
                return;
            }
            id = prepareCacheFilename(id);
            f  = new File(IOUtil.joinDir(f, id));
            IOUtil.writeBytes(f, bytes);
            currentCacheSize = getCacheSize() + bytes.length;
            checkCacheSize();
        } catch (Exception exc) {
            throw new IllegalArgumentException("Error reading cache:" + exc);
        }
    }


    /**
     * Remove files in the cache if we have blown past our limit
     */
    private static void checkCacheSize() {
        currentCacheSize = getCacheSize();
        if (currentCacheSize > maxCacheSize) {
            File[] allFiles = IOUtil.sortFilesOnAge(
                                  IOUtil.toFiles(
                                      IOUtil.getFiles(
                                          cacheDir, true)), false);
            int fileIdx = 0;
            //Go a bit below the limit so we don't thrash
            long limit = (long) (maxCacheSize * 0.75);
            while ((currentCacheSize > limit)
                    && (fileIdx < allFiles.length)) {
                currentCacheSize -= allFiles[fileIdx].length();
                allFiles[fileIdx].delete();
                fileIdx++;
            }
            currentCacheSize = findCacheSize();
        }
    }

    /**
     * This sets the cache size if it has not been set yet and then returns it
     *
     * @return cache size
     */
    private static long getCacheSize() {
        if (currentCacheSize < 0) {
            currentCacheSize = findCacheSize();
        }
        return currentCacheSize;
    }

    /**
     * This searches the cache dir and calculates the total size of the files there
     *
     * @return Cache size
     */
    private static int findCacheSize() {
        if (cacheDir == null) {
            return 0;
        }
        List files = IOUtil.getFiles(cacheDir, true);
        int  size  = 0;
        for (int i = 0; i < files.size(); i++) {
            size += ((File) files.get(i)).length();
        }
        return size;
    }




    /**
     * Initialize the cache
     */
    private static void init() {
        if (haveInitialized) {
            return;
        }
        haveInitialized = true;
        // Create the memory monitor and get flagged when we go 
        // over 90 % available memory
        //FORNOW: Don't do this
        //        MemoryMonitor mm = new MemoryMonitor(90);
    }

    /**
     * Put an object in the cache
     *
     * @param owner   owner of the object
     * @param key     key for the cached object
     * @param value   value for key
     */
    public static void put(Object owner, Object key, Object value) {
        put(owner, key, value, false);
    }

    /**
     * Put an object in the cache
     *
     * @param owner   owner of the object
     * @param key     key for the cached object
     * @param value   value for key
     * @param force   true to cache even if doCache is false
     */
    public static void put(Object owner, Object key, Object value,
                           boolean force) {
        synchronized (MUTEX) {
            if (doCache || force) {
                Hashtable ht = findOrCreate(owner);
                ht.put(key, value);
            }
        }
    }

    /**
     * Clear the cache.
     */
    public static void clearCache() {
        synchronized (MUTEX) {
            //      System.err.println ("clear cache");
            caches = new Hashtable();

            for (int i = 0; i < cacheListeners.size(); i++) {
                ActionListener al = (ActionListener) cacheListeners.get(i);
                al.actionPerformed(new ActionEvent("CacheManager", 0,
                        "cacheclear"));

            }
        }
    }

    /**
     * Add a listener that gets called when we clear the full cache
     *
     * @param a cache listener
     */
    public static void addCacheListener(ActionListener a) {
        cacheListeners.add(a);
    }


    /**
     * Find the cache associated with <code>owner</code> and if not there,
     * create a new cache
     *
     * @param owner   owner to search for
     * @return  caching table
     */
    public static Hashtable findOrCreate(Object owner) {
        synchronized (MUTEX) {
            return find(owner, true);
        }
    }

    /**
     * Find the cache associated with the owner.  Do not create a
     * cache if one does not exist.
     *
     * @param owner    owner for cache
     * @return  associated cache  or <code>null</code>
     */
    private static Hashtable find(Object owner) {
        return find(owner, false);
    }

    /**
     * Find the cache associated with the owner of the cache.  If
     * <code>orCreate</code>, then create a cache if one does not
     * exist
     *
     * @param owner     owner associated with the cache
     * @param orCreate  true to create one if one does not exist.
     * @return  a data cache for the owner
     */
    private static Hashtable find(Object owner, boolean orCreate) {
        init();
        Hashtable ht = (Hashtable) caches.get(owner);
        if ((ht == null) && orCreate) {
            //      System.err.println ("Creating new ht:" + owner);
            ht = new Hashtable();
            caches.put(owner, ht);
        }
        return ht;
    }

    /**
     * Get the cached object.
     *
     * @param owner    cache owner
     * @param key      key within the cache
     * @return  the cached object
     */
    public static Object get(Object owner, Object key) {
        return get(owner, key, false);
    }

    /**
     * Get the cached object.
     *
     * @param owner    cache owner
     * @param key      key within the cache
     * @param force    true to force a lookup
     * @return  the cached object
     */
    public static Object get(Object owner, Object key, boolean force) {
        synchronized (MUTEX) {
            if (doCache || force) {
                Hashtable ht = find(owner);
                if (ht == null) {
                    return null;
                }
                return ht.get(key);
            } else {
                return null;
            }
        }
    }


    /**
     * Remove the cache associated with the <code>owner</code>.
     *
     * @param owner    owner of the cache
     */
    public static void remove(Object owner) {
        synchronized (MUTEX) {
            caches.remove(owner);
        }
    }

    /**
     * Remove a particular item from the owner's cache.
     *
     * @param owner   owner of the cache
     * @param key     key for object to remove
     */
    public static void remove(Object owner, Object key) {
        synchronized (MUTEX) {
            Hashtable ht = find(owner);
            if (ht != null) {
                ht.remove(key);
            }
        }
    }

    /**
     * Print the statistics for this cache.  Glorified toString().
     */
    public static void printStats() {
        System.err.println("\tCacheManager: #caches:" + caches.size());
        for (Enumeration keys = caches.keys(); keys.hasMoreElements(); ) {
            Object    key   = keys.nextElement();
            Hashtable cache = (Hashtable) caches.get(key);
            System.err.println("\tKey:" + key + " size:" + cache.size());
        }
    }


}

