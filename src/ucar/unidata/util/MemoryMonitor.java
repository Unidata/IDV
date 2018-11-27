/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 * Class MemoryMonitor
 *
 *
 * @author Unidata development team
 *
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

    /** Keep track of the last time we cleared the cache */
    private static long lastTimeClearCache = -1;


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
        label1.setToolTipText("Used memory/Max memory");
        label2.setToolTipText("Used memory/Max memory");
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


        popup.add(GuiUtils.makeMenuItem("Clear Cache", MemoryMonitor.this,
                                        "clearCache"));
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
     * Clear the cache
     */
    public void clearCache() {
        CacheManager.clearCache();
    }

    /**
     * Convert byte size into human-readable format in Java
     *
     * @param bytes : number of bytes to convert
     * @param useBaseTwoUnits : true: binary units (base 2), false: use base 10
     * @return String with value and unit
     *
     * modified based on StackOverflow:
     * http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
     *
     */

    private static String humanReadableByteCount(long bytes,
            boolean useBaseTwoUnits) {
        int unit = useBaseTwoUnits
                   ? 1000
                   : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int    exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (useBaseTwoUnits
                      ? "KMGTPE"
                      : "kMGTPE").charAt(exp - 1) + (useBaseTwoUnits
                ? "i"
                : "");
        String humanReadable;
        if ( !pre.toLowerCase().contains("k") && !pre.contains("M")) {
            humanReadable = String.format("%.3f %sB",
                                          bytes / Math.pow(unit, exp), pre);
        } else {
            humanReadable = String.format("%.1f %sB",
                                          bytes / Math.pow(unit, exp), pre);
        }
        return humanReadable;
    }

    /**
     * Show the statisitcs
     */
    private void showStats() {
        try {
            long maxTotalMemory = Runtime.getRuntime().maxMemory();
            long currentAllocatedMemory = Runtime.getRuntime().totalMemory();
            long currentFreeAllocatedMemory =
                Runtime.getRuntime().freeMemory();
            long currentUsedMemory = (currentAllocatedMemory
                                      - currentFreeAllocatedMemory);

            int percent = (int) (100.0
                                 * (currentUsedMemory / maxTotalMemory));

            String text;

            memoryLabel = humanReadableByteCount(currentUsedMemory, false)
                          + "/"
                          + humanReadableByteCount(maxTotalMemory, false);
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
            if (lastTimeClearCache < 0) {
                lastTimeClearCache = now;
            }

            //For the threshold  use the physical memory
            percent = (int) (100.0 * (currentUsedMemory / maxTotalMemory));
            if (percent > percentThreshold) {
                timesAboveThreshold++;
                if (timesAboveThreshold > 5) {
                    //Only run the GC every 5 seconds
                    if (now - lastTimeClearCache > 5000) {
                        CacheManager.clearCache();
                        lastTimeClearCache = now;
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
                lastTimeClearCache  = now;
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
        f.setVisible(true);
    }


}
