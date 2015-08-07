/**
 * Copyright at HighForm Inc. All rights reserved Mar 12, 2013.
 */

package com.highform.datastore.index.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.highform.config.ConfigHelper;
import com.highform.datastore.DSRecord;
import com.highform.datastore.SchemaHelper;
import com.highform.datastore.TableSchemaMap;
import com.highform.datastore.index.IClient;
import com.highform.datastore.index.exceptions.IndexException;
import com.highform.datastore.proto.Datastore.Schema;
import com.highform.datastore.proto.Datastore.TableSchema;
import com.highform.index.elasticsearch.proto.Elasticsearch.ESConfig;

/**
 * Elastic Search Client
 *
 * @author fei
 */
public class ElasticSearchClient implements IClient {
  private final Client client;
  private final Schema schema;

  public final static String INDEX_TYPE = "type";
  public final static String INDEX_SCHEMA = "schema";
  public final static String INDEX_SEARCH_TYPE = "search_type";
  public final static String QUERY_FROM = "from";
  public final static String QUERY_SIZE = "size";

  public ElasticSearchClient(String configFile) {
    this((ESConfig) ConfigHelper.loadConfig(configFile, ESConfig.newBuilder()));
  }

  public ElasticSearchClient(ESConfig config) {
    Preconditions.checkNotNull(config);
    this.client = ElasticSearchConfig.getClient(config);
    this.schema = config.getSchema();
  }

  public ElasticSearchClient(Client client, Schema schema) {
    this.client = client;
    this.schema = schema;
  }

  @Override
  public void connect() {
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.close();
    }
  }

  @Override
  public boolean updateRecords(List<DSRecord<Object>> records, Map<String, String> params) throws IndexException {
    Preconditions.checkNotNull(records);
    String schema = Preconditions.checkNotNull(params.get(INDEX_SCHEMA));
    String type = Preconditions.checkNotNull(params.get(INDEX_TYPE));

    TableSchema tableSchema = SchemaHelper.getIndexSchema(this.schema, schema);

    BulkRequestBuilder bulkRequest = ElasticSearchConfig.createInputDocuments(this.client, records, tableSchema,
        schema, type);
    return executeBulkRequest(bulkRequest);
  }

  @Override
  public boolean deleteRecordsByIds(List<String> ids, Map<String, String> params) throws IndexException {
    Preconditions.checkNotNull(ids);
    String schema = Preconditions.checkNotNull(params.get(INDEX_SCHEMA));
    String type = Preconditions.checkNotNull(params.get(INDEX_TYPE));
    Preconditions.checkArgument(StringUtils.isNotBlank(schema) && StringUtils.isNotBlank(type));

    BulkRequestBuilder bulkRequest = ElasticSearchConfig.createDeleteDocuments(this.client, ids, schema, type);
    return executeBulkRequest(bulkRequest);
  }

  private static boolean executeBulkRequest(BulkRequestBuilder bulkRequest) throws IndexException {
    try {
      BulkResponse bulkResponse = bulkRequest.execute().actionGet();
      if (bulkResponse.hasFailures()) {
        throw new IndexException(bulkResponse.toString());
      }
    } catch (ElasticSearchException e) {
      throw new IndexException(e);
    }
    return true;
  }

  @Override
  public <T> List<DSRecord<Object>> getRecords(T query, Map<String, Object> params) throws IndexException {
    QueryBuilder esQuery = (QueryBuilder) Preconditions.checkNotNull(query);

    SearchRequestBuilder searchRequest = buildSearchRequest(esQuery, params, null);
    SearchResponse response = executeQueryRequest(searchRequest);
    return parseQueryResponse(response);
  }

  @Override
  public <T> List<DSRecord<Object>> getRecords(T query, Map<String, Object> params, Map<String, String> sortFields)
      throws IndexException {
    QueryBuilder esQuery = (QueryBuilder) Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(sortFields);

    SearchRequestBuilder searchRequest = buildSearchRequest(esQuery, params, sortFields);
    SearchResponse response = executeQueryRequest(searchRequest);
    return parseQueryResponse(response);
  }

  private SearchRequestBuilder buildSearchRequest(QueryBuilder query, Map<String, Object> params,
      Map<String, String> sortFields) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(params);
    String schema = (String) Preconditions.checkNotNull(params.get(INDEX_SCHEMA));
    String type = (String) Preconditions.checkNotNull(params.get(INDEX_TYPE));
    SearchType searchType = (SearchType) Preconditions.checkNotNull(params.get(INDEX_SEARCH_TYPE));
    Integer from = (Integer) Preconditions.checkNotNull(params.get(QUERY_FROM));
    Integer size = (Integer) Preconditions.checkNotNull(params.get(QUERY_SIZE));

    Preconditions.checkArgument(StringUtils.isNotBlank(schema) && StringUtils.isNotBlank(type));

    SearchRequestBuilder searchRequest = this.client.prepareSearch(schema).setTypes(type).setSearchType(searchType)
        .setQuery(query).setFrom(from.intValue()).setSize(size.intValue());

    if (sortFields != null) {
      for (Entry<String, String> entry : sortFields.entrySet()) {
        searchRequest.addSort(entry.getKey(), SortOrder.valueOf(entry.getValue()));
      }
    }
    return searchRequest;
  }

  @Override
  public <T> long getCount(T query, Map<String, Object> params) throws IndexException {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(params);
    QueryBuilder esQuery = (QueryBuilder) query;
    String schema = (String) Preconditions.checkNotNull(params.get(INDEX_SCHEMA));
    String type = (String) Preconditions.checkNotNull(params.get(INDEX_TYPE));
    SearchType searchType = (SearchType) Preconditions.checkNotNull(params.get(INDEX_SEARCH_TYPE));

    Preconditions.checkArgument(StringUtils.isNotBlank(schema) && StringUtils.isNotBlank(type));

    SearchRequestBuilder searchRequest = this.client.prepareSearch(schema).setTypes(type).setSearchType(searchType)
        .setQuery(esQuery).setFrom(0).setSize(0);

    SearchResponse response = executeQueryRequest(searchRequest);
    return response.getHits().getTotalHits();
  }

  private static SearchResponse executeQueryRequest(SearchRequestBuilder searchRequest) throws IndexException {
    SearchResponse searchResponse = null;
    try {
      searchResponse = searchRequest.execute().actionGet();
    } catch (ElasticSearchException e) {
      throw new IndexException(e);
    }
    return searchResponse;
  }

  private static List<DSRecord<Object>> parseQueryResponse(SearchResponse response) {
    Preconditions.checkNotNull(response);
    List<DSRecord<Object>> records = Lists.newArrayList();
    SearchHit[] hits = response.getHits().getHits();
    for (SearchHit hit : hits) {
      DSRecord<Object> record = new DSRecord<Object>(hit.getId(), hit.getSource());
      records.add(record);
    }
    return records;
  }

  @Override
  public TableSchemaMap getIndexSchemaMap(String indexName) {
    return SchemaHelper.getIndexSchemaMap(this.schema, indexName);
  }

  @Override
  public void setDefaultCollection(String collectionName) {
  }

}
