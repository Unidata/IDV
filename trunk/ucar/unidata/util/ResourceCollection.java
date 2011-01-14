/*
 * $Id: ResourceCollection.java,v 1.47 2006/10/30 18:10:31 jeffmc Exp $
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * @author Metapps development team
 * @version $Revision: 1.47 $ $Date: 2006/10/30 18:10:31 $
 */



public class ResourceCollection {


    /** The id of this collection */
    protected String id;


    /** The description */
    protected String description;

    /** List of (String) paths to the resources */
    private List resources;

    /** Cache of resource contents */
    private Hashtable cache = new Hashtable();


    /** A mapping from id (String) to the resource path */
    protected Hashtable idToPath = new Hashtable();

    /** A mapping from resource path  to id */
    protected Hashtable pathToId = new Hashtable();

    /** Path to the writable resource */
    protected Resource writableResource;

    /** Index of the writable index */
    protected int writableIndex = -1;

    /** The canloadmore flag */
    private boolean canLoadMore = true;



    /**
     *  Create a ResourceCollection with the given id. The description
     *  of this collection is the id.
     *
     *  @param id The id of this ResourceCollection
     */
    public ResourceCollection(String id) {
        this(id, id);
    }


    /**
     *  Create a ResourceCollection with the given id ad description
     *
     *  @param id The id of this ResourceCollection
     *  @param description The description of this ResourceCollection
     *
     */

    public ResourceCollection(String id, String description) {
        this.id          = id;
        this.description = description;
        resources        = new ArrayList();
    }

    /**
     * Ctor
     *
     * @param id Resource id
     * @param resources List of resources
     *
     */
    public ResourceCollection(String id, List resources) {
        this(id, ((resources.size() > 0)
                  ? resources.get(0).toString()
                  : (String) null), resources);
    }

    /**
     * Ctor
     *
     * @param id Resource id
     * @param resources List of resources
     * @param writable The writable resource
     * @deprecated not good anymore
     *
     */
    public ResourceCollection(String id, String writable, List resources) {
        this.id        = id;
        this.resources = convertResources(resources);
        if (writable != null) {
            this.writableResource = new Resource(writable);
            writableIndex         = resources.indexOf(writableResource);
        }
    }


    /**
     * copy ctor
     *
     * @param id new id
     * @param that resourcecollection to copy from
     */
    public ResourceCollection(String id, ResourceCollection that) {
        this.id        = id;
        this.resources = new ArrayList(that.resources);
    }


