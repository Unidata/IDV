/*
 * $Id: ThreddsHandler.java,v 1.68 2007/07/09 22:59:58 jeffmc Exp $
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



package ucar.unidata.util;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.ui.XmlTree;


import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;



/**
 * A set of utilities for dealing with Thredds catalogs
 *
 * @author IDV development team
 * @version $Revision: 1.68 $Date: 2007/07/09 22:59:58 $
 */

public class CatalogUtil {


    /** This needs to be the same as unidata.data.DataSource.PROP_TITLE */
    public final static String PROP_TITLE = "TITLE";

    /** This needs to be the same as unidata.data.DataSource */
    public static final String PROP_SERVICE_HTTP = "prop.service.http";

    /** Property name for the url of the catalog */
    public static final String PROP_CATALOGURL = "Thredds.CatalogUrl";

    /** Property name for the   data set id */
    public static final String PROP_DATASETID = "Thredds.DataSetId";

    /** Property name for the  data set group */
    public static final String PROP_DATASETGROUP = "Thredds.DataGroup";

    /** Property name for the  annotations server url */
    public static final String PROP_ANNOTATIONSERVER =
        "Thredds.AnnotationServer";

    /** Xml attribute name for the url where the doc came from */
    public static final String ATTR_CATALOGURL = "catalogurl";


    /** Xml attribute name for the data set group */
    public static final String ATTR_DATASETGROUP = "group";

    /** Xml attribute name for the data set id */
    public static final String ATTR_DATASETID = "id";


    public static final String ATTR_METADATATYPE = "metadataType";




    /** Xml attribute name for the data set id */
    public static final String VALUE_ANNOTATIONSERVER = "annotationServer";


    /** Xml attribute value for the summary documentation */
    public static final String VALUE_SUMMARY = "summary";

    /** Xml attribute value for the rights documentation */
    public static final String VALUE_RIGHTS = "rights";



    /** More clear than  then doing (String)null */
    public static final String NULL_STRING = null;

    /**
     * Service name of the special resolver service.
     * If a data set has a resolver service then the url
     * of the data set actually points to a resolver
     * service which will give back a catalog that contains
     * the actual data set
     */
    public static final String SERVICE_RESOLVER = "Resolver";

    /** Service type value for the compound service */
    public static final String SERVICE_COMPOUND = "Compound";

    /** Service type value for the compound service */
    public static final String SERVICE_FILE = "FILE";

    /** Service type value for the wcs service */
    public static final String SERVICE_HTTP = "HTTPServer";

    /** Service type value for the dods service */
    public static final String SERVICE_DODS = "DODS";

    /** Service type value for the adde service */
    public static final String SERVICE_ADDE = "ADDE";

    /** Service type value for the OPeNDAP service */
    public static final String SERVICE_OPENDAP = "OPENDAP";

    /** Value for the thredds catalog v0.4 */
    public static final double THREDDS_VERSION_0_4 = 0.4;

    /** Value for the thredds catalog v0.5 */
    public static final double THREDDS_VERSION_0_5 = 0.5;

    /** Value for the thredds catalog v0.6 */
    public static final double THREDDS_VERSION_0_6 = 0.6;

    /** Value for the thredds catalog v1.0 */
    public static final double THREDDS_VERSION_1_0 = 1.0;

    /** xml tag name */
    public static final String TAG_LATLONBOX = "LatLonBox";

    /** xml tag name */
    public static final String TAG_NORTH = "north";

    /** xml tag name */
    public static final String TAG_SOUTH = "south";

    /** xml tag name */
    public static final String TAG_EAST = "east";

    /** xml tag name */
    public static final String TAG_WEST = "west";

    /** name */
    public static final String TAG_NAME = "name";

    /** contact */
    public static final String TAG_CONTACT = "contact";


    /** _more_ */
    public static final String TAG_GEOSPATIALCOVERAGE = "geospatialCoverage";

    /** _more_ */
    public static final String TAG_TIMECOVERAGE = "timeCoverage";

    /** _more_ */
    public static final String TAG_START = "start";

    /** _more_ */
    public static final String TAG_END = "end";

    /** _more_ */
    public static final String TAG_DATASIZE = "dataSize";

    /** _more_ */
    public static final String TAG_DATE = "date";


    /** Tag name for the xml node &quot;access&quot; */
    public static final String TAG_ACCESS = "access";

