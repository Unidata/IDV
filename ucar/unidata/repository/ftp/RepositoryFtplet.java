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


package ucar.unidata.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




import ucar.unidata.repository.*;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.text.SimpleDateFormat;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */

public class RepositoryFtplet extends DefaultFtplet {

    public static final String CMD_ABOR = "ABOR";
    public static final String CMD_CWD = "CWD";
    public static final String CMD_DELE = "DELE";
    public static final String CMD_LIST = "LIST";
    public static final String CMD_MDTM = "MDTM";
    public static final String CMD_MKD = "MKD";
    public static final String CMD_NLST = "NLST";
    public static final String CMD_PASS = "PASS";
    public static final String CMD_PASV = "PASV";
    public static final String CMD_PORT = "PORT";
    public static final String CMD_PWD = "PWD";
    public static final String CMD_QUIT = "QUIT";
    public static final String CMD_RETR = "RETR";
    public static final String CMD_RMD = "RMD";
    public static final String CMD_RNFR = "RNFR";
    public static final String CMD_RNTO = "RNTO";
    public static final String CMD_SITE = "SITE";
    public static final String CMD_SIZE = "SIZE";
    public static final String CMD_STOR = "STOR";
    public static final String CMD_TYPE = "TYPE";
    public static final String CMD_USER = "USER";
    public static final String CMD_ACCT = "ACCT";
    public static final String CMD_APPE = "APPE";
    public static final String CMD_CDUP = "CDUP";
    public static final String CMD_HELP = "HELP";
    public static final String CMD_MODE = "MODE";
    public static final String CMD_NOOP = "NOOP";
    public static final String CMD_REIN = "REIN";
    public static final String CMD_STAT = "STAT";
    public static final String CMD_STOU = "STOU";
    public static final String CMD_STRU = "STRU";
    public static final String CMD_SYST = "SYST";



    /** _more_          */
    public static final String PROP_ENTRYID = "ramadda.entryid";

    public static final String NL = "\r\n";

    /** _more_          */
    FtpManager ftpManager;

    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     * @param ftpManager _more_
     */
    public RepositoryFtplet(FtpManager ftpManager) {
        this.ftpManager = ftpManager;
        sdf = getRepository().makeSDF("MMM dd HH:mm");
    }

    /**
     * _more_
     *
     * @param ftpletContext _more_
     *
     * @throws FtpException _more_
     */
    public void init(FtpletContext ftpletContext) throws FtpException {
        super.init(ftpletContext);
    }

