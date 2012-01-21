/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.geoloc;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import visad.Unit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This class has a menu and ToolBar to control the viewpoint of
 * a 3D display.
 *
 * @author IDV Development team
 * @version $Revision: 1.31 $
 */
public class ViewpointControl implements ActionListener {

    /** Action command */
    private static final String CMD_ROTATEDIALOG = "cmd.rotatedialog";

    /** Action command */
    private static final String CMD_SETBOTTOM = "cmd.setbottomview";

    /** Action command */
    private static final String CMD_SETEAST = "cmd.seteastview";

    /** Action command */
    private static final String CMD_SETEYEPOSITION = "cmd.seteyeposition";

    /** Action command */
    private static final String CMD_SETNORTH = "cmd.setnorthview";

    /** Action command */
    private static final String CMD_SETSOUTH = "cmd.setsouthview";

    /** Action command */
    private static final String CMD_SETTOP = "cmd.settopview";

    /** Action command */
    private static final String CMD_SETVERTICALSCALE = "cmd.setverticalscale";

    /** Action command */
    private static final String CMD_SETWEST = "cmd.setwestview";

    /** Icon for toolbar */
    private static final String ICON_BOTTOM =
        "/auxdata/ui/icons/Bottom16.gif";

    /** Icon for toolbar */
    private static final String ICON_EAST = "/auxdata/ui/icons/East16.gif";

    /** Icon for toolbar */
    private static final String ICON_NORTH = "/auxdata/ui/icons/North16.gif";

    /** Icon for toolbar */
    private static final String ICON_PARALLEL =
        "/auxdata/ui/icons/parallel_view.gif";

    /** Icon for toolbar */
    private static final String ICON_PERSPECTIVE =
        "/auxdata/ui/icons/perspective_view.gif";

    /** Icon for toolbar */
    private static final String ICON_SETVERTICALSCALE =
        "/auxdata/ui/icons/Vert16.gif";

    /** Icon for toolbar */
    private static final String ICON_SOUTH = "/auxdata/ui/icons/South16.gif";

    /** Icon for toolbar */
    private static final String ICON_TOP = "/auxdata/ui/icons/Top16.gif";

    /** Icon for toolbar */
    private static final String ICON_USERVIEW =
        "/auxdata/ui/icons/UserView16.gif";

    /** Icon for toolbar */
    private static final String ICON_WEST = "/auxdata/ui/icons/West16.gif";

    /** rotate down identifier */
    private static final String ROTATE_DOWN = "rotate.down";

    /** rotate left identifier */
    private static final String ROTATE_LEFT = "rotate.left";

    /** rotate right identifier */
    private static final String ROTATE_RIGHT = "rotate.right";

    /** rotate up identifier */
    private static final String ROTATE_UP = "rotate.up";

    /** rotate clockwise identifier */
    private static final String ROTATE_CLOCKWISE = "rotate.clockwise";

    /** rotate counterclockwise identifier */
    private static final String ROTATE_COUNTERCLOCKWISE =
        "rotate.counterclockwise";

    /** Commands for changing perspectives */
    private static final String[] perspectiveCmds = {
        CMD_SETTOP, CMD_SETBOTTOM, CMD_SETNORTH, CMD_SETEAST, CMD_SETSOUTH,
        CMD_SETWEST
    };

    /** Icons for changing perspectives */
    private static final String[] perspectiveIcons = {
        ICON_TOP, ICON_BOTTOM, ICON_NORTH, ICON_EAST, ICON_SOUTH, ICON_WEST
    };

    /** flag for autorotate view */
    private double eyePosition = 0.004;

    /** default view point Azimuth */
    private double vpAz = 180.0;

    /** default view point tilt */
    private double vpTilt = 45.0;

    /** flag for accepting changes from perspective widgets */

    /** flag for accepting changes from perspective widgets */
    private boolean okToAcceptChangesFromPerspectiveWidgets = true;

    /** flag for accepting changes from autorotate widgets */
    private boolean okToAcceptChangesFromAutoRotateWidgets = true;

    /** flag for autorotate view */
    private boolean autoRotate;

    /** the perspective menu item */
    private JMenuItem eyePositionMenu;

    /** flag for perspective view */
    private boolean isPerspective;

    /** menu of controls */
    private JMenu menu;

    /** navigated display to listen to */
    private NavigatedDisplay navDisplay;

