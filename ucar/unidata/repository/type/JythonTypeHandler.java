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


import org.apache.commons.net.ftp.*;

import org.python.core.*;
import org.python.util.*;


import org.w3c.dom.*;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.data.DataSource;

import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.metadata.*;

import ucar.unidata.repository.output.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Pool;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class JythonTypeHandler extends GenericTypeHandler {

    /** _more_          */
    public static final String ARG_SCRIPT_PASSWORD = "script.password";

    /** _more_ */
    private Pool<String, PythonInterpreter> interpPool =
        new Pool<String, PythonInterpreter>(100) {
        protected PythonInterpreter createValue(String path) {
            try {
                getStorageManager().initPython();
                PythonInterpreter interp = new PythonInterpreter();
                for (String f : getRepository().getPythonLibs()) {
                    interp.execfile(IOUtil.getInputStream(f, getClass()), f);
                }
                interp.exec(
                    getRepository().getResource(
                        "/ucar/unidata/repository/resources/init.py"));
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
        LogUtil.setTestMode(true);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        PythonInterpreter interp = interpPool.get("interp");
        Result            result = getHtmlDisplay(request, entry, interp);
        interpPool.put("interp", interp);
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result getHtmlDisplay(Request request, Entry entry,
                                  PythonInterpreter interp)
            throws Exception {

        String       password = (String) entry.getValues()[0];
        String       init     = (String) entry.getValues()[1];
        String       exec     = (String) entry.getValues()[2];
        StringBuffer sb       = new StringBuffer();
        FormInfo     formInfo = new FormInfo(this, entry, request, sb);
        boolean      makeForm = !request.exists(ARG_SUBMIT);
        String       formUrl  = getRepository().URL_ENTRY_SHOW.getFullUrl();

        interp.set("formInfo", formInfo);
        interp.set("request", request);
        interp.set("formUrl", formUrl);
        interp.set("typeHandler", this);
        interp.set("repository", getRepository());

        interp.set("makeForm", (makeForm
                                ? new Integer(1)
                                : new Integer(0)));

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
                interp.execfile(new java.io.FileInputStream(jythonLib),
                                jythonLib.toString());

            }
        }





        if ((init != null) && (init.trim().length() > 0)) {
            try {
                interp.exec(init);
            } catch (Exception exc) {
                return new Result(entry.getName(),
                                  new StringBuffer("Error:" + exc));
            }
        }

        DataOutputHandler dataOutputHandler =
            getRepository().getDataOutputHandler();
        if (makeForm) {
            StringBuffer formSB = new StringBuffer();
            formSB.append(formInfo.prefix);

            if (formInfo.cnt > 0) {
                if (formInfo.resultFileName != null) {
                    formUrl = formUrl + "/" + formInfo.resultFileName;
                }
                formSB.append(HtmlUtil.uploadForm(formUrl, ""));
                formSB.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
                formSB.append(HtmlUtil.formTable());
                if ((password != null) && (password.trim().length() > 0)) {
                    formSB.append(HtmlUtil.formEntry(msgLabel("Password"),
                            HtmlUtil.password(ARG_SCRIPT_PASSWORD)));
                }
                formSB.append(sb);
                formSB.append(HtmlUtil.formTableClose());
                formSB.append(HtmlUtil.submit(msg("Submit"), ARG_SUBMIT));
                formSB.append(HtmlUtil.formClose());
            }
            Result result = new Result((formInfo.title != null)
                                       ? formInfo.title
                                       : entry.getName(), formSB);
            return result;
        } else {
            if ((password != null) && (password.trim().length() > 0)) {
                if ( !Misc.equals(password.trim(),
                                  request.getString(ARG_SCRIPT_PASSWORD,
                                      "").trim())) {
                    return new Result((formInfo.title != null)
                                      ? formInfo.title
                                      : entry.getName(), new StringBuffer(
                                          repository.showDialogError(
                                              "Bad password")));
                }
            }
            List<String>        ncPaths     = new ArrayList<String>();
            List<NetcdfDataset> ncData      = new ArrayList<NetcdfDataset>();

            List<String>        gridPaths   = new ArrayList<String>();
            List<GridDataset>   gridData    = new ArrayList<GridDataset>();
            List<DataSource>    dataSources = new ArrayList<DataSource>();
            List<File>          files       = new ArrayList<File>();
            try {
                for (InputInfo info : formInfo.inputs) {
                    if (info.type == InputInfo.TYPE_FILE) {
                        String file = request.getUploadedFile(info.id);
                        if ((file != null) && (file.length() > 0)
                                && new File(file).exists()) {
                            files.add(new File(file));
                            interp.set(info.id, file);
                        } else {
                            return new Result((formInfo.title != null)
                                    ? formInfo.title
                                    : entry.getName(), new StringBuffer(
                                        repository.showDialogError(
                                            "No file uploaded")));
                        }

                    } else if (info.type == InputInfo.TYPE_ENTRY) {
                        String entryName = request.getString(info.id, "");

                        String entryId = request.getUnsafeString(info.id
                                             + "_hidden", "");
                        Entry theEntry = getEntryManager().getEntry(request,
                                             entryId);
                        if (theEntry == null) {
                            return new Result((formInfo.title != null)
                                    ? formInfo.title
                                    : entry.getName(), new StringBuffer(
                                        repository.showDialogError(
                                            "No entry selected")));
                        }

                        interp.set(info.id, theEntry);
                        if (theEntry.isFile()) {
                            interp.set(info.id + "_file",
                                       theEntry.getResource().getPath());
                        } else {
                            interp.set(info.id + "_file", null);
                        }
                        String path = dataOutputHandler.getPath(theEntry);
                        if (path != null) {
                            //Try it as grid first
                            GridDataset gds =
                                dataOutputHandler.getGridDataset(theEntry,
                                    path);
                            NetcdfDataset     ncDataset  = null;
                            GeoGridDataSource dataSource = null;
                            interp.set(info.id + "_griddataset", gds);
                            if (gds == null) {
                                //Else try it as a ncdataset
                                ncDataset =
                                    dataOutputHandler.getNetcdfDataset(
                                        theEntry, path);
                            } else {
                                dataSource = new GeoGridDataSource(gds);
                                dataSources.add(dataSource);
                            }
                            interp.set(info.id + "_datasource", dataSource);
                            interp.set(info.id + "_ncdataset", ncDataset);
                            if (ncDataset != null) {
                                ncPaths.add(path);
                                ncData.add(ncDataset);
                            }
                        }
                    } else if (info.type == InputInfo.TYPE_NUMBER) {
                        interp.set(info.id,
                                   new Double(request.getString(info.id,
                                       "").trim()));
                    } else {
                        interp.set(info.id, request.getString(info.id, ""));
                    }
                }
                try {
                    interp.exec(exec);
                } catch (Exception exc) {
                    return new Result(entry.getName(),
                                      new StringBuffer("Error:" + exc));
                }
            } finally {
                for (File f : files) {
                    f.delete();
                }
                for (DataSource dataSource : dataSources) {
                    dataSource.doRemove();
                }
                for (int i = 0; i < ncPaths.size(); i++) {
                    dataOutputHandler.returnNetcdfDataset(ncPaths.get(i),
                            ncData.get(i));
                }
                for (int i = 0; i < gridPaths.size(); i++) {
                    dataOutputHandler.returnGridDataset(gridPaths.get(i),
                            gridData.get(i));
                }

            }

            if (formInfo.errorMessage != null) {
                formInfo.resultHtml =
                    getRepository().showDialogError(formInfo.errorMessage);
            }

            if (formInfo.inputStream != null) {
                return new Result((formInfo.title != null)
                                  ? formInfo.title
                                  : entry.getName(), formInfo.inputStream,
                                  formInfo.mimeType);
            }

            if (formInfo.resultHtml == null) {
                formInfo.resultHtml = "No result provided";
            }
            Result result = new Result((formInfo.title != null)
                                       ? formInfo.title
                                       : entry.getName(), new StringBuffer(
                                           formInfo.resultHtml), formInfo
                                               .mimeType);
            return result;

        }

    }



    /**
     * Class InputInfo _more_
     *
     *
     * @author IDV Development Team
     */
    public static class InputInfo {

        /** _more_          */
        private static final int TYPE_FILE = 0;

        /** _more_          */
        private static final int TYPE_ENTRY = 1;

        /** _more_          */
        private static final int TYPE_TEXT = 2;

        /** _more_          */
        private static final int TYPE_NUMBER = 3;


        /** _more_          */
        int type;

        /** _more_          */
        String id;


        /**
         * _more_
         *
         * @param type _more_
         * @param id _more_
         */
        public InputInfo(int type, String id) {
            this.type = type;
            this.id   = id;
        }
    }

    /**
     * Class FormInfo _more_
     *
     *
     * @author IDV Development Team
     */
    public static class FormInfo {

        /** _more_          */
        List<InputInfo> inputs = new ArrayList<InputInfo>();



        /** _more_          */
        JythonTypeHandler typeHandler;

        /** _more_          */
        Entry entry;


        /** _more_          */
        StringBuffer sb;

        /** _more_          */
        int cnt = 0;

        /** _more_          */
        String title;

        /** _more_          */
        String prefix = "";

        /** _more_          */
        Request request;

        /** _more_          */
        String resultHtml;

        /** _more_          */
        String mimeType = "text/html";

        /** _more_          */
        InputStream inputStream;

        /** _more_          */
        String errorMessage;

        /** _more_          */
        String resultFileName = null;


        /**
         * _more_
         *
         * @param typeHandler _more_
         * @param entry _more_
         * @param request _more_
         * @param sb _more_
         */
        public FormInfo(JythonTypeHandler typeHandler, Entry entry,
                        Request request, StringBuffer sb) {
            this.sb          = sb;
            this.request     = request;
            this.typeHandler = typeHandler;
            this.entry       = entry;
        }

        /**
         * _more_
         *
         * @param value _more_
         */
        public void setErrorMessage(String value) {
            errorMessage = value;
        }

        /**
         * _more_
         *
         * @param f _more_
         */
        public void setResultFileName(String f) {
            resultFileName = f;
        }

        /**
         *  Set the MimeType property.
         *
         *  @param value The new value for MimeType
         */
        public void setMimeType(String value) {
            this.mimeType = value;
        }

        /**
         *  Get the MimeType property.
         *
         *  @return The MimeType
         */
        public String getMimeType() {
            return this.mimeType;
        }

        /**
         *  Set the InputStream property.
         *
         *  @param value The new value for InputStream
         * @param mimeType _more_
         */
        public void setInputStream(InputStream value, String mimeType) {
            this.mimeType    = mimeType;
            this.inputStream = value;
        }

        /**
         *  Get the InputStream property.
         *
         *  @return The InputStream
         */
        public InputStream getInputStream() {
            return this.inputStream;
        }


        /**
         * _more_
         *
         * @param s _more_
         */
        public void append(String s) {
            sb.append(s);
        }

        /**
         * _more_
         *
         * @param html _more_
         */
        public void setResult(String html) {
            resultHtml = html;
        }

        /**
         * _more_
         *
         * @param title _more_
         */
        public void setTitle(String title) {
            this.title = title;
        }


        /**
         * _more_
         *
         * @param prefix _more_
         */
        public void setFormPrefix(String prefix) {
            this.prefix = prefix;
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         */
        public void addFormFileUpload(String id, String label) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_FILE, id));
            sb.append(
                HtmlUtil.formEntry(
                    typeHandler.msgLabel(label),
                    HtmlUtil.fileInput(
                        id, HtmlUtil.attr(HtmlUtil.ATTR_SIZE, "80"))));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         *
         * @throws Exception _more_
         */
        public void addFormEntry(String id, String label) throws Exception {
            inputs.add(new InputInfo(InputInfo.TYPE_ENTRY, id));

            sb.append(HtmlUtil.hidden(id + "_hidden", "",
                                      HtmlUtil.id(id + "_hidden")));
            String select = OutputHandler.getSelect(request, id, "Select",
                                true, null, entry);
            sb.append(HtmlUtil.formEntry(label,
                                         HtmlUtil.disabledInput(id, "",
                                             HtmlUtil.id(id)
                                             + HtmlUtil.SIZE_60) + select));
            cnt++;
        }


        /**
         * _more_
         *
         * @param label _more_
         *
         * @throws Exception _more_
         */
        public void addFormLabel(String label) throws Exception {
            sb.append(HtmlUtil.formEntry("", label));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         * @param columns _more_
         * @param rows _more_
         */
        public void addFormText(String id, String label, String dflt,
                                int columns, int rows) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
            if (rows == 1) {
                sb.append(
                    HtmlUtil.formEntry(
                        typeHandler.msgLabel(label),
                        HtmlUtil.input(
                            id, dflt,
                            HtmlUtil.attr(
                                HtmlUtil.ATTR_SIZE, "" + columns))));
            } else {
                sb.append(HtmlUtil.formEntryTop(typeHandler.msgLabel(label),
                        HtmlUtil.textArea(id, dflt, rows, columns)));
            }
        }

        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         * @param items _more_
         */
        public void addFormSelect(String id, String label, String dflt,
                                  List items) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
            sb.append(HtmlUtil.select(id, items, dflt));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         */
        public void addFormNumber(String id, String label, double dflt) {
            inputs.add(new InputInfo(InputInfo.TYPE_NUMBER, id));
            cnt++;
            sb.append(
                HtmlUtil.formEntry(
                    typeHandler.msgLabel(label),
                    HtmlUtil.input(
                        id, "" + dflt,
                        HtmlUtil.attr(HtmlUtil.ATTR_SIZE, "" + 5))));
        }


    }



}

