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

package ucar.unidata.idv.ui;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;


import ucar.unidata.idv.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;




/**
 * A base class for those components that show a set of
 * {@link ucar.unidata.data.DataSource}s.  For now these are the
 * {@link ucar.unidata.idv.ui.DataTree} and
 * {@link ucar.unidata.idv.ui.DataSelector}. This class provides
 * basic facilities to manage the list of data sources, create the
 * window, etc.
 *
 * @author IDV development team
 */


public abstract class DataSourceHolder {

    /** The IDV */
    protected IntegratedDataViewer idv;

    /** The window this gui is in */
    protected IdvWindow frame;

    /** List of data sources */
    private List dataSources;

    /** The formula data source, i.e., the one that holds the end-user formulas */
    protected DataSource formulaDataSource;

    /** If true then we don't remove the formula data source */
    protected boolean treatFormulaDataSourceSpecial = true;

    /** The size of the window */
    protected Dimension defaultDimension;

    /** Icon to show the derived data choices */
    private static ImageIcon derivedIcon;




    /**
     * Create this object
     *
     * @param idv The IDV
     * @param formulaDataSource The singleton formula data source. We treat this
     *                          special.
     * @param defaultDimension How big is the window.
     */
    public DataSourceHolder(IntegratedDataViewer idv,
                            DataSource formulaDataSource,
                            Dimension defaultDimension) {
        this.idv               = idv;
        this.formulaDataSource = formulaDataSource;
        if (defaultDimension != null) {
            this.defaultDimension = new Dimension(defaultDimension);
        }
    }







    /**
     * Make the IdvWindow. Add  event handlers for adding new data sources
     * and closing the window.
     *
     * @return  The window to put the gui in
     */
    public IdvWindow doMakeFrame() {
        if (frame == null) {
            JButton newBtn = new JButton("Add New Data Source");
            newBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    idv.showChooser();
                }
            });
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doClose();
                }
            });


            JComponent extra = getButtons();
            JComponent buttons;
            if (extra != null) {
                buttons = GuiUtils.wrap(GuiUtils.hflow(Misc.newList(  /*newBtn,*/
                    extra, closeBtn), 4, 4));
            } else {
                buttons = GuiUtils.wrap(GuiUtils.hflow(Misc.newList(  /*newBtn,*/
                    closeBtn), 4, 4));
            }

            JPanel contents = GuiUtils.centerBottom(getContents(), buttons);
            frame = new IdvWindow(getName(), idv, false);
            frame.getContentPane().add(contents);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    doClose();
                }
            });
            frame.pack();
            frame.show();
        }
        return frame;
    }

    /**
     * Return the IDV member.
     *
     * @return The IDV
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     * Get the name of this data source holder. Can be overrode
     * by derived classes to provide their own name.
     *
     * @return The name
     */
    protected String getName() {
        return "Field Selector";
    }

    /**
     * Remove this holder from the  IDV and dispose of the window
     */
    public void doClose() {
        idv.getIdvUIManager().removeDataSourceHolder(this);
        dispose();
    }


    /**
     * Show the window if it is non-null
     */
    public void show() {
        if (frame != null) {
            frame.show();
        }
        Component contents = getContents();
        if (contents != null) {
            GuiUtils.showComponentInTabs(contents, true);
        }
    }

    /**
     * Set our window
     *
     * @param f The window
     */
    public void setFrame(IdvWindow f) {
        frame = f;
    }

    /**
     * Get our window.
     *
     * @return The window
     */
    public IdvWindow getFrame() {
        return frame;
    }

    /**
     * Needs to be overrode by derived classes to return
     * the GUI
     * @return The GUI
     */
    public abstract JComponent getContents();

    /**
     * Utility to determine the location on the screen.
     *
     * @return Screen location
     */
    public Point getLocationOnScreen() {
        return getContents().getLocationOnScreen();
    }


    /**
     * Hook for derived classes to provide their own dialog buttons
     *
     * @return This returns null but is meant to return dialog buttons
     */
    public JComponent getButtons() {
        return null;
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataSource}s
     *
     * @return List of  data sources
     */
    public List getDataSources() {
        if (dataSources == null) {
            dataSources = new ArrayList();
        }
        return dataSources;
    }


    /**
     * Utility method to create, if needed, and return
     * the ImageIcon to be used to show
     * {@link ucar.unidata.data.DerivedDataChoice}s
     *
     * @return The icon for derived data choices
     */
    public static ImageIcon getDerivedIcon() {
        if (derivedIcon == null) {
            derivedIcon =
                GuiUtils.getImageIcon("/auxdata/ui/icons/Derived.gif",
                                      DataSourceHolder.class);
        }
        return derivedIcon;
    }


    /**
     * Set the default window size to be used by this component
     *
     * @param d The default size
     */
    public void setDefaultSize(Dimension d) {
        defaultDimension = new Dimension(d);
    }

    /**
     *  A no-op that can be overrode by a derived class
     *
     * @param control The new display control
     * @param choice The  data choice
     */
    public void addDisplayControl(DisplayControl control,
                                  DataChoice choice) {}

    /**
     *  Remove the given {@link DisplayControl}
     *
     * @param control The  removed display control
     */
    public void removeDisplayControl(DisplayControl control) {}

    /**
     *  Adds the given {@link DisplayControl}
     *
     * @param control The  new display control
     */
    public void addDisplayControl(DisplayControl control) {}




    /**
     *  Remove all references to anything we may have. We do this because (stupid) Swing
     *  seems to keep around lots of different references to thei component and/or it's
     *  frame. So when we do a window.dispose () this DataSourceHolder  does not get gc'ed.
     */
    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
        formulaDataSource = null;
        frame             = null;
        dataSources       = null;
    }




    /**
     *  Remove all data sources
     */
    public synchronized void removeAllDataSources() {
        //Use tmp array because we remove objects from the main list in the loop
        List tmp = new ArrayList(getDataSources());
        for (int i = 0; i < tmp.size(); i++) {
            DataSource source = (DataSource) tmp.get(i);
            removeDataSourceInner(source);
        }
    }


    /**
     * Add the given {@link ucar.unidata.data.DataSource}
     *
     * @param dataSource The new data source
     */
    public void addDataSource(DataSource dataSource) {
        if ( !getDataSources().contains(dataSource)) {
            getDataSources().add(dataSource);
        }
    }

    /**
     *  Remove the given {@link ucar.unidata.data.DataSource}
     * only if it is not the formulaDataSource.
     *
     * @param dataSource The data source to be removed
     */
    public void removeDataSource(DataSource dataSource) {
        removeDataSourceInner(dataSource);
    }


    /**
     *  Remove the specified data source only if it is not the formulaDataSource.
     *
     * @param dataSource The data source to be removed
     * @return Was this actually removed
     */
    protected boolean removeDataSourceInner(DataSource dataSource) {
        if (treatFormulaDataSourceSpecial
                && (dataSource == formulaDataSource)) {
            return false;
        }
        getDataSources().remove(dataSource);
        return true;
    }


    /**
     * Be notified that the given {@link ucar.unidata.data.DataSource} has changed.
     *
     * @param source The data source that changed
     */
    public void dataSourceChanged(DataSource source) {}

    /**
     * Be notified that the tim selection on the given
     * {@link ucar.unidata.data.DataSource} has changed.
     *
     * @param source The data source whose time has changed
     */
    public void dataSourceTimeChanged(DataSource source) {}


    /**
     *  A hook to notify that the list of favorites has changed
     */
    public void displayTemplatesChanged() {}

}
