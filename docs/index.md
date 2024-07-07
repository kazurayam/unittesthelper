- Table of contents
{:toc}

# Unittest Helper

-   author: kazurayam

-   version: unknown

-   source project: <https://github.com/kazurayam/unittest-helper>

-   javadoc: <https://kazurayam.github.io/unittest-helper/api>

-   distributed at: <https://mvnrepository.com/artifact/com.kazurayam/unittest-helper>

## Problems to solve

Let me show you a set of sample codes to explain the problem that I am focused.

I would assume that you have a clone of the [unittest-helper](https://github.com/kazurayam/unittest-helper) project on your machine.

Please note that this project is a [Gradle Multi-Project](https://docs.gradle.org/current/userguide/intro_multi_project_builds.html), where the root directory `unittest-helper` contains a few sub-directories `lib`, `app` and `preliminary-study`. Each of these 3 directories contain `build.gradle` file. Multi-Project is just a usual style of Gradle project structure nowadays. It is no exceptional.

Please find [unittest-helper/preliminary-stydy/src/test/java/study/S2WritingSystemPropertyValueIntoFileInTheOutputDirectoryTest.java](https://github.com/kazurayam/unittest-helper/blob/develop/preliminary-study/src/test/java/study/S2WritingSystemPropertyValueIntoFileInTheOutputDirectoryTest.java):

    package study;

    import org.testng.annotations.Test;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.util.Optional;

    public class S2WritingSystemPropertyValueIntoFileInTheOutputDirectoryTest {

        @Test
        public void testIt() throws IOException {
            // receive a System property
            Optional<String> text =
                    Optional.ofNullable(System.getProperty("browserType"));
            System.out.println("System.property(\"browserType\")=" + text.orElse("null"));

            // write the "browserType" value into a file
            // in the "test-output" directory under the "user.dir"
            Path currentWorkingDirectory = Paths.get(System.getProperty("user.dir"));

            Path outputDir = currentWorkingDirectory.resolve("test-output");
            Path file = outputDir.resolve("browserType.txt");
            System.out.println("file path=" + TestUtils.shortenPath(file));
            if (text.isPresent()) {
                Files.createDirectories(file.getParent());
                Files.write(file, text.get().getBytes());
                System.out.println("file content=" +
                        String.join("", Files.readAllLines(file)));
            }
        }
    }

-   This code is a unit-test class in Java.

-   This test tries to read a Java System Property named `browserType` specified in the commandline option

-   This test creates a directory named `test-output`. The `test-output` directory will be located under the so-called **Current Working Directory** which is identified by the System Property `user.dir`.

-   This test writes the value of System Property `browserType` into a file named `browserType.txt` which is located in the `test-output` directory.

You can find how the Gradle task `testS2` is defined in the [unittest-helper/preliminary-study/build.gradle](https://github.com/kazurayam/unittest-helper/blob/develop/preliminary-study/build.gradle)

    tasks.register("testS2", Test) {
        useTestNG()                                  // (1)
        include "**/S2*"                             // (2)
        systemProperties System.properties           // (3)
        testLogging { showStandardStreams = true }   // (4)
        outputs.upToDateWhen { false }               // (5)
    }

1.  I declared that I want to use NestNG to run my test cases in the `testS2` task.

2.  I specifically chose a class of which className starts with `S2`.

3.  I passed all entries of `System.properties`, including the ones given as a commandline parameter by `-DbrowserType=XXXX`, into the `testS2` task

4.  I requested Gradle to show all messages emitted by the test classes into the stdout to be printed in the console

5.  I forced Gradle to run the `testS2` task even if there is no change in the input.

I opened the Terminal app, and ran the following commands.

### Case1: with the subproject’s directory as Current Working Directory

    $ cd ~/github/unittest-helper
    $ cd preliminary-study
    $ pwd
    /Users/kazurayam/github/unittest-helper/preliminary-study
    $ gradle testS2 -DbrowserType=Chrome

    > Task :preliminary-study:testS2

    Gradle suite > Gradle test > study.S2WritingSystemPropertyValueIntoFileInTheOutputDirectoryTest > testIt STANDARD_OUT
        System.property("browserType")=Chrome
        file path=~/github/unittest-helper/preliminary-study/test-output/browserType.txt
        file content=Chrome

    BUILD SUCCESSFUL in 2s
    2 actionable tasks: 1 executed, 1 up-to-date

Please note that the output directory `test-output` was located in the **subproject’s directory** `unittest-helper/preliminary-study/`.

### Case2: with the root project’s directory as Current Working Directory

Gradle allows us another way of invoking the same task `testS2`, as follows:

    $ pwd
    /Users/kazuakiurayama/github/unittest-helper
    :~/github/unittest-helper (issue36 *)
    $ gradle :preliminary-study:testS2 -DbrowserType=FireFox

    > Task :preliminary-study:testS2

    Gradle suite > Gradle test > study.S2WritingSystemPropertyValueIntoFileInTheOutputDirectoryTest > testIt STANDARD_OUT
        System.property("browserType")=FireFox
        file path=~/github/unittest-helper/test-output/browserType.txt
        file content=FireFox

    BUILD SUCCESSFUL in 3s
    2 actionable tasks: 1 executed, 1 up-to-date

I am surprised to find that, in the case2, the output directory `test-output` was located in the **root project’s directory**; like `unittest-helper/test-output`.

### Problem: Current Working Directory is not reliable: it moves sometimes

I want my test class `study.S2WritingSystemPropertyValueIntoFileInTheOutputDireoctyTest` to create the directory `test-output` steadily under the subproject’s directory.

In the Case1, I got `unittest-helper/preliminary-study/test-output`, with which I am OK. However, in the Case2, I got `unittest-helper/test-output`. I am not happy with the result.

<figure>
<img src="images/00_problem_to_solve.png" alt="00 problem to solve" />
</figure>

In two cases I ran the same test class just a little bit bit differently. In the Case1, I ran the test while the Current Working Directory is `unittest-helper/preliminary-study`, and in the Case2, I ran the test while the CWD is `unittest-helper`.

In the Case2, I got the `test-output` directory under the `unittest-helper` directory, which is the root project’s directory. Why did I get this result? …​ It is because the `testS2` task in the `build.gradle` has a single line:

    tasks.register("testS2", Test) {
        ...
        systemPropert System.properties
        ...
    }

By this single line, Gradle captured all the values of System.properties at the timing when I invoked `gradle` command and the values were transferred into the runtime environment where the test class `S2WrintingSystemPropertyValueIntoFileInTheOutputDirecvtoryTest` ran. In the Case1, the System Property `user.dir` had the value of `` /Users/kazurayam/github/unittest-helpers/preliminary-study; and in the Case2, the `user.dir `` had the value of `/Users/kazurayam/github/unittest-helper`. Therefore, in two cases, the test class got different value of `System.getProperty("user.dir")`.

The Current Working Directory, which is resolved `System.getProperty("user.dir")`, can move sometimes; not very much reliable for a test class to find where the "project directory" is. This is the problem I am focused.

## Solution

I want the `test-output` directory to be steadily located under the subproject’s directory `preliminary-study/` regardless at which directory the System property `user.dir` is set at runtime. For that intent, I do not like to rely on the value of System property `user.dir` to find out where the subproject directory is.

### How to find supProject’s directory without referring to `System.getProperty("user.dir")`?

There is a method for a test classes to find out the location of project’s directory without referring to the System property `user.dir`. I will show you a sample code how to.

Please find [unittest-helper/preliminary-study/src/test/java/study/S3FindingProjectDirByClasspathTest.java](https://github.com/kazurayam/unittest-helper/blob/develop/preliminary-study/src/test/java/study/S3FindingProjectDirByClasspathTest.java):

    package study;

    import org.testng.annotations.Test;

    import java.net.URL;
    import java.security.CodeSource;
    import java.security.ProtectionDomain;

    import static org.assertj.core.api.Assertions.assertThat;

    public class S3FindingProjectDirByClasspathTest {

        @Test
        public void getLocationWhereThisClassIsFound() {

            // THE MAGIC
            ProtectionDomain pd = this.getClass().getProtectionDomain();
            CodeSource codeSource = pd.getCodeSource();
            URL url = codeSource.getLocation();

            System.out.println("codeSource url=" + url.toString());
            // e.g, "url=file:/Users/kazurayam/github/unittest-helper/preliminary-study/build/classes/java/test/"
            assertThat(url.toString()).contains("unittest-helper/preliminary-study/build/classes/java/test");
            String codeSourcePathElementsUnderProjectDirectory = "build/classes/java/test/";
            String projectDir =
                    url.toString().replace(codeSourcePathElementsUnderProjectDirectory,"");
            System.out.println("project directory=" + TestUtils.shortenPath(projectDir));
        }
    }

You can run this test by `testS3` task defined in the `unittest-helper/preliminary-study/build.gradle`. I ran it and got messages, as follows:

    $ pwd
    /Users/kazuakiurayama/github/unittest-helper/preliminary-study

    $ gradle testS3

    > Task :preliminary-study:testS3

    Gradle suite > Gradle test > study.S3FindingProjectDirByClasspathTest > getLocationWhereThisClassIsFound STANDARD_OUT
        codeSource url=file:/Users/kazuakiurayama/github/unittest-helper/preliminary-study/build/classes/java/test/
        project directory=file:/Users/kazuakiurayama/github/unittest-helper/preliminary-study/

    BUILD SUCCESSFUL in 2s
    2 actionable tasks: 1 executed, 1 up-to-date

And also I ran the same task at the root project’s directory:

    $ pwd
    /Users/kazuakiurayama/github/unittest-helper

    $ gradle :preliminary-study:testS3

    > Task :preliminary-study:testS3

    Gradle suite > Gradle test > study.S3FindingProjectDirByClasspathTest > getLocationWhereThisClassIsFound STANDARD_OUT
        codeSource url=file:/Users/kazuakiurayama/github/unittest-helper/preliminary-study/build/classes/java/test/
        project directory=file:/Users/kazuakiurayama/github/unittest-helper/preliminary-study/

    BUILD SUCCESSFUL in 2s
    2 actionable tasks: 1 executed, 1 up-to-date

As you see, in both cases, the test class printed the same path string as the project directory:

        project directory=file:/Users/kazuakiurayama/github/unittest-helper/preliminary-study/

I like this result. The test class `study.S3FindingProjectDirByClasspathTest` proved that it can find the location of the subproject’s directory is without referring to `System.getProperty("user.dir")`.

### Introducing "Code Source Path Elements Under Project Directory"

In order to explain the design of the `study.S3FindingProjectDirByClasspathTest`, I need to introduce a new terminology "Code Source Path Elements Under Project Directory". I would use an acronym "CSPEUPD" for short when appropriate. Let me assume that a test class file has the actual path on my machine like this:

`/Users/kazurayam/github/unittest-helper/preliminary-study/build/classes/java/test/study/S3FindingProjectDirByClasspathTest.class`

I can analyze this long path string into 4 segments, as follows:

-   `/Users/kazurayam/github/unittest-helper/`: Gradle **root project’s directory**

-   `preliminary-study/`: Gradle **subProject’s directory**:

-   `build/classes/java/test/`: [**Code Source Path Elements Under Project Directory**](https://github.com/kazurayam/unittest-helper/blob/issue36/lib/src/main/java/com/kazurayam/unittest/CodeSourcePathElementsUnderProjectDirectory.java)

-   `study/S3FindingProjectDirByClasspathTest.class`: the test class

### "Code Source Path Elements Under Project Directory" differs according to runtime environments

Read the source of `getLocationWhereThisClassIsFound` method of [study.S3FindingProjectDirByClasspathTest](https://github.com/kazurayam/unittest-helper/preliminary-study/src/test/java/study/S3FindingProjectDirByClasspathTest.java). The method has a fragment:

            String codeSourcePathElementsUnderProjectDirectory = "build/classes/java/test/";
            String projectDir =
                    url.toString().replace(codeSourcePathElementsUnderProjectDirectory,"");

Here you find a string literal `build/classes/java/test/`, which is a concatenation of path elements under the project directory, is valid only in a Gradle project in Java language. Different string literals would be required for other Languages (Groovy, Kotlin), for other Build Tools (Gradle, Maven, Ant), for other IDEs (IntelliJ IDEA, Eclipse, NetBeans, etc). The CSPEUPD is dependent on the runtime environment. If I want my test classes to be able to find the subProject’s directory runtime, my test classes have to try the possible values of CSPEUPD to match the actual URL of the CodeSource of the test class at runtime.

Well, it is a complicated processing. This requires good programming efforts.

The `com.kazurayam.unittest.ProjectDirectoryResolver` class provides a solution to this technical issue.

## Sample Codes

### Example-A1 Resolving the project directory resolved via classpath

I want my tests to be able to resolve the path of the project directory so that my tests can locate temporary output files surely under the project directory. And I want my tests to be independent of the **current working directory**. I want to find out the project directory’s path value based on the path value of the class file of the test itself. The following code shows it is possible.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.CodeSourcePathElementsUnderProjectDirectory;
    import com.kazurayam.unittest.ProjectDirectoryResolver;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.nio.file.Path;
    import java.util.List;

    import static org.assertj.core.api.Assertions.assertThat;

    public final class ProjectDirectoryResolverTest {

        Logger log = LoggerFactory.getLogger(ProjectDirectoryResolverTest.class);

        @Test
        public void test_getProjectDirViaClasspath() {
            ProjectDirectoryResolver resolver = new ProjectDirectoryResolver();
            Path projectDir = resolver.resolveProjectDirectoryViaClasspath(ProjectDirectoryResolverTest.class);
            log.info("projectDir: " + projectDir);
        }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ProjectDirectoryResolverTest.java)

This will print the following in the console:

    projectDir: /Users/kazurayam/github/unittest-helper/app

How the `com.kazurayam.unittest.ProjectDirectoryResolver` class find the path of project directory via classpath? --- I will describe the detail later. For now, let me talk about how to utilize this library.

### Example-A2 Printing the list of registered "Code Source Path Elements Under Project Directory"

        @Test
        public void test_getRegisteredListOfCodeSourcePathElementsUnderProjectDirectory() {
            List<CodeSourcePathElementsUnderProjectDirectory> listOfCSPEUPD =
                    new ProjectDirectoryResolver().getRegisteredListOfCodeSourcePathElementsUnderProjectDirectory();
            assertThat(listOfCSPEUPD).isNotNull();
            assertThat(listOfCSPEUPD.size()).isGreaterThanOrEqualTo(2);
            String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
            log.info("[" + methodName + "]");
            for (CodeSourcePathElementsUnderProjectDirectory cspeupd : listOfCSPEUPD) {
                log.info("CodeSourcePathElementsUnderProjectDirectory: " + cspeupd);
            }
        }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ProjectDirectoryResolverTest.java)

This will print the following in the console:

    [test_getRegisteredListOfCodeSourcePathElementsUnderProjectDirectory]
    CodeSourcePathElementsUnderProjectDirectory: target/test-classes/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/java/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/java/functionalTest/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/groovy/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/groovy/functionalTest/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/kotlin/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/kotlin/functionalTest/

This is the list of `com.kazurayam.unittest.CodeSourcePathElementsUnderProjectDirectory` objects registered in the `com.kazurayam.unittest.ProjectDirectoryResolver` class as default.

### Example-A3 adding a custom instance of "Code Source Path Elements Under Project Directory"

        @Test
        public void test_addCodeSourcePathElementsUnderProjectDirectory() {
            ProjectDirectoryResolver pdr = new ProjectDirectoryResolver();
            pdr.addCodeSourcePathElementsUnderProjectDirectory(
                    new CodeSourcePathElementsUnderProjectDirectory("out", "bin"));
            String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
            log.info("[" + methodName + "]");
            List<CodeSourcePathElementsUnderProjectDirectory> listOfCSPEUPD =
                    pdr.getRegisteredListOfCodeSourcePathElementsUnderProjectDirectory();
            for (CodeSourcePathElementsUnderProjectDirectory cspeupd : listOfCSPEUPD) {
                log.info("CodeSourcePathElementsUnderProjectDirectory: " + cspeupd);
            }
        }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ProjectDirectoryResolverTest.java)

This will print the following in the console:

    [test_addCodeSourcePathElementsUnderProjectDirectory]
    CodeSourcePathElementsUnderProjectDirectory: target/test-classes/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/java/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/java/functionalTest/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/groovy/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/groovy/functionalTest/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/kotlin/test/
    CodeSourcePathElementsUnderProjectDirectory: build/classes/kotlin/functionalTest/
    CodeSourcePathElementsUnderProjectDirectory: out/bin/

Please find "out/bin/" is inserted in the list. The "out/bin/" is a Code Source Path Elements generated by IntelliJ IDEA as its native Java project hierarchy.

You can add any more patterns of "Code Source Path Elements" as you like.

### Example-A4 Specifying the project directory explicitly

If your project has a unique directory tree (not a Maven/Gradle project), you can not depend on the project-directory-resolution via classpath. Instead, you can construct a `TestOutputOrganizer` object while specifying a concrete `Path` as the project directory.

    package com.kazurayam.unittesthelperdemo;

    import org.junit.jupiter.api.Test;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import com.kazurayam.unittest.TestOutputOrganizer;

    import static org.assertj.core.api.Assertions.assertThat;

    public class ExampleA4Test {

        @Test
        public void test_specify_project_directory() throws Exception {
            // setup
            Path userHome = Paths.get(System.getProperty("user.home"));
            Path projDir = userHome.resolve("ExampleA4Test");
            Files.createDirectories(projDir);
            // when
            TestOutputOrganizer too =
                    new TestOutputOrganizer.Builder(this.getClass())
                            .projectDirectory(projDir) // specify the project dir
                            .build();
            Path outputDir = too.cleanOutputDirectory();
            // then
            assertThat(outputDir).exists();
            assertThat(outputDir.getParent()).isEqualByComparingTo(projDir);
            assertThat(outputDir.getFileName().toString()).isEqualTo("test-output");
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleA4Test.java)

### Example-A5 Want to resolve the project directory via classpath of a class under the "build/classes/java/main" directory

As shown in the Example-A2, the `ProjectDirectoryResolver` knows the `build/classes/java/test/` as a registered "Code Source Path Element under project directory" as default. But it does not know the `build/classes/java/main/`. Now I want to resolve the project directory via classpath of my `build/classes/java/main/com/example/App.class`. How can I do it?

Yes, you can do it. See the following code example.

Please find how the `CodeSourcePathELementsUnderProjectDirectory` class is used in the `com.example.App` class.

    package com.example;

    import com.kazurayam.unittest.CodeSourcePathElementsUnderProjectDirectory;
    import com.kazurayam.unittest.TestOutputOrganizer;

    import java.io.IOException;
    import java.nio.file.Path;
    import java.nio.file.Files;

    public class App {

        private static TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(App.class)
                        .addCodeSourcePathElementsUnderProjectDirectory(
                                new CodeSourcePathElementsUnderProjectDirectory(
                                        "build", "classes", "java", "main"))
                        .subOutputDirectory(App.class)
                        .build();

        public Path sayHelloTo(String name) throws IOException {
            Path outputDirectory = too.cleanClassOutputDirectory();
            Path output = outputDirectory.resolve("greeting.txt");
            Files.writeString(output, "Hello, " + name + "!");
            return output;
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/main/java/com/example/App.java)

    package com.kazurayam.unittesthelperdemo;

    import com.example.App;
    import org.junit.jupiter.api.Test;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;

    import static org.junit.jupiter.api.Assertions.assertTrue;

    public class ExampleA5Test {

        @Test
        public void test_com_example_App() throws IOException {
            App app = new App();
            Path output = app.sayHelloTo("William Tell");
            assertTrue(Files.exists(output));
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleA5Test.java)

### Example-B1 Locating the default output directory

I want to create a directory named `test-output` under the project directory. I would let my test classes to write files into the directory. I want to for output files `test-output` directory by calling `getOutputDir()`.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.file.Path;

    public class ExampleB1Test {

        Logger log = LoggerFactory.getLogger(ExampleB1Test.class);

        @Test
        public void test_getOutputDir_as_default() throws IOException {
            TestOutputOrganizer too = new TestOutputOrganizer.Builder(this.getClass()).build();
            Path outputDir = too.createOutputDirectory();
            log.info("[test_getOutputDir_as_default] " + outputDir);
            log.info("[test_getOutputDir_as_default] " +
                    too.toHomeRelativeString(outputDir));
        }

    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleB1Test.java)

This will print the following in the console:

    [test_getOutputDir_as_default] /Users/kazurayam/github/unittest-helper/app/test-output
    [test_getOutputDir_as_default] ~/github/unittest-helper/app/test-output

The default name of the **output directory** is `test-output`. You can explicitly set the name of of the output directory. I will explain it later.

With `toHomeRelativePath(Path p)` method, you can convert a full path string into a path string realtive to the Home directory of the OS user. This is useful for documentation purposes.

### Example-B2 Creating a custom output directory

The `com.kazurayam.unittest.TestOutputOrganizer` creates a directory with `getOutputDirectory()` and the default name is `test-output`. You may want some other name. Of course you can do it.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.file.Path;

    public class ExampleB2Test {

        Logger log = LoggerFactory.getLogger(ExampleB2Test.class);

        @Test
        public void test_getOutputDir_custom() throws IOException {
            TestOutputOrganizer too =
                    new TestOutputOrganizer.Builder(this.getClass())
                            .outputDirectoryRelativeToProject("test-output-another")
                            .build();
            Path outputDir = too.createOutputDirectory();
            log.info("[test_getOutputDir_custom] " +
                    too.toHomeRelativeString(outputDir));
        }

        @Test
        public void test_getOutputDir_custom_more() throws IOException {
            TestOutputOrganizer too = new TestOutputOrganizer.Builder(this.getClass())
                    .outputDirectoryRelativeToProject("build/tmp/testOutput")
                    .build();
            Path outputDir = too.createOutputDirectory();
            log.info("[test_getOutputDir_custom_more] " +
                    too.toHomeRelativeString(outputDir));
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleB2Test.java)

This will print the following in the console:

    [test_getOutputDir_custom] ~/github/unittest-helper/app/test-output-another
    [test_getOutputDir_custom_more] ~/github/unittest-helper/app/build/tmp/testOutput

### Example-C1 Writing a file into the default output directory

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.List;

    public class ExampleC1Test {

        Logger log = LoggerFactory.getLogger(ExampleC1Test.class);

        @Test
        public void test_write_a_file_into_the_default_output_directory() throws Exception {
            TestOutputOrganizer too = new TestOutputOrganizer.Builder(this.getClass()).build();
            Path file = too.createOutputDirectory().resolve("sample.txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            log.info("[test_write_a_file_into_the_default_output_directory] " +
                    too.toHomeRelativeString(file));
            List<String> content = Files.readAllLines(file);
            log.info(content.toString());
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleC1.java)

This will print the following in the console:

    [test_write_into_the_default_output_directory] ~/github/unittest-helper/app/test-output/sample.txt
    [Hello, world!]

### Example-C2 Create a subdirectory under the default output directory, write a file into it

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.List;

    public class ExampleC2Test {

        Logger log = LoggerFactory.getLogger(ExampleC2Test.class);

        @Test
        public void test_write_into_subdir_under_the_default_output_directory() throws Exception {
            TestOutputOrganizer too = new TestOutputOrganizer.Builder(this.getClass()).build();
            Path file = too.createOutputDirectory().resolve("sub/sample.txt");
            // you need to make sure that the parent directory exists
            Files.createDirectories(file.getParent());

            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            log.info("[test_write_into_subdir_under_the_default_output_directory] " +
                    too.toHomeRelativeString(file));
            List<String> content = Files.readAllLines(file);
            log.info(content.toString());
        }

    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleC2Test.java)

This will print the following in the console:

    [test_write_into_subdir_under_the_default_output_directory] p = ~/github/unittest-helper/app/test-output/sub/sample.txt

You can create layers of sub-directories under the output directory managed by the `TestOutputOrganizer`. But you are responsible to make sure the parent directory of a file is present. So the following statement was necessary:

            Path file = too.getOutputDirectory().resolve("sub/sample.txt");
            // you need to make sure that the parent directory exists
            Files.createDirectories(file.getParent());

### Example-C3 Create a custom output directory, write a file into it

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.List;

    import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

    public class ExampleC3Test {

        Logger log = LoggerFactory.getLogger(ExampleC3Test.class);

        @Test
        public void test_write_into_the_custom_output_directory() throws Exception {
            TestOutputOrganizer too =
                    new TestOutputOrganizer.Builder(this.getClass())
                            .outputDirectoryRelativeToProject("build/tmp/testOutput").build();
            Path file = too.createOutputDirectory().resolve("sample.txt");
            // you do not have to make sure that the parent directory exists
            // Files.createDirectories(file.getParent());

            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
            log.info("[" + methodName + "] " +
                    too.toHomeRelativeString(file));
            List<String> content = Files.readAllLines(file);
            log.info(content.toString());

            assertThat(too.createOutputDirectory().getFileName().toString()).isEqualTo("testOutput");
        }

    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleC3Test.java)

This will print the following in the console:

    [test_write_into_subdir_under_the_custom_output_directory] ~/github/unittest-helper/app/build/tmp/testOutput/sample.txt
    [Hello, world!]

The `Path getOutputDirectory()` method makes sure that the directory is existing. If not present, the method will silently create it.

### Example-C4 Create subdirectory which stands for the Fully Qualified Class Name of test class

It is a good idea to create a layer of sub-directories, under the output directory, which stands for the Fully Qualified Class Name of the test classes. Please have a look at the following image.

![well organized test outputs](images/well-organized-test-outputs.png)

This directory organization is nice because we can easily see which test class created which file.

Also this organization allows you to name files with duplicating names, for example `my-pdf.pdf`. In the above screenshot, you can find 3 files of that name in 3 different sub-directories.

The following code shows how to use "ClassOutputDirectory" managed by `TestOutputOrganizer`.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.stream.Collectors;

    import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

    public class ExampleC4Test {

        private static final Logger log = LoggerFactory.getLogger(ExampleC4Test.class);
        private static final TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(ExampleC4Test.class)
                        .outputDirectoryRelativeToProject("build/tmp/testOutput")
                        .subOutputDirectory(ExampleC4Test.class)
                        .build();

        @BeforeAll
        public static void beforeAll() throws IOException {
            too.cleanClassOutputDirectory();
        }

        @Test
        public void test_write_a_file() throws Exception {
            // when
            Path classOutputDir = too.createClassOutputDirectory();
            log.info("[test_write_a_file] classOutputDir: " +
                    too.toHomeRelativeString(classOutputDir));

            Path file = classOutputDir.resolve("sample.txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            log.info("[test_write_a_file] created a file " +
                    too.toHomeRelativeString(file));
            // then
            assertThat(numberOfChildren(classOutputDir)).isEqualTo(1);
        }

        int numberOfChildren(Path dir) throws IOException {
            return Files.list(dir).collect(Collectors.toList()).size();
        }

    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleC4Test.java)

Please find that this test calls the `getClassOutputDirectory()` method and the `cleanClassOutputDirectory()` method of `TestOutputOrganizer` class. In order to make these methods operational, you MUST specify `.subDirPath(Clazz<?> testClass)` to the `TestOutputOrganizer.Builder`.

This test prints the following:

    [test_write_a_file] classOutputDir: ~/github/unittest-helper/app/build/tmp/testOutput/com.kazurayam.unittesthelperdemo.ExampleC4Test
    [test_write_a_file] created a file ~/github/unittest-helper/app/build/tmp/testOutput/com.kazurayam.unittesthelperdemo.ExampleC4Test/sample.txt

### Example-C5 One more layer of subdirectory that stands for the test method name

The Example-C4 showed that you can create a subdirectory which stands for the Fully Qualified Class Name of the test class. You can add one more layer of subdirectory which stands for the method name. This is useful for organizing the outputs in further detail.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.AfterAll;
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;

    public class ExampleC5Test {

        private static final Logger log = LoggerFactory.getLogger(ExampleC5Test.class);
        private static final TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(ExampleC5Test.class)
                        .subOutputDirectory(ExampleC5Test.class)
                        .build();
        private static LocalDateTime timestamp;

        @BeforeAll
        public static void beforeAll() throws Exception {
            timestamp = LocalDateTime.now();
        }

        @Test
        public void testMethod1() throws Exception {
            too.cleanMethodOutputDirectory("testMethod1");
            Path methodDir = too.createMethodOutputDirectory("testMethod1");
            Path file = methodDir.resolve(DateTimeFormatter.ISO_DATE_TIME.format(timestamp) + ".txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        public void testMethod2() throws Exception {
            too.cleanMethodOutputDirectory("testMethod2");
            Path methodDir = too.createMethodOutputDirectory("testMethod2");
            Path file = methodDir.resolve(DateTimeFormatter.ISO_DATE_TIME.format(timestamp) + ".txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        public void testMethod3() throws Exception {
            too.cleanMethodOutputDirectory("testMethod3");
            Path methodDir = too.createMethodOutputDirectory("testMethod3");
            Path file = methodDir.resolve(DateTimeFormatter.ISO_DATE_TIME.format(timestamp) + ".txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
        }

        @AfterAll
        public static void afterAll() throws IOException {
            Files.find(too.createClassOutputDirectory(), 999, (p, bfa) -> bfa.isRegularFile())
                    .sorted()
                    .forEach(p -> log.info(too.toHomeRelativeString(p)));
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleC5Test.java)

Please find that this test utilizes the `getMethodOutputDirectory(String methodName)` method and `cleanMethodOutputDirectory(String methodName)` method of `TestOutputOrganizer` class.

When I ran this, I got the following output:

    ~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleC5Test/testMethod1/2023-11-27T15:33:28.362.txt
    ~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleC5Test/testMethod2/2023-11-27T15:33:28.362.txt
    ~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleC5Test/testMethod3/2023-11-27T15:33:28.362.txt

Here you can see

1.  The classOutputDirectory is `~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleC5Test`

2.  Under the classOutputDirectory, there is a layer of methodDirectory, that is:

    -   `testMethod1`

    -   `testMethod2`

    -   `testMethod3`

3.  Even if you repeat executing this test, you would see only single txt file named with timestamp in each method directory, because `too.cleanMethodOutputDirectory()` cleans up the directory everytime the methods are invoked.

### Example-D1 Cleaning the output directory recursively

The `TestOutputOrganizer` class implements

-   `cleanOutputDirectory()`

-   `cleanClassOutputDirectory()`

-   `cleanMethodOutputDirectory(String methodName)`

These methods remove the respective directories recursively and re-create them. See the following sample test class.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.nio.file.Path;

    public class ExampleD1Test {

        private static final Logger log = LoggerFactory.getLogger(ExampleD1Test.class);
        private static final TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(ExampleD1Test.class)
                        .subOutputDirectory(ExampleD1Test.class)
                        .build();
        @BeforeAll
        public static void beforeAll() throws Exception {
            log.info("projectDir=" + too.toHomeRelativeString(too.getProjectDirectory()));
            too.cleanOutputDirectory();
            log.info("outputDirectory=" + too.toHomeRelativeString(too.createOutputDirectory()));
            too.cleanClassOutputDirectory();
            log.info("classOutputDirectory=" + too.toHomeRelativeString(too.createClassOutputDirectory()));
        }

        @Test
        public void testMethod1() throws Exception {
            too.cleanMethodOutputDirectory("testMethod1");
            Path methodDir = too.createMethodOutputDirectory("testMethod1");
            log.info("methodOutputDirectory=" + too.toHomeRelativeString(methodDir));
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleD1Test.java)

This will print the following in the console:

    projectDir=~/github/unittest-helper/app
    outputDirectory=~/github/unittest-helper/app/test-output
    classOutputDirectory=~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleD1Test
    methodOutputDirectory=~/github/unittest-helper/app/test-output/com.kazurayam.unittesthelperdemo.ExampleD1Test/testMethod1

### Example-E1 A helper method that translates an absolute Path to a `user.home`-relative string

A Path object can be turned into an absolute path string like:

    /Users/kazurayam/github/unittest-helper/lib/

In this string you can find my personal name "kazurayam". When I write some document, often I need to copy the output message and paste it into documents. However, I hesitate exposing my personal name public. I would prefer present a path in **Home Relative string** which starts with a tilde character, like:

    ~/github/unittest-helper/lib/

The `TestOutputOrganizer` class provides a helper method that translte a Path object into a Home Relative string. See the following example:

    package com.kazurayam.unittesthelperdemo;


    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.file.Path;

    public class ExampleE1Test {

        private static final Logger log = LoggerFactory.getLogger(ExampleE1Test.class);
        private static final TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(ExampleE1Test.class)
                        .build();
        @Test
        public void test_smoke() throws IOException {
            Path outputDir = too.createOutputDirectory();
            Path file = outputDir.resolve("sample1.txt");
            log.info("file absolute: " + file);
            log.info("file relative: " + too.toHomeRelativeString(file));
        }
    }

This test prints the following output in the console:

    file absolute: /Users/kazurayam/github/unittest-helper/app/test-output/sample1.txt
    file relative: ~/github/unittest-helper/app/test-output/sample1.txt

### Example-F1 Removing a directory recursively

The `com.kazurayam.unittest.DeleteDir` class implements a static method `cleanDirectoryRecursively(Path)` method which removes any directory recursively. See the following sample test class.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.DeleteDir;
    import org.junit.jupiter.api.Test;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;

    import static org.assertj.core.api.Assertions.assertThat;

    public class ExampleF1Test {

        @Test
        public void test_deleteDirectoryRecursively() throws IOException {
            // given
            Path dir = Paths.get("build/work");
            Files.createDirectories(dir);
            Path file = dir.resolve("foo.txt");
            Files.write(file, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            // when
            DeleteDir.deleteDirectoryRecursively(dir);
            // then
            assertThat(file).doesNotExist();
            assertThat(dir).doesNotExist();
        }
    }

[source](https://github.com/kazurayam/unittest-helper/blob/develop/app/src/test/java/com/kazurayam/unittesthelperdemo/ExampleF1Test.java)

The `cleanDirectoryRecursively(Path dir)` of `DeleteDir` class removes the specified directory and its content files/directories recursively. The dir will disappear. If you want to recreate the `dir` as empty dir, use `java.nio.file.Files.createDirectories(Path dir)`.

### Example-F2 Copying a source directory to a target directory recursively

`com.kazurayam.unittest.TestOutputOrganizer` class has an instance method `copyDir(Path source, Path target)` which just copies the content files/directories from the source into the target recursively.

    package com.kazurayam.unittesthelperdemo;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.Test;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;

    import static org.assertj.core.api.Assertions.assertThat;

    public class ExampleF2Test {

        private static final TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(ExampleF2Test.class)
                        .subOutputDirectory(ExampleF2Test.class)
                        .build();

        @Test
        void test_copyDir() throws IOException {
            Path methodDir = too.createMethodOutputDirectory("test_copyDir");
            // given
            Path sourceDir = methodDir.resolve("source");
            Path sourceFile = sourceDir.resolve("foo/hello.txt");
            Files.createDirectories(sourceFile.getParent());
            Files.write(sourceFile, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            Path targetDir = methodDir.resolve("target");
            // when
            too.copyDir(sourceDir, targetDir);
            // then
            Path targetFile = targetDir.resolve("foo/hello.txt");
            assertThat(targetFile).exists();
        }

    }

I know I can do the same dir-to-dir copy by [FileUtils of Apache Commons IO](https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FileUtils.html#copyDirectory(java.io.File,java.io.File)). If I use the `TestOutputOrganizer.copyDir(Path source, Path target)`, I can simplify the dependency of my project. That’s a small but good reason.

### Example-G1 Factory class that instantiates a customized TestOutputOrganizer

It is a good practice for you to define a factory class that creates an instance of `com.kazurayam.unittest.TestOutputOrganizer` with your customized parameters for your own project. Use the factory throughout your project. Then you can standardize the organization of test outputs.

See the following code for sample "factory" class.

    package io.github.someone.examples;

    import com.kazurayam.unittest.TestOutputOrganizer;

    /**
     * A Factory class that creates an instance of com.kazurayam.unittest.TestHelper
     * initialized with custom values of "outputDirPath" and "subDirPath"
     */
    public class TestOutputOrganizerFactory {

        public static TestOutputOrganizer create(Class<?> clazz) {
            return new TestOutputOrganizer.Builder(clazz)
                    .outputDirectoryRelativeToProject("build/tmp/testOutput")
                    .subOutputDirectory(clazz.getName())
                        // e.g, "io.github.somebody.somestuff.SampleTest"
                    .build();
        }
    }

This `TestOutputOrganizerFactory` class implements only a single static method `create(Class)` method which will instantiate a `com.kazurayam.unittest.TestOutputOrganizer` class with customized parameter values.

The following code is using the Factory.

    package io.github.someone.examples;

    import com.kazurayam.unittest.TestOutputOrganizer;
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.Test;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;

    import static org.assertj.core.api.Assertions.assertThat;

    public class SampleTest {

        private static TestOutputOrganizer too =
                new TestOutputOrganizer.Builder(SampleTest.class)
                        .subOutputDirectory(SampleTest.class).build();

        private static DateTimeFormatter dtf;
        private static final Logger log = LoggerFactory.getLogger(SampleTest.class);

        @BeforeAll
        public static void beforeAll() throws IOException {
            log.info("project directory: " + too.toHomeRelativeString(too.getProjectDirectory()));
            // remove the "test-output/io.github.someone.somestuff.SampleTest" directory recursively
            too.cleanClassOutputDirectory();
            dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        }

        @Test
        public void test_write_file() throws IOException {
            Path methodOutputDir = too.createMethodOutputDirectory("test_write_file");
            LocalDateTime ldt = LocalDateTime.now();
            Path p = methodOutputDir.resolve(String.format("sample_%s.txt", dtf.format(ldt)));
            Files.write(p, "Hello, world!".getBytes(StandardCharsets.UTF_8));
            assertThat(p).isNotNull().exists();
            assertThat(p.toFile().length()).isGreaterThan(0);

When you ran the test, the output directory will look like this:

    app/build/tmp/testOutput
    └── io.github.someone.examples.SampleTest
        └── test_write_file
            └── sample_20231103_094817.txt

(FIN)
