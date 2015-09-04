# MultipartDataBuilder
Lightweight library for post multipart data with HttpUrlConnection in Android. You can add text fields, files from file system or files from any InputStream.
It simple to use:

    //Make our connection
    URL url = new URL("http://httpbin.org/post");
    connection = (HttpURLConnection) url.openConnection();

    //Make builder with connection and charset
    new MultipartDataBuilder(connection, "UTF-8")
        //Add fields
        .addFormField("Form1", "I am Form 1")
        .addFormField("Form2", "I am Form 2")
        //Sending file as InputStream.
        .addFormFile("FormFile1", new MultipartDataBuilder.BinaryField("testfile.txt") {
            @Override
            public InputStream openStream() throws IOException {
                return getAssets().open("my_asset.txt");
            }
        })
        //Or you can send simple file like this:
        .addFormFile("Field",new File("/sdcard/myfile.jpg"))
        //Post data
        .build();

    InputStream in = connection.getInputStream();
    //Analyze server response

Including library in your project with .gradle file:

    //Make sure, what you include my maven repository
    repositories {
        maven {
            url "http://dl.bintray.com/bugmaker/maven"
        }
    }

    dependencies {
        //Your project dependencies here
        compile 'org.velmax.multipartdata:builder:1.0.0'
    }