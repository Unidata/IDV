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


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.util.HtmlUtil;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class TagCollection {

    String label;
    Hashtable tagMap = new Hashtable();
    List tags;


    public TagCollection(String label, List tags) {
        this.label = label;
        this.tags = tags;
        for(int i=0;i<tags.size();i++) {
            String tag  = (String)tags.get(i);
            tagMap.put(tag,tag);
        }
    }

    public void appendToForm(StringBuffer sb, String argName, String tag) {
        List tagList = new ArrayList(tags);
        tagList.add(0,TypeHandler.NONE_OBJECT);
        String label = (tag==null?"Add " : "") + getLabel() +":";
        label = label.replace(" ", "&nbsp;");
        String value = (tag!=null?tag:"");
        sb.append(
                  HtmlUtil.formEntry(
                                     label,
                                     HtmlUtil.select(argName, tagList,
                                                     value)));
    }


    public boolean contains(String tag) {
        return tagMap.get(tag)!=null;
    }


    public boolean contains(Tag tag) {
        return contains(tag.getName());
    }



/**
Set the Label property.

@param value The new value for Label
**/
public void setLabel (String value) {
	label = value;
}

/**
Get the Label property.

@return The Label
**/
public String getLabel () {
	return label;
}

/**
Set the Tags property.

@param value The new value for Tags
**/
public void setTags (List value) {
	tags = value;
}

/**
Get the Tags property.

@return The Tags
**/
public List getTags () {
	return tags;
}


}

