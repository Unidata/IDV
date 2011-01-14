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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.*;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.IndependentWindow;
import ucar.unidata.ui.MultiFrame;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.Removable;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * The window class used for the IDV. Really need to break this out into
 * a window manager. This listens for window close operations and manages
 * the bottom message bar/memory monitor/spinning wait icon.
 *
 * @author IDV development team
 */
public class IdvWindow extends MultiFrame {

    /** The chooser components in this window */
    public static final String GROUP_CHOOSERS = "choosers";

    /** The toolbar components in this window */
    public static final String GROUP_TOOLBARS = "toolbars";

    /** Keep around default window positions */
    private static int lastX = 10;

    /** Keep around default window positions */
    private static int lastY = 10;

    /** Are we currently in wait state */
    private static boolean waitState = false;


    /** _more_ */
    private List<Removable> removables = new ArrayList<Removable>();

    /** _more_ */
    private boolean hasBeenDisposed = false;

    /** The window contents */
    JComponent contents;

    /** The wait icon_ */
    private static ImageIcon waitIcon;

    /** The icon to use on a mouse over (i.e., the beer bottle) */
    private static ImageIcon waitOverIcon;

    /** The normal icon */
    private static ImageIcon normalIcon;


    /** List of all active IdvWindow objects */
    private static ArrayList allWindows = new ArrayList();

    /**
     * List of the main windows. The main windows are the ones we look at when
     * we  exit when there are no more windows up.
     */
    private static ArrayList mainWindows = new ArrayList();

    /** Keep track of the last active window */
    private static IdvWindow lastActiveWindow;


    /** The unique id for this window */
    private String uniqueId;

    /** If we were created with an xmlui skin then this is it */
    private IdvXmlUi xmlUI;

    /** Path to the skin xml file. May be null */
    private String skinPath;

    /** The IDV */
    private IntegratedDataViewer idv;


    /** A mapping of named component to component */
    private Hashtable components = new Hashtable();

    /** The view managers that are displayed in this window */
    private List viewManagers = new ArrayList();


    /** Am I spinning */
    private boolean waiting = false;

    /** Is this a main window */
    private boolean isAMainWindow = false;


    /** Window type */
    private String type = "";

    /** The groups within this window */
    private Hashtable groups = new Hashtable();

    /** _more_ */
    private Hashtable persistentComponents = new Hashtable();

    /**
     * Create the window
     *
     * @param title The window title
     * @param theIdv The IDV
     *
     */
    public IdvWindow(String title, IntegratedDataViewer theIdv) {
        this(title, theIdv, false);
    }


