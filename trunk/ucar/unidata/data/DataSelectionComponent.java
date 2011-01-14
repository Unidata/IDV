/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Found2ation; either version 2.1 of the License, or (at
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


package ucar.unidata.data;

import javax.swing.JComponent;


/**
 * Used by a display control and field selector to show arbitrary data
 * selection stuff. This is used to pass information into a control or
 * data source.
 */
public abstract class DataSelectionComponent {

    /** the name */
    private String name;

    /** gui content */
    JComponent contents;

    /** 
     * We can be used by a display control and this is the 
     * control's data selection 
     */
    protected DataSelection dataSelection;

    /**
     * ctor
     *
     * @param name name
     */
    public DataSelectionComponent(String name) {
        this.name = name;
    }


    /**
     * Make if needed and return the gui contents
     *
     * @return gui contents
     */
    public JComponent getContents() {
        return getContents(null);
    }

    /**
     * Make if needed and return the gui contents
     *
     * @param dataSelection If created by a display control 
     *                      this is the control's dataselection
     * @return gui contents
     */
    public JComponent getContents(DataSelection dataSelection) {
        this.dataSelection = dataSelection;
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }


    /**
     * Hook method to make the contents
     *
     * @return gui contents
     */
    protected abstract JComponent doMakeContents();

    /**
     * Apply settings to the data selection
     *
     * @param dataSelection data selection to apply to
     */
    public abstract void applyToDataSelection(DataSelection dataSelection);



    /**
     * get name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Should this show in the control properties, subclasses can override as
     * needed.
     * @return true
     */
    public boolean getShowInControlProperties() {
        return true;
    }

}

