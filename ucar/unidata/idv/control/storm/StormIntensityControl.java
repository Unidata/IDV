package ucar.unidata.idv.control.storm;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import java.util.List;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.rmi.RemoteException;

import visad.georef.EarthLocation;
import visad.Real;
import visad.DisplayEvent;
import visad.Unit;
import visad.VisADException;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Mar 10, 2009
 * Time: 1:03:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormIntensityControl extends DisplayControlImpl {
    private boolean displayOnly = false;
    DisplayEvent event;
    EarthLocation eloc;
    /**
     * Should we handle display events
     *
     * @return Ok to handle events
     */
    protected boolean canHandleEvents() {
        if (displayOnly
                || !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }

    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {

        int id = event.getId();
        this.event = event;
        InputEvent inputEvent = event.getInputEvent();

        if ((id == DisplayEvent.MOUSE_MOVED) ) {
            return;
        }


        if ( !canHandleEvents()) {
            return;
        }

        try {

             
            if (id == DisplayEvent.MOUSE_PRESSED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }

                eloc = toEarth(event);


            }else if (id == DisplayEvent.MOUSE_RELEASED) {
                addADOT(eloc);
            }

        } catch (Exception e) {
            logException("Handling display event changed", e);
        }


    }

      /**
     * Map the screen x/y of the event to an earth location
     *
     * @param event The event
     *
     * @return The earth location
     *
     * @throws java.rmi.RemoteException When bad things happen
     * @throws visad.VisADException When bad things happen
     */
    public EarthLocation toEarth(DisplayEvent event)
            throws VisADException, RemoteException {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null)
               ? null
               : d.getEarthLocation(toBox(event));
    }


    public void addADOT(EarthLocation el) {
        final JDialog dialog   = GuiUtils.createDialog("RUN ADOT", true);
        String question ="Please select storm center";
        String label = "latitude: ";
        String label1 = "longitude: ";
        final JTextField field    = new JTextField( "", 10 );
        final JTextField field1    = new JTextField( "", 10 );


        ObjectListener listener = new ObjectListener(new Boolean(false)) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field) || cmd.equals(GuiUtils.CMD_OK)) {
                    theObject = new Boolean(true);
                } else {
                    theObject = new Boolean(false);
                }
                dialog.setVisible(false);
            }
        };
        ObjectListener   listener1 = new ObjectListener(new Boolean(false)) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field1) || cmd.equals(GuiUtils.CMD_OK)) {
                    theObject = new Boolean(true);
                } else {
                    theObject = new Boolean(false);
                }
                dialog.setVisible(false);
            }
        };
        field.addActionListener(listener);
        field.addActionListener(listener1);
        List comps = new ArrayList();

        comps.add(GuiUtils.left(GuiUtils.inset(new JLabel(question), 4)));

        JPanel topb = GuiUtils.doLayout(new Component[] {
                                  GuiUtils.rLabel(label),
                                  GuiUtils.hbox(field,
                                                GuiUtils.filler()),
                                  GuiUtils.rLabel(label1),
                                  GuiUtils.hbox(field1,
                                                GuiUtils.filler()) }, 4,
                                                    GuiUtils.WT_NYNY, GuiUtils.WT_N);

        comps.add( topb);


        JComponent contents = GuiUtils.inset(GuiUtils.centerBottom(GuiUtils.vbox(comps),
                                  GuiUtils.makeOkCancelButtons(listener1)), 4);

        GuiUtils.packDialog(dialog, contents);
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();

        Point ctr = new Point(ss.width / 2 - 100, ss.height / 2 - 100);
        dialog.setLocation(ctr);
        dialog.setVisible(true);


    }

}
