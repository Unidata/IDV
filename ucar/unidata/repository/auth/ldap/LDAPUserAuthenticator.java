/*
 * Copyright 2010 ramadda.org
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

package ucar.unidata.repository.auth.ldap;


import javax.naming.NamingException;
import ucar.unidata.repository.auth.User;
import ucar.unidata.repository.auth.UserAuthenticator;
import ucar.unidata.repository.auth.UserAuthenticatorImpl;
import ucar.unidata.repository.Repository;
import ucar.unidata.repository.Request;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This is a user authenticator to implement LDAP authentication
 *
 *
 * @author Kristian Sebastián Blalid Coastal Ocean Observing and Forecast System, Balearic Islands ICTS
 * @autho Jeff McWhirter ramadda.org
 */
public class LDAPUserAuthenticator extends UserAuthenticatorImpl {

    private static final String GROUP_REPOSADMIN = "reposAdmin";
    private static final String ATTR_GIVENNAME = "givenName";


    // Manager for Ldap conection
    private LDAPManager manager = null;

    /**
     * constructor. 
     */
    public LDAPUserAuthenticator() {
        debug("created");

    }

    private LDAPManager getManager() {
        if(manager == null) {
            // Conection instance with ldap server. It's necessary the admin user and password.
            try {
                LDAPAdminHandler adminHandler  = LDAPAdminHandler.getLDAPHandler(getRepository());
                manager = LDAPManager.getInstance(
                                                  adminHandler.getServer(),
                                                  adminHandler.getPort(),
                                                  adminHandler.getUserDirectory(),
                                                  adminHandler.getGroupDirectory(),
                                                  adminHandler.getAdminID(),
                                                  adminHandler.getPassword());
            } catch (Exception e) {
                logError("LDAP Error: creating LDAPManager", e);
            }
        }
        return manager;
    }


    /**
     * this gets called when we want to  autheticate the given user/password
     * return null if user/password is unknown or incorrect
     *
     * @param repository the repository
     * @param request the  http request
     * @param extraLoginForm anything extra to put in the login form
     * @param userId the user id
     * @param password the password they provided
     *
     * @return The user if the user id and password are correct. Else return null
     */
    public User authenticateUser(Repository repository, Request request,
                                 StringBuffer extraLoginForm, String userId,
                                 String password) {
        debug("authenticateUser: " + userId);
        if (getManager().isValidUser(userId,password)){
            return findUser(repository,userId);
        } else {
            return null;
        }
    }

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     *
     * @param repository the repository. Note: you can access properties with repository.getProperty("property name");
     * @param userId The user to find
     *
     * @return The  non-local user that matches the given id or null
     */
    @Override
    public User findUser(Repository repository, String userId) {
        try {
            debug("findUser: " + userId);
            debug("creating local user");

            // List of groups the user belongs
            List groupList = new LinkedList();
            // Group name
            String group =  new String();
            // List of user roles, defined by the user groups list
            ArrayList<String> roles = new ArrayList<String>();

            // Hashtable with attributes names and their values
            Hashtable userAttr = new Hashtable();
            // List of attribute values
            List attrValues = new ArrayList();

            userAttr = getManager().getUserAttributes(userId);

            // Attribute givenName only have one value
            attrValues = (ArrayList) userAttr.get(ATTR_GIVENNAME);
            String userName = (String) attrValues.get(0);
            // Attribute sn only have one value
            attrValues = (ArrayList) userAttr.get("sn");
            String userSurname = (String) attrValues.get(0);

            // Create the user with admin priviligies if user is in group reposAdmin
            User user = new User(userId, userName + " " + userSurname ,getManager().userInGroup(userId, GROUP_REPOSADMIN)));

            groupList = getManager().getGroups(userName);
            List groups = new ArrayList(groupList);

            Iterator iter = groups.iterator();

            while (iter.hasNext()){
              group = (String) iter.next();
              roles.add(group);
            }
            user.setRoles(roles);
            return user;

        } catch (NamingException ex) {
            logError("LDAP Error: finding user", ex);
            return null;
        }
    }




    /**
     * This is used to list out the roles in the access pages
     *
     * @return _more_
     */
    @Override
    public List<String> getAllRoles() {
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("some role1");
        roles.add("some role2");
        return roles;
    }

    /**
     * this can be used to list out all of the users and display them
     * in RAMADDA
     * It is not used by RAMADDA right now
     *
     * @return _more_
     */
    @Override
    public List<User> getAllUsers() {
        return new ArrayList<User>();
    }


    /**
     * This will be used to allow this authenticator to add options
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param sb _more_
     */
    @Override
    public void addToConfigurationForm(Repository repository,
                                       StringBuffer sb) {}

    /**
     * This will be used to allow this authenticator to set the options from the config form
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param request _more_
     */
    @Override
    public void applyConfigurationForm(Repository repository,
                                       Request request) {}

}
