package com.quince.lawyeraiassistant.security.mcp.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcp.security")
public class McpToolSecurityProperties {

    private List<String> allowedTools = List.of();

    private Arguments arguments = new Arguments();

    public List<String> getAllowedTools() {

        return allowedTools;
    }

    public void setAllowedTools(
            List<String> allowedTools) {

        this.allowedTools = allowedTools == null
                ? List.of()
                : List.copyOf(
                        allowedTools);
    }

    public Arguments getArguments() {

        return arguments;
    }

    public void setArguments(
            Arguments arguments) {

        this.arguments = arguments == null
                ? new Arguments()
                : arguments;
    }

    public static class Arguments {

        private int maxStringLength = 10000;

        private boolean rejectUnknownFields = true;

        public int getMaxStringLength() {

            return maxStringLength;
        }

        public void setMaxStringLength(
                int maxStringLength) {

            if (maxStringLength <= 0) {
                throw new IllegalArgumentException(
                        "maxStringLength must be greater than 0");
            }

            this.maxStringLength = maxStringLength;
        }

        public boolean isRejectUnknownFields() {

            return rejectUnknownFields;
        }

        public void setRejectUnknownFields(
                boolean rejectUnknownFields) {

            this.rejectUnknownFields = rejectUnknownFields;
        }
    }
}