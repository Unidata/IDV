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
 */

package ucar.unidata.repository.auth;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import ucar.unidata.repository.Repository;
import ucar.unidata.repository.Request;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This is an example of an implementation of the UserAuthenticator interface.
 * This allows one to plugin their own user authentication. To use this make your 
 * own class just like this one and implement the below methods. Put the compiled .class
 * files into your RAMADDA home plugins directory and restart RAMADDA. (Or make a jar file of
 * the classes and put the jar in the plugins dir.). RAMADDA will instantiate of of these
 * classes and use it to do user authentication and management.
 *
 * The way this works is that if ramadda sees a .class file in the plugins directory 
 * (or in a jar in the plugins dir)
 * it will load the class at runtime. If the class is an instanceof UserAuthenticator interface
 * then ramadda will instantiate a version of this class and use it (in UserManager) to
 * do authentication. When authenticating ramadda first looks at its own user database
 * If the user is there then it autheticates normally. If not then it defers to the UserAuthenticator
 * So you will at least need to create one admin user account in ramadda that is separate
 * from the external authenication
 * 
 * To compile this: in the main repository src directory (e.g., ../) you can run:<pre>
 * ant userauthenticator
 *</pre>
 *
 * This will compile this class, make a userauthenticator.jar and echo a message of
 * where the jar is (../../../../lib/userauthenticator.jar) and where to put it
 *
 * This example authenticator handles a user with id=xxx and password=yyy
 *
 * @author Jeff McWhirter
 */
public class TestUserAuthenticator extends UserAuthenticatorImpl {

    // Manager for Ldap conection
    private LDAPManager manager = null;

    /**
     * constructor. 
     */
    public LDAPUserAuthenticator() {
        
        debug("created");

        // Conection instance with ldap server. It's necessary the admin user and password.
        try{
            manager = LDAPManager.getInstance(
                    "172.16.135.62", 389,
                    "ou=Users,dc=socib,dc=es", "ou=Groups,dc=socib,dc=es",
                    "cn=admin,dc=socib,dc=es","oceanbit2009");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void debug(String msg) {
        System.err.println("TestUserAuthenticator: " + msg);
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

        if (manager.isValidUser(userId,password)){
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

            userAttr = manager.getUserAttributes(userId);

            // Attribute givenName only have one value
            attrValues = (ArrayList) userAttr.get("givenName");
            String userName = (String) attrValues.get(0);
            // Attribute sn only have one value
            attrValues = (ArrayList) userAttr.get("sn");
            String userSurname = (String) attrValues.get(0);

            // Create the user with admin priviligies if user is in group reposAdmin
            User user = new User(userId, userName + " " + userSurname ,manager.userInGroup(userId, "reposAdmin"));

            groupList = manager.getGroups(userName);
            List groups = new ArrayList(groupList);

            Iterator iter = groups.iterator();

            while (iter.hasNext()){
              group = (String) iter.next();
              roles.add(group);
            }
            user.setRoles(roles);
            return user;

        } catch (NamingException ex) {
            Logger.getLogger(TestUserAuthenticator.class.getName()).log(Level.SEVERE, null, ex);
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
