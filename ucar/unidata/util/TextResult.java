/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



/**
 */

public class TextResult {
    public static String TYPE_HTML = "text/html";
    public static String TYPE_XML = "text/xml";
    public static String TYPE_CSV = "text/csv";

    private StringBuffer content;
    private String title = "";
    private String mimeType = "text/html";

    public TextResult(String title, String  content) {
        this(title, new StringBuffer(content));
    }

    public TextResult(String title, StringBuffer  content) {
        this(title, content,  "text/html");
    }

    public TextResult(StringBuffer  content) {
        this("", content, "text/html");
    }


    public TextResult(String title, StringBuffer  content, String mimeType) {
        this.content = content;
        this.title = title;
        this.mimeType = mimeType;
    }

    public boolean isHtml() {
        return mimeType.equals(TYPE_HTML);
    }

    public boolean isXml() {
        return mimeType.equals(TYPE_XML);
    }


    public boolean isCsv() {
        return mimeType.equals(TYPE_CSV);
    }


/**
Set the Content property.

@param value The new value for Content
**/
public void setContent (StringBuffer value) {
	content = value;
}

/**
Get the Content property.

@return The Content
**/
public StringBuffer getContent () {
	return content;
}

/**
Set the Title property.

@param value The new value for Title
**/
public void setTitle (String value) {
	title = value;
}

/**
Get the Title property.

@return The Title
**/
public String getTitle () {
	return title;
}

/**
Set the MimeType property.

@param value The new value for MimeType
**/
public void setMimeType (String value) {
	mimeType = value;
}

/**
Get the MimeType property.

@return The MimeType
**/
public String getMimeType () {
	return mimeType;
}





}

