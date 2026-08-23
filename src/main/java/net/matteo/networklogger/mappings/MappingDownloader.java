    package net.matteo.networklogger.mappings;

    import static net.matteo.networklogger.Main.*;

    import java.io.ByteArrayInputStream;
    import java.io.IOException;
    import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;
    import java.net.URI;
    import java.nio.file.Path;
    import java.nio.file.Files;
    import java.security.MessageDigest;
    import java.security.NoSuchAlgorithmException;
    import java.time.Duration;
    import java.util.HexFormat;
    import java.util.zip.ZipEntry;
    import java.util.zip.ZipInputStream;

    public class MappingDownloader {
        public static final String mapping_file_url = "https://piston-data.mojang.com/v1/objects/0b4dba049482496c507b2387a73a913230ebbd76/server.txt";
        public static final String mcp_config_file_url = "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.20.1/mcp_config-1.20.1.zip";
        public static final Path mappingFile = gamedir.resolve("mappings").resolve("server.txt");

        public MappingDownloader() {
            if (mappingFile.toFile().exists()) return;
            try {
                Files.createDirectories(mappingFile.getParent());
                Files.write(mappingFile, downloadMappingFile());
                logger.info("Downloaded Mojang's server mapping in mappings/server.txt - if you are a mod developer, consider using this path for your mappings.");
            } catch (Throwable exception) {
                logger.error("Failed to create the mapping file. The mod will continue to work, but the profiler will be unable to translate the field names");
            }
        }

        private static byte[] downloadMappingFile() throws Throwable {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15L)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(mapping_file_url)).timeout(Duration.ofMinutes(2L)).GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() != 200) {
                    logger.error("Failed to download mapping file with status code {}. The profiler will not translate field names", response.statusCode());
                }

                return response.body();
        }

        public static byte[] readMappingFile() throws IOException {
            return Files.readAllBytes(mappingFile);
        }

        public static byte[] mcpJoinedTsrg(Path cacheDir) throws IOException, InterruptedException {
            Path file = cacheDir.resolve("joined-1.20.1.tsrg");
            if (Files.exists(file)) {
                byte[] cached = Files.readAllBytes(file);
                if (sha256Hex(cached).equalsIgnoreCase("19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453")) {
                    return cached;
                }
            }

            byte[] zipBytes = httpGet();
            verifySha256("MCPConfig zip", zipBytes, "f46d1050d8bed9046886b90a0d2fb80b4c6b5120fc35d32e5d37498dbe0a6d2a");
            byte[] joined = extractFromZip(zipBytes);
            if (joined == null) {
                throw new IOException("config/joined.tsrg not found in MCPConfig zip");
            } else {
                verifySha256("MCPConfig joined.tsrg", joined, "19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453");
                Files.createDirectories(cacheDir);
                Files.write(file, joined);
                return joined;
            }
        }

        private static void verifySha256(String label, byte[] bytes, String expected) throws IOException {
            String actual = sha256Hex(bytes);
            if (!actual.equalsIgnoreCase(expected)) {
                throw new IOException(label + " SHA-256 mismatch: got " + actual + ", expected " + expected);
            }
        }

        private static byte[] extractFromZip(byte[] zipBytes) throws IOException {
            ZipEntry e;
            try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                while((e = zin.getNextEntry()) != null) {
                    if ("config/joined.tsrg".equals(e.getName())) {
                        return zin.readAllBytes();
                    }
                }
            }

            return null;
        }

        private static byte[] httpGet() throws IOException, InterruptedException {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15L)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(mcp_config_file_url)).timeout(Duration.ofMinutes(2L)).GET().build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                int statusCode = resp.statusCode();
                throw new IOException("HTTP " + statusCode + " for " + mcp_config_file_url);
            } else {
                return resp.body();
            }
        }

        private static String sha256Hex(byte[] bytes) {
            return digestHex(bytes);
        }

        private static String digestHex(byte[] bytes) {
            try {
                byte[] h = MessageDigest.getInstance("SHA-256").digest(bytes);
                return HexFormat.of().formatHex(h);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256" + " not available", e);
            }
        }
    }
