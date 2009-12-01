/*
 * $Id: EditCanvas.java,v 1.31 2007/08/08 18:55:26 jeffmc Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.ui.XmlUi;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;


/**
 *
 */

public class EditCanvas extends DisplayCanvas implements MouseListener,
        MouseMotionListener, ActionListener, FocusListener, KeyListener {




    /** _more_ */
    public static final String CMD_ZOOMIN = "cmd.zoom.in";

    /** _more_ */
    public static final String CMD_ZOOMOUT = "cmd.zoom.out";

    /** _more_ */
    public static final String CMD_ZOOMRESET = "cmd.zoom.reset";

    /** _more_ */
    public static final String CMD_EDIT_CUT = "edit.cut";

    /** _more_ */
    public static final String CMD_EDIT_COPY = "edit.copy";

    /** _more_ */
    public static final String CMD_EDIT_PASTE = "edit.paste";

    /** _more_ */
    public static final String CMD_EDIT_SELECTALL = "edit.selectall";

    /** _more_ */
    public static final String CMD_EDIT_GROUP = "edit.group";

    /** _more_ */
    public static final String CMD_EDIT_UNGROUP = "edit.ungroup";

    /** _more_ */
    public static final String CMD_EDIT_TOFRONT = "edit.tofront";

    /** _more_ */
    public static final String CMD_EDIT_TOBACK = "edit.toback";


    /** _more_ */
    public static final String CMD_ALIGN_PREFIX = "align.";

    /** _more_ */
    public static final String CMD_ALIGN_TOP = CMD_ALIGN_PREFIX + "top";

    /** _more_ */
    public static final String CMD_ALIGN_CENTER = CMD_ALIGN_PREFIX + "center";

    /** _more_ */
    public static final String CMD_ALIGN_BOTTOM = CMD_ALIGN_PREFIX + "bottom";

    /** _more_ */
    public static final String CMD_ALIGN_LEFT = CMD_ALIGN_PREFIX + "left";

    /** _more_ */
    public static final String CMD_ALIGN_MIDDLE = CMD_ALIGN_PREFIX + "middle";

    /** _more_ */
    public static final String CMD_ALIGN_RIGHT = CMD_ALIGN_PREFIX + "right";

    public static final String CMD_SPACE_H =  CMD_ALIGN_PREFIX+"spaceh";
    public static final String CMD_SPACE_V =  CMD_ALIGN_PREFIX+"spacev";

    public static final String CMD_SNAP =  "cmd.snap";


    /** _more_ */
    private boolean selectionSticky = false;

    /** _more_ */
    private List selectionMenuItems = new ArrayList();

    /** _more_ */
    private List shapeGroup = new ArrayList();

    /** _more_ */
    private List colorList;

    /**
     *  Some local event state
     */
    int lastx;

    /** _more_ */
    int lasty;

    /** _more_ */
    int mousex;

    /** _more_ */
    int mousey;

    /** _more_ */
    boolean mouseWasPressed = false;

    /** _more_ */
    String currentUrl;


    /**
     *  The current command object. If this is non-null then all
     *  events are routed to it. The command member is set to the return
     *  of the routed-to call
     */
    CanvasCommand currentCommand;




    /**
     * _more_
     *
     */
    public EditCanvas() {
        addFocusListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
    }



    /**
     * _more_
     */
    public void setDefaultCursor() {
        setCursor(DEFAULT_CURSOR);
    }

    /**
     */
    String uniqueifier = null;

    /**
     * _more_
     * @return _more_
     */
    String getGlyphId() {
        if (uniqueifier == null) {
            uniqueifier = "" + System.currentTimeMillis();
        }
        return "ID" + "-" + uniqueifier + "-" + (glyphCnt++);
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean okToPaintHighlight() {
        return true;
    }


    /**
     *  Called from the base class. It calls the parent to paint the list of
     *  Glyphs and then it tells any highlighted glyph to paint. And then
     *  it tells the currentCommand to paint.
     *
     * @param g
     */
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.black);
        if ((highlightedGlyph != null) && okToPaintHighlight()) {
            highlightedGlyph.paintHighlight(g, this);
        }
        if (currentCommand != null) {
            currentCommand.doPaint(g);
        }
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean haveCommand() {
        return (currentCommand != null);
    }

    /** _more_ */
    boolean haveChanged = false;

    /**
     * _more_
     * @return _more_
     */
    public boolean getHaveChanged() {
        return haveChanged;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setHaveChanged(boolean v) {
        haveChanged = v;
    }

    /**
     * _more_
     *
     * @param newCommand
     */
    public void setCommand(CanvasCommand newCommand) {
        if ((newCommand != null) && newCommand.doesChangeGlyphs()) {
            setHaveChanged(true);
        }
        if ((currentCommand != null) && (currentCommand != newCommand)) {
            CanvasCommand tmpCommand = currentCommand;
            currentCommand = null;
            tmpCommand.doComplete();
            setDefaultCursor();
        }
        currentCommand = newCommand;

        if (newCommand != null) {
            Cursor cursor = newCommand.getCursor();
            if (cursor != null) {
                setCursor(cursor);
            }

            /**
             *  Atomic commands do not receive any events
             *  we call setCommand with them for  when we have an undo/redo facility
             */
            if (newCommand.isAtomic()) {
                setCommand(null);
            }

        }
    }




    /**
     *  We have these hooks here so that derived classes can do things based
     *  on glyph events (e.g., tell the whiteboard server that a Glyph moved
     *
     * @param theGlyph
     */
    public void notifyGlyphMoved(Glyph theGlyph) {}

    /**
     * _more_
     *
     * @param theGlyph
     */
    public void notifyGlyphMoveComplete(Glyph theGlyph) {}

    /**
     * _more_
     *
     * @param theGlyph
     * @param attr
     */
    public void notifyGlyphChanged(Glyph theGlyph, String attr) {}

    /**
     * _more_
     *
     * @param theGlyph
     * @param attr
     */
    public void notifyGlyphChangeDone(Glyph theGlyph, String attr) {}


    /**
     * _more_
     *
     * @param g
     * @param diddleSelection
     * @param fromPaste
     */
    public void notifyGlyphCreateComplete(Glyph g, boolean diddleSelection,
                                          boolean fromPaste) {
        super.notifyGlyphCreateComplete(g, diddleSelection, fromPaste);
        if (diddleSelection) {
            clearSelection();
            addSelection(g);
        }
    }


    /**
     * _more_
     *
     * @param e
     */
    public void focusGained(FocusEvent e) {
        if (currentCommand != null) {
            //      currentCommand.doFocusGained (e);
            return;
        }
    }

    /**
     * _more_
     *
     * @param e
     */
    public void focusLost(FocusEvent e) {
        if (currentCommand != null) {
            setCommand(currentCommand.doFocusLost(e));
            return;
        }
    }


    /**
     * _more_
     *
     * @param e
     */
    public void keyReleased(KeyEvent e) {
        if (currentCommand != null) {
            setCommand(currentCommand.doKeyReleased(e));
            return;
        }
    }


    /**
     * _more_
     *
     * @param evt
     */
    public void keyTyped(KeyEvent evt) {}


    /**
     * _more_
     *
     * @param evt
     */
    public void keyPressed(KeyEvent evt) {
        //If we have  a command then route to it.
        if (currentCommand != null) {
            setCommand(currentCommand.doKeyPress(evt));
            return;
        }
        char key     = evt.getKeyChar();
        int  keyCode = evt.getKeyCode();
	if (GuiUtils.isDeleteEvent(evt)) {
            doCut();
        }

        if (evt.isControlDown()) {
            key = (char) (key + 'a' - 1);

            /**
             * if (keyCode ==KeyEvent.VK_MINUS) {
             *   scaleFactor = scaleFactor-0.1;
             *   if(scaleFactor<0.1) scaleFactor = 0.1;
             *   repaint();
             * } else             if (keyCode ==KeyEvent.VK_EQUALS) {
             *   scaleFactor += 0.1;
             *   repaint();
             *   } else
             */
            if (key == 'x') {
                doCut();
            } else if (key == 'c') {
                doCopy();
            } else if (key == 'v') {
                doPaste();
            } else if (key == 'a') {
                selectAll();
            } else if (key == 'g') {
                group();
            } else if (key == 'u') {
                unGroup();
                //Hook for undo
            } else if ((key == 'z') && false) {
                if (cutBuffer != null) {}
            } else {
                //      super.keyPressed (evt);
            }
            return;
        }

        if ((key == 'f') || (key == 'b')) {
            for (int i = 0; i < selectionSet.size(); i++) {
                Glyph g = (Glyph) selectionSet.get(i);
                if (key == 'f') {
                    moveToFront(g);
                } else {
                    moveToBack(g);
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param label
     * @param mnem
     * @param command
     * @param enabled
     * @return _more_
     */
    protected JMenuItem makeSelectionMenuItem(String label, char mnem,
            String command, boolean enabled) {

        JMenuItem mi = makeMenuItem(label, mnem, command);
        selectionMenuItems.add(mi);
        if ( !enabled) {
            mi.setEnabled(false);
        }
        return mi;
    }

    /**
     * _more_
     *
     * @param label
     * @param mnem
     * @param command
     * @return _more_
     */
    protected JMenuItem makeMenuItem(String label, char mnem,
                                     String command) {
        JMenuItem mi = makeMenuItem(label, command);
        //Don't add the mnemomic since we already listen for key commands
        //        mi.setMnemonic(mnem);
        //        mi.setAccelerator(getKeyStroke(mnem));
        return mi;
    }

    /**
     * _more_
     *
     * @param label
     * @param command
     * @return _more_
     */
    protected JMenuItem makeMenuItem(String label, String command) {
        JMenuItem mi = new JMenuItem(label);
        mi.setActionCommand(command);
        mi.addActionListener(this);
        return mi;
    }

    /**
     * _more_
     * @return _more_
     */
    public JMenu makeEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        return makeEditMenu(editMenu);
    }

    /**
     * _more_
     * @return _more_
     */
    public JMenu makeViewMenu() {
        JMenu viewMenu = new JMenu("View");
        return makeViewMenu(viewMenu);
    }


    /**
     * _more_
     */
    public void selectionChanged() {
        super.selectionChanged();
        boolean hasSelection = hasSelection();
        for (int i = 0; i < selectionMenuItems.size(); i++) {
            ((JMenuItem) selectionMenuItems.get(i)).setEnabled(hasSelection);
        }
    }

    /**
     * _more_
     *
     * @param editMenu
     * @return _more_
     */
    public JMenu makeEditMenu(JMenu editMenu) {
        boolean hasSelection = hasSelection();
        boolean hasBuffer    = ((cutBuffer != null)
                                && (cutBuffer.size() > 0));
        editMenu.add(makeSelectionMenuItem("Cut", 'x', CMD_EDIT_CUT,
                                           hasSelection));
        editMenu.add(makeSelectionMenuItem("Copy", 'c', CMD_EDIT_COPY,
                                           hasSelection));
        editMenu.add(makeSelectionMenuItem("Paste", 'v', CMD_EDIT_PASTE,
                                           hasBuffer));
        editMenu.addSeparator();
        editMenu.add(makeMenuItem("Select All", 'a', CMD_EDIT_SELECTALL));
        editMenu.addSeparator();
        editMenu.add(makeSelectionMenuItem("To Front", 'f', CMD_EDIT_TOFRONT,
                                           hasSelection));
        editMenu.add(makeSelectionMenuItem("To Back", 'b', CMD_EDIT_TOBACK,
                                           hasSelection));
        if (doGroup()) {
            editMenu.addSeparator();
            editMenu.add(makeSelectionMenuItem("Group", 'g', CMD_EDIT_GROUP,
                    hasSelection));
            editMenu.add(makeSelectionMenuItem("Ungroup", 'u',
                    CMD_EDIT_UNGROUP, hasSelection));
        }





        return editMenu;
    }


    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    private KeyStroke getKeyStroke(char c) {
        int keyCode = GuiUtils.charToKeyCode(c);
        if (keyCode < 0) {
            return KeyStroke.getKeyStroke(c, InputEvent.CTRL_MASK);
        }
        return KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_MASK);
    }

    /**
     * _more_
     *
     * @param viewMenu
     * @return _more_
     */
    public JMenu makeViewMenu(JMenu viewMenu) {
        JMenuItem mi;
        viewMenu.add(mi = makeMenuItem("Zoom in", '=', CMD_ZOOMIN));
        viewMenu.add(mi = makeMenuItem("Zoom out", '-', CMD_ZOOMOUT));
        viewMenu.add(mi = makeMenuItem("Zoom reset", '0', CMD_ZOOMRESET));

        JMenu gridMenu = new JMenu("Grid");
        viewMenu.add(gridMenu);
        gridMenu.add(GuiUtils.makeCheckboxMenuItem("Show",
                                                   this,"showGrid",null));

        gridMenu.add(GuiUtils.makeMenuItem("Increase",this,"increaseGridSpacing"));
        gridMenu.add(GuiUtils.makeMenuItem("Decrease",this,"decreaseGridSpacing"));


        return viewMenu;
    }






    /*
     * _more_
     * @return _more_
     */
    public boolean doGroup() {
        return true;
    }


    /**
     * zoom in
     */
    protected void doZoomIn() {
        scaleFactor = scaleFactor + 0.1;
        repaint();
    }

    /**
     * zoom out
     */
    protected void doZoomOut() {
        scaleFactor = scaleFactor - 0.1;
        if (scaleFactor < 0.1) {
            scaleFactor = 0.1;
        }
        repaint();
    }

    /**
     * _more_
     *
     * @param event
     */
    public void actionPerformed(ActionEvent event) {
        String action = event.getActionCommand();
        if (action.equals(CMD_ZOOMOUT)) {
            doZoomOut();
        } else if (action.equals(CMD_ZOOMIN)) {
            doZoomIn();
        }
        if (action.equals(CMD_ZOOMRESET)) {
            scaleFactor = 1.0;
            repaint();
        } else if (action.equals(CMD_EDIT_CUT)) {
            doCut();
        } else if (action.equals(CMD_EDIT_COPY)) {
            doCopy();
        } else if (action.equals(CMD_EDIT_TOFRONT)) {
            doToFront();
        } else if (action.equals(CMD_EDIT_TOBACK)) {
            doToBack();
        } else if (action.equals(CMD_EDIT_PASTE)) {
            doPaste();
        } else if (action.equals(CMD_EDIT_SELECTALL)) {
            selectAll();
        } else if (action.equals(CMD_EDIT_GROUP)) {
            group();
        } else if (action.equals(CMD_EDIT_UNGROUP)) {
            unGroup();
        } else if (action.equals(CMD_SNAP)) {
            snapToGrid();
        } else if (action.startsWith(CMD_ALIGN_PREFIX)) {
            doAlign(action);
        }
    }

    /**
     * _more_
     */
    public void group() {
        if ( !doGroup()) {
            return;
        }
        if ( !hasSelection()) {
            return;
        }
        if ((selectionSet.size() == 1)
                && (selectionSet.get(0) instanceof GroupGlyph)) {
            return;
        }

        Glyph glyph = new GroupGlyph(selectionSet);
        glyph.setId(getGlyphId());
        addGlyph(glyph);
        notifyGlyphCreateComplete(glyph, true, false);
    }

    /**
     * _more_
     */
    public void unGroup() {
        if ( !doGroup()) {
            return;
        }
        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph g = (Glyph) selectionSet.get(i);
            if ( !(g instanceof GroupGlyph)) {
                continue;
            }
            //First ungroup, then tell others of the change, then do the remove
            ((GroupGlyph) g).unGroup();
            notifyGlyphChangeDone(g, Glyph.ATTR_CHILDREN);
            removeGlyph(g);
        }
        repaint();
    }


    /**
     *  Paste the given vector of glyphs.
     *  We find the upper left point of the set of glyphs
     *  to get an offset from the given x,y coords.
     *
     * @param l
     * @param x
     * @param y
     */
    public void doPaste(List l, int x, int y) {
        if (l == null) {
            return;
        }
        clearSelection();
        int ox = Integer.MAX_VALUE;
        int oy = Integer.MAX_VALUE;
        for (int i = 0; i < l.size(); i++) {
            Glyph     g = (Glyph) l.get(i);
            Rectangle b = g.getBounds();
            if (b.x < ox) {
                ox = b.x;
            }
            if (b.y < oy) {
                oy = b.y;
            }
        }

        for (int i = 0; i < l.size(); i++) {
            //Get a new id, shift position, add glyph to the glyph list and selection set
            Glyph g = (Glyph) l.get(i);
            g.setId(getGlyphId());
            g.moveBy(x - ox, y - oy);
            addGlyph(g);
            addSelection(g);
            notifyGlyphCreateComplete(g, false, true);
        }
    }

    /**
     * _more_
     */
    public void doPaste() {
        doPaste(cloneGlyphs(cutBuffer), mousex, mousey);
    }


    /**
     * _more_
     *
     * @param from
     * @return _more_
     */
    public List cloneGlyphs(List from) {
        List to = new ArrayList();
        if(from == null) return to;
        for (int i = 0; i < from.size(); i++) {
            Glyph o = (Glyph) from.get(i);
            try {
                to.add(o.clone());
            } catch (Exception exc) {}
        }
        return to;
    }


    /**
     * _more_
     */
    public void doToFront() {
        if ((selectionSet == null) || (selectionSet.size() == 0)) {
            return;
        }
        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph glyph = (Glyph) selectionSet.get(i);
            glyphs.remove(glyph);
            glyphs.add(glyph);
        }
        repaint();
    }

    /**
     * _more_
     */
    public void doToBack() {
        if ((selectionSet == null) || (selectionSet.size() == 0)) {
            return;
        }
        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph glyph = (Glyph) selectionSet.get(i);
            glyphs.remove(glyph);
            glyphs.add(0, glyph);
        }
        repaint();
    }


    /**
     * _more_
     */
    public void doCopy() {
        cutBuffer = cloneGlyphs(selectionSet);
    }


    /**
     * _more_
     */
    public void doCut() {
        cutBuffer    = selectionSet;
        selectionSet = new ArrayList();
        selectionChanged();
        for (int i = 0; i < cutBuffer.size(); i++) {
            Glyph g = (Glyph) cutBuffer.get(i);
            if (g.getPersistent()) {
                removeGlyph(g);
            }
        }
    }

    /**
     * _more_
     */
    public void selectAll() {
        selectionSet.clear();
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = (Glyph) glyphs.get(i);
            if (glyph.pickable()) {
                selectionSet.add(glyph);
            }
        }
        selectionChanged();
        repaint();
    }


    public void spaceH() {
        doAlign(CMD_SPACE_H);
    }

    public void spaceV() {
        doAlign(CMD_SPACE_V);
    }

    /**
     *  Align the set of selected glyphs with the given command
     *  (e.g., align.top, align.bottom, etc.)
     *
     * @param cmd
     */
    public void doAlign(String cmd) {
        int top    = Integer.MAX_VALUE;
        int vmid   = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        int left   = Integer.MAX_VALUE;
        int hmid   = Integer.MIN_VALUE;
        int right  = Integer.MIN_VALUE;

        List<Glyph> glyphs = new ArrayList<Glyph>();
        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph     g = (Glyph) selectionSet.get(i);
            Rectangle b = g.getBounds();
            top    = Math.min(b.y, top);
            left   = Math.min(b.x, left);
            bottom = Math.max(b.y + b.height, bottom);
            right  = Math.max(b.x + b.width, right);
            vmid   = Math.max(b.y + b.height / 2, vmid);
            hmid   = Math.max(b.x + b.width / 2, hmid);
            int size = glyphs.size();
            if (cmd.equals(CMD_SPACE_H)) {
                for(int j=0;j<glyphs.size();j++) {
                    Glyph other =  glyphs.get(j);
                    Rectangle ob = other.getBounds();
                    if(b.x<ob.x) {
                        glyphs.add(j,g);
                        break;
                    }
                }
                if(glyphs.size() == size) glyphs.add(g);
            }  else if (cmd.equals(CMD_SPACE_V)) {
                for(int j=0;j<glyphs.size();j++) {
                    Glyph other =  glyphs.get(j);
                    Rectangle ob = other.getBounds();
                    if(b.y<ob.y) {
                        glyphs.add(j,g);
                        break;
                    }
                }
                if(glyphs.size() == size) glyphs.add(g);
            } else {
                glyphs.add(g);
            }
        }

        if(glyphs.size()==0) return;
        int delta;
        int cnt=0;
        for (Glyph g: glyphs) {
            Rectangle b = g.getBounds();
            if (cmd.equals(CMD_ALIGN_TOP)) {
                g.moveBy(0, top - b.y);
            } else if (cmd.equals(CMD_ALIGN_CENTER)) {
                g.moveBy(0, vmid - (b.y + b.height / 2));
            } else if (cmd.equals(CMD_ALIGN_BOTTOM)) {
                g.moveBy(0, bottom - (b.y + b.height));
            } else if (cmd.equals(CMD_ALIGN_LEFT)) {
                g.moveBy(left - b.x, 0);
            } else if (cmd.equals(CMD_ALIGN_MIDDLE)) {
                g.moveBy(hmid - (b.x + b.width / 2), 0);
            } else if (cmd.equals(CMD_ALIGN_RIGHT)) {
                g.moveBy(right - (b.x + b.width), 0);
            } else if (cmd.equals(CMD_SPACE_H)) {
                int dx = (right-left)/glyphs.size();
                int newX = left+dx*cnt;
                g.moveBy(newX-b.x,0);
            } else if (cmd.equals(CMD_SPACE_V)) {
                int dy = (bottom-top)/glyphs.size();
                int newY = top+dy*cnt;
                g.moveBy(0,newY-b.y);
            }
            cnt++;
            notifyGlyphMoveComplete(g);
        }
        repaint();
    }


    public void snapToGrid() {
        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph     g = (Glyph) selectionSet.get(i);
            Rectangle b = g.getBounds();
            int dx =b.x%gridSpacing;
            int dy =b.y%gridSpacing;
            g.moveBy(-dx, -dy);
       }
        repaint();
    }


    /**



    /**
     * _more_
     *
     * @param g
     * @param l
     * @return _more_
     */
    public List doMakeMenuItems(final Glyph g, List l) {
        l = new ArrayList();
        JMenuItem mi;



        JMenu     colorMenu = new JMenu("Set Color");
        l.add(colorMenu);
        if (colorList == null) {
            initColors();
        }
        for (int i = 0; i < colorList.size(); i++) {
            TwoFacedObject colorTFO = (TwoFacedObject) colorList.get(i);
            colorMenu.add(mi = new JMenuItem(colorTFO.toString()));
            mi.addActionListener(new ObjectListener(colorTFO.getId()) {
                public void actionPerformed(ActionEvent ae) {
                    setColor(g, true, (Color) theObject);
                }
            });
        }
        mi = new JMenuItem("Custom...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setColor(g, true);
            }
        });
        colorMenu.add(mi);
        l.add(GuiUtils.MENU_SEPARATOR);
        l.add(GuiUtils.makeMenuItem("To Front", this, "moveToFront", g));
        l.add(GuiUtils.makeMenuItem("To Back", this, "moveToBack", g));
        l.add(GuiUtils.MENU_SEPARATOR);
        l.add(GuiUtils.makeMenuItem("Delete", this, "removeGlyph", g));
        l.add(GuiUtils.MENU_SEPARATOR);
        return l;
    }

    /**
     * Set the color of the glyph from a color chooser dialog.
     * @param g           <code>Glyph</code> to color.
     * @param foreground  true if the color is for the foreground
     */
    public void setColor(Glyph g, boolean foreground) {
        setColor(g, foreground, null);
    }


    /**
     * Set the color of the glyph from a color chooser dialog.
     * @param g           <code>Glyph</code> to color.
     * @param foreground  true if the color is for the foreground
     * @param newColor    new <code>Color</code> for this <code>Glyph</code>
     */
    public void setColor(Glyph g, boolean foreground, Color newColor) {
        if (newColor == null) {
            newColor = JColorChooser.showDialog(this, "Choose " + (foreground
                    ? "foreground"
                    : "background") + " color", (foreground
                    ? g.getForeground()
                    : g.getBackground()));
        }
        if (newColor != null) {
            if (foreground) {
                g.setForeground(newColor);
            } else {
                g.setBackground(newColor);
            }
            repaint(g);
        }
    }

    /**
     * Handle mouse click events.
     * @param event <code>MouseEvent</code>
     */
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() > 1) {
            return;
        }
        requestFocus();
        if ( !SwingUtilities.isRightMouseButton(event)) {
            return;
        }
        int   x       = transformInputX(event.getX());
        int   y       = transformInputY(event.getY());
        Glyph closest = findGlyph(glyphs, x, y, 10.0);
        if (closest == null) {
            return;
        }
        List menuItems = doMakeMenuItems(closest, new ArrayList());
        if ((menuItems == null) || (menuItems.size() == 0)) {
            return;
        }
        JPopupMenu popup = new JPopupMenu();
        for (int i = 0; i < menuItems.size(); i++) {
            Object item = menuItems.get(i);
            if (item instanceof String) {
                popup.addSeparator();
            } else if (item instanceof Component) {
                popup.add((Component) item);
            }
        }
        popup.show(this, event.getX(), event.getY());
    }

    /**
     * Handle mouse entered events. NO-OP in this implementation.
     * @param e <code>MouseEvent</code>
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Handle mouse exited events. NO-OP in this implementation.
     * @param e  <code>MouseEvent</code>
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * Handle mouse moved events.
     * @param e <code>MouseEvent</code>
     */
    public void mouseMoved(MouseEvent e) {
        int x = transformInputX(e.getX());
        int y = transformInputY(e.getY());
        mousex = x;
        mousey = y;

        if (currentCommand != null) {
            return;
        }

        //We highlight the nearest glyph - only repaint what is neccessary
        Glyph   closestGlyph  = findGlyph(glyphs, x, y, 10.0);
        Glyph   lastHighlight = highlightedGlyph;
        boolean hadUrl        = (currentUrl != null);
        currentUrl = null;

        if (hadUrl && (currentUrl == null)) {
            setCursor(DisplayCanvas.DEFAULT_CURSOR);
        } else if ( !hadUrl && (currentUrl != null)) {
            setCursor(DisplayCanvas.HAND_CURSOR);
        }

        if (closestGlyph != lastHighlight) {
            if (highlightedGlyph != null) {
                repaint(highlightedGlyph);
            }
            setHighlight(closestGlyph);
            if (highlightedGlyph != null) {
                repaint(highlightedGlyph);
            }
        }
    }



    /** _more_ */
    CanvasCommand dragCommand;


    /**
     * _more_
     *
     * @return _more_
     */
    public List getShapeDescriptors() {
        return null;
    }

    /** _more_ */
    JToggleButton selectButton;

    /** _more_ */
    private JMenuBar menuBar;


    /**
     * _more_
     *
     * @return _more_
     */
    public JMenuBar getMenuBar() {
        if (menuBar == null) {
            menuBar = new JMenuBar();
            initMenuBar(menuBar);
        }
        return menuBar;
    }

    /**
     * _more_
     * @return _more_
     */
    protected Component doMakeContents() {
        JPanel labelComp = GuiUtils.inset(getLabelComponent(),
                                          new Insets(5, 5, 5, 5));
        JPanel toolbarPanel =
            GuiUtils.leftCenterRight(GuiUtils.left(doMakeToolbar(null)),
                                     new JLabel("            "), labelComp);

        return GuiUtils.topCenter(
            toolbarPanel,
            GuiUtils.leftCenter(GuiUtils.inset(doMakePalette(), 2), this));
    }

    /**
     * _more_
     * @return _more_
     */
    protected JComponent getLabelComponent() {
        return new JPanel();
    }


    /**
     * _more_
     *
     * @param menuBar
     */
    public void initMenuBar(JMenuBar menuBar) {
        menuBar.add(makeEditMenu());
        menuBar.add(makeViewMenu());
    }

    /**
     * _more_
     *
     * @param pressed
     */
    public void togglePressed(JToggleButton pressed) {
        for (int i = 0; i < shapeGroup.size(); i++) {
            JToggleButton tb = (JToggleButton) shapeGroup.get(i);
            if (tb != pressed) {
                tb.setSelected(false);
            }
        }
    }


    /**
     * _more_
     *
     * @param toolbar
     * @return _more_
     */
    public JToolBar doMakeToolbar(JToolBar toolbar) {
        if (toolbar == null) {
            toolbar = new JToolBar();
        }
        String[] cmds = {
            CMD_ALIGN_TOP, CMD_ALIGN_CENTER, CMD_ALIGN_BOTTOM, null,
            CMD_ALIGN_LEFT, CMD_ALIGN_MIDDLE, CMD_ALIGN_RIGHT,CMD_SPACE_H,CMD_SPACE_V,CMD_SNAP
        };
        String[] icons = {
            "aligntop.gif", "aligncenter.gif", "alignbottom.gif", "",
            "alignleft.gif", "alignmiddle.gif", "alignright.gif","spaceh.gif","spacev.gif","snaptogrid.gif"
        };
        String[] tips = {
            "Align top", "Align center", "Align bottom", "", "Align left",
            "Align middle", "Align right", "Space Horizontally",
            "Space Vertically","Snap to Grid"
        };


        String ip = "/auxdata/ui/icons/";
        for (int i = 0; i < cmds.length; i++) {
            if (cmds[i] == null) {
                toolbar.add(new JLabel("  "));
            } else {
                JButton b = GuiUtils.getImageButton(GuiUtils.getImageIcon(ip
                                + icons[i], getClass()), 3);
                b.setActionCommand(cmds[i]);
                b.addActionListener(this);
                b.setToolTipText(tips[i]);
                toolbar.add(b);
            }
        }


        return toolbar;
    }


    /**
     *  Create the Glyph creation palette
     * @return _more_
     */

    public Component doMakePalette() {
        List shapeDescriptors = getShapeDescriptors();
        if (shapeDescriptors == null) {
            return null;
        }
        List shapeButtons = new ArrayList();
	Border bborder = BorderFactory.createEmptyBorder(2,2,2,2);
        if (showTextInPalette()) {
            ImageIcon image =
                GuiUtils.getImageIcon(
                    "/ucar/unidata/ui/drawing/images/pointer.gif");
            selectButton = new JToggleButton("Select", image);
	    if(!GuiUtils.isMac()) {
		selectButton.setBorder(bborder);
	    }
        } else {
            selectButton = GuiUtils.getToggleButton(
                "/ucar/unidata/ui/drawing/images/pointer.gif", 4, 4);
        }
        Font nf = selectButton.getFont().deriveFont(10.0f);
        selectButton.setFont(nf);
        selectButton.setToolTipText("Select");
        selectButton.setHorizontalAlignment(SwingConstants.LEFT);
        shapeGroup.add(selectButton);
        shapeButtons.add(selectButton);


        for (int i = 0; i < shapeDescriptors.size(); i++) {
            ShapeDescriptor sd = (ShapeDescriptor) shapeDescriptors.get(i);
            JToggleButton   tb;
            if (showTextInPalette()) {
                ImageIcon image = GuiUtils.getImageIcon(sd.iconName);
                tb = new JToggleButton(sd.name, image);
		if(!GuiUtils.isMac()) {
		    tb.setBorder(bborder);
		}
            } else {
                tb = GuiUtils.getToggleButton(sd.iconName, 4, 4);
            }

            tb.setHorizontalAlignment(SwingConstants.LEFT);
            tb.setToolTipText(sd.name);
            tb.setFont(nf);
            shapeGroup.add(tb);
            shapeButtons.add(tb);
            tb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    togglePressed((JToggleButton) ae.getSource());
                }
            });
        }

        JComponent buttons = GuiUtils.vbox(shapeButtons);
	JScrollPane sp = GuiUtils.makeScrollPane(GuiUtils.top(buttons),175,300);
	sp.setPreferredSize(new Dimension(175,300));
        return sp;
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean showTextInPalette() {
        return false;
    }

    /**
     * _more_
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
        requestFocus();
        dragCommand = null;
        int x = transformInputX(e.getX());
        int y = transformInputY(e.getY());


        //Route current currentCommand
        if (currentCommand != null) {
            setCommand(currentCommand.doMousePressed(e, x, y));
            repaint();
            return;
        }

        //If right mouse then do nothing
        if (e.isMetaDown()) {
            return;
        }

        if (mousePressedInner(e)) {
            return;
        }


        //Are we near a glyph?
        Glyph nearestGlyph = (highlightedGlyph != null)
                             ? highlightedGlyph
                             : findGlyph(x, y);

        //If not then drag out a selection rectangle
        if (nearestGlyph == null) {
            clearSelection();
            setCommand(new DragRectCommand(this, e, x, y));
            return;
        }

        //We clicked on a glyph - dink  around with the selection set
        boolean alreadySelected      = isSelected(nearestGlyph);
        boolean shouldClearSelection = !e.isShiftDown() && !e.isControlDown();
        if ( !alreadySelected) {
            if (shouldClearSelection) {
                clearSelection();
            }
            addSelection(nearestGlyph);
        } else {
            if (e.isShiftDown()) {
                removeSelection(nearestGlyph);
            }
        }
        if (e.getClickCount() > 1) {
            doDoubleClick(nearestGlyph);
            return;
        }
        dragCommand = nearestGlyph.getMoveCommand(this, e, x, y);
    }

    /**
     * _more_
     *
     * @param nearestGlyph _more_
     */
    protected void doDoubleClick(Glyph nearestGlyph) {}

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public boolean mousePressedInner(MouseEvent e) {
        int x   = transformInputX(e.getX());
        int y   = transformInputY(e.getY());
        int idx = -1;
        for (int i = 0; (idx < 0) && (i < shapeGroup.size()); i++) {
            JToggleButton tb = (JToggleButton) shapeGroup.get(i);
            if (tb.isSelected()) {
                idx = i;
            }
        }

        if (idx <= 0) {
            return false;
        }

        idx--;
        if ( !selectionSticky) {
            selectButton.setSelected(true);
            togglePressed(selectButton);
        }

        List            shapeDescriptors = getShapeDescriptors();
        ShapeDescriptor sd = (ShapeDescriptor) shapeDescriptors.get(idx);
        Glyph           glyph            = createGlyph(sd.className, snap(x), snap(y));
        if (glyph != null) {
            sd.initializeGlyph(glyph);
            glyph.initDone();
            glyph.setId(getGlyphId());
            addGlyph(glyph);
            clearSelection();
            setCommand(glyph.getCreateCommand(this, e, x, y));
        }
        return true;
    }


    /**
     * _more_
     *
     * @param e
     */
    public void mouseDragged(MouseEvent e) {
        setHighlight(null);

        if (dragCommand != null) {
            setCommand(dragCommand);
            dragCommand = null;
        }

        int x = transformInputX(e.getX());
        int y = transformInputY(e.getY());

        //If we have  a command then route to it.
        if (currentCommand != null) {
            mouseWasPressed = false;
            //      scrollToPoint (x, y);
            setCommand(currentCommand.doMouseDragged(e, x, y));
            return;
        }
    }


    /**
     * _more_
     *
     * @param root
     */
    public void loadXml(Element root) {
        NodeList glyphs = XmlUtil.getElements(root, Glyph.TAG_GLYPH);
        for (int i = 0; i < glyphs.getLength(); i++) {
            Element child = (Element) glyphs.item(i);

        }
    }


    /**
     * _more_
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() > 1) {
            return;
        }
        int x = transformInputX(e.getX());
        int y = transformInputY(e.getY());
        mouseWasPressed = false;
        if (currentCommand != null) {
            setCommand(currentCommand.doMouseReleased(e, x, y));
        }
    }

    /**
     * _more_
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
     * _more_
     *
     * @param className _more_
     * @param name _more_
     * @param iconName _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public ShapeDescriptor makeDescriptor(String className, String name,
                                          String iconName, String attrs) {
        return new ShapeDescriptor(className, name, iconName, attrs);
    }

    /**
     * Class ShapeDescriptor _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.31 $
     */
    public static class ShapeDescriptor {

        /** _more_ */
        String className;

        /** _more_ */
        String name;

        /** _more_ */
        String iconName;

        /** _more_ */
        String attrs;


        /**
         * _more_
         *
         * @param className _more_
         * @param name _more_
         * @param iconName _more_
         * @param attrs _more_
         */
        public ShapeDescriptor(String className, String name,
                               String iconName, String attrs) {
            this.className = className;
            this.name      = name;
            this.iconName  = iconName;
            this.attrs     = attrs;
        }

        /**
         * _more_
         *
         * @param g _more_
         */
        public void initializeGlyph(Glyph g) {
            g.processAttrs(attrs);
        }


    }



}

