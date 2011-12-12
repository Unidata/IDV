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


import org.w3c.dom.Document;
import org.w3c.dom.Element;


import ucar.unidata.idv.*;
import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 */

public class IdvComponentHolder extends ComponentHolder {

    /** _more_ */
    public static final String TYPE_SKIN = "skin";

    /** _more_ */
    public static final String TYPE_CHOOSERS = "choosers";

    /** _more_ */
    public static final String TYPE_DEFAULT = "default";

    /** _more_ */
    private String type = TYPE_DEFAULT;


    /** _more_ */
    IntegratedDataViewer idv;

    /** _more_ */
    private Object object;

    /** _more_ */
    private List viewManagers;

    /**
     * _more_
     */
    public IdvComponentHolder() {}

    /**
     * _more_
     *
     * @param idv _more_
     * @param object _more_
     */
    public IdvComponentHolder(IntegratedDataViewer idv, Object object) {
        super("");
        this.idv    = idv;
        this.object = object;
        if (object instanceof DisplayControl) {
            DisplayControlImpl control = (DisplayControlImpl) object;
            control.setComponentHolder(this);
            control.setShowInTabs(false);
            control.setMakeWindow(false);
            control.guiImported();
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        String name = getNameFromObject();
        if (name == null) {
            return getTypeName();
        }
        return name;
    }


    /**
     * _more_
     *
     * @param doc _more_
     *
     * @return _more_
     */
    public Element createXmlNode(Document doc) {
        if (object == null) {
            return null;
        }
        if (object instanceof ViewManager) {
            Element node = doc.createElement(IdvUIManager.COMP_VIEW);
            node.setAttribute(IdvXmlUi.ATTR_CLASS,
                              object.getClass().getName());
            node.appendChild(XmlUtil.makeCDataNode(doc,
                    idv.encodeObject(object, false)));
            node.setAttribute("showheader", "" + getShowHeader());

            return node;
        }
        if (object instanceof String) {
            if (type.equals(TYPE_SKIN)) {
                Element node =
                    doc.createElement(IdvUIManager.COMP_COMPONENT_SKIN);
                node.setAttribute("url", object.toString());
                return node;
            }
        }
        return null;
    }


    /**
     * _more_
     */
    public void doRemove() {
        //        Misc.printStack("doRemove",10,null);
        if ((object != null) && (idv != null)) {
            try {
                if (viewManagers != null) {
                    for (int i = 0; i < viewManagers.size(); i++) {
                        ((ViewManager) viewManagers.get(i)).destroy();
                    }
                    viewManagers = null;
                }

                if (object instanceof ViewManager) {
                    ((ViewManager) object).destroy();
                    //                    idv.getVMManager().removeViewManager(
                    //                        (ViewManager) object);
                } else if (object instanceof DisplayControl) {
                    ((DisplayControl) object).doRemove();
                } else if (object instanceof DataSelector) {
                    idv.getIdvUIManager().removeDataSourceHolder(
                        (DataSelector) object);
                }

            } catch (Exception exc) {
                LogUtil.logException("Error removing component holder", exc);
            }
            idv    = null;
            object = null;
        }
        super.doRemove();
    }


    /**
     * _more_
     *
     * @param displayControl _more_
     */
    public void removeDisplayControl(DisplayControl displayControl) {
        if (displayControl == object) {
            object = null;
            doRemove();
        }
    }


    /**
     * _more_
     */
    public void undockControl() {
        DisplayControlImpl control = (DisplayControlImpl) object;
        object = null;
        control.setComponentHolder(null);
        control.setShowInTabs(false);
        control.setMakeWindow(true);
        control.guiExported();
        //control.popup(null);
        doRemove();
    }


    /**
     * _more_
     *
     * @param items _more_
     *
     * @return _more_
     */
    protected List getPopupMenuItems(List items) {
        if (object instanceof DisplayControl) {
            items.add(GuiUtils.makeMenuItem("Undock " + getName(), this,
                                            "undockControl"));
        }


        items.add(GuiUtils.makeMenuItem("Remove " + getName(), this,
                                        "removeDisplayComponent"));



        return items;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCategory() {
        if (object instanceof ViewManager) {
            return "Views";
        }
        if (object instanceof DisplayControl) {
            return "Displays";
        }
        if (object instanceof DataSelector) {
            return "Tools";
        }
        if (object instanceof String) {
            if (type.equals(TYPE_SKIN)) {
                return "IDV User Interface";
            }
            if (type.equals(TYPE_CHOOSERS)) {
                return "Data Choosers";
            }
        }
        return super.getCategory();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getNameFromObject() {
        if (object instanceof ViewManager) {
            return ((ViewManager) object).getName();
        }
        if (object instanceof DisplayControl) {
            return ((DisplayControl) object).getLabel();
        }
        if (object instanceof DataSelector) {
            return "Field Selector";
        }
        return super.getName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ImageIcon getIcon() {
        if (object instanceof MapViewManager) {
            MapViewManager mvm = (MapViewManager) object;
            if (mvm.getUseGlobeDisplay()) {
                return ViewPanel.ICON_GLOBE;
            }
            return ViewPanel.ICON_MAP;
        }
        if (object instanceof TransectViewManager) {
            return ViewPanel.ICON_TRANSECT;
        }
        return super.getIcon();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getTypeName() {
        if (object instanceof MapViewManager) {
            MapViewManager mvm = (MapViewManager) object;
            if (mvm.getUseGlobeDisplay()) {
                return "Globe View";
            }
            return "Map View";
        }

        if (object instanceof TransectViewManager) {
            return "Transect View";
        }

        if (object instanceof ViewManager) {
            return "View";
        }
        if (object instanceof DisplayControl) {
            return "Display Control";
        }
        if (object instanceof DataSelector) {
            return "Field Selector";
        }
        if (object instanceof String) {
            if (type.equals(TYPE_SKIN)) {
                return "UI Skin";
            }
            if (type.equals(TYPE_CHOOSERS)) {
                return "Data Choosers";
            }
        }
        return super.getTypeName();

    }

    /**
     * _more_
     */
    public void displayControlHasInitialized() {
        clearContents();
        getContents().invalidate();
        ComponentGroup parent = getParent();
        if (parent != null) {
            parent.redoLayout();
        }
        Component comp = getContents();
        //A total hack but...
        Window w = GuiUtils.getWindow(comp);
        if (w != null) {
            w.doLayout();
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void initWith(Element node) {
        super.initWith(node);
        setShowHeader(XmlUtil.getAttribute(node, "showheader",
                                           getShowHeader()));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent makeSkin() {
        try {
            String  path = (String) object;
            Element root = null;
            XmlResourceCollection skins =
                getIdv().getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_SKIN);


            for (int i = 0; i < skins.size(); i++) {
                String id = skins.getProperty("skinid", i);
                if (Misc.equals(path, skins.getProperty("skinid", i))) {
                    root = skins.getRoot(i, false);
                    break;
                }
            }

            if (root == null) {
                for (int i = 0; i < skins.size(); i++) {
                    if (Misc.equals(path, skins.get(i).toString())) {
                        root = skins.getRoot(i);
                        break;
                    }
                }
            }
            if (root == null) {
                root = XmlUtil.getRoot(path, getClass());
            }
            IdvXmlUi xmlUI = getIdv().getIdvUIManager().doMakeIdvXmlUi(null,
                                 viewManagers, root);
            Element startNode = XmlUtil.findElement(root, null,
                                    "embeddednode", "true");
            if (startNode != null) {
                xmlUI.setStartNode(startNode);
            }
            JComponent contents = (JComponent) xmlUI.getContents();
            viewManagers = xmlUI.getViewManagers();
            return contents;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeContents() {
        if (object instanceof String) {
            if (type.equals(TYPE_SKIN)) {
                return makeSkin();
            }
            if (type.equals(TYPE_CHOOSERS)) {
                List choosers = new ArrayList();
                return (JComponent) getIdv().getIdvChooserManager()
                    .createChoosers(false, choosers, null);
            }
            return new JLabel("Unknown component: " + object);
        }

        if (object instanceof ViewManager) {
            return (JComponent) ((ViewManager) object).getContents();
        }
        if (object instanceof DisplayControlImpl) {
            JComponent inner =
                (JComponent) ((DisplayControlImpl) object).getOuterContents();

            if (inner == null) {
                return new JLabel("");
            }
            return inner;
        }
        if (object instanceof DataSelector) {
            return ((DataSelector) object).getContents();
        }
        return new JLabel("Unknwon object:" + object);
    }



    /**
     * Set the Object property.
     *
     * @param value The new value for Object
     */
    public void setObject(Object value) {
        object = value;
    }

    /**
     * Get the Object property.
     *
     * @return The Object
     */
    public Object getObject() {
        return object;
    }


    /**
     * Set the Idv property.
     *
     * @param value The new value for Idv
     */
    public void setIdv(IntegratedDataViewer value) {
        idv = value;
    }

    /**
     * Get the Idv property.
     *
     * @return The Idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }




    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the ViewManagers property.
     *
     * @param value The new value for ViewManagers
     */
    public void setViewManagers(List value) {
        viewManagers = value;
    }

    /**
     * Get the ViewManagers property.
     *
     * @return The ViewManagers
     */
    public List getViewManagers() {
        return viewManagers;
    }



}
