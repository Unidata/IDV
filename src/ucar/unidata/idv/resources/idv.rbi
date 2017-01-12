<?xml version="1.0" encoding="ISO-8859-1"?>
<resourcebundle name="Default">

<!-- Where the colortables are  -->
  <resources name="idv.resource.colortables">
    <resource location="%USERPATH%/colortables.xml"/>
    <resource location="%SITEPATH%/colortables.xml"/>
    <resource location="%APPPATH%/colortables.xml"/>
    <resource location="%IDVPATH%/colortables.xml"/>
  </resources>

<!-- Where the xml is that defines the data choosers  -->
  <resources name="idv.resource.choosers">
    <resource location="%USERPATH%/choosers.xml"/>
    <resource location="%SITEPATH%/choosers.xml"/>
    <resource location="%APPPATH%/choosers.xml"/>
    <resource location="%IDVPATH%/choosers.xml"/>
  </resources>

<!--The different user interfaces available   -->
  <resources name="idv.resource.skin">
    <resource
       label="User's View"
       location="%USERPATH%/skin.xml">
      <property
         name="default"
         value="true"/>
    </resource>
    <resource
       label="Site View"
       location="%SITEPATH%/skin.xml">
      <property
         name="default"
         value="true"/>
    </resource>

<!--
    <resource
       label="bbq"
       location="%IDVPATH%/skins/bbq.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>
-->



    <resource
       skinid="idv.skin.oneview.map"
       label="Map View&gt;One Pane"
       location="%IDVPATH%/skins/skin.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
      <property
         name="default"
         value="true"/>
    </resource>

    <resource
       label="Component Group&gt;Tabs"
       location="%IDVPATH%/skins/componentgroup.xml">
      <property
         name="layout"
         value="tabs"/>
    </resource>

    <resource
       label="Component Group&gt;Tree"
       location="%IDVPATH%/skins/componentgroup.xml">
      <property
         name="layout"
         value="tree"/>
    </resource>

    <resource
       label="Component Group&gt;One column grid"
       location="%IDVPATH%/skins/componentgroup.xml">
      <property
         name="layout"
         value="gridbag"/>
      <property
         name="layout_columns"
         value="1"/>
    </resource>

    <resource
       label="Component Group&gt;Two column grid"
       location="%IDVPATH%/skins/componentgroup.xml">
      <property
         name="layout"
         value="gridbag"/>
      <property
         name="layout_columns"
         value="2"/>
    </resource>

    <resource
       label="Component Group&gt;Three column grid"
       location="%IDVPATH%/skins/componentgroup.xml">
      <property
         name="layout"
         value="gridbag"/>
      <property
         name="layout_columns"
         value="3"/>
    </resource>





    <resource
       skinid="idv.skin.twoview.map"
       label="Map View&gt;Two Panes"
       location="%IDVPATH%/skins/twoviewskin.xml">
      <property
         name="left_view_class"
         value="ucar.unidata.idv.MapViewManager"/>
      <property
         name="right_view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.threeview.map"
       label="Map View&gt;Three Panes"
       location="%IDVPATH%/skins/threeviewskin.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.fourview.map"
       label="Map View&gt;Four Panes"
       location="%IDVPATH%/skins/fourviewskin.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.oneview.globe"
       label="Globe View&gt;One Pane"
       location="%IDVPATH%/skins/globeskin.xml"/>

    <resource
       skinid="idv.skin.twoview.globe"
       label="Globe View&gt;Two Panes"
       location="%IDVPATH%/skins/twoglobeskin.xml">
    </resource>

    <resource
       skinid="idv.skin.threeview.globe"
       label="Globe View&gt;Three Panes"
       location="%IDVPATH%/skins/threeglobeskin.xml"/>

    <resource
       skinid="idv.skin.fourview.globe"
       label="Globe View&gt;Four Panes"
       location="%IDVPATH%/skins/fourglobeskin.xml"/>

