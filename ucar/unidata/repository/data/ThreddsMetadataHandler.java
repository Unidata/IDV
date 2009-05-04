/**
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
 * WITHOUT ANY WARRANTYP; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository.data;


import org.w3c.dom.*;


import ucar.ma2.*;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;


import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;

import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;


import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;

import ucar.unidata.repository.*;

import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;



import visad.Unit;

import java.io.File;


import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ThreddsMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String TAG_VARIABLES = "variables";


    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_UNITS = "units";

    /** _more_ */
    public static final String ATTR_ROLE = "role";

    /** _more_ */
    public static final String ATTR_EMAIL = "email";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String ATTR_VALUE = "value";



    /** _more_ */
    public static final String TYPE_CREATOR ="thredds.creator";

    /** _more_ */
    public static final String TYPE_LINK ="thredds.link";

    /** _more_ */
    public static final String TYPE_DATAFORMAT ="thredds.dataFormat";

    /** _more_ */
    public static final String TYPE_DATATYPE = "thredds.dataType";

    /** _more_ */
    public static final String TYPE_AUTHORITY ="thredds.authority";

    /** _more_ */
    public static final String TYPE_VARIABLES ="thredds.variables";


    /** _more_ */
    public static final String TYPE_VARIABLE ="thredds.variable";

    /** _more_ */
    public static final String TYPE_PUBLISHER ="thredds.publisher";

    /** _more_ */
    public static final String TYPE_PROJECT = "thredds.project";

    /** _more_ */
    public static final String TYPE_KEYWORD ="thredds.keyword";

    /** _more_ */
    public static final String TYPE_CONTRIBUTOR ="thredds.contributor";

    /** _more_ */
    public static final String TYPE_PROPERTY ="thredds.property";

    /** _more_ */
    public static final String TYPE_DOCUMENTATION ="thredds.documentation";

    /** _more_ */
    public static final String TYPE_ICON ="thredds.icon";

    /** _more_ */
    public static final String TYPE_CDL ="thredds.cdl";


    /** _more_ */
    public static final String NCATTR_STANDARD_NAME = "standard_name";


    public ThreddsMetadataHandler(Repository repository)
            throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ThreddsMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        //        TYPE_LINK.setSearchableMask(Metadata.Type.SEARCHABLE_ATTR1
        //                                    | Metadata.Type.SEARCHABLE_ATTR2);
        //        TYPE_PROPERTY.setSearchableMask(Metadata.Type.SEARCHABLE_ATTR1
        //                                        | Metadata.Type.SEARCHABLE_ATTR2);
    }




    /**
     * _more_
     *
     * @param var _more_
     * @param a _more_
     * @param toUnit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static double[] getRange(VariableSimpleIF var, Array a,
                                     Unit toUnit)
            throws Exception {
        MAMath.MinMax minmax = MAMath.getMinMax(a);
        Unit fromUnit        =
            ucar.visad.Util.parseUnit(var.getUnitsString());
        /*
        System.out.println(var.getName());
        System.out.println("\tminmax:" + minmax.min + " " + minmax.max + " " + fromUnit);
        System.out.println("\tto unit:" + toUnit.toThis(minmax.min, fromUnit) + " " +toUnit.toThis(minmax.min, fromUnit));
        System.out.println("\tto unit:" + new Date((long)(1000*toUnit.toThis(minmax.min, toUnit))));
        */
        double[] result = new double[] { toUnit.toThis(minmax.min, fromUnit),
                                         toUnit.toThis(minmax.max,
                                             fromUnit) };
        return result;
    }


    /**
     * _more_
     *
     * @param var _more_
     * @param ca _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Date> getDates(VariableSimpleIF var, CoordinateAxis ca)
            throws Exception {
        Unit fromUnit        =
            ucar.visad.Util.parseUnit(var.getUnitsString());
        Unit          toUnit = visad.CommonUnit.secondsSinceTheEpoch;
        List<Date>    dates  = new ArrayList<Date>();
        Array         a      = ca.read();
        IndexIterator iter   = a.getIndexIteratorFast();
        while (iter.hasNext()) {
            double val = iter.getDoubleNext();
            if (val != val) {
                continue;
            }
            dates.add(new Date((long) (1000 * toUnit.toThis(val, fromUnit))));

        }
        return dates;
    }

    /**
     * _more_
     *
     * @param dataset _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Date> getDates(NetcdfDataset dataset)
            throws Exception {
        List<Variable> variables = dataset.getVariables();
        for (Variable var : variables) {
            if (var instanceof CoordinateAxis) {
                CoordinateAxis ca       = (CoordinateAxis) var;
                AxisType       axisType = ca.getAxisType();
                if (axisType.equals(AxisType.Time)) {
                    return getDates(var, ca);
                }
            }
        }
        return null;
    }




    /**
     * _more_
     *
     * @param var _more_
     * @param ca _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date[] getMinMaxDates(VariableSimpleIF var,
                                        CoordinateAxis ca)
            throws Exception {
        double[] minmax = getRange(var, ca.read(),
                                   visad.CommonUnit.secondsSinceTheEpoch);
        return new Date[] { new Date((long) minmax[0] * 1000),
                            new Date((long) minmax[1] * 1000) };
    }



    /** _more_ */
    public static final String ATTR_MINLAT = "geospatial_lat_min";

    /** _more_ */
    public static final String ATTR_MAXLAT = "geospatial_lat_max";

    /** _more_ */
    public static final String ATTR_MINLON = "geospatial_lon_min";

    /** _more_ */
    public static final String ATTR_MAXLON = "geospatial_lon_max";

    /** _more_ */
    public static final String ATTR_KEYWORDS = "keywords";


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     * @param extra _more_
     * @param shortForm _more_
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {

        NetcdfDataset dataset = null;
        try {
            DataOutputHandler dataOutputHandler = getRepository().getDataOutputHandler();
            super.getInitialMetadata(request, entry, metadataList, extra,
                                     shortForm);
            if ( !dataOutputHandler.canLoadAsCdm(entry)) {
                return;
            }
            String path =     dataOutputHandler.getPath(entry);
            dataset = NetcdfDataset.openDataset(path);
            boolean         haveBounds = false;
            List<Attribute> attrs      = dataset.getGlobalAttributes();
            for (Attribute attr : attrs) {
                String name  = attr.getName();
                String value = attr.getStringValue();
                if (value == null) {
                    value = "" + attr.getNumericValue();
                }
                if (ATTR_MAXLON.equals(name)) {
                    //                    System.err.println ("maxlon:" + value);
                    extra.put(ARG_MAXLON, new Double(value));
                    continue;
                }
                if (ATTR_MINLON.equals(name)) {
                    //                    System.err.println ("minlon:" + value);
                    extra.put(ARG_MINLON, new Double(value));
                    continue;
                }
                if (ATTR_MAXLAT.equals(name)) {
                    //                    System.err.println ("maxlat:" + value);
                    extra.put(ARG_MAXLAT, new Double(value));
                    continue;
                }
                if (ATTR_MINLAT.equals(name)) {
                    //                    System.err.println ("minlat:" + value);
                    extra.put(ARG_MINLAT, new Double(value));
                    continue;
                }

                if (shortForm) {
                    continue;
                }
                if (ATTR_KEYWORDS.equals(name)) {
                    for (String keyword : (List<String>) StringUtil.split(
                            value, ";", true, true)) {
                        Metadata metadata =
                            new Metadata(getRepository().getGUID(),
                                         entry.getId(), TYPE_KEYWORD,
                                         DFLT_INHERITED, keyword, "", "", "");
                        if ( !entry.hasMetadata(metadata)) {
                            metadataList.add(metadata);
                        }
                    }
                    continue;
                }

                if (name.startsWith("_")) {
                    continue;
                }
                Metadata metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), TYPE_PROPERTY,
                                        DFLT_INHERITED, name, value, "", "");
                if ( !entry.hasMetadata(metadata)) {
                    metadataList.add(metadata);
                }
            }


            List<Variable> variables = dataset.getVariables();
            //            System.err.println(entry.getResource());
            for (Variable var : variables) {
                if (var instanceof CoordinateAxis) {
                    CoordinateAxis ca       = (CoordinateAxis) var;
                    AxisType       axisType = ca.getAxisType();
                    if (axisType == null) {
                        continue;
                    }
                    if (axisType.equals(AxisType.Lat)) {
                        double[] minmax = getRange(var, ca.read(),
                                              visad.CommonUnit.degree);
                        //                        System.err.println("\t" +"lat range:" + minmax[0] + " " + minmax[1]);
                        if (extra.get(ARG_MINLAT) == null) {
                            extra.put(ARG_MINLAT, minmax[0]);
                        }
                        if (extra.get(ARG_MAXLAT) == null) {
                            extra.put(ARG_MAXLAT, minmax[1]);
                        }
                        haveBounds = true;
                    } else if (axisType.equals(AxisType.Lon)) {
                        double[] minmax = getRange(var, ca.read(),
                                              visad.CommonUnit.degree);
                        //                        System.err.println("\t"+" lon range:" + minmax[0] + " " + minmax[1]);
                        if (extra.get(ARG_MINLON) == null) {
                            extra.put(ARG_MINLON, minmax[0]);
                        }
                        if (extra.get(ARG_MAXLON) == null) {
                            extra.put(ARG_MAXLON, minmax[1]);
                        }
                        haveBounds = true;
                    } else if (axisType.equals(AxisType.Time)) {
                        Date[] dates = getMinMaxDates(var, ca);
                        Date minDate = (Date)  extra.get(ARG_FROMDATE);
                        Date maxDate = (Date)  extra.get(ARG_TODATE);
                        if(minDate!=null) {
                            dates[0] = DateUtil.min(dates[0],minDate);
                        }
                        if(maxDate!=null) {
                            dates[1] = DateUtil.max(dates[1],maxDate);
                        }

                        extra.put(ARG_FROMDATE, dates[0]);
                        extra.put(ARG_TODATE, dates[1]);
                    } else {
                        // System.err.println("unknown axis:" + axisType + " for var:" + var.getName());
                    }
                    continue;
                }


                if ( !shortForm) {
                    String varName = var.getShortName();
                    Metadata metadata =
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), TYPE_VARIABLE,
                                     DFLT_INHERITED, varName, var.getName(),
                                     var.getUnitsString(), "");
                    if ( !entry.hasMetadata(metadata)) {
                        metadataList.add(metadata);
                    }

                    //Also add in the standard name
                    ucar.nc2.Attribute att =
                        var.findAttribute(NCATTR_STANDARD_NAME);

                    if (att != null) {
                        varName = att.getStringValue();
                        metadata = new Metadata(getRepository().getGUID(),
                                entry.getId(), TYPE_VARIABLE, DFLT_INHERITED,
                                varName, var.getName(), var.getUnitsString(),
                                "");
                        if ( !entry.hasMetadata(metadata)) {
                            metadataList.add(metadata);
                        }
                    }

                }

            }

            //If we didn't have a lat/lon coordinate axis then check projection
            //We do this here after because I've seen some point files that have an incorrect 360 bbox
            if ( !haveBounds) {
                for (CoordinateSystem coordSys : (List<CoordinateSystem>) dataset
                        .getCoordinateSystems()) {
                    ProjectionImpl proj = coordSys.getProjection();
                    if (proj == null) {
                        continue;
                    }
                    LatLonRect llr = proj.getDefaultMapAreaLL();
                    haveBounds = true;
                    if (extra.get(ARG_MINLAT) == null) {
                        //                        System.err.println("\t"  +" bounds from cs:" + llr);
                        extra.put(ARG_MINLAT, llr.getLatMin());
                        extra.put(ARG_MAXLAT, llr.getLatMax());
                        extra.put(ARG_MINLON, llr.getLonMin());
                        extra.put(ARG_MAXLON, llr.getLonMax());
                    }
                    break;
                }
            }
        } catch (Exception exc) {
            System.out.println("Error reading metadata:" + entry.getResource());
            System.out.println("Error:" + exc);
            //            exc.printStackTrace();
        } finally {
            try {
                if (dataset != null) {
                    dataset.close();
                }
            } catch (Exception ignore) {}
        }
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static String getTag(String type) {
        int idx = type.indexOf(".");
        if (idx < 0) {
            return type;
        }
        return type.substring(idx + 1);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param doc _more_
     * @param datasetNode _more_
     *
     * @throws Exception _more_
     */
    public void addMetadataToCatalog(Request request, Entry entry,
                                     Metadata metadata, Document doc,
                                     Element datasetNode)
            throws Exception {
        if (metadata.getType().equals(TYPE_VARIABLE)) {
            Element variablesNode = XmlUtil.getElement(datasetNode,
                                        TAG_VARIABLES);
            if (variablesNode == null) {
                variablesNode = XmlUtil.create(doc, TAG_VARIABLES,
                        datasetNode);
            }
            XmlUtil.create(doc, getTag(TYPE_VARIABLE), variablesNode,
                           metadata.getAttr2(), new String[] { ATTR_NAME,
                    metadata.getAttr1(), ATTR_UNITS, metadata.getAttr3() });
        } else {
            super.addMetadataToCatalog(request,  entry,
                                       metadata, doc, datasetNode);

        }
    }



    /**
     * _more_
     *
     * @param tag _more_
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isTag(String tag, String type) {
        return ("thredds." + tag).toLowerCase().equals(type);
    }


    /**
     * _more_
     *
     * @param child _more_
     *
     * @return _more_
     */
    public Metadata makeMetadataFromCatalogNode(Element child) {
        String tag = child.getTagName();
        if (isTag(tag, TYPE_DOCUMENTATION)) {
            if (XmlUtil.hasAttribute(child, "xlink:href")) {
                String url = XmlUtil.getAttribute(child, "xlink:href");
                return new Metadata(getRepository().getGUID(), "", TYPE_LINK,
                                    DFLT_INHERITED,
                                    XmlUtil.getAttribute(child,
                                        "xlink:title", url), url, "", "");
            } else {
                String type = XmlUtil.getAttribute(child, "type", "summary");
                String text = XmlUtil.getChildText(child).trim();
                return new Metadata(getRepository().getGUID(), "",
                                    TYPE_DOCUMENTATION, DFLT_INHERITED, type,
                                    text, "", "");
            }
        } else if (isTag(tag, TYPE_PROJECT)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(), "", TYPE_PROJECT,
                                DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), "", "");
        } else if (isTag(tag, TYPE_CONTRIBUTOR)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(), "",
                                TYPE_CONTRIBUTOR, DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_ROLE, ""),
                                "", "");
        } else if (isTag(tag, TYPE_PUBLISHER) || isTag(tag, TYPE_CREATOR)) {
            Element nameNode = XmlUtil.findChild(child,
                                   CatalogUtil.TAG_NAME);
            String name = XmlUtil.getChildText(nameNode).trim();
            String vocabulary = XmlUtil.getAttribute(nameNode,
                                    ATTR_VOCABULARY, "");
            String email = "";
            String url   = "";
            Element contactNode = XmlUtil.findChild(child,
                                      CatalogUtil.TAG_CONTACT);
            if (contactNode != null) {
                email = XmlUtil.getAttribute(contactNode, ATTR_EMAIL, "");
                url   = XmlUtil.getAttribute(contactNode, ATTR_URL, "");
            }
            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                name, vocabulary, email, url);
        } else if (isTag(tag, TYPE_KEYWORD)) {
            String text = XmlUtil.getChildText(child).trim();
            //Some of the catalogs have new lines in the keyword
            text = text.replace("\r\n", " ");
            text = text.replace("\n", " ");
            return new Metadata(getRepository().getGUID(), "", TYPE_KEYWORD,
                                DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), "", "");

        } else if (isTag(tag, TYPE_AUTHORITY) || isTag(tag, TYPE_DATATYPE)
                   || isTag(tag, TYPE_DATAFORMAT)) {
            String text = XmlUtil.getChildText(child).trim();
            text = text.replace("\n", "");
            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                text, "", "", "");
        } else if (isTag(tag, TYPE_PROPERTY)) {
            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                XmlUtil.getAttribute(child, ATTR_NAME),
                                XmlUtil.getAttribute(child, ATTR_VALUE), "",
                                "");
        }
        return null;
    }




}

