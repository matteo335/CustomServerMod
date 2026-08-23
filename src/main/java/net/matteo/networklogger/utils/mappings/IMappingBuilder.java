package net.matteo.networklogger.utils.mappings;

public interface IMappingBuilder {
    static IMappingBuilder create(String... names) {
        return new NamedMappingFile(names != null && names.length != 0 ? names : new String[]{"left", "right"});
    }

    void addPackage(String... var1);

    IClass addClass(String... var1);

    INamedMappingFile build();

    interface IClass {
        IMappingBuilder.IField field(String... var1);

        IMappingBuilder.IMethod method(String var1, String... var2);

        void meta(String var1, String var2);
    }

    interface IField {
        IField descriptor(String var1);

        void meta(String var1, String var2);
    }

    interface IMethod {
        IMappingBuilder.IParameter parameter(int var1, String... var2);

        void meta(String var1, String var2);
    }

    interface IParameter {
        void meta(String var1, String var2);
    }
}
