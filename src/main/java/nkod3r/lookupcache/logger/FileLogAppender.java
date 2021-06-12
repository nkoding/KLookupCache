package nkod3r.lookupcache.logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class FileLogAppender implements LogAppender{
    private final Path path;

    public FileLogAppender(String pathToLogFile) {
        this.path= Paths.get(pathToLogFile);
    }

    @Override
    public void info(String msg) {
        try {
            Files.write(path, msg.getBytes(StandardCharsets.UTF_8), CREATE, APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void error(String msg) {
        info(msg);
    }
}
