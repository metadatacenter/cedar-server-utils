package org.metadatacenter.server.neo4j.cypher.query;

public class CypherQueryBuilderVersion extends AbstractCypherQueryBuilder {

  public static String getResourceWithPreviousVersion() {
    return "" +
        " MATCH (resource:<LABEL.RESOURCE> {previousVersion:{id}})" +
        " RETURN resource";
  }
}
