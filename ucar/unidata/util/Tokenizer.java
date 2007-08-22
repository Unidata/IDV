/*
 * $Id: Tokenizer.java,v 1.12 2006/05/05 19:19:38 jeffmc Exp $
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


import java.io.IOException;



import java.io.InputStream;


/**
 * @author Unidata
 */
public interface Tokenizer {

    /**
     * Tag interface for Tokens.
     * Typically, Token implementations extend ByteString.
     * Not all ByteStrings are Tokens tho.
     */
    static public final Token TK_NONE = new EmptyToken("TK_NONE");

    /** _more_ */
    static public final Token TK_EOF = new EmptyToken("TK_EOF");

    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    public abstract Token nextToken() throws IOException;

    /**
     * Token
     *
     *
     * @author Unidata
     */
    static public interface Token {

        /**
         * _more_
         * @return _more_
         */
        public abstract int getLength();

        /**
         * _more_
         * @return _more_
         */
        public abstract byte[] toArray();

        /**
         * _more_
         *
         * @param pos
         * @return _more_
         */
        public abstract byte byteAt(int pos);
    }


    /**
     * Class EmptyToken
     *
     *
     * @author Unidata
     */
    static public class EmptyToken extends ByteString.Empty implements Token {

        /** _more_ */
        private final String ident_;

        /**
         * _more_
         *
         * @param ident
         *
         */
        EmptyToken(String ident) {
            ident_ = ident;
        }

        /**
         * _more_
         * @return _more_
         */
        public String toString() {
            return "ucar.unidata.util.Tokenizer." + ident_;
        }
    }
}

