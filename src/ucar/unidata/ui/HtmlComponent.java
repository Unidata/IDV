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


import ucar.unidata.collab.PropertiedThing;
import ucar.unidata.util.GuiUtils;



import java.awt.*;
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


import org.w3c.dom.Element;
import ucar.unidata.xml.XmlUtil;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class HtmlComponent extends ComponentHolder {

    public static final String ATTR_TEXT = "text";

    private String text;
    protected JTextArea textArea;
    protected JLabel label;


    /**
     * _more_
     */
    public HtmlComponent() {}


    /**
     * _more_
     *
     * @param name _more_
     */
    public HtmlComponent(String name, String text) {
        super(name);
        this.text = text;
    }


    public void initWith(Element node) {
        if(XmlUtil.hasAttribute(node, ATTR_TEXT)) {
            setText(XmlUtil.getAttribute(node, ATTR_TEXT,""));
        } else {
            setText(XmlUtil.getChildText(node));
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeContents() {
        label =new JLabel("");
        label.setText("<html>"+text+"</html>");
        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTypeName() {
        return "Text Component";
    }



    /**
     * _more_
     *
     * @param comps _more_
     * @param tabIdx _more_
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        comps.add(GuiUtils.top(GuiUtils.rLabel("Text:")));
        textArea = new JTextArea(text,5,30);
        comps.add(GuiUtils.makeScrollPane(textArea,200,100));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean applyProperties() {
        if(!super.applyProperties()) return false;
        text = textArea.getText();
        label.setText("<html>"+text+"</html>");
        return true;
    }


/**
Set the Text property.

@param value The new value for Text
**/
public void setText (String value) {
        text = value;
}

/**
Get the Text property.

@return The Text
**/
public String getText () {
        return text;
}

}

