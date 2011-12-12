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

package ucar.unidata.data;


import org.w3c.dom.Element;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This represents a hierarchical category of string names
 * and is used to categorize  different flavors of data within
 * DataChoice objects and for determining what displays are applicable
 * to what data. A DataCategory object really represents a
 * node in a list of DataCategory objects that define the hierarchy.
 * For example, the string FOO-BAR-ZOO
 * is represented by three DataCategory objects:
 * <pre>
 * +-----+     +-----+     +-----+
 * | FOO |-->  | BAR | --> | ZOO |
 * +-----+     +-----+     +-----+
 * </pre>
 * There are static utility methods for parsing a string category name
 * into a chain of objects. Likewise there are methods for parsing
 * a String of semi-colon delimited category strings  into a Collection
 * of DataCategory chains.
 * <p>
 * Category names can also hold regular expressions,e.g., "*", "+" and "."
 * that gives us alot of flexibility in creating categories that match a variety
 * of flavors of data.
 * @author  IDV development team
 * @version $Revision: 1.74 $
 */
public class DataCategory {

    /** NULL category */
    public static final DataCategory NULL = null;


    /** Identifier for a category divider */
    public static final String DIVIDER = "-";

    /** name of this DataCategory */
    protected String name;

    /** child of thie DataCategory */
    protected DataCategory child;

    /** list of meta categories */
    private List metaCategories;

    /** A local cached copy of my pattern */
    private String myPattern = null;

    /** The category that matches any other category */
    public static final String CATEGORY_ANY = "*";

    /** The text category */
    public static final String CATEGORY_TEXT = "text";

    /** The html category */
    public static final String CATEGORY_HTML = "html";

    /** The display category */
    public static final String CATEGORY_DISPLAY = "DISPLAY";

    /** The grid category */
    public static final String CATEGORY_GRID = "GRID";

    /** The point category */
    public static final String CATEGORY_POINT = "POINT";

    /** The VisAD category */
    public static final String CATEGORY_VISAD = "VISAD";

    /** The image category */
    public static final String CATEGORY_IMAGE = "IMAGE";

    /** The 3D category */
    public static final String CATEGORY_3D = "3D";

    /** The 2D category */
    public static final String CATEGORY_2D = "2D";

    /** The RAOB category */
    public static final String CATEGORY_RAOB = "RAOB";

    /** The Skew-T category */
    public static final String CATEGORY_SKEWT = "skewt";

    /** The Sounding category */
    public static final String CATEGORY_SOUNDING = "Sounding";

    /** The stationplot category */
    public static final String CATEGORY_POINTPLOT = "PointPlot";

    /** The track category */
    public static final String CATEGORY_TRACK = "Track";

    /** The grid skew-T category @deprecated use GRID_SOUNDING */
    public static final String CATEGORY_GRIDSKEWT = "GRID_SKEWT";

    /** The RAOB skew-T category @deprecated use RAOB_SOUNDING */
    public static final String CATEGORY_RAOBSKEWT = "RAOB_SKEWT";

    /** The track skew-T category @deprecated use TRACK_SOUNDING */
    public static final String CATEGORY_TRACKSKEWT = "TRACK_SKEWT";

    /** The track skew-T category @deprecated use TRACK_SOUNDING */
    public static final String CATEGORY_COSMICSKEWT = "COSMIC_SKEWT";

    /** The grid skew-T category */
    public static final String CATEGORY_GRIDSOUNDING = "GRID_SOUNDING";

    /** The grid ensemble category */
    public static final String CATEGORY_ENSEMBLE = "ENSEMBLE";

    /** The RAOB skew-T category */
    public static final String CATEGORY_RAOBSOUNDING = "RAOB_SOUNDING";

    /** The track skew-T category */
    public static final String CATEGORY_TRACKSOUNDING = "TRACK_SOUNDING";

