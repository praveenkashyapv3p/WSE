package servlet;

/**
 * Getters and Setters for URL, Rank, Score
 */
public class FetchedResultData {
    int id;
    int rank;
    String url;
    String title;
    Double score;


    public String getTitle() { return this.title; }

    public void setTitle(String title) { this.title = title; }

    public int getId() { return this.id;  }

    public void setId(int id) { this.id = id; }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "{" +
                "\"rank\":" + rank +
                ",\"url\":\"" + url + '\"' +
                ",\"score\":" + score +
                '}';
    }
}
