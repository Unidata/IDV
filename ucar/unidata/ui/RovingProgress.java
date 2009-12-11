/*
 * $Id: RovingProgress.java,v 1.11 2007/07/06 20:45:33 jeffmc Exp $
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


import ucar.unidata.util.Removable;
import ucar.unidata.util.GuiUtils;



import ucar.unidata.util.Misc;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.text.SimpleDateFormat;

import java.util.Date;

import javax.swing.*;
import javax.swing.border.*;


/**
 * Shows a roving bar
 *
 * @author MetApps Development Team
 * @version $Revision: 1.11 $ $Date: 2007/07/06 20:45:33 $
 */
public class RovingProgress extends JPanel implements Removable {

    /** Default insets */
    public static final Insets DEFAULT_INSETS = new Insets(0, 0, 0, 0);

    /** Default color */
    public static final Color DEFAULT_COLOR = new Color(156, 153, 205);

    /** For running */
    private Runnable currentRunnable;

    /** Bar width */
    private int barWidth = 20;

    /** Percent along the width of the component we are drawing the box */
    private double percent = 0.0;

    /** Are we actve */
    private boolean running = false;

    /** The percent increment */
    private double delta = 0.05;

    /** How long do we sleep */
    private long sleepTime = 100;

    /** The color to draw the box in */
    private Color color = null;

    /** Label to draw. May be null. */
    private String label;

    /** for double buffering */
    private BufferedImage bufferedImage;

    /** for double buffering */
    private Dimension imageSize;

    /** _more_          */
    private boolean showClock = false;

    /** _more_          */
    private boolean clockRunning = false;

    /** _more_          */
    private static final Font clockFont = new Font("Dialog", Font.BOLD, 11);

    /** _more_          */
    private static SimpleDateFormat clockFormat =
        new SimpleDateFormat("HH:mm:ss z");

    /**
     * _more_
     *
     * @param showClock _more_
     */
    public RovingProgress(boolean showClock) {
        this(null, null);
        this.showClock = showClock;
        if (showClock) {
            startClock();
        }
    }


    /**
     * _more_
     */
    private void startClock() {
        if (clockRunning) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                startClockInner();
            }
        });
    }

    /**
     * _more_
     */
    private void startClockInner() {
        while (showClock) {
            repaint();
            Misc.sleep(1000);
        }
        clockRunning = false;
    }


    /**
     * _more_
     *
     * @param showClock _more_
     */
    public void setShowClock(boolean showClock) {
        this.showClock = showClock;
        if (showClock) {
            startClock();
        }
    }




    /**
     * ctor
     *
     */
    public RovingProgress() {
        this(null, null);
    }

    /**
     * ctor
     *
     * @param label The label
     */
    public RovingProgress(String label) {
        this(null, label);
    }

    /**
     * ctor
     *
     * @param c Bar color
     *
     */
    public RovingProgress(Color c) {
        this(c, null);
    }

    /**
     * ctor
     *
     * @param c color
     * @param label label
     *
     */
    public RovingProgress(Color c, String label) {
        this.label = label;
        if (c == null) {
            this.color = DEFAULT_COLOR;
        } else {
            this.color = c;
        }
    }

    /**
     * Stop running
     */
    public synchronized void stop() {
        currentRunnable = null;
        running         = false;
        repaint();
    }

    /**
     * _more_
     */
    public void doRemove() {
        currentRunnable = null;
        running         = false;
        showClock       = false;
        GuiUtils.empty(this, true);
    }



    /**
     * Start running
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running         = true;
        currentRunnable = new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.currentThread().sleep(sleepTime);
                        if (currentRunnable != this) {
                            return;
                        }
                        tick();
                    } catch (Exception exc) {
                        System.err.println(exc);
                    }
                }
            }
        };
        Thread thread = new Thread(currentRunnable);
        thread.start();
    }


    /**
     * Reset the state of the progress bar to the beginning
     */
    public void reset() {
        percent = 0.0;
        repaint(1);
    }

    /**
     * _more_
     */
    private void tick() {
        percent += delta;
        if (percent > 1.0) {
            if (drawFilledSquare()) {
                delta = -delta;
            } else {
                percent = 0;
            }
        } else if (percent < 0.0) {
            delta = -delta;
        }
        repaint(1);
    }


    /**
     * Are we running
     *
     * @return is running
     */
    public boolean isRunning() {
        return running;
    }



    /**
     * Paint
     *
     * @param g The graphics
     */
    public final void paint(Graphics g) {
        Dimension dim = getSize();
        if ( !Misc.equals(dim, imageSize)) {
            imageSize     = dim;
            bufferedImage = (BufferedImage) createImage(dim.width,
                    dim.height);
        }
        Graphics bg = bufferedImage.createGraphics();
        super.paint(bg);
        paintInner(bg);
        g.drawImage(bufferedImage, 0, 0, null);
    }

    /**
     * paint
     *
     * @param g the graphics
     */
    public void paintInner(Graphics g) {
        Rectangle b  = getBounds();
        Color     bg = getBackground();
        if (bg == null) {
            bg = Color.gray;
        }
        g.setColor(getBackground());
        g.fillRect(2, 2, b.width - 4, b.height - 4);
        if ( !isRunning()) {
            if (showClock) {
                g.setFont(clockFont);
                g.setColor(Color.BLACK);
                Date d = new Date();
                clockFormat.setTimeZone(GuiUtils.getTimeZone());
                g.drawString(clockFormat.format(d), 3, b.height - 5);
            }
            return;
        }
        int w = b.width;

        g.setColor(color);
        Insets insets = DEFAULT_INSETS;
        Border border = getBorder();
        if (border != null) {
            insets = border.getBorderInsets(this);
        }

        if (drawFilledSquare()) {
            int x = (int) (percent * (b.width - barWidth));
            if (x + barWidth > b.width) {
                x -= ((x + barWidth) - b.width);
            }
            g.fillRect(x, insets.top, barWidth,
                       b.height - (insets.top + insets.bottom));
        } else {
            int x   = (int) (percent * b.width);
            int myX = 0;
            while (myX < x) {
                g.fillRect(myX, insets.top, 1,
                           b.height - (insets.top + insets.bottom));
                myX += 3;
            }
        }

        paintLabel(g, b);
    }

    /**
     * Should we draw a filled square
     *
     * @return draw filled square
     */
    public boolean drawFilledSquare() {
        return true;
    }

    /**
     * Paint the label if its non-null
     *
     * @param g graphics
     * @param bounds component bounds
     */
    public void paintLabel(Graphics g, Rectangle bounds) {
        if (label != null) {
            g.setColor(Color.gray);
            g.drawString(label, 5, bounds.height - 4);
        }
    }


    /**
     * test
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {
        JFrame         f  = new JFrame();
        RovingProgress mm = new RovingProgress(Color.blue);
        f.getContentPane().add(mm);
        f.pack();
        f.show();
        mm.start();
    }

}

