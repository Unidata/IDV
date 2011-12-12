/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;




import ucar.unidata.idv.*;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.ui.DatasetUI;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.WmsUtil;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;




import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;



/**
 * This handles a variety of flavors of xml documents (e.g., thredds
 * query capability, thredds catalogs, idv menus) to create data
 * choosers from. It provides a combobox to enter urls to xml
 * documents. It retrieves the xml and creates a {@link XmlHandler}
 * based on the type of xml. Currently this class handles two
 * types of xml: Thredds catalog and Web Map Server (WMS)
 * capability documents. The XmlHandler does most of the work.
 * <p>
 * This class maintains the different xml docs the user has gone
 * to coupled with the XmlHandler for each doc. It uses this list
 * to support navigating back and forth through the history of
 * documents.
 *
 * @author IDV development team
 * @version $Revision: 1.80 $Date: 2007/07/09 22:59:58 $
 */


public class XmlChooser extends IdvChooser implements ActionListener {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(XmlChooser.class.getName());


    /** _more_          */
    public static final String PROP_CHOOSER_URL = "idv.chooser.url";

    /**
     * If there was some error in loading the xml
     * we construct and xml document with the error tag
     * and popup the message in the GUI
     */
    public static final String TAG_ERROR = "error";

    /**
     * The xml attr name for any initial url from the
     * choosers.xml segment that created this chooser
     */
    public static final String ATTR_URL = "url";


    /** Action command for choosing a file */
    private static final String CMD_BROWSE = "cmd.browse";

    /** Keeps track of the outstanding url request */
    protected int timestamp = 0;


    /**
     * List of {@link XmlHandler}s that we have create
     * We keep this around so we can go back and forth
     * in the list
     */
    private List handlers = new ArrayList();

    /** The document we created the xml from */
    private Document document;

    /** The xml */
    private String xmlContents;

    /** The current index into the handlers list */
    private int historyIdx = -1;


    /** Back history button */
    private JButton backBtn;

    /** Forward history button */
    private JButton fwdBtn;

    /**
     * A {@link ucar.unidata.util.PreferenceList}
     * that manages the url list. Saving new entries as  a
     * user property, etc.
     */
    private PreferenceList urlListHandler;

    /** Combobox of urls. We get this from the urlListHandler */
    private JComboBox urlBox;

    /** A flag to know when to ignore urlBox selection events */
    private boolean okToDoUrlListEvents = true;

    /** Holds the current XmlHandler GUI */
    private JPanel handlerHolder;

    /** The main gui contents */
    private JPanel myContents;

    /** The first url we have */
    private String initialUrlPath;

    /** The data selector we use */
    private DataSelector dataSelector;


    /**
     * Create the <code>XmlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public XmlChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        initialUrlPath = ((chooserNode != null)
                          ? XmlUtil.getAttribute(chooserNode, ATTR_URL, "")
                          : "");
    }


    /**
     * _more_
     *
     * @param dataSource _more_
     */
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
        String tmp = (String) dataSource.getProperty(PROP_CHOOSER_URL);
        if (tmp != null) {
            initialUrlPath = tmp;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private boolean haveHandler() {
        return ((historyIdx >= 0) && (historyIdx < handlers.size()));
    }

    /**
     * _more_
     */
    protected void updateStatus() {
        if (haveHandler()) {
            XmlHandler handler = (XmlHandler) handlers.get(historyIdx);
            handler.updateStatus();
        } else {
            setStatus("");
        }
    }

    /**
     * _more_
     *
     * @param have _more_
     */
    public void setHaveData(boolean have) {
        super.setHaveData(have);
        updateStatus();
    }




    /**
     *  Overwrite  base class method to do the update on first display.
     *
     * @return Return true. We do want to do an update on initial display.
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return true;
    }

    /**
     *  Handle any Gui actions.
     *
     *  @param ae The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_BROWSE)) {
            String filename = FileManager.getReadFile(FILTER_XML);
            if (filename == null) {
                return;
            }
            urlBox.setSelectedItem(filename);
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            doLoad();
        } else {
            //Here, the base class ChooserPanel will check if this command
            //is the load or cancel command. 
            super.actionPerformed(ae);
        }
    }

    /**
     * _more_
     *
     * @param properties _more_
     */
    public void initSubProperties(Hashtable properties) {
        properties.put(PROP_CHOOSERCLASSNAME, getClass().getName());
        properties.put(PROP_CHOOSER_URL, urlBox.getSelectedItem());
    }


    /**
     * _more_
     *
     * @param definingObject _more_
     * @param dataType _more_
     * @param properties _more_
     *
     * @return _more_
     */
    protected boolean makeDataSource(Object definingObject, String dataType,
                                     Hashtable properties) {
        properties.put(PROP_CHOOSER_URL, urlBox.getSelectedItem());
        return super.makeDataSource(definingObject, dataType, properties);
    }


    /**
     *  Create and return the Gui contents.
     *
     *  @return The gui contents.
     */
    protected JComponent doMakeContents() {
        //        dataSelector = new DataSelector(getIdv(), new Dimension(400, 200),
        //                                        true);

        //Get the list of catalogs but remove the old catalog.xml entry
        urlListHandler = getPreferenceList(PREF_CATALOGLIST);
        final XmlChooser xmlChooser      = this;
        ActionListener   catListListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if ( !okToDoUrlListEvents) {
                    return;
                }
                xmlChooser.actionPerformed(ae);
            }
        };
        urlBox = urlListHandler.createComboBox(GuiUtils.CMD_UPDATE,
                catListListener, true);


