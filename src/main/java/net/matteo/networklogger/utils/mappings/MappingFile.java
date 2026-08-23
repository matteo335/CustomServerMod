package net.matteo.networklogger.utils.mappings;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MappingFile implements IMappingFile {
    private final Map<String, Package> packages = new HashMap<>();
    private final Collection<Package> packagesView;
    private final Map<String, Cls> classes;
    private final Collection<Cls> classesView;
    private final Map<String, String> cache;
    static final Pattern DESC = Pattern.compile("L(?<cls>[^;]+);");

    MappingFile() {
        this.packagesView = Collections.unmodifiableCollection(this.packages.values());
        this.classes = new HashMap<>();
        this.classesView = Collections.unmodifiableCollection(this.classes.values());
        this.cache = new ConcurrentHashMap<>();
    }

    MappingFile(NamedMappingFile source, int from, int to) {
        this.packagesView = Collections.unmodifiableCollection(this.packages.values());
        this.classes = new HashMap<>();
        this.classesView = Collections.unmodifiableCollection(this.classes.values());
        this.cache = new ConcurrentHashMap<>();
        source.getPackages().forEach((pkg) -> this.addPackage(pkg.getName(from), pkg.getName(to), pkg.meta));
        source.getClasses().forEach((cls) -> {
            Cls c = this.addClass(cls.getName(from), cls.getName(to), cls.meta);
            cls.getFields().forEach((fld) -> c.addField(fld.getName(from), fld.getName(to), fld.getDescriptor(from), fld.meta));
            cls.getMethods().forEach((mtd) -> {
                Cls.Method m = c.addMethod(mtd.getName(from), mtd.getDescriptor(from), mtd.getName(to), mtd.meta);
                mtd.getParameters().forEach((par) -> m.addParameter(par.getIndex(), par.getName(from), par.getName(to), par.meta));
            });
        });
    }

    public Collection<Package> getPackages() {
        return this.packagesView;
    }

    private void addPackage(String original, String mapped, Map<String, String> metadata) {
        this.packages.put(original, new Package(original, mapped, metadata));
    }

    public Collection<Cls> getClasses() {
        return this.classesView;
    }

    @Nullable
    public Cls getClass(String original) {
        return this.classes.get(original);
    }

    private Cls addClass(String original, String mapped, Map<String, String> metadata) {
        return retPut(this.classes, original, new Cls(original, mapped, metadata));
    }

    public String remapPackage(String pkg) {
        Package ipkg = this.packages.get(pkg);
        return ipkg == null ? pkg : ipkg.getMapped();
    }

    public String remapClass(String cls) {
        String ret = this.cache.get(cls);
        if (ret == null) {
            Cls _cls = this.classes.get(cls);
            if (_cls == null) {
                int idx = cls.lastIndexOf(36);
                if (idx != -1) {
                    ret = this.remapClass(cls.substring(0, idx)) + '$' + cls.substring(idx + 1);
                } else {
                    ret = cls;
                }
            } else {
                ret = _cls.getMapped();
            }

            this.cache.put(cls, ret);
        }

        return ret;
    }

    public String remapDescriptor(String desc) {
        Matcher matcher = DESC.matcher(desc);
        StringBuilder buf = new StringBuilder();

        while(matcher.find()) {
            matcher.appendReplacement(buf, Matcher.quoteReplacement("L" + this.remapClass(matcher.group("cls")) + ";"));
        }

        matcher.appendTail(buf);
        return buf.toString();
    }

    public MappingFile reverse() {
        MappingFile ret = new MappingFile();
        this.getPackages().forEach((pkg) -> ret.addPackage(pkg.getMapped(), pkg.getOriginal(), pkg.getMetadata()));
        this.getClasses().forEach((cls) -> {
            Cls c = ret.addClass(cls.getMapped(), cls.getOriginal(), cls.getMetadata());
            cls.getFields().forEach((fld) -> c.addField(fld.getMapped(), fld.getOriginal(), fld.getMappedDescriptor(), fld.getMetadata()));
            cls.getMethods().forEach((mtd) -> {
                Cls.Method m = c.addMethod(mtd.getMapped(), mtd.getMappedDescriptor(), mtd.getOriginal(), mtd.getMetadata());
                mtd.getParameters().forEach((par) -> m.addParameter(par.getIndex(), par.getMapped(), par.getOriginal(), par.getMetadata()));
            });
        });
        return ret;
    }

    public MappingFile rename(IRenamer renamer) {
        MappingFile ret = new MappingFile();
        this.getPackages().forEach((pkg) -> ret.addPackage(pkg.getOriginal(), renamer.rename(pkg), pkg.getMetadata()));
        this.getClasses().forEach((cls) -> {
            Cls c = ret.addClass(cls.getOriginal(), renamer.rename(cls), cls.getMetadata());
            cls.getFields().forEach((fld) -> c.addField(fld.getOriginal(), renamer.rename(fld), fld.getDescriptor(), fld.getMetadata()));
            cls.getMethods().forEach((mtd) -> {
                Cls.Method m = c.addMethod(mtd.getOriginal(), mtd.getDescriptor(), renamer.rename(mtd), mtd.getMetadata());
                mtd.getParameters().forEach((par) -> m.addParameter(par.getIndex(), par.getOriginal(), renamer.rename(par), par.getMetadata()));
            });
        });
        return ret;
    }

    public MappingFile chain(final IMappingFile link) {
        return this.rename(new IRenamer() {
            public String rename(IMappingFile.IPackage value) {
                return link.remapPackage(value.getMapped());
            }

            public String rename(IMappingFile.IClass value) {
                return link.remapClass(value.getMapped());
            }

            public String rename(IMappingFile.IField value) {
                IMappingFile.IClass cls = link.getClass(value.getParent().getMapped());
                return cls == null ? value.getMapped() : cls.remapField(value.getMapped());
            }

            public String rename(IMappingFile.IMethod value) {
                IMappingFile.IClass cls = link.getClass((value.getParent()).getMapped());
                return cls == null ? value.getMapped() : cls.remapMethod(value.getMapped(), value.getMappedDescriptor());
            }

            public String rename(IMappingFile.IParameter value) {
                IMappingFile.IMethod mtd = value.getParent();
                IMappingFile.IClass cls = link.getClass((mtd.getParent()).getMapped());
                mtd = cls == null ? null : cls.getMethod(mtd.getMapped(), mtd.getMappedDescriptor());
                return mtd == null ? value.getMapped() : mtd.remapParameter(value.getIndex(), value.getMapped());
            }
        });
    }

    private static <K, V> V retPut(Map<K, V> map, K key, V value) {
        map.put(key, value);
        return value;
    }

    abstract static class Node implements IMappingFile.INode {
        private final String original;
        private final String mapped;
        private final Map<String, String> metadata;

        protected Node(String original, String mapped, Map<String, String> metadata) {
            this.original = original;
            this.mapped = mapped;
            this.metadata = metadata.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
        }

        public String getOriginal() {
            return this.original;
        }

        public String getMapped() {
            return this.mapped;
        }

        public Map<String, String> getMetadata() {
            return this.metadata;
        }
    }

    static class Package extends Node implements IMappingFile.IPackage {
        protected Package(String original, String mapped, Map<String, String> metadata) {
            super(original, mapped, metadata);
        }

        @Nullable
        public String write(IMappingFile.Format format, boolean reversed) {
            String sorig = this.getOriginal().isEmpty() ? "." : this.getOriginal();
            String smap = this.getMapped().isEmpty() ? "." : this.getMapped();
            if (reversed) {
                String tmp = sorig;
                sorig = smap;
                smap = tmp;
            }

            return switch (format) {
                case SRG, XSRG -> "PK: " + sorig + ' ' + smap;
                case CSRG, TSRG, TSRG2 -> this.getOriginal() + "/ " + this.getMapped() + '/';
                case PG, TINY1 -> null;
                default -> throw new UnsupportedOperationException("Unknown format: " + format);
            };
        }

        public String toString() {
            return this.write(Format.SRG, false);
        }
    }

    public class Cls extends Node implements IMappingFile.IClass {
        private final Map<String, Field> fields = new HashMap<>();
        private final Collection<Field> fieldsView;
        private final Map<String, Method> methods;
        private final Collection<Method> methodsView;

        protected Cls(String original, String mapped, Map<String, String> metadata) {
            super(original, mapped, metadata);
            this.fieldsView = Collections.unmodifiableCollection(this.fields.values());
            this.methods = new HashMap<>();
            this.methodsView = Collections.unmodifiableCollection(this.methods.values());
        }

        @Nullable
        public String write(IMappingFile.Format format, boolean reversed) {
            String oName = !reversed ? this.getOriginal() : this.getMapped();
            String mName = !reversed ? this.getMapped() : this.getOriginal();
            return switch (format) {
                case SRG, XSRG -> "CL: " + oName + ' ' + mName;
                case CSRG, TSRG, TSRG2 -> oName + ' ' + mName;
                case PG -> oName.replace('/', '.') + " -> " + mName.replace('/', '.') + ':';
                case TINY1 -> "CLASS\t" + oName + '\t' + mName;
                case TINY -> "c\t" + oName + '\t' + mName;
            };
        }

        public Collection<Field> getFields() {
            return this.fieldsView;
        }

        public String remapField(String field) {
            Field fld = this.fields.get(field);
            return fld == null ? field : fld.getMapped();
        }

        private void addField(String original, String mapped, String desc, Map<String, String> metadata) {
            MappingFile.retPut(this.fields, original, new Field(original, mapped, desc, metadata));
        }

        public Collection<Method> getMethods() {
            return this.methodsView;
        }

        @Nullable
        public Method getMethod(String name, String desc) {
            return this.methods.get(name + desc);
        }

        private Method addMethod(String original, String desc, String mapped, Map<String, String> metadata) {
            return MappingFile.retPut(this.methods, original + desc, new Method(original, desc, mapped, metadata));
        }

        public String remapMethod(String name, String desc) {
            Method mtd = this.methods.get(name + desc);
            return mtd == null ? name : mtd.getMapped();
        }

        public String toString() {
            return this.write(Format.SRG, false);
        }

        class Field extends Node implements IMappingFile.IField {
            private final String desc;

            private Field(String original, String mapped, String desc, Map<String, String> metadata) {
                super(original, mapped, metadata);
                this.desc = desc;
            }

            public String getDescriptor() {
                return this.desc;
            }

            public String getMappedDescriptor() {
                return this.desc == null ? null : MappingFile.this.remapDescriptor(this.desc);
            }

            @Nullable
            public String write(IMappingFile.Format format, boolean reversed) {
                if (format != Format.TSRG2 && format.hasFieldTypes() && this.desc == null) {
                    throw new IllegalStateException("Can not write " + format.name() + " format, field is missing descriptor");
                } else {
                    String oOwner = !reversed ? Cls.this.getOriginal() : Cls.this.getMapped();
                    String mOwner = !reversed ? Cls.this.getMapped() : Cls.this.getOriginal();
                    String oName = !reversed ? this.getOriginal() : this.getMapped();
                    String mName = !reversed ? this.getMapped() : this.getOriginal();
                    String oDesc = !reversed ? this.getDescriptor() : this.getMappedDescriptor();
                    String mDesc = !reversed ? this.getMappedDescriptor() : this.getDescriptor();
                    return switch (format) {
                        case SRG ->
                                "FD: " + oOwner + '/' + oName + ' ' + mOwner + '/' + mName + (oDesc == null ? "" : " # " + oDesc + " " + mDesc);
                        case XSRG ->
                                "FD: " + oOwner + '/' + oName + (oDesc == null ? "" : ' ' + oDesc) + ' ' + mOwner + '/' + mName + (mDesc == null ? "" : ' ' + mDesc);
                        case CSRG -> oOwner + ' ' + oName + ' ' + mName;
                        case TSRG -> '\t' + oName + ' ' + mName;
                        case TSRG2 -> '\t' + oName + (oDesc == null ? "" : ' ' + oDesc) + ' ' + mName;
                        case PG -> "    " + InternalUtils.toSource(oDesc) + ' ' + oName + " -> " + mName;
                        case TINY1 -> "FIELD\t" + oOwner + '\t' + oDesc + '\t' + oName + '\t' + mName;
                        case TINY -> "\tf\t" + oDesc + '\t' + oName + '\t' + mName;
                    };
                }
            }

            public String toString() {
                return this.write(Format.SRG, false);
            }

            public Cls getParent() {
                return Cls.this;
            }
        }

        class Method extends Node implements IMappingFile.IMethod {
            private final String desc;
            private final Map<Integer, Parameter> params;
            private final Collection<Parameter> paramsView;

            private Method(String original, String desc, String mapped, Map<String, String> metadata) {
                super(original, mapped, metadata);
                this.params = new HashMap<>();
                this.paramsView = Collections.unmodifiableCollection(this.params.values());
                this.desc = desc;
            }

            public String getDescriptor() {
                return this.desc;
            }

            public String getMappedDescriptor() {
                return MappingFile.this.remapDescriptor(this.desc);
            }

            public Collection<Parameter> getParameters() {
                return this.paramsView;
            }

            private void addParameter(int index, String original, String mapped, Map<String, String> metadata) {
                MappingFile.retPut(this.params, index, new Parameter(index, original, mapped, metadata));
            }

            public String remapParameter(int index, String name) {
                Parameter param = this.params.get(index);
                return param == null ? name : param.getMapped();
            }

            public String write(IMappingFile.Format format, boolean reversed) {
                String oName = !reversed ? this.getOriginal() : this.getMapped();
                String mName = !reversed ? this.getMapped() : this.getOriginal();
                String oOwner = !reversed ? Cls.this.getOriginal() : Cls.this.getMapped();
                String mOwner = !reversed ? Cls.this.getMapped() : Cls.this.getOriginal();
                String oDesc = !reversed ? this.getDescriptor() : this.getMappedDescriptor();
                String mDesc = !reversed ? this.getMappedDescriptor() : this.getDescriptor();
                switch (format) {
                    case SRG:
                    case XSRG:
                        return "MD: " + oOwner + '/' + oName + ' ' + oDesc + ' ' + mOwner + '/' + mName + ' ' + mDesc;
                    case CSRG:
                        return oOwner + ' ' + oName + ' ' + oDesc + ' ' + mName;
                    case TSRG:
                    case TSRG2:
                        return '\t' + oName + ' ' + oDesc + ' ' + mName;
                    case PG:
                        int start = Integer.parseInt(this.getMetadata().getOrDefault("start_line", "0"));
                        int end = Integer.parseInt(this.getMetadata().getOrDefault("end_line", "0"));
                        return "    " + (start == 0 && end == 0 ? "" : start + ":" + end + ":") + InternalUtils.toSource(oName, oDesc) + " -> " + mName;
                    case TINY1:
                        return "METHOD\t" + oOwner + '\t' + oDesc + '\t' + oName + '\t' + mName;
                    case TINY:
                        return "\tm\t" + oDesc + '\t' + oName + '\t' + mName;
                    default:
                        throw new UnsupportedOperationException("Unknown format: " + format);
                }
            }

            public String toString() {
                return this.write(Format.SRG, false);
            }

            public Cls getParent() {
                return Cls.this;
            }

            class Parameter extends Node implements IMappingFile.IParameter {
                private final int index;

                protected Parameter(int index, String original, String mapped, Map<String, String> metadata) {
                    super(original, mapped, metadata);
                    this.index = index;
                }

                public IMappingFile.IMethod getParent() {
                    return Method.this;
                }

                public int getIndex() {
                    return this.index;
                }

                public String write(IMappingFile.Format format, boolean reversed) {
                    String oName = !reversed ? this.getOriginal() : this.getMapped();
                    String mName = !reversed ? this.getMapped() : this.getOriginal();
                    return switch (format) {
                        case SRG, XSRG, CSRG, TSRG, PG, TINY1 -> null;
                        case TSRG2 -> "\t\t" + this.getIndex() + ' ' + oName + ' ' + mName;
                        case TINY -> "\t\tp\t" + this.getIndex() + '\t' + oName + '\t' + mName;
                    };
                }
            }
        }
    }
}