<!--  
    <resource
       location="%IDVPATH%/skins/racecar.xml"
       label="Misc&gt;Race Car">
     <property name="view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>
 -->

    <resource
       skinid="idv.skin.oneview.transect"
       label="Transect View&gt;One Pane"
       location="%IDVPATH%/skins/skin.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.TransectViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.twoview.transect"
       label="Transect View&gt;Two Panes"
       location="%IDVPATH%/skins/twoviewskin.xml">
      <property
         name="left_view_class"
         value="ucar.unidata.idv.TransectViewManager"/>
      <property
         name="right_view_class"
         value="ucar.unidata.idv.TransectViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.twoview.transectmap"
       label="Misc&gt;Transect and Map"
       location="%IDVPATH%/skins/twoviewskin.xml">
      <property
         name="left_view_class"
         value="ucar.unidata.idv.TransectViewManager"/>
      <property
         name="right_view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.oneview.map2d"
       label="Misc&gt;2D Map View"
       location="%IDVPATH%/skins/skin2D.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin.dashboard"
       label="Windows&gt;Dashboard"
       location="%IDVPATH%/skins/dashboard.xml"></resource>

    <resource
       skinid="idv.skin.fieldselector"
       label="Windows&gt;Field Selector"
       location="%IDVPATH%/skins/windowtemplate.xml">
      <property  name="contents"><![CDATA[<idv.dataselector id="idv.dataselector"/>]]></property>
    </resource>

    <resource
       skinid="idv.skin.datachooser"
       label="Windows&gt;Data Chooser"
       location="%IDVPATH%/skins/windowtemplate.xml">
      <property  name="contents"><![CDATA[<idv.choosers  intabs="false"/>]]></property>
    </resource>

    <resource
       skinid="idv.skin.quicklinks"
       label="Windows&gt;Quick Links"
       location="%IDVPATH%/skins/windowtemplate.xml">
      <property  name="contents"><![CDATA[<idv.quicklinks/>]]></property>
      </resource>


    <resource
       skinid="idv.skin.fieldselectorchooser"
       label="Windows&gt;Field Selector and Chooser"
       location="%IDVPATH%/skins/windowtemplate.xml">
      <property  name="contents"><![CDATA[<splitpane divider="200" onetouchexpandable="true" orientation="v"><idv.dataselector id="idv.dataselector"/><idv.choosers intabs="false"/></splitpane>]]></property>
      </resource>


  </resources>

<!--Defines the toolbar icons   -->
  <resources name="idv.resource.toolbar">
    <resource location="%USERPATH%/toolbar.xml"/>
    <resource location="%SITEPATH%/toolbar.xml"/>
    <resource location="%APPPATH%/toolbar.xml"/>
    <resource location="%IDVPATH%/toolbar.xml"/>
  </resources>


<!--Defines the toolbar icons   -->
  <resources name="idv.resource.publishers">
    <resource location="%USERPATH%/publishers.xml"/>
    <resource location="%SITEPATH%/publishers.xml"/>
    <resource location="%APPPATH%/publishers.xml"/>
    <resource location="%IDVPATH%/publishers.xml"/>
  </resources>

<!-- Defines the actions for the toolbar, etc   -->
  <resources name="idv.resource.actions">
    <resource location="%USERPATH%/actions.xml"/>
    <resource location="%SITEPATH%/actions.xml"/>
    <resource location="%APPPATH%/actions.xml"/>
    <resource location="%IDVPATH%/actions.xml"/>
  </resources>

<!-- Where to find the parameter group files   -->
  <resources name="idv.resource.paramgroups">
    <resource location="%USERPATH%/paramgroups.xml"
       label="User Groups"/>
    <resource location="%SITEPATH%/paramgroups.xml"
       label="Site Groups"/>
    <resource location="%APPPATH%/paramgroups.xml"
       label="Application Groups"/>
    <resource location="%IDVPATH%/paramgroups.xml"
       label="System Groups"/>
  </resources>

<!-- Where to find the specification of the derived quantities   -->
  <resources name="idv.resource.derived">
    <resource location="%USERPATH%/derived.xml"/>
    <resource location="%SITEPATH%/derived.xml"/>
    <resource location="%IDVPATH%/enduserformulas.xml"/>
    <resource location="%APPPATH%/derived.xml"/>
    <resource location="%IDVPATH%/derived.xml"/>
  </resources>



  <resources name="idv.resource.displaysettings">
    <resource
       label="User defaults"
       location="%USERPATH%/displaysettings.xml"/>
    <resource
       label="Site defaults"
       location="%SITEPATH%/displaysettings.xml"/>
    <resource
       label="Application defaults"
       location="%APPPATH%/displaysettings.xml"/>
    <resource
       label="System defaults"
       location="%IDVPATH%/displaysettings.xml"/>
  </resources>



<!-- Where to find the parameter to color table files   -->
  <resources name="idv.resource.paramdefaults">
    <resource
       label="User defaults"
       location="%USERPATH%/paramdefaults.xml"/>
    <resource
       label="Site defaults"
       location="%SITEPATH%/paramdefaults.xml"/>
    <resource
       label="Application defaults"
       location="%APPPATH%/paramdefaults.xml"/>
    <resource
       label="System defaults"
       location="%IDVPATH%/paramdefaults.xml"/>
  </resources>

