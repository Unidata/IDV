/**
 * $Id: Timeline.java,v 1.36 2007/08/16 14:09:56 jeffmc Exp $
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






package ucar.unidata.ui;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedThing;

import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;



import java.io.*;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Widget for selecting dates and times
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.36 $
 */
public class Timeline extends JPanel implements MouseListener,
        MouseMotionListener {

    /** Action command used for the Apply button */
    public static String CMD_APPLY = "Apply";

    /** Action command used for the Cancel button */
    public static String CMD_CANCEL = "Cancel";


    /** Action command used for the Cancel button */
    public static String CMD_OK = "Ok";


    /** The normal cursor_ */
    public static final Cursor CURSOR_NORMAL =
        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);


    /** The normal cursor_ */
    public static final Cursor CURSOR_MOVE =
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    /** The normal cursor_ */
    public static final Cursor CURSOR_LEFT =
        Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

    /** The normal cursor_ */
    public static final Cursor CURSOR_RIGHT =
        Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);


    /** font */
    private static String FONT_FACE = "Monospaced";

    /** font */
    private static final Font FONT_MAIN = new Font(FONT_FACE, Font.BOLD, 10);

    /** font */
    private static final Font FONT_AXIS_MINOR = new Font(FONT_FACE,
                                                    Font.ITALIC, 10);

    /** font */
    private static final Font FONT_AXIS_MAJOR = new Font(FONT_FACE,
                                                    Font.BOLD, 10);

    /** For formatting dates */
    private FieldPosition FP = new FieldPosition(0);

    /** drawing size */
    private static final int DIM_SELECTION_BOX = 10;

    /** drawing size */
    private static final int DIM_TIME_WIDTH = 4;

    /** drawing size */
    public static final int DIM_TIME_HEIGHT = 10;

    /** drawing size */
    private static final int DIM_INTERVAL_HEIGHT = 15;

    /** stroke */
    private static final Stroke STROKE_DASHED_ONEPIXEL =
        new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0.0f, new float[] { 1.0f,
                                            1.0f }, 0.0f);

    /** stroke */
    private static final Stroke STROKE_DASHED_TWOPIXEL =
        new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0.0f, new float[] { 1.0f,
                                            1.0f }, 0.0f);




    /** stroke */
    private static final Stroke STROKE_SOLID_ONEPIXEL =
        new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0.0f, new float[] { Float.MAX_VALUE }, 0.0f);

    /** stroke */
    private static final Stroke STROKE_SOLID_TWOPIXEL =
        new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0.0f, new float[] { Float.MAX_VALUE }, 0.0f);


    /** color */
    private static final Color COLOR_BACKGROUND = Color.white;

    /** color */
    private static final Color COLOR_BACKGROUND_DISABLED = new Color(230,
                                                               230, 230);

    /** color */
    private static final Color COLOR_LINE = Color.black;


    /** color */
    private static final Color COLOR_RANGE = Color.black;

    //    private static final Color COLOR_RANGE_DART = COLOR_RANGE;

    /** _more_ */
    private static final Color COLOR_RANGE_DART = Color.gray;

    /** highlight color for when we drag the date selection */
    private static final Color COLOR_HIGHLIGHT_SELECTION =
        Color.cyan.darker();  //Color.yellow;

    /** highlight color for the date object we're over */
    private static final Color COLOR_HIGHLIGHT_DATE = Color.cyan;  //Color.yellow;

    /** color */
    private static final Color COLOR_TIME_SELECTED = Color.red;

    /** color */
    private static final Color COLOR_TIME_UNSELECTED = Color.blue;

    //    private static final Color COLOR_INTERVAL_FILL =  Color.cyan.brighter().brighter();

    /** color */
    private static final Color COLOR_INTERVAL_FILL = new Color(237, 237, 237);


    /** color */
    private static final Color COLOR_INTERVAL_LINE =
        COLOR_INTERVAL_FILL.darker();

    /** color */
    private static final Color COLOR_AXIS_MINOR = Color.gray;

    /** color */
    private static final Color COLOR_AXIS_MAJOR = Color.black;

    /** How close to a date to pick it */
    private static final int PICK_THRESHOLD = 10;


    /** when dragging the date selection box what are we dragging */
    public static final int DRAGMODE_LEFT = 0;

    /** when dragging the date selection box what are we dragging */
    public static final int DRAGMODE_RIGHT = 1;

    /** when dragging the date selection box what are we dragging */
    public static final int DRAGMODE_CENTER = 2;




    /** formats */
    private static AxisFormat[] FORMATS = {
        new AxisFormat(1 * 100, "ss:SS",
                       new AxisFormat(Calendar.MILLISECOND, 500, "mm:ss:SS")),
        //        new AxisFormat(DateUtil.MILLIS*100, "ss:SS",
        //                       new AxisFormat(Calendar.MILLISECOND, 500,"mm:ss:SS")),
        new AxisFormat(1 * 500, "ss:SS",
                       new AxisFormat(Calendar.SECOND, 2, "mm:ss:SS")),
        new AxisFormat(DateUtil.MILLIS_SECOND, "ss",
                       new AxisFormat(Calendar.SECOND, 10, "HH:mm:ss")),
        new AxisFormat(DateUtil.MILLIS_SECOND * 5, "ss",
                       new AxisFormat(Calendar.SECOND, 30, "HH:mm:ss")),
        new AxisFormat(DateUtil.MILLIS_SECOND * 15, "ss",
                       new AxisFormat(Calendar.MINUTE, 1, "HH:mm:ss")),
        new AxisFormat(DateUtil.MILLIS_SECOND * 30, "ss",
                       new AxisFormat(Calendar.MINUTE, 2, "HH:mm:ss")),
        new AxisFormat(DateUtil.MILLIS_MINUTE, "mm",
                       new AxisFormat(Calendar.MINUTE, 5, "dd/HH:mm")),
        new AxisFormat(DateUtil.MILLIS_MINUTE * 5, "mm",
                       new AxisFormat(Calendar.MINUTE, 30, "dd/HH:mm")),
        new AxisFormat(DateUtil.MILLIS_MINUTE * 10, "m",
                       new AxisFormat(Calendar.MINUTE, 60, "dd/HH:mm")),
        new AxisFormat(DateUtil.MILLIS_MINUTE * 15, "mm",
                       new AxisFormat(Calendar.HOUR, 1, "dd/HH:mm")),
        new AxisFormat(DateUtil.MILLIS_MINUTE * 30, "mm",
                       new AxisFormat(Calendar.HOUR, 2, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 3, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 2, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 6, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 3, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 6, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 4, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 6, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 5, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 12, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 6, "HH:mm",
                       new AxisFormat(Calendar.HOUR, 12, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_HOUR * 12, "HH:mm",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 1, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_DAY, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 1, "dd/HH")),
        new AxisFormat(DateUtil.MILLIS_DAY * 2, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 7, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_DAY * 3, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 14, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_DAY * 4, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 14, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_DAY * 5, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 14, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_DAY * 6, "dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 30, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_DAY * 7, "MM-dd",
                       new AxisFormat(Calendar.DAY_OF_MONTH, 30, "MM-dd")),
        new AxisFormat(DateUtil.MILLIS_MONTH, "MM-dd",
                       new AxisFormat(Calendar.MONTH, 4, "yyyy-MM")),
        new AxisFormat(DateUtil.MILLIS_MONTH * 6, "MM-dd",
                       new AxisFormat(Calendar.YEAR, 2, "yyyy")),
        new AxisFormat(DateUtil.MILLIS_YEAR, "yyyy-MM"),
        new AxisFormat(DateUtil.MILLIS_YEAR * 5, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_YEAR * 10, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_YEAR * 100, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM * 10, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM * 100, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM * 1000, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM * 10000, "yyyy"),
        new AxisFormat(DateUtil.MILLIS_MILLENIUM * 100000, "yyyy")
    };

    /**
     * Class AxisFormat Holds info to do date range size sensitive axis painting
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.36 $
     */
    private static class AxisFormat {

        /** Calendar field */
        int field;

        /** offset */
        int offset;

        /** step */
        long step;

        /** date format */
        String format;

        /** sub-axis info */
        AxisFormat subFormat;

        /**
         * ctor
         *
         * @param field field
         * @param offset offset
         * @param fmt format
         */
        public AxisFormat(int field, int offset, String fmt) {
            this.field  = field;
            this.offset = offset;
            this.format = fmt;
        }


        /**
         * ctor
         *
         * @param step step
         * @param fmt format
         */
        public AxisFormat(long step, String fmt) {
            this(step, fmt, null);
        }


        /**
         * ctor
         *
         * @param step step
         * @param fmt format
         * @param subFormat sub  axis format
         */
        public AxisFormat(long step, String fmt, AxisFormat subFormat) {
            this.step      = step;
            this.format    = fmt;
            this.subFormat = subFormat;
        }

        /**
         * make a date formatter
         *
         * @param tz timezone
         *
         * @return formatter
         */
        SimpleDateFormat getSDF(TimeZone tz) {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            if (tz != null) {
                sdf.setTimeZone(tz);
            }
            return sdf;
        }

    }

    /** Does this timeline support selection */
    private boolean isCapableOfSelection = true;

    /** _more_          */
    private boolean justShowSelected = false;

    /** are we dragging a selection range */
    private boolean doingDragSelect = false;

    /** for dragging */
    private Date dragStartDate;

    /** for dragging */
    private Date dragEndDate;


    /** for dragging */
    private MouseEvent dragStartEvent;


    /** for dragging */
    private boolean draggingDateSelection = false;

    /** date selection rectangle */
    private Rectangle selectionBox;

    /** what are we dragging */
    private int dragMode;

    /** for dragging */
    private long initDragDeltaStart;

    /** for dragging */
    private long initDragDeltaEnd;


    /** for properties dialog */
    private JCheckBox useDateSelectionCbx;

    /** for properties dialog */
    private JCheckBox showIntervalsCbx;

    /** for properties dialog */
    private JCheckBox stickyCbx;

    /** for properties dialog */
    //    private DateTimePicker startTimePicker;

    /** for properties dialog */
    //    private DateTimePicker endTimePicker;

    /** for properties dialog */
    private DateSelectionGui dateSelectionGui;

    /** for properties dialog */
    private JDialog propertiesDialog;

    /** for properties dialog */
    private JDialog dialog;

    /** for properties dialog */
    private boolean dialogOK = false;

    /** the date selection */
    private DateSelection dateSelection = new DateSelection();

    /** are we using the date selection */
    private boolean useDateSelection = true;


    /** first one when we got created. Use this for doing a control-r */
    private DateSelection originalDateSelection;

    /** Do we paint the intervals */
    private boolean paintIntervals = true;


    /** the default time zone */
    private static TimeZone defaultTimeZone;

    /** date format */
    private static String defaultFormat = "yyyy-MM-dd HH:mm:ss";

    /** for painting */
    private int horizontalPadding = 0;

    /** for painting */
    private int verticalPadding = 0;


    /** visible start date */
    private Date startDate;

    /** visible end date */
    private Date endDate;

    /** first start date. Use this for dthe reset */
    private Date originalStartDate;

    /** first end date. Use this for dthe reset */
    private Date originalEndDate;

    /** The thigns we draw */
    private List datedThings;

    /** last thing selected. Keep around for doing a shift-click range select */
    private DatedThing lastSelectedThing;

    /** selected things */
    private List selected = new ArrayList();

    /** thing we're mousing over */
    private DatedThing mouseHighlighted;


    /** gui */
    private JComponent contents;

    /** show intervals */
    private boolean showIntervals = true;


    /** Is the date selection range automatically changed to match the visible range */
    private boolean sticky = false;

    /** _more_          */
    protected List sunriseDates = new ArrayList();

    /** Holds other timelines that we share start/end range with */
    private List timelineGroup;


    /** do we show an abbreviated display */
    private boolean shortDisplay = false;

    /** color */
    private Color colorTimeSelected = COLOR_TIME_SELECTED;

    /** color */
    private Color colorTimeUnselected = COLOR_TIME_UNSELECTED;

    /**
     * Default  ctor
     */
    public Timeline() {
        this(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24),
             new Date(System.currentTimeMillis()));
    }

    /**
     * ctor. start and end date is the min/max of the times
     *
     * @param times List of DatedThings
     * @param initDimension initial width
     */
    public Timeline(List times, int initDimension) {
        this(times, initDimension, 100);
    }


    /**
     * ctor. start and end date is the min/max of the times
     *
     * @param times List of DatedThings
     * @param width init width
     * @param height init height
     */
    public Timeline(List times, int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setDatedThings(times, true);
        initGui();
    }


    /**
     * Create a Timeline with the initial date range
     *
     * @param start start
     * @param end end
     */
    public Timeline(Date start, Date end) {
        this(start, end, 600);
    }


    /**
     * ctor
     *
     * @param start start
     * @param end end
     * @param initDimension width
     */
    public Timeline(Date start, Date end, int initDimension) {
        setPreferredSize(new Dimension(initDimension, 100));
        init(start, end);
        initGui();
    }


    /**
     * get default time zone
     *
     * @return time zone
     */
    public static TimeZone getTimeZone() {
        if (defaultTimeZone == null) {
            defaultTimeZone = TimeZone.getTimeZone("GMT");
        }
        return defaultTimeZone;
    }


    /**
     * set the format
     *
     * @param format format
     */
    public static void setDateFormat(String format) {
        defaultFormat = format;
    }

    /**
     * set timezone
     *
     * @param tz timezone
     */
    public static void setTimeZone(TimeZone tz) {
        defaultTimeZone = tz;
    }


    /**
     * make gui
     */
    protected void initGui() {

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                double smallStep = 0.05;
                double bigStep   = 0.5;
                boolean doDateSelection = e.isControlDown()
                                          && (dateSelectionActive());
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    shiftByPercent((e.isShiftDown()
                                    ? bigStep
                                    : smallStep), doDateSelection);
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    shiftByPercent((e.isShiftDown()
                                    ? -bigStep
                                    : -smallStep), doDateSelection);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    expandByPercent((e.isShiftDown()
                                     ? 0.5
                                     : 0.9), doDateSelection);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    expandByPercent((e.isShiftDown()
                                     ? 1.5
                                     : 1.1), doDateSelection);
                } else if ((e.getKeyCode() == KeyEvent.VK_R)) {
                    reset(e.isControlDown());
                } else if ((e.getKeyCode() == KeyEvent.VK_P)
                           && e.isControlDown()) {
                    showProperties();
                } else if ( !dateSelectionActive()) {
                    if (isCapableOfSelection
                            && (e.getKeyCode() == KeyEvent.VK_A)
                            && e.isControlDown()) {
                        setSelected(new ArrayList(datedThings));
                        repaint();
                    }
                }
            }
        });

        this.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent event) {
                boolean doDateSelection = event.isControlDown()
                                          && dateSelectionActive();
                int notches = event.getWheelRotation();
                if (notches < 0) {
                    expandByPercent((event.isShiftDown()
                                     ? 0.5
                                     : 0.9), doDateSelection);
                } else {
                    expandByPercent((event.isShiftDown()
                                     ? 1.5
                                     : 1.1), doDateSelection);
                }
            }
        });

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        setToolTipText("");
    }


    /**
     * handle mouse event
     *
     * @param me mouse event
     */
    public void mouseMoved(MouseEvent me) {
        if ((startDate == null) || (endDate == null)) {
            return;
        }
        DatedThing closest = findClosest(new Point(me.getX(), me.getY()));
        boolean    changed = (closest != mouseHighlighted);
        setHighlightedDate(closest);
        setCursor(CURSOR_NORMAL);
        if ((selectionBox != null) && dateSelectionActive()) {
            Rectangle r = new Rectangle(selectionBox);
            r.grow(PICK_THRESHOLD, PICK_THRESHOLD);
            if (r.contains(new Point(me.getX(), me.getY()))) {
                double d1 = Math.abs(selectionBox.getX() - me.getX());
                double d2 = Math.abs(selectionBox.getX()
                                     + selectionBox.getWidth() - me.getX());
                if ((d1 > 10) && (d2 > 10)) {
                    setCursor(CURSOR_MOVE);
                } else if (d1 < d2) {
                    setCursor(CURSOR_LEFT);
                } else {
                    setCursor(CURSOR_RIGHT);
                }
            }
        }
        if (changed) {
            repaint();
        }
    }


    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mouseDragged(MouseEvent e) {
        if ((startDate == null) || (endDate == null)) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            return;
        }
        if (doingDragSelect) {
            if (e.isShiftDown()) {
                dragEndDate = toDate(e.getX());
                int w = getWidth();
                int x = e.getX();
                if (x > w) {
                    shiftByPercent(1.2 * (x - w) / (double) w, false);
                } else if (x < 0) {
                    shiftByPercent(1.2 * x / (double) w, false);
                }
                setSelected(DatedObject.select(dragStartDate, dragEndDate,
                        datedThings));
                repaint();
                return;
            } else {
                doingDragSelect = false;
            }
        }


        int    mouseLoc = e.getX();
        int    left     = toLocation(startDate.getTime());
        int    right    = toLocation(endDate.getTime());
        int    delta    = mouseLoc - dragStartEvent.getX();
        double percent  = delta / ((double) right - left);
        //                System.err.println("percent:" +  percent);
        if (draggingDateSelection) {
            Date   d       = toDate(mouseLoc);
            long   newTime = d.getTime();
            double d1      = Math.abs(selectionBox.getX() - mouseLoc);
            double d2 = Math.abs(selectionBox.getX()
                                 + selectionBox.getWidth() - mouseLoc);

            if (dragMode == DRAGMODE_CENTER) {
                dateSelection.setStartFixedTime(newTime + initDragDeltaStart);
                dateSelection.setEndFixedTime(newTime + initDragDeltaEnd);
            } else {
                if ((dragMode == DRAGMODE_LEFT)
                        && (mouseLoc
                            >= selectionBox.getX()
                               + selectionBox.getWidth())) {
                    dragMode = DRAGMODE_RIGHT;
                } else {
                    if ((dragMode == DRAGMODE_RIGHT)
                            && (mouseLoc <= selectionBox.getX())) {
                        dragMode = DRAGMODE_LEFT;
                    }
                }

                if (dragMode == DRAGMODE_LEFT) {
                    dateSelection.setStartFixedTime(newTime);
                } else {
                    dateSelection.setEndFixedTime(newTime);
                }
            }
            if (mouseLoc < 0) {
                long diff = startDate.getTime() - d.getTime();
                startDate = d;
                endDate   = new Date(endDate.getTime() - diff);
                timelineChanged();
            } else if (mouseLoc > getBounds().width) {
                long diff = d.getTime() - endDate.getTime();
                endDate   = d;
                startDate = new Date(startDate.getTime() + diff);
                timelineChanged();
            }
            dateSelectionChanged();
        } else {
            shiftByPercent(-percent, false);
        }
        dragStartEvent = e;
    }



    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mouseExited(MouseEvent e) {
        if (mouseHighlighted != null) {
            setHighlightedDate(null);
            repaint();
        }
    }

    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mouseClicked(MouseEvent e) {
        if ((startDate == null) || (endDate == null)) {
            return;
        }
        if (e.getClickCount() > 1) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            showPopupMenu(e.getX(), e.getY());
            return;
        }
        //If we arent in date selection mode then we are in direct selection mode
        if (dateSelectionActive()) {
            //For now don't do this
            //            return;
        }
        if ( !isCapableOfSelection) {
            return;
        }

        if ( !e.isShiftDown() && !e.isControlDown()) {
            setSelected(new ArrayList());
        }

        DatedThing closest = findClosest(new Point(e.getX(), e.getY()));
        if (closest != null) {
            if (e.isShiftDown() && (lastSelectedThing != null)) {
                long t1 = lastSelectedThing.getDate().getTime();
                long t2 = closest.getDate().getTime();
                if (t1 > t2) {
                    long tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                for (int i = 0; i < datedThings.size(); i++) {
                    DatedThing datedThing = (DatedThing) datedThings.get(i);
                    long       time       = datedThing.getDate().getTime();
                    if ((time >= t1) && (time <= t2)
                            && !selected.contains(datedThing)) {
                        selected.add(datedThing);
                        selectedDatesChanged();
                    }
                }
            } else {
                if (selected.contains(closest)) {
                    selected.remove(closest);
                } else {
                    selected.add(closest);
                }
                selectedDatesChanged();
            }

        }
        lastSelectedThing = closest;
        repaint();
    }

    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mousePressed(MouseEvent e) {
        if ((startDate == null) || (endDate == null)) {
            return;
        }
        requestFocus();
        if (SwingUtilities.isRightMouseButton(e)) {
            return;
        }
        draggingDateSelection = false;
        dragStartEvent        = e;
        doingDragSelect       = false;
        if (isCapableOfSelection && !dateSelectionActive()
                && e.isShiftDown()) {
            doingDragSelect = true;
            dragStartDate   = dragEndDate = toDate(e.getX());
            setSelected(new ArrayList());
            repaint();
            return;
        }


        if ( !dateSelectionActive()) {
            //            return;
        }
        if (selectionBox == null) {
            return;
        }
        Rectangle r = new Rectangle(selectionBox);
        r.grow(PICK_THRESHOLD, PICK_THRESHOLD);
        if (r.contains(new Point(e.getX(), e.getY()))) {
            draggingDateSelection = true;
            double d1 = Math.abs(selectionBox.getX() - e.getX());
            double d2 = Math.abs(selectionBox.getX()
                                 + selectionBox.getWidth() - e.getX());
            if ((d1 > 10) && (d2 > 10)) {
                dragMode = DRAGMODE_CENTER;
            } else if (d1 < d2) {
                dragMode = DRAGMODE_LEFT;
            } else {
                dragMode = DRAGMODE_RIGHT;
            }
            long time = toDate(e.getX()).getTime();
            initDragDeltaStart = dateSelection.getStartFixedTime() - time;
            initDragDeltaEnd   = dateSelection.getEndFixedTime() - time;
        }
    }

    /**
     * handle mouse event
     *
     * @param e mouse event
     */
    public void mouseReleased(MouseEvent e) {
        setCursor(CURSOR_NORMAL);
        dragStartEvent  = null;
        dragStartDate   = null;
        dragEndDate     = null;
        doingDragSelect = false;
        if ((startDate == null) || (endDate == null)) {
            return;
        }
        draggingDateSelection = false;
        repaint();
    }



    /**
     * Set the date selection
     *
     * @param dateSelection The date selection
     */
    public void setDateSelection(DateSelection dateSelection) {
        this.dateSelection    = dateSelection;
        originalDateSelection = new DateSelection(dateSelection);
        dateSelectionChanged();
    }

    /**
     * Get the DateSelection property.
     *
     * @return The DateSelection
     */
    public DateSelection getDateSelection() {
        return dateSelection;
    }


    /**
     * Get the list of dated things we are displaying
     *
     * @return List of dated things
     */
    public List getDatedThings() {
        return datedThings;
    }

    /**
     * Set the list of selected items
     *
     * @param l List of selected items
     */
    public void setSelected(List l) {
        if (l == null) {
            selected = new ArrayList();
        } else {
            selected = new ArrayList(l);
        }
        selectedDatesChanged();
        repaint();
    }

    /** _more_          */
    private Hashtable selectedMap = new Hashtable();

    /**
     * _more_
     */
    public void selectedDatesChanged() {
        selectedMap = new Hashtable();
        if ((datedThings == null) || (selected == null)) {
            return;
        }
        for (int i = 0; i < selected.size(); i++) {
            DatedThing datedThing = (DatedThing) selected.get(i);
            selectedMap.put(datedThing, datedThing);
        }
    }



    /**
     * Get the list of selected DatedThing-s
     *
     * @return The Selected things
     */
    public List getSelected() {
        return new ArrayList(selected);
    }

    /**
     * initialize start/end time
     *
     * @param start start
     * @param end end
     */
    protected void init(Date start, Date end) {
        this.startDate         = start;
        this.endDate           = end;
        this.originalStartDate = start;
        this.originalEndDate   = end;
    }

    /**
     * popup menu at
     *
     * @param x x
     * @param y y
     */
    protected void showPopupMenu(int x, int y) {
        List items = new ArrayList();
        getMenuItems(items);
        JPopupMenu popup = MenuUtil.makePopupMenu(items);
        popup.show(this, x, y);
    }

    /**
     * reset to original date range
     */
    public void resetDateRange() {
        reset(false);
    }

    /**
     * reset to original date selection
     */
    public void resetDateSelection() {
        reset(true);
    }

    /**
     * set the range of the date selection
     *
     * @param r range
     */
    public void setDateSelection(Date[] r) {
        dateSelection.setStartFixedTime(r[0]);
        dateSelection.setEndFixedTime(r[1]);
        startDate = r[0];
        endDate   = r[1];
        dateSelectionChanged();
        expandByPercent(1.1, false);
    }


    /**
     * set the visible range
     *
     * @param r The date range
     */
    public void setVisibleRange(Date[] r) {
        startDate = r[0];
        endDate   = r[1];
        expandByPercent(1.1, false);
        //        timelineChanged();
    }


    /**
     * make menu items
     *
     * @param items items
     */
    protected void getMenuItems(List items) {

        List      subItems;
        JMenuItem mi;

        items.add(mi = MenuUtil.makeMenuItem("Properties", this,
                                             "showProperties"));

        subItems = new ArrayList();
        long     now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(getTimeZone());
        cal.setTimeInMillis(now);
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        now = cal.getTimeInMillis();

        subItems.add(MenuUtil.makeMenuItem("Reset", this, "resetDateRange"));

        subItems.add(
            MenuUtil.makeMenuItem(
                "Today", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(1)),
                             new Date(now) }));
        subItems.add(
            MenuUtil.makeMenuItem(
                "Past Week", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(7)),
                             new Date(now) }));
        subItems.add(
            MenuUtil.makeMenuItem(
                "Past Month", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(30)),
                             new Date(now) }));
        subItems.add(
            MenuUtil.makeMenuItem(
                "Past Year", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(365)),
                             new Date(now) }));

        if (dateSelectionActive()) {
            subItems.add(MenuUtil.makeMenuItem("Selection Range", this,
                    "setVisibleRange",
                    new Date[] { dateSelection.getStartFixedDate(),
                                 dateSelection.getEndFixedDate() }));
        }
        items.add(MenuUtil.makeMenu("Set Visible Range", subItems));


        if (dateSelectionActive()) {
            subItems = new ArrayList();
            subItems.add(MenuUtil.makeMenuItem("View", this,
                    "setVisibleRange",
                    new Date[] { dateSelection.getStartFixedDate(),
                                 dateSelection.getEndFixedDate() }));
            subItems.add(MenuUtil.makeMenuItem("Reset", this,
                    "resetDateSelection"));

            subItems.add(MenuUtil.makeMenuItem("Today", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(1)),
                                 new Date(now) }));
            subItems.add(MenuUtil.makeMenuItem("Past Week", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(7)),
                                 new Date(now) }));
            subItems.add(MenuUtil.makeMenuItem("Past Month", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(30)),
                                 new Date(now) }));
            items.add(MenuUtil.makeMenu("Set Date Selection", subItems));
        }


        if (isCapableOfSelection) {
            items.add(MenuUtil.makeCheckboxMenuItem("Use Date Selection",
                    this, "useDateSelection", null));
        }
        if ( !dateSelectionActive()) {
            return;
        }

        items.add(MenuUtil.makeCheckboxMenuItem("Use Visible Range", this,
                "sticky", null));
        mi.setToolTipText("Make the selection range be the visible range");




        double[] intervals = {
            Double.NaN, 0, DateUtil.minutesToMillis(5),
            DateUtil.minutesToMillis(10), DateUtil.minutesToMillis(15),
            DateUtil.minutesToMillis(30), DateUtil.hoursToMillis(1),
            DateUtil.hoursToMillis(2), DateUtil.hoursToMillis(3),
            DateUtil.hoursToMillis(4), DateUtil.hoursToMillis(5),
            DateUtil.hoursToMillis(6), DateUtil.hoursToMillis(12),
            DateUtil.daysToMillis(1), DateUtil.daysToMillis(2),
            DateUtil.daysToMillis(7)
        };
        String[] intervalNames = {
            "Default", "0 minutes", "5 minutes", "10 minutes", "15 minutes",
            "30 minutes", "1 hour", "2 hours", "3 hours", "4 hours",
            "5 hours", "6 hours", "12 hours", "1 day", "2 days", "7 days",
        };

        subItems = new ArrayList();
        double currentInterval = dateSelection.getInterval();
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] != intervals[i]) {
                continue;
            }
            String lbl = intervalNames[i];
            if (intervals[i] == 0) {
                lbl = "None";
            }
            subItems.add(MenuUtil.makeMenuItem(((intervals[i]
                    == currentInterval)
                    ? "-" + lbl + "-"
                    : " " + lbl + " "), this, "setInterval",
                                        new Double(intervals[i])));

        }

        items.add(MenuUtil.makeMenu("Interval", subItems));
        if (dateSelection.hasInterval()) {
            subItems = new ArrayList();
            double range;
            range = dateSelection.getPreRange();
            for (int i = 0; i < intervals.length; i++) {
                boolean isCurrent = intervals[i] == range;
                if ((range != range) && (intervals[i] != intervals[i])) {
                    isCurrent = true;
                }
                String lbl = intervalNames[i];
                subItems.add(MenuUtil.makeMenuItem((isCurrent
                        ? "-" + lbl + "-"
                        : " " + lbl + " "), this, "setPreRange",
                                            new Double(intervals[i])));

            }
            items.add(MenuUtil.makeMenu("Before Range", subItems));

            subItems = new ArrayList();
            range    = dateSelection.getPostRange();
            for (int i = 0; i < intervals.length; i++) {
                boolean isCurrent = intervals[i] == range;
                if ((range != range) && (intervals[i] != intervals[i])) {
                    isCurrent = true;
                }
                String lbl = intervalNames[i];
                subItems.add(MenuUtil.makeMenuItem((isCurrent
                        ? "-" + lbl + "-"
                        : " " + lbl + " "), this, "setPostRange",
                                            new Double(intervals[i])));

            }
            items.add(MenuUtil.makeMenu("After Range", subItems));
        }





        subItems = new ArrayList();
        int   currentSkip = dateSelection.getSkip();
        int[] skips       = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100
        };
        for (int i = 0; i < skips.length; i++) {
            subItems.add(MenuUtil.makeMenuItem(((skips[i] == currentSkip)
                    ? "-" + skips[i] + "-"
                    : " " + skips[i] + " "), this, "setSkipFactor",
                                             new Integer(skips[i])));
        }
        items.add(MenuUtil.makeMenu("Skip Factor", subItems));


        subItems = new ArrayList();
        int[] counts = {
            DateSelection.MAX_COUNT, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20,
            25, 30, 40, 50, 75, 100
        };
        int currentCount = dateSelection.getCount();
        for (int i = 0; i < counts.length; i++) {
            int    cnt = counts[i];
            String lbl;
            if (cnt == DateSelection.MAX_COUNT) {
                lbl = "All";
            } else {
                lbl = "" + cnt;
            }
            subItems.add(MenuUtil.makeMenuItem(((cnt == currentCount)
                    ? "-" + lbl + "-"
                    : " " + lbl + " "), this, "setCount", new Integer(cnt)));
        }
        items.add(MenuUtil.makeMenu("Count", subItems));

    }


    /**
     * clear intervals
     */
    public void removeIntervals() {
        dateSelection.setInterval(0);
        dateSelectionChanged();
    }

    /**
     * set skip
     *
     * @param i skip
     */
    public void setSkipFactor(Integer i) {
        dateSelection.setSkip(i.intValue());
        dateSelectionChanged();
    }


    /**
     * set interval
     *
     * @param i interval
     */
    public void setInterval(Double i) {
        dateSelection.setInterval(i.doubleValue());
        dateSelectionChanged();
    }

    /**
     * set pre range
     *
     * @param i range
     */
    public void setPreRange(Double i) {
        dateSelection.setPreRange(i.doubleValue());
        dateSelectionChanged();
    }

    /**
     * set post range
     *
     * @param i range_
     */
    public void setPostRange(Double i) {
        dateSelection.setPostRange(i.doubleValue());
        dateSelectionChanged();
    }

    /**
     * set count
     *
     * @param i count
     */
    public void setCount(Integer i) {
        dateSelection.setCount(i.intValue());
        dateSelectionChanged();
    }




    /**
     * show properties dialog
     */
    public void showProperties() {
        if (propertiesDialog == null) {
            useDateSelectionCbx = new JCheckBox("Use Date Selection:",
                    useDateSelection);
            showIntervalsCbx = new JCheckBox("Show Intervals", showIntervals);
            stickyCbx        = new JCheckBox("Use Visible Range", sticky);
            //            startTimePicker  = new DateTimePicker(getStartDate());
            //            endTimePicker    = new DateTimePicker(getEndDate());
            dateSelectionGui = new DateSelectionGui(dateSelection);

            JComponent timeRangePanel = new JPanel();

            /*            JComponent timeRangePanel =
                LayoutUtil.hbox(GuiUtils.rLabel("Time Range: "),
                              startTimePicker, new JLabel("  --  "),
                              endTimePicker);*/
            JComponent flagsPanel = LayoutUtil.hbox(stickyCbx,
                                        showIntervalsCbx);


            JComponent contents;

            if(isCapableOfSelection) {
                contents = LayoutUtil.vbox(timeRangePanel,
                                           LayoutUtil.filler(10, 10),
                                           useDateSelectionCbx,
                                           LayoutUtil.inset(dateSelectionGui,
                                                            new Insets(0, 20, 0, 0)));

            } else {
                contents = timeRangePanel;
            }
            contents = LayoutUtil.inset(contents, 5);

            propertiesDialog = new JDialog((JFrame) null, "Date Selection",
                                           true);
            propertiesDialog.setLocation(new Point(200, 200));
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String cmd = ae.getActionCommand();
                    if (cmd.equals(CMD_APPLY)
                            || cmd.equals(CMD_OK)) {
                        if ( !applyProperties()) {
                            return;
                        }
                    }
                    if (cmd.equals(CMD_CANCEL)
                            || cmd.equals(CMD_OK)) {
                        propertiesDialog.setVisible(false);
                    }
                }
            };


            JComponent buttons = makeButtons(listener, new String[]{CMD_APPLY, CMD_OK,CMD_CANCEL});
            propertiesDialog.getContentPane().add(
                LayoutUtil.inset(LayoutUtil.centerBottom(contents, buttons), 5));
            propertiesDialog.pack();
        }

        //        startTimePicker.setDate(getStartDate());
        //        endTimePicker.setDate(getEndDate());
        dateSelectionGui.setDateSelection(dateSelection);
        propertiesDialog.setVisible(true);
    }

    public static JPanel makeButtons(ActionListener l, String[] labels) {
        return makeButtons(l, labels, labels);
    }


    public static JPanel makeButtons(ActionListener listener, String[] labels,
                                     String[] cmds) {


        JPanel p       = new JPanel();
        List   buttons = new ArrayList();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < cmds.length; i++) {
            String cmd   = ((cmds[i] != null)
                            ? cmds[i]
                            : ((labels != null)
                               ? labels[i]
                               : ""));
            String label = ((labels != null)
                            ? labels[i]
                            : cmd);
            if (label == null) {
                label = cmd;
            }
            JButton button = new JButton(label);
            button.addActionListener(listener);
            button.setActionCommand(cmd);
            if (cmd.equals(CMD_OK)) {
                button.setDefaultCapable(true);
            }
            buttons.add(button);
        }
        return LayoutUtil.doLayout(p, LayoutUtil.getComponentArray(buttons), buttons.size(), LayoutUtil.WT_N,
                                   LayoutUtil.WT_N, null, null, new Insets(5, 5, 5, 5));
    }





    /**
     * apply the properties
     *
     * @return success
     */
    private boolean applyProperties() {
        if (isCapableOfSelection) {
            if ( !dateSelectionGui.applyProperties()) {
                return false;
            }
            useDateSelection = useDateSelectionCbx.isSelected();
            showIntervals    = showIntervalsCbx.isSelected();
            sticky           = stickyCbx.isSelected();
        }
        //        setStartDate(startTimePicker.getDate());
        //        setEndDate(endTimePicker.getDate());

        timelineChanged();
        dateSelectionChanged();
        return true;
    }


    /**
     *  Set the Sticky property.
     *
     *  @param sticky The new value for Sticky
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
        timelineChanged();
    }

    /**
     *  Get the Sticky property.
     *
     *  @return The Sticky
     */
    public boolean getSticky() {
        return sticky;
    }


    /**
     * are we using the date selection
     *
     * @return using the date selection
     */
    protected boolean dateSelectionActive() {
        return isCapableOfSelection && (dateSelection != null)
               && useDateSelection;
    }


    /**
     * Set the UseDateSelection property.
     *
     * @param value The new value for UseDateSelection
     */
    public void setUseDateSelection(boolean value) {
        useDateSelection = value;
        if (useDateSelection) {
            dateSelectionChanged();
        }
        repaint();
    }

    /**
     * Get the UseDateSelection property.
     *
     * @return The UseDateSelection
     */
    public boolean getUseDateSelection() {
        return useDateSelection;
    }


    /**
     * Set the ShowIntervals property.
     *
     * @param value The new value for ShowIntervals
     */
    public void setShowIntervals(boolean value) {
        showIntervals = value;
        repaint();
    }

    /**
     * Get the ShowIntervals property.
     *
     * @return The ShowIntervals
     */
    public boolean getShowIntervals() {
        return showIntervals;
    }



    /**
     * handle changed date selection
     */
    protected void dateSelectionChanged() {
        if (dateSelectionActive()) {
            if(datedThings!=null) {
                setSelected(dateSelection.apply(datedThings));
            }
        }
        repaint();
    }

    /**
     * handle synchronization
     *
     * @param timeline the timeline that changed
     */
    private void rangeChanged(Timeline timeline) {
        startDate        = timeline.startDate;
        endDate          = timeline.endDate;
        setHighlightedDate(mouseHighlighted);
        repaint();
    }

    /**
     * The timeline changed. repaint, etc.
     */
    public void timelineChanged() {
        if (dateSelectionActive()) {
            if (getSticky()) {
                dateSelection.setStartFixedTime(startDate);
                dateSelection.setEndFixedTime(endDate);
                dateSelectionChanged();
            }
        }
        if (timelineGroup != null) {
            for (int i = 0; i < timelineGroup.size(); i++) {
                Timeline timeline = (Timeline) timelineGroup.get(i);
                if (timeline == this) {
                    continue;
                }
                timeline.rangeChanged(this);
            }
        }


        makeSunriseDates();
        setHighlightedDate(null);
        repaint();
    }

    public void makeSunriseDates() {
    }


    /**
     * _more_
     */
    public void selectDaytime() {
        if (datedThings == null) {
            return;
        }
        List selected     = new ArrayList();
        List visibleDates = new ArrayList();
        long start        = startDate.getTime();
        long end          = endDate.getTime();
        for (int i = 0; i < datedThings.size(); i++) {
            DatedThing datedThing = (DatedThing) datedThings.get(i);
            Date       date       = datedThing.getDate();
            long       time       = date.getTime();
            if ((time >= start) && (time <= end)) {
                visibleDates.add(datedThing);
            }
        }
        for (int datedThingIdx = 0; datedThingIdx < visibleDates.size();
                datedThingIdx++) {
            DatedThing datedThing =
                (DatedThing) visibleDates.get(datedThingIdx);
            Date date = datedThing.getDate();
            long time = date.getTime();
            for (int i = 0; i < sunriseDates.size(); i += 2) {
                Date d1 = (Date) sunriseDates.get(i);
                Date d2 = (Date) sunriseDates.get(i+1);
                if ((time >= d1.getTime()) && (time <= d2.getTime())) {
                    selected.add(datedThing);
                    break;
                }
            }
        }

        setSelected(selected);
    }




    /**
     * find closest thing
     *
     * @param p location
     *
     * @return closest thing
     */
    protected DatedThing findClosest(Point p) {
        return findClosest(p, PICK_THRESHOLD);
    }





    /**
     * find closest thing
     *
     * @param p location
     * @param minimumDistance within
     *
     * @return closest thing
     */

    protected DatedThing findClosest(Point p, int minimumDistance) {
        if (p == null) {
            return null;
        }
        Dimension dim    = getSize();
        int       middle = getBaseLine();
        if (p.y > middle + 10) {
            return null;
        }
        if (p.y < middle - 10) {
            return null;
        }


        if (datedThings == null) {
            return null;
        }
        DatedThing closest     = null;
        double     minDistance = minimumDistance;
        for (int i = 0; i < datedThings.size(); i++) {
            DatedThing datedThing = (DatedThing) datedThings.get(i);
            int        location   = toLocation(datedThing.getDate());
            double     distance   = Math.abs(p.getX() - location);
            if (distance < minDistance) {
                minDistance = distance;
                closest     = datedThing;
            }
        }
        return closest;
    }


    /**
     * _more_
     *
     * @param e _more_
     *
     * @return _more_
     */
    public Point getToolTipLocation(MouseEvent e) {
        int height = getBounds().height;
        return new Point(e.getX(), height);
    }


    /**
     * get tooltip
     *
     *
     * @param event event
     * @return tooltip
     */
    public String getToolTipText(MouseEvent event) {
        if ((startDate == null) || (endDate == null)) {
            return null;
        }
        Date dttm;
        if (mouseHighlighted != null) {
            dttm = mouseHighlighted.getDate();
        } else {
            dttm = toDate((int) event.getX());
        }

        SimpleDateFormat sdf = new SimpleDateFormat(defaultFormat);
        sdf.setTimeZone(getTimeZone());
        String tt = sdf.format(dttm.getTime(), new StringBuffer(),
                               FP).toString() + "<br>";



        if (mouseHighlighted != null) {
            String tmp = mouseHighlighted.toString();
            if ((tmp != null) && (tmp.length() > 0)) {
                tt += tmp + "<br>";
            }
        }
        tt += "Control-P to show properties;<br>Arrow keys to shift/zoom;<br>";
        if (dateSelectionActive()) {
            tt += "Use Control-Arrow to change selection;";

        } else {
            if (isCapableOfSelection) {
                tt += "Click to select; Control or Shift Click to select multiples; Shift-drag to select;";
            }
        }
        return "<html>" + tt + "</html>";
    }



    /**
     * make gui
     *
     *
     * @param withButtons add dialog buttons
     * @return gui
     */
    public JComponent getContents(boolean withButtons) {
        return getContents(withButtons, true);
    }

    public JComponent getContents(boolean withButtons, boolean withBorder) {
        if (contents == null) {
            doMakeContents(withButtons, withBorder);
        }
        return contents;
    }




    /**
     * make gui
     *
     * @param withButtons with dialog buttons
     */
    protected void doMakeContents(boolean withButtons) {
        doMakeContents(withButtons, true);
    }
    protected void doMakeContents(boolean withButtons, boolean withBorder) {
        JPanel thisContainer = LayoutUtil.inset(this, 0);
        if(withBorder) {
            thisContainer.setBorder(
                                    BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        if ( !withButtons) {
            contents = thisContainer;
            return;
        }
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                dialogOK = cmd.equals(CMD_OK);
                if(dialog!=null) {
                    dialog.setVisible(false);
                }
            }
        };


        JComponent bottom = makeButtons(listener,new String[]{CMD_OK,CMD_CANCEL});
        contents = LayoutUtil.centerBottom(thisContainer, bottom);
    }



    /**
     * paint
     *
     * @param g  graphics
     * @param dateSelection the date selection
     */
    protected void paintDateSelection(Graphics2D g,
                                      DateSelection dateSelection) {

        Dimension dim    = getSize();
        int       width  = (int) dim.getWidth();
        int       height = (int) dim.getHeight();

        int       top    = 0 + verticalPadding;
        int       bottom = height - verticalPadding * 2;
        int       middle = top + (bottom - top) / 2;
        Date[]    range  = dateSelection.getRange();
        int       left   = toLocation(range[0]);
        int       right  = toLocation(range[1]);

        //        System.err.println (left + " " + right + " " + range[0] + " " + range[1]);
        if (dateSelection.hasInterval() && (Math.abs(right - left) > 5)) {
            paintIntervals((Graphics2D) g);
        }

        selectionBox = new Rectangle(left, middle - DIM_SELECTION_BOX,
                                     (right - left), DIM_SELECTION_BOX * 2);

        g.setStroke(STROKE_SOLID_TWOPIXEL);
        if (draggingDateSelection) {
            g.setColor(COLOR_HIGHLIGHT_SELECTION);
        } else {
            g.setColor(COLOR_LINE);
        }

        g.drawPolyline(new int[] { selectionBox.x, selectionBox.x,
                                   selectionBox.x + selectionBox.width,
                                   selectionBox.x + selectionBox.width,
                                   selectionBox.x }, new int[] {
                                       selectionBox.y + selectionBox.height,
                                       selectionBox.y, selectionBox.y,
                                       selectionBox.y + selectionBox.height,
                                       selectionBox.y
                                       + selectionBox.height }, 5);
        FontMetrics fm = g.getFontMetrics();
        String      s1 = format(range[0]);
        String      s2 = format(range[1]);
        int         w1 = fm.stringWidth(s1);
        int         w2 = fm.stringWidth(s2);
        int         x1 = selectionBox.x;
        int         x2 = selectionBox.x + selectionBox.width - w2;
        int         y1 = selectionBox.y - 2;
        int         y2 = selectionBox.y - 2;
        //If the labels are colliding then offset the right one up a bit
        if (draggingDateSelection) {
            if (x1 + w1 >= x2) {
                y2 -= fm.getHeight();
                g.drawLine(selectionBox.x + selectionBox.width, y1 + 2,
                           selectionBox.x + selectionBox.width, y2);
            }
            g.setStroke(STROKE_SOLID_ONEPIXEL);
            g.drawString(s1, x1, y1);
            g.drawString(s2, x2, y2);
        }
    }


    /**
     * paint intervals
     *
     * @param g  graphics
     */
    protected void paintIntervals(Graphics2D g) {
        if ( !paintIntervals || !showIntervals) {
            return;
        }
        double[] ticks = dateSelection.getIntervalTicks();
        if (ticks == null) {
            return;
        }
        long      start         = startDate.getTime();
        long      end           = endDate.getTime();

        Date[]    range         = dateSelection.getRange();
        long      intervalStart = range[0].getTime();
        long      intervalEnd   = range[1].getTime();

        Dimension dim           = getSize();
        int       width         = (int) dim.getWidth();
        int       height        = (int) dim.getHeight();

        int       top           = 0 + verticalPadding;
        int       bottom        = height - verticalPadding * 2;
        int       middle        = top + (bottom - top) / 2;

        int       left          = 0 + horizontalPadding;
        int       right         = width - horizontalPadding;


        int[]     xs            = new int[5];
        int[]     ys            = new int[5];
        for (int i = 0; i < ticks.length; i++) {
            g.setStroke(STROKE_SOLID_ONEPIXEL);
            long time     = (long) ticks[i];
            int  location = toLocation(time);

            int leftRange = toLocation((long) (time
                                - dateSelection.getPreRangeToUse()));
            int rightRange = toLocation((long) (time
                                 + dateSelection.getPostRangeToUse()));

            if ((time + dateSelection.getPostRangeToUse() < intervalStart)
                    || (time - dateSelection.getPreRangeToUse()
                        > intervalEnd)) {
                continue;
            }
            xs[0] = leftRange;
            xs[1] = leftRange;
            xs[2] = location;
            xs[3] = rightRange;
            xs[4] = rightRange;



            int y = middle - DIM_SELECTION_BOX;

            ys[0] = y;
            ys[1] = y + (middle - y) / 2;
            ys[2] = middle;
            ys[3] = y + (middle - y) / 2;
            ys[4] = y;



            g.setColor(COLOR_INTERVAL_FILL);
            g.fillPolygon(xs, ys, 5);

            g.setColor(COLOR_INTERVAL_LINE);
            //            g.drawPolyline(xs, ys, 5);


            //            g.setStroke(STROKE_DASHED_ONEPIXEL);
            g.drawLine(leftRange, y, leftRange, y + (middle - y) / 2);
            g.drawLine(rightRange, y, rightRange, y + (middle - y) / 2);

            //            g.setStroke(STROKE_DASHED_TWOPIXEL);
            g.drawLine(location, y, location, middle);


        }
        g.setStroke(STROKE_SOLID_ONEPIXEL);


    }

    /**
     * get the axis line
     *
     * @return axis line location
     */
    protected int getBaseLine() {
        if (shortDisplay) {
            return DIM_TIME_HEIGHT;
        }
        int height = (int) getSize().getHeight();
        int top    = verticalPadding;
        int bottom = height - verticalPadding;
        return height / 2;
    }

    /**
     * bottom of drawing area
     *
     * @return bottom
     */
    protected int getBottom() {
        int height = (int) getSize().getHeight();
        return height - verticalPadding;
    }

    /**
     * left of drawing area
     *
     * @return left
     */
    protected int getLeft() {
        return horizontalPadding;
    }

    /**
     * right of drawing area
     *
     * @return right
     */
    protected int getRight() {
        return (int) (getSize().getWidth() - horizontalPadding);
    }






    /**
     * paint axis
     *
     * @param g graphics
     */
    protected void paintAxis(Graphics2D g) {

        g.setFont(FONT_AXIS_MINOR);
        FontMetrics fm       = g.getFontMetrics();
        long        start    = startDate.getTime();
        long        leftTime = startDate.getTime();
        long        end      = endDate.getTime();
        int         baseLine = getBaseLine();
        int         minorY   = baseLine + DIM_TIME_HEIGHT + fm.getHeight();

        int         left     = getLeft();
        int         right    = getRight();
        int         bottom   = getBottom();
        Calendar    cal      = Calendar.getInstance(getTimeZone());

        cal.setTimeInMillis(start);
        cal.clear(Calendar.MILLISECOND);

        long       range    = end - start;
        int        field2   = Calendar.MONTH;
        int        width    = (int) getSize().getWidth();

        AxisFormat format   = null;
        int        maxTicks = width / 40;
        for (int i = 0; i < FORMATS.length; i++) {
            format = FORMATS[i];
            if (format.step > DateUtil.MILLIS_SECOND) {
                cal.set(Calendar.SECOND, 0);
            }
            if (format.step > DateUtil.MILLIS_MINUTE) {
                cal.set(Calendar.MINUTE, 0);
            }
            if (format.step > DateUtil.MILLIS_HOUR) {
                cal.set(Calendar.HOUR, 0);
            }
            if (format.step > DateUtil.MILLIS_DAY * 7) {
                cal.set(Calendar.DAY_OF_MONTH, 0);
            }
            if (format.step > DateUtil.MILLIS_MONTH) {
                cal.set(Calendar.MONTH, 0);
            }
            if (range / format.step <= maxTicks) {
                //                System.err.println ("fmt:" + i + " range: " + range + " " + format.step);
                break;
            }
        }




        SimpleDateFormat sdf = format.getSDF(getTimeZone());
        g.setStroke(STROKE_DASHED_ONEPIXEL);
        g.setColor(COLOR_AXIS_MINOR);
        int majorValue = -1;

        start = cal.getTimeInMillis();
        int x     = 0;


        int cnt   = 0;
        int limit = 100;
        while ((x <= width) && (cnt < limit)) {
            cnt++;
            x = toLocation(start);
            if (x < 0) {
                start += format.step;
                continue;
            }
            String s = sdf.format(start, new StringBuffer(), FP).toString();
            //            if(s.startsWith("0")) s = " " + s.substring(1);
            g.drawLine(x, minorY, x, baseLine);
            int sw = fm.stringWidth(s);
            //            g.drawString(s, x-sw-2, minorY);
            g.drawString(s, x + 2, minorY);
            start += format.step;
        }

        //If we are looking at billions of years just return
        //since the universe is only 4000 years old no since showing
        //anything
        if (cnt >= limit) {
            return;
        }

        g.setFont(FONT_AXIS_MAJOR);
        fm = g.getFontMetrics();
        g.setColor(COLOR_AXIS_MAJOR);
        g.setStroke(STROKE_SOLID_ONEPIXEL);
        if (format.subFormat != null) {
            cal.set(Calendar.MILLISECOND, 0);
            if (format.subFormat.field != Calendar.MILLISECOND) {
                cal.set(Calendar.SECOND, 0);
                if (format.subFormat.field != Calendar.SECOND) {
                    cal.set(Calendar.MINUTE, 0);
                    if (format.subFormat.field != Calendar.MINUTE) {
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.DAY_OF_MONTH, 0);
                    }
                }
            }
            //        cal.clear(Calendar.DAY_OF_YEAR);
            //        cal.clear(Calendar.DATE);
            start = cal.getTimeInMillis();
            int y = baseLine + DIM_TIME_HEIGHT + fm.getHeight() * 2;
            sdf = format.subFormat.getSDF(getTimeZone());
            x   = 0;

            while ((start = cal.getTimeInMillis()) < leftTime) {
                cal.add(format.subFormat.field, format.subFormat.offset);
            }

            while (x <= width) {
                x = toLocation(start);
                if (x < 0) {
                    cal.add(format.subFormat.field, format.subFormat.offset);
                    start = cal.getTimeInMillis();
                    continue;
                }
                String s = sdf.format(start, new StringBuffer(),
                                      FP).toString();
                if (s.startsWith("0")) {
                    s = " " + s.substring(1);
                }
                //                g.drawLine(x, y, x, baseLine);
                g.drawLine(x, minorY + 4, x, baseLine);
                int sw = fm.stringWidth(s);
                //            g.drawString(s, x-sw-2, y);
                //                g.drawString(s, x + 2, y);
                g.drawString(s, x - sw / 2, y);
                cal.add(format.subFormat.field, format.subFormat.offset);
                start = cal.getTimeInMillis();
            }

        }


        g.setStroke(STROKE_SOLID_ONEPIXEL);




    }

    /**
     * paint after we filled the background but before we do anything else
     *
     * @param g graphics
     */
    public void paintBackgroundDecoration(Graphics2D g) {
        if (sunriseDates.size() > 0) {
            g.setColor(Color.yellow);
            int height = (int) getSize().getHeight();
            for (int i = 0; i < sunriseDates.size(); i += 2) {
                Date d1 = (Date) sunriseDates.get(i);
                Date d2 = (Date) sunriseDates.get(i+1);
                int  x1 = toLocation(d1);
                int  x2 = toLocation(d2);
                g.fillRect(x1, 0, (x2 - x1), height);
            }
        }
    }


    /**
     * paint
     *
     * @param g graphics
     */
    public void paint(Graphics g) {

        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g.setFont(FONT_MAIN);
        int width    = (int) getSize().getWidth();
        int height   = (int) getSize().getHeight();
        int baseLine = getBaseLine();
        int left     = getLeft();
        int right    = getRight();

        if (isEnabled()) {
            g.setColor(COLOR_BACKGROUND);
        } else {
            g.setColor(COLOR_BACKGROUND_DISABLED);
        }
        g.fillRect(0, 0, width, height);

        paintBackgroundDecoration(g2);



        if (doingDragSelect && (dragStartDate != null)
                && (dragEndDate != null)) {

            int dx1 = toLocation(dragStartDate);
            int dx2 = toLocation(dragEndDate);
            int x   = Math.min(dx1, dx2);
            int h   = 40;
            int y   = baseLine - h / 2;
            int w   = Math.abs(dx1 - dx2);
            g2.setStroke(STROKE_DASHED_ONEPIXEL);
            g2.setColor(Color.lightGray);
            g2.fillRect(x, y, w, h);
            g2.setColor(Color.black);
            g2.drawRect(x, y, w, h);
        }


        //Draw the axis line
        g.setColor(COLOR_LINE);
        g.drawLine(left, baseLine, right, baseLine);

        if ((startDate == null) || (endDate == null)) {
            return;
        }
        long start = startDate.getTime();
        long end   = endDate.getTime();

        if ( !shortDisplay) {
            paintAxis(g2);
        }



        if (dateSelectionActive()) {
            paintDateSelection((Graphics2D) g, dateSelection);
        }


        g2.setStroke(STROKE_SOLID_ONEPIXEL);
        g2.setColor(COLOR_LINE);

        FontMetrics fm  = g.getFontMetrics();
        String      s1  = format(startDate);
        String      s2  = format(endDate);
        int         w1  = fm.stringWidth(s1);
        int         w2  = fm.stringWidth(s2);
        int         s2x = right - w2 - 2;
        if ( !shortDisplay) {
            int dart = 6;
            s2x -= dart;
            int bottomy = verticalPadding + fm.getHeight();
            g.setColor(COLOR_RANGE_DART);
            g.fillPolygon(new int[] { 0, dart, dart, 0 }, new int[] { 0 + 1,
                    0 + 1, dart + 1, 0 + 1 }, 4);
            g2.setColor(COLOR_RANGE);
            g.drawString(s1, left + dart + 2, bottomy);
            g.setColor(COLOR_RANGE_DART);
            if (s2x <= left + 2 + w1) {
                bottomy = getBottom();
                g.fillPolygon(new int[] { right - dart, right, right - dart,
                                          right - dart }, new int[] {
                                          bottomy - 1,
                                          bottomy - 1, bottomy - dart - 1,
                                          bottomy - 1 }, 4);
            } else {
                g.fillPolygon(new int[] { right - dart, right, right - dart,
                                          right - dart }, new int[] { 0 + 1,
                        0 + 1, dart + 1, 0 + 1 }, 4);
            }
            g2.setColor(COLOR_RANGE);
            g.drawString(s2, s2x, bottomy);


        }

        Hashtable seenLocations = new Hashtable();
        if (datedThings != null) {
            for (int selectIdx = 0; selectIdx < 2; selectIdx++) {
                for (int i = 0; i < datedThings.size(); i++) {
                    DatedThing datedThing = (DatedThing) datedThings.get(i);
                    Date       date       = datedThing.getDate();
                    long       time       = date.getTime();
                    if ((time < start) || (time > end)) {
                        continue;
                    }
                    int     location   = toLocation(date);
                    boolean isSelected = selectedMap.get(datedThing) != null;
                    if (isSelected && (selectIdx == 0)) {
                        continue;
                    }
                    if ( !isSelected && (selectIdx == 1)) {
                        continue;
                    }
                    if (justShowSelected && !isSelected) {
                        continue;
                    }

                    //                Object key = new Integer(location);
                    //                if(seenLocations.get(key)!=null) continue;
                    //                seenLocations.put(key,key);

                    if (datedThing == mouseHighlighted) {
                        if (isSelected) {
                            g.setColor(getColorTimeSelected());
                            g.fillRect(location - DIM_TIME_WIDTH / 2 - 1,
                                       baseLine - DIM_TIME_HEIGHT / 2 - 1,
                                       DIM_TIME_WIDTH + 2,
                                       DIM_TIME_HEIGHT + 2);
                        }
                        g.setColor(COLOR_HIGHLIGHT_DATE);
                    } else {
                        if (isSelected) {
                            g.setColor(getColorTimeSelected());
                        } else {
                            g.setColor(getColorTimeUnselected());
                        }
                    }
                    g.fillRect(location - DIM_TIME_WIDTH / 2,
                               baseLine - DIM_TIME_HEIGHT / 2,
                               DIM_TIME_WIDTH, DIM_TIME_HEIGHT);
                    //g.fillRect(location - DIM_TIME_WIDTH / 2, baseLine,
                    //                           DIM_TIME_WIDTH, DIM_TIME_HEIGHT);
                }
            }
        }

    }

    public void setHighlightedDate(DatedThing d) {
        mouseHighlighted = d;
    }

    public DatedThing getHighlightedDate() {
        return   mouseHighlighted;
    }


    /**
     * set list of things
     *
     * @param l list of DatedThings
     */
    public void setDatedThings(List l) {
        setDatedThings(l, false);
    }



    /**
     * set the group we're linked to
     *
     * @param group group of Timelines
     */
    public void setGroup(List group) {
        timelineGroup = group;
    }



    /**
     * set list of things
     *
     * @param l list of DatedThings
     * @param andSetRange also set the start/end visible range
     */
    public void setDatedThings(List l, boolean andSetRange) {
        this.datedThings = DatedObject.sort(l, true);
        if (andSetRange) {
            if (this.datedThings.size() >= 2) {
                //Pad the initial bounds a little bit
                Date d1 = ((DatedThing) this.datedThings.get(0)).getDate();
                Date d2 = ((DatedThing) this.datedThings.get(
                              this.datedThings.size() - 1)).getDate();
                long middle = d1.getTime()
                              + (d2.getTime() - d1.getTime()) / 2;
                long timeDelta = (long) ((d2.getTime() - d1.getTime()) * 1.1);
                init(new Date(middle - timeDelta / 2),
                     new Date(middle + timeDelta / 2));
            } else {
                startDate = new Date(System.currentTimeMillis()
                                     - DateUtil.MILLIS_DAY);
                startDate = new Date(System.currentTimeMillis());
            }
        }

        repaint();
    }


    /**
     * format date
     *
     * @param date date
     *
     * @return date formatted
     */
    public String format(Date date) {
        return format(date.getTime());
    }


    /**
     * get the format to use given the size of the visible range
     *
     * @return format
     */
    public String getFormat() {
        long start = startDate.getTime();
        long end   = endDate.getTime();
        long diff  = end - start;
        if (diff < DateUtil.MILLIS_SECOND * 20) {
            return "yyyy-MM-dd HH:mm:ss:SS z";
        }

        if (diff < 5 * DateUtil.MILLIS_MINUTE) {
            return "yyyy-MM-dd HH:mm:ss z";
        }

        if (diff < DateUtil.MILLIS_HOUR) {
            return "yyyy-MM-dd HH:mm z";
        }

        if (diff < DateUtil.MILLIS_DAY) {
            return "yyyy-MM-dd HH:mm z";
        }

        if (diff < 3 * DateUtil.MILLIS_DAY) {
            return "yyyy-MM-dd HH:00 z";
        }

        if (diff < DateUtil.MILLIS_WEEK) {
            return "yyyy-MM-dd";
        }
        if (diff < DateUtil.MILLIS_MONTH) {
            return "yyyy-MM-dd";
        }
        if (diff < DateUtil.MILLIS_YEAR) {
            return "yyyy-MM-dd";
        }

        if (diff < DateUtil.MILLIS_DECADE) {
            return "yyyy-MM";
        }

        if (diff > DateUtil.MILLIS_MILLENIUM) {
            return "yyyy G";
        }
        return "yyyy";
    }


    /**
     * format time
     *
     * @param time time
     *
     * @return formatted time
     */
    public String format(long time) {
        return format(time, getFormat());
    }


    /**
     * format time
     *
     * @param time time
     * @param pattern date format
     *
     * @return formatted time
     */
    public String format(long time, String pattern) {
        long   now  = System.currentTimeMillis();
        double diff = now - time;
        if (diff > DateUtil.MILLIS_MILLENIUM * 1000) {
            diff = diff / (DateUtil.MILLIS_MILLENIUM * 1000);
            diff *= 100;
            diff = (int) diff;
            diff /= 100;
            return diff + " mya";
        }
        diff = Math.abs(diff);
        if (diff > DateUtil.MILLIS_MILLENIUM * 1000) {
            diff = diff / (DateUtil.MILLIS_MILLENIUM * 1000);
            diff *= 100;
            diff = (int) diff;
            diff /= 100;
            return diff + " my";
        }


        //        System.err.println("format:" + pattern);

        StringBuffer buf = new StringBuffer();
        DateFormat   sdf = new SimpleDateFormat();
        sdf.setTimeZone(getTimeZone());
        if ((pattern != null) && (sdf instanceof SimpleDateFormat)) {
            ((SimpleDateFormat) sdf).applyPattern(pattern);
        }
        return (sdf.format(time, buf, new FieldPosition(0))).toString();
    }





    /**
     * reset range
     *
     * @param doDateSelection determines whether we reset the date selection range or the visible range
     */
    protected void reset(boolean doDateSelection) {
        if (doDateSelection) {
            if (originalDateSelection != null) {
                dateSelection = new DateSelection(originalDateSelection);
            } else {
                dateSelection = new DateSelection();
            }
            dateSelectionChanged();
        } else {
            startDate = originalStartDate;
            endDate   = originalEndDate;
            timelineChanged();
        }

    }


    /**
     * shift left/right a percentage
     *
     * @param percent how much
     * @param doDateSelection visible or date selection
     */
    protected void shiftByPercent(double percent, boolean doDateSelection) {
        long start     = (doDateSelection
                          ? dateSelection.getStartFixedTime()
                          : startDate.getTime());
        long end       = (doDateSelection
                          ? dateSelection.getEndFixedTime()
                          : endDate.getTime());
        long timeDelta = (long) ((end - start) * percent);
        if (doDateSelection) {
            dateSelection.setStartFixedTime(start + timeDelta);
            dateSelection.setEndFixedTime(end + timeDelta);
            dateSelectionChanged();
        } else {
            startDate = new Date(start + timeDelta);
            endDate   = new Date(end + timeDelta);
            timelineChanged();
        }
    }



    /**
     * zoom
     *
     * @param percent how much
     * @param doDateSelection visible or date selection
     */
    public void expandByPercent(double percent, boolean doDateSelection) {
        long start     = (doDateSelection
                          ? dateSelection.getStartFixedTime()
                          : startDate.getTime());
        long end       = (doDateSelection
                          ? dateSelection.getEndFixedTime()
                          : endDate.getTime());
        long middle    = start + (end - start) / 2;
        long timeDelta = (long) ((end - start) * percent);
        if (timeDelta <= 1) {
            timeDelta = 2;
        }
        long half = timeDelta / 2;
        while (half <= 2) {
            half++;
        }
        long t1 = middle - half;
        long t2 = middle + half;

        if (t2 <= t1) {
            t1 -= (t1 - t2) + 1;
            t2++;
        }
        if (doDateSelection) {
            dateSelection.setStartFixedTime(t1);
            dateSelection.setEndFixedTime(t2);
            dateSelectionChanged();
        } else {
            startDate = new Date(t1);
            endDate   = new Date(t2);
            timelineChanged();
        }
    }


    /**
     * map x location to date
     *
     * @param location x
     *
     * @return date
     */
    public Date toDate(int location) {
        Dimension dim     = getSize();
        long      start   = startDate.getTime();
        long      end     = endDate.getTime();
        int       width   = (int) dim.getWidth();
        int       left    = horizontalPadding;
        int       right   = width - horizontalPadding;
        double    percent = location / (double) (right - left);
        return new Date(start + (long) (percent * (end - start)));
    }


    /**
     * map date to x location
     *
     * @param date location
     *
     * @return x
     */
    public int toLocation(Date date) {
        return toLocation(date.getTime());
    }


    /**
     * map date to x
     *
     * @param date date
     *
     * @return x
     */
    public int toLocation(long date) {
        if (startDate == null) {
            return 0;
        }
        Dimension dim   = getSize();
        long      start = startDate.getTime();
        long      end   = endDate.getTime();
        if (start == end) {
            return -1;
        }
        double percent = (date - start) / (double) (end - start);
        //        System.err.println(date+"     " + percent);
        int width      = (int) dim.getWidth();
        int innerWidth = width - horizontalPadding * 2;

        return horizontalPadding + (int) (percent * (innerWidth));
    }


    /**
     *  Set the StartDate property.
     *
     *
     * @param start start
     * @param end end
     */
    public void setRange(Date start, Date end) {
        setRange(start, end, false);
    }

    /**
     * _more_
     */
    public void makeCurrentRangeOriginal() {
        originalStartDate = startDate;
        originalEndDate   = endDate;
    }


    /**
     * _more_
     *
     * @param start _more_
     * @param end _more_
     * @param makeTheseTheOriginal _more_
     */
    public void setRange(Date start, Date end, boolean makeTheseTheOriginal) {
        startDate = start;
        endDate   = end;
        if (makeTheseTheOriginal) {
            originalStartDate = startDate;
            originalEndDate   = endDate;
        }
        timelineChanged();
    }



    /**
     *  Set the StartDate property.
     *
     *  @param value The new value for StartDate
     */
    public void setStartDate(Date value) {
        startDate = value;
        timelineChanged();
    }

    /**
     *  Get the StartDate property.
     *
     *  @return The StartDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     *  Set the EndDate property.
     *
     *  @param value The new value for EndDate
     */
    public void setEndDate(Date value) {
        endDate = value;
        timelineChanged();
    }

    /**
     *  Get the EndDate property.
     *
     *  @return The EndDate
     */
    public Date getEndDate() {
        return endDate;
    }



    /**
     * popup timeline in a dialog
     *
     *
     * @return ok
     */
    public boolean popup() {
        if (dialog == null) {
            dialog = new JDialog((JFrame) null, "Date Selection", true);
            dialog.getContentPane().add(getContents(true));
            dialog.pack();
            dialog.setLocation(new Point(200, 200));
        }
        dialogOK = false;
        dialog.show();
        return dialogOK;
    }



    /**
     * main
     *
     * @param args args
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        boolean useDateSelection = true;
        try {

            final List dates = new ArrayList();
            if (args.length == 0) {
                long now = System.currentTimeMillis();
                for (int i = 0; i < 30; i++) {
                    DatedObject datedObject =
                        new DatedObject(new Date((long) (now
                            + DateUtil.minutesToMillis(20)
                            - i * Math.random() * 60 * 60 * 1000)));
                    dates.add(datedObject);
                }
            } else {
                useDateSelection = false;
                String fmt = "EEE MMM dd hh:mm:ss yyyy";
                String file;
                if (args.length > 1) {
                    fmt  = args[0];
                    file = args[1];
                } else {
                    file = args[0];
                }

                BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(
                                                new FileInputStream(file)));
                String line;
                //[Sat Jul 28 03:14:46 2007] [error] [client 74.6.26.164] Directory index forbidden by rule: /content/software/IDV/release/stable/webstart/
                SimpleDateFormat dateFormat =
                    new java.text.SimpleDateFormat(fmt);
                java.text.ParsePosition pp = new java.text.ParsePosition(0);
                while ((line = reader.readLine()) != null) {
                    int idx1 = line.indexOf("[");
                    int idx2 = line.indexOf("]", idx1);
                    if ((idx1 < 0) || (idx2 <= idx1)) {
                        continue;
                    }
                    String dateString = line.substring(idx1 + 1, idx2);
                    //                        Date date = dateFormat.parse(dateString, new ParsePosition(0));
                    pp.setIndex(0);
                    Date date = dateFormat.parse(dateString, pp);
                    if (date != null) {
                        //                    System.err.println("date:" + date);
                        dates.add(new DatedObject(date, line));
                    } else {
                        System.err.println("failed to  parse:" + dateString);
                        System.err.println("fmt:" + fmt);
                        return;
                    }
                }


            }

            if (dates.size() == 0) {
                System.err.println("no dates");
                return;
            }
            final Timeline timeline = new Timeline(dates, 400);
            timeline.setUseDateSelection(useDateSelection);
            DateSelection dateSelection =
                new DateSelection(timeline.getStartDate(),
                                  timeline.getEndDate());

            dateSelection.setRoundTo(DateUtil.minutesToMillis(15));
            dateSelection.setInterval(DateUtil.minutesToMillis(120));
            //        dateSelection.setPreRange(DateUtil.minutesToMillis(15));
            timeline.setDateSelection(dateSelection);
            //        dateSelection.setPostRange(0);
            //        dateSelection.setIntervalRange(DateUtil.minutesToMillis(30));


            final JCheckBox justShowSelectedCbx =
                new JCheckBox("Just show selected", false);
            justShowSelectedCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    timeline.justShowSelected =
                        justShowSelectedCbx.isSelected();
                    timeline.repaint();
                }

            });


            final JButton useSelectedBtn = new JButton("Use selected");
            useSelectedBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    timeline.setDatedThings(timeline.getSelected());
                }

            });


            final JButton resetBtn = new JButton("Reset");
            resetBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    timeline.setDatedThings(dates);
                }
            });

            final JTextField searchFld = new JTextField();
            searchFld.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    List toks = StringUtil.split(searchFld.getText().trim(),
                                    ",", true, true);
                    List selected = new ArrayList();
                    for (int i = 0; i < dates.size(); i++) {
                        DatedObject datedObject = (DatedObject) dates.get(i);
                        boolean     ok          = true;
                        for (int tokIdx = 0; tokIdx < toks.size(); tokIdx++) {
                            String  s   = (String) toks.get(tokIdx);
                            boolean not = s.startsWith("!");
                            if (not) {
                                s = s.substring(0);
                            }
                            boolean contains =
                                datedObject.getObject().toString().indexOf(s)
                                >= 0;
                            if (not) {
                                contains = !contains;
                            }
                            //                                if(i==0) System.err.println ("s:" + s + " " + not);
                            if ( !contains) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            selected.add(datedObject);
                        }
                    }
                    timeline.setSelected(selected);

                }
            });
            JComponent bottom = LayoutUtil.centerRight(searchFld,
                                    LayoutUtil.hbox(useSelectedBtn, resetBtn));
            //            JComponent bottom = searchFld;
            JDialog dialog = new JDialog((JFrame) null, "Date Selection",
                                         true);
            dialog.getContentPane().add(
                LayoutUtil.centerBottom(timeline.getContents(true), bottom));
            dialog.pack();
            dialog.setLocation(new Point(200, 200));
            dialog.show();

        } catch (Throwable thr) {
            thr.printStackTrace();
            return;
        }


    }


    /**
     *  Set the IsCapableOfSelection property.
     *
     *  @param value The new value for IsCapableOfSelection
     */
    public void setIsCapableOfSelection(boolean value) {
        isCapableOfSelection = value;
    }

    /**
     *  Get the IsCapableOfSelection property.
     *
     *  @return The IsCapableOfSelection
     */
    public boolean getIsCapableOfSelection() {
        return isCapableOfSelection;
    }


    /**
     * Set the ShortDisplay property.
     *
     * @param value The new value for ShortDisplay
     */
    public void setShortDisplay(boolean value) {
        shortDisplay = value;
        Dimension dim = getPreferredSize();
        if (shortDisplay) {
            setPreferredSize(new Dimension((int) dim.getWidth(), 20));
        } else {
            setPreferredSize(new Dimension((int) dim.getWidth(), 100));
        }
    }

    /**
     * Get the ShortDisplay property.
     *
     * @return The ShortDisplay
     */
    public boolean getShortDisplay() {
        return shortDisplay;
    }

    /**
     * Set the ColorTimeSelected property.
     *
     * @param value The new value for ColorTimeSelected
     */
    public void setColorTimeSelected(Color value) {
        colorTimeSelected = value;
    }

    /**
     * Get the ColorTimeSelected property.
     *
     * @return The ColorTimeSelected
     */
    public Color getColorTimeSelected() {
        return colorTimeSelected;
    }

    /**
     * Set the ColorTimeUnselected property.
     *
     * @param value The new value for ColorTimeUnselected
     */
    public void setColorTimeUnselected(Color value) {
        colorTimeUnselected = value;
    }

    /**
     * Get the ColorTimeUnselected property.
     *
     * @return The ColorTimeUnselected
     */
    public Color getColorTimeUnselected() {
        return colorTimeUnselected;
    }



}

