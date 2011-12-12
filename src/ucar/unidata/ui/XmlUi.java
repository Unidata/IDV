/**
 * $Id: XmlUi.java,v 1.62 2007/08/06 17:04:03 jeffmc Exp $
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;



import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;



/**
 *  This class can create a UI from a "skin" xml.
 *  See the file <a href=../apps/example/docs/xmlui.html>../apps/example/docs/xmlui.html</a>
 *  for further details.
 */

public class XmlUi implements ActionListener, ItemListener {

    /** More clear then going (String) null. */
    public static final String NULLSTRING = null;

    /** Action command for flpping through a card layout */
    public static final String ACTION_UI_FLIP = "ui.flip";

    /** Action command for flpping through a card layout */
    public static final String ACTION_UI_FOCUS = "ui.focus";

    /** Action command for popping up a menu */
    public static final String ACTION_MENUPOPUP = "menu.popup";

    /** Action procedure */
    public static final String ACTION_SETTEXT = "ui.setText";

    /** Action procedure */
    public static final String ACTION_SETBORDER = "ui.setBorder";

    /** Action procedure */
    public static final String ACTION_UI_GROUP_NEXT = "ui.group.next";

    /** Action procedure */
    public static final String ACTION_UI_GROUP_PREV = "ui.group.prev";



    /**
     *  Tag and attribute names for the skin xml
     */
    public static final String TAG_SKIN = "skin";


    /** The xml section that holds the styles */
    public static final String TAG_STYLES = "styles";

    /** Holds a style definition */
    public static final String TAG_STYLE = "style";

    /** xml tag */
    public static final String TAG_UI = "ui";

    /** xml tag */
    public static final String TAG_CARDPANEL = "cardpanel";

    /** Xml tag for creating a combobox */
    public static final String TAG_COMBOBOX = "combobox";

    /** xml tag */
    public static final String TAG_COMPONENTS = "components";

    /** xml tag */
    public static final String TAG_COMPONENT = "component";

    /** xml tag */
    public static final String TAG_BUTTON = "button";

    /** xml tag */
    public static final String TAG_RADIO = "radio";

    /** xml tag */
    public static final String TAG_CHECKBOX = "checkbox";

    /** xml tag */
    public static final String TAG_FILLER = "filler";

    /** For showing html text. Uses url attribute or contained text */
    public static final String TAG_HTML = "html";

    /** xml tag */
    public static final String TAG_IMAGE = "image";

    /** xml tag */
    public static final String TAG_IMPORT = "import";

    /** xml tag */
    public static final String TAG_ITEM = "item";

    /** xml tag */
    public static final String TAG_LABEL = "label";

    /** xml tag */
    public static final String TAG_MENU = "menu";

    /** xml tag */
    public static final String TAG_MENUPOPUP = "menupopup";

    /** xml tag */
    public static final String TAG_MENUBAR = "menubar";

    /** xml tag */
    public static final String TAG_MENUITEM = "menuitem";

    /** xml tag */
    public static final String TAG_PANEL = "panel";

    /** xml tag */
    public static final String TAG_PROPERTIES = "properties";

    /** xml tag */
    public static final String TAG_PROPERTY = "property";

    /** xml tag */
    public static final String TAG_SCROLLER = "scroller";

    /** xml tag */
    public static final String TAG_SEPARATOR = "separator";

    /** xml tag */
    public static final String TAG_SPLITPANE = "splitpane";

    /** xml tag */
    public static final String TAG_TABBEDPANE = "tabbedpane";

    /** xml tag */
    public static final String TAG_BUTTONTABBEDPANE = "buttontabbedpane";

    /** xml tag */
    public static final String TAG_TREEPANEL = "treepanel";

    /** xml tag */
    public static final String TAG_TEXTINPUT = "textinput";

    /** xml tag */
    public static final String TAG_TOGGLEBUTTON = "togglebutton";

    /** xml tag */
    public static final String TAG_TOOLBAR = "toolbar";

    /** xml attribute name */
    public static final String ATTR_ACTION = "action";

    /** Category attribute for treepanel */
    public static final String ATTR_CATEGORY = "category";

    /** Used to define that the component is the category component for a tree panel */
    public static final String ATTR_CATEGORYCOMPONENT = "categorycomponent";


    /** xml attribute name */
    public static final String ATTR_ACTIONTEMPLATE = "actiontemplate";

    /** xml attribute name */
    public static final String ATTR_ALIGN = "align";


    /** xml attribute name */
    public static final String ATTR_HPOSITION = "hposition";

    /** xml attribute name */
    public static final String ATTR_VPOSITION = "vposition";


    /** xml attribute name */
    public static final String ATTR_ANCHOR = "anchor";

    /** xml attribute name */
    public static final String ATTR_BGCOLOR = "bgcolor";

    /** xml attribute name */
    public static final String ATTR_BORDER = "border";

    /** xml attribute name */
    public static final String ATTR_BORDER_TITLE = "border_title";

    /** xml attribute name */
    public static final String ATTR_BORDER_COLOR = "border_color";

    /** xml attribute name */
    public static final String ATTR_BORDER_THICKNESS = "border_thickness";


    /** xml attribute name */
    public static final String ATTR_BORDER_INSET = "border_inset";

    /** xml attribute name */
    public static final String ATTR_BORDER_HINSET = "border_hinset";

    /** xml attribute name */
    public static final String ATTR_BORDER_VINSET = "border_vinset";

    /** xml attribute name */
    public static final String ATTR_BORDER_TOP = "border_top";

    /** xml attribute name */
    public static final String ATTR_BORDER_LEFT = "border_left";

    /** xml attribute name */
    public static final String ATTR_BORDER_BOTTOM = "border_bottom";

    /** xml attribute name */
    public static final String ATTR_BORDER_RIGHT = "border_right";

    /** xml attribute name */
    public static final String ATTR_CLASS = "class";

    /** xml attribute name */
    public static final String ATTR_TAGCLASS = "tagclass";


    /** xml attribute name */
    public static final String ATTR_COLS = "cols";

    /** xml attribute name */
    public static final String ATTR_COLWIDTHS = "colwidths";

    /** xml attribute name */
    public static final String ATTR_CONTINUOUS = "continuous";

    /** xml attribute name */
    public static final String ATTR_ONETOUCHEXPANDABLE = "onetouchexpandable";

    /** xml attribute name */
    public static final String ATTR_DEFAULT = "default";

    /** xml attribute name */
    public static final String ATTR_DIVIDER = "divider";

    /** xml attribute name */
    public static final String ATTR_DOWNIMAGE = "downimage";

    /** Used for the combobox tag to define if the combobox is editable */
    public static final String ATTR_EDITABLE = "editable";

    /** xml attribute name */
    public static final String ATTR_EVENT = "event";

    /** xml attribute name */
    public static final String ATTR_FLOAT = "float";

    /** xml attribute name */
    public static final String ATTR_FGCOLOR = "fgcolor";

    /** xml attribute name */
    public static final String ATTR_FILL = "fill";

    /** xml attribute name */
    public static final String ATTR_FONTSIZE = "fontsize";

    /** xml attribute name */
    public static final String ATTR_FONTFACE = "fontface";

    /** xml attribute name */
    public static final String ATTR_FONTSTYLE = "fontstyle";

    /** xml attribute name */
    public static final String ATTR_GROUP = "group";


    /** xml attribute name */
    public static final String ATTR_HEIGHT = "height";

    /** xml attribute name */
    public static final String ATTR_HSCROLL = "hscroll";

    /** xml attribute name */
    public static final String ATTR_IMAGEWIDTH = "imagewidth";

    /** xml attribute name */
    public static final String ATTR_IMAGEHEIGHT = "imageheight";


    /** xml attribute name */
    public static final String ATTR_KEYPRESS = "keypress";

    /** xml attribute name */
    public static final String ATTR_MOUSE_ENTER = "mouse_enter";

    /** xml attribute name */
    public static final String ATTR_MOUSE_EXIT = "mouse_exit";

    /** xml attribute name */
    public static final String ATTR_MOUSE_CLICK = "mouse_click";

    /** xml attribute name */
    public static final String ATTR_NAME = "name";


    /** xml attribute name */
    public static final String ATTR_PREF_HEIGHT = "pref_height";

    /** Top space for insets */
    public static final String ATTR_TSPACE = "topspace";

    /** Bottom space for insets */
    public static final String ATTR_BSPACE = "bottomspace";

    /** Left space for insets */
    public static final String ATTR_LSPACE = "leftspace";

    /** Right space for insets */
    public static final String ATTR_RSPACE = "rightspace";


    /** xml attribute name */
    public static final String ATTR_HSPACE = "hspace";

    /** xml attribute name */
    public static final String ATTR_ID = "id";

    /** xml attribute name */
    public static final String ATTR_IDREF = "idref";

    /** xml attribute name */
    public static final String ATTR_IMAGE = "image";


    /** xml attribute name */
    public static final String ATTR_LABEL = "label";

    /** xml attribute name */
    public static final String ATTR_LAYOUT = "layout";

    /** xml attribute name */
    public static final String ATTR_MARGIN = "margin";

    /** xml attribute name */
    public static final String ATTR_MNEMONIC = "mnemonic";

    /** xml attribute name */
    public static final String ATTR_ORIENTATION = "orientation";

    /** xml attribute name */
    public static final String ATTR_OVERIMAGE = "overimage";

    /** xml attribute name */
    public static final String ATTR_PLACE = "place";

    /** xml attribute name */
    public static final String ATTR_PREF_WIDTH = "pref_width";

    /** xml attribute name */
    public static final String ATTR_RESIZEWEIGHT = "resizeweight";

    /** xml attribute name */
    public static final String ATTR_ROWHEIGHTS = "rowheights";

    /** xml attribute name */
    public static final String ATTR_ROWS = "rows";

    /** xml attribute name */
    public static final String ATTR_SELECTIMAGE = "selectimage";

    /** xml attribute name */
    public static final String ATTR_SPACE = "space";

    /** xml attribute name */
    public static final String ATTR_TABNESTED = "tabnested";

    /** xml attribute name */
    public static final String ATTR_TABPLACE = "tabplace";

    /** xml attribute name */
    public static final String ATTR_TABPAD = "tabpad";

    /** xml attribute name */
    public static final String ATTR_TABINSETS = "tabinsets";

    /** xml attribute name */
    public static final String ATTR_TABBORDERLEFT = "tabborderleft";

    /** xml attribute name */
    public static final String ATTR_TABBORDERBOTTOM = "tabborderbottom";

    /** xml attribute name */
    public static final String ATTR_TABBORDERRIGHT = "tabborderright";

    /** xml attribute name */
    public static final String ATTR_TABBORDERTOP = "tabbordertop";

