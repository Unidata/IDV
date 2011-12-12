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



package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.collab.Sharable;
import ucar.unidata.idv.ui.CursorReadoutWindow;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplayCursorReadout;
import ucar.unidata.view.geoloc.NavigatedDisplayToolBar;
import ucar.unidata.view.geoloc.VertScaleDialog;
import ucar.unidata.view.geoloc.VertScaleInfo;
import ucar.unidata.view.geoloc.ViewpointControl;

import ucar.visad.display.DisplayMaster;

import visad.ControlEvent;
import visad.CoordinateSystem;
import visad.DisplayEvent;
import visad.MouseHelper;
import visad.Unit;
import visad.VisADException;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author IDV development team
 */
public abstract class NavigatedViewManager extends ViewManager {

    /** Action command to  pan down */
    public static final String CMD_NAV_DOWN = "cmd.nav.down";

    /** Action command to  reset zoom and pan */
    public static final String CMD_NAV_HOME = "cmd.nav.home";

    /** Action command to  pan left */
    public static final String CMD_NAV_LEFT = "cmd.nav.left";

    /** Action command to  pan right */
    public static final String CMD_NAV_RIGHT = "cmd.nav.right";

    /** Action command to  rotate */
    public static final String CMD_NAV_ROTATELEFT = "cmd.nav.rotateleft";

    /** Action command to  rotate */
    public static final String CMD_NAV_ROTATERIGHT = "cmd.nav.rotateright";

    /** Action command to  pan down */
    public static final String CMD_NAV_SMALLDOWN = "cmd.nav.small.down";

    /** Action command to  pan left */
    public static final String CMD_NAV_SMALLLEFT = "cmd.nav.small.left";

    /** Action command to  pan right */
    public static final String CMD_NAV_SMALLRIGHT = "cmd.nav.small.right";

    /** Action command to  rotate */
    public static final String CMD_NAV_SMALLROTATELEFT = "cmd.nav.small.rotateleft";

    /** Action command to  rotate */
    public static final String CMD_NAV_SMALLROTATERIGHT = "cmd.nav.small.rotateright";

    /** Action command to  tilt */
    public static final String CMD_NAV_SMALLTILTDOWN = "cmd.nav.small.tiltdown";

    /** Action command to  tilt */
    public static final String CMD_NAV_SMALLTILTUP = "cmd.nav.small.tiltup";

    /** Action command to  pan up */
    public static final String CMD_NAV_SMALLUP = "cmd.nav.small.up";

    /** Action command to  zoom in */
    public static final String CMD_NAV_SMALLZOOMIN = "cmd.nav.small.zoomin";

    /** Action command to zoom out */
    public static final String CMD_NAV_SMALLZOOMOUT = "cmd.nav.small.zoomout";

    /** Action command to  pan up */
    public static final String CMD_NAV_UP = "cmd.nav.up";

    /** Action command to  zoom in */
    public static final String CMD_NAV_ZOOMIN = "cmd.nav.zoomin";

    /** Action command to zoom out */
    public static final String CMD_NAV_ZOOMOUT = "cmd.nav.zoomout";

    /** Vertical range property */
    public static final String PROP_VERTICALRANGE = "idv.viewmanager.verticalrange";

    /** Defines the viewpoint matrix when sharing state */
    public static final String SHARE_MATRIX = "MapViewManager.SHARE_MATRIX";

    /** How far do we zoom on a zoom in or out */
    public static final double ZOOM_FACTOR = NavigatedDisplayToolBar.ZOOM_FACTOR;

    /** How far do we zoom on a zoom in or out */
    public static final double TRANSLATE_FACTOR = NavigatedDisplayToolBar.TRANSLATE_FACTOR;

    /** Keep this around to know when to share viewpoint state */
    private double[] lastSharedMatrix = null;

    /** time of the mouse moved */
    private long mouseMovedTime = -1;

    /** time of the mouse pressed */
    private long mousePressedTime = -1;

    /** The tmpVertRangeUnit? */
    private Unit tmpVertRangeUnit = null;

    /** cursor readout window */
    protected CursorReadoutWindow cursorReadoutWindow;

    /** last vertical range */
    private double[] lastVerticalRange;

    /** last vertical unit */
    private Unit lastVerticalRangeUnit;