    /** the perspective toggle button */
    private JToggleButton pButton;

    /** the perspective menu item */
    private JCheckBoxMenuItem pMenu;

    /** Names for changing perspectives */
    private String[] perspectiveNames;

    // defined dynamically based on display type

    /** the autorotate toggle button */
    private JToggleButton rotateButton;

    /** the perspective menu item */
    private JCheckBoxMenuItem rotateMenu;

    /** toolbar of controls */
    private JToolBar toolbar;

    /** the viewpoint dialog */
    ViewpointDialog viewpointDialog;

    /** Keep the dialog around for setting the state */
    private VertScaleDialog vsDialog;

    /**
     * Construct a new ViewpointControl for the NavigatedDisplay.
     *
     * @param navDisplay  display to use (cannot be null)
     */
    public ViewpointControl(NavigatedDisplay navDisplay) {
        if (navDisplay == null) {
            throw new NullPointerException(
                "ViewPointControl.ctor: NavigatedDisplay cannot be null");
        }

        this.navDisplay  = navDisplay;
        isPerspective    = navDisplay.isPerspectiveView();
        perspectiveNames = new String[] {
            navDisplay.getTopViewName(), navDisplay.getBottomViewName(),
            navDisplay.getNorthViewName(), navDisplay.getEastViewName(),
            navDisplay.getSouthViewName(), navDisplay.getWestViewName()
        };
    }

    /**
     * Make a Viewpoint controls toolbar.
     *
     * @return the toolbar
     */
    public JToolBar getToolBar() {
        return getToolBar(false);
    }

    /**
     * Make a Viewpoint controls toolbar.
     *
     * @param orientation  JToolBar.VERTICAL or JToolBar.HORIZONTAL
     * @return the toolbar
     */
    public JToolBar getToolBar(int orientation) {
        return getToolBar(false, orientation);
    }

    /**
     * Make a Viewpoint controls toolbar.
     *
     * @param floatable Is the toolbar floatable
     * @return the toolbar
     */
    public JToolBar getToolBar(boolean floatable) {
        return getToolBar(floatable, JToolBar.VERTICAL);
    }

    /**
     * Make a Viewpoint controls toolbar.
     *
     * @param floatable Is the toolbar floatable
     * @param orientation  JToolBar.VERTICAL or JToolBar.HORIZONTAL
     * @return the toolbar
     */
    public JToolBar getToolBar(boolean floatable, int orientation) {
        if (toolbar == null) {
            toolbar = makeToolBar(floatable);
        }

        toolbar.setFloatable(floatable);
        toolbar.setOrientation(orientation);

        return toolbar;
    }

    /**
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();

        if (cmd.equals(CMD_SETTOP)) {
            setTopView();
        } else if (cmd.equals(CMD_SETBOTTOM)) {
            setBottomView();
        } else if (cmd.equals(CMD_SETNORTH)) {
            setNorthView();
        } else if (cmd.equals(CMD_SETEAST)) {
            setEastView();
        } else if (cmd.equals(CMD_SETSOUTH)) {
            setSouthView();
        } else if (cmd.equals(CMD_SETWEST)) {
            setWestView();
        } else if (cmd.equals(CMD_ROTATEDIALOG)) {
            rotateViewpoint(new ViewpointInfo(vpAz, vpTilt));
        } else if (cmd.equals(CMD_SETVERTICALSCALE)) {
            double[] range = navDisplay.getVerticalRange();
            Unit     u     = navDisplay.getVerticalRangeUnit();

            changeVerticalScale(new VertScaleInfo(range[0], range[1], u));
        } else if (cmd.equals(CMD_SETEYEPOSITION)) {
            changeEyePosition();
        }
    }

    /**
     * Make the toolbar for this control.
     *
     * @return the toolbar
     */
    protected JToolBar makeToolBar() {
        return makeToolBar(false);
    }

