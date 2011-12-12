/*
 * $Id: DrawingCommand.java,v 1.7 2007/04/16 20:53:46 jeffmc Exp $
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

package ucar.unidata.idv.control.drawing;


import ucar.unidata.util.Misc;


/**
 * Class DrawingCommand.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.7 $
 */
public class DrawingCommand {

    /** flags */
    int flags;

    /** label */
    String label;

    /** message */
    String message;

    /** toolbar icon */
    String iconPath;

    /**
     * ctor
     */
    public DrawingCommand() {}


    /**
     * ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath Toolbar icon
     */
    public DrawingCommand(String label, String message, String iconPath) {
        this(label, message, iconPath, 0);
    }

    /**
     * ctor
     *
     * @param label Label
     * @param message Message
     * @param iconPath Toolbar icon
     * @param flags Flags
     */
    public DrawingCommand(String label, String message, String iconPath,
                          int flags) {
        this.label    = label;
        this.message  = message;
        this.iconPath = iconPath;
        this.flags    = flags;
    }



    /**
     * Is this command capable of the task defined in mask
     *
     * @param mask The mask
     *
     * @return Is capable
     */
    public boolean isCapable(int mask) {
        return ((flags & mask) != 0);
    }






    /**
     * Equals
     *
     * @param o Object
     *
     * @return Is equals
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ( !(o instanceof DrawingCommand)) {
            return false;
        }
        DrawingCommand that = (DrawingCommand) o;
        return Misc.equals(label, that.label)
               && Misc.equals(iconPath, that.iconPath)
               && Misc.equals(message, that.message);
    }



    /**
     * Set the IconPath property.
     *
     * @param value The new value for IconPath
     */
    public void setIconPath(String value) {
        iconPath = value;
    }

    /**
     * Get the IconPath property.
     *
     * @return The IconPath
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }




    /**
     * Set the Message property.
     *
     * @param value The new value for Message
     */
    public void setMessage(String value) {
        message = value;
    }

    /**
     * Get the Message property.
     *
     * @return The Message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the Flags property.
     *
     * @param value The new value for Flags
     */
    public void setFlags(int value) {
        flags = value;
    }

    /**
     * Get the Flags property.
     *
     * @return The Flags
     */
    public int getFlags() {
        return flags;
    }





}

