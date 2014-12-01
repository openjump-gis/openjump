package jumptest.junit;

import java.awt.geom.*;
import java.util.ArrayList;

import junit.framework.TestCase;

import com.vividsolutions.jump.workbench.ui.renderer.java2D.ShapeCollectionPathIterator;

public class ShapeCollectionPathIteratorTestCase extends TestCase
{

  public ShapeCollectionPathIteratorTestCase(String Name_)
  {
    super(Name_);
  }
  public static void main(String[] args)
  {
    String[] testCaseName = {ShapeCollectionPathIteratorTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }
  public void testPathIterator() {
    PathIterator i = new Rectangle2D.Double(0, 0, 5, 5).getPathIterator(new AffineTransform());
    double d[] = new double[] {0, 0, 0, 0, 0, 0};

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_MOVETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(5, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(5, d[0], 1E-10);
    assertEquals(5, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(5, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_CLOSE, i.currentSegment(d));
    i.next();

    assertEquals(true, i.isDone());
  }

  public void testEmpty() {
    ArrayList shapes = new ArrayList();
    ShapeCollectionPathIterator i = new ShapeCollectionPathIterator(shapes, new AffineTransform());
    assertEquals(true, i.isDone());
  }

  public void test() {
    ArrayList shapes = new ArrayList();
    shapes.add(new Rectangle2D.Double(0, 0, 5, 5));
    shapes.add(new Rectangle2D.Double(10, 10, 5, 5));
    ShapeCollectionPathIterator i = new ShapeCollectionPathIterator(shapes, new AffineTransform());
    double d[] = new double[] {0, 0, 0, 0, 0, 0};

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_MOVETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(5, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(5, d[0], 1E-10);
    assertEquals(5, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(5, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(0, d[0], 1E-10);
    assertEquals(0, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_CLOSE, i.currentSegment(d));
    i.next();

    //next rectangle

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_MOVETO, i.currentSegment(d));
    assertEquals(10, d[0], 1E-10);
    assertEquals(10, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(15, d[0], 1E-10);
    assertEquals(10, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(15, d[0], 1E-10);
    assertEquals(15, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(10, d[0], 1E-10);
    assertEquals(15, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_LINETO, i.currentSegment(d));
    assertEquals(10, d[0], 1E-10);
    assertEquals(10, d[1], 1E-10);
    i.next();

    assertEquals(false, i.isDone());
    assertEquals(PathIterator.SEG_CLOSE, i.currentSegment(d));
    i.next();

    assertEquals(true, i.isDone());
  }
}
