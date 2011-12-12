/*
 * $Id: ExampleIdv.java,v 1.16 2007/06/01 12:10:52 jeffmc Exp $
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

package ucar.unidata.apps.example;


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
 * This is an example class that demonstrates how to extend the IDV framework.
 * Normally, to extend functionality you do two things:
 * 1. Define your own properties file and/or your own rbi file to
 * add in or completely reconfigure the resources that the IDV
 * uses (e.g., user interface skin, color tables, etc.)
 *
 * 2. Create your own versions of the managers and editors the IDV uses
 * to do its work. These are created by a set of doMake... factory methods
 * (defined in ucar.unidata.idv.IdvBase) that you may override in this class.
 * For example, we override the doMakeIdvUIManager factory method to create
 * our own user interface manager.
 *
 * @author IDV development team
 */

public class ExampleIdv extends IntegratedDataViewer {

    /** An example resource definition */
    public static final IdvResourceManager.XmlIdvResource RSC_EXAMPLECATALOGS =
        new IdvResourceManager.XmlIdvResource(
            "idv.resource.examplecatalogs", "Example catalog");


    /**
     * Create the ExampleIdv with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */
    public ExampleIdv(String[] args) throws VisADException, RemoteException {
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
        //The files list contains the default system 
        //properties (idv.properties)
        //We want to completely clobber it to use our own
        //If we just wanted to add ours on top of the system we just
        //don't clear the list. Note the path here is a java resource, i.e.,
        //it is found from the classpath (either on dsk or in a jar file).
        //In  general whenever we specify some path (e.g., for properties,
        //for resources) the path can either be a java resource, a 
        //file system path or a url


        /*
          files.clear();
          files.add("/ucar/unidata/apps/example/example.properties");
        */
    }



    /**
     * Factory method to create the
     * {@link IdvUIManager}. Here we create our own ui manager
     * so it can do Example specific things.
     *
     * @return The UI manager
     */
    protected IdvUIManager doMakeIdvUIManager() {
        return new ExampleUIManager(getIdv());
    }



    /* Uncomment this method to use the ExampleArgsManager
    protected ArgsManager doMakeArgsManager(String[] args) {
        return new ExampleArgsManager(this, args);
    }
    */


    /**
     * The main. Configure the logging and create the ExampleIdv
     *
     * @param args Command line arguments
     *
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        ExampleIdv idv = new ExampleIdv(args);
    }


}







