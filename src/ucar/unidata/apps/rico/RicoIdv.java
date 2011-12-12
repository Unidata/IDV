/*
 * $Id: RicoIdv.java,v 1.3 2006/10/25 16:50:51 jeffmc Exp $
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

package ucar.unidata.apps.rico;


import ucar.unidata.idv.*;
import ucar.unidata.idv.ui.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;



import visad.VisADException;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;



/**
 *
 * @author IDV development team
 */

public class RicoIdv extends IntegratedDataViewer {


    /**
     * Create the RicoIdv with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */
    public RicoIdv(String[] args) throws VisADException, RemoteException {
        super(args);
        init();
    }


    /**
     *  Add in our properties. This is the first part  of the bootstrap
     * initializatio process.  The properties file contains a property
     * that defines any other property files to be loaded in
     * Then the idv looks at the property:<pre>
     * idv.resourcefiles </pre>
     * to find out where the rbi files are located. These  rbi files
     * define where all of the various and sundry resources exist. In this
     * example we use our own rbi file: example.rbi
     *
     * @param files List of property files
     */

    public void initPropertyFiles(List files) {
        files.clear();
        files.add("/ucar/unidata/apps/rico/rico.properties");
    }




    /**
     * The main. Configure the logging and create the RicoIdv
     *
     * @param args Command line arguments
     *
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        RicoIdv idv = new RicoIdv(args);
    }


}







