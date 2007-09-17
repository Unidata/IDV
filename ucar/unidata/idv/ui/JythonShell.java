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



package ucar.unidata.idv.ui;


import org.python.core.*;
import org.python.util.*;

import ucar.unidata.data.*;

import ucar.unidata.idv.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;


import java.io.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


import javax.swing.tree.*;



/**
 * This class provides  an interactive shell for running JYthon
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class JythonShell {

    /** _more_ */
    IntegratedDataViewer idv;

    /** _more_ */
    PythonInterpreter interp;

    /** _more_ */
    JFrame frame;

    /** _more_ */
    JTextField commandFld;

    /** _more_ */
    JEditorPane editorPane;

    /** _more_ */
    StringBuffer sb = new StringBuffer();

    /** _more_ */
    List history = new ArrayList();

    /** _more_ */
    int historyIdx = -1;



    /**
     * _more_
     *
     * @param theIdv _more_
     */
    public JythonShell(IntegratedDataViewer theIdv) {
        this.idv   = theIdv;
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        createInterpreter();
        JScrollPane scroller = GuiUtils.makeScrollPane(editorPane, 400, 300);
        scroller.setPreferredSize(new Dimension(400, 300));
        commandFld = new JTextField();
        commandFld.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == e.VK_B) && e.isControlDown()) {
                    if (commandFld.getCaretPosition() > 0) {
                        commandFld.setCaretPosition(
                            commandFld.getCaretPosition() - 1);
                    }
                }
                if ((e.getKeyCode() == e.VK_F) && e.isControlDown()) {
                    if (commandFld.getCaretPosition()
                            < commandFld.getText().length()) {
                        commandFld.setCaretPosition(
                            commandFld.getCaretPosition() + 1);
                    }
                }
                if (((e.getKeyCode() == e.VK_UP)
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
                        commandFld.setText((String) history.get(historyIdx));
                    }
                }
                if (((e.getKeyCode() == e.VK_DOWN)
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
                        commandFld.setText((String) history.get(historyIdx));
                    }
                }

            }
        });
        commandFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                eval();
            }
        });
        JButton    evalBtn  = GuiUtils.makeButton("Evaluate:", this, "eval");
        JComponent bottom   = GuiUtils.leftCenter(evalBtn, commandFld);
        JComponent contents = GuiUtils.centerBottom(scroller, bottom);
        contents = GuiUtils.inset(contents, 5);

        JMenuBar menuBar = new JMenuBar();
        List     items   = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Clear", this, "clear"));
        items.add(GuiUtils.makeMenuItem("Export Commands", this,
                                        "exportHistory"));
        menuBar.add(GuiUtils.makeMenu("File", items));


        items = new ArrayList();
        List displayMenuItems = new ArrayList();


        List cds = idv.getControlDescriptors();
        Hashtable catMenus = new Hashtable();
        for (int i = 0; i < cds.size(); i++) {
            ControlDescriptor cd =  (ControlDescriptor) cds.get(i);
            JMenu catMenu = (JMenu) catMenus.get(cd.getDisplayCategory());
            if(catMenu == null) {
                catMenu = new JMenu(cd.getDisplayCategory());
                catMenus.put(cd.getDisplayCategory(),catMenu);
                displayMenuItems.add(catMenu);
            }
            catMenu.add(GuiUtils.makeMenuItem(cd.getDescription(),this,"insert", "'"+cd.getControlId()+"'"));
        }


        items.add(GuiUtils.makeMenu("Insert Display Id", displayMenuItems));
        menuBar.add(GuiUtils.makeMenu("Edit", items));


        items = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Help", this, "showHelp"));
        menuBar.add(GuiUtils.makeMenu("Help", items));


        contents = GuiUtils.topCenter(menuBar, contents);
        frame    = new JFrame("Jython Shell");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                idv.getJythonManager().removeInterpreter(interp);
            }
        });
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);

    }




    public void insert(String s) {
        String t = commandFld.getText();
        int pos = commandFld.getCaretPosition();
        t = t.substring(0,pos) + s + t.substring(pos);
        commandFld.setText(t);
        commandFld.setCaretPosition(pos+s.length());
    }


    /**
     * _more_
     */
    public void showHelp() {
        idv.getIdvUIManager().showHelp("idv.tools.jythonshell");
    }

    /**
     * _more_
     */
    public void exportHistory() {
        if (history.size() == 0) {
            LogUtil.userMessage("There are no commands to export");
            return;
        }
        String procedureName =
            GuiUtils.getInput("Enter optional procedure name",
                              "Procedure name: ", "",
                              " (Leave blank for no procedure)");
        if (procedureName == null) {
            return;
        }
        String s;
        if (procedureName.trim().length() == 0) {
            s = StringUtil.join("\n", history);
        } else {
            s = "def " + procedureName + "():\n" + "    "
                + StringUtil.join("\n    ", history);
        }
        s = "#From shell\n" + s + "\n\n";
        idv.getJythonManager().appendJython(s);
    }



    /**
     * _more_
     */
    private void createInterpreter() {
        if (interp != null) {
            idv.getJythonManager().removeInterpreter(interp);
        }
        interp = idv.getJythonManager().createInterpreter();
        interp.set("shell", this);
        OutputStream os = new OutputStream() {
            public void write(int b) {
                //                    output(new String(b));
            }
            public void write(byte[] b, int off, int len) {
                output(new String(b, off, len));
            }
        };

        interp.setOut(os);
        interp.setErr(os);
    }


    /**
     * _more_
     */
    public void clear() {
        try {
            historyIdx = -1;
            history    = new ArrayList();
            sb         = new StringBuffer();
            createInterpreter();
            editorPane.setText("");
        } catch (Exception exc) {
            LogUtil.logException(
                "An error occurred clearing the Jython shell", exc);
        }
    }



    /**
     * _more_
     */
    public void eval() {
        String cmd = commandFld.getText();
        commandFld.setText("");
        eval(cmd);
        /*        if ((historyIdx >= 0) && (historyIdx < history.size())) {
            if (cmd.equals(history.get(historyIdx))) {
                return;
            }
            }*/
        history.add(cmd);
        historyIdx = -1;
    }

    /**
     * _more_
     *
     * @param m _more_
     */
    private void output(String m) {
        sb.append("<br>\n");
        sb.append(m);
        editorPane.setText(sb.toString());
        editorPane.repaint();
    }

    /**
     * _more_
     *
     * @param jython _more_
     */
    private void eval(String jython) {
        Misc.run(this, "evalInThread", jython);
    }

    /**
     * _more_
     *
     * @param jython _more_
     */
    public void evalInThread(String jython) {
        try {
            output("&gt;<i>" + jython + "</i>");
            interp.exec(jython);
        } catch (PySyntaxError pse) {
            output("Syntax error:<br>" + pse);
        } catch (Exception exc) {
            output("An error occurred:<br>" + exc);
        }
    }

}

