/*
 * $Id: GuiUtils.java,v 1.317 2007/08/10 14:26:33 jeffmc Exp $
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


package ucar.unidata.util;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.awt.image.*;


import java.io.*;

import java.lang.reflect.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.tree.*;



/**
 * This is a vast catchall class to old various
 * utilities for doing GUI things.
 *
 *
 * @author IDV development team
 */
public class LayoutUtil extends MenuUtil {

    /** Used by the doLayout routine for the default layout insets */
    private static final Insets DFLT_INSETS = new Insets(0, 0, 0, 0);

    /**
     * If you want to change the insets used in the doLayout routines
     * you can set this and the routine will use the value and then set it to null.
     * I know, I know, it isn't thread safe but...
     */
    public static Insets tmpInsets;

    /** 5 pixel inset */
    public static final Insets INSETS_5 = new Insets(5, 5, 5, 5);

    /** 2 pixel inset */
    public static final Insets INSETS_2 = new Insets(2, 2, 2, 2);

    /**
     * Use this to define your own column fills (i.e., horizontal expansion
     * of the elements in the gribag). Will be set to null after use.
     * Not thread safe but...
     */
    public static int[] tmpColFills = null;


    /** This is the default anchor used in doLayout */
    private static final int DFLT_ANCHOR = GridBagConstraints.WEST;

    /**
     * Set this to define your own anchor in the doLayout routines.
     * Will get reset after use. Not thread safe. You can also
     * call {@link #setAnchorBottom()} and {@link #setAnchorTop()} for
     * changing the anchor.
     */
    public static int tmpAnchor = -1;

    /**
     * Set this to define your own fill in the doLayout routines.
     * Will get reset after use. Not thread safe. You can also
     * call {@link #setNoFill()} and {@link #setHFill()} for doing
     * no fill and horizontal fill.
     */
    public static int tmpFill = -1;


    /** A 0 inset */
    public final static Insets ZERO_INSETS = new Insets(0, 0, 0, 0);



    /**
     * All of the WT_  members are used to define the column and row
     * weights  for the doLayout routines.  They are double arrays,
     * typically one for each col or row, that hold either a 1 or a 0.
     * The &quot;Y&quot; and &quot;N&quot; implies YES or NO, i.e.,
     * does the corresponding row/column get weight (Y) or not get weight (N).
     * <p>
     * So for example, if you wanted to have no stretchiness in the first column
     * and put all of the stretchiness into the second column you would use:<pre>
     * WT_NY</pre>
     * <p>
     * Note, you can pass in your own double arrays to doLayout. These are just here
     * for convenience.
     */
    public static final double[] WT_Y = { 1 };

    /** doLayout weights */
    public static final double[] WT_YY = { 1, 1 };

    /** doLayout weights */
    public static final double[] WT_YYY = { 1, 1, 1 };

    /** doLayout weights */
    public static final double[] WT_N = { 0 };

    /** doLayout weights */
    public static final double[] WT_NN = { 0 };

    /** doLayout weights */
    public static final double[] WT_NNN = { 0 };

    /** doLayout weights */
    public static final double[] WT_YN = { 1, 0 };

    /** doLayout weights */
    public static final double[] WT_YYN = { 1, 1, 0 };

    /** doLayout weights */
    public static final double[] WT_YNYN = { 1, 0, 1, 0 };

     /** doLayout weights */
    public static final double[] WT_NYNYN = { 0,1,0,1,0};

    /** doLayout weights */
    public static final double[] WT_NY = { 0, 1 };

    /** doLayout weights */
    public static final double[] WT_NYN = { 0, 1, 0 };

    /** doLayout weights */
    public static final double[] WT_YNY = { 1, 0, 1 };

    /** doLayout weights */
    public static final double[] WT_NYY = { 0, 1, 1 };
    /** doLayout weights */

    public static final double[] WT_YNN = { 1, 0, 0 };
    /** doLayout weights */

    public static final double[] WT_NNY = { 0, 0, 1 };

    /** doLayout weights */
    public static final double[] WT_NNNY = { 0, 0, 0, 1 };

    /** doLayout weights */
    public static final double[] WT_NNYN = { 0, 0, 1, 0 };

    /** doLayout weights */
    public static final double[] WT_NNYNY = { 0, 0, 1, 0, 1 };

