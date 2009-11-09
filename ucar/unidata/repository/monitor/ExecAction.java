/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository.monitor;


import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class ExecAction extends MonitorAction {

    /** _more_ */
    public static final String PROP_EXEC_EXECLINE = "exec.execline";

    /** _more_ */
    private String execLine;


    /**
     * _more_
     */
    public ExecAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public ExecAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "Exec Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Execute external program on server";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.execLine = request.getString(getArgId(PROP_EXEC_EXECLINE), "");
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     */
    public void addToEditForm(EntryMonitor monitor, StringBuffer sb) {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.colspan("Exec Action", 2));

        sb.append(
            HtmlUtil.formEntry(
                "Execute:",
                HtmlUtil.input(
                    getArgId(PROP_EXEC_EXECLINE), execLine,
                    HtmlUtil.SIZE_60) + HtmlUtil.title(macroTooltip)));
        sb.append(HtmlUtil.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
	if(!monitor.getRepository().getProperty(PROP_MONITOR_ENABLE_EXEC,false)) {
	    throw new IllegalArgumentException("Exec action not enabled");
	}
        Resource resource = entry.getResource();
        String command =
            monitor.getRepository().getEntryManager().replaceMacros(entry,
                execLine);
        try {
            Process process = Runtime.getRuntime().exec(command);
            int     result  = process.waitFor();
            if (result == 0) {
                monitor.getRepository().getLogManager().logInfo(
                    "ExecMonitor executed:" + command);
            } else {
                try {
                    InputStream is    = process.getErrorStream();
                    byte[]      bytes = IOUtil.readBytes(is);
                    monitor.getRepository().getLogManager().logError(
                        "ExecMonitor failed executing:" + command + "\n"
                        + new String(bytes));
                } catch (Exception noop) {
                    monitor.getRepository().getLogManager().logError(
                        "ExecMonitor failed:" + command);
                }
            }
        } catch (Exception exc) {
            monitor.handleError("Error execing monitor", exc);
        }
    }

    /**
     * Set the ExecLine property.
     *
     * @param value The new value for ExecLine
     */
    public void setExecLine(String value) {
        execLine = value;
    }

    /**
     * Get the ExecLine property.
     *
     * @return The ExecLine
     */
    public String getExecLine() {
        return execLine;
    }



}

