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


import org.w3c.dom.Element;

import org.w3c.dom.Node;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;



import ucar.unidata.idv.*;


import ucar.unidata.ui.XmlTree;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;


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
 * This handles the menu bar xml (from idv/resources/defaultmenu.xml)
 * to include idv commands, etc, into a {@link XmlChooser} chooser.
 *
 * @author IDV development team
 * @version $Revision: 1.10 $Date: 2007/06/28 17:13:04 $
 */


public class MenuHandler extends XmlHandler {

    /** Xml tag name for menus */
    public static final String TAG_MENUS = "menus";

    /** Xml tag name for menu */
    public static final String TAG_MENU = "menu";

    /** Xml tag name for menuitems */
    public static final String TAG_MENUITEM = "menuitem";

    /** The action attribute in  menuitems */
    public static final String ATTR_ACTION = "action";

    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public MenuHandler(XmlChooser chooser, Element root, String path) {
        super(chooser, root, path);
    }



    /**
     * Make the GUI
     *
     *  @return The UI component
     */
    public JComponent doMakeContents() {
        XmlTree tree = new XmlTree(root, true) {
            public void doDoubleClick(XmlTree theTree, Element e) {
                if (e.getTagName().equals(TAG_MENUITEM)) {
                    chooser.handleAction(XmlUtil.getAttribute(e,
                            ATTR_ACTION));
                }
            }

            public void doClick(XmlTree theTree, Element e) {
                chooser.setHaveData(e.getTagName().equals(TAG_MENUITEM));
            }

        };
        tree.addTagsToProcess(Misc.newList(TAG_MENUS, TAG_MENU,
                                           TAG_MENUITEM));
        JPanel topPanel = new JPanel();
        return GuiUtils.inset(GuiUtils.topCenter(topPanel,
                tree.getScroller()), 5);

    }


    /**
     *  The user  has pressed the 'Load' button. Check if a  node is selected
     * and process the xml action attribute
     */
    public void doLoad() {
        Element element = tree.getSelectedElement();
        if (element != null) {
            String action = XmlUtil.getAttribute(element, ATTR_ACTION,
                                (String) null);
            if (action != null) {
                chooser.handleAction(action);
            }
        }


    }



}
