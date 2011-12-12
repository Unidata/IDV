/*
 * $Id: BooleanProperty.java,v 1.14 2007/03/12 14:09:50 jeffmc Exp $
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


import java.awt.event.*;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.*;


/**
 * Provides facilities for boolean properties.
 * @author Metapps development group.
 * @version $Revision: 1.14 $ $Date: 2007/03/12 14:09:50 $
 */





public class BooleanProperty {

    /** _more_ */
    protected boolean okToPropagatePropertyChange = true;

    /** _more_ */
    String id;

    /** _more_ */
    String name;

    /** _more_ */
    String desc;

    /** _more_ */
    Boolean value;

    /** _more_ */
    boolean dflt;

    /** _more_ */
    List menuItems = new ArrayList();

    /** _more_ */
    boolean haveBeenInitialized = false;



    /**
     * Copy constructor
     *
     *
     * @param that the object to copy
     *
     */
    public BooleanProperty(BooleanProperty that) {
        this(that.id, that.name, that.desc, that.value, that.dflt);
    }



    /**
     * _more_
     *
     * @param id
     * @param name
     * @param dflt
     *
     */
    public BooleanProperty(String id, String name, boolean dflt) {
        this(id, name, null, dflt);
    }


    /**
     * _more_
     *
     * @param id
     * @param name
     * @param desc
     * @param dflt
     *
     */
    public BooleanProperty(String id, String name, String desc,
                           boolean dflt) {
        this(id, name, desc, null, dflt);
    }


    /**
     * _more_
     *
     * @param id
     * @param name
     * @param desc
     * @param value _more_
     * @param dflt
     *
     */
    public BooleanProperty(String id, String name, String desc,
                           Boolean value, boolean dflt) {



        if ((desc == null) || (desc.length() == 0)) {
            desc = "Toggle " + name.toLowerCase() + " on/off";
        }
        this.id    = id;
        this.name  = name;
        this.desc  = desc;
        this.dflt  = dflt;
        this.value = value;
    }


    /**
     * _more_
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getDefault() {
        return dflt;
    }


    /**
     * _more_
     *
     * @param d _more_
     */
    public void setDefault(boolean d) {
        dflt = d;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getValue() {
        if (value == null) {
            return getDefault();
        }
        return value.booleanValue();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasValue() {
        return value != null;
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean getHaveBeenInitialized() {
        return haveBeenInitialized;
    }


    /**
     * _more_
     *
     * @param newValue
     */
    public final void setValue(boolean newValue) {
        try {
            haveBeenInitialized = true;
            setValueInner(newValue);
        } catch (Exception excp) {
            LogUtil.logException("setting value of " + name, excp);
        }
    }


    /**
     * _more_
     *
     * @param newValue
     *
     * @throws Exception
     */
    public void setValueInner(boolean newValue) throws Exception {
        value                       = new Boolean(newValue);
        okToPropagatePropertyChange = false;
        for (int i = 0; i < menuItems.size(); i++) {
            ((JCheckBoxMenuItem) menuItems.get(i)).setSelected(newValue);
        }
        okToPropagatePropertyChange = true;
    }


    /**
     * _more_
     *
     * @param mi
     */
    public void addItem(JCheckBoxMenuItem mi) {
        menuItems.add(mi);
    }

    /**
     * _more_
     * @return _more_
     */
    public JCheckBoxMenuItem createCBMI() {
        final JCheckBoxMenuItem mi = new JCheckBoxMenuItem(name, getValue());
        mi.setToolTipText(desc);
        addItem(mi);
        mi.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (okToPropagatePropertyChange) {
                    setValue(mi.isSelected());
                }
            }
        });
        return mi;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return id + "=" + getValue();
    }


    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof BooleanProperty)) {
            return false;
        }
        BooleanProperty that = (BooleanProperty) obj;
        return Misc.equals(this.id, that.id)
               && Misc.equals(this.name, that.name)
               && Misc.equals(this.desc, that.desc)
               && Misc.equals(this.value, that.value)
               && (this.dflt == that.dflt);
    }



}

