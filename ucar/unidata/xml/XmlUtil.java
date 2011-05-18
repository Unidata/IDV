/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for Atmospheric Research
 * Copyright 2010- Jeff McWhirter
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
 * 
 */

package ucar.unidata.xml;


import org.w3c.dom.*;

import org.xml.sax.*;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;


import java.awt.Color;


import java.io.ByteArrayInputStream;



import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.SignatureException;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import javax.xml.parsers.*;



/**
 * A collection of utilities for xml.
 *
 * @author IDV development team
 */

public abstract class XmlUtil {

    /** The header to use when writing out xml */
    public static final String XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";


    /**
     *  used for matching any tag
     */
    public static final String TAG_WILDCARD = "*";

    /**
     *  Dummy ctor for doclint.
     */
    public XmlUtil() {}

    /**
     *  Just so we don't have to do (String) null.
     */
    public static final String NULL_STRING = null;

    /**
     *  Wrap the given string value in quotes.
     *
     *  @param value the String to wrap in quotes.
     *  @return The given string argument in quotes.
     */
    public static String quote(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Return the base64 encoded representation of the given byte array
     *
     * @param b The bytes to encode
     * @return The encoded string
     */
    public static String encodeBase64(byte[] b) {
        return org.apache.xerces.impl.dv.util.Base64.encode(b);
    }

    /**
     * Decode the given base64 String
     *
     * @param s Holds the base64 encoded bytes
     * @return The decoded bytes
     */
    public static byte[] decodeBase64(String s) {
        return org.apache.xerces.impl.dv.util.Base64.decode(s);
    }




    /**
     *  Create and return an xml comment.
     *
     *  @param value The String to comment.
     *  @return The value argument wrapped in an xml comment.
     */
    public static String comment(String value) {
        return "<!-- " + value + "-->\n";
    }

    /**
     *  Append onto the given StringBuffer name="value" string encode the value.
     *
     *  @param buff The string buffer to append onto.
     *  @param name The attribute name.
     *  @param value The attribute value.
     *  @param tab   The tab to prepend onto the result.
     */
    public static void attr(Appendable buff, String name, String value,
                            String tab) {
        try {
            buff.append(" ");
            buff.append(name);
            buff.append("=\"");
            //TODO
            value = value.trim();
            for (int i = 0; i < 5; i++) {
                value = StringUtil.replace(value, "  ", " ");
            }

            buff.append(encodeString(value));
            //        buff.append("\" ");

            //TODO
            buff.append("\"");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     *  Return a String with name="value". Encode the value.
     *
     *  @param name The attribute name.
     *  @param value The attribute value.
     *  @return The string &quot;name=value&quot;
     *
     */
    public static String attr(String name, String value) {
        return " " + name + "=" + quote(encodeString(value)) + " ";
    }

    /**
     * _more_
     *
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String attrs(String[] attrs) {
        StringBuffer a = new StringBuffer();
        for (int i = 0; i < attrs.length; i += 2) {
            a.append(attr(attrs[i], attrs[i + 1]));
        }
        return a.toString();
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attrs(String name, String value) {
        return " " + name + "=" + quote(encodeString(value)) + " ";
    }


    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2) {
        return attr(n1, v1) + attr(n2, v2);
    }

    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.  n3=&quot;v3&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @param n3 The third attribute name.
     *  @param v3 The third attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2,
                               String n3, String v3) {
        return attrs(n1, v1, n2, v2) + attr(n3, v3);
    }


    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.  n3=&quot;v3&quot.  n4=&quot;v4&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @param n3 The third attribute name.
     *  @param v3 The third attribute value.
     *  @param n4 The fourth attribute name.
     *  @param v4 The fourth attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2,
                               String n3, String v3, String n4, String v4) {
        return attrs(n1, v1, n2, v2, n3, v3) + attr(n4, v4);
    }


    /**
     *  Return a String of the form: &lt;name attrs &gt; contents &lt;/name&gt;
     *
     *  @param name The tag name.
     *  @param attrs The attribute section of the tag.
     *  @param contents The body of the tag
     *  @return The return xml.
     */
    public static String tag(String name, String attrs, String contents) {
        return "<" + name + ((attrs.length() > 0)
                             ? " "
                             : "") + attrs + ">" + contents + "</" + name
                                   + ">\n";
    }

    /**
     *  Return a String of the form: &lt;name attrs /&gt;
     *
     *  @param name The tag name.
     *  @param attrs The attribute section of the tag.
     *  @return The return xml.
     */
    public static String tag(String name, String attrs) {
        return "<" + name + ((attrs.length() > 0)
                             ? " "
                             : "") + attrs + "/>";
    }


    /**
     *  Return a String of the form: &lt;name attrs /&gt;
     *
     *  @param name The tag name.
     *  @param attrs The attribute section of the tag.
     *  @return The return xml.
     */
    public static String openTag(String name, String attrs) {
        return "<" + name + ((attrs.length() > 0)
                             ? " "
                             : "") + attrs + ">";
    }

    /**
     * Make an open tag
     *
     * @param name tag name
     *
     * @return the open tag
     */
    public static String openTag(String name) {
        return "<" + name + ">";
    }

    /**
     * Make a close tag
     *
     * @param name tag name
     *
     * @return the tag
     */
    public static String closeTag(String name) {
        return "</" + name + ">\n";
    }


    /**
     *  Get the given named attribute from the given element. If not found
     *  then recursively look in the parent of the given element.
     *  If the attribute is finally not found then null is returned.
     *
     *  @param element The xml node to look within.
     *  @param name The attribute name
     *  @return The value of the given attribute or null if not found.
     */
    public static String getAttributeFromTree(Node element, String name) {
        return getAttributeFromTree(element, name, null);
    }


    /**
     *  Get the given named attribute from the given element. If not found
     *  then recursively look in the parent of the given element.
     *  If the attribute is finally not found then the dflt argument is returned.
     *
     *  @param element The xml node to look within.
     *  @param name The attribute name.
     *  @param dflt The default value returned.
     *  @return The value of the given attribute or dflt if not found.
     */
    public static String getAttributeFromTree(Node element, String name,
            String dflt) {
        if (element == null) {
            return dflt;
        }
        String value = getAttribute(element, name, (String) null);
        if (value == null) {
            Node parent = element.getParentNode();
            if (parent != null) {
                value = getAttributeFromTree(parent, name, dflt);
            }
        }
        if (value == null) {
            return dflt;
        }
        return value;
    }





    /**
     *  Get the given named attribute from the given element. If not found
     *  then recursively look in the parent of the given element.
     *  If the attribute is finally not found then the dflt argument is returned.
     *  If it is found then convert the string value to an integer.
     *
     *  @param element The xml node to look within.
     *  @param name The attribute name.
     *  @param dflt The default value returned.
     *  @return The integer value of the attribute or the dflt if the attribute is not found.
     */
    public static int getAttributeFromTree(Node element, String name,
                                           int dflt) {
        if (element == null) {
            return dflt;
        }
        String value = getAttributeFromTree(element, name);
        if (value == null) {
            return dflt;
        }
        return Integer.decode(value).intValue();
    }


    /**
     * _more_
     *
     * @param element _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static boolean getAttributeFromTree(Node element, String name,
            boolean dflt) {
        if (element == null) {
            return dflt;
        }
        String value = getAttributeFromTree(element, name);
        if (value == null) {
            return dflt;
        }
        return new Boolean(value).booleanValue();
    }




    /**
     *  Get a list of  attributes  of the given name from the given element
     *  and all of its parent elements.
     *
     *  @param element The xml node to look within.
     *  @param name The attribute name.
     *  @param tags If non-null then only look at elements whose tag name is held by the tags hashtable.
     *  If the element we are looking at is not within the table then stop searching upwards.
     *  @return The list of attribute values.
     */

    public static List getAttributesFromTree(Node element, String name,
                                             Hashtable tags) {
        return getAttributesFromTree(element, name, tags, null);
    }


    /**
     *  Get a list of  attributes  of the given name from the given element
     *  and all of its parent elements.
     *
     *  @param element The xml node to look within.
     *  @param name The attribute name.
     *  @param tags If non-null then only look at elements whose tag name is held by the tags hashtable.
     *  If the element we are looking at is not within the table then stop searching upwards.
     *  @param listOfValues The list ot put the values in.
     *  @return The list of attribute values.
     */

    public static List getAttributesFromTree(Node element, String name,
                                             Hashtable tags,
                                             List listOfValues) {
        if ((tags != null) && (element instanceof Element)) {
            String tag = getLocalName(((Element) element));
            if (tags.get(tag) == null) {
                return listOfValues;
            }
        }
        Node parent = element.getParentNode();
        if (parent != null) {
            listOfValues = getAttributesFromTree(parent, name, tags,
                    listOfValues);
        }
        String value = getAttribute(element, name, (String) null);
        if (value != null) {
            if (listOfValues == null) {
                listOfValues = new ArrayList();
            }
            listOfValues.add(value);
        }
        return listOfValues;
    }




    /**
     * Does the given node have the attribute
     *
     * @param element node
     * @param attributeName attribute
     *
     * @return has attribute
     */
    public static boolean hasAttribute(Node element, String attributeName) {
        return getAttribute(element, attributeName, (String) null) != null;
    }


    /**
     *  Get the given name attribute from the given element.
     *  If the attribute is not found this will throw an IllegalArgumentException
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @return The value of the attribute.
     */
    public static String getAttribute(Node element, String name) {
        return getAttribute(element.getAttributes(), name);
    }


    /**
     *  Make sure that the given element contains the given attributes.
     *  If the element does not contain all of the given attribute names
     *  then an IllegalArgumentException is thrown.
     *
     *  @param element The xml element to look within.
     *  @param attrs Array of attribute names.
     */
    public static void ensureAttributes(Node element, String[] attrs) {
        for (int i = 0; i < attrs.length; i++) {
            getAttribute(element, attrs[i]);
        }
    }


    /**
     *  Get the given name-d attribute from the given element. If not found
     *  return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The attribute value or the dflt if not found.
     */
    public static String getAttribute(Node element, String name,
                                      String dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given element. Convert it into
     *  a boolean. If not found return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The boolean attribute value or the dflt if not found.
     *
     */
    public static boolean getAttribute(Node element, String name,
                                       boolean dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given element. Convert it into
     *  an int. If not found return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The int attribute value or the dflt if not found.
     */
    public static int getAttribute(Node element, String name, int dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given element. Convert it into
     *  a float. If not found return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The float attribute value or the dflt if not found.
     */
    public static float getAttribute(Node element, String name, float dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given element. Convert it into
     *  a double. If not found return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The double attribute value or the dflt if not found.
     */
    public static double getAttribute(Node element, String name,
                                      double dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given element. Convert it into
     *  a Color. If not found return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The Color attribute value or the dflt if not found.
     */
    public static Color getAttribute(Node element, String name, Color dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     *  Get the given name-d attribute from the given attrs map.
     *  If not found then throw an IllegalArgumentException.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @return The attribute value.
     */
    public static String getAttribute(NamedNodeMap attrs, String name) {
        String value = getAttribute(attrs, name, (String) null);
        if (value == null) {
            throw new IllegalArgumentException(
                "Could not find xml attribute:" + name);
        }
        return value;
    }

    /**
     *  Get the given name-d attribute from the given attrs map.
     *  If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static String getAttribute(NamedNodeMap attrs, String name,
                                      String dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : n.getNodeValue());
    }

    /**
     *  Get the given name-d attribute from the given attrs map. If found
     *  convert  it to int. If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static int getAttribute(NamedNodeMap attrs, String name,
                                   int dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : new Integer(n.getNodeValue()).intValue());
    }

    /**
     *  Get the given name-d attribute from the given attrs map. If found
     *  convert  it to float. If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static float getAttribute(NamedNodeMap attrs, String name,
                                     float dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : new Float(n.getNodeValue()).floatValue());
    }

    /**
     *  Get the given name-d attribute from the given attrs map. If found
     *  convert  it to double. If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static double getAttribute(NamedNodeMap attrs, String name,
                                      double dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : new Double(n.getNodeValue()).doubleValue());
    }

    /**
     *  Get the given name-d attribute from the given attrs map. If found
     *  convert  it to boolean. If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static boolean getAttribute(NamedNodeMap attrs, String name,
                                       boolean dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : new Boolean(n.getNodeValue()).booleanValue());
    }

    /**
     *  Get the given name-d attribute from the given attrs map. If found
     *  convert  it to Color ({@link ucar.unidata.util.GuiUtils#decodeColor}).
     *  If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public static Color getAttribute(NamedNodeMap attrs, String name,
                                     Color dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        if (n == null) {
            return dflt;
        }
        return GuiUtils.decodeColor(n.getNodeValue(), dflt);
    }


    /**
     * A utility  to set the attribute on the given node as the
     * String representation of the given color
     *
     * @param node The node
     * @param name The attr name
     * @param value The color
     */
    public static void setAttribute(Element node, String name, Color value) {
        node.setAttribute(name,
                          "" + value.getRed() + "," + value.getGreen() + ","
                          + value.getBlue());
    }



    /**
     *  Copy the attributes from n2 to n1.
     *
     *  @param n1 The source of the attributes.
     *  @param n2 What to copy into.
     */
    public static void mergeAttributes(Element n1, Element n2) {
        if ((n1 == null) || (n2 == null)) {
            return;
        }
        NamedNodeMap nnm = n2.getAttributes();
        if (nnm == null) {
            return;
        }
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            n1.setAttribute(attr.getNodeName(), attr.getNodeValue());
        }

    }

    /**
     *  The attrs parameter is an array of [name1, value1, name2, value2, etc].
     *  Set these attributes  on the given node.
     *
     *  @param node The  xml element to set attributes on.
     *  @param attrs The array of attribute name/value  pairs.
     */
    public static void setAttributes(Element node, String[] attrs) {
        for (int i = 0; i < attrs.length; i += 2) {
            node.setAttribute(attrs[i], attrs[i + 1]);
        }
    }

    /**
     *  Find the first child element of the given parent Node
     *  whose tag name.equals the given tag. Return null if not found.
     *
     *  @param parent The xml node to search for children.
     *  @param tag The tag name of the child xml element.
     *  @return The child found or null if not found.
     */
    public static Element findChild(Node parent, String tag) {
        List found = findChildren(parent, tag);
        if (found.size() > 0) {
            return (Element) found.get(0);
        }
        return null;
    }


    /**
     *  Find all of the children elements of the given parent Node
     *  and all of its ancestors  whose tag name.equals the given tag.
     *
     *  @param parent The xml node to search for children.
     *  @param tag The tag name of the child xml element.
     *  @return The List of children
     */
    public static List findChildrenRecurseUp(Node parent, String tag) {
        List results = findChildren(parent, tag);
        parent = parent.getParentNode();
        if (parent != null) {
            results.addAll(findChildrenRecurseUp(parent, tag));
        }
        return results;
    }



    /**
     *  Find the first child element of the given parent Node
     *  whose tag name.equals the given tag. If not found then
     *  recursively search up the ancestors of the parent.
     *
     *  @param parent The xml node to search for children.
     *  @param tag The tag name of the child xml element.
     *  @return The child found or null if not found.
     */
    public static Element findChildRecurseUp(Node parent, String tag) {
        Element child = findChild(parent, tag);
        if (child != null) {
            return child;
        }
        parent = parent.getParentNode();
        if (parent == null) {
            return null;
        }
        return findChildRecurseUp(parent, tag);
    }





    /**
     *  Find all of the  children elements of the given parent Node
     *  whose tag name.equals the given tag. Return an empty list if none found.
     *
     *  @param parent The xml node to search for children.
     *  @param tag The tag name of the child xml element.
     *  @return The list of children that match the given tag name.
     */
    public static List findChildren(Node parent, String tag) {
        ArrayList found    = new ArrayList();
        NodeList  children = parent.getChildNodes();
        boolean   doAll    = ((tag == null) || tag.equals(TAG_WILDCARD));
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            //if (doAll || getNodeName(child).equals(tag) || child.getNodeName().equals(tag)) {
            if (doAll || isTag(child, tag)) {
                found.add(child);
            }
        }
        return found;
    }


    /**
     *  Append the given list of children Elements to the given Element.
     *
     *  @param element The parent element.
     *  @param children The list of children.
     */
    public static void addChildren(Element element, List children) {
        for (int i = 0; i < children.size(); i++) {
            element.appendChild((Element) children.get(i));
        }
    }



    /**
     *  Find the first  descendant element of the given parent Node
     *  whose tag name.equals the given tag.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tag The tag name to match.
     *  @return The element foudn or null
     */
    public static Element findDescendant(Node parent, String tag) {
        ArrayList found = new ArrayList();
        findDescendants(parent, tag, found);
        if (found.size() == 0) {
            return null;
        }
        return (Element) found.get(0);
    }



    /**
     *  Find all of the  descendant elements of the given parent Node
     *  whose tag name.equals the given tag.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tag The tag name to match.
     *  @return The list of descendants that match the given tag.
     */
    public static List findDescendants(Node parent, String tag) {
        ArrayList found = new ArrayList();
        findDescendants(parent, tag, found);
        return found;
    }

    /**
     * This method searches, starting at the given parent,
     * for a descendant path. The path is  defined by the &quot;.&quot;
     * delimited path string. Each token in the path
     * is a tag name.
     * <p> So, given:<pre>path=Alabama.Alaska.Arkansas</pre>
     * this method will try to find the Arkansas tag
     * which is a child of Alaska, which is a child of
     * Alabama, which is a child of the given parent parameter.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param path The "." delimited string of tag names.
     *  @return The list of descendants that are under the given path.
     */
    public static Element findDescendantFromPath(Element parent,
            String path) {
        List results = new ArrayList();
        List tags    = StringUtil.split(path, ".");
        findDescendantsFromPath(parent, tags, 0, results, "\t");
        if (results.size() > 0) {
            return (Element) results.get(0);
        }
        return null;
    }

    /**
     * Like findDescendantFromPath, this method finds all descendants.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param path The "." delimited string of tag names.
     *  @return The list of descendants that are under the given path.
     */
    public static List findDescendantsFromPath(Element parent, String path) {
        List results = new ArrayList();
        List tags    = StringUtil.split(path, ".");
        findDescendantsFromPath(parent, tags, 0, results, "\t");
        return results;
    }



    /**
     *  UNTESTED!!!
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tags The list of descendant  tag names.
     *  @param tagIdx ???
     *  @results The list of descendants that are under the given path.
     * @param results
     * @param tab
     */
    private static void findDescendantsFromPath(Element parent, List tags,
            int tagIdx, List results, String tab) {
        String  tag     = (String) tags.get(tagIdx);
        boolean lastTag = (tagIdx == tags.size() - 1);

        //        System.err.println (tab+getLocalName(parent) + " looking for:" + tag + " idx:" + tagIdx+ " lastTag:" + lastTag);
        NodeList elements = getElements(parent);
        tab = tab + "\t";
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            //            System.err.println (tab+">child:" + getLocalName(child));
            if (tag.equals(TAG_WILDCARD) || isTag(child, tag)) {
                if (lastTag) {
                    results.add(child);
                } else {
                    findDescendantsFromPath(child, tags, tagIdx + 1, results,
                                            tab);
                }
            }
        }
    }



    /**
     *  Find all of the  descendant elements of the given parent Node
     *  whose tag name equals the given tag.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tag The tag name to match.
     *  @param found The list of descendants that match the given tag.
     */
    private static void findDescendants(Node parent, String tag, List found) {
        if (getNodeName(parent).equals(tag)) {
            found.add(parent);
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            findDescendants(child, tag, found);
        }
    }


    /**
     *  Parse the given xml and return the root Element.
     *
     *  @param xml The xml.
     *  @return The root of the xml dom.
     *  @throws Exception When something goes wrong.
     */
    public static Element getRoot(String xml) throws Exception {
        return getDocument(xml).getDocumentElement();
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(String tag, Element parent)
            throws Exception {
        return create(parent.getOwnerDocument(), tag, parent);
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(String tag, Element parent, List attrs)
            throws Exception {
        return create(parent.getOwnerDocument(), tag, parent, attrs);
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag) throws Exception {
        return create(doc, tag, (String[]) null);
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, Element parent)
            throws Exception {
        Element child = doc.createElement(tag);
        if (parent != null) {
            parent.appendChild(child);
        }
        return child;
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param parent _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, Element parent,
                                 List attrs)
            throws Exception {
        Element child = create(doc, tag, parent);
        if (attrs != null) {
            setAttributes(child, Misc.listToStringArray(attrs));
        }
        return child;
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, String[] attrs)
            throws Exception {
        return create(doc, tag, (Element) null, attrs);
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param parent _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, Element parent,
                                 String[] attrs)
            throws Exception {
        Element child = create(doc, tag, parent);
        if (attrs != null) {
            setAttributes(child, attrs);
        }
        return child;
    }

    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, Element parent,
                                 String text, String[] attrs)
            throws Exception {
        Element child = create(doc, tag, parent, attrs);
        if (text != null) {
            Text textNode = doc.createTextNode(text);
            child.appendChild(textNode);
        }
        return child;
    }

    /**
     * _more_
     *
     * @param doc _more_
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(Document doc, String tag, Element parent,
                                 String text)
            throws Exception {
        return create(doc, tag, parent, text, null);
    }




    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(String tag, Element parent, String[] attrs)
            throws Exception {
        return create(parent.getOwnerDocument(), tag, parent, attrs);
    }

    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     * @param attrs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(String tag, Element parent, String text,
                                 String[] attrs)
            throws Exception {
        return create(parent.getOwnerDocument(), tag, parent, text, attrs);
    }

    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element create(String tag, Element parent, String text)
            throws Exception {
        return create(parent.getOwnerDocument(), tag, parent, text);
    }




    /**
     *  Read in the xml contained in the given filename, parse it and return the
     *  root Element. If the filename cannot be found then return null
     *
     *  @param filename  The filename, url or resource path of the xml document.
     *  @param originClass Where to look for resources.
     *  @return The root of the xml dom.
     *  @throws Exception When something goes wrong.
     */
    public static Element getRoot(String filename, Class originClass)
            throws Exception {
        Document doc = getDocument(filename, originClass);
        if (doc == null) {
            return null;
        }
        return doc.getDocumentElement();
    }

    /**
     *  Read in the xml contained in the given filename, parse it and return the
     *  root Element.
     *
     *
     * @param stream _more_
     *  @return The root of the xml dom.
     *  @throws Exception When something goes wrong.
     */
    public static Element getRoot(InputStream stream) throws Exception {
        return getDocument(stream).getDocumentElement();
    }



    /**
     *  Read in the xml contained in the given filename, parse it and return the
     *  root Element.
     *
     *  @param filename  The filename, url or resource path of the xml document.
     *  @param originClass Where to look for resources.
     *  @return The root of the xml dom.
     *  @throws Exception When something goes wrong.
     */
    public static Document getDocument(String filename, Class originClass)
            throws Exception {
        String     xml;
        final List errors = new ArrayList();
        if (filename.startsWith("xml:")) {
            xml = filename.substring(4);
        } else {
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = null;
            try {
                is = IOUtil.getInputStream(filename, XmlUtil.class);
            } catch (Exception exc) {
                return null;
            }
            MyErrorHandler errorHandler = new MyErrorHandler();
            builder.setErrorHandler(errorHandler);
            try {
                ucar.unidata.util.Trace.call1("XmlUtil.getDocument");
                Document doc = builder.parse(is);
                ucar.unidata.util.Trace.call2("XmlUtil.getDocument");
                return doc;
            } catch (Exception exc) {
                throw new IllegalStateException("Error parsing xml: "
                        + filename + "\n" + exc.getMessage() + "\n"
                        + errorHandler.errors);
            }

        }
        if (xml == null) {
            return null;
        }
        return getDocument(xml);
    }


    /**
     *  Find the root element of the given node.
     *
     *  @param child The xml element to serach for the root of.
     *  @return The root of the DOM tree that the given child exists in.
     */
    public static Element findRoot(Element child) {
        Node parent = child.getParentNode();
        while ((parent != null) && (parent instanceof Element)) {
            child  = (Element) parent;
            parent = child.getParentNode();
        }
        return child;
    }


    /**
     * Checks if the tag name of the given  node matches the given name.
     * If the given name is fully qualified (e.g., namespace:tagname) then
     * check if it matches the full name of the node.
     * If the node name is fully qualified and the name isn't then strip off
     * the namespace of the node and compare
     * else just compare the 2
     *
     *
     * @param node the xml node
     * @param name name
     *
     * @return is non qualified tag name the same
     */
    public static boolean isTag(Node node, String name) {
        if ((name == null) || name.equals(TAG_WILDCARD)) {
            return true;
        }
        String nodeName = node.getNodeName();
        if (isFullyQualified(name)) {
            return nodeName.equals(name);
        }
        if (isFullyQualified(nodeName)) {
            return Misc.equals(getLocalName(node), name);
        }
        return Misc.equals(nodeName, name);
    }




    /**
     * Get the non qualified tag name
     *
     * @param element element
     *
     * @return tag name
     */
    public static String getLocalName(Node element) {
        String localName = element.getLocalName();
        if (localName != null) {
            return localName;
        }
        String name = element.getNodeName();
        int    idx  = name.indexOf(":");
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        return name;
    }


    /**
     * _more_
     *
     * @param tagName _more_
     *
     * @return _more_
     */
    public static boolean isFullyQualified(String tagName) {
        return tagName.indexOf(":") >= 0;
    }

    /**
     * Get the non qualified tag name
     *
     * @param node node
     *
     * @return node name
     */
    public static String getNodeName(Node node) {
        String localName = node.getLocalName();
        if (localName != null) {
            return localName;
        }
        String name = node.getNodeName();
        int    idx  = name.indexOf(":");
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        return name;
    }





    /**
     *  Find the ancestor of the given node with the given tagname
     *
     *  @param child The xml element to serach for the root of.
     * @param tagName The tag name to look for
     *  @return The ancestor
     */
    public static Element findAncestor(Element child, String tagName) {
        Object parentObj = child.getParentNode();
        if ( !(parentObj instanceof Element)) {
            return null;
        }
        Element parent = (Element) parentObj;
        while (parent != null) {
            if (isTag(parent, tagName)) {
                return parent;
            }
            parentObj = parent.getParentNode();
            if ( !(parentObj instanceof Element)) {
                return null;
            }
            parent = (Element) parentObj;
        }
        return null;
    }


    /**
     * A utility to make an empty document
     *
     * @return An empty document.
     */
    public static Document makeDocument() {
        try {
            return getDocument("");
        } catch (Exception exc) {
            System.err.println("Error making document: " + exc);
            return null;
        }
    }

    /**
     * Class MyErrorHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.81 $
     */
    private static class MyErrorHandler implements ErrorHandler {

        /** _more_ */
        StringBuffer errors = new StringBuffer();

        /**
         * _more_
         */
        public MyErrorHandler() {}

        /**
         * _more_
         *
         * @param exception _more_
         */
        public void error(SAXParseException exception) {
            handleError(exception);
        }

        /**
         * _more_
         *
         * @param exception _more_
         */
        public void fatalError(SAXParseException exception) {
            handleError(exception);
        }

        /**
         * _more_
         *
         * @param exception _more_
         */
        public void warning(SAXParseException exception) {}

        /**
         * _more_
         *
         * @param e _more_
         */
        private void handleError(SAXParseException e) {
            errors.append(e.getMessage() + " line:" + e.getLineNumber()
                          + ((e.getColumnNumber() >= 0)
                             ? (" column:" + e.getColumnNumber())
                             : "") + "\n");

        }
    }



    /**
     *  Create a Document object with the given xml.
     *
     *  @param xml The xml.
     *  @return A new Document.
     *  @throws Exception When something goes wrong.
     */
    public static Document getDocument(String xml) throws Exception {
        xml = xml.trim();
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        if (xml.length() == 0) {
            return builder.newDocument();
        }
        MyErrorHandler errorHandler = new MyErrorHandler();
        builder.setErrorHandler(errorHandler);
        try {
            return builder.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception exc) {
            //            System.err.println("OOps:" + xml);
            throw new IllegalStateException("Error parsing xml: "
                                            + exc.getMessage() + "\n"
                                            + errorHandler.errors);
        }
        /*
        DOMParser parser = new DOMParser();
        parser.parse(
            new InputSource(new ByteArrayInputStream(xml.getBytes())));
        return  parser.getDocument();
        */
    }



    /**
     *  Create a Document object with the given xml.
     *
     *  @param stream stream
     *  @return A new Document.
     *  @throws Exception When something goes wrong.
     */
    public static Document getDocument(InputStream stream) throws Exception {
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        try {
            return builder.parse(stream);
        } catch (Exception exc) {
            throw exc;
        }
        /*
        DOMParser parser = new DOMParser();
        parser.parse(
            new InputSource(new ByteArrayInputStream(xml.getBytes())));
        return  parser.getDocument();
        */
    }






    /**
     *  Get the first Element child of the given parent Element.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return The first child of the given node or null if there are none.
     */
    public static Element getFirstChild(Element parent) {
        NodeList nodeList = getElements(parent);
        if (nodeList.getLength() == 0) {
            return null;
        }
        return (Element) nodeList.item(0);
    }

    /**
     *  Get all children of the given parent Element who are instances of
     *  the Element class.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return All Element children of the given parent.
     */
    public static NodeList getElements(Element parent) {
        return getElements(parent, new XmlNodeList());
    }




    /**
     *  Get all children of the given parent Element who are instances of
     *  the Element class.
     *
     *  @param parent The xml node to search its chidlren.
     * @param nodeList list to add to
     *  @return All Element children of the given parent.
     */
    public static NodeList getElements(Element parent, XmlNodeList nodeList) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                nodeList.add(o);
            }
        }
        return nodeList;
    }


    /**
     *  Get all grand children of the given parent Element who are instances of
     *  the Element class.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return All Element children of the given parent.
     */
    public static NodeList getGrandChildren(Element parent) {
        XmlNodeList nodeList = new XmlNodeList();
        NodeList    children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                getElements((Element) o, nodeList);
            }
        }
        return nodeList;
    }




    /**
     * Finds the child node with the given tag name. Then gets the child text from that node if it is non-null.
     *
     *  @param parent The xml node to search its chidlren.
     * @param childTag _more_
     * @param dflt _more_
     *  @return The text values contained by the children of the given parent.
     */
    public static String getGrandChildText(Node parent, String childTag,
                                           String dflt) {
        String text = getGrandChildText(parent, childTag);
        if (text == null) {
            return dflt;
        }
        if (text.length() == 0) {
            return dflt;
        }
        return text;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param childTag _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static double getGrandChildValue(Node parent, String childTag,
                                            double dflt) {
        String text = getGrandChildText(parent, childTag);
        if (text == null) {
            return dflt;
        }
        text = text.trim();
        if (text.length() == 0) {
            return dflt;
        }
        return new Double(text).doubleValue();
    }


    /**
     * Finds the child node with the given tag name. Then gets the child text from that node if it is non-null.
     *
     *  @param parent The xml node to search its chidlren.
     * @param childTag _more_
     *  @return The text values contained by the children of the given parent.
     */
    public static String getGrandChildText(Node parent, String childTag) {
        Node child = findChild(parent, childTag);
        if (child != null) {
            return getChildText(child);
        }
        return null;
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param bytes _more_
     */
    public static void appendCdataBytes(StringBuffer sb, byte[] bytes) {
        sb.append("<![CDATA[");
        sb.append(encodeBase64(bytes));
        sb.append("]]>");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     */
    public static void appendCdata(StringBuffer sb, String s) {
        sb.append("<![CDATA[");
        sb.append(s);
        sb.append("]]>");
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String getCdata(String s) {
        return "<![CDATA[" + s + "]]>";
    }



    /**
     * _more_
     *
     * @param doc _more_
     * @param text _more_
     *
     * @return _more_
     */
    public static CDATASection makeCDataNode(Document doc, String text) {
        return makeCDataNode(doc, text, true);
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param text _more_
     */
    public static void createCDataNode(Element parent, String text) {
        parent.appendChild(makeCDataNode(parent.getOwnerDocument(), text));
    }



    /**
     * _more_
     *
     * @param doc _more_
     * @param text _more_
     * @param andEncode _more_
     *
     * @return _more_
     */
    public static CDATASection makeCDataNode(Document doc, String text,
                                             boolean andEncode) {
        if (andEncode) {
            return doc.createCDATASection(
                XmlUtil.encodeBase64(text.getBytes()));
        } else {
            return doc.createCDATASection(text);
        }
    }


    /**
     *  Concatenates the node values (grom getNodeValue) of the  children of the given parent Node.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return The text values contained by the children of the given parent.
     */
    public static String getChildText(Node parent) {
        if (parent == null) {
            return null;
        }
        NodeList     children = parent.getChildNodes();
        StringBuffer sb       = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child.getNodeType() == Node.TEXT_NODE)
                    || (child.getNodeType() == Node.CDATA_SECTION_NODE)) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString();
    }



    /**
     *  Get all Element children of the given parent Element with the
     *  given tagName.
     *
     *  @param parent The xml node to search its children.
     *  @param tagName The tag to match.
     *  @return The Element children of the given parent node whose tags match the given tagName.
     */
    public static XmlNodeList getElements(Element parent, String tagName) {
        XmlNodeList nodeList = new XmlNodeList();
        NodeList    children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                Element e = (Element) o;
                if ((tagName == null) || isTag(e, tagName)) {
                    nodeList.add(e);
                }
            }
        }
        return nodeList;
    }


    /**
     *  Get the first  Element children of the given parent Element with the  given tagName.
     *
     *  @param parent The xml node to search its children.
     *  @param tagName The tag to match.
     *  @return The first Element child that matches the given tag name.
     */
    public static Element getElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                Element e = (Element) o;
                if (isTag(e, tagName)) {
                    return e;
                }
            }
        }
        return null;
    }


