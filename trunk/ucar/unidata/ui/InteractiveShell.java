/*
 * $Id: DataTree.java,v 1.50 2007/08/21 12:15:45 jeffmc Exp $
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


import org.python.core.*;
import org.python.util.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;




/**
 * This class provides  an abstract interactive shell
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class InteractiveShell implements HyperlinkListener {

    private static Object MUTEX =  new Object();

    /** _more_ */
    protected JFrame frame;

    /** _more_ */
    protected JTextField commandFld;

    /** _more_ */
    protected JTextArea commandArea;

    /** _more_ */
    private JButton flipBtn;

    /** _more_ */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;

    /** _more_ */
    protected JEditorPane editorPane;

    /** _more_ */
    protected StringBuffer sb = new StringBuffer();

    private boolean bufferOutput = false;

    /** _more_ */
    protected List history = new ArrayList();

    /** _more_ */
    protected int historyIdx = -1;

    /** _more_          */
    private String title;

    /** _more_          */
    protected JComponent contents;





    /**
     * _more_
     *
     *
     * @param title _more_
     */
    public InteractiveShell(String title) {
        this.title = title;
    }

    /**
     * _more_
     */
    protected void makeFrame() {
        frame = new JFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);
        LogUtil.registerWindow(frame);
    }

    public void close() {
        frame.dispose();
    }


    public void show() {
        frame.setVisible(true);
    }

    protected String getHref(String text, String label) {
        String encoded = new String(XmlUtil.encodeBase64(("text:" + text).getBytes()));
        return "<a href=\"" + encoded +"\">"+label+"</a>";
    }

    protected void showWaitCursor() {
        frame.setCursor(GuiUtils.waitCursor);
    }

    protected void showNormalCursor() {
        frame.setCursor(GuiUtils.normalCursor);
    }

    /**
     * _more_
     */
    protected void init() {
        contents = doMakeContents();
        makeFrame();
    }


    /**
     * _more_
     *
     * @param e _more_
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        String url;
        if (e.getURL() == null) {
            url = e.getDescription();
        } else {
            url = e.getURL().toString();
        }
        url = new String(XmlUtil.decodeBase64(url));
        if(url.startsWith("eval:")) {
            Misc.run(this, "eval", url.substring(5));
        } else  if(url.startsWith("text:")) {
            setText(url.substring(5));
        }
    }


    public void setText(String text) {
        getCommandFld().setText(text);
        getCommandFld().requestFocus();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeContents() {
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.addHyperlinkListener(this);
        JScrollPane scroller = GuiUtils.makeScrollPane(editorPane, 400, 300);
        scroller.setPreferredSize(new Dimension(400, 300));
        commandFld = new JTextField();
        GuiUtils.setFixedWidthFont(commandFld);
        GuiUtils.addKeyBindings(commandFld);
        commandFld.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandFld);
            }
        });
        commandFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                eval();
            }
        });
        commandFld.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightMouseClick(commandFld, e);
                }
            }
        });


        commandArea = new JTextArea("", 4, 30);
        GuiUtils.setFixedWidthFont(commandArea);
        GuiUtils.addKeyBindings(commandArea);
        commandArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandArea);
            }
        });
        commandArea.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightMouseClick(commandArea, e);
                }
            }
        });
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();

        //        cardLayoutPanel.addCard(GuiUtils.top(GuiUtils.inset(commandFld,2)));
        cardLayoutPanel.addCard(GuiUtils.top(commandFld));
        cardLayoutPanel.addCard(GuiUtils.makeScrollPane(commandArea, 200,
                100));
        flipBtn = GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif",
                                           this, "flipField");
        JButton evalBtn = GuiUtils.makeButton("Evaluate:", this, "eval");
        JComponent bottom = GuiUtils.leftCenterRight(GuiUtils.top(evalBtn),
                                                     GuiUtils.inset(cardLayoutPanel, 2),GuiUtils.top(flipBtn));
        JComponent contents = GuiUtils.vsplit(scroller, bottom,300);
        contents = GuiUtils.inset(contents, 5);

        JMenuBar menuBar = doMakeMenuBar();
        if (menuBar != null) {
            contents = GuiUtils.topCenter(menuBar, contents);
        }
        return contents;
    }

    /**
     * _more_
     *
     * @param commandFld _more_
     * @param e _more_
     */
    protected void handleRightMouseClick(JTextComponent commandFld,
                                         MouseEvent e) {}


    /**
     * _more_
     *
     * @return _more_
     */
    protected JMenuBar doMakeMenuBar() {
        return null;
    }

    /**
     * _more_
     */
    public void toFront() {
        GuiUtils.toFront(frame);
    }

    /**
     * _more_
     */
    public void flipField() {
        cardLayoutPanel.flip();
        if (getCommandFld() instanceof JTextArea) {
            flipBtn.setIcon(
                GuiUtils.getImageIcon("/auxdata/ui/icons/UpUp.gif"));
        } else {
            flipBtn.setIcon(
                GuiUtils.getImageIcon("/auxdata/ui/icons/DownDown.gif"));
        }

    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void insertText(String t) {
        GuiUtils.insertText(getCommandFld(), t);
    }


    /**
     * _more_
     *
     * @param e _more_
     * @param cmdFld _more_
     */
    protected void handleKeyPress(KeyEvent e, JTextComponent cmdFld) {
        boolean isArea  = (cmdFld instanceof JTextArea);
        if (((!isArea&& e.getKeyCode() == e.VK_UP)
                || ((e.getKeyCode() == e.VK_P)
                    && e.isControlDown())) && (history.size() > 0)) {
            if ((historyIdx < 0) || (historyIdx >= history.size())) {
                historyIdx = history.size() - 1;
            } else {
                historyIdx--;
                if (historyIdx < 0) {
                    historyIdx = 0;
                }
            }
            if ((historyIdx >= 0) && (historyIdx < history.size())) {
                cmdFld.setText((String) history.get(historyIdx));
            }
        }
        if (((!isArea && e.getKeyCode() == e.VK_DOWN)
                || ((e.getKeyCode() == e.VK_N)
                    && e.isControlDown())) && (history.size() > 0)) {
            if ((historyIdx < 0) || (historyIdx >= history.size())) {
                historyIdx = history.size() - 1;
            } else {
                historyIdx++;
                if (historyIdx >= history.size()) {
                    historyIdx = history.size() - 1;
                }
            }
            if ((historyIdx >= 0) && (historyIdx < history.size())) {
                cmdFld.setText((String) history.get(historyIdx));
            }
        }

    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void insert(String s) {
        String t   = getCommandFld().getText();
        int    pos = getCommandFld().getCaretPosition();
        t = t.substring(0, pos) + s + t.substring(pos);
        getCommandFld().setText(t);
        getCommandFld().setCaretPosition(pos + s.length());
    }




    /**
     * _more_
     */
    public void clear() {
        historyIdx = -1;
        history    = new ArrayList();
        clearOutput();
    }

    public void clearOutput() {
        sb         = new StringBuffer();
        editorPane.setText("");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private JTextComponent getCommandFld() {
        if (cardLayoutPanel.getVisibleIndex() == 0) {
            return commandFld;
        }
        return commandArea;
    }



    /**
     * _more_
     */
    public void eval() {
        JTextComponent cmdFld = getCommandFld();
        String         cmd    = cmdFld.getText();
        if(cmd.trim().equals("!!")) {
            if(history.size()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            cmd  = (String)history.get(history.size()-1);
        }  else if(cmd.trim().startsWith("!")) {
            if(history.size()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String prefix = cmd.substring(1);
            cmd = null;
            for(int i=history.size()-1;i>=0;i--) {
                String tmp = (String)history.get(i);
                if(tmp.startsWith(prefix)) {
                    cmd = tmp;
                    break;
                }
            }
            if(cmd==null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
        }
        cmdFld.setText("");
        history.add(cmd);
        historyIdx = -1;
        Misc.run(this, "eval", cmd);
    }

    

    protected void startBufferingOutput() {
        bufferOutput = true;
    }

    protected void endBufferingOutput() {
        bufferOutput = false;
        updateText();
    }


    private void updateText() {
        editorPane.setText(sb.toString());
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        editorPane.scrollRectToVisible(new Rectangle(0, 10000, 1, 1));
                    } catch(Exception exc) {}}});
    }

    /**
     * _more_
     *
     * @param m _more_
     */
    public void output(String m) {
        sb.append(m);
         if(!bufferOutput) {
            updateText();
        }
    }



    /**
     * _more_
     *
     * @param code the code that was evaluated
     */
    public void eval(String code) {
        String evalCode = "eval:"+code;
        String encoded1 = new String(XmlUtil.encodeBase64(evalCode.getBytes()));
        String textCode = "text:"+code;
        String encoded2 = new String(XmlUtil.encodeBase64(textCode.getBytes()));
        output("<div style=\"margin:0; margin-bottom:1; background-color:#cccccc; \"><table width=\"100%\"><tr><td>"
               + formatCode(code)
               + "</td><td align=\"right\" valign=\"top\"><a href=\""
               + encoded2
               + "\"><img src=\"idvresource:/auxdata/ui/icons/Down16.gif\" border=0></a>"+
               "<a href=\""
               + encoded1
               + "\"><img alt=\"Reload\" src=\"idvresource:/auxdata/ui/icons/Refresh16.gif\" border=0></a></td></tr></table></div>");
    }


    /**
     * _more_
     *
     * @param code _more_
     *
     * @return _more_
     */
    protected String formatCode(String code) {
        return code;
    }


}

