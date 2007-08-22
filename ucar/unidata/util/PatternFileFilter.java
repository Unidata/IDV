/*
 * $Id: PatternFileFilter.java,v 1.28 2006/06/26 15:08:18 dmurray Exp $
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


import java.io.File;



import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.*;

import javax.swing.filechooser.*;


/**
 *  This holds a set of regular expression patterns that are used for file filters in a JFileChooser.
 *  You can also pass in an Object id (which can be used as the client code sees fit) and a directoriesOk
 *  flag.
 *
 *  @author Metapps development team
 */

public class PatternFileFilter extends FileFilter implements java.io
    .FileFilter, java.io.FilenameFilter {


    /**
     * Used for passing in a null object id
     */
    public static final Object NULL_OBJECT_ID = null;


    /**
     *  The description (or label) for this filter. e.g., "Html files"
     */
    private String desc;

    /** preferred suffix */
    private String preferredSuffix;

    /**
     *  List of compiled patterns to match.
     */
    private ArrayList patterns = new ArrayList();


    /**
     *  Can hold anything.
     */
    private Object id;

    /**
     *  Keep the original pattern string.
     */
    private String thePattern;

    /**
     *  Should the chooser show directories.
     */
    private boolean directoriesOk = true;

    /**
     *  Should the chooser show hidden files
     */
    private boolean hiddenOk = true;


    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     *  @param description The label to use in the JFileChooser
     */
    public PatternFileFilter(String patternsString, String description) {
        this(patternsString, NULL_OBJECT_ID, description, null);
    }


    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     *  @param description The label to use in the JFileChooser
     *  @param suffix The preferred suffix if none is provided.
     */
    public PatternFileFilter(String patternsString, String description,
                             String suffix) {
        this(patternsString, NULL_OBJECT_ID, description, suffix);
    }


    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     */
    public PatternFileFilter(String patternsString) {
        this(patternsString, true);
    }


    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     *  @param dirsOk Are directories ok to display.
     */
    public PatternFileFilter(String patternsString, boolean dirsOk) {
        this(patternsString, dirsOk, true);
    }

    /**
     * Create a new PatternFileFilter.
     *
     * @param patternsString   A comma separated list of regular expressions.
     * @param dirsOk Are directories ok to display.
     * @param hiddenOk true if hidden files are okay.
     */
    public PatternFileFilter(String patternsString, boolean dirsOk,
                             boolean hiddenOk) {
        this(patternsString, NULL_OBJECT_ID, "", null);
        this.hiddenOk      = hiddenOk;
        this.directoriesOk = dirsOk;
    }



    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     *  @param id An arbitary Object for client code to use.
     *  @param description The label to use in the JFileChooser
     */
    public PatternFileFilter(String patternsString, Object id,
                             String description) {
        this(patternsString, id, description, null);
    }



    /**
     *  Create a new PatternFileFilter.
     *  @param patternsString A comma separated list of regular expressions.
     *  @param id An arbitary Object for client code to use.
     *  @param description The label to use in the JFileChooser
     *  @param suffix The preferred suffix if none is provided.
     */
    public PatternFileFilter(String patternsString, Object id,
                             String description, String suffix) {

        this.thePattern      = patternsString;
        this.preferredSuffix = suffix;
        this.desc            = description;
        this.id              = id;
        List list = StringUtil.split(patternsString, ",");
        for (int i = 0; i < list.size(); i++) {
            patterns.add(Pattern.compile((String) list.get(i), Pattern.CASE_INSENSITIVE));
        }
    }


    /**
     *  Parse the given string and return a list of PatternFileFilter-s
     *  The string is a ";" delimited list of pattern specifications of the
     *  form: pattern":"description or: just the pattern. E.g.:
     *  ".*\.nc:Netcdf files;.*\.html;.*\.txt:Text files"
     *
     *  @param filterString The string of semi-colon delimited filters.
     *  @return List of PatternFileFilter objects.
     */
    public static List createFilters(String filterString) {
        List            filters = new ArrayList();
        StringTokenizer tok     = new StringTokenizer(filterString, ";");
        while (tok.hasMoreTokens()) {
            String v     = tok.nextToken();
            int    index = v.indexOf(":");
            if (index < 0) {
                filters.add(new PatternFileFilter(v.trim(), v));
            } else {
                String pattern = v.substring(0, index);
                String desc    = v.substring(index + 1);
                filters.add(new PatternFileFilter(pattern.trim(), desc));
            }
        }
        return filters;
    }

    /**
     *  Return the object id.
     *  @return the object id.
     */
    public Object getId() {
        return id;
    }

    /**
     *  Override base class method.
     *  @return the string representation of this object.
     */
    public String toString() {
        return "PATTERN<" + thePattern + ">" + " desc(" + desc + ")";
    }

    /**
     *  Does this pattern match the given file.
     *  @param file The given file.
     *  @return        Does this pattern match the given file.
     */
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return directoriesOk;
        }
        if ((file.isHidden() || file.getName().startsWith("."))  //hack for hidden files
                && !hiddenOk) {
            return false;
        }
        String name = file.getName();
        return match(name);
    }


    /**
     * Implement the FilenameFilter method
     *
     * @param dir  directory to check
     * @param name name of file
     *
     * @return true if we should accept this.
     */
    public boolean accept(File dir, String name) {
        return match(name);
    }

    /**
     *  Does this pattern match the given file.
     *  @param name The given file.
     *  @return    Does this pattern match the given file.
     */
    public boolean match(String name) {
        try {
            for (int i = 0; i < patterns.size(); i++) {
                Pattern p = (Pattern) patterns.get(i);
                if (p.matcher(name).find()) {
                    return true;
                }
            }
            return false;
        } catch (Exception exc) {
            //TODO - fix this       System.err.println ("Error matching:" + name);
            //      exc.printStackTrace ();
            return false;
        }
    }




    /**
     *  Return the description.
     *  @return the description.
     */
    public String getDescription() {
        return desc;
    }

    /**
     *  Set the PreferedSuffix property.
     *
     *  @param value The new value for PreferedSuffix
     */
    public void setPreferredSuffix(String value) {
        preferredSuffix = value;
    }

    /**
     *  Get the PreferredSuffix property.
     *
     *  @return The PreferredSuffix
     */
    public String getPreferredSuffix() {
        return preferredSuffix;
    }





    /**
     *  Test
     *  @param args Command line args.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: pattern strings");
            return;
        }
        PatternFileFilter filter = new PatternFileFilter(args[0]);
        for (int i = 1; i < args.length; i++) {
            System.err.println(args[i] + (filter.match(args[i])
                                          ? "  MATCH"
                                          : "  NO MATCH"));
        }
    }


}

