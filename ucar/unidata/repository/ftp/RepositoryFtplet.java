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




import ucar.unidata.repository.*;

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

import java.io.IOException;

import org.apache.ftpserver.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */

    public class RepositoryFtplet extends DefaultFtplet  {
	public static final String PROP_ENTRYID = "ramadda.entryid";


	FtpManager ftpManager;
	public RepositoryFtplet(FtpManager ftpManager) {
	    this.ftpManager = ftpManager;
	}

	public	    void init(FtpletContext ftpletContext)  throws FtpException {
	    System.err.println ("init ");
	    super.init(ftpletContext);
	}

	public	FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
	    System.err.println ("after " + request.getCommand());
	    return super.afterCommand(session, request, reply);
	}

	public	    FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
	    try {
	    System.err.println ("beforeCommand " + request.getCommand());
	    if(request.getCommand().equals("LIST")) {
		return handleList(session, request);
	    }
	    if(request.getCommand().equals("PWD")) {
		return handlePwd(session, request);
	    }
	    if(request.getCommand().equals("CWD")) {
		return handleCwd(session, request);
	    }
	    //	    session.write(new DefaultFtpReply(FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED , "not implemented"));
	    return super.beforeCommand(session, request);
//	    return FtpletResult.SKIP;
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}


	private Repository getRepository() {
	    return ftpManager.getRepository();
	}


	private EntryManager getEntryManager() {
	    return getRepository().getEntryManager();
	}

	private Request getRequest(FtpSession session) throws Exception {
	    try {
	    return new Request(getRepository(), getRepository().getUserManager().getAnonymousUser());
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}

	private Group getGroup(FtpSession session)  throws Exception {
	    String id = (String)session.getAttribute(PROP_ENTRYID);
	    if(id==null) {
		return getEntryManager().getTopGroup();
	    }
	    return (Group)getEntryManager().getEntry(getRequest(session), id);
	}


	public FtpletResult handleError(FtpSession session, FtpRequest request, String message) throws Exception  {
	    session.write(new DefaultFtpReply(FtpReply.REPLY_452_REQUESTED_ACTION_NOT_TAKEN , message));
	    return FtpletResult.SKIP;
	    
	}


	public FtpletResult handlePwd(FtpSession session, FtpRequest request) throws Exception {
	    StringBuffer result  = new StringBuffer();
	    Group group = getGroup(session);
	    if(group == null) {
		return handleError( session,  request, "No current group");
	    }
	    result.append("\n");
	    result.append(group.getFullName());
	    session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, result.toString()));
	    return FtpletResult.SKIP;
	}

	public FtpletResult handleList(FtpSession session, FtpRequest request) throws Exception {
	    //	    if(true) 	    return super.beforeCommand(session, request);
	    //dr-x------   3 user group            0 Oct 20 14:27 Desktop

	    StringBuffer result  = new StringBuffer();
	    Group group = getGroup(session);
	    if(group == null) {
		return handleError( session,  request, "No current group");
	    }
	    List<Entry> children = getEntryManager().getChildren(getRequest(session), group);
	    result.append("\n");
	    for(Entry e: children) {
		String permissions = "dr-x------";
  		result.append(permissions);
		result.append("   ");
		result.append("3 ");
		result.append(e.getUser().getId());
		result.append(" ");
		result.append(" ramadda ");
		result.append(" ");		
		if(e.isFile()) {
		    result.append(e.getResource().getFileSize());
		} else {
		    result.append("0");
		}
		    result.append(" ");
		result.append(e.getName());
		result.append("\n");
	    }
	    session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
	    session.getDataConnection().openConnection().transferToClient(session, result.toString());
	    session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION , "Closing data connection."));

	    session.getDataConnection().closeDataConnection();


	    return FtpletResult.SKIP;
	}


	public FtpletResult handleCwd(FtpSession session, FtpRequest request) throws Exception {
	    session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "HELLO THERE"));
	    return FtpletResult.SKIP;
	}


	public	    void destroy() {
	    //	    System.err.println ("destroy");
	    super.destroy();
	}


	public	    FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
	    //	    System.err.println ("onconnect");
	    return super.onConnect(session);
	}
      
	public	    FtpletResult onDisconnect(FtpSession session)  throws FtpException, IOException {
	    //	    System.err.println ("ondisconnect");
	    return super.onDisconnect(session);
	}


	public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
	    System.err.println("onLogin:" + session.getUser());
	    return FtpletResult.DEFAULT;
	}

    }



