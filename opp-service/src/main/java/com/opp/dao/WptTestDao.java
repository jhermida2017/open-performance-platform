package com.opp.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.opp.config.ScheduledTasks;
import com.opp.domain.ux.WptResult;
import com.opp.domain.ux.WptTestLabel;
import com.opp.domain.ux.WptUINavigation;
import com.opp.dto.ux.WptTestRunData;
import com.opp.dto.ux.WptTrendChart;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static com.opp.dao.util.SelectUtils.getOptional;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by ctobe on 3/22/17.
 */
@Component
public class WptTestDao {


    @Value("${opp.elasticsearch.wptData.index}")
    private String wptEsIndex;

    @Value("${opp.elasticsearch.wptData.indexVersion}")
    private String wptEsIndexVersion;

    @Value("${opp.elasticsearch.wptData.type}")
    private String wptEsType;

    @Value("${opp.elasticsearch.wptData.fetchLimit}")
    private Integer wptEsFetchLimit;

    @Value("${opp.elasticsearch.apiUrl}")
    private String esApiUrl;

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);



    /**
     * Add new WPT Test
     * @param wptResult
     * @return
     */
    public Optional<IndexResponse> insert(WptResult wptResult) {
        return getOptional(() -> {
            try {
                return esClient.prepareIndex(wptEsIndex, wptEsType, wptResult.getId())
                        .setSource(objectMapper.writeValueAsString(wptResult))
                        .get();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Update WPT Test
     * @param wptResult
     */
    public Optional<UpdateResponse> update(WptResult wptResult) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(wptEsIndex);
        updateRequest.type(wptEsType);
        updateRequest.id(wptResult.getId());
        try {
            updateRequest.doc(objectMapper.writeValueAsString(wptResult));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        return getOptional(() -> {
            try {
                return esClient.update(updateRequest).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        });

    }

    /**
     * Get WPT Test by id
     * @param wptTestId
     * @return
     */
    public Optional<WptResult> findById(String wptTestId){
        GetResponse response = esClient.prepareGet(wptEsIndex, wptEsType, wptTestId).get();
        if(response.isSourceEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(objectMapper.convertValue(response.getSource(), WptResult.class));
        }
    }
    

    /**
     * Find all WPT Tests
     * @return
     */
    public List<WptResult> findAll(String esSearchQuery) {
        // need to add start and end dates to avoid nulls in the data or bad records
        long startTime = ZonedDateTime.now(ZoneOffset.UTC).minusYears(5).toEpochSecond(); // default to 5 years
        long endTime = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();

        SearchHit[] hits = esClient.prepareSearch(wptEsIndex).setTypes(wptEsType)
                .setQuery(QueryBuilders.boolQuery()
                                .must(QueryBuilders.rangeQuery("completed").gte(startTime).lte(endTime).format("epoch_second"))
                                .must(QueryBuilders.queryStringQuery(esSearchQuery).analyzeWildcard(true))
                )
                .setSize(wptEsFetchLimit).get().getHits().getHits();
        return Arrays.stream(hits)
                .map(h -> objectMapper.convertValue(h.getSource(), WptResult.class))
                .collect(toList());
    }


    /**
     * Find all WPT Tests
     * @return
     */
    public List<WptResult> findByLabel(String name) {
        SearchResponse resp = esClient.prepareSearch(wptEsIndex).setTypes(wptEsType)
                .setQuery(QueryBuilders.termQuery("label.full", name))
                //.setSource(SearchSourceBuilder.searchSource().fetchSource(new String[]{"id"}, new String[]{}))
                .get();
        return Arrays.stream(resp.getHits().getHits()).map(WptResult.class::cast).collect(toList());
    }



    /**
     * Delete WPT Test by id
     * Returns 1 if deleted and 0 if not
     * @param id
     */
    public int deleteById(String id) {
        DeleteResponse response = esClient.prepareDelete(wptEsIndex, wptEsType, id).get();
        return (response.getResult().toString().equals("deleted")) ? 1 : 0;
    }


    /**
     * Gets the navigation object used by the UX filters
     * @return
     */
    public List<WptUINavigation> getNavigation() {
        SearchResponse resp = esClient.prepareSearch(wptEsIndex).setTypes(wptEsType)
                .addAggregation(AggregationBuilders.terms("agg").field("label.full.keyword").size(2000).order(Terms.Order.term(true))
                        .subAggregation(AggregationBuilders.max("max").field("completed"))).execute().actionGet();
        Terms terms = resp.getAggregations().get("agg");
        return terms.getBuckets().stream().map(b -> {
            Max max = b.getAggregations().get("max");
            return new WptUINavigation(b.getKeyAsString(), (new Double(max.getValue()).longValue()));
        }).collect(toList());

    }

    public List<WptTestRunData> getTrendTableData(String testName, String view, String runDuration){

        SearchResponse resp = esClient.prepareSearch(wptEsIndex).setTypes(wptEsType)
                .setSource(SearchSourceBuilder.searchSource().fetchSource(new String[] { "completed", "label", "runCount", runDuration + "."+view+".TTFB", runDuration + "."+view+".SpeedIndex", runDuration + "."+view+".visualComplete" }, new String[] {}))
                .setQuery(QueryBuilders.termQuery("label.full.keyword", testName).queryName("label")).get();

        return Arrays.stream(resp.getHits().getHits()).map(h -> {
            JsonNode sourceNode = objectMapper.convertValue(h.getSource(), JsonNode.class); // easier parsing than traversing multiple levels of maps
            JsonNode viewNode = sourceNode.path(runDuration).path(view);
            return new WptTestRunData(
                    sourceNode.get("completed").asLong(),
                    objectMapper.convertValue(h.getSource().get("label"), WptTestLabel.class),
                    h.getId(),
                    sourceNode.get("summary").asText(),
                    (viewNode.get("TTFB") != null) ? viewNode.get("TTFB").asInt() : null,
                    (viewNode.get("visualComplete") != null) ? viewNode.get("visualComplete").asInt() : null,
                    (viewNode.get("SpeedIndex") != null) ? viewNode.get("SpeedIndex").asInt() : null,
                    (viewNode.get("aft") != null) ? viewNode.get("aft").asInt() : null
            );
        }).collect(toList());
    }


    public WptTrendChart getTrendChartData(String testName, String view, boolean isUserTimingBaseLine, String interval){

        SearchResponse resp = esClient.prepareSearch(wptEsIndex).setTypes(wptEsType)
                .setSize(0)
                .addAggregation(AggregationBuilders.dateHistogram("histogram").field("completed").dateHistogramInterval(new DateHistogramInterval(interval)).minDocCount(1)
                        .subAggregation(AggregationBuilders.avg("ttfb.min").field("min."+view+".TTFB"))
                        .subAggregation(AggregationBuilders.avg("ttfb.max").field("max."+view+".TTFB"))
                        .subAggregation(AggregationBuilders.avg("ttfb.median").field("median."+view+".TTFB"))
                        .subAggregation(AggregationBuilders.avg("ttfb.average").field("average."+view+".TTFB"))
                        .subAggregation(AggregationBuilders.avg("speedIndex.min").field("min."+view+".SpeedIndex"))
                        .subAggregation(AggregationBuilders.avg("speedIndex.max").field("max."+view+".SpeedIndex"))
                        .subAggregation(AggregationBuilders.avg("speedIndex.median").field("median."+view+".SpeedIndex"))
                        .subAggregation(AggregationBuilders.avg("speedIndex.average").field("average."+view+".SpeedIndex"))
                        .subAggregation(AggregationBuilders.avg("visuallyComplete.min").field("min."+view+".visualComplete"))
                        .subAggregation(AggregationBuilders.avg("visuallyComplete.max").field("max."+view+".visualComplete"))
                        .subAggregation(AggregationBuilders.avg("visuallyComplete.median").field("median."+view+".visualComplete"))
                        .subAggregation(AggregationBuilders.avg("visuallyComplete.average").field("average."+view+".visualComplete"))
                )
                .setQuery(QueryBuilders.termQuery("label.full.keyword", testName).queryName("label")).get();

        Histogram bucket = resp.getAggregations().get("histogram");

        // get hashmap of all metrics to capture (just a dynamic way of doing this to later add more metrics)
        List<String> rangeMetricsToCapture = Arrays.asList("ttfb", "speedIndex", "visuallyComplete");
        Map<String, List<WptTrendChart.BasicMetric>> rangeMetrics = rangeMetricsToCapture.stream()
               .collect(toMap(Function.identity(), metric -> bucket.getBuckets().stream()
                       .map(b -> {
                            // get metrics
                            Double min = ((Avg) b.getAggregations().get(metric + ".min")).getValue();
                            Double max = ((Avg) b.getAggregations().get(metric + ".max")).getValue();
                            Double median = ((Avg) b.getAggregations().get(metric + ".median")).getValue();
                            Double average = ((Avg) b.getAggregations().get(metric + ".average")).getValue();
                            return new WptTrendChart.BasicMetric(Long.valueOf(b.getKeyAsString()), min.intValue(), max.intValue(), median.intValue(), average);
        }).collect(toList())));

        return new WptTrendChart(rangeMetrics.get("ttfb"), rangeMetrics.get("visuallyComplete"), rangeMetrics.get("speedIndex"), new ArrayList<>());
    }


    public long deleteBadRuns(){
        BulkIndexByScrollResponse resp = DeleteByQueryAction.INSTANCE.newRequestBuilder(esClient)
                .filter(QueryBuilders.queryStringQuery("median.firstView.VisuallyComplete:0 OR median.firstView.TTFB:0 OR median.firstView.SpeedIndex:0"))
                .source("persons")
                .get();

        return resp.getDeleted();
    }

    /**
     * Gets cluster metadata info
     * @return
     */
    public MetaData getClusterMetaData() {
        return esClient.admin().cluster().prepareState().execute()
                .actionGet().getState()
                .getMetaData();

    }

    /**
     * Adds an index
     * @param indexName
     * @param indexJson
     * @return
     */
    public boolean addIndex(String indexName, String indexJson){
        // add index and alias
        try {
            HttpResponse<com.mashape.unirest.http.JsonNode> indexResp = Unirest.put(esApiUrl + "/" + indexName).body(indexJson).asJson();
            System.out.println("Creating index: " + indexName);
            System.out.println(indexJson);
            System.out.println("Response from index creation: " + indexResp.getStatusText());
            return (indexResp.getStatus() == 200);
        } catch (UnirestException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Adds all aliases
     * @param aliasesJson
     * @return
     */
    public boolean addAliases(String aliasesJson){
        // add index and alias
        try {
            HttpResponse<com.mashape.unirest.http.JsonNode> aliasResp = Unirest.post(esApiUrl + "/_aliases").body(aliasesJson).asJson();
            System.out.println("Response from alias creation: " + aliasResp.getStatusText());
            return (aliasResp.getStatus() == 200);
        } catch (UnirestException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initializes the WPT Summary index
     * @return
     */
    public boolean initWptSummaryIndex(){
        MetaData metaData = getClusterMetaData();
         if(!metaData.hasAlias(wptEsIndex)) {

             // get resources
             URL index = Resources.getResource("elasticsearch/wptIndex.json");
             URL alias = Resources.getResource("elasticsearch/aliases.json");

             String indexJson = null;
             String aliasJson = null;
             try {
                 indexJson = Resources.toString(index, Charsets.UTF_8);
                 aliasJson = Resources.toString(alias, Charsets.UTF_8);
             } catch (IOException e) {
                 log.error("Failed to get index and/or aliases json");
                 e.printStackTrace();
                 return false;
             }

             // add wpt-summary index
             if (!addIndex(wptEsIndex + "-" + wptEsIndexVersion, indexJson)) {
                 log.error("Failed to add index");
                 return false;
             }
             // add aliases
             if (!addAliases(aliasJson)) {
                 log.error("Failed to add aliases");
                 return false;
             }
         } else {
             log.debug("ES is already initialized");
         }


        return true;
    }
}
