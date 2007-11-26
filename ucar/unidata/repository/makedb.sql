
-- CREATE TABLE nids (file char(200), 
-- 		   INDEX(file), 
--                    date datetime, 
-- 	           INDEX(date),  
--                 station char(50), 
--                    INDEX(station), 
--                    product char(50),
--                    INDEX(product));





CREATE TABLE collections (id varchar(200),
			parent varchar(200),
			name varchar(200),
			description varchar(200));



CREATE TABLE nids (collection varchar(200),
	           file varchar(200),
	           date timestamp, 
                   station varchar(50), 
                   product varchar(50));

CREATE INDEX COLLECTIONINDEX ON nids (COLLECTION);

CREATE INDEX STATIONINDEX ON nids (STATION);

CREATE INDEX PRODUCTINDEX ON nids (PRODUCT);

