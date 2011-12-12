/*
 * $Id: Defaults.java,v 1.7 2006/05/05 19:19:34 jeffmc Exp $
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


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;



import java.util.Properties;


/**
 * The Defaults object is used by objects to store and retrieve
 * various default settings.
 */
public class Defaults {

    /** _more_ */
    private static Defaults defaults = null;

    /** _more_ */
    private static Properties props;

    /** _more_ */
    private static URL defaultsFile = null;

    /**
     * _more_
     *
     * @param defaultsFile
     *
     */
    private Defaults(URL defaultsFile) {
        this.defaultsFile = defaultsFile;
        props             = null;
        props             = new Properties();
        try {
            props.load(new BufferedInputStream(defaultsFile.openStream()));
        } catch (IOException e) {
            System.err.println("loadDefaults: " + e);
        }
    }

    /**
     * Method for accessing a Defaults object.  Ensures only one exists.
     * initialize or reset must be called first.
     *
     * @return a single instance of Defaults
     * @see #initialize
     * @see #reset
     *
     * @throws Exception
     */
    public static Defaults getInstance() throws Exception {
        if (defaults == null) {
            throw new Exception("Defaults not initialized. Must "
                                + "use intialize method first");
        }
        return defaults;
    }

    /**
     * Method for creating or reinitializing a Defaults object.
     * To load the system defaults, use Defaults.DEFAULTS_FILE
     * as the filename or use <code>reset</code> method.
     *
     *
     * @param defaultsFile
     * @return a single instance of Defaults
     * @see #reset
     */
    public static Defaults initialize(URL defaultsFile) {
        defaults = new Defaults(defaultsFile);
        return defaults;
    }

    /**
     * Method for creating or reinitializing a Defaults object.
     * To load the system defaults, use Defaults.DEFAULTS_FILE
     * as the filename or use <code>reset</code> method.
     *
     *
     * @param defaultsFile
     * @return a single instance of Defaults
     * @see #reset
     */
    public static Defaults initialize(String defaultsFile) {
        URL defaultURL = Defaults.class.getResource(defaultsFile);
        defaults = new Defaults(defaultURL);
        return defaults;
    }

    /**
     * Reset the defaults using the system defaults file.
     *
     * @return a new defaults.
     */
    public static Defaults reset() {
        return initialize(defaultsFile);
    }

    // Methods for retrieving and updating defaults

    /**
     * Searches for the property with the specified key in the
     * defaults list.  If the key is not found in this
     * list, the method returns <code>null</code>.
     *
     * @param  key  the defaults key.
     * @return the value in the defaults list with the specified key value
     *         or <code>null</code>.
     */
    public static String getDefault(String key) {
        return props.getProperty(key);
    }

    /**
     * Searches for the property with the specified key in the
     * defaults list.  If the key is not found in this list,
     * the method returns the default value argument.
     *
     * @param  key            the defaults key.
     * @param  defaultValue   the default value to return if the key
     *                        does not exist
     * @return the value in the defaults list with the specified key value
     *         or the defaultValue.
     */
    public static String getDefault(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Updates or adds a property to the defaults list.  If the key does
     * not exist, it is added to the list.
     *
     * @param  key    the defaults key to update or add
     * @param  value  the value of the property
     */
    public static void putDefault(String key, String value) {
        props.put(key, value);
    }

    /**
     * _more_
     */
    public void printProperties() {
        props.list(System.out);
    }
}

