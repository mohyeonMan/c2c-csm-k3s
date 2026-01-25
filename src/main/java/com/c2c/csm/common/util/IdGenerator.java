package com.c2c.csm.common.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String generateId(String prefix) {
        String safePrefix = prefix == null ? "" : prefix;
        String random = "_" + UUID.randomUUID().toString().replace("-", "");
        return safePrefix + random.substring(0, 10);
    }
}

