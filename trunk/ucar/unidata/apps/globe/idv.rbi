<?xml version="1.0" encoding="UTF-8"?>

<resourcebundle name="Default">

  <resources name="idv.resource.choosers"  removeprevious="true" loadmore="false">
    <resource location="%USERPATH%/choosers.xml"/>
  </resources>

  <resources name="idv.resource.toolbar" removeprevious="true" loadmore="false">
    <resource location="%USERPATH%/toolbar.xml"/>
  </resources>

  <resources name="idv.resource.datasource"  loadmore="false">
    <resource location="%USERPATH%/datasource.xml"/>
  </resources>

  <resources name="idv.resource.controls" loadmore="false">
    <resource location="%USERPATH%/controls.xml"/>
  </resources>

  <resources name="idv.resource.skin" loadmore="false" removeprevious="true">
    <resource location="/ucar/unidata/apps/globe/skin.xml"/>
  </resources>

  <!-- The list of station table xml files  -->
  <resources name="idv.resource.locations" loadmore="false" removeprevious="true">
    <resource location="%USERPATH%/userstations.xml"/>
    <resource location="%IDVPATH%/stations/nexradstns.xml"   id="nexrad"/>
    <resource location="/ucar/unidata/apps/simple/places.xml"/>
  </resources>


  <resources name="idv.resource.menubar" removeprevious="true" loadmore="false">
  </resources>

</resourcebundle>
