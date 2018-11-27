/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.gis.shapefile;


import ucar.unidata.util.Resource;
import ucar.unidata.view.NewRendererListener;
import ucar.unidata.view.Renderer;



import javax.swing.AbstractAction;
import javax.swing.Action;


/**
 * Wraps shapefile maps into a MapBean
 *
 * @author John Caron
 */

public class ShapeFileBean implements ucar.unidata.gis.MapBean {

    /** _more_ */
    private ucar.unidata.util.ListenerManager lm;

    /** _more_ */
    private ucar.unidata.util.FileManager shapeManage = null;

    /** _more_ */
    private String shapefileName;

    /** _more_ */
    private Renderer rend = null;

    /** _more_ */
    private AbstractAction action;

    /**
     * contructor for a specific shapefile.
     *
     * @param shapefileName the shapefile name
     */
    public ShapeFileBean(String shapefileName) {
        this.shapefileName = shapefileName;

        javax.swing.ImageIcon icon =
            ucar.unidata.ui.BAMutil.getIcon("DetailedMap", true);
        action = new AbstractAction("Countries", icon) {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (rend == null) {
                    fetchMap();
                }
                if (rend != null) {
                    lm.sendEvent(new ucar.unidata.view.NewRendererEvent(this,
                            rend));
                }
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "use Detailed map");

        init();
    }

    /**
     * Fetch map.
     */
    private void fetchMap() {
        long                startTime = System.currentTimeMillis();
        java.io.InputStream is = Resource.getFileResource(null,
                                     shapefileName);
        if (is == null) {
            System.err.println("ShapeFileBean read failed on resource "
                               + shapefileName);
        } else {
            rend = EsriShapefileRenderer.factory(is);
        }

        if (ucar.unidata.util.Debug.isSet("timing.readShapefile")) {
            long tookTime = System.currentTimeMillis() - startTime;
            System.out.println("timing.readShapefile: " + tookTime * .001
                               + " seconds");
        }
    }

    /** contructor for letting user choose a shapefile with FileManager */
    public ShapeFileBean() {

        javax.swing.ImageIcon icon =
            ucar.unidata.ui.BAMutil.getIcon("Shapefile", true);
        action = new AbstractAction("ShapeFile", icon) {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                chooseShapefile();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "select ShapeFile");

        init();
    }

    /**
     * Init.
     */
    private void init() {
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.NewRendererListener",
            "ucar.unidata.view.NewRendererEvent", "actionPerformed");
    }

    /**
     * get the current renderer.
     *
     * @return the renderer
     */
    public Renderer getRenderer() {
        return rend;
    }

    /**
     * Construct the Action that is called when this bean's menu item/buttcon is selected.
     *  @return the Action to be called.
     */
    public javax.swing.Action getAction() {
        return action;
    }

    /**
     * Choose shapefile.
     */
    private void chooseShapefile() {
        if (shapeManage == null) {
            shapeManage = new ucar.unidata.util.FileManager(null);
        }

        try {
            shapefileName = shapeManage.chooseFilename();
            if (shapefileName != null) {
                //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                rend = EsriShapefileRenderer.factory(shapefileName);
                // setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (rend != null) {
                    lm.sendEvent(new ucar.unidata.view.NewRendererEvent(this,
                            rend));
                }
            }
        } catch (SecurityException e) {
            System.out.println("shapeManage.chooseFilename: security error");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addNewRendererListener(NewRendererListener l) {
        lm.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNewRendererListener(NewRendererListener l) {
        lm.removeListener(l);
    }
}
