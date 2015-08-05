<?php

function graph_cassandra_latency_min_report ( &$rrdtool_graph ) {

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
    $title = 'Latency Median & Min';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title'] = $title;
    } else {
       $rrdtool_graph['title'] = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'microseconds';
    // Fudge to account for number of lines in the chart legend
    $rrdtool_graph['height'] += ($size == 'medium') ? 28 : 0;
    if ( $conf['graphreport_stats'] && $size == 'medium') {
        $rrdtool_graph['height'] -= 43;
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
         "'DEF:Read50=${rrd_dir}/Cassandra_ClientRequest_Read_50thPercentile.rrd:sum:AVERAGE' "
       . "'DEF:Write50=${rrd_dir}/Cassandra_ClientRequest_Write_50thPercentile.rrd:sum:AVERAGE' "
       . "'DEF:RangeSlice50=${rrd_dir}/Cassandra_ClientRequest_RangeSlice_50thPercentile.rrd:sum:AVERAGE' "
       . "'DEF:ReadMin=${rrd_dir}/Cassandra_ClientRequest_Read_Min.rrd:sum:AVERAGE' "
       . "'DEF:WriteMin=${rrd_dir}/Cassandra_ClientRequest_Write_Min.rrd:sum:AVERAGE' "
       . "'DEF:RangeSliceMin=${rrd_dir}/Cassandra_ClientRequest_RangeSlice_Min.rrd:sum:AVERAGE' "
       ;

    if ($context != "host" ) {
        $series .= 
              "DEF:'num_nodes'='${rrd_dir}/Cassandra_ClientRequest_Read_50thPercentile.rrd':'num':AVERAGE "
            . "CDEF:'_Read50'=Read50,num_nodes,/ "
            . "CDEF:'_Write50'=Write50,num_nodes,/ "
            . "CDEF:'_RangeSlice50'=RangeSlice50,num_nodes,/ "
            . "CDEF:'_ReadMin'=ReadMin,num_nodes,/ "
            . "CDEF:'_WriteMin'=WriteMin,num_nodes,/ "
            . "CDEF:'_RangeSliceMin'=RangeSliceMin,num_nodes,/ "
            ;
    } else {
        $series .= 
              "CDEF:'_Read50'=Read50 "
            . "CDEF:'_Write50'=Write50 "
            . "CDEF:'_RangeSlice50'=RangeSlice50 "
            . "CDEF:'_ReadMin'=ReadMin "
            . "CDEF:'_WriteMin'=WriteMin "
            . "CDEF:'_RangeSliceMin'=RangeSliceMin ";
    }
    $series .= 
         " LINE2:_Read50#00FF00:'Read med' "
       . " VDEF:_Read50_last=_Read50,LAST "
       . " VDEF:_Read50_max=_Read50,MAXIMUM "
       . " GPRINT:'_Read50_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_Read50_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE2:_Write50#3333bb:'Write med' "
       . " VDEF:_Write50_last=_Write50,LAST "
       . " VDEF:_Write50_max=_Write50,MAXIMUM "
       . " GPRINT:'_Write50_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_Write50_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE2:_RangeSlice50#FF0000:'RangeSlice med' "
       . " VDEF:_RangeSlice50_last=_RangeSlice50,LAST "
       . " VDEF:_RangeSlice50_max=_RangeSlice50,MAXIMUM "
       . " GPRINT:'_RangeSlice50_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_RangeSlice50_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE:_ReadMin#008000:'Read min' "
       . " VDEF:_ReadMin_last=_ReadMin,LAST "
       . " VDEF:_ReadMin_min=_ReadMin,MINIMUM "
       . " GPRINT:'_ReadMin_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_ReadMin_min':'${space1}Min\:%6.1lf%s\\l' "

       . " LINE:_WriteMin#00FFFF:'Write min' "
       . " VDEF:_WriteMin_last=_WriteMin,LAST "
       . " VDEF:_WriteMin_min=_WriteMin,MINIMUM "
       . " GPRINT:'_WriteMin_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_WriteMin_min':'${space1}Min\:%6.1lf%s\\l' "

       . " LINE:_RangeSliceMin#FF00FF:'RangeSlice min' "
       . " VDEF:_RangeSliceMin_last=_RangeSliceMin,LAST "
       . " VDEF:_RangeSliceMin_min=_RangeSliceMin,MINIMUM "
       . " GPRINT:'_RangeSliceMin_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_RangeSliceMin_min':'${space1}Min\:%6.1lf%s\\l' "
       ;

    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
