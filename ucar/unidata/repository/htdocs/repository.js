
function getDomObject(name) {
    obj = new DomObject(name);
    if(obj.obj) return obj;
    return null;
}



function Util () {
    this.getEvent = function (event) {
        if(event) return event;
        return window.event;
    }


    this.getEventX =    function (event) {
        if (event.pageX) {
            return  event.pageX;
        }
        return  e.clientX + document.body.scrollLeft
        + document.documentElement.scrollLeft;
    }

    this.getEventY =function (event) {
        if (event.pageY) {
            return  event.pageY;
        }
        return  e.clientY + document.body.scrollTop
        + document.documentElement.scrollTop;

    }

    this.getTop = function (obj) {
        if(!obj) return 0;
        return obj.offsetTop+this.getTop(obj.offsetParent);
    }

    this.getLeft =  function(obj) {
        if(!obj) return 0;
        return obj.offsetLeft+this.getLeft(obj.offsetParent);
    }
}

util = new Util();



function DomObject(name) {
    this.obj = null;
// DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById)	{    	
        this.obj = document.getElementById(name);
        if(this.obj) 
            this.style = this.obj.style;
    }
// IE 4
    else if (document.all)	{  			
        this.obj = document.all[name];
        if(this.obj) 
            this.style = this.obj.style;
    }
// NN 4
    else if (document.layers)  { 		
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
}



function noop() {
}



function print(s) {
    var obj = getDomObject("output");
    if(!obj) return;
    obj.obj.innerHTML  =obj.obj.innerHTML+"<br>" +s;
}





document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;

var mouseIsDown = 0;
var draggedEntry;
var draggedEntryName;
var mouseMoveCnt =0;

function mouseDown(event) {
    event = util.getEvent(event);
    mouseIsDown = 1;
    mouseMoveCnt =0;
    return true;
}



function mouseUp(event) {
    event = util.getEvent(event);
    mouseIsDown = 0;
    draggedEntry   = null;
    setCursor('default');
    var obj = getDomObject('floatdiv');
    if(obj) {
        obj.style.visibility = "hidden";
        obj.style.display = "none";
    }
    return true;
}

function mouseMove(event) {
    event = util.getEvent(event);
    if(draggedEntry && mouseIsDown) {
        mouseMoveCnt++;
        var obj = getDomObject('floatdiv');
        if(mouseMoveCnt==6) {
            setCursor('move');
        }
        if(mouseMoveCnt>=6&& obj) {
            moveFloatDiv(util.getEventX(event),util.getEventY(event));
        }
    }    
    return false;
}


function moveFloatDiv(x,y) {
    var obj = getDomObject('floatdiv');
    if(obj) {
        if(obj.style.visibility!="visible") {
            obj.style.visibility = "visible";
            obj.style.display = "block";
            obj.obj.innerHTML = draggedEntryName;
        }
        obj.style.top = y;
        obj.style.left = x+10;
    }
}

function mouseOverOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    if(mouseIsDown)  {
        var obj = getDomObject("span_" + id);
        if(!obj)  return;
        //       if(obj.style && obj.style.borderBottom) {
        obj.style.borderBottom="2px black solid";
        //        }
    }
}

function mouseOutOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
}




function mouseDownOnEntry(event, id, name) {
    event = util.getEvent(event);
    draggedEntry = id;
    draggedEntryName=name;
    mouseIsDown = 1;
    if(event.preventDefault) {
        event.preventDefault();
    } else {
	event.returnValue = false;
        return false;
    }
}


function mouseUpOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
    if(draggedEntry && draggedEntry!=id) {
        url = "${urlroot}/entry/copy?action=action.move&from=" + draggedEntry +"&to=" + id;
        document.location = url;
    }
}



var ttLastMove = 0;

function tooltipHide(event,id) {
    event = util.getEvent(event);
    ttLastMove++;
    var obj = getDomObject("tooltipdiv");
    if(!obj) return;
    obj.style.visibility = "hidden";
    obj.style.display = "none";
}


function tooltipShow(event,id) {
    event = util.getEvent(event);
    ttLastMove++;
    setTimeout("tooltipDoShow(" + ttLastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+")", 1000);
}