    /** xml attribute name */
    public static final String ATTR_ICON = "icon";


    /** xml attribute name */
    public static final String ATTR_TEMPLATE = "template";

    /** xml attribute name */
    public static final String ATTR_TEXT = "text";

    /** xml attribute name */
    public static final String ATTR_TITLE = "title";

    /** xml attribute name */
    public static final String ATTR_TOOLTIP = "tooltip";

    /** xml attribute name */
    public static final String ATTR_TABTOOLTIP = "tabtooltip";

    /** xml attribute name */
    public static final String ATTR_URL = "url";

    /** xml attribute name */
    public static final String ATTR_VALUE = "value";

    /** xml attribute name */
    public static final String ATTR_VSCROLL = "vscroll";

    /** xml attribute name */
    public static final String ATTR_VSPACE = "vspace";

    /** xml attribute name */
    public static final String ATTR_WIDTH = "width";

    /** xml attribute name */
    public static final String ATTR_TREEWIDTH = "treewidth";

    /** xml attribute name */
    public static final String ATTR_USESPLITPANE = "usesplitpane";

    /** xml attribute name */
    public static final String ATTR_X = "x";

    /** xml attribute name */
    public static final String ATTR_Y = "y";




    /** The button border */
    private Border buttonBorder;

    /** border type */
    public static final String BORDER_BUTTON = "button";


    /** border type */
    public static final String BORDER_TITLED = "titled";

    /** border type */
    public static final String BORDER_BEVEL = "bevel";

    /** border type */
    public static final String BORDER_BEVEL_LOWERED = "bevel_lowered";

    /** border type */
    public static final String BORDER_BEVEL_RAISED = "bevel_raised";

    /** border type */
    public static final String BORDER_EMPTY = "empty";

    /** border type */
    public static final String BORDER_LINE = "line";

    /** border type */
    public static final String BORDER_ETCHED = "etched";

    /** border type */
    public static final String BORDER_ETCHED_RAISED = "etched_raised";

    /** border type */
    public static final String BORDER_ETCHED_LOWERED = "etched_lowered";

    /** border type */
    public static final String BORDER_MATTE = "matte";


    /** layout type */
    public static final String LAYOUT_BORDER = "border";

    /** layout type */
    public static final String LAYOUT_CARD = "card";

    /** layout type */
    public static final String LAYOUT_FLOW = "flow";

    /** layout type */
    public static final String LAYOUT_GRAPHPAPER = "graphpaper";

    /** layout type */
    public static final String LAYOUT_GRID = "grid";

    /** layout type */
    public static final String LAYOUT_GRIDBAG = "gridbag";

    /** layout type */
    public static final String LAYOUT_INSET = "inset";

    /** layout type */
    public static final String LAYOUT_WRAP = "wrap";


    /** flow layout names */
    public static final String[] FLOWLAYOUT_NAMES = { "left", "center",
            "leading", "right", "trailing" };


    /** corresponding flow layout values */
    public static final int[] FLOWLAYOUT_VALUES = { FlowLayout.LEFT,
            FlowLayout.CENTER, FlowLayout.LEADING, FlowLayout.RIGHT,
            FlowLayout.TRAILING };


    /** fill names */
    public static final String[] FILL_NAMES = { "h", "v", "both", "none" };

    /** corresponding fill valus */
    public static final int[] FILL_VALUES = { GridBagConstraints.HORIZONTAL,
            GridBagConstraints.VERTICAL, GridBagConstraints.BOTH,
            GridBagConstraints.NONE };


    /** tab place names */
    public static final String[] TABPLACE_NAMES = { "top", "bottom", "left",
            "right" };

    /** corresponding tab place values */
    public static final int[] TABPLACE_VALUES = { JTabbedPane.TOP,
            JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT };

    /** tool bar orientation names */
    public static final String[] TOOLBAR_NAMES = { "h", "v" };

    /** corresponding toolbar orientation values */
    public static final int[] TOOLBAR_VALUES = { JToolBar.HORIZONTAL,
            JToolBar.VERTICAL };

    /** split pane orientation */
    public static final String[] SPLITPANE_NAMES = { "h", "v" };

    /** corresponding split pane values */
    public static final int[] SPLITPANE_VALUES = { JSplitPane
                                                     .HORIZONTAL_SPLIT,
            JSplitPane.VERTICAL_SPLIT };




    /** grid bag anchors */
    public static final String[] ANCHOR_NAMES = {
        "nw", "w", "sw", "ne", "e", "se", "n", "c", "s"
    };

    /** corresponding grid bag anchor values */
    public static final int[] ANCHOR_VALUES = {
        GridBagConstraints.NORTHWEST, GridBagConstraints.WEST,
        GridBagConstraints.SOUTHWEST, GridBagConstraints.NORTHEAST,
        GridBagConstraints.EAST, GridBagConstraints.SOUTHEAST,
        GridBagConstraints.NORTH, GridBagConstraints.CENTER,
        GridBagConstraints.SOUTH
    };



    /** alignment names */
    public static final String[] HPOS_NAMES = { "left", "right", "center",
            "leading", "trailing" };


    /** corresponding alignment values */
    public static final int[] HPOS_VALUES = { SwingConstants.LEFT,
            SwingConstants.RIGHT, SwingConstants.CENTER,
            SwingConstants.LEADING, SwingConstants.TRAILING };

    /** alignment names */
    public static final String[] VPOS_NAMES = { "center", "top", "bottom" };


    /** corresponding alignment values */
    public static final int[] VPOS_VALUES = { SwingConstants.CENTER,
            SwingConstants.TOP, SwingConstants.BOTTOM };


    /** alignment names */
    public static final String[] ALIGN_NAMES = { "left", "right", "center" };

    /** corresponding alignment values */
    public static final int[] ALIGN_VALUES = { JLabel.LEFT, JLabel.RIGHT,
            JLabel.CENTER };

    /** scroll type names */
    public static final String[] SCROLL_NAMES = { "asneeded", "always",
            "never" };

    /** corresponding scroll type values */
    public static final int[] VSCROLL_VALUES = { JScrollPane
                                                   .VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, };

    /** corresponding scroll type values */
    public static final int[] HSCROLL_VALUES = { JScrollPane
                                                   .HORIZONTAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER };



    /** The gui */
    private Component myContents;

    /** root of ui xml */
    private Element root;

    /** root of ui xml */
    private Element startNode;

    /** The TAG_UI node */
    private Element uiNode;

    /** Listener to pass events to */
    private ActionListener actionListener;

    /** Listener to pass events to */
    private ActionHandler actionHandler;


    /**
     *  This is an identifier user to give unique ids for skin components
     *  that do not have an id attribute.
     */
    private int xmlNodeId = 0;

    /** All components */
    private List components = new ArrayList();


    /**
     *  Maps id->Element
     */
    private Hashtable idToElement = new Hashtable();


    /** Mapping of id to ButtonGroup */
    private Hashtable buttonGroups = new Hashtable();


    /**
     *  Maps class->Element
     */
    private Hashtable classToStyle = new Hashtable();


    /**
     *  Skin id to created component
     */
    Hashtable idToComponent = new Hashtable();

    /**
     *  Holds the properties
     *
     */
    protected Hashtable properties = new Hashtable();


    /**
     *  Panel component to the list of Element that define its children components.
     */
    private Hashtable containerToNodeList = new Hashtable();

    /**
     *  java.awt.Component to Element
     */
    private Hashtable compToNode = new Hashtable();

    /**
     *  Element to awt.Component
     */
    private Hashtable nodeToComponent = new Hashtable();

    /**
     *  Map the awt Component to its parent. We keep this here
     *  because some of these components get removed from their parent.
     */
    private Hashtable componentToParent = new Hashtable();

    /**
     *  Maps the awt Component to an action command String
     *  defined in the skin xml. Mostly used for text fields
     */
    private Hashtable componentToAction = new Hashtable();


    /**
     * default ctor
     *
     */
    public XmlUi() {
        this((Element) null, new Hashtable(), null, null);
    }

    /**
     * ctor
     *
     * @param xml The raw xml
     * @param idToComponent pre-defined id to component mapping. Allows you to
     * have your own components that are referenced by the xml
     * @param actionListener Listener to pass events to
     *
     */
    public XmlUi(String xml, Hashtable idToComponent,
                 ActionListener actionListener) {
        this((Element) null, idToComponent, actionListener, null);
        try {
            root = XmlUtil.getRoot(xml);
        } catch (Exception exc) {
            LogUtil.logException("XmlUi: creating xml", exc);
        }
        initXml();
    }


    /**
     * ctor
     *
     * @param root xml
     * @param actionListener Listener to pass events to
     *
     */
    public XmlUi(Element root, ActionListener actionListener) {
        this(root, null, actionListener, null);
    }

    /**
     *  Create the XmlUi processor. Root should point to a "skin"
     *  node. idToComponent is a Hashtable with application specific collection
     *  of id->java.awt.Component pairs. (May  be null).
     *  actionListener - > route actions to it. (May be null).
     *
     * @param root xml root
     * @param idToComponent pre-defined id to component mapping. Allows you to
     * have your own components that are referenced by the xml
     * @param actionListener Listener to pass events to
     * @param initProperties initial properties
     */

    public XmlUi(Element root, Hashtable idToComponent,
                 ActionListener actionListener, Hashtable initProperties) {
        this(root, null, idToComponent, actionListener, initProperties);
    }

    public XmlUi(Element root, Element startNode, Hashtable idToComponent,
                 ActionListener actionListener, Hashtable initProperties) {

        this.root           = root;
        this.startNode  = startNode;
        this.idToComponent  = ((idToComponent == null)
                               ? new Hashtable()
                               : idToComponent);
        this.actionListener = actionListener;
        if (initProperties != null) {
            properties.putAll(initProperties);
        }
        initXml();
    }


    public void setStartNode(Element node) {
        this.startNode = node;
    }


    /**
     * The destructor
     */
    public void dispose() {
        myContents          = null;
        root                = null;
        uiNode              = null;
        actionListener      = null;
        actionHandler       = null;
        idToElement         = null;
        buttonGroups        = null;
        classToStyle        = null;
        idToComponent       = null;
        properties          = null;
        containerToNodeList = null;
        compToNode          = null;
        nodeToComponent     = null;
        componentToParent   = null;
        componentToAction   = null;
    }

    /**
     * Interface to pass ActionEvents to along with this XmlUI object
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.62 $
     */
    public interface ActionHandler {

        /**
         * Handle the event
         *
         * @param event event
         * @param xmlUI me
         */
        public void handleAction(ActionEvent event, XmlUi xmlUI);
    }


