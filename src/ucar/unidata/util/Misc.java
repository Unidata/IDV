/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import java.awt.Color;
import java.awt.Dimension;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.text.DecimalFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import java.util.regex.*;



/**
 * Static class of miscellaneous methods
 *
 * @author IDV development group.
 *
 * @version $Revision: 1.271 $
 */
public class Misc {

    /** We needed some place to put this */
    private static PrototypeManager prototypeManager;

    /** debug flag */
    public static boolean debug = false;

    /** Holds a list of any custom class loaders used in the app */
    private static List<ClassLoader> classLoaders =
        new ArrayList<ClassLoader>();

    /** override current time */
    private static long overrideCurrentTime = -1;

    /** missing string */
    public static final String MISSING = "missing";

    /** NaN string */
    public static final String NaN = "NaN";

    /** Default constructor; does nothing */
    public Misc() {}




    /**
     *  Set the global prototype manager. We needed some palce to put this
     * method.
     *
     * @param prototypeManager The prototype manager
     */
    public static void setPrototypeManager(
            PrototypeManager prototypeManager) {
        if (Misc.prototypeManager != null) {
            throw new IllegalStateException(
                "Only allow one prototype manager");
        }
        Misc.prototypeManager = prototypeManager;
    }

    /**
     * Get the prototype manager
     *
     * @return The prototype manager
     */
    public static PrototypeManager getPrototypeManager() {
        return prototypeManager;
    }


    /**
     * Get the current time
     *
     * @return    the time in milliseconds
     */
    public static long getCurrentTime() {
        if (overrideCurrentTime != -1) {
            return overrideCurrentTime;
        }
        return System.currentTimeMillis();
    }



    /**
     * Set the current time
     *
     * @param date  the date to use
     */
    public static void setCurrentTime(Date date) {
        overrideCurrentTime = date.getTime();
    }





    /**
     * A method for converting an object to a Double.    No check is made
     * to see if this is a Number or not.  This will not handle a localized
     * String representation of a floating point number. Use
     * {@link #parseNumber(String)} instead.
     *
     * @param o   object in question
     * @return  a double value
     */
    public static double toDouble(Object o) {
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        return Double.parseDouble(o.toString());
    }



    /**
     * Decodes a string representation of a latitude or longitude and
     * returns a double version (in degrees).  Acceptible formats are:
     * <pre>
     * +/-  ddd:mm, ddd:mm:, ddd:mm:ss, ddd::ss, ddd.fffff ===>   [+/-] ddd.fffff
     * +/-  ddd, ddd:, ddd::                               ===>   [+/-] ddd
     * +/-  :mm, :mm:, :mm:ss, ::ss, .fffff                ===>   [+/-] .fffff
     * +/-  :, ::                                          ===>       0.0
     * Any of the above with N,S,E,W appended
     * </pre>
     *
     * @param latlon  string representation of lat or lon
     * @return the decoded value in degrees
     */
    public static double decodeLatLon(String latlon) {
        // first check to see if there is a N,S,E,or W on this
        latlon = latlon.trim();
        int    dirIndex    = -1;
        int    southOrWest = 1;
        double value       = Double.NaN;
        if (latlon.indexOf("S") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("S");
        } else if (latlon.indexOf("W") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("W");
        } else if (latlon.indexOf("N") > 0) {
            dirIndex = latlon.indexOf("N");
        } else if (latlon.endsWith("E")) {  // account for 9E-3, 9E-3E, etc
            dirIndex = latlon.lastIndexOf("E");
        }

        if (dirIndex > 0) {
            latlon = latlon.substring(0, dirIndex).trim();
        }

        // now see if this is a negative value
        if (latlon.indexOf("-") == 0) {
            southOrWest *= -1;
            latlon      = latlon.substring(latlon.indexOf("-") + 1).trim();
        }

        if (latlon.indexOf(":") >= 0) {  //have something like DD:MM:SS, DD::, DD:MM:, etc
            int    firstIdx = latlon.indexOf(":");
            String hours    = latlon.substring(0, firstIdx);
            String minutes  = latlon.substring(firstIdx + 1);
            String seconds  = "";
            if (minutes.indexOf(":") >= 0) {
                firstIdx = minutes.indexOf(":");
                String temp = minutes.substring(0, firstIdx);
                seconds = minutes.substring(firstIdx + 1);
                minutes = temp;
            }
            try {

                value = (hours.equals("") == true)
                        ? 0
                        : Double.parseDouble(hours);
                if ( !minutes.equals("")) {
                    value += Double.parseDouble(minutes) / 60.;
                }
                if ( !seconds.equals("")) {
                    value += Double.parseDouble(seconds) / 3600.;
                }
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        } else {  //have something like DD.ddd
            try {
                value = parseNumber(latlon);
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        }
        return value * southOrWest;
    }




    /**
     * Reverse an array
     *
     *
     * @param l The list
     *
     * @return reversed array
     */
    public static List reverseList(List l) {
        ArrayList l2 = new ArrayList();
        for (Object o : l) {
            l2.add(0, o);
        }
        return l2;
    }



    /**
     * Reverse an array
     *
     * @param fromArray   array to reverse
     * @param toArray     reversed array
     *
     * @return reversed array
     */
    public static Object[] reverseArray(Object[] fromArray,
                                        Object[] toArray) {
        if (toArray == null) {
            toArray = new Object[fromArray.length];
        }
        for (int i = 0; i < fromArray.length; i++) {
            toArray[i] = fromArray[fromArray.length - i - 1];
        }

        return toArray;
    }




    /**
     *  Return the end part of the class name
     *
     *  @param c The class
     *  @return The end part of the class name
     */
    public static String getClassName(Class c) {
        String f   = c.getName();
        int    idx = f.lastIndexOf(".");
        if (idx < 0) {
            return f;
        }
        return f.substring(idx + 1);
    }




    /**
     *  Use reflection to find the Method with name "set" + Name.
     *  If found then convert the given value to the appropriate type and invoke the method.
     *
     *  @param object The object to invoke the set property method on.
     *  @param name The name of the method.
     *  @param value The Strign representation of the value to set.
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static void propertySet(Object object, String name, String value)
            throws IllegalAccessException, IllegalArgumentException,
                   InvocationTargetException {
        propertySet(object, name, value, false);
    }





    /**
     *  Use reflection to find the Method with name "set" + Name.
     *  If found then convert the given value to the appropriate type and invoke the method.
     *
     *  @param object The object to invoke the set property method on.
     *  @param name The name of the method.
     *  @param value The String representation of the value to set.
     * @param ignoreError If true then don't print out an error
     *
     *
     * @return Did we successfully set the property
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static boolean propertySet(Object object, String name,
                                      String value, boolean ignoreError)
            throws IllegalAccessException, IllegalArgumentException,
                   InvocationTargetException {

        return setProperty(object, name, value, ignoreError);
    }


    /**
     * For the given property name get the set method name, e.g., returns:<pre>
     * setPropertname
     * </pre>
     *
     * @param prop property name
     *
     * @return setter method
     */
    public static String getSetterMethod(String prop) {
        return "set" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
    }


    /**
     * Set a property on a "bean"
     *
     * @param object   object to set property on
     * @param name     name of the property.  object must have set<Name> method.
     * @param objectValue value of the property
     * @param ignoreError true to ignore errors
     *
     * @return true if successful
     *
     * @throws IllegalAccessException    security violation
     * @throws IllegalArgumentException  illegal argument
     * @throws InvocationTargetException  invocation problem
     */
    public static boolean setProperty(Object object, String name,
                                      Object objectValue, boolean ignoreError)
            throws IllegalAccessException, IllegalArgumentException,
                   InvocationTargetException {

        String methodName = "set" + name.substring(0, 1).toUpperCase()
                            + name.substring(1);
        Method method = findMethod(object.getClass(), methodName,
                                   new Class[] { null });
        if (method == null) {
            if ( !ignoreError) {
                System.err.println("could not find method:" + methodName
                                   + ": on class:"
                                   + object.getClass().getName());
            }
            return false;
        }
        return setProperty(object, method, objectValue, ignoreError);
    }

    /**
     * Set a property on a "bean"
     *
     * @param object   object to set property on
     * @param method   method to call
     * @param objectValue value of the method
     * @param ignoreError true to ignore errors
     *
     * @return true if successful
     *
     * @throws IllegalAccessException    security violation
     * @throws IllegalArgumentException  illegal argument
     * @throws InvocationTargetException  invocation problem
     */
    public static boolean setProperty(Object object, Method method,
                                      Object objectValue, boolean ignoreError)
            throws IllegalAccessException, IllegalArgumentException,
                   InvocationTargetException {

        if (method == null) {
            if ( !ignoreError) {
                System.err.println("Method can't be null");
            }
            return false;
        }
        Object argument  = null;
        Class  paramType = method.getParameterTypes()[0];
        if (paramType.equals(objectValue.getClass())) {
            argument = objectValue;
        } else if (paramType.isAssignableFrom(objectValue.getClass())) {
            argument = objectValue;
        } else {
            String value = objectValue.toString();
            if (paramType.equals(Integer.class)) {
                argument = new Integer(value);
            } else if (paramType.equals(String.class)) {
                argument = value;
            } else if (paramType.equals(ContourInfo.class)) {
                argument = new ContourInfo(value);
            } else if (paramType.equals(Range.class)) {
                int idx = value.indexOf(":");
                if (idx >= 0) {
                    argument = new Range(
                        new Double(value.substring(0, idx)).doubleValue(),
                        new Double(value.substring(idx + 1)).doubleValue());
                }
            } else if (paramType.equals(Double.TYPE)) {
                argument = new Double(value);
            } else if (paramType.equals(Integer.TYPE)) {
                argument = new Integer(value);
            } else if (paramType.equals(Float.TYPE)) {
                argument = new Float(value);
            } else if (paramType.equals(Boolean.TYPE)) {
                argument = new Boolean(value);
            } else if (paramType.equals(Rectangle2D.Float.class)) {
                String[] toks = StringUtil.split(value, ",", 4);
                argument = new Rectangle2D.Float(Float.parseFloat(toks[0]),
                        Float.parseFloat(toks[1]), Float.parseFloat(toks[2]),
                        Float.parseFloat(toks[3]));

            } else if (paramType.equals(Rectangle2D.Double.class)) {
                String[] toks = StringUtil.split(value, ",", 4);
                argument =
                    new Rectangle2D.Double(Double.parseDouble(toks[0]),
                                           Double.parseDouble(toks[1]),
                                           Double.parseDouble(toks[2]),
                                           Double.parseDouble(toks[3]));
            } else if (paramType.equals(Color.class)) {
                argument = GuiUtils.decodeColor(value, Color.white);
            } else if (paramType.equals(Dimension.class)) {
                int idx = value.indexOf(":");
                if (idx >= 0) {
                    argument = new Dimension(new Integer(value.substring(0,
                            idx)).intValue(), new Integer(value.substring(idx
                            + 1)).intValue());
                }
            } else if (paramType.equals(Object.class)) {
                argument = value;
            }
        }
        if (argument == null) {
            if ( !ignoreError) {
                System.err.println("Misc.propertySet: Unknown type:"
                                   + paramType.getName());
            }
        }
        if (argument != null) {
            method.invoke(object, new Object[] { argument });
            return true;
        }
        return false;


    }


    /**
     *  Try to decode the given stringValue as an integer. If it fails return the dflt.
     *
     *  @param stringValue The integer string value.
     *  @param dflt The dflt to return if the decode fails
     *  @return The decoded int value.
     */
    public static int getInt(String stringValue, int dflt) {
        if (stringValue != null) {
            try {
                dflt = Integer.decode(stringValue).intValue();
            } catch (Exception exc) {}
        }
        return dflt;
    }


    /**
     *  Try to decode the given stringValue as an boolean. If it fails return the dflt.
     *
     *  @param stringValue The boolean string value.
     *  @param dflt The dflt to return if the decode fails
     *  @return The decoded boolean value.
     */

    public static boolean getBoolean(String stringValue, boolean dflt) {
        if (stringValue != null) {
            try {
                dflt = new Boolean(stringValue).booleanValue();
            } catch (Exception exc) {}
        }
        return dflt;
    }



    /**
     *  Copy the objects with the given objectArray into a new Vector.
     *
     *  @param objectArray The array of objects.
     *  @return The Vector that contains the objects from the array.
     */
    public static Vector toVector(Object[] objectArray) {
        Vector v = new Vector();
        for (int i = 0; i < objectArray.length; i++) {
            v.add(objectArray[i]);
        }
        return v;
    }

    /**
     * Make the list unique
     *
     * @param l initial list
     *
     * @return unique list
     */
    public static List makeUnique(List l) {
        List      result = new ArrayList();
        Hashtable ht     = new Hashtable();
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (ht.get(o) != null) {
                continue;
            }
            ht.put(o, o);
            result.add(o);
        }
        return result;
    }


    /**
     *  Copy the objects with the given objectArray into a new List.
     *
     *  @param l     The array of objects.
     *
     *  @return The List that contains the objects from the array.
     */
    public static List toList(Object[] l) {
        ArrayList v = new ArrayList();
        for (int i = 0; i < l.length; i++) {
            v.add(l[i]);
        }
        return v;
    }


    /**
     * Create a list from an enumeration
     *
     * @param enumeration The enumeration
     *
     * @return The list
     */
    public static List toList(Enumeration enumeration) {
        List list = new ArrayList();
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }


    /**
     *  Copy the objects with the given objectArray into a new List.
     *
     *  @param l     The array of objects.
     *
     *  @return The List that contains the objects from the array.
     */
    public static List newList(Object[] l) {
        return toList(l);
    }


    /**
     * Utility to do a list get only if the index is ok. Else return null.
     *
     * @param l List
     * @param index Index
     *
     * @return List value or null
     */
    public static Object safeGet(List l, int index) {
        if (l == null) {
            return null;
        }
        if (index < l.size()) {
            return l.get(index);
        }
        return null;
    }


    /**
     * Create and return a List that holds Integers starting at start
     * to end by step.
     *
     * @param start      The start value
     * @param end        The end value.
     * @param step       The step
     *
     * @return List of Integers.
     */
    public static List createIntervalList(int start, int end, int step) {
        List l = new ArrayList();
        for (int i = start; i <= end; i += step) {
            l.add(new Integer(i));
        }
        return l;
    }

    /**
     *  Sort the given List. Note: The return type may vary.
     *
     *  @param listToSort       The list to sort.
     *  @return A new List, sorted.
     */
    public static List sort(Collection listToSort) {
        Object[] array = listToSort.toArray();
        Arrays.sort(array);
        return Arrays.asList(array);
    }

    /**
     * The elements in the given list object arrays. The sort is done on
     * the first element of of the array.
     *
     * @param pairs   list to sort
     * @param ascending  true for ascending sort
     *  @return A new List, sorted.
     */
    public static List sortTuples(List pairs, final boolean ascending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Object[] a1     = (Object[]) o1;
                Object[] a2     = (Object[]) o2;
                int      result = ((Comparable) a1[0]).compareTo(a2[0]);
                if (ascending || (result == 0)) {
                    return result;
                }
                return -result;
            }

            public boolean equals(Object obj) {
                return obj == this;
            }
        };