<!-- The list of station table xml files   -->
  <resources name="idv.resource.locations">
    <resource location="%USERPATH%/userstations.xml"/>
    <resource location="%APPPATH%/places.xml"/>
    <resource location="%IDVPATH%/stations/places.xml"/>
    <resource
       id="nexrad"
       location="%IDVPATH%/stations/nexradstns.xml"
       type="radar"/>
    <resource location="%IDVPATH%/stations/statelocations.xml"/>
  </resources>

<!-- The list of help tip xml files   -->
  <resources name="idv.resource.helptips">
    <resource location="%SITEPATH%/helptips.xml"/>
    <resource location="%APPPATH%/helptips.xml"/>
    <resource location="%IDVPATH%/helptips.xml"/>
  </resources>

<!-- The list of projection table xml files   -->
  <resources name="idv.resource.projections">
    <resource location="%USERPATH%/projections.xml"/>
    <resource location="%SITEPATH%/projections.xml"/>
    <resource location="%APPPATH%/projections.xml"/>
    <resource location="%IDVPATH%/projections.xml"/>
    <resource location="%IDVPATH%/stateprojections.xml"/>
  </resources>

<!-- The list of transect xml files   -->
  <resources name="idv.resource.transects">
    <resource location="%USERPATH%/transects.xml"/>
    <resource location="%SITEPATH%/transects.xml"/>
    <resource location="%APPPATH%/transects.xml"/>
    <resource location="%IDVPATH%/transects.xml"/>
  </resources>

<!-- Where to find the data source specifications   -->
  <resources name="idv.resource.datasource">
    <resource location="%USERPATH%/datasource.xml"/>
    <resource location="%SITEPATH%/datasource.xml"/>
    <resource location="%APPPATH%/datasource.xml"/>
    <resource location="%IDVPATH%/datasource.xml"/>
  </resources>


<!-- Where to find the adde server specifications   -->
  <resources name="idv.resource.addeservers">
    <resource location="%USERPATH%/addeservers.xml"/>
    <resource location="%SITEPATH%/addeservers.xml"/>
    <resource location="%APPPATH%/addeservers.xml"/>
    <resource location="%IDVPATH%/addeservers.xml"/>
  </resources>


<!-- Where to find the specification of the display controls used    -->
  <resources name="idv.resource.controls">
    <resource location="%USERPATH%/controls.xml"/>
    <resource location="%SITEPATH%/controls.xml"/>
    <resource location="%APPPATH%/controls.xml"/>
    <resource location="%IDVPATH%/controls.xml"/>
  </resources>

<!-- Where to find the parameter aliases   -->
  <resources name="idv.resource.aliases">
    <resource
       label="User aliases"
       location="%USERPATH%/aliases.xml"/>
    <resource
       label="Site aliases"
       location="%SITEPATH%/aliases.xml"/>
    <resource
       label="Application aliases"
       location="%APPPATH%/aliases.xml"/>
    <resource
       label="System aliases"
       location="%IDVPATH%/aliases.xml"/>
  </resources>

  <resources name="idv.resource.translations">
      <resource
          label="Translations"
          location="%IDVPATH%/translations.xml"/>
  </resources>

<!-- Where do we find the default bundle(s) that is(are) used at start up   -->
  <resources name="idv.resource.bundles">
    <resource location="%USERPATH%/default.xidv"/>
    <resource location="%SITEPATH%/default.xidv"/>
  </resources>

<!-- Where do we find the xml definition of the 'favorites' bundles   -->
  <resources name="idv.resource.bundlexml">
    <resource location="%USERPATH%/bundles.xml"/>
    <resource location="%SITEPATH%/bundles.xml"/>
    <resource location="%APPPATH%/bundles.xml"/>
    <resource location="%IDVPATH%/bundles.xml"/>
    <resource location="http://ramadda.unidata.ucar.edu/repository/entry/show?entryid=5096049b-17cb-443a-9c85-d9795b954a2e&amp;output=idv.bundles&amp;top= "/>
    <resource location="http://ramadda.unidata.ucar.edu/repository/entry/show?entryid=499df7f2-6d4e-426e-852b-92d4a282b94a&amp;output=idv.bundles&amp;top= "/>
  </resources>

<!-- This points to the xml document(s) that hold the user defined chooser panels   -->
  <resources name="idv.resource.userchooser">
    <resource location="%USERPATH%/userchooser.xml"/>
    <resource location="%SITEPATH%/userchooser.xml"/>
    <resource location="%APPPATH%/userchooser.xml"/>
    <resource location="%IDVPATH%/userchooser.xml"/>
  </resources>

