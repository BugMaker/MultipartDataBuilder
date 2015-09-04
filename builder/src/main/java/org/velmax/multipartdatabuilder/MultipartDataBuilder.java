package org.velmax.multipartdatabuilder;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for multipart data
 */
public class MultipartDataBuilder {
    private static final String LINE_FEED = "\r\n";
    HashMap<String, String> headersFields;
    HashMap<String, String> formFields;
    HashMap<String, BinaryField> formFiles;
    String charset;
    HttpURLConnection connection = null;
    String boundary;
    OutputStream outputStream = null;
    PrintWriter writer = null;

    /**
     * Simple class for post multipart data with HttpUrlConnection
     *
     * @param connection Your HttpUrlConnection
     * @param charset    Charset. I most cases it's "UTF-8"
     * @throws IOException
     */
    public MultipartDataBuilder(HttpURLConnection connection, String charset) throws IOException {
        super();
        this.charset = charset;
        headersFields = new HashMap<>();
        formFields = new HashMap<>();
        formFiles = new HashMap<>();
        this.connection = connection;
    }

    private void prepareConnection() throws IOException {
        // creates a unique boundary based on time stamp
        boundary = "***AndroidMultipartBoundary" + System.currentTimeMillis() + "***";

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
    }

    /**
     * Adds header field to multipart data
     *
     * @param name  Field name
     * @param value Field value
     * @return self
     */
    public MultipartDataBuilder addHeaderField(String name, String value) {
        headersFields.put(name, value);
        return this;
    }

    /**
     * Adds data field to multipart data
     *
     * @param name  Field name
     * @param value Field value
     * @return self
     */
    public MultipartDataBuilder addFormField(String name, String value) {
        formFields.put(name, value);
        return this;
    }

    /**
     * Add file field to multipart data. You need to override BinaryField for get stream
     *
     * @param name Field name
     * @param file Your implementation of BinaryField
     * @return self
     */
    public MultipartDataBuilder addFormFile(String name, BinaryField file) {
        formFiles.put(name, file);
        return this;
    }

    /**
     * Add file from filesystem to multipart data. It change file name to given by you
     *
     * @param name     Field name
     * @param file     Your file
     * @param fileName New file name
     * @return self
     */
    public MultipartDataBuilder addFormFile(String name, File file, String fileName) {
        formFiles.put(name, new FileField(file, fileName));
        return this;
    }

    /**
     * Add file from filesystem to multipart data. It leave file name from file system.
     *
     * @param name Field name
     * @param file Your file
     * @return self
     */
    public MultipartDataBuilder addFormFile(String name, File file) {
        formFiles.put(name, new FileField(file));
        return this;
    }

    private void finalizeContent() throws IOException {
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private void sendContent() throws IOException {
        outputStream = connection.getOutputStream();
        writer = new PrintWriter(outputStream);

        //headers
        for (Map.Entry<String, String> entry : headersFields.entrySet()) {
            writer.append(entry.getKey()).append(": ").append(entry.getValue()).append(LINE_FEED);
        }
        //fields
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"").append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=").append(charset).append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(entry.getValue()).append(LINE_FEED);
        }
        //files
        for (Map.Entry<String, BinaryField> entry : formFiles.entrySet()) {
            BinaryField binaryField = entry.getValue();
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"; filename=\"").append(binaryField.getFileName()).append("\"").append(LINE_FEED);
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(binaryField.getFileName())).append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            InputStream inputStream = binaryField.getStream();
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                inputStream.close();
            }
            outputStream.flush();

            writer.append(LINE_FEED);
        }
        writer.flush();
    }

    public void build() throws IOException {
        try {
            prepareConnection();
            sendContent();
            finalizeContent();
        } finally {
            if (outputStream != null) outputStream.close();
        }
    }


    /**
     * Allow makes custom streams. You need just make openStream method
     */
    public abstract static class BinaryField implements Closeable {
        String fileName;
        InputStream stream;

        public BinaryField(String fileName) {
            super();
            setFileName(fileName);
        }

        public InputStream getStream() throws IOException {
            if (stream != null) return stream;
            else
                return stream = openStream();
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String name) {
            fileName = name;
        }

        /**
         * This method allow open stream immediatelly when file sending.
         * If you have already opended stream, you can return him.
         * Then file was send, stream will be closed.
         * To avoid this behavior, override close() method
         *
         * @return your stream
         * @throws IOException
         */
        protected abstract InputStream openStream() throws IOException;

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }

    /**
     * FileInputStream implementation, for internal consumption
     */
    private static class FileField extends BinaryField {
        File file;

        public FileField(File file) {
            this(file, file.getName());
        }

        public FileField(File file, String name) {
            super(name);
            this.file = file;
        }

        @Override
        protected InputStream openStream() throws IOException {
            return new FileInputStream(file);
        }


    }

}

