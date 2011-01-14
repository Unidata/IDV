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


import org.w3c.dom.Attr;
import org.w3c.dom.Element;


import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.gis.Transect;

import ucar.unidata.gis.maps.MapData;
import ucar.unidata.gis.maps.MapInfo;


import ucar.unidata.idv.control.DisplaySetting;


import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;

import ucar.unidata.util.Resource;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.*;



import java.awt.*;
import java.awt.event.*;


import java.io.*;

import java.net.*;




import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;




import java.util.regex.*;

import javax.swing.*;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;



/**
 * This class manages the set of resources that are used to instantiate the IDV.
 * The idea is that the idv has a number of &quot;rbi&quot; (resource bundle for the idv)
 * files that define the different collections  of resources. For each collection (e.g.,
 * colortables, derived quantities) there is a list of locations that specify the file
 * (either xml or text) that defines the particular resource.
 * <p> For example in resources/idv.rbi we have:
 * <pre>
 * &lt;resources name=&quot;idv.resource.colortables&quot;&gt;
 *   &lt;resource location=&quot;%USERPATH%/colortables.xml&quot;/&gt;
 *   &lt;resource location=&quot;%SITEPATH%/colortables.xml&quot;/&gt;
 *   &lt;resource location=&quot;%IDVPATH%/colortables.xml&quot;/&gt;
 * &lt;/resources&gt;
 * </pre>
 * This defines the collection of color table resources. The %USERPATH%, etc.,
 * are macros that are replaced with the corresponding value.
 * <p>
 * This class has a set of static IdvResource and XmlIdvResource members,
 * one for each resource collection, e.g., RSC_COLORTABLES.
 * The IdvResource objects are those that  point to text resources. The XmlIdvResource
 * objects point to xml files. These objects  provide methods to access the
 * {@link ucar.unidata.util.ResourceCollection}  and
 * {@link ucar.unidata.xml.XmlResourceCollection} objects that actually do the
 * work of reading in the resources, creating the xml doms, etc.
 *
 * @author IDV development team
 */


public class IdvResourceManager extends IdvManager implements HyperlinkListener {


    /** debug flag */
    private boolean debug = false;



    /** Xml tag name for the &quot;resources&quot; tag in  the rbi file */
    public static final String TAG_RESOURCES = "resources";

    /** resource bundle tag */
    public static final String TAG_RESOURCEBUNDLE = "resourcebundle";

    /** Xml tag name for the &quot;resource&quot; tag in  the rbi file */
    public static final String TAG_RESOURCE = "resource";

    /** Xml tag name */
    public static final String TAG_PROPERTY = "property";

    /** Xml attr name for the &quot;loadmore&quot; attribute in  the rbi file */
    public static final String ATTR_LOADMORE = "loadmore";

    /** Xml attr name for the &quot;removeprevious&quot; attribute in  the rbi file */
    public static final String ATTR_REMOVEPREVIOUS = "removeprevious";

    /** xml attribute */
    public static final String ATTR_NAME = "name";

    /** Xml attr name for the &quot;id&quot; attribute in  the rbi file */
    public static final String ATTR_ID = "id";

    /** Xml attr name for the &quot;value&quot; attribute in  the rbi file */
    public static final String ATTR_VALUE = "value";

    /** Xml attr name for the &quot;label&quot; attribute in  the rbi file */
    public static final String ATTR_LABEL = "label";


    /** Xml attr name for the &quot;location&quot; attribute in  the rbi file */
    public static final String ATTR_LOCATION = "location";

    /** Xml attr name for the &quot;type&quot; attribute in  the rbi file */
    public static final String ATTR_RESOURCETYPE = "type";

    /** List of static IdvResource objects */
    protected static List resources = new ArrayList();



    /** Create this for the default stations */
    private NamedStationTable defaultTable;


    /** Holds the macronames */
    private String[] macroNames;

    /** Holds the actual macro values */
    private String[] macroValues;



    /** Points to the color tables */
    public static final IdvResource RSC_COLORTABLES =
        new IdvResource(
            "idv.resource.colortables", "Color tables",
            "(\\.gp$|\\.ncmap$|\\.et$|\\.tbl$|\\.rgb$|colortables\\.xml$)",
            true);

    /** Points to station models */
    public static final IdvResource RSC_STATIONMODELS =
        new IdvResource("idv.resource.stationmodels", "Station models",
                        "(stationmodels\\.xml$|.ism$)", true);

    /** viewpoints */
    public static final IdvResource RSC_VIEWPOINTS =
        new XmlIdvResource("idv.resource.viewpoints", "Viewpoints",
                           "viewpoints\\.xml$", true);

    /** Points to the projections */
    public static final IdvResource RSC_PROJECTIONS =
        new IdvResource("idv.resource.projections", "Map projections",
                        "projections\\.xml$", true);

    /** Points to the parameter defaults */
    public static final IdvResource RSC_PARAMDEFAULTS =
        new XmlIdvResource("idv.resource.paramdefaults",
                           "Parameter defaults", "paramdefaults\\.xml$",
                           true);

    /** Points to the projections */
    public static final IdvResource RSC_DISPLAYSETTINGS =
        new IdvResource("idv.resource.displaysettings", "Display settings",
                        "displaysettings\\.xml$", true);


    /** Points to data aliases */
    public static final IdvResource RSC_ALIASES =
        new XmlIdvResource("idv.resource.aliases", "Data aliases",
                           "aliases\\.xml$", true);

    /** Points to canned bundles */
    public static final IdvResource RSC_BUNDLEXML =
        new XmlIdvResource("idv.resource.bundlexml",
                           "List of favorite bundles", "bundles\\.xml$",
                           true);


    /** Points to the derived quantities and end user formulas */
    public static final IdvResource RSC_DERIVED =
        new XmlIdvResource("idv.resource.derived",
                           "Derived quantities and formulas",
                           "derived\\.xml$", true);


    /** Points to jython libraries */
    public static final IdvResource RSC_JYTHON =
        new IdvResource("idv.resource.jython", "Jython libraries", "\\.py$",
                        true);


    /** Points to the skin xml */
    public static final XmlIdvResource RSC_SKIN =
        new XmlIdvResource("idv.resource.skin", "UI Skin", "skin\\.xml$",
                           false);


