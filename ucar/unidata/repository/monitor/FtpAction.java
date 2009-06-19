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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository.monitor;

import org.apache.commons.net.ftp.*;

import ucar.unidata.repository.*;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class FtpAction extends MonitorAction {
    public static final String PROP_FTP_SERVER = "ftp.server";
    public static final String PROP_FTP_DIRECTORY = "ftp.directory";
    public static final String PROP_FTP_FILETEMPLATE = "ftp.filetemplate";
    public static final String PROP_FTP_USER = "ftp.user";
    public static final String PROP_FTP_PASSWORD = "ftp.password";


    private String  server="";
    private String  directory="";
    private String  fileTemplate="${filename}";
    private String  user="";
    private String  password="";


    /**
     * _more_
     */
    public FtpAction() {}


    /**
     * _more_
     *
     * @param id _more_
     */
    public FtpAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "FTP Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Put file via FTP";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.server    = request.getString(getArgId(PROP_FTP_SERVER), "");
        this.user      = request.getString(getArgId(PROP_FTP_USER), "");
        this.directory   = request.getString(getArgId(PROP_FTP_DIRECTORY), "");
        this.fileTemplate   = request.getString(getArgId(PROP_FTP_FILETEMPLATE), "");
        this.password = request.getString(getArgId(PROP_FTP_PASSWORD), "");
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     */
    public void addToEditForm(EntryMonitor monitor, StringBuffer sb) {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.colspan("FTP Action", 2));


        sb.append(HtmlUtil.formEntry(monitor.getRepository().msgLabel("FTP Server"),
                                     HtmlUtil.input(getArgId(PROP_FTP_SERVER), server,
                                                    HtmlUtil.SIZE_60)));
        sb.append(HtmlUtil.formEntry(monitor.getRepository().msgLabel("FTP Directory"),
                                     HtmlUtil.input(getArgId(PROP_FTP_DIRECTORY), directory,
                                                    HtmlUtil.SIZE_60)));
        String tooltip =
            "macros: ${from_day}  ${from_month} ${from_year} ${from_monthname}  <br>"
            + "${to_day}  ${to_month} ${to_year} ${to_monthname} <br> "
            + "${filename}  ${fileextension} etc";

        sb.append(HtmlUtil.formEntry(monitor.getRepository().msgLabel("File Name Template"),
                                     HtmlUtil.input(getArgId(PROP_FTP_FILETEMPLATE), fileTemplate,
                                                    HtmlUtil.SIZE_60+HtmlUtil.title(tooltip))));
        sb.append(HtmlUtil.formEntry(monitor.getRepository().msgLabel("User ID"),
                                     HtmlUtil.input(getArgId(PROP_FTP_USER), user,
                                                    HtmlUtil.SIZE_60)));
        sb.append(HtmlUtil.formEntry(monitor.getRepository().msgLabel("Password"),
                                     HtmlUtil.password(getArgId(PROP_FTP_PASSWORD), password,
                                                    HtmlUtil.SIZE_20)));

        sb.append(HtmlUtil.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        FTPClient ftpClient = new FTPClient();
        try {
            Resource resource = entry.getResource();
            if ( !resource.isFile()) {
                return;
            }
            if(server.length()==0) return;

            String passwordToUse = monitor.getRepository().processTemplate(password, false);
            ftpClient.connect(server);
            if (user.length()>0) {
                ftpClient.login(user, password);
            }
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                monitor.handleError("FTP server refused connection:" + server,null);
                return;
            }
            ftpClient.setFileType(FTP.IMAGE_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            if(directory.length()>0) 
                ftpClient.changeWorkingDirectory(directory);

            String filename = monitor.getRepository().getEntryManager().replaceMacros(
                                                                         entry, fileTemplate);

            InputStream is = new BufferedInputStream(monitor.getRepository().getStorageManager().getFileInputStream(new File(resource.getPath())));
            boolean ok = ftpClient.storeUniqueFile(filename, is);
            is.close();
            if(ok) {
                monitor.logInfo ("Wrote file:" + directory +" " + filename);
            } else {
                monitor.handleError("Failed to write file:" + directory +" " + filename,null);
            }
        } catch (Exception exc) {
            monitor.handleError("Error posting to FTP:" + server, exc);
        } finally {
            try {
                ftpClient.logout();
            } catch (Exception exc) {}
            try {
                ftpClient.disconnect();
            } catch (Exception exc) {}
        }
    }



    /**
Set the Server property.

@param value The new value for Server
**/
public void setServer (String value) {
	server = value;
}

/**
Get the Server property.

@return The Server
**/
public String getServer () {
	return server;
}

/**
Set the Directory property.

@param value The new value for Directory
**/
public void setDirectory (String value) {
	directory = value;
}

/**
Get the Directory property.

@return The Directory
**/
public String getDirectory () {
	return directory;
}

/**
Set the User property.

@param value The new value for User
**/
public void setUser (String value) {
	user = value;
}

/**
Get the User property.

@return The User
**/
public String getUser () {
	return user;
}


/**
Set the Tmp property.

@param value The new value for Tmp
**/
public void setTmp (byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(XmlUtil.decodeBase64(new String(value)));
        }
}

/**
Get the Tmp property.

@return The Tmp
**/
public byte[] getTmp () {
        if (password == null) {
            return null;
        }
        return XmlUtil.encodeBase64(password.getBytes()).getBytes();
}



}

