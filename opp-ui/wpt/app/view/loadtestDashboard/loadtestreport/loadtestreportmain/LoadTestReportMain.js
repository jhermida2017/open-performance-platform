
Ext.define('OppUI.view.loadTestDashboard.loadtestreport.loadtestreportmain.LoadTestReportMain',{
    extend: 'Ext.panel.Panel',
    alias: 'widget.loadtestreportmain',

    requires: [
        'OppUI.view.loadTestDashboard.loadtestreport.loadtestreportmain.LoadTestReportMainController',
        'OppUI.view.loadTestDashboard.loadtestreport.loadtestreportmain.LoadTestReportMainModel'
    ],

    initComponent: function() {
        var timeSeriesYAxes, aggregateYAxes, view, i;

        timeSeriesYAxes = this.up('loadtestreport').getChartTimeSeriesYAxes();
        aggregateYAxes = this.up('loadtestreport').getChartAggregateYAxes();

        /**
         * Build all the charts.
         */
        for(i = 0; i < timeSeriesYAxes.length; i++) {
            this.items.push({
                xtype: 'loadtestchart',
                itemId: 'timeseries-' + timeSeriesYAxes[i].yaxis,
                title: timeSeriesYAxes[i].title,
                colspan: 2,
                height: 300,
                margin: '0 0 20 0',
                tbar: [
                    '->',
                    {
                        xtype: 'timeseriescombobox'
                    },
                    '-',
                    {
                        text: 'Undo Zoom',
                        handler: function() {
                            var chart = this.up('panel').down('cartesian'),
                                interaction = chart && Ext.ComponentQuery.query('interaction', chart)[0],
                                undoButton = interaction && interaction.getUndoButton(),
                                handler = undoButton && undoButton.handler;
                            if (handler) {
                                handler();
                            }
                        }
                    }
                ]
            });
        }

        for(i = 0; i < aggregateYAxes.length; i++) {
            this.items.push({
                xtype: 'loadtestchart',
                itemId: 'agg-' + aggregateYAxes[i].yaxis,
                title: aggregateYAxes[i].title,
                colspan: 1,
                height: 300,
                margin: '0 0 20 0'
            });
        }
        
        this.callParent(arguments);
    },

    controller: 'loadtestreportmain',
    viewModel: {
        type: 'loadtestreportmain'
    },

    layout: {
        type: 'table',
        columns: 2,
        tableAttrs: {
            style: {
                width: '100%'
            }
        }
    },
    style: {
        overflow: 'auto'
    },
    defaults: {
        padding: 7,
        height: 300
    },
    margins: '5 0 0 0',

    items:[
        {
            xtype: 'panel',
            colspan: 2,
            height: 110,
            margin: '0 0 20 0',
            tpl: [
                '<table class=\'tbl-load-test-details\'>',
                    '<tr>',
                        '<th>Test Name:</th>',
                        '<td>{testName}</td>',
                        '<th class=\'col2\'>Date:</th>',
                        '<td class=\'col2\'>{curTime}</td>',
                        '<th class=\'col2\'>Duration:</th>',
                        '<td class=\'col2\'>{duration}</td>',
                    '</tr>',
                    '<tr>',
                        '<th>Vusers:</th>',
                        '<td>{vuserCount}</td>',
                        '<th class=\'col2\'>Env.:</th>',
                        '<td class=\'col2\' colspan=\'2\'>{environment}</td>',
                    '</tr>',
                    '<tr>',
                        '<th>Description:</td>',
                        '<td colspan=\'4\'>{description}</td>',
                    '</tr>',
                    '<tr>',
                        '<th>CloudTest Result:</td>',
                        '<td colspan=\'3\'><a href=\'{cloudTestLink}\' target=\'_blank\'>{cloudTestLink}</a></td>',
                    '</tr>',
                '</table>'
            ],

            bind: {
                data: {
                    testName: '{testName}',
                    curTime: '{curTime}',
                    duration: '{duration}',
                    vuserCount: '{vuserCount}',
                    environment: '{environment}',
                    description: '{description}',
                    cloudTestLink: '{cloudTestLink}'
                }   
            }
        },
        { 
            xtype: 'loadtestreportsummary',
            height: 300,
            margin: '0px 0px 20px 0px',
            colspan: 2
        }
    ]
});