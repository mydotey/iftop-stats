package org.mydotey.tool.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

/**
 * @author koqizhao
 *
 * Dec 4, 2018
 */
public class IFTopStatsTest {

    @Test
    public void doStatsTest() throws URISyntaxException, IOException {
        URI[] uris = toURI(new String[] { "/eth1.txt" });
        //URI[] uris = toURI(new String[] { "/bond0.txt" });
        //URI[] uris = toURI(new String[] { "/eth1.txt", "/bond0.txt" });
        IFTopStats.doStats(uris);
    }

    static URI[] toURI(String... resources) throws URISyntaxException {
        URI[] results = new URI[resources.length];
        for (int i = 0; i < resources.length; i++)
            results[i] = toURI(resources[i]);
        return results;
    }

    static URI toURI(String resource) throws URISyntaxException {
        URL url = IFTopStatsTest.class.getResource(resource);
        return new URI(url.toString());
    }

}
