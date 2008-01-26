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


    /** _more_ */
    public static final int MAX_ROWS = 500;
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
    public static final String INSERT_ASSOCIATIONS =
        SqlUtil.makeInsert(
            TABLE_ASSOCIATIONS, COLUMNS_ASSOCIATIONS,
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
    public static final String INSERT_COMMENTS =
        SqlUtil.makeInsert(TABLE_COMMENTS, COLUMNS_COMMENTS,
                           SqlUtil.getQuestionMarks(ARRAY_COMMENTS.length));



    /** _more_ */
    public static final String TABLE_DUMMY = "dummy";

    /** _more_ */
    public static final String COL_DUMMY_NAME = TABLE_DUMMY + ".name";

    /** _more_ */
    public static final String[] ARRAY_DUMMY = new String[] {
                                                   COL_DUMMY_NAME };

    /** _more_ */
    public static final String COLUMNS_DUMMY =
        SqlUtil.comma(ARRAY_DUMMY);

    /** _more_ */
    public static final String INSERT_DUMMY =
        SqlUtil.makeInsert(TABLE_DUMMY, COLUMNS_DUMMY,
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
    public static final String COLUMNS_ENTRIES =
        SqlUtil.comma(ARRAY_ENTRIES);

    /** _more_ */
    public static final String INSERT_ENTRIES =
        SqlUtil.makeInsert(TABLE_ENTRIES, COLUMNS_ENTRIES,
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
    public static final String COLUMNS_GLOBALS =
        SqlUtil.comma(ARRAY_GLOBALS);

    /** _more_ */
    public static final String INSERT_GLOBALS =
        SqlUtil.makeInsert(TABLE_GLOBALS, COLUMNS_GLOBALS,
                           SqlUtil.getQuestionMarks(ARRAY_GLOBALS.length));



    /** _more_ */
    public static final String TABLE_LEVEL2RADAR = "level2radar";

    /** _more_ */
    public static final String COL_LEVEL2RADAR_ID = TABLE_LEVEL2RADAR + ".id";

    /** _more_ */
    public static final String COL_LEVEL2RADAR_STATION = TABLE_LEVEL2RADAR
                                                         + ".station";

    /** _more_ */
    public static final String[] ARRAY_LEVEL2RADAR = new String[] {
                                                         COL_LEVEL2RADAR_ID,
            COL_LEVEL2RADAR_STATION };

    /** _more_ */
    public static final String COLUMNS_LEVEL2RADAR =
        SqlUtil.comma(ARRAY_LEVEL2RADAR);

    /** _more_ */
    public static final String INSERT_LEVEL2RADAR =
        SqlUtil.makeInsert(
            TABLE_LEVEL2RADAR, COLUMNS_LEVEL2RADAR,
            SqlUtil.getQuestionMarks(ARRAY_LEVEL2RADAR.length));



    /** _more_ */
    public static final String TABLE_LEVEL3RADAR = "level3radar";

    /** _more_ */
    public static final String COL_LEVEL3RADAR_ID = TABLE_LEVEL3RADAR + ".id";

    /** _more_ */
    public static final String COL_LEVEL3RADAR_STATION = TABLE_LEVEL3RADAR
                                                         + ".station";

    /** _more_ */
    public static final String COL_LEVEL3RADAR_PRODUCT = TABLE_LEVEL3RADAR
                                                         + ".product";

    /** _more_ */
    public static final String[] ARRAY_LEVEL3RADAR = new String[] {
                                                         COL_LEVEL3RADAR_ID,
            COL_LEVEL3RADAR_STATION, COL_LEVEL3RADAR_PRODUCT };

    /** _more_ */
    public static final String COLUMNS_LEVEL3RADAR =
        SqlUtil.comma(ARRAY_LEVEL3RADAR);

    /** _more_ */
    public static final String INSERT_LEVEL3RADAR =
        SqlUtil.makeInsert(
            TABLE_LEVEL3RADAR, COLUMNS_LEVEL3RADAR,
            SqlUtil.getQuestionMarks(ARRAY_LEVEL3RADAR.length));



    /** _more_ */
    public static final String TABLE_METADATA = "metadata";

    /** _more_ */
    public static final String COL_METADATA_ID = TABLE_METADATA + ".id";

    /** _more_ */
    public static final String COL_METADATA_TYPE = TABLE_METADATA + ".type";

    /** _more_ */
    public static final String COL_METADATA_NAME = TABLE_METADATA + ".name";

    /** _more_ */
    public static final String COL_METADATA_CONTENT = TABLE_METADATA
                                                      + ".content";

    /** _more_ */
    public static final String[] ARRAY_METADATA = new String[] {
                                                      COL_METADATA_ID,
            COL_METADATA_TYPE, COL_METADATA_NAME, COL_METADATA_CONTENT };

    /** _more_ */
    public static final String COLUMNS_METADATA =
        SqlUtil.comma(ARRAY_METADATA);

    /** _more_ */
    public static final String INSERT_METADATA =
        SqlUtil.makeInsert(TABLE_METADATA, COLUMNS_METADATA,
                           SqlUtil.getQuestionMarks(ARRAY_METADATA.length));



    /** _more_ */
    public static final String TABLE_MODEL = "model";

    /** _more_ */
    public static final String COL_MODEL_ID = TABLE_MODEL + ".id";

    /** _more_ */
    public static final String COL_MODEL_MODELGROUP = TABLE_MODEL
                                                      + ".modelgroup";

    /** _more_ */
    public static final String COL_MODEL_MODELRUN = TABLE_MODEL + ".modelrun";

    /** _more_ */
    public static final String[] ARRAY_MODEL = new String[] { COL_MODEL_ID,
            COL_MODEL_MODELGROUP, COL_MODEL_MODELRUN };

    /** _more_ */
    public static final String COLUMNS_MODEL =
        SqlUtil.comma(ARRAY_MODEL);

    /** _more_ */
    public static final String INSERT_MODEL =
        SqlUtil.makeInsert(TABLE_MODEL, COLUMNS_MODEL,
                           SqlUtil.getQuestionMarks(ARRAY_MODEL.length));



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
    public static final String INSERT_PERMISSIONS =
        SqlUtil.makeInsert(
            TABLE_PERMISSIONS, COLUMNS_PERMISSIONS,
            SqlUtil.getQuestionMarks(ARRAY_PERMISSIONS.length));



    /** _more_ */
    public static final String TABLE_SATELLITE = "satellite";

    /** _more_ */
    public static final String COL_SATELLITE_ID = TABLE_SATELLITE + ".id";

    /** _more_ */
    public static final String COL_SATELLITE_PLATFORM = TABLE_SATELLITE
                                                        + ".platform";

    /** _more_ */
    public static final String COL_SATELLITE_RESOLUTION = TABLE_SATELLITE
                                                          + ".resolution";

    /** _more_ */
    public static final String COL_SATELLITE_PRODUCT = TABLE_SATELLITE
                                                       + ".product";

    /** _more_ */
    public static final String[] ARRAY_SATELLITE = new String[] {
                                                       COL_SATELLITE_ID,
            COL_SATELLITE_PLATFORM, COL_SATELLITE_RESOLUTION,
            COL_SATELLITE_PRODUCT };

    /** _more_ */
    public static final String COLUMNS_SATELLITE =
        SqlUtil.comma(ARRAY_SATELLITE);

    /** _more_ */
    public static final String INSERT_SATELLITE =
        SqlUtil.makeInsert(TABLE_SATELLITE, COLUMNS_SATELLITE,
                           SqlUtil.getQuestionMarks(ARRAY_SATELLITE.length));



    /** _more_ */
    public static final String TABLE_TAGS = "tags";

    /** _more_ */
    public static final String COL_TAGS_NAME = TABLE_TAGS + ".name";

    /** _more_ */
    public static final String COL_TAGS_ENTRY_ID = TABLE_TAGS + ".entry_id";

    /** _more_ */
    public static final String[] ARRAY_TAGS = new String[] { COL_TAGS_NAME,
            COL_TAGS_ENTRY_ID };

    /** _more_ */
    public static final String COLUMNS_TAGS = SqlUtil.comma(ARRAY_TAGS);

    /** _more_ */
    public static final String INSERT_TAGS =
        SqlUtil.makeInsert(TABLE_TAGS, COLUMNS_TAGS,
                           SqlUtil.getQuestionMarks(ARRAY_TAGS.length));



    /** _more_ */
    public static final String TABLE_TESTIT = "testit";

    /** _more_ */
    public static final String COL_TESTIT_ID = TABLE_TESTIT + ".id";

    /** _more_ */
    public static final String COL_TESTIT_FRUIT = TABLE_TESTIT + ".fruit";

    /** _more_ */
    public static final String COL_TESTIT_FLAG = TABLE_TESTIT + ".flag";

    /** _more_ */
    public static final String COL_TESTIT_NUMBER = TABLE_TESTIT + ".number";

    /** _more_ */
    public static final String[] ARRAY_TESTIT = new String[] { COL_TESTIT_ID,
            COL_TESTIT_FRUIT, COL_TESTIT_FLAG, COL_TESTIT_NUMBER };

    /** _more_ */
    public static final String COLUMNS_TESTIT =
        SqlUtil.comma(ARRAY_TESTIT);

    /** _more_ */
    public static final String INSERT_TESTIT =
        SqlUtil.makeInsert(TABLE_TESTIT, COLUMNS_TESTIT,
                           SqlUtil.getQuestionMarks(ARRAY_TESTIT.length));



    /** _more_ */
    public static final String TABLE_TESTIT2 = "testit2";

    /** _more_ */
    public static final String COL_TESTIT2_ID = TABLE_TESTIT2 + ".id";

    /** _more_ */
    public static final String COL_TESTIT2_GCMD = TABLE_TESTIT2 + ".gcmd";

    /** _more_ */
    public static final String[] ARRAY_TESTIT2 = new String[] {
                                                     COL_TESTIT2_ID,
            COL_TESTIT2_GCMD };

    /** _more_ */
    public static final String COLUMNS_TESTIT2 =
        SqlUtil.comma(ARRAY_TESTIT2);

    /** _more_ */
    public static final String INSERT_TESTIT2 =
        SqlUtil.makeInsert(TABLE_TESTIT2, COLUMNS_TESTIT2,
                           SqlUtil.getQuestionMarks(ARRAY_TESTIT2.length));



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
    public static final String INSERT_USERROLES =
        SqlUtil.makeInsert(TABLE_USERROLES, COLUMNS_USERROLES,
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
    public static final String[] ARRAY_USERS = new String[] {
        COL_USERS_ID, COL_USERS_NAME, COL_USERS_EMAIL, COL_USERS_QUESTION,
        COL_USERS_ANSWER, COL_USERS_PASSWORD, COL_USERS_ADMIN
    };

    /** _more_ */
    public static final String COLUMNS_USERS =
        SqlUtil.comma(ARRAY_USERS);

    /** _more_ */
    public static final String INSERT_USERS =
        SqlUtil.makeInsert(TABLE_USERS, COLUMNS_USERS,
                           SqlUtil.getQuestionMarks(ARRAY_USERS.length));

    /** end generated table definitions */

    //J+

}

