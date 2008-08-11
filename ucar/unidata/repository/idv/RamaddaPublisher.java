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

import ucar.unidata.idv.publish.IdvPublisher;

import HTTPClient.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.idv.*;

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

import javax.swing.*;


/**
 * @author IDV development team
 */
public class RamaddaPublisher 
    extends ucar.unidata.idv.publish.IdvPublisher {

    /** _more_          */
    private RepositoryClient repositoryClient;


    /**
     * _more_
     */
    public RamaddaPublisher() {}



    /**
     * Create the object
     *
     * @param idv The idv
     */
    public RamaddaPublisher(IntegratedDataViewer idv, Element element) {
        super(idv, element);
        repositoryClient = new RepositoryClient();
    }


    public String getName () {
        if(repositoryClient!=null) return repositoryClient.getName();
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
        if(repositoryClient==null) {
            repositoryClient = new RepositoryClient();
        }
        return repositoryClient.doConnect();
    }

    
    private JComponent contents;
    private JTextField parentGroupFld;
    private JTextField groupNameFld;

    private JTextField nameFld;
    private JTextArea descFld;
    private JTextField northFld;
    private JTextField southFld;
    private JTextField eastFld;
    private JTextField westFld;



    private void doMakeContents() {
        parentGroupFld = new JTextField("",5);
        groupNameFld = new JTextField("",30);
        nameFld = new JTextField("",30);
        descFld = new JTextArea("",5,30);
        northFld = new JTextField("",5);
        southFld = new JTextField("",5);
        eastFld = new JTextField("",5);
        westFld = new JTextField("",5);
        JComponent bboxComp = GuiUtils.vbox(GuiUtils.wrap(northFld), GuiUtils.hbox(westFld,eastFld), GuiUtils.wrap(southFld));
        

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        contents = GuiUtils.doLayout(new Component[]{
            GuiUtils.rLabel("Name:"), nameFld,
            GuiUtils.top(GuiUtils.rLabel("Description:")), descFld,
            GuiUtils.rLabel("New Group Name:"), GuiUtils.centerRight(groupNameFld, new JLabel(" (Optional)")),
            GuiUtils.rLabel("Parent Group Id:"), GuiUtils.left(parentGroupFld),
            GuiUtils.top(GuiUtils.rLabel("BBOX:")), GuiUtils.left(bboxComp)
        },2,GuiUtils.WT_NY, GuiUtils.WT_N);

    }


    /**
     * _more_
     *
     * @param title _more_
     * @param filePath _more_
     * @param properties _more_
     */
    public void doPublish() {
        if ( !isConfigured()) {
            return;
        }
        if(contents==null) {
            doMakeContents();
        }

        while(true) {
            if(!GuiUtils.showOkCancelDialog(null,"Publish to RAMADDA",contents, null)) return;
            String parentGroup = parentGroupFld.getText().trim();
            if(parentGroup.length()==0) {
                LogUtil.userMessage("You must specify a parent group id");
            }  else {
                break;
            }
            
        }
        
        try {
            int cnt = 0;
            Document doc   = XmlUtil.makeDocument();
            Element  root  = XmlUtil.create(doc, RepositoryClient.TAG_ENTRIES, null, new String[] {});
            String parentId = parentGroupFld.getText().trim();
            if(groupNameFld.getText().trim().length()>0) {
                String groupId = (cnt++)+"";
                Element groupNode = XmlUtil.create(doc, RepositoryClient.TAG_ENTRY, root,
                                                   new String[] {RepositoryClient.ATTR_ID, groupId,
                                                                 RepositoryClient.ATTR_TYPE, RepositoryClient.TYPE_GROUP,
                                                                 RepositoryClient.ATTR_PARENT, parentId, 
                                                                 RepositoryClient.ATTR_NAME, groupNameFld.getText().trim() });
                parentId  = groupId;
            }
            String bundleId = (cnt++)+"";
            String imageId = (cnt++)+"";
            String movieId = (cnt++)+"";

            Element bundleNode = XmlUtil.create(doc, RepositoryClient.TAG_ENTRY, root,
                                                new String[] {RepositoryClient.ATTR_ID, bundleId,
                                                              RepositoryClient.ATTR_PARENT, parentId, 
                                                              RepositoryClient.ATTR_TYPE, RepositoryClient.TYPE_FILE,
                                                              RepositoryClient.ATTR_NAME, nameFld.getText().trim(),
                                                              RepositoryClient.ATTR_DESCRIPTION, descFld.getText().trim()});



            String xml = XmlUtil.toString(root);
            List entries = new ArrayList();
            entries.add(HttpFormEntry.hidden(RepositoryClient.ARG_SESSIONID, repositoryClient.getSessionId()));
            entries.add(HttpFormEntry.hidden(RepositoryClient.ARG_OUTPUT, "xml"));
            entries.add(new HttpFormEntry(RepositoryClient.ARG_FILE, "entries.xml",xml.getBytes()));
            String[]result = HttpFormEntry.doPost(entries, 
                                                  repositoryClient.URL_ENTRY_XMLCREATE.getFullUrl());

            System.err.println("result:" + result[0]+" " + result[1]);
            /*
            String file = "foo";
            HTTPClient.NVPair[] opts = { new HTTPClient.NVPair(RepositoryClient.ARG_SESSIONID, repositoryClient.getSessionId()),
                                         new HTTPClient.NVPair(RepositoryClient.ARG_OUTPUT, "xml")};

            HTTPClient.NVPair[] files = { new HTTPClient.NVPair(RepositoryClient.ARG_FILE,
                                            file) };
            HTTPClient.NVPair[] hdrs           = new HTTPClient.NVPair[1];
            byte[] formData = Codecs.mpFormDataEncode(opts, files, hdrs);
            HTTPConnection      conn           = new HTTPConnection(repositoryClient.getHostname(),
                                                                    repositoryClient.getPort());
            String              fileUploadPath = .toString();
            HTTPResponse res = conn.Post(fileUploadPath, formData, hdrs);
            String xml = new String(res.getData());
            if (res.getStatusCode() >= 300) {
                LogUtil.userErrorMessage(new String(res.getData()));
                return;
            }

            System.err.println("xml:" + xml);
            */
            //            if ( !resultOk(html)) {
            //                showError("An error has occurred posting the file", html);
            //            }
        } catch (Exception exc) {
            LogUtil.logException("Publishing", exc);
        }


    }



    /**
       Set the RepositoryClient property.

       @param value The new value for RepositoryClient
    **/
    public void setRepositoryClient (RepositoryClient value) {
	repositoryClient = value;
    }

    /**
       Get the RepositoryClient property.

       @return The RepositoryClient
    **/
    public RepositoryClient getRepositoryClient () {
	return repositoryClient;
    }






}

