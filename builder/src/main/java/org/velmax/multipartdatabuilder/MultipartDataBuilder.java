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
     *
     * @param connection Your HttpUrlConnection
     * @param charset Charset. I most cases it's "UTF-8"
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
     * @param name Field name
     * @param value Field value
     * @return
     */
    public MultipartDataBuilder addHeaderField(String name, String value) {
        headersFields.put(name, value);
        return this;
    }

    /**
     * Adds data field to multipart data
     * @param name Field name
     * @param value Field value
     * @return
     */
    public MultipartDataBuilder addFormField(String name, String value) {
        formFields.put(name, value);
        return this;
    }

    /**
     * Add file field to multipart data. You need to override BinaryField for get stream
     * @param name Field name
     * @param file Your implementation of BinaryField
     * @return
     */
    public MultipartDataBuilder addFormFile(String name, BinaryField file) {
        formFiles.put(name, file);
        return this;
    }

    /**
     * Add file from filesystem to multipart data. It change file name to given by you
     * @param name Field name
     * @param file Your file
     * @param fileName New file name
     * @return
     */
    public MultipartDataBuilder addFormFile(String name, File file, String fileName) {
        formFiles.put(name, new FileField(file, fileName));
        return this;
    }

    /**
     * Add file from filesystem to multipart data. It leave file name from file system.
     * @param name Field name
     * @param file Your file
     * @return
     */
    public MultipartDataBuilder addFormFile(String name, File file) {
        formFiles.put(name, new FileField(file));
        return this;
    }

    private void finalizeContent() throws IOException {
        writer.append("--" + boundary + "--" + LINE_FEED);
        writer.close();
    }

    private void sendContent() throws IOException {
        outputStream = connection.getOutputStream();
        writer = new PrintWriter(outputStream);

        //headers
        for (Map.Entry<String, String> entry : headersFields.entrySet()) {
            writer.append(entry.getKey() + ": " + entry.getValue() + LINE_FEED);
            writer.flush();
        }
        //fields
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            writer.append("--" + boundary + LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset + LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(entry.getValue() + LINE_FEED);
            writer.flush();
        }
        //files
        for (Map.Entry<String, BinaryField> entry : formFiles.entrySet()) {
            BinaryField binaryField = entry.getValue();
            writer.append("--" + boundary + LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + entry.getKey()
                            + "\"; filename=\"" + binaryField.getFileName() + "\"" + LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(binaryField.getFileName()) + LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary" + LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            InputStream inputStream = binaryField.getStream();
            try {
                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                inputStream.close();
            }
            outputStream.flush();

            writer.append(LINE_FEED);
            writer.flush();
        }
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