    /** start point */
    private Point mouseStartPoint;

    /** Used to show the cursor readout */
    private NavigatedDisplayCursorReadout readout;

    /** start matrix */
    private double[] startMoveMatrix;

    /**
     *  For unpersistence
     */
    private double[] tmpVerticalRange;

    /** vert scale widget */
    private VertScaleDialog vertScaleWidget;

    /** User to control the viewpoint */
    private ViewpointControl viewpointControl;

    /**
     *  Default constructor
     */
    public NavigatedViewManager() {}

    /**
     * Construct a <code>NavigatedViewManager</code> from an IDV
     *
     * @param viewContext Really the IDV
     */
    public NavigatedViewManager(ViewContext viewContext) {
        super(viewContext);
    }

    /**
     * Construct a <code>NavigatedViewManager</code> with the specified params
     * @param viewContext   context in which this MVM exists
     * @param desc   <code>ViewDescriptor</code>
     * @param properties   semicolon separated list of properties (can be null)
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public NavigatedViewManager(ViewContext viewContext, ViewDescriptor desc, String properties)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties);
    }

    /**
     * Initialize the default vertical range
     */
    private void initVerticalRange() {
        String typeName         = getTypeName().toLowerCase();
        String verticalRangeStr = getIdv().getProperty("idv.viewmanager." + typeName + ".verticalrange", (String) null);

        if (verticalRangeStr != null) {
            List toks = StringUtil.split(verticalRangeStr, ",", true, true);

            if (toks.size() >= 2) {
                tmpVerticalRange = new double[] { new Double(toks.get(0).toString()).doubleValue(),
                                                  new Double(toks.get(1).toString()).doubleValue() };

                if (toks.size() == 3) {
                    try {
                        tmpVertRangeUnit = ucar.visad.Util.parseUnit(toks.get(2).toString());
                    } catch (Exception exc) {}
                }
            }
        }
    }

    /**
     * Get the NavigatedDisplay associated with this ViewManager
     *
     * @return the NavigatedDisplay associated with this ViewManager
     */
    public NavigatedDisplay getNavigatedDisplay() {
        return (NavigatedDisplay) getMaster();
    }

    /**
     * Set the {@link ucar.visad.display.DisplayMaster}
     *
     * @param master The display master
     */
    protected void setDisplayMaster(DisplayMaster master) {
        super.setDisplayMaster(master);

        try {
            NavigatedDisplay navDisplay = (NavigatedDisplay) master;

            if (tmpVertRangeUnit != null) {
                navDisplay.setVerticalRangeUnit(tmpVertRangeUnit);
            }

            if (tmpVerticalRange != null) {
                navDisplay.setVerticalRange(tmpVerticalRange[0], tmpVerticalRange[1]);
            }

            navDisplay.setCursorStringOn(false);
        } catch (Exception e) {
            logException("setDisplayMaster", e);
        }
    }

    /**
     * Initialize this object.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void init() throws VisADException, RemoteException {
        if (getHaveInitialized()) {
            return;
        }

        super.init();
        initVerticalRange();

        if (isInteractive()) {
            if (readout == null) {
                readout = new NavigatedDisplayCursorReadout(getNavigatedDisplay(), null) {
                    protected JLabel getValueDisplay() {
                        JLabel label = super.getValueDisplay();

                        if (label == null) {
                            IdvWindow window = IdvWindow.findWindow(fullContents);

                            if (window != null) {
                                label = ((IdvWindow) window).getMsgLabel();
                                setValueDisplay(label);
                            }
                        }

                        return label;
                    }
                };
            }

            readout.setActive(getShowCursor());

            IdvWindow myWindow = IdvWindow.findWindow(fullContents);

            initReadout(myWindow);
        }
    }

    /**
     * Initialize this object's state with the state from that.
     *
     * @param that The other obejct to get state from
     * @param ignoreWindow If true then don't set the window size and location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void initWithInner(ViewManager that, boolean ignoreWindow) throws VisADException, RemoteException {
        if (!(that instanceof NavigatedViewManager)) {
            return;
        }

        super.initWithInner(that, ignoreWindow);

        NavigatedViewManager nvm = (NavigatedViewManager) that;

        if (this != that) {
            Unit verticalRangeUnit = nvm.getVerticalRangeUnit();

            if (verticalRangeUnit != null) {
                setVerticalRangeUnit(verticalRangeUnit);
            }

            double[] verticalRange = nvm.getVerticalRange();

            if (verticalRange != null) {
                setVerticalRange(verticalRange);
            }
        }
    }

    /**
     * Initialize the Readout
     *
     * @param w window for readout
     */
    private void initReadout(IdvWindow w) {
        if ((w != null) && (readout != null)) {

            // GuiUtils.setFixedWidthFont(w.getMsgLabel());
            readout.setValueDisplay(w.getMsgLabel());
            setReadoutFormat();
        }
    }

