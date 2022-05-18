package org.openjump.core.rasterimage;

public class BasicStatistics {
  private final int number;
  private final double min;
  private final double max;
  private final double mean;
  private final double stdDev;

  public BasicStatistics(int number, double min, double max, double mean, double stdDev) {
    this.number = number;
    this.min = min;
    this.max = max;
    this.mean = mean;
    this.stdDev = stdDev;
  }

  public int getNumber() {
    return number;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public double getMean() {
    return mean;
  }

  public double getStdDev() {
    return stdDev;
  }
}