    /** Points to the quicklink pages */
    public static final IdvResource RSC_QUICKLINKS =
        new IdvResource("idv.resource.quicklinks", "Quicklinks", "\\.qhtml$");


    /** Points to the projections */
    public static final IdvResource RSC_TRANSECTS =
        new IdvResource("idv.resource.transects", "Map transects",
                        "transects\\.xml$");


    /** Points to the message catalogs */
    public static final IdvResource RSC_MESSAGES =
        new IdvResource("idv.resource.messages", "Message catalog",
                        "\\.pack$");

    /** Points to the grib look up tables */
    public static final IdvResource RSC_GRIB1LOOKUPTABLES =
        new IdvResource("idv.resource.grib1lookuptables",
                        "Grib 1 Lookup tables", ".*grib1.*\\.lst$");

    /** Points to the grib look up tables */
    public static final IdvResource RSC_GRIB2LOOKUPTABLES =
        new IdvResource("idv.resource.grib2lookuptables",
                        "Grib 2 Lookup tables", ".*grib2.*\\.lst$");

    /** Points to the gempak parameter look up tables */
    public static final IdvResource RSC_GEMPAKGRIDPARAMTABLES =
        new IdvResource("idv.resource.gempakgridparam",
                        "GEMPAK Grid Parameter tables", ".*grib.*\\.tbl$");

    /**
     * Points to jython libraries that are to be copied into the local
     * Jython directory
     */
    public static final IdvResource RSC_JYTHONTOCOPY =
        new IdvResource("idv.resource.jythontocopy",
                        "Jython libraries to copy");


    /** Points to the adde image defaults */
    public static final XmlIdvResource RSC_IMAGEDEFAULTS =
        new XmlIdvResource("idv.resource.imagedefaults",
                           "ADDE Image Defaults", "imagedefaults\\.xml$");

    /** Points to the background image defaults */
    public static final XmlIdvResource RSC_BACKGROUNDWMS =
        new XmlIdvResource("idv.resource.backgroundwms",
                           "Background WMS images", "backgroundwms\\.xml$");

    /** Points to the background image defaults */
    public static final XmlIdvResource RSC_IMAGESETS =
        new XmlIdvResource("idv.resource.imagesets", "Image Sets",
                           "imagesets\\.xml$");


    /** Points to the automatic display creation xml */
    public static final XmlIdvResource RSC_AUTODISPLAYS =
        new XmlIdvResource("idv.resource.autodisplays",
                           "Automatic display creation",
                           "autodisplays\\.xml$");


    /** Points to the skin xml */
    public static final XmlIdvResource RSC_TOOLBAR =
        new XmlIdvResource("idv.resource.toolbar", "Tool bar",
                           "toolbar\\.xml$");

    /** Points to the skin xml */
    public static final XmlIdvResource RSC_ACTIONS =
        new XmlIdvResource("idv.resource.actions", "Actions",
                           "actions\\.xml$");


    /** Points to station model symbols */
    public static final XmlIdvResource RSC_STATIONSYMBOLS =
        new XmlIdvResource("idv.resource.stationsymbols",
                           "Station model symbols", "stationsymbols\\.xml$");

    /** Points to the pairs of foreground/background colors */
    public static final XmlIdvResource RSC_COLORPAIRS =
        new XmlIdvResource("idv.resource.colorpairs", "Color pairs");


    /** Points to the data source descriptions */
    public static final IdvResource RSC_DATASOURCE =
        new XmlIdvResource("idv.resource.datasource",
                           "Specification of the data sources",
                           "datasource\\.xml$");

    /** Points to the adde server descriptions */
    public static final IdvResource RSC_ADDESERVER =
        new XmlIdvResource("idv.resource.addeservers",
                           "Specification of the ADDE servers",
                           "addeservers\\.xml$");


    /** Points to the specification of the choosers ui */
    public static final IdvResource RSC_CHOOSERS =
        new XmlIdvResource(
            "idv.resource.choosers",
            "The definition of the user interface for data choosers",
            "choosers\\.xml$");

    /** Points to xidv bundle files */
    public static final IdvResource RSC_BUNDLES =
        new IdvResource("idv.resource.bundles",
                        "Default bundles that are evaluated at start up");

    /** Points to the control descriptor specification */
    public static final IdvResource RSC_CONTROLS =
        new XmlIdvResource("idv.resource.controls",
                           "Available display controls", "controls\\.xml$",
                           true);

    /** Points to the help tips */
    public static final IdvResource RSC_HELPTIPS =
        new XmlIdvResource("idv.resource.helptips", "Help tips",
                           "helptips\\.xml$", true);

    /** Points to the location files (e.g., nexrad stations) */
    public static final IdvResource RSC_LOCATIONS =
        new XmlIdvResource("idv.resource.locations",
                           "Fixed station locations",
                           "(locations.*\\.xml$|locations.*\\.csv$)");

    /** Points to the maps */
    public static final IdvResource RSC_GLOBEMAPS =
        new XmlIdvResource("idv.resource.globemaps",
                           "Maps for the globe displays", "globemaps\\.xml$");

    /** Points to the maps */
    public static final IdvResource RSC_MAPS =
        new XmlIdvResource("idv.resource.maps", "Maps for the displays",
                           "maps\\.xml$");



    /** Points to the menu bar xml */
    public static final IdvResource RSC_MENUBAR =
        new XmlIdvResource("idv.resource.menubar",
                           "Commands in the menu bar",
                           "(defaultmenu\\.xml$|menubar\\.xml$)", true);



    /** Points to the parameter groups */
    public static final IdvResource RSC_PARAMGROUPS =
        new XmlIdvResource("idv.resource.paramgroups", "Parameter groups",
                           "paramgroups\\.xml$");

    /** Points to the user created chooser components */
    public static final IdvResource RSC_USERCHOOSER =
        new XmlIdvResource("idv.resource.userchooser",
                           "End user constructed data choosers");

    /** Points to user preferences */
    public static final IdvResource RSC_PREFERENCES =
        new XmlIdvResource("idv.resource.preferences", "User preferences",
                           "main\\.xml$");

    /** Points to plugin */
    public static final IdvResource RSC_PLUGINS =
        new XmlIdvResource("idv.resource.plugins", "Plugins");

