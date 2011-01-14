<isl>
    <bundle file="test.xidv"/>
    <pause/>

    <output 
        file="images.html" 
        template="file:template.html"
        template:header="${text}"
        template:imagehtml="file:imagetemplate.html">

        <output template="header" text="Here are the images"/>

        <image file="test1.png">
            <thumbnail file="test1thumb.png" width="25%"/>
        </image>
        <output template="imagehtml"  thumb="test1thumb.png" image="test1.png" caption="Test1"/>

        <image file="test2.png">
            <thumbnail file="test2thumb.png" width="25%"/>
        </image>
        <output template="imagehtml"  thumb="test2thumb.png" image="test2.png" caption="Test2"/>

        <image file="test3.png">
            <thumbnail file="test3thumb.png" width="25%"/>
        </image>
        <output template="imagehtml"  thumb="test3thumb.png" image="test3.png" caption="Test3"/>


     </output>

</isl>