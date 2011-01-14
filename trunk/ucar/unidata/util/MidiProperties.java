/*
 * $Id: MidiProperties.java,v 1.5 2006/12/03 17:15:15 dmurray Exp $
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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.util;


import java.awt.Insets;
import java.awt.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


import javax.sound.midi.*;

import javax.swing.*;


/**
 * A class to hold properties for a MidiManager
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.5 $
 */
public class MidiProperties {

    /** Are we playing sound */
    private boolean muted = true;

    /** The lowest note */
    private int lowNote = 0;

    /** The highest note */
    private int highNote = 127;

    /** The current instrument */
    private String instrumentName = null;

    /** For properties */
    private JTextField spanFld;

    /** For properties */
    private JComboBox instrumentBox;

    /** For properties */
    JCheckBox mutedCbx;

    /** For properties */
    private JTextField lowNoteFld;

    /** For properties */
    private JTextField highNoteFld;


    /**
     * Default ctor.
     */
    public MidiProperties() {}

    /**
     * Create a new MidiProperties
     *
     * @param instrumentName  name of the instrument
     * @param lowNote  low note value
     * @param highNote high note value
     * @param muted    true if muted
     */
    public MidiProperties(String instrumentName, int lowNote, int highNote,
                          boolean muted) {
        this.instrumentName = instrumentName;
        this.lowNote        = lowNote;
        this.highNote       = highNote;
        this.muted          = muted;
    }

    /**
     * Copy constructor
     *
     * @param that  the other properties object
     */
    public MidiProperties(MidiProperties that) {
        this.instrumentName = that.instrumentName;
        this.lowNote        = that.lowNote;
        this.highNote       = that.highNote;
        this.muted          = that.muted;
    }

    /**
     * Set the InstrumentIndex property.
     *
     * @param value The new value for InstrumentIndex
     */
    public void setInstrumentName(String value) {
        instrumentName = value;
    }

    /**
     * Get the Instrument property.
     *
     * @return The Instrument
     */
    public String getInstrumentName() {
        return instrumentName;
    }


    /**
     * Set the Muted property.
     *
     * @param value The new value for Muted
     */
    public void setMuted(boolean value) {
        muted = value;
    }

    /**
     * Get the Muted property.
     *
     * @return The Muted
     */
    public boolean getMuted() {
        return muted;
    }


    /**
     * Set the LowNote property.
     *
     * @param value The new value for LowNote
     */
    public void setLowNote(int value) {
        lowNote = value;
    }

    /**
     * Get the LowNote property.
     *
     * @return The LowNote
     */
    public int getLowNote() {
        return lowNote;
    }

    /**
     * Set the HighNote property.
     *
     * @param value The new value for HighNote
     */
    public void setHighNote(int value) {
        highNote = value;
    }

    /**
     * Get the HighNote property.
     *
     * @return The HighNote
     */
    public int getHighNote() {
        return highNote;
    }

    /**
     * Set the properties from another.
     *
     * @param that the other
     */
    public void setProperties(MidiProperties that) {
        this.instrumentName = that.instrumentName;
        this.lowNote        = that.lowNote;
        this.highNote       = that.highNote;
        this.muted          = that.muted;
    }

    /**
     * Show a Dialog for setting the properties.
     * @param  w  Window for anchor
     * @return true if properties changed
     */
    public boolean showPropertyDialog(Window w) {
        List comps = getPropertiesComponents(new ArrayList());
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
        if ( !GuiUtils.showOkCancelDialog(w, "MidiManager Properties",
                                          contents, null)) {
            return false;
        }
        return applyProperties();

    }

    /**
     * Apply properties
     *
     * @return success
     */
    public boolean applyProperties() {
        TwoFacedObject tfo = (TwoFacedObject) instrumentBox.getSelectedItem();
        instrumentName = tfo.toString();
        muted          = mutedCbx.isSelected();
        lowNote        = new Integer(lowNoteFld.getText().trim()).intValue();
        highNote       = new Integer(highNoteFld.getText().trim()).intValue();
        // allow for inverted range
        if (highNote < lowNote) {
            lowNote  = Math.max(lowNote, 0);
            highNote = Math.min(highNote, 127);
        } else {
            lowNote  = Math.min(lowNote, 127);
            highNote = Math.max(highNote, 0);
        }
        return true;
    }

    /**
     * Get the Components for setting the properties
     *
     * @param comps compnents to add to
     *
     * @return the list for this.
     */
    public List getPropertiesComponents(List comps) {
        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(mutedCbx = new JCheckBox("Muted",
                getMuted())));
        comps.add(GuiUtils.rLabel("Instrument: "));
        Vector         v           = new Vector();
        Instrument[]   instruments = MidiManager.getInstrumentList();

        TwoFacedObject selected = new TwoFacedObject("None", new Integer(-1));
        if(instruments!=null) {
            for (int i = 0; (i < instruments.length) && (i < 128); i++) {
                if(instruments[i]==null) continue;
                String name = instruments[i].getName();
                if(name==null) continue;
                TwoFacedObject tfo = new TwoFacedObject(name,
                                                        new Integer(i));
                if (name.equals(instrumentName)) {
                    selected = tfo;
                }
                v.add(tfo);
            }
        }
        comps.add(GuiUtils.left(instrumentBox = new JComboBox(v)));
        instrumentBox.setSelectedItem(selected);

        lowNoteFld  = new JTextField("" + getLowNote(), 5);
        highNoteFld = new JTextField("" + getHighNote(), 5);
        comps.add(GuiUtils.rLabel("Note Range:"));
        comps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    Misc.newList(
                        new JLabel("Low: "), lowNoteFld,
                        new JLabel("High: "), highNoteFld))));
        return comps;

    }
}

