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


import ucar.unidata.idv.ControlDescriptor;


import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PropertyValue;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


/**
 *
 * @author  Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public class DisplaySetting {

    /** The name */
    private String name;

    /** List of values */
    private List<PropertyValue> propertyValues;

    /** Is this one of the user's local display settings */
    private boolean isLocal;

    /** Which controls if any is this applicable to */
    private Hashtable applicableToControls;

    /**
     * ctor
     */
    public DisplaySetting() {}

    /**
     * ctor
     *
     * @param name The name
     * @param propertyValues The values
     */
    public DisplaySetting(String name, List<PropertyValue> propertyValues) {
        this.name           = name;
        this.propertyValues = propertyValues;
    }

    /**
     * Is this displaysetting applicable to the given cd
     *
     * @param cd The control descriptor
     *
     * @return is applicable
     */
    public boolean applicableTo(ControlDescriptor cd) {
        if ((applicableToControls == null)
                || (applicableToControls.size() == 0)) {
            return true;
        }
        return applicableToControls.get(cd.getControlId()) != null;
    }


    /** The last category */
    private static String lastCat;

    /** The last name */
    private static String lastName = "";


    /**
     * Change the name of this display setting
     *
     * @param idv the idv
     * @param dialog the dialog
     *
     * @return ok
     */
    public boolean changeName(IntegratedDataViewer idv, JDialog dialog) {
        String name = this.getNameWithoutCategory();
        String cat  = this.getCategory();
        Object[] result = DisplaySetting.getNewName(idv, null, cat, name,
                              null);
        if (result == null) {
            return false;
        }

        String newName = (String) result[0];

        if (newName.equals(this.getName())) {
            return false;
        }
        DisplaySetting existing =
            idv.getResourceManager().findDisplaySetting(newName);
        if (existing != null) {
            idv.getResourceManager().removeDisplaySetting(existing);
        }

        this.setName(newName);
        idv.getResourceManager().displaySettingChanged(this);
        return true;


    }


    /**
     * Save the display settings
     *
     * @param idv the idv
     * @param dialog the dialog
     * @param propList List of properties
     * @param display The display
     */
    public static void doSave(IntegratedDataViewer idv, JDialog dialog,
                              List<PropertyValue> propList,
                              DisplayControlImpl display) {

        ControlDescriptor cd = null;
        if (display != null) {
            cd = idv.getControlDescriptor(display.getDisplayId());
        }

        String lbl = ((cd != null)
                      ? "Applicable only to displays of type: " + cd
                      : null);
        Object[] result = DisplaySetting.getNewName(idv, dialog, lastCat,
                              lastName, lbl);
        if (result == null) {
            return;
        }
        boolean only = ((Boolean) result[1]).booleanValue();

        String  name = (String) result[0];
        DisplaySetting existing =
            idv.getResourceManager().findDisplaySetting(name);

        if (existing != null) {
            existing.setPropertyValues(propList);
            idv.getResourceManager().displaySettingChanged(existing);
        } else {
            existing = new DisplaySetting(name, propList);
            idv.getResourceManager().addDisplaySetting(existing);
        }
        if (cd != null) {
            if (only) {
                existing.setOnlyApplicableTo(cd);
            } else {
                existing.clearOnlyApplicableTo();
            }
        }

    }

    /**
     * Get a new name
     *
     * @param idv the idv
     * @param dialog The dialog
     * @param dfltCategory The dflt category
     * @param dfltName The name
     * @param cbxLabel label for checkbox
     *
     * @return new name to use
     */
    private static Object[] getNewName(IntegratedDataViewer idv,
                                       JDialog dialog, String dfltCategory,
                                       String dfltName, String cbxLabel) {

        JCheckBox cbx        = new JCheckBox((cbxLabel != null)
                                             ? cbxLabel
                                             : "");

        Vector    categories = new Vector();
        List displaySettings = idv.getResourceManager().getDisplaySettings();
        categories.add("");
        for (int i = 0; i < displaySettings.size(); i++) {
            DisplaySetting displaySetting =
                (DisplaySetting) displaySettings.get(i);
            String cat = displaySetting.getCategory();
            if ((cat != null) && !categories.contains(cat)) {
                categories.add(cat);
            }
        }


        JComboBox catBox = new JComboBox(categories);
        catBox.setEditable(true);
        if (dfltCategory != null) {
            catBox.setSelectedItem(dfltCategory);
        }

        JTextField fld = new JTextField("", 20);
        if (dfltName != null) {
            fld.setText(dfltName);
        }

        catBox.setToolTipText("Use '>' for sub-categories");
        JComponent contents =
            GuiUtils.vbox(
                GuiUtils.inset(
                    new JLabel("Please enter a display settings name"),
                    5), GuiUtils.hbox(
                        GuiUtils.label(" Category: ", catBox),
                        GuiUtils.label("  Name: ", fld)));
        if (cbxLabel != null) {
            contents = GuiUtils.vbox(contents, cbx);
        }
        String         name     = null;
        DisplaySetting existing = null;

        while (true) {
            if ( !GuiUtils.showOkCancelDialog(dialog,
                    "Save Display Settings", GuiUtils.inset(contents, 5),
                    null)) {
                return null;
            }
            String cat = catBox.getSelectedItem().toString().trim();
            name = fld.getText().trim();
            if (name.length() == 0) {
                if ( !GuiUtils.showOkCancelDialog(dialog,
                        "Save Display Settings",
                        GuiUtils.inset(new JLabel("Please enter a name"), 5),
                        null)) {
                    return null;
                }
                continue;
            }
            lastCat  = cat;
            lastName = name;
            if (cat.length() > 0) {
                name = cat + ">" + name;
            }
            name     = DisplaySetting.cleanName(name);
            existing = idv.getResourceManager().findDisplaySetting(name);
            if (existing != null) {
                if ( !GuiUtils.askYesNo(
                        "Display Setting",
                        "<html>A display setting with the name:<br><i>&nbsp;&nbsp; "
                        + name
                        + "</i><br>exists. Do you want to overwrite it</html>?")) {
                    continue;
                }
            }
            return new Object[] { name, new Boolean(cbx.isSelected()) };
        }


    }



    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * get the category
     *
     * @return the category
     */
    public String getCategory() {
        int idx = getName().lastIndexOf(">");
        if (idx >= 0) {
            return getName().substring(0, idx).trim();
        }
        return null;
    }

    /**
     * Get just the name without the category prefix
     *
     * @return name
     */
    public String getNameWithoutCategory() {
        int idx = name.lastIndexOf(">");
        if (idx < 0) {
            return name;
        }
        return name.substring(idx + 1, name.length());

    }


    /**
     * Clean up the name
     *
     * @param name the name
     *
     * @return cleaned name
     */
    public static String cleanName(String name) {
        List toks = StringUtil.split(name, ">", true, true);
        return StringUtil.join(">", toks);
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the PropertyValues property.
     *
     *  @param value The new value for PropertyValues
     */
    public void setPropertyValues(List<PropertyValue> value) {
        propertyValues = value;
    }

    /**
     *  Get the PropertyValues property.
     *
     *  @return The PropertyValues
     */
    public List<PropertyValue> getPropertyValues() {
        return propertyValues;
    }



    /**
     * is equals
     *
     * @param o to
     *
     * @return is equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DisplaySetting)) {
            return false;
        }
        DisplaySetting that = (DisplaySetting) o;
        //Just check on name based equality
        return Misc.equals(this.name, that.name);
    }


    /**
     *  Set the IsLocal property.
     *
     *  @param value The new value for IsLocal
     */
    public void setIsLocal(boolean value) {
        isLocal = value;
    }

    /**
     *  Get the IsLocal property.
     *
     *  @return The IsLocal
     */
    public boolean getIsLocal() {
        return isLocal;
    }




    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        //        return GuiUtils.getLocalName(name, isLocal, false);
        if (isLocal) {
            return name + " <local>";
        }
        return name;
    }

    /**
     * clear out the applicable to controls map
     */
    public void clearOnlyApplicableTo() {
        applicableToControls = null;
    }

    /**
     * set that this setting is only applicable to the given control
     *
     * @param cd the control
     */
    public void setOnlyApplicableTo(ControlDescriptor cd) {
        applicableToControls = new Hashtable();
        applicableToControls.put(cd.getControlId(), new Boolean(true));
    }


    /**
     *  Set the ApplicableToControls property.
     *
     *  @param value The new value for ApplicableToControls
     */
    public void setApplicableToControls(Hashtable value) {
        applicableToControls = value;
    }

    /**
     *  Get the ApplicableToControls property.
     *
     *  @return The ApplicableToControls
     */
    public Hashtable getApplicableToControls() {
        return applicableToControls;
    }


}
