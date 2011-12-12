/*
 * $Id: SerializedObjectStore.java,v 1.16 2007/03/05 15:10:15 dmurray Exp $
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



import java.io.*;

import java.util.*;


/**
 *  Implements the PersistentStore interface for metApps persistent objects.
 *
 *  This class stores serialized objects in a HashMap.  All objects passed to it must
 *  implement the Serializable interface.<br><br>
 *
 *
 * @author caron
 * @version $Revision: 1.16 $
 */

public class SerializedObjectStore implements PersistentStore {

    /** _more_ */
    private String coreFilename, siteFilename, userFilename;

    /** _more_ */
    private boolean debug                     = false,
                    debugShowSystemProperties = false;

    /** _more_ */
    private boolean permissionReadDisk = true;

    /** _more_ */
    protected boolean
        debugShowHash  = false,
        showGet        = false,
        showPut        = false,
        debugWhichRead = false;

    /** _more_ */
    protected HashMap hash = new HashMap();

    /**
     * _more_
     *
     */
    protected SerializedObjectStore() {}

    /**
     * Constructor.
     * You can subclass to implement other strategies.
     *
     * In this default implementation, the following files are looked for (in sequence), and
     * if found, are read into the HashMap. Thus, Objects in the later files override ones in the
     * earlier files. If a file doesnt exist, it is skipped.
     * <pre>
     *    /data/config/&lt;app&gt;/&lt;storeName&gt;.ser               CORE configuration files in jar file
     *    $METAPPS_HOME/config/&lt;app&gt;/&lt;storeName&gt;.ser       SITE configuration files
     *    $USER_HOME/<sysName>&lt;app&gt;/&lt;storeName&gt;.ser       USER configuration files
     * where:
     *     $METAPPS_HOME:  check for system property "metapps.home", if none, use current directory
     *     $USER_HOME:  check for system property "user.home"; if none, use current directory
     *
     * Notes:
     *     Check for system property using System.getProperty("property")
     *     Set system property on command line: java -Dmetapps.home=$METAPPS_HOME
     *     An applet can read only from the jar file.
     * </pre>
     *
     *   @param systemName system name.
     *   @param appName application name.
     *   @param storeName store name.
     */
    /* public SerializedObjectStore(String appName, String storeName) {
      this( "metapps", appName, storeName);
    } */

    public SerializedObjectStore(String systemName, String appName,
                                 String storeName) {

        /* read in "core" configuration files. since these are stored in the jar file,
           this will succeed even for an applet with no disk access permissions  */
        coreFilename = new String("/auxdata/config/" + appName + "/"
                                  + storeName + ".ser");
        readConfigFile(true, coreFilename);

        /* read in "site" configuration files */
        String siteHome = getProperty(systemName + ".home");
        if (null == siteHome) {
            siteHome = ".";
        }
        if (debug) {
            System.out.println(systemName + "home = " + siteHome);
        }
        siteFilename = new String(siteHome + "/config/" + appName + "/"
                                  + storeName + ".ser");
        readConfigFile(false, siteFilename);
        if ( !permissionReadDisk) {  // give up now if we cant access disk
            return;
        }

        // create user directory if need be
        String userHome = getProperty("user.home");
        if (null == userHome) {
            userHome = ".";
        }
        if (debug) {
            System.out.println("user home = " + userHome);
        }
        if (debug) {
            System.out.println("java.class.path = "
                               + getProperty("java.class.path"));
        }

        String path = userHome + File.separator + "." + systemName;
        File   f    = new File(path);
        if ( !f.exists()) {
            if ( !f.mkdir()) {
                System.out.println("Configure failed to make directory "
                                   + path);
            } else if (debug) {
                System.out.println("Configure made directory " + f.getPath());
            }
        }
        path = path + File.separator + appName;
        f    = new File(path);
        if ( !f.exists()) {
            if ( !f.mkdir()) {
                System.out.println("Configure failed to make directory "
                                   + path);
            } else if (debug) {
                System.out.println("Configure made directory " + f.getPath());
            }
        }

        // read in user config file
        userFilename = new String(f.getPath() + File.separator + storeName
                                  + ".ser");
        readConfigFile(false, userFilename);
    }

    /**
     * get the value named by the key
     *
     * @param key
     * @return _more_
     */
    public Object get(Object key) {
        Object val = hash.get(key);
        if (showGet || Debug.isSet("util.ObjectStore")) {
            System.out.print("util.ObjectStoreGet key= " + key + " value= "
                             + val);
            if (val != null) {
                System.out.println(" " + val.getClass().getName());
            }
            System.out.println();
        }
        return val;
    }