    /**
     * Apply properties.  Override super class to set class specific props.
     *
     * @return true if successful
     */
    public boolean applyProperties() {
        if (!super.applyProperties()) {
            return false;
        }

        return vertScaleWidget.doApply();
    }

    /**
     * Add components to the properties dialog
     *
     * @param tabbedPane the tabbed pane to add to
     */
    protected void addPropertiesComponents(JTabbedPane tabbedPane) {
        super.addPropertiesComponents(tabbedPane);
        vertScaleWidget = getViewpointControl().getVerticalScaleWidget();
//        tabbedPane.add("Coordinate Scale", GuiUtils.topLeft(vertScaleWidget));
        tabbedPane.add("Vertical Scale", GuiUtils.topLeft(vertScaleWidget));
    }

    /**
     * Initialize the toolbars for the GUI
     */
    protected void initToolBars() {
        addToolBar(doMakeZoomPanToolBar(JToolBar.VERTICAL), "zoompan", "Zoom/Pan toolbar");

        JComponent backButton    = getCommandManager().getBackButton();
        JComponent forwardButton = getCommandManager().getForwardButton();

        backButton.setToolTipText(
            "<html>Undo the viewpoint/projection change<br>Shift-click to go 10 steps back</html>");
        forwardButton.setToolTipText(
            "<html>Redo the viewpoint/projection change<br>Shift-click to go 10 steps forward</html>");

        JToolBar undoToolbar = new JToolBar(JToolBar.VERTICAL);

        undoToolbar.setFloatable(getToolbarsFloatable());
        undoToolbar.add(backButton);
        undoToolbar.add(forwardButton);
        addToolBar(GuiUtils.top(undoToolbar), "undoredo", "Undo/Redo toolbar");

        // JPanel undoPanel = GuiUtils.inset(GuiUtils.vbox(backButton,
        // forwardButton), new Insets(10, 0, 0, 0));
        // addToolBar(undoPanel, "undoredo", "Undo/Redo toolbar");
    }

    /**
     * Has a viewpoint control
     *
     * @return true if it does.
     */
    protected boolean hasViewpointControl() {
        return viewpointControl != null;
    }

    /**
     * Get the viewpoint control for this view manager.
     *
     * @return the ViewpointControl
     */
    protected ViewpointControl getViewpointControl() {
        if (viewpointControl == null) {
            ViewpointControl tmp = new ViewpointControl(getNavigatedDisplay()) {
                public void changePerspectiveView(boolean v) {
                    if (viewpointControl == null) {
                        return;
                    }

                    super.changePerspectiveView(v);
                    perspectiveViewChanged(v);
                }
                protected void applyVerticalScale(VertScaleInfo transfer) throws Exception {
                    if (viewpointControl == null) {
                        return;
                    }

                    super.applyVerticalScale(transfer);
                    verticalScaleChanged();
                }
            };

            // System.err.println("making viewpoint control " +getPerspectiveView());
            tmp.setPerspectiveView(getPerspectiveView());
            viewpointControl = tmp;
        }

        return viewpointControl;
    }

    /**
     * Handle a perspective view change
     *
     * @param v  true to set to perspective view
     */
    protected void perspectiveViewChanged(boolean v) {
        updateDisplayList();
        updateAnnotations();
    }

    /**
     * handle a vertical scale change
     */
    protected void verticalScaleChanged() {}

    /**
     * Cleanup when destroying this object.
     */
    public void destroy() {
        viewpointControl = null;

        if (readout != null) {
            readout.destroy();
        }

        readout = null;
        super.destroy();
    }

