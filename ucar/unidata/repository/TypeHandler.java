/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

import ucar.unidata.data.SqlUtil;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.TextResult;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.sql.Statement;



public class TypeHandler implements Constants {
    public static final String TYPE_ANY = "any";

    /** _more_          */
    public static final String TYPE_LEVEL3RADAR = "level3radar";

    /** _more_          */
    public static final String TYPE_LEVEL2RADAR = "level2radar";

    Repository repository;
    String type;
    String description;

    public TypeHandler(Repository repository, String type) {
        this(repository, type, "");
    }

    public TypeHandler(Repository repository, String type, String description) {
        this.repository = repository;
        this.type = type;
        this.description  = description;
    }

    public String getType() {
        return type;
    }
    
    public boolean isType(String type) {
        return this.type.equals(type);
    }


    public TextResult showFile(DataInfo dataInfo,Hashtable args) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("File:" + dataInfo.getFile() +"<br>");
        sb.append("Type:" + dataInfo.getType() +"<br>");

        if(isType(TYPE_LEVEL3RADAR)) {
            
        }


        return new TextResult("File", sb);

    }

    public TextResult processList(Hashtable args, String what) throws Exception {
        return processRadarList(args, what);
    }


    public TextResult processRadarList(Hashtable args, String what) throws Exception {
        String column;
        String tag;
        String title;
        if(what.equals(WHAT_PRODUCT)) {
            column = "product";
            tag = "product";
            title = "Level 3 Radar Products";
        } else /*if(what.equals(WHAT_STATION))*/ {
            column = "station";
            tag = "station";
            title = "Level 3 Radar Stations";
        }
        List where =assembleWhereClause(args);
        String query = SqlUtil.makeSelect(SqlUtil.distinct(column),
                                           TABLE_LEVEL3RADAR,
                                           SqlUtil.makeAnd(where));
        Statement    statement = repository.execute(query);
        String[]     products  = SqlUtil.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        String output = repository.getValue(args, ARG_OUTPUT, OUTPUT_HTML);
        if(output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Products</h2>");
            sb.append("<ul>");
        } else if(output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(tag + "s"));

        } else if(output.equals(OUTPUT_CSV)) {
        } else {
            throw new IllegalArgumentException("Unknown output type:" + output);
        }

        for (int i = 0; i < products.length; i++) {
            if(output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(repository.getLongName(products[i]) + " (" + products[i]+")");
            } else if(output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(tag,
                                      XmlUtil.attrs(ATTR_ID, products[i],
                                                    ATTR_NAME,
                                                    repository.getLongName(products[i]))));
            } else if(output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(products[i],
                                        repository.getLongName(products[i])));
                sb.append("\n");
            }
        }
        if(output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if(output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(tag + "s"));
        }
        return new TextResult(title, sb, repository.getMimeType(output));
    }



    public void addToForm(StringBuffer  sb, Hashtable args, List where) throws Exception {

        String[] maxdates = SqlUtil.readString(repository.execute(SqlUtil.makeSelect(SqlUtil.max(COL_FILES_TODATE), 
                                                                                       getQueryOnTables(args),
                                                                                       SqlUtil.makeAnd(where))), 1);
        String[] mindates = SqlUtil.readString(repository.execute(SqlUtil.makeSelect(SqlUtil.min(COL_FILES_FROMDATE), 
                                                                                       getQueryOnTables(args),
                                                                                       SqlUtil.makeAnd(where))), 1);
        String   maxdate  = ((maxdates.length > 0)
                             ? maxdates[0]
                             : "");
        String   mindate  = ((mindates.length > 0)
                             ? mindates[0]
                             : "");

        List<TypeHandler> typeHandlers = repository.getTypeHandlers(args);
        if(typeHandlers.size()>1) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler: typeHandlers) {
                tmp.add(new TwoFacedObject(typeHandler.getType(),typeHandler.getType()));
            }
            String typeSelect = HtmlUtil.makeSelect(Repository.ARG_TYPE,tmp);
            sb.append(HtmlUtil.makeTableEntry("<b>Type:</b>",typeSelect));
        } else if(typeHandlers.size()==1) {
            sb.append(HtmlUtil.makeHidden(Repository.ARG_TYPE, typeHandlers.get(0).getType()));
        }
        
        String name = (String) args.get(ARG_NAME);
        if(name == null) {
            sb.append(HtmlUtil.makeTableEntry("<b>Name:</b>",HtmlUtil.makeInput(Repository.ARG_NAME,"")));
        }

        List<Group> groups = repository.getGroups(SqlUtil.readString(repository.execute(SELECT_FILES_GROUPS + SqlUtil.makeWhere(where)), 1));

        if (groups.size() > 1) {
            List groupList = new ArrayList();
            groupList.add("All");
            for (Group group: groups) {
                groupList.add(new TwoFacedObject(group.getFullName()));
            }
            String groupSelect = HtmlUtil.makeSelect(Repository.ARG_GROUP,groupList);
            //            groupSelect+="&nbsp;" +HtmlUtil.checkbox(ARG_GROUP_CHILDREN,"true") +" (Search subgroups)";
            sb.append(HtmlUtil.makeTableEntry("<b>Group:</b>",groupSelect));
        } else if (groups.size() == 1) {
            sb.append(HtmlUtil.makeHidden(Repository.ARG_GROUP, groups.get(0).getFullName()));
        }

        sb.append(HtmlUtil.makeTableEntry("<b>Date Range:</b>","<input name=\"fromdate\" value=\""
                                          + (mindate!=null?""+mindate:"") + "\"> -- <input name=\"todate\" value=\"" + 
                                          (maxdate!=null?""+maxdate:"") + "\">\n"));

        if(isType(TYPE_LEVEL3RADAR)) {
            where.add(SqlUtil.eq(COL_FILES_ID,COL_LEVEL3RADAR_ID));
            String[] products = SqlUtil.readString(repository.execute(SELECT_LEVEL3RADAR_PRODUCTS + 
                                                                       SqlUtil.makeWhere(where)), 1);
            List productList = new ArrayList();
            for(int i=0;i<products.length;i++) {
                productList.add(new TwoFacedObject(repository.getLongName(products[i]),products[i]));
            }
            productList.add(0, "All");

            String[] stations = SqlUtil.readString(repository.execute(SELECT_LEVEL3RADAR_STATIONS +
                                                                       SqlUtil.makeWhere(where)), 1);

            List stationList = new ArrayList();
            for(int i=0;i<stations.length;i++) {
                stationList.add(new TwoFacedObject(repository.getLongName(stations[i]),stations[i]));
            }
            productList.add(0, "All");
            if (stations.length > 1) {
                sb.append(HtmlUtil.makeTableEntry("<b>Station:</b>",HtmlUtil.makeSelect(Repository.ARG_STATION, stationList)));
            } else if (stations.length == 1) {
                sb.append(HtmlUtil.makeHidden(Repository.ARG_STATION, stations[0]));
            }
            if (products.length > 1) {
                sb.append(HtmlUtil.makeTableEntry("<b>Product:</b>",HtmlUtil.makeSelect(Repository.ARG_PRODUCT,productList)));
            } else if (products.length == 1) {
                sb.append(HtmlUtil.makeHidden(Repository.ARG_PRODUCT, products[0]));
            }
        }

    }

    protected List assembleWhereClause(Hashtable args) throws Exception {
        List   where     = new ArrayList();
        String name = (String) args.get(ARG_NAME);
        if(name !=null) name = name.trim();

        String groupName = (String) args.get(Repository.ARG_GROUP);
        if ((groupName != null) && !groupName.toLowerCase().equals("all")) {
            Group group = repository.findGroup(groupName);
            String searchChildren =  (String) args.get(ARG_GROUP_CHILDREN);
            //            System.err.println ("child:" + searchChildren);
            //            if(Misc.equals(searchChildren,"true")) {
            //                where.add(SqlUtil.like(COL_FILES_GROUP_ID,
            //                                       group.getId())));
            //            } else {
                where.add(SqlUtil.eq(COL_FILES_GROUP_ID,
                                     SqlUtil.quote(group.getId())));
                //            }
        }
        addOr(COL_FILES_TYPE, (String) args.get(Repository.ARG_TYPE), where);

        String fromdate = (String) args.get(Repository.ARG_FROMDATE);
        if ((fromdate != null) && (fromdate.trim().length() > 0)) {
            where.add(SqlUtil.ge(COL_FILES_FROMDATE,
                                  SqlUtil.quote(SqlUtil.getDateString(fromdate))));
        }
        String todate = (String) args.get(Repository.ARG_TODATE);
        if ((todate != null) && (todate.trim().length() > 0)) {
            where.add(SqlUtil.le(COL_FILES_TODATE,
                                  SqlUtil.quote(SqlUtil.getDateString(todate))));
        }

        if(isType(TYPE_LEVEL3RADAR)) {
            addOr(COL_LEVEL3RADAR_STATION, (String) args.get(Repository.ARG_STATION), where);
            addOr(COL_LEVEL3RADAR_PRODUCT, (String) args.get(Repository.ARG_PRODUCT), where);
            if(args.contains(Repository.ARG_STATION) || args.contains(Repository.ARG_PRODUCT)) {
                where.add(SqlUtil.eq(COL_FILES_ID, COL_LEVEL3RADAR_ID));
            }
        }

        //        System.err.println ("name:" + name);
        if(name!=null && name.length()>0) {
            if(name.startsWith("%")) {
                where.add(SqlUtil.like(COL_FILES_NAME, name));
            } else {
                where.add(SqlUtil.eq(COL_FILES_NAME, SqlUtil.quote(name)));
            }
        }

        return where;
    }


    protected String getQueryOnTables(Hashtable args) {
        if(isType(TYPE_LEVEL3RADAR)) {
            if(args.contains(Repository.ARG_PRODUCT) || args.contains(Repository.ARG_STATION)) {
                return TABLE_FILES + "," + TABLE_LEVEL3RADAR;
            }
        }
        return TABLE_FILES;
    }



    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param list _more_
     */
    private void addOr(String column, String value, List list) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            list.add("(" + SqlUtil.makeOrSplit(column, value, true) + ")");
        }
    }



    public void makeLinks(StringBuffer sb) {
        sb.append("<p>");
        sb.append(repository.href("/list?what=" +WHAT_TYPE,"List types"));
        sb.append ("&nbsp;|&nbsp;");
        sb.append(repository.href("/list?what=" + WHAT_GROUP,"List groups"));
        sb.append ("&nbsp;|&nbsp;");
        //        if(isType(TYPE_LEVEL3RADAR)) {
        sb.append ("&nbsp;|&nbsp;");
        sb.append(repository.href("/list?what=" + WHAT_STATION,"List stations"));
        sb.append ("&nbsp;|&nbsp;");
        sb.append(repository.href("/list?what=" + WHAT_PRODUCT,"List products"));
            //        }

    }


/**
Set the Description property.

@param value The new value for Description
**/
public void setDescription (String value) {
	description = value;
}

/**
Get the Description property.

@return The Description
**/
public String getDescription () {
	return description;
}

}

