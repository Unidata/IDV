/*
 * $Id: TransectDrawingControl.java,v 1.41 2006/12/28 19:50:59 jeffmc Exp $
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

package ucar.unidata.idv.control.editor;


import ucar.unidata.idv.control.drawing.*;

import ucar.unidata.ui.CommandManager;

import ucar.visad.display.*;

import visad.*;

import java.rmi.RemoteException;



/**
 * Class EditCommand _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FieldCommand extends ucar.unidata.ui.Command {

    /** _more_ */
    RadarEditor editor;

    /** _more_ */
    FieldImpl oldSlice;

    /** _more_ */
    FieldImpl newSlice;

    /**
     * _more_
     *
     * @param editor _more_
     * @param oldSlice _more_
     * @param newSlice _more_
     */
    public FieldCommand(RadarEditor editor, FieldImpl oldSlice,
                        FieldImpl newSlice) {
        this.editor   = editor;
        this.oldSlice = oldSlice;
        this.newSlice = newSlice;
    }


    /**
     * _more_
     */
    public void doCommand() {
        try {
            editor.setField((FieldImpl) newSlice);
        } catch (Exception exc) {
            editor.logException("Error", exc);
        }
    }



    /**
     * _more_
     */
    public void undoCommand() {
        try {
            editor.setField((FieldImpl) oldSlice);
        } catch (Exception exc) {
            editor.logException("Error", exc);
        }
    }

}


