/*
 * $Id: HelpTipDialog.java,v 1.21 2007/07/06 20:45:30 jeffmc Exp $
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

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.xml.XmlObjectStore;



import ucar.unidata.xml.XmlResourceCollection;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;

import java.awt.event.*;

import java.beans.*;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Class HelpTipDialog
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
public class HelpTipDialog extends JDialog implements HyperlinkListener {

    /** help tip preference */
    public static final String PREF_HELPTIPSHOW = "help.helptip.Show";

    /** help tip index */
    public static final String PREF_HELPTIPIDX = "help.helptip.Index";


    /** list of tips */
    private List helpTips = new ArrayList();

    /** resources */
    private XmlResourceCollection resources;

    /** index */
    private int idx = 0;

    /** label panel */
    private JPanel labelPanel;


    /** bottom panel */
    private JPanel bottom;

    /** right panel */
    private JPanel right;

    /** checkbox */
    private JCheckBox cbx;

    /** current action */
    private String currentAction;

    /** help target */
    private String helpTarget;

    /** editor */
    private JEditorPane editor;



    /** store */
    private XmlObjectStore store;

    /** action listener */
    private ActionListener actionListener;


    /**
     * Create the HelpTipDialog
     *
     * @param resources    list of XML resources
     * @param actionListener  listener for changes
     * @param store           store for persistence
     * @param origin          calling class
     * @param showByDefault   true to show by default
     *
     */
    public HelpTipDialog(XmlResourceCollection resources,
                         ActionListener actionListener, XmlObjectStore store,
                         Class origin, boolean showByDefault) {

        this.actionListener = actionListener;
        this.resources      = resources;
        if ((resources == null) || (resources.size() == 0)) {
            return;
        }

        this.store = store;
        //Get the next index to use.
        idx = getStore().get(PREF_HELPTIPIDX, -1) + 1;
        getStore().put(PREF_HELPTIPIDX, idx);
        getStore().save();
        String title = null;
        String icon  = null;


        for (int i = 0; i < resources.size(); i++) {
            Element helpTipRoot = resources.getRoot(i);
            if (helpTipRoot == null) {
                continue;
            }
            if (title == null) {
                title = XmlUtil.getAttribute(helpTipRoot, "title",
                                             (String) null);
            }
            if (icon == null) {
                icon = XmlUtil.getAttribute(helpTipRoot, "icon",
                                            (String) null);
            }
            helpTips.addAll(XmlUtil.findChildren(helpTipRoot, "helptip"));
        }

        if (title == null) {
            title = "Help tips";
        }
        setTitle(title);
        if (icon == null) {
            icon = "logo.gif";
        }
        JLabel    imageLabel = GuiUtils.getImageLabel(icon, origin);

        JMenu     topMenu    = new JMenu("Tips");
        Hashtable menus      = new Hashtable();
        menus.put("top", topMenu);
        for (int i = 0; i < helpTips.size(); i++) {
            Element helpTip = (Element) helpTips.get(i);
            String tipTitle = XmlUtil.getAttribute(helpTip, "title",
                                  (String) null);
            if (tipTitle == null) {
                String message = getMessage(helpTip);
                tipTitle = message.substring(0, 20);
            }
            if (tipTitle.trim().length() == 0) {
                continue;
            }

            String category = XmlUtil.getAttribute(helpTip, "category",
                                  "top");
            JMenu m = (JMenu) menus.get(category);
            if (m == null) {
                m = new JMenu(category);
                menus.put(category, m);
                topMenu.add(m);
            }
            JMenuItem mi = new JMenuItem(tipTitle);
            mi.addActionListener(new ObjectListener(new Integer(i)) {
                public void actionPerformed(ActionEvent ae) {
                    idx = ((Integer) theObject).intValue();
                    showTip();
                }
            });
            m.add(mi);
        }


        labelPanel = new JPanel();
        editor     = new JEditorPane();
        int height = 250;
        editor.setMinimumSize(new Dimension(350, height));
        editor.setPreferredSize(new Dimension(350, height));
        editor.setEditable(false);
        editor.addHyperlinkListener(this);
        editor.setContentType("text/html");
        //editor.setBackground (labelPanel.getBackground ());
        JScrollPane scroller = GuiUtils.makeScrollPane(editor, 350, height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(350, height));
        scroller.setMinimumSize(new Dimension(350, height));



        right = GuiUtils.doLayout(new Component[] { imageLabel,
                GuiUtils.hspace(10, 20) }, 1, GuiUtils.WT_Y, GuiUtils.WT_NY);

        JPanel contents = GuiUtils.centerRight(GuiUtils.inset(scroller, 8),
                              right);
        contents.add("North", GuiUtils.hspace(500, 10));
        JButton prevBtn = new JButton("Previous");
        prevBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                idx--;
                showTip();
                getStore().put(PREF_HELPTIPIDX, idx);
            }
        });

        JButton nextBtn = new JButton("Next");
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                next();
            }
        });

        cbx = new JCheckBox("Show tips on startup", showByDefault);
        cbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                writeShowNextTime();
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                close();
            }
        });


        JPanel buttons = GuiUtils.wrap(GuiUtils.hgrid(Misc.newList(prevBtn,
                             nextBtn, closeBtn), 2));

        bottom = GuiUtils.leftCenterRight(cbx, null, buttons);

        JMenuBar bar = new JMenuBar();
        bar.add(topMenu);
        contents.add("North", bar);
        contents.add("South", bottom);

        contents.setBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED));
        GuiUtils.packDialog(this, contents);
        showTip();
        Dimension size = getSize();
        Dimension ss   = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(ss.width / 2 - size.width / 2,
                    ss.height / 2 - size.height / 2);
        show();
    }

    /**
     * Write show next time
     */
    public void writeShowNextTime() {
        if (getStore().get(PREF_HELPTIPSHOW, true) != cbx.isSelected()) {
            getStore().put(PREF_HELPTIPSHOW, cbx.isSelected());
            getStore().save();
        }
    }

    /**
     * Close the dialog
     */
    public void close() {
        writeShowNextTime();
        setVisible(false);
    }

    /**
     * Get the persistence store
     * @return  the persistence
     */
    public XmlObjectStore getStore() {
        return store;
    }

    /**
     * Go to the next tip.
     */
    private void next() {
        idx++;
        showTip();
        getStore().put(PREF_HELPTIPIDX, idx);
    }

    /**
     * Handle a click on a link
     *
     * @param url  the link definition
     */
    public void click(String url) {
        if (url.trim().equals("next")) {
            next();
        } else {
            actionListener.actionPerformed(new ActionEvent(this, 0, url));
        }
    }

    /**
     * Handle a change to a link
     *
     * @param e  the link's event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                click(e.getDescription());
            } else {
                click(e.getURL().toString());
            }
        }
    }


    /**
     * Get the message for this tip
     *
     * @param helpTip  the tip node
     * @return  the message
     */
    private String getMessage(Node helpTip) {
        String message = XmlUtil.getAttribute(helpTip, "message",
                             (String) null);
        if (message == null) {
            message = XmlUtil.getChildText(helpTip);
        }
        return message;
    }

    /**
     * Show the current tip.
     */
    private void showTip() {
        if (helpTips.size() == 0) {
            return;
        }
        if (idx >= helpTips.size()) {
            idx = 0;
            getStore().put(PREF_HELPTIPIDX, idx);
        } else if (idx < 0) {
            idx = helpTips.size() - 1;
        }
        Node   helpTip = (Node) helpTips.get(idx);

        String title   = XmlUtil.getAttribute(helpTip, "title",
                             (String) null);

        String message = getMessage(helpTip);

        if (title != null) {
            message = "<h2>" + title + "</h2>" + message;
        }
        //      message =  message + "<p><a href=\"next\">Next</a>";
        currentAction = XmlUtil.getAttribute(helpTip, "action",
                                             (String) null);
        String actionLabel = XmlUtil.getAttribute(helpTip, "actionlabel",
                                 "More...");

        right.invalidate();
        helpTarget = XmlUtil.getAttribute(helpTip, "target", (String) null);

        List l = new ArrayList();
        labelPanel.removeAll();
        editor.setText(message);
        labelPanel.repaint();
        GuiUtils.vbox(labelPanel, l);
        labelPanel.validate();
    }




    /**
     * Show a particular help tip
     *
     * @param idx  the tip index
     */
    private void showHelpTip(int idx) {
        if (helpTips == null) {
            return;
        }
        if (idx >= helpTips.size()) {
            idx = 0;
        }
        Node   helpTip = (Node) helpTips.get(idx);
        String message = XmlUtil.getAttribute(helpTip, "message");
    }
}