    /** Tag name for the xml node &quot;documentation&quot; */
    public static final String TAG_DOCUMENTATION = "documentation";

    /** Tag name for the xml node &quot;docparent&quot; */
    public static final String TAG_DOCPARENT = "docparent";

    /** Tag name for the xml node &quot;catalog&quot; */
    public static final String TAG_CATALOG = "catalog";

    /** Tag name for the xml node &quot;catalogRef&quot; */
    public static final String TAG_CATALOGREF = "catalogRef";

    /** Tag name for the xml node &quot;collection&quot; */
    public static final String TAG_COLLECTION = "collection";

    /** Tag name for the xml node &quot;dataset&quot; */
    public static final String TAG_DATASET = "dataset";

    /** Tag name for the xml node &quot;dataType&quot; */
    public static final String TAG_DATATYPE = "dataType";

    /** Tag name for the xml node &quot;metadata&quot; */
    public static final String TAG_METADATA = "metadata";

    /** Tag name for the xml node &quot;queryCapability&quot; */
    public static final String TAG_QUERYCAPABILITY = "queryCapability";

    /** Tag name for the xml node &quot;server &quot; */
    public static final String TAG_SERVER = "server";

    /** Tag name for the xml node &quot;service &quot; */
    public static final String TAG_SERVICE = "service";

    /** Tag name for the xml node &quot;serviceName&quot; */
    public static final String TAG_SERVICENAME = "serviceName";

    /** Attribute name for the xml node &quot;units&quot; */
    public static final String ATTR_UNITS = "units";

    /** Tag name for the xml node &quot;property&quot; */
    public static final String TAG_PROPERTY = "property";

    /** Attribute name for the xml attribute &quot;action &quot; */
    public static final String ATTR_ACTION = "action";

    /** Attribute name for the xml attribute &quot;dataType &quot; */
    public static final String ATTR_DATATYPE = "dataType";

    /** Attribute name for the xml attribute &quot;base &quot; */
    public static final String ATTR_BASE = "base";

    /** Attribute name for the xml attribute &quot;ID &quot; */
    public static final String ATTR_ID = "ID";

    /** Attribute name for the xml attribute &quot;inherited &quot; */
    public static final String ATTR_INHERITED = "inherited";

    /** Attribute name for the xml attribute &quot;name &quot; */
    public static final String ATTR_NAME = "name";

    /** Attribute name for the xml attribute &quot;value &quot; */
    public static final String ATTR_VALUE = "value";

    /** Attribute name for the xml attribute &quot;serverID &quot; */
    public static final String ATTR_SERVERID = "serverID";

    /** Attribute name for the xml attribute &quot;serviceName &quot; */
    public static final String ATTR_SERVICENAME = "serviceName";

    /** Attribute name for the xml attribute &quot;serviceType &quot; */
    public static final String ATTR_SERVICETYPE = "serviceType";

    /** Attribute name for the xml attribute &quot;suffix &quot; */
    public static final String ATTR_SUFFIX = "suffix";

    /** Attribute name for the xml attribute &quot;type &quot; */
    public static final String ATTR_TYPE = "type";

    /** Attribute name for the xml attribute &quot;url &quot; */
    public static final String ATTR_URL = "url";

    /** Attribute name for the xml attribute &quot;urlPath &quot; */
    public static final String ATTR_URLPATH = "urlPath";

    /** Attribute name for the xml attribute &quot;version &quot; */
    public static final String ATTR_VERSION = "version";

    /** Attribute name for the xml attribute &quot;xlink:href &quot; */
    public static final String ATTR_XLINK_HREF = "xlink:href";

    /** Attribute name for the xml attribute &quot;xlink:title &quot; */
    public static final String ATTR_XLINK_TITLE = "xlink:title";



    /**
     * A utiliry to get the version from the catalog root.
     *
     *
     * @param node The xml node
     *
     * @return The version
     */
    public static double getVersion(Element node) {
        if ( !XmlUtil.hasAttribute(node, ATTR_VERSION)) {
            Node parent = node.getParentNode();
            if ((parent == null) || !(parent instanceof Element)) {
                return THREDDS_VERSION_1_0;
            }
            return getVersion((Element) parent);
        }

        String version = XmlUtil.getAttribute(node, ATTR_VERSION,
                             String.valueOf(THREDDS_VERSION_1_0));
        while (version.indexOf(".") != version.lastIndexOf(".")) {
            version = version.substring(0, version.lastIndexOf("."));
        }
        return new Double(version).doubleValue();
    }



