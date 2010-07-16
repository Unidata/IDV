
var lineColor="#00FF00";
var bearingLineColor="#00FF00";

var lastLine;

var lines = new Object();
var bearingLines = new Array();
var addedToMap = new Object();
var markers = new Object();

function MapInitialize(addControls,mapProvider,divname) {
	// Create a map object
	var mapstraction = new Mapstraction(divname, mapProvider);

	if(!addControls) {
//		vemap = mapstraction.maps[mapstraction.api];
//		vemap.SetDashboardSize(VEDashboardSize.Tiny);
//		vemap.SetDashboardSize(VEDashboardSize.Small);
//		vemap.LoadMap();
//	        mapstraction.addSmallControls();
	} else {
//	        mapstraction.addSmallControls();
	} 
//	mapstraction.addLargeControls();

	if(mapProvider=='google') {
            mapstraction.setCenterAndZoom(new LatLonPoint(0,0), 1);
	}	

	mapstraction.addControls({
                pan: true, 
                zoom: 'small',
                map_type: true 
            });

        mapstraction.addEventListener( 'click', onClickMap);  

        return mapstraction;
}


function onClickMap( point) {  
    // the yahoo map returns 0 for lat and 180 for lon when user  
    // clicks on a control on the map (for example on the pan-left arrow)  
    if (point.lat != 0) {  
        if(mapSelector) {
            mapSelector.handleMapClick(point.lat, point.lon);
        }
    }  
} 



function initMarker(marker,id,theMap) {
	marker.iconSize = [16,16];
	marker.iconShadowUrl = "${urlroot}/icons/blank.gif";
	markers[id]=marker;
	theMap.addMarker(marker);
}

function initLine(line,id,addItToMap,theMap) {
 	line.setColor(lineColor);
	line.setWidth(3);
	lines[id]=line;
	addedToMap[id]=addItToMap;
        if (addItToMap) {
            theMap.addPolyline(line);
        }
}

var showBearingLines = 0;

function toggleBearingLines(map) {
    showBearingLines = !showBearingLines;
    for (var i = 0; i < bearingLines.length; i++) {
        var bearingLine = bearingLines[i];
        if(showBearingLines) {
            map.addPolyline(bearingLine);
        } else {
            map.removePolyline(bearingLine);
        }
    }
}



function hiliteEntry(map,id) {
	var marker = markers[id];
	if(marker) {
            marker.openBubble();
	}
	if(lastLine) {
		lastLine.setColor(lineColor);
		map.removePolyline(lastLine);
                if(addedToMap[id]) {
                    map.addPolyline(lastLine);
                }
	}
	lastLine = lines[id];
	if(lastLine) {
            lastLine.setColor("#FF0000");
            map.removePolyline(lastLine);
            map.addPolyline(lastLine);
	}
}



var mapSelector;

function MapSelector (argBase, absolute) {
    mapSelector = this;
    this.doFirst = 1;
    this.argBase = argBase;
    this.absolute = absolute;

    this.fldNorth= util.getDomObject(argBase+"_north");
    this.fldSouth= util.getDomObject(argBase+"_south");
    this.fldEast= util.getDomObject(argBase+"_east");
    this.fldWest= util.getDomObject(argBase+"_west");
    this.fldLat= util.getDomObject(argBase+"_lat");
    this.fldLon= util.getDomObject(argBase+"_lon");

    this.selectorPolyline = null;

    this.handleMapClick = function(lat1,lon1) {
        if(this.fldNorth) {
            if(this.doFirst) {
                this.doFirst =0;
                this.fldNorth.obj.value = ""+lat1;
                this.fldWest.obj.value = ""+lon1;
                this.fldSouth.obj.value = "";
                this.fldEast.obj.value = "";
            } else {
                this.doFirst = 1;
                var ns = this.fldNorth.obj.value;
                var ws = this.fldWest.obj.value;
                if(ns!="" && ws!="") {
                    var lat2 = parseFloat(ns);
                    var lon2 = parseFloat(ws);
                    this.fldSouth.obj.value = ""+Math.min(lat1,lat2);
                    this.fldNorth.obj.value = ""+Math.max(lat1,lat2);
                    this.fldEast.obj.value = ""+Math.max(lon1,lon2);
                    this.fldWest.obj.value = ""+Math.min(lon1,lon2);
                } else {
                    this.fldSouth.obj.value = ""+lat1;
                    this.fldEast.obj.value = ""+lon1;
                }
            }
            this.setSelectorBox();
        } else if(this.fldLat) {
            this.fldLat.obj.value = ""+lat1;
            this.fldLon.obj.value = ""+lon1;
        }
    }

    this.setSelectorBox = function() {
            if(this.selectorPolyline) {
                selectormap.removePolyline(this.selectorPolyline);
            }
            var ns = this.fldNorth.obj.value;
            var ss = this.fldSouth.obj.value;
            var es = this.fldEast.obj.value;
            var ws = this.fldWest.obj.value;
            if(es=="") es=ws;
            if(ss=="") ss=ns;

            if(ns!="" &&
               ss!="" &&
               es!="" &&
               ws!="" ) {
                var n = parseFloat(ns);
                var s = parseFloat(ss);
                var e = parseFloat(es);
                var w = parseFloat(ws);
                line = new Polyline([new LatLonPoint(n,w),
                                     new LatLonPoint(n,e),
                                     new LatLonPoint(s,e),
                                     new LatLonPoint(s,w),
                                     new LatLonPoint(n,w)]);
                line.setColor("#FF0000");
                line.setWidth(2);
                this.selectorPolyline = line;
                selectormap.addPolyline(this.selectorPolyline);
            }
    }

    this.clear = function() {
        if(this.selectorPolyline) {
            selectormap.removePolyline(this.selectorPolyline);
        }
        this.doFirst = 1;
        if(this.fldNorth)
            this.fldNorth.obj.value = "";
        if(this.fldSouth)
            this.fldSouth.obj.value = "";
        if(this.fldEast)
            this.fldEast.obj.value = "";
        if(this.fldWest)
            this.fldWest.obj.value = "";
        if(this.fldLat)
            this.fldLat.obj.value = "";
        if(this.fldLon)
            this.fldLon.obj.value = "";
        if(this.box) {
            var style = util.getStyle(this.box);
            style.visibility =  "hidden";
        }
    }


    this.init = function() {
    }

    this.setSelectorBox();
}
