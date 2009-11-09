/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.auth;


import ucar.unidata.repository.*;


import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.Cache;
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
public class SessionManager extends RepositoryManager {

    /** The number of days a session is active in the database */
    private static final double SESSION_DAYS = 1.0;



    /** _more_ */
    public static final String COOKIE_NAME = "repositorysession";


    /** _more_ */
    private Hashtable<String, Session> sessionMap = new Hashtable<String,
                                                        Session>();



    /** _more_ */
    private List ipUserList = new ArrayList();


    /** _more_ */
    private Cache<Object, Object> sessionExtra = new Cache<Object,
                                                     Object>(5000);


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SessionManager(Repository repository) {
        super(repository);
        //        ipUserList.add("128.117.156.*");
        //        ipUserList.add("jeffmc");

    }

    /**
     * _more_
     */
    public void init() {
        Misc.run(new Runnable() {
            public void run() {
                cullSessions();
            }
        });
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String putSessionExtra(Object value) {
        String id = "${" + getRepository().getGUID() + "}";
        putSessionExtra(id, value);
        return id;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putSessionExtra(Object key, Object value) {
        sessionExtra.put(key, value);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getSessionExtra(Object key) {
        return sessionExtra.get(key);
    }




    /**
     * _more_
     */
    private void cullSessions() {
        //Wait a while before starting
        Misc.sleepSeconds(60);
        //        Misc.sleepSeconds(5);
        while (true) {
            try {
                cullSessionsInner();
            } catch (Exception exc) {
                logException("Culling sessions", exc);
                return;
            }
            //            cull every hour
            Misc.sleepSeconds(60 * 60);
            //            Misc.sleepSeconds(5);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void cullSessionsInner() throws Exception {
        List<Session> sessionsToDelete = new ArrayList<Session>();
        long          now              = new Date().getTime();
        Statement stmt = getDatabaseManager().select(Tables.SESSIONS.COLUMNS,
                             Tables.SESSIONS.NAME, (Clause) null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        double           timeDiff = DateUtil.daysToMillis(SESSION_DAYS);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Session session        = makeSession(results);
                Date    lastActiveDate = session.getLastActivity();
                //Check if the last activity was > 24 hours ago
                if ((now - lastActiveDate.getTime()) > timeDiff) {
                    sessionsToDelete.add(session);
                } else {}
            }
        }
        for (Session session : sessionsToDelete) {
            removeSession(session.getId());
        }

    }


    //TODO: we need to clean out old sessions every once in a while

    /**
     * _more_
     *
     * @param sessionId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Session getSession(String sessionId) throws Exception {
        Session session = sessionMap.get(sessionId);
        if (session == null) {
            Statement stmt = getDatabaseManager().select(
                                 Tables.SESSIONS.COLUMNS,
                                 Tables.SESSIONS.NAME,
                                 Clause.eq(
                                     Tables.SESSIONS.COL_SESSION_ID,
                                     sessionId));
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
            //COL_SESSION_ID,COL_USER_ID,COL_CREATE_DATE,COL_LAST_ACTIVE_DATE,COL_EXTRA
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    session = makeSession(results);
                    session.setLastActivity(new Date());
                    //Remove it from the DB and then readd it so we update the lastActivity
                    removeSession(session.getId());
                    addSession(session);
                    sessionMap.put(sessionId, session);
                    break;
                }
            }
        }
        return session;
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
    private Session makeSession(ResultSet results) throws Exception {
        int    col       = 1;
        String sessionId = results.getString(col++);
        String userId    = results.getString(col++);
        User   user      = getUserManager().findUser(userId);
        if (user == null) {
            user = getUserManager().getAnonymousUser();
        }
        Date createDate     = getDatabaseManager().getDate(results, col++);
        Date lastActiveDate = getDatabaseManager().getDate(results, col++);
        //See if we have it in the map
        Session session = sessionMap.get(sessionId);
        if (session != null) {
            return session;
        }
        return new Session(sessionId, user, createDate, lastActiveDate);
    }


    /**
     * _more_
     *
     * @param sessionId _more_
     *
     * @throws Exception _more_
     */
    public void removeSession(String sessionId) throws Exception {
        sessionMap.remove(sessionId);
        getDatabaseManager().delete(Tables.SESSIONS.NAME,
                                    Clause.eq(Tables.SESSIONS.COL_SESSION_ID,
                                        sessionId));
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @throws Exception _more_
     */
    public void addSession(Session session) throws Exception {
        sessionMap.put(session.getId(), session);
        //COL_SESSION_ID,COL_USER_ID,COL_CREATE_DATE,COL_LAST_ACTIVE_DATE,COL_EXTRA
        getDatabaseManager().executeInsert(Tables.SESSIONS.INSERT,
                                           new Object[] { session.getId(),
                session.getUserId(), new Date(), new Date(), "" });
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Session> getSessions() throws Exception {
        List<Session> sessions = new ArrayList<Session>();
        Statement stmt = getDatabaseManager().select(Tables.SESSIONS.COLUMNS,
                             Tables.SESSIONS.NAME, (Clause) null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                sessions.add(makeSession(results));
            }
        }
        return sessions;
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void checkSession(Request request) throws Exception {

        User         user    = request.getUser();
        List<String> cookies = getCookies(request);

        for (String cookieValue : cookies) {
            request.setSessionId(cookieValue);
            if (user == null) {
                Session session = getSession(request.getSessionId());
                if (session != null) {
                    session.setLastActivity(new Date());
                    user = getUserManager().getCurrentUser(session.getUser());
                    session.setUser(user);
                    break;
                }
            }
        }


        if ((user == null) && request.hasParameter(ARG_SESSIONID)) {
            Session session = getSession(request.getString(ARG_SESSIONID));
            if (session != null) {
                session.setLastActivity(new Date());
                user = getUserManager().getCurrentUser(session.getUser());
                session.setUser(user);
            }
        }

        //Check for url auth
        if ((user == null) && request.exists(ARG_AUTH_USER)
                && request.exists(ARG_AUTH_PASSWORD)) {
            String userId   = request.getString(ARG_AUTH_USER, "");
            String password = request.getString(ARG_AUTH_PASSWORD, "");
            user = getUserManager().findUser(userId, false);
            if (user == null) {
                throw new IllegalArgumentException(msgLabel("Unknown user")
                        + userId);
            }
            if ( !user.getPassword().equals(
                    getUserManager().hashPassword(password))) {
                throw new IllegalArgumentException(msg("Incorrect password"));
            }
            setUserSession(request, user);
        }


        //Check for basic auth
        if (user == null) {
            String auth =
                (String) request.getHttpHeaderArgs().get("Authorization");
            if (auth == null) {
                auth = (String) request.getHttpHeaderArgs().get(
                    "authorization");
            }

            if (auth != null) {
                auth = auth.trim();
                //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
                if (auth.startsWith("Basic")) {
                    auth = new String(
                        XmlUtil.decodeBase64(auth.substring(5).trim()));
                    String[] toks = StringUtil.split(auth, ":", 2);
                    if (toks.length == 2) {
                        user = getUserManager().findUser(toks[0], false);
                        if (user == null) {
                            //                            throw new AccessException(
                            //                                msgLabel("Unknown user") + toks[0],request);
                        } else if ( !user.getPassword().equals(
                                getUserManager().hashPassword(toks[1]))) {
                            //                            throw new AccessException(
                            //                                msg("Incorrect password"),request);
                            user = null;
                        } else {}
                    }
                    if (user != null) {
                        setUserSession(request, user);
                    }
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
                        user = getUserManager().findUser(userName, false);
                        if (user == null) {
                            user = new User(userName, false);
                            getUserManager().makeOrUpdateUser(user, false);
                        }
                    }
                }
            }
        }


        if (request.getSessionId() == null) {
            //            request.setSessionId(getSessionId());
        }

        //Make sure we have the current user state
        user = getUserManager().getCurrentUser(user);

        if (user == null) {
            user = getUserManager().getAnonymousUser();
        }


        request.setUser(user);
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
    private List<String> getCookies(Request request) throws Exception {
        List<String> cookies = new ArrayList<String>();
        String       cookie  = request.getHeaderArg("Cookie");
        if (cookie == null) {
            return cookies;
        }
        request.tmp.append("cookie from header:" + cookie + "<p>");

        List toks = StringUtil.split(cookie, ";", true, true);
        for (int i = 0; i < toks.size(); i++) {
            String tok     = (String) toks.get(i);
            List   subtoks = StringUtil.split(tok, "=", true, true);
            if (subtoks.size() != 2) {
                continue;
            }
            String cookieName  = (String) subtoks.get(0);
            String cookieValue = (String) subtoks.get(1);
            if (cookieName.equals(COOKIE_NAME)) {
                cookies.add(cookieValue);
            }
        }
        request.tmp.append("cookies:" + cookies + "<p>");
        return cookies;
    }




    /** _more_ */
    private Hashtable sessionMessages;
    //    String sessionMessage;


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getSessionMessage(Request request) {
        String sessionMessage = null;
        Object id             = request.getSessionId();
        if ((id != null) && (sessionMessages != null)) {
            synchronized (sessionMessages) {
                sessionMessage = (String) sessionMessages.get(id);
                if (sessionMessage != null) {
                    sessionMessages.remove(id);
                }
            }
        }
        return sessionMessage;
    }

    /**
     * _more_
     *
     * @param message _more_
     *
     * @throws Exception _more_
     */
    public void setSessionMessage(String message) throws Exception {
        sessionMessages = new Hashtable();
        if ((message != null) && (message.trim().length() > 0)) {
            synchronized (sessionMessages) {
                for (Session session : getSessions()) {
                    sessionMessages.put(session.getId(), message);
                }
            }
        }
    }







    /**
     * _more_
     *
     * @return _more_
     */
    public String getSessionId() {
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
    public void setUserSession(Request request, User user) throws Exception {
        if (request.getSessionId() == null) {
            request.setSessionId(getSessionId());
        }
        addSession(new Session(request.getSessionId(), user, new Date()));
        request.setUser(user);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void removeUserSession(Request request) throws Exception {
        if (request.getSessionId() != null) {
            removeSession(request.getSessionId());
        }
        List<String> cookies = getCookies(request);
        for (String cookieValue : cookies) {
            removeSession(cookieValue);
        }
        request.setUser(getUserManager().getAnonymousUser());
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
    public StringBuffer getSessionList(Request request) throws Exception {
        List<Session> sessions    = getSessions();
        StringBuffer  sessionHtml = new StringBuffer(HtmlUtil.formTable());
        sessionHtml.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    HtmlUtil.bold(msg("User")), HtmlUtil.bold(msg("Since")),
                    HtmlUtil.bold(msg("Last Activity")))));
        for (Session session : sessions) {
            String url = request.url(getRepositoryBase().URL_USER_LIST,
                                     ARG_REMOVESESSIONID, session.getId());
            sessionHtml.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.href(url,
                    HtmlUtil.img(iconUrl(ICON_DELETE))) + " "
                        + session.user.getLabel(), formatDate(request,
                            session.createDate), formatDate(request,
                                session.getLastActivity()))));
        }
        sessionHtml.append(HtmlUtil.formTableClose());
        return sessionHtml;
    }






    /**
     * Class Session _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class Session {

        /** _more_ */
        String id;

        /** _more_ */
        User user;

        /** _more_ */
        Date createDate;

        /** _more_ */
        Date lastActivity;

        /**
         * _more_
         *
         * @param id _more_
         * @param user _more_
         * @param createDate _more_
         */
        public Session(String id, User user, Date createDate) {
            this(id, user, createDate, new Date());
        }

        /**
         * _more_
         *
         * @param id _more_
         * @param user _more_
         * @param createDate _more_
         * @param lastActivity _more_
         */
        public Session(String id, User user, Date createDate,
                       Date lastActivity) {
            this.id           = id;
            this.user         = user;
            this.createDate   = createDate;
            this.lastActivity = lastActivity;
        }


        /**
         *  Set the Id property.
         *
         *  @param value The new value for Id
         */
        public void setId(String value) {
            id = value;
        }

        /**
         *  Get the Id property.
         *
         *  @return The Id
         */
        public String getId() {
            return id;
        }

        /**
         *  Set the User property.
         *
         *  @param value The new value for User
         */
        public void setUser(User value) {
            user = value;
        }

        /**
         *  Get the User property.
         *
         *  @return The User
         */
        public User getUser() {
            return user;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getUserId() {
            if (user == null) {
                return "";
            }
            return user.getId();
        }

        /**
         *  Set the CreateDate property.
         *
         *  @param value The new value for CreateDate
         */
        public void setCreateDate(Date value) {
            createDate = value;
        }

        /**
         *  Get the CreateDate property.
         *
         *  @return The CreateDate
         */
        public Date getCreateDate() {
            return createDate;
        }

        /**
         *  Set the LastActivity property.
         *
         *  @param value The new value for LastActivity
         */
        public void setLastActivity(Date value) {
            lastActivity = value;
        }

        /**
         *  Get the LastActivity property.
         *
         *  @return The LastActivity
         */
        public Date getLastActivity() {
            return lastActivity;
        }



    }


}