    /**
     *  Create and return a List of the Element children of the given parent node.
     *
     *  @param parent The xml node to find its children.
     *  @return List of children Elements.
     */
    public static List getListOfElements(Element parent) {
        NodeList elements = getElements(parent);
        List     result   = new ArrayList();
        for (int i = 0; i < elements.getLength(); i++) {
            result.add(elements.item(i));
        }
        return result;
    }



    /**
     *  Return the default xml header.
     *
     *  @return The default xml document header.
     */
    public static String getHeader() {
        return XML_HEADER;
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml String representation.
     *
     *  @param node The xml node to convert its tree.
     *  @return The String representation of the node's tree.
     */
    public static String toString(Node node) {
        return toString(node, "  ", "\n");
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml String representation.
     *
     *  @param node The xml node to convert its tree.
     *  @return The String representation of the node's tree.
     */
    public static String toStringNoChildren(Node node) {
        return toString(node, "  ", "\n", false);
    }



    /**
     *  Convert the DOM rooted at the given Node to an xml String representation.
     * Prepend the xml header.
     *
     *  @param node The xml node to convert its tree.
     *  @return The String representation of the node's tree.
     */
    public static String toStringWithHeader(Node node) {
        return toStringWithHeader(node, "  ", "\n");
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml String representation.
     *
     *  @param node The xml node to convert its tree.
     *  @param prettyPrint Should the string be formatted with
     *                     tabs and new lines
     *  @return The String representation of the node's tree.
     */
    public static String toString(Node node, boolean prettyPrint) {
        if (prettyPrint) {
            return toString(node);
        }
        return toString(node, "", "");
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml representation. Use the
     *  given tabSpacing to include spacing within the hierarchy.
     *
     *  @param node The xml tree root.
     *  @param tabSpacing The spacing to put into the result string before each tag.
     *  @param newLine The new line to output after each tag.
     *  @return The String representation of the node's tree.
     */
    public static String toString(Node node, String tabSpacing,
                                  String newLine) {
        return toString(node, tabSpacing, newLine, true);
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml representation. Use the
     *  given tabSpacing to include spacing within the hierarchy.
     *
     *  @param node The xml tree root.
     *  @param tabSpacing The spacing to put into the result string before each tag.
     *  @param newLine The new line to output after each tag.
     * @param recurse _more_
     *  @return The String representation of the node's tree.
     */
    public static String toString(Node node, String tabSpacing,
                                  String newLine, boolean recurse) {
        StringBuffer xml = new StringBuffer();
        //xml.append(getHeader()+"\n");
        toString(xml, node, "", tabSpacing, newLine, false, recurse);
        return xml.toString();
        //      String result = encodeString (xml.toString ());
        //      return  result;
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml representation. Use the
     *  given tabSpacing to include spacing within the hierarchy. Prepend the xml header
     * to the text.
     *
     *  @param node The xml tree root.
     *  @param tabSpacing The spacing to put into the result string before each tag.
     *  @param newLine The new line to output after each tag.
     *  @return The String representation of the node's tree.
     */
    public static String toStringWithHeader(Node node, String tabSpacing,
                                            String newLine) {
        return toStringWithHeader(node, tabSpacing, newLine, false);
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param appendable _more_
     */
    public static void toString(Node node, Appendable appendable) {
        toString(appendable, node, "", "", "", false, true);
    }


    /**
     * Convert the xml to a string. Add the xml header
     *
     * @param node xml root
     * @param tabSpacing  tab
     * @param newLine new line
     * @param prettifyAttrs If true then layout attributes
     *
     * @return String representation of the given xml node
     */
    public static String toStringWithHeader(Node node, String tabSpacing,
                                            String newLine,
                                            boolean prettifyAttrs) {
        StringBuffer xml = new StringBuffer(getHeader() + "\n");
        toString(xml, node, "", tabSpacing, newLine, prettifyAttrs, true);
        return xml.toString();
    }


    /**
     *  Convert the DOM rooted at the given Node to an xml representation.
     *  Append the results to the given xml StringBuffer. currentTab is the result of
     *  the successive concatentation  of the tabSpacing argument.
     *
     *  @param xml The StringBuffer to append to.
     *  @param node The xml tree root.
     *  @param currentTab How may tabs we are currently indenting.
     *  @param tabSpacing The spacing to put into the result string before each tag.
     *  @param newLine The new line to output after each tag.
     * @param prettifyAttrs Format attributes
     * @param recurse _more_
     */
    private static void toString(Appendable xml, Node node,
                                 String currentTab, String tabSpacing,
                                 String newLine, boolean prettifyAttrs,
                                 boolean recurse) {

        try {
            int type = node.getNodeType();

            switch (type) {

              case Node.DOCUMENT_NODE : {
                  xml.append(getHeader());
                  break;
              }

              case Node.ELEMENT_NODE : {
                  xml.append(currentTab);
                  xml.append('<');
                  xml.append(node.getNodeName());
                  NamedNodeMap nnm     = node.getAttributes();
                  String       nextTab = currentTab + tabSpacing;
                  String       attrTab = nextTab;
                  if (nnm != null) {
                      for (int i = 0; i < nnm.getLength(); i++) {
                          Attr attr = (Attr) nnm.item(i);
                          if (prettifyAttrs && (nnm.getLength() > 2)) {
                              xml.append("\n");
                              xml.append(attrTab);
                          }
                          attr(xml, attr.getNodeName(), attr.getNodeValue(),
                               attrTab);
                      }
                  }
                  boolean  wasText     = false;
                  int      cnt         = 0;
                  NodeList children    = node.getChildNodes();
                  int      numChildren = children.getLength();
                  if (recurse) {
                      for (int i = 0; i < children.getLength(); i++) {
                          Node child = children.item(i);
                          wasText = ((child.getNodeType() == Node.TEXT_NODE)
                                     || (child.getNodeType()
                                         == Node.CDATA_SECTION_NODE));
                          if (cnt == 0) {
                              xml.append(">");
                              if ( !wasText || (numChildren > 1)) {
                                  xml.append(newLine);
                              }
                          }
                          toString(xml, child, nextTab, tabSpacing, newLine,
                                   prettifyAttrs, true);
                          cnt++;
                      }
                  }
                  if (cnt == 0) {
                      xml.append("/>");
                      xml.append(newLine);
                  } else {
                      if ( !wasText || (cnt > 1)) {
                          xml.append(currentTab);
                      }
                      xml.append("</");
                      xml.append(node.getNodeName());
                      xml.append(">");
                      xml.append(newLine);
                  }

                  break;
              }

              case Node.ENTITY_REFERENCE_NODE : {
                  xml.append('&' + node.getNodeName() + ';');
                  break;
              }

              case Node.CDATA_SECTION_NODE : {
                  String value = node.getNodeValue();
                  if (value != null) {
                      if (value.startsWith("XmlUtil.COMMENT:")) {
                          xml.append("\n<!--" + value.substring(16)
                                     + " -->\n");
                      } else {
                          xml.append("<![CDATA[" + value + "]]>");
                      }
                  }
                  break;
              }

              case Node.TEXT_NODE : {
                  //Trim whitespace
                  String v = node.getNodeValue();
                  if (v == null) {
                      break;
                  }
                  xml.append(encodeString(v));
                  break;
              }

              case Node.PROCESSING_INSTRUCTION_NODE : {
                  xml.append("<?" + node.getNodeName());
                  String data = node.getNodeValue();
                  if ((data != null) && (data.length() > 0)) {
                      xml.append(" " + data);
                  }
                  xml.append("?>");
                  break;
              }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }



    /**
     * _more_
     *
     * @param html _more_
     * @param node _more_
     */
    public static void toHtml(StringBuffer html, Node node) {
        switch (node.getNodeType()) {

          case Node.ELEMENT_NODE : {
              NodeList children    = node.getChildNodes();
              int      numChildren = children.getLength();
              html.append("<b>" + node.getNodeName().replace("_", " ")
                          + "</b>");
              html.append(": ");

              for (int i = 0; i < numChildren; i++) {
                  Node child = children.item(i);
                  if (((child.getNodeType() == Node.TEXT_NODE)
                          || (child.getNodeType()
                              == Node.CDATA_SECTION_NODE))) {
                      String v = child.getNodeValue();
                      if (v == null) {
                          continue;
                      }
                      if (v.trim().length() == 0) {
                          continue;
                      }
                      html.append(v);
                      html.append(" ");
                  }
              }
              boolean      didone = false;
              NamedNodeMap nnm    = node.getAttributes();
              if (nnm != null) {
                  for (int i = 0; i < nnm.getLength(); i++) {
                      Attr   attr     = (Attr) nnm.item(i);
                      String attrName = attr.getNodeName();
                      if (attrName.startsWith("xmlns")
                              || attrName.startsWith("xsi:")) {
                          continue;
                      }
                      if ( !didone) {
                          html.append("<ul>");
                          didone = true;
                      }
                      html.append(attrName.replace("_", " ") + "="
                                  + attr.getNodeValue());
                      html.append("<br>\n");
                  }
              }
              int cnt = 0;
              for (int i = 0; i < numChildren; i++) {
                  Node child = children.item(i);
                  if (((child.getNodeType() == Node.TEXT_NODE)
                          || (child.getNodeType()
                              == Node.CDATA_SECTION_NODE))) {
                      continue;
                  }
                  if ( !didone) {
                      html.append("<ul>");
                      didone = true;
                  }
                  if (cnt > 0) {
                      html.append("<br>");
                  }
                  toHtml(html, child);
                  cnt++;
              }
              if (didone) {
                  html.append("</ul>");
              }
              break;
          }
        }
    }



    /**
     *  Do a simple conversion  of special characters to their encoding.
     *
     *  @param v The source string.
     *  @return The encoded String.
     */
    public static String encodeString(String v) {
        return StringUtil.replaceList(v, new String[] { "&", "\"", "<",
                ">" }, new String[] { "&amp;",
                                      "&quot;", "&lt;", "&gt;" });
    }


    /**
     *  Find the first Element in the given NodeList of Elements
     *  that has the given attribute  with value .equals the
     *  given attributeValue.
     *
     *  @param elements The Elements to search.
     *  @param attributeName The attribute name to look for.
     *  @param attributeValue The value to match.
     *  @return The Element in the elements list that has an attribute of the given name with
     *  the given value.
     */
    public static Element findElement(NodeList elements,
                                      String attributeName,
                                      String attributeValue) {
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String  attr    = element.getAttribute(attributeName);
            if (attr == null) {
                continue;
            }
            if (attr.equals(attributeValue)) {
                return element;
            }
        }
        return null;
    }



    /**
     *  Find the Element under (recursively) the given root  Element
     *  with the given tag name (if non-null) that contains
     *  an attribute  of the given name and value.
     *
     *  @param root The Element to seatrch under.
     *  @param tag The tag name of the descendant Element to match.
     *  @param attributeName The attribute name to look for.
     *  @param attributeValue The value to match.
     *  @return The descendant Element that has an attribute of the given name with
     *  the given value.
     */
    public static Element findElement(Element root, String tag,
                                      String attributeName,
                                      String attributeValue) {
        if (tag != null) {
            if (isTag(root, tag)) {}
        }

        NodeList elements = getElements(root);
        //First try breadth first
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if ((tag == null) || isTag(child, tag)) {
                String attr = child.getAttribute(attributeName);
                if (attr != null) {
                    if (attr.equals(attributeValue)) {
                        return child;
                    }
                }
            }
        }

        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            Element found = findElement(child, tag, attributeName,
                                        attributeValue);
            if (found != null) {
                return found;
            }
        }

        return null;
    }




    /**
     *  Remove all children from the given parent Element.
     *
     *  @param parent The xml element to remove children from.
     */
    public static void removeChildren(Element parent) {
        Node child;
        while ((child = parent.getFirstChild()) != null) {
            parent.removeChild(child);
        }
    }


    /**
     *  This method will return either the given node argument
     *  if it does not contain an attribute of the given urlAttr.
     *  Or it will return the parsed document specified by the
     *  value of the urlAttr contained by node.
     *
     *  @param node The node to look at.
     *  @param urlAttr The attribute name that holds the url.
     *  @return The root of the new document or the given node argument if no url ref found.
     */
    public static Element findUrlRefNode(Element node, String urlAttr) {
        String url = XmlUtil.getAttribute(node, urlAttr, (String) null);
        if (url == null) {
            return node;
        }
        try {
            return getRoot(url, XmlUtil.class);
        } catch (Exception exc) {
            ucar.unidata.util.LogUtil.logException("Creating xml:" + url,
                    exc);
        }
        return node;
    }

    /**
     *  This method will return either the given node argument
     *  if it does not contain an attribute of name "url".
     *  Or it will return the parsed document specified by the
     *  value of the urlAttr contained by node.
     *
     *  @param node The node to look at.
     *  @return The root of the new document or the given node argument if no url ref found.
     */
    public static Element findUrlRefNode(Element node) {
        return findUrlRefNode(node, "url");
    }


    /**
     * A main for tests
     *
     * @param args Command line arguments
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {


        /*
        Trace.startTrace();
        for (int i = 0; i < args.length; i++) {
            for(int j=0;j<5;j++) {

                Trace.call1("old way");
                getRoot(args[i], XmlUtil.class);
                Trace.call2("old way");

                Trace.call1("new way");
                FileInputStream fis = new FileInputStream(args[i]);
                DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(fis);
                Trace.call2("new way");
            }
        }
        Trace.stopTrace();
        if(true) return;
        */
        HashSet<String> seen         = new HashSet<String>();

        boolean         doFormat     = true;
        boolean         printTags    = false;;
        boolean         generateCode = false;
        String          packageName  = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-format")) {
                doFormat = true;
                continue;
            }
            if (args[i].equals("-printtags")) {
                printTags = true;
                continue;
            }
            if (args[i].equals("-package")) {
                packageName = args[++i];
                continue;
            }


            if (args[i].equals("-generate")) {
                generateCode = true;
                continue;
            }

            if (generateCode) {
                generateCode(args[i], packageName);
            } else if (printTags) {
                printTags(args[i], seen);
            } else {
                format(args[i]);
            }
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param seen _more_
     * @param tagBuff _more_
     * @param attrBuff _more_
     * @param topBuff _more_
     */
    private static void printTags(Element node, HashSet<String> seen,
                                  StringBuffer tagBuff,
                                  StringBuffer attrBuff,
                                  StringBuffer topBuff) {
        String tagName = node.getTagName();
        if ( !seen.contains("tag:" + tagName)) {
            seen.add("tag:" + tagName);
            String tmp = tagName.replace(":", "_").toUpperCase();
            tagBuff.append("public static final String TAG_" + tmp + " = \""
                           + tagName + "\";\n");
        }
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr   attr     = (Attr) nnm.item(i);
                String attrName = attr.getName();
                if (attrName.startsWith("xmlns")) {
                    seen.add("attr:" + attrName);
                    String value = attr.getNodeValue();
                    attrName = attrName.replace(":", "_");
                    topBuff.append("public static final String XMLNS_"
                                   + attrName.toUpperCase() + " = \"" + value
                                   + "\";\n");

                    continue;
                }

                if ((attrName.indexOf(":") < 0)
                        && !seen.contains("attr:" + attrName)) {
                    seen.add("attr:" + attrName);
                    attrBuff.append("public static final String ATTR_"
                                    + attrName.toUpperCase() + " = \""
                                    + attrName + "\";\n");
                }
            }
        }

        NodeList elements = getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            printTags(child, seen, tagBuff, attrBuff, topBuff);
        }
    }


    /**
     * _more_
     *
     * @param f _more_
     * @param seen _more_
     */
    private static void printTags(String f, HashSet<String> seen) {
        try {
            String       xml      = IOUtil.readContents(f, XmlUtil.class);
            Element      root     = getRoot(xml);
            StringBuffer tagBuff  = new StringBuffer();
            StringBuffer attrBuff = new StringBuffer();
            StringBuffer topBuff  = new StringBuffer();
            printTags(root, seen, tagBuff, attrBuff, topBuff);
            System.out.println(topBuff);
            System.out.println(tagBuff);
            System.out.println(attrBuff);
        } catch (Exception exc) {
            System.err.println("Error processing:" + f);
            exc.printStackTrace();
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     * @param packageName _more_
     *
     * @throws Exception _more_
     */
    private static void generateCode(String f, String packageName)
            throws Exception {
        HashSet<String> seen = new HashSet<String>();
        String          xml  = IOUtil.readContents(f, XmlUtil.class);
        Element         root = getRoot(xml);
        generateCode(root, seen, packageName);
    }


    /**
     * _more_
     *
     * @param element _more_
     * @param seen _more_
     * @param packageName _more_
     *
     * @throws Exception _more_
     */
    private static void generateCode(Element element, HashSet<String> seen,
                                     String packageName)
            throws Exception {
        String className = element.getTagName();
        if ( !seen.contains(className)) {}
    }




    /**
     * _more_
     *
     * @param f _more_
     */
    private static void format(String f) {
        try {
            String       xml     = IOUtil.readContents(f, XmlUtil.class);
            String       origXml = xml;
            StringBuffer buff    = new StringBuffer();
            //Replace comments with special CDATA 
            while (true) {
                int idx1 = xml.indexOf("<!--");
                if (idx1 < 0) {
                    buff.append(xml);
                    break;
                }
                int idx2 = xml.indexOf("-->");
                if ((idx2 < 0) || (idx2 < idx1)) {
                    buff.append(xml);
                    break;
                }
                buff.append(xml.substring(0, idx1));
                String commentBlock = xml.substring(idx1 + 4, idx2);
                xml = xml.substring(idx2 + 3);
                buff.append("<![CDATA[XmlUtil.COMMENT:" + commentBlock
                            + "]]>");
            }

            Element root      = getRoot(buff.toString());
            String  xmlString = toStringWithHeader(root, "  ", "\n", true);

            //                String  xmlString = toStringWithHeader(root, "", "",
            //                                        false);
            IOUtil.writeFile(new java.io.File(f), xmlString);
        } catch (Exception exc) {
            System.err.println("Error processing:" + f);
            exc.printStackTrace();
        }
    }

    /** sha algorithm to use */
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * Computes RFC 2104-compliant HMAC signature.
     *
     * @param data
     *     The data to be signed.
     * @param key
     *     The signing key.
     * @return
     *     The base64-encoded RFC 2104-compliant HMAC signature.
     * @throws java.security.SignatureException when signature generation fails
     */
    public static String calculateRFC2104HMAC(String data, String key)
            throws java.security.SignatureException {
        String result;
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                                           HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            result = XmlUtil.encodeBase64(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                                         + e.getMessage());
        }
        return result;
    }





}
