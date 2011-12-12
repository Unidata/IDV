/*
 * $Id: GroupGlyph.java,v 1.6 2005/05/13 18:32:10 jeffmc Exp $
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





package ucar.unidata.ui.drawing;



import java.util.ArrayList;
import java.util.List;


/**
 *  This is a simple group of glyphs
 */

public class GroupGlyph extends CompositeGlyph {

    /**
     * _more_
     *
     * @param x
     * @param y
     *
     */
    public GroupGlyph(int x, int y) {
        this();
    }


    /**
     * _more_
     *
     * @param newChildren
     *
     */
    public GroupGlyph(List newChildren) {
        super("GROUP", newChildren);
        paintBorder = false;
    }

    /**
     * _more_
     *
     */
    public GroupGlyph() {
        this(null);
    }


}





