package org.velmax.sample;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import org.velmax.multipartdatabuilder.MultipartDataBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onPostButtonPress(View v) {
        new HttpPoster().execute();
    }

    class HttpPoster extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection connection = null;
            String content = null;
            try {
                //Make our connection
                URL url = new URL("http://httpbin.org/post");
                connection = (HttpURLConnection) url.openConnection();
                //Make builder with connection and charset
                MultipartDataBuilder builder = new MultipartDataBuilder(connection, "UTF-8");
                //Add fields
                builder
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
                        //.addFormFile("Field",new File("/sdcard/myfile.jpg"))

                        //Post data
                        .build();
                InputStream in = connection.getInputStream();
                //Simplest way to get String from InputStream
                Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\A"); //Scan to the end of file
                if (scanner.hasNext())
                    content = scanner.next();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String content) {
            if (content != null) {
                //View server response
                WebView webView = (WebView) findViewById(R.id.webView);
                webView.loadData(content, "text/html", "UTF-8");
            } else Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
        }
    }
}
