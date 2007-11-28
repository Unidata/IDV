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

import ucar.unidata.data.SqlUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.DateUtil;
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



public class TypeHandler implements TableDefinitions {
    public static final String TYPE_ANY = "any";

    /** _more_          */
    public static final String TYPE_LEVEL3RADAR = "level3radar";

    /** _more_          */
    public static final String TYPE_LEVEL2RADAR = "level2radar";

    Repository repository;
    String type;

    public TypeHandler(Repository repository, String type) {
        this.repository = repository;
        this.type = type;
    }

    public String getType() {
        return type;
    }
    
    public boolean isType(String type) {
        return this.type.equals(type);
    }

    public void addToForm(StringBuffer  sb, Hashtable args, List where) throws Exception {

        String[] maxdates = SqlUtils.readString(repository.execute(SELECT_FILES_MAXDATE + SqlUtils.makeWhere(where)), 1);
        String[] mindates = SqlUtils.readString(repository.execute(SELECT_FILES_MINDATE + SqlUtils.makeWhere(where)), 1);
        String   maxdate  = ((maxdates.length > 0)
                             ? maxdates[0]
                             : "");
        String   mindate  = ((mindates.length > 0)
                             ? mindates[0]
                             : "");

        List<Group> groups = repository.getGroups(SqlUtils.readString(repository.execute(SELECT_FILES_GROUPS + SqlUtils.makeWhere(where)), 1));

        if (groups.size() > 1) {
            List groupList = new ArrayList();
            for (Group group: groups) {
                groupList.add(new TwoFacedObject(group.getFullName()));
            }
            sb.append(HtmlUtil.makeTableEntry("<b>Group:</b>",HtmlUtil.makeSelect(groupList, Repository.ARG_GROUP)));
        } else if (groups.size() == 1) {
            sb.append(HtmlUtil.makeHidden(groups.get(0).getFullName(), Repository.ARG_GROUP));
        }

        sb.append(HtmlUtil.makeTableEntry("<b>Date Range:</b>","<input name=\"fromdate\" value=\""
            + mindate + "\"> -- <input name=\"todate\" value=\"" + maxdate + "\">"));

        if(isType(TYPE_LEVEL3RADAR)) {
            where.add(SqlUtils.eq(COL_FILES_ID,COL_LEVEL3RADAR_ID));
            String[] products = SqlUtils.readString(repository.execute(SELECT_LEVEL3RADAR_PRODUCTS + 
                                                                       SqlUtils.makeWhere(where)), 1);
            List productList = new ArrayList();
            for(int i=0;i<products.length;i++) {
                productList.add(new TwoFacedObject(repository.getLongName(products[i]),products[i]));
            }
            productList.add(0, "All");

            String[] stations = SqlUtils.readString(repository.execute(SELECT_LEVEL3RADAR_STATIONS +
                                                                       SqlUtils.makeWhere(where)), 1);

            List stationList = new ArrayList();
            for(int i=0;i<stations.length;i++) {
                stationList.add(new TwoFacedObject(repository.getLongName(stations[i]),stations[i]));
            }
            productList.add(0, "All");
            if (stations.length > 1) {
                sb.append(HtmlUtil.makeTableEntry("<b>Station:</b>",HtmlUtil.makeSelect(stationList, Repository.ARG_STATION)));
            } else if (stations.length == 1) {
                sb.append(HtmlUtil.makeHidden(stations[0], Repository.ARG_STATION));
            }
            if (products.length > 1) {
                sb.append(HtmlUtil.makeTableEntry("<b>Product:</b>",HtmlUtil.makeSelect(productList, Repository.ARG_PRODUCT)));
            } else if (products.length == 1) {
                sb.append(HtmlUtil.makeHidden(products[0], Repository.ARG_PRODUCT));
            }
        }

    }

    protected List assembleWhereClause(Hashtable args) throws Exception {
        List   where     = new ArrayList();
        String groupName = (String) args.get(Repository.ARG_GROUP);
        if ((groupName != null) && !groupName.toLowerCase().equals("all")) {
            Group group = repository.findGroup(groupName);
            where.add(SqlUtils.eq(COL_FILES_GROUP_ID,
                                  SqlUtils.quote(group.getId())));
        }
        addOr(COL_FILES_TYPE, (String) args.get(Repository.ARG_TYPE), where);

        String fromdate = (String) args.get(Repository.ARG_FROMDATE);
        if ((fromdate != null) && (fromdate.trim().length() > 0)) {
            where.add(SqlUtils.ge(COL_FILES_FROMDATE,
                                  SqlUtils.quote(SqlUtils.getDateString(fromdate))));
        }
        String todate = (String) args.get(Repository.ARG_TODATE);
        if ((todate != null) && (todate.trim().length() > 0)) {
            where.add(SqlUtils.le(COL_FILES_TODATE,
                                  SqlUtils.quote(SqlUtils.getDateString(todate))));
        }

        if(isType(TYPE_LEVEL3RADAR)) {
            addOr(COL_LEVEL3RADAR_STATION, (String) args.get(Repository.ARG_STATION), where);
            addOr(COL_LEVEL3RADAR_PRODUCT, (String) args.get(Repository.ARG_PRODUCT), where);
            where.add(SqlUtils.eq(COL_FILES_ID, COL_LEVEL3RADAR_ID));
        }


        return where;
    }

    protected String getQueryOnTables(Hashtable args) {
        if(isType(TYPE_LEVEL3RADAR)) {
            return TABLE_FILES + "," + TABLE_LEVEL3RADAR;
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
            list.add("(" + SqlUtils.makeOrSplit(column, value, true) + ")");
        }
    }



    public void makeLinks(StringBuffer sb) {
        sb.append("<p><a href=\"/listgroups\"> List groups</a>");
        if(isType(TYPE_LEVEL3RADAR)) {
            sb.append("<p><a href=\"/radar/liststations\"> List stations</a>");
            sb.append("<p><a href=\"/radar/listproducts\"> List products</a>");
        }

    }

}

