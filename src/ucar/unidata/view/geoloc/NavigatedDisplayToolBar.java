/*
 * $Id: NavigatedDisplayToolBar.java,v 1.14 2007/06/11 21:28:20 jeffmc Exp $
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



package ucar.unidata.view.geoloc;


import ucar.unidata.util.GuiUtils;

import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;



import javax.swing.*;



/**
 * A JToolBar that can be used to move around a NavigatedDisplay.
 * The tool bar has buttons for panning and zooming and resetting
 * the display to the last saved projection.
 *
 * @see ucar.visad.display.DisplayMaster#resetProjection
 * @see NavigatedDisplay#translate
 * @see NavigatedDisplay#zoom
 *
 * @author  Don Murray
 * @version $Revision: 1.14 $ $Date: 2007/06/11 21:28:20 $
 */
public class NavigatedDisplayToolBar extends JToolBar {

    /** NavigatedDisplay that this listens to */
    private NavigatedDisplay navDisplay;

    /** Base translation_ */
    public static final double TRANSLATE_FACTOR = 0.1;

    /** default zoom factor */
    public static final double ZOOM_FACTOR = 1.2;

    /**
     * Construct a NavigatedDisplayToolBar for the navDisplay.
     *
     * @param navDisplay  the display that this toolbar controls
     */
    public NavigatedDisplayToolBar(NavigatedDisplay navDisplay) {
        this(navDisplay, JToolBar.HORIZONTAL);
    }

    /**
     * Construct a NavigatedDisplayToolBar for the navDisplay.
     *
     * @param navDisplay  the display that this toolbar controls
     * @param orientation orientation
     */
    public NavigatedDisplayToolBar(NavigatedDisplay navDisplay,
                                   int orientation) {
        this(navDisplay, orientation, false);
    }

    /**
     * Construct a NavigatedDisplayToolBar for the navDisplay.
     *
     * @param navDisplay  the display that this toolbar controls
     * @param orientation orientation
     * @param floatable Is toolbar floatable
     */
    public NavigatedDisplayToolBar(NavigatedDisplay navDisplay,
                                   int orientation, boolean floatable) {
        super("Navigated Display Toolbar", orientation);
        this.navDisplay = navDisplay;
        setFloatable(floatable);
        makeToolBar();
    }

    public  void destroy() {
        navDisplay =null;
    }

    /**
     * Set the navDisplay that this NavigatedDisplayToolBar controls.
     *
     * @param navDisplay  the display that this toolbar controls
     */
    public void setNavigatedDisplay(NavigatedDisplay navDisplay) {
        this.navDisplay = navDisplay;
    }

    /**
     * Make an image button
     *
     * @param path path to image
     * @param toolTip tooltip
     *
     * @return button
     */
    private JButton makeButton(String path, String toolTip) {
        JButton button = GuiUtils.getScaledImageButton(path, getClass(), 0, 0);
        //        JButton button = GuiUtils.getImageButton(path,getClass(),2,2);
        button.setToolTipText(toolTip);
        add(button);
        return button;
    }


    /**
     * Make the toolbar
     */
    private void makeToolBar() {
        JButton button;

        button = makeButton("/auxdata/ui/icons/magnifier_zoom_in.png", "Zoom in");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.zoom(ZOOM_FACTOR);
            }
        });

        // zoom out button
        button = makeButton("/auxdata/ui/icons/magnifier_zoom_out.png", "Zoom out");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.zoom(1.0 / (double) ZOOM_FACTOR);
            }
        });

        // reset button
        button = makeButton("/auxdata/ui/icons/house.png",
                            "Reset Display Projection");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    navDisplay.resetScaleTranslate();
                } catch (VisADException ve) {
                    ve.printStackTrace();
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
            }
        });

        //        addSeparator();

        // translate left
        button = makeButton("/auxdata/ui/icons/arrow_left.png", "Translate left");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.translate(TRANSLATE_FACTOR, 0.0);
            }
        });


        // translate right
        button = makeButton("/auxdata/ui/icons/arrow_right.png",
                            "Translate right");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.translate(-TRANSLATE_FACTOR, 0.0);
            }
        });


        // translate up
        button = makeButton("/auxdata/ui/icons/arrow_up.png", "Translate up");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.translate(0.0, -TRANSLATE_FACTOR);
            }
        });

        // translate down
        button = makeButton("/auxdata/ui/icons/arrow_down.png", "Translate down");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.translate(0.0, TRANSLATE_FACTOR);
            }
        });

        //        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));


    }
}

