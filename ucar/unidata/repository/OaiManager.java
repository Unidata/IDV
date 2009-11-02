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


import org.w3c.dom.*;

import ucar.unidata.repository.metadata.*;


import ucar.unidata.repository.output.*;
import ucar.unidata.repository.type.*;
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


import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class OaiManager extends RepositoryManager {

    /** _more_          */
    public static final String ARG_VERB = "verb";

    /** _more_          */
    public static final String ARG_IDENTIFIER = "identifier";

    /** _more_          */
    public static final String ARG_RESUMPTIONTOKEN = "resumptionToken";

    /** _more_          */
    public static final String ARG_FROM = "from";

    /** _more_          */
    public static final String ARG_UNTIL = "until";

    /** _more_          */
    public static final String ARG_SET = "set";

    /** _more_          */
    public static final String ARG_METADATAPREFIX = "metadataPrefix";

    /** _more_          */
    private static final String[] ALLARGS = {
        ARG_VERB, ARG_IDENTIFIER, ARG_RESUMPTIONTOKEN, ARG_FROM, ARG_UNTIL,
        ARG_SET, ARG_METADATAPREFIX
    };

    /** _more_          */
    private static final String[] formats = { "yyyy-MM-dd'T'HH:mm:ss Z",
            "yyyyMMdd'T'HHmmss Z" };

    /** _more_          */
    private SimpleDateFormat[] parsers;



    /** _more_          */
    private static HashSet argSet;

    /** _more_          */
    private static HashSet verbSet;

    /** _more_          */
    public static final String VERB_IDENTIFY = "Identify";

    /** _more_          */
    public static final String VERB_LISTMETADATAFORMATS =
        "ListMetadataFormats";

    /** _more_          */
    public static final String VERB_LISTSETS = "ListSets";

    /** _more_          */
    public static final String VERB_LISTIDENTIFIERS = "ListIdentifiers";

    /** _more_          */
    public static final String VERB_LISTRECORDS = "ListRecords";

    /** _more_          */
    public static final String VERB_GETRECORD = "GetRecord";

    /** _more_          */
    private static final String[] ALLVERBS = {
        VERB_IDENTIFY, VERB_LISTMETADATAFORMATS, VERB_LISTSETS,
        VERB_LISTIDENTIFIERS, VERB_LISTRECORDS, VERB_GETRECORD
    };


    /** _more_          */
    public static final String ERROR_BADARGUMENT = "badArgument";

    /** _more_          */
    public static final String ERROR_BADRESUMPTIONTOKEN =
        "badResumptionToken";

    /** _more_          */
    public static final String ERROR_BADVERB = "badVerb";

    /** _more_          */
    public static final String ERROR_CANNOTDISSEMINATEFORMAT =
        "cannotDisseminateFormat";

    /** _more_          */
    public static final String ERROR_IDDOESNOTEXIST = "idDoesNotExist";

    /** _more_          */
    public static final String ERROR_NORECORDSMATCH = "noRecordsMatch";

    /** _more_          */
    public static final String ERROR_NOMETADATAFORMATS = "noMetaDataFormats";

    /** _more_          */
    public static final String ERROR_NOSETHIERARCHY = "noSetHierarchy";





    /** _more_          */
    public static final String TAG_OAI_PMH = "OAI-PMH";

    /** _more_          */
    public static final String TAG_RESPONSEDATE = "responseDate";

    /** _more_          */
    public static final String TAG_REQUEST = "request";

    /** _more_          */
    public static final String TAG_IDENTIFY = "Identify";

    /** _more_          */
    public static final String TAG_REPOSITORYNAME = "repositoryName";

    /** _more_          */
    public static final String TAG_BASEURL = "baseURL";

    /** _more_          */
    public static final String TAG_PROTOCOLVERSION = "protocolVersion";

    /** _more_          */
    public static final String TAG_ADMINEMAIL = "adminEmail";

    /** _more_          */
    public static final String TAG_EARLIESTDATESTAMP = "earliestDatestamp";

    /** _more_          */
    public static final String TAG_DELETEDRECORD = "deletedRecord";

    /** _more_          */
    public static final String TAG_GRANULARITY = "granularity";

    /** _more_          */
    public static final String TAG_DESCRIPTION = "description";

    /** _more_          */
    public static final String TAG_OAI_IDENTIFIER = "oai-identifier";

    /** _more_          */
    public static final String TAG_SCHEME = "scheme";

    /** _more_          */
    public static final String TAG_REPOSITORYIDENTIFIER =
        "repositoryIdentifier";

    /** _more_          */
    public static final String TAG_DELIMITER = "delimiter";

    /** _more_          */
    public static final String TAG_SAMPLEIDENTIFIER = "sampleIdentifier";

    /** _more_          */
    public static final String TAG_ERROR = "error";

    /** _more_          */
    public static final String TAG_LISTMETADATAFORMATS =
        "ListMetadataFormats";

    /** _more_          */
    public static final String TAG_METADATAFORMAT = "metadataFormat";

    /** _more_          */
    public static final String TAG_METADATAPREFIX = "metadataPrefix";

    /** _more_          */
    public static final String TAG_SCHEMA = "schema";

    /** _more_          */
    public static final String TAG_METADATANAMESPACE = "metadataNamespace";

    /** _more_          */
    public static final String TAG_LISTIDENTIFIERS = "ListIdentifiers";


    /** _more_          */
    public static final String TAG_DC_TITLE = "dc:title";

    /** _more_          */
    public static final String TAG_DC_CREATOR = "dc:creator";

    /** _more_          */
    public static final String TAG_DC_PUBLISHER = "dc:publisher";

    /** _more_          */
    public static final String TAG_DC_SUBJECT = "dc:subject";

    /** _more_          */
    public static final String TAG_DC_DESCRIPTION = "dc:description";

    /** _more_          */
    public static final String TAG_DC_CONTRIBUTOR = "dc:contributor";

    /** _more_          */
    public static final String TAG_DC_TYPE = "dc:type";

    /** _more_          */
    public static final String TAG_DC_IDENTIFIER = "dc:identifier";

    /** _more_          */
    public static final String TAG_DC_LANGUAGE = "dc:language";

    /** _more_          */
    public static final String TAG_DC_RELATION = "dc:relation";


    /** _more_          */
    public static final String TAG_GETRECORD = "GetRecord";

    /** _more_          */
    public static final String TAG_RECORD = "record";

    /** _more_          */
    public static final String TAG_HEADER = "header";

    /** _more_          */
    public static final String TAG_IDENTIFIER = "identifier";

    /** _more_          */
    public static final String TAG_DATESTAMP = "datestamp";

    /** _more_          */
    public static final String TAG_METADATA = "metadata";

    /** _more_          */
    public static final String TAG_OAI_DC = "oai_dc";

    /** _more_          */
    public static final String TAG_RESUMPTIONTOKEN = "resumptionToken";


    /** _more_          */
    public static final String ATTR_CODE = "code";

    /** _more_          */
    public static final String ATTR_XMLNS = "xmlns";

    /** _more_          */
    public static final String ATTR_XMLNS_DC = "xmlns:dc";

    /** _more_          */
    public static final String ATTR_XMLNS_XSI = "xmlns:xsi";

    /** _more_          */
    public static final String ATTR_VERB = "verb";

    /** _more_          */
    public static final String ATTR_IDENTIFIER = "identifier";

    /** _more_          */
    public static final String ATTR_METADATAPREFIX = "metadataPrefix";


    /** _more_          */
    public static final String ATTR_XSI_SCHEMALOCATION = "xsi:schemaLocation";

    /** _more_          */
    public static final String SCHEMA =
        "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";




    /** _more_          */
    private SimpleDateFormat sdf;

    /** _more_          */
    private String repositoryIdentifier;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public OaiManager(Repository repository) {
        super(repository);
        sdf    = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        argSet = new HashSet();
        for (String arg : ALLARGS) {
            argSet.add(arg);
        }
        verbSet = new HashSet();
        for (String verb : ALLVERBS) {
            verbSet.add(verb);
        }

        SimpleDateFormat[] parsers = new SimpleDateFormat[formats.length];
        for (int i = 0; i < formats.length; i++) {
            parsers[i] = RepositoryUtil.makeDateFormat(formats[i]);
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
    private Element getRoot(Request request) throws Exception {
        Document doc  = XmlUtil.makeDocument();
        Element  root = XmlUtil.create(doc, TAG_OAI_PMH, null);
        XmlUtil.setAttributes(root, new String[] {
            ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/",
            ATTR_XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd"
        });

        XmlUtil.create(TAG_RESPONSEDATE, root, format(new Date()));
        return root;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void addRequest(Request request, Element root) throws Exception {
        String  url = getRepository().absoluteUrl(request.getRequestPath());
        Element requestNode = XmlUtil.create(TAG_REQUEST, root, url);

        if ( !Misc.equals(request.getString(ARG_METADATAPREFIX, "oai_dc"),
                          "oai_dc")) {
            throw new MyException(ERROR_CANNOTDISSEMINATEFORMAT,
                                  "bad metadataPrefix argument");
        }

        if (request.exists(ARG_RESUMPTIONTOKEN)) {
            try {
                new Integer(request.getString(ARG_RESUMPTIONTOKEN,
                        "")).intValue();
            } catch (Exception exc) {
                throw new MyException(ERROR_BADRESUMPTIONTOKEN,
                                      "bad resumption token");
            }
        }

        Date fromDate  = null;
        Date untilDate = null;

        if (request.exists(ARG_FROM)) {
            fromDate = parseUTC(request.getString(ARG_FROM, ""));
            if (fromDate == null) {
                throw new IllegalArgumentException(
                    "could not parse from date");
            }
        }

        if (request.exists(ARG_UNTIL)) {
            untilDate = parseUTC(request.getString(ARG_UNTIL, ""));
            if (untilDate == null) {
                throw new IllegalArgumentException(
                    "could not parse until date");
            }
        }


        if ((fromDate != null) && (untilDate != null)) {
            if (fromDate.getTime() > untilDate.getTime()) {
                throw new IllegalArgumentException("from date > until date");
            }

        }

        if (request.exists(ARG_FROM) && request.exists(ARG_UNTIL)) {
            if (request.getString(ARG_FROM).length()
                    != request.getString(ARG_UNTIL).length()) {
                throw new IllegalArgumentException(
                    "different granularity of from and until arguments");
            }
        }

        for (Enumeration keys = request.getArgs().keys();
                keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if (argSet.contains(key)) {
                String value = request.getString(key, "");
                requestNode.setAttribute(key, value);
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
    public Result processRequest(Request request) throws Exception {
        Element root = getRoot(request);
        try {
            processRequestInner(request, root);
        } catch (MyException myexc) {
            handleError(request, root, myexc.code, myexc.toString());
        } catch (Exception exc) {
            handleError(request, root, ERROR_BADARGUMENT, exc.toString());
        }
        return makeResult(request, root);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void processRequestInner(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_VERB)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_VERB + "' is missing");
            return;
        }
        String verb = request.getString(ARG_VERB, VERB_IDENTIFY);
        if ( !verbSet.contains(verb)) {
            handleError(request, root, ERROR_BADVERB);
            return;
        }

        addRequest(request, root);


        for (Enumeration keys = request.getArgs().keys();
                keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if ( !argSet.contains(key)) {
                handleError(request, root, ERROR_BADARGUMENT,
                            "Bad argument:" + key);
                return;
            }
            if (request.hasMultiples(key)) {
                handleError(request, root, ERROR_BADARGUMENT,
                            "Multiple arguments:" + key);
                return;
            }
        }


        if (verb.equals(VERB_IDENTIFY)) {
            handleIdentify(request, root);
        } else if (verb.equals(VERB_LISTMETADATAFORMATS)) {
            handleListMetadataformats(request, root);
        } else if (verb.equals(VERB_LISTSETS)) {
            handleListSets(request, root);
        } else if (verb.equals(VERB_LISTIDENTIFIERS)) {
            handleListIdentifiers(request, root);
        } else if (verb.equals(VERB_LISTRECORDS)) {
            handleListRecords(request, root);
        } else if (verb.equals(VERB_GETRECORD)) {
            handleGetRecord(request, root);
        } else {
            handleError(request, root, ERROR_BADVERB);
        }

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeResult(Request request, Element root)
            throws Exception {
        return new Result("", new StringBuffer(XmlUtil.toString(root, true)),
                          "text/xml");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     * @param code _more_
     *
     * @throws Exception _more_
     */
    private void handleError(Request request, Element root, String code)
            throws Exception {
        handleError(request, root, code, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     * @param code _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    private void handleError(Request request, Element root, String code,
                             String contents)
            throws Exception {
        if (contents != null) {
            XmlUtil.create(TAG_ERROR, root, contents,
                           new String[] { ATTR_CODE,
                                          code });
        } else {
            XmlUtil.create(TAG_ERROR, root, new String[] { ATTR_CODE, code });
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleIdentify(Request request, Element root)
            throws Exception {
        Element id = XmlUtil.create(TAG_IDENTIFY, root);
        XmlUtil.create(TAG_REPOSITORYNAME, id,
                       getRepository().getProperty(PROP_REPOSITORY_NAME,
                           "Repository"));
        String url = getRepository().absoluteUrl(request.getUrl());
        XmlUtil.create(TAG_BASEURL, id, url);
        XmlUtil.create(TAG_PROTOCOLVERSION, id, "2.0");
        XmlUtil.create(TAG_ADMINEMAIL, id,
                       getRepository().getProperty(PROP_ADMIN_EMAIL, ""));
        XmlUtil.create(TAG_EARLIESTDATESTAMP, id, "1990-01-01T00:00:00Z");
        XmlUtil.create(TAG_DELETEDRECORD, id, "no");
        XmlUtil.create(TAG_GRANULARITY, id, "YYYY-MM-DDThh:mm:ssZ");
        Element desc = XmlUtil.create(TAG_DESCRIPTION, id);
        Element oai  = XmlUtil.create(TAG_OAI_IDENTIFIER, desc, new String[] {
            ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/oai-identifier",
            ATTR_XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/oai-identifier  http://www.openarchives.org/OAI/2.0/oai-identifier.xsd"
        });

        XmlUtil.create(TAG_SCHEME, oai, "oai");
        XmlUtil.create(TAG_REPOSITORYIDENTIFIER, oai,
                       getRepositoryIdentifier());
        XmlUtil.create(TAG_DELIMITER, oai, ":");
        XmlUtil.create(TAG_SAMPLEIDENTIFIER, oai,
                       makeId(getEntryManager().getTopGroup().getId()));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleListMetadataformats(Request request, Element root)
            throws Exception {
        if (request.exists(ARG_IDENTIFIER)) {
            String id    = getId(request.getString(ARG_IDENTIFIER, ""));
            Entry  entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                handleError(request, root, ERROR_IDDOESNOTEXIST);
                return;
            }
        }



        Element node = XmlUtil.create(TAG_LISTMETADATAFORMATS, root);
        Element fmt  = XmlUtil.create(TAG_METADATAFORMAT, node);
        XmlUtil.create(TAG_METADATAPREFIX, fmt, "oai_dc");
        XmlUtil.create(TAG_SCHEMA, fmt,
                       "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        XmlUtil.create(TAG_METADATANAMESPACE, fmt,
                       "http://www.openarchives.org/OAI/2.0/oai_dc/");
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String getId(String id) {
        id = id.replace("oai:" + getRepositoryIdentifier() + ":", "");
        return id;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private String getRepositoryIdentifier() {
        if (repositoryIdentifier == null) {
            repositoryIdentifier = StringUtil.join(
                ".",
                Misc.reverseList(
                    StringUtil.split(
                        getRepository().getHostname(), ".", true, true)));
        }
        return repositoryIdentifier;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String makeId(String id) {
        return "oai:" + getRepositoryIdentifier() + ":" + id;
    }


    /**
     * Class EntryList _more_
     *
     *
     * @author IDV Development Team
     */
    private static class EntryList {

        /** _more_          */
        String resumptionToken;

        /** _more_          */
        List<Entry> entries = new ArrayList<Entry>();

        /**
         * _more_
         *
         * @param entries _more_
         * @param token _more_
         */
        public EntryList(List<Entry> entries, String token) {
            this.entries         = entries;
            this.resumptionToken = token;
        }
    }



    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    private String format(Date d) {
        return sdf.format(d) + "Z";
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    private void makeHeader(Entry entry, Element node) throws Exception {
        Element header = XmlUtil.create(TAG_HEADER, node);
        XmlUtil.create(TAG_IDENTIFIER, header, makeId(entry.getId()));
        XmlUtil.create(TAG_DATESTAMP, header,
                       format(new Date(entry.getStartDate())));
    }


    /**
     * Class MyException _more_
     *
     *
     * @author IDV Development Team
     */
    public static class MyException extends IllegalArgumentException {

        /** _more_          */
        String code;

        /**
         * _more_
         *
         * @param code _more_
         * @param msg _more_
         */
        public MyException(String code, String msg) {
            super(msg);
            this.code = code;
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private Date parseUTC(String s) {
        for (SimpleDateFormat parser : parsers) {
            try {
                return parser.parse(s);
            } catch (Exception exc) {}
        }
        return null;
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
    private EntryList getEntries(Request request) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        int         max     = request.get(ARG_MAX, 5);
        int         skip    = request.get(ARG_RESUMPTIONTOKEN, 0);
        Request newRequest  = new Request(getRepository(), request.getUser());
        newRequest.put(ARG_SKIP, "" + skip);
        newRequest.put(ARG_MAX, "" + max);

        if (request.exists(ARG_FROM)) {
            newRequest.put(ARG_FROMDATE, request.getString(ARG_FROM, ""));
        }

        if (request.exists(ARG_UNTIL)) {
            newRequest.put(ARG_UNTIL, request.getString(ARG_UNTIL, ""));
        }


        List[] tuple = getEntryManager().getEntries(newRequest,
                           new StringBuffer());
        entries.addAll((List<Entry>) tuple[0]);
        entries.addAll((List<Entry>) tuple[1]);

        String token = null;
        if (entries.size() > 0) {
            if (entries.size() >= max) {
                token = "" + (skip + max);
            }
        }

        return new EntryList(entries, token);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleListSets(Request request, Element root)
            throws Exception {
        handleError(request, root, ERROR_NOSETHIERARCHY);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void addResumption(Request request, Element root,
                               EntryList entries)
            throws Exception {
        if (entries.resumptionToken != null) {
            XmlUtil.create(TAG_RESUMPTIONTOKEN, root,
                           entries.resumptionToken);
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleListIdentifiers(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");
            return;
        }


        EntryList entryList = getEntries(request);
        if (entryList.entries.size() == 0) {
            handleError(request, root, ERROR_NORECORDSMATCH,
                        "No records match");
            return;
        }

        Element listNode = XmlUtil.create(TAG_LISTIDENTIFIERS, root);
        for (Entry entry : entryList.entries) {
            makeHeader(entry, listNode);
        }
        addResumption(request, listNode, entryList);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleListRecords(Request request, Element root)
            throws Exception {
        EntryList entryList = getEntries(request);

        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");
            return;
        }

        if ( !request.exists(ARG_METADATAPREFIX)) {
            throw new IllegalArgumentException(
                "no metadataPrefix argument defined");
        }


        if (entryList.entries.size() == 0) {
            handleError(request, root, ERROR_NORECORDSMATCH,
                        "No records match");
            return;
        }
        for (Entry entry : entryList.entries) {
            makeRecord(request, entry, root);
        }
        addResumption(request, root, entryList);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Request request, Entry entry, Element node)
            throws Exception {
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if (metadataHandler.canHandle(metadata)) {
                    metadataHandler.addMetadataToXml(request,
                            MetadataTypeBase.TEMPLATETYPE_OAIDC, entry,
                            metadata, node.getOwnerDocument(), node);
                    break;
                }
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void handleGetRecord(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_IDENTIFIER)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'identifier' is missing");
            return;
        }
        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");
            return;
        }


        String id    = getId(request.getString(ARG_IDENTIFIER, ""));
        Entry  entry = getEntryManager().getEntry(request, id);
        if (entry == null) {
            handleError(request, root, ERROR_IDDOESNOTEXIST);
            return;
        }

        makeRecord(request, entry, root);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    private void makeRecord(Request request, Entry entry, Element root)
            throws Exception {
        Element node   = XmlUtil.create(TAG_GETRECORD, root);
        Element record = XmlUtil.create(TAG_RECORD, node);
        makeHeader(entry, record);

        Element metadata = XmlUtil.create(TAG_METADATA, record);
        Element oaidc    = XmlUtil.create(TAG_OAI_DC, metadata, new String[] {
            ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/oai_dc/",
            ATTR_XMLNS_DC, "http://purl.org/dc/elements/1.1/", ATTR_XMLNS_XSI,
            "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"
        });

        //      XmlUtil.create(TAG_DC_IDENTIFIER, oaidc,entry.getId());
        XmlUtil.create(TAG_DC_TITLE, oaidc, entry.getName());
        //      XmlUtil.create(TAG_DC_DESCRIPTION, oaidc,entry.getDescription());
        addMetadata(request, entry, oaidc);

    }


}