    /**
     * Handle the receipt of shared data
     *
     * @param from Who is it from
     * @param dataId What is it
     * @param data Here it is
     */
    public void receiveShareData(Sharable from, Object dataId, Object[] data) {
        if (!getInitDone()) {
            return;
        }

        if ((from != null) &&!from.getClass().equals(getClass())) {
            return;
        }

        if (dataId.equals(SHARE_MATRIX)) {
            try {
                lastSharedMatrix = (double[]) data[0];
                setDisplayMatrix(lastSharedMatrix);
                matrixChanged();
            } catch (Exception e) {
                logException("setDisplayMatrix", e);
            }

            return;
        }

        super.receiveShareData(from, dataId, data);
    }

    /**
     * Respond to <code>ControlEvent</code>s.
     *
     * @param e  <code>ControlEvent</code> to respond to
     */
    protected void handleControlChanged(ControlEvent e) {
        checkHistoryMatrix();

        NavigatedDisplay navDisplay = getNavigatedDisplay();

        if ((lastVerticalRangeUnit != null) && (lastVerticalRange != null)) {
            if (!(Misc.equals(lastVerticalRangeUnit, navDisplay.getVerticalRangeUnit())
                    && Arrays.equals(lastVerticalRange, navDisplay.getVerticalRange()))) {
                verticalRangeChanged();
            }
        }

        lastVerticalRangeUnit = navDisplay.getVerticalRangeUnit();
        lastVerticalRange     = navDisplay.getVerticalRange();
        super.handleControlChanged(e);
    }

    /**
     *  An implementation of the the DisplayListener interface.
     * This method turns on/off the wait cursor when it gets a
     * WAIT_ON or WAIT_OFF event. It also, when it receives a
     * FRAME_DONE event for the fist time,  calls <code>firstFrameDone</code>
     * on the {@link DisplayControl}s
     *
     * @param de The <code>DisplayEvent</code>
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void displayChanged(DisplayEvent de) throws VisADException, RemoteException {
        if (getIsDestroyed()) {
            return;
        }

        int        eventId    = de.getId();
        InputEvent inputEvent = de.getInputEvent();

        if ((inputEvent instanceof MouseEvent) && (eventId == DisplayEvent.MOUSE_PRESSED)) {
            mousePressedTime = System.currentTimeMillis();
            getViewpointControl().setAutoRotate(false);
            startMoveMatrix = getProjectionControl().getMatrix();

            MouseEvent mouseEvent = (MouseEvent) inputEvent;

            mouseStartPoint = new Point(mouseEvent.getX(), mouseEvent.getY());

            if ((mouseEvent.getClickCount() > 1) && mouseEvent.isShiftDown()) {
                NavigatedDisplay navDisplay = getNavigatedDisplay();
                double[]         box        = navDisplay.getSpatialCoordinatesFromScreen(mouseEvent.getX(),
                                                  mouseEvent.getY());

                navDisplay.center(navDisplay.getEarthLocation(box));
            }
        }

        if ((eventId == DisplayEvent.MOUSE_PRESSED) || (eventId == DisplayEvent.MOUSE_DRAGGED)) {
            mouseMovedTime = System.currentTimeMillis();

            MouseEvent mouseEvent  = (MouseEvent) inputEvent;
            int[][][]  functionMap = getMaster().getMouseFunctionMap();

            if (functionMap != null) {
                int ctrlIdx  = (GuiUtils.isControlKey(mouseEvent)
                                ? 1
                                : 0);
                int shiftIdx = (mouseEvent.isShiftDown()
                                ? 1
                                : 0);
                int mouseIdx = ((SwingUtilities.isLeftMouseButton(mouseEvent)
                                 &&!SwingUtilities.isRightMouseButton(mouseEvent))
                                ? 0
                                : (SwingUtilities.isMiddleMouseButton(mouseEvent)
                                   ? 1
                                   : ((SwingUtilities.isRightMouseButton(mouseEvent)
                                       &&!SwingUtilities.isLeftMouseButton(mouseEvent))
                                      ? 2
                                      : 1)));
                int function = functionMap[mouseIdx][ctrlIdx][shiftIdx];

                if (function == MouseHelper.CURSOR_TRANSLATE) {
                    if (cursorReadoutWindow == null) {
                        cursorReadoutWindow = new CursorReadoutWindow(this);
                    }

                    cursorReadoutWindow.handleMousePressedOrDragged(mouseEvent);
                }
            }
        } else if (eventId == DisplayEvent.MOUSE_RELEASED) {
            MouseEvent mouseEvent = (MouseEvent) inputEvent;

            if (cursorReadoutWindow != null) {
                cursorReadoutWindow.handleMouseReleased(mouseEvent);
                cursorReadoutWindow = null;
            }

            Point mouseStart = mouseStartPoint;

            if (mouseStart != null) {
                Point  toPoint   = new Point(mouseEvent.getX(), mouseEvent.getY());
                double distance  = GuiUtils.distance(mouseStart.x, mouseStart.y, toPoint.x, toPoint.y);
                long   deltaTime = System.currentTimeMillis() - mouseMovedTime;

                if (System.currentTimeMillis() - mousePressedTime > 0) {
                    double speed = distance / (System.currentTimeMillis() - mousePressedTime);

                    if ((distance > 50) && (deltaTime < 200) && (speed > 0.5)) {
                        double[] endMatrix = getProjectionControl().getMatrix();

                        mouseFlicked(mouseStart, toPoint, startMoveMatrix, endMatrix, speed);
                    }
                }
            }

            mouseMovedTime = -1;
        }

        super.displayChanged(de);
    }

    /**
     * Handle a mouse flicked
     *
     * @param startPoint   the start point
     * @param endPoint     the end point
     * @param startMatrix  the start matrix
     * @param endMatrix    the end matrix
     * @param speed        how fast to move
     */
    protected void mouseFlicked(Point startPoint, Point endPoint, double[] startMatrix, double[] endMatrix,
                                double speed) {}