    /** The track skew-T category */
    public static final String CATEGORY_TRAJECTORYSOUNDING =
        "TRAJECTORY_SOUNDING";

    /** The one station profiler category */
    public static final String CATEGORY_PROFILER_ONESTA = "PROFILER_ONESTA";

    /** The multi station profiler category */
    public static final String CATEGORY_PROFILER_MULTISTA =
        "PROFILER_MULTISTA";

    /** The profiler plan view category */
    public static final String CATEGORY_PROFILER_PLAN = "PROFILER_PLANVIEW";

    /** The profiler 3D category */
    public static final String CATEGORY_PROFILER_3D = "PROFILER_3D";


    /** Category to use for none. Use this, for example, in a CompositeDataChoice that     really has no data and is just used as a container. */
    public static final DataCategory NONE_CATEGORY = new DataCategory("none",
                                                         false);


    /**
     * RAOB-derived data appropriate for a Skew-T display
     * ({@link visad.DateTime}, {@link visad.georef.EarthLocationTuple},
     * {@link ucar.visad.functiontypes.InSituAirTemperatureProfile},
     * {@link ucar.visad.functiontypes.DewPointProfile}).
     */
    public static final DataCategory RAOB_SKEWT_CATEGORY =
        new DataCategory(CATEGORY_RAOBSKEWT, false);

    /**
     * RAOB-derived data appropriate for a sounding display
     * ({@link visad.DateTime}, {@link visad.georef.EarthLocationTuple},
     * {@link ucar.visad.functiontypes.InSituAirTemperatureProfile},
     * {@link ucar.visad.functiontypes.DewPointProfile}).
     */
    public static final DataCategory RAOB_SOUNDING_CATEGORY =
        new DataCategory(CATEGORY_RAOBSOUNDING, false);

    /**
     * Profiler data appropriate for a one station time-hgt display
     */
    public static final DataCategory PROFILER_ONESTA_CATEGORY =
        new DataCategory(CATEGORY_PROFILER_ONESTA, false);

    /**
     * Profiler data appropriate for multi-staiton plan view of winds at hgt;
     */
    public static final DataCategory PROFILER_PLAN_CATEGORY =
        new DataCategory(CATEGORY_PROFILER_PLAN, false);










    /**
     * Grid-derived data appropriate for a Skew-T display
     * ({@link visad.DateTime} -> ({@link visad.georef.EarthLocationTuple}
     * ->  ({@link ucar.visad.quantities.AirTemperature},
     * {@link ucar.visad.quantities.DewPoint}))).
     * @deprecated use #GRID_3D_SOUNDING_CATEGORY
     */
    public static final DataCategory GRID_3D_SKEWT_CATEGORY =
        new DataCategory(CATEGORY_GRIDSKEWT, false);

    /**
     * Track-derived data appropriate for a Skew-T display
     * ({@link visad.DateTime} ->  ({@link ucar.visad.quantities.Pressure},
     * {@link ucar.visad.quantities.Temperature},
     * {@link ucar.visad.quantities.DewPoint},
     * {@link ucar.visad.quantities.PolarHorizontalWind},
     * {@link visad.georef.EarthLocationTuple})).
     * @deprecated use #TRACK_SOUNDING_CATEGORY
     */
    public static final DataCategory TRACK_SKEWT_CATEGORY =
        new DataCategory(CATEGORY_TRACKSKEWT, false);

    /** _more_ */
    public static final DataCategory COSMIC_SKEWT_CATEGORY =
        new DataCategory(CATEGORY_COSMICSKEWT, false);

    /** grid ensemble categories */
    public static final DataCategory ENSEMBLE_CATEGORY =
        new DataCategory(CATEGORY_ENSEMBLE, false);

