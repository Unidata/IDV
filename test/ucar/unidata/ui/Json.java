/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_EOL;
import static java.io.StreamTokenizer.TT_WORD;


// Singleton class

/**
 * Class description
 *
 *
 * @version        Enter version here..., Tue, Aug 4, '15
 * @author         Enter your name here...    
 */
abstract public class Json {

    //////////////////////////////////////////////////
    // Constants

    /** _more_ */
    static final char LBRACKET = '[';

    /** _more_ */
    static final char RBRACKET = ']';

    /** _more_ */
    static final char LBRACE = '{';

    /** _more_ */
    static final char RBRACE = '}';

    /** _more_ */
    static final char COLON = ':';

    /** _more_ */
    static final char COMMA = ',';

    /** _more_ */
    static final char QUOTE = '"';

    /** _more_ */
    static final String TRUE = "true";

    /** _more_ */
    static final String FALSE = "false";

    //////////////////////////////////////////////////

    /**
     * _more_
     *
     * @param text _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    static public Object parse(String text) throws IOException {
        Parser parser = new Parser();
        return parser.parse(text);
    }

    //////////////////////////////////////////////////

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Aug 4, '15
     * @author         Enter your name here...    
     */
    static protected class Parser {

        /**
         * Construct a Parser 
         */
        public Parser() {}

        // SImple recursive descent

        /**
         * _more_
         *
         * @param text _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public Object parse(String text) throws IOException {
            StringReader    rdr    = new StringReader(text);
            StreamTokenizer tokens = new StreamTokenizer(rdr);
            tokens.eolIsSignificant(false);
            tokens.quoteChar(QUOTE);
            tokens.wordChars('a', 'z');
            tokens.wordChars('A', 'Z');
            tokens.wordChars('0', '9');
            tokens.wordChars('_', '_');
            tokens.wordChars('-', '-');
            tokens.wordChars('$', '$');
            Object result = parseR(tokens);
            return result;
        }

        /**
         * _more_
         *
         * @param tokens _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        protected Object parseR(StreamTokenizer tokens) throws IOException {
            int token = tokens.nextToken();
            switch (token) {

              case TT_EOF :
                  return null;

              case TT_WORD :
                  return parseAtomic(tokens);

              case LBRACE :
                  return parseMap(tokens);

              case LBRACKET :
                  return parseArray(tokens);

              case QUOTE :
                  return parseAtomic(tokens);

              default :
                  throw new IOException("Unexpected token:" + (char) token);
            }
        }

        /**
         * _more_
         *
         * @param tokens _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        protected Object parseAtomic(StreamTokenizer tokens)
                throws IOException {
            assert ((tokens.ttype == TT_WORD) || (tokens.ttype == QUOTE));
            String word = tokens.sval;
            if (tokens.ttype == QUOTE) {
                return word;
            }
            try {
                Long l = Long.decode(word);
                return l;
            } catch (NumberFormatException nfe) { /*ignore*/
            }
            ;
            if (word.equalsIgnoreCase(TRUE)) {
                return Boolean.TRUE;
            }
            if (word.equalsIgnoreCase(FALSE)) {
                return Boolean.FALSE;
            }
            return word;
        }

        /**
         * _more_
         *
         * @param tokens _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        protected Object parseArray(StreamTokenizer tokens)
                throws IOException {
            assert (tokens.ttype == LBRACKET);
            List<Object> array = new ArrayList<>();
            loop:
            for (;;) {
                int token = tokens.nextToken();
                switch (token) {

                  case TT_EOL :
                      break;  // ignore

                  case TT_EOF :
                      throw new IOException("Unexpected eof");

                  case RBRACKET :
                      break loop;

                  default :
                      tokens.pushBack();
                      Object o = parseR(tokens);
                      tokens.nextToken();
                      if (tokens.ttype == TT_EOF) {
                          break;
                      } else if (tokens.ttype == RBRACKET) {
                          tokens.pushBack();
                      } else if (tokens.ttype != COMMA) {
                          throw new IOException("Missing comma in list");
                      }
                      array.add(o);
                }
            }
            return array;
        }

        /**
         * _more_
         *
         * @param tokens _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        protected Object parseMap(StreamTokenizer tokens) throws IOException {
            assert (tokens.ttype == LBRACE);
            Map<String, Object> map = new LinkedHashMap<>();  // Keep insertion order
            loop:
            for (;;) {
                int token = tokens.nextToken();
                switch (token) {

                  case TT_EOL :
                      break;  // ignore

                  case TT_EOF :
                      throw new IOException("Unexpected eof");

                  case RBRACE :
                      break loop;

                  default :
                      tokens.pushBack();
                      Object name = parseR(tokens);
                      if (tokens.ttype == TT_EOF) {
                          break;
                      }
                      if ((name instanceof String) || (name instanceof Long)
                              || (name instanceof Boolean)) {
                          /*ok*/
                      } else {
                          throw new IOException("Unexpected map name type: "
                                  + name);
                      }
                      if (tokens.nextToken() != COLON) {
                          throw new IOException("Expected ':'; found: "
                                  + tokens.ttype);
                      }
                      Object o = parseR(tokens);
                      tokens.nextToken();
                      if (tokens.ttype == TT_EOF) {
                          break;
                      } else if (tokens.ttype == RBRACE) {
                          tokens.pushBack();
                      } else if (tokens.ttype != COMMA) {
                          throw new IOException("Missing comma in list");
                      }
                      map.put(name.toString(), o);
                }
            }
            return map;
        }
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    static public String toString(Object o) {
        StringBuilder buf = new StringBuilder();
        toStringR(o, buf, 0);
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param o _more_
     * @param buf _more_
     * @param indent _more_
     */
    static protected void toStringR(Object o, StringBuilder buf, int indent) {
        boolean first = true;
        if (o instanceof List) {
            List<Object> list = (List<Object>) o;
            if (list.size() == 0) {
                buf.append(LBRACKET);
                buf.append(RBRACKET);
            } else {
                buf.append(LBRACKET);
                buf.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    Object e = list.get(i);
                    buf.append(indent(indent));
                    toStringR(e, buf, indent + 2);
                    if (i < list.size() - 1) {
                        buf.append(",");
                    }
                    buf.append("\n");
                }
                buf.append(indent(indent));
                buf.append(RBRACKET);
            }
        } else if (o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            if (map.size() == 0) {
                buf.append(LBRACE);
                buf.append(RBRACE);
            } else {
                buf.append(LBRACE);
                buf.append('\n');
                int i = 0;
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    buf.append(indent(indent + 2));
                    buf.append(QUOTE);
                    buf.append(e.getKey());
                    buf.append(QUOTE);
                    buf.append(' ');
                    buf.append(COLON);
                    buf.append(' ');
                    toStringR(e.getValue(), buf, indent + 2);
                    if (i < map.size() - 1) {
                        buf.append(",");
                    }
                    buf.append("\n");
                    i++;
                }
                buf.append(indent(indent));
                buf.append(RBRACE);
            }
        } else if ((o instanceof Long) || (o instanceof Boolean)) {
            buf.append(o.toString());
        } else {
            buf.append(QUOTE);
            buf.append(o.toString());
            buf.append(QUOTE);
        }
    }

    /** _more_ */
    static String blanks =
        "                                                  ";

    /**
     * _more_
     *
     * @param n _more_
     *
     * @return _more_
     */
    static protected String indent(int n) {
        while (n > blanks.length()) {
            blanks = blanks + blanks;
        }
        return blanks.substring(0, n);
    }

}