    /**
     * Handle an animation time change
     */
    protected void animationTimeChanged() {
        super.animationTimeChanged();

        if (cursorReadoutWindow != null) {
            cursorReadoutWindow.updateReadout();
        }
    }

    /**
     * Handle the change to the matrix
     */
    protected void matrixChanged() {}

    /**
     * Handle a change to the vertical range
     */
    protected void verticalRangeChanged() {}

    /**
     * Set the format for the curso readout
     */
    protected void setReadoutFormat() {
        if (readout != null) {
            String format = (String) getStore().get(IdvConstants.PREF_LATLON_FORMAT);

            if (format != null) {
                readout.setFormatPattern(format);
            }
        }
    }

    /**
     * Apply preferences
     */
    public void applyPreferences() {
        super.applyPreferences();
        setReadoutFormat();
    }

    /**
     * The BooleanProperty identified byt he given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handeling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value) throws Exception {
        if (id.equals(PREF_SHOWCURSOR)) {
            if (readout != null) {
                readout.setActive(value);
            }
        } else if (id.equals(PREF_3DCLIP)) {
            if (hasDisplayMaster()) {
                getNavigatedDisplay().enableClipping(value);
            }
        } else {
            super.handleBooleanPropertyChange(id, value);
        }
    }

    /**
     * Get the intial BooleanProperty-s
     *
     * @param props list to add them to.
     */
    protected void getInitialBooleanProperties(List props) {
        super.getInitialBooleanProperties(props);
        props.add(new BooleanProperty(PREF_3DCLIP, "Clip View At Box", "", false));
        props.add(new BooleanProperty(PREF_SHOWCURSOR, "Show Cursor Readout", "", true));
        props.add(new BooleanProperty(PREF_SHOWDISPLAYLIST, "Show Display List", "", false));
    }

    /**
     * Check the matrix history.
     */
    protected void checkHistoryMatrix() {
        try {
            if (!getInitDone()) {
                return;
            }

            double[] newMatrix = getProjectionControl().getMatrix();

            if ((lastSharedMatrix == null) ||!Arrays.equals(newMatrix, lastSharedMatrix)) {
                matrixChanged();

                if ((lastSharedMatrix != null) &&!getCommandManager().getApplyingCommand()) {
                    if (!mouseDown) {
                        addCommand(new MatrixCommand(this, lastSharedMatrix, getMaster().getProjectionMatrix()));
                    }
                }

                lastSharedMatrix = newMatrix;
                doShare(SHARE_MATRIX, newMatrix);
            }
        } catch (Exception exc) {
            logException("controlChanged", exc);
        }
    }

