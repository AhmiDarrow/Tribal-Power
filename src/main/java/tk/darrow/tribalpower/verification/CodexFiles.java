package tk.darrow.tribalpower.verification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import tk.darrow.tribalpower.client.codex.CodexBook;

/** Reads the English Spirit Codex straight off the classpath, so server-side GameTests can check it. */
final class CodexFiles {
    private CodexFiles() {}

    static CodexBook.Book english() {
        try {
            var url = CodexFiles.class.getClassLoader().getResource("assets/tribalpower/codex/en_us/book.json");
            if (url == null) throw new IllegalStateException("assets/tribalpower/codex/en_us/book.json is missing");
            var uri = url.toURI();
            if (uri.getScheme().equals("jar")) {
                try { FileSystems.getFileSystem(uri); } catch (java.nio.file.FileSystemNotFoundException e) { FileSystems.newFileSystem(uri, Map.of()); }
            }
            Path root = Path.of(uri).getParent();
            Map<String, JsonObject> files = new TreeMap<>();
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        files.put(root.relativize(file).toString().replace('\\', '/'), JsonParser.parseReader(reader).getAsJsonObject());
                    }
                }
            }
            return CodexBook.build(files);
        } catch (IOException | URISyntaxException error) {
            throw new IllegalStateException("Could not read the Spirit Codex", error);
        }
    }
}
