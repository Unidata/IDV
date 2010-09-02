/*
 * Copyright 2010 ramadda.org 
 */

package ucar.unidata.repository.auth.ldap;


import ucar.unidata.repository.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.List;


/**
 * And example class to add ldap configuration options to the admin screen
 *
 */
public class LDAPAdminHandler extends AdminHandlerImpl {

    private int version = 0;

    /** ??? This is the default ldap port       */
    public static final int DEFAULT_PORT = 389;

    /** property id */
    public static final String LDAP_HANDLER_ID = "ldaphandler";


    /** property id */
    public static final String PROP_LDAP_SERVER = "ldap.server";

    /** property id */
    public static final String PROP_LDAP_PORT = "ldap.port";

    /** property id */
    public static final String PROP_LDAP_USER_DIRECTORY =
        "ldap.user.directory";

    /** property id */
    public static final String PROP_LDAP_GROUP_DIRECTORY =
        "ldap.group.directory";

    /** property id */
    public static final String PROP_LDAP_ADMIN = "ldap.admin";

    /** property id */
    public static final String PROP_LDAP_PASSWORD = "ldap.password";


    /** property label */
    public static final String LABEL_LDAP_SERVER = "Server";

    /** property label */
    public static final String LABEL_LDAP_PORT = "Port";

    /** property label */
    public static final String LABEL_LDAP_USER_DIRECTORY = "User Directory";

    /** property label */
    public static final String LABEL_LDAP_GROUP_DIRECTORY = "Group Directory";

    /** property label */
    public static final String LABEL_LDAP_ADMIN = "Admin ID";

    /** property label */
    public static final String LABEL_LDAP_PASSWORD = "Password";


    /** list of property ids */
    public static final String[] PROPERTY_IDS = {
        PROP_LDAP_SERVER, PROP_LDAP_PORT, PROP_LDAP_USER_DIRECTORY,
        PROP_LDAP_GROUP_DIRECTORY, PROP_LDAP_ADMIN, PROP_LDAP_PASSWORD,
    };

    /** list of property labels */
    public static final String[] PROPERTY_LABELS = {
        LABEL_LDAP_SERVER, LABEL_LDAP_PORT, LABEL_LDAP_USER_DIRECTORY,
        LABEL_LDAP_GROUP_DIRECTORY, LABEL_LDAP_ADMIN, LABEL_LDAP_PASSWORD,
    };

    /**
     * ctor. The repository gets set after the ctor
     */
    public LDAPAdminHandler() {}


    /**
     * helper method to find the ldap admin instance
     *
     * @param repository the repository
     *
     * @return this object
     */
    public static LDAPAdminHandler getLDAPHandler(Repository repository) {
        return (LDAPAdminHandler) repository.getAdmin().getAdminHandler(
            LDAP_HANDLER_ID);
    }

    /**
     * Used to uniquely identify this admin handler
     *
     * @return unique id for this admin handler
     */
    public String getId() {
        return LDAP_HANDLER_ID;
    }

    /**
     * This adds the fields into the admin Settings->Access form section
     *
     * @param blockId which section
     * @param sb form buffer to append to
     */
    public void addToSettingsForm(String blockId, StringBuffer sb) {
        //Are we in the access section
        if ( !blockId.equals(Admin.BLOCK_ACCESS)) {
            return;
        }
        sb.append(
            HtmlUtil.row(
                HtmlUtil.colspan(msgHeader("LDAP Configuration"), 2)));
        for (int i = 0; i < PROPERTY_IDS.length; i++) {
            String prop  = PROPERTY_IDS[i];
            String value = getRepository().getProperty(PROPERTY_IDS[i], "");
            //If its the password then we store it obfuscated in the db so its not just plain text
            boolean isPassword = isPassword(prop);
            if ((value != null) && isPassword) {
                value = deobfuscate(value);
            }
            if (isPassword) {
                sb.append(HtmlUtil.formEntry(msgLabel(PROPERTY_LABELS[i]),
                                             HtmlUtil.password(prop, value,
                                                 HtmlUtil.SIZE_40)));
            } else {
                sb.append(HtmlUtil.formEntry(msgLabel(PROPERTY_LABELS[i]),
                                             HtmlUtil.input(prop, value,
                                                 HtmlUtil.SIZE_40)));
            }
        }
    }


    /**
     * is this property  id the password
     *
     * @param prop property if
     *
     * @return is password
     */
    private boolean isPassword(String prop) {
        return prop.equals(PROP_LDAP_PASSWORD);
    }

    /**
     * Returns a integer timestamp to indicate whether anything has changed
     * since the last access to the server info
     */
    public int getVersion() {
        return version;
    }

    /**
     * apply the form submit
     *
     * @param request the request
     *
     * @throws Exception On badness
     */
    public void applySettingsForm(Request request) throws Exception {
        version++;
        for (int i = 0; i < PROPERTY_IDS.length; i++) {
            String prop  = PROPERTY_IDS[i];
            String value = request.getString(PROPERTY_IDS[i], "");
            if (isPassword(prop)) {
                //If its the password then we store it obfuscated in the db so its not just plain text
                value = obfuscate(value);
            }
            getRepository().writeGlobal(PROPERTY_IDS[i], value);
        }
    }

    /**
     * get the server
     *
     * @return the server
     */
    public String getServer() {
        return getRepository().getProperty(PROP_LDAP_SERVER, (String) null);
    }

    /**
     * get the port
     *
     * @return the port
     */
    public int getPort() {
        String value = getRepository().getProperty(PROP_LDAP_PORT,
                           "" + DEFAULT_PORT).trim();
        return new Integer(value).intValue();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUserDirectory() {
        return getRepository().getProperty(PROP_LDAP_USER_DIRECTORY,
                                           (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroupDirectory() {
        return getRepository().getProperty(PROP_LDAP_GROUP_DIRECTORY,
                                           (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getAdminID() {
        return getRepository().getProperty(PROP_LDAP_ADMIN, (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPassword() {
        //If its the password then we store it obfuscated in the db so its not just plain text
        return deobfuscate(getRepository().getProperty(PROP_LDAP_PASSWORD,
                (String) null));
    }

    /**
     * helper method to obfuscate password in db
     *
     * @param value password value
     *
     * @return obfuscated value
     */
    private String obfuscate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return value;
        }
        return new String(XmlUtil.encodeBase64(value.getBytes()));
    }

    /**
     * helper method to deobfuscate password from db
     *
     * @param obfuscated value password value
     *
     * @return unobfuscated value
     */

    private String deobfuscate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return value;
        }
        return new String(XmlUtil.decodeBase64(value));
    }



}
