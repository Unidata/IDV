/*
 * $Id: Poller.java,v 1.17 2006/05/05 19:19:36 jeffmc Exp $
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



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



/**
 * Class to handle polling.
 *
 * @author Metapps development team
 *
 * @version $Revision: 1.17 $ $Date: 2006/05/05 19:19:36 $
 */
public abstract class Poller implements Runnable {

    /** Debug flag */
    public static boolean debug = false;


    /** listener for polling events */
    protected ActionListener listener;

    /** interval for polling (milliseconds) */
    protected long interval;

    /** flag for running */
    protected boolean running = true;


    /**
     * Create a Poller
     *
     * @param interval    polling interval
     *
     */
    public Poller(long interval) {
        this.interval = interval;
    }




    /**
     * Create a Poller
     *
     * @param listener    listener for events
     * @param interval    polling interval
     *
     */
    public Poller(ActionListener listener, long interval) {
        this(interval);
        this.listener = listener;
    }


    /**
     * Initialize.
     */
    public void init() {
        Misc.run(this);
    }



    /**
     * Run the poller
     */
    public void run() {
        if (debug) {
            System.err.println("Poller.run interval=" + interval);
        }
        int subTime = 5000;
        //Sleep  first then poll
        //        System.err.println("Poller.run" + " interval:" + interval);
        while (running) {
            //We will loop and sleep for 5 seconds and keep checking whether we are running
            long timeSlept = 0;
            while (timeSlept < interval-subTime && running) {
                Misc.sleep(subTime);
                timeSlept += subTime;
                //                System.err.println("    running:" + running+ " time slept:" + timeSlept );
            }

            if (running && timeSlept < interval) {
                //                System.err.println ("    sleeping a bit longer: "+ (interval - timeSlept));
                Misc.sleep(interval - timeSlept);
            }
            if (!running) {
                break;
            }


            if (debug) {
                //                System.err.println("doPoll");
            }
            //            System.err.println ("polling");
            doPoll();
        }
        listener = null;
    }


    /**
     * This method does the work.  Subclasses implement what they
     * want.
     */
    protected abstract void doPoll();

    /**
     * Stop polling.
     */
    public void stopRunning() {
        if (debug) {
            System.err.println("Poller.stopRunning");
        }
        listener = null;
        running  = false;
    }

    /**
     * Get the polling interval
     *
     * @return the interval (milliseconds)
     */
    public long getInterval() {
        return interval;
    }
}

