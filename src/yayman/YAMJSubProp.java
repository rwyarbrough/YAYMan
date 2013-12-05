/*
 *      Copyright (c) 2009-2010 nord
 *
 *      Web: http://mediaplayersite.com/YAYMan
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://creativecommons.org/licenses/by-nc/3.0/
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */

package yayman;

public class YAMJSubProp {
    private String subDesc;
    private String subType;
    private String subPrefix;

    public YAMJSubProp() {
        subDesc = null;
        subType = YAMJProp.DefaultType;
        subPrefix = null;
    }

    public YAMJSubProp(String d) {
        this();
        subDesc = d;
    }

    public String getDesc() {
        return subDesc;
    }

    public String getType() {
        return subType;
    }

    public String getPrefix() {
        return subPrefix;
    }

    public void setDesc(String d) {
        subDesc = d;
    }

    public void setType(String t) {
        subType = t;
    }

    public void setPrefix(String p) {
        subPrefix = p;
    }
}
