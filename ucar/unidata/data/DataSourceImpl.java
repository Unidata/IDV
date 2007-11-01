/**
 * $Id: DataSourceImpl.java,v 1.221 2007/08/08 19:12:22 jeffmc Exp $
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


import org.w3c.dom.Document;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



import ucar.unidata.collab.SharableImpl;


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;






import ucar.unidata.idv.DisplayControl;

import ucar.unidata.idv.ui.DataControlDialog;
import ucar.unidata.idv.ui.DataSelectionWidget;
import ucar.unidata.idv.ui.IdvUIManager;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.FilePoller;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Poller;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlUtil;

import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DateTime;
import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



import javax.swing.*;
import javax.swing.event.*;

import javax.swing.filechooser.FileFilter;



/**
 * An abstract class that implements the DataSource interface.
 * Holds a DataContext, name and description and manages a list of DataChoice-s.
 *
 * <p>This class is thread-compatible but not thread-safe.  Concurrent access to
 *  instances of this class should be externally symchronized by the client.</p>
 *
 *  @author IDV Development Team
 *  @version $Revision: 1.221 $
 */

public class DataSourceImpl extends SharableImpl implements DataSource,
        DataSourceFactory, XmlPersistable {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DataSourceImpl.class.getName());


    /** Used for doing file path switches on a bundle load */
    private List tmpPaths;


    /** Has this data source been marked and persisted off so, when unpersisted the user is prompted to set the file paths */
    private boolean dataIsEditable = false;


    /** Use this to synchronize on the getDataChoices call */
    private Object DATACHOICES_MUTEX = new Object();


    /** Use this so this object is unique in the data cache */
    protected Object dataCacheKey = ucar.unidata.util.Misc.getUniqueId();

    /**
     *  The alias property.
     */
    private String alias = "";

    /** next unique id */
    private static int nextId;

    /** count of data choices */
    private int dataChoiceCnt = 0;

    /** flag for errors */
    private boolean inError = false;


    /** flag for errors */
    private boolean needToShowErrorToUser = true;

    /** private error message */
    private String errorMessage = null;

    /** name of the DataSource */
    private String name = "";


    /** The description of this DataSource */
    String description = "";

    /** Used for doing relative file paths */
    private List relativePaths;

    /** List of associated DataChoices */
    protected List dataChoices = null;

    /** The descriptor for this DataSource */
    private DataSourceDescriptor descriptor;

    /** List of available times */
    private List timesList = null;

    /** Private DataSelection */
    private DataSelection theDataSelection;

    /** set of properties */
    private Hashtable properties;


    /**
     *  A list of the listeners interested in when this data source has changed.
     */
    private List dataChangeListeners = new ArrayList();


    /**
     *  If we are polling on a directory we keep the Poller around so
     *  on a doRemove we can tell the poller to stop running.
     */
    private List pollers;

    /** Holds information for polling */
    private PollingInfo pollingInfo;

    /** Has the initPolling been called yet */
    private boolean haveInitedPolling = false;


    /** The properties dialog */
    private JDialog propertiesDialog;

    /** The edit pane to show details html in */
    JEditorPane detailsEditor;

    /** Widget for properties dialog */
    private JCheckBox reloadCbx;

    /** geoselection panel */
    private GeoSelectionPanel geoSelectionPanel;

    /** Widget for properties dialog */
    private JTextField aliasFld;

    /** Widget for properties dialog */
    private JTextField nameFld;

    /** Used to show the times */
    private DataSelectionWidget dsw;

    /** Has this data source been created from a bundle */
    protected boolean haveBeenUnPersisted = false;

    /** If I have been unpersisted fomr a bundle has the appropriate method been called */
    private boolean initAfterUnpersistenceBeenCalled = false;

    /** How many get data calls are we currently waiting on */
    private static int outstandingGetDataCalls = 0;


    /** properties widget */
    private JCheckBox cacheDataToDiskCbx;

    /** properties widget */
    private JTextField cacheClearDelayFld;

    /** do we cache data to disk */
    private boolean cacheDataToDisk = false;

    /** How long do we wait until we clear the cache */
    private long cacheClearDelay = 0;

    /** Where we cache data */
    private String dataCachePath;

    /** _more_ */
    private List paramsToShow;

    /**
     *  Bean constructor
     */
    public DataSourceImpl() {
        haveBeenUnPersisted = true;
    }


    /**
     * Create a DataSourceImpl
     *
     * @param descriptor   the descriptor for this DataSource
     *
     */
    public DataSourceImpl(DataSourceDescriptor descriptor) {
        this(descriptor, null, null, null);
    }



    /**
     * Create this DataSourceImpl, setting the dataContext, name and description
     * attributes.
     *
     * @param descriptor     the descriptor for this DataSource
     * @param name           the name for this
     * @param description    the description
     * @param properties     extra properties
     */
    public DataSourceImpl(DataSourceDescriptor descriptor, String name,
                          String description, Hashtable properties) {
        this.properties = properties;
        if (this.properties == null) {
            this.properties = new Hashtable();
        }
        if ((descriptor != null) && (descriptor.getProperties() != null)) {
            this.properties.putAll(descriptor.getProperties());
        }
        this.descriptor  = descriptor;
        this.description = description;
        //The title or name properties override the name parameter
        String v = getProperty(PROP_TITLE, (String) null);
        if (v == null) {
            v = getProperty(PROP_NAME, (String) null);
        }
        if (v != null) {
            this.name = v;
        } else {
            this.name = name;
        }
        //Create the data selection. The 'true' says to set any defaults
        theDataSelection = new DataSelection(true);
        pollingInfo      = (PollingInfo) getProperty(PROP_POLLINFO);
        if (pollingInfo != null) {
            if (pollingInfo.hasName()) {
                setName(pollingInfo.getName());
            }
        }
    }


    protected boolean canDoView() {
        return false;
    }

    /**
     * Check to see if there is a viewfile defined. If so load it in.
     */
    protected void loadView() {
        String viewFile = (String) getProperty("idv.data.viewfile");
        if (viewFile != null) {
            try {
                String xml = IOUtil.readContents(viewFile, getClass(),
                                 (String) null);
                if (xml == null) {
                    return;
                }
                applyView(XmlUtil.getRoot(xml));
            } catch (Exception exc) {
                throw new WrapperException(exc);
            }
        }
    }


    /**
     * Load any parameter nodes in the given view xml
     *
     * @param root xml root
     */
    protected void applyView(Element root) {
        paramsToShow = new ArrayList();
        NodeList children = XmlUtil.getElements(root, "parameter");
        for (int j = 0; j < children.getLength(); j++) {
            Element child = (Element) children.item(j);
            paramsToShow.add(XmlUtil.getAttribute(child, "name"));
        }
    }


    /**
     * _more_
     */
    public void writeViewFile() {
        String filename = FileManager.getWriteFile(FileManager.FILTER_XML,
                              FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
        }

        String datasourceFilename = IOUtil.stripExtension(filename) + "datasource.xml";
        JCheckBox writeDataSourceXmlCbx = new JCheckBox("Write Datasource XML File:",true);
        JTextField dataSourceFileFld = new JTextField(datasourceFilename);
        JTextField labelFld  =new JTextField("");
        String id = IOUtil.getFileTail(IOUtil.stripExtension(filename));
        JTextField idFld  =new JTextField(id);
        List      choices            = getDataChoices();
        List      checkboxes         = new ArrayList();
        List      categories         = new ArrayList();
        Hashtable catMap             = new Hashtable();
        Hashtable currentDataChoices = new Hashtable();

        List      displays = getDataContext().getIdv().getDisplayControls();
        for (int i = 0; i < displays.size(); i++) {
            List dataChoices =
                ((DisplayControl) displays.get(i)).getDataChoices();
            if (dataChoices == null) {
                continue;
            }
            List finalOnes = new ArrayList();
            for (int j = 0; j < dataChoices.size(); j++) {
                ((DataChoice) dataChoices.get(j)).getFinalDataChoices(
                    finalOnes);
            }
            for (int dcIdx = 0; dcIdx < finalOnes.size(); dcIdx++) {
                DataChoice dc = (DataChoice) finalOnes.get(dcIdx);
                if ( !(dc instanceof DirectDataChoice)) {
                    continue;
                }
                DirectDataChoice ddc = (DirectDataChoice) dc;
                if (ddc.getDataSource() != this) {
                    continue;
                }
                currentDataChoices.put(ddc.getName(), "");
            }
        }



        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            String label = dataChoice.getDescription();
            if (label.length() > 30) {
                label = label.substring(0, 29) + "...";
            }
            JCheckBox cbx =
                new JCheckBox(label,
                              currentDataChoices.get(dataChoice.getName())
                              != null);
            cbx.setToolTipText(dataChoice.getName());
            checkboxes.add(cbx);
            Object dc    = dataChoice.getDisplayCategory();
            if(dc == null) dc = "";
            List     comps =   (List) catMap.get(dc);
            if (comps == null) {
                comps = new ArrayList();
                catMap.put(dc, comps);
                categories.add(dc);
            }
            comps.add(cbx);
        }

        List catComps = new ArrayList();
        for (int i = 0; i < categories.size(); i++) {
            List comps = (List) catMap.get(categories.get(i));
            JPanel innerPanel = GuiUtils.doLayout(comps, 1, GuiUtils.WT_NYN,
                                    GuiUtils.WT_N);
            JScrollPane sp = new JScrollPane(GuiUtils.top(innerPanel));
            sp.setPreferredSize(new Dimension(300, 400));
            JPanel top =
                GuiUtils.left(new JLabel(categories.get(i).toString()));
            catComps.add(GuiUtils.inset(GuiUtils.topCenter(top, sp), 5));
        }


        JComponent contents = GuiUtils.hbox(catComps);
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JComponent top = GuiUtils.doLayout(new Component[]{
            writeDataSourceXmlCbx,
            GuiUtils.centerRight(dataSourceFileFld,GuiUtils.makeFileBrowseButton(dataSourceFileFld)),
            GuiUtils.rLabel("ID:"), 
            idFld,
            GuiUtils.rLabel("Label:"), 
            labelFld,
            new JLabel("Select the fields to write"), 
            GuiUtils.filler()},2,GuiUtils.WT_NY,
                                           GuiUtils.WT_N);
        top = GuiUtils.inset(top,5);
        contents = GuiUtils.topCenter(top,contents);
        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "Data Source View File", contents, null)) {
            return;
        }

        try {
            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement("view");

            for (int i = 0; i < dataChoices.size(); i++) {
                DataChoice dataChoice = (DataChoice) dataChoices.get(i);
                if ( !(dataChoice instanceof DirectDataChoice)) {
                    continue;
                }
                String label = dataChoice.getDescription();
                if (label.length() > 30) {
                    label = label.substring(0, 29) + "...";
                }
                JCheckBox cbx = (JCheckBox) checkboxes.get(i);
                if ( !cbx.isSelected()) {
                    continue;
                }
                Element child = doc.createElement("parameter");
                child.setAttribute("name", dataChoice.getName());
                root.appendChild(child);
            }
            writeViewFile(doc, root);
            IOUtil.writeFile(filename, XmlUtil.toString(root));
            if(writeDataSourceXmlCbx.isSelected()) {
                Document dsdoc = XmlUtil.makeDocument();
                Element dsroot = doc.createElement("datasources");
                Element dsnode  = doc.createElement("datasource");
                Element propnode  = doc.createElement("property");
                dsnode.setAttribute("id", idFld.getText());
                dsnode.setAttribute("fileselection", "true");
                dsnode.setAttribute("factory", getClass().getName());
                dsnode.setAttribute("label", labelFld.getText());
                propnode.setAttribute("name", "idv.data.viewfile");
                propnode.setAttribute("value",  filename);
                dsroot.appendChild(dsnode);
                dsnode.appendChild(propnode);
                IOUtil.writeFile(dataSourceFileFld.getText(), XmlUtil.toString(dsroot));
            }

        } catch (Exception exc) {
            logException("Writing view file", exc);
        }

    }

    /**
     * _more_
     *
     * @param doc _more_
     * @param root _more_
     */
    protected void writeViewFile(Document doc, Element root) {}


    /**
     * Should we show the given parameter name
     *
     * @param name parameter name
     *
     * @return should show the parameter as a data choice
     */
    public boolean canShowParameter(String name) {
        if ((paramsToShow == null) || (paramsToShow.size() == 0)) {
            return true;
        }
        DataAlias alias = DataAlias.findAlias(name);
        for (int i = 0; i < paramsToShow.size(); i++) {
            String param = (String) paramsToShow.get(i);
            if (StringUtil.stringMatch(name, param)) {
                return true;
            }
            if (alias != null) {
                if (StringUtil.stringMatch(alias.getName(), param)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     *
     * @param properties Specification of the name=value properties.
     */
    public void setObjectProperties(Hashtable properties) {
        if (properties == null) {
            return;
        }
        boolean didone = false;
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) properties.get(key);
            setObjectProperty(key, value);
            didone = true;
        }
        if (didone) {
            getDataContext().dataSourceChanged(this);
        }
    }

    /**
     * Set the property on this object. Use reflection to find the appropriate
     * set method
     *
     * @param name property name
     * @param value value
     */
    public void setObjectProperty(String name, String value) {
        try {
            ucar.visad.Util.propertySet(this, name, value, false);
        } catch (Exception exc) {
            logException("setProperty:" + name + " value= " + value, exc);
        }

    }




    /**
     * This is called by the composite data choice to update the list of children. The default is to just return the given list but derived classes might do something different.
     *
     * @param compositeDataChoice The composite
     *
     * @param dataChoices List of children choices
     *
     * @return The dataChoices list
     */
    public List getCompositeDataChoices(
            CompositeDataChoice compositeDataChoice, List dataChoices) {
        return dataChoices;
    }

    /**
     * Implement the XmlPersistable  interface method that is called after
     * this object has been fully created and initialized after being
     * unpersisted by the XmlEncoder.
     */
    public void initAfterUnpersistence() {
        initAfterUnpersistenceBeenCalled = true;
        if (getPollingInfo().getIsActive()) {
            startPolling();
        }
        loadView();
        //TODO: Let's not change anything for now
        //      descriptor = getDataContext().getIdv().getDataManager().getCurrent(descriptor);
    }


    /**
     * Gets called after creation. Initialize the polling.
     */
    public void initAfterCreation() {
        if (getPollingInfo().getIsActive()) {
            startPolling();
        }
        loadView();
        //For now we don't share        initSharable();
        //Don't do this for now. 
    }


    /**
     * Used for when we dynamically change the data source files form a bundle
     * or from image generation
     *
     * @param files List of new files to use
     */
    public void setNewFiles(List files) {}


    /**
     * Create the XML element for persisting this DataSource
     *
     * @param encoder  encoder to use
     * @return  this as XML
     */
    public Element createElement(XmlEncoder encoder) {
        return encoder.createElementForObject(this, getClass());
    }

    /**
     * Initialize this from XML
     *
     * @param encoder   encoder for XML
     * @param element   the XML representation
     * @return  true
     */
    public boolean initFromXml(XmlEncoder encoder, Element element) {
        return true;
    }





    /**
     * Get the associated properties
     *
     * @return  the properties
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Get the associated properties
     *
     * @param p  the new properties
     */
    public void setProperties(Hashtable p) {
        properties = p;
    }

    /**
     * Get a property.
     *
     * @param name  name of property
     *
     * @return  the associated property value or <code>null</code>
     */
    public Object getProperty(String name) {
        Object value = null;
        if (properties != null) {
            value = properties.get(name);
        }
        if ((value == null) && (descriptor != null)) {
            //TODO: Let's not change anything for now
            //      value = descriptor.getProperty(name);
        }

        return value;
    }

    /**
     * Set a string property.
     *
     * @param prop  property name
     * @param value property value
     */
    public void setProperty(String prop, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(prop, value);
    }

    /**
     * Get the named String property.
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public String getProperty(String name, String dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return o.toString();
    }


    /**
     * Get the named long property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public long getProperty(String name, long dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return new Long(o.toString()).longValue();
    }

    /**
     * Get the named int property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public int getProperty(String name, int dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return new Integer(o.toString()).intValue();
    }

    /**
     * Get the named double property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public double getProperty(String name, double dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return Misc.parseDouble(o.toString());
    }



    /**
     * Get the object property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public Object getProperty(String name, Object dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return o;
    }

    /**
     * Get the named boolean property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public boolean getProperty(String name, boolean dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return new Boolean(o.toString()).booleanValue();
    }


    /**
     * Set a boolean property.
     *
     * @param prop  name of property
     * @param value value of property
     */
    public void setProperty(String prop, boolean value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(prop, new Boolean(value));
    }


    /**
     * Should we show the error to the user. Normally this is true
     * but some data sources handle their own errors. By setting this to
     * false the error message won't show up twice.
     *
     * @return Should show error
     */
    public boolean getNeedToShowErrorToUser() {
        return needToShowErrorToUser;
    }



    /**
     * Return whether this DataSource is in error
     *
     * @return  true if in error
     */
    public boolean getInError() {
        return inError;
    }



    /**
     * Set whether this DataSource is in error
     *
     * @param e     true for error
     * @param msg   error message
     */
    protected void setInError(boolean e, String msg) {
        setInError(e, true, msg);
    }


    /**
     * Set the inError
     *
     * @param inError Is in error
     * @param needToShowErrorToUser SHould show to user
     * @param msg Any message
     */
    protected void setInError(boolean inError, boolean needToShowErrorToUser,
                              String msg) {
        this.needToShowErrorToUser = needToShowErrorToUser;
        this.inError               = inError;
        errorMessage               = msg;
    }


    /**
     * Set whether this DataSource is in error
     *
     * @param e     true for error
     */
    protected void setInError(boolean e) {
        inError = e;
    }

    /**
     * Get the error message (if there is one) or create one.
     *
     * @return  the error message
     */
    public String getErrorMessage() {
        if (errorMessage == null) {
            return "Data load failed for " + getClass().getName();
        }
        return errorMessage;
    }


    /**
     * This is called when the CacheManager detects the need ot clear memory.
     * It is intended to be overwritten by derived classes that are holding cached
     * data that is not in the normal putCache facilities provided by this class
     * since that data is actually managed by the CacheManager
     */
    public void clearCachedData() {}



    /**
     * Flush the data cache for this DataSource
     */
    protected void flushCache() {
        // System.out.println("flushing cache");
        CacheManager.remove(dataCacheKey);
    }

    /**
     * Put an object in the cache.
     *
     * @param key     cache key
     * @param value   associated key value
     */
    public void putCache(Object key, Object value) {
        CacheManager.put(dataCacheKey, key, value);
    }

    /**
     * Get an Object from the cache.
     *
     * @param key   key for the object
     * @return  the key value, or <code>null</code>
     */
    public Object getCache(Object key) {
        return CacheManager.get(dataCacheKey, key);
    }

    /**
     * Remove an Object from the cache.
     *
     * @param key   key for the object
     */
    public void removeCache(Object key) {
        CacheManager.remove(dataCacheKey, key);
    }


    /**
     * See if this DataSource should cache or not
     *
     * @param data   Data to cache
     * @return  true
     */
    protected boolean shouldCache(Data data) {
        return true;
    }


    /**
     * Gets called by the {@link DataManager} when this DataSource has
     * been removed.
     */
    public void doRemove() {
        if (propertiesDialog != null) {
            propertiesDialog.dispose();
        }
        try {
            flushCache();
        } catch (Exception noop) {}
        try {
            removeSharable();
        } catch (Exception noop) {}
        try {
            stopPolling();
        } catch (Exception noop) {}
        clearFileCache();
        dataChangeListeners = null;
        dataChoices         = null;
    }



    /**
     * Load the latest file.
     *
     * @param file file to load
     *
     * @return name of file loaded.
     */
    protected File loadLatestFile(File file) {
        if (file.isDirectory()) {
            File mostRecent = IOUtil.getMostRecentFile(file,
                                  descriptor.getFilePatternFilter());
            if (mostRecent == null) {
                throw new IllegalArgumentException("No file in directory:"
                        + file);
            }
            //            setName(mostRecent.getPath());
            return mostRecent;
        }
        return file;
    }


    /**
     * Get the most recent file in a direcdtory
     *
     * @param file   directory
     * @return  the most recent file in the directory
     * @deprecated Use loadLatestFile
     */
    protected File doDirectory(File file) {
        return loadLatestFile(file);
    }







    /**
     * Poll the file or directory.
     */
    private void startPolling() {
        Trace.msg("DataSourceImpl.startPolling");
        if ((pollers != null) && (pollers.size() > 0)) {
            Trace.msg("DataSourceImpl.startPolling - Already polling");
            return;
        }
        getPollingInfo().setIsActive(true);
        Poller poller = null;
        if (pollingInfo.getForFiles()) {
            poller = new FilePoller(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    List files = (List) e.getSource();
                    newFilesFromPolling(files);
                }
            }, pollingInfo);
        } else {
            poller = new Poller(pollingInfo.getInterval()) {
                protected void doPoll() {
                    Trace.msg("DataSourceImpl: calling reload");
                    reloadData();
                }
            };
            poller.init();
        }
        if (pollers == null) {
            pollers = new ArrayList();
        }
        pollers.add(poller);
    }


    /**
     * Stop polling
     */
    private void stopPolling() {
        getPollingInfo().setIsActive(false);
        if (pollers != null) {
            for (int i = 0; i < pollers.size(); i++) {
                ((Poller) pollers.get(i)).stopRunning();
            }
            pollers = null;
        }
    }


    /**
     * This gets called by the FilePoller ActionListener to notify
     * DataSource-s of a change to the file system.
     *
     * @param files List of files
     */
    public void newFilesFromPolling(List files) {
        if (files.size() > 0) {
            newFileFromPolling((File) files.get(0));
        }
    }

    /**
     * This gets called by the FilePoller ActionListener to notify
     * DataSource-s of a change to the file system.
     *
     * @param f   new file
     */
    public void newFileFromPolling(File f) {}

    /**
     * Return a String representation of this DataSource
     *
     * @return  string represenation
     */
    public String toString() {
        if (true) {
            return name;
        }
        String v = getProperty(PROP_TITLE, (String) null);
        if (v == null) {
            v = getProperty(PROP_NAME, (String) null);
        }
        if (v == null) {
            v = name;
        }
        return v;
    }

    /**
     * Return a truncated string for the name if too long
     *
     * @return toString truncated to 30 chars
     */
    public String toStringTruncated() {
        String s = toString();
        if (s.length() > 30) {
            s = s.substring(0, 29) + "...";
        }
        return s;
    }

    /** count static */
    static int cnt = 0;

    /** incremented count */
    int mycnt = (cnt++);

    /**
     * Get a unique id for this DataSource
     *
     * @return   a unique id
     */
    public String getid() {
        return getClass().getName() + ": " + toString() + " #" + (mycnt)
               + " ";
    }



    /**
     * Set the {@link DataSourceDescriptor} for this DataSource.
     *
     * @param d   new descriptor
     */
    public void setDescriptor(DataSourceDescriptor d) {
        descriptor = d;
    }

    /**
     * Get the {@link DataSourceDescriptor} for this DataSource.
     *
     * @return  the descriptor
     */
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }


    /**
     * Get the data change listeners
     *
     * @return Data change listeners
     */
    protected List getDataChangeListeners() {
        return dataChangeListeners;
    }

    /**
     * Add the data change listener
     *
     * @param listener    listener to add
     */
    public void addDataChangeListener(DataChangeListener listener) {
        if (dataChangeListeners == null) {
            return;
        }
        if ( !dataChangeListeners.contains(listener)) {
            dataChangeListeners.add(listener);
        }
    }

    /**
     * Remove the data change listener
     *
     * @param listener   listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        if (dataChangeListeners != null) {
            dataChangeListeners.remove(listener);
        }
    }

    /**
     * Flush the cache and tell listeners we have changed.
     * Derived classes should overwrite this method and clear out any state
     * they may be holding. Then they should call this method to do the notification.
     */
    public void reloadData() {
        dataCachePath = null;
        timesList     = null;
        flushCache();
        notifyDataChange();
        getDataContext().dataSourceChanged(this);
    }


    /**
     * Clear the cache
     */
    protected void clearFileCache() {
        if (dataCachePath != null) {
            IOUtil.deleteDirectory(new File(dataCachePath));
        }
    }




    /**
     *  Notify all {@link DataChangeListener}s of some change to the data
     */
    public void notifyDataChange() {
        if (dataChangeListeners == null) {
            return;
        }
        //        System.err.println ("dataChangeListeners:" + dataChangeListeners);
        for (int i = 0; i < dataChangeListeners.size(); i++) {
            ((DataChangeListener) dataChangeListeners.get(i)).dataChanged();
        }
    }

    /**
     * Get the {@link DataContext} for this DataSource.
     *
     * @return  the DataContext or <code>null</code>.
     */
    public DataContext getDataContext() {
        return ((descriptor != null)
                ? descriptor.getDataContext()
                : null);
    }



    /**
     * This is the method defined for the DataSourceFactory interface.
     * Just return ourself.
     *
     * @return  this
     */
    public DataSource getDataSource() {
        return this;
    }


    /*  Why isn't this implemented?
     *
     *  public int hashCode () {
     *  return name.hashCode ();
     *  }
     */

    /**
     * See if the Object in question is equal to this DataSource.
     *
     * @param o   Object in question
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(getClass().equals(o.getClass()))) {
            return false;
        }
        DataSourceImpl that = (DataSourceImpl) o;
        return Misc.equals(name, that.name)
               && Misc.equals(properties, that.properties);
    }

    /**
     * Is this datasource identified by the given label.
     * The name may be of the form "class:classpattern" or just a pattern
     * to match the name by
     *
     * @param name the name.
     * @return  true if it is
     */
    public boolean identifiedByName(String name) {
        if (name.length() == 0) {
            return true;
        }
        if (name.startsWith("class:")) {
            if (StringUtil.stringMatch(getClass().getName(),
                                       name.substring(6), true, true)) {
                return true;
            }
        }
        if (StringUtil.stringMatch(getName(), name, true, true)) {
            return true;
        }
        return false;

    }



    /**
     * See if this DataSource is identified by the definingObject.
     *
     * @param definingObject   definingObject to check
     *
     * @return  true if this is defined by <code>definingObject</code>
     */
    public boolean identifiedBy(Object definingObject) {
        if ((definingObject instanceof String) && (alias != null)
                && (alias.length() > 0)) {
            if (alias.equals(definingObject.toString())) {
                return true;
            }
        }
        if (Misc.equals(definingObject, getUniqueId())) {
            return true;
        }

        /**
         *   Comment this out for now. The name is not unique.
         * if (Misc.equals(definingObject, name)) {
         *   return true;
         * }
         * if (Misc.equals(definingObject, name.toLowerCase())) {
         *   return true;
         * }
         * if (Misc.equals(definingObject, toString())) {
         *   return true;
         * }
         * if (Misc.equals(definingObject, toString().toLowerCase())) {
         *   return true;
         * }
         */
        return false;
    }

    /**
     * Get the type name for this DataSource
     *
     * @return  the type name  or <code>null</code>
     */
    public final String getTypeName() {
        return ((descriptor != null)
                ? descriptor.getId()
                : null);
    }


    /**
     * Automatically create the given display on initialization. This used to be in the IDV
     * but we moved it here to allow different data sources to do different things.
     *
     * @param displayType The display control type id.
     * @param dataContext Really, the IDV
     */
    public void createAutoDisplay(String displayType,
                                  DataContext dataContext) {
        if (displayType != null) {
            List choices = getDataChoices();
            for (int cIdx = 0; cIdx < choices.size(); cIdx++) {
                DataChoice dataChoice = (DataChoice) choices.get(cIdx);
                dataContext.getIdv().doMakeControl(
                    dataChoice,
                    dataContext.getIdv().getControlDescriptor(displayType),
                    (String) null);
                //For now break after the first one
                //so we don't keep adding displays
                break;
            }
        }
    }


    /**
     * Sets the global id of the given dataChoice to be a per process
     * unique string value. Used (for now) for persistence.
     *
     * @param dataChoice     dataChoice to initialize
     */
    public void initDataChoice(DataChoice dataChoice) {
        Hashtable displayProperties =
            (Hashtable) getProperty(PROP_DISPLAYPROPERTIES);
        if (displayProperties != null) {
            dataChoice.setProperties(displayProperties);
        }
        if (dataChoice instanceof CompositeDataChoice) {
            List children =
                ((CompositeDataChoice) dataChoice).getDataChoices();
            for (int i = 0; i < children.size(); i++) {
                initDataChoice((DataChoice) children.get(i));
            }
        }
    }


    /**
     * Search through the list of DataChoice-s and return the
     * DataChoice object whose id equals the given id parameter.
     * If the id based search is unsuccessful then check using the
     * name of the DataChoice.
     *
     * @param id    id of DataChoice
     *
     * @return  the DataChoice which has id or <code>null</code>
     */
    public DataChoice findDataChoice(Object id) {
        String asString = id.toString();
        List   choices  = getDataChoices();
        if (asString.startsWith("#")) {
            try {
                int index = new Integer(asString.substring(1)).intValue();
                if ((index < choices.size()) && (index >= 0)) {
                    return (DataChoice) choices.get(index);
                }
            } catch (NumberFormatException nfe) {}
        }

        //First check if the choice.id equals the given id
        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            if (choice.getId().equals(id)) {
                return choice.cloneMe();
            }
        }

        //Now check the toString
        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            if (choice.toString().equals(asString)) {
                return choice;
            }
            if (choice.getName().equals(asString)) {
                return choice;
            }
        }

        //Now check for aliases
        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            String canonical =
                DataAlias.aliasToCanonical(choice.getId().toString());
            if ((canonical != null)
                    && canonical.toLowerCase().equals(
                        asString.toLowerCase())) {
                return choice;
            }
        }


        if (id instanceof String) {
            String sid = id.toString();
            if (sid.startsWith("pattern:")) {
                sid = sid.substring(8);
                for (int i = 0; i < choices.size(); i++) {
                    DataChoice choice = (DataChoice) choices.get(i);
                    if (StringUtil.stringMatch(choice.getDescription(), sid,
                            true, false)) {
                        return choice;
                    }
                }
            }
        }

        return null;
    }






    /**
     * Search through the list of DataChoice-s and return the
     * DataChoice object whose id equals the given id parameter.
     * If the id based search is unsuccessful then check using the
     * name of the DataChoice.
     *
     * @param id    id of DataChoice
     *
     * @return  the DataChoice which has id or <code>null</code>
     */
    public List findDataChoices(String id) {
        List   result   = new ArrayList();
        String asString = id.toString();
        List   choices  = getDataChoices();
        if (asString.startsWith("#")) {
            try {
                int index = new Integer(asString.substring(1)).intValue();
                if ((index < choices.size()) && (index >= 0)) {
                    result.add(choices.get(index));
                    return result;
                }
            } catch (NumberFormatException nfe) {}
        }


        //Now check the toString
        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            String     name   = choice.getName();
            String     desc   = choice.getDescription();
            if (desc.equals(asString)) {
                result.add(choice);
                continue;
            }
            if (name.toLowerCase().equals(asString.toLowerCase())) {
                result.add(choice);
                continue;
            }
            List aliases = DataAlias.getAliasesOf(asString);
            if (alias != null) {
                boolean gotIt = false;
                for (int aliasIdx = 0; !gotIt && (aliasIdx < aliases.size());
                        aliasIdx++) {
                    String alias = (String) aliases.get(aliasIdx);
                    if (alias.equals(name)) {
                        result.add(choice);
                        gotIt = true;
                    }
                }
                if (gotIt) {
                    continue;
                }
            }

            if (StringUtil.stringMatch(desc, asString, false, false)) {
                result.add(choice);
                continue;
            }
            if (StringUtil.stringMatch(name, asString, false, false)) {
                result.add(choice);
                continue;
            }
        }





        return result;
    }





    /**
     * some method to initialize
     */
    protected void checkForInitAfterUnPersistence() {
        //Check if we were saved off after we have been removed
        //The display control can still ahve a link to the data source
        //And initAfterUnpersistence is never called
        if (haveBeenUnPersisted && !initAfterUnpersistenceBeenCalled) {
            initAfterUnpersistence();
        }

    }


    /**
     * This will lazily create the actual list of DataChoice-s
     * with a call to doMakeDataChoices which creates the
     * DataChoice objects concretely defined by this DataSource (e.g., the
     * fields within an netCdf file). Any DerivedDataChoices that are applicable
     * to the initial set of DataChoice-s is added to the list.
     * Then each DataChoice is initialized with a call to initDataChoice.
     *
     * @return  List of DataChoices
     */
    public List getDataChoices() {
        checkForInitAfterUnPersistence();

        synchronized (DATACHOICES_MUTEX) {
            if (dataChoices == null) {
                dataChoices = new ArrayList();
                doMakeDataChoices();
                List derivedList =
                    getDataContext().getIdv().getDerivedDataChoices(this,
                        dataChoices);
                if (derivedList != null) {
                    dataChoices.addAll(derivedList);
                }
                for (int i = 0; i < dataChoices.size(); i++) {
                    initDataChoice((DataChoice) dataChoices.get(i));
                }
                //For backward compatible bundles
                List selectedTimes = getDataSelection().getTimes();
                if (holdsDateTimes(selectedTimes)) {
                    getDataSelection().setTimes(
                        Misc.getIndexList(selectedTimes, getAllDateTimes()));
                }
            }
            return dataChoices;
        }
    }


    /**
     * Get the list of all levels available from this DataSource
     *
     *
     * @param dataChoice The data choice to get levels for
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice) {
        return getAllLevels(dataChoice, null);
    }


    /**
     * Get all of the levels
     *
     * @param dataChoice The data choice to get levels for
     * @param dataSelection data selection
     *
     * @return list of levels.
     */
    public List getAllLevels(DataChoice dataChoice,
                             DataSelection dataSelection) {
        return null;
    }


    /**
     * Clear the times list
     */
    public void clearTimes() {
        timesList = null;
    }


    /**
     * Return an array of DateTimes representing all the times
     * in the DataSource. This lazily creates a list, timesList,
     * using a call  to doMakeDateTimes.
     *
     * @return array of DateTimes  (may be null)
     */
    public List getAllDateTimes() {
        if (timesList == null) {
            timesList = doMakeDateTimes();

        }
        return timesList;
    }


    /**
     * Get the list of selected times.
     *
     * @return  list of selected times
     */
    public List getSelectedDateTimes() {
        return getDateTimeSelection();
    }


    /**
     * Get the selected times for the given DataChoice.
     *
     * @param dataChoice   DataChoice in question
     * @return  List of selected times
     */
    public List getSelectedDateTimes(DataChoice dataChoice) {
        return getSelectedDateTimes();
    }

    /**
     * Get all the times for the given DataChoice
     *
     * @param dataChoice  DataChoice in question
     * @return  List of all times for that choice
     */
    public List getAllDateTimes(DataChoice dataChoice) {
        return getAllDateTimes();
    }


    /**
     * Return the DataSelection for this DataSource.  The DataSelection
     * represents the default  criteria used for refining the getData calls.
     * For example, the user can set the date/times to be used for this
     * DataSource. This list of times is held in the DataSelection member.
     *
     * @return  the DataSelection for this DataSource
     */
    public DataSelection getDataSelection() {
        if (theDataSelection == null) {
            theDataSelection = new DataSelection();
        }
        return theDataSelection;
    }

    /**
     * Set the DataSelection for this DataSource.
     *
     * @param s   new selection
     */
    public void setDataSelection(DataSelection s) {
        theDataSelection = s;
    }


    /**
     * See if the selection list is a set of times or indices
     *
     * @param selectedTimes  list to check
     * @return  true if there are any times
     */
    public static boolean holdsDateTimes(List selectedTimes) {
        if ((selectedTimes != null) && (selectedTimes.size() > 0)
                && (selectedTimes.get(0) instanceof DateTime)) {
            return true;
        }
        return false;
    }

    /**
     * A utility to determine whether the given list holds a set of Integer
     * indices.
     *
     * @param selectedTimes      A list of Integer indices or DateTime.
     * @return Does the given list hold indices.
     */
    public static boolean holdsIndices(List selectedTimes) {
        if ((selectedTimes != null) && (selectedTimes.size() > 0)
                && (selectedTimes.get(0) instanceof Integer)) {
            return true;
        }
        return false;
    }

    /**
     * A utility method that returns a list of times. If selected holds
     * DateTime objects then just return selected. Else selected holds
     * Integer indices into the allTimes list.
     *
     * @param selected   Either a list of DateTime or a list of Integer indices.
     * @param allTimes   The source list of DataTimes that may be indexed by
     *                   selected.
     *
     * @return A list of DateTime-s.
     */
    public static List getDateTimes(List selected, List allTimes) {
        if (holdsIndices(selected)) {
            return Misc.getValuesFromIndices(selected, allTimes);
        }
        return selected;
    }


    /**
     * Get the absolute times.
     *
     * @param selected  list of selected times
     * @return  List of absolute times
     */
    public List getAbsoluteDateTimes(List selected) {
        return getDateTimes(selected, getAllDateTimes());
    }

    /**
     * Set the list of selected times for this data source. This is used
     * for XML persistence.
     *
     * @param selectedTimes   List of selected times
     */
    public void setDateTimeSelection(List selectedTimes) {
        //Check to see if we need to convert the absolute times into an index list.
        if (holdsDateTimes(selectedTimes) && (timesList != null)) {
            selectedTimes = Misc.getIndexList(selectedTimes,
                    getAllDateTimes());
        }
        getDataSelection().setTimes(selectedTimes);
    }


    /**
     * Return the list of times held by the DataSelection member.
     *
     * @return  DataSelection times
     */
    public List getDateTimeSelection() {
        return getDataSelection().getTimes();
    }


    /**
     * If givenDataSelection is non-null and has a non-null
     * times list then return that. Else return the times list
     * from the myDataSelection member variable.
     *
     * @param givenDataSelection   the given DataSelection
     * @param dataChoice           the given DataChoice
     * @return  appropriate list of times
     */
    protected List getTimesFromDataSelection(
            DataSelection givenDataSelection, DataChoice dataChoice) {
        //This should always be non-null because of the new use of the
        //DataSelection.merge method in the initial getData call.
        List times                  = null;
        List allTimesFromDataChoice = dataChoice.getAllDateTimes();

        if ((givenDataSelection != null) && givenDataSelection.hasTimes()) {
            times = givenDataSelection.getTimes();
            if ((times == null) || (times.size() == 0)) {
                times = allTimesFromDataChoice;
            }
            times = getDateTimes(times, allTimesFromDataChoice);
            //            System.err.println ("allTimesFromDataChoice: " + allTimesFromDataChoice);
            //            System.err.println ("times -- " + times);
        } else {
            times = dataChoice.getSelectedDateTimes();
        }
        if (times == null) {
            times = allTimesFromDataChoice;
        }
        //        System.err.println ("returning: " + getDateTimes(times, allTimesFromDataChoice));
        return getDateTimes(times, allTimesFromDataChoice);
    }




    /**
     * Get the data applicable to the DataChoice and selection criteria.
     *
     * @param dataChoice         choice that defines the data
     * @param dataCategory       the data category
     * @param requestProperties  extra request properties
     * @return  the associated data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData(DataChoice dataChoice, DataCategory dataCategory,
                        Hashtable requestProperties)
            throws VisADException, RemoteException {
        return getData(dataChoice, dataCategory, DataSelection.NULL,
                       requestProperties);
    }


    /** flag for continuation */
    protected boolean okToContinue = true;

    /**
     * Show a wait dialog.  Subclasses should implement to use.
     *
     * @param count   wait time
     */
    protected void showWaitDialog(int count) {
        okToContinue = true;
    }

    /**
     * Stub method.  Subclasses should implement.
     */
    protected void tick() {}


    /**
     *  How many get data calls are we currently waiting on
     *
     * @return Outstanding calls
     */
    public static int getOutstandingGetDataCalls() {
        return outstandingGetDataCalls;
    }

    /**
     * Increment the static count of get data calls
     */
    public static void incrOutstandingGetDataCalls() {
        outstandingGetDataCalls++;
    }

    /**
     * Decrement the static count of get data calls
     */
    public static void decrOutstandingGetDataCalls() {
        outstandingGetDataCalls--;
    }

    /**
     * Utility to create the key used when caching
     *
     * @param dataChoice data choice
     * @param dataSelection data selection
     * @param requestProperties properties on request
     *
     * @return The object to cache on
     */
    protected Object createCacheKey(DataChoice dataChoice,
                                    DataSelection dataSelection,
                                    Hashtable requestProperties) {
        return Misc.newList(dataChoice, dataSelection);
    }


    //    boolean first = true;

    /**
     * Get the data applicable to the DataChoice and selection criteria.
     *
     * @param dataChoice         choice that defines the data
     * @param category           the data category
     * @param incomingDataSelection   DataSelection for subsetting
     * @param requestProperties  extra request properties
     * @return  the associated data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public synchronized Data getData(DataChoice dataChoice,
                                     DataCategory category,
                                     DataSelection incomingDataSelection,
                                     Hashtable requestProperties)
            throws VisADException, RemoteException {



        //start up polling if we have not done so already.
        initPolling();


        //Just call this in case it has not been called yet
        //because it can trigger a failure in some of
        //the derived DataSource classes.
        getDataChoices();
        getAllDateTimes();


        DataSelection selection = DataSelection.merge(incomingDataSelection,
                                      getDataSelection());
        List cacheKey = Misc.newList(createCacheKey(dataChoice, selection,
                            requestProperties));

        if (requestProperties != null) {
            Hashtable newProperties = (Hashtable) requestProperties.clone();
            newProperties.remove(DataChoice.PROP_REQUESTER);
            if (newProperties.size() > 0) {
                cacheKey.add(newProperties.toString());
            }
        }

        /**
         * if(true)
         *   throw new IllegalArgumentException("BADNESS");
         * if(!first)
         *   throw new IllegalArgumentException("BADNESS");
         * first = false;
         */


        Data cachedData = (Data) getCache(cacheKey);
        if (cachedData == null) {
            outstandingGetDataCalls++;
            try {
                LogUtil.message("Data: " + toStringTruncated() + ": "
                                + dataChoice);
                //              Trace.startTrace();
                cachedData = getDataInner(dataChoice, category, selection,
                                          requestProperties);
                //              Trace.stopTrace();
                LogUtil.message("");
            } finally {
                outstandingGetDataCalls--;
            }
            if ((cachedData != null) && shouldCache(cachedData)) {
                putCache(cacheKey, cachedData);
            }
        } else {}
        return cachedData;
    }


    /**
     * Have this one around for other, non-unidata, datasource implementations.
     *
     * @param dataChoice     The data choice that identifies the requested data.
     * @param category       The data category of the request.
     * @param dataSelection  Identifies any subsetting of the data.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection)
            throws VisADException, RemoteException {
        return null;
    }


    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        return getDataInner(dataChoice, category, dataSelection);
    }


    /**
     * A unique identifier for DataSource objects.
     *
     * @return  the next unique id
     */
    public int getNextId() {
        return nextId++;
    }


    /**
     * Set the name of this DataSource
     *
     * @param n  new name
     */
    public void setName(String n) {
        this.name = n;
    }


    /**
     * Keep around for legacy bundles
     *
     * @param n   template name
     */
    public void setTemplateName(String n) {}


    /**
     * Returns the human readable name of this DataSource.  This is the second
     * argument of {@link #DataSourceImpl(DataSourceDescriptor, String, String,
     * Hashtable)}.
     *
     * @return          The human-readable specification of this data-source.
     */
    public String getName() {
        return name;
    }


    /**
     * Set the description for this DataSource
     *
     * @param n  the description
     */
    public void setDescription(String n) {
        description = n;
    }


    /**
     * Return the human readable description of this DataSource
     *
     * @return   the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Return the human readable description of this DataSource
     *
     * @return   the description
     */
    public String getPartialDescription() {
        return getDescription();
    }



    /**
     * Get the full description for this data source.  Subclasses should
     * override if they want something other than the default.  This is what
     * gets displayed in the details.
     *
     * @return  the full description of this data source (description + name)
     */
    public String getFullDescription() {
        return getDescription() + "<p>" + getName();
    }


    /**
     * Insert the new DataChoice into the dataChoice list.
     *
     * @param choice   new choice to add
     */
    protected void addDataChoice(DataChoice choice) {
        if (dataChoices == null) {
            dataChoices = new ArrayList();
        }
        dataChoices.add(choice);
    }

    /**
     * Gets called by the DataSelection tree gui when a CompositeDataChoice
     * is first opened. This allows us to incrementally expand these nested
     * data choices.
     *
     * @param cdc the data choice
     */
    public void expandIfNeeded(CompositeDataChoice cdc) {}


    /**
     * Remove the choice
     *
     * @param choice the choice
     */
    protected void removeDataChoice(DataChoice choice) {
        if (dataChoices != null) {
            dataChoices.remove(choice);
        }
    }

    /**
     * Replace the given child with the given chold
     *
     *
     * @param oldDataChoice The old data choice
     * @param newDataChoice The new one
     */
    public void replaceDataChoice(DataChoice oldDataChoice,
                                  DataChoice newDataChoice) {
        int idx = dataChoices.indexOf(oldDataChoice);
        if (idx >= 0) {
            dataChoices.set(idx, newDataChoice);
        } else {
            addDataChoice(newDataChoice);
        }
    }





    /**
     * A stub for the derived classes to overwrite.
     * This is not abstract because there are some derived classes
     * (e.g., ListDataSource) that do not create any DataChoice-s
     */
    protected void doMakeDataChoices() {}

    /**
     * A stub for the derived classes to overwrite.
     * This is not abstract because there are some derived classes
     * (e.g., TextDataSource) that do not create have any times
     *
     * @return  empty list from this class
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }


    /**
     * Shortcut to logging facility for subclasses to use
     *
     * @param msg   error message
     * @param exc   error Exception
     */
    public void logException(String msg, Exception exc) {
        LogUtil.printException(log_, msg, exc);
    }

    /**
     * Log the exception with the file bytes
     *
     * @param msg message
     * @param exc exception
     * @param fileBytes bytes to write to a tmp file. May be null.
     */
    public void logException(String msg, Exception exc, byte[] fileBytes) {
        LogUtil.printException(log_, msg, exc, fileBytes);
    }



    /**
     *  Set the Alias property.
     *
     *  @param value The new value for Alias
     */
    public void setAlias(String value) {
        alias = value;
    }

    /**
     *  Get the Alias property.
     *
     *  @return The Alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get any {@link Action}-s associated with this DataSource.  The actions
     * can be used to create menus, buttons, etc.  Subclasses should implement
     * this method making sure to call super.getActions()
     *
     * @return list of actions
     */
    public List getActions() {
        ArrayList l = new ArrayList();
        addActions(l);
        return l;
    }


    /**
     * Show the properties dialog
     *
     * @return Was ok pressed
     */
    public boolean showPropertiesDialog() {
        return showPropertiesDialog(null);
    }





    /**
     * Show the properties dialog
     *
     * @param initTabName If non null then show this tab
     * @return Was ok pressed
     */
    public boolean showPropertiesDialog(String initTabName) {
        return showPropertiesDialog(initTabName, false);

    }

    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return false;
    }

    /**
     * Save the data source files to local disk
     *
     * @param filePrefix This is the directory path to write the files to with the unique file identifier
     * append to it
     * @param loadId For stopping the load through the JobManager
     * @param changeLinks Should this data source also change its internal data references
     *
     * @return List of the files that were written
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String filePrefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        return null;
    }



    /**
     * Save the data to local disk. If the uniqueFilePath is null this prompts
     * the user for a directory and a file prefix.
     *
     *
     * @param changeLinks Should this data source also change its internal data references
     *
     * @param uniqueFilePath Where to write the files to
     * @return List of the files that were written
     */
    public List saveDataToLocalDisk(boolean changeLinks,
                                    String uniqueFilePath) {
        if ( !canSaveDataToLocalDisk()) {
            return null;
        }
        String prefix;
        if (uniqueFilePath == null) {
            prefix = getLocalDirectory(getName() + " Data Directory",
                                       getDataPrefix());
        } else {
            prefix = uniqueFilePath + "_" + getDataPrefix();
        }
        if (prefix == null) {
            return null;
        }
        Object loadId =
            beginWritingDataToLocalDisk("Copying data from server");
        try {
            List files = saveDataToLocalDisk(prefix, loadId, changeLinks);
            endWritingDataToLocalDisk(loadId);
            return files;
        } catch (Exception ioe) {
            endWritingDataToLocalDisk(loadId);
            logException("Saving data to local disk", ioe);
            return null;
        }
    }



    /**
     * Start the JobManager load dialog
     *
     * @param msg Message to show in dialog
     *
     * @return The JobManager load id
     */
    protected Object beginWritingDataToLocalDisk(String msg) {
        final Object loadId = JobManager.getManager().startLoad(msg, true);
        return loadId;
    }

    /**
     * Stop the JobManager load dialog
     * @param loadId The JobManager load id
     */
    protected void endWritingDataToLocalDisk(Object loadId) {
        JobManager.getManager().stopLoad(loadId);
    }


    /**
     * Get the file prefix to use for when saving data to local disk
     *
     * @return File prefix to use
     */
    protected String getDataPrefix() {
        return StringUtil.replace(
            IOUtil.cleanFileName(
                IOUtil.stripExtension(IOUtil.getFileTail(getName()))), " ",
                    "");
    }


    /**
     * Get the directory to write the localized data files to
     *
     * @param label Label to show user
     * @param prefix File prefix
     *
     * @return Path with the file prefix that the user specified appended
     */
    protected String getLocalDirectory(String label, String prefix) {
        JTextField nameFld = new JTextField(prefix, 10);
        File dir = FileManager.getDirectory(
                       null, label,
                       GuiUtils.top(
                           GuiUtils.inset(
                               GuiUtils.label("Prefix: ", nameFld), 5)));
        if (dir == null) {
            return null;
        }
        return IOUtil.joinDir(dir, nameFld.getText().trim());
    }

    /**
     * Show the dialog
     *
     * @param initTabName What tab should we show. May be null.
     * @param modal Is dialog modal
     *
     * @return success
     */
    public boolean showPropertiesDialog(String initTabName, boolean modal) {
        if (modal || (propertiesDialog == null)) {
            JTabbedPane propertiesTab = new JTabbedPane();
            addPropertiesTabs(propertiesTab);
            if (initTabName != null) {
                for (int i = 0; i < propertiesTab.getTabCount(); i++) {
                    if (initTabName.equals(propertiesTab.getTitleAt(i))) {
                        propertiesTab.setSelectedIndex(i);
                        break;
                    }
                }
            }

            propertiesDialog = GuiUtils.createDialog(null,
                    toString() + " Properties", modal);
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String cmd = ae.getActionCommand();
                    Misc.run(DataSourceImpl.this, "handlePropertiesAction",
                             cmd);
                }
            };
            JComponent buttons = GuiUtils.makeApplyOkCancelButtons(listener);
            if (reloadCbx == null) {
                reloadCbx = new JCheckBox("Reload Displays", false);
            }

            buttons = GuiUtils.wrap(GuiUtils.hbox(buttons, reloadCbx));
            JComponent contents =
                GuiUtils.inset(GuiUtils.centerBottom(propertiesTab, buttons),
                               5);
            propertiesDialog.getContentPane().add(contents);
            propertiesDialog.pack();
        }
        //        propertiesDialog.setVisible(true);
        updateDetailsText();
        propertiesDialog.setVisible(true);
        return true;
    }


    /**
     * The properties changed
     */
    protected void propertiesChanged() {
        getDataContext().dataSourceChanged(DataSourceImpl.this);
        updateDetailsText();
    }

    /**
     * Handle the properties action
     *
     * @param cmd Action
     */
    public void handlePropertiesAction(String cmd) {
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            if (applyProperties()) {
                propertiesChanged();
                if ((reloadCbx != null) && reloadCbx.isSelected()) {
                    Misc.run(this, "reloadData");
                }

            } else {
                return;
            }
        }
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
            if (propertiesDialog != null) {
                propertiesDialog.setVisible(false);
                //                propertiesDialog = null;
            }

        }

    }



    /**
     * Utility to create a header for the properties dialog
     *
     * @param label Header label
     *
     * @return Header
     */
    protected JComponent getPropertiesHeader(String label) {
        JComponent header = GuiUtils.lLabel(label);
        header = GuiUtils.left(GuiUtils.inset(header,
                new Insets(10, 5, 0, 0)));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                Color.gray.darker()));
        header = GuiUtils.inset(header, new Insets(0, 0, 0, 10));
        return header;
    }

    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        nameFld = new JTextField(name, 20);
        comps.add(GuiUtils.rLabel("Name: "));
        comps.add(nameFld);


        aliasFld = new JTextField("", 20);
        if (alias != null) {
            aliasFld.setText(alias);
        }
        //Don't use the alias field now
        //        comps.add(GuiUtils.rLabel("Alias: "));
        //        comps.add(aliasFld);

        if (canPoll()) {
            getPollingInfo();
            comps.add(GuiUtils.filler());
            comps.add(getPropertiesHeader("Polling"));

            if (pollingInfo.getForFiles()) {
                if (pollingInfo.isADirectory()) {
                    pollingInfo.getPropertyComponents(comps, false, true);
                } else {
                    comps.add(GuiUtils.filler());
                    comps.add(GuiUtils.left(pollingInfo.getActiveWidget()));
                    comps.add(GuiUtils.rLabel("Check Every: "));
                    comps.add(
                        GuiUtils.left(
                            GuiUtils.hbox(
                                pollingInfo.getIntervalWidget(),
                                GuiUtils.lLabel(" minutes"))));

                }
            } else {
                comps.add(GuiUtils.rLabel("Automatically Reload: "));
                comps.add(GuiUtils.left(pollingInfo.getActiveWidget()));
                comps.add(GuiUtils.rLabel("Check Every: "));
                comps.add(
                    GuiUtils.left(
                        GuiUtils.hbox(
                            pollingInfo.getIntervalWidget(),
                            GuiUtils.lLabel(" minutes"))));
            }
        }


        if (canCacheDataToDisk()) {
            comps.add(GuiUtils.filler());
            comps.add(getPropertiesHeader("Caching"));
            cacheDataToDiskCbx = new JCheckBox("Always cache to disk",
                    cacheDataToDisk);
            comps.add(GuiUtils.filler());
            comps.add(cacheDataToDiskCbx);

            comps.add(GuiUtils.rLabel("Delay:"));
            cacheClearDelayFld = new JTextField(""
                    + (cacheClearDelay / 1000), 6);
            comps.add(GuiUtils.left(GuiUtils.hbox(cacheClearDelayFld,
                    new JLabel(" seconds"))));
        }
    }


    /**
     * Can this data source cache its
     *
     * @return can cache data to disk
     */
    public boolean canCacheDataToDisk() {
        return false;
    }


    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return _can do geo subsetting
     */
    public boolean canDoGeoSelection() {
        return false;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the stride or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionStride() {
        return true;
    }



    /**
     * Add any extra tabs into the properties tab
     *
     * @param tabbedPane The properties tab
     */

    public void addPropertiesTabs(JTabbedPane tabbedPane) {
        List comps = new ArrayList();
        getPropertiesComponents(comps);
        if (comps.size() > 0) {
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent propertiesPanel = GuiUtils.doLayout(comps, 2,
                                             GuiUtils.WT_NY, GuiUtils.WT_N);
            tabbedPane.add("Properties", GuiUtils.top(propertiesPanel));
        }


        List times = getAllDateTimes();
        if ((times != null) && (times.size() > 0)) {
            dsw = new DataSelectionWidget(getDataContext().getIdv());
            dsw.setTimes(getAllDateTimes(), getDateTimeSelection());
            tabbedPane.add("Times", dsw.getTimesList("Use All"));
        }

        if (canDoGeoSelection()) {
            tabbedPane.add("Spatial Subset",
                           doMakeGeoSubsetPropertiesComponent());


        }




        detailsEditor = new JEditorPane();
        int height = 300;
        int width  = 400;
        detailsEditor.setMinimumSize(new Dimension(width, height));
        detailsEditor.setPreferredSize(new Dimension(width, height));
        detailsEditor.setEditable(false);
        detailsEditor.setContentType("text/html");

        updateDetailsText();
        JScrollPane scroller = GuiUtils.makeScrollPane(detailsEditor, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));
        tabbedPane.add("Details", GuiUtils.inset(scroller, 5));
    }




    /**
     * Make the geo subset properties component
     *
     * @return the component
     */
    protected JComponent doMakeGeoSubsetPropertiesComponent() {
        geoSelectionPanel = doMakeGeoSelectionPanel();
        return geoSelectionPanel;
    }


    /**
     * Update the properties dialog details page
     */
    protected void updateDetailsText() {
        if (detailsEditor == null) {
            return;
        }
        StringBuffer desc = new StringBuffer();
        List         docs = (List) getProperty(PROP_DOCUMENTLINKS);
        if ((docs != null) && (docs.size() > 0)) {
            for (int i = 0; i < docs.size(); i++) {
                String msg = (String) docs.get(i);
                if ( !msg.startsWith("http:")) {
                    desc.append(msg);
                    desc.append("<p>");
                } else {
                    desc.append("<a href=\"" + msg + "\">" + msg + "</a><p>");
                }
            }
        }

        desc.append(getFullDescription());
        detailsEditor.setText(desc.toString());
        GuiUtils.scrollToTop(detailsEditor);

    }




    /**
     * Make the subset component
     * @return The subset properties component
     */
    public GeoSelectionPanel doMakeGeoSelectionPanel() {
        return doMakeGeoSelectionPanel(true, null);
    }


    /**
     * Make the geoselection panel
     *
     * @param forProperties   true if for the properties widget
     *
     * @return the panel
     */
    public GeoSelectionPanel doMakeGeoSelectionPanel(boolean forProperties) {
        return doMakeGeoSelectionPanel(forProperties, null);
    }

    /**
     * Make the geoselection panel
     *
     * @param forProperties   true if for the properties widget
     * @param geoSelection    geoselection to populate the panel
     *
     * @return the panel
     */
    public GeoSelectionPanel doMakeGeoSelectionPanel(boolean forProperties,
            GeoSelection geoSelection) {
        //        boolean enabled = false;
        boolean enabled = true;
        if (geoSelection == null) {
            geoSelection = getDataSelection().getGeoSelection(true);
        } else {
            enabled = true;
        }
        return new GeoSelectionPanel(new GeoSelection(geoSelection),
                                     forProperties, enabled,
                                     canDoGeoSelectionStride(),
                                     canDoGeoSelectionMap(),
                                     getSampleDataProjection(),
                                     getExtraGeoSelectionComponent());
    }


    /**
     * Return the extra component for the geo selection panel.
     * Example: This is ued by the grid data source to show the
     * grid size label
     *
     * @return null_
     */
    protected JComponent getExtraGeoSelectionComponent() {
        return null;
    }

    /**
     * Used for the geo subsetting property gui
     *
     * @return default is null
     */
    protected ProjectionImpl getSampleDataProjection() {
        return null;
    }


    /**
     * Used for the geo subsetting property gui as to whether to
     * show the map selection or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionMap() {
        return true;
    }

    /**
     * Apply properties components
     *
     * @return false if something failed and we need to keep showing the dialog
     */
    public boolean applyProperties() {
        String newName = nameFld.getText().trim();
        setName(newName);
        if (properties != null) {
            properties.put(PROP_TITLE, newName);
            properties.put(PROP_NAME, newName);
        }

        setAlias(aliasFld.getText().trim());
        if (dsw != null) {
            setDateTimeSelection(dsw.getSelectedDateTimes());
            getDataContext().getIdv().getIdvUIManager().dataSourceTimeChanged(
                this);
        }

        if (geoSelectionPanel != null) {
            GeoSelection geoSubset = getDataSelection().getGeoSelection(true);
            if ( !geoSelectionPanel.applyProperties(geoSubset)) {
                return false;
            }
        }


        if (canPoll()) {
            if ( !pollingInfo.applyProperties()) {
                return false;
            }
            boolean restart = pollingInfo.getIsActive();
            stopPolling();
            if (restart) {
                startPolling();
            }
        }

        if (cacheDataToDiskCbx != null) {
            setCacheDataToDisk(cacheDataToDiskCbx.isSelected());
            setCacheClearDelay((long) (1000
                                       * new Double(cacheClearDelayFld
                                           .getText().trim()).doubleValue()));
        }



        return true;
    }




    /**
     * Get any {@link Action}-s associated with this DataSource.  The actions
     * can be used to create menus, buttons, etc.  Subclasses should implement
     * this method making sure to call super.getActions()
     *
     * @param actions List of actions
     */
    protected void addActions(List actions) {
        AbstractAction a = null;
        a = new AbstractAction("Reload Data") {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(new Runnable() {
                    public void run() {
                        Misc.run(DataSourceImpl.this, "reloadData");
                    }
                });
            }
        };
        actions.add(a);

        if (canSaveDataToLocalDisk()) {
            a = new AbstractAction("Make Data Source Local") {
                public void actionPerformed(ActionEvent ae) {
                    Misc.run(new Runnable() {
                        public void run() {
                            try {
                                saveDataToLocalDisk(true, null);
                            } catch (Exception exc) {
                                logException("Writing data to local disk",
                                             exc);
                            }
                        }
                    });
                }
            };
            actions.add(a);
        }

        if(canDoView()) {
            a = new AbstractAction("Write View File") {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(new Runnable() {
                    public void run() {
                        Misc.run(DataSourceImpl.this, "writeViewFile");
                    }
                });
            }
                };
            actions.add(a);
        }



    }




    /**
     * Called after created or unpersisted to strat up polling if need be.
     */
    private void initPolling() {
        if (haveInitedPolling) {
            return;
        }
        haveInitedPolling = true;
        if ( !canPoll()) {
            return;
        }

        if (getPollingInfo().getFilePattern() == null) {
            pollingInfo.setFilePattern(descriptor.getPatterns());
        }
        if (pollingInfo.getIsActive()) {
            startPolling();
        }
    }


    /**
     * Return the file fitler that the polling info uses.
     *
     * @return Polling file filter
     */
    protected FileFilter getFileFilterForPolling() {
        String filePattern = null;
        if (pollingInfo != null) {
            filePattern = pollingInfo.getFilePattern();
        }
        if (filePattern == null) {
            filePattern = getProperty(PROP_FILEPATTERN, (String) null);
        }
        if (filePattern == null) {
            filePattern = descriptor.getPatterns();
        }
        return new PatternFileFilter(filePattern, false, (pollingInfo != null)
                ? pollingInfo.getIsHiddenOk()
                : true);
    }


    /**
     * Get the location, either a file or a directory, that is where we poll on.
     *
     * @return File or dir to poll.
     */
    protected File getLocationForPolling() {
        return null;
    }

    /**
     * Get the locations to use for polling
     *
     * @return locations  for polling
     */
    protected List getLocationsForPolling() {
        File f = getLocationForPolling();
        if (f != null) {
            return Misc.newList(f.toString());
        }
        return null;
    }


    /**
     * See if this data source can poll
     *
     * @return true if can poll
     */
    public boolean canPoll() {
        return true;
    }


    /**
     * Popup the polling properties dialog.
     */
    private void showPollingPropertiesDialog() {}



    /**
     * Are we currently polling.
     *
     * @return Are we polling
     */
    protected boolean isPolling() {
        return getPollingInfo().getIsActive();
    }


    /**
     *  Set the PollingInfo property.
     *
     *  @param value The new value for PollingInfo
     */
    public void setPollingInfo(PollingInfo value) {
        pollingInfo = value;
    }


    /**
     *  Get the PollingInfo property.
     *
     *  @return The PollingInfo
     */
    public PollingInfo getPollingInfo() {
        if (pollingInfo == null) {
            pollingInfo = (PollingInfo) getProperty(PROP_POLLINFO);
        }

        if (pollingInfo == null) {
            pollingInfo = new PollingInfo();
            pollingInfo.setInterval(60 * 1000 * 10);

            //We aren't polling  for new files
            pollingInfo.setDontLookForNewFiles();
            List files = getLocationsForPolling();
            if ((files != null) && (files.size() > 0)) {
                File f = new File(files.get(0).toString());
                if ( !f.exists()) {
                    pollingInfo.setForFiles(false);
                }
            } else {
                pollingInfo.setForFiles(false);
            }
        }

        if (pollingInfo.getForFiles() && !pollingInfo.hasFiles()) {
            List files = getLocationsForPolling();
            if (files != null) {
                pollingInfo.setFilePaths(new ArrayList(files));
            }
        }
        return pollingInfo;
    }

    /**
     * Do we have polling info object
     *
     * @return have polling info object
     */
    protected boolean hasPollingInfo() {
        return pollingInfo != null;
    }

    /**
     * noop. Keep around for legacy bundles.
     *
     * @param n   directory name
     * @deprecated
     */
    public void setDirectory(String n) {}


    /**
     * noop. Keep around for legacy bundles.
     *
     *  @param value The new value for PollLocation
     * @deprecated
     */
    public void setPollLocation(String value) {}




    /**
     * If the given list is of size 1 and it is a directory
     * then find all files in the directory that match
     * the filePatternForPolling and return them.
     * Else just return the list.
     *
     * @param sources List of File-s or String file names
     *
     * @return List of file names or the given list
     */
    protected List convertToFilesIfDirectory(List sources) {
        if (sources.size() == 1) {
            File f = new File(sources.get(0).toString());
            if (f.isDirectory()) {
                File mostRecent = IOUtil.getMostRecentFile(f,
                                      getFileFilterForPolling());
                if (mostRecent != null) {
                    sources = Misc.newList(mostRecent.toString());
                } else {
                    return new ArrayList();
                }
            }
        }
        return sources;
    }


    /**
     * Find the most recent cnt number of files in the given directory
     * that match the fileFIlterForPolling
     *
     * @param dir Directory to look at. If it is not a directory
     * then use its parent.
     * @param cnt Number of files to find.
     *
     * @return List of cnt File-s
     */
    protected List getMostRecentFiles(File dir, int cnt) {
        Trace.call1("mostRecent");
        if ( !dir.isDirectory()) {
            dir = dir.getParentFile();
        }
        FileFilter filter = getFileFilterForPolling();
        Trace.call1("listFiles");
        File[] allFiles = dir.listFiles((java.io.FileFilter) filter);
        Trace.call2("listFiles");

        Trace.msg("#files=" + allFiles.length);

        Trace.call1("sort");
        File[] sorted = IOUtil.sortFilesOnAge(allFiles, true);
        Trace.call2("sort");
        List files = new ArrayList();
        int  total = 0;
        for (int i = 0; (i < sorted.length) && (total < cnt); i++) {
            total++;
            files.add(sorted[i]);
        }
        Trace.call2("mostRecent");
        return files;

    }



    /**
     * Set the AskToUpdate property.
     *
     * @param value The new value for AskToUpdate
     * @deprecated Keep around for bundles
     */
    public void setAskToUpdate(boolean value) {}



    /**
     *  Set the DataIsEditable property.
     *
     *  @param value The new value for DataIsEditable
     */
    public void setDataIsRelative(boolean value) {
        //noop        dataIsEditable = value;
    }



    /**
     *  Set the DataIsEditable property.
     *
     *  @param value The new value for DataIsEditable
     */
    public void setDataIsEditable(boolean value) {
        dataIsEditable = value;
    }


    /**
     *  Get the DataIsEditable property.
     *
     *  @return The DataIsEditable
     */
    public boolean getDataIsEditable() {
        return dataIsEditable;
    }


    /**
     * Get the file paths (or urls or whatever) that are to be changed
     * when we re unpersisted and are in data editable mode
     *
     * @return file paths to changed
     */
    public List getDataPaths() {
        return null;
    }


    /**
     * Set the changed file or url paths
     *
     * @param strings List of paths
     */
    public void setDataEditableStrings(List strings) {}


    /**
     * Used when loading from a bundle with  relative file paths
     *
     * @param strings  Relative file paths
     */
    public void setDataRelativeStrings(List strings) {
        setDataEditableStrings(strings);
    }



    /**
     * Return the paths that can be saved off relative to wehre the bundle is. The default here is
     * to return null.
     *
     * @return File paths that can be relative
     */
    public List getPathsThatCanBeRelative() {
        return null;

    }



    /**
     * Set the RelativePaths property.
     *
     * @param value The new value for RelativePaths
     */
    public void setRelativePaths(List value) {
        relativePaths = value;
    }

    /**
     * Get the RelativePaths property.
     *
     * @return The RelativePaths
     */
    public List getRelativePaths() {
        return relativePaths;
    }


    /**
     * Set the TmpPaths property.
     *
     * @param value The new value for TmpPaths
     */
    public void setTmpPaths(List value) {
        tmpPaths = value;
    }

    /**
     * Get the TmpPaths property.
     *
     * @return The TmpPaths
     */
    public List getTmpPaths() {
        return tmpPaths;
    }


    /**
     * A helper method to find the label to use for the given
     * {@link ucar.unidata.data.DataSource}. If the length of
     * the toString of the data source is less than 30 just use that.
     * Else be a bit smart about truncating it.
     *
     * @param ds The data soruce to get a label for
     * @param length String length to clip to
     * @param alwaysDoIt If false then we only lip if this is a file or url
     * @return The label
     */
    public static String getNameForDataSource(DataSource ds, int length,
            boolean alwaysDoIt) {
        String name = ds.toString();
        if (name.length() < length) {
            return name;
        }
        if ((new File(name)).exists() || name.startsWith("http:")
                || name.startsWith("dods:")) {
            int     index         = name.length() - 1;
            boolean seenSeparator = false;
            while (index >= 0) {
                char c = name.charAt(index);
                if ((c == File.separatorChar) || (c == '/')) {
                    seenSeparator = true;
                    break;
                }


                if (seenSeparator && (name.length() - index >= length)) {
                    break;
                }
                index--;
            }
            if ((index == 0) || (index == -1)) {
                return name;
            }
            return "..." + name.substring(index);
        }

        if ( !alwaysDoIt) {
            return name;
        }
        return name.substring(0, length) + "...";
    }



    /**
     * Set the CacheFlatFields property.
     *
     * @param value The new value for CacheFlatFields
     */
    public void setCacheDataToDisk(boolean value) {
        cacheDataToDisk = value;
    }

    /**
     * Get the CacheFlatFields property.
     *
     * @return The CacheFlatFields
     */
    public boolean getCacheDataToDisk() {
        return cacheDataToDisk;
    }

    /**
     * Where do we write cached data to
     *
     * @return cache path
     */
    public String getDataCachePath() {
        if (getDataContext() == null) {
            return null;
        }
        if (dataCachePath == null) {
            String uniqueName = "data_" + Misc.getUniqueId();
            dataCachePath =
                IOUtil.joinDir(getDataContext().getIdv().getDataManager()
                    .getDataCacheDirectory(), uniqueName);
            IOUtil.makeDir(dataCachePath);
        }
        return dataCachePath;
    }


    /**
     * Set the CacheClearDelay property.
     *
     * @param value The new value for CacheClearDelay
     */
    public void setCacheClearDelay(long value) {
        cacheClearDelay = value;
    }

    /**
     * Get the CacheClearDelay property.
     *
     * @return The CacheClearDelay
     */
    public long getCacheClearDelay() {
        return cacheClearDelay;
    }


}