    /** resource listing the plugins index */
    public static final IdvResource RSC_PLUGININDEX =
        new XmlIdvResource("idv.resource.pluginindex",
                           "Index of available plugins", "plugins\\.xml$",
                           true);

    /** Points to prototypes */
    public static final IdvResource RSC_PROTOTYPES =
        new IdvResource("idv.resource.prototypes", "Prototypes");

    /** Points to netcdf-Java config files */
    public static final IdvResource RSC_NJCONFIG =
        new IdvResource("idv.resource.njconfig", "NetCDF-Java Config",
                        "nj.*Config\\.xml$");


    /** Publishers */
    public static final IdvResource RSC_PUBLISHERS =
        new XmlIdvResource("idv.resource.publishers", "Publishers",
                           "publishers\\.xml$");

    /** Publishers */
    public static final IdvResource RSC_PUBLISHERTYPES =
        new XmlIdvResource("idv.resource.publishertypes", "Publisher Types",
                           "publishertypes\\.xml$");


    /** Maps location table full name  to table */
    private Hashtable locationFullNameMap = new Hashtable();

    /** Maps the location table type to a list of location tables */
    private Hashtable typeToLocations = new Hashtable();

    /** Maps location table name  to table */
    private Hashtable locationNameMap = new Hashtable();

    /** NamedStationTables created through the location resource */
    private List locationList;


    /** All display settings_ */
    private List<DisplaySetting> displaySettings;

    /** Local display settings_ */
    private List<DisplaySetting> localDisplaySettings;

    /** ts */
    private long displaySettingsTimestamp = 0;

    /**
     * A list of the ids of all of the
     *   instantiated {@link ucar.unidata.util.ResourceCollection}s
     */
    private List allResources = new ArrayList();

    /** Mapping from id to {@link ucar.unidata.util.ResourceCollection}s */
    private Hashtable resourceCollections = new Hashtable();


    /** Location of all of the rbi files */
    private List rbiFiles;


    /** Transect objects */
    private List transects;

    /** System transect objects */
    private List nonLocalTransects;


    /**
     * Create me
     *
     * @param idv The IDV
     *
     */
    public IdvResourceManager(IntegratedDataViewer idv) {
        super(idv);
    }



    /**
     * Get the resources
     *
     * @return List of all resources
     */
    public List getResources() {
        return resources;
    }

    /**
     * Get the list of resources for the user
     *
     * @return  the list of resources
     */
    public List getResourcesForUser() {
        List userResources = new ArrayList();
        for (int i = 0; i < resources.size(); i++) {
            IdvResource idvResource = (IdvResource) resources.get(i);
            if (idvResource.forUser) {
                userResources.add(idvResource);
            }
        }
        return userResources;
    }





    /**
     * Class IdvResource holds the resources files for a particular
     * type of resource
     *
     *
     * @author IDV development team
     */
    public static class IdvResource {

        /** Extra paths we add on for plugin */
        protected List extraPaths = new ArrayList();

        /**
         * The resource id  (e.g., idv.resource.colortables).
         */
        String id;

        /** The resource description */
        String description;

        /** Is this an xml resource */
        boolean isXml;

        /** flag for whether this is a user resource or not */
        boolean forUser = false;

        /** The pattern to match on files */
        protected String fileNamePattern;

        /**
         * Keep the file name pattern around. This is used
         *   to find files that may match this resource type
         */
        protected Pattern pattern;


        /**
         * Create the object with the given id
         *
         * @param id The id (e.g., idv.resource.colortables)
         *
         */
        public IdvResource(String id) {
            this(id, id, null);
        }

        /**
         * Create the object with the given id and description
         *
         * @param id The id (e.g., idv.resource.colortables)
         * @param description The description
         */
        public IdvResource(String id, String description) {
            this(id, description, null);
        }

        /**
         * Create the object with the given id, description and edit command
         *
         * @param id The id (e.g., idv.resource.colortables)
         * @param description The description
         * @param fileNamePattern The pattern to match
         *
         */
        public IdvResource(String id, String description,
                           String fileNamePattern) {
            this(id, description, fileNamePattern, false);
        }

        /**
         * Create an IdvResource
         *
         * @param id   the id
         * @param description  a description
         * @param fileNamePattern  the file name pattern
         * @param isUser  true if this is a use pattern
         */
        public IdvResource(String id, String description,
                           String fileNamePattern, boolean isUser) {
            this(id, description, fileNamePattern, false, isUser);
        }

        /**
         * Create the object with the given id, description and edit command
         * If isXml is true then this is an xml resource
         *
         * @param id The id (e.g., idv.resource.colortables)
         * @param description The description
         * @param fileNamePattern pattern
         * @param isXml Flag to denote if this is an xml resource
         * @param forUser  flat to denote if this is for the user
         *
         */
        public IdvResource(String id, String description,
                           String fileNamePattern, boolean isXml,
                           boolean forUser) {


            this.forUser         = forUser;
            this.id              = id;
            this.description     = description;
            this.isXml           = isXml;
            this.fileNamePattern = fileNamePattern;
            resources.add(this);
        }


        /**
         * Get the id
         *
         * @return  the id
         */
        public String getId() {
            return id;
        }

        /**
         * Get the description
         *
         * @return description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Create, if needed, and return the file pattern
         *
         * @return file pattern
         */
        public Pattern getPattern() {
            if (fileNamePattern == null) {
                return null;
            }
            if (pattern == null) {
                pattern = Pattern.compile(fileNamePattern);
            }
            return pattern;
        }

        /**
         * Create the actual {@link ucar.unidata.util.ResourceCollection}
         *
         * @param resourceManager The manager that really creates the collection
         * @return The collection
         */
        public ResourceCollection initResourceCollection(
                IdvResourceManager resourceManager) {
            return resourceManager.createResourceCollection(id);
        }




        /**
         * Add the extra path
         *
         * @param path extra path
         */
        public void addExtraPath(String path) {
            extraPaths.add(path);
        }

        /**
         * toString
         *
         * @return toString
         */
        public String toString() {
            return id + " " + fileNamePattern;
        }


    }


    /**
     * Class XmlIdvResource. Respresents xml resources.
     *
     *
     * @author IDV development team
     */
    public static class XmlIdvResource extends IdvResource {

