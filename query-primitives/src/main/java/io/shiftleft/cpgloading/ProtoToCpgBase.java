package io.shiftleft.cpgloading;

import io.shiftleft.proto.cpg.Cpg;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ProtoToCpgBase {
  protected Graph graph;
  protected Logger logger = LogManager.getLogger(getClass());
  protected Map<Long, Object> keyToVertexId = new HashMap<>();

  // for debugging output
  protected int elementImportCounter = 0;
  protected long lastStart = System.currentTimeMillis();

  public ProtoToCpgBase(Graph graph) {
    this.graph = graph;
    configureGraph(graph);
  }

  public abstract void addNodes(Cpg.CpgStruct protoCpg);

  /* optionally configure graph in implementing class */
  protected void configureGraph(Graph graph) {
  }

  public void addEdges(List<Cpg.CpgStruct.Edge> protoEdges) {
    for (Cpg.CpgStruct.Edge protoEdge : protoEdges) {
      // if (elementImportCounter % 1000 == 0) {
      //   long millisSinceLastBatch = System.currentTimeMillis() - lastStart;
      //   lastStart = System.currentTimeMillis();
      //   System.out.println("importing edge " + elementImportCounter + "; millis since last batch: " + millisSinceLastBatch);
      // }
      // if (elementImportCounter % 10000 == 0) {
      //   commit();
      // }
      elementImportCounter++;
      long srcNodeId = protoEdge.getSrc();
      long dstNodeId = protoEdge.getDst();

      Vertex srcVertex = lookupNodeByKey(srcNodeId);
      Vertex dstVertex = lookupNodeByKey(dstNodeId);
      org.apache.tinkerpop.gremlin.structure.Edge edge;
      try {
        edge = srcVertex.addEdge(protoEdge.getType().name(), dstVertex);
      } catch (IllegalArgumentException exception) {
        logger.warn("Failed to insert an edge", exception);
        continue;
      }
      for (Cpg.CpgStruct.Edge.Property property: protoEdge.getPropertyList()) {
        addPropertyToElement(edge, property.getName().name(), property.getValue());
      }
    }
  }

  protected Vertex lookupNodeByKey(Long nodeKey) {
    Object id = keyToVertexId.get(nodeKey);
    Iterator<Vertex> iter = graph.vertices(id);
    if (!iter.hasNext()) {
      logger.error("unable to find node with key=" + nodeKey + " and id=" + id);
      return null;
    }
    return iter.next();
  }

  protected void addPropertyToElement(Element tinkerElement, String propertyName,
                                    Cpg.PropertyValue propertyValue) {
    Cpg.PropertyValue.ValueCase valueCase = propertyValue.getValueCase();
    switch(valueCase) {
      case INT_VALUE:
        tinkerElement.property(propertyName, propertyValue.getIntValue());
        break;
      case STRING_VALUE:
        tinkerElement.property(propertyName, propertyValue.getStringValue());
        break;
      case BOOL_VALUE:
        tinkerElement.property(propertyName, propertyValue.getBoolValue());
        break;
      case STRING_LIST:
        if (tinkerElement instanceof Vertex) {
          propertyValue.getStringList().getValuesList().forEach(value ->
            ((Vertex) tinkerElement).property(VertexProperty.Cardinality.list, propertyName, value));
        } else {
          List<String> propertyList = new LinkedList<>();
          propertyList.addAll(propertyValue.getStringList().getValuesList());
          tinkerElement.property(propertyName, propertyList);
        }
        break;
      case VALUE_NOT_SET:
        break;
      default:
        throw new RuntimeException("Error: unsupported property case: " + valueCase.name());
    }
  }

  public io.shiftleft.queryprimitives.steps.starters.Cpg build() {
    commit();
    return new io.shiftleft.queryprimitives.steps.starters.Cpg(graph);
  }

  /* checks whether graph supports transactions and commits it. 
   * call this regularly when importing in bulk, to avoid the transaction growing too large */
  public void commit() {
    if (graph.features().graph().supportsTransactions()) {
      System.out.println("committing tx");
      graph.tx().commit();
    }
  }
}
