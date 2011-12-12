/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

// $Id: IdvAuthenticator.java,v 1.3 2007/05/09 21:59:26 dmurray Exp $

package ucar.unidata.util;
import ucar.unidata.xml.XmlUtil;




/**
 *
 * @author IDV Development Team
 * @version $Id: PasswordManager.java,v 1.3 2007/05/09 21:59:26 dmurray Exp $
 */
public class UserInfo {

    private String realm;
    private String userId;
    private String password;


    public UserInfo() {
    }


    public UserInfo(String realm, String userId, String password) {
        this.realm = realm;
        this.userId = userId;
        this.password = password;
    }

    public String toString() {
        return userId +":" + password;
    }

    public boolean equals(Object o) {
        if(!(o instanceof UserInfo)) return false;
        UserInfo that = (UserInfo) o;
        return Misc.equals(this.realm, that.realm) &&
            Misc.equals(this.userId, that.userId) &&
            Misc.equals(this.password, that.password);
    }

    public int hashCode() {
        return Misc.hashcode(realm) ^ Misc.hashcode(userId) ^ Misc.hashcode(password);
    }



    public String getPassword() {
        return password;
    }

/**
Set the Realm property.

@param value The new value for Realm
**/
public void setRealm (String value) {
	this.realm = value;
}

/**
Get the Realm property.

@return The Realm
**/
public String getRealm () {
	return this.realm;
}

/**
Set the UserId property.

@param value The new value for UserId
**/
public void setUserId (String value) {
	this.userId = value;
}

/**
Get the UserId property.

@return The UserId
**/
public String getUserId () {
	return this.userId;
}




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




}

