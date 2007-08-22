/*
 * $Id: ByteStringInputStream.java,v 1.10 2006/05/05 19:19:33 jeffmc Exp $
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



import java.io.InputStream;


/**
 * @author $Author: jeffmc $
 * @version $Revision: 1.10 $ $Date: 2006/05/05 19:19:33 $
 */
public class ByteStringInputStream extends InputStream {

    /** _more_ */
    protected final ByteString bs_;

    /** _more_ */
    protected int pos_ = 0;

    /** _more_ */
    protected int mark_ = 0;

    /**
     * _more_
     *
     * @param bs
     *
     */
    public ByteStringInputStream(ByteString bs) {
        bs_ = bs;
    }

    /**
     * _more_
     * @return _more_
     */
    public int read() {
        return (pos_ < bs_.getLength())
               ? (bs_.byteAt(pos_++) & 0xff)
               : -1;
    }

    /**
     * _more_
     *
     * @param dst
     * @param off
     * @param nbytes
     * @return _more_
     */
    public int read(byte dst[], int off, int nbytes) {
        if (dst == null) {
            throw new NullPointerException();
        }
        final int length = bs_.getLength();
        final int end    = off + nbytes;
        if ((off < 0) || (nbytes < 0) || (end > dst.length) || (end < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (pos_ >= length) {
            return -1;
        }
        if (end > length) {
            nbytes = length - pos_;
        }
        if (nbytes <= 0) {
            return 0;
        }


        bs_.copyBytes(pos_, dst, off, nbytes);
        pos_ += nbytes;
        return nbytes;
    }

    /**
     * _more_
     *
     * @param nbytes
     * @return _more_
     */
    public long skip(long nbytes) {
        int length = bs_.getLength();
        if (pos_ + nbytes > length) {
            nbytes = length - pos_;
        }
        if (nbytes < 0) {
            return 0;
        }
        pos_ += nbytes;
        return nbytes;
    }

    /**
     * _more_
     * @return _more_
     */
    public int available() {
        return bs_.getLength() - pos_;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * _more_
     *
     * @param readAheadLimit
     */
    public void mark(int readAheadLimit) {
        mark_ = pos_;
    }

    /**
     * _more_
     */
    public void reset() {
        pos_ = mark_;
    }

}

