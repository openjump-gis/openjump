package org.openjump.core.ui.plugin.tools.aggregate;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.feature.AttributeType;

import java.util.*;

/**
 * Class containing all kinds of aggregator implementation as well as maps
 * containing instances of aggregators.
 */
public class Aggregators {

    private final static Map<AttributeType,Map<String,Aggregator>> aggregatorsByType =
            new HashMap<AttributeType,Map<String,Aggregator>>();
    private final static Map<String,Aggregator> aggregatorsByName =
            new HashMap<String,Aggregator>();

    /**
     * Returns an aggregator from its (internationalized) name
     */
    public static Aggregator getAggregator(String name) {
        return aggregatorsByName.get(name);
    }

    /**
     * Returns aggregators accepting a certain type of values as input.
     */
    public static Map<String,Aggregator> getAggregators(AttributeType type) {
        return aggregatorsByType.get(type);
    }

    private static void addAggregator(AttributeType inputType, Aggregator aggregator) {
        aggregatorsByName.put(aggregator.getName(), aggregator);
        Map<String,Aggregator> map = aggregatorsByType.get(inputType);
        if (map == null) {
            map = new HashMap<>();
            aggregatorsByType.put(inputType, map);
        }
        map.put(aggregator.getName(), aggregator);
    }

    static {
        addAggregator(AttributeType.STRING,   new Count(false));
        addAggregator(AttributeType.BOOLEAN,  new Count(false));
        addAggregator(AttributeType.INTEGER,  new Count(false));
        addAggregator(AttributeType.LONG,     new Count(false));
        addAggregator(AttributeType.DOUBLE,   new Count(false));
        addAggregator(AttributeType.DATE,     new Count(false));
        addAggregator(AttributeType.GEOMETRY, new Count(false));
        addAggregator(AttributeType.OBJECT,   new Count(false));

        addAggregator(AttributeType.GEOMETRY, new Collect());
        addAggregator(AttributeType.GEOMETRY, new Union());

        addAggregator(AttributeType.STRING,   new Concatenate(false));
        addAggregator(AttributeType.STRING,   new ConcatenateUnique(false));
        addAggregator(AttributeType.INTEGER,  new IntSum());
        addAggregator(AttributeType.INTEGER,  new LongSum());
        addAggregator(AttributeType.LONG,     new LongSum());
        addAggregator(AttributeType.DOUBLE,   new LongSum());
        addAggregator(AttributeType.INTEGER,  new DoubleSum());
        addAggregator(AttributeType.LONG,     new DoubleSum());
        addAggregator(AttributeType.DOUBLE,   new DoubleSum());

        addAggregator(AttributeType.INTEGER,  new IntMin());
        addAggregator(AttributeType.LONG,     new LongMin());
        addAggregator(AttributeType.DOUBLE,   new DoubleMin());
        addAggregator(AttributeType.STRING,   new StringMin());
        addAggregator(AttributeType.DATE,     new DateMin());

        addAggregator(AttributeType.INTEGER,  new IntMax());
        addAggregator(AttributeType.LONG,     new LongMax());
        addAggregator(AttributeType.DOUBLE,   new DoubleMax());
        addAggregator(AttributeType.STRING,   new StringMax());
        addAggregator(AttributeType.DATE,     new DateMax());

        addAggregator(AttributeType.INTEGER,  new IntMean(true));
        addAggregator(AttributeType.LONG,     new LongMean(true));
        addAggregator(AttributeType.DOUBLE,   new DoubleMean(true));
        addAggregator(AttributeType.DATE,     new DateMean(true));

        addAggregator(AttributeType.INTEGER,  new IntMedian());
        addAggregator(AttributeType.LONG,     new LongMedian());
        addAggregator(AttributeType.DOUBLE,   new DoubleMedian());
        addAggregator(AttributeType.DATE,     new DateMedian());

        addAggregator(AttributeType.STRING,   new StringMajority(true));
    }


    public static class Count extends AbstractAggregator<Object> {
        public Count(boolean ignoreNull) {
            super(AttributeType.INTEGER, ignoreNull);
        }

        @Override public Count clone() {
            return new Count(ignoreNull());
        }

        @Override public Integer getResult() {
            return getValues().size();
        }
    }

