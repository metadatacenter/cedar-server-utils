package org.metadatacenter.server.neo4j.cypher.query;

import org.metadatacenter.model.IsRoot;
import org.metadatacenter.model.IsSystem;
import org.metadatacenter.model.IsUserHome;
import org.metadatacenter.model.RelationLabel;
import org.metadatacenter.model.folderserver.FolderServerFolder;
import org.metadatacenter.model.folderserver.FolderServerNode;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.server.neo4j.NodeLabel;
import org.metadatacenter.server.neo4j.cypher.NodeProperty;
import org.metadatacenter.server.workspace.QuerySortOptions;

import java.util.List;

public abstract class AbstractCypherQueryBuilder {

  protected final static int ORDER_FOLDER = 1;
  protected final static int ORDER_NON_FOLDER = 2;
  protected static final String ALIAS_FOO = "foo";

  protected static String buildCreateAssignment(NodeProperty property) {
    return property.getValue() + ": {" + property.getValue() + "}";
  }

  protected static String buildUpdateAssignment(NodeProperty property) {
    return property.getValue() + "= {" + property.getValue() + "}";
  }

  protected static String buildSetter(String nodeAlias, NodeProperty property) {
    return " SET " + nodeAlias + "." + buildUpdateAssignment(property);
  }

  protected static String createFSResource(String nodeAlias, FolderServerResource newResource) {
    return createFSNode(nodeAlias, newResource);
  }

  protected static String createFSFolder(String nodeAlias, FolderServerFolder newFolder) {
    return createFSNode(nodeAlias, newFolder);
  }

  private static NodeLabel getFolderLabel(IsRoot isRoot, IsSystem isSystem, IsUserHome isUserHome) {
    if (isUserHome == IsUserHome.TRUE) {
      return NodeLabel.USER_HOME_FOLDER;
    } else if (isSystem == IsSystem.TRUE) {
      return NodeLabel.SYSTEM_FOLDER;
    } else {
      return NodeLabel.FOLDER;
    }
  }

