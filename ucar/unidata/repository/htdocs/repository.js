

function getObj(name)	{
          this.obj = null;
	  if (document.getElementById)	{    	// DOM level 1 browsers: IE 5+, NN 6+
	  	this.obj = document.getElementById(name);
                if(this.obj) 
                     this.style = this.obj.style;
	  }
	  else if (document.all)	{  			// IE 4
		this.obj = document.all[name];
                if(this.obj) 
                    this.style = this.obj.style;
	  }
	  else if (document.layers)  { 			// NN 4
	   	this.obj = document.layers[name];
	   	this.style = document.layers[name];
	  }
}



function noop()  
{
}



function print(s) {
  var obj = new getObj("output");
  if(!obj) return;
  obj.obj.innerHTML  =s;
}


var cnt = 0;
function tooltipHide(event,id)  
{
  cnt++;
//  print('got hjavascript: noop()ide ' + cnt);
  lastMove++;
  var obj = new getObj("tooltipdiv");
  if(!obj) return;
  obj.style.visibility = "hidden";
  obj.style.display = "none";
}






document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;

var mouseIsDown = 0;
var draggedEntry;
var draggedEntryName;
var mouseMoveCnt =0;

function mouseDown(ev){
    mouseIsDown = 1;
    mouseMoveCnt =0;
    return true;
}


function mouseUp(ev){
    mouseIsDown = 0;
    draggedEntry   = null;
    setCursor('default')
    var obj = new getObj('floatdiv');
    if(obj) {
        obj.style.visibility = "hidden";
        obj.style.display = "none";
    }
    return true;
}

function mouseMove(event) {
    if(draggedEntry && mouseIsDown) {
        mouseMoveCnt++;
        var obj = new getObj('floatdiv');
        if(mouseMoveCnt==6) {
           setCursor('move')
        }
        if(mouseMoveCnt>=6&& obj) {
            moveFloatDiv(event.clientX,event.clientY);
        }
    }    
    return false;
//    return true;
}


function moveFloatDiv(x,y) {
        var obj = new getObj('floatdiv');
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
   if(id == draggedEntry) return;
   if(mouseIsDown)  {
      var obj = new getObj("span_" + id);
      if(!obj)  return;
//       if(obj.style && obj.style.borderBottom) {
           obj.style.borderBottom="2px black solid";
//        }
   }
}

function mouseOutOnEntry(event, id) {
   if(id == draggedEntry) return;
   var obj = new getObj("span_" + id);
   if(!obj)  return;
   if(mouseIsDown)  {
       obj.style.borderBottom="";
   }
}


function setCursor(c) {
    var cursor = document.cursor;
    if(!cursor && document.getElementById)
        cursor =  document.getElementById('cursor');
    if(!cursor) {
        document.body.style.cursor = c;
    }
    //    if(!cursor)
    //        cursor =  document.all.cursor;

}


function mouseDownOnEntry(event, id, name) {
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
   if(id == draggedEntry) return;
   var obj = new getObj("span_" + id);
   if(!obj)  return;
   if(mouseIsDown)  {
       obj.style.borderBottom="";
   }
   if(draggedEntry && draggedEntry!=id) {
       url = "${urlroot}/entry/copy?action=action.move&from=" + draggedEntry +"&to=" + id;
       document.location = url
   }
}



var lastMove = 0;
function tooltipShow(event,id) 
{
    lastMove++;
    //    setTimeout("tooltipNowShow(" + lastMove+"," +event.clientX+","+ event.clientY +"," + "'" + id +"'"+")", 2000);
    setTimeout("tooltipNowShow(" + lastMove+"," +event.clientX+","+ event.clientY +"," + "'" + id +"'"+")", 1000);
}






function getTop(obj) {
    if(!obj) return 0;
    return obj.offsetTop+getTop(obj.offsetParent);
}

function getLeft(obj) {
    if(!obj) return 0;
    return obj.offsetLeft+getLeft(obj.offsetParent);
}


function tooltipNowShow(moveId,x,y,id) 
{

   var link = new getObj(id);

   if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
       x= getLeft(link.obj);
       y = link.obj.offsetHeight+getTop(link.obj) + 2;
       //       print("link:" +x + " " + y);
   } else {
	x+=20;
     //	y+=20;
   }

  if(lastMove!=moveId) return
  var obj = new getObj("tooltipdiv");
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
//  alert("tooltip:" + event.clientX);
}

function toggleEntryForm() {
    var obj = new getObj('entryform');
    var img = new getObj('entryformimg');
    if(obj) {
	if(toggleVisibilityOnObject(obj,'')) {
              if(img) img.obj.src =  "${urlroot}/downarrow.gif";
         } else {
              if(img) img.obj.src =  "${urlroot}/rightarrow.gif";
         }
    }
    var cnt = 0;
    while(1) {
        obj = new getObj('entryform' + (cnt++))
	if(!obj.obj) break;
	toggleVisibilityOnObject(obj,'');
    }
}


function hideShow(id,imgid,showimg,hideimg) 
{
    var img = new getObj(imgid);

    if(toggleVisibility(id)) {
	  if(img) img.obj.src = showimg;
    } else {
	  if(img) img.obj.src = hideimg;
    }
}






function hide(id) 
{
  var obj = new getObj(id);
  if(!obj) return 0;
  obj.style.visibility = "hidden";
  obj.style.display = "none";
}


function  getChildText(xmlDoc) {
    var text = '';
    for(i=0;i<xmlDoc.childNodes.length;i++) {
       text = text  + xmlDoc.childNodes[i].nodeValue;
    }
    return text;
	
}

function folderClick(id) {
  var block = new getObj("block_" + id);
  if(!block) return;
  var img = new getObj("img_" +id);
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


function toggleVisibility(id) 
{
  var obj = new getObj(id);
  return toggleVisibilityOnObject(obj,'block');
}

function toggleVisibilityOnObject(obj, display) 
{

  if(!obj) return 0;
  if(obj.style.visibility == "hidden") {
      obj.style.visibility = "visible";
      obj.style.display = display;
      return 1
   } else {
      obj.style.visibility = "hidden";
      obj.style.display = "none";
      return 0
   }
}

