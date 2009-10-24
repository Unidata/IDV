/*
 * $Id: WindowHolder.java,v 1.6 2007/07/06 20:45:34 jeffmc Exp $
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
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;



import javax.swing.event.*;


/**
 * This is a base class that holds a dialog
 *
 *
 * @author IDV development team
 */


public abstract class WindowHolder implements ActionListener {

    /** _more_ */
    protected JComponent contents;

    /** _more_ */
    protected JDialog dialog;

    /** _more_ */
    protected JFrame frame;

    /** _more_ */
    protected Window window;

    private  JMenuBar menuBar;

    /**
     * Construct this object
     *
     */
    public WindowHolder() {}


    public void setMenuBar(JMenuBar menuBar) {
	this.menuBar = menuBar;

	if(frame!=null) {
	    GuiUtils.decorateFrame(frame, menuBar);
	} 
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Window getWindow() {
        if (windowOk()) {
            return null;
        }
        return window;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldMakeDialog() {
        return false;
    }

    /**
     * Bring the dialog to the front
     */
    public void toFront() {
        if ( !windowOk()) {
            return;
        }
        window.toFront();
    }

    /**
     * _more_
     */
    protected void windowIsClosing() {}



    /**
     * _more_
     *
     * @return _more_
     */
    private boolean windowOk() {
        getContents();
        if (contents == null) {
            return false;
        }
        if (shouldMakeDialog()) {
            if (dialog == null) {
                dialog = new JDialog((Frame) null, getWindowTitle(), false);
                LogUtil.registerWindow(dialog);
                GuiUtils.packDialog(dialog, contents);
                dialog.setLocation(100, 100);
                window = dialog;
            }
        } else {
            if (frame == null) {
                frame = new JFrame(getWindowTitle());
		if(menuBar !=null) {
		    GuiUtils.decorateFrame(frame, menuBar);
		}
                LogUtil.registerWindow(frame);
                frame.getContentPane().add(contents);
                frame.pack();
                Msg.translateTree(frame);
                frame.setLocation(100, 100);
                window = frame;
            }
        }
        if (window != null) {
            window.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    windowIsClosing();
                }
            });

        }
        return window != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JDialog getDialog() {
        windowOk();
        return dialog;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JFrame getFrame() {
        windowOk();
        return frame;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isShowing() {
        if ( !windowOk()) {
            return false;
        }
        return getWindow().isShowing();
    }

    /**
     * _more_
     */
    public void removeAll() {
        if (dialog != null) {
            dialog.getContentPane().removeAll();
        }

        if (frame != null) {
            frame.getContentPane().removeAll();
        }
    }

    /**
     * Open me
     */
    public void show() {
        show(false);
    }

    /**
     * _more_
     */
    public void showModal() {

        show(true);
    }

    /**
     * Set the title of the window to the given title
     *
     * @param title The window title
     */
    public void setWindowTitle(String title) {
        if ( !windowOk()) {
            return;
        }
        if (dialog != null) {
            dialog.setTitle(title);
        }
        if (frame != null) {
            frame.setTitle(title);
        }
    }




    /**
     * Open me
     *
     * @param modal _more_
     */
    public void show(boolean modal) {
        if ( !windowOk()) {
            return;
        }
        if (dialog != null) {
            dialog.setModal(modal);
            dialog.show();
        }
        if (frame != null) {
            frame.show();
        }

        if (window != null) {
            GuiUtils.showWidget(window);
        }
    }


    /**
     * Close me
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            if (dialog.isModal()) {
                dialog.setModal(false);
            }
        }

        if (frame != null) {
            frame.setVisible(false);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getWindowTitle() {
        return "";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeContents() {
        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }



    /**
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            close();
        }
    }



}

