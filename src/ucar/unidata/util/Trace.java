/*
 * $Id: Trace.java,v 1.17 2007/06/28 14:07:06 dmurray Exp $
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


import java.awt.*;
import java.awt.event.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.io.PrintStream;




import java.lang.reflect.InvocationTargetException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;


/**
 * Provides for applicatin level call tracing, timing and memory
 * tracing.
 *
 * @author IDV development team
 */


public class Trace {

    /** _more_ */
    private static Hashtable counters = new Hashtable();

    /** _more_ */
    private static List counterList = new ArrayList();

    /** _more_ */
    private static final Object MUTEX = new Object();

    /** _more_ */
    public static boolean displayMsg = false;

    /** _more_ */
    private static boolean showLineNumber = false;


    /** _more_          */
    private static Hashtable accumTable = new Hashtable();

    /** _more_          */
    private static Hashtable accumCntTable = new Hashtable();

    /** _more_          */
    private static Hashtable accum1Table = new Hashtable();

    /** _more_          */
    private static List accumList = new ArrayList();


    /** _more_ */
    private static Hashtable ticks = new Hashtable();

    /** _more_ */
    private static Hashtable mems = new Hashtable();

    /** _more_ */
    private static Hashtable tabs = new Hashtable();

    /** _more_ */
    private static Hashtable traceMsgs = new Hashtable();

    /** _more_ */
    private static String lastThreadName = "";

    /** _more_ */
    private static long initMemory = 0;

    /** _more_ */
    public static long lastMemory = 0;

    /** _more_ */
    public static long lastTime = 0;

    /** _more_ */
    static StringBuffer ts = new StringBuffer();

    /** _more_ */
    static StringBuffer ms = new StringBuffer();

    /** _more_ */
    static StringBuffer tms = new StringBuffer();

    /** _more_ */
    static StringBuffer prefix = new StringBuffer();

    /** _more_ */
    public static StringBuffer buff = new StringBuffer();


    /** _more_ */
    private static List notThese = new ArrayList();

    /** _more_ */
    private static List onlyThese = new ArrayList();

