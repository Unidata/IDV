/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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
 * 
 */

package ucar.unidata.repository;


import ucar.unidata.util.HtmlUtil;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class EntryLink {

    /** _more_ */
    private String link;

    /** _more_ */
    private String folderBlock;

    /** _more_ */
    private String uid;

    /**
     * _more_
     *
     * @param link _more_
     * @param folderBlock _more_
     * @param uid _more_
     */
    public EntryLink(String link, String folderBlock, String uid) {
        this.link        = link;
        this.folderBlock = folderBlock;
        this.uid         = uid;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return link + HtmlUtil.br() + folderBlock;
    }

    /**
     *  Set the Link property.
     *
     *  @param value The new value for Link
     */
    public void setLink(String value) {
        link = value;
    }

    /**
     *  Get the Link property.
     *
     *  @return The Link
     */
    public String getLink() {
        return link;
    }

    /**
     *  Set the FolderBlock property.
     *
     *  @param value The new value for FolderBlock
     */
    public void setFolderBlock(String value) {
        folderBlock = value;
    }

    /**
     *  Get the FolderBlock property.
     *
     *  @return The FolderBlock
     */
    public String getFolderBlock() {
        return folderBlock;
    }

    /**
     *  Set the Uid property.
     *
     *  @param value The new value for Uid
     */
    public void setUid(String value) {
        uid = value;
    }

    /**
     *  Get the Uid property.
     *
     *  @return The Uid
     */
    public String getUid() {
        return uid;
    }



}
