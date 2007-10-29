/*
 * $Id: IdvXmlUi.java,v 1.54 2007/08/16 14:08:22 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
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


import ucar.unidata.idv.*;
import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.LogUtil;

import javax.swing.*;


/**
 */

public class IdvComponentHolder extends ComponentHolder {

    /** _more_          */
    IntegratedDataViewer idv;

    /** _more_          */
    private Object object;

    /**
     * _more_
     */
    public IdvComponentHolder() {}

    /**
     * _more_
     *
     * @param idv _more_
     * @param object _more_
     */
    public IdvComponentHolder(IntegratedDataViewer idv, Object object) {
        super("");
        this.idv    = idv;
        this.object = object;
        if (object instanceof DisplayControl) {
            ((DisplayControlImpl) object).setComponentHolder(this);
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        String name = getNameFromObject();
        if (name == null) {
            return getTypeName();
        }
        return name;
    }

    /**
     * _more_
     */
    public void doRemove() {
        if ((object != null) && (idv != null)) {
            try {
                if (object instanceof ViewManager) {
                    idv.getVMManager().removeViewManager(
                        (ViewManager) object);
                } else if (object instanceof DisplayControl) {
                    ((DisplayControl) object).doRemove();
                } else if (object instanceof DataSelector) {
                    idv.getIdvUIManager().removeDataSourceHolder(
                        (DataSelector) object);
                }

            } catch (Exception exc) {
                LogUtil.logException("Error removing component holder", exc);
            }
            idv    = null;
            object = null;
        }
        super.doRemove();
    }


    /**
     * _more_
     *
     * @param displayControl _more_
     */
    public void removeDisplayControl(DisplayControl displayControl) {
        if (displayControl == object) {
            object = null;
            doRemove();
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getCategory() {
        if (object instanceof ViewManager) {
            return "Views";
        }
        if (object instanceof DisplayControl) {
            return "Displays";
        }
        if (object instanceof DataSelector) {
            return "Tools";
        }
        return super.getCategory();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getNameFromObject() {
        if (object instanceof ViewManager) {
            return ((ViewManager) object).getName();
        }
        if (object instanceof DisplayControl) {
            return ((DisplayControl) object).getLabel();
        }
        if (object instanceof DataSelector) {
            return "Field Selector";
        }
        return super.getName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTypeName() {
        if (object instanceof ViewManager) {
            return "View Manager";
        }
        if (object instanceof DisplayControl) {
            return "Display Control";
        }
        if (object instanceof DataSelector) {
            return "Field Selector";
        }
        return super.getTypeName();

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeContents() {
        if (object instanceof ViewManager) {
            return (JComponent) ((ViewManager) object).getContents();
        }
        if (object instanceof DisplayControlImpl) {
            JComponent inner =
                (JComponent) ((DisplayControlImpl) object).getOuterContents();
            return inner;
        }
        if (object instanceof DataSelector) {
            return ((DataSelector) object).getContents();
        }
        return new JLabel("Unknwon object:" + object);
    }



    /**
     * Set the Object property.
     *
     * @param value The new value for Object
     */
    public void setObject(Object value) {
        object = value;
    }

    /**
     * Get the Object property.
     *
     * @return The Object
     */
    public Object getObject() {
        return object;
    }


    /**
     * Set the Idv property.
     *
     * @param value The new value for Idv
     */
    public void setIdv(IntegratedDataViewer value) {
        idv = value;
    }

    /**
     * Get the Idv property.
     *
     * @return The Idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

}