function tooltipDoShow(moveId,x,y,id) {
    var link = getDomObject(id);
    if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
        x= util.getLeft(link.obj);
        y = link.obj.offsetHeight+util.getTop(link.obj) + 2;
    } else {
	x+=20;
    }

    if(ttLastMove!=moveId) return;
    var obj = getDomObject("tooltipdiv");
    if(!obj) return;
    obj.style.top = y;
    obj.style.left = x;

    url = "${urlroot}/entry/show?id=" + id +"&output=metadataxml";
    var request = window.XMLHttpRequest ?
        new XMLHttpRequest() : new ActiveXObject("MSXML2.XMLHTTP.3.0");
    request.onreadystatechange = function()  {
        if (request.readyState == 4 && request.status == 200)   {
            var xmlDoc=request.responseXML.documentElement;
            obj.style.visibility = "visible";
            obj.style.display = "block";
            obj.obj.innerHTML = getChildText(xmlDoc);
        }
    };
    request.open("GET", url);
    request.send(null);
}

function toggleEntryForm() {
    var obj = getDomObject('entryform');
    var img = getDomObject('entryformimg');
    if(obj) {
	if(toggleVisibilityOnObject(obj,'')) {
            if(img) img.obj.src =  "${urlroot}/downarrow.gif";
        } else {
            if(img) img.obj.src =  "${urlroot}/rightarrow.gif";
        }
    }
    var cnt = 0;
    while(1) {
        obj = getDomObject('entryform' + (cnt++));
        if(!obj) break;
	toggleVisibilityOnObject(obj,'');
    }
}


function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    var img = getDomObject(imgid);
    if(toggleVisibility(id)) {
        if(img) img.obj.src = showimg;
    } else {
        if(img) img.obj.src = hideimg;
    }
}




function folderClick(id) {
    var block = getDomObject("block_" + id);
    if(!block) return;
    var img = getDomObject("img_" +id);
    if(!block.obj.isOpen) {
        block.obj.isOpen = 1;
        block.style.visibility = "visible";
        block.style.display = "block";
        if(img) img.obj.src = "${urlroot}/progress.gif";
        url = "${urlroot}/entry/show?id=" + id +"&output=groupxml";
        var request = window.XMLHttpRequest ?
            new XMLHttpRequest() : new ActiveXObject("MSXML2.XMLHTTP.3.0");
        request.onreadystatechange = function()  {
            if (request.readyState == 4 && request.status == 200)   {
                var xmlDoc=request.responseXML.documentElement;
                block.obj.innerHTML = getChildText(xmlDoc);
                if(img) img.obj.src = "${urlroot}/folderopen.gif";
            }
        }
        request.open("GET", url);
        request.send(null);
    } else {
        if(img) img.obj.src = "${urlroot}/folderclosed.gif";
        block.obj.isOpen = 0;
        block.style.visibility = "hidden";
        block.style.display = "none";
    }
}





function setCursor(c) {
    var cursor = document.cursor;
    if(!cursor && document.getElementById) {
        cursor =  document.getElementById('cursor');
    }
    if(!cursor) {
        document.body.style.cursor = c;
    }
}







function  getChildText(xmlDoc) {
    var text = '';
    for(i=0;i<xmlDoc.childNodes.length;i++) {
        text = text  + xmlDoc.childNodes[i].nodeValue;
    }
    return text;
	
}



function toggleVisibility(id) {
    var obj = getDomObject(id);
    return toggleVisibilityOnObject(obj,'block');
}


function hide(id) {
    hideObject(getDomObject(id));
}

function hideObject(obj) {
    if(!obj) return 0;
    obj.style.visibility = "hidden";
    obj.style.display = "none";
}


function showObject(obj, display) {
    if(!obj) return 0;
    obj.style.visibility = "visible";
    obj.style.display = "display";
}



function toggleVisibilityOnObject(obj, display) {
    if(!obj) return 0;
    if(obj.style.visibility == "hidden") {
        obj.style.visibility = "visible";
        obj.style.display = display;
        return 1;
    } else {
        obj.style.visibility = "hidden";
        obj.style.display = "none";
        return 0;
    }
}

