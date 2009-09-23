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


import org.w3c.dom.Document;


import org.w3c.dom.Element;

import ucar.unidata.ui.HttpFormEntry;


import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;

import java.util.List;
import java.util.zip.*;






/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryClient extends RepositoryBase {


    /** _more_ */
    private String sessionId;

    /** _more_ */
    private String user = "";

    /** _more_ */
    private String password = "";

    /** _more_ */
    private String name = "RAMADDA Client";


    /** _more_ */
    private String defaultGroupId;

    /** _more_ */
    private String defaultGroupName;

    /** _more_ */
    private String sslPort;

    /** _more_ */
    private String title;

    /** _more_ */
    private String description;

    /**
     * _more_
     */
    public RepositoryClient() {}

    /**
     * _more_
     *
     * @param serverUrl _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(URL serverUrl, String user, String password)
            throws Exception {
        setPort(serverUrl.getPort());
        setHostname(serverUrl.getHost());
        setUrlBase(serverUrl.getPath());
        this.user     = user;
        this.password = password;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHttpsPort() {
        return sslPort;
    }


    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base)
            throws Exception {
        this(hostname, port, base, "", "");
    }

    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base,
                            String user, String password)
            throws Exception {
        super(port);
        setHostname(hostname);
        this.user     = user;
        this.password = password;
        setUrlBase(base);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] doPost(RequestUrl url, List<HttpFormEntry> entries)
            throws Exception {
        return HttpFormEntry.doPost(entries, url.getFullUrl());
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println(
            "Usage: RepositoryClient <server url> <user id> <password> <arguments>");
        System.err.println(
            "Where arguments are:\nFor fetching: \n\t-fetch <entry id> <destination file or directory>\n\nFor uploading:\n\t-parent <parent group id>\n\t-file <file to upload>\n\t-name <entry name>\n\t-descr <entry description>\n\t-attach <file to attach>\n\t-addmetadata (Add full metadata to entry)\n\t-addshortmetadata (Add spatial/temporal metadata to entry)\n\t");
        System.exit(1);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    private void processCommandLine(String[] args) throws Exception {

        String     xmlFile    = null;
        List<File> files      = new ArrayList<File>();
        Element    root       = null;
        Element    entryNode  = null;
        Document   doc        = null;
        boolean    haveParent = false;
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-fetch")) {
                if (i>= args.length-2) {
                    usage("Bad -fetch argument");
                }
                File f = writeFile(args[i+1], new File(args[i+2]));
                System.err.println ("Wrote file to:" + f);
                return;
            }
            if (arg.equals("-name")) {
                if (i == args.length) {
                    usage("Bad -name argument");
                }
                i++;
                if (root == null) {
                    doc = XmlUtil.makeDocument();
                    root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
                    entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                            new String[] {});

                }
                entryNode.setAttribute(ATTR_NAME, args[i]);
            } else if (arg.equals("-description")) {
                if (i == args.length) {
                    usage("Bad -description argument");
                }
                if (root == null) {
                    usage("Must specify -name first");
                }
                i++;
                Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION,
                                       entryNode);
                descNode.appendChild(XmlUtil.makeCDataNode(doc, args[i],
                        false));
            } else if (arg.equals("-parent")) {
                if (i == args.length) {
                    usage("Bad -parent argument");
                }
                if (root == null) {
                    usage("Must specify -name first");
                }
                i++;
                entryNode.setAttribute(ATTR_PARENT, args[i]);
                haveParent = true;
            } else if (arg.equals("-localfile")) {
                if (i == args.length) {
                    usage("Bad -localfile argument");
                }
                i++;
                File f = new File(args[i]);
                //                if(!f.exists()) usage("Bad file:" + args[i]);
                if (root == null) {
                    doc = XmlUtil.makeDocument();
                    root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
                    entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                            new String[] {});
                    entryNode.setAttribute(ATTR_NAME,
                                           IOUtil.getFileTail(args[i]));
                }
                entryNode.setAttribute(ATTR_LOCALFILE, f.getPath());
            } else if (arg.equals("-localfiletomove")) {
                if (i == args.length) {
                    usage("Bad -localfiletomove argument");
                }
                i++;
                File f = new File(args[i]);
                //                if(!f.exists()) usage("Bad file:" + args[i]);
                if (root == null) {
                    doc = XmlUtil.makeDocument();
                    root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
                    entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                            new String[] {});
                    entryNode.setAttribute(ATTR_NAME,
                                           IOUtil.getFileTail(args[i]));
                }
                entryNode.setAttribute(ATTR_LOCALFILETOMOVE, f.getPath());
            } else if (arg.equals("-file")) {
                if (i == args.length) {
                    usage("Bad -file argument");
                }
                i++;
                File f = new File(args[i]);
                if ( !f.exists()) {
                    usage("File does not exist:" + args[i]);
                }
                if (root == null) {
                    doc = XmlUtil.makeDocument();
                    root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
                    entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                            new String[] {});
                    entryNode.setAttribute(ATTR_NAME,
                                           IOUtil.getFileTail(args[i]));
                }
                entryNode.setAttribute(ATTR_FILE,
                                       IOUtil.getFileTail(args[i]));
                files.add(f);
            } else if (arg.equals("-addmetadata")) {
                if (entryNode == null) {
                    usage("Need to specify a -file first");
                }
                entryNode.setAttribute(ATTR_ADDMETADATA, "true");
            } else if (arg.equals("-addshortmetadata")) {
                if (entryNode == null) {
                    usage("Need to specify a -file first");
                }
                entryNode.setAttribute(ATTR_ADDSHORTMETADATA, "true");
            } else if (arg.equals("-attach")) {
                if (i == args.length) {
                    usage("Bad -file argument");
                }
                i++;
                File f = new File(args[i]);
                if ( !f.exists()) {
                    usage("File does not exist:" + args[i]);
                }
                if (entryNode == null) {
                    usage("Need to specify a -file first");
                }
                if (root == null) {
                    doc = XmlUtil.makeDocument();
                    root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
                    entryNode.setAttribute(ATTR_NAME,
                                           IOUtil.getFileTail(args[i]));
                }

                addAttachment(entryNode, IOUtil.getFileTail(f.toString()));
                files.add(f);
            } else {
                if ( !new File(args[i]).exists()) {
                    usage("Unknown argument:" + args[i]);
                }
                files.add(new File(args[i]));
            }
        }

        if ((files.size() == 0) && (root == null)) {
            usage("");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream       zos = new ZipOutputStream(bos);

        //Write the xml if we have it
        if (root != null) {
            if ( !haveParent) {
                usage("Must specify a parent group destination with -parent");
            }
            String xml = XmlUtil.toString(root);
            zos.putNextEntry(new ZipEntry("entries.xml"));
            byte[] bytes = xml.getBytes();
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();

        }
        //Now write the files
        for (File f : files) {
            zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(f.toString())));
            byte[] bytes = IOUtil.readBytes(new FileInputStream(f));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        addUrlArgs(postEntries);
        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                                          bos.toByteArray()));
        String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);
            return;
        }

        System.err.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);

        String  body     = XmlUtil.getChildText(response).trim();
        if (responseOk(response)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("Error:" + body);
        }

    }


    /**
     * _more_
     *
     * @param host _more_
     * @param port _more_
     * @param base _more_
     * @param user _more_
     * @param passwd _more_
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     * @param filePath _more_
     *
     * @throws Exception _more_
     */
    public static void uploadFileToRamadda(String host, int port,
                                           String base, String user,
                                           String passwd, String entryName,
                                           String entryDescription,
                                           String parent, String filePath)
            throws Exception {
        uploadFileToRamadda(new URL("http://" + host + ":" + port + base),
                            user, passwd, entryName, entryDescription,
                            parent, filePath);
    }



    /**
     * _more_
     *
     * @param host _more_
     * @param user _more_
     * @param passwd _more_
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     * @param filePath _more_
     *
     * @throws Exception _more_
     */
    public static void uploadFileToRamadda(URL host, String user,
                                           String passwd, String entryName,
                                           String entryDescription,
                                           String parent, String filePath)
            throws Exception {

        RepositoryClient client = new RepositoryClient(host, user, passwd);
        String[]         msg    = { "" };
        if (client.isValidSession(true, msg)) {
            System.err.println("Valid session");
        } else {
            System.err.println("Invalid session:" + msg[0]);
            return;
        }

        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                      new String[] {});
        Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                           new String[] {});

        /*
         * name
         */
        entryNode.setAttribute(ATTR_NAME, entryName);
        /*
         * description
         */
        Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION, entryNode);
        descNode.appendChild(XmlUtil.makeCDataNode(doc, entryDescription,
                false));
        /*
         * parent
         */
        entryNode.setAttribute(ATTR_PARENT, parent);
        /*
         * file
         */
        File file = new File(filePath);
        entryNode.setAttribute(ATTR_FILE, IOUtil.getFileTail(filePath));
        /*
         * addmetadata
         */
        entryNode.setAttribute(ATTR_ADDMETADATA, "true");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream       zos = new ZipOutputStream(bos);

        /*
         * write the xml definition into the zip file
         */
        String xml = XmlUtil.toString(root);
        zos.putNextEntry(new ZipEntry("entries.xml"));
        byte[] bytes = xml.getBytes();
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        /*
         * add also the file
         */
        String file2string = file.toString();
        zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(file2string)));
        bytes = IOUtil.readBytes(new FileInputStream(file));
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        postEntries.add(HttpFormEntry.hidden(ARG_SESSIONID,
                                             client.getSessionId()));
        postEntries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                                          bos.toByteArray()));

        RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(client,
                                             "/entry/xmlcreate");
        String[] result = client.doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);
            return;
        }

        System.err.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);

        String  body     = XmlUtil.getChildText(response).trim();
        if (client.responseOk(response)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("Error:" + body);
        }

    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            usage("Incorrect number of arguments");
        }
        try {
            RepositoryClient client = new RepositoryClient(new URL(args[0]),
                                          args[1], args[2]);
            String[] msg = { "" };
            if ( !client.isValidSession(true, msg)) {
                System.err.println("Error: invalid session:" + msg[0]);
                return;
            }

            //            client.writeFile("46d283c8-b852-4a6f-a6e6-055b7ac19fe9",
            //                             new File("test.nc"));
            client.processCommandLine(args);
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
        }

    }





    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ClientEntry getEntry(String entryId) throws Exception {
        checkSession();
        String[] args = new String[] {
            ARG_ENTRYID, entryId, ARG_OUTPUT, "xml.xml", ARG_SESSIONID,
            getSessionId()
        };
        String url = HtmlUtil.url(URL_ENTRY_SHOW.getFullUrl(), args);
        String xml = IOUtil.readContents(url, getClass());
        Element root  = XmlUtil.getRoot(xml);

        ClientEntry   entry = new ClientEntry(entryId);
        entry.setName(XmlUtil.getAttribute(root, ATTR_NAME));
        String desc = XmlUtil.getGrandChildText(root, TAG_DESCRIPTION);
        if (desc != null) {
            entry.setDescription(desc);
        }
        if (XmlUtil.hasAttribute(root, ATTR_RESOURCE)) {
            entry.setResource(new Resource(XmlUtil.getAttribute(root,
                    ATTR_RESOURCE), XmlUtil.getAttribute(root,
                        ATTR_RESOURCE_TYPE)));
        }

        return entry;
    }


    /**
     * This gets a input stream to the file download for the given entry
     *
     * @param entryId entry id
     *
     * @return  inputstream to the file download
     *
     * @throws Exception On badness
     */
    public InputStream getResourceInputStream(String entryId)
            throws Exception {
        checkSession();
        String url = HtmlUtil.url(URL_ENTRY_GET.getFullUrl(),
                                  new String[] { ARG_ENTRYID,
                entryId, ARG_SESSIONID, getSessionId() });
        return IOUtil.getInputStream(url, getClass());
    }



    /**
     * Download the resource for the given entry and write it to the given file. If the
     * toFileOrDirectory argument is a directory then download the Entry information from
     * RAMADDA and use the file name as the name of the file to write to. If a file of that name
     * already exists then it will get overwritten
     *
     * @param entryId The entry id
     * @param toFileOrDirectory The file or directory to write to
     *
     * @return the file that was written
     * @throws Exception On badness
     */
    public File writeFile(String entryId, File toFileOrDirectory)
            throws Exception {
        //If this is a directory then we need to get the file name from ramadda
        if (toFileOrDirectory.isDirectory()) {
            ClientEntry entry = getEntry(entryId);
            if ((entry.getResource() == null)
                    || (entry.getResource().getPath() == null)) {
                throw new IllegalStateException(
                    "Given entry does not have a file");
            }
            String fileTail =
                RepositoryUtil.getFileTail(entry.getResource().getPath());
            toFileOrDirectory = new File(IOUtil.joinDir(toFileOrDirectory,
                    fileTail));
        }

        InputStream stream = getResourceInputStream(entryId);
        IOUtil.copyFile(stream, toFileOrDirectory);
        stream.close();
        return toFileOrDirectory;
    }



    /**
     * _more_
     *
     * @param node _more_
     * @param tags _more_
     *
     * @throws Exception _more_
     */
    public void addTags(Element node, List<String> tags) throws Exception {
        for (String tag : tags) {
            XmlUtil.create(node.getOwnerDocument(), TAG_METADATA, node,
                           new String[] { ATTR_TYPE,
                                          "enum_tag", ATTR_ATTR1, tag });
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void addAttachment(Element node, String filename)
            throws Exception {
        XmlUtil.create(node.getOwnerDocument(), TAG_METADATA, node,
                       new String[] { ATTR_TYPE,
                                      "content.attachment", ATTR_ATTR1,
                                      filename });
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void addThumbnail(Element node, String filename) throws Exception {
        XmlUtil.create(node.getOwnerDocument(), TAG_METADATA, node,
                       new String[] { ATTR_TYPE,
                                      "content.thumbnail", ATTR_ATTR1,
                                      filename });
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param fromId _more_
     * @param toId _more_
     * @param name _more_
     *
     * @throws Exception _more_
     */
    public void addAssociation(Element node, String fromId, String toId,
                               String name)
            throws Exception {
        XmlUtil.create(node.getOwnerDocument(), TAG_ASSOCIATION, node,
                       new String[] {
            ATTR_FROM, fromId, ATTR_TO, toId, ATTR_NAME, name
        });
    }





    /**
     * _more_
     *
     * @param error _more_
     * @param exc _more_
     */
    public void handleError(String error, Exception exc) {
        System.err.println(error);
        if (exc != null) {
            exc.printStackTrace();
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void handleMessage(String message) {
        System.err.println(message);
    }

    /**
     * _more_
     *
     *
     * @param parentId _more_
     * @param name _more_
     *
     * @return _more_
     */
    public boolean newGroup(String parentId, String name) {
        try {
            if (name == null) {
                return false;
            }
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            Element groupNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                    new String[] {
                ATTR_ID, "123456789", ATTR_TYPE, TYPE_GROUP, ATTR_PARENT,
                parentId, ATTR_NAME, name
            });

            String xml     = XmlUtil.toString(root);
            List   entries = new ArrayList();
            addUrlArgs(entries);
            entries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                                          xml.getBytes()));
            String[] result = HttpFormEntry.doPost(entries,
                                  URL_ENTRY_XMLCREATE.getFullUrl());

            if (result[0] != null) {
                handleError("Error creating group:\n" + result[0], null);
                return false;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                handleMessage("Group created");
                return true;
            }
            String body = XmlUtil.getChildText(response).trim();
            handleError("Error creating group:" + body, null);
        } catch (Exception exc) {
            handleError("Error creating group", exc);
        }
        return false;
    }




    /**
     * _more_
     *
     * @param entries _more_
     */
    public void addUrlArgs(List entries) {
        entries.add(HttpFormEntry.hidden(ARG_SESSIONID, getSessionId()));
        entries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getSessionId() {
        if (isAnonymous()) {
            return "";
        }
        return sessionId;
    }





    /**
     * _more_
     *
     * @throws InvalidSession _more_
     */
    public void checkSession() throws InvalidSession {
        String[] msg = { "" };
        if ( !isValidSession(true, msg)) {
            throw new InvalidSession(msg[0]);
        }
    }



    /**
     * _more_
     *
     * @param doLogin _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public boolean isValidSession(boolean doLogin, String[] msg) {
        if ( !isValidSession(msg)) {
            if (isAnonymous()) {
                return false;
            }
            if (doLogin) {
                return doLogin(msg);
            }
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     */
    public boolean isValidSession(String[] msg) {
        try {
            String url;
            if (isAnonymous()) {
                url = HtmlUtil.url(URL_PING.getFullUrl(),
                                   new String[] { ARG_RESPONSE,
                        RESPONSE_XML });
            } else {
                if (sessionId == null) {
                    msg[0] = "No session id";
                    return false;
                }
                url = HtmlUtil.url(URL_USER_HOME.getFullUrl(),
                                   new String[] { ARG_RESPONSE,
                        RESPONSE_XML, ARG_SESSIONID, sessionId });
            }
            String contents = IOUtil.readContents(url, getClass());
            //            System.err.println ("contents:" + contents);
            Element root = XmlUtil.getRoot(contents);
            if (responseOk(root)) {
                return true;
            } else {
                msg[0] = XmlUtil.getChildText(root).trim();
                return false;
            }
        } catch (Exception exc) {
            System.err.println("error:" + exc);
            msg[0] = "Could not connect to server: " + getHostname();
            return false;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasSession() {
        return sessionId != null;
    }


    /**
     * _more_
     *
     * @param root _more_
     *
     * @return _more_
     */
    public boolean responseOk(Element root) {
        return XmlUtil.getAttribute(root, ATTR_CODE).equals("ok");
    }



    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     */
    public boolean doLogin(String[] msg) {
        if (isAnonymous()) {
            return true;
        }
        try {
            //first get the basic information including the ssl port
            getInfo();

            List entries = Misc.toList(new Object[] {
                               HttpFormEntry.hidden(ARG_RESPONSE,
                                   RESPONSE_XML),
                               HttpFormEntry.hidden(ARG_USER_PASSWORD,
                                   getPassword()),
                               HttpFormEntry.hidden(ARG_USER_ID,
                                   getUser()) });
            String[] result = doPost(URL_USER_LOGIN, entries);
            if (result[0] != null) {
                msg[0] = "Error logging in: " + result[0];
                return false;
            }
            String contents = result[1];
            //            System.err.println(contents);
            Element root = XmlUtil.getRoot(contents);
            String  body = XmlUtil.getChildText(root).trim();
            if (responseOk(root)) {
                sessionId = body;
                return true;
            } else {
                msg[0] = body;
                return false;
            }
        } catch (java.io.IOException exc) {
            msg[0] = "Could not connect to server: " + getHostname();

        } catch (Exception exc) {
            msg[0] = "An error occurred: " + exc + "\n";
        }
        return false;
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void getInfo() throws Exception {
        String url = HtmlUtil.url(URL_INFO.getFullUrl(),
                                  new String[] { ARG_RESPONSE,
                RESPONSE_XML });

        String contents = IOUtil.readContents(url, getClass());
        //        System.err.println(contents);
        Element root = XmlUtil.getRoot(contents);

        sslPort = XmlUtil.getGrandChildText(root,
                                            ServerInfo.TAG_INFO_SSLPORT);
        title = XmlUtil.getGrandChildText(root, ServerInfo.TAG_INFO_TITLE);
        description = XmlUtil.getGrandChildText(root,
                ServerInfo.TAG_INFO_DESCRIPTION);
        //        System.err.println (sslPort + "  "+ title +" " + description);
        if (sslPort != null) {
            URL_USER_LOGIN.setNeedsSsl(true);
        } else {
            URL_USER_LOGIN.setNeedsSsl(false);
        }
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(XmlUtil.decodeBase64(new String(value)));
        }
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }
        return XmlUtil.encodeBase64(password.getBytes()).getBytes();
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getPassword() {
        return password;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    protected void setPassword(String s) {
        password = s;
    }

    /**
     *  Get the Anonymous property.
     *
     *  @return The Anonymous
     */
    public boolean isAnonymous() {
        return (user != null) && (user.trim().length() == 0);
    }


    /**
     *  Set the DefaultGroup property.
     *
     *  @param value The new value for DefaultGroup
     */
    public void setDefaultGroupId(String value) {
        defaultGroupId = value;
    }

    /**
     *  Get the DefaultGroup property.
     *
     *  @return The DefaultGroup
     */
    public String getDefaultGroupId() {
        return defaultGroupId;
    }

    /**
     *  Set the DefaultGroupName property.
     *
     *  @param value The new value for DefaultGroupName
     */
    public void setDefaultGroupName(String value) {
        defaultGroupName = value;
    }

    /**
     *  Get the DefaultGroupName property.
     *
     *  @return The DefaultGroupName
     */
    public String getDefaultGroupName() {
        return defaultGroupName;
    }



    /**
     * Class InvalidSession _more_
     *
     *
     * @author IDV Development Team
     */
    public static class InvalidSession extends RuntimeException {

        /**
         * _more_
         *
         * @param message _more_
         */
        public InvalidSession(String message) {
            super(message);
        }
    }


}

