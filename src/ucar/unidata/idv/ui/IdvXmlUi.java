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

package ucar.unidata.idv.ui;


import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.FineLineBorder;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.MemoryMonitor;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.Constructor;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.help.*;

import javax.swing.*;
import javax.swing.border.*;


/**
 * This class extends the {@link ucar.unidata.ui.XmlUi}
 * class to provide handling of special xml tags that define
 * certain components. The XmlUi class processes an xml
 * file that specifies a UI  and creates the user interface.
 * <p>
 * This class  handles the tags &quot;datatree&quot;
 * and &quot;messagelogger&quot;, though in the future
 * it could handle the creation of view managers, legends, etc.
 * ew
 * @author Idv Development Team
 */

public class IdvXmlUi extends XmlUi {

    /** xml attribute */
    private static final String ATTR_USEPREF = "usepref";

    /** Gets bumped up every time we create a gui */
    private static int version = 0;

    /** References to the idv */
    IntegratedDataViewer idv;

    /** List of ViewManager-s being shown in the gui */
    private List viewManagers = new ArrayList();

    /** List of view managers (may be empty) that we can use instead of creating new ones */
    private List viewManagersToUse;

    /** The window the gui is shown in */
    IdvWindow window;


    /** List of menory monitors */
    private List memoryMonitors = new ArrayList();


    /**
     * Construct this object
     *
     *
     * @param idv The IDV
     * @param root The root of the xml tree that defines the UI
     */
    public IdvXmlUi(IntegratedDataViewer idv, Element root) {
        super(root, idv);
        viewManagersToUse = new ArrayList();
        this.idv          = idv;
        properties.put("versionuid", Misc.getUniqueId());
        properties.put("version", "" + (version++));
    }


    /**
     * Construct this object
     *
     *
     * @param window The window the gui is shown in
     * @param viewManagers The ViewManagers that are being shown
     * @param idv The IDV
     * @param root The root of the xml tree that defines the UI
     */
    public IdvXmlUi(IdvWindow window, List viewManagers,
                    IntegratedDataViewer idv, Element root) {
        this(window, viewManagers, idv, root, null);
    }

    /**
     * _more_
     *
     * @param window _more_
     * @param viewManagers _more_
     * @param idv _more_
     * @param root _more_
     * @param startNode _more_
     */
    public IdvXmlUi(IdvWindow window, List viewManagers,
                    IntegratedDataViewer idv, Element root,
                    Element startNode) {
        super(root, startNode, null, idv, null);
        this.window = window;
        if (viewManagers != null) {
            this.viewManagers.addAll(viewManagers);
            this.viewManagersToUse = new ArrayList(viewManagers);
        }
        if (window != null) {
            window.setXmlUI(this);
        }
        this.idv = idv;
        properties.put("versionuid", Misc.getUniqueId());
        properties.put("version", "" + (version++));
    }


