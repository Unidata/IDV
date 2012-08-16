/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;


import java.awt.event.KeyEvent;

import javax.swing.JTextField;


/**
 * NumericTextField class which only allows number in the text field.
 */
@SuppressWarnings("serial")
public class NumericTextField extends JTextField {

    /** Bad characters. */
    private static final String BADCHARS = "`~!@#$%^&*()_+=\\|\"':;?/><, ";

    /**
     * {@inheritDoc}
     *
     */
    public void processKeyEvent(final KeyEvent ev) {
        final char c = ev.getKeyChar();

        if ((Character.isLetter(c) && !ev.isAltDown())
                || (BADCHARS.indexOf(c) > -1)) {
            ev.consume();
            return;
        }

        if ((c == '-') && (getDocument().getLength() > 0)) {
            ev.consume();
        } else {
            super.processKeyEvent(ev);
        }
    }
}