    /** doLayout weights */
    public static final double[] WT_NNYNYNY = {
        0, 0, 1, 0, 1, 0, 1
    };

    /** doLayout weights */
    public static final double[] WT_NYNY = { 0, 1, 0, 1 };


    /**
     * Turns off component fill for the doLayout routine
     */
    public static void setNoFill() {
        tmpFill = GridBagConstraints.NONE;
    }

    /**
     * Wrap the component so that it only expands horizontally, not vertically
     *
     * @param comp The component
     *
     * @return The wrapped component
     */
    public static JComponent hfill(JComponent comp) {
        setHFill();
        return doLayout(new Component[] { comp }, 1, WT_Y,
                        WT_N);
    }

    /**
     * Turns on component horizontal fill for the doLayout routine
     */
    public static void setHFill() {
        tmpFill = GridBagConstraints.HORIZONTAL;
    }

    /**
     * Sets the anchor to bottom for doLayout
     */
    public static void setAnchorBottom() {
        tmpAnchor = GridBagConstraints.SOUTHWEST;
    }

    /**
     * Sets the anchor to top for doLayout
     */
    public static void setAnchorTop() {
        tmpAnchor = GridBagConstraints.NORTHEAST;
    }





    /**
     * Inset the given component by the given insetSize, both
     * horizontally  and vertically.
     *
     * @param c The component to wrap
     * @param insetSize Horizontal and vertical inset
     * @return A JPanel that contains the given component inset the specified amount.
     */
    public static JPanel inset(Component c, int insetSize) {
        return inset(c, insetSize, insetSize);
    }

    /**
     * Inset the given component by the given insetSize for
     * horizontally  and vertically.
     *
     * @param c The component to wrap
     * @param insetSizeHor Inset horizontal
     * @param insetSizeVert Inset vertical
     * @return A JPanel that contains the given component inset the specified amount.
     */
    public static JPanel inset(Component c, int insetSizeHor,
                               int insetSizeVert) {
        return doLayout(new JPanel(), new Component[] { c }, 1, WT_Y, WT_Y,
                        null, null,
                        new Insets(insetSizeVert, insetSizeHor,
                                   insetSizeVert, insetSizeHor));
    }

    /**
     * Inset the given component by the given insets.
     *
     * @param c The component to wrap
     * @param insets The insets
     * @return A JPanel that contains the given component inset the specified amount.
     */
    public static JPanel inset(Component c, Insets insets) {
        return doLayout(new JPanel(), new Component[] { c }, 1, WT_Y, WT_Y,
                        null, null, insets);
    }

    /**
     * Wrap the given component, aligning it to the left.
     * It won't expand the component.
     *
     * @param c The component to wrap
     * @return The wrapper
     */
    public static JPanel wrapLeft(Component c) {
        return doLayout(new Component[] { c, filler() }, 2, WT_NY, WT_N);
    }


    /**
     * Wrap the given component. This won't expand the component.
     * Use this when you want to place a component and not expand it.
     *
     * @param c The component to wrap
     * @return The wrapper
     */
    public static JPanel wrap(Component c) {
        return doLayout(new Component[] { c }, 1, WT_N, WT_N);
    }



    /**
     * Create a new JPanel and add the given components to it using
     * a FlowLayout.
     *
     * @param components The components to layout
     * @return The new JPanel
     */
    public static JPanel hflow(List components) {
        return hflow(components, 0, 0);
    }



    /**
     * Create a new JPanel and add the given components to it using
     * a FlowLayout.
     *
     * @param components The components to layout
     * @param hgap Horizontal spacing
     * @param vgap Vertical spacing
     * @return The new JPanel
     */
    public static JPanel hflow(List components, int hgap, int vgap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        for (int i = 0; i < components.size(); i++) {
            Component comp = (Component) components.get(i);
            if (comp != null) {
                p.add(comp);
            }
        }
        return p;
    }




    /**
     * Do a horizontal grid layout of the given components
     *
     *
     * @param comp1 comp1
     * @param comp2 comp2_
     * @return The new JPanel holding the given components
     */
    public static JPanel hgrid(Component comp1, Component comp2) {
        return hgrid(toList(new Object[]{comp1, comp2}));
    }


