CREATE DATABASE TEST;

CREATE TABLE table_without_geometry(name varchar);
INSERT INTO table_without_geometry VALUES('toto');
INSERT INTO table_without_geometry VALUES('titi');
INSERT INTO table_without_geometry VALUES(null);

CREATE TABLE table_with_point_geometry(name varchar);
SELECT AddGeometryColumn('public', 'table_with_point_geometry', 'geom', 0, 'POINT', 2);



SELECT AddGeometryColumn('public', 'table_with_point_geometry', 'geom', 0, 'POINT', 2);
