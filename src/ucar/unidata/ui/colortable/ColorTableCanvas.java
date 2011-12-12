/*
 * $Id: ColorTableCanvas.java,v 1.30 2007/05/25 16:39:15 dmurray Exp $
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






package ucar.unidata.ui.colortable;


import ucar.unidata.ui.Command;
import ucar.unidata.ui.CommandManager;


import java.awt.image.*;
import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;

import java.awt.*;

import java.awt.color.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 *  This class provides the guts of the color table editor.   The actual ColorTableCanvas
 *  is a JPanel that draws the color bar in the editor. This class also provides
 *  the gui components that are wrapped the canvas itself.
 */
public class ColorTableCanvas extends JPanel implements MouseMotionListener,
        MouseListener, KeyListener {

    /** For logging */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ColorTableCanvas.class.getName());

    /** Represents when we don;t ahve a breakpoint selected */
    private static final int BREAKPOINT_NONE = -1;

    /** Name of property for the property event listener when color table has changed */
    public static final String PROP_COLORTABLE = "prop.colortable";

    /** Name of property for the property event listener when range has changed */
    public static final String PROP_RANGE = "prop.range";

    /** Name of property for the property event listener when cancel was pressed */
    public static final String PROP_CANCEL = "prop.cancel";

    /** Name of property for the property event listener when window was closed */
    public static final String PROP_CLOSE = "prop.close";

    /** The margin above/below the main color box */
    private static final int MARGIN_V = 30;

    /** The margin right/left of the main color box */
    private static final int MARGIN_H = 40;

    /** Size of breakpoint triangle */
    private static final int ARROW_HEIGHT = 8;

    /** Size of breakpoint triangle */
    private static final int ARROW_WIDTH = ARROW_HEIGHT;


    /** No change when dragging bps */
    private static final int MODE_NONE = 0;

    /** Change  fill when dragging bps */
    private static final int MODE_FILL = 2;


    /** Interpolate when dragging bps */
    private static final int MODE_INTERPOLATE = 1;


    /** Change trans. when dragging bps */
    private static final int MODE_TRANSPARENCY = 3;

    /** Change trans. when dragging bps */
    private static final int MODE_BRIGHTNESS = 4;

    /** The drawing modes */
    private static final int[] modes = { MODE_FILL, MODE_INTERPOLATE,
                                         MODE_BRIGHTNESS, MODE_TRANSPARENCY };

    private int DFLT_COLOR_SPACE = -999;

    /** The drawing mode names */
    private static final String[] modeNames = { "Fill", "Interpolate",
            "Brightness:", "Transparency:" };



    /** image */
    private Image lockImage;

    /** The editor */
    private ColorTableEditor editor;

    /** Font to use for labels_ */
    private Font labelFont = new Font("Dialog", Font.PLAIN, 12);

    /** Bold font to use */
    private Font boldFont = new Font("Dialog", Font.BOLD, 12);

    /** Tracks if the color table has been changed on a mouse drag. */
    private boolean needToPropagateChanges = false;

    /** _more_ */
    private Color selectedColor;

    /** _more_ */
    private boolean cursorOver = false;

    /** _more_ */
    private int cursorPosition = 0;

    /** Allows us to not propagate during the mouse drag */
    private boolean okToPropagateChangesNow = true;

    /** Should we propagate changes */
    private boolean propagateChanges = true;

    /** _more_ */
    private JCheckBox setColorFromChooserCbx =
        new JCheckBox("Actively set color", true);

    /** _more_ */
    private JComboBox colorSpaceCbx;

    /** THe color chooser */
    private JColorChooser colorChooser;

    /** Shows how many cells in the color table */
    private JLabel colorCntLabel = GuiUtils.cLabel("     ");

    /** Shows the min range value */
    private JTextField minField = new JTextField(4);

    /** Shows the max range value */
    private JTextField maxField = new JTextField(4);

    /** Shows the category */
    private JComboBox categoryField;

    /** Transpency box */
    private JComboBox transBox;

    private boolean ignoreTransBoxEvents = false;

    /** Transpancy box */
    private JComboBox brightnessBox;

    /** Keeps track of the last alpha value. Not used now. */
    private int lastAlpha;

    /** The range */
    private Range myRange;

    /** List of Colors from the color table for painting. */
    private ArrayList colorList = new ArrayList();

    /** scales */
    private ArrayList scales = new ArrayList();

    /** The CT we first are editing */
    private ColorTable originalColorTable;

    /** The currently changed/edited CT */
    private ColorTable currentColorTable;

    /** Keeps track of the last index that was changed */
    private int lastChangeIdx = -1;

    /** last control key */
    private int lastControlKey = -1;

    /** Property change listeners */
    private ArrayList listeners;

    /** List of Double for the breakpoints */
    private ArrayList breakpoints;


    /** The selected BP */
    private ColorTable.Breakpoint currentBP = null;

    /** Color table name */
    private JLabel nameLabel;

    /** Some GUI panel */
    private JPanel wrapper;

    /** Drawing mode */
    private int currentMode = MODE_FILL;

    /** _more_ */
    private boolean mouseInBox = false;

    /** Something used when drawing */
    private double activePercent;


    /** Use trans. mode */
    private JRadioButton modeTransparencyBtn;

    /** _more_ */
    private JComponent colorSwatch;

    /** Use trans. mode */
    private JRadioButton modeBrightnessBtn;

    /** Groups the mode radio buttons */
    private ButtonGroup modeGroup;


    /** Something used when drawing */
    private int activeColorIndex;

    /** Something used when drawing */
    private Color activeColor;

    /** Keeps track of the previous colors when drawing */
    private ArrayList priorColors;

    /** Holds the edit commands for undo_ */
    private CommandManager commands = new CommandManager(5000);


    /** The normal cursor_ */
    public static final Cursor normalCursor =
        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    /** _more_ */
    public static Cursor paintCursor;


    /**
     * Create me
     *
     */
    public ColorTableCanvas() {
        this(null, null);
    }



    /**
     * Create me
     *
     * @param editor The editor I am part of
     * @param table The color table to edit
     *
     */
    public ColorTableCanvas(ColorTableEditor editor, ColorTable table) {
        this.editor = editor;
        init(table);
    }


    /**
     * Init me
     *
     * @param newTable The color table to edit
     */
    public void init(ColorTable newTable) {
        //        setToolTipText ("Ctrl-drag to color; Shift-drag to interpolate;Ctrl-shift-drag to set transparency");
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setPreferredSize(new Dimension(250, MARGIN_V * 2 + 40));
        setMinimumSize(new Dimension(250, MARGIN_V * 2 + 40));
        setColorTableInner(newTable, false);
    }

    /**
     * GOt a new colortable
     *
     * @param newTable The new table
     */
    public void setColorTable(ColorTable newTable) {
        commands.add(new ColorTableCommand(this, newTable));
    }

    /**
     * Got a new colortable
     *
     * @param newTable The new table
     * @param notifyChange Should listeners be told
     */
    private void setColorTableInner(ColorTable newTable,
                                    boolean notifyChange) {
        currentBP = null;

        if (newTable == null) {
            newTable = editor.getColorTableManager().getDefaultColorTable();
        }

        if (isStandAlone() || !notifyChange || (myRange == null)) {
            Range tableRange = newTable.getRange();
            if (tableRange != null) {
                myRange = new Range(tableRange);
            } else {
                newTable.setRange(myRange = new Range(0.0, 100.0));
            }
        }
        if (originalColorTable == null) {
            originalColorTable = newTable;
        }
        currentColorTable = new ColorTable(newTable);
        setColorList(currentColorTable.getColorList(), null);
        breakpoints = cloneList(currentColorTable.getBreakpoints());
        if (breakpoints.size() == 0) {
            addToBreakpoints(0.0);
            addToBreakpoints(1.0);
        }

        if (categoryField != null) {
            categoryField.setSelectedItem(currentColorTable.getCategory());
            minField.setText(myRange.formatMin());
            maxField.setText(myRange.formatMax());
            repaint();
        }

        //        Misc.printStack ("setCTInner:" + currentColorTable, 5,null);
        if (nameLabel != null) {
            nameLabel.setText(currentColorTable.getName());
            nameLabel.repaint();
        }

        if (editor != null) {
            editor.setWindowTitle(GuiUtils.getApplicationTitle()
                                  + "Color Table Editor -- "
                                  + currentColorTable.getName());
        }


        if (notifyChange) {
            tableChanged();
        }


    }



    /**
     * Get the main GUI contents.
     *
     * @return The GUI
     */
    public JPanel getContents() {

        colorChooser = new JColorChooser();
        colorChooser.setPreviewPanel(new JPanel());
        colorChooser.getSelectionModel().addChangeListener(
            new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                selectedColor = colorChooser.getColor();
                colorSwatch.setBackground(selectedColor);
                if (inPaint) {
                    return;
                }
                if (setColorFromChooserCbx.isSelected()) {
                    setColor(selectedColor);
                }
            }
        });

        //Remove the swatches
        colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[0]);



        JPanel leftBtn  = getAddRemoveButton(true);
        JPanel rightBtn = getAddRemoveButton(false);
        JPanel colorPanel = GuiUtils.leftCenterRight(leftBtn, colorCntLabel,
                                rightBtn);



        minField.setText(myRange.formatMin());
        maxField.setText(myRange.formatMax());
        ActionListener minMaxListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setMinMax();
            }
        };

        minField.addActionListener(minMaxListener);
        maxField.addActionListener(minMaxListener);

        brightnessBox = new JComboBox(new Vector(getBrightnessList()));
        brightnessBox.setSelectedIndex(6);
        brightnessBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                modeBrightnessBtn.setSelected(true);
                if (setColorFromChooserCbx.isSelected()) {
                    if (currentBP != null) {
                        setBrightness(currentBP, currentBP);
                    }
                }
            }
        });

        transBox = new JComboBox(new Vector(getTransparencyList()));
        //This is a little odd but when the box is editable its preferred size is 
        //increased. So we get the preferred size before, then set editable then reset the 
        //preferred size.
        Dimension preferred = transBox.getPreferredSize();
        transBox.setEditable(true);
        if (GuiUtils.checkHeight(preferred.height)) {
            transBox.setPreferredSize(preferred);
        }

        transBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if(ignoreTransBoxEvents) return;
                modeTransparencyBtn.setSelected(true);
                if (setColorFromChooserCbx.isSelected()) {
                    if (currentBP != null) {
                        ignoreTransBoxEvents = true;
                        setTransparency(currentBP, currentBP);
                        ignoreTransBoxEvents = false;
                    }
                }
            }
        });


        ButtonGroup modeGroup = new ButtonGroup();
        List        modeBtns  = Misc.newList(new JLabel("Paint Mode: "));
        colorSwatch = GuiUtils.filler(30, 15);
        colorSwatch.setSize(new Dimension(30, 15));
        //        colorSwatch.setBorder(BorderFactory.createEtchedBorder());
        colorSwatch.setBorder(BorderFactory.createLineBorder(Color.black));
        for (int i = 0; i < modes.length; i++) {
            JRadioButton rb = new JRadioButton(modeNames[i], (i == 0));
            modeGroup.add(rb);
            modeBtns.add(rb);
            rb.addItemListener(new ObjectListener(new Integer(modes[i])) {
                public void itemStateChanged(ItemEvent e) {
                    if (((JRadioButton) e.getSource()).isSelected()) {
                        currentMode = ((Integer) theObject).intValue();
                    }
                }
            });

            //Add in the color swatch after the fill
            if (modes[i] == MODE_FILL) {
                //                modeBtns.add(GuiUtils.wrap(colorSwatch));
            }

            //Add in the transparency box after the
            if (modes[i] == MODE_TRANSPARENCY) {
                modeBtns.add(GuiUtils.left(GuiUtils.wrap(transBox)));
                modeTransparencyBtn = rb;
            }

            //Add in the transparency box after the
            if (modes[i] == MODE_BRIGHTNESS) {
                modeBtns.add(GuiUtils.left(GuiUtils.wrap(brightnessBox)));
                modeBrightnessBtn = rb;
            }
        }






        JButton rangeBtn = new JButton("Range:");
        rangeBtn.addActionListener(minMaxListener);

        GuiUtils.setNoFill();
        GuiUtils.tmpInsets = new Insets(0, 2, 0, 2);
        JPanel valuePanel =
            GuiUtils.inset(GuiUtils.doLayout(new Component[] { rangeBtn,
                minField, new JLabel("-"), maxField }, 4, GuiUtils.WT_N,
                    GuiUtils.WT_N), 4);
        JPanel bottom = new Msg.SkipPanel(GuiUtils.leftCenterRight(leftBtn,
                            colorCntLabel, rightBtn));

        JPanel thisComponent = GuiUtils.center(this);
        thisComponent.setBorder(BorderFactory.createEtchedBorder());
        JPanel modePanel = GuiUtils.hbox(modeBtns, 7);
        JPanel topPanel = GuiUtils.leftCenterRight(commands.getContents(),
                              null, valuePanel);
        //        topPanel = GuiUtils.vbox(topPanel, modePanel);
        //        wrapper  = GuiUtils.topCenterBottom(topPanel, this, bottom);

        topPanel = GuiUtils.vbox(topPanel, bottom);
        wrapper = GuiUtils.topCenterBottom(topPanel, thisComponent,
                                           GuiUtils.left(modePanel));
        JPanel wrapperWrapper = GuiUtils.inset(wrapper, 5);

        //        wrapperWrapper.setBorder(BorderFactory.createEtchedBorder());
        nameLabel = new JLabel(currentColorTable.getName());
        Font font = nameLabel.getFont();
        nameLabel.setFont(font.deriveFont(Font.ITALIC | Font.BOLD));
        categoryField = new JComboBox(
            editor.getColorTableManager().getCategories().toArray());
        categoryField.setToolTipText("Use \">\" to define sub-categories");
        categoryField.setEditable(true);
        categoryField.setSelectedItem(currentColorTable.getCategory());

        Component fieldPanel =
            GuiUtils.leftRight(
                GuiUtils.hbox(
                    new JLabel("Color table: ", SwingConstants.RIGHT),
                    nameLabel), GuiUtils.hbox(
                        new JLabel("Category: ", SwingConstants.RIGHT),
                        GuiUtils.wrap(categoryField)));

        setColorFromChooserCbx.setToolTipText(
            "Automatically change the color of the selected breakpoint");
        Vector colorSpaces = new Vector();
	


	colorSpaces.add(new TwoFacedObject("Default", DFLT_COLOR_SPACE));
	colorSpaces.add(new TwoFacedObject("RGB", ColorSpace.CS_LINEAR_RGB));
        colorSpaces.add(new TwoFacedObject("SRGB", ColorSpace.CS_sRGB));
        colorSpaces.add(new TwoFacedObject("CIE", ColorSpace.CS_CIEXYZ));
        colorSpaces.add(new TwoFacedObject("GRAY", ColorSpace.CS_GRAY));
        colorSpaces.add(new TwoFacedObject("PYCC", ColorSpace.CS_PYCC));

        colorSpaceCbx = new JComboBox(colorSpaces);
        JPanel colorChooserPanel =
            GuiUtils.topCenter(
                GuiUtils.leftRight(
                    GuiUtils.hbox(
                        setColorFromChooserCbx,
                        GuiUtils.wrap(
				      colorSwatch)), GuiUtils.label("Color Space:",colorSpaceCbx)), colorChooser);


        JPanel contents = GuiUtils.inset(
                              GuiUtils.topCenterBottom(
                                  GuiUtils.inset(fieldPanel, 4),
                                  wrapperWrapper, colorChooserPanel), 4);
        return contents;
    }

    /**
     *  Create a List of {@ref ucar.unidata.util.TwoFacedObject}s that contain
     *  the set of predefined transparecny percentages.
     *
     *  @return A list of percentages for use in the JComboBox.
     */
    private List getTransparencyList() {
        List  l      = new ArrayList();
        int[] values = {
            0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
        };
        for (int i = 0; i < values.length; i++) {
            l.add(new TwoFacedObject(values[i] + "%",
                                     new Integer(values[i])));
        }
        return l;
    }


    /**
     *  Create a List of {@ref ucar.unidata.util.TwoFacedObject}s that contain
     *  the set of predefined brightness percentages.
     *
     *  @return A list of percentages for use in the JComboBox.
     */
    private List getBrightnessList() {
        List    l      = new ArrayList();
        float[] values = {
            1.6f, 1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1.0f, 0.9f, 0.8f, 0.7f, 0.6f,
            0.5f, 0.4f
        };
        for (int i = 0; i < values.length; i++) {
            float  v    = values[i];
            String name = ((int) (v * 100.0f)) + "%";
            l.add(new TwoFacedObject(name, new Float(v)));
        }
        return l;
    }


    /**
     * Set the transparency on the currently selected breakpoint
     */
    public void setTransparency() {
        if (haveBreakpointSelected()) {
            int   index = percentToColorIndex(getPercent(currentBP));
            Color c     = (Color) colorList.get(index);
            colorList.set(index,
                          new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                    getAlphaInt()));
            repaint();
        }
    }

    /**
     * Get the integer value of transparency
     *
     * @return integer value of transparency
     */
    private int getAlphaInt() {
        return (int) (255 * getTransparency());
    }

    /**
     * Get the brightness for the selected object
     *
     * @return the brightness value
     */
    private float getBrightness() {
        TwoFacedObject selected =
            (TwoFacedObject) brightnessBox.getSelectedItem();
        Float f = (Float) selected.getId();
        return f.floatValue();
    }

    /**
     * Get the percent transparency
     *
     * @return percent transparency
     */
    private float getTransparency() {
        Object selected = transBox.getSelectedItem();
        int    value    = 0;
        if (selected instanceof TwoFacedObject) {
            TwoFacedObject tfo = (TwoFacedObject) selected;
            value = ((Integer) tfo.getId()).intValue();
        } else {
            String text = selected.toString();
            try {
                text  = StringUtil.replace(text, "%", "").trim();
                value = (int) new Float(text).floatValue();
            } catch (Exception e) {
                return 1.0f;
            }
        }
        //Inverse because a vlaue of 0 really means 100%
        value = 100 - value;
        //Normalize
        if (value < 0) {
            value = 0;
        }
        if (value > 100) {
            value = 100;
        }
        return ((float) value) / 100.0f;
    }

    /**
     * Is there a shift key pressed
     *
     * @param e Event
     * @return Is shift key pressed
     */
    private boolean isShift(ActionEvent e) {
        return ((e.getModifiers() & e.SHIFT_MASK) != 0);
    }


    /**
     * Make the  panel that shows a plus and minus
     *
     * @param start O left or on right
     * @return The panel
     */
    private JPanel getAddRemoveButton(final boolean start) {
        JButton addColorBtn =
            GuiUtils.getImageButton(GuiUtils.getImageIcon("plus.gif",
                getClass()));
        addColorBtn.setToolTipText("Add color ('+')");
        addColorBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addColor(start, (isShift(event)
                                 ? 10
                                 : 1));
            }
        });

        JButton remColorBtn =
            GuiUtils.getImageButton(GuiUtils.getImageIcon("minus.gif",
                getClass()));
        remColorBtn.setToolTipText("Remove color ('-')");
        remColorBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                removeColor(start, (isShift(event)
                                    ? 10
                                    : 1));
            }
        });

        remColorBtn.setBorder(BorderFactory.createEmptyBorder());
        addColorBtn.setBorder(BorderFactory.createEmptyBorder());
        return GuiUtils.hbox(remColorBtn, addColorBtn);
    }

    /**
     * Set the name of the CT
     *
     * @param name The name
     */
    public void setName(String name) {
        ColorTable newColorTable = new ColorTable(currentColorTable);
        newColorTable.setName(name);
        currentColorTable = newColorTable;
        commands.add(new ColorTableCommand(this, newColorTable));
    }


    /**
     * Set the range from the text fields
     */
    protected void setMinMax() {
        double min;
        double max;
        try {
            min = Misc.parseNumber(minField.getText());
        } catch (NumberFormatException pe) {
            LogUtil.userMessage(log_,
                                "Incorrect value:" + minField.getText());
            return;
        }
        try {
            max = Misc.parseNumber(maxField.getText());
        } catch (NumberFormatException pe) {
            LogUtil.userMessage(log_,
                                "Incorrect value:" + maxField.getText());
            return;
        }
        myRange.setMin(min);
        myRange.setMax(max);
        rangeChanged();
        repaint();
    }


    /**
     * Set the range
     *
     * @param min Range min
     * @param max Range max
     */
    protected void setRange(double min, double max) {
        minField.setText(Misc.format(min));
        maxField.setText(Misc.format(max));
        myRange.setMin(min);
        myRange.setMax(max);
        repaint();
        //        rangeChanged();
    }


    /**
     * Add the listener
     *
     * @param listener The listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList();
        }
        listeners.add(listener);
    }

    /**
     * Do we have listeners
     *
     * @return Do we have listeners
     */
    public boolean isStandAlone() {
        return ((listeners == null) || (listeners.size() == 0));
    }


    /**
     * Apply brightness
     *
     * @param c  the color
     * @param b  the brightness
     *
     * @return  the new color
     */
    private static Color applyBrightness(Color c, float b) {
        if (b != 1.0) {
            c = new Color(Math.min(255, (int) (b * c.getRed())),
                          Math.min(255, (int) (b * c.getGreen())),
                          Math.min(255, (int) (b * c.getBlue())),
                          c.getAlpha());
        }
        return c;
    }


    /**
     * Get the currently edited CT
     *
     * @return The CT
     */
    public ColorTable getCurrentColorTable() {
        if (categoryField != null) {
            String currentCategory = (String) categoryField.getSelectedItem();
            ArrayList actualColors = new ArrayList();
            for (int i = 0; i < colorList.size(); i++) {
                float bright = ((Float) scales.get(i)).floatValue();
                Color c = applyBrightness((Color) colorList.get(i), bright);
                actualColors.add(c);
            }
            if (currentCategory != null) {
                currentCategory = currentCategory.trim();
                return currentColorTable.init(currentColorTable.getName(),
                        currentCategory, actualColors, scales, breakpoints,
                        myRange);
            }
        }
        return currentColorTable;
    }


    /**
     * Send the close event
     */
    public void doClose() {
        propagateEvent(PROP_CLOSE, null);
    }

    /**
     * Send the table and range changed events
     */
    public void doApply() {
        tableChanged(true);
        rangeChanged(true);
    }

    /**
     * Cancel and send the cancel event
     */
    public void doCancel() {
        setColorTableInner(originalColorTable, false);
        propagateEvent(PROP_CANCEL, originalColorTable);
    }

    /**
     * Propagate the table changed event
     */
    public void tableChanged() {
        tableChanged(false);
    }


    /**
     * Propagate the table changed event
     *
     * @param force If true then send the change no matter what
     */
    public void tableChanged(boolean force) {
        if ( !okToPropagateChangesNow) {
            needToPropagateChanges = true;
            return;
        }
        if (force || propagateChanges) {
            propagateEvent(PROP_COLORTABLE, getCurrentColorTable());
        }
    }


    /**
     * Propagate the range changed event
     */
    public void rangeChanged() {
        rangeChanged(false);
    }


    /**
     * Propagate the range changed event
     *
     * @param force If true then send the change no matter what
     */
    private void rangeChanged(boolean force) {
        if (force || propagateChanges) {
            propagateEvent(PROP_RANGE, myRange);
        }
        if (isStandAlone()) {
            currentColorTable.setRange(myRange);
        }
    }



    /**
     * Should we propagate changes
     *
     * @param v The flag
     */
    public void setPropagateChanges(boolean v) {
        propagateChanges = v;
    }

    /**
     * Send the property change event
     *
     * @param event The event
     * @param data The data
     */
    private void propagateEvent(String event, Object data) {
        propagateEvent(new PropertyChangeEvent(this, event, null, data));
    }


    /**
     * Send the property change event
     *
     * @param event The event
     */
    private void propagateEvent(PropertyChangeEvent event) {
        if (listeners == null) {
            return;
        }
        for (int i = 0; i < listeners.size(); i++) {
            ((PropertyChangeListener) listeners.get(i)).propertyChange(event);
        }
    }


    /**
     * Set the brightness on a list of colors
     *
     * @param colors  the colors
     * @param brightnessList  the brightnesses
     */
    private void setColorList(ArrayList colors, ArrayList brightnessList) {
        colorList = new ArrayList(colors);
        scales    = brightnessList;
        if (scales == null) {
            scales = new ArrayList();
            for (int i = 0; i < colorList.size(); i++) {
                scales.add(new Float(1.0f));
            }
        }
        colorsChanged();
    }


    /**
     * The colors have changed. Repaint and update the GUI.
     */
    public void colorsChanged() {
        repaint();
        colorCntLabel.setText(colorList.size() + " colors");
    }

    /**
     * Add cnt number of color slots
     *
     * @param start At start or end
     * @param cnt How many to add
     */
    public void addColor(boolean start, int cnt) {
        prepColorChange();
        while (cnt-- > 0) {
            if (start) {
                colorList.add(0, colorList.get(0));
                scales.add(0, new Float(1.0f));
            } else {
                colorList.add(colorList.get(colorList.size() - 1));
                scales.add(new Float(1.0f));
            }
        }
        colorsChanged();
        tableChanged();
    }

    /**
     * Remove cnt number of color slots
     *
     * @param start At start or end
     * @param cnt How many to remove
     */
    public void removeColor(boolean start, int cnt) {
        while (cnt-- > 0) {
            if (colorList.size() <= 5) {
                break;
            }
            prepColorChange();
            if (start) {
                colorList.remove(0);
                scales.remove(0);
            } else {
                colorList.remove(colorList.size() - 1);
                scales.remove(scales.size() - 1);
            }
            colorsChanged();
            tableChanged();
        }
    }





    /**
     * Catch the key pressed event
     *
     * @param e The event
     */
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();
        boolean moveRight = (code == KeyEvent.VK_KP_RIGHT)
                            || (code == KeyEvent.VK_RIGHT);
        boolean moveLeft = (code == KeyEvent.VK_KP_LEFT)
                           || (code == KeyEvent.VK_LEFT);

        char c = e.getKeyChar();
        if (e.isControlDown()) {
            if (code == KeyEvent.VK_C) {
                if (currentBP != null) {
                    addBreakpointAtData(currentBP);
                }
                lastControlKey = -1;
                return;
            }

            if (code == KeyEvent.VK_L) {
                if (currentBP != null) {
                    saveCurrentBreakpoints();
                    currentBP.setLocked(true);
                    repaint();
                }
                lastControlKey = -1;
                return;
            }

            if (code == KeyEvent.VK_U) {
                if (currentBP != null) {
                    saveCurrentBreakpoints();
                    currentBP.setLocked(false);
                    repaint();
                }
                lastControlKey = -1;
                return;
            }
            if (code == KeyEvent.VK_Z) {
                commands.undo();
                lastControlKey = -1;
                return;
            }
            if (code == KeyEvent.VK_Y) {
                commands.redo();
                lastControlKey = -1;
                return;
            }
            String type = null;
            String dir  = null;
            if (moveRight) {
                dir = "Right";
            } else if (moveLeft) {
                dir = "Left";
            } else if (code == KeyEvent.VK_A) {
                dir = "All";
            } else if (code != KeyEvent.VK_CONTROL) {
                lastControlKey = code;
                return;
            } else {
                return;
            }
            if (lastControlKey == KeyEvent.VK_F) {
                type = "fill";
            } else if (lastControlKey == KeyEvent.VK_I) {
                type = "interpolate";
            } else if (lastControlKey == KeyEvent.VK_T) {
                type = "trans";
            } else if (lastControlKey == KeyEvent.VK_B) {
                type = "bright";
            } else if (code != KeyEvent.VK_CONTROL) {
                lastControlKey = code;
            }
            if ((type != null) && (dir != null)) {
                lastControlKey = -1;
                Misc.run(this, type + dir);
            }
            return;
        }
        lastControlKey = -1;
        if (code == KeyEvent.VK_SPACE) {
            if (breakpoints.size() == 0) {
                return;
            }
            if (currentBP != null) {
                currentBP = getNext(currentBP);
            }
            if (currentBP == null) {
                currentBP = getMin();
            }
            repaint();
        } else if (code == KeyEvent.VK_PLUS) {
            addColor(false, 1);
        } else if (code == KeyEvent.VK_MINUS) {
            removeColor(false, 1);
	} else if(GuiUtils.isDeleteEvent(e)) {
            removeBreakpoint();
        } else if ((currentBP != null) && !currentBP.getLocked()
                   && (moveRight || moveLeft)) {
            int idx = percentToColorIndex(currentBP.getValue());
            if (moveLeft && (idx == 0)) {
                return;
            }
            if (moveRight && (idx == colorList.size() - 1)) {
                return;
            }
            int nextIdx = (moveRight
                           ? idx + 1
                           : idx - 1);
            lastChangeIdx = -1;
            if (currentMode == MODE_TRANSPARENCY) {
                prepColorChange();
                setTransparencyFromTo(nextIdx, nextIdx);
            } else if (currentMode == MODE_FILL) {
                prepColorChange();
                fillFromTo(activeColor, nextIdx, nextIdx);
            } else if (currentMode == MODE_BRIGHTNESS) {
                prepColorChange();
                setBrightnessFromTo(nextIdx, nextIdx);
            } else {
                saveCurrentBreakpoints();
            }
            double perc = nextIdx / (double) (colorList.size() - 1);
            currentBP.setValue(perc);
            repaint();
        }

    }

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {}




    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseMoved(MouseEvent e) {
        checkCursor(e);
    }

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseEntered(MouseEvent e) {
        requestFocus();
        checkCursor(e);
    }

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * Get the paint brush cursor
     *
     * @return paint brush cursor
     */
    public Cursor getPaintCursor() {
        if (paintCursor == null) {
            Image image =
                GuiUtils.getImage("/auxdata/ui/icons/paintbrush.png",
                                  getClass(), true);
            paintCursor =
                Toolkit.getDefaultToolkit().createCustomCursor(image,
                    new Point(0, 0), "paintcursor");
        }
        return paintCursor;
    }



    /**
     * set the cursor
     *
     * @param e event
     */
    public void checkCursor(MouseEvent e) {
        if (cursorOver == reallyInBox(e)) {
            if (cursorPosition != e.getX()) {
                cursorPosition = e.getX();
                repaint();
            }
            return;
        }
        cursorOver     = !cursorOver;
        cursorPosition = e.getX();
        if (cursorOver) {
            setCursor(getPaintCursor());
        } else {
            setCursor(normalCursor);
        }
        repaint();
    }

    /**
     * Is the mouse in the color box
     *
     * @param event event
     * @return is in box
     */
    public boolean isInBox(MouseEvent event) {
        Rectangle box = getColorBox();
	int ex = event.getX();
	int ey = event.getY();
	if(ey<box.y) return false;
	if(ey>box.y+box.height) return false;
	return true;
	//        return box.contains(new Point(event.getX(), event.getY()));
    }

    public boolean reallyInBox(MouseEvent event) {
        Rectangle box = getColorBox();
	return box.contains(new Point(event.getX(), event.getY()));
    }


    /**
     * Mouse was pressed
     *
     * @param event The event
     */
    public void mousePressed(MouseEvent event) {
        if ( !SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        mouseInBox = isInBox(event);

        if ( !mouseInBox) {
            if ( !selectBreakpoint(event)) {
                return;
            }
        }

        switch (currentMode) {

          case MODE_NONE :
              saveCurrentBreakpoints();
              break;


          default :
              activePercent = xToPercent(event.getX());
              prepColorChange();
              if (mouseInBox) {
                  activeColor = colorChooser.getColor();
              } else {
                  activeColor = getBreakpointColor(currentBP);
              }
              activeColorIndex =
                  percentToColorIndex(xToPercent(event.getX()));
              if (currentMode == MODE_INTERPOLATE) {
                  activeColor = (Color) colorList.get(activeColorIndex);
              }


              priorColors = (ArrayList) colorList.clone();
              if (mouseInBox && (activeColorIndex >= 0)) {
                  if (currentMode == MODE_BRIGHTNESS) {
                      setBrightnessFromTo(activeColorIndex, activeColorIndex);
                  } else if ((currentMode == MODE_FILL)
                             && (selectedColor != null)) {
                      colorList.set(activeColorIndex, selectedColor);
                  } else if (currentMode == MODE_TRANSPARENCY) {
                      setTransparencyFromTo(activeColorIndex,
                                            activeColorIndex);

                  }
                  tableChanged();
                  repaint();
              }

        }
    }


    /**
     * The mouse was dragged
     *
     * @param event The event
     */
    public void mouseDragged(MouseEvent event) {
        checkCursor(event);

        int x = 0;
        if ( !mouseInBox) {
            if ( !haveBreakpointSelected()) {
                return;
            }
            if ((currentBP != null) && currentBP.getLocked()) {
                return;
            }
            currentBP.setValue(xToPercent(event.getX()));
            x = getBreakpointX(currentBP);
            repaint();
            return;
        } else {
            x = event.getX();
        }

        if (currentMode != MODE_NONE) {
            colorList = (ArrayList) priorColors.clone();
            int   startColorIndex = activeColorIndex;
            Color startColor      = activeColor;
            int   endColorIndex   = percentToColorIndex(xToPercent(x));
            Color endColor        = (Color) priorColors.get(endColorIndex);

            if (endColorIndex <= startColorIndex) {
                int tmp = endColorIndex;
                endColorIndex   = startColorIndex;
                startColorIndex = tmp;
                Color tmpc = endColor;
                endColor   = startColor;
                startColor = tmpc;
            }

            //Don't propagate the changes when we are dragging
            okToPropagateChangesNow = false;
            if (currentMode == MODE_INTERPOLATE) {
                interpolate(startColorIndex, endColorIndex, startColor,
                            endColor);
            } else if (currentMode == MODE_FILL) {
                fillFromTo(activeColor, startColorIndex, endColorIndex);
            } else if (currentMode == MODE_BRIGHTNESS) {
                boolean ok = true;
                if (startColorIndex == endColorIndex) {
                    if (Math.abs(
                            percentToX(activePercent)
                            - percentToX(xToPercent(event.getX()))) < 3) {
                        ok = false;
                    }
                }

                if (ok) {
                    setBrightnessFromTo(startColorIndex, endColorIndex);
                }

            } else if (currentMode == MODE_TRANSPARENCY) {
                boolean ok = true;
                if (startColorIndex == endColorIndex) {
                    if (Math.abs(
                            percentToX(activePercent)
                            - percentToX(xToPercent(event.getX()))) < 3) {
                        ok = false;
                    }
                }

                if (ok) {
                    setTransparencyFromTo(startColorIndex, endColorIndex);
                }
            }
            okToPropagateChangesNow = true;
        }
        repaint();
    }


    /**
     * Catch the event
     *
     * @param event The event
     */
    public void mouseReleased(MouseEvent event) {
        if ( !SwingUtilities.isLeftMouseButton(event)) {
            return;
        }
        currentBP = null;
        if (needToPropagateChanges) {
            needToPropagateChanges = false;
            tableChanged();
        }
        selectBreakpoint(event);
    }


    /**
     * Catch the event
     *
     * @param event The event
     */
    public void mouseClicked(MouseEvent event) {



        final double xPercent = xToPercent(event.getX());

        if ( !SwingUtilities.isRightMouseButton(event)) {
            //Shift click adds a new break point
            if (event.getClickCount() > 1) {
                insertBreakpoint(xPercent);
            }
            return;
        }
        if ( !selectBreakpoint(event)) {
            currentBP = null;
            repaint();
        }
        JPopupMenu popup = new JPopupMenu();
        JMenuItem  mi;

        if (currentBP != null) {
            popup.add(mi = GuiUtils.makeMenuItem("Remove Breakpoint", this,
                    "removeBreakpoint"));
            mi.setToolTipText("Delete");
            popup.add(mi = GuiUtils.makeMenuItem(currentBP.getLocked()
                    ? "Unlock Breakpoint"
                    : "Lock Breakpoint", this, "lockUnlockBreakpoint",
                                         currentBP));
            mi.setToolTipText("Control-l;Control-u");
            popup.add(mi = GuiUtils.makeMenuItem("Change Breakpoint", this,
                    "addBreakpointAtData", currentBP));
            mi.setToolTipText("Control-c");
            popup.addSeparator();
        }


        JMenu addMenu = new JMenu("Add Breakpoint");
        popup.add(addMenu);

        mi = new JMenuItem("Here");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                insertBreakpoint(xPercent);
            }
        });
        addMenu.add(mi);

        addMenu.add(GuiUtils.makeMenuItem("At Data Point", this,
                                          "addBreakpointAtData"));
        addMenu.add(GuiUtils.makeMenuItem("At Beginning", this,
                                          "addBreakpointBeginning"));
        addMenu.add(GuiUtils.makeMenuItem("At End", this,
                                          "addBreakpointEnd"));


        JMenu colorsMenu = new JMenu("Edit Colors");
        popup.add(colorsMenu);

        JMenu interpMenu = new JMenu("Interpolate");
        colorsMenu.add(interpMenu);

        interpMenu.add(mi = GuiUtils.makeMenuItem("All", this,
                "interpolateAll"));
        mi.setToolTipText("Control-i;Control-a");
        if (currentBP != null) {
            interpMenu.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "interpolateLeft"));
            mi.setToolTipText("Control-i;Control-left arrow");
            interpMenu.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "interpolateRight"));
            mi.setToolTipText("Control-i;Control-right arrow");
        }


        JMenu fillMenu = new JMenu("Fill");
        colorsMenu.add(fillMenu);
        fillMenu.add(mi = GuiUtils.makeMenuItem("All", this, "fillAll"));
        mi.setToolTipText("Control-f;Control-a");
        if (currentBP != null) {
            fillMenu.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "fillLeft"));
            mi.setToolTipText("Control-f;Control-left arrow");
            fillMenu.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "fillRight"));
            mi.setToolTipText("Control-f;Control-right arrow");
        }

        String transLbl  = transBox.getSelectedItem().toString();
        JMenu  transMenu = new JMenu("Transparency (" + transLbl + ")");
        colorsMenu.add(transMenu);

        transMenu.add(mi = GuiUtils.makeMenuItem("All", this, "transAll"));
        mi.setToolTipText("Control-t;Control-a");

        if (currentBP != null) {
            transMenu.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "transLeft"));
            mi.setToolTipText("Control-t;Control-left arrow");
            transMenu.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "transRight"));
            mi.setToolTipText("Control-t;Control-right arrow");

            transMenu.add(mi = GuiUtils.makeMenuItem("Set", this,
                    "setTransparency"));

            JMenu interpTrans = new JMenu("Interpolate");
            transMenu.add(interpTrans);
            interpTrans.add(mi = GuiUtils.makeMenuItem("All", this,
                    "interpolateTransAll"));
            interpTrans.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "interpolateTransLeft"));
            interpTrans.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "interpolateTransRight"));
        }

        String brightLbl = brightnessBox.getSelectedItem().toString();


        JMenu  scaleMenu = new JMenu("Brightness (" + brightLbl + ")");
        colorsMenu.add(scaleMenu);
        scaleMenu.add(mi = GuiUtils.makeMenuItem("All", this, "brightAll"));
        mi.setToolTipText("Control-b;Control-a");
        if (currentBP != null) {
            scaleMenu.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "brightLeft"));
            mi.setToolTipText("Control-b;Control-left arrow");
            scaleMenu.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "brightRight"));
            mi.setToolTipText("Control-b;Control-right arrow");

            JMenu interpBrightness = new JMenu("Interpolate");
            scaleMenu.add(interpBrightness);
            interpBrightness.add(mi = GuiUtils.makeMenuItem("All", this,
                    "interpolateBrightnessAll"));
            interpBrightness.add(mi = GuiUtils.makeMenuItem("Left", this,
                    "interpolateBrightnessLeft"));
            interpBrightness.add(mi = GuiUtils.makeMenuItem("Right", this,
                    "interpolateBrightnessRight"));

        }

        popup.add(GuiUtils.makeMenuItem("Replicate Color Table", this,
                                        "replicate"));

        popup.add(GuiUtils.makeMenuItem("Invert Color Table", this,
                                        "invert"));

        popup.show(this, event.getX(), event.getY());

    }



    /**
     *  Find the closest breakpoint to the given mouse position and select it.
     *
     *
     * @param e
     *  @return true if we found one, false if not.
     */
    private boolean selectBreakpoint(MouseEvent e) {
        int                   minDistance   = Integer.MAX_VALUE;
        ColorTable.Breakpoint minBreakpoint = null;
        for (int i = 0; i < breakpoints.size(); i++) {
            ColorTable.Breakpoint bp =
                (ColorTable.Breakpoint) breakpoints.get(i);
            int distance = Math.abs(e.getX() - getBreakpointX(bp));
            if ((distance < minDistance) && (distance < 10)) {
                minDistance   = distance;
                minBreakpoint = bp;
            }
        }
        repaint();
        currentBP = minBreakpoint;
        return (minBreakpoint != null);
    }

    /**
     * Add a new breakpoint, prompting the user at what data value it shoudl be placed
     */
    public void addBreakpointAtData() {
        addBreakpointAtData(null);
    }

    /**
     * Add a new breakpoint the given index. If index<0
     * then prompt the user at what data value it shoudl be placed
     *
     * @param bp  the sample breakpoint
     */
    public void addBreakpointAtData(ColorTable.Breakpoint bp) {
        String initString = "";
        if (bp != null) {
            initString = Misc.format(percentToValue(getPercent(bp)));
        } else {
            breakpoints.add(bp = new ColorTable.Breakpoint());
        }
        while (true) {
            String inputValue =
                GuiUtils.getInput("Please enter a data value between "
                                  + myRange.formatMin() + " and "
                                  + myRange.formatMax(), "Value: ",
                                      initString, null, null,
                                      "Breakpoint Value", 5);
            if (inputValue == null) {
                return;
            }
            try {
                double d = Misc.parseNumber(inputValue);
                double percent = Math.max(Math.min(myRange.getPercent(d),
                                     1.0), 0.0);
                saveCurrentBreakpoints();
                bp.setValue(percent);
                currentBP = bp;
                repaint();
                return;
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null,
                        "Incorrect number format:" + inputValue, "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Insert a new BP at percent along the line
     *
     * @param percent Where to insert
     */
    public void insertBreakpoint(double percent) {
        saveCurrentBreakpoints();
        breakpoints.add(currentBP = new ColorTable.Breakpoint(percent));
        repaint();
    }

    /**
     * Get the next breakpoint from the input
     *
     * @param bp  the existing breakpoint
     *
     * @return  the next one
     */
    private ColorTable.Breakpoint getNext(ColorTable.Breakpoint bp) {
        if (bp == null) {
            return null;
        }
        ColorTable.Breakpoint next    = null;
        double                minDiff = 0;
        for (int i = 0; i < breakpoints.size(); i++) {
            ColorTable.Breakpoint tmp =
                (ColorTable.Breakpoint) breakpoints.get(i);
            if (tmp == bp) {
                continue;
            }
            if (tmp.getValue() <= bp.getValue()) {
                continue;
            }
            if ((next == null)
                    || (tmp.getValue() - bp.getValue() < minDiff)) {
                minDiff = tmp.getValue() - bp.getValue();
                next    = tmp;
            }
        }
        return next;
    }


    /**
     * get minimum breakpoint
     *
     *
     * @return min breakpoint
     */
    private ColorTable.Breakpoint getMin() {
        ColorTable.Breakpoint min      = null;
        double                minValue = 0;
        for (int i = 0; i < breakpoints.size(); i++) {
            ColorTable.Breakpoint tmp =
                (ColorTable.Breakpoint) breakpoints.get(i);
            if ((min == null) || (tmp.getValue() < minValue)) {
                min      = tmp;
                minValue = tmp.getValue();
            }
        }
        return min;
    }

    /**
     * Get the previous  breakpoint
     *
     * @param bp  the reference breakpoint
     *
     * @return  the previous one
     */
    private ColorTable.Breakpoint getPrevious(ColorTable.Breakpoint bp) {
        if (bp == null) {
            return null;
        }
        ColorTable.Breakpoint prev    = null;
        double                minDiff = 0;
        for (int i = 0; i < breakpoints.size(); i++) {
            ColorTable.Breakpoint tmp =
                (ColorTable.Breakpoint) breakpoints.get(i);
            if (tmp == bp) {
                continue;
            }
            if (tmp.getValue() >= bp.getValue()) {
                continue;
            }
            if ((prev == null)
                    || (bp.getValue() - tmp.getValue() < minDiff)) {
                minDiff = bp.getValue() - tmp.getValue();
                prev    = tmp;
            }
        }
        return prev;
    }


    /**
     * Is there a selected BP
     *
     * @return Is there a selected BP
     */
    private boolean haveBreakpointSelected() {
        return currentBP != null;
    }



    /**
     * Save the list of BPs to the command list
     */
    private void saveCurrentBreakpoints() {
        lastChangeIdx = -1;
        commands.add(new CanvasCommand(this));
    }


    /**
     * Get ready to change the color
     */
    private void prepColorChange() {
        prepColorChange(-1);
    }

    /**
     * Get ready to change the color
     *
     * @param idx The color index
     */
    private void prepColorChange(int idx) {
        if ((idx == -1) || (idx != lastChangeIdx)) {
            commands.add(new ColorListCommand(this));
            lastChangeIdx = idx;
        }
    }



    /**
     * Debug println
     */
    public void debug() {
        System.err.println("" + commands);
    }

    /**
     * Remove the currently selected BP
     */
    public void removeBreakpoint() {
        if (currentBP != null) {
            saveCurrentBreakpoints();
            breakpoints.remove(currentBP);
            currentBP = null;
            repaint();
        }
    }

    /**
     * change the lock status on the BP
     *
     * @param bp  the breakpoint to lock/unlock
     */
    public void lockUnlockBreakpoint(ColorTable.Breakpoint bp) {
        saveCurrentBreakpoints();
        bp.setLocked( !bp.getLocked());
        repaint();
    }


    /**
     * Add BP at the start
     */
    public void addBreakpointBeginning() {
        saveCurrentBreakpoints();
        currentBP = addToBreakpoints(0.0);
        repaint();
    }


    /**
     * Add BP at the end
     */
    public void addBreakpointEnd() {
        saveCurrentBreakpoints();
        currentBP = addToBreakpoints(1.0);
        repaint();
    }



    /**
     * Add a new breakpoint at the value to the list of breakpoints
     *
     * @param d   the value
     *
     * @return  the breakpoint
     */
    private ColorTable.Breakpoint addToBreakpoints(double d) {
        ColorTable.Breakpoint bp = new ColorTable.Breakpoint(d);
        breakpoints.add(bp);
        currentBP = bp;
        return bp;
    }







    /**
     * Set the color at the current BP
     *
     * @param c The color
     */
    public void setColor(Color c) {

        if (currentBP == null) {
            return;
        }

        //        if(true) return;
        double percent = currentBP.getValue();
        int    index   = percentToColorIndex(percent);
        Color  current = (Color) colorList.get(index);
        if (current == c) {
            return;
        }
        prepColorChange(index);
        colorList.set(index,
                      new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                current.getAlpha()));

        repaint();
    }



    /**
     * Get the label to show at the given BP. This shows the value.
     *
     * @param bp What BP
     * @return The label
     */
    private String getBreakpointLabel(ColorTable.Breakpoint bp) {
        return Misc.format(percentToValue(bp.getValue()));
    }



    /**
     * Get the x location of the given BP
     *
     * @param bp The BP
     * @return Its X location
     */
    private int getBreakpointX(ColorTable.Breakpoint bp) {
        return percentToX(getPercent(bp));
    }

    /**
     * Get the percentage of the range from the breakpoint value
     *
     * @param bp  the breakpoint
     *
     * @return  the percentage along the range
     */
    private double getPercent(ColorTable.Breakpoint bp) {
        return getPercent(bp, 0.0);
    }


    /**
     * Get the percentage of the range from the breakpoint value
     *
     * @param bp  the breakpoint
     * @param dflt  default value
     *
     * @return  the percentage along the range or default
     */
    private double getPercent(ColorTable.Breakpoint bp, double dflt) {
        if (bp == null) {
            return dflt;
        }
        return bp.getValue();
    }

    /**
     * Get the color at the given BP
     *
     *
     * @param bp The BP
     * @return The color
     */
    private Color getBreakpointColor(ColorTable.Breakpoint bp) {
        if (bp == null) {
            return null;
        }
        return (percentToColor(bp.getValue()));
    }


    /**
     * What is the width of the color box
     *
     * @return Color box width
     */
    public int boxWidth() {
        return getColorBox().width;
    }

    /**
     * Map x location to percent along color box
     *
     * @param x X location
     * @return Percent along color box
     */
    public double xToPercent(int x) {
        return Math.max(Math.min(1.0,
                                 ((x - MARGIN_H)
                                  / (double) boxWidth())), 0.0);
    }

    /**
     * Map percent along color box to x location
     *
     * @param percent Percent along color box
     * @return X location
     */
    public int percentToX(double percent) {
        return MARGIN_H + (int) (percent * boxWidth());
    }



    /**
     * Get the value of the point percent along the color box
     *
     * @param percent Percent along the color box
     * @return Value
     */
    public double percentToValue(double percent) {
        return myRange.getValueOfPercent(percent);
    }


    /**
     * Interpolate color from end to end
     */
    public void interpolateAll() {
        prepColorChange();
        interpolate(0, colorList.size() - 1, (Color) colorList.get(0),
                    (Color) colorList.get(colorList.size() - 1));
    }


    /**
     * Interpolate color left
     */
    public void interpolateLeft() {
        interpolate(getPrevious(currentBP), currentBP);
    }

    /**
     * Interpolate color right
     */
    public void interpolateRight() {
        interpolate(currentBP, getNext(currentBP));
    }


    /**
     * Interpolate transparency from end to end
     */
    public void interpolateTransAll() {
        prepColorChange();
        interpolateTrans(0, colorList.size() - 1, (Color) colorList.get(0),
                         (Color) colorList.get(colorList.size() - 1));
    }


    /**
     * Interpolate transparency left
     */
    public void interpolateTransLeft() {
        interpolateTrans(getPrevious(currentBP), currentBP);
    }

    /**
     * Interpolate transparency right
     */
    public void interpolateTransRight() {
        interpolateTrans(currentBP, getNext(currentBP));
    }



    /**
     * Interpolate transparency from end to end
     */
    public void interpolateBrightnessAll() {
        prepColorChange();
        interpolateBrightness(0, colorList.size() - 1,
                              (Color) colorList.get(0),
                              (Color) colorList.get(colorList.size() - 1));
    }


    /**
     * Interpolate transparency left
     */
    public void interpolateBrightnessLeft() {
        interpolateBrightness(getPrevious(currentBP), currentBP);
    }

    /**
     * Interpolate transparency right
     */
    public void interpolateBrightnessRight() {
        interpolateBrightness(currentBP, getNext(currentBP));
    }



    /**
     * replicate
     */
    public void replicate() {
        String inputValue = "1";
        while (true) {
            inputValue = GuiUtils.getInput(
                "How many times do you want to replicate the color table?",
                "Value: ", inputValue, null, null, "Replicate Color Table",
                5);

            if (inputValue == null) {
                return;
            }
            try {
                int count = new Integer(inputValue.trim()).intValue();
                prepColorChange();
                if (count > 100) {
                    count = 100;
                }
                List tmp = new ArrayList(colorList);
                for (int i = 0; i < count; i++) {
                    for (int j = 0; j < tmp.size(); j++) {
                        colorList.add(tmp.get(j));
                        scales.add(new Float(1.0f));
                    }
                }
                colorsChanged();
                repaint();
                return;
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad input value:" + inputValue);
            }
        }


    }



    /**
     * invert the color table
     */
    public void invert() {
        prepColorChange();
        List tmpColors = new ArrayList(colorList);
        List tmpScales = new ArrayList(scales);
        colorList = new ArrayList();
        scales    = new ArrayList();
        for (int i = 0; i < tmpColors.size(); i++) {
            colorList.add(0, tmpColors.get(i));
        }

        for (int i = 0; i < tmpScales.size(); i++) {
            scales.add(0, tmpScales.get(i));
        }
        colorsChanged();
        repaint();
    }


    /**
     * Do a linear interpolation of the colors in the range defined
     * by the given breakpoints.
     *
     *
     * @param from starting breakpoint
     * @param to ending breakpoint
     */
    public void interpolate(ColorTable.Breakpoint from,
                            ColorTable.Breakpoint to) {
        int lowerColorIndex = percentToColorIndex(((from != null)
                ? from.getValue()
                : 0.0));
        int upperColorIndex = percentToColorIndex(((to != null)
                ? to.getValue()
                : 1.0));

        prepColorChange();
        interpolate(lowerColorIndex, upperColorIndex,
                    (Color) colorList.get(lowerColorIndex),
                    (Color) colorList.get(upperColorIndex));
    }

    /**
     * Do a linear interpolation between the given colors
     *
     * @param lowerColorIndex Where to start
     * @param upperColorIndex Where to end
     * @param lowerColor The lower color
     * @param upperColor The upper color
     */
     public void interpolate(int lowerColorIndex, int upperColorIndex,
			Color lowerColor, Color upperColor) {
        int colorSpace =
            ((Integer) ((TwoFacedObject) colorSpaceCbx.getSelectedItem())
                .getId()).intValue();
	if(colorSpace  ==DFLT_COLOR_SPACE) {
	    oldInterpolate(lowerColorIndex, upperColorIndex, lowerColor, upperColor);
	} else {
	    newInterpolate(colorSpace, lowerColorIndex, upperColorIndex, lowerColor, upperColor);
	}
        tableChanged();
        repaint();
    }


    public void newInterpolate(int colorSpace, int lowerColorIndex, int upperColorIndex,
			   Color lowerColor, Color upperColor) {

        float      steps = (float) (upperColorIndex - lowerColorIndex + 1);
	ColorSpace rgb   = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
	rgb =  lowerColor.getColorSpace();

        ColorSpace other = ColorSpace.getInstance(colorSpace);

        float[] comps1 = rgb.toCIEXYZ(lowerColor.getColorComponents(rgb,
                             null));
        float[] comps2 = rgb.toCIEXYZ(upperColor.getColorComponents(rgb,
                             null));
        comps1 = other.fromCIEXYZ(comps1);
        comps2 = other.fromCIEXYZ(comps2);

        float[] stepArray = new float[comps1.length];
        for (int j = 0; j < comps1.length; j++) {
            stepArray[j] = (comps2[j] - comps1[j]) / steps;
        }
        int     cnt = 0;
        float[] tmp = new float[comps1.length];
        for (int i = lowerColorIndex; i <= upperColorIndex; i++, cnt++) {
            try {
                Color current = (Color) colorList.get(i);
                for (int j = 0; j < comps1.length; j++) {
                    tmp[j] = comps1[j] + stepArray[j] * i;
		    if(tmp[j]<0.0001f) tmp[j] = 0f;
		    else if(tmp[j]>1.0f) tmp[j] = 1.0f;
                }
                Color c = new Color(other, tmp,
                                    ((float) current.getAlpha()) / 255.0f);
                colorList.set(i, c);
            } catch (IllegalArgumentException iae) {
		/*
		System.err.print("bad  tmp:");
                for (int j = 0; j < comps1.length; j++) {
                    System.err.print(tmp[j] + " ");
                }

		System.err.print(" steps:");
                for (int j = 0; j < comps1.length; j++) {
                    System.err.print(stepArray[j] + " ");
                }
                System.err.println("");
		*/
            }
        }
    }


    public void oldInterpolate(int lowerColorIndex, int upperColorIndex,
                            Color lowerColor, Color upperColor) {
        float      steps = (float) (upperColorIndex - lowerColorIndex + 1);
        double redStep = (upperColor.getRed() - lowerColor.getRed()) / steps;
        double greenStep = (upperColor.getGreen() - lowerColor.getGreen())
                           / steps;
        double blueStep = (upperColor.getBlue() - lowerColor.getBlue())
                          / steps;
        int     cnt = 0;

        for (int i = lowerColorIndex; i <= upperColorIndex; i++, cnt++) {
	    Color current = (Color) colorList.get(i);
	    int   newRed   = lowerColor.getRed() + (int) (redStep * cnt);
	    int   newGreen = lowerColor.getGreen() + (int) (greenStep * cnt);
	    int   newBlue  = lowerColor.getBlue() + (int) (blueStep * cnt);
	    colorList.set(i, new Color(newRed, newGreen, newBlue,
				       current.getAlpha()));
        }
    }


    /**
     * Do a linear interpolation of the colors in the range defined
     * by the given breakpoints.
     *
     *
     * @param from starting breakpoint
     * @param to ending breakpoint
     */
    public void interpolateTrans(ColorTable.Breakpoint from,
                                 ColorTable.Breakpoint to) {
        int lowerColorIndex = percentToColorIndex(((from != null)
                ? from.getValue()
                : 0.0));
        int upperColorIndex = percentToColorIndex(((to != null)
                ? to.getValue()
                : 1.0));

        prepColorChange();
        interpolateTrans(lowerColorIndex, upperColorIndex,
                         (Color) colorList.get(lowerColorIndex),
                         (Color) colorList.get(upperColorIndex));
    }


    /**
     * Do a linear interpolation of transparency between the given colors
     *
     * @param lowerColorIndex Where to start
     * @param upperColorIndex Where to end
     * @param lowerColor The lower color
     * @param upperColor The upper color
     */
    public void interpolateTrans(int lowerColorIndex, int upperColorIndex,
                                 Color lowerColor, Color upperColor) {
        double steps = (double) (upperColorIndex - lowerColorIndex + 1);
        double alphaStep = (upperColor.getAlpha() - lowerColor.getAlpha())
                           / steps;
        int cnt = 0;
        for (int i = lowerColorIndex; i <= upperColorIndex; i++, cnt++) {
            int   newAlpha = lowerColor.getAlpha() + (int) (alphaStep * cnt);
            Color current  = (Color) colorList.get(i);
            colorList.set(i, new Color(current.getRed(), current.getGreen(),
                                       current.getBlue(), newAlpha));
        }
        tableChanged();
        repaint();
    }



    /**
     * Do a linear interpolation of the colors in the range defined
     * by the given breakpoints.
     *
     *
     * @param from starting breakpoint
     * @param to ending breakpoint
     */
    public void interpolateBrightness(ColorTable.Breakpoint from,
                                      ColorTable.Breakpoint to) {
        int lowerColorIndex = percentToColorIndex(((from != null)
                ? from.getValue()
                : 0.0));
        int upperColorIndex = percentToColorIndex(((to != null)
                ? to.getValue()
                : 1.0));

        prepColorChange();
        interpolateBrightness(lowerColorIndex, upperColorIndex,
                              (Color) colorList.get(lowerColorIndex),
                              (Color) colorList.get(upperColorIndex));
    }


    /**
     * Do a linear interpolation of transparency between the given colors
     *
     * @param lowerColorIndex Where to start
     * @param upperColorIndex Where to end
     * @param lowerColor The lower color
     * @param upperColor The upper color
     */
    public void interpolateBrightness(int lowerColorIndex,
                                      int upperColorIndex, Color lowerColor,
                                      Color upperColor) {
        float steps          = (float) (upperColorIndex - lowerColorIndex
                                        + 1);


        float f1 = ((Float) scales.get(lowerColorIndex)).floatValue();
        float f2 = ((Float) scales.get(upperColorIndex)).floatValue();

        float brightnessStep = (f2 - f1) / steps;
        int   cnt            = 0;
        for (int i = lowerColorIndex; i <= upperColorIndex; i++, cnt++) {
            float newBrightness = f1 + (brightnessStep * cnt);
            scales.set(i, new Float(newBrightness));
        }
        tableChanged();
        repaint();
    }



    /**
     * Fill to the left of the selected BP
     */
    public void fillLeft() {
        fill(percentToColor(getPercent(currentBP)), getPrevious(currentBP),
             currentBP);
    }

    /**
     * Fill to the right of the selected BP
     */
    public void fillRight() {
        fill(percentToColor(getPercent(currentBP)), currentBP,
             getNext(currentBP));
    }


    /**
     * Fill with the given color between the 2 BPs.
     *
     * @param color The fill color
     * @param from The lower end
     * @param to The upper end
     */
    public void fill(Color color, ColorTable.Breakpoint from,
                     ColorTable.Breakpoint to) {
        int lowerIndex = percentToColorIndex(((from != null)
                ? from.getValue()
                : 0.0));
        int upperIndex = percentToColorIndex(((to != null)
                ? to.getValue()
                : 1.0));
        prepColorChange();
        fillFromTo(color, lowerIndex, upperIndex);
    }



    /**
     * Fill with the given color between the 2 BPs.
     *
     */
    public void fillAll() {
        prepColorChange();
        fillFromTo((Color) colorList.get(0), 0, colorList.size() - 1);
    }


    /**
     * Fill with the given color between the 2 indices.
     *
     * @param color The fill color
     * @param lowerIndex The lower index
     * @param upperIndex The upper index
     */
    public void fillFromTo(Color color, int lowerIndex, int upperIndex) {
        for (int i = lowerIndex; i <= upperIndex; i++) {
            Color current = (Color) colorList.get(i);
            colorList.set(i, new Color(color.getRed(), color.getGreen(),
                                       color.getBlue(), current.getAlpha()));
        }
        tableChanged();
        repaint();
    }



    /**
     * Set the transparency to the left of the current BP
     */
    public void transLeft() {
        setTransparency(getPrevious(currentBP), currentBP);
    }

    /**
     * Set the transparency to the right of the current BP
     */
    public void transRight() {
        setTransparency(currentBP, getNext(currentBP));
    }

    /**
     * Set the transparency between the 2 BPs
     *
     */
    public void transAll() {
        prepColorChange();
        setTransparencyFromTo(0, colorList.size() - 1);
    }


    /**
     * Set the transparency between the 2 BPs
     *
     * @param from The lower BP
     * @param to The upper BP
     */
    public void setTransparency(ColorTable.Breakpoint from,
                                ColorTable.Breakpoint to) {
        int lowerIndex = percentToColorIndex((from != null)
                                             ? from.getValue()
                                             : 0.0);
        int upperIndex = percentToColorIndex((to != null)
                                             ? to.getValue()
                                             : 1.0);
        prepColorChange();
        setTransparencyFromTo(lowerIndex, upperIndex);
    }


    /**
     * Set the transparency between the 2 indices
     *
     * @param lowerIndex The lower index
     * @param upperIndex The upper index
     */
    public void setTransparencyFromTo(int lowerIndex, int upperIndex) {
        int alpha = getAlphaInt();
        for (int i = lowerIndex; i <= upperIndex; i++) {
            Color c = (Color) colorList.get(i);
            colorList.set(i, new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                       alpha));
        }
        tableChanged();
        repaint();
    }



    /**
     * Set the transparency to the left of the current BP
     */
    public void brightLeft() {
        setBrightness(getPrevious(currentBP), currentBP);
    }

    /**
     * Set the transparency to the right of the current BP
     */
    public void brightRight() {
        setBrightness(currentBP, getNext(currentBP));
    }


    /**
     * Set the transparency between the 2 BPs
     *
     * @param from The lower BP
     * @param to The upper BP
     */
    public void setBrightness(ColorTable.Breakpoint from,
                              ColorTable.Breakpoint to) {
        int lowerIndex = percentToColorIndex((from != null)
                                             ? from.getValue()
                                             : 0.0);
        int upperIndex = percentToColorIndex((to != null)
                                             ? to.getValue()
                                             : 1.0);
        prepColorChange();
        setBrightnessFromTo(lowerIndex, upperIndex);
    }

    /**
     * Set the transparency between the 2 BPs
     *
     */
    public void brightAll() {
        prepColorChange();
        setBrightnessFromTo(0, colorList.size() - 1);
    }


    /**
     * Set the transparency between the 2 indices
     *
     * @param lowerIndex The lower index
     * @param upperIndex The upper index
     */
    public void setBrightnessFromTo(int lowerIndex, int upperIndex) {
        float b = getBrightness();
        for (int i = lowerIndex; i <= upperIndex; i++) {
            scales.set(i, new Float(b));
        }
        tableChanged();
        repaint();
    }



    /**
     * Get the bounding box of the color box
     *
     * @return The color box
     */
    public Rectangle getColorBox() {
        return getColorBox(getBounds());
    }

    /**
     * Get the bounding box of the color box
     *
     * @param bounds The bounds around the color box
     * @return The color box
     */
    public Rectangle getColorBox(Rectangle bounds) {
        return new Rectangle(MARGIN_H, MARGIN_V,
                             bounds.width - (MARGIN_H * 2),
                             bounds.height - (MARGIN_V * 2));
    }

    /**
     * Map percent along the color box to the index in the color list
     *
     * @param percent Percent along the color box
     * @return Color index
     */
    public int percentToColorIndex(double percent) {
        double index = percent * colorList.size();
        return Math.min(Math.max(0, (int) index), colorList.size() - 1);
    }

    /**
     * Map the percent along the color box to the color
     *
     * @param percent Along color box
     * @return The color
     */
    public Color percentToColor(double percent) {
        return (Color) colorList.get(percentToColorIndex(percent));
    }

    /**
     * Paint the color box
     *
     * @param g The graphics
     */
    public void paintColors(Graphics g) {
        Rectangle box = getColorBox();
        paintColors(g, box, colorList, true, true, (List<Float>) scales);
    }




    /**
     * Paint the color box
     *
     * @param g The graphics
     * @param box _more_
     * @param colorList _more_
     * @param doLines _more_
     * @param doCheckerboard _more_
     * @param scales _more_
     */
    public static void paintColors(Graphics g, Rectangle box,
                                   List<Color> colorList, boolean doLines,
                                   boolean doCheckerboard,
                                   List<Float> scales) {
	paintColors(g, box, colorList, doLines, doCheckerboard,false, scales);
    }


    public static void paintColors(Graphics g, Rectangle box,
                                   List<Color> colorList, boolean doLines,
                                   boolean doCheckerboard,
				   boolean doTransparency,
                                   List<Float> scales) {

        //Draw a reference rectangle for transparency
	if(!doTransparency) {
	    g.setColor(Color.white);
	    g.fillRect(box.x, box.y, box.width, box.height);
	}

        if (doCheckerboard) {
            int patternX = box.x;
            g.setColor(Color.black);
            boolean top           = true;
            int     patternHeight = box.height / 2;
            if (patternHeight > 0) {
                while (patternX < box.x + box.width) {
                    int patternWidth = patternHeight;
                    if ((patternX + patternWidth) > (box.x + box.width)) {
                        patternWidth = (box.x + box.width) - patternX;
                    }
                    g.fillRect(patternX, (top
                                          ? box.y
                                          : box.y
                                            + patternHeight), patternWidth,
                                                patternHeight);
                    patternX += patternHeight;
                    top      = !top;
                }
            }
        }

        int length = colorList.size();
        if (length > 50) {
            doLines = false;
        }
        double dWidth         = (((double) box.width) / length);
        int    width          = (int) dWidth;
        double extraPerBox    = dWidth - width;
        int    x              = box.x;
        double remainderWidth = 0.0;
        for (int i = 0; i < length; i++) {
            Color c = (Color) colorList.get(i);
            if (scales != null) {
                float bright = ((Float) scales.get(i)).floatValue();
                c = applyBrightness(c, bright);
            }
            //Clear out the alpha
            if ( !doCheckerboard && !doTransparency) {
                c = new Color(c.getRed(), c.getGreen(), c.getBlue());
            }

            g.setColor(c);
            int extraWidth = 0;
            remainderWidth += extraPerBox;
            if (remainderWidth > 1.0) {
                extraWidth     = (int) remainderWidth;
                remainderWidth -= extraWidth;
            }
            if (i == length - 1) {
                extraWidth = box.width - (x - box.x) - width + 1;
            }

            g.fillRect(x, box.y, width + extraWidth, box.height);
            if (doLines) {
                g.setColor(g.getColor().darker());
                if (i > 0) {
                    g.drawLine(x, box.y, x, box.y + box.height - 1);
                }
            }
            x += width + extraWidth;
        }

    }


    /** Are we currently painting */
    private boolean inPaint = false;

    /**
     * Repaint
     */
    public void repaint() {
        super.repaint();
    }

    /**
     * Paint
     *
     * @param g The graphics
     */
    public void paint(Graphics g) {
        if (inPaint) {
            return;
        }
        inPaint = true;
        paintInner(g);
        inPaint = false;
    }

    /**
     * Really paint
     *
     * @param g The graphics
     */
    public void paintInner(Graphics g) {
        super.paint(g);
        Rectangle bounds = getBounds();
        Rectangle box    = getColorBox(bounds);
        g.setColor(wrapper.getBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        paintColors(g);
        g.setColor(Color.black);
        g.drawRect(box.x - 1, box.y, box.width + 2, box.height - 1);


        for (int i = 0; i < breakpoints.size(); i++) {
            ColorTable.Breakpoint bp =
                (ColorTable.Breakpoint) breakpoints.get(i);
            if (bp == currentBP) {
                continue;
            }

            paintBreakpoint(g, bp, false);
        }
        if (currentBP != null) {
            paintBreakpoint(g, currentBP, true);
        }



        g.setFont(labelFont);
        FontMetrics fm         = g.getFontMetrics();
        String      minLabel   = myRange.formatMin();
        String      midLabel   = myRange.formatMid();
        String      maxLabel   = myRange.formatMax();
        int         lineHeight = fm.getAscent() + fm.getDescent();

        g.setColor(Color.black);
        g.drawString(minLabel, MARGIN_H - fm.stringWidth(minLabel) / 2,
                     lineHeight + MARGIN_V + box.height);
        g.drawString(midLabel,
                     MARGIN_H + box.width / 2 - fm.stringWidth(midLabel) / 2,
                     lineHeight + MARGIN_V + box.height);
        g.drawString(maxLabel,
                     MARGIN_H + box.width - fm.stringWidth(maxLabel) / 2,
                     lineHeight + MARGIN_V + box.height);


        if (cursorOver) {
            double percent = xToPercent(cursorPosition);
            int    index   = percentToColorIndex(percent);
            String value   = Misc.format(percentToValue(percent));
            g.setColor(Color.gray);
            int lineX = cursorPosition;
            g.drawLine(lineX, box.y, lineX, box.y + box.height);
            g.drawString(value, lineX, lineHeight + MARGIN_V + box.height);
        }


    }


    /**
     * Image update method
     *
     * @param img  the image
     * @param flags  flags
     * @param x x  position
     * @param y y  position
     * @param width  width
     * @param height height
     *
     * @return true if successful
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        repaint();
        return true;
    }


    /**
     * Paint the breakpoint
     *
     * @param g graphics
     * @param bp The BP
     * @param isCurrent  true if is the current one
     */
    private void paintBreakpoint(Graphics g, ColorTable.Breakpoint bp,
                                 boolean isCurrent) {
        if (lockImage == null) {
            lockImage = GuiUtils.getImage("/auxdata/ui/icons/lock.gif");
            lockImage.getWidth(this);
        }

        int arrowTop    = MARGIN_V - ARROW_HEIGHT - 1;
        int arrowBottom = MARGIN_V - 1;
        int x           = getBreakpointX(bp);
        g.setColor(getBreakpointColor(bp));
        if (isCurrent) {
            if (colorChooser != null) {
                Color cColor = colorChooser.getColor();
                Color gColor = g.getColor();
                if ((cColor.getRed() != gColor.getRed())
                        || (cColor.getGreen() != gColor.getGreen())
                        || (cColor.getBlue() != gColor.getBlue())) {
                    colorChooser.setColor(g.getColor());
                }
            }

            int alpha = g.getColor().getAlpha();
            if (lastAlpha != alpha) {
                //for now let's not do this
                ignoreTransBoxEvents = true;
                double percent = 1-alpha/(double)255;
                int intPercent = (int)(percent*100+0.5);
                TwoFacedObject tfo = new TwoFacedObject(intPercent + "%",
                                                        new Integer(intPercent));

                transBox.setSelectedItem (tfo);
                ignoreTransBoxEvents = false;
                lastAlpha = alpha;
            }
        }

        if (bp.getLocked()) {
            g.drawImage(lockImage, x - lockImage.getWidth(null) / 2,
                        arrowTop, null);
        } else {
            int[] lxp = { x, x - ARROW_WIDTH, x + ARROW_WIDTH, x };
            int[] lyp = { arrowBottom, arrowTop, arrowTop, arrowBottom };
            g.fillPolygon(lxp, lyp, lxp.length);
            g.setColor(Color.black);
            if (isCurrent) {
                g.setColor(Color.yellow);
            }
            g.drawPolygon(lxp, lyp, lxp.length);
        }

        String label = getBreakpointLabel(bp);
        if (isCurrent) {
            g.setFont(boldFont);
        } else {
            g.setFont(labelFont);
        }

        int labelWidth  = g.getFontMetrics().stringWidth(label);
        int labelHeight = g.getFontMetrics().getHeight();
        if (isCurrent || true) {
            g.setColor(getBackground());
            g.fillRect(x - labelWidth / 2 - 1, arrowTop - labelHeight - 3,
                       labelWidth + 2, labelHeight);
        }


        g.setColor(Color.black);
        g.drawString(label, x - labelWidth / 2, arrowTop - 3);
    }


    /**
     * Class ColorTableCommand is used when the CT is changed and we can undo it using
     * this class
     *
     *
     * @author IDV development team
     */
    public static class ColorTableCommand extends CanvasCommand {

        /** Old one */
        ColorTable oldColorTable;

        /** New one */
        ColorTable newColorTable;



        /**
         * Create me
         *
         * @param canvas The canvas
         * @param newCt  The CT
         *
         */
        public ColorTableCommand(ColorTableCanvas canvas, ColorTable newCt) {
            super(canvas);
            this.oldColorTable =
                new ColorTable(canvas.getCurrentColorTable());
            this.newColorTable = new ColorTable(newCt);
        }

        /**
         * Apply command
         */
        public void doCommand() {
            super.doCommand();
            canvas.setColorTableInner(newColorTable, true);
        }

        /**
         * Undo command
         */
        public void undoCommand() {
            super.undoCommand();
            canvas.setColorTableInner(oldColorTable, true);
        }
    }

    /**
     * Class ColorListCommand
     *
     *
     * @author IDV development team
     */
    public static class ColorListCommand extends CanvasCommand {

        /** Old list */
        ArrayList oldColorList;

        /** New list */
        ArrayList newColorList;

        /** Old list */
        ArrayList oldScalesList;

        /** New list */
        ArrayList newScalesList;

        /**
         * Create me
         *
         * @param canvas The canvas
         *
         */
        public ColorListCommand(ColorTableCanvas canvas) {
            super(canvas);
            oldColorList  = canvas.colorList;
            newColorList  = (ArrayList) oldColorList.clone();
            oldScalesList = canvas.scales;
            newScalesList = (ArrayList) oldScalesList.clone();
        }

        /**
         * Do it
         */
        public void doCommand() {
            super.doCommand();
            canvas.setColorList(newColorList, newScalesList);
            canvas.tableChanged();
        }

        /**
         * Undo it
         */
        public void undoCommand() {
            super.undoCommand();
            canvas.setColorList(oldColorList, oldScalesList);
            canvas.tableChanged();
        }
    }



    /**
     * Clone the list of breakpoings
     *
     * @param l   the list
     *
     * @return  a clone
     */
    private static ArrayList cloneList(List l) {
        return ColorTable.Breakpoint.cloneList(l);
    }

    /**
     * Class CanvasCommand takes a snapshot of the state of the canvas
     *
     *
     * @author IDV development team
     */
    public static class CanvasCommand extends Command {

        /** New BPS */
        ArrayList newBreakpoints;

        /** Old BPS */
        ArrayList oldBreakpoints;

        /** new breakpoint */
        ColorTable.Breakpoint newBP;

        /** old breakpoint */
        ColorTable.Breakpoint oldBP;



        /** The canvas */
        ColorTableCanvas canvas;

        /**
         * Create me
         *
         * @param canvas The canvas
         */
        public CanvasCommand(ColorTableCanvas canvas) {
            this.canvas    = canvas;
            newBreakpoints = canvas.breakpoints;
            newBP          = canvas.currentBP;
        }

        /**
         * Do it
         */
        public void doCommand() {
            oldBreakpoints     = cloneList(canvas.breakpoints);
            oldBP              = find(canvas.currentBP, oldBreakpoints);
            canvas.breakpoints = newBreakpoints;
            canvas.currentBP   = newBP;
            canvas.repaint();
        }

        /**
         * Undo it
         */
        public void undoCommand() {
            newBreakpoints     = cloneList(canvas.breakpoints);
            newBP              = find(canvas.currentBP, newBreakpoints);
            canvas.breakpoints = cloneList(oldBreakpoints);
            canvas.currentBP   = find(oldBP, canvas.breakpoints);
            canvas.repaint();
        }

        /**
         * Find the breakpoint in the list
         *
         * @param bp The BP
         * @param l list
         *
         * @return  the breakpoint or null if not found
         */
        private ColorTable.Breakpoint find(ColorTable.Breakpoint bp, List l) {
            if ((bp == null) || (l == null)) {
                return null;
            }
            for (int i = 0; i < l.size(); i++) {
                ColorTable.Breakpoint tmp = (ColorTable.Breakpoint) l.get(i);
                if ((tmp.getValue() == bp.getValue())
                        && (tmp.getLocked() == bp.getLocked())) {
                    return tmp;
                }
            }
            return null;
        }


    }



    /**
     *  Create and return an ImageIcon that represents this color table.
     *  If there were any errors then return null.
     *
     * @param ct The CT
     * @return The icon
     */
    public static Icon getIcon(ColorTable ct) {
        return getIcon(ct, 100, 15);
    }



    /**
     * _more_
     *
     * @param ct _more_
     * @param width _more_
     * @param height _more_
     *
     * @return _more_
     */
    public static Icon getIcon(ColorTable ct, int width, int height) {
        try {
            if (ct == null) {
                JLabel tmp = new JLabel(" ");
                tmp.setSize(new Dimension(width, height));
                return new ImageIcon(GuiUtils.getImage(tmp));
            }
            //            float[][]array =  ct.getNonAlphaTable();
            //            BaseRGBMap   colorMap = new BaseRGBMap(array);
            //ColorPreview preview  = new ColorPreview(colorMap, height);
            //public static void paintColors(Graphics g, Rectangle box, List<Color> colorList, List<Float>scales) {
            final List<Color> colors  = (List<Color>) ct.getColorList();
            final Rectangle   box     = new Rectangle(0, 0, width, height);
            JPanel            preview = new JPanel() {
                public void paint(Graphics g) {
                    paintColors(g, box, colors, false, true, null);
                }
            };

            preview.setSize(new Dimension(width, height));
            return new ImageIcon(GuiUtils.getImage(preview));
        } catch (Exception exc) {
            return null;
        }
    }




    public static Image getImage(ColorTable ct, int width, int height) {
        try {
            final List<Color> colors  = (List<Color>) ct.getColorList();
            final Rectangle   box     = new Rectangle(0, 0, width, height);
	    BufferedImage image = new BufferedImage((int) width, (int) height,
						    BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = (Graphics2D) image.getGraphics();
	    paintColors(g, box, colors, false, false, true, null);
	    return image;
        } catch (Exception exc) {
            return null;
        }
    }





}

