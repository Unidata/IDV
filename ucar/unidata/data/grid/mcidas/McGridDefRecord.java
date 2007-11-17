/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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




package ucar.unidata.data.grid.mcidas;

import ucar.unidata.data.grid.gempak.GridDefRecord;

/**
 * Class to hold the grid navigation information.
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class McGridDefRecord extends GridDefRecord {

    /** raw values */
    int[] vals = null;

    /** projection type */
    private String proj;

  /** Navigation type for pseudo-mercator grids */
  final int PSEUDO_MERCATOR = 1;
  /** Navigation type for polar stero or lambert conformal conic grids */
  final int PS_OR_LAMBERT_CONIC = 2;
  /** Navigation type for equidistant grids */
  final int EQUIDISTANT = 3;
  /** Navigation type for pseudo-mercator (general case) grids */
  final int PSEUDO_MERCATOR_GENERAL = 4;
  final int NO_NAV = 5;
  /** Navigation type for lambert conformal tangent grids */
  final int LAMBERT_CONFORMAL_TANGENT = 6;
  final double EARTH_RADIUS = 6371.23;
    /**
     * Create a new grid nav block
     */
    public McGridDefRecord() {}

    /**
     * Create a new grid nav block with the values
     *
     * @param words   analysis block values
     */
    public McGridDefRecord(int[] words) {
        setValues(words);
    }

    /**
     * Set the grid nav block values
     *
     * @param values   the raw values
     */
    public void setValues(int[] values) {
        vals = values;
        addParam(GDS_KEY, this.toString());
        setParams();
    }

    /**
     * Print out the navibation block so it looks something like this:
     * <pre>
     *        PROJECTION:          LCC
     *        ANGLES:                25.0   -95.0    25.0
     *        GRID SIZE:           93  65
     *        LL CORNER:              12.19   -133.46
     *        UR CORNER:              57.29    -49.38
     * </pre>
     *
     * @return  a String representation of this.
     */
    public String toString() {
        // TODO: make this more unique
        StringBuffer buf = new StringBuffer("McNAV: ");
        buf.append(getParam("Proj"));
        buf.append(" X:");
        buf.append(getParam(NX));
        buf.append(" ");
        buf.append("Y:");
        buf.append(getParam(NY));
        return buf.toString();
    }


  /* common calculation variables 
  private int navType;
  private double xnr;             // number of rows
  private double xnc;             // number of columns
  private double xnrow;           // number of rows for calculations
  private double xncol;           // number of columns for calculations
  private boolean wierd = false;  // JTYP = 1, navType > 10
  */

  /* Merc and pseudo_merc parameters 
  private double glamx;           // max latitude
  private double glomx;           // max longitude
  private double ginct;           // grid increment in latitude
  private double gincn;           // grid increment in longitude
  */

  /* PS and CONF projection parameters 
  private double xrowi;           // row # of North Pole*10000
  private double xcoli;           // column # of North Pole* 10000
  private double xqlon;           // longitude parallel to columns
  private double xspace;          // column spacing at standard latitude
  private double xh;              // 
  private double xfac;            //
  private double xblat;           //
  */

  /* Equidistant params 
  private double xrot;            // rotation angle
  private double yspace;
  private double xblon; 
  */

    /**
     * Set the parameters for the GDS
     */
    private void setParams() {
     /*
        String angle1 = String.valueOf(vals[10]);
        String angle2 = String.valueOf(vals[11]);
        String angle3 = String.valueOf(vals[12]);
        String lllat  = String.valueOf(vals[6]);
        String lllon  = String.valueOf(vals[7]);
        String urlat  = String.valueOf(vals[8]);
        String urlon  = String.valueOf(vals[9]);
        addParam(NX, String.valueOf(vals[4]));
        addParam(NY, String.valueOf(vals[5]));
        if (proj.equals("STR")) {
            addParam(LOV, angle2);
            addParam(LA1, lllat);
            addParam(LO1, lllon);
            addParam(LA2, urlat);
            addParam(LO2, urlon);
        } else if (proj.equals("LCC")) {
            addParam(LATIN1, angle1);
            addParam(LOV, angle2);
            addParam(LATIN2, angle3);
            addParam(LA1, lllat);
            addParam(LO1, lllon);
            addParam(LA2, urlat);
            addParam(LO2, urlon);
        }
    }
    */
    int gridType = vals[33];
    int navType = (int) gridType%10;
    addParam("Proj", String.valueOf(navType));
    boolean wierd = gridType/10 == 1;
    addParam(NX, String.valueOf(vals[1]));
    addParam(NY, String.valueOf(vals[2]));
    switch(navType)
    {
      case PSEUDO_MERCATOR:
      case PSEUDO_MERCATOR_GENERAL:
        double glamx=vals[34]/10000.;
        double glomx=vals[35]/10000.;
        double ginct=vals[38]/10000.;
        double gincn= 
          (navType == PSEUDO_MERCATOR_GENERAL) 
             ? vals[39]/10000. : ginct;
        addParam("Latin", String.valueOf(gincn*10000));
        addParam(LA1, String.valueOf(glamx));
        addParam(LO1, String.valueOf(glomx));
        /*
        if (wierd) {
          double x = xnr;
          xnr = xnc;
          xnc = x;
        }
        */
        break;
      case PS_OR_LAMBERT_CONIC:
      case LAMBERT_CONFORMAL_TANGENT:
        double xrowi  = vals[34]/10000.;  // row # of the North pole*10000
        double xcoli  = vals[35]/10000.;  // col # of the North pole*10000
        double xspace = vals[36]/1000.;   // column spacing at standard lat (m)
        double xqlon  = vals[37]/10000.;  // lon parallel to cols (deg*10000)
        double xt1 = vals[38]/10000.;  // first standard lat
        double xt2 = xt1;
        if (navType == PS_OR_LAMBERT_CONIC) {
           xt2 = vals[39]/10000.;  // second standard lat
        }
        addParam(LATIN1, String.valueOf(xt1));
        addParam(LOV, String.valueOf(xqlon));
        addParam(LATIN2, String.valueOf(xt2));
        addParam(DX, String.valueOf(xspace));
        addParam(DY, String.valueOf(xspace));
        /*
        xh = (xt1 >= 0) ? 1. : -1.;
        xt1 =(90.-xh*xt1)*xrad;
        xt2 =(90.-xh*xt2)*xrad;
        xfac =1.0;
        if (xt1 != xt2) 
           xfac = (Math.log(Math.sin(xt1))-Math.log(Math.sin(xt2)))/
                  (Math.log(Math.tan(.5*xt1))-Math.log(Math.tan(.5*xt2)));
        xfac = 1.0/xfac;
        xblat = 6370. * Math.sin(xt1)/
                 (xspace*xfac*(Math.pow(Math.tan(xt1*.5),xfac)));
        if (wierd) {
           double x=xnr;
           xnr=xnc;
           xnc=x;
           x=xcoli;
           xcoli=xrowi;
           xrowi=xnr-x+1.0;
           xqlon=xqlon+90.;
        }
        */

        break;
      case EQUIDISTANT:
        /*
        xrowi = 1.;
        xcoli = 1.;
        glamx = vals[34]/10000.;       // lat of (1,1) degrees*10000
        glomx = vals[35]/10000.;       // lon of (1,1) degrees*10000
        xrot  = -xrad*vals[36]/10000.; // clockwise rotation of col 1
        xspace = vals[37]/1000.;       // column spacing
        yspace = vals[38]/1000.;       // row spacing
        xblat = EARTH_RADIUS*xrad/yspace;
        xblon = EARTH_RADIUS*xrad/xspace;

        if (wierd) {
          double x = xnr;
          xnr = xnc;
          xnc = x;
        }
        */

        break;
        /*
      case LAMBERT_CONFORMAL_TANGENT:
        xrowi  = vals[34]/10000.;  // row # of the North pole*10000
        xcoli  = vals[35]/10000.;  // col # of the North pole*10000
        xspace = vals[36]/1000.;   // column spacing at standard lat (m)
        xqlon  = vals[37]/10000.;  // lon parallel to cols (deg*10000)
        double xtl = vals[38]/10000.; // standard lat
        xh = (xtl >= 0) ? 1. : -1.;
        xtl = (90. - xh * xtl) * xrad;
        xfac = Math.cos(xtl);
        xblat = EARTH_RADIUS * Math.tan(xtl) / 
                   (xspace * Math.pow(Math.tan(xtl*.5), xfac));

        break;
        */
      default:  
        break;
    }
  }
}

