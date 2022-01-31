<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
         "https://java.sun.com/products/javahelp/helpset_1_0.dtd">

<helpset version="1.0">

  <!-- title -->
  <title>IDV User's Guide</title>

  <!-- maps -->
  <maps>
     <homeID>top</homeID>
     <mapref location="processed/Map.xml"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>Table Of Contents</label>
    <type>javax.help.TOCView</type>
    <data>processed/TOC.xml</data>
  </view>

  <!-- index -->
  <!--
  <view>
    <name>Index</name>
    <label>Index</label>
    <type>javax.help.IndexView</type>
    <data>Index.xml</data>
  </view>
  -->

  <!-- search -->
  <view>
    <name>Search</name>
    <label>Search</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      JavaHelpSearch
    </data>
  </view>


</helpset>
