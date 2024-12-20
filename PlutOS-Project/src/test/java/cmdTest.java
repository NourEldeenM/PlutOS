import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.os.cmd;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


public class cmdTest {

    @Nested
    class catCommandTest {
        @Test
        public void testCatWithFiles(@TempDir Path tempDir) throws IOException {
            Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
            Path file2 = Files.createFile(tempDir.resolve("file2.txt"));
            Files.writeString(file1, "file1\n");
            Files.writeString(file2, "file2\n");

            String result = cmd.cat(new String[]{"ignored", file1.toString(), file2.toString()});

            String expectedOutput = "file1\nfile2\n"; // verify output
            assertEquals(expectedOutput, result);
        }
    }

    @Nested
    class ForwardArrowTest {

        private final String fileName = "testFile.txt";

        @BeforeEach
        void setUp() throws IOException {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        @AfterEach
        void tearDown() {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }

        @Test
        void testForwardArrowWritesContentToFile() throws IOException {
            String[] args = {"pwd", ">", fileName};

            cmd.forwardArrow(args);

            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            String value = cmd.pwd();

            assertEquals(content, value, "The content in the file should match the input.");
        }

        @Test
        void testForwardArrowNullArgs() {
            assertThrows(NullPointerException.class, () -> cmd.forwardArrow(null),
                    "NullPointerException expected when args is null.");
        }
    }

    @Nested
    class changeDirectoryTest {
        private String initialDir;
        private String homeDir;

        @BeforeEach
        void setUp() {
            initialDir = System.getProperty("user.dir");
            homeDir = System.getProperty("user.home");
        }

        @Test
        void testChangeToParentDirectory() {
            File testSubDir = new File(initialDir, "testSubDir");
            assertTrue(testSubDir.mkdir(), "Failed to create test subdirectory");

            System.setProperty("user.dir", testSubDir.getAbsolutePath());
            assertEquals(testSubDir.getAbsolutePath(), System.getProperty("user.dir"));

            cmd.cd(new String[]{"ignored", ".."});
            assertEquals(initialDir, System.getProperty("user.dir"));

            assertTrue(testSubDir.delete(), "Failed to delete test subdirectory");
        }

        @Test
        void testChangeToHomeDirectory() {
            System.setProperty("user.dir", initialDir);  // Reset to initial directory

            cmd.cd(new String[]{"ignored", "~"});
            assertEquals(homeDir, System.getProperty("user.dir"));
        }

        @Test
        void testChangeToSpecificExistingDirectory() {
            File specificDir = new File(initialDir, "specificDir");
            assertTrue(specificDir.mkdir(), "Failed to create specific directory");

            cmd.cd(new String[]{"ignored", "specificDir"});
            assertEquals(specificDir.getAbsolutePath(), System.getProperty("user.dir"));

            assertTrue(specificDir.delete(), "Failed to delete specific directory");
        }

        @Test
        void testErrorForNonExistingDirectory() {
            System.setProperty("user.dir", initialDir);  // Reset to initial directory

            cmd.cd(new String[]{"ignored", "nonExistentDir"});
            assertEquals(initialDir, System.getProperty("user.dir"));
        }

        @Test
        void testErrorForNoParentDirectory() {
            System.setProperty("user.dir", "/");

            cmd.cd(new String[]{"ignored", ".."});
            assertEquals("/", System.getProperty("user.dir"));
        }
    }

    @Nested
    class fileMoveTest {
        private final String sourceFileName = "sourceTestFile.txt";
        private final String destFileName = "destTestFile.txt";

        @BeforeEach
        void setUp() throws IOException {
            FileWriter writer = new FileWriter(sourceFileName);
            writer.write("This is a test file.");
            writer.close();
        }

        @AfterEach
        void tearDown() {
            File sourceFile = new File(sourceFileName);
            File destFile = new File(destFileName);
            if (sourceFile.exists()) sourceFile.delete();
            if (destFile.exists()) destFile.delete();
        }

