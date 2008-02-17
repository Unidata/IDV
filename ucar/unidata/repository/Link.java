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

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.HtmlUtil;


/**
 * Class FileInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class Link {

    String url;
    String label;
    String icon;

    public Link(String url,String icon, String label ) {
        this.url = url;
        this.label = label;
        this.icon= icon;
    }


    public String toString() {
        return HtmlUtil.href(url, HtmlUtil.img(icon,label));
    }


    /**
Set the Url property.

@param value The new value for Url
**/
public void setUrl (String value) {
	url = value;
}

/**
Get the Url property.

@return The Url
**/
public String getUrl () {
	return url;
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
Set the Icon property.

@param value The new value for Icon
**/
public void setIcon (String value) {
	icon = value;
}

/**
Get the Icon property.

@return The Icon
**/
public String getIcon () {
	return icon;
}



}

