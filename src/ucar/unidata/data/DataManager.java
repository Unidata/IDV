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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

//import opendap.dap.http.HTTPSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.idv.StateManager;
import ucar.unidata.util.AccountManager;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.nc2.util.net.HTTPSession;
import visad.DateTime;
import visad.Real;
import visad.RealType;
import visad.SampledSet;
import visad.Unit;
import visad.VisADException;
import visad.data.text.TextAdapter;




/**
 * A class for managing {@link DataSource}s
 *
 * @author Metapps development team
 * @version $Revision: 1.122 $Date: 2007/08/08 16:24:11 $
 */
public class DataManager {

    /** try to instantiate the VisAD standard quantities */
    static {
        try {
            visad.data.netcdf.StandardQuantityDB sqdb =
                visad.data.netcdf.StandardQuantityDB.instance();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
        try {
            String handlers =
                System.getProperty("java.protocol.handler.pkgs");
            String newProperty = null;
            if (handlers == null) {
                newProperty = "com.sun.net.ssl.internal.www.protocol";
            } else if (handlers.indexOf(
                    "com.sun.net.ssl.internal.www.protocol") < 0) {
                newProperty = "com.sun.net.ssl.internal.www.protocol | "
                              + handlers;
            }
            if (newProperty != null) {  // was set above
                System.setProperty("java.protocol.handler.pkgs", newProperty);
                java.security.Security.addProvider(
                    new com.sun.net.ssl.internal.ssl.Provider());
            }
        } catch (Exception e) {
            System.out.println(
                "Unable to set System Property: java.protocol.handler.pkgs");
        }
    }

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(DataManager.class.getName());

    /** The "unknown" data type */
    public static final String DATATYPE_UNKNOWN = "unknown";

    /** The known data type id */
    public static final String DATATYPE_ID = "datatypeid";

    /** The show in tree property */
    public static final String PROP_SHOW_IN_TREE = "show_in_tree";

    /** _more_ */
    public static final String PROP_CACHE_PERCENT = "idv.data.cache.percent";

    /** bbox property */
    public static final String PROP_GEOSUBSET_BBOX =
        "idv.data.geosubset.bbox";

    /** preference id */
    public static final String PROP_NETCDF_CONVENTIONHANDLERS =
        "idv.data.netcdf.conventionhandlers";

    /** The default display property */
    public static final String PROP_DEFAULT_DISPLAY = "default_display";


    /** The XML tag for datasources */
    public static final String TAG_DATASOURCES = "datasources";

    /** The XML tag for a datasource */
    public static final String TAG_DATASOURCE = "datasource";

    /** The XML tag for a property */
    public static final String TAG_PROPERTY = "property";

    /** The XML attribute for allowing multiple data choices */
    public static final String ATTR_DOESMULTIPLES = "doesmultiples";

    /** xml attribute identifier for the datasource.xml file */
    public static final String ATTR_STANDALONE = "standalone";

    /** xml attribute identifier for the datasource.xml file */
    public static final String ATTR_NCMLTEMPLATE = "ncmltemplate";

    /** The XML "id" attribute */
    public static final String ATTR_ID = "id";

    /** The XML factory attribute */
    public static final String ATTR_FACTORY = "factory";

    /** The XML label attribute */
    public static final String ATTR_LABEL = "label";

    /** The XML name attribute */
    public static final String ATTR_NAME = "name";

    /** The XML patterns attribute */
    public static final String ATTR_PATTERNS = "patterns";

    /** The XML file selection attribute */
    public static final String ATTR_FILESELECTION = "fileselection";

    /** The XML value attribute */
    public static final String ATTR_VALUE = "value";

    /** Preference for where to save the grib index */
    public static final String PREF_GRIBINDEXINCACHE =
        "idv.data.gribindexincache";

    /**
     * The {@link DataContext} provides some services for this
     * DataManager (interacting with the user, etc.). It is usually an
     * instance of {@link  ucar.unidata.idv.IntegratedDataViewer}.
     */
    private DataContext dataContext;

    /**
     * A mapping between a {@link DataSource} and its name.
     */
    private Hashtable dataSourceToDefiningObject = new Hashtable();

    /**
     * A mapping between the object initially used to create a
     * {@link DataSource} and the DataSource.
     */
    private Hashtable definingObjectToDataSource = new Hashtable();

    /**
     * The list of {@link  DataSource}s currently active within the idv.
     */
    private ArrayList<DataSource> dataSources = new ArrayList();

    /**
     * The list of {@link DataSourceDescriptor}s defined by the datasource.xml
     * resource files.
     */
    protected ArrayList descriptors = new ArrayList();

    /** List of the data source descriptors that can be stand alone */
    private List<DataSourceDescriptor> standaloneDescriptors =
        new ArrayList<DataSourceDescriptor>();

    /**
     * A mapping from datasource_id (String) to {@link DataSourceDescriptor}
     * object. This is used when we load multiple datasource descriptor files
     * so we can know whether we have already loaded a descriptor and to
     * do a lookup of a descriptor based on its id.
     */
    protected Hashtable idToDescriptor = new Hashtable();

    /**
     * The list of {@link ucar.unidata.util.PatternFileFilter}s that we create
     * from the datasources.xml. We use this list when we are given a file name
     * or a URL and are looking for the DataSourceDescriptor that handles that
     * type of file.
     */
    protected ArrayList allFilters = new ArrayList();

    /**
     * The list of {@link ucar.unidata.util.PatternFileFilter}s that we create
     * from the datasources.xml for those that are specified to be for file
     * selection (i.e., the xml tag has fileselection="true").
     */
    protected ArrayList fileFilters = new ArrayList();

    /**
     * This hashtable allows us to keep track of the filter strings that we
     * have already seen so we don't create duplicates.
     */
    protected Hashtable seenFilters = new Hashtable();

    /**
     * A list of {@link ucar.unidata.util.TwoFacedObject}s
     * (label, datasource_id) for those datasource descriptors
     * that are for file selection.
     */
    protected ArrayList fileDataSourceIds = new ArrayList();

    /**
     * A list of {@link ucar.unidata.util.TwoFacedObject}s
     * (label, datasource_id) for all  datasource descriptors
     */
    protected ArrayList allDataSourceIds = new ArrayList();

    /** Maps data source names */
    protected Hashtable dataSourceNameMap = new Hashtable();


    /**
     * Create a new DataManager with the given {@link DataContext}.
     *
     * @param dataContext      The {@link DataContext} that this DataManager
     *                         exists within (this is usually an instance of
     *                         {@link  ucar.unidata.idv.IntegratedDataViewer}).
     */
    public DataManager(DataContext dataContext) {
        this.dataContext = dataContext;
        initURLStreamHandlers();
        initCache();

    }

    /**
     * Get the data cache dir
     *
     * @return data cache dir
     */
    public String getDataCacheDirectory() {
        String dataCacheDir =
            IOUtil.joinDir(
                dataContext.getObjectStore().getUserTmpDirectory(), "cache/");
        IOUtil.makeDir(dataCacheDir);
        return dataCacheDir;
    }

    /**
     * Initialize the resources
     *
     * @param resourceManager  resource manager
     */
    public void initResources(IdvResourceManager resourceManager) {

        loadDataSourceXml(
            resourceManager.getXmlResources(
                IdvResourceManager.RSC_DATASOURCE));

        loadIospResources(resourceManager);
        String[] visadProperties = {
            "visad.java3d.imageByRef", "visad.java3d.geometryByRef",
            "visad.actionimpl.tracetime", "visad.actionimpl.tracestack",
            "visad.cachingcoordinatesystem.debugtime",
            "visad.java3d.textureNpot", "visad.data.arraycache.enabled",
            "visad.data.arraycache.lowerthreshold",
            "visad.data.arraycache.upperthreshold",
            "visad.data.arraycache.usedatacachemanager",
            "visad.contourFillSingleValueAsTexture"
        };


        for (String visadProp : visadProperties) {
            System.setProperty(visadProp,
                               dataContext.getIdv().getStateManager()
                                   .getPreferenceOrProperty(visadProp,
                                       "false"));
        }

        SampledSet.setCacheSizeThreshold(
            dataContext.getIdv().getProperty(
                "visad.sampledset.cachesizethreshold", 10000));


        //The IDV can run normally (i.e., the usual interactive IDV) and also in server mode (e.g., within RAMADDA)
        //If in server mode then its expected that the server has done this configuration
        //If we aren't in server mode  then we configure things here
        if ( !dataContext.getIdv().getServerMode()) {
            //Initialize the nc file cache
            //Don't do this for now
            //            NetcdfDataset.initNetcdfFileCache(1, 15, -1);

            //Set the temp file and the cache policy
            String nj22TmpFile =
                IOUtil.joinDir(
                    dataContext.getObjectStore().getUserTmpDirectory(),
                    "nj22/");
            IOUtil.makeDir(nj22TmpFile);
            ucar.nc2.util.DiskCache.setRootDirectory(nj22TmpFile);
            ucar.nc2.util.DiskCache.setCachePolicy(true);
            // have to do this since nj2.2.20
            ucar.nc2.iosp.grid.GridServiceProvider
                .setIndexAlwaysInCache(dataContext.getIdv().getStateManager()
                    .getPreferenceOrProperty(PREF_GRIBINDEXINCACHE, true));


            visad.data.DataCacheManager.getCacheManager().setCacheDir(
                new File(getDataCacheDirectory()));
            visad.data.DataCacheManager.getCacheManager()
                .setMemoryPercent(dataContext.getIdv().getStateManager()
                    .getPreferenceOrProperty(PROP_CACHE_PERCENT, 0.25));

            AccountManager accountManager =
                AccountManager.getGlobalAccountManager();
            if (accountManager == null) {
                accountManager = new AccountManager(
                    dataContext.getIdv().getStore().getUserDirectory());
                AccountManager.setGlobalAccountManager(accountManager);
            }
            AccountManager provider = accountManager;
            HTTPSession.setGlobalCredentialsProvider(provider);
            // Get the current version
            String version =  dataContext.getIdv().getStateManager().getVersion();
            if(version == null) version = "xxx";
            HTTPSession.setGlobalUserAgent("IDV "+version);
            // Force long timeouts
            HTTPSession.setGlobalSoTimeout(5*60*1000);
            HTTPSession.setGlobalConnectionTimeout(5*60*1000);

//            try {
//                HttpWrap client = new HttpWrap();
//                client.setGlobalCredentialsProvider(provider);
//                // fix opendap.dap.DConnect2.setHttpClient(client);
//                ucar.unidata.io.http.HTTPRandomAccessFile.setHttpClient(
//                    client);
//            } catch (Exception exc) {
//                LogUtil.printException(log_, "Cannot create http client",
//                                       exc);
//            }
        }


        String defaultBoundingBoxString =
            dataContext.getIdv().getProperty(PROP_GEOSUBSET_BBOX,
                                             (String) null);

        TextAdapter.addDateParser(new TextAdapter.DateParser() {
            public DateTime createDateTime(String value, String format,
                                           TimeZone timezone)
                    throws VisADException {
                if (format.endsWith("dh1") || format.endsWith("dh2")
                        || format.endsWith("dh3") || format.endsWith("dh4")) {
                    int len = new Integer(format.substring(format.length()
                                  - 1)).intValue();
                    value = value.trim();
                    int idx = value.lastIndexOf(" ");
                    if (idx < 0) {
                        idx = 0;
                    }
                    String dh    = value.substring(idx + 1);
                    int    ptIdx = dh.length() - len;
                    dh = dh.substring(0, ptIdx) + "." + dh.substring(ptIdx);
                    while ((dh.length() > 1) && dh.startsWith("0")) {
                        dh = dh.substring(1);
                    }
                    double hours = new Double(dh);
                    String HH    = "" + (int) hours;
                    String mm    = "" + (int) (60 * (hours - (int) hours));
                    format = format.replace("dh" + len, "HH:mm");
                    String newValue = value.trim().substring(0, idx + 1) + HH
                                      + ":" + mm;
                    return visad.DateTime.createDateTime(newValue, format,
                            timezone);
                }

                return null;
            }
        });


        if (defaultBoundingBoxString != null) {
            List toks = StringUtil.split(defaultBoundingBoxString, ",", true,
                                         true);
            if (toks.size() != 4) {
                System.err.println("Bad idv.geosubset property:"
                                   + defaultBoundingBoxString);
            } else {
                GeoSelection.setDefaultBoundingBox(
                    new GeoLocationInfo(
                        Misc.decodeLatLon((String) toks.get(0)),
                        Misc.decodeLatLon((String) toks.get(1)),
                        Misc.decodeLatLon((String) toks.get(2)),
                        Misc.decodeLatLon((String) toks.get(3))));
            }
        }

        String conventionHandlers =
            dataContext.getIdv().getProperty(PROP_NETCDF_CONVENTIONHANDLERS,
                                             (String) null);
        if (conventionHandlers != null) {
            List tokens = StringUtil.split(conventionHandlers, ",", true,
                                           true);
            for (int i = 0; i < tokens.size(); i++) {
                String token     = (String) tokens.get(i);
                List   subTokens = StringUtil.split(token, ":", true, true);
                if (subTokens.size() < 2) {
                    System.err.println("Bad convention handler token:"
                                       + token);
                    continue;
                }

                String className = subTokens.get(0).toString().trim();
                try {
                    Class  handlerClass   = Misc.findClass(className);
                    String conventionName =
                        subTokens.get(1).toString().trim();
                    log_.debug("Loading convention handler: "
                               + handlerClass.getName() + " for convention:"
                               + conventionName);
                    ucar.nc2.dataset.CoordSysBuilder.registerConvention(
                        conventionName, handlerClass);
                } catch (ClassNotFoundException cnfe) {
                    LogUtil.printException(log_,
                                           "Could not load convention class:"
                                           + className, cnfe);
                } catch (Exception exc) {
                    LogUtil.printException(log_, "Registering conventions",
                                           exc);
                }
            }
        }



    }

    /**
     * Set whether compression is used on DODS  file transfers
     *
     * @param compress true to compress
     */
    public static void setDODSCompression(boolean compress) {
        ucar.nc2.dods.DODSNetcdfFile.setAllowCompression(compress);
    }

    /**
     * Load the grib lookup tables
     *
     * @param resourceManager The resource manager
     * @deprecated  use loadIOSPResources(IdvResourceManager) instead
     */
    protected void loadGribResources(IdvResourceManager resourceManager) {
        loadIospResources(resourceManager);
    }

    /**
     * Load the grib lookup tables
     *
     * @param resourceManager The resource manager
     */
    protected void loadIospResources(IdvResourceManager resourceManager) {
        ucar.grib.GribResourceReader.setGribResourceReader(
            new ucar.grib.GribResourceReader() {
            public InputStream openInputStream(String resourceName)
                    throws IOException {
                try {
                    InputStream inputStream =
                        IOUtil.getInputStream(resourceName);
                    return inputStream;
                } catch (IOException ioe) {
                    //System.err.println ("IDV failed to read:" + resourceName);
                    return null;
                }
            }

        });

        ResourceCollection grib1Resources =
            resourceManager.getResources(
                IdvResourceManager.RSC_GRIB1LOOKUPTABLES);
        for (int i = 0; i < grib1Resources.size(); i++) {
            try {
                ucar.grib.grib1.GribPDSParamTable.addParameterUserLookup(
                    grib1Resources.get(i).toString());
            } catch (Exception exc) {
                //                System.err.println ("bad:"+ exc);
            }
        }
        ResourceCollection grib2Resources =
            resourceManager.getResources(
                IdvResourceManager.RSC_GRIB2LOOKUPTABLES);
        for (int i = 0; i < grib2Resources.size(); i++) {
            try {
                ucar.grib.grib2.ParameterTable.addParametersUser(
                    grib2Resources.get(i).toString());
            } catch (Exception exc) {
                //                System.err.println ("bad:"+ exc);
            }
        }
        ResourceCollection njResources =
            resourceManager.getResources(IdvResourceManager.RSC_NJCONFIG);
        StringBuilder errlog = new StringBuilder();
        for (int i = 0; i < njResources.size(); i++) {
            try {
                Object r = njResources.get(i);
                // System.out.println("resource = " + r);
                InputStream is = IOUtil.getInputStream(r.toString());
                if (is != null) {
                    ucar.nc2.util.xml.RuntimeConfigParser.read(is, errlog);
                }
            } catch (Exception exc) {
                // System.err.println ("bad config:"+ exc);
            }
        }
        ResourceCollection gempakParameters =
            resourceManager.getResources(
                IdvResourceManager.RSC_GEMPAKGRIDPARAMTABLES);
        for (int i = 0; i < gempakParameters.size(); i++) {
            try {
                String r = gempakParameters.get(i).toString();
                ucar.nc2.iosp.gempak.GempakGridParameterTable.addParameters(
                    r);
            } catch (Exception exc) {
                //                System.err.println ("bad:"+ exc);
            }
        }

    }


    /**
     * Initialize the cache
     */
    private void initCache() {
        File cacheDir =
            new File(
                IOUtil.joinDir(
                    dataContext.getObjectStore().getUserDirectory(),
                    "datacache"));
        IOUtil.makeDir(cacheDir);
        CacheManager.setCacheDir(cacheDir);
    }

    /**
     * Get this DataManager's {@link DataContext}.
     *
     * @return The {@link DataContext} of this DataManager.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Process the list of xml documents that define the different
     * {@link DataSource}s used within the idv.
     *
     * @param resources    The {@link ucar.unidata.xml.XmlResourceCollection}
     *                     that holds the set of datasource xml documents.
     *                     This may be null.
     */
    public void loadDataSourceXml(XmlResourceCollection resources) {
        if (resources == null) {
            return;
        }
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i);
            if (root == null) {
                continue;
            }
            String tag = root.getTagName();
            if (tag.equals(TAG_DATASOURCES)) {
                //Iterate through all of the "datasource" child tags of the given datasource node.
                NodeList children = XmlUtil.getElements(root, TAG_DATASOURCE);
                for (int j = 0; j < children.getLength(); j++) {
                    processDataSourceXml((Element) children.item(j));
                }
            } else if (tag.equals(TAG_DATASOURCE)) {
                processDataSourceXml(root);
            } else {
                LogUtil.printMessage("Unknown tag name in datasources.xml:"
                                     + tag);
            }
        }
    }

    /**
     * get an html listing of all of the data source descriptions
     *
     * @return html listing of data sources
     */
    public StringBuffer getDataSourceHtml() {
        StringBuffer html = new StringBuffer();
        try {
            List dataSources = getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                DataSource dataSource = (DataSource) dataSources.get(i);
                try {
                    html.append("\n<hr>");
                    html.append(dataSource.getFullDescription());
                } catch (Exception exc) {
                    html.append("Error getting datasource html:"
                                + dataSource.getName());
                }
            }
            html.append("\n<hr>");
        } catch (Exception exc) {
            html.append("Error getting datasource html");
        }
        return html;
    }

    /**
     * Create a snippet of the datasource xml for the given data source
     *
     * @param type The data source type
     * @param label the label
     * @param datasourceClass the class
     * @param properties properties
     *
     * @return The xml
     *
     * @throws Exception On badness
     */
    public static String getDatasourceXml(String type, String label,
                                          Class datasourceClass,
                                          Hashtable properties)
            throws Exception {
        return getDatasourceXml(type, label, datasourceClass, properties,
                                null);
    }


    /**
     * Create a snippet of the datasource xml for the given data source
     *
     * @param type The data source type
     * @param label the label
     * @param datasourceClass the class
     * @param properties properties
     * @param attributes If non-null then add these are xml attributes
     *
     * @return The xml
     *
     * @throws Exception On badness
     */
    public static String getDatasourceXml(String type, String label,
                                          Class datasourceClass,
                                          Hashtable properties,
                                          String[] attributes)
            throws Exception {
        Document doc  = XmlUtil.makeDocument();
        Element  root = doc.createElement(TAG_DATASOURCES);
        Element  node = XmlUtil.create(TAG_DATASOURCE, root);
        node.setAttribute(ATTR_ID, type);
        node.setAttribute(ATTR_LABEL, label);
        node.setAttribute(ATTR_FACTORY, datasourceClass.getName());
        node.setAttribute(ATTR_FILESELECTION, "true");
        if (properties != null) {
            for (java.util.Enumeration keys = properties.keys();
                    keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) properties.get(key);
                Element propNode = XmlUtil.create(doc, TAG_PROPERTY, node,
                                       value, new String[] { ATTR_NAME,
                        key });
            }
        }
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i += 2) {
                node.setAttribute(attributes[i],
                                  XmlUtil.encodeString(attributes[i + 1]));
            }
        }
        /*
<datasources>
  <datasource
     id="NetCDF.POINT.PAM"
     factory="ucar.unidata.data.point.NetcdfPointDataSource"
     ncmltemplate="/pam.ncml"
     fileselection="true"
     label="PAM Point Data files"/>
</datasources>
        */

        return XmlUtil.getHeader() + XmlUtil.toString(root);
    }




    /**
     * This method processes the given datasourceNode, creating a
     * {@link  DataSourceDescriptor} if one has not been created already.
     *
     * @param datasourceNode    the element for a data source
     */
    private void processDataSourceXml(Element datasourceNode) {

        try {
            String ids    = XmlUtil.getAttribute(datasourceNode, ATTR_ID);
            List   idList = StringUtil.split(ids);
            int    count  = 0;
            for (Iterator iter = idList.iterator(); iter.hasNext(); ) {
                count++;
                String id = ((String) iter.next()).toLowerCase();
                //We have already seen this datasource id
                if (idToDescriptor.get(id) != null) {
                    break;
                }

                String factory = XmlUtil.getAttribute(datasourceNode,
                                     ATTR_FACTORY);
                String patterns = XmlUtil.getAttribute(datasourceNode,
                                      ATTR_PATTERNS, (String) null);
                String label = XmlUtil.getAttribute(datasourceNode,
                                   ATTR_LABEL, "");
                //                System.out.println ("<tr><td>"+id + "</td><td>" + label+"</td></tr>");
                boolean fileSelection = XmlUtil.getAttribute(datasourceNode,
                                            ATTR_FILESELECTION, false);
                if (count > 1) {
                    fileSelection = false;
                }
                boolean doesMultiples = XmlUtil.getAttribute(datasourceNode,
                                            ATTR_DOESMULTIPLES, false);
                if ((label != null) && (label.length() > 0)) {
                    if (dataSourceNameMap.get(label) == null) {
                        dataSourceNameMap.put(label, label);
                        allDataSourceIds.add(new TwoFacedObject(label, id));
                    }
                }

                if (patterns != null) {
                    patterns = patterns.toLowerCase();
                    PatternFileFilter filter = null;
                    if (seenFilters.get(patterns) == null) {
                        seenFilters.put(patterns, patterns);
                        filter = new PatternFileFilter(patterns, (Object) id,
                                label);
                    }
                    if (filter != null) {
                        allFilters.add(filter);
                    }

                    if (fileSelection) {
                        //                        if ((label != null) && (label.length() > 0)) {
                        //                            fileDataSourceIds.add(new TwoFacedObject(label,
                        //                                    id));
                        //                        }
                        if (filter != null) {
                            fileFilters.add(filter);
                        }
                    }
                }

                if (fileSelection) {
                    if ((label != null) && (label.length() > 0)) {
                        fileDataSourceIds.add(new TwoFacedObject(label, id));
                    }
                }


                Hashtable properties = new Hashtable();
                NodeList props = XmlUtil.getElements(datasourceNode,
                                     TAG_PROPERTY);
                for (int propIdx = 0; propIdx < props.getLength();
                        propIdx++) {
                    Element propNode = (Element) props.item(propIdx);
                    if (XmlUtil.hasAttribute(propNode, ATTR_VALUE)) {
                        properties.put(XmlUtil.getAttribute(propNode,
                                ATTR_NAME), XmlUtil.getAttribute(propNode,
                                    ATTR_VALUE));
                    } else {
                        String value = XmlUtil.getChildText(propNode);
                        properties.put(XmlUtil.getAttribute(propNode,
                                ATTR_NAME), value);
                    }
                }
                DataSourceDescriptor descriptor =
                    new DataSourceDescriptor(id, label, this,
                                             Misc.findClass(factory),
                                             patterns, fileSelection,
                                             doesMultiples, properties);
                descriptor.setStandalone(XmlUtil.getAttribute(datasourceNode,
                        ATTR_STANDALONE, false));
                if (descriptor.getStandalone()) {
                    standaloneDescriptors.add(descriptor);
                }
                if (XmlUtil.hasAttribute(datasourceNode, ATTR_NCMLTEMPLATE)) {
                    descriptor.setNcmlTemplate(
                        XmlUtil.getAttribute(
                            datasourceNode, ATTR_NCMLTEMPLATE));
                }
                descriptors.add(descriptor);
                idToDescriptor.put(id, descriptor);
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "Processing data source", exc);
        }

    }


    /**
     * get the data source descriptors
     *
     * @return the descriptors
     */
    public List<DataSourceDescriptor> getDescriptors() {
        return descriptors;
    }

    /**
     * get the data source descriptors for those data sources that can be stand-alone
     *
     * @return stand alone descriptors
     */
    public List<DataSourceDescriptor> getStandaloneDescriptors() {
        return standaloneDescriptors;
    }



    /**
     * This is used by the {@link ucar.unidata.idv.chooser.IdvChooserManager} to allow
     * the user to create new file data chooser items.
     *
     * @return The list of {@link DataSourceDescriptor}s that are meant
     *         to be used for files.
     */
    public List getFileDataSourceList() {
        return fileDataSourceIds;
    }


    /**
     * This is used by the {@link ucar.unidata.idv.chooser.IdvChooserManager}
     *
     * @return The list of {@link DataSourceDescriptor}s
     */
    public List getAllDataSourceIds() {
        return allDataSourceIds;
    }

    /**
     * The DataManager holds a set of active {@link DataSource} objects.
     *
     * @return The list of active {@link DataSource}s.
     */
    public ArrayList getDataSources() {
        return dataSources;
    }



    /**
     * Is the given {@link  DataSource} currently in the list of active
     * datasources.
     *
     * @param dataSource The {@link  DataSource} to check.
     *
     * @return Is dataSource in the datasources list.
     */
    public boolean haveDataSource(DataSource dataSource) {
        return dataSources.contains(dataSource);
    }

    /**
     * Add the given {@link  DataSource} into the list of datasources if it
     * is not in the list and if it is not in error.
     *
     * @param dataSource  The {@link  DataSource} to add into the list.
     * @return True if we added this data source, false if we already have this data source
     */
    public boolean addDataSource(DataSource dataSource) {
        if ( !haveDataSource(dataSource) && !dataSource.getInError()) {
            //Add it to the list here because the dataContext might ask us for the list
            dataSources.add(dataSource);
            if ( !dataContext.loadDataSource(dataSource)) {
                removeDataSource(dataSource);
                return false;
            }
            return true;
        }
        return false;
    }


    /**
     * Reload all the data sources
     */
    public void reloadAllDataSources() {
        LogUtil.message("Reloading all data");
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            dataSource.reloadData();
        }
    }


    /**
     * As the method name implies, remove all datasources managed by this
     * DataManager.
     */
    public void removeAllDataSources() {
        List tmp = new ArrayList(dataSources);
        for (int i = 0; i < tmp.size(); i++) {
            removeDataSource((DataSource) tmp.get(i));
        }
        dataSources                = new ArrayList();
        dataSourceToDefiningObject = new Hashtable();
        definingObjectToDataSource = new Hashtable();
    }


    /**
     * Remove the given {@link  DataSource} from the list of datasources.
     *
     * @param dataSource  The {@link  DataSource} to remove.
     */
    public void removeDataSource(DataSource dataSource) {
        dataSources.remove(dataSource);
        dataSource.doRemove();

        Object definingObject = dataSourceToDefiningObject.get(dataSource);
        if (definingObject != null) {
            definingObjectToDataSource.remove(definingObject);
            dataSourceToDefiningObject.remove(dataSource);
        }

    }

    /**
     * The DataManager creates a list of
     * {@link  ucar.unidata.util.PatternFileFilter}s from the datasource xml.
     *
     * @return The list of {@link  ucar.unidata.util.PatternFileFilter}s.
     */
    public ArrayList getFileFilters() {
        return fileFilters;
    }



    /**
     * Class DataType - internal class for holding a DataType
     */
    private class DataType {

        /** ID for the DataType */
        String dataTypeId;

        /** The defining object for this type */
        Object definingObject;

        /** The properties to use for this data */
        Hashtable properties;


        /**
         * Create a new DataType
         *
         * @param dataTypeId          data type id
         * @param definingObject      defining object
         */
        public DataType(String dataTypeId, Object definingObject) {
            this(dataTypeId, definingObject, null);
        }

        /**
         * Create a new DataType
         *
         * @param dataTypeId          data type id
         * @param definingObject      defining object
         * @param properties The properties
         */
        public DataType(String dataTypeId, Object definingObject,
                        Hashtable properties) {
            this.dataTypeId     = dataTypeId;
            this.definingObject = definingObject;
            this.properties     = properties;
        }

        /**
         * Check if there is an error with this type
         *
         * @return  true if in error
         */
        public boolean isInError() {
            return (dataTypeId == null);
        }

        /**
         * to string
         *
         * @return  to string
         */
        public String toString() {
            return "data type: " + dataTypeId;

        }

    }

    /**
     * Get the actual current  descriptor with the id of the given or return the given one if one is not found. We use this for data sources that
     * are unpersisted so they can get an updated descriptor
     *
     * @param dds Given
     *
     * @return Current
     */
    public DataSourceDescriptor getCurrent(DataSourceDescriptor dds) {
        if (dds == null) {
            return null;
        }
        for (int i = 0; i < descriptors.size(); i++) {
            DataSourceDescriptor descriptor =
                (DataSourceDescriptor) descriptors.get(i);
            if (descriptor.equals(dds)) {
                return descriptor;
            }
        }
        return dds;
    }


    /**
     * Check to see if this list of defining objects can handle
     * multiple objects
     *
     * @param definingObjects    defining objects
     * @param result             resulting list
     * @param filters            filters
     * @param properties the properties
     */
    private void checkMultiples(List definingObjects, List result,
                                List filters, Hashtable properties) {

        //Now make sure the list contains strings
        if ( !Misc.allStrings(definingObjects)) {
            return;
        }
        boolean didone = true;
        while (didone && (definingObjects.size() > 1)) {
            didone = false;
            for (int i = 0; i < descriptors.size(); i++) {
                DataSourceDescriptor descriptor =
                    (DataSourceDescriptor) descriptors.get(i);
                if ( !descriptor.getDoesMultiples()) {
                    continue;
                }
                PatternFileFilter filter = descriptor.getPatternFileFilter();
                if (filter == null) {
                    continue;
                }
                List workingList = null;
                List tmpList     = new ArrayList(definingObjects);

                for (int stringIdx = 0; stringIdx < tmpList.size();
                        stringIdx++) {
                    String  s     = (String) tmpList.get(stringIdx);
                    boolean match = filter.match(s);
                    if (match) {
                        //                        System.err.println("match:" + descriptor);
                        definingObjects.remove(s);
                        if (workingList == null) {
                            workingList = new ArrayList();
                        }
                        workingList.add(s);
                    }
                }
                if (workingList != null) {
                    Hashtable props = null;
                    if (properties != null) {
                        props = (Hashtable) properties.get(
                            DataSource.PROP_SUBPROPERTIES + result.size());
                    }
                    result.add(new DataType(descriptor.getId(), workingList,
                                            props));
                    didone = true;
                }
            }
        }

        for (int stringIdx = 0; stringIdx < definingObjects.size();
                stringIdx++) {
            String s        = (String) definingObjects.get(stringIdx);
            String dataType = findDataType(s, filters);
            if (dataType != null) {
                Hashtable props = null;
                if (properties != null) {
                    props = (Hashtable) properties.get(
                        DataSource.PROP_SUBPROPERTIES + result.size());
                }
                result.add(new DataType(dataType, s, props));
            } else {
                //In error
                result.add(new DataType(null, s));
            }
        }
    }



    /**
     * Look up the named data source type based on the suffix of the data
     * location.
     *
     * @param definingObject     the defining object for the Type
     * @param filters            list of filters
     * @param returnErrorType If not true return null. Else return an empty datatype
     * @param properties the properties
     * @return   List of DataTypes
     */
    private List getDataTypes(Object definingObject, List filters,
                              boolean returnErrorType, Hashtable properties) {
        //Check for arity
        List result = new ArrayList();
        if ((definingObject instanceof List)
                && ((List) definingObject).size() > 0) {
            checkMultiples((List) definingObject, result, filters,
                           properties);
        }

        if (definingObject instanceof String) {
            String dataType = findDataType((String) definingObject, filters);
            if (dataType != null) {
                result.add(new DataType(dataType, definingObject));
            } else {
                if ( !returnErrorType) {
                    return null;
                }
                //In error
                result.add(new DataType(null, definingObject));
            }
        }
        return result;
    }


    /**
     * Find the data type.
     *
     * @param definingObject    defining object for the type
     * @param filters           List of filters
     * @return  the type or null
     */
    private String findDataType(String definingObject, List filters) {
        String file            = definingObject.trim().toLowerCase();
        int    questionMarkIdx = file.indexOf("?");
        String substring       = null;
        if (questionMarkIdx >= 0) {
            substring = file.substring(0, questionMarkIdx);
        }
        for (int i = 0; i < filters.size(); i++) {
            PatternFileFilter filter = (PatternFileFilter) filters.get(i);
            boolean           match  = filter.match(file);
            if ( !match && (substring != null)) {
                match = filter.match(substring);
            }
            if (match) {
                return filter.getId().toString();
            }
        }

        if (IOUtil.isHtmlFile(file)) {
            return "FILE.TEXT";
        }
        return null;
    }


    /**
     * Is there a mapping defined from the given definingObject
     * (which should be a String) to a {@link  DataSourceDescriptor}.
     *
     * @param definingObject          This is the object passed in when we
     *                                try to create a {@link  DataSource}.
     * @return Do we know how to handle the given definingObject.
     */
    public boolean validDatasourceId(Object definingObject) {
        List dataTypes = getDataTypes(definingObject, allFilters, true, null);
        if ((dataTypes == null) || (dataTypes.size() == 0)
                || dataTypes.get(0).equals(DATATYPE_UNKNOWN)) {
            return false;
        }
        return true;
    }

    /**
     * Is there a mapping defined from the given definingObject (which
     * should be a String) to a {@link  DataSourceDescriptor} or is there a
     * DATATYPE_ID entry in the given properties Hashtable.
     *
     * @param definingObject     This is the object passed in when we try to
     *                           create a {@link  DataSource}.
     * @param properties         May hold a DATATYPE_ID entry.
     * @return Do we know how to handle the given definingObject.
     */
    public boolean validDatasourceId(Object definingObject,
                                     Hashtable properties) {
        if ((properties != null) && (properties.get(DATATYPE_ID) != null)) {
            return true;
        }
        return validDatasourceId(definingObject);
    }


    /**
     * Lookup and return the {@link  DataSource} that was created with the
     * given definingObject.
     *
     * @param definingObject The object used to create a DataSource.
     * @param lookupKey What we use to lookup the data source in the
     * definingObjectToDataSource map.
     *
     * @return The DataSource created by the definingObject or null.
     */
    private DataSource findDataSource(Object definingObject,
                                      Object lookupKey) {
        DataSource dataSource =
            (DataSource) definingObjectToDataSource.get(lookupKey);
        if (dataSource != null) {
            return dataSource;
        }
        for (int i = 0; i < dataSources.size(); i++) {
            dataSource = (DataSource) dataSources.get(i);
            if (dataSource.identifiedBy(definingObject)) {
                return dataSource;
            }
        }
        return null;
    }



    /**
     * Find the data source with the given name
     *
     * @param name The name. This is passed to DataSource.identifiedBy() method. It can be 'class:some_class' or a regexp pattern that matches on the data source name
     *
     * @return the data source or null if none found
     */
    public DataSource findDataSource(String name) {
        if (name == null) {
            name = "#0";
        }
        if (name.startsWith("#")) {
            int index = new Integer(name.substring(1).trim()).intValue();
            if ((index >= 0) && (index < dataSources.size())) {
                return dataSources.get(index);
            }
            return null;
        }

        //First check identifiedBy
        for (DataSource dataSource : dataSources) {
            if (dataSource.identifiedBy(name)) {
                return dataSource;
            }
        }

        //next  check identifiedByName
        for (DataSource dataSource : dataSources) {
            if (dataSource.identifiedByName(name)) {
                return dataSource;
            }
        }
        return null;
    }



    /**
     * Create a {@link  DataSource} (if we know how) defined with the given
     * definingObject.
     *
     * @param definingObject       This is the data used to create a DataSource.
     *                             It may be a String (e.g., a URL, a filename)
     *                             or something else (e.g., a list of URLs).
     *
     * @return The list of {@link DataSource}s defined by the given
     *         definingObject  or null.
     */
    public DataSourceResults createDataSource(Object definingObject) {
        return createDataSource(definingObject, (Hashtable) null);
    }

    /**
     * Create a {@link  DataSource} (if we know how) defined with the given
     * definingObject and set of properties (which may be null).
     *
     * @param definingObject      This is the data used to create a DataSource.
     *                            It may be a String (e.g., a URL, a filename)
     *                            or something else (e.g., a list of URLs).
     * @param properties The properties for the new DataSource.
     *
     * @return The list of {@link  DataSource} defined by the given
     *         definingObject  or null.
     */
    public DataSourceResults createDataSource(Object definingObject,
            Hashtable properties) {
        return createDataSource(definingObject, null, properties);
    }


    /**
     * Ask the user for the data soruce type and create the given data source
     *
     * @param definingObject      This is the data used to create a DataSource.
     *                            It may be a String (e.g., a URL, a filename)
     *                            or something else (e.g., a list of URLs).
     * @param properties          The properties for the new DataSource.
     */
    public void createDataSourceAndAskForType(Object definingObject,
            Hashtable properties) {
        String dataType = dataContext.selectDataType(definingObject);
        if (dataType == null) {
            return;
        }
        while (true) {
            try {
                DataSourceResults results = createDataSource(definingObject,
                                                dataType, properties);
                if ( !results.anyFailed()) {
                    return;
                }
            } catch (BadDataException bde) {}
            dataType = dataContext.selectDataType(
                definingObject,
                "<html>Failed to load the data as the given type. Try again?</html>");
            if (dataType == null) {
                return;
            }
        }
    }



    /**
     * Create a {@link  DataSource} (if we know how) defined with the given
     * dataType (if non-null) or by the given definingObject and set of
     * properties (which may be null).
     *
     * @param definingObject      This is the data used to create a DataSource.
     *                            It may be a String (e.g., a URL, a filename)
     *                            or something else (e.g., a list of URLs).
     * @param dataType            The id of the {@link  DataSourceDescriptor}
     *                            (or null).
     * @param properties          The properties for the new DataSource.
     * @return The list of {@link  DataSource} defined by the given
     *         definingObject  or null.
     */
    public DataSourceResults createDataSource(Object definingObject,
            String dataType, Hashtable properties) {


        if ((dataType != null) && (dataType.length() == 0)) {
            dataType = null;
        }
        if ((dataType == null) && (properties != null)) {
            dataType = (String) properties.get("idv.datatype");
        }
        if ((dataType == null) && (definingObject instanceof String)) {
            String file = (String) definingObject;
            if (file.startsWith("type:")) {
                file = file.substring(5);
                int index = file.indexOf(":");
                if (index >= 0) {
                    dataType       = file.substring(0, index);
                    definingObject = file.substring(index + 1);
                }
            }
        }

        //Initialize the properties if it is null
        if (properties == null) {
            properties = new Hashtable();
        }

        //See if there is a data type in the properties file
        if (dataType == null) {
            dataType = (String) properties.get(DATATYPE_ID);
        }

        Object[] lookupKey = new Object[] { definingObject, dataType };

        //Do we already have this dataSource loaded
        DataSource existingDataSource = findDataSource(definingObject,
                                            lookupKey);
        if (existingDataSource != null) {
            //If we already have one then do a reload on it
            existingDataSource.reloadData();
            return new DataSourceResults(existingDataSource, definingObject);
        }

        List dataTypes = null;
        if (dataType != null) {
            if ((definingObject instanceof List)
                    && Misc.allStrings((List) definingObject)) {
                DataSourceDescriptor dsd = getDescriptor(dataType);
                if ((dsd != null) && !dsd.getDoesMultiples()) {
                    List sources = (List) definingObject;
                    dataTypes = new ArrayList(sources.size());
                    for (int i = 0; i < sources.size(); i++) {
                        dataTypes.add(
                            new DataType(
                                dataType, sources.get(i),
                                (Hashtable) properties.get(
                                    DataSource.PROP_SUBPROPERTIES
                                    + dataTypes.size())));
                    }
                } else {
                    dataTypes = Misc.newList(new DataType(dataType,
                            definingObject));
                }

            } else {
                dataTypes = Misc.newList(new DataType(dataType,
                        definingObject));
            }
        }


        //See if we can find the data type from the definingObject (e.g., String pattern matching)
        if (dataTypes == null) {
            dataTypes = getDataTypes(definingObject, allFilters, false,
                                     properties);
        }
        DataSourceResults results = new DataSourceResults();
        if ((dataTypes == null) || (dataTypes.size() == 0) || (definingObject instanceof ArrayList)) {
            //If it is a string then let's just try the VisadDataSource
            if (definingObject instanceof String) {
                String overrideDataType =
                    dataContext.selectDataType(definingObject);
                if (overrideDataType != null) {
                    dataTypes = Misc.newList(new DataType(overrideDataType,
                            definingObject));
                } else {
                    return results;
                    // dataTypes = Misc.newList("FILE.ANY");
                }
            /* If it is an ArrayList (multiple files selected), then
              we need to crack into each member to try the string test
              from above (if string then try the as a VisadDataSource) */
            } else if (definingObject instanceof ArrayList) {
                if (((ArrayList) definingObject).size() > 1) {
                    if (dataTypes.get(0).toString().contains("null")) {
                        String overrideDataType =
                            dataContext.selectDataType(((ArrayList) definingObject).get(0));
                        if (overrideDataType != null) {
                            for (int i = 0; i < dataTypes.size(); i++) {
                                dataTypes.set(i,new DataType(overrideDataType,
                                definingObject));
                            }
                        }
                    }
                }
            } else {
                results.addFailed(
                    definingObject,
                    new BadDataException(
                        "Do not know how to handle the given data: "
                        + definingObject));
                return results;
            }
        }


        for (int i = 0; i < dataTypes.size(); i++) {
            DataType type = (DataType) dataTypes.get(i);
            if (type.isInError()) {
                results.addFailed(
                    type.definingObject,
                    new BadDataException(
                        "Do not know how to handle the given data: "
                        + type.definingObject));
                break;
            }
            dataType       = type.dataTypeId;
            definingObject = type.definingObject;
            DataSourceDescriptor descriptor = getDescriptor(dataType);
            if (descriptor == null) {
                results.addFailed(
                    type.definingObject,
                    new BadDataException(
                        "No factory for dataType: " + dataType));
                break;
            }

            LogUtil.consoleMessage("Loading in data source: "
                                   + descriptor.getLabel());
            LogUtil.message("Loading in data source: "
                            + descriptor.getLabel());

            try {
                Class factoryClass = descriptor.getFactoryClass();
                if (factoryClass == null) {
                    results.addFailed(
                        type.definingObject,
                        new BadDataException(
                            "No factory for dataType: " + dataType));
                    break;
                }


                Constructor ctor = Misc.findConstructor(factoryClass,
                                       new Class[] {
                                           DataSourceDescriptor.class,
                                           definingObject.getClass(),
                                           Hashtable.class });
                if (ctor == null) {
                    results.addFailed(
                        type.definingObject,
                        new IllegalArgumentException(
                            "No constructor found for class:"
                            + factoryClass.getName() + " data:"
                            + definingObject.getClass()));
                    break;
                }


                Hashtable propertiesToUse;
                if (type.properties != null) {
                    propertiesToUse = type.properties;
                } else {
                    propertiesToUse = properties;
                }
                DataSourceFactory factory =
                    (DataSourceFactory) ctor.newInstance(new Object[] {
                        descriptor,
                        definingObject, propertiesToUse });
                DataSource dataSource = factory.getDataSource();

                if (dataSource != null) {
                    dataSource.initAfterCreation();
                    //Ask for the data choices as one way to see if this datasource is ok
                    List choices = dataSource.getDataChoices();
                    if (dataSource.getInError()) {
                        if (dataSource.getNeedToShowErrorToUser()) {
                            results.addFailed(
                                definingObject,
                                new BadDataException(
                                    dataSource.getErrorMessage()));
                        }
                        break;
                    } else {
                        if (addDataSource(dataSource)) {
                            lookupKey = new Object[] { definingObject,
                                    dataType };
                            definingObjectToDataSource.put(lookupKey,
                                    dataSource);
                            dataSourceToDefiningObject.put(dataSource,
                                    lookupKey);
                        } else if (haveDataSource(dataSource)) {
                            int idx = dataSources.indexOf(dataSource);
                            if (idx >= 0) {
                                ((DataSource) dataSources.get(
                                    idx)).reloadData();
                            }
                        }
                    }
                }
                results.addSuccess(dataSource, definingObject);
            } catch (java.lang.IllegalArgumentException iae) {
                results.addFailed(definingObject,
                                  new BadDataException("Cannot open file: "
                                      + definingObject, iae));
                break;
            } catch (java.lang.reflect.InvocationTargetException ite) {
                results.addFailed(
                    definingObject,
                    new BadDataException(
                        "Error creating data source:" + dataType + " with: "
                        + definingObject + "\n", ite.getTargetException()));
                break;
            } catch (WrapperException wexc) {
                results.addFailed(
                    definingObject, new BadDataException(
                        "Error creating data source:" + dataType + " with: "
                        + definingObject + "\n" + wexc.getMessage()
                        + "\n", wexc.getException()));
                break;
            } catch (Throwable exc) {
                results.addFailed(
                    definingObject,
                    new BadDataException(
                        "Error creating data source:" + dataType + " with: "
                        + definingObject + "\n", exc));
                break;
            }
        }
        return results;
    }



    /**
     * Seed the given encoder with the {@link  DataSourceDescriptor}s and the
     * DataManager object.
     *
     * @param encoder   The encoder to seed.
     * @param forRead   Is this encoding for a read or a write.
     */
    public void initEncoder(XmlEncoder encoder, boolean forRead) {
        encoder.defineObjectId(this, "datamanager");

        //If this is for a read then we predefine the DataSourceDescriptors
        //to get around a legacy change where we now don't seed the XmlEncoder
        //with the descriptors.
        if (forRead) {
            for (int i = 0; i < descriptors.size(); i++) {
                DataSourceDescriptor descriptor =
                    (DataSourceDescriptor) descriptors.get(i);
                encoder.defineObjectId(descriptor,
                                       "DESC:" + descriptor.getId());
                //For backward compatibility.
                encoder.defineObjectId(descriptor,
                                       "DESC:"
                                       + descriptor.getId().toUpperCase());
                encoder.defineObjectId(descriptor,
                                       "desc:" + descriptor.getId());

            }
        }
        //Seed the encoder with the pre-existing data sources
        for (int i = 0; i < dataSources.size(); i++) {
            encoder.addSeedObject(dataSources.get(i));
        }
    }

    /**
     * Find the {@link  DataSourceDescriptor} with the given dataType id.
     *
     * @param  dataType     The dataType id to lookup.
     *
     * @return The DataSourceDescriptor defined by the given dataType (or null).
     */
    public DataSourceDescriptor getDescriptor(String dataType) {
        if (dataType == null) {
            return null;
        }
        return (DataSourceDescriptor) idToDescriptor.get(
            dataType.toLowerCase());
    }

    /**
     * Find the given property on the {@link  DataSourceDescriptor} defined
     * by the given dataType.
     *
     * @param dataType             The data source descriptor id.
     * @param property             The property name.
     * @return The property or null.
     */
    public String getProperty(String dataType, String property) {
        if (dataType == null) {
            return null;
        }
        DataSourceDescriptor descriptor = getDescriptor(dataType);
        if (descriptor == null) {
            return null;
        }
        return descriptor.getProperty(property);
    }

    /**
     * Find the given property on the {@link  DataSourceDescriptor} defined
     * by the given dataType. If not found then return the given dflt.
     *
     *  @param dataType         The data source descriptor id.
     *  @param property         The property name.
     *  @param dflt             The default value.
     *  @return The property or the dflt parameter.
     */
    public boolean getProperty(String dataType, String property,
                               boolean dflt) {
        String v = getProperty(dataType, property);
        if (v == null) {
            return dflt;
        }
        return new Boolean(v).booleanValue();
    }


    /**
     *  Add in the AddeURLStreamHandler
     */
    public void initURLStreamHandlers() {
        try {
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                public URLStreamHandler createURLStreamHandler(
                        String protocol) {
                    if (protocol.equalsIgnoreCase("adde")) {
                        return new edu.wisc.ssec.mcidas.adde
                            .AddeURLStreamHandler();
                    } else if (protocol.equalsIgnoreCase(
                            PluginManager.PLUGIN_PROTOCOL)) {
                        return new URLStreamHandler() {
                            protected URLConnection openConnection(URL url) {
                                try {
                                    return IOUtil.getURL(url.getPath(),
                                            getClass()).openConnection();
                                } catch (Exception exc) {
                                    LogUtil.logException(
                                        "Handling idvresource", exc);
                                    return null;
                                }
                            }
                        };

                    } else {
                        return null;
                    }
                }
            });
        } catch (Throwable exc) {}
    }

    /**
     * Check if an object is a data source that holds formulas.
     *
     * @param s   object to check
     * @return  true if it is
     */
    public static boolean isFormulaDataSource(Object s) {
        return (s instanceof DescriptorDataSource);
    }


    /**
     * test main
     *
     * @param args cmd line args
     *
     * @throws Exception On error
     */
    public static void main(String[] args) throws Exception {

        Trace.startTrace();
        Unit     unit     = DataUtil.parseUnit("k");
        RealType realType = DataUtil.makeRealType("temp", unit);
        Trace.call1("start");
        for (int i = 0; i < 2000000; i++) {
            Real r = new Real(realType, 100, unit);
        }

        Trace.call2("start");



    }





}
