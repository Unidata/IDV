/*
 * $Id: ISO646.java,v 1.10 2006/05/05 19:19:35 jeffmc Exp $
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



package ucar.unidata.util;  // ucar.lang :-) ?



/**
 * Byte Classification as ISO 646 ranges, in the manner of
 * <code>\<ctype.h\></code> in the C locale.
 * <p>
 * The motivation for this class is that the character classifications are
 * it is intentionally restricted. <code>isupper(c)</code> is only true
 * for 'A-Z'. It is important to only use this where it is appropriate,
 * where the input is declared to be ISO 646. We use it for decoding
 * meteorological bulletins.
 *
 * @see java.lang.Character
 * @author $Author: jeffmc $
 * @version $Revision: 1.10 $ $Date: 2006/05/05 19:19:35 $
 */
public final class ISO646 {

    /* Implementation translated from Plauger */

    /** _more_ */
    static private final byte BB = (byte) 0x080;  // BEL, BS, ...

    /** _more_ */
    static private final byte CN = (byte) 0x040;  // CR, FF, HT, NL, VT

    /** _more_ */
    static private final byte DI = (byte) 0x020;  // '0' - '9'

    /** _more_ */
    static private final byte LO = (byte) 0x010;  // 'a' - 'z'

    /** _more_ */
    static private final byte PU = (byte) 0x008;  // punctuation

    /** _more_ */
    static private final byte SP = (byte) 0x004;  // space

    /** _more_ */
    static private final byte UP = (byte) 0x002;  // upper

    /** _more_ */
    static private final byte XD = (byte) 0x001;  // hex digit

    /** _more_ */
    static private final byte XDI = (byte) (DI | XD);

    /** _more_ */
    static private final byte XLO = (byte) (LO | XD);

    /** _more_ */
    static private final byte XUP = (byte) (UP | XD);

    /** _more_ */
    static private final byte[] tab_ = {

        BB, BB, BB, BB, BB, BB, BB, BB, BB, CN, CN, CN, CN, CN, BB, BB, BB,
        BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, BB, SP, PU,
        PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, XDI, XDI, XDI,
        XDI, XDI, XDI, XDI, XDI, XDI, XDI, PU, PU, PU, PU, PU, PU, PU, XUP,
        XUP, XUP, XUP, XUP, XUP, UP, UP, UP, UP, UP, UP, UP, UP, UP, UP, UP,
        UP, UP, UP, UP, UP, UP, UP, UP, UP, PU, PU, PU, PU, PU, PU, XLO, XLO,
        XLO, XLO, XLO, XLO, LO, LO, LO, LO, LO, LO, LO, LO, LO, LO, LO, LO,
        LO, LO, LO, LO, LO, LO, LO, LO, PU, PU, PU, PU, BB
    };

    /**
     * _more_
     *
     * @param ch
     * @return _more_
     */
    static public boolean isdigit(byte ch) {
        try {
            return ((tab_[ch] & DI) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /**
     * _more_
     *
     * @param ch
     * @return _more_
     */
    static public boolean isupper(byte ch) {
        try {
            return ((tab_[ch] & UP) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /**
     * _more_
     *
     * @param ch
     * @return _more_
     */
    static public boolean islower(byte ch) {
        try {
            return ((tab_[ch] & LO) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /**
     * _more_
     *
     * @param ch
     * @return _more_
     */
    static public boolean isalnum(byte ch) {
        try {
            return ((tab_[ch] & (DI | LO | UP)) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /**
     * _more_
     *
     * @param ch
     * @return _more_
     */
    static public boolean isspace(byte ch) {
        try {
            return ((tab_[ch] & (SP | CN)) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /**
     * Returns true if upper case or numeric.
     *
     * @param ch
     * @return _more_
     */
    static public boolean isupnum(byte ch) {
        try {
            return ((tab_[ch] & (DI | UP)) != 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    /* Begin Test */

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.print("isdigit: ");
            for (int ch = Byte.MIN_VALUE; ch <= Byte.MAX_VALUE; ch++) {
                byte bb = (byte) ch;
                if (isdigit(bb)) {
                    System.out.print(ByteString.ByteToChar8859_1(bb));
                }
            }
            System.out.println("");

            System.out.print("isupper: ");
            for (int ch = Byte.MIN_VALUE; ch <= Byte.MAX_VALUE; ch++) {
                byte bb = (byte) ch;
                if (isupper(bb)) {
                    System.out.print(ByteString.ByteToChar8859_1(bb));
                }
            }
            System.out.println("");

            System.out.print("islower: ");
            for (int ch = Byte.MIN_VALUE; ch <= Byte.MAX_VALUE; ch++) {
                byte bb = (byte) ch;
                if (islower(bb)) {
                    System.out.print(ByteString.ByteToChar8859_1(bb));
                }
            }
            System.out.println("");

            System.out.print("isupnum: ");
            for (int ch = Byte.MIN_VALUE; ch <= Byte.MAX_VALUE; ch++) {
                byte bb = (byte) ch;
                if (isupnum(bb)) {
                    System.out.print(ByteString.ByteToChar8859_1(bb));
                }
            }
            System.out.println("");
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}

