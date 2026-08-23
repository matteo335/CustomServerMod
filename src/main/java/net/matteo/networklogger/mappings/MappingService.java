    package net.matteo.networklogger.mappings;

    import net.matteo.networklogger.Main;

    import java.io.ByteArrayInputStream;
    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.Map;
    import java.util.TreeMap;
    import java.util.concurrent.atomic.AtomicReference;

    public class MappingService {
        private static final MappingService INSTANCE = new MappingService();
        private final Object loadLock = new Object();
        public final AtomicReference<MappingTable> table;
        private volatile boolean loading;

        public static MappingService get() {
            return INSTANCE;
        }

        private MappingService() {
            this.table = new AtomicReference<>(MappingTable.EMPTY);
            this.loading = false;
        }

        public boolean ready() {
            return !(this.table.get()).isEmpty();
        }


        public MappingTable table() {
            return this.table.get();
        }

        public void initAsync() {
            synchronized(this.loadLock) {
                if (this.loading || this.ready()) {
                    return;
                }

                this.loading = true;
            }

            Thread t = new Thread(this::load, "networklogger-mappings");
            t.setDaemon(true);
            t.setPriority(4);
            t.start();
        }

        private void load() {
            try {
                try {
                    Path cacheDir = cacheDir();
                    Path composed = cacheDir.resolve("srg2mojang-1.20.1.txt");

                    byte[] proGuard = MappingDownloader.readMappingFile();
                    byte[] joinedTsrg = MappingDownloader.mcpJoinedTsrg(cacheDir);
                    MappingTable composedTable = MappingComposer.compose(new ByteArrayInputStream(joinedTsrg), new ByteArrayInputStream(proGuard));
                    if (!isPlausible(composedTable)) {
                        throw new IOException("Composed mapping table looks wrong: only " + composedTable.size() + " entries");
                    }

                    saveFlat(composed, composedTable);
                    this.table.set(composedTable);
                    Main.logger.info("Loaded {} SRG->Mojang mappings", composedTable.size());
                } catch (Throwable exception) {
                    Main.logger.error("Failed to load mapping service", exception);
                }

            } finally {
                synchronized(this.loadLock) {
                    this.loading = false;
                    this.loadLock.notifyAll();
                }
            }
        }

        private static boolean isPlausible(MappingTable table) {
            return table.size() >= 20000;
        }

        private static Path cacheDir() {
            return MappingDownloader.mappingFile.getParent();
        }

        private static void saveFlat(Path file, MappingTable t) throws IOException {
            Map<String, String> sorted = new TreeMap<>(t.asMap());
            StringBuilder sb = new StringBuilder(sorted.size() * 24);
            sb.append("# NetworkLogger SRG->Mojang cache v1\n");
            sb.append("# minecraft=1.20.1 mojangClientSha1=").append("6c48521eed01fe2e8ecdadbd5ae348415f3c47da").append(" mcpJoinedTsrgSha256=").append("19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453").append('\n');

            for(Map.Entry<String, String> entry : sorted.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }

            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        }
    }
