package org.mydotey.tool.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;

public class IFTopStats {

    public static void main(String[] args) throws IOException, URISyntaxException {
        URI[] uris = toURI(args);
        doStats(uris);
    }

    static URI[] toURI(String... files) throws URISyntaxException {
        URI[] results = new URI[files.length];
        for (int i = 0; i < files.length; i++)
            results[i] = new URI(files[i]);
        return results;
    }

    static Comparator<Record> byInCumulative = (r1, r2) -> {
        double result = r1.getIn().getCumulative() - r2.getIn().getCumulative();
        if (result > 0)
            return -1;
        else if (result < 0)
            return 1;
        else
            return 0;
    };

    static Comparator<Record> byOutCumulative = (r1, r2) -> {
        double result = r1.getOut().getCumulative() - r2.getOut().getCumulative();
        if (result > 0)
            return -1;
        else if (result < 0)
            return 1;
        else
            return 0;
    };

    static Comparator<Record> byInLast40s = (r1, r2) -> {
        double result = r1.getIn().getLast40s() - r2.getIn().getLast40s();
        if (result > 0)
            return -1;
        else if (result < 0)
            return 1;
        else
            return 0;
    };

    static Comparator<Record> byOutLast40s = (r1, r2) -> {
        double result = r1.getOut().getLast40s() - r2.getOut().getLast40s();
        if (result > 0)
            return -1;
        else if (result < 0)
            return 1;
        else
            return 0;
    };

    static void doStats(URI... statsFiles) throws IOException {
        List<Record> records = readAllRecords(statsFiles);

        //records.sort(byInLast40s);
        //showInSimple(records);
        //showIn(records);
        //System.out.println("\n");

        records.sort(byOutLast40s);
        showOutSimple(records);
        //showOut(records);
        //System.out.println("\n");
    }

    static void showIn(List<Record> records) {
        printTitle();
        RateStats total = new RateStats();
        records.forEach(r -> {
            printRecord(r.getRemote(), r.getIn());
            total.setLast2s(total.getLast2s() + r.getIn().getLast2s());
            total.setLast10s(total.getLast10s() + r.getIn().getLast10s());
            total.setLast40s(total.getLast40s() + r.getIn().getLast40s());
            total.setCumulative(total.getCumulative() + r.getIn().getCumulative());
        });

        printRecord("total", total.getLast2s(), total.getLast10s(), total.getLast40s(), total.getCumulative());
    }

    static void showOut(List<Record> records) {
        printTitle();
        RateStats total = new RateStats();
        records.forEach(r -> {
            printRecord(r.getRemote(), r.getOut());
            total.setLast2s(total.getLast2s() + r.getOut().getLast2s());
            total.setLast10s(total.getLast10s() + r.getOut().getLast10s());
            total.setLast40s(total.getLast40s() + r.getOut().getLast40s());
            total.setCumulative(total.getCumulative() + r.getOut().getCumulative());
        });

        printRecord("total", total.getLast2s(), total.getLast10s(), total.getLast40s(), total.getCumulative());
    }

    static void showInSimple(List<Record> records) {
        printTitleSimple();
        RateStats total = new RateStats();
        records.forEach(r -> {
            printRecordSimple(r.getRemote(), r.getIn());
            total.setLast2s(total.getLast2s() + r.getIn().getLast2s());
            total.setLast10s(total.getLast10s() + r.getIn().getLast10s());
            total.setLast40s(total.getLast40s() + r.getIn().getLast40s());
            total.setCumulative(total.getCumulative() + r.getIn().getCumulative());
        });

        printRecordSimple("total", total.getLast40s());
    }

    static void showOutSimple(List<Record> records) {
        printTitleSimple();
        RateStats total = new RateStats();
        records.forEach(r -> {
            printRecordSimple(r.getRemote(), r.getOut());
            total.setLast2s(total.getLast2s() + r.getOut().getLast2s());
            total.setLast10s(total.getLast10s() + r.getOut().getLast10s());
            total.setLast40s(total.getLast40s() + r.getOut().getLast40s());
            total.setCumulative(total.getCumulative() + r.getOut().getCumulative());
        });

        printRecordSimple("total", total.getLast40s());
    }

    static void printTitle() {
        System.out.println("remote, last2s, last10s, last40s, cumulative");
    }

    static void printRecord(SocketEnd socketEnd, RateStats rateStats) {
        printRecord(socketEnd.getHost(), rateStats.getLast2s(), rateStats.getLast10s(), rateStats.getLast40s(),
                rateStats.getCumulative());
    }

    static void printRecord(String host, double last2s, double last10s, double last40s, double cumulative) {
        System.out.printf("%s, %s, %s, %s, %s\n", host, toString(last2s), toString(last10s), toString(last40s),
                toString(cumulative));
    }

    static void printTitleSimple() {
        System.out.println("remote, last40s");
    }

    static void printRecordSimple(SocketEnd socketEnd, RateStats rateStats) {
        printRecordSimple(socketEnd.getHost(), rateStats.getLast40s());
    }

    static void printRecordSimple(String host, double last40s) {
        System.out.printf("%s, %s\n", host, toString(last40s));
    }

