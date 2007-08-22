/*
 * $Id: ThreddsPublisher.java,v 1.8 2005/10/10 18:52:16 dmurray Exp $
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

package ucar.unidata.idv.publish;



import ucar.unidata.idv.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.IOUtil;


import ucar.unidata.data.DataSourceImpl;

import java.awt.*;

import javax.swing.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import java.net.*;

import java.io.*;

import ucar.unidata.xml.XmlUtil;

import ucar.unidata.idv.chooser.ThreddsHandler;


import HTTPClient.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Note, This is very much in flux.
 * A client to an infocetera weblog (see: http://www.infocetera.com).
 *
 * @author IDV development team
 */
public class ThreddsPublisher {


    /** Reference to the IDV */
    private IntegratedDataViewer idv;

    /**
     * Create the thredds publisher
     *
     * @param idv The idv
     */
    public ThreddsPublisher(IntegratedDataViewer idv) {
        this.idv = idv;
    }


    /**
     * Check the return xml from the http post
     *
     * @param xml The xml we go back from infocetera
     * @return Was this ok
     */
    public boolean resultOk(String xml) {
        if (xml.indexOf("error") >= 0) {
            return false;
        }
        if (xml.indexOf("Error") >= 0) {
            return false;
        }
        if (xml.indexOf("ERROR") >= 0) {
            return false;
        }
        return true;
    }



    /**
     *  This method strips the html tags from the given html error message.
     *  It also strips out the copyright and other footer lines
     *  and shows the results to the user.
     *
     * @param msg The message
     * @param html  The html
     */
    protected void showError(String msg, String html) {
        int idx = -1;
        if (idx < 0) {
            idx = html.indexOf("error");
        }
        if (idx < 0) {
            idx = html.indexOf("Error");
        }
        if (idx < 0) {
            idx = html.indexOf("ERROR");
        }
        JComponent p = null;
        if (idx >= 0) {
            idx  = idx - 10;
            html = html.substring(idx);
            html = StringUtil.stripTags(html);
            idx  = 0;
            List lines = StringUtil.split(html, "\n");
            html = "";
            for (int i = 0; i < lines.size(); i++) {
                String line = (String) lines.get(i);
                html = html + line + "\n";
            }
            p = GuiUtils.makeScrollPane(new JTextArea(html, 10, 50), 10, 50);
        } else {
            html = "";
        }
        JPanel msgPanel = GuiUtils.topCenter(new JLabel(msg), p);
        LogUtil.userErrorMessage(msgPanel);
    }

    /**
     * Go ahead and publish
     */
    public void publish() {
        //Run in a different thread because of the image grab
        Misc.run(new Runnable() {
            public void run() {
                publishInner();
            }
        });
    }



    /**
     * Publish the current state as a bundle
     */
    private void publishInner() {

        try {
            List   dataSources      = idv.getDataSources();
            String catalogUrl       = null,
                   groupId          = null,
                   datasetId        = null,
                   annotationServer = null;
            //FInd the first data source that has an catalog url and annotation server property
            for (int i = 0; i < dataSources.size(); i++) {
                DataSourceImpl dataSource =
                    (DataSourceImpl) dataSources.get(i);
                Hashtable properties = dataSource.getProperties();
                annotationServer = (String) properties.get(
                    ThreddsHandler.PROP_ANNOTATIONSERVER);
                catalogUrl =
                    (String) properties.get(ThreddsHandler.PROP_CATALOGURL);
                groupId =
                    (String) properties.get(ThreddsHandler.PROP_DATASETGROUP);
                datasetId =
                    (String) properties.get(ThreddsHandler.PROP_DATASETID);
                if ((catalogUrl != null) || (groupId != null)
                        || (datasetId != null)) {
                    break;
                }
            }



            if ( !((catalogUrl != null) || (annotationServer != null))) {
                LogUtil.userMessage(
                    "No Thredds publishable datasets are available");
            }

            //For now, treat this as optional
            if (datasetId == null) {
                datasetId = "";
            }

            //We don't use the group right now but may later?
            if (groupId == null) {
                groupId = "";
            }

            //Ask the user for title and description
            JTextField titleFld = new JTextField("", 20);
            JTextArea  descFld  = new JTextArea("", 5, 20);
            JPanel panel = GuiUtils.doLayout(new Component[]{
                               GuiUtils.rLabel("Subject: "),
                               titleFld,
                               GuiUtils.top(GuiUtils.rLabel("Description: ")),
                               GuiUtils.makeScrollPane(descFld, 200, 50) }, 2,
                                   GuiUtils.WT_NY, GuiUtils.WT_N);

            panel = GuiUtils.topCenter(GuiUtils.cLabel("Publish to Thredds"),
                                       panel);
            if ( !GuiUtils.showOkCancelDialog(null, "Thredds Publisher",
                                              panel, null)) {
                return;
            }

            //Make the annotation url
            if ( !annotationServer.startsWith("http:")) {
                if (annotationServer.startsWith("/")) {
                    URL tmpUrl = new URL(catalogUrl);
                    annotationServer = "http://" + tmpUrl.getHost() + ":"
                                       + tmpUrl.getPort() + annotationServer;
                } else {
                    annotationServer = IOUtil.getFileRoot(catalogUrl) + "/"
                                       + annotationServer;
                }
                //                System.err.println("annotation server:" + annotationServer);
            }




            String bundle = idv.getPersistenceManager().getBundleXml(true);
            bundle = idv.getPersistenceManager().getJnlpBundle(bundle);
            //IOUtil.writeFile("test.jnlp", bundle);

            //Get the jpeg
            String jpgFile =
                idv.getStore().getTmpFile("" + System.currentTimeMillis()
                                          + ".jpg");
            idv.getImageGenerator().captureImage(jpgFile);
            String jpgContent = IOUtil.readContents(jpgFile);
            new File(jpgFile).delete();
            if (jpgContent != null) {
                jpgContent = XmlUtil.encodeBase64(jpgContent.getBytes());
            }


            HTTPClient.NVPair[] opts = {
                new HTTPClient.NVPair("title", titleFld.getText()),
                new HTTPClient.NVPair("description", descFld.getText()),
                new HTTPClient.NVPair("jnlp", bundle),
                new HTTPClient.NVPair("catalog", catalogUrl),
                new HTTPClient.NVPair("dataset", datasetId),
                new HTTPClient.NVPair("type", "view"),
                new HTTPClient.NVPair("jpg", jpgContent),
            };
            URL url = new URL(annotationServer);
            HTTPConnection conn = new HTTPConnection(url.getHost(),
                                                     url.getPort());
            HTTPResponse res = conn.Post(url.getFile(), opts);
            if (res.getStatusCode() >= 300) {
                LogUtil.userErrorMessage(
                    "An error has occurred:\n"
                    + StringUtil.stripTags(new String(res.getData())));
                return;
            }
            byte[] data = res.getData();
            String html = new String(data);
            if ( !resultOk(html)) {
                showError("An error has occurred posting to the weblog",
                          html);
                return;
            }
            System.err.println("result=" + html);
        } catch (Exception exc) {
            LogUtil.logException("Publishing bundle", exc);
        }
    }




}







