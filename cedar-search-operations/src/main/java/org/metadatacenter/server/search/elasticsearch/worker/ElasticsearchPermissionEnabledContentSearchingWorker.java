package org.metadatacenter.server.search.elasticsearch.worker;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.metadatacenter.config.ElasticsearchConfig;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.search.IndexedDocumentType;
import org.metadatacenter.server.security.model.auth.CedarNodeMaterializedPermissions;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.server.security.model.auth.NodeSharePermission;
import org.metadatacenter.server.security.model.user.ResourcePublicationStatusFilter;
import org.metadatacenter.server.security.model.user.ResourceVersionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.metadatacenter.constant.ElasticsearchConstants.*;

public class ElasticsearchPermissionEnabledContentSearchingWorker {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchPermissionEnabledContentSearchingWorker.class);

  private final Client client;
  private final String indexName;

  public ElasticsearchPermissionEnabledContentSearchingWorker(ElasticsearchConfig config, Client client) {
    this.client = client;
    this.indexName = config.getIndexes().getSearchIndex().getName();
  }

  public SearchResponseResult search(CedarRequestContext rctx, String query, List<String> resourceTypes,
                                     ResourceVersionFilter version, ResourcePublicationStatusFilter
                                         publicationStatus, List<String> sortList, int limit, int offset) {

    SearchRequestBuilder searchRequest = getSearchRequestBuilder(rctx, query, resourceTypes, version,
        publicationStatus, sortList);

    searchRequest.setFrom(offset);
    searchRequest.setSize(limit);

    // Execute request
    SearchResponse response = searchRequest.execute().actionGet();

    SearchResponseResult result = new SearchResponseResult();
    result.setTotalCount(response.getHits().getTotalHits());

    for (SearchHit hit : response.getHits()) {
      result.add(hit);
    }

    return result;
  }

  // It uses the scroll API. It retrieves all results. No pagination and therefore no offset. Scrolling is not
  // intended for real time user requests, but rather for processing large amounts of data.
  // More info: https://www.elastic.co/guide/en/elasticsearch/reference/2.3/search-request-scroll.html
  public SearchResponseResult searchDeep(CedarRequestContext rctx, String query, List<String> resourceTypes,
                                         ResourceVersionFilter version, ResourcePublicationStatusFilter
                                             publicationStatus, List<String> sortList, int limit, int offset) {

    SearchRequestBuilder searchRequest = getSearchRequestBuilder(rctx, query, resourceTypes, version,
        publicationStatus, sortList);

    // Set scroll and scroll size
    TimeValue timeout = TimeValue.timeValueMinutes(2);
    searchRequest.setScroll(timeout);
    searchRequest.setSize(offset + limit);

    //log.debug("Search query in Query DSL: " + searchRequest);

    // Execute request
    SearchResponse response = searchRequest.execute().actionGet();

    SearchResponseResult result = new SearchResponseResult();
    result.setTotalCount(response.getHits().getTotalHits());

    int counter = 0;
    while (response.getHits().getHits().length != 0) {
      for (SearchHit hit : response.getHits().getHits()) {
        if (counter >= offset && counter < offset + limit) {
          result.add(hit);
        }
        counter++;
      }
      //next scroll
      response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeout).execute().actionGet();
    }
    return result;
  }

  private SearchRequestBuilder getSearchRequestBuilder(CedarRequestContext rctx, String query,
                                                       List<String> resourceTypes, ResourceVersionFilter version,
                                                       ResourcePublicationStatusFilter publicationStatus,
                                                       List<String> sortList) {

    SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName)
        .setTypes(IndexedDocumentType.DOC.getValue());

    BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

    String userId = rctx.getCedarUser().getId();

    if (!rctx.getCedarUser().has(CedarPermission.READ_NOT_READABLE_NODE)) {
      // Filter by user
      QueryBuilder userIdQuery = QueryBuilders.termQuery(USERS, CedarNodeMaterializedPermissions.getKey(userId,
          NodePermission.READ));
      BoolQueryBuilder permissionQuery = QueryBuilders.boolQuery();

      QueryBuilder everybodyReadQuery =
          QueryBuilders.termsQuery(INFO_EVERYBODY_PERMISSION, NodeSharePermission.READ.getValue());
      QueryBuilder everybodyWriteQuery =
          QueryBuilders.termsQuery(INFO_EVERYBODY_PERMISSION, NodeSharePermission.WRITE.getValue());

      permissionQuery.should(userIdQuery);
      permissionQuery.should(everybodyReadQuery);
      permissionQuery.should(everybodyWriteQuery);
      mainQuery.must(permissionQuery);
    }

    // Search by either title and description (summaryText) or field names and/or values (infoFields). Note that the
    // current implementation does not support queries addressed to summaryText and infoFields at the same time
    // (e.g., cancer AND disease:crc).
    if (query != null && query.length() > 0) {
      // Query summaryText
      if (!query.contains(":")) {
        if (enclosedByQuotes(query)) {
          query = query.substring(1, query.length() - 1);
          QueryBuilder summaryTextQuery = QueryBuilders.matchPhraseQuery(SUMMARY_RAW_TEXT, query);
          mainQuery.must(summaryTextQuery);
        } else {
          QueryBuilder summaryTextQuery = QueryBuilders.queryStringQuery(query).field(SUMMARY_TEXT);
          mainQuery.must(summaryTextQuery);
        }
      // Query infoFields
      } else {

        List<String> translatedQueryTokens = translateToInternalFieldsQueryTokens(query);

        for (String token : translatedQueryTokens) {
          QueryBuilder infoFieldsQuery = QueryBuilders.queryStringQuery(token);
          QueryBuilder nestedInfoFieldsQuery =
              QueryBuilders.nestedQuery("infoFields", infoFieldsQuery, ScoreMode.None);
          mainQuery.must(nestedInfoFieldsQuery);
        }

      }
    }

    // Filter by resource type
    if (resourceTypes != null && resourceTypes.size() > 0) {
      QueryBuilder resourceTypesQuery = QueryBuilders.termsQuery(RESOURCE_TYPE, resourceTypes);
      mainQuery.must(resourceTypesQuery);
    }

    // Filter version
    if (version != null && version != ResourceVersionFilter.ALL) {
      BoolQueryBuilder versionQuery = QueryBuilders.boolQuery();
      BoolQueryBuilder inner1Query = QueryBuilders.boolQuery();
      BoolQueryBuilder inner2Query = QueryBuilders.boolQuery();
      if (version == ResourceVersionFilter.LATEST) {
        QueryBuilder versionEqualsQuery = QueryBuilders.termsQuery(INFO_IS_LATEST_VERSION, true);
        inner1Query.must(versionEqualsQuery);
        QueryBuilder versionExistsQuery = QueryBuilders.existsQuery(INFO_IS_LATEST_VERSION);
        inner2Query.mustNot(versionExistsQuery);
      } else if (version == ResourceVersionFilter.LATEST_BY_STATUS) {
        QueryBuilder versionEquals1Query = QueryBuilders.termsQuery(INFO_IS_LATEST_PUBLISHED_VERSION, true);
        QueryBuilder versionEquals2Query = QueryBuilders.termsQuery(INFO_IS_LATEST_DRAFT_VERSION, true);
        inner1Query.should(versionEquals1Query);
        inner1Query.should(versionEquals2Query);
        QueryBuilder versionExists1Query = QueryBuilders.existsQuery(INFO_IS_LATEST_PUBLISHED_VERSION);
        QueryBuilder versionExists2Query = QueryBuilders.existsQuery(INFO_IS_LATEST_DRAFT_VERSION);
        inner2Query.mustNot(versionExists1Query);
        inner2Query.mustNot(versionExists2Query);
      }
      versionQuery.should(inner1Query);
      versionQuery.should(inner2Query);
      mainQuery.must(versionQuery);
    }

    // Filter publicationStatus
    if (publicationStatus != null && publicationStatus != ResourcePublicationStatusFilter.ALL) {
      BoolQueryBuilder publicationStatusQuery = QueryBuilders.boolQuery();
      BoolQueryBuilder inner1Query = QueryBuilders.boolQuery();
      QueryBuilder publicationStatusEqualsQuery = QueryBuilders.termsQuery(INFO_BIBO_STATUS,
          publicationStatus.getValue());
      inner1Query.must(publicationStatusEqualsQuery);
      BoolQueryBuilder inner2Query = QueryBuilders.boolQuery();
      QueryBuilder publicationStatusExistsQuery = QueryBuilders.existsQuery(INFO_BIBO_STATUS);
      inner2Query.mustNot(publicationStatusExistsQuery);
      publicationStatusQuery.should(inner1Query);
      publicationStatusQuery.should(inner2Query);
      mainQuery.must(publicationStatusQuery);
    }

    // Set main query
    searchRequestBuilder.setQuery(mainQuery);
    log.info("Search query in Query DSL:\n" + mainQuery);

    // Sort by field
    // The name is stored on the node, so we can sort by that
    if (sortList != null && sortList.size() > 0) {
      for (String s : sortList) {
        SortOrder sortOrder = SortOrder.ASC;
        if (s.startsWith(ES_SORT_DESC_PREFIX)) {
          sortOrder = SortOrder.DESC;
          s = s.substring(1);
        }
        if (SORT_BY_NAME.equals(s)) {
          searchRequestBuilder.addSort(INFO_SCHEMA_NAME, sortOrder);
        } else if (SORT_LAST_UPDATED_ON_FIELD.equals(s)) {
          searchRequestBuilder.addSort(INFO_PAV_LAST_UPDATED_ON, sortOrder);
        } else if (SORT_CREATED_ON_FIELD.equals(s)) {
          searchRequestBuilder.addSort(INFO_PAV_CREATED_ON, sortOrder);
        }
      }
    }
    return searchRequestBuilder;
  }

  private boolean enclosedByQuotes(String keyword) {
    return keyword.startsWith("\"") && keyword.endsWith("\"");
  }

  /**
   * Translates a query in the format aaa:bbb to infoFields.fieldName:aaa AND infoFields.fieldValue:bbb
   * Sample queries for testing: aaa:bbb vvv: :bbb \"bla ble\":ccc ddd:\"bli blo\""
   * @param query
   * @return
   */
  private List<String> translateToInternalFieldsQueryTokens(String query) {

    String fieldValueSeparator = ":";

    List<String> translatedQueryTokens = new ArrayList<>();

    List<String> queryTokens = Arrays.asList(query.split("\\s+"));

    for (String token : queryTokens) {
      if (token.contains(fieldValueSeparator)) {
        translatedQueryTokens.add(translateQueryToken(token, fieldValueSeparator));
      }
    }

    return translatedQueryTokens;
  }

  private String translateQueryToken(String queryToken, String fieldValueSeparator) {
    String result = "";
    if (queryToken.contains(fieldValueSeparator)) {
      List<String> fieldValueTokens = Arrays.asList(queryToken.split(":"));
      if (fieldValueTokens.size() >= 1) {
        result = result.concat("infoFields.fieldName:").concat(fieldValueTokens.get(0));
      }
      if (fieldValueTokens.size() == 2) {
        result = result.concat(" AND ").concat("infoFields.fieldValue:").concat(fieldValueTokens.get(1));
      }
      else {
        return queryToken;
      }
      return result;
    }
    else {
      return queryToken;
    }
  }

  /**
   * Extract from the original query the fragment addressed to query the infoFields field
   *
   * @param query
   * @return
   */
  private String extractInfoFieldsQuery(String query) {
    String infoFieldsQuery = null;
    return null;
  }

}
