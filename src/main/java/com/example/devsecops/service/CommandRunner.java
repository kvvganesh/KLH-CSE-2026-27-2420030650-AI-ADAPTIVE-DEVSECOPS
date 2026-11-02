package com.example.devsecops.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runs an external command (no shell), writing its output to a log file. */
public class CommandRunner {

    /** exit -1 = timed out, -2 = command not found. */
    public record Result(int exit, boolean timedOut, boolean notFound) {
    }

    public Result run(List<String> command, Path workingDir, Map<String, String> env, int timeoutMinutes, Path logFile) {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        builder.environment().putAll(env);
        try {
            Process process = builder.start();
            if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                return new Result(-1, true, false);
            }
            return new Result(process.exitValue(), false, false);
        } catch (IOException e) {
            return new Result(-2, false, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(-1, true, false);
        }
    }
}
