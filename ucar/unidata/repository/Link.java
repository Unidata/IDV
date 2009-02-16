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


import ucar.unidata.util.HtmlUtil;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 * Class FileInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class Link {

    /** _more_ */
    String url;

    /** _more_ */
    String label;

    /** _more_ */
    String icon;

    /** _more_ */
    protected boolean hr = false;

    /** _more_          */
    int type = OutputType.TYPE_ACTION;

    /** _more_ */
    OutputType outputType;


    public Link(boolean hr) {
        this.hr = hr;
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param icon _more_
     * @param label _more_
     */
    public Link(String url, String icon, String label, int type) {
        this(url,icon,label,null,type);
    }


    public Link(String url, String icon, String label) {
        this(url, icon, label, null);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param icon _more_
     * @param label _more_
     * @param outputType _more_
     */
    public Link(String url, String icon, String label,
                OutputType outputType) {
        this(url, icon, label, outputType, getLinkType(outputType));
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param icon _more_
     * @param label _more_
     * @param outputType _more_
     * @param linkType _more_
     */
    public Link(String url, String icon, String label, OutputType outputType,
                int linkType) {
        this.url        = url;
        this.label      = label;
        this.icon       = icon;
        this.outputType = outputType;
        this.type       = linkType;
    }

    /**
     * _more_
     *
     * @param typeMask _more_
     *
     * @return _more_
     */
    public boolean isType(int typeMask) {
        return (getType() & typeMask) !=0;
    }

    /**
     * _more_
     *
     * @param type _more_
     */
    public void setLinkType(int type) {
        this.type = type;
    }

    /**
     * _more_
     *
     * @param outputType _more_
     *
     * @return _more_
     */
    public static int getLinkType(OutputType outputType) {
        if (outputType == null) {
            return OutputType.TYPE_ACTION;
        }
        return outputType.getType();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getType() {
        return type;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public OutputType getOutputType() {
        return outputType;
    }

    public boolean getHr() {
        return hr;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (hr) {
            return "<hr>";
        }
        if (icon == null) {
            return HtmlUtil.href(url, label);
        }
        return HtmlUtil.href(url, HtmlUtil.img(icon, label));
    }


    /**
     * Set the Url property.
     *
     * @param value The new value for Url
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     * Get the Url property.
     *
     * @return The Url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the Icon property.
     *
     * @param value The new value for Icon
     */
    public void setIcon(String value) {
        icon = value;
    }

    /**
     * Get the Icon property.
     *
     * @return The Icon
     */
    public String getIcon() {
        return icon;
    }



}

