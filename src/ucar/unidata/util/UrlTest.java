/*
 * $Id: IOUtil.java,v 1.52 2007/08/14 16:06:15 jeffmc Exp $
 *
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


import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class UrlTest {
   
    public static int readUrl(String urlToFetch) throws Exception {
        URL url = new URL(urlToFetch);
        InputStream inputStream =
            url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int read = 0;
        int totalRead = 0;
        while ((read=inputStream.read(buffer)) > 0) {
            totalRead+=read;
        }
        inputStream.close();
        //        System.err.println (totalRead);
        //        System.exit(0);
        return totalRead;
    }

    public static void main(String[] args) throws Exception {
        int [] threadCnts = {1,2,3,4,8,12,16,20};
        for(int argIdx=0;argIdx<args.length;argIdx++) {
            System.err.println (args[argIdx]);
            final List<String> urls =  StringUtil.split(IOUtil.readContents(new FileInputStream(args[argIdx])),"\n",true,true);
            final int[] threadsRunning = {0};
            final int[]nextUrl = {0};
            final int numReads = 50;
            for (int threadCntIdx = 0; threadCntIdx <threadCnts.length;threadCntIdx++) {
                ArrayList<Thread> threads = new ArrayList<Thread>();
                for(int i=0;i<threadCnts[threadCntIdx];i++) {
                    final int  threadId = i;
                    threads.add(new Thread(new Runnable() {
                            public void run() {
                                try {
                                    int totalRead = 0;
                                    for(int i=0;i<numReads;i++) {
                                        int urlIdx;
                                        synchronized(nextUrl) {
                                            nextUrl[0]++;
                                            if(nextUrl[0]>= urls.size()) 
                                                nextUrl[0] = 0;
                                            urlIdx = nextUrl[0];
                                        }
                                        String url = urls.get(urlIdx);
                                        totalRead+=readUrl(url);
                                    }
                                } catch (Exception exc) {
                                    exc.printStackTrace();
                                } finally {
                                    synchronized (threadsRunning) {
                                        threadsRunning[0]--;
                                    }
                                }
                            }
                        }));
                }


                threadsRunning[0] = 0;
                for (Thread thread : threads) {
                    synchronized (threadsRunning) {
                        threadsRunning[0]++;
                    }
          
                    thread.start();
                }
                long t1 = System.currentTimeMillis();
                while (true) {
                    synchronized (threadsRunning) {
                        if (threadsRunning[0] <= 0) {
                            break;
                        }
                    }
                    try {
                        Thread.currentThread().sleep(1);
                    } catch (Exception exc) {}
                }
                long t2 = System.currentTimeMillis();
                double seconds = (t2-t1)/(double)1000.0;
                System.err.println("     total time: " + (t2 - t1) + " # threads:" + threadCnts[threadCntIdx] +  "  reads/s:" +(int)((numReads*threadCnts[threadCntIdx])/(seconds)));
            }
        }
        
    }


}

