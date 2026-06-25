package org.levimc.launcher.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeImageGuard {
    public static final String TOKEN = "img_v4";
    private static final ConcurrentHashMap<String, FileState> CLEAN_FILES = new ConcurrentHashMap<>();

    private NativeImageGuard() {
    }

    public static boolean shouldProcess(File soFile) {
        return false;
    }

    public static boolean processIfNeeded(File soFile) {
        return true;
    }

    public static boolean processRequired(File soFile) {
        return true;
    }

    public static int processDirectory(File dir) {
        return 0;
    }

    private static File cleanMarker(File file) {
        File parent = file.getParentFile();
        String name = "." + file.getName() + "." + TOKEN + ".ok";
        return parent == null ? new File(name) : new File(parent, name);
    }

    private static void markClean(File file) {
        FileState state = FileState.from(file);
        CLEAN_FILES.put(file.getAbsolutePath(), state);
        try {
            File marker = cleanMarker(file);
            File parent = marker.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            String data = TOKEN + ":" + state.length + ":" + state.lastModified;
            Files.write(marker.toPath(), data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void clearCleanMarker(File file) {
        try {
            File marker = cleanMarker(file);
            if (marker.exists()) {
                marker.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static final class FileState {
        private final long length;
        private final long lastModified;

        private FileState(long length, long lastModified) {
            this.length = length;
            this.lastModified = lastModified;
        }

        static FileState from(File file) {
            return new FileState(file.length(), file.lastModified());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FileState)) {
                return false;
            }
            FileState other = (FileState) obj;
            return length == other.length && lastModified == other.lastModified;
        }

        @Override
        public int hashCode() {
            long value = length ^ (length >>> 32) ^ lastModified ^ (lastModified >>> 32);
            return (int) value;
        }
    }
}