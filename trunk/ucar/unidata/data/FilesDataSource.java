/*
 * $Id: FilesDataSource.java,v 1.26 2007/06/21 12:30:01 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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


import ucar.ma2.Range;


import ucar.nc2.*;

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.point.PointOb;

import ucar.unidata.data.point.PointObFactory;

import ucar.unidata.ui.TwoListPanel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;


import visad.*;

import visad.georef.EarthLocationTuple;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A data source for balloon and aircraft tracks
 *
 * @author IDV Development Team
 * @version $Revision: 1.26 $ $Date: 2007/06/21 12:30:01 $
 */
public abstract class FilesDataSource extends DataSourceImpl {

    /** List of sources files */
    protected List sources;

    /** List of sources files */
    protected List adapters;


    /** for unpersistence */
    protected String oldSourceFromBundles;



    /** Default Constructor */
    public FilesDataSource() {}

    public FilesDataSource(DataSourceDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param name The name
     * @param description The long name
     * @param properties properties
     */
    public FilesDataSource(DataSourceDescriptor descriptor, String name,
                           String description, Hashtable properties) {
        this(descriptor, Misc.newList(name), name, description, properties);
    }


    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param newSources List of files or urls
     * @param description The long name
     * @param properties properties
     */
    public FilesDataSource(DataSourceDescriptor descriptor, List newSources,
                           String description, Hashtable properties) {
        this(descriptor, newSources, newSources.get(0).toString(),
             description, properties);
    }


    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param newSources       List of sources of data (filename/URL)
     * @param name my name
     * @param description   description of the data
     * @param properties    extra properties for initialization
     */
    public FilesDataSource(DataSourceDescriptor descriptor, List newSources,
                           String name, String description,
                           Hashtable properties) {
        super(descriptor, name, description, properties);

        int idx = name.indexOf("?");
        if(idx>=0) {
            name= name.substring(0,idx);
            setName(name);
        }

        if (getName().length() == 0) {
            if (newSources.size() > 0) {
                String s = newSources.get(0).toString();
                idx = s.indexOf("?");
                if(idx>=0) s= s.substring(0,idx);
                setName(s);
            } else {
                setName(description);
            }
        }
        this.sources = newSources;
    }


    /**
     * Get the file paths (or urls or whatever) that are to be changed
     * when we are unpersisted and are in data relative mode
     *
     * @return file paths to changed
     */
    public List getDataPaths() {
        return sources;
    }

    /**
     * Are we getting data from a file or from server
     *
     * @return is the data from files
     */
    protected boolean isFileBased() {
        if (sources.size() == 0) {
            return false;
        }
        return (new File(sources.get(0).toString())).exists();
    }

    /**
     * Get the file extension
     *
     * @param file The file
     *
     * @return its extension
     */
    protected String getDataFileExtension(String file) {
        return IOUtil.getFileExtension(file).trim();
    }



    /**
     * Process the file name to get a file that we can use to write to local disk
     *
     * @param filename Filename
     * @param index Which file it it. This can be used by derived classes to add more info to the file name
     *
     * @return The processed filename
     */
    protected String processDataFilename(String filename, int index) {
        int idx = filename.indexOf("?");
        if (idx >= 0) {
	    //            filename = filename.substring(0, idx);
        }
        return filename;
    }

    /**
     * Save the data source files to local disk
     *
     * @param prefix The directory prefix and unique file prefix
     * @param loadId For stopping the load through the JobManager
     * @param changeLinks Should this data source also change its internal data references
     *
     * @return List of the files that were written
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        List processedSources = new ArrayList();
        for (int i = 0; i < sources.size(); i++) {
	    String  source  = sources.get(i).toString();
	    //	    System.err.println(source);
            String file = processDataFilename(source, i);
            if (file == null) {
                continue;
            }
            processedSources.add(file);
        }
        String extension = getDataFileExtension(sources.get(0).toString());
        List newFiles = IOUtil.writeTo(getInputStreams(processedSources),
                                       prefix, extension, loadId);
        if ((newFiles != null) && changeLinks) {
            setNewFiles(newFiles);
        }
        return newFiles;
    }


    /**
     * Get a list of input streams, one for each given file
     *
     * @param processedSources The sources
     *
     * @return List of input streams
     *
     * @throws Exception On badness
     */
    protected List getInputStreams(List processedSources) throws Exception {
        List is = new ArrayList();
        for (int i = 0; i < processedSources.size(); i++) {
            is.add(IOUtil.getInputStream(processedSources.get(i).toString(),
                                         getClass()));
        }
        return is;
    }


    /**
     * Initialize after we have been directly created
     */
    public void initAfterCreation() {
        initWithPollingInfo();
        super.initAfterCreation();
    }


    /**
     * Return the human readable description of this DataSource
     *
     * @return   the description
     */
    public String getPartialDescription() {
        String desc = super.getFullDescription();
        if ((sources != null) && (sources.size() > 0)) {
            desc = desc + "<p><b>Files:</b><ul>";
            for (int i = 0; i < sources.size(); i++) {
                desc = desc + "<li>" + sources.get(i) + "\n";
            }
            desc = desc + "</ul>";
        }
        return desc;
    }



    /**
     * get the detailed description
     *
     * @return description
     */
    public String getFullDescription() {
        return getPartialDescription();
    }

    /**
     * Do I have any sources
     *
     * @return Has sources
     */
    public boolean haveSources() {
        return (sources != null) && (sources.size() > 0);
    }

    /**
     * Initialize after XML decoding.
     */
    public void initAfterUnpersistence() {
        if (oldSourceFromBundles != null) {
            sources              = Misc.newList(oldSourceFromBundles);
            oldSourceFromBundles = null;
        }
        List tmpPaths = getTmpPaths();
        if (tmpPaths != null) {
            sources = new ArrayList(tmpPaths);
            setTmpPaths(null);
        }
        super.initAfterUnpersistence();
        initWithPollingInfo();
    }


    /**
     * This gets called when the user interactively does a Change data
     *
     * @param newObject The new data object. May be a string or a list
     * @param newProperties new properties
     */
    public void updateState(Object newObject, Hashtable newProperties) {
        super.updateState(newObject, newProperties);
        if (newObject instanceof List) {
            List newSources = (List) newObject;
            if ((newSources.size() > 0)
                    && (newSources.get(0) instanceof String)) {
                sources = new ArrayList(newSources);
            }
        } else if (newObject instanceof String) {
            sources = Misc.newList(newObject);
        }
    }



    /**
     * Initialze sources from polling info
     */
    protected void initWithPollingInfo() {
        PollingInfo pollingInfo = getPollingInfo();
        if (pollingInfo.doILookForNewFiles()) {
            adapters     = null;
            this.sources = pollingInfo.getFiles();
            sourcesChanged();
        }
    }



    /**
     * The user changed the properties. Reinitialize from polling info if we have it
     */
    protected void propertiesChanged() {
        PollingInfo pollingInfo = getPollingInfo();
        if (pollingInfo.doILookForNewFiles()) {
            List newSources = pollingInfo.getFiles();
            if ( !Misc.equals(sources, newSources)) {
                adapters = null;
                sources  = newSources;
                sourcesChanged();
            }
        }
        super.propertiesChanged();
    }


    /**
     * Used to change the files we use when loaded in from a bundle
     *
     * @param files Files to use
     */
    public void setNewFiles(List files) {
    	setTmpPaths(null);
        sources = new ArrayList();
        for (int i = 0; i < files.size(); i++) {
            sources.add(files.get(i).toString());
        }
        adapters = null;
        sourcesChanged();
    }


    /**
     * Something changed
     */
    protected void sourcesChanged() {
        clearTimes();
        //if ((dataChoices == null) || (dataChoices.size() == 0)) {
        dataChoices = null;
        getDataChoices();
        getDataContext().dataSourceChanged(this);
        //        }
        reloadData();
        //        notifyDataChange();
    }


    public void reloadData(Object object, Hashtable properties) {
	if(object instanceof String) {
	    sources = Misc.newList(object);
	} else if(object instanceof List)  {
	    sources = (List) object;
	} else {
	    throw new IllegalArgumentException("Unkown data:" + object);
	}
	reloadProperties(properties);
	sourcesChanged();
    }



    /**
     * Get the location where we poll.
     *
     * @return Directory to poll on.
     */
    protected List getLocationsForPolling() {
        if (sources == null) {
            return null;
        }
        return new ArrayList(sources);
    }

    /**
     * If we are polling some directory this method gets called when
     * there is a new file.  We set the file name, clear our state,
     * reload the metadata and tell listeners
     * of the change.
     *
     * @param files New files
     */
    public void newFilesFromPolling(List files) {
        adapters = null;
        initWithPollingInfo();
        notifyDataChange();
    }


    /**
     * Called when Datasource is removed.
     */
    public void doRemove() {
        super.doRemove();
        adapters = null;
    }



    /**
     * Clear out and reinitialize the track
     */
    public void reloadData() {
        adapters = null;
        super.reloadData();
    }


    /**
     * Get the first file path in the sources list
     *
     * @return file path
     */
    protected String getFilePath() {
        if ((sources != null) && (sources.size() > 0)) {
            return (String) sources.get(0);
        }
        return null;
    }


    /**
     * Set the sources property (filename or URL).  Used by persistence
     *
     * @param value  data sources
     */
    public void setSources(List value) {
        sources = value;
    }

    /**
     * Get the source property (filename or URL).  Used by persistence
     * @return  data source
     */
    public List getSources() {
        return sources;
    }

    /**
     * Get the string value of the first entry in the sources list
     *
     * @return The first source
     */
    protected String getSource() {
        if ((sources != null) && (sources.size() > 0)) {
            return sources.get(0).toString();
        }
        return null;
    }

    /**
     * Set the list of sources to be a list containing the given value
     *
     * @param value value
     */
    public void setSource(String value) {
        sources = Misc.newList(value);
    }


    /**
     * Are we equals
     *
     * @param that that
     *
     * @return is equals
     */
    public boolean equals(Object that) {
        if ( !super.equals(that)) {
            return false;
        }
        return Misc.equals(sources, ((FilesDataSource) that).sources);
    }

}

