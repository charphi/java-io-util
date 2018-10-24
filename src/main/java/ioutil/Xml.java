/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ioutil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public interface Parser<T> {

        @Nonnull
        default T parseChars(@Nonnull CharSequence source) throws IOException {
            return parseReader(() -> new StringReader(source.toString()));
        }

        @Nonnull
        default T parseFile(@Nonnull File source) throws IOException {
            checkFile(source);
            return parseStream(() -> open(source));
        }

        @Nonnull
        default T parsePath(@Nonnull Path source) throws IOException {
            Optional<File> file = IO.getFile(source);
            return file.isPresent()
                    ? parseFile(file.get())
                    : parseReader(() -> Files.newBufferedReader(source));
        }

        @Nonnull
        default T parseReader(@Nonnull IO.Supplier<? extends Reader> source) throws IOException {
            try (Reader resource = open(source)) {
                return parseReader(resource);
            }
        }

        @Nonnull
        default T parseStream(@Nonnull IO.Supplier<? extends InputStream> source) throws IOException {
            try (InputStream resource = open(source)) {
                return parseStream(resource);
            }
        }

        @Nonnull
        T parseReader(@Nonnull Reader resource) throws IOException;

        @Nonnull
        T parseStream(@Nonnull InputStream resource) throws IOException;
    }

    public static final class WrappedException extends IOException {

        public WrappedException(Exception ex) {
            super(ex);
        }
    }

    static <T> T open(IO.Supplier<T> source) throws IOException {
        T result = source.getWithIO();
        if (result == null) {
            throw new IOException("Null resource");
        }
        return result;
    }

    static InputStream open(File source) throws IOException {
        return new BufferedInputStream(new FileInputStream(source));
    }

    static void checkFile(File source) throws FileSystemException {
        if (!source.exists()) {
            throw new NoSuchFileException(source.getPath());
        }
        if (!source.isFile()) {
            throw new AccessDeniedException(source.getPath());
        }
    }

    static String getSystemId(File file) {
        return file.toURI().toASCIIString();
    }

    static File getFile(String systemId) {
        try {
            return new File(URI.create(systemId));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
