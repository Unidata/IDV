/*
 * $Id: InfoceteraBlogger.java,v 1.13 2005/05/13 18:31:06 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.repository.idv;


import HTTPClient.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.idv.*;

import ucar.unidata.idv.publish.IdvPublisher;

import ucar.unidata.repository.RepositoryClient;

import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.awt.*;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.*;


import javax.swing.*;


/**
 * @author IDV development team
 */
public class RamaddaPublisher extends ucar.unidata.idv.publish
    .IdvPublisher implements ucar.unidata.repository.Constants {

    /** _more_ */
    private RepositoryClient repositoryClient;


    /**
     * _more_
     */
    public RamaddaPublisher() {}



    /**
     * Create the object
     *
     * @param idv The idv
     * @param element _more_
     */
    public RamaddaPublisher(IntegratedDataViewer idv, Element element) {
        super(idv, element);
        repositoryClient = new RepositoryClient();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        if (repositoryClient != null) {
            return repositoryClient.getName();
        }
        return super.getName();
    }




    /**
     * What is the name of this publisher
     *
     * @return The name
     */
    public String getTypeName() {
        return "RAMADDA repository";
    }


    public void configure(){
        if (repositoryClient == null) {
            repositoryClient = new RepositoryClient();
        }
        repositoryClient.showConfigDialog();
    }


    /**
     * Do the configuration
     *
     * @return Configuration ok
     */
    public boolean doInitNew() {
        return isConfigured();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isConfigured() {
        if (repositoryClient == null) {
            repositoryClient = new RepositoryClient();
        }
        return repositoryClient.doConnect();
    }


    /** _more_ */
    private JComponent contents;

    /** _more_ */
    private JTextField parentGroupFld;

    /** _more_ */
    private JTextField groupNameFld;

    /** _more_ */
    private JTextField nameFld;

    /** _more_ */
    private JTextArea descFld;

    /** _more_ */
    private JTextField contentsNameFld;

    /** _more_ */
    private JTextArea contentsDescFld;

    /** _more_ */
    private JTextField northFld;

    /** _more_ */
    private JTextField southFld;

    /** _more_ */
    private JTextField eastFld;

    /** _more_ */
    private JTextField westFld;



    /**
     * _more_
     */
    private void doMakeContents() {
        parentGroupFld  = new JTextField("", 5);
        groupNameFld    = new JTextField("", 30);
        nameFld         = new JTextField("", 30);
        descFld         = new JTextArea("", 5, 30);
        contentsNameFld = new JTextField("", 30);
        contentsDescFld = new JTextArea("", 5, 30);
        northFld        = new JTextField("", 5);
        southFld        = new JTextField("", 5);
        eastFld         = new JTextField("", 5);
        westFld         = new JTextField("", 5);
        JComponent bboxComp = GuiUtils.vbox(GuiUtils.wrap(northFld),
                                            GuiUtils.hbox(westFld, eastFld),
                                            GuiUtils.wrap(southFld));


        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        contents           = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Name:"), nameFld,
            GuiUtils.top(GuiUtils.rLabel("Description:")), descFld,
            GuiUtils.rLabel("New Group Name:"),
            GuiUtils.centerRight(groupNameFld, new JLabel(" (Optional)")),
            GuiUtils.rLabel("Parent Group Id:"),
            GuiUtils.left(parentGroupFld),
            GuiUtils.top(GuiUtils.rLabel("BBOX:")), GuiUtils.left(bboxComp)
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
    }


    /**
     * _more_
     *
     * @param contentFile _more_
     */
    public void publishContent(String contentFile) {

        if ( !isConfigured()) {
            return;
        }


        try {
            boolean isBundle = ((contentFile == null)
                                ? false
                                : getIdv().getArgsManager().isBundleFile(
                                    contentFile));
            if (contents == null) {
                doMakeContents();
            }
            JCheckBox  doBundleCbx  = new JCheckBox("Include Bundle", true);
            JComponent thisContents = contents;
            if ((contentFile != null) && !isBundle) {
                thisContents =
                    GuiUtils.topCenter(GuiUtils.vbox(new JLabel("File: "
                        + contentFile), doBundleCbx), contents);
            }


            if (contentFile != null) {
                //            nameFld.setText(IOUtil.getFileTail(contentFile));
            } else {
                //            nameFld.setText("");
            }


            while (true) {

                while (true) {
                    if ( !GuiUtils.showOkCancelDialog(null,
                            "Publish to RAMADDA", thisContents, null)) {
                        return;
                    }
                    String parentGroup = parentGroupFld.getText().trim();
                    if (parentGroup.length() == 0) {
                        LogUtil.userMessage(
                            "You must specify a parent group id");
                    } else {
                        break;
                    }
                }

                String bundleFile = null;
                if (isBundle) {
                    bundleFile  = contentFile;
                    contentFile = null;
                } else if (doBundleCbx.isSelected()) {
                    String tmpFile = contentFile;
                    if(tmpFile==null) tmpFile = "publish.xidv";
                    bundleFile = getIdv().getObjectStore().getTmpFile(
                        IOUtil.stripExtension(
                            IOUtil.getFileTail(tmpFile)) + ".xidv");
                    getIdv().getPersistenceManager().doSave(bundleFile);
                }


                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream       zos = new ZipOutputStream(bos);

                if (contentFile != null) {
                    zos.putNextEntry(
                        new ZipEntry(IOUtil.getFileTail(contentFile)));
                    byte[] bytes =
                        IOUtil.readBytes(new FileInputStream(contentFile));
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                }

                if (bundleFile != null) {
                    zos.putNextEntry(
                        new ZipEntry(IOUtil.getFileTail(bundleFile)));
                    byte[] bytes =
                        IOUtil.readBytes(new FileInputStream(bundleFile));
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                }



                //        descFld.setText("");


                int      cnt = 0;
                Document doc = XmlUtil.makeDocument();
                Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                   new String[] {});
                String parentId = parentGroupFld.getText().trim();
                if (groupNameFld.getText().trim().length() > 0) {
                    String groupId = (cnt++) + "";
                    Element groupNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                            new String[] {
                        ATTR_ID, groupId, ATTR_TYPE, TYPE_GROUP, ATTR_PARENT,
                        parentId, ATTR_NAME, groupNameFld.getText().trim()
                    });
                    parentId = groupId;
                }
                String mainId    = (cnt++) + "";
                String contentId = (cnt++) + "";
                String mainFile  = ((bundleFile != null)
                                    ? bundleFile
                                    : contentFile);


                XmlUtil.create(doc, TAG_ENTRY, root, new String[] {
                    ATTR_ID, mainId, ATTR_FILE, IOUtil.getFileTail(mainFile),
                    ATTR_PARENT, parentId, ATTR_TYPE, TYPE_FILE, ATTR_NAME,
                    nameFld.getText().trim(), ATTR_DESCRIPTION,
                    descFld.getText().trim()
                });

                if (contentFile != null) {
                    XmlUtil.create(doc, TAG_ENTRY, root, new String[] {
                        ATTR_ID, contentId, ATTR_FILE,
                        IOUtil.getFileTail(contentFile), ATTR_PARENT,
                        parentId, ATTR_TYPE, TYPE_FILE, ATTR_NAME,
                        nameFld.getText().trim() + " - Product"
                    });
                    if(bundleFile!=null) {
                        XmlUtil.create(doc, TAG_ASSOCIATION, root, new String[] {
                                ATTR_FROM, mainId,
                                ATTR_TO, contentId,
                                ATTR_NAME, "generated product"
                            });

                    }

                }



                String xml = XmlUtil.toString(root);

                zos.putNextEntry(new ZipEntry("entries.xml"));
                byte[] bytes = xml.getBytes();
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
                zos.close();
                bos.close();


                List entries = new ArrayList();
                System.err.println("id:" + repositoryClient.getSessionId());
                entries.add(HttpFormEntry.hidden(ARG_SESSIONID,
                        repositoryClient.getSessionId()));
                entries.add(HttpFormEntry.hidden(ARG_OUTPUT, "xml"));
                entries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                        bos.toByteArray()));
                String[] result =
                    HttpFormEntry.doPost(
                        entries,
                        repositoryClient.URL_ENTRY_XMLCREATE.getFullUrl());

                if (result[0] != null) {
                    LogUtil.userErrorMessage("Error publishing:" + result[0]);
                    return;
                }
                Element response = XmlUtil.getRoot(result[1]);
                if (repositoryClient.responseOk(response)) {
                    LogUtil.userMessage("Publication was successful");
                    return;
                }
                String body = XmlUtil.getChildText(response).trim();
                LogUtil.userErrorMessage("Error publishing:" + body);
            }
        } catch (Exception exc) {
            LogUtil.logException("Publishing", exc);
        }

    }


    /**
     * _more_
     *
     * @param title _more_
     * @param filePath _more_
     * @param properties _more_
     */
    public void doPublish() {
        publishContent(null);
    }



    /**
     *  Set the RepositoryClient property.
     *
     *  @param value The new value for RepositoryClient
     */
    public void setRepositoryClient(RepositoryClient value) {
        repositoryClient = value;
    }

    /**
     *  Get the RepositoryClient property.
     *
     *  @return The RepositoryClient
     */
    public RepositoryClient getRepositoryClient() {
        return repositoryClient;
    }






}

