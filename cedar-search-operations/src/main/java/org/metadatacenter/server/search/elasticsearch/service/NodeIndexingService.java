package org.metadatacenter.server.search.elasticsearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.client.Client;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.folderserver.FolderServerNode;
import org.metadatacenter.model.folderserver.FolderServerNodeInfo;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.PermissionServiceSession;
import org.metadatacenter.server.search.IndexedDocumentId;
import org.metadatacenter.server.search.elasticsearch.document.IndexingDocumentDocument;
import org.metadatacenter.server.search.elasticsearch.worker.ElasticsearchIndexingWorker;
import org.metadatacenter.server.security.model.auth.CedarNodeMaterializedPermissions;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeIndexingService extends AbstractIndexingService {

  private static final Logger log = LoggerFactory.getLogger(NodeIndexingService.class);

  private final ElasticsearchIndexingWorker indexWorker;

  NodeIndexingService(String indexName, Client client) {
    indexWorker = new ElasticsearchIndexingWorker(indexName, client);
  }

  public IndexedDocumentId indexDocument(FolderServerNode node, CedarNodeMaterializedPermissions permissions) throws CedarProcessingException {
    log.debug("Indexing node (id = " + node.getId() + ")");
    IndexingDocumentDocument ir = new IndexingDocumentDocument(node.getId());

    ir.setInfo(FolderServerNodeInfo.fromNode(node));
    ir.setMaterializedPermissions(permissions);
    ir.setSummaryText(getSummaryText(node));

    JsonNode jsonResource = JsonMapper.MAPPER.convertValue(ir, JsonNode.class);
    return indexWorker.addToIndex(jsonResource, null);
  }

  public IndexedDocumentId indexDocument(FolderServerNode node, CedarRequestContext c) throws CedarProcessingException {
    log.debug("Indexing node (id = " + node.getId() + ")");
    IndexingDocumentDocument ir = new IndexingDocumentDocument(node.getId());
    PermissionServiceSession permissionSession = CedarDataServices.getPermissionServiceSession(c);
    CedarNodeMaterializedPermissions permissions = permissionSession.getNodeMaterializedPermission(node.getId(),
        node.getType().asFolderOrResource());

    ir.setInfo(FolderServerNodeInfo.fromNode(node));
    ir.setMaterializedPermissions(permissions);
    ir.setSummaryText(getSummaryText(node));

    JsonNode jsonResource = JsonMapper.MAPPER.convertValue(ir, JsonNode.class);
    return indexWorker.addToIndex(jsonResource, null);
  }

  private String getSummaryText(FolderServerNode node) {
    //TODO: implement summary text creation here
    return node.getName() + " " + node.getDescription();
  }

  public long removeDocumentFromIndex(String nodeId) throws CedarProcessingException {
    if (nodeId != null) {
      log.debug("Removing node from index (id = " + nodeId);
      return indexWorker.removeAllFromIndex(nodeId, null);
    } else {
      return -1;
    }
  }

  public void removeDocumentFromIndex(IndexedDocumentId indexedDocumentId) {
    log.debug("Removing node from index (_id = " + indexedDocumentId.getId());
    indexWorker.removeFromIndex(indexedDocumentId);
  }

}
