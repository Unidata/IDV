/*
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.unidata.idv;


import org.w3c.dom.*;

import ucar.unidata.idv.ui.*;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.drawing.Glyph;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;




import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Plotter;
import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;

import visad.bom.SceneGraphRenderer;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 *
 * @author IDV development team
 */
public class VectorRenderer implements Plotter.Plottable {

    /** _more_ */
    private ViewManager viewManager;

    /** _more_ */
    private boolean ok = true;

    /** _more_ */
    private Dimension dim;



    /** _more_ */
    private String labelHtml;

    /** _more_ */
    private String labelPos = Glyph.PT_LR;

    /** _more_ */
    private Color labelBG = new Color(1.0f, 1.0f, 1.0f, 1.0f);


    /** _more_ */
    private int labelWidth = 200;

    /** _more_ */
    private boolean preview = false;

    /** _more_ */
    private boolean doingPreview = false;


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public VectorRenderer(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void renderTo(String filename) throws Exception {
        Component comp = viewManager.getMaster().getDisplayComponent();
        dim = comp.getSize();
        Plotter plotter = new Plotter(filename);
        if (preview) {
            doingPreview = true;
            ok           = true;
            plotter.plot(this);
            if ( !ok) {
                return;
            }

        }
        doingPreview = false;
        plotter.plot(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showConfigDialog() {
        GuiUtils.ColorSwatch labelBGFld = new GuiUtils.ColorSwatch(labelBG,
                                              "Label Color", true);


        JTextField widthFld = new JTextField("" + labelWidth, 5);
        JTextArea  ta       = new JTextArea(labelHtml, 5, 50);
        ta.setToolTipText(
            "<html>Can be HTML<br>Use '%time%' to include current animation time</html>");
        Vector positions = new Vector(Misc.toList(new Object[] {
                               new TwoFacedObject("Upper Left", Glyph.PT_UL),
                               new TwoFacedObject(
                                   "Upper Right",
                                   Glyph.PT_UR), new TwoFacedObject(
                                       "Lower Left", Glyph.PT_LL),
                               new TwoFacedObject("Lower Right",
                                   Glyph.PT_LR) }));
        JComboBox posBox = new JComboBox(positions);
        posBox.setSelectedItem(TwoFacedObject.findId(labelPos, positions));

        JCheckBox  previewCbx = new JCheckBox("", preview);

        JComponent labelComp  = GuiUtils.hbox(new Component[] {
            new JLabel("Position:"), posBox, new JLabel("Width:"), widthFld,
            GuiUtils.rLabel("Background:"), labelBGFld.getPanel()
        }, 5);
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent comp = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Preview:"), GuiUtils.left(previewCbx),
            GuiUtils.rLabel("Label:"), GuiUtils.left(labelComp),
            GuiUtils.rLabel(""), GuiUtils.makeScrollPane(ta, 200, 100)
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_NY);
        comp = GuiUtils.topCenter(
            GuiUtils.inset(
                new JLabel(
                    "Note: The display needs to be in an overhead view"), 5), comp);
        if (GuiUtils.showOkCancelDialog(null, "Legend Label", comp, null)) {
            labelHtml  = ta.getText();

            labelPos   = TwoFacedObject.getIdString(posBox.getSelectedItem());
            labelWidth = new Integer(widthFld.getText().trim()).intValue();
            labelBG    = labelBGFld.getSwatchColor();
            preview    = previewCbx.isSelected();
            return true;

        } else {
            return false;
        }
    }

    /**
     * _more_
     *
     * @param graphics _more_
     */
    public void plot(Graphics2D graphics) {

        try {
            //Turn off the display list 
            boolean wasShowingDisplayList = viewManager.getShowDisplayList();
            if (wasShowingDisplayList) {
                viewManager.setShowDisplayList(false);
            }
            boolean wasShowingWireframe = viewManager.getWireframe();
            if (wasShowingWireframe) {
                viewManager.setWireframe(false);
            }


            //Find all visibile displays
            final List<DisplayControl> onDisplays =
                new ArrayList<DisplayControl>();
            for (DisplayControl control : (List<DisplayControl>) viewManager
                    .getControls()) {
                if (control.getDisplayVisibility()) {
                    onDisplays.add(control);
                }
            }

            //If previewing then use the graphics from an image
            BufferedImage previewImage = null;
            if (doingPreview) {
                previewImage = new BufferedImage(dim.width, dim.height,
                        BufferedImage.TYPE_INT_RGB);
                graphics = (Graphics2D) previewImage.getGraphics();
            }

            //Turn off all non-raster
            for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                control.toggleVisibilityForVectorRendering(
                    DisplayControl.RASTERMODE_SHOWRASTER);
            }

            viewManager.toFront();
            Misc.sleep(250);

            //capture the image of the rasters and write it into the graphics
            BufferedImage image = viewManager.getMaster().getImage(false);
            graphics.drawImage(image, 0, 0, null);

            //Now,  turn off rasters and turn on all non-raster
            for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                control.toggleVisibilityForVectorRendering(
                    DisplayControl.RASTERMODE_SHOWNONRASTER);
            }

            if (wasShowingWireframe) {
                viewManager.setWireframe(true);
            }

            //Render the scene graph
            SceneGraphRenderer renderer = new SceneGraphRenderer();
            DisplayImpl display =
                (DisplayImpl) viewManager.getMaster().getDisplay();
            renderer
                .plot(graphics, display,
                      ((NavigatedDisplay) viewManager.getMaster())
                          .getDisplayCoordinateSystem(), dim.width,
                              dim.height);


            //Reset all displays
            for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                control.toggleVisibilityForVectorRendering(
                    DisplayControl.RASTERMODE_SHOWALL);
            }

            //Now, draw the display list using the graphics
            int height = dim.height;
            int width  = dim.width;
            if (wasShowingDisplayList) {
                int  cnt = 0;
                Font f   = viewManager.getDisplayListFont();
                graphics.setFont(f);
                FontMetrics fm         = graphics.getFontMetrics();
                int         lineHeight = fm.getAscent() + fm.getDescent();
                for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                    if ( !control.getShowInDisplayList()) {
                        continue;
                    }
                    Data data = control.getDataForDisplayList();
                    if (data == null) {
                        continue;
                    }
                    String text = null;
                    if (data instanceof visad.Text) {
                        text = ((visad.Text) data).getValue();
                    } else if (data instanceof FieldImpl) {
                        Real now =
                            viewManager.getAnimation()
                                .getCurrentAnimationValue();
                        if (now != null) {
                            FieldImpl fi = (FieldImpl) data;
                            Data rangeValue = fi.evaluate(now,
                                                  Data.NEAREST_NEIGHBOR,
                                                  Data.NO_ERRORS);
                            if ((rangeValue != null)
                                    && (rangeValue instanceof visad.Text)) {
                                text = ((visad.Text) rangeValue).getValue();
                            }
                        }
                    }
                    if ((text == null) || (text.length() == 0)) {
                        continue;
                    }
                    Color c =
                        ((ucar.unidata.idv.control
                            .DisplayControlImpl) control)
                                .getDisplayListColor();
                    if (c == null) {
                        c = viewManager.getDisplayListColor();
                    }
                    graphics.setColor(c);
                    int lineWidth = fm.stringWidth(text);
                    graphics.drawString(text, width / 2 - lineWidth / 2,
                                        height - 2
                                        - ((lineHeight + 1) * cnt));
                    cnt++;
                }
                viewManager.setShowDisplayList(true);
            }


