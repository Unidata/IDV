/*
 * $Id: PollingInfo.java,v 1.24 2006/07/26 21:42:38 jeffmc Exp $
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

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;


/**
 * Class PollingInfo Holds the state that controls the FilePoller
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.24 $
 */
public class PollingInfo implements Cloneable {

    /** polling mode */
    public static final int MODE_NONEWFILES = 0;

    /** polling mode */
    public static final int MODE_COUNT = 1;

    /** polling mode */
    public static final int MODE_RELDATERANGE = 2;

    /** polling mode */
    public static final int MODE_ABSDATERANGE = 3;

    /** polling mode */
    public static final int[] MODES = {
        //      MODE_NONEWFILES,
        MODE_COUNT, MODE_RELDATERANGE, MODE_ABSDATERANGE,
    };

    /** polling mode */
    public static final String[] MODELABELS = {
        //      "Check for Changed File",
        "Last N Files in Directory", "Date Range from Youngest File",
        "Date Range from Now",
    };

    /** polling mode */
    private int mode = MODE_NONEWFILES;

    /** The name */
    private String name;


    /** Is this for files */
    private boolean forFiles = true;

    /** Polling interval time. Milliseconds */
    private long interval = 1000 * 60 * 10;

    /** Where to look. Either is a file or a directory */
    private List filePaths = new ArrayList();

    /** The file pattern. May be null */
    private String filePattern;

    /** Should we be polling */
    private boolean isActive = false;

    /** Should we include hidden files */
    private boolean isHiddenOk = true;

    /** how many files */
    private int fileCount = 0;

    /** _more_ */
    private long dateRange = 3600 * 1000;


    /** Widget for properties dialog */
    private JCheckBox activeWidget;

    /** widget for properties */
    private JTextField patternWidget;

    /** widget for properties */
    private JTextField intervalWidget;

    /** widget for properties */
    private JTextField filePathWidget;

    /** widget for properties */
    JRadioButton fileCountButton;

    /** widget for properties */
    JRadioButton dateRangeButton;

    /** widget for properties */
    private JComboBox fileCountWidget;

    /** widget for properties */
    private JTextField dateRangeWidget;

    /** widget for properties */
    private JCheckBox hiddenWidget;

    /** widget for properties */
    private JTextField nameWidget;



    /**
     * Constructor
     *
     */
    public PollingInfo() {}

    /**
     * Clone me
     *
     * @return Cloned version of this
     */
    public PollingInfo cloneMe() {
        try {
            PollingInfo newPollingInfo = (PollingInfo) clone();
            newPollingInfo.initFromCloning();
            return newPollingInfo;
        } catch (CloneNotSupportedException exc) {
            throw new IllegalStateException("Bad clone:" + exc);
        }
    }


    /**
     * _more_
     */
    private void initFromCloning() {
        activeWidget    = null;
        patternWidget   = null;
        intervalWidget  = null;
        filePathWidget  = null;
        fileCountButton = null;
        dateRangeButton = null;
        fileCountWidget = null;
        dateRangeWidget = null;
        hiddenWidget    = null;
        nameWidget      = null;
    }

    /**
     * Constructor
     *
     * @param isActive The isActive parameter
     *
     */
    public PollingInfo(boolean isActive) {
        this.isActive = isActive;
    }


    /**
     * Constructor
     *
     * @param interval The interval parameter
     * @param isActive The isActive parameter
     *
     */
    public PollingInfo(long interval, boolean isActive) {
        this(isActive);
        this.interval = interval;
    }


    /**
     * Constructor
     *
     * @param filePath The filePath parameter
     * @param interval The interval parameter
     * @param filePattern The filePattern parameter
     * @param isActive The isActive parameter
     *
     */
    public PollingInfo(String filePath, long interval, String filePattern,
                       boolean isActive) {
        this(filePath, interval, filePattern, isActive, true);
    }


    /**
     * Constructor
     *
     * @param filePath The filePath parameter
     * @param interval The interval parameter
     * @param filePattern The filePattern parameter
     * @param isActive The isActive parameter
     * @param isHiddenOk  poll on hidden files as well
     */
    public PollingInfo(String filePath, long interval, String filePattern,
                       boolean isActive, boolean isHiddenOk) {
        this(interval, isActive);
        if (filePath != null) {
            filePaths.add(filePath);
        }
        this.filePattern = filePattern;
        this.isHiddenOk  = isHiddenOk;
    }

