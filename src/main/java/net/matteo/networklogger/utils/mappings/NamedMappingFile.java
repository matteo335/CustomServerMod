package net.matteo.networklogger.utils.mappings;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class NamedMappingFile implements INamedMappingFile, IMappingBuilder {
    private final List<String> names;
    private final Map<String, NamedMappingFile.Package> packages = new HashMap<>();
    private final Map<String, NamedMappingFile.Cls> classes = new HashMap<>();
    private final Map<String, String[]> classCache = new ConcurrentHashMap<>();
    private final Map<String, IMappingFile> mapCache = new ConcurrentHashMap<>();

    NamedMappingFile(String... names) {
        if (names != null && names.length >= 2) {
            this.names = List.copyOf(Arrays.asList(names));
        } else {
            throw new IllegalArgumentException("Can not create Mapping file with less then two names");
        }
    }

    private void ensureCount(String... names) {
        if (names == null) {
            throw new IllegalArgumentException("Names can not be null");
        } else if (names.length != this.names.size()) {
            throw new IllegalArgumentException("Invalid number of names, expected " + this.names.size() + " got " + names.length);
        }
    }

    public List<String> getNames() {
        return this.names;
    }

    public IMappingFile getMap(String from, String to) {
        String key = from + "_to_" + to;
        return this.mapCache.computeIfAbsent(key, (k) -> {
            int fromI = this.names.indexOf(from);
            int toI = this.names.indexOf(to);
            if (fromI != -1 && toI != -1) {
                return new MappingFile(this, fromI, toI);
            } else {
                throw new IllegalArgumentException("Could not find mapping names: " + from + " / " + to);
            }
        });
    }

    private static <K, V> V retPut(Map<K, V> map, K key, V value) {
        map.put(key, value);
        return value;
    }

    private String remapClass(int index, String cls) {
        String[] ret = this.remapClass(cls);
        return ret[ret.length == 1 ? 0 : index];
    }

    private String[] remapClass(String cls) {
        String[] ret = this.classCache.get(cls);
        if (ret == null) {
            NamedMappingFile.Cls _cls = this.classes.get(cls);
            if (_cls == null) {
                int idx = cls.lastIndexOf(36);
                if (idx != -1) {
                    String[] parent = this.remapClass(cls.substring(0, idx));
                    ret = new String[parent.length];

                    for(int x = 0; x < ret.length; ++x) {
                        ret[x] = parent[x] + '$' + cls.substring(idx + 1);
                    }
                } else {
                    ret = new String[]{cls};
                }
            } else {
                ret = _cls.getNames();
            }

            this.classCache.put(cls, ret);
        }

        return ret;
    }

    private String remapDescriptor(int index, String desc) {
        Matcher matcher = MappingFile.DESC.matcher(desc);
        StringBuilder buf = new StringBuilder();

        while(matcher.find()) {
            matcher.appendReplacement(buf, Matcher.quoteReplacement("L" + this.remapClass(index, matcher.group("cls")) + ";"));
        }

        matcher.appendTail(buf);
        return buf.toString();
    }

    Stream<NamedMappingFile.Package> getPackages() {
        return this.packages.values().stream();
    }

    Stream<NamedMappingFile.Cls> getClasses() {
        return this.classes.values().stream();
    }

    public void addPackage(String... names) {
        this.ensureCount(names);
        retPut(this.packages, names[0], new Package(names));
    }

    public NamedMappingFile.Cls addClass(String... names) {
        this.ensureCount(names);
        return retPut(this.classes, names[0], new NamedMappingFile.Cls(names));
    }

    public INamedMappingFile build() {
        return this;
    }

    abstract static class Named {
        private final String[] names;

        Named(String... names) {
            this.names = names;
        }

        public String getName(int index) {
            return this.names[index];
        }

        String[] getNames() {
            return this.names;
        }
    }

    static class Package extends NamedMappingFile.Named  {
        final Map<String, String> meta = new LinkedHashMap<>();

        Package(String... names) {
            super(names);
        }
    }

    class Cls extends NamedMappingFile.Named implements IMappingBuilder.IClass {
        private final Map<String, NamedMappingFile.Cls.Field> fields = new HashMap<>();
        private final Map<String, NamedMappingFile.Cls.Method> methods = new HashMap<>();
        final Map<String, String> meta = new LinkedHashMap<>();

        Cls(String... name) {
            super(name);
        }

        Stream<NamedMappingFile.Cls.Field> getFields() {
            return this.fields.values().stream();
        }

        Stream<NamedMappingFile.Cls.Method> getMethods() {
            return this.methods.values().stream();
        }

        public NamedMappingFile.Cls.Field field(String... names) {
            NamedMappingFile.this.ensureCount(names);
            return NamedMappingFile.retPut(this.fields, names[0], new NamedMappingFile.Cls.Field(names));
        }

        public NamedMappingFile.Cls.Method method(String desc, String... names) {
            NamedMappingFile.this.ensureCount(names);
            return NamedMappingFile.retPut(this.methods, names[0] + desc, new NamedMappingFile.Cls.Method(desc, names));
        }

        public void meta(String key, String value) {
            this.meta.put(key, value);
        }

        class Field extends NamedMappingFile.Named implements IMappingBuilder.IField {
            @Nullable
            private String desc;
            final Map<String, String> meta = new LinkedHashMap<>();

            Field(String... names) {
                super(names);
            }

            public String getDescriptor(int index) {
                return this.desc == null ? null : (index == 0 ? this.desc : NamedMappingFile.this.remapDescriptor(index, this.desc));
            }

            public IMappingBuilder.IField descriptor(String value) {
                this.desc = value;
                return this;
            }

            public void meta(String key, String value) {
                this.meta.put(key, value);
            }

        }

        class Method extends NamedMappingFile.Named implements IMappingBuilder.IMethod {
            private final String desc;
            private final Map<Integer, NamedMappingFile.Cls.Method.Parameter> params = new HashMap<>();
            final Map<String, String> meta = new LinkedHashMap<>();

            Method(String desc, String... names) {
                super(names);
                this.desc = desc;
            }

            public IMappingBuilder.IParameter parameter(int index, String... names) {
                NamedMappingFile.this.ensureCount(names);
                return NamedMappingFile.retPut(this.params, index, new NamedMappingFile.Cls.Method.Parameter(index, names));
            }

            public void meta(String key, String value) {
                this.meta.put(key, value);
            }

            public String getDescriptor(int index) {
                return index == 0 ? this.desc : NamedMappingFile.this.remapDescriptor(index, this.desc);
            }

            Stream<NamedMappingFile.Cls.Method.Parameter> getParameters() {
                return this.params.values().stream();
            }

            static class Parameter extends NamedMappingFile.Named implements IMappingBuilder.IParameter {
                private final int index;
                final Map<String, String> meta = new LinkedHashMap<>();

                Parameter(int index, String... names) {
                    super(names);
                    this.index = index;
                }

                public int getIndex() {
                    return this.index;
                }

                public void meta(String key, String value) {
                    this.meta.put(key, value);
                }
            }
        }
    }
}
