/**
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

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class LogManager extends RepositoryManager {


    /** _more_ */
    private OutputStream fullLogFOS;


    private OutputStream runLogFOS;


    /** _more_ */
    public static boolean debug = true;



    /** _more_ */
    private List<LogEntry> log = new ArrayList<LogEntry>();

    private int requestCount = 0;

    public LogManager(Repository repository) {
        super(repository);
    }


    public void init() {
        try {
            fullLogFOS = new FileOutputStream(getStorageManager().getFullLogFile(), true);
            runLogFOS = new FileOutputStream(getStorageManager().getLogFile(), false);
        } catch(Exception exc) {
            throw new RuntimeException (exc);
        }
    }

    public void logRequest(Request request) {
        requestCount++;
        //Keep the size of the log at 200
        synchronized (log) {
            while (log.size() > 200) {
                log.remove(0);
            }
            log.add(new LogEntry(request));
        }
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void debug(String message) {
        if (debug) {
            logInfo(message);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<LogEntry> getLog() {
        synchronized (log) {
            return new ArrayList<LogEntry>(log);
        }
    }

    public int getRequestCount() {
        return requestCount;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    public void log(Request request, String message) {
        logInfo("user:" + request.getUser() + " -- " + message);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logInfo(String message) {
        log(message,null);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logError(String message) {
        logError(message, null);
    }



    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void logError(String message, Throwable exc) {
        log("Error:" +message,exc);
    }



    private void log(String message, Throwable exc) {
        Throwable thr = null;
        if (exc != null) {
            thr = LogUtil.getInnerException(exc);
        }

        if(true || getProperty(PROP_LOG_TOSTDERR,false)) {
            System.err.println(message);
            if (thr!=null) {
                if (thr instanceof RepositoryUtil.MissingEntryException) {
                    System.err.println(thr.getMessage());
                } else {
                    thr.printStackTrace();
                }
            }
        }

        try {
            String line = new Date() + " -- " + message;
            for(FileOutputStream os: (List<FileOutputStream>)Misc.newList(fullLogFOS,runLogFOS)) {
                synchronized(os) {
                    os.write(line.getBytes());
                    os.write("\n".getBytes());
                    if (thr != null) {
                        if (thr instanceof RepositoryUtil.MissingEntryException) {
                            os.write(thr.toString().getBytes());
                            os.write("\n".getBytes());
                        } else {
                            os.write("<stack>".getBytes());
                            os.write("\n".getBytes());
                            os.write(LogUtil.getStackTrace(thr).getBytes());
                            os.write("\n".getBytes());
                            os.write("</stack>".getBytes());
                        }
                    }
                    os.flush();
                }
            }
        } catch (Exception exc2) {
            System.err.println("Error writing log:" + exc2);
        }
    }



    /**
     * Class LogEntry _more_
     *
     *
     * @author IDV Development Team
     */
    public class LogEntry {

        /** _more_ */
        User user;

        /** _more_ */
        Date date;

        /** _more_ */
        String path;

        /** _more_ */
        String ip;

        /** _more_ */
        String userAgent;

        /** _more_ */
        String url;

        /**
         * _more_
         *
         * @param request _more_
         */
        public LogEntry(Request request) {
            this.user = request.getUser();
            this.path = request.getRequestPath();

            String entryPrefix = getRepository().URL_ENTRY_SHOW.toString();
            if (this.path.startsWith(entryPrefix)) {
                url       = request.getUrl();
                this.path = this.path.substring(entryPrefix.length());
                if (path.trim().length() == 0) {
                    path = "/entry/show";
                }

            }

            this.date      = new Date();
            this.ip        = request.getIp();
            this.userAgent = request.getUserAgent();
        }


        /**
         *  Set the Ip property.
         *
         *  @param value The new value for Ip
         */
        public void setIp(String value) {
            ip = value;
        }

        /**
         *  Get the Ip property.
         *
         *  @return The Ip
         */
        public String getIp() {
            return ip;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getUrl() {
            return url;
        }

        /**
         *  Set the UserAgent property.
         *
         *  @param value The new value for UserAgent
         */
        public void setUserAgent(String value) {
            userAgent = value;
        }

        /**
         *  Get the UserAgent property.
         *
         *  @return The UserAgent
         */
        public String getUserAgent() {
            return userAgent;
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
         *  Set the Date property.
         *
         *  @param value The new value for Date
         */
        public void setDate(Date value) {
            date = value;
        }

        /**
         *  Get the Date property.
         *
         *  @return The Date
         */
        public Date getDate() {
            return date;
        }

        /**
         *  Set the Path property.
         *
         *  @param value The new value for Path
         */
        public void setPath(String value) {
            path = value;
        }

        /**
         *  Get the Path property.
         *
         *  @return The Path
         */
        public String getPath() {
            return path;
        }
    }



}

