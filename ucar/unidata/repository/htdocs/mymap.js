
var lineColor="#00FF00";
var lastLine;

var lines = new Object();
var addedToMap = new Object();
var markers = new Object();

var mapstraction;
function MapInitialize(addControls,mapProvider)
{
	// Create a map object
//	mapstraction = new Mapstraction('mapstraction', 'google');
//	mapstraction = new Mapstraction('mapstraction', 'yahoo');
	mapstraction = new Mapstraction('mapstraction', mapProvider);
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

}


function initMarker(marker,id) {
	marker.iconSize = [16,16];
	marker.iconShadowUrl = "${urlroot}/icons/blank.gif";
	markers[id]=marker;
	mapstraction.addMarker(marker);
}

function initLine(line,id,addItToMap) {
 	line.setColor(lineColor);
	line.setWidth(3);
	lines[id]=line;
	addedToMap[id]=addItToMap;
        
        if (addItToMap) {
            mapstraction.addPolyline(line);
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