    /**
     * Initialize
     */
    private void initXml() {
        if (root == null) {
            return;
            //throw new IllegalStateException("No xml root defined");
        }
        String rootTag = root.getTagName();

        if (rootTag.equals(TAG_PANEL) || rootTag.equals(TAG_TABBEDPANE)
                || rootTag.equals(TAG_BUTTONTABBEDPANE)
                || rootTag.equals(TAG_TREEPANEL)
                || rootTag.equals(TAG_SPLITPANE)
                || rootTag.equals(TAG_SCROLLER)
                || rootTag.equals(TAG_TOOLBAR)) {
            return;
        }
        if (rootTag.equals(TAG_UI)) {
            uiNode = root;
        }
        if (uiNode == null) {
            uiNode = XmlUtil.getElement(root, TAG_UI);
            List propertiesNodes = XmlUtil.findChildren(root, TAG_PROPERTIES);
            for (int i = 0; i < propertiesNodes.size(); i++) {
                Element propsNode = (Element) propertiesNodes.get(i);
                List children = XmlUtil.findChildren(propsNode, TAG_PROPERTY);
                for (int j = 0; j < children.size(); j++) {
                    Element propNode = (Element) children.get(j);
                    properties.put(XmlUtil.getAttribute(propNode, ATTR_NAME),
                                   XmlUtil.getAttribute(propNode,
                                       ATTR_VALUE));
                }
            }


            initializeComponents(root);

            List stylesNodes = XmlUtil.findChildren(root, TAG_STYLES);
            for (int stylesNodeIdx = 0; stylesNodeIdx < stylesNodes.size();
                    stylesNodeIdx++) {
                Element stylesNode = (Element) stylesNodes.get(stylesNodeIdx);
                List styleNodes = XmlUtil.findChildren(stylesNode, TAG_STYLE);
                for (int styleNodeIdx = 0; styleNodeIdx < styleNodes.size();
                        styleNodeIdx++) {
                    Element styleNode =
                        (Element) styleNodes.get(styleNodeIdx);
                    classToStyle.put(XmlUtil.getAttribute(styleNode,
                            ATTR_CLASS), styleNode);
                }
            }
        }
    }


    /**
     * initialize
     *
     * @param root xml root
     */
    protected void initializeComponents(Element root) {
        List compsNodes = XmlUtil.findChildren(root, TAG_COMPONENTS);
        for (int compNodeIdx = 0; compNodeIdx < compsNodes.size();
                compNodeIdx++) {
            Element  componentsNode = (Element) compsNodes.get(compNodeIdx);
            NodeList children       = XmlUtil.getElements(componentsNode);
            for (int i = 0; i < children.getLength(); i++) {
                Element component = (Element) children.item(i);
                String  id        = getAttr(component, ATTR_ID,
                                            (String) null);
                if (id != null) {
                    addComponent(id, component);
                }
            }
        }
    }


    /**
     *  Does this object hold this component
     *
     * @param id component id
     * @return  do we currently have this component
     */
    public boolean hasComponent(String id) {
        return (idToComponent.get(id) != null);
    }

    /**
     * Add the component
     *
     * @param id id
     * @param component component
     */
    public void addComponent(String id, Element component) {
        idToElement.put(id, component);
    }


    /**
     *  Returns if this object already has the component id.
     *  if throwException is true the throw an IllegalArgumentException
     *  if we have the component.
     *
     * @param id component id
     * @param throwException If we don't have the component should we throw an exception
     * @return Do we have the component
     */
    private boolean hasComponent(String id, boolean throwException) {
        if (hasComponent(id)) {
            if (throwException) {
                throw new IllegalArgumentException("Already have component:"
                        + id);
            }
            return true;
        }
        return false;
    }

    /**
     *  Add the given component to the idToComponent map.
     *  If the component already exists throw an IllegalStateException
     *
     * @param id
     * @param comp
     */
    public void addComponent(String id, Component comp) {
        idToComponent.put(id, comp);
    }

    /**
     *  Return the Component identified by id
     *
     * @param id component id
     * @return The component
     */
    public Component getComponent(String id) {
        return (Component) idToComponent.get(id);
    }


    /**
     * Get the list of components created
     *
     * @return components
     */
    public List getComponents() {
        return components;
    }


    /**
     *  Return the java.awt.Label align value that corresponds to
     *  the "align" attribute in the given Element
     *
     * @param node xml node to look at
     * @return alignement value
     */
    int getAlign(Element node) {
        return getAlign(getAttr(node, ATTR_ALIGN, "CENTER"));
    }

    /**
     * Get the align value from the given string name
     *
     * @param align name
     * @return align value
     */
    public int getAlign(String align) {
        return findValue(align, ALIGN_NAMES, ALIGN_VALUES, JLabel.LEFT);
    }



    /**
     * Get the value of the given attribute on the node. This will first see
     * if the node has the attribute. If not this will look into the styles.
     * If still no value we will look up the xml tree to see if there is any
     * attributes on nodes with the inherit prefix: i:attr_name
     * If we find a value we will then convert any macros (defined with %...%)
     *
     * @param node node
     * @param attr attribute name
     * @return value or null if none found
     */
    public String getAttr(Element node, String attr) {
        String v = XmlUtil.getAttribute(node, attr, NULLSTRING);
        if (v == null) {
            String styleClass = XmlUtil.getAttribute(node, ATTR_TAGCLASS,
                                    NULLSTRING);

            if (styleClass == null) {
                styleClass = XmlUtil.getAttribute(node, ATTR_CLASS,
                        NULLSTRING);
            }
            if (styleClass != null) {
                Element styleNode = (Element) classToStyle.get(styleClass);
                if (styleNode != null) {
                    v = XmlUtil.getAttribute(styleNode, attr, NULLSTRING);
                }
            }
            if (v == null) {
                v = XmlUtil.getAttributeFromTree(node.getParentNode(),
                        inheritNameOldWay(attr));
            }
            if (v == null) {
                v = XmlUtil.getAttributeFromTree(node.getParentNode(),
                        inheritName(attr));
            }
        }
        if ((v != null) && (v.startsWith("prop:") || v.startsWith("prop_"))) {
            v = getProperty(v.substring(5));
        }

        if (v != null) {
            int idx = v.indexOf("%");
            if (idx >= 0) {
                //Do this the easy (but not as quick) way.
                for (Enumeration keys = properties.keys();
                        keys.hasMoreElements(); ) {
                    String key   = (String) keys.nextElement();
                    String value = (String) properties.get(key);
                    if (value != null) {
                        v = StringUtil.replace(v, "%" + key + "%", value);
                    }
                }
            }
        }
        return v;
    }

    /**
     * Get the inherited attribute  name
     *
     * @param attr attr name
     * @return i:attr
     */
    public static String inheritNameOldWay(String attr) {
        return "i:" + attr;
    }

    public static String inheritName(String attr) {
        return "i_" + attr;
    }


    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public int getAttr(Element node, String attr, int dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        return Integer.decode(v).intValue();
    }

    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public double getAttr(Element node, String attr, double dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        return Double.parseDouble(v);
    }

    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public boolean getAttr(Element node, String attr, boolean dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        return new Boolean(v).booleanValue();
    }


    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public String getAttr(Element node, String attr, String dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        return v;
    }

    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public Color getAttr(Element node, String attr, Color dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        return GuiUtils.decodeColor(v, dflt);
    }


    /**
     * Get the attribute value
     *
     * @param node node
     * @param attr attr
     * @param dflt default
     * @return value or dflt
     */
    public ImageIcon getAttr(Element node, String attr, ImageIcon dflt) {
        String v = getAttr(node, attr);
        if (v == null) {
            return dflt;
        }
        Image image = getImage(v);
        if (image == null) {
            return null;
        }
        return new ImageIcon(image);
    }


    /**
     * Create the border
     *
     * @param type border type
     * @param node xml node that holds any other attributes needed
     * @return The border
     */
    public Border getBorder(String type, Element node) {
        if (type.equals(BORDER_TITLED)) {
            return BorderFactory.createTitledBorder(getAttr(node,
                    ATTR_BORDER_TITLE, "no title"));
        }

        int inset  = getAttr(node, ATTR_BORDER_INSET, 0);
        int hinset = getAttr(node, ATTR_BORDER_HINSET, inset);
        int vinset = getAttr(node, ATTR_BORDER_VINSET, inset);
        int top    = getAttr(node, ATTR_BORDER_TOP, vinset);
        int bottom = getAttr(node, ATTR_BORDER_BOTTOM, vinset);
        int left   = getAttr(node, ATTR_BORDER_LEFT, hinset);
        int right  = getAttr(node, ATTR_BORDER_RIGHT, hinset);


        if (type.equals(BORDER_BUTTON)) {
            //            return BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            //                                                      BorderFactory.createEmptyBorder(5,5,5,5));
            return BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

        }

        if (type.equals(BORDER_BEVEL_LOWERED) || type.equals(BORDER_BEVEL)) {
            return BorderFactory.createBevelBorder(BevelBorder.LOWERED);
        }

        if (type.equals(BORDER_BEVEL_RAISED)) {
            return BorderFactory.createBevelBorder(BevelBorder.RAISED);
        }

        if (type.equals(BORDER_EMPTY)) {
            return BorderFactory.createEmptyBorder(top, left, bottom, right);
        }
        if (type.equals(BORDER_LINE)) {
            return BorderFactory.createLineBorder(getAttr(node,
                    ATTR_BORDER_COLOR, Color.black), getAttr(node,
                        ATTR_BORDER_THICKNESS, 1));

        }
        if (type.equals(BORDER_ETCHED_RAISED) || type.equals(BORDER_ETCHED)) {
            return BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        }
        if (type.equals(BORDER_ETCHED_LOWERED)) {
            return BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        }
        if (type.equals(BORDER_MATTE)) {
            return BorderFactory.createMatteBorder(top, left, bottom, right,
                    getAttr(node, ATTR_BORDER_COLOR, Color.gray));

        }
        System.err.println("Unknown border:" + type);
        return BorderFactory.createEmptyBorder();

    }

