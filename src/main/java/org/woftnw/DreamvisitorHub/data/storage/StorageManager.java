package org.woftnw.DreamvisitorHub.data.storage;

import java.util.Arrays;
import java.util.List;

public class StorageManager {

    private static final List<UnifiedStorage> unifiedStorages = Arrays.asList(
            new UnifiedStorage("config",
                    new ConfigValue(
                            "debug",
                            "Debug Mode",
                            "Whether to enable debug messages. This will send additional messages to help debug Dreamvisitor. Disabled by default.",
                            false,
                            ConfigValue.Type.BOOLEAN
                    )
            )
    );

}