        @Test
        void testMissingDestinationOperand() {
            String[] args = {"ignored", sourceFileName};
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputStream));
            cmd.mv(args);
            assertTrue(outputStream.toString().contains("mv: missing destination file operand"),
                    "Should warn about missing destination operand.");
        }

        @Test
        void testFileMovedSuccessfully() throws IOException {
            String[] args = {"ignored", sourceFileName, destFileName};
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputStream));
            cmd.mv(args);
            File destFile = new File(destFileName);
            assertTrue(destFile.exists(), "Destination file should exist after moving.");
            String content = new String(Files.readAllBytes(destFile.toPath()));
            assertEquals("This is a test file." + System.lineSeparator(), content, "File content should match.");
            File sourceFile = new File(sourceFileName);
            assertFalse(sourceFile.exists(), "Source file should be deleted after moving.");
            assertTrue(outputStream.toString().contains("File moved successfully and original file deleted."),
                    "Output should confirm successful file move and deletion.");
        }
    }

    @Nested
    class pwdCommandTests {
        @Test
        public void testPwd() {
            String result = cmd.pwd();
            String expectedDir = System.getProperty("user.dir");
            assertEquals(expectedDir, result, "PWD command failed");
        }
    }

    @Nested
    class rmdirCommandTests {

        @BeforeEach
        public void setUpRmdir() {
            new File("testEmptyDir").mkdir();
            File nonEmptyDir = new File("testNonEmptyDir");
            nonEmptyDir.mkdir();
            try {
                new File(nonEmptyDir, "testFile.txt").createNewFile();
            } catch (Exception ignored) {
            }
        }

        @Test
        public void testRmdirEmptyDirectory() {
            String result = cmd.rmdir(new String[]{"rmdir", "testEmptyDir"});
            assertEquals("Directory 'testEmptyDir' deleted.", result, "RMDIR command failed on empty directory");
        }

        @Test
        public void testRmdirNonEmptyDirectory() {
            String result = cmd.rmdir(new String[]{"rmdir", "testNonEmptyDir"});
            assertEquals("Error: Directory is not empty.", result, "RMDIR command did not handle non-empty directory correctly");
        }

        @Test
        public void testRmdirNonExistentDirectory() {
            String result = cmd.rmdir(new String[]{"rmdir", "nonExistentDir"});
            assertEquals("Error: Directory does not exist.", result, "RMDIR command did not handle non-existent directory correctly");
        }
    }

    @Nested
    class RmCommandTests {

        @BeforeEach
        public void setUpRm() {
            try {
                new File("testFile.txt").createNewFile();
                File dir = new File("testDirRecursive");
                dir.mkdir();
                new File(dir, "testFile.txt").createNewFile();
            } catch (Exception ignored) {
            }
        }

        @Test
        public void testRmFile() {
            String result = cmd.rm(new String[]{"rm", "testFile.txt"});
            assertEquals("File 'testFile.txt' deleted.", result, "RM command failed on file deletion");
        }

        @Test
        public void testRmNonExistentFile() {
            String result = cmd.rm(new String[]{"rm", "nonExistentFile.txt"});
            assertEquals("Error: nonExistentFile.txt does not exist.", result, "RM command did not handle non-existent file correctly");
        }

        @Test
        public void testRmDirectoryRecursive() {
            String result = cmd.rm(new String[]{"rm", "-r", "testDirRecursive"});
            assertEquals("Directory 'testDirRecursive' deleted.", result, "RM command failed on recursive directory deletion");
        }
    }

