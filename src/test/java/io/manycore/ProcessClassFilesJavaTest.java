package io.manycore;

import io.manycore.privatetest.Privateer;
import io.manycore.utils.ExecutePrograms;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class ProcessClassFilesJavaTest {

    @Test
    public void test_hopefully_all_combinations() throws IOException, InterruptedException {
        File inputDir = new File("build/classes/java/test/io/manycore/privatetest");

        File outputDir = new File("src/test/java/io/manycore/classesOut/io/manycore/privatetest");
        if (outputDir.exists()) {
            Files.walk(outputDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }
        outputDir.mkdirs();

        ProcessClassFiles p = new ProcessClassFiles(inputDir, outputDir);
        p.process();

        // Try to run the new classes.
        ExecutePrograms ep = new ExecutePrograms(new String[]{"java", "-classpath", "src/test/java/io/manycore/classesOut", "io.manycore.privatetest.Main"});
        ep.start();
        ep.waitFor(3, TimeUnit.SECONDS);

        Assertions.assertEquals(0, ep.getExitValue());

        Privateer.publicTest();  // To ensure code is generated
    }

}
