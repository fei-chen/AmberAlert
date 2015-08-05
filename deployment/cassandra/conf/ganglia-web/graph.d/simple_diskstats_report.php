<?php

function graph_simple_diskstats_report ( &$rrdtool_graph ) {

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
    $title = 'Disk IO';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title'] = $title;
    } else {
       $rrdtool_graph['title'] = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'Bytes';
    $rrdtool_graph['height'] += ($size == 'medium') ? 28 : 0;
    //$rrdtool_graph['lower-limit'] = '0';
    //$rrdtool_graph['extras'] = ($conf['graphreport_stats'] == true) ? ' --font LEGEND:7' : '';

    /*
     * Here we actually build the chart series.  This is moderately complicated
     * to show off what you can do.  For a simpler example, look at
     * network_report.php
     */
    $series =
        "'DEF:bytes_read=${rrd_dir}/simple_diskstats_bytes_read.rrd:sum:AVERAGE' "
      . "'DEF:bytes_written=${rrd_dir}/simple_diskstats_bytes_written.rrd:sum:AVERAGE' "
      . "'CDEF:_bytes_read=bytes_read,-1,*' "
      . "'CDEF:_bytes_written=bytes_written' "
      . " LINE1:'0'#00000066:'' "
      . "'LINE2:_bytes_read#3333bb:Read' "
      ;

    if ( $conf['graphreport_stats'] ) {
        $series .=
            "CDEF:read_pos=bytes_read,0,INF,LIMIT "
          . "VDEF:read_last=read_pos,LAST "
          . "VDEF:read_min=read_pos,MINIMUM "
          . "VDEF:read_avg=read_pos,AVERAGE "
          . "VDEF:read_max=read_pos,MAXIMUM "
          . "GPRINT:'read_last':'   ${space1}Now\:%6.1lf%s' "
          . "GPRINT:'read_min':'${space1}Min\:%6.1lf%s' "
          . "GPRINT:'read_avg':'${space1}Avg\:%6.1lf%s' "
          . "GPRINT:'read_max':'${space1}Max\:%6.1lf%s\\l' "
          ;
    }
    
    $series .=
        "'LINE2:_bytes_written#FF0000:Written' "
       ;
    if ( $conf['graphreport_stats'] ) {
        $series .=
            "CDEF:written_pos=bytes_written,0,INF,LIMIT "
          . "VDEF:written_last=written_pos,LAST "
          . "VDEF:written_min=written_pos,MINIMUM "
          . "VDEF:written_avg=written_pos,AVERAGE "
          . "VDEF:written_max=written_pos,MAXIMUM "
          . "GPRINT:'written_last':'${space1}Now\:%6.1lf%s' "
          . "GPRINT:'written_min':'${space1}Min\:%6.1lf%s' "
          . "GPRINT:'written_avg':'${space1}Avg\:%6.1lf%s' "
          . "GPRINT:'written_max':'${space1}Max\:%6.1lf%s\\l' "
          ;
    }

    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
