/*
 * $Id: MultiFrame.java,v 1.7 2007/07/06 20:45:32 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * A class that holds either a JFrame or a JInteralFrame. It allows for client
 * code to eqaily switch between regular windows and JDesktop windows
 *
 * @author IDV development team
 */
public class MultiFrame {

    /** Global to add the internal frames to as a default behavior */
    private static JDesktopPane desktopPane;

    /** Used for event processing */
    private static Window dummyWindow;




    /** The frame */
    private JFrame frame;

    /** The internal frame */
    private JInternalFrame internalFrame;

    /** mapping from windowlistener to internalframelistener */
    private Hashtable listeners = new Hashtable();

    /** _more_          */
    private List<WindowListener> frameListeners =
        new ArrayList<WindowListener>();

    /**
     * Set the global desktopPane. This causes all default MultiFrames
     * to be an internalFrame.
     *
     * @param desktopPane desktop pane
     */
    public static void useDesktopPane(JDesktopPane desktopPane) {
        MultiFrame.desktopPane = desktopPane;
    }



    /**
     * ctor
     */
    public MultiFrame() {
        this("");
    }


    /**
     * ctor
     *
     * @param title Create a JFrame with the given title
     */
    public MultiFrame(String title) {
        if (desktopPane != null) {
            internalFrame = new JInternalFrame(title, true, true, true, true);
            desktopPane.add(internalFrame);
        } else {
            frame = new JFrame(title);
        }
    }

    /**
     * ctor
     *
     * @param frame The frame
     */
    public MultiFrame(JFrame frame) {
        this.frame = frame;
    }

    /**
     * ctor
     *
     * @param internalFrame The internal frame
     */
    public MultiFrame(JInternalFrame internalFrame) {
        this.internalFrame = internalFrame;
    }


