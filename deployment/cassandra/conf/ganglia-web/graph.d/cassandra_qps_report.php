<?php

function graph_cassandra_qps_report ( &$rrdtool_graph ) {

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
    $title = 'QPS';
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
        $rrdtool_graph['height'] -= 7;
        $rrdtool_graph['extras'] .= ' --font LEGEND:7';
    }
    $rrdtool_graph['extras'] .= ' --rigid';
    //$rrdtool_graph['lower-limit'] = '0';
    /*
     * Here we actually build the chart series.  This is moderately complicated
     * to show off what you can do.  For a simpler example, look at
     * network_report.php
     */

    // Context is not "host"
    $series =
         "'DEF:Read=${rrd_dir}/Cassandra_ClientRequest_Read_OneMinuteRate.rrd:sum:AVERAGE' "
       . " LINE:Read#00FF00:'Read' "
       . " VDEF:Read_last=Read,LAST "
       . " VDEF:Read_max=Read,MAXIMUM "
       . " GPRINT:'Read_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'Read_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:Write=${rrd_dir}/Cassandra_ClientRequest_Write_OneMinuteRate.rrd:sum:AVERAGE' "
       . " LINE:Write#3333bb:'Write' "
       . " VDEF:Write_last=Write,LAST "
       . " VDEF:Write_max=Write,MAXIMUM "
       . " GPRINT:'Write_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'Write_max':'${space1}Max\:%6.1lf%s\\l' "

       . "'DEF:RangeSlice=${rrd_dir}/Cassandra_ClientRequest_RangeSlice_OneMinuteRate.rrd:sum:AVERAGE' "
       . " LINE:RangeSlice#FF0000:'RangeSlice' "
       . " VDEF:RangeSlice_last=RangeSlice,LAST "
       . " VDEF:RangeSlice_max=RangeSlice,MAXIMUM "
       . " GPRINT:'RangeSlice_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'RangeSlice_max':'${space1}Max\:%6.1lf%s\\l' ";

    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
