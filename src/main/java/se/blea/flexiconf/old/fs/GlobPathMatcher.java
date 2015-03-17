package se.blea.flexiconf.old.fs;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds all files matching the provided glob
 *
 * @author Tristan Blease (tblease@groupon.com)
 * @version 0.0.1
 * @since 0.0.1
 */
public class GlobPathMatcher {
    private static final Path CWD = Paths.get("");
    private final PathMatcher pathMatcher;

    public GlobPathMatcher(String glob) {
        pathMatcher =  FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    public List<Path> match() throws IOException {
        final List<Path> matchedFiles = new ArrayList<>();

        Files.walkFileTree(CWD, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(file.normalize())) {
                    matchedFiles.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return matchedFiles;
    }
}
