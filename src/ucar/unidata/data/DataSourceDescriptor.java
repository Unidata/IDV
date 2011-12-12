/*
 * $Id: DataSourceDescriptor.java,v 1.18 2006/12/01 20:41:21 jeffmc Exp $
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

package ucar.unidata.data;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;


import visad.VisADException;

import java.io.File;
import java.io.FileOutputStream;

import java.lang.reflect.Constructor;


import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.filechooser.FileFilter;




/**
 * A class to hold the descriptive metadata about a DataSource
 *
 * @author IDV development team
 * @version $Revision: 1.18 $
 */
public class DataSourceDescriptor {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DataSourceDescriptor.class.getName());

    /** Id for this DataSourceDescriptor */
    protected String id;

    /** The data manager */
    protected DataManager manager;

    /** The factory class */
    protected Class factoryClass;

    /** Properties table */
    protected Hashtable properties;

    /** label to display */
    protected String label;

    /** flag for fileseletion */
    protected boolean fileSelection = false;

    /** flag for accepting multiple data references */
    protected boolean doesMultiples = false;

    /** pattern string */
    protected String patterns;

    /** pattern file filter */
    private FileFilter patternFilter;

    /** The ncml template from the datasource.xml entry */
    private String ncmlTemplate;

    /** can this data source be instantiated stand-alone */
    private boolean standalone = false;


    /**
     * Default constructor; does nothing
     */
    public DataSourceDescriptor() {}

    /**
     * Create a DataSourceDescriptor
     *
     * @param id            id for this
     * @param label         text for a label
     * @param manager       Associated DataManager
     * @param factory       the factory class
     * @param patterns      set of patterns
     * @param fileSelection true if this supports file selection
     * @param doesMultiples true if this handles multiple ids
     * @param props         extra properties
     *
     */
    public DataSourceDescriptor(String id, String label, DataManager manager,
                                Class factory, String patterns,
                                boolean fileSelection, boolean doesMultiples,
                                Hashtable props) {
        this.id            = id;
        this.label         = label;
        this.manager       = manager;
        this.patterns      = patterns;
        this.fileSelection = fileSelection;
        this.doesMultiples = doesMultiples;
        this.factoryClass  = factory;
        this.properties    = props;
    }


    /**
     * Return the hashcode for this DataSourceDescription
     * 8
     * @return  the hash code
     */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * See if the object in question is equal to this DataSourceDescriptor
     *
     * @param o    Object in question
     * 8
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ( !(o instanceof DataSourceDescriptor)) {
            return false;
        }
        DataSourceDescriptor that = (DataSourceDescriptor) o;
        return id.equals(that.id);
    }


    /**
     * Get the context for this data.
     *
     * @return  the data context
     */
    public DataContext getDataContext() {
        return ((manager != null)
                ? manager.getDataContext()
                : null);
    }


    /**
     * Get the id for this DataSourceDescriptor.
     *
     * @return  the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id for this DataSourceDescriptor.
     *
     * @param id   new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set the does multiples property
     *
     * @param value   true to handle, false to not
     */
    public void setDoesMultiples(boolean value) {
        doesMultiples = value;
    }

    /**
     * Set the does multiples property
     *
     * @return  true if does multiples
     */
    public boolean getDoesMultiples() {
        return doesMultiples;
    }


    /**
     * Get the DataManager for this DataSourceDescriptor
     *
     * @return the DataManager
     */
    public DataManager getDataManager() {
        return manager;
    }

    /**
     * Set the DataManager for this DataSourceDescriptor
     *
     * @param m   The DataManager
     */
    public void setDataManager(DataManager m) {
        this.manager = m;
    }


    /**
     * Get the label for this DataSourceDescriptor
     *
     * @return  the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the label for this DataSourceDescriptor
     *
     * @param label   The label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get the file selection property
     *
     * @return  true if this is a source for file data
     */
    public boolean getFileSelection() {
        return fileSelection;
    }

    /**
     * Set the file selection property
     *
     * @param fileSelection   true to set up for file selection
     */
    public void setFileSelection(boolean fileSelection) {
        this.fileSelection = fileSelection;
    }

    /**
     * Get the factory class for this DatSourceDescriptor
     *
     * @return  the class
     */
    public Class getFactoryClass() {
        return factoryClass;
    }


    /**
     * Get any properties associated with this
     *
     * @return  the properties or <code>null</code>
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Set the properties.
     *
     * @param p   new properties
     */
    public void setProperties(Hashtable p) {
        this.properties = p;
    }

    /**
     * Get the property.
     *
     * @param property  key for property
     * @return  property value
     */
    public String getProperty(String property) {
        return (String) properties.get(property);
    }

    /**
     * Return a string representation of this DataSourceDescriptor
     * 8
     * @return a string representation of this
     */
    public String toString() {
        return "Label:" + label + " id:" + id + " class:"
               + factoryClass.getName();
    }



    /**
     * Get the file pattern filter for this Descriptor
     *
     * @return  the FileFilter
     */
    public FileFilter getFilePatternFilter() {
        if (patterns == null) {
            return null;
        }
        if (patternFilter == null) {
            patternFilter = new PatternFileFilter(patterns);
        }
        return patternFilter;
    }


    /**
     * Get the file pattern filter for this Descriptor
     *
     * @return  the FileFilter
     */
    public PatternFileFilter getPatternFileFilter() {
        return (PatternFileFilter) getFilePatternFilter();
    }


    /**
     * Set the Patterns property.
     *
     * @param value The new value for Patterns
     */
    public void setPatterns(String value) {
        patterns = value;
    }

    /**
     * Get the Patterns property.
     *
     * @return The Patterns
     */
    public String getPatterns() {
        return patterns;
    }

    /**
     *  Set the NcmlTemplate property.
     *
     *  @param value The new value for NcmlTemplate
     */
    public void setNcmlTemplate(String value) {
        ncmlTemplate = value;
    }

    /**
     *  Get the NcmlTemplate property.
     *
     *  @return The NcmlTemplate
     */
    public String getNcmlTemplate() {
        return ncmlTemplate;
    }

    /**
     *  Set the Standalone property.
     *
     *  @param value The new value for Standalone
     */
    public void setStandalone(boolean value) {
        standalone = value;
    }

    /**
     *  Get the Standalone property.
     *
     *  @return The Standalone
     */
    public boolean getStandalone() {
        return standalone;
    }



}

