package org.woftnw.DreamvisitorHub.data.storage;

import java.util.List;

public class UnifiedStorage {

    private String fileName;

    /**
     * This defines the possible keys and default values of those keys for a given {@link UnifiedStorage}.
     */
    private List<ConfigValue> dataStructure;

    public UnifiedStorage(String name, ConfigValue... structure) {
        fileName = name;
        dataStructure = List.of(structure);
    }

    public String getFileName() {
        return fileName;
    }

}
