Release 1.1.1
-------------
Using new 3.8.11.2 SQLite JDBC driver.  Added a default locale properties
file to avoid NPE when locale is not defined.

Release 1.1.0
-------------
Using new 3.8.6 SQLite JDBC driver

Release 1.0.0
-------------
Package now contains all supported JDBC drivers, with the exception of Oracle.
Now includes support for Geopackage geometries using the spatialite/sqlite 
driver.

Release 0.9.0
-------------

Plugin location moved from tools menu to File menu.
Added internationalization for Finnish, French, German, Italian, and Spanish
languages.


Release 0.8.2
-------------

Various packaging changes to fit better with OpenJump Plus

  * Switched licensing from LGP to GPLV3.
  * Plugin now centers in Workbench window
  * Added title to plugin frame

Release 0.8.1
-------------

Added ability to load Spatialite extensions by appending 
location of Spatialite DLL/SO to JDBC URL.

Release 0.8.0
-------------

Several nice enhancements from Nicolas Ribot:

  * Query dialog doesn't disappear after you submit query
  * Status bar
  * Syntax highlighting for SQL.
  * Ability to cancel in-progress query (PostGIS only)
  * Query history.  Double click query in history area to move it to the 
    query area.
  * Refresh layer - refreshes data in currently select query without creating 
    a new layer.


Bugs fixed:

  
  * Updated MySQL geometry loading to take advantage of patch for bug
    http://bugs.mysql.com/bug.php?id=34194.  

  * Made error message more user-friendly when PostGIS driver was included.