    static List<Record> readAllRecords(URI... statsFiles) throws IOException {
        Map<String, Record> recordMap = new HashMap<>();
        for (URI statsFile : statsFiles) {
            List<Record> records = readRecords(statsFile);
            records.forEach(r -> {
                Record existing = recordMap.get(r.getRemote().getHost());
                if (existing == null) {
                    recordMap.put(r.getRemote().getHost(), r);
                    return;
                }

                merge(existing.getIn(), r.getIn());
                merge(existing.getOut(), r.getOut());
            });
        }

        return new ArrayList<>(recordMap.values());
    }

    static void merge(RateStats rateStats, RateStats rateStats2) {
        rateStats.setLast2s(rateStats.getLast2s() + rateStats2.getLast2s());
        rateStats.setLast10s(rateStats.getLast10s() + rateStats2.getLast10s());
        rateStats.setLast40s(rateStats.getLast40s() + rateStats2.getLast40s());
        rateStats.setCumulative(rateStats.getCumulative() + rateStats2.getCumulative());
    }

    static List<Record> readRecords(URI statsFile) throws IOException {
        Path path = Paths.get(statsFile);
        List<String> lines = Files.readAllLines(path);
        boolean started = false;
        List<Record> records = new ArrayList<>();
        Record current = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean flagLine = line.startsWith("-");
            if (flagLine) {
                if (!started) {
                    started = true;
                    continue;
                }

                break;
            }

            if (!started)
                continue;

            List<String> parts = Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(line);
            if (current == null) {
                current = new Record();
                current.setLocal(parseSocketEnd(parts.get(1)));
                RateStats outStats = new RateStats();
                outStats.setLast2s(parseValue(parts.get(3)));
                outStats.setLast10s(parseValue(parts.get(4)));
                outStats.setLast40s(parseValue(parts.get(5)));
                outStats.setCumulative(parseValue(parts.get(6)));
                current.setOut(outStats);

                continue;
            }

            current.setRemote(parseSocketEnd(parts.get(0)));
            RateStats outStats = new RateStats();
            outStats.setLast2s(parseValue(parts.get(2)));
            outStats.setLast10s(parseValue(parts.get(3)));
            outStats.setLast40s(parseValue(parts.get(4)));
            outStats.setCumulative(parseValue(parts.get(5)));
            current.setIn(outStats);

            records.add(current);
            current = null;
        }

        return records;
    }

    static SocketEnd parseSocketEnd(String hostPort) {
        String[] hostPortParts = hostPort.split(":");
        SocketEnd socketEnd = new SocketEnd();
        socketEnd.setHost(hostPortParts[0]);
        if (hostPortParts.length >= 2)
            socketEnd.setPort(hostPortParts[1]);
        return socketEnd;
    }

    static double parseValue(String rawValue) {
        if (rawValue.endsWith("Gb"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024 * 1024 * 1024 / 8;

        if (rawValue.endsWith("Mb"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024 * 1024 / 8;

        if (rawValue.endsWith("Kb"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024 / 8;

        if (rawValue.endsWith("b"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 1)) / 8;

        if (rawValue.endsWith("GB"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024 * 1024 * 1024;

        if (rawValue.endsWith("MB"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024 * 1024;

        if (rawValue.endsWith("KB"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 2)) * 1024;

        if (rawValue.endsWith("B"))
            return Double.valueOf(rawValue.substring(0, rawValue.length() - 1));

        return Double.valueOf(rawValue);
    }

    static String toString(double value) {
        return String.valueOf(value / 1024 / 1024);
    }

    static class Record {

        private SocketEnd local;
        private SocketEnd remote;
        private RateStats in;
        private RateStats out;

        public SocketEnd getLocal() {
            return local;
        }

        public void setLocal(SocketEnd local) {
            this.local = local;
        }

        public SocketEnd getRemote() {
            return remote;
        }

        public void setRemote(SocketEnd remote) {
            this.remote = remote;
        }

        public RateStats getIn() {
            return in;
        }

        public void setIn(RateStats in) {
            this.in = in;
        }

        public RateStats getOut() {
            return out;
        }

        public void setOut(RateStats out) {
            this.out = out;
        }

        @Override
        public String toString() {
            return "Record [local=" + local + ", remote=" + remote + ", in=" + in + ", out=" + out + "]";
        }

    }

    static class SocketEnd {

        private String host;
        private String port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "SocketEnd [host=" + host + ", port=" + port + "]";
        }

    }

    static class RateStats {

        private double last2s;
        private double last10s;
        private double last40s;
        private double cumulative;

        public double getLast2s() {
            return last2s;
        }

        public void setLast2s(double last2s) {
            this.last2s = last2s;
        }

        public double getLast10s() {
            return last10s;
        }

        public void setLast10s(double last10s) {
            this.last10s = last10s;
        }

        public double getLast40s() {
            return last40s;
        }

        public void setLast40s(double last40s) {
            this.last40s = last40s;
        }

        public double getCumulative() {
            return cumulative;
        }

        public void setCumulative(double cumulative) {
            this.cumulative = cumulative;
        }

        @Override
        public String toString() {
            return "RateStats [last2s=" + last2s + ", last10s=" + last10s + ", last40s=" + last40s + ", cumulative="
                    + cumulative + "]";
        }

    }

}