        GuiUtils.setPreferredWidth(urlBox, 200);

        // top panel
        JButton browseButton = new JButton("Select File...");
        browseButton.setToolTipText("Choose a catalog from disk");
        browseButton.setActionCommand(CMD_BROWSE);
        browseButton.addActionListener(this);

        GuiUtils.setHFill();
        JPanel catListPanel = GuiUtils.doLayout(new Component[] { urlBox },
                                  1, GuiUtils.WT_Y, GuiUtils.WT_N);

        backBtn = GuiUtils.getImageButton(
            GuiUtils.getImageIcon(
                "/auxdata/ui/icons/Left16.gif", getClass()));
        backBtn.setToolTipText("View previous selection");
        GuiUtils.makeMouseOverBorder(backBtn);
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                goBack();
            }
        });

        fwdBtn = GuiUtils.getImageButton(
            GuiUtils.getImageIcon(
                "/auxdata/ui/icons/Right16.gif", getClass()));
        GuiUtils.makeMouseOverBorder(fwdBtn);
        fwdBtn.setToolTipText("View next selection");
        fwdBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                goForward();
            }
        });

        checkButtons();
        JComponent bottomButtons = getDefaultButtons();
        handlerHolder = new JPanel();
        handlerHolder.setLayout(new BorderLayout());
        //        JPanel tmp = new JPanel();
        //        tmp.setPreferredSize(new Dimension(200,500));
        //        handlerHolder.add(tmp, BorderLayout.CENTER);



        if (getIdv().getArgsManager().getInitCatalogs().size() > 0) {
            initialUrlPath =
                (String) getIdv().getArgsManager().getInitCatalogs().get(0);
            urlBox.setSelectedItem(initialUrlPath);
        } else {
            if ((initialUrlPath != null) && (initialUrlPath.length() > 0)) {
                makeUiFromPath(initialUrlPath);
            } else {
                makeBlankTree();
            }
        }
        JPanel navButtons = GuiUtils.hbox(backBtn, fwdBtn);

        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel catPanel = GuiUtils.doLayout(new Component[] {
                              new JLabel("Catalogs:"),
                              catListPanel, browseButton }, 3,
                                  GuiUtils.WT_NYN, GuiUtils.WT_N);
        JPanel topPanel = GuiUtils.leftCenter(navButtons, catPanel);
        myContents = GuiUtils.topCenterBottom(topPanel, handlerHolder,
                bottomButtons);
        //        myContents = GuiUtils.topCenter(getStatusComponent(), myContents);
        return myContents;
    }





    /**
     *  Load the xml defined by the given xmlPath. Try to create a display from it.
     *
     *  @param xmlPath The url pointing to the xml to display.
     *  @return Was this successful
     */
    public boolean makeUiFromPath(String xmlPath) {
        return makeUiFromPath(xmlPath, ++timestamp);
    }


    /**
     *  Load the xml defined by the given xmlPath. Try to create a display from it.
     *
     *  @param xmlPath The url pointing to the xml to display.
     *  @param myTimestamp Keeps track of which request this is.
     *  @return Was this successful
     */
    public boolean makeUiFromPath(String xmlPath, int myTimestamp) {
        boolean ok = true;
        try {
            if (xmlPath.length() > 0) {
                showWaitCursor();
                xmlContents = IOUtil.readContents(xmlPath, NULL_STRING);
                showNormalCursor();
                if (myTimestamp != timestamp) {
                    return false;
                }
                if (xmlContents == null) {
                    //Clear out the tree 
                    xmlContents = XmlUtil.tag(TAG_ERROR,
                            XmlUtil.attr("label",
                                         "Could not load url: " + xmlPath));
                    ok = false;
                }
                //Check if its xml
                if (xmlContents.indexOf("<") >= 0) {
                    document = XmlUtil.getDocument(xmlContents);
                }

                //If we failed to make an xml document then try to tack on the wms
                //capabilities request in case this is a wms url without one
                if ((document == null)
                        || (document.getDocumentElement() == null)) {
                    if (xmlPath.indexOf("?") < 0) {
                        xmlPath = xmlPath
                                  + "?request=GetCapabilities&service=WMS";
                    } else {
                        xmlPath = xmlPath
                                  + "&request=GetCapabilities&service=WMS";
                    }
                    xmlContents = IOUtil.readContents(xmlPath, NULL_STRING);
                    document    = XmlUtil.getDocument(xmlContents);
                }

                if ((document == null)
                        || (document.getDocumentElement() == null)) {
                    throw new IllegalArgumentException(
                        "Could not process XML from:" + xmlPath);
                }
                makeUi(document, document.getDocumentElement(), xmlPath);
            } else {
                makeUi(null, null, xmlPath);
            }
        } catch (Exception exc) {
            if (myTimestamp != timestamp) {
                return false;
            }
            logException("Creating ui:" + xmlPath, exc);
            return false;
        }
        return ok;
    }


    /**
     *  Generate a user interface from the given xml document (derived from the given path).
     *  The xml can be a thredds query capability, any verion of a thredds catalog or
     *  an IDV menus xml file.
     *
     *
     * @param doc the xml document
     *  @param xmlRoot The root of the xml document to create a display for.
     *  @param path The url path we got the xml from.
     */
    protected void makeUi(Document doc, Element xmlRoot, String path) {
        this.document = doc;
        setHaveData(false);
        if (xmlRoot == null) {
            return;
        }
        setSelected(path);
        XmlHandler handler = null;
        String     tagName = XmlUtil.getLocalName(xmlRoot);


        if (tagName.equals(WmsUtil.TAG_WMS1)
                || tagName.equals(WmsUtil.TAG_WMS2)) {
            handler = new WmsHandler(this, xmlRoot, path);
        } else if (tagName.equals(TAG_ERROR)) {
            final String error = XmlUtil.getAttribute(xmlRoot, "label",
                                     "Error");
            LogUtil.userErrorMessage("Error: " + error);
            return;
        } else if (tagName.equals(CatalogUtil.TAG_CATALOG)) {
            handler = new ThreddsHandler(this, xmlRoot, path);
        } else if (tagName.equals("menus")) {
            handler = new MenuHandler(this, xmlRoot, path);
        } else {
            throw new IllegalArgumentException("Unknown xml:"
                    + ((xmlContents.length() > 100)
                       ? xmlContents.substring(0, 100)
                       : xmlContents) + " ...");
        }

        JComponent contents = handler.getContents();
        contents.setPreferredSize(new Dimension(200, 250));
        addToContents(contents);
        addToHistory(handler);
        updateStatus();
    }



    /**
     *  Set the catalog list combobox to the given xmlPath.
     *
     *  @param xmlPath The xmlPath to show in the combo box.
     */
    private void setSelected(String xmlPath) {
        okToDoUrlListEvents = false;
        urlBox.setSelectedItem(xmlPath);
        okToDoUrlListEvents = true;
    }

    /**
     *  Display the document defined in the history list by the current historyIdx.
     */
    private void go() {
        if (haveHandler()) {
            XmlHandler handler = (XmlHandler) handlers.get(historyIdx);
            setSelected(handler.getPath());
            addToContents(handler.getContents());
            checkButtons();
        }
    }


    /**
     *  Go back and display  the previous  document.
     */
    public void goBack() {
        historyIdx--;
        if (historyIdx < 0) {
            historyIdx = 0;
        }
        go();
    }

    /**
     *  Go forward and display  the next   document in the history list.
     */
    public void goForward() {
        historyIdx++;
        if (historyIdx >= handlers.size()) {
            historyIdx = handlers.size() - 1;
        }
        go();
    }


    /**
     *  Disable or enable the forward/back buttons.
     */
    private void checkButtons() {
        fwdBtn.setEnabled(historyIdx < handlers.size() - 1);
        backBtn.setEnabled(historyIdx > 0);
    }



    /**
     *  A holder of a String action and a properties table.
     */
    public static class PropertiedAction {

        /** The action */
        String action;

        /** The properties */
        Hashtable properties;

        /**
         * Create me with the given actio
         *
         * @param action The action
         *
         */
        public PropertiedAction(String action) {
            this(action, new Hashtable());
        }

        /**
         * Create me with the given action and properties
         *
         * @param action The actio
         * @param properties The properties
         */
        public PropertiedAction(String action, Hashtable properties) {
            this.action     = action;
            this.properties = properties;
        }
    }


    /**
     *  A wrapper around @see{handleAction}, passing in an empty properties table.
     *
     *  @param action The String action (Url, Idv command, etc.) to handle.
     */
    protected void handleAction(String action) {
        handleAction(action, new Hashtable());
    }


    /**
     *  Process the given action (e.g., url, idv command) with the given properties.
     *  This just creates  a new PropertiedAction, adds it to a list and turns around
     *  and calls the handleActions method.
     *
     *  @param action The String action (Url, Idv command, etc.) to handle.
     *  @param properties The properties for this action.
     */
    protected void handleAction(String action, Hashtable properties) {
        handleActions(Misc.newList(new PropertiedAction(action, properties)));
    }



    /**
     *  Process the given list of {@link PropertiedAction}s.
     *
     * @param actions The list of actions to process
     */
    protected void handleActions(final List actions) {
        //Run the call in another thread. For now use the ChooserRunnable. This
        //really does nothing but is a hook for when we have  cancel 
        //load, etc, functionality
        Misc.run(new ChooserRunnable(this) {
            public void run() {
                showWaitCursor();
                try {
                    handleActionsInThread(actions);
                } catch (Exception exc) {
                    logException("Creating data source", exc);
                }
                showNormalCursor();
                if (getCanceled()) {}
            }
        });
    }

    /**
     * Actually does the work of handling the actions
     *
     * @param actions List of <code>PropertiedAction</code>s
     */
    protected void handleActionsInThread(List actions) {
        boolean didone        = false;
        String  invalidSource = null;
        for (int i = 0; i < actions.size(); i++) {
            PropertiedAction action = (PropertiedAction) actions.get(i);
            boolean isValidAction = idv.handleAction(action.action,
                                        action.properties);
            if (isValidAction) {
                didone = true;
            } else {
                if (invalidSource == null) {
                    invalidSource = action.action;
                }
            }
        }

        if (didone) {
            closeChooser();
        } else {
            //If we did not do any and if there was one path that was an invalid data source
            //then try to build the gui with it.
            if (invalidSource != null) {
                if ( !invalidSource.endsWith(".xml")) {
                    //              LogUtil.userMessage ("Unknown url:" + invalidSource);
                } else {
                    makeUiFromPath(invalidSource);
                }
            }
        }
    }


    /**
     *  Reload  the current xml and update the display.
     */
    public void doUpdate() {
        if (okToDoUrlListEvents) {
            Misc.run(this, "doUpdateInner");
        }
    }

    /**
     *  Reload  the current xml and update the display.
     */
    public void doUpdateInner() {
        String selected = urlBox.getSelectedItem().toString().trim();
        //Only save off the list on a successful load
        if (selected.length() == 0) {
            if (handlers.size() > 0) {
                goBack();
            } else {
                makeBlankTree();
            }
            return;
        }
        if (makeUiFromPath(selected)) {
            urlListHandler.saveState(urlBox);
        }
    }

    /**
     *  Load the currently selected xml element.
     */
    public void doLoadInThread() {
        showWaitCursor();
        try {

            Object handler = handlers.get(historyIdx);
            if (handler instanceof ThreddsHandler) {
                ((ThreddsHandler) handler).doLoad();
            } else if (handler instanceof WmsHandler) {
                ((WmsHandler) handler).doLoad();
            } else if (handler instanceof DatasetUI) {
                String action = ((DatasetUI) handler).processActionTemplate();
                if (action == null) {
                    return;
                }
                handleAction(action);
            } else if (handler instanceof XmlTree) {
                //              processElement (((XmlTree)object).getSelectedElement ());
            }
        } catch (Exception exc) {
            logException("Loading data", exc);
        }
        showNormalCursor();
    }




    /**
     *  Insert a new ui component into the panel.
     *
     *  @param handler The handler associated with this component.
     */
    private void addToHistory(XmlHandler handler) {
        int howManyToRemove = handlers.size() - historyIdx - 1;
        for (int cnt = 0; cnt < howManyToRemove; cnt++) {
            Misc.removeLast(handlers);
        }
        handlers.add(handler);
        historyIdx = handlers.size() - 1;
        checkButtons();
    }




    /**
     * Just creates an empty XmlTree
     */
    private void makeBlankTree() {
        XmlTree blankTree = new XmlTree(null, true, "");

        addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                blankTree.getScroller()), 5));
    }


    /**
     *  Remove the currently display gui and insert the given one.
     *
     *  @param comp The new gui.
     */
    private void addToContents(JComponent comp) {
        handlerHolder.removeAll();
        comp.setPreferredSize(new Dimension(200, 300));
        handlerHolder.add(comp, BorderLayout.CENTER);
        if (myContents != null) {
            myContents.invalidate();
            myContents.validate();
            myContents.repaint();
        }
    }


    /**
     * Get the xml
     *
     * @return The xml
     */
    public String getXml() {
        return xmlContents;
    }

    /**
     * Get the xml doc
     *
     * @return The xml doc
     */
    public Document getDocument() {
        return document;
    }

}
