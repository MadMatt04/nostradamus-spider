package si.mkejzar.ns;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author matijak on 17/06/14.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "HardcodedLineSeparator", "MethodWithMultipleLoops"})
public class NostradamusCrawler {

    private final Map<String, User> users;

    private final String baseUrl;
    private final String page;
    private final int pages;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd. MM. uuuu HH:mm");

    private final Map<User, Pattern> regexMap = new HashMap<>();

    private final AtomicReference<User> leader = new AtomicReference<>();


    private static final Pattern LEADER_PATTERN = Pattern.compile(
            "<td class=\"tac\">1\\.</td>\n\\s*\n\\s*<td><a href=\"/profil/\\S+\">(.+)</a></td>\n\\s+<td class=\"tac\">(\\d+)</td>");

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getPercentInstance();

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(1);
        NUMBER_FORMAT.setMaximumFractionDigits(1);
    }

    public NostradamusCrawler() {
        users = new LinkedHashMap<>();
        users.put("mm04", new User("Matija", "mm04"));
        users.put("sp_nostradamus", new User("Igor", "sp_nostradamus"));
        users.put("frimili", new User("Emil", "frimili"));
        users.put("SuperMario45", new User("Blaž", "SuperMario45"));
        users.put("HHrv", new User("Helena", "HHrv"));
        users.put("grega,gor", new User("Grega", "grega.gor"));
        users.put("skipper3k", new User("Luka", "skipper3k"));
        users.put("zzl02", new User("Žiga", "zzl02"));
        users.put("Nutrij", new User("Marko", "Nutrij"));

        baseUrl = "http://www.rtvslo.si/nostradamus/svetovno-prvenstvo-rusija-2018/lestvica";
        page = "/?page=";
        pages = 35;
    }

    public void crawl() throws IOException {

        List<User> usersToFind = new ArrayList<>(users.values());

        CloseableHttpClient httpClient = HttpClients.createDefault();

        for (int p = 0; p < pages && !usersToFind.isEmpty(); p++) {
            HttpGet httpget = new HttpGet(baseUrl + page + p);

            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                System.out.println("Reading page " + p);
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Did not receive 200, exiting after " + httpget.getURI());
                }


                String content = EntityUtils.toString(response.getEntity());

                if (p == 0) {
                    Matcher m = LEADER_PATTERN.matcher(content);
                    if (m.find()) {
                        String username = m.group(1);
                        int score = Integer.parseInt(m.group(2));

                        User leaderUser = new User("Skupno vodilni", username);
                        leaderUser.setRanking(1);
                        leaderUser.setScore(score);
                        calculatePercentage(leaderUser);

                        leader.set(leaderUser);

                        System.out.println("Leader found: " + leaderUser);
                    }
                }

                for (Iterator<User> it = usersToFind.iterator(); it.hasNext(); ) {
                    User user = it.next();
                    System.out.print("Looking for user " + user.getUsername() + "...");
                    Pattern regex = regexForUser(user);
                    Matcher m = regex.matcher(content);
                    if (m.find()) {
                        int ranking = Integer.parseInt(m.group(1));
                        int score = Integer.parseInt(m.group(2));

                        System.out.println("FOUND, pts: " + score + ", ranking: " + ranking);
                        user.setScore(score);
                        user.setRanking(ranking);
                        calculatePercentage(user);
                        it.remove();
                    } else {
                        System.out.println("not found");
                    }
                }
            }
        }
    }

    public String produceOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("Lestvica, generirana ");
        sb.append(dateTimeFormatter.format(LocalDateTime.now()));
        sb.append(", nostradamus-spider:\n");

        List<User> sortedUsers = users.values().stream()
                .sorted()
                .collect(Collectors.toList());

        int p = 1;
        for (User user : sortedUsers) {
            sb.append(p++);
            sb.append(". ");
            sb.append(user.getName());
            sb.append(" (");
            sb.append(user.getUsername());
            sb.append(") ");
            sb.append(user.getScore());
            sb.append(" (");
            sb.append(NUMBER_FORMAT.format(user.getPercentage()));
            sb.append(" točk), pozicija: ");
            sb.append(user.getRanking());
            sb.append('\n');
        }

        sb.append("------\n");
        User leaderUser = leader.get();
        sb.append("Skupno vodilni: ");
        sb.append(leaderUser.getRanking());
        sb.append(". ");
        sb.append(leaderUser.getUsername());
        sb.append(": ");
        sb.append(leaderUser.getScore());
        sb.append(" od ");
        sb.append(MatchesData.TOTAL_POINTS);
        sb.append(" točk (");
        sb.append(NUMBER_FORMAT.format(leaderUser.getPercentage()));
        sb.append("), vseh tekem: ");
        sb.append(MatchesData.TOTAL_MATCHES);

        return sb.toString();
    }

    private Pattern regexForUser(User user) {
        Pattern regex = regexMap.get(user);
        if (regex == null) {
            regex = Pattern.compile("<td class=\"tac\">(\\d+)\\.</td>\n\\s*\n\\s*<td><a href=\"/profil/\\S+\">" + user
                    .getUsername() + "</a></td>\n\\s+<td class=\"tac\">(\\d+)</td>");
            regexMap.put(user, regex);
        }

        return regex;
    }

    private void calculatePercentage(User user) {
        user.setPercentage((double)user.getScore() / (double)MatchesData.TOTAL_POINTS);
    }

    public static void main(String[] args) throws IOException {
        NostradamusCrawler c = new NostradamusCrawler();
        c.crawl();
        System.out.println("----------");
        System.out.println(c.produceOutput());
    }

}