    public static boolean haveChildDatasets(Element node) {
        return XmlUtil.findChild(node, TAG_DATASET)!=null;
    }

    public static boolean haveChildCatalogs(Element node) {
        return XmlUtil.findChild(node, TAG_CATALOGREF)!=null;
    }



    /**
     *  Find the service type attribute for the given service node. This is thredds version
     *  specific - looking for either "servicetype" attribute or "type" attr.
     *
     *  @param serviceNode The service node to look for the service type.
     *  @return The service type attribute or null if not found.
     */
    public static String getServiceType(Element serviceNode) {
        String serviceType = XmlUtil.getAttribute(serviceNode,
                                 ATTR_SERVICETYPE, NULL_STRING);
        if (serviceType == null) {
            //Maybe  version 0.4
            serviceType = XmlUtil.getAttribute(serviceNode, ATTR_TYPE,
                    NULL_STRING);
        }
        return serviceType;
    }

    /**
     * Find the data type attribute for the given service node.
     *
     * @param datasetNode The dataset node to look for the data type.
     * @return The dataType attribute or null if not found.
     */
    private static String getDataType(Element datasetNode) {
        String dataType = XmlUtil.getAttribute(datasetNode, ATTR_DATATYPE,
                              NULL_STRING);
        if (dataType == null) {}
        return dataType;
    }

    /**
     *  Assemble the String title for the given dataset. We look for the first two
     *  "name" attributes in the xml tree and concatenate them (If found).
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @return The title for this dataset node. (may be null).
     */


    public static String getTitleFromDataset(Element datasetNode) {
        Hashtable tags = Misc.newHashtable(TAG_DATASET, TAG_DATASET,
                                           TAG_COLLECTION, TAG_COLLECTION);
        List titleAttrs = XmlUtil.getAttributesFromTree(datasetNode,
                              ATTR_NAME, tags);
        String title = null;
        if ((titleAttrs != null) && (titleAttrs.size() >= 1)) {
	    //Don't use more than one for now
            if (false && titleAttrs.size() >= 2) {
                String t1 = titleAttrs.get(titleAttrs.size() - 2).toString().replace("_"," ");
                String t2 = titleAttrs.get(titleAttrs.size() - 1).toString().replace("_"," ");
                //If the first 8 characters are the same then just use the name
                if ((t1.length() > 8) && (t2.length() > 8)
                        && t1.substring(0, 8).equals(t2.substring(0, 8))) {
                    title = t2;
                } else {
                    title = t1 + " " + t2;
                }
            } else {
                title = titleAttrs.get(titleAttrs.size() - 1).toString().replace("_"," ");
            }
        }
	if(title!=null) {
	    String ext = IOUtil.getFileExtension(title);
	    //If it looks like it has an extension then strip it off
	    if(ext!=null && ext.length()<=6) {
		title = IOUtil.stripExtension(title);
	    }
	}
        return title;
    }




    /**
     *  Search up the tree of dataset nodes, looking for a child service node.
     *  If not found then return null.
     *
     *
     * @param nodes nodes to process
     * @param datasetNode The element to look at.
     * @param serviceName The name of the service
     */
    private static void findChildServiceNode(List nodes, Element datasetNode,
                                             String serviceName) {
        if (datasetNode == null) {
            return;
        }
        for (Element serviceNode : (List<Element>) XmlUtil.findChildren(
                datasetNode, TAG_SERVICE)) {
            String serviceType = XmlUtil.getAttribute(serviceNode,
                                     ATTR_SERVICETYPE, NULL_STRING);
            //If the name doesn't match...
            if ( !Misc.equals(XmlUtil.getAttribute(serviceNode, ATTR_NAME,
                    NULL_STRING), serviceName)) {
                // check if it's a compound service and ours is inside.
                if ( !Misc.equals(serviceType, SERVICE_COMPOUND)) {
                    continue;
                }
                for (Element child : (List<Element>) XmlUtil.findChildren(
                        serviceNode, TAG_SERVICE)) {
                    if (Misc.equals(XmlUtil.getAttribute(child, ATTR_NAME,
                            NULL_STRING), serviceName)) {
                        nodes.add(child);
                    }
                }
                continue;
            }

            //Here the name matched. If its a compound service then include all of the children
            if (Misc.equals(serviceType, SERVICE_COMPOUND)) {
                for (Element child : (List<Element>) XmlUtil.findChildren(
                        serviceNode, TAG_SERVICE)) {
                    nodes.add(child);
                }
                continue;
            }
            nodes.add(serviceNode);
        }
        Node node = datasetNode.getParentNode();
        //Are we at the top?
        if (node instanceof Element) {
            findChildServiceNode(nodes, (Element) node, serviceName);
        }
    }

