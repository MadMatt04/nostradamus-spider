package si.mkejzar.ns;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by matija on 17/06/14.
 */
public class NostradamusCrawler {

    private final Map<String, User> users = ImmutableMap.<String, User>builder()
            .put("mm04", new User("Matija", "mm04"))
            .put("sp_nostradamus", new User("Igor", "sp_nostradamus"))
            .put("frimili", new User("Emil", "frimili"))
            .put("SuperMario45", new User("Blaž", "SuperMario45"))
            .put("HHrv", new User("Helena", "HHrv"))
            .put("grega,gor", new User("Grega", "grega.gor"))
            .put("skipper3k", new User("Luka", "skipper3k"))
            .put("zzl02", new User("Žiga", "zzl02"))
            .build();

    private final String baseUrl = "http://www.rtvslo.si/nostradamus/evropsko-prvenstvo-francija-2016/lestvica";
    private final String page = "/?page=";
    private final int pages = 35;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd. MM. uuuu HH:mm");

    private final Map<User, Pattern> regexMap = new HashMap<>();

    public void crawl() throws IOException {

        List<User> usersToFind = new ArrayList<>(users.values());

        CloseableHttpClient httpclient = HttpClients.createDefault();

        for (int p = 0; p < pages && !usersToFind.isEmpty(); p++) {
            HttpGet httpget = new HttpGet((baseUrl + page) + p);
            CloseableHttpResponse response = httpclient.execute(httpget);
            System.out.println("Reading page " + p);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Did not receive 200, exiting after " + httpget.getURI());
            }
            try {
                String content = EntityUtils.toString(response.getEntity());
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
                        it.remove();
                    } else {
                        System.out.println("not found");
                    }
                }
            } finally {
                response.close();
            }
        }
    }

    public String produceOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("Lestvica, generirana ");
        sb.append(dateTimeFormatter.format(LocalDateTime.now()));
        sb.append(", nostradamus-spider:\n");

        List<User> sortedUsers = new ArrayList<>(users.values());
        Collections.sort(sortedUsers);

        int p = 1;
        for (User user : sortedUsers) {
            sb.append(p++);
            sb.append(". ");
            sb.append(user.getName());
            sb.append(" (");
            sb.append(user.getUsername());
            sb.append(") ");
            sb.append(user.getScore());
            sb.append(", pozicija na nostradamusu: ");
            sb.append(user.getRanking());
            sb.append("\n");
        }

        return sb.toString();
    }

    private Pattern regexForUser(User user) {
        Pattern regex = regexMap.get(user);
        if (regex == null) {
            regex = Pattern.compile("<td class=\"tac\">(\\d+)\\.</td>\n\\s*\n\\s*<td><a href=\"/profil/\\S+\">" + user.getUsername() + "</a></td>\n\\s+<td class=\"tac\">(\\d+)</td>");
            regexMap.put(user, regex);
        }

        return regex;
    }

    public static void main(String[] args) throws IOException {
        NostradamusCrawler c = new NostradamusCrawler();
        c.crawl();
        System.out.println("----------");
        System.out.println(c.produceOutput());
    }

}
