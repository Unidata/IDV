/*
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


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;
import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.security.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.jar.*;
import java.util.regex.*;



/**
 * Class PluginClassLoader. Loads plugin classes
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.54 $
 */
public class PluginClassLoader extends ClassLoader {

    /** for url plugins */
    public static final String PLUGIN_PROTOCOL = "idvresource";

    /** Mapping from path name to class */
    private Hashtable loadedClasses = new Hashtable();

    /** The jar file we are loading from */
    private JarFile myJarFile;

    /** path to jar file */
    private String jarFilePath;

    /** Mapping of resource name to jar entry */
    Hashtable canonicalNames = new Hashtable();

    /** For handling getResource */
    private URLStreamHandler urlStreamHandler;

    /** List of non class jar entry names */
    private List entryNames = new ArrayList();


    /** The parent class loader */
    private ClassLoader parent;


    /**
     * ctor
     *
     *
     * @param jarFilePath Where the jar file is
     * @param parent  parent
     *
     * @throws IOException On badness
     */
    public PluginClassLoader(String jarFilePath,
                             ClassLoader parent)
        throws IOException {
        super(parent);
        this.jarFilePath = jarFilePath;
        this.parent      = parent;

        myJarFile        = new JarFile(jarFilePath);
        urlStreamHandler = new URLStreamHandler() {
                protected URLConnection openConnection(URL u)
                    throws IOException {
                    return openURLConnection(u);
                }
            };
        List entries = Misc.toList(myJarFile.entries());
        //First load in the class files
        for (int i = 0; i < entries.size(); i++) {
            JarEntry entry = (JarEntry) entries.get(i);
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (name.endsWith(".class")) {
                //System.err.println ("class:"+entry.getName());
                try {
                    Class c = loadClassFromJar(entry.getName());
                } catch (java.lang.LinkageError jlle) {
                    handleError("Error loading plugin class:"
                             + entry.getName(), jlle);
                }
            } else {
                defineResource(entry);
                entryNames.add(entry.getName());
            }
        }
    }

    public String toString() {
        return this.jarFilePath;
    }

    protected void handleError(String msg, Throwable exc)  {
        throw new WrapperException(msg, exc);
    }

    /**
     * Close the jar file
     */
    public void closeJar() {
        try {
            if (myJarFile != null) {
                myJarFile.close();
            }
            myJarFile = null;
        } catch (IOException exc) {}
    }

    /**
     * Get the list of (String) names of the non-class files in the jar
     *
     * @return List of jar entries
     */
    public List getEntryNames() {
        return entryNames;
    }

    /**
     * Create our own URLConnection for handling getResource
     *
     * @param u The url
     *
     * @return The connectio
     *
     * @throws IOException On badness
     */
    private URLConnection openURLConnection(final URL u)
        throws IOException {
        return new URLConnection(u) {
                public void connect() throws IOException {}

                public InputStream getInputStream() throws IOException {
                    return getResourceAsStream(u.getFile());
                }
            };
    }

    /**
     * Create if needed and return the jar file
     *
     * @return jar file
     */
    private JarFile getJarFile() {
        if (myJarFile == null) {
            try {
                myJarFile = new JarFile(jarFilePath);
            } catch (Exception exc) {
                System.err.println("caught exception:" + exc);
                throw new WrapperException("Opening jar file:"
                                           + jarFilePath, exc);
            }
        }
        return myJarFile;
    }

    /**
     * Load in the class from the jar.
     *
     *
     * @param entryName Name of entry
     *
     * @return The class.
     */
    private Class loadClassFromJar(String entryName) {
        try {
            JarEntry jarEntry = getJarFile().getJarEntry(entryName);
            Class    c = (Class) loadedClasses.get(jarEntry.getName());
            if (c != null) {
                return c;
            }
            InputStream  is    = getJarFile().getInputStream(jarEntry);
            final byte[] bytes = IOUtil.readBytes(is);
            is.close();
            c = loadClass(bytes);
            loadedClasses.put(c.getName(), c);
            loadedClasses.put(jarEntry.getName(), c);
            checkClass(c);
            return c;
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new IllegalArgumentException("Could not load class:"
                                               + entryName + "\n" + exc);
        }
    }


    protected void checkClass(Class c) throws Exception {
    }

    /**
     * Overwrite base class method to load in a class by name
     *
     * @param name class name
     *
     * @return The class
     *
     * @throws ClassNotFoundException On badness
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        //Check if we have this class as a jar entry
        Class c = (Class)  loadedClasses.get(name);
        if(c!=null) {
            return c;
        }
        String fileName = StringUtil.replace(name, ".", "/");
        fileName += ".class";
        JarEntry jarEntry = getJarFile().getJarEntry(fileName);
        if (jarEntry != null) {
            return loadClassFromJar(jarEntry.getName());
        } else {
            return super.loadClass(name);
        }
    }


    /**
     * Check if this class is one we loaded from a plugin
     *
     * @return the class or null
     */
    public Class getClassFromPlugin(String name) {
        return (Class)  loadedClasses.get(name);
    }



    /**
     * Associate the resource name with the jar entry
     *
     * @param jarEntry THe entry
     */
    protected String defineResource(JarEntry jarEntry) {
        String entryName = jarEntry.getName();
        String name      = jarEntry.getName();
        canonicalNames.put("/" + name, entryName);
        canonicalNames.put(jarFilePath + "!" + name, entryName);
        name = "/" + name;
        canonicalNames.put(jarFilePath + "!" + name, entryName);
        String path;
        canonicalNames.put(path=PLUGIN_PROTOCOL + ":" + jarFilePath + "!"
                           + name, entryName);
        canonicalNames.put(PLUGIN_PROTOCOL + ":" + name, entryName);
        return path;
    }


    /**
     * Get the actual name that is used in the jar file
     * The resource might have teh PLUGIN_PROTOCOL prepended to it, etc.
     *
     * @param resource the resource
     *
     * @return jar name
     */
    private String getCanonicalName(String resource) {
        return (String) canonicalNames.get(resource);
    }

    /**
     * Open the resource as a URL
     *
     * @param resource The resource
     *
     * @return The URL
     */
    public URL getResource(String resource) {
        String name = getCanonicalName(resource);
        if (resource.indexOf("testitout") >= 0) {
            //              System.err.println(jarFilePath+" getResource:" + resource + " name=" +name );
            //              System.err.println("names:" + canonicalNames);
        }
        if (name == null) {
            return super.getResource(resource);
        }
        try {
            return new URL(PLUGIN_PROTOCOL, "", -1, resource,
                           urlStreamHandler);
        } catch (Exception exc) {
            return null;
        }
    }


    /**
     * Open the resource as a istream if we have it
     *
     * @param resource The resource
     *
     * @return The istream
     */
    public InputStream getResourceAsStream(String resource) {
        String jarEntryName = getCanonicalName(resource);
        if (jarEntryName != null) {
            try {
                JarFile jarFile = getJarFile();
                return jarFile.getInputStream(
                                              jarFile.getJarEntry(jarEntryName));
            } catch (Exception exc) {}
        }
        return null;
    }


    /**
     * Load class bytes
     *
     * @param bytes class bytes
     *
     * @return New class
     */
    private Class loadClass(byte[] bytes) {
        PermissionCollection pc = new Permissions();
        pc.add(new AllPermission());
        CodeSource codeSource =
            new CodeSource((URL) null,
                           (java.security.cert.Certificate[]) null);
        ProtectionDomain pd = new ProtectionDomain(codeSource, pc);
        return defineClass((String) null, bytes, 0, bytes.length, pd);
    }
}

