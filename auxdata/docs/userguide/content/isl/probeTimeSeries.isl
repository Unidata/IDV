<isl debug="true" offscreen="false">
    <procedure name="mkCHART">
        <bundle clear="true" file="${islpath}/RUC.xidv"/>
        <pause/>
        <displayproperties display="class:ucar.unidata.idv.control.ProbeControl">
          <property name="EarthLocation"  value="${probeLocation}"/>
          <property name="ChartName"  value="Chart #: ${idx}"/>
        </displayproperties>
        <pause/>
        <image file="${islpath}/chart${idx}.png" display="class:ucar.unidata.idv.control.ProbeControl"/>
    </procedure>
    <image file="${islpath}/map.png" />
    <mkCHART idx="10" probeLocation="30.0, -105.0"/>
    <mkCHART idx="11" probeLocation="40.0, -100.0"/>

</isl>
