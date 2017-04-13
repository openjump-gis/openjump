package com.vividsolutions.jump.io.geojson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.AbstractJUMPWriter;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.IllegalParametersException;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.util.FileUtil;

public class GeoJSONWriter extends AbstractJUMPWriter {

  @Override
  public void write(FeatureCollection featureCollection, DriverProperties dp)
      throws Exception {

    FileOutputStream fileStream = null;
    Writer w = null;
    try {
      GeoJSONFeatureCollectionWrapper fcw = new GeoJSONFeatureCollectionWrapper(
          featureCollection);

      String uriString = dp.getProperty(DataSource.URI_KEY);
      if (uriString == null) {
        throw new IllegalParametersException(
            "call to GeoJSONReader.write() has DataProperties w/o an Uri specified");
      }
      URI uri = new URI(uriString);

      fileStream = new FileOutputStream(new File(uri));
      w = new OutputStreamWriter(fileStream, GeoJSONConstants.CHARSET);

      fcw.writeJSONString(w,getTaskMonitor());
    } finally {
      FileUtil.close(w);
      FileUtil.close(fileStream);
    }
  }

}
