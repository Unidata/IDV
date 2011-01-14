/*
 * $Id: DisplayGroup.java,v 1.13 2007/04/16 21:32:37 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.ui;


import org.w3c.dom.Element;


import ucar.unidata.collab.PropertiedThing;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;



import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.swing.tree.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class ComponentHolder extends PropertiedThing {

    /** _more_ */
    public static final String ATTR_NAME = "name";


    /** _more_ */
    public static final String[] BORDERS = { XmlUi.BORDER_EMPTY,
                                             XmlUi.BORDER_TITLED,
                                             XmlUi.BORDER_ETCHED,
                                             XmlUi.BORDER_LINE };

    /** _more_ */
    public static final String[] BORDER_NAMES = { "None", "Title", "Etched",
            "Line" };

    /** _more_ */
    private String borderLayoutLocation = BorderLayout.CENTER;


    /** _more_ */
    private String name;

    /** _more_ */
    protected JTextField nameFld;


    /** _more_ */
    private boolean showHeader = false;

    /** _more_ */
    protected JButton displayBtn;

    /** _more_ */
    protected JComponent header;

    /** _more_ */
    private String category;

    /** _more_ */
    protected ComponentGroup parent;

    /** _more_ */
    private JComponent innerContents;

    /** _more_          */
    private JComponent outerContents;

    /** _more_ */
    private Rectangle layoutRect;

    /** _more_ */
    protected boolean isRemoved = false;

    /** _more_ */
    private String border = XmlUi.BORDER_EMPTY;

    /** _more_ */
    private JComboBox borderBox;

    /** _more_ */
    private JCheckBox showHeaderCbx;


    /** _more_ */
    private JInternalFrame frame;

    /** _more_          */
    private boolean internalFrameShown = true;

    /**
     * _more_
     */
    public ComponentHolder() {}


    /**
     * _more_
     *
     * @param name _more_
     */
    public ComponentHolder(String name) {
        this.name = name;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param contents _more_
     */
    public ComponentHolder(String name, JComponent contents) {
        this(name);
        this.innerContents = contents;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public ComponentGroup getRoot() {
        if (parent != null) {
            return parent.getRoot();
        }
        if (this instanceof ComponentGroup) {
            return (ComponentGroup) this;
        }
        return null;
    }


    private String myToString() {
        return toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JInternalFrame getInternalFrame() {
        if (frame == null) {
            frame = new JInternalFrame(getName(), true, true, true, true);
            frame.addInternalFrameListener(new InternalFrameListener(){
                    public void 	internalFrameClosed(InternalFrameEvent e) {doRemove();}
                    public void 	internalFrameClosing(InternalFrameEvent e) {}
                    public void 	internalFrameDeactivated(InternalFrameEvent e){
                        GuiUtils.toggleHeavyWeightComponents(frame,false);
                    }
                    public void 	internalFrameActivated(InternalFrameEvent e) {
                        GuiUtils.toggleHeavyWeightComponents(frame,true);
                    }
                    public void 	internalFrameDeiconified(InternalFrameEvent e){}
                    public void 	internalFrameIconified(InternalFrameEvent e){}
                    public void 	internalFrameOpened(InternalFrameEvent e) {}
                });
            if ( !internalFrameShown) {
                try {
                    frame.setIcon(true);
                } catch (Exception ignore) {}
            }
        }
        return frame;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHierachicalName() {
        if (getParent() != null) {
            return getParent().getHierachicalName() + "-" + getName();
        }
        return getName();
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void setState(Element node) {
        node.setAttribute(ATTR_NAME, getName());
        node.setAttribute(XmlUi.ATTR_BORDER, border);
        node.setAttribute(XmlUi.ATTR_PLACE, borderLayoutLocation);
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void initWith(Element node) {
        setName(XmlUtil.getAttribute(node, ATTR_NAME, ""));
        if (XmlUtil.hasAttribute(node, XmlUi.ATTR_BORDER)) {
            setBorder(XmlUtil.getAttribute(node, XmlUi.ATTR_BORDER,
                                           XmlUi.BORDER_EMPTY));
        }
        if (XmlUtil.hasAttribute(node, XmlUi.ATTR_PLACE)) {
            borderLayoutLocation = XmlUtil.getAttribute(node,
                    XmlUi.ATTR_PLACE, "");
        }

    }

    /**
     * _more_
     */
    protected void clearContents() {
        innerContents = null;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getContents() {
        if (innerContents == null) {
            innerContents = doMakeContents();
            if (header == null) {
                header = doMakeHeader();
                header.setVisible(getShowHeader());
            }
            innerContents = GuiUtils.topCenter(header, innerContents);
            setBorder(innerContents);
            outerContents = wrapContents(innerContents);
        }
        return outerContents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeHeader() {
        displayBtn = GuiUtils.getImageButton("/auxdata/ui/icons/Down16.gif",
                                             getClass());
        displayBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showPopup(displayBtn, 0,
                          (int) displayBtn.getBounds().getHeight());
            }
        });
        displayBtn.setPreferredSize(null);
        displayBtn.setText(getName());
        return GuiUtils.inset(GuiUtils.left(displayBtn),
                              new Insets(0, 5, 0, 0));
    }


    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     */
    protected JComponent wrapContents(JComponent contents) {
        return contents;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeContents() {
        return innerContents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTypeName() {
        return "Component";
    }

    /**
     * SHow the popup menu
     *
     * @param where component to show near to
     * @param x x
     * @param y y
     */
    public void showPopup(JComponent where, int x, int y) {
        List items = new ArrayList();
        getPopupMenuItems(items);
        //        addGroupMenuItems(items);
        if (items.size() == 0) {
            return;
        }
        GuiUtils.makePopupMenu(items).show(where, x, y);
    }


    /**
     * _more_
     *
     * @param comps _more_
     * @param tabIdx _more_
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);

        nameFld = new JTextField(getName());
        comps.add(GuiUtils.rLabel("Name:"));
        comps.add(nameFld);


        if (tabIdx == 0) {
            showHeaderCbx = new JCheckBox("Show Label", showHeader);
            borderBox = new JComboBox(new Vector(Misc.toList(BORDER_NAMES)));
            borderBox.setSelectedIndex(Misc.toList(BORDERS).indexOf(border));
            comps.add(GuiUtils.rLabel("Border:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(borderBox,
                    GuiUtils.filler(20, 5), showHeaderCbx)));
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

        if (nameFld != null) {
            setName(nameFld.getText());
        }

        //Apply this in case a subclass has changed anything
        if (displayBtn != null) {
            displayBtn.setText(getName());
        }


        String newBorder =
            BORDERS[Misc.toList(BORDER_NAMES).indexOf(borderBox.getSelectedItem())];
        if ( !newBorder.equals(border)) {
            border = newBorder;
            if (innerContents != null) {
                setBorder(innerContents);
            }
        }
        if (showHeader != showHeaderCbx.isSelected()) {
            setShowHeader(showHeaderCbx.isSelected());
        }
        return true;
    }


    /**
     * _more_
     *
     * @param comp _more_
     */
    protected void setBorder(JComponent comp) {
        Border theBorder = null;
        if (border.equals(XmlUi.BORDER_TITLED)) {
            theBorder = BorderFactory.createTitledBorder(getName());
        } else if (border.equals(XmlUi.BORDER_LINE)) {
            theBorder = BorderFactory.createLineBorder(Color.black, 1);
        } else if (border.equals(XmlUi.BORDER_ETCHED)) {
            theBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        }
        if (comp != null) {
            comp.setBorder(theBorder);
        }
    }

    /**
     * _more_
     *
     * @param parent _more_
     *
     * @return _more_
     */
    public DefaultMutableTreeNode makeTree(DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
        if (parent != null) {
            parent.add(node);
        }
        return node;
    }

    /**
     * Get the menu items for the popup menu
     *
     * @param items List of items to add to
     *
     * @return The items list
     */
    protected List getPopupMenuItems(List items) {
        items.add(GuiUtils.makeMenuItem("Remove " + getTypeName(), this,
                                        "removeDisplayComponent"));
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Properties...", this,
                                        "showProperties"));

        return items;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getBeingShown() {
        return true;
    }




    /**
     * Remove me
     *
     * @return was removed
     */
    public boolean removeDisplayComponent() {
        if (GuiUtils.askYesNo("Remove Display",
                              "Are you sure you want to remove: "
                              + toString())) {
            doRemove();
            return true;
        } else {
            return false;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return getName();
    }


    /**
     * _more_
     *
     * @param tab _more_
     */
    public void print(String tab) {
        System.err.println(tab + this);
    }


    /**
     * _more_
     */
    public void doRemove() {
        if (isRemoved) {
            return;
        }
        isRemoved = true;
        if (parent != null) {
            parent.removeComponent(this);
        }
        parent = null;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        if (displayBtn != null) {
            displayBtn.setText(getName());
        }
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
     * Set the Parent property.
     *
     * @param newParent _more_
     */
    public void setParent(ComponentGroup newParent) {
        if (newParent == parent) {
            return;
        }
        ComponentGroup oldParent = parent;
        parent = null;
        if (oldParent != null) {
            oldParent.removeComponent(this);
        }
        parent = newParent;
        if (parent != null) {
            //Don't do this. Anything that calls setParent should also add the component
            //            parent.addComponent(this);
        }
    }

    /**
     * Get the Parent property.
     *
     * @return The Parent
     */
    public ComponentGroup getParent() {
        return parent;
    }


    /**
     * Set the LayoutRect property.
     *
     * @param value The new value for LayoutRect
     */
    public void setLayoutRect(Rectangle value) {
        layoutRect = value;
    }

    /**
     * Get the LayoutRect property.
     *
     * @return The LayoutRect
     */
    public Rectangle getLayoutRect() {
        return layoutRect;
    }

    /**
     * Set the Category property.
     *
     * @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     * Get the Category property.
     *
     * @return The Category
     */
    public String getCategory() {
        return category;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * Set the ShowHeader property.
     *
     * @param value The new value for ShowHeader
     */
    public void setShowHeader(boolean value) {
        //        Misc.printStack("showHeader:" + value);
        showHeader = value;
        if (header != null) {
            header.setVisible(getShowHeader());
        }
    }

    /**
     * Get the ShowHeader property.
     *
     * @return The ShowHeader
     */
    public boolean getShowHeader() {
        return showHeader;
    }

    /**
     * Set the Border property.
     *
     * @param value The new value for Border
     */
    public void setBorder(String value) {
        border = value;
    }

    /**
     * Get the Border property.
     *
     * @return The Border
     */
    public String getBorder() {
        return border;
    }




    /**
     *  Set the BorderLayoutLocation property.
     *
     *  @param value The new value for BorderLayoutLocation
     */
    public void setBorderLayoutLocation(String value) {
        borderLayoutLocation = value;
    }

    /**
     *  Get the BorderLayoutLocation property.
     *
     *  @return The BorderLayoutLocation
     */
    public String getBorderLayoutLocation() {
        return borderLayoutLocation;
    }

    /**
     * Set the InternalFrameShow property.
     *
     * @param value The new value for InternalFrameShow
     */
    public void setInternalFrameShown(boolean value) {
        internalFrameShown = value;
    }

    /**
     * Get the InternalFrameShow property.
     *
     * @return The InternalFrameShow
     */
    public boolean getInternalFrameShown() {
        if (frame != null) {
            return !frame.isIcon();
        }
        return internalFrameShown;
    }





}

