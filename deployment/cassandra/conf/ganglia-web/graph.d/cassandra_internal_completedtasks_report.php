<?php

function graph_cassandra_internal_completedtasks_report ( &$rrdtool_graph ) {

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
    $title = 'Internal Completed Tasks Delta';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title'] = $title;
    } else {
       $rrdtool_graph['title'] = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'count/sec';
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

    $timestep = 30;
    // Context is not "host"
    $series =
         "'DEF:AntiEntropyStage=${rrd_dir}/Cassandra_AntiEntropyStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:AntiEntropyStage_delta=AntiEntropyStage,PREV(AntiEntropyStage),-,${timestep},/' "
       . " LINE:AntiEntropyStage_delta#FF0000:'AntiEntropyS...' "
       . " VDEF:AntiEntropySooo_delta_last=AntiEntropyStage_delta,LAST "
       . " GPRINT:'AntiEntropySooo_delta_last':'${space1}Now\:%6.1lf%s' "

       . "'DEF:CommitlogArchiver=${rrd_dir}/Cassandra_commitlog_archiver_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:CommitlogArchiver_delta=CommitlogArchiver,PREV(CommitlogArchiver),-,${timestep},/' "
       . " LINE:CommitlogArchiver_delta#FFA500:'CommitlogArc...' "
       . " VDEF:CommitlogArcooo_delta_last=CommitlogArchiver_delta,LAST "
       . " GPRINT:'CommitlogArcooo_delta_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:FlushWriter=${rrd_dir}/Cassandra_FlushWriter_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:FlushWriter_delta=FlushWriter,PREV(FlushWriter),-,${timestep},/' "
       . " LINE:FlushWriter_delta#FFFF00:'FlushWriter' "
       . " VDEF:FlushWriter_delta_last=FlushWriter_delta,LAST "
       . " GPRINT:'FlushWriter_delta_last':'    ${space1}Now\:%6.1lf%s' "

       . "'DEF:GossipStage=${rrd_dir}/Cassandra_GossipStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:GossipStage_delta=GossipStage,PREV(GossipStage),-,${timestep},/' "
       . " LINE:GossipStage_delta#00FF00:'GossipStage' "
       . " VDEF:GossipStage_delta_last=GossipStage_delta,LAST "
       . " GPRINT:'GossipStage_delta_last':'    ${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:HintedHandoff=${rrd_dir}/Cassandra_HintedHandoff_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:HintedHandoff_delta=HintedHandoff,PREV(HintedHandoff),-,${timestep},/' "
       . " LINE:HintedHandoff_delta#00FFFF:'HintedHandoff' "
       . " VDEF:HintedHandoff_delta_last=HintedHandoff_delta,LAST "
       . " GPRINT:'HintedHandoff_delta_last':'  ${space1}Now\:%6.1lf%s' "

       . "'DEF:InternalResponseStage=${rrd_dir}/Cassandra_InternalResponseStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:InternalResponseStage_delta=InternalResponseStage,PREV(InternalResponseStage),-,${timestep},/' "
       . " LINE:InternalResponseStage_delta#0000FF:'InternalResp...' "
       . " VDEF:InternalRespooo_delta_last=InternalResponseStage_delta,LAST "
       . " GPRINT:'InternalRespooo_delta_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:MemoryMeter=${rrd_dir}/Cassandra_MemoryMeter_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:MemoryMeter_delta=MemoryMeter,PREV(MemoryMeter),-,${timestep},/' "
       . " LINE:MemoryMeter_delta#800080:'MemoryMeter' "
       . " VDEF:MemoryMeter_delta_last=MemoryMeter_delta,LAST "
       . " GPRINT:'MemoryMeter_delta_last':'    ${space1}Now\:%6.1lf%s' "

       . "'DEF:MemtablePostFlusher=${rrd_dir}/Cassandra_MemtablePostFlusher_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:MemtablePostFlusher_delta=MemtablePostFlusher,PREV(MemtablePostFlusher),-,${timestep},/' "
       . " LINE:MemtablePostFlusher_delta#FF00FF:'MemtablePost...' "
       . " VDEF:MemtablePostooo_delta_last=MemtablePostFlusher_delta,LAST "
       . " GPRINT:'MemtablePostooo_delta_last':'${space1}Now\:%6.1lf%s\\l' "

       . "'DEF:MigrationStage=${rrd_dir}/Cassandra_MigrationStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:MigrationStage_delta=MigrationStage,PREV(MigrationStage),-,${timestep},/' "
       . " LINE:MigrationStage_delta#008000:'MigrationStage' "
       . " VDEF:MigrationStage_delta_last=MigrationStage_delta,LAST "
       . " VDEF:MigrationStage_delta_max=MigrationStage_delta,MAXIMUM "
       . " GPRINT:'MigrationStage_delta_last':' ${space1}Now\:%6.1lf%s\\l' ";

    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
