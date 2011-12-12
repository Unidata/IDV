/*
 * $Id: ByteString.java,v 1.14 2006/05/05 19:19:33 jeffmc Exp $
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



import java.io.Serializable;


/**
 * An immutable array of Byte.
 * Similar to java.lang.String, but byte instead of char.
 * <p>
 * The motivation for this class is to contain meteorlogical
 * bulletins or observations. The "text" in these is encoded
 * in "International Alphabet Number 5",
 * aka CCITT Recommendation T.50, essentially ISO 646, which we know
 * as 7 bit ASCII.
 * Any data (non-text) portion is an octet (byte) sequence.
 * We intend to store _many_
 * of these, so doubling the size to String may not be acceptable.
 * Further, since the data has archival value, we wish to minimize
 * alteration and transformation of the data.
 *
 * @see java.lang.String
 * @author $Author: jeffmc $
 * @version $Revision: 1.14 $ $Date: 2006/05/05 19:19:33 $
 */
public abstract class ByteString implements Comparable, Serializable {

    /**
     * Widen an 8859_1 encoded byte to Unicode char.
     * (ISO 8859_1 is the java default encoding).
     * See sun.io.ByteToChar8859_1.
     *
     * @param bb
     * @return _more_
     */
    static final public char ByteToChar8859_1(byte bb) {
        if (bb < 0) {
            return (char) (256 + bb);
        }
        // else
        return (char) bb;
    }

    /**
     * Narrow Unicode char to an 8859_1 encoded byte.
     * (ISO 8859_1 is the java default encoding).
     * See sun.io.CharToByte8859_1.
     *
     * @param cc
     * @return _more_
     */
    static final public byte CharToByte8859_1(char cc) {
        if (cc > '\u00FF') {
            throw new IllegalArgumentException();
        }
        // java.io.CharConversionException
        // else
        return (byte) cc;
    }

