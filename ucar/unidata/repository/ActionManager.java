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
public class ActionManager extends RepositoryManager {


    /** _more_ */
    public RequestUrl URL_STATUS = new RequestUrl(this, "/status");


    /** _more_ */
    private Hashtable<Object, ActionInfo> actions = new Hashtable<Object,
                                                        ActionInfo>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public ActionManager(Repository repository) {
        super(repository);
    }



    public void setContinueHtml(Object actionId, String html) {
        if(actionId == null) return;
        ActionInfo   action = getAction(actionId);
        if(action!=null) action.setContinueHtml(html);
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
    public Result processStatus(Request request) throws Exception {

        String       id     = request.getString(ARG_ACTION_ID, "");
        ActionInfo   action = getAction(id);
        StringBuffer sb     = new StringBuffer();
        if (action == null) {
            sb.append(msg("No action found"));
            return new Result(msg("Status"), sb);
        }

        sb.append(msgHeader("Action: " + action.getName()));
        if (request.exists(ARG_CANCEL)) {
            action.setRunning(false);
            actions.remove(id);
            sb.append(msg("Action canceled"));
        } else {
            if (action.getError() != null) {
                sb.append(getRepository().error(msg("Error") +"<p>"+action.getError()));
                actions.remove(id);
            } else if ( !action.getRunning()) {
                sb.append(getRepository().note(msg("Completed")));
                sb.append(action.getContinueHtml());
                actions.remove(id);
            } else {
                sb.append("<meta http-equiv=\"refresh\" content=\"2\">");
                sb.append(getRepository().progress(msg("In progress")));
                sb.append(HtmlUtil.href(HtmlUtil.url(URL_STATUS,
                                                     ARG_ACTION_ID, id), msg("Reload")));
                sb.append("<p>");
                sb.append(action.getMessage());
                sb.append("<p>");
                sb.append(HtmlUtil.form(URL_STATUS));
                sb.append(HtmlUtil.submit(msg("Cancel Action"), ARG_CANCEL));
                sb.append(HtmlUtil.hidden(ARG_ACTION_ID, id));
                sb.append(HtmlUtil.formClose());
            }
        }
        return new Result(msg("Status"), sb);
    }






    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected ActionInfo getAction(Object id) {
        if (id == null) {
            return null;
        }
        return actions.get(id);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected boolean getActionOk(Object id) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return false;
        }
        return action.getRunning();
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param msg _more_
     */
    protected void setActionMessage(Object id, String msg) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setMessage(msg);
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    protected void actionComplete(Object id) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setRunning(false);
    }

    /**
     * _more_
     *
     * @param actionId _more_
     * @param exc _more_
     */
    protected void handleError(Object actionId, Exception exc) {
        ActionInfo action = getAction(actionId);
        if (action == null) {
            return;
        }
        action.setError("An error has occurred:" + exc);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param continueHtml _more_
     *
     * @return _more_
     */
    protected Object addAction(String msg, String continueHtml) {
        String id = getRepository().getGUID();
        actions.put(id, new ActionInfo(msg, continueHtml));
        return id;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     *
     * @return _more_
     */
    protected Result doAction(Request request, final Action runnable,
                              String name, String continueHtml) {
        Object actionId = runAction(runnable, name, continueHtml);
        return new Result(HtmlUtil.url(URL_STATUS, ARG_ACTION_ID,
                                       "" + actionId));
    }


    /**
     * _more_
     *
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     *
     * @return _more_
     */
    protected Object runAction(final Action runnable, String name,
                               String continueHtml) {
        final Object actionId = addAction(name, continueHtml);
        Misc.run(new Runnable() {
            public void run() {
                try {
                    runnable.run(actionId);
                } catch (Exception exc) {
                    //TODO: handle the error better
                    handleError(actionId, exc);
                    return;
                }
                actionComplete(actionId);
            }
        });
        return actionId;
    }


    /**
     * Action _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static interface Action {

        /**
         * _more_
         *
         * @param actionId _more_
         *
         * @throws Exception _more_
         */
        public void run(Object actionId) throws Exception;
    }


    /**
     * Class ActionInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class ActionInfo {

        /** _more_ */
        private String name;

        /** _more_ */
        private boolean running = true;

        /** _more_ */
        private String message = "";

        /** _more_ */
        private String continueHtml;

        /** _more_ */
        private String error = null;


        /**
         * _more_
         *
         * @param name _more_
         * @param continueHtml _more_
         */
        public ActionInfo(String name, String continueHtml) {
            this.name         = name;
            this.continueHtml = continueHtml;
        }


        /**
         *  Set the Name property.
         *
         *  @param value The new value for Name
         */
        public void setName(String value) {
            name = value;
        }

        /**
         *  Get the Name property.
         *
         *  @return The Name
         */
        public String getName() {
            return name;
        }

        /**
         *  Set the Running property.
         *
         *  @param value The new value for Running
         */
        public void setRunning(boolean value) {
            running = value;
        }

        /**
         *  Get the Running property.
         *
         *  @return The Running
         */
        public boolean getRunning() {
            return running;
        }



        /**
         * Set the Message property.
         *
         * @param value The new value for Message
         */
        public void setMessage(String value) {
            message = value;
        }

        /**
         * Get the Message property.
         *
         * @return The Message
         */
        public String getMessage() {
            return message;
        }


        /**
         * Set the ContinueHtml property.
         *
         * @param value The new value for ContinueHtml
         */
        public void setContinueHtml(String value) {
            continueHtml = value;
        }

        /**
         * Get the ContinueHtml property.
         *
         * @return The ContinueHtml
         */
        public String getContinueHtml() {
            return continueHtml;
        }

        /**
         * Set the Error property.
         *
         * @param value The new value for Error
         */
        public void setError(String value) {
            error = value;
        }

        /**
         * Get the Error property.
         *
         * @return The Error
         */
        public String getError() {
            return error;
        }




    }



}

