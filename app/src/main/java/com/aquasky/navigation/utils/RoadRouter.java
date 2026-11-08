package com.aquasky.navigation.utils;

import com.aquasky.navigation.models.VehicleProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Vehicle-aware road router for AquaSky (Dhaka city + Dhaka-Chattogram corridor).
 *
 * How it works (this is the part to explain in your viva):
 *  1. The road network is a weighted graph: nodes = junctions, edges = road segments.
 *  2. Every edge has attributes: road class, width, weight limit, height limit, central-city flag.
 *  3. Every vehicle has a rule set: minimum road width, weight, height, speed per road class
 *     (speed 0 = class forbidden), and whether it may use heavy-freight roads.
 *  4. Dijkstra runs on the graph, but an edge that breaks ANY rule for the chosen vehicle is skipped.
 *     Cost = travel time + a small penalty for risk, so the result is fast AND sensible.
 *
 * NOTE: the segments and rules below are SAMPLE / MODELLED data for the project demo,
 * not official regulations. Replace with real data (OSM / routing API) for production.
 */
public class RoadRouter {

    // ------------------------------------------------------------------ road classes
    public enum RoadClass {
        ELEVATED("Elevated Expressway"),
        HIGHWAY("Highway"),
        BYPASS("Freight Bypass"),
        ARTERIAL("City Arterial"),
        NARROW_LANE("Narrow Lane"),
        RURAL("Local / Rural Road");

        final String label;
        RoadClass(String label) { this.label = label; }
    }

    // ------------------------------------------------------------------ graph data
    private static class Node {
        final String name; final double lat, lng; final boolean terminal;
        Node(String name, double lat, double lng, boolean terminal) {
            this.name = name; this.lat = lat; this.lng = lng; this.terminal = terminal;
        }
    }

    private static class Edge {
        final int a, b; final String name; final RoadClass cls;
        final double km, widthM, maxWeightT, maxHeightM; final boolean centralCity; final int tollBdt;
        boolean busCorridor;   // dedicated bus corridor: transit buses move faster here
        Edge(int a, int b, String name, RoadClass cls, double km, double widthM, double maxWeightT,
             double maxHeightM, boolean centralCity, int tollBdt) {
            this.a = a; this.b = b; this.name = name; this.cls = cls; this.km = km;
            this.widthM = widthM; this.maxWeightT = maxWeightT; this.maxHeightM = maxHeightM;
            this.centralCity = centralCity; this.tollBdt = tollBdt;
        }
        int other(int n) { return n == a ? b : a; }
        boolean isHeavyTraffic() { return cls == RoadClass.HIGHWAY || cls == RoadClass.BYPASS; }
    }

    // node indexes
    private static final int AIRPORT = 0, UTTARA = 1, GULSHAN = 2, MOHAKHALI = 3, FARMGATE = 4,
            DHANMONDI = 5, OLD_DHAKA = 6, MOTIJHEEL = 7, SAYEDABAD = 8, EAST_BYPASS = 9,
            KANCHPUR = 10, NARAYANGANJ = 11, DAUDKANDI = 12, COMILLA = 13, FENI = 14,
            MIRSARAI = 15, SITAKUNDA = 16, CTG = 17, CTG_PORT = 18;

    private static final List<Node> NODES = new ArrayList<>();
    private static final List<Edge> EDGES = new ArrayList<>();
    private static final List<List<Edge>> ADJ = new ArrayList<>();

