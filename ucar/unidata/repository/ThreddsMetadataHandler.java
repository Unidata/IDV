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
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;


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

import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import ucar.visad.quantities.CommonUnits;

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
    public static final Metadata.Type TYPE_CREATOR =
        new Metadata.Type("thredds.creator", "Creator");

    /** _more_ */
    public static final Metadata.Type TYPE_LINK =
        new Metadata.Type("thredds.link", "Link");

    /** _more_ */
    public static final Metadata.Type TYPE_DATAFORMAT =
        new Metadata.Type("thredds.dataFormat", "Data Format");

    /** _more_ */
    public static final Metadata.Type TYPE_DATATYPE =
        new Metadata.Type("thredds.dataType", "Data Type");

    /** _more_ */
    public static final Metadata.Type TYPE_AUTHORITY =
        new Metadata.Type("thredds.authority", "Authority");

    /** _more_ */
    public static final Metadata.Type TYPE_VARIABLES =
        new Metadata.Type("thredds.variables", "Variables");


    /** _more_ */
    public static final Metadata.Type TYPE_VARIABLE =
        new Metadata.Type("thredds.variable", "Variable", "Variables");

    /** _more_ */
    public static final Metadata.Type TYPE_PUBLISHER =
        new Metadata.Type("thredds.publisher", "Publisher");

    /** _more_ */
    public static final Metadata.Type TYPE_PROJECT =
        new Metadata.Type("thredds.project", "Project");

    /** _more_ */
    public static final Metadata.Type TYPE_KEYWORD =
        new Metadata.Type("thredds.keyword", "Keyword");

    /** _more_ */
    public static final Metadata.Type TYPE_CONTRIBUTOR =
        new Metadata.Type("thredds.contributor", "Contributor");

    /** _more_ */
    public static final Metadata.Type TYPE_PROPERTY =
        new Metadata.Type("thredds.property", "Property");

    /** _more_ */
    public static final Metadata.Type TYPE_DOCUMENTATION =
        new Metadata.Type("thredds.documentation", "Documentation");

    /** _more_ */
    public static final Metadata.Type TYPE_ICON =
        new Metadata.Type("thredds.icon", "Icon");

    /** _more_ */
    public static final Metadata.Type TYPE_CDL =
        new Metadata.Type("thredds.cdl", "CDL", "CDL");


    /** _more_          */
    public static final String NCATTR_STANDARD_NAME = "standard_name";


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
        TYPE_LINK.setSearchableMask(Metadata.Type.SEARCHABLE_ATTR1
                                    | Metadata.Type.SEARCHABLE_ATTR2);
        TYPE_PROPERTY.setSearchableMask(Metadata.Type.SEARCHABLE_ATTR1
                                        | Metadata.Type.SEARCHABLE_ATTR2);
        addType(TYPE_DOCUMENTATION);
        addType(TYPE_PROPERTY);
        addType(TYPE_LINK);
        addType(TYPE_KEYWORD);
        addType(TYPE_ICON);
        addType(TYPE_PUBLISHER);
        addType(TYPE_CREATOR);
        addType(TYPE_CONTRIBUTOR);
        addType(TYPE_PROJECT);
        addType(TYPE_AUTHORITY);
        addType(TYPE_DATATYPE);
        addType(TYPE_DATAFORMAT);
        //        addType(TYPE_VARIABLES);
        addType(TYPE_VARIABLE);
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
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra) {

        NetcdfDataset dataset = null;
        File file = entry.getResource().getFile();
        try {
            super.getInitialMetadata(request, entry, metadataList, extra);
            if ( !getDataOutputHandler().canLoadAsCdm(entry)) {
                return;
            }
            dataset =
                NetcdfDataset.openDataset(file.toString());
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

                if (ATTR_KEYWORDS.equals(name)) {
                    for (String keyword : (List<String>) StringUtil.split(
                            value, ";", true, true)) {
                        Metadata metadata =
                            new Metadata(getRepository().getGUID(),
                                         entry.getId(), TYPE_KEYWORD,
                                         DFLT_INHERITED, keyword, "", "", "");
                        //The true says to ad dit only if its unique
                        entry.addMetadata(metadata, true);
                    }
                    continue;
                }

                if (name.startsWith("_")) {
                    continue;
                }
                Metadata metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), TYPE_PROPERTY,
                                        DFLT_INHERITED, name, value, "", "");
                entry.addMetadata(metadata, true);
            }


            List<Variable> variables = dataset.getVariables();
            //            System.err.println(entry.getResource());
            for (Variable var : variables) {
                if (var instanceof CoordinateAxis) {
                    CoordinateAxis ca       = (CoordinateAxis) var;
                    AxisType       axisType = ca.getAxisType();
                    if(axisType == null) continue;
                    if (axisType.equals(AxisType.Lat)) {
                        double[] minmax = getRange(var, ca.read(),
                                              CommonUnits.DEGREE);
                        //                        System.err.println("\t" +"lat range:" + minmax[0] + " " + minmax[1]);
                        if(extra.get(ARG_MINLAT)==null)
                            extra.put(ARG_MINLAT, minmax[0]);
                        if(extra.get(ARG_MAXLAT)==null)
                            extra.put(ARG_MAXLAT, minmax[1]);
                        haveBounds = true;
                    } else if (axisType.equals(AxisType.Lon)) {
                        double[] minmax = getRange(var, ca.read(),
                                                   CommonUnits.DEGREE);
                        //                        System.err.println("\t"+" lon range:" + minmax[0] + " " + minmax[1]);
                        if(extra.get(ARG_MINLON)==null)
                            extra.put(ARG_MINLON, minmax[0]);
                        if(extra.get(ARG_MAXLON)==null)
                            extra.put(ARG_MAXLON, minmax[1]);
                        haveBounds = true;
                    } else if (axisType.equals(AxisType.Time)) {
                        Date[] dates = getMinMaxDates(var, ca);
                        extra.put(ARG_FROMDATE, dates[0]);
                        extra.put(ARG_TODATE, dates[1]);
                    } else {
                        //                        System.err.println("unknown axis:" + axisType + " for var:" + var.getName());
                    }
                    continue;
                }


                String varName = var.getShortName();
                Metadata metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), TYPE_VARIABLE,
                                        DFLT_INHERITED, varName,
                                        var.getName(), var.getUnitsString(),
                                        "");
                entry.addMetadata(metadata, true);

                //Also add in the standard name
                ucar.nc2.Attribute att =
                    var.findAttribute(NCATTR_STANDARD_NAME);

                if (att != null) {
                    varName = att.getStringValue();
                    metadata = new Metadata(getRepository().getGUID(),
                                            entry.getId(), TYPE_VARIABLE,
                                            DFLT_INHERITED, varName,
                                            var.getName(), var.getUnitsString(),
                                            "");
                    entry.addMetadata(metadata, true);
                }



            }

            //If we didn't have a lat/lon coordinate axis then check projection
            //We do this here after because I've seen some point files that have an incorrect 360 bbox
            if (!haveBounds) {
                for (CoordinateSystem coordSys : (List<CoordinateSystem>) dataset
                        .getCoordinateSystems()) {
                    ProjectionImpl proj = coordSys.getProjection();
                    if (proj == null) {
                        continue;
                    }
                    LatLonRect llr = proj.getDefaultMapAreaLL();
                    haveBounds = true;
                    if(extra.get(ARG_MINLAT)==null) {
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
            System.out.println("Error reading metadata:" + file);
            System.out.println("Error:" + exc);
            //            exc.printStackTrace();
        } finally {
            try {
                if(dataset!=null) {
                    dataset.close();
                }
            } catch(Exception ignore){}
        }
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    private String getTag(Metadata.Type type) {
        int idx = type.getType().indexOf(".");
        if (idx < 0) {
            return type.getType();
        }
        return type.getType().substring(idx + 1);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Thredds";
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
        Metadata.Type type = getType(metadata.getType());
        if (type.equals(TYPE_LINK)) {
            XmlUtil.create(doc, getTag(TYPE_DOCUMENTATION), datasetNode,
                           new String[] { "xlink:href",
                                          metadata.getAttr2(), "xlink:title",
                                          metadata.getAttr1() });
        } else if (type.equals(TYPE_DOCUMENTATION)) {
            //            System.err.println ("tag:" +  getTag(TYPE_DOCUMENTATION));
            XmlUtil.create(doc, getTag(TYPE_DOCUMENTATION), datasetNode,
                           metadata.getAttr2(), new String[] { ATTR_TYPE,
                    metadata.getAttr1() });
        } else if (type.equals(TYPE_PROPERTY)) {
            XmlUtil.create(doc, getTag(TYPE_PROPERTY), datasetNode,
                           new String[] { ATTR_NAME,
                                          metadata.getAttr1(), ATTR_VALUE,
                                          metadata.getAttr2() });
        } else if (type.equals(TYPE_KEYWORD)) {
            XmlUtil.create(doc, getTag(TYPE_KEYWORD), datasetNode,
                           metadata.getAttr1());
        } else if (type.equals(TYPE_CONTRIBUTOR)) {
            XmlUtil.create(doc, getTag(TYPE_DOCUMENTATION), datasetNode,
                           metadata.getAttr1(), new String[] { ATTR_ROLE,
                    metadata.getAttr2() });

        } else if (type.equals(TYPE_VARIABLE)) {
            Element variablesNode = XmlUtil.getElement(datasetNode,
                                        TAG_VARIABLES);
            if (variablesNode == null) {
                variablesNode = XmlUtil.create(doc, TAG_VARIABLES,
                        datasetNode);
            }
            XmlUtil.create(doc, getTag(TYPE_VARIABLE), variablesNode,
                           metadata.getAttr2(), new String[] { ATTR_NAME,
                    metadata.getAttr1(), ATTR_UNITS, metadata.getAttr3() });
        } else if (type.equals(TYPE_ICON)) {
            XmlUtil.create(doc, getTag(TYPE_DOCUMENTATION), datasetNode,
                           new String[] { "xlink:href",
                                          metadata.getAttr1(), "xlink:title",
                                          "icon" });
        } else if (type.equals(TYPE_PUBLISHER) || type.equals(TYPE_CREATOR)) {
            Element node = XmlUtil.create(doc, getTag(type), datasetNode);
            XmlUtil.create(doc, CatalogOutputHandler.TAG_NAME, node,
                           metadata.getAttr1(), new String[] { ATTR_ROLE,
                    metadata.getAttr2() });
            XmlUtil.create(doc, CatalogOutputHandler.TAG_CONTACT, node,
                           new String[] { ATTR_EMAIL,
                                          metadata.getAttr3(), ATTR_URL,
                                          metadata.getAttr4() });
        }
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean xxcanHandle(String type) {
        if (super.canHandle(type)) {
            return true;
        }
        //For now
        return super.canHandle("thredds." + type);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata) {
        Metadata.Type type    = getType(metadata.getType());
        String        lbl     = msgLabel(type.getLabel());
        String        content = null;
        if (type.equals(TYPE_LINK)) {
            content = getSearchLink(request, metadata)
                      + HtmlUtil.href(metadata.getAttr2(),
                                      metadata.getAttr1());
        } else if (type.equals(TYPE_DOCUMENTATION)) {
            if (metadata.getAttr1().length() > 0) {
                lbl = msgLabel(getLabel(metadata.getAttr1()));
            }
            content = metadata.getAttr2();
        } else if (type.equals(TYPE_PROPERTY)) {
            lbl     = msgLabel(getLabel(metadata.getAttr1()));
            content = getSearchLink(request, metadata) + metadata.getAttr2();
        } else if (type.equals(TYPE_ICON)) {
            lbl     = "";
            content = HtmlUtil.img(metadata.getAttr1());
        } else if (type.equals(TYPE_PUBLISHER) || type.equals(TYPE_CREATOR)) {
            content = getSearchLink(request, metadata) + metadata.getAttr1();
            if (metadata.getAttr3().length() > 0) {
                content += HtmlUtil.br() + msgLabel("Email")
                           + HtmlUtil.space(1)
                           + HtmlUtil.href("mailto:" + metadata.getAttr3(),
                                           metadata.getAttr3());
            }
            if (metadata.getAttr4().length() > 0) {
                content += HtmlUtil.br() + msgLabel("URL")
                           + HtmlUtil.space(1)
                           + HtmlUtil.href(metadata.getAttr4(),
                                           metadata.getAttr4());
            }

        } else if (type.equals(TYPE_VARIABLE)) {
            content = getSearchLink(request, metadata) + metadata.getAttr1()
                      + HtmlUtil.space(1) + "(" + metadata.getAttr3() + ")"
                      + HtmlUtil.space(2) + metadata.getAttr2();

        } else if (type.equals(TYPE_PROJECT)) {
            content = getSearchLink(request, metadata) + metadata.getAttr1();
        } else if (type.equals(TYPE_KEYWORD)) {
            content = getSearchLink(request, metadata) + metadata.getAttr1();
        } else {
            content = metadata.getAttr1();
        }
        if (content == null) {
            return null;
        }
        return new String[] { lbl, content };
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param type _more_
     * @param doSelect _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer sb,
                                Metadata.Type type, boolean doSelect)
            throws Exception {
        sb.append(HtmlUtil.hidden(ARG_METADATA_TYPE + "." + type,
                                  type.toString()));
        String inheritedCbx = HtmlUtil.checkbox(ARG_METADATA_INHERITED + "."
                                  + type, "true", false) + HtmlUtil.space(1)
                                      + "inherited";
        inheritedCbx = "";

        if (doSelect) {
            String[] values = getMetadataManager().getDistinctValues(request,
                                  this, type);
            if ((values == null) || (values.length == 0)) {
                return;
            }
            List l = trimValues((List<String>) Misc.toList(values));
            l.add(0, new TwoFacedObject(msg("-all-"), ""));
            String argName = ARG_METADATA_ATTR1 + "." + type;
            String value   = request.getString(argName, "");
            sb.append(HtmlUtil.formEntry(msgLabel(type.getLabel()),
                                         HtmlUtil.select(argName, l, value,
                                             100) + inheritedCbx));
        } else {
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel(type.getLabel()),
                    HtmlUtil.input(ARG_METADATA_ATTR1 + "." + type, "")
                    + inheritedCbx));
        }

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer sb)
            throws Exception {
        addToSearchForm(request, sb, TYPE_DOCUMENTATION, false);
        addToSearchForm(request, sb, TYPE_KEYWORD, true);
        addToSearchForm(request, sb, TYPE_PROJECT, true);
        addToSearchForm(request, sb, TYPE_CREATOR, true);
        addToSearchForm(request, sb, TYPE_CONTRIBUTOR, true);
        addToSearchForm(request, sb, TYPE_PUBLISHER, true);
        addToSearchForm(request, sb, TYPE_VARIABLE, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToBrowseSearchForm(Request request, StringBuffer sb)
            throws Exception {
        addToBrowseSearchForm(request, sb, TYPE_KEYWORD, true);
        addToBrowseSearchForm(request, sb, TYPE_PROJECT, true);
        addToBrowseSearchForm(request, sb, TYPE_CREATOR, true);
        addToBrowseSearchForm(request, sb, TYPE_CONTRIBUTOR, true);
        addToBrowseSearchForm(request, sb, TYPE_PUBLISHER, true);
        addToBrowseSearchForm(request, sb, TYPE_VARIABLE, true);
    }


    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Entry entry, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        Metadata.Type type    = getType(metadata.getType());
        String        lbl     = msgLabel(type.getLabel());
        String        content = null;
        String        id      = metadata.getId();
        String        suffix  = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }


        String submit = HtmlUtil.submit(msg("Add") + HtmlUtil.space(1)
                                        + type.getLabel());
        String cancel = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
        if (forEdit) {
            submit = "";
            cancel = "";
        }
        String arg1 = ARG_ATTR1 + suffix;
        String arg2 = ARG_ATTR2 + suffix;
        String arg3 = ARG_ATTR3 + suffix;
        String arg4 = ARG_ATTR4 + suffix;
        String size = HtmlUtil.SIZE_70;

        if (type.equals(TYPE_LINK)) {
            content = formEntry(new String[] { submit, msgLabel("Label"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size),
                    msgLabel("Url"),
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });
        } else if (type.equals(TYPE_ICON)) {
            content = formEntry(new String[] { submit, msgLabel("URL"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size) });
        } else if (type.equals(TYPE_DOCUMENTATION)) {
            List types = Misc.toList(new Object[] {
                new TwoFacedObject(msg("Summary"), "summary"),
                new TwoFacedObject(msg("Funding"), "funding"),
                new TwoFacedObject(msg("History"), "history"),
                new TwoFacedObject(msg("Language"), "language"),
                new TwoFacedObject(msg("Processing Level"),
                                   "processing_level"),
                new TwoFacedObject(msg("Rights"), "rights")
            });;

            content = formEntry(new String[] { submit, msgLabel("Type"),
                    HtmlUtil.select(arg1, types, metadata.getAttr1()),
                    msgLabel("Value"),
                    HtmlUtil.textArea(arg2, metadata.getAttr2(), 5, 50) });
        } else if (type.equals(TYPE_CONTRIBUTOR)) {
            content = formEntry(new String[] { submit, msgLabel("Name"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size),
                    msgLabel("Role"),
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });
        } else if (type.equals(TYPE_VARIABLE)) {
            content = formEntry(new String[] { submit, msgLabel("Variable"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size),
                    msgLabel("Role"),
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });
        } else if (type.equals(TYPE_PROPERTY)) {
            content = formEntry(new String[] { submit, msgLabel("Name"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size),
                    msgLabel("Value"),
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });

        } else if (type.equals(TYPE_KEYWORD)) {
            content = formEntry(new String[] { submit, msgLabel("Value"),
                    HtmlUtil.input(arg1,
                                   metadata.getAttr1().replace("\n", ""),
                                   size),
                    msgLabel("Vocabulary"),
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });

        } else if (type.equals(TYPE_PUBLISHER) || type.equals(TYPE_CREATOR)) {
            content = formEntry(new String[] {
                submit, msgLabel("Organization"),
                HtmlUtil.input(arg1, metadata.getAttr1(), size),
                msgLabel("Email"),
                HtmlUtil.input(arg3, metadata.getAttr3(), size),
                msgLabel("URL"),
                HtmlUtil.input(arg4, metadata.getAttr4(), size)
            });
        } else {
            content = formEntry(new String[] { submit, msgLabel("Value"),
                    HtmlUtil.input(arg1, metadata.getAttr1(), size) });
        }
        if (content == null) {
            return null;
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        if (cancel.length() > 0) {
            content = content + HtmlUtil.row(HtmlUtil.colspan(cancel, 2));
        }
        return new String[] { lbl, content };
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isTag(String tag, Metadata.Type type) {
        return ("thredds." + tag).toLowerCase().equals(type.getType());
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
        System.err.println("thredds:" + tag);
        if (isTag(tag, TYPE_DOCUMENTATION)) {
            if (XmlUtil.hasAttribute(child, "xlink:href")) {
                String url = XmlUtil.getAttribute(child, "xlink:href");
                return new Metadata(getRepository().getGUID(), "", TYPE_LINK,
                                    DFLT_INHERITED,
                                    XmlUtil.getAttribute(child,
                                        "xlink:title", url), url, "", "");
            } else {
                System.err.println("making DOCUMENTATION");
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
                                   CatalogOutputHandler.TAG_NAME);
            String name = XmlUtil.getChildText(nameNode).trim();
            String vocabulary = XmlUtil.getAttribute(nameNode,
                                    ATTR_VOCABULARY, "");
            String email = "";
            String url   = "";
            Element contactNode = XmlUtil.findChild(child,
                                      CatalogOutputHandler.TAG_CONTACT);
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

