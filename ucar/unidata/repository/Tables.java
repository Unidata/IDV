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


import ucar.unidata.data.SqlUtil;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public interface Tables {



    /*
      For each of the tables in the database we have the following defs.
      The TABLE_<TABLE NAME> ... is the name of the table.
      The COL_<TABLE NAME>_<COLUMN NAME> is the name of the column in the table
      The ARRAY_<TABLE NAME> is the array of column names
      The COLUMNS_<TABLE NAME> is the comma separated list of columns in the table
      The J- and J+ turn off jindent formatting
     */

    //J+

    /** begin generated table definitions */

    public static final String TABLE_ASSOCIATIONS = "associations";

    /** _more_ */
    public static final String COL_ASSOCIATIONS_NAME = TABLE_ASSOCIATIONS
                                                       + ".name";

    /** _more_ */
    public static final String COL_ASSOCIATIONS_FROM_ENTRY_ID =
        TABLE_ASSOCIATIONS + ".from_entry_id";

    /** _more_ */
    public static final String COL_ASSOCIATIONS_TO_ENTRY_ID =
        TABLE_ASSOCIATIONS + ".to_entry_id";

    /** _more_ */
    public static final String[] ARRAY_ASSOCIATIONS =
        new String[] { COL_ASSOCIATIONS_NAME,
                       COL_ASSOCIATIONS_FROM_ENTRY_ID,
                       COL_ASSOCIATIONS_TO_ENTRY_ID };

    /** _more_ */
    public static final String COLUMNS_ASSOCIATIONS =
        SqlUtil.comma(ARRAY_ASSOCIATIONS);

    /** _more_ */
    public static final String NODOT_COLUMNS_ASSOCIATIONS =
        SqlUtil.commaNoDot(ARRAY_ASSOCIATIONS);

    /** _more_ */
    public static final String INSERT_ASSOCIATIONS =
        SqlUtil.makeInsert(
            TABLE_ASSOCIATIONS, NODOT_COLUMNS_ASSOCIATIONS,
            SqlUtil.getQuestionMarks(ARRAY_ASSOCIATIONS.length));



    /** _more_ */
    public static final String TABLE_COMMENTS = "comments";

    /** _more_ */
    public static final String COL_COMMENTS_ID = TABLE_COMMENTS + ".id";

    /** _more_ */
    public static final String COL_COMMENTS_ENTRY_ID = TABLE_COMMENTS
                                                       + ".entry_id";

    /** _more_ */
    public static final String COL_COMMENTS_USER_ID = TABLE_COMMENTS
                                                      + ".user_id";

    /** _more_ */
    public static final String COL_COMMENTS_DATE = TABLE_COMMENTS + ".date";

    /** _more_ */
    public static final String COL_COMMENTS_SUBJECT = TABLE_COMMENTS
                                                      + ".subject";

    /** _more_ */
    public static final String COL_COMMENTS_COMMENT = TABLE_COMMENTS
                                                      + ".comment";

    /** _more_ */
    public static final String[] ARRAY_COMMENTS = new String[] {
        COL_COMMENTS_ID, COL_COMMENTS_ENTRY_ID, COL_COMMENTS_USER_ID,
        COL_COMMENTS_DATE, COL_COMMENTS_SUBJECT, COL_COMMENTS_COMMENT
    };

    /** _more_ */
    public static final String COLUMNS_COMMENTS =
        SqlUtil.comma(ARRAY_COMMENTS);

    /** _more_ */
    public static final String NODOT_COLUMNS_COMMENTS =
        SqlUtil.commaNoDot(ARRAY_COMMENTS);

    /** _more_ */
    public static final String INSERT_COMMENTS =
        SqlUtil.makeInsert(TABLE_COMMENTS, NODOT_COLUMNS_COMMENTS,
                           SqlUtil.getQuestionMarks(ARRAY_COMMENTS.length));



    /** _more_ */
    public static final String TABLE_DUMMY = "dummy";

    /** _more_ */
    public static final String COL_DUMMY_NAME = TABLE_DUMMY + ".name";

    /** _more_ */
    public static final String[] ARRAY_DUMMY = new String[] {
                                                   COL_DUMMY_NAME };

    /** _more_ */
    public static final String COLUMNS_DUMMY = SqlUtil.comma(ARRAY_DUMMY);

    /** _more_ */
    public static final String NODOT_COLUMNS_DUMMY =
        SqlUtil.commaNoDot(ARRAY_DUMMY);

    /** _more_ */
    public static final String INSERT_DUMMY =
        SqlUtil.makeInsert(TABLE_DUMMY, NODOT_COLUMNS_DUMMY,
                           SqlUtil.getQuestionMarks(ARRAY_DUMMY.length));



    /** _more_ */
    public static final String TABLE_ENTRIES = "entries";

    /** _more_ */
    public static final String COL_ENTRIES_ID = TABLE_ENTRIES + ".id";

    /** _more_ */
    public static final String COL_ENTRIES_TYPE = TABLE_ENTRIES + ".type";

    /** _more_ */
    public static final String COL_ENTRIES_NAME = TABLE_ENTRIES + ".name";

    /** _more_ */
    public static final String COL_ENTRIES_DESCRIPTION = TABLE_ENTRIES
                                                         + ".description";

    /** _more_ */
    public static final String COL_ENTRIES_PARENT_GROUP_ID =
        TABLE_ENTRIES + ".parent_group_id";

    /** _more_ */
    public static final String COL_ENTRIES_USER_ID = TABLE_ENTRIES
                                                     + ".user_id";

    /** _more_ */
    public static final String COL_ENTRIES_RESOURCE = TABLE_ENTRIES
                                                      + ".resource";

    /** _more_ */
    public static final String COL_ENTRIES_RESOURCE_TYPE = TABLE_ENTRIES
                                                           + ".resource_type";

    /** _more_ */
    public static final String COL_ENTRIES_CREATEDATE = TABLE_ENTRIES
                                                        + ".createdate";

    /** _more_ */
    public static final String COL_ENTRIES_FROMDATE = TABLE_ENTRIES
                                                      + ".fromdate";

    /** _more_ */
    public static final String COL_ENTRIES_TODATE = TABLE_ENTRIES + ".todate";

    /** _more_ */
    public static final String COL_ENTRIES_SOUTH = TABLE_ENTRIES + ".south";

    /** _more_ */
    public static final String COL_ENTRIES_NORTH = TABLE_ENTRIES + ".north";

    /** _more_ */
    public static final String COL_ENTRIES_EAST = TABLE_ENTRIES + ".east";

    /** _more_ */
    public static final String COL_ENTRIES_WEST = TABLE_ENTRIES + ".west";

    /** _more_ */
    public static final String[] ARRAY_ENTRIES = new String[] {
        COL_ENTRIES_ID, COL_ENTRIES_TYPE, COL_ENTRIES_NAME,
        COL_ENTRIES_DESCRIPTION, COL_ENTRIES_PARENT_GROUP_ID,
        COL_ENTRIES_USER_ID, COL_ENTRIES_RESOURCE, COL_ENTRIES_RESOURCE_TYPE,
        COL_ENTRIES_CREATEDATE, COL_ENTRIES_FROMDATE, COL_ENTRIES_TODATE,
        COL_ENTRIES_SOUTH, COL_ENTRIES_NORTH, COL_ENTRIES_EAST,
        COL_ENTRIES_WEST
    };

    /** _more_ */
    public static final String COLUMNS_ENTRIES = SqlUtil.comma(ARRAY_ENTRIES);

    /** _more_ */
    public static final String NODOT_COLUMNS_ENTRIES =
        SqlUtil.commaNoDot(ARRAY_ENTRIES);

    /** _more_ */
    public static final String INSERT_ENTRIES =
        SqlUtil.makeInsert(TABLE_ENTRIES, NODOT_COLUMNS_ENTRIES,
                           SqlUtil.getQuestionMarks(ARRAY_ENTRIES.length));

    /** _more_ */
    public static final String UPDATE_ENTRIES =
        SqlUtil.makeUpdate(TABLE_ENTRIES, COL_ENTRIES_ID, ARRAY_ENTRIES);


    /** _more_ */
    public static final String TABLE_GLOBALS = "globals";

    /** _more_ */
    public static final String COL_GLOBALS_NAME = TABLE_GLOBALS + ".name";

    /** _more_ */
    public static final String COL_GLOBALS_VALUE = TABLE_GLOBALS + ".value";

    /** _more_ */
    public static final String[] ARRAY_GLOBALS = new String[] {
                                                     COL_GLOBALS_NAME,
            COL_GLOBALS_VALUE };

    /** _more_ */
    public static final String COLUMNS_GLOBALS = SqlUtil.comma(ARRAY_GLOBALS);

    /** _more_ */
    public static final String NODOT_COLUMNS_GLOBALS =
        SqlUtil.commaNoDot(ARRAY_GLOBALS);

    /** _more_ */
    public static final String INSERT_GLOBALS =
        SqlUtil.makeInsert(TABLE_GLOBALS, NODOT_COLUMNS_GLOBALS,
                           SqlUtil.getQuestionMarks(ARRAY_GLOBALS.length));



    /** _more_ */
    public static final String TABLE_METADATA = "metadata";

    /** _more_ */
    public static final String COL_METADATA_ID = TABLE_METADATA + ".id";

    /** _more_ */
    public static final String COL_METADATA_ENTRY_ID = TABLE_METADATA
                                                       + ".entry_id";

    /** _more_ */
    public static final String COL_METADATA_TYPE = TABLE_METADATA + ".type";

    /** _more_ */
    public static final String COL_METADATA_ATTR1 = TABLE_METADATA + ".attr1";

    /** _more_ */
    public static final String COL_METADATA_ATTR2 = TABLE_METADATA + ".attr2";

    /** _more_ */
    public static final String COL_METADATA_ATTR3 = TABLE_METADATA + ".attr3";

    /** _more_ */
    public static final String COL_METADATA_ATTR4 = TABLE_METADATA + ".attr4";

    /** _more_ */
    public static final String[] ARRAY_METADATA = new String[] {
        COL_METADATA_ID, COL_METADATA_ENTRY_ID, COL_METADATA_TYPE,
        COL_METADATA_ATTR1, COL_METADATA_ATTR2, COL_METADATA_ATTR3,
        COL_METADATA_ATTR4
    };

    /** _more_ */
    public static final String COLUMNS_METADATA =
        SqlUtil.comma(ARRAY_METADATA);

    /** _more_ */
    public static final String NODOT_COLUMNS_METADATA =
        SqlUtil.commaNoDot(ARRAY_METADATA);

    /** _more_ */
    public static final String INSERT_METADATA =
        SqlUtil.makeInsert(TABLE_METADATA, NODOT_COLUMNS_METADATA,
                           SqlUtil.getQuestionMarks(ARRAY_METADATA.length));



    /** _more_ */
    public static final String TABLE_PERMISSIONS = "permissions";

    /** _more_ */
    public static final String COL_PERMISSIONS_ENTRY_ID = TABLE_PERMISSIONS
                                                          + ".entry_id";

    /** _more_ */
    public static final String COL_PERMISSIONS_ACTION = TABLE_PERMISSIONS
                                                        + ".action";

    /** _more_ */
    public static final String COL_PERMISSIONS_ROLE = TABLE_PERMISSIONS
                                                      + ".role";

    /** _more_ */
    public static final String[] ARRAY_PERMISSIONS =
        new String[] { COL_PERMISSIONS_ENTRY_ID,
                       COL_PERMISSIONS_ACTION, COL_PERMISSIONS_ROLE };

    /** _more_ */
    public static final String COLUMNS_PERMISSIONS =
        SqlUtil.comma(ARRAY_PERMISSIONS);

    /** _more_ */
    public static final String NODOT_COLUMNS_PERMISSIONS =
        SqlUtil.commaNoDot(ARRAY_PERMISSIONS);

    /** _more_ */
    public static final String INSERT_PERMISSIONS =
        SqlUtil.makeInsert(
            TABLE_PERMISSIONS, NODOT_COLUMNS_PERMISSIONS,
            SqlUtil.getQuestionMarks(ARRAY_PERMISSIONS.length));





    /** _more_ */
    public static final String TABLE_USER_ROLES = "user_roles";

    /** _more_ */
    public static final String COL_USER_ROLES_USER_ID = TABLE_USER_ROLES
                                                        + ".user_id";

    /** _more_ */
    public static final String COL_USER_ROLES_ROLE = TABLE_USER_ROLES
                                                     + ".role";

    /** _more_ */
    public static final String[] ARRAY_USER_ROLES =
        new String[] { COL_USER_ROLES_USER_ID,
                       COL_USER_ROLES_ROLE };

    /** _more_ */
    public static final String COLUMNS_USER_ROLES =
        SqlUtil.comma(ARRAY_USER_ROLES);

    /** _more_ */
    public static final String NODOT_COLUMNS_USER_ROLES =
        SqlUtil.commaNoDot(ARRAY_USER_ROLES);

    /** _more_ */
    public static final String INSERT_USER_ROLES =
        SqlUtil.makeInsert(TABLE_USER_ROLES, NODOT_COLUMNS_USER_ROLES,
                           SqlUtil.getQuestionMarks(ARRAY_USER_ROLES.length));



    /** _more_ */
    public static final String TABLE_USERROLES = "userroles";

    /** _more_ */
    public static final String COL_USERROLES_USER_ID = TABLE_USERROLES
                                                       + ".user_id";

    /** _more_ */
    public static final String COL_USERROLES_ROLE = TABLE_USERROLES + ".role";

    /** _more_ */
    public static final String[] ARRAY_USERROLES = new String[] {
                                                       COL_USERROLES_USER_ID,
            COL_USERROLES_ROLE };

    /** _more_ */
    public static final String COLUMNS_USERROLES =
        SqlUtil.comma(ARRAY_USERROLES);

    /** _more_ */
    public static final String NODOT_COLUMNS_USERROLES =
        SqlUtil.commaNoDot(ARRAY_USERROLES);

    /** _more_ */
    public static final String INSERT_USERROLES =
        SqlUtil.makeInsert(TABLE_USERROLES, NODOT_COLUMNS_USERROLES,
                           SqlUtil.getQuestionMarks(ARRAY_USERROLES.length));



    /** _more_ */
    public static final String TABLE_USERS = "users";

    /** _more_ */
    public static final String COL_USERS_ID = TABLE_USERS + ".id";

    /** _more_ */
    public static final String COL_USERS_NAME = TABLE_USERS + ".name";

    /** _more_ */
    public static final String COL_USERS_EMAIL = TABLE_USERS + ".email";

    /** _more_ */
    public static final String COL_USERS_QUESTION = TABLE_USERS + ".question";

    /** _more_ */
    public static final String COL_USERS_ANSWER = TABLE_USERS + ".answer";

    /** _more_ */
    public static final String COL_USERS_PASSWORD = TABLE_USERS + ".password";

    /** _more_ */
    public static final String COL_USERS_ADMIN = TABLE_USERS + ".admin";

    /** _more_ */
    public static final String COL_USERS_LANGUAGE = TABLE_USERS + ".language";

    /** _more_ */
    public static final String[] ARRAY_USERS = new String[] {
        COL_USERS_ID, COL_USERS_NAME, COL_USERS_EMAIL, COL_USERS_QUESTION,
        COL_USERS_ANSWER, COL_USERS_PASSWORD, COL_USERS_ADMIN,
        COL_USERS_LANGUAGE
    };

    /** _more_ */
    public static final String COLUMNS_USERS = SqlUtil.comma(ARRAY_USERS);

    /** _more_ */
    public static final String NODOT_COLUMNS_USERS =
        SqlUtil.commaNoDot(ARRAY_USERS);

    /** _more_ */
    public static final String INSERT_USERS =
        SqlUtil.makeInsert(TABLE_USERS, NODOT_COLUMNS_USERS,
                           SqlUtil.getQuestionMarks(ARRAY_USERS.length));

    /** end generated table definitions */

    //J+
}

