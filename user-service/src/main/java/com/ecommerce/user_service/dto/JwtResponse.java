package com.ecommerce.user_service.dto;

public class JwtResponse {
    private String accessToken;

    // Private constructor for builder pattern
    private JwtResponse(Builder builder) {
        this.accessToken = builder.accessToken;
    }

    // Static method to create builder
    public static Builder builder() {
        return new Builder();
    }

    // Getter
    public String getAccessToken() {
        return accessToken;
    }

    // Setter (if needed)
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    // Default constructor (if needed for frameworks)
    public JwtResponse() {}

    // All args constructor (if needed)
    public JwtResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    // Builder class
    public static class Builder {
        private String accessToken;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public JwtResponse build() {
            return new JwtResponse(this);
        }
    }

    @Override
    public String toString() {
        return "JwtResponse{" +
                "accessToken='" + accessToken + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtResponse that = (JwtResponse) o;
        return accessToken != null ? accessToken.equals(that.accessToken) : that.accessToken == null;
    }

    @Override
    public int hashCode() {
        return accessToken != null ? accessToken.hashCode() : 0;
    }
}
