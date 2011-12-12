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

import visad.VisADException;

import java.rmi.RemoteException;

import javax.swing.*;



/**
 *
 * Just shows the "notes" field.
 * @author Jeff McWhirter
 * @version $Revision: 1.9 $
 */
public class NoteControl extends DisplayControlImpl {

    /**
     * Cstr; does nothing. See init() for creation actions.
     */
    public NoteControl() {
        setShowNoteText(true);
    }

    /**
     * Add note text to the main panel
     *
     * @param mainPanel    main panel
     * @param noteWrapper  wrapper for the note
     */
    protected void addNoteText(JPanel mainPanel, JComponent noteWrapper) {
        mainPanel.add("Center", noteWrapper);
    }

    /**
     * Initialize this class
     *
     * @param dataChoice   data choice
     * @return true
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        return true;
    }


}
