Ext.define('OppUI.model.loadtestDashboard.LoadTestGroup', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'loadTestId', mapping: 'summaryTrend[0].loadTestId'},
        {name: 'appUnderTest', mapping: 'summaryTrend[0].appUnderTest'},
        {name: 'appUnderTestVersion', mapping: 'summaryTrend[0].appUnderTestVersion'},
        {name: 'comments', mapping: 'summaryTrend[0].comments'},
        {name: 'description', mapping: 'summaryTrend[0].description'},
        {name: 'environment', mapping: 'summaryTrend[0].environment'},
        {name: 'startTime', type: 'date', dateFormat: 'timestamp', mapping: 'summaryTrend[0].startTime'},
        {name: 'testName', mapping: 'summaryTrend[0].testName'},
        {name: 'testSubName', mapping: 'summaryTrend[0].testSubName'},
        {name: 'testTool', mapping: 'summaryTrend[0].testTool'},
        {name: 'testToolVersion', mapping: 'summaryTrend[0].testToolVersion'},
        {name: 'vuserCount', mapping: 'summaryTrend[0].vuserCount'},
        {name: 'transactionName', mapping: 'summaryTrend[0].transactionName'},
        {name: 'totalCallCount', mapping: 'summaryTrend[0].totalCallCount'},
        {name: 'respAvg', mapping: 'summaryTrend[0].respAvg'},
        {name: 'respMedian', mapping: 'summaryTrend[0].respMedian'},
        {name: 'respPct90', mapping: 'summaryTrend[0].respPct90'},
        {name: 'tpsMedian', mapping: 'summaryTrend[0].tpsMedian'},
        {name: 'tpsMax', mapping: 'summaryTrend[0].tpsMax'},
        {name: 'totalBytes', mapping: 'summaryTrend[0].totalBytes'},
        {name: 'totalCallCountTrend', mapping: 'summaryTrend[0].totalCallCountTrend'},
        {name: 'respAvgTrend', mapping: 'summaryTrend[0].respAvgTrend'},
        {name: 'respMedianTrend', mapping: 'summaryTrend[0].respMedianTrend'},
        {name: 'respPct90Trend', mapping: 'summaryTrend[0].respPct90Trend'},
        {name: 'tpsMedianTrend', mapping: 'summaryTrend[0].tpsMedianTrend'},
        {name: 'tpsMaxTrend', mapping: 'summaryTrend[0].tpsMaxTrend'},
        {name: 'totalBytesTrend', mapping: 'summaryTrend[0].totalBytesTrend'},
        {name: 'sparkline90'},
        {name: 'sparkline50'}
     ]
});