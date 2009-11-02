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

package ucar.unidata.repository;


import ucar.unidata.repository.output.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.type.*;
import ucar.unidata.sql.Clause;


import java.text.SimpleDateFormat;

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

import org.w3c.dom.*;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class OaiManager extends RepositoryManager {

    public static final String ARG_VERB = "verb";
    public static final String ARG_IDENTIFIER = "identifier";
    public static final String ARG_RESUMPTIONTOKEN = "resumptionToken";
    public static final String ARG_FROM = "from";
    public static final String ARG_UNTIL = "until";
    public static final String ARG_SET = "set";
    public static final String ARG_METADATAPREFIX = "metadataPrefix";


    public static final String VERB_IDENTIFY = "Identify";
    public static final String VERB_LISTMETADATAFORMATS = "ListMetadataFormats";
    public static final String VERB_LISTSETS = "ListSets";
    public static final String VERB_LISTIDENTIFIERS = "ListIdentifiers";
    public static final String VERB_LISTRECORDS = "ListRecords";
    public static final String VERB_GETRECORD = "GetRecord";

    public static final String ERROR_BADARGUMENT = "badArgument";
    public static final String ERROR_BADRESUMPTIONTOKEN = "badResumptionToken";
    public static final String ERROR_BADVERB = "badVerb";
    public static final String ERROR_CANNOTDISSEMINATEFORMAT = "cannotDisseminateFormat";
    public static final String ERROR_IDDOESNOTEXIST = "idDoesNotExist";
    public static final String ERROR_NORECORDSMATCH = "noRecordsMatch";
    public static final String ERROR_NOMETADATAFORMATS = "noMetaDataFormats";
    public static final String ERROR_NOSETHIERARCHY = "noSetHierarchy";





    public static final String TAG_OAI_PMH = "OAI-PMH";
    public static final String TAG_RESPONSEDATE = "responseDate";
    public static final String TAG_REQUEST = "request";
    public static final String TAG_IDENTIFY = "Identify";
    public static final String TAG_REPOSITORYNAME = "repositoryName";
    public static final String TAG_BASEURL = "baseURL";
    public static final String TAG_PROTOCOLVERSION = "protocolVersion";
    public static final String TAG_ADMINEMAIL = "adminEmail";
    public static final String TAG_EARLIESTDATESTAMP = "earliestDatestamp";
    public static final String TAG_DELETEDRECORD = "deletedRecord";
    public static final String TAG_GRANULARITY = "granularity";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_OAI_IDENTIFIER = "oai-identifier";
    public static final String TAG_SCHEME = "scheme";
    public static final String TAG_REPOSITORYIDENTIFIER = "repositoryIdentifier";
    public static final String TAG_DELIMITER = "delimiter";
    public static final String TAG_SAMPLEIDENTIFIER = "sampleIdentifier";
    public static final String TAG_ERROR = "error";

    public static final String TAG_LISTMETADATAFORMATS = "ListMetadataFormats";
    public static final String TAG_METADATAFORMAT = "metadataFormat";
    public static final String TAG_METADATAPREFIX = "metadataPrefix";
    public static final String TAG_SCHEMA = "schema";
    public static final String TAG_METADATANAMESPACE = "metadataNamespace";


    public static final String TAG_DC_TITLE = "dc:title";
    public static final String TAG_DC_CREATOR = "dc:creator";
    public static final String TAG_DC_PUBLISHER = "dc:publisher";
    public static final String TAG_DC_SUBJECT = "dc:subject";
    public static final String TAG_DC_DESCRIPTION = "dc:description";
    public static final String TAG_DC_CONTRIBUTOR = "dc:contributor";
    public static final String TAG_DC_TYPE = "dc:type";
    public static final String TAG_DC_IDENTIFIER = "dc:identifier";
    public static final String TAG_DC_LANGUAGE = "dc:language";
    public static final String TAG_DC_RELATION = "dc:relation";


    public static final String TAG_GETRECORD = "GetRecord";
    public static final String TAG_RECORD = "record";
    public static final String TAG_HEADER = "header";
    public static final String TAG_IDENTIFIER = "identifier";
    public static final String TAG_DATESTAMP = "datestamp";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_OAI_DC = "oai_dc";
    public static final String TAG_RESUMPTIONTOKEN  = "resumptionToken";


    public static final String ATTR_CODE = "code";
    public static final String ATTR_XMLNS = "xmlns";
    public static final String ATTR_XMLNS_DC = "xmlns:dc";
    public static final String ATTR_XMLNS_XSI = "xmlns:xsi";
    public static final String ATTR_VERB = "verb";
    public static final String ATTR_IDENTIFIER = "identifier";
    public static final String ATTR_METADATAPREFIX = "metadataPrefix";



    public static final String ATTR_XSI_SCHEMALOCATION = "xsi:schemaLocation";






    private SimpleDateFormat sdf;

    private String repositoryIdentifier;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public OaiManager(Repository repository) {
        super(repository);
	sdf =   RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HHmmssZ");
    }


    private Element getRoot(Request request) throws Exception {
	Document doc = XmlUtil.makeDocument();
	Element root = XmlUtil.create(doc, TAG_OAI_PMH,null );
	XmlUtil.setAttributes(root,
			      new String[]{ATTR_XMLNS,"http://www.openarchives.org/OAI/2.0/",
					   ATTR_XMLNS_XSI,"http://www.w3.org/2001/XMLSchema-instance",
					   ATTR_XSI_SCHEMALOCATION,"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd"});

	XmlUtil.create(TAG_RESPONSEDATE, root, sdf.format(new Date()));
	String url  = getRepository().absoluteUrl(request.getUrl());
	XmlUtil.create(TAG_REQUEST, root, url, new String[]{ATTR_VERB, request.getString(ARG_VERB, "")});
	return root;
    }


    public Result processRequest(Request request) throws Exception {
	Element root = getRoot(request);
	try {
	    if(!request.exists(ARG_VERB)) {
		handleError(request, root, ERROR_BADARGUMENT, "'" + ARG_VERB+"' is missing");
	    } else {
		String verb = request.getString(ARG_VERB,VERB_IDENTIFY);
		if(verb.equals(VERB_IDENTIFY)) {
		    handleIdentify(request, root);
		} else if(verb.equals(VERB_LISTMETADATAFORMATS)) {
		    handleListMetadataformats(request, root);
		} else if(verb.equals(VERB_LISTSETS)) {
		    handleListSets(request, root);
		} else if(verb.equals(VERB_LISTIDENTIFIERS)) {
		    handleListIdentifiers(request, root);
		} else if(verb.equals(VERB_LISTRECORDS)) {
		    handleListRecords(request, root);
		} else if(verb.equals(VERB_GETRECORD))  {
		    handleGetRecord(request, root);
		} else {
		    handleError(request, root,  ERROR_BADVERB);
		}
	    }
	} catch(Exception exc) {
	    handleError(request, root, "", exc.toString());
	}
	return makeResult(request, root);
    }



    private  Result makeResult(Request request, Element root) throws Exception {
	return new Result("", new StringBuffer(XmlUtil.toString(root, false)), "text/xml");
    }

    private void handleError(Request request, Element root, String code) throws Exception {
	handleError(request, root, code,null);
    }

    private void handleError(Request request, Element root, String code, String contents) throws Exception {
	if(contents!=null)
	    XmlUtil.create(TAG_ERROR, root,  contents, new String[]{ATTR_CODE, code});
	else
	    XmlUtil.create(TAG_ERROR, root,  new String[]{ATTR_CODE, ERROR_BADVERB});
    }


    private void handleIdentify(Request request, Element root) throws Exception {
	Element id = XmlUtil.create(TAG_IDENTIFY, root);
	XmlUtil.create(TAG_REPOSITORYNAME, id, getRepository().getProperty(PROP_REPOSITORY_NAME, "Repository"));
	String url  = getRepository().absoluteUrl(request.getUrl());
	XmlUtil.create(TAG_BASEURL, id,url);
	XmlUtil.create(TAG_PROTOCOLVERSION, id,"2.0");
	XmlUtil.create(TAG_ADMINEMAIL, id,getRepository().getProperty(PROP_ADMIN_EMAIL, ""));
	XmlUtil.create(TAG_DELETEDRECORD, id,"no");
	XmlUtil.create(TAG_GRANULARITY, id,"YYYY-MM-DDThh:mm:ssZ");
	Element desc = XmlUtil.create(TAG_DESCRIPTION, id);
	Element oai = XmlUtil.create(TAG_OAI_IDENTIFIER, desc, new String[]{
		ATTR_XSI_SCHEMALOCATION,"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd"});

	XmlUtil.create(TAG_SCHEME, oai,"oai");
	XmlUtil.create(TAG_REPOSITORYIDENTIFIER, oai,getRepositoryIdentifier());
	XmlUtil.create(TAG_DELIMITER, oai,":");
	XmlUtil.create(TAG_SAMPLEIDENTIFIER, oai,makeId(getEntryManager().getTopGroup().getId()));
    }

    private void handleListMetadataformats(Request request, Element root) throws Exception {
	Element node = XmlUtil.create(TAG_LISTMETADATAFORMATS, root);
	Element fmt = XmlUtil.create(TAG_METADATAFORMAT, node);
	XmlUtil.create(TAG_METADATAPREFIX, fmt,"oai_dc");
	XmlUtil.create(TAG_SCHEMA, fmt,"http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
	XmlUtil.create(TAG_METADATANAMESPACE, fmt,"http://www.openarchives.org/OAI/2.0/oai_dc/");
    }

    private String getId(String id) {
	id = id.replace("oai:" + getRepositoryIdentifier()+":", "");
	return id;
    }



    private String getRepositoryIdentifier() {
	if(repositoryIdentifier==null) {
	    repositoryIdentifier = StringUtil.join(".",Misc.reverseList(StringUtil.split(getRepository().getHostname(),".",true,true)));
	}
	return repositoryIdentifier;
    }

    private String makeId(String id) {
	return "oai:" + getRepositoryIdentifier()+":" + id;
    }


    private static class EntryList {
	String resumptionToken;
	List<Entry> entries = new ArrayList<Entry>();	
	public EntryList(List<Entry> entries, String token) {
	    this.entries = entries;
	    this.resumptionToken = token;
	}
    }



    private void makeHeader(Entry entry, Element node) throws Exception {
	Element header = XmlUtil.create(TAG_HEADER, node);
	XmlUtil.create(TAG_IDENTIFIER, header,makeId(entry.getId()));
	XmlUtil.create(TAG_DATESTAMP, header, sdf.format(new Date(entry.getStartDate())));
    }



    private EntryList getEntries(Request request) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();

        int max = request.get(ARG_MAX, DB_MAX_ROWS);
	int skip = request.get(ARG_RESUMPTIONTOKEN,0);

	
	Request newRequest = new Request(getRepository(), request.getUser());
	newRequest.put(ARG_SKIP, ""+skip);
	if(request.exists(ARG_FROM)) {
	    newRequest.put(ARG_FROMDATE, request.getString(ARG_FROM,""));
	}
	if(request.exists(ARG_UNTIL)) {
	    newRequest.put(ARG_TODATE, request.getString(ARG_UNTIL,""));
	}
	
	List[]tuple = getEntryManager().getEntries(newRequest, new StringBuffer());
	entries.addAll((List<Entry>)tuple[0]);
	entries.addAll((List<Entry>)tuple[1]);

	String token = null;
	if(entries.size()>0) {
	    if(entries.size()>=max) {
		token = ""+(skip+max);
	    }
	}

	return new EntryList(entries,token);
    }


    private void handleListSets(Request request, Element root) throws Exception {
	handleError(request, root,  ERROR_NOSETHIERARCHY);
    }

    private void addResumption(Request request, Element root, EntryList entries) throws Exception {
	if(entries.resumptionToken!=null)
	    XmlUtil.create(TAG_RESUMPTIONTOKEN,root, entries.resumptionToken);

    } 


    private void handleListIdentifiers(Request request, Element root) throws Exception {
	EntryList entryList = getEntries(request);
	for(Entry entry:entryList.entries) {
	    makeHeader(entry, root);
	}
	addResumption(request, root, entryList);
    }

    private void handleListRecords(Request request, Element root) throws Exception {
	if(!request.exists(ARG_METADATAPREFIX)) {
	    handleError(request, root, ERROR_BADARGUMENT, "'" + ARG_METADATAPREFIX+"' is missing");
	    return;
	}

	EntryList entryList = getEntries(request);
	for(Entry entry:entryList.entries) {
	    makeRecord(request,entry, root);
	}
	addResumption(request, root, entryList);
    }


    private void addMetadata(Request request, Entry entry, Element node) throws Exception {
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if (metadataHandler.canHandle(metadata)) {
                    metadataHandler.addMetadataToXml(request,
						     MetadataTypeBase.TEMPLATETYPE_OAIDC, entry,
						     metadata, node.getOwnerDocument(),node);
                    break;
                }
            }
        }
    }



    private void handleGetRecord(Request request, Element root) throws Exception {
	if(!request.exists(ARG_IDENTIFIER)) {
	    handleError(request, root, ERROR_BADARGUMENT, "'identifier' is missing");
	    return;
	}
	if(!request.exists(ARG_METADATAPREFIX)) {
	    handleError(request, root, ERROR_BADARGUMENT, "'" + ARG_METADATAPREFIX+"' is missing");
	    return;
	}


	String id = getId(request.getString(ARG_IDENTIFIER,""));
	Entry entry = getEntryManager().getEntry(request, id);
	if(entry==null) {
	    handleError(request, root, ERROR_IDDOESNOTEXIST);
	    return;
	}

	makeRecord(request,entry, root);
    }

    private void makeRecord(Request request,Entry entry, Element root) throws Exception {
	Element node = XmlUtil.create(TAG_GETRECORD, root);
	Element record = XmlUtil.create(TAG_RECORD, node);
	makeHeader(entry, record);

	Element metadata = XmlUtil.create(TAG_METADATA, record);
	Element oaidc = XmlUtil.create(TAG_OAI_DC, metadata, new String[]{
		ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/oai_dc/",
		ATTR_XMLNS_DC,"http://purl.org/dc/elements/1.1/",
		ATTR_XMLNS_XSI,"http://www.w3.org/2001/XMLSchema-instance",
		ATTR_XSI_SCHEMALOCATION,"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd"});
	
	XmlUtil.create(TAG_DC_IDENTIFIER, oaidc,entry.getId());
	XmlUtil.create(TAG_DC_TITLE, oaidc,entry.getName());
	XmlUtil.create(TAG_DC_DESCRIPTION, oaidc,entry.getDescription());
	addMetadata(request, entry, oaidc);

    }


}