    /**
     * Set the window that this ViewManager is shown in.
     * This adds this object as a <code>WindowListener</code>
     * and sets the bounds of the window if the windowBounds
     * is non-null.
     *
     * @param w The window
     */
    public void setWindow(IdvWindow w) {
        super.setWindow(w);
        initReadout(w);
    }

    /**
     * Creates the Viewpoint Toolbar in the specified orientation.
     * @param orientation orientation of the toolbar
     *                    (JToolBar.VERTICAL or JToolBar.HORIZONTAL)
     * @return the toolbar component
     */
    public Component doMakeViewPointToolBar(int orientation) {
        JToolBar toolbar = getViewpointControl().getToolBar(getToolbarsFloatable());

        toolbar.setOrientation(orientation);

        return GuiUtils.top(toolbar);
    }

    /**
     * Creates the Zoom/Pan Toolbar in the specified orientation.
     * @param orientation orientation of the toolbar
     *                    (JToolBar.VERTICAL or JToolBar.HORIZONTAL)
     * @return the toolbar component
     */
    protected Component doMakeZoomPanToolBar(int orientation) {
        JToolBar toolbar = getNavigatedDisplay().getNavigationToolBar(orientation, getToolbarsFloatable());

        return GuiUtils.top(toolbar);
    }

    /**
     * Required interface for ActionEvents, to implement ActionListener
     * for the UI objects such as JButton-s and MenuItem-s
     *
     * @param event an ActionEvent
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();

        if (cmd.equals(CMD_NAV_ZOOMIN)) {
            getNavigatedDisplay().zoom(ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_ROTATELEFT)) {
            getNavigatedDisplay().rotateZ(-5.0);
        } else if (cmd.equals(CMD_NAV_ROTATERIGHT)) {
            getNavigatedDisplay().rotateZ(5.0);
        } else if (cmd.equals(CMD_NAV_ZOOMOUT)) {
            getNavigatedDisplay().zoom(1.0 / (double) ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_HOME)) {
            try {
                getNavigatedDisplay().resetProjection();
            } catch (Exception exc) {}
        } else if (cmd.equals(CMD_NAV_RIGHT)) {
            getNavigatedDisplay().translate(-TRANSLATE_FACTOR, 0.0);
        } else if (cmd.equals(CMD_NAV_LEFT)) {
            getNavigatedDisplay().translate(TRANSLATE_FACTOR, 0.0);
        } else if (cmd.equals(CMD_NAV_UP)) {
            getNavigatedDisplay().translate(0.0, -TRANSLATE_FACTOR);
        } else if (cmd.equals(CMD_NAV_DOWN)) {
            getNavigatedDisplay().translate(0.0, TRANSLATE_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLZOOMIN)) {
            getNavigatedDisplay().zoom(ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLZOOMOUT)) {
            getNavigatedDisplay().zoom(1.0 / ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLROTATELEFT)) {
            getNavigatedDisplay().rotateZ(-2.0);
        } else if (cmd.equals(CMD_NAV_SMALLROTATERIGHT)) {
            getNavigatedDisplay().rotateZ(2.0);
        } else if (cmd.equals(CMD_NAV_SMALLTILTUP)) {
            getNavigatedDisplay().rotateX(-2.0);
        } else if (cmd.equals(CMD_NAV_SMALLTILTDOWN)) {
            getNavigatedDisplay().rotateX(2.0);
        } else if (cmd.equals(CMD_NAV_SMALLRIGHT)) {
            getNavigatedDisplay().translate(-0.02, 0.0);
        } else if (cmd.equals(CMD_NAV_SMALLLEFT)) {
            getNavigatedDisplay().translate(0.02, 0.0);
        } else if (cmd.equals(CMD_NAV_SMALLUP)) {
            getNavigatedDisplay().translate(0.0, -0.02);
        } else if (cmd.equals(CMD_NAV_SMALLDOWN)) {
            getNavigatedDisplay().translate(0.0, 0.02);
        } else {
            super.actionPerformed(event);

            return;
        }

        checkHistoryMatrix();
    }

    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = super.makeShowMenu();

        createCBMI(showMenu, PREF_3DCLIP);
        createCBMI(showMenu, PREF_SHOWCURSOR);

        return showMenu;
    }

    /**
     * Set the  show cursor flag
     *
     * @param value The value
     */
    public void setShowCursor(boolean value) {
        setBp(PREF_SHOWCURSOR, value);
    }

