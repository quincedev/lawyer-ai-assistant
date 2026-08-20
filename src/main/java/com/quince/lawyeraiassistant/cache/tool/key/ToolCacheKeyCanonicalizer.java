package com.quince.lawyeraiassistant.cache.tool.key;

import java.util.Map;

public interface ToolCacheKeyCanonicalizer {

    Map<String, Object> canonicalize(
            String toolName,
            Map<String, Object> arguments);
}