    package net.matteo.networklogger.mappings;

    import java.util.Map;

    public record MappingTable(Map<String, String> srgToMojang) {
        public static final MappingTable EMPTY = new MappingTable(Map.of());

        public int size() {
            return this.srgToMojang.size();
        }

        public boolean isEmpty() {
            return this.srgToMojang.isEmpty();
        }

        public Map<String, String> asMap() {
            return this.srgToMojang;
        }

        public String lookup(String srgToken) {
            return this.srgToMojang.get(srgToken);
        }
    }
