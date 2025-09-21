/*
 * Copyright (c) 2024 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dma.baleen.secom.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A utility class for creating ZIP files with a directory structure in memory.
 * This class provides a fluent API for building ZIP files with directories and files.
 * The class is designed to work with a streaming approach, writing data immediately
 * to the underlying ZIP stream.
 *
 * <p>Instances of this class can only be created through the {@link #of(ZipConsumer)} method.
 * Each instance represents a directory within the ZIP file.
 *
 * <p>Example usage:
 * <pre>{@code
 * byte[] zipContent = ZipDirectory.of(root -> {
 *     root.addFile("test.txt", "Hello World".getBytes());
 *
 *     ZipDirectory docs = root.addDirectory("docs");
 *     docs.addFile("readme.md", "# Documentation".getBytes());
 * });
 * }</pre>
 *
 * <p>The class enforces validation on file and directory names to ensure ZIP file compatibility
 * across different platforms. Invalid characters and potentially dangerous paths (like directory
 * traversal sequences) are not allowed.
 */
public class ZipDirectory {
    private final String name;
    private final ZipOutputStream zos;

    private static final String INVALID_CHARS = "/\\:*?\"<>|";

    /**
     * Creates a new ZIP file in memory using the provided consumer to populate its contents.
     * This method delegates to {@link #of(ZipConsumer, int)} with {@link Integer#MAX_VALUE} as the size limit.
     *
     * @param consumer a consumer that will be called with the root directory to populate the ZIP file
     * @return the complete ZIP file as a byte array
     * @throws IOException if an I/O error occurs while writing the ZIP file
     * @throws NullPointerException if the consumer is null
     */
    public static byte[] of(ZipConsumer<? super ZipDirectory> consumer) throws Exception {
        return of(consumer, Integer.MAX_VALUE);
    }

    /**
     * Creates a new ZIP file in memory using the provided consumer to populate its contents,
     * with a maximum size limit.
     * Note: The consumer should not be executed concurrently as ZipOutputStream is not thread-safe.
     *
     * @param consumer a consumer that will be called with the root directory to populate the ZIP file
     * @param maxSize the maximum size in bytes that the ZIP file can grow to
     * @return the complete ZIP file as a byte array
     * @throws IOException if an I/O error occurs while writing the ZIP file
     * @throws ZipFileSizeExceeded if the size limit would be exceeded
     * @throws NullPointerException if the consumer is null
     * @throws IllegalArgumentException if maxSize is less than or equal to 0
     */
    public static byte[] of(ZipConsumer<? super ZipDirectory> consumer, int maxSize) throws Exception {
        Objects.requireNonNull(consumer, "Consumer cannot be null");
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Maximum size must be greater than 0");
        }

