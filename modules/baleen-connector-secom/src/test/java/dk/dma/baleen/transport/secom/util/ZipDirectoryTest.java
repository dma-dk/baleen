package dk.dma.baleen.transport.secom.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dk.dma.baleen.connector.secom.util.ZipDirectory;

/**
 * Test suite for ZipDirectory class.
 */
class ZipDirectoryTest {

    @Test
    @DisplayName("Test creating empty zip")
    void testEmptyZip() throws Exception {
        byte[] zip = ZipDirectory.of(root -> {});
        assertNotNull(zip);
        assertTrue(zip.length > 0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            assertNull(zis.getNextEntry(), "Empty ZIP should not contain any entries");
        }
    }

    @Test
    @DisplayName("Test adding single file to root")
    void testSingleFile() throws Exception {
        String content = "Hello, World!";
        byte[] zip = ZipDirectory.of(root ->
            root.addFile("test.txt", content.getBytes(StandardCharsets.UTF_8))
        );

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("test.txt", entry.getName());
            assertFalse(entry.isDirectory());

            String fileContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, fileContent);

            assertNull(zis.getNextEntry(), "Should only contain one file");
        }
    }

    @Test
    @DisplayName("Test creating directory structure")
    void testDirectoryStructure() throws Exception {
        byte[] zip = ZipDirectory.of(root -> {
            root.addFile("root.txt", "root".getBytes());

            ZipDirectory docs = root.addDirectory("docs");
            docs.addFile("doc.txt", "doc".getBytes());

            ZipDirectory images = root.addDirectory("images");
            images.addFile("img.png", "fake-image".getBytes());

            ZipDirectory subDocs = docs.addDirectory("subdocs");
            subDocs.addFile("subdoc.txt", "subdoc".getBytes());
        });

        Set<String> expectedEntries = new HashSet<>();
        expectedEntries.add("root.txt");
        expectedEntries.add("docs/");
        expectedEntries.add("docs/doc.txt");
        expectedEntries.add("images/");
        expectedEntries.add("images/img.png");
        expectedEntries.add("docs/subdocs/");
        expectedEntries.add("docs/subdocs/subdoc.txt");

        Set<String> actualEntries = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                actualEntries.add(entry.getName());
                if (!entry.isDirectory()) {
                    assertNotEquals(0, zis.readAllBytes().length,
                        "File " + entry.getName() + " should not be empty");
                }
            }
        }

        assertEquals(expectedEntries, actualEntries, "ZIP should contain exactly the expected entries");
    }

    @Test
    @DisplayName("Test large file handling")
    void testLargeFile() throws Exception {
        // Create 1MB of test data
        byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte)(i % 256);
        }

        byte[] zip = ZipDirectory.of(root ->
            root.addFile("large.dat", largeContent)
        );

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("large.dat", entry.getName());

            byte[] extractedContent = zis.readAllBytes();
            assertArrayEquals(largeContent, extractedContent,
                "Large file content should be preserved exactly");
        }
    }

    @ParameterizedTest
    @DisplayName("Test invalid file names")
    @ValueSource(strings = {
        "../test.txt",
        "/test.txt",
        "test/txt",
        "test\\txt",
        "test:txt",
        "test*txt",
        "test?txt",
        "test\"txt",
        "test<txt",
        "test>txt",
        "test|txt",
        "test/"
    })
    void testInvalidFileNames(String fileName) {
        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root ->
                root.addFile(fileName, "test".getBytes())
            )
        );
    }

    @ParameterizedTest
    @DisplayName("Test invalid directory names")
    @ValueSource(strings = {
        "../docs",
        "/docs",
        "docs/test",
        "docs\\test",
        "docs:test",
        "docs*test",
        "docs?test",
        "docs\"test",
        "docs<test",
        "docs>test",
        "docs|test"
    })
    void testInvalidDirectoryNames(String dirName) {
        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root ->
                root.addDirectory(dirName)
            )
        );
    }

    @Test
    @DisplayName("Test null inputs throw NullPointerException")
    void testNullInputs() {
        assertThrows(NullPointerException.class, () ->
            ZipDirectory.of(null)
        );

        assertThrows(NullPointerException.class, () ->
            ZipDirectory.of(root -> root.addFile(null, "test".getBytes()))
        );

        assertThrows(NullPointerException.class, () ->
            ZipDirectory.of(root -> root.addFile("test.txt", null))
        );

        assertThrows(NullPointerException.class, () ->
            ZipDirectory.of(root -> root.addDirectory(null))
        );
    }

    @Test
    @DisplayName("Test empty inputs throw IllegalArgumentException")
    void testEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root -> root.addFile("", "test".getBytes()))
        );

        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root -> root.addDirectory(""))
        );
    }

    @Test
    @DisplayName("Test directory path correctness")
    void testDirectoryPaths() throws Exception {
        ZipDirectory.of(root -> {
            assertEquals("", root.getPath());

            ZipDirectory docs = root.addDirectory("docs");
            assertEquals("docs/", docs.getPath());

            ZipDirectory subDocs = docs.addDirectory("subdocs");
            assertEquals("docs/subdocs/", subDocs.getPath());
        });
    }

    @Test
    @DisplayName("Test multiple sequential directories")
    void testMultipleDirectories() throws Exception {
        byte[] zip = ZipDirectory.of(root -> {
            // Create multiple directories sequentially
            for (int i = 0; i < 10; i++) {
                ZipDirectory dir = root.addDirectory("dir" + i);
                dir.addFile("file.txt", ("content" + i).getBytes());
            }
        });

        // Verify all directories and files were created
        Set<String> expectedEntries = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            expectedEntries.add("dir" + i + "/");
            expectedEntries.add("dir" + i + "/file.txt");
        }

        Set<String> actualEntries = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                actualEntries.add(entry.getName());
            }
        }

        assertEquals(expectedEntries, actualEntries);
    }

    @Test
    @DisplayName("Test zip size limit exceeded")
    void testZipSizeLimit() {
        // Create incompressible data
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte)(Math.random() * 256);
        }

        assertThrows(ZipDirectory.ZipFileSizeExceeded.class, () ->
            ZipDirectory.of(root -> {
                // Try to add files until we exceed the 1MB limit
                for (int i = 0; i < 2; i++) {
                    root.addFile("file" + i + ".dat", largeContent);
                }
            }, 1024 * 1024) // 1MB limit
        );
    }

    @Test
    @DisplayName("Test zip size limit not exceeded")
    void testZipSizeLimitNotExceeded() throws Exception {
        byte[] content = new byte[1024]; // 1KB
        byte[] zip = ZipDirectory.of(root -> {
            // Add small files, should stay under limit
            for (int i = 0; i < 10; i++) {
                root.addFile("file" + i + ".dat", content);
            }
        }, 50 * 1024); // 50KB limit

        assertNotNull(zip);
        assertTrue(zip.length > 0);
        assertTrue(zip.length < 50 * 1024);
    }

    @Test
    @DisplayName("Test invalid size limit")
    void testInvalidSizeLimit() {
        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root -> {}, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
            ZipDirectory.of(root -> {}, -1)
        );
    }
}