package com.mdanetzky.streamscript;

import java.util.Scanner;

public class Resources {
    public static String read(String filePath) {
        return new Scanner(Resources.class.getResourceAsStream(filePath),
                "UTF-8").useDelimiter("\\A").next();
    }
}
