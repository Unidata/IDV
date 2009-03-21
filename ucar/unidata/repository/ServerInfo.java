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



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ServerInfo {
    
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

