package com.indeed.util.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author jplaisance
 */
public final class NativeFileUtils {

    private static final Logger log = LoggerFactory.getLogger(NativeFileUtils.class);

    public static long du(String path) throws IOException {

        return du(new File(path));
    }

    public static long du(File path) throws IOException {
        Stat stat;
        try {
            stat = Stat.lstat(path);
        } catch (FileNotFoundException e) {
            return 0;
        }
        if (stat.isDirectory()) {
            long sum = 0;
            File[] files = path.listFiles();
            if (files == null) return 0;
            for (File f : files) {
                sum+=du(f);
            }
            return sum + 512 * stat.getNumBlocks();
        }
        return 512 * stat.getNumBlocks();
    }
}