  private static String createFSNode(String nodeAlias, FolderServerNode newNode) {

    NodeLabel label = NodeLabel.forCedarNodeType(newNode.getType());
    if (label == null) {
      FolderServerFolder f = (FolderServerFolder) newNode;
      label = getFolderLabel(IsRoot.forValue(f.isRoot()),
          IsSystem.forValue(f.isSystem()),
          IsUserHome.forValue(f.isUserHome()));
    }

    StringBuilder sb = new StringBuilder();
    sb.append(" CREATE (").append(nodeAlias).append(":").append(label).append(" {");

    sb.append(buildCreateAssignment(NodeProperty.ID)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.NAME)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.DESCRIPTION)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.CREATED_BY)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.CREATED_ON)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.CREATED_ON_TS)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.LAST_UPDATED_BY)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.LAST_UPDATED_ON)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.LAST_UPDATED_ON_TS)).append(",");
    sb.append(buildCreateAssignment(NodeProperty.OWNED_BY)).append(",");
    if (newNode instanceof FolderServerResource) {
      FolderServerResource newResource = (FolderServerResource) newNode;
      if (newResource.getVersion() != null) {
        sb.append(buildCreateAssignment(NodeProperty.VERSION)).append(",");
      }
      if (newResource.getPublicationStatus() != null) {
        sb.append(buildCreateAssignment(NodeProperty.PUBLICATION_STATUS)).append(",");
      }
      if (newResource.getDerivedFrom() != null) {
        sb.append(buildCreateAssignment(NodeProperty.DERIVED_FROM)).append(",");
      }
      if (newResource.getPreviousVersion() != null) {
        sb.append(buildCreateAssignment(NodeProperty.PREVIOUS_VERSION)).append(",");
      }
    } else if (newNode instanceof FolderServerFolder) {
      FolderServerFolder newFolder = (FolderServerFolder) newNode;
      if (newFolder.isRoot()) {
        sb.append(buildCreateAssignment(NodeProperty.IS_ROOT)).append(",");
      }
      if (newFolder.isSystem()) {
        sb.append(buildCreateAssignment(NodeProperty.IS_SYSTEM)).append(",");
      }
      if (newFolder.isUserHome()) {
        sb.append(buildCreateAssignment(NodeProperty.IS_USER_HOME)).append(",");
        sb.append(buildCreateAssignment(NodeProperty.HOME_OF)).append(",");
      }
    }

    sb.append(NodeProperty.NODE_SORT_ORDER).append(":")
        .append(label.isFolder() ? ORDER_FOLDER : ORDER_NON_FOLDER).append(",");

    sb.append(buildCreateAssignment(NodeProperty.NODE_TYPE));
    sb.append("})");
    return sb.toString();
  }

  protected static String getOrderByExpression(String nodeAlias, List<String> sortList) {
    StringBuilder sb = new StringBuilder();
    String prefix = "";
    for (String s : sortList) {
      sb.append(prefix);
      sb.append(getOrderByExpression(nodeAlias, s));
      prefix = ", ";
    }
    return sb.toString();
  }

  protected static String getOrderByExpression(String nodeAlias, String s) {
    StringBuilder sb = new StringBuilder();
    if (s != null) {
      if (s.startsWith("-")) {
        sb.append(getCaseInsensitiveSortExpression(nodeAlias, s.substring(1)));
        sb.append(" DESC");
      } else {
        sb.append(getCaseInsensitiveSortExpression(nodeAlias, s));
        sb.append(" ASC");
      }
    }
    return sb.toString();
  }

  private static String getCaseInsensitiveSortExpression(String nodeAlias, String fieldName) {
    StringBuilder sb = new StringBuilder();
    if (QuerySortOptions.isTextual(fieldName)) {
      sb.append(" LOWER(").append(nodeAlias).append(".").append(QuerySortOptions.getFieldName(fieldName)).append(")");
    } else {
      sb.append(nodeAlias).append(".").append(QuerySortOptions.getFieldName(fieldName));
    }
    return sb.toString();
  }

  public static String addRelation(NodeLabel fromLabel, NodeLabel toLabel, RelationLabel relation) {
    return "" +
        " MATCH (fromNode:" + fromLabel + " {id:{fromId} })" +
        " MATCH (toNode:" + toLabel + " {id:{toId} })" +
        " CREATE (fromNode)-[:" + relation + "]->(toNode)" +
        " RETURN fromNode";
  }

  public static String removeRelation(NodeLabel fromLabel, NodeLabel toLabel, RelationLabel relation) {
    return "" +
        " MATCH (fromNode:" + fromLabel + " {id:{fromId} })" +
        " MATCH (toNode:" + toLabel + " {id:{toId} })" +
        " MATCH (fromNode)-[relation:" + relation + "]->(toNode)" +
        " DELETE relation" +
        " RETURN fromNode";
  }

  protected static String createFSResourceAsChildOfId(FolderServerResource newResource) {
    return "" +
        " MATCH (user:<LABEL.USER> {id:{userId}})" +
        " MATCH (parent:<LABEL.FOLDER> {id:{parentId}})" +
        createFSResource("child", newResource) +
        " CREATE (user)-[:<REL.OWNS>]->(child)" +
        " CREATE (parent)-[:<REL.CONTAINS>]->(child)" +
        " RETURN child";
  }

  protected static String createFSFolderAsChildOfId(FolderServerFolder newFolder) {
    return "" +
        " MATCH (user:<LABEL.USER> {id:{userId}})" +
        " MATCH (parent:<LABEL.FOLDER> {id:{parentId}})" +
        createFSFolder("child", newFolder) +
        " CREATE (user)-[:<REL.OWNS>]->(child)" +
        " CREATE (parent)-[:<REL.CONTAINS>]->(child)" +
        " RETURN child";
  }

  protected static String getUserToResourceRelationOneStepDirectly(RelationLabel relationLabel, String nodeAlias) {
    return "(user)-[:" + relationLabel + "]->(" + nodeAlias + ")";
  }

  protected static String getUserToResourceRelationOneStepThroughGroup(RelationLabel relationLabel, String nodeAlias) {
    return "(user)-[:<REL.MEMBEROF>*0..1]->()-[:" + relationLabel + "]->(" + nodeAlias + ")";
  }

  protected static String getUserToResourceRelationTwoSteps(RelationLabel relationLabel, String nodeAlias) {
    return "(user)-[:<REL.MEMBEROF>*0..1]->()-[:" + relationLabel + "]->()-[:<REL.CONTAINS>*0..]->(" + nodeAlias + ")";
  }

  protected static String getResourcePermissionConditions(String relationPrefix, String nodeAlias) {
    return "" +
        " " + relationPrefix + " " +
        "(" +
        getUserToResourceRelationOneStepDirectly(RelationLabel.OWNS, nodeAlias) +
        " OR " +
        getUserToResourceRelationOneStepThroughGroup(RelationLabel.CANREADTHIS, nodeAlias) +
        " OR " +
        getUserToResourceRelationTwoSteps(RelationLabel.CANREAD, nodeAlias) +
        " OR " +
        getUserToResourceRelationTwoSteps(RelationLabel.CANWRITE, nodeAlias) +
        ")";
  }

  protected static String getVersionConditions(String relationPrefix, String nodeAlias) {
    return "" +
        " " + relationPrefix + " " +
        "(" + nodeAlias + ".<PROP.VERSION> = {version}" + ")";
  }

  protected static String getPublicationStatusConditions(String relationPrefix, String nodeAlias) {
    return "" +
        " " + relationPrefix + " " +
        "(" +
        nodeAlias + ".<PROP.PUBLICATION_STATUS> = {publicationStatus}" +
        " OR " +
        nodeAlias + ".<PROP.PUBLICATION_STATUS> IS NULL" +
        ")";
  }

}
