package org.metadatacenter.server.search.elasticsearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.client.Client;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarBackendException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.search.IndexedDocumentType;
import org.metadatacenter.server.search.IndexedDocumentId;
import org.metadatacenter.server.search.elasticsearch.worker.ElasticsearchIndexingWorker;
import org.metadatacenter.server.search.elasticsearch.worker.ValueRecommenderIndexingWorker;
import org.metadatacenter.server.search.util.IndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueRecommenderIndexingService extends AbstractIndexingService {

  private static final Logger log = LoggerFactory.getLogger(ValueRecommenderIndexingService.class);

  private final ElasticsearchIndexingWorker indexWorker;
  private final IndexUtils indexUtils;

  ValueRecommenderIndexingService(String indexName, CedarConfig cedarConfig, Client client) {
    indexWorker = new ValueRecommenderIndexingWorker(cedarConfig.getElasticsearchConfig(), client,
        IndexedDocumentType.RULES);
    indexUtils = new IndexUtils(cedarConfig);
  }

  /**
   * Indexes all the rules generated for a template
   *
   * @param templateRules
   * @return
   * @throws CedarProcessingException
   */
  public IndexedDocumentId indexTemplateRules(JsonNode templateRules, String templateId) throws
      CedarProcessingException {
    log.info("Indexing template rules for template: " + templateId);
    return indexWorker.addToIndex(templateRules);
  }

  public void removeTemplateRulesFromIndex(String templateId) throws CedarProcessingException {
    if (templateId != null) {
      log.debug("Removing template rules from index (templateId = " + templateId + ")");
      //indexWorker.removeAllFromIndex(resourceId, parent);
    } else {
      throw new CedarProcessingException("TemplateId is null");
    }
  }


}
