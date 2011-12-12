/*
 * $Id: SoundingSelectionList.java,v 1.12 2005/05/13 18:33:38 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.awt.Component;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionListener;

import ucar.unidata.data.sounding.SoundingOb;


/**
 * Provides support for the selection of soundings via a GUI widget.  Sounding
 * identifiers are displayed in a scrollable list from which the user may select
 * the soundings to be displayed.
 *
 * @author Steven R. Emmerson
 * @version $Id: SoundingSelectionList.java,v 1.12 2005/05/13 18:33:38 jeffmc Exp $
 */
public class SoundingSelectionList extends JList {

    /** panel for list */
    private JPanel jPanel;

    /**
     * Constructs from nothing.
     */
    public SoundingSelectionList() {

        setCellRenderer(new DefaultListCellRenderer() {

            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {

                if (value instanceof SoundingOb) {
                    SoundingOb soundingOb = (SoundingOb) value;

                    value = soundingOb.getStationIdentifier().toString()
                            + " " + soundingOb.getTimestamp();
                }

                return super.getListCellRendererComponent(list, value, index,
                                                          isSelected,
                                                          cellHasFocus);
            }
        });
        setPrototypeCellValue("88888 8888-88-88 88:88:88Z");

        JScrollPane jScrollPane = new JScrollPane(this);

        jPanel = new JPanel();

        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.add(jScrollPane);
        setModel(new SoundingObListModel());
    }

    /**
     * Returns the Java Swing component of this widget.  NOTE: This is
     * <em>not</em> necessarilly the same as this JList instance because,
     * for example, this JList may be enclosed by a ScrollPane.
     * @return                  The Java Swing component of this widget.
     */
    public JComponent getComponent() {
        return jPanel;
    }

    /**
     * Removes the currently-selected soundings from this instance.
     */
    public synchronized void clearSelectedSoundings() {

        SoundingObListModel model   = getSoundingObListModel();
        int[]               indices = getSelectedIndices();

        /*
         * NB: The indices are traversed in decreasing order to obviate problems
         * caused by list compaction.
         */
        for (int i = indices.length; --i >= 0; ) {
            model.removeSounding(indices[i]);
        }
    }

    /**
     * Removes all soundings from this instance.
     */
    public synchronized void clearSoundings() {
        getSoundingObListModel().clearSoundings();
    }

    /**
     * Add a sounding to this instance.
     * @param soundingOb        The sounding to be added.
     */
    public synchronized void addSounding(SoundingOb soundingOb) {
        getSoundingObListModel().addSounding(soundingOb);
    }

    /**
     * Returns the number of soundings in this instance.
     * @return                  The number of soundings.
     */
    public int soundingCount() {
        return getSoundingObListModel().getSize();
    }

    /**
     * Returns the sounding-observation list-model of this instance.
     * @return                  The sounding-observation list-model of this
     *                          instance.
     */
    private SoundingObListModel getSoundingObListModel() {
        return (SoundingObListModel) getModel();
    }
}







