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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.List;


/**
 * Class FileInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class EntryListener implements Constants, Tables {

    /** _more_ */
    private Request request;

    /** _more_ */
    private Repository repository;

    /** _more_ */
    private Hashtable properties;

    /** _more_ */
    private Entry entry;

    /** _more_ */
    private List names = new ArrayList();

    /** _more_ */
    private List values = new ArrayList();

    /** _more_ */
    private String id;

    /**
     * _more_
     *
     * @param repository _more_
     * @param request _more_
     */
    public EntryListener(Repository repository, Request request) {
        this(repository, null, request);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     * @param request _more_
     */
    public EntryListener(Repository repository, String id, Request request) {
        this.request    = request;
        this.repository = repository;
        this.properties = request.getDefinedProperties();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            names.add(arg);
            if (arg.equals(ARG_TAG)) {
                values.add(StringUtil.split(value, ",", true, true));
            } else {
                values.add(value);
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public boolean nameMatch(String s1, String s2) {
        //TODO: We need to have a StringMatcher object
        if (s1.endsWith("%")) {
            s1 = s1.substring(0, s1.length() - 1);
            return s2.startsWith(s1);
        }
        if (s1.startsWith("%")) {
            s1 = s1.substring(1);
            return s2.endsWith(s1);
        }
        return s2.equals(s1);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean checkEntry(Entry entry) {
        for (int i = 0; i < names.size(); i++) {
            String  arg   = (String) names.get(i);
            Object  value = values.get(i);
            boolean ok    = false;
            if (arg.equals(ARG_TYPE)) {
                ok = value.equals(entry.getTypeHandler().getType());
            } else if (arg.equals(ARG_NAME)) {
                ok = nameMatch(value.toString(), entry.getName());
            } else if (arg.equals(ARG_DESCRIPTION)) {
                ok = nameMatch(value.toString(), entry.getDescription());
            } else if (arg.equals(ARG_GROUP)) {
                //TODO: check for subgroups
                //                ok = (value.equals(entry.getParentGroup().getFullName())
                //                      || value.equals(entry.getParentGroup().getId()));
            } else if (arg.equals(ARG_TAG)) {
                List tags = entry.getTags();
                if ((tags == null) || (tags.size() == 0)) {
                    ok = false;
                } else {
                    ok = true;
                    List myTags = (List) value;
                    for (int tagIdx = 0; (tagIdx < myTags.size()) && ok;
                            tagIdx++) {
                        if ( !tags.contains(myTags.get(tagIdx))) {
                            ok = false;
                        }
                    }
                }
            } else {
                int match = entry.getTypeHandler().matchValue(arg, value,
                                request, entry);
                if (match == TypeHandler.MATCH_FALSE) {
                    ok = false;
                } else if (match == TypeHandler.MATCH_TRUE) {
                    ok = true;
                } else {
                    System.err.println("unknown Entry listener argument:"
                                       + arg);
                    ok = false;
                }
            }
            if ( !ok) {
                return false;
            }
        }


        this.entry = entry;
        synchronized (this) {
            this.notify();
        }
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Entry getEntry() {
        return entry;
    }

}

