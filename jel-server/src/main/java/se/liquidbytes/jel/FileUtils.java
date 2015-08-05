/*
 * Copyright 2015 Henrik Östman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.liquidbytes.jel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

/**
 * Class containing general file utilities.
 *
 * @author Henrik Östman
 */
public final class FileUtils {

  /**
   * Recursive deletes a directory and everything within it.
   *
   * @param directory Directory to remove
   * @throws IOException
   */
  public static void deleteDirectory(Path directory) throws IOException {
    if (directory != null) {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  /**
   * Uncompress zip-file to directory, creates directory if not exists.
   *
   * @param filePath file to unzip.
   * @param destination target directory.
   * @throws IOException throws exception on errors.
   */
  public static void unzip(Path filePath, Path destination) throws IOException {
    Map<String, String> zipProperties = new HashMap<>();
    /* We want to read an existing ZIP File, so we set this to False */
    zipProperties.put("create", "false");
    zipProperties.put("encoding", "UTF-8");
    URI zipFile = URI.create("jar:file:" + filePath.toUri().getPath());

    try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, zipProperties)) {
      Path rootPath = zipfs.getPath("/");
      Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

          Path targetPath = destination.resolve(rootPath.relativize(dir).toString());
          if (!Files.exists(targetPath)) {
            Files.createDirectory(targetPath);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

          Files.copy(file, destination.resolve(rootPath.relativize(file).toString()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  /**
   * Calculate the checksum of the provided file using the provided name of the algorithm.
   *
   * @param file File to calculate checksum on.
   * @param algorithm The algorithm to use. e.g MD5, SHA1, SHA-256, SHA-512.
   * @return Checksum in hexadecimal notation.
   */
  public static String fileChecksum(File file, String algorithm) {
    try (FileInputStream inputStream = new FileInputStream(file)) {
      MessageDigest digest = MessageDigest.getInstance(algorithm);

      byte[] bytesBuffer = new byte[1024];
      int bytesRead;

      while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
        digest.update(bytesBuffer, 0, bytesRead);
      }

      byte[] hashedBytes = digest.digest();

      return DatatypeConverter.printHexBinary(hashedBytes);
    } catch (NoSuchAlgorithmException | IOException ex) {
      throw new JelException("Failed to calculate checksum of file: " + file.getName(), ex);
    }
  }
}