    /**
     * Do a horizontal grid layout of the given components
     *
     * @param components The components to layout
     * @return The new JPanel holding the given components
     */
    public static JPanel hgrid(List components) {
        return hgrid(components, 0);
    }


    /**
     * Do a horizontal grid layout of the given components
     *
     * @param components The components to layout
     * @param space The spacing
     * @return The new JPanel holding the given components
     */
    public static JPanel hgrid(List components, int space) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0, space, space));
        for (int i = 0; i < components.size(); i++) {
            panel.add((Component) components.get(i));
        }
        return panel;
    }


    /**
     * Do a single column  grid layout of the components
     *
     * @param components The components to layout
     * @return The new JPanel
     */
    public static JPanel vgrid(List components) {
        return vgrid(new JPanel(), components, 0);
    }

    /**
     * Do a single column  grid layout of the components
     *
     * @param panel The panel to add to
     * @param components The components to layout
     * @param space The spacing
     * @return The panel
     */
    public static JPanel vgrid(JPanel panel, List components, int space) {
        if (panel == null) {
            panel = new JPanel();
        }
        panel.setLayout(new GridLayout(0, 1, space, space));
        for (int i = 0; i < components.size(); i++) {
            panel.add((Component) components.get(i));
        }
        return panel;
    }


    /**
     * Do a grid layout with the number of rows
     *
     * @param comps Components to layout
     * @param rows Number of rows
     * @return The new panel
     */
    public static JPanel rowGrid(List comps, int rows) {
        return rowGrid(getComponentArray(comps), rows);
    }

    /**
     * Do a grid layout with the number of rows
     *
     * @param components Components to layout
     * @param rows Number of rows
     * @return The new panel
     */
    public static JPanel rowGrid(Component[] components, int rows) {
        return grid(components, rows, 0, 0);
    }

    /**
     * Do a grid layout with the number of columns
     *
     * @param components Components to layout
     * @param cols Number of cols
     * @return The new panel
     */

    public static JPanel colGrid(Component[] components, int cols) {
        return grid(components, 0, cols, 0);
    }


    /**
     * Do a grid layout with the number of columns
     *
     * @param components Components to layout
     * @param rows Number of rows (0 implies not fixed)
     * @param cols Number of cols (0 implies not fixed)
     * @return The new panel
     */
    public static JPanel grid(Component[] components, int rows, int cols) {
        return grid(components, rows, cols, 0);
    }


    /**
     * Do a grid layout with the number of columns
     *
     * @param components Components to layout
     * @param rows Number of rows (0 implies not fixed)
     * @param cols Number of cols (0 implies not fixed)
     * @param space Spacing
     * @return The new panel
     */


    public static JPanel grid(Component[] components, int rows, int cols,
                              int space) {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(rows, cols, space, space));
        for (int i = 0; i < components.length; i++) {
            if (components[i] == null) {
                p.add(filler());
            } else {
                p.add((Component) components[i]);
            }
        }
        return p;
    }




    /**
     * Inset the given component, adding it into the
     * given parent container, by the given insetSize, both
     * horizontally  and vertically.
     *
     * @param parent The panel to put the component in.
     * @param c The component to wrap
     * @param insetSize Horizontal and vertical inset
     * @return A JPanel that contains the given component inset the specified amount.
     */
    public static JPanel inset(JPanel parent, Component c, int insetSize) {
        return doLayout(parent, new Component[] { c }, 1, WT_Y, WT_Y, null,
                        null,
                        new Insets(insetSize, insetSize, insetSize,
                                   insetSize));
    }






    /**
     * This does a column oriented grid bag layout. It will layout
     * the given components in a grid with the given number of columns.
     *
     * @param components List of {@link Component}s to layout
     * @param numberOfColumns How many columns in the grid layout
     * @param hinset hor. inset
     * @param vinset vert. inset
     * @return THe new panel
     */
    public static JPanel doLayout(List components, int numberOfColumns,
                                  int hinset, int vinset) {
        return doLayout(new JPanel(), getComponentArray(components),
                        numberOfColumns, WT_N, WT_N, null, null,
                        new Insets(hinset, vinset, hinset, vinset));
    }



    /**
     * A utility to convert the listOfComponents into a Component  array
     *
     * @param listOfComponents List of Components
     * @return Component  array
     */
    public static Component[] getComponentArray(List listOfComponents) {
        Component[] c = new Component[listOfComponents.size()];
        for (int i = 0; i < listOfComponents.size(); i++) {
            c[i] = (Component) listOfComponents.get(i);
        }
        return c;
    }



    /**
     * This does a column oriented grid bag layout. It will layout
     * the given components in a grid with the given number of columns.
     *
     * @param components The  components to layout
     * @param numberOfColumns How many columns in the grid layout
     * @param weightsX Defines how much weight to give to each column width. If there are more
     * columns than weights then we use the last weight.
     * @param weightsY Defines how much weight to give to each row height. If there are more
     * rows than weights then we use the last weight.
     * @return New panel
     */
    public static JPanel doLayout(List components, int numberOfColumns,
                                  double[] weightsX, double[] weightsY) {
        return doLayout(new JPanel(), getComponentArray(components),
                        numberOfColumns, weightsX, weightsY, null, null,
                        null);
    }



    /**
     * This does a column oriented grid bag layout. It will layout
     * the given components in a grid with the given number of columns.
     *
     * @param components The  components to layout
     * @param numberOfColumns How many columns in the grid layout
     * @param weightsX Defines how much weight to give to each column width. If there are more
     * columns than weights then we use the last weight.
     * @param weightsY Defines how much weight to give to each row height. If there are more
     * rows than weights then we use the last weight.
     * @return New panel
     */
    public static JPanel doLayout(Component[] components,
                                  int numberOfColumns, double[] weightsX,
                                  double[] weightsY) {
        return doLayout(new JPanel(), components, numberOfColumns, weightsX,
                        weightsY, null, null, null);
    }



    /**
     * This does a column oriented grid bag layout. It will layout
     * the given components in a grid with the given number of columns.
     *
     * @param parentContainer The container to add components to. May be null.
     * @param components The  components to layout
     * @param numberOfColumns How many columns in the grid layout
     * @param weightsX Defines how much weight to give to each column width. If there are more
     * columns than weights then we use the last weight.
     * @param weightsY Defines how much weight to give to each row height. If there are more
     * rows than weights then we use the last weight.
     * @return New panel
     */
    public static JPanel doLayout(JPanel parentContainer,
                                  Component[] components,
                                  int numberOfColumns, double[] weightsX,
                                  double[] weightsY) {
        return doLayout(parentContainer, components, numberOfColumns,
                        weightsX, weightsY, null, null, null);
    }



    /**
     * Layout the given components vertically.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @return The new container of the components.
     */
    public static JPanel vbox(Component c1, Component c2) {
        return vbox(toList(new Object[]{c1, c2}));
    }

    /**
     * Layout the given components vertically.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param c3 Component 3
     * @return The new container of the components.
     */
    public static JPanel vbox(Component c1, Component c2, Component c3) {
        return vbox(toList(new Object[]{c1, c2, c3}));
    }

    /**
     * Layout the given components vertically.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param c3 Component 3
     * @param c4 Component 4
     * @return The new container of the components.
     */
    public static JPanel vbox(Component c1, Component c2, Component c3,
                              Component c4) {
        return vbox(toList(new Object[]{c1, c2, c3, c4}));
    }

    /**
     * Layout the given components vertically.
     *
     * @param components The components
     * @return The new container of the components.
     */
    public static JPanel vbox(Component[] components) {
        return vbox(toList(components));
    }

    public static List toList(Object[]l) {
        ArrayList v = new ArrayList();
        for (int i = 0; i < l.length; i++) {
            v.add(l[i]);
        }
        return v;
    }

    /**
     * Layout the given components vertically.
     *
     * @param components The components
     * @return The new container of the components.
     */
    public static JPanel vbox(List components) {
        return vbox(new JPanel(), components);
    }

    /**
     * Layout the given components vertically.
     *
     * @param components The components
     * @param panel The panel  to put the components in if on-null.
     * @return The given panel if non-null, else the newly create panel.
     */
    public static JPanel vbox(JPanel panel, List components) {
        return doLayout(panel, getComponentArray(components), 1, WT_Y, WT_N);
    }



    /**
     *  This places the given components in a vertical orientation, left aligned.
     *
     * @param components The components
     * @return The new container of the components.
     */
    public static JPanel leftVbox(List components) {
        List tmp = new ArrayList();
        for (int i = 0; i < components.size(); i++) {
            tmp.add(left((Component) components.get(i)));
        }
        return vbox(tmp);
    }



    /**
     * Do a horizontal layout of the given components with the given spacing.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param spacing Inter component spacing
     * @return The new container of the components
     */
    public static JPanel hbox(Component c1, Component c2, int spacing) {
        return hbox(new JPanel(), toList(new Object[]{c1, c2}), spacing);
    }



    /**
     * Do a horizontal layout of the given components with the given spacing.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param c3 Component 3
     * @param spacing Inter component spacing
     * @return The new container of the components
     */
    public static JPanel hbox(Component c1, Component c2, Component c3,
                              int spacing) {
        return hbox(new JPanel(), toList(new Object[]{c1, c2, c3}), spacing);
    }

    /**
     * Do a horizontal layout of the given components.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @return The new container of the components
     */

    public static JPanel hbox(Component c1, Component c2) {
        return hbox(toList(new Object[]{c1, c2}));
    }

    /**
     * Do a horizontal layout of the given components.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param c3 Component 3
     * @return The new container of the components
     */

    public static JPanel hbox(Component c1, Component c2, Component c3) {
        return hbox(toList(new Object[]{c1, c2, c3}));
    }

    /**
     * Do a horizontal layout of the given components.
     *
     * @param c1 Component 1
     * @param c2 Component 2
     * @param c3 Component 3
     * @param c4 Component 4
     * @return The new container of the components
     */
    public static JPanel hbox(Component c1, Component c2, Component c3,
                              Component c4) {
        return hbox(toList(new Object[]{c1, c2, c3, c4}));
    }

    /**
     * Do a horizontal layout of the given components.
     *
     * @param components The components
     * @param spacing The spacing
     * @return The new container of the components
     */
    public static JPanel hbox(List components, int spacing) {
        return hbox(new JPanel(), components, spacing);
    }


    /**
     * Do a horizontal layout of the given components.
     *
     * @param components The components
     * @return The new container of the components
     */
    public static JPanel hbox(List components) {
        return hbox(new JPanel(), components);
    }


    /**
     * Do a horizontal layout of the given components.
     *
     * @param components The components
     * @return The new container of the components
     */
    public static JPanel hbox(Component[] components) {
        return hbox(new JPanel(), toList(components));
    }

    /**
     * Do a horizontal layout of the given components.
     *
     * @param components The components
     * @param spacing _more_
     * @return The new container of the components
     */
    public static JPanel hbox(Component[] components, int spacing) {
        return hbox(new JPanel(), toList(components), spacing);
    }




    /**
     * Do a horizontal layout of the given components.
     *
     * @param panel The panel to use or, if null,  we'll create a new on.
     * @param components The components
     * @return The  container of the components
     */
    public static JPanel hbox(JPanel panel, List components) {
        return hbox(panel, components, 0);
    }




    /**
     * Do a horizontal layout of the given components.
     *
     * @param panel The panel to use or, if null,  we'll create a new on.
     * @param components The components
     * @param space SPacing to use
     * @return The  container of the components
     */
    public static JPanel hbox(JPanel panel, List components, int space) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < components.size(); i++) {
            panel.add((Component) components.get(i));
            if (space > 0) {
                panel.add(Box.createHorizontalStrut(space));
            }
        }
        return panel;
    }



    /**
     * This does a column oriented grid bag layout. It will layout
     * the given components in a grid with the given number of columns.
     * It is probably good to read up a bit on
     * <a href=http://java.sun.com/j2se/1.3/docs/api/java/awt/GridBagLayout.html>GridBagLayout</a>
     * <p>
     * The weights define how much weight or spacing to give to the width
     * of each column and the height of each row.
     * <p>
     * To define the anchor value, i.e., how to fill a component in its grid square,
     * you can either set the global static member <code>tmpAnchor</code> or, for individual
     * Components you can use the anchors table to provide a mapping from Component to an
     * Integer object holding the anchor value.
     * <p>
     * To define the fill value, i.e., how a component expands in its grid square,
     * you can either set the global static member <code>tmpFill</code> or, for individual
     * Components you can use the fills table to provide a mapping from Component to an
     * Integer object holding the fill value.
     * <p>
     * If insets is non-null it will use those insets for the spacing in the grid.
     * else if the static member tmpInsets is non-null then it will use those values
     * and then set tmpInsets to null. Else it uses DFLT_INSETS, which is 0 spacing.
     * <p>
     *
     *
     * @param parentContainer The container to add components to. May be null.
     * @param components The  components to layout
     * @param numberOfColumns How many columns in the grid layout
     * @param weightsX Defines how much weight to give to each column width. If there are more
     * columns than weights then we use the last weight.
     * @param weightsY Defines how much weight to give to each row height. If there are more
     * rows than weights then we use the last weight.
     * @param anchors Hashtable that maps Component to the Integer  which defines the component anchor
     * @param fills Hashtable that maps Component to the Integer  which defines the component fill
     * @param insets The insets to use in the grid
     * @return The parentContainer or the new panel if parentContainer is null
     */
    public static JPanel doLayout(JPanel parentContainer,
                                  Component[] components,
                                  int numberOfColumns, double[] weightsX,
                                  double[] weightsY, Hashtable anchors,
                                  Hashtable fills, Insets insets) {

        if (parentContainer == null) {
            parentContainer = new JPanel();
        }

        //TODO: When we move to 1.6 we need to remove this fix
        //Check if we've blown the size limit for gridbag
        if (components.length > 512) {
            //Not perfect but...
            Component[] comps1 = new Component[components.length / 2];
            Component[] comps2 = new Component[components.length / 2 + 1];
            int         cnt    = 0;
            for (int i = 0; i < components.length; i++) {
                if (i < comps1.length) {
                    comps1[i] = components[i];
                } else {
                    comps2[cnt++] = components[i];
                }
            }
            JComponent comp1 = doLayout(null, comps1, numberOfColumns,
                                        weightsX, weightsY, anchors, fills,
                                        insets);
            JComponent comp2 = doLayout(null, comps2, numberOfColumns,
                                        weightsX, weightsY, anchors, fills,
                                        insets);

            return vbox(comp1, comp2);
        }



        GridBagLayout l = new GridBagLayout();
        parentContainer.setLayout(l);

        GridBagConstraints consts = new GridBagConstraints();
        if (insets == null) {
            insets    = tmpInsets;
            tmpInsets = null;
        }
        consts.insets = ((insets == null)
                         ? DFLT_INSETS
                         : insets);


        int[] dfltColFills = null;
        if (tmpColFills != null) {
            dfltColFills = tmpColFills;
        }
        tmpColFills = null;

        int dfltAnchor = ((tmpAnchor == -1)
                          ? DFLT_ANCHOR
                          : tmpAnchor);
        tmpAnchor = -1;
        int dfltFill = ((tmpFill >= 0)
                        ? tmpFill
                        : GridBagConstraints.BOTH);
        tmpFill = -1;

        int    col     = 0;
        int    row     = 0;

        double weightX = 1.0;
        double weightY = 0.0;

        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            consts.anchor = dfltAnchor;

            consts.fill   = dfltFill;
            if ((fills != null) && (comp != null)) {
                Integer fill = (Integer) fills.get(comp);
                if (fill != null) {
                    consts.fill = fill.intValue();
                }
            } else if (dfltColFills != null) {
                consts.fill = dfltColFills[col];
            }

            if ((weightsX != null) && (col < weightsX.length)) {
                weightX = weightsX[col];
            }

            if ((weightsY != null) && (row < weightsY.length)) {
                weightY = weightsY[row];
            }

            boolean lastCol = false;

            if (col == (numberOfColumns - 1)) {
                lastCol          = true;
                consts.gridwidth = GridBagConstraints.REMAINDER;
            } else {
                col++;
            }
            consts.weightx = weightX;
            consts.weighty = weightY;



            if ((anchors != null) && (comp != null)) {
                Integer anchor = (Integer) anchors.get(comp);
                if (anchor != null) {
                    consts.anchor = anchor.intValue();
                }
            }
            if (lastCol) {
                col = 0;
                row++;
            }

            if (comp != null) {
                l.setConstraints(comp, consts);
                parentContainer.add(comp);
            }
            consts.gridwidth = 1;
        }
        return parentContainer;

    }








    /**
     *  Creates a JPanel and does a BorderLayout-NORTH of the given component
     *
     * @param top The component to place
     * @return The new JPanel
     */
    public static JPanel topLeft(Component top) {
        return top(left(top));
    }



    /**
     *  Creates a JPanel and does a BorderLayout-NORTH of the given component
     *
     * @param top The component to place
     * @return The new JPanel
     */
    public static JPanel top(Component top) {
        return topCenterBottom(top, filler(), null);
    }


    /**
     *  Creates a JPanel and does a BorderLayout.WEST of the given component
     *
     * @param left The component to place
     * @return The new JPanel
     */
    public static JPanel left(Component left) {
        return leftCenter(left, filler());
    }


    /**
     *  Creates a JPanel and does a BorderLayout NORTH/CENTER of the given components
     *
     * @param top The top component
     * @param center The center component
     * @return The new JPanel
     */
    public static JPanel topCenter(Component top, Component center) {
        return topCenterBottom(top, center, null);
    }




    /**
     *  Creates a JPanel and does a BorderLayout NORTH/BOTTOM of the given components
     *
     * @param top The top component
     * @param bottom The bottom component
     * @return The new JPanel
     */
    public static JPanel topBottom(Component top, Component bottom) {
        return topCenterBottom(top, null, bottom);
    }



    /**
     *  Creates a JPanel and does a BorderLayout CENTER/SOUTH of the given components
     *
     * @param center The center component
     * @param bottom The bottom component
     * @return The new JPanel
     */
    public static JPanel centerBottom(Component center, Component bottom) {
        return topCenterBottom(null, center, bottom);
    }


    /**
     *  Creates a JPanel and does a BorderLayout SOUTH of the given component
     *
     * @param bottom The bottom component
     * @return The new JPanel
     */
    public static JPanel bottom(Component bottom) {
        return topCenterBottom(null, null, bottom);
    }

    /**
     *  Creates a JPanel and does a BorderLayout CENTER of the given component
     *
     * @param center The center component
     * @return The new JPanel
     */
    public static JPanel center(Component center) {
        return topCenterBottom(null, center, null);
    }


    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param top The top component  (may be null)
     * @param center The center component (may be null)
     * @param bottom The bottom component (may be null)
     * @return The new JPanel
     */
    public static JPanel topCenterBottom(Component top, Component center,
                                         Component bottom) {
        JPanel p = new JPanel(new BorderLayout());
        if (top != null) {
            p.add("North", top);
        }
        if (center != null) {
            p.add("Center", center);
        }
        if (bottom != null) {
            p.add("South", bottom);
        }
        return p;
    }


    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param left The left component
     * @param right The right component
     * @return The new panel
     */
    public static JPanel leftRight(Component left, Component right) {
        return leftCenterRight(left, null, right);
    }

    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param left The left component
     * @param center The center component
     * @param right The right component
     * @return The new panel
     */
    public static JPanel leftCenterRight(Component left, Component center,
                                         Component right) {
        JPanel p = new JPanel(new BorderLayout());
        if (left != null) {
            p.add("West", left);
        }
        if (center != null) {
            p.add("Center", center);
        }
        if (right != null) {
            p.add("East", right);
        }
        return p;
    }

    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param center The center component
     * @param right The right component
     * @return The new panel
     */
    public static JPanel centerRight(Component center, Component right) {
        return leftCenterRight(null, center, right);
    }

    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param right The right component
     * @return The new panel
     */
    public static JPanel right(Component right) {
        return leftCenterRight(null, null, right);
    }



    /**
     *  Creates a JPanel and does a BorderLayout of the given components
     *
     * @param left The left component
     * @param center The center component
     * @return The new panel
     */

    public static JPanel leftCenter(Component left, Component center) {
        return leftCenterRight(left, center, null);
    }

    /**
     * A utility for having fillers in layout. This just returns  an
     * empty JPanel but we have it as a method so calling code can be explicit
     * in what they are intending.
     *
     * @return Filler
     */
    public static Component filler() {
        return new JPanel();
    }



    /**
     * A utility for having fillers in layout. This just returns  an
     * empty JPanel but we have it as a method so calling code can be explicit
     * in what they are intending.
     *
     *
     * @param width filler width
     * @param height filler height
     * @return Filler
     */
    public static JComponent filler(int width, int height) {
        JPanel filler = new JPanel();
        filler.setPreferredSize(new Dimension(width, height));
        return filler;
    }




}



















