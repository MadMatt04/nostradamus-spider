package si.mkejzar.ns;

/**
 * Created by matija on 17/06/14.
 */
public class User implements Comparable<User> {

    private String name;
    private String username;

    private int score;
    private int ranking = 10000;

    private double percentage;

    public User() {
    }

    public User(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public int compareTo(User o) {
        int cmp = o.score - score;
        if (cmp != 0) {
            return cmp;
        }

        return ranking - o.ranking;
    }
}
