package org.woftnw.DreamvisitorHub.data.type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DVUser {
  private String id;
  private String collectionId;
  private String collectionName;

  private UUID uuid;
  private String mcUsername;
  private String dcUsername;
  private String discord_id;
  private String discord_img;
  private Long snowflakeId;

  private List<String> infractions;
  private List<String> users_home;
  private List<String> inventory_items;
  private List<String> claims;

  private Integer claim_limit;
  private Integer play_time;
  private Double balance;
  private Integer daily_streak;

  private OffsetDateTime last_work;
  private OffsetDateTime last_daily;
  private OffsetDateTime last_played;

  private Boolean is_suspended;
  private Boolean is_banned;

  private OffsetDateTime created;
  private OffsetDateTime updated;

  // Existing getters and setters
  public UUID getUuid() {
    return uuid;
  }

  public Long getSnowflakeId() {
    return snowflakeId;
  }

  public String getDcUsername() {
    return dcUsername;
  }

  public String getMcUsername() {
    return mcUsername;
  }

  public void setDcUsername(String dcUsername) {
    this.dcUsername = dcUsername;
  }

  public void setMcUsername(String mcUsername) {
    this.mcUsername = mcUsername;
  }

  public void setSnowflakeId(Long snowflakeId) {
    this.snowflakeId = snowflakeId;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
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

  public String getDiscord_id() {
    return discord_id;
  }

  public void setDiscord_id(String discord_id) {
    this.discord_id = discord_id;
  }

  public String getDiscord_img() {
    return discord_img;
  }

  public void setDiscord_img(String discord_img) {
    this.discord_img = discord_img;
  }

  public List<String> getInfractions() {
    return infractions;
  }

  public void setInfractions(List<String> infractions) {
    this.infractions = infractions;
  }

  public List<String> getUsers_home() {
    return users_home;
  }

  public void setUsers_home(List<String> users_home) {
    this.users_home = users_home;
  }

  public List<String> getInventory_items() {
    return inventory_items;
  }

  public void setInventory_items(List<String> inventory_items) {
    this.inventory_items = inventory_items;
  }

  public List<String> getClaims() {
    return claims;
  }

  public void setClaims(List<String> claims) {
    this.claims = claims;
  }

  public Integer getClaim_limit() {
    return claim_limit;
  }

  public void setClaim_limit(Integer claim_limit) {
    this.claim_limit = claim_limit;
  }

  public Integer getPlay_time() {
    return play_time;
  }

  public void setPlay_time(Integer play_time) {
    this.play_time = play_time;
  }

  public Double getBalance() {
    return balance;
  }

  public void setBalance(Double balance) {
    this.balance = balance;
  }

  public Integer getDaily_streak() {
    return daily_streak;
  }

  public void setDaily_streak(Integer daily_streak) {
    this.daily_streak = daily_streak;
  }

  public OffsetDateTime getLast_work() {
    return last_work;
  }

  public void setLast_work(OffsetDateTime last_work) {
    this.last_work = last_work;
  }

  public OffsetDateTime getLast_played() {
    return last_played;
  }

  public OffsetDateTime getLast_daily() {
    return last_daily;
  }

  public void setLast_daily(OffsetDateTime last_daily) {
    this.last_daily = last_daily;
  }

  public Boolean getIs_suspended() {
    return is_suspended;
  }

  public void setIs_suspended(Boolean is_suspended) {
    this.is_suspended = is_suspended;
  }

  public Boolean getIs_banned() {
    return is_banned;
  }

  public void setIs_banned(Boolean is_banned) {
    this.is_banned = is_banned;
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

  public void setLast_Played(OffsetDateTime last_played) {
    this.last_played = last_played;
  }
}
