/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import visad.VisADException;


import java.lang.reflect.Constructor;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * This is the default implementation of an IDV. It defers most of the
 * work to the base class and  simply
 * constructs a UI (in doMakeContents)
 *
 * @author IDV development team
 */

public class DefaultIdv extends IntegratedDataViewer {

    /** appearance */
    private String appearanceMode;
    /**
     * Parameterless ctor. Why? I'm not quite sure.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public DefaultIdv() throws VisADException, RemoteException {}

    /**
     * Create the DefaultIdv with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */

    public DefaultIdv(String[] args) throws VisADException, RemoteException {
        super(args);
        this.appearanceMode =
                getStore().get(PREF_APPEARANCEMODE,
                        DisplayControlImpl.APPEARANCE_IDV);

        try {
            if(this.appearanceMode.equals("dark") )
                UIManager.setLookAndFeel( new FlatDarkLaf() );
            else if(this.appearanceMode.equals("light"))
                UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }
        init();
    }


    /**
     *  Add in our properties.
     *
     * @param files List of property files
     */
    public void initPropertyFiles(List files) {
        super.initPropertyFiles(files);
        files.add("/ucar/unidata/idv/resources/haiku.properties");
    }


    /**
     *  This method  checks if the given action should trigger the  Haiku easter egg.
     *
     * @param action The action (file, data source url, etc.)
     * @param properties Properties to pass to the data source creation
     * @param checkForAlias  Should check for aliases.
     * @return Was this action handled by this method.
     */
    public boolean handleAction(String action, Hashtable properties,
                                boolean checkForAlias) {

        if (getIdvUIManager().checkHaiku(action)) {
            return true;
        }
        return super.handleAction(action, properties, checkForAlias);
    }

    /*

      String javaBin = System.getProperty("java.home") + "/bin/java";        File jarFile;
      try {
      jarFile = new File
      (classInJarFile.getClass().getProtectionDomain()
      .getCodeSource().getLocation().toURI());
      } catch(Exception e) {
      return false;
      }

      if ( !jarFile.getName().endsWith(".jar") )
      return false;   //no, it's a .class probably

      String  toExec[] = new String[] { javaBin, "-jar", jarFile.getPath() };
      try {
      Process p = Runtime.getRuntime().exec( toExec );
      }catch(Exception exc) {
      }
    */

    /**
     * The main. Configure the logging and create the DefaultIdv
     *
     * @param args Command line arguments
     *
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {

        LogUtil.configure();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(ARG_MAINCLASS)) {
                Class mainClass = Misc.findClass(args[i + 1]);
                Constructor ctor = Misc.findConstructor(mainClass,
                                       new Class[] { args.getClass() });
                if (ctor == null) {
                    throw new IllegalArgumentException(
                        "Could not find class:" + args[i + 1]);
                }
                ctor.newInstance(new Object[] { args });
                return;
            }
        }

        DefaultIdv idv = new DefaultIdv(args);
    }


}
