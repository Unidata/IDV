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

package ucar.unidata.repository.listener;

import ucar.unidata.repository.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.xml.XmlUtil;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class PasswordEntryListener  extends EntryListener {

    private String userId="";
    private String password="";


    public PasswordEntryListener() {
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     * @param request _more_
     */
    public PasswordEntryListener(Repository repository) {
        super(repository,null);
    }





    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(XmlUtil.decodeBase64(new String(value)));
        }
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }
        return XmlUtil.encodeBase64(password.getBytes()).getBytes();
    }

    protected String getPassword() {
        return password;
    }

    /**
       Set the UserId property.

       @param value The new value for UserId
    **/
    public void setUserId (String value) {
	userId = value;
    }

    /**
       Get the UserId property.

       @return The UserId
    **/
    public String getUserId () {
	return userId;
    }


}

