// Copyright 2015 Indeed
package com.indeed.util.io;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/** @author rboyer */
public class SafeFilesTest {
    @Rule public TemporaryFolder tempDir = new TemporaryFolder();
    private Path root;

    private static void checkFilePermissions(Path path) {
        try {
            final Path tempFile =
                    java.nio.file.Files.createTempFile(
                            "tmp",
                            "tmp",
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString("rwxrwxrwx")));
            final Set<PosixFilePermission> inverseUmask;
            try {
                inverseUmask = Files.getPosixFilePermissions(tempFile);
            } finally {
                java.nio.file.Files.delete(tempFile);
            }

            final Set<PosixFilePermission> posixFilePermissions =
                    java.nio.file.Files.getPosixFilePermissions(path);
            if (inverseUmask.contains(PosixFilePermission.OWNER_READ)) {
                Assert.assertTrue(
                        "owner read permission not set",
                        posixFilePermissions.contains(PosixFilePermission.OWNER_READ));
            }
            if (inverseUmask.contains(PosixFilePermission.OWNER_WRITE)) {
                Assert.assertTrue(
                        "owner write permission not set",
                        posixFilePermissions.contains(PosixFilePermission.OWNER_WRITE));
            }
            if (inverseUmask.contains(PosixFilePermission.GROUP_READ)) {
                Assert.assertTrue(
                        "group read permission not set",
                        posixFilePermissions.contains(PosixFilePermission.GROUP_READ));
            }
            if (inverseUmask.contains(PosixFilePermission.OTHERS_READ)) {
                Assert.assertTrue(
                        "other read permission not set",
                        posixFilePermissions.contains(PosixFilePermission.OTHERS_READ));
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    // these all use the same underlying method, so we don't really have to test all variations
    @Test
    public void write_direct_ok() throws IOException {
        final Path path = root.resolve("foo.bin");

        final byte[] wrote = "hello world".getBytes(Charsets.UTF_8);

        SafeFiles.write(wrote, path);
        checkFilePermissions(path);

        final byte[] read = com.google.common.io.Files.toByteArray(path.toFile());

        Assert.assertArrayEquals(wrote, read);

        Assert.assertEquals("no stray files", 1, Directories.count(root));
    }

    @Test
    public void write_viaSafeFile_asOutputStream() throws IOException {
        final Path path = root.resolve("foo.bin");

        final byte[] wrote = "hello world".getBytes(Charsets.UTF_8);

        try (final SafeOutputStream out = SafeFiles.createAtomicFile(path)) {
            out.write(wrote);
            out.flush();
            out.commit();
        }
        checkFilePermissions(path);

        final byte[] read = com.google.common.io.Files.toByteArray(path.toFile());

        Assert.assertArrayEquals(wrote, read);

        Assert.assertEquals("no stray files", 1, Directories.count(root));
    }

    @Test
    public void write_viaSafeFile_asOutputStream_rollback() throws IOException {
        final Path path = root.resolve("foo.bin");

        final byte[] wrote = "hello world".getBytes(Charsets.UTF_8);

        try (final SafeOutputStream out = SafeFiles.createAtomicFile(path)) {
            out.write(wrote);
            out.flush();
            // no commit
        }

        Assert.assertFalse(java.nio.file.Files.exists(path));

        Assert.assertEquals("no stray files", 0, Directories.count(root));
    }

    // these all use the same underlying method, so we don't really have to test all variations
    @Test
    public void write_asWritableByteChannel_fail() throws IOException {
        final Path path = root.resolve("foo.bin");

        final byte[] wrote = "hello world".getBytes(Charsets.UTF_8);

        try {
            try (final SafeOutputStream out = SafeFiles.createAtomicFile(path)) {
                out.write(ByteBuffer.wrap(wrote));
                throw new IOException("failed after write!");
                // no commit
            }
        } catch (IOException e) {
            // expected
        }

        Assert.assertFalse(java.nio.file.Files.exists(path));

        Assert.assertEquals("no stray files", 0, Directories.count(root));
    }

    // these all use the same underlying method, so we don't really have to test all variations
    @Test
    public void write_asOutputStream_fail() throws IOException {
        final Path path = root.resolve("foo.bin");

        final byte[] wrote = "hello world".getBytes(Charsets.UTF_8);

        try {
            try (final SafeOutputStream out = SafeFiles.createAtomicFile(path)) {
                out.write(wrote);
                throw new IOException("failed after write!");
                // no commit
            }
        } catch (IOException e) {
            // expected
        }

        Assert.assertFalse(java.nio.file.Files.exists(path));

        Assert.assertEquals("no stray files", 0, Directories.count(root));
    }

    @Test
    public void rename_sameDir() throws IOException {
        final byte[] data = "hello world".getBytes(Charsets.UTF_8);

        final Path origPath = root.resolve("orig.txt");
        final Path newPath = root.resolve("new.txt");
        SafeFiles.write(data, origPath);
        checkFilePermissions(origPath);

        Assert.assertTrue(java.nio.file.Files.exists(origPath));
        Assert.assertFalse(java.nio.file.Files.exists(newPath));
        Assert.assertEquals("no stray files", 1, Directories.count(root));

        SafeFiles.rename(origPath, newPath);

        Assert.assertFalse(java.nio.file.Files.exists(origPath));
        Assert.assertTrue(java.nio.file.Files.exists(newPath));
        Assert.assertEquals("no stray files", 1, Directories.count(root));
    }

    @Test
    public void rename_diffDir() throws IOException {
        final byte[] data = "hello world".getBytes(Charsets.UTF_8);

        final Path dir1 = root.resolve("dir1");
        final Path dir2 = root.resolve("dir2");

        SafeFiles.ensureDirectoryExists(dir1);
        SafeFiles.ensureDirectoryExists(dir2);

        final Path origPath = dir1.resolve("orig.txt");
        final Path newPath = dir2.resolve("new.txt");
        SafeFiles.write(data, origPath);
        checkFilePermissions(origPath);

        Assert.assertTrue(java.nio.file.Files.exists(origPath));
        Assert.assertFalse(java.nio.file.Files.exists(newPath));
        Assert.assertEquals("no stray files", 1, Directories.count(dir1));
        Assert.assertEquals("no stray files", 0, Directories.count(dir2));

        SafeFiles.rename(origPath, newPath);

        Assert.assertFalse(java.nio.file.Files.exists(origPath));
        Assert.assertTrue(java.nio.file.Files.exists(newPath));
        Assert.assertEquals("no stray files", 0, Directories.count(dir1));
        Assert.assertEquals("no stray files", 1, Directories.count(dir2));
    }

    @Test
    public void ensureDirectoryExists() throws IOException {
        final Path path = root.resolve("dir");

        Assert.assertFalse(java.nio.file.Files.exists(path));

        SafeFiles.ensureDirectoryExists(path);
        Assert.assertTrue(java.nio.file.Files.exists(path));

        // idempotent
        SafeFiles.ensureDirectoryExists(path);
        Assert.assertTrue(java.nio.file.Files.exists(path));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void fsyncRecursive() throws IOException {
        Assert.assertEquals(0, Directories.count(root));

        tempDir.newFile("foo1");
        tempDir.newFile("foo2");
        final File dir = tempDir.newFolder("dir");

        new File(dir, "bar1").createNewFile();
        new File(dir, "bar2").createNewFile();
        new File(dir, "bar3").createNewFile();

        final File dir2 = new File(dir, "sub");
        dir2.mkdir();

        new File(dir2, "baz").createNewFile();

        Assert.assertEquals(2 /*foo*/ + 3 /*bar*/ + 1 /*baz*/, SafeFiles.fsyncRecursive(root));
    }

    @Test
    public void deleteIfExistsQuietly_doesNotExist() {
        final Path path = root.resolve("blah");
        Assert.assertFalse(java.nio.file.Files.exists(path));
        SafeFiles.deleteIfExistsQuietly(path);
        Assert.assertFalse(java.nio.file.Files.exists(path));
    }

    @Test
    public void deleteIfExistsQuietly_doesExist() throws IOException {
        final Path path = root.resolve("blah");
        SafeFiles.writeUTF8("hello world", path);
        Assert.assertTrue(java.nio.file.Files.exists(path));

        SafeFiles.deleteIfExistsQuietly(path);
        Assert.assertFalse(java.nio.file.Files.exists(path));
    }

    @Before
    public void init() {
        root = tempDir.getRoot().toPath();
    }
}
