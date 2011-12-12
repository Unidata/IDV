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

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.ui.IdvWindow;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

//import visad.util.ReflectedUniverse;

import ucar.visad.display.DisplayMaster;




import visad.*;

import visad.java3d.*;



import visad.ss.BasicSSCell;
import visad.ss.FancySSCell;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * A wrapper around the visad spread sheet cell display
 * @author IDV Development team
 * @version $Revision: 1.40 $
 */
public class OmniControl extends DisplayControlImpl {

    /** the display */
    private FancySSCell display;

    /** list of data */
    private JList dataList;

    /** data choice list */
    private Vector dataChoiceList = new Vector();

    /** list of data instances for the choices */
    private ArrayList dataInstances = new ArrayList();

    /** local copy of scalarmaps (for persistence) */
    private ScalarMap[] scalarMaps = null;

    /** the save string (for persistence) */
    private String saveString = null;

    /** the cell name (for persistence) */
    private String cellName = null;

    /**
     * Default constructor
     */
    public OmniControl() {
        setAttributeFlags(FLAG_DATACONTROL);
    }


    /**
     * Initialize this with the list of {@link DataChoice}s.
     *
     * @param choices    list of choices for data selection
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(List choices) throws VisADException, RemoteException {
        //For now set the cellName to null to force using something unique.
        //If we don't do this then we risk of having errors from the SS 
        //when loading in a bundle
        cellName = null;

        if (cellName == null) {
            cellName = getDisplayName() + "_" + Misc.getUniqueId();
        }
        display = new FancySSCell(cellName);

        display.setPreferredSize(new Dimension(256, 256));
        display.setAutoShowControls(false);
        display.setAutoDetect(scalarMaps == null);
        //TODO: Use ReflectedUniverse so we can use in a non-Java3D setting
        IdvPreferenceManager pm = getControlContext().getPreferenceManager();
        KeyboardBehavior     kb = null;
        if (display.canDo3D()) {
            display.setDimension(BasicSSCell.JAVA3D_3D);
            visad.java3d.DisplayRendererJ3D dr =
                (visad.java3d
                    .DisplayRendererJ3D) display.getDisplay()
                        .getDisplayRenderer();
            kb = new visad.java3d.KeyboardBehaviorJ3D(dr);
            dr.addKeyboardBehavior((visad.java3d.KeyboardBehaviorJ3D) kb);
        } else {
            display.setDimension(BasicSSCell.JAVA2D_2D);
            visad.java2d.DisplayRendererJ2D dr =
                (visad.java2d
                    .DisplayRendererJ2D) display.getDisplay()
                        .getDisplayRenderer();
            kb = new visad.java2d.KeyboardBehaviorJ2D(dr);
            dr.addKeyboardBehavior((visad.java2d.KeyboardBehaviorJ2D) kb);
        }

        if (kb != null) {
            DisplayMaster.setKeyboardEventMap(pm.getKeyboardMap(), kb);
        }
        DisplayImpl displayImpl = display.getDisplay();
        displayImpl.getDisplayRenderer().getMouseBehavior().getMouseHelper()
            .setFunctionMap(pm.getMouseMap());

        dataList = new JList();
        dataList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showDataChoiceMenu(e);
            }
        });
        dataList.setVisibleRowCount(4);
        //      setWindowVisible(true);
        return setData(choices);
    }


    /**
     *  Removes this display control
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        display.hideWidgetFrame();
        DisplayImpl ssDisplay = display.getDisplay();
        if (ssDisplay != null) {
            ssDisplay.destroy();
        }
        display.destroyCell();
    }



    /**
     * Make contents of the OmniControl control window
     *
     * @return  UI for control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    JComponent mycontents;

    /**
     * Make the gui
     *
     * @return the gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        JButton mapping  = new JButton("Mappings");
        JButton controls = new JButton("Controls");
        mapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                display.addMapDialog();
            }
        });
        controls.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                display.showWidgetFrame();
            }
        });

        JScrollPane sp = new JScrollPane();
        sp.getViewport().setView(dataList);
        sp.setBorder(
            BorderFactory.createCompoundBorder(
                new TitledBorder("Data"),
                new BevelBorder(BevelBorder.LOWERED)));
        return mycontents = GuiUtils.centerBottom(display,
                GuiUtils.vbox(sp,
                              GuiUtils.hbox(Misc.newList(mapping,
                                  controls))));
    }


    /**
     * After the XML unpersistence, initialize the set of ScalarMaps,
     * and control parameters
     *
     * @param vc    control context
     * @param properties Can hold arbitrary state when unpersisting
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties) {
        super.initAfterUnPersistence(vc, properties);
        try {
            if (scalarMaps != null) {
                display.setMaps(scalarMaps);
            }
            if (saveString != null) {
                display.setPartialSaveString(saveString, true);
            }
        } catch (Exception excp) {
            logException("initAfterUnPersistence:", excp);
        }
    }

    /**
     * Override superclass method and return the appropriate label.
     *
     * @return new label change parameter label
     */
    protected String getChangeParameterLabel() {
        return "Add Parameter...";
    }

