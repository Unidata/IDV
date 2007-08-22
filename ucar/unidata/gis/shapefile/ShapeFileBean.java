/*
 * $Id: ShapeFileBean.java,v 1.15 2005/10/20 20:46:29 jeffmc Exp $
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

package ucar.unidata.gis.shapefile;



import java.awt.*;

import javax.swing.*;

import ucar.unidata.util.Resource;
import ucar.unidata.view.Renderer;
import ucar.unidata.view.NewRendererListener;


/**
 * Wraps shapefile maps into a MapBean
 *
 * @author John Caron
 * @version $Id: ShapeFileBean.java,v 1.15 2005/10/20 20:46:29 jeffmc Exp $
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
     * contructor for a specific shapefile
     *
     * @param shapefileName
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
     * _more_
     */
    private void fetchMap() {
        long startTime = System.currentTimeMillis();
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
     * _more_
     */
    private void init() {
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.NewRendererListener",
            "ucar.unidata.view.NewRendererEvent", "actionPerformed");
    }

    /**
     * get the current renderer
     * @return _more_
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
     * _more_
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
     * _more_
     *
     * @param l
     */
    public void addNewRendererListener(NewRendererListener l) {
        lm.addListener(l);
    }

    /**
     * _more_
     *
     * @param l
     */
    public void removeNewRendererListener(NewRendererListener l) {
        lm.removeListener(l);
    }
}

/* Change History:
   $Log: ShapeFileBean.java,v $
   Revision 1.15  2005/10/20 20:46:29  jeffmc
   Move FileManager

   Revision 1.14  2005/05/13 18:29:48  jeffmc
   Clean up the odd copyright symbols

   Revision 1.13  2005/03/10 18:38:53  jeffmc
   jindent and javadoc

   Revision 1.12  2004/02/27 21:22:43  jeffmc
   Lots of javadoc warning fixes

   Revision 1.11  2004/01/29 17:36:08  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.10  2000/09/27 19:44:25  caron
   move to auxdata

   Revision 1.9  2000/08/18 04:15:28  russ
   Licensed under GNU LGPL.

   Revision 1.8  2000/05/26 21:19:19  caron
   new GDV release

   Revision 1.7  2000/02/11 01:24:45  caron
   add getDataProjection()

   Revision 1.6  2000/02/10 17:45:17  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/







