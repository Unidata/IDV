/*
 * $Id: DatasetUI.java,v 1.16 2007/07/06 20:45:29 jeffmc Exp $
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

package ucar.unidata.ui;



import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.metdata.NamedStationImpl;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.xml.XmlObjectStore;

import ucar.unidata.xml.XmlUtil;



import visad.CommonUnit;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class DatasetUI
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
public class DatasetUI {

    /** _more_ */
    public static final String NULL_STRING = null;

    /** _more_ */
    public static final String TAG_DATACHOICESET = "datachoiceSet";

    /** _more_ */
    public static final String TAG_INTERFACES = "interfaces";

    /** _more_ */
    public static final String TAG_INTERFACE = "interface";

    /** _more_ */
    public static final String TAG_FIELD = "field";

    /** _more_ */
    public static final String TAG_CHOICE = "choice";

    /** _more_ */
    public static final String TAG_STATION = "station";

    /** _more_ */
    public static final String TAG_URL = "url";

    /** _more_ */
    public static final String TAG_CLIENT = "client";

    /** _more_ */
    public static final String ATTR_ACTION = "action";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_ALT = "alt";

    /** _more_ */
    public static final String ATTR_LAT = "lat";

    /** _more_ */
    public static final String ATTR_LON = "lon";

    /** _more_ */
    public static final String ATTR_MAP = "map";

    /** _more_ */
    public static final String ATTR_REQUIRED = "required";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_CLIENT = "client";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_SELECT = "select";

    /** _more_ */
    public static final String ATTR_TEMPLATE = "template";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_COLS = "cols";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String SELECT_ONE = "one";

    /** _more_ */
    public static final String SELECT_MANY = "many";

    /** _more_ */
    public static final String SELECT_RANGE = "range";


    /** _more_ */
    public static final String TAG_SELECTIONS = "selections";

    /** _more_ */
    public static final String TAG_QUERY = "query";


    /** _more_ */
    public static final String TAG_SEL_LABEL = "selectLabel";

    /** _more_ */
    public static final String TAG_SEL_BOOLEAN = "selectBoolean";

    /** _more_ */
    public static final String TAG_SEL_LIST = "selectList";

    /** _more_ */
    public static final String TAG_SEL_MENU = "selectMenu";

    /** _more_ */
    public static final String TAG_SEL_NUMBER = "selectNumber";

    /** _more_ */
    public static final String TAG_SEL_RADIO = "selectRadio";

    /** _more_ */
    public static final String TAG_SEL_TEXT = "selectText";

    /** _more_ */
    public static final String TAG_SEL_STATIONS = "selectStations";


    /** _more_ */
    private ActionListener actionListener;

    /** _more_ */
    private Element root;

    /** _more_ */
    private JPanel contents;

    /** _more_ */
    private String actionTemplate;

    /** _more_ */
    private List widgets = new ArrayList();

    /** _more_ */
    private Hashtable widgetToNode = new Hashtable();

    /** _more_ */
    private XmlUi xmlUi;

    /** _more_ */
    private List selectComponents = new ArrayList();


    /**
     * _more_
     *
     * @param root
     * @param actionListener
     *
     */
    public DatasetUI(Element root, ActionListener actionListener) {
        this.root           = root;
        this.actionListener = actionListener;
        init();
    }


    /**
     *  This processes the set of selections, creating the gui
     *  elements, and returning the default xml for constructing the
     *  ui.
     *
     * @param selectionsNode
     * @return _more_
     */
    private StringBuffer processSelections(Element selectionsNode) {
        StringBuffer uiXml = new StringBuffer();
        uiXml.append("<panel layout=\"gridbag\" colwidths=\"0,1\" >\n");

        NodeList children = XmlUtil.getElements(selectionsNode);
        for (int i = 0; i < children.getLength(); i++) {
            Element   child = (Element) children.item(i);
            String    tag   = child.getTagName();
            String    id    = XmlUtil.getAttribute(child, ATTR_ID);
            String    label = XmlUtil.getAttribute(child, ATTR_LABEL, id);
            Component comp  = null;
            if (tag.equals(TAG_SEL_MENU)) {
                comp = processMenu(child);
            } else if (tag.equals(TAG_SEL_RADIO)) {
                comp = processRadio(child);
            } else if (tag.equals(TAG_SEL_LABEL)) {
                comp  = new JLabel(label);
                label = "";
            } else if (tag.equals(TAG_SEL_LIST)) {
                comp = processList(child);
            } else if (tag.equals(TAG_SEL_TEXT)) {
                comp = processText(child);
            } else if (tag.equals(TAG_SEL_BOOLEAN)) {
                comp = processBoolean(child);
            } else if (tag.equals(TAG_SEL_STATIONS)) {
                comp = processStations(child);
            } else {
                throw new IllegalArgumentException("Unknown child tag:"
                        + tag);
            }
            if (comp != null) {
                xmlUi.addComponent(id, comp);
                selectComponents.add(comp);
                uiXml.append("<label label=\"" + label + "\"/>\n");
                uiXml.append("<component id=\"" + id + "\"/>\n");
            }
        }
        uiXml.append("</panel>\n");
        return uiXml;
    }


    /**
     * _more_
     */
    private void init() {
        xmlUi = new XmlUi();
        List    components     = new ArrayList();

        Element selectionsNode = XmlUtil.findChild(root, TAG_SELECTIONS);
        if (selectionsNode == null) {
            throw new IllegalArgumentException(
                "Dataset xml does not have a selections tag");
        }
        selectionsNode = XmlUtil.findUrlRefNode(selectionsNode, TAG_URL);
        Element interfacesNode = XmlUtil.findChild(root, TAG_INTERFACES);
        if (interfacesNode != null) {
            interfacesNode = XmlUtil.findUrlRefNode(interfacesNode, TAG_URL);
        }

        StringBuffer uiXml         = processSelections(selectionsNode);
        Element      interfaceNode = null;
        if (interfacesNode != null) {
            interfaceNode =
                XmlUtil.findElement(XmlUtil.getElements(interfacesNode,
                    TAG_INTERFACE), TAG_CLIENT, "idv");
            if (interfaceNode == null) {
                interfaceNode =
                    XmlUtil.findElement(XmlUtil.getElements(interfacesNode,
                        TAG_INTERFACE), TAG_CLIENT, "java");
            }
            if (interfaceNode != null) {
                interfaceNode = XmlUtil.findUrlRefNode(interfaceNode,
                        TAG_URL);
            }
        }
        if (interfaceNode == null) {
            try {
                interfaceNode = XmlUtil.getRoot("<interface><ui>"
                        + uiXml.toString() + "</ui></interface>");
            } catch (Exception exc) {
                LogUtil.logException("Creating ui xml", exc);
            }
        }

        //TODO: Now we need to generate the ui here.
        xmlUi.setRoot(interfaceNode);
        contents = GuiUtils.topCenterBottom(null, xmlUi.getContents(), null);



        Element queryNode = XmlUtil.findChild(root, TAG_QUERY);
        if (queryNode != null) {
            actionTemplate = XmlUtil.getAttribute(queryNode, ATTR_ACTION);
        } else {
            //Error?
        }
    }



    /**
     * _more_
     *
     * @param widget
     * @param node
     */
    private void associate(Object widget, Element node) {
        widgets.add(widget);
        widgetToNode.put(widget, node);
    }



    /**
     * _more_
     * @return _more_
     */
    public String processActionTemplate() {
        String processedTemplate = actionTemplate;
        List   macros            = new ArrayList();
        String errorMsg          = null;
        for (int i = 0; i < widgets.size(); i++) {
            Object  widget = widgets.get(i);
            Element node   = (Element) widgetToNode.get(widget);
            String nodeTemplate = XmlUtil.getAttribute(node, ATTR_TEMPLATE,
                                      "%value%");
            String id = XmlUtil.getAttribute(node, ATTR_ID);
            boolean required = XmlUtil.getAttribute(node, ATTR_REQUIRED,
                                   false);
            List values = getValues(widget);
            if (required && ((values == null) || (values.size() == 0))) {
                if (errorMsg == null) {
                    errorMsg = "";
                }
                errorMsg = errorMsg + "\n The "
                           + XmlUtil.getAttribute(node, ATTR_LABEL, "")
                           + " field is required";
                continue;
            }
            if (values == null) {
                continue;
            }
            String macro = "%" + id + "%";
            macros.add(macro);
            for (int valueIdx = 0; valueIdx < values.size(); valueIdx++) {
                String theValue = (String) values.get(valueIdx);
                theValue = StringUtil.replace(nodeTemplate, "%value%",
                        theValue);
                processedTemplate = StringUtil.replace(processedTemplate,
                        macro, theValue + macro);
            }
        }
        if (errorMsg != null) {
            LogUtil.userMessage(errorMsg);
            return null;
        }

        for (int i = 0; i < macros.size(); i++) {
            processedTemplate = StringUtil.replace(processedTemplate,
                    (String) macros.get(i), "");
        }
        return processedTemplate;
    }


    /**
     * _more_
     *
     * @param comp
     * @return _more_
     */
    private List getValues(Object comp) {
        if (comp instanceof JCheckBox) {
            return Misc.newList("" + ((JCheckBox) comp).isSelected());
        }
        if (comp instanceof StationLocationMap) {
            StationLocationMap slm      = (StationLocationMap) comp;
            List               stations = slm.getSelectedStations();
            List               result   = new ArrayList();
            for (int idx = 0; idx < stations.size(); idx++) {
                NamedStationImpl station =
                    (NamedStationImpl) stations.get(idx);
                result.add(station.getID());
            }
            return result;
        }
        if (comp instanceof JTextField) {
            return Misc.newList(((JTextField) comp).getText());
        }
        if (comp instanceof JTextArea) {
            return Misc.newList(((JTextArea) comp).getText());
        }
        if (comp instanceof JComboBox) {
            TwoFacedObject tfo =
                (TwoFacedObject) ((JComboBox) comp).getSelectedItem();
            return Misc.newList(tfo.getId());
        }
        if (comp instanceof ButtonGroup) {
            return Misc.newList(
                ((ButtonGroup) comp).getSelection().getActionCommand());
        }
        if (comp instanceof JList) {
            JList    jlist  = (JList) comp;
            Object[] values = jlist.getSelectedValues();
            List     result = new ArrayList();
            for (int i = 0; i < values.length; i++) {
                TwoFacedObject tfo = (TwoFacedObject) values[i];
                result.add(tfo.getId());
            }
            return result;
        }
        if (comp instanceof List) {
            List lcomp = (List) comp;
            if (lcomp.size() == 0) {
                return null;
            }
            Object first = lcomp.get(0);
            //....
        }
        System.err.println("Unknown component:" + comp.getClass().getName());
        return null;
    }

    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processBoolean(Element field) {
        boolean   value = XmlUtil.getAttribute(field, ATTR_VALUE, false);
        JCheckBox b     = new JCheckBox("", value);
        associate(b, field);
        return b;
    }

    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processText(Element field) {
        int    rows  = XmlUtil.getAttribute(field, ATTR_ROWS, 1);
        int    cols  = XmlUtil.getAttribute(field, ATTR_COLS, 30);
        String value = XmlUtil.getAttribute(field, ATTR_VALUE, "");
        if (rows == 1) {
            JTextField comp = new JTextField(value, cols);
            associate(comp, field);
            return comp;
        } else {
            JTextArea comp = new JTextArea(value, rows, cols);
            associate(comp, field);
            JScrollPane scroller = new JScrollPane(comp);
            return scroller;
        }

    }



    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processStations(Element field) {
        List     stations = new ArrayList();
        NodeList children = XmlUtil.getElements(field, TAG_STATION);
        for (int i = 0; i < children.getLength(); i++) {
            Element choice = (Element) children.item(i);
            String  value  = XmlUtil.getAttribute(choice, ATTR_VALUE);
            String  name   = XmlUtil.getAttribute(choice, ATTR_NAME, value);
            double  lat    = XmlUtil.getAttribute(choice, ATTR_LAT, 0.0);
            double  lon    = XmlUtil.getAttribute(choice, ATTR_LON, 0.0);
            double  alt    = XmlUtil.getAttribute(choice, ATTR_ALT, 0.0);
            try {
                stations.add(new NamedStationImpl(value, name, lat, lon, alt,
                        CommonUnit.meter));
            } catch (Exception exc) {
                LogUtil.logException("Creating station", exc);
                break;
            }
        }
        String select = XmlUtil.getAttribute(field, ATTR_SELECT, SELECT_ONE);
        String map = XmlUtil.getAttribute(field, ATTR_MAP, NULL_STRING);
        StationLocationMap slm =
            new StationLocationMap(select.equals(SELECT_MANY), map);
        slm.setPreferredSize(new Dimension(300, 300));
        slm.setStations(stations);
        associate(slm, field);
        return slm;
    }


    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processMenu(Element field) {
        Vector   choices  = new Vector();
        NodeList children = XmlUtil.getElements(field, TAG_CHOICE);
        for (int i = 0; i < children.getLength(); i++) {
            Element choice = (Element) children.item(i);
            String  value  = XmlUtil.getAttribute(choice, ATTR_VALUE);
            String  name   = XmlUtil.getAttribute(choice, ATTR_NAME, value);
            choices.add(new TwoFacedObject(name, value));
        }
        JComboBox box = new JComboBox(choices);
        associate(box, field);
        return box;
    }


    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processList(Element field) {
        int      rows     = XmlUtil.getAttribute(field, ATTR_ROWS, 5);
        Vector   choices  = new Vector();
        NodeList children = XmlUtil.getElements(field, TAG_CHOICE);
        int      index    = 0;
        String   value    = XmlUtil.getAttribute(field, ATTR_VALUE, "");
        for (int i = 0; i < children.getLength(); i++) {
            Element choice = (Element) children.item(i);
            String  bvalue = XmlUtil.getAttribute(choice, ATTR_VALUE);
            String  name   = XmlUtil.getAttribute(choice, ATTR_NAME, bvalue);
            choices.add(new TwoFacedObject(name, bvalue));
            if (bvalue.equals(value)) {
                index = i;
            }
        }
        JList l = new JList(choices);
        l.setVisibleRowCount(rows);
        l.setSelectedIndex(index);
        associate(l, field);
        JScrollPane scroller = new JScrollPane(l);
        return scroller;
    }


    /**
     * _more_
     *
     * @param field
     * @return _more_
     */
    private Component processRadio(Element field) {
        String      value    = XmlUtil.getAttribute(field, ATTR_VALUE, "");
        ButtonGroup group    = new ButtonGroup();
        List        buttons  = new ArrayList();
        NodeList    children = XmlUtil.getElements(field, TAG_CHOICE);
        for (int i = 0; i < children.getLength(); i++) {
            Element choice = (Element) children.item(i);
            String  bvalue = XmlUtil.getAttribute(choice, ATTR_VALUE);
            JRadioButton b = new JRadioButton(XmlUtil.getAttribute(choice,
                                 ATTR_NAME, value));
            b.setActionCommand(bvalue);
            if (bvalue.equals(value)) {
                b.setSelected(true);
            }
            buttons.add(b);
            group.add(b);
        }
        associate(group, field);
        return GuiUtils.hbox(buttons);
    }

    /**
     * _more_
     * @return _more_
     */
    public Component getContents() {
        return contents;
    }

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Provide a choices xml file");
            System.exit(0);
        }
        try {
            final DatasetUI t = new DatasetUI(
                                    XmlUtil.getRoot(
                                        IOUtil.readContents(
                                            new File(args[0]))), null);
            JFrame  f     = new JFrame();
            JButton okBtn = new JButton("Process");
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String url = t.processActionTemplate();
                    System.err.println("URL:" + url);
                    try {
                        System.err.println("Contents:"
                                           + IOUtil.readContents(url));
                    } catch (Exception exc) {
                        System.err.println("oops:" + exc);
                    }
                }
            });
            JPanel contents = GuiUtils.centerBottom(t.getContents(), okBtn);

            f.getContentPane().add(contents);
            f.pack();
            f.show();
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
        }
    }

}

