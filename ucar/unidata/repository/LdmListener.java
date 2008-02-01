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
    BufferedReader br;
    
    SimpleDateFormat yearSdf;
    SimpleDateFormat monthSdf;

    List files = new ArrayList();

    Pattern pattern;


    long startTime;
    int cnt=0;

    public LdmListener(String[]args) throws Exception {
        String patternString = "SDUS[2357]. .... ([0-3][0-9])([0-2][0-9])([0-6][0-9]).*/p(...)(...)";
        pattern = Pattern.compile(patternString);
  

        if(args.length==0) throw new IllegalArgumentException("No arguments given");
        InputStreamReader sr = new InputStreamReader(System.in);
        br    = new BufferedReader(sr);

        yearSdf = new SimpleDateFormat();
        yearSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        yearSdf.applyPattern("yyyy");

        monthSdf = new SimpleDateFormat();
        monthSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        monthSdf.applyPattern("MM");
        startTime = System.currentTimeMillis();
        run();
    }


    public void run() throws Exception {
        while (true) {
            try {
                String line= br.readLine();
                if(line!=null) {
                    processLine(line);
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
            System.err.println("no match:" + line);
            return;
        }
        

        cnt++;
        if(cnt%50==0) {
            double minutes  = (System.currentTimeMillis() -startTime)/1000.0/60.0;
            if(minutes>0) {
                System.err.println("#"+cnt + " rate: " + ((int)(cnt/(double)minutes)) +"/minute");
            }
        }

        String filename = "/data/ldm/gempak/nexrad/NIDS/\\5/\\4/\\4_(\\1:yyyy)(\\1:mm)\\1_\\2\\3";

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
        if(!f.exists()) {
            //            System.err.println("**** file:" + filename + " exists:" + f.exists());
            files.add(f);
        } else {
            //            System.err.println("file ok:" + filename + " exists:" + f.exists());
            processFile(f);
        }

        if(files.size()>0) {
            List tmp = new ArrayList(files);
            files  =new ArrayList();
            for(int i=0;i<tmp.size();i++) {
                f = (File) tmp.get(i);
                if(!f.exists()) {
                    files.add(f);
                } else {
                    //                    System.err.println("Processing old file:" + f);
                    processFile(f);
                }
            }
        }
    }


    private void processFile(File f)  {
        //        if(true) return;
        String urlString = "http://localhost:8080/repository/processfile?type=level3radar&file=" + f;
        try {
            URL           url    = new URL(urlString);
            URLConnection connection = url.openConnection();
            InputStream s = connection.getInputStream();
            String results = IOUtil.readContents(s);
            if(!results.equals("OK")) {
                System.out.println("BAD:" + results);
            }
        } catch(Exception exc) {
            System.out.println("error:" + exc);
        }
    }




    public static void main(String[]args) throws Exception {
        LdmListener listener = new LdmListener(args);
    }


}