    /**
     *  Wrapper method that retrieves a boolean value form the store
     *  If the value does not exist this returns the dflt parameter
     *
     * @param key
     * @param dflt
     * @return _more_
     */
    public boolean get(Object key, boolean dflt) {
        Boolean b = (Boolean) get(key);
        return ((b != null)
                ? b.booleanValue()
                : dflt);
    }



    /* Store the (key, value) pair.
     * @exception IllegalArgumentException if key or value not Serializable.
     */

    /**
     * _more_
     *
     * @param key
     * @param value
     */
    public void put(Object key, Object value) {
        if (showPut || Debug.isSet("util.ObjectStore")) {
            System.out.print("util.ObjectStorePut key= " + key + " value= "
                             + value);
            if (value != null) {
                System.out.println(" " + value.getClass().getName());
            }
            System.out.println();
        }
        if ( !(key instanceof Serializable)) {
            throw new IllegalArgumentException(
                "SerializedObjectStore.put non-serializable object " + key);
        }
        if ( !(value instanceof Serializable)) {
            throw new IllegalArgumentException(
                "SerializedObjectStore.put non-serializable object " + value);
        }
        hash.put(key, value);
    }

    /** save the objects to disk */
    public void save() {
        writeConfigData(userFilename, hash);
    }

    /**
     * _more_
     *
     * @param key
     * @return _more_
     */
    private String getProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            System.out.println("ObjectStore: not allowed to get Property "
                               + key);
        }
        return null;
    }

    /* private HashMap readConfigData(String filename) {
      HashMap hm = null;

      File f = new File(filename);
      if (! f.exists()) {
        if (debug) System.out.println( "ObjectStore didnt find " + filename+ " ; will create.");
        return null;
      }

        // read in the serialized objects
      try {
          FileInputStream fp = new FileInputStream(filename);
          ObjectInputStream in =  new ObjectInputStream(fp);

          hm = (HashMap) in.readObject();
          in.close();
          if (debug) System.out.println("Configure read ok on "+ filename);

      } catch (FileNotFoundException e) {
          System.out.println("Configure couldnt find file "+ filename+ "; will create it");
          return null;
      } catch (StreamCorruptedException e) {
          System.out.println("Configure found corrupted file "+ filename+ "; will delete and recreate");
          return null;
      } catch (Exception e) {
          System.out.println("Configure fatal exception on file "+ filename);
          System.out.println("    exception = "+ e);
          return null;
      }

      return hm;
    } */

    /**
     * _more_
     *
     * @param isCore
     * @param filename
     */
    protected void readConfigFile(boolean isCore, String filename) {
        ObjectInputStream in;

        if (debugWhichRead) {
            System.out.println("ObjectStore try to open " + filename);
        }

        try {
            if (isCore) {
                InputStream ins =
                    ucar.unidata.util.Resource.getFileResource(null,
                        filename);
                if (null == ins) {
                    if (debugWhichRead) {
                        System.out.println("    getFileResource failed on "
                                           + filename);
                    }
                    return;
                }
                in = new ObjectInputStream(ins);
            } else {
                in = new ObjectInputStream(new FileInputStream(filename));
            }
        } catch (FileNotFoundException e) {
            if (debugWhichRead) {
                System.out.println("    FileNotFound on " + filename);
            }
            return;
        } catch (IOException e) {
            if (debugWhichRead) {
                System.out.println("ObjectInputStream open failed on "
                                   + filename);
                System.out.println("   exception = " + e);
            }
            return;
        } catch (SecurityException e) {
            System.out.println("ObjectStore: not allowed to read disk");
            permissionReadDisk = false;
            return;
        }

        if (readObjectsFromStream(in)) {
            if (debugWhichRead) {
                System.out.println("   success on " + filename);
            }
        } else {
            if (debugWhichRead) {
                System.out.println("ObjectStore readConfigFile failed on "
                                   + filename);
            }
        }

        try {
            in.close();
        } catch (IOException e) {
            System.out.println("ObjectStore readConfigData failed to close "
                               + filename);
            System.out.println("   exception = " + e);
        }
    }

    /**
     * _more_
     *
     * @param in
     * @return _more_
     */
    protected boolean readObjectsFromStream(ObjectInputStream in) {
        if (debugShowHash) {
            System.out.println("ObjectStore store ");
        }

        // we went through a lot of trouble to try to allow some objects to fail,
        // but it seems that "any Exception is fatal"
        try {
            while (true) {
                Object key   = in.readObject();
                Object value = in.readObject();
                hash.put(key, value);
                if (debugShowHash) {
                    System.out.println("  " + key + " == " + hash.get(key));
                }
            }                       // while
        } catch (EOFException e) {  // ok
        } catch (IOException e) {
            System.out.println("readObjectsFromStream IOException " + e);
        } catch (ClassNotFoundException e) {
            System.out.println(
                "readObjectsFromStream ClassNotFoundException " + e);
        }

        return true;
    }

    /**
     * _more_
     *
     * @param filename
     * @param hm
     */
    private void writeConfigData(String filename, HashMap hm) {
        ObjectOutputStream out;
        if (debugShowHash) {
            System.out.println("saveConfigData Hash " + userFilename);
        }

        if (null == filename) {
            return;
        }

        try {
            out = new ObjectOutputStream(new FileOutputStream(filename));
        } catch (IOException e) {
            System.out.println("ObjectStore writeConfigData failed to open "
                               + filename);
            System.out.println("   exception = " + e);
            return;
        }

        writeObjectsToStream(out);
        if (debug) {
            System.out.println("Configure saved data in " + filename);
        }
    }

    /**
     * _more_
     *
     * @param out
     */
    protected void writeObjectsToStream(ObjectOutputStream out) {
        if (null == hash) {
            return;
        }
        Set keys = hash.keySet();
        if (null == keys) {
            return;
        }

        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            try {
                out.writeObject(key);
                out.writeObject(hash.get(key));
                if (debugShowHash) {
                    System.out.println("  " + key + " == " + hash.get(key));
                }
            } catch (IOException e) {
                System.out.println(
                    "ObjectStore writeObjectsToStream failed to write "
                    + key);
                System.out.println("   exception = " + e);
            }
        }

        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println(
                "ObjectStore writeObjectsToStream failed to close ");
            System.out.println("   exception = " + e);
        }
    }


    /**
     * _more_
     *
     * @param hm
     */
    protected void printHashMap(HashMap hm) {
        if (null == hm) {
            return;
        }
        Set keys = hm.keySet();
        if (null == keys) {
            return;
        }
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            System.out.println("  " + key + " == " + hm.get(key));
        }
    }
}