    /**
     * Do we contain the given path
     *
     * @param path the resource path
     *
     * @return do we contain the resource path
     */
    public boolean contains(String path) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).toString().equals(path)) {
                return true;
            }
        }
        return false;

    }

    /**
     * A utility to change any a list of strings into a lists of Resource
     * objects. If the list is not strings do nothing
     *
     * @param rs List of resources to convert
     *
     * @return converted list
     */
    private List convertResources(List rs) {
        if ((rs.size() > 0) && (rs.get(0) instanceof String)) {
            List tmp = new ArrayList();
            for (int i = 0; i < rs.size(); i++) {
                tmp.add(new Resource(rs.get(i).toString()));
            }
            rs = tmp;
        }
        return rs;

    }

    /**
     * Associate the resource id with the path
     *
     * @param id The id
     * @param path The path
     */
    public void setIdForPath(String id, String path) {
        if (idToPath.get(id) == null) {
            idToPath.put(id, path);
            pathToId.put(path, id);
        }
    }

    /**
     * Find the path for the given id
     *
     * @param id The id
     *
     * @return The path
     */
    public String getPathFromId(String id) {
        return (String) idToPath.get(id);
    }


    /**
     * Find the id for the given resource index
     *
     * @param idx The resource index
     *
     * @return The id. May be null.
     */
    public String getResourceId(int idx) {
        String path = (String) get(idx);
        if (path == null) {
            return null;
        }
        return (String) pathToId.get(path);
    }



    /**
     * Should we keep loading resources from this collection. Have we seen the loadmore=false yet.
     * @return Keep loading resources
     */
    public boolean getCanLoadMore() {
        return canLoadMore;
    }

    /**
     * Set if we can keep loading resources
     *
     * @param lm value
     */
    public void setCanLoadMore(boolean lm) {
        canLoadMore = lm;
    }

    /**
     * The id of the resource collection
     * @return resource id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of the resource collection
     *
     * @param id The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Is the given resource index valid. We try to read that resource.
     *
     * @param i Resource index
     * @return Do we have it
     */
    public boolean isValid(int i) {
        return read(i) != null;
    }

    /**
     *  Return the label (or null if none defined)
     * for the given resource.
     *
     * @param resourceIdx
     *  @return A short name for the given resource.
     */
    public String getLabel(int resourceIdx) {
        Resource resource = (Resource) resources.get(resourceIdx);
        return resource.label;
    }


    /**
     * Get the named property from the given resource
     *
     * @param name property name
     * @param resourceIdx which resource
     *
     * @return property value or null
     */
    public String getProperty(String name, int resourceIdx) {
        Resource resource = (Resource) resources.get(resourceIdx);
        return resource.getProperty(name);
    }


    /**
     *  Return an abbreviated name of the given resource for display purposes.
     *
     *
     * @param resourceIdx
     *  @return A short name for the given resource.
     */
    public String getShortName(int resourceIdx) {
        //For now just return 
        String label = getLabel(resourceIdx);
        if (label != null) {
            return label;
        }
        String path = get(resourceIdx).toString();
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println("decoding error:" + uee);
        }

        String tail = IOUtil.getFileTail(path);
        if (tail.endsWith(".xml")) {
            tail = IOUtil.stripExtension(tail);
        }
        return "..." + IOUtil.getFileTail(IOUtil.getFileRoot(path)) + "/"
               + tail;
    }

    /**
     * Add in the given maps of path to label.
     *
     * @param labelMap Pat to label map
     * @deprecated not used anymore
     */
    public void addLabels(Hashtable labelMap) {}


    /**
     * Add the list of resources
     *
     * @param rs List of Resource-s
     */
    public void addResources(List rs) {
        rs = convertResources(rs);
        if (resources.size() == 0) {
            resources.addAll(rs);
        } else {
            resources.addAll(1, rs);
        }
        checkWritable();
    }


    /**
     * Set the writable resource to be the first one in the list
     */
    private void checkWritable() {
        if ((writableResource == null) && (resources.size() > 0)) {
            writableResource = new Resource(resources.get(0).toString());
            writableIndex    = 0;
        }
    }


    /**
     * Remove the index'th resource
     *
     * @param index the index to remove
     */
    public void removeResource(int index) {
        resources.remove(index);
    }


    /**
     * Add the resource. We create  a  new Resource
     *
     * @param resource The resource path
     */
    public void addResource(String resource) {
        addResource(new Resource(resource));
    }

    /**
     * Add the resource.
     *
     * @param resource The resource
     */
    public void addResource(Resource resource) {
        resources.add(resource);
        checkWritable();
    }


    /**
     * Add the resource.
     *
     * @param resourcePath  the path to the resource
     */
    public void addResourceAtStart(String resourcePath) {
        addResourceAtStart(resourcePath, null);
    }


    /**
     * Add the given resource to the beginning of the list
     *
     * @param resourcePath resource path
     * @param label label
     */
    public void addResourceAtStart(String resourcePath, String label) {
        addResourceAtStart(new Resource(resourcePath, label, null));
    }

    /**
     * Add the given resource to the beginning of the list
     *
     * @param resource The resource
     */
    public void addResourceAtStart(Resource resource) {
        if (resources.size() == 0) {
            resources.add(resource);
        } else {
            resources.add(1, resource);
        }
        checkWritable();
    }



    /**
     * How many resources
     * @return How many resources
     */
    public int size() {
        return resources.size();
    }

    /**
     * Is the i'th resource writable
     *
     * @param i Resource index
     * @return  Is it a writable file
     */
    public boolean isWritable(int i) {
        try {
            File f = new File(get(i).toString());
            return f.canWrite();
        } catch (Exception exc) {}
        return false;
    }


    /**
     * Is the given path an http based path
     *
     * @param resource Resource path
     * @return Is it http
     */
    public boolean isHttp(String resource) {
        return resource.trim().startsWith("http://");
    }


    /**
     *  Is the given index an http based path
     *
     *  @param i Resource index
     *  @return Is it http
     */
    public boolean isHttp(int i) {
        return isHttp(get(i).toString());
    }


    /**
     * Return the name of the i'th  resource
     *
     * @param i The resource index
     * @return The name of the i'th resource
     */
    public Object get(int i) {
        Resource resource = (Resource) resources.get(i);
        return resource.path;
    }

    /**
     * Is the ith resource a writable resource
     *
     * @param i Resource index
     * @return is writable
     */
    public boolean isWritableResource(int i) {
        if (writableResource == null) {
            return false;
        }
        return writableIndex == i;
    }

    /**
     * Read in the writable resource file
     * @return The writable resource contents or null
     */
    public String readWritableResource() {
        return read(writableIndex);
    }

    /**
     *  Write the given contents into the writable resource file
     *
     * @param contents
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void writeWritableResource(String contents)
            throws java.io.FileNotFoundException, java.io.IOException {

        IOUtil.writeFile(getWritable(), contents);
        if (writableIndex >= 0) {
            cache.put(get(writableIndex), contents);
        }
    }



    /**
     * Read and return the contents of the resource. Return null if cannot be read.
     *
     * @param resource The resource
     * @param lookAtCache Should we look in the cache or reread
     * @return Contents or null
     */
    protected String read(Resource resource, boolean lookAtCache) {
        String result = null;
        if (resource == null) {
            throw new IllegalArgumentException("Null resource provided");
        }
        try {
            if (lookAtCache) {
                Object cached = cache.get(resource);
                if (cached != null) {
                    if ( !(cached instanceof String)) {
                        return null;
                    }
                    return (String) cached;
                }
            }
            result = IOUtil.readContents(resource.path, getClass());


            //Check to see if this was an http resource and we got back html
            //Some web servers return a web page to be oh so handy instead of a 404
            if ((result != null) && isHttp(resource.path)
                    && Misc.isHtml(result)) {
                result = null;
            }
            //            if (isHttp(resource))
            //                System.err.println ("read:" + resource + " result = " +result);

            if (resource.properties != null) {
                List patterns = new ArrayList();
                List values   = new ArrayList();
                for (Enumeration keys = resource.properties.keys();
                        keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    patterns.add("${" + key + "}");
                    values.add(resource.properties.get(key));
                }
                result = Misc.replaceList(result, patterns, values);
            }

            cache.put(resource, result);

        } catch (Exception exc) {
            if (LogUtil.getDebugMode()) {
                LogUtil.consoleMessage("Warning: failed to read " + id + ": "
                                       + resource + "\n");
            }
            //Put an error in the cache
            cache.put(resource, new Boolean(false));
            //error logging ??
        }
        return result;
    }

    /**
     * Read the ith resource
     *
     * @param i The resource index
     * @param lookAtCache Should we look in the cache or reread
     * @return Contents or null
     */
    public String read(int i, boolean lookAtCache) {
        return read((Resource) resources.get(i), lookAtCache);
    }


    /**
     * Read the ith resource
     *
     * @param i The resource index
     * @return Contents or null
     */
    public String read(int i) {
        return read(i, true);
    }

    /**
     * Get the list of resources
     * @return List of resources
     */
    public List getResources() {
        return resources;
    }

    /**
     * Delete, if possible, the writable resource file
     */
    public void removeWritable() {
        String writableResource = getWritable();
        if (writableResource == null) {
            return;
        }
        cache.remove(writableResource);
        try {
            (new File(writableResource)).delete();
        } catch (Exception exc) {}
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        cache = new Hashtable();
    }


    /**
     * Delete, if possible, all resource files.
     */
    public void deleteAllFiles() {
        for (int i = 0; i < size(); i++) {
            String file = (String) get(i);
            try {
                (new File(file)).delete();
            } catch (Exception exc) {}
        }
    }


    /**
     * Reset all structures
     */
    public void removeAll() {
        cache     = new Hashtable();
        resources = new ArrayList();
        idToPath  = new Hashtable();
        pathToId  = new Hashtable();
    }


    /**
     * Get the string path of the writable file resource.
     * @return File path
     */
    public String getWritable() {
        if (writableResource != null) {
            return writableResource.toString();
        }
        return null;
    }


    /**
     * Do we have a writable resource
     * @return Do we have a writable resource
     */
    public boolean hasWritableResource() {
        return (writableResource != null)
               && (writableResource.toString().trim().length() > 0);
    }


    /**
     * The toString method
     *
     * @return String representation
     */
    public String toString() {
        return " ResourceCollection:" + resources;
    }

    /**
     *  Set the Description property.
     *
     *  @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     *  Get the Description property.
     *
     *  @return The Description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Class Resource holds a string path which may be a file, url or java resource
     * path. Also holds a label and a set of properties.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.47 $
     */
    public static class Resource {

        /** File, url or java resource */
        String path;

        /** Descriptive label */
        String label;

        /** Properties */
        Hashtable properties;


        /**
         * ctor
         *
         * @param path path
         */
        public Resource(String path) {
            this(path, null, null);
        }

        /**
         * ctor
         *
         * @param path path
         * @param label label
         * @param properties properties
         */
        public Resource(String path, String label, Hashtable properties) {
            this.path       = path;
            this.label      = label;
            this.properties = properties;
        }


        /**
         * _more_
         *
         * @param name _more_
         *
         * @return _more_
         */
        public String getProperty(String name) {
            if (properties == null) {
                return null;
            }
            return (String) properties.get(name);
        }


        /**
         * toString
         *
         * @return toString
         */
        public String toString() {
            return path;
        }


        /**
         * equals
         *
         * @param obj object
         *
         * @return equals
         */
        public boolean equals(Object obj) {
            if ( !(obj instanceof Resource)) {
                return false;
            }
            Resource that = (Resource) obj;
            return Misc.equals(this.path, that.path);
            //                   && Misc.equals(this.label, that.label)
            //                   && Misc.equals(this.properties, that.properties);
        }


    }



}

