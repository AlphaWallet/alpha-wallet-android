package io.stormbird.token.tools;

public class AppleAssociationConfig {

    public Response AppleConfig(Request req) throws Exception {
        return new Response(appleAssociationConfig);
    }

    private static final String appleAssociationConfig = "{\n" +
            "  \"applinks\": {\n" +
            "    \"apps\": [],\n" +
            "    \"details\": [\n" +
            "      {\n" +
            "        \"appID\": \"LRAW5PL536.com.stormbird.alphawallet\",\n" +
            "        \"paths\": [ \"*\" ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    public static class Request {
        public Request() {
        }
    }

    public static class Response {
        String config;

        public String getConfig() { return config; }

        public void setConfig(String config) { this.config = config; }

        public Response(String config) {
            this.config = config;
        }

        public Response() {
        }
    }

}
