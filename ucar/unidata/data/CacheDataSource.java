/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
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


import org.w3c.dom.*;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import visad.Data;
import visad.DataReference;

import visad.VisADException;

import java.awt.event.*;

import java.io.*;

import java.io.File;
import java.io.Serializable;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.util.zip.*;

import javax.swing.*;


/**
 * Used to cache  a data choice and its data
 *
 * @author IDV development team
 * @version $Revision: 1.12 $
 */


public class CacheDataSource extends DataSourceImpl {

    /** Local cache */
    private Hashtable cache = new Hashtable();


    /** cache tmp dir */
    private String tmpDir;


    /** datachoice property */
    public static final String PROP_DATACHOICE = "CacheDataSource.DataChoice";

    /** data property */
    public static final String PROP_DATA = "CacheDataSource.Data";


    /** the data choice we hold */
    private List holders = new ArrayList();


    /** The current set of data choices that we display */
    private List currentChoices;



    /**
     * Default bean constructor; does nothing
     */
    public CacheDataSource() {}

    /**
     * Create a CacheDataSource
     *
     *
     * @param descriptor the datasource descriptor
     * @param name my name
     * @param properties my properties
     */
    public CacheDataSource(DataSourceDescriptor descriptor, String name,
                           Hashtable properties) {
        super(descriptor, "Cached data", "Cached data", properties);
    }