    /**
     * Get  the show cursor readout flag
     * @return The flag value
     */
    public boolean getShowCursor() {
        return getBp(PREF_SHOWCURSOR);
    }

    /**
     * Set the show vertical scale flag
     *
     * @param value The value
     */
    public void setLabelsVisible(boolean value) {
        setBp(PREF_SHOWSCALES, value);
    }

    /**
     * Get the show vertical scale flag
     * @return The flag value
     */
    public boolean getLabelsVisible() {
        return getBp(PREF_SHOWSCALES);
    }

    /**
     * Set the show vertical scale flag
     *
     * @param value The value
     */
    public void setTransectLabelsVisible(boolean value) {
        setBp(PREF_SHOWTRANSECTSCALES, value);
    }

    /**
     * Get the show vertical scale flag
     * @return The flag value
     */
    public boolean getTransectLabelsVisible() {
        return getBp(PREF_SHOWTRANSECTSCALES);
    }

    /**
     * Set the  clipping  flag
     *
     * @param value The value
     */
    public void setClipping(boolean value) {
        setBp(PREF_3DCLIP, value);
    }

    /**
     * Get  the 3d clipping  flag
     * @return The flag value
     */
    public boolean getClipping() {
        return getBp(PREF_3DCLIP);
    }

    /**
     * Set the <code>Unit</code> used for the vertical range.
     *
     * @param u  new unit
     */
    public void setVerticalRangeUnit(Unit u) {
        tmpVertRangeUnit = u;

        if (u == null) {
            return;
        }

        if (!hasDisplayMaster()) {
            return;
        }

        try {
            getNavigatedDisplay().setVerticalRangeUnit(u);
        } catch (Exception exc) {}
    }

    /**
     * Get the <code>Unit</code> used for the vertical range.
     *
     * @return unit of vertical range values
     */
    public Unit getVerticalRangeUnit() {
        if (!hasDisplayMaster()) {
            return tmpVertRangeUnit;
        }

        return getNavigatedDisplay().getVerticalRangeUnit();
    }

    /**
     * Get the min/max used for the vertical range.
     *
     * @return array of vertical range values (double[] {min, max})
     */
    public double[] getVerticalRange() {
        if (!hasDisplayMaster()) {
            return tmpVerticalRange;
        }

        return getNavigatedDisplay().getVerticalRange();
    }

    /**
     * Set the min/max used for the vertical range.
     *
     * @param r array of vertical range values (double[] {min, max})
     */
    public void setVerticalRange(double[] r) {
        tmpVerticalRange = r;

        if (r == null) {
            return;
        }

        if (!hasDisplayMaster()) {
            return;
        }

        try {
            getNavigatedDisplay().setVerticalRange(r[0], r[1]);
        } catch (Exception exc) {}
    }

    /**
     * Set the vertical range unit from the preference
     *
     * @param nd navigated display to set the unit on
     *
     * @throws RemoteException  problem with remote display
     * @throws VisADException problem with local display
     */
    protected void setVerticalRangeUnitPreference(NavigatedDisplay nd) throws VisADException, RemoteException {
        Unit u = null;

        try {
            u = ucar.visad.Util.parseUnit(getIdv().getObjectStore().get(IdvConstants.PREF_VERTICALUNIT, "m"));
        } catch (Exception exc) {
            u = null;
        }

        if (u != null) {
            double[] range       = nd.getVerticalRange();
            Unit     defaultUnit = nd.getVerticalRangeUnit();

            if (!u.equals(defaultUnit) && Unit.canConvert(u, defaultUnit)) {
                range = u.toThis(range, defaultUnit);
                nd.setVerticalRangeUnit(u);
                nd.setVerticalRange(range[0], range[1]);
            }
        }
    }

    /**
     * Get the display side coordinate system, subclasses should implement
     * if there is one.
     * @return  CoordinateSystem or null
     */
    public CoordinateSystem getDisplayCoordinateSystem() {
        return getNavigatedDisplay().getDisplayCoordinateSystem();
    }
}