    /**
     * Gets called when the user has selected a new DataChoice.
     * By default this method extracts the first DataChoice from
     * the list of choices and calls setData (DataChoice dataChoice)
     * {return true;}
     * <p>
     * This returns whether the data setting was successfull or not.
     * @param newChoices  list of choices
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(List newChoices)
            throws VisADException, RemoteException {
        createDataInstances(getDataChoices());
        return true;
    }


    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param  newChoices  list of new data choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        processNewData(newChoices);
        //doShare (SHARE_CHOICES, newChoices);
    }


    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param  newChoices  list of new data choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void processNewData(List newChoices)
            throws VisADException, RemoteException {
        appendDataChoices(newChoices);
        createDataInstances(newChoices);
    }

    /**
     * Remove data for a particular row in the list
     *
     * @param row   row in the list
     */
    private void removeData(int row) {
        DataInstance di = (DataInstance) dataInstances.get(row);
        try {
            display.removeData(di.getData());
            dataInstances.remove(row);
            dataChoiceList.remove(row);
            removeDataChoice(di.getDataChoice());
            dataList.setListData(dataChoiceList);
        } catch (Exception e) {
            logException("removeData:" + row, e);
        }
    }

    /**
     * Create the data instances for the list of choices
     *
     * @param choices   list of data choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void createDataInstances(List choices)
            throws VisADException, RemoteException {
        for (int i = 0; i < choices.size(); i++) {
            DataInstance dataInstance =
                doMakeDataInstance((DataChoice) choices.get(i));
            if (dataInstance.dataOk()) {
                display.addData(getData(dataInstance));
                dataInstances.add(dataInstance);
                dataChoiceList.add(dataInstance.getDataChoice());
            }
        }
        dataList.setListData(dataChoiceList);
    }

    /**
     * Override base class method to just trigger a redisplay of the data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void resetData() throws VisADException, RemoteException {
        dataInstances.clear();
        dataChoiceList.clear();
        createDataInstances(getDataChoices());
        dataList.setListData(dataChoiceList);
    }

    /**
     * Show the DataChoice menu for a given mouse event
     *
     * @param me   mouse event to check
     */
    private void showDataChoiceMenu(MouseEvent me) {

        if (SwingUtilities.isRightMouseButton(me)) {
            final Point point = me.getPoint();
            JMenuItem   mi    = new JMenuItem("Remove");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    removeData(dataList.locationToIndex(point));
                }
            });
            ArrayList items = new ArrayList();
            items.add(mi);
            JPopupMenu menu = GuiUtils.makePopupMenu(items);
            if (menu != null) {
                menu.show(dataList, me.getX(), me.getY());
            }
        }
    }

    /**
     * Get the set of ScalarMaps used in this display. Used by persistence
     *
     * @return maps
     */
    public ScalarMap[] getScalarMaps() {
        if (display != null) {
            scalarMaps = display.getMaps();
        }
        return scalarMaps;
    }

    /**
     * Set the set of ScalarMaps used in this display
     *
     * @param maps set of ScalarMaps to use
     */
    public void setScalarMaps(ScalarMap[] maps) {
        scalarMaps = maps;
    }

    /**
     * Get the name of this cell. Used by persistence
     *
     * @return cell name
     */
    public String getCellName() {
        if (display != null) {
            cellName = display.getName();
        }
        return cellName;
    }

    /**
     * Set the name of this cell. Used by persistence
     *
     * @param name    cell name
     */
    public void setCellName(String name) {
        cellName = name;
    }

    /**
     * Get the save string name of this cell. Used by persistence
     *
     * @return save string
     */
    public String getSaveString() {
        if (display != null) {
            saveString = display.getPartialSaveString();
        }
        return saveString;
    }

    /**
     * Set the saved string properties
     *
     * @param save save string properties
     */
    public void setSaveString(String save) {
        saveString = save;
    }

}
