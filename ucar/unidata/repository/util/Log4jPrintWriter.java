/**
 * $Id: v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import  java.io.*;


/**
 * Orignally from JD Evora
 */


public class Log4jPrintWriter extends PrintWriter {
    private Logger log;
    StringBuffer text = new StringBuffer("");

    public Log4jPrintWriter(Logger log) {
	super(System.err);  // PrintWriter doesn't have default constructor.
	this.log = log;
    }

    public  void log() {
	if(log!=null) {
	    log.info(text.toString());
	}
	text.setLength(0);
    }


    // overrides all the print and println methods for 'print' it to the constructor's Category
    public void close(){
	flush();
    }

    public void flush(){
	if (!text.toString().equals("")){
	    log();
	}
    }



    public void print(boolean b){
	text.append(b);
    }

    public void print(char c){
	text.append(c);
    }
    public void print(char[] s){
	text.append(s);
    }
    public void print(double d){
	text.append(d);
    }
    public void print(float f){
	text.append(f);
    }
    public void print(int i){
	text.append(i);
    }
    public void print(long l){
	text.append(l);
    }
    public void print(Object obj){
	text.append(obj);
    }

    public void print(String s){
	text.append(s);
    }
    public void println(){
	if (!text.toString().equals("")){
	    log();
	}
    }
    public void println(boolean x){
	text.append(x);
	log();
    }
    public void println(char x){
	text.append(x);
	log();
    }

    public void println(char[] x){
	text.append(x);
	log();
    }
    public void println(double x){
	text.append(x);
	log();
    }
    public void println(float x){
	text.append(x);
	log();
    }
    public void println(int x){
	text.append(x);
	log();
    }
    public void println(long x){
	text.append(x);
	log();
    }
    public void println(Object x){
	text.append(x);
	log();
    }
    public void println(String x){
	text.append(x);
	log();
    }
}