    /**
     * _more_
     *
     * @return _more_
     */
    public static List getNotThese() {
        return notThese;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static List getOnlyThese() {
        return onlyThese;
    }

    /**
     * _more_
     *
     * @param pattern _more_
     */
    public static void addNot(String pattern) {
        notThese.add(pattern);
    }

    /**
     * _more_
     *
     * @param pattern _more_
     */
    public static void addOnly(String pattern) {
        onlyThese.add(pattern);
    }


    /**
     * _more_
     *
     * @param pattern _more_
     */
    public static void removeOnly(String pattern) {
        onlyThese.remove(pattern);
    }

    /**
     * Clear out any of the patterns previously added by the addOnly call
     */
    public static void clearOnly() {
        onlyThese = new ArrayList();
    }


    /**
     * _more_
     * @return _more_
     */
    static StringBuffer getBuffer() {
        Thread       t  = Thread.currentThread();
        StringBuffer sb = (StringBuffer) traceMsgs.get(t);
        if (sb == null) {
            sb = new StringBuffer();
            traceMsgs.put(t, sb);
        }
        return sb;
    }

    /**
     * _more_
     * @return _more_
     */
    static Integer getTab() {
        Thread  t   = Thread.currentThread();
        Integer tab = (Integer) tabs.get(t);
        if (tab == null) {
            tab = new Integer(0);
            tabs.put(t, tab);
        }
        return tab;
    }


    /**
     * _more_
     * @return _more_
     */
    static int getCurrentTab() {
        return getTab().intValue();
    }


    /**
     * _more_
     *
     * @param v _more_
     */
    public static void setShowLineNumbers(boolean v) {
        showLineNumber = v;
    }


    /**
     * _more_
     */
    public static void startTrace() {
        displayMsg = true;
        initMemory = (Runtime.getRuntime().totalMemory()
                      - Runtime.getRuntime().freeMemory());
    }

    /**
     * _more_
     */
    public static void stopTrace() {
        displayMsg = false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static boolean traceActive() {
        return displayMsg;
    }

    /**
     * _more_
     *
     * @param delta
     */
    public static void deltaCurrentTab(int delta) {
        if ( !displayMsg) {
            return;
        }
        int v = getCurrentTab();
        tabs.put(Thread.currentThread(), new Integer(v + delta));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static String[] changeFilters() {
        String    notTheseStr  = StringUtil.join("\n", notThese);
        String    onlyTheseStr = StringUtil.join("\n", onlyThese);
        JTextArea notTheseFld  = new JTextArea(notTheseStr, 10, 50);
        JTextArea onlyTheseFld = new JTextArea(onlyTheseStr, 10, 50);
        JPanel contents =
            GuiUtils.vbox(new JLabel("Not these:"),
                          GuiUtils.makeScrollPane(notTheseFld, 100, 50),
                          new JLabel("Only these:"),
                          GuiUtils.makeScrollPane(onlyTheseFld, 100, 50));

        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "Trace filters", contents,
                                          null)) {
            return null;
        }

        setFilters(notTheseFld.getText().trim(),
                   onlyTheseFld.getText().trim());

        return new String[] { notTheseFld.getText().trim(),
                              onlyTheseFld.getText().trim() };
    }


    /**
     * _more_
     *
     * @param notTheseText _more_
     * @param onlyTheseText _more_
     */
    public static void setFilters(String notTheseText, String onlyTheseText) {
        if (notTheseText != null) {
            notThese = StringUtil.split(notTheseText, "\n", true, true);
        }
        if (onlyTheseText != null) {
            onlyThese = StringUtil.split(onlyTheseText, "\n", true, true);
        }
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    private static boolean ok(String msg) {

        if (notThese.size() > 0) {
            if (StringUtil.findMatch(msg, notThese, null) != null) {
                return false;
            }
        }

        if (onlyThese.size() > 0) {
            if (StringUtil.findMatch(msg, onlyThese, null) == null) {
                return false;
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param m
     */
    public static void call1(String m) {
        call1(m, "", true);
    }

    /**
     * _more_
     *
     * @param m
     * @param print
     */
    public static void call1(String m, boolean print) {
        call1(m, "", print);
    }

    /**
     * _more_
     *
     * @param m
     * @param extra
     */
    public static void call1(String m, String extra) {
        call1(m, extra, true);
    }

    /**
     * _more_
     *
     * @param m
     * @param extra
     * @param print
     */
    public static void call1(String m, String extra, boolean print) {
        if ( !displayMsg) {
            return;
        }

        if ( !ok(m)) {
            return;
        }

        synchronized (MUTEX) {
            if (print) {
                writeTrace(">" + m + " " + extra);
            }
            deltaCurrentTab(1);
            ticks.put(m, new Long(System.currentTimeMillis()));
            mems.put(m, new Long(Misc.usedMemory()));
        }
    }


    /**
     * _more_
     *
     * @param m
     */
    public static void call2(String m) {
        call2(m, "");
    }

    /**
     * _more_
     *
     * @param m
     * @param extra
     */
    public static void call2(String m, String extra) {
        if ( !displayMsg) {
            return;
        }
        if ( !ok(m)) {
            return;
        }
        synchronized (MUTEX) {
            deltaCurrentTab(-1);
            long now        = System.currentTimeMillis();
            Long lastTime   = (Long) ticks.get(m);
            Long lastMemory = (Long) mems.get(m);
            if ((lastTime != null) && (lastMemory != null)) {
                long memDiff = Misc.usedMemory() - lastMemory.longValue();
                long then    = lastTime.longValue();
                writeTrace("<" + m + " ms: " + (now - then) + " " + extra);
                ticks.remove(m);
                mems.remove(m);
            } else {
                writeTrace(m + " NO LAST TIME");
            }
        }
    }



    /**
     * _more_
     */
    public static void clearMsgs() {
        tabs      = new Hashtable();
        traceMsgs = new Hashtable();
    }

    /**
     * _more_
     */
    public static void printMsgs() {
        for (java.util.Enumeration keys = traceMsgs.keys();
                keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            System.out.println(key);
            System.out.println(traceMsgs.get(key));
        }
        clearMsgs();
    }


    /**
     * _more_
     *
     * @param m
     */
    public static void before(String m) {
        if ( !displayMsg) {
            return;
        }
        synchronized (MUTEX) {
            writeTrace(m);
            deltaCurrentTab(1);
        }
    }

    /**
     * _more_
     *
     * @param m
     */
    public static void after(String m) {
        if ( !displayMsg) {
            return;
        }
        synchronized (MUTEX) {
            deltaCurrentTab(-1);
            writeTrace(m);
        }
    }


    /**
     * _more_
     *
     * @param m
     */
    public static void msg(String m) {
        if ( !displayMsg) {
            return;
        }
        if ( !ok(m)) {
            return;
        }
        synchronized (MUTEX) {
            writeTrace(m);
        }
    }

    /**
     * _more_
     *
     * @param msg
     */
    private static void writeTrace(String msg) {
        String suff = "";

        if (showLineNumber) {
            List trace = StringUtil.split(LogUtil.getStackTrace(), "\n",
                                          true, true);
            for (int i = 0; i < trace.size(); i++) {
                //            System.err.println("line:" + trace.get(i));
                String line = (String) trace.get(i);
                if ((line.indexOf("(") >= 0)
                        && (line.indexOf("LogUtil.getStackTrace") < 0)
                        && (line.indexOf("Trace.java") < 0)
                        && (line.indexOf("Method") < 0)) {
                    line = line.substring(line.indexOf("(") + 1,
                                          line.length() - 1);
                    suff = "   " + line;
                    break;
                }
            }
        }


        Thread t              = Thread.currentThread();
        String crntThreadName = t.getName();
        if ( !crntThreadName.equals(lastThreadName)) {
            System.out.println("Thread:" + crntThreadName);
            lastThreadName = crntThreadName;
        }
        //      StringBuffer sb = getBuffer ();
        //      printTabs (sb);
        printTabs(null);
        System.out.print(msg + suff + "\n");
        LogUtil.consoleMessage(msg);
        //      sb.append (msg+"\n");
    }



    /**
     * _more_
     *
     * @param sb
     */
    private static void printTabs(StringBuffer sb) {
        if ( !displayMsg) {
            return;
        }
        int tabs = getCurrentTab();
        long usedMemory2 = (Runtime.getRuntime().totalMemory()
                            - Runtime.getRuntime().freeMemory());
        if (initMemory == 0) {
            initMemory = usedMemory2;
        }

        long currentTime = System.currentTimeMillis();

        ts.setLength(0);
        ms.setLength(0);
        tms.setLength(0);
        prefix.setLength(0);
        ts.append((currentTime - lastTime));
        while (ts.length() < 4) {
            ts.append(" ");
        }

        ms.append((int) ((usedMemory2 - lastMemory) / 1000.0));
        while (ms.length() < 5) {
            ms.append(" ");
        }

        //      tms.append  ((int)((usedMemory2-initMemory)/1000.0));
        //      while (tms.length ()<5) {
        //          tms.append (" ");
        //      }

        if (lastTime == 0) {
            prefix.append("S   D     T");
        } else {
            //      prefix=ts+" "+ms+" "+tms+" ";
            prefix.append(ts.toString());
            prefix.append(" ");
            prefix.append(ms.toString());
            prefix.append(" ");
        }

        while (prefix.length() < 10) {
            prefix.append(" ");
        }


        System.out.print(prefix.toString());
        if (sb != null) {
            sb.append(prefix.toString());
        }

        for (int i = 0; i < tabs; i++) {
            if (sb != null) {
                sb.append("  ");
            }
            System.out.print("  ");
        }


        lastTime = currentTime;
        lastMemory = (Runtime.getRuntime().totalMemory()
                      - Runtime.getRuntime().freeMemory());
    }






    /**
     * _more_
     *
     * @param name _more_
     */
    public static void accum1(String name) {
        if ( !displayMsg) {
            return;
        }
        //        Long l = new Long(System.currentTimeMillis());
        Long l = new Long(System.nanoTime());
        accum1Table.put(name, l);
    }

    /**
     * _more_
     *
     * @param name _more_
     */
    public static void accum2(String name) {
        if ( !displayMsg) {
            return;
        }
        //        long time = System.currentTimeMillis();
        long time = System.nanoTime();
        Long l    = (Long) accum1Table.get(name);
        if (l == null) {
            msg("Cannot find accum:" + name);
            return;
        }
        long delta = time - l.longValue();
        Long total = (Long) accumTable.get(name);
        if (total == null) {
            total = new Long(delta);
            accumList.add(name);
        } else {
            total = new Long(total.longValue() + delta);
        }
        Integer cnt = (Integer) accumCntTable.get(name);
        if (cnt == null) {
            cnt = new Integer(1);
        } else {
            cnt = new Integer(cnt.intValue() + 1);
        }
        accumCntTable.put(name, cnt);
        accumTable.put(name, total);
    }


    /**
     * _more_
     */
    public static void printAccum() {
        for (int i = 0; i < accumList.size(); i++) {
            String  name  = (String) accumList.get(i);
            Long    total = (Long) accumTable.get(name);
            Integer cnt   = (Integer) accumCntTable.get(name);
            long nanos = total.longValue();
            msg(name + " Time:" + (nanos/1000000) + " count:" + cnt);
        }
        accumList     = new ArrayList();
        accum1Table   = new Hashtable();
        accumCntTable = new Hashtable();
        accumTable    = new Hashtable();
    }


    /**
     * _more_
     *
     * @param name
     */
    public static void count(String name) {
        Integer i = (Integer) counters.get(name);
        if (i == null) {
            i = new Integer(0);
            counters.put(name, i);
            counterList.add(name);
        }
        i = new Integer(i.intValue() + 1);
        counters.put(name, i);
    }


    /**
     * _more_
     */
    public static void printAndClearCount() {
        for (int i = 0; i < counterList.size(); i++) {
            String  name     = (String) counterList.get(i);
            Integer theCount = (Integer) counters.get(name);
            System.out.println("Count:" + name + "=" + theCount);
        }
        counterList = new ArrayList();
        counters    = new Hashtable();
    }



}

