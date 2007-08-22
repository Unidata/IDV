/*
 * $Id: HelpViewer.java,v 1.5 2007/07/06 20:45:30 jeffmc Exp $
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

package ucar.unidata.ui;


import ucar.unidata.ui.Help;


/**
 * Class for bringing up example help
 */
public class HelpViewer {

    /**
     * Run as "java -Xmx128m ucar.unidata.idv.IdvHelp <default target>
     * @param args  alternative target.
     */
    public static void main(String[] args) {
        String what  = "idv.introduction";
        String where = "/auxdata/docs/userguide";
        if (args.length == 2) {
            where = args[0];
            what  = args[1];
        } else if (args.length == 1) {
            what = args[0];
        }
        Help.setTopDir(where);
        /*        for (int i=0;i<args.length;i++) {
            System.err.println (args[i]+" valid=" + Help.getDefaultHelp().isValidID (args[i]));
            }*/
        Help.getDefaultHelp().gotoTarget(what);
    }
}

