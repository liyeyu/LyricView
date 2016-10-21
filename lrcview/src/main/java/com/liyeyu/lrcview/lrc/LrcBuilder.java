package com.liyeyu.lrcview.lrc;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Liyeyu on 2016/10/13.
 */

public class LrcBuilder extends ILrcBuilder{

    private OnLrcLoadListener mLoadListener ;
    private boolean mIsShowMetaDate;
    private int charsetPos;
    private List<String> mCharsets;

    public LrcBuilder(OnLrcLoadListener listener) {
        this();
        mLoadListener = listener;
    }

    public LrcBuilder() {
        suffix = new ArrayList<>();
        suffix.add("lrc");
        suffix.add("LRC");
        suffix.add("KRC");
        suffix.add("krc");
        suffix.add("TXT");
        suffix.add("txt");
        mCharsets = new ArrayList<>();
        mCharsets.add(LrcTag.CHARSET_UTF);
        mCharsets.add(LrcTag.CHARSET_GBK);
    }

    public LrcInfo parseLrcFromFile(String path) {
        LrcInfo result = null;
        if(TextUtils.isEmpty(path)){
            return result;
        }
        File file = new File(path);
        if(file.exists()){
            curLrcPath = file.getPath();
        }else{
            curLrcPath = file.getName() + suffix.get(0);
            return null;
        }
        try {
            result = parseLrcInfo(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public LrcInfo parseLrcFromMusic(String musicPath) {
        if(TextUtils.isEmpty(musicPath)){
            return null;
        }
        File file = new File(musicPath);
        String name = file.getName();
        for (int i = 0; i < suffix.size(); i++) {
            file = new File(name+"."+suffix.get(i));
            if(file.exists()){
                curLrcPath = file.getPath();
                break;
            }else{
                curLrcPath = name + suffix.get(0);
            }
        }
        return parseLrcFromFile(curLrcPath);
    }


    @Override
    protected void parseLrcRow(String line,LrcInfo lrcInfo) {
        Log.i("parseLrcRow",line+"\n");
        int startTag = line.indexOf("[");
        int endTag = line.indexOf("]");
        if(startTag!=0){
            return;
        }
        if(line.startsWith(LrcTag.TAG_TITLE)){
            lrcInfo.title = line.substring(LrcTag.TAG_TITLE.length(),endTag);
            addLrcMetaDate(lrcInfo.title,lrcInfo);
        }else if(line.startsWith(LrcTag.TAG_ALBUM)){
            lrcInfo.album = line.substring(LrcTag.TAG_ALBUM.length(),endTag);
            addLrcMetaDate(lrcInfo.album,lrcInfo);
        }else if(line.startsWith(LrcTag.TAG_ARTIST)){
            lrcInfo.artist = line.substring(LrcTag.TAG_ARTIST.length(),endTag);
            addLrcMetaDate(lrcInfo.artist,lrcInfo);
        }else if(line.startsWith(LrcTag.TAG_BY)){
            lrcInfo.by = line.substring(LrcTag.TAG_BY.length(),endTag);
            addLrcMetaDate(lrcInfo.by,lrcInfo);
        }else if(line.startsWith(LrcTag.TAG_OFFSET)){
            String offset = line.substring(LrcTag.TAG_OFFSET.length(), endTag);
            if(TextUtils.isDigitsOnly(offset)){
                lrcInfo.offset = Long.parseLong(offset);
            }
        }else if(endTag == 9){
           //once or more 02:34.14
            int lastTag = line.lastIndexOf("]");
            String content = line.substring(lastTag + 1, line.length());
            String[] split = line.substring(0,lastTag+1).replace("[", "-").replace("]", "-").trim().split("-");
            for (int i = 0; i < split.length; i++) {
                if(!TextUtils.isEmpty(split[i])){
                    LrcRow lrcRow = new LrcRow(split[i],parseLrcTime(split[i]),content);
                    lrcInfo.rows.add(lrcRow);
                }
            }
        }
    }

    public LrcBuilder load(){
        if(mLoadListener!=null){
            parseLrcInfo(mLoadListener.loadLrc(this));
        }
        return this;
    }

    public void isShowMetaDate(boolean isShowMetaDate){
        mIsShowMetaDate = isShowMetaDate;
    }

    private void addLrcMetaDate(String content,LrcInfo lrcInfo){
        if(!TextUtils.isEmpty(content) && mIsShowMetaDate){
            lrcInfo.rows.add(new LrcRow(LrcInfo.DEFAULT_TIME,0,content));
        }
    }

    public long parseLrcTime(String startTime){
        long time = 0;
        String[] split = startTime.replace(".", ":").split(":");
        if(split!=null && split.length==3){
            time = Long.parseLong(split[0])*60*1000
                    + Long.parseLong(split[1])*1000
                    + Long.parseLong(split[2])*10;
        }
        return time;
    }

    private LrcInfo parseLrcInfo(InputStream inputStream,String charsetName) {
        LrcInfo lrcInfo = null;
        charsetPos = mCharsets.indexOf(charsetName);
        if(inputStream!=null){
            lrcInfo = new LrcInfo();
            InputStreamReader in = null;
            BufferedReader reader = null;
            try {
                in = new InputStreamReader(inputStream,charsetName);
                reader = new BufferedReader(in);
                String line;
                while((line = reader.readLine())!=null){
                    if(!TextUtils.isEmpty(line)){
                        isMessyCode = isMessyCode(line);
                        if(mLoadListener!=null && isMessyCode && charsetPos<mCharsets.size()-1){
                            load();
                            return lrcInfo;
                        }else{
                            parseLrcRow(line,lrcInfo);
                        }
                    }
                }
                if(mLoadListener!=null){
                    isMessyCode = false;
                    mLoadListener.onLoad(this,lrcInfo);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                closeReader(new Reader[]{in,reader});
            }
        }
        return lrcInfo;
    }

    private void closeReader(Reader ...readers){
        try {
            if(readers!=null){
                for (Reader reader:readers) {
                    reader.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LrcInfo parseLrcInfo(InputStream inputStream) {
        if(isMessyCode){
            charsetPos = (charsetPos+1)%mCharsets.size();
        }else{
            isMessyCode = false;
            charsetPos = 0;
        }
        return parseLrcInfo(inputStream,mCharsets.get(charsetPos));
    }

    /**
     * 判断字符串是否是乱码
     *
     * @param code 字符串
     * @return 是否是乱码
     */
    public static boolean isMessyCode(String code) {
        if(TextUtils.isEmpty(code)){
            return false;
        }
        Pattern p = Pattern.compile("\\s*|t*|r*|n*");
        Matcher m = p.matcher(code);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = ch.length;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
            }
        }
        float result = count / chLength;
        if (result > 0.4) {
            return true;
        } else {
            return false;
        }

    }
    /**
     * 判断字符是否是中文
     *
     * @param c 字符
     * @return 是否是中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    public List<String> getCharsets() {
        return mCharsets;
    }

    public interface OnLrcLoadListener{
        public InputStream loadLrc(LrcBuilder builder);
        public void onLoad(LrcBuilder builder,LrcInfo lrcInfo);
    }
}
