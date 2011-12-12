/*
 * $Id: SoundingSelector.java,v 1.59 2007/07/05 18:46:15 jeffmc Exp $
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



package ucar.unidata.view.sounding;


import org.w3c.dom.Element;


import ucar.unidata.beans.NonVetoableProperty;
import ucar.unidata.beans.Property;
import ucar.unidata.data.sounding.AddeSoundingAdapter;
import ucar.unidata.data.sounding.RAOB;
import ucar.unidata.data.sounding.SoundingAdapter;
import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;


import ucar.unidata.gis.mcidasmap.McidasMap;

import ucar.unidata.idv.chooser.*;
import ucar.unidata.idv.chooser.adde.AddeChooser;
import ucar.unidata.idv.chooser.adde.AddeServer;

import ucar.unidata.metdata.Station;



import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.view.CompositeRenderer;
import ucar.unidata.view.geoloc.NavigatedPanel;
import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.xml.XmlUtil;

import visad.DateTime;

import visad.FlatField;


import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 *  This is the class that can be used for selecting soundings from a
 *  upperair netCDF data file.  The variables that are to be used
 *  are configurable.
 *
 *  @author Don Murray Unidata/UCAR
 *  @version $Id: SoundingSelector.java,v 1.59 2007/07/05 18:46:15 jeffmc Exp $
 */
public class SoundingSelector extends IdvChooser {

    /** the idv chooser */
    private IdvChooser idvChooser;

    /** This is a virtual timestamp that tracks if the threaded adde connection should be aborted or not */
    private int connectionStep = 0;

    /** Server property identifier */
    private static String CMD_SERVER = "cmd.server";

    /** list of servers */
    private PreferenceList servers;

    /** input for file name */
    private JTextField selectedFileDisplay;

    /** list of times */
    private JList timesList;

    /** list of observations */
    private JList obsList;

    /** selected observations */
    private Vector selectedObs = new Vector();

    /** station location map */
    private StationLocationMap stationMap;

    /** current directory */
    private String directory = null;

    /** current file name */
    private String filename = null;

    /** frame for the display */
    private static JFrame frame;

    /** file browser */
    private SoundingFileBrowser fileBrowser = null;


    /** the adde chooser */
    AddeChooser addeChooser;


    /** dataset group selector */
    private JComboBox groupSelector;

    /** selected file */
    private File selectedFile;

    /** declutter flag */
    private boolean declutter = true;

    /** sounding adapter used by this seleccor */
    SoundingAdapter soundingAdapter;

    /** flag for allowing multiple selections */
    private boolean multipleSelect = false;

    /** flag for server vs. file */
    private boolean forServer = true;

    /** flag for server vs. file */
    private boolean showMainHoursOnly = true;


    /**
     * Create a new SoundingSelector associated with the resources
     *
     * @param idvChooser  the associated IDV chooser
     * @param mgr  the associated IDV chooser manager
     * @param chooserNode  the description of the chooser in XML
     */
    public SoundingSelector(IdvChooser idvChooser, IdvChooserManager mgr,
                            Element chooserNode) {
        super(mgr, chooserNode);
        this.idvChooser = idvChooser;
        this.servers =
            idvChooser.getPreferenceList(idvChooser.PREF_ADDESERVERS);
        this.forServer = XmlUtil.getAttribute(chooserNode,
                RaobChooser.ATTR_SHOWSERVER, true);
        this.multipleSelect = true;
    }



    /**
     * Construct an object for selecting sounding files starting at
     * the current directory and from a default ADDE server
     *
     * @param servers  list of servers
     */
    public SoundingSelector(PreferenceList servers) {
        this(servers, true, false);
    }


    /**
     * Construct an object for selecting sounding files starting at
     * the current directory and from a default ADDE server, set the
     * multipleSelect flag to the given value
     *
     * @param servers  list of servers
     * @param forServer   true for server vs. file
     * @param multipleSelect  true to select multiple stations
     */
    public SoundingSelector(PreferenceList servers, boolean forServer,
                            boolean multipleSelect) {
        this(servers, ".", "", forServer, multipleSelect);
    }


    /**
     * Construct an object for selecting sounding files starting at
     * the current directory and from a default ADDE server, set the
     * multipleSelect flag to the given value
     *
     *
     * @param idvChooser  the IDV chooser
     * @param servers  list of servers
     * @param forServer   true for server vs. file
     * @param multipleSelect  true to select multiple stations
     */
    public SoundingSelector(IdvChooser idvChooser, PreferenceList servers,
                            boolean forServer, boolean multipleSelect) {
        this(idvChooser, servers, ".", "", forServer, multipleSelect);
    }