        /**
         * Create this object with the given id
         *
         * @param id The id
         *
         */
        public XmlIdvResource(String id) {
            this(id, id);
        }

        /**
         * Create this object with the given id and description
         *
         * @param id The id
         * @param description The description
         *
         */
        public XmlIdvResource(String id, String description) {
            this(id, description, null);
        }

        /**
         * Create this object with the given id and description
         * and jython edit command.
         *
         * @param id The id
         * @param description The description
         * This may be null.
         * @param fileNamePattern pattern
         *
         */
        public XmlIdvResource(String id, String description,
                              String fileNamePattern) {
            this(id, description, fileNamePattern, false);
        }


        /**
         * Create an XmlIdvResource
         *
         * @param id   the id
         * @param description  a description
         * @param fileNamePattern  the file name pattern
         * @param forUser  true if for the user
         */
        public XmlIdvResource(String id, String description,
                              String fileNamePattern, boolean forUser) {
            super(id, description, fileNamePattern, true, forUser);
        }

        /**
         * Create the {@link ucar.unidata.util.ResourceCollection}
         *
         * @param resourceManager The resource manager that does the work
         * @return The new resource collection
         */
        public ResourceCollection initResourceCollection(
                IdvResourceManager resourceManager) {
            return resourceManager.createXmlResourceCollection(id);
        }
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.util.ResourceCollection} that is represented by
     * the given resource.
     *
     * @param resource The resource
     * @return The resource collection
     */
    public ResourceCollection getResources(IdvResource resource) {
        ResourceCollection resourceCollection = getResources(resource.id);
        resourceCollection.addResources(resource.extraPaths);
        return resourceCollection;
    }

    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.xml.XmlResourceCollection} that is represented by
     * the given resource.
     *
     * @param resource The resource
     * @return The resource collection
     */
    public XmlResourceCollection getXmlResources(IdvResource resource) {
        XmlResourceCollection resourceCollection =
            getXmlResources(resource.id);
        resourceCollection.addResources(resource.extraPaths);
        return resourceCollection;
    }



    /**
     * Remove all default bundles
     */
    public void clearDefaultBundles() {
        getResources(RSC_BUNDLES).deleteAllFiles();
    }


    /**
     * Show the html representation of the list of resources
     */
    public void showHtmlView() {
        GuiUtils.showHtmlDialog(getHtmlView(), this);
    }

    /**
     * Create the html representation of the list of resources.
     *
     * @return The html
     */
    public String getHtmlView() {
        StringBuffer buff = new StringBuffer("<h3>Resources</h3>\n<ul>\n");
        for (int i = 0; i < resources.size(); i++) {
            IdvResource resource = (IdvResource) resources.get(i);
            buff.append("<li><b>" + resource.description + "\n</b> ");

            buff.append(resource.id + "<br>" + " pattern:"
                        + ((resource.fileNamePattern != null)
                           ? "\"" + resource.fileNamePattern + "\""
                           : "none"));

            buff.append("<ul>");
            ResourceCollection rc = getResources(resource);
            for (int resourceIdx = 0; resourceIdx < rc.size();
                    resourceIdx++) {
                boolean isValid = rc.isValid(resourceIdx);
                if (isValid) {
                    buff.append("<li><font color=green>"
                                + rc.get(resourceIdx) + "</font>");
                } else {
                    buff.append("<li><font color=red> X "
                                + rc.get(resourceIdx) + "</font>\n");
                }
            }
            buff.append("</ul>\n");
        }
        buff.append("</ul>\n");

        return buff.toString();
    }

    /**
     * Respond to events from the html view. Bring up the editor
     * for the resource if there is one, etc.
     *
     * @param e The event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        String url;
        if (e.getURL() == null) {
            url = e.getDescription();
        } else {
            url = e.getURL().toString();
        }

        if (url.startsWith("view:")) {
            url = url.substring(5);
            String[]           split = StringUtil.split(url, ":", 2);
            ResourceCollection rc    = getResources(split[0]);
            if (rc == null) {
                return;
            }
            int        index    = new Integer(split[1]).intValue();
            String     contents = rc.read(index);
            JComponent guiContents;
            JTextArea  text = new JTextArea(contents);
            guiContents = GuiUtils.makeScrollPane(text, 50, 100);
            guiContents.setPreferredSize(new Dimension(500, 600));

            if (rc instanceof XmlResourceCollection) {
                XmlResourceCollection xrc     = (XmlResourceCollection) rc;
                Element               root    = xrc.getRoot(index);
                XmlTree               xmlTree = new XmlTree(root);
                xmlTree.setIncludeAttributes(true);
                xmlTree.setUseTagNameAsLabel(true);
                xmlTree.setXmlRoot(root);
                JTabbedPane tab = new JTabbedPane();
                tab.add("Xml", xmlTree);
                tab.add("Text", guiContents);
                guiContents = tab;
            }

            JFrame frame = GuiUtils.createFrame("Resource:" + rc.get(index));
            GuiUtils.packWindow(frame, guiContents, true);
        } else {
            getIdv().handleAction(url, null);
        }

    }



    /**
     * Process the top level root of the rbi xml file
     *
     *  @param root The root of the rbi file
     */
    public void processRbi(Element root) {
        processRbi(root, false);
    }



    /**
     * Process the top level root of the rbi xml file
     *
     *  @param root The root of the rbi file
     * @param payAttentionToLoadMore Flags whether we should stop processing
     * when we encounter a &quot;loadmore&quot; tag in the rbi xml
     */
    protected void processRbi(Element root, boolean payAttentionToLoadMore) {

        NodeList children = XmlUtil.getElements(root, TAG_RESOURCES);

        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            boolean removePrevious = XmlUtil.getAttribute(child,
                                         ATTR_REMOVEPREVIOUS, false);
            String resourceName = XmlUtil.getAttribute(child, ATTR_NAME);
            resourceName = StateManager.fixIds(resourceName);
            ResourceCollection rc = getResources(resourceName);
            if (rc == null) {
                if (XmlUtil.getAttribute(child, ATTR_RESOURCETYPE,
                                         "text").equals("text")) {
                    rc = createResourceCollection(resourceName);
                } else {
                    rc = createXmlResourceCollection(resourceName);
                }
            }



            if (removePrevious) {
                rc.removeAll();
            }

            if (payAttentionToLoadMore && !rc.getCanLoadMore()) {
                continue;
            }
            boolean loadMore = XmlUtil.getAttribute(child, ATTR_LOADMORE,
                                   true);
            if ( !loadMore) {
                rc.setCanLoadMore(false);
            }


            List      locationList = new ArrayList();
            NodeList  resources    = XmlUtil.getElements(child, TAG_RESOURCE);
            Hashtable labelMap     = null;
            for (int resourceIdx = 0; resourceIdx < resources.getLength();
                    resourceIdx++) {
                Element resourceNode = (Element) resources.item(resourceIdx);
                String path =
                    getResourcePath(XmlUtil.getAttribute(resourceNode,
                        ATTR_LOCATION));
                if ((path == null) || (path.length() == 0)) {
                    continue;
                }

                String label = XmlUtil.getAttribute(resourceNode, ATTR_LABEL,
                                   (String) null);
                String id = XmlUtil.getAttribute(resourceNode, ATTR_ID,
                                (String) null);

                NodeList propertyList = XmlUtil.getElements(resourceNode,
                                            TAG_PROPERTY);
                Hashtable nodeProperties = null;
                for (int propIdx = 0; propIdx < propertyList.getLength();
                        propIdx++) {
                    if (nodeProperties == null) {
                        nodeProperties = new Hashtable();
                    }
                    Element propNode = (Element) propertyList.item(propIdx);
                    String propName = XmlUtil.getAttribute(propNode,
                                          ATTR_NAME);
                    String propValue = XmlUtil.getAttribute(propNode,
                                           ATTR_VALUE, (String) null);
                    if (propValue == null) {
                        propValue = XmlUtil.getChildText(propNode);
                    }
                    nodeProperties.put(propName, propValue);
                }

                NamedNodeMap nnm = resourceNode.getAttributes();
                if (nnm != null) {
                    for (int attrIdx = 0; attrIdx < nnm.getLength();
                            attrIdx++) {
                        Attr attr = (Attr) nnm.item(attrIdx);
                        if ( !attr.getNodeName().equals(ATTR_LOCATION)
                                && !attr.getNodeName().equals(ATTR_ID)) {
                            if (nodeProperties == null) {
                                nodeProperties = new Hashtable();
                            }
                            nodeProperties.put(attr.getNodeName(),
                                    attr.getNodeValue());
                        }
                    }
                }


                List paths = new ArrayList();
                if (path.startsWith("index:")) {
                    path = path.substring(6);
                    String index = IOUtil.readContents(path, (String) null);
                    if (index != null) {
                        List lines = StringUtil.split(index, "\n", true,
                                         true);
                        for (int lineIdx = 0; lineIdx < lines.size();
                                lineIdx++) {
                            String line =
                                ((String) lines.get(lineIdx)).trim();
                            if (line.startsWith("#")) {
                                continue;
                            }
                            paths.add(getResourcePath(line));
                        }
                    }
                } else {
                    paths.add(path);
                }


                for (int pathIdx = 0; pathIdx < paths.size(); pathIdx++) {
                    path = (String) paths.get(pathIdx);
                    if (id != null) {
                        rc.setIdForPath(id, path);
                    }
                    locationList.add(new ResourceCollection.Resource(path,
                            label, nodeProperties));
                }
            }
            //            if(debug && locationList.toString().indexOf("defaultmenu")>=0) {
            //                System.err.println ("rbi resources:" + locationList);
            //            }
            rc.addResources(locationList);
        }

    }


    /**
     * Add the given resource collection into the list of resources
     *
     * @param rc The resource collection to add
     */
    private void addResourceCollection(ResourceCollection rc) {
        resourceCollections.put(rc.getId(), rc);
        if ( !allResources.contains(rc.getId())) {
            allResources.add(rc.getId());
        }
    }


    /**
     * Create, if needed, and return the ResourceCollection
     * defined by the given  id
     *
     * @param id The id of the resource, e.g., idv.resource.colortables
     * @return The resource collection
     */
    protected ResourceCollection createResourceCollection(String id) {
        ResourceCollection rc = getResources(id);
        if (rc == null) {
            rc = new ResourceCollection(id, getDescription(id));
            addResourceCollection(rc);
        }
        return rc;
    }


    /**
     * Create, if needed, and return the XmlResourceCollection
     * defined by the given  id
     *
     * @param id The id of the resource, e.g., idv.resource.colortables
     * @return The xml resource collection
     */

    protected XmlResourceCollection createXmlResourceCollection(String id) {
        XmlResourceCollection rc = getXmlResources(id);
        if (rc == null) {
            rc = new XmlResourceCollection(id, getDescription(id));
            addResourceCollection(rc);
        }
        return rc;
    }

    /**
     * Lookup up in  the resource collections map the ResouceCollection
     * with the given id
     *
     * @param id The resource id
     * @return The resource collection or null if not found
     */
    public ResourceCollection getResources(String id) {
        return (ResourceCollection) resourceCollections.get(id);
    }


    /**
     * Remove the resources identified by id
     *
     * @param id id of the resource to remove
     */
    public void removeResources(String id) {
        resourceCollections.remove(id);
    }

    /**
     * Lookup up in  the resource collections map the XmlResouceCollection
     * with the given id
     *
     * @param name The resource name
     * @return The xml resource collection or null if not found
     */
    public XmlResourceCollection getXmlResources(String name) {
        return (XmlResourceCollection) resourceCollections.get(name);
    }


    /**
     *  Return the description of the given resource id. If not found then return the id.
     *
     * @param id Resource id
     * @return Description
     */
    private String getDescription(String id) {
        //TODO?
        return id;
    }


    /**
     *  Create the set of resources (defined in the idv.properties file)
     *  and do the relevant initializations.
     *
     * @param rbiFiles List of rbi files
     */
    protected void init(List rbiFiles) {

        this.rbiFiles = getResourcePaths(rbiFiles);

        for (int i = 0; i < resources.size(); i++) {
            IdvResource resource = (IdvResource) resources.get(i);
            resource.initResourceCollection(this);
        }

        for (int i = 0; i < allResources.size(); i++) {
            String resourceName            = (String) allResources.get(i);
            List   resourcesFromProperties = getResourceList(resourceName);
            if (resourcesFromProperties.size() > 0) {
                ResourceCollection rc = getResources(resourceName);
                if (rc != null) {
                    rc.addResources(resourcesFromProperties);
                    rc.setCanLoadMore(false);
                }
            }
        }


        XmlResourceCollection rbiCollection =
            new XmlResourceCollection("RBI files", this.rbiFiles);
        for (int i = 0; i < rbiCollection.size(); i++) {
            Element root = rbiCollection.getRoot(i);
            if (root == null) {
                continue;
            }
            processRbi(root, true);
        }

        //Load the plugins
        getIdv().getPluginManager();

        if (getArgsManager().listResources) {
            /*
            System.out.println("Resources:");
            for (int i = 0; i < resources.size(); i++) {
                IdvResource idvResource = (IdvResource) resources.get(i);
                System.out.println(idvResource.id + " "
                                   + idvResource.description + " pattern:"
                                   + ((idvResource.fileNamePattern != null)
                                      ? "\"" + idvResource.fileNamePattern
                                        + "\""
                                      : "none"));
                                      }*/

            showHtmlView();
        }

    }



    /**
     *  Do a property lookup of the given resource list property.
     *  Turn the String property value (";" delimited) into a List of Strings
     *  Do a textual substitution of a set of macros with their values (e.g., %USERPATH%).
     *
     * @param propName The property name
     * @return List of expanded Strings
     */
    protected List getResourceList(String propName) {
        String prop = getProperty(propName, NULL_STRING);
        if (prop == null) {
            return new ArrayList();
        }
        return getResourcePaths(StringUtil.split(prop, ";", true, true));
    }

    /**
     * For each string in the given paths list
     * do a textual substitution of a set of macros with their values (e.g., %USERPATH%).
     *
     * @param paths Incoming strings
     * @return Expanded strings
     */
    public List getResourcePaths(List paths) {
        List result = new ArrayList();
        for (int i = 0; i < paths.size(); i++) {
            String path = getResourcePath(paths.get(i).toString());
            if ((path == null) || (path.length() == 0)) {
                continue;
            }
            result.add(path);
        }
        return result;
    }



    /**
     * Clear  the macro values
     *
     */
    public void clearResourceMacros() {
        macroNames  = null;
        macroValues = null;
    }


    /**
     * Get map of macro name -> value. eg: the SITEPATH, USERPATH, etc.
     *
     * @return macros
     */
    protected Hashtable getMacroMap() {
        Hashtable map = new Hashtable();
        for (int i = 0; i < macroNames.length; i++) {
            if ((macroValues[i] == null) || (macroValues[i].length() == 0)) {
                continue;
            }
            map.put(macroNames[i], macroValues[i]);
        }
        return map;
    }


    /**
     * Initialize the macro values
     *
     */
    private void initResourceMacros() {
        macroNames = new String[] {
            "USERPATH", "SITEPATH", "IDVPATH", "DATAPATH", "APPPATH",
            "USERHOME", "VERSION", "VERSION.MAJOR", "VERSION.MINOR",
            "VERSION.REVISION"
        };


        macroValues = new String[] {
            getUserPath(), getSitePath(), getIdvResourcePath(),
            getDataResourcePath(), getAppResourcePath(), getUserHome(),
            getStateManager().getVersion(),
            getStateManager().getVersionMajor(),
            getStateManager().getVersionMinor(),
            getStateManager().getVersionRevision()
        };
    }

    /**
     * Do the macro expansion
     *
     * @param path Macro containing patha
     * @return Expanded path
     */
    public String getResourcePath(String path) {
        //Create the name/value arrays  if needed
        if ((macroNames == null) || (macroValues == null)) {
            initResourceMacros();
        }

        //      boolean printit = path.indexOf("VERSION")>=0;
        //
        //if(printit)System.err.println ("path:" + path);
        //Do the substitution
        for (int i = 0; i < macroNames.length; i++) {
            if (macroValues[i] == null) {
                continue;
            }
            if (macroValues[i].length() > 0) {
                path = StringUtil.replace(path, "%" + macroNames[i] + "%",
                                          macroValues[i]);
            }
        }
        //if(printit)   System.err.println ("\tafter:" + path);

        //Now check if the path has a unsubstituted macro in it.
        //If it does then return null. We do this so we can ignore
        //paths that have macros that were not set (e.g., SITEPATH)
        for (int i = 0; i < macroNames.length; i++) {
            if (path.indexOf("%" + macroNames[i] + "%") >= 0) {
                return null;
            }
        }


        return path;
    }






    /**
     *  Create (if null) and return the list NamedStationTable-s defined by the
     *  locationResources XmlResourceCollection.
     *
     * @return List of named station tables
     */
    public List getLocationList() {
        if (locationList == null) {
            try {
                locationList = NamedStationTable.createStationTables(
                    getXmlResources(RSC_LOCATIONS));
                for (int i = 0; i < locationList.size(); i++) {
                    NamedStationTable location =
                        (NamedStationTable) locationList.get(i);
                    locationFullNameMap.put(
                        location.getFullName().toLowerCase(), location);
                    locationNameMap.put(location.getName().toLowerCase(),
                                        location);
                }
            } catch (Throwable exc) {
                logException("Creating location list", exc);
                return null;
            }
        }
        return locationList;
    }


    /**
     *  Return the path used to substitute the %IDVPATH% macro for in the
     *  resource list properties.
     *
     * @return idv path
     */
    public String getIdvResourcePath() {
        return "/ucar/unidata/idv/resources";
    }

    /**
     *  Return the path used to substitute the %USERHOME% macro for in the
     *  resource list properties.
     *
     * @return idv path
     */
    public String getUserHome() {
        return Misc.getSystemProperty("user.home", ".");
    }


    /**
     *  Return the path used to substitute the %DATAPATH% macro for in the
     *  resource list properties.
     *
     * @return The data path
     */
    public String getDataResourcePath() {
        return "/ucar/unidata/data";
    }


    /**
     *  Return the path used to substitute the %APPPATH% macro for in the
     *  resource list properties.
     *
     * @return The app path
     */
    public String getAppResourcePath() {
        String fromProperties = getProperty(PROP_APPRESOURCEPATH,
                                            (String) null);
        if (fromProperties != null) {
            return fromProperties;
        }
        Class  idvClass = getIdv().getClass();
        String path     = idvClass.getName();
        path = StringUtil.replace(path, ".", "/");
        path = "/" + IOUtil.getFileRoot(path);
        return path;
    }



    /**
     *  Return the path used to substitute the %SITEPATH% macro for in the
     *  resource list properties.
     *
     * @return The site path
     */
    public String getSitePath() {
        if (getArgsManager().sitePathFromArgs != null) {
            return getArgsManager().sitePathFromArgs;
        }
        return getStore().get(PREF_SITEPATH, NULL_STRING);
    }

    /**
     *  Return the path used to substitute the %USERPATH% macro for in the
     *  resource list properties.
     *
     * @return The user path (e.g., ~/.unidata/idv/DefaultIdv)
     */
    public String getUserPath() {
        return getStore().getUserDirectory().getPath();
    }




    /**
     * Remove any local maps.xml file
     *
     *
     * @param forGlobe if true then use the globemaps resource
     * @return Was there one removed
     */
    public boolean removeLocalMaps(boolean forGlobe) {
        ResourceCollection rc = getResourceManager().getResources(forGlobe
                ? IdvResourceManager.RSC_GLOBEMAPS
                : IdvResourceManager.RSC_MAPS);
        for (int i = 0; i < rc.size(); i++) {
            if (rc.isWritable(i)) {
                File f = new File(rc.get(i).toString());
                if (f.exists()) {
                    f.delete();
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * _more_
     *
     * @param forGlobe _more_
     *
     * @return _more_
     */
    public XmlResourceCollection getMapResources(boolean forGlobe) {
        XmlResourceCollection mapResources   = getXmlResources(RSC_MAPS);
        XmlResourceCollection globeResources = getXmlResources(RSC_GLOBEMAPS);
        //If this is for the globe then clone the mapResources and swap out the first one which should be the writable
        if (forGlobe && (globeResources.size() > 0)
                && (mapResources.size() > 0)) {
            mapResources = new XmlResourceCollection(RSC_GLOBEMAPS.id,
                    mapResources);
            Object firstGlobeResource = globeResources.get(0);
            mapResources.removeResource(0);
            mapResources.addResourceAtStart(firstGlobeResource.toString());
        }

        return mapResources;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<MapData> getMaps() {
        MapInfo mapInfo = new MapInfo(getXmlResources(RSC_MAPS), false,
                                      false);
        List<MapData> results = new ArrayList<MapData>();
        List          maps    = mapInfo.getMapDataList();
        Hashtable     seen    = new Hashtable();
        for (int i = 0; i < maps.size(); i++) {
            MapData mapData = (MapData) maps.get(i);
            //Be unique on source---description
            String key = mapData.getSource() + "-----"
                         + mapData.getDescription();
            if (seen.get(key) != null) {
                continue;
            }
            seen.put(key, key);
            //            mapData.setVisible(true);
            results.add(mapData);
        }
        return results;
    }




    /**
     * A utility to instantiate a MapInfo from the maps resources
     *
     *
     * @param forGlobe if true then use the globemaps resource
     * @return The MapInfo holding the map stuff.
     */
    public MapInfo createMapInfo(boolean forGlobe) {
        return new MapInfo(getMapResources(forGlobe), true);
    }



    /**
     * Write out the given maps xml to the writable resource
     *
     * @param mapsXml The maps xml
     * @param forGlobe if true then use the globemaps resource
     */

    public void writeMapState(String mapsXml, boolean forGlobe) {
        try {
            ResourceCollection mapResources = getResources(forGlobe
                    ? RSC_GLOBEMAPS
                    : RSC_MAPS);
            mapResources.writeWritableResource(mapsXml);
            mapResources.clearCache();
        } catch (Exception exc) {
            logException("Writing maps", exc);
        }
    }





    /**
     * Find the default NamedStationTable
     *
     * @return The default stations
     */
    public NamedStationTable getDefaultStations() {
        if (defaultTable == null) {
            defaultTable = findLocations(getProperty(PROP_DEFAULTLOCATIONS,
                    "NEXRAD Sites"));
            if (defaultTable == null) {
                defaultTable = (NamedStationTable) getLocationList().get(0);
            }
        }
        return defaultTable;
    }





    /**
     * Find the NamedStationTable with the given name
     *
     * @param type type
     *
     * @return The list of stations
     */
    public List findLocationsByType(String type) {
        List result = (List) typeToLocations.get(type);
        if (result == null) {
            result = new ArrayList();
            typeToLocations.put(type, result);
            List tables = getLocationList();
            for (int i = 0; i < tables.size(); i++) {
                NamedStationTable table = (NamedStationTable) tables.get(i);
                if (Misc.equals(table.getType(), type)) {
                    result.add(table);
                }
            }
        }
        return result;
    }



    /**
     * Find the NamedStationTable with the given name
     *
     * @param name Name of station
     *
     * @return The station
     */
    public NamedStationTable findLocations(String name) {
        List tables = getLocationList();
        NamedStationTable stationTable =
            (NamedStationTable) locationFullNameMap.get(name.toLowerCase());
        if (stationTable != null) {
            return stationTable;
        }
        return (NamedStationTable) locationNameMap.get(name.toLowerCase());
    }


    /**
     * A hardcoded url base to find resources at
     *
     * @return http url resource base
     */
    public String getResourceUrlBase() {
        return "http://www.unidata.ucar.edu/georesources";
    }





    /**
     * Get the list of transects
     *
     * @return List of transects
     */
    public List getTransects() {
        if (transects == null) {
            transects         = new ArrayList();
            nonLocalTransects = new ArrayList();
            ResourceCollection rc = getResourceManager().getResources(
                                        IdvResourceManager.RSC_TRANSECTS);
            Hashtable seen = new Hashtable();
            for (int resourceIdx = rc.size() - 1; resourceIdx >= 0;
                    resourceIdx--) {
                String resourcePath = rc.get(resourceIdx).toString();
                if (StringUtil.stringMatch(resourcePath, "jar[0-9]+:")) {
                    int idx = resourcePath.indexOf(":");
                    resourcePath = resourcePath.substring(idx + 1);
                    if ( !resourcePath.startsWith("/")) {
                        resourcePath = "/" + resourcePath;
                    }
                }
                if (seen.get(resourcePath) != null) {
                    continue;
                }
                seen.put(resourcePath, "");
                String xml = rc.read(resourceIdx, false);
                if (xml == null) {
                    continue;
                }
                boolean writable = rc.isWritable(resourceIdx);
                try {
                    Element root = XmlUtil.getRoot(xml);
                    List    tmp  = Transect.parseXml(root);
                    for (int transectIdx = 0; transectIdx < tmp.size();
                            transectIdx++) {
                        Transect transect = (Transect) tmp.get(transectIdx);
                        transect.setEditable(writable);
                        if ( !transects.contains(transect)) {
                            transects.add(transect);
                            if ( !writable) {
                                nonLocalTransects.add(transect);
                            }
                        }
                    }

                } catch (Throwable parseExc) {
                    logException("Error parsing transect xml", parseExc);
                }

            }

        }
        return transects;
    }


    /**
     * Write the non-system transects in the list to the users
     * transects resource file
     *
     * @param t List of transects
     */
    public void writeTransects(List t) {
        getTransects();
        List local = new ArrayList();
        for (int i = 0; i < t.size(); i++) {
            Transect transect = (Transect) t.get(i);
            if ( !nonLocalTransects.contains(transect)) {
                local.add(transect);
            }
        }
        //        String xml = Transect.toXml(local);
        String xml = Transect.toXml(t);
        ResourceCollection rc = getResourceManager().getResources(
                                    IdvResourceManager.RSC_TRANSECTS);

        try {
            rc.writeWritableResource(xml);
        } catch (Throwable parseExc) {
            logException("Writing transects", parseExc);
        }
        transects         = null;
        nonLocalTransects = null;
    }



    /**
     * Get all display settings
     *
     * @return display settings
     */
    public List<DisplaySetting> getDisplaySettings() {
        if (displaySettings == null) {
            displaySettings = new ArrayList<DisplaySetting>();
            try {
                Hashtable map = new Hashtable();
                ResourceCollection rc =
                    getResourceManager().getResources(
                        IdvResourceManager.RSC_DISPLAYSETTINGS);
                for (int i = 0; i < rc.size(); i++) {
                    String xml = rc.read(i);
                    if (xml == null) {
                        continue;
                    }
                    List<DisplaySetting> tmp =
                        (List<DisplaySetting>) getIdv().decodeObject(xml);
                    if (tmp == null) {
                        continue;
                    }
                    boolean isLocal = rc.isWritable(i);
                    if (isLocal) {
                        localDisplaySettings = tmp;
                    }
                    List<DisplaySetting> uniqueOnes =
                        new ArrayList<DisplaySetting>();
                    for (int displaySettingIdx = 0;
                            displaySettingIdx < tmp.size();
                            displaySettingIdx++) {
                        DisplaySetting displaySetting =
                            (DisplaySetting) tmp.get(displaySettingIdx);
                        if (displaySetting.getName() == null) {
                            displaySetting.setName("");
                        }
                        if (map.get(displaySetting.getName()) != null) {
                            continue;
                        }
                        map.put(displaySetting.getName(), displaySetting);
                        displaySetting.setIsLocal(isLocal);
                        uniqueOnes.add(displaySetting);
                    }
                    displaySettings.addAll(uniqueOnes);
                }
            } catch (Throwable exc) {
                logException("Reading display settings", exc);
            }

        }
        if (localDisplaySettings == null) {
            localDisplaySettings = new ArrayList<DisplaySetting>();
        }
        return displaySettings;
    }

    /**
     * Find display setting by name
     *
     * @param name name
     *
     * @return displaysetting
     */
    public DisplaySetting findDisplaySetting(String name) {
        List<DisplaySetting> displaySettings = getDisplaySettings();
        for (int i = 0; i < displaySettings.size(); i++) {
            DisplaySetting displaySetting =
                (DisplaySetting) displaySettings.get(i);
            if (Misc.equals(name, displaySetting.getName())) {
                return displaySetting;
            }
        }
        return null;
    }


    /**
     * handle when a display setting changed
     *
     * @param displaySetting display setting that changed
     */
    public void displaySettingChanged(DisplaySetting displaySetting) {
        if ( !localDisplaySettings.contains(displaySetting)) {
            displaySetting.setIsLocal(true);
            localDisplaySettings.add(displaySetting);
        }
        writeDisplaySettings();
    }


    /**
     * Add new display setting
     *
     * @param displaySetting display setting
     */
    public void addDisplaySetting(DisplaySetting displaySetting) {
        displaySetting.setIsLocal(true);
        localDisplaySettings.add(displaySetting);
        displaySettings.add(displaySetting);
        writeDisplaySettings();
    }

    /**
     * Remove list of display settings
     *
     * @param list list
     */
    public void removeDisplaySettings(List<DisplaySetting> list) {
        for (int displaySettingIdx = 0; displaySettingIdx < list.size();
                displaySettingIdx++) {
            DisplaySetting displaySetting =
                (DisplaySetting) list.get(displaySettingIdx);
            localDisplaySettings.remove(displaySetting);
            displaySettings.remove(displaySetting);
        }
        writeDisplaySettings();
    }

    /**
     * Remove display setting
     *
     * @param displaySetting display setting
     */
    public void removeDisplaySetting(DisplaySetting displaySetting) {
        removeDisplaySettings(
            (List<DisplaySetting>) Misc.newList(displaySetting));
    }


    /**
     * write
     */
    private void writeDisplaySettings() {
        try {
            displaySettingsTimestamp++;
            String xml = getIdv().encodeObject(localDisplaySettings, true);
            ResourceCollection rc =
                getResourceManager().getResources(
                    IdvResourceManager.RSC_DISPLAYSETTINGS);
            rc.writeWritableResource(xml);
        } catch (Throwable exc) {
            logException("Writing display settings", exc);
        }

    }

    /**
     * get timestamp
     *
     * @return timestamp_
     */
    public long getDisplaySettingsTimestamp() {
        return displaySettingsTimestamp;
    }


}
