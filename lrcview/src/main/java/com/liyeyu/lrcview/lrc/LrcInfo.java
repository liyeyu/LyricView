package com.liyeyu.lrcview.lrc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liyeyu on 2016/10/13.
 */

public class LrcInfo implements Serializable{
    public static final String DEFAULT_TIME = "00:00";
    public List<LrcRow> rows;
    public String title;
    public String artist;
    public String album;
    public long offset;
    public String by;

    public LrcInfo() {
        rows = new ArrayList<>();
    }
}
