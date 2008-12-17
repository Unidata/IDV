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


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WikiPageTypeHandler extends GenericTypeHandler {

    /** _more_          */
    public static String TYPE_WIKIPAGE = "wikipage";

    public static final String ARG_WIKI_TEXT = "wikipage.wikitext";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public WikiPageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        initWikiTable(entryNode);

    }

    private void initWikiTable(Element node) throws Exception {
        Statement statement = getDatabaseManager().createStatement();

        StringBuffer tableDef = new StringBuffer("CREATE TABLE "
                                    + getTableName()+"_history" + " (\n");

        tableDef.append(COL_ID + " varchar(200),");
        tableDef.append(COL_ID + " varchar(200),");
        tableDef.append(")");
        try {
            statement.execute(tableDef.toString());
        } catch (Throwable exc) {
        }
        statement.close();
    }



    public void initializeEntry(Request request, Entry entry) throws Exception {
        super.initializeEntry(request,entry);
        //        System.err.println(" entry:" + entry);

        Object[]values = entry.getValues();
        if(values!=null && values.length>1 && values[0]!=null) {
            String wikiText = (String)values[0];
            
            
            //            System.err.println(" got text:" + wikiText);
        //            String wikiText = (String)values[0];
        }

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer sb, Entry entry)
            throws Exception {
        String size = HtmlUtil.SIZE_70;
        String name;
        if(entry!=null) {
            name =entry.getName();
        } else {
            name =request.getString(ARG_NAME,"");
            List tmp = new ArrayList();
            for(String tok: (List<String>)StringUtil.split(name," ", true,true)) {
                tmp.add(StringUtil.camelCase(tok));
            }
            name = StringUtil.join(" ",tmp);
        }
        sb.append(HtmlUtil.formEntry(msgLabel("Wiki Page Title"),
                                     HtmlUtil.input(ARG_NAME, name, size)));
        String wikiText = "";
        if(entry!=null) {
            Object[]values = entry.getValues();
            if(values!=null && values.length>0 && values[0]!=null)
                wikiText = (String)values[0];
        }


        StringBuffer help = new StringBuffer();
        help.append("<b>Import:</b><br>");
        help.append("e.g., <i>{{property &lt;optional arguments&gt;}}</i><br>");
        help.append("Or: <i>{{import entryid property &lt;arguments&gt;}}</i><br>");
        help.append("<i>{{&lt;output identifier&gt;}}</i><br>");


        String select = OutputHandler.getSelect(request, ARG_WIKI_TEXT,
                                                "Add link", true, "wikilink")+HtmlUtil.space(1) +
            OutputHandler.getSelect(request, ARG_WIKI_TEXT,
                                    "Add import entry", true, "entryid");

        StringBuffer buttons = new StringBuffer();
        buttons.append(addButton("button_bold.png","Bold text","\\'\\'\\'","\\'\\'\\'","Bold text","mw-editbutton-bold"));
        buttons.append(addButton("button_italic.png","Italic text","\\'\\'","\\'\\'","Italic text","mw-editbutton-italic"));
        buttons.append(addButton("button_link.png","Internal link","[[","]]","Link title","mw-editbutton-link"));
        buttons.append(addButton("button_extlink.png","External link (remember http:// prefix)","[","]","http://www.example.com link title","mw-editbutton-extlink"));
        buttons.append(addButton("button_headline.png","Level 2 headline","\\n== "," ==\\n","Headline text","mw-editbutton-headline"));
        buttons.append(addButton("button_linebreak.png","Line break","<br>","","","mw-editbutton-headline"));
        buttons.append(addButton("button_strike.png","Strike Through","<s>","</s>","Strike-through text","mw-editbutton-headline"));
        buttons.append(addButton("button_upper_letter.png","Super Script","<sup>","</sup>","Super script text","mw-editbutton-headline"));
        buttons.append(addButton("button_lower_letter.png","Sub Script","<sub>","</sub>","Subscript script text","mw-editbutton-headline"));
        buttons.append(addButton("button_small.png","Small text","<small>","</small>","Small text","mw-editbutton-headline"));
        buttons.append(addButton("button_blockquote.png","Insert block quote","<blockquote>","</blockquote>","Quoted text","mw-editbutton-headline"));
        //        buttons.append(addButton("button_image.png","Embedded file","[[File:","]]","Example.jpg","mw-editbutton-image"));
        //        buttons.append(addButton("button_media.png","File link","[[Media:","]]","Example.ogg","mw-editbutton-media"));
        //        buttons.append(addButton("button_nowiki.png","Ignore wiki formatting","\\x3cnowiki\\x3e","\\x3c/nowiki\\x3e","Insert non-formatted text here","mw-editbutton-nowiki"));
        //        buttons.append(addButton("button_sig.png","Your signature with timestamp","--~~~~","","","mw-editbutton-signature"));
        buttons.append(addButton("button_hr.png","Horizontal line (use sparingly)","\\n----\\n","","","mw-editbutton-hr"));

        StringBuffer propertyMenu = new StringBuffer();
        StringBuffer importMenu = new StringBuffer();
        for(int i=0;i<OutputHandler.WIKIPROPS.length;i++) {
            String prop = OutputHandler.WIKIPROPS[i];
            String js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote("{{") +","+
                HtmlUtil.squote("}}") +","+
                HtmlUtil.squote(prop)+");";
            propertyMenu.append(HtmlUtil.href(js,prop));
            propertyMenu.append(HtmlUtil.br());

            String js2 = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote("{{import ") +","+
                HtmlUtil.squote(" " + prop+"}}") +","+
                HtmlUtil.squote(" entryid ")+");";
            importMenu.append(HtmlUtil.href(js2,prop));
            importMenu.append(HtmlUtil.br());
        }

        List<OutputType> types = getRepository().getOutputTypes(request,
                                                                new OutputHandler.State(entry));


        propertyMenu.append("<hr>");
        for(OutputType type: types) {
            String prop = type.getId();
            String js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote("{{") +","+
                HtmlUtil.squote("}}") +","+
                HtmlUtil.squote(prop)+");";
            propertyMenu.append(HtmlUtil.href(js,type.getLabel()));
            propertyMenu.append(HtmlUtil.br());
        }


        List<OutputType> allTypes = getRepository().getOutputTypes();
        StringBuffer importOutputMenu = new StringBuffer();
        //        importMenu.append("<hr>");
        for(OutputType type: allTypes) {
            String prop = type.getId();
            String js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote("{{import ") +","+
                HtmlUtil.squote(" " + type.getId()+" }}") +","+
                HtmlUtil.squote("entryid")+");";
            importOutputMenu.append(HtmlUtil.href(js,type.getLabel()));
            importOutputMenu.append(HtmlUtil.br());
        }



        String propertyMenuLabel = HtmlUtil.img(fileUrl("/icons/wiki/button_property.png"),"Add Entry Property");
        String propertyButton = getRepository().makeMenuPopupLink(propertyMenuLabel, propertyMenu.toString());
        buttons.append(propertyButton);
        String importMenuLabel = HtmlUtil.img(fileUrl("/icons/wiki/button_import.png"),"Import Entry Property");
        String importButton = getRepository().makeMenuPopupLink(importMenuLabel,         HtmlUtil.hbox(importMenu.toString(),importOutputMenu.toString()));
        buttons.append(importButton);
        buttons.append(HtmlUtil.space(2));
        buttons.append(select);


        String textWidget = buttons+HtmlUtil.br()+HtmlUtil.textArea(ARG_WIKI_TEXT,
                                              wikiText,
                                              200, 80,
                                              HtmlUtil.id(ARG_WIKI_TEXT));
        String right =  HtmlUtil.div(help.toString(),HtmlUtil.cssClass("smallhelp"));
        textWidget = "<table><tr valign=\"top\"><td>" + textWidget
                     + "</td><td>" + right + "</td></tr></table>";
        sb.append(HtmlUtil.formEntryTop(msgLabel("Wiki Text"), textWidget));
    }



    private String addButton(String icon,String label,String prefix, String suffix,String example,String huh) {
        String prop = prefix + example +suffix;
        String js;
        if(suffix.length()==0) {
            js = "javascript:insertText(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote(prop)+");";
        } else {
            js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                HtmlUtil.squote(prefix) +","+
                HtmlUtil.squote(suffix) +","+
                HtmlUtil.squote(example)+");";
        }
        return HtmlUtil.href(js,HtmlUtil.img(fileUrl("/icons/wiki/" + icon), label));

    }

}

