/*
 * $Id: IdvXmlUi.java,v 1.54 2007/08/16 14:08:22 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
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


import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.idv.*;

import ucar.unidata.idv.control.*;

import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.util.FileManager;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 */

public class IdvComponentGroup extends ComponentGroup {

    /** _more_ */
    IntegratedDataViewer idv;

    /** _more_          */
    private boolean autoImportDisplays = false;

    /** _more_          */
    private JCheckBox autoImportCbx;

    /**
     * _more_
     */
    public IdvComponentGroup() {}


    /**
     * _more_
     *
     * @param idv _more_
     * @param name _more_
     */
    public IdvComponentGroup(IntegratedDataViewer idv, String name) {
        super(name);
        this.idv = idv;
    }


    /**
     * _more_
     */
    public void writeSkin() {
        try {
            JCheckBox pluginCbx = new JCheckBox("Install as plugin", true);
            JComponent extra =
                GuiUtils
                    .inset(GuiUtils
                        .top(GuiUtils
                            .vbox(new JLabel("Note: Filename should end in \"skin.xml\""),
                                  pluginCbx)), 5);
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML, extra);
            if (filename == null) {
                return;
            }
            Element root = createXmlNode(XmlUtil.makeDocument());
            setState(root);
            String xml = XmlUtil.toString(root);
            String templatePath = idv.getProperty("idv.ui.skin.template",
                                      (String) null);
            System.err.println(templatePath);
            if (templatePath != null) {
                String template = IOUtil.readContents(templatePath,
                                      getClass(), (String) null);
                if (template != null) {
                    xml = StringUtil.replace(template, "%contents%", xml);
                }
            }
            IOUtil.writeFile(filename, xml);
            if (pluginCbx.isSelected()) {
                idv.getPluginManager().installPluginFromFile(filename);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error writing skin file", exc);
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void initWith(Element node) {
        super.initWith(node);
        autoImportDisplays = XmlUtil.getAttribute(node, "autoimportdisplays",
                autoImportDisplays);
    }


    /**
     * _more_
     *
     * @param doc _more_
     *
     * @return _more_
     */
    public Element createXmlNode(Document doc) {
        Element node = doc.createElement(IdvUIManager.COMP_COMPONENT_GROUP);
        node.setAttribute("autoimportdisplays", "" + autoImportDisplays);
        List displayComponents = getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp  =
                (ComponentHolder) displayComponents.get(i);
            Element         child = null;
            if (comp instanceof IdvComponentHolder) {
                child = ((IdvComponentHolder) comp).createXmlNode(doc);
            } else if (comp instanceof IdvComponentGroup) {
                child = ((IdvComponentGroup) comp).createXmlNode(doc);
            } else if (comp instanceof HtmlComponent) {
                child = doc.createElement(IdvUIManager.COMP_COMPONENT_HTML);
                child.appendChild(XmlUtil.makeCDataNode(doc,
                        ((HtmlComponent) comp).getText()));
            }
            if (child != null) {
                comp.setState(child);
                node.appendChild(child);
            }
        }

        return node;
    }


    /**
     * _more_
     *
     * @param items _more_
     *
     * @return _more_
     */
    protected List getPopupMenuItems(List items) {
        List newItems = new ArrayList();
        newItems.add(GuiUtils.makeMenuItem("Map View", this, "makeNew",
                                           IdvUIManager.COMP_MAPVIEW));
        newItems.add(GuiUtils.makeMenuItem("Globe View", this, "makeNew",
                                           IdvUIManager.COMP_GLOBEVIEW));
        newItems.add(GuiUtils.makeMenuItem("Transect View", this, "makeNew",
                                           IdvUIManager.COMP_TRANSECTVIEW));
        newItems.add(GuiUtils.makeMenuItem("Field Selector", this, "makeNew",
                                           IdvUIManager.COMP_DATASELECTOR));
        newItems.add(
            GuiUtils.makeMenuItem(
                "Tab Group", this, "makeNew",
                IdvUIManager.COMP_COMPONENT_GROUP));
        newItems.add(GuiUtils.makeMenuItem("Html Text", this, "makeNew",
                                           IdvUIManager.COMP_COMPONENT_HTML));


        items.add(GuiUtils.makeMenu("New", newItems));


        List importItems = new ArrayList();
        List vms         = idv.getVMManager().getViewManagers();
        for (int vmIdx = 0; vmIdx < vms.size(); vmIdx++) {
            ViewManager vm              = (ViewManager) vms.get(vmIdx);
            List        viewItems       = new ArrayList();
            List        displayControls = vm.getControls();
            for (int i = 0; i < displayControls.size(); i++) {
                DisplayControlImpl dc =
                    (DisplayControlImpl) displayControls.get(i);
                if ((dc.getComponentHolder() != null)
                        && (dc.getComponentHolder().getParent() == this)) {
                    continue;
                }
                if (viewItems.size() == 0) {
                    if (displayControls.size() > 0) {
                        viewItems.add(GuiUtils.makeMenuItem("Import All",
                                this, "importAllDisplayControls",
                                displayControls));
                        viewItems.add(GuiUtils.MENU_SEPARATOR);
                    }
                }
                viewItems.add(GuiUtils.makeMenuItem(dc.getLabel(), this,
                        "importDisplayControl", dc));
            }
            if (viewItems.size() > 0) {
                String name = vm.getName();
                if ((name == null) || (name.trim().length() == 0)) {
                    name = "View " + (vmIdx + 1);
                }
                importItems.add(GuiUtils.makeMenu(name, viewItems));
            }
        }
        if (importItems.size() == 0) {
            importItems.add(new JMenuItem("No displays to import"));
        }
        items.add(GuiUtils.makeMenu("Import Display", importItems));

        items.add(GuiUtils.makeMenuItem("Write Skin", this, "writeSkin"));
        items.add(GuiUtils.MENU_SEPARATOR);
        super.getPopupMenuItems(items);
        return items;
    }




    /**
     * _more_
     *
     * @param viewManagers _more_
     */
    public void getViewManagers(List viewManagers) {
        List displayComponents = getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp  =
                (ComponentHolder) displayComponents.get(i);
            Element         child = null;
            if (comp instanceof IdvComponentHolder) {
                Object obj = ((IdvComponentHolder) comp).getObject();
                if (obj instanceof ViewManager) {
                    viewManagers.add(obj);
                }
            } else if (comp instanceof IdvComponentGroup) {
                ((IdvComponentGroup) comp).getViewManagers(viewManagers);
            }
        }

    }


