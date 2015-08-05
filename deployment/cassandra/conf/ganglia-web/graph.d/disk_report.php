<?php

function graph_disk_report ( &$rrdtool_graph ) {

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
    $title = 'Disk';
    if ($context != 'host') {
       //  This will be turned into: "Clustername $TITLE last $timerange",
       //  so keep it short
       $rrdtool_graph['title'] = $title;
    } else {
       $rrdtool_graph['title'] = "$hostname $title last $range";
    }
    $rrdtool_graph['vertical-label'] = 'Bytes';
    $rrdtool_graph['height'] += ($size == 'medium') ? 28 : 0;
    $rrdtool_graph['lower-limit'] = '0';
    //$rrdtool_graph['extras'] = ($conf['graphreport_stats'] == true) ? ' --font LEGEND:7' : '';

    /*
     * Here we actually build the chart series.  This is moderately complicated
     * to show off what you can do.  For a simpler example, look at
     * network_report.php
     */
    $series ="'DEF:disk_total=${rrd_dir}/disk_total.rrd:sum:AVERAGE' "
      . "'DEF:disk_free=${rrd_dir}/disk_free.rrd:sum:AVERAGE' "
      . "'CDEF:disk_total_final=disk_total,1073741824,*' "
      . "'CDEF:disk_used=disk_total,disk_free,-' "
      . "'CDEF:disk_used_final=disk_used,1073741824,*' "
      . "'LINE:disk_total_final#FF0000:Disk Total' ";

    if ( $conf['graphreport_stats'] ) {
        $series .= "CDEF:total_pos=disk_total_final,0,INF,LIMIT "
                . "VDEF:total_last=total_pos,LAST "
                . "VDEF:total_min=total_pos,MINIMUM "
                . "VDEF:total_avg=total_pos,AVERAGE "
                . "VDEF:total_max=total_pos,MAXIMUM "
                . "GPRINT:'total_last':'${space1}Now\:%6.1lf%s' "
                . "GPRINT:'total_min':'${space1}Min\:%6.1lf%s' "
                . "GPRINT:'total_avg':'${space1}Avg\:%6.1lf%s' "
                . "GPRINT:'total_max':'${space1}Max\:%6.1lf%s\\l' ";
    }
    
    $series .= "'AREA:disk_used_final#A7D8D8:Disk Used' ";
    if ( $conf['graphreport_stats'] ) {
        $series .= "CDEF:used_pos=disk_used_final,0,INF,LIMIT "
                . "VDEF:used_last=used_pos,LAST "
                . "VDEF:used_min=used_pos,MINIMUM "
                . "VDEF:used_avg=used_pos,AVERAGE "
                . "VDEF:used_max=used_pos,MAXIMUM "
                . "GPRINT:'used_last':' ${space1}Now\:%6.1lf%s' "
                . "GPRINT:'used_min':'${space1}Min\:%6.1lf%s' "
                . "GPRINT:'used_avg':'${space1}Avg\:%6.1lf%s' "
                . "GPRINT:'used_max':'${space1}Max\:%6.1lf%s\\l' ";
    }

    // We have everything now, so add it to the array, and go on our way.
    $rrdtool_graph['series'] = $series;

    return $rrdtool_graph;
}

?>
