/*
 * $Id: ViewpointDialog.java,v 1.13 2007/05/04 14:17:36 dmurray Exp $
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

package ucar.unidata.view.geoloc;



import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


/**
 * A widget to get viewpoint (azimuth, tilt) info from the user.
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class ViewpointDialog extends JDialog implements ActionListener {

    /** input fields for azimuth and tilt */
    private JTextField azimuthFld, tiltFld;

    /** flag for ok or cancel */
    private boolean ok;

    /** buttons */
    private JButton okButton, cancelButton;

    /** the viewpoint control */
    private ViewpointControl viewpointControl;

    /**
     * Create a new dialog for setting the viewpoing
     *
     *
     * @param vc the viewpoint control
     * @param parent   parent for modal dialog
     *
     */
    public ViewpointDialog(ViewpointControl vc, JFrame parent) {
        super(parent, new String("Viewpoint Settings"), false);
        this.viewpointControl = vc;

        GuiUtils.tmpInsets    = new Insets(5, 5, 0, 0);
        JPanel panel = GuiUtils.doLayout(new Component[]{
                           GuiUtils.rLabel("Azimuth from North: "),
                           azimuthFld = new JTextField("", 7),
                           GuiUtils.rLabel("Tilt down from top: "),
                           tiltFld = new JTextField("", 7) }, 2,
                               GuiUtils.WT_NY, GuiUtils.WT_N);

        azimuthFld.setActionCommand(GuiUtils.CMD_OK);
        azimuthFld.addActionListener(this);
        tiltFld.setActionCommand(GuiUtils.CMD_OK);
        tiltFld.addActionListener(this);
        getContentPane().add("Center", GuiUtils.inset(panel, 5));

        getContentPane().add("South",
                             GuiUtils.makeApplyOkCancelButtons(this));
        pack();
        setLocation(100, 100);
    }  // end ContLevelDialog cstr

    /**
     * Handle user click on OK or other(cancel) button.  Closes the
     * dialog.
     *
     * @param evt  event to handle
     */
    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            try {
                double azmo = Misc.parseNumber(azimuthFld.getText());
                double tilt = Misc.parseNumber(tiltFld.getText());
                viewpointControl.setViewpointInfo(new ViewpointInfo(azmo,
                        tilt));
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Invalid azimuth or tilt value");
                return;
            }
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            setVisible(false);
        }
    }


    /**
     * Show the dialog and get the user input
     *
     * @param transfer   initial values
     */
    public void showDialog(ViewpointInfo transfer) {
        azimuthFld.setText(Misc.format(transfer.azimuth));
        tiltFld.setText(Misc.format(transfer.tilt));
        this.setVisible(true);
    }
}
