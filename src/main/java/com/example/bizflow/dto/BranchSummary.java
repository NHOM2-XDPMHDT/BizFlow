package com.example.bizflow.dto;

public class BranchSummary {
    private Long id;
    private String name;
    private String ownerName;
    private Boolean active;
    private String note;

    public BranchSummary() {}

    public BranchSummary(Long id, String name, String ownerName, Boolean active, String note) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.active = active;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