    /**
     *  Set bgcolor, fgcolor and font attributes, defined in the Element,
     *  on the given component.
     *
     * @param comp
     * @param node
     */
    void setAttrs(Component comp, Element node) {
        if (comp instanceof JComponent) {
            JComponent jcomp   = (JComponent) comp;
            String     tooltip = getAttr(node, ATTR_TOOLTIP, NULLSTRING);
            if (tooltip != null) {
                jcomp.setToolTipText(tooltip);
            }

            String border = getAttr(node, ATTR_BORDER, NULLSTRING);
            if (border != null) {
                jcomp.setBorder(getBorder(border, node));
            }

            int pw = getAttr(node, ATTR_PREF_WIDTH, -1);
            int ph = getAttr(node, ATTR_PREF_HEIGHT, -1);
            if ((pw != -1) && (ph != -1)) {
                jcomp.setPreferredSize(new Dimension(pw, ph));
            }

        }
        int w = getAttr(node, ATTR_WIDTH, -1);
        int h = getAttr(node, ATTR_HEIGHT, -1);
        if ((w != -1) && (h != -1)) {
            comp.setSize(new Dimension(w, h));
        }

        Color bgColor = getAttr(node, ATTR_BGCOLOR, (Color) null);
        if (bgColor != null) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setOpaque(true);
            }
            comp.setBackground(bgColor);
        }
        Color fgColor = getAttr(node, ATTR_FGCOLOR, (Color) null);
        if (fgColor != null) {
            comp.setForeground(fgColor);
        }
        String fontSizeStr  = getAttr(node, ATTR_FONTSIZE, NULLSTRING);
        String fontFace     = getAttr(node, ATTR_FONTFACE, NULLSTRING);
        String fontStyleStr = getAttr(node, ATTR_FONTSTYLE, NULLSTRING);

        if ((fontSizeStr != null) || (fontFace != null)
                || (fontStyleStr != null)) {
            Font   f        = comp.getFont();
            int    fontSize = f.getSize();
            int    style    = f.getStyle();
            String face     = f.getFamily();

            if (fontSizeStr != null) {
                if (fontSizeStr.startsWith("+")) {
                    fontSize +=
                        new Integer(fontSizeStr.substring(1)).intValue();
                } else if (fontSizeStr.startsWith("-")) {
                    fontSize -=
                        new Integer(fontSizeStr.substring(1)).intValue();
                } else {
                    fontSize = new Integer(fontSizeStr).intValue();
                }
            }
            if (fontStyleStr != null) {
                fontStyleStr = fontStyleStr.toLowerCase();
                if (fontStyleStr.equals("bold")) {
                    style = Font.BOLD;
                } else if (fontStyleStr.equals("italic")) {
                    style = Font.ITALIC;
                } else if (fontStyleStr.equals("plain")) {
                    style = Font.PLAIN;
                }

            }
            f = new Font(fontFace, style, fontSize);
            comp.setFont(f);
        }
    }




    /**
     * Set the xml root and reinitialize
     *
     * @param root xml root
     */
    public void setRoot(Element root) {
        this.root  = root;
        myContents = null;
        initXml();
    }

    /**
     * Create, if needed, and return the gui
     * @return gui
     */
    public Component getContents() {
        if (myContents == null) {
            myContents = doMakeContents();
        }
        return myContents;
    }


    /**
     *  The root element can either be:
     *  <pre>
     *  &lt;panel&gt;...&lt;/panel&gt;
     *  or:
     *  &lt;tabbedpane&gt;...&lt;/tabbedpane&gt;
     *  or:
     *  &lt;ui&gt;....&lt;/ui&gt;
     *  or:
     *  &lt;some_tag_name&gt;
     *  &lt;ui&gt;...&lt;/ui&gt;
     *  &lt;components&gt;...&lt;/components&gt;
     *  &lt;/some_tag_name&gt;
     *  </pre>
     *  Where the components tag is optional
     * @return gui contents
     */
    private Component doMakeContents() {
        String rootTag = root.getTagName();
        if (rootTag.equals(TAG_PANEL) || rootTag.equals(TAG_TABBEDPANE)
                || rootTag.equals(TAG_BUTTONTABBEDPANE)
                || rootTag.equals(TAG_TREEPANEL)
                || rootTag.equals(TAG_SPLITPANE)
                || rootTag.equals(TAG_SCROLLER)
                || rootTag.equals(TAG_TOOLBAR)) {
            return xmlToUi(root);
        }

        Element initialNode = startNode;
        if (initialNode == null && uiNode != null) {
            //            badState("Error: No <ui> tag found");
            //        }
            NodeList children = XmlUtil.getElements(uiNode);
            if (children.getLength() != 1) {
                badState("Error: <ui> tag must have only one child");
            }
            initialNode = (Element) children.item(0);
        }
        if(initialNode!=null) {
            return xmlToUi(initialNode);
        }
        return xmlToUi(root);
    }

    /**
     * Look up the property
     *
     * @param name property name
     *
     * @return property value
     */
    public String getProperty(String name) {
        return (String) properties.get(name);
    }


    /**
     * The xml nodes can contain an idref field. If so this returns the
     * node that that id defines
     *
     * @param node node
     * @return The node or the referenced node
     */
    private Element getReffedNode(Element node) {
        String idRef = getAttr(node, ATTR_IDREF, NULLSTRING);
        if (idRef == null) {
            return node;
        }

        Element reffedNode = (Element) idToElement.get(idRef);
        if (reffedNode == null) {
            badState("Could not find idref=" + idRef);
        }

        //TODO Make a new copy of the node    reffedNode = reffedNode.copy ();
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attrNode = map.item(i);
            if ( !attrNode.getNodeName().equals(ATTR_IDREF)) {
                reffedNode.setAttribute(attrNode.getNodeName(),
                                        attrNode.getNodeValue());
            }
        }
        return reffedNode;
    }


    /**
     * Convert the given xml node tree to a gui component
     *
     * @param node node
     * @return gui
     */
    protected Component xmlToUi(Element node) {


        if (node.getTagName().equals(TAG_IMPORT)) {
            try {
                Element root = XmlUtil.getRoot(getAttr(node, ATTR_URL),
                                   getClass());
                if (root.getTagName().equals(TAG_SKIN)) {
                    //TODO: Handle the skin/ui/component tags
                }
                Element child =
                    (Element) node.getOwnerDocument().importNode(root, true);
                node.appendChild(child);
                node = child;
            } catch (Exception exc) {
                LogUtil.logException("XmlUi: creating xml from import", exc);
            }
        }

        String id = getAttr(node, ATTR_ID, (String) null);
        if (id == null) {
            id = "nodeid" + (xmlNodeId++);
            node.setAttribute(ATTR_ID, id);
        }

        Component comp = (Component) nodeToComponent.get(node);
        if (comp != null) {
            return comp;
        }
        Component topComp = null;
        comp = (Component) idToComponent.get(id);
        if (comp == null) {
            comp = createComponent(node, id);
            if (comp != null) {
                components.add(comp);
                compToNode.put(comp, node);
                nodeToComponent.put(node, comp);
                idToComponent.put(id, comp);
                final String keyPress = getAttr(node, ATTR_KEYPRESS,
                                            NULLSTRING);
                final Component theComp = comp;
                if (keyPress != null) {
                    final List commands = StringUtil.split(keyPress, ";",
                                              true, true);
                    comp.setFocusable(true);
                    comp.addKeyListener(new KeyListener() {
                        public void keyPressed(KeyEvent event) {
                            String key = event.getKeyText(event.getKeyCode());
                            for (int i = 0; i < commands.size(); i++) {
                                String tmp = commands.get(i).toString();
                                int    idx = tmp.indexOf(":");
                                if (tmp.substring(0, idx).equals(key)) {
                                    actionPerformed(new ActionEvent(theComp,
                                            0, tmp.substring(idx + 1)));
                                }
                            }
                        }

                        public void keyReleased(KeyEvent event) {}

                        public void keyTyped(KeyEvent event) {}
                    });
                }

                final String mouseClick = getAttr(node, ATTR_MOUSE_CLICK,
                                              NULLSTRING);
                final String mouseEnter = getAttr(node, ATTR_MOUSE_ENTER,
                                              NULLSTRING);
                final String mouseExit = getAttr(node, ATTR_MOUSE_EXIT,
                                             NULLSTRING);

                if ((mouseClick != null) || (mouseEnter != null)
                        || (mouseExit != null)) {

                    comp.addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            if (mouseEnter != null) {
                                actionPerformed(new ActionEvent(theComp, 0,
                                        mouseEnter));
                            }
                        }

                        public void mouseExited(MouseEvent e) {
                            if (mouseExit != null) {
                                actionPerformed(new ActionEvent(theComp, 0,
                                        mouseExit));
                            }
                        }

                        public void mouseClicked(MouseEvent e) {
                            if (mouseClick != null) {
                                actionPerformed(new ActionEvent(theComp, 0,
                                        mouseClick));
                            }
                        }
                    });
                }


                String margin = getAttr(node, ATTR_MARGIN, NULLSTRING);
                if ((margin != null) && (comp != null)) {
                    double[] insets = Misc.parseDoubles(margin);
                    if (insets.length == 1) {
                        topComp = GuiUtils.inset(comp, (int) insets[0]);
                    } else if (insets.length == 4) {
                        topComp = GuiUtils.inset(comp,
                                new Insets((int) insets[0], (int) insets[1],
                                           (int) insets[2], (int) insets[3]));
                    }
                }

            }
        }
        if (comp != null) {
            setAttrs(comp, node);
        }
        if (topComp != null) {
            return topComp;
        }
        return comp;

    }

    /**
     * Get an insets from the comma separated string of doubles
     *
     * @param str string
     *
     * @return insets
     */
    private Insets getInsets(String str) {
        if (str == null) {
            return null;
        }
        double[] insets = Misc.parseDoubles(str);
        if (insets.length == 1) {
            return new Insets((int) insets[0], (int) insets[0],
                              (int) insets[0], (int) insets[0]);
        }
        if (insets.length == 4) {
            return new Insets((int) insets[0], (int) insets[1],
                              (int) insets[2], (int) insets[3]);
        }
        return null;
    }


    /** Special value */
    public static final int NOVALUE = -9999999;

    /**
     * Find the corresponding int value in the values array
     * at the index of the v value in the names array
     *
     * @param v value
     * @param names value names
     * @param values values
     * @return The value or, if none found, the value of the first element in the array
     */
    private int findValue(String v, String[] names, int[] values) {
        return findValue(v, names, values, values[0]);
    }


    /**
     * Find the corresponding int value in the values array
     * at the index of the v value in the names array
     *
     * @param v value
     * @param names value names
     * @param values values
     * @param dflt
     * @return The value or, if none found, the dflt
     */
    private int findValue(String v, String[] names, int[] values, int dflt) {
        if (v == null) {
            return dflt;
        }
        v = v.toLowerCase();
        for (int fidx = 0; fidx < names.length; fidx++) {
            if (v.equals(names[fidx])) {
                return values[fidx];
            }
        }
        return dflt;

    }

    /**
     * Some utility
     *
     * @param ht hashtable
     * @param v value
     * @param o object
     * @param names names
     * @param values values
     * @return The ht
     */
    private Hashtable addInteger(Hashtable ht, String v, Object o,
                                 String[] names, int[] values) {
        int value = findValue(v, names, values, NOVALUE);
        if (value == NOVALUE) {
            return ht;
        }
        if (ht == null) {
            ht = new Hashtable();
        }
        ht.put(o, new Integer(value));
        return ht;
    }


    /**
     *  Layout (or relayout) the children of the given Container.
     *
     * @param panel
     * @param node
     * @param xmlChildren
     * @return The container
     */
    private Container layoutContainer(JPanel panel, Element node,
                                      List xmlChildren) {

        panel.removeAll();
        String    panelId     = getAttr(node, ATTR_ID, (String) null);
        String    layout      = getAttr(node, ATTR_LAYOUT, "");
        Hashtable fills       = null;
        Hashtable anchors     = null;
        List      children    = new ArrayList();
        List      nodes       = new ArrayList();
        int       space       = getAttr(node, ATTR_SPACE, 0);
        int       hspace      = getAttr(node, ATTR_HSPACE, space);
        int       vspace      = getAttr(node, ATTR_VSPACE, space);
        int       leftSpace   = getAttr(node, ATTR_LSPACE, hspace);
        int       rightSpace  = getAttr(node, ATTR_RSPACE, hspace);
        int       topSpace    = getAttr(node, ATTR_TSPACE, vspace);
        int       bottomSpace = getAttr(node, ATTR_BSPACE, vspace);
        int       rows        = getAttr(node, ATTR_ROWS, 0);
        int       cols        = getAttr(node, ATTR_COLS, 1);

        String    defaultComp = getAttr(node, ATTR_DEFAULT, "nocomp");
        if (panel instanceof CardPanel) {
            panel.setLayout(new BorderLayout());
        } else if (layout.equals(LAYOUT_BORDER)) {
            panel.setLayout(new BorderLayout());
        } else if (layout.equals(LAYOUT_CARD)) {
            panel.setLayout(new CardLayout());
        } else if (layout.equals(LAYOUT_FLOW)) {
            int align = findValue(getAttr(node, ATTR_ALIGN, NULLSTRING),
                                  FLOWLAYOUT_NAMES, FLOWLAYOUT_VALUES);
            panel.setLayout(new FlowLayout(align, hspace, vspace));
        } else if (layout.equals(LAYOUT_GRAPHPAPER)) {
            panel.setLayout(new GraphPaperLayout(new Dimension(rows, cols)));
        } else if (layout.equals(LAYOUT_GRID)) {
            panel.setLayout(new GridLayout(rows, cols, hspace, vspace));
        }

        for (int i = 0; i < xmlChildren.size(); i++) {
            Element childElement =
                getReffedNode((Element) xmlChildren.get(i));
            Component childComponent = xmlToUi(childElement);
            if (childComponent == null) {
                continue;
            }
            componentToParent.put(childComponent, panel);
            children.add(childComponent);
            nodes.add(childElement);
            //      if (!childComponent.isVisible ()) continue;
            if (layout.equals(LAYOUT_BORDER)) {
                String place = getAttr(childElement, ATTR_PLACE, "Center");
                panel.add(place, childComponent);
            } else if ((panel instanceof CardPanel)
                       || layout.equals(LAYOUT_CARD)) {
                String childId = getAttr(childElement, ATTR_ID,
                                         (String) null);
                panel.add(childId, childComponent);
                if (defaultComp.equals(childId)) {
                    if (layout.equals(LAYOUT_CARD)) {
                        ((CardLayout) panel.getLayout()).show(panel, childId);
                    } else {
                        ((CardPanel) panel).flip(childId);
                    }
                }
            } else if (layout.equals(LAYOUT_INSET)) {
                GuiUtils.tmpInsets = new Insets(topSpace, leftSpace,
                        bottomSpace, rightSpace);
                GuiUtils.doLayout(panel, new Component[] { childComponent },
                                  1, GuiUtils.WT_Y, GuiUtils.WT_Y);
                break;
            } else if (layout.equals(LAYOUT_WRAP)) {
                GuiUtils.doLayout(panel, new Component[] { childComponent },
                                  1, GuiUtils.WT_N, GuiUtils.WT_N);
                break;
            } else if (layout.equals(LAYOUT_GRAPHPAPER)) {
                Rectangle r = new Rectangle(getAttr(childElement, ATTR_X, 0),
                                            getAttr(childElement, ATTR_Y, 0),
                                            getAttr(childElement, ATTR_COLS,
                                                1), getAttr(childElement,
                                                    ATTR_ROWS, 1));
                panel.add(childComponent, r);
            } else if (layout.equals(LAYOUT_GRIDBAG)) {
                fills = addInteger(fills,
                                   getAttr(childElement, ATTR_FILL,
                                           (String) null), childComponent,
                                               FILL_NAMES, FILL_VALUES);

                anchors = addInteger(anchors,
                                     getAttr(childElement, ATTR_ANCHOR,
                                             (String) null), childComponent,
                                                 ANCHOR_NAMES, ANCHOR_VALUES);

            } else {
                panel.add(childComponent);
            }
        }
        if (layout.equals(LAYOUT_GRIDBAG)) {
            double[] cw = Misc.parseDoubles(getAttr(node, ATTR_COLWIDTHS,
                              (String) null));
            double[] rh = Misc.parseDoubles(getAttr(node, ATTR_ROWHEIGHTS,
                              (String) null));
            if (cw == null) {
                cw = GuiUtils.WT_Y;
            }
            if (rh == null) {
                rh = GuiUtils.WT_N;
            }
            GuiUtils.tmpInsets = new Insets(topSpace, leftSpace, bottomSpace,
                                            rightSpace);
            GuiUtils.doLayout(panel, GuiUtils.getComponentArray(children),
                              cols, cw, rh, anchors, fills, null);
        }

        containerToNodeList.put(panel, nodes);
        return panel;
    }


    /**
     * Class CardPanel Holds a card layout state
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.62 $
     */
    public static class CardPanel extends JPanel {

        /** components */
        List members = new ArrayList();

        /** id to component mapping */
        Hashtable idToMember = new Hashtable();

        /** which card */
        int currentIdx = 0;

        /**
         * ctor
         */
        public CardPanel() {
            super();
        }


        /**
         * add the component
         *
         * @param comp  component
         *
         * @return The component
         */
        public Component add(Component comp) {
            addCardComponent(null, comp);
            return comp;
        }

        /**
         * Add the id-d component
         *
         * @param id id
         * @param comp component
         *
         * @return The comp
         */
        public Component add(String id, Component comp) {
            addCardComponent(id, comp);
            return comp;
        }

        /**
         * add component
         *
         * @param id id
         * @param comp comp
         */
        public void addCardComponent(String id, Component comp) {
            members.add(comp);
            if (id != null) {
                idToMember.put(id, comp);
            }
            if (members.size() == 1) {
                super.add(comp, BorderLayout.CENTER);
            } else {
                comp.setVisible(false);
            }
        }

        /**
         * set visibility of each component
         */
        private void checkVisible() {
            for (int i = 0; i < members.size(); i++) {
                ((Component) members.get(i)).setVisible(i == currentIdx);
            }
        }

        /**
         * Show next component
         */
        public void next() {
            currentIdx++;
            if (currentIdx >= members.size()) {
                currentIdx = 0;
            }
            Component next = (Component) members.get(currentIdx);
            checkVisible();
            super.removeAll();
            super.add(next, BorderLayout.CENTER);
        }

        /**
         * Set visibility to the given component id
         *
         * @param id comp id
         */
        public void flip(String id) {
            if (id == null) {
                return;
            }
            Component next = (Component) idToMember.get(id);
            if (next == null) {
                return;
            }
            int idx = members.indexOf(next);
            if ((idx < 0) || (idx == currentIdx)) {
                return;
            }
            currentIdx = idx;
            checkVisible();
            super.removeAll();
            super.add(next, BorderLayout.CENTER);
        }



    }

    ;


    /**
     * Create an image
     *
     * @param path image path
     * @return image
     */
    public static Image getImage(String path) {
        return GuiUtils.getImage(path);
    }

    /**
     * Get image defined by attr name
     *
     * @param node node
     * @param attr attr
     *
     * @return image
     */
    public Image getImageAttr(Element node, String attr) {
        String path = getAttr(node, attr, NULLSTRING);
        if (path != null) {
            return getImage(path);
        }
        return null;
    }

    /**
     * We are in a bad way. Throw an exception.
     *
     * @param msg error msg
     */
    private void badState(String msg) {
        throw new IllegalStateException(msg);
    }


    /**
     *  Create an image label or button from the given Element
     *
     * @param tag
     * @param node
     * @param imagePath
     * @param action
     * @return The button
     */
    private AbstractButton makeImageButton(String tag, Element node,
                                           String imagePath, String action) {
        if (imagePath == null) {
            imagePath = getAttr(node, ATTR_IMAGE, "no path");
        }
        Image image = getImage(imagePath);
        if (image == null) {
            badState("No image found for:" + imagePath);
        }

        int imageWidth  = getAttr(node, ATTR_IMAGEWIDTH, -1);
        int imageHeight = getAttr(node, ATTR_IMAGEHEIGHT, -1);
        if ((imageWidth != -1) || (imageHeight != -1)) {
            //We really should scale the other dimension here
            if (imageWidth == -1) {
                imageWidth = imageHeight;
            }
            if (imageHeight == -1) {
                imageHeight = imageWidth;
            }
            image = image.getScaledInstance(imageWidth, imageHeight, 0);
        }


        Image          overImage   = getImageAttr(node, ATTR_OVERIMAGE);
        Image          downImage   = getImageAttr(node, ATTR_DOWNIMAGE);
        Image          selectImage = getImageAttr(node, ATTR_SELECTIMAGE);

        ImageIcon      icon        = new ImageIcon(image);
        icon  = GuiUtils.scaleImageIcon(icon);
        AbstractButton tb;
        if (tag.equals(TAG_TOGGLEBUTTON)) {
            tb = new JToggleButton(icon);
        } else {
            tb = new JButton(icon);
            //      tb.setBorderPainted(false);
            tb.setContentAreaFilled(false);
        }

        int space  = getAttr(node, ATTR_SPACE, 0);
        int hspace = getAttr(node, ATTR_HSPACE, space);
        int vspace = getAttr(node, ATTR_VSPACE, space);

        if (overImage != null) {
            tb.setRolloverIcon(new ImageIcon(overImage));
        }
        if (downImage != null) {
            tb.setPressedIcon(new ImageIcon(downImage));
        }
        if (selectImage != null) {
            tb.setSelectedIcon(new ImageIcon(selectImage));
        }
        if (XmlUtil.hasAttribute(node, ATTR_LABEL)) {
            String label = getAttr(node, ATTR_LABEL, "");
            tb.setText(label);
            if (XmlUtil.hasAttribute(node, ATTR_HPOSITION)) {
                tb.setHorizontalTextPosition(findValue(getAttr(node,
                        ATTR_HPOSITION, ""), HPOS_NAMES, HPOS_VALUES,
                                             SwingConstants.TRAILING));
            }

            if (XmlUtil.hasAttribute(node, ATTR_VPOSITION)) {
                tb.setVerticalTextPosition(findValue(getAttr(node,
                        ATTR_VPOSITION, ""), VPOS_NAMES, VPOS_VALUES,
                                             SwingConstants.CENTER));
            }



        } else {
            tb.setPreferredSize(new Dimension(icon.getIconWidth() + hspace,
                    icon.getIconHeight() + vspace));

        }

        if (action == null) {
            action = getAttr(node, ATTR_ACTION, (String) null);
        }
        if (action != null) {
            tb.setActionCommand(action);
            tb.addActionListener(this);
        }
        return tb;
    }

    /**
     *  Create the awt Component defined by the given skin node.
     *
     * @param node node
     * @param id id
     * @return The component
     */
    public Component createComponent(Element node, String id) {

        Component comp = null;
        String    tag  = node.getTagName();

        if (tag.equals(TAG_PANEL) || tag.equals(TAG_CARDPANEL)) {
            JPanel thePanel;
            if (tag.equals(TAG_CARDPANEL)) {
                thePanel = new CardPanel();
            } else {
                thePanel = new JPanel();
            }
            comp = layoutContainer(thePanel, node,
                                   XmlUtil.getListOfElements(node));
        } else if (tag.equals(TAG_TOOLBAR)) {
            List xmlChildren = XmlUtil.getListOfElements(node);

            int orientation = findValue(getAttr(node, ATTR_ORIENTATION,
                                  NULLSTRING), TOOLBAR_NAMES, TOOLBAR_VALUES);
            JToolBar toolbar = new JToolBar(orientation);
            toolbar.setFloatable(getAttr(node, ATTR_FLOAT, true));
            comp = toolbar;
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                toolbar.add(childComponent);
            }
        } else if (tag.equals(TAG_MENUBAR)) {
            List     xmlChildren = XmlUtil.getListOfElements(node);
            JMenuBar menuBar     = new JMenuBar();
            comp = menuBar;
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                menuBar.add(childComponent);
            }

        } else if (tag.equals(TAG_TABBEDPANE)) {
            List xmlChildren = XmlUtil.getListOfElements(node);
            int orient = findValue(getAttr(node, ATTR_TABPLACE, NULLSTRING),
                                   TABPLACE_NAMES, TABPLACE_VALUES,
                                   JTabbedPane.TOP);
            JTabbedPane tab;
            Insets      tabInsets = null;
            String insetsString   = getAttr(node, ATTR_TABINSETS, NULLSTRING);
            if (insetsString != null) {
                double[] insets = Misc.parseDoubles(insetsString);
                if (insets.length == 1) {
                    tabInsets = new Insets((int) insets[0], (int) insets[0],
                                           (int) insets[0], (int) insets[0]);
                } else if (insets.length == 4) {
                    tabInsets = new Insets((int) insets[0], (int) insets[1],
                                           (int) insets[2], (int) insets[3]);
                }
            }



            Insets labelInsets = getInsets(getAttr(node, ATTR_TABPAD,
                                     NULLSTRING));
            if (XmlUtil.hasAttribute(node, ATTR_TABBORDERTOP)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERLEFT)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERBOTTOM)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERRIGHT)) {
                tab = GuiUtils.getNestedTabbedPane(orient,
                        getAttr(node, ATTR_TABBORDERTOP, 0),
                        getAttr(node, ATTR_TABBORDERLEFT, 0),
                        getAttr(node, ATTR_TABBORDERBOTTOM, 0),
                        getAttr(node, ATTR_TABBORDERRIGHT, 0));

            } else if (getAttr(node, ATTR_TABNESTED, false)) {
                tab = GuiUtils.getNestedTabbedPane(orient);
            } else {
                tab = new JTabbedPane(orient);
            }
            comp = tab;
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                if (XmlUtil.getAttribute(childElement,
                                         ATTR_CATEGORYCOMPONENT, false)) {
                    continue;
                }
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                String iconName = XmlUtil.getAttribute(childElement,
                                      ATTR_ICON, (String) null);
                Icon icon = null;
                if (iconName != null) {
                    Image image = getImage(iconName);
                    if (image != null) {
                        icon = new ImageIcon(image);
                    }
                }

                String label = getAttr(childElement, ATTR_TITLE, "");
                if (labelInsets != null) {
                    label = "<html><div style=\"" + "margin-top:"
                            + labelInsets.top + ";margin-right:"
                            + labelInsets.right + ";margin-bottom:"
                            + labelInsets.bottom + ";margin-left:"
                            + labelInsets.left + "\">" + label
                            + "</div></html>";
                }


                if (tabInsets != null) {
                    childComponent = GuiUtils.inset(childComponent,
                            tabInsets);
                }
                tab.addTab(label, icon, childComponent);
                String mnemonic = XmlUtil.getAttribute(childElement,
                                      ATTR_MNEMONIC, (String) null);

                /**
                 * * Comment this out while we are still using 1.3
                 *    if(mnemonic!=null)
                 *    tab.setMnemonicAt(i,GuiUtils.charToKeyCode(mnemonic));
                 */
                String tabTooltip = XmlUtil.getAttribute(childElement,
                                        ATTR_TABTOOLTIP, (String) null);
                if (tabTooltip != null) {
                    tab.setToolTipTextAt(i, tabTooltip);
                }
            }

        } else if (tag.equals(TAG_TABBEDPANE)) {
            List xmlChildren = XmlUtil.getListOfElements(node);
            int orient = findValue(getAttr(node, ATTR_TABPLACE, NULLSTRING),
                                   TABPLACE_NAMES, TABPLACE_VALUES,
                                   JTabbedPane.TOP);
            JTabbedPane tab;
            Insets      tabInsets = null;
            String insetsString   = getAttr(node, ATTR_TABINSETS, NULLSTRING);
            if (insetsString != null) {
                double[] insets = Misc.parseDoubles(insetsString);
                if (insets.length == 1) {
                    tabInsets = new Insets((int) insets[0], (int) insets[0],
                                           (int) insets[0], (int) insets[0]);
                } else if (insets.length == 4) {
                    tabInsets = new Insets((int) insets[0], (int) insets[1],
                                           (int) insets[2], (int) insets[3]);
                }
            }



            Insets labelInsets = getInsets(getAttr(node, ATTR_TABPAD,
                                     NULLSTRING));
            if (XmlUtil.hasAttribute(node, ATTR_TABBORDERTOP)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERLEFT)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERBOTTOM)
                    || XmlUtil.hasAttribute(node, ATTR_TABBORDERRIGHT)) {
                tab = GuiUtils.getNestedTabbedPane(orient,
                        getAttr(node, ATTR_TABBORDERTOP, 0),
                        getAttr(node, ATTR_TABBORDERLEFT, 0),
                        getAttr(node, ATTR_TABBORDERBOTTOM, 0),
                        getAttr(node, ATTR_TABBORDERRIGHT, 0));

            } else if (getAttr(node, ATTR_TABNESTED, false)) {
                tab = GuiUtils.getNestedTabbedPane(orient);
            } else {
                tab = new JTabbedPane(orient);
            }
            comp = tab;
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                if (XmlUtil.getAttribute(childElement,
                                         ATTR_CATEGORYCOMPONENT, false)) {
                    continue;
                }
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                String iconName = XmlUtil.getAttribute(childElement,
                                      ATTR_ICON, (String) null);
                Icon icon = null;
                if (iconName != null) {
                    Image image = getImage(iconName);
                    if (image != null) {
                        icon = new ImageIcon(image);
                    }
                }

                String label = getAttr(childElement, ATTR_TITLE, "");
                if (labelInsets != null) {
                    label = "<html><div style=\"" + "margin-top:"
                            + labelInsets.top + ";margin-right:"
                            + labelInsets.right + ";margin-bottom:"
                            + labelInsets.bottom + ";margin-left:"
                            + labelInsets.left + "\">" + label
                            + "</div></html>";
                }


                if (tabInsets != null) {
                    childComponent = GuiUtils.inset(childComponent,
                            tabInsets);
                }
                tab.addTab(label, icon, childComponent);
                String mnemonic = XmlUtil.getAttribute(childElement,
                                      ATTR_MNEMONIC, (String) null);

                /**
                 * * Comment this out while we are still using 1.3
                 *    if(mnemonic!=null)
                 *    tab.setMnemonicAt(i,GuiUtils.charToKeyCode(mnemonic));
                 */
                String tabTooltip = XmlUtil.getAttribute(childElement,
                                        ATTR_TABTOOLTIP, (String) null);
                if (tabTooltip != null) {
                    tab.setToolTipTextAt(i, tabTooltip);
                }
            }
        } else if (tag.equals(TAG_BUTTONTABBEDPANE)) {
            List             xmlChildren = XmlUtil.getListOfElements(node);
            ButtonTabbedPane tab         = new ButtonTabbedPane(-1);;
            comp = tab;
            Component firstComp = null;
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                if (XmlUtil.getAttribute(childElement,
                                         ATTR_CATEGORYCOMPONENT, false)) {
                    continue;
                }
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                if (firstComp == null) {
                    firstComp = childComponent;
                }
                String label = getAttr(childElement, ATTR_TITLE, "");
                tab.addTab(label, childComponent);
            }
            if (firstComp != null) {
                tab.show(firstComp);
            }
        } else if (tag.equals(TAG_TREEPANEL)) {
            TreePanel treePanel = new TreePanel(getAttr(node,
                                      ATTR_USESPLITPANE,
                                      false), getAttr(node, ATTR_TREEWIDTH,
                                          -1));
            comp = treePanel;
            List xmlChildren = XmlUtil.getListOfElements(node);

            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                String label = getAttr(childElement, ATTR_TITLE, "");
                //                String cat = getAttr(childElement, ATTR_CATEGORY, "Category");               
                ImageIcon icon = getAttr(childElement, ATTR_ICON,
                                         (ImageIcon) null);
                String cat = getAttr(childElement, ATTR_CATEGORY,
                                     (String) null);
                if (XmlUtil.getAttribute(childElement,
                                         ATTR_CATEGORYCOMPONENT, false)) {
                    treePanel.addCategoryComponent(cat,
                            (JComponent) childComponent);
                } else {
                    treePanel.addComponent((JComponent) childComponent, cat,
                                           label, icon);
                }
            }
            treePanel.openAll();

        } else if (tag.equals(TAG_SPLITPANE)) {
            List xmlChildren = XmlUtil.getListOfElements(node);
            if (xmlChildren.size() != 2) {
                badState("splitpane tag needs to  have 2 children");
            }
            Element leftElement = getReffedNode((Element) xmlChildren.get(0));
            Component leftComponent = xmlToUi(leftElement);
            Element rightElement =
                getReffedNode((Element) xmlChildren.get(1));
            Component rightComponent = xmlToUi(rightElement);
            boolean   continuous     = getAttr(node, ATTR_CONTINUOUS, true);
            int orientation = findValue(getAttr(node, ATTR_ORIENTATION,
                                  NULLSTRING), SPLITPANE_NAMES,
                                      SPLITPANE_VALUES);

            JSplitPane split = new JSplitPane(orientation, continuous,
                                   leftComponent, rightComponent);
            int divider = getAttr(node, ATTR_DIVIDER, -1);
            if (divider != -1) {
                split.setDividerLocation(divider);
            }
            split.setOneTouchExpandable(getAttr(node,
                    ATTR_ONETOUCHEXPANDABLE, false));
            double resizeweight = getAttr(node, ATTR_RESIZEWEIGHT, -1.0);
            if (resizeweight != -1.0) {
                split.setResizeWeight(resizeweight);
            }


            comp = split;
        } else if (tag.equals(TAG_SCROLLER)) {
            List xmlChildren = XmlUtil.getListOfElements(node);
            if (xmlChildren.size() != 1) {
                badState("scroller tag needs to  have 1 children");
            }
            Element   element   = getReffedNode((Element) xmlChildren.get(0));
            Component component = xmlToUi(element);
            JScrollPane scroller =
                new JScrollPane(
                    component,
                    findValue(
                        getAttr(node, ATTR_VSCROLL, "asneeded"),
                        SCROLL_NAMES, VSCROLL_VALUES), findValue(
                            getAttr(node, ATTR_HSCROLL, "asneeded"),
                            SCROLL_NAMES, HSCROLL_VALUES));


            comp = scroller;
        } else if (tag.equals(TAG_TEXTINPUT)) {
            int            cols  = getAttr(node, ATTR_COLS, -1);
            int            rows  = getAttr(node, ATTR_ROWS, -1);
            String         value = getAttr(node, ATTR_VALUE, "");
            JTextComponent textComp;
            if (rows > 1) {
                if (cols < 0) {
                    cols = 30;
                }
                textComp = new JTextArea(value, rows, cols);
            } else {
                if (cols == -1) {
                    textComp = new JTextField(value);
                } else {
                    textComp = new JTextField(value, cols);
                }
                ((JTextField) textComp).addActionListener(this);
            }
            comp = textComp;
            String action = getAttr(node, ATTR_ACTION, (String) null);
            if (action != null) {
                componentToAction.put(textComp, action);
            }
        } else if (tag.equals(TAG_FILLER)) {
            int w  = getAttr(node, ATTR_WIDTH, 0);
            int h  = getAttr(node, ATTR_HEIGHT, 0);
            int pw = getAttr(node, ATTR_PREF_WIDTH, w);
            int ph = getAttr(node, ATTR_PREF_HEIGHT, h);
            comp = GuiUtils.topCenter(null,
                                      new Box.Filler(new Dimension(w, h),
                                          new Dimension(pw, ph), null));
        } else if (tag.equals(TAG_LABEL)) {
            String label     = getAttr(node, ATTR_TEXT, NULLSTRING);
            String imagePath = getAttr(node, ATTR_IMAGE, NULLSTRING);
            Image  image     = getImageAttr(node, ATTR_IMAGE);
            String template  = getAttr(node, ATTR_TEMPLATE, NULLSTRING);
            if ((template != null) && (label != null)) {
                label = StringUtil.replace(template, "%text%", label);
            }

            if ((label == null) && (image == null)) {
                label = XmlUtil.getChildText(node);
                if ((label != null) && (label.trim().length() == 0)) {
                    label = null;
                }
                if (label != null) {
                    label = label.trim();
                }
            }

            if ((label == null) && (image == null)) {
                label = "No label";
            }
            if (label == null) {
                comp = new JLabel(new ImageIcon(image), getAlign(node));
            } else if (image == null) {
                comp = new JLabel(label, getAlign(node));
            } else {
                comp = new JLabel(label, new ImageIcon(image),
                                  getAlign(node));
            }
            //      Image overImage = getImageAttr(node,ATTR_OVERIMAGE);
            //      if(overImage!=null)
        } else if (tag.equals(TAG_HTML)) {
            String text = getAttr(node, ATTR_TEXT, NULLSTRING);
            if (text == null) {
                String url = getAttr(node, ATTR_URL, NULLSTRING);
                if (url != null) {
                    text = IOUtil.readContents(url, (String) null);
                }
                if (text == null) {
                    text = XmlUtil.getChildText(node);
                }
            }
            HyperlinkListener linkListener = new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String url;
                        if (e.getURL() == null) {
                            url = e.getDescription();
                        } else {
                            url = e.getURL().toString();
                        }
                        actionPerformed(new ActionEvent(this, 0, url));
                    }
                }
            };
            Component[] comps = GuiUtils.getHtmlComponent(text, linkListener,
                                    getAttr(node, ATTR_WIDTH, 200),
                                    getAttr(node, ATTR_HEIGHT, 200));
            ((JComponent)comps[0]).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            setAttrs(comps[0],node);
            comp = comps[1];

        } else if (tag.equals(TAG_IMAGE)) {
            Image image = getImageAttr(node, ATTR_IMAGE);
            if (image == null) {
                badState("No image found for:"
                         + getAttr(node, ATTR_IMAGE, NULLSTRING));
            }
            comp = new JLabel(new ImageIcon(image));
            //      Image overImage = getImageAttr(node,ATTR_OVERIMAGE);
        } else if (tag.equals(TAG_RADIO)) {
            JRadioButton radio = new JRadioButton(getAttr(node, ATTR_LABEL,
                                     ""), getAttr(node, ATTR_DEFAULT, false));
            String group = getAttr(node, ATTR_GROUP, (String) null);
            if (group != null) {
                ButtonGroup bg = (ButtonGroup) buttonGroups.get(group);
                if (bg == null) {
                    bg = new ButtonGroup();
                    buttonGroups.put(group, bg);
                }
                bg.add(radio);
                String action = getAttr(node, ATTR_ACTION, (String) null);
                if (action != null) {
                    radio.setActionCommand(action);
                } else {
                    String actionTemplate = getAttr(node,
                                                ATTR_ACTIONTEMPLATE,
                                                (String) null);
                    if (actionTemplate != null) {
                        String value = getAttr(node, ATTR_VALUE,
                                           (String) null);
                        if (value != null) {
                            radio.setActionCommand(
                                StringUtil.replace(
                                    actionTemplate, "%value%", value));
                        }
                    }
                }
            }
            radio.addActionListener(this);
            comp = radio;
        } else if (tag.equals(TAG_BUTTON) || tag.equals(TAG_TOGGLEBUTTON)) {
            comp = makeButton(tag, node);
        } else if (tag.equals(TAG_CHECKBOX)) {
            JCheckBox b = new JCheckBox(getAttr(node, ATTR_LABEL,
                              "No label"), getAttr(node, ATTR_VALUE, false));
            String action = getAttr(node, ATTR_ACTION, (String) null);
            comp = b;
            if (action != null) {
                componentToAction.put(comp, action);
                b.addItemListener(this);
                actionPerformed(new ActionEvent(comp, 0, action));
            }
        } else if (tag.equals(TAG_COMBOBOX)) {
            List   items    = XmlUtil.getElements(node, TAG_ITEM);
            Vector boxItems = new Vector();
            for (int i = 0; i < items.size(); i++) {
                Element child  = getReffedNode((Element) items.get(i));
                String  label  = getAttr(child, ATTR_LABEL);
                String  action = getAttr(child, ATTR_ACTION, (String) null);
                String  value  = getAttr(child, ATTR_VALUE, (String) null);
                boxItems.add(new TwoFacedObject(label, new String[] { action,
                        value }));
            }
            JComboBox box = new JComboBox(boxItems);
            box.setEditable(getAttr(node, ATTR_EDITABLE, false));
            comp = box;
            box.addActionListener(this);
        } else if (tag.equals(TAG_MENUITEM)) {
            comp = makeMenuItem(node, null);
        } else if (tag.equals(TAG_MENU)) {
            JMenu menu = new JMenu(getAttr(node, ATTR_LABEL,
                                           (String) "No label"));
            List xmlChildren = XmlUtil.getListOfElements(node);
            for (int i = 0; i < xmlChildren.size(); i++) {
                Element childElement =
                    getReffedNode((Element) xmlChildren.get(i));
                if (childElement.getTagName().equals(
                        GuiUtils.MENU_SEPARATOR)) {
                    menu.addSeparator();
                    continue;
                }
                Component childComponent = xmlToUi(childElement);
                if (childComponent == null) {
                    continue;
                }
                menu.add(childComponent);
            }
            comp = menu;
        } else if (tag.equals(TAG_MENUPOPUP)) {
            String image = getAttr(node, ATTR_IMAGE, (String) null);
            if (image == null) {
                JButton b = new JButton(getAttr(node, ATTR_LABEL,
                                "No label"));
                b.setActionCommand(ACTION_MENUPOPUP);
                b.addActionListener(this);
                comp = b;
            } else {
                comp = makeImageButton(tag, node, image, ACTION_MENUPOPUP);
            }
        } else {
            comp = new JLabel("Unknown tag:" + tag);
        }
        return comp;
    }

    /**
     * Make a button
     *
     * @param tag  tag name
     * @param node node
     *
     * @return button
     */
    public JComponent makeButton(String tag, Element node) {

        String image  = getAttr(node, ATTR_IMAGE, (String) null);
        String action = getAttr(node, ATTR_ACTION, "No action");
        if (image == null) {
            AbstractButton b;
            if (tag.equals(TAG_BUTTON)) {
                b = new JButton(getAttr(node, ATTR_LABEL, "No label"));
            } else {
                b = new JToggleButton(getAttr(node, ATTR_LABEL, "No label"));
            }
            b.setActionCommand(action);
            b.addActionListener(this);
            return b;
        } else {
            return makeImageButton(tag, node, image, action);
        }

    }


    /**
     * Set the text in the component
     *
     * @param comp TextComponent, JLabel, etc.
     * @param text text
     */
    private void setText(Component comp, String text) {
        if (comp instanceof JTextComponent) {
            ((JTextComponent) comp).setText(text);
        } else if (comp instanceof JLabel) {
            ((JLabel) comp).setText(text);
        } else if (comp instanceof JButton) {
            ((JButton) comp).setText(text);
        }
    }

    /**
     * handle event
     *
     * @param event event
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();
        String action = (String) componentToAction.get(source);
        if (action == null) {
            return;
        }
        actionPerformed(new ActionEvent(source, 0, action));
    }


    /**
     * handle event. This calls actionPerformedInner in a non-swing thread
     *
     * @param event event
     */
    public void actionPerformed(final ActionEvent event) {
        Misc.run(new Runnable(){
                public void run() {
                    actionPerformedInner(event);
                }
            });
    }


    /**
     * handle event
     *
     * @param event event
     */
    private void actionPerformedInner(ActionEvent event) {
        Object source = event.getSource();
        String cmd    = null;

        if (source instanceof JComboBox) {
            TwoFacedObject tfo =
                (TwoFacedObject) ((JComboBox) source).getSelectedItem();
            String[] actionOrValue = (String[]) tfo.getId();
            Element  boxNode       = (Element) compToNode.get(source);
            if (actionOrValue[0] != null) {
                cmd = actionOrValue[0];
            } else if (actionOrValue[1] != null) {
                String actionTemplate = getAttr(boxNode, ATTR_ACTIONTEMPLATE);
                if (actionTemplate != null) {
                    cmd = StringUtil.replace(actionTemplate, "%value%",
                                             actionOrValue[1]);
                }
            } else {
                cmd = getAttr(boxNode, ATTR_ACTION);
            }
        }

        if (cmd == null) {
            cmd = event.getActionCommand();
            //            System.err.println ("action:" + cmd);
            String otherAction = (String) componentToAction.get(source);
            if (otherAction != null) {
                cmd = otherAction;
            }
        }

        if (cmd == null) {
            return;
        }
        StringTokenizer tok = new StringTokenizer(cmd, ";");
        while (tok.hasMoreTokens()) {
            processAction(tok.nextToken(), source);
        }
    }

    /**
     * utility to parse commands
     *
     * @param cmd command
     * @return The arg
     */
    public static String extractOneArg(String cmd) {
        int idx1 = cmd.indexOf("(");
        int idx2 = cmd.indexOf(")");
        if ((idx1 < 0) || (idx2 < 0)) {
            return null;
        }
        return cmd.substring(idx1 + 1, idx2);
    }

    /**
     * utility to parse commands
     *
     * @param cmd command
     * @return The args
     */

    public static String[] extractTwoArgs(String cmd) {
        int idx1 = cmd.indexOf("(");
        int idx2 = cmd.indexOf(",");
        int idx3 = cmd.indexOf(")");
        if ((idx1 < 0) || (idx2 < 0) || (idx3 < 0)) {
            return null;
        }
        if ( !((idx1 < idx2) && (idx2 < idx3))) {
            return null;
        }
        return new String[] { cmd.substring(idx1 + 1, idx2),
                              cmd.substring(idx2 + 1, idx3) };
    }

    /**
     * Parse command
     *
     * @param cmd command
     * @param source source
     */
    private void processAction(String cmd, Object source) {

        cmd = cmd.trim();
        if (cmd.startsWith("ui.toggle")) {
            String    compId = extractOneArg(cmd);
            Component comp   = (Component) idToComponent.get(compId);
            if (comp == null) {
                System.err.println("Unable to find: " + compId);
                return;
            }
            Container parent = (Container) componentToParent.get(comp);

            if (parent == null) {
                return;
            }
            Element parentNode = (Element) compToNode.get(parent);
            if (parentNode == null) {
                return;
            }
            comp.setVisible( !comp.isVisible());
            layoutContainer((JPanel) parent, parentNode,
                            (List) containerToNodeList.get(parent));
            myContents.invalidate();
            myContents.validate();
        } else if (cmd.equals(ACTION_MENUPOPUP)) {
            Element node = (Element) compToNode.get(source);
            if (node == null) {
                return;
            }
            JPopupMenu m = new JPopupMenu();
            //      myContents.add (m);
            processMenu(null, m, node,
                        getAttr(node, ATTR_ACTIONTEMPLATE, (String) null));
            Component mb = (Component) source;
            m.show(mb, 0, mb.getBounds().height);
        } else if (cmd.startsWith(ACTION_SETTEXT)) {
            String[] args = extractTwoArgs(cmd);
            if (args != null) {
                Component comp = (args[0].equals("this")
                                  ? (Component) source
                                  : (Component) idToComponent.get(args[0]));
                if (comp != null) {
                    String text = args[1];
                    if (text.startsWith("prop:")) {
                        Element node = (Element) compToNode.get(source);
                        if (node == null) {
                            return;
                        }
                        text = getAttr(node, text.substring(5), "");
                    }
                    setText(comp, text);
                }
            }
        } else if (cmd.startsWith(ACTION_SETBORDER)) {
            String[] args = extractTwoArgs(cmd);
            if (args != null) {

                JComponent comp = (args[0].equals("this")
                                   ? (JComponent) source
                                   : (JComponent) idToComponent.get(args[0]));
                if (comp != null) {

                    String  border = args[1];
                    Element node   = (Element) compToNode.get(source);
                    comp.setBorder(getBorder(border, node));
                }
            }
        } else if (cmd.startsWith(ACTION_UI_FOCUS)) {
            String id = extractOneArg(cmd);
            if (id == null) {
                return;
            }
            Component comp = (Component) (id.equals("this")
                                          ? source
                                          : idToComponent.get(id));
            if (comp == null) {
                return;
            }
            if (comp instanceof JComponent) {
                ((JComponent) comp).requestFocusInWindow();
            }

        } else if (cmd.startsWith(ACTION_UI_GROUP_NEXT)) {
            String groupId = extractOneArg(cmd);
            if (groupId == null) {
                return;
            }
            ButtonGroup bg = (ButtonGroup) buttonGroups.get(groupId);
            if (bg == null) {
                return;
            }
            boolean nextOneOn = false;
            List    buttons   = Misc.toList(bg.getElements());
            for (int i = 0; i < buttons.size(); i++) {
                JToggleButton btn = (JToggleButton) buttons.get(i);
                if (nextOneOn) {
                    btn.setSelected(true);
                    String action = btn.getActionCommand();
                    if (action != null) {
                        actionPerformed(new ActionEvent(btn, 0, action));
                    }
                    break;
                }
                nextOneOn = btn.isSelected();
            }
        } else if (cmd.startsWith(ACTION_UI_GROUP_PREV)) {
            String groupId = extractOneArg(cmd);
            if (groupId == null) {
                return;
            }
            ButtonGroup bg = (ButtonGroup) buttonGroups.get(groupId);
            if (bg == null) {
                return;
            }
            boolean nextOneOn = false;
            List    buttons   = Misc.toList(bg.getElements());
            for (int i = buttons.size() - 1; i >= 0; i--) {
                JToggleButton btn = (JToggleButton) buttons.get(i);
                if (nextOneOn) {
                    btn.setSelected(true);
                    String action = btn.getActionCommand();
                    if (action != null) {
                        actionPerformed(new ActionEvent(btn, 0, action));
                    }
                    break;
                }
                nextOneOn = btn.isSelected();
            }
        } else if (cmd.startsWith(ACTION_UI_FLIP)) {
            //flip (panel,subcomponent)
            String[] args = extractTwoArgs(cmd);
            if (args != null) {
                Container panel = (Container) idToComponent.get(args[0]);
                Component comp  = (Component) idToComponent.get(args[1]);
                if ((panel == null) || (comp == null)) {
                    System.err.println("Unable to find: " + args[0] + " or "
                                       + args[1]);
                    return;
                }
                if (panel instanceof CardPanel) {
                    ((CardPanel) panel).flip(args[1]);
                    panel.invalidate();
                    panel.validate();
                    panel.repaint();
                } else {
                    CardLayout layout = (CardLayout) panel.getLayout();
                    layout.show(panel, args[1]);
                }
            } else {
                String arg = extractOneArg(cmd);
                if (arg == null) {
                    return;
                }
                Container panel = (Container) idToComponent.get(arg);
                if (panel == null) {
                    System.err.println("Unable to find: " + arg);
                    return;
                }
                if (panel instanceof CardPanel) {
                    ((CardPanel) panel).next();
                    panel.invalidate();
                    panel.validate();
                    panel.repaint();
                } else {
                    CardLayout layout = (CardLayout) panel.getLayout();
                    layout.next(panel);
                }
            }
        } else {
            if (actionListener != null) {
                actionListener.actionPerformed(new ActionEvent(source, 0,
                        cmd));
            }
        }
    }

    /**
     * Make a menu item from the child node
     *
     * @param child node
     * @param actionTemplate template
     *
     * @return menu item
     */
    private JMenuItem makeMenuItem(Element child, String actionTemplate) {
        JMenuItem mi = new JMenuItem(getAttr(child, ATTR_LABEL, "No label"));
        String    action = getAttr(child, ATTR_ACTION, (String) null);
        String    value  = getAttr(child, ATTR_VALUE, (String) null);
        if ((action == null) && (actionTemplate != null) && (value != null)) {
            action = StringUtil.replace(actionTemplate, "%value%", value);
        }
        if (action != null) {
            mi.setActionCommand(action);
            mi.addActionListener(this);
        }
        String tooltip = getAttr(child, ATTR_TOOLTIP, (String) null);
        if (tooltip != null) {
            mi.setToolTipText(tooltip);
        }
        return mi;
    }

    /**
     * _Process the meu
     *
     * @param m menu
     * @param pm popup
     * @param node node
     * @param actionTemplate action
     */
    private void processMenu(JMenu m, JPopupMenu pm, Element node,
                             String actionTemplate) {
        Container cont     = ((m != null)
                              ? (Container) m
                              : (Container) pm);
        NodeList  children = XmlUtil.getElements(node);
        for (int i = 0; i < children.getLength(); i++) {
            Element child = getReffedNode((Element) children.item(i));
            if (child.getTagName().equals(TAG_SEPARATOR)) {
                if (m != null) {
                    m.addSeparator();
                } else {
                    pm.addSeparator();
                }
            } else if (child.getTagName().equals(TAG_MENUITEM)) {
                cont.add(makeMenuItem(child, actionTemplate));
            } else if (child.getTagName().equals(TAG_MENU)) {
                JMenu childMenu = new JMenu(getAttr(child, ATTR_LABEL,
                                      "No label"));
                m.add(childMenu);
                processMenu(childMenu, null, child, actionTemplate);
            }
        }
    }


    /**
     * test
     *
     * @param args args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Provide an xml file");
            return;
        }

        try {
            XmlUi xmlUi = new XmlUi(XmlUtil.getRoot(args[0], XmlUi.class),
                                    null, null, null);
            JFrame f = new JFrame();
            f.getContentPane().add(xmlUi.getContents());
            f.pack();
            f.setLocation(300, 300);
            f.show();
        } catch (Exception exc) {
            System.err.println("Error:");
            exc.printStackTrace();
        }
    }


}

