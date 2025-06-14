package com.example.gpsstandalone;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> headers;

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.headers = new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // text data
            Map<String, String> params = getParams();
            if (params != null && params.size() > 0) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    buildTextPart(dos, entry.getKey(), entry.getValue());
                }
            }

            // file data
            Map<String, DataPart> data = getByteData();
            if (data != null && data.size() > 0) {
                for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                    buildFilePart(dos, entry.getKey(), entry.getValue());
                }
            }

            // end multipart
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            return bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void buildTextPart(DataOutputStream dos, String paramName, String paramValue) throws IOException {
        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + paramName + "\"" + lineEnd);
        dos.writeBytes(lineEnd);
        dos.writeBytes(paramValue + lineEnd);
    }

    private void buildFilePart(DataOutputStream dos, String paramName, DataPart fileData) throws IOException {
        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" +
                fileData.getFileName() + "\"" + lineEnd);
        dos.writeBytes("Content-Type: " + fileData.getType() + lineEnd);
        dos.writeBytes(lineEnd);

        dos.write(fileData.getContent());
        dos.writeBytes(lineEnd);
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    public abstract Map<String, DataPart> getByteData();

    public static class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String name, byte[] data) {
            this(name, data, "application/octet-stream");
        }

        public DataPart(String name, byte[] data, String type) {
            fileName = name;
            content = data;
            this.type = type;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }
}
