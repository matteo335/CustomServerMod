package net.matteo.networklogger.mappings;

import net.matteo.networklogger.utils.mappings.IMappingFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MappingComposer {

    public static MappingTable compose(InputStream joinedTsrg, InputStream mojangProGuard) throws IOException {
        IMappingFile srgToNotch = IMappingFile.load(joinedTsrg).reverse();
        IMappingFile notchToMojang = IMappingFile.load(mojangProGuard).reverse();
        IMappingFile srgToMojang = srgToNotch.chain(notchToMojang);
        Map<String, String> out = new HashMap<>(64000);

        for(IMappingFile.IClass c : srgToMojang.getClasses()) {
            for(IMappingFile.IMethod m : c.getMethods()) {
                String src = m.getOriginal();
                String dst = m.getMapped();
                if (dst != null && src.startsWith("m_") && !src.equals(dst)) {
                    out.put(src, dst);
                }
            }

            for(IMappingFile.IField f : c.getFields()) {
                String src = f.getOriginal();
                String dst = f.getMapped();
                if (dst != null && src.startsWith("f_") && !src.equals(dst)) {
                    out.put(src, dst);
                }
            }
        }

        return new MappingTable(Map.copyOf(out));
    }
}