    /**
     * Construct an object for selecting sounding files starting at
     * the current directory and from a default ADDE server, set the
     * multipleSelect flag to the given value
     *
     * @param servers    list of servers
     * @param directoryName  directory for files
     * @param serverName  default server
     */
    public SoundingSelector(PreferenceList servers, String directoryName,
                            String serverName) {
        this(servers, directoryName, serverName, true, false);
    }


    /**
     * Construct an object for selecting sounding files starting at
     * the specified directory.
     *
     * @param servers          list of servers
     * @param directoryName    starting directory for files
     * @param serverName       default server
     * @param forServer        true for server vs. file
     * @param multipleSelect   true to select multiple stations
     */
    public SoundingSelector(PreferenceList servers, String directoryName,
                            String serverName, boolean forServer,
                            boolean multipleSelect) {

        this(null, servers, directoryName, serverName, forServer,
             multipleSelect);
    }

    /** the directory name */
    private String directoryName;

    /**
     * Construct an object for selecting sounding files starting at
     * the specified directory.
     *
     *
     * @param idvChooser       the IDV chooser
     * @param servers          list of servers
     * @param directoryName    starting directory for files
     * @param serverName       default server
     * @param forServer        true for server vs. file
     * @param multipleSelect   true to select multiple stations
     */
    public SoundingSelector(IdvChooser idvChooser, PreferenceList servers,
                            String directoryName, String serverName,
                            boolean forServer, boolean multipleSelect) {
        super(idvChooser.getIdv(), idvChooser.getXmlNode());
        this.directoryName  = directoryName;
        this.idvChooser     = idvChooser;
        this.forServer      = forServer;
        this.servers        = servers;
        this.multipleSelect = multipleSelect;
    }


