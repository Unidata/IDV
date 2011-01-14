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

package ucar.unidata.idv.control;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.ui.colortable.ColorTableEditor;


import ucar.unidata.ui.colortable.ColorTableManager;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;

import ucar.visad.UtcDate;

import visad.DateTime;

import visad.Unit;

import visad.VisADException;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;

import java.awt.*;
import java.awt.event.*;



import java.awt.geom.Rectangle2D;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * Provides label and mouse over display for color tables
 *
 * @author  Jeff McWhirter
 * @version $Revision: 1.11 $
 */

public class RangeColorPreview extends JPanel implements MouseMotionListener,
        MouseListener {

    /** Is this widget for the main window or for a legend */
    boolean forMain;

    /** If for a legend is this for the side or bottom legend */
    int legendType;

    /** For formatting */
    DisplayConventions displayConventions;


    /** font for range */
    private static Font rangeFont;

    /** x position in mouse preview */
    private int previewMouseX;

    /** flag for whether mouse is in the preview */
    private boolean mouseInPreview = false;

    /** lower label */
    JLabel lowerLbl;

    /** upper label */
    JLabel upperLbl;

    /** middle label */
    JLabel middleLbl;

    /** My range */
    Range range;


    /** preferred size */
    Dimension myPreferredSize;

    /** is this a time range? */
    private boolean isTime;

    /** the list of colors */
    private List<Color> colors;


    /**
     * Create a new color table preview
     *
     * @param colors the colors
     * @param dc For formatting
     */
    public RangeColorPreview(List<Color> colors, DisplayConventions dc) {
        this(colors, dc, DisplayControlImpl.SIDE_LEGEND, false, false);
    }


    /**
     * Create a new color table preview
     *
     * @param colors the colors
     * @param dc For formatting
     * @param isTime  is this a time range
     */
    public RangeColorPreview(List<Color> colors, DisplayConventions dc,
                             boolean isTime) {
        this(colors, dc, DisplayControlImpl.SIDE_LEGEND, false, isTime);
    }


    /**
     * Create a new color table preview
     *
     * @param colors the colors
     * @param dc For formatting
     * @param legendType What legend
     * @param forMain Or in main window
     */
    public RangeColorPreview(List<Color> colors, DisplayConventions dc,
                             int legendType, boolean forMain) {
        this(colors, dc, DisplayControlImpl.SIDE_LEGEND, forMain, false);
    }

    /**
     * Create a new color table preview
     *
     * @param colors the colors
     * @param dc For formatting
     * @param legendType What legend
     * @param forMain Or in main window
     * @param isTime  is this a time range
     */
    public RangeColorPreview(List<Color> colors, DisplayConventions dc,
                             int legendType, boolean forMain,
                             boolean isTime) {
        this.colors             = colors;
        this.forMain            = forMain;
        this.displayConventions = dc;
        this.legendType         = legendType;
        this.isTime             = isTime;
        addMouseMotionListener(this);
        addMouseListener(this);
        lowerLbl  = new JLabel(StringUtil.padRight("", 5), JLabel.RIGHT);
        upperLbl  = new JLabel(StringUtil.padLeft("", 5), JLabel.LEFT);
        middleLbl = new JLabel(StringUtil.padRight("", 5), JLabel.RIGHT);
        Font lblFont = lowerLbl.getFont();
        Font monoFont = new Font("Monospaced", lblFont.getStyle(),
                                 lblFont.getSize());
        if (forSideLegend()) {
            lowerLbl.setFont(monoFont);
            upperLbl.setFont(monoFont);
            middleLbl.setFont(monoFont);
        }
    }


    /**
     * Get the maximum size
     *
     * @return the size
     */
    public Dimension xxxgetMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 15);
    }

    /**
     * Local implementation of preferred size setting
     *
     * @param size   new size
     */
    public void setMyPreferredSize(Dimension size) {
        myPreferredSize = size;
    }

    /**
     * Get the preferred size
     *
     * @return  preferred size
     */
    public Dimension getPreferredSize() {
        return ((myPreferredSize != null)
                ? myPreferredSize
                : super.getPreferredSize());
    }




    /**
     * Make the contents
     *
     * @return The contents
     */
    public JPanel doMakeContents() {
        if ( !forMain) {
            switch (legendType) {

              case (DisplayControlImpl.SIDE_LEGEND) :
                  setMyPreferredSize(new Dimension(175, 15));
                  lowerLbl.setHorizontalAlignment(JLabel.LEFT);
                  upperLbl.setHorizontalAlignment(JLabel.RIGHT);
                  middleLbl.setHorizontalAlignment(JLabel.CENTER);
                  //JPanel labels = GuiUtils.leftCenterRight(lowerLbl,
                  //middleLbl,
                  //                                                           upperLbl);

                  JPanel labels = GuiUtils.leftRight(lowerLbl, upperLbl);
                  JComponent comp = GuiUtils.doLayout(new Component[] { this,
                          labels }, 1, GuiUtils.WT_Y, GuiUtils.WT_NN);
                  return GuiUtils.left(comp);

              case (DisplayControlImpl.BOTTOM_LEGEND) :
              default :
                  setMyPreferredSize(new Dimension(150, 15));
                  return GuiUtils.doLayout(new Component[] { lowerLbl, this,
                          upperLbl }, 3, GuiUtils.WT_NYN, GuiUtils.WT_N);
            }
        } else {
            setMyPreferredSize(new Dimension(125, 15));
            setMinimumSize(new Dimension(125, 15));
            return GuiUtils.leftCenterRight(lowerLbl, GuiUtils.bottom(this),
                                            upperLbl);
        }
    }


    /**
     * get the range
     *
     * @return The range
     */
    public Range getRange() {
        return range;
    }


    /**
     * Is this for the side legend
     *
     * @return Is this widget displaying in the side legend
     */
    protected boolean forSideLegend() {
        return ( !forMain && (legendType == DisplayControlImpl.SIDE_LEGEND));
    }


    /**
     * Set the range for the component
     *
     * @param range  new range
     */
    public void setRange(Range range) {
        setRange(range, null);
    }


    /**
     * Set the range for the component
     *
     * @param range  new range
     * @param unit The unit
     */
    public void setRange(Range range, Unit unit) {
        this.range = range;
        String lower;
        String upper;
        String unitString = "";
        if (unit != null) {
            unitString = unit.toString();
            if (forSideLegend()) {
                if (unitString.length() > 12) {
                    unitString = unitString.substring(0, 12) + ".";
                }
            }
            unitString = " " + unitString;
        }
        if (range != null) {
            if ( !isTime) {
                lower = displayConventions.format(range.getMin());
                if (forSideLegend()) {
                    upper = displayConventions.format(range.getMax());
                } else {
                    upper = displayConventions.format(range.getMax())
                            + unitString;
                }
            } else {
                try {
                    lower = UtcDate.getHHMM(new DateTime(range.getMin()));
                    upper = UtcDate.getHHMM(new DateTime(range.getMax()));
                } catch (VisADException ve) {
                    lower = "";
                    upper = "";
                }
            }
        } else {
            lower = "";
            upper = "";
        }
        if ( !forSideLegend()) {
            lowerLbl.setText(StringUtil.padLeft(lower, 5));
            upperLbl.setText(StringUtil.padRight(upper, 5));
        } else {
            lowerLbl.setText(lower);
            middleLbl.setText(unitString);
            upperLbl.setText(upper);
        }
    }




    /**
     * Update the graphics
     *
     * @param g  graphics to update
     */
    public void paint(Graphics g) {
        super.paint(g);
        ColorTableCanvas.paintColors(g, getBounds(), colors, false, false,
                                     null);
        if ( !mouseInPreview) {
            return;
        }
        if (rangeFont == null) {
            rangeFont = g.getFont().deriveFont(10.0f);
        }
        Rectangle bounds = getBounds();
        //Sanity check
        if ((bounds.width == 0) || (getRange() == null)) {
            return;
        }
        double percent     = previewMouseX / (double) bounds.width;
        double value       = getRange().getValueOfPercent(percent);
        String rangeString = "";
        if (isTime) {
            try {
                rangeString = UtcDate.getHHMM(new DateTime(value));
            } catch (VisADException ve) {
                rangeString = "";
            }
        }
        if (rangeString.length() == 0) {
            rangeString = displayConventions.format(value);
        }
        g.setFont(rangeFont);
        FontMetrics fm      = g.getFontMetrics();
        Rectangle2D sBounds = fm.getStringBounds(rangeString, g);
        int         width   = fm.stringWidth(rangeString);
        g.setColor(Color.lightGray);
        g.fillRect(2, (int) (bounds.y + bounds.height - sBounds.getHeight()),
                   (int) (sBounds.getWidth()), (int) (sBounds.getHeight()));
        g.drawLine(previewMouseX, 0, previewMouseX, bounds.height - 2);
        g.setColor(Color.black);
        g.drawString(rangeString, 2, bounds.height - 1);
    }

    /**
     * Set the colors
     *
     * @param colors the colors
     */
    public void setColors(List<Color> colors) {
        this.colors = colors;
        repaint();
    }


    /**
     * Mouse dragged event handler
     *
     * @param e  the MouseEvent
     */
    public void mouseDragged(MouseEvent e) {}

    /**
     * Mouse moved event handler
     *
     * @param e  the MouseEvent
     */
    public void mouseMoved(MouseEvent e) {
        previewMouseX  = e.getX();
        mouseInPreview = true;
        repaint();
    }

    /**
     * Mouse released event handler
     *
     * @param e  the MouseEvent
     */
    public void mouseReleased(MouseEvent e) {}


    /**
     * Mouse clicked event handler
     *
     * @param event  the MouseEvent
     */
    public void mouseClicked(MouseEvent event) {}

    /**
     * Mouse entered event handler
     *
     * @param event  the MouseEvent
     */
    public void mouseEntered(MouseEvent event) {
        repaint();
    }

    /**
     * Mouse exited event handler
     *
     * @param event  the MouseEvent
     */
    public void mouseExited(MouseEvent event) {
        mouseInPreview = false;
        repaint();
    }



    /**
     * Mouse pressed event handler
     *
     * @param event  the MouseEvent
     */
    public void mousePressed(MouseEvent event) {}

    /**
     * Is this a time preview?
     * @return true if it is
     */
    public boolean getIsTime() {
        return isTime;
    }
}
