package com.example.flux.domain.model;

public class FeedPost {
    public String userId;
    public String userEmail;
    public String habitName;
    public String aiVerdict;
    public long timestamp;
    public String photoUrl;    // optional, for future storage
    public String postType;    // "ai_verified" or "manual"
}
