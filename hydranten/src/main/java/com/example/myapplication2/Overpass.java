package com.example.myapplication2;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by michaelzilske on 01/01/14.
 */
public class Overpass {
    public static final String BOUNDING_BOX = "(52.33743685775091,13.103256225585936,52.60888546492018,13.646392822265625)";

    public static List<FireStation> getFireStations() throws IOException, IllegalStateException, XmlPullParserException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://overpass-api.de/api/interpreter");
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("data", "node[\"amenity\"=\"fire_station\"]"+ BOUNDING_BOX +";out;"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            System.out.println(response.toString());
            HttpEntity responseEntity = response.getEntity();
            List<FireStation> result = parse(responseEntity.getContent());
            return result;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<FireStation> getFireStationsWays() throws IOException, IllegalStateException, XmlPullParserException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://overpass-api.de/api/interpreter");
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("data", "way[\"amenity\"=\"fire_station\"]"+ BOUNDING_BOX +";(._;>;);out;"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            System.out.println(response.toString());
            HttpEntity responseEntity = response.getEntity();
            List<FireStation> result = parse(responseEntity.getContent());
            return result;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Hydrant> getHydrants() throws IOException, IllegalStateException, XmlPullParserException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://overpass-api.de/api/interpreter");
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("data", "node[\"emergency\"=\"fire_hydrant\"]" + BOUNDING_BOX +";out;"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            System.out.println(response.toString());
            HttpEntity responseEntity = response.getEntity();
            List<Hydrant> result = parse(responseEntity.getContent());
            return result;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List parse(InputStream content) throws XmlPullParserException, IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(content, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            content.close();
        }
    }

    private static List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();
        Map<String, LatLng> nodes = new HashMap<String, LatLng>();
        String ns = null;
        parser.require(XmlPullParser.START_TAG, ns, "osm");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("node")) {
                Object node = readEntry(parser, nodes);
                if (node != null) {
                    entries.add(node);
                }
            } else if (name.equals("way")) {
                FireStation way = readWay(parser, nodes);
                entries.add(way);
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static FireStation readWay(XmlPullParser parser, Map<String, LatLng> nodes) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "way");
        double latitude = 0.0;
        double longitude = 0.0;
        int nNd = 0;
        Map<String, String> tags = new HashMap<String, String>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("tag")) {
                readTag(tags, parser);
            } else if (name.equals("nd")) {
                String k = parser.getAttributeValue(null, "ref");
                LatLng nd = nodes.get(k);
                latitude += nd.latitude;
                longitude += nd.longitude;
                ++nNd;
                while (parser.next() != XmlPullParser.END_TAG) {
                    continue;
                }
            } else {
                skip(parser);
            }
        }
        if ("fire_station".equals(tags.get("amenity"))) {
            return new FireStation(new LatLng(latitude / nNd, longitude / nNd));
        } else {
            throw new RuntimeException("Strange thing.");
        }
    }

    private static Object readEntry(XmlPullParser parser, Map<String, LatLng> nodes) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "node");
        String id = parser.getAttributeValue(null, "id");
        String lat = parser.getAttributeValue(null, "lat");
        String lon = parser.getAttributeValue(null, "lon");
        LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
        Map<String, String> tags = new HashMap<String, String>();
        nodes.put(id, latLng);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("tag")) {
                readTag(tags, parser);
            } else {
                skip(parser);
            }
        }
        if ("fire_hydrant".equals(tags.get("emergency"))) {
            return new Hydrant(latLng, tags.get("fire_hydrant:type"));
        } else if ("fire_station".equals(tags.get("amenity"))) {
            return new FireStation(latLng);
        } else {
            return null;
        }
    }

    private static void readTag(Map<String, String> tags, XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "tag");
        String k = parser.getAttributeValue(null, "k");
        String v = parser.getAttributeValue(null, "v");
        tags.put(k, v);
        while (parser.next() != XmlPullParser.END_TAG) {
            continue;
        }
    }

    static class Hydrant {
        public final String type;
        public final LatLng latLng;
        public Hydrant(LatLng latLng, String s) {
            this.latLng = latLng;
            this.type = s;
        }
    }

    static class FireStation {
        public LatLng latLng;

        private FireStation(LatLng latLng) {
            this.latLng = latLng;
        }
    }
}
