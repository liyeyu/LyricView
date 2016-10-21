package com.liyeyu.lrcview.lrc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liyeyu on 2016/10/13.
 */

public abstract class ILrcBuilder {
    public boolean isMessyCode = false;
    public List<String> suffix = new ArrayList<>();
    public String curLrcPath;

    protected abstract void parseLrcRow(String line,LrcInfo lrcInfo);
    public abstract LrcInfo parseLrcInfo(InputStream inputStream);
}
