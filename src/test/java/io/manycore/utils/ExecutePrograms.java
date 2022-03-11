package io.manycore.utils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExecutePrograms {

    private final String[] cmdArr;
    private final ProcessBuilder processBuilder;

    private Process process;
    private StreamConsumer stdout;
    private StreamConsumer stderr;
    private Thread stdoutThread;
    private Thread stderrThread;
    private int maxRowsLogged;

    public ExecutePrograms(String[] cmdArr) {
        this.cmdArr = cmdArr;
        this.processBuilder = new ProcessBuilder(this.cmdArr);
        this.maxRowsLogged = Integer.MAX_VALUE;
    }

    public ExecutePrograms(String[] cmdArr, File executeInThisDirectory) {
        this(cmdArr);
        processBuilder.directory(executeInThisDirectory);
    }

    public void setMaxRowsToLog(int val) {
        maxRowsLogged = val;
    }

    public void start() throws IOException {
        process = processBuilder.start();
        stdout = new StreamConsumer(process.getInputStream(), maxRowsLogged);
        stderr = new StreamConsumer(process.getErrorStream(), maxRowsLogged);
        stdoutThread = new Thread(stdout);
        stderrThread = new Thread(stderr);
        stdoutThread.start();
        stderrThread.start();
    }

    private int waitFor() throws InterruptedException {
        int res;

        res = process.waitFor();

        while (stdoutThread.isAlive()) {
            Thread.sleep(5);
        }

        stdoutThread.join();

        while (stderrThread.isAlive()) {
            Thread.sleep(5);
        }

        stderrThread.join();

        return res;
    }

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {

        if (process.waitFor(timeout, unit)) {

            while (stdoutThread.isAlive()) {
                Thread.sleep(5);
            }

            stdoutThread.join();

            while (stderrThread.isAlive()) {
                Thread.sleep(5);
            }

            stderrThread.join();

            return true;

        } else {
            // it did not exit!
            return false;
        }

    }

    public void killProcess() throws InterruptedException {
        if (process == null) {
            return;
        }

        if (! process.isAlive()) {
            return;
        }

        process.destroy();

        Thread.sleep(1000);

        if (process.isAlive()) {
            process.destroyForcibly();
        }

        while (stdoutThread.isAlive()) {
            Thread.sleep(5);
        }

        stdoutThread.join();

        while (stderrThread.isAlive()) {
            Thread.sleep(5);
        }

        stderrThread.join();
    }

    public int getExitValue() {
        return process.exitValue();
    }

    public StreamConsumer getStdout() {
        return stdout;
    }

    public StreamConsumer getStderr() {
        return stderr;
    }

    static public class StreamConsumer implements Runnable {
        private InputStream stream;
        private IOException problematicException;
        private List<String> output;
        private int loggedRowsCounter;

        StreamConsumer(InputStream stream, int maxRowsToLog) {
            this.stream = stream;
            this.problematicException = null;
            this.output = new LinkedList<>();
            this.loggedRowsCounter = maxRowsToLog;
        }

        public boolean hadProblems() {
            return problematicException != null;
        }

        public List<String> getOutput() {
            return output;
        }

        @Override
        public void run()
        {
            BufferedReader brCleanUp;

            InputStreamReader isr = new InputStreamReader(stream);
            brCleanUp = new BufferedReader (isr);

            String line;
            try {
                while ((line = brCleanUp.readLine ()) != null) {
                    if (loggedRowsCounter > 0) {
                        output.add(line);
                        loggedRowsCounter--;
                    }
                }
                brCleanUp.close();
            } catch (IOException e) {
                problematicException = e;
            }
        }
    }
}
