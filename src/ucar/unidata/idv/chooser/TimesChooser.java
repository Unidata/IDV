/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;

import ucar.nc2.units.DateUnit;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DataSource;

import ucar.unidata.data.imagery.AddeImageDescriptor;


import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.ui.IdvTimeline;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.ui.Timeline;

import ucar.unidata.util.*;

import ucar.visad.Util;
import ucar.visad.display.Animation;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;



/**
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.12 $
 */
public class TimesChooser extends IdvChooser {

    /** flag for relative times range */
    private static final int TIMES_RELATIVERANGE = 0;

    /** flag for absolute times */
    private static final int TIMES_ABSOLUTE = 1;

    /** flag for relative times */
    private static final int TIMES_RELATIVE = 2;


    /** Message for selecting times */
    protected static final String MSG_TIMES =
        "Please select one or more times";

    /** Label for times */
    protected static final String LABEL_TIMES = "Times:";


    /** flag for ignoring combobox changes */
    private boolean ignoreTimeChangedEvents = false;

    /** _more_ */
    private int ignoreCnt = 0;


    /** relative times list */
    private ChooserList relTimesList;

    /** _more_ */
    private boolean usingTimeline = false;

    /** The timeline we popup */
    private IdvTimeline popupTimeline;

    /** The in gui timeline */
    private IdvTimeline timeline;

    /** _more_ */
    private JTabbedPane timesTab;

    /** _more_ */
    private GuiUtils.CardLayoutPanel timesCardPanel;

    /** times container */
    protected JComponent timesContainer;

    /** _more_ */
    private JLabel absTimesLbl = new JLabel("  ");

    /** times list */
    private ChooserList timesList;

    /** List of current absolute times */
    private List absoluteTimes = new ArrayList();

    /** _more_ */
    private int[] currentSelectedAbsoluteTimes;

    /** Keep track of when are are doing absolute times */
    private boolean doAbsoluteTimes = false;

    /** default times mode */
    private int timesMode = TIMES_RELATIVE;


    /** _more_ */
    protected List timesComponents = new ArrayList();

    /** _more_ */
    private List driverMenuList = new ArrayList();

    /** _more_ */
    private List timeDrivers = new ArrayList();

    /** _more_ */
    private boolean doTimeDrivers = false;

    /** _more_          */
    JComponent centerPopup;

    /** _more_          */
    JPopupMenu driverPopupMenu;

    /** _more_          */
    JLabel driverLbl = new JLabel("Click to Select Time Matching  ");

    /** _more_ */
    private boolean readingDrivers = false;