        Object[] array = pairs.toArray();
        Arrays.sort(array, comp);
        return Arrays.asList(array);
    }




    /**
     * Create a Properties table from the given semi-colon delimited attribute
     * string. The propString is of the form: "name1=value1;name2=value2;"
     *
     *  @param propString             The property String.
     *  @return The properties table.
     */
    public static Properties parseProperties(String propString) {
        return parseProperties(propString, ";");
    }


    /**
     * Create a Properties table from the given semi-colon delimited attribute
     * string. The propString is of the form: "name1=value1;name2=value2;"
     *
     *  @param propString             The property String.
     * @param delimiter The delimiter between each  name/value pair
     *  @return The properties table.
     */
    public static Properties parseProperties(String propString,
                                             String delimiter) {
        Properties p = new Properties();
        if (propString == null) {
            return p;
        }
        StringTokenizer tok = new StringTokenizer(propString, delimiter);
        while (tok.hasMoreTokens()) {
            String nameValue = tok.nextToken();
            int    idx       = nameValue.indexOf("=");
            if (idx < 0) {
                continue;
            }
            p.put(nameValue.substring(0, idx).trim(),
                  nameValue.substring(idx + 1).trim());
        }
        return p;
    }



    /**
     * Look up the String prop in the given set of properties.
     * If it exists then  return it. Else return the given dflt.
     *
     * @param p           property table
     * @param prop        property to look up.
     * @param dflt        default value
     * @return  existing property value or default if it doesn't exist
     */
    public static String getProperty(Hashtable p, String prop, String dflt) {
        if (p == null) {
            return dflt;
        }
        Object v = p.get(prop);
        if (v != null) {
            return v.toString();
        }
        return dflt;
    }


    /**
     * Look up the prop in the given set of properties.
     * If it exists convert it to a Color and return it.
     * Else return the given dflt.
     *
     * @param props     properties table
     * @param prop      property name
     * @param dflt      default color
     * @return  a color defined by the property value or the default
     */
    public static Color getProperty(Hashtable props, String prop,
                                    Color dflt) {
        if (props == null) {
            return dflt;
        }
        Object v = props.get(prop);
        if (v == null) {
            return dflt;
        }
        return Color.decode(v.toString());
    }

    /**
     * Look up the prop in the given set of Hashtable.
     * If it exists convert it to a boolean and return it.
     * Else return the given dflt.
     *
     * @param props     table of properties
     * @param prop      property name
     * @param dflt      default value
     * @return   value converted to a boolean or the default
     */
    public static boolean getProperty(Hashtable props, String prop,
                                      boolean dflt) {
        if (props == null) {
            return dflt;
        }
        Object v = props.get(prop);
        if (v == null) {
            return dflt;
        }
        return new Boolean(v.toString()).booleanValue();
    }

    /**
     * Look up the prop in the given set of Hashtable.
     * If it exists convert it to a int and return it.
     * Else return the given dflt.
     *
     * @param props     table of properties
     * @param prop      property name
     * @param dflt      default value
     * @return   value converted to an int or the default
     */
    public static int getProperty(Hashtable props, String prop, int dflt) {
        if (props == null) {
            return dflt;
        }
        Object v = props.get(prop);
        if (v == null) {
            return dflt;
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            System.err.println("Number format exception: " + prop + " " + v);
            return dflt;
        }
    }

    /**
     *  Look up the prop in the given set of Hashtable.
     *  If it exists convert it to a float and return it.
     *  Else return the given dflt.
     *
     * @param props     table of properties
     * @param prop      property name
     * @param dflt      default value
     * @return   value converted to a float or the default
     */
    public static float getProperty(Hashtable props, String prop,
                                    float dflt) {
        if (props == null) {
            return dflt;
        }
        Object v = props.get(prop);
        if (v == null) {
            return dflt;
        }
        try {
            return Float.parseFloat(v.toString());
        } catch (NumberFormatException e) {
            System.err.println("Number format exception: " + prop + " " + v);
            return dflt;
        }
    }


    /**
     * get a double property from the props table
     *
     * @param props the props
     * @param prop the prop name
     * @param dflt the default value if none found
     *
     * @return the value
     */
    public static double getProperty(Hashtable props, String prop,
                                     double dflt) {
        if (props == null) {
            return dflt;
        }
        Object v = props.get(prop);
        if (v == null) {
            return dflt;
        }
        try {
            return Float.parseFloat(v.toString());
        } catch (NumberFormatException e) {
            System.err.println("Number format exception: " + prop + " " + v);
            return dflt;
        }
    }


    /**
     * Read the given property filename, defined in relation to the given Class.
     * If the given Properties object is null create a new one.
     *
     * @param filename The property filename
     * @param properties Where to put the properties.
     * @param origin Where to look for the properties file.
     *
     * @return The properties object.
     */
    public static Properties readProperties(String filename,
                                            Properties properties,
                                            Class origin) {
        InputStream s = null;
        try {
            s = IOUtil.getInputStream(filename, origin);
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Could not open  property file:" + filename);
        }

        if (properties == null) {
            properties = new Properties();
        }
        try {
            properties.load(s);
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Could not open  property file:" + filename + " Exception:"
                + exc);
        }
        return properties;
    }


    /**
     * Find the url with the given name as a resource relative to the given
     * class.  Look up the class hierarchy until found.
     *
     * @param name         The resource name
     * @param origin         Where to look.
     *
     * @return The URL or null if not found.
     */
    public static URL getURL(String name, Class origin) {
        URL url = null;
        while (origin != null) {
            url = origin.getResource(name);
            if (url != null) {
                break;
            }
            origin = origin.getSuperclass();
        }
        return url;
    }


    /**
     *  Create a url argument String with the given args. Example:
     *  "?arg1=value1&arg2=value2"
     *
     *  @param base         The base of the url.
     *  @param args         The name/value pairs.
     *  @return The contructed url string.
     */
    public static String appendUrlArgs(String base, String[] args) {
        return appendUrlArgs(base, args, true);
    }


    /**
     *  Create a url argument String with the given args. Example:
     *  "?arg1=value1&arg2=value2"
     *
     *  @param base                 The base of the url.
     *  @param args                 The name/value pairs. This may be null.
     *  @param addQuestionMark      Do we start by appending a "?"
     *  @return The constructed url string.
     */
    public static String appendUrlArgs(String base, String[] args,
                                       boolean addQuestionMark) {
        StringBuffer sb = new StringBuffer(base);
        if (args != null) {
            for (int i = 0; i < args.length; i += 2) {
                if (i == 0) {
                    if (addQuestionMark) {
                        sb.append("?");
                    }
                } else {
                    sb.append("&");
                }
                sb.append(args[i]);
                sb.append("=");
                sb.append(args[i + 1]);
            }
        }
        return sb.toString();

    }


    /**
     * Create a URL String with the given protocol, server, url and url
     * arguments. The result is:    "protocol://server/urlRoot?args"
     *
     * @param protocol         The url protocol.
     * @param server           The server.
     * @param urlRoot          The root of the url path.
     * @param args             Any url arguments. This may be null.
     * @return   the constructed URL string
     */
    public static String makeUrl(String protocol, String server,
                                 String urlRoot, String[] args) {
        StringBuffer sb = new StringBuffer(protocol);
        sb.append("://");
        sb.append(server);
        sb.append(urlRoot);
        return appendUrlArgs(sb.toString(), args);
    }


    /**
     * Run the {@link Runnable}.
     *
     * @param r  Runnable to run
     * @see #runInABit
     */
    public static void run(final Runnable r) {
        runInABit(1, r);
    }



    /**
     *  Call object.runMethod in a different thread
     *
     *
     * @param object The  object
     * @param methodName The method
     */
    public static void run(final Object object, String methodName) {
        Misc.runInABit(0, object, methodName, null);
    }



    /**
     *  Call object.runMethod in a different thread. If arg is non-null
     * pass it to the method
     *
     *
     * @param object The  object
     * @param methodName The method
     * @param arg The arg
     */
    public static void run(final Object object, String methodName,
                           final Object arg) {
        Misc.runInABit(0, object, methodName, arg);
    }


    /**
     *  Call object.runMethod in a different thread after a ms millisecond delay
     *
     * @param ms   delay (milliseconds)
     * @param object The object
     * @param methodName The method
     * @param arg The arg
     */
    public static void runInABit(final long ms, final Object object,
                                 String methodName, final Object arg) {
        try {
            final Method theMethod = ((arg == null)
                                      ? Misc.findMethod(object.getClass(),
                                          methodName, new Class[] {})
                                      : Misc.findMethod(object.getClass(),
                                          methodName,
                                          new Class[] { arg.getClass() }));

            if (theMethod == null) {
                throw new IllegalArgumentException("Could not find method:"
                        + object.getClass().getName() + "." + methodName);
            }
            runInABit(ms, new Runnable() {
                public void run() {
                    try {
                        if (arg == null) {
                            theMethod.invoke(object, new Object[] {});
                        } else {
                            theMethod.invoke(object, new Object[] { arg });
                        }
                    } catch (Exception exc) {
                        System.err.println("Error in Misc.run:" + exc);
                        exc.printStackTrace();
                    }
                }
            });
        } catch (Exception exc) {
            System.err.println("Error in Misc.run:" + exc);
            exc.printStackTrace();
        }
    }


    /**
     *  Call r.run () in a different thread after a ms millisecond delay
     *
     * @param ms   delay (milliseconds)
     * @param r    Runnable to run
     */
    public static void runInABit(final long ms, final Runnable r) {
        Thread t = new Thread() {
            public void run() {
                try {
                    this.sleep(ms);
                    r.run();
                } catch (Exception exc) {
                    //                    LogUtil.logException("", exc);
                }
            }
        };
        t.start();
    }


    /**
     * Post the form data
     *
     * @param action   post action
     * @param data     data to post
     * @return  posted data
     */
    public static String doPost(String action, String data) {
        return doPost(action, data, null);
    }


    /**
     * Post the form data
     *
     * @param action   post action
     * @param data     data to post
     * @param attrs    data attributes
     * @return  posted data
     */
    public static String doPost(String action, String data, String[] attrs) {
        Object[] result = doPostAndGetConnection(action, data, attrs);
        if (result == null) {
            return null;
        }
        return (String) result[1];
    }


    /**
     * Do an http post with the given action, writing the given data
     * as the post data.
     *
     * @param action   post action
     * @param data     data to post
     * @return    returned data
     */
    public static Object[] doPostAndGetConnection(String action,
            String data) {
        return doPostAndGetConnection(action, data, null);
    }


    /**
     *  Do an http post with the given action, writing the given data
     *  as the post data.
     *
     * @param action   post action
     * @param data     data to post
     * @param attrs    data attributes
     * @return    returned data
     */
    public static Object[] doPostAndGetConnection(String action, String data,
            String[] attrs) {
        HttpURLConnection urlConn = null;
        try {
            URL url = new URL(action);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod("POST");
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            if (attrs != null) {
                for (int i = 0; i < attrs.length; i += 2) {
                    urlConn.setRequestProperty(attrs[i], attrs[i + 1]);
                }
            }
            //      System.err.println ("data:" + data);
            DataOutputStream printout =
                new DataOutputStream(urlConn.getOutputStream());
            BufferedReader input;
            printout.writeBytes(data);
            printout.flush();
            printout.close();
            input = new BufferedReader(
                new InputStreamReader(urlConn.getInputStream()));
            StringBuffer result = new StringBuffer();
            String       str;
            while (null != ((str = input.readLine()))) {
                result.append(str);
            }
            input.close();
            return new Object[] { urlConn, result.toString() };
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            try {
                System.err.println("UrlC:" + urlConn.getResponseMessage());
            } catch (Exception exc2) {}
            return new Object[] { urlConn, null };
        }
    }

    /**
     *  Looks up the named  value in the props hashtable.
     *  This  does a hash lookup of the key. If a value is found it iterates
     *  across that value looking for all occurrences of the string
     *  "macro.[a-zA-Z0-9]+" it then does a second lookup in the hashtable
     *  with the macro name, substiting the found value in the result string.
     *
     * @param key       key for value
     * @param props     property table to look up
     * @return   the value, or <code>null</code>
     */
    public static String getValue(String key, Hashtable props) {
        String value = (String) props.get(key);
        if (value == null) {
            return null;
        }
        String returnValue = "";
        //Substitute all "macro.somename" with the corresponding hashed value
        while (true) {
            int idx1 = value.indexOf("macro.");
            if (idx1 < 0) {
                break;
            }
            returnValue += value.substring(0, idx1);
            value       = value.substring(idx1);
            int idx2   = 0;
            int length = value.length();
            while (idx2 < length) {
                char c = value.charAt(idx2);
                if ((c != '.') && !Character.isLetterOrDigit(c)) {
                    break;
                }
                idx2++;
            }
            String macroKey = value.substring(0, idx2);
            value = value.substring(idx2);
            String macroValue = (String) props.get(macroKey.trim());
            if (macroValue != null) {
                returnValue += macroValue;
            }
        }
        returnValue += value;
        return returnValue;
    }




    /** baseTime */
    private static String baseTime;

    /** cout of objects */
    private static int objectCount = 0;


    /**
     * Gets a (sort of) unique String identifier. We use the system time
     * (in milliseconds) append with a counter. This might no be unique
     * if there is another run of the IDV that got a call to this method
     * at the exact same time.
     *
     * @return A (sortof) unique id.
     */
    public static String getUniqueId() {
        if (baseTime == null) {
            baseTime = "" + System.currentTimeMillis() + "_" + Math.random();
            //Swap out '.' for '_'
            baseTime = baseTime.replace('.', '_');
        }
        return baseTime + "_" + (++objectCount);
    }


    /**
     * Adds all of the elements held by src into the dest list
     * if they are not held in the seen Hashtable. Adds them to seen
     * when added to dest. returns the given dest List parameter.
     *
     * @param dest      The destination list.
     * @param src       The list of objects to add if they are unique.
     * @param seen      Keeps track of what objects we have seen.
     * @return   new list with unique items added
     */
    public static List addUnique(List dest, List src, Hashtable seen) {
        if (src != null) {
            for (int i = 0; i < src.size(); i++) {
                Object o = src.get(i);
                if (seen.get(o) == null) {
                    seen.put(o, o);
                    dest.add(o);
                }
            }
        }
        return dest;

    }

    /**
     *  Utility to create a new list or handle a null argument gracefully.
     *
     *  @param sourceList The source list.
     *  @return If the  given list argument is null then return null, else
     *          return a new list which holds the elements of the original list.
     */
    public static List cloneList(List sourceList) {
        if (sourceList == null) {
            return null;
        }
        return new ArrayList(sourceList);
    }



    /**
     * Create a Hashtable. Put the values held in the given array
     * into the hashtable. The argument array consists of:
     * name1,value1,name2,value2, etc.
     *
     * @param ht        The hashtable to put the values into. If
     *                  <code>null</code>, we create a new one.
     * @param o         The array of (key,value) objects.
     * @return The added to Hashtable.
     */
    public static Hashtable newHashtable(Hashtable ht, Object[] o) {
        if (ht == null) {
            ht = new Hashtable();
        }
        for (int i = 0; i < o.length; i += 2) {
            ht.put(o[i], o[i + 1]);
        }
        return ht;
    }


    /**
     * Create a new Hashtable. Put the values held in the given array
     * into the hashtable. The argument array consists of:
     * name1,value1,name2,value2, etc.
     *
     * @param o     The array of (key,value) objects.
     *
     * @return The newly created  Hashtable.
     */
    public static Hashtable newHashtable(Object[] o) {
        return newHashtable(new Hashtable(), o);
    }

    /**
     * Create a Hashtable and add the given keys and their corresponding
     * values into it.
     *
     * @param key1      key for table
     * @param value1    corresponding value
     *
     * @return A new Hashtable containg the given keys and values.
     */
    public static Hashtable newHashtable(Object key1, Object value1) {
        return newHashtable(new Object[] { key1 }, new Object[] { value1 });
    }

    /**
     * Create a Hashtable and add the given keys and their corresponding
     * values into it.
     *
     * @param key1      first key
     * @param value1    corresponding first value
     * @param key2      second key
     * @param value2    corresponding second value
     *
     * @return A new Hashtable containg the given keys and values.
     */
    public static Hashtable newHashtable(Object key1, Object value1,
                                         Object key2, Object value2) {
        return newHashtable(new Object[] { key1, key2 },
                            new Object[] { value1,
                                           value2 });
    }

    /**
     * Create a Hashtable and add the given keys and their corresponding
     * values into it.
     *
     * @param key1      first key
     * @param value1    corresponding first value
     * @param key2      second key
     * @param value2    corresponding second value
     * @param key3      third key
     * @param value3    corresponding third value
     *
     * @return A new Hashtable containg the given keys and values.
     */
    public static Hashtable newHashtable(Object key1, Object value1,
                                         Object key2, Object value2,
                                         Object key3, Object value3) {
        return newHashtable(new Object[] { key1, key2, key3 },
                            new Object[] { value1,
                                           value2, value3 });
    }


    /**
     * Create a Hashtable and add the given keys and their
     * corresponding values into it.
     *
     * @param keys     set of keys
     * @param values   set of corresponding values
     *
     * @return A new Hashtable containg the given keys and values.
     */
    public static Hashtable newHashtable(Object[] keys, Object[] values) {
        Hashtable ht = new Hashtable();
        for (int i = 0; i < keys.length; i++) {
            if ((keys[i] != null) && (values[i] != null)) {
                ht.put(keys[i], values[i]);
            }
        }
        return ht;
    }

    /**
     * Return the last object in the given list or null if the list is empty.
     *
     * @param l The list
     * @return The last object in the list.
     */
    public static Object getLast(List l) {
        if ((l == null) || (l.size() == 0)) {
            return null;
        }
        return l.get(l.size() - 1);
    }

    /**
     * Remove the last element in the given list.
     *
     * @param l the list
     */
    public static void removeLast(List l) {
        if ((l == null) || (l.size() == 0)) {
            return;
        }
        l.remove(l.size() - 1);
    }


    /**
     * Create a List and add the argument to it.
     *
     * @param o1    object to add to list
     * @return  a list with <code>object</code> in it
     */
    public static List newList(Object o1) {
        return toList(new Object[] { o1 });
    }

    /**
     * Create a List and add the arguments to it.
     *
     * @param o1   first object to add
     * @param o2   second object to add
     * @return  list with the two objects
     */
    public static List newList(Object o1, Object o2) {
        return toList(new Object[] { o1, o2 });
    }

    /**
     * Create a List and add the arguments to it.
     *
     * @param o1   first object to add
     * @param o2   second object to add
     * @param o3   third object to add
     * @return  list with the three objects
     */
    public static List newList(Object o1, Object o2, Object o3) {
        return toList(new Object[] { o1, o2, o3 });
    }

    /**
     * Create a List and add the arguments to it.
     *
     * @param o1   first object to add
     * @param o2   second object to add
     * @param o3   third object to add
     * @param o4   fourth object to add
     * @return  list with the four objects
     */
    public static List newList(Object o1, Object o2, Object o3, Object o4) {
        return toList(new Object[] { o1, o2, o3, o4 });
    }

    /**
     * Create a List and add the arguments to it.
     *
     * @param o1   first object to add
     * @param o2   second object to add
     * @param o3   third object to add
     * @param o4   fourth object to add
     * @param o5   fifth object to add
     * @return  list with the five objects
     */
    public static List newList(Object o1, Object o2, Object o3, Object o4,
                               Object o5) {
        return toList(new Object[] { o1, o2, o3, o4, o5 });
    }

    /**
     * See if the string is HTML.
     *
     * @param s   String to check
     * @return  true if it looks like HTML
     */
    public static boolean isHtml(String s) {
        s = s.trim().toLowerCase();
        if (s.startsWith("<!doctype html")) {
            return true;
        }
        if (s.indexOf("<html") >= 0) {
            return true;
        }
        return false;
    }







    /**
     * Returns true if the Classes defined in the actual parameter
     * are equal or a sub-class of the corresponding classes defined in the
     * formal argument.
     *
     * @param formals     formal classes (types)
     * @param actuals     actual classes
     * @return   true  if they match
     */
    public static boolean typesMatch(Class[] formals, Class[] actuals) {
        if (formals.length != actuals.length) {
            return false;
        }
        for (int j = 0; j < formals.length; j++) {
            if (actuals[j] == null) {
                continue;
            }
            if ( !formals[j].isAssignableFrom(actuals[j])) {
                return false;
            }
        }
        return true;
    }


    /**
     * dummy
     *
     * @param formals dummy
     * @param actuals dummy
     *
     * @return dummy
     */
    public static boolean typesMatchx(Class[] formals, Class[] actuals) {
        if (formals.length != actuals.length) {
            System.err.println("Bad length");
            return false;
        }
        for (int j = 0; j < formals.length; j++) {
            if (actuals[j] == null) {
                continue;
            }

            if ( !formals[j].isAssignableFrom(actuals[j])) {
                System.err.println("not assignable " + formals[j].getName()
                                   + " " + actuals[j].getName() + " equals? "
                                   + formals[j].equals(actuals[j]));
                return false;
            }
        }
        return true;
    }


    /**
     * Find all methods with the given name.
     * Of these methods find one whose parameter types
     * are assignable from the given parameter types.
     *
     * @param c            class to check
     * @param methodName   name of method
     * @param paramTypes   parameter types
     * @return  class method or <code>null</code> if one doesn't exist
     */
    public static Method findMethod(Class c, String methodName,
                                    Class[] paramTypes) {
        ArrayList all     = new ArrayList();
        Method[]  methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if ( !methodName.equals(methods[i].getName())) {
                continue;
            }
            if (paramTypes == null) {
                return methods[i];
            }
            if (typesMatch(methods[i].getParameterTypes(), paramTypes)) {
                all.add(methods[i]);
            }
        }
        if (all.size() > 1) {
            String msg = "More than one method: " + methodName
                         + " found for class:" + c.getName();
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] != null) {
                    msg += " " + paramTypes[i].getName();
                }
            }
            throw new IllegalArgumentException(msg);
        }
        if (all.size() == 1) {
            return (Method) all.get(0);
        }
        return null;
    }


    /**
     *  Utility method that returns the ClassName.methodName part of the
     *  (possibly) full package.ClassName.methodName path.
     *
     * @param path      full path to check
     * @return  ClassName.methodName portion of the full path
     */
    public static String getClassMethod(String path) {
        //Get the index of the first and last "."
        int i1 = path.lastIndexOf(".");
        int i2 = path.indexOf(".");
        //If the indices are the same then we have the form ClassName.methodName
        //just return it
        if (i1 == i2) {
            return path;
        }

        //Here we have a full package.ClassName.methodName, e.g., ucar.foo.ClassName.methodName
        String methodName = path.substring(i1 + 1);
        path = path.substring(0, i1);
        String className = path.substring(path.lastIndexOf(".") + 1);
        return className + "." + methodName;
    }




    /**
     * Return a list of Integer indexes, the indexes that each value
     * object is in the all values list.
     *
     * @param values The values we look up
     * @param allValues The list that we look into
     *
     * @return The list of indices.
     */
    public static List getIndexList(List values, List allValues) {
        List indices = new ArrayList();
        if ((values == null) || (allValues == null)) {
            return indices;
        }
        for (int i = 0; i < values.size(); i++) {
            int index = allValues.indexOf(values.get(i));
            if (index < 0) {
                continue;
            }
            indices.add(new Integer(index));
        }
        return indices;
    }


    /**
     * Return a list of objects from the allValues list that are at the
     * indices contained by the given indices list.
     *
     * @param indices A list of Integer indexes into the allValues list.
     * @param allValues The source of the values
     *
     * @return The values within allValues at the given indices.
     */
    public static List getValuesFromIndices(List indices, List allValues) {
        List valueList = new ArrayList();
        if ((indices == null) || (allValues == null)) {
            return valueList;
        }
        for (int i = 0; i < indices.size(); i++) {
            int index = ((Integer) indices.get(i)).intValue();
            if ((index >= 0) && (index < allValues.size())) {
                valueList.add(allValues.get(index));
            }
        }
        return valueList;
    }


    /**
     * Find all constructors of the given class
     * Of these methods find one whose parameter types
     * are assignable from the given parameter types.
     *
     * @param c             class to check
     * @param paramTypes    constructor parameter classes
     * @return   constructor  that matches or <code>null</code> if none match
     */
    public static Constructor findConstructor(Class c, Class[] paramTypes) {
        ArrayList     allCtors     = new ArrayList();
        Constructor[] constructors = c.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            if (typesMatch(constructors[i].getParameterTypes(), paramTypes)) {
                allCtors.add(constructors[i]);
            }
        }
        if (allCtors.size() > 1) {
            throw new IllegalArgumentException(
                "More than one constructors matched for class:"
                + c.getName());
        }
        if (allCtors.size() == 1) {
            return (Constructor) allCtors.get(0);
        }
        return null;
    }





    /**
     * Read the given system property. Return it if found.
     * If not found then return the given dflt
     *
     * @param key        key for property
     * @param dflt       default value
     * @return   property value or default if not defined
     */
    public static String getSystemProperty(String key, String dflt) {
        try {
            String result = System.getProperty(key);
            if (result == null) {
                result = dflt;
            }
            return result;
        } catch (SecurityException e) {
            System.out.println("ObjectStore: not allowed to get Property "
                               + key);
        }
        return dflt;
    }


    /**
     * Serialize the given object into its byte array form
     *
     * @param object     object to serialize
     * @return  serialized byte array
     */
    public static byte[] serialize(Serializable object) {
        try {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            ObjectOutputStream    p       = new ObjectOutputStream(ostream);
            p.writeObject(object);
            p.flush();
            ostream.close();
            byte[] bytes = ostream.toByteArray();
            return bytes;
        } catch (Exception exc) {
            System.err.println("Error serializing:" + exc);
        }
        return null;
    }


    /**
     *  Deserialize the given byte array into an object
     *
     * @param bytes   serialized object bytes
     * @return  Object
     */
    public static Object deserialize(byte[] bytes) {
        try {
            ByteArrayInputStream istream   = new ByteArrayInputStream(bytes);
            ObjectInputStream    p         = new ObjectInputStream(istream);
            Object               theObject = p.readObject();
            return theObject;
        } catch (Exception exc) {
            System.err.println("Error deserializing:" + exc);
        }
        return null;
    }

    /** default decimal formatter */
    static DecimalFormat formatter = new DecimalFormat();

    /** decimal formatter for 1 digit after the decimal place */
    static DecimalFormat format0 = new DecimalFormat("0.#");

    /** decimal format for scientific notation */
    static DecimalFormat format1 = new DecimalFormat("0.#E0");
    //new DecimalFormat("#.00000");

    /** decimal format for 4 places after the decimal point */
    static DecimalFormat format2 = new DecimalFormat("#.00##");

    /** decimal format for 3 places after the decimal point */
    static DecimalFormat format3 = new DecimalFormat("#.0##");

    /** decimal format for 3.1 format */
    static DecimalFormat format4 = new DecimalFormat("###.#");

    /** decimal format for 4.1 format */
    static DecimalFormat format5 = new DecimalFormat("####.#");

    /** decimal format for 5.1 format */
    static DecimalFormat format6 = new DecimalFormat("#####.#");

    /** decimal format for scientific notation of large numbers */
    static DecimalFormat format7 = new DecimalFormat("0.###E0");

    /**
     * Return different pre-defined DecimalFormat objects depending
     * on the value of the given double
     *
     * @param v  valut in question
     * @return   appropriate formatter
     */
    public static DecimalFormat getDecimalFormat(double v) {
        v = Math.abs(v);
        if (v == 0.0) {
            return format0;
        }
        if (v < 0.01) {
            return format1;
        }
        if (v < 0.1) {
            return format2;
        }
        if (v < 1.0) {
            return format3;
        }
        if (v < 10.0) {
            return format4;
        }
        if (v > 999999.0) {
            return format7;
        }
        if (v > 9999.0) {
            return format6;
        }
        if (v > 999.0) {
            return format5;
        }
        return format4;
    }

    /**
     * Provide a consistent String formatting of the given double value
     * @param value  value to format
     * @return  formatted value
     */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return MISSING;
        }
        return getDecimalFormat(value).format(value);
    }


    /**
     * Provide a consistent parsing that takes into account localization
     * of the String format of the given numeric value
     *
     * @param value String representation of a number.  Use double since
     *              that is the highest common denominator
     *
     * @return  parsed value as a double
     * @deprecated  replaced by {@link #parseNumber(String)}
     *
     * @throws NumberFormatException  unable to parse the value
     */
    public static double parseValue(String value)
            throws NumberFormatException {
        return parseNumber(value);
    }

    /**
     * Provide a consistent parsing that takes into account localization
     * of the String format of the given numeric value
     *
     * @param value String representation of a number.  Use double since
     *              that is the highest common denominator
     *
     * @return  parsed value as a double
     *
     * @throws NumberFormatException  unable to parse the value
     */
    public static double parseNumber(String value)
            throws NumberFormatException {
        if (value.equals(MISSING) || value.equals(NaN)) {
            return Double.NaN;
        }
        try {
            return formatter.parse(value).doubleValue();
        } catch (ParseException pe) {
            throw new NumberFormatException(pe.getMessage());
        }
    }


    /**
     * Return the hashcode for the object.  Accounts for null objects
     * by returning 1.
     *
     * @param o1  object in question
     *
     * @return  <code>o1</code>-s hashCode or 1 if <code>o1</code> is null
     */
    public static int hashcode(Object o1) {
        return ((o1 == null)
                ? 1
                : o1.hashCode());
    }

    /**
     * Does an o1.equals (o2) if both objects are non-null.
     * Else if one or both are null then returns
     * whether they are both null.
     *
     * @param o1  first <code>Object</code> in question
     * @param o2  second <code>Object</code> in question
     *
     * @return true if both objects are non-null and they are equal,
     *              or if both are null, otherwise false if only
     *              one is null or they are non-null, but not equal.
     */
    public static boolean equals(Object o1, Object o2) {
        if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        }
        return ((o1 == null) && (o2 == null));
    }

    /**
     * Compares two object-s toString() methods to see if they are
     * equal, ignoring case. Uses same logic as equals(o1,o2) for
     * handling null-objects.
     *
     * @param o1  first object to compare
     * @param o2  second object to compare
     *
     * @return true if both are non-null and their toString() methods
     *         are equal.
     */
    public static boolean equalsIgnoreCase(Object o1, Object o2) {
        if ((o1 != null) && (o2 != null)) {
            return o1.toString().equalsIgnoreCase(o2.toString());
        }
        return ((o1 == null) && (o2 == null));
    }



    /**
     * Checks to see if the object o is in the given Hashtable.
     * If it is then return true. If not then add the object to the
     * hasthable and return false
     *
     * @param o    object in question
     * @param map  map of seen objects
     * @return  true if o is in map
     */
    public static boolean haveSeen(Object o, Hashtable map) {
        if (o == null) {
            return false;
        }
        if (map.get(o) != null) {
            return true;
        }
        map.put(o, o);
        return false;
    }

    /**
     * The inverse of haveSeen
     *
     * @param o    object in question
     * @param map  map of seen objects
     * @return  true if o is not in map
     */
    public static boolean haveNotSeen(Object o, Hashtable map) {
        return !haveSeen(o, map);
    }


    /**
     * Print out the values in an int array.
     *
     * @param prefix prefix to append to output
     * @param array  array to print
     */
    public static void printArray(String prefix, Object[] array) {
        System.out.println(prefix + ((array == null)
                                     ? " null"
                                     : StringUtil.toString(array)));
    }


    /**
     * Print out the values in an int array.
     *
     * @param prefix  prefix string
     * @param array  array to print
     */
    public static void printArray(String prefix, byte[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        buf.append(": ");
        if (array == null) {
            buf.append(" null ");
        } else {
            for (int i = 0; i < array.length; i++) {
                buf.append("[");
                buf.append(i);
                buf.append("]: ");
                buf.append(array[i]);
                buf.append(" ");
            }
        }
        System.out.println(buf.toString());
    }


    /**
     * Print out the values in an int array.
     *
     * @param prefix  prefix string
     * @param array  array to print
     */
    public static void printArray(String prefix, int[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        buf.append(": ");
        if (array == null) {
            buf.append(" null ");
        } else {
            for (int i = 0; i < array.length; i++) {
                buf.append("[");
                buf.append(i);
                buf.append("]: ");
                buf.append(array[i]);
                buf.append(" ");
            }
        }
        System.out.println(buf.toString());
    }

    /**
     * Print out the values in a float array.
     *
     * @param prefix  prefix string
     * @param array  array to print
     */
    public static void printArray(String prefix, float[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        buf.append(": ");
        if (array == null) {
            buf.append(" null ");
        } else {
            for (int i = 0; i < array.length; i++) {
                buf.append("[");
                buf.append(i);
                buf.append("]: ");
                buf.append(array[i]);
                buf.append(" ");
            }
        }
        System.out.println(buf.toString());
    }

    /**
     * Print out the values in a double array.
     *
     * @param prefix  prefix string
     * @param array  array to print
     */
    public static void printArray(String prefix, double[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        buf.append(": ");
        if (array == null) {
            buf.append(" null ");
        } else {
            for (int i = 0; i < array.length; i++) {
                buf.append("[");
                buf.append(i);
                buf.append("]: ");
                buf.append(array[i]);
                buf.append(" ");
            }
        }
        System.out.println(buf.toString());
    }

    /**
     * Print out the values in a boolean array.
     *
     * @param prefix  prefix string
     * @param array  array to print
     */
    public static void printArray(String prefix, boolean[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        buf.append(": ");
        if (array == null) {
            buf.append(" null ");
        } else {
            for (int i = 0; i < array.length; i++) {
                buf.append("[");
                buf.append(i);
                buf.append("]: ");
                buf.append(array[i]);
                buf.append(" ");
            }
        }
        System.out.println(buf.toString());
    }




    /**
     * Print the members of an Object
     *
     * @param o  Object in question
     */
    public static void printMembers(Object o) {
        Class   c      = o.getClass();

        Field[] fields = c.getFields();
        System.err.println("Fields:");
        for (int i = 0; i < fields.length; i++) {
            try {
                System.err.println("\t" + fields[i].getName() + "="
                                   + fields[i].get(o));
            } catch (Exception exc) {
                System.err.println("\t" + fields[i].getName()
                                   + "= no access");
            }
        }

        System.err.println("Methods:");
        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            String name = methods[i].getName();
            if ( !name.startsWith("get")) {
                continue;
            }
            try {
                System.err.println("\t" + name + "="
                                   + methods[i].invoke(o, new Object[] {}));
            } catch (Exception exc) {
                System.err.println("\t" + name + "= no access");
            }
        }
    }

    /**
     * Print the stack trace for a given line of code.
     *
     * @param msg        message to print
     * @param maxLines   number of lines in the stack to print
     * @param onlyIfTraceContainsThisString  if true, only print if it
     *                                       contains this String
     */
    public static void printStack(String msg, int maxLines,
                                  String onlyIfTraceContainsThisString) {
        String trace = getStackTrace();
        if (onlyIfTraceContainsThisString != null) {
            if (trace.indexOf(onlyIfTraceContainsThisString) < 0) {
                return;
            }
        }
        if (msg != null) {
            System.out.println(msg);
        }
        StringTokenizer tok    = new StringTokenizer(trace, "\n");
        int             allcnt = 0;
        int             cnt    = 0;
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            allcnt++;
            if (allcnt > 4) {
                System.out.println(line);
                cnt++;
                if (cnt > maxLines) {
                    break;
                }
            }
        }
    }

    /**
     * Print the stack trace for a given line of code.
     *
     * @param msg        message to print
     * @param maxLines   number of lines in the stack to print
     */
    public static void printStack(String msg, int maxLines) {
        printStack(msg, maxLines, null);
    }

    /**
     * Print the stack trace for a given line of code.
     *
     * @param msg        message to print
     */
    public static void printStack(String msg) {
        printStack(msg, Integer.MAX_VALUE);
    }

    /**
     * Get the amount of used memory
     * @return  amount of memory (total-free)
     */
    public static long usedMemory() {
        return (Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory());
    }

    /**
     * Run the garbage collector.
     * @return   used memory
     * @see #usedMemory()
     */
    public static long gc() {
        Thread t = Thread.currentThread();
        for (int i = 0; i < 5; i++) {
            Runtime.getRuntime().gc();
            try {
                t.sleep(400);
            } catch (Exception exc) {}
        }
        return usedMemory();
    }


    /**
     * Pause the current Thread for a specific number of seconds.
     *
     * @param seconds  seconds to pause
     */
    public static void sleepSeconds(long seconds) {
        try {
            Thread.currentThread().sleep(seconds * 1000);
        } catch (Throwable exc) {}
    }



    /**
     * Pause the current Thread for a specific number of milliseconds.
     *
     * @param ms  milliseconds to pause
     */
    public static void sleep(long ms) {
        try {
            Thread.currentThread().sleep(ms);
        } catch (Throwable exc) {}
    }




    /**
     * Utility method to print an exception and exit
     *
     * @param exc   exception that was thrown
     */
    public static void fatal(Throwable exc) {
        System.err.println("An error  has occurred:");
        if (exc != null) {
            exc.printStackTrace();
        }
        System.exit(1);
    }


    /**
     * Utility method to print an error and exit
     *
     * @param msg     message to print
     */
    public static void fatal(String msg) {
        System.err.println("An error  has occurred:" + msg);
        System.exit(1);
    }

    /**
     * Convert double array to float array
     *
     * @param d double array
     *
     * @return float array
     */
    public static float[] toFloat(double[] d) {
        float[] f = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (float) d[i];
        }
        return f;
    }


    /**
     * Convert double array to float array
     *
     * @param d double array
     *
     * @return float array
     */
    public static double[] toDouble(float[] d) {
        double[] f = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (double) d[i];
        }
        return f;
    }


    /**
     * Convert int array to double array
     *
     * @param d input
     *
     * @return double array
     */
    public static double[][] toDouble(int[][] d) {
        double[][] f = new double[d.length][];
        for (int i = 0; i < f.length; i++) {
            f[i] = toDouble(d[i]);
        }
        return f;
    }


    /**
     * Convert int array to double array
     *
     * @param d input
     *
     * @return double array
     */
    public static double[] toDouble(int[] d) {
        double[] f = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (double) d[i];
        }
        return f;
    }



    /**
     * convert float array to double array
     *
     * @param d float array
     *
     * @return double array
     */
    public static double[] arrayToDouble(float[] d) {
        double[] f = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (float) d[i];
        }
        return f;
    }

    /**
     * Convert to float
     *
     * @param d input
     *
     * @return float
     */
    public static float[][] toFloat(double[][] d) {
        float[][] f = new float[d.length][];
        for (int i = 0; i < f.length; i++) {
            f[i] = toFloat(d[i]);
        }
        return f;
    }


    /**
     * Convert to float
     *
     * @param d input
     *
     * @return float
     */
    public static double[][] toDouble(float[][] d) {
        double[][] f = new double[d.length][];
        for (int i = 0; i < f.length; i++) {
            f[i] = toDouble(d[i]);
        }
        return f;
    }



    /**
     * Parse the double value specified by the string s.
     * If s == null or s == "NaN" return Double.NaN.
     * String must be in the form of the a floating point number as
     * defined in 3.10.2 of the Java Language Specification.
     *
     * @param s    parse the String representation of a double, must
     *             conform to the form of a floating point number as
     *             defined in 3.10.2 of the Java Language Specification.
     * @return  value as a double.
     */
    public static double parseDouble(String s) {
        if (s == null) {
            return Double.NaN;
        }
        s = s.trim();
        if (s.equals("NaN")) {
            return Double.NaN;
        }
        return Double.parseDouble(s);
    }


    /**
     * Average the array. This ignores NaNs. If all NaNs or values array empty return NaN
     *
     * @param values values to average
     *
     * @return the average.
     */
    public static float getAverage(float[] values) {
        if ((values == null) || (values.length == 0)) {
            return Float.NaN;
        }
        int   size  = values.length;
        int   total = 0;
        float all   = 0;
        for (int i = 0; i < size; i++) {
            if (values[i] == values[i]) {
                total++;
                all = all + values[i];
            }
        }
        if (total == 0) {
            return Float.NaN;
        }
        return (float) all / total;
    }



    /**
     * Parse the float value specified by the string s.
     * If s == null or s == "NaN" return Float.NaN
     * String must be in the form of the a floating point number as
     * defined in 3.10.2 of the Java Language Specification.
     *
     * @param s    parse the String representation of a float, must
     *             conform to the form of a floating point number as
     *             defined in 3.10.2 of the Java Language Specification.
     * @return  value as a float.
     */
    public static float parseFloat(String s) {
        if (s == null) {
            return Float.NaN;
        }
        s = s.trim();
        if (s.equals("NaN")) {
            return Float.NaN;
        }
        return Float.parseFloat(s);
    }


    /**
     * The given sourceString is a comma separated list of values, this method
     * parses this String, converting it into an array of doubles.
     *
     * @param sourceString The comma separated list.
     * @return Array of doubles.
     */
    public static double[] parseDoubles(String sourceString) {
        return parseDoubles(sourceString, ",");
    }


    /**
     * The given sourceString is a <code>delimiter</code> separated list
     * of doubles. This method parses this String, converting it into an
     * array of doubles.
     *
     * @param sourceString The comma separated list.
     * @param delimiter    delimiter between values
     * @return Array of doubles.
     */
    public static double[] parseDoubles(String sourceString,
                                        String delimiter) {
        if (sourceString == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(sourceString, delimiter);
        double[]        cw  = new double[tok.countTokens()];
        int             cnt = 0;
        while (tok.hasMoreTokens()) {
            cw[cnt++] = parseDouble(tok.nextToken());
        }
        return cw;
    }


    /**
     * The given sourceString is a comma separated list of lat or lon values,
     * this method parses this String, converting it into an array of doubles.
     *
     * @param sourceString The comma separated list.
     * @return Array of doubles.
     */
    public static double[] parseLatLons(String sourceString) {
        return parseLatLons(sourceString, ",");
    }


    /**
     * The given sourceString is a <code>delimiter</code> separated list
     * of lats or lons. This method parses this String, converting it into an
     * array of doubles.
     *
     * @param sourceString The comma separated list.
     * @param delimiter    delimiter between values
     * @return Array of doubles.
     */
    public static double[] parseLatLons(String sourceString,
                                        String delimiter) {
        if (sourceString == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(sourceString, delimiter);
        double[]        cw  = new double[tok.countTokens()];
        int             cnt = 0;
        while (tok.hasMoreTokens()) {
            cw[cnt++] = decodeLatLon(tok.nextToken());
        }
        return cw;
    }


    /**
     * The given sourceString is a comma separated list of values, this method
     * parses this String, converting it into an array of floats.
     *
     * @param sourceString The comma separated list.
     * @return Array of floats.
     */
    public static float[] parseFloats(String sourceString) {
        return parseFloats(sourceString, ",");
    }

    /**
     * The given sourceString is a <code>delimiter</code> separated list
     * of doubles. This method parses this String, converting it into an
     * array of floats.
     *
     * @param sourceString The comma separated list.
     * @param delimiter    delimiter between values
     * @return Array of doubles.
     */
    public static float[] parseFloats(String sourceString, String delimiter) {
        if (sourceString == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(sourceString, delimiter);
        float[]         cw  = new float[tok.countTokens()];
        int             cnt = 0;
        while (tok.hasMoreTokens()) {
            cw[cnt++] = parseFloat(tok.nextToken());
        }
        return cw;
    }


    /**
     * The given sourceString is a <code>delimiter</code> separated list
     * of doubles. This method parses this String, converting it into an
     * array of ints.
     *
     * @param sourceString The comma separated list.
     * @param delimiter    delimiter between values
     * @return Array of doubles.
     */
    public static int[] parseInts(String sourceString, String delimiter) {
        if (sourceString == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(sourceString, delimiter);
        int[]           cw  = new int[tok.countTokens()];
        int             cnt = 0;
        while (tok.hasMoreTokens()) {
            cw[cnt++] = (int) parseDouble(tok.nextToken());
        }
        return cw;
    }


    /**
     * Normalize a longitude value to the range between -180 and 180.
     *
     * @param lonValue  longitude value to adjust (in degrees)
     * @return adjusted value.
     */
    public static double normalizeLongitude(double lonValue) {
        int cnt = 0;
        while ((lonValue < -180.) || (lonValue > 180.)) {
            lonValue = (lonValue < -180)
                       ? lonValue + 360.
                       : lonValue - 360.;
            if (cnt++ > 10000) {
                throw new IllegalArgumentException("Bad longitude value:"
                        + lonValue);
            }
        }
        return lonValue;
    }


    /**
     * Compute the tick mark values based on the input.  Useful for
     * labels, contour values.
     *
     * @param high  highest value of range
     * @param low   low value of range
     * @param base  base value for centering ticks
     * @param interval  interval between ticks
     *
     * @return  array of computed tick values
     */
    public static double[] computeTicks(double high, double low, double base,
                                        double interval) {
        double[] vals = null;

        // compute nlo and nhi, for low and high contour values in the box
        long nlo = Math.round((Math.ceil((low - base) / Math.abs(interval))));
        long nhi = Math.round((Math.floor((high - base)
                                          / Math.abs(interval))));

        // how many contour lines are needed.
        int numc = (int) (nhi - nlo) + 1;
        if (numc < 1) {
            return vals;
        }

        vals = new double[numc];

        for (int i = 0; i < numc; i++) {
            vals[i] = base + (nlo + i) * interval;
        }

        return vals;
    }

    /**
     * Compute the tick mark values based on the input.  Useful for
     * labels, contour values.
     *
     * @param high  highest value of range
     * @param low   low value of range
     * @param base  base value for centering ticks
     * @param interval  interval between ticks
     *
     * @return  array of computed tick values
     */
    public static float[] computeTicks(float high, float low, float base,
                                       float interval) {
        float[] vals = null;

        // compute nlo and nhi, for low and high contour values in the box
        long nlo = Math.round((Math.ceil((low - base) / Math.abs(interval))));
        long nhi = Math.round((Math.floor((high - base)
                                          / Math.abs(interval))));

        // how many contour lines are needed.
        int numc = (int) (nhi - nlo) + 1;
        if (numc < 1) {
            return vals;
        }

        vals = new float[numc];

        for (int i = 0; i < numc; i++) {
            vals[i] = base + (nlo + i) * interval;
        }

        return vals;
    }

    /**
     * Calculate a good spacing interval for labels, contours based
     * on the range of the data.
     *
     * @param min  minimum value in range
     * @param max  maximum value in range
     *
     * @return  tick spacing
     */
    public static double computeTickSpacing(double min, double max) {

        double range = Math.abs(max - min);
        if (range == Double.POSITIVE_INFINITY) {
            return 1.0;
        }
        double majorTickSpacing = range;
        // compute tick mark values
        double tens = 1.0;
        if (range < tens) {
            tens /= 10.0;
            while (range < tens) {
                tens /= 10.0;
            }
        } else {
            while (10.0 * tens <= range) {
                tens *= 10.0;
            }
        }
        double ratio = range / tens;
        if (ratio < 2.0) {
            tens = tens / 5.0;
        } else if (ratio < 4.0) {
            tens = tens / 2.0;
        }
        majorTickSpacing = tens;
        return majorTickSpacing;
    }

    /**
     * Creates a hashtable that will draw text labels starting at the
     * starting point specified using the increment field.
     * If you call createStandardLabels(100, 0, 2.0, 10.0), then it will
     * make labels for the values 2, 12, 22, 32, etc.
     *
     * @param max  highest value of range
     * @param min   low value of range
     * @param base  base value for centering ticks
     * @param increment  interval between ticks
     *
     * @return a table of values with a Double as the key and a formatted
     *         String as the value or an empty table if there are no labels
     *
     */
    public static Hashtable createLabelTable(double max, double min,
                                             double base, double increment) {
        /*
        if (min > max) {
            throw new IllegalArgumentException(
                "max must be greater than min");
        }
        if (increment > (max - min)) {
            throw new IllegalArgumentException(
                "increment must be less than or equal to range (max-min)");
        }
        */

        Hashtable labelTable = new Hashtable();
        if ((min > max) || (increment > (max - min))) {
            return labelTable;
        }
        double[] values = computeTicks(max, min, base, increment);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                labelTable.put(new Double(values[i]), format(values[i]));
            }
        }
        return labelTable;

    }




    /**
     * A method for printing a list.  Each element is preceded by it's
     * position in the list
     *
     * @param l   List to print
     */
    public static void print(List l) {
        for (int i = 0; i < l.size(); i++) {
            System.err.println("" + i + "=" + l.get(i));
        }
    }




    /**
     * Does the the list contain the string
     *
     * @param string  the string to search for
     * @param objects the list of objects
     * @param lowerCase  check case insensibly
     *
     * @return true if the string matches the string values of the objects
     */
    public static boolean containsString(String string, List objects,
                                         boolean lowerCase) {
        if (lowerCase) {
            string = string.toLowerCase();
        }
        for (Object o : objects) {
            if (lowerCase) {
                if (Misc.equals(string, o.toString().toLowerCase())) {
                    return true;
                }
            } else {
                if (Misc.equals(string, o.toString())) {
                    return true;
                }
            }
        }
        return false;
    }



    /**
     * Does the given array contain the given object.
     *
     * @param object THe object
     * @param array The array
     *
     * @return Array contains object
     */
    public static boolean contains(Object object, Object[] array) {
        return indexOf(object, array) >= 0;
    }

    /**
     * What is the index of the given object
     *
     * @param object THe object
     * @param array The array
     *
     * @return index of object or -1
     */
    public static int indexOf(Object object, Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (Misc.equals(object, array[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Is the given List a list of String objects.
     * @param  listToCheck  List to Check
     * @return true if all the objects in the list are Strings.
     */
    public static boolean allStrings(List listToCheck) {
        for (Iterator iter = listToCheck.iterator(); iter.hasNext(); ) {
            if ( !(iter.next() instanceof String)) {
                return false;
            }
        }
        return true;
    }








    //Old file routines


    /**
     * Does the given file or url have the given suffix
     *
     * @param fileOrUrl The name of the file or url
     * @param suffix The suffix
     *
     * @return Does the fileOrUrl have the suffix
     */
    public static boolean hasSuffix(String fileOrUrl, String suffix) {
        return IOUtil.hasSuffix(fileOrUrl, suffix);
    }



    /**
     * @param filename The filename to check.
     * @return Is the filename a text file.
     * @deprecated Use IOUtil
     */
    public static boolean isTextFile(String filename) {
        return IOUtil.isTextFile(filename);
    }


    /**
     *  @param filename The filename to check.
     *  @return Is the filename an image file.
     * @deprecated Use IOUtil
     */
    public static boolean isImageFile(String filename) {
        return IOUtil.isImageFile(filename);
    }

    /**
     *  @param filenameOrUrl The filename to check.
     *  @return Is the filename an html file.
     * @deprecated Use IOUtil
     */
    public static boolean isHtmlFile(String filenameOrUrl) {
        return IOUtil.isHtmlFile(filenameOrUrl);
    }



    /**
     *
     * @param filename   name of file
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     * @deprecated Use IOUtil
     */
    public static InputStream getInputStream(String filename)
            throws FileNotFoundException, IOException {
        return IOUtil.getInputStream(filename);
    }

    /**
     *
     * @param filename    name of file
     * @param origin      relative origin point for file location
     * @return  corresponding input stream
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     * @deprecated Use IOUtil
     */
    public static InputStream getInputStream(String filename, Class origin)
            throws FileNotFoundException, IOException {
        return IOUtil.getInputStream(filename, origin);
    }




    /**
     * @param file    file to read.
     * @return  contents as a String
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem opening stream
     * @deprecated Use IOUtil
     */
    public static String readContents(File file)
            throws FileNotFoundException, IOException {
        return IOUtil.readContents(file);
    }

    /**
     *
     * @param contentName   URL or filename
     * @param dflt          default to return if a problem
     * @return  contents or default value
     * @deprecated Use IOUtil
     */
    public static String readContents(String contentName, String dflt) {
        return IOUtil.readContents(contentName, dflt);
    }


    /**
     * @param contentName can either be a URL, a filename or a resource.
     *
     * @return   contents or <code>null</code> if there is a problem.
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem reading contents
     * @deprecated Use IOUtil
     */
    public static String readContents(String contentName)
            throws FileNotFoundException, IOException {
        return IOUtil.readContents(contentName);
    }



    /**
     * @param contentName can either be a URL, a filename or a resource.
     * @param origin    relative origin for path to file
     *
     * @return   contents or <code>null</code> if there is a problem.
     *
     * @throws FileNotFoundException     couldn't find the file
     * @throws IOException               problem reading contents
     * @deprecated Use IOUtil
     */
    public static String readContents(String contentName, Class origin)
            throws FileNotFoundException, IOException {
        return IOUtil.readContents(contentName, origin);
    }



    /**
     * @param is   InputStream to read from
     * @return  contents as a String
     *
     * @throws IOException  problem reading contents
     * @deprecated Use IOUtil
     */
    public static String readContents(InputStream is) throws IOException {
        return IOUtil.readContents(is);
    }


    /**
     *
     * @param is   InputStream to read from
     * @return  bytes read
     *
     * @throws IOException  problem reading contents
     * @deprecated Use IOUtil
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        return IOUtil.readBytes(is);
    }

    /**
     * @param is   InputStream to read from
     * @param loadId loadid
     * @return  bytes read
     *
     * @throws IOException  problem reading contents
     * @deprecated Use IOUtil
     */
    public static byte[] readBytes(InputStream is, Object loadId)
            throws IOException {
        return IOUtil.readBytes(is, loadId);
    }



    /**
     *
     *  @param dir The directory to search in.
     *  @return The most recent file (or null if none found).
     * @deprecated Use IOUtil
     */
    public static File getMostRecentFile(File dir) {
        return IOUtil.getMostRecentFile(dir);
    }

    /**
     *  @param dir The directory to search in.
     *  @param filter The {@link FileFilter} to be used to limit what files we look at (may be null).
     *  @return The most recent file (or null if none found).
     * @deprecated Use IOUtil
     */
    public static File getMostRecentFile(File dir,
                                         java.io.FileFilter filter) {
        return IOUtil.getMostRecentFile(dir, filter);
    }



    /**
     * @param directory The directory
     * @param filter The filter
     * @param youngestFirst Ascending or descending
     *
     * @deprecated Use IOUtil
     * @return The sorted files
     */
    public static File[] sortFilesOnAge(File directory,
                                        java.io.FileFilter filter,
                                        boolean youngestFirst) {
        return IOUtil.sortFilesOnAge(directory, filter, youngestFirst);
    }




    /**
     *
     * @param files The files
     * @param youngestFirst Ascending or descending
     * @deprecated Use IOUtil
     * @return Just return the given array
     */
    public static File[] sortFilesOnAge(File[] files,
                                        final boolean youngestFirst) {
        return IOUtil.sortFilesOnAge(files, youngestFirst);
    }

    /**
     * @param files List of files
     *
     * @return array of files
     * @deprecated Use IOUtil
     */
    public static File[] toFiles(List files) {
        return IOUtil.toFiles(files);
    }


    /**
     *
     * @param filter The filechooser file filter
     *
     * @return The javaio FileFilter.
     * @deprecated Use IOUtil
     */
    public static java.io.FileFilter wrapFilter(
            final javax.swing.filechooser.FileFilter filter) {
        return IOUtil.wrapFilter(filter);
    }



    /**
     *  @param dir The directory to search in.
     *  @param filter The filter to be used to limit what files we look at (may be null).
     *  @return The most recent file (or null if none found).
     * @deprecated Use IOUtil
     */
    public static File getMostRecentFile(
            File dir, final javax.swing.filechooser.FileFilter filter) {
        return IOUtil.getMostRecentFile(dir, filter);
    }



    /**
     *  @param f The file path.
     *  @return The file name.
     * @deprecated Use IOUtil
     */
    public static String getFileTail(String f) {
        return IOUtil.getFileTail(f);
    }


    /**
     *  @param f The file path.
     *  @return The file name.
     * @deprecated Use IOUtil
     */
    public static String getFileRoot(String f) {
        return IOUtil.getFileRoot(f);
    }


    /**
     *  Remove any file extension from the given file name.
     *
     *  @param f The file path.
     *  @return The file name without the extension.
     * @deprecated Use IOUtil
     *
     */
    public static String stripExtension(String f) {
        return IOUtil.stripExtension(f);
    }


    /**
     *
     * @param name The filename to be cleaned up
     * @return The cleaned up filename
     * @deprecated Use IOUtil
     */
    public static String cleanFileName(String name) {
        return IOUtil.cleanFileName(name);
    }


    /**
     *
     *  @param f The file path.
     *  @return The file  extension or an empty string if none found.
     * @deprecated Use IOUtil
     */
    public static String getFileExtension(String f) {
        return IOUtil.getFileExtension(f);
    }


    /**
     *
     * @param filename  filename to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     * @deprecated Use IOUtil
     */
    public static void writeFile(String filename, String contents)
            throws FileNotFoundException, IOException {
        IOUtil.writeFile(filename, contents);
    }


    /**
     *
     * @param filename  File to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     * @deprecated Use IOUtil
     */
    public static void writeFile(File filename, String contents)
            throws FileNotFoundException, IOException {
        IOUtil.writeFile(filename, contents);
    }


    /**
     * Write out a file to the {@link File} specified.
     *
     * @param filename  File to write to
     * @param contents  file contents
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     * @deprecated Use IOUtil
     */
    public static void writeBytes(File filename, byte[] contents)
            throws FileNotFoundException, IOException {
        IOUtil.writeBytes(filename, contents);
    }


    /**
     *
     * @param filename  file name
     * @param origin    anchor point for relative location of file
     * @param dflt      default to return
     * @return  contents as a String  or <code>dflt</code>
     * @deprecated Use IOUtil
     */
    public static String readFile(String filename, Class origin,
                                  String dflt) {
        return IOUtil.readContents(filename, origin, dflt);
    }

    /**
     *
     * @param filename  file name
     * @param origin    anchor point for relative location of file
     * @return null if the file can't be found
     *
     * @throws FileNotFoundException    if the file does not exist
     * @throws IOException              if there is a problem writing
     * @deprecated Use IOUtil
     */
    public static String readFile(String filename, Class origin)
            throws FileNotFoundException, IOException {
        return IOUtil.readContents(filename, origin);
    }


    /**
     *
     * @param from File to move
     * @param to The destination
     *
     *
     * @throws FileNotFoundException When we cannot find the file
     * @throws IOException When something untoward happens
     * @deprecated Use IOUtil
     */
    public static void moveFile(File from, File to)
            throws FileNotFoundException, IOException {
        IOUtil.moveFile(from, to);
    }



    /**
     *
     * @param from File to copy
     * @param to The destination
     *
     * @throws FileNotFoundException When we cannot find the file
     * @throws IOException When something untoward happens
     * @deprecated Use IOUtil
     */
    public static void copyFile(File from, File to)
            throws FileNotFoundException, IOException {
        IOUtil.copyFile(from, to);
    }


    /**
     * @param f1    directory path
     * @param f2    filename
     * @return  concatenated String with the appropriate file separator
     * @deprecated Use IOUtil
     */
    public static String joinDir(String f1, String f2) {
        return IOUtil.joinDir(f1, f2);
    }


    /**
     *
     * @param f1          directory path
     * @param filename    filename
     * @return  concatenated String with the appropriate file separator
     * @deprecated Use IOUtil
     */
    public static String joinDir(File f1, String filename) {
        return IOUtil.joinDir(f1, filename);
    }


    /**
     *
     * @param path   directory to make
     * @return  the directory path
     * @deprecated Use IOUtil
     */
    public static String makeDir(String path) {
        return IOUtil.makeDir(path);
    }


    /**
     *
     * @param f   directory as a file
     * @return   directory path
     * @deprecated Use IOUtil
     */
    public static String makeDir(File f) {
        return IOUtil.makeDir(f);
    }



    /**
     *
     * @param dir The directory to look at
     * @param recurse Do we recurse
     *
     * @return List of subdirs (File)
     * @deprecated Use IOUtil
     */
    public static List getDirectories(File dir, boolean recurse) {
        return IOUtil.getDirectories(dir, recurse);
    }


    /**
     *
     * @param dirs List of directories to look at
     * @param recurse Do we recurse
     *
     * @return List of subdirs (File)
     * @deprecated Use IOUtil
     */
    public static List getDirectories(List dirs, boolean recurse) {
        return IOUtil.getDirectories(dirs, recurse);
    }


    /**
     *
     * @param root Directory to prune
     * @deprecated Use IOUtil
     */
    private static void pruneIfEmpty(File root) {
        IOUtil.pruneIfEmpty(root);
    }



    /**
     *
     * @param root Directory to prune
     * @deprecated Use IOUtil
     */
    public static void pruneEmptyDirectories(File root) {
        IOUtil.pruneEmptyDirectories(root);
    }


    /**
     * @deprecated Use IOUtil
     *
     * @param root root
     */
    public static void deleteDirectory(File root) {
        IOUtil.deleteDirectory(root);
    }






    // String routines



    /**
     *
     * @param source      Source String to match on.
     * @param patternList List of objects whose toString is the pattern.
     * @param dflt        The default if nothing matches.
     * @deprecated See StringUtil
     * @return The Object whose toString matches the source or the
     *                    dflt if no matches found.
     */
    public static Object findMatch(String source, List patternList,
                                   Object dflt) {
        return StringUtil.findMatch(source, patternList, dflt);
    }

    /**
     *
     * @param source        Source String to match on.
     * @param patternList   List of objects whose toString is the pattern.
     * @param results       The list of return objects.
     * @param dflt          The default if nothing matches.
     * @deprecated See StringUtil
     * @return The return Object whose toString matches the source or the dflt if no matches found.
     */
    public static Object findMatch(String source, List patternList,
                                   List results, Object dflt) {
        return StringUtil.findMatch(source, patternList, results, dflt);
    }





    /**
     *
     * @param patternString   pattern string to check
     * @deprecated See StringUtil
     * @return  true if it contains (^,*,$,+).
     */
    public static boolean containsRegExp(String patternString) {
        return StringUtil.containsRegExp(patternString);
    }



    /**
     * @param input           The input source string.
     * @param patternString   The regular expression pattern.
     * @deprecated See StringUtil
     * @return                true if the pattern match the input.
     */
    public static boolean stringMatch(String input, String patternString) {
        return StringUtil.stringMatch(input, patternString);
    }


    /**
     * @param input           The input source string.
     * @param patternString   The regular expression pattern.
     * @param substring Search for substrings
     * @param caseSensitive Is case sensitive
     * @deprecated See StringUtil
     * @return                true if the pattern match the input.
     */
    public static boolean stringMatch(String input, String patternString,
                                      boolean substring,
                                      boolean caseSensitive) {

        return StringUtil.stringMatch(input, patternString, substring,
                                      caseSensitive);
    }

    /**
     * @param  value  String to check
     * @deprecated See StringUtil
     * @return true if value starts with a, e, i, o, or u (but not sometimes y).
     *         Check is case insensitive.
     */
    public static boolean startsWithVowel(String value) {
        return StringUtil.startsWithVowel(value);
    }


    /**
     *
     * @param text The text to convert
     * @param insert string to insert
     * @param lineSize line size to insert at
     *
     * @deprecated See StringUtil
     * @return The text with added br tags.
     */
    public static String breakText(String text, String insert, int lineSize) {
        return StringUtil.breakText(text, insert, lineSize);
    }



    /**
     *  @param html The source html string.
     * @deprecated See StringUtil
     *  @return The raw text.
     */
    public static String stripTags(String html) {
        return StringUtil.stripTags(html);
    }


    /**
     *  @param inputString The string to remove the whitespace.
     * @deprecated See StringUtil
     *  @return The whitespaceless result.
     */
    public static String removeWhitespace(String inputString) {
        return StringUtil.removeWhitespace(inputString);
    }



    /**
     * @param value The value.
     * @deprecated See StringUtil
     * @return The String  represenation of the value, padded with a
     *         leading "0" if value &lt; 10
     */
    public static String zeroString(int value) {
        return StringUtil.zeroString(value);
    }


    /**
     * @param value The value.
     * @param numDigits number of digits
     * @deprecated See StringUtil
     * @return The String  represenation of the value, padded with
     *         leading "0"-s if value &lt; 10E(numDigits-1)
     */
    public static String padZero(int value, int numDigits) {
        return StringUtil.padZero(value, numDigits);
    }


    /**
     * @param s               String to pad
     * @param desiredLength   ending length
     * @deprecated See StringUtil
     * @return  padded String
     */
    public static String padLeft(String s, int desiredLength) {
        return StringUtil.padLeft(s, desiredLength);
    }


    /**
     * @param s               String to pad
     * @param desiredLength   ending length
     * @param padString       String to pad with (e.g, " ")
     * @deprecated See StringUtil
     * @return  padded String
     */
    public static String padLeft(String s, int desiredLength,
                                 String padString) {
        return StringUtil.padLeft(s, desiredLength, padString);
    }



    /**
     * @param s               String to pad
     * @param desiredLength   ending length
     * @deprecated See StringUtil
     * @return  padded String
     */
    public static String padRight(String s, int desiredLength) {
        return StringUtil.padRight(s, desiredLength);
    }


    /**
     * @param s               String to pad
     * @param desiredLength   ending length
     * @param padString       String to pad with (e.g, " ")
     * @deprecated See StringUtil
     * @return  padded String
     */
    public static String padRight(String s, int desiredLength,
                                  String padString) {
        return StringUtil.padRight(s, desiredLength, padString);
    }


    /**
     *  @param args An array of Strings to merge.
     * @deprecated See StringUtil
     *  @return The given strings concatenated together with a
     *          space between each.
     */
    public static String join(String[] args) {
        return StringUtil.join(args);
    }

    /**
     *  @param delimiter The delimiter.
     *  @param args An array of Strings to merge.
     * @deprecated See StringUtil
     *  @return The given strings concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, Object[] args) {
        return StringUtil.join(delimiter, args);
    }


    /**
     *  @param delimiter The delimiter.
     *  @param args An array of Strings to merge.
     *  @param  ignoreEmptyStrings Don't join empty strings
     * @deprecated See StringUtil
     *  @return The given strings concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, Object[] args,
                              boolean ignoreEmptyStrings) {
        return StringUtil.join(delimiter, args, ignoreEmptyStrings);
    }


    /**
     *  @param delimiter The delimiter.
     *  @param args A List of objects whose toString value are merged.
     * @deprecated See StringUtil
     *  @return The given object.toString values concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, List args) {
        return StringUtil.join(delimiter, args);
    }


    /**
     *  @param delimiter The delimiter.
     *  @param args A List of objects whose toString value are merged.
     *  @param ignoreEmptyStrings Should ignore empty strings
     * @deprecated See StringUtil
     *  @return The given object.toString values concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, List args,
                              boolean ignoreEmptyStrings) {
        return StringUtil.join(delimiter, args, ignoreEmptyStrings);
    }





    /**
     * @param source The source object string.
     *
     * @deprecated See StringUtil
     * @return List of String tokens.
     */
    public static List split(Object source) {
        return StringUtil.split(source);
    }


    /**
     * @param content The String to  parse
     * @param indices the index in the line which defines the word start.
     * @param lengths the length of each word.
     * @param lineDelimiter What to split  the line content string on (usually "\n").
     *  @param commentString If non-null defines the comment String in the content.
     *  @param trimWords Do we trim each word.
     *
     * @deprecated See StringUtil
     *  @return A list of String arrays  that holds the words.
     */
    public static List parseLineWords(String content, int[] indices,
                                      int[] lengths, String lineDelimiter,
                                      String commentString,
                                      boolean trimWords) {
        return StringUtil.parseLineWords(content, indices, lengths,
                                         lineDelimiter, commentString,
                                         trimWords);
    }

    /**
     * @param source     The source object string.
     * @param delimiter  The delimiter to break up the sourceString on.
     * @deprecated See StringUtil
     * @return List of String tokens.
     */
    public static List split(Object source, String delimiter) {
        return StringUtil.split(source, delimiter);
    }

    /**
     * @param source     The source object string.
     * @param delimiter  The delimiter to break up the sourceString on.
     * @param trim       Do we string trim the tokens.
     *
     * @deprecated See StringUtil
     * @return List of String tokens.
     */
    public static List split(Object source, String delimiter, boolean trim) {
        return StringUtil.split(source, delimiter, trim);
    }

    /**
     * @param source             The source object string.
     * @param delimiter          The delimiter to break up the sourceString on.
     * @param trim               Do we string trim the tokens.
     * @param excludeZeroLength  If true then don't add in zero length strings.
     *
     * @deprecated See StringUtil
     * @return List of String tokens.
     */
    public static List split(Object source, String delimiter, boolean trim,
                             boolean excludeZeroLength) {
        return StringUtil.split(source, delimiter, trim, excludeZeroLength);
    }


    /**
     * @param s             String to split
     * @param delimiter     token delimeter
     * @param cnt           max number of tokens
     * @deprecated See StringUtil
     * @return array of strings or <code>null</code> if unable to split
     *         the string.
     */
    public static String[] split(String s, String delimiter, int cnt) {
        return StringUtil.split(s, delimiter, cnt);
    }




    /**
     *  @param  objectList The list of objects.
     * @deprecated See StringUtil
     *  @return The array of the object string values.
     */
    public static String[] listToStringArray(List objectList) {
        return StringUtil.listToStringArray(objectList);
    }


    /**
     *
     * @param l   list of objects
     * @deprecated See StringUtil
     * @return  semicolon separated String of Strings.
     */
    public static String listToString(List l) {
        return StringUtil.listToString(l);
    }

    /**
     * @param l List of objects
     * @deprecated See StringUtil
     * @return List of strings.
     */
    public static List toString(List l) {
        return StringUtil.toString(l);
    }


    /**
     * @param string   string to munge
     * @param pattern  pattern to replace
     * @param value    replacement value
     * @deprecated See StringUtil
     * @return  munged string
     */
    public static String replace(String string, String pattern,
                                 String value) {
        return StringUtil.replace(string, pattern, value);
    }

    /**
     * @param v            original String
     * @param patterns     patterns to match
     * @param values       replacement values
     * @deprecated See StringUtil
     * @return  munged String
     */
    public static String replaceList(String v, String[] patterns,
                                     String[] values) {
        return StringUtil.replaceList(v, patterns, values);
    }


    /**
     * @param v            original String
     * @param patterns     patterns to match
     * @param values       replacement values
     * @deprecated See StringUtil
     * @return  munged String
     */
    public static String replaceList(String v, List patterns, List values) {
        return StringUtil.replaceList(v, patterns, values);
    }


    /**
     * @param sourceList   original list of Strings
     * @param patterns     patterns to replace
     * @param values       replacement values
     * @deprecated See StringUtil
     * @return  new list with replaced values
     */
    public static List replaceList(List sourceList, String[] patterns,
                                   String[] values) {
        return StringUtil.replaceList(sourceList, patterns, values);
    }


    /**
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  object to append
     * @deprecated See StringUtil
     * @return  StringBuffer with appended object
     */
    public static StringBuffer append(StringBuffer sb, Object s1) {
        return StringUtil.append(sb, s1);
    }


    /**
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @deprecated See StringUtil
     * @return  StringBuffer with appended objects
     * `
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2) {
        return StringUtil.append(sb, s1, s2);
    }

    /**
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @deprecated See StringUtil
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3) {
        return StringUtil.append(sb, s1, s2, s3);
    }

    /**
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @param s4  fourth object to append
     * @deprecated See StringUtil
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3, Object s4) {
        return StringUtil.append(sb, s1, s2, s3, s4);
    }

    /**
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @param s4  fourth object to append
     * @param s5  fifth object to append
     * @deprecated See StringUtil
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3, Object s4, Object s5) {
        return StringUtil.append(sb, s1, s2, s3, s4, s5);
    }


    /**
     *
     * @param s             String to parse
     * @param skipFirst     true to skip the first value
     * @deprecated See StringUtil
     * @return   list of parsed Strings
     */
    public static List parseCsv(String s, boolean skipFirst) {
        return StringUtil.parseCsv(s, skipFirst);
    }



    /**
     * @param s      String to shorten
     * @param length shortened length where elipses will start
     *
     * @deprecated See StringUtil
     * @return shortened string.
     */
    public static final String shorten(String s, int length) {
        return StringUtil.shorten(s, length);
    }


    /**
     * @param array  array to print
     *
     * @deprecated See StringUtil
     * @return  array as a String
     */
    public static String toString(Object[] array) {
        return StringUtil.toString(array);
    }



    /**
     * Add the given class loader into the list of custom class loaders
     *
     * @param cl The class loader
     */
    public static void addClassLoader(ClassLoader cl) {
        synchronized (classLoaders) {
            classLoaders.add(cl);
        }
    }


    /**
     * Get the list of dynamic class loaders
     *
     * @return List of dynamic classloaders
     */
    public static List<ClassLoader> getClassLoaders() {
        synchronized (classLoaders) {
            return new ArrayList<ClassLoader>(classLoaders);
        }
    }


    /**
     * Find the Class for the given class name. This first checks any custom class
     * loaders the app may be using. Next it just uses the standard Class.forName
     *
     * @param className The class name
     *
     * @return The class
     *
     * @throws ClassNotFoundException When the className could not be found
     */
    public static Class findClass(String className)
            throws ClassNotFoundException {

        //First look at the PluginClassLoaders. We do this because if we have 2 or more
        //of these we can get class conflicts
        List<ClassLoader> classLoaders = getClassLoaders();
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader instanceof PluginClassLoader) {
                PluginClassLoader pcl = (PluginClassLoader) classLoader;
                Class             c   = pcl.getClassFromPlugin(className);
                if (c != null) {
                    return c;
                }
            }
        }


        for (ClassLoader classLoader : classLoaders) {
            try {
                Class c = Class.forName(className, true, classLoader);
                if (c != null) {
                    return c;
                }
            } catch (ClassNotFoundException cnfe) {}
        }
        return Class.forName(className);
    }



    /**
     * figure out how long to wait for
     *
     * @param minutesDelta time to wait for
     *
     * @return hummm
     */
    public static long getPauseEveryTime(int minutesDelta) {
        if (minutesDelta <= 0) {
            minutesDelta = 1;
        }
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        int currentMinutes = cal.get(Calendar.MINUTE)
                             + cal.get(Calendar.HOUR) * 60;
        double sleepTime;
        if (minutesDelta > currentMinutes) {
            sleepTime = 60 * 1000 * (minutesDelta - currentMinutes);
        } else {
            int foo = currentMinutes % minutesDelta;
            sleepTime = 60 * 1000 * (minutesDelta - foo);
        }
        return (long) sleepTime;
    }




    /**
     * Pause every n minutes
     *
     * @param minutesDelta   number of minutes
     */
    public static void pauseEvery(int minutesDelta) {
        long sleepTime = getPauseEveryTime(minutesDelta);
        Misc.sleep((long) sleepTime);
    }


    /**
     * Clone an array of floats
     *
     * @param a  array to clone
     *
     * @return  cloned array
     */
    public static float[][] cloneArray(float[][] a) {
        float[][] b = new float[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, b[i], 0, a[0].length);
        }
        return b;
    }

    /**
     * Reverse the array
     *
     * @param a  input array
     *
     * @return  the array in reverse order
     */
    public static float[] reverseArray(float[] a) {
        float[] b = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            b[a.length - i] = a[i];
        }
        return b;
    }


    /**
     * Set c=a-b and return c. If c is null then create a new array
     *
     * @param a  the array
     * @param b  the the other array
     * @param c  the return array
     *
     * @return  the new array
     */
    public static float[][] subtractArray(float[][] a, float[][] b,
                                          float[][] c) {
        if (c == null) {
            c = new float[a.length][a[0].length];
        }
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                c[i][j] = a[i][j] - b[i][j];
            }
        }
        return c;
    }

    /**
     * Add two arrays together
     *
     * @param a  the array
     * @param b  the the other array
     * @param c  the return array
     *
     * @return  the new array
     */
    public static float[][] addArray(float[][] a, float[][] b, float[][] c) {
        if (c == null) {
            c = new float[a.length][a[0].length];
        }
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                c[i][j] = a[i][j] + b[i][j];
            }
        }
        return c;
    }

    /**
     * Get the range of all values
     *
     * @param a  arrays of values
     *
     * @return the range (min,max)
     */
    public static float[] getRange(float[][] a) {
        float[] range = { Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                float val = a[i][j];
                if (val == val) {
                    range[0] = (float) Math.min(range[0], val);
                    range[1] = (float) Math.max(range[1], val);
                }
            }
        }
        return range;
    }


    /**
     * Get the ranges for each column
     *
     * @param a arrays of values
     *
     * @return  the range (min,max) of each column
     */
    public static float[][] getRanges(float[][] a) {
        float[][] ranges = new float[a.length][];
        for (int i = 0; i < a.length; i++) {
            ranges[i] = getRange(new float[][] {
                a[i]
            });
        }
        return ranges;
    }


    /**
     * fill array with value
     *
     * @param a array
     * @param value value
     */
    public static void fillArray(float[][] a, float value) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                a[i][j] = value;
            }
        }
    }


    /**
     * Clone an array of doubles
     *
     * @param a  array to clone
     *
     * @return  cloned array
     */
    public static double[][] cloneArray(double[][] a) {
        double[][] b = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, b[i], 0, a[0].length);
        }
        return b;
    }

    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(double[] a, double[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }



    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(int[] a, int[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(float[] a, float[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }



    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(String[] a, String[] b) {
        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }
        if ((b == null) && (a == null)) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if ( !equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }



    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(float[][] a, float[][] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        if (a[0].length != b[0].length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if ( !java.util.Arrays.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }





    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(double[][] a, double[][] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        if (a[0].length != b[0].length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if ( !java.util.Arrays.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }


    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(int[][] a, int[][] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        if (a[0].length != b[0].length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if ( !java.util.Arrays.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }



    /**
     * check for equality a or b can be null;
     *
     * @param a array 1
     * @param b array 2
     *
     * @return equals
     */
    public static boolean arraysEquals(int[][][] a, int[][][] b) {
        if (a == b) {
            return true;
        }

        if ((a == null) && (b != null)) {
            return false;
        }
        if ((b == null) && (a != null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        if (a[0].length != b[0].length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if ( !arraysEquals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }



    /**
     * Merge the two arrays into one.
     * Either of the arrays can be null
     *
     * @param a array 1
     * @param b array 2
     *
     * @return An array of length a.length + b.length with the values copied
     */
    public static float[] merge(float[] a, float[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        float[] newArray = new float[a.length + b.length];
        for (int i = 0; i < a.length; i++) {
            newArray[i] = a[i];
        }

        for (int i = 0; i < b.length; i++) {
            newArray[i + a.length] = b[i];
        }
        return newArray;
    }



    /**
     * Do a linear interpolation with cnt points between start and end
     *
     * @param cnt number of points
     * @param start start value
     * @param end end value
     *
     * @return Array of interpolated points
     */
    public static float[] interpolate(int cnt, float start, float end) {
        double  delta = (end - start) / (cnt - 1);
        float[] vals  = new float[cnt];
        for (int i = 0; i < cnt; i++) {
            vals[i] = (float) (start + delta * i);
        }
        return vals;
    }

    /**
     * Copy an array to another
     *
     * @param pts  the input
     * @param pointCnt  the number of points to copy
     *
     * @return the copied array
     */
    public static float[][] copy(float[][] pts, int pointCnt) {
        int       numFields = pts.length;
        float[][] newPts    = new float[numFields][pointCnt];
        for (int i = 0; i < pointCnt; i++) {
            for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
                newPts[fieldIdx][i] = pts[fieldIdx][i];
            }
        }
        return newPts;
    }

    /**
     * Expand an array of points
     *
     * @param pts  the points
     *
     * @return  the expanded array
     */
    public static float[][] expand(float[][] pts) {
        int       numFields = pts.length;
        float[][] newPts    = new float[numFields][pts[0].length * 2];
        for (int i = 0; i < pts[0].length; i++) {
            for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
                newPts[fieldIdx][i] = pts[fieldIdx][i];
            }
        }
        return newPts;
    }

    /*
     * Unpack an array of packed integers
     *
     * @param sourceValues  packed integer array
     * @param lengths       lengths of packed bits
     *
     * @param args test unpack
     *
     * @return an array of unpacked integers
     * public static int[] unpack(int[] sourceValues, int[] lengths) {
     *   int[] dest   = new int[lengths.length];
     *   int   bitIdx = 0;
     *   int   bitCnt = 0;
     *   for (int lengthIdx = 0; lengthIdx < lengths.length; lengthIdx++) {
     *       int length = lengths[lengthIdx];
     *       int value  = 0;
     *       //            System.err.println("reading value:" + lengthIdx);
     *       int    bitsReadSoFar = 0;
     *       String s             = "";
     *       while (bitsReadSoFar < length) {
     *           int idxIntoValuesArray = bitCnt / 32;
     *           int bitOffset          = 31 - (bitCnt % 32);
     *           int mask               = 1 << bitOffset;
     *           int v = mask & sourceValues[idxIntoValuesArray];
     *           if (v != 0) {
     *               value |= 1 << (length - bitsReadSoFar - 1);
     *           }
     *           //                System.err.println("    bitCnt:" + bitCnt +" idx:" + idxIntoValuesArray + " offset:" + bitOffset);
     *           if (v != 0) {
     *               s += "1";
     *           } else {
     *               s += "0";
     *           }
     *           bitCnt++;
     *           bitsReadSoFar++;
     *       }
     *       //            System.err.println("   s:" + s);
     *       dest[lengthIdx] = value;
     *   }
     *   return dest;
     * }
     */






    /**
     * Get a Runnable object
     *
     * @return the Runnable
     */
    public static Runnable getRunnable() {
        return new Runnable() {
            public void run() {
                int x = 0;
                int y = 0;
                while (true) {
                    for (int i = 0; i < 100000000; i++) {
                        y = x;
                    }
                    x++;
                    System.err.print(".");
                }
            }
        };
    }

    /**
     * Print out the bit pattern in an integer
     *
     * @param b  the integer
     */
    public static void printBits(int b) {
        System.err.println(getBits(b));
    }

    /**
     * Get the bits string for an int
     *
     * @param b  the integer
     * @return the string as bits (e.g.: 01100001|11000000|10011010|10110100|)
     */
    private static String getBits(int b) {
        String s = "";
        for (int i = 31; i >= 0; i--) {
            if ((b & (1 << i)) != 0) {
                s = s + "1";
            } else {
                s = s + "0";
            }
            if (i % 8 == 0) {
                s = s + "|";
            }
        }
        return s;
    }

    /**
     * Return the stack trace of this calling thread
     *
     * @return  The stack trace
     */
    public static String getStackTrace() {
        return getStackTrace(new IllegalArgumentException(""));
    }


    /**
     * Get the stack trace from the given exception
     *
     * @param exc The exception to get the trace from
     * @return The stack trace
     */
    public static String getStackTrace(Throwable exc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exc.printStackTrace(new PrintStream(baos));
        return baos.toString();
    }


    /**
     * Test this class
     *
     * @param args  ignored
     */
    public static void main(String[] args) {
        long   t1 = System.currentTimeMillis();
        double d  = 0;

        for (int i = 0; i < 10000000; i++) {
            d = new Double("2.3").doubleValue();
            //      d = Double.parseDouble("2.3");
        }
        long t2 = System.currentTimeMillis();


        long t3 = System.currentTimeMillis();

        System.err.println("time 1:" + (t2 - t1));

        if (true) {
            return;
        }



        /*

        List<String> test = new ArrayList<String>();


        for (int i = 0; i < 1000000; i++) {
            test.add("test string " + i);
        }
        int  size = test.size();


        long t1, t2;

        System.err.println("size:" + size);
        t1 = System.currentTimeMillis();
        for (int i = 0; i < test.size(); i++) {
            String s = test.get(i);
        }
        t2 = System.currentTimeMillis();
        System.err.println("time 1:" + (t2 - t1));

        t1 = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            String s = test.get(i);
        }
        t2 = System.currentTimeMillis();
        System.err.println("time 2:" + (t2 - t1));


        t1 = System.currentTimeMillis();
        for (String s : test) {}
        t2 = System.currentTimeMillis();
        System.err.println("time 3:" + (t2 - t1));


        */

    }

    /**
     * Find the indices of the array where it is equal to the value
     *
     * @param value  the value to look for
     * @param array  the array
     *
     * @return the array of indices
     */
    public static int[] find(int value, int[] array) {
        List<Integer> indices = new ArrayList<Integer>(array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                indices.add(i);
            }
        }
        int   m  = indices.size();
        int[] rv = new int[m];
        for (int i = 0; i < m; i++) {
            rv[i] = indices.get(i);
        }
        return rv;
    }

    /**
     * Find the indices of the array where it is equal to the value
     *
     * @param value  the value to look for
     * @param array  the array
     *
     * @return the array of indices
     */
    public static int[] find(float value, float[] array) {
        List<Integer> indices = new ArrayList<Integer>(array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                indices.add(i);
            }
        }
        int   m  = indices.size();
        int[] rv = new int[m];
        for (int i = 0; i < m; i++) {
            rv[i] = indices.get(i);
        }
        return rv;
    }

    /**
     * Find the indices of the array where it is equal to the value
     *
     * @param value  the value to look for
     * @param array  the array
     *
     * @return the array of indices
     */
    public static int[] find(double value, double[] array) {
        List<Integer> indices = new ArrayList<Integer>(array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                indices.add(i);
            }
        }
        int   m  = indices.size();
        int[] rv = new int[m];
        for (int i = 0; i < m; i++) {
            rv[i] = indices.get(i);
        }
        return rv;
    }

    /**
     * Check if all the values in the array are NaNs
     *
     * @param values  the array values
     *
     * @return  true if all NaN
     */
    public static boolean isNaN(float[][] values) {
        // if first data value is NaN, check if all are.
        if (Float.isNaN(values[0][0])) {
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    float value = values[i][j];
                    if (value == value) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Check if all the values in the array are NaNs
     *
     * @param values  array values
     *
     * @return  true if all NaN
     */
    public static boolean isNaN(double[][] values) {
        // if first data value is NaN, check if all are.
        if (Double.isNaN(values[0][0])) {
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    double value = values[i][j];
                    if (value == value) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
