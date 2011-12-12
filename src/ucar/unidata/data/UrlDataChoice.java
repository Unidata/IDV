/*
 * $Id: UrlDataChoice.java,v 1.15 2006/12/01 20:41:23 jeffmc Exp $
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

package ucar.unidata.data;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;



import visad.*;

import visad.georef.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;



/**
 * A DataChoice for a URL
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class UrlDataChoice extends DataChoice {

    /** The URL */
    String url;

    /**
     * Default Constructor; does nothing
     */
    public UrlDataChoice() {}

    /**
     * Create a UrlDataChoice from the URL
     *
     * @param url   URL for this choice
     *
     */
    public UrlDataChoice(String url) {
        this(url, url);
    }

    /**
     * Create a UrlDataChoice.
     *
     * @param url    URL (choice ID)
     * @param desc   DataChoice description
     */
    public UrlDataChoice(String url, String desc) {
        super(url, desc, desc,
              Misc.newList(new DataCategory("Documentation", true),
                           new DataCategory(DataCategory.CATEGORY_HTML,
                                            false)));
        this.url = url;
    }

    /**
     * Create a UrlDataChoice from another.
     *
     * @param other     other UrlDataChoice
     *
     */
    public UrlDataChoice(UrlDataChoice other) {
        super(other);
        this.url = other.url;
    }


    /**
     * Override superclass method for creating a clone.
     *
     * @return   cloned choice
     */
    public DataChoice cloneMe() {
        return new UrlDataChoice(this);
    }

    /**
     * Set the URL for this choice.  Used by XML encoding
     *
     * @param value   URL for choice
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     * Get the URL for this choice.  Used by XML encoding
     *
     * @return   URL for choice
     */
    public String getUrl() {
        return url;
    }

    /**
     * Implementation of the getData method.
     *
     * @param category               the data category
     * @param dataSelection          the selection properties
     * @param requestProperties      special request properties
     *
     * @return the data based on the input parameters
     *
     * @throws DataCancelException      if the request was canceled
     * @throws RemoteException          Java RMI problem
     * @throws VisADException           problem creating the Data object
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        try {
            //This could be simple text
            if ( !url.startsWith("http:")) {
                return new visad.Text(url);
            }
            return new visad.Text(IOUtil.readContents(url));
        } catch (java.io.FileNotFoundException fnfe) {
            LogUtil.printException(log_, "getData", fnfe);
        } catch (java.io.IOException ioe) {
            LogUtil.printException(log_, "getData", ioe);
        }
        return null;
    }



    /**
     * Return the hash code for this UrlDataChoice.
     * @return the hash code for this UrlDataChoice
     */
    public int hashCode() {
        //TODO - is this a good way to combine the hash codes?
        return (url.hashCode() ^ (super.hashCode()));
    }

    /**
     * See if the object in question is equal to this UrlDataChoice.
     *
     * @param o   Object in question
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(o instanceof UrlDataChoice)) {
            return false;
        }
        return super.equals(o) && ((UrlDataChoice) o).url.equals(url);
    }

    /**
     * Add the data change listener.  Does nothing.
     *
     * @param listener   listener to add
     */
    public void addDataChangeListener(DataChangeListener listener) {}

    /**
     * Remove the data change listener.  Does nothing.
     *
     * @param listener   listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {}



}