        OutputStream out = maxSize == Integer.MAX_VALUE
            ? new ByteArrayOutputStream()
            : new SizeLimitedOutputStream(maxSize);

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipDirectory root = new ZipDirectory("", zos);
            consumer.accept(root);
        }

        return out instanceof ByteArrayOutputStream
            ? ((ByteArrayOutputStream) out).toByteArray()
            : ((SizeLimitedOutputStream) out).toByteArray();
    }

    private ZipDirectory(String name, ZipOutputStream zos) throws IOException {
        if (!name.endsWith("/") && !name.isEmpty()) {
            name += "/";
        }
        this.name = name;
        this.zos = zos;

        if (!name.isEmpty()) {
            zos.putNextEntry(new ZipEntry(name));
            zos.closeEntry();
        }
    }

    /**
     * Adds a file to this directory in the ZIP file.
     * The file is written immediately to the underlying ZIP stream.
     *
     * @param fileName the name of the file to add
     * @param content the content of the file as a byte array
     * @throws IOException if an I/O error occurs while writing the file
     * @throws ZipFileSizeExceeded if the size limit would be exceeded
     * @throws NullPointerException if fileName or content is null
     * @throws IllegalArgumentException if the file name is invalid. A file name is invalid if it:
     *         <ul>
     *           <li>is empty</li>
     *           <li>contains path traversal sequences (..)</li>
     *           <li>starts with a slash</li>
     *           <li>ends with a slash</li>
     *           <li>contains invalid characters (/\:*?"<>|)</li>
     *         </ul>
     */
    public void addFile(String fileName, byte[] content) throws IOException {
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(content, "File content cannot be null");

        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        validateFileName(fileName);

        ZipEntry zipEntry = new ZipEntry(name + fileName);
        zos.putNextEntry(zipEntry);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(content)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    /**
     * Creates a new subdirectory in this directory.
     * The directory entry is written immediately to the underlying ZIP stream.
     *
     * @param dirName the name of the directory to create
     * @return a new ZipDirectory instance representing the created subdirectory
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws NullPointerException if dirName is null
     * @throws IllegalArgumentException if the directory name is invalid. A directory name is invalid if it:
     *         <ul>
     *           <li>is empty</li>
     *           <li>contains path traversal sequences (..)</li>
     *           <li>starts with a slash</li>
     *           <li>contains invalid characters (/\:*?"<>|)</li>
     *         </ul>
     */
    public ZipDirectory addDirectory(String dirName) throws IOException {
        Objects.requireNonNull(dirName, "Directory name cannot be null");

        if (dirName.isEmpty()) {
            throw new IllegalArgumentException("Directory name cannot be empty");
        }
        validateDirectoryName(dirName);
        String fullPath = name + dirName;
        return new ZipDirectory(fullPath, zos);
    }

    /**
     * Returns the full path of this directory within the ZIP file.
     * The path will end with a forward slash unless this is the root directory.
     *
     * @return the full path of this directory
     */
    public String getPath() {
        return name;
    }

    private static void validateFileName(String fileName) {
        if (fileName.endsWith("/")) {
            throw new IllegalArgumentException("File name cannot end with a slash");
        }
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("File name cannot contain path traversal sequences");
        }
        if (fileName.startsWith("/")) {
            throw new IllegalArgumentException("File name cannot start with a slash");
        }
        for (char c : INVALID_CHARS.toCharArray()) {
            if (fileName.indexOf(c) != -1) {
                throw new IllegalArgumentException("File name contains invalid character: " + c);
            }
        }
        if (fileName.contains("\\")) {
            throw new IllegalArgumentException("File name cannot contain backslashes");
        }
    }

    private static void validateDirectoryName(String dirName) {
        if (dirName.contains("..")) {
            throw new IllegalArgumentException("Directory name cannot contain path traversal sequences");
        }
        if (dirName.startsWith("/")) {
            throw new IllegalArgumentException("Directory name cannot start with a slash");
        }
        for (char c : INVALID_CHARS.toCharArray()) {
            if (dirName.indexOf(c) != -1) {
                throw new IllegalArgumentException("Directory name contains invalid character: " + c);
            }
        }
        if (dirName.contains("\\")) {
            throw new IllegalArgumentException("Directory name cannot contain backslashes");
        }
    }

    private static class SizeLimitedOutputStream extends OutputStream {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final int maxSize;

        SizeLimitedOutputStream(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public void write(int b) throws ZipFileSizeExceeded {
            if (baos.size() + 1 > maxSize) {
                throw new ZipFileSizeExceeded(maxSize);
            }
            baos.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws ZipFileSizeExceeded {
            if (baos.size() + len > maxSize) {
                throw new ZipFileSizeExceeded(maxSize);
            }
            baos.write(b, off, len);
        }

        public byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    /**
     * Represents an operation that accepts a single input argument and returns no
     * result. Unlike {@link java.util.function.Consumer}, this interface is specifically
     * designed for ZIP operations and may throw checked exceptions.
     */
    @FunctionalInterface
    public interface ZipConsumer<T> {
        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @throws Exception if an error occurs during the operation
         */
        void accept(T t) throws Exception;
    }

    /**
     * Exception thrown when the ZIP file size would exceed the specified limit.
     */
    public static class ZipFileSizeExceeded extends IOException {
        private static final long serialVersionUID = 1L;

        public ZipFileSizeExceeded(int maxSize) {
            super("ZIP file size would exceed limit of " + maxSize + " bytes");
        }
    }
}