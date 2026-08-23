package net.matteo.networklogger.utils.mappings;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public interface IMappingFile {

    static IMappingFile load(InputStream in) throws IOException {
        return InternalUtils.load(in);
    }

    Collection<? extends IMappingFile.IPackage> getPackages();

    Collection<? extends IMappingFile.IClass> getClasses();

    IMappingFile.IClass getClass(String var1);

    String remapPackage(String var1);

    String remapClass(String var1);

    String remapDescriptor(String var1);

    IMappingFile reverse();

    IMappingFile rename(IRenamer var1);

    IMappingFile chain(IMappingFile var1);

    enum Format {
        SRG(false),
        XSRG(true),
        CSRG(false),
        TSRG(false),
        TSRG2(true),
        PG(true),
        TINY1(true),
        TINY(true);

        private final boolean hasFieldTypes;

        Format(boolean hasFieldTypes) {
            this.hasFieldTypes = hasFieldTypes;
        }

        public boolean hasFieldTypes() {
            return this.hasFieldTypes;
        }

        public static IMappingFile.Format get(String name) {
            name = name.toUpperCase(Locale.ENGLISH);

            for(IMappingFile.Format value : values()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }

            return null;
        }
    }

    interface IClass extends IMappingFile.INode {
        Collection<? extends IMappingFile.IField> getFields();

        Collection<? extends IMappingFile.IMethod> getMethods();

        String remapField(String var1);

        String remapMethod(String var1, String var2);

        @Nullable
        IMappingFile.IMethod getMethod(String var1, String var2);
    }

    interface IField extends IMappingFile.IOwnedNode<IMappingFile.IClass> {
        @Nullable
        String getDescriptor();

        @Nullable
        String getMappedDescriptor();
    }

    interface IMethod extends IMappingFile.IOwnedNode<IMappingFile.IClass> {
        String getDescriptor();

        String getMappedDescriptor();

        Collection<? extends IMappingFile.IParameter> getParameters();

        String remapParameter(int var1, String var2);
    }

    interface INode {
        String getOriginal();

        String getMapped();

        @Nullable
        String write(IMappingFile.Format var1, boolean var2);

        Map<String, String> getMetadata();
    }

    interface IOwnedNode<T> extends IMappingFile.INode {
        T getParent();
    }

    interface IPackage extends IMappingFile.INode {
    }

    interface IParameter extends IMappingFile.IOwnedNode<IMappingFile.IMethod> {
        int getIndex();
    }
}
