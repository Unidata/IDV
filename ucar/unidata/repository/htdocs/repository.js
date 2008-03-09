

function getObj(name)	{
	  if (document.getElementById)	{    	// DOM level 1 browsers: IE 5+, NN 6+
	  	this.obj = document.getElementById(name);
		this.style = document.getElementById(name).style;
	  }
	  else if (document.all)	{  			// IE 4
		this.obj = document.all[name];
		this.style = document.all[name].style;
	  }
	  else if (document.layers)  { 			// NN 4
	   	this.obj = document.layers[name];
	   	this.style = document.layers[name];
	  }
}



function noop()  
{
}

function tooltipHide(event,id)  
{
  var obj = new getObj("tooltipdiv");
  if(!obj) return;
  obj.style.visibility = "hidden";
  obj.style.display = "none";
}


var lastMove = 0;
function tooltipShow(event,id) 
{
    return;
    lastMove++;
    setTimeout("tooltipNowShow(" + lastMove+"," +event.clientX+","+ event.clientY +"," + "'" + id +"'"+")", 1000);
}


function handleMouseMove() {
//   lastMove++;
}

//document.addEventListener("mousemove",handleMouseMove,false); 


function tooltipNowShow(moveId,x,y,id) 
{
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
          block.obj.innerHTML = getChildText(xmlDoc);
       }
      };
      request.open("GET", url);
      request.send(null);
//  alert("tooltip:" + event.clientX);
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


function folderClickHandler() {
 alert('callback ' + this.readyState +' ' + this.status);
 if(this.readyState == 4 && this.status == 200) {
  if(this.responseXML != null && this.responseXML.getElementById('test').firstChild.data)
    alert(this.responseXML.getElementById('test').firstChild.data);
  else
     alert("none");
 } else if (this.readyState == 4 && this.status != 200) {
     alert("error");
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
  if(!obj) return 0;
  if(obj.style.visibility == "hidden") {
      obj.style.visibility = "visible";
      obj.style.display = "block";
	return 1
   } else {
      obj.style.visibility = "hidden";
      obj.style.display = "none";
      return 0
   }
}

