

function Util () {

    this.loadXML = function (url, callback,arg) {
        var req = false;
        if(window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch(e) {
                req = false;
            }
        } else if(window.ActiveXObject)  {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch(e) {
                    req = false;
                }
            }
        }
        if(req) {
            req.onreadystatechange = function () { 
                if (req.readyState == 4 && req.status == 200)   {
                    callback(req,arg); 
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    }



    this.setCursor = function(c) {
        var cursor = document.cursor;
        if(!cursor && document.getElementById) {
            cursor =  document.getElementById('cursor');
        }
        if(!cursor) {
            document.body.style.cursor = c;
        }
    }


    this.getDomObject = function(name) {
        obj = new DomObject(name);
        if(obj.obj) return obj;
        return null;
    }



    this.getKeyChar = function(event) {
        event = util.getEvent(event);
        if(event.keyCode) {
            return String.fromCharCode(event.keyCode);
        }
        if(event.which)  {
            return String.fromCharCode(event.which);
        }
        return '';
    }


    this.print = function (s, clear) {
        var obj = util.getDomObject("output");
        if(!obj) return;
        if(clear) {
            obj.obj.innerHTML  ="";
        }
        obj.obj.innerHTML  =obj.obj.innerHTML+"<br>" +s;
    }



    this.getEvent = function (event) {
        if(event) return event;
        return window.event;
    }


    this.getEventX =    function (event) {
        if (event.pageX) {
            return  event.pageX;
        }
        return  event.clientX + document.body.scrollLeft
        + document.documentElement.scrollLeft;
    }

    this.getEventY =function (event) {
        if (event.pageY) {
            return  event.pageY;
        }
        return  event.clientY + document.body.scrollTop
        + document.documentElement.scrollTop;

    }

    this.getTop = function (obj) {
        if(!obj) return 0;
        return obj.offsetTop+this.getTop(obj.offsetParent);
    }



    this.setPosition = function(obj,x,y) {
        obj.style.top = y;
        obj.style.left = x;
    }

    this.getLeft =  function(obj) {
        if(!obj) return 0;
        return obj.offsetLeft+this.getLeft(obj.offsetParent);
    }
    this.getRight =  function(obj) {
        if(!obj) return 0;
        return obj.offsetRight+this.getRight(obj.offsetParent);
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




function hideObject(obj) {
    if(!obj) return 0;
    obj.style.visibility = "hidden";
    obj.style.display = "none";
}


function hideMore(base) {
        var link = util.getDomObject("morelink_" + base);
        var div = util.getDomObject("morediv_" + base);
	hideObject(div);
	showObject(link);
}

function showMore(base) {
        var link = util.getDomObject("morelink_" + base);
        var div = util.getDomObject("morediv_" + base);
	hideObject(link);
	showObject(div);
}




function showObject(obj, display) {
    if(!obj) return 0;
    if(!display) display = "block";
    obj.style.visibility = "visible";
    obj.style.display = display;
    return 1;
}


