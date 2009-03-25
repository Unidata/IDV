/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.xml.XmlUtil;

import org.w3c.dom.*;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ServerInfo implements Constants {
    
    private String hostname;
    private int port;
    private int sslPort;
    private String basePath;
    private String title;
    private String description;


    public ServerInfo(String hostname,
                      int port,
                      String title,String description) {
        this(hostname, port,-1,"/repository",title, description);
    }


    public ServerInfo(String hostname,
                      int port,
                      int sslPort,
                      String basePath,
                      String title,
                      String description) {
        this.hostname=hostname;
        this.port=port;
        this.sslPort=sslPort;
        this.basePath=basePath;
        this.title=title;
        this.description=description;
    }


    public String toString() {
        return getLabel();
    }


    public boolean equals(Object o) {
        if(!(o instanceof ServerInfo)) return false;
        ServerInfo that = (ServerInfo) o;
        return  this.getId().equals(that.getId());
    }

    public Element toXml(Document doc) throws Exception {
        Element info = XmlUtil.create(doc, TAG_INFO, null,
                                      new String[] {});
        XmlUtil.create(doc, TAG_INFO_DESCRIPTION, info,
                       description,
                       null);

        XmlUtil.create(doc, TAG_INFO_TITLE, info,
                       title,
                       null);
        XmlUtil.create(doc, TAG_INFO_HOSTNAME, info, hostname, null);
        XmlUtil.create(doc, TAG_INFO_BASEPATH, info, basePath, null);
        XmlUtil.create(doc, TAG_INFO_PORT, info, ""+port, null);
        if (sslPort>0) {
            XmlUtil.create(doc, TAG_INFO_SSLPORT, info, ""+sslPort, null);
        }
        return info;
    }


    public String getId() {
        return hostname+":"+port+basePath;

    }
    public String getHref (String extra) {
        return HtmlUtil.href("http://" + hostname+":" + port+basePath,
                             getLabel(),extra);
    }

    public String getUrl () {
        return "http://" + hostname+":" + port+basePath;
    }


    public String getLabel () {
        if(title!=null && title.length()>0) return title;
        if(port!=80) 
            return hostname+":" + port;
        return hostname;
    }

    /**
       Set the Hostname property.

       @param value The new value for Hostname
    **/
    public void setHostname (String value) {
	this.hostname = value;
    }

    /**
       Get the Hostname property.

       @return The Hostname
    **/
    public String getHostname () {
	return this.hostname;
    }

    /**
       Set the Port property.

       @param value The new value for Port
    **/
    public void setPort (int value) {
	this.port = value;
    }

    /**
       Get the Port property.

       @return The Port
    **/
    public int getPort () {
	return this.port;
    }

    /**
       Set the SslPort property.

       @param value The new value for SslPort
    **/
    public void setSslPort (int value) {
	this.sslPort = value;
    }

    /**
       Get the SslPort property.

       @return The SslPort
    **/
    public int getSslPort () {
	return this.sslPort;
    }

    /**
       Set the BasePath property.

       @param value The new value for BasePath
    **/
    public void setBasePath (String value) {
	this.basePath = value;
    }

    /**
       Get the BasePath property.

       @return The BasePath
    **/
    public String getBasePath () {
	return this.basePath;
    }

    /**
       Set the Title property.

       @param value The new value for Title
    **/
    public void setTitle (String value) {
	this.title = value;
    }

    /**
       Get the Title property.

       @return The Title
    **/
    public String getTitle () {
	return this.title;
    }

    /**
       Set the Description property.

       @param value The new value for Description
    **/
    public void setDescription (String value) {
	this.description = value;
    }

    /**
       Get the Description property.

       @return The Description
    **/
    public String getDescription () {
	return this.description;
    }


}

