package com.liyeyu.lrcview.lrc;

import java.io.Serializable;

/**
 * Created by Liyeyu on 2016/10/13.
 */

public class LrcRow implements Serializable,Comparable<LrcRow> {

    public String strTime;
    public String content;
    public long time;
    public float x;
    public float y;

    public LrcRow(String strTime,long time,String content) {
        this.strTime = strTime;
        this.content = content;
        this.time = time;
    }

    @Override
    public String toString() {
        return "[" + strTime + " ]"  + content +"\n";
    }

    @Override
    public int compareTo(LrcRow another) {
        return (int)(this.time - another.time);
    }
}