            if ((labelHtml != null) && (labelHtml.trim().length() > 0)) {
                Real dttm =
                    viewManager.getAnimation().getCurrentAnimationValue();
                String dttmString = ((dttm != null)
                                     ? dttm.toString()
                                     : "none");
                labelHtml = labelHtml.replace("%time%", dttmString);

                JEditorPane editor = ImageUtils.getEditor(null, labelHtml,
                                         labelWidth, null, null);

                if (labelBG != null) {
                    editor.setBackground(labelBG);
                } else {
                    editor.setBackground(viewManager.getBackground());
                }
                editor.setBorder(
                    BorderFactory.createBevelBorder(BevelBorder.RAISED));
                RepaintManager repaintManager =
                    RepaintManager.currentManager(editor);

                repaintManager.setDoubleBufferingEnabled(false);
                Dimension       cdim  = editor.getSize();
                AffineTransform tform = graphics.getTransform();
                int             dx    = 0,
                                dy    = 0;
                int             pad   = 5;
                if (labelPos.equals(Glyph.PT_LR)) {
                    dx = width - cdim.width - pad;
                    dy = height - cdim.height - pad;
                } else if (labelPos.equals(Glyph.PT_LL)) {
                    dx = pad;
                    dy = height - cdim.height - pad;
                } else if (labelPos.equals(Glyph.PT_UL)) {
                    dx = pad;
                    dy = pad;
                } else if (labelPos.equals(Glyph.PT_UR)) {
                    dx = width - cdim.width - pad;
                    dy = pad;
                }

                AffineTransform translate =
                    AffineTransform.getTranslateInstance(dx, dy);
                tform.concatenate(translate);
                graphics.setTransform(tform);
                editor.paint(graphics);
                graphics.setTransform(tform);
                repaintManager.setDoubleBufferingEnabled(true);
            }
            if (doingPreview) {
                ok = GuiUtils.showOkCancelDialog(null, "",
                        new JLabel(new ImageIcon(previewImage)), null);
            }


        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        }


    }


    //TODO:For the pdf, ps, svg, this probably never gets called
    //Not sure what to do if it does get called. Maybe get the colors from the scenegraphrenderer

    /**
     * _more_
     *
     * @return _more_
     */
    public Color[] getColours() {
        return new Color[] { Color.red, Color.green, Color.blue };
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int[] getSize() {
        return new int[] { dim.width, dim.height };
    }

    /**
     *  Set the LabelHtml property.
     *
     *  @param value The new value for LabelHtml
     */
    public void setLabelHtml(String value) {
        labelHtml = value;
    }

    /**
     *  Get the LabelHtml property.
     *
     *  @return The LabelHtml
     */
    public String getLabelHtml() {
        return labelHtml;
    }

    /**
     *  Set the LabelPos property.
     *
     *  @param value The new value for LabelPos
     */
    public void setLabelPos(String value) {
        labelPos = value;
    }

    /**
     *  Get the LabelPos property.
     *
     *  @return The LabelPos
     */
    public String getLabelPos() {
        return labelPos;
    }

    /**
     *  Set the LabelBG property.
     *
     *  @param value The new value for LabelBG
     */
    public void setLabelBG(Color value) {
        labelBG = value;
    }

    /**
     *  Get the LabelBG property.
     *
     *  @return The LabelBG
     */
    public Color getLabelBG() {
        return labelBG;
    }

    /**
     *  Set the LabelWidth property.
     *
     *  @param value The new value for LabelWidth
     */
    public void setLabelWidth(int value) {
        labelWidth = value;
    }

    /**
     *  Get the LabelWidth property.
     *
     *  @return The LabelWidth
     */
    public int getLabelWidth() {
        return labelWidth;
    }

    /**
     *  Set the Preview property.
     *
     *  @param value The new value for Preview
     */
    public void setPreview(boolean value) {
        preview = value;
    }

    /**
     *  Get the Preview property.
     *
     *  @return The Preview
     */
    public boolean getPreview() {
        return preview;
    }




}

