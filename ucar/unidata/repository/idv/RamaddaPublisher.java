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



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataSource;

import ucar.unidata.idv.*;

import ucar.unidata.idv.publish.IdvPublisher;

import ucar.unidata.repository.InteractiveRepositoryClient;

import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.display.Animation;


import visad.DateTime;



import java.awt.*;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.*;


import javax.swing.*;
import ucar.unidata.ui.RovingProgress;

/**
 * @author IDV development team
 */
public class RamaddaPublisher extends ucar.unidata.idv.publish
    .IdvPublisher implements ucar.unidata.repository.Constants {


    /** _more_          */
    private DateTimePicker fromDateFld;

    /** _more_          */
    private DateTimePicker toDateFld;


    /** _more_ */
    private JTextField nameFld;

    private JTextField tagFld;

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

    /** _more_          */
    private JCheckBox doBundleCbx = new JCheckBox("Include bundle", true);

    private JCheckBox uploadZidvDataCbx = new JCheckBox("Upload ZIDV Data", false);
    private JCheckBox uploadZidvBundleCbx = new JCheckBox("Upload ZIDV Bundle", true);

    /** _more_          */
    private JCheckBox myAddAssociationCbx = new JCheckBox("", false);


    /** _more_          */
    private List comps;


    /** _more_ */
    private InteractiveRepositoryClient repositoryClient;


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
        repositoryClient = new InteractiveRepositoryClient();
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


    /**
     * _more_
     */
    public void configure() {
        if (repositoryClient == null) {
            repositoryClient = new InteractiveRepositoryClient();
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
            repositoryClient = new InteractiveRepositoryClient();
        }
        return repositoryClient.doConnect();
    }





    /**
     * _more_
     */
    private void doMakeContents() {
        if (fromDateFld != null) {
            return;
        }
        //        dialog = new JDialog("Publish to RAMADDA",true);

        comps = new ArrayList();
        Date now = new Date();
        fromDateFld     = new DateTimePicker(now);
        toDateFld       = new DateTimePicker(now);
        nameFld         = new JTextField("", 30);
        tagFld         = new JTextField("", 30);
        tagFld.setToolTipText("Comma separated tag values");
        descFld         = new JTextArea("", 5, 30);
        contentsNameFld = new JTextField("", 30);
        contentsDescFld = new JTextArea("", 5, 30);
        northFld        = new JTextField("", 5);
        southFld        = new JTextField("", 5);
        eastFld         = new JTextField("", 5);
        westFld         = new JTextField("", 5);
        JComponent treeComp = repositoryClient.getTreeComponent();

        Insets     i        = new Insets(1, 1, 1, 1);
        JComponent bboxComp = GuiUtils.vbox(
                                  GuiUtils.wrap(GuiUtils.inset(northFld, i)),
                                  GuiUtils.hbox(
                                      GuiUtils.inset(westFld, i),
                                      GuiUtils.inset(
                                          eastFld, i)), GuiUtils.wrap(
                                              GuiUtils.inset(southFld, i)));



        Dimension dim = new Dimension(200, 50);
        JScrollPane descScroller = GuiUtils.makeScrollPane(descFld,
                                       (int) dim.getWidth(),
                                       (int) dim.getHeight());
        descScroller.setPreferredSize(dim);
        JComponent dateComp = GuiUtils.hbox(fromDateFld, toDateFld);
        comps = Misc.toList(new Component[] {
            GuiUtils.rLabel("Name:"), nameFld,
            GuiUtils.top(GuiUtils.rLabel("Description:")), descScroller,
            GuiUtils.rLabel("Tags:"), tagFld,
            GuiUtils.rLabel("Parent Group:"), treeComp,
            GuiUtils.top(GuiUtils.rLabel("Date Range:")), dateComp,
            GuiUtils.rLabel("Lat/Lon Box:"), GuiUtils.left(bboxComp)
        });
    }


    /** _more_          */
    private String lastBundleFile;

    /** _more_          */
    private String lastBundleId;

    /** _more_          */
    private boolean dialogOk;

    /** _more_          */
    private JDialog dialog;

    /**
     * _more_
     *
     * @param contentFile _more_
     * @param fromViewManager _more_
     */
    public void publishContent(String contentFile,
                               ViewManager fromViewManager) {

        if ( !isConfigured()) {
            return;
        }

        try {
            boolean isBundle = ((contentFile == null)
                                ? false
                                : getIdv().getArgsManager().isBundleFile(
                                    contentFile));

            boolean isZidv = ((contentFile == null)
                                ? false
                                : getIdv().getArgsManager().isZidvFile(
                                    contentFile));

            doMakeContents();
            List      myComps            = new ArrayList(comps);

            JCheckBox addAssociationCbx  = null;
            List      topComps           = new ArrayList();

            List      myDataSources      = new ArrayList();
            List      myDataSourcesCbx   = new ArrayList();
            List      myDataSourcesIds   = new ArrayList();
            List      notMineDataSources = new ArrayList();
            List      dataSources        = getIdv().getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                DataSource dataSource = (DataSource) dataSources.get(i);
                String ramaddaId =
                    (String) dataSource.getProperty("ramadda.id");
                String ramaddaHost =
                    (String) dataSource.getProperty("ramadda.host");
                if ((ramaddaId == null) || (ramaddaHost == null)) {
                    notMineDataSources.add(dataSource);
                    continue;
                }
                if ( !Misc.equals(ramaddaHost,
                                  repositoryClient.getHostname())) {
                    notMineDataSources.add(dataSource);
                    continue;
                }
                myDataSources.add(dataSource);
                myDataSourcesCbx.add(new JCheckBox(dataSource.toString(),
                        false));
                myDataSourcesIds.add(ramaddaId);
            }




            if ((contentFile != null) && !isBundle) {
                topComps.add(GuiUtils.rLabel("File:"));
                topComps.add(
                    GuiUtils.left(
                        GuiUtils.hbox(
                            new JLabel(IOUtil.getFileTail(contentFile)),
                            GuiUtils.filler(10, 5), doBundleCbx)));
                if (lastBundleId != null) {
                    addAssociationCbx = myAddAssociationCbx;
                    addAssociationCbx.setText(
                        "Add association with last bundle: "
                        + IOUtil.getFileTail(lastBundleFile));
                    topComps.add(GuiUtils.filler());
                    topComps.add(addAssociationCbx);
                }
            }


            if(isZidv) {
                topComps.add(GuiUtils.filler());
                topComps.add(GuiUtils.left(GuiUtils.hbox(uploadZidvDataCbx,uploadZidvBundleCbx)));
            }


            int numTopComps = topComps.size();
            topComps.addAll(myComps);
            if (myDataSourcesCbx.size() > 0) {
                topComps.add(GuiUtils.rLabel("Make associations to:"));
                topComps.add(GuiUtils.left(GuiUtils.vbox(myDataSourcesCbx)));
            }


            GuiUtils.tmpInsets = GuiUtils.INSETS_5;
            double[] wts = {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            };
            wts[numTopComps / 2 + 1] = 0.2;
            wts[numTopComps / 2 + 3] = 1.0;


            JComponent contents = GuiUtils.doLayout(topComps, 2,
                                      GuiUtils.WT_NY, wts);


            //Get one from the list
            if (fromViewManager == null) {
                List viewManagers = getIdv().getVMManager().getViewManagers();
                if (viewManagers.size() == 1) {
                    fromViewManager = (ViewManager) viewManagers.get(0);
                }
            }


            if (fromViewManager != null) {
                if ((fromViewManager instanceof MapViewManager)) {
                    MapViewManager     mvm = (MapViewManager) fromViewManager;
                    NavigatedDisplay   navDisplay = mvm.getNavigatedDisplay();
                    Rectangle2D.Double bbox = navDisplay.getLatLonBox(false,false);
                    if (bbox != null) {
                        southFld.setText(getIdv().getDisplayConventions().formatLatLon(bbox.getY()));
                        northFld.setText(getIdv().getDisplayConventions().formatLatLon((bbox.getY()
                                + bbox.getHeight())));
                        westFld.setText(getIdv().getDisplayConventions().formatLatLon(bbox.getX()));
                        eastFld.setText(getIdv().getDisplayConventions().formatLatLon((bbox.getX() + bbox.getWidth())));
                    }
                }
                Animation anim = fromViewManager.getAnimation();
                if (anim != null) {
                    DateTime[] dttms = anim.getTimes();
                    if ((dttms != null) && (dttms.length > 0)) {
                        fromDateFld.setDate(Util.makeDate(dttms[0]));
                        toDateFld.setDate(
                            Util.makeDate(dttms[dttms.length - 1]));
                    }

                }
            }
            if (contentFile != null) {
                nameFld.setText(IOUtil.getFileTail(contentFile));
            } else {
                nameFld.setText("");
            }


            dialogOk = false;
            String parentId = null;
            while (true) {
                while (true) {
                    if ( !GuiUtils.showOkCancelDialog(null,
                            "Publish to RAMADDA", contents, null)) {
                        return;
                    }
                    parentId = repositoryClient.getSelectedGroupFromTree();
                    if (parentId == null) {
                        LogUtil.userMessage("You must select a parent group");
                    } else {
                        break;
                    }
                }

                JLabel statusLbl = new JLabel(" ");
                RovingProgress progress = new RovingProgress();
                progress.setMinimumSize(new Dimension(150,20));
                progress.start();
                JDialog dialog = GuiUtils.createDialog(null,"Publishing to RAMADDA",false);
                dialog.getContentPane().add(GuiUtils.vbox(progress,statusLbl));
                dialog.pack();
                dialog.setLocation(200,200);
                dialog.show();

                String bundleFile = null;
                if (isBundle) {
                    bundleFile  = contentFile;
                    contentFile = null;
                } else if (doBundleCbx.isSelected()) {
                    String tmpFile = contentFile;
                    if (tmpFile == null) {
                        tmpFile = "publish.xidv";
                    }
                    bundleFile = getIdv().getObjectStore().getTmpFile(
                        IOUtil.stripExtension(IOUtil.getFileTail(tmpFile))
                        + ".xidv");
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

                String fromDate =
                    repositoryClient.formatDate(fromDateFld.getDate());
                String toDate =
                    repositoryClient.formatDate(toDateFld.getDate());
                int      cnt = 0;
                Document doc = XmlUtil.makeDocument();
                //Create the top level node
                Element root = XmlUtil.create(doc, TAG_ENTRIES);
                List tags = StringUtil.split(tagFld.getText().trim(),",",true,true);

                String mainId    = (cnt++) + "";
                String contentId = (cnt++) + "";
                String mainFile;
                if (bundleFile != null) {
                    mainFile = bundleFile;
                } else {
                    mainFile    = contentFile;
                    contentFile = null;
                    contentId   = mainId;
                }

                String zidvFile = (isZidv?bundleFile:null);

                List attrs;
                Element node;


                //Create entry attributes
                attrs = Misc.toList(new String[] {
                    ATTR_ID, mainId, ATTR_FILE, IOUtil.getFileTail(mainFile),
                    ATTR_PARENT, parentId, ATTR_TYPE, TYPE_FILE, ATTR_NAME,
                    nameFld.getText().trim(), ATTR_DESCRIPTION,
                    descFld.getText().trim(), ATTR_FROMDATE, fromDate,
                    ATTR_TODATE, toDate
                });


                checkAndAdd(attrs, ATTR_NORTH, northFld);
                checkAndAdd(attrs, ATTR_SOUTH, southFld);
                checkAndAdd(attrs, ATTR_EAST, eastFld);
                checkAndAdd(attrs, ATTR_WEST, westFld);


                //Create the entry node
                node = XmlUtil.create(TAG_ENTRY, root,attrs);
                repositoryClient.addTags(node,tags);

                for (int i = 0; i < myDataSourcesCbx.size(); i++) {
                    if (((JCheckBox) myDataSourcesCbx.get(i)).isSelected()) {
                        String id = (String) myDataSourcesIds.get(i);
                        repositoryClient.addAssociation(root,id,mainId, "uses data");
                    }
                }

                if(isZidv && !uploadZidvBundleCbx.isSelected()) {
                    bundleFile = null;
                }

                if (contentFile != null) {
                    attrs = Misc.toList(new String[] {
                        ATTR_ID, contentId, ATTR_FILE,
                        IOUtil.getFileTail(contentFile), ATTR_PARENT,
                        parentId, ATTR_TYPE, TYPE_FILE, ATTR_NAME,
                        nameFld.getText().trim() + " - Product",
                        ATTR_FROMDATE, fromDate, ATTR_TODATE, toDate
                    });
                    checkAndAdd(attrs, ATTR_NORTH, northFld);
                    checkAndAdd(attrs, ATTR_SOUTH, southFld);
                    checkAndAdd(attrs, ATTR_EAST, eastFld);
                    checkAndAdd(attrs, ATTR_WEST, westFld);

                    node = XmlUtil.create(TAG_ENTRY, root,attrs);
                    repositoryClient.addTags(node,tags);
                    if (bundleFile != null) {
                        repositoryClient.addAssociation(root,mainId,contentId,"generated product");
                    }

                    for (int i = 0; i < myDataSourcesCbx.size(); i++) {
                        if (((JCheckBox) myDataSourcesCbx.get(
                                i)).isSelected()) {
                            String id = (String) myDataSourcesIds.get(i);
                            repositoryClient.addAssociation(root,id,contentId,"uses data");
                        }
                    }

                }

                if(zidvFile!=null && isZidv && uploadZidvDataCbx.isSelected()) {
                    ZipInputStream zin  = new ZipInputStream(new FileInputStream(new File(zidvFile)));
                    ZipEntry ze;
                    while ((ze = zin.getNextEntry())!=null) {
                        String entryName = ze.getName();
                        if(getIdv().getArgsManager().isBundleFile(entryName)) {
                            continue;
                        }
                        statusLbl.setText("Adding " + entryName);
                        zos.putNextEntry(new ZipEntry(entryName));
                        byte[] bytes =
                            IOUtil.readBytes(zin,null, false);
                        zos.write(bytes, 0, bytes.length);
                        zos.closeEntry();
                        String id = (cnt++) + "";
                        attrs = Misc.toList(new String[] {
                            ATTR_ID, id, ATTR_FILE,
                            entryName, ATTR_PARENT,
                            parentId, ATTR_TYPE, TYPE_FILE, ATTR_NAME,
                            entryName});
                        node = XmlUtil.create(TAG_ENTRY, root,attrs);
                    }
                }



                if ((addAssociationCbx != null)
                        && addAssociationCbx.isSelected()) {
                    repositoryClient.addAssociation(root,lastBundleId,contentId,"generated product");
                }


                String xml = XmlUtil.toString(root);
                //                System.out.println(xml);

                zos.putNextEntry(new ZipEntry("entries.xml"));
                byte[] bytes = xml.getBytes();
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
                zos.close();
                bos.close();


                List<HttpFormEntry> entries = new ArrayList<HttpFormEntry>();
                repositoryClient.addUrlArgs(entries);
                entries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                        bos.toByteArray()));
               

                statusLbl.setText("Posting to RAMADDA");
                String[] result =repositoryClient.doPost(repositoryClient.URL_ENTRY_XMLCREATE,  entries);
                dialog.dispose();
                if (result[0] != null) {
                    LogUtil.userErrorMessage("Error publishing:\n"
                                             + result[0]);
                    return;
                }
                //                System.out.println(result[1]);
                Element response = XmlUtil.getRoot(result[1]);

                if (repositoryClient.responseOk(response)) {
                    if (bundleFile != null) {
                        //TODO: get the id of the bundle created
                        Element firstResult = XmlUtil.findChild(response,
                                                  TAG_ENTRY);
                        if (firstResult != null) {
                            lastBundleId = XmlUtil.getAttribute(firstResult,
                                    ATTR_ID);
                            lastBundleFile = bundleFile;
                        }
                    }
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
     * @param attrs _more_
     * @param attr _more_
     * @param fld _more_
     */
    private void checkAndAdd(List attrs, String attr, JTextField fld) {
        String v = fld.getText().trim();
        if (v.length() > 0) {
            attrs.add(attr);
            attrs.add(v);
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
        publishContent(null, null);
    }



    /**
     *  Set the RepositoryClient property.
     *
     *  @param value The new value for RepositoryClient
     */
    public void setRepositoryClient(InteractiveRepositoryClient value) {
        repositoryClient = value;
    }

    /**
     *  Get the RepositoryClient property.
     *
     *  @return The RepositoryClient
     */
    public InteractiveRepositoryClient getRepositoryClient() {
        return repositoryClient;
    }






}

