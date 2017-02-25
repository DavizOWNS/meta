package sk.tuke.mp.example;


import sk.tuke.mp.persistence.annotations.Column;
import sk.tuke.mp.persistence.annotations.Entity;
import sk.tuke.mp.persistence.annotations.Id;

@Entity(name = "Departments")
public class Department implements IDepartment{
    @Id
    @Column(required = true)
	private int id;
    @Column(required = true, maxLength = 20)
    private String name;
    @Column(required = true, maxLength = 10)
    private String code;

    public Department() {
    }

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String toString() {
        return String.format("Department %d: %s (%s)", id, name, code);
    }
}
