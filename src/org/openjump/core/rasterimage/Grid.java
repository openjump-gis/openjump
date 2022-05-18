package org.openjump.core.rasterimage;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.awt.*;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Objects;

abstract public class Grid {

  static public class Header {

    final int colNumber;
    final int rowNumber;
    final Coordinate llCorner;
    final Resolution resolution;
    final double noDataValue;

    public Header(int colNumber, int rowNumber, Coordinate llCorner, Resolution resolution, double noDataValue) {
      this.colNumber = colNumber;
      this.rowNumber = rowNumber;
      this.llCorner = llCorner;
      this.resolution = resolution;
      this.noDataValue = noDataValue;
    }

    public Envelope getEnvelope() {
      return new Envelope(
          llCorner.x, llCorner.x + colNumber * resolution.getX(),
          llCorner.y, llCorner.y + rowNumber * resolution.getY()
      );
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Header)) return false;
      Header header = (Header) o;
      return colNumber == header.colNumber && rowNumber == header.rowNumber &&
          Double.compare(header.noDataValue, noDataValue) == 0 &&
          Objects.equals(llCorner, header.llCorner) &&
          Objects.equals(resolution, header.resolution);
    }

    @Override
    public int hashCode() {
      return Objects.hash(colNumber, rowNumber, llCorner, resolution);
    }
  }

  private final String fileName;
  private Header header;
  protected Raster raster;
  protected BasicStatistics statistics;

  public Grid(String fileName) {
    this.fileName = fileName;
  }

  public Grid(String fileName, Header header) {
    this.fileName = fileName;
    this.header = header;
  }

  public Grid(String fileName, Grid grid) {
    this.fileName = fileName;
    this.header = grid.header;
  }

  public String getFileName() {
    return fileName;
  }

  protected Header getHeader() throws IOException {
    if (header == null) {
      readHeader();
    }
    return header;
  }

  void setHeader(Header header) {
    this.header = header;
  }

  public int getColNumber() throws IOException {
    return getHeader().colNumber;
  }

  public int getRowNumber() throws IOException {
    return getHeader().rowNumber;
  }

  public Coordinate getLlCorner() throws IOException {
    return getHeader().llCorner;
  }

  public Resolution getResolution() throws IOException {
    return getHeader().resolution;
  }

  public double getNoDataValue() throws IOException {
    return getHeader().noDataValue;
  }

  public Raster getRaster() throws IOException {
    return raster;
  }

  public BasicStatistics getStatistics() throws IOException {
    return statistics;
  }

  public Envelope getEnvelope() {
    return header.getEnvelope();
  }

  abstract protected void readHeader() throws IOException;

  abstract protected void readGrid(Rectangle subset) throws IOException;

}
