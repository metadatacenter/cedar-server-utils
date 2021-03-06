package org.metadatacenter.server.neo4j.cypher.query;

public class CypherQueryBuilderVersion extends AbstractCypherQueryBuilder {

  public static String getResourceWithPreviousVersion() {
    return "" +
        " MATCH (artifact:<LABEL.ARTIFACT> {<PROP.PREVIOUS_VERSION>:{<PROP.ID>}})" +
        " RETURN artifact";
  }
}
