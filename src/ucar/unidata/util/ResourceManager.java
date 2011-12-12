/*
 * $Id: ResourceManager.java,v 1.37 2007/08/10 14:27:02 jeffmc Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;





import java.beans.*;

import java.beans.*;




import java.io.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.filechooser.FileFilter;


/**
 * A class to manage a collection of system and user color tables
 *
 * @author Metapps Development Team
 * @version $Id: ResourceManager.java,v 1.37 2007/08/10 14:27:02 jeffmc Exp $
 */
public abstract class ResourceManager {

    /** _more_ */
    public static final String PROP_RESOURCECHANGE = "prop.resourcechange";

    /** _more_ */
    public static final String PROP_RESOURCEREMOVE = "prop.resourceremove";

    /** _more_ */
    public static final LogUtil LU = null;

    /** _more_ */
    public static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ResourceManager.class.getName());

    /** _more_ */
    protected Hashtable nameToObject = new Hashtable();

    /** _more_ */
    protected List usersList = new ArrayList();

    /** _more_ */
    protected String writablePath;

    /** _more_ */
    protected List listOfResourceLists = new ArrayList();

    /** _more_ */
    protected List resources = new ArrayList();

    /** _more_ */
    private FileManager fileChooser;

    /** _more_          */
    private int resourceTimestamp = 0;


    /** My encoder */
    private XmlEncoder xmlEncoder;

    /**
     * no param ctor
     */
    public ResourceManager() {
    }


    /**
     * ctor
     *
     * @param xmlEncoder The encoder to use
     */
    public ResourceManager(XmlEncoder xmlEncoder) {
        this.xmlEncoder = xmlEncoder;
    }


    /**
     * _more_
     *
     * @param resources
     */
    public void init(ResourceCollection resources) {
        boolean gotUsers = false;
        try {
            for (int i = 0; i < resources.size(); i++) {
                boolean writable = ((writablePath == null)
                                    && resources.isWritableResource(i));
                if (writable) {
                    writablePath = resources.get(i).toString();
                }
                Object object = initResource(resources, i);
                if (object == null) {
                    continue;
                }
                Object newObject = processObject(object);
                if (newObject == null) {
                    continue;
                }
                if (newObject instanceof NamedObject) {
                    newObject = Misc.newList(newObject);
                } else if ( !(newObject instanceof List)) {
                    throw new IllegalArgumentException(
                        "Resource must be a NamedObject or a List:"
                        + newObject.getClass().getName());
                }
                if (writable) {
                    usersList = (List) newObject;
                    gotUsers  = true;
                }
                listOfResourceLists.add(newObject);
            }
        } catch (Throwable exc) {
            System.err.println("Error handling resources:" + resources);
            exc.printStackTrace();
        }
        if ( !gotUsers) {
            listOfResourceLists.add(0, usersList);
        }
        initDone();
    }



    /**
     * _more_
     *
     * @param resources _more_
     * @param index _more_
     *
     * @return _more_
     */
    protected Object initResource(ResourceCollection resources, int index) {
        String xml = resources.read(index);
        if (xml == null) {
            return null;
        }
        return toObject(xml, (String) resources.get(index));
    }



    /**
     * _more_
     * @return _more_
     */
    public NamedObject getDefault() {
        if (resources.size() > 0) {
            return (NamedObject) resources.get(0);
        }
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getResourceTimestamp() {
        return resourceTimestamp;
    }


    /**
     * _more_
     */
    protected void reloadResources() {
        resourceTimestamp++;
        resources    = new ArrayList();
        nameToObject = new Hashtable();
        for (int i = 0; i < listOfResourceLists.size(); i++) {
            List newResources = (List) listOfResourceLists.get(i);
            for (int ctIdx = 0; ctIdx < newResources.size(); ctIdx++) {
                NamedObject ct = (NamedObject) newResources.get(ctIdx);
                if (nameToObject.get(ct.getName()) == null) {
                    nameToObject.put(ct.getName(), ct);
                    resources.add(ct);
                }
            }
        }

    }

    /**
     * _more_
     *
     * @param name
     * @return _more_
     */
    public boolean resourceExists(String name) {
        return (nameToObject.get(name) != null);
    }

    /**
     * _more_
     */
    protected void initDone() {
        reloadResources();
    }

    /**
     * _more_
     *
     * @param o
     * @return _more_
     */
    protected Object processObject(Object o) {
        return o;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getTitle() {
        return "Resource";
    }


    /**
     * _more_
     *
     * @param object
     * @return _more_
     */
    public int getUsersIndex(NamedObject object) {
        for (int i = 0; i < usersList.size(); i++) {
            if (((NamedObject) usersList.get(i)).getName().equals(
                    object.getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * _more_
     *
     * @param name
     * @return _more_
     */
    public NamedObject getObject(String name) {
        return (NamedObject) nameToObject.get(name);
    }

    /**
     * _more_
     * @return _more_
     */
    public List getResources() {
        return resources;
    }


    /**
     * _more_
     *
     * @param object
     * @return _more_
     */
    public boolean isUsers(NamedObject object) {
        return (getUsersIndex(object) >= 0);
    }

    /**
     * _more_
     *
     * @param object
     */
    public void removeUsers(NamedObject object) {
        usersList.remove(object);
        reloadResources();
        writeUsers();
        firePropertyChange(PROP_RESOURCEREMOVE, object, null);
    }

    /**
     * _more_
     *
     * @param object
     */
    public void addUsers(NamedObject object) {
        int index = getUsersIndex(object);
        if (index >= 0) {
            usersList.set(index, object);
        } else {
            usersList.add(object);
        }
        reloadResources();
        writeUsers();
        firePropertyChange(PROP_RESOURCECHANGE, null, object);
    }

    /**
     * create if needed and return the XmlEncoder
     * @return The xml encoder
     */
    protected XmlEncoder getEncoder() {
        if (xmlEncoder == null) {
            xmlEncoder = new XmlEncoder();
        }
        return xmlEncoder;     
    }


    /**
     * _more_
     */
    private void writeUsers() {
        if (writablePath != null) {
            try {
                IOUtil.writeFile(writablePath, getEncoder().toXml(usersList));
            } catch (Exception exc) {
                LU.printException(log_, "Writing resource file", exc);
            }
        }

    }


    /**
     * _more_
     * @return _more_
     */
    public List getWriteFileFilters() {
        return getFileFilters();
    }

    /**
     * _more_
     * @return _more_
     */
    public List getReadFileFilters() {
        return getFileFilters();
    }


    /**
     * _more_
     * @return _more_
     */
    public List getFileFilters() {
        return null;
    }


    /**
     * _more_
     * @return _more_
     */
    public String getFileSuffix() {
        return null;
    }


    /**
     * _more_
     *
     * @param object
     */
    public void doExport(NamedObject object) {
        String file = FileManager.getWriteFile(getTitle() + " export",
                          getWriteFileFilters(), getFileSuffix());
        if (file == null) {
            return;
        }
        doExport(object, file);
    }


    /**
     * _more_
     *
     * @param object
     * @param file
     * @return _more_
     */
    protected String getExportContents(NamedObject object, String file) {
        return (getEncoder()).toXml(object);
    }


    /**
     * _more_
     *
     * @param object
     * @param file
     */
    public void doExport(NamedObject object, String file) {
        String contents = getExportContents(object, file);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(contents.getBytes());
            fos.close();
        } catch (Exception exc) {
            LU.printException(log_, "Exporting resource", exc);
        }
    }

    /**
     * _more_
     *
     * @param o
     * @param forceUnique
     * @return _more_
     */
    public NamedObject doImport(Object o, boolean forceUnique) {
        if (o == null) {
            return null;
        }

        if (o instanceof List) {
            List        list  = (List) o;
            NamedObject first = null;
            for (int i = 0; i < list.size(); i++) {
                NamedObject obj = doImport(list.get(i), forceUnique);
                if (first == null) {
                    first = obj;
                }
            }
            //System.err.println("first:" + first);
            return first;
        }

        if (o instanceof NamedObject) {
            if (forceUnique) {
                NamedObject nobj = (NamedObject) o;
                String      name = nobj.getName();
                int         cnt  = 1;
                //Keep checking if we have the object
                //Change its name everytime
                //Quit at 20 times just in case
                while (resourceExists(nobj.getName()) && (cnt < 100)) {
                    nobj.setName(name + "_" + cnt);
                    cnt++;
                }
            }

            //System.err.println("adding users:" + o);
            addUsers((NamedObject) o);
            return (NamedObject) o;
        }
        return null;
    }


    /**
     * _more_
     * @return _more_
     */
    public NamedObject doImport() {
        return doImport(false);
    }


    /**
     * _more_
     *
     * @param makeUnique
     * @return _more_
     */
    public NamedObject doImport(boolean makeUnique) {
        String file = FileManager.getReadFile(getTitle() + " import",
                          getReadFileFilters());
        if (file == null) {
            return null;
        }
        try {
            String xml = IOUtil.readContents(file, ResourceManager.class);
            if (xml == null) {
                return null;
            }
            Object o = (getEncoder()).toObject(xml);
            return doImport(o, makeUnique);
        } catch (Exception exc) {
            LU.printException(log_, "Error reading file:" + file, exc);
        }
        return null;
    }


    /**
     * _more_
     *
     * @param component
     * @return _more_
     */
    public String doNew(Component component) {
        return doNew(component, "New");
    }

    /**
     * _more_
     *
     * @param component
     * @param label
     * @return _more_
     */
    public String doNew(Component component, String label) {
        return doNew(component, label, "", null);
    }

    /**
     * _more_
     *
     * @param component _more_
     * @param label _more_
     * @param initName _more_
     * @param tooltip _more_
     *
     * @return _more_
     */
    public String doNew(Component component, String label, String initName,
                        String tooltip) {
        JTextField field = new JTextField(initName, 20);
        if (tooltip != null) {
            field.setToolTipText(tooltip);
        }
        Component contents = GuiUtils.inset(GuiUtils.label(getTitle()
                                 + " Name: ", field), 4);
        while (true) {
            if ( !GuiUtils.showOkCancelDialog(null, label + " " + getTitle(),
                    contents, component, Misc.newList(field))) {
                return null;
            }
            String newName = field.getText().trim();
            if (newName.equals("")) {
                LogUtil.userMessage("The name cannot be blank");
                continue;
            }
            //Clean up any categories
            List toks = StringUtil.split(newName, ">", true, true);
            newName = StringUtil.join(">", toks);
            if ( !hasUsers(newName)) {
                return newName;
            }
            int result =
                GuiUtils.showYesNoCancelDialog(null,
                    "A resource with name " + newName
                    + " exists.  Do you want to overwrite?", getTitle());
            if (result == 2) {
                return null;
            }
            if (result == 0) {
                return newName;
            }
        }
    }



    /**
     * _more_
     *
     * @param o
     * @param component
     * @return _more_
     */
    public String doSaveAs(NamedObject o, Component component) {
        String     originalName = o.getName();
        JTextField field        = new JTextField(o.getName(), 20);
        Component contents = GuiUtils.inset(GuiUtils.label(getTitle()
                                 + " name: ", field), 4);
        while (true) {
            if ( !GuiUtils.showOkCancelDialog(null, "Save", contents,
                    component, Misc.newList(field))) {
                return null;
            }
            String newName = field.getText().trim();
            if (newName.equals("")) {
                LogUtil.userMessage("The name cannot be blank");
                continue;
            }
            if ( !(newName.equals(originalName)) && hasUsers(newName)) {
                if ( !GuiUtils.askYesNo(
                        getTitle() + " exists",
                        "A resource with name " + newName
                        + " exists.  Do you want to overwrite?")) {
                    return null;
                }
            }
            return newName;
        }
    }

    /**
     * _more_
     *
     * @param name
     * @return _more_
     */
    public boolean hasUsers(String name) {
        return (getUsersObject(name) != null);
    }

    /**
     * _more_
     *
     * @param name
     * @return _more_
     */
    public NamedObject getUsersObject(String name) {
        for (int i = 0; i < usersList.size(); i++) {
            if (((NamedObject) usersList.get(i)).getName().equals(name)) {
                return (NamedObject) usersList.get(i);
            }
        }
        return null;
    }




    /**
     * _more_
     *
     * @param xml
     * @param filename _more_
     * @return _more_
     */
    public Object toObject(String xml, String filename) {
        return toObject(xml);
    }



    /**
     * _more_
     *
     * @param xml
     * @return _more_
     */
    public Object toObject(String xml) {
        try {
            Element root = XmlUtil.getRoot(xml);
            return getEncoder().toObject(root);
        } catch (Exception exc) {
            if (shouldWeIgnoreThisXml(xml)) {
                return null;
            }
            exc.printStackTrace();
        }
        return null;
    }


    /**
     * _more_
     *
     * @param xml
     * @return _more_
     */
    protected boolean shouldWeIgnoreThisXml(String xml) {
        return Misc.isHtml(xml);
    }

    /** _more_ */
    private PropertyChangeSupport propertyListeners;

    /**
     * Returns the PropertyChangeListener-s of this instance.
     * @return                  The PropertyChangeListener-s.
     */
    private PropertyChangeSupport getPropertyListeners() {

        if (propertyListeners == null) {
            synchronized (this) {
                if (propertyListeners == null) {
                    propertyListeners = new PropertyChangeSupport(this);
                }
            }
        }

        return propertyListeners;
    }

    /**
     * Adds a PropertyChangeListener to this instance.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from this instance.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(listener);
        }
    }


    /**
     * Fires a PropertyChangeEvent.
     * @param event             The PropertyChangeEvent.
     */
    protected void firePropertyChange(PropertyChangeEvent event) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(event);
        }
    }


    /**
     * Fires a PropertyChangeEvent.
     * @param propertyName      The name of the property.
     * @param oldValue          The old value of the property.
     * @param newValue          The new value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(propertyName, oldValue,
                    newValue);
        }
    }




}

