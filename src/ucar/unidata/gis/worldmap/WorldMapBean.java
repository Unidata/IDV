/*
 * $Id: WorldMapBean.java,v 1.9 2005/05/13 18:29:53 jeffmc Exp $
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

package ucar.unidata.gis.worldmap;



import javax.swing.*;


/** Wraps the default WorldMap object into a MapBean */

public class WorldMapBean implements ucar.unidata.gis.MapBean {

    /** _more_ */
    private ucar.unidata.view.Renderer rend;

    /** _more_ */
    private ucar.unidata.util.ListenerManager lm;

    /**
     * _more_
     *
     */
    public WorldMapBean() {
        rend = new WorldMap();

        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.NewRendererListener",
            "ucar.unidata.view.NewRendererEvent", "actionPerformed");
    }

    /**
     * _more_
     * @return _more_
     */
    public ucar.unidata.view.Renderer getRenderer() {
        return rend;
    }

    /**
     * Construct the Action that is called when its menu item/buttcon is selected.
     * Typically this routine is only called once when the bean is added.
     * The Action itself is called whenever the menu/buttcon is selected.
     * @return the Action to be called.
     */
    public javax.swing.Action getAction() {
        javax.swing.ImageIcon icon =
            ucar.unidata.ui.BAMutil.getIcon("WorldMap", true);
        //javax.swing.ImageIcon iconR = ucar.unidata.ui.BAMutil.getIcon("WorldSel", true);

        AbstractAction useWorldMap = new AbstractAction("WorldMap", icon) {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                lm.sendEvent(
                    new ucar.unidata.view.NewRendererEvent(
                        WorldMapBean.this, rend));
            }
        };
        useWorldMap.putValue(Action.SHORT_DESCRIPTION, "use World Map");
        //useWorldMap.putValue(ucar.unidata.ui.BAMutil.ROLLOVER_ICON, iconR);

        return useWorldMap;
    }

    /**
     * _more_
     *
     * @param l
     */
    public void addNewRendererListener(
            ucar.unidata.view.NewRendererListener l) {
        lm.addListener(l);
    }

    /**
     * _more_
     *
     * @param l
     */
    public void removeNewRendererListener(
            ucar.unidata.view.NewRendererListener l) {
        lm.removeListener(l);
    }
}







