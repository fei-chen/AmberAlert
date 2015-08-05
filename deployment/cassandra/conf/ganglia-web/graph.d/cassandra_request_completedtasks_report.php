<?php

function graph_cassandra_request_completedtasks_report ( &$rrdtool_graph ) {

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
    $title = 'Request Completed Tasks Delta';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title']  = $title;
    } else {
       $rrdtool_graph['title']  = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'count/sec';
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

    $timestep=30;
    // Context is not "host"
    $series =
         "'DEF:MutationStage=${rrd_dir}/Cassandra_MutationStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:MutationStage_delta=MutationStage,PREV(MutationStage),-,${timestep},/' "
       . " LINE:MutationStage_delta#FF0000:'MutationStage_delta' "
       . " VDEF:MutationStage_delta_last=MutationStage_delta,LAST "
       . " VDEF:MutationStage_delta_max=MutationStage_delta,MAXIMUM "
       . " GPRINT:'MutationStage_delta_last':'        ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'MutationStage_delta_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReadRepairStage=${rrd_dir}/Cassandra_ReadRepairStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:ReadRepairStage_delta=ReadRepairStage,PREV(ReadRepairStage),-,${timestep},/' "
       . " LINE:ReadRepairStage_delta#FFA500:'ReadRepairStage_delta' "
       . " VDEF:ReadRepairStage_delta_last=ReadRepairStage_delta,LAST "
       . " VDEF:ReadRepairStage_delta_max=ReadRepairStage_delta,MAXIMUM "
       . " GPRINT:'ReadRepairStage_delta_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReadRepairStage_delta_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReadStage=${rrd_dir}/Cassandra_ReadStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:ReadStage_delta=ReadStage,PREV(ReadStage),-,${timestep},/' "
       . " LINE:ReadStage_delta#FF00FF:'ReadStage_delta' "
       . " VDEF:ReadStage_delta_last=ReadStage_delta,LAST "
       . " VDEF:ReadStage_delta_max=ReadStage_delta,MAXIMUM "
       . " GPRINT:'ReadStage_delta_last':'            ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReadStage_delta_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:ReplicateOnWriteStage=${rrd_dir}/Cassandra_ReplicateOnWriteStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:ReplicateOnWriteStage_delta=ReplicateOnWriteStage,PREV(ReplicateOnWriteStage),-,${timestep},/' "
       . " LINE:ReplicateOnWriteStage_delta#00FF00:'ReplicateOnWriteStage_delta' "
       . " VDEF:ReplicateOnWriteStage_delta_last=ReplicateOnWriteStage_delta,LAST "
       . " VDEF:ReplicateOnWriteStage_delta_max=ReplicateOnWriteStage_delta,MAXIMUM "
       . " GPRINT:'ReplicateOnWriteStage_delta_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'ReplicateOnWriteStage_delta_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:RequestResponseStage=${rrd_dir}/Cassandra_RequestResponseStage_CompletedTasks.rrd:sum:AVERAGE:step=${timestep}' "
       . "'CDEF:RequestResponseStage_delta=RequestResponseStage,PREV(RequestResponseStage),-,${timestep},/' "
       . " LINE:RequestResponseStage_delta#0000FF:'RequestResponseStage_delta' "
       . " VDEF:RequestResponseStage_delta_last=RequestResponseStage_delta,LAST "
       . " VDEF:RequestResponseStage_delta_max=RequestResponseStage_delta,MAXIMUM "
       . " GPRINT:'RequestResponseStage_delta_last':' ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'RequestResponseStage_delta_max':'${space1}Max\:%6.1lf%s\\l' ";
    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
