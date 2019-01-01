package jumptest.junit;

import java.util.*;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.warp.*;
import com.vividsolutions.jump.warp.Triangle;

public class TriangulatorTestCase extends TestCase {

    private TestTriangulator triangulator = new TestTriangulator();
    private GeometryFactory factory = new GeometryFactory();

    public TriangulatorTestCase(String Name_) {
        super(Name_);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TriangulatorTestCase.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testContains2() {
        //Demonstrates that Saalfeld's point-in-triangle test is not robust. [Jon Aquino]
        Triangle t =
            new Triangle(
                new Coordinate(1684837.632040163, 501388.22581428883),
                new Coordinate(1700270.3028528732, 501388.22581428883),
                new Coordinate(1694394.5629020752, 500206.7497179573));      
        assertTrue(t.contains(new Coordinate(1694394.5629020752, 500206.74971795734)));
    }

    public void testTag() {
        Coordinate[] sourceQuad =
            new Coordinate[] {
                new Coordinate(1, 2),
                new Coordinate(3, 4),
                new Coordinate(5, 6),
                new Coordinate(7, 8)};
        Coordinate[] destQuad =
            new Coordinate[] {
                new Coordinate(-1, -2),
                new Coordinate(-3, -4),
                new Coordinate(-5, -6),
                new Coordinate(-7, -8)};
        TaggedCoordinate[] t = triangulator.tag(sourceQuad, destQuad);
        assertEquals(new Coordinate(3, 4), t[1]);
        assertEquals(new Coordinate(-3, -4), t[1].getTag());
    }

    public void testAddVector() {
        assertEquals(
            new Coordinate(20, 35),
            triangulator.add(
                new Coordinate(10, 20),
                factory.createLineString(
                    new Coordinate[] { new Coordinate(100, 100), new Coordinate(110, 115)})));
    }

    public void testVectorWithNearestTail1() {
        LineString v1 =
            factory.createLineString(
                new Coordinate[] { new Coordinate(200, 200), new Coordinate(15, 15)});
        LineString v2 =
            factory.createLineString(
                new Coordinate[] { new Coordinate(0, -20), new Coordinate(0, -10)});
        List vectors = Arrays.asList(new LineString[] { v1, v2 });
        Coordinate x = new Coordinate(10, 10);
        assertEquals(v2, triangulator.vectorWithNearestTail(x, vectors));
    }

    private GeometryFactory geometryFactory = new GeometryFactory();

    public void testVectorWithNearestTail2() {
        //flip v1
        LineString v1 =
            geometryFactory.createLineString(
                new Coordinate[] { new Coordinate(15, 15), new Coordinate(200, 200)});
        LineString v2 =
            geometryFactory.createLineString(
                new Coordinate[] { new Coordinate(0, -20), new Coordinate(0, -10)});
        List vectors = Arrays.asList(new LineString[] { v1, v2 });
        Coordinate x = new Coordinate(10, 10);
        assertEquals(v1, triangulator.vectorWithNearestTail(x, vectors));
    }

    public void testTriangulateQuadrilateral() {
        Coordinate[] quad =
            new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(10, 0),
                new Coordinate(10, 10),
                new Coordinate(0, 10)};
        List triangles = triangulator.triangles(quad);
        assertEquals(2, triangles.size());
        assertTrue(((Triangle) triangles.get(0)).equals(new TestTriangle(0, 0, 10, 0, 10, 10)));
        assertTrue(((Triangle) triangles.get(1)).equals(new TestTriangle(0, 0, 0, 10, 10, 10)));
    }

    public void testHeightMaximizedTriangles1() {
        Triangle t1 = new TestTriangle(0, 0, 20, 0, 15, 15);
        Triangle t2 = new TestTriangle(0, 0, 0, 20, 15, 15);
        Triangle a1 = new TestTriangle(0, 0, 20, 0, 0, 20);
        Triangle a2 = new TestTriangle(15, 15, 20, 0, 0, 20);
        List heightMaximizedTriangles = triangulator.heightMaximizedTriangles(t1, t2);
        Triangle x1 = (Triangle) heightMaximizedTriangles.get(0);
        Triangle x2 = (Triangle) heightMaximizedTriangles.get(1);
        assertTrue((t1.equals(x1) && t2.equals(x2)) || (t1.equals(x2) && t2.equals(x1)));
        assertTrue(!t1.equals(t2));
        assertTrue(!x1.equals(x2));
        assertTrue(!t1.equals(a1));
        assertTrue(!t1.equals(a2));
        assertTrue(!t2.equals(a1));
        assertTrue(!t2.equals(a2));
    }

