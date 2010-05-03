
var lineColor="#00FF00";
var lastLine;

var lines = new Object();
var addedToMap = new Object();
var markers = new Object();

function MapInitialize(addControls,mapProvider,divname)
{
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

        /*
        mapstraction.addEventListener( 'click', onClickMap);  
        function onClickMap( point) {  
            alert('click ' + point.lat);
            // the yahoo map returns 0 for lat and 180 for lon when user  
            // clicks on a control on the map (for example on the pan-left arrow)  
             if (point.lat != 0) {  
                 mapstraction.addMarker( new Marker( new LatLonPoint(point.lat,point.lon)));  
             }  
        } 
        */ 

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
