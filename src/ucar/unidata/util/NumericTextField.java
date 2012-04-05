package ucar.unidata.util;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.KeyEvent;

import javax.swing.JTextField;

/**
 * NumericTextField class which only allows number in the text field.
 */
@SuppressWarnings("serial")
public class NumericTextField extends JTextField {

    /** The badchars. */
    private static final String badchars = "`~!@#$%^&*()_+=\\|\"':;?/><, ";

    /**
     * {@inheritDoc}
     */
    public void processKeyEvent(final KeyEvent ev) {
        final char c = ev.getKeyChar();

        if ((Character.isLetter(c) &&!ev.isAltDown()) || (badchars.indexOf(c) > -1)) {
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
