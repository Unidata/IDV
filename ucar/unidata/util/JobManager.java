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


import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


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

    private static Hashtable dialogMap = new Hashtable();


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
     * @param name _more_
     *
     * @return _more_
     */
    public Object startLoad(String name) {
        return startLoad(name, false);
    }

    public void setDialogLabel1(Object jobId, String msg) {
        if(jobId==null) return;
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if(dialogInfo!=null) {
            dialogInfo.label1.setText(msg); 
        }
    }


    public void setDialogLabel2(Object jobId, String msg) {
        if(jobId==null) return;
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if(dialogInfo!=null) {
            dialogInfo.label2.setText(msg); 
        }
    }


    public String getDialogLabel2(Object jobId) {
        if(jobId==null) return null;
        DialogInfo dialogInfo = (DialogInfo) dialogMap.get(jobId);
        if(dialogInfo!=null) {
            return dialogInfo.label2.getText();
        }
        return null;
    }



    private class DialogInfo {
        JDialog dialog;
        JLabel label1;
        JLabel label2;
        Object jobId;
        public DialogInfo(Object id, String name, boolean showDialog) {
            jobId = id;
            dialog =  GuiUtils.createDialog(null, name, false);
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        stopLoad(jobId);
                        dialog.dispose();
                    }
                });
            JLabel         waitLbl        = new JLabel(new ImageIcon(GuiUtils.getImage("/ucar/unidata/idv/images/wait.gif")));
            label1 = new JLabel("                                   ");
            label2 = new JLabel("                                   ");
            if(showDialog) {
                JComponent contents = LayoutUtil.vbox(LayoutUtil.hbox(new JLabel(name), LayoutUtil.inset(waitLbl,5)),
                                                      LayoutUtil.vbox(LayoutUtil.filler(300,5),
                                                                      label1,
                                                                      label2),
                                                      LayoutUtil.wrap(LayoutUtil.inset(cancelBtn,10)));

                dialog.getContentPane().add(LayoutUtil.inset(contents,5));
                dialog.pack();
                GuiUtils.packInCenter(dialog);
                dialog.show();
            }
        }
    }

    public Object startLoad(String name, boolean showDialog) {
        synchronized (MUTEX) {
            final Object id = new Integer(++objectCount);
            loadMap.put(id, name);
            dialogMap.put(id, new DialogInfo(id, name,showDialog));
            return id;
        }
    }


    public void startLoad(String name, Object id) {
        synchronized (MUTEX) {
            loadMap.put(id, name);
            dialogMap.put(id, new DialogInfo(id, name,false));
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
            if(dialogInfo!=null && dialogInfo.dialog!=null) {
                dialogInfo.dialog.dispose();
                dialogMap.remove(id);
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

