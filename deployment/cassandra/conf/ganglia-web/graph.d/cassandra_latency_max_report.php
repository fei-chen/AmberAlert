<?php

function graph_cassandra_latency_max_report ( &$rrdtool_graph ) {

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
    $title = 'Latency Max & 95%';
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
         "'DEF:ReadMax=${rrd_dir}/Cassandra_ClientRequest_Read_Max.rrd:sum:AVERAGE' "
       . "'DEF:WriteMax=${rrd_dir}/Cassandra_ClientRequest_Write_Max.rrd:sum:AVERAGE' "
       . "'DEF:RangeSliceMax=${rrd_dir}/Cassandra_ClientRequest_RangeSlice_Max.rrd:sum:AVERAGE' "
       . "'DEF:Read95=${rrd_dir}/Cassandra_ClientRequest_Read_95thPercentile.rrd:sum:AVERAGE' "
       . "'DEF:Write95=${rrd_dir}/Cassandra_ClientRequest_Write_95thPercentile.rrd:sum:AVERAGE' "
       . "'DEF:RangeSlice95=${rrd_dir}/Cassandra_ClientRequest_RangeSlice_95thPercentile.rrd:sum:AVERAGE' "
       ;

    if ($context != "host" ) {
        $series .= 
              "DEF:'num_nodes'='${rrd_dir}/Cassandra_ClientRequest_Read_95thPercentile.rrd':'num':AVERAGE "
            . "CDEF:'_ReadMax'=ReadMax,num_nodes,/ "
            . "CDEF:'_WriteMax'=WriteMax,num_nodes,/ "
            . "CDEF:'_RangeSliceMax'=RangeSliceMax,num_nodes,/ "
            . "CDEF:'_Read95'=Read95,num_nodes,/ "
            . "CDEF:'_Write95'=Write95,num_nodes,/ "
            . "CDEF:'_RangeSlice95'=RangeSlice95,num_nodes,/ "
            ;
    } else {
        $series .= 
              "CDEF:'_ReadMax'=ReadMax "
            . "CDEF:'_WriteMax'=WriteMax "
            . "CDEF:'_RangeSliceMax'=RangeSliceMax "
            . "CDEF:'_Read95'=Read95 "
            . "CDEF:'_Write95'=Write95 "
            . "CDEF:'_RangeSlice95'=RangeSlice95 "
            ;
    }
    $series .= 
         " LINE2:_ReadMax#00FF00:'Read max' "
       . " VDEF:_ReadMax_last=_ReadMax,LAST "
       . " VDEF:_ReadMax_max=_ReadMax,MINIMUM "
       . " GPRINT:'_ReadMax_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_ReadMax_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE2:_WriteMax#3333bb:'Write max' "
       . " VDEF:_WriteMax_last=_WriteMax,LAST "
       . " VDEF:_WriteMax_max=_WriteMax,MINIMUM "
       . " GPRINT:'_WriteMax_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_WriteMax_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE2:_RangeSliceMax#FF0000:'RangeSlice max' "
       . " VDEF:_RangeSliceMax_last=_RangeSliceMax,LAST "
       . " VDEF:_RangeSliceMax_max=_RangeSliceMax,MINIMUM "
       . " GPRINT:'_RangeSliceMax_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_RangeSliceMax_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE:_Read95#008000:'Read 95%' "
       . " VDEF:_Read95_last=_Read95,LAST "
       . " VDEF:_Read95_max=_Read95,MAXIMUM "
       . " GPRINT:'_Read95_last':'      ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_Read95_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE:_Write95#00FFFF:'Write 95%' "
       . " VDEF:_Write95_last=_Write95,LAST "
       . " VDEF:_Write95_max=_Write95,MAXIMUM "
       . " GPRINT:'_Write95_last':'     ${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_Write95_max':'${space1}Max\:%6.1lf%s\\l' "

       . " LINE:_RangeSlice95#FF00FF:'RangeSlice 95%' "
       . " VDEF:_RangeSlice95_last=_RangeSlice95,LAST "
       . " VDEF:_RangeSlice95_max=_RangeSlice95,MAXIMUM "
       . " GPRINT:'_RangeSlice95_last':'${space1}Now\:%6.1lf%s' "
       . " GPRINT:'_RangeSlice95_max':'${space1}Max\:%6.1lf%s\\l' "
       ;

    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
