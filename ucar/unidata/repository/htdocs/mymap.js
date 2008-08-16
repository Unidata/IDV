
var lineColor="#00FF00";
var lastLine;

var lines = new Object();
var markers = new Object();

var mapstraction;
function MapInitialize()
{
	// Create a map object
//	mapstraction = new Mapstraction('mapstraction', 'google');
//	mapstraction = new Mapstraction('mapstraction', 'yahoo');
	mapstraction = new Mapstraction('mapstraction', 'microsoft');
        mapstraction.addSmallControls();
}


//<div style="width:400px; height:400px" id="mapstraction"></div>a


function initMarker(marker,id) {
	markers[id]=marker;
	mapstraction.addMarker(marker);
}

function initLine(line,id) {
 	line.setColor(lineColor);
	line.setWidth(3);
	lines[id]=line;
	mapstraction.addPolyline(line);

}

function hiliteEntry(map,id) {
	var marker = markers[id];
	if(marker) {
		marker.openBubble();
	}
	if(lastLine) {
		lastLine.setColor(lineColor);
		map.removePolyline(lastLine);
		map.addPolyline(lastLine);
	}
	lastLine = lines[id];
	if(lastLine) {
		lastLine.setColor("#FF0000");
		map.removePolyline(lastLine);
		map.addPolyline(lastLine);
	}
}
