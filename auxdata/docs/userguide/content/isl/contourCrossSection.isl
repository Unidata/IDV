<isl debug="ture" offscreen="false">
      <procedure name="mkContourCrossSection">
          <bundle clear="true" file="${islpath}/RUC.xidv"/>
          <pause/>
          <displayproperties display="class:ucar.unidata.idv.control.ContourCrossSectionControl">
            <property name="StartPoint"  value="${startLocation}"/>
            <property name="EndPoint"  value="${endLocation}"/>
          </displayproperties>
          <pause/>
          <image file="${islpath}/contourCS${idx}.png" display="class:ucar.unidata.idv.control.ContourCrossSectionControl"/>
      </procedure>
      <mkContourCrossSection idx="11" startLocation="34.0, -140.0" endLocation="40.0, -55.0"/>
</isl>