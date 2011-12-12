/*
 * $Id: WMOHeader.java,v 1.8 2005/05/13 18:31:32 jeffmc Exp $
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

package ucar.unidata.metdata;



import ucar.unidata.util.ByteString;
import ucar.unidata.util.Timestamp;


/**
 * This class enscapsulates an WMO "Abbreviated heading", the
 * string used as an identifer on WMO format messages.
 *
 * Ref: _Manual on the Global Telecommunications System_, Volume 1,
 *      Global Aspects. WMO-No. 386, 1986
 *      Paragraph 2.3.2 page A.II-3
 *
 * @author Glenn Davis
 * @version $Revision: 1.8 $
 */
public class WMOHeader implements Comparable, java.io.Serializable {

    /* package */

    /** the text */
    final ByteString.Concrete text_;

    /** TTAA string */
    private final ByteString TTAA_;

    /** a byte */
    private final byte ii_;

    /** CCCC string */
    private final ByteString CCCC_;

    /** timestamp for header */
    private final Timestamp ts_;

    /**
     * Create a new WMOHeader
     *
     * @param src          source of the Header
     * @param TTAA         TTAA portion
     * @param ii           II portion
     * @param CCCC         CCCC portion
     * @param ts           timestamp
     */
    public WMOHeader(ByteString.Concrete src, ByteString.TrSubString TTAA,
                     byte ii, ByteString.SubString CCCC, Timestamp ts) {
        text_ = src;
        TTAA_ = TTAA;
        ii_   = ii;
        CCCC_ = CCCC;
        ts_   = ts;
    }

    /**
     * Get the timestamp
     * @return  timestamp
     */
    public Timestamp getTimestamp() {
        return ts_;
    }

    /**
     * Get the header
     * @return  the header text
     */
    public ByteString getText() {
        return text_;
    }

    /**
     * Returns the "international four letter location indicator
     * of the station originating or compiling the bulletin" (CCCC).
     *
     * @return  the CCCC portion
     */
    public ByteString getCallSign() {
        return CCCC_;
    }

    /**
     * Imposes a "natural order" on WMOHeaders.
     * <p>
     * Compares this object with the specified object for order.
     * Returns a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than the
     * specified object.
     * <p>
     * @see java.lang.Comparable
     * @param hdr the Object to be compared.
     *
     * @return  a negative integer, zero, or a
     *      positive integer as this object is less than, equal to,
     *      or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(WMOHeader hdr) {
        return text_.compareTo(hdr.text_);
    }

    /**
     * Compare to another object
     *
     * @param oo  Object to compare
     *
     * @return  a negative integer, zero, or a
     *      positive integer as this object is less than, equal to,
     *      or greater than the specified object.
     */
    public int compareTo(Object oo) {
        return compareTo((WMOHeader) oo);
    }

    /**
     * Return a String representation of this WMOHeader
     * @return a String representation of this WMOHeader
     */
    public String toString() {
        return text_.toString();
    }

    /**
     * Class Retransmit
     */
    static abstract class Retransmit {

        /** sequence number */
        private final byte seq_;

        /**
         * Create a new Retransmit
         *
         * @param seq  sequence number
         *
         */
        protected Retransmit(byte seq) {
            seq_ = seq;
        }
    }

    /**
     * Class RTD  for late transmissions
     */
    static class RTD extends Retransmit {

        /**
         * Create a new RTD
         *
         * @param seq  sequence number
         *
         */
        public RTD(byte seq) {
            super(seq);
        }
    }

    /**
     * Class COR  for corrections
     */
    static class COR extends Retransmit {

        /**
         * Create a new COR
         *
         * @param seq  sequence number
         *
         */
        public COR(byte seq) {
            super(seq);
        }
    }

    /**
     * Class AMD  for ammended retransmissions
     */
    static class AMD extends Retransmit {

        /**
         * Create a new AMD
         *
         * @param seq  sequence number
         */
        public AMD(byte seq) {
            super(seq);
        }
    }

    /**
     * Class PIE
     */
    static class PIE extends Retransmit {

        /**
         * Create a new PIE
         *
         * @param seq  sequence number
         *
         */
        public PIE(byte seq) {
            super(seq);
        }
    }

}
