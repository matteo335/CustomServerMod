package net.matteo.networklogger.utils.mappings;

public interface IRenamer {
    default String rename(IMappingFile.IPackage value) {
        return value.getMapped();
    }

    default String rename(IMappingFile.IClass value) {
        return value.getMapped();
    }

    default String rename(IMappingFile.IField value) {
        return value.getMapped();
    }

    default String rename(IMappingFile.IMethod value) {
        return value.getMapped();
    }

    default String rename(IMappingFile.IParameter value) {
        return value.getMapped();
    }
}
