package org.acme;

import jakarta.persistence.*;

@Entity
@Table
@NamedQuery(name = "Organizations.findAll", query = "SELECT c FROM Organization c ORDER BY c.name")
public class Organization {

    @Id
    @SequenceGenerator(name = "organizationsSequence", sequenceName = "known_organizations_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "organizationsSequence")
    private Integer id;

    @Column()
    private String name;

    public Organization() {
    }

    public Organization(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
 
}