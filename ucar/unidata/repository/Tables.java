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


import ucar.unidata.sql.SqlUtil;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Tables {

    /**
     * Class ENTRIES _more_
     *
     *
     * @author IDV Development Team
     */
    public static class ENTRIES {

        /** _more_          */
        public static final String NAME = "entries";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_TYPE = NAME + ".type";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String COL_DESCRIPTION = NAME + ".description";

        /** _more_          */
        public static final String COL_PARENT_GROUP_ID = NAME
                                                         + ".parent_group_id";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_RESOURCE = NAME + ".resource";

        /** _more_          */
        public static final String COL_RESOURCE_TYPE = NAME
                                                       + ".resource_type";

        /** _more_          */
        public static final String COL_DATATYPE = NAME + ".datatype";

        /** _more_          */
        public static final String COL_CREATEDATE = NAME + ".createdate";

        /** _more_          */
        public static final String COL_FROMDATE = NAME + ".fromdate";

        /** _more_          */
        public static final String COL_TODATE = NAME + ".todate";

        /** _more_          */
        public static final String COL_SOUTH = NAME + ".south";

        /** _more_          */
        public static final String COL_NORTH = NAME + ".north";

        /** _more_          */
        public static final String COL_EAST = NAME + ".east";

        /** _more_          */
        public static final String COL_WEST = NAME + ".west";

        /** _more_          */
        public static final String[] ARRAY = new String[] {
            COL_ID, COL_TYPE, COL_NAME, COL_DESCRIPTION, COL_PARENT_GROUP_ID,
            COL_USER_ID, COL_RESOURCE, COL_RESOURCE_TYPE, COL_DATATYPE,
            COL_CREATEDATE, COL_FROMDATE, COL_TODATE, COL_SOUTH, COL_NORTH,
            COL_EAST, COL_WEST
        };

        /** _more_          */
        public static final String UPDATE = SqlUtil.makeUpdate(NAME, COL_ID,
                                                ARRAY);

        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class ANCESTORS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class ANCESTORS {

        /** _more_          */
        public static final String NAME = "ancestors";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_ANCESTOR_ID = NAME + ".ancestor_id";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ID,
                COL_ANCESTOR_ID };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class METADATA _more_
     *
     *
     * @author IDV Development Team
     */
    public static class METADATA {

        /** _more_          */
        public static final String NAME = "metadata";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_          */
        public static final String COL_TYPE = NAME + ".type";

        /** _more_          */
        public static final String COL_INHERITED = NAME + ".inherited";

        /** _more_          */
        public static final String COL_ATTR1 = NAME + ".attr1";

        /** _more_          */
        public static final String COL_ATTR2 = NAME + ".attr2";

        /** _more_          */
        public static final String COL_ATTR3 = NAME + ".attr3";

        /** _more_          */
        public static final String COL_ATTR4 = NAME + ".attr4";

        /** _more_          */
        public static final String[] ARRAY = new String[] {
            COL_ID, COL_ENTRY_ID, COL_TYPE, COL_INHERITED, COL_ATTR1,
            COL_ATTR2, COL_ATTR3, COL_ATTR4
        };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class COMMENTS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class COMMENTS {

        /** _more_          */
        public static final String NAME = "comments";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_DATE = NAME + ".date";

        /** _more_          */
        public static final String COL_SUBJECT = NAME + ".subject";

        /** _more_          */
        public static final String COL_COMMENT = NAME + ".comment";

        /** _more_          */
        public static final String[] ARRAY = new String[] {
            COL_ID, COL_ENTRY_ID, COL_USER_ID, COL_DATE, COL_SUBJECT,
            COL_COMMENT
        };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class ASSOCIATIONS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class ASSOCIATIONS {

        /** _more_          */
        public static final String NAME = "associations";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String COL_TYPE = NAME + ".type";

        /** _more_          */
        public static final String COL_FROM_ENTRY_ID = NAME
                                                       + ".from_entry_id";

        /** _more_          */
        public static final String COL_TO_ENTRY_ID = NAME + ".to_entry_id";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ID, COL_NAME,
                COL_TYPE, COL_FROM_ENTRY_ID, COL_TO_ENTRY_ID };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class USERS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class USERS {

        /** _more_          */
        public static final String NAME = "users";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String COL_EMAIL = NAME + ".email";

        /** _more_          */
        public static final String COL_QUESTION = NAME + ".question";

        /** _more_          */
        public static final String COL_ANSWER = NAME + ".answer";

        /** _more_          */
        public static final String COL_PASSWORD = NAME + ".password";

        /** _more_          */
        public static final String COL_ADMIN = NAME + ".admin";

        /** _more_          */
        public static final String COL_LANGUAGE = NAME + ".language";

        /** _more_          */
        public static final String COL_TEMPLATE = NAME + ".template";

        /** _more_          */
        public static final String[] ARRAY = new String[] {
            COL_ID, COL_NAME, COL_EMAIL, COL_QUESTION, COL_ANSWER,
            COL_PASSWORD, COL_ADMIN, COL_LANGUAGE, COL_TEMPLATE
        };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;


    /**
     * Class USER_ACTIVITY _more_
     *
     *
     * @author IDV Development Team
     */
    public static class USER_ACTIVITY {

        /** _more_          */
        public static final String NAME = "user_activity";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_DATE = NAME + ".date";

        /** _more_          */
        public static final String COL_WHAT = NAME + ".what";

        /** _more_          */
        public static final String COL_EXTRA = NAME + ".extra";

        /** _more_          */
        public static final String COL_IPADDRESS = NAME + ".ipaddress";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_USER_ID,
                COL_DATE, COL_WHAT, COL_EXTRA, COL_IPADDRESS };




        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;


    /**
     * Class SESSIONS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class SESSIONS {

        /** _more_          */
        public static final String NAME = "sessions";

        /** _more_          */
        public static final String COL_SESSION_ID = NAME + ".session_id";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_CREATE_DATE = NAME + ".create_date";

        /** _more_          */
        public static final String COL_LAST_ACTIVE_DATE =
            NAME + ".last_active_date";

        /** _more_          */
        public static final String COL_EXTRA = NAME + ".extra";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_SESSION_ID,
                COL_USER_ID, COL_CREATE_DATE, COL_LAST_ACTIVE_DATE,
                COL_EXTRA };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;




    /**
     * Class FAVORITES _more_
     *
     *
     * @author IDV Development Team
     */
    public static class FAVORITES {

        /** _more_          */
        public static final String NAME = "favorites";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String COL_CATEGORY = NAME + ".category";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ID,
                COL_USER_ID, COL_ENTRY_ID, COL_NAME, COL_CATEGORY };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;





    /**
     * Class USERROLES _more_
     *
     *
     * @author IDV Development Team
     */
    public static class USERROLES {

        /** _more_          */
        public static final String NAME = "userroles";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_ROLE = NAME + ".role";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_USER_ID,
                COL_ROLE };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class PERMISSIONS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class PERMISSIONS {

        /** _more_          */
        public static final String NAME = "permissions";

        /** _more_          */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_          */
        public static final String COL_ACTION = NAME + ".action";

        /** _more_          */
        public static final String COL_ROLE = NAME + ".role";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ENTRY_ID,
                COL_ACTION, COL_ROLE };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class HARVESTERS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class HARVESTERS {

        /** _more_          */
        public static final String NAME = "harvesters";

        /** _more_          */
        public static final String COL_ID = NAME + ".id";

        /** _more_          */
        public static final String COL_CLASS = NAME + ".class";

        /** _more_          */
        public static final String COL_CONTENT = NAME + ".content";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ID, COL_CLASS,
                COL_CONTENT };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class GLOBALS _more_
     *
     *
     * @author IDV Development Team
     */
    public static class GLOBALS {

        /** _more_          */
        public static final String NAME = "globals";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String COL_VALUE = NAME + ".value";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_NAME,
                COL_VALUE };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class WIKIPAGEHISTORY _more_
     *
     *
     * @author IDV Development Team
     */
    public static class WIKIPAGEHISTORY {

        /** _more_          */
        public static final String NAME = "wikipagehistory";

        /** _more_          */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_          */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_          */
        public static final String COL_DATE = NAME + ".date";

        /** _more_          */
        public static final String COL_DESCRIPTION = NAME + ".description";

        /** _more_          */
        public static final String COL_WIKITEXT = NAME + ".wikitext";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_ENTRY_ID,
                COL_USER_ID, COL_DATE, COL_DESCRIPTION, COL_WIKITEXT };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;

    /**
     * Class DUMMY _more_
     *
     *
     * @author IDV Development Team
     */
    public static class DUMMY {

        /** _more_          */
        public static final String NAME = "dummy";

        /** _more_          */
        public static final String COL_NAME = NAME + ".name";

        /** _more_          */
        public static final String[] ARRAY = new String[] { COL_NAME };


        /** _more_          */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_          */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_          */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;


}

