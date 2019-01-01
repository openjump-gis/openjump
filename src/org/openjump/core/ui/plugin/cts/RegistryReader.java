package org.openjump.core.ui.plugin.cts;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class parses quickly a register to associate CRS codes with names.
 * Name of CRSs may be written in a comment just before the CRS definition
 */
public class RegistryReader {

    static Map<String,String> read(String registry) throws IOException {
        Map<String,String> map = new LinkedHashMap<String,String>();
        InputStream is = null;
        try {
            is = org.cts.registry.Registry.class.getResourceAsStream(registry.toLowerCase());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line, name = null, code;
            while (null != (line=br.readLine())) {
                if (line.startsWith("#")) {
                    name = line.substring(1).trim();
                } else if (line.startsWith("<")) {
                    code = line.substring(1, line.indexOf(">"));
                    map.put(code, code);
                    if (name != null && name.length() > 0) {
                        map.put(name, code);
                    }
                    name = null;
                } else {
                    name = null;
                }
            }
        } finally {
            if (is != null) try {is.close();} catch(IOException e){}
        }
        return map;
    }
}

