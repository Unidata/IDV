/*
 * $Id: ShapefileToLocation.java,v 1.1 2005/12/15 22:10:21 jeffmc Exp $
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

package ucar.unidata.apps.trex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import ucar.unidata.gis.*;
import ucar.unidata.gis.shapefile.*;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

/**
 */
public class ShapefileToLocation {

    public static void main(String[]args) throws Exception {
        List names = new ArrayList();
        List files = new ArrayList();

        for(int i=0;i<args.length;i++) {
            StringBuffer sb = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            String name = IOUtil.getFileTail(IOUtil.stripExtension(args[i]));
            name = StringUtil.replace(name,"_"," ");
            names.add(name);
            sb.append("<stationtable name=\"" + name +"\">\n");
            EsriShapefile    shapefile = new EsriShapefile(args[i]);
            List features = shapefile.getFeatures();
            for(int featureIdx=0;featureIdx<features.size();featureIdx++) {
                EsriShapefile.EsriFeature feature = (EsriShapefile.EsriFeature) features.get(featureIdx);
                if(!(feature instanceof  EsriShapefile.EsriPoint)) continue;
                Iterator parts = feature.getGisParts();
                while(parts.hasNext()) {
                    GisPart part = (GisPart) parts.next();
                    double[] x = part.getX();
                    double[] y = part.getY();
                    if(features.size()>1)
                        sb.append("<station name=\""+ name+" " +(featureIdx+1) +"\" lat=\""+ y[0]+"\" lon=\"" + x[0] +"\"/>\n");
                    else
                        sb.append("<station name=\""+ name +"\" lat=\""+ y[0]+"\" lon=\"" + x[0] +"\"/>\n");
                    break;
                }
            }
            sb.append("</stationtable>\n");
            String file = IOUtil.stripExtension(args[i])+".xml";
            files.add(file);
            System.err.println ("Writing " + file);
            IOUtil.writeFile(file, sb.toString());
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<stationtables>\n");
        for(int i=0;i<names.size();i++) {
            String name = (String) names.get(i);
            String file = (String) files.get(i);
            sb.append("<stationtable category=\"TREX\" name=\"" + name +"\"  href=\"/ucar/unidata/apps/trex/" + file +"\"/>\n");
        }
        sb.append("</stationtables>\n");
        IOUtil.writeFile("places.xml", sb.toString());
    }


}


