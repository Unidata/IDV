
var lineColor="#00FF00";
var lastLine;

var lines = new Object();
var addedToMap = new Object();
var markers = new Object();

function MapInitialize(addControls,mapProvider)
{
	// Create a map object
//	mapstraction = new Mapstraction('mapstraction', 'google');
//	mapstraction = new Mapstraction('mapstraction', 'yahoo');
	var mapstraction = new Mapstraction('mapstraction', mapProvider);
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
        return mapstraction;
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