<!-- Python libraries    -->
  <resources name="idv.resource.jython">
    <resource
       label="User's Library"
       location="%USERPATH%/python/default.py"/>

    <resource
      location="%USERPATH%/python">
      <property
         name="category"
         value="Local Library"/>
    </resource>
    <resource
       label="Site Library"
       location="%SITEPATH%/default.py"/>
    <resource
       label="Application Library"
       location="%APPPATH%/default.py"/>
    <resource
       label="Grid Routines"
       location="%IDVPATH%/python/grid.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Ensemble Grid Routines"
       location="%IDVPATH%/python/ensemble.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Image Routines"
       location="%IDVPATH%/python/image.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Shell Utilities"
       location="%IDVPATH%/python/shell.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Miscellaneous Routines"
       location="%IDVPATH%/python/misc.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Constants"
       location="%IDVPATH%/python/constants.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Grid Diagnostics"
       location="%IDVPATH%/python/griddiag.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Radar Diagnostics"
       location="%IDVPATH%/python/radar.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Map Routines"
       location="%IDVPATH%/python/maps.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="Climatology Diagnostics"
       location="%IDVPATH%/python/climate.py">
      <property
         name="category"
         value="System"/>
    </resource>
    <resource
       label="ISL"
       location="%IDVPATH%/python/isl.py">
      <property
         name="category"
         value="System"/>
      <property
         name="showineditor"
         value="true"/>
     </resource>
    <resource
       label="Test Routines"
       location="%IDVPATH%/python/test.py">
      <property
         name="category"
         value="System"/>
      <property
         name="showineditor"
         value="false"/>
     </resource>
  </resources>

<!--We don't use this now. Python libraries     -->
  <resources name="idv.resource.jythontocopy">
    <resource location="/auxdata/jython/lib/subs.py"/>
    <resource location="/auxdata/jython/lib/graph.py"/>
  </resources>

<!-- Holds an xml specification of the menu bar used in the guis   -->
  <resources name="idv.resource.menubar">
    <resource location="%IDVPATH%/defaultmenu.xml"/>
    <resource location="%APPPATH%/defaultmenu.xml"/>
    <resource location="%SITEPATH%/defaultmenu.xml"/>
    <resource location="%USERPATH%/defaultmenu.xml"/>
  </resources>

<!-- Defines the set of system maps  -->
  <resources name="idv.resource.maps">
    <resource location="%USERPATH%/maps.xml"/>
    <resource location="%SITEPATH%/maps.xml"/>
    <resource location="%APPPATH%/maps.xml"/>
    <resource location="/auxdata/maps/maps.xml"/>
  </resources>

<!-- Defines the set of system maps  -->
  <resources name="idv.resource.globemaps">
    <resource location="%USERPATH%/globemaps.xml"/>
    <resource location="/auxdata/maps/globemaps.xml"/>
  </resources>


<!-- Where we find station models  -->
  <resources name="idv.resource.stationmodels">
    <resource location="%USERPATH%/stationmodels.xml"/>
    <resource location="%SITEPATH%/stationmodels.xml"/>
    <resource location="%APPPATH%/stationmodels.xml"/>
    <resource location="%IDVPATH%/stationmodels.xml"/>
  </resources>


<!-- Where we find viewpoints  -->
  <resources name="idv.resource.viewpoints">
    <resource location="%USERPATH%/viewpoints.xml"/>
    <resource location="%SITEPATH%/viewpoints.xml"/>
    <resource location="%APPPATH%/viewpoints.xml"/>
    <resource location="%IDVPATH%/viewpoints.xml"/>
  </resources>

<!-- What goes into the station model editor  -->
  <resources name="idv.resource.stationsymbols">
    <resource location="%USERPATH%/stationsymbols.xml"/>
    <resource location="%SITEPATH%/stationsymbols.xml"/>
    <resource location="%APPPATH%/stationsymbols.xml"/>
    <resource location="%IDVPATH%/stationsymbols.xml"/>
  </resources>

<!-- Defaults for adde  based imagery  -->
  <resources name="idv.resource.imagedefaults">
    <resource location="%USERPATH%/imagedefaults.xml"/>
    <resource location="%SITEPATH%/imagedefaults.xml"/>
    <resource location="%APPPATH%/imagedefaults.xml"/>
    <resource location="%IDVPATH%/imagedefaults.xml"/>
  </resources>

<!-- We don't use this now but it will hold message catalogs  -->
  <resources name="idv.resource.messages">
    <resource location="%USERPATH%/messages.properties"/>
    <resource location="%SITEPATH%/messages.properties"/>
    <resource location="%APPPATH%/messages.properties"/>
    <resource location="%IDVPATH%/messages.properties"/>
  </resources>

