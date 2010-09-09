/*
 * Copyright 2010 ramadda.org 
 */

package ucar.unidata.repository.auth.ldap;


import ucar.unidata.repository.Repository;
import ucar.unidata.repository.Request;
import ucar.unidata.repository.auth.User;
import ucar.unidata.repository.auth.UserAuthenticator;
import ucar.unidata.repository.auth.UserAuthenticatorImpl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import javax.naming.NamingException;


/**
 * This is a user authenticator to implement LDAP authentication
 *
 *
 * @author Kristian Sebastián Blalid Coastal Ocean Observing and Forecast System, Balearic Islands ICTS
 * @autho Jeff McWhirter ramadda.org
 */
public class LDAPUserAuthenticator extends UserAuthenticatorImpl {

    /** property name          */
    private static final String PROP_GROUP_ADMIN = "ldap.group.admin";

    /** property name          */
    private static final String PROP_ATTR_GIVENNAME = "ldap.attr.givenname";

    /** property name          */
    private static final String PROP_ATTR_SURNAME = "ldap.attr.surname";

    /** default value for admin role          */
    private static final String DFLT_GROUP_ADMIN = "reposAdmin";

    /** default value for given name          */
    private static final String DFLT_ATTR_GIVENNAME = "givenName";

    /** default value for  surname         */
    private static final String DFLT_ATTR_SURNAME = "sn";

    /** Manager for Ldap conection        */
    private LDAPManager manager = null;

    private int lastLDAPServerVersion = -1;



    /**
     * constructor.
     */
    public LDAPUserAuthenticator() {
        debug("created");

    }

    public LDAPUserAuthenticator(Repository repository) {
        super(repository);
        debug("created");
    }

    /**
     * If not already created create and return the LDAPManager. If manager fails then log the error
     * and return null
     *
     * @return LDAPManager
     */
    private LDAPManager getManager() {
        LDAPAdminHandler adminHandler =
            LDAPAdminHandler.getLDAPHandler(getRepository());

        //Check if the admin handler has changed its state
        if (lastLDAPServerVersion!=adminHandler.getVersion() ||
            manager == null) {
            //Connection instance with ldap server. 
            //It's necessary the admin user and password.
            try {
                lastLDAPServerVersion=adminHandler.getVersion();
                manager = LDAPManager.getInstance(adminHandler.getServer(),
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
     * do we have a valid manager
     *
     * @return has valid manager
     */
    public boolean hasManager() {
        return getManager() != null;
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
        if ( !hasManager()) {
            return null;
        }
        if (getManager().isValidUser(userId, password)) {
            return findUser(repository, userId);
        } else {
            return null;
        }
    }

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     *
     * @param repository the repository. 
     * @param userId The user to find
     *
     * @return The  non-local user that matches the given id or null
     */
    @Override
    public User findUser(Repository repository, String userId) {
        if ( !hasManager()) {
            return null;
        }
        try {
            debug("findUser: " + userId);
            debug("creating local user");

            // List of groups the user belongs
            List groupList = new LinkedList();
            // Group name
            String group = new String();
            // List of user roles, defined by the user groups list
            ArrayList<String> roles = new ArrayList<String>();

            // Hashtable with attributes names and their values
            Hashtable userAttr = new Hashtable();
            // List of attribute values
            List attrValues = new ArrayList();

            userAttr = getManager().getUserAttributes(userId);

            /*
              NOTE: I moved the names of the givenname, admin role, etc, into the ldap.properties
              file. The getProperty method looks up those values, if they are defined then return the value
              else return the DFLT value
            */


            // Attribute givenName only have one value
            attrValues =
                (ArrayList) userAttr.get(getProperty(PROP_ATTR_GIVENNAME,
                    DFLT_ATTR_GIVENNAME));
            String userName = (String) attrValues.get(0);
            // Attribute sn only have one value
            attrValues =
                (ArrayList) userAttr.get(getProperty(PROP_ATTR_SURNAME,
                    DFLT_ATTR_SURNAME));
            String userSurname = (String) attrValues.get(0);

            // Create the user with admin priviligies if user is in group reposAdmin
            String adminGroup = getProperty(PROP_GROUP_ADMIN, DFLT_GROUP_ADMIN);
            boolean isAdmin = getManager().userInGroup(userId,adminGroup);
            User user = new User(userId, userName + " " + userSurname, isAdmin);

            groupList = getManager().getGroups(userName);
            List     groups = new ArrayList(groupList);
            Iterator iter   = groups.iterator();
            while (iter.hasNext()) {
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
