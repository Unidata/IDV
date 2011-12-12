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


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSource;




import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;


import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



import visad.*;


import java.applet.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.util.StringTokenizer;
import java.util.Vector;

import javax.accessibility.*;

import javax.swing.*;

import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.*;



/**
 * DisplayControl for displaying straight text or HTML.
 *
 * @author IDV development team
 * @version $Revision: 1.93 $
 */
public class TextDisplayControl extends DisplayControlImpl implements HyperlinkListener,
        MouseMotionListener, MouseListener {

    /** property for sharing URL */
    public static final String SHARE_URL = "TextDisplayControl.SHARE_URL";


    /** For having internal images (from auxdata) in html */
    public static final String TAG_INTERNALIMAGE = "intimg";

    /** view tag */
    public static final String TAG_VIEW = "view";

    /** display tag */
    public static final String TAG_DISPLAY = "display";

    /** applet tag */
    public static final String TAG_APPLET = "applet";

    /** component tag */
    public static final String TAG_COMPONENT = "component";

    /** default width */
    public static final int DEFAULT_WIDTH = 450;

    /** default height */
    public static final int DEFAULT_HEIGHT = 550;
    //    public static final Dimension EDITOR_PREFERRRED_SIZE = new Dimension (DEFAULT_WIDTH-5, DEFAULT_HEIGHT);

    /** Preferred dimension for the editor pane */
    public static final Dimension EDITOR_PREFERRRED_SIZE =
        new Dimension(100, DEFAULT_HEIGHT);

    /** Preferred dimension for the scroll pane */
    public static final Dimension SCROLLPANE_PREFERRRED_SIZE =
        new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    /**
     *  With a loud gagging sound...
     *  A flag to tell when we recently set the mouse over label.
     *  We keep track of this because when we do the setText it ends up
     *  triggering a doLayout in the scrollpane (go figure). Which screws up the
     *  scrolling state.
     */
    private long lastTimeSettingMouseOverLabel = 0;

    /** list of display controls */
    private List displayControls;

    /** text contents */
    //    String textContents;

    /** filename for text */
    String filename;

    /** history list for back/forth */
    ArrayList history = new ArrayList();

    /** glyph history for annotations */
    ArrayList glyphHistory = new ArrayList();

    /** list of colors */
    Vector colorList;

    /** list of glyphs */
    private List glyphs = new ArrayList();

    /** current glyph */
    private PolyGlyph currentGlyph = null;

    /** glyph width selector box */
    JComboBox glyphWidthBox;

    /** color selector box */
    JComboBox colorBox;

    /** history index */
    int historyIdx = -1;

    /** mouse over label */
    private JLabel mouseOverLabel = new JLabel("     ");

    /** forward button */
    private JButton fBtn;

    /** back button */
    private JButton bBtn;

    /** editor pane */
    private MyEditorPane editor;

    /** scroll pane */
    private ScrollPane scroller;

    /** lightweight scroll pane */
    private JScrollPane jscroller;

    /** text field for url display/entry */
    private JTextField urlField = new JTextField();



    /**
     * Default constructor; does nothing.  Heavy work done in init().
     */
    public TextDisplayControl() {}



    /**
     * Initialize this class with the supplied {@link DataChoice}.
     *
     * @param dataChoice   choice to describe the data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        //Perhaps we have been unpersisted
        if (history.size() > 0) {
            filename = (String) history.get(historyIdx);
            try {
                //                this.textContents = IOUtil.readContents(filename);
            } catch (Exception exc) {
                //                this.textContents = "Error opening: " + filename + exc;
            }
            setFieldUrl(filename);
            checkHistory();
        } else {
            if ( !setData(dataChoice)) {
                return false;
            }

            /**
             * this.textContents =
             *   ((visad.Text) (getDataInstance().getDataChoice().getData(
             *       getDataSelection(), getRequestProperties()))).getValue();
             */
            filename = dataChoice.getStringId();
            goToUrl(filename);
        }
        return true;
    }


    /**
     *  Overwrite the base class method to return the filename or url.
     *
     *  @return The filename or url as the title.
     */
    protected String getTitle() {
        if (filename != null) {
            return filename;
        }
        return super.getTitle();
    }


    /**
     * Class PolyGlyph
     */
    public static class PolyGlyph {

        /** polygon */
        Polygon polygon = new Polygon();

        /** glyph width */
        float width;

        /** glyph color */
        Color color;

        /** stroke */
        Stroke stroke;

        /**
         * Default Constructor
         */
        public PolyGlyph() {}

        /**
         * Create a new PolyGlyph of the given width and color.
         *
         * @param width    width of glyph
         * @param color    color of glyph
         *
         */
        public PolyGlyph(float width, Color color) {
            this.width = width;
            this.color = color;
        }

        /**
         * Paint this glyph
         *
         * @param g   graphics to paint on
         */
        public void paint(Graphics g) {
            g.setColor((color == null)
                       ? Color.black
                       : color);
            if (stroke == null) {
                stroke = new BasicStroke(width);
            }
            if (g instanceof Graphics2D) {
                ((Graphics2D) g).setStroke(stroke);
            }
            g.drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);
        }

        /**
         * Add a point
         *
         * @param x   x position
         * @param y   y position
         */
        public void addPoint(int x, int y) {
            polygon.addPoint(x, y);
        }


        /**
         *  Set the Color property.
         *
         *  @param value The new value for Color
         */
        public void setColor(Color value) {
            color = value;
        }

        /**
         *  Get the Color property.
         *
         *  @return The Color
         */
        public Color getColor() {
            return color;
        }

        /**
         *  Set the Width property.
         *
         *  @param value The new value for Width
         */
        public void setWidth(float value) {
            width = value;
        }

        /**
         *  Get the Width property.
         *
         *  @return The Width
         */
        public float getWidth() {
            return width;
        }


        /**
         * Set the X points property.
         *
         * @param value  set of x values
         */
        public void setXPoints(int[] value) {
            polygon.xpoints = value;
        }

        /**
         *  Get the XPoints property.
         *
         *  @return The XPoints
         */
        public int[] getXPoints() {
            return polygon.xpoints;
        }


        /**
         *  Set the YPoints property.
         *
         *  @param value The new value for YPoints
         */
        public void setYPoints(int[] value) {
            polygon.ypoints = value;
        }

        /**
         *  Get the YPoints property.
         *
         *  @return The YPoints
         */
        public int[] getYPoints() {
            return polygon.ypoints;
        }

        /**
         *  Set the NPoints property.
         *
         *  @param value The new value for NPoints
         */
        public void setNPoints(int value) {
            polygon.npoints = value;
        }

        /**
         *  Get the NPoints property.
         *  @return The NPoints
         */
        public int getNPoints() {
            return polygon.npoints;
        }
    }

    /**
     * Make the contents for this control
     * @return  UI for the control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        urlField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String fieldUrl = getFieldUrl().trim();
                if ( !getCurrentUrl().equals(fieldUrl)) {
                    click(fieldUrl, true);
                }
            }
        });

        fBtn = GuiUtils.getImageButton(
            "/ucar/unidata/idv/control/images/Forward16.gif", getClass());
        fBtn.setToolTipText("Forward");
        bBtn = GuiUtils.getImageButton(
            "/ucar/unidata/idv/control/images/Back16.gif", getClass());
        bBtn.setToolTipText("Back");
        checkHistory();

        fBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                history(1);
            }
        });

        bBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                history(-1);
            }
        });

        scroller = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS) {
            public void doLayout() {
                //With a loud gagging sound...
                if (System.currentTimeMillis()
                        - lastTimeSettingMouseOverLabel < 1000) {
                    return;
                }
                super.doLayout();
            }
        };


        glyphWidthBox = new JComboBox(new Vector(Misc.newList(new Float(1.0),
                new Float(2.0), new Float(3.0), new Float(4.0))));
        initColors();
        colorBox = new JComboBox(colorList);

        JPanel drawingPanel =
            GuiUtils.hflow(Misc.newList(new JLabel("Width: "), glyphWidthBox,
                                        new JLabel("Color: "), colorBox));

        boolean doJScroller = false;
        editor = new MyEditorPane();
        editor.setEditable(false);
        editor.addHyperlinkListener(this);
        editor.setContentType("text/html");
        editor.setEditorKit(getEditorKit());
        editor.addMouseMotionListener(this);
        editor.addMouseListener(this);

        //First try to use the id of the data choice as a url
        processUrl(filename, true, false);

        if ( !doJScroller) {
            scroller.add(editor);
        }
        editor.setPreferredSize(EDITOR_PREFERRRED_SIZE);


        if (doJScroller) {
            jscroller = new JScrollPane(
                editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);



            JViewport vp = jscroller.getViewport();
            vp.setViewSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        } else {
            scroller.setSize(SCROLLPANE_PREFERRRED_SIZE);
        }

        JButton goBtn = GuiUtils.makeButton("Go:", this, "reload");

        JButton reloadBtn =
            GuiUtils.getImageButton(
                "/ucar/unidata/idv/control/images/Refresh16.gif", getClass());
        reloadBtn.setToolTipText("Reload page");
        reloadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                reload();
            }
        });

        JButton viewSrcBtn =
            GuiUtils.getImageButton(
                "/ucar/unidata/idv/control/images/Source16.gif", getClass());
        viewSrcBtn.setToolTipText("View source");
        viewSrcBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                viewSource();
            }
        });


        JPanel historyPanel = GuiUtils.hbox(Misc.newList(bBtn, fBtn));
        JPanel urlPanel = GuiUtils.hbox(goBtn, urlField, reloadBtn,
                                        viewSrcBtn);
        JPanel controls = GuiUtils.hbox(Misc.newList(historyPanel,
                              new JLabel("  "), urlPanel));
        if (shouldBeDrawing()) {
            controls = GuiUtils.vbox(controls, GuiUtils.left(drawingPanel));
        }

        if (doJScroller) {
            return GuiUtils.topCenter(controls, jscroller);
        } else {
            return GuiUtils.topCenter(controls, scroller);
        }
    }



    /**
     * Create a list of colors.  Can't we use the DisplayConventions method?
     */
    private void initColors() {
        colorList = new Vector();
        colorList.add(new TwoFacedObject("black", Color.black));
        colorList.add(new TwoFacedObject("blue", Color.blue));
        colorList.add(new TwoFacedObject("cyan", Color.cyan));
        colorList.add(new TwoFacedObject("dark gray", Color.darkGray));
        colorList.add(new TwoFacedObject("gray", Color.gray));
        colorList.add(new TwoFacedObject("green", Color.green));
        colorList.add(new TwoFacedObject("light gray", Color.lightGray));
        colorList.add(new TwoFacedObject("magenta", Color.magenta));
        colorList.add(new TwoFacedObject("orange", Color.orange));
        colorList.add(new TwoFacedObject("pink", Color.pink));
        colorList.add(new TwoFacedObject("red", Color.red));
        colorList.add(new TwoFacedObject("yellow", Color.yellow));
        colorList.add(new TwoFacedObject("white", Color.white));
    }


    /**
     * See if we should be drawing or not
     * @return  false
     */
    private boolean shouldBeDrawing() {
        return false;
    }

    /**
     * Get the line width
     * @return  line width
     */
    private float getGlyphLineWidth() {
        return ((Float) glyphWidthBox.getSelectedItem()).floatValue();
    }

    /**
     * Get the glyph color.
     * @return color for glyph
     */
    private Color getGlyphColor() {
        return (Color) ((TwoFacedObject) colorBox.getSelectedItem()).getId();
    }

    /**
     * Paint on the editor
     *
     * @param g  Graphics to paint on.
     */
    private void paintEditor(Graphics g) {
        if ( !shouldBeDrawing()) {
            return;
        }
        for (int i = 0; i < glyphs.size(); i++) {
            PolyGlyph glyph = (PolyGlyph) glyphs.get(i);
            glyph.paint(g);
        }
    }



    /**
     * Public method for mouse dragged events
     *
     * @param e   mouse event
     */
    public void mouseDragged(MouseEvent e) {
        if ( !shouldBeDrawing()) {
            return;
        }
        if (currentGlyph == null) {
            currentGlyph = new PolyGlyph(getGlyphLineWidth(),
                                         getGlyphColor());
            glyphs.add(currentGlyph);
        }
        currentGlyph.addPoint(e.getX(), e.getY());
        editor.repaint();
    }

    /**
     * Public method for mouse moved events
     *
     * @param e   mouse event
     */
    public void mouseMoved(MouseEvent e) {}

    /**
     * Public method for mouse clicked events
     *
     * @param e   mouse event
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * Public method for mouse entered events
     *
     * @param e   mouse event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Public method for mouse exited events
     *
     * @param e   mouse event
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * Public method for mouse Pressed events
     *
     * @param e   mouse event
     */
    public void mousePressed(MouseEvent e) {}

    /**
     * Public method for mouse released events
     *
     * @param e   mouse event
     */
    public void mouseReleased(MouseEvent e) {
        if (currentGlyph != null) {
            currentGlyph = null;
            editor.repaint();
        }
    }

    /**
     * View the document source
     */
    private void viewSource() {
        JTextArea t = new JTextArea(editor.getText());
        JScrollPane sp =
            new JScrollPane(
                t, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(400, 500));
        final JFrame f   = new JFrame("File: " + filename);
        JButton      btn = new JButton("Close");
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                f.dispose();
            }
        });
        JPanel contents = GuiUtils.centerBottom(sp, GuiUtils.wrap(btn));
        GuiUtils.packWindow(f, contents, true);
    }

    /**
     * Reload the page
     */
    public void reload() {
        click(getFieldUrl(), false);
    }


    /**
     * Print the element
     *
     * @param elem    element to print
     * @param tab     tab (prefix) string
     */
    public void print(Element elem, String tab) {
        AttributeSet attrs = elem.getAttributes();
        System.err.println(
            tab + "Tag: " + attrs.getAttribute(StyleConstants.NameAttribute));
        for (Enumeration names = attrs.getAttributeNames();
                names.hasMoreElements(); ) {
            Object name = names.nextElement();
            if (name.equals(StyleConstants.NameAttribute)) {
                continue;
            }
            System.err.println("  " + tab + "" + name + " ="
                               + attrs.getAttribute(name));
        }
        System.err.println(tab + "children:");
        for (int i = 0; i < elem.getElementCount(); i++) {
            print(elem.getElement(i), tab + "  ");
        }
        System.err.println(tab + "done children");
    }

    /**
     * See if an element has an end tag
     *
     * @param elem   element to check
     * @return  true if it has an end tag
     */
    boolean hasEndTag(Element elem) {
        AttributeSet attrs = elem.getAttributes();
        for (Enumeration names = attrs.getAttributeNames();
                names.hasMoreElements(); ) {
            Object name = names.nextElement();
            if (name.toString().equals("endtag")) {
                return true;
            }
        }
        return false;
    }

    /** flag for doing one */
    boolean didone = false;

    /** local kit */
    private HTMLEditorKit editorKit;

    /**
     * Get the editor kit.  Lazy initialization.
     * @return  editor kit
     */
    private HTMLEditorKit getEditorKit() {
        if (editorKit == null) {
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
                    public View create(Element elem) {

                        Object o = elem.getAttributes().getAttribute(
                                       StyleConstants.NameAttribute);
                        if (o.toString().equals(TAG_VIEW)) {
                            return new ViewWrapper(elem);
                        }
                        if (o.toString().equals(TAG_INTERNALIMAGE)) {
                            return new ImageWrapper(elem);
                        }

                        if (o.toString().equals(TAG_DISPLAY)) {
                            return new DisplayWrapper(elem);
                        }
                        if (o.toString().equals(TAG_COMPONENT)) {
                            return new ComponentWrapper(elem);
                        }
                        if (o.toString().equals(TAG_APPLET)) {
                            if (hasEndTag(elem)) {
                                return super.create(elem);
                            }
                            Element parent = elem;
                            while (parent.getParentElement() != null) {
                                parent = parent.getParentElement();
                            }

                            if ( !didone) {
                                didone = true;
                                System.err.println("Applet:");
                                print(parent, "    ");
                            }
                            return new AppletWrapper(elem);
                        }
                        if (o.equals(HTML.Tag.INPUT)) {
                            return new FormWrapper(elem);
                        }
                        return super.create(elem);
                    }
                }
            };
        }
        return editorKit;
    }

    /**
     * Get the history list. Used by XML persistence.
     * @return  list of visited sites
     */
    public ArrayList getHistory() {
        return history;
    }

    /**
     * Set the history list. Used by XML persistence.
     *
     * @param h  list to use
     */
    public void setHistory(ArrayList h) {
        history = h;
    }

    /**
     * Get the glyph history. Used by XML persistence.
     * @return  glyph history list
     */
    public ArrayList getGlyphHistory() {
        return glyphHistory;
    }

    /**
     * Set the history list.  Used by XML persistence.
     *
     * @param h  history list
     */
    public void setGlyphHistory(ArrayList h) {
        glyphHistory = h;
    }

    /**
     *  Set the Glyphs property.
     *
     *  @param value The new value for Glyphs
     */
    public void setGlyphs(List value) {
        glyphs = value;
    }

    /**
     *  Get the Glyphs property.
     *
     *  @return The Glyphs
     */
    public List getGlyphs() {
        return glyphs;
    }



    /**
     * Get the index into the history
     * @return  history index.
     */
    public int getHistoryIdx() {
        return historyIdx;
    }

    /**
     * Set the index into the history
     *
     * @param h  index
     */
    public void setHistoryIdx(int h) {
        historyIdx = h;
    }


    /**
     * Override the base class button panel method
     * @return  component for button panel
     */
    protected Component doMakeMainButtonPanel() {
        Component removeControl = doMakeRemoveControl("Remove this "
                                      + getDisplayName());
        mouseOverLabel.setBorder(new FineLineBorder(BevelBorder.LOWERED));
        JPanel buttonPanel = GuiUtils.centerRight(mouseOverLabel,
                                 removeControl);
        return GuiUtils.inset(buttonPanel, 2);
    }

    /**
     * Process the form data.
     *
     * @param action   action for form
     * @param data     form data.
     */
    public void processForm(String action, String data) {
        if (action == null) {
            //System.err.println ("No action given");
            return;
        }
        StringTokenizer tok  = new StringTokenizer(data, "&");
        String          root = getRootPath();
        if (root == null) {
            root = ".";
        } else {
            //Here the root (should) have a "/" at the end which we want to strip
            if (root.endsWith("/")) {
                root = root.substring(0, root.length() - 1);
            }
        }
        action = StringUtil.replace(action, "%HTMLROOT%", root);

        try {
            while (tok.hasMoreTokens()) {
                String nameValue = tok.nextToken();
                int    idx       = nameValue.indexOf("=");
                if (idx < 0) {
                    continue;
                }
                nameValue = java.net.URLDecoder.decode(nameValue, "UTF-8");
                action =
                    StringUtil.replace(action,
                                       "%"
                                       + nameValue.substring(0, idx).trim()
                                       + "%", nameValue.substring(idx + 1));
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            throw new ucar.unidata.util.WrapperException(uee);
        }
        if (processByTheIdv(action)) {
            return;
        }
        try {
            String result = Misc.doPost(action, data);
            if (result == null) {
                LogUtil.userMessage("Unable to post form to: " + action);
            } else {
                editor.setText(result.toString());
                goToUrl(action);
            }
        } catch (Exception exc) {
            logException("Posting form", exc);
        }
    }

    /**
     * Respond to any hyperlink events: ACTIVATED, ENTERED, EXITED.
     * TODO: When we move to 1.4 we can get the DOM Anchor Element
     * that perhaps could contain a description attribute.
     *
     * @param e  event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                click(e.getDescription(), true);
            } else {
                click(e.getURL().toString(), true);
            }
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            String desc =
                StringUtil.replace(StringUtil.replace(e.getDescription(),
                    "idv:", ""), "idv.", "");
            //With a loud gagging sound
            lastTimeSettingMouseOverLabel = System.currentTimeMillis();
            mouseOverLabel.setText(desc);
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            //With a loud gagging sound
            lastTimeSettingMouseOverLabel = System.currentTimeMillis();
            mouseOverLabel.setText("                  ");
        }
    }

    /**
     * Get the root path.
     * @return  root path
     */
    public String getRootPath() {
        if (filename != null) {
            int idx = filename.lastIndexOf("/");
            if (idx >= 0) {
                return filename.substring(0, idx + 1);
            }
        }
        return null;
    }


    /**
     * Respond to a click.
     *
     * @param linkText     link that was clicked
     * @param doHistory    true if should look at history
     */
    public void click(String linkText, boolean doHistory) {
        click(linkText, doHistory, true);
    }


    /**
     * Respond to a click.
     *
     * @param linkText     link that was clicked
     * @param doHistory    true if should look at history
     * @param andShare     true to share event
     */
    public void click(String linkText, boolean doHistory, boolean andShare) {
        //With a loud gagging sound
        lastTimeSettingMouseOverLabel = 0;
        URL url = null;
        try {
            url = new URL(linkText);
        } catch (Exception e) {
            String root = getRootPath();
            if (root != null) {
                try {
                    url = new URL(root + linkText);
                } catch (Exception e2) {
                    try {
                        url = new URL("file:" + root + linkText);
                    } catch (Exception e3) {}
                }
            }
        }

        if (url == null) {
            try {
                url = new URL("file:" + linkText);
            } catch (Exception e2) {}
        }
        processUrl(url, doHistory, andShare);
    }



    /**
     * Process the url
     *
     * @param linkText The url
     * @param doHistory Add to history
     * @param andShare Share with others
     */
    private void processUrl(String linkText, boolean doHistory,
                            boolean andShare) {
        URL url = null;
        try {
            url = new URL(linkText);
        } catch (Exception e) {
            try {
                url = new URL("file:" + linkText);
            } catch (Exception e2) {}
        }
        processUrl(url, doHistory, andShare);
    }


    /**
     * Process the url
     *
     * @param url The url
     * @param doHistory Add to history
     * @param andShare Share with others
     */
    private void processUrl(URL url, boolean doHistory, boolean andShare) {

        //      System.err.println ("url:" + url);
        if (getHaveInitialized()) {
            if (processByTheIdv(url.toString())) {
                return;
            }
        }


        //Make sure we remove any viewManagers  and display controls that may have been
        //created via <view> and <display> tags
        try {
            clearViewManagers();
            clearDisplayControls();
        } catch (Exception exc) {
            logException("Clearing display controls", exc);
        }

        URL currentUrl = editor.getPage();
        //We need to do this because if we do 2 consecutive setPage calls with the same url
        //(which we do when we do a reload) the editor seems to ignore this.
        try {
            editor.setPage("file:/some/bad/file/path/to/trick/the/editor");
        } catch (Exception exc) {}
        editor.setText("");
        editor.setEditorKit(getEditorKit());
        try {
            if (IOUtil.isImageFile(url.toString())) {
                editor.setText("<img src=\"" + url + "\">");
            } else {
                File dir = getDirectory(url.toString());
                if (dir != null) {
                    dir = dir.getAbsoluteFile();
                    StringBuffer sb     = new StringBuffer();
                    File[]       files  = dir.listFiles();
                    File         parent = dir.getParentFile();

                    sb.append("<html><body><h2>Index of: " + dir
                              + "</h2><hr>\n");
                    if (parent != null) {
                        sb.append("<a href=\"file:" + parent
                                  + "\">Up to higher level directory</a><p>");
                    }

                    sb.append("<table>");
                    if (files != null) {
                        java.util.Arrays.sort(files, new Comparator() {
                            public int compare(Object o1, Object o2) {
                                return o1.toString().toLowerCase().compareTo(
                                    o2.toString().toLowerCase());
                            }
                        });
                        boolean didone = false;
                        for (int i = 0; i < files.length; i++) {
                            if ( !files[i].isDirectory()) {
                                continue;
                            }
                            if ( !didone) {
                                sb.append(
                                    "<tr><td><b>Directories</b></td></tr>\n");
                            }
                            didone = true;

                            sb.append(
                                "<tr  valign=\"bottom\"><td>&nbsp;&nbsp;&nbsp;"
                                + "<" + TAG_INTERNALIMAGE
                                + " src=\"/auxdata/ui/icons/Folder.gif\"> "
                                + "<a href=\"file:" + files[i] + "\">"
                                + IOUtil.getFileTail(files[i].toString())
                                + "</a></td><td></td></tr>\n");
                        }
                        didone = false;
                        for (int i = 0; i < files.length; i++) {
                            if (files[i].isDirectory()) {
                                continue;
                            }
                            if ( !didone) {
                                sb.append("<tr><td><b>Files</b></td></tr>\n");
                            }
                            didone = true;
                            long   len = files[i].length();
                            String lenStr;
                            if (len < 1000) {
                                lenStr = len + " B";
                            } else {
                                lenStr = (int) (len / 1000) + " KB";
                            }
                            sb.append(
                                "<tr valign=\"bottom\"><td>&nbsp;&nbsp;&nbsp;"
                                + "<" + TAG_INTERNALIMAGE
                                + " src=\"/auxdata/ui/icons/File.gif\"> "
                                + "<a href=\"file:" + files[i] + "\">"
                                + IOUtil.getFileTail(files[i].toString())
                                + "</a></td><td align=\"right\"><b>" + lenStr
                                + "</b></td></tr>\n");
                        }
                    }
                    sb.append("</table></body></html>");
                    editor.setText(sb.toString());
                } else {
                    editor.setPage(url);
                }
            }
        } catch (Exception e) {
            logException("Error opening url:" + url, e);
            return;
        }


        filename = url.toString();
        if (andShare) {
            doShare(SHARE_URL, filename);
        }
        setTitle(filename);
        if (doHistory) {
            goToUrl(filename);
        } else {
            setFieldUrl(filename);
        }
    }

    /**
     * Dores the given path represent a file system dir.
     *
     * @param path file path
     *
     * @return Dir File if path is a dir
     */
    private File getDirectory(String path) {
        if (path.startsWith("file:")) {
            String file = path.substring(5);
            File   dir  = new File(file);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Receive shared data from another control
     *
     * @param from        sending control
     * @param dataId      data type
     * @param data        shared data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_URL)) {
            //TODO:            click((String) data[0], true, false);
        }
        super.receiveShareData(from, dataId, data);

    }



    /**
     * Get the display name.
     * @return  empty string
     */
    public String getDisplayName() {
        return "";
    }

    /**
     * Remove from the IDV
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        clearDisplayControls();
    }

    /**
     * Clear and display controls in this pane
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void clearDisplayControls()
            throws RemoteException, VisADException {
        if (displayControls == null) {
            return;
        }
        for (int i = 0; i < displayControls.size(); i++) {
            ((DisplayControl) displayControls.get(i)).doRemove();
        }
        displayControls = null;
    }

    /**
     * Add a display control to this control
     *
     * @param dc   DisplayControl to add
     */
    private void addDisplayControl(DisplayControl dc) {
        if (displayControls == null) {
            displayControls = new ArrayList();
        }
        displayControls.add(dc);
    }


    /**
     * Get the field URL property.
     * @return  field URL property
     */
    private String getFieldUrl() {
        return urlField.getText();
    }

    /**
     * Set the field URL property.
     *
     * @param url   URL to use
     */
    private void setFieldUrl(String url) {
        urlField.setText(url);
    }

    /**
     * Get the current URL.
     * @return  current URL.
     */
    public String getCurrentUrl() {
        return (String) history.get(historyIdx);
    }

    /**
     * Scroll through the history.
     *
     * @param delta  index
     */
    public void history(int delta) {
        historyIdx += delta;
        if (historyIdx < 0) {
            historyIdx = 0;
        } else if (historyIdx >= history.size()) {
            historyIdx = history.size() - 1;
        }
        checkHistory();
        click((String) history.get(historyIdx), false);
    }

    /**
     * Go to a particular URL
     *
     * @param url   URL
     */
    public void goToUrl(String url) {
        //Prune history
        int i = historyIdx + 1;
        while (i < history.size()) {
            history.remove(i);
        }
        url = url.trim();
        if ( !url.startsWith("file:")) {
            if (new File(url).exists()) {
                url = "file:" + url;
            }
        }
        history.add(url);
        historyIdx++;
        setFieldUrl(url);
        checkHistory();
    }

    /**
     * Check the history
     */
    private void checkHistory() {
        if (fBtn != null) {
            bBtn.setEnabled(historyIdx > 0);
            fBtn.setEnabled(historyIdx < history.size() - 1);
        }
    }


    /**
     * Set the wait cursor
     */
    private void waitCursor() {
        editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * Set the normal cursor
     */
    private void normalCursor() {
        editor.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Process an action by the IDV
     *
     * @param action  action to process
     * @return  true if successful
     */
    private boolean processByTheIdv(String action) {
        if (getDirectory(action) != null) {
            return false;
        }


        action = action.trim();
        int idx = action.indexOf("jython:");
        if (idx >= 0) {
            action = action.substring(idx);
        } else {
            if (IOUtil.isHtmlFile(action) || IOUtil.isTextFile(action)
                    || IOUtil.isImageFile(action)) {
                return false;
            }
        }

        final String theAction = action;

        Misc.run(new Runnable() {
            public void run() {
                waitCursor();
                try {
                    getControlContext().handleAction(theAction, null);
                } catch (Exception exc) {
                    logException("Decoding file:" + theAction, exc);
                }
                normalCursor();
            }
        });

        return true;
    }


    /**
     * Class FormWrapper
     *
     */
    class FormWrapper extends FormView {

        /**
         * New form wrapper
         *
         * @param elem   form element
         *
         */
        public FormWrapper(Element elem) {
            super(elem);
        }

        /**
         * Submit the form
         *
         * @param data  form data
         */
        protected void submitData(String data) {
            Element elt = getElement();
            while (elt != null) {
                if (elt.getName().equals("form")) {
                    break;
                }
                elt = elt.getParentElement();
            }
            if (elt == null) {
                elt = getElement();
                if (elt == null) {
                    //System.err.println ("Could not find form element");
                    return;
                }
            }
            processForm(getAction(elt), data);
        }

        /**
         * Get the action from the form element
         *
         * @param formElement   form element
         * @return  action
         */
        public String getAction(Element formElement) {
            AttributeSet formAttr = formElement.getAttributes();
            Enumeration  names    = formAttr.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
            }
            String action =
                (String) (formAttr.getAttribute(HTML.Attribute.ACTION));
            if (action == null) {
                formAttr =
                    (AttributeSet) formAttr.getAttribute(HTML.Tag.FORM);
                if (formAttr != null) {
                    action =
                        (String) formAttr.getAttribute(HTML.Attribute.ACTION);
                    names = formAttr.getAttributeNames();
                    while (names.hasMoreElements()) {
                        Object name = names.nextElement();
                    }
                }
            }
            return action;
        }

    }

    /**
     * Class ViewWrapper.  Wraps a viewer in the display
     */
    class ViewWrapper extends ComponentView {

        /** the view manager to wrap */
        ViewManager viewManager;

        /** Element describing ViewManager */
        Element element;

        /**
         * Construct a wrapper based on the element
         *
         * @param e   element describing ViewManager.
         *
         */
        ViewWrapper(Element e) {
            super(e);
            element = e;
        }

        /**
         * Create a view manager from the element supplied at construction
         * @return  constructed ViewManager
         */
        protected ViewManager createViewManager() {
            AttributeSet attrs    = element.getAttributes();
            Enumeration  names    = attrs.getAttributeNames();
            String       viewName = "viewmanager";

            //String properties = "";
            StringBuffer properties = new StringBuffer("");
            while (names.hasMoreElements()) {
                Object name  = names.nextElement();
                String value = attrs.getAttribute(name).toString();
                //                System.err.println ("name = " +name + " " + value );
                if (name.toString().equalsIgnoreCase("id")) {
                    viewName = value;
                } else if (name.toString().equalsIgnoreCase("props")) {
                    if ( !value.endsWith(";")) {
                        properties.append(";");
                    }
                    properties.append(value);
                } else {
                    properties.append(name.toString());
                    properties.append("=");
                    properties.append(value);
                    properties.append(";");
                }
            }

            //            System.err.println ("properties:" + properties);


            ViewManager viewManager = getControlContext().getViewManager(
                                          new ViewDescriptor(viewName),
                                          false, properties.toString());
            addViewManager(viewManager);
            return viewManager;
        }

        /**
         * Create the ViewManager component
         * @return  the component
         */
        protected Component createComponent() {
            try {
                if (viewManager == null) {
                    viewManager = createViewManager();
                }
                if (viewManager == null) {
                    return null;
                }
                return viewManager.getContents();
            } catch (Exception exc) {
                logException("TextDisplayControl.createComponent", exc);
                return new JLabel("Error");
            }
        }

    }




    /**
     * Class DisplayWrapper.  Wrapper class for a DisplayControl
     */
    class DisplayWrapper extends ComponentView {

        /** label for displaying errors */
        JLabel errorLabel;

        /** display control */
        DisplayControlImpl display;

        /** Element describing control */
        Element element;

        /**
         * Create a wraper using the element
         *
         * @param e  element describing the display control
         *
         */
        DisplayWrapper(Element e) {
            super(e);
            element = e;
        }

        /**
         * Create the DisplayControl based on the element supplied at ctor
         * @return   created DisplayControl
         */
        protected DisplayControlImpl createDisplayControl() {

            StringBuffer properties     = new StringBuffer("");
            String       displayName    = null;
            String       dataSourceName = null;
            String       paramName      = null;

            AttributeSet attrs          = element.getAttributes();
            Enumeration  names          = attrs.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name  = names.nextElement();
                String value = attrs.getAttribute(name).toString();
                if (name.toString().equalsIgnoreCase("display")) {
                    displayName = value;
                } else if (name.toString().equalsIgnoreCase("data")) {
                    dataSourceName = value;
                } else if (name.toString().equalsIgnoreCase("param")) {
                    paramName = value;
                } else {
                    properties.append(name.toString());
                    properties.append("=");
                    properties.append(value);
                    properties.append(";");
                }
            }
            if (displayName == null) {
                errorLabel =
                    new JLabel("Error: No \"display\" attribute given");
                return null;
            }
            if (dataSourceName == null) {
                errorLabel = new JLabel("Error: No \"data\" attribute given");
                return null;
            }
            if (paramName == null) {
                errorLabel =
                    new JLabel("Error: No \"param\" attribute given");
                return null;
            }

            properties.append("makeWindow=false;");

            display = (DisplayControlImpl) getControlContext().createDisplay(
                dataSourceName, paramName, displayName,
                properties.toString(), false);
            if (display != null) {
                addDisplayControl(display);
            } else {
                errorLabel =
                    new JLabel("Error: Failed to create display of type: "
                               + displayName);
            }
            return display;

        }

        /**
         * Create the DisplayControl component
         * @return  the control
         */
        protected Component createComponent() {
            try {
                if (errorLabel != null) {
                    errorLabel.setBackground(Color.lightGray);
                    return errorLabel;
                }
                if (display == null) {
                    display = createDisplayControl();
                }
                if (display == null) {
                    if (errorLabel == null) {
                        errorLabel = new JLabel("Error creating display");
                    }
                    return errorLabel;
                }
                return display.getOuterContents();
            } catch (Exception exc) {
                logException("TextDisplayControl.createComponent", exc);
                return errorLabel = new JLabel("Error");
            }
        }

    }



    /**
     * Class ComponentWrapper.  Wrapper for a component
     */
    class ComponentWrapper extends ComponentView {

        /** element describing component */
        Element element;

        /** the component */
        protected Component comp;

        /**
         * Construct a wrapper for the component.
         *
         * @param e   Element describing component
         */
        public ComponentWrapper(Element e) {
            super(e);
            this.element = e;
        }

        /**
         * Create the component from the Element.
         * @return  component
         */
        protected Component createComponent() {
            if (comp == null) {
                try {
                    comp = createComponentInner();
                } catch (Exception exc) {
                    comp = new JLabel("Error:" + exc.getMessage());
                    exc.printStackTrace();
                }
            }
            return comp;
        }

        /**
         * Inner method for createing the component
         * @return  a label
         */
        protected Component createComponentInner() {
            return new JLabel("Comp");
        }
    }

    /**
     * Wraps an internal Applet.
     */
    class ImageWrapper extends ComponentWrapper {


        /**
         * Construct the wrapper
         *
         * @param e  Element describing the Applet
         */
        ImageWrapper(Element e) {
            super(e);
        }

        /**
         * Create the component
         * @return  the component
         */
        protected Component createComponentInner() {
            AttributeSet attrs = element.getAttributes();
            Enumeration  names = attrs.getAttributeNames();
            String       src   = null;
            while (names.hasMoreElements()) {
                Object name  = names.nextElement();
                String value = attrs.getAttribute(name).toString();
                if (name.toString().trim().equalsIgnoreCase("src")) {
                    src = value;
                    break;
                }
            }


            if (src == null) {
                return new JLabel("no src");
            }
            ImageIcon icon = GuiUtils.getImageIcon(src);
            if (icon == null) {
                return new JLabel("bad image:" + src);
            }
            JLabel lbl = new JLabel(icon);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            lbl.setPreferredSize(new Dimension(icon.getIconHeight(),
                    icon.getIconWidth()));


            return lbl;
        }
    }



    /**
     * Class AppletWrapper.  Wraps an Applet.
     */
    class AppletWrapper extends ComponentWrapper {

        /** frame */
        AppletFrame frame;

        /**
         * Construct the wrapper
         *
         * @param e  Element describing the Applet
         */
        AppletWrapper(Element e) {
            super(e);
        }

        /**
         * Create the component
         * @return  the component
         */
        protected Component createComponentInner() {
            AttributeSet attrs    = element.getAttributes();
            Enumeration  names    = attrs.getAttributeNames();
            int          w        = 400;
            int          h        = 300;
            String       code     = null;
            String       codebase = null;
            String       baseDir  = null;
            Hashtable    params   = new Hashtable();

            while (names.hasMoreElements()) {
                Object name  = names.nextElement();
                String sname = name.toString().toLowerCase().trim();
                String value = attrs.getAttribute(name).toString();
                if (sname.equals("code")) {
                    code = value;
                } else if (sname.equals("width")) {
                    w = new Integer(value).intValue();
                } else if (sname.equals("height")) {
                    h = new Integer(value).intValue();
                } else if (sname.equals("codebase")) {
                    codebase = value;
                } else {
                    params.put(sname, value);
                }
            }

            if (code == null) {
                return new JLabel("No \"code\" attribute given");
            }

            try {
                if (codebase != null) {}
                Applet a = (Applet) Class.forName(code).newInstance();
                return new AppletFrame(a, w, h, baseDir, params);
            } catch (Exception e) {
                e.printStackTrace();
                return new JLabel("Error: " + code + " " + e.getMessage());
            }
        }
    }


    /**
     * Class MyEditorPane.  Custom JEditorPane
     */
    private class MyEditorPane extends JEditorPane {

        /**
         * Default ctor
         */
        public MyEditorPane() {}

        /**
         * Override base class paint method
         *
         * @param g  graphics for painting
         */
        public void paint(Graphics g) {
            super.paint(g);
            paintEditor(g);
        }

        /*
         *    public void setPreferredSize(Dimension d) {
         *    super.setPreferredSize(d);
         *    System.err.println ("******setPreferredSize:" + d);
         *
         *    }
         */

        /**
         * Don't really know what we are all doing but we need to
         * override these JEditorPane methods so it behaves properly
         * when contained within a awt.ScrollPane
         *
         * @return the preferred size
         */
        public Dimension getPreferredSize() {
            Dimension d          = EDITOR_PREFERRRED_SIZE;
            Component port       = getPort();
            TextUI    ui         = getUI();
            int       prefWidth  = d.width;
            int       prefHeight = d.height;
            if ( !getScrollableTracksViewportWidth()) {
                int       w   = port.getWidth();
                Dimension min = ui.getMinimumSize(this);
                if ((w != 0) && (w < min.width)) {
                    // Only adjust to min if we have a valid size
                    prefWidth = min.width;
                }
            }
            if ( !getScrollableTracksViewportHeight()) {
                int       h   = port.getHeight();
                Dimension min = ui.getMinimumSize(this);
                if ((h != 0) && (h < min.height)) {
                    // Only adjust to min if we have a valid size
                    prefHeight = min.height;
                }
            }
            if ((prefWidth != d.width) || (prefHeight != d.height)) {
                d = new Dimension(prefWidth, prefHeight);
            }
            return d;
        }


        /**
         * Override base class method
         * @return  true if okay
         */
        public boolean getScrollableTracksViewportWidth() {
            Component port = getPort();
            TextUI    ui   = getUI();
            int       w    = port.getWidth();
            Dimension min  = ui.getMinimumSize(this);
            Dimension max  = ui.getMaximumSize(this);
            if ((w >= min.width) && (w <= max.width)) {
                return true;
            }
            return false;
        }

        /**
         * Returns true if a viewport should always force the height of this
         * <code>Scrollable</code> to match the height of the viewport.
         *
         * @return true if a viewport should force the
         *              <code>Scrollable</code>'s height to match its own,
         *              false otherwise
         */
        public boolean getScrollableTracksViewportHeight() {
            Component port = getPort();
            TextUI    ui   = getUI();
            int       h    = port.getHeight();
            Dimension min  = ui.getMinimumSize(this);
            if (h >= min.height) {
                Dimension max = ui.getMaximumSize(this);
                if (h <= max.height) {
                    return true;
                }
            }
            return false;
        }


        /**
         * We use this so we can easily switch between using a awt.ScrollPane
         * nad a JScrollPane.
         *
         * @return  the port component
         */
        private Component getPort() {
            if (getParent() instanceof JViewport) {
                return getParent();
            }
            return scroller;
        }



        /**
         *    Keep these around for debugging
         * public Dimension getSize() {
         *   Dimension s = super.getSize ();
         *   System.err.println ("getSize:" + s);
         *   return s;
         * }
         *
         * public void setSize(Dimension d) {
         *   super.setSize (d);
         *   System.err.println ("*****setSize:" + d);
         * }
         *
         * public void setSize(int w, int h) {
         *   super.setSize (w,h);
         *   System.err.println ("*****setSize:" + w +","+h);
         * }
         *
         *
         * public void setBounds(int x, int y, int width, int height) {
         *   super.setBounds (x,y,width,height);
         *   System.err.println ("***setBounds " + x +","+y+","+width+","+height);
         * }
         *
         * public void setBounds(Rectangle r) {
         *   super.setBounds (r);
         *   System.err.println ("***setBounds " + r);
         * }
         *
         * public int getWidth() {
         *   int s = super.getWidth ();
         *   //            System.err.println ("getWidth:" + s);
         *   return s;
         * }
         *
         * public int getHeight() {
         *   int s = super.getHeight ();
         *   //            System.err.println ("getHeight:" + s);
         *   return s;
         * }
         *
         * public Dimension getMaximumSize() {
         *   Dimension d= super.getMaximumSize();
         *   System.err.println ("getMaximumSize:" + d);
         *   return d;
         * }
         *
         *
         * public Dimension getMinimumSize() {
         *   Dimension d= super.getMinimumSize();
         *   System.err.println ("getMinimumSize:" + d);
         *   return d;
         * }
         *
         */
    }


    /**
     * test code
     *
     * @param archiveName archive name
     */
    public void writeTestArchive(String archiveName) {
        try {
            archiveName = archiveName + "_" + getDisplayId();
            String guiImageFile = archiveName + "_editor.jpg";
            toFront();
            Misc.sleep(200);
            System.err.println("Writing image:" + guiImageFile);
            ImageUtils.writeImageToFile(editor, guiImageFile);
            super.writeTestArchive(archiveName);
        } catch (Exception exc) {
            logException("writing image", exc);
        }
    }


}
