/*
 * Copyright 2016 National Bank of Belgium
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;

/**
 * Set of utilities related to ZIP.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Zip {

    /**
     * Represents a zip resource whose content can be loaded.
     */
    public interface Loader extends IO.Loader<String, InputStream>, Closeable {

        /**
         * Creates a new loader from a zip file.
         *
         * @param file non-null zip file
         * @return a non-null loader
         * @throws IOException
         */
        @Nonnull
        static Loader of(@Nonnull File file) throws IOException {
            return new ZipFileLoader(new ZipFile(file));
        }

        /**
         * Creates a new loader by copying the content of a zip file.
         *
         * @param inputStream non-null content of zip file
         * @param filter non-null filter to avoid copying everything
         * @return a non-null loader
         * @throws IOException
         */
        @Nonnull
        static Loader copyOf(@Nonnull InputStream inputStream, @Nonnull IO.Predicate<? super ZipEntry> filter) throws IOException {
            return new BytesLoader(readAll(inputStream, filter));
        }
    }

    @lombok.AllArgsConstructor
    private static final class ZipFileLoader implements Loader {

        private final ZipFile file;

        @Override
        public InputStream load(String source) throws IOException {
            return getInputStream(file, source);
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class BytesLoader implements Loader {

        private final Map<String, byte[]> data;
        private boolean closed = false;

        @Override
        public InputStream load(String source) throws IOException {
            if (closed) {
                throw new IllegalStateException();
            }
            return getInputStream(data, source);
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    private InputStream getInputStream(ZipFile zipFile, String name) throws IOException {
        ZipEntry result = zipFile.getEntry(name);
        if (result == null) {
            throw new IOException("Missing entry '" + name + "' in file '" + zipFile.getName() + "'");
        }
        return zipFile.getInputStream(result);
    }

    private InputStream getInputStream(Map<String, byte[]> data, String name) throws IOException {
        Objects.requireNonNull(name);
        byte[] result = data.get(name);
        if (result == null) {
            throw new IOException("Missing entry '" + name + "'");
        }
        return new ByteArrayInputStream(result);
    }

    private Map<String, byte[]> readAll(InputStream stream, IO.Predicate<? super ZipEntry> filter) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(stream)) {
            Zip.forEach(zis, filter, (k, v) -> result.put(k.getName(), v));
        }
        return result;
    }

    private void forEach(ZipInputStream zis, IO.Predicate<? super ZipEntry> filter, BiConsumer<ZipEntry, byte[]> consumer) throws IOException {
        Objects.requireNonNull(filter);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (filter.testWithIO(entry)) {
                consumer.accept(entry, Zip.toByteArray(entry, zis));
            }
        }
    }

    private byte[] toByteArray(ZipEntry entry, ZipInputStream stream) throws IOException {
        long size = entry.getSize();
        if (size >= Integer.MAX_VALUE) {
            throw new IOException("ZIP entry size is too large");
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream(size > 0 ? (int) size : 4096);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = stream.read(buffer)) != -1) {
            result.write(buffer, 0, len);
        }
        return result.toByteArray();
    }
}