    /**
     * Get the file browser
     *
     * @return the file browser
     */
    private SoundingFileBrowser getFileBrowser() {
        if (fileBrowser == null) {
            fileBrowser = new SoundingFileBrowser(directoryName);
            fileBrowser.addPropertyChangeListener("soundingAdapter",
                    new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    setSoundingAdapter((SoundingAdapter) evt.getNewValue());
                }
            });
        }
        return fileBrowser;
    }


    /**
     * Update the status
     */
    public void updateStatus() {
        if (getHaveData()) {
            setStatus("Press \"" + CMD_LOAD
                      + "\" to load the selected sounding data", "buttons");
            return;
        }

        if (forServer) {
            if (soundingAdapter == null) {
                setStatus("Please connect to the server");
            } else {
                setStatus("Select one or more soundings");
            }

        } else {
            if (soundingAdapter == null) {
                setStatus("Select a sounding file");
            } else {
                setStatus("Select one or more soundings");
            }


        }
    }


    /**
     * Make the UI contents
     *
     * @return the UI contents
     */
    protected JComponent doMakeContents() {

        // set the user interface
        // Top panel
        GuiUtils.tmpInsets = new Insets(2, 2, 2, 2);

        List servers =
            idvChooser.getIdv().getIdvChooserManager().getAddeServers(
                AddeServer.TYPE_POINT);
        addeChooser =
            new AddeChooser(idvChooser.getIdv().getIdvChooserManager(),
                            null) {
            public void setStatus(String message, String from) {
                System.err.println("status:" + message);
                SoundingSelector.this.setStatus(message, from);
            }
        };
        groupSelector = GuiUtils.getEditableBox(Misc.newList("RTPTSRC"),
                null);

        JComponent selectorPanel;
        JComponent extraTimeComp = null;
        if (forServer) {
            JButton connectBtn = new JButton("Connect");
            connectBtn.addActionListener(this);
            connectBtn.setActionCommand(CMD_SERVER);
            JCheckBox mainHoursCbx = new JCheckBox("00 & 12Z only",
                                         showMainHoursOnly);
            mainHoursCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    showMainHoursOnly =
                        ((JCheckBox) ev.getSource()).isSelected();
                    if (soundingAdapter != null) {
                        doUpdateInner(true);
                    }
                }
            });
            extraTimeComp = mainHoursCbx;
            selectorPanel = GuiUtils.hbox(new Component[] {
                addeChooser.getServerSelector(),
                GuiUtils.rLabel(" Group: "), groupSelector,
                GuiUtils.filler(), connectBtn });
            selectorPanel = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Server:"),
                selectorPanel });
        } else {
            selectorPanel = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("File: "),
                getFileBrowser().getContents() });
        }
        selectorPanel = GuiUtils.inset(GuiUtils.leftCenter(selectorPanel,
                GuiUtils.filler()), 4);

        JComponent topPanel = selectorPanel;



        // Middle Panel
        JPanel middlePanel = new JPanel(new BorderLayout());

        obsList = new JList();
        obsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                obsListClicked(e);
            }
        });
        JScrollPane obsPane = new JScrollPane(obsList);
        obsPane.setPreferredSize(new Dimension(175, 50));

        // Add the times panel
        JScrollPane timesPane = new JScrollPane(createTimesList());
        timesPane.setPreferredSize(new Dimension(175, 50));




        JComponent timeLabel = new JLabel("Available Times:");
        if (extraTimeComp != null) {
            timeLabel = GuiUtils.vbox(timeLabel, extraTimeComp);
        }

        JPanel left = GuiUtils.doLayout(new Component[] { timeLabel,
                timesPane, new JLabel("Selected Soundings:"), obsPane }, 1,
                    GuiUtils.WT_N, GuiUtils.WT_NYNY);

        middlePanel.add(GuiUtils.inset(left, 5), BorderLayout.WEST);

        // Add the station display panel
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Available Stations"));
        p.setLayout(new BorderLayout());

        CompositeRenderer mapRenderer = new CompositeRenderer();
        mapRenderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPW"));
        mapRenderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPU"));
        mapRenderer.setColor(MAP_COLOR);
        stationMap = new StationLocationMap(multipleSelect, mapRenderer);

        stationMap.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pe) {
                if (pe.getPropertyName().equals(
                        StationLocationMap.SELECTED_PROPERTY)) {
                    stationSelected((SoundingStation) pe.getNewValue());
                } else if (pe.getPropertyName().equals(
                        StationLocationMap.UNSELECTED_PROPERTY)) {
                    stationUnselected((SoundingStation) pe.getNewValue());
                } else if (pe.getPropertyName().equals(
                        StationLocationMap.ALL_UNSELECTED_PROPERTY)) {
                    unselectAll();
                }
            }
        });
        middlePanel.add(stationMap, BorderLayout.CENTER);
        JComponent buttons = getDefaultButtons();
        return GuiUtils.topCenterBottom(topPanel, middlePanel, buttons);
    }



    /**
     * Handle a station selection
     *
     * @param station  selected station
     */
    private void stationSelected(SoundingStation station) {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        for (int i = 0; i < selectedTimes.size(); i++) {
            DateTime dt = (DateTime) selectedTimes.get(i);
            List times =
                soundingAdapter.getSoundingTimes(station);
            if ((times != null) && (times.size() > 0)) {
                if (times.contains(dt)) {
                    SoundingOb newObs = new SoundingOb(station, dt);
                    if ( !selectedObs.contains(newObs)) {
                        selectedObs.add(newObs);
                    }
                }
            }
        }
        obsList.setListData(selectedObs);
        checkLoadData();
    }

    /**
     * get the addechooser we use
     *
     * @return adde chooser
     */
    public AddeChooser getAddeChooser() {
        return addeChooser;
    }


    /**
     * check whether data has been loaded or not
     */
    private void checkLoadData() {
        setHaveData(obsList.getModel().getSize() > 0);
        updateStatus();
    }

    /**
     * Unselect a station
     *
     * @param station  station to unselect
     */
    private void stationUnselected(SoundingStation station) {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        for (int i = 0; i < selectedTimes.size(); i++) {
            SoundingOb newObs = new SoundingOb(station,
                                    (DateTime) selectedTimes.get(i));
            if (selectedObs.contains(newObs)) {
                selectedObs.remove(newObs);
            }
        }
        obsList.setListData(selectedObs);
        checkLoadData();
    }


    /**
     * Unselect all station
     */
    private void unselectAll() {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        selectedObs.removeAllElements();
        obsList.setListData(selectedObs);
        checkLoadData();
    }


    /**
     * Get the server name.
     *
     * @return server name
     */
    private String getServer() {
        return addeChooser.getServer();
    }

    /**
     * Get the dataset (group) name.
     *
     * @return group name
     */
    private String getGroupName() {
        return groupSelector.getSelectedItem().toString();
    }

    /**
     * Get the mandatory dataset name.
     *
     * @return mandatory dataset name
     */
    private String getMandatoryDataset() {
        return getGroupName() + "/UPPERMAND";
    }

    /**
     * Get the sig level dataset name.
     *
     * @return sig level dataset name
     */
    private String getSigLevelDataset() {
        return getGroupName() + "/UPPERSIG";
    }


    /**
     * Handle the selection of an ob
     *
     * @param event  MouseEvent for selection
     */
    private void obsListClicked(MouseEvent event) {
        if ( !SwingUtilities.isRightMouseButton(event)) {
            return;
        }
        int index = obsList.locationToIndex(new Point(event.getX(),
                        event.getY()));
        if ((index < 0) || (index >= selectedObs.size())) {
            return;
        }

        final SoundingOb obs   = (SoundingOb) selectedObs.get(index);

        JPopupMenu       popup = new JPopupMenu();
        JMenuItem        mi;

        mi = new JMenuItem("Remove " + obs);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedObs.remove(obs);
                obsList.setListData(selectedObs);
                checkLoadData();
                stationMap.setSelectedStations(getCurrentSelectedStations());
            }
        });

        popup.add(mi);

        mi = new JMenuItem("Remove all");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedObs.removeAllElements();
                obsList.setListData(selectedObs);
                checkLoadData();
                stationMap.setSelectedStations(getCurrentSelectedStations());
            }
        });

        popup.add(mi);

        popup.show(obsList, event.getX(), event.getY());
    }




    /**
     * Update the selector.
     */
    public void doUpdate() {
        if ( !forServer) {
            return;
        }
        doUpdateInner(false);
    }


    /**
     * Really update the selector.
     *
     * @param forceNewAdapter If true then create a new adapter.
     *                        Else, tell the existing one to update.
     */
    private void doUpdateInner(final boolean forceNewAdapter) {
        //Use the timestep so we can tell if the user hit connect again while 
        //we are waiting in this thread. If they do then we abort this one.
        final int timestep = ++connectionStep;
        Misc.run(new Runnable() {
            public void run() {
                clearWaitCursor();
                showWaitCursor();
                try {
                    if (forceNewAdapter) {
                        setStatus("Connecting to the server: " + getServer());
                        AddeSoundingAdapter newAdapter =
                            new AddeSoundingAdapter(getServer(),
                                getMandatoryDataset(), getSigLevelDataset(),
                                showMainHoursOnly);
                        if (timestep != connectionStep) {
                            return;
                        }
                        soundingAdapter = null;
                        setSoundingAdapter(newAdapter);
                    } else if (soundingAdapter != null) {
                        setStatus("Connecting to the server: " + getServer());
                        List times = getSelectedTimes();
                        soundingAdapter.update();
                        setStations();
                        setTimesListData(times);
                    }
                } catch (Exception exc) {
                    LogUtil.logException("Updating sounding data", exc);
                } finally {
                    updateStatus();
                }
                showNormalCursor();
            }
        });

    }


    /**
     *  Gets called when the user presses Cancel in multipleSelect mode
     *  This can get overwritten by a derived class to do something.
     */
    public void doCancel() {}

    /**
     * Get the SoundingAdapter used by this selector.
     * @return the SoundingAdapter used by this selector.
     */
    public SoundingAdapter getSoundingAdapter() {
        return soundingAdapter;
    }

    /**
     * Set the SoundingAdapter used by this selector
     *
     * @param newAdapter   new adapter
     */
    protected void setSoundingAdapter(SoundingAdapter newAdapter) {
        soundingAdapter = newAdapter;
        selectedObs.removeAllElements();
        obsList.setListData(selectedObs);
        setStations();
        setTimesListData();
        checkLoadData();
    }

    /**
     * Create the list of times.
     *
     * @return List of times
     */
    private JList createTimesList() {
        timesList = new JList();
        timesList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        timesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !timesList.isSelectionEmpty()
                        && !e.getValueIsAdjusting()) {
                    Object[] t = timesList.getSelectedValues();
                    newTimes(Misc.toList(t));
                }
            }
        });
        return timesList;
    }

    /**
     * Set the new times
     *
     * @param times new times to use
     */
    private void newTimes(List times) {
        List current = stationMap.getSelectedStations();
        if ((current == null) || (current.size() < 1)) {
            return;
        }
        selectedObs.removeAllElements();
        for (int i = 0; i < times.size(); i++) {
            DateTime dt = (DateTime) times.get(i);
            for (int j = 0; j < current.size(); j++) {
                SoundingStation ss      = (SoundingStation) current.get(j);
                List            ssTimes =
                    soundingAdapter.getSoundingTimes(ss);
                if ((ssTimes != null) && (times.size() > 0)) {
                    if (ssTimes.contains(dt)) {
                        SoundingOb newObs = new SoundingOb(ss, dt);
                        if ( !selectedObs.contains(newObs)) {
                            selectedObs.add(newObs);
                        }
                    }
                }
            }
        }
        obsList.setListData(selectedObs);
        checkLoadData();
    }

    /**
     * Get the current list of stations that are selected
     */
    private void setStations() {
        List current = getCurrentSelectedStations();
        stationMap.setStations(getSoundingAdapter().getStations(), current,
                               declutter);
        stationMap.redraw();
    }

    /**
     * Create a declutter checkbox.
     *
     * @return the checkbox to handle decluttering.
     */
    private JCheckBox createDeclutterCheckBox() {
        JCheckBox cb = new JCheckBox("Declutter", declutter);
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                declutter = ((JCheckBox) e.getSource()).isSelected();
                stationMap.setDeclutter(declutter);
            }
        });
        return cb;
    }



    /**
     * Handle actions.
     *
     * @param e  ActionEvent to handle.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals(CMD_SERVER)) {
            doUpdateInner(true);
            //            servers.saveState(serverSelector);
        } else {
            super.actionPerformed(e);
        }
    }


    /**
     *  This looks in the selectedList of SoundingOb-s for all stations
     *  that are selectged for the current time. It creates and returns
     *  a list of the Station-s held by these current SoundingOb-s
     *
     * @return list of currently selected stations
     */
    private List getCurrentSelectedStations() {
        List     current     = new ArrayList();
        DateTime currentTime = getSelectedTime();
        for (int i = 0; i < selectedObs.size(); i++) {
            SoundingOb ob = (SoundingOb) selectedObs.get(i);
            if (ob.getTimestamp().equals(currentTime)) {
                current.add(ob.getStation());
            }
        }
        return current;

    }

    /**
     * Set the data in the times list
     */
    private void setTimesListData() {
        setTimesListData(null);
    }

    /**
     * Set the data in the times list
     *
     * @param selected  a list of times that should be selected
     */
    private void setTimesListData(List selected) {
        DateTime[] times = getSoundingAdapter().getSoundingTimes();
        if (times != null) {
            timesList.setListData(times);
            if ((selected != null) && (selected.size() > 0)) {
                ListModel lm      = timesList.getModel();
                int[]     indices = new int[times.length];
                int       l       = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    if (selected.contains(lm.getElementAt(i))) {
                        indices[l++] = i;
                    }
                }
                if (l > 0) {
                    int[] selectedIndices = new int[l];
                    System.arraycopy(indices, 0, selectedIndices, 0, l);
                    timesList.setSelectedIndices(selectedIndices);
                    timesList.ensureIndexIsVisible(selectedIndices[l - 1]);
                } else {
                    timesList.setSelectedValue(times[times.length - 1], true);
                }
            } else if (times.length > 0) {
                timesList.setSelectedValue(times[times.length - 1], true);
            }
        } else {
            LogUtil.userMessage("No data available");
        }
    }

    /**
     * Test routine
     *
     * @param argv  not used
     */
    public static void main(String[] argv) {
        frame = new JFrame("Sounding Selector");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //JOptionPane.setRootFrame(frame);
        SoundingSelector ss =
            new SoundingSelector(
                new PreferenceList(Misc.newList("adde.ucar.edu")),
                "/var/data/ldm/decoded", "adde.ucar.edu", true, true);

        Container contentPane = frame.getContentPane();

        contentPane.add(ss.getContents(), BorderLayout.CENTER);

        // Add a close button
        JPanel  panel   = new JPanel(false);
        JButton dismiss = new JButton("Dismiss");

        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        panel.add(dismiss);
        contentPane.add(panel, BorderLayout.SOUTH);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width / 2 - 320,
                          screenSize.height / 2 - 125);
        frame.setVisible(true);
    }


    /**
     * Get the selected time.
     *
     * @return the time selected in the list
     */
    public DateTime getSelectedTime() {
        return (DateTime) timesList.getSelectedValue();
    }

    /**
     * Get the selected time.
     *
     * @return the time selected in the list
     */
    public List getSelectedTimes() {
        return Misc.toList(timesList.getSelectedValues());
    }

    /**
     * Get the selected soundings
     *
     * @return List of selected soundings
     */
    public List getSelectedSoundings() {
        return selectedObs;
    }


}

