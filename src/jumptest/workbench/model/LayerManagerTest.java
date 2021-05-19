package jumptest.workbench.model;

import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import org.junit.Test;
import org.locationtech.jts.util.Assert;

import java.awt.*;
import java.util.Iterator;

public class LayerManagerTest {

  static FeatureDataset dataset = new FeatureDataset(new FeatureSchema());

  static class MyLayer extends Layer {
    public MyLayer(String name, LayerManager manager) {super(name, Color.BLACK, dataset, manager);}
  }

  @Test
  public void layerIteratorTest() {
    LayerManager manager = new LayerManager();
    manager.addCategory("A");
    manager.addCategory("B");
    manager.addCategory("C");
    manager.addLayerable("A", new Layer("A1",Color.BLACK, dataset, manager));
    manager.addLayerable("A", new Layer("A2",Color.BLACK, dataset, manager));
    manager.addLayerable("B", new MyLayer("B3", manager));
    manager.addLayerable("B", new Layer("B4",Color.BLACK, dataset, manager));

    int count = 0;
    for (Iterator<Category> it = manager.iterator(Category.class); it.hasNext();) {
      it.next(); count++;
    }
    Assert.equals(3, count); // 3 categories

    count = 0;
    for (Iterator<Layer> it = manager.iterator(Layer.class); it.hasNext();) {
      it.next(); count++;
    }
    Assert.equals(4, count); // 4 Layers

    count = 0;
    for (Iterator<MyLayer> it = manager.iterator(MyLayer.class); it.hasNext();) {
      it.next(); count++;
    }
    Assert.equals(1, count); // 1 MyLayer
  }

}