    /**
     * Compare two byte strings lexographically.
     * @see java.lang.String#compareTo
     *
     * @param bs1
     * @param bs2
     * @return _more_
     */
    static public int compare(ByteString bs1, ByteString bs2) {
        final int len1 = bs1.getLength();
        final int len2 = bs2.getLength();
        final int nn   = Math.min(len1, len2);
        for (int ii = 0; ii < nn; ii++) {
            final char c1 = bs1.charAt(ii);
            final char c2 = bs2.charAt(ii);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    /**
     * Compare two byte strings lexographically.
     * @see java.lang.String#compareTo
     *
     * @param bs1
     * @param s2
     * @return _more_
     */
    static public int compare(ByteString bs1, String s2) {
        final int len1 = bs1.getLength();
        final int len2 = s2.length();
        final int nn   = Math.min(len1, len2);
        for (int ii = 0; ii < nn; ii++) {
            final char c1 = bs1.charAt(ii);
            final char c2 = s2.charAt(ii);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    /**
     * _more_
     *
     * @param b1
     * @param b2
     * @param sep
     * @return _more_
     */
    public static byte[] concat(byte[] b1, byte[] b2, byte sep) {
        final byte[] ba = new byte[b1.length + b2.length + 1];

        if (b1.length > 0) {
            System.arraycopy(b1, 0, ba, 0, b1.length);
        }
        ba[b1.length] = sep;

        if (b2.length > 0) {
            System.arraycopy(b2, 0, ba, b1.length + 1, b2.length);
        }

        return ba;
    }

    /**
     * _more_
     *
     * @param bs1
     * @param bs2
     * @param sep
     * @return _more_
     */
    public static ByteString concat(ByteString bs1, ByteString bs2,
                                    char sep) {
        final int    l1 = bs1.getLength();
        final int    l2 = bs2.getLength();
        final byte[] ba = new byte[l1 + l2 + 1];

        bs1.copyBytes(0, ba, 0, l1);
        ba[l1] = CharToByte8859_1(sep);
        bs2.copyBytes(0, ba, l1 + 1, l2);

        return new ByteString.Concrete(ba, true);
    }

    /* Why does the compiler demand this when we have an inner class? */

    /**
     * _more_
     *
     */
    protected ByteString() {}

    /**
     * Returns the length of this string.
     * The length is equal to the number of byte elements in the string.
     *
     * @return  The number of bytes in the string
     */
    public abstract int getLength();

    /**
     * Returns the byte at the specified index. An index ranges
     * from <code>0</code> to <code>getLength() - 1</code>.
     * The first byte of the sequence is at index <code>0</code>,
     * the next at index <code>1</code>, and so on, as for array indexing.
     *
     * @param ii   the index of the character.
     *
     * @return     the byte at the specified index of this string.
     */
    public abstract byte byteAt(int ii);

    /**/

    /**
     * Returns the byte at the specified index, widened
     * to Unicode char.
     * @see #byteAt
     * @see java.lang.String#charAt(int)
     *
     * @param ii
     * @return _more_
     */
    public char charAt(int ii) {
        return ByteToChar8859_1(byteAt(ii));
    }

    /**
     * Copies bytes from this ByteString into the destination byte
     * array.
     * <p>
     * The first byte to be copied is at index <code>srcpos</code>;
     * The total number of bytes to be copied is
     * <code>nbytes</code>.  The bytes are copied into the subarray of
     * <code>dst</code> starting at index <code>dstpos</code>.
     *
     * @param      srcpos   int index of the first byte to copy.
     * @param      dst        the destination array.
     * @param      dstpos   int start index in the destination array.
     * @param      nbytes   int number of bytes to copy
     * @return _more_
     */
    /*
     * Implementation here in terms of byteAt().
     * Subclasses can often do better.
     */
    public byte[] copyBytes(int srcpos, byte dst[], int dstpos, int nbytes) {
        final int last = srcpos + nbytes;
        while (srcpos < last) {
            dst[dstpos++] = byteAt(srcpos++);
        }
        return dst;
    }

    /**
     * Converts this string to a new byte array.
     * @return a new byte array whose length is the
     * length of this and whose contents are initialized
     * to the byte sequence contained herein.
     */
    /*
     * Implementation here in terms of copyBytes().
     * Subclasses can often do better.
     */
    public byte[] toArray() {
        final int    nbytes = getLength();
        final byte[] ba     = new byte[nbytes];
        return copyBytes(0, ba, 0, nbytes);
    }


    /**
     * Returns a new string that is a substring of this string. The
     * substring begins at the specified <code>beginIndex</code> and
     * is <code>length</code> long.
     * <p>
     * The resulting substring will share the same byte storage as this.
     * <p>
     * Note: the parameters and name of this method are somewhat
     * different than <code>java.lang.String.substring(int,int)</code>
     * There is a <code>String</code> compatible version below.
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @param       length  int length of the substring.
     * @return     the specified substring.
     */
    /* default implementation makes a copy of the storage */
    public ByteString subByteString(int beginIndex, int length) {
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex < 0");
        }
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        if (beginIndex == 0) {
            if (length == getLength()) {
                return this;
            }
            return new ByteString.Concrete(this, length);
        }
        // else
        return new ByteString.Concrete(this, beginIndex, length);
    }

    /**
     * @see java.lang.String#substring(int,int)
     *
     * @param beginIndex
     * @param endindex
     * @return _more_
     */
    public ByteString substring(int beginIndex, int endindex) {
        return subByteString(beginIndex, endindex - beginIndex);
    }

    /**
     * @see java.lang.String#substring(int)
     *
     * @param beginIndex
     * @return _more_
     */
    /* default implementation makes a copy of the storage */
    public ByteString substring(int beginIndex) {
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex < 0");
        }
        if (beginIndex == 0) {
            return this;
        }
        return new ByteString.Concrete(this, beginIndex,
                                       getLength() - beginIndex);
    }

    /**
     * String compatible entry point for getLength();
     * @see #getLength()
     * @see java.lang.String#length()
     * @return _more_
     */
    public final int length() {
        return getLength();
    }

    /**
     * Compare this and another byte string lexographically.
     * @see #compare
     *
     * @param bs
     * @return _more_
     */
    public int compareTo(ByteString bs) {
        return compare(this, bs);
    }

    /**
     * Compare this and a String string lexographically.
     * @see #compare
     *
     * @param ss
     * @return _more_
     */
    public int compareTo(String ss) {
        return compare(this, ss);
    }

    /**
     * Compares this to another Object.  If the Object is a ByteString,
     * this function behaves like <code>compareTo(ByteString)</code>.
     * Otherwise, it throws a <code>ClassCastException</code>.
     *
     * @param oo
     * @return _more_
     */
    public int compareTo(Object oo) {
        return compareTo((ByteString) oo);
    }

    /**
     * Widen this to Unicode in a StringBuffer.
     *
     * @param buf
     * @return _more_
     */
    public StringBuffer format(StringBuffer buf) {

        final int len = getLength();
        for (int ii = 0; ii < len; ii++) {
            buf.append(charAt(ii));
        }
        return buf;
    }

    /**
     * _more_
     * @return _more_
     */
    public int hashCode() {
        /* Same computation as String.hashCode() */
        int       hash = 0;
        final int len  = getLength();
        for (int ii = 0; ii < len; ii++) {
            hash = 31 * hash + charAt(ii);
        }
        return hash;
    }

    /**
     * _more_
     *
     * @param oo
     * @return _more_
     */
    public boolean equals(Object oo) {
        if (this == oo) {
            return true;
        }
        if ((oo != null) && (oo instanceof ByteString)) {
            final ByteString other = (ByteString) oo;
            final int        len   = getLength();
            if (len != other.getLength()) {
                return false;
            }
            for (int ii = 0; ii < len; ii++) {
                if (byteAt(ii) != other.byteAt(ii)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true iff this and the String argument
     * represent the same sequence of characters.
     *
     * @param ss
     * @return _more_
     */
    public boolean equalS(String ss) {
        if (ss != null) {
            return (compareTo(ss) == 0);
        }
        return false;
    }

    /**
     * Widen this to a Unicode as in a String
     * @return _more_
     */
    public String toString() {
        return format(new StringBuffer(getLength())).toString();
    }

    /**/

    /**
     * Zero length byte array to use as the contents of Empty ByteString
     */
    static public final byte[] nada = new byte[0];

    /**
     * Class Empty
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    static public class Empty extends ByteString {

        /**
         * _more_
         * @return _more_
         */
        public int getLength() {
            return 0;
        }

        /**
         * _more_
         *
         * @param pos
         * @return _more_
         */
        public byte byteAt(int pos) {
            throw new IndexOutOfBoundsException();
        }

        /**
         * _more_
         *
         * @param srcpos
         * @param dst
         * @param dstpos
         * @param nbytes
         * @return _more_
         */
        public byte[] copyBytes(int srcpos, byte dst[], int dstpos,
                                int nbytes) {
            if (nbytes > 0) {
                throw new IndexOutOfBoundsException();
            }
            return dst;
        }

        /**
         * _more_
         * @return _more_
         */
        public byte[] toArray() {
            return nada;
        }

    }

    /**/

    /**
     * Class Concrete
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    static public class Concrete extends ByteString implements Serializable {

        /**
         * Storage for the data
         */
        protected final byte[] ba_;

        /**
         * Construct an empty ByteString
         * public
         * Concrete()
         * {
         *       ba_ = EMPTY.ba_;
         * }
         *
         * @param ba
         * @param share
         */

        /**
         * Constructs a new <code>ByteString</code> initialized to
         * contain the same sequence of bytes as the contained in
         * the byte array argument. If <code>share</code> is
         * <code>true</code> then the byte array argument is used
         * as backing storage. Caution: in this case, subsequent
         * modification of the byte array affects the newly created
         * ByteString. Otherwise, the contents of the byte array
         * are copied and subsequent modification of the byte array
         * does not affect the newly created ByteString.
         *
         * @param  ba   byte array which initializes contents of this
         * @param  share boolean, if false, make a private copy of ba
         */
        public Concrete(byte[] ba, boolean share) {
            if (share) {
                ba_ = ba;
            } else {
                synchronized (ba) {
                    ba_ = new byte[ba.length];
                    System.arraycopy(ba, 0, ba_, 0, ba.length);
                }
            }
        }

        /**
         * Copy constuctor.
         * Never shares storage with bs.
         *
         * @param bs
         */
        public Concrete(ByteString bs) {
            ba_ = bs.toArray();
        }

        /**
         * Copy constuctor.
         *
         * @param bs
         * @param share
         */
        protected Concrete(ByteString.Concrete bs, boolean share) {
            if (share) {
                ba_ = bs.ba_;
            } else {
                ba_ = bs.toArray();
            }
        }

        /**
         * Copy constuctor which trims input to length.
         * Used by subByteString and substring.
         * Never shares storage with bs.
         *
         * @param bs
         * @param length
         */
        public Concrete(ByteString bs, int length) {
            if (length > bs.getLength()) {
                throw new IllegalArgumentException("Invalid length");
            }
            ba_ = new byte[length];
            bs.copyBytes(0, ba_, 0, length);
        }

        /**
         * Copy constuctor which trims input to
         * [origin, origin + length).
         * Used by subByteString and substring.
         * Never shares storage with bs.
         *
         * @param bs
         * @param origin
         * @param length
         */
        public Concrete(ByteString bs, int origin, int length) {
            final int len = bs.getLength();
            if (origin > len) {
                throw new IllegalArgumentException("Invalid origin");
            }
            if (origin + length > len) {
                throw new IllegalArgumentException("Invalid length");
            }
            ba_ = new byte[length];
            bs.copyBytes(origin, ba_, 0, length);
        }

        /**
         * Construct a ByteString by narrowing a String.
         *
         * @param str
         */
        public Concrete(String str) {
            ba_ = str.getBytes();
        }

        /*
        public
        Concrete(String str, int begin, int end)
        {
                ba_ = new byte[end - begin];
                str.getBytes(begin, end, ba_, 0);
        }
         */

        /**
         * _more_
         * @return _more_
         */
        public int getLength() {
            return ba_.length;
        }

        /**
         * _more_
         *
         * @param ii
         * @return _more_
         */
        public byte byteAt(int ii) {
            return ba_[ii];
        }

        /**
         * _more_
         *
         * @param srcpos
         * @param dst
         * @param dstpos
         * @param nbytes
         * @return _more_
         */
        public byte[] copyBytes(int srcpos, byte dst[], int dstpos,
                                int nbytes) {
            System.arraycopy(ba_, srcpos, dst, dstpos, nbytes);
            return dst;
        }

        /**
         * _more_
         * @return _more_
         */
        public byte[] toArray() {
            return (byte[]) ba_.clone();
        }

        /**
         * _more_
         *
         * @param beginIndex
         * @param length
         * @return _more_
         */
        public ByteString subByteString(int beginIndex, int length) {
            if (beginIndex < 0) {
                throw new IllegalArgumentException("beginIndex < 0");
            }
            if (length < 0) {
                throw new IllegalArgumentException();
            }
            if (beginIndex == 0) {
                if (length == getLength()) {
                    return this;
                }
                return new TrSubString(this, length);
            }
            // else
            return new SubString(this, beginIndex, length);
        }

        /**
         * _more_
         *
         * @param beginIndex
         * @return _more_
         */
        public ByteString substring(int beginIndex) {
            if (beginIndex < 0) {
                throw new IllegalArgumentException("beginIndex < 0");
            }
            if (beginIndex == 0) {
                return this;
            }
            return new SubString(this, beginIndex, getLength() - beginIndex);
        }
    }

    /**/

    /**
     * A ByteString implementation which
     * which is zero based in the underlying byte array
     * but whose length is possibly less than the underlying
     * byte array. "Truncated SubString of ByteString".
     */
    static public class TrSubString extends Concrete {

        /** _more_ */
        protected final int length_;

        /**
         * _more_
         *
         * @param ba
         * @param share
         * @param length
         *
         */
        public TrSubString(byte[] ba, boolean share, int length) {
            super(ba, share);
            if (length > ba_.length) {
                throw new IllegalArgumentException("Invalid length");
            }
            length_ = length;
        }

        /**
         * _more_
         *
         * @param bs
         * @param length
         *
         */
        public TrSubString(ByteString.Concrete bs, int length) {
            super(bs.ba_, true);
            if (length > bs.getLength()) {
                throw new IllegalArgumentException("Invalid length");
            }
            length_ = length;
        }

        /**
         * _more_
         * @return _more_
         */
        public int getLength() {
            return length_;
        }

        /**
         * _more_
         *
         * @param ii
         * @return _more_
         */
        public byte byteAt(int ii) {
            if (ii > length_) {
                throw new IndexOutOfBoundsException();
            }
            return ba_[ii];
        }

        /**
         * _more_
         *
         * @param srcpos
         * @param dst
         * @param dstpos
         * @param nbytes
         * @return _more_
         */
        public byte[] copyBytes(int srcpos, byte dst[], int dstpos,
                                int nbytes) {
            System.arraycopy(ba_, srcpos, dst, dstpos, nbytes);
            return dst;
        }

        /**
         * _more_
         * @return _more_
         */
        public byte[] toArray() {
            return (byte[]) ba_.clone();
        }

    }

    /**
     * A ByteString implementation whose
     * origin is offset in the underlying byte array and
     * whose length is possibly less than the underlying
     * byte array. "SubString of ByteString".
     */
    static public class SubString extends TrSubString {

        /** _more_ */
        protected final int origin_;

        /**
         * _more_
         *
         * @param ba
         * @param share
         * @param origin
         * @param length
         *
         */
        public SubString(byte[] ba, boolean share, int origin, int length) {
            super(ba, share, length);
            if (origin > ba_.length) {
                throw new IllegalArgumentException("Invalid origin");
            }
            origin_ = origin;
            if (origin + length_ > ba_.length) {
                throw new IllegalArgumentException("Invalid length");
            }
        }

        /**
         * _more_
         *
         * @param sbs
         * @param origin
         * @param length
         *
         */
        public SubString(SubString sbs, int origin, int length) {
            super(sbs.ba_, true, length);
            origin += sbs.origin_;
            final int len = sbs.getLength();
            if (origin > len) {
                throw new IllegalArgumentException("Invalid origin");
            }
            origin_ = origin;
            if (origin + length > len) {
                throw new IllegalArgumentException("Invalid length");
            }
        }

        /**
         * _more_
         *
         * @param bs
         * @param origin
         * @param length
         *
         */
        public SubString(ByteString.Concrete bs, int origin, int length) {
            super(bs.ba_, true, length);
            final int len = bs.getLength();
            if (origin > len) {
                throw new IllegalArgumentException("Invalid origin");
            }
            origin_ = origin;
            if (origin + length > len) {
                throw new IllegalArgumentException("Invalid length");
            }
        }

        /**
         * _more_
         *
         * @param ii
         * @return _more_
         */
        public byte byteAt(int ii) {
            if ((ii < 0) || (ii > length_)) {
                throw new IndexOutOfBoundsException();
            }
            return ba_[origin_ + ii];
        }


        /**
         * _more_
         *
         * @param srcpos
         * @param dst
         * @param dstpos
         * @param nbytes
         * @return _more_
         */
        public byte[] copyBytes(int srcpos, byte dst[], int dstpos,
                                int nbytes) {
            if ((srcpos < 0) || (srcpos > length_)
                    || (srcpos + nbytes > length_)) {
                throw new IndexOutOfBoundsException();
            }
            System.arraycopy(ba_, srcpos + origin_, dst, dstpos, nbytes);
            return dst;
        }

        /**
         * _more_
         * @return _more_
         */
        public byte[] toArray() {
            final byte[] ba = new byte[length_];
            System.arraycopy(ba_, origin_, ba, 0, length_);
            return ba;
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
            java.io.BufferedReader rdr = new java.io.BufferedReader(
                                             new java.io.InputStreamReader(
                                                 System.in));
            Object prev = null;
            String line;
            for (line = rdr.readLine(); line != null;
                    line = rdr.readLine().trim()) {
                ByteString bs = new ByteString.Concrete(line);
                System.out.println("String hash: " + line.hashCode());
                System.out.print("       hash: " + bs.hashCode());
                System.out.println(" \"" + bs + "\"");
                if (prev != null) {
                    System.out.println("cmp " + bs.compareTo(prev));
                }
                prev = bs;
            }
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}