    /**
     * Make the toolbar for this control.
     *
     *
     * @param floatable Is the toolbar floatable
     * @return the toolbar
     */
    protected JToolBar makeToolBar(boolean floatable) {

        // Viewpoint control menu
        // JToolBar toolbar = new JToolBar("Viewpoint Toolbar",
        // JToolBar.VERTICAL);
        JToolBar toolbar = new JToolBar("Viewpoint Toolbar");

        toolbar.setFloatable(floatable);

        for (int i = 0; i < perspectiveCmds.length; i++) {
            toolbar.add(makeButton(perspectiveIcons[i], perspectiveCmds[i],
                                   "Rotate to "
                                   + perspectiveNames[i].toLowerCase()
                                   + " viewpoint"));
        }

        pButton = GuiUtils.getToggleImageButton(
            GuiUtils.getScaledImageIcon(ICON_PERSPECTIVE, null, true),
            GuiUtils.getScaledImageIcon(ICON_PARALLEL, null, true), 2, 2);
        pButton.setSelected(isPerspective);
        pButton.setToolTipText("Set parallel/perspective projection");
        pButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !okToAcceptChangesFromPerspectiveWidgets) {
                    return;
                }

                boolean value = ((JToggleButton) e.getSource()).isSelected();

                changePerspectiveView(value);
            }
        });
        toolbar.add(pButton);
        toolbar.add(makeButton(ICON_USERVIEW, CMD_ROTATEDIALOG,
                               "Rotate to user specified view"));

        JButton vertScaleButton =
            makeButton(
                ICON_SETVERTICALSCALE, CMD_SETVERTICALSCALE,
                "<html>Set the vertical scale<br>Right mouse:<br>&nbsp;&nbsp;Click: increase upper 10%<br>&nbsp;&nbsp;Control-Click: decrease upper 10%<br>&nbsp;&nbsp;Shift-Click: decrease lower 10%<br>&nbsp;&nbsp;Shift-Control-Click: increase lower 10%</html>");

        vertScaleButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if ( !SwingUtilities.isRightMouseButton(me)) {
                    return;
                }

                try {
                    double[] range = navDisplay.getVerticalRange();
                    double   diff  = range[1] - range[0];

                    diff = (range[1] - range[0]) * (me.isControlDown()
                            ? -0.1
                            : 0.1);

                    if (me.isShiftDown()) {
                        navDisplay.setVerticalRange(range[0] - diff,
                                range[1]);
                    } else {
                        navDisplay.setVerticalRange(range[0],
                                range[1] + diff);
                    }

                    range = navDisplay.getVerticalRange();
                    ucar.unidata.util.LogUtil.message(
                        "Setting vertical range to: " + Misc.format(range[0])
                        + " - " + Misc.format(range[1]) + " "
                        + navDisplay.getVerticalRangeUnit());
                } catch (Exception exp) {
                    ucar.unidata.util.LogUtil.logException(
                        "Setting vertical scale", exp);
                }
            }
        });
        toolbar.add(vertScaleButton);
        rotateButton =
            GuiUtils.getToggleImageButton(
                GuiUtils.getScaledImageIcon(
                    "/auxdata/ui/icons/arrow_rotate_clockwise.png", null,
                    true), GuiUtils.getScaledImageIcon(
                        "/auxdata/ui/icons/arrow_rotate_clockwise.png", null,
                        true), 2, 2);
        rotateButton.setToolTipText("Auto-rotate;Right click to show menu");
        rotateButton.setSelected(getAutoRotate());
        rotateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeAutoRotate(rotateButton.isSelected());
            }
        });
        rotateButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if ( !SwingUtilities.isRightMouseButton(me)) {
                    return;
                }

                List<Object> items = new ArrayList<Object>();

                items.add(GuiUtils.makeMenuItem("Rotate Right",
                        ViewpointControl.this, "setRotate", ROTATE_RIGHT));
                items.add(GuiUtils.makeMenuItem("Rotate Left",
                        ViewpointControl.this, "setRotate", ROTATE_LEFT));
                items.add(GuiUtils.makeMenuItem("Rotate Up",
                        ViewpointControl.this, "setRotate", ROTATE_UP));
                items.add(GuiUtils.makeMenuItem("Rotate Down",
                        ViewpointControl.this, "setRotate", ROTATE_DOWN));
                items.add(GuiUtils.makeMenuItem("Rotate Clockwise",
                        ViewpointControl.this, "setRotate",
                        ROTATE_CLOCKWISE));
                items.add(GuiUtils.makeMenuItem("Rotate CounterClockwise",
                        ViewpointControl.this, "setRotate",
                        ROTATE_COUNTERCLOCKWISE));
                items.add(GuiUtils.MENU_SEPARATOR);
                items.add(GuiUtils.makeMenuItem("Rotate Faster", navDisplay,
                        "rotateFaster"));
                items.add(GuiUtils.makeMenuItem("Rotate Slower", navDisplay,
                        "rotateSlower"));
                GuiUtils.showPopupMenu(items, rotateButton);
            }
        });
        toolbar.add(rotateButton);

        // if(true) return toolbar;
        // toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return toolbar;
    }

    /**
     * Set the rotate direction
     *
     * @param dir  the direction
     */
    public void setRotate(String dir) {
        if (dir.equals(ROTATE_RIGHT)) {
            navDisplay.setRotationMultiplierMatrix(0, -1, 0);
        } else if (dir.equals(ROTATE_LEFT)) {
            navDisplay.setRotationMultiplierMatrix(0, 1, 0);
        } else if (dir.equals(ROTATE_UP)) {
            navDisplay.setRotationMultiplierMatrix(1, 0, 0);
        } else if (dir.equals(ROTATE_DOWN)) {
            navDisplay.setRotationMultiplierMatrix(-1, 0, 0);
        } else if (dir.equals(ROTATE_CLOCKWISE)) {
            navDisplay.setRotationMultiplierMatrix(0, 0, 1);
        } else if (dir.equals(ROTATE_COUNTERCLOCKWISE)) {
            navDisplay.setRotationMultiplierMatrix(0, 0, -1);
        }

        setAutoRotate(true);
    }

    /**
     * Utility to make an image button
     *
     * @param icon The icon
     * @param cmd The action command
     * @param tooltip The tooltip
     *
     * @return The button
     */
    private JButton makeButton(String icon, String cmd, String tooltip) {
        JButton button = GuiUtils.getScaledImageButton(icon, getClass(), 2,
                             2);

        button.setToolTipText(tooltip);
        button.setActionCommand(cmd);
        button.addActionListener(this);

        return button;
    }

    /**
     * Make a Viewpoint controls menu
     *
     * @return the menu
     */
    public JMenu getMenu() {
        return (menu != null)
               ? menu
               : makeMenu();
    }

    /**
     * Make the view point controls menu.
     *
     * @return the menu
     */
    protected JMenu makeMenu() {

        // Viewpoint control menu
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        JMenu viewMenu = new JMenu("Viewpoint");

        viewMenu.setToolTipText(" Get Viewpoint control widget");

        JMenuItem mi;
        ImageIcon icon;

        for (int i = 0; i < perspectiveCmds.length; i++) {
            mi = new JMenuItem(perspectiveNames[i]);
            mi.setMnemonic(perspectiveNames[i].charAt(0));
            mi.setIcon(GuiUtils.getScaledImageIcon(perspectiveIcons[i], null,
                    true));
            mi.setActionCommand(perspectiveCmds[i]);
            mi.addActionListener(this);
            viewMenu.add(mi);
        }

        viewMenu.add(pMenu = new JCheckBoxMenuItem("Perspective View",
                isPerspective));
        pMenu.setMnemonic('P');
        pMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !okToAcceptChangesFromPerspectiveWidgets) {
                    return;
                }

                changePerspectiveView(
                    ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        viewMenu.add(mi = new JMenuItem("Viewpoint Dialog..."));
        mi.setMnemonic('D');
        mi.setIcon(GuiUtils.getScaledImageIcon(ICON_USERVIEW, null, true));
        mi.setActionCommand(CMD_ROTATEDIALOG);
        mi.addActionListener(this);
        makeVerticalScaleMenuItem(viewMenu);
        viewMenu.add(rotateMenu = new JCheckBoxMenuItem("Auto-Rotate View",
                autoRotate));
        rotateMenu.setMnemonic('R');
        rotateMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !okToAcceptChangesFromPerspectiveWidgets) {
                    return;
                }

                changeAutoRotate(
                    ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        if (navDisplay.getStereoAvailable()) {
            eyePositionMenu = new JMenuItem("Stereo Controls...");
            viewMenu.add(eyePositionMenu);
            eyePositionMenu.setActionCommand(CMD_SETEYEPOSITION);
            eyePositionMenu.addActionListener(this);
        }

        return viewMenu;
    }

    /**
     * Add the vertical scale menu item to the menu
     *
     * @param viewMenu Menu to add item to
     */
    public void makeVerticalScaleMenuItem(JMenu viewMenu) {
        JMenuItem mi = new JMenuItem("Vertical Scale...");

        viewMenu.add(mi);
        mi.setMnemonic('V');
        mi.setIcon(GuiUtils.getScaledImageIcon(ICON_SETVERTICALSCALE, null,
                true));
        mi.setActionCommand(CMD_SETVERTICALSCALE);
        mi.addActionListener(this);
    }

    /**
     * Set the view to the top.
     */
    public void setTopView() {
        try {
            navDisplay.setView(navDisplay.TOP_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to top view got " + exp);
        }
    }

    /**
     * Set the view to the bottom.
     */
    public void setBottomView() {
        try {
            navDisplay.setView(navDisplay.BOTTOM_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to bottom view got " + exp);
        }
    }

    /**
     * Set the view to the north.
     */
    public void setNorthView() {
        try {
            navDisplay.setView(navDisplay.NORTH_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to north view got " + exp);
        }
    }

    /**
     * Set the view to the south.
     */
    public void setSouthView() {
        try {
            navDisplay.setView(navDisplay.SOUTH_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to south view got " + exp);
        }
    }

    /**
     * Set the view to the east.
     */
    public void setEastView() {
        try {
            navDisplay.setView(navDisplay.EAST_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to east view got " + exp);
        }
    }

    /**
     * Set the view to the west.
     */
    public void setWestView() {
        try {
            navDisplay.setView(navDisplay.WEST_VIEW);
        } catch (Exception exp) {
            System.out.println("  set viewpoint to west view got " + exp);
        }
    }

    /**
     * Set the eye position
     *
     * @param position the eye position
     */
    public void setEyePosition(double position) {
        eyePosition = position;
    }

    /**
     * Get the eye position
     *
     * @return the eye position
     */
    public double getEyePosition() {
        return eyePosition;
    }

    /**
     * Change the eye position for stereo systems
     */
    public void changeEyePosition() {
        final JLabel  label  = new JLabel(Misc.format(eyePosition));
        final JSlider slider = new JSlider(0, 1000,
                                           (int) (eyePosition * 1000));

        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                double value = slider.getValue() / 1000.;

                label.setText(Misc.format(value));

                if ( !slider.getValueIsAdjusting()) {
                    try {
                        navDisplay.setEyePosition(value);
                    } catch (Exception exp) {
                        System.out.println("  set the eye position got "
                                           + exp);
                    }

                    eyePosition = value;
                }
            }
        });

        JPanel p = new JPanel();

        p.setLayout(new BorderLayout());
        p.add(BorderLayout.CENTER, slider);
        p.add(BorderLayout.EAST, label);
        GuiUtils.showOkCancelDialog(null, "Set Eye Position", p,
                                    eyePositionMenu);
    }

    /**
     * Get whether the view is perspective or parallel.
     * @return true if perspective
     */
    public boolean getPerspectiveView() {
        return isPerspective;
    }

    /**
     * A method that is called when the user changes the perspective view
     * through the menu or button. It can be overridden so other code can
     * be notified.
     *
     * @param v  true to set perspective view
     */
    public void changePerspectiveView(boolean v) {
        setPerspectiveView(v);
    }

    /**
     * Set the perspective view.
     *
     * @param perspective  true for perspective
     */
    public void setPerspectiveView(boolean perspective) {
        if (perspective == isPerspective) {
            return;
        }

        try {
            isPerspective                           = perspective;
            okToAcceptChangesFromPerspectiveWidgets = false;
            navDisplay.setPerspectiveView(perspective);

            if ((pMenu != null) && (perspective != pMenu.isSelected())) {
                pMenu.setSelected(perspective);
            }

            if ((pButton != null) && (perspective != pButton.isSelected())) {
                pButton.setSelected(perspective);
            }

            okToAcceptChangesFromPerspectiveWidgets = true;
        } catch (Exception exp) {
            exp.printStackTrace();
            System.out.println("  set perspective to " + perspective
                               + " got " + exp);
        }
    }

    /**
     * Get whether the display should autorotate or not
     * @return true if autorotate
     */
    public boolean getAutoRotate() {
        return autoRotate;
    }

    /**
     * A method that is called when the user changes the state of
     * autorotation through the menu or button. It can be overridden
     * so other code can be notified.
     *
     * @param v  true to set auto rotation
     */
    public void changeAutoRotate(boolean v) {
        setAutoRotate(v);
    }

    /**
     * Set the auto rotate value
     *
     * @param rotate true to rotate
     */
    public void setAutoRotate(boolean rotate) {
        if (rotate == autoRotate) {
            return;
        }

        try {
            autoRotate                             = rotate;
            okToAcceptChangesFromAutoRotateWidgets = false;
            navDisplay.setAutoRotate(autoRotate);

            if ((rotateMenu != null)
                    && (autoRotate != rotateMenu.isSelected())) {
                rotateMenu.setSelected(autoRotate);
            }

            if ((rotateButton != null)
                    && (rotate != rotateButton.isSelected())) {
                rotateButton.setSelected(rotate);
            }

            okToAcceptChangesFromAutoRotateWidgets = true;
        } catch (Exception exp) {
            exp.printStackTrace();
            System.out.println("  set autorotate to " + rotate + " got "
                               + exp);
        }
    }

    /**
     * Rotate the viewpoint to the parameters in <code>transfer</code>
     *
     * @param transfer  holder of the new view point information
     * @return the transfer
     */
    public ViewpointInfo rotateViewpoint(ViewpointInfo transfer) {
        if (viewpointDialog == null) {
            viewpointDialog = new ViewpointDialog(this,
                    GuiUtils.getFrame(navDisplay.getComponent()));
        }

        viewpointDialog.showDialog(transfer);

        return transfer;
    }

    /**
     * Get the view point information.
     *
     * @return the view point information
     */
    public ViewpointInfo getViewpointInfo() {
        return new ViewpointInfo(vpAz, vpTilt);
    }

    /**
     * Set the view point information
     *
     * @param vpi  new view point info
     */
    public void setViewpointInfo(ViewpointInfo vpi) {
        vpAz   = vpi.azimuth;
        vpTilt = vpi.tilt;

        try {
            if (navDisplay != null) {
                navDisplay.rotateView(navDisplay.getSavedProjectionMatrix(),
                                      vpi.azimuth, vpi.tilt);
            }
        } catch (Exception exp) {
            System.out.println("   rotateViewpoint got " + exp);
        }
    }

    /**
     * Get the vertical scale widget
     *
     * @return vertical scale widget
     */
    public VertScaleDialog getVerticalScaleWidget() {
        double[]        range = navDisplay.getVerticalRange();
        Unit            u     = navDisplay.getVerticalRangeUnit();
        VertScaleInfo   temp  = new VertScaleInfo(range[0], range[1], u);
        VertScaleDialog vsd   = null;

        if (navDisplay instanceof GlobeDisplay) {
            vsd = new VertScaleDialog(
                GuiUtils.getFrame(navDisplay.getComponent()), this, temp);
        } else {
            vsd = new VertScaleDialog(
                GuiUtils.getFrame(navDisplay.getComponent()), this, temp);
        }

        return vsd;
    }

    /**
     * Change the vertical scale
     *
     * @param transfer  vertical scale information
     */
    public void changeVerticalScale(VertScaleInfo transfer) {
        if (vsDialog == null) {
            if (navDisplay instanceof GlobeDisplay) {
                vsDialog = new GlobeScaleDialog(
                    GuiUtils.getFrame(navDisplay.getComponent()), this);
            } else {
                vsDialog = new VertScaleDialog(
                    GuiUtils.getFrame(navDisplay.getComponent()), this);
            }
        }

        vsDialog.showDialog(transfer);
    }

    /**
     * Apply the vertical scale
     *
     * @param transfer The info
     *
     * @throws Exception On badness
     */
    protected void applyVerticalScale(VertScaleInfo transfer)
            throws Exception {
        navDisplay.setDisplayInactive();
        navDisplay.setVerticalRangeUnit(transfer.unit);
        navDisplay.setVerticalRange(transfer.minVertScale,
                                    transfer.maxVertScale);
        navDisplay.setDisplayActive();
    }

    /**
     * Apply the lat/lon info
     *
     * @param latLonInfo
     * @throws Exception
     */
    public void applyLatLonScale(LatLonScaleInfo latLonInfo)
            throws Exception {
        navDisplay.setDisplayInactive();
        navDisplay.setLatLonScaleInfo(latLonInfo);
        navDisplay.setDisplayActive();
    }
}
