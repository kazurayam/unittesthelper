package my;

import com.kazurayam.unittest.TestHelper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SampleTest {

    /*
     * will create a file `<projectDir>/sample1.txt`
     */
    @Test
    public void test_write_under_the_project_dir() throws Exception {
        Path p = Paths.get("sample1.txt");
        Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
    }

    /*
     * will create a file `<projectDir>/test-output/sample2.txt`
     */
    @Test
    public void test_write_into_the_default_dir() throws Exception {
        Path p = new TestHelper(this.getClass())
                .resolveOutput("sample2.txt");
        Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void test_write_into_subdir_under_the_default_dir() throws Exception {
        Path p = new TestHelper(this.getClass())
                .resolveOutput("sub/sample3.txt");
        Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
    }

    /*
     * will create a file `<projectDir>/build/tmp/testOutput/sample3.txt`
     */
    @Test
    public void test_write_into_custom_dir() throws Exception {
        Path p = new TestHelper(this.getClass())
                .setOutputDirPath(Paths.get("build/tmp/testOutput"))
                .resolveOutput("sample4.txt");
        Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
    }


    @Test
    public void test_write_into_another_custom_dir() throws Exception {
        Path outdir = new TestHelper(this.getClass())
                .setOutputDirPath(Paths.get("test-output-another"))
                .getOutputDir();
        Files.createDirectories(outdir);
        Path p = outdir.resolve("sample5.txt");
        Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
    }

}
