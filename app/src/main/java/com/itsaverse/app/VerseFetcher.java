package com.itsaverse.app;

import com.itsaverse.app.utils.DataUtils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class VerseFetcher {

    public VerseFetcher() {}

    public static void requestEsvPassage(String passage, Callback<String> callback) {
        Converter basicConverter = new Converter() {
            @Override
            public String fromBody(TypedInput typedInput, Type type) throws ConversionException {
                if (typedInput == null) return null;

                try {
                    return IOUtils.toString(typedInput.in());
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public TypedOutput toBody(final Object o) {
                return new TypedOutput() {
                    @Override
                    public String fileName() {
                        return null;
                    }

                    @Override
                    public String mimeType() {
                        return "text/html";
                    }

                    @Override
                    public long length() {
                        return ((String)o).length();
                    }

                    @Override
                    public void writeTo(OutputStream outputStream) throws IOException {
                        IOUtils.write(((String)o), outputStream);
                    }
                };
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://www.esvapi.org/v2/rest")
                .setConverter(basicConverter)
                .build();

        EsvService service = restAdapter.create(EsvService.class);
        service.passageHtml("IP", passage, callback);
    }

    public interface EsvService {
        @GET("/passageQuery")
        void passageHtml(
                @Query("key") String key,
                @Query("passage") String passage,
                Callback<String> cb);
    }
}
