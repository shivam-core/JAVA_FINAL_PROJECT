package com.shivamcore.loanapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a system user.
 */
public class User {
    private Long id;
    private String fname;
    private String lname;
    private String mobile;
    private String email;
    private String location;
    private String panCard;
    private Integer cibilScore;
    private LocalDateTime lastModifiedDate;
    private Status status;
    private String username;
    private String passwordHash;
    private Role role;
    private int failedAttempts;

    public User() {
        this.status = Status.ACTIVE;
        this.role = Role.ROLE_USER;
        this.failedAttempts = 0;
        this.lastModifiedDate = LocalDateTime.now();
    }

    public User(String fname, String lname, String mobile, String email, String location, String username, String passwordHash) {
        this(null, fname, lname, mobile, email, location, LocalDateTime.now(), Status.ACTIVE, username, passwordHash, Role.ROLE_USER, 0);
    }

    public User(Long id, String fname, String lname, String mobile, String email, String location,
                LocalDateTime lastModifiedDate, Status status, String username, String passwordHash, int failedAttempts) {
        this(id, fname, lname, mobile, email, location, lastModifiedDate, status, username, passwordHash, Role.ROLE_USER, failedAttempts);
    }

    public User(Long id, String fname, String lname, String mobile, String email, String location,
                LocalDateTime lastModifiedDate, Status status, String username, String passwordHash, Role role, int failedAttempts) {
        this.id = id;
        this.fname = fname;
        this.lname = lname;
        this.mobile = mobile;
        this.email = email;
        this.location = location;
        this.lastModifiedDate = lastModifiedDate != null ? lastModifiedDate : LocalDateTime.now();
        this.status = status != null ? status : Status.ACTIVE;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.ROLE_USER;
        this.failedAttempts = failedAttempts;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getFullName() {
        return (fname != null ? fname : "") + " " + (lname != null ? lname : "");
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPanCard() {
        return panCard;
    }

    public void setPanCard(String panCard) {
        this.panCard = panCard;
    }

    public Integer getCibilScore() {
        return cibilScore;
    }

    public void setCibilScore(Integer cibilScore) {
        this.cibilScore = cibilScore;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Role getRole() {
        return role != null ? role : Role.ROLE_USER;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.ROLE_USER;
    }

    public void setUserRole(Role role) {
        setRole(role);
    }

    public boolean isAdmin() {
        return Role.ROLE_ADMIN.equals(this.role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) ||
                (Objects.equals(username, user.username) && username != null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", location='" + location + '\'' +
                ", lastModifiedDate=" + lastModifiedDate +
                ", status=" + status +
                ", username='" + username + '\'' +
                ", failedAttempts=" + failedAttempts +
                '}';
    }
}