    /**
     * _more_
     */
    public void setDontLookForNewFiles() {
        mode = MODE_NONEWFILES;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean doILookForNewFiles() {
        return mode != MODE_NONEWFILES;
    }

    /**
     * _more_
     *
     * @param comps _more_
     * @param includeName _more_
     * @param includeFileCount _more_
     */
    public void getPropertyComponents(List comps, boolean includeName,
                                      boolean includeFileCount) {
        JButton directoryBtn = new JButton("Select");
        boolean isFile       = false;
        if (getFile() != null) {
            File f = new File(getFile());
            isFile = f.isFile();
        }
        GuiUtils.setupFileChooser(directoryBtn, getFilePathWidget(), !isFile);
        if (isFile) {
            comps.add(GuiUtils.rLabel("File: "));
        } else {
            comps.add(GuiUtils.rLabel("Directory: "));
        }
        comps.add(
            GuiUtils.left(
                GuiUtils.centerRight(
                    GuiUtils.wrap(getFilePathWidget()),
                    GuiUtils.inset(directoryBtn, new Insets(0, 5, 0, 0)))));

        if (includeName) {
            comps.add(GuiUtils.rLabel("Name: "));
            comps.add(GuiUtils.left(getNameWidget()));
        }

        comps.add(GuiUtils.rLabel("File Pattern:"));
        comps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.wrap(
                        GuiUtils.hbox(
                            getPatternWidget(),
                            getHiddenWidget())), GuiUtils.makeButton(
                                "Verify", this, "checkPattern"))));


        if (includeFileCount) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    checkModeEnabled();
                }
            };
            comps.add(GuiUtils.rLabel("Files:"));
            fileCountButton = new JRadioButton("", mode == MODE_COUNT);
            dateRangeButton = new JRadioButton("All files in last:",
                    mode == MODE_ABSDATERANGE);
            fileCountButton.addActionListener(actionListener);
            dateRangeButton.addActionListener(actionListener);
            ButtonGroup bg = GuiUtils.buttonGroup(fileCountButton,
                                 dateRangeButton);
            List modeComps = new ArrayList();
            modeComps.add(fileCountButton);
            modeComps.add(getFileCountWidget());
            modeComps.add(new JLabel("  "));
            modeComps.add(dateRangeButton);
            modeComps.add(getDateRangeWidget());
            modeComps.add(new JLabel(" minutes"));
            checkModeEnabled();
            comps.add(GuiUtils.left(GuiUtils.hbox(modeComps)));
        }

        comps.add(GuiUtils.rLabel("Polling:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(getActiveWidget(),
                GuiUtils.lLabel("     Check every: "),
                GuiUtils.wrap(getIntervalWidget()),
                GuiUtils.lLabel(" minutes"))));
    }


    /**
     * _more_
     */
    private void checkModeEnabled() {
        getDateRangeWidget().setEnabled( !fileCountButton.isSelected());
        getFileCountWidget().setEnabled(fileCountButton.isSelected());
    }

    /**
     * _more_
     */
    public void checkPattern() {
        String pattern = getPatternWidget().getText().trim();
        File   dir     = new File(getFilePathWidget().getText().trim());
        File[] list =
            dir.listFiles((java.io.FileFilter) new PatternFileFilter(pattern,
                false, getHiddenWidget().isSelected()));
        StringBuffer sb = new StringBuffer("<html>");
        if ((list == null) || (list.length == 0)) {
            sb.append(
                "<h3><center><font color=\"red\">No files match the pattern</font></center></h3>");
            sb.append("Pattern: " + pattern + "<br>");
            list = dir.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (i == 0) {
                        sb.append("<hr>Example Files:<br>");
                    }
                    if (i > 20) {
                        sb.append("&nbsp;&nbsp;...<br>\n");
                        break;
                    }
                    sb.append("&nbsp;&nbsp;" + list[i] + "<br>\n");
                }
            }
        } else {
            for (int i = 0; i < list.length; i++) {
                if (i == 0) {
                    sb.append("<h3><center>Files that match</center></h3>\n");
                }
                if (i > 20) {
                    sb.append("&nbsp;&nbsp;...<br>\n");
                    break;
                }
                sb.append("&nbsp;&nbsp;" + list[i] + "<br>\n");
            }
        }
        sb.append("</html>");
        GuiUtils.showDialog("Sample files",
                            GuiUtils.inset(new JLabel(sb.toString()), 5));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JCheckBox getHiddenWidget() {
        if (hiddenWidget == null) {
            hiddenWidget = new JCheckBox("Include Hidden Files", isHiddenOk);
        }
        return hiddenWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JCheckBox getActiveWidget() {
        if (activeWidget == null) {
            activeWidget = new JCheckBox("Active", isActive);
        }
        return activeWidget;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JTextField getPatternWidget() {
        if (patternWidget == null) {
            patternWidget = new JTextField(((filePattern != null)
                                            ? filePattern
                                            : ""), 10);
        }
        return patternWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JTextField getIntervalWidget() {
        if (intervalWidget == null) {
            intervalWidget = new JTextField("" + (interval
                    / (double) 60000.0), 5);
        }
        return intervalWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JTextField getFilePathWidget() {
        if (filePathWidget == null) {
            String filePath = getFile();
            filePathWidget = new JTextField(((filePath != null)
                                             ? filePath
                                             : ""), 30);
        }
        return filePathWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JTextField getNameWidget() {
        if (nameWidget == null) {
            nameWidget = new JTextField(((name != null)
                                         ? name
                                         : ""), 30);
        }
        return nameWidget;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JTextField getDateRangeWidget() {
        if (dateRangeWidget == null) {
            dateRangeWidget = new JTextField("" + (dateRange / (1000 * 60)),
                                             5);
        }
        return dateRangeWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComboBox getFileCountWidget() {
        int[] values = {
            Integer.MAX_VALUE, 1, 2, 3, 4, 5, 6, 8, 10, 15, 20,
        };
        return getFileCountWidget(values);
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public JComboBox getFileCountWidget(int[] values) {
        if (fileCountWidget == null) {
            TwoFacedObject selected = null;
            Vector         items    = new Vector();
            for (int i = 0; i < values.length; i++) {
                String label;
                if (values[i] == Integer.MAX_VALUE) {
                    label = "All Files";
                } else if (values[i] == 0) {
                    label = "Use Selected File";
                } else if (values[i] == 1) {
                    label = "Use Most Recent File";
                } else {
                    label = "Use Most Recent " + values[i] + " Files";
                }
                TwoFacedObject tfo = new TwoFacedObject(label,
                                         new Integer(values[i]));
                if (values[i] == fileCount) {
                    selected = tfo;
                }
                items.add(tfo);
            }
            fileCountWidget = new JComboBox(items);
            if (selected == null) {
                selected = new TwoFacedObject("Use Most Recent " + fileCount
                        + " Files", new Integer(fileCount));
            }
            fileCountWidget.setSelectedItem(selected);
        }
        return fileCountWidget;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isADirectory() {
        if (filePaths.size() == 1) {
            File f = new File(filePaths.get(0).toString());
            return f.isDirectory();
        }
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasFiles() {
        return filePaths.size() > 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean applyProperties() {
        if (dateRangeButton != null) {
            if (dateRangeButton.isSelected()) {
                mode = MODE_ABSDATERANGE;
            } else {
                mode = MODE_COUNT;
            }
        }


        isHiddenOk = getHiddenWidget().isSelected();
        fileCount =
            ((Integer) ((TwoFacedObject) getFileCountWidget()
                .getSelectedItem()).getId()).intValue();
        setFilePath(getFilePathWidget().getText().trim());
        setFilePattern(getPatternWidget().getText().trim());
        setName(getNameWidget().getText().trim());
        setIsActive(getActiveWidget().isSelected());
        try {
            setInterval((long) (new Double(getIntervalWidget().getText()
                .trim()).doubleValue() * 60000));
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad number format:"
                                     + getIntervalWidget().getText());
            return false;
        }
        try {
            setDateRange((long) (new Double(getDateRangeWidget().getText()
                .trim()).doubleValue() * 60000));
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad number format:"
                                     + getDateRangeWidget().getText());
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getFiles() {
        List   files    = new ArrayList();
        String filePath = getFile();
        File   file     = new File(filePath);
        if ( !file.isDirectory() || !doILookForNewFiles()) {
            if (file.exists()) {
                files.add(file.toString());
            }
            return files;
        }
        String pattern = filePattern;
        if (pattern == null) {
            pattern = ".*";
        }
        File[] list = file.listFiles(
                          (java.io.FileFilter) new PatternFileFilter(
                              pattern, false, isHiddenOk));
        if (list == null) {
            return files;
        }
        list = IOUtil.getNormalFiles(list);
        if (list.length == 0) {
            return files;
        }
        if (list.length == 1) {
            files.add(list[0].toString());
            return files;
        }
        list = IOUtil.sortFilesOnAge(list, true);
        if (mode == MODE_COUNT) {
            for (int i = 0; (i < list.length) && (files.size() < fileCount);
                    i++) {
                files.add(0, list[i].toString());
            }
        } else {
            long endDate;
            if (mode == MODE_ABSDATERANGE) {
                endDate = Misc.getCurrentTime();
            } else {
                endDate = list[0].lastModified();
            }
            long startDate = endDate - dateRange;
            for (int i = 0; i < list.length; i++) {
                if (list[i].lastModified() < startDate) {
                    break;
                }
                files.add(0, list[i].toString());
            }
        }
        return files;
    }


    /**
     * Get the interval property.
     *
     * @return The interval property.
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Set the interval property.
     *
     * @param newValue The new vaue for the  interval property.
     */
    public void setInterval(long newValue) {
        interval = newValue;
    }

    /**
     * Get the filePath property.
     *
     * @return The filePath property.
     */
    public String getFile() {
        if (filePaths.size() > 0) {
            return (String) filePaths.get(0);
        }
        return null;
    }

    /**
     * Set the filePath property.
     *
     * @param newValue The new vaue for the  filePath property.
     * @deprecated Use setFilePaths
     */
    public void setFilePath(String newValue) {
        filePaths = Misc.newList(newValue);
    }

    /**
     * Get the filePattern property.
     *
     * @return The filePattern property.
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasName() {
        return (name != null) && (name.trim().length() > 0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasFilePattern() {
        return (filePattern != null) && (filePattern.trim().length() > 0);
    }

    /**
     * Set the filePattern property.
     *
     * @param newValue The new vaue for the  filePattern property.
     */
    public void setFilePattern(String newValue) {
        filePattern = newValue;
    }

    /**
     * Get the isActive property.
     *
     * @return The isActive property.
     */
    public boolean getIsActive() {
        return isActive;
    }

    /**
     * Set the isActive property.
     *
     * @param newValue The new vaue for the  isActive property.
     */
    public void setIsActive(boolean newValue) {
        isActive = newValue;
    }

    /**
     * Get the isHiddenOk property.
     *
     * @return The isHiddenOk property.
     */
    public boolean getIsHiddenOk() {
        return isHiddenOk;
    }

    /**
     * Set the isHiddenOk property.
     *
     * @param newValue The new vaue for the  isHiddenOk property.
     */
    public void setIsHiddenOk(boolean newValue) {
        isHiddenOk = newValue;
    }

    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Interval: ");
        buf.append(interval);
        buf.append(" ms, paths: ");
        buf.append(filePaths);
        buf.append(", pattern: ");
        buf.append(filePattern);
        buf.append(", active: ");
        buf.append(isActive);
        buf.append(", hidden OK?: ");
        buf.append(isHiddenOk);
        return buf.toString();
    }


    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if ( !(object instanceof PollingInfo)) {
            return false;
        }
        PollingInfo that = (PollingInfo) object;
        return (this.interval == that.interval)
               && (this.fileCount == that.fileCount)
               && (this.forFiles == that.forFiles)
               && (this.isActive == that.isActive)
               && (this.isHiddenOk == that.isHiddenOk)
               && Misc.equals(this.filePattern, that.filePattern)
               && Misc.equals(this.filePaths, that.filePaths);
    }

    /**
     * Set the ForFiles property.
     *
     * @param value The new value for ForFiles
     */
    public void setForFiles(boolean value) {
        forFiles = value;
    }

    /**
     * Get the ForFiles property.
     *
     * @return The ForFiles
     */
    public boolean getForFiles() {
        return forFiles;
    }


    /**
     *  Set the Mode property.
     *
     *  @param value The new value for Mode
     */
    public void setMode(int value) {
        mode = value;
    }

    /**
     *  Get the Mode property.
     *
     *  @return The Mode
     */
    public int getMode() {
        return mode;
    }

    /**
     *  Set the FileCount property.
     *
     *  @param value The new value for FileCount
     */
    public void setFileCount(int value) {
        fileCount = value;
    }

    /**
     *  Get the FileCount property.
     *
     *  @return The FileCount
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     *  Set the DateRange property.
     *
     *  @param value The new value for DateRange
     */
    public void setDateRange(long value) {
        dateRange = value;
    }

    /**
     *  Get the DateRange property.
     *
     *  @return The DateRange
     */
    public long getDateRange() {
        return dateRange;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                "Usage: PollingInfo <file> <filepattern>");
        }

        /*
        PollingInfo pollingInfo = new PollingInfo(args[0], (args.length>1?args[1]:(String)null));
        pollingInfo.setFileCount(3);
        pollingInfo.setMode(MODE_RELDATERANGE);
        System.err.println ("files:" + pollingInfo.getFiles());
        */

    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the FilePaths property.
     *
     * @param value The new value for FilePaths
     */
    public void setFilePaths(List value) {
        filePaths = value;
    }

    /**
     * Get the FilePaths property.
     *
     * @return The FilePaths
     */
    public List getFilePaths() {
        return filePaths;
    }



}

