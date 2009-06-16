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

import ucar.unidata.repository.output.*;
import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class UserManager extends RepositoryManager {

    /** _more_ */
    public static final OutputType OUTPUT_CART =
        new OutputType("Add to Cart", "user.cart", OutputType.TYPE_NONHTML,
                       "", ICON_CART_ADD);

    /** _more_ */
    public static final OutputType OUTPUT_FAVORITE =
        new OutputType("Add as Favorite", "user.addfavorite",
                       OutputType.TYPE_NONHTML, "", ICON_FAVORITE);

    /** _more_ */
    public static final String ROLE_ANY = "any";

    /** _more_ */
    public static final String ROLE_NONE = "none";




    /** _more_ */
    protected RequestUrl[] userUrls = { getRepositoryBase().URL_USER_HOME,
                                        getRepositoryBase().URL_USER_SETTINGS,
                                        getRepositoryBase().URL_USER_CART,
                                        getRepositoryBase()
                                            .URL_USER_MONITORS };


    /** _more_ */
    protected RequestUrl[] anonUserUrls = { getRepositoryBase()
                                              .URL_USER_CART };



    /** _more_ */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();

    /** _more_ */
    private Hashtable userCart = new Hashtable();

    /** _more_ */
    public final User localFileUser = new User("localuser", false);

    private List<UserAuthenticator> userAuthenticators = new ArrayList<UserAuthenticator>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public UserManager(Repository repository) {
        super(repository);
    }


    protected void addUserAuthenticator(UserAuthenticator userAuthenticator) {
        userAuthenticators.add(userAuthenticator);
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initUsers(List<User>cmdLineUsers) throws Exception {
        for (User user : cmdLineUsers) {
            makeOrUpdateUser(user, true);
        }

        //If we have an admin property then it is of the form userid:password
        //and is used to set the password of the admin
        String adminFromProperties = getProperty(PROP_ADMIN, null);
        if (adminFromProperties != null) {
            List<String> toks = StringUtil.split(adminFromProperties, ":");
            if (toks.size() != 2) {
                getLogManager().logError("Error: The " + PROP_ADMIN
                         + " property is incorrect");
                return;
            }
            User user = new User(toks.get(0), "", false);
            user.setPassword(UserManager.hashPassword(toks.get(1).trim()));
            makeOrUpdateUser(user, true, true);
            logInfo("Password for:" + user.getId() + " has been updated");
        }
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
            //            MessageDigest md = MessageDigest.getInstance("SHA");
            MessageDigest md = MessageDigest.getInstance("SHA-512");
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
     * @param password _more_
     *
     * @return _more_
     */
    public static String hashPasswordOld(String password) {
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
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<FavoriteEntry> getFavorites(Request request, User user)
            throws Exception {
        List<FavoriteEntry> favorites = user.getFavorites();
        if (favorites == null) {
            favorites = new ArrayList<FavoriteEntry>();
            Statement stmt = getDatabaseManager().select(
                                 Tables.FAVORITES.COLUMNS,
                                 Tables.FAVORITES.NAME,
                                 Clause.eq(
                                     Tables.FAVORITES.COL_USER_ID,
                                     user.getId()));
            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            //COL_ID,COL_USER_ID,COL_ENTRY_ID,COL_NAME,COL_CATEGORY
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    int    col    = 1;
                    String id     = results.getString(col++);
                    String userId = results.getString(col++);
                    Entry entry = getEntryManager().getEntry(request,
                                      results.getString(col++));
                    String name     = results.getString(col++);
                    String category = results.getString(col++);
                    if (entry == null) {
                         getDatabaseManager().delete(
                                 Tables.FAVORITES.NAME,
                                 Clause.and(
                                            Clause.eq(
                                                      Tables.FAVORITES.COL_USER_ID,
                                                      user.getId()),
                                            Clause.eq(
                                                      Tables.FAVORITES.COL_ID,
                                                      id)));
                        continue;
                    }
                    favorites.add(new FavoriteEntry(id, entry, name,
                            category));
                }
            }
            user.setFavorites(favorites);
        }
        return favorites;
    }



    /**
     * _more_
     *
     * @param user _more_
     *
     * @return _more_
     */
    public User getCurrentUser(User user) {
        if (user == null) {
            return null;
        }
        User currentUser = userMap.get(user.getId());
        if (currentUser != null) {
            return currentUser;
        }
        return user;
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
        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }

        sb.append(header(msg("Please login")));
        String id = request.getString(ARG_USER_ID, "");
        sb.append(
            HtmlUtil.formPost(getRepositoryBase().URL_USER_LOGIN.toString()));
        if (request.defined(ARG_REDIRECT)) {
            sb.append(HtmlUtil.hidden(ARG_REDIRECT,
                                      request.getUnsafeString(ARG_REDIRECT,
                                          "")));
        }
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("User"),
                                     HtmlUtil.input(ARG_USER_ID, id)));
        sb.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD)));
        sb.append(extra);

        sb.append(HtmlUtil.formEntry("", HtmlUtil.submit(msg("Login"))));
        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.formTableClose());

        if (getAdmin().isEmailCapable()) {
            sb.append(HtmlUtil.p());
            sb.append(
                HtmlUtil.href(
                    request.url(getRepositoryBase().URL_USER_FINDUSERID),
                    msg("Forget your user ID?")));
            sb.append(HtmlUtil.p());
            sb.append(
                HtmlUtil.href(
                    request.url(getRepositoryBase().URL_USER_RESETPASSWORD),
                    msg("Forget your password?")));
        }


        return sb.toString();
    }

    /** _more_ */
    private static final String USER_DEFAULT = "default";

    /** _more_ */
    private static final String USER_ANONYMOUS = "anonymous";

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public User getDefaultUser() throws Exception {
        makeUserIfNeeded(new User(USER_DEFAULT, "Default User", false));
        return findUser(USER_DEFAULT);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public User getAnonymousUser() throws Exception {
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
    public User findUser(String id) throws Exception {
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
    public User findUser(String id, boolean userDefaultIfNotFound)
            throws Exception {
        if (id == null) {
            return null;
        }
        User user = userMap.get(id);
        if (user != null) {
            //            System.err.println ("got from user map:" + id +" " + user);
            return user;
        }
        Statement stmt = getDatabaseManager().select(Tables.USERS.COLUMNS,
                             Tables.USERS.NAME,
                             Clause.eq(Tables.USERS.COL_ID, id));
        ResultSet results = stmt.getResultSet();
        if (results.next()) {
            user = getUser(results);
            getDatabaseManager().close(stmt);
        } else {
            for(UserAuthenticator userAuthenticator: userAuthenticators) {
                user = userAuthenticator.findUser(getRepository(),id);
                if(user!=null) {
                    user.setIsLocal(false);
                    break;
                }
            }
        }
        getDatabaseManager().close(stmt);

        if (user == null) {
            if (userDefaultIfNotFound) {
                return getDefaultUser();
            }
            return null;
        }


        userMap.put(user.getId(), user);
        return user;
    }


    /**
     * _more_
     *
     * @param email _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUserFromEmail(String email) throws Exception {
        Statement stmt = getDatabaseManager().select(Tables.USERS.COLUMNS,
                             Tables.USERS.NAME,
                             Clause.eq(Tables.USERS.COL_EMAIL, email));
        ResultSet results = stmt.getResultSet();
        if ( !results.next()) {
            return null;
        }
        return getUser(results);
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
        makeOrUpdateUser(user, true, false);
    }

    /**
     * _more_
     *
     * @param user _more_
     * @param updateIfNeeded _more_
     * @param onlyPassword _more_
     *
     * @throws Exception _more_
     */
    protected void makeOrUpdateUser(User user, boolean updateIfNeeded,
                                    boolean onlyPassword)
            throws Exception {
        if (getDatabaseManager().tableContains(user.getId(),
                Tables.USERS.NAME, Tables.USERS.COL_ID)) {
            if ( !updateIfNeeded) {
                throw new IllegalArgumentException(
                    msgLabel("Database already contains user")
                    + user.getId());
            }
            if (onlyPassword) {
                getDatabaseManager().update(
                    Tables.USERS.NAME, Tables.USERS.COL_ID, user.getId(),
                    new String[] { Tables.USERS.COL_PASSWORD },
                    new Object[] { user.getPassword() });
            } else {
                getDatabaseManager().update(Tables.USERS.NAME,
                                            Tables.USERS.COL_ID,
                                            user.getId(), new String[] {
                    Tables.USERS.COL_NAME, Tables.USERS.COL_PASSWORD,
                    Tables.USERS.COL_EMAIL, Tables.USERS.COL_QUESTION,
                    Tables.USERS.COL_ANSWER, Tables.USERS.COL_ADMIN,
                    Tables.USERS.COL_LANGUAGE, Tables.USERS.COL_TEMPLATE
                }, new Object[] {
                    user.getName(), user.getPassword(), user.getEmail(),
                    user.getQuestion(), user.getAnswer(), user.getAdmin()
                            ? new Integer(1)
                            : new Integer(0), user.getLanguage(),
                    user.getTemplate()
                });
                userMap.remove(user.getId());
            }

            return;
        }

        getDatabaseManager().executeInsert(Tables.USERS.INSERT, new Object[] {
            user.getId(), user.getName(), user.getEmail(), user.getQuestion(),
            user.getAnswer(), user.getPassword(),
            new Boolean(user.getAdmin()), user.getLanguage(),
            user.getTemplate()
        });

        userMap.put(user.getId(), user);
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
        userMap.remove(user.getId());
        deleteRoles(user);
        getDatabaseManager().delete(Tables.USERS.NAME,
                                    Clause.eq(Tables.USERS.COL_ID,
                                        user.getId()));
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteRoles(User user) throws Exception {
        getDatabaseManager().delete(Tables.USERROLES.NAME,
                                    Clause.eq(Tables.USERROLES.COL_USER_ID,
                                        user.getId()));
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
        user.setTemplate(request.getString(ARG_TEMPLATE, user.getTemplate()));
        user.setLanguage(request.getString(ARG_USER_LANGUAGE, user.getLanguage()));
        user.setQuestion(request.getString(ARG_USER_QUESTION,
                                           user.getQuestion()));
        user.setAnswer(request.getString(ARG_USER_ANSWER, user.getAnswer()));
        if (doAdmin) {
            if(!request.getUser().getAdmin()) throw new IllegalArgumentException("Need to be admin");
            if ( !request.defined(ARG_USER_ADMIN)) {
                user.setAdmin(false);
            } else {
                user.setAdmin(request.get(ARG_USER_ADMIN, user.getAdmin()));
            }
            List<String> roles =
                StringUtil.split(request.getString(ARG_USER_ROLES, ""), "\n",
                                 true, true);

            user.setRoles(roles);
            setRoles(request, user);
        }
        makeOrUpdateUser(user, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @throws Exception _more_
     */
    private void setRoles(Request request, User user) throws Exception {
        deleteRoles(user);
        if (user.getRoles() == null) {
            return;
        }
        for (String role : user.getRoles()) {
            getDatabaseManager().executeInsert(Tables.USERROLES.INSERT,
                    new Object[] { user.getId(),
                                   role });
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
    public Result adminUserEdit(Request request) throws Exception {
        String userId = request.getString(ARG_USER_ID, "");
        User   user   = findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException(
                msgLabel("Could not find user") + userId);
        }

        if (request.defined(ARG_USER_DELETE_CONFIRM)) {
            deleteUser(user);
            return new Result(request.url(getRepositoryBase().URL_USER_LIST));
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


        sb.append(RepositoryUtil.header(msgLabel("User") + HtmlUtil.space(1)
                                        + user.getId()));
        sb.append(HtmlUtil.p());
        sb.append(request.form(getRepositoryBase().URL_USER_EDIT));
        sb.append(HtmlUtil.hidden(ARG_USER_ID, user.getId()));
        if (request.defined(ARG_USER_DELETE)) {
            sb.append(
                getRepository().question(
                    msg("Are you sure you want to delete the user?"),
                    RepositoryUtil.buttons(
                        HtmlUtil.submit(msg("Yes"), ARG_USER_DELETE_CONFIRM),
                        HtmlUtil.submit(msg("Cancel"), ARG_USER_CANCEL))));
        } else {
            String buttons =
                HtmlUtil.submit(msg("Change User"), ARG_USER_CHANGE)
                + HtmlUtil.space(2)
                + HtmlUtil.submit(msg("Delete User"), ARG_USER_DELETE)
                + HtmlUtil.space(2)
                + HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
            sb.append(buttons);
            makeUserForm(request, user, sb, true);
            makePasswordForm(request, user, sb);
            sb.append(buttons);
        }
        sb.append(HtmlUtil.formClose());
        Result result = new Result(msgLabel("User") + user.getLabel(), sb);
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
        //        System.err.println ("User:" + user);
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_USER_NAME,
                                         user.getName(), HtmlUtil.SIZE_40)));
        if (includeAdmin) {
            if(!request.getUser().getAdmin()) throw new IllegalArgumentException("Need to be admin");
            sb.append(HtmlUtil.formEntry(msgLabel("Admin"),
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
            sb.append(HtmlUtil.formEntryTop(msgLabel("Roles"), roleEntry));
        }

        sb.append(HtmlUtil.formEntry(msgLabel("Email"),
                                     HtmlUtil.input(ARG_USER_EMAIL,
                                         user.getEmail(), HtmlUtil.SIZE_40)));

        List<TwoFacedObject> templates =
            getRepository().getTemplateSelectList();
        sb.append(HtmlUtil.formEntry(msgLabel("Page Template"),
                                     HtmlUtil.select(ARG_TEMPLATE, templates,
                                         user.getTemplate())));

        List languages = new ArrayList(getRepository().getLanguages());
        languages.add(0, new TwoFacedObject("None", ""));
        sb.append(HtmlUtil.formEntry(msgLabel("Language"),
                                     HtmlUtil.select(ARG_USER_LANGUAGE,
                                         languages, user.getLanguage())));
        sb.append(HtmlUtil.formTableClose());
    }



    private void makePasswordForm(Request request, User user, StringBuffer sb) 
            throws Exception {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        sb.append(HtmlUtil.formEntry(msgLabel("Password Again"),
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));

        sb.append(HtmlUtil.formTableClose());
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

        String       id        = "";
        String       name      = "";
        String       email     = "";
        String       password1 = "";
        String       password2 = "";
        List<String> roles;

        boolean      admin       = false;

        StringBuffer sb          = new StringBuffer();

        StringBuffer errorBuffer = new StringBuffer();

        if (request.exists(ARG_USER_BULK)) {
            List<User> users = new ArrayList<User>();
            boolean    ok    = true;
            for (String line : (List<String>) StringUtil.split(
                    request.getString(ARG_USER_BULK, ""), "\n", true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = (List<String>) StringUtil.split(line,
                                        ",", true, true);
                if (toks.size() == 0) {
                    continue;
                }
                if (toks.size() < 2) {
                    ok = false;
                    sb.append(getRepository().error("Bad line:" + line));
                    break;
                }
                id        = toks.get(0);
                password1 = toks.get(1);
                name      = ((toks.size() >= 3)
                             ? toks.get(2)
                             : id);
                email     = ((toks.size() >= 4)
                             ? toks.get(3)
                             : "");
                roles     = new ArrayList<String>();
                for (int i = 4; i < toks.size(); i++) {
                    roles.add(toks.get(i));
                }
                if (findUser(id) != null) {
                    ok = false;
                    sb.append(
                        getRepository().error(
                            msg("User already exists") + ": " + id));
                    break;
                }
                User user = new User(id, name, email, "", "",
                                     hashPassword(password1), false, "", "");
                user.setRoles(roles);
                users.add(user);
            }
            if (ok) {
                for (User user : users) {
                    makeOrUpdateUser(user, false);
                    setRoles(request, user);
                    sb.append(msgLabel("Created user"));
                    sb.append(
                        HtmlUtil.href(
                            request.url(
                                getRepositoryBase().URL_USER_EDIT,
                                ARG_USER_ID, user.getId()), user.getId()));
                    sb.append(HtmlUtil.br());
                }
                if (users.size() == 0) {
                    sb.append(getRepository().note(msg("No users created")));
                    makeBulkForm(request, sb,
                                 request.getString(ARG_USER_BULK, null));
                }
            } else {
                makeBulkForm(request, sb,
                             request.getString(ARG_USER_BULK, ""));
            }
            Result result = new Result(msg("New User"), sb);
            result.putProperty(PROP_NAVSUBLINKS,
                               getRepository().getSubNavLinks(request,
                                   getAdmin().adminUrls));

            return result;
        }


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
                errorBuffer.append(msg("Please enter an ID"));
                errorBuffer.append(HtmlUtil.br());
            }

            if(password1.length()==0 && password2.length()==0) {
                password1 = password2 = getRepository().getGUID() +"."+Math.random();
            }


            if ((password1.length() == 0) || !password1.equals(password2)) {
                okToAdd = false;
                errorBuffer.append(msg("Invalid password"));
                errorBuffer.append(HtmlUtil.br());
            }

            if (findUser(id) != null) {
                okToAdd = false;
                errorBuffer.append(msg("User with given id already exists"));
                errorBuffer.append(HtmlUtil.br());
            }

            if (okToAdd) {
                String homeGroupId = request.getString(ARG_USER_HOME+"_hidden","");
                User newUser = new User(id, name, email, "", "",
                                          hashPassword(password1), admin, "",
                                        "");
                List<String> newUserRoles =
                    StringUtil.split(request.getString(ARG_USER_ROLES, ""), "\n",
                                     true, true);

                newUser.setRoles(newUserRoles);
                makeOrUpdateUser(newUser,false);
                setRoles(request, newUser);
                
                StringBuffer msg = new StringBuffer(request.getString(ARG_USER_MESSAGE,""));
                msg.append("<p>User id: " + id+"<p>");
                msg.append("Click on this link to send a password reset link to your registered email address:<br>");
                String resetUrl = HtmlUtil.url(getRepositoryBase().URL_USER_RESETPASSWORD.toString(),
                                               ARG_USER_NAME,id);

                if(!resetUrl.startsWith("http"))
                    resetUrl = getRepository().absoluteUrl(resetUrl);
                msg.append(HtmlUtil.href(resetUrl,"Send Password Reset Message"));
                msg.append("<p>");

                if(homeGroupId.length()>0) {
                    Group parent = getEntryManager().findGroup(request, homeGroupId);
                    Group home = getEntryManager().makeNewGroup(parent, name, newUser,null,TypeHandler.TYPE_HOMEPAGE);
                    msg.append("A home group has been created for you: ");
                    String homeUrl = HtmlUtil.url(getRepositoryBase().URL_ENTRY_SHOW.toString(),
                                                  ARG_ENTRYID,home.getId());
                    msg.append(HtmlUtil.href(getRepository().absoluteUrl(homeUrl),home.getFullName()));
                    addFavorites(request, newUser, (List<Entry>)Misc.newList(home));
                }

                if(newUser.getEmail().length()>0 && request.get(ARG_USER_SENDMAIL,false) &&   getAdmin().isEmailCapable()) {
                    getAdmin().sendEmail(newUser.getEmail(), "RAMADDA User Account",
                                         msg.toString(), true);
                    
                }


                String userEditLink =
                    request.url(getRepositoryBase().URL_USER_EDIT,
                                ARG_USER_ID, id);
                return new Result(userEditLink);
            }
        }



        if (errorBuffer.toString().length() > 0) {
            sb.append(getRepository().warning(errorBuffer.toString()));
        }
        sb.append(msgHeader("Create User"));
        sb.append(request.form(getRepositoryBase().URL_USER_NEW));
        StringBuffer formSB = new StringBuffer();
        formSB.append(HtmlUtil.formTable());
        formSB.append(HtmlUtil.formEntry(msgLabel("ID"),
                                     HtmlUtil.input(ARG_USER_ID, id,
                                         HtmlUtil.SIZE_40)));
        formSB.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_USER_NAME, name,
                                         HtmlUtil.SIZE_40)));


        formSB.append(HtmlUtil.formEntry(msgLabel("Admin"),
                                     HtmlUtil.checkbox(ARG_USER_ADMIN,
                                         "true", admin)));

        formSB.append(HtmlUtil.formEntry(msgLabel("Email"),
                                     HtmlUtil.input(ARG_USER_EMAIL, email,
                                         HtmlUtil.SIZE_40)));

        formSB.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        formSB.append(HtmlUtil.formEntry(msgLabel("Password Again"),
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));


        String select =
            getRepository().getHtmlOutputHandler().getSelect(request,
                                                             ARG_USER_HOME,
                                                             HtmlUtil.space(1) + msg("Select"), false, "");
        formSB.append(HtmlUtil.hidden(ARG_USER_HOME + "_hidden", "",
                                          HtmlUtil.id(ARG_USER_HOME + "_hidden")));

        formSB.append(HtmlUtil.space(1));
        String groupMsg = "Create a group using the user's name under this group";
        formSB.append(HtmlUtil.formEntry(msgLabel("Home group"),
                                             HtmlUtil.disabledInput(ARG_USER_HOME, "",
                                                                    HtmlUtil.SIZE_40
                                                                    + HtmlUtil.id(ARG_USER_HOME)) +
                                             HtmlUtil.space(1) +select+"<br>"+groupMsg));

        formSB.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit(msg("Create User"),
                                         ARG_USER_NEW)));
        formSB.append(HtmlUtil.formTableClose());



        StringBuffer msgSB = new StringBuffer();

        String msg = "A new RAMADDA account has been created for you.";
        msgSB.append(HtmlUtil.checkbox(ARG_USER_SENDMAIL,"true",false));
        msgSB.append(HtmlUtil.space(1));
        msgSB.append(msg("Send an email to the new user with message:"));
        msgSB.append(HtmlUtil.br());
        msgSB.append(HtmlUtil.textArea(ARG_USER_MESSAGE, msg, 5, 50));
        msgSB.append(HtmlUtil.p());
        msgSB.append(msgLabel("User Roles"));
        msgSB.append(HtmlUtil.br());
        msgSB.append(HtmlUtil.textArea(ARG_USER_ROLES, request.getString(ARG_USER_ROLES,""), 5, 50));

        if(getAdmin().isEmailCapable()) {
            sb.append(HtmlUtil.table(HtmlUtil.rowTop(HtmlUtil.cols(formSB.toString(),
                                                                   msgSB.toString()))));
        } else {
            sb.append(formSB);
        }
        sb.append(HtmlUtil.formClose());



        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.hr());
        makeBulkForm(request, sb, null);
        Result result = new Result(msg("New User"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param init _more_
     */
    private void makeBulkForm(Request request, StringBuffer sb, String init) {
        if (init == null) {
            init = "#one per line\n#user id, password, name, email";
        }
        sb.append(msgHeader("Bulk User Create"));
        sb.append(request.form(getRepositoryBase().URL_USER_NEW));
        sb.append(HtmlUtil.textArea(ARG_USER_BULK, init, 10, 80));
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.submit("Submit"));
        sb.append("\n</form>\n");

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

        if(request.exists(ARG_REMOVESESSIONID)) {
            getSessionManager().removeSession(request.getString(ARG_REMOVESESSIONID));
            return new Result(request.url(getRepositoryBase().URL_USER_LIST,ARG_SHOWTAB,"2"));
        }


        Hashtable<String, StringBuffer> rolesMap = new Hashtable<String,
                                                       StringBuffer>();
        List<String> rolesList = new ArrayList<String>();
        StringBuffer usersHtml = new StringBuffer();
        StringBuffer rolesHtml = new StringBuffer();

        usersHtml.append(request.form(getRepositoryBase().URL_USER_NEW));
        usersHtml.append(HtmlUtil.submit(msg("New User")));
        usersHtml.append("</form>");

        Statement stmt = getDatabaseManager().select(Tables.USERS.COLUMNS,
                             Tables.USERS.NAME, new Clause(),
                             " order by " + Tables.USERS.COL_ID);

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;

        List<User>       users = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                users.add(getUser(results));
            }
        }


        usersHtml.append("<table>");
        usersHtml.append(HtmlUtil.row(HtmlUtil.cols("",
                HtmlUtil.bold(msg("ID")) + HtmlUtil.space(2),
                HtmlUtil.bold(msg("Name")) + HtmlUtil.space(2),
        //                    HtmlUtil.bold(msg("Roles")) + HtmlUtil.space(2),
        HtmlUtil.bold(msg("Email")) + HtmlUtil.space(2), HtmlUtil.bold(
            msg("Admin?")) + HtmlUtil.space(2))));

        for (User user : users) {
            String userEditLink = HtmlUtil.href(
                                      request.url(
                                          getRepositoryBase().URL_USER_EDIT,
                                          ARG_USER_ID,
                                          user.getId()), HtmlUtil.img(
                                              iconUrl(ICON_EDIT),
                                              msg("Edit user")));

            String userProfileLink =
                HtmlUtil.href(
                    HtmlUtil.url(
                        request.url(getRepository().URL_USER_PROFILE),
                        ARG_USER_ID, user.getId()), user.getId(),
                            "title=\"View user profile\"");

            String userLogLink =
                HtmlUtil.href(
                    request.url(
                        getRepositoryBase().URL_USER_ACTIVITY, ARG_USER_ID,
                        user.getId()), HtmlUtil.img(
                            getRepository().iconUrl(ICON_LOG),
                            msg("View user log")));


            String row = (user.getAdmin()
                          ? "<tr valign=\"top\" style=\"background-color:#cccccc;\">"
                          : "<tr valign=\"top\" >") + HtmlUtil.cols(
                              userLogLink + userEditLink, userProfileLink,
                              user.getName(),
            /*user.getRolesAsString("<br>"),*/
            user.getEmail(), "" + user.getAdmin()) + "</tr>";
            usersHtml.append(row);

            List<String> roles = user.getRoles();
            if (roles != null) {
                for (String role : roles) {
                    StringBuffer rolesSB = rolesMap.get(role);
                    if (rolesSB == null) {
                        rolesSB = new StringBuffer("");
                        rolesList.add(role);
                        rolesMap.put(role, rolesSB);
                    }
                    rolesSB.append("<li> ");
                    rolesSB.append(userEditLink + HtmlUtil.space(2)
                                   + user.getName());
                }
            }
        }
        usersHtml.append("</table>");

        for (String role : rolesList) {
            StringBuffer rolesSB = rolesMap.get(role);
            rolesHtml.append(HtmlUtil.makeShowHideBlock(role,
                    "<ul>" + rolesSB.toString() + "</ul>", false));
        }
        if (rolesList.size() == 0) {
            rolesHtml.append(msg("No roles"));
        }


        StringBuffer sb         = new StringBuffer();
        List         tabTitles  = new ArrayList();
        List         tabContent = new ArrayList();

        int showTab = request.get(ARG_SHOWTAB,0);
        tabTitles.add(msg("User List"));
        tabContent.add(usersHtml.toString());

        tabTitles.add(msg("Roles"));
        tabContent.add(rolesHtml.toString());


        tabTitles.add(msg("Current Sessions"));
        tabContent.add(
            getSessionManager().getSessionList(request).toString());


        tabTitles.add(msg("Recent User Activity"));
        tabContent.add(getUserActivities(request, null));


        tabTitles.set(showTab, "selected:"+tabTitles.get(showTab));
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.makeTabs(tabTitles, tabContent, true));

        Result result = new Result(msg("Users"), sb);
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
                             results.getBoolean(col++),
                             results.getString(col++),
                             results.getString(col++));

        Statement stmt = getDatabaseManager().select(
                             Tables.USERROLES.COL_ROLE,
                             Tables.USERROLES.NAME,
                             Clause.eq(
                                 Tables.USERROLES.COL_USER_ID, user.getId()));

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
    protected List<Entry> getCart(Request request) {
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
            Entry entry = getEntryManager().getEntry(request,
                              request.getId(""));
            if (entry == null) {
                throw new IllegalArgumentException(
                    msgLabel("Could not find entry with id")
                    + request.getId(""));
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

        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }
        List<Entry> entries = getCart(request);
        if (entries.size() == 0) {
            entries = new ArrayList<Entry>();
            sb.append(getRepository().note(
                                           msg("No entries in cart.")+
                                           HtmlUtil.space(1)+
                                           msg("Using top group.")));
            entries.add(getEntryManager().getTopGroup());
        }

       sb.append(HtmlUtil.p());
        if (entries.size() == 0) {
            return makeResult(request, "User Cart", sb);
        }

        boolean splitScreen = request.getString(ARG_ACTION,
                                  "").equals(ACTION_SPLIT);
        boolean      haveFrom = request.defined(ARG_FROM);
        StringBuffer header   = new StringBuffer();

        if ( !haveFrom) {
            if (splitScreen) {
                header.append(
                    HtmlUtil.href(
                        request.url(getRepositoryBase().URL_USER_CART),
                        msg("Unsplit Screen")));
            } else {
                header.append(
                    HtmlUtil.href(
                        request.url(
                            getRepositoryBase().URL_USER_CART, ARG_ACTION,
                            ACTION_SPLIT), msg("Split Screen")));
            }
            header.append(HtmlUtil.span("&nbsp;|&nbsp;",
                                        HtmlUtil.cssClass("separator")));
        }

        header.append(
            HtmlUtil.href(
                request.url(
                    getRepositoryBase().URL_USER_CART, ARG_ACTION,
                    ACTION_CLEAR), msg("Clear Cart")));

        sb.append(HtmlUtil.center(header.toString()));

        sb.append(HtmlUtil.p());
        if (haveFrom) {
            Entry fromEntry = getEntryManager().getEntry(request,
                                  request.getString(ARG_FROM, ""));
            sb.append(HtmlUtil.br());
            sb.append(msgLabel("Pick an entry  to associate with")
                      + HtmlUtil.space(1) + fromEntry.getName());
        }


        if ( !haveFrom && !splitScreen) {
            String[] formTuple =
                getRepository().getHtmlOutputHandler().getEntryFormStart(
                    request, entries, false);

            sb.append(formTuple[2]);
        }
        OutputHandler outputHandler =
            getRepository().getOutputHandler(request);

        int cnt = 1;
        if (splitScreen) {
            cnt = 2;
        }
        entries = getEntryManager().doGroupAndNameSort(entries,false);
        List<StringBuffer> columns = new ArrayList<StringBuffer>();
        StringBuffer       jsSB    = null;
        for (int column = 0; column < cnt; column++) {
            StringBuffer colSB = new StringBuffer();
            columns.add(colSB);
            for (Entry entry : entries) {
                if (haveFrom) {
                    colSB.append(HtmlUtil.img(getEntryManager().getIconUrl(request, entry)));
                    colSB.append(
                        HtmlUtil.href(
                            request.url(
                                getRepository().URL_ASSOCIATION_ADD,
                                ARG_FROM, request.getString(ARG_FROM, ""),
                                ARG_TO, entry.getId()), HtmlUtil.img(
                                    getRepository().iconUrl(
                                        ICON_ASSOCIATION), msg(
                                        "Create an association")) + HtmlUtil.space(
                                            1) + entry.getLabel()));
                } else if (splitScreen) {
                    request.put(ARG_SHOWLINK, "false");
                    colSB.append(getEntryManager().getAjaxLink(request,
                            entry, entry.getLabel(), null));

                    request.remove(ARG_SHOWLINK);
                } else {
                    String cbxId = "checkbox_" + HtmlUtil.blockCnt++;
                    String links = HtmlUtil.checkbox("entry_"
                                                     + entry.getId(), "true",
                                                     false,
                                                     HtmlUtil.attrs(HtmlUtil.ATTR_ID,
                                                                    cbxId,
                                                                    HtmlUtil.ATTR_ONCLICK,
                                                                    HtmlUtil.call("checkboxClicked",
                                                                                  HtmlUtil.comma("event",
                                                                                                 HtmlUtil.squote("entry_"),
                                                                                                 HtmlUtil.squote(cbxId)))));
                                                                                                 

                    colSB.append(HtmlUtil.hidden("all_" + entry.getId(),
                            "1"));
                    colSB.append(links);
                    colSB.append(HtmlUtil.img(getEntryManager().getIconUrl(request, entry)));
                    colSB.append(
                        HtmlUtil.href(
                            request.url(
                                getRepositoryBase().URL_USER_CART, ARG_FROM,
                                entry.getId()), HtmlUtil.img(
                                    getRepository().iconUrl(
                                        ICON_ASSOCIATION), msg(
                                        "Create an association"))));

                }
                if ( !splitScreen) {
                    colSB.append(HtmlUtil.space(1));
                    if (haveFrom) {}
                    else {
                        colSB.append(
                            getEntryManager().getBreadCrumbs(request, entry));
                    }
                    colSB.append(HtmlUtil.br());
                }
            }
        }
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE,
                                HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, "100%")));

        sb.append(HtmlUtil.open(HtmlUtil.TAG_TR,
                                HtmlUtil.attr(HtmlUtil.ATTR_VALIGN, "top")));
        int colCnt = 0;
        for (StringBuffer colSB : columns) {
            colCnt++;
            String extra = "";
            if (colCnt == 1) {
                extra = HtmlUtil.style("border-right : 1px black solid;");
            }
            sb.append(HtmlUtil.open(HtmlUtil.TAG_TD,
                                    HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, "50%")
                                    + extra));
            if (splitScreen) {
                sb.append(
                    HtmlUtil.open(
                        HtmlUtil.TAG_DIV,
                        HtmlUtil.style(
                            "max-height: 600px; overflow-y: auto;")));
            } 
            sb.append(colSB);
            if (splitScreen) {
                sb.append(HtmlUtil.close(HtmlUtil.TAG_DIV));
            }
            sb.append(HtmlUtil.close(HtmlUtil.TAG_TD));
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TR));
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

        if ( !haveFrom && !splitScreen) {
            sb.append("</form>");
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
    public Result makeResult(Request request, String title, StringBuffer sb) {
        return getRepository().makeResult(request, title, sb,
                                          (request.getUser().getAnonymous()
                                           ? anonUserUrls
                                           : userUrls));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getUserLinks(Request request) {
        User user = request.getUser();
        String template = getRepository().getTemplateProperty(request,
                              "ramadda.template.link.wrapper", "");
        template = getRepository().getTemplateProperty(request,
                "ramadda.template.userlink.wrapper", template);
        String separator = getRepository().getTemplateProperty(request,
                               "ramadda.template.link.separator", "");
        separator = getRepository().getTemplateProperty(request,
                "ramadda.template.userlink.separator", separator);

        List extras = new ArrayList();
        List urls   = new ArrayList();
        List labels = new ArrayList();
        List tips   = new ArrayList();

        extras.add("");
        urls.add(request.url(getRepositoryBase().URL_USER_CART));
        //        labels.add(HtmlUtil.img(getRepository().iconUrl(ICON_CART),
        //                                msg("Data Cart")));
        labels.add(msg("Data Cart"));
        tips.add(msg("View data cart"));

        if (user.getAnonymous()) {
            request.remove(ARG_MESSAGE);
            request.remove(ARG_REDIRECT);
            String redirect =
                XmlUtil.encodeBase64(request.getUrl().getBytes());
            extras.add("");
            //            System.err.println ("initial url " + request.getUrl());
            urls.add(request.url(getRepositoryBase().URL_USER_LOGIN,
                                 ARG_REDIRECT, redirect));
            labels.add(msg("Login"));
            tips.add(msg("Login"));
        } else {
            extras.add("");
            urls.add(request.url(getRepositoryBase().URL_USER_LOGOUT));
            labels.add(msg("Logout"));
            tips.add(msg("Logout"));
            extras.add("");
            urls.add(request.url(getRepositoryBase().URL_USER_HOME));
            String label = user.getLabel().replace(" ", "&nbsp;");
            labels.add(label);
            tips.add(msg("Go to user settings"));
        }

        urls.add(request.url(getRepositoryBase().URL_HELP));
        extras.add("");
        labels.add(msg("Help"));
        tips.add(msg("View Help"));

        List links = new ArrayList();
        for (int i = 0; i < urls.size(); i++) {
            String link = template.replace("${label}",
                                           labels.get(i).toString());
            link = link.replace("${url}", urls.get(i).toString());
            link = link.replace("${tooltip}", tips.get(i).toString());
            link = link.replace("${extra}", extras.get(i).toString());
            links.add(link);
        }
        return StringUtil.join(separator, links);
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
    public Result processFavorite(Request request) throws Exception {
        String message = "";
        User   user    = request.getUser();
        if (user.getAnonymous()) {
            return new Result(
                msg("Favorites"),
                new StringBuffer(
                    getRepository().error(
                        "Anonymous users cannCot have favorites")));
        }
        String entryId = request.getString(ARG_ENTRYID, BLANK);

        if (request.get(ARG_FAVORITE_ADD, false)) {
            Entry entry = getEntryManager().getEntry(request, entryId);
            if (entry == null) {
                return new Result(
                    msg("Favorites"),
                    new StringBuffer(
                        getRepository().error(
                            "Cannot find or access entry")));
            }

            addFavorites(request, user, (List<Entry>) Misc.newList(entry));
            message = msg("Favorite added");
        } else if (request.get(ARG_FAVORITE_DELETE, false)) {
            getDatabaseManager().delete(
                Tables.FAVORITES.NAME,
                Clause.and(
                    Clause.eq(
                        Tables.FAVORITES.COL_ID,
                        request.getString(ARG_FAVORITE_ID, "")), Clause.eq(
                            Tables.FAVORITES.COL_USER_ID, user.getId())));
            message = msg("Favorite deleted");
            user.setFavorites(null);
        } else {
            message = msg("Unknown favorite command");
        }

        String redirect = getRepositoryBase().URL_USER_HOME.toString();
        return new Result(HtmlUtil.url(redirect, ARG_MESSAGE, message));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void addFavorites(Request request, User user, List<Entry> entries)
            throws Exception {
        List<Entry> favorites =
            FavoriteEntry.getEntries(getFavorites(request, user));
        if (user.getAnonymous()) {
            throw new IllegalArgumentException(
                "Need to be logged in to add favorites");
        }
        for (Entry entry : entries) {
            if (favorites.contains(entry)) {
                continue;
            }
            //COL_ID,COL_USER_ID,COL_ENTRY_ID,COL_NAME
            String name     = "";
            String category = "";
            getDatabaseManager().executeInsert(Tables.FAVORITES.INSERT,
                    new Object[] { getRepository().getGUID(),
                                   user.getId(), entry.getId(), name,
                                   category });
        }
        user.setFavorites(null);
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
        boolean responseAsXml = request.getString(ARG_RESPONSE,
                                    "").equals(RESPONSE_XML);
        StringBuffer sb   = new StringBuffer();
        User         user = request.getUser();
        if (user.getAnonymous()) {
            if (responseAsXml) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
                        XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                        "No user defined"), MIME_XML);
            }
            String msg = msg("No user defined");
            if (request.exists(ARG_FROMLOGIN)) {
                msg = msg + HtmlUtil.p()
                      + msg("If you had logged in perhaps you have cookies turned off?");
            }
            sb.append(getRepository().warning(msg));
            sb.append(makeLoginForm(request));
        } else if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }

        if (responseAsXml) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, "ok"),
                                          user.getId()), MIME_XML);
        }


        sb.append(HtmlUtil.p());
        int cnt = 0;
        for (FavoriteEntry favorite : getFavorites(request, user)) {
            if (cnt == 0) {
                sb.append(msgHeader("Favorites"));
            }
            cnt++;
            //TODO: Use the categories
            String removeLink =
                HtmlUtil.href(
                    request.url(
                        getRepositoryBase().URL_USER_FAVORITE,
                        ARG_FAVORITE_ID, favorite.getId(),
                        ARG_FAVORITE_DELETE, "true"), HtmlUtil.img(
                            getRepository().iconUrl(ICON_DELETE),
                            msg("Delete this favorite")));
            sb.append(removeLink);
            sb.append(HtmlUtil.space(1));
            sb.append(getEntryManager().getBreadCrumbs(request,
                    favorite.getEntry()));
            sb.append(HtmlUtil.br());
        }


        if (cnt == 0) {
            sb.append(
                "You have no favorite entries defined.<br>When you see an  entry or group just click on the "
                + HtmlUtil.img(iconUrl(ICON_FAVORITE))
                + " icon to add it to your list of favorites");
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
    public Result processProfile(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        User         user = findUser(request.getString(ARG_USER_ID, ""));
        if (user == null) {
            sb.append(msgLabel("Unknown user"));
            sb.append(request.getString(ARG_USER_ID, ""));
            return new Result(msg("User Profile"), sb);
        }

        sb.append(msgHeader("User Profile"));
        String searchLink =
            HtmlUtil.href(
                HtmlUtil.url(
                    request.url(getRepository().URL_ENTRY_SEARCH),
                    ARG_USER_ID, user.getId()), HtmlUtil.img(
                        getRepository().iconUrl(ICON_SEARCH),
                        msg("Search for entries created by this user")));

        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("ID"),
                                     user.getId() + HtmlUtil.space(2)
                                     + searchLink));
        sb.append(HtmlUtil.formEntry(msgLabel("Name"), user.getLabel()));
        String email = user.getEmail();
        if (email.length() > 0) {
            email = email.replace("@", " _AT_ ");
            sb.append(HtmlUtil.formEntry(msgLabel("Email"), email));
        }
        sb.append(HtmlUtil.formTableClose());
        return new Result(msg("User Profile"), sb);
    }




    /**
     * Class PasswordReset _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class PasswordReset {

        /** _more_ */
        String user;

        /** _more_ */
        Date dttm;

        /**
         * _more_
         *
         * @param user _more_
         * @param dttm _more_
         */
        public PasswordReset(String user, Date dttm) {
            this.user = user;
            this.dttm = dttm;
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
    public Result processFindUserId(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if ( !getAdmin().isEmailCapable()) {
            return makeResult(
                request, "User Information",
                new StringBuffer(
                    getRepository().warning(
                        msg(
                        "This RAMADDA server has not been configured to send email"))));
        }

        String email = request.getString(ARG_USER_EMAIL, "").trim();
        if (email.length() > 0) {
            User user = findUserFromEmail(email);
            if (user != null) {
                String userIdMailTemplate =
                    getProperty(PROP_USER_RESET_ID_TEMPLATE, "${userid}");
                String contents = userIdMailTemplate.replace("${userid}",
                                      user.getId());
                contents = contents.replace(
                    "${url}", getRepository().URL_USER_LOGIN.getFullUrl(""));
                String subject = getProperty(PROP_USER_RESET_ID_SUBJECT,
                                             "Your RAMADDA ID");
                getAdmin().sendEmail(user.getEmail(), subject,
                                     contents.toString(), true);
                String message =
                    "You user id has been sent to your registered email address";
                return new Result(
                    request.url(
                        getRepositoryBase().URL_USER_LOGIN, ARG_MESSAGE,
                        message));
            }
            sb.append(
                getRepository().error(
                    msg(
                    "No user is registered with the given email address")));
        }

        sb.append(msgHeader("Please enter your registered email address"));
        sb.append(HtmlUtil.p());

        sb.append(request.form(getRepositoryBase().URL_USER_FINDUSERID));
        sb.append(msgLabel("Email"));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.input(ARG_USER_EMAIL, email, HtmlUtil.SIZE_30));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.submit("Submit"));
        sb.append(HtmlUtil.formClose());
        return new Result(msg("Password Reset"), sb);
    }



    /** _more_ */
    private Hashtable passwordResets = new Hashtable();

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processResetPassword(Request request) throws Exception {

        String key = request.getString(ARG_USER_PASSWORDKEY, (String) null);
        PasswordReset resetInfo = null;
        StringBuffer  sb        = new StringBuffer();
        if (key != null) {
            resetInfo = (PasswordReset) passwordResets.get(key);
            if (resetInfo != null) {
                if (new Date().getTime() > resetInfo.dttm.getTime()) {
                    sb.append(
                        getRepository().error(
                            msg("Password reset has timed out") + "<br>"
                            + msg("Please try again")));
                    resetInfo = null;
                    passwordResets.remove(key);
                }
            } else {
                sb.append(
                    getRepository().error(
                        msg("Password reset has timed out") + "<br>"
                        + msg("Please try again")));
            }
        }

        User user = ((resetInfo != null)
                     ? findUser(resetInfo.user, false)
                     : null);
        if (user != null) {
            if (request.exists(ARG_USER_PASSWORD1)) {
                if (checkPasswords(request, user)) {
                    applyState(request, user, false);
                    sb.append(
                        getRepository().note(
                            msg("Your password has been reset")));
                    sb.append(makeLoginForm(request));
                    return new Result(msg("Password Reset"), sb);
                }
                sb.append(getRepository().warning("Incorrect passwords"));
            }
            sb.append(msgHeader("Please reset your password"));
            sb.append(HtmlUtil.p());

            sb.append(
                request.form(getRepositoryBase().URL_USER_RESETPASSWORD));
            sb.append(HtmlUtil.hidden(ARG_USER_PASSWORDKEY, key));
            sb.append(HtmlUtil.formTable());
            sb.append(HtmlUtil.formEntry(msgLabel("User"), user.getId()));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Password"),
                    HtmlUtil.password(ARG_USER_PASSWORD1)));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Password Again"),
                    HtmlUtil.password(ARG_USER_PASSWORD2)));
            sb.append(HtmlUtil.formEntry("", HtmlUtil.submit("Submit")));

            sb.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.formClose());
            return new Result(msg("Password Reset"), sb);
        }

        if ( !getAdmin().isEmailCapable()) {
            return new Result(
                msg("Password Reset"),
                new StringBuffer(
                    getRepository().warning(
                        msg(
                        "This RAMADDA server has not been configured to send email"))));
        }


        if (user == null) {
            user = findUser(request.getString(ARG_USER_NAME, ""), false);
        }
        if (user == null) {
            if (request.exists(ARG_USER_NAME)) {
                sb.append(
                    getRepository().error(msg("Not a registered user")));
                sb.append(HtmlUtil.p());
            }
            addPasswordResetForm(request, sb,
                                 request.getString(ARG_USER_NAME, ""));
            return new Result(msg("Password Reset"), sb);
        }

        key = getRepository().getGUID() + "_" + Math.random();
        //Time out is 1 hour
        resetInfo = new PasswordReset(user.getId(),
                                      new Date(new Date().getTime()
                                          + 1000 * 60 * 60));
        passwordResets.put(key, resetInfo);
        String toUser = user.getEmail();
        String url = getRepository().URL_USER_RESETPASSWORD.getFullUrl("?"
                         + ARG_USER_PASSWORDKEY + "=" + key);
        String template = getProperty(PROP_USER_RESET_PASSWORD_TEMPLATE, "");
        template = template.replace("${url}", url);
        template = template.replace("${userid}", user.getId());
        String subject = getProperty(PROP_USER_RESET_PASSWORD_SUBJECT,
                                     "Your RAMADDA Password");
        getAdmin().sendEmail(toUser, subject, template, true);
        StringBuffer message = new StringBuffer();
        message.append(getRepository().note("Instructions on how to reset your password have been sent to your registered email address.")); 
        return new Result("Password Reset",message);


        //        return new Result(request.url(getRepositoryBase().URL_USER_LOGIN,
        //                                      ARG_MESSAGE, message));
        /*
        sb.append(
            getRepository().note(
                msg(
                "Instructions on how to reset your password have been sent to your registered email address")));
        return new Result(msg("Password Reset"), sb);
        */
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param name _more_
     */
    private void addPasswordResetForm(Request request, StringBuffer sb,
                                      String name) {
        sb.append(msgHeader("Please enter your user ID"));
        sb.append(HtmlUtil.p());
        sb.append(request.form(getRepositoryBase().URL_USER_RESETPASSWORD));
        sb.append(msgLabel("User ID"));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.input(ARG_USER_NAME, name, HtmlUtil.SIZE_20));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.submit(msg("Reset your password")));
        sb.append(HtmlUtil.formClose());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param what _more_
     * @param extra _more_
     *
     * @throws Exception _more_
     */
    private void addActivity(Request request, User user, String what,
                             String extra)
            throws Exception {
        getDatabaseManager().executeInsert(Tables.USER_ACTIVITY.INSERT,
                                           new Object[] { user.getId(),
                new Date(), what, extra, request.getIp() });
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

        boolean responseAsXml = request.getString(ARG_RESPONSE,
                                    "").equals(RESPONSE_XML);
        StringBuffer sb     = new StringBuffer();
        User         user   = null;
        String       output = request.getString(ARG_OUTPUT, "");
        if (request.exists(ARG_USER_ID)) {
            String name = request.getString(ARG_USER_ID, "").trim();
            if (name.equals(USER_DEFAULT) || name.equals(USER_ANONYMOUS)) {
                name = "";
            }
            String password = request.getString(ARG_USER_PASSWORD, "").trim();
            if ((name.length() > 0) && (password.length() > 0)) {
                String hashedPassword = hashPassword(password);
                Statement stmt =
                    getDatabaseManager().select(Tables.USERS.COLUMNS,
                        Tables.USERS.NAME,
                        Clause.and(Clause.eq(Tables.USERS.COL_ID, name),
                                   Clause.eq(Tables.USERS.COL_PASSWORD,
                                             hashedPassword)));

                ResultSet results = stmt.getResultSet();
                if ( !results.next()) {
                    //We changed the password hash function to a new one so
                    //check if the DB has the old hashed password
                    //If it does then insert the new one
                    String oldHashedPassword = hashPasswordOld(password);
                    Statement stmt2 =
                        getDatabaseManager().select(Tables.USERS.COLUMNS,
                            Tables.USERS.NAME,
                            Clause.and(Clause.eq(Tables.USERS.COL_ID, name),
                                       Clause.eq(Tables.USERS.COL_PASSWORD,
                                           oldHashedPassword)));


                    ResultSet results2 = stmt2.getResultSet();
                    if (results2.next()) {
                        user = getUser(results2);
                        getDatabaseManager().update(Tables.USERS.NAME,
                                Tables.USERS.COL_ID, user.getId(),
                                new String[] { Tables.USERS.COL_PASSWORD },
                                new Object[] { hashedPassword });
                    }
                    stmt2.close();
                } else {
                    user = getUser(results);
                }
                stmt.close();

                if(user == null) {
                    for(UserAuthenticator userAuthenticator: userAuthenticators) {
                        user = userAuthenticator.authenticateUser(getRepository(),name,password);
                        if(user!=null) {
                            user.setIsLocal(false);
                            break;
                        }
                    }
                }
            }


            if (user != null) {
                addActivity(request, user, "login", "");
                getSessionManager().setUserSession(request, user);
                if (responseAsXml) {
                    return new Result(XmlUtil.tag(TAG_RESPONSE,
                            XmlUtil.attr(ATTR_CODE, CODE_OK),
                            request.getSessionId()), MIME_XML);
                }
                if (request.exists(ARG_REDIRECT)) {
                    String redirect = new String(
                                          XmlUtil.decodeBase64(
                                              request.getUnsafeString(
                                                  ARG_REDIRECT, "")));
                    if ( !redirect.startsWith("http")) {
                        redirect = getRepository().absoluteUrl(redirect);
                    }
                    //                    System.err.println("xxx  redirecting to:" + redirect);
                    return new Result(HtmlUtil.url(redirect, ARG_FROMLOGIN,
                            "true", ARG_MESSAGE, msg("You are logged in")));
                } else {
                    String redirect;
                    //If we are under ssl then redirect to non-ssl
                    if (getRepository().isSSLEnabled(request)) {
                        redirect =
                            getRepositoryBase().URL_USER_HOME.getFullUrl("");
                    } else {
                        redirect =
                            getRepositoryBase().URL_USER_HOME.toString();
                    }
                    //                    System.err.println("yyy redirecting to:" + redirect);
                    return new Result(HtmlUtil.url(redirect, ARG_FROMLOGIN,
                            "true", ARG_MESSAGE, msg("You are logged in")));
                }
            } else {
                if (responseAsXml) {
                    return new Result(XmlUtil.tag(TAG_RESPONSE,
                            XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                            "Incorrect user name or password"), MIME_XML);
                }

                if (name.length() > 0) {
                    //Check if they have a blank password
                    Statement stmt = getDatabaseManager().select(
                                         Tables.USERS.COL_PASSWORD,
                                         Tables.USERS.NAME,
                                         Clause.eq(
                                             Tables.USERS.COL_ID, name));
                    ResultSet results = stmt.getResultSet();
                    if (results.next()) {
                        password = results.getString(1);
                        if ((password == null) || (password.length() == 0)) {
                            sb.append(
                                getRepository().note(
                                    msg(
                                    "Sorry, we were doing some cleanup and have reset your password")));

                            addPasswordResetForm(request, sb, name);
                            stmt.close();
                            return new Result(msg("Login"), sb);
                        }
                    }
                    stmt.close();
                }

                //TODO: what to do when we have ssl here?
                sb.append(
                    getRepository().warning(
                        msg("Incorrect user name or password")));


            }
        }


        if (user == null) {
            sb.append(makeLoginForm(request));
        }
        return new Result(msg("Login"), sb);

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
        getSessionManager().removeUserSession(request);
        request.setSessionId(getSessionManager().getSessionId());
        sb.append(getRepository().note(msg("You are logged out")));
        sb.append(makeLoginForm(request));
        Result result = new Result(msg("Logout"), sb);
        return result;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initOutputHandlers() throws Exception {
        OutputHandler outputHandler = new OutputHandler(getRepository(),
                                          "Cart") {
            public void getEntryLinks(Request request, State state,
                                         List<Link> links)
                    throws Exception {
                if (state.getEntry() != null) {
                    Link link = makeLink(request, state.getEntry(),
                                         OUTPUT_CART);
                    link.setLinkType(OutputType.TYPE_ACTION);
                    links.add(link);
                    if ( !request.getUser().getAnonymous()) {
                        link = makeLink(request, state.getEntry(),
                                        OUTPUT_FAVORITE);
                        link.setLinkType(OutputType.TYPE_ACTION);
                        links.add(link);
                    }
                }
            }

            public boolean canHandleOutput(OutputType output) {
                return output.equals(OUTPUT_CART)
                       || output.equals(OUTPUT_FAVORITE);
            }

            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {
                OutputType output = request.getOutput();
                if (output.equals(OUTPUT_CART)) {
                    if (group.isDummy()) {
                        addToCart(request, entries);
                        addToCart(request,
                                  (List<Entry>) new ArrayList(subGroups));
                    } else {
                        addToCart(request, (List<Entry>) Misc.newList(group));
                    }
                    return showCart(request);
                } else {
                    User user = request.getUser();
                    if (group.isDummy()) {
                        addFavorites(request, user, entries);
                        addFavorites(request, user,
                                     (List<Entry>) new ArrayList(subGroups));
                    } else {
                        addFavorites(request, user,
                                     (List<Entry>) Misc.newList(group));
                    }
                    String redirect =
                        getRepositoryBase().URL_USER_HOME.toString();
                    //                    System.err.println("zzz redirecting to:" + redirect);
                    return new Result(HtmlUtil.url(redirect, ARG_MESSAGE,
                            msg("Favorites Added")));

                }

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
                getDatabaseManager().select(
                    SqlUtil.distinct(Tables.USERROLES.COL_ROLE),
                    Tables.USERROLES.NAME, new Clause()), 1);
        List<String> roles = new ArrayList<String>(Misc.toList(roleArray));

        for(UserAuthenticator userAuthenticator: userAuthenticators) {
            roles.addAll(userAuthenticator.getAllRoles());
        }

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
    public Result processActivityLog(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();

        User         user = findUser(request.getString(ARG_USER_ID, ""));

        if (user == null) {
            sb.append(getRepository().error("Could not find user"));
        } else {
            sb.append(getUserActivities(request, user));
        }
        Result result = new Result(msg("User Log"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param theUser _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getUserActivities(Request request, User theUser)
            throws Exception {
        StringBuffer sb          = new StringBuffer();
        Clause       clause      = null;
        String       limitString = "";
        if (theUser != null) {
            clause = Clause.eq(Tables.USER_ACTIVITY.COL_USER_ID,
                               theUser.getId());
        } else {
            limitString = getDatabaseManager().getLimitString(0,
                    request.get(ARG_LIMIT, 100));
        }


        Statement stmt =
            getDatabaseManager().select(Tables.USER_ACTIVITY.COLUMNS,
                                        Tables.USER_ACTIVITY.NAME, clause,
                                        " order by "
                                        + Tables.USER_ACTIVITY.COL_DATE
                                        + " desc " + limitString);

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        if (theUser != null) {
            sb.append(msgLabel("Activity for User"));
            sb.append(HtmlUtil.space(1));
            sb.append(theUser.getLabel());
        }
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.row(HtmlUtil.cols(((theUser == null)
                ? HtmlUtil.b(msg("User"))
                : ""), HtmlUtil.b(msg("Activity")), HtmlUtil.b(msg("Date")),
                       HtmlUtil.b(msg("IP Address")))));

        int cnt = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int    col      = 1;
                String userId   = results.getString(col++);
                String firstCol = "";
                if (theUser == null) {
                    User user = findUser(userId);
                    if (user == null) {
                        firstCol = "No user:" + userId;
                    } else {
                        firstCol =
                            HtmlUtil.href(
                                request.url(
                                    getRepositoryBase().URL_USER_ACTIVITY,
                                    ARG_USER_ID, user.getId()), HtmlUtil.img(
                                        getRepository().iconUrl(ICON_LOG),
                                        msg("View user log")) + " "
                                            + user.getLabel());
                    }

                }
                Date   dttm  = getDatabaseManager().getDate(results, col++);
                String what  = results.getString(col++);
                String extra = results.getString(col++);
                String ip    = results.getString(col++);
                sb.append(HtmlUtil.row(HtmlUtil.cols(firstCol, what,
                        getRepository().formatDate(dttm), ip)));

                cnt++;
            }
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));
        if (cnt == 0) {
            sb.append(msg("No activity"));
        }
        return sb.toString();
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
                    msg("You need to be logged in to change user settings")));
            sb.append(makeLoginForm(request));
            return new Result(msg("User Settings"), sb);
        }

        if (request.exists(ARG_USER_CHANGE)) {
            boolean settingsOk = true;
            String message;
            if (request.exists(ARG_USER_PASSWORD1)) {
                settingsOk = checkPasswords(request, user);
                if ( !settingsOk) {
                    sb.append(
                              getRepository().warning(msg("Incorrect passwords")));
                } 
                message = "Your password has been changed";
            }  else {
                message = "Your settings have been changed";
            }
            if(settingsOk) {
                applyState(request, user, false);
                String redirect;
                //If we are under ssl then redirect to non-ssl
                if (getRepository().isSSLEnabled(request)) {
                    redirect =
                        getRepositoryBase().URL_USER_SETTINGS.getFullUrl("");
                } else {
                    redirect =
                        getRepositoryBase().URL_USER_SETTINGS.toString();
                }
                return new Result(HtmlUtil.url(redirect, ARG_MESSAGE,
                                               msg(message)));
            }
        }

        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }

        sb.append(HtmlUtil.p());

        sb.append(request.form(getRepositoryBase().URL_USER_SETTINGS));
        makeUserForm(request, user, sb, false);
        sb.append(HtmlUtil.submit(msg("Change Settings"), ARG_USER_CHANGE));
        sb.append(HtmlUtil.formClose());

        sb.append(HtmlUtil.p());
        sb.append(request.form(getRepositoryBase().URL_USER_SETTINGS));
        makePasswordForm(request, user, sb);
        sb.append(HtmlUtil.submit(msg("Change Password"), ARG_USER_CHANGE));
        sb.append(HtmlUtil.formClose());

        sb.append(HtmlUtil.formTable());
        String roles = user.getRolesAsString("<br>").trim();
        sb.append(HtmlUtil.formEntry(HtmlUtil.space(1), ""));
        if (roles.length() == 0) {
            roles = "--none--";
        }
        sb.append(HtmlUtil.formEntryTop(msgLabel("Roles"), roles));
        sb.append(HtmlUtil.formTableClose());
        return makeResult(request, msg("User Settings"), sb);
    }




}

