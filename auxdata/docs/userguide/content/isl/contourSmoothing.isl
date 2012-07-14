<isl debug="ture" offscreen="false">
      <procedure name="mkSmoothedContour">
          <bundle clear="true" file="${islpath}/RUC.xidv"/>
          <pause/>
          <displayproperties display="class:ucar.unidata.idv.control.ContourPlanViewControl">
            <property name="SmoothingType"  value="${stype}"/>
            <property name="SmoothingFactor"  value="${sfactor}"/>
          </displayproperties>
          <pause/>
          <image file="${islpath}/contourImage${idx}.png"/>
      </procedure>

      <mkSmoothedContour idx="11" sfactor="11" stype="GWFS"/>
</isl>