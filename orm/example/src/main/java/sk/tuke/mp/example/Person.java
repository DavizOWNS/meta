package sk.tuke.mp.example;

import sk.tuke.mp.persistence.annotations.Column;
import sk.tuke.mp.persistence.annotations.Entity;
import sk.tuke.mp.persistence.annotations.Id;
import sk.tuke.mp.persistence.annotations.LazyFetch;

@Entity(name = "People")
public class Person implements IPerson{
    @Id
    @Column(required = true)
    private int id;
    @Column(required = true, maxLength = 20)
    private String surname;
    @Column(getter = "getName", maxLength = 20)
    private String name;
    @Column()
    private int age;

    @LazyFetch(targetEntity = Department.class)
    @Column(name = "dep")
    private IDepartment department;

    public Person(String surname, String name, int age) {
        this.surname = surname;
        this.name = name;
        this.age = age;
    }

    public Person() {
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getId() {
        return id;
    }


    public IDepartment getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return String.format("Person %d: %s %s (%d)", id, surname, name, age);
    }
}
