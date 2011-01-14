
<!-- 
        This is an example RBI (Resource Bundle for the IDV) file.
        The IDV uses the RBI file to find out where different resource
        files exist that it uses to configure itself. 

        Each "resources" tag can hold a set of "resource" tags. The resource
        tag has a location attribute that can be a file, url or jar resource,
        that points to some resource to load (e.g., the color tables).
        This location can also hold macros that the IDV replaces with
        the appropriate path:
        %USERPATH% is the .metapps/<AppName> directory that is created in the user's home directory.
        %SITEPATH% is the sitepath defined with the -sitepath argument
        %IDVPATH% is the /ucar/unidata/idv/resources path in the jar file.

        If the resources tag has a loadmore="false" then the IDV
        will not load anymore resources of this type (e.g., this allows you
        to not load the default system resources.) The default  is that loadmore="true"
        
        If you want to add your own resources and/or overwrite the defaults then
        add the appropriate <resource location="..."/>  tag. For your convenience we included
        commented out resource tags below.

        Note: For resource lists that hold "writable" resources (e.g., color tables)
        the first resource in the list is the "writable" resource, i.e., where we write
        new content.

-->


<resourcebundle  name="User resources"> 


<!-- Where the xml is that defines the data choosers -->
  <resources name="idv.resource.choosers" loadmore="false">
        <resource location="%APPPATH%/tb0choosers.xml"/> 
  </resources>

</resourcebundle>
