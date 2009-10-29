/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.type;
import ucar.unidata.repository.*;
import ucar.unidata.repository.metadata.*;

import ucar.unidata.repository.output.*;
import ucar.unidata.repository.data.*;


import org.apache.commons.net.ftp.*;


import org.w3c.dom.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Pool;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import ucar.nc2.dataset.NetcdfDataset;

import org.python.core.*;
import org.python.util.*;

/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class JythonTypeHandler extends GenericTypeHandler {

    /** _more_ */
    private Pool<String, PythonInterpreter> interpPool = new Pool<String,
	PythonInterpreter>(100) {
        protected PythonInterpreter createValue(String path) {
            try {
		PythonInterpreter interp = new PythonInterpreter();
		for(String f: getRepository().getPythonLibs()) {
		    interp.execfile(IOUtil.getInputStream(f, getClass()), f);
		}
		return interp;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    };


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public JythonTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }


    public void initializeNewEntry(Entry entry) throws Exception {
	super.initializeNewEntry(entry);
	
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }


    public Result getHtmlDisplay(Request request, Entry entry)
	throws Exception {
        PythonInterpreter interp = interpPool.get("interp");
	Result result =  getHtmlDisplay(request, entry, interp);
	interpPool.put("interp", interp);
	return result;
    }


    private Result getHtmlDisplay(Request request, Entry entry, PythonInterpreter interp)
	throws Exception {

	String password = (String)entry.getValues()[0];
	String init = (String)entry.getValues()[1];
	String exec = (String)entry.getValues()[2];
	StringBuffer sb = new StringBuffer();
	FormInfo formInfo = new FormInfo(this, request, sb);
	boolean makeForm = !request.exists(ARG_SUBMIT);


	interp.set("formInfo", formInfo);
	interp.set("request", request);
	interp.set("typeHandler", this);
	interp.set("repository", getRepository());
	interp.set("makeForm", (makeForm?new Integer(1):new Integer(0)));

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(entry,
                ContentMetadataHandler.TYPE_JYTHON, true);
        if (metadataList != null) {
	    for (Metadata metadata : metadataList) {
                File jythonLib =
                    new File(
                        IOUtil.joinDir(
                            getRepository().getStorageManager().getEntryDir(
                                metadata.getEntryId(),
                                false), metadata.getAttr1()));
                interp.execfile(new java.io.FileInputStream(jythonLib), jythonLib.toString());
		
	    }
	}





	if(init!=null && init.trim().length()>0) {
	    try {
		interp.exec(init);
	    } catch(Exception exc) {
		return  new Result(entry.getName(), new StringBuffer("Error:" + exc));
	    }
	}

	DataOutputHandler dataOutputHandler =
	    getRepository().getDataOutputHandler();
	if(makeForm)  {
	    StringBuffer formSB = new StringBuffer();
	    formSB.append(formInfo.prefix);

	    if(formInfo.cnt>0) {
		String formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
		formSB.append(HtmlUtil.uploadForm(formUrl,""));
		formSB.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
		formSB.append(HtmlUtil.formTable());
		if(password!=null && password.length()>0) {
		    formSB.append(HtmlUtil.formEntry(msgLabel("Password"), HtmlUtil.password(ARG_PASSWORD)));
		}
		formSB.append(sb);
		formSB.append(HtmlUtil.formTableClose());
		formSB.append(HtmlUtil.submit(msg("Submit"), ARG_SUBMIT)); 
		formSB.append(HtmlUtil.formClose());
	    }
	    Result result = new Result(formInfo.title!=null?formInfo.title:entry.getName(), formSB);
	    return result;
	} else {
	    if(password!=null && password.length()>0) {
		if(!Misc.equals(password, request.getString(ARG_PASSWORD,""))) {
		    return new Result(formInfo.title!=null?formInfo.title:entry.getName(), new StringBuffer(repository.showDialogError("Bad password")));
		}
	    }
	    List<String> ncPaths =  new ArrayList<String>();
	    List<NetcdfDataset> ncData =  new ArrayList<NetcdfDataset>();
	    List<File> files = new ArrayList<File>();
	    try {
		for(InputInfo info: formInfo.inputs) {
		    if(info.type ==InputInfo.TYPE_FILE) {
			String  file  = request.getUploadedFile(info.id);
			if ((file != null) && (file.length() > 0)
			    && new File(file).exists()) {
			    files.add(new File(file));
			    interp.set(info.id, file);
			} else {
			    return new Result(formInfo.title!=null?formInfo.title:entry.getName(), new StringBuffer(repository.showDialogError("No file uploaded")));
			}

		    } else 	if(info.type ==InputInfo.TYPE_ENTRY) {
			String entryName = request.getString(info.id, "");

			String entryId =  request.getUnsafeString(info.id + "_hidden", "");
			Entry theEntry =   getEntryManager().getEntry(request, entryId);
			if(theEntry==null) {
			    return new Result(formInfo.title!=null?formInfo.title:entry.getName(), new StringBuffer(repository.showDialogError("No entry selected")));
			}

			interp.set(info.id, theEntry);
			if(theEntry.isFile()) {
			    interp.set(info.id+"_file", theEntry.getResource().getPath());
			} else {
			    interp.set(info.id+"_file", null);
			}
			String       path   = dataOutputHandler.getPath(theEntry);
			if(path!=null) {
			    NetcdfDataset ncDataset = dataOutputHandler.getNetcdfDataset(theEntry, path);
			    if(ncDataset!=null) {
				ncPaths.add(path);
				ncData.add(ncDataset);
				interp.set(info.id+"_ncdataset", ncDataset);
			    } else {
				interp.set(info.id+"_ncdataset", null);
			    }
			
			}

		    } else 	if(info.type ==InputInfo.TYPE_NUMBER) {
			interp.set(info.id, new Double(request.getString(info.id,"").trim()));
		    } else {
			interp.set(info.id, request.getString(info.id,""));
		    }
		}
		try {
		    interp.exec(exec);
		} catch(Exception exc) {
		    return  new Result(entry.getName(), new StringBuffer("Error:" + exc));
		}
	    } finally {
		for(File f: files) {
		    f.delete();
		}
		for(int i=0;i<ncPaths.size();i++) {
		    dataOutputHandler.returnNetcdfDataset(ncPaths.get(i), ncData.get(i));

		}
	    }
	    if(formInfo.inputStream != null) {
		return new Result(formInfo.title!=null?formInfo.title:entry.getName(), formInfo.inputStream, formInfo.mimeType);
	    }

	    if(formInfo.resultHtml == null) {
		formInfo.resultHtml = "No result provided";
	    }
	    Result result = new Result(formInfo.title!=null?formInfo.title:entry.getName(), new StringBuffer(formInfo.resultHtml), formInfo.mimeType);
	    return result;

	}

    }



    public static class InputInfo {
	private static final int TYPE_FILE = 0;
	private static final int TYPE_ENTRY = 1;
	private static final int TYPE_TEXT = 2;
	private static final int TYPE_NUMBER = 3;


	int type;
	String id;


	public InputInfo(int type, String id) {
	    this.type = type;
	    this.id = id;
	}
    }

    public static class FormInfo {
	
	List<InputInfo> inputs = new ArrayList<InputInfo>();

	StringBuffer sb;
	int cnt = 0;
	JythonTypeHandler typeHandler;
	String title;
	String prefix="";
	Request request;

	String resultHtml;
	String mimeType = "text/html";
	InputStream inputStream;
	public FormInfo(JythonTypeHandler typeHandler, Request request, StringBuffer sb) {
	    this.sb = sb;
	    this.request = request;
	    this.typeHandler = typeHandler;
	}

	/**
	   Set the MimeType property.

	   @param value The new value for MimeType
	**/
	public void setMimeType (String value) {
	    this.mimeType = value;
	}

	/**
	   Get the MimeType property.

	   @return The MimeType
	**/
	public String getMimeType () {
	    return this.mimeType;
	}

	/**
	   Set the InputStream property.

	   @param value The new value for InputStream
	**/
	public void setInputStream (InputStream value) {
	    this.inputStream = value;
	}

	/**
	   Get the InputStream property.

	   @return The InputStream
	**/
	public InputStream getInputStream () {
	    return this.inputStream;
	}


	public void append(String s) {
	    sb.append(s);
	}

	public void setResultHtml(String html) {
	    resultHtml = html;
	}

	public void setTitle(String title) {
	    this.title = title;
	}


	public void setPrefix(String prefix) {
	    this.prefix = prefix;
	}

	public void addFormFileUpload(String id, String label) {
	    cnt++;
	    inputs.add(new InputInfo(InputInfo.TYPE_FILE, id));
	    sb.append(HtmlUtil.formEntry(typeHandler.msgLabel(label),
					 HtmlUtil.fileInput(id, HtmlUtil.attr(HtmlUtil.ATTR_SIZE,"80"))));
	}


	public void addFormEntry(String id, String label) throws Exception {
	    inputs.add(new InputInfo(InputInfo.TYPE_ENTRY, id));

	    sb.append(HtmlUtil.hidden(id + "_hidden", "", HtmlUtil.id(id + "_hidden")));
            String select = OutputHandler.getSelect(request, id,
						    "Select", true, null);
	    sb.append(HtmlUtil.formEntry(label,
					 HtmlUtil.disabledInput(id,
								"", HtmlUtil.id(id)
								+ HtmlUtil.SIZE_60) + select));
	    cnt++;
	}


	public void addFormLabel(String label) throws Exception {
	    sb.append(HtmlUtil.formEntry("", label));
	}


	public void addFormText(String id, String label, String dflt, int columns, int rows) {
	    cnt++;
	    inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
	    if(rows==1) {
		sb.append(HtmlUtil.formEntry(typeHandler.msgLabel(label),
					     HtmlUtil.input(id, dflt, HtmlUtil.attr(HtmlUtil.ATTR_SIZE,""+columns))));
	    } else {
		sb.append(HtmlUtil.formEntryTop(typeHandler.msgLabel(label),
					     HtmlUtil.textArea(id, dflt,rows,columns)));
	    }
	}


	public void addFormNumber(String id, String label, double dflt) {
	    inputs.add(new InputInfo(InputInfo.TYPE_NUMBER, id));
	    cnt++;
	    sb.append(HtmlUtil.formEntry(typeHandler.msgLabel(label),
					 HtmlUtil.input(id, ""+dflt, HtmlUtil.attr(HtmlUtil.ATTR_SIZE,""+5))));
	}

    }



}

