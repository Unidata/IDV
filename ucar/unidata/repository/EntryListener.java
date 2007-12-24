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

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

import java.util.List;


/**
 * Class FileInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class EntryListener implements Constants, Tables {
    private     Request request;
    private     Repository repository;
    private     Hashtable properties;
    private     Entry entry;
    private     List names = new ArrayList();
    private     List values = new ArrayList();
    private     String id;

    public  EntryListener (Repository repository, Request request) {
        this(repository, null, request);
    }


    public  EntryListener (Repository repository, String id, Request request) {
        this.request = request;
        this.repository = repository;
        this.properties = request.getDefinedProperties();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            names.add(arg);
            values.add(value);
        }
    }

    
    public String getId() {
        return id;
    }

    public boolean nameMatch(String s1, String s2) {
        //TODO: We need to have a StringMatcher object
        if(s1.endsWith("%")) {
            s1 = s1.substring(0,s1.length()-1);
            return s2.startsWith(s1);
        }
        if(s1.startsWith("%")) {
            s1 = s1.substring(1);
            return s2.endsWith(s1);
        }
        return s2.equals(s1);
    }

    public boolean processEntry(Entry entry) {
        for (int i=0;i<names.size();i++) {
            String arg   = (String) names.get(i);
            String value = (String) values.get(i);
            if(arg.equals(ARG_TYPE)) {
                if(!value.equals(entry.getTypeHandler().getType())) return false;
            } else if(arg.equals(ARG_NAME)) {
                if(!nameMatch(value, entry.getName())) return false;
            } else if(arg.equals(ARG_DESCRIPTION)) {
                if(!nameMatch(value, entry.getDescription())) return false;
            } else if(arg.equals(ARG_GROUP)) {
                //TODO: check for subgroups
                if(!(value.equals(entry.getGroup().getFullName()) ||
                     value.equals(entry.getGroup().getId()))) return false;
            } else {
                //TODO: ask the type handler
            }
        }

        this.entry = entry;
        synchronized(this) {
            this.notify();
        }
        return true;
    }

    public Entry getEntry() {
        return entry;
    }

}