    /**
     *  Add in the appropriate state to the given toolbar xml
     *
     * @param root Root of the toolbar xml
     * @param uiManager the ui manager
     */
    protected static void processToolbarXml(Element root,
                                            IdvUIManager uiManager) {
        root.removeAttribute(ATTR_ID);
        NodeList elements = XmlUtil.getElements(root);
        Object iconSize =
            uiManager.getIdv().getStateManager().getPreferenceOrProperty(
                "idv.ui.iconsize");
        if ((iconSize != null)
                && (iconSize.toString().trim().length() == 0)) {
            iconSize = null;
        }
        if (iconSize == null) {
            iconSize =
                uiManager.getIdv().getStateManager().getPreferenceOrProperty(
                    "idv.ui.minimumiconsize");
        }
        if ((iconSize != null)
                && (iconSize.toString().trim().length() == 0)) {
            iconSize = null;
        }
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            child.removeAttribute(ATTR_ID);
            if ( !child.getTagName().equals(TAG_BUTTON)) {
                continue;
            }
            String action = XmlUtil.getAttribute(child, ATTR_ACTION, "");
            String desc   = uiManager.getActionDescription(action);
            if (XmlUtil.getAttribute(child, ATTR_TOOLTIP, (String) null)
                    == null) {
                if (desc != null) {
                    child.setAttribute(ATTR_TOOLTIP, desc);
                }
            }
            boolean haveImage = true;
            if (XmlUtil.getAttribute(child, ATTR_IMAGE, (String) null)
                    == null) {
                String image = uiManager.getActionImage(action);
                if (image != null) {
                    if (iconSize != null) {
                        if (image.indexOf("16") >= 0) {
                            String tmp = StringUtil.replace(image, "16",
                                             iconSize.toString());
                            if (GuiUtils.getImage(tmp, IdvXmlUi.class, true,
                                    true) != null) {
                                image = tmp;
                            } else {
                                child.setAttribute("imagewidth",
                                        iconSize.toString());
                            }
                        } else {
                            //                            System.err.println ("No dimension in name:" + image);
                        }
                    }
                    child.setAttribute(ATTR_IMAGE, image);
                } else {
                    haveImage = false;
                    if (XmlUtil.getAttribute(child, ATTR_LABEL,
                                             (String) null) == null) {
                        child.setAttribute(ATTR_LABEL, desc);
                    }
                }
            }

            if (haveImage) {
                child.setAttribute(ATTR_SPACE, "2");
                child.setAttribute(
                    ATTR_MOUSE_ENTER,
                    "ui.setText(idv.messagelabel,prop:tooltip);ui.setBorder(this,etched);");
                child.setAttribute(
                    ATTR_MOUSE_EXIT,
                    "ui.setText(idv.messagelabel,);ui.setBorder(this,empty);");
                child.setAttribute(ATTR_BORDER, "empty");
            } else {
                child.setAttribute(ATTR_SPACE, "0");
                child.setAttribute(ATTR_BORDER, "empty");
            }

            //            child.setAttribute(ATTR_LABEL, "LABEL");
            //            child.setAttribute(ATTR_VPOSITION, "bottom");
            //            child.setAttribute(ATTR_HPOSITION, "left");
            if (XmlUtil.getAttribute(child, ATTR_CLASS, (String) null)
                    == null) {
                if (haveImage) {
                    child.setAttribute(ATTR_CLASS, "iconbtn");
                } else {
                    child.setAttribute(ATTR_CLASS, "textbtn");
                }
            }
        }
    }

    /**
     * The destructor
     */
    public void dispose() {
        for (int i = 0; i < memoryMonitors.size(); i++) {
            ((MemoryMonitor) memoryMonitors.get(i)).stop();
        }
        memoryMonitors    = null;
        viewManagers      = null;
        viewManagersToUse = null;
        window            = null;
        super.dispose();
    }

    /** _more_ */
    int componentCnt = 0;


    /**
     * Override the base class factory method to create
     * idv specific components.
     *
     * @param node The node th e xml ui tree
     * @param id The id of the node
     * @return The Component that represents the  given node
     */
    public Component createComponent(Element node, String id) {

        String         tagName = node.getTagName();

        QuicklinkPanel editor;
        if (tagName.equals("idv.quicklinks")) {
            if (XmlUtil.getAttribute(node, ATTR_USEPREF, false)) {
                if ( !idv.getIdvUIManager().embedQuickLinksInDashboard()) {
                    return null;
                }
            }
            return QuicklinkPanel.createQuicklinksFromResources(idv);
        }

        if (tagName.equals("idv.quicklinks.favorites")) {
            editor = new QuicklinkPanel.Bundle(idv, "Favorite Bundles",
                    IdvPersistenceManager.BUNDLES_FAVORITES);
            editor.doUpdate();
            return editor.getContents();
        }

        if (tagName.equals("idv.quicklinks.datasources")) {
            editor = new QuicklinkPanel.Bundle(idv, "Data Favorites",
                    IdvPersistenceManager.BUNDLES_DATA);
            editor.doUpdate();
            return editor.getContents();
        }

        if (tagName.equals("idv.quicklinks.displaytemplates")) {
            editor = new QuicklinkPanel.Bundle(idv, "Display Templates",
                    IdvPersistenceManager.BUNDLES_DISPLAY);
            editor.doUpdate();
            return editor.getContents();
        }

        if (tagName.equals("idv.quicklinks.history")) {
            editor = new QuicklinkPanel.FileHistory(idv, "History");
            editor.doUpdate();
            return editor.getContents();
        }


        if (tagName.equals("idv.quicklinks.special")) {
            editor = new QuicklinkPanel.Control(idv, "Special Displays");
            editor.doUpdate();
            return editor.getContents();
        }

        if (tagName.equals("idv.quicklinks.windows")) {
            editor =
                new QuicklinkPanel.Html(idv, "New Window",
                                        "Create New Window",
                                        idv.getIdvUIManager().getSkinHtml());
            editor.doUpdate();
            return editor.getContents();
        }

        if (tagName.equals(IdvUIManager.COMP_HELP)) {
            try {
                java.net.URL url = IOUtil.getURL(XmlUtil.getAttribute(node,
                                       "helpset",
                                       idv.getStateManager().getHelpRoot()
                                       + "/HelpSet.hs"), getClass());
                if (url == null) {
                    return null;
                }
                return new IdvHelp(new HelpSet(getClass().getClassLoader(),
                        url));
            } catch (Exception exc) {
                LogUtil.logException("Error loading help", exc);
                return null;
            }
        }

        if (tagName.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
            String key = XmlUtil.getAttribute(node, ATTR_ID,
                             "" + componentCnt);
            componentCnt++;
            ComponentGroup compGroup = (ComponentGroup) ((window != null)
                    ? window.getPersistentComponent(key)
                    : null);

            if (compGroup == null) {
                compGroup = makeComponentGroup(node);
                compGroup.setShowHeader(XmlUtil.getAttribute(node,
                        "showheader", true));
                if (window != null) {
                    window.putPersistentComponent(key, compGroup);
                }
            } else {}
            return compGroup.getContents();
        }


        if (tagName.equals(IdvUIManager.COMP_CHOOSERS)) {
            if (XmlUtil.getAttribute(node, ATTR_USEPREF, false)) {
                if ( !idv.getIdvUIManager().embedDataChooserInDashboard()) {
                    return null;
                }
            }
            boolean inTabs = XmlUtil.getAttribute(
                                 node, "intabs",
                                 !idv.getProperty(
                                     IdvChooserManager.PROP_CHOOSER_TREEVIEW,
                                     false));
            List choosers = new ArrayList();
            Component comp =
                idv.getIdvChooserManager().createChoosers(inTabs, choosers,
                    node);
            for (int i = 0; i < choosers.size(); i++) {
                if (window != null) {
                    window.addToGroup(IdvWindow.GROUP_CHOOSERS,
                                      (Component) choosers.get(i));
                }
            }
            return comp;
        }


        if (tagName.equals(IdvUIManager.COMP_CHOOSER)) {
            if (idv.getPreferenceManager().shouldShowChooser(
                    IdvChooserManager.getChooserId(node))) {
                Component comp =
                    idv.getIdvChooserManager().createChooser(node);
                if ((comp != null) && (window != null)) {
                    window.addToGroup(IdvWindow.GROUP_CHOOSERS, comp);
                }
                return comp;
            }
            return new JPanel();
        }


        if (tagName.equals(IdvUIManager.COMP_VIEWPANEL)) {
            if (XmlUtil.getAttribute(node, ATTR_USEPREF, false)) {
                if ( !idv.getIdvUIManager().getShowControlsInTab()) {
                    return null;
                }
            }
            return idv.getIdvUIManager().getViewPanel().getContents();
        }

        if (tagName.equals(IdvUIManager.COMP_TOOLBAR)) {
            JComponent toolbar = idv.getIdvUIManager().doMakeToolbar();
            toolbar = GuiUtils.center(toolbar);
            if (window != null) {
                window.addToGroup(IdvWindow.GROUP_TOOLBARS, toolbar);
            }
            return toolbar;
        }
        if (tagName.equals(IdvUIManager.COMP_MENUBAR)) {
            if (GuiUtils.doMacMenubar()) {
                return GuiUtils.filler();
            }
            return idv.getIdvUIManager().doMakeMenuBar(window);
        }

        if (tagName.equals(IdvUIManager.COMP_MEMORYMONITOR)) {
            MemoryMonitor monitor =
                new MemoryMonitor(
                    80,
                    new Boolean(
                        idv.getStateManager().getPreferenceOrProperty(
                            IdvConstants.PROP_SHOWCLOCK,
                            "true")).booleanValue());
            memoryMonitors.add(monitor);
            return monitor;
        }

        if (tagName.equals(IdvUIManager.COMP_STATUSBAR)) {
            return idv.getIdvUIManager().doMakeStatusBar(window);
        }

        if (tagName.equals(IdvUIManager.COMP_DATASELECTOR)) {
            if (XmlUtil.getAttribute(node, ATTR_USEPREF, false)) {
                if ( !idv.getIdvUIManager().embedFieldSelectorInDashboard()) {
                    return null;
                }
            }
            return idv.getIdvUIManager().createDataSelector(false,
                    false).getContents();
        }

        if (tagName.equals(IdvUIManager.COMP_PROGRESSBAR)) {
            return new RovingProgress(true);
        }

        if (tagName.equals(IdvUIManager.COMP_DATATREE)) {
            return idv.getIdvUIManager().createDataTree(false).getScroller();
        }
        if (tagName.equals(IdvUIManager.COMP_MESSAGELOGGER)) {
            JTextArea messageLog = new JTextArea(3, 40);
            messageLog.setEditable(false);
            LogUtil.addMessageLogger(messageLog);
            return messageLog;
        }

        if (tagName.equals(IdvUIManager.COMP_WAITLABEL)) {
            JLabel waitLabel = new JLabel(IdvWindow.getNormalIcon());
            addComponent(IdvUIManager.COMP_WAITLABEL, waitLabel);
            return waitLabel;
        }


        if (tagName.equals(IdvUIManager.COMP_PROGRESSBAR)) {
            return idv.getIdvUIManager().doMakeRovingProgressBar();
        }

        if (tagName.equals(IdvUIManager.COMP_MESSAGELABEL)) {
            JLabel msgLabel = new JLabel(" ");
            LogUtil.addMessageLogger(msgLabel);
            return msgLabel;
        }

        if (tagName.equals(IdvUIManager.COMP_MAPVIEW)
                || tagName.equals(IdvUIManager.COMP_VIEW)) {
            ViewManager viewManager = null;
            if ((viewManagersToUse != null)
                    && (viewManagersToUse.size() > 0)) {
                viewManager = (ViewManager) viewManagersToUse.get(0);
                viewManagersToUse.remove(0);
            }

            if (viewManager == null) {
                viewManager = getViewManager(node);
            }

            if (viewManager == null) {
                return new JLabel("Error creating view manager");
            }
            return viewManager.getContents();
        }
        return super.createComponent(node, id);
    }



    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    protected IdvComponentGroup makeComponentGroup(Element node) {
        IdvComponentGroup compGroup = new IdvComponentGroup(idv, "");
        compGroup.initWith(node);

        NodeList elements = XmlUtil.getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child        = (Element) elements.item(i);
            String  childTagName = child.getTagName();
            if (childTagName.equals(IdvUIManager.COMP_MAPVIEW)
                    || childTagName.equals(IdvUIManager.COMP_VIEW)) {
                ViewManager viewManager = getViewManager(child);
                compGroup.addComponent(new IdvComponentHolder(idv,
                        viewManager));
            } else if (childTagName.equals(
                    IdvUIManager.COMP_COMPONENT_CHOOSERS)) {
                IdvComponentHolder comp = new IdvComponentHolder(idv,
                                              "choosers");
                comp.setType(comp.TYPE_CHOOSERS);
                comp.setName(XmlUtil.getAttribute(child, "name", "Choosers"));
                compGroup.addComponent(comp);
            } else if (childTagName.equals(
                    IdvUIManager.COMP_COMPONENT_SKIN)) {
                IdvComponentHolder comp = new IdvComponentHolder(idv,
                                              XmlUtil.getAttribute(child,
                                                  "url"));
                comp.setType(comp.TYPE_SKIN);
                comp.setName(XmlUtil.getAttribute(child, "name", "UI"));
                compGroup.addComponent(comp);
            } else if (childTagName.equals(
                    IdvUIManager.COMP_COMPONENT_HTML)) {
                String text = XmlUtil.getChildText(child);
                text = new String(XmlUtil.decodeBase64(text.trim()));
                ComponentHolder comp = new HtmlComponent("Html Text", text);
                comp.setShowHeader(false);
                comp.setName(XmlUtil.getAttribute(child, "name", "HTML"));
                compGroup.addComponent(comp);
            } else if (childTagName.equals(
                    IdvUIManager.COMP_COMPONENT_GROUP)) {
                IdvComponentGroup childCompGroup = makeComponentGroup(child);
                compGroup.addComponent(childCompGroup);
            } else if (childTagName.equals(IdvUIManager.COMP_DATASELECTOR)) {
                compGroup.addComponent(new IdvComponentHolder(idv,
                        idv.getIdvUIManager().createDataSelector(false,
                            false)));
            } else {
                System.err.println("Unknwon component element:"
                                   + XmlUtil.toString(child));
            }
        }
        return compGroup;
    }


    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    protected ViewManager getViewManager(Element node) {
        String bundleText = XmlUtil.getChildText(node);
        if ((bundleText != null) && (bundleText.trim().length() > 0)) {
            try {
                bundleText =
                    new String(XmlUtil.decodeBase64(bundleText.trim()));
                return (ViewManager) idv.decodeObject(bundleText);
            } catch (Exception exc) {
                throw new WrapperException(exc);
            }
        }

        String properties = idv.getViewManagerProperties();
        if (properties == null) {
            properties = "";
        }
        String xmlProperties = getAttr(node, "properties", (String) null);
        if (xmlProperties != null) {
            properties += ";" + xmlProperties;
        }
        String         viewId         = getAttr(node, "viewid",
                                            (String) null);
        ViewDescriptor viewDescriptor = null;
        if (viewId != null) {
            viewDescriptor = new ViewDescriptor(viewId);
        }




        ViewManager viewManager = null;
        String      className   = getAttr(node, "class", (String) null);
        //Handle any problems from the change over  to property based skins
        if ((className != null) && className.startsWith("${")) {
            className = null;
        }

        if (className != null) {
            try {
                if (viewDescriptor == null) {
                    viewDescriptor = new ViewDescriptor();
                }
                Class vmClass = Misc.findClass(className);
                Constructor ctor = Misc.findConstructor(vmClass,
                                       new Class[] {
                                           IntegratedDataViewer.class,
                                           ViewDescriptor.class,
                                           String.class });
                if (ctor == null) {
                    System.err.println("Could not find ctor for:"
                                       + vmClass.getName());
                } else {
                    viewManager =
                        (ViewManager) ctor.newInstance(new Object[] { idv,
                            viewDescriptor, properties });
                    viewManager.initFromSkin(node);
                    idv.getVMManager().addViewManager(viewManager);
                    viewManagers.add(viewManager);
                }
            } catch (Exception exc) {
                LogUtil.logException("", exc);
                System.err.println("Error creating class:" + className
                                   + " exception: " + exc);
                exc.printStackTrace();
            }
        }



        if (viewManager == null) {
            viewManager =
                idv.getVMManager().createViewManager(viewDescriptor,
                    properties);
            if (viewManager == null) {
                return null;
            }
            viewManagers.add(viewManager);
        } else {
            //We don't want to apply the properties to
            //an unpersisted vm, since it has its own state
            //viewManager.parseProperties(properties);
        }
        return viewManager;
    }

    /**
     * Override base class method to provide the idv.status border
     *
     * @param type Border type
     * @param node Component xml node
     *
     * @return The border
     */
    public Border getBorder(String type, Element node) {
        if (type.equals("idv.status")) {
            Border outside = new FineLineBorder(BevelBorder.LOWERED);
            Border inside  = BorderFactory.createEmptyBorder(0, 2, 0, 2);
            return BorderFactory.createCompoundBorder(outside, inside);
        }
        return super.getBorder(type, node);
    }

    /**
     * Get the view managers created
     *
     * @return The created view managers
     */
    public List getViewManagers() {
        return viewManagers;
    }



}
