
    var myCanvas;

    function printit(ll) {
        var n = ll.north;
        var s = ll.south;
        var e = ll.east;
        var w = ll.west;
        
        if (w > e && e < 0) {
        	e = 360 + e;
        }
        document.getElementById('area_north').value = n;
        document.getElementById('area_west').value = w;
        document.getElementById('area_east').value = e;
        document.getElementById('area_south').value = s;
    }

    function printpoly(poly) {
    }

    function printrc(rcbox) {
    }

    function printmouse(pt) {
    }

    function toggleXY(theBox) {
        myCanvas.setShowXY(theBox.checked);
    }

    function toggleLatLon(theBox) {
        myCanvas.setShowLatLon(theBox.checked);
    }

    function toggleSphRect(theBox) {
        myCanvas.setShowSphericalRectangle(theBox.checked);
    }

    function toggleRowCol(theBox) {
        myCanvas.setShowRowCol(theBox.checked);
    }

    function changeView(viewSel) {
        myCanvas.changeAspect(viewSel);
    }

    function zoomin_box() {
        myCanvas.zoom('in_box');
    }

    function zoomin_center() {
        myCanvas.zoom('in_center');
    }

    function zoomout_center() {
        myCanvas.zoom('out_center');
    }

    function dopan(xdir, ydir) {
        myCanvas.pan(xdir, ydir);
    }

    function changeFactor(theSel) {
        myCanvas.changeZoomFactor(theSel.options[theSel.selectedIndex].value);
    }

    function change_dblclick(dblclk_sel) {
        myCanvas.changeDblClickMode(dblclk_sel.options[dblclk_sel.selectedIndex].value);
    }

    function changeLatLon(latlontype, newvalue) {
        myCanvas.adjustLatLonParam(latlontype, newvalue);
    }

    function checksize() {
        var imgsize = myCanvas.getImageSize();
        alert('The image is ' + imgsize.width + ' wide by ' + imgsize.height + ' high.');
    }

    var mx = new Array(1);
    var g  = new Array(1);
    var map = new Array(1);

    function initMap(canvasName) {
        var canvas = document.getElementById(canvasName);
	if(!canvas) return;
        var mm = new MapMaker();
        mx[0] = mm.createMapx('Azimuthal Equal-Area',  90, 0, 0, 0, 0, 1,  90, 0,   0, 90, -180, 180, 15, 30, 0, 0, 0, 0, 1, 6371228.0);
        mx[1] = mm.createMapx('Azimuthal Equal-Area', -90, 0, 0, 0, 0, 1, -90, 0, -90,  0, -180, 180, 15, 30, 0, 0, 0, 0, 1, 6371228.0);
        
        myCanvas = new WMSMapCanvas(canvas,128,128);

        var a = new WMSAspect('north', 
                              'http://www.nsidc.org/cgi-bin/nsidc_ogc_north.pl', 
                              'EPSG:3408', 
                              {minx:-9036842.762,miny:-9036842.762,maxx:9036842.762,maxy:9036842.762},
                              'blue_marble_07_circle,north_pole_geographic',
                              19, 19, 
                              mx[0]);
        myCanvas.addAspect(a);

        var a = new WMSAspect('south',
                              'http://www.nsidc.org/cgi-bin/nsidc_ogc_south.pl',
                              'EPSG:3409',
                              {minx:-9036842.762,miny:-9036842.762,maxx:9036842.762,maxy:9036842.762},
                              'blue_marble_01_circle,south_pole_geographic',
                              19, 19,
                              mx[1]);
        myCanvas.addAspect(a);

        myCanvas.addLatLonListener(printit);
        myCanvas.addLatLonPolygonListener(printpoly);
        myCanvas.addRowColListener(printrc);
        myCanvas.addMousePosListener(printmouse);
        myCanvas.setShowLatLon(true);
        myCanvas.setShowXY(false);
        myCanvas.setShowSphericalRectangle(false);
        myCanvas.setShowRowCol(false);
    }





