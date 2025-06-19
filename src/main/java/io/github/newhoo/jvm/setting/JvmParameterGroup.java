package io.github.newhoo.jvm.setting;

import java.util.ArrayList;
import java.util.List;

/**
 * JvmParameterGroup
 *
 * @author huzunrong
 * @since 1.0.8
 */
public class JvmParameterGroup {

    private String name;
    private Boolean isGlobal;
    private List<JvmParameter> items = new ArrayList<>();

    public JvmParameterGroup() {
    }

    public JvmParameterGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getGlobal() {
        return isGlobal;
    }

    public void setGlobal(Boolean global) {
        isGlobal = global;
    }

    public List<JvmParameter> getItems() {
        return items;
    }

    public void setItems(List<JvmParameter> items) {
        this.items = items;
    }
} 