    /**
     * Show the component
     */
    public void show() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.show();
        } else if (internalFrame != null) {
            internalFrame.show();
        }
    }


    /**
     * dispose of the component
     */
    public void dispose() {
        if ((frame == null) && (internalFrame == null)) {
            return;
        }
        JFrame theFrame = frame;
        if (theFrame != null) {
            if (frameListeners != null) {
                for (WindowListener listener : frameListeners) {
                    theFrame.removeWindowListener(listener);
                }
            }
            theFrame.dispose();
        } else if (internalFrame != null) {
            internalFrame.dispose();
        }

        desktopPane    = null;
        frame          = null;
        internalFrame  = null;
        listeners      = null;
        frameListeners = null;
    }


    /**
     * set visibility of the component
     *
     * @param visible visible
     */
    public void setVisible(boolean visible) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setVisible(visible);
        } else if (internalFrame != null) {
            internalFrame.setVisible(visible);
        }
    }

    /**
     * Get the content pane of the component
     *
     * @return content pane
     */
    public Container getContentPane() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            return theFrame.getContentPane();
        } else if (internalFrame != null) {
            return internalFrame.getContentPane();
        } else {
            return null;
        }
    }


    /**
     * Get the container
     *
     * @return Either the frame or the internalFrame
     */
    public Container getContainer() {
        if (frame != null) {
            return frame;
        } else {
            return internalFrame;
        }
    }

    /**
     * Set the cursor
     *
     * @param cursor cursor
     */
    public void setCursor(Cursor cursor) {
        Window window = getWindow();
        if(window!=null) {
            window.setCursor(cursor);
        }
    }


    /**
     * wrapper method
     *
     * @param state state
     */
    public void setState(int state) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setState(state);
        } else if (internalFrame != null) {
            try {
                if (state == Frame.NORMAL) {
                    internalFrame.setIcon(false);
                } else {
                    internalFrame.setIcon(true);
                }
            } catch (Exception exc) {
                LogUtil.logException("MultiFrame", exc);
            }
        }
    }

    /**
     * Set the title
     *
     * @param title The title
     */
    public void setTitle(String title) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setTitle(title);
        } else if (internalFrame != null) {
            internalFrame.setTitle(title);
        }
    }


    /**
     * Get the title
     *
     * @return The title
     */
    public String getTitle() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            return theFrame.getTitle();
        } else if (internalFrame != null) {
            return internalFrame.getTitle();
        } else {
            return "";
        }
    }


    /**
     * access the JFrame
     *
     * @return The frame
     */
    public JFrame getFrame() {
        return frame;
    }


    /**
     * Access the internal frame
     *
     * @return internal frame
     */
    public JInternalFrame getInternalFrame() {
        return internalFrame;
    }

    /**
     * Finds the Window we are a part of
     *
     * @return the window
     */
    public Window getWindow() {
        if (frame != null) {
            return frame;
        } else if (internalFrame != null) {
            return GuiUtils.getWindow(getComponent());
        } else {
            return null;
        }

    }

    /**
     * wrapper method
     */
    public void pack() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.pack();
        } else if (internalFrame != null) {
            internalFrame.pack();
        }
    }

    /**
     * wrapper method
     *
     * @param operation operation
     */
    public void setDefaultCloseOperation(int operation) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setDefaultCloseOperation(operation);
        } else if (internalFrame != null) {
            internalFrame.setDefaultCloseOperation(operation);
        }
    }





    /**
     * wrapper method
     *
     * @param l listener
     */
    public void addWindowListener(final WindowListener l) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            frameListeners.add(l);
            theFrame.addWindowListener(l);
        } else if (internalFrame != null) {
            if (dummyWindow == null) {
                dummyWindow = new JFrame();
            }
            //Make a bridge here
            InternalFrameListener listener = new InternalFrameListener() {
                public void internalFrameActivated(InternalFrameEvent e) {
                    l.windowActivated(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameClosed(InternalFrameEvent e) {
                    l.windowClosed(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameClosing(InternalFrameEvent e) {
                    l.windowClosing(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameDeactivated(InternalFrameEvent e) {
                    l.windowDeactivated(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameDeiconified(InternalFrameEvent e) {
                    l.windowDeiconified(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameIconified(InternalFrameEvent e) {
                    l.windowIconified(new WindowEvent(dummyWindow, 0));
                }

                public void internalFrameOpened(InternalFrameEvent e) {
                    l.windowOpened(new WindowEvent(dummyWindow, 0));
                }
            };
            internalFrame.addInternalFrameListener(listener);
            listeners.put(l, listener);

        }
    }


    /**
     * wrapper method
     *
     * @param l _more_
     */
    public void removeWindowListener(WindowListener l) {
        //TODO
        JFrame theFrame = frame;
        if ((theFrame != null) && (frameListeners != null)) {
            theFrame.removeWindowListener(l);
            frameListeners.remove(l);
        } else if ((listeners != null) && (internalFrame != null)) {
            InternalFrameListener listener =
                (InternalFrameListener) listeners.get(l);
            if (listener != null) {
                listeners.remove(l);
                internalFrame.removeInternalFrameListener(listener);
            }

        }
    }

    /**
     * wrapper method
     *
     * @param menuBar _more_
     */
    public void setJMenuBar(JMenuBar menuBar) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setJMenuBar(menuBar);
        } else if (internalFrame != null) {
            internalFrame.setJMenuBar(menuBar);
        }
    }

    /**
     * wrapper method
     *
     * @return _more_
     */
    public Rectangle getBounds() {
        return getComponent().getBounds();
    }

    /**
     * wrapper method
     *
     * @param bounds _more_
     */
    public void setBounds(Rectangle bounds) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            if (bounds != null) {
                GuiUtils.positionAndFitToScreen(theFrame, bounds);
            }
        } else if (internalFrame != null) {
            internalFrame.setBounds(bounds);
        }
    }

    /**
     * wrapper method
     *
     * @param icon _more_
     */
    public void setIconImage(Image icon) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setIconImage(icon);
        } else if (internalFrame != null) {
            //            internalFrame.setFrameIcon(new ImageIcon(icon));
        }
    }

    /**
     * wrapper method
     */
    public void toFront() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            GuiUtils.toFront(theFrame);
        } else if (internalFrame != null) {
            GuiUtils.toFront(getWindow());
            internalFrame.toFront();
        }
    }

    /**
     * Return the component. Either the frame or the internalFrame
     *
     * @return The component
     */
    public Component getComponent() {
        if (frame != null) {
            return frame;
        } else if (internalFrame != null) {
            return internalFrame;
        } else {
            return new JPanel();
        }
    }

    /**
     * wrapper method
     *
     * @return is visible
     */
    public boolean isVisible() {
        return getComponent().isVisible();
    }


    /**
     * wrapper method
     *
     * @return is showing
     */
    public boolean isShowing() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            return theFrame.isShowing();
        } else if (internalFrame != null) {
            return getWindow().isShowing() && !internalFrame.isIcon();
        } else {
            return false;
        }
    }

    /**
     * wrapper method
     *
     * @return location
     */
    public Point getLocation() {
        return getComponent().getLocation();
    }


    /**
     * wrapper method
     *
     * @param x x
     * @param y y
     */
    public void setLocation(int x, int y) {
        JFrame theFrame = frame;
        if (theFrame != null) {
            theFrame.setLocation(x, y);
        } else if (internalFrame != null) {
            //TODO?
        }
    }


    /**
     * wrapper method
     *
     * @param size size_
     */
    public void setSize(Dimension size) {
        getComponent().setSize(size);
    }


    /**
     * wrapper method
     *
     * @return size
     */
    public Dimension getSize() {
        return getComponent().getSize();
    }

    /**
     * wrapper method
     *
     * @return state
     */
    public int getState() {
        JFrame theFrame = frame;
        if (theFrame != null) {
            return theFrame.getState();
        } else if (internalFrame != null) {
            if (internalFrame.isIcon()) {
                return Frame.ICONIFIED;
            }
        }
        return Frame.NORMAL;
    }


}

