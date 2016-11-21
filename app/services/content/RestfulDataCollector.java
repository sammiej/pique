package services.content;

import services.dataAccess.AbstractDataAccess;
import services.dataAccess.proto.PostProto.Post;
import services.sources.RestfulSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static services.PublicConstants.HTTP_GET;

/**
 * This class is capable of making RESTful API calls to collect data, given an
 * {@link RestfulSource} to collect from and an {@link AbstractDataAccess} to place the
 * results
 *
 * @author Reid Oliveira, Sammie Jiang
 */
public class RestfulDataCollector extends AbstractDataCollector {

    private final String USER_AGENT = "Mozilla/5.0";

	private RestfulSource source;
	private Queue<String> trends = new LinkedList<>();

	public RestfulDataCollector(AbstractDataAccess dataAccess, RestfulSource source) {
        super(dataAccess);
        this.source = source;
    }

	@Override
	public RestfulSource getSource() {
		return source;
	}

	@Override
	public List<Post> fetch() {
		/**
		 * All RestfulDataCollectors operate similarly and that behaviour is specified here.
		 * The exact request (url and endpoint) is generated by the source class, and likely
		 * interpreted by it as well
		 */

        String response = makeRequest();
        return source.parseResponse(response);
	}

    /**
     * Builds an http request with the help of a {@link RestfulSource}
     * @return
     */
    public String makeRequest() {

        // if no trends exist for this collector, retrieve them
        if (trends.isEmpty()) {
            trends.addAll(source.getTrends("canada", "vancouver"));
        }

        String nextTrend = trends.poll();

        // source: http://stackoverflow.com/questions/1359689/how-to-send-http-request-in-java

        HttpURLConnection connection = null; // these are one time use connections

        try {
            // send request
            URL url = new URL(source.generateRequestUrl(nextTrend));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HTTP_GET);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            source.addRequestHeaders(connection);

            int responseCode = connection.getResponseCode();

            // get response
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();

        } catch(IOException e) {
            e.printStackTrace();

            // TODO
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }
}
