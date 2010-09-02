/*--

 Copyright (C) 2001 Brett McLaughlin.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows
    these conditions in the do*****entation and/or other materials
    provided with the distribution.

 3. The name "Building Java Enterprise Applications" must not be used
    to endorse or promote products derived from this software without
    prior written permission.  For written permission, please contact
    brett@newInstance.com.

 In addition, we request (but do not require) that you include in the
 end-user do*****entation provided with the redistribution and/or in the
 software itself an acknowledgement equivalent to the following:
     "This product includes software developed for the
      'Building Java Enterprise Applications' book, by Brett McLaughlin
      (O'Reilly & Associates)."

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 */
//jeffmc: change the package name to a ramadda package
//package ldap;
package ucar.unidata.repository.auth.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LDAPManager {

    /** The OU (organizational unit) to add users to */
    private static String USERS_OU = new String();

    /** The OU (organizational unit) to add groups to */
    private static String GROUPS_OU = new String();

    /** The OU (organizational unit) to add permissions to */
    private static final String PERMISSIONS_OU =
        "ou=Permissions,o=forethought.com";

    /** The default LDAP port */
    private static final int DEFAULT_PORT = 389;

    /** The LDAPManager instance object */
    private static Map instances = new HashMap();

    /** The connection, through a <code>DirContext</code>, to LDAP */
    private DirContext context;

    /** The hostname connected to */
    private String hostname;

    /** The port connected to */
    private int port;

    protected LDAPManager(  String hostname, int port,
                            String users_ou, String groups_ou,
                            String username, String password)
        throws NamingException {

        context = getInitialContext(hostname, port, username, password);

        // Only save data if we got connected
        this.hostname = hostname;
        this.port = port;
        this.USERS_OU = users_ou;
        this.GROUPS_OU = groups_ou;
    }

    public static LDAPManager getInstance(String hostname,
                                          int port,
                                          String users_ou,
                                          String groups_ou,
                                          String username,
                                          String password)
        throws NamingException {

        // Construct the key for the supplied information
        String key = new StringBuffer()
            .append(hostname)
            .append(":")
            .append(port)
            .append("|")
            .append((username == null ? "" : username))
            .append("|")
            .append((password == null ? "" : password))
            .toString();

        if (!instances.containsKey(key)) {
            synchronized (LDAPManager.class) {
                if (!instances.containsKey(key)) {
                    LDAPManager instance =
                        new LDAPManager(hostname, port,
                                        users_ou, groups_ou,
                                        username, password);
                    instances.put(key, instance);
                    return instance;
                }
            }
        }

        return (LDAPManager)instances.get(key);
    }

    public static LDAPManager getInstance(String hostname, int port)
        throws NamingException {
        return getInstance(hostname, port, null, null,null,null);
    }

    public static LDAPManager getInstance(String hostname)
        throws NamingException {
        return getInstance(hostname, DEFAULT_PORT, null, null,null, null);
    }

    public boolean isValidUser(String username, String password) {
        try {
            DirContext context =
                getInitialContext(hostname, port, getUserDN(username),
                                  password);
            return true;
        } catch (javax.naming.NameNotFoundException e) {
            return false;
        } catch (NamingException e) {
            // Any other error indicates couldn't log user in
            return false;
        }
    }

    public boolean userInGroup(String username, String groupName)
        throws NamingException {

        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniqueMember";

        Attributes attributes =
            context.getAttributes(getGroupDN(groupName),
                                  searchAttributes);
        if (attributes != null) {
            Attribute memberAtts = attributes.get("uniqueMember");
            if (memberAtts != null) {
                for (NamingEnumeration vals = memberAtts.getAll();
                     vals.hasMoreElements();
                     ) {
                    if (username.equalsIgnoreCase(
                        getUserUID((String)vals.nextElement()))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /*
     * Get the attributes defined by the variable userAttributes and their values
     *
     * @param username          Name of the user
     *
     * @return userAttributes   Hashtable keys are the attributes names and
     *                          hashtable values are the attribute values
     *
     */

    public Hashtable getUserAttributes(String username)
        throws NamingException {

        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniqueMember";

        Hashtable userAttributes = new Hashtable();
        List attributeValues = new ArrayList();

        Attributes attributes =
            context.getAttributes(getUserDN(username));
        if (attributes != null) {
            for (NamingEnumeration ae = attributes.getAll(); ae.hasMore();) {
                Attribute attr = (Attribute)ae.next();
                System.out.println("attribute: " + attr.getID());
                if (attr.getID().equals("givenName") || attr.getID().equals("sn")){
                    /* Print each value */
                    for (NamingEnumeration e = attr.getAll(); e.hasMore(); ){
                        String value = new String((String)e.next());
                        attributeValues.add(value);
                        System.out.println("value: " + value);
                    }
                    userAttributes.put((String) attr.getID(),attributeValues);
                    attributeValues = new ArrayList();
                }
            }
            return userAttributes;
        }
        return null;
    }

    public List getMembers(String groupName) throws NamingException {
        List members = new LinkedList();

        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniqueMember";

        Attributes attributes =
            context.getAttributes(getGroupDN(groupName),
                                  searchAttributes);
        if (attributes != null) {
            Attribute memberAtts = attributes.get("uniqueMember");
            if (memberAtts != null) {
                for (NamingEnumeration vals = memberAtts.getAll();
                     vals.hasMoreElements();
                     members.add(
                         getUserUID((String)vals.nextElement()))) ;
            }
        }

        return members;
    }

    public List getGroups(String username) throws NamingException {
        List groups = new LinkedList();

        // Set up criteria to search on
        String filter = new StringBuffer()
            .append("(&")
            .append("(objectClass=groupOfForethoughtNames)")
            .append("(uniqueMember=")
            .append(getUserDN(username))
            .append(")")
            .append(")")
            .toString();

        // Set up search constraints
        SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        NamingEnumeration results =
            context.search(GROUPS_OU, filter, cons);

        while (results.hasMore()) {
            SearchResult result = (SearchResult)results.next();
            groups.add(getGroupCN(result.getName()));
        }

        return groups;
    }

    public void revokePermission(String groupName, String permissionName)
        throws NamingException {

        try {
            ModificationItem[] mods = new ModificationItem[1];

            Attribute mod =
                new BasicAttribute("uniquePermission",
                                   getPermissionDN(permissionName));
            mods[0] =
                new ModificationItem(DirContext.REMOVE_ATTRIBUTE, mod);
            context.modifyAttributes(getGroupDN(groupName), mods);
        } catch (NoSuchAttributeException e) {
            // Ignore errors if the attribute doesn't exist
        }
    }

    public boolean hasPermission(String groupName, String permissionName)
        throws NamingException {

        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniquePermission";

        Attributes attributes =
            context.getAttributes(getGroupDN(groupName),
                                  searchAttributes);
        if (attributes != null) {
            Attribute permAtts = attributes.get("uniquePermission");
            if (permAtts != null) {
                for (NamingEnumeration vals = permAtts.getAll();
                     vals.hasMoreElements();
                     ) {
                    if (permissionName.equalsIgnoreCase(
                        getPermissionCN((String)vals.nextElement()))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public List getPermissions(String groupName) throws NamingException {
        List permissions = new LinkedList();

        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniquePermission";

        Attributes attributes =
            context.getAttributes(getGroupDN(groupName),
                                  searchAttributes);
        if (attributes != null) {
            Attribute permAtts = attributes.get("uniquePermission");
            if (permAtts != null) {
                for (NamingEnumeration vals = permAtts.getAll();
                     vals.hasMoreElements();
                     permissions.add(
                         getPermissionCN((String)vals.nextElement()))) ;
            }
        }

        return permissions;
    }

    private String getUserDN(String username) {
        return new StringBuffer()
                .append("uid=")
                .append(username)
                .append(",")
                .append(USERS_OU)
                .toString();
    }

    private String getUserUID(String userDN) {
        int start = userDN.indexOf("=");
        int end = userDN.indexOf(",");

        if (end == -1) {
            end = userDN.length();
        }

        return userDN.substring(start+1, end);
    }

    private String getUserCN(String username) {
        return new StringBuffer()
                .append("cn=")
                .append(username)
                .append(",")
                .append(USERS_OU)
                .toString();
    }

    private String getGroupDN(String name) {
        return new StringBuffer()
                .append("cn=")
                .append(name)
                .append(",")
                .append(GROUPS_OU)
                .toString();
    }

    private String getGroupCN(String groupDN) {
        int start = groupDN.indexOf("=");
        int end = groupDN.indexOf(",");

        if (end == -1) {
            end = groupDN.length();
        }

        return groupDN.substring(start+1, end);
    }

    private String getPermissionDN(String name) {
        return new StringBuffer()
                .append("cn=")
                .append(name)
                .append(",")
                .append(PERMISSIONS_OU)
                .toString();
    }

    private String getPermissionCN(String permissionDN) {
        int start = permissionDN.indexOf("=");
        int end = permissionDN.indexOf(",");

        if (end == -1) {
            end = permissionDN.length();
        }

        return permissionDN.substring(start+1, end);
    }

    private DirContext getInitialContext(String hostname, int port,
                                         String username, String password)
        throws NamingException {

        String providerURL =
            new StringBuffer("ldap://")
                .append(hostname)
                .append(":")
                .append(port)
                .toString();

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY,
                  "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, providerURL);

        if ((username != null) && (!username.equals(""))) {
            props.put(Context.SECURITY_AUTHENTICATION, "simple");
            props.put(Context.SECURITY_PRINCIPAL, username);
            props.put(Context.SECURITY_CREDENTIALS,
                ((password == null) ? "" : password));
        }

        return new InitialDirContext(props);
    }
}