/*
 *  Change History:
 *  $Log: SerializedObjectStore.java,v $
 *  Revision 1.16  2007/03/05 15:10:15  dmurray
 *  fix last javadoc checkin
 *
 *  Revision 1.15  2007/03/05 14:24:55  dmurray
 *  fix some javadoc error that were causing the javadoc portion of the build
 *  to fail.
 *
 *  Revision 1.14  2006/05/05 19:19:37  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.13  2005/05/13 18:32:44  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.12  2004/08/19 21:34:47  jeffmc
 *  Scratch log4j
 *
 *  Revision 1.11  2004/02/27 21:18:54  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.10  2004/01/29 17:37:42  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.9  2001/11/19 17:00:44  jeffmc
 *  Added a wraper method around get to return a boolean value (with a default if
 *  the object not found in the store).
 *
 *  Revision 1.8  2001/02/08 00:50:53  caron
 *  debug Debug
 *
 *  Revision 1.7  2001/01/25 01:13:10  caron
 *  clean up jnlp
 *
 *  Revision 1.6  2000/12/14 20:38:37  caron
 *  remove outdated constructor
 *
 *  Revision 1.5  2000/08/18 04:16:12  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.4  2000/08/18 04:01:56  russ
 *  Removed c**pyrights.
 *
 *  Revision 1.3  2000/08/17 20:44:44  caron
 *  remove c**yrights
 *
 *  Revision 1.2  2000/08/03 16:34:44  caron
 *  cleanup and checkin for GDV release
 *
 *  Revision 1.1  2000/05/26 21:19:38  caron
 *  new GDV release
 *
 *  Revision 1.5  2000/04/26 21:14:04  caron
 *  latest version of GDV
 *
 *  Revision 1.4  2000/02/07 18:01:06  caron
 *  better debugging
 *
 *  Revision 1.3  1999/06/03 01:44:20  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:22  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:50  caron
 *  startAgain
 *
 * # Revision 1.4  1999/03/26  19:58:47  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.3  1999/03/18  18:21:24  caron
 * # bug fixes
 * #
 * # Revision 1.2  1999/03/16  17:01:24  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.1  1999/03/08  19:46:26  caron
 * # world coord now Point2D
 * #
 */