    /**
     * Grid-derived data appropriate for an aerological sounding display
     * ({@link visad.DateTime} -> ({@link visad.georef.EarthLocationTuple}
     * ->  ({@link ucar.visad.quantities.AirTemperature},
     * {@link ucar.visad.quantities.DewPoint}))).
     */
    public static final DataCategory GRID_3D_SOUNDING_CATEGORY =
        new DataCategory(CATEGORY_GRIDSOUNDING, false);

    /**
     * Track-derived data appropriate for an aerological sounding display
     * ({@link visad.DateTime} ->  ({@link ucar.visad.quantities.Pressure},
     * {@link ucar.visad.quantities.Temperature},
     * {@link ucar.visad.quantities.DewPoint},
     * {@link ucar.visad.quantities.PolarHorizontalWind},
     * {@link visad.georef.EarthLocationTuple})).
     */
    public static final DataCategory TRACK_SOUNDING_CATEGORY =
        new DataCategory(CATEGORY_TRACKSOUNDING, false);

    /** Category for the trajectory feature type data files */
    public static final DataCategory TRAJECTORY_SOUNDING_CATEGORY =
        new DataCategory(CATEGORY_TRAJECTORYSOUNDING, false);

    /** Category for the drawing files */
    public static final DataCategory XGRF_CATEGORY = new DataCategory("xgrf",
                                                         false);

    /** Category for locations */
    public static final DataCategory LOCATIONS_CATEGORY =
        new DataCategory("locations", false);

    /**
     * Point Plot Category
     */
    public static final DataCategory POINT_PLOT_CATEGORY =
        new DataCategory(CATEGORY_POINTPLOT, false);

    /** the category index */
    private int categoryIndex = -1;

    /** the child index */
    private int childIndex = -1;

    /** appended category name */
    private String append;

    /** string pattern:value to replace in derived categories */
    private String replace;

    /** flag for whether a category if for display */
    private boolean forDisplay = true;

    /**
     * Default constructor.
     */
    public DataCategory() {
        this("");
    }

    /**
     * ctor
     *
     * @param forDisplay Is this category a display category
     */
    public DataCategory(boolean forDisplay) {
        this("", forDisplay);
    }

    /**
     * Create a parent-less category with the given name.
     *
     * @param name  name of this DataCategory.
     */
    public DataCategory(String name) {
        this(name, true);
    }

    /**
     * Create a parent-less category with the given forDisplay state
     *
     * @param name name of this DataCategory
     * @param forDisplay  true if this should be displayed
     */
    public DataCategory(String name, boolean forDisplay) {
        this.name       = name;
        this.forDisplay = forDisplay;
    }


    /**
     * Create a parent-less category with the given metacategories.
     *
     * @param name             name of this DataCategory
     * @param metaCategories   list of meta categories
     *
     */
    public DataCategory(String name, List metaCategories) {
        this.name           = name;
        this.metaCategories = metaCategories;
    }

    /**
     * Create a parent-less category with the given metacategory.
     *
     * @param name             name of this DataCategory
     * @param metaCategory     meta category
     *
     */
    public DataCategory(String name, DataCategory metaCategory) {
        this(name, Misc.newList(metaCategory));
    }


    /**
     *  Create a category with given parent category and name.
     *  Add the new category to the parent.
     *
     * @param parent   parent category
     * @param name     name of this sub category
     */
    public DataCategory(DataCategory parent, String name) {
        this.name = name;
        parent.setChild(this);
    }


    /**
     * Check if this DataCategory has a meta category.
     *
     * @return   true if it has a metacategory
     */
    public boolean hasCategory() {
        return (metaCategories != null);
    }

    /**
     * Get the list of meta categories for this DataCategory
     * @return  list of meta categories
     */
    public List getMetaCategories() {
        return metaCategories;
    }


    /** An id for a set of categories */
    public static final String TAG_CATEGORIES = "categories";

    /** An id for a single category */
    public static final String TAG_CATEGORY = "category";

    /** The name attribute (for XML) */
    public static final String ATTR_NAME = "name";

