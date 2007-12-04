
--- Initial data base creation

CREATE TABLE  groups (id varchar(500),
                     parent varchar(200),
                     name varchar(200),
		     description varchar(200));


CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
		     admin int);


CREATE TABLE files (id varchar(200),
	           name varchar(200),
                   description varchar(500),
                   type varchar(200),
                   group_id varchar(200),
   		   user_id varchar(200),
	           file varchar(200),
	           createdate timestamp, 
	           fromdate timestamp, 
	           todate timestamp); 

CREATE INDEX FILES_INDEX_ID ON files (ID);
CREATE INDEX FILES_INDEX_GROUP ON files (GROUP_ID);
CREATE INDEX FILES_INDEX_TYPE ON files (TYPE);
CREATE INDEX FILES_INDEX_USER_ID ON files (TYPE);

CREATE TABLE level3radar (
	           id varchar(200),
                   station varchar(50), 
                   product varchar(50));

CREATE INDEX LEVEL3RADAR_INDEX_ID ON level3radar (ID);
CREATE INDEX LEVEL3RADAR_INDEX_STATION ON level3radar (STATION);
CREATE INDEX LEVEL3RADAR_INDEX_PRODUCT ON level3radar (PRODUCT);


CREATE TABLE level2radar (
	           id varchar(200),
                   station varchar(50));

CREATE INDEX LEVEL2RADAR_INDEX_ID ON level2radar (ID);
CREATE INDEX LEVEL2RADAR_INDEX_STATION ON level2radar (STATION);

CREATE TABLE typehandler (
	           type varchar(200),
                   class varchar(50),
		   description varchar(200));

CREATE TABLE tags (name varchar(200),
	           file_id varchar(200));

--insert into typehandler values ('any', 'ucar.unidata.repository.TypeHandler','Any Type');
--insert into typehandler values ('level3radar', 'ucar.unidata.repository.Level3RadarTypeHandler', 'Level 3 Radar');
--insert into typehandler values ('level2radar', 'ucar.unidata.repository.Level2RadarTypeHandler', 'Level 2 Radar');


