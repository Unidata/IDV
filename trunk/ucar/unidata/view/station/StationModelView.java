/*
 * $Id: StationModelView.java,v 1.8 2005/08/11 22:26:12 dmurray Exp $
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

package ucar.unidata.view.station;



import java.awt.*;
import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Iterator;

import ucar.unidata.ui.symbol.*;


/**
 * Encapsolates how a station observation should be displayed.
 * A StationModelView is essentially a collection of MetSymbols.
 * @author MetApps Development Team
 * @version $Revision: 1.8 $ $Date: 2005/08/11 22:26:12 $
 */
public class StationModelView implements Cloneable, java.io.Serializable {

    /** list of met symbols */
    private ArrayList metsym = new ArrayList(20);

    /** the name of this view */
    private String name;

    /** debug flag */
    private boolean debug = false;

    /** selected symbol */
    private MetSymbol selected = null;


    /**
     * constructor
     * @param name name of this object
     */
    public StationModelView(String name) {
        this.name = name;
    }


    /**
     * Add a symbol to the collection.
     * @param ms symbol to add
     */
    public void addSymbol(MetSymbol ms) {
        metsym.add(ms);
    }

    /**
     * Remove a symbol from the collection.
     * @param ms symbol to add
     */
    public void removeSymbol(MetSymbol ms) {
        metsym.remove(ms);
    }

    /**
     * increment or decrement all MetSymbols that are TextSymbols
     * @param incr : true = increase, false = decrease
     */
    public void changeTextSize(boolean incr) {
        if (debug) {
            System.out.println("StationModel changeTextSize");
        }
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            if (debug) {
                System.out.println(" changeTextSize " + ms);
            }
            if (ms instanceof TextSymbol) {
                if (incr) {
                    ((TextSymbol) ms).incrFontSize();
                } else {
                    ((TextSymbol) ms).decrFontSize();
                }
            }
        }
    }

    /**
     * Get an iterator over MetSymbol objects
     * @return the iterator for the collection
     */
    public Iterator iterator() {
        return metsym.iterator();
    }

    /**
     * find the MetSymbol with this name
     * @param name the name to match
     * @return the found MetSybpol or null if no match
     */
    public MetSymbol findSymbolByName(String name) {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            if (0 == name.compareTo(ms.getName())) {
                return ms;
            }
        }
        return null;
    }

    /**
     * Find the MetSymbol at the specified index
     * @param idx  the symbol index
     * @return the found MetSymbol
     * @throws ArrayIndexOutOfBoundsException if idx is larger than life
     */
    public MetSymbol findSymbolByIndex(int idx) {
        return (MetSymbol) metsym.get(idx);
    }

    /** default size */
    private int size = 20;

    /**
     * Set the size
     *
     * @param size the new size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Get the name
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Set all values to missing if true.
     *
     * @param missing  true to set all missing
     */
    public void setAllMissing(boolean missing) {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            ms.setMissing(missing);
        }
    }

    /**
     * Draw the entire StationModel at the given location.
     *  Note that all MetSymbols expect to use "normalized device" coordinates.
     *
     * @param g
     * @param loc
     */
    public void draw(Graphics2D g, Point2D loc) {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            if (ms.getActive() && !ms.getMissing()) {
                ms.draw(g, loc);
            }
        }
    }

    /**
     * Get the selected MetSymbol, or null if none. (Used for editing)
     * @return the selected MetSymbol
     */
    public MetSymbol getSelected() {
        return selected;
    }

    /**
     * Set the selected MetSymbol. (Used for editing)
     *
     * @param ms
     */
    public void setSelected(MetSymbol ms) {
        selected = ms;
    }

    /**
     *  Set the color of all the MetSymbols
     *
     * @param c
     */
    public void setColor(Color c) {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            ms.setForeground(c);
        }
    }

    /**
     * Get the bounds
     * @return the bounds
     */
    public Rectangle getBounds() {
        return getBounds(new Point(0, 0));
    }

    /**
     * Return union of bounding box of all the  symbols
     *
     * @param loc
     * @return the union of bounding box of all the  symbols
     */
    public Rectangle getBounds(Point2D loc) {
        Rectangle bounds = null;
        int       cnt    = 0;
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            if (ms.getActive() && !ms.getMissing()) {
                if (bounds == null) {
                    bounds = new Rectangle(ms.getBounds(loc));
                } else {
                    bounds.add(ms.getBounds(loc));
                }
            }
        }
        if (bounds == null) {
            return new Rectangle();
        }
        return bounds;
    }

    // standard methods

    /**
     * Get a String representation of this object
     * @return a String representation of this object
     */
    public String toString() {
        return name;
    }

    /**
     * Clone this object
     * @return a clone
     */
    public Object clone() {
        StationModelView cl;
        try {
            cl = (StationModelView) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }  // ignore

        // not primitive fields must be cloned separately
        cl.name   = new String(name);
        cl.metsym = new ArrayList(metsym.size());
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            cl.addSymbol((MetSymbol) ((MetSymbol) iter.next()).cloneMe());
        }
        return cl;
    }

}





