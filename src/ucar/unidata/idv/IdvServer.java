/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv;


import java.io.File;


import java.util.Hashtable;


/**
 * @author IDV development team
 */

public class IdvServer {

    /** _more_          */
    private Object MUTEX = new Object();

    /** _more_          */
    private MyIdv idv;

    /** _more_          */
    private int callCnt = 0;

    /** _more_          */
    private File userDir;


    /**
     * _more_
     *
     * @param userDir _more_
     *
     * @throws Exception _more_
     */
    public IdvServer(File userDir) throws Exception {
        idv = new MyIdv(((userDir == null)
                         ? null
                         : userDir.toString()));
        idv.getStateManager().putProperty(IdvConstants.PROP_MAP_MAP_LEVEL,
                                          "0");
        this.userDir = userDir;
    }


    /**
     * _more_
     *
     * @param isl _more_
     *
     * @throws Exception _more_
     */
    public void evaluateIsl(StringBuffer isl) throws Exception {
        evaluateIsl(isl, new Hashtable());
    }


    /**
     * _more_
     *
     * @param isl _more_
     * @param properties _more_
     *
     * @throws Exception _more_
     */
    public void evaluateIsl(StringBuffer isl, Hashtable properties)
            throws Exception {
        synchronized (MUTEX) {
            //Make a new one every 100 calls
            if (callCnt++ > 100) {
                idv.cleanup();
                idv = new MyIdv(((userDir == null)
                                 ? null
                                 : userDir.toString()));
                idv.getStateManager().putProperty(
                    IdvConstants.PROP_MAP_MAP_LEVEL, "0");
                callCnt = 0;
            }
            idv.getImageGenerator().processScriptFile("xml:" + isl,
                    properties);
            idv.cleanup();
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public MyIdv getIdv() {
        return idv;
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Jan 12, '10
     * @author         Enter your name here...    
     */
    public class MyIdv extends IntegratedDataViewer {

        /**
         * _more_
         *
         * @param userDir _more_
         *
         * @throws Exception _more_
         */
        public MyIdv(String userDir) throws Exception {
            super(((userDir == null)
                   ? new String[] {}
                   : new String[] { ARG_USERPATH, userDir }), false);
        }
    }



}
