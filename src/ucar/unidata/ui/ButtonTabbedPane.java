/*
 * $Id: ButtonTabbedPane.java,v 1.8 2007/07/06 20:45:29 jeffmc Exp $
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


import ucar.unidata.util.GuiUtils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;



import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * This is a vast catchall class to old various
 * utilities for doing GUI things.
 *
 *
 * @author IDV development team
 */
public class ButtonTabbedPane extends JPanel {


    /** _more_ */
    private static Image BUTTON_ICON;

    /** _more_ */
    public static final Color BUTTON_FG_COLOR = Color.black;

    /** _more_ */
    public static final Color BUTTON_ON_COLOR = Color.gray.brighter();

    /** _more_ */
    public static final Color BUTTON_LINE_COLOR = Color.gray;

    /** _more_ */
    private static final Font BUTTON_FONT = new Font("Dialog", Font.BOLD, 12);

    /** _more_ */
    private static final Border BUTTON_BORDER =
        BorderFactory.createEmptyBorder(4, 6, 2, 0);


    /** _more_ */
    private List buttonList = new ArrayList();

    /** _more_ */
    private List contentList = new ArrayList();

    /** _more_ */
    private JComponent buttonPanel;

    /** _more_ */
    private ComponentPanel rightPanel;

    /** _more_ */
    private ButtonGroup buttonGroup = new ButtonGroup();

    /** _more_ */
    private int width;

    /** _more_ */
    private boolean deleteEnabled = false;

    /**
     * _more_
     */
    public ButtonTabbedPane() {
        this(null);
    }



    /**
     * _more_
     *
     * @param width _more_
     */
    public ButtonTabbedPane(int width) {
        this(null, width);
    }