    /**
     * _more_
     *
     * @param session _more_
     * @param request _more_
     * @param reply _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult afterCommand(FtpSession session, FtpRequest ftpRequest,
                                     FtpReply reply)
            throws FtpException, IOException {
        return super.afterCommand(session, ftpRequest, reply);
    }


    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult beforeCommand(FtpSession session, FtpRequest ftpRequest)
            throws FtpException, IOException {
        try {
            System.err.println("command:" + ftpRequest.getCommand() + " arg:" + ftpRequest.getArgument());
            if (ftpRequest.getCommand().equals(CMD_LIST)) {
                return handleList(getRequest(session), session, ftpRequest);
            }

            if (ftpRequest.getCommand().equals(CMD_SYST)) {
                return handleSyst(getRequest(session), session, ftpRequest);
            }
            if (ftpRequest.getCommand().equals(CMD_PWD)) {
                return handlePwd(getRequest(session), session, ftpRequest);
            }
            if (ftpRequest.getCommand().equals(CMD_CWD)) {
                return handleCwd(getRequest(session), session, ftpRequest);
            }
            if (ftpRequest.getCommand().equals(CMD_RETR)) {
                return handleRetr(getRequest(session), session, ftpRequest);
            }
            return super.beforeCommand(session, ftpRequest);
            //          return FtpletResult.SKIP;
        } catch (Exception exc) {
            exc.printStackTrace();
            ftpManager.logError("Error handling ftp request:" + ftpRequest.getCommand() +" arg:"  + ftpRequest.getArgument(), exc);
            return handleError(session, ftpRequest, "Error:" + exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private Repository getRepository() {
        return ftpManager.getRepository();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private EntryManager getEntryManager() {
        return getRepository().getEntryManager();
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Request getRequest(FtpSession session) throws Exception {
        try {
            return new Request(
                getRepository(),
                getRepository().getUserManager().getAnonymousUser());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Group getGroup(Request request, FtpSession session) throws Exception {
        String id = (String) session.getAttribute(PROP_ENTRYID);
        if (id == null) {
            return getEntryManager().getTopGroup();
        }
        return (Group) getEntryManager().getEntry(request, id);
    }


    /**
     * _more_
     *
     * @param session _more_
     * @param request _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleError(FtpSession session, FtpRequest ftpRequest,
                                    String message) throws     FtpException, IOException {
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN , message));
        return FtpletResult.SKIP;

    }


                                    

    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handlePwd(Request request,FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        StringBuffer result = new StringBuffer();
        Group        group  = getGroup(request, session);
        if (group == null) {
            return handleError(session, ftpRequest, "No current group");
        }
        if(group.isTopGroup()) {
            result.append("\"" +"/"+"\"");
        } else {
            result.append("\"" +group.getFullName()+"\"");
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED,
                                          result.toString()));
        return FtpletResult.SKIP;
    }

    public FtpletResult handleSyst(Request request,FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        session.write(new DefaultFtpReply(FtpReply.REPLY_215_NAME_SYSTEM_TYPE ,
                                          "UNIX Type: L8"));
        return FtpletResult.SKIP;
    }

    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleList(Request request, FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        //      if(true)        return super.beforeCommand(session, ftpRequest);
        //dr-x------   3 user group            0 Oct 20 14:27 Desktop

        StringBuffer result = new StringBuffer();
        Group        group  = getGroup(request, session);
        if (group == null) {
            return handleError(session, ftpRequest, "No current group");
        }

        List<Entry> children =
            getEntryManager().getChildren(getRequest(session), group);
        System.err.println("group:" + group.getName() +" children:" + children.size());
        for (Entry e : children) {
            String prefix;
            String name;
            String size;
            String w = "-";
            String permissions;
            if (e.isFile()) {
                size = ""+e.getResource().getFileSize();
                name = getRepository().getStorageManager().getFileTail(e);
                permissions = "-rw-r--r-- 1";
            } else if(e.isGroup()) {
                name = e.getName();
                prefix = "d";
                size = "0";
                permissions = "drwxr-xr-x 1";
            } else {
                continue;
            }

            result.append(permissions);
            result.append(" ");
            result.append(e.getUser().getId());
            result.append(" ");
            result.append(" ramadda ");
            result.append(" ");
            result.append(StringUtil.padLeft(size,13," "));
            result.append(" ");
            result.append(sdf.format(new Date(e.getCreateDate())));
            result.append(" ");
            result.append(name);
            result.append(NL);
        }
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_150_FILE_STATUS_OKAY,
                " Here comes the directory listing.."));

        System.err.println(" result:" +result);
        session.getDataConnection().openConnection().transferToClient(
            session, result.toString());
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                "Directory send OK."));

        session.getDataConnection().closeDataConnection();


        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param session _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleRetr(Request request, FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        Group        group  = getGroup(request, session);



        if (group == null) {
            return handleError(session, ftpRequest, "No cwd");
        }


        Entry entry = findEntry(request, group, ftpRequest.getArgument());
        if (entry == null) {
            return handleError(session, ftpRequest, "Not a valid file:" + ftpRequest.getArgument());
        }

        if(!entry.isFile()) {
            return handleError(session, ftpRequest, "Not a file");
        }


        if(!getRepository().getAccessManager().canAccessFile(request, entry)) {
            return handleError(session, ftpRequest, "You don't have permission to get the file");
        }



        File file   = entry.getFile();
        long length = file.length();
        InputStream inputStream =
            getRepository().getStorageManager().getFileInputStream(file);

        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_150_FILE_STATUS_OKAY,
                "Opening binary mode connect."));
        session.getDataConnection().openConnection().transferToClient(
            session, inputStream);
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                "Closing data connection."));

        session.getDataConnection().closeDataConnection();

        return FtpletResult.SKIP;
    }


    private Entry findEntry(Request request,  Group parent,String  name) throws Exception {
        if(name.endsWith("/")) name = name.substring(0, name.length()-1);
        //        System.err.println("name:" + name+":");
        if(name.length()==0) {
            return getEntryManager().getTopGroup();
        }
        if(name.startsWith("/")) {
            if(name.startsWith("/")) name = name.substring(1);
            if(name.length()==0) {
                return getEntryManager().getTopGroup();
            }
            Entry entry = getEntryManager().findEntryFromName(name, request.getUser(), false);
            return entry;
        } else {
            Entry entry = getEntryManager().findEntryWithName(request, parent, name);
            if(entry == null || entry.getName().equals(parent)) return parent;
            return entry;
        }
    }


    /**
     * _more_
     *
     * @param session _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleCwd(Request request, FtpSession session, FtpRequest ftpRequest)
            throws Exception {

        StringBuffer result = new StringBuffer();
        Group        group  = getGroup(request, session);
        if (group == null) {
            return handleError(session, ftpRequest, "No current group");
        }
        String subGroupName =ftpRequest.getArgument().trim();
        Entry entry =findEntry(request, group, subGroupName);
        if (entry == null) {
            return handleError(session, ftpRequest, "Not a valid directory:" + subGroupName);
        }
        if (!entry.isGroup()) {
            return handleError(session, ftpRequest, "Not a valid directory:" + subGroupName);
        }
        Group subGroup = (Group) entry;

        session.setAttribute(PROP_ENTRYID, subGroup.getId());
        result.append("Directory successfully changed");
        session.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY,
                                          result.toString()));
        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult onLogin(FtpSession session, FtpRequest ftpRequest)
            throws FtpException, IOException {
        return FtpletResult.DEFAULT;
    }

}

