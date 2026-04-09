package com.example.applicationrftgcma;

import com.google.gson.annotations.SerializedName;

public class FilmComment {
    public FilmComment() {
    }

    // =========================
    // ATTRIBUTS
    // =========================

    @SerializedName("commentId")
    private int id;

    @SerializedName("filmId")
    private int filmId;

    @SerializedName("customerId")
    private int customerId;

    @SerializedName("commentText")
    private String commentText;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("customerName")
    private String customerName;

    // =========================
    // GETTERS
    // =========================

    public int getId() {
        return id;
    }

    public int getFilmId() {
        return filmId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getCommentText() {
        return commentText != null ? commentText : "";
    }

    public String getCreatedDate() {
        return createdDate != null ? createdDate : "";
    }

    public String getCustomerName() {
        return customerName != null ? customerName : "";
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(int id) {
        this.id = id;
    }

    public void setFilmId(int filmId) {
        this.filmId = filmId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // =========================
    // TOSTRING
    // =========================

    @Override
    public String toString() {
        return "FilmComment{" +
                "id=" + id +
                ", filmId=" + filmId +
                ", customerId=" + customerId +
                ", commentText='" + commentText + '\'' +
                ", createdDate='" + createdDate + '\'' +
                '}';
    }
}