    /**
     * _more_
     *
     * @param label _more_
     */
    public ButtonTabbedPane(JComponent label) {
        this(label, 150);
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param width _more_
     */
    public ButtonTabbedPane(JComponent label, int width) {
        this.width  = width;
        buttonPanel = new JPanel(new BorderLayout());
        JComponent buttonPanelHolder = GuiUtils.top(buttonPanel);
        JScrollPane sp = new JScrollPane(buttonPanelHolder);
        buttonPanelHolder.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        sp.setBorder(null);
        buttonPanelHolder = sp;

        JComponent labelComp;
        JComponent filler = ((width < 0)
                             ? new JPanel()
                             : GuiUtils.filler(width, 1));
        if (label != null) {
            labelComp = GuiUtils.vbox(filler, label);
        } else {
            labelComp = filler;
        }
        JComponent leftPanel = GuiUtils.topCenter(labelComp,
                                   buttonPanelHolder);
        rightPanel = new ComponentPanel(this);
        this.setLayout(new BorderLayout());
        this.add(BorderLayout.WEST, leftPanel);
        this.add(BorderLayout.CENTER, rightPanel);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Image getIcon() {
        if (BUTTON_ICON == null) {
            BUTTON_ICON = GuiUtils.getImage("/auxdata/ui/icons/Selected.gif");
            BUTTON_ICON.getWidth(this);
        }
        return BUTTON_ICON;
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param button _more_
     */
    protected void paintButton(Graphics g, AbstractButton button) {
        getIcon();
        Rectangle b = button.getBounds();
        g.setColor(button.getBackground());
        g.setFont(button.getFont());
        if (button.isSelected()) {
            g.setColor(BUTTON_ON_COLOR);
        }
        g.fillRect(0, 0, b.width, b.height);
        g.setColor(BUTTON_LINE_COLOR);
        g.drawLine(0, b.height - 1, b.width, b.height - 1);
        g.setColor(BUTTON_FG_COLOR);
        String      text           = button.getText();
        FontMetrics fm             = g.getFontMetrics(g.getFont());
        int         y = (button.getHeight() + fm.getHeight()) / 2 - 2;
        int         offset         = 2 + BUTTON_ICON.getWidth(null) + 4;
        int         textWidth      = fm.stringWidth(text);
        int         availableWidth = b.width - offset - 2;
        while ((textWidth > availableWidth) && (text.length() > 5)) {
            text      = text.substring(0, text.length() - 2);
            textWidth = fm.stringWidth(text);
        }
        g.drawString(text, offset, y);
        //      g.drawLine(offset, y, offset+width, y);
        if (button.isSelected()) {
            int imageHeight = BUTTON_ICON.getHeight(null);
            g.drawImage(BUTTON_ICON, 2, b.height / 2 - imageHeight / 2, null);
        }
    }




    /**
     * _more_
     *
     * @param img _more_
     * @param flags _more_
     * @param x _more_
     * @param y _more_
     * @param width _more_
     * @param height _more_
     *
     * @return _more_
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if ((flags & ImageObserver.ALLBITS) != 0) {
            buttonPanel.repaint();
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param idx _more_
     */
    private void buttonsChanged(int idx) {
        List    tmp         = new ArrayList(buttonList);
        boolean anySelected = false;
        if (idx < 0) {
            for (int i = 0; (i < tmp.size()) && !anySelected; i++) {
                JToggleButton btn = (JToggleButton) tmp.get(i);
                anySelected = btn.isSelected();
            }
            if ( !anySelected && (tmp.size() > 0)) {
                select((JToggleButton) tmp.get(tmp.size() - 1));
            }
        } else {
            while ((idx >= 0) && (idx >= tmp.size())) {
                idx--;
            }
            if ((idx >= 0) && (idx < tmp.size())) {
                select((JToggleButton) tmp.get(idx));
            }
        }

        buttonPanel.removeAll();
        buttonPanel.add(BorderLayout.NORTH, GuiUtils.vbox(buttonList));
        buttonPanel.repaint();
    }


    /**
     * _more_
     *
     * @param i _more_
     */
    public void setSelectedIndex(int i) {
        show((Component) contentList.get(i));
    }

    /**
     * _more_
     *
     * @param i _more_
     *
     * @return _more_
     */
    public String getTitleAt(int i) {
        return ((AbstractButton) buttonList.get(i)).getText();
    }


    /**
     * _more_
     *
     * @param content _more_
     */
    public void show(Component content) {
        for (int i = 0; i < contentList.size(); i++) {
            if (content == contentList.get(i)) {
                ((JToggleButton) buttonList.get(i)).setSelected(true);
            }
        }
        rightPanel.show(content);
    }


    /**
     * _more_
     *
     * @param button _more_
     * @param contents _more_
     */
    public void remove(JToggleButton button, Component contents) {
        int idx = buttonList.indexOf(button);
        if ( !button.isSelected()) {
            idx = -1;
        }
        buttonGroup.remove(button);
        buttonList.remove(button);
        contentList.remove(contents);
        rightPanel.remove(contents);
        buttonsChanged(idx);
    }

    /**
     * Select the tab that holds the component.
     * @param comp  component in the hierarchy of the tabs main component
     */
    public void selectTabForComponent(Component comp) {
        for (int i = 0; i < contentList.size(); i++) {
            Component c = (Component) contentList.get(i);
            if ((c == comp) || ((Container) c).isAncestorOf(comp)) {
                ((JToggleButton) buttonList.get(i)).setSelected(true);
            }
        }
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param contents _more_
     *
     * @return _more_
     */
    public JToggleButton addTab(String label, final Component contents) {
        final JToggleButton[] tmp    = { null };
        JToggleButton         button = tmp[0] = new JToggleButton(label) {
            public void paint(Graphics g) {
                paintButton(g, tmp[0]);
            }
        };
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ce) {
                if (tmp[0].isSelected()) {
                    rightPanel.show(contents);
                }
            }
        });

        button.setForeground(BUTTON_FG_COLOR);
        button.setBorder(BUTTON_BORDER);
        int height = button.getPreferredSize().height;
        if(GuiUtils.checkHeight(height)) {
            if (width > 0) {
                button.setPreferredSize(new Dimension(width, height));
            } else {
                button.setPreferredSize(
                                        new Dimension(
                                                      button.getPreferredSize().width + 10,
                                                      height));
            }
        }

        addTab(button, contents);
        return button;
    }



    /**
     * _more_
     *
     * @param b _more_
     */
    public void setDeleteEnabled(boolean b) {
        deleteEnabled = b;
    }

    /**
     * _more_
     *
     * @param button _more_
     * @param contents _more_
     */
    public void addTab(final JToggleButton button, final Component contents) {
        buttonGroup.add(button);
        buttonList.add(button);
        contentList.add(contents);
        rightPanel.addCard(contents);
        buttonsChanged(-1);
        select(button);
        button.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (deleteEnabled && GuiUtils.isDeleteEvent(e)) {
                    remove(button, contents);
                } else if (code == KeyEvent.VK_DOWN) {
                    int idx = buttonList.indexOf(button) + 1;
                    if (idx < buttonList.size()) {
                        select((JToggleButton) buttonList.get(idx));
                    }
                } else if (code == KeyEvent.VK_UP) {
                    int idx = buttonList.indexOf(button) - 1;
                    if (idx >= 0) {
                        select((JToggleButton) buttonList.get(idx));
                    }
                }
            }
        });
    }

    /**
     * _more_
     *
     * @param b _more_
     */
    private void select(JToggleButton b) {
        b.doClick();
        b.requestFocusInWindow();
    }



    /**
     * Class ComponentPanel _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.8 $
     */
    public static class ComponentPanel extends GuiUtils.CardLayoutPanel {

        /** _more_          */
        ButtonTabbedPane tab;

        /**
         * _more_
         *
         * @param tab _more_
         */
        public ComponentPanel(ButtonTabbedPane tab) {
            this.tab = tab;
        }

        /**
         * _more_
         *
         * @param comp _more_
         */
        public void show(Component comp) {
            super.show(comp);
            tab.selectTabForComponent(comp);
        }



    }

}