    /**
     * Create the window
     *
     * @param title The window title
     * @param theIdv The IDV
     * @param isAMainWindow Is this a main window
     */
    public IdvWindow(String title, IntegratedDataViewer theIdv,
                     boolean isAMainWindow) {
        super(title);
        if (uniqueId == null) {
            uniqueId = Misc.getUniqueId();
        }
        allWindows.add(this);
        if (isAMainWindow) {
            //            Misc.printStack("new window", 5,null);
            mainWindows.add(this);
        }
        this.idv           = theIdv;
        this.isAMainWindow = isAMainWindow;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        final WindowAdapter[] wa = { null };
        addWindowListener(wa[0] = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (doClose()) {
                    removeWindowListener(wa[0]);
                    lastActiveWindow = null;
                }
            }
            public void windowActivated(WindowEvent e) {
                lastActiveWindow = IdvWindow.this;
            }
        });
        LogUtil.registerWindow(getWindow());
        if (lastX > 500) {
            lastX = 10;
            lastY = 10;
        }
        setLocation(lastX, lastY);
        lastX += 50;
        //        lastY += 50;
    }


    /**
     * Get the current active window
     *
     * @return active window
     */
    public static IdvWindow getActiveWindow() {
        return lastActiveWindow;

    }



    /**
     * Set the global waitState flag
     *
     * @param waiting waiting value
     */
    public static void setWaitState(boolean waiting) {
        waitState = waiting;
    }


    /**
     * Get the global wait state
     *
     * @return waiting value
     */
    public static boolean getWaitState() {
        return waitState;
    }



    /**
     * Get the contents
     *
     * @return window contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Set the contents. Add the contents into the content pane and pack.
     *
     * @param contents window contents
     */
    public void setContents(JComponent contents) {
        this.contents = contents;
        if (contents == null) {
            return;
        }
        Container contentPane = getContentPane();
        if (contentPane != null) {
            contentPane.removeAll();
            contentPane.add(contents);
            pack();
        }
    }


    /**
     * set the bounds of the window
     *
     * @param r window bounds
     */
    public void setWindowBounds(Rectangle r) {
        //        boolean wasVisible = isVisible();
        //        setVisible(false);
        setBounds(r);
        //        setVisible(wasVisible);
    }


    /**
     * Find the IdvWindow that contains the window contents
     *
     * @param contents Contents to look for
     *
     * @return The IdvWindow that holds the contents
     */
    public static IdvWindow findWindow(Component contents) {
        if (contents == null) {
            return null;
        }
        Component parent = contents.getParent();
        while (parent != null) {
            if (parent instanceof Window) {
                for (int i = 0; i < allWindows.size(); i++) {
                    IdvWindow idvWindow = (IdvWindow) allWindows.get(i);
                    if (idvWindow.getFrame() == parent) {
                        return idvWindow;
                    }
                }
            }
            if (parent instanceof JInternalFrame) {
                for (int i = 0; i < allWindows.size(); i++) {
                    IdvWindow idvWindow = (IdvWindow) allWindows.get(i);
                    if (idvWindow.getInternalFrame() == parent) {
                        return idvWindow;
                    }
                }
            }
            parent = parent.getParent();
        }
        return null;
    }



    /**
     * Set the xmlui skin object
     *
     * @param xmlUI The xmlui skin object
     */
    public void setXmlUI(IdvXmlUi xmlUI) {
        this.xmlUI = xmlUI;
    }

    /**
     * Get the xmlui object that created the gui in this window. May be null.
     *
     * @return The xmlui.
     */
    public IdvXmlUi getXmlUI() {
        return xmlUI;
    }


    /**
     * Get the path to the xml skin.
     *
     * @return The xml skin.
     */
    public String getSkinPath() {
        return skinPath;
    }

    /**
     * Set the path to the xml skin.
     *
     * @param b The skin path
     */
    public void setSkinPath(String b) {
        skinPath = b;
    }



    /**
     * Override toString
     *
     * @return The string
     */
    public String toString() {
        return "IdvWindow:" + skinPath;
    }

    /**
     * Get the icon used to show wait state (the spinning globe)
     *
     * @return The wait icon.
     */
    public static ImageIcon getWaitIcon() {
        if (waitIcon == null) {
            return setWaitIcon("/ucar/unidata/idv/images/wait.gif");
        }
        return waitIcon;
    }

    /**
     * Get the icon used to show wait state (the spinning globe)
     *
     * @param path Set the wait icon to use
     * @return The wait icon.
     */
    public static ImageIcon setWaitIcon(String path) {
        return waitIcon = GuiUtils.getImageIcon(path);
        //      return waitIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/Loading.gif");
    }


    /**
     * Show the window if its ok.
     */
    public void show() {
        if ((idv != null) && !idv.okToShowWindows()) {
            return;
        }
        if (waitState) {
            startWait();
        }
        // deiconify if needed
        setState(Frame.NORMAL);
        super.show();


    }

    /**
     * Get the icon used to show normal state
     *
     * @return The normal icon.
     */
    public static ImageIcon getNormalIcon() {
        if (normalIcon == null) {
            Image waitImage = getWaitIcon().getImage();
            BufferedImage image = new BufferedImage(waitImage.getWidth(null),
                                      waitImage.getHeight(null),
                                      BufferedImage.TYPE_INT_ARGB);
            normalIcon = new ImageIcon(image);
        }
        return normalIcon;
    }

    /**
     * Get the icon used to show normal state
     *
     *
     * @param path The path to the normal icon to use
     * @return The normal icon.
     */
    public static ImageIcon setNormalIcon(String path) {
        //      return normalIcon = GuiUtils.getImageIcon(path);
        return getNormalIcon();
    }



    /**
     * Get the icon used when mousing over the label in wait state (the beer bottles)
     *
     * @return The mouse over wait icon.
     */
    public static ImageIcon getWaitOverIcon() {
        if (waitOverIcon == null) {
            return setWaitOverIcon("/ucar/unidata/idv/images/wait_over.gif");
        }
        return waitOverIcon;
    }

    /**
     * Get the icon used when mousing over the label in wait state (the beer bottles)
     *
     *
     * @param path The path to the wait over icon to use
     * @return The mouse over wait icon.
     */
    public static ImageIcon setWaitOverIcon(String path) {
        return waitOverIcon = GuiUtils.getImageIcon(path);
    }



    /**
     * Get the JLabel message label.
     *
     * @return The JLabel for messages
     */

    public JLabel getMsgLabel() {
        return (JLabel) getComponent(IdvUIManager.COMP_MESSAGELABEL);
    }

    /**
     *  List listeners = new ArrayList ();
     *  public void addWindowListener (WindowListener l) {
     *  super.addWindowListener (l);
     *  listeners.add (l);
     *  }
     *
     *  public void removeWindowListener (WindowListener l) {
     *  super.removeWindowListener (l);
     *  listeners.remove (l);
     *  }
     */



    /**
     * Start spinning
     */
    public void startWait() {
        waiting = true;
        setWaitIcon(getWaitIcon());
        RovingProgress progress =
            (RovingProgress) getComponent(IdvUIManager.COMP_PROGRESSBAR);
        if (progress != null) {
            progress.start();
        }
    }

    /**
     * Stop spinning
     */
    public void endWait() {
        waiting = false;
        setWaitIcon(getNormalIcon());
        RovingProgress progress =
            (RovingProgress) getComponent(IdvUIManager.COMP_PROGRESSBAR);
        if (progress != null) {
            progress.stop();
        }
    }

    /**
     * Set the icon for the wait label.
     *
     * @param icon The wait icon.
     */
    public void setWaitIcon(Icon icon) {
        JLabel waitLbl = getWaitLabel();
        if (waitLbl != null) {
            waitLbl.setIcon(icon);
            waitLbl.repaint();
        }

    }

    /**
     * Get the label that we spin
     *
     * @return The wait label
     */
    public JLabel getWaitLabel() {
        return (JLabel) getComponent(IdvUIManager.COMP_WAITLABEL);
    }


    /**
     * Close this window. If it is the last main window then ask to exit
     *
     * @return Was closed
     */
    protected boolean doClose() {
        if (isAMainWindow && mainWindows.contains(this)
                && (mainWindows.size() == 1)) {
            if ( !idv.quit()) {
                return false;
            }
        }
        dispose();
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getHasBeenDisposed() {
        return hasBeenDisposed;
    }


    /**
     * Dispose of this window.
     */
    public void dispose() {
        if (hasBeenDisposed) {
            return;
        }
        hasBeenDisposed = true;

        if (lastActiveWindow == this) {
            lastActiveWindow = null;
        }

        idv.getIdvUIManager().removeWindow(this);

        for (Removable removable : removables) {
            removable.doRemove();
        }
        removables = null;

        JMenuBar menuBar = (JMenuBar) getComponent(IdvUIManager.COMP_MENUBAR);
        if (menuBar != null) {
            GuiUtils.empty(menuBar, true);
        }

        RovingProgress progress =
            (RovingProgress) getComponent(IdvUIManager.COMP_PROGRESSBAR);
        if (progress != null) {
            progress.doRemove();
        }

        JComponent messageLogger =
            (JComponent) getComponent(IdvUIManager.COMP_MESSAGELABEL);
        if (messageLogger != null) {
            LogUtil.removeMessageLogger(messageLogger);
        }


        allWindows.remove(this);
        mainWindows.remove(this);

        List groups = getComponentGroups();
        for (int i = 0; i < groups.size(); i++) {
            ComponentGroup group = (ComponentGroup) groups.get(i);
            group.doRemove();
        }
        this.groups = null;
        destroyViewManagers();
        viewManagers         = null;

        components           = null;
        persistentComponents = null;
        contents             = null;

        if (xmlUI != null) {
            //This was commented out. Not sure why.
            xmlUI.dispose();
            xmlUI = null;
        }
        super.dispose();
    }

    /**
     * Nuke the view managers held by this window
     */
    private void destroyViewManagers() {
        List viewManagers = getViewManagers();
        if (viewManagers == null) {
            return;
        }
        //Not sure why we are returning here
        //        if (true) {
        //            return;
        //        }

        try {
            for (int i = 0; i < viewManagers.size(); i++) {
                ((ViewManager) viewManagers.get(i)).destroy();
            }
        } catch (Exception exc) {
            LogUtil.logException("Destroying window", exc);
        }
    }

    /**
     * Destroy this window. This just empties out the contents.
     */
    public void destroy() {
        destroyViewManagers();
        GuiUtils.empty(getContainer(), true);
    }


    /**
     * Return the list of main windows currently in use.
     *
     * @return List of main windows
     */
    public static List getMainWindows() {
        return mainWindows;
    }


    /**
     * The IdvWindow can hold a number of named components.
     * e.g., This could be a toolbar, etc.
     *
     * @param componentName The name or id
     * @param component The component
     */
    public void setComponent(String componentName, Object component) {
        if (components == null) {
            return;
        }
        components.put(componentName, component);
    }

    /**
     * _more_
     *
     * @param removable _more_
     */
    public void addRemovable(Removable removable) {
        removables.add(removable);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param object _more_
     */
    public void putPersistentComponent(Object key, Object object) {
        if (persistentComponents == null) {
            return;
        }
        persistentComponents.put(key, object);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getPersistentComponent(Object key) {
        if (persistentComponents == null) {
            return null;
        }
        return persistentComponents.get(key);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<IdvComponentGroup> getComponentGroups() {
        List groups = new ArrayList<IdvComponentGroup>();
        if (persistentComponents == null) {
            return groups;
        }
        Hashtable map = getPersistentComponents();
        for (Enumeration keys = map.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object obj = map.get(key);
            if (obj instanceof IdvComponentGroup) {
                groups.add((IdvComponentGroup) obj);
            }
        }
        return groups;
    }



    /**
     * An IdvWindow can hold a group of objects, identified
     * by the groupKey. We use this to store the choosers that are in
     * an window
     *
     * @param groupKey The group key. Usually a String name
     * @param comp The object (ususally a Component) to add to the group
     */
    public void addToGroup(Object groupKey, Object comp) {
        List comps = (List) groups.get(groupKey);
        if (comps == null) {
            comps = new ArrayList();
            groups.put(groupKey, comps);
        }
        if ( !comps.contains(comp)) {
            comps.add(comp);
        }
    }

    /**
     * Get the list of objects that are in the group.
     * If none found return null
     *
     * @param  groupKey The group key
     * @return List of objects in group or null
     */
    public List getGroup(Object groupKey) {
        return (List) groups.get(groupKey);
    }


    /**
     * Get the  list of components held by the xmlui
     * @return components
     */
    public List getComponents() {
        if (xmlUI != null) {
            return xmlUI.getComponents();
        }
        return null;
    }


    /**
     * The IdvWindow can hold a number of named components.
     * e.g., This could be a toolbar, etc.
     *
     * @param componentName The name or id
     * @return The component
     */
    public Object getComponent(String componentName) {
        if (components == null) {
            return null;
        }
        Object comp = components.get(componentName);
        if ((comp == null) && (xmlUI != null)) {
            comp = xmlUI.getComponent(componentName);
        }
        return comp;
    }


    /**
     * Get all of the current windows.
     *
     * @return List of IdvWindow objects
     */
    public static List getWindows() {
        return new ArrayList(allWindows);
    }


    /**
     * Does this window contain any view managers
     *
     * @return Contains view managers
     */
    public boolean hasViewManagers() {
        List viewManagers = getViewManagers();
        if (viewManagers != null) {
            return viewManagers.size() > 0;
        }
        return false;
    }


    /**
     *  Set the ViewManagers property.
     *
     *  @param value The new value for ViewManagers
     */
    public void setTheViewManagers(List value) {
        viewManagers = value;
    }

    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public void addViewManager(ViewManager viewManager) {
        if (viewManagers == null) {
            viewManagers = new ArrayList();
        }
        if ( !viewManagers.contains(viewManager)) {
            viewManagers.add(viewManager);
        }
    }

    /**
     *  Get the ViewManagers property.
     *
     *  @return The ViewManagers
     */
    public List getViewManagers() {
        List tmp = new ArrayList();
        if (viewManagers != null) {
            tmp.addAll(viewManagers);
        }

        if (persistentComponents != null) {
            for (Enumeration keys = persistentComponents.keys();
                    keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                Object obj = persistentComponents.get(key);
                if (obj instanceof IdvComponentGroup) {
                    ((IdvComponentGroup) obj).getViewManagers(tmp);
                }
            }
        }
        return tmp;
    }


    /**
     * Set the UniqueId property.
     *
     * @param value The new value for UniqueId
     */
    public void setUniqueId(String value) {
        uniqueId = value;
    }

    /**
     * Get the UniqueId property.
     *
     * @return The UniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     *  Set the IsAMainWindow property.
     *
     *  @param value The new value for IsAMainWindow
     */
    public void setIsAMainWindow(boolean value) {
        isAMainWindow = value;
    }

    /**
     *  Get the IsAMainWindow property.
     *
     *  @return The IsAMainWindow
     */
    public boolean getIsAMainWindow() {
        return isAMainWindow;
    }


    /**
     * Show the wait cursor
     */
    public void showWaitCursor() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * Show the normal cursor
     */
    public void showNormalCursor() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }


    /**
     * Set the PersistenceComponents property.
     *
     * @param value The new value for PersistenceComponents
     */
    public void setPersistentComponents(Hashtable value) {
        if (value == null) {
            persistentComponents = value;
            return;
        }
        persistentComponents = new Hashtable();
        persistentComponents.putAll(value);
    }

    /**
     * Get the PersistentComponents property.
     *
     * @return The PersistentComponents
     */
    public Hashtable getPersistentComponents() {
        return persistentComponents;
    }



}
