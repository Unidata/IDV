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


import visad.Data;

import visad.DataImpl;

import visad.VisADException;

import visad.data.*;
import visad.data.mcidas.MapForm;



import java.io.IOException;

import java.net.URL;

import java.rmi.RemoteException;


/**
 * A container for all the supported Map types.  Currently, ESRI
 * shapefiles, McIDAS and Zebra map files are supported.
 * To read a <tt>Data</tt> object from a file or URL:<br>
 * <pre>
 *    Data data = new MapFamily("maps").open(string);
 * </pre>
 * @see  ucar.visad.ShapefileForm
 * @see  ucar.visad.ZebraMapForm
 * @see  visad.data.mcidas.MapForm
 * @author Don Murray
 * @version $Revision: 1.10 $
 */
public class MapFamily extends FunctionFormFamily {

    /**
     *  List of all supported VisAD datatype Forms.
     *  @serial
     */
    private static FormNode[] list = new FormNode[10];

    /** flag for whether the list has been initialized */
    private static boolean listInitialized = false;

    /**
     * Build a list of all known file adapter Forms
     */
    private static void buildList() {

        int i = 0;

        try {
            list[i] = new MapForm();

            i++;
        } catch (Throwable t) {}

        try {
            list[i] = new ShapefileForm();

            i++;
        } catch (Throwable t) {}

        try {
            list[i] = new ZebraMapForm();

            i++;
        } catch (Throwable t) {}

        // throw an Exception if too many Forms for list
        FormNode junk = list[i];

        while (i < list.length) {
            list[i++] = null;
        }

        listInitialized = true;  // WLH 24 Jan 2000
    }

    /**
     * Add to the family of the supported map datatype Forms
     * @param  form   FormNode to add to the list
     *
     * @exception ArrayIndexOutOfBoundsException
     *                   If there is no more room in the list.
     */
    public static void addFormToList(FormNode form)
            throws ArrayIndexOutOfBoundsException {

        synchronized (list) {
            if ( !listInitialized) {
                buildList();
            }

            int i = 0;

            while (i < list.length) {
                if (list[i] == null) {
                    list[i] = form;

                    return;
                }

                i++;
            }
        }

        throw new ArrayIndexOutOfBoundsException("Only " + list.length
                + " entries allowed");
    }

    /**
     * Construct a family of the supported map datatype Forms
     * @param  name   name of the family
     */
    public MapFamily(String name) {

        super(name);

        synchronized (list) {
            if ( !listInitialized) {
                buildList();
            }
        }

        for (int i = 0; (i < list.length) && (list[i] != null); i++) {
            forms.addElement(list[i]);
        }
    }

    /**
     * Open a local data object using the first appropriate map form.
     *
     * @param  id   String representing the path of the map file
     *
     * @return  the VisAD Data representation of the map file
     *
     * @throws  BadFormException  - no form is appropriate
     * @throws  VisADException  - VisAD error
     */
    public DataImpl open(String id) throws BadFormException, VisADException {
        return super.open(id);
    }

    /**
     * Open a remote data object using the first appropriate map form.
     *
     * @param  url   URL representing the location of the map file
     *
     * @return  the VisAD Data representation of the map file
     *
     * @throws  BadFormException  - no form is appropriate
     * @throws  VisADException  - VisAD error
     * @throws  IOException  - file not found
     */
    public DataImpl open(URL url)
            throws BadFormException, VisADException, IOException {
        return super.open(url);
    }

    /**
     * Test the MapFamily class.
     * Run java ucar.visad.MapFamily  map1 map2 ... mapn
     *
     * @param args  map file locations
     *
     * @throws BadFormException
     * @throws IOException
     * @throws RemoteException
     * @throws VisADException
     */
    public static void main(String[] args)
            throws BadFormException, IOException, RemoteException,
                   VisADException {

        if (args.length < 1) {
            System.err.println("Usage: MapFamily infile [infile ...]");
            System.exit(1);

            return;
        }

        MapFamily fr = new MapFamily("sample");

        for (int i = 0; i < args.length; i++) {
            Data data;

            System.out.println("Trying file " + args[i]);

            data = fr.open(args[i]);

            System.out.println(args[i] + ": "
                               + data.getType().prettyString());
        }
    }
}
