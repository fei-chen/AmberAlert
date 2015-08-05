<?php

function graph_cassandra_internal_pendingtasks_report ( &$rrdtool_graph ) {

// pull in a number of global variables, many set in conf.php (such as colors)
// but other from elsewhere, such as get_context.php

    global $conf,
           $context,
           $range,
           $rrd_dir,
           $size;

    if ($conf['strip_domainname']) {
       $hostname = strip_domainname($GLOBALS['hostname']);
    } else {
       $hostname = $GLOBALS['hostname'];
    }

    //
    // You *MUST* set at least the 'title', 'vertical-label', and 'series'
    // variables otherwise, the graph *will not work*.
    //
    $title = 'Internal Pending Tasks';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title'] = $title;
    } else {
       $rrdtool_graph['title'] = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'count';
    // Fudge to account for number of lines in the chart legend
    $rrdtool_graph['height'] += ($size == 'medium') ? 28 : 0;
    if ( $conf['graphreport_stats'] && $size == 'medium') {
        $rrdtool_graph['height'] -= 31;
        $rrdtool_graph['extras'] .= ' --font LEGEND:7';
    }
    $rrdtool_graph['extras'] .= ' --rigid';
    $rrdtool_graph['lower-limit'] = '0';
    /*
     * Here we actually build the chart series.  This is moderately complicated
     * to show off what you can do.  For a simpler example, look at
     * network_report.php
     */

    // Context is not "host"
    $series =
         "'DEF:AntiEntropyStage=${rrd_dir}/Cassandra_AntiEntropyStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:AntiEntropyStage#FF0000:'AntiEntropyS...' "
       . " VDEF:AntiEntropySooo_last=AntiEntropyStage,LAST "
       . " GPRINT:'AntiEntropySooo_last':'${space1}Now\:%6.1lf%s' "

       . "'DEF:CommitlogArchiver=${rrd_dir}/Cassandra_commitlog_archiver_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:CommitlogArchiver#FFA500:'CommitlogArc...' "
       . " VDEF:CommitlogArcooo_last=CommitlogArchiver,LAST "
       . " GPRINT:'CommitlogArcooo_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:FlushWriter=${rrd_dir}/Cassandra_FlushWriter_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:FlushWriter#FFFF00:'FlushWriter' "
       . " VDEF:FlushWriter_last=FlushWriter,LAST "
       . " GPRINT:'FlushWriter_last':'    ${space1}Now\:%6.1lf%s' "

       . "'DEF:GossipStage=${rrd_dir}/Cassandra_GossipStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:GossipStage#00FF00:'GossipStage' "
       . " VDEF:GossipStage_last=GossipStage,LAST "
       . " GPRINT:'GossipStage_last':'    ${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:HintedHandoff=${rrd_dir}/Cassandra_HintedHandoff_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:HintedHandoff#00FFFF:'HintedHandoff' "
       . " VDEF:HintedHandoff_last=HintedHandoff,LAST "
       . " GPRINT:'HintedHandoff_last':'  ${space1}Now\:%6.1lf%s' "

       . "'DEF:InternalResponseStage=${rrd_dir}/Cassandra_InternalResponseStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:InternalResponseStage#0000FF:'InternalResp...' "
       . " VDEF:InternalRespooo_last=InternalResponseStage,LAST "
       . " GPRINT:'InternalRespooo_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:MemoryMeter=${rrd_dir}/Cassandra_MemoryMeter_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:MemoryMeter#800080:'MemoryMeter' "
       . " VDEF:MemoryMeter_last=MemoryMeter,LAST "
       . " GPRINT:'MemoryMeter_last':'    ${space1}Now\:%6.1lf%s' "

       . "'DEF:MemtablePostFlusher=${rrd_dir}/Cassandra_MemtablePostFlusher_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:MemtablePostFlusher#FF00FF:'MemtablePost...' "
       . " VDEF:MemtablePostooo_last=MemtablePostFlusher,LAST "
       . " GPRINT:'MemtablePostooo_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:MigrationStage=${rrd_dir}/Cassandra_MigrationStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:MigrationStage#008000:'MigrationStage' "
       . " VDEF:MigrationStage_last=MigrationStage,LAST "
       . " VDEF:MigrationStage_max=MigrationStage,MAXIMUM "
       . " GPRINT:'MigrationStage_last':' ${space1}Now\:%6.1lf%s\\l' ";

    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
