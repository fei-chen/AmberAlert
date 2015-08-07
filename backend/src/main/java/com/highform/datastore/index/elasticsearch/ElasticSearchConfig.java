/**
 * Copyright at HighForm Inc. All rights reserved Mar 12, 2013.
 */

package com.highform.datastore.index.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.highform.datastore.DSRecord;
import com.highform.datastore.TableSchemaMap;
import com.highform.datastore.proto.Datastore.TableSchema;
import com.highform.index.elasticsearch.proto.Elasticsearch.ESConfig;
import com.highform.index.elasticsearch.proto.Elasticsearch.Host;
import com.highform.index.elasticsearch.proto.Elasticsearch.Transport;

/**
 * ElasticSearchClient helper
 * 
 * @author fei
 */
public class ElasticSearchConfig {
  private final static Logger log = LoggerFactory.getLogger(ElasticSearchConfig.class);

  public static Client getClient(ESConfig config) {
    Preconditions.checkNotNull(config);

    Client client = null;
    switch (config.getClientType()) {
    case NodeClient:
      client = getNodeClient(config);
      break;
    case TransportClient:
      client = getTransportClient(config);
      break;
    default:
      log.error("Invalid client type: " + config.getClientType().toString());
    }
    return client;
  }

  public static Client getTransportClient(ESConfig config) {
    Preconditions.checkNotNull(config);

    Builder builder = ImmutableSettings.settingsBuilder().put("cluster.name", config.getCluster());
    if (config.hasTransport()) {
      Transport transport = config.getTransport();
      builder.put("client.transport.sniff", transport.getIsSniff());
      builder.put("client.transport.ignore_cluster_name", transport.getIsIgnoreClusterName());
      builder.put("client.transport.ping_timeout", transport.getPingTimeout());
      builder.put("client.transport.nodes_sampler_interval", transport.getNodesSamplerInterval());
    }
    TransportClient client = new TransportClient(builder.build());
    for (Host host : config.getHostList()) {
      try {
        InetAddress ip = InetAddress.getByName(host.getIp());
        client.addTransportAddress(new InetSocketTransportAddress(ip, host.getPort()));
      } catch (UnknownHostException e) {
        log.error("Wrong format of ip address: " + host.getIp());
      }
    }
    return client;
  }

  public static Client getNodeClient(ESConfig config) {
    Preconditions.checkNotNull(config);

    Settings settings = ImmutableSettings.settingsBuilder().put("cloud.aws.access_key", "AKIAI5TDC2FXHBVF6KQQ")
        .put("cloud.aws.secret_key", "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").put("cloud.aws.region", "us-east-1")
        .put("discovery.type", "ec2")
        .put("discovery.ec2.tag." + config.getEc2Tag().getKey(), config.getEc2Tag().getValue()).build();
    Node node = NodeBuilder.nodeBuilder().clusterName(config.getCluster()).client(true).local(false).settings(settings)
        .node();
    return node.client();
  }

  public static IndexRequestBuilder createInputDocument(Client client, DSRecord<Object> record,
      TableSchemaMap indexSchemaMap, String schema, String type, String uniqueKey) {
    Preconditions.checkNotNull(client);
    Preconditions.checkNotNull(record);
    Preconditions.checkNotNull(indexSchemaMap);
    Preconditions.checkArgument(StringUtils.isNotBlank(schema));
    Preconditions.checkArgument(StringUtils.isNotBlank(type));
    Preconditions.checkArgument(StringUtils.isNotBlank(uniqueKey));

    String uniqueKeyValue = null;
    IndexRequestBuilder requestBuilder = null;
    try {
      XContentBuilder builder = jsonBuilder().startObject();
      for (Map.Entry<String, Object> entry : record.getFields()) {
        String field = entry.getKey();
        Object value = entry.getValue();
        if (indexSchemaMap.containsField(field)) {
          if (field.equals(uniqueKey)) {
            uniqueKeyValue = value.toString();
          }
          builder.field(field, value);
        }
      }
      builder.endObject();
      requestBuilder = client.prepareIndex(schema, type, uniqueKeyValue).setSource(builder);
    } catch (IOException e) {
      log.error("Failed to create input document. Error: " + e.getMessage());
    }
    return requestBuilder;
  }

  public static BulkRequestBuilder createInputDocuments(Client client, List<DSRecord<Object>> records,
      TableSchema tableSchema, String schema, String type) {
    Preconditions.checkNotNull(records);
    Preconditions.checkNotNull(tableSchema);

    String uniqueKey = tableSchema.getFieldSchema(tableSchema.getPrimaryKey().getFieldSchemaIndex(0)).getName();
    TableSchemaMap indexSchemaMap = new TableSchemaMap(tableSchema);

    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for (DSRecord<Object> record : records) {
      IndexRequestBuilder request = createInputDocument(client, record, indexSchemaMap, schema, type, uniqueKey);
      if (request != null) {
        bulkRequest.add(request);
      }
    }
    return bulkRequest;
  }

  public static BulkRequestBuilder createDeleteDocuments(Client client, List<String> ids, String schema, String type) {
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for (String id : ids) {
      Preconditions.checkArgument(StringUtils.isNotBlank(id));
      DeleteRequestBuilder request = client.prepareDelete(schema, type, id);
      bulkRequest.add(request);
    }
    return bulkRequest;
  }

}
