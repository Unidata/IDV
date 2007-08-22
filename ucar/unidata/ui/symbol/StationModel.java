/*
 * $Id: StationModel.java,v 1.15 2007/05/29 13:36:56 jeffmc Exp $
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

package ucar.unidata.ui.symbol;


import ucar.unidata.ui.drawing.Glyph;



import ucar.unidata.util.NamedList;

import java.awt.*;
import java.awt.event.*;

import java.awt.geom.Point2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 */

public class StationModel extends NamedList {


    /** The bounds of the station model */
    private Rectangle bounds;

    /** Change listeners */
    private List listeners = new ArrayList();


    /**
     * default ctor
     *
     */
    public StationModel() {
        this("");
    }

    /**
     * ctor
     *
     * @param name The name of the station model
     *
     */
    public StationModel(String name) {
        this(name, new ArrayList());
    }

    /**
     * ctor
     *
     * @param name name
     * @param l List of MetSymbols in this station model
     *
     */
    public StationModel(String name, List l) {
        super(name, l);
    }



    /**
     * Get the list of symbols
     * @return list of symbols
     */
    public List getSymbols() {
        return getList();
    }

    /**
     * Add the symbol
     *
     * @param s The symbol to add
     */
    public void addSymbol(MetSymbol s) {
        getList().add(s);
        bounds = null;
    }

    /**
     * Remove the symbol
     *
     * @param s Symbol to remove
     */
    public void removeSymbol(MetSymbol s) {
        getList().remove(s);
        bounds = null;
    }

    /**
     * Get the bounds. This is the union of the bounds of each symbol
     * @return bounds
     */
    public Rectangle getBounds() {
        /* For now, don't cache the bounds
           if (bounds != null) {
           return bounds;
           }
        */
        //Just use a local bounds variable so we don't get a threading conflict
        Rectangle tmpBounds = null;
        List      glyphs    = getList();
        for (int i = 0; i < glyphs.size(); i++) {
            Rectangle gb = ((Glyph) glyphs.get(i)).getBounds();
            if (tmpBounds == null) {
                tmpBounds = new Rectangle(gb);
            } else {
                tmpBounds.add(gb);
            }
        }
        if (tmpBounds == null) {
            tmpBounds = new Rectangle(0, 0, 0, 0);
        }
        bounds = tmpBounds;

        return tmpBounds;
    }

    public String getDisplayName() {
        List toks =  ucar.unidata.util.StringUtil.split(getName(), ">", true,true);
        if(toks.size()>0)
            return (String)toks.get(toks.size()-1);
        return getName();
    }

    /**
     * Find the named symbol
     *
     * @param name Symbol name
     * @return Named symbol or null
     */
    public MetSymbol findSymbolByName(String name) {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            MetSymbol ms = (MetSymbol) iter.next();
            if (name.equals(ms.getName())) {
                return ms;
            }
        }
        return null;
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





}

