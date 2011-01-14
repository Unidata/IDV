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
import ucar.unidata.idv.flythrough.Flythrough;

import ucar.unidata.idv.ui.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;

import ucar.unidata.view.geoloc.NavigatedDisplay;


import ucar.visad.GeoUtils;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.awt.image.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * An abstract  base class used to represents display control legends
 * for view managers.
 *
 * @author IDV development team
 */

public class EarthNavPanel extends JPanel implements MouseListener,
        MouseMotionListener {


    /** Property to show the address field */
    private static final String PROP_SHOWADDRESS = "earthnav.showaddress";

    /** show the address field */
    private boolean showAddress = true;

    /** action command */
    private static final String CMD_JOYSTICK = "cmd.joystick";


    /** should ignore changes */
    private boolean ignoreChanges = false;

    /** My view manager */
    private MapViewManager viewManager;

    /** Sandra's icon */
    private ImageIcon panelIcon;

    /** List of hotspots */
    private List locations = new ArrayList();

    /** The label to show the icon in */
    JLabel label;

    /** go to address widget */
    JComboBox addressBox;

    /** Use to kill threaded goto address calls */
    private int[] masterTimeStamp = { 0 };

    /** _more_          */
    private Flythrough flythrough;

    /** _more_ */
    private boolean handlingKey = false;


    /**
     * ctor
     *
     * @param viewManager The viewManager
     */
    public EarthNavPanel(MapViewManager viewManager) {
        String prop = viewManager.getSkinProperty(PROP_SHOWADDRESS);
        if (prop != null) {
            showAddress = new Boolean(prop).booleanValue();
        }
        init(viewManager);
    }

    /**
     * _more_
     *
     * @param viewManager _more_
     * @param flythrough _more_
     * @param showAddress _more_
     */
    public EarthNavPanel(MapViewManager viewManager, Flythrough flythrough,
                         boolean showAddress) {
        this.showAddress = showAddress;
        this.flythrough  = flythrough;
        init(viewManager);
    }


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    private void init(MapViewManager viewManager) {
        this.viewManager = viewManager;
        setLayout(new BorderLayout());
        label = new JLabel("", SwingConstants.CENTER) {
            public void paint(Graphics g) {
                super.paint(g);
                if ((pressed != null)
                        && !pressed.method.equals(CMD_JOYSTICK)) {
                    int iconWidth = panelIcon.getIconWidth();
                    int myWidth   = getBounds().width;
                    int iconX     = myWidth / 2 - iconWidth / 2;
                    g.setColor(Color.gray);
                    //            g.setColor(Color.red);
                    g.drawArc(iconX + pressed.r.x, pressed.r.y,
                              pressed.r.width, pressed.r.height, 0, 360);
                }

            }
        };
        add(BorderLayout.CENTER, label);
        label.setIcon(panelIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/EarthPanel.png"));
        label.addMouseListener(this);
        label.addMouseMotionListener(this);

        label.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent ke) {
                isControlDown = ke.isControlDown();
                isShiftDown   = ke.isShiftDown();
            }

            public void keyPressed(KeyEvent ke) {
                isControlDown = ke.isControlDown();
                isShiftDown   = ke.isShiftDown();
                if (flythrough == null) {
                    return;
                }
                if (handlingKey) {
                    return;
                }
                handlingKey = true;
                try {
                    if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                        flythrough.driveRight();
                    } else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                        flythrough.driveLeft();
                    } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
                        flythrough.driveForward();
                    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                        flythrough.driveBack();
                    }
                } finally {
                    handlingKey = false;
                }
            }
        });




        addressBox = new JComboBox();
        addressBox.setToolTipText(
            "<html>Enter an address, zip code, city,state, etc.<br>"
            + GeoUtils.addressToolTip + "</html>");
        GeoUtils.initAddressBox(addressBox);
        addressBox.setEditable(true);
        addressBox.addActionListener(GuiUtils.makeActionListener(this,
                "goToAddress", null));
        JButton addressSearchBtn = GuiUtils.makeButton("Go To Address:",
                                       this, "goToAddress");
        addressSearchBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
                0));
        JComponent addressPanel = GuiUtils.vbox(addressSearchBtn, addressBox);
        addressPanel = GuiUtils.inset(GuiUtils.top(addressPanel), 5);
        if (showAddress) {
            add(BorderLayout.WEST, addressPanel);
        }

        initLocations();
    }





    /**
     * Go to address
     */
    public void goToAddress() {
        if (ignoreChanges) {
            return;
        }
        Misc.run(this, "goToAddressInner");
    }

    /**
     * Go to address
     */
    public void goToAddressInner() {
        try {
            masterTimeStamp[0]++;
            int timestamp = masterTimeStamp[0];
            viewManager.getIdvUIManager().showWaitCursor();
            String address = (String) addressBox.getSelectedItem();
            LatLonPoint llp = GeoUtils.getLocationFromAddress(address,
                                  masterTimeStamp);
            viewManager.getIdvUIManager().showNormalCursor();
            if (llp == null) {
                if (timestamp == masterTimeStamp[0]) {
                    LogUtil.userMessage("Could not find address");
                }
            } else {
                Real alt = new Real(RealType.Altitude, 1000.0);
                viewManager.getMapDisplay().centerAndZoom(
                    GeoUtils.toEarthLocation(llp), alt, 1.0, true, true);
                ignoreChanges = true;
                GeoUtils.initAddressBox(addressBox);
                ignoreChanges = false;
            }
        } catch (Exception exc) {
            LogUtil.logException("Error going to address", exc);
        }
    }

    /** location in icon */
    private static int width0 = 10;

    /** location in icon */
    private static int width1 = 18;

    /** location in icon */
    private static int width2 = 20;

    /** location in icon */
    private static int width3 = 32;

    /** location in icon */
    private static int topY1 = 33;

    /** location in icon */
    private static int bottomY1 = 70;

    /** location in icon */
    private static int zoomX = 20;

    /** location in icon */
    private static int tiltX = 275;

    /** location in icon */
    private static int rotateY = 23;

    /** location in icon */
    private static int rotateLeftX = 66;

    /** location in icon */
    private static int rotateRightX = 227;

    /** location in icon */
    private static int panLeftX = 117;

    /** location in icon */
    private static int panRightX = 174;

    /** location in icon */
    private static int panHY = 53;

    /** location in icon */
    private static int panVX = 145;

    /** location in icon */
    private static int panUpY = 28;

    /** location in icon */
    private static int panDownY = 78;

    /** location in icon */
    private Location pressed;

    /** location in icon */
    private Location joystick;

    /** current drag event */
    private MouseEvent dragEvent;

    /** event flag */
    private boolean isControlDown = false;

    /** event flag */
    private boolean isShiftDown = false;

    /** For threading */
    private int timeStamp = 0;



    /**
     * init the hotspots
     */
    private void initLocations() {
        locations.add(new Location(146, 52, width0, CMD_JOYSTICK));


        locations.add(new Location(zoomX, topY1, width1,
                                   "action:"
                                   + MapViewManager.CMD_NAV_SMALLZOOMIN));
        locations.add(new Location(zoomX, bottomY1, width1,
                                   "action:"
                                   + MapViewManager.CMD_NAV_SMALLZOOMOUT));
        locations.add(new Location(tiltX, topY1, width1,
                                   "action:"
                                   + MapViewManager.CMD_NAV_SMALLTILTDOWN));

        locations.add(new Location(tiltX, bottomY1, width1,
                                   "action:"
                                   + MapViewManager.CMD_NAV_SMALLTILTUP));
        locations.add(new Location(rotateLeftX, rotateY, width3,
                                   "action:"
                                   + MapViewManager.CMD_NAV_SMALLROTATELEFT));
        locations.add(
            new Location(
                rotateRightX, rotateY, width3,
                "action:" + MapViewManager.CMD_NAV_SMALLROTATERIGHT));

        if (flythrough != null) {
            locations.add(new Location(panLeftX, panHY, width2,
                                       "action:"
                                       + MapViewManager.CMD_FLY_LEFT));
            locations.add(new Location(panRightX, panHY, width2,
                                       "action:"
                                       + MapViewManager.CMD_FLY_RIGHT));

            locations.add(new Location(panVX, panUpY, width2,
                                       "action:"
                                       + MapViewManager.CMD_FLY_FORWARD));
            locations.add(new Location(panVX, panDownY, width2,
                                       "action:"
                                       + MapViewManager.CMD_FLY_BACK));
        } else {
            locations.add(new Location(panLeftX, panHY, width2,
                                       "action:"
                                       + MapViewManager.CMD_NAV_SMALLLEFT));
            locations.add(new Location(panRightX, panHY, width2,
                                       "action:"
                                       + MapViewManager.CMD_NAV_SMALLRIGHT));

            locations.add(new Location(panVX, panUpY, width2,
                                       "action:"
                                       + MapViewManager.CMD_NAV_SMALLUP));
            locations.add(new Location(panVX, panDownY, width2,
                                       "action:"
                                       + MapViewManager.CMD_NAV_SMALLDOWN));
        }




        locations.add(new Location(216, 86, 22,
                                   "action:" + MapViewManager.CMD_NAV_HOME));

        locations.add(new Location(76, 73, width2, "action:none"));
    }






    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseDragged(MouseEvent e) {
        isControlDown = e.isControlDown();
        isShiftDown   = e.isShiftDown();
        dragEvent     = e;

    }


    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseMoved(MouseEvent e) {
        isControlDown = e.isControlDown();
        isShiftDown   = e.isShiftDown();
    }

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseExited(MouseEvent e) {}


    /**
     * Mouse was pressed
     *
     * @param event The event
     */
    public void mousePressed(MouseEvent event) {
        label.requestFocus();
        isControlDown = event.isControlDown();
        isShiftDown   = event.isShiftDown();
        timeStamp++;
        pressed = findLocation(event);
        if (pressed == null) {
            return;
        }
        if (pressed.method.equals(CMD_JOYSTICK)) {
            //            dragged =pressed;
            //            pressed = null;
        }
        if (pressed != null) {
            label.repaint();
            startRun(pressed, ++timeStamp);
        }
    }

    /**
     * Handle the hotspot event
     *
     * @param location The hotspot
     * @param theTimeStamp for multiple events
     */
    public void startRun(final Location location, final int theTimeStamp) {
        Misc.run(new Runnable() {
            public void run() {
                while (true) {
                    processLocation(pressed);
                    Misc.sleep(100);
                    if ((timeStamp != theTimeStamp)
                            || (location != pressed)) {
                        break;
                    }
                }
            }
        });
    }


    /**
     * Mouse was pressed
     *
     * @param event The event
     */
    public void mouseReleased(MouseEvent event) {
        isControlDown = event.isControlDown();
        isShiftDown   = event.isShiftDown();
        timeStamp++;
        if (pressed != null) {
            pressed = null;
            label.repaint();
        }
    }

    /**
     * Get the relative x
     *
     * @param event the event
     *
     * @return The relative x
     */
    private int getX(MouseEvent event) {
        int iconWidth = panelIcon.getIconWidth();
        int myWidth   = label.getBounds().width;
        int iconX     = myWidth / 2 - iconWidth / 2;
        return event.getX() - iconX;
    }


    /**
     * Find the hotspot
     *
     * @param event event
     *
     * @return The hotspot at the event
     */
    private Location findLocation(MouseEvent event) {
        int iconX = getX(event);
        int iconY = event.getY();

        //        System.err.println(iconX + " " + iconY);

        Location closest = null;
        Point    p       = new Point(iconX, iconY);
        for (int i = 0; i < locations.size(); i++) {
            Location l = (Location) locations.get(i);
            if (l.r.contains(p)) {
                return l;
            }
        }

        return closest;
    }



    /**
     * Handle the hotspot
     *
     * @param location The hotspot
     */
    public void processLocation(Location location) {
        if (location == null) {
            return;
        }
        if (location.method.equals(CMD_JOYSTICK) && (dragEvent != null)) {
            NavigatedDisplay navDisplay = viewManager.getMapDisplay();
            int              x          = getX(dragEvent);
            int              y          = dragEvent.getY();
            int              dx         = location.cx - x;
            int              dy         = y - location.cy;
            int              limit      = 40;
            if (dx < -limit) {
                dx = -limit;
            }
            if (dx > limit) {
                dx = limit;
            }
            if (dy < -limit) {
                dy = -limit;
            }
            if (dy > limit) {
                dy = limit;
            }
            double px = dx / (double) limit;
            double py = dy / (double) limit;

            //            if(x<location.centerX) 
            if (isControlDown) {
                //              viewManager.actionPerformed(new ActionEvent(this, 1, MapViewManager.CMD_NAV_SMALLZOOMIN));
            } else if (isShiftDown) {
                //              viewManager.actionPerformed(new ActionEvent(this, 1, MapViewManager.CMD_NAV_SMALLZOOMOUT));
            }
            navDisplay.translate(px * 0.06, py * 0.06);
            return;
        }


        if (location.method.startsWith("action:")) {
            String cmd = location.method.substring(7);
            viewManager.actionPerformed(new ActionEvent(this, 1, cmd));
            return;
        }
    }


    /**
     * Handle event
     *
     * @param event event
     */
    public void mouseClicked(MouseEvent event) {}


    /**
     * Class Location is used to define the hotspots in the icon
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.18 $
     */
    private static class Location {

        /** center x */
        int cx;

        /** center y */
        int cy;

        /** size */
        Rectangle r;

        /** The method to process */
        String method;

        /**
         * ctor
         *
         * @param x x
         * @param y y
         * @param r r
         * @param method method
         */
        public Location(int x, int y, int r, String method) {
            this(x, y, r, r, method);
        }

        /**
         * ctor
         *
         * @param x x
         * @param y y
         * @param w width
         * @param h height
         * @param method method
         */
        public Location(int x, int y, int w, int h, String method) {
            this.r      = new Rectangle(x - w / 2, y - h / 2, w, h);
            cx          = x;
            cy          = y;
            this.method = method;
        }

        /**
         * to string
         *
         * @return to string
         */
        public String toString() {
            return method;
        }
    }




}
