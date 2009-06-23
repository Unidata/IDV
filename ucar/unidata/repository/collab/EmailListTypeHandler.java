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

package ucar.unidata.repository.collab;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.output.OutputHandler;


import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class EmailListTypeHandler extends GenericTypeHandler {

    public static String ARG_EMAIL_SHOWFORM = "email.showform";
    public static String ARG_EMAIL_FROMADDRESS = "email.fromadress";   
    public static String ARG_EMAIL_FROMNAME = "email.fromname";
    public static String ARG_EMAIL_SUBJECT = "email.subject";
    public static String ARG_EMAIL_MESSAGE = "email.message";
    public static String ARG_EMAIL_BCC = "email.bcc";



    /** _more_ */
    public static String TYPE_EMAILLIST = "emaillist";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public EmailListTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    public Result showForm(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtil.submit(msg("Send Message")));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.hidden(ARG_ENTRYID,entry.getId()));
        sb.append(HtmlUtil.formEntry(msgLabel("From name"),
                                     HtmlUtil.input(ARG_EMAIL_FROMNAME,request.getUser().getName(),HtmlUtil.SIZE_40)));
        sb.append(HtmlUtil.formEntry(msgLabel("From email"),
                                     HtmlUtil.input(ARG_EMAIL_FROMADDRESS,request.getUser().getEmail(),HtmlUtil.SIZE_40)));
        String bcc = HtmlUtil.checkbox(ARG_EMAIL_BCC,"true",false) +HtmlUtil.space(1) +msg("Send as BCC");


        sb.append(HtmlUtil.formEntry(msgLabel("Subject"),
                                     HtmlUtil.input(ARG_EMAIL_SUBJECT,"",HtmlUtil.SIZE_40)+HtmlUtil.space(2)+bcc));
        sb.append(HtmlUtil.formEntryTop(msgLabel("Message"),
                                     HtmlUtil.textArea(ARG_EMAIL_MESSAGE,"",30,60)));
        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.submit(msg("Send Message")));
        sb.append(HtmlUtil.formClose());

        return new Result("Mailing list",sb);
    }


    public Result sendMail(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        List<Address> to = getAddresses(entry);
        getAdmin().sendEmail(to, 
                    new InternetAddress(request.getString(ARG_EMAIL_FROMADDRESS,""),
                                        request.getString(ARG_EMAIL_FROMNAME,"")),
                    request.getString(ARG_EMAIL_SUBJECT,""),
                    request.getString(ARG_EMAIL_MESSAGE,""),
                    request.get(ARG_EMAIL_BCC,false),
                    false);
        sb.append(getRepository().showDialogNote(msg("Message Sent")));
        return showList(request, entry, sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        if(request.get(ARG_EMAIL_SHOWFORM,false)) {
            return showForm(request, entry);
        }

        if(request.exists(ARG_EMAIL_FROMNAME)) {
            return sendMail(request, entry);
        }

        return showList(request, entry, sb);
    }

    public Result showList(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,entry,ARG_EMAIL_SHOWFORM,"true"),msg("Send message")));
        sb.append(HtmlUtil.formTable());
        for(String[] tuple: getTuples(entry)) {
            String email = HtmlUtil.href("mailto:" + tuple[0],tuple[0]);
            if(tuple.length==1) {
                sb.append(HtmlUtil.row(HtmlUtil.cols(email)));
            } else {
                sb.append(HtmlUtil.row(HtmlUtil.cols(email,tuple[1])));
            }
        }
        sb.append(HtmlUtil.formTableClose());
        return new Result("Mailing list",sb);
    }

    private List<String[]>  getTuples(Entry entry) {
        List<String[]> addresses = new ArrayList<String[]>();
        String list = "";
        Object[] values = entry.getValues();
        if ((values != null) && (values.length > 0)
            && (values[0] != null)) {
            list = (String)values[0];
        }
        for(String line: StringUtil.split(list,"\n",true,true)) {
            String[]tuple = StringUtil.split(line,",",2);
            if(tuple==null) {
                addresses.add(new String[]{line});
            } else {
                addresses.add(tuple);
            }
        }
        return addresses;
    }

    private List<Address> getAddresses(Entry entry) throws Exception {
        List<Address> addresses = new ArrayList<Address>();
        for(String[] tuple: getTuples(entry)) {
            Address address;
            if(tuple.length==1) {
                address  = new InternetAddress(tuple[0]);
            } else {
                address  = new InternetAddress(tuple[0],tuple[1]);
            }
            addresses.add(address);
        }
        return addresses;
    }




}

