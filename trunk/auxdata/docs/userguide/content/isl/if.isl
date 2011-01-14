<isl loop="1000" sleep="3600">
    <!-- Note the less than sign is escaped -->
    <if expr="${k}&gt;12">
        <then>
            <echo message="Generating image at ${k}:${mm}"/>
            <image file="test.png"/>
        </then>
    </if>
    <!-- Or you can include the expression in a CDATA block:
         so you don't have to escape anything -->
    <if>
        <![CDATA[${k}>12]]>
        <then>
            <echo message="Generating image at ${k}:${mm}"/>
            <image file="test.png"/>
        </then>
    </if>
</isl>
