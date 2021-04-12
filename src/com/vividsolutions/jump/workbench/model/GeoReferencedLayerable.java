package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.util.Blackboard;
import org.locationtech.jts.geom.Envelope;
import org.openjump.core.ccordsys.utils.SRSInfo;


/**
 * Add a parent class to georeferenced layers having SRSInfo and Envelope
 * @since 2.0
 */
public abstract class GeoReferencedLayerable extends AbstractLayerable implements Disposable {

  private final Blackboard blackboard = new Blackboard();

  private Envelope envelope;

  private SRSInfo srsInfo;

  /**
   * Called by Java2XML
   */
  public GeoReferencedLayerable() {
  }

  public GeoReferencedLayerable(String name, LayerManager layerManager) {
    super(name, layerManager);
  }

  @Override
  public Blackboard getBlackboard() {
    return blackboard;
  }

  public Envelope getEnvelope() {
    return envelope;
  }

  public void setEnvelope(Envelope envelope) {
    this.envelope = envelope;
  }

  public SRSInfo getSrsInfo() {
    return srsInfo;
  }

  public void setSrsInfo(SRSInfo srsInfo) {
    this.srsInfo = srsInfo;
  }
}