    /**
     * _more_
     *
     * @param displayControls _more_
     */
    public void importAllDisplayControls(List displayControls) {
        for (int i = 0; i < displayControls.size(); i++) {
            DisplayControlImpl dc =
                (DisplayControlImpl) displayControls.get(i);
            if ((dc.getComponentHolder() != null)
                    && (dc.getComponentHolder().getParent() == this)) {
                continue;
            }
            importDisplayControl(dc);
        }
    }


    protected void doDrop(Object object) {
        if(object instanceof DisplayControl) {
            importDisplayControl((DisplayControlImpl)object);
        } else {
            super.doDrop(object);
        }
    }

    public boolean dropOk(Object object) {
        if(object instanceof DisplayControl) {
            return true;
        }
        return super.dropOk(object);
    }


    /**
     * _more_
     *
     * @param control _more_
     *
     * @return _more_
     */
    public boolean tryToImportDisplayControl(DisplayControlImpl control) {
        if (autoImportDisplays) {
            importDisplayControl(control);
            return true;
        }
        List displayComponents = getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp = (ComponentHolder) displayComponents.get(i);
            if (comp instanceof IdvComponentGroup) {
                if (((IdvComponentGroup) comp).tryToImportDisplayControl(
                        control)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void addGroups(List l) {
        l.add(this);
        List displayComponents = getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp  =
                (ComponentHolder) displayComponents.get(i);
            Element         child = null;
            if (comp instanceof IdvComponentGroup) {
                ((IdvComponentGroup)comp).addGroups(l);
            }
        }
    }

    /**
     * _more_
     *
     * @param dc _more_
     */
    public void importDisplayControl(DisplayControlImpl dc) {
        if (dc.getComponentHolder() != null) {
            dc.getComponentHolder().removeDisplayControl(dc);
        }
        idv.getIdvUIManager().getViewPanel().removeDisplayControl(dc);
        dc.guiImported();
        addComponent(new IdvComponentHolder(idv, dc));
    }

    /**
     * _more_
     *
     * @param what _more_
     */
    public void makeNew(String what) {
        try {
            if (what.equals(IdvUIManager.COMP_MAPVIEW)) {
                ViewManager vm = new MapViewManager(idv,
                                     new ViewDescriptor(),
                                     "showControlLegend=false");
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_GLOBEVIEW)) {
                MapViewManager vm = new MapViewManager(idv,
                                        new ViewDescriptor(),
                                        "showControlLegend=false");
                vm.setUseGlobeDisplay(true);
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_TRANSECTVIEW)) {
                ViewManager vm = new TransectViewManager(idv,
                                     new ViewDescriptor(),
                                     "showControlLegend=false");
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_DATASELECTOR)) {
                addComponent(new IdvComponentHolder(idv,
                        idv.getIdvUIManager().createDataSelector(false,
                            false)));
            } else if (what.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
                String name = GuiUtils.getInput("Enter name for tab group",
                                  "Name: ", "Group");
                if (name == null) {
                    return;
                }
                IdvComponentGroup group = new IdvComponentGroup(idv, name);
                group.setLayout(group.LAYOUT_TABS);
                addComponent(group);

            } else if (what.equals(IdvUIManager.COMP_COMPONENT_HTML)) {
                String text = GuiUtils.getInput("Enter html", "Html: ", "");
                if (text == null) {
                    return;
                }
                ComponentHolder comp = new HtmlComponent("Html Text", text);
                comp.setShowLabel(false);
                addComponent(comp);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error making new " + what, exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean applyProperties() {
        boolean result = super.applyProperties();
        if ( !result) {
            return false;
        }
        autoImportDisplays = autoImportCbx.isSelected();
        return true;
    }


    /**
     * _more_
     *
     * @param comps _more_
     * @param tabIdx _more_
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx == 0) {
            autoImportCbx =
                new JCheckBox("Import any displays added to window",
                              autoImportDisplays);
            comps.add(GuiUtils.filler());
            comps.add(GuiUtils.left(autoImportCbx));
        }
    }



    /**
     *  Set the Idv property.
     *
     *  @param value The new value for Idv
     */
    public void setIdv(IntegratedDataViewer value) {
        idv = value;
    }

    /**
     *  Get the Idv property.
     *
     *  @return The Idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     *  Set the AutoImportDisplays property.
     *
     *  @param value The new value for AutoImportDisplays
     */
    public void setAutoImportDisplays(boolean value) {
        autoImportDisplays = value;
    }

    /**
     *  Get the AutoImportDisplays property.
     *
     *  @return The AutoImportDisplays
     */
    public boolean getAutoImportDisplays() {
        return autoImportDisplays;
    }


}

