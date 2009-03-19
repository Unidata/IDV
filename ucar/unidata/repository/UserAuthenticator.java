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

import java.util.List;

public interface UserAuthenticator {

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     */
    public User findUser(Repository repository, String userId);

    /**
     * this gets called when we want to autheticate the given user/password
     * return null if user/password is unknown or incorrect
     */
    public User authenticateUser(Repository repository, String userId, String password);


    /**
     * This is used to list out the roles for display in the access pages
     */
    public List<String> getAllRoles();

    /**
     * this can be used to list out all of the users and display them
     * in RAMADDA
     * It is not used by RAMADDA right now
     */
    public List<User> getAllUsers();



    /**
     * This will be used to allow this authenticator to add options
     * to the admin config form
     * Its not used right now
     */
    public void addToConfigurationForm(Repository repository, StringBuffer sb);

    /**
     * This will be used to allow this authenticator to set the options from the config form
     * to the admin config form
     * Its not used right now
     */
    public void applyConfigurationForm(Repository repository,Request request);

}