    /**
     * Create me.
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public TimesChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        setTimeDrivers(new ArrayList());
    }


    /**
     * Are there any times in the times list.
     *
     * @return Do we have any times at all.
     */
    protected boolean haveAnyTimes() {
        if (timesList == null) {
            return false;
        }
        return timesList.getModel().getSize() > 0;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean checkIgnore() {
        if (ignoreTimeChangedEvents) {
            return true;
        }
        pushIgnore();
        return false;
    }


    /**
     * _more_
     */
    private void pushIgnore() {
        ignoreCnt++;
        ignoreTimeChangedEvents = true;
    }

    /**
     * _more_
     */
    private void popIgnore() {
        ignoreCnt--;
        if (ignoreCnt <= 0) {
            ignoreTimeChangedEvents = false;
            ignoreCnt               = 0;
        }
    }



    /**
     * Create (if needed) and return the list that shows times.
     *
     * @return The times list.
     */
    public ChooserList getTimesList() {
        if (timesList == null) {
            timesList = new ChooserList(getAbsoluteTimeSelectMode());
            timesComponents.add(timesList);
            timesList.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        doTimeDrivers = false;
                        selectedDriver = null;
                        driverLbl.setText("Click to Select Time Matching");

                    }
                }
            });
            timesList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (checkIgnore()) {
                        return;
                    }
                    List items =
                        Misc.toList(getTimesList().getSelectedValues());

                    setSelectedAbsoluteTimes(items);

                    absoluteTimesSelectionChanged();
                    if ((items.size() > 0) && usingTimeline) {
                        items = DatedObject.sort(items, true);
                        Date startDate =
                            ((DatedThing) items.get(0)).getDate();
                        Date endDate = ((DatedThing) items.get(items.size()
                                           - 1)).getDate();
                        if (timeline.getUseDateSelection()) {
                            timeline.getDateSelection().setStartFixedTime(
                                startDate);
                            timeline.getDateSelection().setEndFixedTime(
                                endDate);
                        }

                        long visStart = timeline.getStartDate().getTime();
                        long visEnd   = timeline.getEndDate().getTime();
                        long selStart = startDate.getTime();
                        long selEnd   = endDate.getTime();
                        long width    = visEnd - visStart;
                        if (items.size() >= 2) {
                            //If we have more than one and if the start or end time is not shown then set the visible range
                            //to the selected start/end time and expand
                            if ((selStart < visStart) || (selStart > visEnd)
                                    || (selEnd < selStart)
                                    || (selEnd > visEnd)) {
                                timeline.setStartDate(new Date(selStart));
                                timeline.setEndDate(new Date(selEnd));
                                timeline.expandByPercent(2.0, false);
                            }
                        } else if ((selStart < visStart)
                                   || (selStart > visEnd)) {
                            //Here we just have one selected time
                            timeline.setStartDate(new Date(selStart
                                    - width / 2));
                            timeline.setEndDate(new Date(selStart
                                    + width / 2));
                            //                            timeline.expandByPercent(1.2, false);
                        }
                    }
                    updateStatus();
                    popIgnore();

                }
            });
        }
        return timesList;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public JPopupMenu getTimeDriverPopupMenu() {


        return driverPopupMenu;
    }

    /**
     * Get the selection mode for the absolute times panel. Subclasses
     * can override.
     *
     * @return select mode
     */
    protected int getAbsoluteTimeSelectMode() {
        return ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    }

    /**
     * Create (if needed) and return the list that shows times.
     *
     * @return The times list.
     */
    public ChooserList getRelativeTimesList() {
        if (relTimesList == null) {
            relTimesList = new ChooserList();
            relTimesList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
            //            GuiUtils.configureStepSelection(relTimesList);
            timesComponents.add(relTimesList);


            Vector items = new Vector();
            /*
            for (int i = 0; i < 50; i++) {
                if (i == 0) {
                    items.add("Most recent");
                } else if (i < StringUtil.ordinalNames.length) {
                    items.add(StringUtil.ordinalNames[i] + " most recent");
                } else {
                    items.add((i + 1) + "th most recent");
                }
            }
            items = new Vector();
            */
            for (int i = 0; i < 100; i++) {
                if (i == 0) {
                    items.add("Most recent");
                } else {
                    items.add((i + 1) + " most recent");
                }
            }
            relTimesList.setListData(items);

            //relTimesList.addSelectionInterval(0,getDefaultRelativeTimeIndex());
            relTimesList.setSelectedIndex(getDefaultRelativeTimeIndex());

            relTimesList.addListSelectionListener(
                new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateStatus();
                }
            });
            relTimesList.ensureIndexIsVisible(getDefaultRelativeTimeIndex());


        }
        return relTimesList;
    }




    /**
     * Clear all times in the times list.
     */
    protected void clearTimesList() {
        setAbsoluteTimes(new ArrayList());
    }


    /**
     *  Do what needs to be done to read in the times.  Subclasses
     *  need to implement this.  This is public by implementation only
     */
    public void readTimes() {}

    /**
     * _more_
     */
    public void readDrivers() {
        if (readingDrivers) {
            return;
        }
        readingDrivers = true;
        List drivers = new ArrayList();
        //   if (getDoTimeDrivers()) {
        // ucar.unidata.util.Trace.call1("TDSRadarChooser.readDrivers");

        try {
            showWaitCursor();
            setTimeDrivers(new ArrayList());
            setStatus("Reading drivers ...");
            drivers = updateTimeDriver();
            showNormalCursor();
        } catch (Exception exc) {
            userMessage("Error reading drivers... ");
            showNormalCursor();
            readingDrivers = false;
            return;
        }

        // ucar.unidata.util.Trace.call2("TDSRadarChooser.readDrivers");
        //  }
        readingDrivers = false;
        setTimeDrivers(drivers);

    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List updateTimeDriver() {
        //GuiUtils.enableTree(timesPanel, !getTimeDriverEnabled());

        List<ViewManager>    vms = getIdv().getVMManager().getViewManagers();
        List<TwoFacedObject> driverNames = new ArrayList<TwoFacedObject>();

        for (ViewManager vm : vms) {
            for (DisplayControl control :
                    (List<DisplayControl>) vm.getControls()) {
                if (control.getIsTimeDriver()) {
                    try {
                        visad.Set timeSet = control.getTimeSet();
                        DateTime[] driverTimes =
                            Animation.getDateTimeArray(timeSet);
                        List dslist = new ArrayList();
                        control.getDataChoice().getDataSources(dslist);
                        DataSource ds    = (DataSource) dslist.get(0);
                        String     lable = ds.getName();
                        TwoFacedObject twoObj = new TwoFacedObject(lable,
                                                    driverTimes);
                        //   control.getDataChoice().getId());
                        driverNames.add(twoObj);
                    } catch (Exception e) {}

                }
            }
        }
        return driverNames;

    }

    /**
     * Create the absolute/relative times selector
     *
     * @return  the image list panel
     */
    protected JPanel makeTimesPanel() {
        return makeTimesPanel(true);
    }



    /**
     * Handle when the absolute times selection has changed
     */
    protected void absoluteTimesSelectionChanged() {
        updateStatus();
    }

    /**
     * Make the times panel
     *
     * @param includeExtra   true if including extra time component
     *
     * @return  the time selection panel
     */
    protected JPanel makeTimesPanel(boolean includeExtra) {
        return makeTimesPanel(includeExtra, true);
    }

    /**
     * _more_
     */
    protected void updateStatus() {
        super.updateStatus();
        if (doAbsoluteTimes) {
            if ((currentSelectedAbsoluteTimes == null)
                    || (currentSelectedAbsoluteTimes.length == 0)) {
                absTimesLbl.setText(" No times selected");
            } else if (currentSelectedAbsoluteTimes.length == 1) {
                absTimesLbl.setText(" 1 time selected");
            } else {
                absTimesLbl.setText(" " + currentSelectedAbsoluteTimes.length
                                    + " times selected");
            }

            if (doTimeDrivers) {
                driverLbl.setText("Time Matching to "
                                  + selectedDriver.getLabel());
            } else {

                selectedDriver = null;
                driverLbl.setText("Click to Select Time Matching");
            }
        } else {
            absTimesLbl.setText(" ");
        }
    }

    /**
     *  _more_
     *
     *  @param includeExtra _more_
     *  @param useTimeLine _more_
     *
     *  @return _more_
     */
    protected JPanel makeTimesPanel(boolean includeExtra,
                                    boolean useTimeLine) {
        if(getIdv().getUseTimeDriver())
            return makeTimesPanel(includeExtra, useTimeLine, true);
        else
            return makeTimesPanel(includeExtra, useTimeLine, false);
    }

    /**
     * _more_
     *
     * @param includeExtra _more_
     * @param useTimeLine _more_
     * @param doDriverTab _more_
     *
     * @return _more_
     */
    protected JPanel makeTimesPanel(boolean includeExtra,
                                    boolean useTimeLine,
                                    boolean doDriverTab) {

        pushIgnore();
        JButton timelineBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Calendar16.gif",
                                     this, "popupTimeline");
        timelineBtn.setToolTipText("Select times in timeline");


        timeline = new IdvTimeline(new ArrayList(), 200) {
            public List getSunriseLocations() {
                return getIdv().getIdvUIManager().getMapLocations();
            }

            public void selectedDatesChanged() {
                super.selectedDatesChanged();
                if (checkIgnore()) {
                    return;
                }
                setSelectedAbsoluteTimes(timeline.getSelected());
                updateStatus();
                absoluteTimesSelectionChanged();
                popIgnore();
            }
        };

        timeline.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                   doTimeDrivers = false;
                }
            }
        });
        //        timeline.setUseDateSelection(false);
        timeline.setUseDateSelection(true);

        Date toDate = new Date(System.currentTimeMillis()
                               + DateUtil.daysToMillis(1));
        Date fromDate = new Date(System.currentTimeMillis()
                                 - DateUtil.daysToMillis(1));
        timeline.setRange(fromDate, toDate, true);
        timeline.setSticky(false);
        //        timeline.setSticky(true);
        DateSelection dateSelection =
            new DateSelection(timeline.getStartDate(), timeline.getEndDate());
        timeline.setDateSelection(dateSelection);


        ChooserList    timesList = getTimesList();
        ChangeListener listener  = new ChangeListener() {
            public void stateChanged(ChangeEvent ae) {
                boolean currentAbs = (timesTab.getSelectedIndex() == 1);
                if (currentAbs == getDoAbsoluteTimes()) {
                    return;
                }
                doAbsoluteTimes = currentAbs;
                if (doAbsoluteTimes && !haveAnyTimes()) {
                    Misc.run(TimesChooser.this, "readTimes");
                }

                updateStatus();
                enableWidgets();
            }
        };

        final JButton centerPopupBtn =
            GuiUtils.getImageButton("/auxdata/ui/icons/MapIcon16.png",
                                    getClass());
        centerPopupBtn.setToolTipText(
            "Select a time driver display to do the time matching");
        centerPopupBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(TimesChooser.this, "readDrivers");
            }
        });

        centerPopupBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {

                    JPopupMenu popup = GuiUtils.makePopupMenu(driverMenuList);
                    popup.show(centerPopupBtn, e.getX(),
                               (int) popup.getBounds().getHeight());
                }
            }
        });

        JComponent timesExtra    = getExtraTimeComponent();
        JComponent absoluteExtra = getExtraAbsoluteTimeComponent();
        JComponent relativeExtra = getExtraRelativeTimeComponent();
        timesCardPanel     = new GuiUtils.CardLayoutPanel();
        timesContainer     = GuiUtils.center(timesCardPanel);
        timesTab           = GuiUtils.getNestedTabbedPane();
        this.usingTimeline = useTimeLine;
        timesTab.add(
            "Relative",
            GuiUtils.centerBottom(
                getRelativeTimesList().getScroller(), relativeExtra));
        if (useTimeLine) {
            //                timesTab.add("Timeline", timeline.getContents(false));
            timesList.getScroller().setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            //Shrink down the font a bit
            Font f = timesList.getFont();
            f = f.deriveFont(f.getSize() - 2f);
            timesList.setFont(f);
            JSplitPane splitter =
                GuiUtils.hsplit(timeline.getContents(false),
                                timesList.getScroller(), 0.75);
            splitter.setOneTouchExpandable(true);


            if (doDriverTab) {
                JPanel tDriver =
                GuiUtils.left(GuiUtils.hflow(Misc.newList(new Component[] {
                    driverLbl,
                    centerPopupBtn }), 2, 1));
                timesTab.add("Absolute",
                             GuiUtils.centerBottom(splitter,
                                 GuiUtils.leftRight(absTimesLbl, tDriver)));
            } else {

                timesTab.add("Absolute",
                             GuiUtils.centerBottom(splitter,
                                 GuiUtils.leftRight(absTimesLbl,
                                     absoluteExtra)));
            }
            //  popup.show(driverPopupBtn, 0, 1);


            timesTab.setPreferredSize(new Dimension(350, 150));
        } else {
            timesTab.add("Absolute",
                         GuiUtils.centerBottom(timesList.getScroller(),
                             absoluteExtra));
            timesTab.setPreferredSize(new Dimension(350, 150));
        }


        JPanel panel;
        if (includeExtra) {
            panel = GuiUtils.centerBottom(timesTab,
                                          GuiUtils.left(timesExtra));
        } else {
            panel = GuiUtils.center(timesTab);
        }

        timesTab.addChangeListener(listener);
        popIgnore();
        return panel;
    }



    /**
     * Enable the absolute times list
     *
     * @param enabled enabled
     */
    protected void enableAbsoluteTimesList(boolean enabled) {
        if (usingTimeline) {
            timeline.setEnabled(enabled);
        }
        getTimesList().setEnabled(enabled);
    }

    /**
     * Set the absolute times list. The times list can contain any of the object types
     * that makeDatedObjects knows how to handle, i.e., Date, visad.DateTime, DatedThing, AddeImageDescriptor, etc.
     *
     * @param times List of thinggs to put into absolute times list
     */
    protected void setAbsoluteTimes(List times) {
        List newAbsoluteTimes = makeDatedObjects(times);
        if (Misc.equals(absoluteTimes, newAbsoluteTimes)) {
            return;
        }
        absoluteTimes = newAbsoluteTimes;
        pushIgnore();
        getTimesList().setListData(new Vector(absoluteTimes));
        getTimesList().ensureIndexIsVisible(absoluteTimes.size() - 1);
        setSelectedAbsoluteTimes(new ArrayList());
        if (usingTimeline) {
            try {
                timeline.setDatedThings(absoluteTimes, true);
                Date endDate, startDate;
                if (absoluteTimes.size() > 1) {
                    //Go back N times
                    endDate =
                        ((DatedThing) absoluteTimes.get(absoluteTimes.size()
                            - 1)).getDate();
                    int index = Math.max(0, absoluteTimes.size()
                                         - getNumTimesToSelect());
                    startDate =
                        ((DatedThing) absoluteTimes.get(index)).getDate();
                } else if (absoluteTimes.size() == 1) {
                    DatedThing theDate = (DatedThing) absoluteTimes.get(0);
                    endDate = new Date(theDate.getDate().getTime()
                                       + DateUtil.daysToMillis(1));
                    startDate = new Date(theDate.getDate().getTime()
                                         - DateUtil.daysToMillis(1));
                } else {
                    startDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(1));
                    endDate = new Date(System.currentTimeMillis());
                }

                if (timeline.getDateSelection() == null) {
                    DateSelection dateSelection =
                        new DateSelection(startDate, endDate);
                    timeline.setDateSelection(dateSelection);
                } else {
                    timeline.getDateSelection().setStartFixedTime(startDate);
                    timeline.getDateSelection().setEndFixedTime(endDate);
                }

                if (absoluteTimes.size() >= 2) {
                    //Go back 2 times the autoselection size
                    endDate =
                        ((DatedThing) absoluteTimes.get(absoluteTimes.size()
                            - 1)).getDate();
                    int index = Math.max(0, absoluteTimes.size()
                                         - getNumTimesToSelect() * 2);
                    startDate =
                        ((DatedThing) absoluteTimes.get(index)).getDate();

                } else if (absoluteTimes.size() != 1) {
                    startDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(1));
                    endDate = new Date(System.currentTimeMillis());
                }


                //Use 2.5 times the selected date range
                long diff = endDate.getTime() - startDate.getTime();
                startDate = new Date(endDate.getTime() - (long) 2.5 * diff);
                timeline.setRange(startDate, endDate, true);
                //Zoom out a little bit
                timeline.expandByPercent(1.2, false);
                //Make this the default range (for doing a reset)
                timeline.makeCurrentRangeOriginal();

                //Calling this applys the dateselection
                if (timeline.getUseDateSelection()) {
                    timeline.setUseDateSelection(true);
                    setSelectedAbsoluteTimes(timeline.getSelected());
                    updateStatus();
                } else {}
            } catch (Exception exc) {
                logException("Setting times", exc);
            }
        }
        popIgnore();
    }

    /**
     * _more_
     *
     * @param drivers _more_
     */

    protected void setTimeDrivers(List drivers) {

        int size = drivers.size();
        driverMenuList = new ArrayList();
        JMenuItem menuItem = new JMenuItem("Select a Matching Display");
        driverMenuList.add(menuItem);
        for (int i = 0; i < size; i++) {
            TwoFacedObject driver = (TwoFacedObject) drivers.get(i);

            JMenuItem ji = GuiUtils.makeMenuItem((String) driver.getLabel(),
                               TimesChooser.this, "updatetimeline", driver);
            driverMenuList.add(ji);
        }

    }

    /** _more_          */
    protected TwoFacedObject selectedDriver = null;

    /**
     * _more_
     *
     * @param id _more_
     */
    public void updatetimeline(TwoFacedObject id) {
        doTimeDrivers  = true;
        selectedDriver = id;

        DateTime[] driverTimes = (DateTime[]) id.getId();
        List<DateTime> dtimes = new ArrayList<DateTime>();

        for (int i = 0; i < driverTimes.length; i++) {
            dtimes.add(driverTimes[i]);
        }
        Collections.sort(dtimes);
        int n = driverTimes.length;


        List absTimes = null;
        List allTimes = new ArrayList();
        List selectedTimes   = null;

        try {
            absTimes = makeDatedObjects(getAbsoluteTimes());

            for(Object od : absTimes){
                Date de = ((DatedObject)od).getDate();
                 allTimes.add(new DateTime(de));
            }
            selectedTimes = DataUtil.selectTimesFromList(allTimes,
                    dtimes);

            List newAbsoluteTimes = makeDatedObjects(selectedTimes);

            int size = newAbsoluteTimes.size();
            Date endDate, startDate;
            if(size >= 1) {
                endDate =
                        ((DatedThing) newAbsoluteTimes.get(size-1)).getDate();
                int index = Math.max(0, size
                                     - getNumTimesToSelect());
                startDate =
                    ((DatedThing) newAbsoluteTimes.get(index)).getDate();
                getTimeLine().setDateSelection(new Date[]{startDate, endDate});

            }
            int[] indices  = new int[selectedTimes.size()];
            for (int i = 0; i < selectedTimes.size(); i++) {
                if(newAbsoluteTimes.get(i) instanceof DatedThing && allTimes.get(i) instanceof DatedThing ){
                    System.out.println("dd");
                }
                indices[i] = allTimes.indexOf(selectedTimes.get(i));
            }
            setSelectedAbsoluteTimes(indices);
            setDoTimeDrivers(true);
            absoluteTimesSelectionChanged();

        } catch (Exception e) {}

    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected int getNumTimesToSelect() {
        return 5;
    }


    /**
     * Any absolute times selected
     *
     * @return Any absolute times selected
     */
    protected boolean getHaveAbsoluteTimesSelected() {
        if (usingTimeline) {
            return timeline.getSelected().size() > 0;
        }
        return getTimesList().haveDataSelected();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Timeline getTimeLine() {

        return timeline;
    }

    /**
     * Get the list of all absolute times. This returns the list of objects that was
     * passed in from setAbsoluteTimes. What is really held by the timesList and the timeLines
     * is a list of DatedObjects that holds the objects that are passed in.
     *
     * @return all absolute times
     */
    protected List getAbsoluteTimes() {
        return DatedObject.getObjects(absoluteTimes);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected List getTimeDrivers() {
        return timeDrivers;
    }

    /**
     * Get selected absolute times
     *
     * @return selected absolute times
     */
    protected List getSelectedAbsoluteTimes() {
        if (usingTimeline) {
            return DatedObject.getObjects(timeline.getSelected());
        } else {
            return DatedObject.getObjects(
                Misc.toList(getTimesList().getSelectedValues()));
        }
    }

    /**
     * Set the selected indices in the absolute times list
     *
     *
     * @param selectedTimes List of selected times
     */
    protected void setSelectedAbsoluteTimes(List selectedTimes) {
        selectedTimes = makeDatedObjects(selectedTimes);
        List  allTimes = absoluteTimes;
        int[] indices  = new int[selectedTimes.size()];
        for (int i = 0; i < selectedTimes.size(); i++) {
            indices[i] = allTimes.indexOf(selectedTimes.get(i));
        }

        setSelectedAbsoluteTimes(indices);
    }


    /**
     * Set the selected indices in the absolute times list
     *
     * @param indices selected indices
     */
    protected void setSelectedAbsoluteTimes(int[] indices) {
        if (Misc.arraysEquals(currentSelectedAbsoluteTimes, indices)) {
            return;
        }
        currentSelectedAbsoluteTimes = indices;


        pushIgnore();
        if (usingTimeline) {
            List selected = new ArrayList();
            for (int i = 0; i < indices.length; i++) {
                if ((indices[i] >= 0)
                        && (indices[i] < absoluteTimes.size())) {
                    selected.add(absoluteTimes.get(indices[i]));
                }
            }
            timeline.setSelected(selected);
        }
        if ( !Misc.arraysEquals(getTimesList().getSelectedIndices(),
                                indices)) {
            getTimesList().setSelectedIndices(indices);
            int selectedIndex = getTimesList().getSelectedIndex();
            if (selectedIndex >= 0) {
                getTimesList().ensureIndexIsVisible(selectedIndex);
            }
        }
        if ((currentSelectedAbsoluteTimes == null)
                || (currentSelectedAbsoluteTimes.length == 0)) {
            absTimesLbl.setText(" No times selected");
        } else if (currentSelectedAbsoluteTimes.length == 1) {
            absTimesLbl.setText(" 1 time selected");
        } else {
            absTimesLbl.setText(" " + currentSelectedAbsoluteTimes.length
                                + " times selected");
        }

        if (doTimeDrivers) {
            driverLbl.setText("Time Matching to "
                              + selectedDriver.getLabel());
        } else {

            selectedDriver = null;
            driverLbl.setText("Click to Select Time Matching");
        }
        popIgnore();
    }


    /**
     * Set the selected index
     *
     * @param selectedIndex selected index
     */
    protected void setSelectedAbsoluteTime(int selectedIndex) {
        setSelectedAbsoluteTimes(new int[] { selectedIndex });
    }


    /**
     * Set range of selected times
     *
     * @param from from index
     * @param to to index
     */
    protected void setSelectedAbsoluteTime(int from, int to) {
        if (from > to) {
            return;
        }
        int[] indices = new int[to - from + 1];
        int   cnt     = 0;
        for (int i = from; i <= to; i++) {
            indices[cnt++] = i;
        }
        setSelectedAbsoluteTimes(indices);
    }



    /**
     * Utility to wrap the given items as DatedThings
     *
     * @param items List of items. Might be a DatedThing, an AddeImageDescriptor or a DateTime
     *
     * @return List of DatedThings
     *
     */
    protected List<DatedThing> makeDatedObjects(List items) {
        List<DatedThing> datedThings = new ArrayList<DatedThing>();
        try {
            for (int i = 0; i < items.size(); i++) {
                Object object = items.get(i);
                Date   date   = null;
                if (object instanceof DatedThing) {
                    return items;
                    //                    date = ((DatedThing) object).getDate();
                } else if (object instanceof AddeImageDescriptor) {
                    AddeImageDescriptor aid  = (AddeImageDescriptor) object;
                    DateTime            dttm = aid.getImageTime();
                    if (dttm == null) {
                        continue;
                    }
                    date = new Date(
                        (long) dttm.getValue(CommonUnit.secondsSinceTheEpoch)
                        * 1000);
                } else if (object instanceof DateTime) {
                    DateTime dttm = (DateTime) object;
                    date = new Date(
                        (long) dttm.getValue(CommonUnit.secondsSinceTheEpoch)
                        * 1000);
                }
                if (date == null) {
                    continue;
                }
                DatedThing datedThing = new DatedObject(date, object);
                datedThings.add(datedThing);
            }
        } catch (Exception exc) {
            logException("Making list of dated things", exc);
        }
        return DatedObject.sort(datedThings, true);
    }



    /**
     * Show the timeline
     *
     * @throws Exception On badness
     */
    public void popupTimeline() throws Exception {
        List datedObjects = absoluteTimes;
        if (datedObjects.size() == 0) {
            LogUtil.userMessage("No times in list");
            return;
        }
        List selected      = new ArrayList();
        List selectedTimes = getSelectedAbsoluteTimes();
        for (int i = 0; i < datedObjects.size(); i++) {
            DatedObject datedObject = (DatedObject) datedObjects.get(i);
            if (selectedTimes.contains(datedObject.getObject())) {
                selected.add(datedObject);
            }
        }

        if (popupTimeline == null) {
            popupTimeline = new IdvTimeline(datedObjects, 400);
            popupTimeline.setUseDateSelection(false);
            DateSelection dateSelection =
                new DateSelection(popupTimeline.getStartDate(),
                                  popupTimeline.getEndDate());
            popupTimeline.setDateSelection(dateSelection);
        } else {
            popupTimeline.setDatedThings(datedObjects, true);
        }

        DatedThing endDate   = null;
        DatedThing startDate = null;
        if (selected.size() > 0) {
            List tmp = DatedObject.sort(selected, true);
            endDate = (DatedThing) tmp.get(tmp.size() - 1);
            if (selected.size() == 1) {
                int index = datedObjects.indexOf(endDate);
                if (index >= 0) {
                    index     = Math.max(0, index - 10);
                    startDate = (DatedThing) datedObjects.get(index);
                }
            } else {
                startDate = (DatedThing) tmp.get(0);
            }
        } else if (datedObjects.size() > 0) {
            endDate = (DatedThing) datedObjects.get(datedObjects.size() - 1);
            int index = Math.max(0, datedObjects.size() - 10);
            startDate = (DatedThing) datedObjects.get(index);
        }
        if ((endDate != null) && (startDate != null)) {
            popupTimeline.setStartDate(startDate.getDate());
            popupTimeline.setEndDate(endDate.getDate());
            popupTimeline.expandByPercent(1.2, false);
            popupTimeline.makeCurrentRangeOriginal();
        }
        popupTimeline.setSelected(selected);
        if ( !popupTimeline.popup()) {
            return;
        }
        selected = popupTimeline.getSelected();
        int[] selectedIndices = new int[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            DatedObject datedObject = (DatedObject) selected.get(i);
            selectedIndices[i] = datedObjects.indexOf(datedObject);
        }
        setSelectedAbsoluteTimes(selectedIndices);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getExtraAbsoluteTimeComponent() {
        return new JPanel();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getExtraRelativeTimeComponent() {
        return new JPanel();
    }



    /**
     * Get the extra time widget.  Subclasses can add their own time
     * widgets.
     *
     * @return a widget that can be selected for more options
     */
    protected JComponent getExtraTimeComponent() {
        JComponent timesExtra;
        if (false) {
            final JButton timesExtraBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/clock.gif",
                                        getClass());
            timesExtraBtn.setToolTipText("Use times from displays");
            timesExtraBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    popupTimesMenu(timesExtraBtn);
                }
            });
            timesExtra = timesExtraBtn;
        } else {
            timesExtra = new JPanel();
        }
        return timesExtra;
    }

    /**
     * Popup a times menu to access the times from displays
     *
     * @param near  component to popup near
     */
    private void popupTimesMenu(JComponent near) {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    DateTime[] times = (DateTime[]) ae.getSource();
                    setSelectedTimes(times);
                } catch (Exception exc) {
                    logException("Setting times", exc);
                }
            }
        };
        List menuItems = makeTimeMenus(listener);
        if (menuItems.size() == 0) {
            menuItems.add(new JMenuItem("No times in displays"));
        }
        GuiUtils.showPopupMenu(menuItems, near);
    }

    /**
     * Set the selected times in the list based on the input times.
     * This is a NOOP - subclasses should implement
     *
     * @param times  times to use for sampling
     */
    protected void setSelectedTimes(DateTime[] times) {}


    /**
     * Get the default selected index for the relative times list.
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected int getDefaultTimeDriverIndex() {
        return 0;
    }

    /**
     * Are there any times selected.
     *
     * @return Any times selected.
     */
    protected boolean haveTimeSelected() {
        return !getDoAbsoluteTimes() || getHaveAbsoluteTimesSelected();
    }

    /**
     * Get the increment between times for relative time requests.
     * Subclasse need to override this.
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement() {
        return 1;
    }


    /**
     * Create (if needed) and return the list that shows times.
     *
     * @return The times list.
     */
    public JComponent getRelativeTimesChooser() {
        return getRelativeTimesList().getScroller();
    }


    /**
     * Get the relative time indices
     *
     * @return an array of indices
     */
    public int[] getRelativeTimeIndices() {
        int   count   = getRelativeTimesList().getSelectedIndex() + 1;
        int[] indices = new int[count];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        return indices;
        //        return getRelativeTimesList().getSelectedIndices();
    }


    /**
     * Do we do absolute or relative times
     *
     * @return Do we do absolute times
     */
    protected boolean getDoAbsoluteTimes() {
        return doAbsoluteTimes;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean getDoTimeDrivers() {
        return doTimeDrivers;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    protected void setDoTimeDrivers(boolean value) {
        doTimeDrivers = value;
    }

    /**
     * Set whether we do absolute or relative times
     *
     * @param yesorno true to do absolute times
     */
    protected void setDoAbsoluteTimes(boolean yesorno) {
        doAbsoluteTimes = yesorno;
        // Should this be in 
        if (timesTab != null) {
            if (yesorno) {
                timesTab.setSelectedIndex(1);
            } else {
                timesTab.setSelectedIndex(0);
            }
        }
    }

    /**
     * Did the user select relative times?
     *
     * @return Should we load relative times
     */
    protected boolean getDoRelativeTimes() {
        return !getDoAbsoluteTimes();
    }

    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        checkTimesLists();
    }

    /**
     * Check the times lists
     */
    protected void checkTimesLists() {
        if (timesCardPanel == null) {
            return;
        }
        if (getDoAbsoluteTimes()) {
            timesCardPanel.show("absolute");
        } else {
            timesCardPanel.show("relative");
        }


    }




    /**
     * Add a listener to the JList that pops up a menu on a right
     * click that allos for the selection of different strides.
     *
     * @param list list to popup on
     * @param timeline _more_
     */
    public static void addTimeSelectionListener(final JList list,
            final Timeline timeline) {
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupTimeSelection(e, list, timeline);
                }
            }
        });
        if (list.getToolTipText() == null) {
            list.setToolTipText(
                "Right mouse to show range select popup menu");
        }

    }

    /**
     * popup a menu to select strides
     *
     * @param e mouse click
     * @param list JList
     * @param timeline _more_
     */
    private static void popupTimeSelection(MouseEvent e, final JList list,
                                           final Timeline timeline) {
        if ( !list.isEnabled()) {
            return;
        }
        List items = new ArrayList();
        List dates = GuiUtils.getItems(list);
        if ((dates.size() > 0) && (dates.get(0) instanceof DateTime)) {
            JMenuItem menuItem = new JMenuItem("Show Timeline");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Misc.runInABit(1, new Runnable() {
                        public void run() {
                            showTimelineDialog(list, timeline);
                        }
                    });

                }
            });
            items.add(menuItem);
        }
        GuiUtils.getConfigureStepSelectionItems(list, items);
        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        popup.show(list, e.getX(), e.getY());
    }


    /**
     * _more_
     *
     * @param list _more_
     * @param timeline _more_
     */
    private static void showTimelineDialog(JList list, Timeline timeline) {
        try {
            if (timeline == null) {
                timeline = new Timeline();
            }
            List     selected = new ArrayList();
            Object[] tmp      = list.getSelectedValues();
            for (int i = 0; i < tmp.length; i++) {
                selected.add(
                    new DatedObject(Util.makeDate((DateTime) tmp[i])));
            }

            final JDialog dialog =
                GuiUtils.createDialog(GuiUtils.getWindow(list), "", true);
            dialog.setUndecorated(true);
            final boolean[] ok          = { false };
            List            dates       = GuiUtils.getItems(list);
            List            datedThings = new ArrayList();
            for (DateTime dttm : (List<DateTime>) dates) {
                datedThings.add(new DatedObject(Util.makeDate(dttm)));
            }

            timeline.setDatedThings(datedThings, true);
            DateSelection dateSelection;
            if (selected.size() == dates.size()) {
                dateSelection = new DateSelection(timeline.getStartDate(),
                        timeline.getEndDate());
            } else if (selected.size() == 0) {
                Date end = new Date(timeline.getStartDate().getTime()
                                    + 1000 * 60 * 60);
                dateSelection = new DateSelection(timeline.getStartDate(),
                        end);

            } else {
                dateSelection = new DateSelection(
                    ((DatedObject) selected.get(0)).getDate(),
                    ((DatedObject) selected.get(
                        selected.size() - 1)).getDate());

            }



            timeline.setSelected(selected);
            timeline.setUseDateSelection(true);
            timeline.setDateSelection(dateSelection);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (ae.getActionCommand().equals(GuiUtils.CMD_OK)) {
                        ok[0] = true;
                    }
                    dialog.dispose();
                }
            };
            JComponent buttons  = GuiUtils.makeOkCancelButtons(listener);
            JComponent contents = GuiUtils.centerBottom(timeline, buttons);
            buttons.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                    Color.black));
            contents.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                    Color.black));
            dialog.getContentPane().add(contents);

            dialog.pack();
            Point loc = list.getLocationOnScreen();
            //            loc.y += timelineBtn.getSize().height;
            dialog.setLocation(loc);
            GuiUtils.positionAndFitToScreen(dialog, dialog.getBounds());
            dialog.show();
            if (ok[0]) {
                selected = new ArrayList();
                for (Date dttm :
                        (List<Date>) DatedObject.unwrap(
                            timeline.getSelected())) {
                    selected.add(new DateTime(dttm));
                }
                if (selected.size() == 0) {
                    list.setSelectedIndices(new int[] {});
                } else {
                    GuiUtils.setSelectedItems(list, selected);
                }
            }

        } catch (Exception exc) {
            LogUtil.logException("Showing timeline dialog", exc);
        }
    }




}
