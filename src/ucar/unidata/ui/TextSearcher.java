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

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;




import javax.swing.text.*;

import javax.swing.tree.*;


/**
 * This provides a text search bar for a JTextComponent
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class TextSearcher extends JPanel {

    /** _more_ */
    private JTextField findFld;

    /** _more_ */
    private JCheckBox caseCbx;

    /** _more_ */
    JToggleButton highlightAllBtn;


    /** _more_          */
    private TextWrapper textWrapper;

    /** _more_          */
    public static final Color COLOR_BADSEARCH = new Color(255, 102, 102);

    /**
     * _more_
     *
     * @param comp _more_
     */
    public TextSearcher(JTextComponent comp) {
        textWrapper = new TextWrapper(comp);
        init();
    }


    /**
     * _more_
     */
    public TextSearcher() {
        init();
    }



    /**
     * _more_
     */
    private void init() {
        this.setLayout(new BorderLayout());

        highlightAllBtn = GuiUtils.getToggleImageButton(
            "/ucar/unidata/ui/images/SearchHighlightOff16.gif",
            "/ucar/unidata/ui/images/SearchHighlightOn16.gif", 0, 0, true);
        highlightAllBtn.addActionListener(GuiUtils.makeActionListener(this,
                "searchFor", null));

        highlightAllBtn.setToolTipText("Highlight All");
        caseCbx = new JCheckBox("Match case", false);
        caseCbx.addActionListener(GuiUtils.makeActionListener(this,
                "searchFor", null));
        findFld = new JTextField("", 20);
        JButton nextBtn =
            GuiUtils.makeImageButton("/ucar/unidata/ui/images/SearchNext16.gif",
                                     this, "doSearch", null, true);
        JButton prevBtn =
            GuiUtils.makeImageButton("/ucar/unidata/ui/images/SearchPrev16.gif",
                                     this, "doSearchPrevious", null, true);
        nextBtn.setToolTipText("Find Next");
        prevBtn.setToolTipText("Find Previous");



        findFld.addActionListener(GuiUtils.makeActionListener(this,
                "doSearch", null));
        findFld.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown()) {
                    return;
                }
                if (e.getKeyCode() != e.VK_ENTER) {
                    searchFor(findFld.getText(), true);
                }
            }
        });


        JComponent buttons = GuiUtils.hbox(Misc.newList(nextBtn, prevBtn,
                                 highlightAllBtn, caseCbx), 2);
        JComponent bottomPanel =
            GuiUtils.left(GuiUtils.hbox(new JLabel(" Find: "),
                                        GuiUtils.hfill(findFld), buttons));

        this.add(BorderLayout.CENTER, bottomPanel);

    }

    /**
     * _more_
     *
     * @param text _more_
     * @param next _more_
     */
    private void searchFor(String text, boolean next) {
        TextWrapper holder = getTextWrapper();
        if(holder==null) return;
        if ( !holder.find(text, highlightAllBtn.isSelected(),
                          caseCbx.isSelected(), next)) {
            findFld.setBackground(COLOR_BADSEARCH);
        } else {
            findFld.setBackground(Color.white);
        }
    }

    /**
     * _more_
     */
    public void searchFor() {
        searchFor(findFld.getText(), true);
    }

    /**
     * _more_
     */
    public void doSearch() {
        TextWrapper holder = getTextWrapper();
        holder.resetToCurrentSearchIndex();
        searchFor(findFld.getText(), true);
    }

    /**
     * _more_
     */
    public void doSearchPrevious() {
        TextWrapper holder = getTextWrapper();
        holder.resetToCurrentSearchIndex();
        searchFor(findFld.getText(), false);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public TextSearcher.TextWrapper getTextWrapper() {
        return textWrapper;
    }



    /**
     * Class TextWrapper _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class TextWrapper {

        /** _more_          */
        JTextComponent textComp;

        /** _more_ */
        String lastSearch;

        /** _more_ */
        int lastIndex = -1;

        /** _more_ */
        int currentIndex = -1;

        /**
         * _more_
         */
        public TextWrapper() {
            if (allPainter == null) {
                allPainter = new DefaultHighlighter.DefaultHighlightPainter(
                    Color.yellow);
                onePainter = new DefaultHighlighter.DefaultHighlightPainter(
                    Color.green);
            }
        }

        /**
         * _more_
         *
         * @param comp _more_
         */
        public TextWrapper(JTextComponent comp) {
            this();
            textComp = comp;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public JTextComponent getTextComponent() {
            return textComp;
        }

        /**
         * _more_
         *
         * @param comp _more_
         */
        protected void setTextComponent(JTextComponent comp) {
            textComp = comp;
        }


        /**
         * _more_
         *
         * @param pt _more_
         */
        public void setSearchIndex(Point pt) {
            Highlighter highlighter = textComp.getHighlighter();
            removeHighlights();
            lastIndex  = textComp.viewToModel(pt);
            lastSearch = null;
        }


        /**
         * _more_
         */
        public void removeHighlights() {
            Highlighter highlighter = textComp.getHighlighter();
            for (int i = 0; i < highlights.size(); i++) {
                highlighter.removeHighlight(highlights.get(i));
            }
        }

        /** _more_ */
        List highlights = new ArrayList();

        /** _more_ */
        static DefaultHighlighter.DefaultHighlightPainter allPainter;

        /** _more_ */
        static DefaultHighlighter.DefaultHighlightPainter onePainter;

        /**
         * _more_
         */
        public void resetToCurrentSearchIndex() {
            lastIndex = currentIndex;
        }

        /**
         * _more_
         *
         * @param what _more_
         * @param t _more_
         */
        private void highlightAll(String what, String t) {
            if (what.length() == 0) {
                return;
            }
            Highlighter highlighter = textComp.getHighlighter();
            int         baseIndex   = 0;
            int         index;
            int         lastIndex = -1;
            try {
                while ((index = t.indexOf(what, baseIndex)) >= 0) {
                    if (index == lastIndex) {
                        break;
                    }
                    lastIndex = index;
                    highlights.add(highlighter.addHighlight(index,
                            index + what.length(), allPainter));
                    baseIndex = index + 1;
                }
            } catch (Exception exc) {
                LogUtil.logException("Bad highlight area", exc);
            }

        }


        /**
         * _more_
         *
         * @param what _more_
         * @param highlightAll _more_
         * @param doCase _more_
         * @param next _more_
         *
         * @return _more_
         */
        public boolean find(String what, boolean highlightAll,
                            boolean doCase, boolean next) {
            String t = textComp.getText();
            if ( !doCase) {
                t = t.toLowerCase();
                what = what.toLowerCase();
            }
            int start = 0;
            start = lastIndex + 1;
            Highlighter highlighter = textComp.getHighlighter();
            removeHighlights();
            if ( !next) {
                String prevText = t;
                if (currentIndex >= 0) {
                    prevText = prevText.substring(0, currentIndex);
                }
                currentIndex = prevText.lastIndexOf(what, start);
            } else {
                currentIndex = t.indexOf(what, start);
            }
            if (currentIndex >= 0) {
                try {
                    highlights.add(highlighter.addHighlight(currentIndex,
                            currentIndex + what.length(), onePainter));
                    textComp.setCaretPosition(currentIndex);
                } catch (Exception exc) {
                    LogUtil.logException("Bad highlight area", exc);
                }
            }

            if (highlightAll) {
                highlightAll(what, t);
            }
            return currentIndex >= 0;
        }


    }




}

