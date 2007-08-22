/*
 * $Id: WxTextDisplayControl.java,v 1.3 2007/06/04 20:24:06 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.unidata.apps.example;


import ucar.unidata.data.DataChoice;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.metdata.NamedStation;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import javax.swing.*;


/**
 * A class for displaying weather text bulletins
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WxTextDisplayControl extends DisplayControlImpl {

    /** the text display pane */
    private JEditorPane myEditor;

    /** the text display contents */
    private String textContents = null;

    /** the highlighted text */
    private String highlightedText = "";

    /** the text field for the user */
    private JTextField searchTextField = null;

    /** station */
    private NamedStation station;

    /**
     * Default bean constructor
     */
    public WxTextDisplayControl() {
        // setAttributeFlags(FLAG_COLOR);
    }

    /**
     * Make Gui contents
     *
     * @return User interface contents
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        myEditor = new JEditorPane();
        myEditor.setContentType("text/html");
        if (textContents != null) {
            myEditor.setText(textContents);
        }
        int width  = 300;
        int height = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(myEditor, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        JPanel p = GuiUtils.center(scroller);
        /*
        searchTextField = new JTextField(highlightedText);
        searchTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                highlightedText = ((JTextField) e.getSource()).getText();
                highlightText();
            }
        });
        JComponent bottom = GuiUtils.hbox(GuiUtils.rLabel("Search: "),
                                          searchTextField);
        p = GuiUtils.centerBottom(scroller, bottom);
        */
        return p;

    }

    /**
     * Initialize this class with the supplied {@link DataChoice}.
     *
     * @param dataChoice   choice to describe the data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !setData(dataChoice)) {
            return false;
        }

        textContents =
            ((visad.Text) (getDataInstance().getDataChoice().getData(
                getDataSelection(), getRequestProperties()))).getValue();
        /*
        station =
            (NamedStation) dataChoice.getProperty(WxTextChooser.PROP_STATION);
        if (station != null) {
            EarthLocationLite elt = new EarthLocationLite(
                                        station.getLatitude(),
                                        station.getLongitude(),
                                        station.getAltitude().getValue(
                                            CommonUnit.meter));
            SelectorPoint p = new SelectorPoint("location", elt);
            p.setManipulable(false);
            addDisplayable(p, FLAG_COLOR);
        }
        */
        return true;
    }

    /**
     * Highlight the text.
     */
    private void highlightText() {
        highlightText(highlightedText, getColor());
    }

    /**
     * Hightlight the string in the text with the color.
     * @param text text to highlight
     * @param color color to highlight with
     */
    private void highlightText(String text, Color color) {
        if ((text == null) || (textContents == null) || (myEditor == null)
                || (color == null)) {
            return;
        }
        if (text.equals("")) {
            return;
        }
        String hiLight = "<span style = \"background-color:"
                         + StringUtil.toHexString(getColor()) + "\">" + text
                         + "</span>";
        String hiText = textContents.replaceAll(text, hiLight);
        myEditor.setText(hiText);
    }

    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     public String getColorWidgetLabel() {
         return "Highlight Color";
     }
     */

    /**
     * If the color  is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLOR
     *
     * @throws RemoteException
     * @throws VisADException
     protected void applyColor() throws VisADException, RemoteException {
        highlightText();
        super.applyColor();
     }
     */

}