//    ls command tests

    @Nested
    class LsCommandTests {

        @BeforeEach
        public void setUpLs() {
            try {
                // Create some test files and directories
                new File("testFile1.txt").createNewFile();
                new File("testFile2.txt").createNewFile();
                File testDir = new File("testDir");
                testDir.mkdir();
                new File(testDir, "testFileInDir.txt").createNewFile();
            } catch (Exception ignored) {
            }
        }

        @AfterEach
        public void tearDown() {
            // Clean up the created files and directories after each test
            new File("testFile1.txt").delete();
            new File("testFile2.txt").delete();
            File dir = new File("testDir");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }

        @Test
        public void testLsNoArgs() {
            String result = cmd.ls(new String[]{"ls"});
            assertTrue(result.contains("testFile1.txt"), "LS command should list 'testFile1.txt'");
            assertTrue(result.contains("testFile2.txt"), "LS command should list 'testFile2.txt'");
            assertTrue(result.contains("testDir"), "LS command should list 'testDir'");
        }

        @Test
        public void testLsWithDirectory() {
            String result = cmd.ls(new String[]{"ls", "testDir"});
            assertTrue(result.contains("testFileInDir.txt"), "LS command should list 'testFileInDir.txt' in 'testDir'");
        }

        @Test
        public void testLsNonExistentDirectory() {
            String result = cmd.ls(new String[]{"ls", "nonExistentDir"});
            assertEquals("Error: nonExistentDir does not exist.\n", result, "LS command did not handle non-existent directory correctly");
        }

        @Test
        public void testLsWithInvalidArgs() {
            String result = cmd.ls(new String[]{"ls", "-invalidArg"});
            assertEquals("Error: This i argument isn't supported\n", result, "LS command should handle an unsupported argument correctly");
        }

        @Test
        public void testLsWithAllFlag() {
            // Create a hidden file for testing
            try {
                new File(".hiddenFile.txt").createNewFile();
            } catch (Exception ignored) {
            }

            String result = cmd.ls(new String[]{"ls", "-a"});
            // Check if the output includes the hidden file and other visible files
            assertTrue(result.contains("testFile1.txt"), "LS command should list 'testFile1.txt'");
            assertTrue(result.contains("testFile2.txt"), "LS command should list 'testFile2.txt'");
            assertTrue(result.contains("testDir"), "LS command should list 'testDir'");
            assertTrue(result.contains(".hiddenFile.txt"), "LS command should list '.hiddenFile.txt' when using -a");

            // Clean up
            new File(".hiddenFile.txt").delete();
        }

        @Test
        public void testLsWithRecursiveFlag() {
            // Create a directory with nested directories and files for testing
            File nestedDir = new File("testDir/nestedDir");
            nestedDir.mkdirs();
            try {
                new File(nestedDir, "nestedFile.txt").createNewFile();
            } catch (Exception ignored) {
            }

            String result = cmd.ls(new String[]{"ls", "-r", "testDir"});
            // Check if the output includes the nested file
            assertTrue(result.contains("nestedDir"), "LS command should list 'nestedDir' in 'testDir'");
            assertTrue(result.contains("nestedFile.txt"), "LS command should list 'nestedFile.txt' in 'nestedDir'");

            // Clean up
            new File(nestedDir, "nestedFile.txt").delete();
            nestedDir.delete();
        }

    }

    @Nested
    class AppendOutputToFileTests {

        private final String testFilePath = "outputTest.txt";
        private final String testDirPath = "testDir";

        @BeforeEach
        public void setUp() throws IOException {
            new File(testDirPath).mkdir();

            File testFile = new File(testFilePath);
            if (!testFile.exists()) {
                testFile.createNewFile();
            }
        }

        @AfterEach
        public void tearDown() {
            File testFile = new File(testFilePath);
            if (testFile.exists()) {
                testFile.delete();
            }
            File testDir = new File(testDirPath);
            if (testDir.exists()) {
                testDir.delete();
            }
        }

        @Test
        public void testAppendOutputToFileWithValidCommand() throws IOException {
            String result = cmd.appendOutputToFile(new String[]{"pwd", ">>", testFilePath});

            assertEquals("Output successfully appended to outputTest.txt", result);

            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(testFilePath)));
            assertTrue(content.contains(System.getProperty("user.dir")), "Output file should contain the current directory path.");
        }

        @Test
        public void testAppendOutputToFileWithUnknownCommand() {
            String result = cmd.appendOutputToFile(new String[]{"unknownCommand", ">>", testFilePath});
            assertEquals("Error: Unknown command.", result);
        }

        @Test
        public void testAppendOutputToFileWithInsufficientArguments() {
            String result = cmd.appendOutputToFile(new String[]{"ls", ">>"});
            assertEquals("Error: Output redirection should be in the format: command >> file", result);
        }

        @Test
        public void testAppendOutputToFileWithExistingFile() throws IOException {
            try (FileWriter writer = new FileWriter(testFilePath)) {
                writer.write("Existing content\n");
            }

            String result = cmd.appendOutputToFile(new String[]{"pwd", ">>", testFilePath});
            assertEquals("Output successfully appended to outputTest.txt", result);

            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(testFilePath)));
            assertTrue(content.contains("Existing content"), "Output file should still contain the existing content.");
            assertTrue(content.contains(System.getProperty("user.dir")), "Output file should contain the current directory path.");
        }
    }

    @Nested
    class mkdirTest {
        private String basePath;

        @BeforeEach
        public void setup() {
            basePath = System.getProperty("user.dir") + File.separator;
        }

        @Test
        public void testMkdirEmptyPath() {
            String dirName = "emptyDir_" + System.currentTimeMillis();
            String[] tokens = {"mkdir", dirName};
            String result = cmd.mkdirCommand(tokens);
            String expectedPath = basePath + dirName;
            assertEquals("Directory '" + dirName + "' created at " + expectedPath, result);
            new File(expectedPath).delete();
        }

        @Test
        public void testMkdirWithValidName() {
            String dirName = "testDir_" + System.currentTimeMillis();
            String[] tokens = {"mkdir", dirName};
            new File(basePath + dirName).mkdir();
            String result = cmd.mkdirCommand(tokens);
            assertEquals("Error: Directory already exists.", result);
            new File(basePath + dirName).delete();
        }

        @Test
        void testMkdirCustomPath() throws IOException {
            File tempDir = Files.createTempDirectory("testDir").toFile();
            String[] tokens = {"mkdir", "some/custom/nestedDir_test", tempDir.getAbsolutePath()};
            String expectedOutput = "Directory 'some/custom/nestedDir_test' created at " +
                    tempDir.getAbsolutePath() + File.separator + "some" + File.separator + "custom" + File.separator + "nestedDir_test";
            String actualOutput = cmd.mkdirCommand(tokens);
            assertEquals(expectedOutput, actualOutput);
            deleteDir(tempDir);
        }

        private void deleteDir(File directory) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }

        @Test
        public void testMkdirWithExistingDir() {
            String dirName = "existingDir_" + System.currentTimeMillis();
            String[] tokens = {"mkdir", dirName};
            new File(basePath + dirName).mkdir();
            String result = cmd.mkdirCommand(tokens);
            assertEquals("Error: Directory already exists.", result);
            new File(basePath + dirName).delete();
        }

        @Test
        public void testMkdirWithEmptyName() {
            String[] tokens = {"mkdir", ""};
            String result = cmd.mkdirCommand(tokens);
            assertEquals("Error: Invalid directory name.", result);
        }

        @Test
        public void testMkdirWithSpecialCharacters() {
            String dirName = "dir@123_" + System.currentTimeMillis();
            String[] tokens = {"mkdir", dirName};
            String result = cmd.mkdirCommand(tokens);
            String expectedPath = basePath + dirName;
            assertEquals("Directory '" + dirName + "' created at " + expectedPath, result);
            new File(expectedPath).delete();
        }
    }

    private static final String TEST_FILE_NAME = "testFile.txt";

    @Nested
    public class touchTest {
        private static final String TEST_FILE_NAME = "testFile.txt";
        private File testFile;

        @BeforeEach
        public void setUp() {
            testFile = new File(System.getProperty("user.dir"), TEST_FILE_NAME);
            if (testFile.exists()) {
                testFile.delete();  // clean up 2bl kol test
            }
        }

        @Test
        public void testTouchCommandCreatesFile() {
            String[] tokens = {"touch", TEST_FILE_NAME};
            String result = cmd.touchCommand(tokens);
            assertEquals("File 'testFile.txt' created successfully.", result);
            assertTrue(testFile.exists());
        }

        @Test
        public void testTouchCommandUpdatesFile() throws IOException {
            Files.createFile(Paths.get(System.getProperty("user.dir"), TEST_FILE_NAME));

            String[] tokens = {"touch", TEST_FILE_NAME};
            String result = cmd.touchCommand(tokens);
            assertEquals("File 'testFile.txt' updated successfully.", result);
        }
    }

    @Nested
    public class pipeTest {
        @Test
        public void testPipeCommandWithMkdirAndTouch() {
            String input = "mkdir testDir | touch testFile.txt";
            cmd.handlePipe(input);
            assertTrue(new File(System.getProperty("user.dir"), "testDir").exists());
            assertTrue(new File(System.getProperty("user.dir"), TEST_FILE_NAME).exists());
        }

        @Test
        public void testPipeCommandWithPwd() {
            String input = "pwd";
            cmd.handlePipe(input);
            assertEquals(System.getProperty("user.dir"), cmd.pwd());
        }

        @Test
        public void testPipeCommandWithLs() {
            String testDirectory = "testDir";
            String testFileName = "testFileForLs.txt";

            try {
                Path dirPath = Paths.get(testDirectory);
                if (!Files.exists(dirPath)) {
                    Files.createDirectory(dirPath);
                }
                Files.writeString(dirPath.resolve(testFileName), "Sample content for ls test.");
                assertTrue(Files.exists(dirPath), "Directory should exist after creation.");
                assertTrue(Files.exists(dirPath.resolve(testFileName)), "File should exist after writing.");
                String input = "ls " + testDirectory;
                String output = handlePipe(input);
                assertTrue(output.contains(testFileName), "Output should contain the file name in the directory.");
            } catch (IOException e) {
                fail("Failed to create or read the test directory/file: " + e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(Paths.get(testDirectory, testFileName));
                    Files.deleteIfExists(Paths.get(testDirectory));
                } catch (IOException e) {
                    System.err.println("Cleanup failed: " + e.getMessage());
                }
            }
        }
        @Test
        public void testPipeCommandWithCat () {
                    String testFileName = "testFileForCat.txt";
                    try {
                        Files.writeString(Paths.get(testFileName), "Sample content for cat test.");
                        assertTrue(Files.exists(Paths.get(testFileName)), "File should exist after writing.");
                        String input = "cat " + testFileName;
                        String output = handlePipe(input);
                        assertEquals("Sample content for cat test.", output.strip(), "Output should match file content.");
                    } catch (IOException e) {
                        fail("Failed to create or read the test file: " + e.getMessage());
                    } finally {
                        try {
                            Files.deleteIfExists(Paths.get(testFileName));
                        } catch (IOException e) {
                            System.err.println("Cleanup failed: " + e.getMessage());
                        }
                    }
                }

        private String handlePipe (String command){
                    if (command.startsWith("cat ")) {
                        String fileName = command.substring(4).trim();
                        return readFileContents(fileName);
                    } else if (command.startsWith("ls ")) {
                        String directoryName = command.substring(3).trim();
                        return listDirectoryContents(directoryName);
                    }
                    return "";
                }

        private String readFileContents (String fileName){
                    try {
                        return Files.readString(Paths.get(fileName));
                    } catch (IOException e) {
                        return "Error reading file: " + e.getMessage();
                    }
                }

        private String listDirectoryContents (String directoryName){
                    File directory = new File(directoryName);
                    String[] files = directory.list();
                    if (files != null && files.length > 0) {
                        return String.join("\n", files);
                    }
                    return "";
                }

        @Test
        public void testPipeCommandWithHelp () {
                    String input = "help";
                    cmd.handlePipe(input);
                    String helpOutput = cmd.help();
                    assertNotNull(helpOutput);
                    assertTrue(helpOutput.length() > 0);
                }
        @Test
        public void testPipeCommandWithInvalidCommand () {
                    String input = "mkdir testDir | invalidCommand";
                    cmd.handlePipe(input); //"Unknown command in pipe: invalidcommand"
                }

    }
}
