<isl>
    <output file="output.txt">
        <output text="Some text to write"/>
<!-- Read in the contents of the file. Apply properties to the name and the contents -->
        <output fromfile="${islpath}/file.txt"/>

        <output>Some more text</output>
        <output><![CDATA[
Some text in a CDATA Section. Thi allows you to have " and < without 
escaping the in the xml
        >]]></output>
   </output>

</isl>