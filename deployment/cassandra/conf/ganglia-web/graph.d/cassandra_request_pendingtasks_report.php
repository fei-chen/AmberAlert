<?php

function graph_cassandra_request_pendingtasks_report ( &$rrdtool_graph ) {

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
    $title = 'Request Pending Tasks';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title']  = $title;
    } else {
       $rrdtool_graph['title']  = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'count';
    // Fudge to account for number of lines in the chart legend
    $rrdtool_graph['height']        += ($size == 'medium') ? 28 : 0;
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
         "'DEF:MutationStage=${rrd_dir}/Cassandra_MutationStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:MutationStage#FF0000:'MutationStage' "
       . " VDEF:MutationStage_last=MutationStage,LAST "
       . " VDEF:MutationStage_max=MutationStage,MAXIMUM "
       . " GPRINT:'MutationStage_last':'        ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'MutationStage_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReadRepairStage=${rrd_dir}/Cassandra_ReadRepairStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:ReadRepairStage#FFA500:'ReadRepairStage' "
       . " VDEF:ReadRepairStage_last=ReadRepairStage,LAST "
       . " VDEF:ReadRepairStage_max=ReadRepairStage,MAXIMUM "
       . " GPRINT:'ReadRepairStage_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReadRepairStage_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReadStage=${rrd_dir}/Cassandra_ReadStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:ReadStage#FF00FF:'ReadStage' "
       . " VDEF:ReadStage_last=ReadStage,LAST "
       . " VDEF:ReadStage_max=ReadStage,MAXIMUM "
       . " GPRINT:'ReadStage_last':'            ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReadStage_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReplicateOnWriteStage=${rrd_dir}/Cassandra_ReplicateOnWriteStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:ReplicateOnWriteStage#00FF00:'ReplicateOnWriteStage' "
       . " VDEF:ReplicateOnWriteStage_last=ReplicateOnWriteStage,LAST "
       . " VDEF:ReplicateOnWriteStage_max=ReplicateOnWriteStage,MAXIMUM "
       . " GPRINT:'ReplicateOnWriteStage_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReplicateOnWriteStage_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:RequestResponseStage=${rrd_dir}/Cassandra_RequestResponseStage_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:RequestResponseStage#0000FF:'RequestResponseStage' "
       . " VDEF:RequestResponseStage_last=RequestResponseStage,LAST "
       . " VDEF:RequestResponseStage_max=RequestResponseStage,MAXIMUM "
       . " GPRINT:'RequestResponseStage_last':' ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'RequestResponseStage_max':'${space1}Max\:%6.1lf%s\\l' ";
    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