    public static class Concatenate extends AbstractAggregator<Object> {
        public Concatenate(boolean ignoreNull) {
            super(AttributeType.STRING, ignoreNull);
            setParameter(Aggregator.SEPARATOR_NAME, ",");
        }
        @Override public Concatenate clone() {
            Concatenate concatenate = new Concatenate(ignoreNull());
            concatenate.setParameter(Aggregator.SEPARATOR_NAME, this.getParameter(Aggregator.SEPARATOR_NAME));
            return concatenate;
        }
        @Override public String getResult() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0 ; i < getValues().size() ; i++) {
                String v = getValues().get(i) == null ? "<NULL>" : getValues().get(i).toString();
                if (i == 0) sb.append(v);
                else sb.append(getParameter(Aggregator.SEPARATOR_NAME)).append(v);
            }
            return sb.toString();
        }
    }

    public static class ConcatenateUnique extends AbstractAggregator<Object> {
        public ConcatenateUnique(boolean ignoreNull) {
            super(AttributeType.STRING, ignoreNull);
            setParameter(Aggregator.SEPARATOR_NAME, ",");
        }
        @Override public ConcatenateUnique clone() {
            ConcatenateUnique concatenate = new ConcatenateUnique(ignoreNull());
            concatenate.setParameter(Aggregator.SEPARATOR_NAME, this.getParameter(Aggregator.SEPARATOR_NAME));
            return concatenate;
        }
        @Override public String getResult() {
            StringBuilder sb = new StringBuilder();
            List<Object> values = getValues();
            for (int i = 0 ; i < values.size() ; i++) {
                if (values.get(i) == null) values.set(i, "<NULL>");
            }
            Set set = new TreeSet<>(values);
            for (Object value : set) {
                if (sb.length()==0) sb.append(value);
                else sb.append(getParameter(SEPARATOR_NAME)).append(value);
            }
            return sb.toString();
        }
    }

    public static class Collect extends AbstractAggregator<Geometry> {
        public Collect() {
            super(AttributeType.GEOMETRY, true);
        }
        @Override public Collect clone() {
            return new Collect();
        }
        @Override public Geometry getResult() {
            return new GeometryFactory().buildGeometry(getValues());
        }
    }

    public static class Union extends AbstractAggregator<Geometry> {
        public Union() {
            super(AttributeType.GEOMETRY, true);
        }
        @Override public Union clone() {
            return new Union();
        }
        @Override public Geometry getResult() {
            GeometryFactory gf = getValues().size() == 0 ? new GeometryFactory() : getValues().get(0).getFactory();
            Geometry collected = gf.buildGeometry(getValues());
            if (getValues().size() == 0) return collected;
            List<Geometry> points      = new ArrayList<Geometry>();
            List<Geometry> lineStrings = new ArrayList<Geometry>();
            List<Geometry> polygons    = new ArrayList<Geometry>();
            decompose(collected, points, lineStrings, polygons);
            LineMerger merger = new LineMerger();
            merger.add(lineStrings);
            List<Geometry> geometries = new ArrayList<Geometry>();
            geometries.addAll(points);
            geometries.addAll(merger.getMergedLineStrings());
            Geometry mpoly = UnaryUnionOp.union(polygons);
            if (mpoly != null) {
                for (int i = 0; i < mpoly.getNumGeometries(); i++) {
                    geometries.add(mpoly.getGeometryN(i));
                }
            }
            return gf.buildGeometry(geometries);
        }
        private void decompose(Geometry geometry, List<Geometry> dim0, List<Geometry> dim1, List<Geometry> dim2) {
            if (geometry instanceof GeometryCollection) {
                for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
                    decompose(geometry.getGeometryN(i), dim0, dim1, dim2);
                }
            }
            else if (geometry.getDimension() == 2) dim2.add(geometry);
            else if (geometry.getDimension() == 1) dim1.add(geometry);
            else if (geometry.getDimension() == 0) dim0.add(geometry);
            else {
                assert false : "Should never reach here";
            }
        }
    }

    public static class IntSum extends AbstractAggregator<Integer> {
        public IntSum() {
            super(AttributeType.INTEGER, true);
        }
        @Override public IntSum clone() {
            return new IntSum();
        }
        @Override public Integer getResult() {
            int sum = 0;
            for (Integer value : getValues()) {
                if (value != null) {
                    sum += value;
                }
            }
            return sum;
        }
    }

    public static class LongSum extends AbstractAggregator<Number> {
        public LongSum() {
            super(AttributeType.LONG, true);
        }
        @Override public LongSum clone() {
            return new LongSum();
        }
        @Override public Long getResult() {
            long sum = 0;
            for (Number value : getValues()) {
                if (value != null) {
                    sum += value.longValue();
                }
            }
            return sum;
        }
    }

    public static class DoubleSum extends AbstractAggregator<Number> {
        public DoubleSum() {
            super(AttributeType.DOUBLE, true);
        }
        @Override public DoubleSum clone() {
            return new DoubleSum();
        }
        @Override public Double getResult() {
            double sum = 0;
            for (Number value : getValues()) {
                if (value != null) {
                    sum += value.doubleValue();
                }
            }
            return sum;
        }
    }

    public static class IntMin extends AbstractAggregator<Integer> {
        public IntMin() {
            super(AttributeType.INTEGER, true);
        }
        @Override public IntMin clone() {
            return new IntMin();
        }
        @Override public Integer getResult() {
            Integer min = null;
            for (Integer value : getValues()) {
                if (min == null || (value != null && value < min)) {
                    min = value;
                }
            }
            return min;
        }
    }

    public static class LongMin extends AbstractAggregator<Long> {
        public LongMin() {
            super(AttributeType.LONG, true);
        }
        @Override public LongMin clone() {
            return new LongMin();
        }
        @Override public Long getResult() {
            Long min = null;
            for (Long value : getValues()) {
                if (min == null || (value != null && value < min)) {
                    min = value;
                }
            }
            return min;
        }
    }

    public static class DoubleMin extends AbstractAggregator<Double> {
        public DoubleMin() {
            super(AttributeType.DOUBLE, true);
        }
        @Override public DoubleMin clone() {
            return new DoubleMin();
        }
        @Override public Double getResult() {
            Double min = null;
            for (Double value : getValues()) {
                if (min == null || (value != null && value < min)) {
                    min = value;
                }
            }
            return min;
        }
    }

    public static class StringMin extends AbstractAggregator<String> {
        public StringMin() {
            super(AttributeType.STRING, true);
        }
        @Override public StringMin clone() {
            return new StringMin();
        }
        @Override public String getResult() {
            String min = null;
            for (String value : getValues()) {
                if (min == null || (value != null && value.compareToIgnoreCase(min) < 0)) {
                    min = value;
                }
            }
            return min;
        }
    }

    public static class DateMin extends AbstractAggregator<Date> {
        public DateMin() {
            super(AttributeType.DATE, true);
        }
        @Override public DateMin clone() {
            return new DateMin();
        }
        @Override public Date getResult() {
            Date min = null;
            for (Date value : getValues()) {
                if (min == null || (value != null && value.before(min))) {
                    min = value;
                }
            }
            return min;
        }
    }

    public static class IntMax extends AbstractAggregator<Integer> {
        public IntMax() {
            super(AttributeType.INTEGER, true);
        }
        @Override public IntMax clone() {
            return new IntMax();
        }
        @Override public Integer getResult() {
            Integer max = null;
            for (Integer value : getValues()) {
                if (max == null || (value != null && value > max)) {
                    max = value;
                }
            }
            return max;
        }
    }

    public static class LongMax extends AbstractAggregator<Long> {
        public LongMax() {
            super(AttributeType.LONG, true);
        }
        @Override public LongMax clone() {
            return new LongMax();
        }
        @Override public Long getResult() {
            Long max = null;
            for (Long value : getValues()) {
                if (max == null || (value != null && value > max)) {
                    max = value;
                }
            }
            return max;
        }
    }

    public static class DoubleMax extends AbstractAggregator<Double> {
        public DoubleMax() {
            super(AttributeType.DOUBLE, true);
        }
        @Override public DoubleMax clone() {
            return new DoubleMax();
        }
        @Override public Double getResult() {
            Double max = null;
            for (Double value : getValues()) {
                if (max == null || (value != null && value > max)) {
                    max = value;
                }
            }
            return max;
        }
    }

    public static class StringMax extends AbstractAggregator<String> {
        public StringMax() {
            super(AttributeType.STRING, true);
        }
        @Override public StringMax clone() {
            return new StringMax();
        }
        @Override public String getResult() {
            String max = null;
            for (String value : getValues()) {
                if (max == null || (value != null && value.compareToIgnoreCase(max) > 0)) {
                    max = value;
                }
            }
            return max;
        }
    }

    public static class DateMax extends AbstractAggregator<Date> {
        public DateMax() {
            super(AttributeType.DATE, true);
        }
        @Override public DateMax clone() {
            return new DateMax();
        }
        @Override public Date getResult() {
            Date max = null;
            for (Date value : getValues()) {
                if (max == null || (value != null && value.after(max))) {
                    max = value;
                }
            }
            return max;
        }
    }

    public static class IntMean extends AbstractAggregator<Integer> {
        public IntMean(boolean ignoreNull) {
            super(AttributeType.INTEGER, ignoreNull);
        }
        @Override public IntMean clone() {
            return new IntMean(ignoreNull());
        }
        @Override public Integer getResult() {
            long sum = 0;
            for (Integer value : getValues()) {
                if (value != null) sum += value;
            }
            if (getValues().size()==0) return null;
            return (int)(sum/getValues().size());
        }
    }

    public static class LongMean extends AbstractAggregator<Long> {
        public LongMean(boolean ignoreNull) {
            super(AttributeType.LONG, ignoreNull);
        }
        @Override public LongMean clone() {
            return new LongMean(ignoreNull());
        }
        @Override public Long getResult() {
            long sum = 0;
            for (Long value : getValues()) {
                if (value != null) sum += value;
            }
            if (getValues().size()==0) return null;
            return sum/getValues().size();
        }
    }

    public static class DoubleMean extends AbstractAggregator<Double> {
        public DoubleMean(boolean ignoreNull) {
            super(AttributeType.DOUBLE, ignoreNull);
        }
        @Override public DoubleMean clone() {
            return new DoubleMean(ignoreNull());
        }
        @Override public Double getResult() {
            double sum = 0;
            for (Double value : getValues()) {
                if (value != null) sum += value;
            }
            if (getValues().size()==0) return null;
            return sum/getValues().size();
        }
    }


    public static class DateMean extends AbstractAggregator<Date> {
        public DateMean(boolean ignoreNull) {
            super(AttributeType.DATE, ignoreNull);
        }
        @Override public DateMean clone() {
            return new DateMean(ignoreNull());
        }
        @Override public Date getResult() {
            long sum = 0;
            for (Date value : getValues()) {
                if (value != null) sum += value.getTime();
            }
            if (getValues().size()==0) return null;
            return new Date(sum/getValues().size());
        }
    }

    public static class IntMedian extends AbstractAggregator<Integer> {
        public IntMedian() {
            super(AttributeType.INTEGER, true);
        }
        @Override public IntMedian clone() {
            return new IntMedian();
        }
        @Override public Integer getResult() {
            List<Integer> sortedList = new ArrayList<Integer>(getValues());
            if (sortedList.size() == 0) return null;
            Collections.sort(sortedList);
            return sortedList.get(sortedList.size()/2);
        }
    }

    public static class LongMedian extends AbstractAggregator<Long> {
        public LongMedian() {
            super(AttributeType.LONG, true);
        }
        @Override public LongMedian clone() {
            return new LongMedian();
        }
        @Override public Long getResult() {
            List<Long> sortedList = new ArrayList<Long>(getValues());
            if (sortedList.size() == 0) return null;
            Collections.sort(sortedList);
            return sortedList.get(sortedList.size()/2);
        }
    }

    public static class DoubleMedian extends AbstractAggregator<Double> {
        public DoubleMedian() {
            super(AttributeType.DOUBLE, true);
        }
        @Override public DoubleMedian clone() {
            return new DoubleMedian();
        }
        @Override public Double getResult() {
            List<Double> sortedList = new ArrayList<Double>(getValues());
            if (sortedList.size() == 0) return null;
            Collections.sort(sortedList);
            return sortedList.get(sortedList.size()/2);
        }
    }


    public static class DateMedian extends AbstractAggregator<Date> {
        public DateMedian() {
            super(AttributeType.DATE, true);
        }
        @Override public DateMedian clone() {
            return new DateMedian();
        }
        @Override public Date getResult() {
            List<Date> sortedList = new ArrayList<Date>(getValues());
            if (sortedList.size() == 0) return null;
            Collections.sort(sortedList);
            return sortedList.get(sortedList.size()/2);
        }
    }

    public static class StringMajority extends AbstractAggregator<String> {
        public StringMajority(boolean ignoreNull) {
            super(AttributeType.STRING, ignoreNull);
        }
        @Override public StringMajority clone() {
            return new StringMajority(ignoreNull());
        }
        @Override public String getResult() {
            Map<String,Integer> map = new HashMap<String, Integer>();
            for (Object value : getValues()) {
                String v = value==null?null:value.toString();
                Integer i = map.get(v);
                if (i == null) i = 0;
                i = i+1;
                map.put(v,i);
            }
            String maj = null;
            int majNumber = 0;
            for (Map.Entry<String,Integer> entry : map.entrySet()) {
                if (entry.getValue() > majNumber) {
                    majNumber = entry.getValue();
                    maj = entry.getKey();
                }
            }
            return maj;
        }
    }

}