    /**
     * Log the error
     *
     * @param msg  the error message
     */
    public static void errorMessage(String msg) {
        LogUtil.userErrorMessage(msg);
    }


    /**
     *  Find the service xml element for the given dataset node. First, we look for any  service nodes
     *  contained by the dataset node. If not found then we find the service name from the dataset node.
     *  If no service name attrbiute is found then  print an error and return null.  Now, we search the
     *  xml tree under the root node to find a service node with the given name. If not found then
     *  print an error and return null.
     *
     * @param datasetNode The dataset node to look for a service node for.
     * @param showErrors Do we tell the user if there was an error
     * @param type service type
     * @return Return the service node or null if not found.
     */
    public static Element findServiceNodeForDataset(Element datasetNode,
            boolean showErrors, String type) {
        double version = getVersion(datasetNode);
        String serviceName = findServiceNameForDataset(datasetNode, version,
                                 true);

        if (serviceName == null) {
            if (showErrors) {
                errorMessage("Could not find service name");
            }
            return null;
        }

        List serviceNodes = new ArrayList();
        findChildServiceNode(serviceNodes, datasetNode, serviceName);
        if (serviceNodes.size() == 0) {
            if (showErrors) {
                errorMessage("No service found with id = " + serviceName);
            }
            //            System.err.println (XmlUtil.toString (root));
            return null;
        }

        boolean typeWasNull = (type == null);
        if (type == null) {
            type = SERVICE_DODS + "|" + SERVICE_OPENDAP + "|" + SERVICE_ADDE
                   + "|" + SERVICE_RESOLVER + "|" + SERVICE_FILE;
        }
        type = type.toLowerCase();

        for (int i = 0; i < serviceNodes.size(); i++) {
            Element serviceNode = (Element) serviceNodes.get(i);

            //Now, we see if we have a compound service. If we do then we find a service node
            //with DODS as the service. This is a hack but for now it works.
            String serviceType = XmlUtil.getAttribute(serviceNode,
                                     ATTR_SERVICETYPE, NULL_STRING);

            if (serviceType == null) {
                continue;
            }
            serviceType = serviceType.toLowerCase();
            if (StringUtil.stringMatch(serviceType, type)) {
                return serviceNode;
            }
        }


        if (typeWasNull && (serviceNodes.size() > 0)) {
            return (Element) serviceNodes.get(0);
        }

        if (showErrors) {
            errorMessage("No service found with id = " + serviceName);
        }
        //            System.err.println (XmlUtil.toString (root));
        return null;
    }


    /**
     *  Find the base url attribute from the service that the given datasetNode is
     *  associated with.
     *
     *  @param datasetNode The dataset node we are looking for a base url for.
     * @param root
     *  @return The base url for the given dataset node.
     */
    public static String findBaseForDataset(Element datasetNode,
                                            Element root) {


        //Find the service node
        Element serviceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
            true, null);

        //If we couldn't find it then return null - the error message has been shown
        if (serviceNode == null) {
            return null;
        }

        //Pull out the base attribute
        String base = XmlUtil.getAttribute(serviceNode, ATTR_BASE,
                                           NULL_STRING);
        if (base == null) {
            errorMessage("No base found for dataset.");
            return null;
        }