    /**
     * add menu actions
     *
     * @param actions list of actions
     */
    protected void addActions(List actions) {
        super.addActions(actions);
        AbstractAction a = null;
        a = new AbstractAction("Write to Serialized Format") {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(new Runnable() {
                    public void run() {
                        Misc.run(CacheDataSource.this, "writeIser");
                    }
                });
            }
        };
        actions.add(a);
    }


    /**
     * write out the iser file
     */
    public void writeIser() {

        JDialog dialog = null;

        try {
            PatternFileFilter FILTER_ISER =
                new PatternFileFilter("(.+\\.iser$)",
                                      "Serialized Data File (*.iser)",
                                      ".iser");


            StringBuffer sb = new StringBuffer();
            String filename =
                FileManager.getWriteFile(Misc.newList(FILTER_ISER), "iser");
            if (filename == null) {
                return;
            }

            dialog = GuiUtils.createDialog(null, "Progress", false);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("");


            JLabel label =
                new JLabel(
                    "Writing serialized data file                                                  ");
            dialog.getContentPane().add(GuiUtils.inset(GuiUtils.vbox(label,
                    progressBar), 5));
            dialog.pack();
            dialog.setLocation(200, 200);
            dialog.show();


            List holders = writeToCache(progressBar);
            if (holders.size() == 0) {
                LogUtil.userErrorMessage(
                    "None of the cached data has a serialized form");
                dialog.dispose();
                return;
            }

            progressBar.setString(" ");

            if (holders.size() > 1) {
                List cbxs = new ArrayList();
                for (int i = 0; i < holders.size(); i++) {
                    DataChoiceHolder holder =
                        (DataChoiceHolder) holders.get(i);
                    cbxs.add(new JCheckBox(holder.getName(), true));
                }
                JComponent contents =
                    GuiUtils.vbox(
                        new JLabel("What fields should be serialized?"),
                        GuiUtils.vbox(cbxs));
                if ( !GuiUtils.askOkCancel("Serialized Data", contents)) {
                    dialog.dispose();
                    return;
                }
                List goodHolders = new ArrayList();
                for (int i = 0; i < holders.size(); i++) {
                    DataChoiceHolder holder =
                        (DataChoiceHolder) holders.get(i);
                    if (((JCheckBox) cbxs.get(i)).isSelected()) {
                        goodHolders.add(holder);
                    }
                }
                holders = goodHolders;
            }

            if (holders.size() == 0) {
                LogUtil.userErrorMessage(
                    "No fields selected. Not writing file.");
                dialog.dispose();
                return;
            }

            Document doc =
                XmlUtil.getDocument("<serializeddata></serializeddata>");
            Element root  = doc.getDocumentElement();


            int     total = 0;
            ZipOutputStream zos =
                new ZipOutputStream(new FileOutputStream(filename));
            for (int i = 0; i < holders.size(); i++) {
                DataChoiceHolder holder = (DataChoiceHolder) holders.get(i);
                progressBar.setString("Writing:  " + holder.getName());
                File   f    = getCachedDataFile(holder.getId());
                String tail = IOUtil.getFileTail(f.toString());
                zos.putNextEntry(new ZipEntry(tail));
                total += IOUtil.writeTo(new FileInputStream(f), zos, null, 0);
                progressBar.setString("Wrote " + total + " bytes");

                DataChoice dataChoice = holder.getDataChoice();
                List       categories = dataChoice.getCategories();
                String     catstring  = StringUtil.join(";", categories);
                Element dataElement =
                    doc.createElement(SerializedDataSource.TAG_DATA);
                root.appendChild(dataElement);
                dataElement.setAttribute(SerializedDataSource.ATTR_FILE,
                                         tail);
                dataElement.setAttribute(SerializedDataSource.ATTR_NAME,
                                         holder.getName());
                dataElement.setAttribute(
                    SerializedDataSource.ATTR_CATEGORIES, catstring);
                String icon =
                    (String) dataChoice.getProperty(DataChoice.PROP_ICON);
                if (icon != null) {
                    dataElement.setAttribute(SerializedDataSource.ATTR_ICON,
                                             icon);

                }

            }
            byte[] xmlbytes = XmlUtil.toString(root).getBytes();
            zos.putNextEntry(new ZipEntry("data.xser"));
            zos.write(xmlbytes, 0, xmlbytes.length);
            zos.close();


            dialog.dispose();

            JLabel question =
                new JLabel(
                    "<html>The file:<br><i>" + filename
                    + "</i><br>has been written.<p>&nbsp;<br>"
                    + "<b>Note: the data held in this serialized format is not guaranteed to work with future versions of the IDV.</b><p><hr><p>"
                    + "Do you want to load the data into the IDV now?</html>");

            if (GuiUtils.askYesNo("Serialized Data File", question)) {
                getDataContext().getIdv().handleAction(filename, null);
            }

        } catch (Exception iexc) {
            if (dialog != null) {
                dialog.dispose();
            }
            LogUtil.logException(
                "There was an error writing the serialized data", iexc);
        }



    }



    /**
     * Add a data choice into the list with the given name.
     * If data is non-null then cache it
     *
     * @param dataChoice The datachoice
     * @param name The name
     * @param data The data to cache
     */
    public void addDataChoice(DataChoice dataChoice, String name, Data data) {
        addDataChoice(dataChoice, name, data, null);
    }

    /**
     * add the data choice
     *
     * @param dataChoice data choice
     * @param name name
     * @param data the data
     * @param dataSelection data selection
     */
    public void addDataChoice(DataChoice dataChoice, String name, Data data,
                              DataSelection dataSelection) {
        currentChoices = null;
        DataChoiceHolder holder = new DataChoiceHolder(dataChoice, name,
                                      dataSelection);
        holders.add(holder);
        if (data != null) {
            putCache(holder.getId(), data);
        }
        getDataContext().dataSourceChanged(this);
    }


    /**
     * Get the cache key for the given data choice holder
     *
     * @param holder holder
     *
     * @return cache key
     */
    private Object getCacheKey(DataChoiceHolder holder) {
        return holder.getId();

    }




    /**
     * Get the file where we cache the id
     *
     * @param id id
     *
     * @return file
     */
    private File getCachedDataFile(Object id) {
        if (tmpDir == null) {
            return null;
        }
        return new File(IOUtil.joinDir(tmpDir, id.toString()) + ".ser");
    }

    /**
     * clear the cache
     */
    public void clearCachedData() {
        super.clearCachedData();
        writeToCache(null);
        cache = new Hashtable();
    }


    /**
     * Write to cache
     *
     * @param progressBar progress bar
     *
     * @return The data choice holders that we actually were able to write
     */
    private List writeToCache(JProgressBar progressBar) {
        if (tmpDir == null) {
            tmpDir = IOUtil.joinDir(
                getDataContext().getIdv().getStore().getUserTmpDirectory(),
                "datacache_" + Misc.getUniqueId());

            IOUtil.makeDir(tmpDir);
        }

        Hashtable cachedKeys = new Hashtable();

        for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            Object value = cache.get(key);
            File   f     = getCachedDataFile(key);
            if (f.exists()) {
                cachedKeys.put(key, key);
                continue;
            }
            try {
                DataChoiceHolder holder = findHolder(key);
                if (progressBar != null) {
                    progressBar.setString("Serializing data:"
                                          + holder.getName());
                }
                byte[] bytes = Misc.serialize((Serializable) value);
                if (bytes == null) {
                    continue;
                }
                IOUtil.writeBytes(f, bytes);
                cachedKeys.put(key, key);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
            }
        }


        List goodHolders = new ArrayList();
        for (int holderIdx = 0; holderIdx < holders.size(); holderIdx++) {
            DataChoiceHolder holder =
                (DataChoiceHolder) holders.get(holderIdx);
            if (cachedKeys.get(holder.getId()) != null) {
                goodHolders.add(holder);
            }
        }

        return goodHolders;
    }


    /**
     * Find the data choice holder with the given cache key
     *
     * @param key cache key
     *
     * @return holder
     */
    private DataChoiceHolder findHolder(Object key) {
        for (int holderIdx = 0; holderIdx < holders.size(); holderIdx++) {
            DataChoiceHolder holder =
                (DataChoiceHolder) holders.get(holderIdx);
            if (holder.getId().equals(key)) {
                return holder;
            }
        }
        return null;
    }


    /**
     * Put the cache
     *
     * @param key key
     * @param value value
     */
    public void putCache(Object key, Object value) {
        cache.put(key, value);
    }

    /**
     * Get an Object from the cache.
     *
     * @param key   key for the object
     * @return  the key value, or <code>null</code>
     */
    public Object getCache(Object key) {
        return cache.get(key);
    }

    /**
     * Remove an Object from the cache.
     *
     * @param key   key for the object
     */
    public void removeCache(Object key) {
        cache.remove(key);
    }




    /**
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        Object           id     = dataChoice.getId();
        DataChoiceHolder holder = null;
        for (int holderIdx = 0; holderIdx < holders.size(); holderIdx++) {
            DataChoiceHolder tmpHolder =
                (DataChoiceHolder) holders.get(holderIdx);
            if (tmpHolder.getId().equals(dataChoice.getId())) {
                holder = tmpHolder;
                break;
            }
        }
        if (holder == null) {
            return null;
        }
        DataChoice theDataChoice = holder.getDataChoice();
        Hashtable newRequestProperties =
            DataChoice.mergeRequestProperties(requestProperties,
                theDataChoice.getFixedRequestProperties());

        Object cacheKey = holder.getId();
        Data   data     = (Data) getCache(cacheKey);
        if (data == null) {
            File f = getCachedDataFile(cacheKey);
            if ((f != null) && f.exists()) {
                try {
                    data = (Data) Misc.deserialize(
                        IOUtil.readBytes(
                            IOUtil.getInputStream(f.toString(), getClass())));
                } catch (Exception exc) {
                    System.err.println("Error reading cached data:" + exc);
                }
            }
        }


        if (data == null) {
            //            System.err.println ("data was *not*  in cache");
            data = theDataChoice.getData(holder.getDataSelection(),
                                         requestProperties);
            putCache(cacheKey, data);
        } else {
            //            System.err.println ("data was in cache");
        }
        return data;
    }



    /**
     * Get the DataChoices.
     *
     * @return  List of DataChoices.
     */
    public List getDataChoices() {
        if (currentChoices == null) {
            currentChoices = new ArrayList();

            for (int holderIdx = 0; holderIdx < holders.size(); holderIdx++) {
                DataChoiceHolder holder =
                    (DataChoiceHolder) holders.get(holderIdx);
                DataChoice dataChoice    = holder.getDataChoice();
                List       categories    = new ArrayList();
                List       tmpCategories = dataChoice.getCategories();
                if (tmpCategories != null) {
                    for (int catIdx = 0; catIdx < tmpCategories.size();
                            catIdx++) {
                        DataCategory dc =
                            (DataCategory) tmpCategories.get(catIdx);
                        if (dc.getForDisplay()) {
                            continue;
                        }
                        categories.add(dc);
                    }
                }

                Hashtable properties = null;
                if (dataChoice.getProperties() != null) {
                    properties = new Hashtable(dataChoice.getProperties());
                }

                DirectDataChoice choice = new DirectDataChoice(this,
                                              holder.getId(),
                                              holder.getName(),
                                              holder.getName(), categories,
                                              properties);
                choice.setFixedRequestProperties(
                    dataChoice.getFixedRequestProperties());
                currentChoices.add(choice);
            }
        }
        return currentChoices;
    }



    /**
     * Set the Holders property.
     *
     * @param value The new value for Holders
     */
    public void setHolders(List value) {
        holders = value;
    }

    /**
     * Get the Holders property.
     *
     * @return The Holders
     */
    public List getHolders() {
        return holders;
    }





    /**
     * Class DataChoiceHolder holds a data chocie and a unique id and a name
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.12 $
     */
    public static class DataChoiceHolder {

        /** The id */
        Object id;

        /** The data choice */
        DataChoice dataChoice;

        /** The name */
        String name;

        /** data selection */
        DataSelection dataSelection;

        /**
         * ctor
         */
        public DataChoiceHolder() {}

        /**
         * ctor
         *
         * @param dataChoice the data choice
         * @param name its name
         * @param dataSelection data selection
         */
        public DataChoiceHolder(DataChoice dataChoice, String name,
                                DataSelection dataSelection) {
            this.id            = Misc.getUniqueId();
            this.dataChoice    = dataChoice;
            this.name          = name;
            this.dataSelection = dataSelection;
        }

        /**
         *  Set the Id property.
         *
         *  @param value The new value for Id
         */
        public void setId(Object value) {
            id = value;
        }

        /**
         *  Get the Id property.
         *
         *  @return The Id
         */
        public Object getId() {
            return id;
        }

        /**
         *  Set the DataChoice property.
         *
         *  @param value The new value for DataChoice
         */
        public void setDataChoice(DataChoice value) {
            dataChoice = value;
        }

        /**
         *  Get the DataChoice property.
         *
         *  @return The DataChoice
         */
        public DataChoice getDataChoice() {
            return dataChoice;
        }

        /**
         * Set the Name property.
         *
         * @param value The new value for Name
         */
        public void setName(String value) {
            name = value;
        }

        /**
         * Get the Name property.
         *
         * @return The Name
         */
        public String getName() {
            return name;
        }

        /**
         *  Set the DataSelection property.
         *
         *  @param value The new value for DataSelection
         */
        public void setDataSelection(DataSelection value) {
            dataSelection = value;
        }

        /**
         *  Get the DataSelection property.
         *
         *  @return The DataSelection
         */
        public DataSelection getDataSelection() {
            return dataSelection;
        }




    }




}

