/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
 *
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.adt;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Topo {

    public Topo() {
    }

    public static int ReadTopoFile(String topofile, double inputlat, double inputlon)
            throws IOException {

        int num_lon_elev = 5760;
        int num_lat_elev = 2880;
        double first_lon_elev = 0.0;
        double first_lat_elev = 90.0;
        double last_lon_elev = 360.0;
        double last_lat_elev = -90.0;

        double ax = inputlat;
        double bx = inputlon; /* to make mcidas compliant */
        System.out.printf("TOPO: lat: %f  lon: %f\n", ax, bx);

        InputStream filestream = Topo.class.getResourceAsStream(topofile);
        DataInputStream dis = new DataInputStream(filestream);

        double del_lon_elev = (last_lon_elev - first_lon_elev) / num_lon_elev;
        double del_lat_elev = (first_lat_elev - last_lat_elev) / num_lat_elev;
        System.out.printf("TOPO: dlat: %f  dlon: %f\n", del_lon_elev, del_lat_elev);

        int ay = (int) ((90.0 - ax) / del_lat_elev);
        if (bx < 0.0)
            bx = bx + 360.0;
        int by = (int) (bx / del_lon_elev);
        System.out.printf("TOPO: lat: %d  lon: %d \n", ay, by);
        long position = (long) (2 * ((ay * ((double) num_lon_elev)) + by));
        System.out.printf("TOPO: position=%d\n", position);
        // filestream.seek(position+1);
        dis.skip(position);
        System.err.println("After skip, about to read val...");

        int i = dis.readShort();
        System.err.println("After read, val: " + i);

        int ichar = (i == 0) ? 2 : 1;
        System.err.println("After read, returning: " + ichar);
        System.out.printf("TOPO: position=%d Value=%d landflag=%d \n ", position, i, ichar);
        filestream.close();

        return ichar;
    }

}