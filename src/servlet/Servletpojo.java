package servlet;

import java.util.List;

public class Servletpojo {
    List<FetchedResultData> fetchedResultData;
    List<MetaPOJO> metaResultData;
    List<Servletpojo> adResultData;
    List<ImagePOJO> imageResultData;
    String[] searchTerms;
    String[] stemmedTerms;
    String language;
    int k_val;
    int model;
    String advertitle;
    String adverturl;
    String textofad;
    String advertimage;

    public String getAdvertitle() {
        return this.advertitle;
    }

    public void setAdvertitle(String advertitle) {
        this.advertitle = advertitle;
    }

    public String getAdverturl() {
        return this.adverturl;
    }

    public void setAdverturl(String adverturl) {
        this.adverturl = adverturl;
    }

    public String getTextofad() {
        return this.textofad;
    }

    public void setTextofad(String textofad) {
        this.textofad = textofad;
    }

    public String getAdvertimage() {
        return this.advertimage;
    }

    public void setAdvertimage(String advertimage) {
        this.advertimage = advertimage;
    }

    public int getModel() {
        return this.model;
    }

    public void setModel(int model) {
        this.model = model;
    }

    public int getK_val() {
        return this.k_val;
    }

    public void setK_val(int k_val) {
        this.k_val = k_val;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String[] getSearchTerms() {
        return this.searchTerms;
    }

    public void setSearchTerms(String[] searchTerms) {
        this.searchTerms = searchTerms;
    }

    public void setImageResultData(List<ImagePOJO> fetchedResultData) {
        this.imageResultData = fetchedResultData;
    }

    public List<ImagePOJO> getImageResultData() {
        return imageResultData;
    }

    public List<Servletpojo> getAdResultData() {
        return adResultData;
    }

    public void setAdResultData(List<Servletpojo> adResultData) {
        this.adResultData = adResultData;
    }

    public List<MetaPOJO> getMetaResultData() {
        return metaResultData;
    }

    public void setMetaResultData(List<MetaPOJO> metaResultData) {
        this.metaResultData = metaResultData;
    }

    public List<FetchedResultData> getFetchedResultData() {
        return fetchedResultData;
    }

    public void setFetchedResultData(List<FetchedResultData> fetchedResultData) {
        this.fetchedResultData = fetchedResultData;
    }

    public String[] getStemmedTerms() {
        return stemmedTerms;
    }

    public void setStemmedTerms(String[] stemmedTerms) {
        this.stemmedTerms = stemmedTerms;
    }

}
