/*
 * $Id: MemoryMonitor.java,v 1.12 2006/12/27 19:53:50 jeffmc Exp $
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



package ucar.unidata.util;


import ucar.unidata.util.GuiUtils;


import java.awt.*;
import java.awt.event.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.swing.*;

import javax.swing.event.*;


/**
 * Class MemoryMonitor
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
public class MemoryMonitor extends JPanel implements Runnable, Removable {


    /** flag for running */
    private boolean running = false;

    /** sleep interval */
    private long sleepInterval = 1000;

    /** a thread */
    private Thread thread;

    /** percent threshold */
    private int percentThreshold;

    /** number of times above the threshold */
    private int timesAboveThreshold = 0;

    /** format */
    private static DecimalFormat fmt = new DecimalFormat("#0");

    /** the label */
    private JLabel label1;

    /** another label */
    private JLabel label2;

    /** the foreground color for the label */
    private Color labelForeground;

    /** flag for in the red */
    private boolean inTheRed = false;

    /** Keep track of the last time we ran the gc and cleared the cache */
    private static long lastTimeRanGC = -1;


    /** _more_ */
    private boolean showClock = true;

    /** _more_ */
    private static final Font clockFont = new Font("Dialog", Font.BOLD, 11);

    /** _more_ */
    private static SimpleDateFormat clockFormat =
        new SimpleDateFormat("HH:mm:ss z");



    /** _more_ */
    private String memoryLabel = "";

    /**
     * Default constructor
     */
    public MemoryMonitor() {
        this(80);
    }




    /**
     * Create a new MemoryMonitor
     *
     * @param percentThreshold  the percentage of use memory before
     *                          garbage collection is run
     *
     */
    public MemoryMonitor(int percentThreshold) {
        this(percentThreshold, true);
    }


    /**
     * _more_
     *
     * @param percentThreshold _more_
     * @param showTheClock _more_
     */
    public MemoryMonitor(int percentThreshold, boolean showTheClock) {
        super(new BorderLayout());
        this.showClock = showTheClock;
        label1         = new JLabel("", SwingConstants.RIGHT) {
            public String getToolTipText(MouseEvent me) {
                StringBuffer sb = new StringBuffer();
                sb.append("<html><table>");
                sb.append(
                    "<tr><td align=right><b>Current Date/Time:</b></td><td><i>"
                    + GuiUtils.formatDate(new Date()) + "</i></td></tr>");
                sb.append(
                    "<tr><td align=right><b>Memory Usage:</b></td><td><i>"
                    + memoryLabel + "</i></td></tr>");
                sb.append("</table>");
                if (showClock) {
                    sb.append("Click to show memory usage");
                } else {
                    sb.append("Click to show clock");
                }
                sb.append("</html>");
                return sb.toString();
            }
        };
        label2 = new JLabel("");

        Font f = label1.getFont();
        label1.setToolTipText("Used memory/Max used memory/Max memory");
        label2.setToolTipText("Used memory/Max used memory/Max memory");
        //        f = f.deriveFont(8.0f);
        label1.setFont(f);
        label2.setFont(f);
        this.percentThreshold = percentThreshold;
        this.add(BorderLayout.CENTER,
                 new Msg.SkipPanel(GuiUtils.hbox(Misc.newList(label1,
                     label2))));

        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    showClock = !showClock;
                    showStats();
                }
                handleMouseEvent(e);
            }
        };
        labelForeground = label2.getForeground();
        label1.addMouseListener(ml);
        label2.addMouseListener(ml);
        start();
    }

    /**
     * _more_
     */
    public void doRemove() {
        running = false;
        GuiUtils.empty(this);
    }

    /**
     * Popup a menu on an event
     *
     * @param event the event
     */
    private void popupMenu(MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();
        if (running) {
            //            popup.add(GuiUtils.makeMenuItem("Stop Running",
            //                                            MemoryMonitor.this,
            //                                            "toggleRunning"));
        } else {
            //            popup.add(GuiUtils.makeMenuItem("Resume Running",
            //                                            MemoryMonitor.this,
            //                                            "toggleRunning"));
        }


        popup.add(GuiUtils.makeMenuItem("Clear Memory & Cache",
                                        MemoryMonitor.this, "runGC"));
        popup.show(this, event.getX(), event.getY());
    }


    /**
     * Toggle running
     */
    public void toggleRunning() {
        if (running) {
            stop();
        } else {
            start();
        }
    }


    /**
     * Handle a mouse event
     *
     * @param event the event
     */
    private void handleMouseEvent(MouseEvent event) {

        if (SwingUtilities.isRightMouseButton(event)) {
            popupMenu(event);
            return;
        }

    }



    /**
     * Set the label font
     *
     * @param f the font
     */
    public void setLabelFont(Font f) {
        label1.setFont(f);
        label2.setFont(f);
    }

    /**
     * Stop running
     */
    public synchronized void stop() {
        running = false;
        label1.setEnabled(false);
        label2.setEnabled(false);
    }



    /**
     * Start running
     */
    private synchronized void start() {
        if (running) {
            return;
        }
        label1.setEnabled(true);
        label2.setEnabled(true);
        running = true;
        thread  = new Thread(this, "Memory monitor");
        thread.start();
    }


    /**
     * Run the GC and clear the cache
     */
    public void runGC() {
        CacheManager.clearCache();
        Runtime.getRuntime().gc();
        lastTimeRanGC = System.currentTimeMillis();
    }

    /**
     * Show the statisitcs
     */
    private void showStats() {
        try {
            double totalMemory   = (double) Runtime.getRuntime().maxMemory();
            double highWaterMark =
                (double) Runtime.getRuntime().totalMemory();
            double freeMemory = (double) Runtime.getRuntime().freeMemory();
            double usedMemory = (highWaterMark - freeMemory);


            int    percent    = (int) (100.0 * (usedMemory / totalMemory));
            totalMemory   = totalMemory / 1000000.0;
            usedMemory    = usedMemory / 1000000.0;
            highWaterMark = highWaterMark / 1000000.0;
            String text;
            memoryLabel = " " + fmt.format(usedMemory) + "/"
                          + fmt.format(highWaterMark) + "/"
                          + fmt.format(totalMemory) + " " + Msg.msg("MB");

            if (showClock) {
                //                g.setFont(clockFont);
                Date d = new Date();
                clockFormat.setTimeZone(GuiUtils.getTimeZone());
                text = "  " + clockFormat.format(d);
                //                text = StringUtil.padLeft(text,20);
            } else {
                text = memoryLabel;
            }
            label1.setText(text);

            //            label2.setText(" (" + percent + "%)  ");

            long now = System.currentTimeMillis();
            if (lastTimeRanGC < 0) {
                lastTimeRanGC = now;
            }

            //For the threshold  use the physical memory
            percent = (int) (100.0 * (usedMemory / totalMemory));
            if (percent > percentThreshold) {
                timesAboveThreshold++;
                if (timesAboveThreshold > 5) {
                    //Only run the GC every 5 seconds
                    if (now - lastTimeRanGC > 5000) {
                        //For now just clear the cache. Don't run the gc
                        //                        System.err.println("clearing cache");
                        CacheManager.clearCache();
                        //                        runGC();
                        lastTimeRanGC = now;
                    }
                    if ( !inTheRed) {
                        setInTheRed(true);
                    }
                }
            } else {
                if (inTheRed) {
                    setInTheRed(false);
                }
                timesAboveThreshold = 0;
                lastTimeRanGC       = now;
            }
        } catch (IllegalStateException ise) {}
    }

    /**
     * Set whether we are in the red or not (above the threshold)
     *
     * @param red
     */
    protected void setInTheRed(boolean red) {
        inTheRed = red;
        if (inTheRed) {
            label2.setForeground(Color.red);
        } else {
            label2.setForeground(labelForeground);
        }
    }

    /**
     * Run this monitor
     */
    public void run() {
        while (running) {
            showStats();
            try {
                thread.sleep(sleepInterval);
            } catch (Exception exc) {}
        }
    }

    /**
     * Set whether we are running
     *
     * @param r  true if we are running
     */
    public void setRunning(boolean r) {
        running = r;
    }

    /**
     * Get whether we are running
     * @return  true if we are
     */
    public boolean getRunning() {
        return running;
    }

    /**
     * Test routine
     *
     * @param args not used
     */
    public static void main(String[] args) {
        JFrame        f  = new JFrame();
        MemoryMonitor mm = new MemoryMonitor();
        f.getContentPane().add(mm);
        f.pack();
        f.show();
    }


}