    /** The description attribute (for XML) */
    public static final String ATTR_DESC = "desc";

    /** a hashtable of all categories */
    private static Hashtable allCategoriesMap;

    /** the list of all categories */
    private static List allCategories;

    /**
     * Process the categories specified in the XML.
     *
     * @param root   root element for the XML
     */
    private static void processCategories(Element root) {
        List children = XmlUtil.findChildren(root, TAG_CATEGORY);
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) children.get(i);
            String  name  = XmlUtil.getAttribute(child, ATTR_NAME);
            String  value = XmlUtil.getAttribute(child, ATTR_DESC);
            if (allCategoriesMap.get(name) == null) {
                allCategories.add(name);
                allCategoriesMap.put(name, value);
            }
        }
    }

    /**
     * Used by XML persistence initialization.
     *
     * @param resources   collection of XML resources
     */
    public static void init(XmlResourceCollection resources) {
        if (allCategoriesMap == null) {
            allCategoriesMap = new Hashtable();
            allCategories    = new ArrayList();
        }
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i);
            if (root != null) {
                processCategories(root);
            }
        }
    }


    /** Keeps track of all of the data categories */
    private static List currentCategories = new ArrayList();

    /** Keeps track of all of the data categories */
    private static Hashtable currentCategoriesMap = new Hashtable();


    /**
     * Add the category into the global list
     *
     * @param c category
     */
    public static void addCurrentCategory(String c) {
        if (currentCategoriesMap.get(c) == null) {
            currentCategoriesMap.put(c, c);
            if ( !c.startsWith("display:") && (c.indexOf(";") < 0)
                    && !c.startsWith("param:")) {
                currentCategories.add(c);
            }
        }
    }

    /**
     * Get all current category strings
     *
     * @return List of category strings
     */
    public static List getCurrentCategories() {
        return currentCategories;
    }

    /**
     *  Parse out a string of the form "catname1-catname2-catnameN"
     *  and return a chain of category objects representing
     *  catname1->catname2->catnameN. The category objects will have their
     *  forDisplay flag set to the given value.
     *
     *
     * @param c      string of categories
     * @param forDisplay  true if the first is for display only
     * @return  new DataCategory
     */
    public static DataCategory parseCategory(String c, boolean forDisplay) {
        if ( !forDisplay) {
            addCurrentCategory(c);
        }


        StringTokenizer tok     = new StringTokenizer(c, DIVIDER);
        DataCategory    top     = null;
        DataCategory    current = null;
        while (tok.hasMoreTokens()) {
            String name = tok.nextToken().trim();
            if (name.length() == 0) {
                continue;
            }
            if (top == null) {
                top = current = new DataCategory(name, forDisplay);
            } else {
                current = new DataCategory(current, name);
            }
        }
        return top;
    }


    /**
     *  Parse out a string of semi-colon delimited categories
     *  e.g.:
     *  "catname1-catname2-catnameN;othercategory1-othercategory2;..."
     *
     * @param c   semi-colon delimeted String of categories
     * @return  list of categories
     */
    public static List parseCategories(String c) {
        return parseCategories(c, true);
    }

    /**
     *  Parse out a string of semi-colon delimited categories
     *  e.g.:
     *  "catname1-catname2-catnameN;othercategory1-othercategory2;..."
     *
     * @param c   semi-colon delimeted String of categories
     * @param firstOneForDisplay  true if the first one is for display only
     * @return  list of categories
     */
    public static List parseCategories(String c, boolean firstOneForDisplay) {
        if (c == null) {
            return new ArrayList();
        }
        StringTokenizer tok        = new StringTokenizer(c, ";");
        List            categories = new ArrayList();
        int             cnt        = 0;
        String          path       = null;
        while (tok.hasMoreTokens()) {
            cnt++;
            path = tok.nextToken().trim();
            //      if (path.length () == 0) {continue;}
            DataCategory dataCategory = parseCategory(path,
                                            ((cnt == 1)
                                             && firstOneForDisplay));
            if (dataCategory != null) {
                categories.add(dataCategory);
            }
        }
        if ((cnt == 1) && firstOneForDisplay) {
            addCurrentCategory(path);
        }
        return categories;
    }

    /**
     * Create a DataCategory from the array of names.  This is a
     * hierarchical list.
     *
     * @param names  array of category names
     * @return   associated DataCategory
     */
    public static DataCategory createCategory(String[] names) {
        DataCategory top     = null;
        DataCategory current = null;
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                break;
            }
            if (top == null) {
                top = current = new DataCategory(names[i]);
            } else {
                current = new DataCategory(current, names[i]);
            }
        }
        return top;
    }

    /**
     * Helper method to instantiate a single category
     *
     * @param n1   name of the category
     * @return  DataCategory with name n1
     */
    public static DataCategory createCategory(String n1) {
        return createCategory(new String[] { n1 });
    }

    /**
     * Helper method to instantiate two categories
     *
     * @param n1   name of the category
     * @param n2   name of the child of n1
     * @return  DataCategory with one child
     */
    public static DataCategory createCategory(String n1, String n2) {
        return createCategory(new String[] { n1, n2 });
    }

    /**
     * Helper method to instantiate a three categories
     *
     * @param n1   name of the category
     * @param n2   name of the child of n1
     * @param n3   name of the child of n2
     * @return  DataCategory with a child with a child
     */
    public static DataCategory createCategory(String n1, String n2,
            String n3) {
        return createCategory(new String[] { n1, n2, n3 });
    }

    /**
     * Helper method to instantiate a four categories
     *
     * @param n1   name of the category
     * @param n2   name of the child of n1
     * @param n3   name of the child of n2
     * @param n4   name of the child of n3
     * @return  DataCategory with a child with a child with a child
     */
    public static DataCategory createCategory(String n1, String n2,
            String n3, String n4) {
        return createCategory(new String[] { n1, n2, n3, n4 });
    }



    /**
     * Return the child category of this object.
     * Note: this can be null.
     *
     * @return  child or <code>null</code> if none.
     */
    public DataCategory getChild() {
        return child;
    }

    /**
     * Sets the child member of this category.
     * DataCategories are a single linear chain
     * of objects, thus we only have one child (and one parent)
     *
     * @param child   child category for this
     */
    public void setChild(DataCategory child) {
        this.child = child;
    }

    /**
     * Return the name of this category
     *
     * @return  category name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name property.  Used by XML persistence
     *
     * @param n   name
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * See if this is an inherited category
     *
     * @return  true if inherited
     */
    public boolean isInherited() {
        return (name.equals("inherit"));
    }

    /**
     * Return the full path of this category with its child
     * Use the default DIVIDER as the string divider between
     * sub categories.
     *
     * @return   the full name of this category (e.g. -foo-)
     */
    public String getFullName() {
        return getFullName(DIVIDER);
    }

    /**
     * Return the full path of this category with its child
     * Use the given argument as the string divider between
     * sub categories.
     *
     * @param divider  divider character
     * @return  full name using divider
     */
    public String getFullName(String divider) {
        return (name + ((child == null)
                        ? ""
                        : divider + child.getFullName(divider)));
    }

    /**
     * Return the full path of this category with its child
     * Use the given arguments as the string divider between
     * sub categories.
     *
     * @param prefix    prefix divider
     * @param suffix    suffix divider
     * @return  full name with dividers
     */
    public String getFullName(String prefix, String suffix) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append(name);
        sb.append(suffix);
        if (child != null) {
            sb.append(child.getFullName(prefix, suffix));
        }
        return sb.toString();
    }



    /**
     * Return the regular expression that this DataCategory represents
     * It is somewhat tricky because the parent category might
     * have been none (i.e., "*" or if this is the topmost category)
     *
     * @return  regular expression pattern for this category
     */
    protected String getPattern() {
        if (myPattern == null) {
            String myPart = "";
            if (name.equals("*")) {         //0 or more
                myPart = "(-[^-]-)*";
            } else if (name.equals("+")) {  //1 or more
                myPart = "(-[^-]-)+";
            } else if (name.equals(".")) {  //just one
                myPart = "-[^-]+-";
            } else {
                myPart = DIVIDER + name + DIVIDER;
            }
            if (child == null) {
                myPattern = myPart;
            } else {
                return myPattern = myPart + child.getPattern();
            }
        }
        return myPattern;
    }

    /**
     * Return a String representation of this DataCategory
     * @return string representation of this
     */
    public String toString() {
        return toString(DIVIDER);
    }


    /**
     * Return a String representation of this DataCategory using the given divider
     *
     * @param divider The divider to use between sub-categories.
     * @return string representation of this
     */
    public String toString(String divider) {
        if (child == null) {
            return name;
        } else {
            return name + divider + child.toString(divider);
        }
    }



    /**
     *  Go through the list of DataCategory objects.
     *  If this object is applicable to any of them
     *  return true. If the list is empty then return true.
     *
     * @param dcs   list of DataCategories
     * @return  true if this DataCategory is applicable to any of them
     */
    public boolean applicableTo(List dcs) {
        //Count the DataCategories that we check.
        int cnt = 0;
        for (int i = 0; i < dcs.size(); i++) {
            DataCategory dc = (DataCategory) dcs.get(i);
            //Skip over the one just meant for display
            if (dc.getForDisplay()) {
                continue;
            }
            cnt++;
            if (applicableTo(dc)) {
                return true;
            }
        }
        //If there  weren't any categories checked return true.
        return (cnt == 0);
    }

    /**
     *  Check to see if this object is applicable to the given
     *  DataCategory argument. The definition of applicability
     *  is that this data category is hierarchically a "base-class"
     *  hierachy of the given argument. There is a slight twist though:
     *  A data category can have a sub-component that represents a regular
     *  expression, i.e., :
     *  <pre>
     *  "*" represents 0 or more sub-categories,
     *  "+" represents one or more sub-categories
     *  "." represents one sub-category
     *  </pre>
     *  Here are some examples:
     *  <table>
     *  <tr><td>This</td><td>Argument</td><td> applicableTo</td></tr>
     *  <tr><td>"FOO-BAR"</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  <tr><td>"FOO-BAR-ZOO"</td><td>"FOO-BAR"</td><td>false</td></tr>
     *  <tr><td>"FOO-BAR-ZOO"</td><td>"FOO-BAR"</td><td>false</td></tr>
     *  <tr><td>"FOO-BAR-*"</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  <tr><td>"FOO-BAR-+"</td><td>"FOO-BAR"</td><td>false</td></tr>
     *  <tr><td>"FOO-."</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  <tr><td>"FOO-.-*"</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  <tr><td>"*"</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  <tr><td>".-.-*"</td><td>"FOO-BAR"</td><td>true</td></tr>
     *  </table>
     *
     * @param d   DataCategory to check
     * @return  true if applicable to <code>d</code>
     */
    public boolean applicableTo(DataCategory d) {
        String myPattern = getPattern();
        String input     = d.getFullName(DIVIDER, DIVIDER);
        if (input.equals("-*-")) {
            return true;
        }
        try {
            //boolean results = matchRegexp(input, myPattern);
            boolean results = StringUtil.stringMatch(input, myPattern);
            return results;
        } catch (Exception exc) {
            throw new IllegalArgumentException("DataCategory error:" + exc);
        }
    }

    /**
     * Set the "for display" property.  (used by XML persistence)
     *
     * @param value  for display property
     */
    public void setForDisplay(boolean value) {
        forDisplay = value;
    }

    /**
     * Get the "for display" property.  (used by XML persistence)
     *
     * @return  the for display property
     */
    public boolean getForDisplay() {
        return forDisplay;
    }


    /**
     * Return whether any of the DataCategory's in from are
     * applicable to the list in <code>to</code>.
     *
     * @param from   List of DataCategories to check
     * @param to     List of categories that <code>from</code> may be
     *               applicable to.
     *
     * @return  true if any of the <code>from</code> list is applicable
     *          to the <code>to</code> list.
     * @see #applicableTo(List)
     */
    public static boolean applicableTo(List from, List to) {
        for (int i = 0; i < from.size(); i++) {
            DataCategory category = (DataCategory) from.get(i);
            if (category.applicableTo(to)) {
                return true;
            }
        }
        return false;
    }


    /**
     * See if this DataCategory is equivalent to another
     *
     * @param o   other category
     * @return  true if <code>o</code> is a DataCategory and they
     *          have the same full name.
     */
    public boolean equals(Object o) {
        if (o instanceof DataCategory) {
            return getFullName().equals(((DataCategory) o).getFullName());
        }
        return false;
    }

    /**
     * Return the hashcode for this DataCategory
     *
     * @return  hash code
     */
    public int hashCode() {
        return getFullName().hashCode();
    }

    /**
     * Method for testing this class.
     *
     * @param args   category string
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        DataCategory test = parseCategory(args[0], false);
        for (int i = 1; i < args.length; i++) {
            DataCategory dc = parseCategory(args[i], false);
            System.err.println("Pattern:" + dc.getPattern() + " match:"
                               + dc.applicableTo(test));

        }

    }

    /*
     * private static Perl5Matcher matcher;
     * private static Perl5Compiler compiler;
     */

    /**
     * See if a String is a match for a pattern
     *
     * @param source      source string
     * @param pattern     regular expression pattern
     * @return  true if there is a match
     * @deprecated  use ucar.unidata.util.StringUtil.stringMatch(String, String) instead
     */
    public static boolean matchRegexp(String source, String pattern)
    //        throws MalformedPatternException 
    {
        return StringUtil.stringMatch(source, pattern);
        /*
        if (matcher == null) {
            matcher  = new Perl5Matcher();
            compiler = new Perl5Compiler();
        }
        return matcher.contains(source, compiler.compile(pattern));
        */
    }


    /**
     * Set the category index (used by XML persistence)
     *
     * @param value   the index
     */
    public void setCategoryIndex(int value) {
        categoryIndex = value;
    }

    /**
     * Get the category index (used by XML persistence)
     *
     * @return  the category index
     */
    public int getCategoryIndex() {
        return categoryIndex;
    }

    /**
     * Set the child index (used by XML persistence)
     *
     * @param value  the child index
     */
    public void setChildIndex(int value) {
        childIndex = value;
    }

    /**
     * Get the child index (used by XML persistence)
     * @return  child index
     */
    public int getChildIndex() {
        return childIndex;
    }


    /**
     * Set the appended category string (used by XML persistence)
     *
     * @param value   append value
     */
    public void setAppend(String value) {
        append = value;
    }

    /**
     * Get the appended category string (used by XML persistence)
     * @return  appended category string
     */
    public String getAppend() {
        return append;
    }

    /**
     *  Set the Replace property.
     *
     *  @param value The new value for Replace
     */
    public void setReplace(String value) {
        this.replace = value;
    }

    /**
     *  Get the Replace property.
     *
     *  @return The Replace
     */
    public String getReplace() {
        return this.replace;
    }



    /**
     * Append the string to the data category and return a new category.
     *
     * @param append category string to append  (may be null)
     *
     * @return The new data category
     */
    public DataCategory copyAndAppend(String append) {
        String newName = this.toString();
        if (append != null) {
            newName = newName + DIVIDER + append;
        }
        return parseCategory(newName, getForDisplay());
    }
}
