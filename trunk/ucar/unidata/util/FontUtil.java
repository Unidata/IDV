/*
 * $Id: FontUtil.java,v 1.8 2006/05/05 19:19:34 jeffmc Exp $
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



import java.awt.*;


/**
 * font utilities
 * @author John Caron
 * @version $Id: FontUtil.java,v 1.8 2006/05/05 19:19:34 jeffmc Exp $
 */
public class FontUtil {

    //  private static GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    //  private static String fontList[] = ge.getAvailableFontFamilyNames();
    //  private static Font font[] = ge.getAvailableFonts();

    /** _more_ */
    private static final int MAX_FONTS = 15;

    /** _more_ */
    private static int fontType = Font.PLAIN;
    // standard

    /** _more_ */
    private static Font[] stdFont = new Font[MAX_FONTS];  // list of fonts to use to make text bigger/smaller

    /** _more_ */
    private static FontMetrics[] stdMetrics = new FontMetrics[MAX_FONTS];  // fontMetric for each font
    // mono

    /** _more_ */
    private static Font[] monoFont = new Font[MAX_FONTS];  // list of fonts to use to make text bigger/smaller

    /** _more_ */
    private static FontMetrics[] monoMetrics = new FontMetrics[MAX_FONTS];  // fontMetric for each font

    /** _more_ */
    private static boolean debug = false;

    /** _more_ */
    private static boolean isInit = false;

    /**
     * _more_
     */
    static private void init() {
        if (isInit) {
            return;
        }
        initFontFamily("SansSerif", stdFont, stdMetrics);
        initFontFamily("Monospaced", monoFont, monoMetrics);
        isInit = true;
    }

    /**
     * _more_
     *
     * @param name
     * @param fonts
     * @param fontMetrics
     */
    static private void initFontFamily(String name, Font[] fonts,
                                       FontMetrics[] fontMetrics) {
        for (int i = 0; i < MAX_FONTS; i++) {
            int fontSize = (i < 6)
                           ? 5 + i
                           : ((i < 11)
                              ? 10 + 2 * (i - 5)
                              : 20 + 4 * (i - 10));
            fonts[i] = new Font(name, fontType, fontSize);
            fontMetrics[i] =
                Toolkit.getDefaultToolkit().getFontMetrics(fonts[i]);

            if (debug) {
                System.out.println("TextSymbol font " + fonts[i] + " "
                                   + fontSize + " "
                                   + fontMetrics[i].getAscent());
            }
        }
    }

    // gets largest font smaller than pixel_height

    /**
     * _more_
     *
     * @param pixel_height
     * @return _more_
     */
    static public FontUtil.StandardFont getStandardFont(int pixel_height) {
        init();
        return new StandardFont(stdFont, stdMetrics, pixel_height);
    }

    // gets largest font smaller than pixel_height

    /**
     * _more_
     *
     * @param pixel_height
     * @return _more_
     */
    static public FontUtil.StandardFont getMonoFont(int pixel_height) {
        init();
        return new StandardFont(monoFont, monoMetrics, pixel_height);
    }

    /**
     * Class StandardFont
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public static class StandardFont {

        /** _more_ */
        private int currFontNo;

        /** _more_ */
        private int height;

        /** _more_ */
        private Font[] fonts;

        /** _more_ */
        private FontMetrics[] fontMetrics;

        /**
         * _more_
         *
         * @param fonts
         * @param fontMetrics
         * @param pixel_height
         *
         */
        StandardFont(Font[] fonts, FontMetrics[] fontMetrics,
                     int pixel_height) {
            this.fonts       = fonts;
            this.fontMetrics = fontMetrics;
            currFontNo       = findClosest(pixel_height);
            height           = fontMetrics[currFontNo].getAscent();
        }

        /**
         * _more_
         * @return _more_
         */
        public Font getFont() {
            return fonts[currFontNo];
        }

        /**
         * _more_
         * @return _more_
         */
        public int getFontHeight() {
            return height;
        }

        /**
         * increment the font size one "increment"
         * @return _more_
         */
        public Font incrFontSize() {
            if (currFontNo < MAX_FONTS - 1) {
                currFontNo++;
                this.height = fontMetrics[currFontNo].getAscent();
            }
            return getFont();
        }

        /**
         * decrement the font size one "increment"
         * @return _more_
         */
        public Font decrFontSize() {
            if (currFontNo > 0) {
                currFontNo--;
                this.height = fontMetrics[currFontNo].getAscent();
            }
            return getFont();
        }

        /**
         * _more_
         *
         * @param s
         * @return _more_
         */
        public Dimension getBoundingBox(String s) {
            return new Dimension(fontMetrics[currFontNo].stringWidth(s),
                                 height);
        }

        // gets largest font smaller than pixel_height

        /**
         * _more_
         *
         * @param pixel_height
         * @return _more_
         */
        private int findClosest(int pixel_height) {
            for (int i = 0; i < MAX_FONTS - 1; i++) {
                if (fontMetrics[i + 1].getAscent() > pixel_height) {
                    return i;
                }
            }
            return MAX_FONTS - 1;
        }

    }  // inner class StandardFont
}

/*
 *  Change History:
 *  $Log: FontUtil.java,v $
 *  Revision 1.8  2006/05/05 19:19:34  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.7  2005/05/13 18:32:39  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.6  2004/08/19 21:34:44  jeffmc
 *  Scratch log4j
 *
 *  Revision 1.5  2004/02/27 21:18:50  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.4  2004/02/27 21:09:33  jeffmc
 *  Snapshot of javadoc fixes
 *
 *  Revision 1.3  2004/01/29 17:37:38  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.2  2003/05/19 19:17:58  jeffmc
 *  MIssed changing the package
 *
 *  Revision 1.1  2003/05/14 19:40:40  jeffmc
 *  Moved these over from text package.
 *
 *  Revision 1.3  2001/02/06 22:45:12  caron
 *  add FOntFamiliy
 *
 *  Revision 1.2  2000/08/18 04:15:50  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.1  1999/12/16 22:58:05  caron
 *  gridded data viewer checkin
 *
 */






