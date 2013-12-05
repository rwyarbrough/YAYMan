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

import java.util.*;

public class YAMJProp {
    private String key;
    private String desc;
    private String defaultValue;
    private String value;
    private String type;
    private String validValues;
    private boolean disabled;
    private ArrayList<YAMJSubProp> subProps;
    private YAMJProp parentProp;
    private boolean moreInfoOnWiki;
    private String keyForValues;
    private String valueDelimiter;

    static String DefaultType = "DEFAULT";

    public YAMJProp() {
        disabled = false;
        key = null;
        desc = null;
        defaultValue = null;
        value = null;
        type = DefaultType;
        validValues = null;
        subProps = new ArrayList();
        parentProp = null;
        moreInfoOnWiki = true;
        keyForValues = null;
        valueDelimiter = ",";
    }

    public YAMJProp(String KEY) {
        this();
        key = KEY;
    }

    public YAMJProp(String KEY, String DESC, String DEFVALUE) {
        this(KEY);
        desc = DESC;
        defaultValue = DEFVALUE;
    }

    public YAMJProp(String KEY, String DESC, String DEFVALUE, boolean DISABLED) {
        this(KEY, DESC, DEFVALUE);
        disabled = DISABLED;
    }

    public YAMJProp(String KEY, String DESC, String DEFVALUE, boolean DISABLED, String TYPE) {
        this(KEY, DESC, DEFVALUE, DISABLED);
        type = TYPE;
    }

    public YAMJProp(String KEY, String DESC, String DEFVALUE, boolean DISABLED, String TYPE, String VALIDVALUES) {
        this(KEY, DESC, DEFVALUE, DISABLED, TYPE);
        validValues = VALIDVALUES;
    }

    @Override
    public String toString() {
        if (hasParentProperty()) {
            return key.replace(parentProp.getKey()+".", "");
        }
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getValidValues() {
        return validValues;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getValue() {
        return value;
    }

    public String getUsedValue() {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isDefaultValue() {
        if (value == null) {
            return true;
        }
        return false;
    }
    
    public boolean hasSubProperties() {
        if (subProps.isEmpty()) return false;
        return true;
    }

    public ArrayList<YAMJSubProp> getSubProperties() {
        return subProps;
    }

    public YAMJSubProp getSubProperty(int i) {
        return subProps.get(i);
    }

    public boolean hasParentProperty() {
        if (parentProp != null) return true;
        return false;
    }

    public YAMJProp getParentProperty() {
        return parentProp;
    }

    public boolean hasWikiInfo() {
        return moreInfoOnWiki;
    }

    public String getKeyForValues() {
        return keyForValues;
    }

    public String getValueDelimiter() {
        return valueDelimiter;
    }

    public void setKey(String KEY) {
        key = KEY;
    }

    public void setDescription(String DESC) {
        desc = DESC;
    }

    public void setDefaultValue(String DEFVALUE) {
        defaultValue = DEFVALUE;
    }

    public void setValue(String VALUE) {
        value = VALUE;
    }

    public void setDisabled(boolean DISABLED) {
        disabled = DISABLED;
    }

    public void setType(String TYPE) {
        type = TYPE;
    }

    public void setValidValues(String VALIDVALUES) {
        validValues = VALIDVALUES;
    }

    public void addSubProperty(YAMJSubProp s) {
        subProps.add(s);
    }

    public void setParentProperty(YAMJProp p) {
        parentProp = p;
    }

    public void setWikiInfo(boolean b) {
        moreInfoOnWiki = b;
    }

    public void setKeyForValues(String s) {
        keyForValues = s;
    }

    public void setValueDelimiter(String s) {
        valueDelimiter = s;
    }
}
