package org.woftnw.dreamvisitorhub.data.type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DVUser {
  private String id;
  private String collectionId;
  private String collectionName;

  private UUID mcUuid;
  private String mcUsername;
  private String discordUsername;
  private String discordId;
  private String discordImg;
  private Long snowflakeId;

  private List<String> infractions;
  private List<String> usersHome;
  private List<String> inventoryItems;
  private List<String> claims;
  private List<String> alts; // Added alts field

  private Integer claimLimit;
  private Integer playTime;
  private Double balance;
  private Integer dailyStreak;

  private OffsetDateTime lastWork;
  private OffsetDateTime lastDaily;
  private OffsetDateTime lastPlayed;

  private Boolean isSuspended;
  private Boolean isBanned;

  private OffsetDateTime created;
  private OffsetDateTime updated;

  // Existing getters and setters
  public UUID getMinecraftUuid() {
    return mcUuid;
  }

  public Long getSnowflakeId() {
    return snowflakeId;
  }

  public String getDiscordUsername() {
    return discordUsername;
  }

  public String getMinecraftUsername() {
    return mcUsername;
  }

  public void setDiscordUsername(String dcUsername) {
    this.discordUsername = dcUsername;
  }

  public void setMcUsername(String mcUsername) {
    this.mcUsername = mcUsername;
  }

  public void setSnowflakeId(Long snowflakeId) {
    this.snowflakeId = snowflakeId;
  }

  public void setMcUuid(UUID uuid) {
    this.mcUuid = uuid;
  }

  // New getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public String getDiscordId() {
    return discordId;
  }

  public void setDiscordId(String discord_id) {
    this.discordId = discord_id;
  }

  public String getDiscordAvatarUrl() {
    return discordImg;
  }

  public void setDiscordImg(String discord_img) {
    this.discordImg = discord_img;
  }

  public List<String> getInfractions() {
    return infractions;
  }

  public void setInfractions(List<String> infractions) {
    this.infractions = infractions;
  }

  public List<String> getUsersHome() {
    return usersHome;
  }

  public void setUsersHome(List<String> usersHome) {
    this.usersHome = usersHome;
  }

  public List<String> getInventoryItems() {
    return inventoryItems;
  }

  public void setInventoryItems(List<String> inventoryItems) {
    this.inventoryItems = inventoryItems;
  }

  public List<String> getClaims() {
    return claims;
  }

  public void setClaims(List<String> claims) {
    this.claims = claims;
  }

  public List<String> getAlts() {
    return alts;
  }

  public void setAlts(List<String> alts) {
    this.alts = alts;
  }

  public Integer getClaimLimit() {
    return claimLimit;
  }

  public void setClaimLimit(Integer claimLimit) {
    this.claimLimit = claimLimit;
  }

  public Integer getPlayTime() {
    return playTime;
  }

  public void setPlayTime(Integer playTime) {
    this.playTime = playTime;
  }

  public Double getBalance() {
    return balance;
  }

  public void setBalance(Double balance) {
    this.balance = balance;
  }

  public Integer getDailyStreak() {
    return dailyStreak;
  }

  public void setDailyStreak(Integer daily_streak) {
    this.dailyStreak = daily_streak;
  }

  public OffsetDateTime getLastWork() {
    return lastWork;
  }

  public void setLastWork(OffsetDateTime lastWork) {
    this.lastWork = lastWork;
  }

  public OffsetDateTime getLastPlayed() {
    return lastPlayed;
  }

  public OffsetDateTime getLastDaily() {
    return lastDaily;
  }

  public void setLastDaily(OffsetDateTime lastDaily) {
    this.lastDaily = lastDaily;
  }

  public Boolean getIsSuspended() {
    return isSuspended;
  }

  public void setIsSuspended(Boolean isSuspended) {
    this.isSuspended = isSuspended;
  }

  public Boolean getIsBanned() {
    return isBanned;
  }

  public void setIsBanned(Boolean isBanned) {
    this.isBanned = isBanned;
  }

  public OffsetDateTime getCreated() {
    return created;
  }

  public void setCreated(OffsetDateTime created) {
    this.created = created;
  }

  public OffsetDateTime getUpdated() {
    return updated;
  }

  public void setUpdated(OffsetDateTime updated) {
    this.updated = updated;
  }

  public void setLastPlayed(OffsetDateTime last_played) {
    this.lastPlayed = last_played;
  }
}
