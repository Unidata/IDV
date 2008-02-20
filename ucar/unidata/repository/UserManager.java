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
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;



import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class UserManager extends RepositoryManager {

    /** _more_          */
    public static final String ROLE_ANY = "any";


    /** _more_ */
    private Hashtable session = new Hashtable();


    /** _more_ */
    public RequestUrl URL_USER_LOGIN = new RequestUrl(this, "/user/login");


    /** _more_ */
    public RequestUrl URL_USER_LOGOUT = new RequestUrl(this, "/user/logout");


    /** _more_ */
    public RequestUrl URL_USER_HOME = new RequestUrl(this, "/user/home");

    /** _more_ */
    public RequestUrl URL_USER_SETTINGS = new RequestUrl(this,
                                              "/user/settings", "Settings");

    /** _more_ */
    public RequestUrl URL_USER_CART = new RequestUrl(this, "/user/cart",
                                          "Data Cart");

    /** _more_ */
    public RequestUrl URL_USER_LIST = new RequestUrl(this, "/user/list",
                                          "Users");

    /** _more_ */
    public RequestUrl URL_USER_EDIT = new RequestUrl(this, "/user/edit",
                                          "Users");

    /** _more_ */
    public RequestUrl URL_USER_NEW = new RequestUrl(this, "/user/new");


    /** _more_ */
    protected RequestUrl[] userUrls = { URL_USER_SETTINGS, URL_USER_CART };


    /** _more_ */
    public static final String ARG_USER_DELETE_CONFIRM =
        "user.delete.confirm";

    /** _more_ */
    public static final String ARG_USER_DELETE = "user.delete";



    /** _more_ */
    public static final String ARG_USER_CANCEL = "user.cancel";

    /** _more_ */
    public static final String ARG_USER_CHANGE = "user.change";

    /** _more_ */
    public static final String ARG_USER_NEW = "user.new";

    /** _more_ */
    public static final String ARG_USER_ID = "user.id";

    /** _more_ */
    public static final String ARG_USER_NAME = "user.name";

    /** _more_ */
    public static final String ARG_USER_ROLES = "user.roles";

    /** _more_ */
    public static final String ARG_USER_PASSWORD1 = "user.password1";

    /** _more_ */
    public static final String ARG_USER_PASSWORD2 = "user.password2";

    /** _more_ */
    public static final String ARG_USER_EMAIL = "user.email";

    /** _more_ */
    public static final String ARG_USER_QUESTION = "user.question";

    /** _more_ */
    public static final String ARG_USER_ANSWER = "user.answer";

    /** _more_ */
    public static final String ARG_USER_ADMIN = "user.admin";




    /** _more_ */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();

    /** _more_ */
    private Hashtable userCart = new Hashtable();

    /** _more_ */
    private List ipUserList = new ArrayList();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public UserManager(Repository repository) {
        super(repository);
        //        ipUserList.add("128.117.156.*");
        //        ipUserList.add("jeffmc");

    }


    /**
     * _more_
     *
     * @param password _more_
     *
     * @return _more_
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
            return XmlUtil.encodeBase64(md.digest()).trim();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    protected void checkSession(Request request) throws Exception {

        String cookie = request.getHeaderArg("Cookie");
        User   user   = request.getUser();

        if (cookie != null) {
            List toks = StringUtil.split(cookie, ";", true, true);
            for (int i = 0; i < toks.size(); i++) {
                String tok     = (String) toks.get(i);
                List   subtoks = StringUtil.split(tok, "=", true, true);
                if (subtoks.size() != 2) {
                    continue;
                }
                String cookieName  = (String) subtoks.get(0);
                String cookieValue = (String) subtoks.get(1);
                if (cookieName.equals("repository-session")) {
                    request.setSessionId(cookieValue);
                    if (user == null) {
                        user = (User) session.get(request.getSessionId());
                        if (user != null) {
                            break;
                        }
                    }
                }
            }
        }


        //Check for url auth
        if ((user == null) && request.exists(ARG_AUTH_USER)
                && request.exists(ARG_AUTH_PASSWORD)) {
            String userId   = request.getString(ARG_AUTH_USER, "");
            String password = request.getString(ARG_AUTH_PASSWORD, "");
            user = findUser(userId, false);
            if (user == null) {
                throw new IllegalArgumentException("Unknown user:" + userId);
            }
            if ( !user.getPassword().equals(hashPassword(password))) {
                throw new IllegalArgumentException("Incorrect password");
            }
            setUserSession(request, user);
        }


        //Check for basic auth
        if (user == null) {
            String auth =
                (String) request.getHttpHeaderArgs().get("Authorization");
            if (auth != null) {
                auth = auth.trim();
                //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
                if (auth.startsWith("Basic")) {
                    auth = new String(
                        XmlUtil.decodeBase64(auth.substring(5).trim()));
                    String[] toks = StringUtil.split(auth, ":", 2);
                    if (toks.length == 2) {
                        user = findUser(toks[0], false);
                        if (user == null) {
                            throw new Repository.AccessException(
                                "Unknown user:" + toks[0]);
                        }
                        if ( !user.getPassword().equals(
                                hashPassword(toks[1]))) {
                            throw new Repository.AccessException(
                                "Incorrect password");
                        }
                    }
                    setUserSession(request, user);
                }
            }
        }



        if (user == null) {
            String requestIp = request.getIp();
            if (requestIp != null) {
                for (int i = 0; i < ipUserList.size(); i += 2) {
                    String ip       = (String) ipUserList.get(i);
                    String userName = (String) ipUserList.get(i + 1);
                    if (requestIp.matches(ip)) {
                        user = findUser(userName, false);
                        if (user == null) {
                            user = new User(userName, false);
                            makeOrUpdateUser(user, false);
                        }
                    }
                }
            }
        }


        if (request.getSessionId() == null) {
            request.setSessionId(getSessionId());
        }


        if (user == null) {
            user = getUserManager().getAnonymousUser();
        }

        request.setUser(user);



    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getSessionId() {
        return getRepository().getGUID() + "_" + Math.random();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void setUserSession(Request request, User user)
            throws Exception {
        if (request.getSessionId() == null) {
            request.setSessionId(getSessionId());
        }
        session.put(request.getSessionId(), user);
        request.setUser(user);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    protected void removeUserSession(Request request) throws Exception {
        if (request.getSessionId() != null) {
            session.remove(request.getSessionId());
        }
        request.setUser(getUserManager().getAnonymousUser());
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected boolean isRequestOk(Request request) {
        if (getProperty(PROP_ACCESS_ADMINONLY, false)
                && !request.getUser().getAdmin()) {
            if ( !request.getRequestPath().startsWith(
                    getRepository().getUrlBase() + "/user/")) {
                return false;
            }
        }

        if (getProperty(PROP_ACCESS_REQUIRELOGIN, false)
                && request.getUser().getAnonymous()) {
            if ( !request.getRequestPath().startsWith(
                    getRepository().getUrlBase() + "/user/")) {
                return false;
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String makeLoginForm(Request request) {
        return makeLoginForm(request, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String makeLoginForm(Request request, String extra) {
        StringBuffer sb   = new StringBuffer("<h3>Please login</h3>");
        String       name = request.getString(ARG_USER_NAME, "");
        sb.append(HtmlUtil.form(URL_USER_LOGIN));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry("User:",
                                     HtmlUtil.input(ARG_USER_NAME, name)));
        sb.append(HtmlUtil.formEntry("Password:",
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));
        sb.append(extra);

        sb.append(HtmlUtil.formEntry("", HtmlUtil.submit("Login")));

        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.formClose());
        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getDefaultUser() throws Exception {
        makeUserIfNeeded(new User("default", "Default User", false));
        return findUser("default");
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getAnonymousUser() throws Exception {
        return new User();
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUser(String id) throws Exception {
        return findUser(id, false);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param userDefaultIfNotFound _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUser(String id, boolean userDefaultIfNotFound)
            throws Exception {
        if (id == null) {
            return null;
        }
        User user = userMap.get(id);
        if (user != null) {
            return user;
        }
        String query = SqlUtil.makeSelect(COLUMNS_USERS,
                                          Misc.newList(TABLE_USERS),
                                          SqlUtil.eq(COL_USERS_ID,
                                              SqlUtil.quote(id)));
        ResultSet results =
            getDatabaseManager().execute(query).getResultSet();
        if ( !results.next()) {
            //            throw new IllegalArgumentException ("Could not find  user id:" + id + " sql:" + query);
            if (userDefaultIfNotFound) {
                return getDefaultUser();
            }
            return null;
        } else {
            user = getUser(results);
        }

        userMap.put(user.getId(), user);
        return user;
    }



    /**
     * _more_
     *
     * @param user _more_
     * @param updateIfNeeded _more_
     *
     * @throws Exception _more_
     */
    protected void makeOrUpdateUser(User user, boolean updateIfNeeded)
            throws Exception {
        if (getRepository().tableContains(user.getId(), TABLE_USERS,
                                          COL_USERS_ID)) {
            if ( !updateIfNeeded) {
                throw new IllegalArgumentException(
                    "Database already contains user:" + user.getId());
            }
            String query = SqlUtil.makeUpdate(TABLE_USERS, COL_USERS_ID,
                               SqlUtil.quote(user.getId()), new String[] {
                COL_USERS_NAME, COL_USERS_PASSWORD, COL_USERS_EMAIL,
                COL_USERS_QUESTION, COL_USERS_ANSWER, COL_USERS_ADMIN
            }, new String[] {
                SqlUtil.quote(user.getName()),
                SqlUtil.quote(user.getPassword()),
                SqlUtil.quote(user.getEmail()),
                SqlUtil.quote(user.getQuestion()),
                SqlUtil.quote(user.getAnswer()), (user.getAdmin()
                        ? "1"
                        : "0")
            });
            getDatabaseManager().execute(query);
            return;
        }

        getDatabaseManager().executeInsert(INSERT_USERS, new Object[] {
            user.getId(), user.getName(), user.getEmail(), user.getQuestion(),
            user.getAnswer(), user.getPassword(), new Boolean(user.getAdmin())
        });
    }





    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void makeUserIfNeeded(User user) throws Exception {
        if (findUser(user.getId()) == null) {
            makeOrUpdateUser(user, true);
        }
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteUser(User user) throws Exception {
        getDatabaseManager().executeDelete(TABLE_USERS, COL_USERS_ID,
                                           SqlUtil.quote(user.getId()));
        deleteRoles(user);
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteRoles(User user) throws Exception {
        getDatabaseManager().executeDelete(TABLE_USERROLES,
                                           COL_USERROLES_USER_ID,
                                           SqlUtil.quote(user.getId()));
    }


    /*
    protected List<String> getRoles(User user) throws Exception {
        if(user.getRoles() == null) {
        }
        }*/


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @return _more_
     */
    private boolean checkPasswords(Request request, User user) {
        String password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
        String password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
        if (password1.length() > 0) {
            if ( !password1.equals(password2)) {
                return false;
            } else {
                user.setPassword(hashPassword(password1));
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param doAdmin _more_
     *
     * @throws Exception _more_
     */
    private void applyState(Request request, User user, boolean doAdmin)
            throws Exception {
        user.setName(request.getString(ARG_USER_NAME, user.getName()));
        user.setEmail(request.getString(ARG_USER_EMAIL, user.getEmail()));
        user.setQuestion(request.getString(ARG_USER_QUESTION,
                                           user.getQuestion()));
        user.setAnswer(request.getString(ARG_USER_ANSWER, user.getAnswer()));
        if (doAdmin) {
            if ( !request.defined(ARG_USER_ADMIN)) {
                user.setAdmin(false);
            } else {
                user.setAdmin(request.get(ARG_USER_ADMIN, user.getAdmin()));
            }
            List<String> roles =
                StringUtil.split(request.getString(ARG_USER_ROLES, ""), "\n",
                                 true, true);
            deleteRoles(user);
            for (String role : roles) {
                getDatabaseManager().executeInsert(INSERT_USERROLES,
                        new Object[] { user.getId(),
                                       role });
            }
            user.setRoles(roles);
        }
        makeOrUpdateUser(user, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminUserEdit(Request request) throws Exception {

        String userId = request.getUserArg();
        User   user   = findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("Could not find user:"
                    + userId);
        }

        if (request.defined(ARG_USER_DELETE_CONFIRM)) {
            deleteUser(user);
            return new Result(URL_USER_LIST.toString());
        }


        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_USER_CHANGE)) {
            boolean okToChangeUser = true;
            okToChangeUser = checkPasswords(request, user);
            if ( !okToChangeUser) {
                sb.append(getRepository().warning("Incorrect passwords"));
            }

            if (okToChangeUser) {
                applyState(request, user, true);
            }
        }


        sb.append(getRepository().header("User: " + user.getLabel()));
        sb.append("\n<p/>\n");
        sb.append(HtmlUtil.form(URL_USER_EDIT));
        sb.append("\n");
        sb.append(HtmlUtil.hidden(ARG_USER, user.getId()));
        sb.append("\n");
        if (request.defined(ARG_USER_DELETE)) {
            sb.append("Are you sure you want to delete the user?  ");
            sb.append(HtmlUtil.submit("Yes", ARG_USER_DELETE_CONFIRM));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit("Cancel", ARG_USER_CANCEL));
        } else {
            makeUserForm(request, user, sb, true);
            sb.append(HtmlUtil.formEntry("&nbsp;<p>", ""));
            sb.append(
                HtmlUtil.formEntry(
                    "",
                    HtmlUtil.submit("Change User", ARG_USER_CHANGE)
                    + HtmlUtil.space(2)
                    + HtmlUtil.submit("Delete User", ARG_USER_DELETE)));
            sb.append("</table>");
        }
        sb.append("\n</form>\n");
        Result result = new Result("User:" + user.getName(), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param sb _more_
     * @param includeAdmin _more_
     *
     * @throws Exception _more_
     */
    private void makeUserForm(Request request, User user, StringBuffer sb,
                              boolean includeAdmin)
            throws Exception {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry("Name:",
                                     HtmlUtil.input(ARG_USER_NAME,
                                         user.getName())));
        if (includeAdmin) {
            sb.append(HtmlUtil.formEntry("Admin:",
                                         HtmlUtil.checkbox(ARG_USER_ADMIN,
                                             "true", user.getAdmin())));
            String       userRoles = user.getRolesAsString("\n");
            StringBuffer allRoles  = new StringBuffer();
            List         roles     = getRoles();
            allRoles.append(
                "<table border=0 cellspacing=0 cellpadding=0><tr valign=\"top\"><td><b>e.g.:</b></td><td>&nbsp;&nbsp;</td><td>");
            int cnt = 0;
            for (int i = 0; i < roles.size(); i++) {
                if (cnt++ > 4) {
                    allRoles.append("</td><td>&nbsp;&nbsp;</td><td>");
                    cnt = 0;
                }
                allRoles.append("<i>");
                allRoles.append(roles.get(i));
                allRoles.append("</i><br>");
            }
            allRoles.append("</table>\n");

            String roleEntry =
                HtmlUtil.hbox(HtmlUtil.textArea(ARG_USER_ROLES, userRoles, 5,
                    20), allRoles.toString());
            sb.append(HtmlUtil.formEntryTop("Roles:", roleEntry));
        }

        sb.append(HtmlUtil.formEntry("Email:",
                                     HtmlUtil.input(ARG_USER_EMAIL,
                                         user.getEmail())));

        sb.append(HtmlUtil.formEntry("&nbsp;<p>", ""));

        sb.append(HtmlUtil.formEntry("Password:",
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        sb.append(HtmlUtil.formEntry("Password Again:",
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));

    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminUserNew(Request request) throws Exception {
        String       id          = "";
        String       name        = "";
        String       email       = "";
        String       password1   = "";
        String       password2   = "";
        boolean      admin       = false;

        StringBuffer sb          = new StringBuffer();

        StringBuffer errorBuffer = new StringBuffer();
        if (request.exists(ARG_USER_ID)) {
            id        = request.getString(ARG_USER_ID, "").trim();
            name      = request.getString(ARG_USER_NAME, name).trim();
            email     = request.getString(ARG_USER_EMAIL, "").trim();
            password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
            password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
            admin     = request.get(ARG_USER_ADMIN, false);
            boolean okToAdd = true;
            if (id.length() == 0) {
                okToAdd = false;
                errorBuffer.append("Please enter an ID<br>");
            }

            if (password1.length() == 0) {
                okToAdd = false;
                errorBuffer.append("Invalid password<br>");
            } else if ( !password1.equals(password2)) {
                okToAdd = false;
                errorBuffer.append("Invalid password<br>");
            }

            if (findUser(id) != null) {
                okToAdd = false;
                errorBuffer.append("User with given id already exists<br>");
            }

            if (okToAdd) {
                makeOrUpdateUser(new User(id, name, email, "", "",
                                          hashPassword(password1),
                                          admin), false);
                String userEditLink = HtmlUtil.url(URL_USER_EDIT, ARG_USER,
                                          id);
                return new Result(userEditLink);
            }
        }



        if (errorBuffer.toString().length() > 0) {
            sb.append(getRepository().warning(errorBuffer.toString()));
        }
        sb.append(getRepository().header("Create User"));
        sb.append(HtmlUtil.form(URL_USER_NEW));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry("Id:", HtmlUtil.input(ARG_USER_ID, id)));
        sb.append(HtmlUtil.formEntry("Name:",
                                     HtmlUtil.input(ARG_USER_NAME, name)));


        sb.append(HtmlUtil.formEntry("Admin:",
                                     HtmlUtil.checkbox(ARG_USER_ADMIN,
                                         "true", admin)));

        sb.append(HtmlUtil.formEntry("Email:",
                                     HtmlUtil.input(ARG_USER_EMAIL, email)));

        sb.append(HtmlUtil.formEntry("Password:",
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        sb.append(HtmlUtil.formEntry("Password Again:",
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Create User",
                                         ARG_USER_NEW)));
        sb.append("</table>");
        sb.append("\n</form>\n");
        Result result = new Result("New User", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminUserList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getRepository().header("Users"));
        sb.append(HtmlUtil.form(URL_USER_NEW));
        sb.append(HtmlUtil.submit("New User"));
        sb.append("</form>");

        String query = SqlUtil.makeSelect(COLUMNS_USERS,
                                          Misc.newList(TABLE_USERS));

        SqlUtil.Iterator iter =
            SqlUtil.getIterator(getDatabaseManager().execute(query));
        ResultSet  results;

        List<User> users = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                users.add(getUser(results));
            }
        }
        sb.append("<table>");
        sb.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    HtmlUtil.bold("ID") + HtmlUtil.space(2),
                    HtmlUtil.bold("Name") + HtmlUtil.space(2),
                    HtmlUtil.bold("Roles") + HtmlUtil.space(2),
                    HtmlUtil.bold("Email") + HtmlUtil.space(2),
                    HtmlUtil.bold("Admin?") + HtmlUtil.space(2))));

        for (User user : users) {
            String userEditLink = HtmlUtil.href(HtmlUtil.url(URL_USER_EDIT,
                                      ARG_USER, user.getId()), user.getId());

            String row = (user.getAdmin()
                          ? "<tr valign=\"top\" style=\"background-color:#cccccc;\">"
                          : "<tr valign=\"top\" >") + HtmlUtil.cols(
                              userEditLink, user.getName(),
                              user.getRolesAsString("<br>"), user.getEmail(),
                              "" + user.getAdmin()) + "</tr>";
            sb.append(row);

        }
        sb.append("</table>");
        Result result = new Result("Users", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getUser(ResultSet results) throws Exception {
        int col = 1;
        User user = new User(results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getBoolean(col++));

        String query = SqlUtil.makeSelect(COL_USERROLES_ROLE,
                                          Misc.newList(TABLE_USERROLES),
                                          SqlUtil.eq(COL_USERROLES_USER_ID,
                                              SqlUtil.quote(user.getId())));
        Statement    stmt  = getDatabaseManager().execute(query);
        String[]     array = SqlUtil.readString(stmt, 1);
        List<String> roles = new ArrayList<String>(Misc.toList(array));
        user.setRoles(roles);
        return user;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private List<Entry> getCart(Request request) {
        String sessionId = request.getSessionId();

        if (sessionId == null) {
            return new ArrayList<Entry>();
        }
        List<Entry> cart = (List<Entry>) userCart.get(sessionId);
        if (cart == null) {
            cart = new ArrayList<Entry>();
            userCart.put(sessionId, cart);
        }
        return cart;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void addToCart(Request request, List<Entry> entries)
            throws Exception {
        List<Entry> cart = getCart(request);
        for (Entry entry : entries) {
            if ( !cart.contains(entry)) {
                cart.add(entry);
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processCart(Request request) throws Exception {
        String       action = request.getString(ARG_ACTION, "");
        StringBuffer sb     = new StringBuffer();
        if (action.equals(ACTION_CLEAR)) {
            getCart(request).clear();
        } else if (action.equals(ACTION_ADD)) {
            Entry entry = getRepository().getEntry(request.getId(""),
                              request);
            if (entry == null) {
                throw new IllegalArgumentException(
                    "Could not find entry with id:" + request.getId(""));
            }
            if ( !getCart(request).contains(entry)) {
                getCart(request).add(entry);
            }
        }

        return showCart(request);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result showCart(Request request) throws Exception {
        StringBuffer sb      = new StringBuffer();
        List<Entry>  entries = getCart(request);
        sb.append("<h3>User Cart</h3>");
        if (entries.size() == 0) {
            sb.append("No entries in cart");
        } else {
            sb.append(HtmlUtil.href(HtmlUtil.url(URL_USER_CART, ARG_ACTION,
                    ACTION_CLEAR), "Clear Cart"));
            boolean haveFrom = request.defined(ARG_FROM);
            if (haveFrom) {
                Entry fromEntry =
                    getRepository().getEntry(request.getString(ARG_FROM, ""),
                                             request);
                sb.append("<br>Pick an entry  to associate with: "
                          + fromEntry.getName());
            }


            if ( !haveFrom) {
                sb.append(
                    HtmlUtil.form(
                        repository.URL_GETENTRIES,
                        "name=\"getentries\" method=\"post\""));
                sb.append(HtmlUtil.submit("Get selected", "getselected"));
                sb.append(HtmlUtil.submit("Get all", "getall"));
                sb.append(" As: ");
                List outputList =
                    repository.getOutputTypesForEntries(request, entries);
                sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            }
            //            sb.append("<br>");
            sb.append("<ul>");
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            for (Entry entry : entries) {
                sb.append("<li> ");
                if (haveFrom) {
                    sb.append(
                        HtmlUtil.href(
                            HtmlUtil.url(
                                getRepository().URL_ASSOCIATION_ADD,
                                ARG_FROM, request.getString(ARG_FROM, ""),
                                ARG_TO, entry.getId()), HtmlUtil.img(
                                    getRepository().fileUrl(
                                        ICON_ASSOCIATION), "Create an association")));
                } else {
                    String links = HtmlUtil.checkbox("entry_"
                                       + entry.getId(), "true");
                    sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                    sb.append(links);
                    sb.append(
                        HtmlUtil.href(
                            HtmlUtil.url(
                                URL_USER_CART, ARG_FROM,
                                entry.getId()), HtmlUtil.img(
                                    getRepository().fileUrl(
                                        ICON_ASSOCIATION), "Create an association")));
                }
                sb.append(HtmlUtil.space(1));
                sb.append(outputHandler.getEntryUrl(entry));
            }
            sb.append("</ul>");
            if ( !haveFrom) {
                sb.append("</form>");
            }
        }
        return makeResult(request, "User Cart", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     */
    private Result makeResult(Request request, String title,
                              StringBuffer sb) {
        Result result = new Result(title, sb);
        if ( !request.getUser().getAnonymous()) {
            result.putProperty(PROP_NAVSUBLINKS,
                               getRepository().getSubNavLinks(request,
                                   userUrls));
        }
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getUserLinks(Request request) {
        User   user = request.getUser();
        String userLink;
        String cartEntry =
            HtmlUtil.href(URL_USER_CART,
                          HtmlUtil.img(getRepository().fileUrl(ICON_CART),
                                       "Data Cart"));
        if (user.getAnonymous()) {
            userLink =
                "<a href=\"${root}/user/login\" class=\"navlink\">Login</a>";
        } else {
            userLink = HtmlUtil.href(
                URL_USER_LOGOUT, "Logout",
                " class=\"navlink\" ") + HtmlUtil.space(1) + "|"
                    + HtmlUtil.space(1)
                    + HtmlUtil.href(
                        URL_USER_SETTINGS, user.getLabel(),
                            " class=\"navlink\" ") + HtmlUtil.space(1);
        }
        return cartEntry + HtmlUtil.space(2) + userLink;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processHome(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }
        return makeResult(request, "User Home", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processLogin(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        User         user = null;
        if (request.exists(ARG_USER_NAME)) {
            String name     = request.getString(ARG_USER_NAME, "");
            String password = request.getString(ARG_USER_PASSWORD1, "");
            password = hashPassword(password);
            String query = SqlUtil.makeSelect(
                               COLUMNS_USERS, Misc.newList(TABLE_USERS),
                               SqlUtil.makeAnd(
                                   Misc.newList(
                                       SqlUtil.eq(
                                           COL_USERS_ID,
                                           SqlUtil.quote(name)), SqlUtil.eq(
                                               COL_USERS_PASSWORD,
                                               SqlUtil.quote(password)))));

            ResultSet results =
                getDatabaseManager().execute(query).getResultSet();
            if (results.next()) {
                user = getUser(results);
                setUserSession(request, user);
                if (request.exists(ARG_REDIRECT)) {
                    return new Result(
                        HtmlUtil.url(
                            request.getUnsafeString(ARG_REDIRECT, ""),
                            ARG_MESSAGE, "You are logged in"));
                } else {
                    return new Result(HtmlUtil.url(URL_USER_HOME,
                            ARG_MESSAGE, "You are logged in"));
                }
            } else {
                sb.append(
                    getRepository().warning(
                        "Incorrect user name or password"));
            }
        }

        if (user == null) {
            sb.append(makeLoginForm(request));
        }
        return new Result("Login", sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processLogout(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        removeUserSession(request);
        sb.append("Logged out");
        sb.append(makeLoginForm(request));
        Result result = new Result("Logout", sb);
        return result;
    }


    /** _more_ */
    public static final String OUTPUT_CART = "user.cart";

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initOutputHandlers() throws Exception {
        OutputHandler outputHandler = new OutputHandler(getRepository()) {
            protected void getEntryLinks(Request request, Entry entry,
                                         List<Link> links)
                    throws Exception {
                links.add(
                    new Link(
                        HtmlUtil.url(
                            getRepository().getUserManager().URL_USER_CART,
                            ARG_ACTION, ACTION_ADD, ARG_ID,
                            entry.getId()), getRepository().fileUrl(
                                ICON_CART), "Add to cart"));
            }


            public boolean canHandle(String output) {
                return output.equals(OUTPUT_CART);
            }
            protected void getOutputTypesFor(Request request, String what,
                                             List types)
                    throws Exception {
                //                    if (what.equals(WHAT_ENTRIES)) {
                //                        types.add(new TwoFacedObject("Cart", OUTPUT_CART));
                //                    }
            }
            protected void getOutputTypesForEntry(Request request,
                    Entry entry, List types)
                    throws Exception {}
            protected void getOutputTypesForEntries(Request request,
                    List<Entry> entries, List types)
                    throws Exception {
                types.add(new TwoFacedObject("Cart", OUTPUT_CART));
            }
            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {
                addToCart(request, entries);
                return showCart(request);
            }
        };

        getRepository().addOutputHandler(outputHandler);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getRoles() throws Exception {
        String[] roleArray =
            SqlUtil.readString(
                getDatabaseManager().execute(
                    SqlUtil.makeSelect(
                        SqlUtil.distinct(COL_USERROLES_ROLE),
                        Misc.newList(TABLE_USERROLES))), 1);
        List<String> roles = new ArrayList<String>(Misc.toList(roleArray));
        roles.add(0, ROLE_ANY);
        return roles;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSettings(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();

        User         user = request.getUser();
        if (user.getAnonymous()) {
            sb.append(
                getRepository().warning(
                    "You need to be logged in to change user settings"));
            sb.append(makeLoginForm(request));
            return new Result("User Settings", sb);
        }

        if (request.exists(ARG_USER_CHANGE)) {
            boolean okToChangeUser = checkPasswords(request, user);
            if ( !okToChangeUser) {
                sb.append(getRepository().warning("Incorrect passwords"));
            } else {
                applyState(request, user, false);
                return new Result(HtmlUtil.url(URL_USER_SETTINGS,
                        ARG_MESSAGE, "User settings changed"));
            }
        }

        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }
        sb.append(HtmlUtil.form(URL_USER_SETTINGS));
        makeUserForm(request, user, sb, false);

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Change Settings",
                                         ARG_USER_CHANGE)));
        sb.append(HtmlUtil.formClose());
        String roles = user.getRolesAsString("<br>").trim();
        sb.append(HtmlUtil.formEntry(HtmlUtil.space(1), ""));
        if (roles.length() == 0) {
            roles = "--none--";
        }
        sb.append(HtmlUtil.formEntryTop("Roles:", roles));

        sb.append(HtmlUtil.formTableClose());
        return makeResult(request, "User Settings", sb);
    }




}

