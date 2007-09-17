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

import ucar.unidata.idv.*;
import ucar.unidata.data.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.awt.*;
import java.awt.event.*;


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

    IntegratedDataViewer idv;
    PythonInterpreter interp;
    JFrame frame;
    JTextField commandFld;
    JEditorPane editorPane;
    StringBuffer sb = new StringBuffer();
    List history = new ArrayList();
    int historyIdx = -1;

    public JythonShell(IntegratedDataViewer theIdv) {
        this.idv = theIdv;
        interp = idv.getJythonManager().createInterpreter();
        interp.set("shell", this);
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");

        OutputStream os = new OutputStream() {
                public  void 	write(int b) {
                    //                    output(new String(b));
                }
                public void 	write(byte[] b, int off, int len) {
                    output(new String(b,off,len));
                } 
            };

        interp.setOut(os);
        interp.setErr(os);

        JScrollPane scroller = GuiUtils.makeScrollPane(editorPane, 400,300);
        scroller.setPreferredSize(new Dimension(400,300));
        commandFld = new  JTextField();
        commandFld.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == e.VK_P && e.isControlDown() && history.size()>0) {
                        if(historyIdx <0 || historyIdx>= history.size())
                            historyIdx = history.size()-1;
                        else
                            historyIdx--;
                        if(historyIdx>=0 && historyIdx<history.size())
                            commandFld.setText((String)history.get(historyIdx));
                    }
                    if (e.getKeyCode() == e.VK_N && e.isControlDown() && history.size()>0) {
                        if(historyIdx <0 || historyIdx>= history.size())
                            historyIdx = history.size()-1;
                        else
                            historyIdx++;
                        if(historyIdx>=0 && historyIdx<history.size())
                            commandFld.setText((String)history.get(historyIdx));
                    }

                }});
        commandFld.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    eval();
                }
            });
        JButton evalBtn = GuiUtils.makeButton("Evaluate:", this,"eval");
        JComponent bottom = GuiUtils.leftCenter(evalBtn,commandFld);
        JComponent contents = GuiUtils.centerBottom(scroller,bottom);
        contents = GuiUtils.inset(contents,5);
        frame       = new JFrame("Jython Shell");
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

    public void clear() {
        sb = new StringBuffer();
        editorPane.setText(sb.toString());
    }

    public void eval() {
            String cmd = commandFld.getText();
            commandFld.setText("");
            history.add(cmd);
            historyIdx= -1;
            eval(cmd);
    }

    private void output(String m) {
        sb.append("<br>\n");
        sb.append(m);
        editorPane.setText(sb.toString());
        editorPane.repaint();
    }

    private void eval(String jython) {
        Misc.run(this, "evalInThread", jython);
    }

    public void evalInThread(String jython) {
        try {
            output("&gt;<i>" +jython+"</i>");
            interp.exec(jython);
        } catch (PySyntaxError pse) {
            output("Syntax error:<br>"+pse);
        } catch (Exception exc) {
            output("An error occurred:<br>"+exc);
        }
    }

}

