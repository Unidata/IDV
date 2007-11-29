
CREATE TABLE dummy (id varchar(200));


CREATE TABLE groups (id varchar(200),
	  		parent varchar(200),
			name varchar(200),
			description varchar(200));


CREATE TABLE files (id varchar(200),
	           name varchar(200),
                   description varchar(500),
                   type varchar(200),
                   group_id varchar(200),
	           file varchar(200),
	           fromdate timestamp, 
	           todate timestamp); 

CREATE INDEX FILES_IDINDEX ON files (ID);
CREATE INDEX FILES_GROUPINDEX ON files (GROUP_ID);
CREATE INDEX FILES_TYPEINDEX ON files (TYPE);

CREATE TABLE level3radar (
	           id varchar(200),
                   station varchar(50), 
                   product varchar(50));


CREATE TABLE level2radar (
	           id varchar(200),
                   station varchar(50));




CREATE INDEX LEVEL3RADAR_IDINDEX ON level3radar (ID);
CREATE INDEX LEVEL3RADAR_STATIONINDEX ON level3radar (STATION);
CREATE INDEX LEVEL3RADAR_PRODUCTINDEX ON level3radar (PRODUCT);