<!-- Where to find extra grib 1 lookup tables  -->
  <resources name="idv.resource.grib1lookuptables">
    <resource location="%USERPATH%/grib1lookuptable.lst"/>
    <resource location="%SITEPATH%/grib1lookuptable.lst"/>
    <resource location="%APPPATH%/grib1lookuptable.lst"/>
    <resource location="%IDVPATH%/grib1lookuptable.lst"/>
  </resources>

<!-- Where to find extra grib 2 lookup tables  -->
  <resources name="idv.resource.grib2lookuptables">
    <resource location="%USERPATH%/grib2lookuptable.lst"/>
    <resource location="%SITEPATH%/grib2lookuptable.lst"/>
    <resource location="%APPPATH%/grib2lookuptable.lst"/>
    <resource location="%IDVPATH%/grib2lookuptable.lst"/>
  </resources>

<!-- Where to look for extensions  -->
  <resources name="idv.resource.plugins">
    <resource location="%USERPATH%/plugins"/>
  </resources>

<!-- Where to look for extensions  -->
  <resources name="idv.resource.pluginindex">
    <resource location="http://www.unidata.ucar.edu/software/idv/plugins/plugins.xml"/>
  </resources>

<!-- Where to look for prototypes  -->
  <resources name="idv.resource.prototypes">
    <resource location="%USERPATH%/prototypes"/>
  </resources>

<!-- Where are the  quicklink html pages  -->
  <resources name="idv.resource.quicklinks">

<!--
            <resource location="ucar.unidata.idv.chooser.RadarWizard.class"/>
   -->
  </resources>

<!-- Where we find the color pairs for the foreground/background colors  -->
  <resources name="idv.resource.colorpairs">
    <resource location="%USERPATH%/colorpairs.xml"/>
    <resource location="%SITEPATH%/colorpairs.xml"/>
    <resource location="%APPPATH%/colorpairs.xml"/>
    <resource location="%IDVPATH%/colorpairs.xml"/>
  </resources>

<!-- Automatically create displays from the data  -->
  <resources name="idv.resource.autodisplays">
    <resource location="%USERPATH%/autodisplays.xml"/>
    <resource location="%SITEPATH%/autodisplays.xml"/>
    <resource location="%APPPATH%/autodisplays.xml"/>
    <resource location="%IDVPATH%/autodisplays.xml"/>
  </resources>

<!-- Defines the background image wms info   -->
  <resources name="idv.resource.backgroundwms">
    <resource location="%USERPATH%/backgroundwms.xml"/>
    <resource location="%SITEPATH%/backgroundwms.xml"/>
    <resource location="%APPPATH%/backgroundwms.xml"/>
    <resource location="%IDVPATH%/backgroundwms.xml"/>
  </resources>

<!-- Defines the image set xml   -->
  <resources name="idv.resource.imagesets">
    <resource location="%USERPATH%/imagesets.xml"/>
    <resource location="%SITEPATH%/imagesets.xml"/>
    <resource location="%APPPATH%/imagesets.xml"/>
    <resource location="%IDVPATH%/imagesets.xml"/>
    <resource location="http://www.unidata.ucar.edu/georesources/webcams/images/index.xml"/>
  </resources>

  <resources name="idv.resource.njconfig">
    <resource location="%USERPATH%/njConfig.xml"/>
    <resource location="%USERHOME%/.unidata/nj22Config.xml"/>
    <resource location="%IDVPATH%/njConfig.xml"/>
  </resources>

<!-- Where to find extra gempak grid lookup tables  -->
  <resources name="idv.resource.gempakgridparam">
    <resource location="%USERPATH%/gempakparamtable.tbl"/>
    <resource location="%SITEPATH%/gempakparamtable.tbl"/>
    <resource location="%APPPATH%/gempakparamtable.tbl"/>
  </resources>

<!-- Where to find url maps -->
  <resources name="idv.resource.urlmaps">
    <resource location="%USERPATH%/urlmaps.xml"/>
    <resource location="%SITEPATH%/urlmaps.xml"/>
    <resource location="%APPPATH%/urlmaps.xml"/>
    <resource location="%IDVPATH%/urlmaps.xml"/>
  </resources>

<!-- Where to find variable renaming maps for use at the datasource level -->
  <resources name="idv.resource.variablerenamer">
    <resource location="%USERPATH%/varrenamer.xml"/>
    <resource location="%SITEPATH%/varrenamer.xml"/>
    <resource location="%APPPATH%/varrenamer.xml"/>
    <resource location="%IDVPATH%/varrenamer.xml"/>
  </resources>

</resourcebundle>
