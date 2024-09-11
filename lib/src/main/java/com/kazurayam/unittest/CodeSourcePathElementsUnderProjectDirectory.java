package com.kazurayam.unittest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class gives a categorical name to the part {@code "build/classes/java/test/"} in a URL string
 * of CodeSource object of a Java class, like
 * {@code "file:/Users/kazurayam/github/unittest-helper/preliminary-study/build/classes/java/test/"}.
 * The CodeSourcePathElementUnderProjectDirectory (CSPEUPD for short) is dependent on the environment
 * in which the class runs.
 *
 * <pre>{@code
 * import java.net.URL;
 * import java.security.CodeSource;
 * import java.security.ProjectionDomain;
 * import org.testng.annotation.Test;
 *
 * public class S3FindingCodeSource() {
 *     @Test
 *     public void getLocationWhereTisClassIsFound() {
 *         ProtectionDomain pd = this.getClass().getProtectionDomain();
 *         CodeSource cs = pd.getCodeSource();
 *         URL url = cs.getLocation();
 *         System.out.println("codeSource URL=" + url.toString());
 *     }
 * }
 * }</pre>
 *
 * This will print something like this:
 *
 * <pre>{@code
 * codeSource URL=file:/Users/kazurayam/github/unittest-helper/preliminary-study/build/classes/java/test/
 * }</pre>
 *
 * I call {@code file:/Users/kazurayam/github/unittest-helper/preliminary-study/} as the "Project Directory",
 * {@code build/classes/java/test/} as the "CodeSource Path Elements Under Project Directory".
 *
 *
 */
public final class CodeSourcePathElementsUnderProjectDirectory {

    public static CodeSourcePathElementsUnderProjectDirectory MAVEN_TEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "target", "test-classes");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_JAVA_TEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "java", "test");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_JAVA_FUNCTIONALTEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "java", "functionalTest");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_JAVA_MAIN =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "java", "main");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_GROOVY_TEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "groovy", "test");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_GROOVY_FUNCTIONALTEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "groovy", "functionalTest");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_GROOVY_MAIN =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "groovy", "main");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_KOTLIN_TEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "kotlin", "test");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_KOTLIN_FUNCTIONALTEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "kotlin", "functionalTest");

    public static CodeSourcePathElementsUnderProjectDirectory GRADLE_KOTLIN_MAIN =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "build", "classes", "kotlin", "main");

    public static CodeSourcePathElementsUnderProjectDirectory IDEA_PRODUCTION =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "out", "production", "classes");

    public static CodeSourcePathElementsUnderProjectDirectory IDEA_TEST =
            new CodeSourcePathElementsUnderProjectDirectory(
                    "out", "test", "classes");

    private List<String> cspeupdStringList = new ArrayList<>();

    public CodeSourcePathElementsUnderProjectDirectory(List<String> cspeupdStringList) {
        this.cspeupdStringList = cspeupdStringList;
    }

    public CodeSourcePathElementsUnderProjectDirectory(Path cspeupd) {
        if (cspeupd.isAbsolute()) {
            throw new IllegalArgumentException("cspeupd is absolute");
        }
        List<String> list = new ArrayList<>();
        for (Path element : cspeupd) {
            list.add(element.toString());
        }
        this.cspeupdStringList = list;
    }

    public CodeSourcePathElementsUnderProjectDirectory(String pathElement, String... more) {
        this.cspeupdStringList = new ArrayList<>();
        Path p = Paths.get(pathElement);
        if (p.getNameCount() > 0) {
            for (int i = 0; i < p.getNameCount(); i++) {
                this.cspeupdStringList.add(p.getName(i).toString());
            }
        }
        if (more.length > 0) {
            this.cspeupdStringList.addAll(Arrays.asList(more));
        }
    }

    /**
     * copy constructor
     */
    public CodeSourcePathElementsUnderProjectDirectory(CodeSourcePathElementsUnderProjectDirectory source) {
        this(source.toString());
    }

    public List<String> asList() {
        return new ArrayList<>(this.cspeupdStringList);
    }

    public boolean isEmpty() {
        return this.cspeupdStringList.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : cspeupdStringList) {
            sb.append(s);
            sb.append(File.separator);
        }
        return sb.toString();
    }
}
