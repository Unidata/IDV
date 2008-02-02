/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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


import java.text.SimpleDateFormat;

import java.util.List;
import java.util.ArrayList;

import java.util.GregorianCalendar;
import java.util.Date;
import java.util.regex.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.IOUtil;

public class LdmListener {
    private     boolean debug = false;
    private     BufferedReader br;
    private     SimpleDateFormat yearSdf;
    private     SimpleDateFormat monthSdf;
    private     List files = new ArrayList();
    private     Pattern pattern;
    private     String type = "any";

    private String bufferFile;
    private FileOutputStream bufferOS;
    int bufferCnt = 0;

    long startTime;
    int cnt=0;

    private Object FILES_MUTEX = new Object();
    private Object PROCESS_MUTEX = new Object();

    // = "SDUS[2357]. .... ([0-3][0-9])([0-2][0-9])([0-6][0-9]).*/p(...)(...)";
    private String patternString;
    //"/data/ldm/gempak/nexrad/NIDS/\\5/\\4/\\4_(\\1:yyyy)(\\1:mm)\\1_\\2\\3";
    private String fileTemplate;


    private String fileUrlTemplate = "http://localhost:8080/repository/processfile?file=${file}&type=${type}";
    private String bufferUrlTemplate = "http://localhost:8080/repository/processfile?tocfile=${file}";

    private void usage(String msg) {
        System.err.println(msg);
        System.err.println ("usage: LdmListener -pattern <product pattern> -template <file template> -debug -type <repository type>");
        System.exit(1);
    }

    public LdmListener(String[]args) throws Exception {
        processArgs(args);
        System.err.println ("pattern:" + patternString);
        pattern = Pattern.compile(patternString);
        InputStreamReader sr = new InputStreamReader(System.in);
        br    = new BufferedReader(sr);
        yearSdf = new SimpleDateFormat();
        yearSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        yearSdf.applyPattern("yyyy");

        monthSdf = new SimpleDateFormat();
        monthSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        monthSdf.applyPattern("MM");
        startTime = System.currentTimeMillis();
        Misc.run(new Runnable() {
                public void run() {
                    checkFiles();
                }
            });
        processIncoming();
        System.exit(0);
    }



    private void processArgs(String[]args) {
        for(int i=0;i<args.length;i++) {
            if(args[i].equals("-pattern")) {
                if(i==args.length-1) usage("Incorrect input");
                patternString = args[++i];
            } else if(args[i].equals("-debug")) {
                debug= true;
            } else if(args[i].equals("-template")) {
                if(i==args.length-1) usage("Incorrect input");
                fileTemplate = args[++i];     
            } else if(args[i].equals("-bufferfile")) {
                if(i==args.length-1) usage("Incorrect input");
                bufferFile = args[++i];     
            } else if(args[i].equals("-type")) {
                if(i==args.length-1) usage("Incorrect input");
                type = args[++i];     
            } else {
                usage("Unknown argument:" + args[i]);
            }
        }
        if(patternString ==null) {
            usage("No -pattern given");
        }
        if(fileTemplate ==null) {
            usage("No -template given");
        }
    }

    public void processIncoming() throws Exception {
        while (true) {
            try {
                String line= br.readLine();
                if(line!=null) {
                    processLine(line);
                } else {
                    Misc.sleep(100);
                }
            }
            catch (IOException e) {
                break;
            }
        }
    }


    private void processLine(String line)  {

        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            if(debug) 
                System.err.println("no match:" + line);
            return;
        }
        

        if(debug) {
            if((cnt++)%50==0) {
                double minutes  = (System.currentTimeMillis() -startTime)/1000.0/60.0;
                if(minutes>0) {
                    System.err.println("#"+cnt + " rate: " + ((int)(cnt/(double)minutes)) +"/minute");
                }
            }
        }
        String filename = fileTemplate;

        Date now = new Date();
        String year = yearSdf.format(now);
        String month = monthSdf.format(now);
        
        int count = matcher.groupCount();
        for(int groupIdx=1;groupIdx<=count;groupIdx++) {
            String match = matcher.group(groupIdx);
            filename = filename.replace("(\\" + groupIdx+":yyyy)", year);
            filename = filename.replace("(\\" + groupIdx+":mm)", month);
            filename = filename.replace("\\" + groupIdx+"", match);
        }

        File f= new File(filename);
        int cnt = 0;
        if(debug) {
            System.err.println("file:" + filename + " exists:" + f.exists());
        }
        addFile(f);
    }


    private void checkFiles() {
        while(true) {
            if(bufferCnt>500) {
            }
            if(files.size()>0) {
                List tmp;
                synchronized(FILES_MUTEX) {
                    tmp = new ArrayList(files);
                    files  =new ArrayList();
                }
                for(int i=0;i<tmp.size();i++) {
                    File f = (File) tmp.get(i);
                    if(!f.exists()) {
                        addFile(f);
                    } else {
                        processFile(f);
                    }
                }
            }
            Misc.sleep(1000);
        }
    }

    private void addFile(File f)  {
        synchronized(FILES_MUTEX) {
            files.add(f);
        }
    }


    private void  writeToBuffer(File f)  throws Exception {
        if(bufferOS==null) {
            bufferOS = new FileOutputStream(bufferFile,true);
        }
        String s = type+":" + f+"\n";
        bufferOS.write(s.getBytes());
        bufferOS.flush();
        bufferCnt++;
    }

    private boolean processFile(File f)  {
        synchronized(PROCESS_MUTEX) {
            if(bufferFile!=null) {
                try {
                    writeToBuffer(f);
                } catch(Exception exc) {
                    bufferOS = null;
                    System.out.println("error:" + exc);
                    addFile(f);
                    return false;
                }
            }

            String urlString = fileUrlTemplate.replace("${file}", f.toString());
            urlString = urlString.replace("${type}", type);
            try {
                URL     url    = new URL(urlString);
                URLConnection connection = url.openConnection();
                InputStream s = connection.getInputStream();
                String results = IOUtil.readContents(s);
                if(!results.equals("OK")) {
                    addFile(f);
                    if(debug) {
                        System.out.println("connection not successful:" + results);
                    }
                    return false;
                }
            } catch(Exception exc) {
                System.out.println("error:" + exc);
                addFile(f);
                return false;
            }
            return true;
        }
    }




    public static void main(String[]args) throws Exception {
        LdmListener listener = new LdmListener(args);
    }


}
