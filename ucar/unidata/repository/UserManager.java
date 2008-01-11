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

    /** _more_ */
    public RequestUrl URL_USER_LOGIN = new RequestUrl(this, "/user/login");

    /** _more_ */
    public RequestUrl URL_USER_SETTINGS = new RequestUrl(this, "/user/settings");

    public RequestUrl URL_USER_CART = new RequestUrl(this, "/user/cart");


    /** _more_          */
    public static final String ARG_USER_NAME = "user.name";

    /** _more_          */
    public static final String ARG_USER_PASSWORD1 = "user.password1";

    /** _more_          */
    public static final String ARG_USER_PASSWORD2 = "user.password2";

    /** _more_          */
    public static final String ARG_USER_EMAIL = "user.email";

    /** _more_          */
    public static final String ARG_USER_QUESTION = "user.question";

    /** _more_          */
    public static final String ARG_USER_ANSWER = "user.answer";

    /** _more_          */
    public static final String ARG_USER_ADMIN = "user.admin";



    /** _more_          */
    boolean requireLogin = true;

    /** _more_ */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();

    private Hashtable userCart = new Hashtable();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public UserManager(Repository repository) {
        super(repository);
        requireLogin    = getRepository().getProperty(PROP_USER_REQUIRELOGIN,
                true);
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
            return XmlUtil.encodeBase64(md.digest());
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
     * @return _more_
     */
    protected boolean isRequestOk(Request request) {
        if (requireLogin
                && request.getRequestContext().getUser().getAnonymous()) {
            if ( !request.getRequestPath().startsWith(getRepository().getUrlBase()
                    + "/user/")) {
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
        StringBuffer sb   = new StringBuffer("<h3>Please login</h3>");
        String       name = request.getString(ARG_USER_NAME, "");
        sb.append(HtmlUtil.form(URL_USER_LOGIN));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry("User:",
                                     HtmlUtil.input(ARG_USER_NAME, name)));
        sb.append(HtmlUtil.formEntry("Password:",
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));
        sb.append(HtmlUtil.formEntry("", HtmlUtil.submit("Login")));

        sb.append("</form>");
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
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUser(String id) throws Exception {
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
        ResultSet results = getRepository().execute(query).getResultSet();
        if ( !results.next()) {
            //            throw new IllegalArgumentException ("Could not find  user id:" + id + " sql:" + query);
            return null;
        } else {
            int col = 1;
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
                               SqlUtil.quote(user.getId()),
                               new String[] { COL_USERS_NAME,
                    COL_USERS_EMAIL, COL_USERS_QUESTION, COL_USERS_ANSWER,
                    COL_USERS_ADMIN }, new String[] {
                        SqlUtil.quote(user.getName()),
                        SqlUtil.quote(user.getEmail()),
                        SqlUtil.quote(user.getQuestion()),
                        SqlUtil.quote(user.getAnswer()), (user.getAdmin()
                    ? "1"
                    : "0") });
            getRepository().execute(query);
            return;
        }

        getRepository().execute(INSERT_USERS, new Object[] {
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
        String query = SqlUtil.makeDelete(TABLE_USERS, COL_USERS_ID,
                                          SqlUtil.quote(user.getId()));
        getRepository().execute(query);
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
    public Result adminUser(Request request) throws Exception {
        String userId = request.getUser();
        User   user   = findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("Could not find user:"
                    + userId);
        }


        if (request.defined(ARG_DELETE_CONFIRM)) {
            deleteUser(user);
            return new Result(getAdmin().URL_ADMIN_USER_LIST.toString());
        }

        if (request.defined(ARG_CHANGE)) {
            user.setName(request.getString(ARG_USER_NAME, user.getName()));
            user.setEmail(request.getString(ARG_USER_EMAIL, user.getEmail()));
            user.setQuestion(request.getString(ARG_USER_QUESTION,
                    user.getQuestion()));
            user.setAnswer(request.getString(ARG_USER_ANSWER,
                                             user.getAnswer()));
            if ( !request.defined(ARG_USER_ADMIN)) {
                user.setAdmin(false);
            } else {
                user.setAdmin(request.get(ARG_USER_ADMIN, user.getAdmin()));
            }
            makeOrUpdateUser(user, true);
        }


        StringBuffer sb = new StringBuffer();
        sb.append(getRepository().header("User: " + user.getName()));
        sb.append("\n<p/>\n");
        sb.append(HtmlUtil.form(getAdmin().URL_ADMIN_USER));
        sb.append("\n");
        sb.append(HtmlUtil.hidden(ARG_USER, user.getId()));
        sb.append("\n");
        if (request.defined(ARG_DELETE)) {
            sb.append("Are you sure you want to delete the user?  ");
            sb.append(HtmlUtil.submit("Yes", ARG_DELETE_CONFIRM));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit("Cancel", ARG_CANCEL));
        } else {
            sb.append(HtmlUtil.formTable());
            sb.append(HtmlUtil.formEntry("Name:",
                                         HtmlUtil.input(ARG_USER_NAME,
                                             user.getName())));
            sb.append(HtmlUtil.formEntry("Admin:",
                                         HtmlUtil.checkbox(ARG_USER_ADMIN,
                                             "true", user.getAdmin())));
            sb.append(
                HtmlUtil.formEntry(
                    "",
                    HtmlUtil.submit("Change User", ARG_CHANGE)
                    + HtmlUtil.space(2)
                    + HtmlUtil.submit("Delete User", ARG_DELETE)));
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminUserList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getRepository().header("Users"));
        String query = SqlUtil.makeSelect(COLUMNS_USERS,
                                          Misc.newList(TABLE_USERS));

        SqlUtil.Iterator iter =
            SqlUtil.getIterator(getRepository().execute(query));
        ResultSet  results;

        List<User> users = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                users.add(getUser(results));
            }
        }
        sb.append("<table>");
        sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold("ID"),
                                             HtmlUtil.bold("Name"),
                                             HtmlUtil.bold("Admin?"))));
        for (User user : users) {
            sb.append(
                HtmlUtil.row(
                    HtmlUtil.cols(
                        HtmlUtil.href(
                            HtmlUtil.url(
                                getAdmin().URL_ADMIN_USER, ARG_USER,
                                user.getId()), user.getId()), user.getName(),
                                    "" + user.getAdmin())));
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
        return new User(results.getString(col++), results.getString(col++),
                        results.getString(col++), results.getString(col++),
                        results.getString(col++), results.getString(col++),
                        results.getBoolean(col++));

    }



    private List<Entry> getCart(Request request) {
        User user = request.getRequestContext().getUser();
        List<Entry> cart = (List<Entry>) userCart.get(user);
        if(cart==null) {
            cart = new ArrayList<Entry>();
            userCart.put(user, cart);
        }
        return cart;
    }

    private void addToCart(Request request,List<Entry>entries) throws Exception {
        List<Entry> cart = getCart(request);
        for(Entry entry : entries) {
            if(!cart.contains(entry)) {
                cart.add(entry);
            }
        }
    }


    public Result processCart(Request request) throws Exception {
        String action = request.getString(ARG_ACTION,"");
        User user = request.getRequestContext().getUser();
        StringBuffer sb = new StringBuffer();
        if(user.getAnonymous()) {
            sb.append("No cart available for anonymous user");
            return new Result("User Cart", sb);
        }
        if(action.equals(ACTION_CLEAR)) {
            getCart(request).clear();
        } else  if(action.equals(ACTION_ADD)) {
            Entry entry = getRepository().getEntry(request.getId(""), request);
            if(entry == null) {
                throw new IllegalArgumentException(
                                                   "Could not find entry with id:" + request.getId(""));
            }
            if(!getCart(request).contains(entry)) {
                getCart(request).add(entry);
            }
        }

        return showCart(request);
    }



    public Result showCart(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<Entry> entries = getCart(request);
        sb.append("<h3>User Cart</h3>");
        if(entries.size()==0) {
            sb.append("No entries in cart");
        } else {
            sb.append(HtmlUtil.href(HtmlUtil.url(URL_USER_CART,ARG_ACTION, ACTION_CLEAR),"Clear Cart"));
            boolean haveFrom = request.defined(ARG_FROM);
            if(haveFrom) {
                Entry fromEntry = getRepository().getEntry(request.getString(ARG_FROM,""),request);
                sb.append("<br>Pick an entry  to associate with: " + fromEntry.getName());
            }
            sb.append("<ul>");
            OutputHandler outputHandler =  getRepository().getOutputHandler(request);
            for(Entry entry: entries) {
                sb.append("<li> ");
                if(haveFrom) {
                    sb.append(HtmlUtil.href(HtmlUtil.url(getRepository().URL_ASSOCIATION_ADD, ARG_FROM, request.getString(ARG_FROM,""),ARG_TO, entry.getId()),
                                            HtmlUtil.img(
                                                         getRepository().fileUrl("/Association.gif"),
                                                         "Create an association")));
                } else {
                    sb.append(HtmlUtil.href(HtmlUtil.url(URL_USER_CART, ARG_FROM, entry.getId()),
                                            HtmlUtil.img(
                                                         getRepository().fileUrl("/Association.gif"),
                                                         "Create an association")));
                }
                sb.append(HtmlUtil.space(1));
                sb.append(outputHandler.getEntryUrl(entry));
            }
           
            sb.append("</ul>");
        }
        Result result = new Result("User Cart", sb);
        return result;
    }


    public String getUserLinks(Request request) {
        User   user = request.getRequestContext().getUser();
        String userLink;
        if (user.getAnonymous()) {
            userLink =
                "<a href=\"${root}/user/login\" class=\"navlink\">Login</a>";
        } else {
            String cartEntry = HtmlUtil.href(URL_USER_CART, HtmlUtil.img(
                                                                                    getRepository().fileUrl("/Cart.gif"),
                                                                                    "Data Cart"));

            userLink = cartEntry + HtmlUtil.space(1) +
                HtmlUtil.href(URL_USER_SETTINGS, user.getLabel(), " class=\"navlink\" ");
        }
        return userLink;
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
        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_USER_NAME)) {
            String name     = request.getString(ARG_USER_NAME, "");
            String password = request.getString(ARG_USER_PASSWORD1, "");
            sb.append("Could not log you in");
        }

        sb.append(makeLoginForm(request));


        Result result = new Result("Login", sb);
        return result;
    }


    /** _more_ */
    public static final String OUTPUT_CART = "user.cart";

    protected void initOutputHandlers() throws Exception {
        OutputHandler outputHandler = new OutputHandler(getRepository()) {
                public boolean canHandle(String output) {
                    return output.equals(OUTPUT_CART);
                }
                protected void getOutputTypesForEntries(Request request,
                                                        List<Entry> entries, List types)
                    throws Exception {
                    types.add(new TwoFacedObject("Cart", OUTPUT_CART));
                }
                public Result outputEntries(Request request, List<Entry> entries)
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSettings(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        Result       result = new Result("User Settings", sb);
        return result;
    }




}