    public void testMin() {
        TestTriangle t = new TestTriangle(0, 1, 2, 3, 4, 5);
        assertEquals(new Coordinate(0, 0), t.min(new Coordinate(0, 0), new Coordinate(1, 1)));
        assertEquals(new Coordinate(0, 1), t.min(new Coordinate(0, 1), new Coordinate(1, 0)));
        assertEquals(new Coordinate(0, 1), t.min(new Coordinate(1, 0), new Coordinate(0, 1)));
        assertEquals(new Coordinate(0, 0), t.min(new Coordinate(1, 1), new Coordinate(0, 0)));
    }

    public void testAlternativeTriangles() {
        Triangle t1 = new TestTriangle(0, 0, 20, 0, 10, 10);
        Triangle t2 = new TestTriangle(0, 0, 20, 0, 10, 11);
        //quad not convex [Jon Aquino]
        assertTrue(null == triangulator.alternativeTriangles(t1, t2));
    }

    public void testHeightMaximizedTriangles2() {
        Triangle t1 = new TestTriangle(0, 0, 20, 0, 15, 15);
        Triangle t2 = new TestTriangle(0, 0, 0, 20, 15, 15);
        Triangle a1 = new TestTriangle(0, 0, 20, 0, 0, 20);
        Triangle a2 = new TestTriangle(15, 15, 20, 0, 0, 20);
        List heightMaximizedTriangles = triangulator.heightMaximizedTriangles(a1, a2);
        Triangle x1 = (Triangle) heightMaximizedTriangles.get(0);
        Triangle x2 = (Triangle) heightMaximizedTriangles.get(1);
        assertTrue((t1.equals(x1) && t2.equals(x2)) || (t1.equals(x2) && t2.equals(x1)));
        assertTrue(!t1.equals(t2));
        assertTrue(!x1.equals(x2));
        assertTrue(!x1.equals(a1));
        assertTrue(!x1.equals(a2));
        assertTrue(!x2.equals(a1));
        assertTrue(!x2.equals(a2));
    }

    public void testTriangleContaining() {
        Triangle triangle1 = new TestTriangle(0, 0, 10, 0, 10, 10);
        Triangle triangle2 = new TestTriangle(0, 0, 0, 10, 10, 10);
        ArrayList triangles = new ArrayList();
        triangles.add(triangle1);
        triangles.add(triangle2);
        assertTrue(
            triangle1.equals(triangulator.triangleContaining(new Coordinate(5, 4), triangles)));
        assertTrue(
            !triangle2.equals(triangulator.triangleContaining(new Coordinate(5, 4), triangles)));
        assertTrue(
            !triangle1.equals(triangulator.triangleContaining(new Coordinate(5, 6), triangles)));
        assertTrue(
            triangle2.equals(triangulator.triangleContaining(new Coordinate(5, 6), triangles)));
        assertTrue(null == triangulator.triangleContaining(new Coordinate(20, 20), triangles));
        assertTrue(!triangle1.contains(new Coordinate(0, 5)));
        assertTrue(triangle1.contains(new Coordinate(5, 5)));
        assertTrue(triangle1.contains(new Coordinate(10, 5)));
        assertTrue(triangle2.contains(new Coordinate(0, 5)));
        assertTrue(triangle2.contains(new Coordinate(5, 5)));
        assertTrue(!triangle2.contains(new Coordinate(10, 5)));
    }

    public void testHasVertex() {
        Triangle t = new TestTriangle(0, 0, 10, 0, 10, 10);
        assertTrue(t.hasVertex(new Coordinate(0, 0)));
        assertTrue(t.hasVertex(new Coordinate(10, 0)));
        assertTrue(t.hasVertex(new Coordinate(10, 10)));
        assertTrue(!t.hasVertex(new Coordinate(10, 1)));
        assertTrue(!t.hasVertex(new Coordinate(0, 10)));
    }

