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

package ucar.unidata.idv;


import org.w3c.dom.*;

import ucar.unidata.idv.ui.*;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.drawing.Glyph;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;


import ucar.visad.Plotter;
import ucar.visad.display.Animation;

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

import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 *
 * @author IDV development team
 */
public class VectorGraphicsRenderer implements Plotter.Plottable {

    /** The view manager */
    private ViewManager viewManager;

    /** ok flag */
    private boolean ok = true;

    /** dimension */
    private Dimension dim;



    /** label html */
    private String labelHtml;

    /** label position */
    private String labelPos = Glyph.PT_LR;

    /** label background */
    private Color labelBG = new Color(1.0f, 1.0f, 1.0f, 1.0f);


    /** label width */
    private int labelWidth = 200;

    /** preview flag */
    private boolean preview = false;

    /** flag for previewing */
    private boolean doingPreview = false;


    /**
     * Create a new vector graphics renderer for the view manager
     *
     * @param viewManager  the ViewManager
     */
    public VectorGraphicsRenderer(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    /**
     * Render to the file
     *
     * @param filename  the filename
     *
     * @throws Exception  problem writing to the file
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
     * Show the configuration dialog
     *
     * @return true if successful
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
     * Plot to the graphics
     *
     * @param graphics  the graphics to plot to
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
            boolean wasShowingScales = viewManager.getShowScales();
            if (wasShowingScales) {
                viewManager.setShowScales(false);
            }


            //Find all visibile displays
            final List<DisplayControl> onDisplays =
                new ArrayList<DisplayControl>();
            for (DisplayControl control :
                    (List<DisplayControl>) viewManager.getControls()) {
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
                control.toggleVisibilityForVectorGraphicsRendering(
                    DisplayControl.RASTERMODE_SHOWRASTER);
            }

            viewManager.toFront();
            Misc.sleep(1000);

            //capture the image of the rasters and write it into the graphics
            BufferedImage image = viewManager.getMaster().getImage(false);
            //            GuiUtils.showOkCancelDialog(null,"",new JLabel(new ImageIcon(image)),null);
            graphics.drawImage(image, 0, 0, null);

            //Now,  turn off rasters and turn on all non-raster
            for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                control.toggleVisibilityForVectorGraphicsRendering(
                    DisplayControl.RASTERMODE_SHOWNONRASTER);
            }

            if (wasShowingWireframe) {
                viewManager.setWireframe(true);
            }
            if (wasShowingScales) {
                viewManager.setShowScales(true);
            }
            Misc.sleep(500);

            //Render the scene graph
            SceneGraphRenderer renderer = new SceneGraphRenderer();
            DisplayImpl display =
                (DisplayImpl) viewManager.getMaster().getDisplay();
            boolean is3D = !viewManager.getDisplayRenderer().getMode2D();
            renderer.setTransformToScreenCoords(is3D);
            renderer.plot(graphics, display,
                          viewManager.getDisplayCoordinateSystem(),
                          dim.width, dim.height);
            //viewManager.getBp(ViewManager.PREF_3DCLIP));

            //Reset all displays
            for (DisplayControl control : (List<DisplayControl>) onDisplays) {
                control.toggleVisibilityForVectorGraphicsRendering(
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
                viewManager.paintDisplayList(
                    graphics, (List<DisplayControl>) onDisplays, width,
                    height, true, null, null);
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
                JComponent previewContents =
                    GuiUtils.centerBottom(
                        new JLabel(new ImageIcon(previewImage)),
                        GuiUtils.inset(
                            new JLabel(
                                "Note: The actual capture will take place after the preview window is dismissed so make sure the window is not occluded"), 5));
                ok = GuiUtils.showOkCancelDialog(null, "", previewContents,
                        null);
            }


        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        }


    }


    //TODO:For the pdf, ps, svg, this probably never gets called
    //Not sure what to do if it does get called. Maybe get the colors from the scenegraphrenderer

    /**
     * Get the colours
     *
     * @return  the color
     */
    public Color[] getColours() {
        return new Color[] { Color.red, Color.green, Color.blue };
    }

    /**
     * Get the size
     *
     * @return  the size (width, height)
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