    static {
        node("Dhaka Airport (Kawla)", 23.851, 90.408, false);
        node("Uttara", 23.875, 90.385, false);
        node("Gulshan", 23.790, 90.414, false);
        node("Mohakhali Bus Terminal", 23.778, 90.398, true);
        node("Farmgate", 23.757, 90.389, false);
        node("Dhanmondi", 23.746, 90.374, false);
        node("Old Dhaka (Lalbagh)", 23.710, 90.407, false);
        node("Motijheel", 23.733, 90.418, false);
        node("Sayedabad Bus Terminal", 23.710, 90.431, true);
        node("Eastern Bypass", 23.900, 90.470, false);
        node("Kanchpur Bridge", 23.690, 90.505, false);
        node("Narayanganj", 23.620, 90.500, false);
        node("Daudkandi", 23.540, 90.720, false);
        node("Comilla", 23.460, 91.180, false);
        node("Feni", 23.020, 91.400, false);
        node("Mirsarai", 22.770, 91.580, false);
        node("Sitakunda", 22.620, 91.670, false);
        node("Chattogram City", 22.340, 91.810, false);
        node("Chattogram Port", 22.310, 91.800, false);

        // ---- Dhaka city ----
        edge(AIRPORT, UTTARA, "Airport Road", RoadClass.ARTERIAL, 14, 99, 9, false, 0);
        edge(UTTARA, EAST_BYPASS, "Uttara-Bypass Link", RoadClass.ARTERIAL, 12, 99, 9, false, 0);
        edge(AIRPORT, EAST_BYPASS, "Airport-Eastern Bypass Link", RoadClass.ARTERIAL, 12, 99, 9, false, 0);
        edge(AIRPORT, MOHAKHALI, "Airport Rd - Mohakhali", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(AIRPORT, GULSHAN, "Airport - Gulshan Link", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(GULSHAN, MOHAKHALI, "Gulshan - Mohakhali", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(GULSHAN, MOTIJHEEL, "Gulshan - Moghbazar - Motijheel", RoadClass.ARTERIAL, 11, 99, 9, true, 0);
        edge(MOHAKHALI, FARMGATE, "Mohakhali - Farmgate", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(FARMGATE, DHANMONDI, "Farmgate - Dhanmondi", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(DHANMONDI, OLD_DHAKA, "Dhanmondi - Lalbagh", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(FARMGATE, MOTIJHEEL, "Farmgate - Motijheel (ground)", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(MOTIJHEEL, SAYEDABAD, "Motijheel - Sayedabad (ground)", RoadClass.ARTERIAL, 12, 99, 9, true, 0);
        edge(OLD_DHAKA, MOTIJHEEL, "Old Dhaka Lanes (Lalbagh-Motijheel)", RoadClass.NARROW_LANE, 3.0, 99, 9, true, 0);
        edge(OLD_DHAKA, SAYEDABAD, "Old Dhaka Lanes (Lalbagh-Sayedabad)", RoadClass.NARROW_LANE, 3.0, 99, 9, true, 0);
        // elevated expressway (parallel to the ground roads)
        edge(AIRPORT, FARMGATE, "Kawla - Farmgate Elevated Expressway", RoadClass.ELEVATED, 14, 99, 9, true, 60);
        edge(FARMGATE, MOTIJHEEL, "Farmgate - Motijheel Elevated Expressway", RoadClass.ELEVATED, 14, 99, 9, true, 60);
        edge(MOTIJHEEL, SAYEDABAD, "Motijheel - Sayedabad Elevated Expressway", RoadClass.ELEVATED, 14, 99, 9, true, 40);

        // ---- Dhaka -> Chattogram corridor: main (heavy-freight) highway ----
        edge(SAYEDABAD, KANCHPUR, "Dhaka-Chattogram Hwy (Jatrabari-Kanchpur)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);
        edge(EAST_BYPASS, KANCHPUR, "Eastern Freight Bypass", RoadClass.BYPASS, 20, 99, 9, false, 0);
        edge(KANCHPUR, DAUDKANDI, "Dhaka-Chattogram Hwy (Meghna Bridge)", RoadClass.HIGHWAY, 20, 99, 9, false, 160);
        edge(DAUDKANDI, COMILLA, "Dhaka-Chattogram Hwy (Daudkandi-Comilla)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);
        edge(COMILLA, FENI, "Dhaka-Chattogram Hwy (Comilla-Feni)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);
        edge(FENI, MIRSARAI, "Dhaka-Chattogram Hwy (Feni-Mirsarai)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);
        edge(MIRSARAI, SITAKUNDA, "Dhaka-Chattogram Hwy (Mirsarai-Sitakunda)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);
        edge(SITAKUNDA, CTG, "Dhaka-Chattogram Hwy (Sitakunda-Chattogram)", RoadClass.HIGHWAY, 20, 99, 9, false, 0);

        // ---- parallel local / rural roads (slower, narrow, weak bridges: light vehicles only) ----
        edge(SAYEDABAD, NARAYANGANJ, "Demra-Narayanganj Road", RoadClass.ARTERIAL, 12, 99, 9, false, 0);
        edge(KANCHPUR, NARAYANGANJ, "Kanchpur-Narayanganj Road (rail underpass)", RoadClass.ARTERIAL, 15, 15, 3.9, false, 0);
        edge(NARAYANGANJ, DAUDKANDI, "Narayanganj-Daudkandi Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);
        edge(DAUDKANDI, COMILLA, "Daudkandi-Comilla Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);
        edge(COMILLA, FENI, "Comilla-Feni Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);
        edge(FENI, MIRSARAI, "Feni-Mirsarai Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);
        edge(MIRSARAI, SITAKUNDA, "Mirsarai-Sitakunda Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);
        edge(SITAKUNDA, CTG, "Sitakunda-Chattogram Local Road", RoadClass.RURAL, 3.5, 4, 9, false, 0);

        // ---- Chattogram ----
        edge(CTG, CTG_PORT, "Chattogram Port Access Road", RoadClass.ARTERIAL, 5, 99, 9, false, 0);

        // ---- dedicated bus corridors (connect the Mohakhali & Sayedabad terminals) ----
        String[] busCorridors = {"Airport Rd - Mohakhali", "Mohakhali - Farmgate",
                "Farmgate - Motijheel (ground)", "Motijheel - Sayedabad (ground)"};
        for (Edge e : EDGES) {
            for (String name : busCorridors) {
                if (e.name.equals(name)) e.busCorridor = true;
            }
        }
    }

    private static void node(String name, double lat, double lng, boolean terminal) {
        NODES.add(new Node(name, lat, lng, terminal));
        ADJ.add(new ArrayList<Edge>());
    }

    /** distance factor turns straight-line km into road km. */
    private static void edge(int a, int b, String name, RoadClass cls, double widthOrKmHint,
                             double maxWeightT, double maxHeightM, boolean central, int toll) {
        // For readability the 5th argument is the carriageway width in metres for narrow/rural roads
        // and a "typical width hint" for the others; actual width is chosen by class below.
        double width;
        switch (cls) {
            case ELEVATED: width = 14; break;
            case HIGHWAY: width = 16; break;
            case BYPASS: width = 20; break;
            case ARTERIAL: width = 12; break;
            default: width = widthOrKmHint; break;   // NARROW_LANE / RURAL carry their own width
        }
        double factor;
        switch (cls) {
            case ELEVATED: factor = 1.05; break;
            case HIGHWAY: factor = 1.15; break;
            case BYPASS: factor = 1.25; break;
            case ARTERIAL: factor = 1.35; break;
            case NARROW_LANE: factor = 1.45; break;
            default: factor = 1.35; break;
        }
        Node na = NODES.get(a), nb = NODES.get(b);
        double km = haversineKm(na.lat, na.lng, nb.lat, nb.lng) * factor;
        Edge e = new Edge(a, b, name, cls, km, width, maxWeightT, maxHeightM, central, toll);
        EDGES.add(e);
        ADJ.get(a).add(e);
        ADJ.get(b).add(e);
    }

    // ------------------------------------------------------------------ vehicle rules
    private static class Rule {
        final String label, emoji;
        final double minRoadWidthM, weightT, heightM;
        final double[] speedKmh;            // by RoadClass ordinal; 0 = not allowed on that class
        final boolean bannedFromCentralCity, bannedFromHeavyTraffic;
        final double riskMult, heavyRiskMult, tollMult;
        Rule(String label, String emoji, double minRoadWidthM, double weightT, double heightM,
             double[] speedKmh, boolean bannedFromCentralCity, boolean bannedFromHeavyTraffic,
             double riskMult, double heavyRiskMult, double tollMult) {
            this.label = label; this.emoji = emoji; this.minRoadWidthM = minRoadWidthM;
            this.weightT = weightT; this.heightM = heightM; this.speedKmh = speedKmh;
            this.bannedFromCentralCity = bannedFromCentralCity;
            this.bannedFromHeavyTraffic = bannedFromHeavyTraffic;
            this.riskMult = riskMult; this.heavyRiskMult = heavyRiskMult; this.tollMult = tollMult;
        }
    }

    // speeds order: ELEVATED, HIGHWAY, BYPASS, ARTERIAL, NARROW_LANE, RURAL
    private static final Rule IDEAL = new Rule("Unrestricted", "", 0, 0, 0,
            new double[]{75, 70, 65, 22, 18, 35}, false, false, 1, 1, 1);

    private static Rule ruleFor(VehicleProfile.Type t) {
        switch (t) {
            case TRANSIT_BUS:
                return new Rule("Transit Bus", "🚌", 6.0, 12, 3.6,
                        new double[]{0, 55, 55, 15, 0, 0}, false, false, 0.9, 1.0, 1.6);
            case CARGO_TRUCK:
                return new Rule("Cargo Truck", "🚛", 6.5, 16, 4.2,
                        new double[]{0, 48, 50, 25, 0, 0}, true, false, 1.0, 1.0, 2.5);
            case MOTORCYCLE:
                return new Rule("Bike", "🏍️", 1.2, 0.2, 1.6,
                        new double[]{0, 55, 50, 30, 22, 35}, false, false, 2.0, 1.4, 0.125);
            case CNG_AUTO:
                return new Rule("Taxi / Auto", "🛺", 2.5, 0.6, 1.7,
                        new double[]{0, 30, 30, 18, 14, 28}, false, true, 1.5, 1.5, 0.5);
            case CAR_SUV:
            default:
                return new Rule("Car / SUV", "🚗", 3.5, 2, 1.8,
                        new double[]{75, 70, 65, 22, 0, 35}, false, false, 1.0, 1.0, 1.0);
        }
    }

    /** Effective speed of a vehicle on an edge (buses get a faster speed on bus corridors). */
    private static double speedOn(Edge e, Rule r) {
        if (e.busCorridor && r.label.equals("Transit Bus")) return 30;
        return r.speedKmh[e.cls.ordinal()];
    }

    /** Returns the reason an edge is closed to this vehicle, or null if it is allowed. */
    private static String blockReason(Edge e, Rule r) {
        if (r.speedKmh[e.cls.ordinal()] <= 0) {
            if (e.cls == RoadClass.NARROW_LANE || e.cls == RoadClass.RURAL) {
                return e.name + ": road too narrow / weak for " + r.label;
            }
            return e.name + ": " + r.label + " not allowed on " + e.cls.label.toLowerCase(Locale.US);
        }
        if (r.bannedFromCentralCity && e.centralCity) {
            return e.name + ": central-city heavy vehicle restriction";
        }
        if (r.bannedFromHeavyTraffic && e.isHeavyTraffic()) {
            return e.name + ": heavy freight traffic, unsafe for " + r.label;
        }
        if (e.widthM < r.minRoadWidthM) {
            return String.format(Locale.US, "%s: only %.1f m wide, %s needs %.1f m", e.name, e.widthM, r.label, r.minRoadWidthM);
        }
        if (e.maxWeightT < r.weightT) {
            return String.format(Locale.US, "%s: %.0f t weight limit, %s is %.0f t", e.name, e.maxWeightT, r.label, r.weightT);
        }
        if (e.maxHeightM < r.heightM) {
            return String.format(Locale.US, "%s: %.1f m clearance, %s is %.1f m tall", e.name, e.maxHeightM, r.label, r.heightM);
        }
        return null;
    }

    private static double baseRiskPerKm(RoadClass c) {
        switch (c) {
            case ELEVATED: return 6;
            case HIGHWAY: return 14;
            case BYPASS: return 10;
            case ARTERIAL: return 14;
            case NARROW_LANE: return 16;
            default: return 12;
        }
    }

    private static double edgeRisk(Edge e, Rule r) {
        double mult = e.isHeavyTraffic() ? r.riskMult * r.heavyRiskMult : r.riskMult;
        return baseRiskPerKm(e.cls) * mult;
    }

    // ------------------------------------------------------------------ result
    public static class Result {
        public boolean found;
        public String message;                 // used when found == false
        public List<double[]> coords = new ArrayList<>();   // {lat, lng}
        public List<String> viaNames = new ArrayList<>();
        public List<String> notes = new ArrayList<>();       // why other roads were avoided
        public double distanceKm;
        public int etaMinutes;
        public int riskScore;                  // 0..100
        public int tollBdt;
        public String vehicleLabel = "";
        public String vehicleEmoji = "";
        public String startName = "", endName = "";
    }

    // ------------------------------------------------------------------ public API
    public static Result findRoute(VehicleProfile.Type type, double sLat, double sLng, double eLat, double eLng) {
        Rule rule = ruleFor(type);
        Result res = new Result();
        res.vehicleLabel = rule.label;
        res.vehicleEmoji = rule.emoji;

        int s = nearestNode(sLat, sLng);
        int t = nearestNode(eLat, eLng);
        res.startName = NODES.get(s).name;
        res.endName = NODES.get(t).name;

        if (s == t) {
            res.found = false;
            res.message = "Origin and destination map to the same junction (" + res.startName + ").";
            return res;
        }

        List<Edge> path = dijkstra(s, t, rule, true);
        if (path == null) {
            res.found = false;
            res.message = "No safe route for " + rule.label + " between " + res.startName + " and " + res.endName
                    + ". Every connecting road breaks a size, weight or safety rule.";
            return res;
        }

        // ---- build geometry & totals
        double minutes = 0, riskWeighted = 0, km = 0;
        int toll = 0;
        int cur = s;
        res.coords.add(new double[]{NODES.get(cur).lat, NODES.get(cur).lng});
        res.viaNames.add(NODES.get(cur).name);
        for (Edge e : path) {
            double speed = speedOn(e, rule);
            minutes += e.km / speed * 60.0;
            riskWeighted += edgeRisk(e, rule) * e.km;
            km += e.km;
            toll += Math.round(e.tollBdt * rule.tollMult);
            cur = e.other(cur);
            res.coords.add(new double[]{NODES.get(cur).lat, NODES.get(cur).lng});
            res.viaNames.add(NODES.get(cur).name);
        }
        res.found = true;
        res.distanceKm = km;
        res.etaMinutes = (int) Math.round(minutes);
        res.tollBdt = toll;
        res.riskScore = (int) Math.max(3, Math.min(95, Math.round(riskWeighted / Math.max(km, 1))));

        // ---- explain: which roads would an unrestricted driver use that this vehicle must avoid?
        List<Edge> ideal = dijkstra(s, t, IDEAL, false);
        if (ideal != null) {
            List<String> seenReasons = new ArrayList<>();
            for (Edge e : ideal) {
                String why = blockReason(e, rule);
                if (why == null) continue;
                // one note per kind of restriction (all elevated segments share one reason, etc.)
                String key = why.substring(why.indexOf(": ") + 2);
                if (!seenReasons.contains(key) && res.notes.size() < 3) {
                    seenReasons.add(key);
                    res.notes.add("Avoided " + why);
                }
            }
        }
        // positive facts about the chosen route
        for (Edge e : path) {
            if (e.cls == RoadClass.NARROW_LANE) { res.notes.add("Uses narrow lanes as a shortcut"); break; }
        }
        for (Edge e : path) {
            if (e.cls == RoadClass.ELEVATED) { res.notes.add("Uses the elevated expressway"); break; }
        }
        for (Edge e : path) {
            if (e.cls == RoadClass.BYPASS) { res.notes.add("Uses freight bypass, avoids city centre"); break; }
        }
        boolean viaLocal = false;
        for (Edge e : path) if (e.cls == RoadClass.RURAL) viaLocal = true;
        if (viaLocal && rule.bannedFromHeavyTraffic) {
            res.notes.add("Takes slower local roads to stay off the heavy freight highway");
        }
        if (type == VehicleProfile.Type.TRANSIT_BUS) {
            for (Edge e : path) {
                if (e.busCorridor) { res.notes.add("Uses dedicated bus corridor"); break; }
            }
        }
        for (int i = 0; i < res.viaNames.size(); i++) {
            for (int n = 0; n < NODES.size(); n++) {
                if (NODES.get(n).terminal && NODES.get(n).name.equals(res.viaNames.get(i))
                        && type == VehicleProfile.Type.TRANSIT_BUS) {
                    res.notes.add("Stops at " + NODES.get(n).name);
                }
            }
        }
        return res;
    }

    // ------------------------------------------------------------------ internals
    /** Dijkstra: skips every edge that is blocked for the vehicle (when enforce == true). */
    private static List<Edge> dijkstra(int src, int dst, Rule rule, boolean enforce) {
        int n = NODES.size();
        double[] dist = new double[n];
        Edge[] prevEdge = new Edge[n];
        boolean[] done = new boolean[n];
        java.util.Arrays.fill(dist, Double.MAX_VALUE);
        dist[src] = 0;

        for (int iter = 0; iter < n; iter++) {
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!done[i] && (u == -1 || dist[i] < dist[u])) u = i;
            }
            if (u == -1 || dist[u] == Double.MAX_VALUE) break;
            done[u] = true;
            if (u == dst) break;

            for (Edge e : ADJ.get(u)) {
                if (enforce && blockReason(e, rule) != null) continue;
                int v = e.other(u);
                double speed = speedOn(e, rule);
                if (speed <= 0) continue;
                double minutes = e.km / speed * 60.0;
                double cost = minutes + edgeRisk(e, rule) * e.km * 0.04;   // time + small risk penalty
                if (dist[u] + cost < dist[v]) {
                    dist[v] = dist[u] + cost;
                    prevEdge[v] = e;
                }
            }
        }
        if (dist[dst] == Double.MAX_VALUE) return null;

        List<Edge> path = new ArrayList<>();
        int cur = dst;
        while (cur != src) {
            Edge e = prevEdge[cur];
            path.add(e);
            cur = e.other(cur);
        }
        Collections.reverse(path);
        return path;
    }

    private static int nearestNode(double lat, double lng) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < NODES.size(); i++) {
            double d = haversineKm(lat, lng, NODES.get(i).lat, NODES.get(i).lng);
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