    public void testGetArea() {
        assertEquals(500, (new TestTriangle(0, 0, 100, 0, 0, 10)).getArea(), 1E-10);
        assertEquals(2500, (new TestTriangle(0, 0, 5, 0, 0, 1000)).getArea(), 1E-10);
    }

    public void testGetMinHeight() {
        assertEquals(
            9.9503719020998913566527375373857,
            (new TestTriangle(0, 0, 100, 0, 0, 10)).getMinHeight(),
            1E-10);
    }

    public void testToSimplicialCoordinate() {
        Triangle t = new TestTriangle(0, 1, 10, 12, 23, 20);
        Coordinate e = new Coordinate(5, 4);
        Coordinate s = t.toSimplicialCoordinate(e);
        Coordinate e2 = t.toEuclideanCoordinate(s);
        assertEquals(e.x, e2.x, 1E-13);
        assertEquals(e.y, e2.y, 1E-13);
    }

    private class TestTriangulator extends Triangulator {
        public TaggedCoordinate[] tag(Coordinate[] sourceQuad, Coordinate[] destQuad) {
            Quadrilateral taggedSourceQuad =
                super.tag(
                    new Quadrilateral(sourceQuad[0], sourceQuad[1], sourceQuad[2], sourceQuad[3]),
                    new Quadrilateral(destQuad[0], destQuad[1], destQuad[2], destQuad[3]));
            return new TaggedCoordinate[] {
                (TaggedCoordinate) taggedSourceQuad.getP1(),
                (TaggedCoordinate) taggedSourceQuad.getP2(),
                (TaggedCoordinate) taggedSourceQuad.getP3(),
                (TaggedCoordinate) taggedSourceQuad.getP4()};
        }

        public List triangles(Coordinate[] quad) {
            return (new Quadrilateral(quad[0], quad[1], quad[2], quad[3])).triangles();
        }

        public List alternativeTriangles(Triangle PQS, Triangle QRS) {
            return super.alternativeTriangles(PQS, QRS);
        }

        public Triangle triangleContaining(Coordinate vertex, List triangles) {
            return super.triangleContaining(vertex, triangles);
        }

        public List heightMaximizedTriangles(Triangle PQS, Triangle QRS) {
            return super.heightMaximizedTriangles(PQS, QRS);
        }

        public LineString vectorWithNearestTail(Coordinate x, List vectors) {
            return super.vectorWithNearestTail(x, vectors);
        }

        public Coordinate add(Coordinate a, LineString vector) {
            return super.add(a, vector);
        }
    }

    private class TestTriangle extends Triangle {
        public TestTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
            super(new Coordinate(x1, y1), new Coordinate(x2, y2), new Coordinate(x3, y3));
        }

        protected Coordinate min(Coordinate a, Coordinate b) {
            return super.min(a, b);
        }
    }

    public void testContains() {
        Coordinate c = new Coordinate(1194845.037570758, 381015.872414322);
        Triangle t =
            new Triangle(
                new Coordinate(c),
                new Coordinate(1194963.831717294, 381197.705245446),
                new Coordinate(1194991.149065747, 381311.756990355));
        assertTrue(t.contains(c));
    }

    public void testContainsBasic() {
        doTestContainsBasic(
            new Triangle(new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(0, 10)));
        doTestContainsBasic(
            new Triangle(new Coordinate(0, 0), new Coordinate(0, 10), new Coordinate(10, 0)));
    }

    private void doTestContainsBasic(Triangle triangle) {
        assertTrue(triangle.contains(new Coordinate(1, 1)));
        assertTrue(triangle.contains(new Coordinate(0, 0)));
        assertTrue(triangle.contains(new Coordinate(5, 5)));
        assertTrue(!triangle.contains(new Coordinate(1, -1)));
        assertTrue(!triangle.contains(new Coordinate(-1, 1)));
        assertTrue(!triangle.contains(new Coordinate(5.1, 5.1)));
    }

}
