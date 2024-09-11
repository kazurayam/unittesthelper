package com.kazurayam.unittest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ProjectDirectoryResolver resolves the project's directory based on the classpath
 * of a JVM class you are developing.
 * The following quote from
 * https://discuss.gradle.org/t/how-do-i-set-the-working-directory-for-testng-in-a-multi-project-gradle-build/7379/7
 * explains what I wanted to do.
 *
 * <blockquote>
 *     <p>luke_daley Gradle Employee Nov '13</p>
 *
 *     <p>Loading from the filesystem using relative paths during unit tests is problematic
 *     because different environments will set a different working directory for the test process.
 *     For example, Gradle uses the projects directory while IntelliJ uses the directory of the root project.</p>
 *
 *     <p>The only really safe way to solve this problem is to load via the classpath.
 *     Is this a possibility for your scenario?</p>
 * </blockquote>
 */
public final class ProjectDirectoryResolver {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDirectoryResolver.class);
    private final FileSystem fileSystem;
    private final List<CodeSourcePathElementsUnderProjectDirectory> listOfCSPEUPD;

    /**
     *
     */
    public ProjectDirectoryResolver() {
        this(FileSystems.getDefault());
    }

    public ProjectDirectoryResolver(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        listOfCSPEUPD = new ArrayList<>();
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.MAVEN_TEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_JAVA_TEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_JAVA_FUNCTIONALTEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_JAVA_MAIN);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_GROOVY_TEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_GROOVY_FUNCTIONALTEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_GROOVY_MAIN);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_KOTLIN_TEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_KOTLIN_FUNCTIONALTEST);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.GRADLE_KOTLIN_MAIN);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.IDEA_PRODUCTION);
        listOfCSPEUPD.add(CodeSourcePathElementsUnderProjectDirectory.IDEA_TEST);
    }

    /**
     *
     * @param cspeupd e.g, CodeSourcePathElementsUnderProjectDirectory of ["build", "classes", "main", "java"]
     *
     */
    public void addCodeSourcePathElementsUnderProjectDirectory(CodeSourcePathElementsUnderProjectDirectory cspeupd) {
        Objects.requireNonNull(cspeupd);
        if (cspeupd.isEmpty()) {
            throw new IllegalArgumentException("codeSourcePathElementsUnderProjectDirectory must not be null");
        }
        this.listOfCSPEUPD.add(cspeupd);
    }

    /**
     *
     * @return the list of Sublist Patterns including both builtin and ones you added
     */
    public List<CodeSourcePathElementsUnderProjectDirectory>
    getRegisteredListOfCodeSourcePathElementsUnderProjectDirectory() {
        List<CodeSourcePathElementsUnderProjectDirectory> clone = new ArrayList<>();
        for (CodeSourcePathElementsUnderProjectDirectory cspeupd : listOfCSPEUPD) {
            clone.add(new CodeSourcePathElementsUnderProjectDirectory(cspeupd));
        }
        return clone;
    }

    /*
     * to match "file:/Users/foo/bar", "/Users/foo/bar", "s3:/Users/foo/bar"
     */
    static final Pattern URL_PATTERN = Pattern.compile("(\\w+\\:)?(.+)");

    /**
     * returns the Path in which the class binary of
     * `com.kazurayam.unittest.ProjectDirectoryResolver` is found on disk.
     * e.g, "/Users/somebody/selenium-webdriver-java/selenium-webdriver-junit4/build/classes/java/test/" when built by Gradle
     * e.g, "/Users/somebody/selenium-webdriver-java/selenium-webdriver-junit4/target/test-classes/" when built by Maven
     * @param clazz the Class object based on which the project dir is resolved
     * @return a java.nio.file.Path instance which is derived by clazz.getProtectionDomain().getCodeSource().getLocation()
     */
    public Path getCodeSourcePathOf(Class<?> clazz) {
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        URL url = codeSource.getLocation();
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * returns the project directory in which the class binary of
     * `com.kazurayam.webdriver.TestOutputOrganizer` is found on disk.
     * The same value will be returned in either case where the class
     * was built by Gradle and Maven.
     * e.g, "/Users/somebody/selenium-webdriver-java/selenium-webdriver-junit4/"
     * The point is, the System property "user.dir" will return
     * "/Users/somebody/selenium-webdriver-java".
     * This does not include the subproject directory
     * "selenium-webdriver-junit4", therefore it is not what we want.
     * When the TestOutputOrganizer class is built using Gradle, the class will be stored
     * in the directory "selenium-webdriver-java/selenium-webdriver-junit4/build".
     * When the TestOutputOrganizer class is built using Maven, the class will be stored
     * in the directory "selenium-webdriver-java/selenium-webdriver-junit5/target".
     * So this method look up the file name "build" or "target" in the code source
     * of the TestOutputOrganizer class.
     * Then we can get the Path value of the project directory properly.
     *
     * @param clazz the Class object based on which the project dir is resolved
     * @return a java.nio.file.Path instance that represents the project directory.
     *         if the project is a Gradle Multi-Project, then this method returns the
     *         path of the subproject's root dir.
     */
    public Path resolveProjectDirectoryViaClasspath(Class<?> clazz) {
        // methodName == "resolveProjectDirectoryViaClasspath"
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        //
        Path codeSourcePath = this.getCodeSourcePathOf(clazz);
        // e.g, codeSourcePath is "/Users/kazurayam/github/unittest-helper/lib/build/classes/java/test/"
        logger.trace(String.format("[%s] codeSourcePath=%s", methodName, codeSourcePath));
        //
        Path root = codeSourcePath.getRoot();
        // root="/" on Mac/Linux
        // root="C:\\" on Windows
        logger.trace(String.format("[%s] root=%s", methodName, root));
        //
        List<String> nameElements = toPathElementNames(codeSourcePath);
        // nameElements : ["Users","kazurayam","github","unittest-helper","lib","build","classes","java","test"]
        logger.trace(String.format("[%s] nameElements=%s", methodName, nameElements));
        StringSequence ss = new StringSequence(nameElements);
        int boundary = -1;
        StringBuilder sb = new StringBuilder();
        for (CodeSourcePathElementsUnderProjectDirectory cspe : this.listOfCSPEUPD) {
            int indexOfBuildDir = ss.indexOf(cspe);
            if (indexOfBuildDir > 0) {
                boundary = indexOfBuildDir;
                logger.trace(String.format(
                        "[%s] CodeSourcePathElementsUnderProjectDirectory %s is found in the CodeSource %s at the index %d",
                        methodName,
                        cspe, ss, boundary));
                break;
            } else {
                logger.trace(String.format(
                        "[%s] sublistPattern %s is NOT found in the code source path %s",
                        methodName,
                        cspe, ss));
            }
            sb.append(cspe.toString());
            sb.append("\n");
        }
        if (boundary == -1) {
            throw new IllegalStateException(String.format(
                    "[%s] unable to resolve the project directory via classpath. Tried the following CodeSourcePathElement candidates:\n%s",
                    methodName, sb));
        }
        // build the project dir to return as the result
        Path w = root;
        for (int i = 0; i < boundary; i++) {
            w = w.resolve(nameElements.get(i));
        }
        return w;   // e.g, /Users/myname/oreilly/selenium-webdriver-java/selenium-webdriver-junit4
    }

    /**
     * Convert a Path into a List<String>, which is a list of path elements that comprises the given Path
     *
     * @param codeSourcePath e.g., "/Users/kazurayam/github/unittest-helper/build/classes/test/java"
     * @return e.g, ["Users","kazurayam","github","unittest-helper","build","classes","test","java"]
     */
    private static List<String> toPathElementNames(Path codeSourcePath) {
        List<String> pathElementNames = new ArrayList<>();
        for (Path p : codeSourcePath) {
            pathElementNames.add(p.getFileName().toString());
        }
        return pathElementNames;
    }
}
