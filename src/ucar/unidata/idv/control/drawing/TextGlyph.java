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

package ucar.unidata.idv.control.drawing;


import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.*;
import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;

import visad.util.ImageHelper;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


import javax.swing.event.ChangeEvent;
import javax.swing.text.*;
import javax.swing.text.html.*;


/**
 * Class TextGlyph Draws text
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.52 $
 */
public class TextGlyph extends DrawingGlyph {

    /** Justifications */
    private static final TextControl.Justification[] JUSTIFICATIONS = { TextControl
                                                                          .Justification
                                                                          .LEFT,
            TextControl.Justification.CENTER, TextControl.Justification.RIGHT,
            TextControl.Justification.TOP, TextControl.Justification.BOTTOM };

    /** Justification name */
    public static final String JUST_LEFT = "Left";

    /** Justification name */
    public static final String JUST_CENTER = "Center";

    /** Justification name */
    public static final String JUST_RIGHT = "Right";

    /** Justification name */
    public static final String JUST_TOP = "Top";

    /** Justification name */
    public static final String JUST_BOTTOM = "Bottom";

    /** Justification names */
    private static final String[] JUSTIFICATION_NAMES = { JUST_LEFT,
            JUST_CENTER, JUST_RIGHT, JUST_TOP, JUST_BOTTOM };



    /** Keep track of the last click in the display */
    private DisplayEvent displayEvent;

    /** Holds the text_ */
    JTextArea textArea;

    /** Holds the text scroller */
    JScrollPane textSP;

    /** Shows the html */
    JEditorPane editor;

    /** The listener we add */
    HyperlinkListener hyperlinkListener;


    /** Holds the text editor */
    JComponent editorWrapper;

    /** holds the text */
    JComponent textContents;


    /** xml attr names */
    public static final String ATTR_MARKER = "marker";

    /** xml attr names */
    public static final String ATTR_MARKERSCALE = "markerscale";

    /** xml attr names */
    public static final String ATTR_SHOWMARKER = "showmarker";

    /** xml attr names */
    public static final String ATTR_JUSTIFICATION = "justification";

    /** xml attr names */
    public static final String ATTR_FONTSIZE = "fontsize";

    /** xml attr names */
    public static final String ATTR_FONTFACE = "fontface";

    /** xml attr names */
    public static final String ATTR_FONTSTRING = "fontstring";

    /** xml attr names */
    public static final String ATTR_FONT = "font";

    /** The horizontal justification */
    private String horizontalJustification = JUST_LEFT;

    /** The vertical justification */
    private String verticalJustification = JUST_BOTTOM;

    /** The type used in the display */
    private TextType textType;

    /** The font used in the display */
    private Font font;

    /** The text drawn */
    private String text;

    /** The current editor we use for rendering the image */
    private JEditorPane renderedEditor;

    /** Shows the text image */
    private ImageRGBDisplayable imageDisplayable;

    /** The domain */
    Linear2DSet imageDomain;

    /** The last rendered image */
    Image image;

    /** width */
    int width;

    /** height */
    int height;

    /** image data */
    FlatField imageData;

    /** The marker */
    private ShapeDisplayable pointDisplayable;

    /** Show the marker */
    protected boolean showMarker = false;

    /** marker scale */
    protected double markerScale = 0.1;

    /** marker type */
    protected String markerType = ShapeUtility.PIN;

    /** width of the html editor */
    private int htmlWidth = -1;


    /** origin */
    double[] origin;

    /** lower right */
    double[] lr;

    /** list of dialogs */
    List dialogs = new ArrayList();

    /** Last scale */
    private float lastScale = Float.NaN;

    /** HTML Editor kit */
    private HTMLEditorKit editorKit;

    /**
     * ctor
     */
    public TextGlyph() {}



    /**
     * ctor
     *
     * @param control The control I'm in
     * @param event The event when I was created
     * @param text Initial text
     */
    public TextGlyph(DrawingControl control, DisplayEvent event,
                     String text) {
        super(control, event);
        this.text = text;
    }

    /**
     * is this glyph a raster
     *
     * @return is raster
     */
    public boolean getIsRaster() {
        return true;
    }




    /**
     * Get the extra descripition used in the JTable listing
     *
     * @return extra description
     */
    public String getExtraDescription() {
        return text;
    }



