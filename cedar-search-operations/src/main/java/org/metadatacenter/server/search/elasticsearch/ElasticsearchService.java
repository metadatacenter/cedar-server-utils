package org.metadatacenter.server.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.metadatacenter.config.ElasticsearchConfig;
import org.metadatacenter.config.ElasticsearchSettingsMappingsConfig;
import org.metadatacenter.exception.CedarProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static org.metadatacenter.constant.ElasticsearchConstants.*;

public class ElasticsearchService implements IElasticsearchService {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);

  private Settings settings;
  private String esHost;
  private int esTransportPort;
  private int esSize;
  private int scrollKeepAlive;
  private HashMap<String, Object> indexSettings;
  private HashMap<String, Object> indexMappingsResource;
  private HashMap<String, Object> indexMappingsPermissions;
  private Client client = null;

  public ElasticsearchService(ElasticsearchConfig esc, ElasticsearchSettingsMappingsConfig essmc) {
    this.esHost = esc.getHost();
    this.esTransportPort = esc.getTransportPort();
    this.esSize = esc.getSize();
    this.scrollKeepAlive = esc.getScrollKeepAlive();
    this.indexSettings = essmc.getSettings();
    this.indexMappingsResource = essmc.getMappingsResource();
    this.indexMappingsPermissions = essmc.getMappingsPermissions();

    settings = Settings.settingsBuilder()
        .put("cluster.name", esc.getCluster()).build();
  }

  public void createIndex(String indexName, String documentTypeResource, String documentTypePermissions)
      throws CedarProcessingException {
    try {
      client = getClient();
      CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
      // Set settings
      if (indexSettings != null) {
        createIndexRequestBuilder.setSettings(indexSettings);
      }
      // Put mapping
      if (indexMappingsResource != null) {
        createIndexRequestBuilder.addMapping(documentTypeResource, indexMappingsResource);
      }
      if (indexMappingsPermissions != null) {
        createIndexRequestBuilder.addMapping(documentTypePermissions, indexMappingsPermissions);
      }
      // Create index
      CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();
      if (!response.isAcknowledged()) {
        throw new CedarProcessingException("Failed to create the index " + indexName);
      }
      log.info("The index " + indexName + " has been created");
    } catch (Exception e) {
      throw new CedarProcessingException(e);
    }
  }

  public IndexedDocumentId addToIndex(JsonNode json, String indexName, String documentType, IndexedDocumentId parent) throws
      CedarProcessingException {
    IndexedDocumentId newId = null;
    try {
      boolean again = true;
      int maxAttempts = 20;
      int count = 0;
      while (again) {
        try {
          client = getClient();
          IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, documentType).setSource(json
              .toString());
          if (parent != null && parent.getId() != null) {
            indexRequestBuilder.setParent(parent.getId());
          }
          IndexResponse response = indexRequestBuilder.get();
          if (response.isCreated()) {
            log.debug("The resource has been indexed");
            again = false;
            newId = new IndexedDocumentId(response.getId());
          } else {
            throw new CedarProcessingException("Failed to index resource");
          }
        } catch (NoNodeAvailableException e) {
          if (count++ > maxAttempts) {
            throw e;
          }
        }
      }
    } catch (Exception e) {
      throw new CedarProcessingException(e);
    }
    return newId;
  }

  public void removeFromIndex(String resourceId, String indexName, String documentType) throws
      CedarProcessingException {
    log.debug("Removing resource @id=" + resourceId + "from the index");
    try {
      client = getClient();
      // Get resources by resource id
      SearchResponse responseSearch = client.prepareSearch(indexName)
          .setTypes(documentType).setQuery(QueryBuilders.matchQuery(ES_RESOURCE_PREFIX + ES_RESOURCE_ID_FIELD,
              resourceId))
          .execute().actionGet();

      // Delete by Elasticsearch id
      for (SearchHit hit : responseSearch.getHits()) {
        DeleteResponse responseDelete = client.prepareDelete(indexName, documentType, hit.id())
            .execute()
            .actionGet();
        if (!responseDelete.isFound()) {
          throw new CedarProcessingException("Failed to remove resource " + resourceId + " from the index");
        }
        log.debug("The resource " + resourceId + " has been removed from the index");
      }
    } catch (Exception e) {
      throw new CedarProcessingException(e);
    }
  }

  public SearchResponse search(String query, List<String> resourceTypes, List<String> sortList, String templateId,
                               String indexName, String documentType, int limit, int offset) throws
      CedarProcessingException {

    client = getClient();
    SearchRequestBuilder searchRequest = getSearchRequestBuilder(client, query, resourceTypes, sortList, templateId,
        indexName, documentType);

    // Set offset (from) and limit (size)
    searchRequest.setFrom(offset);
    searchRequest.setSize(limit);
    //System.out.println("Search query in Query DSL: " + searchRequest.internalBuilder());

    // Execute request
    SearchResponse response = searchRequest.execute().actionGet();
    return response;
  }

  // It uses the scroll API. It retrieves all results. No pagination and therefore no offset. Scrolling is not
  // intended for real time user requests, but rather for processing large amounts of data.
  // More info: https://www.elastic.co/guide/en/elasticsearch/reference/2.3/search-request-scroll.html
  public List<SearchHit> searchDeep(String query, List<String> resourceTypes, List<String> sortList, String templateId,
                                    String indexName, String documentType, int limit) throws CedarProcessingException {

    client = getClient();
    SearchRequestBuilder searchRequest = getSearchRequestBuilder(client, query, resourceTypes, sortList, templateId,
        indexName, documentType);

    // Set scroll and scroll size
    searchRequest.setScroll(TimeValue.timeValueMinutes(2));
    searchRequest.setSize(50000);

    //System.out.println("Search query in Query DSL: " + searchRequest.internalBuilder());

    // Execute request
    SearchResponse response = searchRequest.execute().actionGet();

    List<SearchHit> allHits = new ArrayList<>();

    while (response.getHits().hits().length != 0 || response.getHits().hits().length >= limit) {
      allHits.addAll(Arrays.asList(response.getHits().hits()));
      //next scroll
      response = client.prepareSearchScroll(response.getScrollId()).setScroll(TimeValue.timeValueMinutes(2))
          .execute().actionGet();
    }
    return allHits;
  }

  private SearchRequestBuilder getSearchRequestBuilder(Client client, String query, List<String> resourceTypes,
                                                       List<String> sortList, String templateId, String indexName,
                                                       String documentType) {

    SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName).setTypes(documentType);

    // See: https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
    BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

    // Search query
    if (query != null && query.length() > 0) {
      mainQuery.must(QueryBuilders.queryStringQuery(query).field(ES_RESOURCE_PREFIX + ES_RESOURCE_NAME_FIELD));
    } else {
      mainQuery.must(QueryBuilders.matchAllQuery());
    }

    // Filter by resource type
    BoolQueryBuilder resourceTypesQuery = QueryBuilders.boolQuery();
    if (resourceTypes != null && resourceTypes.size() > 0) {
      for (String rt : resourceTypes) {
        resourceTypesQuery.should(QueryBuilders.termQuery(ES_RESOURCE_PREFIX + ES_RESOURCE_RESOURCETYPE_FIELD, rt));
      }
    }
    mainQuery.must(resourceTypesQuery);

    // Filter by template id
    if (templateId != null) {
      mainQuery.must(QueryBuilders.matchQuery(ES_TEMPLATEID_FIELD, templateId));
    }

    // Set main query
    searchRequestBuilder.setQuery(mainQuery);

    // Sort by field
    if (sortList != null && sortList.size() > 0) {
      for (String s : sortList) {
        SortOrder sortOrder = SortOrder.ASC;
        if (s.startsWith(ES_SORT_DESC_PREFIX)) {
          sortOrder = SortOrder.DESC;
          s = s.substring(1);
        }
        String sortField = ES_RESOURCE_PREFIX + (s.compareTo(ES_RESOURCE_NAME_FIELD) == 0 ?
            ES_RESOURCE_SORTABLE_NAME_FIELD : s);
        searchRequestBuilder.addSort(sortField, sortOrder);
      }
    }
    return searchRequestBuilder;
  }

  public boolean indexExists(String indexName) throws CedarProcessingException {
    client = getClient();
    boolean exists = client.admin().indices().prepareExists(indexName)
        .execute().actionGet().isExists();
    return exists;
  }

  public void deleteIndex(String indexName) throws CedarProcessingException {
    client = getClient();
    DeleteIndexResponse deleteIndexResponse =
        client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
    if (!deleteIndexResponse.isAcknowledged()) {
      throw new CedarProcessingException("Failed to delete index '" + indexName + "'");
    }
    log.info("The index '" + indexName + "' has been deleted");
  }

  public void addAlias(String indexName, String aliasName) throws CedarProcessingException {
    client = getClient();
    IndicesAliasesResponse response = client.admin().indices().prepareAliases()
        .addAlias(indexName, aliasName)
        .execute().actionGet();
    if (!response.isAcknowledged()) {
      throw new CedarProcessingException("Failed to add alias '" + aliasName + "' to index '" + indexName + "'");
    }
    log.info("The alias '" + aliasName + "' has been added to index '" + indexName + "'");
  }

  public void deleteAlias(String indexName, String aliasName) throws CedarProcessingException {
    IndicesAliasesResponse response = getClient().admin().indices().prepareAliases()
        .removeAlias(indexName, aliasName)
        .execute().actionGet();
    if (!response.isAcknowledged()) {
      throw new CedarProcessingException("Failed to remove alias '" + aliasName + "' from index '" + indexName + "'");
    }
    log.info("The alias '" + aliasName + "' has been removed from the index '" + indexName + "'");
  }

  public List<String> getIndexesByAlias(String aliasName) throws CedarProcessingException {
    List<String> indexNames = new ArrayList<>();
    client = getClient();
    AliasOrIndex alias = client.admin().cluster()
        .prepareState().execute()
        .actionGet().getState()
        .getMetaData().getAliasAndIndexLookup().get(aliasName);
    for (IndexMetaData indexInfo : alias.getIndices()) {
      indexNames.add(indexInfo.getIndex());
    }
    return indexNames;
  }

  // Retrieve all values for a fieldName. Dot notation is allowed (e.g. info.@id)
  public List<String> findAllValuesForField(String fieldName, String indexName, String documentType) throws
      CedarProcessingException {
    List<String> fieldValues = new ArrayList<>();
    client = getClient();
    QueryBuilder qb = QueryBuilders.matchAllQuery();
    SearchRequestBuilder searchRequest = client.prepareSearch(indexName).setTypes(documentType)
        .setFetchSource(new String[]{fieldName}, null)
        .setScroll(new TimeValue(scrollKeepAlive)).setQuery(qb).setSize(esSize);
    //System.out.println("Search query in Query DSL: " + searchRequest.internalBuilder());
    SearchResponse response = searchRequest.execute().actionGet();
    // Scroll until no hits are returned
    while (true) {
      for (SearchHit hit : response.getHits().getHits()) {
        Map<String, Object> f = hit.getSource();
        String[] pathFragments = fieldName.split("\\.");
        for (int i = 0; i < pathFragments.length - 1; i++) {
          f = (Map<String, Object>) f.get(pathFragments[0]);
        }
        String fieldValue = (String) f.get(pathFragments[pathFragments.length - 1]);
        fieldValues.add(fieldValue);
      }
      response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(scrollKeepAlive))
          .execute().actionGet();
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        break;
      }
    }
    return fieldValues;
  }

  private Client getClient() throws CedarProcessingException {
    try {
      if (client == null) {
        client = TransportClient.builder().settings(settings).build().addTransportAddress(new
            InetSocketTransportAddress(InetAddress.getByName(esHost), esTransportPort));
      }
      return client;
    } catch (UnknownHostException e) {
      throw new CedarProcessingException(e);
    }
  }

  public void closeClient() {
    client.close();
  }

}