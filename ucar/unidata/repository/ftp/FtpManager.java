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


package ucar.unidata.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ssl.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




import ucar.unidata.util.Misc;
import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FtpManager extends RepositoryManager {
    public static final String DFLT_PASSIVE_PORTS = "44001-44099";
    private final Logger LOG = LoggerFactory.getLogger("org.apache.ftpserver");

    private FtpServer server;
    private int port=-1;
    private String passivePorts;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public FtpManager(Repository repository) {
        super(repository);
        try {
	    checkServer();
        } catch (Exception exc) {
            exc.printStackTrace();
            logError("Creating FTP server", exc);
        }
    }


    public void logError(String message, Exception exc) {
        getRepository().getLogManager().logError(LOG,"RAMADDA:" +  message, exc);
    }


    public void logInfo(String message) {
        LOG.info("RAMADDA:" + message);
    }


    public void checkServer() throws Exception {
	int newPort = getRepository().getProperty(PROP_FTP_PORT,-1);
	if(newPort<0) {
	    stop();
	    return;
	} 
	if(newPort!=port) {
	    stop();
	} else  if(!Misc.equals(passivePorts,getRepository().getProperty(PROP_FTP_PASSIVEPORTS,DFLT_PASSIVE_PORTS))) {
	    stop();
        }

        logInfo("Calling initFtpServer");
	port  = newPort;
	if(server == null) {
	    initFtpServer();
	}

    }


    public void shutdown() {
        stop();
    }

    private void stop() {
	if(server!=null) {
            logInfo("Calling server.stop");
	    server.stop();
	}
	server= null;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initFtpServer() throws Exception {

        FtpServerFactory serverFactory = new FtpServerFactory();

        Hashtable        ftplets = new Hashtable<java.lang.String, Ftplet>();
        ftplets.put("default", new RepositoryFtplet(this));
        serverFactory.setFtplets(ftplets);
	
        ListenerFactory factory = new ListenerFactory();
        logInfo("Setting port to " + port);
        // set the port of the listener
        factory.setPort(port);


        DataConnectionConfigurationFactory dccf = new DataConnectionConfigurationFactory();
        passivePorts = getRepository().getProperty(PROP_FTP_PASSIVEPORTS,DFLT_PASSIVE_PORTS);
        logInfo("Setting passive ports to:" + passivePorts);
        dccf.setPassivePorts(passivePorts);
        factory.setDataConnectionConfiguration(dccf.createDataConnectionConfiguration());

        File keystore =
            new File(getRepository().getPropertyValue(PROP_SSL_KEYSTORE,
                getRepository().getStorageManager().getRepositoryDir()
                + "/keystore", false));
	
        if (keystore.exists()) {
	    logInfo("Using FTPS");
	    String password = getRepository().getPropertyValue(PROP_SSL_PASSWORD,
							       (String) null, false);
	    String keyPassword = getRepository().getPropertyValue(PROP_SSL_PASSWORD,
								  password, false);

	    SslConfigurationFactory ssl = new SslConfigurationFactory();
	    ssl.setKeystoreFile(keystore);
	    ssl.setKeystorePassword(keyPassword);

	    factory.setSslConfiguration(ssl.createSslConfiguration());
	    factory.setImplicitSsl(true);
	}



        // replace the default listener
        serverFactory.addListener("default", factory.createListener());


        serverFactory.setUserManager(new RepositoryFtpUserManager(this));

        // start the server
        server = serverFactory.createServer();
	logInfo("Calling server.start");
        server.start();
	logInfo("Starting server on port:" + port);
    }



}

