package net.matteo.networklogger.utils.mappings;

import java.util.List;

public interface INamedMappingFile {

    List<String> getNames();

    IMappingFile getMap(String var1, String var2);
}