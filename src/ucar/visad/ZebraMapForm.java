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

package ucar.visad;



import visad.*;

import visad.data.*;

import visad.util.*;

import java.io.IOException;

import java.net.URL;

import java.rmi.RemoteException;


/**
 * ZebraMapForm is the shapefile data format adapter for
 * ESRI shapefile maps.<P>
 *
 * @author Don Murray
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:34:07 $
 */
public class ZebraMapForm extends Form implements FormFileInformer {

    /** counter @serialized */
    private static int num = 0;

    /**
     * Construct a Form for reading in ESRI shapefile map files
     */
    public ZebraMapForm() {
        super("ZebraMapForm" + num++);
    }

    /**
     * Determines if this is a shapefile map file from the name
     * @param  name  name of the file
     * @return  true if it matches the pattern for shapefile map files
     */
    public boolean isThisType(String name) {
        return (name.endsWith(".map"));
    }

    /**
     * Determines if this is a shapfile map file from the starting block
     * @param  block  block of data to check
     * @return  false  - there is no identifying block in a Zebra map file
     */
    public boolean isThisType(byte[] block) {
        return false;
    }

    /**
     * Get a list of default suffixes for shapfile map files
     * @return  valid list of suffixes
     */
    public String[] getDefaultSuffixes() {

        String[] suff = { ".map" };

        return suff;
    }

    /**
     * Save a VisAD data object in this form
     *
     * @param id        location of data
     * @param data      data to add
     * @param replace   true to replace old with new
     *
     * @throws BadFormException  bad form
     * @throws IOException       file doesn't exist
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    Problem creating data
     */
    public synchronized void save(String id, Data data, boolean replace)
            throws BadFormException, IOException, RemoteException,
                   VisADException {
        throw new UnimplementedException(
            "Can't yet save Zebra ASCII map files");
    }

    /**
     * Add data to an existing data object
     *
     * @param id        location of data
     * @param data      data to add
     * @param replace   true to replace old with new
     * @throws BadFormException  always for this form
     */
    public synchronized void add(String id, Data data, boolean replace)
            throws BadFormException {
        throw new BadFormException("ZebraMapForm.add");
    }

    /**
     * Open the file specified by the string
     * @param  id   string representing the path to the file
     * @return a Data object representing the map lines.
     *
     * @throws BadFormException
     * @throws IOException
     * @throws VisADException
     */
    public synchronized DataImpl open(String id)
            throws BadFormException, IOException, VisADException {

        try {
            ZebraAsciiMapAdapter zama = new ZebraAsciiMapAdapter(id);

            return zama.getData();
        } catch (IOException e) {
            throw new VisADException("IOException: " + e.getMessage());
        }
    }

    /**
     * Open the file specified by the URL
     *
     * @param url   URL of the remote map
     *
     * @return a Data object representing the map lines.
     *
     * @throws BadFormException  bad form
     * @throws IOException       file doesn't exist
     * @throws VisADException    Problem creating data
     */
    public synchronized DataImpl open(URL url)
            throws BadFormException, VisADException, IOException {

        ZebraAsciiMapAdapter zama = new ZebraAsciiMapAdapter(url);

        return zama.getData();
    }

    /**
     * Return the data forms that are compatible with a data object
     *
     * @param data   data in question
     * @return null
     */
    public synchronized FormNode getForms(Data data) {
        return null;
    }
}
