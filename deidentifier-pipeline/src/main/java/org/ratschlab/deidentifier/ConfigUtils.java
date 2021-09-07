package org.ratschlab.deidentifier;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.util.Optional;

public class ConfigUtils {

    /*
public static File resolvePath(Config conf, String key) {
    //conf.getStringList()
    //return //
}
*/

    public static Config loadConfig(File path) {
        //

        return ConfigFactory.parseFile(path).withValue("config_file_dir", ConfigValueFactory.fromAnyRef(path.getParentFile().toURI().toString())).resolve();
    }

    public static Optional<String> getOptionalString(Config conf, String key) {
        if(conf.hasPath(key)) {
            return Optional.of(conf.getString(key));
        } else {
            return Optional.empty();
        }
    }
}
