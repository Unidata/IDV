/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv;


import org.w3c.dom.Element;



import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DerivedDataChoice;

import ucar.unidata.ui.Help;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.xml.XmlResourceCollection;

import ucar.unidata.xml.XmlUtil;



import visad.VisADException;


import java.lang.reflect.InvocationTargetException;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * This holds a description, derived from a controls.xml file,
 * of a {@link DisplayControl}. It provides facilities
 * for instantiating the DisplayControl it represents.
 *
 * @author IDV development team
 */

public class ControlDescriptor {

    /** Special control descriptor ID for the display templates */
    public static final String ID_DISPLAYTEMPLATE = "displaytemplate";

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ControlDescriptor.class.getName());


    /** The id in the controls.xml file for the map display */
    public static final String DISPLAYID_MAP = "mapdisplay";


    /** Xml &quot;controls&quot; tag name  for the control descriptor xml */
    public static final String TAG_CONTROLS = "controls";

    /** Xml &quot;control&quot; tag name  for the control descriptor xml */
    public static final String TAG_CONTROL = "control";

    /** Xml &quot;categories&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_CATEGORIES = "categories";


    /**  */
    public static final String ATTR_DOESLEVELS = "doeslevels";

    /** Xml &quot;displaycategory&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_DISPLAYCATEGORY = "displaycategory";

    /** Xml &quot;class&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_CLASS = "class";

    /** Xml &quot;description&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_DESCRIPTION = "description";

    /** Xml &quot;code&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_CODE = "code";

    /** Xml &quot;icon&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_ICON = "icon";

    /** Xml &quot;id&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_ID = "id";

    /** Xml &quot;label&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_LABEL = "label";

    /** Xml &quot;levels&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_LEVELS = "levels";

    /** Xml &quot;properties&quot; attribute name  for the control descriptor xml */
    public static final String TAG_PROPERTY = "property";

    /** Xml &quot;properties&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_PROPERTIES = "properties";

    /** Xml &quot;canstandalone&quot; attribute name for the control descriptor xml */
    public static final String ATTR_CANSTANDALONE = "canstandalone";

    /** Xml &quot;viewmanagers&quot; attribute name  for the control descriptor xml */
    public static final String ATTR_VIEWMANAGERS = "viewmanagers";

    /** Xml &quot;display&quot; tag name for the display xml */
    public static final String TAG_DISPLAY = "display";

    /** Xml &quot;datachoice&quot; tag name for the display xml */
    public static final String TAG_DATACHOICE = "datachoice";

    /** Xml &quot;datasource&quot; tag name for the display xml */
    public static final String TAG_DATASOURCE = "datasource";

    /** Xml &quot;name&quot; attribute name for the display xml */
    public static final String ATTR_NAME = "name";

    /** Xml &quot;name&quot; attribute value for the display xml */
    public static final String ATTR_VALUE = "value";

    /** Xml &quot;source&quot; attribute name for the display xml */
    public static final String ATTR_SOURCE = "source";

    /** Xml &quot;type&quot; attribute name for the display xml */
    public static final String ATTR_TYPE = "type";

    /** Xml &quot;label&quot; attribute name  for the control descriptor xml */
    public static final String PROP_DISPLAYNAME = "displayName";





    /** Reference to the IDV */
    IntegratedDataViewer idv;

    /** The java Class of the {@link DisplayControl} */
    Class controlClass;

    /**
     * Semi-colon separated list of properties passed to the
     *   display control
     */
    Hashtable properties = new Hashtable();

    /** The identifier of the display control */
    String controlId;

    /** The label of the display control */
    String label;

    /** The description of the display control */
    String description;

    /**
     * The controls.xml can specify jython code that  is applied to the
     * {@link ucar.unidata.data.DataChoice}
     */
    String code;

    /** Path to the UI icon for this display control */
    String icon;

    /**
     * List of {@link ucar.unidata.data.DataCategory}s that this
     * display control is applicable to.
     */
    List dataCategories;

    /** This is the category used to place the display controls into the legend. */
    String displayCategory;


    /** The name of the {@link DisplayControl} class this descriptor represents */
    public String className;

    /**
     * If true the the {@link DisplayControl} this descriptor represents
     * can be created without any data choices. (e.g., radar rings)
     */
    public boolean canStandAlone = false;

    /** Does this control do levels */
    public boolean doesLevels = false;

    /**
     * This is the file path to the display template file
     * for when we are a wrapper around the template.
     */
    private String displayTemplateFile;


    /** If we are a wrapper arounda display template this is the prototype display control object */
    private DisplayControl displayTemplatePrototype;


    /** The controls.xml node */
    Element node;

    /** the levels */
    private List levels;

    /**
     * Parameterless constructor for xml encoding/decoding
     */
    public ControlDescriptor() {}


    /**
     * Constructor for when we are a wrapper around a display template
     *
     * @param idv The idv
     * @param displayTemplateFile The template we are a wrapper around.
     * @param prototype This is the prototype object that is created form the display template.
     * we get it passed in here so we can grab some of its state (e.g., categories).
     */
    public ControlDescriptor(IntegratedDataViewer idv,
                             String displayTemplateFile,
                             DisplayControl prototype) {

        this.idv                 = idv;
        this.displayTemplateFile = displayTemplateFile;
        this.label =
            IOUtil.stripExtension(IOUtil.getFileTail(displayTemplateFile));

        displayCategory          = "Display Templates";
        this.controlId           = ID_DISPLAYTEMPLATE;
        displayTemplatePrototype = prototype;
        this.dataCategories      = prototype.getCategories();
    }





    /**
     * Create the descriptor
     *
     * @param idv The IDV
     * @param node The xml node that defines this descriptor
     *
     * @throws ClassNotFoundException When the Class defined by the classname attribute  cannot
     * be found.
     */
    public ControlDescriptor(IntegratedDataViewer idv, Element node)
            throws ClassNotFoundException {
        this.idv     = idv;
        this.node    = node;
        controlId    = XmlUtil.getAttribute(node, ATTR_ID);
        controlClass = Misc.findClass(XmlUtil.getAttribute(node, ATTR_CLASS));
        label        = XmlUtil.getAttribute(node, ATTR_LABEL, "");
        description  = XmlUtil.getAttribute(node, ATTR_DESCRIPTION, label);
        if (label.length() == 0) {
            label = description;
        }
        code            = XmlUtil.getAttribute(node, ATTR_CODE,
                (String) null);

        icon            = XmlUtil.getAttribute(node, ATTR_ICON,
                (String) null);
        displayCategory = XmlUtil.getAttribute(node, ATTR_DISPLAYCATEGORY,
                "");
        canStandAlone = XmlUtil.getAttribute(node, ATTR_CANSTANDALONE, false);
        doesLevels    = XmlUtil.getAttribute(node, ATTR_DOESLEVELS, false);

        if (XmlUtil.hasAttribute(node, ATTR_LEVELS)) {
            List<String> toks = StringUtil.split(XmlUtil.getAttribute(node,
                                    ATTR_LEVELS), ",", true, true);
            levels = new ArrayList();
            try {
                for (String tok : toks) {
                    levels.add(ucar.visad.Util.toReal(tok));
                }
            } catch (Throwable exc) {
                logException("Processing levels", exc);
            }
        }


        properties = new Hashtable();
        properties.put(PROP_DISPLAYNAME, label);
        properties.putAll(
            StringUtil.parsePropertiesString(
                XmlUtil.getAttribute(node, ATTR_PROPERTIES, "")));
        if (properties.get("displayName") == null) {
            properties.put("displayName", label);
        }
        List nodes = XmlUtil.findChildren(node, TAG_PROPERTY);
        for (int i = 0; i < nodes.size(); i++) {
            Element propertyNode = (Element) nodes.get(i);
            String  name = XmlUtil.getAttribute(propertyNode, ATTR_NAME);
            String  value        = null;
            if (XmlUtil.hasAttribute(propertyNode, ATTR_VALUE)) {
                value = XmlUtil.getAttribute(propertyNode, ATTR_VALUE);
            } else {
                value = XmlUtil.getChildText(propertyNode);
            }
            properties.put(name, value);

        }


        this.dataCategories =
            DataCategory.parseCategories(XmlUtil.getAttribute(node,
                ATTR_CATEGORIES, ""));
        //Add in the display:... category
        this.dataCategories.add(new DataCategory("display:" + controlId,
                false));
    }


    /**
     * Get the xml representation of the controls.xml node
     *
     * @param sb buffer to append to
     */
    protected void getDescriptorXml(StringBuffer sb) {
        if (node != null) {
            sb.append(XmlUtil.toString(node, true));
        }
    }






    /**
     *  Iterate through the ControlDescriptor describing xml files defined by the given
     *  resources parameter and create the ControlDescriptor objects
     *
     * @param idv The IDV
     * @param resources The collection of controls descriptor .xml files
     */
    protected static void load(IntegratedDataViewer idv,
                               XmlResourceCollection resources) {
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i);
            if (root == null) {
                continue;
            }
            createControlDescriptors(idv, root);
        }
    }


    /**
     *  Create the control descriptors described under the root xml element.
     *
     * @param idv The IDV
     * @param root  The root of the control descriptor xml
     */
    public static void createControlDescriptors(IntegratedDataViewer idv,
            Element root) {
        try {
            if (root.getTagName().equals(TAG_CONTROL)) {
                createControlDescriptor(idv, root);
            } else {
                List controlNodes = XmlUtil.findChildren(root, TAG_CONTROL);
                for (int i = 0; i < controlNodes.size(); i++) {
                    Element node = (Element) controlNodes.get(i);
                    createControlDescriptor(idv, node);
                }
            }
        } catch (Throwable exc) {
            logException("Creating control descriptors", exc);
            return;
        }
    }



    /**
     *  Process the given display xml file.
     *
     * @param idv The IDV
     * @param xml The xml (e.g., from resources/controls.xml)
     */

    public static void processDisplayXml(IntegratedDataViewer idv,
                                         String xml) {
        String displayFile = "display xml file";
        try {
            Element root = XmlUtil.getRoot(xml);
            if (root == null) {
                return;
            }
            List displayNodes = XmlUtil.findChildren(root, TAG_DISPLAY);
            for (int displayIdx = 0; displayIdx < displayNodes.size();
                    displayIdx++) {
                Element displayNode = (Element) displayNodes.get(displayIdx);
                String displayType = XmlUtil.getAttribute(displayNode,
                                         ATTR_TYPE);
                ControlDescriptor cd = idv.getControlDescriptor(displayType);
                if (cd == null) {
                    throw new IllegalStateException(
                        "Could not find control descriptor:" + displayType);
                }
                List dataChoices = new ArrayList();
                List dataChoiceNodes = XmlUtil.findChildren(displayNode,
                                           TAG_DATACHOICE);
                for (int dcIdx = 0; dcIdx < dataChoiceNodes.size(); dcIdx++) {
                    Element dataChoiceNode =
                        (Element) dataChoiceNodes.get(dcIdx);
                    String dcName = XmlUtil.getAttribute(dataChoiceNode,
                                        ATTR_NAME);
                    Element dataSourceNode =
                        XmlUtil.findChild(dataChoiceNode, TAG_DATASOURCE);
                    if (dataSourceNode == null) {
                        throw new IllegalStateException(
                            "Could not find data source node");
                    }
                    String dataSourceType =
                        XmlUtil.getAttribute(dataSourceNode, ATTR_TYPE,
                                             (String) null);
                    String dataSourceSource =
                        XmlUtil.getAttribute(dataSourceNode, ATTR_SOURCE);
                    DataSource dataSource =
                        idv.makeOneDataSource(dataSourceSource,
                            dataSourceType, null);
                    if (dataSource == null) {
                        continue;
                    }
                    DataChoice dataChoice = dataSource.findDataChoice(dcName);
                    if (dataChoice == null) {
                        throw new IllegalStateException(
                            "Could not find data choice : " + dcName
                            + " from:" + dataSource);
                    }
                    dataChoices.add(dataChoice);
                }
                cd.doMakeDisplay(dataChoices, idv, "", null, true);

            }
        } catch (Throwable exc) {
            logException("Processing display file:" + displayFile, exc);
            return;
        }

    }




    /**
     *  Create the control descriptor described by the given control node
     *
     * @param idv The IDV
     * @param node The control node
     */
    public static void createControlDescriptor(IntegratedDataViewer idv,
            Element node) {
        try {
            idv.addControlDescriptor(new ControlDescriptor(idv, node));
        } catch (Throwable exc) {
            logException("Creating control descriptor", exc);
            return;
        }
    }



    /**
     * This finds and returns the set of {@link ControlDescriptor}s,
     * from the given descriptors list,
     * that are applicable to any of the {@link ucar.unidata.data.DataCategory}s
     * in the given categories list.
     *
     * @param categories List of {@link ucar.unidata.data.DataCategory}s
     * @param descriptors List of control descriptors
     * @return List of applicable control descriptors
     */

    public static List getApplicableControlDescriptors(List categories,
            List descriptors) {
        return getApplicableControlDescriptors(categories, descriptors,
                false, true);
    }

    /**
     * Find the list of  control descriptors  that are applicable
     * applicable to the given data categories.
     *
     * @param categories The categories
     * @param descriptors The descriptors to check
     * @param includeStandAlone ditto
     * @param includeIfEmpty If we have no categories do we keep going
     *
     * @return  List of ControlDescriptors
     */
    public static List getApplicableControlDescriptors(List categories,
            List descriptors, boolean includeStandAlone,
            boolean includeIfEmpty) {
        List    l              = new ArrayList();
        boolean haveCategories = (categories.size() > 0);
        if (categories.contains(DataCategory.NONE_CATEGORY)) {
            return l;
        }
        for (int i = 0; i < descriptors.size(); i++) {
            ControlDescriptor dd = (ControlDescriptor) descriptors.get(i);
            if (includeStandAlone && dd.canStandAlone()) {
                l.add(dd);
                continue;
            }
            if ( !includeIfEmpty && !haveCategories) {
                continue;
            }
            if ( !dd.applicableTo(categories)) {
                continue;
            }
            l.add(dd);
        }
        return l;
    }

    /** My prototype */
    private DisplayControl prototype;

    /**
     * Show help for the display control
     */
    public void showHelp() {
        try {
            if (prototype == null) {
                prototype = (DisplayControl) controlClass.newInstance();
                prototype.initBasic(controlId, dataCategories, properties);
            }
            prototype.showHelp();
        } catch (Throwable exc) {
            LogUtil.printException(log_, "Showing help", exc);
        }
    }




    /* TODO:
    protected boolean isControlHelpValid () {
        try {
            DisplayControl control =
                (DisplayControl) controlClass.newInstance();
            control.initBasic (controlId, dataCategories, properties);
            List helpIds = control.getHelpIds();
            boolean isValidHelp = false;
            for (int i=0;i<helpIds.size() && !isValidHelp;i++) {
                String id = (String) helpIds.get (i);
                if (id == null) continue;
                if (Help.getDefaultHelp().isValidID (id)) {
                    isValidHelp = true;
                }
            }
            if (!isValidHelp) {
                System.err.println ("Invalid help for:" + this+"\nHelp ids= " + helpIds);
            }
            return isValidHelp;
        } catch (Exception exc) {
            LogUtil.printException(log_,
                                   "ControlDescriptor.Creating display", exc);
        }
        return true;
    }
    */


    /**
     * Initialize  this descriptor
     *
     * @param controlId  The id of the display control
     * @param controlClass The class of the display control
     * @param label The label
     * @param description The description
     * @param icon The icon to use in the gui (or null)
     * @param categories String representation of the
     *                   {@link  ucar.unidata.data.DataCategory}s
     * @param properties Semi-colon delimited list of name=value properties
     */
    private void initxxx(String controlId, Class controlClass, String label,
                         String description, String icon, String categories,
                         String properties) {
        this.dataCategories = DataCategory.parseCategories(categories);
        this.controlId      = controlId;
        this.controlClass   = controlClass;
        this.label          = label;
        this.description    = description;
        //        this.properties     = properties;
        this.icon = icon;

    }


    /**
     * Can this descriptor stand alone
     *
     * @return Can stand alone
     */
    public boolean canStandAlone() {
        return canStandAlone;
    }

    /**
     * Get the levels
     *
     * @return the levels
     */
    public List getLevels() {
        return levels;
    }


    /**
     * Does levels
     *
     * @return does levels
     */
    public boolean doesLevels() {
        return doesLevels;
    }

    /**
     * Get the semi-color delimited list of name=value properties
     * that is passed to the display control
     *
     * @return The properties string
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Get the path to the icon used in the GUI
     *
     * @return The icon path
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get the list of {@link ucar.unidata.data.DataCategory}s
     *
     * @return List of data categories
     */
    public List getCategories() {
        return dataCategories;
    }

    /**
     * Set the list of {@link ucar.unidata.data.DataCategory}s
     *
     * @param categoryList The new category list
     */
    public void setCategories(List categoryList) {
        this.dataCategories = categoryList;
    }

    /*
     *    public boolean getCanTakeMultiples () {
     *   return canTakeMultiples;
     *   }
     */

    /**
     *  Wrapper that calls applicableTo
     * with the list of categories of the given
     * {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice The data choice we get the categories from
     *
     * @return Is the display control applicable to any of
     * DataCategory-s that describe the given DataChoice
     */

    public boolean applicableTo(DataChoice dataChoice) {
        return applicableTo(dataChoice.getCategories());
    }

    /**
     *  Wrapper that calls applicableTo with a list that
     *  contains the given category
     *
     * @param category The category to add into the list
     * @return Is the display control applicable to this DataCategory
     */
    public boolean applicableTo(DataCategory category) {
        return applicableTo(Misc.newList(category));
    }

    /**
     *  Go through the list of categories. If any of the data categories
     *  for this object is applicable to any of the given categories
     *  then return true. Else return false;
     *
     * @param categories The list of {@link ucar.unidata.data.DataCategory}s
     * @return Is the display control applicable to any of
     * DataCategory-s in the list
     */
    public boolean applicableTo(List categories) {
        return DataCategory.applicableTo(dataCategories, categories);
    }


    /**
     *  Wrapper method that makes a single element array of DataChoices with the given
     *  DataChoice parameter. This just creates a list holding the given
     *  data choice and passes through to
     * {@link #doMakeDisplay (List,IntegratedDataView,String,DataSelection)}
     *
     * @param dataChoice The data choice to create the display with
     * @param viewer The IDV
     * @param argProperties Semi-colon separated name=value property string
     * @param dataSelection The data selection that the user may have defined
     * for subsetting times, etc.
     * @return The newly create DisplayControl
     *
     * @throws IllegalAccessException When  we cannot access the constructor through reflection
     * @throws InstantiationException When something bad happens in the reflection based object creation
     * @throws InvocationTargetException When something bad happens in the reflection based object creation
     * @throws RemoteException When something bad happens in the instantiated DisplayControl
     * @throws VisADException When something bad happens in the instantiated DisplayControl
     */
    public DisplayControl doMakeDisplay(DataChoice dataChoice,
                                        IntegratedDataViewer viewer,
                                        String argProperties,
                                        DataSelection dataSelection)
            throws InstantiationException, VisADException, RemoteException,
                   IllegalAccessException, InvocationTargetException {
        return doMakeDisplay(Misc.newList(dataChoice), viewer, argProperties,
                             dataSelection, true);
    }


    /**
     *  If this ControlDescriptor has a "code" attribute (which represents jython code that
     *  is used to get the true data from the DataChoice) then actually create and return
     *  new {@link ucar.unidata.data.DerivedDataChoice}-s that have the incoming DataChoice
     *  as an operand and the "code" as the code.
     *
     * @param choices List of {@link ucar.unidata.data.DataChoice}s to wrap
     * @return The new set of  {@link ucar.unidata.data.DerivedDataChoice}s  that
     * wrap the arguments or the argument list, choices, if there was node code member.
     */
    private List processList(List choices) {
        if (code != null) {
            List newList = new ArrayList();
            for (int i = 0; i < choices.size(); i++) {
                DataChoice dataChoice = (DataChoice) choices.get(i);
                DerivedDataChoice ddc =
                    new DerivedDataChoice((DataContext) idv,
                                          Misc.newList(dataChoice),
                                          dataChoice.getId().toString(),
                                          dataChoice.getDescription(), "",
                                          null, null, code);
                newList.add(ddc);
            }
            return newList;
        }
        return choices;
    }


    /**
     * Instantiate the DisplayControl defined by the Class data member. This clones
     * the given dataChoices list and the data choices it contains, creates
     * the DisplayControl via reflection and then  initializes the control
     * in a thread.
     *
     * @param dataChoices List of {@link ucar.unidata.data.DataChoice}s
     * to instantiate the display control with.
     * @param viewer The IDV
     * @param argPropertiesString  Semi-colon separated name=value property string
     * @param dataSelection The data selection that the user may have defined
     * for subsetting times, etc.
     * @param initDisplayInThread If true then initialize the display in a thread, else do it here
     *
     * @return The newly create DisplayControl
     *
     * @throws IllegalAccessException When  we cannot access the constructor through reflection
     * @throws InstantiationException When something bad happens in the reflection based object creation
     * @throws InvocationTargetException When something bad happens in the reflection based object creation
     * @throws RemoteException When something bad happens in the instantiated DisplayControl
     * @throws VisADException When something bad happens in the instantiated DisplayControl
     */
    public DisplayControl doMakeDisplay(List dataChoices,
                                        final IntegratedDataViewer viewer,
                                        String argPropertiesString,
                                        final DataSelection dataSelection,
                                        boolean initDisplayInThread)
            throws InstantiationException, VisADException, RemoteException,
                   IllegalAccessException, InvocationTargetException {
        Hashtable argProperties = null;
        if (argPropertiesString != null) {
            argProperties =
                StringUtil.parsePropertiesString(argPropertiesString);
        }
        return doMakeDisplay(dataChoices, viewer, argProperties,
                             dataSelection, initDisplayInThread);
    }




    /**
     * Instantiate the DisplayControl defined by the Class data member. This clones
     * the given dataChoices list and the data choices it contains, creates
     * the DisplayControl via reflection and then  initializes the control
     * in a thread.
     *
     * @param dataChoices List of {@link ucar.unidata.data.DataChoice}s
     * to instantiate the display control with.
     * @param viewer The IDV
     * @param argProperties properties
     * @param dataSelection The data selection that the user may have defined
     * for subsetting times, etc.
     * @param initDisplayInThread If true then initialize the display in a thread, else do it here
     *
     * @return The newly create DisplayControl
     *
     * @throws IllegalAccessException When  we cannot access the constructor through reflection
     * @throws InstantiationException When something bad happens in the reflection based object creation
     * @throws InvocationTargetException When something bad happens in the reflection based object creation
     * @throws RemoteException When something bad happens in the instantiated DisplayControl
     * @throws VisADException When something bad happens in the instantiated DisplayControl
     */
    public DisplayControl doMakeDisplay(List dataChoices,
                                        final IntegratedDataViewer viewer,
                                        Hashtable argProperties,
                                        final DataSelection dataSelection,
                                        boolean initDisplayInThread)
            throws InstantiationException, VisADException, RemoteException,
                   IllegalAccessException, InvocationTargetException {




        dataChoices = processList(dataChoices);
        final List newDataChoices = DataChoice.cloneDataChoices(dataChoices);



        if (argProperties == null) {
            argProperties = new Hashtable();
        }

        DisplayControl control = null;

        if (displayTemplateFile != null) {
            control = idv.getPersistenceManager().instantiateFromTemplate(
                displayTemplateFile);
            control.initAsTemplate();
        } else {
            control =
                (DisplayControl) idv.getPersistenceManager().getPrototype(
                    controlClass);
            if (control == null) {
                control = (DisplayControl) controlClass.newInstance();
            }
        }



        if (properties != null) {
            argProperties.putAll(properties);
        }
        if (idv != null) {
            boolean showWindow =
                ((Boolean) idv.getStateManager().getPreference(
                    IdvConstants.PREF_SHOWCONTROLWINDOW,
                    Boolean.TRUE)).booleanValue();
            if (showWindow) {
                argProperties.put("windowVisible", "true");
            }
        }

        argProperties.put("version", "1.3");
        final Hashtable      newProperties = argProperties;
        final DisplayControl theNewControl = control;
        if (initDisplayInThread) {
            Misc.run(new Runnable() {
                public void run() {
                    initControl(theNewControl, newDataChoices, viewer,
                                newProperties, dataSelection);
                }
            });
        } else {
            initControl(theNewControl, newDataChoices, viewer, newProperties,
                        dataSelection);
        }
        return control;
    }


    /**
     * Initializes the {@link DisplayControl}
     *
     * @param control The control to initialize
     * @param newDataChoices List of {@link ucar.unidata.data.DataChoice}s
     *                       to pass to the display control
     * @param idv The idv
     * @param properties properties
     * @param dataSelection The data selection that the user may have defined
     *                      to hold data subsetting information (e.g., times)
     * @deprecated Use other initControl
     */
    public void initControl(DisplayControl control, List newDataChoices,
                            IntegratedDataViewer idv, String properties,
                            DataSelection dataSelection) {

        initControl(control, newDataChoices, idv, ((properties == null)
                ? null
                : StringUtil.parsePropertiesString(
                    properties)), dataSelection);
    }



    /**
     * Initializes the {@link DisplayControl}
     *
     * @param control The control to initialize
     * @param newDataChoices List of {@link ucar.unidata.data.DataChoice}s
     *                       to pass to the display control
     * @param idv The idv
     * @param newProperties Semi-colon delimited list of name=value properties
     * @param dataSelection The data selection that the user may have defined
     *                      to hold data subsetting information (e.g., times)
     */
    public void initControl(DisplayControl control, List newDataChoices,
                            IntegratedDataViewer idv,
                            Hashtable newProperties,
                            DataSelection dataSelection) {




        if (newProperties == null) {
            newProperties = new Hashtable();
        }

        if (newProperties.get("displayCategory") == null) {
            if (displayTemplatePrototype != null) {
                newProperties.put(
                    "displayCategory",
                    displayTemplatePrototype.getDisplayCategory());
            } else {
                if (displayCategory.length() > 0) {
                    newProperties.put("displayCategory", displayCategory);
                }
            }
        }

        idv.showWaitCursor();
        try {

            Trace.call1("ControlDescriptor control.init");
            control.init(controlId, dataCategories, newDataChoices, idv,
                         newProperties, dataSelection);
            Trace.call2("ControlDescriptor control.init");

            idv.controlHasBeenInitialized(control);
        } catch (DataCancelException dce) {
            //This means the selection of some derived quantity operand was canceled
        } catch (org.python.core.PyException pye) {  // kind of a hack to get the real error
            List<String> lines = StringUtil.split(pye.toString(), "\n", true,
                                     true);
            String message = lines.get(lines.size() - 1);
            message = message.replace("visad.VisADException:", "");
            message = message.replace("java.lang.Exception:", "");
            message = message.trim();
            LogUtil.printException(log_,
                                   "Creating display: " + label + "\n"
                                   + message, pye);
        } catch (Throwable exc) {
            LogUtil.printException(log_, "Creating display: " + label, exc);
        }
        idv.showNormalCursor();
    }

    /**
     * Get the label
     * Mostly used for xml encoding
     *
     * @return The label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label
     * Mostly used for xml encoding
     *
     * @param label The label
     */
    public void setLabel(String label) {
        this.label = label;
    }


    /**
     * Get the control id
     * Mostly used for xml encoding
     *
     * @return The control id
     */
    public String getControlId() {
        return controlId;
    }

    /**
     * get the description
     * Mostly used for xml encoding
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the tooltip text
     *
     * @return tooltip
     */
    public String getToolTipText() {
        if (description == null) {
            return label;
        }
        return description;
    }

    /**
     * Set the description
     * Mostly used for xml encoding
     *
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * String representation of this object
     *
     * @return The label
     */
    public String toString() {
        return label;
    }

    /**
     * Log the error
     *
     * @param msg error message
     * @param exc The exception
     */
    public static void logException(String msg, Throwable exc) {
        LogUtil.printException(log_, msg, exc);
    }

    /**
     * Get the display category.
     * Mostly used for xml encoding
     *
     * @return The display category
     */
    public String getDisplayCategory() {
        return displayCategory;
    }


    /**
     * Set the display category.
     * Mostly used for xml encoding
     *
     * @param c The display category
     */
    public void setDisplayCategory(String c) {
        displayCategory = c;
    }


    /**
     * Set the DisplayTemplateFile property.
     *
     * @param value The new value for DisplayTemplateFile
     */
    public void setDisplayTemplateFile(String value) {
        displayTemplateFile = value;
    }

    /**
     * Get the DisplayTemplateFile property.
     *
     * @return The DisplayTemplateFile
     */
    public String getDisplayTemplateFile() {
        return displayTemplateFile;
    }




}