    /**
     * Initialize after creation
     *
     * @param event The event
     *
     * @return this
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {
        super.handleCreation(event);
        points = Misc.newList(getPoint(event));
        updateLocation();
        return null;
    }


    /**
     * User created me.
     *
     *
     * @param control The control I'm in
     * @param event The event
     *
     * @return ok
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        Font font = control.getFont();
        if (font != null) {
            setFont(font);
        }
        horizontalJustification = control.getJustification();
        verticalJustification   = control.getVerticalJustification();
        makeTextContents();
        if ( !GuiUtils.showOkCancelDialog(null, "Text entry", textContents,
                                          null)) {
            return false;
        }
        text      = textArea.getText();
        htmlWidth = editor.getSize().width;
        return super.initFromUser(control, event);
    }


    /**
     * Handle the event
     *
     * @param event the event
     *
     * @return Did we handle this event
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean mousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        this.displayEvent = event;
        if ((renderedEditor == null) || (origin == null) || (lr == null)) {
            return false;
        }
        double[] xy = control.screenToBox(event.getX(), event.getY());
        double xpercent = (xy[IDX_X] - origin[IDX_X])
                          / (lr[IDX_X] - origin[IDX_X]);
        double ypercent = (xy[IDX_Y] - origin[IDX_Y])
                          / (lr[IDX_Y] - origin[IDX_Y]);

        //        System.err.println ("event:" +xy[0] + " " + xy[1] + " " + xpercent+ " " + ypercent);

        if ((xpercent < 0) || (xpercent > 1)) {
            return false;
        }
        if ((ypercent < 0) || (ypercent > 1)) {
            return false;
        }

        int             x  = (int) (xpercent * width);
        int             y  = (int) (ypercent * height);

        MouseListener[] ml = renderedEditor.getMouseListeners();
        //        MouseListener[] ml = editor.getMouseListeners();
        MouseEvent me = null;
        //Try to find the left mouse trigger
        int[] buttons = { MouseEvent.BUTTON1, MouseEvent.BUTTON2,
                          MouseEvent.BUTTON3 };
        int[] masks = { InputEvent.BUTTON1_MASK, InputEvent.BUTTON2_MASK,
                        InputEvent.BUTTON2_MASK };

        for (int i = 0; i < buttons.length; i++) {
            MouseEvent tmpMe = new MouseEvent(renderedEditor, 0,
                                   System.currentTimeMillis(), masks[i], x,
                                   y, 1, false, buttons[i]);
            if (SwingUtilities.isLeftMouseButton(tmpMe)) {
                me = tmpMe;
                break;
            }
        }
        if (me == null) {
            return true;
        }
        for (int j = 0; j < ml.length; j++) {
            ml[j].mouseClicked(me);
        }
        return true;
    }



    /**
     * Mkae the text gui
     */
    private void makeTextContents() {
        if (textArea == null) {
            textArea = new JTextArea("", 5, 30);
            editor   = new JEditorPane();
            editor.setEditable(false);
            editor.setContentType("text/html");
            //            editor.setEditorKit(getEditorKit());
            editor.addHyperlinkListener(getHyperlinkListener());


            textArea.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent ke) {
                    try {
                        editor.setText(processHtml(textArea.getText()));
                    } catch (Exception exc) {
                        LogUtil.logException("Rendering html image", exc);
                    }
                }
            });
            textSP = new JScrollPane(textArea);
            textArea.setPreferredSize(new Dimension(300, 100));
            textSP.setPreferredSize(new Dimension(300, 100));
            editor.setPreferredSize(new Dimension(300, 75));

            //            GuiUtils.tmpInsets = GuiUtils.INSETS_5;
            textContents = GuiUtils.doLayout(new JComponent[] {
                new JLabel("Text: (May be html)"),
                textSP,
                GuiUtils.inset(new JLabel("Preview:"),
                               new Insets(5, 0, 0, 0)),
                editorWrapper = GuiUtils.hsplit(editor, GuiUtils.filler(),
                                                getHtmlWidthToUse(), 1.0) }, 1, GuiUtils.WT_Y, GuiUtils.WT_NYNY);


            textContents = GuiUtils.inset(textContents, 5);


        }
        textArea.setText(text);
    }

    /**
     * Xml created me
     *
     *
     * @param control The control
     * @param node The xml
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, org.w3c.dom.Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);
        String just = XmlUtil.getAttribute(node, ATTR_JUSTIFICATION,
                                           (String) null);
        if (just != null) {
            just = just.toLowerCase();
            if (just.length() != 2) {
                System.err.println("Unknown text justification:" + just);
            } else {
                if (just.charAt(0) == 'l') {
                    horizontalJustification = JUST_LEFT;
                } else if (just.charAt(0) == 'c') {
                    horizontalJustification = JUST_CENTER;
                } else if (just.charAt(0) == 'r') {
                    horizontalJustification = JUST_RIGHT;
                } else {
                    System.err.println(
                        "Unknown text horizontal justification:" + just);
                }
                if (just.charAt(1) == 'a') {
                    verticalJustification = JUST_BOTTOM;
                } else if (just.charAt(1) == 'b') {
                    verticalJustification = JUST_TOP;
                } else if (just.charAt(1) == 'c') {
                    verticalJustification = JUST_CENTER;
                } else {
                    System.err.println("Unknown text vertical justification:"
                                       + just);
                }
            }
        }

        String fontStr = XmlUtil.getAttribute(node, ATTR_FONTSTRING,
                             (String) null);
        if (fontStr != null) {
            setFont(Font.decode(fontStr));
        } else {
            setFont(new Font(XmlUtil.getAttribute(node, ATTR_FONTFACE,
                    "times"), Font.PLAIN,
                              XmlUtil.getAttribute(node, ATTR_FONTSIZE, 12)));
        }
        this.text = XmlUtil.getAttribute(node, ATTR_TEXT);
        this.showMarker = XmlUtil.getAttribute(node, ATTR_SHOWMARKER,
                showMarker);
        this.markerType = XmlUtil.getAttribute(node, ATTR_MARKER, markerType);
        this.markerScale = XmlUtil.getAttribute(node, ATTR_MARKERSCALE,
                markerScale);

    }




    /**
     * The tag to use in the xml
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_TEXT;
    }

    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(org.w3c.dom.Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_TEXT, text);
        String just = "";
        if (horizontalJustification.equalsIgnoreCase(JUST_LEFT)) {
            just = "l";
        } else if (horizontalJustification.equalsIgnoreCase(JUST_CENTER)) {
            just = "c";
        } else {
            just = "r";
        }
        if (verticalJustification.equalsIgnoreCase(JUST_TOP)) {
            just += "b";
        } else if (verticalJustification.equalsIgnoreCase(JUST_BOTTOM)) {
            just += "a";
        } else {
            just += "c";
        }
        e.setAttribute(ATTR_JUSTIFICATION, just);
        e.setAttribute(ATTR_MARKER, markerType);
        e.setAttribute(ATTR_SHOWMARKER, "" + showMarker);
        e.setAttribute(ATTR_MARKERSCALE, "" + markerScale);

        if (font != null) {
            e.setAttribute(ATTR_FONTFACE, font.getName());
            e.setAttribute(ATTR_FONTSIZE, "" + font.getSize());
        }
    }


    /**
     * Create, if needed, the hyper link listener
     *
     * @return hyper link listener
     */
    private HyperlinkListener getHyperlinkListener() {
        if (hyperlinkListener == null) {
            hyperlinkListener = new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    handleHyperlinkUpdate(e);
                }
            };
        }
        return hyperlinkListener;
    }


    /**
     * Render the html
     *
     * @param html html
     * @param htmlWidth how wide
     *
     * @return the image
     *
     * @throws Exception On badness
     */
    private Image getImage(String html, int htmlWidth) throws Exception {
        Color bg = getBgcolor();
        if (renderedEditor == null) {
            renderedEditor = new JEditorPane();
            renderedEditor.setContentType("text/html");
            renderedEditor.setEditorKit(getEditorKit());
            renderedEditor.addHyperlinkListener(getHyperlinkListener());
        }

        Color tmpBg = new Color(123, 124, 125);

        ImageUtils.getEditor(renderedEditor, processHtml(html), htmlWidth,
                             ((bg == null)
                              ? tmpBg
                              : null), null);

        if (bg != null) {
            renderedEditor.setBackground(bg);
        } else {
            renderedEditor.setBackground(tmpBg);
        }
        // System.err.println("\nGetting image");
        Image image = ((bg != null)
                       ? ImageUtils.getImage(renderedEditor)
                       : ImageUtils.getImage(renderedEditor, tmpBg));
        //        JComponent p = GuiUtils.inset(new JLabel(new ImageIcon(image)),new Insets(20,20,20,20));
        //        p.setBackground(Color.green);
        //        GuiUtils.showOkCancelDialog(null,null,p , null);
        return image;
    }

    /**
     * Handle the click
     *
     * @param e event
     */
    private void handleHyperlinkUpdate(HyperlinkEvent e) {
        Hashtable properties = new Hashtable();
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }

        String url;
        if (e.getURL() == null) {
            url = e.getDescription();
        } else {
            url = e.getURL().toString();
        }
        //        System.err.println ("Click:" + url);
        url = url.trim();
        if (url.indexOf("popup(") >= 0) {
            String  text;
            boolean modal = true;
            if (url.startsWith("modalpopup")) {
                text  = url.trim().substring(11);
                modal = true;
            } else {
                text  = url.trim().substring(6);
                modal = false;
            }
            text = text.substring(0, text.length() - 1);
            Component comp =
                imageDisplayable.getDisplayMaster().getDisplayComponent();
            int x = comp.getLocationOnScreen().x + displayEvent.getX();
            int y = comp.getLocationOnScreen().y + displayEvent.getY();
            if (dialogs.size() > 0) {
                Component[] comps = (Component[]) dialogs.get(0);
                try {
                    String contents = IOUtil.readContents(text,
                                          (String) null);
                    if (contents != null) {
                        text = contents;
                    }
                } catch (Exception exc) {}
                ((JEditorPane) comps[0]).setText(text);
                JDialog dialog = (JDialog) comps[1];
                if ( !dialog.isVisible()) {
                    dialog.setLocation(new Point(x, y));
                    dialog.show();
                } else {
                    dialog.toFront();
                }
                return;
            }
            Component[] comps = GuiUtils.popup(text, x, y, modal);
            if ( !modal) {
                dialogs.add(comps);
            }
            return;
        }

        properties.put("glyph", this);
        properties.put("control", control);
        control.getControlContext().getIdv().hyperlinkUpdate(e, properties);
    }

    /**
     * Has this glyph been removed
     *
     * @param value been removed
     */
    public void setBeenRemoved(boolean value) {
        super.setBeenRemoved(value);
        if (value) {
            for (int i = 0; i < dialogs.size(); i++) {
                Component[] comps = (Component[]) dialogs.get(i);
                ((JDialog) comps[1]).dispose();
            }
        }
    }


    /**
     * Process the html.
     *
     * @param text  The text
     *
     * @return The processed text
     */
    private String processHtml(String text) {
        Color  c   = getColor();
        String tmp = text;
        //Not sure if we want to replace new line with br
        //        String tmp = StringUtil.replace(text, "\n", "<br>");
        tmp = "<html><body style=\"margin:0;" + "color:#"
              + StringUtil.padRight(Integer.toHexString(c.getRed()), 2, "0")
              + StringUtil.padRight(
                  Integer.toHexString(c.getGreen()), 2,
                  "0") + StringUtil.padRight(
                      Integer.toHexString(c.getBlue()), 2, "0") + "; "
                          + ((font != null)
                             ? "font-size:" + font.getSize() + "pt;"
                             : "") + ((font != null)
                                      ? "font-family:" + font.getFamily()
                                        + ";"
                                      : "") + "\">" + tmp + "</body></html>";
        //        System.err.println("tmp:" + tmp);
        return tmp;
    }


    /**
     * Get the html width to use. This is either the width of the editor pane or,
     * if the text begins with the tag "<nobr>" then -1 which results in one big line.
     *
     * @return html image width to use for rendering
     */
    private int getHtmlWidthToUse() {
        if(text!=null && text.trim().startsWith("<nobr>")) {
            return  -1;
        }
        return htmlWidth;
    }


    /**
     * update the image
     */
    private void updateImage() {
        try {
            image = getImage(text, getHtmlWidthToUse());
            //Use our own makeField for now
            //            imageData   = Util.makeField(image, true);
            imageData   = makeField(image, 0f, false);
            imageDomain = (Linear2DSet) imageData.getDomainSet();
        } catch (Exception exc) {
            LogUtil.logException("Rendering html image", exc);
        }
    }


    /**
     * Do the final initialization
     *
     * @return Successful
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {
        if ( !super.initFinalInner()) {
            return false;
        }
        updateImage();


        textType = TextType.getTextType("TextGlyph_" + (typeCnt++));
        imageDisplayable = new ImageRGBDisplayable("ImageGlyph."
                + (typeCnt++), false);
        ColorTable colorTable = control.getRGBColorTable();
        imageDisplayable.setRangeForColor(0.0, 255.0);
        imageDisplayable.setColorPalette(colorTable.getAlphaTable());

        if ( !getFullLatLon() || isInXYSpace()) {
            imageDisplayable.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        } else {
            imageDisplayable.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        }


        addDisplayable(imageDisplayable);

        pointDisplayable = new ShapeDisplayable("TextGlyph_" + (typeCnt++),
                ShapeUtility.createShape(ShapeUtility.PLUS)[0]);

        setMarkerType(markerType);
        pointDisplayable.setVisible(showMarker);
        pointDisplayable.setAutoSize(true);
        if (font != null) {
            setFont(font);
        }
        if (showMarker) {
            addDisplayable(pointDisplayable);
        }
        return true;
    }


    /**
     * Handle event
     *
     * @param event The event
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return null;
    }


    /**
     * Handle event
     *
     * @param event The event
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseMoved(DisplayEvent event)
            throws VisADException, RemoteException {
        points = Misc.newList(getPoint(event));
        updateLocation();
        return this;
    }



    /**
     * Done being created._
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void textGlyphDoneBeingCreated()
            throws VisADException, RemoteException {
        if ( !showMarker && (text.trim().length() == 0)) {
            control.removeGlyph(this);
        } else {
            control.setSelection(this);
        }
    }



    /**
     *  Set the Font property.
     *
     *  @param value The new value for Font
     */
    public void setFont(Font value) {
        font = value;
    }

    /**
     *  Get the Font property.
     *
     *  @return The Font
     */
    public Font getFont() {
        return font;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldShowBgColorSelector() {
        return true;
    }




    /**
     * Handle the property apply.
     *
     * @param compMap Holds property widgets
     *
     *
     * @return success
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if ( !super.applyProperties(compMap)) {
            return false;
        }
        text      = ((JTextField) compMap.get(ATTR_TEXT)).getText();
        text      = textArea.getText();
        htmlWidth = editor.getSize().width;

        JComboBox      fontBox = (JComboBox) compMap.get(ATTR_FONT);
        TwoFacedObject fontTfo = (TwoFacedObject) fontBox.getSelectedItem();
        font = (Font) fontTfo.getId();



        JComboBox fontSizeBox = (JComboBox) compMap.get(ATTR_FONTSIZE);
        Object    selected    = fontSizeBox.getSelectedItem();
        int       fontSize    = (new Integer(selected.toString())).intValue();

        setShowMarker(
            ((JCheckBox) compMap.get("showMarkerCbx")).isSelected());
        JComboBox  shapeBox   = (JComboBox) compMap.get("shapeBox");
        JTextField scaleField = (JTextField) compMap.get("scaleField");
        markerScale = Misc.parseNumber(scaleField.getText().trim());
        setMarkerType(TwoFacedObject.getIdString(shapeBox.getSelectedItem()));


        horizontalJustification =
            (String) ((JComboBox) compMap.get("hjust")).getSelectedItem();
        verticalJustification =
            (String) ((JComboBox) compMap.get("vjust")).getSelectedItem();

        setFont(font.deriveFont((float) fontSize));
        setText(text);
        editor.setText(processHtml(text));
        updateImage();
        return true;
    }

    /**
     * Get the editor kit.  Lazy initialization.
     * @return  editor kit
     */
    private HTMLEditorKit getEditorKit() {
        if ((editorKit == null) || true) {
            editorKit = new HTMLEditorKit() {
                public ViewFactory getViewFactory() {
                    return new MyHTMLFactory();
                }

                public String toString() {
                    return "MyEditorKit";
                }

                class MyHTMLFactory extends HTMLFactory {

                    /**
                     * Create a new View from the Element
                     *
                     * @param elem   element to use
                     * @return  created View
                     */
                    public View create(javax.swing.text.Element elem) {

                        Object o = elem.getAttributes().getAttribute(
                                       StyleConstants.NameAttribute);
                        if (o.toString().equals("colortable")) {
                            System.err.println("making ctw");
                            return new ColorTableWrapper(elem);
                        }
                        return super.create(elem);
                    }
                }
            };
        }
        return editorKit;
    }


    /**
     * Class ViewWrapper.  Wraps a viewer in the display
     */
    class ColorTableWrapper extends ComponentView {

        /** Element describing ViewManager */
        javax.swing.text.Element element;

        /**
         * Construct a wrapper based on the element
         *
         * @param e   element describing ViewManager.
         *
         */
        ColorTableWrapper(javax.swing.text.Element e) {
            super(e);
            element = e;
        }



        /**
         * Create the ViewManager component
         * @return  the component
         */
        protected Component createComponent() {
            AttributeSet attrs  = element.getAttributes();
            Enumeration  names  = attrs.getAttributeNames();
            String       ctName = null;
            //String properties = "";
            StringBuffer properties = new StringBuffer("");
            while (names.hasMoreElements()) {
                Object name  = names.nextElement();
                String value = attrs.getAttribute(name).toString();
                if ( !value.equals("colortable")
                        && name.toString().equalsIgnoreCase("name")) {
                    ctName = value;
                    break;
                }
            }
            if (ctName == null) {
                return new JLabel("No name defined");
            }
            JLabel label =
                control.getControlContext().getIdv().getColorTableManager()
                    .getLabel(ctName);
            if (label == null) {
                return new JLabel("Unknown name:" + ctName);
            }

            JLabel other = new JLabel("THIS IS THE LABEL") {
                public void paintComponent(Graphics g) {
                    g.setColor(Color.red);
                    super.paintComponent(g);
                    System.err.println("painting label");
                }
            };
            return GuiUtils.vbox(label, other);
            //            return label;
        }

    }





    /**
     * Viewpoint changed. Update the image if needed
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void viewpointChanged() throws VisADException, RemoteException {
        float scale = control.getDisplayScale();
        if (scale == lastScale) {
            return;
        }
        updateImage();
        updateLocation();
    }

    /**
     * Projection changed. Update the image.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void projectionChanged() throws VisADException, RemoteException {
        lastScale = Float.NaN;
        updateLocation();
    }


    /**
     * Make the properties widgetsF
     *
     * @param comps List of components
     * @param compMap Map to hold name to widget
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        JTextField textField = new JTextField(text);
        makeTextContents();

        comps.add(GuiUtils.rLabel("Text:"));
        comps.add(textSP);
        comps.add(GuiUtils.rLabel("Preview:"));
        comps.add(editorWrapper);

        editor.setText(processHtml(text));
        compMap.put(ATTR_TEXT, textField);

        JComboBox fontBox     = GuiUtils.doMakeFontBox(font);
        JComboBox fontSizeBox = GuiUtils.doMakeFontSizeBox(font.getSize());
        compMap.put(ATTR_FONTSIZE, fontSizeBox);
        compMap.put(ATTR_FONT, fontBox);
        comps.add(GuiUtils.rLabel("Font:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(fontBox, new JLabel("Size:"),
                fontSizeBox, 5)));




        JComboBox horizontalJustificationBox = new JComboBox(new Object[] {
                                                   TextGlyph.JUST_LEFT,
                TextGlyph.JUST_CENTER, TextGlyph.JUST_RIGHT });
        horizontalJustificationBox.setSelectedItem(horizontalJustification);
        compMap.put("hjust", horizontalJustificationBox);

        JComboBox verticalJustificationBox = new JComboBox(new Object[] {
                                                 TextGlyph.JUST_BOTTOM,
                TextGlyph.JUST_CENTER, TextGlyph.JUST_TOP });

        verticalJustificationBox.setSelectedItem(verticalJustification);
        compMap.put("vjust", verticalJustificationBox);
        comps.add(GuiUtils.rLabel("Alignment:"));
        comps.add(GuiUtils.left(GuiUtils.flow(new Component[] {
            horizontalJustificationBox,
            verticalJustificationBox })));


        JTextField     scaleField    = new JTextField("" + markerScale, 5);
        JCheckBox      showMarkerCbx = new JCheckBox("Show", showMarker);
        JComboBox      shapeBox      = new JComboBox(ShapeUtility.SHAPES);
        TwoFacedObject tfo = new TwoFacedObject(markerType, markerType);
        int            index = Misc.toList(ShapeUtility.SHAPES).indexOf(tfo);
        if (index >= 0) {
            shapeBox.setSelectedIndex(index);
        } else {
            shapeBox.setSelectedItem(tfo);
        }
        compMap.put("showMarkerCbx", showMarkerCbx);
        compMap.put("shapeBox", shapeBox);
        compMap.put("scaleField", scaleField);

        JPanel markerPanel = GuiUtils.left(GuiUtils.hbox(showMarkerCbx,
                                 shapeBox,
                                 GuiUtils.label(" Scale: ", scaleField)));
        comps.add(GuiUtils.rLabel("Marker:"));
        comps.add(markerPanel);


        super.getPropertiesComponents(comps, compMap);
    }



    /**
     * Get the name of this glyph type
     *
     * @return  The name
     */
    public String getTypeName() {
        return "Text";
    }


    /**
     * Handle glyph moved
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() == 0) {
            return;
        }
        if ((text == null) || (imageDisplayable == null)
                || (points.size() == 0)) {
            return;
        }
        if (font == null) {
            Font cfont = control.getFont();
            if (cfont != null) {
                setFont(cfont);
            }
        }

        lastScale = control.getDisplayScale();
        try {
            int[] pt = control.boxToScreen(getBoxPoint(0));
            int   x  = pt[IDX_X];
            int   y  = pt[IDX_Y];
            width  = imageDomain.getX().getLength();
            height = imageDomain.getY().getLength();
            int left, right, top, bottom;


            if (horizontalJustification.equalsIgnoreCase(JUST_LEFT)) {
                left  = x;
                right = x + width;
            } else if (horizontalJustification.equalsIgnoreCase(JUST_RIGHT)) {
                left  = x - width;
                right = x;
            } else {
                left  = x - width / 2;
                right = x + width / 2;
            }
            if (verticalJustification.equalsIgnoreCase(JUST_TOP)) {
                top    = y;
                bottom = y + height;
            } else if (verticalJustification.equalsIgnoreCase(JUST_BOTTOM)) {
                top    = y - height;
                bottom = y;
            } else {
                top    = y - height / 2;
                bottom = y + height / 2;
            }

            origin = control.screenToBox(left, top);
            lr     = control.screenToBox(right, bottom);
            lr[2]  = origin[2] = getZPosition();
            Linear2DSet domain =
                new Linear2DSet(RealTupleType.SpatialCartesian2DTuple,
                                (float) origin[IDX_X], (float) lr[IDX_X],
                                imageDomain.getX().getLength(),
                                (float) origin[IDX_Y], (float) lr[IDX_Y],
                                imageDomain.getY().getLength());

            /*
            int         pixels = width * height;
            float[][] 3dvalues = new float[3][pixels];
            int       pixel  = 0;
            for (int row = 0; row < height; row++) {
                double rowPercent = row / (double) height;
                for (int col = 0; col < width; col++) {
                    double colPercent = col / (double) width;
                                        3dValues[0][pixel] = (float) lat;
                                        3dValues[1][pixel] = (float) lon;
                                        3dValues[2][pixel] = (float) (alt1
                                                                      + (alt2 - alt1) * colPercent);
                                        pixel++;
                }
            }

            Gridded3DSet    newDomain = new Gridded3DSet(
                                RealTupleType.SpatialCartesian3DTuple,
                                3dValues, width, height, null, null, null,
                                false);
            */
            FlatField newImageData =
                (FlatField) GridUtil.setSpatialDomain(imageData, domain);
            imageDisplayable.setConstantPosition(getZPosition(),
                    Display.ZAxis);
            imageDisplayable.loadData((FieldImpl) getTimeField(newImageData));
            actualPoints = getBoundingBox(Misc.newList(origin, lr));
        } catch (Exception exc) {
            LogUtil.logException("Rendering image", exc);
            return;
        }

        float[][] points = getPointValues(true);
        pointDisplayable.setPoint((double) points[0][0],
                                  (double) points[1][0],
                                  (double) points[2][0]);

        super.updateLocation();
    }


    /**
     *  Set the Text property.
     *
     *  @param value The new value for Text
     */
    public void setText(String value) {
        text = value;
        try {
            updateLocation();
        } catch (Exception exc) {
            LogUtil.logException("Setting text", exc);
        }
    }


    /**
     *  Get the Text property.
     *
     *  @return The Text
     */
    public String getText() {
        return text;
    }

    /**
     * Set the Justification property.
     *
     * @param value The new value for Justification
     */
    public void setHorizontalJustification(String value) {
        horizontalJustification = value;
    }

    /**
     * Get the Justification property.
     *
     * @return The Justification
     */
    public String getHorizontalJustification() {
        return horizontalJustification;
    }


    /**
     * Set the VerticalJustification property.
     *
     * @param value The new value for VerticalJustification
     */
    public void setVerticalJustification(String value) {
        verticalJustification = value;
    }

    /**
     * Get the VerticalJustification property.
     *
     * @return The VerticalJustification
     */
    public String getVerticalJustification() {
        return verticalJustification;
    }

    /**
     * Set the ShowMarker property.
     *
     * @param value The new value for ShowMarker
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setShowMarker(boolean value)
            throws VisADException, RemoteException {
        boolean change = (showMarker != value);
        showMarker = value;
        if (change && (pointDisplayable != null)) {
            if (showMarker) {
                addDisplayable(pointDisplayable);
            } else {
                removeDisplayable(pointDisplayable);
            }
        }
    }


    /**
     * Get the ShowMarker property.
     *
     * @return The ShowMarker
     */
    public boolean getShowMarker() {
        return showMarker;
    }


    /**
     * Set the MarkerType property.
     *
     * @param value The new value for MarkerType
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setMarkerType(String value)
            throws VisADException, RemoteException {
        markerType = value;
        if (pointDisplayable != null) {
            VisADGeometryArray marker =
                ShapeUtility.createShape(markerType)[0];
            marker =
                ShapeUtility.setSize(marker,
                                     0.4f * (float) markerScale
                                     * (float) control.getDisplayScale());
            pointDisplayable.setMarker(marker);
        }
    }



    /**
     * Get the MarkerType property.
     *
     * @return The MarkerType
     */
    public String getMarkerType() {
        return markerType;
    }

    /**
     * Set the MarkerScale property.
     *
     * @param value The new value for MarkerScale
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setMarkerScale(double value)
            throws VisADException, RemoteException {
        markerScale = value;
        setMarkerType(markerType);
    }

    /**
     * Get the MarkerScale property.
     *
     * @return The MarkerScale
     */
    public double getMarkerScale() {
        return markerScale;
    }



    /**
     *  Set the HtmlWidth property.
     *
     *  @param value The new value for HtmlWidth
     */
    public void setHtmlWidth(int value) {
        htmlWidth = value;
    }

    /**
     * Get the HtmlWidth property.
     *
     *  @return The HtmlWidth
     */
    public int getHtmlWidth() {
        return htmlWidth;
    }




    /**
     * For now copy this frim Util to fix the colliding realtype problem with ImageGlyphs
     *
     * @param image _more_
     * @param alphaThreshold _more_
     * @param makeAlpha _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     * @throws VisADException _more_
     */
    private static FlatField makeField(Image image, float alphaThreshold,
                                       boolean makeAlpha)
            throws IOException, VisADException {

        if (image == null) {
            throw new VisADException("image cannot be null");
        }
        ImageHelper ih = new ImageHelper();

        // determine image height and width
        int width  = -1;
        int height = -1;
        do {
            if (width < 0) {
                width = image.getWidth(ih);
            }
            if (height < 0) {
                height = image.getHeight(ih);
            }
            if (ih.badImage || ((width >= 0) && (height >= 0))) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        } while (true);
        if (ih.badImage) {
            throw new IOException("Not an image");
        }

        // extract image pixels
        int   numPixels = width * height;
        int[] words     = new int[numPixels];

        PixelGrabber grabber = new PixelGrabber(image.getSource(), 0, 0,
                                   width, height, words, 0, width);

        try {
            grabber.grabPixels();
        } catch (InterruptedException e) {}

        ColorModel cm        = grabber.getColorModel();

        float[]    red_pix   = new float[numPixels];
        float[]    green_pix = new float[numPixels];
        float[]    blue_pix  = new float[numPixels];
        float[]    alpha_pix = new float[numPixels];

        boolean    hasAlpha  = cm.hasAlpha();


        for (int i = 0; i < numPixels; i++) {
            red_pix[i]   = cm.getRed(words[i]);
            green_pix[i] = cm.getGreen(words[i]);
            blue_pix[i]  = cm.getBlue(words[i]);
            if (hasAlpha) {
                alpha_pix[i] = cm.getAlpha(words[i]);
            } else {
                alpha_pix[i] = 0f;
            }
        }


        int     alphaCnt = 0;
        boolean opaque   = true;

        if ( !makeAlpha && (alphaThreshold >= 0)) {
            float alphaValue;
            for (int i = 0; i < numPixels; i++) {
                if (hasAlpha) {
                    alphaValue = cm.getAlpha(words[i]);
                } else {
                    alphaValue = 255.0f;
                }


                if (alphaValue <= alphaThreshold) {
                    alphaCnt++;
                    red_pix[i]   = Float.NaN;
                    green_pix[i] = Float.NaN;
                    blue_pix[i]  = Float.NaN;
                    opaque       = false;
                }
            }
        }

        //        System.err.println(alphaThreshold +" hasAlpha:" + hasAlpha +" make alpha:" + makeAlpha +" cnt:" + alphaCnt);

        //System.out.println("opaque = " + opaque);

        // build FlatField
        RealType   line    = RealType.getRealType("ImageLine");
        RealType   element = RealType.getRealType("ImageElement");
        RealType   c_red   = RealType.getRealType("Red_text");
        RealType   c_green = RealType.getRealType("Green_text");
        RealType   c_blue  = RealType.getRealType("Blue_text");
        RealType   c_alpha = RealType.getRealType("Alpha_text");

        RealType[] c_all   = (makeAlpha
                              ? new RealType[] { c_red, c_green, c_blue,
                c_alpha }
                              : new RealType[] { c_red, c_green, c_blue });
        RealTupleType radiance          = new RealTupleType(c_all);

        RealType[]    domain_components = { element, line };
        RealTupleType image_domain      =
            new RealTupleType(domain_components);
        Linear2DSet domain_set = new Linear2DSet(image_domain, 0.0,
                                     (float) (width - 1.0), width,
                                     (float) (height - 1.0), 0.0, height);
        FunctionType image_type = new FunctionType(image_domain, radiance);

        FlatField    field      = new FlatField(image_type, domain_set);

        float[][]    samples    = (makeAlpha
                                   ? new float[][] {
                  red_pix, green_pix, blue_pix, alpha_pix
              }
                                   : new float[][] {
                  red_pix, green_pix, blue_pix
              });
        try {
            field.setSamples(samples, false);
        } catch (RemoteException e) {
            throw new VisADException("Couldn't finish image initialization");
        }

        return field;

    }



}
