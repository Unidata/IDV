/**
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


package ucar.unidata.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




import ucar.unidata.repository.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */

public class RepositoryFtpUserManager implements ucar.unidata.repository.Constants, org.apache.ftpserver.ftplet.UserManager {

    /** _more_          */
    FtpManager ftpManager;

    /** _more_          */
    BaseUser user;

    /**
     * _more_
     *
     * @param ftpManager _more_
     */
    public RepositoryFtpUserManager(FtpManager ftpManager) {
        this.ftpManager = ftpManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private Repository getRepository() {
        return ftpManager.getRepository();
    }

    /**
     * _more_
     *
     * @param auth _more_
     *
     * @return _more_
     */
    public org.apache.ftpserver.ftplet.User authenticate(
            Authentication auth) {

        String name = "anonymous";
        String password = "";
        
        try {
        if(auth instanceof UsernamePasswordAuthentication) {
            UsernamePasswordAuthentication upa = (UsernamePasswordAuthentication) auth;
	    name = upa.getUsername(); 
            password = upa.getPassword();
	    System.err.println("name:" + name +" password:" + password);
            if(!ftpManager.getRepository().getUserManager().isPasswordValid(name, password)) {
		System.err.println("bad pass");
                return null;
            }
        } else  if(auth instanceof AnonymousAuthentication) {
	    if (getRepository().getProperty(PROP_ACCESS_REQUIRELOGIN, false)) {
		return null;
	    }
	    name = ucar.unidata.repository.UserManager.USER_ANONYMOUS;
        } else {
            return null;
        }

	ucar.unidata.repository.User repositoryUser = getRepository().getUserManager().findUser(name);
	if(repositoryUser == null) {
	    System.err.println(" no user");
	    return null;
	}

	if (getRepository().getProperty(PROP_ACCESS_ADMINONLY, false)) {
	    if(!repositoryUser.getAdmin()) return  null;
	}

        BaseUser user   = new BaseUser();
        user.setName(name);
	user.setHomeDirectory(getRepository().getStorageManager().getTmpDir().toString());
        List<Authority> auths = new ArrayList<Authority>();
        auths.add(new ConcurrentLoginPermission(10, 10));
        user.setAuthorities(auths);
        //	System.err.println(" returning user:"+ user);
        return user;
        } catch(Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param username _more_
     */
    public void delete(java.lang.String username) {}

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public boolean doesExist(java.lang.String username) {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getAdminName() {
        return "foo";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getAllUserNames() {
        return new String[] { "jeffmc" };
    }


    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public org.apache.ftpserver.ftplet.User getUserByName(
            java.lang.String username) {
        return user;
    }

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public boolean isAdmin(java.lang.String username) {
        return false;
    }

    /**
     * _more_
     *
     * @param user _more_
     */
    public void save(org.apache.ftpserver.ftplet.User user) {}

}

