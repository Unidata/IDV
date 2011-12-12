/*
 * $Id: JobManager.java,v 1.13 2007/03/23 14:04:52 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


import java.awt.*;
import java.awt.event.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


import javax.swing.*;
import javax.swing.event.*;


/**
 * Static class of miscellaneous methods
 *
 * @author IDV development group.
 *
 * @version $Revision: 1.13 $
 */
public class JobManager {

    //TODO: implements visad.VisADJobManager {

    /** _more_ */
    private static JobManager jobManager;

    /** _more_ */
    private static Object MUTEX = new Object();

    /** _more_ */
    private static Hashtable loadMap = new Hashtable();

    /** _more_          */
    private static Hashtable dialogMap = new Hashtable();

    private static Hashtable stoppedLoads = new Hashtable();

    /** count of objects */
    private static int objectCount = 0;


    /**
     * _more_
     *
     * @return _more_
     */
    public static JobManager getManager() {
        if (jobManager == null) {
            jobManager = new JobManager();
        }
        return jobManager;
    }


    /**
     * _more_
     *
     * @param jobId _more_
     * @param msg _more_
     */
    public void setDialogLabel1(Object jobId, String msg) {
        if (jobId == null) {
            return;
        }
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if (dialogInfo != null) {
            dialogInfo.label1.setText(msg);
        }
    }


    /**
     * _more_
     *
     * @param jobId _more_
     * @param msg _more_
     */
    public void setDialogLabel2(Object jobId, String msg) {
        if (jobId == null) {
            return;
        }
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if (dialogInfo != null) {
            dialogInfo.label2.setText(msg);
        }
    }


    /**
     * _more_
     *
     * @param jobId _more_
     *
     * @return _more_
     */
    public String getDialogLabel2(Object jobId) {
        if (jobId == null) {
            return null;
        }
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if (dialogInfo != null) {
            return dialogInfo.label2.getText();
        }
        return null;
    }



    /**
     * Class DialogInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class DialogInfo {

        /** _more_          */
        JDialog dialog;

        /** _more_          */
        JLabel label1;

        /** _more_          */
        JLabel label2;

        /** _more_          */
        Object jobId;

        boolean modal;

        String name;


        /**
         * _more_
         *
         * @param id _more_
         * @param name _more_
         * @param modal _more_
         */
        public DialogInfo(Object id, String name, boolean modal) {
            jobId  = id;
            this.modal = modal;
            this.name = name;
            label1 = new JLabel("                                   ");
            label2 = new JLabel("                                   ");
        }

        /**
         * _more_
         */
        public void showDialog() {
            getDialog().show();
        }

        public JDialog getDialog() {
            if(dialog == null) {
                dialog = GuiUtils.createDialog(null, name, modal);
                JButton cancelBtn = new JButton("Cancel");
                cancelBtn.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            stopLoad(jobId);
                            dialog.dispose();
                        }
                    });
                JLabel waitLbl =
                    new JLabel(
                               new ImageIcon(
                                             GuiUtils.getImage(
                                                               "/ucar/unidata/idv/images/wait.gif")));
                JComponent contents = LayoutUtil.vbox(
                                                      LayoutUtil.hbox(
                                                                      new JLabel(name),
                                                                      LayoutUtil.inset(
                                                                                       waitLbl, 5)), LayoutUtil.vbox(
                                                                                                                     LayoutUtil.filler(300, 5),
                                                                                                                     label1,
                                                                                                                     label2), LayoutUtil.wrap(
                                                                                                                                              LayoutUtil.inset(
                                                                                                                                                               cancelBtn, 10)));

                dialog.getContentPane().add(LayoutUtil.inset(contents, 5));
                dialog.pack();
                GuiUtils.packInCenter(dialog);
            }
            return dialog;
        }



    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public Object startLoad(String name) {
        return startLoad(name, false);
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param showDialog _more_
     *
     * @return _more_
     */
    public Object startLoad(String name, boolean showDialog) {
        return startLoad(name, showDialog, false);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param showDialog _more_
     * @param modal _more_
     *
     * @return _more_
     */
    public Object startLoad(final String name, final boolean showDialog,
                            final boolean modal) {
        synchronized (MUTEX) {
            final Object id = new Integer(++objectCount);
            loadMap.put(id, name);
            Misc.run(new Runnable() {
                public void run() {
                    DialogInfo dialogInfo = null;
                    synchronized (MUTEX) {
                        if(loadMap.get(id)==null) return;
                        dialogInfo = new DialogInfo(id, name, modal);
                        //If we're going to shwo the dialog then create it here in the MUTEX
                        //to stop a race condition when we want to stop the run
                        if(showDialog) dialogInfo.getDialog();
                        dialogMap.put(id, dialogInfo);
                    }
                    if (showDialog) {
                        dialogInfo.showDialog();
                    }
                }
            });
            return id;
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param id _more_
     */
    public void startLoad(final String name, final Object id) {
        synchronized (MUTEX) {
            loadMap.put(id, name);
            dialogMap.put(id, new DialogInfo(id, name, false));
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean canContinue(Object id) {
        if (id == null) {
            return true;
        }
        synchronized (MUTEX) {
            return loadMap.get(id) != null;
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     *
     * @return _more_
     */
    public Object stopAndRestart(Object id, String name) {
        synchronized (MUTEX) {
            stopLoad(id);
            return startLoad(name);
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public void stopLoad(Object id) {
        if (id == null) {
            return;
        }
        synchronized (MUTEX) {
            Object a = loadMap.get(id);
            if (a == null) {
                return;
            }
            loadMap.remove(id);
            DialogInfo dialogInfo = (DialogInfo) dialogMap.get(id);
            dialogMap.remove(id);
            if ((dialogInfo != null) && (dialogInfo.dialog != null)) {
                dialogInfo.dialog.dispose();
            }
        }
    }

    /**
     * _more_
     */
    public void stopAllLoads() {
        synchronized (MUTEX) {
            Enumeration keys = loadMap.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                stopLoad(key);
            }
        }
    }


}

