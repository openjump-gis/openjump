The JUMP DB Query Plugin gives OpenJump users a way to visualize geometric 
information in their spatially enabled databases by entering arbitrary SQL
queries.

License
-------
The JUMP DB Query plugin is licensed under the GNU General 
License, version 3.   See the file COPYING for more information.

Installation 
------------
Download the jumpdbquery package, unzip it, and place all the files in the
unzipped folder in the JUMP_HOME/lib/ext folder.

Install Database Drivers
------------------------
The OpenJump database query plugin currently supports four databases:
Oracle, MySQL, PostgreSQL, and Spatialite.  Some database functionality
requires installation of additional software.  See below for instructions.

SpatiaLite
----------
The plugin  by default supports SQLite and SpatiaLite geometries encoded 
by the Geopackage specification, FDO RFC 16, and native Spatialite.  
However, if you want to use Spatialite functions, you need to tell 
the DB Query Plugin to load the Spatialite drivers.  First, install the 
SpatiaLite drivers, including all dependencies as described in 
http://www.gaia-gis.it/spatialite-2.3.1/binaries.html. Then tell the Db Query
Plugin to load the SpatiaLite drivers by appending 
"?spatialite=/path/to/spatialite.dll" to the JDBC url.  

Oracle
------
If you want to query an Oracle database, download the Oracle JDBC drivers
from http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html.
You'll need an OTN account to download the drivers, but you can get one for free
from Oracle.  Read and accept the Oracle license, download ojdbc6.jar, and
place it in the JUMP_HOME/lib/ext folder.

MySQL and PostGreSQL
--------------------
MySQL and PostGreSQL drivers are included in the plugin and/or OpenJump itself.
No special steps are needed to query MySQL and PostGreSQL databases.


Update dbquery.properties
-------------------------
One of the files installed in JUMP_HOME/lib/ext is called dbquery.properties. 
You can use this config file to store database connection information so that 
you don't have to enter that information everytime you run a query.  Follow the 
example settings in dbquery.properties and add the JDBC connection information 
for any databases you want to query.

Instructions
------------
Start OpenJUMP, and go to Tools-->Database Query.  Select the database you want 
to query from the database dropdown.  Update any connection information, if 
necessary, and enter the database password.  Enter your SQL SELECT statement.  
The SELECT statement should include the geometry column, along with any other
columns you want.  The plugin will generate a featureset where the geometry 
column defines the feature geometry, and the other columns define the feature
attributes.   For example, if you have a table called WORLDMAP, with a geometry 
column G, a map color column called "COLOR", and a country name column called 
COUNTRY, you could run the following query to view all countries starting with 
"A":

SELECT G, COLOR, COUNTRY FROM WORLDMAP WHERE COUNTRY LIKE 'A%'

The plugin will use the G column to show country polygons, and COLOR and COUNTRY
will show up as attributes of the polygon features.  If you don't put a geometry
column in your query, or if some of the geometry values returned by the query 
are null, the plugin will automatically create a square polygon around the 
coordinate system origin for those records that don't have a geometry value. 


I hope you find this plugin useful.  If you have problems, contact me, Larry
Reeder, at lnreeder@gmail.com.

