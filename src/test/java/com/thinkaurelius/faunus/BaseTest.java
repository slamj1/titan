package com.thinkaurelius.faunus;

import com.thinkaurelius.faunus.formats.graphson.GraphSONUtility;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import junit.framework.TestCase;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.apache.hadoop.mrunit.types.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseTest extends TestCase {

    public static enum ExampleGraph {GRAPH_OF_THE_GODS, TINKERGRAPH}

    public void testTrue() {
        assertTrue(true);
    }

    public static <T> List<T> asList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<T>();
        for (final T t : iterable) {
            list.add(t);
        }
        return list;
    }

    public static List<FaunusVertex> generateToyGraph(final ExampleGraph example) throws IOException {
        if (ExampleGraph.TINKERGRAPH.equals(example))
            return new GraphSONUtility().fromJSON(GraphSONUtility.class.getResourceAsStream("graph-example-1.json"));
        else
            return new GraphSONUtility().fromJSON(GraphSONUtility.class.getResourceAsStream("graph-of-the-gods.json"));
    }

    public static Map<Long, FaunusVertex> indexResults(final List<Pair<NullWritable, FaunusVertex>> pairs) {
        final Map<Long, FaunusVertex> map = new HashMap<Long, FaunusVertex>();
        for (final Pair<NullWritable, FaunusVertex> pair : pairs) {
            map.put(pair.getSecond().getIdAsLong(), pair.getSecond());
        }
        return map;
    }

    public static Map<Long, FaunusVertex> runWithToyGraph(final ExampleGraph example, final MapReduceDriver driver) throws IOException {
        driver.resetOutput();
        for (final FaunusVertex vertex : generateToyGraph(example)) {
            driver.withInput(NullWritable.get(), vertex);
        }
        return indexResults(driver.run());
    }

    public static Map<Long, FaunusVertex> runWithGraph(final Graph graph, final MapReduceDriver driver) throws IOException {
        driver.resetOutput();
        for (final Vertex vertex : graph.getVertices()) {
            final FaunusVertex temp = new FaunusVertex(Long.valueOf(vertex.getId().toString()));
            for (final Edge edge : vertex.getEdges(Direction.OUT)) {
                temp.addEdge(Direction.OUT, new FaunusEdge(temp, new FaunusVertex(Long.valueOf(edge.getVertex(Direction.IN).getId().toString())), edge.getLabel()));
            }
            for (final Edge edge : vertex.getEdges(Direction.IN)) {
                temp.addEdge(Direction.IN, new FaunusEdge(new FaunusVertex(Long.valueOf(edge.getVertex(Direction.OUT).getId().toString())), temp, edge.getLabel()));
            }
            for (final String key : vertex.getPropertyKeys()) {
                temp.setProperty(key, vertex.getProperty(key));
            }
            driver.withInput(NullWritable.get(), temp);
        }
        return indexResults(driver.run());
    }

    public static List runWithToyGraphNoFormatting(final ExampleGraph example, final MapReduceDriver driver) throws IOException {
        driver.resetOutput();
        for (final FaunusVertex vertex : generateToyGraph(example)) {
            driver.withInput(NullWritable.get(), vertex);
        }
        return driver.run();
    }

    /*public void testConverter() throws IOException {
        //Graph graph = new TinkerGraph();
        //GraphMLReader.inputGraph(graph, JSONUtility.class.getResourceAsStream("graph-of-the-gods.xml"));
        Graph graph = TinkerGraphFactory.createTinkerGraph();
        BufferedWriter bw = new BufferedWriter(new FileWriter("target/graph-example-1.json"));
        for (final Vertex vertex : graph.getVertices()) {
            bw.write(JSONUtility.toJSON(vertex).toString() + "\n");
        }
        bw.close();
    }*/

    /*public void testClosure() throws Exception {
        GroovyScriptEngineImpl engine = new GroovyScriptEngineImpl();
        Closure closure = (Closure) engine.eval("{-> 1+2}");
        closure = closure.dehydrate();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(closure);
        out.flush();
        out.close();

        //Configuration conf = new Configuration();
        //conf.set("method", bos.toString());
        BASE64Encoder encoder = new BASE64Encoder();
        String temp = encoder.encode(bos.toByteArray());
        System.out.println(temp);

        ByteArrayInputStream bis = new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(temp));
        ObjectInputStream in = new ObjectInputStream(bis);
        Closure closure1 = (Closure) in.readObject();
        in.close();
        
    }*/
}