        return base;
    }

    /**
     * Recurse up the dom tree to find the node that has a catalogurl attribute
     *
     * @param node The node to look at
     *
     * @return  The foudn catalogurl attribute or null
     */
    private static String findCatalogSource(Element node) {
        String source = XmlUtil.getAttribute(node, ATTR_CATALOGURL,
                                             (String) null);
        if (source != null) {
            return source;
        }
        Node parent = node.getParentNode();
        if ((parent == null) || !(parent instanceof Element)) {
            return null;
        }
        return findCatalogSource((Element) parent);
    }

    /**
     *  Recurse up the DOM tree, looking for a dataset that contains a serviceName attribute.
     *  We also look at "access" nodes contained by the dataset node.
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @param version The catalog version
     *  @param firstCall Is this the leaf node
     *  @return The name of the service that provides this dataset.
     */
    private static String findServiceNameForDataset(Element datasetNode,
            double version, boolean firstCall) {
        //First look for a service name attribute.
        String serviceName = XmlUtil.getAttribute(datasetNode,
                                 ATTR_SERVICENAME, NULL_STRING);
        if (serviceName != null) {
            return serviceName;
        }

        //Look for a contained access node
        Element accessNode = XmlUtil.findChild(datasetNode, TAG_ACCESS);
        if (accessNode != null) {
            serviceName = XmlUtil.getAttribute(accessNode, ATTR_SERVICENAME);
            if (serviceName != null) {
                return serviceName;
            }
        }

        serviceName = findServiceNameTagValue(datasetNode);
        if (serviceName != null) {
            return serviceName;
        }



        if (version >= 1.0) {
            return findServiceNameFromMetaData(datasetNode, version, true);
        } else {
            Element parent = (Element) datasetNode.getParentNode();
            //Only look at parent dataset nodes.
            if ((parent == null)
                || !XmlUtil.isTag(parent,TAG_DATASET)) {
                return null;
            }
            return findServiceNameForDataset(parent, version, false);
        }
    }

    /**
     * Recurse up the DOM tree, looking for a dataset that contains a
     * dataType attribute.
     *  We also look at "access" nodes contained by the dataset node.
     *
     * @param datasetNode The dataset node we are looking at.
     * @param root The root of the xml tree
     * @param version The catalog version<
     * @param firstCall Is this the leaf node
     * @return The name of the service that provides this dataset.
     */
    public static String findDataTypeForDataset(Element datasetNode,
            Element root, double version, boolean firstCall) {
        //First look for a data type attribute.
        String dataType = XmlUtil.getAttributeFromTree(datasetNode,
                              ATTR_DATATYPE, null);

        if (dataType != null) {
            return dataType;
        }

        dataType = findTagValue(datasetNode, TAG_DATATYPE);
        if (dataType != null) {
            return dataType;
        }

        if (version >= 1.0) {
            return findTagValueFromMetaData(datasetNode, version, true,
                                            TAG_DATATYPE);
        } else {
            Element parent = (Element) datasetNode.getParentNode();
            //Only look at parent dataset nodes.
            if ((parent == null)
                || !XmlUtil.isTag(parent,TAG_DATASET)) {
                return null;
            }
            return findDataTypeForDataset(parent, root, version, false);
        }
    }

    /**
     *  Find the value of the serviceName tag which is a child of the
     * given datasetNode. If none found then return null.
     *
     * @param datasetNode The node to look under
     *
     * @return The service name or null if none found
     */
    private static String findServiceNameTagValue(Element datasetNode) {
        return findTagValue(datasetNode, TAG_SERVICENAME);
    }

    /**
     * Find the value of the which is a child of the given datasetNode.
     * If none found then return null.
     *
     * @param datasetNode The node to look under
     * @param tagName name of the tag to look for
     *
     * @return The tag or null if none found
     */
    private static String findTagValue(Element datasetNode, String tagName) {
        Element tagNode = XmlUtil.findChild(datasetNode, tagName);
        if (tagNode != null) {
            String value = XmlUtil.getChildText(tagNode);
            if (value != null) {
                if (value.trim().length() > 0) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    /**
     * Find the value contained by the serviceName node contained by
     * a metadata node contained by the given datasetNode.
     * If this is not the first recursive call then only look at metadata
     * nodes that have inherited=true.
     *
     * @param datasetNode The data set node
     * @param version The catalog version
     * @param first Is this the first recursive call
     *
     * @return The service name or null if none found
     */
    private static String findServiceNameFromMetaData(Element datasetNode,
            double version, boolean first) {
        return findTagValueFromMetaData(datasetNode, version, first,
                                        TAG_SERVICENAME);
    }

    /**
     * Find the value of the tag from the metadata node
     *
     * @param datasetNode The data set node
     * @param version The catalog version
     * @param first Is this the first recursive call
     * @param tagName  name of the tag to search for
     *
     * @return the tag value or null if none found
     */
    private static String findTagValueFromMetaData(Element datasetNode,
            double version, boolean first, String tagName) {
        List children = XmlUtil.findChildren(datasetNode, TAG_METADATA);
        for (int i = 0; i < children.size(); i++) {
            Element metaDataNode = (Element) children.get(i);
            if ( !first
                    && !XmlUtil.getAttribute(metaDataNode, ATTR_INHERITED,
                                             false)) {
                continue;
            }
            String value = findTagValue(metaDataNode, tagName);
            if (value != null) {
                return value;
            }
        }


        Element parent = (Element) datasetNode.getParentNode();
        if ((parent == null) || !XmlUtil.isTag(parent,TAG_DATASET)) {
            return null;
        }
        return findTagValueFromMetaData(parent, version, false, tagName);

    }

    /**
     * Find the base url for the given service node. If not found print an
     * error and return null. If found then look for the "suffix" attribute
     * of the service node. If found append it to the urlPath. Return the
     * base concatenated with the urlPath.
     *
     * @param serviceNode The  service node for the given urlPath.
     * @param urlPath The  tail end of the absolute url.
     * @return The full url path.
     */
    public static String getAbsoluteUrl(Element serviceNode, String urlPath) {
        String base = XmlUtil.getAttribute(serviceNode, ATTR_BASE,
                                           NULL_STRING);
        if (base == null) {
            errorMessage("No base found for dataset.");
            return null;
        }
        String suffix = XmlUtil.getAttribute(serviceNode, ATTR_SUFFIX,
                                             NULL_STRING);
        if (suffix != null) {
            urlPath = urlPath + suffix;
        }


        base = base + urlPath;
        String catalogSource = findCatalogSource(serviceNode);
        if (catalogSource != null) {
            base = XmlTree.expandRelativeUrl(base, catalogSource);
        }
        return base;
    }


    /**
     *  Lookup and return the urlPath defined for the given datasetNode.
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @return The url path for the dataset node.
     */
    public static String getUrlPath(Element datasetNode) {
        String urlPath = XmlUtil.getAttribute(datasetNode, ATTR_URLPATH,
                             NULL_STRING);

        //   <dataset name="Model data" dataType="Grid">
        //      <service serviceType="DODS" name="mlode" base="http://motherlode.ucar.edu/cgi-bin/dods/nph-nc/"/>
        //      <dataset name="NCEP AVN-Q model data">
        //         <dataset name="NCEP AVN-Q 2002-12-20 18:00:00 GMT" serviceName="mlode" urlPath="dods/model/2002122018_avn-q.nc"/>


        //If no urlPath attribute look for a contained access node which holds a urlPath
        if (urlPath == null) {
            Element accessNode = XmlUtil.findChild(datasetNode, TAG_ACCESS);
            if (accessNode != null) {
                urlPath = XmlUtil.getAttribute(accessNode, ATTR_URLPATH);
            }
        }
        return urlPath;
    }


    /**
     * This reads the xml pointed to by the given resolverUrl. It flags an
     * error if the url is bad, the xml is bad, the xml contains 0 dataset
     * nodes, the xml contains more than one dataset node. It returns an
     * array of object which contain:
     *  <pre>
     *        Object[] {newXmlRoot, datasetNode, serviceNode, url}
     *  </pre>
     *
     *  @param resolverUrl The url pointing to the resolver catalog.
     * @param properties The properties
     *  @return Array of root element, dataset node, service node and the
     *          absolute url of the data.
     */
    public static Object[] getResolverData(String resolverUrl,
                                           Hashtable properties) {
        Element newRoot = null;
        try {
            String contents = IOUtil.readContents(resolverUrl);
            if (contents == null) {
                errorMessage("Failed to read the catalog:" + resolverUrl);
                return null;
            }
            newRoot = XmlUtil.getRoot(contents);
            newRoot.setAttribute(ATTR_CATALOGURL, resolverUrl);
        } catch (Exception exc) {
            errorMessage("Error reading catalog:" + resolverUrl + "\n" + exc);
            return null;
        }
        if (newRoot == null) {
            errorMessage("Failed to retrieve the catalog:" + resolverUrl);
            return null;
        }

        List datasetNodes = XmlUtil.findDescendants(newRoot, TAG_DATASET);
        if (datasetNodes.size() == 0) {
            errorMessage("No dataset nodes found in the  catalog:"
                         + resolverUrl);
            return null;
        }
        if (datasetNodes.size() > 1) {
            errorMessage("Too many dataset nodes found in the  catalog:"
                         + resolverUrl);
            return null;
        }
        Element datasetNode = (Element) datasetNodes.get(0);
        Element serviceNode = findServiceNodeForDataset(datasetNode, false,
                                  null);

        if (serviceNode == null) {
            errorMessage("Could not find service node");
            return null;
        }
        String urlPath = getUrlPath(datasetNode);
        if (properties != null) {
            addServiceProperties(datasetNode, properties, urlPath);
        }
        return new Object[] { newRoot, datasetNode, serviceNode,
                              getAbsoluteUrl(serviceNode, urlPath) };
    }

    /**
     *  The given resolverUrl should return a catalog that holds one dataset. This method returns
     *  the absolute url that that catalog holds. If the given properties is no null then
     *  this will also try to extract the title from the xml and will put the PROP_TITLE into the
     *  properties.
     *
     *  @param resolverUrl The url pointing to the resolved catalog.
     *  @param properties To put the title into.
     *  @return The absolute url that the resolverUrl resolves to (may be null).
     */
    public static String resolveUrl(String resolverUrl,
                                    Hashtable properties) {

        Object[] result = getResolverData(resolverUrl, properties);
        if (result == null) {
            return null;
        }
        if (properties != null) {
            String title = getTitleFromDataset((Element) result[1]);
            if (title != null) {
                properties.put(PROP_TITLE, title);
            }
        }
        return (String) result[3];
    }




    /**
     * Add any service urls to the properties
     *
     * @param datasetNode data set node
     * @param properties properties
     * @param urlPath base url
     */
    public static void addServiceProperties(Element datasetNode,
                                            Hashtable properties,
                                            String urlPath) {


        Element dataServiceNode;

        dataServiceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
                false, SERVICE_HTTP);
        if (dataServiceNode != null) {
            String serviceUrl = getAbsoluteUrl(dataServiceNode, urlPath);
            if (serviceUrl != null) {
                properties.put(PROP_SERVICE_HTTP, serviceUrl);
            }
        }
    }


    /**
     * Generate an html representation of the catalog
     *
     * @param root Root of the catalog
     * @param datasetNode The data set node we are looking at
     * @param cnt The current count of the data set nodes we have processed
     * @param bundleTemplate The bundle template we generate the bundle from
     * @param jnlpTemplate The jnlp template
     *
     * @return The current count
     */
    public static int generateHtml(Element root, Element datasetNode,
                                   int cnt, String bundleTemplate,
                                   String jnlpTemplate) {
        cnt++;
        String  name        = XmlUtil.getAttribute(datasetNode, "name");
        Element serviceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
            false, null);
        if (serviceNode == null) {
            System.out.println("<li> " + name + "\n");
        } else {
            String    serviceType = getServiceType(serviceNode);
            Hashtable properties  = new Hashtable();
            boolean   isResolver  = (SERVICE_RESOLVER.equals(serviceType));
            if ( !isResolver) {
                String urlPath    = getUrlPath(datasetNode);
                String dataUrl    = getAbsoluteUrl(serviceNode, urlPath);
                String jnlpFile   = "generated" + cnt + ".jnlp";
                String bundleFile = "generated" + cnt + ".xidv";
                System.out.println("<li> <a href=\"" + jnlpFile + "\">"
                                   + name + "</a>\n");
                try {
                    String bundle = StringUtil.replace(bundleTemplate,
                                        "%datasource%", dataUrl);
                    bundle = StringUtil.replace(bundle, "%title%", name);
                    IOUtil.writeFile(bundleFile, bundle);
                    String jnlp = StringUtil.replace(jnlpTemplate, "%title%",
                                      "Generated bundle for:" + name);
                    jnlp = StringUtil.replace(jnlp, "%jnlpfile%", jnlpFile);
                    jnlp = StringUtil.replace(
                        jnlp, "%bundle%",
                        "http://www.unidata.ucar.edu/projects/metapps/testgen/"
                        + bundleFile);
                    IOUtil.writeFile(jnlpFile, jnlp);
                } catch (Exception exc) {
                    System.err.println("error:" + exc);
                    System.exit(1);
                }
            }
        }

        List children = XmlUtil.findChildren(datasetNode, TAG_DATASET);
        for (int i = 0; i < children.size(); i++) {
            if (i == 0) {
                System.out.println("<ul>");
            }
            Element child = (Element) children.get(i);
            cnt = generateHtml(root, child, cnt, bundleTemplate,
                               jnlpTemplate);
        }
        if (children.size() > 0) {
            System.out.println("</ul>");
        }
        return cnt;
    }




}

