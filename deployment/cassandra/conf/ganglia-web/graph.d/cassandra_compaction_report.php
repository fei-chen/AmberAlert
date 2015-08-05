<?php

function graph_cassandra_compaction_report ( &$rrdtool_graph ) {

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
    $title = 'Compaction';
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
        $rrdtool_graph['height'] += 14;
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
         "'DEF:pending_tasks=${rrd_dir}/Cassandra_CompactionManager_PendingTasks.rrd:sum:AVERAGE' "
       . " LINE:pending_tasks#3333bb:'Pending Tasks' "
       . " VDEF:pending_tasks_last=pending_tasks,LAST "
       . " VDEF:pending_tasks_max=pending_tasks,MAXIMUM "
       . " GPRINT:'pending_tasks_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'pending_tasks_max':'${space1}Max\:%6.1lf%s\\l' ";

    $rrdtool_graph['series'] = $series;
    
    return $rrdtool_graph;
}

?>
